package com.orbitalix.api;

@FunctionalInterface
public interface MissionEventListener {
    void onEvent(MissionEvent event);
}
