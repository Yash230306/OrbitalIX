package com.orbitalix.gui;

import com.orbitalix.simulation.SensorSnapshot;
import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.SubsystemSnapshot;
import com.orbitalix.simulation.TickListener;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JPanel;

/**
 * Advanced real-time telemetry line chart plotting live sensor readings with glowing area fills,
 * statistical metrics (MIN/MAX/AVG), warning thresholds, and interactive crosshair inspection.
 * Demonstrates: Observer pattern (TickListener), Java2D advanced rendering, encapsulation.
 */
public class LiveChartPanel extends JPanel implements TickListener {

    private static final int MAX_HISTORY = 60;

    private String selectedSensorId = "TEMP-CORE";
    private String selectedUnit = "C";
    private double warnLimit = 45.0;
    private final List<Double> history = new ArrayList<>();
    private Point hoverPoint = null;

    public LiveChartPanel() {
        setBackground(Theme.PANEL_BG);
        setBorder(Theme.createBorder("LIVE SENSOR TELEMETRY & SPECTRUM"));
        setPreferredSize(new Dimension(420, 260));

        MouseAdapter hoverHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoverPoint = e.getPoint();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverPoint = null;
                repaint();
            }
        };
        addMouseListener(hoverHandler);
        addMouseMotionListener(hoverHandler);
    }

    public synchronized void setSelectedSensor(String sensorId) {
        if (sensorId == null || sensorId.equals(this.selectedSensorId)) {
            return;
        }
        this.selectedSensorId = sensorId;
        this.history.clear();

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
        hoverPoint = null;
        repaint();
    }

    @Override
    public synchronized void onTick(SimulationSnapshot snapshot) {
        if (snapshot.tick() <= 1) {
            history.clear();
        }

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

        int padLeft = 52;
        int padRight = 20;
        int padTop = 38;
        int padBottom = 30;

        int w = getWidth();
        int h = getHeight();
        int chartW = w - padLeft - padRight;
        int chartH = h - padTop - padBottom;

        if (chartW <= 0 || chartH <= 0) {
            g2.dispose();
            return;
        }

        // Inner Chart Box
        g2.setColor(new Color(0x0E, 0x14, 0x28));
        g2.fillRect(padLeft, padTop, chartW, chartH);
        g2.setColor(Theme.PANEL_BORDER);
        g2.drawRect(padLeft, padTop, chartW, chartH);

        // Header Metrics Bar
        double latest = history.isEmpty() ? 0 : history.get(history.size() - 1);
        double minVal = latest;
        double maxVal = latest;
        double sumVal = 0;
        for (double v : history) {
            if (v < minVal) minVal = v;
            if (v > maxVal) maxVal = v;
            sumVal += v;
        }
        double avgVal = history.isEmpty() ? 0 : sumVal / history.size();

        // Left Header: Selected Instrument
        g2.setFont(Theme.FONT_LABEL_BOLD);
        g2.setColor(Theme.ACCENT_CYAN);
        String title = String.format("SENSOR: %s  |  CURRENT: %.2f %s", selectedSensorId, latest, selectedUnit);
        g2.drawString(title, padLeft, padTop - 12);

        // Right Header: Stats Badges
        g2.setFont(Theme.FONT_MONO);
        g2.setColor(Theme.TEXT_MUTED);
        String stats = String.format(Locale.US, "MIN: %.1f | MAX: %.1f | AVG: %.1f", minVal, maxVal, avgVal);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(stats, padLeft + chartW - fm.stringWidth(stats), padTop - 12);

        if (history.isEmpty()) {
            g2.setFont(Theme.FONT_LABEL);
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString("SYNCHRONIZING TELEMETRY STREAM...", padLeft + 20, padTop + chartH / 2);
            g2.dispose();
            return;
        }

        // Y-Range
        double minY = minVal;
        double maxY = maxVal;
        if (warnLimit < minY) minY = warnLimit;
        if (warnLimit > maxY) maxY = warnLimit;

        double range = maxY - minY;
        if (range < 4.0) range = 4.0;
        minY -= range * 0.15;
        maxY += range * 0.15;
        range = maxY - minY;

        // Grid lines with coordinate labels
        int gridDivs = 4;
        g2.setFont(Theme.FONT_MONO);
        Stroke solidGrid = new BasicStroke(1.0f);
        Stroke dotGrid = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{2.0f, 4.0f}, 0.0f);

        for (int i = 0; i <= gridDivs; i++) {
            int y = padTop + (int) (chartH * (1.0 - (double) i / gridDivs));
            g2.setStroke(dotGrid);
            g2.setColor(new Color(0x1F, 0x2C, 0x54));
            g2.drawLine(padLeft, y, padLeft + chartW, y);

            double labelVal = minY + (range * i / gridDivs);
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(String.format(Locale.US, "%5.1f", labelVal), 6, y + 4);
        }

        // Warning Limit Threshold Line (Dashed Amber)
        if (warnLimit >= minY && warnLimit <= maxY) {
            int warnY = padTop + (int) (chartH * (1.0 - (warnLimit - minY) / range));
            Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6.0f, 4.0f}, 0.0f);
            g2.setStroke(dashed);
            g2.setColor(Theme.STATUS_WARNING);
            g2.drawLine(padLeft, warnY, padLeft + chartW, warnY);
            g2.drawString(String.format(Locale.US, "WARN LIMIT: %.1f %s", warnLimit, selectedUnit), padLeft + 8, warnY - 4);
        }

        // Compute XY Points
        int n = history.size();
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = padLeft + (int) ((double) i / (MAX_HISTORY - 1) * chartW);
            double v = history.get(i);
            ys[i] = padTop + (int) (chartH * (1.0 - (v - minY) / range));
        }

        // 1. Shaded Area Fill Under Curve
        if (n > 1) {
            Polygon area = new Polygon();
            area.addPoint(xs[0], padTop + chartH);
            for (int i = 0; i < n; i++) {
                area.addPoint(xs[i], ys[i]);
            }
            area.addPoint(xs[n - 1], padTop + chartH);

            GradientPaint gp = new GradientPaint(
                    0, padTop, new Color(0x00, 0xE5, 0xFF, 60),
                    0, padTop + chartH, new Color(0x00, 0xE5, 0xFF, 0)
            );
            g2.setPaint(gp);
            g2.fill(area);
        }

        // 2. Soft Glowing Line Halo
        g2.setColor(new Color(0x00, 0xE5, 0xFF, 65));
        g2.setStroke(new BasicStroke(4.5f));
        for (int i = 0; i < n - 1; i++) {
            g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
        }

        // 3. Crisp Core Telemetry Line
        g2.setColor(Theme.ACCENT_CYAN);
        g2.setStroke(new BasicStroke(2.0f));
        for (int i = 0; i < n - 1; i++) {
            g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
        }

        // 4. Latest Point Target Reticle
        if (n > 0) {
            int lx = xs[n - 1];
            int ly = ys[n - 1];
            g2.setColor(Theme.STATUS_NOMINAL);
            g2.fillOval(lx - 4, ly - 4, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawOval(lx - 4, ly - 4, 8, 8);
            g2.setColor(new Color(Theme.STATUS_NOMINAL.getRed(), Theme.STATUS_NOMINAL.getGreen(), Theme.STATUS_NOMINAL.getBlue(), 120));
            g2.drawOval(lx - 8, ly - 8, 16, 16);
        }

        // 5. Interactive Mouse Crosshair & Inspection Tooltip
        if (hoverPoint != null && hoverPoint.x >= padLeft && hoverPoint.x <= padLeft + chartW
                && hoverPoint.y >= padTop && hoverPoint.y <= padTop + chartH && n > 0) {
            int hoverIndex = (int) Math.round(((double) (hoverPoint.x - padLeft) / chartW) * (MAX_HISTORY - 1));
            if (hoverIndex >= 0 && hoverIndex < n) {
                int hx = xs[hoverIndex];
                int hy = ys[hoverIndex];

                // Vertical & Horizontal crosshair lines
                g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f));
                g2.setColor(new Color(0xE0, 0xE6, 0xFF, 160));
                g2.drawLine(hx, padTop, hx, padTop + chartH);
                g2.drawLine(padLeft, hy, padLeft + chartW, hy);

                // Tooltip badge
                String tip = String.format(Locale.US, "VAL: %.2f %s", history.get(hoverIndex), selectedUnit);
                int tipW = fm.stringWidth(tip) + 14;
                int tipX = Math.min(hx + 10, padLeft + chartW - tipW - 4);
                int tipY = Math.max(hy - 24, padTop + 4);

                g2.setColor(new Color(0x18, 0x22, 0x45, 230));
                g2.fillRoundRect(tipX, tipY, tipW, 20, 6, 6);
                g2.setColor(Theme.ACCENT_CYAN);
                g2.drawRoundRect(tipX, tipY, tipW, 20, 6, 6);
                g2.drawString(tip, tipX + 7, tipY + 14);
            }
        }

        g2.dispose();
    }
}
