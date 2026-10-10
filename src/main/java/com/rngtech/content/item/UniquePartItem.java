package com.rngtech.content.item;

import com.rngtech.rpg.ModifierSet;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.unique.UniqueDefinition;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A catalog Unique installed as machine Gear. Its stats come from the Unique catalog and the rolls stored on the stack. */
public class UniquePartItem extends MachinePartItem {
    private final UniqueDefinition definition;

    public UniquePartItem(UniqueDefinition definition, Properties properties) {
        super(
                definition.host().partType(),
                definition.host().machineType(),
                new ModifierSet(Rarity.UNIQUE, 0, List.of()),
                properties
        );
        this.definition = definition;
    }

    public UniqueDefinition definition() {
        return definition;
    }

    /** Heat Core fuel tier: a Unique Heat Core burns the fuels of its slot stage. */
    public int maxFuelTier() {
        return definition.slotStage();
    }

    @Override
    protected int componentStage() {
        return definition.slotStage();
    }

    @Override
    public Component getName(ItemStack stack) {
        return UniqueTooltip.name(super.getName(stack));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        UniqueTooltip.append(stack, definition, tooltipComponents);
    }
}
