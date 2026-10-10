package com.rngtech.rpg.unique;

import com.rngtech.rpg.MachineStatDisplay;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Path of Exile style stat text for Unique lines: "(60-80)% less Warmup Time" or "+(1-3) items Batch Size". */
public final class UniqueLineText {
    private UniqueLineText() {
    }

    public static MutableComponent range(UniqueStatLine line) {
        return describe(line, line.worst(), line.best());
    }

    public static MutableComponent rolled(UniqueStatLine line, double value) {
        return describe(line, value, value);
    }

    /** Just a line's range, such as "(60–80)%" or "+(1–3) items", for the Shift roll view. */
    public static String rangeAmount(UniqueStatLine line) {
        double low = Math.min(Math.abs(line.worst()), Math.abs(line.best()));
        double high = Math.max(Math.abs(line.worst()), Math.abs(line.best()));
        double signed = line.worst() != 0.0 ? line.worst() : line.best();
        return switch (line.operation()) {
            case FLAT -> (signed < 0.0 ? "-" : "+") + amount(low, high) + MachineStatDisplay.additiveUnit(line.stat(), high);
            case INCREASED, MORE -> amount(low * 100.0, high * 100.0) + "%";
        };
    }

    /** "60%" style roll quality. */
    public static String quality(double quality) {
        return Math.round(quality * 100.0) + "%";
    }

    private static MutableComponent describe(UniqueStatLine line, double first, double second) {
        double signed = first != 0.0 ? first : second;
        double low = Math.min(Math.abs(first), Math.abs(second));
        double high = Math.max(Math.abs(first), Math.abs(second));
        Component stat = Component.translatable(line.stat().translationKey());
        return switch (line.operation()) {
            case FLAT -> {
                String unit = MachineStatDisplay.additiveUnit(line.stat(), high);
                yield Component.translatable("rngtech.unique.line.flat", (signed < 0.0 ? "-" : "+") + amount(low, high) + unit, stat);
            }
            case INCREASED -> Component.translatable(
                    signed < 0.0 ? "rngtech.stat.keyword.decreased_percent" : "rngtech.stat.keyword.increased_percent",
                    amount(low * 100.0, high * 100.0),
                    stat
            );
            case MORE -> Component.translatable(
                    signed < 0.0 ? "rngtech.stat.keyword.less" : "rngtech.stat.keyword.more",
                    amount(low * 100.0, high * 100.0),
                    stat
            );
        };
    }

    private static String amount(double low, double high) {
        String lowText = MachineStatDisplay.formatNumber(round(low));
        String highText = MachineStatDisplay.formatNumber(round(high));
        return lowText.equals(highText) ? lowText : "(" + lowText + "–" + highText + ")";
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
