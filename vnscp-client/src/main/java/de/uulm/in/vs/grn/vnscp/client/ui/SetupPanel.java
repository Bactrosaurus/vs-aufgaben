package de.uulm.in.vs.grn.vnscp.client.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class SetupPanel {

    private String username;
    private String host;
    private int commandPort;
    private int pubSubPort;

    private final JPanel setupPanel;
    private final JTextField usernameField;
    private final JTextField hostField;
    private final JTextField commandPortField;
    private final JTextField pubSubPortField;
    private int result;

    public SetupPanel() {
        this.usernameField = new JTextField();
        this.hostField = new JTextField();
        this.commandPortField = new JTextField();
        this.pubSubPortField = new JTextField();

        setupPanel = new JPanel(new GridLayout(4, 1));
        setupPanel.setPreferredSize(new Dimension(300, 80));
        setupPanel.add(new JLabel("Username: "));
        setupPanel.add(usernameField);
        setupPanel.add(new JLabel("Host: "));
        setupPanel.add(hostField);
        setupPanel.add(new JLabel("Command-Port: "));
        setupPanel.add(commandPortField);
        setupPanel.add(new JLabel("PubSub-Port: "));
        setupPanel.add(pubSubPortField);

        Arrays.stream(setupPanel.getComponents()).forEach(component -> {
            component.setFont(VNSCPTheme.getFont());
            component.setBackground(VNSCPTheme.backgroundColor);
            component.setForeground(VNSCPTheme.textColor);
        });
    }

    public boolean open() {
        result = JOptionPane.showConfirmDialog(null, setupPanel, "Setup", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            username = usernameField.getText();
            host = hostField.getText();
            try {
                commandPort = Integer.parseInt(commandPortField.getText());
                pubSubPort = Integer.parseInt(pubSubPortField.getText());
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    public int getCommandPort() {
        return commandPort;
    }

    public int getPubSubPort() {
        return pubSubPort;
    }

    public String getUsername() {
        return username;
    }

    public String getHost() {
        return host;
    }

    public int getResult() {
        return result;
    }

}
