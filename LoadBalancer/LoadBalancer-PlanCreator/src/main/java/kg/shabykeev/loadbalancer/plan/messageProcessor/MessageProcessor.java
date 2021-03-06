package kg.shabykeev.loadbalancer.plan.messageProcessor;

import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.Payload;
import de.hasenburg.geobroker.commons.model.message.PayloadKt;
import de.hasenburg.geobroker.commons.model.message.Subscription;
import de.hasenburg.geobroker.commons.model.message.loadbalancer.PlanResult;
import de.hasenburg.geobroker.commons.model.message.loadbalancer.Task;
import kg.shabykeev.loadbalancer.commons.ZMsgType;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * MessageProcessor is a class to manage message processing of MessageProcessorAgent.
 *
 * @author Nursultan
 * @version 1.0
 */
public class MessageProcessor {
    private static final Logger logger = LogManager.getLogger();
    private KryoSerializer kryo = new KryoSerializer();
    private LinkedList<ZMsg> msgQueue = new LinkedList<>();
    private Task currentTask;
    private PlanResult planResult;
    private LinkedList<Task> tasks = new LinkedList<>();
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

    /**
     * Processes Plan Creator messages coming to the pipe socket, i.e., topic metrics and acknowledgements of migration tasks.
     */
    public void processPipeMessage() {
        ZMsg msg = ZMsg.recvMsg(this.pipe);
        ZMsg msgCopy = msg.duplicate();
        Pair<String, Payload> pair = PayloadKt.transformZMsgWithId(msg, kryo);
        if (pair != null) {
            String localLoadAnalyzerId = pair.getFirst();
            Payload payload = pair.getSecond();
            if (payload instanceof Payload.MetricsPayload && !isMigrationOn) {
                msgQueue.add(msgCopy);
                logger.info("added to queue" + msgCopy);
            } else if (payload instanceof Payload.ReqSubscriptionsAckPayload ||
                    payload instanceof Payload.InjectSubscriptionsAckPayload ||
                    payload instanceof Payload.UnsubscribeTopicAckPayload) {
                //TODO check reason codes
                performTasks(payload);
            }
        }

    }

    /**
     * Processes Generator messages coming to the pair socket, i.e., plan updates.
     * If a plan update contains a migration procedure, MessageProcessor start the migration before releasing a plan.
     */
    public void processPairSocketMessage() {
        ZMsg msg = ZMsg.recvMsg(this.pairSocket);
        Payload payload = PayloadKt.transformZMsg(msg, kryo);
        if (payload != null) {
            if (payload instanceof Payload.PlanResultPayload) {
                PlanResult planResult = ((Payload.PlanResultPayload) payload).getPlanResult();

                if (planResult.isNewPlan()) {
                    this.planResult = planResult;

                    if (planResult.getTasks().size() > 0) {
                        isMigrationOn = true;
                        this.tasks.addAll(planResult.getTasks());
                        performTasks(null);
                    } else {
                        releasePlan();
                    }
                }
            }
        }
    }

    /**
     * Sends metrics from a temporary queue to Generator.
     */
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

    private void releasePlan() {
        Payload.PlanPayload payload = new Payload.PlanPayload(planResult.getPlan());
        ZMsg msg = PayloadKt.payloadToZMsg(payload, kryo, null);
        msg.push(ZMsgType.PLAN.toString());

        msg.send(pipe);
        logger.info("Plan " + planResult.getPlanNumber() + " has been released");
        this.planResult = null;
    }

    private void performTasks(Payload payload) {
        currentTask = this.tasks.poll();
        if (currentTask == null) {
            isMigrationOn = false;
            releasePlan();
            return;
        }

        switch (currentTask.getTaskType()) {
            case REQ_SUBSCRIBERS:
                Payload.ReqSubscriptionsPayload reqPayload = new Payload.ReqSubscriptionsPayload(currentTask.getTaskId(), currentTask.getTopic());
                ZMsg migMsg = PayloadKt.payloadToZMsg(reqPayload, kryo, currentTask.getServer());
                migMsg.push(ZMsgType.MIGRATION_TASK.toString());
                migMsg.send(pipe);
                break;
            case INJECT_SUBSCRIBERS:
                List<Subscription> subscriptions = ((Payload.ReqSubscriptionsAckPayload) payload).getSubscriptions();
                Payload.InjectSubscriptionsPayload subPayload = new Payload.InjectSubscriptionsPayload(currentTask.getTaskId(), subscriptions);
                ZMsg injMsg = PayloadKt.payloadToZMsg(subPayload, kryo, currentTask.getServer());
                injMsg.push(ZMsgType.MIGRATION_TASK.toString());
                injMsg.send(pipe);
                break;
            case UNSUBSCRIBE:
                Payload.UnsubscribeTopicPayload unsubPayload = new Payload.UnsubscribeTopicPayload(currentTask.getTaskId(), currentTask.getTopic());
                ZMsg unsubMsg = PayloadKt.payloadToZMsg(unsubPayload, kryo, currentTask.getServer());
                unsubMsg.push(ZMsgType.MIGRATION_TASK.toString());
                unsubMsg.send(pipe);
                break;
            default:
                break;
        }
    }

}
