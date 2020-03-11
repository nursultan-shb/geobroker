package de.hasenburg.geobroker.server.loadAnalysis;

import de.hasenburg.geobroker.commons.model.message.Topic;
import de.hasenburg.geobroker.commons.model.message.loadbalancer.TopicMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ResourceMetrics is a thread-safe class used to store topic metrics.
 *
 * @author Nursultan
 * @version 1.0
 */

public class ResourceMetrics {
    static ConcurrentHashMap<Topic, Integer> publishedMessages = new ConcurrentHashMap<>();

    /**
     * Sets a number of published messages for a topic, i.e., topic/number of published messages mapping.
     *
     * @param topic         A topic.
     * @param messagesCount a number of published messages
     */
    public synchronized static void setPublishedMessages(Topic topic, Integer messagesCount) {
        if (publishedMessages.containsKey(topic)) {
            Integer mc = publishedMessages.get(topic);
            publishedMessages.put(topic, mc + messagesCount);
        } else {
            publishedMessages.put(topic, messagesCount);
        }
    }

    /**
     * Gets all topic/published messages mappings as a list of TopicMetrics.
     *
     * @param brokerId The ID of a current broker.
     * @return List of TopicMetrics
     */
    public synchronized static List<TopicMetrics> getPublishedMessages(String brokerId) {
        List<TopicMetrics> tm = new ArrayList<>();
        publishedMessages.forEach((k, v) -> tm.add(new TopicMetrics(brokerId, k.getTopic(), v)));
        return tm;
    }

    /**
     * Clears the topic/published messages map.
     */
    public synchronized static void clear() {
        publishedMessages.clear();
    }
}
