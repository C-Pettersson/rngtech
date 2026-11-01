package com.rngtech.content.menu;

import com.rngtech.content.block.ComponentRecyclerBlock;
import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.ComponentRecyclerBlockEntity;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.jewel.JewelSockets;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.content.workorder.DriveBays;
import com.rngtech.content.workorder.WorkOrderDiscovery;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkOrderFamilies;
import com.rngtech.rpg.workorder.WorkOrderHost;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class ComponentRecyclerMenu extends AbstractContainerMenu implements StatBreakdownMenu, WorkOrderMenuView, JewelMenuView {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;
    public static final int TAB_ORDERS = 4;
    public static final int TAB_JEWELS = 5;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_ENERGY_PER_TICK = 4;
    private static final int DATA_PROCESSING_LEVEL = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_PROCESSING_SPEED = 7;
    private static final int DATA_EFFICIENCY = 8;
    private static final int DATA_ENERGY_USAGE = 9;
    private static final int DATA_ENERGY_CAPACITY_STAT = 10;
    private static final int DATA_ENERGY_TRANSFER = 11;
    private static final int DATA_STABILITY = 12;
    private static final int DATA_REFINEMENT_POTENTIAL = 13;
    private static final int DATA_COUNT = 14;
    private static final int STAT_SCALE = 100;
    private static final int PROCESS_SLOT_COUNT = ComponentRecyclerBlockEntity.PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_START = PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_END = GEAR_SLOT_START + ComponentRecyclerBlockEntity.GEAR_SLOT_COUNT;
    private static final int REFINEMENT_CONSUMABLE_SLOT = GEAR_SLOT_END;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = REFINEMENT_TARGET_SLOT + 1;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final ComponentRecyclerBlockEntity recycler;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private final boolean manual;
    private final DataSlot ordersShown;
    private int selectedTab = TAB_PROCESSING;
    private ContainerData workOrderData;

    public ComponentRecyclerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public ComponentRecyclerMenu(
            int containerId,
            Inventory playerInventory,
            ComponentRecyclerBlockEntity recycler,
            ContainerData data
    ) {
        this(containerId, playerInventory, recycler, data, recycler.machineTraits());
    }

    private ComponentRecyclerMenu(
            int containerId,
            Inventory playerInventory,
            ComponentRecyclerBlockEntity recycler,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.COMPONENT_RECYCLER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(recycler.getLevel(), recycler.getBlockPos());
        this.data = data;
        this.recycler = recycler;
        this.manual = recycler.isManual();
        RefinementMenuSupport.setMachineDisplay(refinementTarget, recycler.getBlockState().getBlock(), machineTraits);

        ItemStackHandler processInventory = recycler.getProcessInventory();
        addSlot(new TabbedSlot(processInventory, ComponentRecyclerBlockEntity.SLOT_INPUT, 45, 52, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, ComponentRecyclerBlockEntity.SLOT_OUTPUT_PRIMARY, 177, 30, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, ComponentRecyclerBlockEntity.SLOT_OUTPUT_SECONDARY, 177, 52, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, ComponentRecyclerBlockEntity.SLOT_OUTPUT_TERTIARY, 177, 74, () -> selectedTab == TAB_PROCESSING));

        ItemStackHandler gearInventory = recycler.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, ComponentRecyclerBlockEntity.SLOT_BATTERY_CELL, 56, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, ComponentRecyclerBlockEntity.SLOT_DISASSEMBLY_HEAD, 92, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, ComponentRecyclerBlockEntity.SLOT_RECOVERY_FILTER, 128, 48, () -> selectedTab == TAB_GEAR));

        addSlot(RefinementMenuSupport.consumableSlot(
                recycler.getRefinementInventory(),
                0,
                RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X,
                RefinementMenuSupport.REFINEMENT_SLOT_Y,
                () -> selectedTab == TAB_REFINEMENT
        ));
        addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, RefinementMenuSupport.REFINEMENT_TARGET_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));

        jewelInventoryStart = slots.size();

        addPlayerInventory(playerInventory);
        IItemHandler bays = playerInventory.player.level().isClientSide ? DriveBays.client(this) : new DriveBays(recycler);
        for (int bay = 0; bay < MachineWorkOrders.SLOTS; bay++) {
            addSlot(new DriveBaySlot(bays, bay, () -> selectedTab == TAB_ORDERS));
        }
        jewelSlotStart = slots.size();
        IItemHandler sockets = playerInventory.player.level().isClientSide ? JewelSockets.client() : new JewelSockets(recycler);
        for (int index = 0; index < JewelSockets.SLOTS; index++) {
            int slot = index;
            addSlot(new JewelSocketSlot(sockets, slot, () -> jewelsTabSelected() && jewelSlotOpen(slot)));
        }
        addDataSlots(data);
        workOrderData = playerInventory.player.level().isClientSide ? new SimpleContainerData(WorkOrderMenuSupport.FIELD_COUNT)
                : WorkOrderMenuSupport.data(recycler);
        addDataSlots(workOrderData);
        ordersShown = addDataSlot(WorkOrderDiscovery.shownSlot(playerInventory.player, recycler));
    }
    private int jewelInventoryStart;
    private int jewelSlotStart;

    @Override public ItemStack jewel(int index) {
        return index >= 0 && index < JewelSockets.SLOTS ? getSlot(jewelSlotStart + index).getItem() : ItemStack.EMPTY;
    }
    @Override public int jewelTreeCapacity() { return 0; }
    @Override public int jewelBodyCapacity() { return JewelSockets.bodySockets(machineTraits()); }
    @Override public boolean jewelsTabSelected() { return selectedTab == TAB_JEWELS; }
    private boolean jewelSlotOpen(int index) {
        return !JewelSocketSlot.isTreeSlot(index) && index - JewelSockets.BODY_START < jewelBodyCapacity();
    }


    public void selectTab(int tab) {
        if (manual) {
            selectedTab = TAB_PROCESSING;
            return;
        }
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_STATS -> TAB_STATS;
            case TAB_REFINEMENT -> TAB_REFINEMENT;
            case TAB_ORDERS -> TAB_ORDERS;
            case TAB_JEWELS -> TAB_JEWELS;
            default -> TAB_PROCESSING;
        };
    }

    @Override public WorkOrderHost workOrderHost() { return recycler; }
    @Override public boolean workOrdersShown() { return ordersShown.get() != 0; }
    @Override public String workOrderFamily() { return WorkOrderFamilies.COMPONENT_RECYCLER; }
    @Override public int workOrderStage() { return recycler.workOrderStage(); }
    @Override public int workOrderMasteryLevel() { return 0; }
    @Override public WorkOrderMenuSupport.View workOrderView() { return WorkOrderMenuSupport.view(workOrderData, 0); }
    @Override public ItemStack workOrderChip(int bay) {
        return bay >= 0 && bay < MachineWorkOrders.SLOTS ? getSlot(HOTBAR_END + bay).getItem() : ItemStack.EMPTY;
    }

    public int selectedTab() {
        return selectedTab;
    }

    public boolean isManual() {
        return manual;
    }

    public float processingProgress() {
        if (instantFlash()) {
            return 1.0F;
        }
        int ticks = data.get(DATA_PROCESSING_TICKS);
        return ticks <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_PROGRESS) / (float) ticks, 0.0F, 1.0F);
    }

    public float energyProgress() {
        int capacity = energyCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp((float) energy() / (float) capacity, 0.0F, 1.0F);
    }

    /** Whether the bar should flash for a job that just finished instantly. */
    public boolean instantFlash() {
        return data.get(DATA_PROGRESS) == BaseMachineBlockEntity.INSTANT_FLASH_PROGRESS;
    }

    public int progress() {
        return Math.max(0, data.get(DATA_PROGRESS));
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

    public int energyPerTick() {
        return data.get(DATA_ENERGY_PER_TICK);
    }

    public int processingLevel() {
        return data.get(DATA_PROCESSING_LEVEL);
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    public MachineTraits machineTraits() {
        return RefinementMenuSupport.displayTraits(getSlot(REFINEMENT_TARGET_SLOT).getItem());
    }

    public int refinementConsumableSlot() {
        return REFINEMENT_CONSUMABLE_SLOT;
    }

    public int refinementTargetSlot() {
        return REFINEMENT_TARGET_SLOT;
    }

    public double statValue(int dataIndex) {
        if (dataIndex == DATA_PROCESSING_LEVEL) {
            return data.get(dataIndex);
        }
        return data.get(dataIndex) / (double) STAT_SCALE;
    }

    public static int processingSpeedDataIndex() {
        return DATA_PROCESSING_SPEED;
    }

    public static int efficiencyDataIndex() {
        return DATA_EFFICIENCY;
    }

    public static int energyUsageDataIndex() {
        return DATA_ENERGY_USAGE;
    }

    public static int energyCapacityStatDataIndex() {
        return DATA_ENERGY_CAPACITY_STAT;
    }

    public static int energyTransferDataIndex() {
        return DATA_ENERGY_TRANSFER;
    }

    public static int processingLevelDataIndex() {
        return DATA_PROCESSING_LEVEL;
    }

    public static int stabilityDataIndex() {
        return DATA_STABILITY;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (manual) {
            return false;
        }
        if (id >= WorkOrderMenuSupport.BUTTON_SELECT_TAB && id <= WorkOrderMenuSupport.BUTTON_SELECT_TAB + TAB_JEWELS) {
            selectTab(id - WorkOrderMenuSupport.BUTTON_SELECT_TAB);
            return true;
        }
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return RefinementMenuSupport.applyToMachine(player, recycler, refinementTarget, recycler.getBlockState().getBlock());
    }

    @Override
    public MachineStatAccumulator breakdownStats() {
        return recycler.effectiveStats();
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof ComponentRecyclerBlock
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
                true
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot jewelSlot = slots.get(index);
        if (index >= jewelSlotStart && index < jewelSlotStart + JewelSockets.SLOTS) {
            ItemStack jewel = jewelSlot.getItem();
            ItemStack taken = jewel.copy();
            if (!moveItemStackTo(jewel, jewelInventoryStart, jewelInventoryStart + 36, true)) {
                return ItemStack.EMPTY;
            }
            if (jewel.isEmpty()) {
                jewelSlot.setByPlayer(ItemStack.EMPTY);
            } else {
                jewelSlot.setChanged();
            }
            return taken;
        }
        if (index >= jewelInventoryStart && index < jewelInventoryStart + 36 && JewelSockets.accepts(jewelSlot.getItem()) && jewelBodyCapacity() > 0) {
            ItemStack jewel = jewelSlot.getItem();
            ItemStack placed = jewel.copy();
            int first = jewelSlotStart + JewelSockets.BODY_START;
            if (!moveItemStackTo(jewel, first, first + jewelBodyCapacity(), false)) {
                return ItemStack.EMPTY;
            }
            if (jewel.isEmpty()) {
                jewelSlot.setByPlayer(ItemStack.EMPTY);
            } else {
                jewelSlot.setChanged();
            }
            return placed;
        }
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return moved;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();

        if (index >= HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (selectedTab == TAB_ORDERS && index >= PLAYER_INVENTORY_START && JobChipItem.isWorkOrder(stack)) {
            if (!moveItemStackTo(stack, HOTBAR_END, HOTBAR_END + MachineWorkOrders.SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isOutputSlot(index)) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, moved);
        } else if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!manual && RefinementMenuSupport.isConsumable(stack)) {
            if (!moveItemStackTo(stack, REFINEMENT_CONSUMABLE_SLOT, REFINEMENT_CONSUMABLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!manual && ComponentRecyclerBlockEntity.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + ComponentRecyclerBlockEntity.SLOT_BATTERY_CELL, GEAR_SLOT_START + ComponentRecyclerBlockEntity.SLOT_BATTERY_CELL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!manual && recycler.isDisassemblyHead(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + ComponentRecyclerBlockEntity.SLOT_DISASSEMBLY_HEAD, GEAR_SLOT_START + ComponentRecyclerBlockEntity.SLOT_DISASSEMBLY_HEAD + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!manual && recycler.isRecoveryFilter(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + ComponentRecyclerBlockEntity.SLOT_RECOVERY_FILTER, GEAR_SLOT_START + ComponentRecyclerBlockEntity.SLOT_RECOVERY_FILTER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (recycler.isKnownInput(stack)) {
            if (!moveItemStackTo(stack, ComponentRecyclerBlockEntity.SLOT_INPUT, ComponentRecyclerBlockEntity.SLOT_INPUT + 1, false)) {
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

    private boolean isOutputSlot(int index) {
        return index == ComponentRecyclerBlockEntity.SLOT_OUTPUT_PRIMARY
                || index == ComponentRecyclerBlockEntity.SLOT_OUTPUT_SECONDARY
                || index == ComponentRecyclerBlockEntity.SLOT_OUTPUT_TERTIARY;
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

    private static ComponentRecyclerBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof ComponentRecyclerBlockEntity recycler) {
            return recycler;
        }
        throw new IllegalStateException("Expected component recycler block entity at " + pos);
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
