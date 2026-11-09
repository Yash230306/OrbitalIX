# OrbitalIX: Mission Control Suite for Spacecraft Failure Management and Autonomous Recovery

> MASTER CONTEXT FILE. Give this file to the AI at the start of EVERY session.
> The whole prompt is: "Read PROJECT_CONTEXT.md and run the project. GitHub repo: <url>" (the repo URL is optional)

## 0. HOW TO RUN THIS FILE (READ THIS FIRST)
(If the AI's first reply is a question other than the one-time permission request, it is not following this file.)

### Autonomous mode: do A1 to A17 in ONE continuous run
- Do tasks A1 to A17 (section 11) in order, back to back.
- **Do NOT stop between tasks. Do NOT ask the user for confirmation, approval, choices or "should I continue?".**
  If something is unspecified, choose the simplest reasonable option yourself and note it in HANDOFF STATE.
- The ONLY thing you may ask the user, once, at the very start: to grant permissions or settings you need
  (create/edit files, run shell commands, run Java, install packages). Ask for all of them in one go, then proceed.
  If JDK 17+ is missing and you cannot install it, tell the user exactly what to install, then continue.
- After each task: compile and run (console mode for A1-A7, GUI plus screenshot harness for A8-A16). Fix every error yourself,
  as many attempts as needed, before moving on. Never leave stubs, TODOs or code that does not compile.
- Then tick the task box in section 11, add a line to the Progress log, update HANDOFF STATE, and START THE NEXT TASK IMMEDIATELY.
- Do NOT do A18 or any B task unless the user asks. When A17 is done, print: a 5-line summary,
  the exact commands to run the GUI and console mode, and the line "A18 and the B tasks remain for the humans."
- Work package by package. Do not regenerate whole files you have already written; edit them.

### Release gates (the project must be demonstrable at every stage)
The work is split into 4 releases. At the end of each one: compile everything, run `--selftest`, run the screenshot harness
(from R2 on), fix problems, then `git tag R1` (R2, R3, R4) and push the tag.
| Release | After | What exists |
|---|---|---|
| R1 | A7 | Full simulation engine, 15 failures, advisor, autopilot, self-healing, console mode, `--selftest` |
| R2 | A10 | Polished GUI core: shell, Dashboard, pseudo-3D Cutaway, Orbit map |
| R3 | A14 | Planner, Systems, Crew, What-if, Replay, Console, Reports, Settings, scoring, sound, effects |
| R4 | A17 | Tuned, visually QA'd, documented, pushed |
If time or credits are short: finish R1, then R2, then R3 in order. Never start polishing a later release before the
earlier one is complete. A later task that breaks the build must be fixed or reverted to the last tag before moving on.

### GitHub: commit and push after every task
- The user's first message may include a repo URL (https://github.com/<user>/<repo>). Also check `gh auth status`.
- In A1: `git init`, add a `.gitignore` (build output such as `target/` and `out/`, `*.class`, IDE folders, `logs/`;
  keep `docs/`), make the first commit, and set up the remote:
  - If a repo URL was given: `git remote add origin <url>`.
  - Else if `gh` is logged in: `gh repo create orbitalix --private --source=. --remote=origin`.
  - Else: do NOT ask. Work with local commits only, write "GitHub push: NOT CONFIGURED" in HANDOFF STATE and continue.
    Retry the push after each task in case the user has fixed the login meanwhile.
- After EVERY task, and after every HANDOFF STATE update: `git add -A`, commit with a clear message ("A6 done: actions and advisor"),
  then `git push -u origin HEAD`. If the push fails, fix it if you can (for example `git pull --rebase`, then push).
  Never use `--force`. If it still fails, note it in HANDOFF STATE and continue working.
- Never commit secrets (tokens, passwords, keys) and never write them into HANDOFF STATE. Only the repo URL goes there.
- The teammate pulls from this repo, so the last commit must always compile.

### Handoff protocol: another AI may take over when credits run out
The user may switch to a different AI tool in the middle of the work. If you are the new AI, CONTINUE, do not restart:
1. If the folder has no project yet but HANDOFF STATE or the user gives a repo URL, `git clone` it first (or `git pull`).
   Then read this whole file, then HANDOFF STATE (bottom), then list the real files under `src/`.
2. Run the compile command. If it fails, fix that first. The ticked boxes may be wrong; the code is the truth.
3. Resume at "Currently in progress" in HANDOFF STATE. Review its files, finish them, then continue with the next task.
4. Do NOT rewrite, rename or restructure finished code. Reuse the existing class names and the contract (section 8).
5. Credits can run out at any moment, so update HANDOFF STATE after EVERY finished package or file group
   (roughly every 10 minutes), not only at the end of a task.
6. Commit and push after every task (see the GitHub rules above), so work is never lost and the next AI can pull it.
   This lets a broken run be rolled back.

## 1. The idea in plain words
Think of the **co-pilot in an aircraft**. It watches every gauge. When fuel gets low it says "fuel low, reduce speed".
If the pilot does not react in time, it takes the action itself.

OrbitalIX is that co-pilot for a **crewed spacecraft**, wrapped in a complete **mission-control suite**: the operator plans a
mission (destination, spacecraft type, difficulty), launches it, and watches a living, animated control room: a rotating
pseudo-3D spacecraft, an orbit map, round gauges, a crew panel, a system-health tree and a master-caution panel.

The simulated spacecraft must **reach its destination safely.** Fuel, battery and oxygen drain all the time. The human operator
can push the craft harder (raise the throttle limit, raise alarm limits, override safety limits). **15 kinds of failures** strike,
caused by the human's choices, by random space events, by scripted scenarios, or by a seeded "Chaos Director".
OrbitalIX continuously does seven things:

1. **Detects** what is wrong (anomaly detectors).
2. **Predicts** how long until it becomes critical ("fuel runs out in 42 ticks", "debris impact in 18 ticks").
3. **Advises** the human what to do, ranked, with the reason, and can **preview** the outcome of any advice before executing it (what-if).
4. **Acts by itself** if the human does not respond in time (autopilot), depending on the autonomy mode the human chose.
5. **Heals itself**: problems it can fix safely are fixed at once, in every mode (self-healing, section 4b).
6. **Manages the crew**: crew members have health, stress and radiation dose, and can be sent on tasks (EVA repair, medical care).
7. **Records and scores**: everything is recorded (replayable), scored, graded, and exported as a mission report.

A key idea, visible in the demo: **commands from Earth are delayed** (uplink delay grows with distance). A human cannot react fast
enough to a 5-tick-late command, which is exactly why onboard autonomy and self-healing matter.

### The 2-minute demo story (also the presentation and viva demo)
1. Boot splash, then the **Planner**: choose Mars Transfer, Crew Capsule, Hard, press LAUNCH (3-2-1 animation).
2. **Dashboard**: gauges animate, master caution panel is dark. Raise MAX THROTTLE past its design value (the system warns,
   a toast appears), push throttle to 100%. An advisor card appears: "Fuel runs out in 38 ticks, mission needs 52." Click **Preview**:
   a what-if panel shows "Reduce throttle to 60%: arrives with 11% fuel". Click Execute: the command shows in the **uplink queue** with a
   delay countdown before it takes effect.
3. Switch to the **Cutaway** view: the 3D spacecraft rotates, the flame is huge. Inject a radiation storm: the view tints purple,
   the crew panel shows rising dose, a card says "Enter radiation shelter".
4. Inject a gyro drift: no card waits for you. **Self-healing** recalibrates it at once (SELF-HEALING in the log, a toast).
5. Switch to FULL AUTO, inject a debris threat and a computer fault: watch the **Orbit map** show the debris cone, the autopilot
   evade, and the computer reboot. A crew member is sent on an EVA repair.
6. Type `status` and `inject hull_breach` in the **Console**.
7. Open **Replay**, drag the scrubber back to the moment of the fuel warning: every view shows the past.
8. **Reports**: grade stamp (A), achievements unlocked, export the HTML report.

## 2. Evaluation requirements (must all be visibly demonstrated)
- Classes, objects, attributes, methods, constructors (incl. overloading and chaining)
- Encapsulation, abstraction, inheritance, polymorphism
- Association, aggregation, composition (each used at least once, and labelled in comments)
- UML: use-case, class, sequence, activity diagrams
- SOLID principles
- Design patterns (section 9: at least 16 are implemented)
- Exception handling (custom exceptions), modularity, maintainable design
- Deliverables: source code, project report, PPT, live presentation, viva voce

## 3. Tech stack and rules for the AI
- Java 17. Run `java -version` and `mvn -v` first. If Maven exists use Maven (JUnit 5 for tests only);
  if not, use plain `javac` with `compile` and `run` scripts (.bat and .sh). No other external libraries.
- GUI uses **Swing + Java2D** (built into the JDK) but is **fully custom drawn**: no default Swing look anywhere.
  Components (buttons, toggles, sliders, gauges, cards, charts, tabs) are our own classes painted with `Graphics2D`.
  No chart, UI or 3D library: the 3D spacecraft uses our own tiny 3D engine (section 6, `gui.render3d`).
- Two timers: the **simulation timer** (`javax.swing.Timer`, ticks per second) and the **animation timer** (16 ms, about 60 FPS).
  The animation loop interpolates and eases visual values between ticks so everything moves smoothly. All GUI work stays on the Swing thread.
  Never use raw threads for GUI updates.
- Package root: `com.orbitalix`. One public class per file. Java naming conventions.
- Every class gets a short Javadoc stating WHICH OOM concept or pattern it demonstrates
  (e.g. "Demonstrates: inheritance, polymorphism, Template Method"). This is for the viva.
- Keep code readable. A student must be able to explain every class. Prefer clear OO design over clever tricks.
- Simulation must be deterministic when a seed is given (one `DeterministicRandom` class, copyable, seeded from `MissionConfig`).
- After writing code, ALWAYS compile and run it and fix errors before saying you are done.
- Do not change class names, package structure or the contract in section 8 without noting it in HANDOFF STATE.

### 3a. Built-in verification tools (build these early, use them constantly)
- `--selftest` (A7): runs every scenario x every autonomy mode x every difficulty headless, prints a results table
  (outcome, ticks, fuel left, crew alive, score) and checks: determinism (same seed = same result), `fork()` equivalence
  (a what-if fork with no changes gives the same future as the real run), no exceptions escape, MANUAL loses Cascading Failure,
  FULL_AUTO survives it on NORMAL. Exit code non-zero on failure.
- `--screenshots <dir>` (A8): runs HEADLESS (`-Djava.awt.headless=true`). Builds every `View` as a plain JPanel (MainWindow only wraps
  the views), drives a deterministic scenario to an interesting moment (e.g. radiation storm and low fuel), paints each view to a PNG at
  1600x900 in every theme. If you can view images, open them and fix every visual defect (clipping, overlap, low contrast, empty areas,
  text cut off). If you cannot view images, at least check each PNG is not blank (pixel variance) and that no view throws.

### 3b. GUI quality bar (the GUI must look professional, not like a student Swing app)
- Dark "glass" look: rounded cards (12 px radius), soft drop shadows, subtle gradients, 1 px translucent borders, an 8 px spacing grid.
- Every interactive component has hover, pressed, disabled and keyboard-focus states, a hand cursor, and a styled tooltip.
- Smooth motion: gauge needles and bars ease to new values, cards slide/fade in and out, view changes cross-fade, numbers count up.
- Rendering hints: anti-aliasing, text anti-aliasing, quality interpolation, stroke control. HiDPI aware. Minimum window 1280x800,
  fully resizable, layouts never clip. Cache static images (starfield, panel backgrounds) so painting stays under 8 ms.
- Colour is meaning: green nominal, amber warning, red critical (pulsing), blue safe mode, grey failed, cyan for interactive accents.
  Never rely on colour alone (also use icons or labels).
- Consistent typography: one UI font and one monospace font from the system (use fallbacks), sizes from a small scale in `Theme`.
- Keyboard: Space start/pause, 1..0 switch views, F11 fullscreen, Ctrl+S screenshot, Ctrl+K focus console, Esc close dialogs.
- Eye-catching moments: boot splash, 3-2-1 launch countdown, grade stamp slam, screen shake on debris impact, red vignette on
  CRITICAL, purple tint on radiation, venting particles on breach, engine plume that scales with throttle.

## 4. THE 15 FAILURE FEATURES (each = a problem, how it is detected, what is advised, what action fixes it)
Every feature needs: a `Fault` class that causes it, at least one sensor reading that shows it, one `RecommendationRule`,
and at least one `CorrectiveAction`. Each must be demonstrable in the GUI, in the console view and in CLI mode.

| # | Feature | What goes wrong | Detected / predicted by | Advice and corrective action(s) |
|---|---|---|---|---|
| 1 | Fuel depletion and leak | Fuel drains fast (high throttle) or leaks | TrendPredictor: ticks to empty vs ticks mission needs | `ReduceThrottleAction`, `CrossFeedReserveFuelAction`, `AbortBurnAction` |
| 2 | Engine overheating | Temperature rises with throttle | ThresholdDetector + trend to limit | `ActivateRadiatorsAction`, `ReduceThrottleAction` |
| 3 | Coolant pump failure | Radiators stop cooling | Temperature rises although radiators are on | `RestartCoolantPumpAction` (takes 3 ticks to work) |
| 4 | Battery drain / power shortage | Load exceeds generation | Trend to MIN_BATTERY | `ShedNonEssentialLoadAction` |
| 5 | Solar array failure | Generation drops to 0 | Voltage drop (RateOfChangeDetector) | `ReorientSolarPanelsAction` (restores 50%), `ShedNonEssentialLoadAction`; crew `EVA_REPAIR` restores it fully |
| 6 | Communication blackout | Primary antenna dies | SignalSensor reads 0, `CommunicationLossException` | `SwitchToBackupAntennaAction` |
| 7 | Gyro drift / attitude loss | Pointing error grows | ZScoreDetector on gyro | `RecalibrateGyroAction`, `SwitchToBackupSensorAction` |
| 8 | Oxygen depletion / CO2 buildup | Scrubber fails, CO2 rises, oxygen drains | Atmosphere sensors, trend to limit | `SwitchToBackupScrubberAction`, `EnterSafeModeAction` |
| 9 | Cabin pressure leak (hull breach) | Micro-meteoroid hole, pressure and oxygen drop | Pressure RateOfChangeDetector | `SealBreachAction` (uses sealant stock) |
| 10 | Radiation storm | Radiation climbs, causes sensor spikes, computer errors and crew dose | RadiationSensor threshold | `EnterRadiationShelterAction` (crew shelter, electronics shielded, throttle capped) |
| 11 | Navigation drift | Course error grows, progress slows, trajectory bends away | CourseSensor trend | `CourseCorrectionBurnAction` (costs 3 fuel) |
| 12 | Debris collision warning | A debris object will hit in N ticks (predicted) | Threat countdown, shown on the Orbit map | `EvasiveManeuverAction` (costs 4 fuel, adds a little course error); if ignored: breach + a subsystem damaged + screen shake |
| 13 | Onboard computer fault | Error rate rises, then crash: telemetry freezes 5 ticks, **autopilot and self-healing go offline** | Error-rate threshold | `RebootComputerAction` (telemetry blanks 3 ticks), `SwitchToBackupComputerAction` |
| 14 | Engine misfire | Thrust efficiency drops to 55%, same fuel burn | Progress per fuel falls | `SwitchToBackupEngineAction` (85%), `ReduceThrottleAction` |
| 15 | Unsafe limit override | Human raised a limit past its design maximum | The limit change itself | `RestoreSafeLimitsAction`; `LimitOverrideException` is caught and logged |

Sensor faults (drift, spike, stuck value) can hit any sensor. They are implemented as **runtime Decorators** wrapped around the sensor
(`DriftDecorator`, `SpikeDecorator`, `StuckDecorator`); `SwitchToBackupSensorAction` unwraps them.

### 4b. Self-healing and the response chain (Chain of Responsibility)
Every `CorrectiveAction` has `ActionRisk risk()`:
- **SAFE** = reversible, costs no fuel or one-time resource, does not override a human decision.
  SAFE actions: ActivateRadiators, RestartCoolantPump, ShedNonEssentialLoad, ReorientSolarPanels, SwitchToBackupSensor,
  SwitchToBackupAntenna, RecalibrateGyro, RebootComputer.
- **COSTLY** = spends fuel or a limited resource, changes the mission, or overrides the human's choice: ReduceThrottle,
  CrossFeedReserveFuel, AbortBurn, SwitchToBackupScrubber, SealBreach, EnterRadiationShelter, CourseCorrectionBurn,
  EvasiveManeuver, SwitchToBackupComputer, SwitchToBackupEngine, RestoreSafeLimits, EnterSafeMode.

Every new recommendation travels down a **chain of handlers**, each of which either handles it or passes it on:
1. `SelfHealingHandler`: executes SAFE recommendations immediately when self-healing is ON (every autonomy mode, including MANUAL).
   Counts attempts per issue. After 2 failed attempts or if the problem returns, it passes it on marked "escalated".
2. `AutopilotHandler`: COSTLY recommendations follow the autonomy mode (MANUAL: passes on; TIMED: acts when the response window
   expires; FULL_AUTO: acts immediately, highest priority first).
3. `CrewHandler`: if the best fix needs a person (EVA repair, medical care) and a suitable crew member is free, assigns the task
   (FULL_AUTO and TIMED only; MANUAL leaves it to the human).
4. `EscalationHandler`: leaves the card for the human, marked NEEDS HUMAN with a clear reason when escalated, and publishes a HIGH event.

Rules: self-healing and the autopilot both need the onboard computer; while it is crashed the first three handlers are OFFLINE.
Onboard handlers are NOT affected by the uplink delay (that is the whole point). Turning self-healing OFF lets the human demonstrate pure manual control.
Event type SELF_HEALING / ACTION_BY_AUTOPILOT / ACTION_BY_HUMAN / CREW is recorded with `decidedBy`.

## 4c. THE MISSION CONTROL SUITE (features beyond the failures)
| # | Feature | Summary |
|---|---|---|
| S1 | Mission Planner | Choose destination, spacecraft type, difficulty, seed, scenario, chaos director. Shows an estimate card (ETA, fuel margin) and a LAUNCH button with a 3-2-1 countdown |
| S2 | Orbit map | Earth, destination, planned path (dashed), actual trail (fading), predicted path (dotted), spacecraft, debris cones, comm link with delay label, zoom and pan |
| S3 | Pseudo-3D cutaway | Rotating, mouse-draggable 3D spacecraft (own 3D engine), 8 clickable parts glowing by health, engine plume, radiation shield flash, debris, breach venting |
| S4 | Systems tree | Composite tree Spacecraft > Subsystem > Sensor with health roll-up, sparkline per sensor, active-fault badges, and a **dependency graph** with animated failure cascades |
| S5 | Crew panel | 3-4 crew members (Commander, Flight Engineer, Medical Officer, Pilot) with health, stress, radiation dose, skills; assign EVA_REPAIR, MEDICAL_CARE, MANUAL_REBOOT, REST |
| S6 | Uplink delay and command queue | Operator commands reach the craft after a delay (grows with distance); queue and countdown visible; blocked during antenna blackout |
| S7 | Command console | Terminal with history and Tab completion: `help status throttle inject exec dismiss autonomy heal limits crew task scenario seek report clear` |
| S8 | Master caution panel | Aviation-style annunciator grid of 20+ tiles, flashing master caution, alarm tones (synthesised) |
| S9 | What-if preview | "Preview" on any advisor card simulates 30 ticks ahead on a forked copy and shows the predicted outcome before you execute |
| S10 | Replay and timeline | Every tick is recorded; drag the scrubber to any past moment and ALL views show it; event markers; human vs autopilot vs self-healing action tally |
| S11 | Scoring and achievements | Score, grade S/A/B/C/D/F, 10 achievements (badges) with a gallery |
| S12 | Reports and export | HTML mission report with embedded charts-as-tables and timeline, CSV telemetry export, report preview in the GUI |
| S13 | Themes and settings | 4 themes (Dark Mission, Neon, Projector light, High Contrast), sound, volume, animation quality, effects, persisted to `~/.orbitalix/settings.properties` |
| S14 | Chaos Director | Seeded generator that adds random failures following a pressure curve scaled by difficulty; never creates an unsolvable situation |
| S15 | Toasts, splash, effects | Boot splash with progress lines, toast notifications, vignette, screen shake, tint, particles |

Total: 15 failure features + 6 intelligence features (3 detectors, predictor, advisor, autonomy levels, self-healing/chain, what-if) + 15 suite features.

Achievements (implement as `Achievement` objects): Ice Cold (never above the temperature warning), Fuel Miser (arrive with over 40% fuel),
Hands-Off (finish in FULL_AUTO with zero human actions), Phoenix (recover from CRITICAL), Dodge This (evade 3 debris),
Cascade Survivor (finish Cascading Failure), Cowboy (override a limit and still succeed), Guardian Angel (no crew member injured),
Quick Fix (resolve a CRITICAL card within 3 ticks), Whatif Wizard (use Preview 5 times).

## 5. The rules of the simulated world (implement these; tune constants as needed)
One tick = one simulation step. Default speed 2 ticks per second on screen (speed slider 1 to 20).

**Mission configuration** (`MissionConfig`, built by `MissionConfigBuilder`):
- `Destination` enum (fields and behaviour): MOON (distance 80, uplink delay 1, radiation base 0.1), MARS_TRANSFER (120, delay 3, 0.2),
  ASTEROID_BELT (160, delay 5, 0.35, debris rate x2).
- `CraftType` enum built by a `CraftFactory` (Abstract Factory, one factory per craft): EXPLORER (fuel 100, oxygen 100, 3 crew, sealant 3, balanced),
  FREIGHTER (fuel 160, oxygen 60, 2 crew, sealant 2, progress x0.8), CREW_CAPSULE (fuel 70, oxygen 140, 4 crew, sealant 5, strong life support).
- `Difficulty` enum with abstract methods: EASY (fault rate x0.5, burn x0.9, response window x1.5), NORMAL (x1), HARD (faults x1.6, burn x1.1,
  window x0.6, uplink delay on by default).

**Physics**
- **Mission:** progress per tick = throttle% x 0.02 x engine efficiency x course factor x craft factor,
  where course factor = max(0.3, 1 - courseError/30). SUCCESS at the destination distance. LOST if fuel hits 0 before arrival,
  if oxygen hits 0, if all crew are dead, or on an unrecoverable fault.
- **Fuel:** main tank (craft-dependent) plus a reserve tank of 20 (only via cross-feed). Burn per tick = 0.1 + 1.4 x (throttle/100)^2 (x difficulty). FUEL_LEAK adds extra burn.
- **Battery:** capacity 100. Change per tick = solar generation (0.6, 0 if solar failure, 0.3 if reoriented) minus load
  (0.2 + 0.3 x throttle/100 + 0.2 if non-essential systems on). At 0, subsystems start failing.
- **Temperature:** target = 20 + 0.75 x throttle + 5 (non-essential on) - 15 (radiators active and pump working). Actual moves 10% toward target per tick.
- **Gyro:** drift grows slowly; much faster under a drift fault; recalibrate resets it.
- **Comm:** primary antenna strong normally; DROPOUT sets it to 0 until the backup antenna is used.
- **Oxygen:** crew uses 0.2 per crew-member-share per tick (more with a breach). **CO2:** +0.05%/tick, scrubber removes 0.08%/tick.
  Scrubber failure stops removal. CO2 over 4% warning, over 6% critical. Oxygen under 25 warning.
- **Cabin pressure:** nominal 101 kPa. Breach: -2 kPa/tick until sealed. Under 80 warning, under 60 critical.
- **Radiation:** baseline from the destination. A storm ramps to about 8 over 10 ticks and lasts 25 ticks. Over 3 warning, over 6 critical.
  Un-sheltered electronics get random sensor spikes and the computer error rate rises; crew dose rises.
- **Navigation:** course error grows 0.05/tick (faster with misfire or gyro drift). Over 5 warning, over 10 critical.
- **Debris:** a threat with time-to-impact (default 30 ticks, shown on the Orbit map). Evasive maneuver in time = no impact.
  Impact = breach + one random subsystem FAILED + screen shake.
- **Computer:** error rate rises under a computer fault or radiation. If not fixed in 15 ticks it crashes: telemetry frozen
  5 ticks and the autopilot and self-healing are OFFLINE until reboot or the backup computer.
- **Engine:** efficiency 100%; misfire 55%; backup engine 85%.
- **Limits** (`OperatingLimits`): MAX_THROTTLE (default 80, design max 100), MAX_TEMPERATURE (alarm 85, design max 100),
  MIN_BATTERY (20), MIN_FUEL_RESERVE (15). Beyond a design value needs the OVERRIDE checkbox, raises a `LimitOverrideException`
  that is caught, logged as a WARNING event, and applied anyway. Throttle cannot exceed MAX_THROTTLE.

**Crew** (`CrewMember` abstract class; subclasses `Commander`, `FlightEngineer`, `MedicalOfficer`, `Pilot`, each overriding its skill bonus):
- Each has health (0-100), stress (0-100), radiation dose. Stress rises with CRITICAL conditions and falls while resting. Health falls with low oxygen,
  high CO2, low pressure or high dose. Death at health 0 (big score penalty).
- Tasks are Command objects with a duration: `EVA_REPAIR` (10 ticks, engineer 6; fully repairs a FAILED subsystem; triple radiation dose; not allowed during a storm),
  `MEDICAL_CARE` (8 ticks, +30 health, medical officer faster), `MANUAL_REBOOT` (5 ticks, restarts a crashed computer), `REST` (lowers stress).
  One task at a time per person.

**Orbit model** (`OrbitModel`): path from Earth (0,0) to the destination is a quadratic Bezier curve. The planned position is at the
progress fraction; the actual position is offset sideways by course error. The trail keeps the last 200 positions. The predicted path
extrapolates the remaining distance at the current speed and course error. Debris objects have a position approaching the craft.

**Uplink** (`CommandChannel` interface: `DirectChannel`, and `DelayedChannel` which is a Proxy around the direct one): operator commands
(throttle, limits, autonomy, execute or dismiss recommendation, crew tasks) wait for the destination's delay in ticks when uplink delay is ON.
`injectFault` and the Console `inject` command are debug tools and are immediate. During a communication blackout the queue is blocked and
a command times out after 20 ticks with a logged `CommunicationLossException`.

**Scoring** (`ScoreKeeper`): on success 500 base + fuel% x 2 + battery% + oxygen% + average crew health x 2, then minus 50 per unresolved critical event,
minus 100 per crew death, plus 25 per successful self-heal, plus 10 per achievement. Grade: S 900+, A 750+, B 600+, C 450+, D 300+, else F. On failure: only partial progress points.

- **Tuning goal:** at 50% throttle with no faults the mission completes with 40-55% fuel left; at 100% throttle it nearly runs dry;
  each scripted scenario reaches a critical situation within 60-120 ticks; every feature can be fixed in time if the human or autopilot reacts,
  and can lose the mission if ignored; Cascading Failure on NORMAL is lost in MANUAL and survived in FULL_AUTO.

## 6. Architecture (package -> classes)
Key ideas: the **GUI imports only `com.orbitalix.api`**; the engine never imports `gui`. Everything stateful in the simulation implements
`Copyable<T>` (deep `copy()`), which powers the what-if preview (Prototype).

**api**: the contract (section 8): `SimulationController`, listeners, records, enums.

**util**: `Copyable<T>`, `Identifiable`, `DeterministicRandom` (copyable), `RingBuffer<T>` (generic, bounded, `Iterable<T>`),
`EventLogger` (Singleton, console + `logs/orbitalix.log`), `Settings` (Singleton, Properties file), `Clock`/tick formatting.

**config**: `MissionConfig` (immutable) with `MissionConfigBuilder` (Builder), enums `Destination`, `CraftType`, `Difficulty`,
`CraftFactory` (Abstract Factory) with `ExplorerFactory`, `FreighterFactory`, `CrewCapsuleFactory`.

**sensor**: `interface Sensor` (id, unit, `measure()`, history, `SystemComponent`), `abstract BaseSensor`, concrete `TemperatureSensor`, `VoltageSensor`,
`PressureSensor`, `GyroSensor`, `SignalSensor`, `RadiationSensor`, `AtmosphereSensor`, `CourseSensor`, `ComputerSensor`;
`abstract SensorDecorator` with `NoiseDecorator`, `DriftDecorator`, `SpikeDecorator`, `StuckDecorator`; `Reading` (immutable record).

**resource**: `abstract Resource` (level, capacity, `consume()`, `refill()`, `ticksToEmpty()`), `FuelTank` (main + reserve), `Battery`, `OxygenSupply`, `SealantStock`.

**fault**: `abstract Fault` (`apply(Spacecraft)`, `tick(Spacecraft)`, `isActive()`, `describe()`, `FaultType type()`), one subclass per fault
(`FuelLeakFault`, `OverheatFault`, `CoolantFailureFault`, `SolarFailureFault`, `DropoutFault`, `GyroDriftFault`, `ScrubberFailureFault`,
`HullBreachFault`, `RadiationStormFault`, `NavDriftFault`, `DebrisThreatFault`, `ComputerFault`, `EngineMisfireFault`, `SensorFaultFault`),
`FaultFactory` (Factory Method: `FaultType` -> `Fault`), `FaultInjector` (overloaded `inject(...)`).

**model**: `interface SystemComponent` (Composite: `name()`, `health()`, `children()`, `accept(ReportVisitor)`), `abstract Subsystem`
(Template Method: `final update()` calls `applyEnvironment()`, `applyPhysics()`, `refreshHealth()`),
8 subsystems `PropulsionSubsystem`, `PowerSubsystem`, `ThermalSubsystem`, `CommunicationSubsystem`, `AttitudeSubsystem`, `LifeSupportSubsystem`,
`NavigationSubsystem`, `ComputerSubsystem`; `Spacecraft` (composes subsystems, resources, crew, limits, state), `Mission`, `OperatingLimits`,
`SpaceEnvironment`, `OrbitModel`, `DependencyGraph` (which subsystem needs which, for cascade visuals).

**crew**: `abstract CrewMember` (+ 4 subclasses), `CrewTask` (Command with duration), `CrewTaskType`, `CrewRoster`.

**detection**: `interface AnomalyDetector`, `ThresholdDetector`, `RateOfChangeDetector`, `ZScoreDetector`, `TrendPredictor`, `DetectionEngine`, `Anomaly`.

**event**: `EventBus` (Mediator + Observer; generic typed topics), `ConsoleEventListener`, `FileEventListener`.

**action**: `interface CorrectiveAction` (Command) with 20 implementations + `NoAction` (Null Object); `ActionResult`.

**advisor**: `Recommendation` (implements `Comparable`, `equals/hashCode`), `interface RecommendationRule` with 15 rules, `AdvisorEngine`.

**autonomy**: `interface AutonomyPolicy` (Strategy: `ManualPolicy`, `TimedAutopilotPolicy`, `FullAutoPolicy`), `abstract ResponseHandler`
with `SelfHealingHandler`, `AutopilotHandler`, `CrewHandler`, `EscalationHandler` (Chain of Responsibility), `AutopilotController`.

**state**: `interface SpacecraftState` (State: `NominalState`, `WarningState`, `CriticalState`, `SafeModeState`).

**comms**: `interface CommandChannel`, `DirectChannel`, `DelayedChannel` (Proxy), `QueuedCommand`.

**whatif**: `PreviewService` (forks the engine, applies the action, runs N ticks, returns `PreviewResult`).

**replay**: `History` (generic RingBuffer of snapshots, Memento-style immutable snapshots, `Iterable`), `ReplayController`.

**scoring**: `ScoreKeeper`, `interface Achievement` + 10 implementations, `Grade` enum.

**report**: `interface Visitable`, `interface ReportVisitor` (Visitor), `HtmlReportVisitor`, `TextReportVisitor`, `CsvVisitor`, `ReportWriter`.

**console**: `interface ConsoleCommand` (Interpreter), one class per command, `CommandRegistry`, `CommandParser`.

**simulation**: `SimulationEngine` (one `tick()`: environment, faults, physics, detection, prediction, advice, handler chain, crew, scoring, record, publish),
`Scenario` (+ `ScenarioFactory`), `ChaosDirector`, `DefaultSimulationController` (Facade implementing the API), `SelfTest`.

**exception**: `OrbitalIXException` (checked, base, supports cause chaining), `SensorFailureException`, `InvalidTelemetryException`,
`CommunicationLossException`, `FuelExhaustedException`, `LimitOverrideException`, `ActionNotPossibleException`, `LifeSupportException`,
`CrewException`, `UnrecoverableFaultException`, `CommandParseException`. `MissionRecorder implements AutoCloseable` (try-with-resources).

**gui** (View; imports ONLY `api`), see 6a.

**app**: `Main` (no arg = GUI; `--cli`; `--selftest`; `--screenshots <dir>`).

### 6a. GUI architecture and views
- `gui.theme`: `interface Theme` (Strategy: colours, fonts, gradients) with `DarkMissionTheme`, `NeonTheme`, `ProjectorTheme`, `HighContrastTheme`; `Themes` registry.
- `gui.kit` (our own widgets, all custom painted): `GlassCard`, `GlowButton`, `IconButton`, `ToggleSwitch`, `SegmentedControl`, `ArcGauge` (animated, glow, limit marker),
  `Sparkline`, `LineChart` (multi-series, prediction dotted line, limit lines), `ProgressRing`, `BarMeter`, `Badge`, `Tooltip`, `ToastLayer`, `Easing` (interface, Strategy: `EaseOut`, `Spring`).
- `gui.anim`: `AnimationLoop` (60 FPS Timer, `Animatable` interface), `AnimatedValue` (eased double), `Starfield` (parallax, cached), `ParticleSystem` (+ `Emitter` subclasses: exhaust, venting, sparks), `ScreenShake`, `Vignette`.
- `gui.render3d`: `Vec3`, `Matrix`, `Camera`, `abstract Mesh` with `BoxMesh`, `CylinderMesh`, `ConeMesh`, `SphereMesh`, `Part3D` (a mesh + the subsystem it represents), `Renderer` (painter's algorithm, Lambert lighting, picking), `SpacecraftModel` (builds the 8 parts).
- `gui.audio`: `interface SoundPlayer` with `SynthSoundPlayer` (javax.sound.sampled, generated tones: caution, alarm, chime, click) and `SilentSoundPlayer` (Null Object, used if no audio line).
- `gui.core`: `abstract View extends JPanel implements TickListener` (Template Method: `onSnapshot()`, `animate(dt)`, `paintView(g)`), `MainWindow`, `NavRail`, `TopBar`, `BottomTicker`, `SplashScreen`, `ViewRegistry`, `ScreenshotHarness`.
- Views: `DashboardView`, `CutawayView`, `OrbitView`, `SystemsView`, `CrewView`, `PlannerView`, `ReplayView`, `ConsoleView`, `ReportsView`, `SettingsView`.

**Shell:** left `NavRail` (drawn icons, active indicator, hover glow); `TopBar` (logo, mission clock `T+ 00:03:21`, state chip, autopilot ONLINE/OFFLINE chip, uplink chip, score, play/pause, speed,
SELF-HEAL toggle, AUTONOMY segmented control); centre cross-fading view area; `BottomTicker` (scrolling latest events, click to open an event drawer with severity filters); `ToastLayer` on the glass pane.

**DashboardView:** left column: vertical throttle slider with detents, limit sliders with design-max marks and OVERRIDE switches (turn red when unsafe); centre top: 6 `ArcGauge`s (Fuel, Battery, Oxygen, Temperature, Cabin Pressure, Radiation, each with limit marker and "empty in N ticks");
centre: live `LineChart` with series chips and a dotted prediction line; right: Mission Advisor cards (slide in/out, priority order, countdown ring, SAFE/COSTLY badge, decided-by tag, NEEDS HUMAN in red, **Preview / Execute / Dismiss**); a what-if popover under a card;
bottom-left: master caution annunciator panel (20+ tiles: FUEL TEMP PWR COMM GYRO O2 CO2 PRESS RAD NAV DEBRIS CPU ENGINE LIMITS CREW UPLINK SOLAR COOLANT HULL AUTOPILOT...), flashing MASTER CAUTION button.

**CutawayView:** the 3D spacecraft (see S3) with a side legend of the 8 parts, health colours and click-to-select; camera: drag to rotate, wheel to zoom, auto-rotate toggle. Fallback if short on time: a flat 2D drawing with the same clickable parts.

**OrbitView:** see S2. Zoom with wheel, pan with drag, legend, distance and ETA labels, comm link line green or red with delay in ticks.

**SystemsView:** left Composite tree with health dots; right detail with sensors, sparklines, active fault badges (decorators), resource rates; bottom `DependencyGraph` drawing with animated cascade pulses along failing edges.

**CrewView:** one card per crew member (drawn avatar, role icon, health/stress/dose bars, task progress ring, skill chips, buttons for the 4 tasks).

**PlannerView:** step cards (destination art, craft type with stats bars, difficulty), seed field with dice button, scenario dropdown, chaos toggle, a live "Mission Brief" estimate panel, LAUNCH with a 3-2-1 overlay.

**ReplayView:** timeline with coloured event markers, scrubber, step and play controls, "RESUME LIVE", history chart, tally bar of who acted (human / autopilot / self-healing / crew). While replaying, the TopBar shows a REPLAY badge and every view renders the recorded snapshot.

**ConsoleView:** terminal with history (Up/Down), Tab completion, colour-coded output, `help`.

**ReportsView:** report preview (HTML in a `JEditorPane`), Export HTML / Export CSV buttons, grade stamp animation, achievement gallery (earned glow, locked greyed), score breakdown.

**SettingsView:** theme picker with live preview, sound switch and volume, animation quality, effects toggles, FPS counter, UI scale; saved through `Settings`.

## 7. Relationships (must be explicit in code and in the class diagram)
- Composition: Spacecraft -> Subsystems, Resources, CrewRoster, OperatingLimits; Subsystem -> Sensors; CrewRoster -> CrewMembers
- Aggregation: DefaultSimulationController -> SimulationEngine; DetectionEngine -> AnomalyDetectors; AdvisorEngine -> RecommendationRules;
  EventBus -> listeners; ResponseHandler chain -> next handler; MainWindow -> its Views; History -> snapshots
- Association: Recommendation -> CorrectiveAction; Anomaly -> Sensor; CorrectiveAction -> Spacecraft; Fault -> Spacecraft; CrewTask -> CrewMember; Views -> SimulationController
- Dependency: Visitors -> visited classes; PreviewService -> SimulationEngine (fork)
- Inheritance: BaseSensor, Subsystem, Resource, CrewMember, Fault, Mesh, View hierarchies; exception hierarchy
- Realization: detectors, rules, policies, states, actions, handlers (via abstract class), listeners, channels, themes, visitors implement their interfaces

## 8. THE CONTRACT between the core and the GUI (package `com.orbitalix.api`)
The GUI talks ONLY to these. This is MVC and Dependency Inversion. Snapshots are immutable (records with `List.copyOf`).

```java
public interface SimulationController {
    // setup
    List<String> getScenarioNames();
    List<String> getFaultTargets();                 // the 8 subsystem names plus "Environment"
    MissionConfigOptions getConfigOptions();
    void configureMission(MissionConfig config);    // applies the config and resets
    void loadScenario(int scenarioIndex);
    // run control
    void start();  void pause();  void reset();
    void setSpeed(int ticksPerSecond);
    // operator commands (go through the uplink channel, may be delayed)
    void setThrottle(int percent);
    void setLimit(LimitType type, double value, boolean override);
    void setAutonomyMode(AutonomyMode mode);
    void executeRecommendation(String recommendationId);
    void dismissRecommendation(String recommendationId);
    void assignCrewTask(String crewName, CrewTaskType task);
    // settings and debug tools (immediate)
    void setSelfHealing(boolean enabled);
    void setUplinkDelay(boolean enabled);
    void injectFault(String target, FaultType type);
    // what-if
    PreviewResult previewRecommendation(String recommendationId, int lookaheadTicks);
    // replay
    List<SimulationSnapshot> getHistory();
    void seekTo(int tick);
    void resumeLive();
    // console and reports
    String executeCommand(String line);
    String generateReportHtml();
    String exportTelemetryCsv();
    // listeners
    void addTickListener(TickListener listener);
    void addEventListener(MissionEventListener listener);
}
public interface TickListener { void onTick(SimulationSnapshot snapshot); }
public interface MissionEventListener { void onEvent(MissionEvent event); }

public record Vec2(double x, double y) {}
public record MissionConfig(String destination, String craftType, String difficulty, long seed, boolean chaosDirector) {}
public record OptionInfo(String name, String description) {}
public record MissionConfigOptions(List<OptionInfo> destinations, List<OptionInfo> craftTypes, List<OptionInfo> difficulties) {}
public record PreviewResult(String recommendationId, int lookaheadTicks, String outcome, double fuelAfter,
        double progressAfter, int ticksToCritical, List<String> sideEffects) {}

public record SimulationSnapshot(int tick, boolean replaying, String spacecraftState, int throttle, AutonomyMode autonomyMode,
        boolean autopilotOnline, boolean selfHealing, MissionConfig config, MissionSnapshot mission,
        List<SubsystemSnapshot> subsystems, List<ResourceSnapshot> resources, List<LimitSnapshot> limits,
        List<RecommendationSnapshot> recommendations, List<ThreatSnapshot> threats, OrbitSnapshot orbit,
        List<CrewSnapshot> crew, ScoreSnapshot score, CommsSnapshot comms, List<DependencySnapshot> dependencies) {}
public record MissionSnapshot(double progressPercent, double distanceLeft, int etaTicks, boolean ended, String outcome) {}
public record SubsystemSnapshot(String name, HealthStatus status, List<SensorSnapshot> sensors, List<String> activeFaults) {}
public record SensorSnapshot(String sensorId, double value, String unit, HealthStatus status, List<Double> recent) {}
public record ResourceSnapshot(String name, double level, double capacity, String unit, int ticksToEmpty) {}
public record LimitSnapshot(LimitType type, double value, double designMax, boolean overridden) {}
public record RecommendationSnapshot(String id, String title, String explanation, int priority, int ticksLeft,
        String status, String decidedBy, ActionRisk risk, boolean needsHuman, boolean previewAvailable) {}
public record ThreatSnapshot(String name, int ticksToImpact, double intensity, Vec2 position) {}
public record OrbitSnapshot(Vec2 earth, Vec2 destination, Vec2 position, Vec2 plannedPosition, List<Vec2> plannedPath,
        List<Vec2> trail, List<Vec2> predictedPath, double courseErrorDegrees) {}
public record CrewSnapshot(String name, String role, double health, double stress, double radiationDose,
        String task, int taskTicksLeft, boolean alive) {}
public record ScoreSnapshot(int points, String grade, List<String> earnedAchievements, List<String> lockedAchievements) {}
public record CommsSnapshot(int delayTicks, double signalStrength, boolean blackout, List<UplinkSnapshot> queue) {}
public record UplinkSnapshot(String commandText, int ticksUntilExecution) {}
public record DependencySnapshot(String from, String to, HealthStatus fromStatus, boolean propagating) {}
public record MissionEvent(int tick, EventType type, Severity severity, String message) {}

public enum LimitType { MAX_THROTTLE, MAX_TEMPERATURE, MIN_BATTERY, MIN_FUEL_RESERVE }
public enum AutonomyMode { MANUAL, TIMED_AUTOPILOT, FULL_AUTO }
public enum CrewTaskType { EVA_REPAIR, MEDICAL_CARE, MANUAL_REBOOT, REST }
public enum EventType { ANOMALY, PREDICTION, RECOMMENDATION, ACTION_BY_HUMAN, ACTION_BY_AUTOPILOT, SELF_HEALING, ESCALATION, CREW,
        UPLINK, MODE_CHANGE, LIMIT_CHANGE, THREAT, CHAOS, ACHIEVEMENT, MISSION_END }
public enum ActionRisk { SAFE, COSTLY }
public enum Severity { INFO, LOW, MEDIUM, HIGH, CRITICAL }
public enum HealthStatus { NOMINAL, WARNING, CRITICAL, FAILED }
public enum FaultType { SENSOR_DRIFT, SPIKE, STUCK_VALUE, DROPOUT, FUEL_LEAK, OVERHEAT, COOLANT_FAILURE, SOLAR_FAILURE,
        SCRUBBER_FAILURE, HULL_BREACH, RADIATION_STORM, NAV_DRIFT, DEBRIS_THREAT, COMPUTER_FAULT, ENGINE_MISFIRE }
```
When `replaying` is true the snapshot comes from `History`; views render it exactly like a live one.

## 9. Design patterns and OOP depth
### 9a. Design patterns (at least these 16, each named in the Javadoc of its classes)
| Pattern | Where | Why |
|---|---|---|
| Strategy | AnomalyDetector, AutonomyPolicy, RecommendationRule, Theme, Easing, CommandChannel | swap behaviour at runtime |
| Observer | EventBus / MissionEventListener, TickListener | decouple the engine from log, console and GUI |
| State | SpacecraftState classes | behaviour changes with operating mode |
| Command | CorrectiveAction, CrewTask, QueuedCommand, ConsoleCommand | an action is an object: recommended, previewed, delayed, queued, undone, logged |
| Factory Method / Simple Factory | FaultFactory, SensorFactory, ScenarioFactory | centralise creation |
| Abstract Factory | CraftFactory (Explorer, Freighter, CrewCapsule) | build a whole consistent family of objects per craft |
| Builder | MissionConfigBuilder | many optional settings, immutable result |
| Singleton | EventLogger, Settings | one shared instance |
| Composite | SystemComponent (Spacecraft > Subsystem > Sensor), `Part3D` tree | treat parts and wholes uniformly, health roll-up |
| Decorator | SensorDecorator and its subclasses (runtime sensor faults) | add behaviour to an object without subclassing |
| Chain of Responsibility | ResponseHandler chain | self-heal, then autopilot, then crew, then human |
| Template Method | Subsystem.update(), View.paint cycle, Fault lifecycle | fixed skeleton, subclass hooks |
| Prototype | `Copyable.copy()` and `SimulationEngine.fork()` | clone the whole simulation for what-if |
| Memento | History of immutable snapshots, replay | save and restore past state |
| Visitor | ReportVisitor (Html, Text, Csv) over SystemComponent | new report formats without touching model classes |
| Interpreter | ConsoleCommand and CommandParser | parse and execute a small command language |
| Proxy | DelayedChannel wraps DirectChannel | adds uplink delay transparently |
| Facade | DefaultSimulationController | one simple API in front of a big engine |
| Mediator | EventBus | components talk through it, not to each other |
| Null Object | NoAction, SilentSoundPlayer | avoid null checks |
| MVC | gui (View), SimulationController (Controller), model + simulation (Model) | GUI never touches core classes |

### 9b. OOP depth checklist (the AI must make sure every item is present and name it in `docs/OOP_CONCEPT_MAP.md` with file names)
- Abstract classes with 3+ concrete subclasses each: BaseSensor, Subsystem, Resource, CrewMember, Fault, Mesh, View, ResponseHandler.
- 12+ interfaces; at least one with `default` and one with `static` methods (e.g. `Sensor`, `Copyable`, `Theme`).
- Polymorphism by dynamic dispatch instead of `switch` on type: faults, actions, crew, meshes, visitors. Method overloading (`FaultInjector.inject`, constructors, `log`) and overriding.
- Constructor chaining with `this(...)` and `super(...)`; `static` and instance initialiser blocks; `static` counters (id generators); `final` fields and classes; `protected` hooks.
- Encapsulation: private state, no public mutable fields, defensive copies in getters, immutable value objects (records) with validation in compact constructors.
- Enums with fields and abstract methods (`Difficulty`, `Destination`, `Grade`).
- Generics: `RingBuffer<T>`, `EventBus` topics, a bounded generic method (`<T extends Identifiable>`), wildcard use. `Comparable` and `Comparator` (Recommendation ordering), `equals`/`hashCode`/`toString` on value types.
- Inner classes: a static nested Builder, an inner `Iterator` in `RingBuffer`, an anonymous class and lambdas for listeners. `Iterable` implementation.
- Exceptions: custom checked hierarchy, `throws`, multi-catch, `finally`, cause chaining (`new ActionNotPossibleException(msg, cause)`), try-with-resources (`MissionRecorder`), exception translation at the API boundary.
- `Optional`, streams and collectors in analysis code; `instanceof` pattern matching; `switch` expressions; `var` sparingly; text blocks for the HTML report template.
- Packages with clear dependency direction (api <- gui; engine never imports gui); package-private classes where appropriate.

## 10. SOLID mapping (the report and viva must state this)
- S: Sensor measures, DetectionEngine detects, TrendPredictor predicts, AdvisorEngine advises, the handler chain acts, ScoreKeeper scores, each View shows one thing
- O: add a new feature (Fault + rule + action) or a new report format (Visitor) or a new theme without editing existing classes
- L: any Sensor, Subsystem, Resource, CrewMember, Fault, Action or Theme subclass works wherever its parent is expected
- I: small interfaces (AnomalyDetector, RecommendationRule, AutonomyPolicy, CorrectiveAction, CommandChannel, ReportVisitor, MissionEventListener, TickListener)
- D: GUI and engines depend on interfaces (`SimulationController`, `AutonomyPolicy`, `CorrectiveAction`, `CommandChannel`), never on concrete classes

## 11. Task list

### Member A (Bharat): ALL the code, about 80% of the work
Do in this order. Each task must compile and run before starting the next.

**Release R1: the engine (console only)**
- [ ] A1 (~50 min) Project setup + git and GitHub (section 0) + `api` contract (section 8) + `exception`, `util` (Copyable, DeterministicRandom, RingBuffer, EventLogger, Settings), `config` (builder, enums, craft factories)
- [ ] A2 (~60 min) `sensor` (interface, base, 9 sensors, 4 decorators), `resource`, `fault` (all 14 Fault classes, FaultFactory, FaultInjector)
- [ ] A3 (~70 min) `model` part 1: `SystemComponent`, `Subsystem` (Template Method), `Spacecraft`, `Mission`, `OperatingLimits`, `SpaceEnvironment`, `OrbitModel`, Propulsion, Power, Thermal, Communication, Attitude subsystems with the world rules of section 5
- [ ] A4 (~60 min) `model` part 2: LifeSupport, Navigation, Computer subsystems, `DependencyGraph`, and `crew` (4 CrewMember subclasses, CrewTask, CrewRoster)
- [ ] A5 (~45 min) `detection`, `event` (EventBus, listeners), and the `Copyable` implementations across model, sensors, resources, faults, crew (needed for fork)
- [ ] A6 (~80 min) `action` (20 actions + NoAction), `advisor` (15 rules), `autonomy` (3 policies, handler chain, AutopilotController), `state` (4 states)
- [ ] A7 (~90 min) `simulation` (engine tick pipeline, 11 scenarios, ChaosDirector, controller facade), `comms` (Direct and Delayed channels), `scoring` (ScoreKeeper, 10 achievements), `replay` (History), `whatif` (PreviewService), `console` (Interpreter), `report` (Visitors), `Main` CLI mode and `--selftest`. **Tag R1.**

**Release R2: the GUI core**
- [ ] A8 (~90 min) GUI foundation: 4 themes, `gui.kit` widgets, `gui.anim`, `gui.audio` (synth + silent), `View` base class, `MainWindow` shell (NavRail, TopBar, BottomTicker, ToastLayer, SplashScreen), view registry, **`--screenshots` harness**
- [ ] A9 (~90 min) `DashboardView`: gauges, chart, advisor cards (with Preview popover), limits, throttle, annunciator panel, master caution
- [ ] A10 (~120 min) `gui.render3d` + `CutawayView` (3D spacecraft with effects) and `OrbitView`. **Tag R2.**

**Release R3: the suite**
- [ ] A11 (~70 min) `PlannerView` (launch countdown), `ConsoleView`, `SettingsView` (persisted)
- [ ] A12 (~80 min) `SystemsView` (tree, sparklines, dependency graph with cascade pulses), `CrewView`
- [ ] A13 (~70 min) `ReplayView` (scrubber, markers, tally), replay mode across all views, what-if popover polish
- [ ] A14 (~70 min) `ReportsView` (preview, exports, grade stamp, achievements gallery), toasts, vignette, screen shake, tint, particles everywhere, sound wiring. **Tag R3.**

**Release R4: finish**
- [ ] A15 (~60 min) Tuning: run `--selftest`, play all 11 scenarios on 3 difficulties and 3 autonomy modes, tune constants until the section 5 tuning goal holds, fix bugs
- [ ] A16 (~60 min) Visual QA: run `--screenshots` for every view in every theme at 1280x720 and 1920x1080, inspect, fix every defect; verify keyboard shortcuts and the demo story of section 1 end to end
- [ ] A17 (~50 min) Docs and delivery: `README.md` (run instructions, features, repo URL at top), `docs/OOP_CONCEPT_MAP.md` (section 9b with file names), `docs/ARCHITECTURE.md` with PlantUML sources of the main class diagrams generated from the real code (`docs/puml/`), 12-16 screenshots in `docs/screenshots/`, a sample HTML report in `docs/`, check self-healing, escalation and self-healing OFF, push everything. **Tag R4.**
- [ ] A18 (tomorrow, ~40 min) Viva prep: AI explains every package and every pattern in plain language; A answers 25 mock viva questions

### Member B (teammate): documentation and checking, about 20% of the work
- [ ] B1 (can start NOW, needs only this file) Requirements list + use-case diagram (PlantUML) + report chapters 1-4
- [ ] B2 (after A17) Class diagrams (PlantUML), checked against `docs/puml/` and the real code (split into 4 diagrams: model+crew, advisor/action/autonomy chain, patterns showcase, gui)
- [ ] B3 (after A17) Sequence diagram (fuel leak: sensor reading -> detection -> prediction -> recommendation -> handler chain -> uplink-delayed human action) + activity diagram (one simulation tick)
- [ ] B4 (after A17) Tests: at least 25 tests covering detectors, TrendPredictor, rules, handler chain, policies, state transitions, fork equivalence, decorators, console parser, visitors, exceptions
- [ ] B5 Full report (outline in section 13), using `docs/screenshots/`, `docs/OOP_CONCEPT_MAP.md` and the sample report
- [ ] B6 PPT (outline in section 13)
- [ ] B7 Viva Q&A sheet: 40 likely questions with short answers, tied to the real code and patterns
- [ ] B8 Run the app on a second computer from a fresh clone, report any problem (README check)

## 12. Scenarios (scripted fault lists; the user can still inject any fault by hand)
1. Free Flight (sandbox): no scripted faults; problems come from the human pushing limits and, if enabled, the Chaos Director (features 1, 2, 4, 15)
2. Fuel Leak: leak at tick 20 (feature 1)
3. Engine Trouble: engine misfire at 20, overheat at 35, coolant failure at 50 (features 14, 2, 3)
4. Power Crisis: solar failure at 15 (features 5, 4)
5. Lost in Space: gyro drift at 20, navigation drift at 35 (features 7, 11)
6. Antenna Blackout: dropout at 30; commands queue up and time out (feature 6, uplink)
7. Hull Breach: breach at 25, scrubber failure at 45 (features 9, 8)
8. Solar Storm: radiation storm at 20; crew dose and EVA restrictions (feature 10)
9. Debris Alert: debris threat at 15, second one at 55 (feature 12)
10. Computer Crash: computer fault at 25, radiation storm at 30 (feature 13 plus 10): autopilot goes offline if ignored
11. Cascading Failure: leak at 15, overheat at 25, radiation storm at 35, debris at 45, computer fault at 55, breach at 65:
    only FULL_AUTO reliably survives, MANUAL almost never does
12. Endless Chaos: Chaos Director only, survive as long as possible (score by ticks survived)

## 13. Report and PPT outlines
**Report:** 1 Introduction and problem statement (use the aircraft co-pilot analogy); 2 Objectives and scope; 3 Requirements;
4 Use-case diagram; 5 System design (class diagrams, relationships); 6 Sequence and activity diagrams;
7 OO concepts used (table from `docs/OOP_CONCEPT_MAP.md`); 8 Design patterns incl. MVC (the 20-row table + short justification);
9 SOLID mapping; 10 Exception handling strategy; 11 The 15 failure features and the simulation model; 12 Autonomy levels, self-healing and the
response chain (MANUAL vs TIMED vs FULL AUTO, SAFE vs COSTLY, uplink delay, results from the scenarios); 13 The mission-control suite (S1-S15);
14 GUI design (custom widgets, 3D engine, themes, screenshots); 15 Implementation (packages, key code snippets); 16 Testing and results
(`--selftest` table, screenshots, sample mission report); 17 Limitations and future work; 18 Conclusion; References.

**PPT (about 18 slides):** title; problem (spacecraft far away, humans too slow); objectives; use case; architecture (api boundary); class diagram;
the 15 failures; the suite; patterns (2 slides); SOLID; autonomy levels and the response chain; uplink delay idea; sequence diagram;
GUI screenshots (3 slides); demo scenarios and results; testing; conclusion and future work.

## HANDOFF STATE (AI: keep this updated. The next AI reads it first.)
- Last updated by: Copilot SDK in VS Code, 2026-11-10
- Repo: No GitHub URL provided; push status NOT CONFIGURED (GitHub CLI is not authenticated)
- Release reached: none
- Currently in progress: A1-A16 requested; runnable Java foundation, simulation, CLI, GUI canvas, and screenshot matrix exist, but several detailed package/class requirements in sections 4-9 remain consolidated or simplified.
- Next step: Authenticate with `gh auth login`; then create the private `orbitalix` remote and push the local branch. Continue the missing individually modeled sensor, fault, action, detection, autonomy, audio, and interactive-view components before claiming the release gates.
- Compile command: `.\compile.bat`
- Run commands: GUI: `.\run.bat`   Console: `.\run.bat --cli`   Selftest: `.\run.bat --selftest`   Screenshots: `.\run.bat --screenshots out\screenshots`
- Decisions made: plain `javac --release 17` because Maven is unavailable; no external libraries.
- Known issues: GitHub account access is unavailable in this session. The implementation is a working, tested baseline, not a complete realization of every class and visual behavior specified in A1-A16.

## Progress log
(AI: append one line per finished task, format `YYYY-MM-DD A1 done: <what exists now>`)
- 2026-11-10 A1 foundation: local Git repository, Java 17 compile/run scripts, immutable API records, configuration builder/enums, utilities, and base exceptions.
- 2026-11-10 A7 baseline: deterministic simulation/controller, scenario matrix, console commands, history/replay snapshots, reports, and self-test are implemented in a consolidated engine.
- 2026-11-10 A14 baseline: ten themed Java2D views, control-room shell, CLI controls, settings, exports, and headless screenshot harness are implemented with simplified interactions/rendering.
- 2026-11-10 A15-A16 baseline: 108 scenario/difficulty/autonomy runs and API regression checks pass; all 80 view/theme/resolution screenshots render and pass nonblank checks.
