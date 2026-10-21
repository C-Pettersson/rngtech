package com.rngtech.client.screen;

import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.PhaseCradleBlockEntity;
import com.rngtech.content.blockentity.PhaseLine;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.GlyphItem;
import com.rngtech.content.menu.PhaseCradleMenu;
import com.rngtech.content.recipe.PhaseArrayRecipe;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierText;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierDefinition;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.PhaseArrayRules;
import com.rngtech.rpg.refinement.PhaseArrayAction;
import com.rngtech.rpg.refinement.PhasePulse;
import com.rngtech.rpg.refinement.PhasePulseSequence;
import com.rngtech.rpg.refinement.RefinementEngine;
import com.rngtech.rpg.refinement.RefinementSelection;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The Phase Cradle's one panel, in the Dormant Machine's style: the line drawn as it stands, the target in a chamber,
 * a charge conduit, nine cell sockets, a row of operation marks, an affix list, and one button. Hover for every value.
 */
public class PhaseCradleScreen extends AbstractContainerScreen<PhaseCradleMenu> {
    private static final int PLATE_TOP = 0xFF2B3138;
    private static final int PLATE_BOTTOM = 0xFF1E2328;
    private static final int BEZEL_LIGHT = 0xFF5A6470;
    private static final int BEZEL_DARK = 0xFF14181C;
    private static final int TRACE = 0xFF3A444E;
    private static final int SOCKET = 0xFF0F1316;
    private static final int TEXT = 0xFFE6EDF3;
    private static final int DIM = 0xFF6B747D;
    private static final int LIT = 0xFF7FE8FF;
    private static final int LIT_DEEP = 0xFF2E8FA3;
    private static final int AMBER = 0xFFE3A23A;
    private static final int RED = 0xFFD0473A;
    private static final int GREEN = 0xFF7BD88F;
    private static final int NODE_DARK = 0xFF2A3036;
    private static final int ROW = 0xFF262C33;
    private static final int ROW_SELECTED = 0xFF35505A;
    private static final int ROW_HOVER = 0xFF303840;
    private static final int LIT_BRIGHT = 0xFFD8FBFF;

    private static final int LINE_Y = 20;
    private static final int EMITTER_X = 76;
    private static final int NODE_X = 92;
    private static final int NODE_SPACING = 12;
    private static final int NODE_SIZE = 8;
    private static final int CONDUIT_X = 76;
    private static final int CONDUIT_Y = 42;
    private static final int CONDUIT_WIDTH = 140;
    private static final int CONDUIT_HEIGHT = 7;
    private static final int STATUS_X = 12;
    private static final int STATUS_Y = 18;
    private static final int RESULT_X = 76;
    private static final int RESULT_Y = 54;
    private static final int OPERATION_X = 76;
    private static final int OPERATION_Y = 70;
    private static final int OPERATION_SIZE = 12;
    private static final int OPERATION_SPACING = 14;
    private static final int LIST_X = 76;
    private static final int LIST_Y = 86;
    private static final int LIST_WIDTH = 140;
    private static final int ROW_HEIGHT = 12;
    private static final int LIST_ROWS = 6;
    private static final int BUTTON_X = 76;
    private static final int BUTTON_Y = 160;
    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 14;
    private static final int STRAIN_MARKS = 12;

    /** A row of the affix list: what it selects, its title, and its tooltip. */
    private record Row(int button, Component title, List<Component> tooltip, boolean selected, boolean enabled) {
    }

    private int scroll;
    private int ticks;

    public PhaseCradleScreen(PhaseCradleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PhaseCradleMenu.WIDTH;
        imageHeight = PhaseCradleMenu.HEIGHT;
        inventoryLabelX = PhaseCradleMenu.INVENTORY_X;
        inventoryLabelY = PhaseCradleMenu.INVENTORY_Y - 11;
        titleLabelX = 8;
        titleLabelY = 6;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ticks++;
    }

    // -- Drawing -------------------------------------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.fillGradient(left, top, left + imageWidth, top + imageHeight, PLATE_TOP, PLATE_BOTTOM);
        graphics.fill(left, top, left + imageWidth, top + 1, BEZEL_LIGHT);
        graphics.fill(left, top, left + 1, top + imageHeight, BEZEL_LIGHT);
        graphics.fill(left + imageWidth - 1, top, left + imageWidth, top + imageHeight, BEZEL_DARK);
        graphics.fill(left, top + imageHeight - 1, left + imageWidth, top + imageHeight, BEZEL_DARK);
        drawStatus(graphics, left + STATUS_X, top + STATUS_Y);
        drawLine(graphics, left, top);
        drawConduit(graphics, left + CONDUIT_X, top + CONDUIT_Y);
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                drawSocket(graphics, left + slot.x - 1, top + slot.y - 1, 0);
            } else if (slot.index >= PhaseCradleBlockEntity.SLOT_COUNT) {
                drawSocket(graphics, left + slot.x - 1, top + slot.y - 1, cellRing(slot.getItem()));
            } else {
                drawSocket(graphics, left + slot.x - 1, top + slot.y - 1, slot.index == PhaseCradleBlockEntity.SLOT_TARGET ? LIT_DEEP : TRACE);
            }
        }
        drawStrainMarks(graphics, left + PhaseCradleMenu.TARGET_X - 1, top + PhaseCradleMenu.TARGET_Y - 1);
        if (menu.pulsing()) {
            int glow = (ticks / 4) % 2 == 0 ? LIT : LIT_DEEP;
            int gx = left + PhaseCradleMenu.TARGET_X - 3;
            int gy = top + PhaseCradleMenu.TARGET_Y - 3;
            graphics.fill(gx, gy, gx + 22, gy + 1, glow);
            graphics.fill(gx, gy + 21, gx + 22, gy + 22, glow);
            graphics.fill(gx, gy, gx + 1, gy + 22, glow);
            graphics.fill(gx + 21, gy, gx + 22, gy + 22, glow);
        }
        drawOperations(graphics, left + OPERATION_X, top + OPERATION_Y, mouseX, mouseY);
        drawList(graphics, left + LIST_X, top + LIST_Y, mouseX, mouseY);
        drawButton(graphics, left + BUTTON_X, top + BUTTON_Y, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        List<Row> rows = visibleRows();
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            int color = row.selected() ? LIT : row.enabled() ? TEXT : DIM;
            drawClipped(graphics, row.title(), LIST_X + 3, LIST_Y + index * ROW_HEIGHT + 2, LIST_WIDTH - 6, color);
        }
        Component result = resultLine();
        if (result != null) {
            drawClipped(graphics, result, RESULT_X, RESULT_Y, LIST_WIDTH, resultColor());
        }
        Component label = buttonLabel();
        int width = font.width(label);
        graphics.drawString(font, label, BUTTON_X + (BUTTON_WIDTH - width) / 2, BUTTON_Y + 3, buttonEnabled() ? TEXT : DIM, false);
    }

    private void drawClipped(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        String clipped = font.plainSubstrByWidth(text.getString(), maxWidth);
        graphics.drawString(font, clipped, x, y, color, false);
    }

    private void drawStatus(GuiGraphics graphics, int x, int y) {
        int status = menu.status();
        boolean blocked = status == PhaseCradleBlockEntity.STATUS_NO_LINE || status == PhaseCradleBlockEntity.STATUS_NO_COILS
                || status == PhaseCradleBlockEntity.STATUS_LOCKED || status == PhaseCradleBlockEntity.STATUS_OUTPUT_FULL
                || status == PhaseCradleBlockEntity.STATUS_ILLEGAL || status == PhaseCradleBlockEntity.STATUS_NO_RECIPE
                || status == PhaseCradleBlockEntity.STATUS_DONOR_MISMATCH;
        boolean waiting = status == PhaseCradleBlockEntity.STATUS_NO_TARGET || status == PhaseCradleBlockEntity.STATUS_NO_GLYPH
                || status == PhaseCradleBlockEntity.STATUS_NO_RP || status == PhaseCradleBlockEntity.STATUS_NO_SELECTION;
        boolean live = status == PhaseCradleBlockEntity.STATUS_CHARGING || status == PhaseCradleBlockEntity.STATUS_READY
                || status == PhaseCradleBlockEntity.STATUS_PULSING;
        for (int row = 0; row < 5; row++) {
            graphics.fill(x + 2 - row / 2, y + row, x + 3 + row / 2, y + row + 1, blocked ? RED : NODE_DARK);
        }
        graphics.fill(x + 10, y, x + 15, y + 5, waiting ? AMBER : NODE_DARK);
        int circle = live ? LIT : NODE_DARK;
        graphics.fill(x + 21, y, x + 24, y + 5, circle);
        graphics.fill(x + 20, y + 1, x + 25, y + 4, circle);
    }

    private void drawLine(GuiGraphics graphics, int left, int top) {
        int y = top + LINE_Y;
        boolean pulsing = menu.pulsing();
        graphics.fill(left + EMITTER_X + 10, y + 4, left + PhaseCradleMenu.TARGET_X - 2, y + 5, pulsing ? LIT : menu.formed() ? LIT_DEEP : TRACE);
        drawNode(graphics, left + EMITTER_X, y, 10, pulsing ? LIT_BRIGHT : menu.formed() ? LIT : NODE_DARK, menu.formed() ? LIT_DEEP : TRACE);
        if (pulsing) {
            drawPulse(graphics, left, y);
            return;
        }
        int coils = menu.coilCount();
        int usable = menu.usableCoils();
        int required = menu.requiredCoils();
        for (int position = 1; position <= PhaseArrayRules.MAX_COILS; position++) {
            int x = left + NODE_X + (position - 1) * NODE_SPACING;
            int fill;
            int edge = TRACE;
            if (position > coils || !menu.formed()) {
                fill = position <= coils ? NODE_DARK : 0;
            } else if (position <= usable) {
                fill = lastPulseColor(position, LIT);
                edge = position <= required ? LIT_DEEP : AMBER;
            } else {
                fill = NODE_DARK;
                edge = AMBER;
            }
            drawNode(graphics, x, y + 1, NODE_SIZE, fill, edge);
        }
    }

    /** The sequence as the Cradle reports it: engaged Coils bright, the slipped one flashing, the rest dark. */
    private void drawPulse(GuiGraphics graphics, int left, int y) {
        PhasePulseSequence.Stage stage = menu.pulseStage();
        int lit = menu.pulseLit();
        int flashing = menu.pulseFlashing();
        boolean blink = (ticks / 3) % 2 == 0;
        for (int position = 1; position <= PhaseArrayRules.MAX_COILS; position++) {
            int x = left + NODE_X + (position - 1) * NODE_SPACING;
            if (position > menu.coilCount()) {
                drawNode(graphics, x, y + 1, NODE_SIZE, 0, TRACE);
                continue;
            }
            int fill = NODE_DARK;
            int edge = TRACE;
            if (position == flashing) {
                int slip = flashing > menu.requiredCoils() ? AMBER : RED;
                fill = blink ? slip : NODE_DARK;
                edge = slip;
            } else if (position <= lit) {
                fill = stage == PhasePulseSequence.Stage.HOLDING && blink ? LIT_BRIGHT : LIT;
                edge = LIT_DEEP;
            } else if (stage == PhasePulseSequence.Stage.FRACTURED) {
                fill = blink ? RED : NODE_DARK;
            }
            drawNode(graphics, x, y + 1, NODE_SIZE, fill, edge);
        }
    }

    /**
     * After a pulse, held Coils stay lit and the slipped one shows red until the next target goes in; a slip past the
     * operation's Coil count did no harm, so it shows amber.
     */
    private int lastPulseColor(int position, int base) {
        PhasePulse.Outcome outcome = menu.lastOutcome();
        if (outcome == null || !menu.targetStack().isEmpty()) {
            return base;
        }
        if (position == menu.lastSlipped()) {
            return outcome == PhasePulse.Outcome.SUCCESS || outcome == PhasePulse.Outcome.OVERHOLD ? AMBER : RED;
        }
        return position <= menu.lastHeld() ? GREEN : NODE_DARK;
    }

    private void drawNode(GuiGraphics graphics, int x, int y, int size, int fill, int edge) {
        graphics.fill(x, y, x + size, y + size, edge);
        if (fill != 0) {
            graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, fill);
        } else {
            graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, SOCKET);
        }
    }

    private void drawConduit(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + CONDUIT_WIDTH, y + CONDUIT_HEIGHT, BEZEL_DARK);
        graphics.fill(x + 1, y + 1, x + CONDUIT_WIDTH - 1, y + CONDUIT_HEIGHT - 1, SOCKET);
        long cost = menu.cost();
        double fraction = cost <= 0 ? 0 : Math.min(1.0, menu.charge() / (double) cost);
        int filled = (int) Math.round(fraction * (CONDUIT_WIDTH - 2));
        if (filled > 0) {
            boolean ready = menu.status() == PhaseCradleBlockEntity.STATUS_READY;
            int bright = ready && (ticks / 10) % 2 == 0 ? 0xFFB8F5FF : LIT;
            graphics.fillGradient(x + 1, y + 1, x + 1 + filled, y + CONDUIT_HEIGHT - 1, bright, LIT_DEEP);
            int offset = (ticks / 2) % 6;
            for (int stripe = x + 1 + offset; stripe < x + 1 + filled; stripe += 6) {
                graphics.fill(stripe, y + 1, stripe + 1, y + CONDUIT_HEIGHT - 1, 0x66FFFFFF);
            }
        }
        graphics.fill(x + 1, y + 1, x + CONDUIT_WIDTH - 1, y + 2, 0x33FFFFFF);
    }

    private void drawSocket(GuiGraphics graphics, int x, int y, int ring) {
        graphics.fill(x, y, x + 18, y + 18, BEZEL_DARK);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, BEZEL_LIGHT);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, SOCKET);
        if (ring != 0) {
            graphics.fill(x, y, x + 18, y + 1, ring);
            graphics.fill(x, y + 17, x + 18, y + 18, ring);
            graphics.fill(x, y, x + 1, y + 18, ring);
            graphics.fill(x + 17, y, x + 18, y + 18, ring);
        }
    }

    private static int cellRing(ItemStack cell) {
        if (!BatteryCellItem.isBatteryCell(cell)) {
            return 0;
        }
        int capacity = BatteryCellItem.energyCapacity(cell);
        int stored = BatteryCellItem.energyStored(cell);
        if (capacity <= 0 || stored <= 0) {
            return 0xFF3A2A22;
        }
        return lerpColor(0xFF1E5662, LIT, stored / (double) capacity);
    }

    private void drawStrainMarks(GuiGraphics graphics, int x, int y) {
        int strain = menu.strain();
        int tolerance = menu.strainTolerance();
        for (int mark = 0; mark < STRAIN_MARKS; mark++) {
            int color = mark < strain ? (mark < tolerance ? GREEN : AMBER) : NODE_DARK;
            int side = mark / 3;
            int along = mark % 3 * 6 + 2;
            switch (side) {
                case 0 -> graphics.fill(x + along, y - 3, x + along + 3, y - 2, color);
                case 1 -> graphics.fill(x + 19, y + along, x + 20, y + along + 3, color);
                case 2 -> graphics.fill(x + along, y + 19, x + along + 3, y + 20, color);
                default -> graphics.fill(x - 3, y + along, x - 2, y + along + 3, color);
            }
        }
    }

    private void drawOperations(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        PhaseArrayAction selected = menu.action();
        PhaseArrayAction[] actions = PhaseArrayAction.values();
        for (int index = 0; index < actions.length; index++) {
            int markX = x + index * OPERATION_SPACING;
            boolean hovered = inside(mouseX - leftPos, mouseY - topPos, OPERATION_X + index * OPERATION_SPACING, OPERATION_Y, OPERATION_SIZE, OPERATION_SIZE);
            boolean active = actions[index] == selected;
            graphics.fill(markX, y, markX + OPERATION_SIZE, y + OPERATION_SIZE, active ? LIT : hovered ? BEZEL_LIGHT : BEZEL_DARK);
            graphics.fill(markX + 1, y + 1, markX + OPERATION_SIZE - 1, y + OPERATION_SIZE - 1, active ? ROW_SELECTED : SOCKET);
            drawOperationGlyph(graphics, actions[index], markX + 2, y + 2, active ? LIT : TEXT);
        }
    }

    /** Nine small 8x8 pixel marks, one shape per operation, so the row reads without labels. */
    private void drawOperationGlyph(GuiGraphics graphics, PhaseArrayAction action, int x, int y, int color) {
        switch (action) {
            case REFORGE -> {
                graphics.fill(x + 1, y + 1, x + 7, y + 2, color);
                graphics.fill(x + 1, y + 6, x + 7, y + 7, color);
                graphics.fill(x + 3, y + 3, x + 5, y + 5, color);
            }
            case ATTUNE -> {
                graphics.fill(x + 1, y + 4, x + 7, y + 5, color);
                graphics.fill(x + 2, y + 2, x + 3, y + 7, color);
                graphics.fill(x + 5, y + 1, x + 6, y + 6, color);
            }
            case RESONATE -> {
                graphics.fill(x + 3, y + 1, x + 5, y + 7, color);
                graphics.fill(x + 1, y + 3, x + 7, y + 4, color);
            }
            case EXCISE -> graphics.fill(x + 1, y + 3, x + 7, y + 5, color);
            case IMPRINT -> {
                graphics.fill(x + 1, y + 1, x + 7, y + 7, color);
                graphics.fill(x + 2, y + 2, x + 6, y + 6, SOCKET);
                graphics.fill(x + 3, y + 3, x + 5, y + 5, color);
            }
            case TRANSMUTE -> {
                graphics.fill(x + 1, y + 2, x + 6, y + 3, color);
                graphics.fill(x + 2, y + 5, x + 7, y + 6, color);
                graphics.fill(x + 5, y + 1, x + 6, y + 2, color);
                graphics.fill(x + 2, y + 6, x + 3, y + 7, color);
            }
            case TEMPER -> {
                graphics.fill(x + 1, y + 6, x + 7, y + 7, color);
                graphics.fill(x + 2, y + 4, x + 6, y + 5, color);
                graphics.fill(x + 3, y + 2, x + 5, y + 3, color);
            }
            case EXALT -> {
                graphics.fill(x + 3, y + 1, x + 5, y + 2, color);
                graphics.fill(x + 2, y + 2, x + 6, y + 3, color);
                graphics.fill(x + 1, y + 3, x + 7, y + 4, color);
                graphics.fill(x + 3, y + 4, x + 5, y + 7, color);
            }
            case GRAFT -> {
                graphics.fill(x + 1, y + 1, x + 3, y + 7, color);
                graphics.fill(x + 5, y + 1, x + 7, y + 7, color);
                graphics.fill(x + 3, y + 3, x + 5, y + 5, color);
            }
        }
    }

    private void drawList(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        graphics.fill(x - 1, y - 1, x + LIST_WIDTH + 1, y + LIST_ROWS * ROW_HEIGHT + 1, BEZEL_DARK);
        graphics.fill(x, y, x + LIST_WIDTH, y + LIST_ROWS * ROW_HEIGHT, SOCKET);
        List<Row> rows = visibleRows();
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            int rowY = y + index * ROW_HEIGHT;
            boolean hovered = hoveredRow(mouseX, mouseY) == index;
            graphics.fill(x, rowY, x + LIST_WIDTH, rowY + ROW_HEIGHT - 1, row.selected() ? ROW_SELECTED : hovered && row.enabled() ? ROW_HOVER : ROW);
        }
        int total = allRows().size();
        if (total > LIST_ROWS) {
            int trackHeight = LIST_ROWS * ROW_HEIGHT;
            int thumb = Math.max(4, trackHeight * LIST_ROWS / total);
            int thumbY = y + (trackHeight - thumb) * scroll / Math.max(1, total - LIST_ROWS);
            graphics.fill(x + LIST_WIDTH - 2, thumbY, x + LIST_WIDTH - 1, thumbY + thumb, BEZEL_LIGHT);
        }
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX - leftPos, mouseY - topPos, BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        boolean enabled = buttonEnabled();
        int status = menu.status();
        int edge = status == PhaseCradleBlockEntity.STATUS_READY ? LIT : enabled ? BEZEL_LIGHT : BEZEL_DARK;
        graphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, edge);
        graphics.fill(x + 1, y + 1, x + BUTTON_WIDTH - 1, y + BUTTON_HEIGHT - 1, enabled && hovered ? ROW_HOVER : ROW);
    }

    // -- Rows ------------------------------------------------------------------------------------------------------------

    private MachineTraits targetTraits() {
        ItemStack target = menu.targetStack();
        return RefinementTargets.canRefine(target) ? RefinementTargets.storedTraits(target) : MachineTraits.EMPTY;
    }

    private List<MachineModifier> affixes(MachineTraits traits) {
        return traits.modifiers().stream().filter(modifier -> modifier.slot().isAffix()).toList();
    }

    /** The whole list for the current mode: the target's affixes and slots, Imprint candidates, or the donor's affixes. */
    private List<Row> allRows() {
        List<Row> rows = new ArrayList<>();
        ItemStack target = menu.targetStack();
        if (!RefinementTargets.canRefine(target)) {
            rows.add(new Row(-1, Component.translatable("rngtech.phase_cradle.screen.no_target"), List.of(), false, false));
            return rows;
        }
        MachineTraits traits = targetTraits();
        PhaseArrayAction action = menu.action();
        RefinementSelection selection = menu.selection();
        boolean picking = selection.kind() == RefinementSelection.Kind.EMPTY_SLOT
                && (action == PhaseArrayAction.IMPRINT || action == PhaseArrayAction.GRAFT);
        if (picking) {
            rows.add(new Row(PhaseCradleMenu.BUTTON_SELECTION_NONE,
                    Component.translatable("rngtech.phase_cradle.screen.back").withStyle(ChatFormatting.GRAY), List.of(), false, true));
            if (action == PhaseArrayAction.IMPRINT) {
                List<ModifierDefinition> candidates = RefinementEngine.phaseImprintCandidates(
                        RefinementTargets.eligibilityProfile(target), traits, selection.emptySlot());
                for (int index = 0; index < candidates.size(); index++) {
                    ModifierDefinition definition = candidates.get(index);
                    rows.add(new Row(PhaseCradleMenu.BUTTON_DEFINITION_BASE + index, definitionName(definition),
                            List.of(definitionName(definition), Component.translatable(definition.stat().translationKey()).withStyle(ChatFormatting.GRAY)),
                            index == menu.definitionIndex(), true));
                }
            } else {
                List<MachineModifier> donor = PhaseCradleBlockEntity.donorAffixes(menu.donorStack());
                if (donor.isEmpty()) {
                    rows.add(new Row(-1, Component.translatable("rngtech.phase_cradle.screen.donor.missing"), List.of(), false, false));
                }
                for (int index = 0; index < donor.size(); index++) {
                    MachineModifier modifier = donor.get(index);
                    RefinementEngine.GraftBlock block = RefinementEngine.phaseGraftBlock(
                            RefinementTargets.eligibilityProfile(target), traits, modifier, selection.emptySlot());
                    List<Component> tooltip = new ArrayList<>(List.of(MachineModifierText.tooltipLine(modifier)));
                    if (block != RefinementEngine.GraftBlock.NONE) {
                        tooltip.add(Component.translatable("rngtech.phase_cradle.screen.donor." + block.name().toLowerCase(java.util.Locale.ROOT),
                                MachineModifierText.slotLabel(modifier.slot())).withStyle(ChatFormatting.RED));
                    }
                    rows.add(new Row(PhaseCradleMenu.BUTTON_DONOR_BASE + index, MachineModifierText.displayName(modifier),
                            tooltip, index == menu.donorAffixIndex(), block == RefinementEngine.GraftBlock.NONE));
                }
            }
            return rows;
        }
        boolean wantsAffix = action.selection() == PhaseArrayAction.Selection.AFFIX;
        boolean wantsSlot = action.selection() == PhaseArrayAction.Selection.EMPTY_SLOT_AND_DEFINITION
                || action.selection() == PhaseArrayAction.Selection.EMPTY_SLOT_AND_DONOR_AFFIX;
        List<MachineModifier> affixes = affixes(traits);
        for (int index = 0; index < affixes.size(); index++) {
            MachineModifier modifier = affixes.get(index);
            boolean selected = selection.kind() == RefinementSelection.Kind.EXISTING_MODIFIER && selection.affixIndex() == index;
            rows.add(new Row(PhaseCradleMenu.BUTTON_SELECTION_EXISTING_BASE + index, MachineModifierText.displayName(modifier),
                    List.of(MachineModifierText.tooltipLine(modifier)), selected, wantsAffix));
        }
        for (ModifierSlot slot : List.of(ModifierSlot.PREFIX, ModifierSlot.SUFFIX)) {
            if (RefinementEngine.hasOpenAffixSlot(traits, slot)) {
                boolean selected = selection.kind() == RefinementSelection.Kind.EMPTY_SLOT && selection.emptySlot() == slot;
                int button = slot == ModifierSlot.SUFFIX ? PhaseCradleMenu.BUTTON_SELECTION_EMPTY_SUFFIX : PhaseCradleMenu.BUTTON_SELECTION_EMPTY_PREFIX;
                rows.add(new Row(button, Component.translatable("rngtech.refinement.empty_slot", MachineModifierText.slotLabel(slot)).withStyle(ChatFormatting.GRAY),
                        List.of(Component.translatable("rngtech.refinement.select_empty_slot")), selected, wantsSlot));
            }
        }
        return rows;
    }

    private static Component definitionName(ModifierDefinition definition) {
        String slot = definition.slot() == ModifierSlot.SUFFIX ? "suffix" : "prefix";
        return Component.translatable("rngtech.affix." + slot + "." + definition.id());
    }

    private List<Row> visibleRows() {
        List<Row> rows = allRows();
        int max = Math.max(0, rows.size() - LIST_ROWS);
        scroll = Math.max(0, Math.min(scroll, max));
        return rows.subList(scroll, Math.min(rows.size(), scroll + LIST_ROWS));
    }

    private int hoveredRow(int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (!inside(x, y, LIST_X, LIST_Y, LIST_WIDTH, LIST_ROWS * ROW_HEIGHT)) {
            return -1;
        }
        int index = (y - LIST_Y) / ROW_HEIGHT;
        return index < visibleRows().size() ? index : -1;
    }

    // -- Button ----------------------------------------------------------------------------------------------------------

    private boolean buttonEnabled() {
        int status = menu.status();
        return status == PhaseCradleBlockEntity.STATUS_IDLE || status == PhaseCradleBlockEntity.STATUS_CHARGING
                || status == PhaseCradleBlockEntity.STATUS_READY;
    }

    private Component buttonLabel() {
        int status = menu.status();
        if (status == PhaseCradleBlockEntity.STATUS_PULSING) {
            return Component.translatable("rngtech.phase_cradle.screen.button.pulsing");
        }
        if (status == PhaseCradleBlockEntity.STATUS_READY) {
            return Component.translatable("rngtech.phase_cradle.screen.button.fire");
        }
        if (status == PhaseCradleBlockEntity.STATUS_CHARGING) {
            return Component.translatable("rngtech.phase_cradle.screen.button.stop");
        }
        return Component.translatable("rngtech.phase_cradle.screen.button.charge");
    }

    // -- Input -----------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (int) mouseX - leftPos;
            int y = (int) mouseY - topPos;
            PhaseArrayAction[] actions = PhaseArrayAction.values();
            for (int index = 0; index < actions.length; index++) {
                if (inside(x, y, OPERATION_X + index * OPERATION_SPACING, OPERATION_Y, OPERATION_SIZE, OPERATION_SIZE)) {
                    sendButton(PhaseCradleMenu.BUTTON_ACTION_BASE + index);
                    scroll = 0;
                    return true;
                }
            }
            if (inside(x, y, NODE_X, LINE_Y, PhaseArrayRules.MAX_COILS * NODE_SPACING, NODE_SIZE + 2) && !menu.pulsing()) {
                int position = (x - NODE_X) / NODE_SPACING + 1;
                if (position <= menu.coilCount() && position > menu.usableCoils()) {
                    sendButton(PhaseCradleMenu.BUTTON_INSCRIBE_BASE + position - 1);
                    return true;
                }
            }
            int row = hoveredRow((int) mouseX, (int) mouseY);
            if (row >= 0) {
                Row hit = visibleRows().get(row);
                if (hit.enabled() && hit.button() >= 0) {
                    sendButton(hit.button());
                    scroll = 0;
                }
                return true;
            }
            if (inside(x, y, BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT) && buttonEnabled()) {
                int status = menu.status();
                sendButton(status == PhaseCradleBlockEntity.STATUS_READY ? PhaseCradleMenu.BUTTON_FIRE : PhaseCradleMenu.BUTTON_CHARGE);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside((int) mouseX - leftPos, (int) mouseY - topPos, LIST_X, LIST_Y, LIST_WIDTH, LIST_ROWS * ROW_HEIGHT)) {
            scroll -= (int) Math.signum(scrollY);
            visibleRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null && minecraft.player != null && menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    // -- Tooltips --------------------------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderPanelTooltips(graphics, mouseX, mouseY);
    }

    private void renderPanelTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredSlot != null && hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            return;
        }
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        List<Component> lines = new ArrayList<>();
        if (inside(x, y, STATUS_X, STATUS_Y, 26, 6)) {
            lines.add(Component.translatable(PhaseCradleBlockEntity.statusKey(menu.status())));
        } else if (inside(x, y, EMITTER_X, LINE_Y, 10, 10)) {
            lines.add(Component.translatable("rngtech.phase_cradle.screen.emitter"));
            lines.add(lineLine());
            if (menu.formed()) {
                lines.add(gray("rngtech.phase_cradle.screen.emitter.capacity", CompactValueText.energyAmount(menu.chargeCapacity())));
                lines.add(gray("rngtech.phase_cradle.screen.emitter.rate", CompactValueText.energyRate(menu.chargeRate())));
                lines.add(gray("rngtech.phase_cradle.screen.emitter.window", menu.partialWindow()));
                lines.add(gray("rngtech.phase_cradle.screen.emitter.tolerance", menu.strainTolerance()));
            }
            PhaseLine.Scan scan = clientScan();
            if (scan != null && scan.emitter() != null) {
                affixLines(lines, scan.emitter());
            }
        } else if (inside(x, y, NODE_X, LINE_Y, PhaseArrayRules.MAX_COILS * NODE_SPACING, NODE_SIZE + 2)) {
            int position = (x - NODE_X) / NODE_SPACING + 1;
            lines.add(coilLine(position));
            lines.add(lineLine());
            if (position <= menu.usableCoils()) {
                lines.add(gray("rngtech.phase_cradle.screen.coil.second", percent(menu.secondHold(position))));
                lines.add(gray("rngtech.phase_cradle.screen.coil.overhold", percent(menu.overholdChance(position))));
            }
            PhaseLine.Scan scan = clientScan();
            if (scan != null && position <= scan.coilCount()) {
                affixLines(lines, scan.coilsFromEmitter().get(position - 1));
            }
        } else if (inside(x, y, CONDUIT_X, CONDUIT_Y, CONDUIT_WIDTH, CONDUIT_HEIGHT)) {
            lines.add(Component.translatable("rngtech.phase_cradle.screen.charge",
                    CompactValueText.exactEnergyAmountPair(menu.charge(), menu.cost()),
                    CompactValueText.energyRate(menu.lastInput()),
                    CompactValueText.energyRate(menu.chargeRate())));
            lines.add(chargeTimeLine());
        } else if (inside(x, y, PhaseCradleMenu.TARGET_X - 4, PhaseCradleMenu.TARGET_Y - 4, 26, 26) && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            lines.add(Component.translatable("rngtech.phase_cradle.screen.chamber"));
            lines.add(strainLine());
            lastPulseLines(lines);
        } else if (inside(x, y, PhaseCradleMenu.CELL_X - 2, PhaseCradleMenu.CELL_Y - 2, 3 * PhaseCradleMenu.CELL_SPACING + 2, 3 * PhaseCradleMenu.CELL_SPACING + 2)) {
            lines.add(Component.translatable("rngtech.phase_cradle.screen.cells"));
        } else if (inside(x, y, OPERATION_X, OPERATION_Y, PhaseArrayAction.values().length * OPERATION_SPACING, OPERATION_SIZE)) {
            int index = (x - OPERATION_X) / OPERATION_SPACING;
            if ((x - OPERATION_X) % OPERATION_SPACING < OPERATION_SIZE && index < PhaseArrayAction.values().length) {
                operationLines(lines, PhaseArrayAction.values()[index]);
            }
        } else if (inside(x, y, BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            lines.add(Component.translatable(PhaseCradleBlockEntity.statusKey(menu.status())));
            if (menu.status() == PhaseCradleBlockEntity.STATUS_READY || menu.status() == PhaseCradleBlockEntity.STATUS_CHARGING
                    || menu.status() == PhaseCradleBlockEntity.STATUS_IDLE) {
                lines.add(oddsLine());
                lines.add(strainLine());
            }
        } else {
            int row = hoveredRow(mouseX, mouseY);
            if (row >= 0) {
                lines.addAll(visibleRows().get(row).tooltip());
            }
        }
        if (!lines.isEmpty()) {
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    /** What the last pulse did, shown until a new target goes in; Partial and Overhold name their effect. */
    private Component resultLine() {
        PhasePulse.Outcome outcome = menu.lastOutcome();
        PhaseArrayAction action = menu.lastAction();
        if (outcome == null || action == null || menu.pulsing() || !menu.targetStack().isEmpty()) {
            return null;
        }
        Component name = Component.translatable(outcome.translationKey());
        PhaseArrayRecipe recipe = recipeFor(action);
        return switch (outcome) {
            case PARTIAL -> recipe == null ? name : Component.translatable("rngtech.phase_cradle.screen.result.effect", name,
                    Component.translatable("rngtech.phase_array.partial." + recipe.partial().getSerializedName()));
            case OVERHOLD -> recipe == null ? name : Component.translatable("rngtech.phase_cradle.screen.result.effect", name,
                    Component.translatable("rngtech.phase_array.overhold." + recipe.overhold().getSerializedName()));
            case SUCCESS -> Component.translatable("rngtech.phase_cradle.screen.result.effect", name, Component.translatable(action.translationKey()));
            case MISS -> Component.translatable("rngtech.phase_cradle.screen.result.miss", name, menu.lastSlipped());
            case FRACTURE -> Component.translatable("rngtech.phase_cradle.screen.result.fracture", name);
        };
    }

    private int resultColor() {
        PhasePulse.Outcome outcome = menu.lastOutcome();
        if (outcome == null) {
            return TEXT;
        }
        return switch (outcome) {
            case SUCCESS, OVERHOLD -> GREEN;
            case PARTIAL -> AMBER;
            case MISS -> DIM;
            case FRACTURE -> RED;
        };
    }

    private PhaseArrayRecipe recipeFor(PhaseArrayAction action) {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getRecipeManager().getAllRecipesFor(ModRecipes.PHASE_ARRAY_TYPE.get()).stream()
                .map(holder -> holder.value())
                .filter(recipe -> recipe.action() == action)
                .findFirst()
                .orElse(null);
    }

    private Component lineLine() {
        if (!menu.formed()) {
            return Component.translatable("rngtech.phase_cradle.screen.line.missing").withStyle(ChatFormatting.RED);
        }
        return Component.translatable("rngtech.phase_cradle.screen.line", menu.coilCount(), menu.usableCoils()).withStyle(ChatFormatting.GRAY);
    }

    private Component coilLine(int position) {
        int floor = PhaseArrayRules.lowestGlyphTier(position);
        if (position > menu.coilCount()) {
            return Component.translatable("rngtech.phase_cradle.screen.coil.absent", position).withStyle(ChatFormatting.GRAY);
        }
        if (position <= menu.usableCoils()) {
            return Component.translatable("rngtech.phase_cradle.screen.coil", position, percent(menu.holdChance(position)));
        }
        int lockTier = minecraft != null && minecraft.player != null
                ? PhaseCradleBlockEntity.lockInTier(position, minecraft.player.getInventory())
                : 0;
        Component hint = lockTier > 0
                ? Component.translatable("rngtech.phase_cradle.screen.coil.lock", GlyphItem.numeral(lockTier))
                : Component.translatable("rngtech.phase_cradle.screen.coil.lock_missing", GlyphItem.numeral(floor));
        if (position == menu.usableCoils() + 1) {
            return Component.translatable("rngtech.phase_cradle.screen.coil.blank", position, GlyphItem.numeral(floor)).withStyle(ChatFormatting.GOLD)
                    .append(" ").append(hint.copy().withStyle(ChatFormatting.GRAY));
        }
        return Component.translatable("rngtech.phase_cradle.screen.coil.low", position, GlyphItem.numeral(floor)).withStyle(ChatFormatting.GOLD)
                .append(" ").append(hint.copy().withStyle(ChatFormatting.GRAY));
    }

    private static Component gray(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    /** The line as the client sees it, for the parts' affixes; the numbers themselves come from the menu. */
    private PhaseLine.Scan clientScan() {
        PhaseCradleBlockEntity cradle = menu.cradle();
        if (cradle == null || minecraft == null || minecraft.level == null) {
            return null;
        }
        return PhaseLine.scan(minecraft.level, cradle.getBlockPos());
    }

    private void affixLines(List<Component> lines, BlockPos pos) {
        if (minecraft == null || minecraft.level == null
                || !(minecraft.level.getBlockEntity(pos) instanceof BaseMachineBlockEntity part)) {
            return;
        }
        for (MachineModifier modifier : affixes(part.machineTraits())) {
            lines.add(Component.literal("  ").append(MachineModifierText.tooltipLine(modifier)));
        }
    }

    /** How long the charge takes to fill at the rate FE arrived last tick. */
    private Component chargeTimeLine() {
        long remaining = menu.cost() - menu.charge();
        if (menu.cost() <= 0 || remaining <= 0) {
            return gray("rngtech.phase_cradle.screen.charge.full");
        }
        int rate = menu.lastInput();
        if (rate <= 0) {
            return Component.translatable("rngtech.phase_cradle.screen.charge.stalled").withStyle(ChatFormatting.GOLD);
        }
        long seconds = Math.max(1L, (remaining + rate * 20L - 1) / (rate * 20L));
        return gray("rngtech.phase_cradle.screen.charge.time", duration(seconds));
    }

    private static String duration(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m " + (seconds % 60) + "s";
        }
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private Component strainLine() {
        PhasePulse.Odds odds = menu.odds();
        return Component.translatable("rngtech.phase_cradle.screen.strain", menu.strain(), menu.strainTolerance(), percent(odds.fracture()))
                .withStyle(menu.strain() > menu.strainTolerance() ? ChatFormatting.GOLD : ChatFormatting.GRAY);
    }

    private Component oddsLine() {
        PhasePulse.Odds odds = menu.odds();
        return Component.translatable("rngtech.phase_cradle.screen.odds", percent(odds.success()), percent(odds.overhold()),
                percent(odds.partial()), percent(odds.miss()), percent(odds.fracture()));
    }

    private void lastPulseLines(List<Component> lines) {
        PhasePulse.Outcome outcome = menu.lastOutcome();
        if (outcome == null) {
            return;
        }
        lines.add(Component.translatable("rngtech.phase_cradle.screen.last", Component.translatable(outcome.translationKey()), menu.lastHeld())
                .withStyle(ChatFormatting.GRAY));
        if (menu.lastSlipped() > 0) {
            lines.add(Component.translatable("rngtech.phase_cradle.screen.last.slipped", menu.lastSlipped()).withStyle(ChatFormatting.GRAY));
        }
    }

    private void operationLines(List<Component> lines, PhaseArrayAction action) {
        lines.add(Component.translatable(action.translationKey()));
        lines.add(Component.translatable(action.translationKey() + ".description").withStyle(ChatFormatting.GRAY));
        if (action == menu.action()) {
            lines.add(Component.translatable("rngtech.phase_cradle.screen.operation.coils", menu.requiredCoils()).withStyle(ChatFormatting.AQUA));
            if (menu.glyphTier() > 0) {
                lines.add(Component.translatable("rngtech.phase_cradle.screen.operation.glyph", GlyphItem.numeral(menu.glyphTier())).withStyle(ChatFormatting.AQUA));
            }
            lines.add(Component.translatable("rngtech.phase_cradle.screen.operation.rp", menu.rpCost()).withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("rngtech.phase_cradle.screen.operation.fe", CompactValueText.exactEnergyAmount(menu.cost())).withStyle(ChatFormatting.AQUA));
            lines.add(oddsLine());
        }
    }

    private static String percent(double fraction) {
        return String.format("%.0f%%", fraction * 100.0);
    }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static int lerpColor(int from, int to, double amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        int a = 0xFF;
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
