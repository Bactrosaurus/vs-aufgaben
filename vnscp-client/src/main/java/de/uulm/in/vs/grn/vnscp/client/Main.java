package de.uulm.in.vs.grn.vnscp.client;

import de.uulm.in.vs.grn.vnscp.client.ui.ClientWindow;

public class Main {

    public static void main(String[] args) {
        // Set system properties to make fonts not look ugly
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");

        ClientWindow window = new ClientWindow();
    }

}
