package de.uulm.in.vs.grn.vnscp.common.network;

import de.uulm.in.vs.grn.vnscp.common.network.request.VNSCPRequest;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Future;

public abstract class VNSCPInstance {

    protected final Socket socket;
    protected final BufferedReader reader;
    protected final BufferedWriter writer;

    public VNSCPInstance(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public <T> Future<T> sendRequest(VNSCPRequest<T> request) {
        request.
    }

}
