package com.orbitalix.api;

/** Demonstrates the Prototype pattern for deterministic simulation state. */
public interface Copyable<T> {
    T copy();
}
