package kg.shabykeev.loadbalancer.stateManagement;

public class ClientState {
    private boolean isConnAckRequired;
    private boolean isPingRespRequired;

    public ClientState(boolean isConnAckRequired) {
        this.isConnAckRequired = isConnAckRequired;
    }

    public boolean isConnAckRequired() {
        return isConnAckRequired;
    }

    public void setConnAckRequired(boolean connAckRequired) {
        isConnAckRequired = connAckRequired;
    }

    public boolean isPingRespRequired() {
        return isPingRespRequired;
    }

    public void setPingRespRequired(boolean pingRespRequired) {
        isPingRespRequired = pingRespRequired;
    }

}
