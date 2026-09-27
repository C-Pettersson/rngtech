package com.rngtech.content.item;

import com.rngtech.rpg.MachineChassisType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MachineChassisItem extends Item {
    private final MachineChassisType chassisType;
    private final int componentStage;

    public MachineChassisItem(MachineChassisType chassisType, Properties properties) {
        this(chassisType, -1, properties);
    }

    public MachineChassisItem(MachineChassisType chassisType, int componentStage, Properties properties) {
        super(properties);
        this.chassisType = chassisType;
        this.componentStage = componentStage;
    }

    public MachineChassisType chassisType() {
        return chassisType;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.machine_chassis.subtype",
                Component.translatable(chassisType.translationKey()).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.GRAY));
        if (componentStage >= 0) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.component_stage",
                    componentStage
            ).withStyle(ChatFormatting.GRAY));
        }
    }
}
