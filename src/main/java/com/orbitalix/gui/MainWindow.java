package com.orbitalix.gui;

import com.orbitalix.api.AutonomyMode;
import com.orbitalix.api.SimulationController;
import com.orbitalix.gui.theme.Themes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Custom-painted Swing mission-control shell; all simulation access crosses the public API. */
public final class MainWindow extends JFrame {
    private static final List<String> VIEWS = List.of("Planner", "Dashboard", "Cutaway", "Orbit",
            "Systems", "Crew", "Replay", "Console", "Reports", "Settings");
    private final SimulationController controller;
    private final Timer simulationTimer;
    private final Timer animationTimer;
    private final JPanel stage = new JPanel(new BorderLayout());
    private final JLabel status = new JLabel("SYSTEM NOMINAL");
    private Themes theme;
    private String selectedView = "Dashboard";
    private int speed = 2;
    private boolean fullScreen;
    private final JTextField commandField = new JTextField();
    private MissionView activeView;

    public MainWindow(SimulationController controller) {
        super("OrbitalIX | Mission Control");
        this.controller = controller;
        this.theme = readTheme();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 800));
        setSize(1500, 920);
        setLocationRelativeTo(null);

        stage.setOpaque(true);
        stage.setBackground(theme.background());
        JPanel root = createShell();
        setContentPane(root);
        showView(selectedView);
        installShortcuts(root);

        simulationTimer = new Timer(1000 / speed, event -> {
            if (controller.isRunning()) controller.tick();
        });
        simulationTimer.start();
        animationTimer = new Timer(16, event -> {
            root.repaint();
            if (activeView != null) activeView.repaint();
        });
        animationTimer.start();
    }

    private Themes readTheme() {
        String saved = controller.getSetting("theme", "DARK_MISSION");
        try { return Themes.valueOf(saved); }
        catch (IllegalArgumentException ex) { return Themes.DARK_MISSION; }
    }

    private void showView(String name) {
        selectedView = name;
        activeView = new MissionView(name, controller, theme);
        stage.removeAll();
        stage.add(activeView, BorderLayout.CENTER);
        stage.revalidate();
        stage.repaint();
        setContentPane(createShell());
        revalidate();
        repaint();
    }

    private JPanel createShell() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(theme.background());
        root.add(new TopBar(), BorderLayout.NORTH);
        root.add(new NavRail(), BorderLayout.WEST);
        root.add(stage, BorderLayout.CENTER);
        root.add(new Footer(), BorderLayout.SOUTH);
        return root;
    }

    private void cycleTheme() {
        theme = Themes.values()[(theme.ordinal() + 1) % Themes.values().length];
        controller.setSetting("theme", theme.name());
        showView(selectedView);
    }

    private void toggleSelfHealing() {
        controller.setSelfHealing(!controller.snapshot().selfHealing());
        status.setText("SELF-HEALING " + (controller.snapshot().selfHealing() ? "ENABLED" : "DISABLED"));
    }

    private void toggleAutonomy() {
        AutonomyMode[] modes = AutonomyMode.values();
        int next = (controller.snapshot().autonomyMode().ordinal() + 1) % modes.length;
        controller.setAutonomyMode(modes[next]);
        status.setText("AUTONOMY " + modes[next]);
    }

    private void toggleRun() {
        if (controller.snapshot().mission().ended()) controller.reset();
        if (controller.isRunning()) {
            controller.pause();
            status.setText("MISSION PAUSED");
        } else {
            controller.start();
            status.setText("MISSION RUNNING");
        }
        repaint();
    }

    private void runCommand() {
        String command = commandField.getText().trim();
        if (command.isEmpty()) return;
        String output = controller.executeCommand(command);
        status.setText(output.isBlank() ? "COMMAND ACCEPTED" : output);
        commandField.setText("");
        if (selectedView.equals("Console")) showView("Console");
    }

    private void exportReport(boolean html) {
        try {
            Path folder = Path.of("out");
            Files.createDirectories(folder);
            Path destination = folder.resolve(html ? "mission-report.html" : "telemetry.csv");
            Files.writeString(destination, html ? controller.generateReportHtml() : controller.exportTelemetryCsv());
            status.setText("EXPORTED " + destination.toAbsolutePath());
        } catch (IOException e) {
            status.setText("EXPORT FAILED: " + e.getMessage());
        }
    }

    private void installShortcuts(JComponent root) {
        InputMap input = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = root.getActionMap();
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "play");
        actions.put("play", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { toggleRun(); } });
        for (int i = 0; i < 10; i++) {
            final String view = VIEWS.get((i + 1) % 10);
            input.put(KeyStroke.getKeyStroke(KeyEvent.VK_0 + ((i + 1) % 10), 0), "view" + i);
            actions.put("view" + i, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { showView(view); }
            });
        }
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK), "focusConsole");
        actions.put("focusConsole", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { showView("Console"); commandField.requestFocusInWindow(); }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "screenshot");
        actions.put("screenshot", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                try { ScreenshotHarness.capture(Path.of("out", "screenshots"), controller); status.setText("SCREENSHOTS SAVED TO out\\screenshots"); }
                catch (IOException ex) { status.setText("SCREENSHOT FAILED: " + ex.getMessage()); }
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "fullscreen");
        actions.put("fullscreen", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                fullScreen = !fullScreen;
                dispose();
                setUndecorated(fullScreen);
                setVisible(true);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        actions.put("escape", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { commandField.setText(""); }
        });
    }

    private final class TopBar extends JPanel {
        private TopBar() {
            setLayout(new BorderLayout());
            setBackground(theme.panel());
            setBorder(new EmptyBorder(10, 18, 10, 18));
            JLabel logo = new JLabel("ORBITALIX  /  MISSION CONTROL");
            logo.setForeground(theme.foreground());
            logo.setFont(new Font("Segoe UI", Font.BOLD, 16));
            add(logo, BorderLayout.WEST);
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            controls.setOpaque(false);
            addButton(controls, "LAUNCH", MainWindow.this::toggleRun);
            addButton(controls, "AUTONOMY", MainWindow.this::toggleAutonomy);
            addButton(controls, "SELF-HEAL", MainWindow.this::toggleSelfHealing);
            addButton(controls, "THEME", MainWindow.this::cycleTheme);
            addButton(controls, "HTML", () -> exportReport(true));
            addButton(controls, "CSV", () -> exportReport(false));
            addButton(controls, speed + " T/S", () -> {
                speed = speed == 20 ? 1 : speed + 1;
                controller.setSpeed(speed);
                simulationTimer.setDelay(1000 / speed);
                ((JButton) controls.getComponent(controls.getComponentCount() - 1)).setText(speed + " T/S");
            });
            add(controls, BorderLayout.EAST);
        }

        private void addButton(JPanel parent, String text, Runnable action) {
            JButton button = new JButton(text);
            button.setFocusPainted(false);
            button.setForeground(theme.foreground());
            button.setBackground(theme.panelRaised());
            button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(theme.grid()),
                    new EmptyBorder(7, 11, 7, 11)));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.addActionListener(event -> action.run());
            parent.add(button);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(theme.grid()); g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        }
    }

    private final class NavRail extends JPanel {
        private NavRail() {
            setLayout(new GridLayout(VIEWS.size(), 1, 0, 4));
            setBackground(theme.panel());
            setBorder(new EmptyBorder(12, 8, 12, 8));
            setPreferredSize(new Dimension(178, 0));
            for (int i = 0; i < VIEWS.size(); i++) {
                final String view = VIEWS.get(i);
                JButton button = new JButton((i + 1) % 10 + "   " + view);
                button.setHorizontalAlignment(SwingConstants.LEFT);
                button.setFocusPainted(false);
                button.setForeground(view.equals(selectedView) ? theme.accent() : theme.muted());
                button.setBackground(theme.panel());
                button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 4));
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                button.addActionListener(event -> showView(view));
                add(button);
            }
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(theme.grid()); g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
        }
    }

    private final class Footer extends JPanel {
        private Footer() {
            setLayout(new BorderLayout(12, 0));
            setBackground(theme.panel());
            setBorder(new EmptyBorder(8, 14, 8, 14));
            status.setForeground(theme.nominal());
            status.setFont(new Font("Segoe UI", Font.BOLD, 11));
            add(status, BorderLayout.WEST);
            commandField.setPreferredSize(new Dimension(500, 34));
            commandField.setBackground(theme.background());
            commandField.setForeground(theme.foreground());
            commandField.setCaretColor(theme.accent());
            commandField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(theme.grid()),
                    new EmptyBorder(4, 10, 4, 10)));
            commandField.setToolTipText("Enter onboard command (help for command list)");
            commandField.addActionListener(event -> runCommand());
            add(commandField, BorderLayout.EAST);
            JButton execute = new JButton("SEND");
            execute.setForeground(theme.foreground()); execute.setBackground(theme.panelRaised());
            execute.addActionListener(event -> runCommand());
            add(execute, BorderLayout.CENTER);
        }
    }
}
