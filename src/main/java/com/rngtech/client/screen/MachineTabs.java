package com.rngtech.client.screen;

import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.jewel.JewelColour;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Tab strip shared by the tabbed machine screens. Tabs stack down the panel's left edge with icon and name,
 * collapse to an icon rail when the screen has no room for names, and move above the panel when even the rail
 * does not fit (the expanded Mastery tree, or a panel wider than the screen).
 */
public final class MachineTabs {
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_STEP = 22;
    private static final int RAIL_TOP = 4;
    private static final int ICON_RAIL_WIDTH = 22;
    private static final int LABEL_GAP = 4;
    private static final int LABEL_PADDING = 6;
    private static final int SCREEN_MARGIN = 2;
    private static final int TOP_TAB_X = 6;
    private static final int TOP_TAB_WIDTH = 24;
    private static final int TOP_TAB_HEIGHT = 22;
    private static final int TOP_TAB_STEP = 26;
    private static final int SELECTED_LABEL_MARGIN = 8;

    public enum Mode {
        LABELLED,
        ICONS,
        TOP
    }

    public interface Host {
        MachineTabs machineTabs();
    }

    public record Tab(int id, Component label, ItemStack icon, BooleanSupplier shown) {
    }

    public record Style(int panel, int light, int dark, int idle, int hover, int text, int idleText) {
        public static final Style STANDARD = new Style(0xFFC6C6C6, 0xFFE9E9E9, 0xFF8B8B8B, 0xFF9A9A9A, 0xFFB0B0B0, 0xFF3F3F3F, 0xFF2F2F2F);
    }

    private final AbstractContainerScreen<?> screen;
    private final List<Tab> tabs;
    private final Style style;
    private final int labelledWidth;

    private MachineTabs(AbstractContainerScreen<?> screen, List<Tab> tabs, Style style) {
        this.screen = screen;
        this.tabs = List.copyOf(tabs);
        this.style = style;
        int widestLabel = 0;
        for (Tab tab : this.tabs) {
            widestLabel = Math.max(widestLabel, font().width(tab.label()));
        }
        this.labelledWidth = 3 + 16 + LABEL_GAP + widestLabel + LABEL_PADDING;
    }

    public static Builder builder(AbstractContainerScreen<?> screen) {
        return new Builder(screen);
    }

    public Mode mode() {
        int left = screen.getGuiLeft();
        boolean railFits = RAIL_TOP + shownTabs().size() * TAB_STEP <= screen.getYSize();
        if (railFits && left >= labelledWidth + SCREEN_MARGIN) {
            return Mode.LABELLED;
        }
        if (railFits && left >= ICON_RAIL_WIDTH + SCREEN_MARGIN) {
            return Mode.ICONS;
        }
        return Mode.TOP;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, int selectedId) {
        Mode mode = mode();
        List<Tab> tabs = shownTabs();
        for (int index = 0; index < tabs.size(); index++) {
            Tab tab = tabs.get(index);
            Rect2i area = tabArea(mode, index);
            boolean selected = tab.id() == selectedId;
            boolean hovered = !selected && area.contains(mouseX, mouseY);
            if (mode == Mode.TOP) {
                renderTopTab(guiGraphics, area, selected, hovered);
                guiGraphics.renderItem(tab.icon(), area.getX() + 4, area.getY() + 3);
            } else {
                renderSideTab(guiGraphics, area, selected, hovered);
                guiGraphics.renderItem(tab.icon(), area.getX() + 3, area.getY() + 2);
                if (mode == Mode.LABELLED) {
                    guiGraphics.drawString(
                            font(),
                            tab.label(),
                            area.getX() + 3 + 16 + LABEL_GAP,
                            area.getY() + 6,
                            selected ? style.text() : style.idleText(),
                            false
                    );
                }
            }
        }
    }

    /**
     * Draws the selected tab's name at the right of the title row when the tabs show no names. Call from
     * {@code renderLabels}; it is skipped when the title leaves no room.
     */
    public void renderSelectedLabel(GuiGraphics guiGraphics, int selectedId, int titleX, int titleY, int color) {
        if (mode() == Mode.LABELLED) {
            return;
        }
        Tab tab = tab(selectedId);
        if (tab == null) {
            return;
        }
        int labelWidth = font().width(tab.label());
        int x = screen.getXSize() - SELECTED_LABEL_MARGIN - labelWidth;
        if (titleX + font().width(screen.getTitle()) + SELECTED_LABEL_MARGIN > x) {
            return;
        }
        guiGraphics.drawString(font(), tab.label(), x, titleY, color, false);
    }

    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Mode mode = mode();
        if (mode == Mode.LABELLED) {
            return;
        }
        int index = indexAt(mode, mouseX, mouseY);
        if (index >= 0) {
            guiGraphics.renderTooltip(font(), shownTabs().get(index).label(), mouseX, mouseY);
        }
    }

    /** Returns the id of the tab under the mouse, or -1. */
    public int tabAt(double mouseX, double mouseY) {
        int index = indexAt(mode(), mouseX, mouseY);
        return index < 0 ? -1 : shownTabs().get(index).id();
    }

    /** Screen-space rectangles the tabs cover outside the panel, for recipe viewer exclusion areas. */
    public List<Rect2i> areas() {
        Mode mode = mode();
        int count = shownTabs().size();
        if (count == 0) {
            return List.of();
        }
        Rect2i first = tabArea(mode, 0);
        Rect2i last = tabArea(mode, count - 1);
        int right = last.getX() + last.getWidth();
        int bottom = last.getY() + last.getHeight();
        return List.of(new Rect2i(first.getX(), first.getY(), right - first.getX(), bottom - first.getY()));
    }

    private int indexAt(Mode mode, double mouseX, double mouseY) {
        int count = shownTabs().size();
        for (int index = 0; index < count; index++) {
            Rect2i area = tabArea(mode, index);
            if (mouseX >= area.getX() && mouseX < area.getX() + area.getWidth()
                    && mouseY >= area.getY() && mouseY < area.getY() + area.getHeight()) {
                return index;
            }
        }
        return -1;
    }

    private Rect2i tabArea(Mode mode, int index) {
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        return switch (mode) {
            case LABELLED -> new Rect2i(left - labelledWidth, top + RAIL_TOP + index * TAB_STEP, labelledWidth, TAB_HEIGHT);
            case ICONS -> new Rect2i(left - ICON_RAIL_WIDTH, top + RAIL_TOP + index * TAB_STEP, ICON_RAIL_WIDTH, TAB_HEIGHT);
            case TOP -> new Rect2i(left + TOP_TAB_X + index * TOP_TAB_STEP, top - TOP_TAB_HEIGHT + 1, TOP_TAB_WIDTH, TOP_TAB_HEIGHT);
        };
    }

    private void renderSideTab(GuiGraphics guiGraphics, Rect2i area, boolean selected, boolean hovered) {
        int x = area.getX();
        int y = area.getY();
        int right = x + area.getWidth();
        int bottom = y + area.getHeight();
        guiGraphics.fill(x, y, right, bottom, selected ? style.panel() : hovered ? style.hover() : style.idle());
        guiGraphics.fill(x, y, right, y + 1, style.light());
        guiGraphics.fill(x, y, x + 1, bottom, style.light());
        guiGraphics.fill(x, bottom - 1, right, bottom, style.dark());
        if (selected) {
            guiGraphics.fill(right, y + 1, right + 1, bottom - 1, style.panel());
        }
    }

    private void renderTopTab(GuiGraphics guiGraphics, Rect2i area, boolean selected, boolean hovered) {
        int x = area.getX();
        int y = area.getY();
        int right = x + area.getWidth();
        int bottom = y + area.getHeight();
        guiGraphics.fill(x, y, right, bottom, selected ? style.panel() : hovered ? style.hover() : style.idle());
        guiGraphics.fill(x, y, right, y + 1, style.light());
        guiGraphics.fill(x, y, x + 1, bottom, style.light());
        guiGraphics.fill(right - 1, y, right, bottom, style.dark());
        if (selected) {
            guiGraphics.fill(x + 1, bottom - 1, right - 1, bottom, style.panel());
        } else {
            guiGraphics.fill(x, bottom - 1, right, bottom, style.dark());
        }
    }

    private List<Tab> shownTabs() {
        List<Tab> shown = new ArrayList<>(tabs.size());
        for (Tab tab : tabs) {
            if (tab.shown().getAsBoolean()) {
                shown.add(tab);
            }
        }
        return shown;
    }

    private Tab tab(int id) {
        for (Tab tab : shownTabs()) {
            if (tab.id() == id) {
                return tab;
            }
        }
        return null;
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    public static final class Builder {
        private final AbstractContainerScreen<?> screen;
        private final List<Tab> tabs = new ArrayList<>();
        private Style style = Style.STANDARD;

        private Builder(AbstractContainerScreen<?> screen) {
            this.screen = screen;
        }

        public Builder tab(int id, String labelKey, ItemLike icon) {
            return tab(id, Component.translatable(labelKey), new ItemStack(icon));
        }

        public Builder tab(int id, Component label, ItemStack icon) {
            return tab(id, label, icon, () -> true);
        }

        private Builder tab(int id, Component label, ItemStack icon, BooleanSupplier shown) {
            tabs.add(new Tab(id, label, icon, shown));
            return this;
        }

        /** The machine's main tab, shown with the block the player opened when it belongs to the same machine. */
        public Builder processing(int id, ItemLike machine) {
            return tab(id, Component.translatable("rngtech.tab.processing"), openedMachineIcon(machine));
        }

        public Builder gear(int id) {
            return tab(id, "rngtech.tab.gear", ModItems.WRENCH.get());
        }

        public Builder stats(int id) {
            return tab(id, "rngtech.tab.stats", ModItems.CONFIGURATOR.get());
        }

        public Builder refinement(int id) {
            return tab(id, "rngtech.tab.refinement", ModItems.AFFIX_MODIFIER.get());
        }

        public Builder mastery(int id) {
            return tab(id, "rngtech.tab.mastery", ModItems.MASTERY_REFUND.get());
        }

        public Builder jewels(int id) {
            return tab(id, "rngtech.tab.jewels", ModItems.jewel(JewelColour.DRIVE).get());
        }

        /** The Orders tab, hidden until {@code shown} holds, so players who have not found Work Orders do not see it. */
        public Builder orders(int id, BooleanSupplier shown) {
            return tab(id, Component.translatable("rngtech.tab.orders"), new ItemStack(ModItems.JOB_CHIP.get()), shown);
        }

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public MachineTabs build() {
            return new MachineTabs(screen, tabs, style);
        }

        private static ItemStack openedMachineIcon(ItemLike machine) {
            ItemStack fallback = new ItemStack(machine);
            Minecraft minecraft = Minecraft.getInstance();
            if (!(fallback.getItem() instanceof BlockItem machineItem)
                    || minecraft.level == null
                    || !(minecraft.hitResult instanceof BlockHitResult hit)
                    || hit.getType() != HitResult.Type.BLOCK) {
                return fallback;
            }
            Block opened = minecraft.level.getBlockState(hit.getBlockPos()).getBlock();
            if (opened.getClass() != machineItem.getBlock().getClass() || opened.asItem() == Items.AIR) {
                return fallback;
            }
            return new ItemStack(opened);
        }
    }
}
