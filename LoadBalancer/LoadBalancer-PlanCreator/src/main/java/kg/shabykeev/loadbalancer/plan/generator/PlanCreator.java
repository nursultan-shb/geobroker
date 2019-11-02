package kg.shabykeev.loadbalancer.plan.generator;

import de.hasenburg.geobroker.commons.model.message.TopicMetrics;
import kg.shabykeev.loadbalancer.commons.Plan;
import kg.shabykeev.loadbalancer.commons.ServerLoadMetrics;
import kg.shabykeev.loadbalancer.plan.util.MessageParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

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

    private List<Task> tasks = new ArrayList<>();

    /**
     * Creates a plan based on incoming metrics
     *
     * @param metrics ServerLoadMetrics and TopicMetrics of GeoBroker
     * @return whether a plan is new and should be broadcasted
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

            result.setPlan(planMap);
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
                    tasks.add(new Task(tm.getTopic(), tm.getServer(), leastLm.getServer(), TaskType.MIGRATE));
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

    private void clearData() {
        topicServerMap.clear();
        topicPubMessages.clear();
        topicSubMessages.clear();
        serverLoadMetrics.clear();
        tasks.clear();
    }
}
