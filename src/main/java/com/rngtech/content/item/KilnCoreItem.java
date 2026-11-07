package com.rngtech.content.item;

import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierSet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The Stage 0 Furnace's only Gear: it runs the kiln on FE and lifts it to 1,000 heat, and no higher. */
public class KilnCoreItem extends MachinePartItem {
    public KilnCoreItem(Properties properties) {
        super(MachinePartType.KILN_CORE, MachineType.FURNACE, ModifierSet.empty(), properties);
    }

    @Override
    protected int componentStage() {
        return 0;
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        tooltipComponents.add(Component.translatable("rngtech.tooltip.kiln_core.profile").withStyle(ChatFormatting.GRAY));
    }
}
