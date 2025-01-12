package de.uulm.in.vs.grn.vnscp.common.network.request;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ByeRequest extends VNSCPRequest {

    public ByeRequest() {
        super(RequestType.BYE);
    }

    @Override
    void sendToVNSCP(OutputStream outputStream) throws IOException {
        outputStream.write("BYE VNSCP/1.0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

}
