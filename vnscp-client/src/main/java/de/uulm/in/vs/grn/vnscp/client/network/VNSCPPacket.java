package de.uulm.in.vs.grn.vnscp.client.network;

import java.util.HashMap;
import java.util.Map;

public class VNSCPPacket {

    public enum PacketType {
        EVENT, MESSAGE, SEND, LOGIN, PING, PONG, BYE, LOGGEDIN, SENT, EXPIRED, ERROR, BYEBYE
    }

    private final PacketType packetType;
    private final Map<String, String> fields = new HashMap<>();

    public VNSCPPacket(PacketType packetType) {
        this.packetType = packetType;
    }

    public PacketType getPacketType() {
        return this.packetType;
    }

    public void addField(String key, String value) {
        fields.put(key, value);
    }

    public String getField(String key) {
        return fields.get(key);
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder(getPacketType().toString() + " VNSCP/1.0\r\n");
        fields.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\r\n"));
        sb.append("\r\n");
        return sb.toString();
    }

    public static VNSCPPacket parse(String message) {
        String[] lines = message.split("\r\n");
        if (lines.length == 0 || lines[0].isEmpty()) {
            throw new IllegalArgumentException("Invalid message format: Empty message");
        }
        VNSCPPacket vnsMessage = getVnscpMessage(lines);

        // Parse fields
        parseMessage(lines, vnsMessage);
        return vnsMessage;
    }

    private static void parseMessage(String[] lines, VNSCPPacket vnsMessage) {
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) break;
            String[] parts = lines[i].split(": ", 2);
            if (parts.length == 2) {
                vnsMessage.addField(parts[0], parts[1]);
            }
        }
    }

    private static VNSCPPacket getVnscpMessage(String[] lines) {
        String[] firstLineParts = lines[0].split(" ");
        VNSCPPacket vnsMessage;

        if (firstLineParts.length == 2) {
            // Format: <COMMAND> VNSCP/1.0
            if ("VNSCP/1.0".equals(firstLineParts[1])) {
                vnsMessage = new VNSCPPacket(PacketType.valueOf(firstLineParts[0]));
            }
            // Format: VNSCP/1.0 <COMMAND>
            else if ("VNSCP/1.0".equals(firstLineParts[0])) {
                vnsMessage = new VNSCPPacket(PacketType.valueOf(firstLineParts[1]));
            } else {
                throw new IllegalArgumentException("Invalid protocol or version: " + lines[0]);
            }
        } else {
            throw new IllegalArgumentException("Invalid protocol or version: " + lines[0]);
        }
        return vnsMessage;
    }

}