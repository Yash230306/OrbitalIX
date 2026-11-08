package com.orbitalix.gui;

import com.orbitalix.model.HealthStatus;
import com.orbitalix.simulation.SensorSnapshot;
import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.SubsystemSnapshot;
import com.orbitalix.simulation.TickListener;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * High-performance 3D interactive spacecraft visualization rendered entirely with Java2D.
 * Features software perspective projection, Lambertian directional lighting, depth-sorting
 * (Painter's algorithm), 3D starfield parallax, user orbit controls (mouse drag & zoom),
 * and real-time 3D-to-2D projected telemetry callout HUDs.
 * Demonstrates: Java2D 3D computer graphics pipeline, Observer pattern (TickListener), User Interaction.
 */
public class SatellitePanel extends JPanel implements TickListener {

    private final List<Polygon3D> baseMesh;
    private final Map<String, HealthStatus> subsystemHealth = new HashMap<>();
    private final Map<String, String> subsystemReadings = new HashMap<>();
    private Consumer<String> onSensorSelected;

    // 3D Camera & Orbit Controls
    private double pitch = 0.28;
    private double yaw = -0.52;
    private double roll = 0.0;
    private double fov = 520.0;
    private double cameraDistance = 380.0;
    private final Vec3 lightDir = new Vec3(0.55, 0.75, -0.65).normalize();

    private Point dragStartPoint = null;
    private boolean userInteracting = false;
    private int idleTicksSinceInteraction = 0;

    // 3D Starfield with depth
    private static final int STAR_COUNT = 90;
    private final Vec3[] stars = new Vec3[STAR_COUNT];
    private final float[] starBrightness = new float[STAR_COUNT];

    // Pulsing animation for critical state
    private float pulsePhase = 0f;
    private boolean pulseUp = true;
    private final Timer animationTimer;

    // Interactive 2D Screen Callout Hitboxes
    private final Map<String, Rectangle> calloutBounds = new HashMap<>();

    public SatellitePanel() {
        setBackground(Theme.BG_DARK);
        setBorder(Theme.createBorder("3D SPACECRAFT SYSTEMS"));
        setPreferredSize(new Dimension(450, 420));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        baseMesh = Satellite3DModel.buildMesh();

        // Initialize 3D starfield coordinates around the satellite
        Random rng = new Random(101);
        for (int i = 0; i < STAR_COUNT; i++) {
            double sx = (rng.nextDouble() - 0.5) * 800;
            double sy = (rng.nextDouble() - 0.5) * 800;
            double sz = 150 + rng.nextDouble() * 500;
            stars[i] = new Vec3(sx, sy, sz);
            starBrightness[i] = 0.3f + rng.nextFloat() * 0.7f;
        }

        // Initialize default health values
        subsystemHealth.put("Power", HealthStatus.NOMINAL);
        subsystemHealth.put("Thermal", HealthStatus.NOMINAL);
        subsystemHealth.put("Communication", HealthStatus.NOMINAL);
        subsystemHealth.put("Attitude", HealthStatus.NOMINAL);

        subsystemReadings.put("Power", "28.0 V");
        subsystemReadings.put("Thermal", "25.0 C");
        subsystemReadings.put("Communication", "96 %");
        subsystemReadings.put("Attitude", "0.00 deg/s");

        // Mouse Drag to Orbit in 3D
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getPoint();
                userInteracting = true;
                idleTicksSinceInteraction = 0;

                // Check click on 2D HUD callouts
                for (Map.Entry<String, Rectangle> entry : calloutBounds.entrySet()) {
                    if (entry.getValue().contains(e.getPoint())) {
                        notifySelection(sensorIdForSubsystem(entry.getKey()));
                        return;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStartPoint = null;
                userInteracting = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint != null) {
                    int dx = e.getX() - dragStartPoint.x;
                    int dy = e.getY() - dragStartPoint.y;
                    yaw += dx * 0.008;
                    pitch += dy * 0.008;
                    dragStartPoint = e.getPoint();
                    idleTicksSinceInteraction = 0;
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Zoom in / out
                fov = Math.max(320.0, Math.min(780.0, fov - e.getWheelRotation() * 28.0));
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Click to select subsystem
                Point p = e.getPoint();
                for (Map.Entry<String, Rectangle> entry : calloutBounds.entrySet()) {
                    if (entry.getValue().contains(p)) {
                        notifySelection(sensorIdForSubsystem(entry.getKey()));
                        return;
                    }
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);

        // Real-time rendering animation loop (smooth 3D orbit & pulses)
        animationTimer = new Timer(33, e -> {
            // Gentle continuous idle rotation when user is not manually rotating
            if (!userInteracting) {
                idleTicksSinceInteraction++;
                if (idleTicksSinceInteraction > 30) {
                    yaw += 0.006;
                    pitch = 0.24 + 0.08 * Math.sin(System.currentTimeMillis() * 0.0008);
                }
            }

            // Pulse phase calculation
            if (pulseUp) {
                pulsePhase += 0.06f;
                if (pulsePhase >= 1.0f) pulseUp = false;
            } else {
                pulsePhase -= 0.06f;
                if (pulsePhase <= 0.2f) pulseUp = true;
            }
            repaint();
        });
        animationTimer.start();
    }

    private String sensorIdForSubsystem(String sub) {
        return switch (sub) {
            case "Power" -> "VOLT-BUS";
            case "Thermal" -> "TEMP-CORE";
            case "Communication" -> "SIG-ANT";
            case "Attitude" -> "GYRO-PRI";
            default -> "TEMP-CORE";
        };
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

        // 1. Draw 3D Space Background & Parallax Starfield
        draw3DStarfield(g2, w, h);

        // 2. Transform 3D mesh according to current pitch, yaw, roll
        List<Polygon3D> transformedPolys = new ArrayList<>(baseMesh.size());
        for (Polygon3D p : baseMesh) {
            transformedPolys.add(p.transform(pitch, yaw, roll));
        }

        // 3. Painter's Algorithm: Sort polygons by average Z (furthest drawn first)
        Collections.sort(transformedPolys);

        // 4. Render 3D Polygons
        for (Polygon3D poly : transformedPolys) {
            if (!poly.isVisible()) continue;

            Polygon screenPoly = poly.toScreenPolygon(fov, cameraDistance, w, h);
            HealthStatus status = subsystemHealth.get(poly.getSubsystemName());
            Color dynamicColor = resolveDynamicColor(poly, status);

            // Shaded face fill
            Color shaded = poly.computeShadedColor(dynamicColor, lightDir);
            g2.setColor(shaded);
            g2.fill(screenPoly);

            // Glowing edge outline
            Color edgeColor = resolveEdgeColor(poly, status);
            g2.setColor(edgeColor);
            g2.setStroke(poly.isEmissive() ? new BasicStroke(1.8f) : new BasicStroke(1.0f));
            g2.draw(screenPoly);
        }

        // 5. Draw 3D-to-2D Projected Telemetry Callouts
        drawCallouts(g2, w, h);

        // 6. Draw Aerospace HUD Overlay (angles, coordinates, instructions)
        drawHudOverlay(g2, w, h);

        g2.dispose();
    }

    private void draw3DStarfield(Graphics2D g2, int w, int h) {
        for (int i = 0; i < STAR_COUNT; i++) {
            Vec3 rotStar = stars[i].rotateY(yaw * 0.15).rotateX(pitch * 0.15);
            Point pt = rotStar.project(fov, cameraDistance + 100, w, h);
            if (pt.x >= 0 && pt.x < w && pt.y >= 0 && pt.y < h) {
                int alpha = (int) (starBrightness[i] * 230);
                g2.setColor(new Color(230, 240, 255, Math.min(255, Math.max(30, alpha))));
                int sz = (i % 7 == 0) ? 2 : 1;
                g2.fillRect(pt.x, pt.y, sz, sz);
            }
        }
    }

    private void drawCallouts(Graphics2D g2, int w, int h) {
        calloutBounds.clear();

        // 3D Anchor positions for each subsystem
        renderSingleCallout(g2, w, h, "Power", new Vec3(-145, 0, 0), "PWR", -110, -35);
        renderSingleCallout(g2, w, h, "Thermal", new Vec3(0, 0, 55), "THM", 70, -25);
        renderSingleCallout(g2, w, h, "Communication", new Vec3(0, 95, 0), "COM", 65, -60);
        renderSingleCallout(g2, w, h, "Attitude", new Vec3(0, -85, 0), "ATT", -110, 45);
    }

    private void renderSingleCallout(Graphics2D g2, int w, int h, String subName,
                                     Vec3 modelAnchor, String tag, int offsetX, int offsetY) {
        Vec3 transformedAnchor = modelAnchor.rotateX(pitch).rotateY(yaw).rotateZ(roll);
        Point screenPt = transformedAnchor.project(fov, cameraDistance, w, h);

        int badgeX = screenPt.x + offsetX;
        int badgeY = screenPt.y + offsetY;
        int badgeW = 118;
        int badgeH = 28;

        // Keep within bounds
        badgeX = Math.max(10, Math.min(w - badgeW - 10, badgeX));
        badgeY = Math.max(25, Math.min(h - badgeH - 25, badgeY));

        Rectangle bounds = new Rectangle(badgeX, badgeY, badgeW, badgeH);
        calloutBounds.put(subName, bounds);

        HealthStatus status = subsystemHealth.getOrDefault(subName, HealthStatus.NOMINAL);
        Color statusColor = Theme.getHealthColor(status);
        String reading = subsystemReadings.getOrDefault(subName, "--");

        // Thin connector line from 3D projected point to HUD badge
        g2.setColor(new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 120));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawLine(screenPt.x, screenPt.y, badgeX + badgeW / 2, badgeY + badgeH / 2);
        g2.fillOval(screenPt.x - 2, screenPt.y - 2, 4, 4);

        // Badge Container (glassmorphic dark box with neon border)
        g2.setColor(new Color(0x10, 0x18, 0x30, 225));
        g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);
        g2.setColor(statusColor);
        g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);

        // Text
        g2.setFont(Theme.FONT_MONO_BOLD);
        g2.setColor(statusColor);
        g2.drawString("[" + tag + "]", badgeX + 6, badgeY + 18);

        g2.setFont(Theme.FONT_MONO);
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.drawString(reading, badgeX + 46, badgeY + 18);

        // Mini status indicator dot
        g2.setColor(statusColor);
        g2.fillOval(badgeX + badgeW - 14, badgeY + 9, 7, 7);
    }

    private void drawHudOverlay(Graphics2D g2, int w, int h) {
        g2.setFont(Theme.FONT_MONO);
        g2.setColor(Theme.TEXT_MUTED);

        // Orientation telemetry readout
        int pDeg = (int) Math.toDegrees(pitch) % 360;
        int yDeg = (int) Math.toDegrees(yaw) % 360;
        String orientText = String.format("ATTITUDE: PITCH %+03d° | YAW %+03d° | FOV %d", pDeg, yDeg, (int) fov);
        g2.drawString(orientText, 12, h - 12);

        // Interaction hint
        String hint = "DRAG MOUSE TO ORBIT 3D · SCROLL TO ZOOM · CLICK CALLOUT TO CHART";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(hint, w - fm.stringWidth(hint) - 12, h - 12);

        // Aerospace HUD corner brackets
        drawCornerBrackets(g2, 8, 20, w - 16, h - 38);
    }

    private void drawCornerBrackets(Graphics2D g2, int x, int y, int bw, int bh) {
        g2.setColor(new Color(0x23, 0x31, 0x66, 120));
        g2.setStroke(new BasicStroke(1.2f));
        int len = 12;

        // Top-left
        g2.drawLine(x, y, x + len, y);
        g2.drawLine(x, y, x, y + len);
        // Top-right
        g2.drawLine(x + bw, y, x + bw - len, y);
        g2.drawLine(x + bw, y, x + bw, y + len);
        // Bottom-left
        g2.drawLine(x, y + bh, x + len, y + bh);
        g2.drawLine(x, y + bh, x, y + bh - len);
        // Bottom-right
        g2.drawLine(x + bw, y + bh, x + bw - len, y + bh);
        g2.drawLine(x + bw, y + bh, x + bw, y + bh - len);
    }

    private Color resolveDynamicColor(Polygon3D poly, HealthStatus status) {
        if (status == null || status == HealthStatus.NOMINAL) {
            return null; // use model's natural texture colors
        }
        Color statusColor = Theme.getHealthColor(status);
        if (status == HealthStatus.CRITICAL) {
            int alpha = (int) (130 + pulsePhase * 120);
            return new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), Math.min(255, alpha));
        } else if (status == HealthStatus.WARNING) {
            return new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 180);
        } else if (status == HealthStatus.FAILED) {
            return Theme.STATUS_FAILED;
        }
        return statusColor;
    }

    private Color resolveEdgeColor(Polygon3D poly, HealthStatus status) {
        if (status == HealthStatus.CRITICAL) {
            return Theme.STATUS_CRITICAL;
        } else if (status == HealthStatus.WARNING) {
            return Theme.STATUS_WARNING;
        } else if (status == HealthStatus.FAILED) {
            return Theme.STATUS_FAILED;
        }
        return poly.isEmissive() ? Theme.ACCENT_CYAN : new Color(0x35, 0x48, 0x75);
    }
}
