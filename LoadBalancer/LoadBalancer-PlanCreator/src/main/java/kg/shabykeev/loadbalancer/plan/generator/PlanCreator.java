package kg.shabykeev.loadbalancer.plan.generator;

import de.hasenburg.geobroker.commons.model.message.Payload;
import de.hasenburg.geobroker.commons.model.message.loadbalancer.*;
import kg.shabykeev.loadbalancer.commons.ServerLoadMetrics;
import kg.shabykeev.loadbalancer.plan.util.MessageParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * PlanCreator is a class to manage the plan creation.
 * It checks whether CPU usages of cluster nodes are above SERVER_LOAD_THRESHOLD. If there is one, it
 * creates a set of migration tasks where to transfer a topic with most published messages to the least loaded node.
 *
 * @author Nursultan
 * @version 1.0
 */
public class PlanCreator {
    private static final Logger logger = LogManager.getLogger();
    private static final Double SERVER_LOAD_THRESHOLD = 60D;

    private Integer planNumber = 0;
    private ArrayList<ServerLoadMetrics> serverLoadMetrics = new ArrayList<>();

    /**
     * A number of publications per topic
     */
    private ArrayList<TopicMetrics> topicPubMessages = new ArrayList<>();

    /**
     * A number of subscribers per topic
     */
    private ArrayList<TopicMetrics> topicSubMessages = new ArrayList<>();

    /**
     * Final mapping between a topic and a server
     */
    private HashMap<String, String> planMap = new HashMap<>();

    /**
     * Temporary mapping between a topic and a server
     */
    private HashMap<String, String> topicServerMap = new HashMap<>();

    private List<Task> tasks = new LinkedList<>();

    /**
     * Creates a plan based on incoming metrics from brokers.
     *
     * @param metrics ServerLoadMetrics and TopicMetrics of GeoBroker
     * @return PlanResult object
     */
    public PlanResult createPlan(List<String> metrics) {
        PlanResult result = new PlanResult();

        if (metrics.size() > 0) {
            parseMessages(metrics);
            ArrayList<Plan> newPlans = getPlan();
            if (newPlans.size() > 0) {
                boolean isNew = mergePlan(newPlans) || tasks.size() > 0; //there might be completely new high load topics that do no exist in planMap yet

                if (isNew) {
                    result.setNewPlan(true);
                    result.setPlanNumber(planNumber++);
                    result.setTasks(tasks);
                }
            }

            result.setPlan(convertToPlanList(planMap));
        }

        return result;
    }

    /**
     * Checks whether a new plan is different from the old one.
     *
     * @param plans ArrayList of new plans
     * @return whether a new plan differs from the old one.
     */
    private boolean mergePlan(ArrayList<Plan> plans) {
        boolean isNew = false;

        for (Plan plan : plans) {
            if (planMap.containsKey(plan.getTopic())) {
                String server = planMap.get(plan.getTopic());
                if (!server.equals(plan.getServer())) {
                    isNew = true;
                }
            } else {
                isNew = true;
            }

            planMap.put(plan.getTopic(), plan.getServer());
        }

        return isNew;
    }

    private void parseMessages(List<String> messages) {
        clearData();

        Metrics metrics = MessageParser.parseMessage(messages);
        serverLoadMetrics.addAll(metrics.getServerLoadMetrics());
        topicPubMessages.addAll(metrics.getTopicPubMetrics());
    }

    /**
     * Creates a new plan.
     *
     * @return ArrayList of Plan objects.
     */
    private ArrayList<Plan> getPlan() {
        ArrayList<Plan> newPlans = new ArrayList<>();
        if (topicPubMessages.size() == 0) {
            return newPlans;
        }

        logger.info("Plan creation has been started");
        ServerLoadMetrics leastLm = getLeastLoadedServer();

        for (ServerLoadMetrics slm : serverLoadMetrics) {
            if (slm.getLoad() >= SERVER_LOAD_THRESHOLD) {
                TopicMetrics tm = getMostLoadedTopic(slm.getServer());
                if (tm != null) {
                    createMigrationTasks(tm.getTopic(), slm.getLocalLoadAnalyzer(), leastLm.getLocalLoadAnalyzer());
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
     *
     * @return ServerLoadMetrics of the least loaded server
     */
    private ServerLoadMetrics getLeastLoadedServer() {
        ServerLoadMetrics slm = serverLoadMetrics.stream().min(Comparator.comparing(ServerLoadMetrics::getLoad)).get();
        return slm;
    }

    /**
     * Returns the most loaded topic
     *
     * @return TopicMetrics of the most loaded server
     */
    private TopicMetrics getMostLoadedTopic(String server) {
        TopicMetrics tm = topicPubMessages.stream()
                .filter(s -> s.getServer().equals(server))
                .max(Comparator.comparing(TopicMetrics::getMessagesCount)).orElse(null);

        return tm;
    }

    private List<Plan> convertToPlanList(Map<String, String> planMap) {
        List<Plan> plan = new ArrayList<>();
        planMap.forEach((k, v) -> plan.add(new Plan(k, v)));
        return plan;
    }

    private Payload.PlanPayload getPlanPayload() {
        List<Plan> planList = convertToPlanList(planMap);

        return new Payload.PlanPayload(planList);
    }

    private void clearData() {
        topicServerMap.clear();
        topicPubMessages.clear();
        topicSubMessages.clear();
        serverLoadMetrics.clear();
        tasks.clear();
    }

    private void createMigrationTasks(String topic, String serverSource, String serverDestination) {
        String uuid = UUID.randomUUID().toString();
        tasks.add(new Task(UUID.randomUUID().toString(), 1, topic, serverSource, uuid, TaskType.REQ_SUBSCRIBERS, TaskStatus.CREATED));
        tasks.add(new Task(UUID.randomUUID().toString(), 2, topic, serverDestination, uuid, TaskType.INJECT_SUBSCRIBERS, TaskStatus.CREATED));
        tasks.add(new Task(UUID.randomUUID().toString(), 3, topic, serverSource, uuid, TaskType.UNSUBSCRIBE, TaskStatus.CREATED));
    }
}
