package kg.shabykeev.loadbalancer.server;


import de.hasenburg.geobroker.commons.communication.ZMQControlUtility;
import de.hasenburg.geobroker.commons.communication.ZMQProcess;
import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.Payload;
import de.hasenburg.geobroker.commons.model.message.PayloadKt;
import kg.shabykeev.loadbalancer.commons.ZMsgType;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ZMQProcess_LoadBalancer extends ZMQProcess {

    private static final Logger logger = LogManager.getLogger();

    // Address and port of server frontend
    private String ip;
    private int frontend_port;
    private int backend_port;
    private ZMQ.Socket state_pipe;

    // socket indices
    private final int FRONTEND_INDEX = 0;
    private final int BACKEND_INDEX = 1;
    private final int STATE_PIPE_INDEX = 2;

    private String planCreatorAddress;

    private HashSet<String> brokers = new HashSet<>();
    private HashMap<String, String> planMap = new HashMap<>();
    private Boolean isDefaultPlanActive = true;

    private KryoSerializer kryo = new KryoSerializer();

    ZMQProcess_LoadBalancer(String identity, String ip, int frontendPort, int backendPort, String planCreatorAddress) {
        super(identity);
        this.ip = ip;
        this.frontend_port = frontendPort;
        this.backend_port = backendPort;
        this.planCreatorAddress = planCreatorAddress;

        planMap.put("red", "broker-server-1");
        planMap.put("rose", "broker-server-1");
        planMap.put("blue", "broker-server-2");
        planMap.put("ocean", "broker-server-1");
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
        state_pipe = ZThread.fork(context, agent, args);
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

    private void handleFrontendMessage(ZMsg msg) {
        ZMsg msgCopy = msg.duplicate();
        Pair<String, Payload> pair = PayloadKt.transformZMsgWithId(msg, kryo);
        if (pair != null) {
            Payload payload = pair.getSecond();
            if (payload instanceof Payload.SUBSCRIBEPayload) {
                String subTopic = ((Payload.SUBSCRIBEPayload) payload).getTopic().getTopic();
                msgCopy.push(planMap.get(subTopic));
                msgCopy.send(sockets.get(BACKEND_INDEX));
            }
            else if (payload instanceof Payload.PUBLISHPayload){
                String pubTopic = ((Payload.PUBLISHPayload) payload).getTopic().getTopic();
                msgCopy.push(planMap.get(pubTopic));
                msgCopy.send(sockets.get(BACKEND_INDEX));
            } else {
                for (String broker : brokers) {
                    ZMsg m = msgCopy.duplicate();
                    m.push(broker);
                    m.send(sockets.get(BACKEND_INDEX));
                }
            }
        }
    }

    private void handleBackendMessage(ZMsg msg) {
        if (msg.size() == 2) {
            String sender = msg.popString();
            brokers.add(sender);

            ZMsg reply = new ZMsg();
            reply.add(sender);
            reply.add(ZMsgType.PINGRESP.toString());
            reply.send(sockets.get(BACKEND_INDEX));
            msg.destroy();
            return;
        }

        String broker = msg.popString();
        if (!msg.send(sockets.get(FRONTEND_INDEX))) {
            logger.warn("Dropping response to client as HWM reached.");
        }
    }

    private void handlePipeMessage(ZMsg msg) {
        updatePlan(msg);
    }

    private void updatePlan(ZMsg msg) {
        String strPlan = msg.popString();
        if (strPlan.length() > 3) {
            planMap.clear();

            logger.info("\nReceived a new plan update(s)");

            for (String el: strPlan.split("\\|")){
                String[] values = el.split("=");
                planMap.put(values[0].trim(), values[1].trim());
                logger.info(el);
            }
        }
    }

    //region shutdownCompleted
    @Override
    protected void shutdownCompleted() {
        logger.info("Shut down ZMQProcess_Server {}", "");
    }
    //endregion

    @Override
    protected void utilizationCalculated(double utilization) {
    }

    @Override
    protected void processZMQControlCommandOtherThanKill(ZMQControlUtility.ZMQControlCommand zmqControlCommand,
                                                         ZMsg msg) {
        // no other commands are of interest
    }
}
