package kg.shabykeev.loadbalancer.plan.generator;

import kg.shabykeev.loadbalancer.commons.Plan;
import kg.shabykeev.loadbalancer.commons.ServerLoadMetrics;
import kg.shabykeev.loadbalancer.commons.TopicMetrics;
import kg.shabykeev.loadbalancer.commons.ZMsgType;
import kg.shabykeev.loadbalancer.plan.util.MessageParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class Generator extends Thread {

    private static final Logger logger = LogManager.getLogger();

    private ZContext ctx;
    public ZMQ.Socket pairSocket;

    private Integer planNumber = 0;

    private static final Double SERVER_LOAD_THRESHOLD = 5D;
    private static final Integer ALL_SUBS_THRESHOLD = 15;
    private static final Integer PUBLICATION_THRESHOLD = 1000;
    private static final Integer MAX_NUMBER_SERVERS = 4;

    /**
     * A number of publications per topic
     */
    private ArrayList<TopicMetrics> topicPubMessages = new ArrayList<>();
    /**
     * A number of subscribers per topic
     */
    private ArrayList<TopicMetrics> topicSubMessages = new ArrayList<>();

    private ArrayList<ServerLoadMetrics> serverLoadMetrics = new ArrayList<>();
    /**
     * Final mapping between a topic and a server
     */
    private HashMap<String, String> planMap = new HashMap<>();
    /**
     * Temporary mapping between a topic and a server
     */
    private HashMap<String, String> topicServerMap = new HashMap<>();

    public Generator(ZContext context, String socketAddress) {
        this.ctx = context;
        Thread.currentThread().setName("plan-generator");

        pairSocket = ctx.createSocket(SocketType.PAIR);
        pairSocket.bind(socketAddress);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String msg = pairSocket.recvStr();
                logger.info("Generator starts the job");
                managePlan(msg);
            } catch (Exception ex) {
                logger.error(ex);
            }

        }
    }

    public void managePlan(String metrics) {
        if (metrics.length() > 3) {
            parseMessages(metrics);
            ArrayList<Plan> newPlans = createPlan();
            if (newPlans != null) {
                boolean isDifferent = mergePlan(newPlans);

                if (isDifferent) {
                    logger.info("sending plan " + planNumber);
                    sendPlan();
                    planNumber++;
                }
            }
        }
    }

    public void sendPlan() {
        ZMsg msg = new ZMsg();
        msg.add(ZMsgType.PLAN.toString());
        msg.add(convertPlanToString());
        msg.send(pairSocket);
    }

    private void parseMessages(String message) {
        if (message.length() <= 3) return;

        clearData();

        Metrics metrics = MessageParser.parseMessage(message);
        serverLoadMetrics.addAll(metrics.getServerLoadMetrics());
        topicPubMessages.addAll(metrics.getTopicPubMetrics());
    }

    private boolean mergePlan(ArrayList<Plan> plans){
        boolean isDifferent = false;

        for(Plan plan: plans){
            if (planMap.containsKey(plan.getTopic())){
                String server = planMap.get(plan.getTopic());
                if (!server.equals(plan.getServer())){
                    isDifferent = true;
                }
            }
            else {
                isDifferent = true;
            }

            planMap.put(plan.getTopic(), plan.getServer());
        }

        return isDifferent;
    }

    private ArrayList<Plan> createPlan() {
        ArrayList<Plan> newPlans = new ArrayList<>();
        if (topicPubMessages.size() == 0) {
            return newPlans;
        }

        ServerLoadMetrics leastLm = getLeastLoadedServer();

        for (ServerLoadMetrics slm : serverLoadMetrics) {
            if (slm.getLoad() >= SERVER_LOAD_THRESHOLD) {
                TopicMetrics tm = getMostLoadedTopic(slm.getServer());
                if (tm != null) {
                    tm.setServer(leastLm.getServer());
                    tm.setMessagesCount(0);
                }
            }
        }

        topicPubMessages.forEach(s -> newPlans.add(new Plan(s.getTopic(), s.getServer())));
        return newPlans;
    }

    /**
     * Returns the least loaded server
     * @return  ServerLoadMetrics of the least loaded server
     */
    private ServerLoadMetrics getLeastLoadedServer() {
        ServerLoadMetrics slm = serverLoadMetrics.stream()
                .min(Comparator.comparing(ServerLoadMetrics::getLoad)).get();

        return slm;
    }

    /**
     * Returns the most loaded topic
     * @return  TopicMetrics of the most loaded server
     */
    private TopicMetrics getMostLoadedTopic(String server) {
        TopicMetrics tm = topicPubMessages.stream()
                .filter(s -> s.getServer().equals(server))
                .max(Comparator.comparing(TopicMetrics::getMessagesCount)).orElse(null);

        return tm;
    }

    private void clearData() {
        topicServerMap.clear();
        topicPubMessages.clear();
        topicSubMessages.clear();
        serverLoadMetrics.clear();
    }

    private String convertPlanToString() {
        /*
        String listString = plans.stream().map(Plan::toString)
                .collect(Collectors.joining(", "));
        */

        StringBuilder sb = new StringBuilder();
        planMap.forEach((k, v) -> sb.append(String.format(k + "=" + v + "|")));
        String result = sb.length() > 0 ? sb.substring(0, sb.length() - 1).trim() : "";

        return result;
    }

}
