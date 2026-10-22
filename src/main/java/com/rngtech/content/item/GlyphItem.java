package com.rngtech.content.item;

import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.PhaseArrayRules;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A Glyph for the Phase Array: no recipe, a tier I to V, consumed by deep operations or inscribed into a Phase Coil for
 * good. A plain stack with no tier reads as Tier I, so a quest or {@code /give} Glyph works.
 */
public class GlyphItem extends Item {
    private static final String[] NUMERALS = { "I", "II", "III", "IV", "V" };

    public GlyphItem(Properties properties) {
        super(properties);
    }

    public static int tier(ItemStack stack) {
        Integer tier = stack.get(ModDataComponents.GLYPH_TIER.get());
        return tier == null ? 1 : PhaseArrayRules.clampGlyphTier(tier);
    }

    public static boolean isGlyph(ItemStack stack) {
        return stack.getItem() instanceof GlyphItem;
    }

    public static ItemStack of(Item item, int tier) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.GLYPH_TIER.get(), PhaseArrayRules.clampGlyphTier(tier));
        return stack;
    }

    public static String numeral(int tier) {
        return NUMERALS[PhaseArrayRules.clampGlyphTier(tier) - 1];
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.rngtech.glyph.tiered", numeral(tier(stack)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("rngtech.tooltip.glyph.use").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("rngtech.tooltip.glyph.inscribe").withStyle(ChatFormatting.GRAY));
    }
}
