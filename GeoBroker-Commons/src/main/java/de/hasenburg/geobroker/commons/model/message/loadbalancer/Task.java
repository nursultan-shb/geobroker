package de.hasenburg.geobroker.commons.model.message.loadbalancer;

public class Task {

    private Integer orderId;
    private String topic;
    private String server;
    private String groupId;
    private TaskType taskType;
    private TaskStatus taskStatus;

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
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

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public Task(Integer orderId, String topic, String server, String groupId, TaskType taskType, TaskStatus taskStatus) {
        this.orderId = orderId;
        this.topic = topic;
        this.server = server;
        this.groupId = groupId;
        this.taskType = taskType;
        this.taskStatus = taskStatus;
    }

}
