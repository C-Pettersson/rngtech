package com.rngtech.content.material;

import java.util.List;

public record OreDefinition(
        String materialId,
        String materialDisplayName,
        int materialStage,
        int hardnessLevel,
        List<OreHost> hosts,
        List<OreHost> generationHosts,
        boolean generatedByDefault,
        int veinSize,
        int veinsPerChunk,
        int minY,
        int maxY,
        String biomes,
        double discardChanceOnAirExposure
) {
    public String blockId(OreHost host) {
        return host.blockId(materialId);
    }

    public String displayName(OreHost host) {
        return host.displayName(materialDisplayName);
    }

    public boolean hasDefaultGeneration() {
        return generatedByDefault && veinsPerChunk > 0;
    }
}
