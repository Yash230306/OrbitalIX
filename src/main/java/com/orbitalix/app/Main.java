package com.orbitalix.app;

import com.orbitalix.api.*;
import com.orbitalix.config.*;
import com.orbitalix.gui.ScreenshotHarness;
import com.orbitalix.gui.MainWindow;
import com.orbitalix.simulation.DefaultSimulationController;
import com.orbitalix.simulation.SelfTest;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

/** Application entry point for the desktop mission-control GUI and CLI tools. */
public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--selftest")) {
            System.exit(SelfTest.run());
        } else if (args.length > 0 && args[0].equals("--screenshots")) {
            if (args.length < 2) throw new IllegalArgumentException("Usage: --screenshots <directory>");
            ScreenshotHarness.capture(Path.of(args[1]), new DefaultSimulationController());
        } else if (args.length > 0 && args[0].equals("--cli")) {
            runConsole();
        } else {
            SwingUtilities.invokeLater(() -> new MainWindow(new DefaultSimulationController()).setVisible(true));
        }
    }

    private static void runConsole() throws Exception {
        DefaultSimulationController controller = new DefaultSimulationController();
        controller.addEventListener(event -> System.out.printf("[T+%d][%s] %s%n", event.tick(), event.severity(), event.message()));
        System.out.println("OrbitalIX Mission Control CLI - type help, tick [n], or quit");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null && !line.equalsIgnoreCase("quit")) {
            if (line.equalsIgnoreCase("tick") || line.startsWith("tick ")) {
                int count = line.equals("tick") ? 1 : Integer.parseInt(line.substring(5).trim());
                for (int i = 0; i < count; i++) controller.tick();
                System.out.println(controller.executeCommand("status"));
            } else if (line.equalsIgnoreCase("report")) {
                System.out.println(controller.executeCommand("report"));
            } else {
                System.out.println(controller.executeCommand(line));
            }
        }
    }
}
