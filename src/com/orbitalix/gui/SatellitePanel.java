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
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Interactive Java2D visual model of the satellite against a twinkling deep-space starfield.
 * Each subsystem component glows and pulses in accordance with real-time operational health.
 * Demonstrates: Java2D custom rendering, Observer pattern (TickListener), user interaction (mouse selection).
 */
public class SatellitePanel extends JPanel implements TickListener {

    private final Map<String, HealthStatus> subsystemHealth = new HashMap<>();
    private final Map<String, String> subsystemReadings = new HashMap<>();
    private Consumer<String> onSensorSelected;

    // Starfield
    private static final int STAR_COUNT = 75;
    private final int[] starX = new int[STAR_COUNT];
    private final int[] starY = new int[STAR_COUNT];
    private final float[] starAlpha = new float[STAR_COUNT];
    private final boolean[] starAlphaUp = new boolean[STAR_COUNT];

    // Pulsing animation for critical parts
    private float pulsePhase = 0f;
    private boolean pulseUp = true;
    private final Timer animationTimer;

    // Component clickable bounding boxes
    private Rectangle thermalBounds = new Rectangle();
    private Rectangle powerLeftBounds = new Rectangle();
    private Rectangle powerRightBounds = new Rectangle();
    private Rectangle commBounds = new Rectangle();
    private Rectangle attitudeBounds = new Rectangle();

    public SatellitePanel() {
        setBackground(Theme.BG_DARK);
        setBorder(Theme.createBorder("ORBITAL PLATFORM STATUS"));
        setPreferredSize(new Dimension(420, 400));

        // Initialize starfield coordinates
        Random rng = new Random(42);
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = rng.nextInt(600);
            starY[i] = rng.nextInt(600);
            starAlpha[i] = 0.2f + rng.nextFloat() * 0.7f;
            starAlphaUp[i] = rng.nextBoolean();
        }

        // Initialize default healths
        subsystemHealth.put("Power", HealthStatus.NOMINAL);
        subsystemHealth.put("Thermal", HealthStatus.NOMINAL);
        subsystemHealth.put("Communication", HealthStatus.NOMINAL);
        subsystemHealth.put("Attitude", HealthStatus.NOMINAL);

        subsystemReadings.put("Power", "28.0 V");
        subsystemReadings.put("Thermal", "25.0 C");
        subsystemReadings.put("Communication", "96 %");
        subsystemReadings.put("Attitude", "0.00 deg/s");

        // Starfield twinkle and pulse animation timer
        animationTimer = new Timer(50, e -> {
            for (int i = 0; i < STAR_COUNT; i++) {
                if (starAlphaUp[i]) {
                    starAlpha[i] += 0.03f;
                    if (starAlpha[i] >= 0.9f) starAlphaUp[i] = false;
                } else {
                    starAlpha[i] -= 0.03f;
                    if (starAlpha[i] <= 0.2f) starAlphaUp[i] = true;
                }
            }
            if (pulseUp) {
                pulsePhase += 0.07f;
                if (pulsePhase >= 1.0f) pulseUp = false;
            } else {
                pulsePhase -= 0.07f;
                if (pulsePhase <= 0.2f) pulseUp = true;
            }
            repaint();
        });
        animationTimer.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                if (thermalBounds.contains(p)) {
                    notifySelection("TEMP-CORE");
                } else if (powerLeftBounds.contains(p) || powerRightBounds.contains(p)) {
                    notifySelection("VOLT-BUS");
                } else if (commBounds.contains(p)) {
                    notifySelection("SIG-ANT");
                } else if (attitudeBounds.contains(p)) {
                    notifySelection("GYRO-PRI");
                }
            }
        });
    }

    private void notifySelection(String sensorId) {
        if (onSensorSelected != null) {
            onSensorSelected.accept(sensorId);
        }
    }

    public void setOnSensorSelected(Consumer<String> onSensorSelected) {
        this.onSensorSelected = onSensorSelected;
    }

    @Override
    public void onTick(SimulationSnapshot snapshot) {
        for (SubsystemSnapshot sub : snapshot.subsystems()) {
            subsystemHealth.put(sub.name(), sub.status());
            if (!sub.sensors().isEmpty()) {
                SensorSnapshot primary = sub.sensors().get(0);
                subsystemReadings.put(sub.name(), String.format("%.1f %s", primary.value(), primary.unit()));
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

        // 1. Draw Starfield
        for (int i = 0; i < STAR_COUNT; i++) {
            int sx = starX[i] % Math.max(1, w);
            int sy = starY[i] % Math.max(1, h);
            int alpha = (int) (starAlpha[i] * 255);
            g2.setColor(new Color(255, 255, 255, Math.min(255, Math.max(0, alpha))));
            g2.fillRect(sx, sy, (i % 5 == 0) ? 2 : 1, (i % 5 == 0) ? 2 : 1);
        }

        // Center of satellite
        int cx = w / 2;
        int cy = h / 2 + 10;

        // Colors according to subsystem health
        Color pwrColor = getGlowColor(subsystemHealth.get("Power"));
        Color thmColor = getGlowColor(subsystemHealth.get("Thermal"));
        Color comColor = getGlowColor(subsystemHealth.get("Communication"));
        Color attColor = getGlowColor(subsystemHealth.get("Attitude"));

        // 2. Solar Panels (Power) - Left and Right
        int panelW = 75;
        int panelH = 55;
        powerLeftBounds = new Rectangle(cx - 150, cy - panelH / 2, panelW, panelH);
        powerRightBounds = new Rectangle(cx + 75, cy - panelH / 2, panelW, panelH);

        // Connecting truss
        g2.setColor(new Color(0x3A, 0x4B, 0x7A));
        g2.setStroke(new BasicStroke(3.0f));
        g2.drawLine(cx - 150, cy, cx + 150, cy);

        drawSolarPanel(g2, powerLeftBounds, pwrColor, "POWER (L)");
        drawSolarPanel(g2, powerRightBounds, pwrColor, "POWER (R)");

        // 3. Attitude Subsystem - Gyro stabilization ring at bottom
        int gyroW = 90;
        int gyroH = 26;
        attitudeBounds = new Rectangle(cx - gyroW / 2, cy + 65, gyroW, gyroH);
        g2.setColor(new Color(attColor.getRed(), attColor.getGreen(), attColor.getBlue(), 40));
        g2.fillOval(attitudeBounds.x, attitudeBounds.y, gyroW, gyroH);
        g2.setColor(attColor);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawOval(attitudeBounds.x, attitudeBounds.y, gyroW, gyroH);
        drawSubLabel(g2, "ATTITUDE: " + subsystemReadings.getOrDefault("Attitude", ""), cx, cy + 105, attColor);

        // 4. Central Body (Thermal)
        int bodyW = 90;
        int bodyH = 100;
        thermalBounds = new Rectangle(cx - bodyW / 2, cy - bodyH / 2, bodyW, bodyH);

        g2.setColor(new Color(thmColor.getRed(), thmColor.getGreen(), thmColor.getBlue(), 55));
        g2.fillRoundRect(thermalBounds.x, thermalBounds.y, bodyW, bodyH, 16, 16);
        g2.setColor(thmColor);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(thermalBounds.x, thermalBounds.y, bodyW, bodyH, 16, 16);

        // Inner core graphic
        g2.setColor(new Color(0x18, 0x22, 0x45));
        g2.fillRoundRect(cx - 30, cy - 30, 60, 60, 10, 10);
        g2.setColor(thmColor);
        g2.drawRoundRect(cx - 30, cy - 30, 60, 60, 10, 10);

        drawSubLabel(g2, "THERMAL", cx, cy - 6, thmColor);
        drawSubLabel(g2, subsystemReadings.getOrDefault("Thermal", ""), cx, cy + 14, Theme.TEXT_PRIMARY);

        // 5. Communication Subsystem - Antenna dish on top
        int dishW = 60;
        int dishH = 24;
        commBounds = new Rectangle(cx - dishW / 2, cy - 90, dishW, dishH + 20);

        // Mast
        g2.setColor(new Color(0x3A, 0x4B, 0x7A));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawLine(cx, cy - 50, cx, cy - 70);

        // Dish arc
        g2.setColor(comColor);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawArc(cx - dishW / 2, cy - 85, dishW, dishH, 0, 180);
        // Feed horn
        g2.drawLine(cx, cy - 73, cx, cy - 90);
        g2.fillOval(cx - 3, cy - 93, 6, 6);

        drawSubLabel(g2, "COMM: " + subsystemReadings.getOrDefault("Communication", ""), cx, cy - 100, comColor);

        // Hint at bottom
        g2.setFont(Theme.FONT_LABEL);
        g2.setColor(Theme.TEXT_MUTED);
        String hint = "Click any component to chart its telemetry";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(hint, (w - fm.stringWidth(hint)) / 2, h - 8);

        g2.dispose();
    }

    private void drawSolarPanel(Graphics2D g2, Rectangle r, Color color, String label) {
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRect(r.x, r.y, r.width, r.height);

        // Solar cell grid
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 90));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawLine(r.x + r.width / 3, r.y, r.x + r.width / 3, r.y + r.height);
        g2.drawLine(r.x + 2 * r.width / 3, r.y, r.x + 2 * r.width / 3, r.y + r.height);
        g2.drawLine(r.x, r.y + r.height / 2, r.x + r.width, r.y + r.height / 2);

        drawSubLabel(g2, label, r.x + r.width / 2, r.y + r.height + 14, color);
    }

    private void drawSubLabel(Graphics2D g2, String text, int centerX, int baselineY, Color color) {
        g2.setFont(Theme.FONT_LABEL_BOLD);
        FontMetrics fm = g2.getFontMetrics();
        int tx = centerX - fm.stringWidth(text) / 2;
        g2.setColor(color);
        g2.drawString(text, tx, baselineY);
    }

    private Color getGlowColor(HealthStatus status) {
        Color base = Theme.getHealthColor(status);
        if (status == HealthStatus.CRITICAL) {
            int alpha = (int) (140 + pulsePhase * 115);
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.min(255, alpha));
        }
        return base;
    }
}
