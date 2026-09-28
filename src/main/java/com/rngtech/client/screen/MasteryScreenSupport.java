package com.rngtech.client.screen;

import com.rngtech.content.menu.MasteryMenuView;
import com.rngtech.content.network.MasteryActionPayload;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatDisplay;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MasteryBuildCode;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveNode;
import com.rngtech.rpg.progression.PassiveNodeFlag;
import com.rngtech.rpg.progression.PassiveNodeKind;
import com.rngtech.rpg.progression.PassiveStatType;
import com.rngtech.rpg.progression.PassiveTree;
import com.rngtech.rpg.progression.PassiveTreeLayouts;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

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
    private static final int MASTERY_RING_SPACING = 120;
    private static final int MASTERY_SEARCH_X = 154;
    private static final double MASTERY_MIN_ZOOM = 0.5D;
    private static final double MASTERY_MAX_ZOOM = 2.0D;
    private static final double MASTERY_ZOOM_STEP = 1.25D;
    private static final double MASTERY_FOCUS_ZOOM = 0.7D;
    private static final double MASTERY_DRAG_THRESHOLD = 3.0D;
    private static final float MASTERY_MIN_HIT_RADIUS = 4.0F;
    private static final float MASTERY_MIN_ICON_SIZE = 5.0F;
    private static final float MASTERY_DETAIL_RADIUS = 2.5F;
    private static final long MASTERY_PENDING_TIMEOUT_MS = 1500L;
    private static final int ROUTE_READY = 0xFFF2E6C2;
    private static final int ROUTE_BLOCKED = 0xFFC8604E;
    private static final int SEARCH_HIGHLIGHT = 0xFFFFFF80;
    private static final int TARGET_BORDER = 0xFF8E9BE8;
    private static final int[] NO_ROUTE = new int[0];

    private final List<N> nodes;
    private final N starter;
    private final MasteryMenuView<N> view;
    private final Callbacks<N> callbacks;
    private final Palette palette;
    private final int contentMinX;
    private final int contentMinY;
    private final int contentMaxX;
    private final int contentMaxY;
    private final int[] positionByIndex;
    private final float[] centerX;
    private final float[] centerY;
    private final float[] radius;
    private final int[][] neighbors;
    private final float[] linkSegments;
    private final int[] linkFrom;
    private final int[] linkTo;
    private final List<ResourceLocation> iconTextures;
    private final int[][] iconPositions;

    private final BitSet allocated = new BitSet();
    private final BitSet pending = new BitSet();
    private final BitSet unlocked = new BitSet();
    private final BitSet unlockable = new BitSet();
    private final BitSet targets = new BitSet();
    private final BitSet searchMatches = new BitSet();
    private final BitSet scratch = new BitSet();
    private BitSet relevant;
    private String[] searchText;
    private List<String> targetIds = List.of();
    private String matchedSearch = "";
    private int matchCount;
    private long pendingUntil;
    private int stateVersion;
    private int routeTarget = -1;
    private int routeVersion = -1;
    private int[] route = NO_ROUTE;

    private double panX;
    private double panY;
    private double zoom = 1.0D;
    private double compactPanX;
    private double compactPanY;
    private double compactZoom = 1.0D;
    private boolean expanded;
    private boolean dragging;
    private boolean dragMoved;
    private boolean panInitialized;
    private boolean fitRequested;
    private boolean restoreRequested;
    private double pressX;
    private double pressY;
    private int pressedNode = -1;
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
        this.nodes = List.copyOf(nodes);
        this.starter = starter;
        this.view = view;
        this.callbacks = callbacks;
        this.palette = palette;
        this.contentMinX = nodes.stream().mapToInt(PassiveNode::x).min().orElse(0);
        this.contentMinY = nodes.stream().mapToInt(PassiveNode::y).min().orElse(0);
        this.contentMaxX = tree.contentMaxX();
        this.contentMaxY = tree.contentMaxY();

        int count = this.nodes.size();
        this.positionByIndex = new int[this.nodes.stream().mapToInt(PassiveNode::index).max().orElse(-1) + 1];
        Arrays.fill(positionByIndex, -1);
        this.centerX = new float[count];
        this.centerY = new float[count];
        this.radius = new float[count];
        for (int position = 0; position < count; position++) {
            N node = this.nodes.get(position);
            positionByIndex[node.index()] = position;
            centerX[position] = node.x() + node.size() / 2.0F;
            centerY[position] = node.y() + node.size() / 2.0F;
            radius[position] = node.size() / 2.0F;
        }

        this.neighbors = new int[count][];
        List<float[]> segments = new ArrayList<>();
        List<int[]> segmentEnds = new ArrayList<>();
        for (int position = 0; position < count; position++) {
            N node = this.nodes.get(position);
            neighbors[position] = node.parents().stream().mapToInt(this::position).filter(parent -> parent >= 0).toArray();
            for (PassiveNode parent : node.parents()) {
                int parentPosition = position(parent);
                if (parentPosition < 0 || parent.index() >= node.index()) {
                    continue;
                }
                List<PassiveTreeLayouts.Point> path = parent.linkPathTo(node);
                for (int index = 1; index < path.size(); index++) {
                    PassiveTreeLayouts.Point from = path.get(index - 1);
                    PassiveTreeLayouts.Point to = path.get(index);
                    segments.add(new float[] {from.x(), from.y(), to.x(), to.y()});
                    segmentEnds.add(new int[] {parentPosition, position});
                }
            }
        }
        this.linkSegments = new float[segments.size() * 4];
        this.linkFrom = new int[segments.size()];
        this.linkTo = new int[segments.size()];
        for (int index = 0; index < segments.size(); index++) {
            System.arraycopy(segments.get(index), 0, linkSegments, index * 4, 4);
            linkFrom[index] = segmentEnds.get(index)[0];
            linkTo[index] = segmentEnds.get(index)[1];
        }

        Map<ResourceLocation, List<Integer>> iconGroups = new LinkedHashMap<>();
        for (int position = 0; position < count; position++) {
            ResourceLocation texture = iconTextures.get(this.nodes.get(position));
            if (texture != null) {
                iconGroups.computeIfAbsent(texture, ignored -> new ArrayList<>()).add(position);
            }
        }
        this.iconTextures = List.copyOf(iconGroups.keySet());
        this.iconPositions = iconGroups.values().stream()
                .map(positions -> positions.stream().mapToInt(Integer::intValue).toArray())
                .toArray(int[][]::new);
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
        dragMoved = false;
        pressedNode = -1;
    }

    public void clampAfterGeometryChange(int imageWidth, int imageHeight) {
        if (panInitialized) {
            setPan(panX, panY, imageWidth, imageHeight);
        }
    }

    public void render(GuiGraphics guiGraphics, int leftPos, int topPos, int imageWidth, int imageHeight, int mouseX, int mouseY) {
        refreshState();
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

        int hovered = panning() ? -1 : hoveredNode(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        int[] hoveredRoute = hovered >= 0 ? routeTo(hovered) : NO_ROUTE;
        guiGraphics.enableScissor(viewLeft, viewTop, viewRight, viewBottom);
        GuiShapeBatch shapes = new GuiShapeBatch(guiGraphics, viewLeft, viewTop, viewRight, viewBottom);
        drawRings(shapes, leftPos, topPos);
        drawLinks(shapes, leftPos, topPos);
        drawRoute(shapes, leftPos, topPos, hoveredRoute);
        for (int position = 0; position < nodes.size(); position++) {
            drawNode(shapes, leftPos, topPos, position);
        }
        drawHighlights(shapes, leftPos, topPos, hovered, hoveredRoute);
        shapes.flush();
        drawIcons(guiGraphics, leftPos, topPos, viewLeft, viewTop, viewRight, viewBottom);
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
        if (!searchFocused && isOverSearch(leftPos, topPos, imageWidth, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(Component.translatable("rngtech.mastery.tooltip.search")), mouseX, mouseY);
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
        int hovered = panning() ? -1 : hoveredNode(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        if (hovered >= 0) {
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
            int hovered = hoveredNode(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
            if (hovered >= 0
                    && allocated.get(hovered)
                    && nodes.get(hovered) instanceof MegaPassiveNode node
                    && node.kind() != PassiveNodeKind.STARTER) {
                send("refund", node.id());
                return true;
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
                    else { Minecraft.getInstance().keyboardHandler.setClipboard(MasteryBuildCode.copy(view.masterySnapshot()).encode()); }
                }
                case 1 -> {
                    String code = Minecraft.getInstance().keyboardHandler.getClipboard();
                    if (code.length() <= MasteryBuildCode.MAX_LENGTH) { send("paste", code); }
                }
                case 2 -> send(view.masterySnapshot().following() ? "pause" : "resume", "");
                case 3 -> { if (Screen.hasShiftDown()) { send("clear", ""); } }
                case 4 -> { zoom = MASTERY_FOCUS_ZOOM; panInitialized = false; ensurePanInitialized(imageWidth, imageHeight); }
                case 5 -> fitTree(imageWidth, imageHeight);
                case 6 -> highlightRelevant = !highlightRelevant;
                default -> { }
            }
            return true;
        }
        searchFocused = isOverSearch(leftPos, topPos, imageWidth, mouseX, mouseY);
        if (searchFocused) { return true; }
        if (isOverExpandButton(leftPos, topPos, imageWidth, mouseX, mouseY)) {
            toggleExpanded(imageWidth, imageHeight);
            return true;
        }
        if (isOverViewport(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY)) {
            // Allocation waits for release so a drag that starts on a node only pans.
            dragging = true;
            dragMoved = false;
            pressX = mouseX;
            pressY = mouseY;
            lastDragX = mouseX;
            lastDragY = mouseY;
            pressedNode = hoveredNode(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
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
        if (button != 0 || !dragging) {
            return false;
        }
        if (!dragMoved) {
            double dx = mouseX - pressX;
            double dy = mouseY - pressY;
            if (dx * dx + dy * dy < MASTERY_DRAG_THRESHOLD * MASTERY_DRAG_THRESHOLD) {
                return true;
            }
            dragMoved = true;
        }
        setPan(panX - (mouseX - lastDragX) / zoom, panY - (mouseY - lastDragY) / zoom, imageWidth, imageHeight);
        lastDragX = mouseX;
        lastDragY = mouseY;
        return true;
    }

    public boolean mouseReleased(
            double mouseX,
            double mouseY,
            int button,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight
    ) {
        if (button != 0 || !dragging) {
            return false;
        }
        boolean click = !dragMoved;
        int pressed = pressedNode;
        resetDragging();
        if (click && pressed >= 0 && pressed == hoveredNode(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY)) {
            allocate(pressed);
        }
        return true;
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
        boolean horizontal = Math.abs(scrollX) > Math.abs(scrollY);
        double scrollAmount = horizontal ? scrollX : scrollY;
        double amount = scrollAmount * MASTERY_SCROLL_STEP / zoom;
        if (horizontal || Screen.hasShiftDown()) {
            setPan(panX - amount, panY, imageWidth, imageHeight);
        } else if (Screen.hasControlDown()) {
            setPan(panX, panY - amount, imageWidth, imageHeight);
        } else {
            zoom(scrollAmount, mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight);
        }
        return true;
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
        if (key == 257 || key == 335) {
            refreshSearch();
            if (matchCount > 0) {
                int match = searchMatches.nextSetBit(0);
                for (int skip = Math.floorMod(searchCursor++, matchCount); skip > 0; skip--) {
                    match = searchMatches.nextSetBit(match + 1);
                }
                zoom = Math.max(zoom, MASTERY_FOCUS_ZOOM);
                centerOn(match, width, height);
            }
        }
        return true;
    }

    private void refreshState() {
        snapshot = view.masterySnapshot();
        scratch.clear();
        for (int position = 0; position < nodes.size(); position++) {
            if (view.hasPassiveNode(nodes.get(position))) {
                scratch.set(position);
            }
        }
        boolean changed = false;
        if (!scratch.equals(allocated)) {
            allocated.clear();
            allocated.or(scratch);
            pending.andNot(allocated);
            changed = true;
        }
        if (!pending.isEmpty() && Util.getMillis() > pendingUntil) {
            pending.clear();
            changed = true;
        }
        if (changed) {
            unlocked.clear();
            unlocked.or(allocated);
            unlocked.or(pending);
            stateVersion++;
        }
        if (!snapshot.targetNodes().equals(targetIds)) {
            targetIds = snapshot.targetNodes();
            targets.clear();
            for (String id : targetIds) {
                int position = position(MegaPassiveTree.node(id));
                if (position >= 0) {
                    targets.set(position);
                }
            }
        }
        refreshUnlockable();
        refreshSearch();
    }

    private void refreshUnlockable() {
        unlockable.clear();
        if (availablePoints() <= 0) {
            return;
        }
        for (int position = unlocked.nextSetBit(0); position >= 0; position = unlocked.nextSetBit(position + 1)) {
            for (int neighbor : neighbors[position]) {
                if (!unlocked.get(neighbor) && !unlockable.get(neighbor) && allocatable(neighbor)) {
                    unlockable.set(neighbor);
                }
            }
        }
    }

    private void refreshSearch() {
        if (search.equals(matchedSearch)) {
            return;
        }
        matchedSearch = search;
        searchMatches.clear();
        matchCount = 0;
        if (search.isBlank()) {
            return;
        }
        String term = search.strip().toLowerCase(Locale.ROOT);
        String idTerm = term.replace(' ', '_');
        PassiveNodeKind kind = searchKind(term);
        for (int position = 0; position < nodes.size(); position++) {
            if (nodes.get(position) instanceof MegaPassiveNode node && (kind != null ? node.kind() == kind
                    : searchText(position).contains(term) || node.id().contains(idTerm))) {
                searchMatches.set(position);
                matchCount++;
            }
        }
    }

    /** A query that is exactly a node-type word, such as "keystones", highlights every node of that type. */
    private static PassiveNodeKind searchKind(String term) {
        for (PassiveNodeKind kind : List.of(PassiveNodeKind.KEYSTONE, PassiveNodeKind.NOTABLE, PassiveNodeKind.TRAVEL)) {
            String words = Component.translatable("rngtech.mastery.search.kind." + kind.name().toLowerCase(Locale.ROOT)).getString();
            if (List.of(words.toLowerCase(Locale.ROOT).split("\\|")).contains(term)) {
                return kind;
            }
        }
        return null;
    }

    private String searchText(int position) {
        if (searchText == null) {
            searchText = new String[nodes.size()];
        }
        if (searchText[position] == null) {
            N node = nodes.get(position);
            StringBuilder text = new StringBuilder(Component.translatable(node.translationKey()).getString());
            for (var effect : node.effects()) {
                text.append('\n').append(MachineStatDisplay.effectText(effect).getString());
            }
            searchText[position] = text.toString().toLowerCase(Locale.ROOT);
        }
        return searchText[position];
    }

    private BitSet relevant() {
        if (relevant == null) {
            relevant = new BitSet(nodes.size());
            for (int position = 0; position < nodes.size(); position++) {
                if (nodes.get(position) instanceof MegaPassiveNode node && relevant(node)) {
                    relevant.set(position);
                }
            }
        }
        return relevant;
    }

    private boolean relevant(MegaPassiveNode node) {
        return node.effects().stream().anyMatch(effect -> view.masterySupports(effect.stat()))
                || node.scaling().stream().anyMatch(effect -> view.masterySupports(effect.stat()))
                || node.fixed().keySet().stream().anyMatch(view::masterySupportsAbsolute)
                || node.ceilings().keySet().stream().anyMatch(view::masterySupportsAbsolute)
                || node.behaviors().stream().anyMatch(view::masterySupportsBehavior)
                || node.passive().keySet().stream().anyMatch(view.masteryFamily()::supports)
                || node.recipeHardnessCeiling() > 0 && view.masteryFamily() == MachineMasteryFamily.CRUSHER;
    }

    private int availablePoints() {
        return Math.max(0, view.unspentPassivePoints() - pending.cardinality());
    }

    private boolean allocatable(int position) {
        N node = nodes.get(position);
        return node instanceof MegaPassiveNode
                && node.kind() != PassiveNodeKind.STARTER
                && !node.alwaysAllocated()
                && view.machineLevel() >= node.requiredLevel()
                && callbacks.gearAllowsUnlock(node);
    }

    private int[] routeTo(int position) {
        if (position != routeTarget || routeVersion != stateVersion) {
            routeTarget = position;
            routeVersion = stateVersion;
            route = findRoute(position);
        }
        return route;
    }

    private int[] findRoute(int position) {
        if (unlocked.get(position) || !(nodes.get(position) instanceof MegaPassiveNode target)) {
            return NO_ROUTE;
        }
        List<MegaPassiveNode> path = MegaPassiveTree.allocationPath(this::displayedUnlocked, target);
        int[] steps = new int[path.size()];
        for (int index = 0; index < steps.length; index++) {
            steps[index] = position(path.get(index));
            if (steps[index] < 0) {
                return NO_ROUTE;
            }
        }
        return steps;
    }

    private boolean displayedUnlocked(PassiveNode node) {
        int position = position(node);
        return position >= 0 && unlocked.get(position);
    }

    private RouteStatus routeStatus(int[] steps) {
        if (steps.length == 0) {
            return RouteStatus.UNREACHABLE;
        }
        for (int step : steps) {
            if (!allocatable(step)) {
                return RouteStatus.BLOCKED;
            }
        }
        int points = availablePoints();
        if (points <= 0) {
            return RouteStatus.NO_POINTS;
        }
        return steps.length > points ? RouteStatus.TOO_FEW_POINTS : RouteStatus.READY;
    }

    private void allocate(int position) {
        int[] steps = routeTo(position);
        if (routeStatus(steps) != RouteStatus.READY) {
            return;
        }
        StringJoiner ids = new StringJoiner(",");
        for (int step : steps) {
            ids.add(((MegaPassiveNode) nodes.get(step)).id());
        }
        if (!send("allocate", ids.toString())) {
            return;
        }
        // Show the route immediately; the next menu sync confirms it, and a rejected route expires.
        for (int step : steps) {
            pending.set(step);
            unlocked.set(step);
        }
        pendingUntil = Util.getMillis() + MASTERY_PENDING_TIMEOUT_MS;
        stateVersion++;
        refreshUnlockable();
    }

    private int anchorOf(int position) {
        for (int neighbor : neighbors[position]) {
            if (unlocked.get(neighbor)) {
                return neighbor;
            }
        }
        return -1;
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

    private void drawRings(GuiShapeBatch shapes, int leftPos, int topPos) {
        int starterPosition = position(starter);
        if (starterPosition < 0) {
            return;
        }
        float ringCenterX = screenX(centerX[starterPosition], leftPos);
        float ringCenterY = screenY(centerY[starterPosition], topPos);
        int outerRadius = Math.max(contentMaxX, contentMaxY) * 2 / 3;
        for (int ring = MASTERY_RING_SPACING; ring <= outerRadius; ring += MASTERY_RING_SPACING) {
            float scaled = (float) (ring * zoom);
            shapes.ring(ringCenterX, ringCenterY, scaled - 0.5F, scaled + 0.5F, 0xFF282B28);
            float shadow = (float) ((ring + 5) * zoom);
            shapes.ring(ringCenterX, ringCenterY, shadow - 0.5F, shadow + 0.5F, 0xFF192126);
        }
    }

    private void drawLinks(GuiShapeBatch shapes, int leftPos, int topPos) {
        float thin = Math.max(1.0F, (float) zoom);
        float thick = Math.max(1.0F, (float) (zoom * 2.0D));
        for (int segment = 0; segment < linkFrom.length; segment++) {
            boolean fromUnlocked = unlocked.get(linkFrom[segment]);
            boolean toUnlocked = unlocked.get(linkTo[segment]);
            boolean active = fromUnlocked && toUnlocked;
            int color = active ? palette.statAccent() : fromUnlocked != toUnlocked ? palette.outputBonus() : 0xFF82745A;
            int offset = segment * 4;
            shapes.line(
                    screenX(linkSegments[offset], leftPos),
                    screenY(linkSegments[offset + 1], topPos),
                    screenX(linkSegments[offset + 2], leftPos),
                    screenY(linkSegments[offset + 3], topPos),
                    active ? thick : thin,
                    color
            );
        }
    }

    private void drawRoute(GuiShapeBatch shapes, int leftPos, int topPos, int[] steps) {
        if (steps.length == 0) {
            return;
        }
        int color = routeStatus(steps) == RouteStatus.READY ? ROUTE_READY : ROUTE_BLOCKED;
        float thickness = Math.max(1.5F, (float) (zoom * 2.0D));
        int previous = anchorOf(steps[0]);
        for (int step : steps) {
            if (previous >= 0) {
                shapes.line(
                        screenX(centerX[previous], leftPos),
                        screenY(centerY[previous], topPos),
                        screenX(centerX[step], leftPos),
                        screenY(centerY[step], topPos),
                        thickness,
                        color
                );
            }
            previous = step;
        }
    }

    private void drawNode(GuiShapeBatch shapes, int leftPos, int topPos, int position) {
        float nodeX = screenX(centerX[position], leftPos);
        float nodeY = screenY(centerY[position], topPos);
        float nodeRadius = screenRadius(position);
        if (!shapes.circleVisible(nodeX, nodeY, nodeRadius)) {
            return;
        }
        N node = nodes.get(position);
        boolean isUnlocked = unlocked.get(position);
        boolean isUnlockable = unlockable.get(position);
        int border = node.alwaysAllocated()
                ? palette.startNode()
                : isUnlocked ? palette.statAccent() : isUnlockable ? palette.outputBonus() : 0xFF9D885F;
        if (targets.get(position) && !isUnlocked) {
            border = TARGET_BORDER;
        }
        if (searchMatches.get(position) || highlightRelevant && relevant().get(position)) {
            border = SEARCH_HIGHLIGHT;
        }
        if (node.kind() == PassiveNodeKind.KEYSTONE) {
            shapes.diamond(nodeX, nodeY, nodeRadius, border);
            nodeRadius = Math.max(3.0F, nodeRadius - (float) (zoom * 2.0D));
        }
        if (nodeRadius < MASTERY_DETAIL_RADIUS) {
            shapes.disc(nodeX, nodeY, nodeRadius, border);
            return;
        }
        shapes.disc(nodeX, nodeY, nodeRadius, 0xFF080D11);
        shapes.disc(nodeX, nodeY, nodeRadius - 1.0F, border);
        shapes.disc(nodeX, nodeY, nodeRadius - 2.0F, nodeFill(node, isUnlocked, isUnlockable));
        if (node.kind() == PassiveNodeKind.NOTABLE || node.kind() == PassiveNodeKind.KEYSTONE || node.alwaysAllocated()) {
            float innerRadius = nodeRadius - Math.max(3.0F, (float) (zoom * 4.0D));
            if (innerRadius > 1.0F) {
                int color = isUnlocked ? palette.statAccent() : node.kind() == PassiveNodeKind.KEYSTONE ? 0xFF9275B0 : 0xFF766D52;
                shapes.ring(nodeX, nodeY, innerRadius - 0.5F, innerRadius + 0.5F, color);
            }
        }
    }

    private void drawHighlights(GuiShapeBatch shapes, int leftPos, int topPos, int hovered, int[] steps) {
        for (int position = searchMatches.nextSetBit(0); position >= 0; position = searchMatches.nextSetBit(position + 1)) {
            // Halos keep matches visible after they shrink to a few pixels.
            float halo = Math.max(screenRadius(position) + 1.5F, 4.0F);
            shapes.ring(screenX(centerX[position], leftPos), screenY(centerY[position], topPos), halo, halo + 1.0F, SEARCH_HIGHLIGHT);
        }
        int routeColor = routeStatus(steps) == RouteStatus.READY ? ROUTE_READY : ROUTE_BLOCKED;
        for (int step : steps) {
            outline(shapes, leftPos, topPos, step, routeColor);
        }
        if (hovered >= 0) {
            outline(shapes, leftPos, topPos, hovered, ROUTE_READY);
        }
    }

    private void outline(GuiShapeBatch shapes, int leftPos, int topPos, int position, int color) {
        float outer = screenRadius(position) + 1.0F;
        shapes.ring(screenX(centerX[position], leftPos), screenY(centerY[position], topPos), outer, outer + 1.0F, color);
    }

    private void drawIcons(GuiGraphics guiGraphics, int leftPos, int topPos, int viewLeft, int viewTop, int viewRight, int viewBottom) {
        Matrix4f pose = guiGraphics.pose().last().pose();
        for (int group = 0; group < iconTextures.size(); group++) {
            BufferBuilder builder = null;
            for (int position : iconPositions[group]) {
                float size = Math.min(MASTERY_ICON_TEXTURE_SIZE, (float) (radius[position] * 1.2D * zoom));
                if (size < MASTERY_MIN_ICON_SIZE) {
                    continue;
                }
                float left = screenX(centerX[position], leftPos) - size / 2.0F;
                float top = screenY(centerY[position], topPos) - size / 2.0F;
                if (left > viewRight || top > viewBottom || left + size < viewLeft || top + size < viewTop) {
                    continue;
                }
                if (builder == null) {
                    builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                }
                builder.addVertex(pose, left, top, 0.0F).setUv(0.0F, 0.0F);
                builder.addVertex(pose, left, top + size, 0.0F).setUv(0.0F, 1.0F);
                builder.addVertex(pose, left + size, top + size, 0.0F).setUv(1.0F, 1.0F);
                builder.addVertex(pose, left + size, top, 0.0F).setUv(1.0F, 0.0F);
            }
            MeshData mesh = builder == null ? null : builder.build();
            if (mesh != null) {
                RenderSystem.setShaderTexture(0, iconTextures.get(group));
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                BufferUploader.drawWithShader(mesh);
            }
        }
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

    private List<Component> nodeTooltip(int position) {
        N node = nodes.get(position);
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
            shared.fixed().forEach((stat, value) -> tooltip.add(applicability(Component.translatable("rngtech.mastery.fixed", Component.translatable(stat.translationKey()), MachineStatDisplay.formatNumber(value)), view.masterySupportsAbsolute(stat))));
            shared.ceilings().forEach((stat, value) -> tooltip.add(applicability(Component.translatable("rngtech.mastery.ceiling", Component.translatable(stat.translationKey()), MachineStatDisplay.formatNumber(value)), view.masterySupportsAbsolute(stat))));
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
            if (allocated.get(position)) {
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
        if (unlocked.get(position)) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.unlocked").withStyle(ChatFormatting.GREEN));
        } else if (view.machineLevel() < node.requiredLevel()) {
            tooltip.add(Component.translatable("rngtech.mastery.tooltip.locked").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(routeTooltip(routeTo(position)));
        }
        return tooltip;
    }

    private Component routeTooltip(int[] steps) {
        return switch (routeStatus(steps)) {
            case READY -> (steps.length == 1
                    ? Component.translatable("rngtech.mastery.tooltip.click_unlock")
                    : Component.translatable("rngtech.mastery.tooltip.click_path", steps.length)).withStyle(ChatFormatting.YELLOW);
            case UNREACHABLE -> Component.translatable("rngtech.mastery.tooltip.requires_link").withStyle(ChatFormatting.RED);
            case BLOCKED -> Component.translatable(steps.length == 1
                    ? "rngtech.mastery.tooltip.gear_conflict"
                    : "rngtech.mastery.tooltip.path_gear_conflict").withStyle(ChatFormatting.RED);
            case NO_POINTS -> Component.translatable("rngtech.mastery.tooltip.no_points").withStyle(ChatFormatting.RED);
            case TOO_FEW_POINTS -> Component.translatable("rngtech.mastery.tooltip.path_points", steps.length, availablePoints())
                    .withStyle(ChatFormatting.RED);
        };
    }

    private int hoveredNode(int leftPos, int topPos, int imageWidth, int imageHeight, double mouseX, double mouseY) {
        if (!isOverViewport(leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY)) {
            return -1;
        }
        double contentX = screenToContentX(mouseX, leftPos);
        double contentY = screenToContentY(mouseY, topPos);
        double minimumReach = MASTERY_MIN_HIT_RADIUS / zoom;
        double margin = 1.0D / zoom;
        int best = -1;
        double bestScore = Double.MAX_VALUE;
        for (int position = 0; position < nodes.size(); position++) {
            double dx = centerX[position] - contentX;
            double dy = centerY[position] - contentY;
            double reach = Math.max(radius[position] + margin, minimumReach);
            double distanceSquared = dx * dx + dy * dy;
            if (distanceSquared > reach * reach) {
                continue;
            }
            // Prefer the node whose edge is closest, so a large node wins inside its own outline.
            double score = Math.sqrt(distanceSquared) - radius[position];
            if (score < bestScore) {
                best = position;
                bestScore = score;
            }
        }
        return best;
    }

    private void renderToolbar(GuiGraphics graphics, int left, int top, int width) {
        Font font = Minecraft.getInstance().font;
        String[] labels = {"C", "P", snapshot.following() ? "||" : ">", "X", "H", "F", "R"};
        for (int i = 0; i < labels.length; i++) {
            int x = left + 8 + i * 20;
            graphics.fill(x, top + 42, x + 18, top + 54, i == 6 && highlightRelevant ? 0xFF596035 : 0xFF343E49);
            graphics.drawString(font, labels[i], x + 5, top + 44, 0xFFE7DDC0, false);
        }
        int searchLeft = left + MASTERY_SEARCH_X;
        int searchRight = left + width - 8;
        graphics.fill(searchLeft, top + 42, searchRight, top + 54, searchFocused ? 0xFF465366 : 0xFF252E38);
        String count = search.isBlank() ? "" : Integer.toString(matchCount);
        int countWidth = count.isEmpty() ? 0 : font.width(count) + 4;
        if (!count.isEmpty()) {
            graphics.drawString(font, count, searchRight - 2 - font.width(count), top + 44, matchCount > 0 ? 0xFFA9A08A : 0xFFC8604E, false);
        }
        String text = search.isEmpty() ? Component.translatable("rngtech.mastery.search").getString() : search;
        graphics.drawString(font, font.plainSubstrByWidth(text, Math.max(1, width - 168 - countWidth)), searchLeft + 3, top + 44, 0xFFE7DDC0, false);
    }

    private int toolbarIndex(int left, int top, double x, double y) {
        if (y < top + 42 || y >= top + 54 || x < left + 8 || x >= left + 148) { return -1; }
        return (int) (x - left - 8) / 20;
    }

    private boolean send(String action, String value) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        PacketDistributor.sendToServer(new MasteryActionPayload(player.containerMenu.containerId, action, value));
        return true;
    }

    private static Component applicability(Component text, boolean applies) {
        return applies ? text.copy().withStyle(ChatFormatting.GOLD) : Component.translatable("rngtech.mastery.inactive", text).withStyle(ChatFormatting.DARK_GRAY);
    }

    private boolean panning() {
        return dragging && dragMoved;
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

    private static boolean isOverSearch(int leftPos, int topPos, int imageWidth, double mouseX, double mouseY) {
        return mouseX >= leftPos + MASTERY_SEARCH_X && mouseX < leftPos + imageWidth - 8
                && mouseY >= topPos + 42 && mouseY < topPos + 54;
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
        resetDragging();
        fitRequested = expanded;
        restoreRequested = !expanded;
        callbacks.geometryChanged();
        clampAfterGeometryChange(imageWidth, imageHeight);
    }

    private void setPan(double nextPanX, double nextPanY, int imageWidth, int imageHeight) {
        double minPanX = contentMinX - MASTERY_CONTENT_PADDING - MASTERY_VIEW_X / zoom;
        double maxPanX = contentMaxX + MASTERY_CONTENT_PADDING - viewRight(imageWidth) / zoom;
        double minPanY = contentMinY - MASTERY_CONTENT_PADDING - MASTERY_VIEW_Y / zoom;
        double maxPanY = contentMaxY + MASTERY_CONTENT_PADDING - viewBottom(imageHeight) / zoom;
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
        centerOn(position(starter), imageWidth, imageHeight);
    }

    private void centerOn(int position, int imageWidth, int imageHeight) {
        if (position < 0) {
            return;
        }
        setPan(
                centerX[position] - (MASTERY_VIEW_X + viewWidth(imageWidth) / 2.0D) / zoom,
                centerY[position] - (MASTERY_VIEW_Y + viewHeight(imageHeight) / 2.0D) / zoom,
                imageWidth,
                imageHeight
        );
    }

    private double fitZoom(int imageWidth, int imageHeight) {
        return Math.min(1.0D, Math.min(
                (double) viewWidth(imageWidth) / (contentMaxX - contentMinX + MASTERY_CONTENT_PADDING * 2),
                (double) viewHeight(imageHeight) / (contentMaxY - contentMinY + MASTERY_CONTENT_PADDING * 2)
        ));
    }

    private void fitTree(int imageWidth, int imageHeight) {
        zoom = fitZoom(imageWidth, imageHeight);
        setPan(
                (contentMinX + contentMaxX) / 2.0D - (MASTERY_VIEW_X + viewWidth(imageWidth) / 2.0D) / zoom,
                (contentMinY + contentMaxY) / 2.0D - (MASTERY_VIEW_Y + viewHeight(imageHeight) / 2.0D) / zoom,
                imageWidth,
                imageHeight
        );
    }

    private int position(PassiveNode node) {
        if (node == null || node.index() < 0 || node.index() >= positionByIndex.length) {
            return -1;
        }
        int position = positionByIndex[node.index()];
        return position >= 0 && nodes.get(position) == node ? position : -1;
    }

    private float screenX(double contentX, int leftPos) {
        return (float) (leftPos + (contentX - panX) * zoom);
    }

    private float screenY(double contentY, int topPos) {
        return (float) (topPos + (contentY - panY) * zoom);
    }

    private float screenRadius(int position) {
        return Math.max(1.0F, (float) (radius[position] * zoom));
    }

    private double screenToContentX(double screenX, int leftPos) {
        return panX + (screenX - leftPos) / zoom;
    }

    private double screenToContentY(double screenY, int topPos) {
        return panY + (screenY - topPos) / zoom;
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

    private static String flagTranslationKey(PassiveNodeFlag flag) {
        return "rngtech.mastery.flag." + flag.name().toLowerCase(Locale.ROOT);
    }

    private static String passiveStatTranslationKey(PassiveStatType stat) {
        return "rngtech.mastery.passive_stat." + stat.name().toLowerCase(Locale.ROOT);
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private enum RouteStatus {
        READY,
        UNREACHABLE,
        BLOCKED,
        NO_POINTS,
        TOO_FEW_POINTS
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

        default void appendSpecialTooltip(N node, List<Component> tooltip) {
        }

        default void geometryChanged() {
        }
    }
}
