package com.rngtech.client.screen;

import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatDisplay;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierEligibilityProfiles;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class MachineScreenStyle {
    private static final int FRAME_SHADOW = 0xFF5F5F5F;
    private static final int FRAME_DARK = 0xFF8B8B8B;
    private static final int FRAME_MID = 0xFFE0E0E0;
    private static final int FRAME_LIGHT = 0xFFFFFFFF;
    private static final int INSET = 0xFFC6C6C6;
    private static final int HEADER = 0xFFD7D7D7;
    private static final int ROW_DARK = 0xFFBDBDBD;
    private static final int ROW_LIGHT = 0xFFCFCFCF;
    private static final int ROW_ENHANCED = 0xFFE1D7B0;
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_MUTED = 0xFF5F5F5F;
    private static final int TEXT_VALUE = 0xFF1F1F1F;
    private static final int TEXT_ENHANCED = 0xFF7B5B18;
    private static final int HEADER_HEIGHT = 14;
    private static final int ROW_TOP_OFFSET = 4;
    private static final int ROW_HEIGHT = 12;
    private static final int AFFIX_ROW_TOP_OFFSET = 3;
    private static final int AFFIX_ROW_HEIGHT = 20;
    private static final int FOOTER_PADDING = 4;
    private static final int COMPACT_HEADER_HEIGHT = 12;
    private static final int COMPACT_ROW_TOP_OFFSET = 3;
    private static final int COMPACT_ROW_HEIGHT = 9;
    private static final int COMPACT_SECTION_GAP = 9;
    private static final int COMPACT_COLUMN_GAP = 4;
    private static final int COMPACT_SIDE_PADDING = 5;
    private static final int LIST_HEADER_HEIGHT = 11;
    private static final int LIST_ROW_TOP_OFFSET = 2;
    private static final int LIST_ROW_HEIGHT = 9;
    private static final int LIST_FOOTER_PADDING = 2;
    private static final double EPSILON = 0.0001;
    private static final Set<MachineStat> ZERO_VALUE_MODIFIER_STATS = EnumSet.of(
            MachineStat.SUPER_OUTPUT_CHANCE,
            MachineStat.INSTANT_PROCESS_CHANCE,
            MachineStat.REFINEMENT_POTENTIAL_BONUS,
            MachineStat.MOONLIGHT_CONVERSION,
            MachineStat.WEATHER_RECOVERY,
            MachineStat.SOLAR_PANEL_SYNCHRONIZATION,
            MachineStat.SOLAR_PANEL_ARBITRATION,
            MachineStat.OVERFLOW_SHUNTING,
            MachineStat.CLEAR_SKY_AMPLIFICATION,
            MachineStat.LUNAR_INVERSION,
            MachineStat.OUTPUT_GUARD_GRACE,
            MachineStat.NO_BATTERY_OUTPUT_RETENTION,
            MachineStat.HIGH_HARDNESS_ENERGY_MITIGATION,
            MachineStat.CRUSHER_INPUT_FILTER,
            MachineStat.CRUSHER_SALVAGE_CHANCE
    );

    private MachineScreenStyle() {
    }

    static void renderStatPanel(
            GuiGraphics guiGraphics,
            int leftPos,
            int topPos,
            int x,
            int y,
            int width,
            int rowCount,
            int accent
    ) {
        int left = leftPos + x;
        int top = topPos + y;
        int height = panelHeight(rowCount);

        guiGraphics.fill(left + 2, top + 2, left + width + 2, top + height + 2, FRAME_SHADOW);
        guiGraphics.fill(left, top, left + width, top + height, FRAME_DARK);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, FRAME_MID);
        guiGraphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, INSET);
        guiGraphics.fill(left + 3, top + 3, left + width - 3, top + HEADER_HEIGHT, HEADER);
        guiGraphics.fill(left + 4, top + HEADER_HEIGHT, left + width - 4, top + HEADER_HEIGHT + 1, accent);

        renderCorner(guiGraphics, left, top, width, height, accent);
        renderRows(guiGraphics, left, top, width, rowCount, accent);
    }

    static void renderAffixPanel(
            GuiGraphics guiGraphics,
            int leftPos,
            int topPos,
            int x,
            int y,
            int width,
            AffixLine[] affixLines,
            int accent
    ) {
        int left = leftPos + x;
        int top = topPos + y;
        int height = affixPanelHeight(affixLines.length);

        guiGraphics.fill(left + 2, top + 2, left + width + 2, top + height + 2, FRAME_SHADOW);
        guiGraphics.fill(left, top, left + width, top + height, FRAME_DARK);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, FRAME_MID);
        guiGraphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, INSET);
        guiGraphics.fill(left + 3, top + 3, left + width - 3, top + HEADER_HEIGHT, HEADER);
        guiGraphics.fill(left + 4, top + HEADER_HEIGHT, left + width - 4, top + HEADER_HEIGHT + 1, accent);

        renderCorner(guiGraphics, left, top, width, height, accent);
        renderAffixRows(guiGraphics, left, top, width, affixLines.length, accent);
    }

    static void renderCompactStatsPanel(
            GuiGraphics guiGraphics,
            int leftPos,
            int topPos,
            int x,
            int y,
            int width,
            StatLine[] statLines,
            AffixLine[] affixLines,
            int accent
    ) {
        int left = leftPos + x;
        int top = topPos + y;
        int statRows = compactRows(statLines.length);
        int affixRows = compactRows(affixLines.length);
        int height = compactPanelHeight(statRows, affixRows);
        int statRowTop = top + COMPACT_HEADER_HEIGHT + COMPACT_ROW_TOP_OFFSET;
        int affixRowTop = statRowTop + statRows * COMPACT_ROW_HEIGHT + COMPACT_SECTION_GAP;

        guiGraphics.fill(left + 2, top + 2, left + width + 2, top + height + 2, FRAME_SHADOW);
        guiGraphics.fill(left, top, left + width, top + height, FRAME_DARK);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, FRAME_MID);
        guiGraphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, INSET);
        guiGraphics.fill(left + 3, top + 3, left + width - 3, top + COMPACT_HEADER_HEIGHT, HEADER);
        guiGraphics.fill(left + 4, top + COMPACT_HEADER_HEIGHT, left + width - 4, top + COMPACT_HEADER_HEIGHT + 1, accent);

        renderCorner(guiGraphics, left, top, width, height, accent);
        renderCompactCells(guiGraphics, left, statRowTop, width, statLines.length, accent);
        renderCompactCells(guiGraphics, left, affixRowTop, width, affixLines.length, accent);
    }

    static void renderCompactAffixPanel(
            GuiGraphics guiGraphics,
            int leftPos,
            int topPos,
            int x,
            int y,
            int width,
            AffixLine[] affixLines,
            int accent
    ) {
        int left = leftPos + x;
        int top = topPos + y;
        int height = compactAffixPanelHeight(compactRows(affixLines.length));
        int rowTop = top + COMPACT_HEADER_HEIGHT + COMPACT_ROW_TOP_OFFSET;

        guiGraphics.fill(left + 2, top + 2, left + width + 2, top + height + 2, FRAME_SHADOW);
        guiGraphics.fill(left, top, left + width, top + height, FRAME_DARK);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, FRAME_MID);
        guiGraphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, INSET);
        guiGraphics.fill(left + 3, top + 3, left + width - 3, top + COMPACT_HEADER_HEIGHT, HEADER);
        guiGraphics.fill(left + 4, top + COMPACT_HEADER_HEIGHT, left + width - 4, top + COMPACT_HEADER_HEIGHT + 1, accent);

        renderCorner(guiGraphics, left, top, width, height, accent);
        renderCompactCells(guiGraphics, left, rowTop, width, affixLines.length, accent);
    }

    static void renderAffixListPanel(
            GuiGraphics guiGraphics,
            int leftPos,
            int topPos,
            int x,
            int y,
            int width,
            AffixLine[] affixLines,
            int accent
    ) {
        int left = leftPos + x;
        int top = topPos + y;
        int height = affixListPanelHeight(affixLines.length);
        int rowTop = top + LIST_HEADER_HEIGHT + LIST_ROW_TOP_OFFSET;

        guiGraphics.fill(left + 1, top + 1, left + width + 1, top + height + 1, FRAME_SHADOW);
        guiGraphics.fill(left, top, left + width, top + height, INSET);
        guiGraphics.fill(left, top, left + width, top + 1, FRAME_LIGHT);
        guiGraphics.fill(left, top, left + 1, top + height, FRAME_LIGHT);
        guiGraphics.fill(left + width - 1, top, left + width, top + height, FRAME_DARK);
        guiGraphics.fill(left, top + height - 1, left + width, top + height, FRAME_DARK);
        guiGraphics.fill(left + 2, top + 2, left + width - 2, top + LIST_HEADER_HEIGHT, HEADER);
        guiGraphics.fill(left + 3, top + LIST_HEADER_HEIGHT, left + width - 3, top + LIST_HEADER_HEIGHT + 1, accent);

        renderAffixListRows(guiGraphics, left, rowTop, width, affixLines.length, accent);
    }

    static void drawStatPanelLabels(
            GuiGraphics guiGraphics,
            Font font,
            Component title,
            StatLine[] statLines,
            int x,
            int y,
            int width,
            int accent
    ) {
        guiGraphics.drawString(font, title, x + 8, y + 4, TEXT, false);
        int rowY = y + HEADER_HEIGHT + ROW_TOP_OFFSET;
        for (StatLine statLine : statLines) {
            int valueColor = statLine.enhanced() ? TEXT_ENHANCED : TEXT_VALUE;
            if (statLine.enhanced()) {
                guiGraphics.fill(x + 6, rowY, x + width - 6, rowY + ROW_HEIGHT - 1, ROW_ENHANCED);
                guiGraphics.fill(x + 6, rowY, x + width - 5, rowY + 1, accent);
                guiGraphics.fill(x + 6, rowY + ROW_HEIGHT - 2, x + width - 5, rowY + ROW_HEIGHT - 1, accent);
            }
            drawGlyph(guiGraphics, x + 7, rowY + 2, accent, statLine.pipCount(), statLine.enhanced());

            int labelX = x + 18;
            int valueWidth = font.width(statLine.value());
            int valueX = x + width - 8 - valueWidth;
            int labelWidth = Math.max(0, valueX - labelX - 6);
            if (labelWidth > 0) {
                guiGraphics.drawString(
                        font,
                        clipped(font, statLine.label(), labelWidth),
                        labelX,
                        rowY + 2,
                        statLine.enhanced() ? TEXT : TEXT_MUTED,
                        false
                );
            }
            guiGraphics.drawString(font, statLine.value(), valueX, rowY + 2, valueColor, false);
            rowY += ROW_HEIGHT;
        }
    }

    static void drawClippedCentered(
            GuiGraphics guiGraphics,
            Font font,
            Component label,
            int centerX,
            int y,
            int width,
            int color
    ) {
        String text = font.plainSubstrByWidth(label.getString(), width);
        guiGraphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    static void renderStatPanelTooltip(
            GuiGraphics guiGraphics,
            Font font,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            StatLine[] statLines
    ) {
        int left = leftPos + x;
        int rowY = topPos + y + HEADER_HEIGHT + ROW_TOP_OFFSET;
        for (StatLine statLine : statLines) {
            if (mouseX >= left + 6
                    && mouseX < left + width - 6
                    && mouseY >= rowY
                    && mouseY < rowY + ROW_HEIGHT - 1
                    && !statLine.tooltip().getString().isEmpty()) {
                guiGraphics.renderTooltip(font, statLine.tooltip(), mouseX, mouseY);
                return;
            }
            rowY += ROW_HEIGHT;
        }
    }

    static void renderCompactStatPanelTooltip(
            GuiGraphics guiGraphics,
            Font font,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            StatLine[] statLines
    ) {
        int rowTop = topPos + y + COMPACT_HEADER_HEIGHT + COMPACT_ROW_TOP_OFFSET;
        int cellWidth = compactCellWidth(width);
        for (int index = 0; index < statLines.length; index++) {
            int row = index / 2;
            int column = index % 2;
            int cellX = leftPos + compactCellX(x, width, column);
            int cellY = rowTop + row * COMPACT_ROW_HEIGHT;
            StatLine statLine = statLines[index];
            if (mouseX >= cellX
                    && mouseX < cellX + cellWidth
                    && mouseY >= cellY
                    && mouseY < cellY + COMPACT_ROW_HEIGHT - 1
                    && !statLine.tooltip().getString().isEmpty()) {
                guiGraphics.renderTooltip(font, statLine.tooltip(), mouseX, mouseY);
                return;
            }
        }
    }

    static void drawAffixPanelLabels(
            GuiGraphics guiGraphics,
            Font font,
            Component title,
            AffixLine[] affixLines,
            int x,
            int y,
            int width,
            int accent
    ) {
        guiGraphics.drawString(font, title, x + 8, y + 4, TEXT, false);
        int rowY = y + HEADER_HEIGHT + AFFIX_ROW_TOP_OFFSET;
        for (AffixLine affixLine : affixLines) {
            if (affixLine.highlighted()) {
                guiGraphics.fill(x + 6, rowY, x + width - 6, rowY + AFFIX_ROW_HEIGHT - 1, ROW_ENHANCED);
                guiGraphics.fill(x + 6, rowY, x + width - 5, rowY + 1, accent);
            }

            drawAffixGlyph(guiGraphics, x + 8, rowY + 6, accent, affixLine.highlighted());
            drawAffixTitle(guiGraphics, font, affixLine, x + 18, rowY + 1, width - 26);
            guiGraphics.drawString(
                    font,
                    clipped(font, affixLine.effect(), width - 26),
                    x + 18,
                    rowY + 10,
                    affixLine.highlighted() ? TEXT_VALUE : TEXT_MUTED,
                    false
            );
            rowY += AFFIX_ROW_HEIGHT;
        }
    }

    static void drawCompactStatsPanelLabels(
            GuiGraphics guiGraphics,
            Font font,
            Component title,
            Component affixTitle,
            StatLine[] statLines,
            AffixLine[] affixLines,
            int x,
            int y,
            int width,
            int accent
    ) {
        int statRows = compactRows(statLines.length);
        int statRowTop = y + COMPACT_HEADER_HEIGHT + COMPACT_ROW_TOP_OFFSET;
        int affixTitleY = statRowTop + statRows * COMPACT_ROW_HEIGHT;
        int affixRowTop = affixTitleY + COMPACT_SECTION_GAP;

        guiGraphics.drawString(font, title, x + 7, y + 3, TEXT, false);
        drawCompactStatLines(guiGraphics, font, statLines, x, statRowTop, width, accent);
        guiGraphics.drawString(font, affixTitle, x + 7, affixTitleY + 1, TEXT_MUTED, false);
        drawCompactAffixLines(guiGraphics, font, affixLines, x, affixRowTop, width, accent);
    }

    static void drawCompactAffixPanelLabels(
            GuiGraphics guiGraphics,
            Font font,
            Component title,
            AffixLine[] affixLines,
            int x,
            int y,
            int width,
            int accent
    ) {
        int rowTop = y + COMPACT_HEADER_HEIGHT + COMPACT_ROW_TOP_OFFSET;

        guiGraphics.drawString(font, title, x + 7, y + 3, TEXT, false);
        drawCompactAffixLines(guiGraphics, font, affixLines, x, rowTop, width, accent);
    }

    static void drawAffixListPanelLabels(
            GuiGraphics guiGraphics,
            Font font,
            Component title,
            AffixLine[] affixLines,
            int x,
            int y,
            int width,
            int accent
    ) {
        int rowTop = y + LIST_HEADER_HEIGHT + LIST_ROW_TOP_OFFSET;

        guiGraphics.drawString(font, clipped(font, title, width - 8), x + 4, y + 2, TEXT, false);
        drawAffixListLines(guiGraphics, font, affixLines, x, rowTop, width, accent);
    }

    static void renderAffixListPanelTooltip(
            GuiGraphics guiGraphics,
            Font font,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            AffixLine[] affixLines
    ) {
        int left = leftPos + x;
        int rowY = topPos + y + LIST_HEADER_HEIGHT + LIST_ROW_TOP_OFFSET;
        for (AffixLine affixLine : affixLines) {
            if (mouseX >= left + 3
                    && mouseX < left + width - 3
                    && mouseY >= rowY
                    && mouseY < rowY + LIST_ROW_HEIGHT - 1
                    && !affixLine.tooltip().isEmpty()) {
                guiGraphics.renderComponentTooltip(font, affixLine.tooltip(), mouseX, mouseY);
                return;
            }
            rowY += LIST_ROW_HEIGHT;
        }
    }

    static AffixLine[] machineAffixLines(MachineTraits traits) {
        List<AffixLine> affixLines = new ArrayList<>();
        addMachineAffixes(affixLines, traits);
        addEmptyAffixLineIfNeeded(affixLines, traits);
        return affixLines.toArray(AffixLine[]::new);
    }

    static AffixLine[] fitAffixLines(AffixLine[] affixLines, int maxLines) {
        if (affixLines.length <= maxLines) {
            return affixLines;
        }

        AffixLine[] visible = new AffixLine[maxLines];
        System.arraycopy(affixLines, 0, visible, 0, maxLines - 1);
        visible[maxLines - 1] = new AffixLine(
                Component.empty(),
                Component.translatable("rngtech.configuration.more_affixes", affixLines.length - maxLines + 1),
                Component.empty(),
                TEXT_MUTED,
                false,
                overflowTooltip(affixLines, maxLines - 1)
        );
        return visible;
    }

    static StatLine statLine(Component label, String value, double strength, boolean integral) {
        return new StatLine(label, value, strength, integral, false, Component.empty());
    }

    static StatLine statLine(Component label, String value, double strength, boolean integral, boolean enhanced) {
        return new StatLine(label, value, strength, integral, enhanced, Component.empty());
    }

    static String statValue(MachineStat stat, double value) {
        return MachineStatDisplay.statValue(stat, value);
    }

    static StatLine statLine(
            Component label,
            String value,
            double strength,
            boolean integral,
            boolean enhanced,
            Component tooltip
    ) {
        return new StatLine(label, value, strength, integral, enhanced, tooltip);
    }

    /** Declared ascendancy stats follow the machine's own rows, highlighted, only while an allocated node grants them. */
    static StatLine[] withAscendancyStats(StatLine[] statLines, List<MasteryMenuSupport.GrantedStat> granted) {
        StatLine[] all = Arrays.copyOf(statLines, statLines.length + granted.size());
        for (int index = 0; index < granted.size(); index++) {
            MasteryMenuSupport.GrantedStat stat = granted.get(index);
            all[statLines.length + index] = statLine(
                    Component.translatable(stat.stat().translationKey()),
                    statValue(stat.stat(), stat.value()),
                    1.0,
                    true,
                    true,
                    MachineStatDisplay.statTooltip(stat.stat(), stat.value())
            );
        }
        return all;
    }

    /** Rows a stat panel at {@code panelY} can show inside a screen {@code imageHeight} tall. */
    static int maxStatRows(int panelY, int imageHeight) {
        return Math.max(1, (imageHeight - panelY - HEADER_HEIGHT - ROW_TOP_OFFSET - FOOTER_PADDING - 4) / ROW_HEIGHT);
    }

    /** Keeps a stat panel on screen: rows past the limit collapse into a last row whose tooltip lists them. */
    static StatLine[] fitStatLines(StatLine[] statLines, int maxRows) {
        if (statLines.length <= maxRows) {
            return statLines;
        }
        StatLine[] visible = Arrays.copyOf(statLines, maxRows);
        MutableComponent hidden = Component.empty();
        for (int index = maxRows - 1; index < statLines.length; index++) {
            if (index > maxRows - 1) {
                hidden.append(", ");
            }
            hidden.append(statLines[index].label()).append(" " + statLines[index].value());
        }
        visible[maxRows - 1] = new StatLine(
                Component.translatable("rngtech.configuration.more_stats", statLines.length - maxRows + 1),
                "",
                0.0,
                true,
                false,
                hidden
        );
        return visible;
    }

    static StatLine[] withoutInactiveModifierStats(MachineStat[] stats, StatLine[] statLines) {
        if (stats.length != statLines.length) {
            return statLines;
        }

        List<StatLine> visible = new ArrayList<>();
        for (int index = 0; index < statLines.length; index++) {
            if (!isInactiveModifierStat(stats[index], statLines[index].strength())) {
                visible.add(statLines[index]);
            }
        }
        return visible.toArray(StatLine[]::new);
    }

    static Component statLayerTooltip(ItemStack displayStack, MachineStat stat, double finalValue) {
        return MachineStatDisplay.statTooltip(stat, finalValue);
    }

    static void renderStatusSquare(GuiGraphics guiGraphics, int leftPos, int topPos, int x, int y) {
        int left = leftPos + x;
        int top = topPos + y;
        guiGraphics.fill(left, top, left + 12, top + 12, 0xFF373737);
        guiGraphics.fill(left + 1, top + 1, left + 11, top + 11, FRAME_SHADOW);
        guiGraphics.fill(left + 2, top + 2, left + 10, top + 10, 0xFF2F2F2F);
    }

    static void renderProcessingLevelSquare(
            GuiGraphics guiGraphics,
            int leftPos,
            int topPos,
            int x,
            int y,
            int processingLevel,
            int maximumLevel,
            int accent
    ) {
        renderStatusSquare(guiGraphics, leftPos, topPos, x, y);
        int filledPips = statusSquarePips(processingLevel, maximumLevel);
        int left = leftPos + x;
        int top = topPos + y;
        for (int pip = 0; pip < 4; pip++) {
            int color = pip < filledPips ? accent : FRAME_DARK;
            int rowTop = top + 8 - pip * 2;
            guiGraphics.fill(left + 3, rowTop, left + 9, rowTop + 1, color);
        }
    }

    static void renderStatusSquareTooltip(
            GuiGraphics guiGraphics,
            Font font,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY,
            int x,
            int y,
            Component tooltip
    ) {
        CompactValueText.renderTooltipIfHovered(
                guiGraphics,
                font,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                x,
                y,
                12,
                12,
                tooltip
        );
    }

    private static boolean isInactiveModifierStat(MachineStat stat, double value) {
        return ZERO_VALUE_MODIFIER_STATS.contains(stat) && isZero(value);
    }

    private static int statusSquarePips(int value, int maximum) {
        if (value <= 0 || maximum <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(4, (int) Math.ceil(Math.min(value, maximum) * 4.0D / maximum)));
    }

    private static boolean isZero(double value) {
        return Math.abs(value) <= EPSILON;
    }

    private static int panelHeight(int rowCount) {
        return HEADER_HEIGHT + ROW_TOP_OFFSET + rowCount * ROW_HEIGHT + FOOTER_PADDING;
    }

    private static int affixPanelHeight(int rowCount) {
        return HEADER_HEIGHT + AFFIX_ROW_TOP_OFFSET + rowCount * AFFIX_ROW_HEIGHT + FOOTER_PADDING;
    }

    private static int compactPanelHeight(int statRows, int affixRows) {
        return COMPACT_HEADER_HEIGHT
                + COMPACT_ROW_TOP_OFFSET
                + statRows * COMPACT_ROW_HEIGHT
                + COMPACT_SECTION_GAP
                + affixRows * COMPACT_ROW_HEIGHT
                + FOOTER_PADDING;
    }

    private static int compactAffixPanelHeight(int affixRows) {
        return COMPACT_HEADER_HEIGHT
                + COMPACT_ROW_TOP_OFFSET
                + affixRows * COMPACT_ROW_HEIGHT
                + FOOTER_PADDING;
    }

    private static int affixListPanelHeight(int rowCount) {
        return LIST_HEADER_HEIGHT
                + LIST_ROW_TOP_OFFSET
                + rowCount * LIST_ROW_HEIGHT
                + LIST_FOOTER_PADDING;
    }

    private static int compactRows(int lineCount) {
        return (lineCount + 1) / 2;
    }

    private static void renderCorner(GuiGraphics guiGraphics, int left, int top, int width, int height, int accent) {
        guiGraphics.fill(left, top, left + width, top + 1, FRAME_LIGHT);
        guiGraphics.fill(left, top, left + 1, top + height, FRAME_LIGHT);
        guiGraphics.fill(left + width - 1, top, left + width, top + height, FRAME_DARK);
        guiGraphics.fill(left, top + height - 1, left + width, top + height, FRAME_DARK);
    }

    private static void renderRows(GuiGraphics guiGraphics, int left, int top, int width, int rowCount, int accent) {
        int rowY = top + HEADER_HEIGHT + ROW_TOP_OFFSET;
        for (int index = 0; index < rowCount; index++) {
            int rowColor = index % 2 == 0 ? ROW_DARK : ROW_LIGHT;
            guiGraphics.fill(left + 6, rowY, left + width - 6, rowY + ROW_HEIGHT - 1, rowColor);
            guiGraphics.fill(left + 6, rowY, left + 7, rowY + ROW_HEIGHT - 1, accent);
            guiGraphics.fill(left + width - 7, rowY, left + width - 6, rowY + ROW_HEIGHT - 1, FRAME_DARK);
            rowY += ROW_HEIGHT;
        }
    }

    private static void renderAffixRows(GuiGraphics guiGraphics, int left, int top, int width, int rowCount, int accent) {
        int rowY = top + HEADER_HEIGHT + AFFIX_ROW_TOP_OFFSET;
        for (int index = 0; index < rowCount; index++) {
            int rowColor = index % 2 == 0 ? ROW_DARK : ROW_LIGHT;
            guiGraphics.fill(left + 6, rowY, left + width - 6, rowY + AFFIX_ROW_HEIGHT - 1, rowColor);
            guiGraphics.fill(left + 6, rowY, left + 7, rowY + AFFIX_ROW_HEIGHT - 1, accent);
            guiGraphics.fill(left + width - 7, rowY, left + width - 6, rowY + AFFIX_ROW_HEIGHT - 1, FRAME_DARK);
            rowY += AFFIX_ROW_HEIGHT;
        }
    }

    private static void renderCompactCells(
            GuiGraphics guiGraphics,
            int left,
            int rowTop,
            int width,
            int lineCount,
            int accent
    ) {
        int cellWidth = compactCellWidth(width);
        for (int index = 0; index < lineCount; index++) {
            int row = index / 2;
            int column = index % 2;
            int x = compactCellX(left, width, column);
            int y = rowTop + row * COMPACT_ROW_HEIGHT;
            int rowColor = row % 2 == 0 ? ROW_DARK : ROW_LIGHT;
            guiGraphics.fill(x, y, x + cellWidth, y + COMPACT_ROW_HEIGHT - 1, rowColor);
            guiGraphics.fill(x, y, x + 1, y + COMPACT_ROW_HEIGHT - 1, accent);
            guiGraphics.fill(x + cellWidth - 1, y, x + cellWidth, y + COMPACT_ROW_HEIGHT - 1, FRAME_DARK);
        }
    }

    private static void renderAffixListRows(
            GuiGraphics guiGraphics,
            int left,
            int rowTop,
            int width,
            int lineCount,
            int accent
    ) {
        for (int index = 0; index < lineCount; index++) {
            int y = rowTop + index * LIST_ROW_HEIGHT;
            int rowColor = index % 2 == 0 ? ROW_DARK : ROW_LIGHT;
            guiGraphics.fill(left + 3, y, left + width - 3, y + LIST_ROW_HEIGHT - 1, rowColor);
            guiGraphics.fill(left + 3, y, left + 4, y + LIST_ROW_HEIGHT - 1, accent);
            guiGraphics.fill(left + width - 4, y, left + width - 3, y + LIST_ROW_HEIGHT - 1, FRAME_DARK);
        }
    }

    private static void drawCompactStatLines(
            GuiGraphics guiGraphics,
            Font font,
            StatLine[] statLines,
            int x,
            int rowTop,
            int width,
            int accent
    ) {
        int cellWidth = compactCellWidth(width);
        for (int index = 0; index < statLines.length; index++) {
            int row = index / 2;
            int column = index % 2;
            int cellX = compactCellX(x, width, column);
            int cellY = rowTop + row * COMPACT_ROW_HEIGHT;
            drawCompactStatLine(guiGraphics, font, statLines[index], cellX, cellY, cellWidth, accent);
        }
    }

    private static void drawCompactAffixLines(
            GuiGraphics guiGraphics,
            Font font,
            AffixLine[] affixLines,
            int x,
            int rowTop,
            int width,
            int accent
    ) {
        int cellWidth = compactCellWidth(width);
        for (int index = 0; index < affixLines.length; index++) {
            int row = index / 2;
            int column = index % 2;
            int cellX = compactCellX(x, width, column);
            int cellY = rowTop + row * COMPACT_ROW_HEIGHT;
            drawCompactAffixLine(guiGraphics, font, affixLines[index], cellX, cellY, cellWidth, accent);
        }
    }

    private static void drawAffixListLines(
            GuiGraphics guiGraphics,
            Font font,
            AffixLine[] affixLines,
            int x,
            int rowTop,
            int width,
            int accent
    ) {
        for (int index = 0; index < affixLines.length; index++) {
            int rowY = rowTop + index * LIST_ROW_HEIGHT;
            drawAffixListLine(guiGraphics, font, affixLines[index], x + 3, rowY, width - 6, accent);
        }
    }

    private static void drawCompactStatLine(
            GuiGraphics guiGraphics,
            Font font,
            StatLine statLine,
            int x,
            int y,
            int width,
            int accent
    ) {
        int valueColor = statLine.enhanced() ? TEXT_ENHANCED : TEXT_VALUE;
        if (statLine.enhanced()) {
            guiGraphics.fill(x, y, x + width, y + COMPACT_ROW_HEIGHT - 1, ROW_ENHANCED);
            guiGraphics.fill(x, y, x + width, y + 1, accent);
        }

        int labelX = x + 4;
        int valueWidth = font.width(statLine.value());
        int valueX = x + width - 4 - valueWidth;
        int labelWidth = Math.max(0, valueX - labelX - 3);
        if (labelWidth > 0) {
            guiGraphics.drawString(
                    font,
                    clipped(font, statLine.label(), labelWidth),
                    labelX,
                    y + 1,
                    statLine.enhanced() ? TEXT : TEXT_MUTED,
                    false
            );
        }
        guiGraphics.drawString(font, statLine.value(), valueX, y + 1, valueColor, false);
    }

    private static void drawCompactAffixLine(
            GuiGraphics guiGraphics,
            Font font,
            AffixLine affixLine,
            int x,
            int y,
            int width,
            int accent
    ) {
        if (affixLine.highlighted()) {
            guiGraphics.fill(x, y, x + width, y + COMPACT_ROW_HEIGHT - 1, ROW_ENHANCED);
            guiGraphics.fill(x, y, x + width, y + 1, accent);
        }

        int nameX = x + 4;
        int effectWidth = Math.min(font.width(affixLine.effect()), Math.max(24, width / 2));
        Component effect = clipped(font, affixLine.effect(), effectWidth);
        int effectX = x + width - 4 - font.width(effect);
        int nameWidth = Math.max(0, effectX - nameX - 3);
        if (nameWidth > 0) {
            guiGraphics.drawString(font, clipped(font, affixLine.name(), nameWidth), nameX, y + 1, affixLine.nameColor(), false);
        }
        guiGraphics.drawString(font, effect, effectX, y + 1, affixLine.highlighted() ? TEXT_VALUE : TEXT_MUTED, false);
    }

    private static void drawAffixListLine(
            GuiGraphics guiGraphics,
            Font font,
            AffixLine affixLine,
            int x,
            int y,
            int width,
            int accent
    ) {
        if (affixLine.highlighted()) {
            guiGraphics.fill(x, y, x + width, y + LIST_ROW_HEIGHT - 1, ROW_ENHANCED);
            guiGraphics.fill(x, y, x + width, y + 1, accent);
        }

        drawAffixGlyph(guiGraphics, x + 4, y + 1, accent, affixLine.highlighted());
        int nameX = x + 15;
        guiGraphics.drawString(
                font,
                clipped(font, affixLine.name(), width - 19),
                nameX,
                y + 1,
                affixLine.nameColor(),
                false
        );
    }

    private static int compactCellWidth(int width) {
        return (width - COMPACT_SIDE_PADDING * 2 - COMPACT_COLUMN_GAP) / 2;
    }

    private static int compactCellX(int left, int width, int column) {
        return left + COMPACT_SIDE_PADDING + column * (compactCellWidth(width) + COMPACT_COLUMN_GAP);
    }

    private static void drawGlyph(GuiGraphics guiGraphics, int x, int y, int accent, int pips, boolean enhanced) {
        guiGraphics.fill(x, y + 1, x + 7, y + 8, FRAME_DARK);
        guiGraphics.fill(x + 1, y + 2, x + 6, y + 7, enhanced ? TEXT_ENHANCED : accent);
        for (int index = 0; index < pips; index++) {
            guiGraphics.fill(x + 1 + index, y, x + 2 + index, y + 1, enhanced ? TEXT_ENHANCED : accent);
        }
    }

    private static void drawAffixGlyph(GuiGraphics guiGraphics, int x, int y, int accent, boolean highlighted) {
        guiGraphics.fill(x + 2, y, x + 5, y + 1, highlighted ? TEXT_ENHANCED : accent);
        guiGraphics.fill(x + 1, y + 1, x + 6, y + 2, 0xFF14110E);
        guiGraphics.fill(x, y + 2, x + 7, y + 5, highlighted ? TEXT_ENHANCED : accent);
        guiGraphics.fill(x + 1, y + 5, x + 6, y + 6, 0xFF14110E);
        guiGraphics.fill(x + 2, y + 6, x + 5, y + 7, highlighted ? TEXT_ENHANCED : accent);
    }

    private static void drawAffixTitle(GuiGraphics guiGraphics, Font font, AffixLine affixLine, int x, int y, int width) {
        Component source = affixLine.source();
        int nameX = x;
        int nameWidth = width;
        if (!source.getString().isEmpty()) {
            int sourceWidth = Math.min(font.width(source), 42);
            guiGraphics.drawString(font, clipped(font, source, sourceWidth), x, y, TEXT_MUTED, false);
            nameX = x + sourceWidth + 5;
            nameWidth = Math.max(0, width - sourceWidth - 5);
        }
        guiGraphics.drawString(font, clipped(font, affixLine.name(), nameWidth), nameX, y, affixLine.nameColor(), false);
    }

    private static Component clipped(Font font, Component component, int width) {
        String text = component.getString();
        if (font.width(text) <= width) {
            return component;
        }

        String suffix = "...";
        int availableWidth = width - font.width(suffix);
        if (availableWidth <= 0) {
            return Component.empty();
        }
        while (!text.isEmpty() && font.width(text) > availableWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return Component.literal(text + suffix);
    }

    private static void addMachineAffixes(List<AffixLine> affixLines, MachineTraits traits) {
        for (MachineModifier modifier : traits.modifiers()) {
            if (!shouldShowModifier(modifier)) {
                continue;
            }
            Component source = MachineModifierText.slotLabel(modifier.slot());
            Component name = MachineModifierText.displayName(modifier);
            affixLines.add(new AffixLine(
                    source,
                    name,
                    modifierSummary(modifier),
                    TEXT_ENHANCED,
                    true,
                    modifierTooltip(source, name, modifier)
            ));
        }
        for (MachineBehavior behavior : traits.behaviors()) {
            affixLines.add(new AffixLine(
                    Component.translatable("rngtech.configuration.source.behavior"),
                    Component.translatable(behavior.translationKey()),
                    Component.translatable(behavior.descriptionKey()),
                    TEXT_ENHANCED,
                    true
            ));
        }
    }

    private static void addEmptyAffixLineIfNeeded(List<AffixLine> affixLines, MachineTraits traits) {
        if (!affixLines.isEmpty()) {
            return;
        }
        affixLines.add(new AffixLine(
                Component.translatable("rngtech.configuration.source.forge"),
                Component.translatable("rngtech.configuration.no_affixes"),
                Component.translatable("rngtech.configuration.refinement_potential", traits.refinementPotential()),
                TEXT_MUTED,
                false
        ));
    }

    private static boolean shouldShowModifier(MachineModifier modifier) {
        return MachineNameGenerator.hasModifierWord(modifier)
                && (hasActiveNumericEffect(modifier)
                        || ModifierEligibilityProfiles.isBatteryChassisBalanceMode(modifier));
    }

    private static boolean hasActiveNumericEffect(MachineModifier modifier) {
        return modifier.effects().stream().anyMatch(MachineScreenStyle::isActiveEffect);
    }

    private static boolean isActiveEffect(MachineModifierEffect effect) {
        return switch (effect.operation()) {
            case ADD, INCREASED_PERCENT, DECREASED_PERCENT -> !isZero(effect.value());
            case MORE, LESS -> !isZero(effect.value() - 1.0);
        };
    }

    private static Component modifierSummary(MachineModifier modifier) {
        if (ModifierEligibilityProfiles.isBatteryChassisBalanceMode(modifier)) {
            return Component.translatable("rngtech.tooltip.modifier.balance_mode.effect");
        }
        if (modifier.effects().size() != 1) {
            return Component.translatable("rngtech.tooltip.modifier.description.multiple");
        }
        return modifierEffectDescription(modifier.effects().getFirst());
    }

    private static List<Component> modifierTooltip(Component source, Component name, MachineModifier modifier) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(tooltipTitle(source, name));
        tooltip.addAll(modifierDescriptionLines(modifier));
        addModifierDescriptionDetail(tooltip, modifier);
        return tooltip;
    }

    private static List<Component> modifierDescriptionLines(MachineModifier modifier) {
        if (ModifierEligibilityProfiles.isBatteryChassisBalanceMode(modifier)) {
            return List.of(Component.translatable("rngtech.tooltip.modifier.balance_mode"));
        }
        if (MachineModifierText.hasCustomDescriptionDetail(modifier)) {
            return List.of(MachineModifierText.tooltipLine(modifier));
        }

        return List.of(MachineModifierText.fullEffect(modifier));
    }

    private static void addModifierDescriptionDetail(List<Component> tooltip, MachineModifier modifier) {
        if (!MachineModifierText.hasCustomDescriptionDetail(modifier)) {
            return;
        }
        tooltip.add(Screen.hasShiftDown()
                ? MachineModifierText.customDescriptionDetail(modifier)
                : Component.translatable("rngtech.tooltip.hold_shift_details"));
    }

    private static Component modifierEffectDescription(MachineModifierEffect effect) {
        return MachineStatDisplay.effectText(effect);
    }

    private static List<Component> affixTooltip(Component source, Component name, Component effect) {
        if (effect.getString().isEmpty()) {
            return List.of(tooltipTitle(source, name));
        }
        return List.of(tooltipTitle(source, name), effect);
    }

    private static Component tooltipTitle(Component source, Component name) {
        if (source.getString().isEmpty()) {
            return name;
        }
        return Component.empty().append(source).append(Component.literal(": ")).append(name);
    }

    private static List<Component> overflowTooltip(AffixLine[] affixLines, int firstHiddenIndex) {
        List<Component> tooltip = new ArrayList<>();
        for (int index = firstHiddenIndex; index < affixLines.length; index++) {
            AffixLine affixLine = affixLines[index];
            tooltip.add(affixLine.tooltip().isEmpty() ? affixLine.name() : affixLine.tooltip().getFirst());
        }
        return tooltip;
    }

    static final class StatLine {
        private final Component label;
        private final String value;
        private final double strength;
        private final boolean integral;
        private final boolean enhanced;
        private final Component tooltip;

        private StatLine(Component label, String value, double strength, boolean integral, boolean enhanced, Component tooltip) {
            this.label = label;
            this.value = value;
            this.strength = strength;
            this.integral = integral;
            this.enhanced = enhanced;
            this.tooltip = tooltip;
        }

        private Component label() {
            return label;
        }

        private String value() {
            return value;
        }

        private boolean enhanced() {
            return enhanced;
        }

        private double strength() {
            return strength;
        }

        private Component tooltip() {
            return tooltip;
        }

        private int pipCount() {
            if (strength <= 0.0) {
                return 0;
            }
            int pips = integral ? (int) Math.round(strength) : (int) Math.round(strength * 2.0);
            return Math.max(1, Math.min(5, pips));
        }
    }

    static final class AffixLine {
        private final Component source;
        private final Component name;
        private final Component effect;
        private final int nameColor;
        private final boolean highlighted;
        private final List<Component> tooltip;

        private AffixLine(Component source, Component name, Component effect, int nameColor, boolean highlighted) {
            this(source, name, effect, nameColor, highlighted, affixTooltip(source, name, effect));
        }

        private AffixLine(
                Component source,
                Component name,
                Component effect,
                int nameColor,
                boolean highlighted,
                List<Component> tooltip
        ) {
            this.source = source;
            this.name = name;
            this.effect = effect;
            this.nameColor = nameColor;
            this.highlighted = highlighted;
            this.tooltip = List.copyOf(tooltip);
        }

        private Component source() {
            return source;
        }

        private Component name() {
            return name;
        }

        private Component effect() {
            return effect;
        }

        private int nameColor() {
            return nameColor;
        }

        private boolean highlighted() {
            return highlighted;
        }

        private List<Component> tooltip() {
            return tooltip;
        }
    }
}
