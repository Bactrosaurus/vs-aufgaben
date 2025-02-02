package de.uulm.in.vs.grn.vnscp.server.network;

import de.uulm.in.vs.grn.vnscp.server.VNSCPServer;

import java.io.*;
import java.net.*;
import java.util.logging.*;

public class PubSubHandler implements Runnable {

    private final Socket socket;
    private final VNSCPServer server;
    private BufferedWriter writer;
    private static final Logger logger = Logger.getLogger(PubSubHandler.class.getName());

    public PubSubHandler(Socket socket, VNSCPServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            logger.info("New Pub/Sub subscriber connected.");
        } catch (IOException e) {
            logger.warning("Error initializing Pub/Sub connection: " + e.getMessage());
            server.removeSubscriber(this);
        }
    }

    public void sendPubSubMessage(VNSCPPacket.PacketType packetType, String... fields) {
        try {
            VNSCPPacket message = new VNSCPPacket(packetType);
            for (int i = 0; i < fields.length; i += 2) {
                message.addField(fields[i], fields[i + 1]);
            }
            writer.write(message.serialize(VNSCPPacket.PacketTarget.TO_CLIENT));
            writer.flush();
        } catch (IOException e) {
            logger.warning("Pub/Sub connection lost: " + e.getMessage());
            server.removeSubscriber(this);
            close();
        }
    }

    private void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

}

