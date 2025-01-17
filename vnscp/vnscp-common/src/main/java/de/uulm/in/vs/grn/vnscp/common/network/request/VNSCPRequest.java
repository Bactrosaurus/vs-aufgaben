package de.uulm.in.vs.grn.vnscp.common.network.request;

import java.io.IOException;

public abstract class VNSCPRequest {

    protected final RequestType requestType;

    protected VNSCPRequest(RequestType requestType) {
        this.requestType = requestType;
    }

    abstract byte[] requestData();

    public RequestType getRequestType() {
        return requestType;
    }

    public enum RequestType {
        LOGIN, SEND, PING, BYE
    }

}
