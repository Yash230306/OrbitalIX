package com.orbitalix.util;

import com.orbitalix.gui.MainWindow;
import com.orbitalix.simulation.DefaultSimulationController;
import com.orbitalix.simulation.SimulationEngine;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Utility to generate and capture real GUI screenshots across all simulation scenarios.
 * Saves high-resolution dashboard screenshots to docs/screenshots.
 * Demonstrates: Java2D offscreen image rendering, automation.
 */
public class ScreenshotCapture {

    public static void main(String[] args) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                generateScreenshots();
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    public static void generateScreenshots() {
        System.out.println("Generating GUI screenshots...");
        File dir = new File("docs/screenshots");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        DefaultSimulationController controller = new DefaultSimulationController();
        MainWindow window = new MainWindow(controller);
        window.pack();
        window.setSize(new Dimension(1380, 860));
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        // 1. Dashboard Overview / Nominal
        controller.loadScenario(0);
        stepSimulation(controller, 5);
        capture(window, "docs/screenshots/01_nominal_flight.png");
        capture(window, "docs/screenshots/08_main_dashboard_overview.png");

        // 2. Solar Panel Degradation
        controller.loadScenario(1);
        stepSimulation(controller, 14);
        capture(window, "docs/screenshots/02_solar_degradation.png");

        // 3. Thermal Runaway (Critical)
        controller.loadScenario(2);
        stepSimulation(controller, 16);
        capture(window, "docs/screenshots/03_thermal_runaway_critical.png");

        // 4. Safe Mode Cooling
        stepSimulation(controller, 7); // Reaches ~tick 23
        capture(window, "docs/screenshots/04_safe_mode_cooling.png");

        // 5. Gyro Drift & Backup Sensor Switch
        controller.loadScenario(3);
        stepSimulation(controller, 18);
        capture(window, "docs/screenshots/05_gyro_drift_backup.png");

        // 6. Communication Blackout
        controller.loadScenario(4);
        stepSimulation(controller, 10);
        capture(window, "docs/screenshots/06_communication_dropout.png");

        // 7. Cascading Failure
        controller.loadScenario(5);
        stepSimulation(controller, 24);
        capture(window, "docs/screenshots/07_cascading_failure.png");

        // 8. Capture 2D Orbit Ground Track view
        controller.loadScenario(0);
        stepSimulation(controller, 8);
        capture(window, "docs/screenshots/09_orbit_ground_track_radar.png");

        window.dispose();
        System.out.println("All screenshots generated successfully in docs/screenshots/");
    }

    private static void stepSimulation(DefaultSimulationController controller, int ticks) {
        SimulationEngine engine = controller.getEngine();
        for (int i = 0; i < ticks; i++) {
            engine.tick();
        }
    }

    private static void capture(MainWindow window, String filePath) {
        int w = window.getWidth();
        int h = window.getHeight();
        if (w <= 0) w = 1280;
        if (h <= 0) h = 800;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        window.getContentPane().paintAll(g2);
        g2.dispose();

        try {
            ImageIO.write(img, "png", new File(filePath));
            System.out.println("Captured: " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to write screenshot " + filePath + ": " + e.getMessage());
        }
    }
}
