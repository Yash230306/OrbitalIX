package com.orbitalix.gui;

import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.TickListener;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
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
        setBorder(Theme.createBorder("ORBITAL GROUND TRACK & DSN COVERAGE"));
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

        // Ocean Background
        g2.setColor(new Color(0x0A, 0x12, 0x24));
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

        // Satellite Radar Ping & Pulse Rings
        int pingRadius = (int) (12 + (radarSweepAngle / (2 * Math.PI)) * 26);
        g2.setColor(new Color(0x00, 0xE5, 0xFF, (int) (220 * (1.0f - (radarSweepAngle / (float) (2 * Math.PI))))));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(satScreen.x - pingRadius, satScreen.y - pingRadius, pingRadius * 2, pingRadius * 2);

        // Satellite Marker Icon
        g2.setColor(Theme.ACCENT_CYAN);
        g2.fillRect(satScreen.x - 4, satScreen.y - 4, 8, 8);
        g2.setColor(Color.WHITE);
        g2.fillOval(satScreen.x - 2, satScreen.y - 2, 4, 4);

        // Satellite Label
        g2.setFont(Theme.FONT_LABEL_BOLD);
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.drawString("ORBITALIX-1", satScreen.x + 10, satScreen.y - 6);

        // Bottom Telemetry Bar
        g2.setFont(Theme.FONT_MONO);
        g2.setColor(Theme.ACCENT_CYAN);
        String subSatInfo = String.format(Locale.US,
                "SUB-SATELLITE POSITION: LAT %+05.1f° | LON %+06.1f° | ALT: 548.2 KM | GROUND TRACK SPEED: 7.61 KM/S",
                satLat, satLon);
        g2.drawString(subSatInfo, mapPadLeft, h - 14);

        g2.dispose();
    }

    private Point geoToScreen(double lat, double lon, int ox, int oy, int w, int h) {
        int x = ox + (int) (((lon + 180.0) / 360.0) * w);
        int y = oy + (int) (((90.0 - lat) / 180.0) * h);
        return new Point(x, y);
    }

    private void drawContinents(Graphics2D g2, int ox, int oy, int w, int h) {
        g2.setColor(new Color(0x18, 0x2A, 0x48));

        // Simplified representative landmass boxes & polygons for clean vector look
        // North America
        drawLandBox(g2, 25, 70, -140, -60, ox, oy, w, h);
        // South America
        drawLandBox(g2, -50, 10, -80, -35, ox, oy, w, h);
        // Eurasia
        drawLandBox(g2, 35, 72, -10, 150, ox, oy, w, h);
        // Africa
        drawLandBox(g2, -34, 35, -18, 50, ox, oy, w, h);
        // Australia
        drawLandBox(g2, -38, -12, 115, 152, ox, oy, w, h);
        // Antarctica
        drawLandBox(g2, -85, -68, -180, 180, ox, oy, w, h);
    }

    private void drawLandBox(Graphics2D g2, double latS, double latN, double lonW, double lonE,
                             int ox, int oy, int w, int h) {
        Point p1 = geoToScreen(latN, lonW, ox, oy, w, h);
        Point p2 = geoToScreen(latS, lonE, ox, oy, w, h);
        g2.fillRoundRect(p1.x, p1.y, Math.max(4, p2.x - p1.x), Math.max(4, p2.y - p1.y), 10, 10);
    }
}
