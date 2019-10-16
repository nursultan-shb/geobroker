package de.hasenburg.geobroker.server.loadAnalysis;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZThread;

public class LoadAnalyzerAgent implements ZThread.IAttachedRunnable {
    private Long lastUtilizationRequestTime = 0L; //seconds
    private Long lastLoadBalancerPingTime = 0L;

    @Override
    public void run(Object[] args, ZContext ctx, ZMQ.Socket pipe)
    {
        String planCreatorAddress = args[0].toString();
        String brokerAddress = args[1].toString();
        String loadBalancerAddress = args[2].toString();

        LoadAnalyzer loadAnalyzer = new LoadAnalyzer(ctx, pipe, brokerAddress, loadBalancerAddress);
        loadAnalyzer.dealer.connect(planCreatorAddress);

        ZMQ.Poller poller = ctx.createPoller(2);
        poller.register(loadAnalyzer.pipe, ZMQ.Poller.POLLIN);
        poller.register(loadAnalyzer.dealer, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {

            if (System.currentTimeMillis() - lastUtilizationRequestTime > 10*1000 ){
                lastUtilizationRequestTime = System.currentTimeMillis();
                loadAnalyzer.requestUtilization();
            }

            if (System.currentTimeMillis() - lastLoadBalancerPingTime > 10*1000 ){
                lastLoadBalancerPingTime = System.currentTimeMillis();
                loadAnalyzer.sendPing();
            }

            int rc = poller.poll(1000);
            if (rc == -1)
                break; //  Context has been shut down

            if (poller.pollin(0))
                loadAnalyzer.handlePipeMessage();

            if (poller.pollin(1))
                loadAnalyzer.handleDealerMessage();
        }
    }
}
