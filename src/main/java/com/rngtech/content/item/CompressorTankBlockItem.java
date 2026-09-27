package com.rngtech.content.item;

import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.rpg.MachineType;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class CompressorTankBlockItem extends MachineBlockItem {
    private final CompressorTankMaterial material;

    public CompressorTankBlockItem(Block block, CompressorTankMaterial material, Item.Properties properties) {
        super(block, machineType(material), properties);
        this.material = material;
    }

    public CompressorTankMaterial material() {
        return material;
    }

    @Override
    protected int componentStage() {
        return material.stage();
    }

    private static MachineType machineType(CompressorTankMaterial material) {
        return material.supportsCompression() ? MachineType.COMPRESSOR_TANK : MachineType.FLUID_TANK;
    }
}
