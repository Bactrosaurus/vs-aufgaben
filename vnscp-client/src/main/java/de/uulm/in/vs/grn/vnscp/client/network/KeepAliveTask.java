package de.uulm.in.vs.grn.vnscp.client.network;

import de.uulm.in.vs.grn.vnscp.client.VNSCPClient;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class KeepAliveTask {

    public void startTask(VNSCPClient client) {
        Timer timer = new Timer(true);

        // 10 minutes
        int timeoutMs = 600000;

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    VNSCPPacket pingMessage = new VNSCPPacket(VNSCPPacket.PacketType.PING);
                    client.getCommandConnection().send(pingMessage);
                    VNSCPPacket pongResponse = client.getCommandConnection().receive();

                    if (pongResponse.getPacketType() == VNSCPPacket.PacketType.PONG) {
                        System.out.println("PONG received. Users online: " + pongResponse.getField("Usernames"));
                    } else if (pongResponse.getPacketType() == VNSCPPacket.PacketType.EXPIRED) {
                        System.out.println("Session expired. Logging in again.");
                        client.login();
                    }
                } catch (IOException e) {
                    System.err.println("Error during PING: " + e.getMessage());
                }
            }
        }, timeoutMs / 2, timeoutMs / 2); // Ping every 5 minutes
    }

}
