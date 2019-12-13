package kg.shabykeev.loadbalancer.server;


import de.hasenburg.geobroker.commons.communication.ZMQControlUtility;
import de.hasenburg.geobroker.commons.communication.ZMQProcess;
import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.Payload;
import de.hasenburg.geobroker.commons.model.message.PayloadKt;
import de.hasenburg.geobroker.commons.model.message.ReasonCode;
import de.hasenburg.geobroker.commons.model.message.loadbalancer.Plan;
import kg.shabykeev.loadbalancer.agent.Agent;
import kg.shabykeev.loadbalancer.commons.ZMsgType;
import kg.shabykeev.loadbalancer.stateManagement.ClientManager;
import kg.shabykeev.loadbalancer.stateManagement.PlanManager;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.*;

import java.util.Arrays;
import java.util.List;

public class ZMQProcess_LoadBalancer extends ZMQProcess {

    private static final Logger logger = LogManager.getLogger();
    private KryoSerializer kryo = new KryoSerializer();
    private PlanManager planManager = new PlanManager(logger);
    private ClientManager clientManager = new ClientManager(logger);

    // Address and port of server frontend
    private String ip;
    private int frontend_port;
    private int backend_port;
    private ZMQ.Socket state_pipe;

    // socket indices
    private final int FRONTEND_INDEX = 0;
    private final int BACKEND_INDEX = 1;
    private final int STATE_PIPE_INDEX = 2;

    ZMQProcess_LoadBalancer(String identity, String ip, int frontendPort, int backendPort, String planCreatorAddress) {
        super(identity);
        this.ip = ip;
        this.frontend_port = frontendPort;
        this.backend_port = backendPort;
    }

    @Override
    protected List<ZMQ.Socket> bindAndConnectSockets(ZContext context) {
        ZMQ.Socket[] socketArray = new ZMQ.Socket[3];
        String frontendAddress = ip + ":" + frontend_port;
        String backendAddress = ip + ":" + backend_port;

        ZMQ.Socket frontend = context.createSocket(SocketType.ROUTER);
        frontend.setHWM(10000);
        frontend.setIdentity(frontendAddress.getBytes(ZMQ.CHARSET));
        frontend.bind(frontendAddress);
        frontend.setSendTimeOut(1);
        socketArray[FRONTEND_INDEX] = frontend;

        ZMQ.Socket backend = context.createSocket(SocketType.ROUTER);
        backend.setHWM(10000);
        backend.setIdentity(backendAddress.getBytes(ZMQ.CHARSET));
        backend.bind(backendAddress);
        backend.setSendTimeOut(1);
        socketArray[BACKEND_INDEX] = backend;

        Agent agent = new Agent();
        Object[] args = new Object[0];
        state_pipe = ZThread.fork(new ZContext(), agent, args);
        socketArray[STATE_PIPE_INDEX] = state_pipe;

        return Arrays.asList(socketArray);
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
            case STATE_PIPE_INDEX:
                handlePipeMessage(msg);
                break;
            default:
                logger.error("Cannot process message for socket at index {}, as this index is not known.", socketIndex);
        }
    }

    private void handleFrontendMessage(ZMsg frontendMsg) {
        ZMsg msg = frontendMsg.duplicate();
        Pair<String, Payload> pair = PayloadKt.transformZMsgWithId(frontendMsg, kryo);
        if (pair != null) {
            Payload payload = pair.getSecond();
            if (payload instanceof Payload.SUBSCRIBEPayload) {
                String subTopic = ((Payload.SUBSCRIBEPayload) payload).getTopic().getTopic();
                msg.push(planManager.getServer(subTopic));
                msg.send(sockets.get(BACKEND_INDEX));
            } else if (payload instanceof Payload.PUBLISHPayload) {
                String pubTopic = ((Payload.PUBLISHPayload) payload).getTopic().getTopic();
                msg.push(planManager.getServer(pubTopic));
                msg.send(sockets.get(BACKEND_INDEX));
            } else if (payload instanceof Payload.UNSUBSCRIBEPayload) {
                String topic = ((Payload.UNSUBSCRIBEPayload) payload).getTopic().getTopic();
                msg.push(planManager.getServer(topic));
                msg.send(sockets.get(BACKEND_INDEX));
            } else if (payload instanceof Payload.CONNECTPayload) {
                clientManager.addClient(pair.getFirst());
                sendToBrokers(msg);
            } else if (payload instanceof Payload.PINGREQPayload) {
                clientManager.incrementPingReq(pair.getFirst());
                sendToBrokers(msg);
            } else if (payload instanceof Payload.DISCONNECTPayload) {
                sendToBrokers(msg);
            }
        }
    }

    private void handleBackendMessage(ZMsg backendMsg) {
        String brokerServer = backendMsg.popString();

        if (backendMsg.getFirst().toString().equals(ZMsgType.PINGREQ.toString())) {
           addBroker(backendMsg, brokerServer);
           return;
        }

        ZMsg msg = backendMsg.duplicate();
        boolean sendMessage = reduceMessage(backendMsg);

        if (sendMessage) {
            if (!msg.send(sockets.get(FRONTEND_INDEX))) {
                logger.warn("Dropping response to client as HWM reached.");
            }
        } else {
            msg.destroy();
        }
    }

    private void handlePipeMessage(ZMsg msg) {
        updatePlan(msg);
    }

    private void updatePlan(ZMsg msg) {
        Payload payload = PayloadKt.transformZMsg(msg, kryo);
        if (payload instanceof Payload.PlanPayload) {
            List<Plan> planList = ((Payload.PlanPayload) payload).getPlan();

            for (Plan p : planList) {
                planManager.addPlan(p.getTopic(), p.getServer());
            }

            logger.info("New plan updates (size {}) have been accepted", planList.size());
            planManager.printPlan();
        }
    }

    private void sendToBrokers(ZMsg msg) {
        for (String broker : planManager.getBrokers()) {
            ZMsg m = msg.duplicate();
            m.push(broker);
            m.send(sockets.get(BACKEND_INDEX));
        }
    }

    @Override
    protected void shutdownCompleted() {
        logger.info("Shut down ZMQProcess_Server {}", "");
    }


    @Override
    protected void utilizationCalculated(double utilization) {
    }

    @Override
    protected void processZMQControlCommandOtherThanKill(ZMQControlUtility.ZMQControlCommand zmqControlCommand,
                                                         ZMsg msg) {
        // no other commands are of interest
    }

    private void addBroker(ZMsg msg, String brokerServer) {
        planManager.addServer(brokerServer);

        ZMsg reply = PayloadKt.payloadToZMsg(new Payload.PINGRESPPayload(ReasonCode.Success), kryo, brokerServer);
        reply.send(sockets.get(BACKEND_INDEX));
        msg.destroy();
        return;
    }

    private boolean reduceMessage(ZMsg backendMsg) {
        boolean sendMessage = true;

        Pair<String, Payload> pair = PayloadKt.transformZMsgWithId(backendMsg, kryo);
        if (pair != null) {
            Payload payload = pair.getSecond();
            if (payload instanceof Payload.CONNACKPayload) {
                sendMessage = clientManager.isConAckRequired(pair.getFirst());
                if (sendMessage) {
                    clientManager.setIsConAckRequired(pair.getFirst(), false);
                }
            } else if (payload instanceof Payload.PINGRESPPayload) {
                sendMessage = clientManager.isPingRespRequired(pair.getFirst());
                if (sendMessage) {
                    clientManager.incrementPingResp(pair.getFirst());
                }
            } else if (payload instanceof Payload.DISCONNECTPayload) {
                sendMessage = clientManager.clientExists(pair.getFirst());
                if (sendMessage) {
                    clientManager.removeClient(pair.getFirst());
                }
            }
        }

        return sendMessage;
    }
}
