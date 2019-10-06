package de.hasenburg.geobroker.commons.loadAnalysis;

import de.hasenburg.geobroker.commons.model.message.ControlPacketType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.Random;

public class LoadAnalyzer {
    private static final Logger logger = LogManager.getLogger();

    private ZContext ctx;      //  Own context
    public ZMQ.Socket pipe;     //  Socket to talk back to application
    public ZMQ.Socket dealer;
    private String identity = "local-load-analyzer";
    private static Random rand = new Random(System.currentTimeMillis());

    private String brokerAddress = "";
    private String loadBalancerAddress = "";

    protected LoadAnalyzer(ZContext ctx, ZMQ.Socket pipe, String brokerAddress, String loadBalancerAddress)
    {
        this.ctx = ctx;
        this.pipe = pipe;
        dealer = ctx.createSocket(SocketType.DEALER);
        setIdentity(dealer);
        Thread.currentThread().setName(identity);
        this.brokerAddress = brokerAddress;
        this.loadBalancerAddress = loadBalancerAddress;
    }

    public void handlePipeMessage(){
        ZMsg msg = ZMsg.recvMsg(pipe);
        ControlPacketType msgType = ControlPacketType.valueOf(msg.getFirst().toString());

        if (msgType == ControlPacketType.TOPIC_METRICS) {
            msg.send(dealer);
        }
    }

    public void handleDealerMessage(){
        ZMsg msg = ZMsg.recvMsg(dealer);
        String command = msg.popString();
    }

    public void requestUtilization(){
        ZMsg msgRequest = new ZMsg();
        msgRequest.add(ControlPacketType.TOPIC_METRICS.toString());
        msgRequest.send(pipe);
    }

    private void setIdentity(ZMQ.Socket socket){
        String id = String.format(identity + " %04X-%04X", rand.nextInt(), rand.nextInt());
        socket.setIdentity(id.getBytes(ZMQ.CHARSET));
    }

    public void sendPing(){
        ZMsg msg = new ZMsg();
        msg.add(loadBalancerAddress.getBytes(ZMQ.CHARSET));
        msg.add(ControlPacketType.PINGREQ.toString());
        msg.send(pipe);
    }
}
