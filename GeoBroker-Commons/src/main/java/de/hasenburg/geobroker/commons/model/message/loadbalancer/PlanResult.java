package de.hasenburg.geobroker.commons.model.message.loadbalancer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PlanResult {
    private LinkedList<Task> tasks = new LinkedList<>();
    private boolean isNewPlan = false;
    private int planNumber = 0;
    private List<Plan> plan = new ArrayList<>();

    public PlanResult () {

    }

    public PlanResult(List<Task> tasks, boolean isNewPlan, int planNumber, List<Plan> plan) {
        this.tasks = new LinkedList<>(tasks);
        this.isNewPlan = isNewPlan;
        this.planNumber = planNumber;
        this.plan = plan;
    }

    public LinkedList<Task> getTasks() {
        return tasks;
    }

    public void setTasks(LinkedList<Task> tasks) {
        this.tasks.clear();
        this.tasks.addAll(tasks);
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
        plan.clear();
        plan.addAll(plan);
    }
}
