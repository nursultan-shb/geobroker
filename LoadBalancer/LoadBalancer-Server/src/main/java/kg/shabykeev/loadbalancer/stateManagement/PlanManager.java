package kg.shabykeev.loadbalancer.stateManagement;

import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlanManager {
    private Logger logger;
    private HashMap<Integer, String> brokers = new HashMap<>();
    private HashMap<String, String> planMap = new HashMap<>();
    private List<String> brokersList = new ArrayList<>();

    private int nextRoundRobinServerId = 0;
    private int nextServerId = 0;

    public PlanManager(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns a responsible server for a given topic.
     * If it is a new topic, returns a server according to the algorithm.
     *
     * @param topic for which the function returns a responsible server
     * @return server as a String
     */
    public String getServer(String topic) {
        String server = planMap.get(topic);
        if (server != null) {
            return server;
        }

        return getNextServer(topic);
    }

    public void addServer(String server) {
        if (!brokers.containsValue(server)) {
            brokers.put(nextServerId, server);
            brokersList.add(server);
            nextServerId++;

            logger.info("New broker is connected: {}", server);
            logger.info("Total number of brokers: {}", nextServerId);
        }
    }

    public List<String> getBrokers() {
        return brokersList;
    }

    public void addPlan(String topic, String server) {
        planMap.put(topic, server);
    }

    public void printPlan() {
        StringBuilder sb = new StringBuilder();
        planMap.forEach((k, v) -> sb.append(String.format("%s:%s ", k, v)));
        logger.info("Plan: {}", sb.toString());
    }

    private String getNextServer(String topic) {
        String server = brokers.get(nextRoundRobinServerId);
        planMap.put(topic, server);
        logger.info("Default plan insert: {} : {}", topic, server);
        nextRoundRobinServerId = nextRoundRobinServerId == brokers.size() - 1 ? 0 : nextRoundRobinServerId + 1;

        return server;
    }
}
