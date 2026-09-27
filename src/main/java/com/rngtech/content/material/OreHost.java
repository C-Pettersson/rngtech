package com.rngtech.content.material;

public enum OreHost {
    STONE("stone", "%s_ore", "%s Ore"),
    DEEPSLATE("deepslate", "deepslate_%s_ore", "Deepslate %s Ore"),
    NETHER("nether", "nether_%s_ore", "Nether %s Ore"),
    END("end", "end_%s_ore", "End Stone %s Ore");

    private final String id;
    private final String blockPattern;
    private final String displayPattern;

    OreHost(String id, String blockPattern, String displayPattern) {
        this.id = id;
        this.blockPattern = blockPattern;
        this.displayPattern = displayPattern;
    }

    public String id() {
        return id;
    }

    public String blockId(String materialId) {
        return blockPattern.formatted(materialId);
    }

    public String displayName(String materialDisplayName) {
        return displayPattern.formatted(materialDisplayName);
    }

    public static OreHost byId(String id) {
        for (OreHost host : values()) {
            if (host.id.equals(id)) {
                return host;
            }
        }
        throw new IllegalArgumentException("Unknown ore host: " + id);
    }
}
