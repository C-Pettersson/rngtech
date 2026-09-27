package com.rngtech.content.item;

import com.rngtech.rpg.MachineType;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class BioGeneratorBlockItem extends MachineBlockItem {
    public BioGeneratorBlockItem(Block block, Item.Properties properties) {
        super(block, MachineType.BIO_GENERATOR, properties);
    }

    @Override
    protected int componentStage() {
        return 2;
    }
}
