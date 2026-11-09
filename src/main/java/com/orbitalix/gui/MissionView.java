package com.orbitalix.gui;

import com.orbitalix.api.*;
import com.orbitalix.gui.theme.Theme;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Locale;

/** Custom Java2D view canvas used by all mission-control panels. */
public final class MissionView extends JPanel implements TickListener {
    private final String title;
    private final SimulationController controller;
    private final Theme theme;
    private SimulationSnapshot snapshot;

    public MissionView(String title, SimulationController controller, Theme theme) {
        this.title = title;
        this.controller = controller;
        this.theme = theme;
        this.snapshot = controller.snapshot();
        setOpaque(true);
        setBackground(theme.background());
        controller.addTickListener(this);
    }

    public String title() { return title; }
    @Override public void onTick(SimulationSnapshot value) { snapshot = value; repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        int width = getWidth(), height = getHeight();
        drawBackground(g, width, height);
        g.setColor(theme.foreground());
        g.setFont(new Font("Segoe UI", Font.BOLD, 24));
        g.drawString(title.toUpperCase(Locale.ROOT), 28, 40);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g.setColor(theme.muted());
        g.drawString("MISSION TELEMETRY  /  T+" + String.format(Locale.ROOT, "%03d", snapshot.tick())
                + (snapshot.replaying() ? "  /  REPLAY" : ""), 30, 62);

        switch (title) {
            case "Dashboard" -> drawDashboard(g, width, height);
            case "Planner" -> drawPlanner(g, width, height);
            case "Orbit" -> drawOrbit(g, width, height);
            case "Cutaway" -> drawCutaway(g, width, height);
            case "Systems" -> drawSystems(g, width, height);
            case "Crew" -> drawCrew(g, width, height);
            case "Replay" -> drawReplay(g, width, height);
            case "Console" -> drawConsole(g, width, height);
            case "Reports" -> drawReports(g, width, height);
            case "Settings" -> drawSettings(g, width, height);
            default -> drawDashboard(g, width, height);
        }
        g.dispose();
    }

    private void drawBackground(Graphics2D g, int width, int height) {
        g.setPaint(new GradientPaint(0, 0, theme.background(), width, height, theme.panel()));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(theme.accent().getRed(), theme.accent().getGreen(), theme.accent().getBlue(), 18));
        for (int i = 0; i < 50; i++) {
            int x = Math.floorMod(i * 193 + 37, Math.max(1, width));
            int y = Math.floorMod(i * 97 + 17, Math.max(1, height));
            g.fillOval(x, y, i % 4 == 0 ? 2 : 1, i % 4 == 0 ? 2 : 1);
        }
    }

    private void drawDashboard(Graphics2D g, int w, int h) {
        int pad = 24, gap = 16, top = 78;
        int cardsW = Math.max(240, (w - 2 * pad - 2 * gap) * 2 / 3);
        int sideW = w - 2 * pad - 2 * gap - cardsW;
        List<ResourceSnapshot> resources = snapshot.resources();
        int gaugeW = (cardsW - gap) / 2;
        int gaugeH = Math.max(120, (h - top - 2 * pad - gap) / 3);
        for (int i = 0; i < Math.min(resources.size(), 6); i++) {
            int col = i % 2, row = i / 2;
            int x = pad + col * (gaugeW + gap), y = top + row * (gaugeH + gap);
            card(g, x, y, gaugeW, gaugeH);
            ResourceSnapshot resource = resources.get(i);
            int gaugeRadius = Math.min(62, Math.max(34, (gaugeW - 150) / 2));
            int gaugeCenterX = x + gaugeRadius + 16;
            drawGauge(g, gaugeCenterX, y + gaugeH / 2 + 5, gaugeRadius,
                    resource.name(), resource.level(), resource.capacity(),
                    resource.level() < resource.capacity() * .25 ? theme.warning() : theme.accent());
            g.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g.setColor(theme.foreground());
            g.drawString(number(resource.level()) + " " + resource.unit(), x + gaugeRadius * 2 + 36, y + gaugeH / 2);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g.setColor(theme.muted());
            g.drawString(resource.ticksToEmpty() >= 0 ? "EST. " + resource.ticksToEmpty() + " TICKS" : "NOMINAL",
                    x + gaugeRadius * 2 + 36, y + gaugeH / 2 + 20);
        }
        int sideX = pad + cardsW + gap;
        card(g, sideX, top, sideW, Math.max(120, (h - top - 2 * pad) * 2 / 3));
        label(g, "MISSION ADVISOR", sideX + 18, top + 28, theme.accent());
        List<RecommendationSnapshot> recs = snapshot.recommendations();
        int y = top + 52;
        if (recs.isEmpty()) {
            label(g, "No active recommendations", sideX + 18, y + 15, theme.nominal());
            small(g, "Systems are nominal. Preview suggestions appear here.", sideX + 18, y + 37);
        }
        for (RecommendationSnapshot rec : recs.stream().limit(5).toList()) {
            g.setColor(theme.panelRaised()); g.fillRoundRect(sideX + 12, y, sideW - 24, 67, 10, 10);
            g.setColor(rec.risk() == ActionRisk.COSTLY ? theme.warning() : theme.nominal());
            g.fillRoundRect(sideX + 12, y, 4, 67, 4, 4);
            label(g, rec.title(), sideX + 25, y + 21, theme.foreground());
            small(g, rec.explanation(), sideX + 25, y + 39);
            small(g, rec.risk().name() + "  /  " + rec.ticksLeft() + " TICKS", sideX + 25, y + 56);
            y += 75;
        }
        int plotY = top + Math.max(120, (h - top - 2 * pad) * 2 / 3) + gap;
        int plotH = h - plotY - pad;
        card(g, sideX, plotY, sideW, plotH);
        label(g, "MISSION PROGRESS", sideX + 18, plotY + 27, theme.accent());
        drawProgress(g, sideX + 18, plotY + 48, sideW - 36, 10, snapshot.mission().progressPercent());
        small(g, String.format(Locale.ROOT, "%.1f%%     ETA %d TICKS", snapshot.mission().progressPercent(),
                snapshot.mission().etaTicks()), sideX + 18, plotY + 80);
        drawTimeline(g, sideX + 18, plotY + 95, sideW - 36, Math.max(35, plotH - 110));
        g.setColor(snapshot.spacecraftState().equals("WARNING") ? theme.warning() : theme.nominal());
        g.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g.drawString(snapshot.spacecraftState() + (snapshot.autopilotOnline() ? "  / AUTOPILOT ONLINE" : "  / AUTOPILOT OFFLINE"), 30, h - 18);
    }

    private void drawPlanner(Graphics2D g, int w, int h) {
        int x = 28, y = 90, cardW = (w - 80) / 3;
        String[] options = {"DESTINATION", snapshot.config().destination().name(), "SPACECRAFT", snapshot.config().craftType().name(), "DIFFICULTY", snapshot.config().difficulty().name()};
        for (int i = 0; i < 3; i++) {
            card(g, x + i * (cardW + 12), y, cardW, 174);
            label(g, options[i * 2], x + i * (cardW + 12) + 18, y + 29, theme.accent());
            g.setFont(new Font("Segoe UI", Font.BOLD, 18)); g.setColor(theme.foreground());
            g.drawString(options[i * 2 + 1], x + i * (cardW + 12) + 18, y + 67);
            small(g, switch (i) {
                case 0 -> "Distance " + (int) snapshot.config().destination().distance() + "  /  Uplink " + snapshot.config().destination().uplinkDelay() + " ticks";
                case 1 -> "Fuel " + snapshot.config().craftType().fuel() + "  /  Crew " + snapshot.config().craftType().crew();
                default -> "Seed " + snapshot.config().seed() + "  /  " + (snapshot.config().chaosDirector() ? "CHAOS ON" : "CHAOS OFF");
            }, x + i * (cardW + 12) + 18, y + 96);
            drawButton(g, x + i * (cardW + 12) + 18, y + 119, cardW - 36, 36, i == 0 ? "CHANGE DESTINATION" : i == 1 ? "CHANGE CRAFT" : "CHANGE SCENARIO", i == 2);
        }
        card(g, 28, 290, w - 56, Math.max(140, h - 330));
        label(g, "MISSION BRIEF", 50, 324, theme.accent());
        small(g, "Choose a profile, verify reserves, then launch. The selected scenario is controlled from the Console view.", 50, 350);
        drawProgress(g, 50, 380, Math.max(160, w - 100), 12, snapshot.mission().progressPercent());
        drawButton(g, w / 2 - 110, h - 84, 220, 48, snapshot.mission().ended() ? "MISSION COMPLETE" : "LAUNCH / RESUME", true);
    }

    private void drawOrbit(Graphics2D g, int w, int h) {
        int x = 28, y = 86, width = w - 56, height = h - 120;
        card(g, x, y, width, height);
        int left = x + 42, right = x + width - 42, mid = y + height / 2;
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{7, 7}, 0));
        g.setColor(theme.grid());
        g.drawLine(left, mid, right, mid);
        Path2D planned = new Path2D.Double();
        planned.moveTo(left, mid);
        planned.curveTo(w * .35, y + height * .15, w * .68, y + height * .85, right, mid);
        g.setColor(theme.accent()); g.draw(planned);
        double p = Math.min(1, snapshot.mission().progressPercent() / 100);
        int shipX = (int) (left + (right - left) * p);
        int shipY = (int) (mid - Math.sin(p * Math.PI) * height * .16);
        g.setColor(theme.nominal()); g.fillOval(left - 8, mid - 8, 16, 16);
        g.setColor(theme.accent()); g.fillOval(right - 10, mid - 10, 20, 20);
        g.setColor(theme.foreground()); g.fillRoundRect(shipX - 10, shipY - 5, 20, 10, 6, 6);
        label(g, "EARTH", left - 12, mid + 28, theme.muted());
        label(g, snapshot.config().destination().name().replace('_', ' '), right - 70, mid + 30, theme.muted());
        small(g, "DISTANCE LEFT " + number(snapshot.mission().distanceLeft()) + "     ETA " + snapshot.mission().etaTicks() + " TICKS", x + 22, y + height - 20);
        if (!snapshot.threats().isEmpty()) {
            g.setColor(theme.critical()); g.setStroke(new BasicStroke(2)); g.drawOval(shipX + 40, shipY - 40, 66, 66);
            label(g, "DEBRIS: " + snapshot.threats().get(0).ticksToImpact() + " TICKS", shipX + 110, shipY - 20, theme.critical());
        }
        g.setColor(snapshot.comms().blackout() ? theme.critical() : theme.nominal());
        small(g, "UPLINK " + snapshot.comms().delayTicks() + " TICKS  /  SIGNAL " + Math.round(snapshot.comms().signalStrength() * 100) + "%", x + 22, y + 30);
    }

    private void drawCutaway(Graphics2D g, int w, int h) {
        int cx = w / 2, cy = h / 2 + 15;
        card(g, 28, 82, w - 56, h - 112);
        g.setColor(new Color(theme.accent().getRed(), theme.accent().getGreen(), theme.accent().getBlue(), 35));
        g.fillOval(cx - 190, cy - 150, 380, 300);
        g.setColor(theme.panelRaised()); g.fillRoundRect(cx - 145, cy - 55, 290, 110, 30, 30);
        g.setColor(theme.accent()); g.setStroke(new BasicStroke(3)); g.drawRoundRect(cx - 145, cy - 55, 290, 110, 30, 30);
        for (int i = 0; i < 4; i++) {
            g.setColor(i % 2 == 0 ? theme.nominal() : theme.warning());
            g.fillRoundRect(cx - 110 + i * 58, cy - 28, 42, 55, 12, 12);
            g.setColor(theme.muted()); g.drawString("SYS " + (i + 1), cx - 110 + i * 58, cy + 47);
        }
        g.setColor(theme.warning()); g.fillPolygon(new int[]{cx + 146, cx + 194, cx + 146}, new int[]{cy - 28, cy, cy + 28}, 3);
        g.setColor(theme.accent()); g.fillOval(cx - 24, cy - 108, 48, 48);
        g.setColor(theme.foreground()); g.drawString("CREW MODULE", cx - 46, cy - 82);
        label(g, "ROTATING SPACECRAFT / ENGINE PLUME", 52, 118, theme.accent());
        small(g, "Click the view or inject faults in Console to observe subsystem health and failure effects.", 52, h - 52);
        drawButton(g, w - 210, h - 80, 160, 36, "AUTO-ROTATE: ON", false);
    }

    private void drawSystems(Graphics2D g, int w, int h) {
        int listX = 28, top = 86, listW = (w - 72) / 2, row = 56;
        card(g, listX, top, listW, h - 120);
        label(g, "SPACECRAFT HEALTH TREE", listX + 18, top + 28, theme.accent());
        int y = top + 54;
        for (SubsystemSnapshot subsystem : snapshot.subsystems()) {
            g.setColor(subsystem.status() == HealthStatus.NOMINAL ? theme.nominal() : theme.warning());
            g.fillOval(listX + 20, y - 10, 9, 9);
            label(g, subsystem.name(), listX + 40, y, theme.foreground());
            small(g, subsystem.activeFaults().isEmpty() ? "ALL SENSORS NOMINAL" : String.join(", ", subsystem.activeFaults()), listX + 40, y + 19);
            y += row;
        }
        int detailX = listX + listW + 16;
        card(g, detailX, top, w - detailX - 28, h - 120);
        label(g, "DEPENDENCY / TELEMETRY", detailX + 18, top + 28, theme.accent());
        int graphY = top + 68;
        for (DependencySnapshot dep : snapshot.dependencies()) {
            label(g, dep.from() + "  →  " + dep.to(), detailX + 22, graphY, dep.propagating() ? theme.warning() : theme.foreground());
            small(g, dep.propagating() ? "CASCADE PROPAGATING" : "LINK NOMINAL", detailX + 22, graphY + 18);
            graphY += 54;
        }
        graphY += 15;
        for (SubsystemSnapshot subsystem : snapshot.subsystems()) {
            if (subsystem.sensors().isEmpty()) continue;
            SensorSnapshot sensor = subsystem.sensors().get(0);
            small(g, sensor.sensorId().toUpperCase(Locale.ROOT) + "     " + number(sensor.value()) + " " + sensor.unit(), detailX + 22, graphY);
            graphY += 25;
        }
    }

    private void drawCrew(Graphics2D g, int w, int h) {
        int cols = 2, gap = 14, margin = 28, top = 90;
        int cardW = (w - margin * 2 - gap) / cols;
        int cardH = (h - top - 44 - gap) / 2;
        for (int i = 0; i < snapshot.crew().size(); i++) {
            CrewSnapshot person = snapshot.crew().get(i);
            int x = margin + (i % cols) * (cardW + gap), y = top + (i / cols) * (cardH + gap);
            card(g, x, y, cardW, cardH);
            g.setColor(theme.accent()); g.fillOval(x + 22, y + 22, 52, 52);
            g.setColor(theme.background()); g.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g.drawString(person.name().substring(0, 1), x + 41, y + 56);
            label(g, person.name(), x + 88, y + 38, theme.foreground());
            small(g, person.role() + "  /  " + person.task(), x + 88, y + 58);
            drawMeter(g, x + 22, y + 102, cardW - 44, "HEALTH", person.health(), theme.nominal());
            drawMeter(g, x + 22, y + 132, cardW - 44, "STRESS", person.stress(), theme.warning());
            drawMeter(g, x + 22, y + 162, cardW - 44, "RADIATION DOSE", Math.min(100, person.radiationDose()), theme.critical());
            if (cardH > 210) {
                small(g, person.alive() ? "CREW STATUS  /  NOMINAL" : "CREW STATUS  /  CASUALTY", x + 22, y + 194);
                small(g, "ASSIGNMENT  /  " + (person.taskTicksLeft() > 0 ? person.taskTicksLeft() + " TICKS REMAINING" : "READY FOR TASKING"),
                        x + 22, y + 211);
                drawButton(g, x + 22, y + cardH - 48, cardW - 44, 32, "ASSIGN TASK", true);
            }
        }
    }

    private void drawReplay(Graphics2D g, int w, int h) {
        int x = 28, y = 90;
        card(g, x, y, w - 56, h - 132);
        label(g, "MISSION TIMELINE", x + 22, y + 32, theme.accent());
        drawProgress(g, x + 22, y + 58, w - 100, 14,
                snapshot.mission().ended() ? 100 : Math.min(100, snapshot.tick() / 3d));
        int lineY = y + 115;
        g.setColor(theme.grid()); g.fillRoundRect(x + 24, lineY, w - 104, 3, 3, 3);
        List<SimulationSnapshot> items = controller.getHistory();
        if (!items.isEmpty()) {
            for (int i = 0; i < items.size(); i += Math.max(1, items.size() / 24)) {
                int px = x + 24 + (int) ((w - 104) * i / (double) Math.max(1, items.size() - 1));
                g.setColor(items.get(i).recommendations().isEmpty() ? theme.accent() : theme.warning());
                g.fillOval(px - 4, lineY - 2, 8, 8);
            }
        }
        label(g, "T+" + snapshot.tick(), x + 22, lineY + 35, theme.foreground());
        drawButton(g, x + 22, lineY + 58, 160, 38, "RESUME LIVE", true);
        small(g, "Recorded snapshots: " + items.size() + "   /   Actions: HUMAN · AUTOPILOT · SELF-HEAL", x + 22, lineY + 126);
        drawTimeline(g, x + 22, lineY + 151, w - 104, Math.max(60, h - lineY - 205));
    }

    private void drawConsole(Graphics2D g, int w, int h) {
        card(g, 28, 84, w - 56, h - 112);
        label(g, "ONBOARD COMMAND TERMINAL", 50, 116, theme.accent());
        String[] lines = {"ORBITALIX FLIGHT COMPUTER / CORE LINK ESTABLISHED",
                "Commands: help status throttle inject autonomy heal limits crew scenario seek report",
                "> " + snapshot.spacecraftState() + "  fuel " + number(snapshot.resources().get(0).level())
                        + "  battery " + number(snapshot.resources().get(2).level())};
        int y = 158;
        for (String line : lines) {
            g.setFont(new Font("Consolas", Font.PLAIN, 14)); g.setColor(theme.foreground());
            g.drawString(line, 50, y); y += 28;
        }
        g.setColor(theme.accent()); g.fillRoundRect(48, h - 92, w - 96, 42, 10, 10);
        g.setColor(theme.background()); g.setFont(new Font("Consolas", Font.BOLD, 14));
        g.drawString("TYPE COMMAND IN THE BAR BELOW   >", 64, h - 66);
    }

    private void drawReports(Graphics2D g, int w, int h) {
        card(g, 28, 88, w - 56, h - 126);
        int x = 54, y = 124;
        label(g, "FLIGHT SUMMARY", x, y, theme.accent());
        g.setFont(new Font("Segoe UI", Font.BOLD, 34)); g.setColor(theme.foreground());
        g.drawString(snapshot.score().grade(), x, y + 58);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        g.drawString(snapshot.mission().outcome(), x + 74, y + 56);
        small(g, "TICKS " + snapshot.tick() + "   /   SCORE " + snapshot.score().points() + "   /   PROGRESS "
                + number(snapshot.mission().progressPercent()) + "%", x, y + 88);
        label(g, "ACHIEVEMENTS", x, y + 134, theme.accent());
        int badgeX = x;
        for (String achievement : snapshot.score().earnedAchievements()) {
            drawButton(g, badgeX, y + 153, 160, 36, achievement.toUpperCase(Locale.ROOT), true);
            badgeX += 174;
        }
        label(g, "EXPORTS", x, y + 225, theme.accent());
        drawButton(g, x, y + 244, 180, 40, "EXPORT HTML REPORT", true);
        drawButton(g, x + 194, y + 244, 180, 40, "EXPORT TELEMETRY CSV", false);
        small(g, "Report includes telemetry, fault history, score, and crew state.", x, y + 312);
    }

    private void drawSettings(Graphics2D g, int w, int h) {
        card(g, 28, 86, w - 56, h - 120);
        label(g, "DISPLAY & MISSION SETTINGS", 52, 120, theme.accent());
        String[] rows = {"COLOR THEME", theme.displayName(), "SOUND", "SYNTHESIZED ALERTS", "ANIMATION", "SMOOTH / 60 FPS",
                "UPLINK DELAY", snapshot.comms().delayTicks() + " TICKS", "SELF-HEALING", snapshot.selfHealing() ? "ENABLED" : "DISABLED"};
        for (int i = 0; i < rows.length; i += 2) {
            int y = 164 + i * 24;
            label(g, rows[i], 58, y, theme.muted());
            label(g, rows[i + 1], Math.min(w - 300, 330), y, theme.foreground());
            drawButton(g, w - 250, y - 20, 180, 34, i == 0 ? "CHANGE THEME" : i == 6 ? "TOGGLE UPLINK" : "TOGGLE", false);
        }
    }

    private void drawTimeline(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(theme.grid());
        for (int i = 0; i <= 4; i++) g.drawLine(x, y + height * i / 4, x + width, y + height * i / 4);
        g.setColor(theme.accent());
        Path2D path = new Path2D.Double();
        int samples = Math.max(8, Math.min(50, snapshot.tick() + 1));
        for (int i = 0; i < samples; i++) {
            double px = x + (width * i / (double) (samples - 1));
            double py = y + height * (.78 - .6 * i / (double) (samples - 1));
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        g.setStroke(new BasicStroke(2)); g.draw(path);
    }

    private void drawGauge(Graphics2D g, int cx, int cy, int radius, String label, double value, double max, Color color) {
        int d = radius * 2;
        g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(theme.grid()); g.draw(new Arc2D.Double(cx - radius, cy - radius, d, d, 135, 270, Arc2D.OPEN));
        g.setColor(color); g.draw(new Arc2D.Double(cx - radius, cy - radius, d, d, 135, 270 * Math.max(0, Math.min(1, value / max)), Arc2D.OPEN));
        g.setColor(theme.muted()); g.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g.drawString(label.toUpperCase(Locale.ROOT), cx - radius, cy + radius + 17);
    }

    private void drawMeter(Graphics2D g, int x, int y, int width, String name, double value, Color color) {
        small(g, name, x, y);
        g.setColor(theme.grid()); g.fillRoundRect(x, y + 7, width, 7, 7, 7);
        g.setColor(color); g.fillRoundRect(x, y + 7, (int) (width * Math.max(0, Math.min(1, value / 100))), 7, 7, 7);
        small(g, number(value) + "%", x + width - 40, y);
    }

    private void drawProgress(Graphics2D g, int x, int y, int width, int height, double percent) {
        g.setColor(theme.grid()); g.fillRoundRect(x, y, width, height, height, height);
        g.setColor(theme.accent()); g.fillRoundRect(x, y, (int) (width * Math.max(0, Math.min(1, percent / 100))), height, height, height);
    }
    private void card(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(new Color(0, 0, 0, 32)); g.fillRoundRect(x + 2, y + 4, width, height, 16, 16);
        g.setColor(theme.panel()); g.fillRoundRect(x, y, width, height, 16, 16);
        g.setColor(theme.grid()); g.drawRoundRect(x, y, width, height, 16, 16);
    }
    private void drawButton(Graphics2D g, int x, int y, int width, int height, String text, boolean primary) {
        g.setColor(primary ? theme.accent() : theme.panelRaised());
        g.fillRoundRect(x, y, width, height, 10, 10);
        g.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g.setColor(primary && theme.displayName().equals("Projector Light") ? Color.WHITE : theme.foreground());
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + Math.max(8, (width - fm.stringWidth(text)) / 2), y + height / 2 + 4);
    }
    private void label(Graphics2D g, String text, int x, int y, Color color) {
        g.setColor(color); g.setFont(new Font("Segoe UI", Font.BOLD, 12)); g.drawString(text, x, y);
    }
    private void small(Graphics2D g, String text, int x, int y) {
        g.setColor(theme.muted()); g.setFont(new Font("Segoe UI", Font.PLAIN, 10)); g.drawString(text, x, y);
    }
    private static String number(double value) { return String.format(Locale.ROOT, "%.1f", value); }
}
