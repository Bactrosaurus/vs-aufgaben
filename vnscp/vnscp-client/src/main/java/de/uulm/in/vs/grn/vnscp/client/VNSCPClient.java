package de.uulm.in.vs.grn.vnscp.client;

import de.uulm.in.vs.grn.vnscp.common.network.VNSCPInstance;
import de.uulm.in.vs.grn.vnscp.common.network.request.VNSCPRequest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Future;

public class VNSCPClient implements VNSCPInstance {

    private final Socket clientSocket;
    private final BufferedReader bufferedReader;
    private final BufferedWriter bufferedWriter;

    public VNSCPClient(String host, int port) throws IOException {
        this.clientSocket = new Socket(host, port);

    }

    @Override
    public <T> Future<T> sendRequest(VNSCPRequest<T> request) {

    }

    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

}