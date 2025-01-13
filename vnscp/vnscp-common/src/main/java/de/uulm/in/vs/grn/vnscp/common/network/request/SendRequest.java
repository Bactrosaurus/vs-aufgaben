package de.uulm.in.vs.grn.vnscp.common.network.request;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SendRequest extends VNSCPRequest {

    private final String message;

    public SendRequest(String message) {
        super(RequestType.LOGIN);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    void sendToVNSCP(OutputStream outputStream) throws IOException {
        outputStream.write("SEND VNSCP/1.0\r\n".getBytes(StandardCharsets.UTF_8));
        // TODO: !!! Check if message is valid according to protocol specification !!!
        outputStream.write(("Text: " + this.message + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

}
