package com.rngtech;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RNGTechClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ANIMATED_SOLAR_PANELS = BUILDER
            .comment(
                    "Whether Solar Panel decks turn smoothly with the sun. Each visible panel then redraws its deck every frame.",
                    "When false, panels use static models that tilt 22.5 degrees east or west in steps, which is cheaper for very large fields.",
                    "Takes effect after reloading resources (F3 + T)."
            )
            .define("solar.animatedPanels", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private RNGTechClientConfig() {
    }

    public static boolean animatedSolarPanels() {
        return !SPEC.isLoaded() || ANIMATED_SOLAR_PANELS.get();
    }
}
