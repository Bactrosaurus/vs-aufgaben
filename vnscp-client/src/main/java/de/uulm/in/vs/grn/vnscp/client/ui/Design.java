package de.uulm.in.vs.grn.vnscp.client.ui;


import de.uulm.in.vs.grn.vnscp.client.Main;

import java.awt.*;
import java.io.InputStream;

public class Design {

    public static final Color backgroundColor = new Color(240, 240, 240); // Light gray background
    public static final Color textColor = new Color(75, 75, 75);

    public static Font getFont() {
        Font font;
        try (InputStream fontIs = Main.class.getClassLoader().getResourceAsStream("font.ttf")) {
            assert fontIs != null;
            font = Font.createFont(java.awt.Font.TRUETYPE_FONT, fontIs).deriveFont(java.awt.Font.PLAIN, 14);
        } catch (Exception e) {
            font = new Font("Default", java.awt.Font.PLAIN, 14);
        }
        return font;
    }

    public static void apply(Component component) {
        component.setFont(Design.getFont());
        component.setBackground(Design.backgroundColor);
        component.setForeground(Design.textColor);
    }

}
