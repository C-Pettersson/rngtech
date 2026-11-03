package com.rngtech.content.item;

import com.rngtech.content.affixsorter.SorterRules;
import com.rngtech.content.blockentity.AffixSorterBlockEntity;
import com.rngtech.content.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Copies an Affix Sorter's rule list. Sneak-use on a sorter copies its rules onto the card, and so does a use with an
 * empty card; a use with a written card writes the rules to the sorter, replacing its whole list.
 */
public class RuleCardItem extends Item {
    public RuleCardItem(Properties properties) {
        super(properties);
    }

    public static SorterRules rules(ItemStack stack) {
        return stack.get(ModDataComponents.SORTER_RULES.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof AffixSorterBlockEntity sorter)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        ItemStack card = context.getItemInHand();
        SorterRules stored = rules(card);
        boolean copy = stored == null || player != null && player.isShiftKeyDown();
        if (copy) {
            card.set(ModDataComponents.SORTER_RULES.get(), sorter.sorterRules());
            message(player, Component.translatable("rngtech.rule_card.copied", sorter.sorterRules().rules().size()));
        } else {
            sorter.setSorterRules(stored);
            int disabled = stored.enabledCount() - sorter.sorterRules().enabledCount();
            message(player, disabled > 0
                    ? Component.translatable("rngtech.rule_card.written_disabled", stored.rules().size(), disabled)
                    : Component.translatable("rngtech.rule_card.written", stored.rules().size()));
        }
        return InteractionResult.CONSUME;
    }

    private static void message(Player player, Component text) {
        if (player != null) {
            player.displayClientMessage(text, true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SorterRules stored = rules(stack);
        tooltip.add(stored == null
                ? Component.translatable("rngtech.tooltip.rule_card.empty").withStyle(ChatFormatting.GRAY)
                : Component.translatable("rngtech.tooltip.rule_card.rules", stored.rules().size(), stored.enabledCount())
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("rngtech.tooltip.rule_card.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
