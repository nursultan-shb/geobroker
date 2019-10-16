package kg.shabykeev.loadbalancer.server;

import kg.shabykeev.loadbalancer.commons.util.ZHelper;
import kg.shabykeev.loadbalancer.commons.ZMsgType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

public class StateManager {
    private static final Logger logger = LogManager.getLogger();

    private ZContext ctx;

    private String planCreatorAddress = "tcp://127.0.0.1:7000";

    public Boolean isRegisteredInPlanCreator() {
        return isRegisteredInPlanCreator;
    }

    private Boolean isRegisteredInPlanCreator = false;

    public ZMQ.Socket pipe;

    public ZMQ.Socket dealer;

    public StateManager(ZContext ctx, ZMQ.Socket pipe){
        this.ctx = ctx;
        this.pipe = pipe;
        this.dealer = ctx.createSocket(SocketType.DEALER);
    }

    public void connectSockets(){
        ZHelper.setId("LB-Agent-Dealer", this.dealer);
        this.dealer.connect(planCreatorAddress);
    }

    public void handlePipeMessage(){
    }

    public void handleDealerMessage(){
        ZMsg msg = ZMsg.recvMsg(dealer);
        ZMsgType msgType = ZMsgType.valueOf(msg.popString());
        switch (msgType){
            case ACKREG_LOAD_BALANCER:
                this.isRegisteredInPlanCreator = true;
                if (msg.size() > 2){
                    msg.send(pipe);
                }

                break;
            case PLAN:
                msg.send(pipe);
                break;
        }
    }

    public void register(){
        ZMsg msg = new ZMsg();
        msg.add(ZMsgType.REG_LOAD_BALANCER.toString());
        msg.send(dealer);
    }
}
