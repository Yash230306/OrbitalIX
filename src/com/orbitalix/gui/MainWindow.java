package com.orbitalix.gui;

import com.orbitalix.simulation.DefaultSimulationController;
import com.orbitalix.simulation.SimulationController;
import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.TickListener;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Main mission control dashboard integrating 3D interactive satellite visualization,
 * 2D orbital ground track mapping, tactical gauge cockpit, AI flight director,
 * real-time telemetry matrix, live spectrum chart, and alert console.
 * Demonstrates: MVC architecture (MainWindow is the View root), Observer pattern, aggregation.
 */
public class MainWindow extends JFrame implements TickListener {

    private final SimulationController controller;
    private final HeaderPanel headerPanel;
    private final ControlPanel controlPanel;
    private final SatellitePanel satellitePanel;
    private final OrbitMapPanel orbitMapPanel;
    private final SubsystemGaugesPanel gaugesPanel;
    private final FlightDirectorPanel flightDirectorPanel;
    private final LiveChartPanel chartPanel;
    private final TelemetryPanel telemetryPanel;
    private final AlertLogPanel alertLogPanel;

    private final CardLayout centerCardLayout = new CardLayout();
    private final JPanel centerCardPanel = new JPanel(centerCardLayout);
    private final List<JButton> viewButtons = new ArrayList<>();

    public MainWindow(SimulationController controller) {
        super("OrbitalIX · Satellite Health Mission Control");
        this.controller = controller;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1340, 840));
        setPreferredSize(new Dimension(1440, 910));
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout(8, 8));

        // 1. North: Header with Live UTC, MET, Dynamics & Audio Toggle
        headerPanel = new HeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 2. West: Scenario Cards, Controls & Anomaly Injector
        controlPanel = new ControlPanel(controller);
        add(controlPanel, BorderLayout.WEST);

        // 3. Center: Multi-Mode Tactical View (3D Satellite | Orbit Map | Cockpit Gauges) + AI Flight Director
        JPanel centerContainer = new JPanel(new BorderLayout(0, 6));
        centerContainer.setOpaque(false);

        // View Mode Switcher Toolbar
        JPanel viewSwitcher = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        viewSwitcher.setOpaque(false);

        JButton btn3D = createViewButton("🛰 3D SATELLITE MODEL", true);
        JButton btnMap = createViewButton("🌍 ORBIT GROUND TRACK", false);
        JButton btnGauges = createViewButton("📊 TACTICAL GAUGES", false);

        btn3D.addActionListener(e -> switchView("3D", btn3D));
        btnMap.addActionListener(e -> switchView("MAP", btnMap));
        btnGauges.addActionListener(e -> switchView("GAUGES", btnGauges));

        viewButtons.add(btn3D);
        viewButtons.add(btnMap);
        viewButtons.add(btnGauges);

        viewSwitcher.add(btn3D);
        viewSwitcher.add(btnMap);
        viewSwitcher.add(btnGauges);
        centerContainer.add(viewSwitcher, BorderLayout.NORTH);

        // Card Panels
        satellitePanel = new SatellitePanel();
        orbitMapPanel = new OrbitMapPanel();
        gaugesPanel = new SubsystemGaugesPanel();

        centerCardPanel.setOpaque(false);
        centerCardPanel.add(satellitePanel, "3D");
        centerCardPanel.add(orbitMapPanel, "MAP");
        centerCardPanel.add(gaugesPanel, "GAUGES");
        centerContainer.add(centerCardPanel, BorderLayout.CENTER);

        // AI Flight Director & Guided Tour
        flightDirectorPanel = new FlightDirectorPanel(controller);
        centerContainer.add(flightDirectorPanel, BorderLayout.SOUTH);

        add(centerContainer, BorderLayout.CENTER);

        // 4. East: Split with Live Chart on top, Telemetry Table on bottom
        JPanel eastPanel = new JPanel(new GridLayout(2, 1, 0, 8));
        eastPanel.setOpaque(false);
        eastPanel.setPreferredSize(new Dimension(460, 600));

        chartPanel = new LiveChartPanel();
        telemetryPanel = new TelemetryPanel();
        eastPanel.add(chartPanel);
        eastPanel.add(telemetryPanel);
        add(eastPanel, BorderLayout.EAST);

        // 5. South: Mission Anomaly & Flight Log Console
        alertLogPanel = new AlertLogPanel();
        add(alertLogPanel, BorderLayout.SOUTH);

        // Wire selections: Clicking satellite or table selects sensor in the live chart
        satellitePanel.setOnSensorSelected(chartPanel::setSelectedSensor);
        telemetryPanel.setOnSensorSelected(chartPanel::setSelectedSensor);

        // Register listeners with the simulation controller
        controller.addTickListener(this);
        controller.addAlertListener(new GuiAlertListener(alertLogPanel));

        pack();
        setLocationRelativeTo(null);
    }

    private JButton createViewButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_LABEL_BOLD);
        btn.setBackground(active ? new Color(0x23, 0x31, 0x66) : new Color(0x14, 0x1C, 0x38));
        btn.setForeground(active ? Theme.ACCENT_CYAN : Theme.TEXT_MUTED);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(active ? Theme.ACCENT_CYAN : new Color(0x23, 0x31, 0x5C), 1),
                BorderFactory.createEmptyBorder(5, 14, 5, 14)
        ));
        return btn;
    }

    private void switchView(String cardName, JButton selectedBtn) {
        SoundEffects.getInstance().playClick();
        centerCardLayout.show(centerCardPanel, cardName);

        for (JButton btn : viewButtons) {
            boolean isSel = (btn == selectedBtn);
            btn.setBackground(isSel ? new Color(0x23, 0x31, 0x66) : new Color(0x14, 0x1C, 0x38));
            btn.setForeground(isSel ? Theme.ACCENT_CYAN : Theme.TEXT_MUTED);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isSel ? Theme.ACCENT_CYAN : new Color(0x23, 0x31, 0x5C), 1),
                    BorderFactory.createEmptyBorder(5, 14, 5, 14)
            ));
        }
    }

    @Override
    public void onTick(SimulationSnapshot snapshot) {
        if (snapshot.tick() == 0) {
            alertLogPanel.logInfo("System reset: telemetry and subsystems nominal.");
        }
        headerPanel.updateSnapshot(snapshot);
        controlPanel.onTick(snapshot);
        satellitePanel.onTick(snapshot);
        orbitMapPanel.onTick(snapshot);
        gaugesPanel.onTick(snapshot);
        flightDirectorPanel.onTick(snapshot);
        chartPanel.onTick(snapshot);
        telemetryPanel.onTick(snapshot);
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            SimulationController controller = new DefaultSimulationController();
            MainWindow window = new MainWindow(controller);
            window.setVisible(true);
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
