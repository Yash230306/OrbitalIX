package com.orbitalix.gui;

import com.orbitalix.model.HealthStatus;
import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.SubsystemSnapshot;
import com.orbitalix.simulation.TickListener;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;

/**
 * Large-scale 2D spacecraft illustration with live subsystem health overlays.
 * Demonstrates: Java2D composition, encapsulation, and the Observer pattern.
 */
public class Spacecraft2DPanel extends JPanel implements TickListener {

    private final Map<String, HealthStatus> health = new HashMap<>();
    private int tick;

    public Spacecraft2DPanel() {
        setBackground(Theme.BG_DARK);
        setBorder(Theme.createBorder("ORBITALIX-1  ·  2D SPACECRAFT"));
        setPreferredSize(new Dimension(450, 420));
        health.put("Power", HealthStatus.NOMINAL);
        health.put("Thermal", HealthStatus.NOMINAL);
        health.put("Communication", HealthStatus.NOMINAL);
        health.put("Attitude", HealthStatus.NOMINAL);
    }

    @Override
    public void onTick(SimulationSnapshot snapshot) {
        tick = snapshot.tick();
        for (SubsystemSnapshot subsystem : snapshot.subsystems()) {
            health.put(subsystem.name(), subsystem.status());
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        paintSpace(g2, width, height);
        paintEarthHorizon(g2, width, height);
        paintCraft(g2, width, height);
        paintLabels(g2, width, height);

        g2.dispose();
    }

    private void paintSpace(Graphics2D g2, int width, int height) {
        g2.setPaint(new GradientPaint(0, 0, new Color(0x10, 0x1B, 0x3B),
                width, height, new Color(0x06, 0x0B, 0x18)));
        g2.fillRect(0, 0, width, height);

        for (int i = 0; i < 80; i++) {
            int x = Math.floorMod(i * 83 + 31, Math.max(1, width));
            int y = Math.floorMod(i * 47 + 17, Math.max(1, height));
            int alpha = 65 + Math.floorMod(i * 37, 150);
            g2.setColor(new Color(0xBD, 0xDC, 0xFF, alpha));
            int size = i % 11 == 0 ? 2 : 1;
            g2.fillOval(x, y, size, size);
        }

        g2.setColor(new Color(0x00, 0xE5, 0xFF, 35));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawArc(width / 2 - 170, 12, 340, 210, 15, 145);
        g2.setColor(new Color(0x9D, 0xD8, 0xF5, 180));
        g2.drawString("LOW EARTH ORBIT  ·  T+" + String.format("%05d", tick),
                Math.max(18, width - 190), 30);
    }

    private void paintEarthHorizon(Graphics2D g2, int width, int height) {
        int horizonY = (int) (height * 0.79);
        g2.setPaint(new GradientPaint(0, horizonY, new Color(0x11, 0x66, 0x9B),
                0, height, new Color(0x08, 0x13, 0x32)));
        g2.fill(new Ellipse2D.Double(-width * 0.28, horizonY, width * 1.56, height * 0.66));

        g2.setColor(new Color(0x00, 0xE5, 0xFF, 175));
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawArc((int) (-width * 0.28), horizonY - 5, (int) (width * 1.56), (int) (height * 0.66), 0, 180);
        g2.setFont(Theme.FONT_MONO);
        g2.setColor(new Color(0xD4, 0xE8, 0xFF, 190));
        g2.drawString("EARTH  ·  548 KM BELOW", 18, height - 18);
    }

    private void paintCraft(Graphics2D g2, int width, int height) {
        Graphics2D craft = (Graphics2D) g2.create();
        craft.translate(width * 0.50, height * 0.48);
        double scale = Math.max(0.45, Math.min(0.72,
                Math.min((width / 2.0 - 28) / 295.0, (height * 0.45 - 22) / 154.0)));
        craft.scale(scale, scale);
        craft.rotate(-0.09);

        paintSolarWing(craft, -235, -55, "Power");
        paintSolarWing(craft, 115, -55, "Power");
        paintBoom(craft, -122, -6);
        paintBoom(craft, 102, -6);
        paintMainBus(craft);
        paintThermalRadiator(craft);
        paintAntenna(craft);
        paintPropulsion(craft);
        paintAttitudeRing(craft);

        craft.dispose();
    }

    private void paintSolarWing(Graphics2D g2, int x, int y, String subsystem) {
        Color status = colorFor(subsystem);
        g2.setColor(new Color(0x0C, 0x28, 0x56));
        g2.fillRoundRect(x, y, 120, 110, 8, 8);
        g2.setColor(status);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawRoundRect(x, y, 120, 110, 8, 8);

        for (int column = 1; column < 4; column++) {
            g2.drawLine(x + column * 30, y + 3, x + column * 30, y + 107);
        }
        for (int row = 1; row < 3; row++) {
            g2.drawLine(x + 3, y + row * 36, x + 117, y + row * 36);
        }
        g2.setColor(new Color(status.getRed(), status.getGreen(), status.getBlue(), 38));
        g2.fillRoundRect(x, y, 120, 110, 8, 8);
    }

    private void paintBoom(Graphics2D g2, int x1, int x2) {
        g2.setColor(new Color(0x9A, 0xB6, 0xCE));
        g2.setStroke(new BasicStroke(5.0f));
        g2.drawLine(x1, 0, x2, 0);
        g2.setStroke(new BasicStroke(1.3f));
        g2.drawLine(x1, -8, x2, 8);
        g2.drawLine(x1, 8, x2, -8);
    }

    private void paintMainBus(Graphics2D g2) {
        Color status = colorFor("Thermal");
        g2.setPaint(new GradientPaint(-58, -60, new Color(0xD6, 0xE4, 0xEC),
                60, 65, new Color(0x55, 0x67, 0x82)));
        g2.fillRoundRect(-62, -68, 124, 136, 18, 18);
        g2.setColor(status);
        g2.setStroke(new BasicStroke(3.0f));
        g2.drawRoundRect(-62, -68, 124, 136, 18, 18);

        g2.setColor(new Color(0x0C, 0x16, 0x2B));
        g2.fillRoundRect(-43, -45, 86, 87, 12, 12);
        g2.setColor(new Color(0x36, 0x56, 0x77));
        g2.drawRoundRect(-43, -45, 86, 87, 12, 12);

        g2.setColor(new Color(0x69, 0xD8, 0xF5));
        g2.fillOval(-11, -18, 22, 22);
        g2.setColor(new Color(0xE0, 0xE6, 0xFF));
        g2.fillOval(-5, -12, 10, 10);
        g2.setFont(Theme.FONT_MONO_BOLD);
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.drawString("ORBITALIX", -34, 29);
        g2.setFont(Theme.FONT_MONO);
        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString("FLIGHT BUS", -32, 45);
    }

    private void paintThermalRadiator(Graphics2D g2) {
        Color status = colorFor("Thermal");
        g2.setColor(new Color(0x2A, 0x3D, 0x58));
        g2.fillRoundRect(-45, -91, 90, 19, 7, 7);
        g2.setColor(status);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawRoundRect(-45, -91, 90, 19, 7, 7);
        for (int x = -34; x <= 34; x += 17) {
            g2.drawLine(x, -88, x, -75);
        }
    }

    private void paintAntenna(Graphics2D g2) {
        Color status = colorFor("Communication");
        g2.setColor(new Color(0xB8, 0xC8, 0xD8));
        g2.setStroke(new BasicStroke(3.0f));
        g2.drawLine(31, -42, 91, -99);
        g2.drawLine(85, -92, 96, -104);
        g2.setColor(new Color(status.getRed(), status.getGreen(), status.getBlue(), 65));
        g2.fillOval(74, -122, 54, 37);
        g2.setColor(status);
        g2.setStroke(new BasicStroke(3.0f));
        g2.drawArc(74, -122, 54, 37, 210, 120);
        g2.fillOval(91, -108, 8, 8);
        g2.drawLine(84, -112, 80, -118);
        g2.drawLine(100, -114, 102, -122);
    }

    private void paintPropulsion(Graphics2D g2) {
        Color status = colorFor("Attitude");
        g2.setColor(new Color(0x93, 0xA8, 0xBC));
        g2.fillPolygon(new int[] { -30, 30, 21, -21 }, new int[] { 66, 66, 99, 99 }, 4);
        g2.setColor(status);
        g2.drawPolygon(new int[] { -30, 30, 21, -21 }, new int[] { 66, 66, 99, 99 }, 4);

        g2.setPaint(new GradientPaint(0, 98, new Color(0xFF, 0xD1, 0x68, 210),
                0, 150, new Color(0x00, 0xE5, 0xFF, 0)));
        g2.fillPolygon(new int[] { -17, 17, 9, 0, -9 }, new int[] { 98, 98, 134, 154, 134 }, 5);
    }

    private void paintAttitudeRing(Graphics2D g2) {
        Color status = colorFor("Attitude");
        g2.setColor(new Color(status.getRed(), status.getGreen(), status.getBlue(), 100));
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10.0f, new float[] { 5.0f, 5.0f }, 0.0f));
        g2.drawOval(-88, -92, 176, 184);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawLine(-85, -5, -64, -5);
        g2.drawLine(64, 5, 85, 5);
    }

    private void paintLabels(Graphics2D g2, int width, int height) {
        drawCallout(g2, 18, 58, "POWER", health.get("Power"));
        drawCallout(g2, 18, height - 58, "SOLAR ARRAYS", health.get("Power"));
        drawCallout(g2, width - 162, 58, "COMMS DISH", health.get("Communication"));
        drawCallout(g2, width - 162, height - 58, "THERMAL / BUS", health.get("Thermal"));
    }

    private void drawCallout(Graphics2D g2, int x, int y, String text, HealthStatus status) {
        Color color = Theme.getHealthColor(status);
        g2.setColor(new Color(0x08, 0x12, 0x27, 225));
        g2.fillRoundRect(x, y - 17, 144, 25, 10, 10);
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 190));
        g2.drawRoundRect(x, y - 17, 144, 25, 10, 10);
        g2.setFont(Theme.FONT_LABEL_BOLD);
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.drawString(text, x + 9, y);
        g2.setColor(color);
        g2.fillOval(x + 127, y - 8, 7, 7);
    }

    private Color colorFor(String subsystem) {
        return Theme.getHealthColor(health.getOrDefault(subsystem, HealthStatus.NOMINAL));
    }
}
