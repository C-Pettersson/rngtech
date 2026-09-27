package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.chemistry.GasChemistryMachine;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.GasChemistryPartItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.menu.GasChemistryMenu;
import com.rngtech.content.purge.FluidOutputOverflow;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.CoalGasificationRecipe;
import com.rngtech.content.recipe.GasCombustionRecipe;
import com.rngtech.content.recipe.GasReformingRecipe;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.util.MachineEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class GasChemistryBlockEntity extends BaseMachineBlockEntity implements MenuProvider, PurgeableFluidStorage {
    public static final int SLOT_PROCESS_INPUT = 0;
    public static final int SLOT_PROCESS_OUTPUT = 1;
    public static final int SLOT_HEAT_CORE = 0;
    public static final int SLOT_BATTERY_CELL = 1;
    public static final int SLOT_SERVO = 2;
    public static final int SLOT_CATALYST_BED = 3;
    public static final int GEAR_SLOT_COUNT = 4;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_HEAT_CORE = 1;
    public static final int STATUS_MISSING_CATALYST = 2;
    public static final int STATUS_NO_RECIPE = 3;
    public static final int STATUS_NO_POWER = 4;
    public static final int STATUS_ENERGY_FULL = 5;
    public static final int STATUS_OUTPUT_FULL = 6;
    public static final int STATUS_NO_FLUID = 7;
    public static final int STATUS_NO_INPUT = 8;
    public static final int PURGE_WATER_TANK = 0;
    public static final int PURGE_INPUT_TANK = 1;
    public static final int PURGE_OUTPUT_TANK = 2;
    public static final int PURGE_SECONDARY_OUTPUT_TANK = 3;

    private static final int TANK_CAPACITY = 8000;
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
    private static final int STAT_SCALE = 100;

    private GasChemistryMachine machine;
    private final ItemStackHandler processInventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_PROCESS_INPUT && machine == GasChemistryMachine.COAL_GASIFIER;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_HEAT_CORE -> hasHeatCoreGearSlot() && isHeatCore(stack);
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_SERVO -> hasServoGearSlot() && isServo(stack);
                case SLOT_CATALYST_BED -> hasCatalystGearSlot() && isReformingCatalyst(stack);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final MachineEnergyStorage internalEnergy =
            new MachineEnergyStorage(this::internalEnergyCapacity, this::effectiveEnergyTransfer, this::effectiveEnergyTransfer, this::setChanged);
    private final FluidTank waterTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.is(net.minecraft.world.level.material.Fluids.WATER);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank inputTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return switch (machine) {
                case COAL_GASIFIER -> false;
                case SYNGAS_COMBUSTOR -> stack.is(ModFluids.SYNGAS_SOURCE.get()) || stack.is(ModFluids.CARBON_MONOXIDE_SOURCE.get());
                case STEAM_METHANE_REFORMER -> stack.is(ModFluids.METHANE_SOURCE.get()) || stack.is(ModFluids.SYNGAS_SOURCE.get());
            };
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return switch (machine) {
                case COAL_GASIFIER -> stack.is(ModFluids.SYNGAS_SOURCE.get());
                case SYNGAS_COMBUSTOR -> stack.is(ModFluids.CARBON_EXHAUST_SOURCE.get());
                case STEAM_METHANE_REFORMER -> stack.is(ModFluids.HYDROGEN_SOURCE.get());
            };
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank secondaryOutputTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return machine == GasChemistryMachine.STEAM_METHANE_REFORMER && stack.is(ModFluids.CARBON_MONOXIDE_SOURCE.get());
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler fluidHandler = new ChemistryFluidHandler();
    private final IItemHandler inputItemHandler = new InputItemHandler();
    private final IItemHandler outputItemHandler = new OutputItemHandler();
    private final IItemHandler emptyItemHandler = new EmptyItemHandler();
    private final IEnergyStorage consumerEnergyView = new ConsumerEnergyStorage();
    private final IEnergyStorage generatorEnergyView = new GeneratorEnergyStorage();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> activeTicks;
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_WATER -> waterTank.getFluidAmount();
                case DATA_INPUT -> inputTank.getFluidAmount();
                case DATA_OUTPUT -> outputTank.getFluidAmount();
                case DATA_SECONDARY_OUTPUT -> secondaryOutputTank.getFluidAmount();
                case DATA_TANK_CAPACITY -> TANK_CAPACITY;
                case DATA_STATUS -> statusCode();
                case DATA_ENERGY_DELTA -> currentEnergyDelta();
                case DATA_ENERGY_GENERATION -> scaledStat(stats, MachineStat.ENERGY_GENERATION);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_MAX_TEMPERATURE -> scaledStat(stats, MachineStat.MAX_TEMPERATURE);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_REFINEMENT_POTENTIAL + 1;
        }
    };

    private int progress;
    private int activeTicks;
    private long activeEnergy;
    private double energyCarry;

    public GasChemistryBlockEntity(BlockPos pos, BlockState blockState, GasChemistryMachine machine) {
        super(ModBlockEntities.GAS_CHEMISTRY.get(), pos, blockState, machine.machineType(), SLOT_PROCESS_INPUT, SLOT_PROCESS_INPUT, SLOT_PROCESS_OUTPUT);
        this.machine = machine;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GasChemistryBlockEntity gasChemistry) {
        boolean worked = gasChemistry.tickRecipe();
        boolean exported = gasChemistry.machine == GasChemistryMachine.SYNGAS_COMBUSTOR && gasChemistry.exportEnergy(level, pos);
        BaseMachineBlock.setActive(level, pos, state, worked);
        if (worked || exported) {
            gasChemistry.setChanged();
        }
    }

    public GasChemistryMachine machine() {
        return machine;
    }

    public ItemStackHandler getProcessInventory() {
        return processInventory;
    }

    public ItemStackHandler getGearInventory() {
        return gearInventory;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        if (side == Direction.DOWN) {
            return null;
        }
        return machine == GasChemistryMachine.SYNGAS_COMBUSTOR ? generatorEnergyView : consumerEnergyView;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return fluidHandler;
    }

    public FluidStack getWaterFluid() {
        return waterTank.getFluid();
    }

    public FluidStack getInputFluid() {
        return inputTank.getFluid();
    }

    public FluidStack getOutputFluid() {
        return outputTank.getFluid();
    }

    public FluidStack getSecondaryOutputFluid() {
        return secondaryOutputTank.getFluid();
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        List<FluidPurgeTarget> targets = new ArrayList<>();
        if (hasWaterTank()) {
            targets.add(FluidPurgeTarget.of(
                    PURGE_WATER_TANK,
                    Component.translatable("rngtech.purge.target.water_tank"),
                    FluidPurgeRole.INPUT,
                    waterTank::getFluid,
                    waterTank::drain,
                    this::clearActiveRecipe,
                    this::setChanged
            ));
        }
        if (hasInputGasTank()) {
            targets.add(FluidPurgeTarget.of(
                    PURGE_INPUT_TANK,
                    Component.translatable("rngtech.purge.target.input_tank"),
                    FluidPurgeRole.INPUT,
                    inputTank::getFluid,
                    inputTank::drain,
                    this::clearActiveRecipe,
                    this::setChanged
            ));
        }
        targets.add(FluidPurgeTarget.of(
                PURGE_OUTPUT_TANK,
                Component.translatable("rngtech.purge.target.output_tank"),
                FluidPurgeRole.OUTPUT,
                outputTank::getFluid,
                outputTank::drain,
                this::setChanged
        ));
        if (hasSecondaryOutputTank()) {
            targets.add(FluidPurgeTarget.of(
                    PURGE_SECONDARY_OUTPUT_TANK,
                    Component.translatable("rngtech.purge.target.secondary_output_tank"),
                    FluidPurgeRole.OUTPUT,
                    secondaryOutputTank::getFluid,
                    secondaryOutputTank::drain,
                    this::setChanged
            ));
        }
        return List.copyOf(targets);
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (machine != GasChemistryMachine.COAL_GASIFIER) {
            return emptyItemHandler;
        }
        if (side == Direction.DOWN) {
            return outputItemHandler;
        }
        return side == Direction.UP ? inputItemHandler : emptyItemHandler;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return processInventory;
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(machineTraits(), Component.translatable(machine.containerTranslationKey()));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GasChemistryMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        MachineTraits.STREAM_CODEC.encode(buffer, machineTraits());
    }

    public void dropInventory(Level level) {
        dropSlot(level, processInventory, SLOT_PROCESS_INPUT);
        dropSlot(level, processInventory, SLOT_PROCESS_OUTPUT);
        for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
            dropSlot(level, gearInventory, slot);
        }
        dropRefinementInventory(level);
    }

    public boolean isHeatCore(ItemStack stack) {
        return stack.getItem() instanceof SolidFuelBurnerPartItem part
                && part.partType() == MachinePartType.HEAT_CORE
                && part.stage() >= requiredHeatCoreStage();
    }

    public boolean isBatteryCell(ItemStack stack) {
        return stack.getItem() instanceof BatteryCellItem cell && cell.material().stage() >= machine.stage();
    }

    public boolean isServo(ItemStack stack) {
        return stack.getItem() instanceof ServoItem servo && servo.material().stage() >= 4;
    }

    public boolean isReformingCatalyst(ItemStack stack) {
        return stack.getItem() instanceof GasChemistryPartItem part
                && part.partType() == MachinePartType.REFORMING_CATALYST_BED
                && part.machineType() == GasChemistryMachine.STEAM_METHANE_REFORMER.machineType();
    }

    public boolean hasInputSlot() {
        return machine == GasChemistryMachine.COAL_GASIFIER;
    }

    public boolean hasWaterTank() {
        return machine == GasChemistryMachine.COAL_GASIFIER || machine == GasChemistryMachine.STEAM_METHANE_REFORMER;
    }

    public boolean hasSecondaryOutputTank() {
        return machine == GasChemistryMachine.STEAM_METHANE_REFORMER;
    }

    public boolean hasInputGasTank() {
        return machine != GasChemistryMachine.COAL_GASIFIER;
    }

    public boolean hasHeatCoreGearSlot() {
        return machine != GasChemistryMachine.SYNGAS_COMBUSTOR;
    }

    public boolean hasServoGearSlot() {
        return true;
    }

    public boolean hasCatalystGearSlot() {
        return machine == GasChemistryMachine.STEAM_METHANE_REFORMER;
    }

    private boolean tickRecipe() {
        return switch (machine) {
            case COAL_GASIFIER -> tickGasification();
            case SYNGAS_COMBUSTOR -> tickCombustion();
            case STEAM_METHANE_REFORMER -> tickReforming();
        };
    }

    private boolean tickGasification() {
        CoalGasificationRecipe recipe = currentGasificationRecipe();
        if (recipe == null || !canRunGasification(recipe)) {
            resetActiveRecipe(recipe == null ? 0 : recipe.processingTicks(), recipe == null ? 0 : recipe.energy());
            return false;
        }
        activeTicks = adjustedTicks(recipe.processingTicks());
        activeEnergy = adjustedEnergy(recipe.energy());
        int cost = Math.max(1, Mth.ceil(activeEnergy / (double) activeTicks));
        if (consumeEnergy(cost, true) < cost) {
            return false;
        }
        consumeEnergy(cost, false);
        progress++;
        if (progress >= activeTicks) {
            processInventory.extractItem(SLOT_PROCESS_INPUT, 1, false);
            waterTank.drain(recipe.waterInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            outputTank.fill(recipe.outputFluid(), IFluidHandler.FluidAction.EXECUTE);
            mergeResidue(recipe.residue());
            finishRecipe();
        }
        return true;
    }

    private boolean tickCombustion() {
        GasCombustionRecipe recipe = currentCombustionRecipe();
        if (recipe == null || !canRunCombustion(recipe)) {
            resetActiveRecipe(recipe == null ? 0 : recipe.processingTicks(), recipe == null ? 0 : recipe.energy());
            return false;
        }
        activeTicks = adjustedTicks(recipe.processingTicks());
        activeEnergy = adjustedGeneration(recipe.energy(), activeTicks);
        int generated = generateEnergyThisTick();
        if (storeEnergy(generated, true) < generated) {
            return false;
        }
        storeEnergy(generated, false);
        progress++;
        if (progress >= activeTicks) {
            inputTank.drain(recipe.gasInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            outputTank.fill(recipe.exhaustFluid(), IFluidHandler.FluidAction.EXECUTE);
            finishRecipe();
        }
        return true;
    }

    private boolean tickReforming() {
        GasReformingRecipe recipe = currentReformingRecipe();
        if (recipe == null || !canRunReforming(recipe)) {
            resetActiveRecipe(recipe == null ? 0 : recipe.processingTicks(), recipe == null ? 0 : recipe.energy());
            return false;
        }
        activeTicks = adjustedTicks(recipe.processingTicks());
        activeEnergy = adjustedEnergy(recipe.energy());
        int cost = Math.max(1, Mth.ceil(activeEnergy / (double) activeTicks));
        if (consumeEnergy(cost, true) < cost) {
            return false;
        }
        consumeEnergy(cost, false);
        progress++;
        if (progress >= activeTicks) {
            inputTank.drain(recipe.gasInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            waterTank.drain(recipe.waterInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            outputTank.fill(recipe.hydrogenFluid(), IFluidHandler.FluidAction.EXECUTE);
            secondaryOutputTank.fill(recipe.carbonMonoxideFluid(), IFluidHandler.FluidAction.EXECUTE);
            finishRecipe();
        }
        return true;
    }

    private CoalGasificationRecipe currentGasificationRecipe() {
        return GasChemistryRecipes.gasification(level, processInventory.getStackInSlot(SLOT_PROCESS_INPUT), waterTank.getFluid())
                .orElse(null);
    }

    private GasCombustionRecipe currentCombustionRecipe() {
        return GasChemistryRecipes.combustion(level, inputTank.getFluid()).orElse(null);
    }

    private GasReformingRecipe currentReformingRecipe() {
        return GasChemistryRecipes.reforming(level, catalystStack(), inputTank.getFluid(), waterTank.getFluid()).orElse(null);
    }

    private boolean canRunGasification(CoalGasificationRecipe recipe) {
        return heatCoreStage() >= recipe.minimumHeatCoreStage()
                && waterTank.getFluidAmount() >= recipe.waterInput().amount()
                && canAcceptOutput(outputTank, recipe.outputFluid())
                && canAcceptResidue(recipe.residue());
    }

    private boolean canRunCombustion(GasCombustionRecipe recipe) {
        return inputTank.getFluidAmount() >= recipe.gasInput().amount()
                && canAcceptOutput(outputTank, recipe.exhaustFluid());
    }

    private boolean canRunReforming(GasReformingRecipe recipe) {
        return heatCoreStage() >= requiredHeatCoreStage()
                && catalystStage() >= recipe.minimumCatalystStage()
                && inputTank.getFluidAmount() >= recipe.gasInput().amount()
                && waterTank.getFluidAmount() >= recipe.waterInput().amount()
                && canAcceptOutput(outputTank, recipe.hydrogenFluid())
                && canAcceptOutput(secondaryOutputTank, recipe.carbonMonoxideFluid());
    }

    private int statusCode() {
        return switch (machine) {
            case COAL_GASIFIER -> gasificationStatus();
            case SYNGAS_COMBUSTOR -> combustionStatus();
            case STEAM_METHANE_REFORMER -> reformingStatus();
        };
    }

    private int gasificationStatus() {
        if (!isHeatCore(heatCoreStack())) {
            return STATUS_MISSING_HEAT_CORE;
        }
        if (processInventory.getStackInSlot(SLOT_PROCESS_INPUT).isEmpty()) {
            return STATUS_NO_INPUT;
        }
        if (waterTank.isEmpty()) {
            return STATUS_NO_FLUID;
        }
        CoalGasificationRecipe recipe = currentGasificationRecipe();
        if (recipe == null || heatCoreStage() < recipe.minimumHeatCoreStage()) {
            return STATUS_NO_RECIPE;
        }
        if (!canAcceptOutput(outputTank, recipe.outputFluid()) || !canAcceptResidue(recipe.residue())) {
            return STATUS_OUTPUT_FULL;
        }
        int cost = currentEnergyDelta(recipe.energy(), recipe.processingTicks(), false);
        return consumeEnergy(cost, true) < cost ? STATUS_NO_POWER : STATUS_READY;
    }

    private int combustionStatus() {
        if (inputTank.isEmpty()) {
            return STATUS_NO_FLUID;
        }
        GasCombustionRecipe recipe = currentCombustionRecipe();
        if (recipe == null) {
            return STATUS_NO_RECIPE;
        }
        if (!canAcceptOutput(outputTank, recipe.exhaustFluid())) {
            return STATUS_OUTPUT_FULL;
        }
        int generated = currentEnergyDelta(recipe.energy(), recipe.processingTicks(), true);
        return generated > 0 && storeEnergy(generated, true) < generated ? STATUS_ENERGY_FULL : STATUS_READY;
    }

    private int reformingStatus() {
        if (!isHeatCore(heatCoreStack())) {
            return STATUS_MISSING_HEAT_CORE;
        }
        if (!isReformingCatalyst(catalystStack())) {
            return STATUS_MISSING_CATALYST;
        }
        if (waterTank.isEmpty() || inputTank.isEmpty()) {
            return STATUS_NO_FLUID;
        }
        GasReformingRecipe recipe = currentReformingRecipe();
        if (recipe == null || catalystStage() < recipe.minimumCatalystStage()) {
            return STATUS_NO_RECIPE;
        }
        if (!canAcceptOutput(outputTank, recipe.hydrogenFluid())
                || !canAcceptOutput(secondaryOutputTank, recipe.carbonMonoxideFluid())) {
            return STATUS_OUTPUT_FULL;
        }
        int cost = currentEnergyDelta(recipe.energy(), recipe.processingTicks(), false);
        return consumeEnergy(cost, true) < cost ? STATUS_NO_POWER : STATUS_READY;
    }

    private int currentEnergyDelta() {
        return switch (machine) {
            case COAL_GASIFIER -> {
                CoalGasificationRecipe recipe = currentGasificationRecipe();
                yield recipe == null ? 0 : currentEnergyDelta(recipe.energy(), recipe.processingTicks(), false);
            }
            case SYNGAS_COMBUSTOR -> {
                GasCombustionRecipe recipe = currentCombustionRecipe();
                yield recipe == null ? 0 : currentEnergyDelta(recipe.energy(), recipe.processingTicks(), true);
            }
            case STEAM_METHANE_REFORMER -> {
                GasReformingRecipe recipe = currentReformingRecipe();
                yield recipe == null ? 0 : currentEnergyDelta(recipe.energy(), recipe.processingTicks(), false);
            }
        };
    }

    private int currentEnergyDelta(long energy, int ticks, boolean generation) {
        int adjustedTicks = adjustedTicks(ticks);
        long adjustedEnergy = generation ? adjustedGeneration(energy, adjustedTicks) : adjustedEnergy(energy);
        return Math.max(1, Mth.ceil(adjustedEnergy / (double) adjustedTicks));
    }

    private int adjustedTicks(int baseTicks) {
        return Math.max(1, Mth.ceil(baseTicks / effectiveStats().value(MachineStat.PROCESSING_SPEED)));
    }

    private long adjustedEnergy(long baseEnergy) {
        return Math.max(1L, Mth.lfloor(baseEnergy * effectiveStats().value(MachineStat.ENERGY_USAGE)));
    }

    private long adjustedGeneration(long baseEnergy, int processingTicks) {
        MachineStatAccumulator stats = effectiveStats();
        return Math.max(1L, Mth.lfloor(stats.generatedEnergyTotal(baseEnergy, processingTicks) * stats.value(MachineStat.EFFICIENCY)));
    }

    private boolean canAcceptOutput(FluidTank tank, FluidStack output) {
        return FluidOutputOverflow.canAcceptOrVoidExcess(tank, output, autoPurgesFluidOutput());
    }

    private boolean autoPurgesFluidOutput() {
        return hasServoGearSlot() && MachineImplicitCatalog.hasBehavior(servoStack(), MachineBehavior.AUTO_PURGE);
    }

    private MachineStatAccumulator effectiveStats() {
        MachineStatAccumulator stats = switch (machine) {
            case COAL_GASIFIER -> MachineBaseStatCatalog.coalGasifier();
            case SYNGAS_COMBUSTOR -> MachineBaseStatCatalog.syngasCombustor();
            case STEAM_METHANE_REFORMER -> MachineBaseStatCatalog.steamMethaneReformer();
        };
        machineTraits().modifiers().forEach(stats::apply);
        if (hasHeatCoreGearSlot()) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, heatCoreStack());
        }
        if (hasServoGearSlot()) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, servoStack());
        }
        if (hasCatalystGearSlot()) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, catalystStack());
        }
        return stats;
    }

    private boolean canAcceptResidue(ItemStack residue) {
        if (residue.isEmpty()) {
            return true;
        }
        ItemStack output = processInventory.getStackInSlot(SLOT_PROCESS_OUTPUT);
        int slotLimit = Math.min(residue.getMaxStackSize(), processInventory.getSlotLimit(SLOT_PROCESS_OUTPUT));
        if (output.isEmpty()) {
            return residue.getCount() <= slotLimit;
        }
        return ItemStack.isSameItemSameComponents(output, residue)
                && output.getCount() + residue.getCount() <= slotLimit;
    }

    private void mergeResidue(ItemStack residue) {
        if (residue.isEmpty()) {
            return;
        }
        ItemStack output = processInventory.getStackInSlot(SLOT_PROCESS_OUTPUT);
        if (output.isEmpty()) {
            processInventory.setStackInSlot(SLOT_PROCESS_OUTPUT, residue.copy());
            return;
        }
        ItemStack merged = output.copy();
        merged.grow(residue.getCount());
        processInventory.setStackInSlot(SLOT_PROCESS_OUTPUT, merged);
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        int remaining = effectiveEnergyTransfer();
        boolean moved = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (remaining <= 0) {
                break;
            }
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(direction), direction.getOpposite());
            if (target == null || !target.canReceive()) {
                continue;
            }
            int offered = extractEnergyInternal(remaining, true);
            int received = target.receiveEnergy(offered, false);
            int delivered = extractEnergyInternal(received, false);
            remaining -= delivered;
            moved |= delivered > 0;
        }
        return moved;
    }

    private int consumeEnergy(int amount, boolean simulate) {
        int remaining = amount;
        int internal = internalEnergy.consumeEnergy(remaining, simulate);
        remaining -= internal;
        IEnergyStorage cell = batteryCellStorage();
        if (remaining > 0 && cell != null && cell.canExtract()) {
            remaining -= cell.extractEnergy(remaining, simulate);
        }
        return amount - remaining;
    }

    private int storeEnergy(int amount, boolean simulate) {
        int remaining = amount;
        int internal = internalEnergy.generateEnergy(remaining, simulate);
        remaining -= internal;
        IEnergyStorage cell = batteryCellStorage();
        if (remaining > 0 && cell != null && cell.canReceive()) {
            remaining -= cell.receiveEnergy(remaining, simulate);
        }
        return amount - remaining;
    }

    private int extractEnergyInternal(int amount, boolean simulate) {
        int remaining = amount;
        int internal = internalEnergy.extractEnergy(remaining, simulate);
        remaining -= internal;
        IEnergyStorage cell = batteryCellStorage();
        if (remaining > 0 && cell != null && cell.canExtract()) {
            remaining -= cell.extractEnergy(remaining, simulate);
        }
        return amount - remaining;
    }

    private int energyStored() {
        IEnergyStorage cell = batteryCellStorage();
        return internalEnergy.getEnergyStored() + (cell == null ? 0 : cell.getEnergyStored());
    }

    private int energyCapacity() {
        IEnergyStorage cell = batteryCellStorage();
        return internalEnergy.getMaxEnergyStored() + (cell == null ? 0 : cell.getMaxEnergyStored());
    }

    private int internalEnergyCapacity() {
        return Math.max(1, Mth.floor(effectiveStats().value(MachineStat.ENERGY_CAPACITY)));
    }

    private int effectiveEnergyTransfer() {
        return Math.max(1, Mth.floor(effectiveStats().value(MachineStat.ENERGY_TRANSFER)));
    }

    private IEnergyStorage batteryCellStorage() {
        ItemStack stack = gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
        return stack.getItem() instanceof BatteryCellItem ? BatteryCellItem.energyStorage(stack) : null;
    }

    private int generateEnergyThisTick() {
        if (activeTicks <= 0 || activeEnergy <= 0) {
            return 0;
        }
        double exact = activeEnergy / (double) activeTicks + energyCarry;
        int whole = Mth.floor(exact);
        energyCarry = exact - whole;
        return Math.max(1, whole);
    }

    private void resetActiveRecipe(int ticks, long energy) {
        progress = 0;
        activeTicks = ticks;
        activeEnergy = energy;
        energyCarry = 0.0D;
    }

    private void clearActiveRecipe() {
        resetActiveRecipe(0, 0L);
    }

    private void finishRecipe() {
        progress = 0;
        energyCarry = 0.0D;
    }

    private ItemStack heatCoreStack() {
        return gearInventory.getStackInSlot(SLOT_HEAT_CORE);
    }

    private ItemStack servoStack() {
        return gearInventory.getStackInSlot(SLOT_SERVO);
    }

    private ItemStack catalystStack() {
        return gearInventory.getStackInSlot(SLOT_CATALYST_BED);
    }

    private int heatCoreStage() {
        return heatCoreStack().getItem() instanceof SolidFuelBurnerPartItem part ? part.stage() : 0;
    }

    private int catalystStage() {
        return catalystStack().getItem() instanceof GasChemistryPartItem part ? part.stage() : 0;
    }

    private int requiredHeatCoreStage() {
        return machine == GasChemistryMachine.STEAM_METHANE_REFORMER ? 6 : 5;
    }

    private static int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return Mth.floor(stats.value(stat) * STAT_SCALE);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.putInt("Energy", internalEnergy.getEnergyStored());
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("SecondaryOutputTank", secondaryOutputTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
        tag.putInt("ActiveTicks", activeTicks);
        tag.putLong("ActiveEnergy", activeEnergy);
        tag.putDouble("EnergyCarry", energyCarry);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        internalEnergy.setEnergy(tag.getInt("Energy"));
        waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        secondaryOutputTank.readFromNBT(registries, tag.getCompound("SecondaryOutputTank"));
        clearUnavailableTanks();
        progress = tag.getInt("Progress");
        activeTicks = tag.getInt("ActiveTicks");
        activeEnergy = tag.getLong("ActiveEnergy");
        energyCarry = tag.getDouble("EnergyCarry");
    }

    private void clearUnavailableTanks() {
        if (!hasWaterTank()) {
            waterTank.setFluid(FluidStack.EMPTY);
        }
        if (!hasInputGasTank()) {
            inputTank.setFluid(FluidStack.EMPTY);
        }
        if (!hasSecondaryOutputTank()) {
            secondaryOutputTank.setFluid(FluidStack.EMPTY);
        }
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private final class ChemistryFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return switch (machine) {
                case COAL_GASIFIER, SYNGAS_COMBUSTOR -> 2;
                case STEAM_METHANE_REFORMER -> 4;
            };
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidTank exposed = exposedTank(tank);
            return exposed == null ? FluidStack.EMPTY : exposed.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return exposedTank(tank) == null ? 0 : TANK_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return switch (tank) {
                case 0 -> machine == GasChemistryMachine.SYNGAS_COMBUSTOR ? inputTank.isFluidValid(stack) : waterTank.isFluidValid(stack);
                case 1 -> machine == GasChemistryMachine.STEAM_METHANE_REFORMER && inputTank.isFluidValid(stack);
                default -> false;
            };
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            if (hasWaterTank() && waterTank.isFluidValid(resource)) {
                return waterTank.fill(resource, action);
            }
            if (hasInputGasTank() && inputTank.isFluidValid(resource)) {
                return inputTank.fill(resource, action);
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (outputTank.getFluid().is(resource.getFluid())) {
                return outputTank.drain(resource, action);
            }
            if (hasSecondaryOutputTank() && secondaryOutputTank.getFluid().is(resource.getFluid())) {
                return secondaryOutputTank.drain(resource, action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = outputTank.drain(maxDrain, action);
            return drained.isEmpty() && hasSecondaryOutputTank() ? secondaryOutputTank.drain(maxDrain, action) : drained;
        }

        private FluidTank exposedTank(int tank) {
            return switch (machine) {
                case COAL_GASIFIER -> switch (tank) {
                    case 0 -> waterTank;
                    case 1 -> outputTank;
                    default -> null;
                };
                case SYNGAS_COMBUSTOR -> switch (tank) {
                    case 0 -> inputTank;
                    case 1 -> outputTank;
                    default -> null;
                };
                case STEAM_METHANE_REFORMER -> switch (tank) {
                    case 0 -> waterTank;
                    case 1 -> inputTank;
                    case 2 -> outputTank;
                    case 3 -> secondaryOutputTank;
                    default -> null;
                };
            };
        }
    }

    private final class ConsumerEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return internalEnergy.receiveEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyCapacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    private final class GeneratorEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return extractEnergyInternal(toExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return energyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyCapacity();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

    private final class InputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(SLOT_PROCESS_INPUT);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return processInventory.insertItem(SLOT_PROCESS_INPUT, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(SLOT_PROCESS_INPUT);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return processInventory.isItemValid(SLOT_PROCESS_INPUT, stack);
        }
    }

    private final class OutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(SLOT_PROCESS_OUTPUT);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return processInventory.extractItem(SLOT_PROCESS_OUTPUT, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(SLOT_PROCESS_OUTPUT);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    private static final class EmptyItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 0;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
