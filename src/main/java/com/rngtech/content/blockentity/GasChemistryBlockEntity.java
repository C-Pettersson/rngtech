package com.rngtech.content.blockentity;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.chemistry.GasChemistryMachine;
import com.rngtech.content.energy.CombustionMixture;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.GasChemistryPartItem;
import com.rngtech.content.item.GearParts;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.menu.GasChemistryMenu;
import com.rngtech.content.purge.FluidOutputOverflow;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.CoalGasificationRecipe;
import com.rngtech.content.recipe.CoalGasificationRecipeInput;
import com.rngtech.content.recipe.GasCombustionRecipe;
import com.rngtech.content.recipe.GasReformingRecipe;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModRecipes;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class GasChemistryBlockEntity extends BaseMachineBlockEntity
        implements MenuProvider, PurgeableFluidStorage, MachineInfoProvider {
    private final EnergyExportTargets exportTargets = new EnergyExportTargets(this);
    private final RecipeCache<CoalGasificationRecipe> gasificationRecipes = new RecipeCache<>(1);
    private final RecipeCache<GasCombustionRecipe> combustionRecipes = new RecipeCache<>(1);
    private final RecipeCache<GasReformingRecipe> reformingRecipes = new RecipeCache<>(1);
    private static final RecipeInputFilter<CoalGasificationRecipeInput, CoalGasificationRecipe> INPUT_FILTER = new RecipeInputFilter<>(
            ModRecipes.COAL_GASIFICATION_TYPE::get,
            (recipe, item) -> recipe.carbonInput().test(item));
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
    private static final int DATA_REFINEMENT_POTENTIAL = 17;
    private static final int DATA_FLAT_ENERGY_GENERATION = 18;
    private static final int DATA_BASE_ENERGY_GENERATION = 19;
    private static final int DATA_WATER_FLUID_ID = 20;
    private static final int DATA_INPUT_FLUID_ID = 21;
    private static final int DATA_OUTPUT_FLUID_ID = 22;
    private static final int DATA_SECONDARY_OUTPUT_FLUID_ID = 23;
    private static final int DATA_MIXTURE = 24;
    private static final int DATA_EXHAUST_PER_CYCLE = 25;
    private static final int STAT_SCALE = 100;
    private static final int VISUAL_SYNC_INTERVAL = 5;
    private static final double EPSILON = 1.0E-9;

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
            new MachineEnergyStorage(this::internalEnergyCapacity, () -> Integer.MAX_VALUE, () -> Integer.MAX_VALUE, this::setChanged);
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
                case STEAM_METHANE_REFORMER -> stack.is(ModFluids.METHANE_SOURCE.get()) || stack.is(ModFluids.SYNGAS_SOURCE.get())
                        || stack.is(ModFluids.CARBON_MONOXIDE_SOURCE.get());
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
            // Carbon Monoxide from a reform, Carbon Exhaust from a water-gas shift.
            return machine == GasChemistryMachine.STEAM_METHANE_REFORMER
                    && (stack.is(ModFluids.CARBON_MONOXIDE_SOURCE.get()) || stack.is(ModFluids.CARBON_EXHAUST_SOURCE.get()));
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
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedGeneratorEnergyView = energyFlow.track(generatorEnergyView);
    private final IEnergyStorage trackedConsumerEnergyView = energyFlow.track(consumerEnergyView);
    private final InstalledBatteryCell batteryCell = new InstalledBatteryCell(this::batteryCellStack);
    private final BulkSpeedState bulkSpeed = new BulkSpeedState();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_PROGRESS -> syncedProgress(0, progress);
                case DATA_PROCESSING_TICKS -> activeTicks;
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_WATER -> waterTank.getFluidAmount();
                case DATA_INPUT -> inputTank.getFluidAmount();
                case DATA_OUTPUT -> outputTank.getFluidAmount();
                case DATA_SECONDARY_OUTPUT -> secondaryOutputTank.getFluidAmount();
                case DATA_TANK_CAPACITY -> inputTank.getCapacity();
                case DATA_STATUS -> statusCode();
                case DATA_ENERGY_DELTA -> currentEnergyDelta();
                case DATA_ENERGY_GENERATION -> (int) Math.round(stats.effectiveEnergyGenerationMultiplier() * STAT_SCALE);
                case DATA_FLAT_ENERGY_GENERATION -> (int) Math.round(stats.effectiveFlatEnergyGenerationBonus() * STAT_SCALE);
                case DATA_BASE_ENERGY_GENERATION -> (int) Math.round(baseEnergyPerTick() * STAT_SCALE);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_WATER_FLUID_ID -> BuiltInRegistries.FLUID.getId(waterTank.getFluid().getFluid());
                case DATA_INPUT_FLUID_ID -> BuiltInRegistries.FLUID.getId(inputTank.getFluid().getFluid());
                case DATA_OUTPUT_FLUID_ID -> BuiltInRegistries.FLUID.getId(outputTank.getFluid().getFluid());
                case DATA_SECONDARY_OUTPUT_FLUID_ID -> BuiltInRegistries.FLUID.getId(secondaryOutputTank.getFluid().getFluid());
                case DATA_MIXTURE -> mixture;
                case DATA_EXHAUST_PER_CYCLE -> exhaustPerCycle();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_EXHAUST_PER_CYCLE + 1;
        }
    };

    private int progress;
    private int activeTicks;
    private long activeEnergy;
    private double energyCarry;
    private int mixture = CombustionMixture.RATED_MIXTURE;
    private double burnProgress;
    private double exhaustCharge;
    private CombustorVisualState visualState = CombustorVisualState.IDLE;
    private CombustorVisualState.Key lastSyncedVisual;
    private final CombustorVisualState.Animation animation = new CombustorVisualState.Animation();
    private GasProcessorVisualState processorVisualState = GasProcessorVisualState.IDLE;
    private GasProcessorVisualState.Key lastSyncedProcessorVisual;
    private final GasProcessorVisualState.Animation processorAnimation = new GasProcessorVisualState.Animation();

    public GasChemistryBlockEntity(BlockPos pos, BlockState blockState, GasChemistryMachine machine) {
        super(ModBlockEntities.GAS_CHEMISTRY.get(), pos, blockState, machine.machineType(), SLOT_PROCESS_INPUT, SLOT_PROCESS_INPUT, SLOT_PROCESS_OUTPUT);
        trackStatSlots(gearInventory);
        this.machine = machine;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GasChemistryBlockEntity gasChemistry) {
        gasChemistry.updateTankCapacity(gasChemistry.effectiveStats());
        boolean worked = gasChemistry.tickRecipe();
        boolean exported = gasChemistry.machine == GasChemistryMachine.SYNGAS_COMBUSTOR && gasChemistry.exportEnergy(level, pos);
        BaseMachineBlock.setActive(level, pos, state, worked);
        if (worked || exported) {
            gasChemistry.setChanged();
        }
        gasChemistry.syncVisualState(level);
    }

    /** Fluid Capacity sizes every tank; fluid above a smaller capacity stays until it drains. */
    private void updateTankCapacity(MachineStatAccumulator stats) {
        int capacity = Math.max(TANK_CAPACITY, (int) Math.round(stats.value(MachineStat.FLUID_CAPACITY)));
        if (inputTank.getCapacity() != capacity) {
            waterTank.setCapacity(capacity);
            inputTank.setCapacity(capacity);
            outputTank.setCapacity(capacity);
            secondaryOutputTank.setCapacity(capacity);
        }
    }

    private boolean isCombustor() {
        return machine == GasChemistryMachine.SYNGAS_COMBUSTOR;
    }

    /**
     * Machines saved while the block was a solid cube stored no light inside it, which leaves the renderer's parts unlit.
     * Rechecking the block's own light once on load fixes those saves.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            level.getLightEngine().checkBlock(worldPosition);
        }
    }

    /** Client copy of the state the Syngas Combustor renderer draws; synced from the server in the update tag. */
    public CombustorVisualState visualState() {
        return visualState;
    }

    public CombustorVisualState.Animation animation() {
        return animation;
    }

    /** Client copy of the state the Coal Gasifier and Steam Methane Reformer renderers draw. */
    public GasProcessorVisualState processorVisualState() {
        return processorVisualState;
    }

    public GasProcessorVisualState.Animation processorAnimation() {
        return processorAnimation;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return isCombustor() ? currentVisualState().save() : currentProcessorVisualState().save();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadVisualState(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        loadVisualState(packet.getTag());
    }

    private void loadVisualState(CompoundTag tag) {
        if (isCombustor()) {
            visualState = CombustorVisualState.load(tag);
        } else {
            processorVisualState = GasProcessorVisualState.load(tag);
        }
    }

    private void syncVisualState(Level level) {
        if (level.getGameTime() % VISUAL_SYNC_INTERVAL != 0) {
            return;
        }
        boolean changed;
        if (isCombustor()) {
            CombustorVisualState.Key key = currentVisualState().key();
            changed = !key.equals(lastSyncedVisual);
            lastSyncedVisual = key;
        } else {
            GasProcessorVisualState.Key key = currentProcessorVisualState().key();
            changed = !key.equals(lastSyncedProcessorVisual);
            lastSyncedProcessorVisual = key;
        }
        if (changed) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    GasProcessorVisualState currentProcessorVisualState() {
        IEnergyStorage cell = batteryCellStorage();
        float capacity = inputTank.getCapacity();
        return new GasProcessorVisualState(
                isActive(),
                (float) effectiveStats().value(MachineStat.PROCESSING_SPEED),
                activeTicks <= 0 ? 0.0F : Mth.clamp(progress / (float) activeTicks, 0.0F, 1.0F),
                waterTank.getFluidAmount() / capacity,
                inputTank.getFluidAmount() / capacity,
                inputTank.getFluid().getFluid(),
                outputTank.getFluidAmount() / capacity,
                secondaryOutputTank.getFluidAmount() / capacity,
                fill(processInventory.getStackInSlot(SLOT_PROCESS_INPUT)),
                fill(processInventory.getStackInSlot(SLOT_PROCESS_OUTPUT)),
                gearLook(heatCoreStack()),
                gearLook(servoStack()),
                gearLook(batteryCellStack()),
                cell == null || cell.getMaxEnergyStored() <= 0 ? 0.0F : cell.getEnergyStored() / (float) cell.getMaxEnergyStored(),
                hasCatalystGearSlot() && isReformingCatalyst(catalystStack())
        );
    }

    private static float fill(ItemStack stack) {
        return stack.isEmpty() ? 0.0F : stack.getCount() / (float) stack.getMaxStackSize();
    }

    CombustorVisualState currentVisualState() {
        IEnergyStorage cell = batteryCellStorage();
        return new CombustorVisualState(
                isActive(),
                (float) effectiveStats().value(MachineStat.PROCESSING_SPEED),
                mixture,
                inputTank.getFluidAmount() / (float) inputTank.getCapacity(),
                inputTank.getFluid().getFluid(),
                outputTank.getFluidAmount() / (float) outputTank.getCapacity(),
                outputTank.getFluid().getFluid(),
                gearLook(servoStack()),
                gearLook(batteryCellStack()),
                cell == null || cell.getMaxEnergyStored() <= 0 ? 0.0F : cell.getEnergyStored() / (float) cell.getMaxEnergyStored()
        );
    }

    private static String gearLook(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        if (GearParts.isUnique(stack)) {
            return "unique";
        }
        if (stack.getItem() instanceof ServoItem servo) {
            return servo.material().getSerializedName();
        }
        if (stack.getItem() instanceof BatteryCellItem cell) {
            return cell.material().unique() ? "unique" : cell.material().getSerializedName();
        }
        if (stack.getItem() instanceof SolidFuelBurnerPartItem part && part.heatCoreMaterial() != null) {
            return part.heatCoreMaterial().getSerializedName();
        }
        return "";
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
        return machine == GasChemistryMachine.SYNGAS_COMBUSTOR ? trackedGeneratorEnergyView : trackedConsumerEnergyView;
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
            ).withCapacity(waterTank::getCapacity));
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
            ).withCapacity(inputTank::getCapacity));
        }
        targets.add(FluidPurgeTarget.of(
                PURGE_OUTPUT_TANK,
                Component.translatable("rngtech.purge.target.output_tank"),
                FluidPurgeRole.OUTPUT,
                outputTank::getFluid,
                outputTank::drain,
                this::setChanged
        ).withCapacity(outputTank::getCapacity));
        if (hasSecondaryOutputTank()) {
            targets.add(FluidPurgeTarget.of(
                    PURGE_SECONDARY_OUTPUT_TANK,
                    Component.translatable("rngtech.purge.target.secondary_output_tank"),
                    FluidPurgeRole.OUTPUT,
                    secondaryOutputTank::getFluid,
                    secondaryOutputTank::drain,
                    this::setChanged
            ).withCapacity(secondaryOutputTank::getCapacity));
        }
        return List.copyOf(targets);
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        int status = statusCode();
        boolean generator = machine == GasChemistryMachine.SYNGAS_COMBUSTOR;
        int energyDelta = hasEnergyRateStatus(status, generator) ? currentEnergyDelta() : 0;
        AdjacentEnergyConnector.Info connector = generator
                ? AdjacentEnergyConnector.forSource(level, worldPosition)
                : AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder(machine.blockId())
                .stage(machine.stage())
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY,
                                isActive(),
                                status == STATUS_NO_FLUID || status == STATUS_NO_INPUT
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .progress(progress, activeTicks)
                .energy(energyStored(), energyCapacity(), generator ? energyDelta : -energyDelta)
                .energyTelemetry(
                        energyFlow.lastInput(),
                        energyFlow.lastOutput(),
                        connector.transferRate(),
                        energyBottleneck(generator, connector, energyDelta)
                )
                .gear(gearSummary())
                .output(outputSummary(status))
                .refinement(machineTraits())
                .build();
    }

    private static MachineInfoSnapshot.EnergyBottleneck energyBottleneck(
            boolean generator,
            AdjacentEnergyConnector.Info connector,
            int energyDelta
    ) {
        if (!connector.present() || energyDelta <= connector.transferRate()) {
            return MachineInfoSnapshot.EnergyBottleneck.NONE;
        }
        return generator
                ? MachineInfoSnapshot.EnergyBottleneck.CONNECTOR_OUTPUT
                : MachineInfoSnapshot.EnergyBottleneck.CONNECTOR_INPUT;
    }

    private static boolean hasEnergyRateStatus(int status, boolean generator) {
        return status == STATUS_READY || (!generator && status == STATUS_NO_POWER);
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_MISSING_HEAT_CORE -> "no_heat_core";
            case STATUS_MISSING_CATALYST -> "no_catalyst";
            case STATUS_NO_RECIPE -> "no_recipe";
            case STATUS_NO_POWER -> "no_power";
            case STATUS_ENERGY_FULL -> "energy_full";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_NO_FLUID -> "no_fluid";
            case STATUS_NO_INPUT -> "no_input";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.gas_chemistry.status." + name;
    }

    private MachineInfoSnapshot.GearSummary gearSummary() {
        if (hasHeatCoreGearSlot()) {
            return isHeatCore(heatCoreStack())
                    ? MachineInfoSnapshot.GearSummary.HEAT_CORE_INSTALLED
                    : MachineInfoSnapshot.GearSummary.MISSING_HEAT_CORE;
        }
        return batteryCellStorage() != null
                ? MachineInfoSnapshot.GearSummary.BATTERY_CELL_INSTALLED
                : MachineInfoSnapshot.GearSummary.NONE;
    }

    private static MachineInfoSnapshot.OutputSummary outputSummary(int status) {
        return switch (status) {
            case STATUS_OUTPUT_FULL -> MachineInfoSnapshot.OutputSummary.OUTPUT_FULL;
            case STATUS_ENERGY_FULL -> MachineInfoSnapshot.OutputSummary.ENERGY_FULL;
            default -> MachineInfoSnapshot.OutputSummary.NONE;
        };
    }

    private boolean isActive() {
        BlockState state = getBlockState();
        return state.hasProperty(BaseMachineBlock.ACTIVE) && state.getValue(BaseMachineBlock.ACTIVE);
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
        return GearParts.is(stack, MachinePartType.HEAT_CORE) && GearParts.stage(stack) >= requiredHeatCoreStage();
    }

    public boolean isBatteryCell(ItemStack stack) {
        return stack.getItem() instanceof BatteryCellItem cell && cell.material().stage() >= machine.stage();
    }

    public boolean isServo(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.SERVO) && GearParts.stage(stack) >= 4;
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
            interruptRecipe(recipe == null);
            resetActiveRecipe(recipe == null ? 0 : recipe.processingTicks(), recipe == null ? 0 : recipe.energy());
            return false;
        }
        return tickProcess(recipe, recipe.processingTicks(), recipe.energy(), () -> {
            processInventory.extractItem(SLOT_PROCESS_INPUT, 1, false);
            waterTank.drain(recipe.waterInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            outputTank.fill(recipe.outputFluid(), IFluidHandler.FluidAction.EXECUTE);
            mergeResidue(recipe.residue());
        });
    }

    /**
     * One tick of a powered gas process. A job may finish at once through Instant Process, paying its whole FE, and each
     * finished job feeds Bulk Speed.
     */
    private boolean tickProcess(Object recipe, int baseTicks, long baseEnergy, Runnable complete) {
        if (progress == 0 && bulkSpeed.startRecipe(recipe)) {
            setChanged();
        }
        activeTicks = adjustedTicks(baseTicks);
        activeEnergy = adjustedEnergy(baseEnergy);
        if (progress == 0 && level != null && ProcessingChance.rollInstant(level, effectiveStats())) {
            int fullCost = (int) Math.min(Integer.MAX_VALUE, activeEnergy);
            if (consumeEnergy(fullCost, true) >= fullCost) {
                consumeEnergy(fullCost, false);
                markInstantProcess();
                completeProcess(complete);
                return true;
            }
        }
        int cost = Math.max(1, Mth.ceil(activeEnergy / (double) activeTicks));
        if (consumeEnergy(cost, true) < cost) {
            return false;
        }
        consumeEnergy(cost, false);
        progress++;
        if (progress >= activeTicks) {
            completeProcess(complete);
        }
        return true;
    }

    private void completeProcess(Runnable complete) {
        complete.run();
        finishRecipe();
        bulkSpeed.recordProcess(effectiveTraits());
    }

    /** Losing the recipe part-way through a job breaks the Bulk Speed chain; a full output only pauses it. */
    private void interruptRecipe(boolean recipeLost) {
        if (recipeLost && progress > 0 && bulkSpeed.reset()) {
            setChanged();
        }
    }

    private boolean tickCombustion() {
        GasCombustionRecipe recipe = currentCombustionRecipe();
        if (recipe == null || !canRunCombustion(recipe)) {
            resetActiveRecipe(recipe == null ? 0 : recipe.processingTicks(), recipe == null ? 0 : recipe.energy());
            return false;
        }
        activeTicks = adjustedTicks(recipe.processingTicks());
        activeEnergy = adjustedGeneration(recipe.energy(), activeTicks);
        int generated = generateEnergyThisTick(CombustionMixture.power(mixture));
        if (storeEnergy(generated, true) < generated) {
            return false;
        }
        storeEnergy(generated, false);
        double step = CombustionMixture.fuelRate(mixture);
        burnProgress += step;
        exhaustCharge += exhaustPerTick(recipe) * step;
        if (burnProgress >= activeTicks - EPSILON) {
            inputTank.drain(recipe.gasInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            int exhaust = (int) Math.floor(exhaustCharge + EPSILON);
            if (exhaust > 0) {
                outputTank.fill(recipe.exhaustFluid().copyWithAmount(exhaust), IFluidHandler.FluidAction.EXECUTE);
            }
            exhaustCharge = Math.max(0.0, exhaustCharge - exhaust);
            burnProgress = Math.max(0.0, burnProgress - activeTicks);
            energyCarry = 0.0D;
        }
        progress = (int) burnProgress;
        return true;
    }

    /**
     * Carbon Exhaust charged per rated tick of the cycle at the current mixture. Charging it as the fuel burns, not at the
     * end, means switching mixture late in a cycle cannot dodge the exhaust of the fuel already burned.
     */
    private double exhaustPerTick(GasCombustionRecipe recipe) {
        return recipe.exhaustFluid().getAmount() * CombustionMixture.exhaustPerFuel(mixture) / Math.max(1, activeTicks);
    }

    /** The exhaust this cycle will have made by its end, at the current mixture. */
    private FluidStack projectedExhaust(GasCombustionRecipe recipe) {
        int ticks = adjustedTicks(recipe.processingTicks());
        double perTick = recipe.exhaustFluid().getAmount() * CombustionMixture.exhaustPerFuel(mixture) / ticks;
        int amount = (int) Math.ceil(exhaustCharge + Math.max(0.0, ticks - burnProgress) * perTick - EPSILON);
        return recipe.exhaustFluid().copyWithAmount(Math.max(1, amount));
    }

    private int exhaustPerCycle() {
        GasCombustionRecipe recipe = isCombustor() ? currentCombustionRecipe() : null;
        return recipe == null ? 0 : CombustionMixture.exhaustAmount(recipe.exhaustFluid().getAmount(), mixture);
    }

    public int mixture() {
        return mixture;
    }

    public void setMixture(int mixture) {
        int clamped = CombustionMixture.clamp(mixture);
        if (clamped != this.mixture) {
            this.mixture = clamped;
            setChanged();
        }
    }

    private boolean tickReforming() {
        GasReformingRecipe recipe = currentReformingRecipe();
        if (recipe == null || !canRunReforming(recipe)) {
            interruptRecipe(recipe == null);
            resetActiveRecipe(recipe == null ? 0 : recipe.processingTicks(), recipe == null ? 0 : recipe.energy());
            return false;
        }
        return tickProcess(recipe, recipe.processingTicks(), recipe.energy(), () -> {
            inputTank.drain(recipe.gasInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            waterTank.drain(recipe.waterInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            outputTank.fill(recipe.hydrogenFluid(), IFluidHandler.FluidAction.EXECUTE);
            secondaryOutputTank.fill(recipe.byproductFluid(), IFluidHandler.FluidAction.EXECUTE);
        });
    }

    private CoalGasificationRecipe currentGasificationRecipe() {
        if (level == null) {
            return null;
        }
        ItemStack carbon = processInventory.getStackInSlot(SLOT_PROCESS_INPUT);
        FluidStack water = waterTank.getFluid();
        return gasificationRecipes.lookup(0, level).item(carbon).fluid(water)
                .find(() -> GasChemistryRecipes.gasification(level, carbon, water));
    }

    private GasCombustionRecipe currentCombustionRecipe() {
        if (level == null) {
            return null;
        }
        FluidStack gas = inputTank.getFluid();
        return combustionRecipes.lookup(0, level).fluid(gas).find(() -> GasChemistryRecipes.combustion(level, gas));
    }

    private GasReformingRecipe currentReformingRecipe() {
        if (level == null) {
            return null;
        }
        ItemStack catalyst = catalystStack();
        FluidStack feedGas = inputTank.getFluid();
        FluidStack water = waterTank.getFluid();
        return reformingRecipes.lookup(0, level).item(catalyst).fluid(feedGas).fluid(water)
                .find(() -> GasChemistryRecipes.reforming(level, catalyst, feedGas, water));
    }

    /** How many recipe lookups searched the recipes instead of reusing a remembered result. */
    public int recipeSearches() {
        return gasificationRecipes.searches() + combustionRecipes.searches() + reformingRecipes.searches();
    }

    private boolean canRunGasification(CoalGasificationRecipe recipe) {
        return heatCoreStage() >= recipe.minimumHeatCoreStage()
                && waterTank.getFluidAmount() >= recipe.waterInput().amount()
                && canAcceptOutput(outputTank, recipe.outputFluid())
                && canAcceptResidue(recipe.residue());
    }

    private boolean canRunCombustion(GasCombustionRecipe recipe) {
        return inputTank.getFluidAmount() >= recipe.gasInput().amount()
                && canAcceptOutput(outputTank, projectedExhaust(recipe));
    }

    private boolean canRunReforming(GasReformingRecipe recipe) {
        return heatCoreStage() >= requiredHeatCoreStage()
                && catalystStage() >= recipe.minimumCatalystStage()
                && inputTank.getFluidAmount() >= recipe.gasInput().amount()
                && waterTank.getFluidAmount() >= recipe.waterInput().amount()
                && canAcceptOutput(outputTank, recipe.hydrogenFluid())
                && canAcceptOutput(secondaryOutputTank, recipe.byproductFluid());
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
        if (!canAcceptOutput(outputTank, projectedExhaust(recipe))) {
            return STATUS_OUTPUT_FULL;
        }
        int generated = combustionEnergyDelta(recipe);
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
                || !canAcceptOutput(secondaryOutputTank, recipe.byproductFluid())) {
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
                yield recipe == null ? 0 : combustionEnergyDelta(recipe);
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

    /** FE/t at the current mixture. */
    private int combustionEnergyDelta(GasCombustionRecipe recipe) {
        int ticks = adjustedTicks(recipe.processingTicks());
        return Math.max(1, Mth.ceil(adjustedGeneration(recipe.energy(), ticks) / (double) ticks * CombustionMixture.power(mixture)));
    }

    private int adjustedTicks(int baseTicks) {
        return Math.max(1, Mth.ceil(baseTicks / effectiveStats().value(MachineStat.PROCESSING_SPEED)));
    }

    private long adjustedEnergy(long baseEnergy) {
        return Math.max(1L, Mth.lfloor(baseEnergy * effectiveStats().value(MachineStat.ENERGY_USAGE)));
    }

    private long adjustedGeneration(long baseEnergy, int processingTicks) {
        MachineStatAccumulator stats = effectiveStats();
        return Math.max(1L, Mth.lfloor(RNGTechConfig.generatedEnergy(stats.generatedEnergyTotal(baseEnergy, processingTicks) * stats.value(MachineStat.EFFICIENCY))));
    }

    private boolean canAcceptOutput(FluidTank tank, FluidStack output) {
        return FluidOutputOverflow.canAcceptOrVoidExcess(tank, output, autoPurgesFluidOutput());
    }

    private boolean autoPurgesFluidOutput() {
        return hasServoGearSlot() && MachineImplicitCatalog.hasBehavior(servoStack(), MachineBehavior.AUTO_PURGE);
    }

    public MachineStatAccumulator effectiveStats() {
        return cachedStats(bulkSpeed.count());
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = switch (machine) {
            case COAL_GASIFIER -> MachineBaseStatCatalog.coalGasifier();
            case SYNGAS_COMBUSTOR -> MachineBaseStatCatalog.syngasCombustor();
            case STEAM_METHANE_REFORMER -> MachineBaseStatCatalog.steamMethaneReformer();
        };
        machineTraits().activeModifiers().forEach(stats::apply);
        if (hasHeatCoreGearSlot()) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, heatCoreStack());
        }
        if (hasServoGearSlot()) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, servoStack());
        }
        if (hasCatalystGearSlot()) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, catalystStack());
        }
        bulkSpeed.apply(stats, effectiveTraits());
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

    /** How many times an energy export looked a neighbour up instead of reusing its cached capability. */
    public int exportTargetLookups() {
        return exportTargets.lookups();
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        int remaining = energyStored();
        boolean moved = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (remaining <= 0) {
                break;
            }
            IEnergyStorage target = exportTargets.get(level, pos, direction);
            if (target == null || !target.canReceive()) {
                continue;
            }
            int offered = extractEnergyInternal(remaining, true);
            int received = target.receiveEnergy(offered, false);
            int delivered = extractEnergyInternal(received, false);
            energyFlow.recordOutput(delivered);
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

    private IEnergyStorage batteryCellStorage() {
        return batteryCell.storage();
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private int generateEnergyThisTick(double factor) {
        if (activeTicks <= 0 || activeEnergy <= 0) {
            return 0;
        }
        double exact = activeEnergy / (double) activeTicks * factor + energyCarry;
        int whole = Mth.floor(exact);
        energyCarry = exact - whole;
        return Math.max(1, whole);
    }

    private void resetActiveRecipe(int ticks, long energy) {
        progress = 0;
        burnProgress = 0.0;
        exhaustCharge = 0.0;
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
        return GearParts.is(heatCoreStack(), MachinePartType.HEAT_CORE) ? GearParts.stage(heatCoreStack()) : 0;
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
        bulkSpeed.save(tag);
        if (isCombustor()) {
            tag.putInt("Mixture", mixture);
            tag.putDouble("BurnProgress", burnProgress);
            tag.putDouble("ExhaustCharge", exhaustCharge);
        }
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
        bulkSpeed.load(tag);
        if (isCombustor()) {
            mixture = tag.contains("Mixture") ? CombustionMixture.clamp(tag.getInt("Mixture")) : CombustionMixture.RATED_MIXTURE;
            // Combustors saved before mixtures kept whole-tick progress and charged their exhaust at the end of the cycle.
            burnProgress = tag.contains("BurnProgress") ? Math.max(0.0, tag.getDouble("BurnProgress")) : progress;
            exhaustCharge = tag.contains("ExhaustCharge") ? Math.max(0.0, tag.getDouble("ExhaustCharge")) : 0.0;
            progress = (int) burnProgress;
        }
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
            FluidTank exposed = exposedTank(tank);
            return exposed == null ? 0 : exposed.getCapacity();
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
            return storeEnergy(toReceive, simulate);
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
            return energyStored() < energyCapacity();
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
            if (!isItemValid(slot, stack)) {
                return stack;
            }
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
            return processInventory.isItemValid(SLOT_PROCESS_INPUT, stack) && isGasificationInput(stack);
        }
    }

    public boolean isGasificationInput(ItemStack stack) {
        return INPUT_FILTER.accepts(level, stack);
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

    /** Generation at the current mixture before machine and part stats, for the Stats tab breakdown; 0 when unknown. */
    private double baseEnergyPerTick() {
        GasCombustionRecipe recipe = currentCombustionRecipe();
        return recipe == null ? 0.0 : recipe.energy() / (double) Math.max(1, recipe.processingTicks()) * CombustionMixture.power(mixture);
    }
}
