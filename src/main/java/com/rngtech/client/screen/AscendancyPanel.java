package com.rngtech.client.screen;

import com.rngtech.RNGTech;
import com.rngtech.content.menu.MasteryMenuView;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.progression.AscendStatus;
import com.rngtech.rpg.progression.Ascendancy;
import com.rngtech.rpg.progression.AscendancyCatalog;
import com.rngtech.rpg.progression.AscendancyNode;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * The Mastery tab's Ascendancy panel. It covers the tree view and shows either the chosen ascendancy, with its points and
 * the next Seal, or the choose dialog that previews every ascendancy for the family before a Seal is spent.
 */
final class AscendancyPanel {
    static final int ACCENT = 0xFFE3B866;
    private static final int HEADER_HEIGHT = 16;
    private static final int FOOTER_HEIGHT = 22;
    private static final int PADDING = 4;
    private static final int BUTTON_SIZE = 18;
    private static final int BUTTON_GAP = 4;
    private static final int CLOSE_SIZE = 10;
    private static final int MIN_COLUMN_WIDTH = 100;
    private static final int CARD_MAX_WIDTH = 196;
    private static final int CARD_MAX_HEIGHT = 300;
    private static final int CARD_GAP = 12;
    private static final int CARD_INSET = 6;
    private static final int CARD_HEADER = 28;
    private static final int CARD_HEADER_SHORT = 15;
    /** Cards shorter than this drop the root subtitle, as in the compact Mastery view. */
    private static final int CARD_SHORT_HEIGHT = 170;
    /** Text lines give way until the tree keeps at least this much height. */
    private static final int CARD_MIN_TREE_HEIGHT = 64;
    private static final float CARD_TREE_SPACING = 34.0F;
    private static final int LINE_HEIGHT = 10;
    private static final int ROOT_LINES = 4;
    private static final int CARD_LINES = 7;
    private static final int LABEL_GAP = 6;
    private static final int BACKGROUND = 0xF20D1318;
    private static final int BORDER = 0xFF655B46;
    private static final int BUTTON = 0xFF343E49;
    private static final int BUTTON_HOVER = 0xFF465366;
    private static final int BUTTON_READY = 0xFF596035;
    private static final int COLUMN = 0xFF141D24;
    private static final int COLUMN_SELECTED = 0xFF232C1F;
    private static final int TITLE = 0xFFFFFFFF;
    private static final int TEXT = 0xFFE7DDC0;
    private static final int MUTED = 0xFF8A8372;
    private static final int LINK = 0xFF82745A;
    private static final int LOCKED_BORDER = 0xFF9D885F;
    private static final int OUTLINE = 0xFF080D11;
    private static final int HOVER_RING = 0xFFF2E6C2;
    private static final int CARD_HOVER = 0xFF8C7F5E;
    private static final int DEEP_TEXT = 0xFFB794E6;

    private final MasteryMenuView<?> view;
    private final MasteryScreenSupport.Palette palette;
    private final BiPredicate<String, String> send;
    private final Map<ResourceLocation, Boolean> iconPresent = new HashMap<>();
    private boolean open;
    private boolean choosing;
    private Purpose purpose = Purpose.ASCEND;
    private String selected;
    private int page;

    private enum Purpose { ASCEND, FREE, SWITCH }

    private enum Part { NONE, CLOSE, POINTS, PRIMARY, SECONDARY, PREVIOUS, NEXT, COLUMN, NODE }

    /** A line of card text in its own color. */
    private record Line(FormattedCharSequence text, int color) { }

    /**
     * Where the choose dialog draws the current page: one centered card per ascendancy, and a footer with the confirm
     * button centered beside its label. Rendering and hit tests share it.
     */
    private record ChooseLayout(List<Ascendancy> shown, List<int[]> cards, int page, int pages, int primaryX, int labelX, FormattedCharSequence label) { }

    private record Hit(Part part, Ascendancy ascendancy, AscendancyTreeLayout.Placed node) {
        private static final Hit NONE = new Hit(Part.NONE, null, null);

        private static Hit of(Part part) {
            return new Hit(part, null, null);
        }
    }

    AscendancyPanel(MasteryMenuView<?> view, MasteryScreenSupport.Palette palette, BiPredicate<String, String> send) {
        this.view = view;
        this.palette = palette;
        this.send = send;
    }

    /** The panel exists for families with ascendancies, and for machines that already earned a tier. */
    boolean available() {
        return !AscendancyCatalog.forFamily(view.masteryFamily()).isEmpty() || view.masterySnapshot().sealTiers() > 0;
    }

    boolean isOpen() {
        return open;
    }

    void toggle() {
        open = !open;
        choosing = false;
        selected = null;
        page = 0;
    }

    void close() {
        open = false;
    }

    /** A Seal can be used, a free choice is waiting, or points are unspent. */
    boolean needsAttention() {
        MachineProgressionState state = view.masterySnapshot();
        AscendStatus status = status(state);
        return status == AscendStatus.READY || status == AscendStatus.FREE_CHOICE || state.ascendancyUnspent() > 0 && !state.ascendancy().isEmpty();
    }

    void render(GuiGraphics graphics, Font font, MasterySummaryDrawer.Bounds bounds, int mouseX, int mouseY) {
        MachineProgressionState state = sync();
        graphics.fill(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), BACKGROUND);
        graphics.fill(bounds.left(), bounds.top() + HEADER_HEIGHT, bounds.right(), bounds.top() + HEADER_HEIGHT + 1, BORDER);
        graphics.fill(bounds.left(), bounds.bottom() - FOOTER_HEIGHT - 1, bounds.right(), bounds.bottom() - FOOTER_HEIGHT, BORDER);
        Hit hit = hit(bounds, font, state, mouseX, mouseY);
        renderClose(graphics, bounds, hit.part() == Part.CLOSE);
        if (choosing) {
            renderChoose(graphics, font, bounds, state, hit);
        } else {
            renderTree(graphics, font, bounds, state, hit);
        }
    }

    boolean mouseClicked(MasterySummaryDrawer.Bounds bounds, double mouseX, double mouseY, int button) {
        MachineProgressionState state = sync();
        Hit hit = hit(bounds, Minecraft.getInstance().font, state, mouseX, mouseY);
        if (button == 1) {
            if (!choosing && hit.part() == Part.NODE && state.ascendancyNodes().contains(hit.node().node().id())) {
                send.test("refund_ascendancy", hit.node().node().id());
            }
            return true;
        }
        if (button != 0) {
            return true;
        }
        switch (hit.part()) {
            case CLOSE -> close();
            case PRIMARY -> primary(state);
            case SECONDARY -> secondary();
            case PREVIOUS -> page = Math.max(0, page - 1);
            case NEXT -> page++;
            case COLUMN -> selected = hit.ascendancy().id();
            case NODE -> {
                if (choosing) {
                    selected = hit.ascendancy().id();
                } else if (allocatable(state, hit.ascendancy(), hit.node().node())) {
                    send.test("allocate_ascendancy", hit.node().node().id());
                }
            }
            default -> { }
        }
        return true;
    }

    boolean mouseScrolled(double amount) {
        if (choosing) {
            page = Math.max(0, page - (int) Math.signum(amount));
        }
        return true;
    }

    List<Component> tooltip(MasterySummaryDrawer.Bounds bounds, Font font, double mouseX, double mouseY) {
        MachineProgressionState state = sync();
        Hit hit = hit(bounds, font, state, mouseX, mouseY);
        return switch (hit.part()) {
            case CLOSE -> List.of(Component.translatable("rngtech.mastery.ascendancy.close"));
            case POINTS -> pointsTooltip(state);
            case PRIMARY -> primaryTooltip(state);
            case SECONDARY -> secondaryTooltip(state);
            case PREVIOUS -> List.of(Component.translatable("rngtech.mastery.ascendancy.previous"));
            case NEXT -> List.of(Component.translatable("rngtech.mastery.ascendancy.next"));
            case COLUMN -> columnTooltip(hit.ascendancy());
            case NODE -> choosing ? nodeTooltip(hit.node().node(), false) : nodeTooltip(hit.node().node(), true);
            case NONE -> List.of();
        };
    }

    /** The crest beside the start node: the ascendancy, Seal tiers as gems, points, and what to do next. */
    List<Component> badgeTooltip() {
        MachineProgressionState state = view.masterySnapshot();
        Ascendancy chosen = AscendancyCatalog.get(state.ascendancy());
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("rngtech.mastery.ascendancy.crest.title")
                .withStyle(style -> style.withColor(TextColor.fromRgb(ACCENT & 0xFFFFFF)).withBold(true)));
        tooltip.add(chosen == null
                ? Component.translatable("rngtech.mastery.ascendancy.crest.unclaimed").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                : Component.translatable(chosen.translationKey()).withStyle(ChatFormatting.LIGHT_PURPLE));
        MutableComponent tiers = Component.empty();
        for (int tier = 0; tier < AscendancyCatalog.MAX_TIERS; tier++) {
            boolean earned = tier < state.sealTiers();
            tiers.append(Component.literal(earned ? "\u25c6" : "\u25c7").withStyle(earned ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        }
        tooltip.add(tiers.append("  ").append(Component.translatable("rngtech.mastery.ascendancy.crest.points",
                state.ascendancyNodes().size(), state.ascendancyPoints()).withStyle(ChatFormatting.GRAY)));
        tooltip.add(crestHint(state));
        return tooltip;
    }

    private Component crestHint(MachineProgressionState state) {
        AscendStatus status = status(state);
        if (status == AscendStatus.READY) {
            return Component.translatable("rngtech.mastery.ascendancy.crest.ready").withStyle(ChatFormatting.GREEN);
        }
        if (status == AscendStatus.FREE_CHOICE) {
            return Component.translatable("rngtech.mastery.ascendancy.crest.choose").withStyle(ChatFormatting.GREEN);
        }
        if (state.ascendancyUnspent() > 0 && !state.ascendancy().isEmpty()) {
            return Component.translatable("rngtech.mastery.ascendancy.crest.unspent", state.ascendancyUnspent()).withStyle(ChatFormatting.YELLOW);
        }
        return Component.translatable("rngtech.mastery.ascendancy.open").withStyle(ChatFormatting.DARK_GRAY);
    }

    /** A node's name, kind, and effects; {@code actions} adds allocation and refund hints for the chosen tree. */
    List<Component> nodeTooltip(AscendancyNode node, boolean actions) {
        Ascendancy ascendancy = AscendancyCatalog.get(node.ascendancy());
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(node.translationKey()).withStyle(ChatFormatting.WHITE));
        switch (node.kind()) {
            case ROOT -> tooltip.add(Component.translatable("rngtech.mastery.ascendancy.tooltip.root").withStyle(ChatFormatting.GOLD));
            case NOTABLE -> tooltip.add(ascendancy != null && ascendancy.isDeep(node)
                    ? Component.translatable("rngtech.mastery.ascendancy.tooltip.deep_notable").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("rngtech.mastery.tooltip.notable").withStyle(ChatFormatting.AQUA));
            case SMALL -> { }
        }
        MasteryScreenSupport.effectLines(node, view, tooltip);
        if (!actions || ascendancy == null) {
            return tooltip;
        }
        MachineProgressionState state = view.masterySnapshot();
        if (node.kind() == AscendancyNode.Kind.ROOT || state.ascendancyNodes().contains(node.id())) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.unlocked").withStyle(ChatFormatting.GREEN));
            if (node.kind() != AscendancyNode.Kind.ROOT) {
                tooltip.add(Component.translatable("rngtech.mastery.ascendancy.refund_hint", AscendancyCatalog.REFUNDS_PER_NODE).withStyle(ChatFormatting.DARK_GRAY));
            }
        } else if (!connected(state, ascendancy, node)) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.requires_link").withStyle(ChatFormatting.RED));
        } else if (state.ascendancyUnspent() <= 0) {
            tooltip.add(Component.translatable("rngtech.mastery.ascendancy.no_points").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.click_unlock").withStyle(ChatFormatting.YELLOW));
        }
        return tooltip;
    }

    /** Follows the synced state: a new choice shows its tree, and a machine without one shows the choose dialog. */
    private MachineProgressionState sync() {
        MachineProgressionState state = view.masterySnapshot();
        boolean chosen = !state.ascendancy().isEmpty();
        if (choosing && chosen && (purpose != Purpose.SWITCH || state.ascendancy().equals(selected))) {
            choosing = false;
            selected = null;
        } else if (!choosing && !chosen) {
            choosing = true;
            purpose = state.awaitingAscendancyChoice() ? Purpose.FREE : Purpose.ASCEND;
            selected = null;
            page = 0;
        }
        return state;
    }

    /** The tree view's primary button uses the next Seal; the choose dialog's confirms the selection. */
    private void primary(MachineProgressionState state) {
        if (!primaryReady(state)) {
            return;
        }
        if (!choosing) {
            send.test("ascend", "");
            return;
        }
        switch (purpose) {
            case ASCEND -> send.test("ascend", selected);
            case FREE -> send.test("choose_ascendancy", selected);
            case SWITCH -> send.test("switch_ascendancy", selected);
        }
    }

    /** The tree view's secondary button opens the switch dialog; while switching it goes back to the tree. */
    private void secondary() {
        choosing = !choosing;
        purpose = Purpose.SWITCH;
        selected = null;
        page = 0;
    }

    private boolean primaryReady(MachineProgressionState state) {
        if (!choosing) {
            return status(state) == AscendStatus.READY;
        }
        return selected != null && switch (purpose) {
            case ASCEND -> status(state) == AscendStatus.READY;
            case FREE -> true;
            case SWITCH -> switchReady(state);
        };
    }

    private boolean switchReady(MachineProgressionState state) {
        return state.ascendancyNodes().isEmpty() && hasSeal(1);
    }

    private boolean switchable() {
        return AscendancyCatalog.forFamily(view.masteryFamily()).size() > 1;
    }

    private AscendStatus status(MachineProgressionState state) {
        return AscendStatus.of(state, view.masteryFamily(), view.ascendancyEntryStage(), AscendancyPanel::hasSeal);
    }

    // Flat fills, text, and items end the GUI vertex batch, so each view draws them first, then its shapes, then icons.
    private void renderTree(GuiGraphics graphics, Font font, MasterySummaryDrawer.Bounds bounds, MachineProgressionState state, Hit hit) {
        Ascendancy chosen = AscendancyCatalog.get(state.ascendancy());
        if (chosen == null) {
            return;
        }
        int textLeft = bounds.left() + PADDING;
        graphics.drawString(font, clip(font, Component.translatable(chosen.translationKey()), pointsLeft(bounds, font) - textLeft - PADDING), textLeft, bounds.top() + 4, ACCENT, false);
        renderPoints(graphics, font, bounds, state);
        int tier = state.sealTiers() + 1;
        boolean ascendable = tier <= AscendancyCatalog.MAX_TIERS;
        if (ascendable) {
            renderButton(graphics, font, primaryX(bounds), buttonY(bounds), hit.part() == Part.PRIMARY, primaryReady(state), sealStack(tier), null);
        }
        if (switchable()) {
            renderButton(graphics, font, secondaryX(bounds, ascendable), buttonY(bounds), hit.part() == Part.SECONDARY, switchReady(state), null, "\u2194");
        }
        List<AscendancyTreeLayout.Placed> placed = treeLayout(bounds, chosen);
        GuiShapeBatch shapes = new GuiShapeBatch(graphics, bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
        drawTree(shapes, chosen, placed, state, hit, true);
        shapes.flush();
        drawIcons(graphics, placed);
    }

    private void renderChoose(GuiGraphics graphics, Font font, MasterySummaryDrawer.Bounds bounds, MachineProgressionState state, Hit hit) {
        Component title = Component.translatable("rngtech.mastery.ascendancy.choose." + purpose.name().toLowerCase(Locale.ROOT));
        int textLeft = bounds.left() + PADDING;
        graphics.drawString(font, clip(font, title, closeLeft(bounds) - textLeft - PADDING), textLeft, bounds.top() + 4, TITLE, false);
        if (candidates(state).isEmpty()) {
            graphics.drawString(font, clip(font, Component.translatable("rngtech.mastery.ascendancy.status.none"), bounds.right() - textLeft - PADDING),
                    textLeft, bodyTop(bounds) + 4, MUTED, false);
            return;
        }
        ChooseLayout layout = chooseLayout(bounds, font, state);
        page = layout.page();
        Map<Ascendancy, List<AscendancyTreeLayout.Placed>> trees = new LinkedHashMap<>();
        for (int index = 0; index < layout.shown().size(); index++) {
            Ascendancy ascendancy = layout.shown().get(index);
            int[] card = layout.cards().get(index);
            boolean hovered = (hit.part() == Part.COLUMN || hit.part() == Part.NODE) && hit.ascendancy() == ascendancy;
            renderCard(graphics, font, card, ascendancy, ascendancy.id().equals(selected), hovered);
            trees.put(ascendancy, previewLayout(font, card, ascendancy));
        }
        boolean ready = primaryReady(state);
        ItemStack seal = purpose == Purpose.FREE ? null : sealStack(purpose == Purpose.SWITCH ? 1 : state.sealTiers() + 1);
        renderButton(graphics, font, layout.primaryX(), buttonY(bounds), hit.part() == Part.PRIMARY, ready, seal, seal == null ? "\u2714" : null);
        graphics.drawString(font, layout.label(), layout.labelX(), buttonY(bounds) + 5, ready ? TEXT : MUTED, false);
        if (purpose == Purpose.SWITCH) {
            renderButton(graphics, font, primaryX(bounds), buttonY(bounds), hit.part() == Part.SECONDARY, true, null, "\u2190");
        }
        if (layout.pages() > 1) {
            renderButton(graphics, font, previousX(bounds), buttonY(bounds), hit.part() == Part.PREVIOUS, layout.page() > 0, null, "<");
            renderButton(graphics, font, nextX(bounds), buttonY(bounds), hit.part() == Part.NEXT, layout.page() < layout.pages() - 1, null, ">");
            String count = (layout.page() + 1) + "/" + layout.pages();
            graphics.drawString(font, count, previousX(bounds) - BUTTON_GAP - font.width(count), buttonY(bounds) + 5, MUTED, false);
        }
        GuiShapeBatch shapes = new GuiShapeBatch(graphics, bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
        trees.forEach((ascendancy, placed) -> drawTree(shapes, ascendancy, placed, state, hit, false));
        shapes.flush();
        trees.values().forEach(placed -> drawIcons(graphics, placed));
    }

    /** A card: the ascendancy and its root at the top, the tree in the middle, and the root's effects and deep notables below. */
    private void renderCard(GuiGraphics graphics, Font font, int[] card, Ascendancy ascendancy, boolean picked, boolean hovered) {
        int border = picked ? palette.outputBonus() : hovered ? CARD_HOVER : BORDER;
        graphics.fill(card[0] - 1, card[1] - 1, card[2] + 1, card[3] + 1, border);
        graphics.fill(card[0], card[1], card[2], card[3], picked ? COLUMN_SELECTED : COLUMN);
        int center = (card[0] + card[2]) / 2;
        int textWidth = card[2] - card[0] - CARD_INSET * 2;
        FormattedCharSequence name = clip(font, Component.translatable(ascendancy.translationKey()), textWidth);
        graphics.drawString(font, name, center - font.width(name) / 2, card[1] + 5, ACCENT, false);
        int header = cardHeader(card);
        if (header == CARD_HEADER) {
            FormattedCharSequence root = clip(font, Component.translatable(ascendancy.root().translationKey()), textWidth);
            graphics.drawString(font, root, center - font.width(root) / 2, card[1] + 16, MUTED, false);
        }
        graphics.fill(card[0] + CARD_INSET, card[1] + header - 1, card[2] - CARD_INSET, card[1] + header, picked ? palette.outputBonus() : BORDER);
        List<Line> lines = cardLines(font, card, ascendancy);
        int y = card[3] - CARD_INSET - lines.size() * LINE_HEIGHT;
        if (!lines.isEmpty()) {
            graphics.fill(card[0] + CARD_INSET, y - 4, card[2] - CARD_INSET, y - 3, BORDER);
        }
        for (Line line : lines) {
            graphics.drawString(font, line.text(), card[0] + CARD_INSET, y, line.color(), false);
            y += LINE_HEIGHT;
        }
    }

    private void drawTree(GuiShapeBatch shapes, Ascendancy ascendancy, List<AscendancyTreeLayout.Placed> placed, MachineProgressionState state, Hit hit, boolean live) {
        Map<String, AscendancyTreeLayout.Placed> byId = new HashMap<>();
        placed.forEach(entry -> byId.put(entry.node().id(), entry));
        for (AscendancyTreeLayout.Placed entry : placed) {
            AscendancyTreeLayout.Placed parent = byId.get(entry.node().parent());
            if (parent != null) {
                boolean active = live && allocated(state, entry.node()) && allocated(state, parent.node());
                shapes.line(parent.x(), parent.y(), entry.x(), entry.y(), active ? 2.0F : 1.0F, active ? palette.statAccent() : LINK);
            }
        }
        for (AscendancyTreeLayout.Placed entry : placed) {
            AscendancyNode node = entry.node();
            boolean isAllocated = live && allocated(state, node);
            boolean isAllocatable = live && !isAllocated && allocatable(state, ascendancy, node);
            int border = node.kind() == AscendancyNode.Kind.ROOT ? palette.startNode()
                    : isAllocated ? palette.statAccent() : isAllocatable ? palette.outputBonus() : LOCKED_BORDER;
            float radius = entry.radius();
            if (entry.deep()) {
                shapes.diamond(entry.x(), entry.y(), radius + 1.0F, OUTLINE);
                shapes.diamond(entry.x(), entry.y(), radius, border);
                shapes.diamond(entry.x(), entry.y(), Math.max(1.0F, radius - 1.5F), isAllocated ? 0xFF49315C : 0xFF291E35);
            } else {
                shapes.disc(entry.x(), entry.y(), radius + 1.0F, OUTLINE);
                shapes.disc(entry.x(), entry.y(), radius, border);
                shapes.disc(entry.x(), entry.y(), Math.max(1.0F, radius - 1.0F), fill(node, isAllocated, isAllocatable));
            }
            if (hit.part() == Part.NODE && hit.node() == entry) {
                shapes.ring(entry.x(), entry.y(), radius + 1.5F, radius + 2.5F, HOVER_RING);
            }
        }
    }

    private int fill(AscendancyNode node, boolean allocated, boolean allocatable) {
        if (node.kind() == AscendancyNode.Kind.ROOT) {
            return 0xFF54421E;
        }
        if (allocatable) {
            return 0xFF454024;
        }
        return switch (node.kind()) {
            case NOTABLE -> allocated ? 0xFF27434C : 0xFF1C3037;
            default -> allocated ? 0xFF263C3E : 0xFF202A2D;
        };
    }

    /** Draws icons that exist; roots and notables fall back to their shape until their art lands. */
    private void drawIcons(GuiGraphics graphics, List<AscendancyTreeLayout.Placed> placed) {
        for (AscendancyTreeLayout.Placed entry : placed) {
            int size = Math.min(16, Math.round(entry.radius() * 1.2F));
            if (size < 5) {
                continue;
            }
            ResourceLocation texture = RNGTech.id("textures/gui/mastery/" + entry.node().masteryIconKey() + ".png");
            boolean present = iconPresent.computeIfAbsent(texture, key -> Minecraft.getInstance().getResourceManager().getResource(key).isPresent());
            if (present) {
                graphics.blit(texture, Math.round(entry.x() - size / 2.0F), Math.round(entry.y() - size / 2.0F), size, size, 0, 0, 16, 16, 16, 16);
            }
        }
    }

    private void renderPoints(GuiGraphics graphics, Font font, MasterySummaryDrawer.Bounds bounds, MachineProgressionState state) {
        String points = state.ascendancyNodes().size() + "/" + state.ascendancyPoints();
        int left = pointsLeft(bounds, font);
        for (int tier = 0; tier < AscendancyCatalog.MAX_TIERS; tier++) {
            int x = left + tier * 7 + 3;
            int y = bounds.top() + 8;
            int color = tier < state.sealTiers() ? ACCENT : 0xFF3A3F44;
            for (int row = -3; row <= 3; row++) {
                int half = 3 - Math.abs(row);
                graphics.fill(x - half, y + row, x + half + 1, y + row + 1, color);
            }
        }
        int color = state.ascendancyUnspent() > 0 ? palette.outputBonus() : TEXT;
        graphics.drawString(font, points, left + AscendancyCatalog.MAX_TIERS * 7 + 3, bounds.top() + 4, color, false);
    }

    private void renderClose(GuiGraphics graphics, MasterySummaryDrawer.Bounds bounds, boolean hovered) {
        int left = closeLeft(bounds);
        int top = bounds.top() + 3;
        graphics.fill(left, top, left + CLOSE_SIZE, top + CLOSE_SIZE, hovered ? BUTTON_HOVER : BUTTON);
        for (int step = 2; step < CLOSE_SIZE - 2; step++) {
            graphics.fill(left + step, top + step, left + step + 1, top + step + 1, TEXT);
            graphics.fill(left + CLOSE_SIZE - 1 - step, top + step, left + CLOSE_SIZE - step, top + step + 1, TEXT);
        }
    }

    private void renderButton(GuiGraphics graphics, Font font, int left, int top, boolean hovered, boolean ready, ItemStack icon, String glyph) {
        graphics.fill(left - 1, top - 1, left + BUTTON_SIZE + 1, top + BUTTON_SIZE + 1, ready ? palette.outputBonus() : BORDER);
        graphics.fill(left, top, left + BUTTON_SIZE, top + BUTTON_SIZE, hovered ? BUTTON_HOVER : ready ? BUTTON_READY : BUTTON);
        if (icon != null) {
            graphics.renderItem(icon, left + 1, top + 1);
            if (!ready) {
                // Items render in front of flat fills, so the dimming layer moves forward too.
                graphics.pose().pushPose();
                graphics.pose().translate(0.0F, 0.0F, 200.0F);
                graphics.fill(left, top, left + BUTTON_SIZE, top + BUTTON_SIZE, 0x90101418);
                graphics.pose().popPose();
            }
        } else if (glyph != null) {
            graphics.drawString(font, glyph, left + (BUTTON_SIZE - font.width(glyph) + 1) / 2, top + 5, ready ? TEXT : MUTED, false);
        }
    }

    private Hit hit(MasterySummaryDrawer.Bounds bounds, Font font, MachineProgressionState state, double mouseX, double mouseY) {
        if (inside(mouseX, mouseY, closeLeft(bounds), bounds.top() + 3, CLOSE_SIZE, CLOSE_SIZE)) {
            return Hit.of(Part.CLOSE);
        }
        int buttonY = buttonY(bounds);
        if (choosing) {
            if (candidates(state).isEmpty()) {
                return Hit.NONE;
            }
            ChooseLayout layout = chooseLayout(bounds, font, state);
            if (inside(mouseX, mouseY, layout.primaryX(), buttonY, BUTTON_SIZE, BUTTON_SIZE)) {
                return Hit.of(Part.PRIMARY);
            }
            if (purpose == Purpose.SWITCH && inside(mouseX, mouseY, primaryX(bounds), buttonY, BUTTON_SIZE, BUTTON_SIZE)) {
                return Hit.of(Part.SECONDARY);
            }
            if (layout.pages() > 1 && inside(mouseX, mouseY, previousX(bounds), buttonY, BUTTON_SIZE, BUTTON_SIZE)) {
                return Hit.of(Part.PREVIOUS);
            }
            if (layout.pages() > 1 && inside(mouseX, mouseY, nextX(bounds), buttonY, BUTTON_SIZE, BUTTON_SIZE)) {
                return Hit.of(Part.NEXT);
            }
            for (int index = 0; index < layout.shown().size(); index++) {
                int[] card = layout.cards().get(index);
                if (mouseX >= card[0] && mouseX < card[2] && mouseY >= card[1] && mouseY < card[3]) {
                    Ascendancy ascendancy = layout.shown().get(index);
                    for (AscendancyTreeLayout.Placed entry : previewLayout(font, card, ascendancy)) {
                        if (entry.contains(mouseX, mouseY)) {
                            return new Hit(Part.NODE, ascendancy, entry);
                        }
                    }
                    return new Hit(Part.COLUMN, ascendancy, null);
                }
            }
            return Hit.NONE;
        }
        Ascendancy chosen = AscendancyCatalog.get(state.ascendancy());
        if (chosen == null) {
            return Hit.NONE;
        }
        boolean ascendable = state.sealTiers() < AscendancyCatalog.MAX_TIERS;
        if (ascendable && inside(mouseX, mouseY, primaryX(bounds), buttonY, BUTTON_SIZE, BUTTON_SIZE)) {
            return Hit.of(Part.PRIMARY);
        }
        if (switchable() && inside(mouseX, mouseY, secondaryX(bounds, ascendable), buttonY, BUTTON_SIZE, BUTTON_SIZE)) {
            return Hit.of(Part.SECONDARY);
        }
        if (mouseY >= bounds.top() && mouseY < bounds.top() + HEADER_HEIGHT && mouseX >= pointsLeft(bounds, font) && mouseX < closeLeft(bounds)) {
            return Hit.of(Part.POINTS);
        }
        for (AscendancyTreeLayout.Placed entry : treeLayout(bounds, chosen)) {
            if (entry.contains(mouseX, mouseY)) {
                return new Hit(Part.NODE, chosen, entry);
            }
        }
        return Hit.NONE;
    }

    private List<Component> pointsTooltip(MachineProgressionState state) {
        return List.of(
                Component.translatable("rngtech.mastery.ascendancy.title").withStyle(ChatFormatting.WHITE),
                Component.translatable("rngtech.mastery.ascendancy.tiers", state.sealTiers(), AscendancyCatalog.MAX_TIERS).withStyle(ChatFormatting.GRAY),
                Component.translatable("rngtech.mastery.ascendancy.points", state.ascendancyNodes().size(), state.ascendancyPoints(), state.ascendancyUnspent())
                        .withStyle(ChatFormatting.GRAY));
    }

    private List<Component> primaryTooltip(MachineProgressionState state) {
        List<Component> tooltip = new ArrayList<>();
        if (!choosing) {
            int tier = state.sealTiers() + 1;
            tooltip.add(Component.translatable("rngtech.mastery.ascendancy.ascend").withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("rngtech.mastery.ascendancy.uses_seal", sealName(tier), AscendancyCatalog.POINTS_PER_TIER).withStyle(ChatFormatting.GRAY));
            tooltip.add(statusLine(status(state), tier));
            return tooltip;
        }
        tooltip.add(Component.translatable("rngtech.mastery.ascendancy.confirm." + purpose.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.WHITE));
        switch (purpose) {
            case ASCEND -> {
                tooltip.add(Component.translatable("rngtech.mastery.ascendancy.uses_seal", sealName(1), AscendancyCatalog.POINTS_PER_TIER).withStyle(ChatFormatting.GRAY));
                AscendStatus status = status(state);
                if (status != AscendStatus.READY) {
                    tooltip.add(statusLine(status, state.sealTiers() + 1));
                } else if (selected == null) {
                    tooltip.add(Component.translatable("rngtech.mastery.ascendancy.select_first").withStyle(ChatFormatting.RED));
                } else {
                    tooltip.add(Component.translatable("rngtech.mastery.ascendancy.click_confirm").withStyle(ChatFormatting.YELLOW));
                }
            }
            case FREE -> {
                tooltip.add(statusLine(AscendStatus.FREE_CHOICE, 1));
                tooltip.add((selected == null ? Component.translatable("rngtech.mastery.ascendancy.select_first").withStyle(ChatFormatting.RED)
                        : Component.translatable("rngtech.mastery.ascendancy.click_confirm").withStyle(ChatFormatting.YELLOW)));
            }
            case SWITCH -> {
                tooltip.add(Component.translatable("rngtech.mastery.ascendancy.switch_cost", sealName(1)).withStyle(ChatFormatting.GRAY));
                if (!hasSeal(1)) {
                    tooltip.add(statusLine(AscendStatus.SEAL_MISSING, 1));
                } else if (selected == null) {
                    tooltip.add(Component.translatable("rngtech.mastery.ascendancy.select_first").withStyle(ChatFormatting.RED));
                } else {
                    tooltip.add(Component.translatable("rngtech.mastery.ascendancy.click_confirm").withStyle(ChatFormatting.YELLOW));
                }
            }
        }
        return tooltip;
    }

    private List<Component> switchTooltip(MachineProgressionState state) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("rngtech.mastery.ascendancy.switch").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("rngtech.mastery.ascendancy.switch_cost", sealName(1)).withStyle(ChatFormatting.GRAY));
        if (!state.ascendancyNodes().isEmpty()) {
            tooltip.add(Component.translatable("rngtech.mastery.ascendancy.switch_refund_first").withStyle(ChatFormatting.RED));
        } else if (!hasSeal(1)) {
            tooltip.add(statusLine(AscendStatus.SEAL_MISSING, 1));
        } else {
            tooltip.add(Component.translatable("rngtech.mastery.ascendancy.click_switch").withStyle(ChatFormatting.YELLOW));
        }
        return tooltip;
    }

    private List<Component> secondaryTooltip(MachineProgressionState state) {
        return choosing ? List.of(Component.translatable("rngtech.mastery.ascendancy.back")) : switchTooltip(state);
    }

    private List<Component> columnTooltip(Ascendancy ascendancy) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(ascendancy.translationKey()).withStyle(ChatFormatting.GOLD));
        tooltip.addAll(nodeTooltip(ascendancy.root(), false));
        tooltip.add(Component.translatable(ascendancy.id().equals(selected) ? "rngtech.mastery.ascendancy.selected" : "rngtech.mastery.ascendancy.click_select")
                .withStyle(ascendancy.id().equals(selected) ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        return tooltip;
    }

    private Component statusLine(AscendStatus status, int tier) {
        return switch (status) {
            case READY -> Component.translatable("rngtech.mastery.ascendancy.status.ready").withStyle(ChatFormatting.YELLOW);
            case FREE_CHOICE -> Component.translatable("rngtech.mastery.ascendancy.status.free_choice").withStyle(ChatFormatting.GREEN);
            case ALL_TIERS -> Component.translatable("rngtech.mastery.ascendancy.status.all_tiers").withStyle(ChatFormatting.GREEN);
            case NO_ASCENDANCIES -> Component.translatable("rngtech.mastery.ascendancy.status.none").withStyle(ChatFormatting.RED);
            case ENTRY_STAGE -> Component.translatable(view.masteryFamily() == MachineMasteryFamily.FORESTRY
                            ? "rngtech.mastery.ascendancy.status.entry_stage_tool" : "rngtech.mastery.ascendancy.status.entry_stage",
                    AscendancyCatalog.ENTRY_STAGE, view.ascendancyEntryStage()).withStyle(ChatFormatting.RED);
            case SEAL_MISSING -> Component.translatable("rngtech.mastery.ascendancy.status.seal_missing", sealName(tier)).withStyle(ChatFormatting.RED);
        };
    }

    /** Every ascendancy for the family; switching leaves out the current one. */
    private List<Ascendancy> candidates(MachineProgressionState state) {
        List<Ascendancy> all = AscendancyCatalog.forFamily(view.masteryFamily());
        return purpose == Purpose.SWITCH ? all.stream().filter(ascendancy -> !ascendancy.id().equals(state.ascendancy())).toList() : all;
    }

    private boolean allocated(MachineProgressionState state, AscendancyNode node) {
        return node.kind() == AscendancyNode.Kind.ROOT || state.ascendancyNodes().contains(node.id());
    }

    private boolean connected(MachineProgressionState state, Ascendancy ascendancy, AscendancyNode node) {
        return node.parent().equals(ascendancy.root().id()) || state.ascendancyNodes().contains(node.parent());
    }

    private boolean allocatable(MachineProgressionState state, Ascendancy ascendancy, AscendancyNode node) {
        return node.kind() != AscendancyNode.Kind.ROOT && !state.ascendancyNodes().contains(node.id())
                && state.ascendancyUnspent() > 0 && connected(state, ascendancy, node);
    }

    private List<AscendancyTreeLayout.Placed> treeLayout(MasterySummaryDrawer.Bounds bounds, Ascendancy ascendancy) {
        return AscendancyTreeLayout.fit(ascendancy, bounds.left() + PADDING, bodyTop(bounds), bounds.right() - PADDING, bodyBottom(bounds));
    }

    private List<AscendancyTreeLayout.Placed> previewLayout(Font font, int[] card, Ascendancy ascendancy) {
        int lines = cardLines(font, card, ascendancy).size();
        int bottom = card[3] - (lines > 0 ? CARD_INSET + lines * LINE_HEIGHT + 10 : CARD_INSET + 2);
        return AscendancyTreeLayout.fit(ascendancy, card[0] + CARD_INSET, card[1] + cardHeader(card) + 6, card[2] - CARD_INSET, bottom, CARD_TREE_SPACING);
    }

    private static int cardHeader(int[] card) {
        return card[3] - card[1] < CARD_SHORT_HEIGHT ? CARD_HEADER_SHORT : CARD_HEADER;
    }

    /**
     * The root's effects, then each deep notable, which defines the build. Lines give way, deep notables first, until the
     * tree keeps {@link #CARD_MIN_TREE_HEIGHT}; the card's tooltip still lists the root's effects.
     */
    private List<Line> cardLines(Font font, int[] card, Ascendancy ascendancy) {
        int room = card[3] - card[1] - cardHeader(card) - 6 - CARD_MIN_TREE_HEIGHT - CARD_INSET - 10;
        int limit = Math.max(0, Math.min(CARD_LINES, room / LINE_HEIGHT));
        int width = Math.max(1, card[2] - card[0] - CARD_INSET * 2);
        List<Component> effects = new ArrayList<>();
        MasteryScreenSupport.effectLines(ascendancy.root(), view, effects);
        List<Line> lines = new ArrayList<>();
        for (Component effect : effects) {
            font.split(effect, width).forEach(line -> lines.add(new Line(line, TEXT)));
        }
        List<Line> kept = new ArrayList<>(lines.subList(0, Math.min(lines.size(), Math.min(ROOT_LINES, limit))));
        for (AscendancyNode node : ascendancy.nodes().values()) {
            if (ascendancy.isDeep(node) && kept.size() < limit) {
                kept.add(new Line(clip(font, Component.literal("\u25c6 ").append(Component.translatable(node.translationKey())), width), DEEP_TEXT));
            }
        }
        return kept;
    }

    /** Cards for the current page, centered as a row and capped in size; the confirm button and its label sit centered below. */
    private ChooseLayout chooseLayout(MasterySummaryDrawer.Bounds bounds, Font font, MachineProgressionState state) {
        List<Ascendancy> candidates = candidates(state);
        int width = bounds.right() - bounds.left() - PADDING * 2;
        int perPage = Math.max(1, (width + CARD_GAP) / (MIN_COLUMN_WIDTH + CARD_GAP));
        int pages = Math.max(1, (candidates.size() + perPage - 1) / perPage);
        int current = Math.max(0, Math.min(page, pages - 1));
        List<Ascendancy> shown = candidates.subList(Math.min(candidates.size(), current * perPage), Math.min(candidates.size(), (current + 1) * perPage));
        int count = Math.max(1, shown.size());
        int cardWidth = Math.min(CARD_MAX_WIDTH, (width - CARD_GAP * (count - 1)) / count);
        int height = bodyBottom(bounds) - bodyTop(bounds);
        int cardHeight = Math.min(CARD_MAX_HEIGHT, height);
        int left = bounds.left() + PADDING + (width - count * cardWidth - CARD_GAP * (count - 1)) / 2;
        int top = bodyTop(bounds) + (height - cardHeight) / 2;
        List<int[]> cards = new ArrayList<>();
        for (int index = 0; index < shown.size(); index++) {
            int cardLeft = left + index * (cardWidth + CARD_GAP);
            cards.add(new int[] {cardLeft, top, cardLeft + cardWidth, top + cardHeight});
        }
        int spanLeft = purpose == Purpose.SWITCH ? primaryX(bounds) + BUTTON_SIZE + BUTTON_GAP : bounds.left() + PADDING;
        int spanRight = pages > 1 ? previousX(bounds) - BUTTON_GAP - font.width(pages + "/" + pages) - BUTTON_GAP : bounds.right() - PADDING;
        FormattedCharSequence label = clip(font, confirmLabel(), Math.max(1, spanRight - spanLeft - BUTTON_SIZE - LABEL_GAP));
        int group = BUTTON_SIZE + LABEL_GAP + font.width(label);
        int primary = Math.max(spanLeft, (bounds.left() + bounds.right()) / 2 - group / 2);
        return new ChooseLayout(shown, cards, current, pages, primary, primary + BUTTON_SIZE + LABEL_GAP, label);
    }

    private Component confirmLabel() {
        Ascendancy picked = AscendancyCatalog.get(selected);
        if (picked == null) {
            return Component.translatable("rngtech.mastery.ascendancy.choose_hint");
        }
        return Component.translatable("rngtech.mastery.ascendancy.confirm_label." + purpose.name().toLowerCase(Locale.ROOT),
                Component.translatable(picked.translationKey()));
    }

    private static FormattedCharSequence clip(Font font, Component text, int width) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(1, width));
        return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.getFirst();
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static int bodyTop(MasterySummaryDrawer.Bounds bounds) {
        return bounds.top() + HEADER_HEIGHT + 4;
    }

    private static int bodyBottom(MasterySummaryDrawer.Bounds bounds) {
        return bounds.bottom() - FOOTER_HEIGHT - 4;
    }

    private static int buttonY(MasterySummaryDrawer.Bounds bounds) {
        return bounds.bottom() - FOOTER_HEIGHT + 2;
    }

    private static int primaryX(MasterySummaryDrawer.Bounds bounds) {
        return bounds.left() + PADDING + 1;
    }

    private static int secondaryX(MasterySummaryDrawer.Bounds bounds, boolean afterPrimary) {
        return afterPrimary ? primaryX(bounds) + BUTTON_SIZE + BUTTON_GAP : primaryX(bounds);
    }

    private static int nextX(MasterySummaryDrawer.Bounds bounds) {
        return bounds.right() - PADDING - 1 - BUTTON_SIZE;
    }

    private static int previousX(MasterySummaryDrawer.Bounds bounds) {
        return nextX(bounds) - BUTTON_GAP - BUTTON_SIZE;
    }

    private static int closeLeft(MasterySummaryDrawer.Bounds bounds) {
        return bounds.right() - PADDING - CLOSE_SIZE;
    }

    private static int pointsLeft(MasterySummaryDrawer.Bounds bounds, Font font) {
        return closeLeft(bounds) - PADDING - AscendancyCatalog.MAX_TIERS * 7 - 3 - font.width("6/6");
    }

    private static Component sealName(int tier) {
        return tier < 1 || tier > AscendancyCatalog.MAX_TIERS ? Component.empty() : sealStack(tier).getHoverName();
    }

    private static ItemStack sealStack(int tier) {
        return new ItemStack(ModItems.ascendancySeal(tier).get());
    }

    /** Creative players also need the Seal in inventory, matching the server. */
    private static boolean hasSeal(int tier) {
        var player = Minecraft.getInstance().player;
        if (player == null || tier < 1 || tier > AscendancyCatalog.MAX_TIERS) {
            return false;
        }
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(ModItems.ascendancySeal(tier).get())) {
                return true;
            }
        }
        return false;
    }
}
