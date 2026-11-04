package com.rngtech.content.menu;

import com.rngtech.content.block.BatteryAssemblerBlock;
import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.BatteryAssemblerBlockEntity;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.jewel.JewelSockets;
import com.rngtech.content.purge.FluidPurgeSupport;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.content.workorder.DriveBays;
import com.rngtech.content.workorder.WorkOrderDiscovery;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkOrderFamilies;
import com.rngtech.rpg.workorder.WorkOrderHost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class BatteryAssemblerMenu extends AbstractContainerMenu implements StatBreakdownMenu, WorkOrderMenuView, JewelMenuView {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_CONFIGURATION = 2;
    public static final int TAB_REFINEMENT = 3;
    public static final int TAB_ORDERS = 4;
    public static final int TAB_JEWELS = 5;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_FLUID = 4;
    private static final int DATA_FLUID_CAPACITY = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_PROCESSING_SPEED = 7;
    private static final int DATA_ENERGY_USAGE = 8;
    private static final int DATA_ENERGY_CAPACITY_STAT = 9;
    private static final int DATA_ENERGY_TRANSFER = 10;
    private static final int DATA_FLUID_TRANSFER = 11;
    private static final int DATA_REFINEMENT_POTENTIAL = 12;
    private static final int DATA_FLUID_ID = 13;
    private static final int DATA_OUTPUT_PER_MINUTE = 14;
    private static final int DATA_COUNT = 15;
    private static final int STAT_SCALE = 100;
    private static final int PROCESS_SLOT_COUNT = BatteryAssemblerBlockEntity.PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_START = PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_END = GEAR_SLOT_START + BatteryAssemblerBlockEntity.GEAR_SLOT_COUNT;
    private static final int REFINEMENT_CONSUMABLE_SLOT = GEAR_SLOT_END;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int MENU_MACHINE_SLOT_COUNT = REFINEMENT_TARGET_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = MENU_MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final BatteryAssemblerBlockEntity assembler;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private final DataSlot ordersShown;
    private int selectedTab = TAB_PROCESSING;
    private final ContainerData workOrderData;

    public BatteryAssemblerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public BatteryAssemblerMenu(
            int containerId,
            Inventory playerInventory,
            BatteryAssemblerBlockEntity assembler,
            ContainerData data
    ) {
        this(containerId, playerInventory, assembler, data, assembler.machineTraits());
    }

    private BatteryAssemblerMenu(
            int containerId,
            Inventory playerInventory,
            BatteryAssemblerBlockEntity assembler,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.BATTERY_ASSEMBLER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(assembler.getLevel(), assembler.getBlockPos());
        this.data = data;
        this.assembler = assembler;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, assembler.getBlockState().getBlock(), machineTraits);

        ItemStackHandler processInventory = assembler.getProcessInventory();
        addSlot(new TabbedSlot(processInventory, BatteryAssemblerBlockEntity.SLOT_INPUT_0, 57, 29, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, BatteryAssemblerBlockEntity.SLOT_INPUT_1, 79, 29, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, BatteryAssemblerBlockEntity.SLOT_INPUT_2, 101, 29, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, BatteryAssemblerBlockEntity.SLOT_INPUT_3, 123, 29, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, BatteryAssemblerBlockEntity.SLOT_ELECTROLYTE_INPUT, 57, 75, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, BatteryAssemblerBlockEntity.SLOT_OUTPUT_0, 189, 29, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, BatteryAssemblerBlockEntity.SLOT_OUTPUT_1, 211, 29, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, BatteryAssemblerBlockEntity.SLOT_OUTPUT_2, 189, 51, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, BatteryAssemblerBlockEntity.SLOT_OUTPUT_3, 211, 51, () -> selectedTab == TAB_PROCESSING));

        ItemStackHandler gearInventory = assembler.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, BatteryAssemblerBlockEntity.SLOT_BATTERY_CELL, 112, 48, () -> selectedTab == TAB_GEAR));

        addSlot(RefinementMenuSupport.consumableSlot(
                assembler.getRefinementInventory(),
                0,
                RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X,
                RefinementMenuSupport.REFINEMENT_SLOT_Y,
                () -> selectedTab == TAB_REFINEMENT
        ));
        addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, RefinementMenuSupport.REFINEMENT_TARGET_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));

        jewelInventoryStart = slots.size();

        addPlayerInventory(playerInventory);
        IItemHandler bays = playerInventory.player.level().isClientSide ? DriveBays.client(this) : new DriveBays(assembler);
        for (int bay = 0; bay < MachineWorkOrders.SLOTS; bay++) {
            addSlot(new DriveBaySlot(bays, bay, () -> selectedTab == TAB_ORDERS));
        }
        jewelSlotStart = slots.size();
        IItemHandler sockets = playerInventory.player.level().isClientSide ? JewelSockets.client() : new JewelSockets(assembler);
        for (int index = 0; index < JewelSockets.SLOTS; index++) {
            int slot = index;
            addSlot(new JewelSocketSlot(sockets, slot, () -> jewelsTabSelected() && jewelSlotOpen(slot)));
        }
        addDataSlots(data);
        workOrderData = playerInventory.player.level().isClientSide ? new SimpleContainerData(WorkOrderMenuSupport.FIELD_COUNT)
                : WorkOrderMenuSupport.data(assembler);
        addDataSlots(workOrderData);
        ordersShown = addDataSlot(WorkOrderDiscovery.shownSlot(playerInventory.player, assembler));
    }

    @Override
    public WorkOrderHost workOrderHost() {
        return assembler;
    }

    @Override
    public boolean workOrdersShown() {
        return ordersShown.get() != 0;
    }

    @Override
    public String workOrderFamily() {
        return WorkOrderFamilies.COMPONENT_ASSEMBLER;
    }

    @Override
    public int workOrderStage() {
        return 0;
    }

    @Override
    public int workOrderMasteryLevel() {
        return 0;
    }

    @Override
    public WorkOrderMenuSupport.View workOrderView() {
        return WorkOrderMenuSupport.view(workOrderData, 0);
    }

    @Override
    public ItemStack workOrderChip(int bay) {
        return bay >= 0 && bay < MachineWorkOrders.SLOTS ? getSlot(HOTBAR_END + bay).getItem() : ItemStack.EMPTY;
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
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_CONFIGURATION -> TAB_CONFIGURATION;
            case TAB_REFINEMENT -> TAB_REFINEMENT;
            case TAB_ORDERS -> TAB_ORDERS;
            case TAB_JEWELS -> TAB_JEWELS;
            default -> TAB_PROCESSING;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    public float processingProgress() {
        if (instantFlash()) {
            return 1.0F;
        }
        int ticks = data.get(DATA_PROCESSING_TICKS);
        return ticks <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_PROGRESS) / (float) ticks, 0.0F, 1.0F);
    }

    public float energyProgress() {
        int capacity = data.get(DATA_ENERGY_CAPACITY);
        return capacity <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_ENERGY) / (float) capacity, 0.0F, 1.0F);
    }

    public float fluidProgress() {
        int capacity = data.get(DATA_FLUID_CAPACITY);
        return capacity <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_FLUID) / (float) capacity, 0.0F, 1.0F);
    }

    /** Whether the bar should flash for a job that just finished instantly. */
    public boolean instantFlash() {
        return data.get(DATA_PROGRESS) == BaseMachineBlockEntity.INSTANT_FLASH_PROGRESS;
    }

    public int progress() {
        return Math.max(0, data.get(DATA_PROGRESS));
    }

    /** Expected items per minute for the current cycle, in tenths. */
    public int outputPerMinuteTenths() {
        return data.get(DATA_OUTPUT_PER_MINUTE);
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

    public int fluid() {
        return data.get(DATA_FLUID);
    }

    public int fluidCapacity() {
        return data.get(DATA_FLUID_CAPACITY);
    }

    public Component fluidName() {
        Fluid fluid = fluidById(data.get(DATA_FLUID_ID));
        if (fluid != Fluids.EMPTY) {
            return new FluidStack(fluid, 1).getHoverName();
        }
        return Component.translatable("rngtech.purge.target.assembly_fluid");
    }

    public int fluidTransfer() {
        return data.get(DATA_FLUID_TRANSFER);
    }

    public int status() {
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
        return data.get(dataIndex) / (double) STAT_SCALE;
    }

    public static int processingSpeedDataIndex() {
        return DATA_PROCESSING_SPEED;
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

    public static int fluidTransferDataIndex() {
        return DATA_FLUID_TRANSFER;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= WorkOrderMenuSupport.BUTTON_SELECT_TAB && id <= WorkOrderMenuSupport.BUTTON_SELECT_TAB + TAB_JEWELS) {
            selectTab(id - WorkOrderMenuSupport.BUTTON_SELECT_TAB);
            return true;
        }
        if (FluidPurgeSupport.handleMenuButton(player, assembler, id)) {
            return true;
        }
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return RefinementMenuSupport.applyToMachine(player, assembler, refinementTarget, assembler.getBlockState().getBlock());
    }

    @Override
    public MachineStatAccumulator breakdownStats() {
        return assembler.effectiveStats();
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof BatteryAssemblerBlock
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
                true
        );
    }

    private boolean moveToAssemblyInputs(ItemStack stack) {
        boolean moved = false;
        for (int slot = BatteryAssemblerBlockEntity.SLOT_INPUT_0; slot <= BatteryAssemblerBlockEntity.SLOT_INPUT_3 && !stack.isEmpty(); slot++) {
            if (assembler.isAssemblyInput(slot - BatteryAssemblerBlockEntity.SLOT_INPUT_0, stack)) {
                moved |= moveItemStackTo(stack, slot, slot + 1, false);
            }
        }
        return moved;
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
        } else if (index == REFINEMENT_CONSUMABLE_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (RefinementMenuSupport.isConsumable(stack)) {
            if (!moveItemStackTo(stack, REFINEMENT_CONSUMABLE_SLOT, REFINEMENT_CONSUMABLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BatteryAssemblerBlockEntity.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + BatteryAssemblerBlockEntity.SLOT_BATTERY_CELL, GEAR_SLOT_START + BatteryAssemblerBlockEntity.SLOT_BATTERY_CELL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BatteryAssemblerBlockEntity.isElectrolyteReagent(stack)
                || BatteryAssemblerBlockEntity.isFluidInputContainer(stack)) {
            if (!moveItemStackTo(stack, BatteryAssemblerBlockEntity.SLOT_ELECTROLYTE_INPUT, BatteryAssemblerBlockEntity.SLOT_ELECTROLYTE_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveToAssemblyInputs(stack)) {
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
                        () -> selectedTab != TAB_CONFIGURATION
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(
                    playerInventory,
                    column,
                    39 + column * 18,
                    174,
                    () -> selectedTab != TAB_CONFIGURATION
            ));
        }
    }

    private static BatteryAssemblerBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof BatteryAssemblerBlockEntity assembler) {
            return assembler;
        }
        throw new IllegalStateException("Expected battery assembler block entity at " + pos);
    }

    private static Fluid fluidById(int id) {
        Fluid fluid = BuiltInRegistries.FLUID.byId(id);
        return fluid == null ? Fluids.EMPTY : fluid;
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
