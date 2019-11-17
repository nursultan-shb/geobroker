package kg.shabykeev.loadbalancer.stateManagement;

import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ClientManager {
    private Logger logger;
    private Map<String, ClientState> clients = new HashMap<>();

    public ClientManager (Logger logger) {
        this.logger = logger;
    }

    public void addClient(String clientId) {
        clients.put(clientId, new ClientState(true));
    }

    public void removeClient(String clientId) {
        clients.remove(clientId);
    }

    public void setIsConAckRequired(String clientId, boolean isConAckRequired) {
        ClientState clientState = clients.get(clientId);
        clientState.setConnAckRequired(isConAckRequired);
        clients.put(clientId, clientState);
    }

    public boolean isConAckRequired(String clientId) {
        ClientState clientState = clients.get(clientId);
        return clientState.isConnAckRequired();
    }

    public boolean isPingRespRequired(String clientId) {
        ClientState clientState = clients.get(clientId);
        return clientState.isPingRespRequired();
    }

    public void isSetPingRespRequired(String clientId, boolean isRequired) {
        ClientState clientState = clients.get(clientId);
        clientState.setPingRespRequired(isRequired);
        clients.put(clientId, clientState);
    }
}
