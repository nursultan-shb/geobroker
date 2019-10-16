package kg.shabykeev.loadbalancer.commons.server;

/**
 * Author: Jonathan Hasenburg
 * Source: https://github.com/MoeweX/geobroker
 * */

public class ServerLifeCycle {

    private final IServerLogic serverLogic;

    public ServerLifeCycle(IServerLogic serverLogic) {
        this.serverLogic = serverLogic;
    }

    public void run(Configuration configuration) {
        serverLogic.loadConfiguration(configuration);

        serverLogic.initializeFields();

        serverLogic.startServer();

        serverLogic.serverIsRunning();

        serverLogic.cleanUp();
    }

}
