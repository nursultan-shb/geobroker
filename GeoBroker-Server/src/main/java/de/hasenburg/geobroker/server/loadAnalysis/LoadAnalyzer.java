package de.hasenburg.geobroker.server.loadAnalysis;

import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.Payload;
import de.hasenburg.geobroker.commons.model.message.PayloadKt;
import de.hasenburg.geobroker.commons.model.spatial.Location;
import kg.shabykeev.loadbalancer.commons.ZMsgType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;

/**
 * LoadAnalyzer processes messages coming to LoadAnalyzerAgent.
 *
 * @author Nursultan
 * @version 1.0
 */
public class LoadAnalyzer {
    private static final Logger logger = LogManager.getLogger();
    private KryoSerializer kryo = new KryoSerializer();

    private ZContext ctx;      //  Own context
    public ZMQ.Socket pipe;     //  Socket to talk back to application
    public ZMQ.Socket dealer;
    private String baseIdentity = "lla";
    private String identity;
    private static Random rand = new Random(System.currentTimeMillis());

    private String brokerAddress = "";
    private String loadBalancerAddress = "";
    private String planCreatorAddress = "";
    private boolean isAwsDeployment;

    private CloudWatchClient cloudWatchClient;
    private Dimension dimension;

    protected LoadAnalyzer(ZContext ctx, ZMQ.Socket pipe, String brokerAddress,
                           String loadBalancerAddress, String planCreatorAddress, String instanceId, boolean isAwsDeployment) {
        this.brokerAddress = brokerAddress;
        this.loadBalancerAddress = loadBalancerAddress;
        this.planCreatorAddress = planCreatorAddress;
        this.isAwsDeployment = isAwsDeployment;

        this.ctx = ctx;
        this.pipe = pipe;
        dealer = ctx.createSocket(SocketType.DEALER);
        identity = setIdentity(dealer);
        dealer.connect(planCreatorAddress);

        Thread.currentThread().setName(baseIdentity);
        this.dimension = Dimension.builder().name("InstanceId").value(instanceId).build();
        this.cloudWatchClient = CloudWatchClient.builder().build();
        logger.info("Local Load Analyzer started. IsAwsDeployment: {}. Instance_Id: {}", isAwsDeployment, instanceId);
    }

    /**
     * Handles messages from the pipe socket, i.e., topic metrics and directs them to PlanCreator through the dealer socket.
     */
    public void handlePipeMessage() {
        ZMsg msg = ZMsg.recvMsg(pipe);
        msg.send(dealer);
    }

    /**
     * Handles messages from the pipe dealer socket, i.e., commands from PlanCreator related to the topic migration.
     */
    public void handleDealerMessage() {
        ZMsg msg = ZMsg.recvMsg(dealer);
        msg.push(planCreatorAddress);
        msg.push(ZMsgType.PLAN_CREATOR_MESSAGE.toString());
        msg.send(pipe);
    }

    /**
     * Requests topic metrics from GeoBroker.
     */
    public void requestUtilization() {
        ZMsg msgRequest = new ZMsg();
        msgRequest.add(ZMsgType.TOPIC_METRICS.toString());
        if (isAwsDeployment) {
            double cpuLoad = getCpuLoad(1);
            msgRequest.add(String.valueOf(cpuLoad));
        }

        msgRequest.send(pipe);
    }

    /**
     * Sends the ping message to GeoBroker that is to be directed to Load Balancer.
     */
    public void sendPing() {
        Payload.PINGREQPayload payload = new Payload.PINGREQPayload(Location.undefined());
        ZMsg msg = PayloadKt.payloadToZMsg(payload, kryo, loadBalancerAddress);
        msg.push(ZMsgType.PINGREQ.toString());
        msg.send(pipe);
    }

    protected void destroy() {
        this.dealer.setLinger(1);
        this.dealer.close();
        this.pipe.setLinger(1);
        this.pipe.close();
    }

    private String setIdentity(ZMQ.Socket socket) {
        String id = String.format(baseIdentity + " %04X-%04X", rand.nextInt(), rand.nextInt());
        socket.setIdentity(id.getBytes(ZMQ.CHARSET));
        return id;
    }

    private double getCpuLoad(int startFromMinutesBack) {
        GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                .metricName("CPUUtilization").namespace("AWS/EC2").period(60)
                .statistics(Statistic.MAXIMUM)
                .startTime(Instant.ofEpochMilli((new Date(new Date().getTime() - startFromMinutesBack * 60 * 1000)).getTime()))
                .endTime(Instant.ofEpochMilli((new Date()).getTime()))
                .unit(StandardUnit.PERCENT)
                .dimensions(dimension)
                .build();

        GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
        Datapoint datapoint = response.datapoints().stream().max(Comparator.comparing(Datapoint::maximum)).orElse(null);

        double cpuLoad = datapoint == null ? 0 : datapoint.maximum();
        logger.info("CPU Load: {}", cpuLoad);
        return cpuLoad;
    }
}
