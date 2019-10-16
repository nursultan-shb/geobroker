package kg.shabykeev.loadbalancer.commons.server;

/**
 * Author: Jonathan Hasenburg
 * Source: https://github.com/MoeweX/geobroker
 * */

public interface IServerLogic {
    void loadConfiguration(Configuration configuration);

    void initializeFields();

    void startServer();

    void serverIsRunning();

    void cleanUp();

}