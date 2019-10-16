package ms.shabykeev.loadbalancer.plan.server;

import de.hasenburg.geobroker.commons.communication.ZMQControlUtility;
import de.hasenburg.geobroker.commons.communication.ZMQProcess;
import ms.shabykeev.loadbalancer.common.ZMsgType;
import ms.shabykeev.loadbalancer.plan.generator.GeneratorAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.*;

import java.util.*;

public class ZMQProcess_PlanServer extends ZMQProcess {

    private static final Logger logger = LogManager.getLogger();

    private String frontendAddress = "tcp://127.0.0.1:6061";
    private String backendAddress = "tcp://127.0.0.1:6062";
    private Set<String> loadBalancers = new HashSet<>();

    private ZContext context;
    private ZMQ.Socket frontend;
    private ZMQ.Socket backend;
    private ZMQ.Socket metrics_pipe;
    private ZFrame plan;

    // socket indices
    private final int FRONTEND_INDEX = 0;
    private final int BACKEND_INDEX = 1;
    private final int METRICS_PIPE_INDEX = 2;

    private Integer planGenerationDelay = 0;



    ZMQProcess_PlanServer(String address, int frontendPort, int backendPort, String serverId, Integer planGenerationDelay) {
        super(getServerIdentity(serverId));
        this.frontendAddress = address + ":" + frontendPort;
        this.backendAddress = address + ":" + backendPort;
        this.planGenerationDelay = planGenerationDelay;
    }

    @Override
    protected void processZMsg(int socketIndex, ZMsg msg) {
        switch (socketIndex) {
            case BACKEND_INDEX:
                handleBackendMessages(msg);
                break;
            case FRONTEND_INDEX:
                handleFrontendMessages(msg);
                break;
            case METRICS_PIPE_INDEX:
                handleMetricsPipeMessage(msg);
                break;
            default:
                logger.error("Cannot process message for socket at index {}, as this index is not known.", socketIndex);
        }
    }


    @Override
    protected List<ZMQ.Socket> bindAndConnectSockets(ZContext context) {
        ZMQ.Socket[] socketArray = new ZMQ.Socket[3];

        frontend = context.createSocket(SocketType.ROUTER);
        frontend.setHWM(10000);
        frontend.setIdentity(frontendAddress.getBytes(ZMQ.CHARSET));
        frontend.bind(frontendAddress);
        frontend.setSendTimeOut(1);
        socketArray[FRONTEND_INDEX] = frontend;

        backend = context.createSocket(SocketType.ROUTER);
        backend.setHWM(10000);
        backend.setIdentity(backendAddress.getBytes(ZMQ.CHARSET));
        backend.bind(backendAddress);
        backend.setSendTimeOut(1);
        socketArray[BACKEND_INDEX] = backend;

        GeneratorAgent queueAgent = new GeneratorAgent();
        Object[] args = new Object[0];
        metrics_pipe = ZThread.fork(context, queueAgent, args);
        socketArray[METRICS_PIPE_INDEX] = metrics_pipe;

        return Arrays.asList(socketArray);
    }

    private void handleBackendMessages(ZMsg msg){
        forwardToMetricsPipe(msg);
    }

    private void handleFrontendMessages(ZMsg msg){
        String sender = msg.popString();
        String msgType = msg.popString();

        if (msgType.equals(ZMsgType.REG_LOAD_BALANCER.toString())){
            loadBalancers.add(sender);

            ZMsg ack = new ZMsg();
            ack.add(sender);
            ack.add(ZMsgType.ACKREG_LOAD_BALANCER.toString());
            if (plan != null){
                ack.add(plan);
            }
            ack.send(frontend);
            msg.destroy();
        }
    }

    private void forwardToMetricsPipe(ZMsg msg){
        msg.send(metrics_pipe);
    }

    private void handleMetricsPipeMessage(ZMsg msg){
        String msgType = msg.getFirst().toString();

        if (msgType.equals(ZMsgType.PLAN.toString())){
            for(String lbId: loadBalancers){
                msg.push(lbId);
                msg.send(frontend);
            }
        }
    }

    @Override
    protected void utilizationCalculated(double utilization) {

    }

    @Override
    protected void shutdownCompleted() {
        logger.info("Shut down ZMQProcess_Server {}", getServerIdentity(identity));
    }

    public static String getServerIdentity(String brokerId) {
        return brokerId + "-server";
    }

    @Override
    protected void processZMQControlCommandOtherThanKill(ZMQControlUtility.ZMQControlCommand zmqControlCommand,
                                                         ZMsg msg) {
        // no other commands are of interest
    }

}
