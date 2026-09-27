package com.rngtech.content.material;

import com.rngtech.RNGTechConfig;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MaterialEnablement {
    public static boolean isEnabled(String materialId) {
        ModConfigSpec.BooleanValue value = RNGTechConfig.MATERIALS.get(materialId);
        return value == null || value.get();
    }

    public static boolean isEnabled(MaterialItemDefinition definition) {
        return !definition.belongsToMaterialFamily() || isEnabled(definition.materialId());
    }

    private MaterialEnablement() {
    }
}
