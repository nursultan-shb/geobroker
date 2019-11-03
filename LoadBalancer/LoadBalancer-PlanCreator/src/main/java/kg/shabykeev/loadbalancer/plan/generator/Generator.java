package kg.shabykeev.loadbalancer.plan.generator;

import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.*;
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

    PlanCreator planCreator = new PlanCreator();

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
                    Payload payload = pair.getSecond();

                    if (payload instanceof Payload.MetricsAnalyzePayload) {
                        processMetricsAnalyzePayload((Payload.MetricsAnalyzePayload) payload);
                    } else {
                        logger.error("Cannot process message with a payload {}, as this type is not known.", payload);
                    }
                }

            } catch (Exception ex) {
                logger.error(ex);
            }
        }
    }

    private void processMetricsAnalyzePayload(Payload.MetricsAnalyzePayload payload) {
        PlanResult planResult = planCreator.createPlan(payload.getMetrics());
        Payload.PlanResultPayload resultPayload = new Payload.PlanResultPayload(planResult);
        ZMsg msg = PayloadKt.payloadToZMsg(resultPayload, kryo, null);

        msg.send(pairSocket);
    }
}
