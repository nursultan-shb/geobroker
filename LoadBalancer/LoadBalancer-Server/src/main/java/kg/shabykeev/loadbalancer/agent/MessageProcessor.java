package kg.shabykeev.loadbalancer.agent;

import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.Payload;
import de.hasenburg.geobroker.commons.model.message.PayloadKt;
import de.hasenburg.geobroker.commons.model.spatial.Location;
import de.hasenburg.geobroker.commons.util.ZHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

public class MessageProcessor {
    private static final Logger logger = LogManager.getLogger();
    private KryoSerializer kryo = new KryoSerializer();

    private ZContext ctx;

    private String planCreatorAddress = "tcp://127.0.0.1:7000";

    public ZMQ.Socket pipe;

    public ZMQ.Socket dealer;

    public MessageProcessor(ZContext ctx, ZMQ.Socket pipe) {
        this.ctx = ctx;
        this.pipe = pipe;
        this.dealer = ctx.createSocket(SocketType.DEALER);
    }

    public void connectSockets() {
        ZHelper.setId("LB-Agent-Dealer", this.dealer);
        this.dealer.connect(planCreatorAddress);
    }

    public void handlePipeMessage() {
    }

    public void handleDealerMessage() {
        ZMsg dealerMessage = ZMsg.recvMsg(dealer);
        ZMsg msg = dealerMessage.duplicate();
        Payload payload = PayloadKt.transformZMsg(dealerMessage, kryo);
        if (payload != null) {
            if (payload instanceof Payload.PINGRESPPayload) {

            } else if (payload instanceof Payload.PlanPayload) {
                msg.send(pipe);
            }
        }
    }

    public void ping() {
        Payload.PINGREQPayload payload = new Payload.PINGREQPayload(Location.random());
        ZMsg msg = PayloadKt.payloadToZMsg(payload, kryo, null);
        msg.send(dealer);
    }
}
