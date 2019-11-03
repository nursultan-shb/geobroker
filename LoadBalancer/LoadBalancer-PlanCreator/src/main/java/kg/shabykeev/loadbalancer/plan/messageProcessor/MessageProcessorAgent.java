package kg.shabykeev.loadbalancer.plan.messageProcessor;

import kg.shabykeev.loadbalancer.plan.generator.Generator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZThread;

public class MessageProcessorAgent implements ZThread.IAttachedRunnable {
    private static final Logger logger = LogManager.getLogger();

    // socket indices
    private final int PIPE_INDEX = 0;
    private final int PAIR_SOCKET_INDEX = 1;

    private Long planGenerationDelay = 20*1000L;
    private Long lastPlanGenerationTime = planGenerationDelay; //millis
    private boolean isGeneratorBusy = false;

    private MessageProcessor messageProcessor;

    private static final String PAIR_SOCKET_ADDRESS = "inproc://plan2";

    @Override
    public void run(Object[] args, ZContext context, ZMQ.Socket pipe) {
        messageProcessor = new MessageProcessor(context, pipe, PAIR_SOCKET_ADDRESS);
        Generator generator = new Generator(context, PAIR_SOCKET_ADDRESS);
        generator.start();

        ZMQ.Poller poller = context.createPoller(2);
        poller.register(messageProcessor.pipe, ZMQ.Poller.POLLIN);
        poller.register(messageProcessor.pairSocket, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(100);

            if (poller.pollin(PIPE_INDEX)) {
                messageProcessor.addMessage();
            }

            if (poller.pollin(PAIR_SOCKET_INDEX)) {
                messageProcessor.processPairSocketMessage();
            }

            if ((System.currentTimeMillis() - lastPlanGenerationTime >= planGenerationDelay) ) {
                messageProcessor.sendMetrics();
                lastPlanGenerationTime = System.currentTimeMillis();
            }
        }
    }
}
