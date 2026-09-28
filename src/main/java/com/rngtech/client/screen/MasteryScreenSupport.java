package com.rngtech.client.screen;

import com.rngtech.content.menu.MasteryMenuView;
import com.rngtech.content.network.MasteryActionPayload;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatDisplay;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MasteryBuildCode;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.PassiveNode;
import com.rngtech.rpg.progression.PassiveNodeFlag;
import com.rngtech.rpg.progression.PassiveNodeKind;
import com.rngtech.rpg.progression.PassiveStatType;
import com.rngtech.rpg.progression.PassiveTree;
import com.rngtech.rpg.progression.PassiveTreeLayouts;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MasteryScreenSupport<N extends PassiveNode> {
    private static final int MASTERY_XP_BAR_X = 32;
    private static final int MASTERY_XP_BAR_Y = 30;
    private static final int MASTERY_XP_BAR_WIDTH = 176;
    private static final int MASTERY_VIEW_X = 8;
    private static final int MASTERY_VIEW_Y = 58;
    private static final int MASTERY_VIEW_WIDTH = 224;
    private static final int MASTERY_VIEW_HEIGHT = 148;
    private static final int MASTERY_VIEW_BOTTOM_PADDING = 10;
    private static final int MASTERY_CONTENT_PADDING = 24;
    private static final int MASTERY_SCROLL_STEP = 18;
    private static final int MASTERY_SCREEN_MARGIN_X = 32;
    private static final int MASTERY_SCREEN_MARGIN_Y = 56;
    private static final int MASTERY_EXPAND_BUTTON_SIZE = 12;
    private static final int MASTERY_EXPAND_BUTTON_MARGIN = 8;
    private static final int MASTERY_EXPAND_BUTTON_Y = 16;
    private static final int MASTERY_ICON_TEXTURE_SIZE = 16;
    private static final double MASTERY_MIN_ZOOM = 0.5D;
    private static final double MASTERY_MAX_ZOOM = 2.0D;
    private static final double MASTERY_ZOOM_STEP = 1.2D;

    private final PassiveTree<N> tree;
    private final List<N> nodes;
    private final N starter;
    private final Map<N, ResourceLocation> iconTextures;
    private final MasteryMenuView<N> view;
    private final Callbacks<N> callbacks;
    private final Palette palette;
    private final int contentMinX;
    private final int contentMinY;
    private final List<RenderedLink> links;

    private double panX;
    private double panY;
    private double zoom = 1.0D;
    private double compactPanX;
    private double compactPanY;
    private double compactZoom = 1.0D;
    private boolean expanded;
    private boolean dragging;
    private boolean panInitialized;
    private boolean fitRequested;
    private boolean restoreRequested;
    private double lastDragX;
    private double lastDragY;
    private boolean searchFocused;
    private String search = "";
    private boolean highlightRelevant;
    private int searchCursor;
    private MachineProgressionState snapshot = MachineProgressionState.EMPTY;

    public MasteryScreenSupport(
            PassiveTree<N> tree,
            List<N> nodes,
            N starter,
            Map<N, ResourceLocation> iconTextures,
            MasteryMenuView<N> view,
            Callbacks<N> callbacks,
            Palette palette
    ) {
        this.tree = tree;
        this.nodes = List.copyOf(nodes);
        this.starter = starter;
        this.iconTextures = Map.copyOf(iconTextures);
        this.view = view;
        this.callbacks = callbacks;
        this.palette = palette;
        this.contentMinX = nodes.stream().mapToInt(PassiveNode::x).min().orElse(0);
        this.contentMinY = nodes.stream().mapToInt(PassiveNode::y).min().orElse(0);
        List<RenderedLink> renderedLinks = new ArrayList<>();
        for (N node : nodes) {
            for (PassiveNode parent : node.parents()) {
                if (parent.index() < node.index()) {
                    renderedLinks.add(new RenderedLink(parent, node, parent.linkPathTo(node)));
                }
            }
        }
        this.links = List.copyOf(renderedLinks);
    }

    public boolean expanded() {
        return expanded;
    }

    public int imageWidth(int baseImageWidth, int screenWidth) {
        return expanded ? Math.max(baseImageWidth, screenWidth - MASTERY_SCREEN_MARGIN_X) : baseImageWidth;
    }

    public int imageHeight(int baseImageHeight, int screenHeight) {
        return expanded ? Math.max(baseImageHeight, screenHeight - MASTERY_SCREEN_MARGIN_Y) : baseImageHeight;
    }

    public void resetDragging() {
        dragging = false;
    }

    public void clampAfterGeometryChange(int imageWidth, int imageHeight) {
        if (panInitialized) {
            setPan(panX, panY, imageWidth, imageHeight);
        }
    }

    public void render(GuiGraphics guiGraphics, int leftPos, int topPos, int imageWidth, int imageHeight) {
        snapshot = view.masterySnapshot();
        ensurePanInitialized(imageWidth, imageHeight);
        if (fitRequested) {
            fitTree(imageWidth, imageHeight);
            fitRequested = false;
        } else if (restoreRequested) {
            zoom = compactZoom;
            setPan(compactPanX, compactPanY, imageWidth, imageHeight);
            restoreRequested = false;
        }

        int xpBarWidth = xpBarWidth(imageWidth);
        guiGraphics.fill(
                leftPos + MASTERY_XP_BAR_X,
                topPos + MASTERY_XP_BAR_Y,
                leftPos + MASTERY_XP_BAR_X + xpBarWidth,
                topPos + MASTERY_XP_BAR_Y + 7,
                0xFF5F5F5F
        );
        guiGraphics.fill(
                leftPos + MASTERY_XP_BAR_X + 1,
                topPos + MASTERY_XP_BAR_Y + 1,
                leftPos + MASTERY_XP_BAR_X + 1 + Math.round((xpBarWidth - 2) * view.machineXpProgress()),
                topPos + MASTERY_XP_BAR_Y + 6,
                palette.statAccent()
        );
        renderExpandButton(guiGraphics, leftPos, topPos, imageWidth);
        renderToolbar(guiGraphics, leftPos, topPos, imageWidth);

        int viewLeft = leftPos + MASTERY_VIEW_X;
        int viewTop = topPos + MASTERY_VIEW_Y;
        int viewRight = viewLeft + viewWidth(imageWidth);
        int viewBottom = viewTop + viewHeight(imageHeight);
        guiGraphics.fill(viewLeft - 1, viewTop - 1, viewRight + 1, viewBottom + 1, 0xFF655B46);
        guiGraphics.fill(viewLeft, viewTop, viewRight, viewBottom, 0xFF10171D);
        guiGraphics.enableScissor(viewLeft, viewTop, viewRight, viewBottom);
        drawRings(guiGraphics, leftPos, topPos);
        for (RenderedLink link : links) {
            drawLink(guiGraphics, leftPos, topPos, link);
        }
        for (N node : nodes) {
            drawNode(guiGraphics, leftPos, topPos, node);
        }
        guiGraphics.disableScissor();
    }

    public void drawLabels(GuiGraphics guiGraphics, Font font, int imageWidth) {
        Component points = Component.translatable("rngtech.mastery.points", view.unspentPassivePoints());
        guiGraphics.drawString(
                font,
                Component.translatable("rngtech.mastery.level", view.machineLevel()),
                12,
                18,
                palette.text(),
                false
        );
        guiGraphics.drawString(
                font,
                points,
                expandButtonX(imageWidth) - 6 - font.width(points),
                18,
                palette.text(),
                false
        );
    }

    public void renderTooltips(
            GuiGraphics guiGraphics,
            Font font,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int mouseX,
            int mouseY
    ) {
        int tool = toolbarIndex(leftPos, topPos, mouseX, mouseY);
        if (tool >= 0) {
            guiGraphics.renderComponentTooltip(font, List.of(Component.translatable("rngtech.mastery.toolbar." + tool)), mouseX, mouseY);
            return;
        }
        if (isOverExpandButton(leftPos, topPos, imageWidth, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(
                    font,
                    List.of(Component.translatable(expanded
                            ? "rngtech.mastery.tooltip.compact_view"
                            : "rngtech.mastery.tooltip.expand_view")),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (mouseX >= leftPos + MASTERY_XP_BAR_X
                && mouseX < leftPos + MASTERY_XP_BAR_X + xpBarWidth(imageWidth)
                && mouseY >= topPos + MASTERY_XP_BAR_Y
                && mouseY < topPos + MASTERY_XP_BAR_Y + 7) {
            guiGraphics.renderComponentTooltip(font, List.of(xpTooltip()), mouseX, mouseY);
            return;
        }
        N hovered = hoveredNode(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        if (hovered != null) {
            guiGraphics.renderComponentTooltip(font, nodeTooltip(hovered), mouseX, mouseY);
        }
    }

    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight
    ) {
        if (button == 1) {
            N hovered = hoveredNode(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
            if (hovered instanceof MegaPassiveNode node && view.hasPassiveNode(node)) {
                send("refund", node.id()); return true;
            }
            return false;
        }
        if (button != 0) {
            return false;
        }
        int tool = toolbarIndex(leftPos, topPos, mouseX, mouseY);
        if (tool >= 0) {
            switch (tool) {
                case 0 -> {
                    if (Screen.hasShiftDown()) { send("copy_configurator", ""); }
                    else { net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(MasteryBuildCode.copy(view.masterySnapshot()).encode()); }
                }
                case 1 -> {
                    String code = net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
                    if (code.length() <= MasteryBuildCode.MAX_LENGTH) { send("paste", code); }
                }
                case 2 -> send(view.masterySnapshot().following() ? "pause" : "resume", "");
                case 3 -> { if (Screen.hasShiftDown()) { send("clear", ""); } }
                case 4 -> { zoom = 0.7; panInitialized = false; ensurePanInitialized(imageWidth, imageHeight); }
                case 5 -> fitTree(imageWidth, imageHeight);
                case 6 -> highlightRelevant = !highlightRelevant;
                default -> { }
            }
            return true;
        }
        searchFocused = mouseX >= leftPos + 154 && mouseX < leftPos + imageWidth - 8
                && mouseY >= topPos + 42 && mouseY < topPos + 54;
        if (searchFocused) { return true; }
        if (isOverExpandButton(leftPos, topPos, imageWidth, mouseX, mouseY)) {
            toggleExpanded(imageWidth, imageHeight);
            return true;
        }
        N hovered = hoveredNode(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        if (hovered != null
                && view.canUnlockPassiveNode(hovered)
                && callbacks.gearAllowsUnlock(hovered)
                && callbacks.unlock(hovered)) {
            return true;
        }
        if (isOverViewport(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY)) {
            dragging = true;
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            int imageWidth,
            int imageHeight
    ) {
        if (button == 0 && dragging) {
            double nextPanX = panX - (mouseX - lastDragX) / zoom;
            double nextPanY = panY - (mouseY - lastDragY) / zoom;
            setPan(nextPanX, nextPanY, imageWidth, imageHeight);
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight
    ) {
        if (!isOverViewport(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY)) {
            return false;
        }
        double scrollAmount = Math.abs(scrollX) > Math.abs(scrollY) ? scrollX : scrollY;
        if (Screen.hasControlDown()) {
            zoom(scrollAmount, mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight);
            return true;
        }
        double amount = scrollAmount * MASTERY_SCROLL_STEP / zoom;
        if (Screen.hasShiftDown() || Math.abs(scrollX) > Math.abs(scrollY)) {
            setPan(panX - amount, panY, imageWidth, imageHeight);
        } else {
            setPan(panX, panY - amount, imageWidth, imageHeight);
        }
        return true;
    }

    private void renderExpandButton(GuiGraphics guiGraphics, int leftPos, int topPos, int imageWidth) {
        int left = leftPos + expandButtonX(imageWidth);
        int top = topPos + MASTERY_EXPAND_BUTTON_Y;
        int right = left + MASTERY_EXPAND_BUTTON_SIZE;
        int bottom = top + MASTERY_EXPAND_BUTTON_SIZE;
        guiGraphics.fill(left, top, right, bottom, 0xFF5F5F5F);
        guiGraphics.fill(left, top, right, top + 1, palette.panelLight());
        guiGraphics.fill(left, top, left + 1, bottom, palette.panelLight());
        guiGraphics.fill(right - 1, top, right, bottom, palette.panelDark());
        guiGraphics.fill(left, bottom - 1, right, bottom, palette.panelDark());
        int color = expanded ? palette.outputBonus() : palette.statAccent();
        if (expanded) {
            guiGraphics.fill(left + 3, top + 3, right - 3, top + 4, color);
            guiGraphics.fill(left + 3, bottom - 4, right - 3, bottom - 3, color);
            guiGraphics.fill(left + 3, top + 3, left + 4, bottom - 3, color);
            guiGraphics.fill(right - 4, top + 3, right - 3, bottom - 3, color);
        } else {
            guiGraphics.fill(left + 2, top + 2, left + 6, top + 3, color);
            guiGraphics.fill(left + 2, top + 2, left + 3, top + 6, color);
            guiGraphics.fill(right - 6, top + 2, right - 2, top + 3, color);
            guiGraphics.fill(right - 3, top + 2, right - 2, top + 6, color);
            guiGraphics.fill(left + 2, bottom - 3, left + 6, bottom - 2, color);
            guiGraphics.fill(left + 2, bottom - 6, left + 3, bottom - 2, color);
            guiGraphics.fill(right - 6, bottom - 3, right - 2, bottom - 2, color);
            guiGraphics.fill(right - 3, bottom - 6, right - 2, bottom - 2, color);
        }
    }

    private void drawRings(GuiGraphics guiGraphics, int leftPos, int topPos) {
        int centerX = nodeCenterX(starter, leftPos);
        int centerY = nodeCenterY(starter, topPos);
        int outerRadius = Math.max(tree.contentMaxX(), tree.contentMaxY()) * 2 / 3;
        for (int radius = 120; radius <= outerRadius; radius += 120) {
            drawCircle(guiGraphics, centerX, centerY, scaledLength(radius), 0xFF282B28);
            drawCircle(guiGraphics, centerX, centerY, scaledLength(radius + 5), 0xFF192126);
        }
    }

    private void drawLink(GuiGraphics guiGraphics, int leftPos, int topPos, RenderedLink link) {
        boolean fromUnlocked = view.hasPassiveNode(link.from());
        boolean toUnlocked = view.hasPassiveNode(link.to());
        boolean active = fromUnlocked && toUnlocked;
        boolean available = fromUnlocked != toUnlocked;
        int color = active ? palette.statAccent() : available ? palette.outputBonus() : 0xFF82745A;
        for (int index = 1; index < link.path().size(); index++) {
            PassiveTreeLayouts.Point from = link.path().get(index - 1);
            PassiveTreeLayouts.Point to = link.path().get(index);
            drawLine(
                    guiGraphics,
                    contentToScreenX(from.x(), leftPos),
                    contentToScreenY(from.y(), topPos),
                    contentToScreenX(to.x(), leftPos),
                    contentToScreenY(to.y(), topPos),
                    color,
                    Math.max(1, scaledLength(active ? 2 : 1))
            );
        }
    }

    private void drawNode(GuiGraphics guiGraphics, int leftPos, int topPos, N node) {
        boolean unlocked = view.hasPassiveNode(node);
        boolean unlockable = view.canUnlockPassiveNode(node) && callbacks.gearAllowsUnlock(node);
        int border = node.alwaysAllocated()
                ? palette.startNode()
                : unlocked ? palette.statAccent() : unlockable ? palette.outputBonus() : 0xFF9D885F;
        int fill = nodeFill(node, unlocked, unlockable);
        if (node instanceof MegaPassiveNode shared) {
            if (snapshot.targetNodes().contains(shared.id()) && !unlocked) { border = 0xFF8E9BE8; }
            if (!search.isBlank() && matchesSearch(shared) || highlightRelevant && relevant(shared)) { border = 0xFFFFFF80; }
        }
        int centerX = nodeCenterX(node, leftPos);
        int centerY = nodeCenterY(node, topPos);
        int radius = Math.max(1, scaledLength(node.size() / 2.0D));
        if (node.kind() == PassiveNodeKind.KEYSTONE) {
            drawDiamond(guiGraphics, centerX, centerY, radius, border);
            radius = Math.max(3, radius - scaledLength(2));
        }
        fillCircle(guiGraphics, centerX, centerY, radius, 0xFF080D11);
        fillCircle(guiGraphics, centerX, centerY, radius - 1, border);
        fillCircle(guiGraphics, centerX, centerY, radius - 2, fill);
        if (node.kind() == PassiveNodeKind.NOTABLE || node.kind() == PassiveNodeKind.KEYSTONE || node.alwaysAllocated()) {
            int innerRadius = radius - Math.max(3, scaledLength(4));
            if (innerRadius > 1) {
                drawCircle(guiGraphics, centerX, centerY, innerRadius,
                        unlocked ? palette.statAccent() : node.kind() == PassiveNodeKind.KEYSTONE ? 0xFF9275B0 : 0xFF766D52);
            }
        }
        drawIcon(guiGraphics, node, centerX, centerY);
    }

    private void drawIcon(GuiGraphics guiGraphics, N node, int centerX, int centerY) {
        ResourceLocation texture = iconTextures.get(node);
        if (texture == null) {
            return;
        }
        int iconSize = Math.max(2, Math.min(MASTERY_ICON_TEXTURE_SIZE, scaledLength(node.size() * 0.6D)));
        int iconX = centerX - iconSize / 2;
        int iconY = centerY - iconSize / 2;
        guiGraphics.blit(
                texture,
                iconX,
                iconY,
                iconSize,
                iconSize,
                0.0F,
                0.0F,
                MASTERY_ICON_TEXTURE_SIZE,
                MASTERY_ICON_TEXTURE_SIZE,
                MASTERY_ICON_TEXTURE_SIZE,
                MASTERY_ICON_TEXTURE_SIZE
        );
    }

    private int nodeFill(PassiveNode node, boolean unlocked, boolean unlockable) {
        if (node.alwaysAllocated()) {
            return 0xFF54421E;
        }
        if (unlocked) {
            return switch (node.kind()) {
                case STARTER -> 0xFF54421E;
                case KEYSTONE -> 0xFF49315C;
                case NOTABLE -> 0xFF27434C;
                case NODE -> 0xFF263C3E;
                case TRAVEL -> 0xFF30382D;
            };
        }
        if (unlockable) {
            return 0xFF454024;
        }
        return switch (node.kind()) {
            case STARTER -> 0xFF54421E;
            case TRAVEL -> 0xFF1D2526;
            case KEYSTONE -> 0xFF291E35;
            case NOTABLE -> 0xFF1C3037;
            case NODE -> 0xFF202A2D;
        };
    }

    private Component xpTooltip() {
        if (view.machineLevel() >= MachineProgressionState.MAX_LEVEL) {
            return Component.translatable("rngtech.mastery.tooltip.xp_max", view.machineXp());
        }
        return Component.translatable(
                "rngtech.mastery.tooltip.xp",
                view.machineXpInLevel(),
                view.machineXpToNextLevel(),
                view.machineXp()
        );
    }

    private List<Component> nodeTooltip(N node) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(node.translationKey()).withStyle(ChatFormatting.WHITE));
        if (node.kind() == PassiveNodeKind.KEYSTONE) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.keystone").withStyle(ChatFormatting.GOLD));
        } else if (node.kind() == PassiveNodeKind.NOTABLE) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.notable").withStyle(ChatFormatting.AQUA));
        }
        for (var effect : node.effects()) {
            Component text = MachineStatDisplay.effectText(effect);
            tooltip.add(view.masterySupports(effect.stat()) ? text.copy().withStyle(ChatFormatting.GRAY)
                    : Component.translatable("rngtech.mastery.inactive", text).withStyle(ChatFormatting.DARK_GRAY));
        }
        for (PassiveNodeFlag flag : node.flags()) {
            tooltip.add(Component.translatable(flagTranslationKey(flag)).withStyle(ChatFormatting.GOLD));
        }
        for (PassiveStatType stat : PassiveStatType.values()) {
            int value = node.passiveStat(stat);
            if (value != 0) {
                tooltip.add(applicability(Component.translatable(passiveStatTranslationKey(stat), signed(value)), view.masteryFamily().supports(stat)));
            }
        }
        if (node instanceof MegaPassiveNode shared) {
            shared.fixed().forEach((stat, value) -> tooltip.add(applicability(Component.translatable("rngtech.mastery.fixed", Component.translatable(stat.translationKey()), MachineStatDisplay.formatNumber(value)), view.masterySupports(stat))));
            shared.ceilings().forEach((stat, value) -> tooltip.add(applicability(Component.translatable("rngtech.mastery.ceiling", Component.translatable(stat.translationKey()), MachineStatDisplay.formatNumber(value)), view.masterySupports(stat))));
            for (var scaling : shared.scaling()) {
                tooltip.add(applicability(Component.translatable("rngtech.mastery.scaling", MachineStatDisplay.formatNumber(scaling.perPoint()),
                        Component.translatable("rngtech.mastery.operation." + scaling.operation().name().toLowerCase(Locale.ROOT)),
                        Component.translatable(scaling.stat().translationKey()), Component.translatable(scaling.attribute().translationKey())), view.masterySupports(scaling.stat())));
            }
            for (String behavior : shared.behaviors()) {
                if (!behavior.equals("MUTE_MACHINE_SOUND")) {
                    tooltip.add(applicability(Component.translatable("rngtech.mastery.behavior." + behavior.toLowerCase(Locale.ROOT)), view.masterySupportsBehavior(behavior)));
                }
            }
            if (shared.recipeHardnessCeiling() > 0) {
                tooltip.add(applicability(Component.translatable("rngtech.mastery.hardness_ceiling", shared.recipeHardnessCeiling()), view.masteryFamily() == MachineMasteryFamily.CRUSHER));
            }
            if (shared.kind() == PassiveNodeKind.STARTER) {
                if (shared.id().equals(view.masteryFamily().startNodeId())) {
                    for (var attribute : List.of(MachineStat.CONTROL, MachineStat.DRIVE, MachineStat.RESERVE)) {
                        tooltip.add(Component.translatable("rngtech.mastery.attribute_total", Component.translatable(attribute.translationKey()), MachineStatDisplay.formatNumber(view.masteryAttribute(attribute))).withStyle(ChatFormatting.AQUA));
                    }
                    tooltip.add(Component.translatable("rngtech.mastery.tooltip.unlocked").withStyle(ChatFormatting.GREEN));
                } else {
                    tooltip.add(Component.translatable("rngtech.mastery.other_start").withStyle(ChatFormatting.DARK_GRAY));
                }
                return tooltip;
            }
            if (view.hasPassiveNode(node) && node.kind() != PassiveNodeKind.STARTER) {
                tooltip.add(Component.translatable("rngtech.mastery.refund_hint").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else { callbacks.appendSpecialTooltip(node, tooltip); }
        if (node.alwaysAllocated()) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.unlocked").withStyle(ChatFormatting.GREEN));
            return tooltip;
        }
        if (node.requiredLevel() > MachineProgressionState.MIN_LEVEL) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.required_level", node.requiredLevel())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (view.hasPassiveNode(node)) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.unlocked").withStyle(ChatFormatting.GREEN));
        } else if (view.machineLevel() < node.requiredLevel()) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.locked").withStyle(ChatFormatting.RED));
        } else if (!view.hasUnlockedPassiveConnection(node)) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.requires_link").withStyle(ChatFormatting.RED));
        } else if (!view.canUnlockPassiveNode(node)) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.locked").withStyle(ChatFormatting.RED));
        } else if (!callbacks.gearAllowsUnlock(node)) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.gear_conflict").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.click_unlock").withStyle(ChatFormatting.YELLOW));
        }
        return tooltip;
    }

    private N hoveredNode(int leftPos, int topPos, int imageWidth, int imageHeight, double mouseX, double mouseY) {
        if (!isOverViewport(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY)) {
            return null;
        }
        for (N node : nodes) {
            int radius = Math.max(3, scaledLength(node.size() / 2.0D));
            if (Math.abs(mouseX - nodeCenterX(node, leftPos)) <= radius
                    && Math.abs(mouseY - nodeCenterY(node, topPos)) <= radius) {
                return node;
            }
        }
        return null;
    }

    private void renderToolbar(GuiGraphics graphics, int left, int top, int width) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        String[] labels = {"C", "P", snapshot.following() ? "||" : ">", "X", "H", "F", "R"};
        for (int i = 0; i < labels.length; i++) {
            int x = left + 8 + i * 20;
            graphics.fill(x, top + 42, x + 18, top + 54, i == 6 && highlightRelevant ? 0xFF596035 : 0xFF343E49);
            graphics.drawString(font, labels[i], x + 5, top + 44, 0xFFE7DDC0, false);
        }
        graphics.fill(left + 154, top + 42, left + width - 8, top + 54, searchFocused ? 0xFF465366 : 0xFF252E38);
        String text = search.isEmpty() ? Component.translatable("rngtech.mastery.search").getString() : search;
        graphics.drawString(font, font.plainSubstrByWidth(text, Math.max(1, width - 168)), left + 157, top + 44, 0xFFE7DDC0, false);
    }

    private int toolbarIndex(int left, int top, double x, double y) {
        if (y < top + 42 || y >= top + 54 || x < left + 8 || x >= left + 148) { return -1; }
        return (int) (x - left - 8) / 20;
    }

    private void send(String action, String value) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new MasteryActionPayload(player.containerMenu.containerId, action, value));
        }
    }

    private boolean matchesSearch(MegaPassiveNode node) {
        String term = search.toLowerCase(Locale.ROOT);
        return Component.translatable(node.translationKey()).getString().toLowerCase(Locale.ROOT).contains(term)
                || node.id().contains(term.replace(' ', '_'))
                || node.effects().stream().anyMatch(effect -> MachineStatDisplay.effectText(effect).getString().toLowerCase(Locale.ROOT).contains(term));
    }

    private static Component applicability(Component text, boolean applies) {
        return applies ? text.copy().withStyle(ChatFormatting.GOLD) : Component.translatable("rngtech.mastery.inactive", text).withStyle(ChatFormatting.DARK_GRAY);
    }

    private boolean relevant(MegaPassiveNode node) {
        return node.effects().stream().anyMatch(effect -> view.masterySupports(effect.stat()))
                || node.scaling().stream().anyMatch(effect -> view.masterySupports(effect.stat()))
                || node.fixed().keySet().stream().anyMatch(view::masterySupports)
                || node.ceilings().keySet().stream().anyMatch(view::masterySupports)
                || node.behaviors().stream().anyMatch(view::masterySupportsBehavior)
                || node.passive().keySet().stream().anyMatch(view.masteryFamily()::supports)
                || node.recipeHardnessCeiling() > 0 && view.masteryFamily() == MachineMasteryFamily.CRUSHER;
    }

    public boolean charTyped(char character) {
        if (!searchFocused) { return false; }
        if (character >= 32 && character != 127 && search.length() < 64) { search += character; searchCursor = 0; }
        return true;
    }

    public boolean keyPressed(int key, int scan, int modifiers, int width, int height) {
        if (key == 70 && Screen.hasControlDown()) { searchFocused = true; return true; }
        if (!searchFocused) { return false; }
        if (key == 256) { searchFocused = false; return true; }
        if (key == 259 && !search.isEmpty()) { search = search.substring(0, search.length() - 1); searchCursor = 0; }
        if (key == 257 && !search.isBlank()) {
            List<N> matches = nodes.stream().filter(node -> node instanceof MegaPassiveNode shared && matchesSearch(shared)).toList();
            if (!matches.isEmpty()) {
                N node = matches.get(Math.floorMod(searchCursor++, matches.size()));
                zoom = 0.7;
                setPan(node.x() + node.size() / 2.0 - (MASTERY_VIEW_X + viewWidth(width) / 2.0) / zoom,
                        node.y() + node.size() / 2.0 - (MASTERY_VIEW_Y + viewHeight(height) / 2.0) / zoom, width, height);
            }
        }
        return true;
    }

    private boolean isOverViewport(
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            double mouseX,
            double mouseY
    ) {
        return mouseX >= leftPos + MASTERY_VIEW_X
                && mouseX < leftPos + viewRight(imageWidth)
                && mouseY >= topPos + MASTERY_VIEW_Y
                && mouseY < topPos + viewBottom(imageHeight);
    }

    private boolean isOverExpandButton(int leftPos, int topPos, int imageWidth, double mouseX, double mouseY) {
        int left = leftPos + expandButtonX(imageWidth);
        int top = topPos + MASTERY_EXPAND_BUTTON_Y;
        return mouseX >= left
                && mouseX < left + MASTERY_EXPAND_BUTTON_SIZE
                && mouseY >= top
                && mouseY < top + MASTERY_EXPAND_BUTTON_SIZE;
    }

    private void toggleExpanded(int imageWidth, int imageHeight) {
        if (!expanded) {
            compactPanX = panX;
            compactPanY = panY;
            compactZoom = zoom;
        }
        expanded = !expanded;
        dragging = false;
        fitRequested = expanded;
        restoreRequested = !expanded;
        callbacks.geometryChanged();
        clampAfterGeometryChange(imageWidth, imageHeight);
    }

    private void setPan(double nextPanX, double nextPanY, int imageWidth, int imageHeight) {
        double minPanX = contentMinX - MASTERY_CONTENT_PADDING - MASTERY_VIEW_X / zoom;
        double maxPanX = tree.contentMaxX() + MASTERY_CONTENT_PADDING - viewRight(imageWidth) / zoom;
        double minPanY = contentMinY - MASTERY_CONTENT_PADDING - MASTERY_VIEW_Y / zoom;
        double maxPanY = tree.contentMaxY() + MASTERY_CONTENT_PADDING - viewBottom(imageHeight) / zoom;
        panX = minPanX > maxPanX ? (minPanX + maxPanX) / 2.0D : Mth.clamp(nextPanX, minPanX, maxPanX);
        panY = minPanY > maxPanY ? (minPanY + maxPanY) / 2.0D : Mth.clamp(nextPanY, minPanY, maxPanY);
    }

    private void zoom(
            double amount,
            double mouseX,
            double mouseY,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight
    ) {
        if (amount == 0.0D) {
            return;
        }
        double anchorX = screenToContentX(mouseX, leftPos);
        double anchorY = screenToContentY(mouseY, topPos);
        double factor = Math.pow(MASTERY_ZOOM_STEP, amount);
        double nextZoom = Mth.clamp(zoom * factor, Math.min(MASTERY_MIN_ZOOM, fitZoom(imageWidth, imageHeight)), MASTERY_MAX_ZOOM);
        if (nextZoom == zoom) {
            return;
        }
        zoom = nextZoom;
        setPan(anchorX - (mouseX - leftPos) / zoom, anchorY - (mouseY - topPos) / zoom, imageWidth, imageHeight);
    }

    private void ensurePanInitialized(int imageWidth, int imageHeight) {
        if (panInitialized) {
            return;
        }
        panInitialized = true;
        setPan(
                starter.x() + starter.size() / 2.0D - (MASTERY_VIEW_X + viewWidth(imageWidth) / 2.0D) / zoom,
                starter.y() + starter.size() / 2.0D - (MASTERY_VIEW_Y + viewHeight(imageHeight) / 2.0D) / zoom,
                imageWidth,
                imageHeight
        );
    }

    private double fitZoom(int imageWidth, int imageHeight) {
        return Math.min(1.0D, Math.min(
                (double) viewWidth(imageWidth) / (tree.contentMaxX() - contentMinX + MASTERY_CONTENT_PADDING * 2),
                (double) viewHeight(imageHeight) / (tree.contentMaxY() - contentMinY + MASTERY_CONTENT_PADDING * 2)
        ));
    }

    private void fitTree(int imageWidth, int imageHeight) {
        zoom = fitZoom(imageWidth, imageHeight);
        setPan(
                (contentMinX + tree.contentMaxX()) / 2.0D - (MASTERY_VIEW_X + viewWidth(imageWidth) / 2.0D) / zoom,
                (contentMinY + tree.contentMaxY()) / 2.0D - (MASTERY_VIEW_Y + viewHeight(imageHeight) / 2.0D) / zoom,
                imageWidth,
                imageHeight
        );
    }

    private int nodeCenterX(PassiveNode node, int leftPos) {
        return contentToScreenX(node.x() + node.size() / 2.0D, leftPos);
    }

    private int nodeCenterY(PassiveNode node, int topPos) {
        return contentToScreenY(node.y() + node.size() / 2.0D, topPos);
    }

    private int contentToScreenX(double contentX, int leftPos) {
        return leftPos + (int) Math.round((contentX - panX) * zoom);
    }

    private int contentToScreenY(double contentY, int topPos) {
        return topPos + (int) Math.round((contentY - panY) * zoom);
    }

    private double screenToContentX(double screenX, int leftPos) {
        return panX + (screenX - leftPos) / zoom;
    }

    private double screenToContentY(double screenY, int topPos) {
        return panY + (screenY - topPos) / zoom;
    }

    private int scaledLength(double length) {
        return (int) Math.round(length * zoom);
    }

    private int viewWidth(int imageWidth) {
        return Math.max(MASTERY_VIEW_WIDTH, imageWidth - MASTERY_VIEW_X * 2);
    }

    private int viewHeight(int imageHeight) {
        return Math.max(MASTERY_VIEW_HEIGHT, imageHeight - MASTERY_VIEW_Y - MASTERY_VIEW_BOTTOM_PADDING);
    }

    private int viewRight(int imageWidth) {
        return MASTERY_VIEW_X + viewWidth(imageWidth);
    }

    private int viewBottom(int imageHeight) {
        return MASTERY_VIEW_Y + viewHeight(imageHeight);
    }

    private int xpBarWidth(int imageWidth) {
        return Math.max(MASTERY_XP_BAR_WIDTH, imageWidth - MASTERY_XP_BAR_X * 2);
    }

    private int expandButtonX(int imageWidth) {
        return imageWidth - MASTERY_EXPAND_BUTTON_MARGIN - MASTERY_EXPAND_BUTTON_SIZE;
    }

    private void drawLine(
            GuiGraphics guiGraphics,
            int fromX,
            int fromY,
            int toX,
            int toY,
            int color,
            int thickness
    ) {
        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);
        int stepX = fromX < toX ? 1 : -1;
        int stepY = fromY < toY ? 1 : -1;
        int error = dx - dy;
        int x = fromX;
        int y = fromY;
        while (true) {
            guiGraphics.fill(x, y, x + thickness, y + thickness, color);
            if (x == toX && y == toY) {
                return;
            }
            int doubledError = error * 2;
            if (doubledError > -dy) {
                error -= dy;
                x += stepX;
            }
            if (doubledError < dx) {
                error += dx;
                y += stepY;
            }
        }
    }

    private void drawDiamond(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        if (radius <= 0) {
            return;
        }
        for (int offsetY = -radius; offsetY <= radius; offsetY++) {
            int width = radius - Math.abs(offsetY);
            guiGraphics.fill(centerX - width, centerY + offsetY, centerX + width + 1, centerY + offsetY + 1, color);
        }
    }

    private void drawCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        int x = radius;
        int y = 0;
        int error = 0;
        while (x >= y) {
            plotCirclePoints(guiGraphics, centerX, centerY, x, y, color);
            y++;
            if (error <= 0) {
                error += 2 * y + 1;
            }
            if (error > 0) {
                x--;
                error -= 2 * x + 1;
            }
        }
    }

    private void fillCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        if (radius < 0) {
            return;
        }
        for (int offsetY = -radius; offsetY <= radius; offsetY++) {
            int halfWidth = (int) Math.sqrt(radius * radius - offsetY * offsetY);
            guiGraphics.fill(centerX - halfWidth, centerY + offsetY, centerX + halfWidth + 1, centerY + offsetY + 1, color);
        }
    }

    private void plotCirclePoints(GuiGraphics guiGraphics, int centerX, int centerY, int x, int y, int color) {
        plotPixel(guiGraphics, centerX + x, centerY + y, color);
        plotPixel(guiGraphics, centerX + y, centerY + x, color);
        plotPixel(guiGraphics, centerX - y, centerY + x, color);
        plotPixel(guiGraphics, centerX - x, centerY + y, color);
        plotPixel(guiGraphics, centerX - x, centerY - y, color);
        plotPixel(guiGraphics, centerX - y, centerY - x, color);
        plotPixel(guiGraphics, centerX + y, centerY - x, color);
        plotPixel(guiGraphics, centerX + x, centerY - y, color);
    }

    private void plotPixel(GuiGraphics guiGraphics, int x, int y, int color) {
        guiGraphics.fill(x, y, x + 1, y + 1, color);
    }

    private static String flagTranslationKey(PassiveNodeFlag flag) {
        return "rngtech.mastery.flag." + flag.name().toLowerCase(Locale.ROOT);
    }

    private static String passiveStatTranslationKey(PassiveStatType stat) {
        return "rngtech.mastery.passive_stat." + stat.name().toLowerCase(Locale.ROOT);
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private record RenderedLink(PassiveNode from, PassiveNode to, List<PassiveTreeLayouts.Point> path) {
    }

    public record Palette(
            int panelDark,
            int panelLight,
            int text,
            int statAccent,
            int outputBonus,
            int startNode,
            int masteryRing
    ) {
    }

    public interface Callbacks<N extends PassiveNode> {
        boolean gearAllowsUnlock(N node);

        boolean unlock(N node);

        default void appendSpecialTooltip(N node, List<Component> tooltip) {
        }

        default void geometryChanged() {
        }
    }
}
