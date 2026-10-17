package com.rngtech.content.blockentity;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.FurnaceBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.GearParts;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.loot.ChallengeContext;
import com.rngtech.content.loot.ChallengeLoot;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.menu.FurnaceMenu;
import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.content.recipe.FurnaceRecipe;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModTags;
import com.rngtech.rpg.BatchProcessing;
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
import com.rngtech.rpg.OutputLedger;
import com.rngtech.rpg.progression.AscendancyFormulas;
import com.rngtech.rpg.progression.FurnacePassiveTree;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FurnaceBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider, MachineMasteryHost {
    public static final int MAX_PROCESSING_SLOTS = 4;
    /** Every lane shares the furnace's one Gear slot, so one Heat Core drives a whole Lead Furnace. */
    public static final int MAX_GEAR_SLOTS = 1;
    public static final int SLOT_INPUT_START = 0;
    public static final int SLOT_INPUT = SLOT_INPUT_START;
    public static final int SLOT_FUEL = SLOT_INPUT_START + MAX_PROCESSING_SLOTS;
    public static final int SLOT_OUTPUT_START = SLOT_FUEL + 1;
    public static final int SLOT_OUTPUT = SLOT_OUTPUT_START;
    public static final int SLOT_COUNT = SLOT_OUTPUT_START + MAX_PROCESSING_SLOTS;

    public static final int STATUS_READY = 0;
    public static final int STATUS_NO_INPUT = 1;
    public static final int STATUS_INVALID_RECIPE = 2;
    public static final int STATUS_HEAT_LOW = 3;
    public static final int STATUS_STABILITY_LOW = 4;
    public static final int STATUS_OUTPUT_FULL = 5;
    public static final int STATUS_NO_POWER = 6;
    public static final int STATUS_NO_FUEL = 7;
    public static final int STATUS_WARMING = 8;
    public static final int STATUS_POWER_DROP = 9;
    public static final int STATUS_FAILURE_RISK = 10;
    public static final int STATUS_POWER_LIMITED = 11;
    public static final int STATUS_HEAT_HIGH = 12;
    public static final int STATUS_FILLING_BATCH = 13;

    /** Share of an electric recipe's FE that Closed Loop Recuperator returns to the internal buffer. */
    private static final double CLOSED_LOOP_REFUND = 0.25D;
    private static final int DATA_PROGRESS_START = 0;
    private static final int DATA_PROCESSING_TICKS_START = DATA_PROGRESS_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_ENERGY_PER_TICK_START = DATA_PROCESSING_TICKS_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_ENERGY_PER_CRAFT_START = DATA_ENERGY_PER_TICK_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_BURN_TIME = DATA_ENERGY_PER_CRAFT_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_TOTAL_BURN_TIME = DATA_BURN_TIME + 1;
    private static final int DATA_PROCESSING_SPEED = DATA_TOTAL_BURN_TIME + 1;
    private static final int DATA_EFFICIENCY = DATA_PROCESSING_SPEED + 1;
    private static final int DATA_HEAT_TRANSFER = DATA_EFFICIENCY + 1;
    private static final int DATA_MAX_TEMPERATURE = DATA_HEAT_TRANSFER + 1;
    private static final int DATA_TEMPERATURE_STABILITY = DATA_MAX_TEMPERATURE + 1;
    private static final int DATA_FUEL_EFFICIENCY = DATA_TEMPERATURE_STABILITY + 1;
    private static final int DATA_FUEL_DURATION = DATA_FUEL_EFFICIENCY + 1;
    private static final int DATA_REFINEMENT_POTENTIAL = DATA_FUEL_DURATION + 1;
    private static final int DATA_ENERGY = DATA_REFINEMENT_POTENTIAL + 1;
    private static final int DATA_ENERGY_CAPACITY = DATA_ENERGY + 1;
    private static final int DATA_ENERGY_USAGE = DATA_ENERGY_CAPACITY + 1;
    private static final int DATA_ENERGY_CAPACITY_STAT = DATA_ENERGY_USAGE + 1;
    private static final int DATA_ENERGY_TRANSFER = DATA_ENERGY_CAPACITY_STAT + 1;
    private static final int DATA_THERMAL_MASS = DATA_ENERGY_TRANSFER + 1;
    private static final int DATA_COOLING_RATE = DATA_THERMAL_MASS + 1;
    private static final int DATA_OVERHEAT_TOLERANCE = DATA_COOLING_RATE + 1;
    private static final int DATA_INPUT_SLOTS = DATA_OVERHEAT_TOLERANCE + 1;
    private static final int DATA_GEAR_SLOTS = DATA_INPUT_SLOTS + 1;
    private static final int DATA_MIN_TEMPERATURE = DATA_GEAR_SLOTS + 1;
    private static final int DATA_STATUS = DATA_MIN_TEMPERATURE + 1;
    private static final int DATA_HEAT_START = DATA_STATUS + 1;
    private static final int DATA_TARGET_TEMPERATURE_START = DATA_HEAT_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_SAFE_MAX_TEMPERATURE_START = DATA_TARGET_TEMPERATURE_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_OVERHEAT_TEMPERATURE_START = DATA_SAFE_MAX_TEMPERATURE_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_FAILURE_STRAIN_START = DATA_OVERHEAT_TEMPERATURE_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_FAILURE_ENABLED_START = DATA_FAILURE_STRAIN_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_POWER_SENSITIVE_START = DATA_FAILURE_ENABLED_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_LEDGER_START = DATA_POWER_SENSITIVE_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_BATCH_SIZE = DATA_LEDGER_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_HEAT_SLIDER = DATA_BATCH_SIZE + 1;
    private static final int DATA_SETPOINT_START = DATA_HEAT_SLIDER + 1;
    private static final int DATA_THERMAL_CONDUCTIVITY = DATA_SETPOINT_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_HOLDING_ENERGY = DATA_THERMAL_CONDUCTIVITY + 1;
    private static final int DATA_HEATING_ENERGY = DATA_HOLDING_ENERGY + 1;
    private static final int DATA_HEAT_FACTOR = DATA_HEATING_ENERGY + 1;
    private static final int DATA_WOBBLE = DATA_HEAT_FACTOR + 1;
    private static final int DATA_HEAT_REACH = DATA_WOBBLE + 1;
    private static final int DATA_HEAT_MAXIMUM = DATA_HEAT_REACH + 1;
    private static final int DATA_INPUT_ENERGY = DATA_HEAT_MAXIMUM + 1;
    private static final int DATA_NITROGEN = DATA_INPUT_ENERGY + 1;
    private static final int DATA_NITROGEN_USE = DATA_NITROGEN + 1;
    private static final int DATA_NITROGEN_BONUS = DATA_NITROGEN_USE + 1;
    private static final int DATA_MACHINE_PROGRESSION_START = DATA_NITROGEN_BONUS + 1;
    private static final int DATA_COUNT = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int STAT_SCALE = 100;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (isOutputSlot(slot)) {
                return false;
            }
            if (slot == SLOT_FUEL) {
                return isElectric() ? isBatteryCell(stack) : isFuel(stack);
            }
            int lane = inputLane(slot);
            return lane >= 0 && lane < activeProcessingSlots();
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_FUEL && isElectric() ? 1 : super.getSlotLimit(slot);
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
            if (slot == SLOT_FUEL) {
                clampInternalEnergy();
            }
            setChanged();
        }
    };

    private final ItemStackHandler gearInventory = new ItemStackHandler(MAX_GEAR_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 0 && slot < activeGearSlots() && isAllowedGearComponent(slot, stack);
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
            resetAllCycles();
            resetBulkSpeed();
            setChanged();
        }
    };

    private final IItemHandler topItemHandler = new FurnaceSidedItemHandler(SLOT_INPUT_START, true, false, true);
    private final IItemHandler sideItemHandler = new FurnaceSidedItemHandler(SLOT_FUEL, true, false, false);
    private final IItemHandler bottomItemHandler = new FurnaceSidedItemHandler(SLOT_OUTPUT_START, false, true, true);
    private final IEnergyStorage energy = new FurnaceEnergyStorage();

    private final InstalledBatteryCell batteryCell = new InstalledBatteryCell(this::batteryCellStack);
    private final InertAtmosphere atmosphere = new InertAtmosphere(this::setChanged, this::isElectric);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            if (index >= DATA_PROGRESS_START && index < DATA_PROGRESS_START + MAX_PROCESSING_SLOTS) {
                return syncedProgress(index - DATA_PROGRESS_START, currentElapsedTicks(index - DATA_PROGRESS_START));
            }
            if (index >= DATA_PROCESSING_TICKS_START && index < DATA_PROCESSING_TICKS_START + MAX_PROCESSING_SLOTS) {
                return currentProcessingTicks(index - DATA_PROCESSING_TICKS_START);
            }
            if (index >= DATA_ENERGY_PER_TICK_START && index < DATA_ENERGY_PER_TICK_START + MAX_PROCESSING_SLOTS) {
                return currentEnergyCostPerTick(index - DATA_ENERGY_PER_TICK_START);
            }
            if (index >= DATA_ENERGY_PER_CRAFT_START && index < DATA_ENERGY_PER_CRAFT_START + MAX_PROCESSING_SLOTS) {
                return currentEnergyCostPerCraft(index - DATA_ENERGY_PER_CRAFT_START);
            }
            if (index >= DATA_HEAT_START && index < DATA_HEAT_START + MAX_PROCESSING_SLOTS) {
                return heat[index - DATA_HEAT_START].temperature();
            }
            if (index >= DATA_TARGET_TEMPERATURE_START && index < DATA_TARGET_TEMPERATURE_START + MAX_PROCESSING_SLOTS) {
                return currentTargetTemperature(index - DATA_TARGET_TEMPERATURE_START);
            }
            if (index >= DATA_SAFE_MAX_TEMPERATURE_START && index < DATA_SAFE_MAX_TEMPERATURE_START + MAX_PROCESSING_SLOTS) {
                return currentSafeMaximumTemperature(index - DATA_SAFE_MAX_TEMPERATURE_START);
            }
            if (index >= DATA_OVERHEAT_TEMPERATURE_START && index < DATA_OVERHEAT_TEMPERATURE_START + MAX_PROCESSING_SLOTS) {
                return currentOverheatTemperature(index - DATA_OVERHEAT_TEMPERATURE_START);
            }
            if (index >= DATA_FAILURE_STRAIN_START && index < DATA_FAILURE_STRAIN_START + MAX_PROCESSING_SLOTS) {
                return HeatControl.failureProgress(failureStrain[index - DATA_FAILURE_STRAIN_START]);
            }
            if (index >= DATA_FAILURE_ENABLED_START && index < DATA_FAILURE_ENABLED_START + MAX_PROCESSING_SLOTS) {
                int lane = index - DATA_FAILURE_ENABLED_START;
                return overheatFailureEnabled(lane, statsForLane(lane)) ? 1 : 0;
            }
            if (index >= DATA_POWER_SENSITIVE_START && index < DATA_POWER_SENSITIVE_START + MAX_PROCESSING_SLOTS) {
                int lane = index - DATA_POWER_SENSITIVE_START;
                return powerSensitiveActive(lane, statsForLane(lane)) ? 1 : 0;
            }
            if (index >= DATA_LEDGER_START && index < DATA_LEDGER_START + MAX_PROCESSING_SLOTS) {
                return ledgerDisplay(index - DATA_LEDGER_START);
            }
            if (index >= DATA_SETPOINT_START && index < DATA_SETPOINT_START + MAX_PROCESSING_SLOTS) {
                return currentSetpoint(index - DATA_SETPOINT_START);
            }
            if (index >= DATA_MACHINE_PROGRESSION_START
                    && index < DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT) {
                return MasteryMenuSupport.get(FurnaceBlockEntity.this, index - DATA_MACHINE_PROGRESSION_START, FurnaceBlockEntity.this::effectiveStats);
            }
            return switch (index) {
                case DATA_BURN_TIME -> burnTime;
                case DATA_TOTAL_BURN_TIME -> totalBurnTime;
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_HEAT_TRANSFER -> scaledStat(stats, MachineStat.HEAT_TRANSFER);
                case DATA_MAX_TEMPERATURE -> scaledStat(stats, MachineStat.MAX_TEMPERATURE);
                case DATA_TEMPERATURE_STABILITY -> scaledStat(stats, MachineStat.TEMPERATURE_STABILITY);
                case DATA_FUEL_EFFICIENCY -> scaledStat(stats, MachineStat.FUEL_EFFICIENCY);
                case DATA_FUEL_DURATION -> scaledStat(stats, MachineStat.FUEL_DURATION);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_THERMAL_MASS -> scaledStat(stats, MachineStat.THERMAL_MASS);
                case DATA_COOLING_RATE -> scaledStat(stats, MachineStat.COOLING_RATE);
                case DATA_OVERHEAT_TOLERANCE -> scaledStat(stats, MachineStat.OVERHEAT_TOLERANCE);
                case DATA_INPUT_SLOTS -> scaledStat(stats, MachineStat.INPUT_SLOTS);
                case DATA_BATCH_SIZE -> BatchProcessing.statBatchSize(stats) * STAT_SCALE;
                case DATA_HEAT_SLIDER -> heatSlider;
                case DATA_THERMAL_CONDUCTIVITY -> scaledStat(stats, MachineStat.THERMAL_CONDUCTIVITY);
                case DATA_HOLDING_ENERGY -> heatEnergy(0);
                case DATA_INPUT_ENERGY -> heatEnergy(1);
                case DATA_HEATING_ENERGY -> heatEnergy(2);
                case DATA_HEAT_FACTOR -> heatFactorDisplay();
                case DATA_WOBBLE -> wobbleDisplay();
                case DATA_HEAT_REACH -> reachDisplay();
                case DATA_HEAT_MAXIMUM -> baseEffectiveStats().intValue(MachineStat.MAX_TEMPERATURE);
                case DATA_GEAR_SLOTS -> activeGearSlots();
                case DATA_MIN_TEMPERATURE -> currentMinimumTemperature(stats);
                case DATA_STATUS -> statusCode(stats);
                case DATA_NITROGEN -> atmosphere.amount();
                case DATA_NITROGEN_USE -> atmosphere.lastUse();
                case DATA_NITROGEN_BONUS -> (int) Math.round(atmosphereBonus() * 100.0);
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
    private static final RecipeCache.Search<FurnaceRecipe> RECIPE_SEARCH = (level, input, unused) -> FurnaceRecipes.find(level, input);
    /** One lookup slot per lane, plus one for stacks offered by players or automation. */
    private final RecipeCache<FurnaceRecipe> recipes = new RecipeCache<>(MAX_PROCESSING_SLOTS + 1);
    private final MachineStatsCache[] laneStatsCaches = new MachineStatsCache[MAX_PROCESSING_SLOTS];
    private final List<Supplier<MachineStatAccumulator>> laneStatsBuilders = new ArrayList<>(MAX_PROCESSING_SLOTS);
    private final HeatLane[] heat = new HeatLane[MAX_PROCESSING_SLOTS];
    private final int[] lockedBatch = new int[MAX_PROCESSING_SLOTS];
    private final int[] lastTargetTemperature = new int[MAX_PROCESSING_SLOTS];
    private final int[] lastSafeMaximumTemperature = new int[MAX_PROCESSING_SLOTS];
    private final int[] failureStrain = new int[MAX_PROCESSING_SLOTS];
    /** Lanes cooling down to their work ceiling, and lanes the heater could not keep fed, as of the last tick. */
    private final boolean[] tooHotLane = new boolean[MAX_PROCESSING_SLOTS];
    private final boolean[] starved = new boolean[MAX_PROCESSING_SLOTS];
    /** How far past the recipe target the lanes run, in percent of the band up to Max Temperature. */
    private int heatSlider;
    private final ItemStack[] activeInputs = emptyInputs();
    private final OutputLedger<Item> bloomLedger = new OutputLedger<>();
    private int burnTime;
    private int totalBurnTime;
    private int internalEnergy;
    private long energyTelemetryTick = Long.MIN_VALUE;
    private int energyInputThisTick;
    private int lastEnergyInput;

    public FurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.FURNACE.get(), pos, blockState, MachineType.FURNACE, SLOT_INPUT, SLOT_FUEL, SLOT_OUTPUT);
        trackStatSlots(gearInventory);
        trackStatSlots(inventory, SLOT_FUEL);
        for (int lane = 0; lane < MAX_PROCESSING_SLOTS; lane++) {
            int laneIndex = lane;
            heat[lane] = new HeatLane();
            laneStatsCaches[lane] = new MachineStatsCache();
            laneStatsBuilders.add(() -> buildLaneStats(laneIndex));
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FurnaceBlockEntity furnace) {
        furnace.beginEnergyTelemetryTick();
        furnace.atmosphere.beginTick();
        furnace.releaseRemovedGear(level);
        MachineStatAccumulator baseStats = furnace.baseEffectiveStats();
        int activeLanes = furnace.activeProcessingSlots(baseStats);
        boolean running = false;
        boolean changed = false;

        FurnaceRecipe[] recipes = new FurnaceRecipe[MAX_PROCESSING_SLOTS];
        int heatedLanes = 0;
        for (int lane = 0; lane < activeLanes; lane++) {
            recipes[lane] = furnace.findNextRecipe(lane, furnace.statsForLane(lane));
            heatedLanes += recipes[lane] == null ? 0 : 1;
        }

        for (int lane = 0; lane < activeLanes; lane++) {
            MachineStatAccumulator laneStats = furnace.statsForLane(lane);
            FurnaceRecipe recipe = recipes[lane];
            HeatLane heat = furnace.heat[lane];
            if (recipe == null) {
                changed |= furnace.resetCycle(lane);
                FurnaceRecipe blocked = furnace.outputBlockedRecipe(lane, laneStats);
                changed |= blocked != null ? furnace.holdLane(lane, blocked, laneStats) : furnace.idleLane(lane, laneStats);
                continue;
            }
            if (!furnace.activeCycleMatches(lane)) {
                changed |= furnace.resetCycle(lane);
                changed |= furnace.resetBulkSpeed();
            }
            changed |= furnace.rememberHeatEnvelope(lane, recipe);
            HeatEngine.Envelope envelope = furnace.envelope(lane, recipe, laneStats);
            HeatEngine.HeatSupply supply = furnace.heatSupply(lane, laneStats);

            if (!heat.started()
                    && heat.exactTemperature() >= envelope.minimum()
                    && heat.exactTemperature() <= envelope.ceilingFor(heat)
                    && ProcessingChance.rollInstant(level, laneStats)
                    && furnace.payInstant(lane, recipe, laneStats, supply)) {
                furnace.markInstantProcess(lane);
                furnace.startCycleIfNeeded(lane, recipe, laneStats);
                if (furnace.process(lane, recipe, laneStats)) {
                    changed |= furnace.bulkSpeed.recordProcess(furnace.activeTraits());
                }
                running = true;
                changed = true;
                continue;
            }

            boolean wasStarted = heat.started();
            HeatEngine.Load load = furnace.load(lane, recipe, laneStats, heatedLanes);
            HeatEngine.Tick tick = HeatEngine.work(heat, envelope, laneStats, load, supply, level.getGameTime(), lane);
            furnace.tooHotLane[lane] = tick.tooHot();
            furnace.starved[lane] = tick.starved();
            changed = true;
            if (tick.tooHot()) {
                continue;
            }
            running |= tick.working() || heat.lastHeat() > 0.0;
            if (!heat.started()) {
                continue;
            }
            if (!wasStarted) {
                furnace.startCycleIfNeeded(lane, recipe, laneStats);
                furnace.shareLoadDip(lane, envelope, laneStats, load, activeLanes, heatedLanes);
            }
            if (tick.starved()) {
                furnace.registerPowerDrop(lane, recipe, laneStats);
            }
            furnace.updateFailureStrain(lane, recipe, laneStats, envelope, tick.workTemperature());
            furnace.atmosphere.use(tick.workTemperature(), HeatEngine.overheatTemperature(envelope.safeMaximum(), laneStats), furnace.atmosphereBonus());
            if (furnace.failureStrain[lane] >= HeatControl.FAILURE_STRAIN_THRESHOLD) {
                furnace.tryFailCycle(lane, recipe);
                continue;
            }
            if (HeatEngine.finished(heat, furnace.cycleWork(lane, recipe, laneStats)) && furnace.process(lane, recipe, laneStats)) {
                changed |= furnace.bulkSpeed.recordProcess(furnace.activeTraits());
            }
        }

        for (int lane = activeLanes; lane < MAX_PROCESSING_SLOTS; lane++) {
            changed |= furnace.resetCycle(lane);
            changed |= furnace.idleLane(lane, baseStats);
        }

        if (!running) {
            changed |= furnace.bulkSpeed.reset();
        }
        BaseMachineBlock.setActive(level, pos, state, running);
        if (changed) {
            furnace.setChanged();
        }
    }

    public int heatSlider() {
        return heatSlider;
    }

    /** Sets how far past each recipe target the lanes run, from 0 to 100 percent of the band up to Max Temperature. */
    public void setHeatSlider(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        if (clamped != heatSlider) {
            heatSlider = clamped;
            setChanged();
        }
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ItemStackHandler getGearInventory() {
        return gearInventory;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == Direction.UP) {
            return topItemHandler;
        }
        if (side == Direction.DOWN) {
            return bottomItemHandler;
        }
        if (side != null) {
            return sideItemHandler;
        }
        return getMachineInventory();
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return energy;
    }

    public boolean isElectric() {
        return furnaceMaterial().electric();
    }

    public boolean usesHeatCores() {
        return furnaceMaterial().usesHeatCores();
    }

    public int activeProcessingSlots() {
        return activeProcessingSlots(effectiveStats());
    }

    public int activeGearSlots() {
        return Math.max(1, Math.min(MAX_GEAR_SLOTS, furnaceMaterial().heatCoreSlots()));
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator baseStats = baseEffectiveStats();
        int status = statusCode(baseStats);
        int lane = machineInfoLane(baseStats);
        FurnaceRecipe recipe = lane < 0 ? null : findRecipeWithoutGates(lane);
        long energyDemand = isElectric() && hasEnergyDemandStatus(status) ? totalActiveEnergyDemand(baseStats) : 0;
        long energyRate = energyDemand > 0 ? -energyDemand : 0;
        int connectorDemand = (int) Math.min(Integer.MAX_VALUE, energyDemand);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("furnace")
                .stage(furnaceMaterial().stage())
                .state(machineInfoState(status), furnaceBlockedReason(status))
                .progress(lane < 0 ? 0 : currentElapsedTicks(lane), lane < 0 ? 0 : currentProcessingTicks(lane))
                .energy(energyStored(), energyCapacity(), energyRate)
                .energyTelemetry(
                        lastEnergyInput(),
                        0,
                        connector.transferRate(),
                        energyBottleneck(connector, connectorDemand)
                )
                .fuel(burnTime, totalBurnTime)
                .heat(lane < 0 ? 0 : heat[lane].temperature(), lane < 0 ? 0 : currentTargetTemperature(lane))
                .setpoint(lane < 0 ? 0 : currentSetpoint(lane))
                .batch(lane < 0 || recipe == null ? 0 : laneBatch(lane, recipe, statsForLane(lane)))
                .slots(activeProcessingSlots(baseStats), MAX_PROCESSING_SLOTS)
                .gear(furnaceGearSummary())
                .output(status == STATUS_OUTPUT_FULL
                        ? MachineInfoSnapshot.OutputSummary.OUTPUT_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .refinement(machineTraits())
                .build();
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return inventory;
    }

    @Override
    public MachineType refinementMachineType() {
        return furnaceMaterial().machineType();
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(
                machineTraits(),
                Component.translatable("container.rngtech." + furnaceMaterial().blockId())
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FurnaceMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        MachineTraits.STREAM_CODEC.encode(buffer, machineTraits());
    }

    public void dropInventory(Level level) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        dropGearInventory(level);
        dropRefinementInventory(level);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        ListTag lanes = new ListTag();
        for (HeatLane lane : heat) {
            CompoundTag laneTag = new CompoundTag();
            lane.save(laneTag);
            lanes.add(laneTag);
        }
        tag.put("HeatLanes", lanes);
        tag.putInt("HeatTrack", heatSlider);
        tag.putIntArray("LockedBatches", lockedBatch);
        tag.putIntArray("LastTargetTemperatures", lastTargetTemperature);
        tag.putIntArray("LastSafeMaximumTemperatures", lastSafeMaximumTemperature);
        tag.putIntArray("FailureStrains", failureStrain);
        atmosphere.save(tag, registries);
        for (int lane = 0; lane < MAX_PROCESSING_SLOTS; lane++) {
            tag.put("ActiveInput" + lane, activeInputs[lane].saveOptional(registries));
        }
        tag.putInt("BurnTime", burnTime);
        tag.putInt("TotalBurnTime", totalBurnTime);
        tag.putInt("Energy", internalEnergyStored());
        bulkSpeed.save(tag);
        LedgerNbt.save(tag, "BloomLedger", bloomLedger);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadInventory(tag.getCompound("Inventory"), registries);
        if (tag.contains("GearInventory")) {
            loadGearInventory(tag.getCompound("GearInventory"), registries);
        }
        ListTag savedLanes = tag.getList("HeatLanes", Tag.TAG_COMPOUND);
        int[] legacyTemperatures = tag.getIntArray("CurrentTemperatures");
        for (int lane = 0; lane < heat.length; lane++) {
            heat[lane] = new HeatLane();
            if (lane < savedLanes.size()) {
                heat[lane].load(savedLanes.getCompound(lane));
            } else if (lane < legacyTemperatures.length) {
                // Saved before the heat engine: the lane keeps its heat, and an unfinished smelt starts over.
                heat[lane].setTemperature(legacyTemperatures[lane]);
            }
        }
        heatSlider = Math.max(0, Math.min(100, tag.getInt("HeatTrack")));
        int[] savedBatches = tag.getIntArray("LockedBatches");
        for (int lane = 0; lane < lockedBatch.length; lane++) {
            lockedBatch[lane] = lane < savedBatches.length ? Math.max(0, savedBatches[lane]) : 0;
        }
        int[] savedTargetTemperatures = tag.getIntArray("LastTargetTemperatures");
        if (savedTargetTemperatures.length > 0) {
            System.arraycopy(
                    savedTargetTemperatures,
                    0,
                    lastTargetTemperature,
                    0,
                    Math.min(savedTargetTemperatures.length, lastTargetTemperature.length)
            );
        }
        int[] savedSafeTemperatures = tag.getIntArray("LastSafeMaximumTemperatures");
        if (savedSafeTemperatures.length > 0) {
            System.arraycopy(
                    savedSafeTemperatures,
                    0,
                    lastSafeMaximumTemperature,
                    0,
                    Math.min(savedSafeTemperatures.length, lastSafeMaximumTemperature.length)
            );
        }
        atmosphere.load(tag, registries);
        int[] savedFailureStrains = tag.getIntArray("FailureStrains");
        if (savedFailureStrains.length > 0) {
            System.arraycopy(savedFailureStrains, 0, failureStrain, 0, Math.min(savedFailureStrains.length, failureStrain.length));
        }
        for (int lane = 0; lane < activeInputs.length; lane++) {
            activeInputs[lane] = ItemStack.parseOptional(registries, tag.getCompound("ActiveInput" + lane));
            if (activeInputs[lane].isEmpty() && heat[lane].started()) {
                activeInputs[lane] = singleCopy(inventory.getStackInSlot(inputSlot(lane)));
            }
        }
        burnTime = tag.getInt("BurnTime");
        totalBurnTime = tag.getInt("TotalBurnTime");
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        bulkSpeed.load(tag);
        clampInternalEnergy();
        LedgerNbt.load(tag, "BloomLedger", bloomLedger);
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    public static boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public static boolean isGearComponent(ItemStack stack) {
        return stack.getItem() instanceof MachinePartItem part
                && (part.machineType() == MachineType.FURNACE
                        || part.machineType() == MachineType.ELECTRIC_FURNACE
                        || part.partType() == MachinePartType.HEAT_CORE);
    }

    public MachineStatAccumulator effectiveStats() {
        return baseEffectiveStats();
    }

    private MachineStatAccumulator baseEffectiveStats() {
        return cachedStats(bulkSpeed.count());
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.furnace(furnaceMaterial());
        MachineTraits activeTraits = effectiveTraits();
        stats.apply(activeTraits);
        MegaPassiveTree.applyNodeStats(stats, machineProgression(), MachineMasteryFamily.FURNACE);
        for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
            ItemStack gear = gearInventory.getStackInSlot(slot);
            if (isAllowedGearComponent(slot, gear)) {
                ComponentBaseStatCatalog.applyEffectiveContribution(stats, gear);
                ComponentBaseStatCatalog.applyAttributeAffixes(stats, gear);
            }
        }
        MegaPassiveTree.applyAttributeStats(stats, machineProgression(), MachineMasteryFamily.FURNACE);
        if (isElectric() && !hasBatteryCell()) {
            stats.apply(MachineStatAccumulator.NO_BATTERY_SOURCE, new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.PROCESSING_SPEED,
                    ModifierOperation.LESS,
                    RNGTechConfig.FURNACE_NO_BATTERY_CELL_PROCESSING_SPEED_MULTIPLIER.get()
            ));
        }
        bulkSpeed.apply(stats, activeTraits);
        return stats;
    }

    private MachineTraits activeTraits() {
        return effectiveTraits();
    }

    private int activeProcessingSlots(MachineStatAccumulator stats) {
        return Math.max(1, Math.min(MAX_PROCESSING_SLOTS, stats.intValue(MachineStat.INPUT_SLOTS)));
    }

    private boolean isAllowedGearComponent(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= activeGearSlots()) {
            return false;
        }
        if (usesHeatCores()) {
            return GearParts.is(stack, MachinePartType.HEAT_CORE, furnaceMaterial().stage());
        }
        return stack.getItem() instanceof MachinePartItem part && part.machineType() == furnaceMaterial().machineType();
    }

    private void dropGearInventory(Level level) {
        for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
            ItemStack stack = gearInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
                gearInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private FurnaceRecipe findNextRecipe(int lane) {
        return findNextRecipe(lane, statsForLane(lane));
    }

    private FurnaceRecipe findNextRecipe(int lane, MachineStatAccumulator stats) {
        if (level == null || lane < 0 || lane >= activeProcessingSlots(stats)) {
            return null;
        }
        ItemStack input = inventory.getStackInSlot(inputSlot(lane));
        if (input.isEmpty()) {
            return null;
        }
        FurnaceRecipe recipe = laneRecipe(lane);
        if (recipe == null || !meetsRecipeRequirements(recipe, stats, input) || !canAcceptOutput(lane, recipe)
                || waitsForFullBatch(lane, recipe, stats)) {
            return null;
        }
        return recipe;
    }

    /** How many recipe lookups searched the recipes instead of reusing a remembered result. */
    public int recipeSearches() {
        return recipes.searches();
    }

    /** The recipe for a lane's input, ignoring heat and output gates. */
    private FurnaceRecipe laneRecipe(int lane) {
        return level == null ? null : recipes.find(lane, level, inventory.getStackInSlot(inputSlot(lane)), RECIPE_SEARCH);
    }

    private FurnaceRecipe findRecipeWithoutGates(int lane) {
        if (level == null || lane < 0 || lane >= MAX_PROCESSING_SLOTS) {
            return null;
        }
        ItemStack input = inventory.getStackInSlot(inputSlot(lane));
        return input.isEmpty() ? null : laneRecipe(lane);
    }

    public boolean isSmeltable(ItemStack stack) {
        return level != null && recipes.find(MAX_PROCESSING_SLOTS, level, stack, RECIPE_SEARCH) != null;
    }

    private boolean meetsRecipeRequirements(FurnaceRecipe recipe, MachineStatAccumulator stats, ItemStack input) {
        if (stats.intValue(MachineStat.MAX_TEMPERATURE) < targetTemperature(recipe)) {
            return false;
        }
        return (recipe.hasFailureOutput() && furnaceMaterial().stage() >= 4 && !cleanBloom(input))
                || stats.value(MachineStat.TEMPERATURE_STABILITY) >= recipe.requiredTemperatureStability();
    }

    /** Fluxed Blend lowers every temperature of a blend smelt. */
    private int blendRelief(FurnaceRecipe recipe) {
        if (!hasMasteryBehavior("FLUXED_BLEND")) {
            return 0;
        }
        for (ItemStack stack : recipe.ingredient().getItems()) {
            if (stack.is(ModTags.Items.ALLOY_BLEND_SMELTABLES)) {
                return AscendancyFormulas.FLUXED_BLEND_DEGREES;
            }
        }
        return 0;
    }

    private int targetTemperature(FurnaceRecipe recipe) {
        return Math.max(0, recipe.targetTemperature() - blendRelief(recipe));
    }

    private int minimumTemperature(FurnaceRecipe recipe) {
        return Math.max(0, recipe.minimumTemperature() - blendRelief(recipe));
    }

    private int safeMaximumTemperature(FurnaceRecipe recipe) {
        return Math.max(targetTemperature(recipe), recipe.safeMaximumTemperature() - blendRelief(recipe));
    }

    /** Clean Bloom: ore, raw, and crushed smelts never fail; low stability pauses them as it does below Stage 4. */
    private boolean cleanBloom(ItemStack input) {
        return hasMasteryBehavior("CLEAN_BLOOM") && input.is(ModTags.Items.BLOOM_LEDGER_INPUTS);
    }

    private int currentMinimumTemperature(MachineStatAccumulator baseStats) {
        int activeLanes = activeProcessingSlots(baseStats);
        int minimumTemperature = 0;
        for (int lane = 0; lane < activeLanes; lane++) {
            ItemStack input = inventory.getStackInSlot(inputSlot(lane));
            if (input.isEmpty()) {
                continue;
            }
            FurnaceRecipe recipe = laneRecipe(lane);
            if (recipe == null) {
                continue;
            }
            MachineStatAccumulator laneStats = statsForLane(lane);
            if (laneStats.intValue(MachineStat.MAX_TEMPERATURE) < targetTemperature(recipe)) {
                return minimumTemperature(recipe);
            }
            minimumTemperature = Math.max(minimumTemperature, minimumTemperature(recipe));
        }
        return minimumTemperature;
    }

    private int statusCode(MachineStatAccumulator baseStats) {
        boolean hasInput = false;
        int blockedStatus = STATUS_NO_INPUT;
        int activeLanes = activeProcessingSlots(baseStats);
        for (int lane = 0; lane < activeLanes; lane++) {
            ItemStack input = inventory.getStackInSlot(inputSlot(lane));
            if (input.isEmpty()) {
                continue;
            }
            hasInput = true;

            MachineStatAccumulator laneStats = statsForLane(lane);
            FurnaceRecipe recipe = laneRecipe(lane);
            if (recipe == null) {
                blockedStatus = STATUS_INVALID_RECIPE;
                continue;
            }
            if (laneStats.intValue(MachineStat.MAX_TEMPERATURE) < targetTemperature(recipe)) {
                blockedStatus = STATUS_HEAT_LOW;
                continue;
            }
            if (!failureEnabled(lane, laneStats)
                    && laneStats.value(MachineStat.TEMPERATURE_STABILITY) < recipe.requiredTemperatureStability()) {
                blockedStatus = STATUS_STABILITY_LOW;
                continue;
            }
            if (!canAcceptOutput(lane, recipe)) {
                blockedStatus = STATUS_OUTPUT_FULL;
                continue;
            }
            if (waitsForFullBatch(lane, recipe, laneStats)) {
                blockedStatus = input.getCount() < fullBatch(lane, laneStats) ? STATUS_FILLING_BATCH : STATUS_OUTPUT_FULL;
                continue;
            }
            if (overheatFailureEnabled(lane, laneStats)
                    && failureStrain[lane] >= HeatControl.FAILURE_STRAIN_THRESHOLD
                    && !canAcceptFailureOutput(lane, recipe)) {
                blockedStatus = STATUS_OUTPUT_FULL;
                continue;
            }
            if (tooHotLane[lane]) {
                blockedStatus = STATUS_HEAT_HIGH;
                continue;
            }
            if (powerSensitiveActive(lane, laneStats)) {
                blockedStatus = STATUS_POWER_DROP;
                continue;
            }
            if (failureStrain[lane] > 0 && overheatFailureEnabled(lane, laneStats)) {
                blockedStatus = STATUS_FAILURE_RISK;
                continue;
            }
            int energyStatus = laneEnergyStatus(lane);
            if (energyStatus != STATUS_READY) {
                blockedStatus = energyStatus;
                continue;
            }
            return heat[lane].started() ? STATUS_READY : STATUS_WARMING;
        }
        return hasInput ? blockedStatus : STATUS_NO_INPUT;
    }

    private int machineInfoLane(MachineStatAccumulator baseStats) {
        int activeLanes = activeProcessingSlots(baseStats);
        for (int lane = 0; lane < activeLanes; lane++) {
            if (heat[lane].started()) {
                return lane;
            }
        }
        for (int lane = 0; lane < activeLanes; lane++) {
            if (!inventory.getStackInSlot(inputSlot(lane)).isEmpty()) {
                return lane;
            }
        }
        return activeLanes > 0 ? 0 : -1;
    }

    private MachineInfoSnapshot.WorkState machineInfoState(int status) {
        if (status == STATUS_READY || status == STATUS_WARMING || status == STATUS_HEAT_HIGH || status == STATUS_FAILURE_RISK) {
            return getBlockState().getValue(BaseMachineBlock.ACTIVE)
                    ? MachineInfoSnapshot.WorkState.RUNNING
                    : MachineInfoSnapshot.WorkState.IDLE;
        }
        return status == STATUS_NO_INPUT || status == STATUS_FILLING_BATCH
                ? MachineInfoSnapshot.WorkState.IDLE
                : MachineInfoSnapshot.WorkState.BLOCKED;
    }

    private static boolean hasEnergyDemandStatus(int status) {
        return status == STATUS_READY
                || status == STATUS_WARMING
                || status == STATUS_NO_POWER
                || status == STATUS_POWER_LIMITED
                || status == STATUS_POWER_DROP
                || status == STATUS_FAILURE_RISK;
    }

    private MachineInfoSnapshot.BlockedReason furnaceBlockedReason(int status) {
        return switch (status) {
            case STATUS_NO_INPUT -> MachineInfoSnapshot.BlockedReason.NO_INPUT;
            case STATUS_INVALID_RECIPE -> MachineInfoSnapshot.BlockedReason.INVALID_RECIPE;
            case STATUS_HEAT_LOW -> MachineInfoSnapshot.BlockedReason.HEAT_LOW;
            case STATUS_STABILITY_LOW -> MachineInfoSnapshot.BlockedReason.STABILITY_LOW;
            case STATUS_OUTPUT_FULL -> MachineInfoSnapshot.BlockedReason.OUTPUT_FULL;
            case STATUS_NO_POWER, STATUS_POWER_LIMITED, STATUS_POWER_DROP -> MachineInfoSnapshot.BlockedReason.NO_POWER;
            case STATUS_NO_FUEL -> MachineInfoSnapshot.BlockedReason.NO_FUEL;
            default -> MachineInfoSnapshot.BlockedReason.NONE;
        };
    }

    private MachineInfoSnapshot.GearSummary furnaceGearSummary() {
        if (usesHeatCores() && !hasAllowedHeatCore()) {
            return MachineInfoSnapshot.GearSummary.MISSING_HEAT_CORE;
        }
        if (isElectric() && !hasBatteryCell()) {
            return MachineInfoSnapshot.GearSummary.MISSING_BATTERY_CELL;
        }
        return usesHeatCores() ? MachineInfoSnapshot.GearSummary.HEAT_CORE_INSTALLED : MachineInfoSnapshot.GearSummary.NONE;
    }

    private boolean process(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (level == null) {
            return false;
        }
        int batch = laneBatch(lane, recipe, stats);
        int itemOutput = recipe.outputStack().getCount();
        int crafted = 0;
        // Each item keeps room for the base output of the items after it, so a bonus never strands a paid-for item.
        while (crafted < batch && craft(lane, recipe, stats, (batch - crafted - 1) * itemOutput)) {
            crafted++;
        }
        if (crafted == 0) {
            return false;
        }
        completeCycle(lane);
        setChanged();
        return true;
    }

    private boolean craft(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats, int reserved) {
        ItemStack input = inventory.getStackInSlot(inputSlot(lane));
        if (!recipe.matches(new SingleRecipeInput(input), level)) {
            return false;
        }

        ItemStack baseResult = recipe.outputStack();
        ItemStack result = ProcessingChance.applySuperOutput(level, stats, recipe, baseResult, baseResult);
        int outputSlot = outputSlot(lane);
        if (!canMergeOutput(outputSlot, result, reserved)) {
            result = baseResult;
            if (!canMergeOutput(outputSlot, result, reserved)) {
                return false;
            }
        }

        result = withLedger(recipe, stats, input, result, outputSlot, reserved);
        input.shrink(1);
        mergeOutput(outputSlot, result);
        grantRecipeXp(recipe);
        refundClosedLoopEnergy(recipe, stats);
        return true;
    }

    /** The Bloom Ledger banks a share of each eligible smelt and pays whole items once the output has room. */
    private ItemStack withLedger(
            FurnaceRecipe recipe,
            MachineStatAccumulator stats,
            ItemStack input,
            ItemStack result,
            int outputSlot,
            int reserved
    ) {
        if (!ledgerEligible(recipe, input, stats)) {
            return result;
        }
        ItemStack base = recipe.outputStack();
        Item item = base.getItem();
        boolean crushed = hasMasteryBehavior("CRUSHER_LINE") && input.is(ModTags.Items.CRUSHED_MATERIALS);
        bloomLedger.add(item, AscendancyFormulas.ledgerShare(stats, base.getCount(), crushed));
        int payable = bloomLedger.payable(item);
        ItemStack paid = ProcessingChance.grow(result, base, payable);
        if (paid == result || !canMergeOutput(outputSlot, paid, reserved)) {
            return result;
        }
        bloomLedger.pay(item, payable);
        return paid;
    }

    private boolean ledgerEligible(FurnaceRecipe recipe, ItemStack input, MachineStatAccumulator stats) {
        return recipe.allowsBonusOutput() && stats.value(MachineStat.LEDGER_RATE) > 0.0 && input.is(ModTags.Items.BLOOM_LEDGER_INPUTS);
    }

    /** A lane's Bloom Ledger progress toward its next item, per thousand, or -1 when its smelt does not feed the ledger. */
    private int ledgerDisplay(int lane) {
        FurnaceRecipe recipe = findRecipeWithoutGates(lane);
        if (recipe == null || lane >= activeProcessingSlots()
                || !ledgerEligible(recipe, inventory.getStackInSlot(inputSlot(lane)), statsForLane(lane))) {
            return -1;
        }
        return LedgerNbt.permille(bloomLedger.progress(recipe.outputStack().getItem()));
    }

    private void refundClosedLoopEnergy(FurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (!isElectric() || !FurnacePassiveTree.usesClosedLoopRecuperator(machineProgression())) {
            return;
        }
        int refund = Math.max(1, (int) Math.ceil(recipe.heat() * HeatEngine.energyRate(stats) * CLOSED_LOOP_REFUND));
        int stored = internalEnergyStored();
        int capacity = internalEnergyCapacity();
        if (stored >= capacity) {
            return;
        }
        internalEnergy = Math.min(capacity, stored + refund);
    }

    private void grantRecipeXp(FurnaceRecipe recipe) {
        if (recipe.machineXp() <= 0) {
            return;
        }
        MachineProgressionState progression = machineProgression();
        int xpQuarters = recipeXpQuarters(recipe, progression);
        if (xpQuarters <= 0) {
            return;
        }
        grantMasteryXp(MachineProgressionState.workXp(recipe.machineXp(), recipe.machineXpBand()), xpQuarters);
    }

    private static int recipeXpQuarters(FurnaceRecipe recipe, MachineProgressionState progression) {
        int levelDelta = progression.level() - recipe.machineXpBand();
        if (levelDelta <= 0) {
            return MachineProgressionState.XP_REMAINDER_SCALE;
        }
        if (levelDelta == 1) {
            return MachineProgressionState.XP_REMAINDER_SCALE / 2;
        }
        if (levelDelta == 2) {
            return MachineProgressionState.XP_REMAINDER_SCALE / 4;
        }
        return 0;
    }

    public boolean unlockPassiveNode(MegaPassiveNode node) {
        return allocateMastery(node);
    }

    @Override
    public boolean mutesMachineSound() {
        return FurnacePassiveTree.mutesMachineSound(machineProgression());
    }

    private boolean tryFailCycle(int lane, FurnaceRecipe recipe) {
        if (!overheatFailureEnabled(lane, statsForLane(lane)) || level == null) {
            return false;
        }
        ItemStack input = inventory.getStackInSlot(inputSlot(lane));
        if (!recipe.matches(new SingleRecipeInput(input), level)) {
            return false;
        }

        // A failed batch ruins every item in it.
        int batch = laneBatch(lane, recipe, statsForLane(lane));
        ItemStack failure = recipe.failureStack();
        ItemStack result = failure.copyWithCount(failure.getCount() * batch);
        int outputSlot = outputSlot(lane);
        if (!canMergeOutput(outputSlot, result)) {
            return false;
        }

        input.shrink(batch);
        mergeOutput(outputSlot, result);
        resetCycle(lane);
        resetBulkSpeed();
        ChallengeLoot.reward(level, worldPosition, ChallengeLoot.HEAT_FAILURE,
                ChallengeContext.of(this).withRecipeStage(recipe.machineXpBand()), inventory, outputSlot);
        setChanged();
        return true;
    }

    private boolean canAcceptOutput(int lane, FurnaceRecipe recipe) {
        if (level == null) {
            return false;
        }
        return canMergeOutput(outputSlot(lane), recipe.outputStack());
    }

    private boolean canAcceptFailureOutput(int lane, FurnaceRecipe recipe) {
        if (!recipe.hasFailureOutput()) {
            return false;
        }
        ItemStack failure = recipe.failureStack();
        return canMergeOutput(outputSlot(lane), failure.copyWithCount(failure.getCount() * laneBatch(lane, recipe, statsForLane(lane))));
    }

    private boolean canMergeOutput(int slot, ItemStack result) {
        return canMergeOutput(slot, result, 0);
    }

    /** Whether {@code result} fits while still leaving room for {@code reserved} more of it. */
    private boolean canMergeOutput(int slot, ItemStack result, int reserved) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = inventory.getStackInSlot(slot);
        int slotLimit = Math.min(result.getMaxStackSize(), inventory.getSlotLimit(slot));
        if (output.isEmpty()) {
            return result.getCount() + reserved <= slotLimit;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() + reserved <= slotLimit;
    }

    private void mergeOutput(int slot, ItemStack result) {
        ItemStack output = inventory.getStackInSlot(slot);
        if (output.isEmpty()) {
            inventory.setStackInSlot(slot, result.copy());
            return;
        }
        ItemStack merged = output.copy();
        merged.grow(result.getCount());
        inventory.setStackInSlot(slot, merged);
    }

    private int currentProcessingTicks(int lane) {
        FurnaceRecipe recipe = findNextRecipe(lane);
        if (recipe == null) {
            return 0;
        }
        MachineStatAccumulator stats = statsForLane(lane);
        return HeatEngine.cycleTicks(cycleWork(lane, recipe, stats), load(lane, recipe, stats, 1), envelope(lane, recipe, stats));
    }

    private int currentElapsedTicks(int lane) {
        FurnaceRecipe recipe = findNextRecipe(lane);
        if (recipe == null) {
            return 0;
        }
        MachineStatAccumulator stats = statsForLane(lane);
        return HeatEngine.elapsedTicks(heat[lane], load(lane, recipe, stats, 1), envelope(lane, recipe, stats));
    }

    /** FE/t the lanes' heaters spent last tick: on wall loss when {@code holding}, otherwise on heating chambers and inputs. */
    private int heatEnergy(int part) {
        if (!isElectric()) {
            return 0;
        }
        double total = 0.0;
        for (int lane = 0; lane < activeProcessingSlots(); lane++) {
            total += HeatEngine.heatSplit(heat[lane])[part] * HeatEngine.energyRate(statsForLane(lane));
        }
        return (int) Math.ceil(total);
    }

    private int reachDisplay() {
        int lane = machineInfoLane(baseEffectiveStats());
        FurnaceRecipe recipe = lane < 0 ? null : findNextRecipe(lane);
        return recipe == null ? 0 : envelope(lane, recipe, statsForLane(lane)).reach();
    }

    private int heatFactorDisplay() {
        int lane = machineInfoLane(baseEffectiveStats());
        FurnaceRecipe recipe = lane < 0 ? null : findNextRecipe(lane);
        return recipe == null ? 0 : (int) Math.round(100.0 * HeatEngine.heatFactor(heat[lane].exactTemperature(),
                envelope(lane, recipe, statsForLane(lane))));
    }

    private int wobbleDisplay() {
        int lane = machineInfoLane(baseEffectiveStats());
        FurnaceRecipe recipe = lane < 0 ? null : findNextRecipe(lane);
        if (recipe == null) {
            return 0;
        }
        MachineStatAccumulator stats = statsForLane(lane);
        return (int) Math.round(HeatEngine.wobbleAmplitude(envelope(lane, recipe, stats), stats));
    }

    private int currentSetpoint(int lane) {
        FurnaceRecipe recipe = findNextRecipe(lane);
        return recipe == null ? 0 : envelope(lane, recipe, statsForLane(lane)).setpoint();
    }

    /** FE the lane's heater drew last tick. */
    private int currentEnergyCostPerTick(int lane) {
        if (!isElectric() || lane >= activeProcessingSlots()) {
            return 0;
        }
        return (int) Math.ceil(heat[lane].lastHeat() * HeatEngine.energyRate(statsForLane(lane)));
    }

    private int laneEnergyStatus(int lane) {
        if (isElectric()) {
            // A lane still taking heat while short of power is underpowered, not out of power.
            return !starved[lane] ? STATUS_READY
                    : energyStored() > 0 || heat[lane].lastAbsorbed() > 0.0 ? STATUS_POWER_LIMITED : STATUS_NO_POWER;
        }
        return hasBurnTime() || isFuel(inventory.getStackInSlot(SLOT_FUEL)) ? STATUS_READY : STATUS_NO_FUEL;
    }

    /** FE one item costs at the setpoint: its own heat plus its share of the wall loss during the cycle. */
    private int currentEnergyCostPerCraft(int lane) {
        if (!isElectric()) {
            return 0;
        }
        FurnaceRecipe recipe = findNextRecipe(lane);
        if (recipe == null) {
            return 0;
        }
        MachineStatAccumulator stats = statsForLane(lane);
        int batch = laneBatch(lane, recipe, stats);
        HeatEngine.Load load = load(lane, recipe, stats, 1);
        return HeatEngine.steadyEnergyPerCycle(envelope(lane, recipe, stats), load, cycleWork(lane, recipe, stats), stats) / Math.max(1, batch);
    }

    private long totalActiveEnergyDemand(MachineStatAccumulator baseStats) {
        long total = 0;
        int activeSlots = activeProcessingSlots(baseStats);
        for (int lane = 0; lane < activeSlots; lane++) {
            total += currentEnergyCostPerTick(lane);
        }
        return Math.min(Integer.MAX_VALUE, total);
    }

    private int currentTargetTemperature(int lane) {
        FurnaceRecipe recipe = findNextRecipe(lane);
        if (recipe != null) {
            return targetTemperature(recipe);
        }
        return hasCooldownHeat(lane) ? lastTargetTemperature[lane] : 0;
    }

    private int currentSafeMaximumTemperature(int lane) {
        FurnaceRecipe recipe = findNextRecipe(lane);
        if (recipe != null) {
            return safeMaximumTemperature(recipe);
        }
        return hasCooldownHeat(lane) ? lastSafeMaximumTemperature[lane] : 0;
    }

    private int currentOverheatTemperature(int lane) {
        FurnaceRecipe recipe = findNextRecipe(lane);
        int safeMaximumTemperature = recipe == null
                ? hasCooldownHeat(lane) ? lastSafeMaximumTemperature[lane] : 0
                : safeMaximumTemperature(recipe);
        if (safeMaximumTemperature <= 0) {
            return 0;
        }
        return HeatControl.effectiveOverheatTemperature(safeMaximumTemperature, statsForLane(lane), atmosphereBonus());
    }

    /** Overheat Tolerance the Nitrogen tank adds: electric chassis with Blast Draft only. */
    private double atmosphereBonus() {
        return isElectric() ? atmosphere.overheatBonus(hasMasteryBehavior("BLAST_DRAFT")) : 0.0;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return atmosphere.inlet();
    }

    /**
     * The lane's heat band. Overdrive Margin caps the slider that far below the recipe's safe maximum, and Safe Overdrive
     * caps it at the safe maximum.
     */
    private HeatEngine.Envelope envelope(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        int margin = stats.intValue(MachineStat.OVERDRIVE_MARGIN);
        int ceiling = margin > 0 || hasMasteryBehavior("SAFE_OVERDRIVE") ? safeMaximumTemperature(recipe) - margin : Integer.MAX_VALUE;
        return HeatEngine.envelope(
                minimumTemperature(recipe),
                targetTemperature(recipe),
                safeMaximumTemperature(recipe),
                recipe.requiredTemperatureStability(),
                overheatFailureEnabled(lane, stats),
                hasMasteryBehavior("BLAST_DRAFT"),
                stats,
                isElectric() ? heatSlider / 100.0 : 0.0,
                ceiling,
                atmosphereBonus()
        ).coolingFirst(hasMasteryBehavior("COOL_HEADED"));
    }

    /** Reference ticks a cycle needs: the recipe's time, longer for every extra item in the batch. */
    private double cycleWork(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        return recipe.processingTicks() * BatchProcessing.timeMultiplier(stats, laneBatch(lane, recipe, stats));
    }

    /**
     * Every item in the batch absorbs the recipe's heat over the cycle. Overdrive speeds work past the safe maximum, and
     * Shared Hearth splits each load dip across the lanes that are smelting.
     */
    private HeatEngine.Load load(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats, int heatedLanes) {
        int batch = laneBatch(lane, recipe, stats);
        int safeMaximum = safeMaximumTemperature(recipe);
        double overdrive = safeMaximum <= 0 ? 1.0 : AscendancyFormulas.overdriveSpeedMultiplier(heat[lane].temperature(), safeMaximum, stats);
        double workPerTick = HeatEngine.speed(stats) * overdrive;
        double heatPerTick = recipe.heat() * (double) batch * workPerTick / cycleWork(lane, recipe, stats);
        double dipShare = hasMasteryBehavior("SHARED_HEARTH") && heatedLanes > 1 ? 1.0 / heatedLanes : 1.0;
        return new HeatEngine.Load(heatPerTick, workPerTick, batch, dipShare);
    }

    /** Shared Hearth: the rest of a starting lane's load dip comes out of the other smelting lanes. */
    private void shareLoadDip(
            int lane,
            HeatEngine.Envelope envelope,
            MachineStatAccumulator stats,
            HeatEngine.Load load,
            int activeLanes,
            int heatedLanes
    ) {
        if (load.dipShare() >= 1.0) {
            return;
        }
        double share = HeatEngine.loadDip(envelope, stats, load.batch()) * load.dipShare();
        for (int other = 0; other < activeLanes; other++) {
            if (other != lane && heat[other].started()) {
                heat[other].setTemperature(Math.max(lastTargetTemperature[other], heat[other].exactTemperature() - share));
            }
        }
    }

    private HeatEngine.HeatSupply heatSupply(int lane, MachineStatAccumulator stats) {
        return isElectric() ? HeatEngine.electric(heat[lane], stats, this::drawEnergy) : HeatEngine.fuel(heat[lane], this::burnFuel);
    }

    /** Takes up to {@code amount} FE from the internal buffer and the Battery Cell. */
    private int drawEnergy(int amount) {
        int take = Math.min(amount, consumeWorkingEnergy(amount, true));
        return take > 0 ? consumeWorkingEnergy(take, false) : 0;
    }

    /** Burns up to {@code ticks} burn ticks, lighting the next fuel item when the current one runs out. */
    private int burnFuel(int ticks) {
        int burned = 0;
        while (burned < ticks && (hasBurnTime() || tryStartBurningFuel())) {
            int take = Math.min(ticks - burned, burnTime);
            burnTime -= take;
            burned += take;
        }
        return burned;
    }

    /** Instant Process pays the whole batch's heat at once, without the wall loss of a full cycle. */
    private boolean payInstant(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats, HeatEngine.HeatSupply supply) {
        double needed = recipe.heat() * (double) laneBatch(lane, recipe, stats);
        if (isElectric()) {
            int energyCost = (int) Math.ceil(needed * HeatEngine.energyRate(stats));
            if (consumeWorkingEnergy(energyCost, true) < energyCost) {
                return false;
            }
        }
        return HeatEngine.payAtOnce(supply, needed);
    }

    /** The batch locked when the lane's cycle started, shrunk only if its input has since gone. */
    private int laneBatch(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        int available = availableBatch(lane, recipe, stats);
        return heat[lane].started() && lockedBatch[lane] > 0 ? Math.min(lockedBatch[lane], available) : available;
    }

    /** Dense Batching: an idle lane waits until its stack and output room fit a full batch. */
    private boolean waitsForFullBatch(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        return !heat[lane].started() && hasMasteryBehavior("DENSE_BATCHING") && availableBatch(lane, recipe, stats) < fullBatch(lane, stats);
    }

    private int fullBatch(int lane, MachineStatAccumulator stats) {
        return BatchProcessing.fullBatch(stats, false, inventory.getStackInSlot(inputSlot(lane)).getMaxStackSize());
    }

    /**
     * A lane batches items from its input stack, as far as its output slot has room for their base output. Crucible Heart
     * adds one item while the lane runs at twice the recipe target or hotter.
     */
    private int availableBatch(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        int crucibleHeart = hasMasteryBehavior("CRUCIBLE_HEART") && heat[lane].temperature() >= 2 * targetTemperature(recipe) ? 1 : 0;
        int limit = Math.min(BatchProcessing.batchSize(stats, false) + crucibleHeart, inventory.getStackInSlot(inputSlot(lane)).getCount());
        ItemStack output = recipe.outputStack();
        return Math.max(1, BatchProcessing.largestFitting(limit,
                batch -> batch == 1 || canMergeOutput(outputSlot(lane), output.copyWithCount(output.getCount() * batch))));
    }

    /** Lane stats follow the base stats and the lane's input item, whose tags pick the lane bonuses. */
    private MachineStatAccumulator statsForLane(int lane) {
        Item input = inventory.getStackInSlot(inputSlot(lane)).getItem();
        return laneStatsCaches[lane].get(baseEffectiveStats(), input, 0L, laneStatsBuilders.get(lane));
    }

    private MachineStatAccumulator buildLaneStats(int lane) {
        MachineStatAccumulator stats = baseEffectiveStats().mutable();
        if (hasMasteryBehavior("SLAG_RECLAIM") && inventory.getStackInSlot(inputSlot(lane)).is(ModTags.Items.MALFORMED_INGOTS)) {
            stats.apply(MegaPassiveTree.behaviorSource(machineProgression(), "SLAG_RECLAIM"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.PROCESSING_SPEED, ModifierOperation.MORE, 2.0));
            stats.apply(MegaPassiveTree.behaviorSource(machineProgression(), "SLAG_RECLAIM"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.ENERGY_USAGE, ModifierOperation.LESS, 0.5));
        }
        if (alloyBlendActive(lane)) {
            stats.apply(Component.translatable(MachineBehavior.ALLOY_BLEND.translationKey()), new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.PROCESSING_SPEED,
                    ModifierOperation.INCREASED_PERCENT,
                    20.0
            ));
        }
        return stats;
    }

    private boolean alloyBlendActive(int lane) {
        if (!effectiveTraits()
                .hasBehavior(MachineBehavior.ALLOY_BLEND)) {
            return false;
        }
        return inventory.getStackInSlot(inputSlot(lane)).is(ModTags.Items.ALLOY_BLEND_SMELTABLES);
    }

    private int energyStored() {
        return internalEnergyStored() + batteryCell.energyStored();
    }

    private int energyCapacity() {
        return internalEnergyCapacity() + batteryCell.capacity();
    }

    private int internalEnergyCapacity() {
        if (!isElectric()) {
            return 0;
        }
        return Math.max(1, (int) Math.round(effectiveStats().value(MachineStat.ENERGY_CAPACITY)));
    }

    private int internalEnergyStored() {
        return Math.max(0, Math.min(internalEnergy, internalEnergyCapacity()));
    }

    private int receiveInternalEnergy(int toReceive, boolean simulate) {
        if (!isElectric() || toReceive <= 0) {
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
        if (!isElectric() || toConsume <= 0) {
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

    private MachineInfoSnapshot.EnergyBottleneck energyBottleneck(
            AdjacentEnergyConnector.Info connector,
            int energyDemand
    ) {
        if (energyDemand > 0
                && energyStored() >= energyDemand
                && consumeWorkingEnergy(energyDemand, true) < energyDemand) {
            return MachineInfoSnapshot.EnergyBottleneck.CELL_RATE;
        }
        return connector.present() && energyDemand > connector.transferRate()
                ? MachineInfoSnapshot.EnergyBottleneck.CONNECTOR_INPUT
                : MachineInfoSnapshot.EnergyBottleneck.NONE;
    }

    private int lastEnergyInput() {
        beginEnergyTelemetryTick();
        return lastEnergyInput;
    }

    private void recordEnergyInput(int amount) {
        if (amount <= 0) {
            return;
        }
        beginEnergyTelemetryTick();
        energyInputThisTick += amount;
    }

    private void beginEnergyTelemetryTick() {
        if (level == null) {
            return;
        }
        long gameTime = level.getGameTime();
        if (energyTelemetryTick == gameTime) {
            return;
        }
        energyTelemetryTick = gameTime;
        lastEnergyInput = energyInputThisTick;
        energyInputThisTick = 0;
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
    }

    private boolean hasBurnTime() {
        return burnTime > 0;
    }

    private boolean tryStartBurningFuel() {
        if (level == null) {
            return false;
        }

        ItemStack fuel = inventory.getStackInSlot(SLOT_FUEL);
        int fuelBurnTime = fuel.getBurnTime(RecipeType.SMELTING);
        if (fuelBurnTime <= 0) {
            totalBurnTime = 0;
            return false;
        }

        int adjustedBurnTime = effectiveStats().adjustedFuelTicks(fuelBurnTime);
        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (!remainder.isEmpty()) {
            if (fuel.isEmpty()) {
                inventory.setStackInSlot(SLOT_FUEL, remainder);
            } else {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder);
            }
        }

        burnTime = adjustedBurnTime;
        totalBurnTime = adjustedBurnTime;
        setChanged();
        return true;
    }

    private boolean rememberHeatEnvelope(int lane, FurnaceRecipe recipe) {
        boolean changed = lastTargetTemperature[lane] != targetTemperature(recipe)
                || lastSafeMaximumTemperature[lane] != safeMaximumTemperature(recipe);
        lastTargetTemperature[lane] = targetTemperature(recipe);
        lastSafeMaximumTemperature[lane] = safeMaximumTemperature(recipe);
        return changed;
    }

    private boolean clearHeatEnvelope(int lane) {
        boolean changed = lastTargetTemperature[lane] != 0 || lastSafeMaximumTemperature[lane] != 0;
        lastTargetTemperature[lane] = 0;
        lastSafeMaximumTemperature[lane] = 0;
        return changed;
    }

    private boolean hasCooldownHeat(int lane) {
        return heat[lane].temperature() > HeatEngine.AMBIENT && lastSafeMaximumTemperature[lane] > 0;
    }

    /** An idle lane leaks heat; Hold the Fire keeps it while the lane holds an input it could smelt. */
    /** The lane's recipe when only a full output stops it, so the lane holds its heat instead of cooling. */
    private FurnaceRecipe outputBlockedRecipe(int lane, MachineStatAccumulator stats) {
        ItemStack input = inventory.getStackInSlot(inputSlot(lane));
        FurnaceRecipe recipe = input.isEmpty() ? null : laneRecipe(lane);
        return recipe != null && meetsRecipeRequirements(recipe, stats, input) && !canAcceptOutput(lane, recipe) ? recipe : null;
    }

    private boolean holdLane(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        tooHotLane[lane] = false;
        starved[lane] = false;
        HeatEngine.hold(heat[lane], envelope(lane, recipe, stats), stats, heatSupply(lane, stats));
        return true;
    }

    private boolean idleLane(int lane, MachineStatAccumulator stats) {
        tooHotLane[lane] = false;
        starved[lane] = false;
        boolean hold = hasMasteryBehavior("HOLD_THE_FIRE") && findRecipeWithoutGates(lane) != null;
        boolean changed = HeatEngine.idle(heat[lane], stats, hold);
        if (heat[lane].temperature() <= HeatEngine.AMBIENT) {
            changed |= clearHeatEnvelope(lane);
        }
        return changed;
    }

    private boolean updateFailureStrain(
            int lane,
            FurnaceRecipe recipe,
            MachineStatAccumulator stats,
            HeatEngine.Envelope envelope,
            double workTemperature
    ) {
        if (!envelope.canFail()) {
            if (failureStrain[lane] == 0) {
                return false;
            }
            failureStrain[lane] = 0;
            return true;
        }

        int simulatedTemperature = (int) Math.round(workTemperature);
        // Safe Overdrive keeps wobble spikes at the overheat temperature.
        if (hasMasteryBehavior("SAFE_OVERDRIVE")) {
            simulatedTemperature = Math.min(simulatedTemperature, envelope.overheat());
        }
        boolean fullFailure = failureEnabled(lane, stats);
        int addedStrain = HeatControl.failureStrainFromTemperature(
                simulatedTemperature,
                fullFailure ? envelope.minimum() : 0,
                envelope.overheat(),
                fullFailure ? recipe.requiredTemperatureStability() : 0.0,
                stats
        );
        addedStrain = HeatControl.paced(addedStrain, workTemperature, envelope);
        if (FurnacePassiveTree.usesQuenchProtocol(machineProgression())) {
            addedStrain = HeatControl.quenched(addedStrain);
        }
        if (addedStrain > 0) {
            failureStrain[lane] = Math.min(HeatControl.FAILURE_STRAIN_THRESHOLD, failureStrain[lane] + addedStrain);
            return true;
        }
        int recovered = Math.min(failureStrain[lane], HeatControl.strainRecovery(stats));
        if (recovered <= 0) {
            return false;
        }
        failureStrain[lane] -= recovered;
        return true;
    }

    private boolean registerPowerDrop(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (!recipe.powerSensitive() || !heat[lane].started() || !failureEnabled(lane, stats)) {
            return false;
        }
        failureStrain[lane] = Math.min(
                HeatControl.FAILURE_STRAIN_THRESHOLD,
                failureStrain[lane] + (hasPowerGrace() ? HeatControl.POWER_DROP_STRAIN / 2 : HeatControl.POWER_DROP_STRAIN)
        );
        return true;
    }

    /** Stage 4+ chassis: underheat, instability, overheating, and sensitive power drops all build strain. */
    private boolean failureEnabled(int lane, MachineStatAccumulator stats) {
        return furnaceMaterial().stage() >= 4 && overheatFailureEnabled(lane, stats);
    }

    /** Any chassis: a recipe with a failure output builds strain when its lane runs past the overheat temperature. */
    private boolean overheatFailureEnabled(int lane, MachineStatAccumulator stats) {
        FurnaceRecipe recipe = laneRecipe(lane);
        return recipe != null
                && recipe.hasFailureOutput()
                && !cleanBloom(inventory.getStackInSlot(inputSlot(lane)))
                && stats.intValue(MachineStat.MAX_TEMPERATURE) >= targetTemperature(recipe);
    }

    private boolean powerSensitiveActive(int lane, MachineStatAccumulator stats) {
        FurnaceRecipe recipe = laneRecipe(lane);
        return recipe != null
                && recipe.powerSensitive()
                && heat[lane].started()
                && failureEnabled(lane, stats)
                && starved[lane];
    }

    private boolean hasPowerGrace() {
        if (activeTraits().hasBehavior(MachineBehavior.POWER_GRACE)) {
            return true;
        }
        for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
            if (MachineImplicitCatalog.hasBehavior(gearInventory.getStackInSlot(slot), MachineBehavior.POWER_GRACE)) {
                return true;
            }
        }
        return false;
    }

    private void startCycleIfNeeded(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (lockedBatch[lane] != 0) {
            return;
        }
        lockedBatch[lane] = availableBatch(lane, recipe, stats);
        activeInputs[lane] = singleCopy(inventory.getStackInSlot(inputSlot(lane)));
    }

    private boolean activeCycleMatches(int lane) {
        if (!heat[lane].started()) {
            return true;
        }
        return !activeInputs[lane].isEmpty()
                && ItemStack.isSameItemSameComponents(activeInputs[lane], inventory.getStackInSlot(inputSlot(lane)));
    }

    private boolean resetCycle(int lane) {
        boolean changed = heat[lane].resetCycle()
                || lockedBatch[lane] != 0
                || failureStrain[lane] != 0
                || !activeInputs[lane].isEmpty();
        lockedBatch[lane] = 0;
        failureStrain[lane] = 0;
        activeInputs[lane] = ItemStack.EMPTY;
        return changed;
    }

    /** A finished cycle keeps its lane's strain: only time in the safe band drains it, so short cycles cannot dodge it. */
    private void completeCycle(int lane) {
        int strain = failureStrain[lane];
        resetCycle(lane);
        failureStrain[lane] = strain;
    }

    private void resetAllCycles() {
        for (int lane = 0; lane < MAX_PROCESSING_SLOTS; lane++) {
            resetCycle(lane);
        }
    }

    private boolean resetBulkSpeed() {
        return bulkSpeed.reset();
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
    }

    private ItemStack batteryCellStack() {
        return inventory.getStackInSlot(SLOT_FUEL);
    }

    private boolean hasBatteryCell() {
        return isBatteryCell(batteryCellStack());
    }

    private boolean hasAllowedHeatCore() {
        for (int slot = 0; slot < activeGearSlots(); slot++) {
            if (isAllowedGearComponent(slot, gearInventory.getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    private IEnergyStorage batteryCellEnergyStorage() {
        return batteryCell.storage();
    }

    private FurnaceChassisMaterial furnaceMaterial() {
        return getBlockState().getBlock() instanceof FurnaceBlock furnace ? furnace.material() : FurnaceChassisMaterial.PRIMITIVE;
    }

    private void loadInventory(CompoundTag inventoryTag, HolderLookup.Provider registries) {
        int savedSize = inventoryTag.getInt("Size");
        if (savedSize == SLOT_COUNT) {
            inventory.deserializeNBT(registries, inventoryTag);
            return;
        }

        ItemStackHandler legacyInventory = new ItemStackHandler(Math.max(0, savedSize));
        legacyInventory.deserializeNBT(registries, inventoryTag);
        if (legacyInventory.getSlots() > SLOT_INPUT) {
            inventory.setStackInSlot(SLOT_INPUT, legacyInventory.getStackInSlot(SLOT_INPUT));
        }
        if (legacyInventory.getSlots() > 1) {
            inventory.setStackInSlot(SLOT_FUEL, legacyInventory.getStackInSlot(1));
        }
        if (legacyInventory.getSlots() > 2) {
            inventory.setStackInSlot(SLOT_OUTPUT, legacyInventory.getStackInSlot(2));
        }
        for (int slot = 3; slot < Math.min(legacyInventory.getSlots(), SLOT_COUNT); slot++) {
            inventory.setStackInSlot(slot, legacyInventory.getStackInSlot(slot));
        }
    }

    private void loadGearInventory(CompoundTag gearTag, HolderLookup.Provider registries) {
        int savedSize = gearTag.getInt("Size");
        if (savedSize == MAX_GEAR_SLOTS) {
            gearInventory.deserializeNBT(registries, gearTag);
            return;
        }

        // Furnaces once had four Gear slots, one Heat Core per Lead Furnace lane. The first item keeps the slot; the rest leave.
        ItemStackHandler legacyGear = new ItemStackHandler(Math.max(0, savedSize));
        legacyGear.deserializeNBT(registries, gearTag);
        for (int slot = 0; slot < legacyGear.getSlots(); slot++) {
            ItemStack stack = legacyGear.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (gearInventory.getStackInSlot(0).isEmpty()) {
                gearInventory.setStackInSlot(0, stack);
            } else {
                holdRemovedGear(stack);
            }
        }
    }

    private static int inputSlot(int lane) {
        return SLOT_INPUT_START + lane;
    }

    private static int outputSlot(int lane) {
        return SLOT_OUTPUT_START + lane;
    }

    private static int inputLane(int slot) {
        int lane = slot - SLOT_INPUT_START;
        return lane >= 0 && lane < MAX_PROCESSING_SLOTS ? lane : -1;
    }

    private static boolean isOutputSlot(int slot) {
        return slot >= SLOT_OUTPUT_START && slot < SLOT_OUTPUT_START + MAX_PROCESSING_SLOTS;
    }

    private static ItemStack[] emptyInputs() {
        ItemStack[] stacks = new ItemStack[MAX_PROCESSING_SLOTS];
        for (int lane = 0; lane < stacks.length; lane++) {
            stacks[lane] = ItemStack.EMPTY;
        }
        return stacks;
    }

    private static ItemStack singleCopy(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    private final class FurnaceSidedItemHandler implements IItemHandler {
        private final int firstSlot;
        private final boolean allowInsert;
        private final boolean allowExtract;
        private final boolean processingLaneSlots;

        private FurnaceSidedItemHandler(int firstSlot, boolean allowInsert, boolean allowExtract, boolean processingLaneSlots) {
            this.firstSlot = firstSlot;
            this.allowInsert = allowInsert;
            this.allowExtract = allowExtract;
            this.processingLaneSlots = processingLaneSlots;
        }

        @Override
        public int getSlots() {
            return processingLaneSlots ? activeProcessingSlots() : 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(mappedSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            if (processingLaneSlots && hasMasteryBehavior("EVEN_FEED")) {
                return insertEvenly(stack, simulate);
            }
            return inventory.insertItem(mappedSlot(slot), stack, simulate);
        }

        /**
         * Even Feed ignores the offered lane: each item goes to the emptiest active lane that can take it, so a trickle
         * or a bulk shipment spreads across every lane instead of filling lane 1 first.
         */
        private ItemStack insertEvenly(ItemStack stack, boolean simulate) {
            int lanes = activeProcessingSlots();
            int[] counts = new int[lanes];
            int[] room = new int[lanes];
            for (int lane = 0; lane < lanes; lane++) {
                int slot = firstSlot + lane;
                ItemStack existing = inventory.getStackInSlot(slot);
                counts[lane] = existing.getCount();
                if (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack)) {
                    room[lane] = Math.max(0, Math.min(inventory.getSlotLimit(slot), stack.getMaxStackSize()) - existing.getCount());
                }
            }
            int[] added = new int[lanes];
            int remaining = stack.getCount();
            while (remaining > 0) {
                int best = -1;
                for (int lane = 0; lane < lanes; lane++) {
                    if (added[lane] < room[lane] && (best < 0 || counts[lane] + added[lane] < counts[best] + added[best])) {
                        best = lane;
                    }
                }
                if (best < 0) {
                    break;
                }
                added[best]++;
                remaining--;
            }
            if (!simulate) {
                for (int lane = 0; lane < lanes; lane++) {
                    if (added[lane] > 0) {
                        inventory.insertItem(firstSlot + lane, stack.copyWithCount(added[lane]), false);
                    }
                }
            }
            return remaining == stack.getCount() ? stack : stack.copyWithCount(remaining);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!allowExtract) {
                return ItemStack.EMPTY;
            }
            return inventory.extractItem(mappedSlot(slot), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(mappedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return allowInsert
                    && inventory.isItemValid(mappedSlot(slot), stack)
                    && (!processingLaneSlots || isSmeltable(stack));
        }

        private int mappedSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
            }
            return processingLaneSlots ? firstSlot + slot : firstSlot;
        }
    }

    private final class FurnaceEnergyStorage implements IEnergyStorage {
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
            if (!simulate && received > 0) {
                recordEnergyInput(received);
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
            if (!isElectric()) {
                return false;
            }
            if (internalEnergyStored() < internalEnergyCapacity()) {
                return true;
            }
            IEnergyStorage cell = batteryCellEnergyStorage();
            return cell != null && cell.canReceive() && cell.getEnergyStored() < cell.getMaxEnergyStored();
        }
    }
    @Override public MachineMasteryFamily masteryFamily() { return MachineMasteryFamily.FURNACE; }
    @Override public int ascendancyEntryStage() { return furnaceMaterial().stage(); }

    @Override public MachineProgressionState machineProgression() { return super.machineProgression().forFamily(masteryFamily()); }

    @Override public void masteryChanged() { resetAllCycles(); resetBulkSpeed(); clampInternalEnergy(); setChanged(); }

}
