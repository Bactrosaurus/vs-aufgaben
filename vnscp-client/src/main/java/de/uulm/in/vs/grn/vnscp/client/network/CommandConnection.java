package de.uulm.in.vs.grn.vnscp.client.network;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandConnection implements AutoCloseable {

    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final ExecutorService sendThreads = Executors.newCachedThreadPool();
    private volatile boolean running = true;

    public CommandConnection(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void send(VNSCPPacket message) {
        if (!running) return;

        sendThreads.submit(() -> {
            try {
                writer.write(message.serialize());
                writer.flush();
            } catch (IOException e) {
                if (!running) return;
                throw new RuntimeException(e);
            }
        });
    }

    public synchronized VNSCPPacket receive() throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while (!(line = reader.readLine()).isEmpty() && running) {
            response.append(line).append("\r\n");
        }
        return VNSCPPacket.parse(response.toString());
    }

    @Override
    public void close() throws IOException {
        running = false;
        sendThreads.shutdown();
        socket.close();
    }

}