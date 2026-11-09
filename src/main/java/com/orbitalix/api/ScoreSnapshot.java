package com.orbitalix.api;

import java.util.List;

public record ScoreSnapshot(int points, String grade, List<String> earnedAchievements, List<String> lockedAchievements) {
    public ScoreSnapshot {
        earnedAchievements = List.copyOf(earnedAchievements);
        lockedAchievements = List.copyOf(lockedAchievements);
    }
}
