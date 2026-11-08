package com.orbitalix.gui;

import com.orbitalix.model.HealthStatus;
import com.orbitalix.simulation.SensorSnapshot;
import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.SubsystemSnapshot;
import com.orbitalix.simulation.TickListener;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.JPanel;

/**
 * Visual diagnostic cockpit with radial dial gauges and overall satellite health scoring.
 * Translates raw engineering telemetry into clear, human-readable circular meters and plain-English indicators.
 * Demonstrates: Java2D custom arc rendering, encapsulation, Observer pattern (TickListener).
 */
public class SubsystemGaugesPanel extends JPanel implements TickListener {

    private final Map<String, Double> sensorValues = new HashMap<>();
    private final Map<String, HealthStatus> subHealth = new HashMap<>();
    private String spacecraftState = "NOMINAL";

    public SubsystemGaugesPanel() {
        setBackground(Theme.BG_DARK);
        setBorder(Theme.createBorder("TACTICAL GAUGES & HEALTH COCKPIT"));
        setPreferredSize(new Dimension(450, 420));

        // Default baselines
        sensorValues.put("VOLT-BUS", 28.0);
        sensorValues.put("TEMP-CORE", 25.0);
        sensorValues.put("SIG-ANT", 96.0);
        sensorValues.put("GYRO-PRI", 0.0);

        subHealth.put("Power", HealthStatus.NOMINAL);
        subHealth.put("Thermal", HealthStatus.NOMINAL);
        subHealth.put("Communication", HealthStatus.NOMINAL);
        subHealth.put("Attitude", HealthStatus.NOMINAL);
    }

    @Override
    public void onTick(SimulationSnapshot snapshot) {
        this.spacecraftState = snapshot.spacecraftState();
        for (SubsystemSnapshot sub : snapshot.subsystems()) {
            subHealth.put(sub.name(), sub.status());
            for (SensorSnapshot s : sub.sensors()) {
                sensorValues.put(s.sensorId(), s.value());
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Overall Platform Health Banner in Top-Center
        int overallScore = calculateHealthScore();
        Color healthColor = Theme.getModeColor(spacecraftState);

        g2.setColor(new Color(0x12, 0x1A, 0x33));
        g2.fillRoundRect(w / 2 - 130, 28, 260, 48, 12, 12);
        g2.setColor(healthColor);
        g2.drawRoundRect(w / 2 - 130, 28, 260, 48, 12, 12);

        g2.setFont(Theme.FONT_TITLE);
        String scoreText = String.format("FLEET HEALTH: %d%%", overallScore);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(scoreText, w / 2 - fm.stringWidth(scoreText) / 2, 54);

        g2.setFont(Theme.FONT_LABEL);
        g2.setColor(Theme.TEXT_MUTED);
        String stateSub = "OPERATIONAL MODE: " + spacecraftState;
        fm = g2.getFontMetrics();
        g2.drawString(stateSub, w / 2 - fm.stringWidth(stateSub) / 2, 70);

        // 2. Draw 4 Circular Radial Arc Gauges (2x2 Grid)
        int gaugeW = (w - 60) / 2;
        int gaugeH = (h - 110) / 2;

        int gx1 = 30;
        int gx2 = 30 + gaugeW;
        int gy1 = 92;
        int gy2 = 92 + gaugeH;

        // Dial 1: Power Bus Voltage
        double volt = sensorValues.getOrDefault("VOLT-BUS", 28.0);
        drawRadialGauge(g2, gx1, gy1, gaugeW, gaugeH, "POWER BUS", volt, 15.0, 35.0, "V",
                subHealth.get("Power"), 24.0, 32.0);

        // Dial 2: Thermal Core Temperature
        double temp = sensorValues.getOrDefault("TEMP-CORE", 25.0);
        drawRadialGauge(g2, gx2, gy1, gaugeW, gaugeH, "THERMAL CORE", temp, 0.0, 90.0, "°C",
                subHealth.get("Thermal"), 15.0, 45.0);

        // Dial 3: Communication Antenna Signal
        double sig = sensorValues.getOrDefault("SIG-ANT", 96.0);
        drawRadialGauge(g2, gx1, gy2, gaugeW, gaugeH, "UPLINK SIGNAL", sig, 0.0, 100.0, "%",
                subHealth.get("Communication"), 60.0, 100.0);

        // Dial 4: Attitude Gyro Stability
        double gyro = sensorValues.getOrDefault("GYRO-PRI", 0.0);
        drawRadialGauge(g2, gx2, gy2, gaugeW, gaugeH, "GYRO DRIFT", gyro, -6.0, 6.0, "°/s",
                subHealth.get("Attitude"), -3.0, 3.0);

        g2.dispose();
    }

    private void drawRadialGauge(Graphics2D g2, int x, int y, int w, int h, String title,
                                 double val, double min, double max, String unit,
                                 HealthStatus status, double safeMin, double safeMax) {
        int cx = x + w / 2;
        int cy = y + h / 2 - 4;
        int radius = Math.min(w, h) / 2 - 20;

        // Background Dial Arc (240 degrees from 150 to -30)
        int startAngle = -210;
        int arcExtent = -240;

        g2.setStroke(new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(0x1B, 0x24, 0x40));
        g2.drawArc(cx - radius, cy - radius, radius * 2, radius * 2, startAngle, arcExtent);

        // Normalized progress [0.0 to 1.0]
        double norm = (val - min) / (max - min);
        norm = Math.max(0.0, Math.min(1.0, norm));
        int activeExtent = (int) (arcExtent * norm);

        Color valColor = Theme.getHealthColor(status);
        g2.setColor(valColor);
        g2.drawArc(cx - radius, cy - radius, radius * 2, radius * 2, startAngle, activeExtent);

        // Value text
        g2.setFont(Theme.FONT_TITLE);
        g2.setColor(Theme.TEXT_PRIMARY);
        String valStr = String.format(Locale.US, "%.1f %s", val, unit);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(valStr, cx - fm.stringWidth(valStr) / 2, cy + 6);

        // Title text
        g2.setFont(Theme.FONT_LABEL_BOLD);
        g2.setColor(Theme.ACCENT_CYAN);
        fm = g2.getFontMetrics();
        g2.drawString(title, cx - fm.stringWidth(title) / 2, cy - radius + 2);

        // Plain English status label
        g2.setFont(Theme.FONT_MONO);
        g2.setColor(valColor);
        String statusLabel = status != null ? status.name() : "NOMINAL";
        fm = g2.getFontMetrics();
        g2.drawString(statusLabel, cx - fm.stringWidth(statusLabel) / 2, cy + 24);
    }

    private int calculateHealthScore() {
        int nominals = 0;
        int total = subHealth.size();
        for (HealthStatus st : subHealth.values()) {
            if (st == HealthStatus.NOMINAL) nominals += 25;
            else if (st == HealthStatus.WARNING) nominals += 12;
            else if (st == HealthStatus.CRITICAL) nominals += 4;
        }
        return Math.max(5, Math.min(100, nominals));
    }
}
