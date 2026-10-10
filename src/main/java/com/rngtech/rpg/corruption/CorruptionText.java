package com.rngtech.rpg.corruption;

import com.rngtech.rpg.CorruptionOutcome;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStatDisplay;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/** Shared wording for Volatile Catalyst outcome tables in screens, hover text and JEI. */
public final class CorruptionText {
    private CorruptionText() {
    }

    public static ChatFormatting color(CorruptionOutcome outcome) {
        return switch (outcome) {
            case UNTOUCHED -> ChatFormatting.GRAY;
            case BLESSED -> ChatFormatting.GREEN;
            case REFORGED -> ChatFormatting.GOLD;
            case WARPED -> ChatFormatting.LIGHT_PURPLE;
            case BLIGHTED -> ChatFormatting.RED;
        };
    }

    /** "Blessed 25%", or a removed line when a Stabilization Crystal took the outcome away. */
    public static MutableComponent chance(CorruptionPreview.Row row, boolean warded) {
        if (warded && row.outcome() == CorruptionOutcome.BLIGHTED) {
            return Component.translatable("rngtech.corruption.preview.removed", Component.translatable(row.outcome().translationKey()));
        }
        return Component.translatable(
                "rngtech.corruption.preview.chance",
                Component.translatable(row.outcome().translationKey()),
                MachineStatDisplay.formatNumber(Math.round(row.percent() * 10.0) / 10.0)
        );
    }

    /** What the outcome does, followed by each implicit it can grant with its chance within the pool. */
    public static List<Component> outcomeTooltip(CorruptionPreview.Row row, boolean warded) {
        List<Component> lines = new ArrayList<>();
        lines.add(chance(row, warded).withStyle(color(row.outcome())));
        lines.add(description(row.outcome()).withStyle(ChatFormatting.GRAY));
        if (warded && row.outcome() == CorruptionOutcome.BLIGHTED) {
            lines.add(Component.translatable("rngtech.corruption.preview.warded").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        for (CorruptionCatalog.Entry entry : row.entries()) {
            String percent = MachineStatDisplay.formatNumber(Math.round(CorruptionPreview.entryPercent(row.entries(), entry) * 10.0) / 10.0);
            lines.add(Component.translatable("rngtech.corruption.preview.entry", percent, entryText(entry))
                    .withStyle(color(row.outcome())));
        }
        return lines;
    }

    /** The whole table as hover lines, for screens without room for a panel. */
    public static List<Component> table(List<CorruptionPreview.Row> rows, boolean warded) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("rngtech.corruption.preview.title").withStyle(ChatFormatting.DARK_RED));
        for (CorruptionPreview.Row row : rows) {
            if (row.percent() <= 0.0 && !(warded && row.outcome() == CorruptionOutcome.BLIGHTED)) {
                continue;
            }
            lines.add(chance(row, warded).withStyle(color(row.outcome())));
            if (row.outcome().grantsImplicit() && !(warded && row.outcome() == CorruptionOutcome.BLIGHTED)) {
                for (CorruptionCatalog.Entry entry : row.entries()) {
                    lines.add(Component.literal("  ").append(entryText(entry)).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }
        lines.add(Component.translatable("rngtech.corruption.preview.no_cost").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    /** What an outcome does; Warped names the active warp range. */
    public static MutableComponent description(CorruptionOutcome outcome) {
        String key = outcome.translationKey() + ".description";
        if (outcome != CorruptionOutcome.WARPED) {
            return Component.translatable(key);
        }
        CorruptionCatalog.WarpRange warp = CorruptionCatalog.active().warp();
        return Component.translatable(key, signedPercent(warp.minPercent()), signedPercent(warp.maxPercent()));
    }

    private static String signedPercent(double value) {
        String number = MachineStatDisplay.formatNumber(value);
        return value > 0 ? "+" + number : number;
    }

    public static MutableComponent entryText(CorruptionCatalog.Entry entry) {
        MutableComponent text = Component.empty();
        boolean first = true;
        for (MachineModifierEffect effect : entry.effects()) {
            if (!first) {
                text.append(Component.literal(", "));
            }
            text.append(MachineStatDisplay.effectText(effect));
            first = false;
        }
        for (MachineBehavior behavior : entry.behaviors()) {
            if (!first) {
                text.append(Component.literal(", "));
            }
            text.append(Component.translatable(behavior.translationKey()));
            first = false;
        }
        return text;
    }
}
