package kg.shabykeev.loadbalancer.plan.messageProcessor;

import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.*;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MessageProcessor {
    private static final Logger logger = LogManager.getLogger();
    private KryoSerializer kryo = new KryoSerializer();
    private LinkedList<ZMsg> msgQueue = new LinkedList<>();
    private PlanResult planResult;

    private ZContext ctx;
    public ZMQ.Socket pipe;     //  Socket to talk back to application
    public ZMQ.Socket pairSocket;

    public MessageProcessor(ZContext ctx, ZMQ.Socket pipe, String pairSocketAddress){
        this.ctx = ctx;
        this.pipe = pipe;
        this.pairSocket = this.ctx.createSocket(SocketType.PAIR);
        pairSocket.connect(pairSocketAddress);
    }

    public void addMessage(){
        ZMsg msg = ZMsg.recvMsg(this.pipe);
        ZMsg msgCopy = msg.duplicate();
        Pair<String, Payload> pair = PayloadKt.transformZMsgWithId(msg, kryo);
        if (pair != null) {
            String localLoadAnalyzerId = pair.getFirst();
            Payload payload = pair.getSecond();
            if (payload instanceof Payload.MetricsPayload) {
                msgQueue.add(msgCopy);
            } else if (payload instanceof Payload.TopicMigrateAckPayload) {
                processTopicMigrateAckPayload(localLoadAnalyzerId, (Payload.TopicMigrateAckPayload)payload);
            }
        }

        logger.info("added to queue" + msg);
    }

    public void processPairSocketMessage(){
        ZMsg msg = ZMsg.recvMsg(this.pairSocket);
        Payload payload = PayloadKt.transformZMsg(msg, kryo);
        if (payload != null) {
            if (payload instanceof Payload.PlanResultPayload) {
                PlanResult planResult = ((Payload.PlanResultPayload) payload).getPlanResult();

                if (planResult.isNewPlan()) {
                    if (planResult.getTasks().size() > 0) {
                        performTasks(planResult);
                    } else {
                        releasePlan(planResult);
                    }
                }
            }
        }

        msg.send(this.pipe);
    }

    public void sendMetrics(){
        if (msgQueue.size() > 0) {
            List<String> messages = new ArrayList<>();

            for (ZMsg message: msgQueue) {
                ZFrame lastFrame = message.pollLast();
                String strBytes = Arrays.toString(lastFrame.getData());
                String value = strBytes.replace(",", ";").replace("[", "").replace("]", "");
                message.addLast(value);
            }

            msgQueue.stream().forEach(s->messages.add(s.toString()));
            msgQueue.clear();

            Payload.MetricsAnalyzePayload payload = new Payload.MetricsAnalyzePayload(messages);
            ZMsg msg = PayloadKt.payloadToZMsg(payload, kryo, this.pairSocket.getLastEndpoint());
            msg.send(this.pairSocket);
        }
    }

    private void releasePlan(PlanResult planResult) {
        Payload.PlanPayload payload = new Payload.PlanPayload(planResult.getPlan());
        ZMsg msg = PayloadKt.payloadToZMsg(payload, kryo, pairSocket.getLastEndpoint());

        msg.send(pipe);
        logger.info("Plan " + planResult.getPlanNumber() + " has been released");
        this.planResult = null;
    }

    private void performTasks(PlanResult planResult) {
        this.planResult = planResult;
        for (Task task : planResult.getTasks()) {
            if (task.getTaskType() == TaskType.MIGRATE) {
                Payload.TopicMigratePayload payload = new Payload.TopicMigratePayload(task.getTopic(), task.getServerDestination());
                ZMsg msg = PayloadKt.payloadToZMsg(payload, kryo, task.getServerSource());

                msg.send(pairSocket);
            }
        }

        logger.info("Migration tasks for the plan " + planResult.getPlanNumber() + " have been sent");
    }

    private void processTopicMigrateAckPayload(String server, Payload.TopicMigrateAckPayload payload) {
        updateTask(server, payload.getTopic());

        boolean allDone = !this.planResult.getTasks().stream().anyMatch(s -> s.isDone() == false);
        if (allDone) {
            releasePlan(this.planResult);
        }
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
