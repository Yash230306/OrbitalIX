package com.orbitalix.util;

import com.orbitalix.api.Copyable;

/** Copyable SplitMix64 generator gives seeded runs reproducible random sequences. */
public final class DeterministicRandom implements Copyable<DeterministicRandom> {
    private long state;

    public DeterministicRandom(long seed) { state = seed; }
    private DeterministicRandom(long state, boolean ignored) { this.state = state; }

    public long nextLong() {
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public double nextDouble() { return (nextLong() >>> 11) * 0x1.0p-53; }
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        return (int) (nextDouble() * bound);
    }
    public boolean nextBoolean() { return (nextLong() & 1) != 0; }
    @Override public DeterministicRandom copy() { return new DeterministicRandom(state, true); }
}
