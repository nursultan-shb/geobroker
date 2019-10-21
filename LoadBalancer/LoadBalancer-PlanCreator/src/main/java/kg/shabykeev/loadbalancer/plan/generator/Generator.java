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

    private PlanCreator planCreator;

    public Generator(ZContext context, String socketAddress) {
        this.ctx = context;
        Thread.currentThread().setName("plan-generator");

        pairSocket = ctx.createSocket(SocketType.PAIR);
        pairSocket.bind(socketAddress);
    }

    @Override
    public void run() {
        planCreator = new PlanCreator();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                String msg = pairSocket.recvStr();
                logger.info("Plan-Generator starts the job");
                boolean isNewPlan = planCreator.createPlan(msg);

                if (isNewPlan) {
                    logger.info("sending plan " + planCreator.planNumber);
                    sendPlan();
                }
            } catch (Exception ex) {
                logger.error(ex);
            }

        }
    }

    public void sendPlan() {
        ZMsg msg = new ZMsg();
        msg.add(ZMsgType.PLAN.toString());
        msg.add(planCreator.getPlanAsString());
        msg.send(pairSocket);
    }

}
