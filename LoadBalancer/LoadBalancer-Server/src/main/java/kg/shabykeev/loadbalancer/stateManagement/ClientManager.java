package kg.shabykeev.loadbalancer.stateManagement;

import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * ClientManager is a class to manage client states.
 * Connect, disconnect, and ping messages are propagated to all brokers. Brokers, in return, send responses/acknowledgements.
 * ClientManager ensures that only a single response/acknowledgement will be returned to a client.
 * For that, it has a map to track whether a client has already received a response.
 *
 * @author Nursultan
 * @version 1.0
 */
public class ClientManager {
    private Logger logger;
    private Map<String, ClientState> clients = new HashMap<>();

    public ClientManager(Logger logger) {
        this.logger = logger;
    }

    /**
     * Adds a client to the map.
     *
     * @param clientId The ID of a client
     */
    public void addClient(String clientId) {
        clients.put(clientId, new ClientState(true));
    }

    /**
     * Removes a client by its ID.
     *
     * @param clientId The ID of a client
     */
    public void removeClient(String clientId) {
        if (clients.get(clientId) != null) {
            clients.remove(clientId);
        }
    }

    /**
     * Checks whether a client exists in a map.
     *
     * @param clientId The ID of a client
     * @return true/false
     */
    public boolean clientExists(String clientId) {
        return clients.get(clientId) != null;
    }

    /**
     * Sets whether is connection acknowledgement is required for a client.
     * If false, then the ack has already been sent.
     *
     * @param clientId         The ID of a client
     * @param isConAckRequired true/false
     */
    public void setIsConAckRequired(String clientId, boolean isConAckRequired) {
        ClientState clientState = clients.get(clientId);
        if (clientState != null) {
            clientState.setConnAckRequired(isConAckRequired);
            clients.put(clientId, clientState);
        }
    }

    /**
     * Checks whether is connection acknowledgement is required for a client.
     * If false, then the ack has already been sent.
     *
     * @param clientId The ID of a client
     */
    public boolean isConAckRequired(String clientId) {
        ClientState clientState = clients.get(clientId);
        return clientState == null ? true : clientState.isConnAckRequired();
    }

    /**
     * Checks whether is a ping response is required for a client.
     * If false, then a response has already been sent.
     *
     * @param clientId The ID of a client
     */
    public boolean isPingRespRequired(String clientId) {
        ClientState clientState = clients.get(clientId);
        return clientState == null ? true : clientState.isPingRespRequired();
    }

    /**
     * Increments a number of pings.
     * A client is to receive exactly the same number of ping responses as a number of its ping requests.
     *
     * @param clientId The ID of a client
     */
    public void incrementPingReq(String clientId) {
        ClientState clientState = clients.get(clientId);
        if (clientState != null) {
            clientState.incrementPingReq();
            clients.put(clientId, clientState);
        }
    }

    /**
     * Increments a number of ping responses.
     * A client is to receive exactly the same number of ping responses as a number of its ping requests.
     *
     * @param clientId The ID of a client
     */
    public void incrementPingResp(String clientId) {
        ClientState clientState = clients.get(clientId);
        if (clientState != null) {
            clientState.incrementPingResp();
            clients.put(clientId, clientState);
        }
    }

    /**
     * Returns a number of connected clients.
     */
    public int getClientsCount() {
        return this.clients.size();
    }
}
