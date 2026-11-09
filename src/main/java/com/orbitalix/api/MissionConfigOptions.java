package com.orbitalix.api;

import java.util.List;

public record MissionConfigOptions(List<OptionInfo> destinations, List<OptionInfo> craftTypes,
                                   List<OptionInfo> difficulties) {
    public MissionConfigOptions {
        destinations = List.copyOf(destinations);
        craftTypes = List.copyOf(craftTypes);
        difficulties = List.copyOf(difficulties);
    }
}
