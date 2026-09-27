package com.rngtech.compat.jei;

import com.rngtech.content.gear.GearMachineSpec;
import com.rngtech.content.gear.GearSlotCatalog;

import java.util.List;

record MachineGearJeiRecipe(GearMachineSpec spec) {
    static List<MachineGearJeiRecipe> recipes() {
        return GearSlotCatalog.all().stream().map(MachineGearJeiRecipe::new).toList();
    }
}
