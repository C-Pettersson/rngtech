package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.CavitationPartItem;
import com.rngtech.content.item.CollapseNozzleItem;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.menu.CavitationGeneratorMenu;
import com.rngtech.content.purge.FluidOutputOverflow;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.CavitationRecipe;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;

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
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public class CavitationGeneratorBlockEntity extends BaseMachineBlockEntity
        implements MenuProvider, PurgeableFluidStorage, MachineInfoProvider {
    public static final int SLOT_FLUID_INPUT_CONTAINER = 0;
    public static final int SLOT_DAMAGED_ROTOR = 1;
    public static final int PROCESS_SLOT_COUNT = 2;
    public static final int SLOT_ROTOR = 0;
    public static final int SLOT_NOZZLE = 1;
    public static final int SLOT_HEAT_CORE = 2;
    public static final int SLOT_BATTERY_CELL = 3;
    public static final int SLOT_SERVO = 4;
    public static final int SLOT_ENERGY_CONNECTOR = 5;
    public static final int GEAR_SLOT_COUNT = 6;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_ROTOR = 1;
    public static final int STATUS_MISSING_NOZZLE = 2;
    public static final int STATUS_NO_FLUID = 3;
    public static final int STATUS_INVALID_RECIPE = 4;
    public static final int STATUS_BLOCKED_STAGE = 5;
    public static final int STATUS_ENERGY_FULL = 6;
    public static final int STATUS_STRAIN_HIGH = 7;
    public static final int STATUS_ROTOR_WORN = 8;
    public static final int STATUS_OUTPUT_FULL = 9;
    public static final int STATUS_REDSTONE_DISABLED = 10;
    public static final int STATUS_FLUID_OUTPUT_FULL = 11;
    public static final int PURGE_INPUT_TANK = 0;
    public static final int PURGE_OUTPUT_TANK = 1;

    private static final int TANK_CAPACITY = FluidType.BUCKET_VOLUME * 4;
    private static final int OUTPUT_TANK_CAPACITY = FluidType.BUCKET_VOLUME * 8;
    private static final int MAX_HEAT_STRAIN = 1000;
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
    private static final int STAT_SCALE = 100;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_FLUID_INPUT_CONTAINER && isFluidInputContainer(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
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
                case SLOT_ROTOR -> isRotor(stack);
                case SLOT_NOZZLE -> isNozzle(stack);
                case SLOT_HEAT_CORE -> isHeatCore(stack);
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_SERVO -> isServo(stack);
                case SLOT_ENERGY_CONNECTOR -> isEnergyConnector(stack);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == SLOT_ROTOR || slot == SLOT_NOZZLE) {
                clearActiveRecipe();
            }
            clampInternalEnergy();
            setChanged();
        }
    };
    private final FluidTank inputTank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank outputTank = new FluidTank(OUTPUT_TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IItemHandler containerInputHandler = new ProcessItemHandler(SLOT_FLUID_INPUT_CONTAINER, SLOT_FLUID_INPUT_CONTAINER, true, false);
    private final IItemHandler damagedRotorHandler = new ProcessItemHandler(SLOT_DAMAGED_ROTOR, SLOT_DAMAGED_ROTOR, false, true);
    private final IEnergyStorage energyStorage = new GeneratorEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final IFluidHandler fluidHandler = new InputFluidHandler();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            CavitationRecipe recipe = nextRecipe();
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> currentProcessingTicks(stats);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_ENERGY_PER_TICK -> currentEnergyPerTick(stats);
                case DATA_MAX_OUTPUT -> effectiveOutputRate(stats);
                case DATA_RECIPE_ENERGY -> currentRecipeEnergy(stats);
                case DATA_PROCESSING_LEVEL -> rotorStage(stats);
                case DATA_STATUS -> statusCode(stats);
                case DATA_INPUT_FLUID -> inputTank.getFluidAmount();
                case DATA_INPUT_FLUID_CAPACITY -> inputTank.getCapacity();
                case DATA_HEAT_STRAIN -> heatStrain;
                case DATA_MAX_HEAT_STRAIN -> MAX_HEAT_STRAIN;
                case DATA_ROTOR_WEAR -> rotorWear();
                case DATA_ROTOR_WEAR_LIMIT -> rotorWearLimit();
                case DATA_PRESSURE_RATING -> recipe == null ? 0 : recipe.pressureRating();
                case DATA_RECIPE_STRAIN -> recipe == null ? 0 : recipe.heatStrain();
                case DATA_RECIPE_WEAR -> recipe == null ? 0 : recipe.wear();
                case DATA_ENERGY_GENERATION -> scaledStat(stats, MachineStat.ENERGY_GENERATION);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> effectiveOutputRate(stats);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_STABILITY -> scaledStat(stats, MachineStat.STABILITY);
                case DATA_TEMPERATURE_STABILITY -> scaledStat(stats, MachineStat.TEMPERATURE_STABILITY);
                case DATA_FLUID_TRANSFER -> scaledStat(stats, MachineStat.FLUID_TRANSFER);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_OUTPUT_AMOUNT -> scaledStat(stats, MachineStat.OUTPUT_AMOUNT);
                case DATA_OUTPUT_FLUID -> outputTank.getFluidAmount();
                case DATA_OUTPUT_FLUID_CAPACITY -> outputTank.getCapacity();
                case DATA_RECIPE_FLUID_OUTPUT -> currentRecipeFluidOutput(stats);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_RECIPE_FLUID_OUTPUT + 1;
        }
    };

    private int progress;
    private int activeProcessingTicks;
    private int internalEnergy;
    private double activeTotalEnergy;
    private double remainingEnergy;
    private double generationCarry;
    private int activeHeatStrain;
    private int activeWear;
    private FluidStack activeFluid = FluidStack.EMPTY;
    private FluidStack activeFluidOutput = FluidStack.EMPTY;
    private int heatStrain;

    public CavitationGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CAVITATION_GENERATOR.get(), pos, blockState, MachineType.CAVITATION_GENERATOR, SLOT_FLUID_INPUT_CONTAINER, SLOT_FLUID_INPUT_CONTAINER, SLOT_DAMAGED_ROTOR);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CavitationGeneratorBlockEntity generator) {
        generator.drainInputContainer();
        boolean generated = generator.tickGenerator();
        boolean exported = generator.exportEnergy(level, pos);
        BaseMachineBlock.setActive(level, pos, state, generator.isWorking());
        if (generated || exported) {
            generator.setChanged();
        }
    }

    public ItemStackHandler getProcessInventory() {
        return processInventory;
    }

    public ItemStackHandler getGearInventory() {
        return gearInventory;
    }

    public FluidTank getInputTank() {
        return inputTank;
    }

    public FluidTank getOutputTank() {
        return outputTank;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return side == Direction.UP || side == Direction.DOWN ? null : trackedEnergyStorage;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return side == null ? null : fluidHandler;
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        return List.of(
                FluidPurgeTarget.of(
                        PURGE_INPUT_TANK,
                        Component.translatable("rngtech.purge.target.input_tank"),
                        FluidPurgeRole.INPUT,
                        inputTank::getFluid,
                        inputTank::drain,
                        this::clearActiveRecipe,
                        this::setChanged
                ).withCapacity(inputTank::getCapacity),
                FluidPurgeTarget.of(
                        PURGE_OUTPUT_TANK,
                        Component.translatable("rngtech.purge.target.output_tank"),
                        FluidPurgeRole.OUTPUT,
                        outputTank::getFluid,
                        outputTank::drain,
                        this::setChanged
                ).withCapacity(outputTank::getCapacity)
        );
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        CavitationRecipe recipe = nextRecipe();
        int status = statusCode(stats);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSource(level, worldPosition);
        return MachineInfoSnapshot.builder("cavitation_generator")
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY,
                                getBlockState().getValue(BaseMachineBlock.ACTIVE),
                                status == STATUS_NO_FLUID
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .progress(progress, currentProcessingTicks(stats))
                .energy(energyStored(), energyCapacity(), isWorking() ? currentEnergyPerTick(stats) : 0)
                .energyTelemetry(energyFlow.lastInput(), energyFlow.lastOutput(), connector.transferRate(), MachineInfoSnapshot.EnergyBottleneck.NONE)
                .processingLevel(
                        rotorStage(stats),
                        recipe == null ? MachineInfoSnapshot.UNSET : recipe.minimumRotorStage()
                )
                .output(outputSummary(status))
                .refinement(machineTraits())
                .build();
    }

    private static MachineInfoSnapshot.OutputSummary outputSummary(int status) {
        return switch (status) {
            case STATUS_OUTPUT_FULL, STATUS_FLUID_OUTPUT_FULL -> MachineInfoSnapshot.OutputSummary.OUTPUT_FULL;
            case STATUS_ENERGY_FULL -> MachineInfoSnapshot.OutputSummary.ENERGY_FULL;
            default -> MachineInfoSnapshot.OutputSummary.NONE;
        };
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_MISSING_ROTOR -> "no_rotor";
            case STATUS_MISSING_NOZZLE -> "no_nozzle";
            case STATUS_NO_FLUID -> "no_fluid";
            case STATUS_INVALID_RECIPE -> "invalid_recipe";
            case STATUS_BLOCKED_STAGE -> "blocked_stage";
            case STATUS_ENERGY_FULL -> "energy_full";
            case STATUS_STRAIN_HIGH -> "strain_high";
            case STATUS_ROTOR_WORN -> "rotor_worn";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_FLUID_OUTPUT_FULL -> "fluid_output_full";
            case STATUS_REDSTONE_DISABLED -> "redstone";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.cavitation.status." + name;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == Direction.DOWN) {
            return damagedRotorHandler;
        }
        if (side != null) {
            return containerInputHandler;
        }
        return processInventory;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return processInventory;
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(
                machineTraits(),
                Component.translatable("container.rngtech.cavitation_generator")
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CavitationGeneratorMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        MachineTraits.STREAM_CODEC.encode(buffer, machineTraits());
    }

    public void dropInventory(Level level) {
        for (int slot = 0; slot < processInventory.getSlots(); slot++) {
            dropSlot(level, processInventory, slot);
        }
        for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
            dropSlot(level, gearInventory, slot);
        }
        dropRefinementInventory(level);
    }

    public boolean isRotor(ItemStack stack) {
        return stack.getItem() instanceof CavitationPartItem part
                && part.partType() == MachinePartType.CAVITATION_ROTOR
                && part.machineType() == MachineType.CAVITATION_GENERATOR;
    }

    public boolean isNozzle(ItemStack stack) {
        return stack.getItem() instanceof CollapseNozzleItem nozzle
                && nozzle.material().cavitationCompatible();
    }

    public boolean isHeatCore(ItemStack stack) {
        return stack.getItem() instanceof SolidFuelBurnerPartItem part
                && part.partType() == MachinePartType.HEAT_CORE
                && part.stage() <= 8;
    }

    public boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public boolean isServo(ItemStack stack) {
        return stack.getItem() instanceof ServoItem servo && servo.stage() >= 6;
    }

    public boolean isEnergyConnector(ItemStack stack) {
        return stack.getItem() instanceof EnergyConnectorItem;
    }

    public boolean isKnownFluidContainer(ItemStack stack) {
        return isFluidInputContainer(stack);
    }

    public MachineStatAccumulator effectiveStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.cavitationGenerator();
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
        applyPartStats(stats, rotorStack());
        applyPartStats(stats, nozzleStack());
        applyHeatCoreStats(stats, heatCoreStack());
        applyPartStats(stats, servoStack());
        return stats;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
        tag.putInt("ActiveProcessingTicks", activeProcessingTicks);
        tag.putInt("Energy", internalEnergyStored());
        tag.putDouble("ActiveTotalEnergy", activeTotalEnergy);
        tag.putDouble("RemainingEnergy", remainingEnergy);
        tag.putDouble("GenerationCarry", generationCarry);
        tag.putInt("ActiveHeatStrain", activeHeatStrain);
        tag.putInt("ActiveWear", activeWear);
        tag.put("ActiveFluid", activeFluid.saveOptional(registries));
        tag.put("ActiveFluidOutput", activeFluidOutput.saveOptional(registries));
        tag.putInt("HeatStrain", heatStrain);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        loadGearInventory(tag.getCompound("GearInventory"), registries);
        inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        progress = tag.getInt("Progress");
        activeProcessingTicks = tag.getInt("ActiveProcessingTicks");
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        activeTotalEnergy = tag.getDouble("ActiveTotalEnergy");
        remainingEnergy = tag.getDouble("RemainingEnergy");
        generationCarry = tag.getDouble("GenerationCarry");
        activeHeatStrain = tag.getInt("ActiveHeatStrain");
        activeWear = tag.getInt("ActiveWear");
        activeFluid = FluidStack.parseOptional(registries, tag.getCompound("ActiveFluid"));
        activeFluidOutput = FluidStack.parseOptional(registries, tag.getCompound("ActiveFluidOutput"));
        heatStrain = Mth.clamp(tag.getInt("HeatStrain"), 0, MAX_HEAT_STRAIN);
        if (tag.contains("RotorWear")) {
            CavitationPartItem.setRotorWear(rotorStack(), tag.getInt("RotorWear"));
        }
        clampInternalEnergy();
    }

    private boolean tickGenerator() {
        coolDown();
        if (level == null || redstoneDisabled() || !hasRequiredComponents()) {
            return false;
        }

        MachineStatAccumulator stats = effectiveStats();
        if (rotorWear() >= rotorWearLimit() && !ejectWornRotor()) {
            return false;
        }
        if (!hasActiveRecipe() && !tryStartRecipe(stats)) {
            return false;
        }
        normalizeActiveRecipeEnergy(stats);
        if (energyStored() >= energyCapacity()) {
            return false;
        }
        return generateActiveRecipe(stats);
    }

    private boolean tryStartRecipe(MachineStatAccumulator stats) {
        CavitationRecipe recipe = nextRecipe();
        if (recipe == null || rotorStage(stats) < recipe.minimumRotorStage() || heatStrain >= MAX_HEAT_STRAIN) {
            clearInactiveProgress();
            return false;
        }
        FluidStack fluidOutput = scaledFluidOutput(recipe, stats);
        if (inputTank.getFluidAmount() < recipe.fluidInput().amount()
                || energyStored() >= energyCapacity()
                || !canAcceptFluidOutput(fluidOutput)) {
            return false;
        }

        FluidStack drained = inputTank.drain(inputTank.getFluid().copyWithAmount(recipe.fluidInput().amount()), IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() < recipe.fluidInput().amount()) {
            return false;
        }

        activeFluid = drained.copy();
        activeProcessingTicks = stats.adjustedProcessingTicks(recipe.processingTicks());
        activeTotalEnergy = effectiveRecipeEnergy(recipe, stats);
        remainingEnergy = activeTotalEnergy;
        generationCarry = 0.0;
        activeHeatStrain = effectiveHeatStrain(recipe, stats);
        activeWear = effectiveWear(recipe, stats);
        activeFluidOutput = fluidOutput;
        progress = 0;
        setChanged();
        return remainingEnergy > 0.0;
    }

    private boolean generateActiveRecipe(MachineStatAccumulator stats) {
        double perTick = currentGenerationRate(activeTotalEnergy, activeProcessingTicks);
        generationCarry += Math.min(perTick, remainingEnergy);
        int freeSpace = energyCapacity() - energyStored();
        int wholeEnergy = Math.min((int) Math.floor(generationCarry), freeSpace);
        if (wholeEnergy > 0) {
            int accepted = receiveGeneratedEnergy(wholeEnergy, false);
            if (accepted <= 0) {
                return false;
            }
            generationCarry -= accepted;
            remainingEnergy = Math.max(0.0, remainingEnergy - accepted);
        }

        progress++;
        if (progress >= activeProcessingTicks || remainingEnergy <= 0.0001) {
            finishActiveRecipe();
        }
        return true;
    }

    private void finishActiveRecipe() {
        if (!activeFluidOutput.isEmpty()) {
            outputTank.fill(activeFluidOutput, IFluidHandler.FluidAction.EXECUTE);
        }
        heatStrain = Mth.clamp(heatStrain + activeHeatStrain, 0, MAX_HEAT_STRAIN);
        CavitationPartItem.setRotorWear(rotorStack(), rotorWear() + activeWear);
        if (rotorWear() >= rotorWearLimit()) {
            ejectWornRotor();
        }
        clearActiveRecipe();
        setChanged();
    }

    private boolean ejectWornRotor() {
        ItemStack rotor = rotorStack();
        if (!isRotor(rotor) || !canMergeDamagedRotor()) {
            return false;
        }
        processInventory.setStackInSlot(SLOT_DAMAGED_ROTOR, new ItemStack(ModItems.PITTED_CAVITATION_ROTOR.get()));
        gearInventory.setStackInSlot(SLOT_ROTOR, ItemStack.EMPTY);
        clearActiveRecipe();
        setChanged();
        return true;
    }

    private boolean canMergeDamagedRotor() {
        return processInventory.getStackInSlot(SLOT_DAMAGED_ROTOR).isEmpty();
    }

    private CavitationRecipe nextRecipe() {
        return level == null ? null : CavitationRecipes.find(level, inputTank.getFluid(), rotorStack(), nozzleStack()).orElse(null);
    }

    private double effectiveRecipeEnergy(CavitationRecipe recipe, MachineStatAccumulator stats) {
        int ticks = stats.adjustedProcessingTicks(recipe.processingTicks());
        return Math.max(1.0, stats.generatedEnergyTotal(recipe.energy(), ticks)
                * Math.max(0.1, stats.value(MachineStat.EFFICIENCY)));
    }

    private int effectiveHeatStrain(CavitationRecipe recipe, MachineStatAccumulator stats) {
        double stability = Math.max(0.25, stats.value(MachineStat.TEMPERATURE_STABILITY));
        return Math.max(0, (int) Math.round(recipe.heatStrain() / stability));
    }

    private int effectiveWear(CavitationRecipe recipe, MachineStatAccumulator stats) {
        double stability = Math.max(0.25, stats.value(MachineStat.STABILITY));
        return Math.max(1, (int) Math.round(recipe.wear() / stability));
    }

    private FluidStack scaledFluidOutput(CavitationRecipe recipe, MachineStatAccumulator stats) {
        FluidStack output = recipe.fluidOutputStack();
        if (output.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int amount = Math.max(1, (int) Math.round(output.getAmount() * Math.max(0.1, stats.value(MachineStat.OUTPUT_AMOUNT))));
        return output.copyWithAmount(amount);
    }

    private boolean canAcceptFluidOutput(FluidStack output) {
        return FluidOutputOverflow.canAcceptOrVoidExcess(outputTank, output, autoPurgesFluidOutput());
    }

    private boolean autoPurgesFluidOutput() {
        return MachineImplicitCatalog.hasBehavior(servoStack(), MachineBehavior.AUTO_PURGE);
    }

    private int currentProcessingTicks(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return activeProcessingTicks;
        }
        CavitationRecipe recipe = nextRecipe();
        return recipe == null || rotorStage(stats) < recipe.minimumRotorStage() ? 0 : stats.adjustedProcessingTicks(recipe.processingTicks());
    }

    private int currentEnergyPerTick(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return visibleEnergyPerTick(activeTotalEnergyForDisplay(stats), activeProcessingTicks);
        }
        CavitationRecipe recipe = nextRecipe();
        if (recipe == null || rotorStage(stats) < recipe.minimumRotorStage()) {
            return 0;
        }
        return visibleEnergyPerTick(effectiveRecipeEnergy(recipe, stats), stats.adjustedProcessingTicks(recipe.processingTicks()));
    }

    private double currentGenerationRate(double totalEnergy, int processingTicks) {
        double strainPenalty = 1.0 - Math.min(0.50, heatStrain / (double) MAX_HEAT_STRAIN * 0.50);
        return totalEnergy * strainPenalty / Math.max(1, processingTicks);
    }

    private int visibleEnergyPerTick(double totalEnergy, int processingTicks) {
        if (totalEnergy <= 0.0 || processingTicks <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(currentGenerationRate(totalEnergy, processingTicks)));
    }

    private int currentRecipeEnergy(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return Math.max(0, (int) Math.round(activeTotalEnergyForDisplay(stats)));
        }
        CavitationRecipe recipe = nextRecipe();
        return recipe == null || rotorStage(stats) < recipe.minimumRotorStage() ? 0 : Math.max(1, (int) Math.round(effectiveRecipeEnergy(recipe, stats)));
    }

    private int currentRecipeFluidOutput(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return activeFluidOutput.getAmount();
        }
        CavitationRecipe recipe = nextRecipe();
        return recipe == null || rotorStage(stats) < recipe.minimumRotorStage() ? 0 : scaledFluidOutput(recipe, stats).getAmount();
    }

    private void normalizeActiveRecipeEnergy(MachineStatAccumulator stats) {
        if (!hasActiveRecipe() || activeTotalEnergy <= 0.0) {
            return;
        }
        double expectedTotal = currentActiveRecipeEnergy(stats);
        if (expectedTotal <= 0.0 || activeTotalEnergy <= expectedTotal + 0.0001) {
            return;
        }
        double remainingRatio = Mth.clamp(remainingEnergy / activeTotalEnergy, 0.0, 1.0);
        activeTotalEnergy = expectedTotal;
        remainingEnergy = Math.max(0.0, expectedTotal * remainingRatio);
        generationCarry = Math.min(generationCarry, remainingEnergy);
        setChanged();
    }

    private double activeTotalEnergyForDisplay(MachineStatAccumulator stats) {
        double expectedTotal = currentActiveRecipeEnergy(stats);
        return expectedTotal <= 0.0 ? activeTotalEnergy : Math.min(activeTotalEnergy, expectedTotal);
    }

    private double currentActiveRecipeEnergy(MachineStatAccumulator stats) {
        CavitationRecipe recipe = activeRecipe();
        return recipe == null ? 0.0 : effectiveRecipeEnergy(recipe, stats);
    }

    private CavitationRecipe activeRecipe() {
        return level == null || activeFluid.isEmpty()
                ? null
                : CavitationRecipes.find(level, activeFluid, rotorStack(), nozzleStack()).orElse(null);
    }

    private int statusCode(MachineStatAccumulator stats) {
        if (redstoneDisabled()) {
            return STATUS_REDSTONE_DISABLED;
        }
        if (!isRotor(rotorStack())) {
            return STATUS_MISSING_ROTOR;
        }
        if (!isNozzle(nozzleStack())) {
            return STATUS_MISSING_NOZZLE;
        }
        if (rotorWear() >= rotorWearLimit()) {
            return canMergeDamagedRotor() ? STATUS_ROTOR_WORN : STATUS_OUTPUT_FULL;
        }
        if (heatStrain >= MAX_HEAT_STRAIN) {
            return STATUS_STRAIN_HIGH;
        }
        if (inputTank.isEmpty()) {
            return STATUS_NO_FLUID;
        }
        CavitationRecipe recipe = nextRecipe();
        if (recipe == null) {
            return STATUS_INVALID_RECIPE;
        }
        if (rotorStage(stats) < recipe.minimumRotorStage()) {
            return STATUS_BLOCKED_STAGE;
        }
        FluidStack fluidOutput = scaledFluidOutput(recipe, stats);
        if (!canAcceptFluidOutput(fluidOutput)) {
            return STATUS_FLUID_OUTPUT_FULL;
        }
        return energyStored() >= energyCapacity() ? STATUS_ENERGY_FULL : STATUS_READY;
    }

    private int rotorStage(MachineStatAccumulator stats) {
        return hasRequiredComponents() ? Math.max(0, stats.intValue(MachineStat.PROCESSING_LEVEL)) : 0;
    }

    private boolean hasRequiredComponents() {
        return isRotor(rotorStack()) && isNozzle(nozzleStack());
    }

    private int rotorWear() {
        return CavitationPartItem.rotorWear(rotorStack());
    }

    private int rotorWearLimit() {
        return CavitationPartItem.maxRotorDurability(rotorStack());
    }

    private boolean hasActiveRecipe() {
        return activeProcessingTicks > 0 && remainingEnergy > 0.0001;
    }

    private boolean isWorking() {
        return hasActiveRecipe() && !redstoneDisabled();
    }

    private boolean redstoneDisabled() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    private void coolDown() {
        int cooling = 1 + (hasHeatCore() ? Math.max(0, heatCoreStage() / 2) : 0);
        if (heatStrain > 0) {
            heatStrain = Math.max(0, heatStrain - cooling);
        }
    }

    private void clearInactiveProgress() {
        if (!hasActiveRecipe() && progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    private void clearActiveRecipe() {
        progress = 0;
        activeProcessingTicks = 0;
        activeTotalEnergy = 0.0;
        remainingEnergy = 0.0;
        generationCarry = 0.0;
        activeHeatStrain = 0;
        activeWear = 0;
        activeFluid = FluidStack.EMPTY;
        activeFluidOutput = FluidStack.EMPTY;
    }

    private void drainInputContainer() {
        ItemStack stack = processInventory.getStackInSlot(SLOT_FLUID_INPUT_CONTAINER);
        if (stack.isEmpty()) {
            return;
        }
        FluidActionResult result = FluidUtil.tryEmptyContainer(stack, inputTank, Integer.MAX_VALUE, null, true);
        if (result.isSuccess()) {
            processInventory.setStackInSlot(SLOT_FLUID_INPUT_CONTAINER, result.getResult());
            setChanged();
        }
    }

    public static boolean isFluidInputContainer(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).isPresent();
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        if (energyStored() <= 0) {
            return false;
        }

        MachineStatAccumulator stats = effectiveStats();
        int remainingOutput = Math.min(effectiveOutputRate(stats), energyStored());
        boolean exported = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (remainingOutput <= 0 || energyStored() <= 0) {
                return exported;
            }

            IEnergyStorage target = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    pos.relative(direction),
                    direction.getOpposite()
            );
            if (target == null || !target.canReceive()) {
                continue;
            }

            int extracted = extractEnergyInternal(remainingOutput, true);
            int received = target.receiveEnergy(extracted, false);
            int delivered = extractEnergyInternal(received, false);
            energyFlow.recordOutput(delivered);
            remainingOutput -= delivered;
            exported |= delivered > 0;
        }
        return exported;
    }

    private int receiveGeneratedEnergy(int toReceive, boolean simulate) {
        int received = receiveInternalEnergy(toReceive, simulate);
        int remaining = toReceive - received;
        IEnergyStorage cell = batteryCellEnergyStorage();
        if (cell != null && cell.canReceive() && remaining > 0) {
            int cellReceived = cell.receiveEnergy(remaining, simulate);
            received += cellReceived;
            if (!simulate && cellReceived > 0) {
                setChanged();
            }
        }
        return received;
    }

    private int receiveInternalEnergy(int toReceive, boolean simulate) {
        if (toReceive <= 0) {
            return 0;
        }
        int received = Math.min(toReceive, internalEnergyCapacity() - internalEnergyStored());
        if (!simulate && received > 0) {
            internalEnergy = internalEnergyStored() + received;
            setChanged();
        }
        return received;
    }

    private int extractEnergyInternal(int toExtract, boolean simulate) {
        if (toExtract <= 0) {
            return 0;
        }
        int extracted = Math.min(toExtract, internalEnergyStored());
        if (!simulate && extracted > 0) {
            internalEnergy = internalEnergyStored() - extracted;
            setChanged();
        }
        int remaining = toExtract - extracted;
        IEnergyStorage cell = batteryCellEnergyStorage();
        if (cell != null && cell.canExtract() && remaining > 0) {
            int cellExtracted = cell.extractEnergy(remaining, simulate);
            extracted += cellExtracted;
            if (!simulate && cellExtracted > 0) {
                setChanged();
            }
        }
        return extracted;
    }

    private int energyStored() {
        return internalEnergyStored() + BatteryCellItem.energyStored(batteryCellStack());
    }

    private int energyCapacity() {
        return internalEnergyCapacity() + BatteryCellItem.energyCapacity(batteryCellStack());
    }

    private int internalEnergyCapacity() {
        return Math.max(1, (int) Math.round(effectiveStats().value(MachineStat.ENERGY_CAPACITY)));
    }

    private int internalEnergyStored() {
        return Math.max(0, Math.min(internalEnergy, internalEnergyCapacity()));
    }

    private int effectiveOutputRate(MachineStatAccumulator stats) {
        ItemStack connectorStack = energyConnectorStack();
        if (connectorStack.getItem() instanceof EnergyConnectorItem connector) {
            return connector.tier().transferRate();
        }
        return Math.max(0, (int) Math.round(stats.value(MachineStat.ENERGY_TRANSFER)));
    }

    private int effectiveFluidTransfer(MachineStatAccumulator stats) {
        return Math.max(1, (int) Math.round(stats.value(MachineStat.FLUID_TRANSFER)));
    }

    private IEnergyStorage batteryCellEnergyStorage() {
        ItemStack cell = batteryCellStack();
        return cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
    }

    private ItemStack rotorStack() {
        return gearInventory.getStackInSlot(SLOT_ROTOR);
    }

    private ItemStack nozzleStack() {
        return gearInventory.getStackInSlot(SLOT_NOZZLE);
    }

    private ItemStack heatCoreStack() {
        return gearInventory.getStackInSlot(SLOT_HEAT_CORE);
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private ItemStack servoStack() {
        return gearInventory.getStackInSlot(SLOT_SERVO);
    }

    private ItemStack energyConnectorStack() {
        return gearInventory.getStackInSlot(SLOT_ENERGY_CONNECTOR);
    }

    private boolean hasHeatCore() {
        return isHeatCore(heatCoreStack());
    }

    private int heatCoreStage() {
        return heatCoreStack().getItem() instanceof SolidFuelBurnerPartItem part ? part.stage() : 0;
    }

    private void applyPartStats(MachineStatAccumulator stats, ItemStack stack) {
        if (stack.getItem() instanceof MachinePartItem) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        }
    }

    private void applyHeatCoreStats(MachineStatAccumulator stats, ItemStack stack) {
        if (!(stack.getItem() instanceof SolidFuelBurnerPartItem part)
                || part.partType() != MachinePartType.HEAT_CORE) {
            return;
        }
        MachineStatAccumulator contribution = ComponentBaseStatCatalog.effectiveStats(stack);
        if (contribution == null) {
            return;
        }
        applyMoreStat(stats, MachineStat.HEAT_TRANSFER, contribution.value(MachineStat.HEAT_TRANSFER));
        applyMoreStat(stats, MachineStat.HEAT_ISOLATION, contribution.value(MachineStat.HEAT_ISOLATION));
        applyMoreStat(stats, MachineStat.COOLING_RATE, contribution.value(MachineStat.COOLING_RATE));
        applyMoreStat(stats, MachineStat.TEMPERATURE_STABILITY, contribution.value(MachineStat.TEMPERATURE_STABILITY));
        applyMoreStat(stats, MachineStat.OVERHEAT_TOLERANCE, contribution.value(MachineStat.OVERHEAT_TOLERANCE));
        applyAddStat(stats, MachineStat.MAX_TEMPERATURE, contribution.value(MachineStat.MAX_TEMPERATURE));
    }

    private void applyMoreStat(MachineStatAccumulator stats, MachineStat stat, double value) {
        if (Math.abs(value - 1.0) > 0.0001) {
            stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, stat, ModifierOperation.MORE, value));
        }
    }

    private void applyAddStat(MachineStatAccumulator stats, MachineStat stat, double value) {
        if (Math.abs(value) > 0.0001) {
            stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, stat, ModifierOperation.ADD, value));
        }
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
    }

    private void loadGearInventory(CompoundTag gearTag, HolderLookup.Provider registries) {
        int savedSize = gearTag.getInt("Size");
        if (savedSize == GEAR_SLOT_COUNT) {
            gearInventory.deserializeNBT(registries, gearTag);
            return;
        }

        ItemStackHandler legacyGear = new ItemStackHandler(Math.max(0, savedSize));
        legacyGear.deserializeNBT(registries, gearTag);
        gearInventory.setSize(GEAR_SLOT_COUNT);
        for (int slot = 0; slot < Math.min(legacyGear.getSlots(), GEAR_SLOT_COUNT); slot++) {
            gearInventory.setStackInSlot(slot, legacyGear.getStackInSlot(slot));
        }
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private final class ProcessItemHandler implements IItemHandler {
        private final int firstSlot;
        private final int lastSlot;
        private final boolean allowInsert;
        private final boolean allowExtract;

        private ProcessItemHandler(int firstSlot, int lastSlot, boolean allowInsert, boolean allowExtract) {
            this.firstSlot = firstSlot;
            this.lastSlot = lastSlot;
            this.allowInsert = allowInsert;
            this.allowExtract = allowExtract;
        }

        @Override
        public int getSlots() {
            return lastSlot - firstSlot + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(mappedSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return allowInsert ? processInventory.insertItem(mappedSlot(slot), stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return allowExtract ? processInventory.extractItem(mappedSlot(slot), amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(mappedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return allowInsert && processInventory.isItemValid(mappedSlot(slot), stack);
        }

        private int mappedSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
            }
            return firstSlot + slot;
        }
    }

    private final class InputFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> inputTank.getFluidInTank(0);
                case 1 -> outputTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> inputTank.getTankCapacity(0);
                case 1 -> outputTank.getTankCapacity(0);
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && inputTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return resource.isEmpty() ? 0 : inputTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, outputTank.getFluid())) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            int capped = Math.min(maxDrain, effectiveFluidTransfer(effectiveStats()));
            return capped <= 0 ? FluidStack.EMPTY : outputTank.drain(capped, action);
        }
    }

    private final class GeneratorEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return extractEnergyInternal(Math.min(toExtract, effectiveOutputRate(effectiveStats())), simulate);
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
            return energyStored() > 0 && effectiveOutputRate(effectiveStats()) > 0;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
