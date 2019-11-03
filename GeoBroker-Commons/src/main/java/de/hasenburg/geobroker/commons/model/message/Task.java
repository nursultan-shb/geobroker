package de.hasenburg.geobroker.commons.model.message;

public class Task {
    private String topic;
    private String serverSource;
    private String serverDestination;
    private TaskType taskType;
    private boolean isDone = false;

    public Task(String topic, String source, String destination, TaskType taskType, boolean isDone) {
        this.topic = topic;
        this.serverSource = source;
        this.serverDestination = destination;
        this.taskType = taskType;
        this.isDone = isDone;
    }

    public String getServerSource() {
        return serverSource;
    }

    public void setServerSource(String serverSource) {
        this.serverSource = serverSource;
    }

    public String getServerDestination() {
        return serverDestination;
    }

    public void setServerDestination(String serverDestination) {
        this.serverDestination = serverDestination;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }
}
