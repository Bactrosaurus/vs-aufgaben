package de.uulm.in.vs.grn.vnscp.client.ui;

import de.uulm.in.vs.grn.vnscp.client.VNSCPClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.UnknownHostException;

public class ClientWindow {

    private final JFrame frame;
    private final JTextArea messageArea;
    private final DefaultListModel<String> connectedUsers;
    private VNSCPClient client;

    public ClientWindow() {
        this.frame = new JFrame("VNSCP Client");
        this.frame.setResizable(true);
        this.frame.setSize(800, 600);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.connectedUsers = new DefaultListModel<>();

        this.messageArea = new JTextArea();
        this.messageArea.setEditable(false);
        this.messageArea.setLineWrap(true);
        this.messageArea.setWrapStyleWord(true);
        this.messageArea.setMinimumSize(new Dimension(100, 100));
        Design.apply(this.messageArea);

        // user list
        JList<String> userList = new JList<>(connectedUsers);
        Design.apply(userList);
        userList.setMinimumSize(new Dimension(100, 100));

        // make user list resizable
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(messageArea), new JScrollPane(userList));
        splitPane.setResizeWeight(0.5);
        frame.add(splitPane, BorderLayout.CENTER);

        // Text entry field
        JTextField textField = new JTextField();
        Design.apply(textField);
        textField.addActionListener((ActionEvent e) -> {
            if (!client.sendMessage(textField.getText())) {
                JOptionPane.showMessageDialog(frame, this.client.getLatestError(), "Message error", JOptionPane.ERROR_MESSAGE);
            } else {
                textField.setText("");
            }
        });

        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(textField, BorderLayout.SOUTH);

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

            // Try to create client
            try {
                this.client = new VNSCPClient(this, setupPanel.getHost(), setupPanel.getCommandPort(), setupPanel.getPubSubPort(), setupPanel.getUsername());
            } catch (IOException e) {
                String errorMessage;
                if (e instanceof UnknownHostException) {
                    errorMessage = "Unknown host";
                } else {
                    errorMessage = e.toString();
                }
                JOptionPane.showMessageDialog(frame, errorMessage, "Initialization error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            // Check if login failed
            if (!this.client.login()) {
                JOptionPane.showMessageDialog(frame, this.client.getLatestError(), "Connection error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            // Client logged in
            break;
        }

        // Client logged in and setup panel closed
        frame.setTitle("VNSCP Client - " + setupPanel.getUsername());

        // log out and close socket and io on window close
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.logout();
            }
        });

        frame.setVisible(true);
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z0-9]{3,15}$");
    }

    public void displayEvent(String eventDescription) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append("[EVENT] " + eventDescription + "\r\n");

            String[] messageParts = eventDescription.split(" ");
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
        });
    }

    public void displayMessage(String username, String message) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append("[" + username + "] " + message + "\r\n");
        });
    }

    public DefaultListModel<String> getConnectedUsers() {
        return connectedUsers;
    }

}
