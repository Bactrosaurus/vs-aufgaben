package de.uulm.in.vs.grn.vnscp.client.ui;

import de.uulm.in.vs.grn.vnscp.client.VNSCPClient;
import de.uulm.in.vs.grn.vnscp.client.network.VNSCPPacket;
import de.uulm.in.vs.grn.vnscp.client.utils.Design;

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

        // Show login panel
        SetupPanel setupPanel = new SetupPanel();
        while (true) {
            if (!setupPanel.open()) {
                JOptionPane.showMessageDialog(frame, "Invalid port numbers", "Error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

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
                JOptionPane.showMessageDialog(frame, e, "Initialization error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            if (!this.client.login()) {
                JOptionPane.showMessageDialog(frame, this.client.getLatestError(), "Connection error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            // Client logged in
            break;
        }

        // Client logged in and login panel closed
        JTextArea messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setMinimumSize(new Dimension(100, 100));
        setDesign(messageArea);

        // Set initial ping to get all online users
        VNSCPPacket initialPing = new VNSCPPacket(VNSCPPacket.PacketType.PING);
        this.client.getCommandConnection().send(initialPing);
        try {
            initialPing = this.client.getCommandConnection().receive();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] onlineUsers = initialPing.getField("Usernames").split(",");

        // user list
        DefaultListModel<String> connectedUsers = new DefaultListModel<>();
        JList<String> userList = new JList<>(connectedUsers);
        setDesign(userList);
        userList.setMinimumSize(new Dimension(100, 100));
        Arrays.stream(onlineUsers).forEach(connectedUsers::addElement);

        // make user list resizable
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(messageArea), new JScrollPane(userList));
        splitPane.setResizeWeight(0.5);
        frame.add(splitPane, BorderLayout.CENTER);

        frame.setTitle("VNSCP Client - " + setupPanel.getUsername());

        // Text entry field
        JTextField textField = new JTextField();
        setDesign(textField);
        textField.addActionListener((ActionEvent e) -> {
            if (!client.sendMessage(textField.getText())) {
                JOptionPane.showMessageDialog(frame, this.client.getLatestError(), "Message error", JOptionPane.ERROR_MESSAGE);
            } else {
                textField.setText("");
            }
        });

        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(textField, BorderLayout.SOUTH);

        // log out and close socket and io on window close
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.logout();
            }
        });

        frame.setVisible(true);

        // handle incoming messages and events
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

    private void setDesign(Component component) {
        component.setFont(Design.getFont());
        component.setBackground(Design.backgroundColor);
        component.setForeground(Design.textColor);
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z0-9]{3,15}$");
    }

}
