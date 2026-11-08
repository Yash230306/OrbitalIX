package com.orbitalix.gui;

import com.orbitalix.simulation.SimulationSnapshot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Top dashboard header featuring title branding, active mission clock, and operational status banner.
 * Demonstrates: composition (owns StatusBanner and labels), encapsulation.
 */
public class HeaderPanel extends JPanel {

    private final JLabel clockLabel;
    private final StatusBanner statusBanner;

    public HeaderPanel() {
        super(new BorderLayout(15, 0));
        setBackground(Theme.PANEL_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.PANEL_BORDER),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        // West: Branding
        JPanel brandPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        brandPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("ORBITALIX · MISSION CONTROL");
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.ACCENT_CYAN);

        JLabel subtitleLabel = new JLabel("AUTONOMOUS SATELLITE HEALTH & TELEMETRY MONITOR");
        subtitleLabel.setFont(Theme.FONT_LABEL);
        subtitleLabel.setForeground(Theme.TEXT_MUTED);

        brandPanel.add(titleLabel);
        brandPanel.add(subtitleLabel);
        add(brandPanel, BorderLayout.WEST);

        // Center: Mission Clock
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        centerPanel.setOpaque(false);
        JLabel clockHeader = new JLabel("MISSION CLOCK:");
        clockHeader.setFont(Theme.FONT_LABEL_BOLD);
        clockHeader.setForeground(Theme.TEXT_MUTED);

        clockLabel = new JLabel("T+ 00:00:00");
        clockLabel.setFont(Theme.FONT_MONO_LARGE);
        clockLabel.setForeground(Theme.STATUS_NOMINAL);

        centerPanel.add(clockHeader);
        centerPanel.add(clockLabel);
        add(centerPanel, BorderLayout.CENTER);

        // East: Status Banner
        statusBanner = new StatusBanner();
        add(statusBanner, BorderLayout.EAST);
    }

    public void updateSnapshot(SimulationSnapshot snapshot) {
        int ticks = snapshot.tick();
        int hrs = ticks / 3600;
        int mins = (ticks % 3600) / 60;
        int secs = ticks % 60;
        clockLabel.setText(String.format("T+ %02d:%02d:%02d", hrs, mins, secs));
        statusBanner.setMode(snapshot.spacecraftState());
    }

    public StatusBanner getStatusBanner() {
        return statusBanner;
    }
}
