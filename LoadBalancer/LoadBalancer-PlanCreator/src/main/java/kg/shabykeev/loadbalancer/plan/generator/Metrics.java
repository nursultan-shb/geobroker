package kg.shabykeev.loadbalancer.plan.generator;

import kg.shabykeev.loadbalancer.commons.ServerLoadMetrics;
import kg.shabykeev.loadbalancer.commons.TopicMetrics;

import java.util.ArrayList;

public class Metrics {
    private ArrayList<ServerLoadMetrics> serverLoadMetrics = new ArrayList<>();
    private ArrayList<TopicMetrics> topicPubMetrics = new ArrayList<>();

    public ArrayList<ServerLoadMetrics> getServerLoadMetrics() {
        return serverLoadMetrics;
    }

    public ArrayList<TopicMetrics> getTopicPubMetrics() {
        return topicPubMetrics;
    }

    public Metrics(ArrayList<ServerLoadMetrics> serverLoadMetrics, ArrayList<TopicMetrics> topicMetrics) {
        this.serverLoadMetrics.addAll(serverLoadMetrics);
        this.topicPubMetrics.addAll(topicMetrics);
    }
}
