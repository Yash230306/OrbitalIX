package com.orbitalix.api;

public record RecommendationSnapshot(String id, String title, String explanation, int priority, int ticksLeft,
        String status, String decidedBy, ActionRisk risk, boolean needsHuman, boolean previewAvailable) {}
