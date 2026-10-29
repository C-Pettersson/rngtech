package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.MetalPressBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.GearParts;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.item.UniquePartItem;
import com.rngtech.content.material.MaterialCatalog;
import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.content.menu.MetalPressMenu;
import com.rngtech.content.recipe.MetalPressRecipe;
import com.rngtech.content.recipe.MetalPressRecipeInput;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
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
import com.rngtech.rpg.OutputLedger;
import com.rngtech.rpg.progression.AscendancyFormulas;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrderConditions;
import com.rngtech.rpg.workorder.WorkOrderFamilies;
import com.rngtech.rpg.workorder.WorkOrderHost;
import com.rngtech.rpg.workorder.WorkReport;

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
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class MetalPressBlockEntity extends BaseMachineBlockEntity
        implements MenuProvider, MachineMasteryHost, MachineInfoProvider, WorkOrderHost {
    private static final RecipeInputFilter<MetalPressRecipeInput, MetalPressRecipe> INPUT_FILTER = new RecipeInputFilter<>(
            ModRecipes.METAL_PRESS_TYPE::get,
            (recipe, item) -> recipe.ingredient().test(item));
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int PROCESS_SLOT_COUNT = 2;
    public static final int SLOT_HEAT_CORE = 0;
    public static final int SLOT_SERVO = 1;
    public static final int SLOT_MOLD = 2;
    public static final int MOLD_SLOT_COUNT = 5;
    public static final int SLOT_BATTERY_CELL = SLOT_MOLD + MOLD_SLOT_COUNT;
    public static final int GEAR_SLOT_COUNT = SLOT_BATTERY_CELL + 1;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_HEAT_CORE = 1;
    public static final int STATUS_MISSING_SERVO = 2;
    public static final int STATUS_MISSING_MOLD = 3;
    public static final int STATUS_NO_INPUT = 4;
    public static final int STATUS_INVALID_RECIPE = 5;
    public static final int STATUS_HEAT_LOW = 6;
    public static final int STATUS_OUTPUT_FULL = 7;
    public static final int STATUS_NO_POWER = 8;
    public static final int STATUS_POWER_DROP = 9;
    public static final int STATUS_HEAT_HIGH = 10;
    public static final int STATUS_STABILITY_LOW = 11;
    public static final int STATUS_WARMING = 12;
    public static final int STATUS_ROUTE_DISABLED = 13;
    public static final int STATUS_SWAPPING_MOLD = 14;
    public static final int STATUS_FILLING_BATCH = 15;

    private static final double NO_BATTERY_PROCESSING_SPEED = 0.85;
    private static final int NO_BATTERY_STABILITY_PENALTY = 20;
    private static final int FORMING_BED_TEMPERATURE = 800;
    private static final int FAILURE_STRAIN_THRESHOLD = 6000;
    private static final int POWER_DROP_STRAIN = 1400;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_HEAT = 4;
    private static final int DATA_MIN_TEMPERATURE = 5;
    private static final int DATA_TARGET_TEMPERATURE = 6;
    private static final int DATA_SAFE_MAX_TEMPERATURE = 7;
    private static final int DATA_OVERHEAT_TEMPERATURE = 8;
    private static final int DATA_FAILURE_RISK = 9;
    private static final int DATA_STATUS = 10;
    private static final int DATA_PROCESSING_SPEED = 11;
    private static final int DATA_EFFICIENCY = 12;
    private static final int DATA_ENERGY_USAGE = 13;
    private static final int DATA_ENERGY_CAPACITY_STAT = 14;
    private static final int DATA_ENERGY_TRANSFER = 15;
    private static final int DATA_HEAT_TRANSFER = 16;
    private static final int DATA_MAX_TEMPERATURE = 17;
    private static final int DATA_TEMPERATURE_STABILITY = 18;
    private static final int DATA_THERMAL_MASS = 19;
    private static final int DATA_COOLING_RATE = 20;
    private static final int DATA_OVERHEAT_TOLERANCE = 21;
    private static final int DATA_STABILITY = 22;
    private static final int DATA_REFINEMENT_POTENTIAL = 23;
    private static final int DATA_SELECTED_MOLD = 24;
    private static final int DATA_ENERGY_PER_TICK = 25;
    private static final int DATA_ENERGY_PER_CRAFT = 26;
    private static final int DATA_LEDGER = DATA_ENERGY_PER_CRAFT + 1;
    private static final int DATA_BATCH_SIZE = DATA_LEDGER + 1;
    private static final int DATA_HEAT_SLIDER = DATA_BATCH_SIZE + 1;
    private static final int DATA_SETPOINT = DATA_HEAT_SLIDER + 1;
    private static final int DATA_THERMAL_CONDUCTIVITY = DATA_SETPOINT + 1;
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
    private static final int DATA_HYDROGEN = DATA_NITROGEN_BONUS + 1;
    private static final int DATA_ANNEAL_MODE = DATA_HYDROGEN + 1;
    private static final int DATA_MACHINE_PROGRESSION_START = DATA_ANNEAL_MODE + 1;
    public static final int ANNEAL_NONE = 0;
    public static final int ANNEAL_CIRCUITS = 1;
    public static final int ANNEAL_PLATES = 2;
    private static final int DATA_OUTPUT_PER_MINUTE = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int DATA_COUNT = DATA_OUTPUT_PER_MINUTE + 1;
    private static final int STAT_SCALE = 100;
    private static final int LEGACY_SLOT_BATTERY_CELL = 3;
    private static final int VISUAL_SYNC_INTERVAL = 5;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT;
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
            if (isMoldSlot(slot)) {
                return isMetalPressMold(stack);
            }
            return switch (slot) {
                case SLOT_HEAT_CORE -> isHeatCore(stack);
                case SLOT_SERVO -> isServo(stack);
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
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
            if (slot != SLOT_BATTERY_CELL) {
                resetCycle();
                resetBulkSpeed();
            }
            clampInternalEnergy();
            setChanged();
        }
    };
    private final IItemHandler inputHandler = new ProcessItemHandler(SLOT_INPUT, true, false);
    private final IItemHandler outputHandler = new ProcessItemHandler(SLOT_OUTPUT, false, true);
    private final IEnergyStorage energyStorage = new PressEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final InstalledBatteryCell batteryCell = new InstalledBatteryCell(this::batteryCellStack);
    private final InertAtmosphere atmosphere = new InertAtmosphere(this::setChanged, () -> true, () -> annealMode() != ANNEAL_NONE);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_MACHINE_PROGRESSION_START && index < DATA_COUNT) {
                return MasteryMenuSupport.get(
                        MetalPressBlockEntity.this,
                        index - DATA_MACHINE_PROGRESSION_START,
                        MetalPressBlockEntity.this::effectiveStats
                );
            }
            MetalPressRecipe recipe = nextRecipe();
            MachineStatAccumulator stats = routeStats(recipe, effectiveStats());
            return switch (index) {
                case DATA_PROGRESS -> syncedProgress(0, currentElapsedTicks(recipe, stats));
                case DATA_PROCESSING_TICKS -> currentProcessingTicks(recipe, stats);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_HEAT -> heat.temperature();
                case DATA_MIN_TEMPERATURE -> currentMinimumTemperature(recipe, stats);
                case DATA_TARGET_TEMPERATURE -> currentTargetTemperature(recipe);
                case DATA_SAFE_MAX_TEMPERATURE -> currentSafeMaximumTemperature(recipe, stats);
                case DATA_OVERHEAT_TEMPERATURE -> currentOverheatTemperature(recipe, stats);
                case DATA_FAILURE_RISK -> HeatControl.failureProgress(failureStrain);
                case DATA_STATUS -> statusCode(recipe, stats);
                case DATA_NITROGEN -> atmosphere.amount();
                case DATA_NITROGEN_USE -> atmosphere.lastUse();
                case DATA_NITROGEN_BONUS -> (int) Math.round(atmosphereBonus() * 100.0);
                case DATA_HYDROGEN -> atmosphere.holdsHydrogen() ? 1 : 0;
                case DATA_ANNEAL_MODE -> annealMode();
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_HEAT_TRANSFER -> scaledStat(stats, MachineStat.HEAT_TRANSFER);
                case DATA_MAX_TEMPERATURE -> scaledStat(stats, MachineStat.MAX_TEMPERATURE);
                case DATA_TEMPERATURE_STABILITY -> scaledStat(stats, MachineStat.TEMPERATURE_STABILITY);
                case DATA_THERMAL_MASS -> scaledStat(stats, MachineStat.THERMAL_MASS);
                case DATA_COOLING_RATE -> scaledStat(stats, MachineStat.COOLING_RATE);
                case DATA_OVERHEAT_TOLERANCE -> scaledStat(stats, MachineStat.OVERHEAT_TOLERANCE);
                case DATA_STABILITY -> scaledStat(stats, MachineStat.STABILITY);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_BATCH_SIZE -> BatchProcessing.statBatchSize(stats) * STAT_SCALE;
                case DATA_SELECTED_MOLD -> selectedMold;
                case DATA_ENERGY_PER_TICK -> currentEnergyCostPerTick(stats);
                case DATA_ENERGY_PER_CRAFT -> recipe == null ? 0 : currentEnergyCostPerCraft(recipe, stats);
                case DATA_HEAT_SLIDER -> heatSlider;
                case DATA_THERMAL_CONDUCTIVITY -> scaledStat(stats, MachineStat.THERMAL_CONDUCTIVITY);
                case DATA_HOLDING_ENERGY -> heatEnergy(0);
                case DATA_INPUT_ENERGY -> heatEnergy(1);
                case DATA_HEATING_ENERGY -> heatEnergy(2);
                case DATA_HEAT_FACTOR -> heatFactorDisplay();
                case DATA_WOBBLE -> wobbleDisplay();
                case DATA_SETPOINT -> recipe == null ? 0 : envelope(recipe, stats).setpoint();
                case DATA_HEAT_REACH -> recipe == null ? 0 : envelope(recipe, stats).reach();
                case DATA_HEAT_MAXIMUM -> stats.intValue(MachineStat.MAX_TEMPERATURE);
                case DATA_LEDGER -> recipe == null || !ledgerActive(recipe, stats) ? -1 : LedgerNbt.permille(batchLedger.progress(recipe.outputStack().getItem()));
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
    private static final RecipeCache.Search<MetalPressRecipe> RECIPE_SEARCH = MetalPressRecipes::find;
    private final RecipeCache<MetalPressRecipe> recipes = new RecipeCache<>(MOLD_SLOT_COUNT);
    private final HeatLane heat = new HeatLane();
    /** Whether the press is cooling down to its work ceiling, and whether the heater went short of FE, as of the last tick. */
    private boolean tooHot;
    private boolean starved;
    /** How far past the recipe target the press runs, in percent of the band up to Max Temperature. */
    private int heatSlider;
    private int internalEnergy;
    private int lastMinimumTemperature;
    private int lastTargetTemperature;
    private int lastSafeMaximumTemperature;
    private int failureStrain;
    private int powerDropTicks;
    private int selectedMold;
    private ItemStack activeInput = ItemStack.EMPTY;
    private ItemStack activeMold = ItemStack.EMPTY;
    private final OutputLedger<Item> batchLedger = new OutputLedger<>();
    private int moldSwapTicks;
    private int lockedBatch;
    /** Finished cycles since load, for the ram's strike; not saved. */
    private int presses;
    private MetalPressVisualState visualState = MetalPressVisualState.IDLE;
    private MetalPressVisualState.Key lastSyncedVisual;
    private final MetalPressVisualState.Animation animation = new MetalPressVisualState.Animation();

    public MetalPressBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.METAL_PRESS.get(), pos, blockState, MachineType.METAL_PRESS, SLOT_INPUT, SLOT_INPUT, SLOT_OUTPUT);
        trackStatSlots(gearInventory);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MetalPressBlockEntity press) {
        press.tickWorkOrders(level);
        work(level, pos, state, press);
        press.syncVisualState(level);
    }

    private static void work(Level level, BlockPos pos, BlockState state, MetalPressBlockEntity press) {
        press.atmosphere.beginTick();
        MetalPressRecipe recipe = press.nextRecipe();
        MachineStatAccumulator stats = press.routeStats(recipe, press.effectiveStats());
        if (recipe == null || !press.hasRequiredComponents()) {
            press.escapement.idle();
            boolean swapping = press.rackMolds(stats);
            press.resetCycleIfActive();
            // Quick Change keeps the press hot while Mold Rack swaps.
            if (press.idle(stats, swapping && press.hasMasteryBehavior("QUICK_CHANGE"))) {
                press.setChanged();
            }
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }
        if (!press.activeCycleMatches()) {
            press.resetCycle();
            press.resetBulkSpeed();
        }
        press.moldSwapTicks = 0;
        press.escapement.observe(recipe);
        press.rememberHeatEnvelope(recipe, stats);

        // A blocked output keeps the forming bed at its setpoint, so pressing resumes as soon as there is room.
        if (press.effectiveHeat(stats) >= recipe.targetTemperature()
                && !press.crudePressBlocksUnstableRecipe(recipe, stats)
                && !press.canMergeOutput(press.pressedOutput(recipe))) {
            press.tooHot = false;
            press.starved = false;
            HeatEngine.hold(press.heat, press.envelope(recipe, stats), stats, HeatEngine.electric(press.heat, stats, press::drawEnergy));
            press.setChanged();
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }
        if (press.effectiveHeat(stats) < recipe.targetTemperature()
                || press.crudePressBlocksUnstableRecipe(recipe, stats)
                || press.waitsForFullBatch(recipe, stats)) {
            if (press.idle(stats, false)) {
                press.setChanged();
            }
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        HeatEngine.Envelope envelope = press.envelope(recipe, stats);
        HeatEngine.HeatSupply supply = HeatEngine.electric(press.heat, stats, press::drawEnergy);
        HeatLane heat = press.heat;
        if (!heat.started()
                && heat.exactTemperature() >= envelope.minimum()
                && heat.exactTemperature() <= envelope.ceilingFor(heat)
                && ProcessingChance.rollInstant(level, stats)
                && press.payInstant(recipe, stats, supply)) {
            press.markInstantProcess();
            press.startCycleIfNeeded(recipe, stats);
            press.decayPowerDrop();
            if (press.process(recipe, stats)) {
                press.bulkSpeed.recordProcess(press.activeTraits());
            }
            BaseMachineBlock.setActive(level, pos, state, true);
            press.setChanged();
            return;
        }

        boolean wasStarted = heat.started();
        HeatEngine.Tick tick = HeatEngine.work(heat, envelope, stats, press.load(recipe, stats), supply, level.getGameTime(), 0);
        press.tooHot = tick.tooHot();
        press.starved = tick.starved();
        BaseMachineBlock.setActive(level, pos, state, tick.working() || heat.lastHeat() > 0.0);
        press.setChanged();
        if (tick.tooHot() || !heat.started()) {
            return;
        }
        if (!wasStarted) {
            press.startCycleIfNeeded(recipe, stats);
        }
        if (tick.starved()) {
            if (!press.isCrudePress() && press.registerPowerDrop(recipe)) {
                press.tryFailCycle(recipe);
                return;
            }
        } else {
            press.decayPowerDrop();
        }
        press.updateFailureStrain(recipe, stats, envelope, tick.workTemperature());
        press.atmosphere.use(tick.workTemperature(), HeatEngine.overheatTemperature(envelope.safeMaximum(), stats), press.atmosphereBonus());
        if (!press.isCrudePress() && press.failureStrain >= FAILURE_STRAIN_THRESHOLD) {
            press.tryFailCycle(recipe);
            return;
        }
        if (HeatEngine.finished(heat, press.cycleWork(recipe, stats)) && press.process(recipe, stats)) {
            press.bulkSpeed.recordProcess(press.activeTraits());
        }
    }

    /** FE/t the heater spent last tick: on wall loss when {@code holding}, otherwise on heating the chamber and input. */
    private int heatEnergy(int part) {
        return (int) Math.ceil(HeatEngine.heatSplit(heat)[part] * HeatEngine.energyRate(effectiveStats()));
    }

    private int heatFactorDisplay() {
        MetalPressRecipe recipe = nextRecipe();
        MachineStatAccumulator stats = routeStats(recipe, effectiveStats());
        return recipe == null || !(hasRequiredComponents()) ? 0 : (int) Math.round(100.0 * HeatEngine.heatFactor(heat.exactTemperature(), envelope(recipe, stats)));
    }

    private int wobbleDisplay() {
        MetalPressRecipe recipe = nextRecipe();
        MachineStatAccumulator stats = routeStats(recipe, effectiveStats());
        return recipe == null || !(hasRequiredComponents()) ? 0 : (int) Math.round(HeatEngine.wobbleAmplitude(envelope(recipe, stats), stats));
    }

    /**
     * Presses saved while the block was a solid cube stored no light inside it, which leaves the renderer's die, ram, and
     * Gear unlit. Rechecking the block's own light once on load fixes those saves.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            level.getLightEngine().checkBlock(worldPosition);
        }
    }

    /** Client copy of the state the renderer draws; synced from the server in the block entity update tag. */
    public MetalPressVisualState visualState() {
        return visualState;
    }

    public MetalPressVisualState.Animation animation() {
        return animation;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return currentVisualState().save();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        visualState = MetalPressVisualState.load(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        visualState = MetalPressVisualState.load(packet.getTag());
    }

    private void syncVisualState(Level level) {
        if (level.getGameTime() % VISUAL_SYNC_INTERVAL != 0) {
            return;
        }
        MetalPressVisualState.Key key = currentVisualState().key();
        if (!key.equals(lastSyncedVisual)) {
            lastSyncedVisual = key;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** What the press shows right now, built on the server. */
    public MetalPressVisualState currentVisualState() {
        MetalPressRecipe recipe = nextRecipe();
        MachineStatAccumulator baseStats = effectiveStats();
        MachineStatAccumulator stats = routeStats(recipe, baseStats);
        boolean pressing = recipe != null && hasRequiredComponents();
        float progress = 0.0F;
        int batch = 1;
        if (pressing) {
            int ticks = currentProcessingTicks(recipe, stats);
            progress = ticks > 0 ? Math.min(1.0F, currentElapsedTicks(recipe, stats) / (float) ticks) : 0.0F;
            batch = Math.max(1, batchJobs(recipe, stats));
        }
        String[] rack = MetalPressVisualState.emptyRack();
        for (int index = 0; index < MOLD_SLOT_COUNT; index++) {
            if (index != selectedMold) {
                rack[index] = moldKey(gearInventory.getStackInSlot(SLOT_MOLD + index));
            }
        }
        ItemStack output = processInventory.getStackInSlot(SLOT_OUTPUT);
        ItemStack cell = batteryCellStack();
        boolean hasCell = isBatteryCell(cell);
        return new MetalPressVisualState(
                heat.temperature(),
                currentTargetTemperature(recipe),
                pressing ? envelope(recipe, stats).setpoint() : 0,
                currentOverheatTemperature(recipe, stats),
                hasHeatCore() ? Math.max(0, baseStats.intValue(MachineStat.MAX_TEMPERATURE)) : 0,
                wobbleDisplay(),
                getBlockState().getValue(BaseMachineBlock.ACTIVE),
                progress,
                presses & 0xFF,
                batch,
                inputStack().getItem(),
                output.getItem(),
                output.isEmpty() ? 0.0F : output.getCount() / (float) output.getMaxStackSize(),
                moldKey(moldStack()),
                rack,
                heatCoreKey(heatCoreStack()),
                hasServo() ? partKey(servoStack()) : "",
                hasCell ? ((BatteryCellItem) cell.getItem()).material().getSerializedName() : "",
                hasCell && energyCapacity() > 0 ? energyStored() / (float) energyCapacity() : -1.0F,
                atmosphere.amount() / (float) InertAtmosphere.CAPACITY,
                atmosphere.holdsHydrogen(),
                atmosphere.lastUse() > 0,
                isCrudePress() ? -1.0F : HeatControl.failureProgress(failureStrain) / 100.0F
        );
    }

    /** A Mold's kind, such as {@code plate} for the Plate Mold; empty for anything else. */
    private static String moldKey(ItemStack stack) {
        if (!isMetalPressMold(stack)) {
            return "";
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.endsWith("_mold") ? path.substring(0, path.length() - "_mold".length()) : "";
    }

    private static String heatCoreKey(ItemStack stack) {
        if (!isHeatCore(stack)) {
            return "";
        }
        if (stack.getItem() instanceof SolidFuelBurnerPartItem part && part.heatCoreMaterial() != null) {
            return part.heatCoreMaterial().getSerializedName();
        }
        return stack.getItem() instanceof UniquePartItem ? "unique" : "";
    }

    private static String partKey(ItemStack stack) {
        if (stack.getItem() instanceof ServoItem servo) {
            return servo.material().getSerializedName();
        }
        return stack.getItem() instanceof UniquePartItem ? "unique" : "";
    }

    public int heatSlider() {
        return heatSlider;
    }

    /** Sets how far past each recipe target the press runs, from 0 to 100 percent of the band up to Max Temperature. */
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

    public ContainerData getMenuData() {
        return menuData;
    }

    public int selectedMold() {
        return selectedMold;
    }

    public void selectMold(int moldIndex) {
        int clampedIndex = clampMoldIndex(moldIndex);
        if (selectedMold == clampedIndex) {
            return;
        }
        selectedMold = clampedIndex;
        resetCycle();
        resetBulkSpeed();
        setChanged();
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return trackedEnergyStorage;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return atmosphere.inlet();
    }

    /** Overheat Tolerance the Nitrogen tank adds, with Blast Draft only. */
    private double atmosphereBonus() {
        return atmosphere.overheatBonus(hasMasteryBehavior("BLAST_DRAFT"));
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == Direction.UP) {
            return inputHandler;
        }
        if (side == Direction.DOWN) {
            return outputHandler;
        }
        return side == null ? processInventory : null;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return processInventory;
    }

    /** Expected items per minute for the current cycle, in tenths. */
    private int currentOutputPerMinuteTenths(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        if (recipe == null) {
            return 0;
        }
        return OutputRate.perMinuteTenths(
                batchJobs(recipe, stats) * recipe.outputStack().getCount(),
                currentProcessingTicks(recipe, stats)
        );
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MetalPressRecipe recipe = nextRecipe();
        MachineStatAccumulator stats = routeStats(recipe, effectiveStats());
        int status = statusCode(recipe, stats);
        int energyDemand = energyDemand(status, recipe, stats);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("metal_press")
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY || status == STATUS_WARMING,
                                getBlockState().getValue(BaseMachineBlock.ACTIVE),
                                status == STATUS_NO_INPUT || status == STATUS_FILLING_BATCH
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(
                        statusKey(status),
                        status == STATUS_WARMING ? currentTargetTemperature(recipe) : currentMinimumTemperature(recipe, stats)
                )
                .progress(currentElapsedTicks(recipe, stats), currentProcessingTicks(recipe, stats))
                .outputRate(currentOutputPerMinuteTenths(recipe, stats))
                .energy(energyStored(), energyCapacity(), -energyDemand)
                .energyTelemetry(
                        energyFlow.lastInput(),
                        energyFlow.lastOutput(),
                        connector.transferRate(),
                        AdjacentEnergyConnector.inputBottleneck(connector, energyDemand)
                )
                .heat(heat.temperature(), currentTargetTemperature(recipe))
                .setpoint(recipe == null ? 0 : envelope(recipe, stats).setpoint())
                .batch(recipe == null ? 0 : batchJobs(recipe, stats))
                .gear(hasHeatCore()
                        ? MachineInfoSnapshot.GearSummary.HEAT_CORE_INSTALLED
                        : MachineInfoSnapshot.GearSummary.MISSING_HEAT_CORE)
                .output(status == STATUS_OUTPUT_FULL
                        ? MachineInfoSnapshot.OutputSummary.OUTPUT_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .refinement(machineTraits())
                .build();
    }

    private int energyDemand(int status, MetalPressRecipe recipe, MachineStatAccumulator stats) {
        if (recipe == null) {
            return 0;
        }
        return switch (status) {
            case STATUS_READY, STATUS_WARMING, STATUS_NO_POWER -> currentEnergyCostPerTick(stats);
            default -> 0;
        };
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_MISSING_HEAT_CORE -> "missing_heat_core";
            case STATUS_MISSING_SERVO -> "missing_servo";
            case STATUS_MISSING_MOLD -> "missing_mold";
            case STATUS_NO_INPUT -> "no_input";
            case STATUS_INVALID_RECIPE -> "invalid_recipe";
            case STATUS_HEAT_LOW -> "heat_low";
            case STATUS_HEAT_HIGH -> "heat_high";
            case STATUS_STABILITY_LOW -> "stability_low";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_NO_POWER -> "no_power";
            case STATUS_POWER_DROP -> "power_drop";
            case STATUS_WARMING -> "warming";
            case STATUS_ROUTE_DISABLED -> "route_disabled";
            case STATUS_SWAPPING_MOLD -> "swapping_mold";
            case STATUS_FILLING_BATCH -> "filling_batch";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.press.status." + name;
    }

    @Override
    public Component getDisplayName() {
        Component fallbackName = Component.translatable(isCrudePress()
                ? "container.rngtech.crude_metal_press"
                : "container.rngtech.metal_press");
        return MachineNameGenerator.generatedName(
                machineTraits(),
                fallbackName
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MetalPressMenu(containerId, playerInventory, this, menuData);
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
        MachineStatAccumulator stats = MachineBaseStatCatalog.metalPress();
        stats.apply(effectiveTraits());
        MegaPassiveTree.applyNodeStats(stats, machineProgression(), MachineMasteryFamily.METAL_PRESS);
        applyHeatCoreStats(stats);
        applyServoStats(stats);
        MegaPassiveTree.applyAttributeStats(stats, machineProgression(), MachineMasteryFamily.METAL_PRESS);
        WorkOrderConditions.apply(stats, workOrders().activeOrder());
        if (!hasBatteryCell()) {
            stats.apply(MachineStatAccumulator.NO_BATTERY_SOURCE, new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.PROCESSING_SPEED,
                    ModifierOperation.LESS,
                    NO_BATTERY_PROCESSING_SPEED
            ));
            stats.apply(MachineStatAccumulator.NO_BATTERY_SOURCE, new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.STABILITY,
                    ModifierOperation.DECREASED_PERCENT,
                    NO_BATTERY_STABILITY_PENALTY
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
        CompoundTag heatTag = new CompoundTag();
        heat.save(heatTag);
        tag.put("Heat", heatTag);
        tag.putInt("HeatTrack", heatSlider);
        tag.putInt("Energy", internalEnergyStored());
        tag.putInt("LastMinimumTemperature", lastMinimumTemperature);
        tag.putInt("LastTargetTemperature", lastTargetTemperature);
        tag.putInt("LastSafeMaximumTemperature", lastSafeMaximumTemperature);
        tag.putInt("FailureStrain", failureStrain);
        atmosphere.save(tag, registries);
        tag.putInt("PowerDropTicks", powerDropTicks);
        tag.putInt("SelectedMold", selectedMold);
        tag.put("ActiveInput", activeInput.saveOptional(registries));
        tag.put("ActiveMold", activeMold.saveOptional(registries));
        bulkSpeed.save(tag);
        LedgerNbt.save(tag, "BatchLedger", batchLedger);
        tag.putInt("MoldSwapTicks", moldSwapTicks);
        tag.putInt("LockedBatch", lockedBatch);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        CompoundTag gearTag = expandedGearInventoryTag(tag.getCompound("GearInventory"));
        boolean legacyGearLayout = !tag.contains("SelectedMold") && gearTag.getInt("Size") > LEGACY_SLOT_BATTERY_CELL;
        gearInventory.deserializeNBT(registries, gearTag);
        if (legacyGearLayout) {
            migrateLegacyBatteryCell();
        }
        if (tag.contains("Heat")) {
            heat.load(tag.getCompound("Heat"));
        } else {
            // Saved before the heat engine: the press keeps its heat, and an unfinished press starts over.
            heat.resetCycle();
            heat.setTemperature(tag.getInt("CurrentTemperature"));
        }
        heatSlider = Math.max(0, Math.min(100, tag.getInt("HeatTrack")));
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        lastMinimumTemperature = Math.max(0, tag.getInt("LastMinimumTemperature"));
        lastTargetTemperature = Math.max(0, tag.getInt("LastTargetTemperature"));
        lastSafeMaximumTemperature = Math.max(0, tag.getInt("LastSafeMaximumTemperature"));
        failureStrain = Math.max(0, tag.getInt("FailureStrain"));
        atmosphere.load(tag, registries);
        powerDropTicks = Math.max(0, tag.getInt("PowerDropTicks"));
        selectedMold = clampMoldIndex(tag.getInt("SelectedMold"));
        activeInput = ItemStack.parseOptional(registries, tag.getCompound("ActiveInput"));
        activeMold = ItemStack.parseOptional(registries, tag.getCompound("ActiveMold"));
        bulkSpeed.load(tag);
        LedgerNbt.load(tag, "BatchLedger", batchLedger);
        moldSwapTicks = Math.max(0, tag.getInt("MoldSwapTicks"));
        lockedBatch = Math.max(0, tag.getInt("LockedBatch"));
        clampInternalEnergy();
    }

    private static CompoundTag expandedGearInventoryTag(CompoundTag gearTag) {
        CompoundTag expandedTag = gearTag.copy();
        if (expandedTag.getInt("Size") < GEAR_SLOT_COUNT) {
            expandedTag.putInt("Size", GEAR_SLOT_COUNT);
        }
        return expandedTag;
    }

    private void migrateLegacyBatteryCell() {
        ItemStack legacyBattery = gearInventory.getStackInSlot(LEGACY_SLOT_BATTERY_CELL);
        if (isBatteryCell(legacyBattery) && gearInventory.getStackInSlot(SLOT_BATTERY_CELL).isEmpty()) {
            gearInventory.setStackInSlot(SLOT_BATTERY_CELL, legacyBattery.copy());
            gearInventory.setStackInSlot(LEGACY_SLOT_BATTERY_CELL, ItemStack.EMPTY);
        }
    }

    public static boolean isHeatCore(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.HEAT_CORE, 6);
    }

    public static boolean isServo(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.SERVO);
    }

    public static boolean isPlateMold(ItemStack stack) {
        return stack.is(ModItems.PLATE_MOLD.get());
    }

    public static boolean isCasingMold(ItemStack stack) {
        return stack.is(ModItems.CASING_MOLD.get());
    }

    public static boolean isMetalPressMold(ItemStack stack) {
        return stack.is(ModTags.Items.METAL_PRESS_MOLDS);
    }

    public static boolean isMoldSlot(int slot) {
        return slot >= SLOT_MOLD && slot < SLOT_MOLD + MOLD_SLOT_COUNT;
    }

    public static boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public static boolean isGearComponent(ItemStack stack) {
        return isHeatCore(stack) || isServo(stack) || isMetalPressMold(stack);
    }

    private MetalPressRecipe nextRecipe() {
        return level == null
                ? null
                : routeEnabled(moldRecipe(selectedMold));
    }

    /** How many recipe lookups searched the recipes instead of reusing a remembered result. */
    public int recipeSearches() {
        return recipes.searches();
    }

    /** The recipe for the input in one stored Mold, ignoring disabled routes; each Mold has its own lookup slot. */
    private MetalPressRecipe moldRecipe(int moldIndex) {
        return recipes.find(moldIndex, level, inputStack(), gearInventory.getStackInSlot(SLOT_MOLD + moldIndex), RECIPE_SEARCH);
    }

    private MetalPressRecipe routeEnabled(MetalPressRecipe recipe) {
        return recipe == null || routeDisabled(recipe) ? null : recipe;
    }

    private static boolean isCircuit(MetalPressRecipe recipe) {
        return moldMatches(recipe, ModTags.Items.CIRCUIT_MOLDS);
    }

    private static boolean isPlate(MetalPressRecipe recipe) {
        return moldMatches(recipe, ModTags.Items.PLATE_MOLDS);
    }

    private static boolean isGearOrCasing(MetalPressRecipe recipe) {
        return moldMatches(recipe, ModTags.Items.GEAR_MOLDS) || moldMatches(recipe, ModTags.Items.CASING_MOLDS);
    }

    private static boolean moldMatches(MetalPressRecipe recipe, net.minecraft.tags.TagKey<Item> tag) {
        for (ItemStack mold : recipe.mold().getItems()) {
            if (mold.is(tag)) {
                return true;
            }
        }
        return false;
    }

    /** Which pressed items this press anneals: Bright Anneal takes circuits, Hydrogen Quench takes plates. */
    private int annealMode() {
        return hasMasteryBehavior("BRIGHT_ANNEAL") ? ANNEAL_CIRCUITS : hasMasteryBehavior("HYDROGEN_QUENCH") ? ANNEAL_PLATES : ANNEAL_NONE;
    }

    /** Whether this recipe's output comes out Annealed while the tank holds enough Hydrogen for one item. */
    private boolean willAnneal(MetalPressRecipe recipe) {
        ItemStack output = recipe.outputStack();
        boolean anneals = switch (annealMode()) {
            case ANNEAL_CIRCUITS -> isCircuit(recipe) && output.is(ModTags.Items.ANNEALED_CALIBRATION_INPUTS);
            case ANNEAL_PLATES -> isPlate(recipe) && output.is(ModTags.Items.PLATES);
            default -> false;
        };
        return anneals && atmosphere.canAnneal(1);
    }

    /** The output this press makes next: Annealed when it will anneal it. */
    private ItemStack pressedOutput(MetalPressRecipe recipe) {
        return willAnneal(recipe) ? annealed(recipe.outputStack()) : recipe.outputStack();
    }

    private static ItemStack annealed(ItemStack stack) {
        stack.set(ModDataComponents.ANNEALED.get(), true);
        return stack;
    }

    /** Forge Line disables circuit recipes. */
    private boolean routeDisabled(MetalPressRecipe recipe) {
        return isCircuit(recipe) && hasMasteryBehavior("FORGE_LINE");
    }

    /** Circuit Discipline: circuit recipes gain Stability and use less FE. */
    private MachineStatAccumulator routeStats(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        if (recipe != null && isCircuit(recipe) && hasMasteryBehavior("CIRCUIT_DISCIPLINE")) {
            stats = stats.mutable();
            stats.apply(MegaPassiveTree.behaviorSource(machineProgression(), "CIRCUIT_DISCIPLINE"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.STABILITY, ModifierOperation.INCREASED_PERCENT, 20.0));
            stats.apply(MegaPassiveTree.behaviorSource(machineProgression(), "CIRCUIT_DISCIPLINE"), new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.ENERGY_USAGE, ModifierOperation.LESS, 0.85));
        }
        return stats;
    }

    /** Production Die speeds gears and casings and slows circuits; Hot Stamping speeds plates above their target. */
    private double routeSpeed(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        double speed = 1.0;
        if (hasMasteryBehavior("PRODUCTION_DIE")) {
            speed *= isGearOrCasing(recipe) ? 1.4 : isCircuit(recipe) ? 0.75 : 1.0;
        }
        if (hasMasteryBehavior("HOT_STAMPING") && isPlate(recipe) && heat.temperature() > recipe.targetTemperature()) {
            speed *= 1.2;
        }
        return speed;
    }

    /** The batch locked when the cycle started, shrunk only if its input has since gone. */
    private int batchJobs(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        int available = availableBatch(recipe, stats);
        return heat.started() && lockedBatch > 0 ? Math.min(lockedBatch, available) : available;
    }

    /** Dense Batching: an idle press waits until its stack and output room fit a full batch. */
    private boolean waitsForFullBatch(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return !heat.started() && hasMasteryBehavior("DENSE_BATCHING") && availableBatch(recipe, stats) < fullBatch(recipe, stats);
    }

    private int fullBatch(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        if (isCircuit(recipe) || !(isPlate(recipe) || isGearOrCasing(recipe))) {
            return 1;
        }
        return BatchProcessing.fullBatch(stats, false, inputStack().getMaxStackSize() / Math.max(1, recipe.inputCount()));
    }

    /** Plates, gears, and casings press one input set per batched item, as far as input and output allow. Circuits never batch. */
    private int availableBatch(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        if (isCircuit(recipe) || !(isPlate(recipe) || isGearOrCasing(recipe))) {
            return 1;
        }
        int limit = Math.min(BatchProcessing.batchSize(stats, false), inputStack().getCount() / Math.max(1, recipe.inputCount()));
        if (willAnneal(recipe)) {
            limit = Math.min(limit, atmosphere.amount() / InertAtmosphere.HYDROGEN_PER_ANNEAL);
        }
        ItemStack output = pressedOutput(recipe);
        return Math.max(1, BatchProcessing.largestFitting(limit, batch -> batch == 1 || canMergeOutput(output.copyWithCount(output.getCount() * batch))));
    }

    private boolean ledgerActive(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return recipe.allowsBonusOutput() && stats.value(MachineStat.LEDGER_RATE) > 0.0;
    }

    /**
     * Mold Rack: when the selected mold has no recipe for the input, count toward a swap to a stored mold that has one.
     * Returns whether a swap is under way.
     */
    private boolean rackMolds(MachineStatAccumulator stats) {
        if (!hasMasteryBehavior("MOLD_RACK") || level == null || inputStack().isEmpty()) {
            moldSwapTicks = 0;
            return false;
        }
        int match = -1;
        for (int index = 0; index < MOLD_SLOT_COUNT && match < 0; index++) {
            ItemStack mold = gearInventory.getStackInSlot(SLOT_MOLD + index);
            if (index != selectedMold && isMetalPressMold(mold) && routeEnabled(moldRecipe(index)) != null) {
                match = index;
            }
        }
        if (match < 0) {
            moldSwapTicks = 0;
            return false;
        }
        if (++moldSwapTicks >= stats.intValue(MachineStat.MOLD_SWAP_TIME)) {
            moldSwapTicks = 0;
            selectMold(match);
        }
        setChanged();
        return true;
    }

    private int minimumTemperature(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return AscendancyFormulas.windowEdge(recipe.targetTemperature(), recipe.minimumTemperature(), stats);
    }

    private int safeMaximumTemperature(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return AscendancyFormulas.windowEdge(recipe.targetTemperature(), recipe.safeMaximumTemperature(), stats);
    }

    private boolean process(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        if (level == null || !recipe.matches(new MetalPressRecipeInput(inputStack(), moldStack()), level)) {
            return false;
        }

        int jobs = batchJobs(recipe, stats);
        boolean annealed = willAnneal(recipe) && atmosphere.canAnneal(jobs);
        ItemStack baseResult = annealed ? annealed(recipe.outputStack()) : recipe.outputStack();
        ItemStack batch = baseResult.copyWithCount(baseResult.getCount() * jobs);
        if (!canMergeOutput(batch)) {
            return false;
        }
        ItemStack result = batch;
        for (int job = 0; job < jobs; job++) {
            if (ProcessingChance.rollSuperOutput(level, stats, recipe, baseResult)) {
                ItemStack grown = ProcessingChance.grow(result, baseResult, baseResult.getCount());
                result = canMergeOutput(grown) ? grown : result;
            }
        }
        result = withBatchLedger(recipe, stats, result, baseResult, jobs);

        consumeInput(recipe.inputCount() * jobs);
        if (annealed) {
            atmosphere.anneal(jobs);
        }
        mergeOutput(result);
        for (int job = 0; job < jobs; job++) {
            grantRecipeXp(recipe);
        }
        String mold = BuiltInRegistries.ITEM.getKey(moldStack().getItem()).getPath();
        workOrderWork(new WorkReport(WorkObjectiveType.PRESS, Math.max(0, MaterialCatalog.materialStage(recipe.failureMaterial())), heatSlider,
                BuiltInRegistries.ITEM.getKey(baseResult.getItem()).toString(), jobs, mold.endsWith("_mold") ? mold.substring(0, mold.length() - 5) : "")
                .withTicks(recipe.processingTicks()));
        escapement.completed();
        completeCycle();
        presses++;
        setChanged();
        return true;
    }

    /** The Batch Ledger banks a share of every eligible job and pays whole items once the output has room. */
    private ItemStack withBatchLedger(MetalPressRecipe recipe, MachineStatAccumulator stats, ItemStack result, ItemStack base, int jobs) {
        if (!ledgerActive(recipe, stats)) {
            return result;
        }
        batchLedger.add(base.getItem(), AscendancyFormulas.ledgerShare(stats, base.getCount(), false) * jobs);
        int payable = batchLedger.payable(base.getItem());
        ItemStack paid = ProcessingChance.grow(result, base, payable);
        if (paid == result || !canMergeOutput(paid)) {
            return result;
        }
        batchLedger.pay(base.getItem(), payable);
        return paid;
    }

    private void grantRecipeXp(MetalPressRecipe recipe) {
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
    public MachineMasteryFamily masteryFamily() {
        return MachineMasteryFamily.METAL_PRESS;
    }

    @Override
    public double masteryXpGain() {
        return MachineMasteryHost.xpGain(effectiveStats());
    }

    /** The Crude Metal Press is the Stage 3 bootstrap body; the Steel Metal Press is Stage 4. */
    @Override
    public int ascendancyEntryStage() {
        return isCrudePress() ? 3 : 4;
    }

    @Override public String workOrderFamily() { return WorkOrderFamilies.of(masteryFamily()); }
    @Override public int workOrderStage() { return ascendancyEntryStage(); }
    @Override public int workOrderMasteryLevel() { return masteryState().level(); }
    @Override public String workOrderAscendancy() { return masteryState().ascendancy(); }
    @Override public double workOrderStat(String stat) {
        MachineStat value = MachineStat.bySerializedName(stat);
        return value == null ? 0 : effectiveStats().value(value);
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

    @Override
    public boolean mutesMachineSound() {
        return MegaPassiveTree.has(machineProgression(), "MUTE_MACHINE_SOUND");
    }

    private boolean tryFailCycle(MetalPressRecipe recipe) {
        if (isCrudePress()) {
            return false;
        }
        if (level == null || !recipe.matches(new MetalPressRecipeInput(inputStack(), moldStack()), level)) {
            return false;
        }

        // A failed batch ruins every job's input, unless Split Failure limits it to one job; the rest waits for the next cycle.
        int jobs = hasMasteryBehavior("SPLIT_FAILURE") ? 1 : batchJobs(recipe, effectiveStats());
        ItemStack failure = recipe.failureStack();
        ItemStack failed = failure.copyWithCount(failure.getCount() * jobs);
        if (!canMergeOutput(failed)) {
            return false;
        }

        consumeInput(recipe.inputCount() * jobs);
        mergeOutput(failed);
        presses++;
        resetCycle();
        resetBulkSpeed();
        workOrderStreakBroken();
        workOrderSetback(WorkOrderConditions.BRITTLE_LINING);
        setChanged();
        return true;
    }

    private void consumeInput(int count) {
        ItemStack input = inputStack();
        int consumed = Math.min(count, input.getCount());
        ItemStack remainder = input.getCraftingRemainingItem();
        input.shrink(consumed);
        for (int index = 0; index < consumed && !remainder.isEmpty(); index++) {
            ItemStack remainderCopy = remainder.copy();
            if (input.isEmpty()) {
                processInventory.setStackInSlot(SLOT_INPUT, remainderCopy);
                input = inputStack();
            } else if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainderCopy);
            }
        }
    }

    private boolean canMergeOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = processInventory.getStackInSlot(SLOT_OUTPUT);
        int slotLimit = Math.min(result.getMaxStackSize(), processInventory.getSlotLimit(SLOT_OUTPUT));
        if (output.isEmpty()) {
            return result.getCount() <= slotLimit;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= slotLimit;
    }

    private void mergeOutput(ItemStack result) {
        ItemStack output = processInventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            processInventory.setStackInSlot(SLOT_OUTPUT, result.copy());
            return;
        }
        ItemStack merged = output.copy();
        merged.grow(result.getCount());
        processInventory.setStackInSlot(SLOT_OUTPUT, merged);
    }

    /** The press's heat band: Heat Window moves the minimum and the safe maximum; Master Die ignores stability wobble. */
    private HeatEngine.Envelope envelope(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        double requiredStability = hasMasteryBehavior("MASTER_DIE") ? 0.0 : recipe.requiredTemperatureStability();
        return HeatEngine.envelope(
                minimumTemperature(recipe, stats),
                recipe.targetTemperature(),
                safeMaximumTemperature(recipe, stats),
                requiredStability,
                failureEnabled(recipe),
                hasMasteryBehavior("BLAST_DRAFT"),
                stats,
                heatSlider / 100.0,
                Integer.MAX_VALUE,
                atmosphereBonus()
        ).coolingFirst(hasMasteryBehavior("COOL_HEADED"));
    }

    /** Reference ticks a cycle needs: the recipe's time, longer for every extra item in the batch. */
    private double cycleWork(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return recipe.processingTicks() * BatchProcessing.timeMultiplier(stats, batchJobs(recipe, stats));
    }

    private HeatEngine.Load load(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        int jobs = batchJobs(recipe, stats);
        double workPerTick = HeatEngine.speed(stats) * routeSpeed(recipe, stats) * escapementSpeed(stats);
        return new HeatEngine.Load(recipe.heat() * (double) jobs * workPerTick / cycleWork(recipe, stats), workPerTick, jobs);
    }

    private int drawEnergy(int amount) {
        int take = Math.min(amount, consumeWorkingEnergy(amount, true));
        return take > 0 ? consumeWorkingEnergy(take, false) : 0;
    }

    /** Instant Process pays the whole batch's heat at once, without the wall loss of a full cycle. */
    private boolean payInstant(MetalPressRecipe recipe, MachineStatAccumulator stats, HeatEngine.HeatSupply supply) {
        double needed = recipe.heat() * (double) batchJobs(recipe, stats);
        int energyCost = (int) Math.ceil(needed * HeatEngine.energyRate(stats));
        return consumeWorkingEnergy(energyCost, true) >= energyCost && HeatEngine.payAtOnce(supply, needed);
    }

    private double escapementSpeed(MachineStatAccumulator stats) {
        return escapement.speed(MachineImplicitCatalog.hasBehavior(servoStack(), MachineBehavior.ESCAPEMENT), stats);
    }


    /** FE the heater drew last tick. */
    private int currentEnergyCostPerTick(MachineStatAccumulator stats) {
        return (int) Math.ceil(heat.lastHeat() * HeatEngine.energyRate(stats));
    }

    /** FE one item costs at the setpoint: its own heat plus its share of the wall loss during the cycle. */
    private int currentEnergyCostPerCraft(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        int jobs = batchJobs(recipe, stats);
        return HeatEngine.steadyEnergyPerCycle(envelope(recipe, stats), load(recipe, stats), cycleWork(recipe, stats), stats) / Math.max(1, jobs);
    }

    private int currentProcessingTicks(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null ? 0 : HeatEngine.cycleTicks(cycleWork(recipe, stats), load(recipe, stats), envelope(recipe, stats));
    }

    private int currentElapsedTicks(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null ? 0 : HeatEngine.elapsedTicks(heat, load(recipe, stats), envelope(recipe, stats));
    }

    private int currentMinimumTemperature(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null ? hasCooldownHeat() ? lastMinimumTemperature : 0 : minimumTemperature(recipe, stats);
    }

    private int currentTargetTemperature(MetalPressRecipe recipe) {
        return recipe == null ? hasCooldownHeat() ? lastTargetTemperature : 0 : recipe.targetTemperature();
    }

    private int currentSafeMaximumTemperature(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null ? hasCooldownHeat() ? lastSafeMaximumTemperature : 0 : safeMaximumTemperature(recipe, stats);
    }

    private int currentOverheatTemperature(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        int safeMaximumTemperature = currentSafeMaximumTemperature(recipe, stats);
        return safeMaximumTemperature <= 0
                ? 0
                : HeatControl.effectiveOverheatTemperature(safeMaximumTemperature, stats, atmosphereBonus());
    }

    private int effectiveSafeMaximumTemperature(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return HeatControl.effectiveOverheatTemperature(safeMaximumTemperature(recipe, stats), stats);
    }

    private int statusCode(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        if (!hasHeatCore()) {
            return STATUS_MISSING_HEAT_CORE;
        }
        if (!isCrudePress() && !hasServo()) {
            return STATUS_MISSING_SERVO;
        }
        if (moldSwapTicks > 0) {
            return STATUS_SWAPPING_MOLD;
        }
        if (!hasMold()) {
            return STATUS_MISSING_MOLD;
        }
        if (inputStack().isEmpty()) {
            return STATUS_NO_INPUT;
        }
        if (recipe == null) {
            return level != null && moldRecipe(selectedMold) != null ? STATUS_ROUTE_DISABLED : STATUS_INVALID_RECIPE;
        }
        if (tooHot) {
            return STATUS_HEAT_HIGH;
        }
        if (effectiveHeat(stats) < recipe.targetTemperature()) {
            return STATUS_HEAT_LOW;
        }
        if (isCrudePress() && stats.value(MachineStat.TEMPERATURE_STABILITY) < recipe.requiredTemperatureStability()) {
            return STATUS_STABILITY_LOW;
        }
        if (!canMergeOutput(pressedOutput(recipe))
                || (!isCrudePress()
                        && failureStrain >= FAILURE_STRAIN_THRESHOLD
                        && !canMergeOutput(recipe.failureStack()))) {
            return STATUS_OUTPUT_FULL;
        }
        if (waitsForFullBatch(recipe, stats)) {
            return inputStack().getCount() / Math.max(1, recipe.inputCount()) < fullBatch(recipe, stats)
                    ? STATUS_FILLING_BATCH
                    : STATUS_OUTPUT_FULL;
        }
        if (powerDropTicks > 0) {
            return STATUS_POWER_DROP;
        }
        if (starved) {
            return STATUS_NO_POWER;
        }
        return heat.started() ? STATUS_READY : STATUS_WARMING;
    }

    private boolean crudePressBlocksUnstableRecipe(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return isCrudePress() && stats.value(MachineStat.TEMPERATURE_STABILITY) < recipe.requiredTemperatureStability();
    }

    /** An idle press leaks heat unless something holds it. */
    private boolean idle(MachineStatAccumulator stats, boolean holdHeat) {
        tooHot = false;
        starved = false;
        boolean changed = HeatEngine.idle(heat, stats, holdHeat);
        if (heat.temperature() <= HeatEngine.AMBIENT) {
            changed |= clearHeatEnvelope();
        }
        return changed;
    }

    private void rememberHeatEnvelope(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        lastMinimumTemperature = minimumTemperature(recipe, stats);
        lastTargetTemperature = recipe.targetTemperature();
        lastSafeMaximumTemperature = safeMaximumTemperature(recipe, stats);
    }

    private boolean clearHeatEnvelope() {
        boolean changed = lastMinimumTemperature != 0 || lastTargetTemperature != 0 || lastSafeMaximumTemperature != 0;
        lastMinimumTemperature = 0;
        lastTargetTemperature = 0;
        lastSafeMaximumTemperature = 0;
        return changed;
    }

    private boolean hasCooldownHeat() {
        return heat.temperature() > HeatEngine.AMBIENT && lastSafeMaximumTemperature > 0;
    }

    private void updateFailureStrain(
            MetalPressRecipe recipe,
            MachineStatAccumulator stats,
            HeatEngine.Envelope envelope,
            double workTemperature
    ) {
        if (!envelope.canFail()) {
            failureStrain = 0;
            return;
        }

        // Master Die: only heat outside the window adds strain, measured without the stability wobble.
        boolean masterDie = hasMasteryBehavior("MASTER_DIE");
        double requiredStability = masterDie ? 0.0 : recipe.requiredTemperatureStability();
        int addedStrain = HeatControl.failureStrainFromTemperature(
                (int) Math.round(workTemperature),
                envelope.minimum(),
                envelope.overheat(),
                requiredStability,
                stats
        );
        addedStrain = HeatControl.paced(addedStrain, workTemperature, envelope);
        if (hasMasteryBehavior("TOLERANCE_MAP")) {
            addedStrain = (int) Math.round(addedStrain * 0.6);
        }
        if (hasMasteryBehavior("QUENCH_PROTOCOL")) {
            addedStrain = HeatControl.quenched(addedStrain);
        }
        double servoInstability = masterDie ? 0.0 : Math.max(0.0, 1.0 - stats.value(MachineStat.STABILITY));
        addedStrain += Math.max(0, (int) Math.round(servoInstability * 50.0));
        if (addedStrain > 0) {
            failureStrain = Math.min(FAILURE_STRAIN_THRESHOLD, failureStrain + addedStrain);
            return;
        }
        failureStrain = Math.max(0, failureStrain - HeatControl.strainRecovery(stats));
    }

    private boolean failureEnabled(MetalPressRecipe recipe) {
        return !isCrudePress() && recipe.hasFailureOutput();
    }

    private boolean registerPowerDrop(MetalPressRecipe recipe) {
        if (!recipe.powerSensitive()) {
            return false;
        }
        powerDropTicks++;
        int strain = hasPowerGrace() ? POWER_DROP_STRAIN / 2 : POWER_DROP_STRAIN;
        // Servo Sync, and Shock Absorbers while batching, double the grace: each halves the strain again.
        if (hasMasteryBehavior("SERVO_SYNC")) {
            strain /= 2;
        }
        MetalPressRecipe current = nextRecipe();
        if (hasMasteryBehavior("SHOCK_ABSORBERS") && current != null && batchJobs(current, effectiveStats()) > 1) {
            strain /= 2;
        }
        failureStrain += strain;
        setChanged();
        return failureStrain >= FAILURE_STRAIN_THRESHOLD;
    }

    private void decayPowerDrop() {
        if (powerDropTicks > 0) {
            powerDropTicks--;
        }
    }

    private void startCycleIfNeeded(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        if (lockedBatch != 0) {
            return;
        }
        lockedBatch = availableBatch(recipe, stats);
        activeInput = singleCopy(inputStack());
        activeMold = singleCopy(moldStack());
        powerDropTicks = 0;
        if (bulkSpeed.startRecipe(recipe)) {
            setChanged();
        }
    }

    private boolean activeCycleMatches() {
        if (!heat.started()) {
            return true;
        }
        return !activeInput.isEmpty()
                && !activeMold.isEmpty()
                && ItemStack.isSameItemSameComponents(activeInput, inputStack())
                && ItemStack.isSameItemSameComponents(activeMold, moldStack());
    }

    private final EscapementState escapement = new EscapementState();

    private void resetCycleIfActive() {
        if (heat.started()
                || heat.work() > 0.0
                || failureStrain != 0
                || powerDropTicks != 0
                || !activeInput.isEmpty()
                || !activeMold.isEmpty()) {
            resetCycle();
            resetBulkSpeed();
            setChanged();
        }
    }

    private void resetCycle() {
        heat.resetCycle();
        lockedBatch = 0;
        failureStrain = 0;
        powerDropTicks = 0;
        activeInput = ItemStack.EMPTY;
        activeMold = ItemStack.EMPTY;
    }

    /** A finished cycle keeps its strain: only time in the safe band drains it, so short cycles cannot dodge it. */
    private void completeCycle() {
        int strain = failureStrain;
        resetCycle();
        failureStrain = strain;
    }

    private void resetBulkSpeed() {
        if (bulkSpeed.reset()) {
            setChanged();
        }
    }

    private int effectiveHeat(MachineStatAccumulator stats) {
        return hasHeatCore() ? Math.max(0, stats.intValue(MachineStat.MAX_TEMPERATURE)) : 0;
    }

    private void applyHeatCoreStats(MachineStatAccumulator stats) {
        ItemStack stack = heatCoreStack();
        if (!isHeatCore(stack)) {
            return;
        }
        stats.apply(stack.getHoverName(), new MachineModifier(
                ModifierSlot.IMPLICIT,
                MachineStat.MAX_TEMPERATURE,
                ModifierOperation.ADD,
                FORMING_BED_TEMPERATURE
        ));
        ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        ComponentBaseStatCatalog.applyAttributeAffixes(stats, stack);
    }

    private void applyServoStats(MachineStatAccumulator stats) {
        ItemStack stack = servoStack();
        if (isServo(stack) && stack.getItem() instanceof MachinePartItem part) {
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

    private boolean hasRequiredComponents() {
        return hasHeatCore() && (isCrudePress() || hasServo()) && hasMold();
    }

    public boolean isCrudePress() {
        return getBlockState().getBlock() instanceof MetalPressBlock press && press.isCrude();
    }

    private boolean hasHeatCore() {
        return isHeatCore(heatCoreStack());
    }

    private boolean hasServo() {
        return isServo(servoStack());
    }

    private boolean hasMold() {
        return isMetalPressMold(moldStack());
    }

    private boolean hasBatteryCell() {
        return isBatteryCell(batteryCellStack());
    }

    private boolean hasPowerGrace() {
        return activeTraits().hasBehavior(MachineBehavior.POWER_GRACE)
                || MachineImplicitCatalog.hasBehavior(heatCoreStack(), MachineBehavior.POWER_GRACE)
                || MachineImplicitCatalog.hasBehavior(servoStack(), MachineBehavior.POWER_GRACE);
    }

    private ItemStack inputStack() {
        return processInventory.getStackInSlot(SLOT_INPUT);
    }

    private ItemStack heatCoreStack() {
        return gearInventory.getStackInSlot(SLOT_HEAT_CORE);
    }

    private ItemStack servoStack() {
        return gearInventory.getStackInSlot(SLOT_SERVO);
    }

    private ItemStack moldStack() {
        return gearInventory.getStackInSlot(activeMoldSlot());
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private IEnergyStorage batteryCellEnergyStorage() {
        return batteryCell.storage();
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
    }

    private int activeMoldSlot() {
        return SLOT_MOLD + selectedMold;
    }

    private static int clampMoldIndex(int moldIndex) {
        return Math.max(0, Math.min(MOLD_SLOT_COUNT - 1, moldIndex));
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

    /** Any mold's recipe counts, since the press switches to whichever installed mold fits the input. */
    public boolean isPressInput(ItemStack stack) {
        return INPUT_FILTER.accepts(level, stack);
    }

    private final class ProcessItemHandler implements IItemHandler {
        private final int slot;
        private final boolean allowInsert;
        private final boolean allowExtract;

        private ProcessItemHandler(int slot, boolean allowInsert, boolean allowExtract) {
            this.slot = slot;
            this.allowInsert = allowInsert;
            this.allowExtract = allowExtract;
        }

        @Override
        public int getSlots() {
            return 1;
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
            return allowInsert
                    && processInventory.isItemValid(mappedSlot(slot), stack)
                    && (mappedSlot(slot) != SLOT_INPUT || isPressInput(stack));
        }

        private int mappedSlot(int slot) {
            if (slot != 0) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
            }
            return this.slot;
        }
    }

    private final class PressEnergyStorage implements IEnergyStorage {
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
