package com.orbitalix.app;

import com.orbitalix.alert.ConsoleAlertListener;
import com.orbitalix.alert.FileAlertListener;
import com.orbitalix.model.Subsystem;
import com.orbitalix.sensor.Sensor;
import com.orbitalix.simulation.DefaultSimulationController;
import com.orbitalix.simulation.Scenario;
import com.orbitalix.simulation.SimulationEngine;
import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.SubsystemSnapshot;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Scanner;

/**
 * Main application entry point for OrbitalIX Mission Control.
 * Supports launching the Swing GUI dashboard or running simulation scenarios via CLI.
 * Demonstrates: MVC entry point, CLI interface, polymorphism, exception handling.
 */
public class Main {

    public static void main(String[] args) {
        boolean cliRequested = false;
        int scenarioIndex = -1;
        int ticksToRun = 25;
        boolean runAll = false;

        for (int i = 0; i < args.length; i++) {
            if ("--cli".equalsIgnoreCase(args[i])) {
                cliRequested = true;
            } else if ("--scenario".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                cliRequested = true;
                try {
                    scenarioIndex = Integer.parseInt(args[++i]) - 1;
                } catch (NumberFormatException ignored) {}
            } else if ("--ticks".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                try {
                    ticksToRun = Integer.parseInt(args[++i]);
                } catch (NumberFormatException ignored) {}
            } else if ("--all".equalsIgnoreCase(args[i])) {
                cliRequested = true;
                runAll = true;
            }
        }

        if (!cliRequested) {
            // Attempt to launch GUI if available
            try {
                Class<?> mainWindowClass = Class.forName("com.orbitalix.gui.MainWindow");
                Method launchMethod = mainWindowClass.getMethod("launch");
                launchMethod.invoke(null);
                return;
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                // GUI not yet implemented, fall through to CLI
                System.out.println("GUI not yet present; falling back to CLI mode.");
            } catch (Exception e) {
                System.err.println("Error launching GUI: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Run CLI mode
        runCli(scenarioIndex, ticksToRun, runAll);
    }

    public static void runCli(int targetScenarioIndex, int ticksToRun, boolean runAll) {
        List<Scenario> scenarios = Scenario.defaultScenarios();

        System.out.println("==============================================================");
        System.out.println("       ORBITALIX · SATELLITE MISSION CONTROL (CLI)            ");
        System.out.println("==============================================================");

        if (runAll) {
            for (int i = 0; i < scenarios.size(); i++) {
                runScenarioSimulation(scenarios.get(i), 25);
            }
            return;
        }

        if (targetScenarioIndex >= 0 && targetScenarioIndex < scenarios.size()) {
            runScenarioSimulation(scenarios.get(targetScenarioIndex), ticksToRun);
            return;
        }

        // Interactive scenario selection if possible
        System.out.println("Available Scenarios:");
        for (int i = 0; i < scenarios.size(); i++) {
            System.out.printf("  [%d] %-30s : %s%n", (i + 1), scenarios.get(i).getName(), scenarios.get(i).getDescription());
        }
        System.out.println("  [A] Run All Scenarios");
        System.out.print("Select scenario (1-6, A, or Enter for default): ");

        Scanner scanner = new Scanner(System.in);
        String line = "";
        try {
            if (scanner.hasNextLine()) {
                line = scanner.nextLine().trim();
            }
        } catch (Exception ignored) {}

        if ("A".equalsIgnoreCase(line)) {
            for (Scenario s : scenarios) {
                runScenarioSimulation(s, 25);
            }
            return;
        }

        int choice = 0;
        try {
            if (!line.isEmpty()) {
                choice = Integer.parseInt(line) - 1;
            }
        } catch (NumberFormatException ignored) {}

        if (choice < 0 || choice >= scenarios.size()) {
            choice = 0; // Default to nominal
        }

        runScenarioSimulation(scenarios.get(choice), ticksToRun);
    }

    public static void runScenarioSimulation(Scenario scenario, int ticks) {
        System.out.println("\n--------------------------------------------------------------");
        System.out.println("RUNNING SCENARIO: " + scenario.getName());
        System.out.println("Description: " + scenario.getDescription());
        System.out.println("--------------------------------------------------------------");

        SimulationEngine engine = new SimulationEngine(scenario);
        engine.addAlertListener(new ConsoleAlertListener());
        engine.addAlertListener(new FileAlertListener());

        for (int t = 1; t <= ticks; t++) {
            SimulationSnapshot snap = engine.tick();

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Tick %02d | Mode: %-10s", snap.tick(), snap.spacecraftState()));
            for (SubsystemSnapshot sub : snap.subsystems()) {
                sb.append(String.format(" | %s: %s", sub.name(), sub.status()));
            }
            System.out.println(sb);
        }

        System.out.println("\n--- SCENARIO SUMMARY: " + scenario.getName() + " ---");
        System.out.println("Final Spacecraft Mode: " + engine.getSpacecraft().getModeName());
        System.out.println("Handled Errors Count:  " + engine.getMissionControl().getHandledErrors().size());
        for (Subsystem sub : engine.getSpacecraft().getSubsystems()) {
            System.out.printf("  Subsystem %-15s : Health %-8s%n", sub.getName(), sub.getHealth());
            for (Sensor s : sub.getSensors()) {
                System.out.printf("    - Sensor %-10s : %6.2f %-5s (backup active: %s, failed: %s)%n",
                        s.getId(), s.getLastValue(), s.getUnit(), s.isUsingBackup(), s.isFailed());
            }
        }
        System.out.println("--------------------------------------------------------------\n");
    }
}
