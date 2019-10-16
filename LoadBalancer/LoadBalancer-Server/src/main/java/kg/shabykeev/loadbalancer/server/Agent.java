package kg.shabykeev.loadbalancer.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZThread;

public class Agent implements ZThread.IAttachedRunnable {
    private static final Logger logger = LogManager.getLogger();
    private StateManager stateManager;

    private final int PIPE_INDEX = 0;
    private final int DEALER_INDEX = 1;
    private Long lastRegistrationCheckTime = 0L;


    @Override
    public void run(Object[] args, ZContext context, ZMQ.Socket pipe) {

        stateManager = new StateManager(context, pipe);
        stateManager.connectSockets();

        ZMQ.Poller poller = context.createPoller(2);
        poller.register(stateManager.pipe, ZMQ.Poller.POLLIN);
        poller.register(stateManager.dealer, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(100);

            if (poller.pollin(PIPE_INDEX)) {
                stateManager.handlePipeMessage();
            }

            if (poller.pollin(DEALER_INDEX)){
                stateManager.handleDealerMessage();
            }

            if (System.currentTimeMillis() - lastRegistrationCheckTime >= 5000 && !stateManager.isRegisteredInPlanCreator()) {
                stateManager.register();
                lastRegistrationCheckTime = System.currentTimeMillis();
            }
        }
    }
}
