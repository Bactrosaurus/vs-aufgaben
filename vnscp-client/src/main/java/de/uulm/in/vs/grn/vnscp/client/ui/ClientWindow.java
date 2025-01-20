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

    private final DefaultListModel<String> connectedUsers;
    private final JTextArea messageArea;
    private VNSCPClient client;

    public ClientWindow() {
        JFrame frame = new JFrame("VNSCP Client");
        frame.setResizable(true);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.connectedUsers = new DefaultListModel<>();

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
                this.client = new VNSCPClient(this, setupPanel.getHost(), setupPanel.getCommandPort(), setupPanel.getPubSubPort(), setupPanel.getUsername());
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
        this.messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setMinimumSize(new Dimension(100, 100));
        setDesign(messageArea);

        // user list
        JList<String> userList = new JList<>(connectedUsers);
        setDesign(userList);
        userList.setMinimumSize(new Dimension(100, 100));

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
    }

    private void setDesign(Component component) {
        component.setFont(Design.getFont());
        component.setBackground(Design.backgroundColor);
        component.setForeground(Design.textColor);
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z0-9]{3,15}$");
    }

    public void displayEvent(String eventDescription) {
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
        messageArea.repaint();
    }

    public void displayMessage(String username, String message) {
        messageArea.append("[" + username + "] " + message + "\r\n");
        messageArea.repaint();
    }

    public DefaultListModel<String> getConnectedUsers() {
        return connectedUsers;
    }

}
