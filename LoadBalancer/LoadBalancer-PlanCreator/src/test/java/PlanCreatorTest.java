import kg.shabykeev.loadbalancer.plan.generator.PlanCreator;
import kg.shabykeev.loadbalancer.plan.generator.PlanResult;
import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZMsg;

public class PlanCreatorTest {

    @Test
    public void testOnlyIncomingServerLoadMetrics() {
        String message = "[[ local-load-analyzer 9AB1EBAC-DFBB0C48, TOPIC_METRICS, broker-server-2, 0.0,  ], [ local-load-analyzer 2E2F29A6-B5E43D79, TOPIC_METRICS, broker-server-1, 80,  ], [ local-load-analyzer 9AB1EBAC-DFBB0C48, TOPIC_METRICS, broker-server-2, 0.0,  ], [ local-load-analyzer 2E2F29A6-B5E43D79, TOPIC_METRICS, broker-server-1, 80,  ]]";
        PlanCreator planCreator = new PlanCreator();
        PlanResult planResult = planCreator.createPlan(message);

        Assert.assertFalse(planResult.isNewPlan());
    }

    @Test
    public void testPlanCreation() {
        ZMsg msg1 = new ZMsg();
        msg1.add("lla_1");
        msg1.add("TOPIC_METRICS");
        msg1.add("broker1");
        msg1.add("10");
        msg1.add("red=100|blue=200");


        ZMsg msg2 = new ZMsg();
        msg2.add("lla_2");
        msg2.add("TOPIC_METRICS");
        msg2.add("broker2");
        msg2.add("80");
        msg2.add("red=100|blue=200|sanders=1000");

        String metrics = msg1.toString();
        PlanCreator planCreator = new PlanCreator();


    }
}
