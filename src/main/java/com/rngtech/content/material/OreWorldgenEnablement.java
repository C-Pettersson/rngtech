package com.rngtech.content.material;

import com.rngtech.RNGTechConfig;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class OreWorldgenEnablement {
    public static boolean isEnabled(String materialId) {
        ModConfigSpec.BooleanValue value = RNGTechConfig.ORE_WORLDGEN.get(materialId);
        return RNGTechConfig.ORE_WORLDGEN_ENABLED.get()
                && MaterialEnablement.isEnabled(materialId)
                && value != null
                && value.get();
    }

    private OreWorldgenEnablement() {
    }
}
