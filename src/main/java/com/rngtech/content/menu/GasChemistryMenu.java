package com.rngtech.content.menu;

import com.rngtech.content.blockentity.GasChemistryBlockEntity;
import com.rngtech.content.chemistry.GasChemistryMachine;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class GasChemistryMenu extends AbstractContainerMenu {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_WATER = 4;
    private static final int DATA_INPUT = 5;
    private static final int DATA_OUTPUT = 6;
    private static final int DATA_SECONDARY_OUTPUT = 7;
    private static final int DATA_TANK_CAPACITY = 8;
    private static final int DATA_STATUS = 9;
    private static final int DATA_ENERGY_DELTA = 10;
    private static final int DATA_ENERGY_GENERATION = 11;
    private static final int DATA_ENERGY_USAGE = 12;
    private static final int DATA_ENERGY_TRANSFER = 13;
    private static final int DATA_PROCESSING_SPEED = 14;
    private static final int DATA_EFFICIENCY = 15;
    private static final int DATA_MAX_TEMPERATURE = 16;
    private static final int DATA_REFINEMENT_POTENTIAL = 17;
    private static final int DATA_COUNT = 18;
    private static final int STAT_SCALE = 100;
    private static final int PROCESS_INPUT_SLOT = 0;
    private static final int PROCESS_OUTPUT_SLOT = 1;
    private static final int HEAT_CORE_SLOT = 2;
    private static final int BATTERY_SLOT = 3;
    private static final int SERVO_SLOT = 4;
    private static final int CATALYST_SLOT = 5;
    private static final int REFINEMENT_CONSUMABLE_SLOT = 6;
    private static final int REFINEMENT_TARGET_SLOT = 7;
    private static final int PLAYER_INVENTORY_START = 8;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;
    private static final int GASIFIER_INPUT_SLOT_X = 71;
    private static final int GASIFIER_OUTPUT_SLOT_X = 143;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final GasChemistryBlockEntity gasChemistry;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private int selectedTab = TAB_PROCESSING;

    public GasChemistryMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, blockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(DATA_COUNT), MachineTraits.STREAM_CODEC.decode(extraData));
    }

    public GasChemistryMenu(int containerId, Inventory playerInventory, GasChemistryBlockEntity gasChemistry, ContainerData data) {
        this(containerId, playerInventory, gasChemistry, data, gasChemistry.machineTraits());
    }

    private GasChemistryMenu(int containerId, Inventory playerInventory, GasChemistryBlockEntity gasChemistry, ContainerData data, MachineTraits traits) {
        super(ModMenus.GAS_CHEMISTRY.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(gasChemistry.getLevel(), gasChemistry.getBlockPos());
        this.data = data;
        this.gasChemistry = gasChemistry;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, machineBlock(), traits);
        addSlot(new TabbedSlot(gasChemistry.getProcessInventory(), GasChemistryBlockEntity.SLOT_PROCESS_INPUT, GASIFIER_INPUT_SLOT_X, 50, () -> selectedTab == TAB_PROCESSING && gasChemistry.hasInputSlot()));
        addSlot(new TabbedSlot(gasChemistry.getProcessInventory(), GasChemistryBlockEntity.SLOT_PROCESS_OUTPUT, GASIFIER_OUTPUT_SLOT_X, 50, () -> selectedTab == TAB_PROCESSING && gasChemistry.hasInputSlot()));
        addSlot(new TabbedSlot(gasChemistry.getGearInventory(), GasChemistryBlockEntity.SLOT_HEAT_CORE, 46, 48, () -> selectedTab == TAB_GEAR && hasHeatCoreSlot()));
        addSlot(new TabbedSlot(gasChemistry.getGearInventory(), GasChemistryBlockEntity.SLOT_BATTERY_CELL, 82, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gasChemistry.getGearInventory(), GasChemistryBlockEntity.SLOT_SERVO, 118, 48, () -> selectedTab == TAB_GEAR && hasServoSlot()));
        addSlot(new TabbedSlot(gasChemistry.getGearInventory(), GasChemistryBlockEntity.SLOT_CATALYST_BED, 154, 48, () -> selectedTab == TAB_GEAR && hasCatalystSlot()));
        addSlot(RefinementMenuSupport.consumableSlot(gasChemistry.getRefinementInventory(), 0, RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));
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

    public GasChemistryMachine machine() {
        return gasChemistry.machine();
    }

    public float progressFill() {
        int ticks = data.get(DATA_PROCESSING_TICKS);
        return ticks <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_PROGRESS) / (float) ticks, 0.0F, 1.0F);
    }

    public float energyFill() {
        return fill(DATA_ENERGY, DATA_ENERGY_CAPACITY);
    }

    public float waterFill() {
        return fill(DATA_WATER, DATA_TANK_CAPACITY);
    }

    public float inputFill() {
        return fill(DATA_INPUT, DATA_TANK_CAPACITY);
    }

    public float outputFill() {
        return fill(DATA_OUTPUT, DATA_TANK_CAPACITY);
    }

    public float secondaryOutputFill() {
        return fill(DATA_SECONDARY_OUTPUT, DATA_TANK_CAPACITY);
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

    public int water() {
        return data.get(DATA_WATER);
    }

    public int input() {
        return data.get(DATA_INPUT);
    }

    public int output() {
        return data.get(DATA_OUTPUT);
    }

    public int secondaryOutput() {
        return data.get(DATA_SECONDARY_OUTPUT);
    }

    public int tankCapacity() {
        return data.get(DATA_TANK_CAPACITY);
    }

    public Component waterFluidName() {
        return fluidName(gasChemistry.getWaterFluid(), Component.translatable("block.minecraft.water"));
    }

    public Component inputFluidName() {
        return fluidName(gasChemistry.getInputFluid(), configuredInputFluidName());
    }

    public Component outputFluidName() {
        return fluidName(gasChemistry.getOutputFluid(), configuredOutputFluidName());
    }

    public Component secondaryOutputFluidName() {
        return fluidName(gasChemistry.getSecondaryOutputFluid(), Component.translatable("fluid.rngtech.carbon_monoxide"));
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    public int energyDelta() {
        return data.get(DATA_ENERGY_DELTA);
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

    public static int energyUsageDataIndex() {
        return DATA_ENERGY_USAGE;
    }

    public static int energyTransferDataIndex() {
        return DATA_ENERGY_TRANSFER;
    }

    public static int processingSpeedDataIndex() {
        return DATA_PROCESSING_SPEED;
    }

    public static int efficiencyDataIndex() {
        return DATA_EFFICIENCY;
    }

    public static int maxTemperatureDataIndex() {
        return DATA_MAX_TEMPERATURE;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    private Component configuredInputFluidName() {
        return switch (machine()) {
            case SYNGAS_COMBUSTOR -> Component.literal("Syngas / Carbon Monoxide");
            case STEAM_METHANE_REFORMER -> Component.literal("Methane / Syngas");
            default -> Component.translatable("rngtech.purge.empty_fluid");
        };
    }

    private Component configuredOutputFluidName() {
        return switch (machine()) {
            case COAL_GASIFIER -> Component.translatable("fluid.rngtech.syngas");
            case SYNGAS_COMBUSTOR -> Component.translatable("fluid.rngtech.carbon_exhaust");
            case STEAM_METHANE_REFORMER -> Component.translatable("fluid.rngtech.hydrogen");
        };
    }

    private static Component fluidName(FluidStack stack, Component emptyName) {
        return stack.isEmpty() ? emptyName : stack.getHoverName();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (FluidPurgeSupport.handleMenuButton(player, gasChemistry, id)) {
            return true;
        }
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        return player.level().isClientSide || RefinementMenuSupport.applyToMachine(player, gasChemistry, refinementTarget, machineBlock());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, machineBlock());
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
        } else if (hasHeatCoreSlot() && gasChemistry.isHeatCore(stack)) {
            if (!moveItemStackTo(stack, HEAT_CORE_SLOT, HEAT_CORE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (gasChemistry.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, BATTERY_SLOT, BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (hasServoSlot() && gasChemistry.isServo(stack)) {
            if (!moveItemStackTo(stack, SERVO_SLOT, SERVO_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (hasCatalystSlot() && gasChemistry.isReformingCatalyst(stack)) {
            if (!moveItemStackTo(stack, CATALYST_SLOT, CATALYST_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (gasChemistry.hasInputSlot()) {
            if (!moveItemStackTo(stack, PROCESS_INPUT_SLOT, PROCESS_INPUT_SLOT + 1, false)) {
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

    private Block machineBlock() {
        return switch (gasChemistry.machine()) {
            case COAL_GASIFIER -> ModBlocks.COAL_GASIFIER.get();
            case SYNGAS_COMBUSTOR -> ModBlocks.SYNGAS_COMBUSTOR.get();
            case STEAM_METHANE_REFORMER -> ModBlocks.STEAM_METHANE_REFORMER.get();
        };
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

    private boolean hasHeatCoreSlot() {
        return gasChemistry.hasHeatCoreGearSlot();
    }

    private boolean hasServoSlot() {
        return gasChemistry.hasServoGearSlot();
    }

    private boolean hasCatalystSlot() {
        return gasChemistry.hasCatalystGearSlot();
    }

    private static GasChemistryBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof GasChemistryBlockEntity gasChemistry) {
            return gasChemistry;
        }
        throw new IllegalStateException("Expected gas chemistry block entity at " + pos);
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
