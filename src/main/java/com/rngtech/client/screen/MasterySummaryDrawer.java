package com.rngtech.client.screen;

import com.rngtech.content.menu.MasteryMenuView;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatDisplay;
import com.rngtech.rpg.progression.Ascendancy;
import com.rngtech.rpg.progression.AscendancyCatalog;
import com.rngtech.rpg.progression.AscendancyNode;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MasteryBonusSummary;
import com.rngtech.rpg.progression.MasteryEffectSource;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveNodeKind;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/** Toggleable side drawer for the expanded Mastery view that lists the combined bonuses of every allocated node. */
final class MasterySummaryDrawer {
    private static final int WIDTH = 188;
    private static final int HANDLE_WIDTH = 10;
    private static final int HANDLE_HEIGHT = 40;
    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int SECTION_GAP = 4;
    private static final int SCROLL_STEP = 12;
    private static final int MAX_SOURCES = 8;
    private static final int BACKGROUND = 0xF20D1318;
    private static final int BORDER = 0xFF655B46;
    private static final int HANDLE = 0xFF343E49;
    private static final int HANDLE_HOVER = 0xFF465366;
    private static final int TITLE = 0xFFFFFFFF;
    private static final int HEADER = 0xFFE7DDC0;
    private static final int VALUE = 0xFFC9C3B3;
    private static final int ATTRIBUTE = 0xFF8FD3E6;
    private static final int KEYSTONE = 0xFFC79DEA;
    private static final int ASCENDANCY = 0xFFE3B866;
    private static final int INACTIVE = 0xFF6F6A60;
    private static final int HOVER = 0x30FFFFFF;
    private static final List<MachineStat> ATTRIBUTES = List.of(MachineStat.CONTROL, MachineStat.DRIVE, MachineStat.RESERVE);

    private final MasteryMenuView<?> view;
    private final Function<MasteryEffectSource, List<Component>> nodeTooltip;
    private boolean open;
    private double scroll;
    private Object rowsKey;
    private List<Row> rows = List.of();
    private List<List<FormattedCharSequence>> wrapped;
    private int wrappedWidth;

    MasterySummaryDrawer(MasteryMenuView<?> view, Function<MasteryEffectSource, List<Component>> nodeTooltip) {
        this.view = view;
        this.nodeTooltip = nodeTooltip;
    }

    /** True over the handle or the open drawer, where the tree beneath must not react. */
    boolean covers(Bounds bounds, double mouseX, double mouseY) {
        return overHandle(bounds, mouseX, mouseY) || open && overPanel(bounds, mouseX, mouseY);
    }

    boolean mouseClicked(Bounds bounds, double mouseX, double mouseY) {
        if (overHandle(bounds, mouseX, mouseY)) {
            open = !open;
            return true;
        }
        return open && overPanel(bounds, mouseX, mouseY);
    }

    boolean mouseScrolled(Bounds bounds, Font font, double mouseX, double mouseY, double amount) {
        if (!open || !overPanel(bounds, mouseX, mouseY)) {
            return false;
        }
        scroll = Mth.clamp(scroll - amount * SCROLL_STEP, 0, maxScroll(bounds, font));
        return true;
    }

    void render(GuiGraphics graphics, Font font, Bounds bounds, int mouseX, int mouseY) {
        if (open) {
            renderPanel(graphics, font, bounds, mouseX, mouseY);
        }
        int left = handleLeft(bounds);
        int top = handleTop(bounds);
        graphics.fill(left - 1, top - 1, left + HANDLE_WIDTH + 1, top + HANDLE_HEIGHT + 1, BORDER);
        graphics.fill(left, top, left + HANDLE_WIDTH, top + HANDLE_HEIGHT, overHandle(bounds, mouseX, mouseY) ? HANDLE_HOVER : HANDLE);
        // The chevron points the way the drawer moves when clicked.
        int middle = top + HANDLE_HEIGHT / 2;
        for (int step = 0; step < 5; step++) {
            int offset = Math.abs(step - 2) * 2;
            int x = left + 2 + (open ? 4 - offset : offset);
            graphics.fill(x, middle - 5 + step * 2, x + 2, middle - 3 + step * 2, HEADER);
        }
    }

    List<Component> tooltip(Bounds bounds, Font font, double mouseX, double mouseY) {
        if (overHandle(bounds, mouseX, mouseY)) {
            return List.of(Component.translatable(open ? "rngtech.mastery.summary.close" : "rngtech.mastery.summary.open"));
        }
        if (!open || !overPanel(bounds, mouseX, mouseY)) {
            return List.of();
        }
        Row row = rowAt(bounds, font, mouseY);
        return row == null || row.tooltip() == null ? List.of() : row.tooltip().get();
    }

    private void renderPanel(GuiGraphics graphics, Font font, Bounds bounds, int mouseX, int mouseY) {
        int left = panelLeft(bounds);
        graphics.fill(left - 1, bounds.top(), left, bounds.bottom(), BORDER);
        graphics.fill(left, bounds.top(), bounds.right(), bounds.bottom(), BACKGROUND);
        scroll = Mth.clamp(scroll, 0, maxScroll(bounds, font));
        Row hovered = overPanel(bounds, mouseX, mouseY) ? rowAt(bounds, font, mouseY) : null;
        graphics.enableScissor(left, bounds.top() + 1, bounds.right(), bounds.bottom() - 1);
        List<Row> rows = rows();
        List<List<FormattedCharSequence>> text = wrapped(font, bounds);
        int y = bounds.top() + PADDING - (int) scroll;
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            List<FormattedCharSequence> lines = text.get(index);
            y += row.gap();
            if (row == hovered && row.tooltip() != null) {
                graphics.fill(left + 2, y - 1, bounds.right() - 2, y + lines.size() * LINE_HEIGHT - 1, HOVER);
            }
            for (FormattedCharSequence line : lines) {
                graphics.drawString(font, line, left + PADDING + row.indent(), y, row.color(), false);
                y += LINE_HEIGHT;
            }
        }
        graphics.disableScissor();
        int overflow = (int) maxScroll(bounds, font);
        if (overflow > 0) {
            int track = bounds.bottom() - bounds.top() - 4;
            int thumb = Math.max(12, track * track / (track + overflow));
            int thumbTop = bounds.top() + 2 + (int) ((track - thumb) * scroll / overflow);
            graphics.fill(bounds.right() - 3, thumbTop, bounds.right() - 1, thumbTop + thumb, BORDER);
        }
    }

    private Row rowAt(Bounds bounds, Font font, double mouseY) {
        List<Row> rows = rows();
        List<List<FormattedCharSequence>> text = wrapped(font, bounds);
        int y = bounds.top() + PADDING - (int) scroll;
        for (int index = 0; index < rows.size(); index++) {
            y += rows.get(index).gap();
            int height = text.get(index).size() * LINE_HEIGHT;
            if (mouseY >= y - 1 && mouseY < y + height - 1) {
                return rows.get(index);
            }
            y += height;
        }
        return null;
    }

    private double maxScroll(Bounds bounds, Font font) {
        List<Row> rows = rows();
        List<List<FormattedCharSequence>> text = wrapped(font, bounds);
        int height = PADDING * 2;
        for (int index = 0; index < rows.size(); index++) {
            height += rows.get(index).gap() + text.get(index).size() * LINE_HEIGHT;
        }
        return Math.max(0, height - (bounds.bottom() - bounds.top()));
    }

    private List<Row> rows() {
        var snapshot = view.masterySnapshot();
        double[] attributes = ATTRIBUTES.stream().mapToDouble(view::masteryAttribute).toArray();
        Object key = List.of(snapshot.allocatedNodes(), snapshot.ascendancy(), snapshot.ascendancyNodes(), view.masteryFamily(), Arrays.toString(attributes));
        if (!key.equals(rowsKey)) {
            rowsKey = key;
            rows = buildRows(snapshot, attributes);
            wrapped = null;
        }
        return rows;
    }

    /** Wrapped text for each row, kept until the rows or the drawer width change. */
    private List<List<FormattedCharSequence>> wrapped(Font font, Bounds bounds) {
        List<Row> current = rows();
        int width = textWidth(bounds);
        if (wrapped == null || wrappedWidth != width) {
            wrappedWidth = width;
            wrapped = current.stream().map(row -> font.split(row.text(), width - row.indent())).toList();
        }
        return wrapped;
    }

    private List<Row> buildRows(MachineProgressionState snapshot, double[] attributes) {
        List<String> allocatedIds = snapshot.allocatedNodes();
        List<AscendancyNode> ascendancy = AscendancyCatalog.allocated(snapshot, view.masteryFamily());
        List<MasteryBonusSummary.Line> lines = MasteryBonusSummary.of(snapshot, view, attributes[0], attributes[1], attributes[2]);
        List<Row> built = new ArrayList<>();
        built.add(new Row(Component.translatable("rngtech.mastery.summary.title"), TITLE, 0, 0, null));
        built.add(new Row(allocatedIds.isEmpty() && ascendancy.isEmpty() ? Component.translatable("rngtech.mastery.summary.empty")
                : Component.translatable("rngtech.mastery.summary.allocated", allocatedIds.size()), INACTIVE, 0, 0, null));
        for (int index = 0; index < ATTRIBUTES.size(); index++) {
            MachineStat attribute = ATTRIBUTES.get(index);
            double total = attributes[index];
            Component text = Component.translatable("rngtech.mastery.attribute_total", Component.translatable(attribute.translationKey()), MachineStatDisplay.formatNumber(total));
            built.add(new Row(text, ATTRIBUTE, 0, index == 0 ? SECTION_GAP : 0, () -> attributeTooltip(text, attribute, lines)));
        }

        List<MegaPassiveNode> keystones = allocatedIds.stream().map(MegaPassiveTree::node).filter(Objects::nonNull)
                .filter(node -> node.kind() == PassiveNodeKind.KEYSTONE).toList();
        section(built, "keystones", keystones.stream().map(node -> new Row(Component.translatable(node.translationKey()), KEYSTONE, 4, 0, () -> nodeTooltip.apply(node))).toList());
        section(built, "ascendancy", ascendancyRows(snapshot, ascendancy));
        section(built, "modifiers", rowsOf(lines, true, MasteryBonusSummary.Kind.EFFECT));
        section(built, "attributes", rowsOf(lines, true, MasteryBonusSummary.Kind.CONVERSION));
        section(built, "scaling", rowsOf(lines, true, MasteryBonusSummary.Kind.SCALING));
        section(built, "limits", rowsOf(lines, true, MasteryBonusSummary.Kind.FIXED, MasteryBonusSummary.Kind.CEILING, MasteryBonusSummary.Kind.HARDNESS));
        section(built, "special", rowsOf(lines, true, MasteryBonusSummary.Kind.BEHAVIOR, MasteryBonusSummary.Kind.PASSIVE));
        section(built, "inactive", rowsOf(lines, false, MasteryBonusSummary.Kind.values()));
        return List.copyOf(built);
    }

    /** The chosen ascendancy with its root, then its allocated notables; small nodes appear only in the combined lines. */
    private List<Row> ascendancyRows(MachineProgressionState snapshot, List<AscendancyNode> allocated) {
        Ascendancy chosen = AscendancyCatalog.get(snapshot.ascendancy());
        if (allocated.isEmpty() || chosen == null) {
            return List.of();
        }
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(Component.translatable("rngtech.mastery.summary.ascendancy", Component.translatable(chosen.translationKey()),
                snapshot.ascendancyNodes().size(), snapshot.ascendancyPoints()), ASCENDANCY, 4, 0, () -> nodeTooltip.apply(chosen.root())));
        allocated.stream().filter(node -> node.kind() == AscendancyNode.Kind.NOTABLE)
                .forEach(node -> rows.add(new Row(Component.translatable(node.translationKey()), ASCENDANCY, 8, 0, () -> nodeTooltip.apply(node))));
        return rows;
    }

    private static void section(List<Row> rows, String name, List<Row> entries) {
        if (entries.isEmpty()) {
            return;
        }
        rows.add(new Row(Component.translatable("rngtech.mastery.summary.section." + name), HEADER, 0, SECTION_GAP, null));
        rows.addAll(entries);
    }

    private List<Row> rowsOf(List<MasteryBonusSummary.Line> lines, boolean active, MasteryBonusSummary.Kind... kinds) {
        List<MasteryBonusSummary.Kind> wanted = List.of(kinds);
        return lines.stream().filter(line -> line.active() == active && wanted.contains(line.kind()))
                .map(line -> new Row(text(line), active ? VALUE : INACTIVE, 4, 0, () -> lineTooltip(line))).toList();
    }

    private static Component text(MasteryBonusSummary.Line line) {
        MachineModifierEffect effect = line.effect();
        return switch (line.kind()) {
            case EFFECT -> MachineStatDisplay.effectText(effect);
            case CONVERSION -> Component.translatable("rngtech.mastery.summary.conversion", MachineStatDisplay.effectText(effect), Component.translatable(line.attribute().translationKey()));
            case SCALING -> Component.translatable("rngtech.mastery.scaling", MachineStatDisplay.formatNumber(effect.value()),
                    Component.translatable("rngtech.mastery.operation." + effect.operation().name().toLowerCase(Locale.ROOT)),
                    Component.translatable(effect.stat().translationKey()), Component.translatable(line.attribute().translationKey()));
            case FIXED -> Component.translatable("rngtech.mastery.fixed", Component.translatable(effect.stat().translationKey()), MachineStatDisplay.formatNumber(effect.value()));
            case CEILING -> Component.translatable("rngtech.mastery.ceiling", Component.translatable(effect.stat().translationKey()), MachineStatDisplay.formatNumber(effect.value()));
            case HARDNESS -> Component.translatable("rngtech.mastery.hardness_ceiling", line.amount());
            case PASSIVE -> Component.translatable("rngtech.mastery.passive_stat." + line.passive().name().toLowerCase(Locale.ROOT), (line.amount() > 0 ? "+" : "") + line.amount());
            case BEHAVIOR -> Component.translatable("rngtech.mastery.behavior." + line.behavior().toLowerCase(Locale.ROOT));
        };
    }

    private List<Component> lineTooltip(MasteryBonusSummary.Line line) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(text(line).copy().withStyle(ChatFormatting.WHITE));
        MachineModifierEffect effect = line.effect();
        switch (line.kind()) {
            case EFFECT -> {
                description(tooltip, effect.stat(), MachineStatDisplay.effectValue(effect));
                if (line.sources().size() > 1) {
                    tooltip.add(Component.translatable("rngtech.mastery.summary.combined").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            case CONVERSION -> {
                description(tooltip, effect.stat(), MachineStatDisplay.effectValue(effect));
                tooltip.add(Component.translatable("rngtech.mastery.summary.conversion_hint",
                        MachineStatDisplay.formatNumber(view.masteryAttribute(line.attribute())), Component.translatable(line.attribute().translationKey())).withStyle(ChatFormatting.DARK_GRAY));
            }
            case SCALING -> {
                double attribute = Math.max(0, view.masteryAttribute(line.attribute()));
                MachineModifierEffect current = MachineModifierEffect.fixed(effect.stat(), effect.operation(), effect.value() * attribute);
                description(tooltip, effect.stat(), MachineStatDisplay.effectValue(current));
                tooltip.add(Component.translatable("rngtech.mastery.summary.scaling_hint", MachineStatDisplay.effectValue(current),
                        MachineStatDisplay.formatNumber(attribute), Component.translatable(line.attribute().translationKey())).withStyle(ChatFormatting.GRAY));
            }
            case FIXED, CEILING, HARDNESS -> tooltip.add(Component.translatable("rngtech.mastery.summary.limit_hint").withStyle(ChatFormatting.GRAY));
            default -> { }
        }
        if (!line.active()) {
            tooltip.add(Component.translatable("rngtech.mastery.summary.inactive_hint").withStyle(ChatFormatting.RED));
        }
        sources(tooltip, line.sources());
        return tooltip;
    }

    private List<Component> attributeTooltip(Component text, MachineStat attribute, List<MasteryBonusSummary.Line> lines) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(text.copy().withStyle(ChatFormatting.WHITE));
        description(tooltip, attribute, MachineStatDisplay.statValue(attribute, view.masteryAttribute(attribute)));
        lines.stream().filter(line -> line.kind() == MasteryBonusSummary.Kind.CONVERSION && line.active() && line.attribute() == attribute)
                .forEach(line -> tooltip.add(MachineStatDisplay.effectText(line.effect()).withStyle(ChatFormatting.AQUA)));
        return tooltip;
    }

    private static void description(List<Component> tooltip, MachineStat stat, String value) {
        String key = stat.translationKey() + ".description";
        if (Language.getInstance().has(key)) {
            tooltip.add(Component.translatable(key, value).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void sources(List<Component> tooltip, List<MasteryEffectSource> sources) {
        if (sources.isEmpty()) {
            return;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        sources.forEach(node -> counts.merge(node.translationKey(), 1, Integer::sum));
        tooltip.add(Component.translatable("rngtech.mastery.summary.sources").withStyle(ChatFormatting.DARK_GRAY));
        counts.entrySet().stream().limit(MAX_SOURCES).forEach(entry -> tooltip.add((entry.getValue() > 1
                ? Component.translatable("rngtech.mastery.summary.source", Component.translatable(entry.getKey()), entry.getValue())
                : Component.translatable(entry.getKey())).withStyle(ChatFormatting.DARK_GRAY)));
        if (counts.size() > MAX_SOURCES) {
            tooltip.add(Component.translatable("rngtech.mastery.summary.more_sources", counts.size() - MAX_SOURCES).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private boolean overHandle(Bounds bounds, double mouseX, double mouseY) {
        int left = handleLeft(bounds);
        int top = handleTop(bounds);
        return mouseX >= left && mouseX < left + HANDLE_WIDTH && mouseY >= top && mouseY < top + HANDLE_HEIGHT;
    }

    private boolean overPanel(Bounds bounds, double mouseX, double mouseY) {
        return mouseX >= panelLeft(bounds) && mouseX < bounds.right() && mouseY >= bounds.top() && mouseY < bounds.bottom();
    }

    private int panelLeft(Bounds bounds) {
        return bounds.right() - Math.min(WIDTH, (bounds.right() - bounds.left()) / 2);
    }

    private int textWidth(Bounds bounds) {
        return bounds.right() - panelLeft(bounds) - PADDING * 2 - 2;
    }

    private int handleLeft(Bounds bounds) {
        return (open ? panelLeft(bounds) - 1 : bounds.right()) - HANDLE_WIDTH;
    }

    private int handleTop(Bounds bounds) {
        return bounds.top() + (bounds.bottom() - bounds.top() - HANDLE_HEIGHT) / 2;
    }

    record Bounds(int left, int top, int right, int bottom) {
    }

    private record Row(Component text, int color, int indent, int gap, Supplier<List<Component>> tooltip) {
    }
}
