package com.orbitalix.gui;

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
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JPanel;

/**
 * Custom-drawn real-time telemetry line chart plotting the last 60 readings with limit thresholds.
 * Drawn completely using Java2D Graphics2D with anti-aliasing.
 * Demonstrates: Observer pattern (TickListener), Java2D custom rendering, encapsulation.
 */
public class LiveChartPanel extends JPanel implements TickListener {

    private static final int MAX_HISTORY = 60;

    private String selectedSensorId = "TEMP-CORE";
    private String selectedUnit = "C";
    private double warnLimit = 45.0;
    private final List<Double> history = new ArrayList<>();

    public LiveChartPanel() {
        setBackground(Theme.PANEL_BG);
        setBorder(Theme.createBorder("LIVE SENSOR TELEMETRY"));
        setPreferredSize(new Dimension(380, 240));
    }

    public synchronized void setSelectedSensor(String sensorId) {
        if (sensorId == null || sensorId.equals(this.selectedSensorId)) {
            return;
        }
        this.selectedSensorId = sensorId;
        this.history.clear();

        // Update default warning limits and units
        if (sensorId.contains("TEMP")) {
            warnLimit = sensorId.contains("BATT") ? 40.0 : 45.0;
            selectedUnit = "C";
        } else if (sensorId.contains("VOLT")) {
            warnLimit = 24.0;
            selectedUnit = "V";
        } else if (sensorId.contains("SIG")) {
            warnLimit = 60.0;
            selectedUnit = "%";
        } else if (sensorId.contains("GYRO")) {
            warnLimit = 4.0;
            selectedUnit = "deg/s";
        }
        repaint();
    }

    public synchronized void reset() {
        history.clear();
        repaint();
    }

    @Override
    public synchronized void onTick(SimulationSnapshot snapshot) {
        if (snapshot.tick() <= 1) {
            history.clear();
        }

        // Find selected sensor reading
        Double foundValue = null;
        for (SubsystemSnapshot sub : snapshot.subsystems()) {
            for (SensorSnapshot s : sub.sensors()) {
                if (s.sensorId().equalsIgnoreCase(selectedSensorId)) {
                    foundValue = s.value();
                    selectedUnit = s.unit();
                    break;
                }
            }
            if (foundValue != null) break;
        }

        if (foundValue != null) {
            history.add(foundValue);
            if (history.size() > MAX_HISTORY) {
                history.remove(0);
            }
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int padLeft = 45;
        int padRight = 20;
        int padTop = 30;
        int padBottom = 30;

        int w = getWidth();
        int h = getHeight();
        int chartW = w - padLeft - padRight;
        int chartH = h - padTop - padBottom;

        if (chartW <= 0 || chartH <= 0) {
            g2.dispose();
            return;
        }

        // Background
        g2.setColor(Theme.PANEL_CARD);
        g2.fillRect(padLeft, padTop, chartW, chartH);
        g2.setColor(Theme.PANEL_BORDER);
        g2.drawRect(padLeft, padTop, chartW, chartH);

        // Header text
        g2.setFont(Theme.FONT_LABEL_BOLD);
        g2.setColor(Theme.ACCENT_CYAN);
        double latest = history.isEmpty() ? 0 : history.get(history.size() - 1);
        String title = String.format(Locale.US, "SENSOR: %s  |  CURRENT: %.2f %s", selectedSensorId, latest, selectedUnit);
        g2.drawString(title, padLeft, padTop - 10);

        if (history.isEmpty()) {
            g2.setFont(Theme.FONT_LABEL);
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString("Awaiting telemetry data...", padLeft + 20, padTop + chartH / 2);
            g2.dispose();
            return;
        }

        // Determine min and max Y range
        double minY = history.get(0);
        double maxY = history.get(0);
        for (double v : history) {
            if (v < minY) minY = v;
            if (v > maxY) maxY = v;
        }
        if (warnLimit < minY) minY = warnLimit;
        if (warnLimit > maxY) maxY = warnLimit;

        double range = maxY - minY;
        if (range < 4.0) range = 4.0;
        minY -= range * 0.15;
        maxY += range * 0.15;
        range = maxY - minY;

        // Grid lines
        g2.setColor(new Color(0x23, 0x31, 0x5C));
        Stroke solidGrid = new BasicStroke(1.0f);
        g2.setStroke(solidGrid);
        int gridDivisions = 4;
        g2.setFont(Theme.FONT_MONO);
        for (int i = 0; i <= gridDivisions; i++) {
            int y = padTop + (int) (chartH * (1.0 - (double) i / gridDivisions));
            g2.drawLine(padLeft, y, padLeft + chartW, y);

            double labelVal = minY + (range * i / gridDivisions);
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(String.format(Locale.US, "%5.1f", labelVal), 4, y + 4);
            g2.setColor(new Color(0x23, 0x31, 0x5C));
        }

        // Draw Warning Limit Line (dashed amber)
        if (warnLimit >= minY && warnLimit <= maxY) {
            int warnY = padTop + (int) (chartH * (1.0 - (warnLimit - minY) / range));
            Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6.0f, 4.0f}, 0.0f);
            g2.setStroke(dashed);
            g2.setColor(Theme.STATUS_WARNING);
            g2.drawLine(padLeft, warnY, padLeft + chartW, warnY);
            g2.drawString(String.format(Locale.US, "WARN LIMIT (%.1f %s)", warnLimit, selectedUnit), padLeft + 6, warnY - 4);
        }

        // Plot telemetry line
        int n = history.size();
        int[] xs = new int[n];
        int[] ys = new int[n];

        for (int i = 0; i < n; i++) {
            xs[i] = padLeft + (int) ((double) i / (MAX_HISTORY - 1) * chartW);
            double v = history.get(i);
            ys[i] = padTop + (int) (chartH * (1.0 - (v - minY) / range));
        }

        // Telemetry path
        g2.setStroke(new BasicStroke(2.2f));
        g2.setColor(Theme.ACCENT_CYAN);
        for (int i = 0; i < n - 1; i++) {
            g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
        }

        // Highlight latest point
        if (n > 0) {
            int lastX = xs[n - 1];
            int lastY = ys[n - 1];
            g2.setColor(Theme.STATUS_NOMINAL);
            g2.fillOval(lastX - 4, lastY - 4, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawOval(lastX - 4, lastY - 4, 8, 8);
        }

        g2.dispose();
    }
}
