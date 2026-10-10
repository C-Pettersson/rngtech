package com.rngtech.rpg;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/** Tooltip lines that show how a {@link StatBreakdown} reaches its final value, one labelled source per line. */
public final class StatBreakdownText {
    private static final double EPSILON = 0.0001;

    private StatBreakdownText() {
    }

    public static List<Component> lines(StatBreakdown breakdown) {
        MachineStat stat = breakdown.stat();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "rngtech.stat.breakdown.title",
                Component.translatable(stat.translationKey()),
                amount(stat, breakdown.finalValue())
        ));
        if (stat == MachineStat.ENERGY_GENERATION) {
            lines.add(Component.translatable("rngtech.stat.breakdown.generation_multiplier").withStyle(ChatFormatting.DARK_GRAY));
        }

        double base = breakdown.base();
        if (Math.abs(base) > EPSILON) {
            lines.add(header("rngtech.stat.breakdown.base", amount(stat, base)));
        }
        section(lines, breakdown, StatBreakdown.Kind.ADD, "rngtech.stat.breakdown.added", signedAmount(stat, breakdown.added()));
        section(
                lines,
                breakdown,
                StatBreakdown.Kind.INCREASED,
                "rngtech.stat.breakdown.increased",
                MachineStatDisplay.formatSignedPercentPoints(breakdown.increasedPercent())
        );
        section(lines, breakdown, StatBreakdown.Kind.MORE, "rngtech.stat.breakdown.more", factor(breakdown.more()));

        if (hasScaling(breakdown)) {
            lines.add(Component.translatable(
                    "rngtech.stat.breakdown.formula",
                    amount(stat, base + breakdown.added()),
                    factor(Math.max(0.0, 1.0 + breakdown.increasedPercent() / 100.0)),
                    factor(breakdown.more()),
                    amount(stat, breakdown.ordinary())
            ).withStyle(ChatFormatting.GRAY));
        }
        if (1.0 + breakdown.increasedPercent() / 100.0 < 0.0) {
            lines.add(Component.translatable("rngtech.stat.breakdown.clamped").withStyle(ChatFormatting.DARK_GRAY));
        }
        override(lines, breakdown.lowest(StatBreakdown.Kind.FIXED), "rngtech.stat.breakdown.fixed", stat);
        override(lines, breakdown.lowest(StatBreakdown.Kind.CEILING), "rngtech.stat.breakdown.capped", stat);
        return lines;
    }

    private static void section(
            List<Component> lines,
            StatBreakdown breakdown,
            StatBreakdown.Kind kind,
            String headerKey,
            String total
    ) {
        List<StatBreakdown.Term> terms = breakdown.terms(kind);
        if (terms.isEmpty()) {
            return;
        }
        lines.add(header(headerKey, total));
        for (StatBreakdown.Term term : terms) {
            String value = switch (kind) {
                case ADD -> signedAmount(breakdown.stat(), term.value());
                case INCREASED -> MachineStatDisplay.formatSignedPercentPoints(term.value());
                default -> factor(term.value());
            };
            lines.add(Component.literal("  " + value + "  ").append(term.source().copy().withStyle(ChatFormatting.GRAY)));
        }
    }

    private static void override(List<Component> lines, StatBreakdown.Term term, String key, MachineStat stat) {
        if (term != null) {
            lines.add(Component.translatable(key, amount(stat, term.value()), term.source()).withStyle(ChatFormatting.GOLD));
        }
    }

    private static MutableComponent header(String key, String value) {
        return Component.translatable(key, Component.literal(value).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.GRAY);
    }

    private static boolean hasScaling(StatBreakdown breakdown) {
        return !breakdown.terms(StatBreakdown.Kind.INCREASED).isEmpty() || !breakdown.terms(StatBreakdown.Kind.MORE).isEmpty();
    }

    /** Energy Generation is a multiplier in the accumulator, so it skips the FE/t unit of its displayed row. */
    private static String amount(MachineStat stat, double value) {
        return stat == MachineStat.ENERGY_GENERATION
                ? MachineStatDisplay.formatNumber(value)
                : MachineStatDisplay.statValue(stat, value);
    }

    private static String signedAmount(MachineStat stat, double value) {
        return stat == MachineStat.ENERGY_GENERATION
                ? MachineStatDisplay.formatSignedNumber(value)
                : MachineStatDisplay.formatAdditiveEffectValue(stat, value);
    }

    private static String factor(double value) {
        return "×" + MachineStatDisplay.formatNumber(value);
    }
}
