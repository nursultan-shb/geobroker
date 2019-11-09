package de.hasenburg.geobroker.server.loadAnalysis;

import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.Payload;
import de.hasenburg.geobroker.commons.model.message.PayloadKt;
import de.hasenburg.geobroker.commons.model.spatial.Location;
import kg.shabykeev.loadbalancer.commons.ZMsgType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.Random;

public class LoadAnalyzer {
    private static final Logger logger = LogManager.getLogger();
    private KryoSerializer kryo = new KryoSerializer();

    private ZContext ctx;      //  Own context
    public ZMQ.Socket pipe;     //  Socket to talk back to application
    public ZMQ.Socket dealer;
    private String baseIdentity = "lla";
    private String identity;
    private static Random rand = new Random(System.currentTimeMillis());

    private String brokerAddress = "";
    private String loadBalancerAddress = "";
    private String planCreatorAddress = "";

    protected LoadAnalyzer(ZContext ctx, ZMQ.Socket pipe, String brokerAddress, String loadBalancerAddress, String planCreatorAddress) {
        this.brokerAddress = brokerAddress;
        this.loadBalancerAddress = loadBalancerAddress;
        this.planCreatorAddress = planCreatorAddress;

        this.ctx = ctx;
        this.pipe = pipe;
        dealer = ctx.createSocket(SocketType.DEALER);
        identity = setIdentity(dealer);
        dealer.connect(planCreatorAddress);

        Thread.currentThread().setName(baseIdentity);
    }

    public void handlePipeMessage() {
        //metrics
        ZMsg msg = ZMsg.recvMsg(pipe);
        msg.send(dealer);
    }

    public void handleDealerMessage() {
        ZMsg msg = ZMsg.recvMsg(dealer);
        msg.push(planCreatorAddress);
        msg.push(ZMsgType.PLAN_CREATOR_MESSAGE.toString());
        msg.send(pipe);
    }

    public void requestUtilization() {
        ZMsg msgRequest = new ZMsg();
        msgRequest.add(ZMsgType.TOPIC_METRICS.toString());
        msgRequest.send(pipe);
    }

    private String setIdentity(ZMQ.Socket socket) {
        String id = String.format(baseIdentity + " %04X-%04X", rand.nextInt(), rand.nextInt());
        socket.setIdentity(id.getBytes(ZMQ.CHARSET));
        return id;
    }

    public void sendPing() {
        Payload.PINGREQPayload payload = new Payload.PINGREQPayload(Location.undefined());
        ZMsg msg = PayloadKt.payloadToZMsg(payload, kryo, loadBalancerAddress);
        msg.push(ZMsgType.PINGREQ.toString());
        msg.send(pipe);
    }
}
