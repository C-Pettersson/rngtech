package com.rngtech.rpg;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                isRate(stat) ? MachineStatDisplay.statValue(stat, breakdown.finalValue()) : amount(stat, breakdown.finalValue())
        ));
        if (stat == MachineStat.ENERGY_GENERATION) {
            lines.add(Component.translatable("rngtech.stat.breakdown.generation_multiplier").withStyle(ChatFormatting.DARK_GRAY));
        }

        double base = breakdown.base();
        if (Math.abs(base) > EPSILON) {
            lines.add(header(isRate(stat) ? "rngtech.stat.breakdown.chassis_base" : "rngtech.stat.breakdown.base", amount(stat, base)));
        }
        section(lines, breakdown, StatBreakdown.Kind.ADD, "rngtech.stat.breakdown.added", signedAmount(stat, breakdown.added()));
        StatBreakdown.Term converted = breakdown.lowest(StatBreakdown.Kind.CONVERTED);
        if (converted == null) {
            section(
                    lines,
                    breakdown,
                    StatBreakdown.Kind.INCREASED,
                    "rngtech.stat.breakdown.increased",
                    MachineStatDisplay.formatSignedPercentPoints(breakdown.increasedPercent())
            );
            softCap(lines, breakdown);
        } else if (breakdown.increasedPercent() != 0.0) {
            // The converted bucket is listed under the stat it now feeds, not here as well.
            lines.add(Component.translatable("rngtech.stat.breakdown.converted", converted.source(),
                    MachineStatDisplay.formatSignedPercentPoints(breakdown.increasedPercent())).withStyle(ChatFormatting.GOLD));
        }
        section(lines, breakdown, StatBreakdown.Kind.MORE, "rngtech.stat.breakdown.more", factor(breakdown.more()));

        if (hasScaling(breakdown)) {
            lines.add(Component.translatable(
                    "rngtech.stat.breakdown.formula",
                    amount(stat, base + breakdown.added()),
                    factor(breakdown.increasedScale()),
                    factor(breakdown.more()),
                    amount(stat, breakdown.ordinary())
            ).withStyle(ChatFormatting.GRAY));
        }
        if (breakdown.dividesReductions() && breakdown.reductionsPercent() > 0.0) {
            lines.add(Component.translatable(
                    "rngtech.stat.breakdown.divided",
                    factor(1.0 + breakdown.increasesPercent() / 100.0),
                    MachineStatDisplay.formatNumber(1.0 + breakdown.reductionsPercent() / 100.0)
            ).withStyle(ChatFormatting.GRAY));
        }
        if (!breakdown.dividesReductions() && 1.0 + breakdown.paidIncreasedPercent() / 100.0 < 0.0) {
            lines.add(Component.translatable("rngtech.stat.breakdown.clamped").withStyle(ChatFormatting.DARK_GRAY));
        }
        floor(lines, breakdown);
        override(lines, breakdown.lowest(StatBreakdown.Kind.FIXED), "rngtech.stat.breakdown.fixed", stat);
        override(lines, breakdown.lowest(StatBreakdown.Kind.CEILING), "rngtech.stat.breakdown.capped", stat);
        return lines;
    }

    /** Shows how the soft cap bends a positive increased bucket. */
    private static void softCap(List<Component> lines, StatBreakdown breakdown) {
        StatBreakdown.Term cap = breakdown.lowest(StatBreakdown.Kind.SOFT_CAP);
        double increased = breakdown.increasedPercent();
        if (cap == null || increased <= 0.0) {
            return;
        }
        String bucket = MachineStatDisplay.formatNumber(increased);
        String limit = MachineStatDisplay.formatNumber(cap.value());
        String paid = MachineStatDisplay.formatSignedPercentPoints(breakdown.paidIncreasedPercent());
        lines.add(Component.translatable("rngtech.stat.breakdown.soft_cap", bucket, limit, bucket, limit, paid)
                .withStyle(ChatFormatting.GRAY));
    }

    /** Shows the floor when it lifts the value, and how bonus-only factors scale the part above it. */
    private static void floor(List<Component> lines, StatBreakdown breakdown) {
        Double floor = breakdown.floor();
        if (floor == null) {
            return;
        }
        MachineStat stat = breakdown.stat();
        if (breakdown.ordinary() < floor - EPSILON) {
            lines.add(Component.translatable("rngtech.stat.breakdown.floor", amount(stat, floor)).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (breakdown.terms(StatBreakdown.Kind.BONUS_MORE).isEmpty()) {
            return;
        }
        section(lines, breakdown, StatBreakdown.Kind.BONUS_MORE, "rngtech.stat.breakdown.bonus_more", factor(breakdown.bonusMore()));
        lines.add(Component.translatable(
                "rngtech.stat.breakdown.bonus_formula",
                MachineStatDisplay.formatNumber(floor),
                MachineStatDisplay.formatNumber(Math.max(floor, breakdown.ordinary())),
                MachineStatDisplay.formatNumber(floor),
                MachineStatDisplay.formatNumber(breakdown.bonusMore()),
                MachineStatDisplay.formatNumber(breakdown.floored())
        ).withStyle(ChatFormatting.GRAY));
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
        // Terms from identically named sources, such as dozens of +2 Drive travel nodes, combine into one line.
        Map<String, List<StatBreakdown.Term>> bySource = new LinkedHashMap<>();
        for (StatBreakdown.Term term : terms) {
            bySource.computeIfAbsent(term.source().getString(), ignored -> new ArrayList<>()).add(term);
        }
        for (List<StatBreakdown.Term> group : bySource.values()) {
            double combined = kind == StatBreakdown.Kind.ADD || kind == StatBreakdown.Kind.INCREASED
                    ? group.stream().mapToDouble(StatBreakdown.Term::value).sum()
                    : group.stream().mapToDouble(StatBreakdown.Term::value).reduce(1.0, (a, b) -> a * b);
            String value = switch (kind) {
                case ADD -> signedAmount(breakdown.stat(), combined);
                case INCREASED -> MachineStatDisplay.formatSignedPercentPoints(combined);
                default -> factor(combined);
            };
            MutableComponent line = Component.literal("  " + value + "  ").append(group.getFirst().source().copy().withStyle(ChatFormatting.GRAY));
            if (group.size() > 1) {
                line.append(Component.literal(" ×" + group.size()).withStyle(ChatFormatting.DARK_GRAY));
            }
            lines.add(line);
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
        if (isRate(stat)) {
            return factor(value);
        }
        return stat == MachineStat.ENERGY_GENERATION
                ? MachineStatDisplay.formatNumber(value)
                : MachineStatDisplay.statValue(stat, value);
    }

    /** Stats shown as a multiplier on a listed time, like attack speed: the math multiplies a chassis base. */
    private static boolean isRate(MachineStat stat) {
        return stat == MachineStat.PROCESSING_SPEED;
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
