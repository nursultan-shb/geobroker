package de.hasenburg.geobroker.server.loadAnalysis;

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

    private ZContext ctx;      //  Own context
    public ZMQ.Socket pipe;     //  Socket to talk back to application
    public ZMQ.Socket dealer;
    private String baseIdentity = "local-load-analyzer";
    private String identity;
    private static Random rand = new Random(System.currentTimeMillis());

    private String brokerAddress = "";
    private String loadBalancerAddress = "";

    protected LoadAnalyzer(ZContext ctx, ZMQ.Socket pipe, String brokerAddress, String loadBalancerAddress)
    {
        this.ctx = ctx;
        this.pipe = pipe;
        dealer = ctx.createSocket(SocketType.DEALER);
        identity = setIdentity(dealer);
        Thread.currentThread().setName(baseIdentity);
        this.brokerAddress = brokerAddress;
        this.loadBalancerAddress = loadBalancerAddress;
    }

    public void handlePipeMessage(){
        //metrics
        ZMsg msg = ZMsg.recvMsg(pipe);
        msg.send(dealer);
    }

    public void handleDealerMessage(){
        ZMsg msg = ZMsg.recvMsg(dealer);
        msg.send(pipe);
    }

    public void requestUtilization(){
        ZMsg msgRequest = new ZMsg();
        msgRequest.add(this.identity);
        msgRequest.add(ZMsgType.TOPIC_METRICS.toString());
        msgRequest.send(pipe);
    }

    private String setIdentity(ZMQ.Socket socket){
        String id = String.format(identity + " %04X-%04X", rand.nextInt(), rand.nextInt());
        socket.setIdentity(id.getBytes(ZMQ.CHARSET));
        return id;
    }

    public void sendPing(){
        ZMsg msg = new ZMsg();
        msg.add(loadBalancerAddress.getBytes(ZMQ.CHARSET));
        msg.add(ZMsgType.PINGREQ.toString());
        msg.send(pipe);
    }
}
