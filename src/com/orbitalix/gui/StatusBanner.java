package com.orbitalix.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Large glowing status banner indicating current spacecraft operational mode.
 * Pulses continuously during CRITICAL state.
 * Demonstrates: inheritance (JComponent), encapsulation, custom Java2D rendering.
 */
public class StatusBanner extends JComponent {

    private String mode = "NOMINAL";
    private float pulsePhase = 0f;
    private boolean pulseUp = true;
    private final Timer pulseTimer;

    public StatusBanner() {
        setPreferredSize(new Dimension(180, 42));
        setMinimumSize(new Dimension(140, 36));

        // Pulsing animation timer for CRITICAL mode
        pulseTimer = new Timer(40, e -> {
            if ("CRITICAL".equalsIgnoreCase(mode)) {
                if (pulseUp) {
                    pulsePhase += 0.08f;
                    if (pulsePhase >= 1.0f) {
                        pulsePhase = 1.0f;
                        pulseUp = false;
                    }
                } else {
                    pulsePhase -= 0.08f;
                    if (pulsePhase <= 0.2f) {
                        pulsePhase = 0.2f;
                        pulseUp = true;
                    }
                }
                repaint();
            }
        });
    }

    public void setMode(String newMode) {
        if (newMode == null) newMode = "NOMINAL";
        this.mode = newMode.toUpperCase();
        if ("CRITICAL".equalsIgnoreCase(this.mode)) {
            if (!pulseTimer.isRunning()) {
                pulseTimer.start();
            }
        } else {
            if (pulseTimer.isRunning()) {
                pulseTimer.stop();
            }
        }
        repaint();
    }

    public String getMode() {
        return mode;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        Color baseColor = Theme.getModeColor(mode);

        // Calculate background glow
        Color fillColor;
        if ("CRITICAL".equalsIgnoreCase(mode)) {
            int alpha = (int) (60 + pulsePhase * 160);
            fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
        } else {
            fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 50);
        }

        // Draw rounded container
        g2.setColor(fillColor);
        g2.fillRoundRect(2, 2, w - 4, h - 4, 16, 16);

        // Draw glowing border
        g2.setColor(baseColor);
        g2.drawRoundRect(2, 2, w - 4, h - 4, 16, 16);

        // Draw mode text
        g2.setFont(Theme.FONT_TITLE);
        g2.setColor(Theme.TEXT_PRIMARY);
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(mode);
        int textH = fm.getAscent();
        int x = (w - textW) / 2;
        int y = (h + textH) / 2 - 4;

        // Subtle text shadow
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(mode, x + 1, y + 1);

        g2.setColor(baseColor.brighter());
        g2.drawString(mode, x, y);

        g2.dispose();
    }
}
