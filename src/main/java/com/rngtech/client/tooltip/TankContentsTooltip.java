package com.rngtech.client.tooltip;

import com.rngtech.client.screen.CompactValueText;
import com.rngtech.content.item.CompressorTankBlockItem;
import com.rngtech.content.item.TankContents;
import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.content.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.ArrayList;
import java.util.List;

/** Shows the fluid a broken tank carries under its name. */
public final class TankContentsTooltip {
    private TankContentsTooltip() {
    }

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        TankContents contents = stack.get(ModDataComponents.TANK_CONTENTS.get());
        if (contents == null || contents.isEmpty() || !(stack.getItem() instanceof CompressorTankBlockItem item)) {
            return;
        }
        CompressorTankMaterial material = item.material();
        int looseCapacity = material.looseBuckets() * FluidType.BUCKET_VOLUME;
        int compressedCapacity = material.compressedPhysicalBuckets() * FluidType.BUCKET_VOLUME * material.compressionRatio();
        List<Component> lines = new ArrayList<>();
        String looseKey = material.supportsCompression() ? "loose" : "fluid";
        line(lines, looseKey, contents.loose(), looseCapacity);
        line(lines, "compressed", contents.compressed(), compressedCapacity);
        event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), lines);
    }

    private static void line(List<Component> lines, String key, SimpleFluidContent fluid, int capacity) {
        if (fluid.isEmpty()) {
            return;
        }
        lines.add(Component.translatable(
                "rngtech.tooltip.tank_contents." + key,
                fluid.copy().getHoverName(),
                CompactValueText.fluidAmountPair(fluid.getAmount(), capacity)
        ).withStyle(ChatFormatting.GRAY));
    }
}
