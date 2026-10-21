package com.rngtech.content.item;

import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.PhaseArrayRules;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** The Phase Coil item: a Stage 8 part that also carries the Glyph tier inscribed into it. */
public class PhaseCoilBlockItem extends MachineBlockItem {
    public PhaseCoilBlockItem(Block block, Properties properties) {
        super(block, MachineType.PHASE_COIL, properties);
    }

    public static int coilTier(ItemStack stack) {
        Integer tier = stack.get(ModDataComponents.COIL_TIER.get());
        return tier == null ? 0 : PhaseArrayRules.clampGlyphTier(tier);
    }

    @Override
    protected int componentStage() {
        return PhaseArrayRules.PART_STAGE;
    }

    @Override
    protected void appendMaterialTooltip(ItemStack stack, List<Component> tooltipComponents) {
        super.appendMaterialTooltip(stack, tooltipComponents);
        int tier = coilTier(stack);
        if (tier == 0) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.phase_coil.blank").withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.phase_coil.inscribed", GlyphItem.numeral(tier))
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
