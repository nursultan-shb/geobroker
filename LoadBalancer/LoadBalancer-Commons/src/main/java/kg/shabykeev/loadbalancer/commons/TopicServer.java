package kg.shabykeev.loadbalancer.commons;

public class TopicServer {
    private String topic;
    private String server;

    public TopicServer(String topic, String server) {
        this.topic = topic;
        this.server = server;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

}
