package de.uulm.in.vs.grn.vnscp.common.network.request;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LoginRequest extends VNSCPRequest {

    private final String username;

    public LoginRequest(String username) {
        super(RequestType.LOGIN);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    void sendToVNSCP(OutputStream outputStream) throws IOException {
        outputStream.write("LOGIN VNSCP/1.0\r\n".getBytes(StandardCharsets.UTF_8));
        // TODO: !!! Check if username is valid according to protocol specification !!!
        outputStream.write(("Username: " + this.username + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

}
