package de.hasenburg.geobroker.commons.model.message;

public class Plan {
    public Plan(String topic, String server){
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

    private String topic;
    private String server;

    @Override
    public String toString(){
        return topic + ":" + server;
    }
}
