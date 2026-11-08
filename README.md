# OrbitalIX: Autonomous Satellite Health & Telemetry Mission Control

[![Java Version](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)
[![GUI](https://img.shields.io/badge/GUI-Swing%20%2B%20Java2D-green.svg)]()
[![Architecture](https://img.shields.io/badge/Architecture-MVC%20%2B%20Observer%20%2B%20Strategy-orange.svg)]()

> **Live Mission-Control Dashboard & Autonomous Fault-Recovery Simulator for a Deep-Space Spacecraft.**
> Built with pure Java 17 (Swing + Java2D) with zero external runtime dependencies.

---

## 1. Project Concept & The Hospital ICU Analogy

A satellite in orbit is hundreds or millions of kilometers away from Earth. When hardware fails or telemetry drifts, no technician can walk up and replace a chip. The satellite's onboard flight software must **monitor vital telemetry, detect anomalies in real time, raise alarms to mission control, and autonomously execute recovery actions.**

OrbitalIX models this system just like an Intensive Care Unit (ICU) patient monitoring system:

| Hospital ICU Monitor | OrbitalIX Satellite Mission Control | Code Location |
|---|---|---|
| **Patient** | Spacecraft platform (`OrbitalIX-1`) | `com.orbitalix.model.Spacecraft` |
| **Vital Sensors** (Pulse, BP, Temp) | Power (V), Thermal (°C), Gyro (deg/s), Signal (%) | `com.orbitalix.sensor.*` |
| **Diagnostic Rules** ("Heart rate > 120 bpm") | Detection algorithms (Threshold, Rate-of-Change, Z-Score) | `com.orbitalix.detection.*` |
| **Alarm Beeps & Logs** | Visual status banner, console alerts, log file | `com.orbitalix.alert.*`, `com.orbitalix.gui.*` |
| **Emergency Treatment** | Autonomous recovery actions (Safe Mode, backup gyro, restart) | `com.orbitalix.recovery.*`, `com.orbitalix.state.*` |

---

## 2. System Architecture & Packages

The system adheres strictly to the **Model-View-Controller (MVC)** architectural pattern and **Dependency Inversion**: the GUI View never touches core domain entities directly, communicating exclusively through the `SimulationController` contract.

```
com.orbitalix
├── app          # Main entry point (GUI launcher and interactive CLI simulator)
├── sensor       # Abstract Sensor, Reading, VoltageSensor, TemperatureSensor, SignalSensor, GyroSensor
├── model        # Spacecraft, Subsystem (Power, Thermal, Comm, Attitude), Anomaly, HealthStatus, MissionControl
├── detection    # AnomalyDetector Strategy, ThresholdDetector, RateOfChangeDetector, ZScoreDetector, DetectionEngine
├── alert        # AlertListener Observer, AlertPublisher, ConsoleAlertListener, FileAlertListener
├── state        # SpacecraftState State pattern (NominalState, WarningState, CriticalState, SafeModeState)
├── recovery     # RecoveryAction Command pattern, RestartSubsystemAction, SwitchToBackupSensorAction, EnterSafeModeAction
├── simulation   # SimulationEngine, SimulationController, FaultInjector, Scenario catalog, Factories
├── gui          # Theme, MainWindow, HeaderPanel, StatusBanner, SatellitePanel, LiveChartPanel, TelemetryPanel, AlertLogPanel
├── util         # EventLogger (Thread-safe Singleton), ScreenshotCapture
└── exception    # Checked hierarchy: OrbitalIXException, SensorFailureException, CommunicationLossException, etc.
```

---

## 3. Object-Oriented Principles & Design Patterns

### Object-Oriented Mapping

| Concept | Implementation in Code |
|---|---|
| **Encapsulation** | Private sensor state, calibrated history buffers, immutable `SimulationSnapshot` records. |
| **Abstraction** | `Sensor`, `Subsystem`, `AnomalyDetector`, `RecoveryAction`, and `SimulationController` interfaces. |
| **Inheritance** | Sensor hierarchy (`Sensor` -> `VoltageSensor`, `TemperatureSensor`, etc.), Subsystem hierarchy. |
| **Polymorphism** | Dynamic dispatch in `sensor.measure()`, `detector.analyze()`, `state.handle()`, and `action.execute()`. |
| **Composition** | `Spacecraft` owns its `Subsystem`s; `Subsystem` owns its `Sensor`s (lifetime bound to parent). |
| **Aggregation** | `MissionControl` aggregates `Spacecraft`; `DetectionEngine` aggregates `AnomalyDetector`s. |
| **Association** | `Anomaly` references `Sensor`; `RecoveryAction` executes upon `Spacecraft`; GUI references `SimulationController`. |

### Design Patterns

1. **Model-View-Controller (MVC)**:
   - **Model**: `Spacecraft`, `Subsystem`, `Sensor`, `MissionControl`.
   - **View**: `MainWindow`, `SatellitePanel`, `LiveChartPanel`, `TelemetryPanel`, `AlertLogPanel`.
   - **Controller**: `SimulationController`, `DefaultSimulationController`.
2. **Strategy Pattern** (`com.orbitalix.detection`):
   - Algorithms for anomaly detection (`ThresholdDetector`, `RateOfChangeDetector`, `ZScoreDetector`) implement `AnomalyDetector` and can be swapped or combined dynamically by `DetectionEngine`.
3. **Observer Pattern** (`com.orbitalix.alert`, `com.orbitalix.simulation`):
   - `AlertPublisher` notifies registered `AlertListener`s (`ConsoleAlertListener`, `FileAlertListener`, `GuiAlertListener`).
   - `SimulationEngine` notifies `TickListener`s on every clock step with an immutable `SimulationSnapshot`.
4. **State Pattern** (`com.orbitalix.state`):
   - Spacecraft behavior transitions across `NominalState` -> `WarningState` -> `CriticalState` -> `SafeModeState`.
5. **Command Pattern** (`com.orbitalix.recovery`):
   - Recovery operations encapsulated in `RecoveryAction` objects (`RestartSubsystemAction`, `SwitchToBackupSensorAction`, `EnterSafeModeAction`).
6. **Factory Pattern** (`com.orbitalix.simulation`):
   - `SensorFactory` and `SubsystemFactory` centralize object assembly and configuration.
7. **Singleton Pattern** (`com.orbitalix.util.EventLogger`):
   - Thread-safe single shared instance logging to both console output and `logs/orbitalix.log`.

### SOLID Compliance

- **S (Single Responsibility)**: Sensors sample physical signals; Detectors analyze; Recovery plans fixes; Logger logs; GUI panels render only their specific view.
- **O (Open/Closed)**: New sensors, detectors, or recovery actions can be added by implementing interfaces without modifying engine code.
- **L (Liskov Substitution)**: Any subclass of `Sensor` or `Subsystem` behaves correctly wherever the base class is expected.
- **I (Interface Segregation)**: Focused, minimal interfaces (`AnomalyDetector`, `AlertListener`, `TickListener`, `RecoveryAction`).
- **D (Dependency Inversion)**: High-level modules and GUI depend on interfaces (`SimulationController`, `TickListener`), not concrete engine classes.

---

## 4. The 6 Demo Scenarios

OrbitalIX comes pre-scripted with 6 comprehensive flight scenarios:

1. **Nominal Run**: Spacecraft operates under normal parameters. Telemetry stays clean and all subsystems remain in `NOMINAL` state.
2. **Solar Panel Degradation**: At tick 5, solar array bus voltage drops. Caught by `ThresholdDetector`, triggering a `WARNING` alert and moving the spacecraft to `WARNING` mode.
3. **Thermal Runaway**: At tick 5, core heater runs wild. `RateOfChangeDetector` triggers a `WARNING`, followed by `CRITICAL` mode. Spacecraft autonomously enters `SAFE MODE`, sheds load, and initiates core radiator cooling until nominal temperatures return.
4. **Gyro Drift**: At tick 12, primary gyroscope begins drifting away from baseline. `ZScoreDetector` identifies statistical deviation ($Z > 30$) and `RecoveryPlanner` commands an automated switchover to the redundant backup gyro (`GYRO-BKP`).
5. **Communication Blackout**: At tick 6, antenna signal suffers complete dropout. Throws checked `CommunicationLossException`, logs the fault, and schedules an autonomous subsystem radio restart.
6. **Cascading Failure**: Compound catastrophe across Power, Thermal, and Attitude. Demonstrates system resilience when multiple subsystems fail simultaneously, gracefully capturing `UnrecoverableFaultException` and isolating failing components.

---

## 5. How to Compile and Run

### Prerequisites
- JDK 17 or higher (`java -version`, `javac -version`).
- Operating System: Windows, macOS, or Linux.

### Windows (Quick Start)

**1. Compile the Project:**
```cmd
compile.bat
```

**2. Launch the Mission Control GUI Dashboard:**
```cmd
run.bat
```
*(Or directly: `java -cp out com.orbitalix.app.Main`)*

**3. Run in Console/CLI Mode:**
```cmd
java -cp out com.orbitalix.app.Main --cli
```
- Run all 6 scenarios automatically: `java -cp out com.orbitalix.app.Main --cli --all`
- Run a specific scenario: `java -cp out com.orbitalix.app.Main --cli --scenario 3 --ticks 25`

### Linux / macOS

```bash
chmod +x compile.sh run.sh
./compile.sh
./run.sh
```

---

## 6. Dashboard User Interface & 3D Visualization

The mission control interface is built using a custom pure-Java software 3D rendering pipeline and Swing, adhering to a high-contrast dark aerospace aesthetic:

- **Multi-Mode Center Tactical Display**:
  - **🛰 3D SPACECRAFT**: Full shaded model with interactive mouse orbit/drag, zoom, directional lighting, and clickable system callouts.
  - **🚀 2D SPACECRAFT**: A large, detailed spacecraft illustration in orbit, with the flight bus, segmented solar wings, high-gain dish, thermal radiators, attitude ring and propulsion plume highlighted by live system health.
  - **🌍 ORBIT MAP**: Equirectangular Earth map tracking spacecraft position and ground-station coverage; the moving map marker shows the spacecraft's hull, solar wings, dish and engine.
  - **🧩 OOP GUIDE**: Interactive-friendly visual guide to classes, objects, fields, methods, overloaded constructors, encapsulation, abstraction, inheritance, polymorphism and object relationships. The polymorphism example updates with live sensor readings.
  - **📊 TACTICAL COCKPIT GAUGES**: Circular radial dial meters (Power Bus, Core Temp, Uplink Signal, Gyro Drift) and an overall Platform Health Score (0–100%) that makes status instantly readable to any outsider.
- **AI Flight Director & 60-Second Guided Viva Tour**:
  - Real-time plain-English briefing translating raw telemetry into clear human narratives.
  - One-click `[ ★ 60-SEC VIVA TOUR ]` button that automatically steps through the entire presentation story (Nominal -> Fault Injection -> Warning Detection -> Critical Red Alert -> Automated Safe Mode Recovery -> Nominal restoration) with on-screen narration.
- **Synthesized Sci-Fi Audio Synthesizer**: Pure Java in-memory waveform sound effects (`javax.sound.sampled`) producing tactile UI clicks, telemetry beeps, warning chirps, emergency critical klaxons, and safe mode recovery chimes with a one-click `[ 🔊 AUDIO ]` toggle.
- **Flight Operations Header (North)**: Title branding, live UTC clock, Mission Elapsed Time (MET `T+ HH:MM:SS`), orbital flight dynamics strip (Altitude, Velocity, Inclination, Apogee/Perigee), and a pulsing hexagonal `StatusBanner`.
- **Build-Your-Own Mission Controls (West)**: Starts with beginner-friendly controls for core temperature, power-bus voltage, antenna signal, and spacecraft spin. Set readings directly, see normal/warning/critical feedback, and watch the live dashboard respond; scripted scenarios and manual fault injection remain available on the Scenarios tab.
- **Live Telemetry & Spectrum Chart (East Top)**: Real-time telemetry line chart with soft glowing line effects, shaded area gradient fill, statistical badges (MIN, MAX, AVG), dashed warning threshold line, and interactive mouse crosshair inspection.
- **Real-Time Telemetry Matrix (East Bottom)**: Real-time readings table with icon badges, engineering units, alternating high-tech row styling, and click-to-focus integration.
- **Mission Anomaly Console (South)**: Terminal-style monospaced log recording anomalies, autonomous recovery commands, with live counters for total alerts, warnings, and criticals.

The **OOP GUIDE** view connects each concept to real project classes: `Sensor` and its concrete sensor subclasses, `Spacecraft` and `Subsystem`, `MissionControl`, immutable simulation snapshots, and `Anomaly`. This is a learning aid alongside the implementation; design patterns are summarized in the guide footer.

---

## 7. Captured Screenshots

All screenshots are generated directly from the live Java application:

| Scenario / View | Screenshot |
|---|---|
| **1. Nominal Flight** | `docs/screenshots/01_nominal_flight.png` |
| **2. Solar Panel Degradation** | `docs/screenshots/02_solar_degradation.png` |
| **3. Thermal Runaway (Critical)** | `docs/screenshots/03_thermal_runaway_critical.png` |
| **4. Safe Mode Cooling** | `docs/screenshots/04_safe_mode_cooling.png` |
| **5. Gyro Drift & Backup Switch** | `docs/screenshots/05_gyro_drift_backup.png` |
| **6. Communication Blackout** | `docs/screenshots/06_communication_dropout.png` |
| **7. Cascading Compound Failure** | `docs/screenshots/07_cascading_failure.png` |
| **8. Mission Control Overview** | `docs/screenshots/08_main_dashboard_overview.png` |
| **9. 2D Orbit Ground Track & Radar** | `docs/screenshots/09_orbit_ground_track_radar.png` |

---

## 8. Viva Voce Cheat Sheet

1. **Why does the GUI only import the simulation contract?**  
   To enforce **Dependency Inversion (D in SOLID)** and **MVC**. The view depends solely on `SimulationController` and snapshot records, preventing tight coupling with simulation internals.
2. **How does the system ensure thread safety with Swing?**  
   Simulation clock steps are driven by `javax.swing.Timer`, guaranteeing all GUI updates happen on the Event Dispatch Thread (EDT).
3. **What is the difference between Aggregation and Composition here?**  
   `Spacecraft` **composes** its `Subsystem`s (they are created for it and die with it). `MissionControl` **aggregates** `Spacecraft` (it monitors a spacecraft passed into it from outside).
4. **How are custom exceptions used?**  
   `OrbitalIXException` is the base checked exception. Specific domain failures like `CommunicationLossException` and `UnrecoverableFaultException` signal hardware dropouts and trigger targeted safety procedures.
