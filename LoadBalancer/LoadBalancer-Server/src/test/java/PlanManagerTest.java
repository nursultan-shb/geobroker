import kg.shabykeev.loadbalancer.server.PlanManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PlanManagerTest {
    private static final Logger logger = LogManager.getLogger();
    private PlanManager testee = new PlanManager(logger);

    @Test
    public void getServerTest() {
        String broker1 = "broker-1";
        String broker2 = "broker-2";

        testee.addServer(broker1);
        testee.addServer(broker2);

        testee.addPlan("red", broker1);
        testee.addPlan("blue", broker2);

        assertTrue(testee.getServer("red").equals(broker1));

        //test round-robin
        String nextServer = testee.getServer("newTopic1");
        assertTrue(nextServer.equals(broker1));
        nextServer = testee.getServer("newTopic2");
        assertTrue(nextServer.equals(broker2));
        nextServer = testee.getServer("newTopic3");
        assertTrue(nextServer.equals(broker1));
    }
}
