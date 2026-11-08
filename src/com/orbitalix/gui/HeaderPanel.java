package com.orbitalix.gui;

import com.orbitalix.simulation.SimulationSnapshot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Advanced mission control header telemetry display with live UTC clock, Mission Elapsed Time (MET),
 * dynamic orbital parameter strip, and operational status banner.
 * Demonstrates: composition (owns StatusBanner and labels), encapsulation.
 */
public class HeaderPanel extends JPanel {

    private final JLabel clockLabel;
    private final JLabel utcLabel;
    private final StatusBanner statusBanner;
    private final DateTimeFormatter utcFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");

    public HeaderPanel() {
        super(new BorderLayout(20, 0));
        setBackground(new Color(0x0A, 0x0E, 0x1E));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x1F, 0x2C, 0x54)),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));

        // West: Branding & Mission Info
        JPanel brandPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        brandPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("ORBITALIX // MISSION CONTROL");
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.ACCENT_CYAN);

        JLabel subtitleLabel = new JLabel("LEO ORBITAL PLATFORM HEALTH & AUTONOMOUS SURVIVAL SYSTEM");
        subtitleLabel.setFont(Theme.FONT_LABEL);
        subtitleLabel.setForeground(Theme.TEXT_MUTED);

        brandPanel.add(titleLabel);
        brandPanel.add(subtitleLabel);
        add(brandPanel, BorderLayout.WEST);

        // Center: Mission Clock & Orbital Telemetry
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        centerPanel.setOpaque(false);

        JPanel clocksRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        clocksRow.setOpaque(false);

        JLabel metHeader = new JLabel("MET:");
        metHeader.setFont(Theme.FONT_LABEL_BOLD);
        metHeader.setForeground(Theme.TEXT_MUTED);

        clockLabel = new JLabel("T+ 00:00:00");
        clockLabel.setFont(Theme.FONT_MONO_LARGE);
        clockLabel.setForeground(Theme.STATUS_NOMINAL);

        utcLabel = new JLabel(ZonedDateTime.now(ZoneOffset.UTC).format(utcFormatter));
        utcLabel.setFont(Theme.FONT_MONO);
        utcLabel.setForeground(Theme.TEXT_MUTED);

        clocksRow.add(metHeader);
        clocksRow.add(clockLabel);
        clocksRow.add(new JLabel(" | "));
        clocksRow.add(utcLabel);

        // Orbital Flight Dynamics Strip
        JLabel orbitStrip = new JLabel("ORBIT: 548.2 KM · VEL: 7.61 KM/S · INC: 97.4° · APOGEE: 554 KM · PERIGEE: 542 KM");
        orbitStrip.setFont(Theme.FONT_MONO);
        orbitStrip.setForeground(new Color(0x82, 0x9B, 0xC4));
        orbitStrip.setHorizontalAlignment(JLabel.CENTER);

        centerPanel.add(clocksRow);
        centerPanel.add(orbitStrip);
        add(centerPanel, BorderLayout.CENTER);

        // East: Audio Toggle & Status Banner
        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        eastPanel.setOpaque(false);

        JButton audioBtn = new JButton("🔊 AUDIO ON");
        audioBtn.setFont(Theme.FONT_MONO);
        audioBtn.setBackground(new Color(0x18, 0x22, 0x45));
        audioBtn.setForeground(Theme.ACCENT_CYAN);
        audioBtn.setFocusPainted(false);
        audioBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x23, 0x31, 0x5C)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        audioBtn.addActionListener(e -> {
            SoundEffects.getInstance().toggleSound();
            boolean on = SoundEffects.getInstance().isSoundEnabled();
            audioBtn.setText(on ? "🔊 AUDIO ON" : "🔇 AUDIO OFF");
            audioBtn.setForeground(on ? Theme.ACCENT_CYAN : Theme.TEXT_MUTED);
            if (on) SoundEffects.getInstance().playClick();
        });

        statusBanner = new StatusBanner();
        eastPanel.add(audioBtn);
        eastPanel.add(statusBanner);
        add(eastPanel, BorderLayout.EAST);

        // Live UTC Clock updater
        Timer utcTimer = new Timer(1000, e -> {
            utcLabel.setText(ZonedDateTime.now(ZoneOffset.UTC).format(utcFormatter));
        });
        utcTimer.start();
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
