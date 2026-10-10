package com.rngtech.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

final class CompactValueText {
    private static final String[] UNITS = {"", "K", "M", "G"};

    private CompactValueText() {
    }

    static String energyAmount(int amount) {
        return compact(amount) + " FE";
    }

    static String energyAmount(long amount) {
        return compact(amount) + " FE";
    }

    static String energyAmountPair(int amount, int capacity) {
        return compact(amount) + " / " + compact(capacity) + " FE";
    }

    static String energyAmountPair(long amount, long capacity) {
        return compact(amount) + " / " + compact(capacity) + " FE";
    }

    static String energyRate(int amount) {
        return compact(amount) + " FE/t";
    }

    static String energyRate(long amount) {
        return compact(amount) + " FE/t";
    }

    static String fluidAmountPair(int amount, int capacity) {
        int reference = Math.max(Math.abs(amount), Math.abs(capacity));
        if (reference >= 1_000_000_000) {
            return compactFluidValue(amount, 1_000_000_000) + " / " + compactFluidValue(capacity, 1_000_000_000) + "GB";
        }
        if (reference >= 1_000_000) {
            return compactFluidValue(amount, 1_000_000) + " / " + compactFluidValue(capacity, 1_000_000) + "MB";
        }
        if (reference >= 10_000) {
            return compactFluidValue(amount, 1_000) + " / " + compactFluidValue(capacity, 1_000) + "B";
        }
        return amount + " / " + capacity + " mB";
    }

    static Component exactEnergyAmount(int amount) {
        return Component.literal(exact(amount) + " FE");
    }

    static Component exactEnergyAmount(long amount) {
        return Component.literal(exact(amount) + " FE");
    }

    static Component exactEnergyAmountPair(int amount, int capacity) {
        return Component.literal(exact(amount) + " / " + exact(capacity) + " FE");
    }

    static Component exactEnergyAmountPair(long amount, long capacity) {
        return Component.literal(exact(amount) + " / " + exact(capacity) + " FE");
    }

    static Component exactEnergyRate(int amount) {
        return Component.literal(exact(amount) + " FE/t");
    }

    static Component exactEnergyRate(long amount) {
        return Component.literal(exact(amount) + " FE/t");
    }

    static void renderTooltipIfHovered(
            GuiGraphics guiGraphics,
            Font font,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY,
            int x,
            int y,
            String text,
            Component tooltip
    ) {
        renderTooltipIfHovered(guiGraphics, font, leftPos, topPos, mouseX, mouseY, x, y, font.width(text), font.lineHeight, tooltip);
    }

    static void renderTooltipIfHovered(
            GuiGraphics guiGraphics,
            Font font,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            Component tooltip
    ) {
        int left = leftPos + x;
        int top = topPos + y;
        if (mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height) {
            guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    static void renderTooltipIfHovered(
            GuiGraphics guiGraphics,
            Font font,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            List<Component> tooltip
    ) {
        int left = leftPos + x;
        int top = topPos + y;
        if (mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height) {
            guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private static String compact(int value) {
        return compact((long) value);
    }

    private static String compact(long value) {
        if (Math.abs(value) < 1000L) {
            return Long.toString(value);
        }

        double scaled = Math.abs((double) value);
        int unitIndex = 0;
        while (scaled >= 1000.0 && unitIndex < UNITS.length - 1) {
            scaled /= 1000.0;
            unitIndex++;
        }

        scaled = roundForDisplay(scaled);
        if (scaled >= 1000.0 && unitIndex < UNITS.length - 1) {
            scaled = roundForDisplay(scaled / 1000.0);
            unitIndex++;
        }

        String pattern = scaled >= 100.0 || scaled == Math.rint(scaled) ? "%.0f%s" : "%.1f%s";
        return (value < 0 ? "-" : "") + String.format(Locale.ROOT, pattern, scaled, UNITS[unitIndex]);
    }

    private static double roundForDisplay(double value) {
        if (value >= 100.0) {
            return Math.round(value);
        }
        return Math.round(value * 10.0) / 10.0;
    }

    private static String compactFluidValue(int amount, int scale) {
        if (amount == 0) {
            return "0";
        }
        double scaled = amount / (double) scale;
        double magnitude = Math.abs(scaled);
        if (magnitude < 0.01) {
            return amount < 0 ? ">-0.01" : "<0.01";
        }
        String pattern = magnitude >= 100.0 ? "%.0f" : magnitude >= 10.0 ? "%.1f" : "%.2f";
        return trimTrailingZeroes(String.format(Locale.ROOT, pattern, scaled));
    }

    private static String trimTrailingZeroes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && value.charAt(end - 1) == '.') {
            end--;
        }
        return value.substring(0, end);
    }

    private static String exact(int value) {
        return exact((long) value);
    }

    private static String exact(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
