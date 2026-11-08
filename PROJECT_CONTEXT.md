# OrbitalIX: Mission Control for a Satellite's Health

> MASTER CONTEXT FILE. Give this file to the AI at the start of EVERY session.
> Then the prompt is just: "Read PROJECT_CONTEXT.md and run the project."

## 0. HOW TO RUN THIS FILE (READ THIS FIRST)

### Autonomous mode: do A1 to A7 in ONE continuous run
- Do tasks A1, A2, A3, A4, A5, A6, A7 (section 9) in order, back to back.
- **Do NOT stop between tasks. Do NOT ask the user for confirmation, approval, choices or "should I continue?".**
  If something is unspecified, choose the simplest reasonable option yourself and note the decision in HANDOFF STATE.
- The ONLY thing you may ask the user, once, at the very start: to grant permissions or settings you need
  (create/edit files, run shell commands, run Java, install packages). Ask for all of them in one go, then proceed.
  If JDK 17+ is missing and you cannot install it, tell the user exactly what to install, then continue.
- After each task: compile and run (console mode for A1-A4, launch the GUI for A5-A6). Fix every error yourself,
  as many attempts as needed, before moving on. Never leave stubs, TODOs or code that does not compile.
- Then tick the task box in section 9, add a line to the Progress log, update HANDOFF STATE, and START THE NEXT TASK IMMEDIATELY.
- Do NOT do A8 or any B task unless the user asks. When A7 is done, print: a 5-line summary,
  the exact commands to run the GUI and console mode, and the line "A8 and the B tasks remain for the humans."

### Handoff protocol: another AI may take over when credits run out
The user may switch to a different AI tool in the middle of the work. If you are the new AI, CONTINUE, do not restart:
1. Read this whole file, then HANDOFF STATE (bottom), then list the real files under `src/`.
2. Run the compile command. If it fails, fix that first. The ticked boxes may be wrong; the code is the truth.
3. Resume at "Currently in progress" in HANDOFF STATE. Review its files, finish them, then continue with the next task.
4. Do NOT rewrite, rename or restructure finished code. Reuse the existing class names and the contract (section 6).
5. Credits can run out at any moment, so update HANDOFF STATE after EVERY finished package or file group
   (roughly every 10 minutes), not only at the end of a task.
6. If git is available: `git init` in A1 and commit after every task ("A3 done: state and recovery packages").
   This lets a broken run be rolled back.

## 1. The idea in plain words
A satellite is millions of kilometres away. Nobody can walk up and fix it. So software must
**watch it, notice when something goes wrong, warn the humans, and try to fix it automatically.**

Think of a hospital ICU patient monitor:
| Hospital | OrbitalIX |
|---|---|
| Patient | Spacecraft |
| Pulse, blood pressure, temperature sensors | Voltage, temperature, gyro, signal sensors |
| Doctor's rules ("pulse above 120 is bad") | Anomaly detectors |
| Alarm beeps | Alerts to the dashboard, console and log file |
| Treatment | Recovery actions (restart, backup sensor, safe mode) |

OrbitalIX is a **live mission-control dashboard** for a simulated satellite. You press Start and watch
the satellite fly. You pick a failure (or inject one by hand) and watch the system detect it,
raise alerts, change the satellite's mode and recover.

### The 60-second demo story (also the presentation and viva demo)
1. Press Start: everything glows green, the status banner says NOMINAL.
2. Pick "Thermal Runaway": the satellite's core slowly heats up, the chart climbs, the core turns amber,
   an alert appears, the banner says WARNING.
3. It keeps heating: the core turns red and pulses, the banner says CRITICAL, the system automatically enters SAFE MODE.
4. Recovery runs, temperature falls, the core returns to green.
5. In the viva: click "Inject fault" live, choose "Communication dropout", and show the exception being handled.

## 2. Evaluation requirements (must all be visibly demonstrated)
- Classes, objects, attributes, methods, constructors (incl. overloading)
- Encapsulation, abstraction, inheritance, polymorphism
- Association, aggregation, composition (each used at least once, and labelled in comments)
- UML: use-case, class, sequence, activity diagrams
- SOLID principles
- Design patterns (see section 7)
- Exception handling (custom exceptions), modularity, maintainable design
- Deliverables: source code, project report, PPT, live presentation, viva voce

## 3. Tech stack and rules for the AI
- Java 17. Run `java -version` and `mvn -v` first. If Maven exists use Maven (JUnit 5 for tests only);
  if not, use plain `javac` with `compile` and `run` scripts (.bat and .sh). No other external libraries.
- GUI uses **Swing + Java2D** (built into the JDK). No chart or UI library: everything is drawn by hand
  with `paintComponent` and `Graphics2D` (anti-aliasing ON).
- The simulation is driven by `javax.swing.Timer` so all GUI updates happen on the Swing thread.
  Never use raw threads for GUI updates.
- Package root: `com.orbitalix`. One public class per file. Java naming conventions.
- Every class gets a short Javadoc stating WHICH OOM concept or pattern it demonstrates
  (e.g. "Demonstrates: inheritance, polymorphism"). This is for the viva.
- Keep code simple and readable. No clever tricks. A student must be able to explain every line.
- After writing code, ALWAYS compile and run it and fix errors before saying you are done.
  For GUI code, confirm it starts without exceptions.
- Do not change class names, package structure or the contract in section 6 without telling the user.
- When a task is finished: tick it in section 9, add one line to "Progress log", and print the exact command to run the app.

## 4. Architecture (package -> classes)

**sensor**
- `abstract Sensor` (id, unit, history of `Reading`, `abstract double measure()`, `read()`, can switch to backup)
- `TemperatureSensor`, `VoltageSensor`, `GyroSensor`, `SignalSensor` extend Sensor
- `Reading` (value, timestamp)

**model**
- `abstract Subsystem` (name, `List<Sensor>`, health status, `abstract HealthStatus evaluateHealth()`)
- `PowerSubsystem` (solar panels + battery voltage), `ThermalSubsystem` (core + battery temperature),
  `CommunicationSubsystem` (antenna signal), `AttitudeSubsystem` (gyro: keeps the satellite pointing correctly)
- `Spacecraft` (owns the 4 subsystems, holds current `SpacecraftState`)
- `MissionControl` (monitors the spacecraft, registers alert listeners)
- `Anomaly` (sensor, severity, message, timestamp), enums `Severity`, `HealthStatus`

**detection**
- `interface AnomalyDetector { Optional<Anomaly> analyze(Sensor s); }`
- `ThresholdDetector` (value beyond a limit), `RateOfChangeDetector` (changes too fast),
  `ZScoreDetector` (statistically unusual vs. recent average)
- `DetectionEngine` (holds a list of detectors, strategies can be swapped at runtime)

**alert**
- `interface AlertListener { void onAnomaly(Anomaly a); }`
- `ConsoleAlertListener`, `FileAlertListener`
- `AlertPublisher` (subject: register / remove / notify)

**state**
- `interface SpacecraftState { void handle(Anomaly a, Spacecraft sc); String name(); }`
- `NominalState`, `WarningState`, `SafeModeState`, `CriticalState` (transitions depend on severity)

**recovery**
- `interface RecoveryAction { void execute(Spacecraft sc) throws OrbitalIXException; }`
- `RestartSubsystemAction`, `SwitchToBackupSensorAction`, `EnterSafeModeAction`
- `RecoveryPlanner` (maps an anomaly to the right action)

**simulation**
- `FaultType` enum (SENSOR_DRIFT, SPIKE, STUCK_VALUE, DROPOUT, OVERHEAT, POWER_DROP)
- `FaultInjector` (overloaded `inject(...)` methods), `Scenario` (scripted list of tick + subsystem + fault),
  `SimulationEngine` (one `tick()` step), `SubsystemFactory`, `SensorFactory`
- `DefaultSimulationController` (implements the controller contract, section 6)

**gui** (the View; it must NEVER import `model`, `sensor` or `detection` classes, only the contract)
Look and feel: **dark mission-control theme.**
- `Theme` (all colours and fonts in one place: background #0B1020, panels #121A33, text #E0E6FF,
  cyan accent #00E5FF, green #00E676, amber #FFB300, red #FF1744, grey for FAILED;
  Segoe UI for labels, Consolas/Monospaced for numbers and logs)
- `MainWindow` (JFrame). Layout: NORTH header, WEST controls, CENTER satellite, EAST chart + table, SOUTH alerts
- Header: title "ORBITALIX · MISSION CONTROL", mission clock `T+ 00:03:21`, `StatusBanner`
- `StatusBanner` (big rounded label: NOMINAL green, WARNING amber, SAFE MODE blue, CRITICAL red and pulsing)
- `SatellitePanel` (**the showpiece**, drawn with Graphics2D): twinkling starfield background and a drawn
  satellite in the middle: central body = Thermal, two solar panels left/right = Power,
  antenna dish on top = Communication, gyro ring at the bottom = Attitude.
  Each part glows green / amber / red by its subsystem's health; CRITICAL parts pulse; FAILED parts are grey.
  Each part has a small label (name + key reading). Clicking a part selects that subsystem for the chart.
- `ControlPanel` (scenario "cards" as buttons, Start / Pause / Reset, speed slider,
  fault injection: subsystem dropdown + fault type dropdown + "Inject fault" button)
- `TelemetryPanel` (JTable: subsystem, sensor, value, unit, status; rows coloured by status)
- `LiveChartPanel` (line chart of the selected sensor's last 60 values, with a dashed line at the warning limit)
- `AlertLogPanel` (terminal-style scrolling log, monospaced, coloured by severity, newest at the bottom)
- `GuiAlertListener` (implements `AlertListener`, pushes alerts into `AlertLogPanel`)

**util**: `EventLogger` (Singleton, writes to console and `logs/orbitalix.log`)

**exception**: `OrbitalIXException` (checked, base), `SensorFailureException`,
`InvalidTelemetryException`, `CommunicationLossException`, `UnrecoverableFaultException`

**app**: `Main` (no argument = open GUI; `--cli` = console menu to choose a scenario, run, show summary)

## 5. Relationships (must be explicit in code and in the class diagram)
- Composition: Spacecraft -> Subsystems; Subsystem -> Sensors (they die with the parent)
- Aggregation: MissionControl -> Spacecraft; DetectionEngine -> AnomalyDetectors; AlertPublisher -> AlertListeners;
  MainWindow -> its panels
- Association: Anomaly -> Sensor; RecoveryAction -> Spacecraft; panels -> SimulationController
- Inheritance: Sensor and Subsystem hierarchies, exception hierarchy
- Realization: detectors, listeners, states, recovery actions, controller implement their interfaces

## 6. THE CONTRACT between the core and the GUI
The GUI talks ONLY to these. This is MVC and Dependency Inversion.

```java
// package com.orbitalix.simulation
public interface SimulationController {
    List<String> getScenarioNames();
    List<String> getSubsystemNames();
    void loadScenario(int scenarioIndex);
    void start();
    void pause();
    void reset();
    void setSpeed(int ticksPerSecond);
    void injectFault(String subsystemName, FaultType type);
    void addTickListener(TickListener listener);
    void addAlertListener(AlertListener listener);   // AlertListener lives in com.orbitalix.alert
}
public interface TickListener { void onTick(SimulationSnapshot snapshot); }
public record SimulationSnapshot(int tick, String spacecraftState, List<SubsystemSnapshot> subsystems) {}
public record SubsystemSnapshot(String name, HealthStatus status, List<SensorSnapshot> sensors) {}
public record SensorSnapshot(String sensorId, double value, String unit, HealthStatus status) {}
```

## 7. Design patterns and where they live
| Pattern | Where | Why |
|---|---|---|
| Strategy | AnomalyDetector implementations | swap detection algorithms |
| Observer | AlertPublisher / AlertListener, TickListener | decouple detection and engine from reporting and GUI |
| State | SpacecraftState classes | behaviour changes with operating mode |
| Factory | SubsystemFactory, SensorFactory | centralise object creation |
| Singleton | EventLogger | one shared log |
| Command | RecoveryAction | encapsulate recovery operations |
| MVC | gui (View), SimulationController (Controller), model + simulation (Model) | GUI never touches core classes |

## 8. SOLID mapping (the report and viva must state this)
- S: Sensor reads, DetectionEngine detects, RecoveryPlanner recovers, Logger logs, each GUI panel shows one thing
- O: add a new detector or sensor type without editing existing classes
- L: any Sensor/Subsystem/Detector subclass works wherever its parent is expected
- I: small interfaces (AnomalyDetector, AlertListener, RecoveryAction, TickListener)
- D: GUI and engines depend on interfaces (`SimulationController`, `AnomalyDetector`), not concrete classes

## 9. Task list

### Member A (Bharat): ALL the code, about 80% of the work, tonight
Do in this order. Each task must compile and run before starting the next.
- [x] A1 (~40 min) Project setup + contract files (section 6) + `sensor`, `model`, `exception` packages
- [x] A2 (~30 min) `detection` package + `alert` package + `EventLogger` singleton
- [x] A3 (~30 min) `state` and `recovery` packages
- [x] A4 (~40 min) `simulation` package: engine, fault injector, factories, the 6 scenarios (section 10),
      `DefaultSimulationController`, and the CLI mode of `Main`. Test in CLI mode.
- [x] A5 (~60 min) GUI part 1: `Theme`, `MainWindow`, `Header + StatusBanner`, `ControlPanel`, `TelemetryPanel`,
      `LiveChartPanel`, `AlertLogPanel`, `GuiAlertListener`; the app opens and runs a scenario
- [x] A6 (~45 min) GUI part 2: `SatellitePanel` (drawn satellite, starfield, glowing/pulsing parts, click to select)
      + polish and spacing
- [ ] A7 (~30 min) Integration: run all 6 scenarios in CLI and GUI, fix bugs, write `README.md` (how to run, how to use),
      take 6-8 screenshots into `docs/screenshots/`
- [ ] A8 (tomorrow, ~30 min) Viva prep: AI explains every package in plain language; A answers 20 mock viva questions

### Member B (teammate): documentation and checking, about 20% of the work
- [ ] B1 (can start NOW, needs only this file) Requirements list + use-case diagram (PlantUML) + report chapters 1-4
- [ ] B2 (after A7) Class diagram (PlantUML), generated FROM THE ACTUAL CODE
- [ ] B3 (after A7) Sequence diagram (one anomaly: sensor reading to detection to alert to recovery) + activity diagram (one tick)
- [ ] B4 (after A7) Tests: at least 12 tests covering detectors, state transitions, exceptions, factories
- [ ] B5 Full report (outline in section 11), using the screenshots
- [ ] B6 PPT (outline in section 11)
- [ ] B7 Viva Q&A sheet: 30 likely questions with short answers, tied to the real code

## 10. Demo scenarios
1. Nominal run (no faults, no alerts)
2. Solar panel degradation (POWER_DROP): power part goes amber, WarningState
3. Thermal runaway (OVERHEAT): core goes amber then red, CriticalState then SafeModeState, recovery cools it
4. Gyro drift (SENSOR_DRIFT): caught by ZScoreDetector, switch to backup sensor
5. Communication blackout (DROPOUT): throws CommunicationLossException, handled and logged
6. Cascading failure (power, then thermal, then attitude): ends in UnrecoverableFaultException, handled gracefully

Each scenario is a scripted list of (tick number, subsystem, fault type). The user can also inject faults by hand.

## 11. Report and PPT outlines
**Report:** 1 Introduction and problem statement (use the hospital analogy); 2 Objectives and scope; 3 Requirements;
4 Use-case diagram; 5 System design (class diagram, relationships); 6 Sequence and activity diagrams;
7 OO concepts used (table: concept -> class/file); 8 Design patterns incl. MVC (table + short justification);
9 SOLID mapping; 10 Exception handling strategy; 11 GUI design (layout, screenshots);
12 Implementation (packages, key code snippets); 13 Testing and results (screenshots of scenarios);
14 Limitations and future work; 15 Conclusion; References.

**PPT (about 13 slides):** title; problem (satellite far away, cannot be repaired by hand); objectives; use case;
architecture; class diagram; patterns; SOLID; sequence diagram; GUI screenshot; demo scenarios; testing;
conclusion and future work.

## HANDOFF STATE (AI: keep this updated. The next AI reads it first.)
- Last updated by: Antigravity, 2026-11-09 01:48
- Currently in progress: A6 finished. Starting A7 (Integration, testing scenarios, README.md, screenshots).
- Next step: Run all scenarios in CLI and GUI, verify integration, generate docs/screenshots, and write README.md.
- Compile command: `javac --release 17 -encoding UTF-8 -d out @sources.txt` after building sources.txt with forward-slash quoted paths (see compile.bat). Maven is not installed.
- Run commands: GUI: `java -cp out com.orbitalix.app.Main`   Console: `java -cp out com.orbitalix.app.Main --cli`
- Decisions made: Full Swing+Java2D GUI implemented: Theme, HeaderPanel, StatusBanner (with pulsing critical animation), ControlPanel (scenarios, controls, speed, manual fault injector), SatellitePanel (twinkling starfield, glowing/pulsing subsystems, click-to-chart), LiveChartPanel (custom Java2D line chart with warning threshold), TelemetryPanel (colorized real-time JTable), AlertLogPanel (monospaced terminal style log), and GuiAlertListener. MainWindow wires MVC. Tested GUI instantiation and CLI modes.
- Known issues: none.

## Progress log
(AI: append one line per finished task, format `YYYY-MM-DD A1 done: <what exists now>`)
2026-11-09 A1 done: javac scripts, section 6 contract, sensor, model, exception, and a smoke Main.
2026-11-09 A2 done: detection (threshold, rate of change, z-score, engine), alert publisher, listeners, EventLogger.
2026-11-09 A3 done: state and recovery packages, safe mode cooling, and passing smoke test.
2026-11-09 A4 done: simulation package (engine, injector, factories, 6 scenarios, controller) and Main CLI runner.
2026-11-09 A5 done: GUI core (Theme, Header, StatusBanner, ControlPanel, TelemetryPanel, LiveChartPanel, AlertLogPanel, GuiAlertListener).
2026-11-09 A6 done: SatellitePanel (Java2D starfield, glowing parts, pulse animation, interactive component selection).
