package com.orbitalix.gui;

import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.TickListener;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.util.Locale;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * 2D World Ground Track & Orbit Radar panel showing real-time sub-satellite position,
 * orbital trajectory curve, day/night terminator, and Deep Space Network (DSN) ground stations.
 * Demonstrates: Java2D vector graphics, trigonometric orbital projection, Observer pattern (TickListener).
 */
public class OrbitMapPanel extends JPanel implements TickListener {

    private int currentTick = 0;
    private double orbitPhase = 0.0;
    private float radarSweepAngle = 0f;
    private final Timer radarTimer;

    // Ground station locations: [Lat, Lon, Name]
    private static final Object[][] GROUND_STATIONS = {
            { 35.4, -116.9, "GOLDSTONE DSN" },
            { 40.4, -4.2, "MADRID DSN" },
            { -35.4, 148.9, "CANBERRA DSN" }
    };

    public OrbitMapPanel() {
        setBackground(Theme.BG_DARK);
        setBorder(Theme.createBorder("SPACECRAFT ORBIT MAP & GROUND STATIONS"));
        setPreferredSize(new Dimension(450, 420));

        radarTimer = new Timer(40, e -> {
            radarSweepAngle += 0.06f;
            if (radarSweepAngle > 2 * Math.PI) {
                radarSweepAngle = 0f;
            }
            repaint();
        });
        radarTimer.start();
    }

    @Override
    public void onTick(SimulationSnapshot snapshot) {
        this.currentTick = snapshot.tick();
        this.orbitPhase = (currentTick * 0.04) % (2 * Math.PI);
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

        int mapPadLeft = 35;
        int mapPadRight = 20;
        int mapPadTop = 32;
        int mapPadBottom = 42;

        int mapW = w - mapPadLeft - mapPadRight;
        int mapH = h - mapPadTop - mapPadBottom;

        if (mapW <= 0 || mapH <= 0) {
            g2.dispose();
            return;
        }

        // Deep-ocean gradient makes the map easier to read without competing with telemetry.
        g2.setPaint(new GradientPaint(mapPadLeft, mapPadTop, new Color(0x0B, 0x1C, 0x35),
                mapPadLeft, mapPadTop + mapH, new Color(0x08, 0x10, 0x21)));
        g2.fillRect(mapPadLeft, mapPadTop, mapW, mapH);
        g2.setColor(Theme.PANEL_BORDER);
        g2.drawRect(mapPadLeft, mapPadTop, mapW, mapH);

        // Latitude & Longitude Coordinate Grid
        g2.setFont(Theme.FONT_MONO);
        Stroke gridStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{2.0f, 4.0f}, 0.0f);
        g2.setStroke(gridStroke);

        // Longitudes (Meridians)
        for (int lon = -180; lon <= 180; lon += 60) {
            int x = mapPadLeft + (int) (((lon + 180.0) / 360.0) * mapW);
            g2.setColor(new Color(0x1E, 0x2B, 0x50));
            g2.drawLine(x, mapPadTop, x, mapPadTop + mapH);
            if (lon != 180) {
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(String.format(Locale.US, "%d°", lon), x + 2, mapPadTop + mapH + 12);
            }
        }

        // Latitudes (Parallels)
        for (int lat = -60; lat <= 60; lat += 30) {
            int y = mapPadTop + (int) (((90.0 - lat) / 180.0) * mapH);
            g2.setColor(new Color(0x1E, 0x2B, 0x50));
            g2.drawLine(mapPadLeft, y, mapPadLeft + mapW, y);
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(String.format(Locale.US, "%+d°", lat), 4, y + 4);
        }

        // Equator (Solid Green Line)
        int eqY = mapPadTop + mapH / 2;
        g2.setColor(new Color(0x00, 0xE5, 0xFF, 90));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(mapPadLeft, eqY, mapPadLeft + mapW, eqY);

        // Stylized Vector Landmasses
        drawContinents(g2, mapPadLeft, mapPadTop, mapW, mapH);

        // Draw Ground Stations and Coverage Cones
        for (Object[] station : GROUND_STATIONS) {
            double sLat = (double) station[0];
            double sLon = (double) station[1];
            String name = (String) station[2];

            Point sp = geoToScreen(sLat, sLon, mapPadLeft, mapPadTop, mapW, mapH);

            // Coverage ring (radius ~ 45 km)
            g2.setColor(new Color(0x00, 0xE6, 0x76, 35));
            g2.fillOval(sp.x - 28, sp.y - 28, 56, 56);
            g2.setColor(new Color(0x00, 0xE6, 0x76, 120));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawOval(sp.x - 28, sp.y - 28, 56, 56);

            // Ground station marker
            g2.setColor(Theme.STATUS_NOMINAL);
            g2.fillOval(sp.x - 3, sp.y - 3, 6, 6);
            g2.drawString(name, sp.x + 6, sp.y - 4);
        }

        // Draw LEO Orbital Ground Track (Sine wave with inclination 97.4°)
        double inclination = 65.0; // visual representation
        g2.setStroke(new BasicStroke(2.0f));
        g2.setColor(new Color(0x00, 0xE5, 0xFF, 160));

        Point prevPt = null;
        for (int step = 0; step <= 360; step += 4) {
            double angle = Math.toRadians(step);
            double trackLat = inclination * Math.sin(angle);
            double trackLon = ((step - 180 + Math.toDegrees(orbitPhase) * 0.4) % 360) - 180;
            if (trackLon < -180) trackLon += 360;

            Point curPt = geoToScreen(trackLat, trackLon, mapPadLeft, mapPadTop, mapW, mapH);
            if (prevPt != null && Math.abs(curPt.x - prevPt.x) < mapW / 2) {
                g2.drawLine(prevPt.x, prevPt.y, curPt.x, curPt.y);
            }
            prevPt = curPt;
        }

        // Calculate Current Sub-Satellite Coordinate
        double satLat = inclination * Math.sin(orbitPhase);
        double satLon = ((Math.toDegrees(orbitPhase) * 1.8) % 360) - 180;
        Point satScreen = geoToScreen(satLat, satLon, mapPadLeft, mapPadTop, mapW, mapH);
        int vehicleX = Math.max(mapPadLeft + 22, Math.min(mapPadLeft + mapW - 22, satScreen.x));
        if (vehicleX != satScreen.x) {
            g2.setColor(new Color(Theme.ACCENT_CYAN.getRed(), Theme.ACCENT_CYAN.getGreen(),
                    Theme.ACCENT_CYAN.getBlue(), 110));
            g2.drawLine(satScreen.x, satScreen.y, vehicleX, satScreen.y);
        }

        // Spacecraft radar ping & pulse rings
        int pingRadius = (int) (12 + (radarSweepAngle / (2 * Math.PI)) * 26);
        g2.setColor(new Color(0x00, 0xE5, 0xFF, (int) (220 * (1.0f - (radarSweepAngle / (float) (2 * Math.PI))))));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(vehicleX - pingRadius, satScreen.y - pingRadius, pingRadius * 2, pingRadius * 2);

        // Point the vehicle along its projected direction of travel.
        double course = Math.atan2(-0.45 * Math.cos(orbitPhase), 1.0);
        drawSpacecraft(g2, vehicleX, satScreen.y, course);

        // Spacecraft label
        int labelX = vehicleX + 23;
        if (labelX + 104 > mapPadLeft + mapW - 4) {
            labelX = vehicleX - 112;
        }
        labelX = Math.max(mapPadLeft + 4, labelX);
        int labelY = Math.max(mapPadTop + 4, Math.min(mapPadTop + mapH - 28, satScreen.y - 21));
        g2.setColor(new Color(0x08, 0x12, 0x27, 225));
        g2.fillRoundRect(labelX, labelY, 104, 24, 10, 10);
        g2.setColor(new Color(Theme.ACCENT_CYAN.getRed(), Theme.ACCENT_CYAN.getGreen(),
                Theme.ACCENT_CYAN.getBlue(), 150));
        g2.drawRoundRect(labelX, labelY, 104, 24, 10, 10);
        g2.setFont(Theme.FONT_LABEL_BOLD);
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.drawString("ORBITALIX-1", labelX + 8, labelY + 16);

        // Bottom telemetry bar
        g2.setFont(Theme.FONT_MONO);
        g2.setColor(Theme.ACCENT_CYAN);
        String subSatInfo = String.format(Locale.US,
                "SPACECRAFT POSITION  LAT %+05.1f°  |  LON %+06.1f°  |  ALT 548 KM",
                satLat, satLon);
        g2.drawString(subSatInfo, mapPadLeft, h - 14);

        g2.dispose();
    }

    private void drawSpacecraft(Graphics2D g2, int x, int y, double course) {
        Graphics2D craft = (Graphics2D) g2.create();
        craft.translate(x, y);
        craft.rotate(course);
        craft.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Engine plume
        craft.setColor(new Color(0xFF, 0xB3, 0x00, 150));
        craft.fillPolygon(new int[] { -12, -22, -12 }, new int[] { -3, 0, 3 }, 3);
        craft.setColor(new Color(0x00, 0xE5, 0xFF, 170));
        craft.fillPolygon(new int[] { -12, -18, -12 }, new int[] { -1, 0, 1 }, 3);

        // Solar wings with visible panel cells
        craft.setColor(new Color(0x13, 0x4F, 0x82));
        craft.fillRoundRect(-7, -19, 13, 11, 3, 3);
        craft.fillRoundRect(-7, 8, 13, 11, 3, 3);
        craft.setColor(new Color(0x57, 0xC8, 0xFF));
        craft.drawRoundRect(-7, -19, 13, 11, 3, 3);
        craft.drawRoundRect(-7, 8, 13, 11, 3, 3);
        craft.drawLine(-2, -18, -2, -9);
        craft.drawLine(2, -18, 2, -9);
        craft.drawLine(-2, 9, -2, 18);
        craft.drawLine(2, 9, 2, 18);
        craft.setColor(new Color(0xB5, 0xE9, 0xFF));
        craft.drawLine(-5, -5, 10, -5);
        craft.drawLine(-5, 5, 10, 5);

        // Main hull and pointed nose
        craft.setColor(new Color(0xD6, 0xE8, 0xF5));
        craft.fillPolygon(new int[] { -11, 7, 15, 7 }, new int[] { -5, -5, 0, 5 }, 4);
        craft.setColor(new Color(0x00, 0xE5, 0xFF));
        craft.drawPolygon(new int[] { -11, 7, 15, 7 }, new int[] { -5, -5, 0, 5 }, 4);
        craft.setColor(new Color(0x15, 0x62, 0x83));
        craft.fillOval(1, -2, 5, 4);
        craft.setColor(new Color(0xE0, 0xE6, 0xFF));
        craft.drawLine(5, -4, 8, -9);
        craft.drawArc(6, -13, 7, 7, 25, 115);

        craft.dispose();
    }

    private Point geoToScreen(double lat, double lon, int ox, int oy, int w, int h) {
        int x = ox + (int) (((lon + 180.0) / 360.0) * w);
        int y = oy + (int) (((90.0 - lat) / 180.0) * h);
        return new Point(x, y);
    }

    private void drawContinents(Graphics2D g2, int ox, int oy, int w, int h) {
        g2.setColor(new Color(0x20, 0x3A, 0x59, 185));
        drawLandPolygon(g2, ox, oy, w, h, new double[][] {
            { 70, -140 }, { 62, -128 }, { 55, -130 }, { 48, -124 }, { 38, -123 },
            { 30, -115 }, { 22, -106 }, { 15, -98 }, { 9, -83 }, { 18, -78 },
            { 25, -81 }, { 32, -78 }, { 42, -70 }, { 48, -60 }, { 56, -62 },
            { 62, -78 }, { 68, -96 }, { 72, -118 }
        });
        drawLandPolygon(g2, ox, oy, w, h, new double[][] {
            { 12, -80 }, { 6, -75 }, { -5, -78 }, { -15, -73 }, { -24, -70 },
            { -36, -72 }, { -55, -68 }, { -50, -60 }, { -40, -62 }, { -26, -54 },
            { -10, -38 }, { 0, -49 }, { 8, -61 }
        });
        drawLandPolygon(g2, ox, oy, w, h, new double[][] {
            { 71, -10 }, { 65, 5 }, { 60, 25 }, { 65, 45 }, { 60, 65 }, { 57, 82 },
            { 60, 105 }, { 54, 130 }, { 48, 145 }, { 40, 135 }, { 35, 120 },
            { 25, 112 }, { 20, 100 }, { 8, 105 }, { 5, 80 }, { 15, 65 }, { 25, 55 },
            { 35, 45 }, { 40, 28 }, { 48, 15 }, { 55, 5 }
        });
        drawLandPolygon(g2, ox, oy, w, h, new double[][] {
            { 35, -10 }, { 32, 10 }, { 20, 25 }, { 10, 35 }, { 0, 42 }, { -12, 50 },
            { -25, 45 }, { -35, 28 }, { -30, 15 }, { -18, 10 }, { -5, 5 }, { 5, -5 },
            { 18, -16 }, { 28, -12 }
        });
        drawLandPolygon(g2, ox, oy, w, h, new double[][] {
            { -12, 112 }, { -15, 130 }, { -24, 153 }, { -36, 150 }, { -40, 138 },
            { -34, 118 }, { -25, 113 }
        });
        drawLandPolygon(g2, ox, oy, w, h, new double[][] {
            { -70, -180 }, { -76, -120 }, { -72, -60 }, { -78, 0 }, { -73, 60 },
            { -77, 120 }, { -70, 180 }
        });
    }

    private void drawLandPolygon(Graphics2D g2, int ox, int oy, int w, int h, double[][] coordinates) {
        Path2D polygon = new Path2D.Double();
        for (int i = 0; i < coordinates.length; i++) {
            Point point = geoToScreen(coordinates[i][0], coordinates[i][1], ox, oy, w, h);
            if (i == 0) {
                polygon.moveTo(point.x, point.y);
            } else {
                polygon.lineTo(point.x, point.y);
            }
        }
        polygon.closePath();
        g2.fill(polygon);
        g2.setColor(new Color(0x42, 0x7A, 0x91, 110));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(polygon);
        g2.setColor(new Color(0x20, 0x3A, 0x59, 185));
    }
}
