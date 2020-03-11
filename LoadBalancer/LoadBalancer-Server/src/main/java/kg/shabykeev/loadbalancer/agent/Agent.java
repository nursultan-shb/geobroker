package kg.shabykeev.loadbalancer.agent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZThread;

/**
 * Agent is a background task that runs as an attached thread, talking to its parent over a pipe socket.
 * It communicates with PlanCreator through the dealer socket by sending keep-alive pings to PlanCreator
 * and accepting plan updates.
 * For that, it polls its two sockets and processes incoming messages.
 * @author Nursultan
 * @version 1.0
 */
public class Agent implements ZThread.IAttachedRunnable {
    private static final Logger logger = LogManager.getLogger();
    private MessageProcessor messageProcessor;

    private final int PIPE_INDEX = 0;
    private final int DEALER_INDEX = 1;
    private Long lastPlanCreatorPingTime = 0L;
    private Long lastStatisticsPrintTime = System.currentTimeMillis();

    @Override
    public void run(Object[] args, ZContext context, ZMQ.Socket pipe) {

        messageProcessor = new MessageProcessor(context, pipe);
        messageProcessor.connectSockets();

        ZMQ.Poller poller = context.createPoller(2);
        poller.register(messageProcessor.pipe, ZMQ.Poller.POLLIN);
        poller.register(messageProcessor.dealer, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(100);

            if (poller.pollin(PIPE_INDEX)) {
                messageProcessor.handlePipeMessage();
            }

            if (poller.pollin(DEALER_INDEX)){
                messageProcessor.handleDealerMessage();
            }

            if (System.currentTimeMillis() - lastPlanCreatorPingTime >= 30000) {
                messageProcessor.ping();
                lastPlanCreatorPingTime = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - lastStatisticsPrintTime >= 5*60*1000) {
                messageProcessor.printStatistics();
                lastStatisticsPrintTime = System.currentTimeMillis();
            }
        }

        messageProcessor.destroy();
    }
}
