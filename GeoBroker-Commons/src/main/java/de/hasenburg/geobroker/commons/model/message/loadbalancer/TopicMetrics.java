package de.hasenburg.geobroker.commons.model.message.loadbalancer;

public class TopicMetrics {

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getMessagesCount() {
        return messagesCount;
    }

    public void setMessagesCount(Integer messagesCount) {
        this.messagesCount = messagesCount;
    }

    public TopicMetrics(String server, String topic, Integer messagesCount) {
        this.server = server;
        this.topic = topic;
        this.messagesCount = messagesCount;
    }

    private String server;
    private String topic;
    private Integer messagesCount;
}