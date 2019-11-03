package kg.shabykeev.loadbalancer.plan.server;

import de.hasenburg.geobroker.commons.communication.ZMQControlUtility;
import de.hasenburg.geobroker.commons.communication.ZMQProcess;
import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.Payload;
import de.hasenburg.geobroker.commons.model.message.PayloadKt;
import de.hasenburg.geobroker.commons.model.message.ReasonCode;
import kg.shabykeev.loadbalancer.plan.messageProcessor.MessageProcessorAgent;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ZMQProcess_PlanServer extends ZMQProcess {

    private static final Logger logger = LogManager.getLogger();
    private KryoSerializer kryo = new KryoSerializer();

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
                handlePipeMessage(msg);
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

        MessageProcessorAgent queueAgent = new MessageProcessorAgent();
        Object[] args = new Object[0];
        metrics_pipe = ZThread.fork(context, queueAgent, args);
        socketArray[METRICS_PIPE_INDEX] = metrics_pipe;

        return Arrays.asList(socketArray);
    }

    private void handleBackendMessages(ZMsg msg) {
        msg.send(metrics_pipe);
    }

    private void handleFrontendMessages(ZMsg msg) {
        Pair<String, Payload> pair = PayloadKt.transformZMsgWithId(msg, kryo);
        if (pair != null) {
            Payload payload = pair.getSecond();
            if (payload instanceof Payload.PINGREQPayload) {
                loadBalancers.add(pair.getFirst());

                Payload.PINGRESPPayload respPayload = new Payload.PINGRESPPayload(ReasonCode.Success);
                ZMsg respMsg = PayloadKt.payloadToZMsg(respPayload, kryo, pair.getFirst());

                respMsg.send(frontend);
            }
        }
    }

    private void handlePipeMessage(ZMsg msg) {
        Pair<String, Payload> pair = PayloadKt.transformZMsgWithId(msg, kryo);
        if (pair != null) {
            Payload payload = pair.getSecond();
            if (payload instanceof Payload.PlanPayload) {
                for (String lbId : loadBalancers) {
                    msg.push(lbId);
                    msg.send(frontend);
                }
            } else if (payload instanceof Payload.TopicMigratePayload) {
                String brokerLocalLoadAnalyzerId = pair.getFirst();
                ZMsg migrateMsg = PayloadKt.payloadToZMsg(payload, kryo, brokerLocalLoadAnalyzerId);
                migrateMsg.send(backend);
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
