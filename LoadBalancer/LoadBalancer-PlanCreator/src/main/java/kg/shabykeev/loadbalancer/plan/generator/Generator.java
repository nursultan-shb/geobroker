package kg.shabykeev.loadbalancer.plan.generator;

import kg.shabykeev.loadbalancer.commons.ZMsgType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

public class Generator extends Thread {

    private static final Logger logger = LogManager.getLogger();

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
        PlanCreator planCreator = new PlanCreator();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                ZMsg msg = ZMsg.recvMsg(pairSocket);
                ZMsgType msgType = ZMsgType.valueOf(msg.popString());
                switch (msgType) {
                    case TOPIC_METRICS:
                        logger.info("Plan-Generator starts the plan creation");
                        PlanResult planResult = planCreator.createPlan(msg.toString());
                        if (planResult.isNewPlan()) {
                            if (planResult.getTasks().size() > 0) {
                                migrate(planResult);
                            } else {
                                releasePlan(planResult);
                            }
                        }

                        break;
                    case ACK_TOPIC_MIGRATION:
                        updateTask(msg);

                        boolean allDone = !this.planResult.getTasks().stream().anyMatch(s->s.isDone() == false);
                        if (allDone) {
                            releasePlan(this.planResult);
                        }
                        break;
                    default:
                        logger.error("Cannot process message for a type {}, as this type is not known.", msgType);
                        break;
                }

            } catch (Exception ex) {
                logger.error(ex);
            }

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
            ZMsg msg = new ZMsg();
            msg.add(ZMsgType.TOPIC_MIGRATION.toString());
            msg.add(task.getServerSource());
            msg.add(task.getTopic());
            msg.add(task.getServerDestination());

            msg.send(pairSocket);
        }

        logger.info("Migration tasks for the plan " + planResult.getPlanNumber() + " have been sent");
    }

    private void updateTask(ZMsg msg){
        String server = msg.popString();
        String topic = msg.popString();

        for (Task task : planResult.getTasks()) {
            if (task.getTopic().equals(topic) && task.getServerSource().equals(server)) {
                task.setDone(true);
                break;
            }
        }

        msg.destroy();
    }
}
