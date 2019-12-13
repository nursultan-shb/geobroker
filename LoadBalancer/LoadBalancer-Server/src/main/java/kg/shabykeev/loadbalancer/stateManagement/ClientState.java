package kg.shabykeev.loadbalancer.stateManagement;

public class ClientState {
    private boolean isConnAckRequired;

    public int pingReq = 0;
    public int pingResp = 0;

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
        return this.pingReq != this.pingResp;
    }

    public void incrementPingReq() {
        this.pingReq++;
    }

    public void incrementPingResp() {
        this.pingResp++;
    }
}
