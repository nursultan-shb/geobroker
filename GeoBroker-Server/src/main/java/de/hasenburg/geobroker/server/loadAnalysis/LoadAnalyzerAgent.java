package de.hasenburg.geobroker.server.loadAnalysis;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZThread;

/**
 * LoadAnalyzerAgent is a background agent task that runs as an attached thread, talking to its parent over a pipe socket.
 * It requests metrics from GeoBroker and sends notifications to ping Load Balancer.
 * For that, it polls its two sockets and processes incoming messages.
 * Additionally, through the dealer socket it can get a command from PlanCreator related to the migration procedure.
 *
 * @author Nursultan
 * @version 1.0
 */
public class LoadAnalyzerAgent implements ZThread.IAttachedRunnable {
    private Long lastUtilizationRequestTime = System.currentTimeMillis();
    private Long lastLoadBalancerPingTime = 0L;

    @Override
    public void run(Object[] args, ZContext ctx, ZMQ.Socket pipe)
    {
        String planCreatorAddress = args[0].toString();
        String brokerAddress = args[1].toString();
        String loadBalancerAddress = args[2].toString();
        String instanceId = args[3].toString();
        boolean isAwsDeployment = Boolean.parseBoolean(args[4].toString());

        LoadAnalyzer loadAnalyzer = new LoadAnalyzer(ctx, pipe, brokerAddress, loadBalancerAddress,
                planCreatorAddress, instanceId, isAwsDeployment);

        ZMQ.Poller poller = ctx.createPoller(2);
        poller.register(loadAnalyzer.pipe, ZMQ.Poller.POLLIN);
        poller.register(loadAnalyzer.dealer, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {

            if (System.currentTimeMillis() - lastLoadBalancerPingTime > 30*1000 ){
                lastLoadBalancerPingTime = System.currentTimeMillis();
                loadAnalyzer.sendPing();
            }

            if (System.currentTimeMillis() - lastUtilizationRequestTime > 30*1000 ){
                lastUtilizationRequestTime = System.currentTimeMillis();
                loadAnalyzer.requestUtilization();
            }

            int rc = poller.poll(1000);
            if (rc == -1)
                break; //  Context has been shut down

            if (poller.pollin(0))
                loadAnalyzer.handlePipeMessage();

            if (poller.pollin(1))
                loadAnalyzer.handleDealerMessage();
        }

     loadAnalyzer.destroy();
    }
}
