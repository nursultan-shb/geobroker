package de.hasenburg.geobroker.server.communication;

import de.hasenburg.geobroker.commons.model.message.Topic;

import java.util.concurrent.ConcurrentHashMap;

public class ResourceMetrics {
    static ConcurrentHashMap<Topic, Integer> publishedMessages = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Topic, Integer> subscriptions = new ConcurrentHashMap<>();

    public synchronized static void setPublishedMessages(Topic topic, Integer messagesCount) {
        if (publishedMessages.containsKey(topic)) {
            Integer mc = publishedMessages.get(topic);
            publishedMessages.put(topic, mc + messagesCount);
        } else {
            publishedMessages.put(topic, messagesCount);
        }
    }

    public synchronized static void addSubscription(Topic topic) {
        if (subscriptions.containsKey(topic)) {
            Integer mc = subscriptions.get(topic);
            subscriptions.put(topic, mc + 1);
        } else {
            subscriptions.put(topic, 1);
        }
    }

    public synchronized static void removeSubscription(Topic topic) {
        if (subscriptions.containsKey(topic)) {
            Integer mc = subscriptions.get(topic);
            if (mc > 0) {
                subscriptions.put(topic, mc - 1);
            }
        }
    }


    public synchronized static String getPublishedMessagesAsString() {
        StringBuilder sb = new StringBuilder();
        publishedMessages.forEach((k, v) -> sb.append(String.format(k + "=" + v + "|")));
        String result = sb.length() > 0 ? sb.substring(0, sb.length() - 1).trim() : "";

        return result;
    }

    public synchronized static String getSubscriptionsAsString() {
        StringBuilder sb = new StringBuilder();
        subscriptions.forEach((k, v) -> sb.append(String.format(k + "=" + v + "|")));
        String result = sb.length() > 0 ? sb.substring(0, sb.length() - 1).trim() : "";

        return result;
    }

    public synchronized static void clear() {
        publishedMessages.clear();
    }
}
