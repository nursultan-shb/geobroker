package ms.shabykeev.loadbalancer.plan.server;

import de.hasenburg.geobroker.commons.Utility;
import de.hasenburg.geobroker.commons.communication.ZMQProcessManager;
import ms.shabykeev.loadbalancer.common.server.Configuration;
import ms.shabykeev.loadbalancer.common.server.IServerLogic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.concurrent.atomic.AtomicBoolean;

public class PlanServerLogic implements IServerLogic {
    private static final Logger logger = LogManager.getLogger();
    private ZMQProcessManager processManager;
    private Configuration config;

    @Override
    public void loadConfiguration(Configuration configuration) {
        this.config = configuration;
    }

    @Override
    public void initializeFields() {

        processManager = new ZMQProcessManager();
    }

    @Override
    public void startServer(){
        ZMQProcess_PlanServer zmqProcess = new ZMQProcess_PlanServer(config.getAddress(),
                config.getFrontendPort(), config.getBackendPort(), config.getServerId(), config.getPlanGenerationDelay());
        processManager.submitZMQProcess(config.getServerId(), zmqProcess);     ;

        logger.info(String.format("Started a plan creator server successfully on a frontend port: %d and a backend port: %d",
                config.getFrontendPort(), config.getBackendPort()));
    }

    @Override
    public void serverIsRunning() {
        AtomicBoolean keepRunning = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> keepRunning.set(false)));

        while (keepRunning.get()) {
            Utility.sleepNoLog(200000, 0);
        }
    }

    @Override
    public void cleanUp() {
        processManager.tearDown(2000);
        logger.info("Tear down completed");
    }
}
