package de.uulm.in.vs.grn.vnscp.common.network.response;

public abstract class VNSCPResponse {

    protected final ResponseType response;

    protected VNSCPResponse(ResponseType response) {
        this.response = response;
    }

    public enum ResponseType {
        LOGGED_IN, SENT, PONG, EXPIRED, ERROR, BYE
    }

    public ResponseType getResponse() {
        return response;
    }
}
