package kg.shabykeev.loadbalancer.plan.messageProcessor;

import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.Payload;
import de.hasenburg.geobroker.commons.model.message.PayloadKt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.LinkedList;

public class MessageProcessor {
    private static final Logger logger = LogManager.getLogger();
    private KryoSerializer kryo = new KryoSerializer();
    private LinkedList<ZMsg> msgQueue = new LinkedList<>();

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
        msgQueue.add(msg);
        logger.info("added to queue" + msg);
    }

    public void sendPlan(){
        ZMsg msg = ZMsg.recvMsg(this.pairSocket);
        msg.send(this.pipe);
    }

    public void sendMetrics(){
        if (msgQueue.size() > 0) {
            String metrics = msgQueue.toString();
            msgQueue.clear();

            Payload.MetricsBulkAnalyzePayload payload = new Payload.MetricsBulkAnalyzePayload(metrics);
            ZMsg msg = PayloadKt.payloadToZMsg(payload, kryo, this.pairSocket.getLastEndpoint());
            msg.send(this.pairSocket);
            logger.info("queue is empty");
        }
    }
}
