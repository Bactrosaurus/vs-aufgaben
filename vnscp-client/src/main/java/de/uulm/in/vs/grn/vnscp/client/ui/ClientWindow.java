package de.uulm.in.vs.grn.vnscp.client.ui;

import de.uulm.in.vs.grn.vnscp.client.VNSCPClient;
import de.uulm.in.vs.grn.vnscp.client.network.VNSCPPacket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Arrays;

public class ClientWindow {

    private VNSCPClient client;

    public ClientWindow() {
        JFrame frame = new JFrame("VNSCP Client");
        frame.setResizable(true);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Color backgroundColor = new Color(240, 240, 240); // Light gray background
        Color textColor = new Color(75, 75, 75);
        Font font = new Font("Default", Font.PLAIN, 14);

        JTextArea messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(font);
        messageArea.setBackground(backgroundColor);
        messageArea.setForeground(textColor);
        messageArea.setMinimumSize(new Dimension(100, 100));

        SetupPanel setupPanel = new SetupPanel();

        while (true) {
            setupPanel.open();

            if (setupPanel.getResult() != JOptionPane.OK_OPTION) {
                System.exit(0);
                break;
            }

            if (!isValidUsername(setupPanel.getUsername())) {
                JOptionPane.showMessageDialog(frame, "Username is invalid", "Error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            try {
                this.client = new VNSCPClient(setupPanel.getHost(), setupPanel.getCommandPort(), setupPanel.getPubSubPort(), setupPanel.getUsername());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, e.getMessage(), "Initialization error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            if (!this.client.login()) {
                JOptionPane.showMessageDialog(frame, this.client.getLatestError(), "Connection error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            // Client logged in
            break;
        }

        VNSCPPacket initialPing = new VNSCPPacket(VNSCPPacket.PacketType.PING);
        this.client.getCommandConnection().send(initialPing);

        try {
            initialPing = this.client.getCommandConnection().receive();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String[] onlineUsers = initialPing.getField("Usernames").split(",");

        DefaultListModel<String> connectedUsers = new DefaultListModel<>();
        JList<String> userList = new JList<>(connectedUsers);
        userList.setFont(font);
        userList.setBackground(backgroundColor);
        userList.setForeground(textColor);
        userList.setMinimumSize(new Dimension(100, 100));
        Arrays.stream(onlineUsers).forEach(connectedUsers::addElement);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(messageArea), new JScrollPane(userList));
        splitPane.setResizeWeight(0.5);
        frame.add(splitPane, BorderLayout.CENTER);

        frame.setTitle("VNSCP Client - " + setupPanel.getUsername());

        JTextField textField = new JTextField();
        textField.setFont(font);
        textField.setForeground(textColor);
        textField.setBackground(backgroundColor);
        textField.addActionListener((ActionEvent e) -> {
            if (!client.sendMessage(textField.getText())) {
                JOptionPane.showMessageDialog(frame, this.client.getLatestError(), "Message error", JOptionPane.ERROR_MESSAGE);
            } else {
                textField.setText("");
            }
        });

        frame.add(splitPane, BorderLayout.CENTER);

        frame.add(textField, BorderLayout.SOUTH);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.logout();
            }
        });

        frame.setVisible(true);

        client.getPubSubConnection().listen(receivedMessage -> SwingUtilities.invokeLater(() -> {
            if (receivedMessage.getPacketType() == VNSCPPacket.PacketType.EVENT) {
                messageArea.append("[EVENT] " + receivedMessage.getEventDescription() + "\r\n");

                String[] messageParts = receivedMessage.getEventDescription().split(" ");
                if (messageParts.length == 3) {
                    if (messageParts[2].equals("joined")) {
                        if (!connectedUsers.contains(messageParts[0])) {
                            connectedUsers.addElement(messageParts[0]);
                        }
                    }

                    if (messageParts[2].equals("left")) {
                        connectedUsers.removeElement(messageParts[0]);
                    }
                }
            }

            if (receivedMessage.getPacketType() == VNSCPPacket.PacketType.MESSAGE) {
                messageArea.append("[" + receivedMessage.getUsername() + "] " + receivedMessage.getMessage() + "\r\n");
            }
        }));
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z0-9]{3,15}$");
    }

}
