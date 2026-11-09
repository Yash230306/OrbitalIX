package com.orbitalix.simulation;

import com.orbitalix.api.AutonomyMode;
import com.orbitalix.api.MissionConfig;
import com.orbitalix.api.LimitType;
import com.orbitalix.config.CraftType;
import com.orbitalix.config.Destination;
import com.orbitalix.config.Difficulty;

import java.util.Locale;

/** Headless regression matrix verifies seeded determinism and representative autonomy survival. */
public final class SelfTest {
    private SelfTest() {}

    public static int run() {
        int failures = 0;
        System.out.println("Scenario             Difficulty  Autonomy          Outcome                  Ticks  Fuel");
        for (int scenario = 0; scenario < 12; scenario++) {
            for (Difficulty difficulty : Difficulty.values()) {
                for (AutonomyMode autonomy : AutonomyMode.values()) {
                    SimulationEngine first = new SimulationEngine(new MissionConfig(Destination.MARS_TRANSFER,
                            CraftType.CREW_CAPSULE, difficulty, 19770318L, scenario == 11, scenario));
                    first.setAutonomyMode(autonomy);
                    for (int i = 0; i < 300 && !first.snapshot().mission().ended(); i++) first.tick();
                    SimulationEngine second = new SimulationEngine(new MissionConfig(Destination.MARS_TRANSFER,
                            CraftType.CREW_CAPSULE, difficulty, 19770318L, scenario == 11, scenario));
                    second.setAutonomyMode(autonomy);
                    for (int i = 0; i < 300 && !second.snapshot().mission().ended(); i++) second.tick();
                    boolean deterministic = first.snapshot().mission().outcome().equals(second.snapshot().mission().outcome())
                            && Math.abs(first.fuel() - second.fuel()) < 0.000001;
                    if (!deterministic) failures++;
                    if (scenario == 10 && difficulty == Difficulty.NORMAL && autonomy == AutonomyMode.FULL_AUTO
                            && !first.snapshot().mission().outcome().startsWith("SUCCESS")) failures++;
                    System.out.printf(Locale.ROOT, "%-20s %-11s %-17s %-24s %5d %5.1f%n",
                            first.scenarioNames().get(scenario), difficulty, autonomy,
                            first.snapshot().mission().outcome(), first.tickCount(), first.fuel());
                }
            }
        }
        failures += runApiChecks();
        System.out.println(failures == 0 ? "SELFTEST PASS (determinism and survival checks)" : "SELFTEST FAIL: " + failures + " assertion(s)");
        return failures == 0 ? 0 : 1;
    }

    private static int runApiChecks() {
        int failures = 0;
        SimulationEngine engine = new SimulationEngine();
        engine.tick();
        double fuelBeforePreview = engine.fuel();
        int tickBeforePreview = engine.tickCount();
        engine.preview("reduce-throttle", 30);
        if (engine.tickCount() != tickBeforePreview || engine.fuel() != fuelBeforePreview) failures++;

        engine.setUplinkDelay(true);
        engine.setThrottle(60);
        if (engine.snapshot().comms().queue().size() != 1) failures++;
        for (int i = 0; i < engine.config().destination().uplinkDelay(); i++) engine.tick();
        if (engine.snapshot().throttle() != 60) failures++;

        SimulationEngine fork = engine.copy();
        engine.tick();
        fork.tick();
        if (Math.abs(engine.fuel() - fork.fuel()) > 0.000001) failures++;
        try {
            engine.snapshot().resources().clear();
            failures++;
        } catch (UnsupportedOperationException expected) {
            // Snapshot lists are deliberately immutable.
        }
        try {
            engine.setLimit(LimitType.MAX_THROTTLE, 120, false);
            failures++;
        } catch (IllegalArgumentException expected) {
            // Unsafe values require explicit override.
        }
        if (failures != 0) System.err.println("API checks failed: " + failures);
        return failures;
    }
}
