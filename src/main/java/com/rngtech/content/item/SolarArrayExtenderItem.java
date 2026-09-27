package com.rngtech.content.item;

import com.rngtech.content.energy.SolarArrayExtenderMaterial;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;

public class SolarArrayExtenderItem extends MachinePartItem {
    private final SolarArrayExtenderMaterial material;

    public SolarArrayExtenderItem(SolarArrayExtenderMaterial material, Properties properties) {
        super(MachinePartType.SOLAR_ARRAY_EXTENDER, MachineType.SOLAR_ARRAY_CONTROLLER, material.modifierSet(), properties);
        this.material = material;
    }

    public SolarArrayExtenderMaterial material() {
        return material;
    }

    @Override
    protected int componentStage() {
        return material.stage();
    }
}
