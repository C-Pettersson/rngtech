package com.rngtech.content.menu;

import com.rngtech.content.blockentity.AmmoniaFuelCellBlockEntity;
import com.rngtech.content.purge.FluidPurgeSupport;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;

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

public class AmmoniaFuelCellMenu extends AbstractContainerMenu implements StatBreakdownMenu {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_AMMONIA = 4;
    private static final int DATA_TANK_CAPACITY = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_ENERGY_PER_TICK = 7;
    private static final int DATA_ENERGY_GENERATION = 8;
    private static final int DATA_ENERGY_TRANSFER = 9;
    private static final int DATA_EFFICIENCY = 10;
    private static final int DATA_PROCESSING_SPEED = 11;
    private static final int DATA_REFINEMENT_POTENTIAL = 12;
    private static final int DATA_FLAT_ENERGY_GENERATION = 13;
    private static final int DATA_BASE_ENERGY_GENERATION = 14;
    private static final int DATA_COUNT = 15;
    private static final int STAT_SCALE = 100;
    private static final int RESIDUE_SLOT = 0;
    private static final int MEMBRANE_SLOT = 1;
    private static final int BATTERY_SLOT = 2;
    private static final int REFINEMENT_CONSUMABLE_SLOT = 3;
    private static final int REFINEMENT_TARGET_SLOT = 4;
    private static final int PLAYER_INVENTORY_START = 5;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final AmmoniaFuelCellBlockEntity fuelCell;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private int selectedTab = TAB_PROCESSING;

    public AmmoniaFuelCellMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, blockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(DATA_COUNT), MachineTraits.STREAM_CODEC.decode(extraData));
    }

    public AmmoniaFuelCellMenu(int containerId, Inventory playerInventory, AmmoniaFuelCellBlockEntity fuelCell, ContainerData data) {
        this(containerId, playerInventory, fuelCell, data, fuelCell.machineTraits());
    }

    private AmmoniaFuelCellMenu(int containerId, Inventory playerInventory, AmmoniaFuelCellBlockEntity fuelCell, ContainerData data, MachineTraits traits) {
        super(ModMenus.AMMONIA_FUEL_CELL.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(fuelCell.getLevel(), fuelCell.getBlockPos());
        this.data = data;
        this.fuelCell = fuelCell;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, ModBlocks.AMMONIA_FUEL_CELL.get(), traits);
        addSlot(new TabbedSlot(fuelCell.getProcessInventory(), AmmoniaFuelCellBlockEntity.SLOT_RESIDUE, 195, 61, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(fuelCell.getGearInventory(), AmmoniaFuelCellBlockEntity.SLOT_MEMBRANE, 66, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(fuelCell.getGearInventory(), AmmoniaFuelCellBlockEntity.SLOT_BATTERY_CELL, 126, 48, () -> selectedTab == TAB_GEAR));
        addSlot(RefinementMenuSupport.consumableSlot(fuelCell.getRefinementInventory(), 0, RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));
        addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, RefinementMenuSupport.REFINEMENT_TARGET_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_STATS -> TAB_STATS;
            case TAB_REFINEMENT -> TAB_REFINEMENT;
            default -> TAB_PROCESSING;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    public float progressFill() {
        int ticks = data.get(DATA_PROCESSING_TICKS);
        return ticks <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_PROGRESS) / (float) ticks, 0.0F, 1.0F);
    }

    public float energyFill() {
        return fill(DATA_ENERGY, DATA_ENERGY_CAPACITY);
    }

    public float ammoniaFill() {
        return fill(DATA_AMMONIA, DATA_TANK_CAPACITY);
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

    public int ammonia() {
        return data.get(DATA_AMMONIA);
    }

    public int tankCapacity() {
        return data.get(DATA_TANK_CAPACITY);
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    public int energyPerTick() {
        return data.get(DATA_ENERGY_PER_TICK);
    }

    public int value(int index) {
        return data.get(index);
    }

    public MachineTraits machineTraits() {
        return RefinementMenuSupport.displayTraits(getSlot(REFINEMENT_TARGET_SLOT).getItem());
    }

    public int refinementTargetSlot() {
        return REFINEMENT_TARGET_SLOT;
    }

    public double statValue(int dataIndex) {
        return data.get(dataIndex) / (double) STAT_SCALE;
    }

    public static int energyGenerationDataIndex() {
        return DATA_ENERGY_GENERATION;
    }

    public static int flatEnergyGenerationDataIndex() {
        return DATA_FLAT_ENERGY_GENERATION;
    }

    public static int baseEnergyGenerationDataIndex() {
        return DATA_BASE_ENERGY_GENERATION;
    }

    public static int energyTransferDataIndex() {
        return DATA_ENERGY_TRANSFER;
    }

    public static int efficiencyDataIndex() {
        return DATA_EFFICIENCY;
    }

    public static int processingSpeedDataIndex() {
        return DATA_PROCESSING_SPEED;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (FluidPurgeSupport.handleMenuButton(player, fuelCell, id)) {
            return true;
        }
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        return player.level().isClientSide || RefinementMenuSupport.applyToMachine(player, fuelCell, refinementTarget, ModBlocks.AMMONIA_FUEL_CELL.get());
    }

    @Override
    public MachineStatAccumulator breakdownStats() {
        return fuelCell.effectiveStats();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.AMMONIA_FUEL_CELL.get());
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
        if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (RefinementMenuSupport.isConsumable(stack)) {
            if (!moveItemStackTo(stack, REFINEMENT_CONSUMABLE_SLOT, REFINEMENT_CONSUMABLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (fuelCell.isMembrane(stack)) {
            if (!moveItemStackTo(stack, MEMBRANE_SLOT, MEMBRANE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (fuelCell.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
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
        return stack.getCount() == moved.getCount() ? ItemStack.EMPTY : moved;
    }

    private float fill(int amountIndex, int capacityIndex) {
        int capacity = data.get(capacityIndex);
        return capacity <= 0 ? 0.0F : Mth.clamp((float) data.get(amountIndex) / (float) capacity, 0.0F, 1.0F);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new TabbedInventorySlot(playerInventory, column + row * 9 + 9, 39 + column * 18, 116 + row * 18, () -> selectedTab != TAB_STATS));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(playerInventory, column, 39 + column * 18, 174, () -> selectedTab != TAB_STATS));
        }
    }

    private static AmmoniaFuelCellBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof AmmoniaFuelCellBlockEntity fuelCell) {
            return fuelCell;
        }
        throw new IllegalStateException("Expected ammonia fuel cell block entity at " + pos);
    }

    private static final class TabbedSlot extends SlotItemHandler {
        private final BooleanSupplier activeSupplier;

        private TabbedSlot(ItemStackHandler itemHandler, int index, int x, int y, BooleanSupplier activeSupplier) {
            super(itemHandler, index, x, y);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }

    private static final class TabbedInventorySlot extends Slot {
        private final BooleanSupplier activeSupplier;

        private TabbedInventorySlot(Inventory inventory, int index, int x, int y, BooleanSupplier activeSupplier) {
            super(inventory, index, x, y);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }
}
