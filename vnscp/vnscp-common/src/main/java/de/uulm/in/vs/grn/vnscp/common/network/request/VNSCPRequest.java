package de.uulm.in.vs.grn.vnscp.common.network.request;

import java.io.IOException;
import java.io.OutputStream;

public abstract class VNSCPRequest {

    protected final RequestType requestType;

    protected VNSCPRequest(RequestType requestType) {
        this.requestType = requestType;
    }

    abstract void sendToVNSCP(OutputStream outputStream) throws IOException;

    public RequestType getRequestType() {
        return requestType;
    }

    public enum RequestType {
        LOGIN, SEND, PING, BYE
    }

}
