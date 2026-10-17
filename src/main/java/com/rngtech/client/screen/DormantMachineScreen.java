package com.rngtech.client.screen;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.dormant.DormantPhase;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.menu.DormantMachineMenu;
import com.rngtech.rpg.progression.MachineMasteryFamily;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Dormant Machine screen breaks from the gray machine panel on purpose: a worn gunmetal plate with recessed cell
 * sockets, a glass wake conduit, and a soul chamber whose glyph ring lights as the charge rises. Everything is drawn
 * from fills, so the colors live here and nowhere else.
 */
public class DormantMachineScreen extends AbstractContainerScreen<DormantMachineMenu> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 196;

    private static final int PLATE_TOP = 0xFF30353B;
    private static final int PLATE_BOTTOM = 0xFF1C2024;
    private static final int BEZEL_LIGHT = 0xFF4A525B;
    private static final int BEZEL_DARK = 0xFF0C0E10;
    private static final int TRACE = 0xFF262B30;
    private static final int TRACE_LIT = 0xFF3E8C9C;
    private static final int RIVET = 0xFF59626C;
    private static final int SOCKET = 0xFF111316;
    private static final int TEXT = 0xFFC9D3DC;
    private static final int SOUL = 0xFFCFF6FF;
    private static final int SOUL_GLOW = 0xFF7FE8FF;
    private static final int GLYPH_DARK = 0xFF2A3036;
    private static final int AMBER = 0xFFE3A23A;
    private static final int RED = 0xFFD0473A;

    private static final int GAUGE_X = 13;
    private static final int GAUGE_Y = 22;
    private static final int GAUGE_WIDTH = 55;
    private static final int CONDUIT_X = 76;
    private static final int CONDUIT_Y = 57;
    private static final int CONDUIT_WIDTH = 46;
    private static final int CONDUIT_HEIGHT = 9;
    private static final int STATUS_Y = 46;
    private static final int CHAMBER_X = 131;
    private static final int CHAMBER_Y = 24;
    private static final int CHAMBER_WIDTH = 30;
    private static final int CHAMBER_HEIGHT = 58;
    private static final int GLYPHS = 12;
    private static final int PIPS_Y = 92;
    private static final int HAZARD_X = 154;
    private static final int HAZARD_Y = 5;

    private DormantPhase shownPhase;
    private int flareTicks;
    private int ticks;

    public DormantMachineScreen(DormantMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = DormantMachineMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ticks++;
        DormantPhase phase = menu.phase();
        if (shownPhase == DormantPhase.ASLEEP && phase == DormantPhase.AWAKE) { flareTicks = 20; }
        shownPhase = phase;
        if (flareTicks > 0) { flareTicks--; }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderPanelTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        float time = ticks + partialTick;
        drawPlate(graphics, x, y);
        drawTraces(graphics, x, y);
        drawTitlePlate(graphics, x, y);
        drawGauge(graphics, x, y);
        for (Slot slot : menu.slots) {
            drawSocket(graphics, x + slot.x - 1, y + slot.y - 1, slot.index < 9 ? cellRing(slot.getItem()) : 0);
        }
        drawStatus(graphics, x, y);
        drawConduit(graphics, x, y, time);
        drawLevel(graphics, x, y);
        drawChamber(graphics, x, y, time);
        drawPips(graphics, x, y);
        if (menu.failedAbsorb() != RNGTechConfig.DormantFailedAbsorb.NOTHING) { drawHazard(graphics, x + HAZARD_X, y + HAZARD_Y); }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 11, 7, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF8A949E, false);
    }

    private void drawPlate(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, BEZEL_DARK);
        graphics.fillGradient(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, PLATE_TOP, PLATE_BOTTOM);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + 2, BEZEL_LIGHT);
        graphics.fill(x + 1, y + 1, x + 2, y + HEIGHT - 1, BEZEL_LIGHT);
        graphics.fill(x + 3, y + 104, x + WIDTH - 3, y + 105, BEZEL_DARK);
        graphics.fill(x + 3, y + 105, x + WIDTH - 3, y + 106, BEZEL_LIGHT);
        for (int[] rivet : new int[][] {{4, 4}, {WIDTH - 6, 4}, {4, HEIGHT - 6}, {WIDTH - 6, HEIGHT - 6}, {4, 101}, {WIDTH - 6, 101}}) {
            graphics.fill(x + rivet[0], y + rivet[1], x + rivet[0] + 2, y + rivet[1] + 2, RIVET);
            graphics.fill(x + rivet[0] + 1, y + rivet[1] + 1, x + rivet[0] + 2, y + rivet[1] + 2, BEZEL_DARK);
        }
        // Worn edges: a few nicks in the bezel.
        for (int nick : new int[] {37, 88, 141}) {
            graphics.fill(x + nick, y + 1, x + nick + 3, y + 2, PLATE_TOP);
        }
    }

    /** Etched circuit traces; the ones leading from the cell bank to the conduit light up while charge flows. */
    private void drawTraces(GuiGraphics graphics, int x, int y) {
        boolean flowing = menu.lastInput() > 0 && menu.phase() == DormantPhase.ASLEEP;
        int lit = menu.phase() == DormantPhase.AWAKE || flowing ? TRACE_LIT : TRACE;
        graphics.fill(x + 69, y + 61, x + CONDUIT_X, y + 62, lit);
        graphics.fill(x + 69, y + 44, x + 70, y + 80, lit);
        graphics.fill(x + CONDUIT_X + CONDUIT_WIDTH, y + 61, x + CHAMBER_X - 1, y + 62, menu.phase() == DormantPhase.AWAKE ? TRACE_LIT : TRACE);
        graphics.fill(x + 8, y + 95, x + 70, y + 96, TRACE);
        graphics.fill(x + 100, y + 72, x + 101, y + 99, TRACE);
        graphics.fill(x + 100, y + 99, x + 126, y + 100, TRACE);
    }

    private void drawTitlePlate(GuiGraphics graphics, int x, int y) {
        int width = Math.min(font.width(title) + 8, HAZARD_X - 10);
        graphics.fill(x + 7, y + 4, x + 7 + width, y + 17, BEZEL_DARK);
        graphics.fill(x + 8, y + 5, x + 6 + width, y + 16, 0xFF252A2F);
        graphics.fill(x + 8, y + 15, x + 6 + width, y + 16, BEZEL_LIGHT);
    }

    /** A small input gauge: the needle sits at this tick's FE input against the site's wake input rate. */
    private void drawGauge(GuiGraphics graphics, int x, int y) {
        int left = x + GAUGE_X;
        int top = y + GAUGE_Y;
        graphics.fill(left - 1, top - 1, left + GAUGE_WIDTH + 1, top + 7, BEZEL_DARK);
        graphics.fill(left, top, left + GAUGE_WIDTH, top + 6, SOCKET);
        for (int tick = 0; tick <= 10; tick++) {
            int tx = left + tick * (GAUGE_WIDTH - 1) / 10;
            graphics.fill(tx, top, tx + 1, top + (tick % 5 == 0 ? 3 : 2), 0xFF3A4249);
        }
        float share = menu.wakeInput() <= 0 ? 0.0F : Math.min(1.0F, menu.lastInput() / (float) menu.wakeInput());
        int needle = left + Math.round(share * (GAUGE_WIDTH - 1));
        graphics.fill(needle, top, needle + 1, top + 6, menu.lastInput() > 0 ? AMBER : 0xFF6B747D);
    }

    private void drawSocket(GuiGraphics graphics, int x, int y, int ring) {
        graphics.fill(x, y, x + 18, y + 18, BEZEL_DARK);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, BEZEL_LIGHT);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, SOCKET);
        if (ring != 0) {
            graphics.fill(x + 1, y + 1, x + 17, y + 2, ring);
            graphics.fill(x + 1, y + 16, x + 17, y + 17, ring);
            graphics.fill(x + 1, y + 2, x + 2, y + 16, ring);
            graphics.fill(x + 16, y + 2, x + 17, y + 16, ring);
        }
    }

    /** A cell's charge ring: bright while it holds charge, a dim ember once it is empty. */
    private static int cellRing(ItemStack stack) {
        if (!BatteryCellItem.isBatteryCell(stack)) { return 0; }
        int capacity = BatteryCellItem.energyCapacity(stack);
        float share = capacity <= 0 ? 0.0F : BatteryCellItem.energyStored(stack) / (float) capacity;
        if (share <= 0.0F) { return 0xFF3A2A22; }
        return lerpColor(0xFF1E5662, SOUL_GLOW, share);
    }

    /** Three lights over the conduit, told apart by shape: no input, input capped, awake. */
    private void drawStatus(GuiGraphics graphics, int x, int y) {
        DormantPhase phase = menu.phase();
        boolean noInput = phase == DormantPhase.ASLEEP && menu.lastInput() <= 0;
        boolean capped = phase == DormantPhase.ASLEEP && menu.wakeInput() > 0 && menu.lastInput() >= menu.wakeInput();
        boolean awake = phase == DormantPhase.AWAKE;
        int left = x + CONDUIT_X + 10;
        int top = y + STATUS_Y;
        // Triangle: no power coming in.
        int triangle = noInput ? RED : GLYPH_DARK;
        for (int row = 0; row < 5; row++) {
            graphics.fill(left + 2 - row / 2, top + row, left + 3 + row / 2, top + row + 1, triangle);
        }
        // Square: the site's input rate is the limit.
        graphics.fill(left + 10, top, left + 15, top + 5, capped ? AMBER : GLYPH_DARK);
        // Circle: awake.
        int circle = awake ? SOUL_GLOW : GLYPH_DARK;
        graphics.fill(left + 21, top, left + 24, top + 5, circle);
        graphics.fill(left + 20, top + 1, left + 25, top + 4, circle);
    }

    /** The glass wake conduit fills left to right; light runs through it while charge flows. */
    private void drawConduit(GuiGraphics graphics, int x, int y, float time) {
        int left = x + CONDUIT_X;
        int top = y + CONDUIT_Y;
        graphics.fill(left - 1, top - 1, left + CONDUIT_WIDTH + 1, top + CONDUIT_HEIGHT + 1, BEZEL_DARK);
        graphics.fill(left, top, left + CONDUIT_WIDTH, top + CONDUIT_HEIGHT, 0xFF0E1215);
        float fraction = menu.phase() == DormantPhase.AWAKE ? 1.0F : menu.chargeFraction();
        int filled = Math.round(fraction * CONDUIT_WIDTH);
        if (filled > 0) {
            graphics.fillGradient(left, top + 1, left + filled, top + CONDUIT_HEIGHT - 1, SOUL_GLOW, 0xFF2E8FA3);
            boolean moving = menu.lastInput() > 0 || menu.phase() == DormantPhase.AWAKE;
            int offset = moving ? (int) (time * 0.75F) % 6 : 0;
            for (int stripe = left - 6 + offset; stripe < left + filled; stripe += 6) {
                int start = Math.max(left, stripe);
                int end = Math.min(left + filled, stripe + 2);
                if (end > start) { graphics.fill(start, top + 2, end, top + CONDUIT_HEIGHT - 2, 0x66FFFFFF); }
            }
        }
        graphics.fill(left, top, left + CONDUIT_WIDTH, top + 1, 0x55FFFFFF);
        // End caps.
        graphics.fill(left - 3, top - 2, left - 1, top + CONDUIT_HEIGHT + 2, RIVET);
        graphics.fill(left + CONDUIT_WIDTH + 1, top - 2, left + CONDUIT_WIDTH + 3, top + CONDUIT_HEIGHT + 2, RIVET);
    }

    private void drawLevel(GuiGraphics graphics, int x, int y) {
        if (menu.soulLevel() <= 0) { return; }
        String digits = String.valueOf(menu.soulLevel());
        int width = font.width(digits) + 6;
        int left = x + CONDUIT_X + (CONDUIT_WIDTH - width) / 2;
        int top = y + CONDUIT_Y + CONDUIT_HEIGHT + 5;
        graphics.fill(left, top, left + width, top + 12, BEZEL_DARK);
        graphics.fill(left + 1, top + 1, left + width - 1, top + 11, 0xFF0B1A1E);
        int color = menu.phase() == DormantPhase.AWAKE ? SOUL : SOUL_GLOW;
        graphics.drawString(font, digits, left + 3, top + 2, color, false);
    }

    /** The glass vessel, its glyph ring, and the wisp inside. */
    private void drawChamber(GuiGraphics graphics, int x, int y, float time) {
        int left = x + CHAMBER_X;
        int top = y + CHAMBER_Y;
        int right = left + CHAMBER_WIDTH;
        int bottom = top + CHAMBER_HEIGHT;
        DormantPhase phase = menu.phase();
        float fraction = phase == DormantPhase.AWAKE ? 1.0F : menu.chargeFraction();

        drawGlyphRing(graphics, left, top, right, bottom, fraction);
        graphics.fill(left, top, right, bottom, BEZEL_DARK);
        graphics.fillGradient(left + 1, top + 1, right - 1, bottom - 1, 0xFF0D1417, 0xFF050809);
        graphics.fill(left + 2, top + 2, left + 3, bottom - 2, 0x33FFFFFF);
        graphics.fill(left + 1, top + 4, right - 1, top + 5, BEZEL_LIGHT);
        graphics.fill(left + 1, bottom - 5, right - 1, bottom - 4, BEZEL_LIGHT);

        float brightness = switch (phase) {
            case ASLEEP -> 0.25F + fraction * 0.55F;
            case AWAKE -> 0.85F + 0.15F * Mth.sin(time * 0.15F);
            case SPENT -> 0.0F;
        };
        if (flareTicks > 0) { brightness = 1.0F; }
        if (brightness <= 0.0F || menu.soulLevel() <= 0) { return; }
        float speed = phase == DormantPhase.AWAKE ? 0.08F : 0.01F + fraction * 0.05F;
        int centerX = left + CHAMBER_WIDTH / 2;
        int centerY = top + CHAMBER_HEIGHT / 2;
        for (int mote = 0; mote < 14; mote++) {
            float phaseShift = mote * 0.9F;
            int mx = centerX + Math.round(Mth.sin(time * speed + phaseShift) * (3 + mote % 4));
            int my = centerY + Math.round(Mth.cos(time * speed * 0.7F + phaseShift * 1.3F) * (6 + mote % 7) * 1.4F);
            int alpha = Mth.clamp(Math.round(brightness * (110 + mote * 9)), 0, 255);
            int color = alpha << 24 | (mote % 3 == 0 ? SOUL : SOUL_GLOW) & 0xFFFFFF;
            graphics.fill(mx, my, mx + 1 + mote % 2, my + 1 + mote % 2, color);
        }
        int core = Mth.clamp(Math.round(brightness * 200), 0, 255) << 24 | SOUL & 0xFFFFFF;
        graphics.fill(centerX - 1, centerY - 2, centerX + 2, centerY + 3, core);
        if (flareTicks > 0) {
            int flare = Mth.clamp(flareTicks * 10, 0, 200) << 24 | SOUL & 0xFFFFFF;
            graphics.fill(left + 1, top + 1, right - 1, bottom - 1, flare);
        }
    }

    /** Twelve etched glyphs around the vessel; they light one by one as the charge rises and close when it wakes. */
    private void drawGlyphRing(GuiGraphics graphics, int left, int top, int right, int bottom, float fraction) {
        int lit = Math.round(fraction * GLYPHS);
        for (int glyph = 0; glyph < GLYPHS; glyph++) {
            int[] at = glyphPosition(glyph, left, top, right, bottom);
            int color = glyph < lit ? (flareTicks > 0 ? 0xFFFFFFFF : SOUL_GLOW) : GLYPH_DARK;
            switch (glyph % 3) {
                case 0 -> {
                    graphics.fill(at[0], at[1], at[0] + 3, at[1] + 1, color);
                    graphics.fill(at[0] + 1, at[1] + 1, at[0] + 2, at[1] + 3, color);
                }
                case 1 -> {
                    graphics.fill(at[0], at[1], at[0] + 1, at[1] + 3, color);
                    graphics.fill(at[0] + 2, at[1], at[0] + 3, at[1] + 3, color);
                    graphics.fill(at[0] + 1, at[1] + 1, at[0] + 2, at[1] + 2, color);
                }
                default -> {
                    graphics.fill(at[0] + 1, at[1], at[0] + 2, at[1] + 3, color);
                    graphics.fill(at[0], at[1] + 2, at[0] + 3, at[1] + 3, color);
                }
            }
        }
    }

    /** Glyphs run clockwise around the vessel: three along the top, three down each side, three along the bottom. */
    private static int[] glyphPosition(int glyph, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        return switch (glyph / 3) {
            case 0 -> new int[] {left + 4 + glyph % 3 * (width - 8) / 2 - 1, top - 5};
            case 1 -> new int[] {right + 2, top + 8 + glyph % 3 * (height - 16) / 2};
            case 2 -> new int[] {right - 4 - glyph % 3 * (width - 8) / 2 - 2, bottom + 2};
            default -> new int[] {left - 5, bottom - 8 - glyph % 3 * (height - 16) / 2 - 3};
        };
    }

    private void drawPips(GuiGraphics graphics, int x, int y) {
        int stage = menu.releaseStage();
        int left = x + CHAMBER_X - 1;
        int top = y + PIPS_Y;
        for (int pip = 0; pip < 8; pip++) {
            int px = left + pip * 4;
            graphics.fill(px, top, px + 3, top + 3, pip < stage ? AMBER : GLYPH_DARK);
        }
    }

    private void drawHazard(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 15, y + 12, BEZEL_DARK);
        for (int column = 0; column < 13; column++) {
            for (int row = 0; row < 10; row++) {
                boolean stripe = (column + row) / 3 % 2 == 0;
                graphics.fill(x + 1 + column, y + 1 + row, x + 2 + column, y + 2 + row, stripe ? AMBER : 0xFF16181A);
            }
        }
        graphics.fill(x + 1, y + 1, x + 2, y + 2, RIVET);
        graphics.fill(x + 13, y + 1, x + 14, y + 2, RIVET);
    }

    private void renderPanelTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.hasItem()) { return; }
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        List<Component> lines = new ArrayList<>();
        if (inside(localX, localY, CHAMBER_X - 6, CHAMBER_Y - 6, CHAMBER_WIDTH + 12, CHAMBER_HEIGHT + 12)) {
            chamberTooltip(lines);
        } else if (inside(localX, localY, CONDUIT_X - 3, CONDUIT_Y - 2, CONDUIT_WIDTH + 6, CONDUIT_HEIGHT + 4)) {
            lines.add(Component.translatable("rngtech.dormant_machine.screen.charge"));
            lines.add(CompactValueText.exactEnergyAmountPair(menu.charge(), menu.wakeFe()).copy().withStyle(ChatFormatting.GRAY));
        } else if (inside(localX, localY, GAUGE_X - 1, GAUGE_Y - 1, GAUGE_WIDTH + 2, 8)) {
            lines.add(Component.translatable("rngtech.dormant_machine.screen.input"));
            lines.add(Component.literal(CompactValueText.energyRate(menu.lastInput()) + " / " + CompactValueText.energyRate(menu.wakeInput()))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("rngtech.dormant_machine.screen.cells", CompactValueText.energyRate(menu.cellOutput()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else if (inside(localX, localY, CONDUIT_X + 9, STATUS_Y - 1, 7, 7)) {
            lines.add(Component.translatable("rngtech.dormant_machine.screen.status.no_input"));
        } else if (inside(localX, localY, CONDUIT_X + 19, STATUS_Y - 1, 7, 7)) {
            lines.add(Component.translatable("rngtech.dormant_machine.screen.status.capped"));
        } else if (inside(localX, localY, CONDUIT_X + 29, STATUS_Y - 1, 7, 7)) {
            lines.add(Component.translatable("rngtech.dormant_machine.screen.status.awake"));
        } else if (inside(localX, localY, CHAMBER_X - 1, PIPS_Y - 1, 33, 5)) {
            lines.add(releaseLine());
        } else if (menu.failedAbsorb() != RNGTechConfig.DormantFailedAbsorb.NOTHING && inside(localX, localY, HAZARD_X, HAZARD_Y, 15, 12)) {
            lines.add(Component.translatable("rngtech.dormant_machine.screen.hazard").withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("rngtech.dormant_machine.screen.hazard."
                    + menu.failedAbsorb().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        }
        if (!lines.isEmpty()) {
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private void chamberTooltip(List<Component> lines) {
        MachineMasteryFamily family = menu.family();
        if (family == null || menu.phase() == DormantPhase.SPENT) {
            lines.add(Component.translatable("rngtech.dormant_machine.screen.empty"));
            return;
        }
        lines.add(Component.translatable("rngtech.dormant_machine.screen.soul", menu.soulLevel(), Component.translatable(family.translationKey()))
                .withStyle(ChatFormatting.AQUA));
        lines.add(releaseLine());
        if (menu.phase() == DormantPhase.AWAKE) {
            lines.add(Component.translatable("rngtech.dormant_machine.screen.ready").withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("rngtech.dormant_machine.screen.remaining",
                    CompactValueText.energyAmount(Math.max(0L, menu.wakeFe() - menu.charge()))).withStyle(ChatFormatting.GRAY));
        }
    }

    private Component releaseLine() {
        return menu.releaseStage() <= 0
                ? Component.translatable("rngtech.dormant_machine.screen.release_any").withStyle(ChatFormatting.GRAY)
                : Component.translatable("rngtech.dormant_machine.screen.release", menu.releaseStage()).withStyle(ChatFormatting.GRAY);
    }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static int lerpColor(int from, int to, float amount) {
        int a = Math.round(Mth.lerp(amount, from >>> 24, to >>> 24));
        int r = Math.round(Mth.lerp(amount, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(amount, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int b = Math.round(Mth.lerp(amount, from & 0xFF, to & 0xFF));
        return a << 24 | r << 16 | g << 8 | b;
    }
}
