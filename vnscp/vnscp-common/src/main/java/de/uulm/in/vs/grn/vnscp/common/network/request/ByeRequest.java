package de.uulm.in.vs.grn.vnscp.common.network.request;

import java.nio.charset.StandardCharsets;

public class ByeRequest extends VNSCPRequest<Boolean> {

    public ByeRequest() {
        super(RequestType.BYE);
    }

    @Override
    byte[] requestData() {
        return "BYE VNSCP/1.0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    }

}
