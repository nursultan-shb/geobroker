package kg.shabykeev.loadbalancer.plan.generator;

import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.Payload;
import de.hasenburg.geobroker.commons.model.message.PayloadKt;
import kg.shabykeev.loadbalancer.commons.ZMsgType;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

public class Generator extends Thread {
    private static final Logger logger = LogManager.getLogger();
    private KryoSerializer kryo = new KryoSerializer();

    private ZContext ctx;
    public ZMQ.Socket pairSocket;

    private PlanResult planResult = null;

    public Generator(ZContext context, String socketAddress) {
        this.ctx = context;
        Thread.currentThread().setName("plan-generator");

        pairSocket = ctx.createSocket(SocketType.PAIR);
        pairSocket.bind(socketAddress);
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {
            try {
                ZMsg msg = ZMsg.recvMsg(pairSocket);
                Pair<String, Payload> pair = PayloadKt.transformZMsgWithId(msg, kryo);
                if (pair != null) {
                    String server = pair.getFirst();
                    Payload payload = pair.getSecond();

                    if (payload instanceof Payload.MetricsBulkAnalyzePayload) {
                        processMetricsBulkAnalyzePayload((Payload.MetricsBulkAnalyzePayload) payload);
                    } else if (payload instanceof Payload.TopicMigrateAckPayload) {
                        processTopicMigrateAckPayload(server, (Payload.TopicMigrateAckPayload) payload);
                    } else {
                        logger.error("Cannot process message with a payload {}, as this type is not known.", payload);
                    }
                }

            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }

    private void processMetricsBulkAnalyzePayload(Payload.MetricsBulkAnalyzePayload payload) {
        logger.info("Plan-Generator starts the plan creation");
        PlanCreator planCreator = new PlanCreator();
        PlanResult planResult = planCreator.createPlan(payload.getMetrics());
        if (planResult.isNewPlan()) {
            if (planResult.getTasks().size() > 0) {
                migrate(planResult);
            } else {
                releasePlan(planResult);
            }
        }
    }

    private void processTopicMigrateAckPayload(String server, Payload.TopicMigrateAckPayload payload) {
        updateTask(server, payload.getTopic());

        boolean allDone = !this.planResult.getTasks().stream().anyMatch(s -> s.isDone() == false);
        if (allDone) {
            releasePlan(this.planResult);
        }
    }

    private void releasePlan(PlanResult planResult) {
        ZMsg msg = new ZMsg();
        msg.add(ZMsgType.PLAN.toString());
        msg.add(planResult.getPlan());
        msg.send(pairSocket);
        logger.info("Plan " + planResult.getPlanNumber() + " has been released");
        this.planResult = null;
    }

    private void migrate(PlanResult planResult) {
        this.planResult = planResult;
        for (Task task : planResult.getTasks()) {
            Payload.TopicMigratePayload payload = new Payload.TopicMigratePayload(task.getTopic(), task.getServerDestination());
            ZMsg msg = PayloadKt.payloadToZMsg(payload, kryo, task.getServerSource());

            msg.send(pairSocket);
        }

        logger.info("Migration tasks for the plan " + planResult.getPlanNumber() + " have been sent");
    }

    private void updateTask(String server, String topic) {
        for (Task task : planResult.getTasks()) {
            if (task.getTopic().equals(topic) && task.getServerSource().equals(server)) {
                task.setDone(true);
                break;
            }
        }
    }
}
