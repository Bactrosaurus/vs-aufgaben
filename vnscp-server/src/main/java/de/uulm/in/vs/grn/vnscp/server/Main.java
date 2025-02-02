package de.uulm.in.vs.grn.vnscp.server;

public class Main {

    public static void main(String[] args) {
        VNSCPServer server = new VNSCPServer(12345, 12346);
        server.start();
    }

}