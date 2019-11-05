package kg.shabykeev.loadbalancer.plan.messageProcessor;

import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.*;
import de.hasenburg.geobroker.commons.model.message.loadbalancer.PlanResult;
import de.hasenburg.geobroker.commons.model.message.loadbalancer.Task;
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
    private Task currentTask;
    private PlanResult planResult;
    private boolean isMigrationOn = false;

    private ZContext ctx;
    public ZMQ.Socket pipe;     //  Socket to talk back to application
    public ZMQ.Socket pairSocket;

    public MessageProcessor(ZContext ctx, ZMQ.Socket pipe, String pairSocketAddress) {
        this.ctx = ctx;
        this.pipe = pipe;
        this.pairSocket = this.ctx.createSocket(SocketType.PAIR);
        pairSocket.connect(pairSocketAddress);
    }

    public void processPipeMessage() {
        ZMsg msg = ZMsg.recvMsg(this.pipe);
        ZMsg msgCopy = msg.duplicate();
        Pair<String, Payload> pair = PayloadKt.transformZMsgWithId(msg, kryo);
        if (pair != null) {
            String localLoadAnalyzerId = pair.getFirst();
            Payload payload = pair.getSecond();
            if (payload instanceof Payload.MetricsPayload && !isMigrationOn) {
                msgQueue.add(msgCopy);
                logger.info("added to queue" + msg);
            } else if (payload instanceof Payload.ReqTopicSubscriptionsAckPayload) {
                //if (((Payload.ReqTopicSubscriptionsAckPayload) payload).getReasonCode() == ReasonCode.Success) {
                    performTasks();
                //}
            }
        }

    }

    public void processPairSocketMessage() {
        ZMsg msg = ZMsg.recvMsg(this.pairSocket);
        Payload payload = PayloadKt.transformZMsg(msg, kryo);
        if (payload != null) {
            if (payload instanceof Payload.PlanResultPayload) {
                PlanResult planResult = ((Payload.PlanResultPayload) payload).getPlanResult();

                if (planResult.isNewPlan()) {
                    if (planResult.getTasks().size() > 0) {
                        isMigrationOn = true;
                        this.planResult = planResult;
                        performTasks();
                    } else {
                        releasePlan(planResult);
                        isMigrationOn = false;
                    }
                }
            }
        }

        msg.send(this.pipe);
    }

    public void sendMetrics() {
        if (msgQueue.size() > 0 && !isMigrationOn) {
            List<String> messages = new ArrayList<>();

            for (ZMsg message : msgQueue) {
                ZFrame lastFrame = message.pollLast();
                String strBytes = Arrays.toString(lastFrame.getData());
                String value = strBytes.replace(",", ";").replace("[", "").replace("]", "");
                message.addLast(value);
            }

            msgQueue.stream().forEach(s -> messages.add(s.toString()));
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

    private void performTasks() {
        currentTask = this.planResult.getTasks().poll();

        switch (currentTask.getTaskType()) {
            case REQ_SUBSCRIBERS:
                Payload.ReqTopicSubscriptionsPayload payload = new Payload.ReqTopicSubscriptionsPayload(currentTask.getTopic());
                ZMsg msg = PayloadKt.payloadToZMsg(payload, kryo, currentTask.getServer());
                msg.send(pipe);
                break;
            case INJECT_SUBSCRIBERS:

                break;
            case UNSUBSCRIBE:
                break;
            default:
                break;
        }
    }

}
