package de.uulm.in.vs.grn.vnscp.server.network;

import de.uulm.in.vs.grn.vnscp.server.VNSCPServer;
import de.uulm.in.vs.grn.vnscp.server.util.EventUtil;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {

    private final Socket commandSocket;
    private final VNSCPServer server;
    private BufferedReader commandReader;
    private BufferedWriter commandWriter;
    private String username;
    private boolean loggedIn = false;
    private boolean sessionAlive = true;
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private final AtomicLong lastActiveTime = new AtomicLong(System.currentTimeMillis());

    public ClientHandler(Socket commandSocket, VNSCPServer server) {
        this.commandSocket = commandSocket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            commandReader = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));
            commandWriter = new BufferedWriter(new OutputStreamWriter(commandSocket.getOutputStream()));

            String line;
            StringBuilder rawMessage = new StringBuilder();
            while (sessionAlive && (line = commandReader.readLine()) != null) {
                if (line.isEmpty()) {
                    try {
                        VNSCPPacket request = VNSCPPacket.parse(rawMessage.toString());
                        handleRequest(request);
                        rawMessage.setLength(0); // Clear the buffer for the next message
                    } catch (IllegalArgumentException e) {
                        rawMessage.setLength(0);
                        handleError("Invalid message format or version.");
                    }
                } else {
                    rawMessage.append(line).append("\r\n");
                }
            }
        } catch (IOException e) {
            logger.warning(username + " disconnected.");
        } finally {
            forceLogout();
        }
    }

    private void handleRequest(VNSCPPacket request) throws IOException {
        lastActiveTime.set(System.currentTimeMillis());

        switch (request.getPacketType()) {
            case LOGIN -> handleLogin(request);
            case SEND -> handleSend(request);
            case PING -> handlePing();
            case BYE -> handleBye();
            default -> handleError("Invalid command.");
        }
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z0-9]{3,15}$");
    }

    private void handleError(String message) {
        if (message != null) {
            sendResponse(VNSCPPacket.PacketType.ERROR, "Date", EventUtil.getCurrentDate(), "Reason", message);
        }
    }

    private void handleLogin(VNSCPPacket request) {
        username = request.getField("Username");

        // Check if username is valid according to protocol spec
        if (username == null || !isValidUsername(username)) {
            handleError("Invalid username.");
            return;
        }

        long id = EventUtil.getNewId();

        if (!server.registerUser(username, this, id)) {
            handleError("Already logged in.");
        } else {
            loggedIn = true;
            sendResponse(
                    VNSCPPacket.PacketType.LOGGEDIN,
                    "Id", String.valueOf(id),
                    "Date", EventUtil.getCurrentDate()
            );
            logger.info(username + " logged in.");
        }
    }

    private void handleSend(VNSCPPacket request) {
        if (!loggedIn) {
            handleError("Not logged in.");
            return;
        }

        if (server.getInactiveUsers().contains(username)) {
            sendResponse(VNSCPPacket.PacketType.EXPIRED, "Date", EventUtil.getCurrentDate());
            return;
        }

        long id = EventUtil.getNewId();

        String message = request.getField("Text");
        if (message.length() > 512) {
            handleError("Message too long.");
        } else {
            server.broadcastMessage(username, message, id);
            sendResponse(VNSCPPacket.PacketType.SENT, "Id", String.valueOf(id), "Date", EventUtil.getCurrentDate());
        }
    }

    private void handlePing() {
        if (!loggedIn) {
            handleError("Not logged in.");
            return;
        }

        if (server.getInactiveUsers().contains(username)) {
            sendResponse(VNSCPPacket.PacketType.EXPIRED, "Date", EventUtil.getCurrentDate());
            return;
        }

        sendResponse(VNSCPPacket.PacketType.PONG, "Date", EventUtil.getCurrentDate(), "Usernames", String.join(",", server.getActiveUsers()));
    }

    private void handleBye() {
        if (loggedIn) {
            long id = EventUtil.getNewId();

            if (server.getInactiveUsers().contains(username)) {
                sendResponse(VNSCPPacket.PacketType.EXPIRED, "Date", EventUtil.getCurrentDate());
                return;
            }

            logout(id);
            sendResponse(
                    VNSCPPacket.PacketType.BYEBYE,
                    "Id", String.valueOf(id),
                    "Date", EventUtil.getCurrentDate()
            );
            close();
        }
    }

    private void logout(long id) {
        this.sessionAlive = false;
        if (loggedIn) {
            server.unregisterUser(username, id);
            loggedIn = false;
            logger.info(username + " logged out.");
        }
    }

    private void close() {
        try {
            commandReader.close();
            commandReader.close();
            commandWriter.close();
            commandSocket.close();
        } catch (IOException ignored) {
        }
    }

    public boolean isInactive() {
        return System.currentTimeMillis() - lastActiveTime.get() > 600_000; // 10 minute timeout
    }

    public void forceLogout() {
        logout(EventUtil.getNewId());
        close();
    }

    private void sendResponse(VNSCPPacket.PacketType packetType, String... fields) {
        try {
            VNSCPPacket response = new VNSCPPacket(packetType);
            for (int i = 0; i < fields.length; i += 2) {
                response.addField(fields[i], fields[i + 1]);
            }
            commandWriter.write(response.serialize(VNSCPPacket.PacketTarget.TO_CLIENT));
            commandWriter.flush();
        } catch (IOException ignored) {
        }
    }

}
