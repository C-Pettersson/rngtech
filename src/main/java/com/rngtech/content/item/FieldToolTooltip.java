package com.rngtech.content.item;

import com.rngtech.content.tool.ToolBaseStatCatalog;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class FieldToolTooltip {
    static void appendBaseStats(
            ItemStack stack,
            MachineStatAccumulator baseStats,
            List<Component> tooltipComponents
    ) {
        if (!TooltipKeyState.hasShiftDown()) {
            return;
        }
        if (baseStats == null) {
            return;
        }
        List<MachineStat> stats = ToolBaseStatCatalog.summaryStats(stack).stream()
                .filter(stat -> shouldShow(stat, baseStats.baseValue(stat)))
                .toList();
        if (stats.isEmpty()) {
            return;
        }

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("rngtech.tooltip.base_stats").withStyle(ChatFormatting.DARK_AQUA));
        for (MachineStat stat : stats) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.base_stat",
                    Component.translatable(stat.translationKey()),
                    MachineModifierText.formatValue(baseStats.baseValue(stat))
            ).withStyle(ChatFormatting.BLUE));
        }
    }

    private static boolean shouldShow(MachineStat stat, double value) {
        if (Math.abs(value) <= 0.0001) {
            return false;
        }
        if (stat == MachineStat.AREA_WIDTH
                || stat == MachineStat.AREA_HEIGHT
                || stat == MachineStat.TREE_FELL_LIMIT
                || stat == MachineStat.VEIN_MINE_LIMIT) {
            return value > 1.0001;
        }
        if (stat == MachineStat.ORE_BURST_SPEED || stat == MachineStat.VEIN_MINE_FE_USAGE) {
            return value > 0.0001;
        }
        return true;
    }

    private FieldToolTooltip() {
    }
}
