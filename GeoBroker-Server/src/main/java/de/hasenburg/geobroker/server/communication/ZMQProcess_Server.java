package de.hasenburg.geobroker.server.communication;

import de.hasenburg.geobroker.commons.communication.ZMQControlUtility;
import de.hasenburg.geobroker.commons.communication.ZMQProcess;
import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.Payload;
import de.hasenburg.geobroker.commons.model.message.PayloadKt;
import de.hasenburg.geobroker.server.loadAnalysis.LoadAnalyzerAgent;
import kg.shabykeev.loadbalancer.commons.ZMsgType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.*;
import org.zeromq.ZMQ.Socket;

import java.util.Arrays;
import java.util.List;

class ZMQProcess_Server extends ZMQProcess {

    private static final Logger logger = LogManager.getLogger();
    private KryoSerializer kryo = new KryoSerializer();

    // Address and port of server frontend
    private String ip;
    private int port;

    // socket indices
    private final int FRONTEND_INDEX = 0;
    private final int BACKEND_INDEX = 1;

    private final int PIPE_INDEX = 2;

    private double cpuUtilization = 0d;
    private String frontendAddress = "";

    private String loadBalancerAddress = "tcp://127.0.0.1:5559";
    private String planCreatorBackend = "tcp://127.0.0.1:7001";
    private String brokerIdentity = "";

    /**
     * @param brokerId - should be the broker id this server is running on
     */
    ZMQProcess_Server(String ip, int port, String brokerId) {
        super(getServerIdentity(brokerId));
        this.ip = "127.0.0.1";
        this.port = port;
    }

    public static String getServerIdentity(String brokerId) {
        return brokerId + "-server";
    }

    @Override
    protected List<Socket> bindAndConnectSockets(ZContext context) {
        Socket[] socketArray = new Socket[3];

        Socket frontend = context.createSocket(SocketType.ROUTER);
        frontend.setHWM(10000);
        frontendAddress = "tcp://" + ip + ":" + port;

        this.brokerIdentity = "broker-server-1"; //ZHelper.setId(identity, frontend);
        frontend.setIdentity(this.brokerIdentity.getBytes(ZMQ.CHARSET));
        frontend.connect(frontendAddress);
        frontend.setSendTimeOut(1);
        socketArray[FRONTEND_INDEX] = frontend;

        Socket backend = context.createSocket(SocketType.DEALER);
        backend.setHWM(10000);
        backend.bind("inproc://" + identity);
        // backend.setIdentity(identity.getBytes()); TODO test whether we can do this
        backend.setSendTimeOut(1);
        socketArray[BACKEND_INDEX] = backend;

        LoadAnalyzerAgent agent = new LoadAnalyzerAgent();
        Object[] args = new Object[3];
        args[0] = planCreatorBackend;
        args[1] = brokerIdentity;
        args[2] = loadBalancerAddress;
        Socket pipe = ZThread.fork(context, agent, args);
        socketArray[PIPE_INDEX] = pipe;

        return Arrays.asList(socketArray);
    }

    @Override
    protected void processZMQControlCommandOtherThanKill(ZMQControlUtility.ZMQControlCommand zmqControlCommand,
                                                         ZMsg msg) {
        // no other commands are of interest
    }

    @Override
    protected void processZMsg(int socketIndex, ZMsg msg) {
        switch (socketIndex) {
            case BACKEND_INDEX:
                handleBackendMessage(msg);
                break;
            case FRONTEND_INDEX:
                handleFrontendMessage(msg);
                break;
            case PIPE_INDEX:
                handlePipeMessage(msg);
                break;
            default:
                logger.error("Cannot process message for socket at index {}, as this index is not known.", socketIndex);
        }
    }

    private void handleBackendMessage(ZMsg msg) {
        msg.push(frontendAddress);
        if (!msg.send(sockets.get(FRONTEND_INDEX))) {
            logger.warn("Dropping response to client as HWM reached.");
        }

    }

    private void handleFrontendMessage(ZMsg msg) {
        if (msg.size() == 2) { //PINGRESP
            msg.destroy();
            return;
        }

        String lb = msg.popString();
        if (!msg.send(sockets.get(BACKEND_INDEX))) {
            logger.warn("Dropping client request as HWM reached.");
        }
    }

    private void handlePipeMessage(ZMsg msg) {
        ZMsgType msgType = ZMsgType.valueOf(msg.getLast().toString());

        switch (msgType) {
            case TOPIC_METRICS:
                String localLoadAnalyzerId = msg.getFirst().toString();
                ZMsg metricsMsg = getLoadMetrics(localLoadAnalyzerId);
                if (!metricsMsg.send(sockets.get(PIPE_INDEX))) {
                    logger.warn("Dropping client request as HWM reached.");
                }
                break;
            case PINGREQ:
                msg.send(sockets.get(FRONTEND_INDEX));
                break;
            case PLAN_CREATOR_MESSAGE:
                Payload payload = PayloadKt.transformZMsg(msg, kryo);
                if (payload != null) {
                    if (payload instanceof Payload.ReqTopicSubscriptionsPayload) {

                    }
                }
                break;

            default:
                logger.error("Cannot process message for socket at index {}, as this index is not known.", msgType);
        }
    }

    @Override
    protected void utilizationCalculated(double utilization) {
        cpuUtilization = utilization;
        logger.info("Current Utilization is {}%", utilization);
    }

    @Override
    protected void shutdownCompleted() {
        logger.info("Shut down ZMQProcess_Server {}", getServerIdentity(identity));
    }

    private ZMsg getLoadMetrics(String localLoadAnalyzerId) {
        Payload.MetricsPayload payload = new Payload.MetricsPayload(this.brokerIdentity, 80, ResourceMetrics.getPublishedMessages(this.brokerIdentity));
        ZMsg msg = PayloadKt.payloadToZMsg(payload, kryo, localLoadAnalyzerId);
        ResourceMetrics.clear();

        return msg;
    }
}
