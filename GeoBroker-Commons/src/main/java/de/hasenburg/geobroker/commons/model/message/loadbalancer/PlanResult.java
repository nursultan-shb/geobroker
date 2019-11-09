package de.hasenburg.geobroker.commons.model.message.loadbalancer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PlanResult {
    private List<Task> tasks = new LinkedList<>();
    private boolean isNewPlan = false;
    private int planNumber = 0;
    private int tasksSize = 0;
    private List<Plan> plan = new ArrayList<>();

    public PlanResult () {

    }

    public PlanResult(List<Task> tasks, boolean isNewPlan, int planNumber, List<Plan> plan) {
        this.tasks = new LinkedList<>(tasks);
        this.tasksSize = this.tasks.size();
        this.isNewPlan = isNewPlan;
        this.planNumber = planNumber;
        this.plan = new ArrayList<>(plan);
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks.clear();
        this.tasks.addAll(tasks);
        this.tasksSize = this.tasks.size();
    }

    public boolean isNewPlan() {
        return isNewPlan;
    }

    public void setNewPlan(boolean newPlan) {
        isNewPlan = newPlan;
    }

    public int getPlanNumber() {
        return planNumber;
    }

    public void setPlanNumber(int planNumber) {
        this.planNumber = planNumber;
    }

    public List<Plan> getPlan() {
        return plan;
    }

    public void setPlan(List<Plan> plan) {
        this.plan.clear();
        this.plan.addAll(plan);
    }

    public int getTasksSize() {
        return tasksSize;
    }

}
