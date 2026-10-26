package com.rngtech.content.menu;

import com.rngtech.content.block.PhaseCradleBlock;
import com.rngtech.content.blockentity.PhaseCradleBlockEntity;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.GlyphItem;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.PhaseArrayRules;
import com.rngtech.rpg.refinement.PhaseArrayAction;
import com.rngtech.rpg.refinement.PhasePulse;
import com.rngtech.rpg.refinement.PhasePulseSequence;
import com.rngtech.rpg.refinement.RefinementSelection;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The Phase Cradle's one-panel menu: target, Glyph, donor, output, nine cell sockets, and the player's inventory. */
public class PhaseCradleMenu extends AbstractContainerMenu {
    public static final int WIDTH = 256;
    public static final int HEIGHT = 262;
    public static final int TARGET_X = 226;
    public static final int TARGET_Y = 24;
    public static final int OUTPUT_X = 226;
    public static final int OUTPUT_Y = 50;
    public static final int GLYPH_X = 204;
    public static final int GLYPH_Y = 50;
    public static final int DONOR_X = 182;
    public static final int DONOR_Y = 50;
    public static final int CELL_X = 12;
    public static final int CELL_Y = 40;
    public static final int CELL_SPACING = 19;
    /** The five Glyph seats, a column under the output slot, seat I at the top. */
    public static final int SEAT_X = 226;
    public static final int SEAT_Y = 72;
    public static final int SEAT_SPACING = 18;
    public static final int INVENTORY_X = 48;
    public static final int INVENTORY_Y = 180;
    public static final int HOTBAR_Y = 238;

    public static final int BUTTON_ACTION_BASE = 100;
    public static final int BUTTON_SELECTION_NONE = 190;
    public static final int BUTTON_SELECTION_EXISTING_BASE = 200;
    public static final int BUTTON_SELECTION_EMPTY_PREFIX = 300;
    public static final int BUTTON_SELECTION_EMPTY_SUFFIX = 301;
    public static final int BUTTON_DEFINITION_BASE = 400;
    public static final int BUTTON_DONOR_BASE = 500;
    public static final int BUTTON_CHARGE = 600;
    public static final int BUTTON_FIRE = 601;
    /** The Cradle's own slots come first in menu order: target, Glyph, donor, output, the nine cell sockets, then the five seats. */
    public static final int MACHINE_SLOTS = PhaseCradleBlockEntity.SLOT_COUNT + PhaseCradleBlockEntity.CELL_SLOTS + PhaseCradleBlockEntity.GLYPH_SEATS;

    private static final int ITEM_SLOTS = PhaseCradleBlockEntity.SLOT_COUNT;
    private static final int CELL_SLOT_START = ITEM_SLOTS;
    public static final int SEAT_SLOT_START = CELL_SLOT_START + PhaseCradleBlockEntity.CELL_SLOTS;
    private static final int PLAYER_SLOT_START = SEAT_SLOT_START + PhaseCradleBlockEntity.GLYPH_SEATS;
    private static final int PLAYER_SLOT_END = PLAYER_SLOT_START + 36;

    private final PhaseCradleBlockEntity cradle;
    private final ContainerData data;

    public PhaseCradleMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, cradleAt(inventory, extraData.readBlockPos()), new SimpleContainerData(PhaseCradleBlockEntity.DATA_COUNT));
    }

    public PhaseCradleMenu(int containerId, Inventory inventory, PhaseCradleBlockEntity cradle, ContainerData data) {
        super(ModMenus.EXOTIC_AFFIX_FORGE.get(), containerId);
        this.cradle = cradle;
        this.data = data;
        checkContainerDataCount(data, PhaseCradleBlockEntity.DATA_COUNT);
        ItemStackHandler items = cradle == null ? new ItemStackHandler(ITEM_SLOTS) : cradle.getItems();
        ItemStackHandler cells = cradle == null ? new ItemStackHandler(PhaseCradleBlockEntity.CELL_SLOTS) : cradle.getCells();
        ItemStackHandler seats = cradle == null ? new ItemStackHandler(PhaseCradleBlockEntity.GLYPH_SEATS) : cradle.getSeats();
        addSlot(new SlotItemHandler(items, PhaseCradleBlockEntity.SLOT_TARGET, TARGET_X, TARGET_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return RefinementTargets.canPhase(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new SlotItemHandler(items, PhaseCradleBlockEntity.SLOT_GLYPH, GLYPH_X, GLYPH_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GlyphItem.isGlyph(stack);
            }
        });
        addSlot(new SlotItemHandler(items, PhaseCradleBlockEntity.SLOT_DONOR, DONOR_X, DONOR_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return RefinementTargets.canPhase(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new SlotItemHandler(items, PhaseCradleBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int slot = 0; slot < PhaseCradleBlockEntity.CELL_SLOTS; slot++) {
            addSlot(new SlotItemHandler(cells, slot, CELL_X + slot % 3 * CELL_SPACING, CELL_Y + slot / 3 * CELL_SPACING) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return BatteryCellItem.isBatteryCell(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int seat = 0; seat < PhaseCradleBlockEntity.GLYPH_SEATS; seat++) {
            int tierSeat = seat;
            addSlot(new SlotItemHandler(seats, seat, SEAT_X, SEAT_Y + seat * SEAT_SPACING) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return PhaseCradleBlockEntity.acceptsSeat(tierSeat, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    private static PhaseCradleBlockEntity cradleAt(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof PhaseCradleBlockEntity cradle ? cradle : null;
    }

    // -- Buttons -------------------------------------------------------------------------------------------------------

    public static int selectionCode(RefinementSelection selection) {
        return switch (selection.kind()) {
            case NONE -> BUTTON_SELECTION_NONE;
            case EXISTING_MODIFIER -> BUTTON_SELECTION_EXISTING_BASE + selection.affixIndex();
            case EMPTY_SLOT -> selection.emptySlot() == ModifierSlot.SUFFIX ? BUTTON_SELECTION_EMPTY_SUFFIX : BUTTON_SELECTION_EMPTY_PREFIX;
        };
    }

    public static RefinementSelection selectionFromCode(int code) {
        if (code == BUTTON_SELECTION_EMPTY_PREFIX) {
            return RefinementSelection.emptySlot(ModifierSlot.PREFIX);
        }
        if (code == BUTTON_SELECTION_EMPTY_SUFFIX) {
            return RefinementSelection.emptySlot(ModifierSlot.SUFFIX);
        }
        if (code >= BUTTON_SELECTION_EXISTING_BASE && code < BUTTON_SELECTION_EMPTY_PREFIX) {
            return RefinementSelection.existingModifier(code - BUTTON_SELECTION_EXISTING_BASE);
        }
        return RefinementSelection.none();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (cradle == null || player.level().isClientSide) {
            return true;
        }
        if (cradle.isPulsing()) {
            return true;
        }
        if (id >= BUTTON_ACTION_BASE && id < BUTTON_ACTION_BASE + PhaseArrayAction.values().length) {
            cradle.setAction(PhaseArrayAction.values()[id - BUTTON_ACTION_BASE]);
        } else if (id >= BUTTON_SELECTION_NONE && id < BUTTON_DEFINITION_BASE) {
            cradle.setSelection(selectionFromCode(id));
        } else if (id >= BUTTON_DEFINITION_BASE && id < BUTTON_DONOR_BASE) {
            cradle.setDefinitionIndex(id - BUTTON_DEFINITION_BASE);
        } else if (id >= BUTTON_DONOR_BASE && id < BUTTON_CHARGE) {
            cradle.setDonorAffixIndex(id - BUTTON_DONOR_BASE);
        } else if (id == BUTTON_CHARGE) {
            cradle.toggleCharging();
        } else if (id == BUTTON_FIRE) {
            cradle.fire();
        } else {
            return false;
        }
        return true;
    }

    // -- Synced data ---------------------------------------------------------------------------------------------------

    public int status() {
        return data.get(PhaseCradleBlockEntity.DATA_STATUS);
    }

    public boolean formed() {
        return data.get(PhaseCradleBlockEntity.DATA_FORMED) == 1;
    }

    public int coilCount() {
        return data.get(PhaseCradleBlockEntity.DATA_COIL_COUNT);
    }

    public int usableCoils() {
        return data.get(PhaseCradleBlockEntity.DATA_USABLE_COILS);
    }

    public PhaseArrayAction action() {
        int ordinal = data.get(PhaseCradleBlockEntity.DATA_ACTION);
        PhaseArrayAction[] actions = PhaseArrayAction.values();
        return actions[Math.max(0, Math.min(actions.length - 1, ordinal))];
    }

    public RefinementSelection selection() {
        return selectionFromCode(data.get(PhaseCradleBlockEntity.DATA_SELECTION));
    }

    public int definitionIndex() {
        return data.get(PhaseCradleBlockEntity.DATA_DEFINITION);
    }

    public int donorAffixIndex() {
        return data.get(PhaseCradleBlockEntity.DATA_DONOR_AFFIX);
    }

    public long charge() {
        return composeLong(PhaseCradleBlockEntity.DATA_CHARGE_LOW, PhaseCradleBlockEntity.DATA_CHARGE_HIGH);
    }

    public long cost() {
        return composeLong(PhaseCradleBlockEntity.DATA_COST_LOW, PhaseCradleBlockEntity.DATA_COST_HIGH);
    }

    private long composeLong(int low, int high) {
        return Integer.toUnsignedLong(data.get(low)) | ((long) data.get(high) << 32);
    }

    public int chargeRate() {
        return data.get(PhaseCradleBlockEntity.DATA_CHARGE_RATE);
    }

    public int lastInput() {
        return data.get(PhaseCradleBlockEntity.DATA_LAST_INPUT);
    }

    /** The last pulse's outcome, or null before the first. */
    public PhasePulse.Outcome lastOutcome() {
        int ordinal = data.get(PhaseCradleBlockEntity.DATA_LAST_OUTCOME);
        return ordinal < 0 || ordinal >= PhasePulse.Outcome.values().length ? null : PhasePulse.Outcome.values()[ordinal];
    }

    /** The action the last pulse ran, or null before the first. */
    public PhaseArrayAction lastAction() {
        int ordinal = data.get(PhaseCradleBlockEntity.DATA_LAST_ACTION);
        return ordinal < 0 || ordinal >= PhaseArrayAction.values().length ? null : PhaseArrayAction.values()[ordinal];
    }

    public int lastHeld() {
        return data.get(PhaseCradleBlockEntity.DATA_LAST_HELD);
    }

    public int lastSlipped() {
        return data.get(PhaseCradleBlockEntity.DATA_LAST_SLIPPED);
    }

    public int strain() {
        return data.get(PhaseCradleBlockEntity.DATA_STRAIN);
    }

    public int rpCost() {
        return data.get(PhaseCradleBlockEntity.DATA_RP_COST);
    }

    public int glyphTier() {
        return data.get(PhaseCradleBlockEntity.DATA_GLYPH_TIER);
    }

    public int requiredCoils() {
        return data.get(PhaseCradleBlockEntity.DATA_REQUIRED_COILS);
    }

    public PhasePulse.Odds odds() {
        return new PhasePulse.Odds(
                fraction(PhaseCradleBlockEntity.DATA_ODDS_SUCCESS),
                fraction(PhaseCradleBlockEntity.DATA_ODDS_OVERHOLD),
                fraction(PhaseCradleBlockEntity.DATA_ODDS_PARTIAL),
                fraction(PhaseCradleBlockEntity.DATA_ODDS_MISS),
                fraction(PhaseCradleBlockEntity.DATA_ODDS_FRACTURE)
        );
    }

    /** A Coil's Hold Chance as a fraction, position 1 next to the Emitter. */
    public double holdChance(int position) {
        return perCoil(PhaseCradleBlockEntity.DATA_HOLD_BASE, position);
    }

    public double secondHold(int position) {
        return perCoil(PhaseCradleBlockEntity.DATA_SECOND_BASE, position);
    }

    public double overholdChance(int position) {
        return perCoil(PhaseCradleBlockEntity.DATA_OVERHOLD_BASE, position);
    }

    private double perCoil(int base, int position) {
        if (position < 1 || position > PhaseArrayRules.MAX_COILS) {
            return 0;
        }
        return fraction(base + position - 1);
    }

    public long chargeCapacity() {
        return composeLong(PhaseCradleBlockEntity.DATA_CAPACITY_LOW, PhaseCradleBlockEntity.DATA_CAPACITY_HIGH);
    }

    /** The Cradle this menu opened on; on the client, its client-side block entity, or {@code null} if unloaded. */
    public PhaseCradleBlockEntity cradle() {
        return cradle;
    }

    private double fraction(int index) {
        return data.get(index) / (double) PhaseCradleBlockEntity.ODDS_SCALE;
    }

    public int partialWindow() {
        return data.get(PhaseCradleBlockEntity.DATA_PARTIAL_WINDOW);
    }

    public int strainTolerance() {
        return data.get(PhaseCradleBlockEntity.DATA_STRAIN_TOLERANCE);
    }

    public boolean charging() {
        return data.get(PhaseCradleBlockEntity.DATA_CHARGING) == 1;
    }

    public boolean pulsing() {
        return data.get(PhaseCradleBlockEntity.DATA_PULSE_STAGE) >= 0;
    }

    /** The sequence's stage, or null between pulses. */
    public PhasePulseSequence.Stage pulseStage() {
        int ordinal = data.get(PhaseCradleBlockEntity.DATA_PULSE_STAGE);
        PhasePulseSequence.Stage[] stages = PhasePulseSequence.Stage.values();
        return ordinal < 0 || ordinal >= stages.length ? null : stages[ordinal];
    }

    public int pulseLit() {
        return data.get(PhaseCradleBlockEntity.DATA_PULSE_LIT);
    }

    public int pulseFlashing() {
        return data.get(PhaseCradleBlockEntity.DATA_PULSE_FLASHING);
    }

    public ItemStack targetStack() {
        return getSlot(PhaseCradleBlockEntity.SLOT_TARGET).getItem();
    }

    public ItemStack donorStack() {
        return getSlot(PhaseCradleBlockEntity.SLOT_DONOR).getItem();
    }

    public ItemStack glyphStack() {
        return getSlot(PhaseCradleBlockEntity.SLOT_GLYPH).getItem();
    }

    // -- Vanilla plumbing ----------------------------------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_SLOT_START) {
            if (!moveItemStackTo(stack, PLAYER_SLOT_START, PLAYER_SLOT_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (BatteryCellItem.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, CELL_SLOT_START, PLAYER_SLOT_START, false)) {
                return ItemStack.EMPTY;
            }
        } else if (GlyphItem.isGlyph(stack)) {
            // An empty seat of the Glyph's own tier first, so a new line fills its seats; the spend slot takes the rest.
            int seat = SEAT_SLOT_START + GlyphItem.tier(stack) - 1;
            if (!moveItemStackTo(stack, seat, seat + 1, false)
                    && !moveItemStackTo(stack, PhaseCradleBlockEntity.SLOT_GLYPH, PhaseCradleBlockEntity.SLOT_GLYPH + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (RefinementTargets.canPhase(stack)) {
            if (!moveItemStackTo(stack, PhaseCradleBlockEntity.SLOT_TARGET, PhaseCradleBlockEntity.SLOT_TARGET + 1, false)
                    && !moveItemStackTo(stack, PhaseCradleBlockEntity.SLOT_DONOR, PhaseCradleBlockEntity.SLOT_DONOR + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (cradle == null || cradle.isRemoved()) {
            return false;
        }
        BlockPos pos = cradle.getBlockPos();
        return player.level().getBlockState(pos).getBlock() instanceof PhaseCradleBlock
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
