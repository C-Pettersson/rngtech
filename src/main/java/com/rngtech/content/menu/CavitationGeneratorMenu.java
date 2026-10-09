package com.rngtech.content.menu;

import com.rngtech.content.blockentity.CavitationGeneratorBlockEntity;
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
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class CavitationGeneratorMenu extends AbstractContainerMenu {
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
    private static final int DATA_PROCESSING_LEVEL = 7;
    private static final int DATA_STATUS = 8;
    private static final int DATA_INPUT_FLUID = 9;
    private static final int DATA_INPUT_FLUID_CAPACITY = 10;
    private static final int DATA_HEAT_STRAIN = 11;
    private static final int DATA_MAX_HEAT_STRAIN = 12;
    private static final int DATA_ROTOR_WEAR = 13;
    private static final int DATA_ROTOR_WEAR_LIMIT = 14;
    private static final int DATA_PRESSURE_RATING = 15;
    private static final int DATA_RECIPE_STRAIN = 16;
    private static final int DATA_RECIPE_WEAR = 17;
    private static final int DATA_ENERGY_GENERATION = 18;
    private static final int DATA_ENERGY_CAPACITY_STAT = 19;
    private static final int DATA_ENERGY_TRANSFER = 20;
    private static final int DATA_EFFICIENCY = 21;
    private static final int DATA_PROCESSING_SPEED = 22;
    private static final int DATA_STABILITY = 23;
    private static final int DATA_TEMPERATURE_STABILITY = 24;
    private static final int DATA_FLUID_TRANSFER = 25;
    private static final int DATA_REFINEMENT_POTENTIAL = 26;
    private static final int DATA_OUTPUT_AMOUNT = 27;
    private static final int DATA_OUTPUT_FLUID = 28;
    private static final int DATA_OUTPUT_FLUID_CAPACITY = 29;
    private static final int DATA_RECIPE_FLUID_OUTPUT = 30;
    private static final int DATA_FLAT_ENERGY_GENERATION = 31;
    private static final int DATA_BASE_ENERGY_GENERATION = 32;
    private static final int DATA_INPUT_FLUID_ID = 33;
    private static final int DATA_OUTPUT_FLUID_ID = 34;
    private static final int DATA_COUNT = 35;
    private static final int STAT_SCALE = 100;

    private static final int FLUID_CONTAINER_SLOT = 0;
    private static final int DAMAGED_ROTOR_SLOT = 1;
    private static final int ROTOR_SLOT = 2;
    private static final int NOZZLE_SLOT = 3;
    private static final int HEAT_CORE_SLOT = 4;
    private static final int BATTERY_CELL_SLOT = 5;
    private static final int SERVO_SLOT = 6;
    private static final int ENERGY_CONNECTOR_SLOT = 7;
    private static final int REFINEMENT_CONSUMABLE_SLOT = 8;
    private static final int REFINEMENT_TARGET_SLOT = 9;
    private static final int PLAYER_INVENTORY_START = 10;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final CavitationGeneratorBlockEntity generator;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private int selectedTab = TAB_PROCESSING;

    public CavitationGeneratorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public CavitationGeneratorMenu(
            int containerId,
            Inventory playerInventory,
            CavitationGeneratorBlockEntity generator,
            ContainerData data
    ) {
        this(containerId, playerInventory, generator, data, generator.machineTraits());
    }

    private CavitationGeneratorMenu(
            int containerId,
            Inventory playerInventory,
            CavitationGeneratorBlockEntity generator,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.CAVITATION_GENERATOR.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(generator.getLevel(), generator.getBlockPos());
        this.data = data;
        this.generator = generator;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, ModBlocks.CAVITATION_GENERATOR.get(), machineTraits);

        ItemStackHandler processInventory = generator.getProcessInventory();
        addSlot(new TabbedSlot(processInventory, CavitationGeneratorBlockEntity.SLOT_FLUID_INPUT_CONTAINER, 29, 59, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, CavitationGeneratorBlockEntity.SLOT_DAMAGED_ROTOR, 193, 62, () -> selectedTab == TAB_PROCESSING));

        ItemStackHandler gearInventory = generator.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, CavitationGeneratorBlockEntity.SLOT_ROTOR, 19, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, CavitationGeneratorBlockEntity.SLOT_NOZZLE, 53, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, CavitationGeneratorBlockEntity.SLOT_HEAT_CORE, 87, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, CavitationGeneratorBlockEntity.SLOT_BATTERY_CELL, 121, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, CavitationGeneratorBlockEntity.SLOT_SERVO, 155, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, CavitationGeneratorBlockEntity.SLOT_ENERGY_CONNECTOR, 189, 48, () -> selectedTab == TAB_GEAR));

        addSlot(RefinementMenuSupport.consumableSlot(
                generator.getRefinementInventory(),
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
        int total = processingTicks();
        return total <= 0 ? 0.0F : Mth.clamp((float) progress() / (float) total, 0.0F, 1.0F);
    }

    public float energyProgress() {
        return energyCapacity() <= 0 ? 0.0F : Mth.clamp((float) energy() / (float) energyCapacity(), 0.0F, 1.0F);
    }

    public float fluidProgress() {
        return inputFluidCapacity() <= 0 ? 0.0F : Mth.clamp((float) inputFluid() / (float) inputFluidCapacity(), 0.0F, 1.0F);
    }

    public float outputFluidProgress() {
        return outputFluidCapacity() <= 0 ? 0.0F : Mth.clamp((float) outputFluid() / (float) outputFluidCapacity(), 0.0F, 1.0F);
    }

    public float strainProgress() {
        return maxHeatStrain() <= 0 ? 0.0F : Mth.clamp((float) heatStrain() / (float) maxHeatStrain(), 0.0F, 1.0F);
    }

    public float wearProgress() {
        return rotorWearLimit() <= 0 ? 0.0F : Mth.clamp((float) rotorWear() / (float) rotorWearLimit(), 0.0F, 1.0F);
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

    public int processingLevel() {
        return data.get(DATA_PROCESSING_LEVEL);
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    public int inputFluid() {
        return data.get(DATA_INPUT_FLUID);
    }

    public int inputFluidCapacity() {
        return data.get(DATA_INPUT_FLUID_CAPACITY);
    }

    public Fluid inputFluidType() {
        return FluidMenuSupport.fluid(data.get(DATA_INPUT_FLUID_ID));
    }

    public Fluid outputFluidType() {
        return FluidMenuSupport.fluid(data.get(DATA_OUTPUT_FLUID_ID));
    }

    public Component inputFluidName() {
        return FluidMenuSupport.fluidName(data.get(DATA_INPUT_FLUID_ID), Component.translatable("rngtech.purge.empty_fluid"));
    }

    public int heatStrain() {
        return data.get(DATA_HEAT_STRAIN);
    }

    public int maxHeatStrain() {
        return data.get(DATA_MAX_HEAT_STRAIN);
    }

    public int rotorWear() {
        return data.get(DATA_ROTOR_WEAR);
    }

    public int rotorWearLimit() {
        return data.get(DATA_ROTOR_WEAR_LIMIT);
    }

    public int pressureRating() {
        return data.get(DATA_PRESSURE_RATING);
    }

    public int recipeStrain() {
        return data.get(DATA_RECIPE_STRAIN);
    }

    public int recipeWear() {
        return data.get(DATA_RECIPE_WEAR);
    }

    public int outputFluid() {
        return data.get(DATA_OUTPUT_FLUID);
    }

    public int outputFluidCapacity() {
        return data.get(DATA_OUTPUT_FLUID_CAPACITY);
    }

    public Component outputFluidName() {
        return FluidMenuSupport.fluidName(data.get(DATA_OUTPUT_FLUID_ID), Component.translatable("rngtech.purge.empty_fluid"));
    }

    public int recipeFluidOutput() {
        return data.get(DATA_RECIPE_FLUID_OUTPUT);
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
        if (dataIndex == DATA_ENERGY_TRANSFER) {
            return data.get(dataIndex);
        }
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

    public static int energyCapacityDataIndex() {
        return DATA_ENERGY_CAPACITY_STAT;
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

    public static int processingLevelDataIndex() {
        return DATA_PROCESSING_LEVEL;
    }

    public static int stabilityDataIndex() {
        return DATA_STABILITY;
    }

    public static int temperatureStabilityDataIndex() {
        return DATA_TEMPERATURE_STABILITY;
    }

    public static int fluidTransferDataIndex() {
        return DATA_FLUID_TRANSFER;
    }

    public static int outputAmountDataIndex() {
        return DATA_OUTPUT_AMOUNT;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (FluidPurgeSupport.handleMenuButton(player, generator, id)) {
            return true;
        }
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return RefinementMenuSupport.applyToMachine(player, generator, refinementTarget, ModBlocks.CAVITATION_GENERATOR.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.CAVITATION_GENERATOR.get());
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
        } else if (generator.isRotor(stack)) {
            if (!moveItemStackTo(stack, ROTOR_SLOT, ROTOR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (generator.isNozzle(stack)) {
            if (!moveItemStackTo(stack, NOZZLE_SLOT, NOZZLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (generator.isHeatCore(stack)) {
            if (!moveItemStackTo(stack, HEAT_CORE_SLOT, HEAT_CORE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (generator.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, BATTERY_CELL_SLOT, BATTERY_CELL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (generator.isServo(stack)) {
            if (!moveItemStackTo(stack, SERVO_SLOT, SERVO_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (generator.isEnergyConnector(stack)) {
            if (!moveItemStackTo(stack, ENERGY_CONNECTOR_SLOT, ENERGY_CONNECTOR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (generator.isKnownFluidContainer(stack)) {
            if (!moveItemStackTo(stack, FLUID_CONTAINER_SLOT, FLUID_CONTAINER_SLOT + 1, false)) {
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
                addSlot(new TabbedInventorySlot(playerInventory, column + row * 9 + 9, 39 + column * 18, 116 + row * 18, () -> selectedTab != TAB_STATS));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(playerInventory, column, 39 + column * 18, 174, () -> selectedTab != TAB_STATS));
        }
    }

    private static CavitationGeneratorBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof CavitationGeneratorBlockEntity generator) {
            return generator;
        }
        throw new IllegalStateException("Expected cavitation generator block entity at " + pos);
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
