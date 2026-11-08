package com.orbitalix.gui;

import com.orbitalix.simulation.DefaultSimulationController;
import com.orbitalix.simulation.SimulationController;
import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.TickListener;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Main mission control dashboard window combining telemetry, visual satellite representation,
 * line charts, alert console, and simulation controls.
 * Demonstrates: MVC architecture (MainWindow is the View root, communicating exclusively via SimulationController),
 * aggregation (owns the GUI panels).
 */
public class MainWindow extends JFrame implements TickListener {

    private final SimulationController controller;
    private final HeaderPanel headerPanel;
    private final ControlPanel controlPanel;
    private final SatellitePanel satellitePanel;
    private final LiveChartPanel chartPanel;
    private final TelemetryPanel telemetryPanel;
    private final AlertLogPanel alertLogPanel;

    public MainWindow(SimulationController controller) {
        super("OrbitalIX · Satellite Health Mission Control");
        this.controller = controller;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1260, 780));
        setPreferredSize(new Dimension(1340, 840));
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout(6, 6));

        // 1. North: Header & Status
        headerPanel = new HeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 2. West: Controls
        controlPanel = new ControlPanel(controller);
        add(controlPanel, BorderLayout.WEST);

        // 3. Center: Interactive Satellite Panel
        satellitePanel = new SatellitePanel();
        add(satellitePanel, BorderLayout.CENTER);

        // 4. East: Split with Live Chart on top, Telemetry Table on bottom
        JPanel eastPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        eastPanel.setOpaque(false);
        eastPanel.setPreferredSize(new Dimension(420, 600));

        chartPanel = new LiveChartPanel();
        telemetryPanel = new TelemetryPanel();
        eastPanel.add(chartPanel);
        eastPanel.add(telemetryPanel);
        add(eastPanel, BorderLayout.EAST);

        // 5. South: Alerts Console
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

    @Override
    public void onTick(SimulationSnapshot snapshot) {
        if (snapshot.tick() == 0) {
            alertLogPanel.logInfo("System reset: telemetry and subsystems nominal.");
        }
        headerPanel.updateSnapshot(snapshot);
        satellitePanel.onTick(snapshot);
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
