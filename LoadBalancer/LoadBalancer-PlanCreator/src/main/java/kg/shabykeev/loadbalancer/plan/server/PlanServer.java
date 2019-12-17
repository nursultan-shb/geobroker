package kg.shabykeev.loadbalancer.plan.server;


import kg.shabykeev.loadbalancer.commons.server.Configuration;
import kg.shabykeev.loadbalancer.commons.server.IServerLogic;
import kg.shabykeev.loadbalancer.commons.server.ServerLifeCycle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class PlanServer {
    private static final Logger logger = LogManager.getLogger();

    public PlanServer() {

    }

    public static void main(String[] args) {
        Configuration configuration;

        if (args.length > 0) {
            configuration = Configuration.readConfiguration(args[0]);
        } else {
            configuration = Configuration.readInternalConfiguration("configuration.toml");
        }

        IServerLogic logic = new PlanServerLogic();
        ServerLifeCycle lifecycle = new ServerLifeCycle(logic);
        lifecycle.run(configuration);
    }
}
