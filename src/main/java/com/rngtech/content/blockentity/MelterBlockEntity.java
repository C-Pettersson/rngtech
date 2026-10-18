package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.GearParts;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.content.menu.MelterMenu;
import com.rngtech.content.purge.FluidOutputOverflow;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.CountedIngredient;
import com.rngtech.content.recipe.MelterRecipe;
import com.rngtech.content.recipe.MelterRecipeInput;
import com.rngtech.content.recipe.ShapelessMatch;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.content.registry.ModTags;
import com.rngtech.rpg.BatchProcessing;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.EscapementState;
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
import com.rngtech.rpg.progression.AscendancyFormulas;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
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

public class MelterBlockEntity extends BaseMachineBlockEntity
        implements MenuProvider, PurgeableFluidStorage, MachineMasteryHost, MachineInfoProvider {
    private final RecipeCache<MelterRecipe> recipes = new RecipeCache<>(1);
    private static final RecipeInputFilter<MelterRecipeInput, MelterRecipe> INPUT_FILTER = new RecipeInputFilter<>(
            ModRecipes.MELTER_TYPE::get,
            (recipe, item) -> recipe.primaryIngredient().test(item) || recipe.secondaryIngredient().test(item));
    private static final InputSlotPlanner<MelterRecipeInput, MelterRecipe> SLOT_PLANNER = InputSlotPlanner.shapeless(
            ModRecipes.MELTER_TYPE::get,
            recipe -> List.of(recipe.primaryIngredient(), recipe.secondaryIngredient()));
    public static final int SLOT_PRIMARY_INPUT = 0;
    public static final int SLOT_SECONDARY_INPUT = 1;
    public static final int SLOT_FLUID_INPUT_CONTAINER = 2;
    public static final int SLOT_FLUID_OUTPUT_CONTAINER = 3;
    public static final int PROCESS_SLOT_COUNT = 4;
    public static final int SLOT_HEAT_CORE = 0;
    public static final int SLOT_CRUSH_HEAD = 1;
    public static final int SLOT_BATTERY_CELL = 2;
    public static final int SLOT_SERVO = 3;
    public static final int SLOT_FLUID_PUMP = 4;
    public static final int GEAR_SLOT_COUNT = 5;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_HEAT_CORE = 1;
    public static final int STATUS_MISSING_CRUSH_HEAD = 2;
    public static final int STATUS_NO_INPUT = 3;
    public static final int STATUS_INVALID_RECIPE = 4;
    public static final int STATUS_HEAT_LOW = 5;
    public static final int STATUS_LEVEL_LOW = 6;
    public static final int STATUS_OUTPUT_TANK_FULL = 7;
    public static final int STATUS_NO_POWER = 8;
    public static final int STATUS_NO_FLUID = 9;
    public static final int STATUS_FILLING_BATCH = 10;
    public static final int STATUS_WARMING = 11;
    public static final int STATUS_HEAT_HIGH = 12;
    public static final int PURGE_INPUT_TANK = 0;
    public static final int PURGE_OUTPUT_TANK = 1;

    private static final int TANK_CAPACITY = FluidType.BUCKET_VOLUME * 4;
    private static final double NO_BATTERY_PROCESSING_SPEED = 0.80;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_HEAT = 4;
    private static final int DATA_MIN_TEMPERATURE = 5;
    private static final int DATA_PROCESSING_LEVEL = 6;
    private static final int DATA_REQUIRED_PROCESSING_LEVEL = 7;
    private static final int DATA_INPUT_FLUID = 8;
    private static final int DATA_INPUT_FLUID_CAPACITY = 9;
    private static final int DATA_OUTPUT_FLUID = 10;
    private static final int DATA_OUTPUT_FLUID_CAPACITY = 11;
    private static final int DATA_STATUS = 12;
    private static final int DATA_PROCESSING_SPEED = 13;
    private static final int DATA_EFFICIENCY = 14;
    private static final int DATA_ENERGY_USAGE = 15;
    private static final int DATA_ENERGY_CAPACITY_STAT = 16;
    private static final int DATA_ENERGY_TRANSFER = 17;
    private static final int DATA_HEAT_TRANSFER = 18;
    private static final int DATA_MAX_TEMPERATURE = 19;
    private static final int DATA_FLUID_TRANSFER = 20;
    private static final int DATA_REFINEMENT_POTENTIAL = 21;
    private static final int DATA_BATCH_SIZE = DATA_REFINEMENT_POTENTIAL + 1;
    private static final int DATA_INPUT_FLUID_ID = DATA_BATCH_SIZE + 1;
    private static final int DATA_OUTPUT_FLUID_ID = DATA_INPUT_FLUID_ID + 1;
    private static final int DATA_HEAT_SLIDER = DATA_OUTPUT_FLUID_ID + 1;
    private static final int DATA_SETPOINT = DATA_HEAT_SLIDER + 1;
    private static final int DATA_THERMAL_CONDUCTIVITY = DATA_SETPOINT + 1;
    private static final int DATA_HOLDING_ENERGY = DATA_THERMAL_CONDUCTIVITY + 1;
    private static final int DATA_HEATING_ENERGY = DATA_HOLDING_ENERGY + 1;
    private static final int DATA_HEAT_FACTOR = DATA_HEATING_ENERGY + 1;
    private static final int DATA_WOBBLE = DATA_HEAT_FACTOR + 1;
    private static final int DATA_HEAT_REACH = DATA_WOBBLE + 1;
    private static final int DATA_HEAT_MAXIMUM = DATA_HEAT_REACH + 1;
    private static final int DATA_OVERHEAT_TEMPERATURE = DATA_HEAT_MAXIMUM + 1;
    private static final int DATA_INPUT_ENERGY = DATA_OVERHEAT_TEMPERATURE + 1;
    private static final int DATA_MACHINE_PROGRESSION_START = DATA_INPUT_ENERGY + 1;
    private static final int DATA_OUTPUT_PER_MINUTE = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int DATA_COUNT = DATA_OUTPUT_PER_MINUTE + 1;
    private static final int STAT_SCALE = 100;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_PRIMARY_INPUT, SLOT_SECONDARY_INPUT -> !stack.isEmpty();
                case SLOT_FLUID_INPUT_CONTAINER -> isFluidInputContainer(stack);
                case SLOT_FLUID_OUTPUT_CONTAINER -> isFluidOutputContainer(stack);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_FLUID_INPUT_CONTAINER || slot == SLOT_FLUID_OUTPUT_CONTAINER ? 1 : super.getSlotLimit(slot);
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
                case SLOT_HEAT_CORE -> isHeatCore(stack);
                case SLOT_CRUSH_HEAD -> isCrushHead(stack);
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_SERVO -> isServo(stack);
                case SLOT_FLUID_PUMP -> isFluidPump(stack);
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
            if (slot != SLOT_BATTERY_CELL && slot != SLOT_FLUID_PUMP) {
                resetCycle();
                resetBulkSpeed();
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
    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY, MelterBlockEntity::isValidOutputFluid) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IItemHandler ingredientInputHandler = new ProcessItemHandler(SLOT_PRIMARY_INPUT, SLOT_SECONDARY_INPUT, true, false);
    private final IItemHandler containerInputHandler = new ProcessItemHandler(SLOT_FLUID_INPUT_CONTAINER, SLOT_FLUID_OUTPUT_CONTAINER, true, false);
    private final IItemHandler containerOutputHandler = new ProcessItemHandler(SLOT_FLUID_OUTPUT_CONTAINER, SLOT_FLUID_OUTPUT_CONTAINER, false, true);
    private final IEnergyStorage energyStorage = new MelterEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final IFluidHandler fluidHandler = new MelterFluidHandler();
    private final InstalledBatteryCell batteryCell = new InstalledBatteryCell(this::batteryCellStack);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_MACHINE_PROGRESSION_START && index < DATA_COUNT) {
                return MasteryMenuSupport.get(MelterBlockEntity.this, index - DATA_MACHINE_PROGRESSION_START, MelterBlockEntity.this::effectiveStats);
            }
            MachineStatAccumulator stats = effectiveStats();
            MelterRecipe recipe = nextRecipe();
            return switch (index) {
                case DATA_PROGRESS -> syncedProgress(0, currentElapsedTicks(recipe, stats));
                case DATA_PROCESSING_TICKS -> currentProcessingTicks(recipe, stats);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_HEAT -> heat.temperature();
                case DATA_MIN_TEMPERATURE -> recipe == null ? 0 : recipe.minimumTemperature();
                case DATA_HEAT_SLIDER -> heatSlider;
                case DATA_THERMAL_CONDUCTIVITY -> scaledStat(stats, MachineStat.THERMAL_CONDUCTIVITY);
                case DATA_HOLDING_ENERGY -> heatEnergy(0);
                case DATA_INPUT_ENERGY -> heatEnergy(1);
                case DATA_HEATING_ENERGY -> heatEnergy(2);
                case DATA_HEAT_FACTOR -> heatFactorDisplay();
                case DATA_WOBBLE -> wobbleDisplay();
                case DATA_SETPOINT -> recipe == null || !hasRequiredComponents() ? 0 : envelope(recipe, stats).setpoint();
                case DATA_HEAT_REACH -> recipe == null || !hasRequiredComponents() ? 0 : envelope(recipe, stats).reach();
                case DATA_HEAT_MAXIMUM -> stats.intValue(MachineStat.MAX_TEMPERATURE);
                case DATA_OVERHEAT_TEMPERATURE -> recipe == null ? 0 : envelope(recipe, stats).overheat();
                case DATA_PROCESSING_LEVEL -> processingLevel(stats);
                case DATA_REQUIRED_PROCESSING_LEVEL -> recipe == null ? 0 : recipe.requiredProcessingLevel();
                case DATA_INPUT_FLUID -> inputTank.getFluidAmount();
                case DATA_INPUT_FLUID_CAPACITY -> inputTank.getCapacity();
                case DATA_OUTPUT_FLUID -> outputTank.getFluidAmount();
                case DATA_OUTPUT_FLUID_CAPACITY -> outputTank.getCapacity();
                case DATA_STATUS -> statusCode(recipe, stats);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_HEAT_TRANSFER -> scaledStat(stats, MachineStat.HEAT_TRANSFER);
                case DATA_MAX_TEMPERATURE -> scaledStat(stats, MachineStat.MAX_TEMPERATURE);
                case DATA_FLUID_TRANSFER -> effectiveFluidTransfer(stats);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_BATCH_SIZE -> BatchProcessing.statBatchSize(stats) * STAT_SCALE;
                case DATA_INPUT_FLUID_ID -> BuiltInRegistries.FLUID.getId(inputTank.getFluid().getFluid());
                case DATA_OUTPUT_FLUID_ID -> BuiltInRegistries.FLUID.getId(outputTank.getFluid().getFluid());
                case DATA_OUTPUT_PER_MINUTE -> currentOutputPerMinuteTenths(recipe, stats);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private final BulkSpeedState bulkSpeed = new BulkSpeedState();
    private final HeatLane heat = new HeatLane();
    /** Melt speed locked when the cycle started, so Overpressure does not flicker mid-melt. */
    private double activeMeltSpeed;
    private int lockedBatch;
    /** Whether the crucible is cooling down to its work ceiling, and whether the heater went short of FE, as of the last tick. */
    private boolean tooHot;
    private boolean starved;
    /** How far past the recipe's melting point the crucible runs, in percent of the band up to Max Temperature. */
    private int heatSlider;
    private int internalEnergy;
    private ItemStack activePrimary = ItemStack.EMPTY;
    private ItemStack activeSecondary = ItemStack.EMPTY;
    private FluidStack activeFluid = FluidStack.EMPTY;

    public MelterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MELTER.get(), pos, blockState, MachineType.MELTER, SLOT_PRIMARY_INPUT, SLOT_FLUID_INPUT_CONTAINER, SLOT_FLUID_OUTPUT_CONTAINER);
        trackStatSlots(gearInventory);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MelterBlockEntity melter) {
        melter.drainInputContainer();
        melter.fillOutputContainer();

        MachineStatAccumulator stats = melter.effectiveStats();
        melter.updateTankCapacity(stats);
        MelterRecipe recipe = melter.nextRecipe();
        if (recipe == null || !melter.hasRequiredComponents()) {
            melter.escapement.idle();
            melter.resetCycleIfActive();
            melter.idle(stats);
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }
        if (!melter.activeCycleMatches()) {
            melter.resetCycle();
            melter.resetBulkSpeed();
        }
        melter.escapement.observe(recipe);
        // A full output tank keeps the crucible at its setpoint, so melting resumes as soon as there is room.
        if (melter.effectiveHeat(stats) >= recipe.minimumTemperature()
                && melter.processingLevel(stats) >= recipe.requiredProcessingLevel()
                && !melter.canAcceptOutputFluid(melter.yieldedOutput(recipe, stats))) {
            melter.tooHot = false;
            melter.starved = false;
            HeatEngine.hold(melter.heat, melter.envelope(recipe, stats), stats, HeatEngine.electric(melter.heat, stats, melter::drawEnergy));
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }
        if (melter.effectiveHeat(stats) < recipe.minimumTemperature()
                || melter.processingLevel(stats) < recipe.requiredProcessingLevel()
                || melter.waitsForFullBatch(recipe, stats)) {
            melter.idle(stats);
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        HeatEngine.Envelope envelope = melter.envelope(recipe, stats);
        HeatEngine.HeatSupply supply = HeatEngine.electric(melter.heat, stats, melter::drawEnergy);
        HeatLane heat = melter.heat;
        if (!heat.started()
                && heat.exactTemperature() >= envelope.minimum()
                && heat.exactTemperature() <= envelope.ceilingFor(heat)
                && ProcessingChance.rollInstant(level, stats)
                && melter.payInstant(recipe, stats, supply)) {
            melter.markInstantProcess();
            melter.startCycleIfNeeded(recipe, stats);
            if (melter.process(recipe, stats)) {
                melter.bulkSpeed.recordProcess(melter.activeTraits());
            }
            BaseMachineBlock.setActive(level, pos, state, true);
            melter.setChanged();
            return;
        }

        boolean wasStarted = heat.started();
        HeatEngine.Tick tick = HeatEngine.work(heat, envelope, stats, melter.load(recipe, stats), supply, level.getGameTime(), 0);
        melter.tooHot = tick.tooHot();
        melter.starved = tick.starved();
        BaseMachineBlock.setActive(level, pos, state, tick.working() || heat.lastHeat() > 0.0);
        melter.setChanged();
        if (tick.tooHot() || !heat.started()) {
            return;
        }
        if (!wasStarted) {
            melter.startCycleIfNeeded(recipe, stats);
        }
        if (HeatEngine.finished(heat, melter.cycleWork(recipe, stats)) && melter.process(recipe, stats)) {
            melter.bulkSpeed.recordProcess(melter.activeTraits());
        }
    }

    /** FE/t the heater spent last tick: on wall loss when {@code holding}, otherwise on heating the chamber and input. */
    private int heatEnergy(int part) {
        return (int) Math.ceil(HeatEngine.heatSplit(heat)[part] * HeatEngine.energyRate(effectiveStats()));
    }

    private int heatFactorDisplay() {
        MelterRecipe recipe = nextRecipe();
        MachineStatAccumulator stats = effectiveStats();
        return recipe == null || !(hasRequiredComponents()) ? 0 : (int) Math.round(100.0 * HeatEngine.heatFactor(heat.exactTemperature(), envelope(recipe, stats)));
    }

    private int wobbleDisplay() {
        MelterRecipe recipe = nextRecipe();
        MachineStatAccumulator stats = effectiveStats();
        return recipe == null || !(hasRequiredComponents()) ? 0 : (int) Math.round(HeatEngine.wobbleAmplitude(envelope(recipe, stats), stats));
    }

    public int heatSlider() {
        return heatSlider;
    }

    /** Sets how far past each recipe's melting point the crucible runs, from 0 to 100 percent of the band up to Max Temperature. */
    public void setHeatSlider(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        if (clamped != heatSlider) {
            heatSlider = clamped;
            setChanged();
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
        return trackedEnergyStorage;
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
                        this::resetCycleAndBulkSpeed,
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

    /** Expected mB per minute for the current cycle, in tenths. */
    private int currentOutputPerMinuteTenths(MelterRecipe recipe, MachineStatAccumulator stats) {
        if (recipe == null) {
            return 0;
        }
        return OutputRate.perMinuteTenths(
                parallelMelts(recipe, stats) * (double) yieldedOutput(recipe, stats).getAmount(),
                currentProcessingTicks(recipe, stats)
        );
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        MelterRecipe recipe = nextRecipe();
        int status = statusCode(recipe, stats);
        int energyDemand = status == STATUS_READY || status == STATUS_WARMING || status == STATUS_NO_POWER ? currentEnergyCostPerTick(stats) : 0;
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("melter")
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY,
                                getBlockState().getValue(BaseMachineBlock.ACTIVE),
                                status == STATUS_NO_INPUT || status == STATUS_FILLING_BATCH
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status), recipe == null ? 0 : recipe.minimumTemperature())
                .progress(currentElapsedTicks(recipe, stats), currentProcessingTicks(recipe, stats))
                .fluidOutputRate(currentOutputPerMinuteTenths(recipe, stats))
                .energy(energyStored(), energyCapacity(), -energyDemand)
                .energyTelemetry(
                        energyFlow.lastInput(),
                        energyFlow.lastOutput(),
                        connector.transferRate(),
                        AdjacentEnergyConnector.inputBottleneck(connector, energyDemand)
                )
                .processingLevel(
                        processingLevel(stats),
                        recipe == null ? MachineInfoSnapshot.UNSET : recipe.requiredProcessingLevel()
                )
                .heat(heat.temperature(), recipe == null ? 0 : recipe.minimumTemperature())
                .setpoint(recipe == null || !hasRequiredComponents() ? 0 : envelope(recipe, stats).setpoint())
                .batch(recipe == null ? 0 : parallelMelts(recipe, stats))
                .gear(hasHeatCore()
                        ? MachineInfoSnapshot.GearSummary.HEAT_CORE_INSTALLED
                        : MachineInfoSnapshot.GearSummary.MISSING_HEAT_CORE)
                .output(status == STATUS_OUTPUT_TANK_FULL
                        ? MachineInfoSnapshot.OutputSummary.OUTPUT_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .refinement(machineTraits())
                .build();
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_MISSING_HEAT_CORE -> "missing_heat_core";
            case STATUS_MISSING_CRUSH_HEAD -> "missing_crush_head";
            case STATUS_NO_INPUT -> "no_input";
            case STATUS_INVALID_RECIPE -> "invalid_recipe";
            case STATUS_HEAT_LOW -> "heat_low";
            case STATUS_LEVEL_LOW -> "level_low";
            case STATUS_OUTPUT_TANK_FULL -> "output_full";
            case STATUS_NO_POWER -> "no_power";
            case STATUS_NO_FLUID -> "no_fluid";
            case STATUS_WARMING -> "warming";
            case STATUS_HEAT_HIGH -> "heat_high";
            case STATUS_FILLING_BATCH -> "filling_batch";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.melter.status." + name;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == Direction.UP) {
            return ingredientInputHandler;
        }
        if (side == Direction.DOWN) {
            return containerOutputHandler;
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
                Component.translatable("container.rngtech.melter")
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MelterMenu(containerId, playerInventory, this, menuData);
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

    public MachineStatAccumulator effectiveStats() {
        return cachedStats(bulkSpeed.count());
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.melter();
        stats.apply(effectiveTraits());
        MegaPassiveTree.applyNodeStats(stats, machineProgression(), MachineMasteryFamily.MELTER);
        applyHeatCoreStats(stats);
        applyCrushHeadStats(stats);
        applyServoStats(stats);
        applyFluidPumpStats(stats);
        MegaPassiveTree.applyAttributeStats(stats, machineProgression(), MachineMasteryFamily.MELTER);
        if (!hasBatteryCell()) {
            stats.apply(MachineStatAccumulator.NO_BATTERY_SOURCE, new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.PROCESSING_SPEED,
                    ModifierOperation.LESS,
                    NO_BATTERY_PROCESSING_SPEED
            ));
        }
        bulkSpeed.apply(stats, activeTraits());
        return stats;
    }

    private MachineTraits activeTraits() {
        return effectiveTraits();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        CompoundTag heatTag = new CompoundTag();
        heat.save(heatTag);
        tag.put("Heat", heatTag);
        tag.putInt("HeatTrack", heatSlider);
        tag.putDouble("ActiveMeltSpeed", activeMeltSpeed);
        tag.putInt("LockedBatch", lockedBatch);
        tag.putInt("Energy", internalEnergyStored());
        tag.put("ActivePrimary", activePrimary.saveOptional(registries));
        tag.put("ActiveSecondary", activeSecondary.saveOptional(registries));
        tag.put("ActiveFluid", activeFluid.saveOptional(registries));
        bulkSpeed.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        heat.resetCycle();
        if (tag.contains("Heat")) {
            heat.load(tag.getCompound("Heat"));
        }
        heatSlider = Math.max(0, Math.min(100, tag.getInt("HeatTrack")));
        activeMeltSpeed = Math.max(0.0, tag.getDouble("ActiveMeltSpeed"));
        lockedBatch = Math.max(0, tag.getInt("LockedBatch"));
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        activePrimary = ItemStack.parseOptional(registries, tag.getCompound("ActivePrimary"));
        activeSecondary = ItemStack.parseOptional(registries, tag.getCompound("ActiveSecondary"));
        activeFluid = FluidStack.parseOptional(registries, tag.getCompound("ActiveFluid"));
        bulkSpeed.load(tag);
        clampInternalEnergy();
    }

    private final EscapementState escapement = new EscapementState();

    public static boolean isHeatCore(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.HEAT_CORE, 6);
    }

    public static boolean isCrushHead(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.CRUSH_HEAD);
    }

    public static boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public static boolean isServo(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.SERVO);
    }

    public static boolean isFluidPump(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.FLUID_PUMP);
    }

    public static boolean isFluidInputContainer(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).isPresent();
    }

    public static boolean isFluidOutputContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack copy = stack.copyWithCount(1);
        FluidStack lubricant = new FluidStack(ModFluids.LUBRICANT_SOURCE.get(), FluidType.BUCKET_VOLUME);
        FluidStack lava = new FluidStack(Fluids.LAVA, FluidType.BUCKET_VOLUME);
        return FluidUtil.getFluidHandler(copy)
                .map(handler -> handler.fill(lubricant, IFluidHandler.FluidAction.SIMULATE) > 0
                        || handler.fill(lava, IFluidHandler.FluidAction.SIMULATE) > 0)
                .orElse(false);
    }

    private static boolean isValidOutputFluid(FluidStack stack) {
        return stack.is(ModTags.Fluids.MELTER_OUTPUTS);
    }

    public static boolean isGearComponent(ItemStack stack) {
        return isHeatCore(stack) || isCrushHead(stack) || isServo(stack) || isFluidPump(stack);
    }

    private MelterRecipe nextRecipe() {
        if (level == null) {
            return null;
        }
        ItemStack primary = primaryStack();
        ItemStack secondary = secondaryStack();
        FluidStack fluid = inputTank.getFluid();
        return recipes.lookup(0, level).item(primary).item(secondary).fluid(fluid)
                .find(() -> MelterRecipes.find(level, primary, secondary, fluid));
    }

    /** How many recipe lookups searched the recipes instead of reusing a remembered result. */
    public int recipeSearches() {
        return recipes.searches();
    }

    private boolean process(MelterRecipe recipe, MachineStatAccumulator stats) {
        int jobs = parallelMelts(recipe, stats);
        boolean melted = false;
        for (int job = 0; job < jobs; job++) {
            int[] consumed = level == null ? null : recipe.consumed(currentInput(), 1);
            if (consumed == null || !recipe.fluidInput().test(inputTank.getFluid())) {
                break;
            }
            FluidStack output = yieldedOutput(recipe, stats);
            if (!canAcceptOutputFluid(output)) {
                break;
            }
            consumeInputs(consumed);
            FluidStack drained = inputTank.drain(inputTank.getFluid().copyWithAmount(recipe.fluidInput().amount()), IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() < recipe.fluidInput().amount()) {
                break;
            }
            if (outputTank.fill(output, IFluidHandler.FluidAction.EXECUTE) > 0) {
                grantRecipeXp(recipe);
            }
            melted = true;
        }
        if (melted) {
            escapement.completed();
        }
        resetCycle();
        if (!melted) {
            resetBulkSpeed();
        }
        setChanged();
        return melted;
    }

    /** One melt's output with Fluid Yield on eligible recipes; Methane Trap and Brine Loop add to their fluids. */
    private FluidStack yieldedOutput(MelterRecipe recipe, MachineStatAccumulator stats) {
        FluidStack output = recipe.outputFluid();
        if (!recipe.allowsBonusOutput()) {
            return output;
        }
        String fluid = BuiltInRegistries.FLUID.getKey(output.getFluid()).toString();
        double yield = stats.value(MachineStat.FLUID_YIELD)
                + (hasMasteryBehavior("METHANE_TRAP") && fluid.equals("rngtech:methane") ? 25.0 : 0.0)
                + (hasMasteryBehavior("BRINE_LOOP") && fluid.equals("rngtech:electrolyte_solution") ? 25.0 : 0.0);
        return output.copyWithAmount(AscendancyFormulas.yieldedFluid(output.getAmount(), yield));
    }

    /** The batch locked when the cycle started, shrunk only if its inputs have since gone. */
    private int parallelMelts(MelterRecipe recipe, MachineStatAccumulator stats) {
        int available = availableMelts(recipe, stats);
        return heat.started() && lockedBatch > 0 ? Math.min(lockedBatch, available) : available;
    }

    /** Dense Batching: an idle Melter waits until its inputs, fluid, and tank room fit a full batch. */
    private boolean waitsForFullBatch(MelterRecipe recipe, MachineStatAccumulator stats) {
        return !heat.started() && hasMasteryBehavior("DENSE_BATCHING") && availableMelts(recipe, stats) < fullBatch(recipe, stats);
    }

    /** The two input slots and the input tank must hold every ingredient of the full batch at once. */
    private int fullBatch(MelterRecipe recipe, MachineStatAccumulator stats) {
        int units = recipe.itemIngredients().stream().mapToInt(CountedIngredient::count).sum();
        int slotLimit = units <= 0 ? BatchProcessing.MAX_BATCH_SIZE : 2 * 64 / units;
        slotLimit = Math.min(slotLimit, inputTank.getCapacity() / Math.max(1, recipe.fluidInput().amount()));
        return BatchProcessing.fullBatch(stats, hasMasteryBehavior("FUSED_CRUCIBLES"), slotLimit);
    }

    /** Each batched melt needs its own inputs, fluid, and tank room. Fused Crucibles gives up batching. */
    private int availableMelts(MelterRecipe recipe, MachineStatAccumulator stats) {
        int limit = BatchProcessing.batchSize(stats, hasMasteryBehavior("FUSED_CRUCIBLES"));
        limit = ShapelessMatch.crafts(recipe.itemIngredients(), new ItemStack[] {primaryStack(), secondaryStack()}, limit);
        limit = Math.min(limit, inputTank.getFluidAmount() / Math.max(1, recipe.fluidInput().amount()));
        FluidStack output = yieldedOutput(recipe, stats);
        return Math.max(1, BatchProcessing.largestFitting(limit, batch -> batch == 1 || canAcceptOutputFluid(output.copyWithAmount(output.getAmount() * batch))));
    }

    /** Batched melts each need their own heat; Shared Heat makes every melt after the first need 20% less. */
    private double batchHeat(MelterRecipe recipe, MachineStatAccumulator stats) {
        int extra = parallelMelts(recipe, stats) - 1;
        return recipe.heat() * (1.0 + extra * (hasMasteryBehavior("SHARED_HEAT") ? 0.8 : 1.0));
    }

    /** Overpressure, Lava Tap, Crush Feed, and Fused Crucibles speed up a melt; set when its cycle starts. */
    private double meltSpeed(MelterRecipe recipe, MachineStatAccumulator stats) {
        double speed = 1.0;
        if (hasMasteryBehavior("OVERPRESSURE") && outputTank.getFluidAmount() * 2 > outputTank.getCapacity()) {
            speed *= 1.3;
        }
        if (hasMasteryBehavior("LAVA_TAP") && recipe.fluidOutput().getFluid().isSame(Fluids.LAVA)) {
            speed *= 1.5;
        }
        int overlevel = Math.max(0, processingLevel(stats) - recipe.requiredProcessingLevel());
        speed *= 1.0 + Math.max(0.0, stats.value(MachineStat.OVERLEVEL_SPEED)) / 100.0 * overlevel;
        if (hasMasteryBehavior("FUSED_CRUCIBLES")) {
            speed *= 1.0 + AscendancyFormulas.FUSED_CRUCIBLES_SPEED_PER_BATCH * BatchProcessing.statBatchSize(stats);
        }
        return speed;
    }

    /** Fluid Capacity sizes both tanks; fluid above a smaller capacity stays until it drains. */
    private void updateTankCapacity(MachineStatAccumulator stats) {
        int capacity = Math.max(TANK_CAPACITY, (int) Math.round(stats.value(MachineStat.FLUID_CAPACITY)));
        if (inputTank.getCapacity() != capacity) {
            inputTank.setCapacity(capacity);
            outputTank.setCapacity(capacity);
        }
    }

    private void grantRecipeXp(MelterRecipe recipe) {
        if (recipe.machineXp() <= 0) {
            return;
        }
        int xpQuarters = MachineProgressionState.xpQuarters(machineProgression().level(), recipe.machineXpBand());
        if (xpQuarters <= 0) {
            return;
        }
        grantMasteryXp(MachineProgressionState.workXp(recipe.machineXp(), recipe.machineXpBand()), xpQuarters);
    }

    public boolean unlockPassiveNode(MegaPassiveNode node) {
        return allocateMastery(node);
    }

    @Override
    public boolean mutesMachineSound() {
        return MegaPassiveTree.has(machineProgression(), "MUTE_MACHINE_SOUND");
    }

    @Override
    public MachineMasteryFamily masteryFamily() {
        return MachineMasteryFamily.MELTER;
    }

    /** The Melter has a single Titanium-stage body. */
    @Override
    public int ascendancyEntryStage() {
        return 6;
    }

    @Override
    public MachineProgressionState machineProgression() {
        return super.machineProgression().forFamily(masteryFamily());
    }

    @Override
    public void masteryChanged() {
        resetCycle();
        resetBulkSpeed();
        clampInternalEnergy();
        setChanged();
    }

    /** Remainders are placed once both slots are consumed, so the slot planner sees what the melt left behind. */
    private void consumeInputs(int[] consumed) {
        ItemStack[] remainders = {ItemStack.EMPTY, ItemStack.EMPTY};
        for (int slot = 0; slot < remainders.length; slot++) {
            for (int unit = 0; unit < consumed[slot]; unit++) {
                ItemStack input = processInventory.getStackInSlot(SLOT_PRIMARY_INPUT + slot);
                if (input.isEmpty()) {
                    break;
                }
                ItemStack remainder = input.getCraftingRemainingItem();
                input.shrink(1);
                if (remainders[slot].isEmpty()) {
                    remainders[slot] = remainder;
                } else {
                    releaseRemainder(remainder);
                }
            }
        }
        for (int slot = 0; slot < remainders.length; slot++) {
            placeRemainder(SLOT_PRIMARY_INPUT + slot, remainders[slot]);
        }
    }

    /**
     * A remainder refills its emptied slot only where automation would insert it itself; any other would jam automated
     * input, so it leaves.
     */
    private void placeRemainder(int slot, ItemStack remainder) {
        if (remainder.isEmpty()) {
            return;
        }
        if (processInventory.getStackInSlot(slot).isEmpty() && acceptsAutomatedInput(slot, remainder)) {
            processInventory.setStackInSlot(slot, remainder);
        } else {
            releaseRemainder(remainder);
        }
    }

    /** A remainder that can hold Melter output waits in the output container slot; anything else leaves the machine. */
    private void releaseRemainder(ItemStack remainder) {
        ejectBelowOrDrop(processInventory.insertItem(SLOT_FLUID_OUTPUT_CONTAINER, remainder, false));
    }

    /** Sealed Lines never vents excess output, so a full tank pauses the melt instead. */
    private boolean canAcceptOutputFluid(FluidStack result) {
        return !result.isEmpty()
                && FluidOutputOverflow.canAcceptOrVoidExcess(outputTank, result, autoPurgesFluidOutput() && !hasMasteryBehavior("SEALED_LINES"));
    }

    /** Melter recipes only name a melting point, which is both the minimum and the target; nothing can fail. */
    private HeatEngine.Envelope envelope(MelterRecipe recipe, MachineStatAccumulator stats) {
        int target = recipe.minimumTemperature();
        return HeatEngine.envelope(target, target, HeatControl.defaultSafeMaximumTemperature(target), 0.0, false,
                hasMasteryBehavior("BLAST_DRAFT"), stats, heatSlider / 100.0, Integer.MAX_VALUE).coolingFirst(hasMasteryBehavior("COOL_HEADED"));
    }

    /** Reference ticks a melt needs: the recipe's time, longer for every extra melt in the batch. */
    private double cycleWork(MelterRecipe recipe, MachineStatAccumulator stats) {
        return recipe.processingTicks() * BatchProcessing.timeMultiplier(stats, parallelMelts(recipe, stats));
    }

    private HeatEngine.Load load(MelterRecipe recipe, MachineStatAccumulator stats) {
        double meltSpeed = heat.started() && activeMeltSpeed > 0.0 ? activeMeltSpeed : meltSpeed(recipe, stats);
        double workPerTick = HeatEngine.speed(stats) * meltSpeed * escapementSpeed(stats);
        return new HeatEngine.Load(batchHeat(recipe, stats) * workPerTick / cycleWork(recipe, stats), workPerTick, parallelMelts(recipe, stats));
    }

    private int drawEnergy(int amount) {
        int take = Math.min(amount, consumeWorkingEnergy(amount, true));
        return take > 0 ? consumeWorkingEnergy(take, false) : 0;
    }

    /** Instant Process pays the whole batch's heat at once, without the wall loss of a full melt. */
    private boolean payInstant(MelterRecipe recipe, MachineStatAccumulator stats, HeatEngine.HeatSupply supply) {
        double needed = batchHeat(recipe, stats);
        int energyCost = (int) Math.ceil(needed * HeatEngine.energyRate(stats));
        return consumeWorkingEnergy(energyCost, true) >= energyCost && HeatEngine.payAtOnce(supply, needed);
    }

    private void idle(MachineStatAccumulator stats) {
        tooHot = false;
        starved = false;
        HeatEngine.idle(heat, stats, false);
    }

    private double escapementSpeed(MachineStatAccumulator stats) {
        return escapement.speed(MachineImplicitCatalog.hasBehavior(servoStack(), MachineBehavior.ESCAPEMENT), stats);
    }


    /** FE the heater drew last tick. */
    private int currentEnergyCostPerTick(MachineStatAccumulator stats) {
        return (int) Math.ceil(heat.lastHeat() * HeatEngine.energyRate(stats));
    }

    private int currentProcessingTicks(MelterRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null || !hasRequiredComponents()
                ? 0
                : HeatEngine.cycleTicks(cycleWork(recipe, stats), load(recipe, stats), envelope(recipe, stats));
    }

    private int currentElapsedTicks(MelterRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null || !hasRequiredComponents()
                ? 0
                : HeatEngine.elapsedTicks(heat, load(recipe, stats), envelope(recipe, stats));
    }

    private int statusCode(MelterRecipe recipe, MachineStatAccumulator stats) {
        if (!hasHeatCore()) {
            return STATUS_MISSING_HEAT_CORE;
        }
        if (!hasCrushHead()) {
            return STATUS_MISSING_CRUSH_HEAD;
        }
        if (recipe == null && (primaryStack().isEmpty() || secondaryStack().isEmpty())) {
            return STATUS_NO_INPUT;
        }
        if (inputTank.isEmpty()) {
            return STATUS_NO_FLUID;
        }
        if (recipe == null) {
            return STATUS_INVALID_RECIPE;
        }
        if (effectiveHeat(stats) < recipe.minimumTemperature()) {
            return STATUS_HEAT_LOW;
        }
        if (processingLevel(stats) < recipe.requiredProcessingLevel()) {
            return STATUS_LEVEL_LOW;
        }
        if (!canAcceptOutputFluid(yieldedOutput(recipe, stats))) {
            return STATUS_OUTPUT_TANK_FULL;
        }
        if (waitsForFullBatch(recipe, stats)) {
            int loaded = ShapelessMatch.crafts(recipe.itemIngredients(), new ItemStack[] {primaryStack(), secondaryStack()}, BatchProcessing.MAX_BATCH_SIZE);
            loaded = Math.min(loaded, inputTank.getFluidAmount() / Math.max(1, recipe.fluidInput().amount()));
            return loaded < fullBatch(recipe, stats) ? STATUS_FILLING_BATCH : STATUS_OUTPUT_TANK_FULL;
        }
        if (tooHot) {
            return STATUS_HEAT_HIGH;
        }
        if (starved) {
            return STATUS_NO_POWER;
        }
        return heat.started() ? STATUS_READY : STATUS_WARMING;
    }

    private void startCycleIfNeeded(MelterRecipe recipe, MachineStatAccumulator stats) {
        if (lockedBatch != 0) {
            return;
        }
        lockedBatch = availableMelts(recipe, stats);
        activePrimary = singleCopy(primaryStack());
        activeSecondary = singleCopy(secondaryStack());
        activeFluid = inputTank.getFluid().copyWithAmount(recipe.fluidInput().amount());
        activeMeltSpeed = meltSpeed(recipe, stats);
        if (bulkSpeed.startRecipe(recipe)) {
            setChanged();
        }
    }

    private boolean activeCycleMatches() {
        if (!heat.started()) {
            return true;
        }
        return (!activePrimary.isEmpty() || !activeSecondary.isEmpty())
                && !activeFluid.isEmpty()
                && sameItemKinds(activePrimary, activeSecondary, primaryStack(), secondaryStack())
                && FluidStack.isSameFluidSameComponents(activeFluid, inputTank.getFluid())
                && inputTank.getFluidAmount() >= activeFluid.getAmount();
    }

    /** Slot order does not matter, so a top-up that lands in the other slot keeps the melt. */
    private static boolean sameItemKinds(ItemStack activeA, ItemStack activeB, ItemStack currentA, ItemStack currentB) {
        return isOneOf(activeA, currentA, currentB) && isOneOf(activeB, currentA, currentB)
                && isOneOf(currentA, activeA, activeB) && isOneOf(currentB, activeA, activeB);
    }

    private static boolean isOneOf(ItemStack stack, ItemStack first, ItemStack second) {
        return stack.isEmpty()
                || ItemStack.isSameItemSameComponents(stack, first)
                || ItemStack.isSameItemSameComponents(stack, second);
    }

    private void resetCycleIfActive() {
        if (hasActiveCycle()) {
            resetCycle();
            resetBulkSpeed();
            setChanged();
        }
    }

    private void resetCycle() {
        heat.resetCycle();
        activeMeltSpeed = 0.0;
        lockedBatch = 0;
        activePrimary = ItemStack.EMPTY;
        activeSecondary = ItemStack.EMPTY;
        activeFluid = FluidStack.EMPTY;
    }

    private boolean hasActiveCycle() {
        return heat.started() || heat.work() > 0.0 || !activePrimary.isEmpty() || !activeSecondary.isEmpty() || !activeFluid.isEmpty();
    }

    private void resetBulkSpeed() {
        if (bulkSpeed.reset()) {
            setChanged();
        }
    }

    private void resetCycleAndBulkSpeed() {
        resetCycle();
        resetBulkSpeed();
    }

    private int effectiveHeat(MachineStatAccumulator stats) {
        return hasHeatCore() ? Math.max(0, stats.intValue(MachineStat.MAX_TEMPERATURE)) : 0;
    }

    private int processingLevel(MachineStatAccumulator stats) {
        return hasCrushHead() ? Math.max(0, stats.intValue(MachineStat.PROCESSING_LEVEL)) : 0;
    }

    private int effectiveFluidTransfer(MachineStatAccumulator stats) {
        return hasFluidPump() ? Math.max(1, stats.intValue(MachineStat.FLUID_TRANSFER)) : 0;
    }

    private void applyHeatCoreStats(MachineStatAccumulator stats) {
        ItemStack stack = heatCoreStack();
        if (!isHeatCore(stack)) {
            return;
        }
        ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        ComponentBaseStatCatalog.applyAttributeAffixes(stats, stack);
    }

    private void applyCrushHeadStats(MachineStatAccumulator stats) {
        ItemStack stack = crushHeadStack();
        if (isCrushHead(stack) && stack.getItem() instanceof MachinePartItem part) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
            ComponentBaseStatCatalog.applyAttributeAffixes(stats, stack);
        }
    }

    private void applyServoStats(MachineStatAccumulator stats) {
        ItemStack stack = servoStack();
        if (isServo(stack) && stack.getItem() instanceof MachinePartItem part) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
            ComponentBaseStatCatalog.applyAttributeAffixes(stats, stack);
        }
    }

    private void applyFluidPumpStats(MachineStatAccumulator stats) {
        ItemStack stack = fluidPumpStack();
        if (isFluidPump(stack) && stack.getItem() instanceof MachinePartItem part) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
            ComponentBaseStatCatalog.applyAttributeAffixes(stats, stack);
        }
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
    }

    private int energyStored() {
        return internalEnergyStored() + batteryCell.energyStored();
    }

    private int energyCapacity() {
        return internalEnergyCapacity() + batteryCell.capacity();
    }

    private int internalEnergyCapacity() {
        return Math.max(1, (int) Math.round(effectiveStats().value(MachineStat.ENERGY_CAPACITY)));
    }

    private int internalEnergyStored() {
        return Math.max(0, Math.min(internalEnergy, internalEnergyCapacity()));
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

    private int consumeWorkingEnergy(int toConsume, boolean simulate) {
        if (toConsume <= 0) {
            return 0;
        }

        int internal = Math.min(toConsume, internalEnergyStored());
        int remaining = toConsume - internal;
        IEnergyStorage cell = batteryCellEnergyStorage();
        int cellExtracted = 0;
        if (remaining > 0 && cell != null && cell.canExtract()) {
            cellExtracted = cell.extractEnergy(remaining, true);
        }

        int consumed = internal + cellExtracted;
        if (simulate || consumed < toConsume) {
            return consumed;
        }

        if (internal > 0) {
            internalEnergy = internalEnergyStored() - internal;
        }
        if (cellExtracted > 0) {
            cell.extractEnergy(cellExtracted, false);
        }
        if (consumed > 0) {
            setChanged();
        }
        return consumed;
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

    private void fillOutputContainer() {
        if (!hasSideFluidOutput()) {
            return;
        }
        ItemStack stack = processInventory.getStackInSlot(SLOT_FLUID_OUTPUT_CONTAINER);
        if (stack.isEmpty() || outputTank.isEmpty()) {
            return;
        }
        int maxFill = Math.max(FluidType.BUCKET_VOLUME, effectiveFluidTransfer(effectiveStats()));
        FluidActionResult result = FluidUtil.tryFillContainer(stack, outputTank, maxFill, null, true);
        if (result.isSuccess()) {
            processInventory.setStackInSlot(SLOT_FLUID_OUTPUT_CONTAINER, result.getResult());
            setChanged();
        }
    }

    private boolean hasRequiredComponents() {
        return hasHeatCore() && hasCrushHead();
    }

    private boolean hasHeatCore() {
        return isHeatCore(heatCoreStack());
    }

    private boolean hasCrushHead() {
        return isCrushHead(crushHeadStack());
    }

    private boolean hasBatteryCell() {
        return isBatteryCell(batteryCellStack());
    }

    private boolean hasFluidPump() {
        return isFluidPump(fluidPumpStack());
    }

    private boolean hasSideFluidOutput() {
        return hasFluidPump() && MachineImplicitCatalog.hasBehavior(fluidPumpStack(), MachineBehavior.SIDE_FLUID_OUTPUT);
    }

    private boolean autoPurgesFluidOutput() {
        return MachineImplicitCatalog.hasBehavior(servoStack(), MachineBehavior.AUTO_PURGE);
    }

    private MelterRecipeInput currentInput() {
        return new MelterRecipeInput(primaryStack(), secondaryStack(), inputTank.getFluid());
    }

    private ItemStack primaryStack() {
        return processInventory.getStackInSlot(SLOT_PRIMARY_INPUT);
    }

    private ItemStack secondaryStack() {
        return processInventory.getStackInSlot(SLOT_SECONDARY_INPUT);
    }

    private ItemStack heatCoreStack() {
        return gearInventory.getStackInSlot(SLOT_HEAT_CORE);
    }

    private ItemStack crushHeadStack() {
        return gearInventory.getStackInSlot(SLOT_CRUSH_HEAD);
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private ItemStack servoStack() {
        return gearInventory.getStackInSlot(SLOT_SERVO);
    }

    private ItemStack fluidPumpStack() {
        return gearInventory.getStackInSlot(SLOT_FLUID_PUMP);
    }

    private IEnergyStorage batteryCellEnergyStorage() {
        return batteryCell.storage();
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
    }

    private static ItemStack singleCopy(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    public boolean isMeltingInput(ItemStack stack) {
        return INPUT_FILTER.accepts(level, stack);
    }

    /** Automation keeps one ingredient from taking both input slots while a recipe still needs another item. */
    public boolean acceptsAutomatedInput(int slot, ItemStack stack) {
        return isMeltingInput(stack)
                && SLOT_PLANNER.accepts(level, new ItemStack[] {primaryStack(), secondaryStack()}, slot - SLOT_PRIMARY_INPUT, stack);
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
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return processInventory.insertItem(mappedSlot(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!allowExtract) {
                return ItemStack.EMPTY;
            }
            return processInventory.extractItem(mappedSlot(slot), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(mappedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            int target = mappedSlot(slot);
            return allowInsert
                    && processInventory.isItemValid(target, stack)
                    && (target != SLOT_PRIMARY_INPUT && target != SLOT_SECONDARY_INPUT || acceptsAutomatedInput(target, stack));
        }

        private int mappedSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
            }
            return firstSlot + slot;
        }
    }

    private final class MelterFluidHandler implements IFluidHandler {
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
            if (!hasSideFluidOutput()) {
                return FluidStack.EMPTY;
            }
            if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, outputTank.getFluid())) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!hasSideFluidOutput()) {
                return FluidStack.EMPTY;
            }
            int capped = Math.min(maxDrain, effectiveFluidTransfer(effectiveStats()));
            return outputTank.drain(capped, action);
        }
    }

    private final class MelterEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (!canReceive() || toReceive <= 0) {
                return 0;
            }

            int remaining = toReceive;
            int received = receiveInternalEnergy(remaining, simulate);
            remaining -= received;

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
            if (internalEnergyStored() < internalEnergyCapacity()) {
                return true;
            }
            IEnergyStorage cell = batteryCellEnergyStorage();
            return cell != null && cell.canReceive() && cell.getEnergyStored() < cell.getMaxEnergyStored();
        }
    }
}
