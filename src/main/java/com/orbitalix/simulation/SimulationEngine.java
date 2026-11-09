package com.orbitalix.simulation;

import com.orbitalix.api.*;
import com.orbitalix.config.*;
import com.orbitalix.util.DeterministicRandom;
import com.orbitalix.util.RingBuffer;

import java.util.*;

/** Deterministic simulation model with fault injection, advice, autonomy, replay, and scoring. */
public final class SimulationEngine implements Copyable<SimulationEngine> {
    private static final int HISTORY_LIMIT = 600;
    private static final List<String> SCENARIOS = List.of("Free Flight", "Fuel Leak", "Engine Trouble",
            "Power Crisis", "Lost in Space", "Antenna Blackout", "Hull Breach", "Solar Storm",
            "Debris Alert", "Computer Crash", "Cascading Failure", "Endless Chaos");
    private final List<TickListener> tickListeners = new ArrayList<>();
    private final List<MissionEventListener> eventListeners = new ArrayList<>();
    private final List<MissionEvent> events = new ArrayList<>();
    private final RingBuffer<SimulationSnapshot> history = new RingBuffer<>(HISTORY_LIMIT);
    private final EnumMap<FaultType, Integer> faults = new EnumMap<>(FaultType.class);
    private final EnumMap<LimitType, Double> limits = new EnumMap<>(LimitType.class);
    private final LinkedHashMap<String, RecommendationSnapshot> recommendations = new LinkedHashMap<>();
    private final ArrayDeque<UplinkSnapshot> uplinkQueue = new ArrayDeque<>();
    private final ArrayList<Vec2> trail = new ArrayList<>();
    private final ArrayList<CrewState> crew = new ArrayList<>();
    private MissionConfig config;
    private DeterministicRandom random;
    private int tick;
    private int throttle = 50;
    private int speed = 2;
    private AutonomyMode autonomy = AutonomyMode.MANUAL;
    private boolean running;
    private boolean selfHealing = true;
    private boolean uplinkEnabled;
    private boolean replaying;
    private int replayTick;
    private double fuel;
    private double reserveFuel = 20;
    private double battery = 85;
    private double oxygen;
    private double co2 = 0.5;
    private double temperature = 24;
    private double pressure = 101;
    private double radiation;
    private double courseError;
    private double progress;
    private double signal = 1;
    private double score;
    private int sealant;
    private int computerError;
    private int computerFreeze;
    private int debrisImpact = -1;
    private int activeStormTicks;
    private boolean radiators = true;
    private boolean coolant = true;
    private boolean solarReoriented;
    private boolean backupAntenna;
    private boolean backupScrubber;
    private boolean nonEssential = true;
    private boolean shielded;
    private boolean backupEngine;
    private boolean reserveConnected;
    private boolean ended;
    private String outcome = "IN PROGRESS";
    private boolean overridden;
    private int previewCount;

    public SimulationEngine() { this(MissionConfig.defaults()); }
    public SimulationEngine(MissionConfig config) { configure(config); }

    public synchronized void configure(MissionConfig value) {
        config = Objects.requireNonNull(value);
        random = new DeterministicRandom(config.seed());
        tick = 0; throttle = 50; autonomy = AutonomyMode.MANUAL; running = false; selfHealing = true;
        replaying = false; fuel = config.craftType().fuel(); reserveFuel = 20; battery = 85;
        oxygen = config.craftType().oxygen(); co2 = 0.5; temperature = 24; pressure = 101; radiation = config.destination().radiationBase();
        courseError = 0; progress = 0; signal = 1; score = 0; sealant = config.craftType().sealant();
        computerError = 0; computerFreeze = 0; debrisImpact = -1; activeStormTicks = 0;
        radiators = true; coolant = true; solarReoriented = false; backupAntenna = false; backupScrubber = false;
        nonEssential = true; shielded = false; backupEngine = false; reserveConnected = false; ended = false;
        outcome = "IN PROGRESS"; overridden = false; previewCount = 0;
        faults.clear(); recommendations.clear(); uplinkQueue.clear(); events.clear(); history.clear(); trail.clear(); crew.clear();
        limits.clear();
        limits.put(LimitType.MAX_THROTTLE, 80d);
        limits.put(LimitType.MAX_TEMPERATURE, 85d);
        limits.put(LimitType.MIN_BATTERY, 20d);
        limits.put(LimitType.MIN_FUEL_RESERVE, 15d);
        uplinkEnabled = config.difficulty().uplinkByDefault();
        String[] names = {"Commander", "Flight Engineer", "Medical Officer", "Pilot"};
        String[] roles = {"Commander", "Engineer", "Medical", "Pilot"};
        for (int i = 0; i < config.craftType().crew(); i++) crew.add(new CrewState(names[i], roles[i]));
        emit(EventType.MODE_CHANGE, Severity.INFO, "Mission configured: " + config.destination() + " / " + config.craftType());
        publishSnapshot();
    }

    public synchronized void tick() {
        if (ended) return;
        replaying = false;
        tick++;
        triggerScenarioFaults();
        processUplink();
        advanceFaults();
        advancePhysics();
        updateDetectorsAndAdvice();
        applyAutonomy();
        advanceCrew();
        checkMissionEnd();
        updateScore();
        publishSnapshot();
    }

    private void advancePhysics() {
        double burn = (0.1 + 1.4 * Math.pow(throttle / 100d, 2)) * config.difficulty().burnMultiplier();
        if (faults.containsKey(FaultType.FUEL_LEAK)) burn += 0.45;
        if (reserveConnected) {
            if (fuel >= burn) fuel -= burn;
            else {
                reserveFuel = Math.max(0, reserveFuel - (burn - fuel));
                fuel = 0;
            }
        } else {
            fuel = Math.max(0, fuel - burn);
        }
        double solar = faults.containsKey(FaultType.SOLAR_FAILURE) ? (solarReoriented ? 0.3 : 0) : 0.6;
        battery = clamp(battery + solar - (0.2 + 0.3 * throttle / 100d + (nonEssential ? 0.2 : 0)), 0, 100);
        if (battery < limits.get(LimitType.MIN_BATTERY)) nonEssential = false;

        double cooling = radiators && coolant ? 15 : 0;
        double targetTemperature = 20 + 0.75 * throttle + (nonEssential ? 5 : 0) - cooling;
        if (faults.containsKey(FaultType.OVERHEAT)) targetTemperature += 17;
        temperature += (targetTemperature - temperature) * 0.1;
        if (faults.containsKey(FaultType.COOLANT_FAILURE)) coolant = false;

        double crewUse = crew.stream().filter(member -> member.health > 0).count() * 0.2;
        if (faults.containsKey(FaultType.HULL_BREACH)) crewUse *= 1.6;
        oxygen = Math.max(0, oxygen - crewUse);
        co2 = Math.max(0, co2 + 0.05 - (faults.containsKey(FaultType.SCRUBBER_FAILURE) && !backupScrubber ? 0 : 0.08));
        if (faults.containsKey(FaultType.HULL_BREACH)) pressure = Math.max(0, pressure - 2);
        if (activeStormTicks > 0) {
            radiation = Math.min(8, radiation + 0.8);
            activeStormTicks--;
            if (activeStormTicks == 0) faults.remove(FaultType.RADIATION_STORM);
        } else {
            radiation = Math.max(config.destination().radiationBase(), radiation - 0.15);
        }
        if (faults.containsKey(FaultType.DROPOUT) && !backupAntenna) signal = 0;
        else signal = Math.max(0.35, 1 - courseError / 100);

        if (faults.containsKey(FaultType.SENSOR_DRIFT)) courseError += 0.8;
        if (faults.containsKey(FaultType.NAV_DRIFT)) courseError += 0.7;
        courseError += 0.05 + Math.max(0, temperature - 85) * 0.003;
        if (faults.containsKey(FaultType.RADIATION_STORM) && !shielded) {
            computerError++;
            crew.forEach(member -> member.radiationDose += 0.2);
        }
        if (faults.containsKey(FaultType.COMPUTER_FAULT)) computerError++;
        if (computerError >= 15 && computerFreeze == 0) {
            computerFreeze = 5;
            emit(EventType.ANOMALY, Severity.CRITICAL, "Onboard computer crashed; autonomy is offline");
        }
        if (computerFreeze > 0) computerFreeze--;

        double efficiency = faults.containsKey(FaultType.ENGINE_MISFIRE) ? (backupEngine ? 0.85 : 0.55) : 1;
        double courseFactor = Math.max(0.3, 1 - courseError / 30);
        double actualThrottle = Math.min(throttle, limits.get(LimitType.MAX_THROTTLE));
        progress += actualThrottle * 0.02 * efficiency * courseFactor * config.craftType().progressFactor();
        trail.add(new Vec2(progress, courseError));
        if (trail.size() > 200) trail.remove(0);

        if (debrisImpact >= 0) {
            debrisImpact--;
            if (debrisImpact == 0) {
                faults.put(FaultType.HULL_BREACH, tick);
                FaultType[] affected = {FaultType.ENGINE_MISFIRE, FaultType.SOLAR_FAILURE, FaultType.COMPUTER_FAULT};
                faults.put(affected[random.nextInt(affected.length)], tick);
                emit(EventType.THREAT, Severity.CRITICAL, "Debris impact: hull breach and subsystem damage");
                faults.remove(FaultType.DEBRIS_THREAT);
                debrisImpact = -1;
            }
        }
    }

    private void triggerScenarioFaults() {
        int scenario = config.scenarioIndex();
        if (scenario == 1 && tick == 20) injectFault("Propulsion", FaultType.FUEL_LEAK);
        if (scenario == 2) {
            if (tick == 20) injectFault("Propulsion", FaultType.ENGINE_MISFIRE);
            if (tick == 35) injectFault("Thermal", FaultType.OVERHEAT);
            if (tick == 50) injectFault("Thermal", FaultType.COOLANT_FAILURE);
        }
        if (scenario == 3 && tick == 15) injectFault("Power", FaultType.SOLAR_FAILURE);
        if (scenario == 4) {
            if (tick == 20) injectFault("Attitude", FaultType.SENSOR_DRIFT);
            if (tick == 35) injectFault("Navigation", FaultType.NAV_DRIFT);
        }
        if (scenario == 5 && tick == 30) injectFault("Communication", FaultType.DROPOUT);
        if (scenario == 6) {
            if (tick == 25) injectFault("Life Support", FaultType.HULL_BREACH);
            if (tick == 45) injectFault("Life Support", FaultType.SCRUBBER_FAILURE);
        }
        if (scenario == 7 && tick == 20) injectFault("Environment", FaultType.RADIATION_STORM);
        if (scenario == 8 && (tick == 15 || tick == 55)) injectFault("Environment", FaultType.DEBRIS_THREAT);
        if (scenario == 9) {
            if (tick == 25) injectFault("Computer", FaultType.COMPUTER_FAULT);
            if (tick == 30) injectFault("Environment", FaultType.RADIATION_STORM);
        }
        if (scenario == 10) {
            if (tick == 15) injectFault("Propulsion", FaultType.FUEL_LEAK);
            if (tick == 25) injectFault("Thermal", FaultType.OVERHEAT);
            if (tick == 35) injectFault("Environment", FaultType.RADIATION_STORM);
            if (tick == 45) injectFault("Environment", FaultType.DEBRIS_THREAT);
            if (tick == 55) injectFault("Computer", FaultType.COMPUTER_FAULT);
            if (tick == 65) injectFault("Life Support", FaultType.HULL_BREACH);
        }
        if (scenario == 11 && config.chaosDirector() && tick % 40 == 0) {
            FaultType[] randomFaults = {FaultType.OVERHEAT, FaultType.FUEL_LEAK, FaultType.SOLAR_FAILURE, FaultType.NAV_DRIFT};
            injectFault("Environment", randomFaults[random.nextInt(randomFaults.length)]);
            emit(EventType.CHAOS, Severity.MEDIUM, "Chaos Director introduced a pressure-scaled fault");
        }
    }

    private void advanceFaults() {
        if (faults.containsKey(FaultType.RADIATION_STORM) && activeStormTicks == 0) activeStormTicks = 25;
        if (faults.containsKey(FaultType.DEBRIS_THREAT) && debrisImpact < 0) debrisImpact = 30;
    }

    private void updateDetectorsAndAdvice() {
        recommendations.clear();
        if (fuel <= estimatedRequiredFuel() + 8 || faults.containsKey(FaultType.FUEL_LEAK))
            recommend("reduce-throttle", "Fuel margin critical", "Reduce throttle to preserve fuel for arrival.", 100, ActionRisk.COSTLY);
        if (!reserveConnected && reserveFuel > 0 && fuel < config.craftType().fuel() * 0.4)
            recommend("cross-feed", "Main tank reserve low", "Cross-feed reserve fuel into the main propulsion supply.", 105, ActionRisk.COSTLY);
        if (temperature > limits.get(LimitType.MAX_TEMPERATURE) || faults.containsKey(FaultType.OVERHEAT))
            recommend("activate-radiators", "Thermal limit exceeded", "Activate cooling and reduce engine load.", 90, ActionRisk.SAFE);
        if (battery < limits.get(LimitType.MIN_BATTERY) + 10 || faults.containsKey(FaultType.SOLAR_FAILURE))
            recommend("shed-load", "Power reserve falling", "Shed non-essential loads or reorient the solar array.", 80, ActionRisk.SAFE);
        if (courseError > 5 || faults.containsKey(FaultType.SENSOR_DRIFT) || faults.containsKey(FaultType.NAV_DRIFT))
            recommend("recalibrate-gyro", "Attitude/navigation drift", "Recalibrate guidance before course error grows.", 80, ActionRisk.SAFE);
        if (oxygen < 25 || co2 > 4 || pressure < 80 || faults.containsKey(FaultType.HULL_BREACH))
            recommend("seal-breach", "Cabin atmosphere unsafe", "Seal the hull and protect crew life support.", 100, ActionRisk.COSTLY);
        if (radiation > 3 || activeStormTicks > 0)
            recommend("radiation-shelter", "Radiation exposure rising", "Shield crew and non-essential electronics.", 90, ActionRisk.COSTLY);
        if (debrisImpact > 0)
            recommend("evade-debris", "Debris impact in " + debrisImpact + " ticks", "Perform an evasive maneuver immediately.", 110, ActionRisk.COSTLY);
        if (computerError > 4 || computerFreeze > 0)
            recommend("reboot-computer", "Computer fault detected", "Reboot the onboard computer to restore autonomy.", 95, ActionRisk.SAFE);
        if (faults.containsKey(FaultType.DROPOUT))
            recommend("backup-antenna", "Communications blackout", "Switch to the backup antenna.", 85, ActionRisk.SAFE);
        if (faults.containsKey(FaultType.SPIKE) || faults.containsKey(FaultType.STUCK_VALUE))
            recommend("backup-sensor", "Telemetry sensor unreliable", "Switch to the redundant sensor channel.", 82, ActionRisk.SAFE);
        if (faults.containsKey(FaultType.ENGINE_MISFIRE))
            recommend("backup-engine", "Engine efficiency degraded", "Switch to the backup engine.", 75, ActionRisk.COSTLY);
        if (faults.containsKey(FaultType.SCRUBBER_FAILURE))
            recommend("backup-scrubber", "CO2 scrubber offline", "Switch to the backup scrubber.", 85, ActionRisk.COSTLY);
        if (overridden) recommend("restore-limits", "Unsafe limit override active", "Restore safe operating limits.", 70, ActionRisk.COSTLY);

        if (selfHealing && computerFreeze == 0 && computerError < 15) {
            if (battery < limits.get(LimitType.MIN_BATTERY) + 10 || faults.containsKey(FaultType.SOLAR_FAILURE)) {
                nonEssential = false;
                solarReoriented = true;
                if (recommendations.remove("shed-load") != null)
                    emit(EventType.SELF_HEALING, Severity.INFO, "SELF-HEALING: essential power preserved and arrays reoriented");
            }
            if (faults.remove(FaultType.OVERHEAT) != null) {
                radiators = true;
                coolant = true;
                emit(EventType.SELF_HEALING, Severity.INFO, "SELF-HEALING: thermal system returned to control");
                recommendations.remove("activate-radiators");
            }
            boolean gyroFault = faults.remove(FaultType.SENSOR_DRIFT) != null;
            boolean navigationFault = faults.remove(FaultType.NAV_DRIFT) != null;
            if (gyroFault || navigationFault) {
                courseError = Math.min(courseError, 1);
                emit(EventType.SELF_HEALING, Severity.INFO, "SELF-HEALING: guidance recalibrated");
                recommendations.remove("recalibrate-gyro");
            }
            if (faults.remove(FaultType.COOLANT_FAILURE) != null) {
                coolant = true; radiators = true;
                emit(EventType.SELF_HEALING, Severity.INFO, "SELF-HEALING: coolant pump restarted");
            }
            if (faults.remove(FaultType.COMPUTER_FAULT) != null) {
                computerError = 0; computerFreeze = 0;
                emit(EventType.SELF_HEALING, Severity.INFO, "SELF-HEALING: onboard computer rebooted");
            }
            if (faults.remove(FaultType.DROPOUT) != null) {
                backupAntenna = true;
                emit(EventType.SELF_HEALING, Severity.INFO, "SELF-HEALING: backup antenna engaged");
            }
            boolean spikeFault = faults.remove(FaultType.SPIKE) != null;
            boolean stuckFault = faults.remove(FaultType.STUCK_VALUE) != null;
            if (spikeFault || stuckFault) {
                emit(EventType.SELF_HEALING, Severity.INFO, "SELF-HEALING: redundant sensor channel selected");
                recommendations.remove("backup-sensor");
            }
        }
    }

    private void recommend(String id, String title, String explanation, int priority, ActionRisk risk) {
        recommendations.put(id, new RecommendationSnapshot(id, title, explanation, priority,
                Math.max(1, (int) Math.ceil(config.difficulty().responseWindow() * 10)), "OPEN",
                "HUMAN", risk, autonomy == AutonomyMode.MANUAL, true));
    }

    private void applyAutonomy() {
        if (computerFreeze > 0) return;
        List<RecommendationSnapshot> ordered = recommendations.values().stream()
                .sorted(Comparator.comparingInt(RecommendationSnapshot::priority).reversed()).toList();
        for (RecommendationSnapshot recommendation : ordered) {
            if (autonomy == AutonomyMode.FULL_AUTO && recommendation.risk() == ActionRisk.COSTLY) {
                applyAction(recommendation.id(), "AUTOPILOT");
            } else if (autonomy == AutonomyMode.TIMED_AUTOPILOT && tick % Math.max(1, recommendation.ticksLeft()) == 0) {
                applyAction(recommendation.id(), "AUTOPILOT");
            }
        }
        if (autonomy != AutonomyMode.MANUAL && faults.containsKey(FaultType.FUEL_LEAK)
                && crew.stream().noneMatch(member -> member.task == CrewTaskType.EVA_REPAIR)) {
            CrewState engineer = crew.stream().filter(member -> member.role.equals("Engineer") && member.task == null)
                    .findFirst().orElse(null);
            if (engineer != null && activeStormTicks == 0) {
                engineer.task = CrewTaskType.EVA_REPAIR;
                engineer.taskTicks = 10;
                engineer.targetFault = FaultType.FUEL_LEAK;
                emit(EventType.CREW, Severity.INFO, engineer.name + " dispatched to repair the fuel leak");
            }
        }
    }

    private void applyAction(String id, String actor) {
        boolean applied = switch (id) {
            case "reduce-throttle" -> { throttle = Math.min(throttle, 45); yield true; }
            case "cross-feed" -> { reserveConnected = true; yield true; }
            case "activate-radiators" -> { radiators = true; coolant = true; faults.remove(FaultType.OVERHEAT); faults.remove(FaultType.COOLANT_FAILURE); yield true; }
            case "shed-load" -> { nonEssential = false; solarReoriented = true; yield true; }
            case "recalibrate-gyro" -> { courseError = Math.min(courseError, 1); faults.remove(FaultType.SENSOR_DRIFT); faults.remove(FaultType.NAV_DRIFT); yield true; }
            case "seal-breach" -> { if (sealant > 0) { sealant--; pressure = Math.min(101, pressure + 18); faults.remove(FaultType.HULL_BREACH); yield true; } yield false; }
            case "radiation-shelter" -> { shielded = true; throttle = Math.min(throttle, 60); yield true; }
            case "evade-debris" -> { if (fuel >= 4) { fuel -= 4; courseError += 1; debrisImpact = -1; faults.remove(FaultType.DEBRIS_THREAT); yield true; } yield false; }
            case "reboot-computer" -> { computerError = 0; computerFreeze = 0; faults.remove(FaultType.COMPUTER_FAULT); yield true; }
            case "backup-antenna" -> { backupAntenna = true; faults.remove(FaultType.DROPOUT); yield true; }
            case "backup-sensor" -> { faults.remove(FaultType.SPIKE); faults.remove(FaultType.STUCK_VALUE); yield true; }
            case "backup-engine" -> { backupEngine = true; faults.remove(FaultType.ENGINE_MISFIRE); yield true; }
            case "backup-scrubber" -> { backupScrubber = true; faults.remove(FaultType.SCRUBBER_FAILURE); yield true; }
            case "restore-limits" -> { limits.put(LimitType.MAX_THROTTLE, 80d); overridden = false; yield true; }
            default -> false;
        };
        if (applied) {
            recommendations.remove(id);
            emit(actor.equals("HUMAN") ? EventType.ACTION_BY_HUMAN : EventType.ACTION_BY_AUTOPILOT,
                    Severity.INFO, actor + " executed " + id);
        }
    }

    private void advanceCrew() {
        for (CrewState member : crew) {
            if (member.health <= 0) continue;
            member.stress = clamp(member.stress + (temperature > 85 || pressure < 80 ? 2 : -0.5), 0, 100);
            if (oxygen < 20 || co2 > 6 || pressure < 60 || member.radiationDose > 40) member.health -= 1;
            if (member.taskTicks > 0) {
                member.taskTicks--;
                if (member.taskTicks == 0) finishCrewTask(member);
            }
        }
    }

    private void finishCrewTask(CrewState member) {
        if (member.task == CrewTaskType.MEDICAL_CARE) member.health = Math.min(100, member.health + 30);
        if (member.task == CrewTaskType.REST) member.stress = Math.max(0, member.stress - 30);
        if (member.task == CrewTaskType.EVA_REPAIR && member.targetFault != null) faults.remove(member.targetFault);
        if (member.task == CrewTaskType.MANUAL_REBOOT) { computerError = 0; computerFreeze = 0; faults.remove(FaultType.COMPUTER_FAULT); }
        emit(EventType.CREW, Severity.INFO, member.name + " completed " + member.task);
        member.task = null;
        member.targetFault = null;
    }

    private void checkMissionEnd() {
        if (fuel <= 0 && (!reserveConnected || reserveFuel <= 0)) finish("LOST - FUEL EXHAUSTED");
        else if (oxygen <= 0 || crew.stream().noneMatch(member -> member.health > 0)) finish("LOST - LIFE SUPPORT");
        else if (progress >= config.destination().distance()) finish("SUCCESS - DESTINATION REACHED");
    }

    private void finish(String result) {
        ended = true; running = false; outcome = result;
        emit(EventType.MISSION_END, result.startsWith("SUCCESS") ? Severity.INFO : Severity.CRITICAL, result);
    }

    private void updateScore() {
        double crewHealth = crew.stream().mapToDouble(member -> member.health).average().orElse(0);
        score = (ended && outcome.startsWith("SUCCESS") ? 500 : 0) + progress * 2 + fuel * 2 + battery + oxygen + crewHealth;
    }

    private double estimatedRequiredFuel() {
        double remaining = Math.max(0, config.destination().distance() - progress);
        double progressPerTick = Math.max(0.1, Math.min(throttle, limits.get(LimitType.MAX_THROTTLE)) * 0.02);
        double ticks = remaining / progressPerTick;
        return ticks * (0.1 + 1.4 * Math.pow(Math.min(throttle, 80) / 100d, 2)) * config.difficulty().burnMultiplier();
    }

    private void processUplink() {
        if (uplinkQueue.isEmpty()) return;
        List<UplinkSnapshot> pending = new ArrayList<>(uplinkQueue);
        uplinkQueue.clear();
        for (UplinkSnapshot item : pending) {
            if (item.ticksUntilExecution() <= 1) executeQueued(item.commandText());
            else uplinkQueue.addLast(new UplinkSnapshot(item.commandText(), item.ticksUntilExecution() - 1));
        }
    }

    private void executeQueued(String command) {
        String[] parts = command.split(":", -1);
        switch (parts[0]) {
            case "throttle" -> throttle = Math.min(Integer.parseInt(parts[1]), limits.get(LimitType.MAX_THROTTLE).intValue());
            case "limit" -> applyLimit(LimitType.valueOf(parts[1]), Double.parseDouble(parts[2]), Boolean.parseBoolean(parts[3]));
            case "autonomy" -> autonomy = AutonomyMode.valueOf(parts[1]);
            case "execute" -> applyAction(parts[1], "HUMAN");
            case "dismiss" -> recommendations.remove(parts[1]);
            case "crew" -> startCrewTask(parts[1], CrewTaskType.valueOf(parts[2]));
            default -> throw new IllegalArgumentException("Unknown queued uplink command: " + parts[0]);
        }
        emit(EventType.UPLINK, Severity.INFO, "Uplink command executed: " + command);
    }

    public synchronized void injectFault(String target, FaultType type) {
        Objects.requireNonNull(type);
        faults.put(type, tick);
        if (type == FaultType.RADIATION_STORM) activeStormTicks = 25;
        if (type == FaultType.DEBRIS_THREAT) debrisImpact = 30;
        emit(type == FaultType.DEBRIS_THREAT || type == FaultType.RADIATION_STORM ? EventType.THREAT : EventType.ANOMALY,
                Severity.HIGH, type + " injected at " + target);
    }

    public synchronized void executeRecommendation(String id) {
        submitOrRun("execute:" + id, () -> applyAction(id, "HUMAN"));
    }
    public synchronized void dismissRecommendation(String id) {
        submitOrRun("dismiss:" + id, () -> {
            if (recommendations.remove(id) != null) emit(EventType.ACTION_BY_HUMAN, Severity.LOW, "Dismissed recommendation " + id);
        });
    }
    public synchronized void setThrottle(int value) {
        if (value < 0 || value > 100) throw new IllegalArgumentException("Throttle must be between 0 and 100");
        if (value > limits.get(LimitType.MAX_THROTTLE)) throw new IllegalArgumentException("Throttle exceeds MAX_THROTTLE");
        submitOrRun("throttle:" + value, () -> throttle = value);
    }
    public synchronized void setLimit(LimitType type, double value, boolean override) {
        validateLimit(type, value, override);
        submitOrRun("limit:" + type.name() + ":" + value + ":" + override, () -> applyLimit(type, value, override));
    }

    private void validateLimit(LimitType type, double value, boolean override) {
        Objects.requireNonNull(type);
        if (value < 0 || !Double.isFinite(value)) throw new IllegalArgumentException("Limit must be finite and non-negative");
        if (value > 100 && !override) throw new IllegalArgumentException("Limit exceeds design value; enable override");
    }

    private void applyLimit(LimitType type, double value, boolean override) {
        if (value > 100 && !override) throw new IllegalArgumentException("Limit exceeds design value; enable override");
        if (value > 100) {
            overridden = true;
            emit(EventType.LIMIT_CHANGE, Severity.HIGH, "Limit override accepted: " + type + " = " + value);
        }
        limits.put(type, value);
        emit(EventType.LIMIT_CHANGE, Severity.INFO, "Operating limit changed: " + type + " = " + value);
    }
    public synchronized void setAutonomyMode(AutonomyMode value) {
        Objects.requireNonNull(value);
        submitOrRun("autonomy:" + value.name(), () -> {
            autonomy = value;
            emit(EventType.MODE_CHANGE, Severity.INFO, "Autonomy set to " + value);
        });
    }
    public synchronized void assignCrewTask(String name, CrewTaskType task) {
        CrewState member = crew.stream().filter(person -> person.name.equalsIgnoreCase(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown crew member: " + name));
        if (member.taskTicks > 0) throw new IllegalStateException(name + " is already assigned");
        if (task == CrewTaskType.EVA_REPAIR && activeStormTicks > 0) throw new IllegalStateException("EVA is unsafe during a radiation storm");
        submitOrRun("crew:" + member.name + ":" + task.name(), () -> startCrewTask(member.name, task));
    }

    private void startCrewTask(String name, CrewTaskType task) {
        CrewState member = crew.stream().filter(person -> person.name.equalsIgnoreCase(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown crew member: " + name));
        if (member.taskTicks > 0) throw new IllegalStateException(name + " is already assigned");
        if (task == CrewTaskType.EVA_REPAIR && activeStormTicks > 0) throw new IllegalStateException("EVA is unsafe during a radiation storm");
        member.task = Objects.requireNonNull(task);
        member.taskTicks = switch (task) { case EVA_REPAIR -> 10; case MEDICAL_CARE -> 8; case MANUAL_REBOOT -> 5; case REST -> 6; };
        emit(EventType.CREW, Severity.INFO, name + " assigned " + task);
    }
    private void submitOrRun(String command, Runnable action) {
        if (uplinkEnabled) {
            uplinkQueue.addLast(new UplinkSnapshot(command, config.destination().uplinkDelay()));
            emit(EventType.UPLINK, Severity.INFO, "Uplink queued (" + config.destination().uplinkDelay() + " ticks): " + command);
        } else action.run();
    }

    public synchronized PreviewResult preview(String id, int lookahead) {
        if (lookahead < 1 || lookahead > 500) throw new IllegalArgumentException("Lookahead must be 1..500 ticks");
        previewCount++;
        SimulationEngine forecast = copy();
        forecast.replaying = false;
        forecast.applyAction(id, "PREVIEW");
        for (int i = 0; i < lookahead && !forecast.ended; i++) forecast.tick();
        String prediction = forecast.ended ? forecast.outcome
                : String.format(Locale.ROOT, "After %d ticks: %.1f%% fuel remains; %.1f distance covered", lookahead,
                        forecast.fuel / forecast.config.craftType().fuel() * 100, forecast.progress);
        return new PreviewResult(id, lookahead, prediction, forecast.fuel, forecast.progress,
                fuel < estimatedRequiredFuel() ? Math.max(1, (int) (fuel /
                        Math.max(0.01, 0.1 + 1.4 * Math.pow(throttle / 100d, 2)))) : -1,
                List.of("Forecast simulated on an isolated copy", "Includes active faults, autonomy, and crew tasks"));
    }

    public synchronized List<SimulationSnapshot> getHistory() { return history.stream().toList(); }
    public synchronized void seekTo(int value) {
        SimulationSnapshot snapshot = history.stream().filter(item -> item.tick() == value).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No recorded tick " + value));
        replaying = true; replayTick = snapshot.tick();
        tickListeners.forEach(listener -> listener.onTick(asReplay(snapshot)));
    }
    public synchronized void resumeLive() { replaying = false; publishSnapshot(); }
    private SimulationSnapshot asReplay(SimulationSnapshot item) {
        return new SimulationSnapshot(item.tick(), true, item.spacecraftState(), item.throttle(), item.autonomyMode(),
                item.autopilotOnline(), item.selfHealing(), item.config(), item.mission(), item.subsystems(), item.resources(),
                item.limits(), item.recommendations(), item.threats(), item.orbit(), item.crew(), item.score(), item.comms(),
                item.dependencies());
    }

    public synchronized SimulationSnapshot snapshot() {
        if (replaying) {
            return history.stream().filter(item -> item.tick() == replayTick).findFirst().map(this::asReplay)
                    .orElseGet(this::liveSnapshot);
        }
        return liveSnapshot();
    }

    private SimulationSnapshot liveSnapshot() {
        double distanceLeft = Math.max(0, config.destination().distance() - progress);
        int eta = (int) Math.ceil(distanceLeft / Math.max(0.01, Math.min(throttle, 80) * 0.02));
        MissionSnapshot mission = new MissionSnapshot(progress / config.destination().distance() * 100,
                distanceLeft, eta, ended, outcome);
        List<ResourceSnapshot> resources = List.of(
                new ResourceSnapshot("Fuel", fuel, config.craftType().fuel(), "%", (int) (fuel / Math.max(0.01, 0.1 + 1.4 * Math.pow(throttle / 100d, 2)))),
                new ResourceSnapshot("Reserve Fuel", reserveFuel, 20, "%", -1),
                new ResourceSnapshot("Battery", battery, 100, "%", -1),
                new ResourceSnapshot("Oxygen", oxygen, config.craftType().oxygen(), "%", (int) (oxygen / Math.max(0.1, crew.size() * 0.2))));
        List<SubsystemSnapshot> subsystems = buildSubsystems();
        List<LimitSnapshot> limitSnapshots = Arrays.stream(LimitType.values()).map(type ->
                new LimitSnapshot(type, limits.get(type), type == LimitType.MAX_THROTTLE || type == LimitType.MAX_TEMPERATURE ? 100 : 100, overridden)).toList();
        List<Vec2> path = List.of(new Vec2(0, 0), new Vec2(config.destination().distance() * .25, 16),
                new Vec2(config.destination().distance() * .75, -16), new Vec2(config.destination().distance(), 0));
        Vec2 actual = new Vec2(progress, courseError);
        List<Vec2> predicted = List.of(actual, new Vec2(Math.min(config.destination().distance(), progress + 10), courseError));
        List<ThreatSnapshot> threats = debrisImpact > 0
                ? List.of(new ThreatSnapshot("Debris", debrisImpact, 1, new Vec2(progress + debrisImpact, 2))) : List.of();
        List<CrewSnapshot> crewSnapshots = crew.stream().map(person -> new CrewSnapshot(person.name, person.role, person.health,
                person.stress, person.radiationDose, person.task == null ? "AVAILABLE" : person.task.name(), person.taskTicks, person.health > 0)).toList();
        List<RecommendationSnapshot> recs = recommendations.values().stream()
                .sorted(Comparator.comparingInt(RecommendationSnapshot::priority).reversed()).toList();
        List<DependencySnapshot> dependencies = List.of(
                new DependencySnapshot("Power", "Computer", status(FaultType.SOLAR_FAILURE), faults.containsKey(FaultType.SOLAR_FAILURE)),
                new DependencySnapshot("Thermal", "Propulsion", status(FaultType.COOLANT_FAILURE), faults.containsKey(FaultType.COOLANT_FAILURE)),
                new DependencySnapshot("Life Support", "Crew", status(FaultType.HULL_BREACH), faults.containsKey(FaultType.HULL_BREACH)));
        double points = score;
        List<String> achievements = new ArrayList<>();
        if (ended && outcome.startsWith("SUCCESS")) achievements.add("Mission Complete");
        if (autonomy == AutonomyMode.FULL_AUTO && ended && outcome.startsWith("SUCCESS")) achievements.add("Hands-Off");
        if (previewCount >= 5) achievements.add("Whatif Wizard");
        ScoreSnapshot scoreSnapshot = new ScoreSnapshot((int) points, grade(points), achievements,
                List.of("Fuel Miser", "Guardian Angel", "Phoenix", "Dodge This"));
        return new SimulationSnapshot(tick, replaying, spacecraftStatus(), throttle, autonomy,
                computerFreeze == 0, selfHealing, config, mission, subsystems, resources, limitSnapshots, recs,
                threats, new OrbitSnapshot(new Vec2(0, 0), new Vec2(config.destination().distance(), 0), actual,
                new Vec2(progress, 0), path, trail, predicted, courseError), crewSnapshots, scoreSnapshot,
                new CommsSnapshot(uplinkEnabled ? config.destination().uplinkDelay() : 0, signal, signal < 0.1,
                        List.copyOf(uplinkQueue)), dependencies);
    }

    private List<SubsystemSnapshot> buildSubsystems() {
        Map<String, FaultType> representatives = Map.of("Propulsion", FaultType.ENGINE_MISFIRE,
                "Power", FaultType.SOLAR_FAILURE, "Thermal", FaultType.COOLANT_FAILURE,
                "Communication", FaultType.DROPOUT, "Attitude", FaultType.SENSOR_DRIFT,
                "Life Support", FaultType.HULL_BREACH, "Navigation", FaultType.NAV_DRIFT,
                "Computer", FaultType.COMPUTER_FAULT);
        List<SubsystemSnapshot> result = new ArrayList<>();
        for (Map.Entry<String, FaultType> entry : representatives.entrySet()) {
            FaultType fault = entry.getValue();
            double value = switch (entry.getKey()) {
                case "Thermal" -> temperature;
                case "Power" -> battery;
                case "Life Support" -> pressure;
                case "Communication" -> signal;
                case "Attitude", "Navigation" -> courseError;
                case "Computer" -> computerError;
                default -> fuel;
            };
            if (faults.containsKey(FaultType.STUCK_VALUE)) value = 0;
            else if (faults.containsKey(FaultType.SPIKE)) value += 12 + tick % 7;
            HealthStatus status = faults.containsKey(fault) ? HealthStatus.WARNING : HealthStatus.NOMINAL;
            if (entry.getKey().equals("Thermal") && temperature > 85 || entry.getKey().equals("Life Support") && pressure < 60)
                status = HealthStatus.CRITICAL;
            if (computerFreeze > 0 && entry.getKey().equals("Computer")) status = HealthStatus.FAILED;
            List<Double> recent = List.of(value * 0.95, value * 0.97, value);
            SensorSnapshot sensor = new SensorSnapshot(entry.getKey().toLowerCase(Locale.ROOT) + "-sensor", value,
                    entry.getKey().equals("Thermal") ? "C" : "%", status, recent);
            List<String> active = faults.keySet().stream().filter(type -> faultSubsystem(type).equals(entry.getKey()))
                    .map(Enum::name).toList();
            result.add(new SubsystemSnapshot(entry.getKey(), status, List.of(sensor), active));
        }
        return List.copyOf(result);
    }

    private String faultSubsystem(FaultType type) {
        return switch (type) {
            case FUEL_LEAK, ENGINE_MISFIRE, OVERHEAT -> type == FaultType.OVERHEAT ? "Thermal" : "Propulsion";
            case COOLANT_FAILURE -> "Thermal";
            case SOLAR_FAILURE -> "Power";
            case DROPOUT -> "Communication";
            case SENSOR_DRIFT, SPIKE, STUCK_VALUE -> "Attitude";
            case SCRUBBER_FAILURE, HULL_BREACH -> "Life Support";
            case NAV_DRIFT -> "Navigation";
            case COMPUTER_FAULT -> "Computer";
            default -> "Environment";
        };
    }

    private String spacecraftStatus() {
        if (ended) return outcome.startsWith("SUCCESS") ? "SUCCESS" : "LOST";
        if (temperature > 85 || pressure < 80 || battery < 20 || radiation > 3) return "WARNING";
        return "NOMINAL";
    }
    private HealthStatus status(FaultType type) { return faults.containsKey(type) ? HealthStatus.WARNING : HealthStatus.NOMINAL; }
    private String grade(double points) { return points >= 900 ? "S" : points >= 750 ? "A" : points >= 600 ? "B" : points >= 450 ? "C" : points >= 300 ? "D" : "F"; }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private void emit(EventType type, Severity severity, String message) {
        MissionEvent event = new MissionEvent(tick, type, severity, message);
        events.add(event);
        for (MissionEventListener listener : List.copyOf(eventListeners)) listener.onEvent(event);
    }
    private void publishSnapshot() {
        SimulationSnapshot value = liveSnapshot();
        history.add(value);
        for (TickListener listener : List.copyOf(tickListeners)) listener.onTick(value);
    }
    public synchronized void addTickListener(TickListener listener) { tickListeners.add(Objects.requireNonNull(listener)); }
    public synchronized void addEventListener(MissionEventListener listener) { eventListeners.add(Objects.requireNonNull(listener)); }
    public synchronized List<MissionEvent> events() { return List.copyOf(events); }
    public synchronized List<String> scenarioNames() { return SCENARIOS; }
    public synchronized boolean running() { return running; }
    public synchronized void start() { running = true; }
    public synchronized void pause() { running = false; }
    public synchronized void setSpeed(int value) {
        if (value < 1 || value > 20) throw new IllegalArgumentException("Speed must be 1..20");
        speed = value;
    }
    public synchronized int speed() { return speed; }
    public synchronized void reset() { configure(config); }
    public synchronized void setSelfHealing(boolean value) { selfHealing = value; }
    public synchronized void setUplinkDelay(boolean value) { uplinkEnabled = value; }
    public synchronized void setAutonomy(AutonomyMode mode) { setAutonomyMode(mode); }
    public synchronized void loadScenario(int index) {
        if (index < 0 || index >= SCENARIOS.size()) throw new IllegalArgumentException("Unknown scenario: " + index);
        config = new MissionConfig(config.destination(), config.craftType(), config.difficulty(), config.seed(), config.chaosDirector(), index);
        configure(config);
    }
    public synchronized MissionConfig config() { return config; }
    public synchronized double fuel() { return fuel; }
    public synchronized int tickCount() { return tick; }
    public synchronized String executeCommand(String line) {
        if (line == null || line.isBlank()) return "Enter a command. Type help.";
        String[] tokens = line.trim().split("\\s+");
        try {
            return switch (tokens[0].toLowerCase(Locale.ROOT)) {
                case "help" -> "Commands: help status throttle <0-100> inject <fault> exec <id> dismiss <id> limits [type value [override]] crew task <name> <task> autonomy <manual|timed|full> heal <on|off> uplink <on|off> preview <id> [ticks] scenario <0-11> seek <tick> report";
                case "status" -> "T+" + tick + " " + spacecraftStatus() + " fuel=" + fmt(fuel) + " battery=" + fmt(battery) +
                        " oxygen=" + fmt(oxygen) + " progress=" + fmt(progress) + "/" + fmt(config.destination().distance());
                case "throttle" -> { require(tokens, 2); setThrottle(Integer.parseInt(tokens[1])); yield "Throttle command accepted: " + tokens[1] + "%"; }
                case "inject" -> { require(tokens, 2); FaultType fault = parseFault(tokens[1]); injectFault("Console", fault); yield "Injected " + fault; }
                case "autonomy" -> { require(tokens, 2); setAutonomyMode(parseAutonomy(tokens[1])); yield "Autonomy set to " + autonomy; }
                case "heal" -> { require(tokens, 2); setSelfHealing(parseBoolean(tokens[1])); yield "Self-healing " + selfHealing; }
                case "uplink" -> { require(tokens, 2); setUplinkDelay(parseBoolean(tokens[1])); yield "Uplink delay " + (uplinkEnabled ? "enabled" : "disabled"); }
                case "exec" -> { require(tokens, 2); executeRecommendation(tokens[1]); yield "Executed recommendation " + tokens[1]; }
                case "dismiss" -> { require(tokens, 2); dismissRecommendation(tokens[1]); yield "Dismissed recommendation " + tokens[1]; }
                case "limits" -> {
                    if (tokens.length == 1) yield limits.entrySet().toString();
                    require(tokens, 3);
                    setLimit(LimitType.valueOf(tokens[1].toUpperCase(Locale.ROOT)), Double.parseDouble(tokens[2]),
                            tokens.length > 3 && parseBoolean(tokens[3]));
                    yield "Limit updated: " + tokens[1].toUpperCase(Locale.ROOT);
                }
                case "crew" -> crew.stream().map(member -> member.name + " (" + member.role + ", "
                        + (member.task == null ? "available" : member.task) + ")").reduce((a, b) -> a + "; " + b).orElse("No crew");
                case "task" -> {
                    require(tokens, 3);
                    CrewState member = crew.stream().filter(person -> person.name.equalsIgnoreCase(tokens[1])
                            || person.role.equalsIgnoreCase(tokens[1])).findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Unknown crew member: " + tokens[1]));
                    assignCrewTask(member.name, CrewTaskType.valueOf(tokens[2].toUpperCase(Locale.ROOT)));
                    yield member.name + " assigned " + tokens[2].toUpperCase(Locale.ROOT);
                }
                case "preview" -> {
                    require(tokens, 2);
                    PreviewResult preview = preview(tokens[1], tokens.length > 2 ? Integer.parseInt(tokens[2]) : 30);
                    yield preview.outcome();
                }
                case "scenario" -> { require(tokens, 2); loadScenario(Integer.parseInt(tokens[1])); yield "Loaded " + SCENARIOS.get(config.scenarioIndex()); }
                case "config" -> {
                    require(tokens, 3);
                    MissionConfig updated = switch (tokens[1].toLowerCase(Locale.ROOT)) {
                        case "destination" -> new MissionConfig(Destination.valueOf(tokens[2].toUpperCase(Locale.ROOT)),
                                config.craftType(), config.difficulty(), config.seed(), config.chaosDirector(), config.scenarioIndex());
                        case "craft" -> new MissionConfig(config.destination(), CraftType.valueOf(tokens[2].toUpperCase(Locale.ROOT)),
                                config.difficulty(), config.seed(), config.chaosDirector(), config.scenarioIndex());
                        case "difficulty" -> new MissionConfig(config.destination(), config.craftType(),
                                Difficulty.valueOf(tokens[2].toUpperCase(Locale.ROOT)), config.seed(), config.chaosDirector(), config.scenarioIndex());
                        default -> throw new IllegalArgumentException("Use config destination|craft|difficulty <name>");
                    };
                    configure(updated);
                    yield "Mission configured: " + updated.destination() + " / " + updated.craftType() + " / " + updated.difficulty();
                }
                case "seek" -> { require(tokens, 2); seekTo(Integer.parseInt(tokens[1])); yield "Replaying tick " + replayTick; }
                case "report" -> "Mission report: " + outcome + ", ticks=" + tick + ", fuel=" + fmt(fuel);
                case "clear" -> "";
                default -> "Unknown command: " + tokens[0] + ". Type help.";
            };
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }
    private static void require(String[] tokens, int count) {
        if (tokens.length < count) throw new IllegalArgumentException("Missing command argument");
    }
    private static boolean parseBoolean(String value) {
        if (value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("off") || value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("Expected on or off");
    }
    private static FaultType parseFault(String text) {
        String normalized = text.replace('-', '_').toUpperCase(Locale.ROOT);
        if (normalized.equals("GYRO_DRIFT")) normalized = "SENSOR_DRIFT";
        return FaultType.valueOf(normalized);
    }
    private static AutonomyMode parseAutonomy(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "manual" -> AutonomyMode.MANUAL;
            case "timed", "timed_autopilot" -> AutonomyMode.TIMED_AUTOPILOT;
            case "full", "full_auto" -> AutonomyMode.FULL_AUTO;
            default -> throw new IllegalArgumentException("Mode must be manual, timed, or full");
        };
    }
    private static String fmt(double value) { return String.format(Locale.ROOT, "%.1f", value); }

    public synchronized PreviewResult previewRecommendation(String id, int lookahead) { return preview(id, lookahead); }
    public synchronized String exportCsv() {
        StringBuilder csv = new StringBuilder("tick,fuel,battery,oxygen,temperature,pressure,progress,status\n");
        for (SimulationSnapshot item : history) {
            csv.append(item.tick()).append(',').append(value(item, "Fuel")).append(',').append(value(item, "Battery"))
                    .append(',').append(value(item, "Oxygen")).append(',').append(value(item, "Temperature")).append(',')
                    .append(value(item, "Pressure")).append(',').append(item.mission().progressPercent()).append(',')
                    .append(item.spacecraftState()).append('\n');
        }
        return csv.toString();
    }
    private static double value(SimulationSnapshot snapshot, String name) {
        return snapshot.resources().stream().filter(item -> item.name().equals(name)).mapToDouble(ResourceSnapshot::level)
                .findFirst().orElseGet(() -> snapshot.subsystems().stream().flatMap(item -> item.sensors().stream())
                        .filter(sensor -> sensor.sensorId().equalsIgnoreCase(name + "-sensor")).mapToDouble(SensorSnapshot::value).findFirst().orElse(0));
    }
    public synchronized String reportHtml() {
        SimulationSnapshot now = snapshot();
        return "<html><body style='font-family:sans-serif;background:#101722;color:#e8f1ff;padding:24px'>"
                + "<h1>OrbitalIX Mission Report</h1><h2>" + now.mission().outcome() + "</h2><p>Ticks: " + now.tick()
                + " | Progress: " + fmt(now.mission().progressPercent()) + "% | Fuel: " + fmt(fuel)
                + " | Grade: " + now.score().grade() + "</p><h3>Event timeline</h3><ul>"
                + events.stream().map(event -> "<li>T+" + event.tick() + " [" + event.severity() + "] " + event.message() + "</li>")
                .reduce("", String::concat) + "</ul></body></html>";
    }
    private static final class CrewState {
        private final String name;
        private final String role;
        private double health = 100;
        private double stress = 5;
        private double radiationDose;
        private CrewTaskType task;
        private int taskTicks;
        private FaultType targetFault;
        private CrewState(String name, String role) { this.name = name; this.role = role; }
    }

    @Override public synchronized SimulationEngine copy() {
        SimulationEngine clone = new SimulationEngine(config);
        clone.tick = tick; clone.throttle = throttle; clone.speed = speed; clone.autonomy = autonomy;
        clone.running = running; clone.selfHealing = selfHealing; clone.uplinkEnabled = uplinkEnabled;
        clone.fuel = fuel; clone.reserveFuel = reserveFuel; clone.battery = battery; clone.oxygen = oxygen;
        clone.co2 = co2; clone.temperature = temperature; clone.pressure = pressure; clone.radiation = radiation;
        clone.courseError = courseError; clone.progress = progress; clone.signal = signal; clone.score = score;
        clone.sealant = sealant; clone.computerError = computerError; clone.computerFreeze = computerFreeze;
        clone.debrisImpact = debrisImpact; clone.activeStormTicks = activeStormTicks; clone.radiators = radiators;
        clone.coolant = coolant; clone.solarReoriented = solarReoriented; clone.backupAntenna = backupAntenna;
        clone.backupScrubber = backupScrubber; clone.nonEssential = nonEssential; clone.shielded = shielded;
        clone.backupEngine = backupEngine; clone.reserveConnected = reserveConnected; clone.ended = ended;
        clone.outcome = outcome; clone.overridden = overridden; clone.previewCount = previewCount;
        clone.random = random.copy(); clone.faults.putAll(faults); clone.limits.putAll(limits);
        clone.recommendations.putAll(recommendations); clone.uplinkQueue.addAll(uplinkQueue);
        clone.trail.addAll(trail);
        clone.events.clear();
        clone.events.addAll(events);
        clone.history.clear(); for (SimulationSnapshot item : history) clone.history.add(item);
        clone.crew.clear();
        for (CrewState person : crew) {
            CrewState copy = new CrewState(person.name, person.role);
            copy.health = person.health; copy.stress = person.stress; copy.radiationDose = person.radiationDose;
            copy.task = person.task; copy.taskTicks = person.taskTicks;
            copy.targetFault = person.targetFault;
            clone.crew.add(copy);
        }
        return clone;
    }
}
