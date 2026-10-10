package com.rngtech.content.menu;

import com.rngtech.content.block.CompressorTankBlock;
import com.rngtech.content.blockentity.CompressorTankBlockEntity;
import com.rngtech.content.purge.FluidPurgeSupport;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.rpg.MachineStatAccumulator;
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
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class CompressorTankMenu extends AbstractContainerMenu implements StatBreakdownMenu {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;

    private static final int DATA_LOOSE_FLUID = 0;
    private static final int DATA_LOOSE_FLUID_CAPACITY = 1;
    private static final int DATA_COMPRESSED_PHYSICAL = 2;
    private static final int DATA_COMPRESSED_PHYSICAL_CAPACITY = 3;
    private static final int DATA_COMPRESSED_EQUIVALENT = 4;
    private static final int DATA_COMPRESSED_EQUIVALENT_CAPACITY = 5;
    private static final int DATA_ENERGY = 6;
    private static final int DATA_ENERGY_CAPACITY = 7;
    private static final int DATA_STATUS = 8;
    private static final int DATA_COMPRESSION_RATE = 9;
    private static final int DATA_COMPRESS_FE_PER_BUCKET = 10;
    private static final int DATA_DECOMPRESS_FE_PER_BUCKET = 11;
    private static final int DATA_FLUID_CAPACITY_STAT = 12;
    private static final int DATA_COMPRESSION_RATIO = 13;
    private static final int DATA_PROCESSING_SPEED = 14;
    private static final int DATA_ENERGY_USAGE = 15;
    private static final int DATA_ENERGY_CAPACITY_STAT = 16;
    private static final int DATA_ENERGY_TRANSFER = 17;
    private static final int DATA_FLUID_TRANSFER = 18;
    private static final int DATA_REFINEMENT_POTENTIAL = 19;
    private static final int DATA_LOOSE_FLUID_ID = 20;
    private static final int DATA_COMPRESSED_FLUID_ID = 21;
    private static final int DATA_COUNT = 22;
    private static final int STAT_SCALE = 100;
    private static final int PROCESS_SLOT_COUNT = CompressorTankBlockEntity.PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_START = PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_END = GEAR_SLOT_START + CompressorTankBlockEntity.GEAR_SLOT_COUNT;
    private static final int REFINEMENT_CONSUMABLE_SLOT = GEAR_SLOT_END;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PROCESSING_TARGET_SLOT = REFINEMENT_TARGET_SLOT + 1;
    private static final int MENU_MACHINE_SLOT_COUNT = PROCESSING_TARGET_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = MENU_MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final CompressorTankBlockEntity tank;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private int selectedTab = TAB_PROCESSING;

    public CompressorTankMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public CompressorTankMenu(
            int containerId,
            Inventory playerInventory,
            CompressorTankBlockEntity tank,
            ContainerData data
    ) {
        this(containerId, playerInventory, tank, data, tank.machineTraits());
    }

    private CompressorTankMenu(
            int containerId,
            Inventory playerInventory,
            CompressorTankBlockEntity tank,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.COMPRESSOR_TANK.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(tank.getLevel(), tank.getBlockPos());
        this.data = data;
        this.tank = tank;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, tank.getBlockState().getBlock(), machineTraits);

        ItemStackHandler processInventory = tank.getProcessInventory();
        addSlot(new TabbedSlot(
                processInventory,
                CompressorTankBlockEntity.SLOT_FLUID_INPUT_CONTAINER,
                85,
                81,
                () -> selectedTab == TAB_PROCESSING
        ));
        addSlot(new TabbedSlot(
                processInventory,
                CompressorTankBlockEntity.SLOT_FLUID_OUTPUT_CONTAINER,
                117,
                81,
                () -> selectedTab == TAB_PROCESSING
        ));

        ItemStackHandler gearInventory = tank.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, CompressorTankBlockEntity.SLOT_BATTERY_CELL, 32, 48, this::gearTabActive));
        addSlot(new TabbedSlot(gearInventory, CompressorTankBlockEntity.SLOT_SERVO_0, 68, 48, this::gearTabActive));
        addSlot(new TabbedSlot(gearInventory, CompressorTankBlockEntity.SLOT_SERVO_1, 104, 48, this::gearTabActive));
        addSlot(new TabbedSlot(gearInventory, CompressorTankBlockEntity.SLOT_SERVO_2, 140, 48, this::gearTabActive));
        addSlot(new TabbedSlot(gearInventory, CompressorTankBlockEntity.SLOT_SERVO_3, 176, 48, this::gearTabActive));

        addSlot(RefinementMenuSupport.consumableSlot(
                tank.getRefinementInventory(),
                0,
                RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X,
                RefinementMenuSupport.REFINEMENT_SLOT_Y,
                () -> selectedTab == TAB_REFINEMENT
        ));
        addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, RefinementMenuSupport.REFINEMENT_TARGET_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));
        addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, 204, 42, () -> selectedTab == TAB_PROCESSING));

        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_GEAR -> supportsCompression() ? TAB_GEAR : TAB_PROCESSING;
            case TAB_STATS -> TAB_STATS;
            case TAB_REFINEMENT -> TAB_REFINEMENT;
            default -> TAB_PROCESSING;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    public boolean supportsCompression() {
        return tank.supportsCompression();
    }

    private boolean gearTabActive() {
        return selectedTab == TAB_GEAR && supportsCompression();
    }

    public float looseFluidProgress() {
        int capacity = data.get(DATA_LOOSE_FLUID_CAPACITY);
        return capacity <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_LOOSE_FLUID) / (float) capacity, 0.0F, 1.0F);
    }

    public float compressedFluidProgress() {
        int capacity = data.get(DATA_COMPRESSED_PHYSICAL_CAPACITY);
        return capacity <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_COMPRESSED_PHYSICAL) / (float) capacity, 0.0F, 1.0F);
    }

    public float energyProgress() {
        int capacity = data.get(DATA_ENERGY_CAPACITY);
        return capacity <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_ENERGY) / (float) capacity, 0.0F, 1.0F);
    }

    public int looseFluid() {
        return data.get(DATA_LOOSE_FLUID);
    }

    public int looseFluidCapacity() {
        return data.get(DATA_LOOSE_FLUID_CAPACITY);
    }

    public Fluid looseFluidType() {
        return FluidMenuSupport.fluid(data.get(DATA_LOOSE_FLUID_ID));
    }

    public Fluid compressedFluidType() {
        return FluidMenuSupport.fluid(data.get(DATA_COMPRESSED_FLUID_ID));
    }

    public Component looseFluidName() {
        Component emptyName = Component.translatable(supportsCompression() ? "rngtech.compressor_tank.loose_fluid" : "rngtech.compressor_tank.stored_fluid");
        return FluidMenuSupport.fluidName(data.get(DATA_LOOSE_FLUID_ID), emptyName);
    }

    public Component compressedFluidName() {
        return FluidMenuSupport.fluidName(data.get(DATA_COMPRESSED_FLUID_ID), Component.translatable("rngtech.compressor_tank.compressed_fluid"));
    }

    public int compressedPhysical() {
        return data.get(DATA_COMPRESSED_PHYSICAL);
    }

    public int compressedPhysicalCapacity() {
        return data.get(DATA_COMPRESSED_PHYSICAL_CAPACITY);
    }

    public int compressedEquivalent() {
        return data.get(DATA_COMPRESSED_EQUIVALENT);
    }

    public int compressedEquivalentCapacity() {
        return data.get(DATA_COMPRESSED_EQUIVALENT_CAPACITY);
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

    public int compressionRate() {
        return data.get(DATA_COMPRESSION_RATE);
    }

    public int compressFePerBucket() {
        return data.get(DATA_COMPRESS_FE_PER_BUCKET);
    }

    public int decompressFePerBucket() {
        return data.get(DATA_DECOMPRESS_FE_PER_BUCKET);
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

    public static int fluidCapacityStatDataIndex() {
        return DATA_FLUID_CAPACITY_STAT;
    }

    public static int compressionRatioDataIndex() {
        return DATA_COMPRESSION_RATIO;
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
        if (FluidPurgeSupport.handleMenuButton(player, tank, id)) {
            return true;
        }
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return RefinementMenuSupport.applyToMachine(player, tank, refinementTarget, tank.getBlockState().getBlock());
    }

    @Override
    public MachineStatAccumulator breakdownStats() {
        return tank.effectiveStats();
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof CompressorTankBlock
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

        if (index == REFINEMENT_CONSUMABLE_SLOT) {
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
        } else if (tank.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + CompressorTankBlockEntity.SLOT_BATTERY_CELL, GEAR_SLOT_START + CompressorTankBlockEntity.SLOT_BATTERY_CELL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (tank.isServo(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + CompressorTankBlockEntity.SLOT_SERVO_0, GEAR_SLOT_START + CompressorTankBlockEntity.SLOT_SERVO_3 + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (CompressorTankBlockEntity.isFluidInputContainer(stack)) {
            if (!moveItemStackTo(stack, CompressorTankBlockEntity.SLOT_FLUID_INPUT_CONTAINER, CompressorTankBlockEntity.SLOT_FLUID_INPUT_CONTAINER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (CompressorTankBlockEntity.isFluidOutputContainer(stack)) {
            if (!moveItemStackTo(stack, CompressorTankBlockEntity.SLOT_FLUID_OUTPUT_CONTAINER, CompressorTankBlockEntity.SLOT_FLUID_OUTPUT_CONTAINER + 1, false)) {
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

    private static CompressorTankBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof CompressorTankBlockEntity tank) {
            return tank;
        }
        throw new IllegalStateException("Expected compressor tank block entity at " + pos + ", found " + blockEntity);
    }

    private static final class TabbedSlot extends SlotItemHandler {
        private final BooleanSupplier visible;

        private TabbedSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition, BooleanSupplier visible) {
            super(itemHandler, index, xPosition, yPosition);
            this.visible = visible;
        }

        @Override
        public boolean isActive() {
            return visible.getAsBoolean();
        }
    }

    private static final class TabbedInventorySlot extends Slot {
        private final BooleanSupplier visible;

        private TabbedInventorySlot(Inventory inventory, int index, int xPosition, int yPosition, BooleanSupplier visible) {
            super(inventory, index, xPosition, yPosition);
            this.visible = visible;
        }

        @Override
        public boolean isActive() {
            return visible.getAsBoolean();
        }
    }
}
