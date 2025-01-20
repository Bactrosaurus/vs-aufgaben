package de.uulm.in.vs.grn.vnscp.client;

import de.uulm.in.vs.grn.vnscp.client.network.CommandConnection;
import de.uulm.in.vs.grn.vnscp.client.network.KeepAliveTask;
import de.uulm.in.vs.grn.vnscp.client.network.PubSubConnection;
import de.uulm.in.vs.grn.vnscp.client.network.VNSCPPacket;
import de.uulm.in.vs.grn.vnscp.client.ui.ClientWindow;

import java.io.IOException;
import java.util.Arrays;

public class VNSCPClient {

    private final CommandConnection commandConnection;
    private final PubSubConnection pubSubConnection;
    private final KeepAliveTask keepAliveTask;
    private String latestError;
    private final String username;
    private final ClientWindow clientWindow;

    public VNSCPClient(ClientWindow clientWindow, String host, int commandPort, int pubSubPort, String username) throws IOException {
        this.commandConnection = new CommandConnection(host, commandPort);
        this.pubSubConnection = new PubSubConnection(host, pubSubPort);
        this.keepAliveTask = new KeepAliveTask();
        this.latestError = "";
        this.username = username;
        this.clientWindow = clientWindow;
    }

    public boolean login() {
        boolean loggedIn = false;

        // tries to log in 5 times
        for (int i = 0; !loggedIn && i < 5; i++) {
            VNSCPPacket loginRequest = new VNSCPPacket(VNSCPPacket.PacketType.LOGIN);
            loginRequest.addField("Username", username);

            // System.out.println("Sending login request: " + loginRequest.serialize());
            this.commandConnection.send(loginRequest);

            VNSCPPacket loginResponse;
            try {
                loginResponse = this.commandConnection.receive();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // System.out.println("Received response: " + loginResponse.serialize());

            if (loginResponse.getPacketType() == VNSCPPacket.PacketType.LOGGEDIN) {
                System.out.println("Logged in successfully!");
                this.keepAliveTask.startTask(this);
                loggedIn = true;
            } else if (loginResponse.getPacketType() == VNSCPPacket.PacketType.ERROR) {
                String loginFailure = "Login failed: " + loginResponse.getField("Reason");
                System.err.println(loginFailure);
                this.latestError = loginFailure;
            }
        }

        // Set initial ping to get all online users
        VNSCPPacket initialPing = new VNSCPPacket(VNSCPPacket.PacketType.PING);
        this.getCommandConnection().send(initialPing);
        try {
            initialPing = this.getCommandConnection().receive();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] onlineUsers = initialPing.getField("Usernames").split(",");

        // Add connected users to user list in GUI
        Arrays.stream(onlineUsers).forEach(clientWindow.getConnectedUsers()::addElement);

        return loggedIn;
    }

    public void initPubSubListener() {
        // Listen for incoming pub-sub messages
        this.pubSubConnection.listen(message -> {
            if (message.getPacketType() == VNSCPPacket.PacketType.EVENT) {
                clientWindow.displayEvent(message.getField("Description"));
            }

            if (message.getPacketType() == VNSCPPacket.PacketType.MESSAGE) {
                clientWindow.displayMessage(message.getField("Username"), message.getField("Text"));
            }
        });
    }

    public boolean sendMessage(String message) {
        // Make sure message is shorter than 512 bytes as specified in protocol
        if (message.getBytes().length > 512) {
            String error = "Message too long!";
            System.err.println(error);
            this.latestError = error;
            return false;
        }

        try {
            VNSCPPacket sendMessage = new VNSCPPacket(VNSCPPacket.PacketType.SEND);
            sendMessage.addField("Text", message);
            commandConnection.send(sendMessage);

            VNSCPPacket sendResponse = commandConnection.receive();

            if (sendResponse.getPacketType() == VNSCPPacket.PacketType.SENT) {
                System.out.println("Message sent successfully!");
                return true;
            } else if (sendResponse.getPacketType() == VNSCPPacket.PacketType.EXPIRED) {
                System.out.println("Session expired. Logging in again.");
                login();
                return sendMessage(message);
            } else {
                String messageFailure = "Message failed: " + sendResponse.getField("Reason");
                System.err.println(messageFailure);
                this.latestError = messageFailure;
            }
        } catch (IOException e) {
            String sendFailure = "Send failed: " + e.getMessage();
            System.err.println(sendFailure);
            this.latestError = sendFailure;
        }

        return false;
    }

    public boolean logout() {
        try {
            pubSubConnection.close();

            VNSCPPacket byeMessage = new VNSCPPacket(VNSCPPacket.PacketType.BYE);
            commandConnection.send(byeMessage);

            VNSCPPacket byeResponse = commandConnection.receive();
            if (byeResponse.getPacketType() == VNSCPPacket.PacketType.BYEBYE) {
                System.out.println("Logged out successfully!");
                return true;
            }

            commandConnection.close();
        } catch (IOException e) {
            String logoutFailure = "Logout failed: " + e.getMessage();
            System.err.println(logoutFailure);
            this.latestError = logoutFailure;
        }

        return false;
    }

    public String getLatestError() {
        return latestError;
    }

    public PubSubConnection getPubSubConnection() {
        return pubSubConnection;
    }

    public CommandConnection getCommandConnection() {
        return commandConnection;
    }

    public ClientWindow getClientWindow() {
        return clientWindow;
    }

}
