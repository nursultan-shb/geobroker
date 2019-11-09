package de.hasenburg.geobroker.commons.model.message.loadbalancer;

public class Task {

    private String taskId;
    private Integer orderId;
    private String topic;
    private String server;
    private String groupId;
    private TaskType taskType;
    private TaskStatus taskStatus;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

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

    public Task(String taskId, Integer orderId, String topic, String server, String groupId, TaskType taskType, TaskStatus taskStatus) {
        this.taskId = taskId;
        this.orderId = orderId;
        this.topic = topic;
        this.server = server;
        this.groupId = groupId;
        this.taskType = taskType;
        this.taskStatus = taskStatus;
    }

}
