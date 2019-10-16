package kg.shabykeev.loadbalancer.plan.generator;

import kg.shabykeev.loadbalancer.commons.Plan;
import kg.shabykeev.loadbalancer.commons.ServerLoadMetrics;
import kg.shabykeev.loadbalancer.commons.TopicMetrics;
import kg.shabykeev.loadbalancer.commons.ZMsgType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

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
    private HashMap<String, Set<String>> planMap = new HashMap<>();
    private HashMap<String, Set<String>> topicServerMap = new HashMap<>();

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

    private void parseMessages(String metrics) {
        if (metrics.length() <= 3) return;

        clearData();

        ArrayList<ServerLoadMetrics> lmList = new ArrayList<>();
        ArrayList<TopicMetrics> topicPubMessagesList = new ArrayList<>();
        ArrayList<TopicMetrics> topicSubMessagesList = new ArrayList<>();

        metrics = deleteEdgeSymbols(metrics.trim());
        String[] strValues = metrics.split("],");

        for (String strValue : strValues) {
            String value = replaceSpecialCharacters(strValue);
            String[] elements = Arrays.stream(value.split(",")).map(String::trim).toArray(String[]::new);

            if (value.contains(ZMsgType.TOPIC_METRICS.toString())) {
                String server = elements[2];
                ServerLoadMetrics slm = new ServerLoadMetrics(server, elements[0], Double.valueOf(elements[3]));
                lmList.add(slm);

                //parse topic metrics
                if (elements.length > 4) {
                    topicPubMessagesList.addAll(parseTopicMetrics(elements[4], server));
                    topicSubMessagesList.addAll(parseTopicMetrics(elements[5], server));
                }
            }
        }

        serverLoadMetrics.addAll(aggregateServerLoadMetrics(lmList));
        topicPubMessages.addAll(aggregateTopicMetrics(topicPubMessagesList));
        topicSubMessages.addAll(aggregateTopicMetrics(topicSubMessagesList));

        updateTopicServerMap(topicPubMessages);
        updateTopicServerMap(topicSubMessages);
    }

    private void updateTopicServerMap(ArrayList<TopicMetrics> topicMetrics) {
        for (TopicMetrics tm : topicMetrics) {
            Set<String> servers = topicServerMap.get(tm.getTopic());
            if (servers != null) {
                servers.add(tm.getServer());
                topicServerMap.put(tm.getTopic(), servers);
            } else {
                HashSet<String> serverSet = new HashSet<>();
                serverSet.add(tm.getServer());
                topicServerMap.put(tm.getTopic(), serverSet);
            }
        }
    }

    private ArrayList<ServerLoadMetrics> aggregateServerLoadMetrics(ArrayList<ServerLoadMetrics> lmList) {
        ArrayList<ServerLoadMetrics> slAggMetrics = new ArrayList<>();

        Map<String, Optional<ServerLoadMetrics>> lmMap = lmList.stream()
                .collect(groupingBy(ServerLoadMetrics::getServer,
                        Collectors.maxBy(Comparator.comparing(ServerLoadMetrics::getLoad))));

        for (Map.Entry element : lmMap.entrySet()) {
            Object obj = element.getValue();
            if (obj != null) {
                slAggMetrics.add(((Optional<ServerLoadMetrics>) obj).get());
            }
        }

        return slAggMetrics;
    }

    private ArrayList<TopicMetrics> aggregateTopicMetrics(ArrayList<TopicMetrics> tmList) {
        ArrayList<TopicMetrics> topicAggMetrics = new ArrayList<>();

        Map<String, Map<String, Optional<TopicMetrics>>> tmMap = tmList.stream()
                .collect(groupingBy(TopicMetrics::getServer, groupingBy(TopicMetrics::getTopic,
                        Collectors.maxBy(Comparator.comparing(TopicMetrics::getMessagesCount)))));

        for (Map.Entry element : tmMap.entrySet()) {
            Object obj = element.getValue();
            if (obj != null) {
                Map<String, Optional<TopicMetrics>> nestedMap = ((Map) (obj));
                for (Map.Entry nestedElement : nestedMap.entrySet()) {
                    Object nestedObject = nestedElement.getValue();
                    if (nestedObject != null) {
                        topicAggMetrics.add(((Optional<TopicMetrics>) nestedObject).get());
                    }
                }
            }
        }

        return topicAggMetrics;
    }

    private boolean mergePlan(ArrayList<Plan> plans) {
        boolean isDifferent = false;

        for (Plan plan : plans) {
            if (planMap.containsKey(plan.getTopic())) {
                String server = planMap.get(plan.getTopic());
                if (!server.equals(plan.getServer())) {
                    isDifferent = true;
                }
            } else {
                isDifferent = true;
            }

            planMap.put(plan.getTopic(), plan.getServer());
        }

        return isDifferent;
    }

    private ArrayList<Plan> createPlan() {
        List<ServerLoadMetrics> highLoadedServers = serverLoadMetrics.stream().filter
                (x -> x.getLoad() >= SERVER_LOAD_THRESHOLD).collect(Collectors.toList());

        if (highLoadedServers.isEmpty()) {
            return null;
        }

        ArrayList<Plan> newPlan = new ArrayList<>();

        for (ServerLoadMetrics s : highLoadedServers) {
            Set<String> topics = getTopics(s.getServer());

            for (String topic : topics) {
                ArrayList<String> servers = rebalanceTopic(topic, s.getServer());
                servers.forEach(x -> newPlan.add(new Plan(topic, x)));
            }
        }

        return newPlan;
    }

    private Set<String> getTopics(String server){
        Set<String> topics = new HashSet<>();
        for (Map.Entry<String, Set<String>> pair : topicServerMap.entrySet()) {
            if (pair.getValue().contains(server)) {
                topics.add(pair.getKey());
            }
        }

        return topics;
    }

    /**
     * Assigns servers for a topic
     *
     * @param topic  a topic
     * @param server a server that a topic is currently bound to
     * @return a list of assigned servers for a topic
     */
    private ArrayList<String> rebalanceTopic(String topic, String server) {
        ArrayList<String> servers = new ArrayList<>();
        servers.add(server);

        Optional<TopicMetrics> topicPubMetrics = topicPubMessages.stream().filter(s -> s.getServer().equals(server) && s.getTopic().equals(topic)).findFirst();
        Optional<TopicMetrics> topicSubMetrics = topicSubMessages.stream().filter(s -> s.getServer().equals(server) && s.getTopic().equals(topic)).findFirst();

        if (topicPubMetrics.isPresent() && topicSubMetrics.isPresent()) {
            int numberOfPublications = topicPubMetrics.get().getMessagesCount();
            double pRatio = numberOfPublications / topicSubMetrics.get().getMessagesCount();
            if (pRatio > ALL_SUBS_THRESHOLD && numberOfPublications > PUBLICATION_THRESHOLD) {
                int numberOfServers = (int) (pRatio / ALL_SUBS_THRESHOLD);
                ArrayList<String> additionalServers = replicateTopic(numberOfServers, topic, server);
                servers.addAll(additionalServers);
            }
        }

        return servers;
    }

    private ArrayList<String> replicateTopic(int numberOfServers, String topic, String server) {
        if (numberOfServers > MAX_NUMBER_SERVERS) {
            numberOfServers = MAX_NUMBER_SERVERS;
        }

        List<ServerLoadMetrics> servers = serverLoadMetrics.stream().filter(s -> !s.getServer().equals(server)).collect(Collectors.toList());
        Collections.sort(servers, Collections.reverseOrder());

        ArrayList<String> topicServers = new ArrayList<>();
        for (int i = 0; i < numberOfServers; i++) {
            topicServers.add(servers.get(i).getServer());
        }

        return topicServers;
    }

    private ArrayList<TopicMetrics> parseTopicMetrics(String value, String server) {
        ArrayList<TopicMetrics> tmList = new ArrayList<>();

        if (value.trim().length() < 3) {
            return tmList;
        }

        String[] keyValuePairs = value.split("\\|");

        for (String pair : keyValuePairs) {
            String[] entry = pair.split("=");
            TopicMetrics tm = new TopicMetrics();
            tm.setServer(server);
            tm.setTopic(entry[0].trim());
            tm.setMessagesCount(Integer.valueOf(entry[1].trim()));
            tmList.add(tm);
        }

        return tmList;
    }

    private String deleteEdgeSymbols(String str) {
        return str.substring(1, str.length() - 1).trim();
    }

    private String replaceSpecialCharacters(String str) {
        return str.replace("[", "").replace("]", "");
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
