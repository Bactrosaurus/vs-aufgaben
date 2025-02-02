package de.uulm.in.vs.grn.vnscp.server;

import de.uulm.in.vs.grn.vnscp.server.network.ClientHandler;
import de.uulm.in.vs.grn.vnscp.server.network.PubSubHandler;
import de.uulm.in.vs.grn.vnscp.server.network.VNSCPPacket;
import de.uulm.in.vs.grn.vnscp.server.util.EventUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class VNSCPServer {

    private final int commandPort;
    private final int pubSubPort;
    private final Map<String, ClientHandler> activeUsers = new ConcurrentHashMap<>();
    private final Set<PubSubHandler> pubSubSubscribers = ConcurrentHashMap.newKeySet();
    private final Set<String> inactiveUsers = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static final Logger logger = Logger.getLogger(VNSCPServer.class.getName());

    public VNSCPServer(int commandPort, int pubSubPort) {
        this.commandPort = commandPort;
        this.pubSubPort = pubSubPort;
    }

    public void start() {
        threadPool.execute(this::acceptCommandConnections);
        threadPool.execute(this::acceptPubSubConnections);
    }

    private void acceptCommandConnections() {
        try (ServerSocket commandServer = new ServerSocket(commandPort)) {
            logger.info("Command Server started on port " + commandPort);
            while (true) {
                Socket clientSocket = commandServer.accept();
                threadPool.execute(new ClientHandler(clientSocket, this));
            }
        } catch (IOException e) {
            logger.severe("Command Server encountered an error: " + e.getMessage());
        }
    }

    private void acceptPubSubConnections() {
        try (ServerSocket pubSubServer = new ServerSocket(pubSubPort)) {
            logger.info("Pub/Sub Server started on port " + pubSubPort);
            while (true) {
                Socket pubSubSocket = pubSubServer.accept();
                PubSubHandler subscriber = new PubSubHandler(pubSubSocket, this);
                pubSubSubscribers.add(subscriber);
                threadPool.execute(subscriber);
            }
        } catch (IOException e) {
            logger.severe("Pub/Sub Server encountered an error: " + e.getMessage());
        }
    }

    public synchronized boolean registerUser(String username, ClientHandler handler, long id) {
        if (activeUsers.containsKey(username)) {
            return false;
        }

        activeUsers.put(username, handler);
        inactiveUsers.remove(username);
        broadcastUserEvent(username + " has joined", id);
        scheduleTimeout(username);
        logger.info("User registered: " + username);
        return true;
    }

    public synchronized void unregisterUser(String username, long id) {
        ClientHandler removed = activeUsers.remove(username);
        if (removed != null) {
            broadcastUserEvent(username + " has left", id);
            logger.info("User unregistered: " + username);
        }
    }

    private synchronized void broadcastUserEvent(String eventMessage, long id) {
        for (PubSubHandler user : pubSubSubscribers) {
            user.sendPubSubMessage(
                    VNSCPPacket.PacketType.EVENT,
                    "Id", String.valueOf(id),
                    "Date", EventUtil.getCurrentDate(),
                    "Description", eventMessage
            );
        }
    }

    public synchronized void broadcastMessage(String username, String message, long id) {
        for (PubSubHandler user : pubSubSubscribers) {
            user.sendPubSubMessage(
                    VNSCPPacket.PacketType.MESSAGE,
                    "Id", String.valueOf(id),
                    "Date", EventUtil.getCurrentDate(),
                    "Username", username,
                    "Text", message
            );
        }
        logger.info("Broadcasted message from " + username + ": " + message);
    }

    public synchronized Set<String> getActiveUsers() {
        // Make sure inactive clients are removed
        activeUsers.entrySet().removeIf(entry -> entry.getValue().isInactive());

        return activeUsers.keySet();
    }

    public synchronized Set<String> getInactiveUsers() {
        return inactiveUsers;
    }

    private synchronized void scheduleTimeout(String username) {
        timeoutScheduler.schedule(() -> {
            ClientHandler client = activeUsers.get(username);
            if (client != null && !client.isInactive()) {
                unregisterUser(username, EventUtil.getNewId());
                inactiveUsers.add(username);
                logger.warning("User " + username + " timed out due to inactivity.");
            }
        }, 10, TimeUnit.MINUTES);
    }

    public synchronized void removeSubscriber(PubSubHandler subscriber) {
        pubSubSubscribers.remove(subscriber);
    }

}
