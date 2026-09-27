package com.rngtech.content.menu;

import com.rngtech.content.block.ExoticAffixForgeBlock;
import com.rngtech.content.blockentity.ExoticAffixForgeBlockEntity;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.refinement.ExoticAffixForgeAction;
import com.rngtech.rpg.refinement.RefinementSelection;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class ExoticAffixForgeMenu extends AbstractContainerMenu {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_STATUS = 4;
    private static final int DATA_EFFECTIVE_ENERGY_LOW = 5;
    private static final int DATA_EFFECTIVE_ENERGY_HIGH = 6;
    private static final int DATA_REQUIRED_CATALYSTS = 7;
    private static final int DATA_TOTAL_USES = 8;
    private static final int DATA_ACTION_USES = 9;
    private static final int DATA_REFINEMENT_POTENTIAL_COST = 10;
    private static final int DATA_POWER_FAILURE = 11;
    private static final int DATA_CRAFT_ACTIVE = 12;
    private static final int DATA_CRAFT_FAILED = 13;
    private static final int DATA_COUNT = 14;
    private static final int PROCESS_SLOT_COUNT = ExoticAffixForgeBlockEntity.PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_START = PROCESS_SLOT_COUNT;
    private static final int MENU_MACHINE_SLOT_COUNT = PROCESS_SLOT_COUNT + ExoticAffixForgeBlockEntity.GEAR_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MENU_MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;
    private static final int BUTTON_ACTION_BASE = 100;
    private static final int BUTTON_SELECTION_NONE = 190;
    private static final int BUTTON_SELECTION_EXISTING_BASE = 200;
    private static final int BUTTON_SELECTION_EMPTY_PREFIX = 300;
    private static final int BUTTON_SELECTION_EMPTY_SUFFIX = 301;
    private static final int BUTTON_APPLY = 400;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final ExoticAffixForgeBlockEntity affixForge;
    private int selectedTab = TAB_PROCESSING;
    private ExoticAffixForgeAction selectedAction = ExoticAffixForgeAction.REFINE_ALL;
    private RefinementSelection selectedRefinement = RefinementSelection.none();

    public ExoticAffixForgeMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public ExoticAffixForgeMenu(
            int containerId,
            Inventory playerInventory,
            ExoticAffixForgeBlockEntity affixForge,
            ContainerData data
    ) {
        super(ModMenus.EXOTIC_AFFIX_FORGE.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(affixForge.getLevel(), affixForge.getBlockPos());
        this.data = data;
        this.affixForge = affixForge;
        selectedAction = affixForge.selectedAction();
        selectedRefinement = affixForge.selectedRefinement();

        ItemStackHandler processInventory = affixForge.getProcessInventory();
        addSlot(new TabbedSlot(
                processInventory,
                ExoticAffixForgeBlockEntity.SLOT_TARGET,
                14,
                32,
                () -> selectedTab == TAB_PROCESSING
        ));
        addSlot(new TabbedSlot(
                processInventory,
                ExoticAffixForgeBlockEntity.SLOT_CATALYST,
                43,
                32,
                () -> selectedTab == TAB_PROCESSING
        ));
        addSlot(new TabbedSlot(
                processInventory,
                ExoticAffixForgeBlockEntity.SLOT_OUTPUT,
                72,
                32,
                () -> selectedTab == TAB_PROCESSING
        ));

        addSlot(new TabbedSlot(
                affixForge.getGearInventory(),
                ExoticAffixForgeBlockEntity.SLOT_BATTERY_CELL,
                43,
                48,
                () -> selectedTab == TAB_GEAR
        ));

        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_STATS -> TAB_STATS;
            default -> TAB_PROCESSING;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    public ExoticAffixForgeAction selectedAction() {
        return selectedAction;
    }

    public RefinementSelection selectedRefinement() {
        return selectedRefinement;
    }

    public void selectExistingModifier(int affixIndex) {
        selectedRefinement = RefinementSelection.existingModifier(affixIndex);
    }

    public void selectEmptySlot(ModifierSlot slot) {
        selectedRefinement = RefinementSelection.emptySlot(slot);
    }

    public void clearSelection() {
        selectedRefinement = RefinementSelection.none();
    }

    public int actionButtonId(ExoticAffixForgeAction action) {
        return BUTTON_ACTION_BASE + action.ordinal();
    }

    public int selectionButtonId() {
        return switch (selectedRefinement.kind()) {
            case EXISTING_MODIFIER -> BUTTON_SELECTION_EXISTING_BASE + selectedRefinement.affixIndex();
            case EMPTY_SLOT -> selectedRefinement.emptySlot() == ModifierSlot.PREFIX
                    ? BUTTON_SELECTION_EMPTY_PREFIX
                    : BUTTON_SELECTION_EMPTY_SUFFIX;
            case NONE -> BUTTON_SELECTION_NONE;
        };
    }

    public float processingProgress() {
        int ticks = data.get(DATA_PROCESSING_TICKS);
        return ticks <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_PROGRESS) / (float) ticks, 0.0F, 1.0F);
    }

    public float energyProgress() {
        int capacity = data.get(DATA_ENERGY_CAPACITY);
        return capacity <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_ENERGY) / (float) capacity, 0.0F, 1.0F);
    }

    public int progress() {
        return data.get(DATA_PROGRESS);
    }

    public int processingTicks() {
        return data.get(DATA_PROCESSING_TICKS);
    }

    public int energy() {
        return data.get(DATA_ENERGY);
    }

    public int energyCapacity() {
        return data.get(DATA_ENERGY_CAPACITY);
    }

    public int status() {
        return data.get(DATA_STATUS);
    }

    public long effectiveEnergyCost() {
        return Integer.toUnsignedLong(data.get(DATA_EFFECTIVE_ENERGY_LOW))
                | Integer.toUnsignedLong(data.get(DATA_EFFECTIVE_ENERGY_HIGH)) << 32;
    }

    public int requiredCatalysts() {
        return data.get(DATA_REQUIRED_CATALYSTS);
    }

    public int totalUses() {
        return data.get(DATA_TOTAL_USES);
    }

    public int actionUses() {
        return data.get(DATA_ACTION_USES);
    }

    public int refinementPotentialCost() {
        return data.get(DATA_REFINEMENT_POTENTIAL_COST);
    }

    public float powerFailureProgress() {
        return Mth.clamp((float) powerFailure() / 100.0F, 0.0F, 1.0F);
    }

    public int powerFailure() {
        return data.get(DATA_POWER_FAILURE);
    }

    public boolean craftActive() {
        return data.get(DATA_CRAFT_ACTIVE) > 0;
    }

    public boolean craftFailed() {
        return data.get(DATA_CRAFT_FAILED) > 0;
    }

    public boolean canApplyCraft() {
        return !craftActive()
                && (craftFailed() || status() == ExoticAffixForgeBlockEntity.STATUS_READY);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_APPLY) {
            if (craftActive()) {
                return false;
            }
            if (!player.level().isClientSide) {
                affixForge.requestCraft();
            }
            return true;
        }
        if (isActionButton(id)) {
            if (craftActive()) {
                return false;
            }
            selectedAction = ExoticAffixForgeAction.byOrdinal(id - BUTTON_ACTION_BASE);
            if (!player.level().isClientSide) {
                affixForge.setSelectedAction(selectedAction);
            }
            return true;
        }
        if (isSelectionButton(id)) {
            if (craftActive()) {
                return false;
            }
            selectedRefinement = selectionFromButton(id);
            if (!player.level().isClientSide) {
                affixForge.setSelectedRefinement(selectedRefinement);
            }
            return true;
        }
        return false;
    }

    public int applyButtonId() {
        return BUTTON_APPLY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof ExoticAffixForgeBlock
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
                true
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return moved;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();

        if (index == ExoticAffixForgeBlockEntity.SLOT_OUTPUT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, moved);
        } else if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BatteryCellItem.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START, GEAR_SLOT_START + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (RefinementTargets.canRefine(stack)) {
            if (!moveItemStackTo(stack, ExoticAffixForgeBlockEntity.SLOT_TARGET, ExoticAffixForgeBlockEntity.SLOT_TARGET + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, ExoticAffixForgeBlockEntity.SLOT_CATALYST, ExoticAffixForgeBlockEntity.SLOT_CATALYST + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return moved;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new TabbedInventorySlot(
                        playerInventory,
                        column + row * 9 + 9,
                        61 + column * 18,
                        143 + row * 18,
                        () -> true
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(playerInventory, column, 61 + column * 18, 201, () -> true));
        }
    }

    private static boolean isActionButton(int id) {
        return id >= BUTTON_ACTION_BASE && id < BUTTON_ACTION_BASE + ExoticAffixForgeAction.values().length;
    }

    private static boolean isSelectionButton(int id) {
        return id == BUTTON_SELECTION_NONE
                || id == BUTTON_SELECTION_EMPTY_PREFIX
                || id == BUTTON_SELECTION_EMPTY_SUFFIX
                || id >= BUTTON_SELECTION_EXISTING_BASE && id < BUTTON_SELECTION_EXISTING_BASE + 64;
    }

    private static RefinementSelection selectionFromButton(int id) {
        if (id == BUTTON_SELECTION_EMPTY_PREFIX) {
            return RefinementSelection.emptySlot(ModifierSlot.PREFIX);
        }
        if (id == BUTTON_SELECTION_EMPTY_SUFFIX) {
            return RefinementSelection.emptySlot(ModifierSlot.SUFFIX);
        }
        if (id >= BUTTON_SELECTION_EXISTING_BASE && id < BUTTON_SELECTION_EXISTING_BASE + 64) {
            return RefinementSelection.existingModifier(id - BUTTON_SELECTION_EXISTING_BASE);
        }
        return RefinementSelection.none();
    }

    private static ExoticAffixForgeBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof ExoticAffixForgeBlockEntity affixForge) {
            return affixForge;
        }
        throw new IllegalStateException("Expected exotic affix forge block entity at " + pos);
    }

    private static final class TabbedSlot extends SlotItemHandler {
        private final BooleanSupplier activeSupplier;

        private TabbedSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition, BooleanSupplier activeSupplier) {
            super(itemHandler, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }

    private static final class TabbedInventorySlot extends Slot {
        private final BooleanSupplier activeSupplier;

        private TabbedInventorySlot(Inventory inventory, int index, int xPosition, int yPosition, BooleanSupplier activeSupplier) {
            super(inventory, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }
}
