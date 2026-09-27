package com.rngtech.content.item;

import com.rngtech.rpg.MachineType;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class AmmoniaMachineBlockItem extends MachineBlockItem {
    public AmmoniaMachineBlockItem(Block block, MachineType machineType, Item.Properties properties) {
        super(block, machineType, properties);
    }

    @Override
    protected int componentStage() {
        return 6;
    }
}
