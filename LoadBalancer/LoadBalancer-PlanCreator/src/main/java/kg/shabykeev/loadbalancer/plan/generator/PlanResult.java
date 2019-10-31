package kg.shabykeev.loadbalancer.plan.generator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanResult {
    private List<Task> tasks;
    private boolean isNewPlan = false;
    private int planNumber = 0;
    private Map<String, String> plan = new HashMap<>();

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        Collections.copy(this.tasks, tasks);
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

    public Map<String, String> getPlan() {
        return plan;
    }

    public void setPlan(Map<String, String> plan) {
        plan.clear();
        plan.putAll(plan);
    }

}
