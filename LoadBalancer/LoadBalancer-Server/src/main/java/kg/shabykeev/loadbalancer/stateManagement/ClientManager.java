package kg.shabykeev.loadbalancer.stateManagement;

import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ClientManager {
    private Logger logger;
    private Map<String, ClientState> clients = new HashMap<>();

    public ClientManager(Logger logger) {
        this.logger = logger;
    }

    public void addClient(String clientId) {
        clients.put(clientId, new ClientState(true));
    }

    public void removeClient(String clientId) {
        if (clients.get(clientId) != null) {
            clients.remove(clientId);
        }
    }

    public boolean clientExists(String clientId) {
        return clients.get(clientId) != null;
    }

    public void setIsConAckRequired(String clientId, boolean isConAckRequired) {
        ClientState clientState = clients.get(clientId);
        if (clientState != null) {
            clientState.setConnAckRequired(isConAckRequired);
            clients.put(clientId, clientState);
        }
    }

    public boolean isConAckRequired(String clientId) {
        ClientState clientState = clients.get(clientId);
        return clientState == null ? true : clientState.isConnAckRequired();
    }

    public boolean isPingRespRequired(String clientId) {
        ClientState clientState = clients.get(clientId);
        return clientState == null ? true : clientState.isPingRespRequired();
    }

    public void incrementPingReq(String clientId) {
        ClientState clientState = clients.get(clientId);
        if (clientState != null) {
            clientState.incrementPingReq();
            clients.put(clientId, clientState);
        }
    }

    public void incrementPingResp(String clientId) {
        ClientState clientState = clients.get(clientId);
        if (clientState != null) {
            clientState.incrementPingResp();
            clients.put(clientId, clientState);
        }
    }

    public int getClientsCount() {
        return this.clients.size();
    }
}
