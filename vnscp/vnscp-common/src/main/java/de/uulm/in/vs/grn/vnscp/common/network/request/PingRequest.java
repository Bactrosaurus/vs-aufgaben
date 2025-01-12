package de.uulm.in.vs.grn.vnscp.common.network.request;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class PingRequest extends VNSCPRequest {

    public PingRequest() {
        super(RequestType.PING);
    }

    @Override
    void sendToVNSCP(OutputStream outputStream) throws IOException {
        outputStream.write("PING VNSCP/1.0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

}
