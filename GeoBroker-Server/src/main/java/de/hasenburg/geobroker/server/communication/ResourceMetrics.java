package de.hasenburg.geobroker.server.communication;

import de.hasenburg.geobroker.commons.model.message.Topic;
import de.hasenburg.geobroker.commons.model.message.loadbalancer.TopicMetrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceMetrics {
    static ConcurrentHashMap<Topic, Integer> publishedMessages = new ConcurrentHashMap<>();
    private static final Logger logger = LogManager.getLogger();

    public synchronized static void setPublishedMessages(Topic topic, Integer messagesCount) {
        if (publishedMessages.containsKey(topic)) {
            Integer mc = publishedMessages.get(topic);
            publishedMessages.put(topic, mc + messagesCount);
        } else {
            publishedMessages.put(topic, messagesCount);
        }
    }

    public synchronized static List<TopicMetrics> getPublishedMessages(String brokerId){
        List<TopicMetrics> tm = new ArrayList<>();
        publishedMessages.forEach((k,v) -> tm.add(new TopicMetrics(brokerId, k.getTopic(), v)));
        return tm;
    }


    public synchronized static String getPublishedMessagesAsString() {
        StringBuilder sb = new StringBuilder();
        publishedMessages.forEach((k, v) -> sb.append(String.format(k + "=" + v + "|")));
        String result = sb.length() > 0 ? sb.substring(0, sb.length() - 1).trim() : "";

        return result;
    }

    public synchronized static void clear() {
        publishedMessages.clear();
    }
}
