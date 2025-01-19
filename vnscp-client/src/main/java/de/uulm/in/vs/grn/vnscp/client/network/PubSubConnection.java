package de.uulm.in.vs.grn.vnscp.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PubSubConnection implements AutoCloseable {

    private final Socket socket;
    private final BufferedReader reader;
    private volatile boolean running = true; // Flag to control the listener thread
    private final ExecutorService eventThreads = Executors.newCachedThreadPool();

    public PubSubConnection(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public interface EventListener {
        void onPubSubMessage(VNSCPPacket message);
    }

    public void listen(EventListener listener) {
        eventThreads.submit(() -> {
            try {
                String line;
                StringBuilder rawMessage = new StringBuilder();
                while (running && (line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        VNSCPPacket event = VNSCPPacket.parse(rawMessage.toString());
                        listener.onPubSubMessage(event);
                        rawMessage.setLength(0); // Clear the buffer for the next message
                    } else {
                        rawMessage.append(line).append("\r\n");
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error reading Pub/Sub connection: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void close() throws IOException {
        running = false;
        eventThreads.shutdown();
        socket.close();
    }

}