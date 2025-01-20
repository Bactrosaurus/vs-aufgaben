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

    private JFrame frame;
    private JTextArea messageArea;
    private final DefaultListModel<String> connectedUsers = new DefaultListModel<>();
    private VNSCPClient client;

    public ClientWindow() {
        if (!performLogin()) {
            System.exit(0); // Exit if login process fails or is canceled
        }

        initializeUI();
        client.initPubSubListener();
    }

    private boolean performLogin() {
        SetupPanel setupPanel = new SetupPanel();

        while (true) {
            if (!setupPanel.open()) {
                JOptionPane.showMessageDialog(null, "Invalid port numbers", "Error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            if (setupPanel.getResult() != JOptionPane.OK_OPTION) {
                return false; // User canceled the setup
            }

            if (!isValidUsername(setupPanel.getUsername())) {
                JOptionPane.showMessageDialog(null, "Username is invalid", "Error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            try {
                this.client = new VNSCPClient(this, setupPanel.getHost(), setupPanel.getCommandPort(), setupPanel.getPubSubPort(), setupPanel.getUsername());
            } catch (IOException e) {
                String errorMessage = (e instanceof UnknownHostException) ? "Unknown host" : e.toString();
                JOptionPane.showMessageDialog(null, errorMessage, "Initialization error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            if (!this.client.login()) {
                JOptionPane.showMessageDialog(null, this.client.getLatestError(), "Connection error", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            return true; // Login succeeded
        }
    }

    private void initializeUI() {
        frame = new JFrame("VNSCP Client");
        frame.setResizable(true);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setMinimumSize(new Dimension(100, 100));
        VNSCPTheme.apply(messageArea);

        JList<String> userList = new JList<>(connectedUsers);
        userList.setMinimumSize(new Dimension(100, 100));
        VNSCPTheme.apply(userList);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(messageArea), new JScrollPane(userList));
        splitPane.setResizeWeight(0.5);
        VNSCPTheme.apply(splitPane);
        frame.add(splitPane, BorderLayout.CENTER);

        JTextField textField = new JTextField();
        VNSCPTheme.apply(textField);
        textField.addActionListener((ActionEvent e) -> {
            if (!client.sendMessage(textField.getText())) {
                JOptionPane.showMessageDialog(frame, this.client.getLatestError(), "Message error", JOptionPane.ERROR_MESSAGE);
            } else {
                textField.setText("");
            }
        });

        frame.add(textField, BorderLayout.SOUTH);

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
        SwingUtilities.invokeLater(() -> messageArea.append("[" + username + "] " + message + "\r\n"));
    }

    public DefaultListModel<String> getConnectedUsers() {
        return connectedUsers;
    }

}

