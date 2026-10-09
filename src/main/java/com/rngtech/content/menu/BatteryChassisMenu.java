package com.rngtech.content.menu;

import com.rngtech.content.blockentity.BatteryChassisBlockEntity;
import com.rngtech.content.registry.ModMenus;
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

public class BatteryChassisMenu extends AbstractContainerMenu {
    public static final int TAB_STATUS = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;

    private static final int DATA_ENERGY = 0;
    private static final int DATA_ENERGY_CAPACITY = 1;
    private static final int DATA_MAX_INPUT = 2;
    private static final int DATA_MAX_OUTPUT = 3;
    private static final int DATA_BURST_OUTPUT = 4;
    private static final int DATA_CELL_SLOTS = 5;
    private static final int DATA_ENERGY_TRANSFER = 6;
    private static final int DATA_BURST_TRANSFER = 7;
    private static final int DATA_BURST_DURATION = 8;
    private static final int DATA_EFFICIENCY = 9;
    private static final int DATA_STABILITY = 10;
    private static final int DATA_IDLE_LOSS = 11;
    private static final int DATA_GLOBAL_MODIFIER_STRENGTH = 12;
    private static final int DATA_REFINEMENT_POTENTIAL = 13;
    private static final int DATA_CELL_INPUT_SUM = 14;
    private static final int DATA_CELL_OUTPUT_SUM = 15;
    private static final int DATA_CONNECTOR_INPUT_CAP = 16;
    private static final int DATA_CONNECTOR_OUTPUT_CAP = 17;
    private static final int DATA_LAST_INPUT = 18;
    private static final int DATA_LAST_OUTPUT = 19;
    private static final int DATA_COUNT = 20;
    private static final int STAT_SCALE = 100;
    private static final int CELL_SLOT_START = 0;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final BatteryChassisBlockEntity chassis;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private final ItemStackHandler cellInventory;
    private final int physicalCellSlotCount;
    private final int refinementConsumableSlot;
    private final int refinementTargetSlot;
    private final int playerInventoryStart;
    private final int hotbarEnd;
    private int selectedTab = TAB_STATUS;

    public BatteryChassisMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public BatteryChassisMenu(int containerId, Inventory playerInventory, BatteryChassisBlockEntity chassis, ContainerData data) {
        this(containerId, playerInventory, chassis, data, chassis.machineTraits());
    }

    private BatteryChassisMenu(
            int containerId,
            Inventory playerInventory,
            BatteryChassisBlockEntity chassis,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.BATTERY_CHASSIS.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(chassis.getLevel(), chassis.getBlockPos());
        this.data = data;
        this.chassis = chassis;
        cellInventory = chassis.getInventory();
        physicalCellSlotCount = cellInventory.getSlots();
        refinementConsumableSlot = CELL_SLOT_START + physicalCellSlotCount;
        refinementTargetSlot = refinementConsumableSlot + 1;
        playerInventoryStart = refinementTargetSlot + 1;
        hotbarEnd = playerInventoryStart + 36;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, chassis.block().asItem(), machineTraits);

        for (int slot = 0; slot < physicalCellSlotCount; slot++) {
            int slotIndex = slot;
            addSlot(new TabbedSlot(
                    cellInventory,
                    slot,
                    cellSlotX(slot),
                    cellSlotY(slot),
                    () -> selectedTab == TAB_GEAR && isCellSlotVisible(slotIndex)
            ));
        }
        addSlot(RefinementMenuSupport.consumableSlot(
                chassis.getRefinementInventory(),
                0,
                RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X,
                RefinementMenuSupport.REFINEMENT_SLOT_Y,
                () -> selectedTab == TAB_REFINEMENT
        ));
        addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, RefinementMenuSupport.REFINEMENT_TARGET_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));

        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_STATS -> TAB_STATS;
            case TAB_REFINEMENT -> TAB_REFINEMENT;
            default -> TAB_STATUS;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    public float energyProgress() {
        int capacity = energyCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp((float) energy() / (float) capacity, 0.0F, 1.0F);
    }

    public int energy() {
        return data.get(DATA_ENERGY);
    }

    public int energyCapacity() {
        return data.get(DATA_ENERGY_CAPACITY);
    }

    public int maxInput() {
        return data.get(DATA_MAX_INPUT);
    }

    public int maxOutput() {
        return data.get(DATA_MAX_OUTPUT);
    }

    public int burstOutput() {
        return data.get(DATA_BURST_OUTPUT);
    }

    public int chassisCeiling() {
        return data.get(DATA_ENERGY_TRANSFER);
    }

    public int cellInputSum() {
        return data.get(DATA_CELL_INPUT_SUM);
    }

    public int cellOutputSum() {
        return data.get(DATA_CELL_OUTPUT_SUM);
    }

    public int inputConnectorCap() {
        return data.get(DATA_CONNECTOR_INPUT_CAP);
    }

    public int outputConnectorCap() {
        return data.get(DATA_CONNECTOR_OUTPUT_CAP);
    }

    public int connectorCap() {
        return Math.max(inputConnectorCap(), outputConnectorCap());
    }

    public int lastInput() {
        return data.get(DATA_LAST_INPUT);
    }

    public int lastOutput() {
        return data.get(DATA_LAST_OUTPUT);
    }

    public int cellSlotCount() {
        return data.get(DATA_CELL_SLOTS);
    }

    public int visibleCellSlotCount() {
        int visible = cellSlotCount();
        for (int slot = physicalCellSlotCount - 1; slot >= 0; slot--) {
            if (!cellInventory.getStackInSlot(slot).isEmpty()) {
                visible = Math.max(visible, slot + 1);
                break;
            }
        }
        return visible;
    }

    public boolean isCellSlotVisible(int slot) {
        return slot >= 0
                && slot < physicalCellSlotCount
                && (slot < cellSlotCount() || !cellInventory.getStackInSlot(slot).isEmpty());
    }

    public MachineTraits machineTraits() {
        return RefinementMenuSupport.displayTraits(getSlot(refinementTargetSlot).getItem());
    }

    public int refinementConsumableSlot() {
        return refinementConsumableSlot;
    }

    public int refinementTargetSlot() {
        return refinementTargetSlot;
    }

    public double statValue(int dataIndex) {
        if (dataIndex == DATA_CELL_SLOTS || dataIndex == DATA_BURST_DURATION || dataIndex == DATA_REFINEMENT_POTENTIAL) {
            return data.get(dataIndex);
        }
        return data.get(dataIndex) / (double) STAT_SCALE;
    }

    public static int cellSlotX(int slot) {
        return 52 + (slot % 5) * 30;
    }

    public static int cellSlotY(int slot) {
        return 34 + (slot / 5) * 24;
    }

    public static int cellSlotsDataIndex() {
        return DATA_CELL_SLOTS;
    }

    public static int energyTransferDataIndex() {
        return DATA_ENERGY_TRANSFER;
    }

    public static int burstTransferDataIndex() {
        return DATA_BURST_TRANSFER;
    }

    public static int burstDurationDataIndex() {
        return DATA_BURST_DURATION;
    }

    public static int efficiencyDataIndex() {
        return DATA_EFFICIENCY;
    }

    public static int stabilityDataIndex() {
        return DATA_STABILITY;
    }

    public static int idleLossDataIndex() {
        return DATA_IDLE_LOSS;
    }

    public static int globalModifierStrengthDataIndex() {
        return DATA_GLOBAL_MODIFIER_STRENGTH;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return RefinementMenuSupport.applyToMachine(player, chassis, refinementTarget, chassis.block().asItem());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, chassis.block());
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

        if (index < physicalCellSlotCount) {
            if (!moveItemStackTo(stack, playerInventoryStart, hotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index == refinementConsumableSlot) {
            if (!moveItemStackTo(stack, playerInventoryStart, hotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < playerInventoryStart) {
            return ItemStack.EMPTY;
        } else if (RefinementMenuSupport.isConsumable(stack)) {
            if (!moveItemStackTo(stack, refinementConsumableSlot, refinementConsumableSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BatteryChassisBlockEntity.isCell(stack)) {
            if (!moveItemStackTo(stack, CELL_SLOT_START, CELL_SLOT_START + physicalCellSlotCount, false)) {
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
                        39 + column * 18,
                        116 + row * 18,
                        () -> selectedTab != TAB_STATS
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(
                    playerInventory,
                    column,
                    39 + column * 18,
                    174,
                    () -> selectedTab != TAB_STATS
            ));
        }
    }

    private static BatteryChassisBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof BatteryChassisBlockEntity chassis) {
            return chassis;
        }
        throw new IllegalStateException("Expected battery chassis block entity at " + pos);
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
