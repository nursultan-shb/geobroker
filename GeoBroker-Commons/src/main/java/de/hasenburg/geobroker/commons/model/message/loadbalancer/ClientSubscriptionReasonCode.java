package de.hasenburg.geobroker.commons.model.message.loadbalancer;

import de.hasenburg.geobroker.commons.model.message.ReasonCode;

public class ClientSubscriptionReasonCode {
    private String clientId;
    private ReasonCode reasonCode;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(ReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public ClientSubscriptionReasonCode(String clientId, ReasonCode reasonCode) {
        this.clientId = clientId;
        this.reasonCode = reasonCode;
    }
}