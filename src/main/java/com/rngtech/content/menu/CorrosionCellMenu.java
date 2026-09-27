package com.rngtech.content.menu;

import com.rngtech.content.blockentity.CorrosionCellBlockEntity;
import com.rngtech.content.purge.FluidPurgeSupport;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.rpg.MachineTraits;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class CorrosionCellMenu extends AbstractContainerMenu {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_ENERGY_PER_TICK = 4;
    private static final int DATA_MAX_OUTPUT = 5;
    private static final int DATA_RECIPE_ENERGY = 6;
    private static final int DATA_MINIMUM_STAGE = 7;
    private static final int DATA_STATUS = 8;
    private static final int DATA_ELECTROLYTE_FLUID = 9;
    private static final int DATA_ELECTROLYTE_FLUID_CAPACITY = 10;
    private static final int DATA_FLUID_TRANSFER = 11;
    private static final int DATA_ENERGY_GENERATION = 12;
    private static final int DATA_ENERGY_CAPACITY_STAT = 13;
    private static final int DATA_ENERGY_TRANSFER = 14;
    private static final int DATA_EFFICIENCY = 15;
    private static final int DATA_PROCESSING_SPEED = 16;
    private static final int DATA_STABILITY = 17;
    private static final int DATA_REFINEMENT_POTENTIAL = 18;
    private static final int DATA_COUNT = 19;
    private static final int STAT_SCALE = 100;
    private static final int PLATE_SLOT = 0;
    private static final int ELECTROLYTE_SLOT = 1;
    private static final int RESIDUE_SLOT = 2;
    private static final int GEAR_SLOT_START = 3;
    private static final int BATTERY_CELL_SLOT = GEAR_SLOT_START + CorrosionCellBlockEntity.SLOT_BATTERY_CELL;
    private static final int FLUID_PUMP_SLOT = GEAR_SLOT_START + CorrosionCellBlockEntity.SLOT_FLUID_PUMP;
    private static final int REFINEMENT_CONSUMABLE_SLOT = GEAR_SLOT_START + CorrosionCellBlockEntity.GEAR_SLOT_COUNT;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = REFINEMENT_TARGET_SLOT + 1;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final CorrosionCellBlockEntity cell;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private int selectedTab = TAB_PROCESSING;

    public CorrosionCellMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public CorrosionCellMenu(int containerId, Inventory playerInventory, CorrosionCellBlockEntity cell, ContainerData data) {
        this(containerId, playerInventory, cell, data, cell.machineTraits());
    }

    private CorrosionCellMenu(
            int containerId,
            Inventory playerInventory,
            CorrosionCellBlockEntity cell,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.CORROSION_CELL.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(cell.getLevel(), cell.getBlockPos());
        this.data = data;
        this.cell = cell;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, ModBlocks.CORROSION_CELL.get().asItem(), machineTraits);

        ItemStackHandler processInventory = cell.getProcessInventory();
        addSlot(new TabbedSlot(processInventory, CorrosionCellBlockEntity.SLOT_PLATE, 17, 33, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, CorrosionCellBlockEntity.SLOT_ELECTROLYTE, 17, 71, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, CorrosionCellBlockEntity.SLOT_RESIDUE, 176, 71, () -> selectedTab == TAB_PROCESSING));

        addSlot(new TabbedSlot(cell.getGearInventory(), CorrosionCellBlockEntity.SLOT_BATTERY_CELL, 74, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(cell.getGearInventory(), CorrosionCellBlockEntity.SLOT_FLUID_PUMP, 116, 48, () -> selectedTab == TAB_GEAR));

        addSlot(RefinementMenuSupport.consumableSlot(
                cell.getRefinementInventory(),
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
            default -> TAB_PROCESSING;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    public float processingProgress() {
        int total = data.get(DATA_PROCESSING_TICKS);
        return total <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_PROGRESS) / (float) total, 0.0F, 1.0F);
    }

    public float energyProgress() {
        int capacity = energyCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp((float) energy() / (float) capacity, 0.0F, 1.0F);
    }

    public float electrolyteFluidProgress() {
        int capacity = electrolyteFluidCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp((float) electrolyteFluid() / (float) capacity, 0.0F, 1.0F);
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

    public int energyPerTick() {
        return data.get(DATA_ENERGY_PER_TICK);
    }

    public int maxOutput() {
        return data.get(DATA_MAX_OUTPUT);
    }

    public int recipeEnergy() {
        return data.get(DATA_RECIPE_ENERGY);
    }

    public int minimumStage() {
        return data.get(DATA_MINIMUM_STAGE);
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    public int electrolyteFluid() {
        return data.get(DATA_ELECTROLYTE_FLUID);
    }

    public int electrolyteFluidCapacity() {
        return data.get(DATA_ELECTROLYTE_FLUID_CAPACITY);
    }

    public Component electrolyteFluidName() {
        FluidStack stack = cell.getElectrolyteTank().getFluid();
        return stack.isEmpty() ? Component.translatable("fluid.rngtech.electrolyte_solution") : stack.getHoverName();
    }

    public int fluidTransfer() {
        return data.get(DATA_FLUID_TRANSFER);
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
        if (dataIndex == DATA_REFINEMENT_POTENTIAL) {
            return data.get(dataIndex);
        }
        return data.get(dataIndex) / (double) STAT_SCALE;
    }

    public static int energyGenerationDataIndex() {
        return DATA_ENERGY_GENERATION;
    }

    public static int energyCapacityDataIndex() {
        return DATA_ENERGY_CAPACITY_STAT;
    }

    public static int energyTransferDataIndex() {
        return DATA_ENERGY_TRANSFER;
    }

    public static int fluidTransferDataIndex() {
        return DATA_FLUID_TRANSFER;
    }

    public static int efficiencyDataIndex() {
        return DATA_EFFICIENCY;
    }

    public static int processingSpeedDataIndex() {
        return DATA_PROCESSING_SPEED;
    }

    public static int stabilityDataIndex() {
        return DATA_STABILITY;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (FluidPurgeSupport.handleMenuButton(player, cell, id)) {
            return true;
        }
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return RefinementMenuSupport.applyToMachine(player, cell, refinementTarget, ModBlocks.CORROSION_CELL.get().asItem());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.CORROSION_CELL.get());
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
        } else if (cell.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, BATTERY_CELL_SLOT, BATTERY_CELL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (CorrosionCellBlockEntity.isFluidPump(stack)) {
            if (!moveItemStackTo(stack, FLUID_PUMP_SLOT, FLUID_PUMP_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (cell.isKnownElectrolyteInput(stack)) {
            if (!moveItemStackTo(stack, ELECTROLYTE_SLOT, ELECTROLYTE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (cell.isKnownPlateInput(stack)) {
            if (!moveItemStackTo(stack, PLATE_SLOT, PLATE_SLOT + 1, false)) {
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

    private static CorrosionCellBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof CorrosionCellBlockEntity cell) {
            return cell;
        }
        throw new IllegalStateException("Expected corrosion cell block entity at " + pos);
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
