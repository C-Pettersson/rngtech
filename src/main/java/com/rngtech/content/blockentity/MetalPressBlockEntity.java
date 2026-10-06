package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.MetalPressBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.content.menu.MetalPressMenu;
import com.rngtech.content.recipe.MetalPressRecipe;
import com.rngtech.content.recipe.MetalPressRecipeInput;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModItems;
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
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class MetalPressBlockEntity extends BaseMachineBlockEntity
        implements MenuProvider, MachineMasteryHost, MachineInfoProvider {
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
    private static final int DATA_WARMUP_TIME = 19;
    private static final int DATA_COOLING_RATE = 20;
    private static final int DATA_OVERHEAT_TOLERANCE = 21;
    private static final int DATA_STABILITY = 22;
    private static final int DATA_REFINEMENT_POTENTIAL = 23;
    private static final int DATA_SELECTED_MOLD = 24;
    private static final int DATA_ENERGY_PER_TICK = 25;
    private static final int DATA_ENERGY_PER_CRAFT = 26;
    private static final int DATA_LEDGER = DATA_ENERGY_PER_CRAFT + 1;
    private static final int DATA_BATCH_SIZE = DATA_LEDGER + 1;
    private static final int DATA_MACHINE_PROGRESSION_START = DATA_BATCH_SIZE + 1;
    private static final int DATA_COUNT = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int STAT_SCALE = 100;
    private static final int LEGACY_SLOT_BATTERY_CELL = 3;

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
            if (slot == SLOT_INPUT) {
                resetCycle();
                resetBulkSpeed();
            }
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
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> currentProcessingTicks(recipe, stats);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_HEAT -> currentTemperature;
                case DATA_MIN_TEMPERATURE -> currentMinimumTemperature(recipe, stats);
                case DATA_TARGET_TEMPERATURE -> currentTargetTemperature(recipe);
                case DATA_SAFE_MAX_TEMPERATURE -> currentSafeMaximumTemperature(recipe, stats);
                case DATA_OVERHEAT_TEMPERATURE -> currentOverheatTemperature(recipe, stats);
                case DATA_FAILURE_RISK -> HeatControl.failureProgress(failureStrain);
                case DATA_STATUS -> statusCode(recipe, stats);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_HEAT_TRANSFER -> scaledStat(stats, MachineStat.HEAT_TRANSFER);
                case DATA_MAX_TEMPERATURE -> scaledStat(stats, MachineStat.MAX_TEMPERATURE);
                case DATA_TEMPERATURE_STABILITY -> scaledStat(stats, MachineStat.TEMPERATURE_STABILITY);
                case DATA_WARMUP_TIME -> scaledStat(stats, MachineStat.WARMUP_TIME);
                case DATA_COOLING_RATE -> scaledStat(stats, MachineStat.COOLING_RATE);
                case DATA_OVERHEAT_TOLERANCE -> scaledStat(stats, MachineStat.OVERHEAT_TOLERANCE);
                case DATA_STABILITY -> scaledStat(stats, MachineStat.STABILITY);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_BATCH_SIZE -> BatchProcessing.statBatchSize(stats) * STAT_SCALE;
                case DATA_SELECTED_MOLD -> selectedMold;
                case DATA_ENERGY_PER_TICK -> recipe == null ? 0 : energyCostPerTick(recipe, stats);
                case DATA_ENERGY_PER_CRAFT -> recipe == null ? 0 : energyCostPerCraft(recipe, stats);
                case DATA_LEDGER -> recipe == null || !ledgerActive(recipe, stats) ? -1 : LedgerNbt.permille(batchLedger.progress(recipe.outputStack().getItem()));
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
    private int progress;
    private int internalEnergy;
    private int currentTemperature;
    private int lastMinimumTemperature;
    private int lastTargetTemperature;
    private int lastSafeMaximumTemperature;
    private int failureStrain;
    private int powerDropTicks;
    private boolean targetReached;
    private int selectedMold;
    private ItemStack activeInput = ItemStack.EMPTY;
    private ItemStack activeMold = ItemStack.EMPTY;
    private final OutputLedger<Item> batchLedger = new OutputLedger<>();
    private int moldSwapTicks;
    private int lockedBatch;

    public MetalPressBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.METAL_PRESS.get(), pos, blockState, MachineType.METAL_PRESS, SLOT_INPUT, SLOT_INPUT, SLOT_OUTPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MetalPressBlockEntity press) {
        MetalPressRecipe recipe = press.nextRecipe();
        MachineStatAccumulator stats = press.routeStats(recipe, press.effectiveStats());
        if (recipe == null || !press.hasRequiredComponents()) {
            boolean swapping = press.rackMolds(stats);
            press.resetCycleIfActive();
            // Quick Change keeps the press hot while Mold Rack swaps.
            if (!(swapping && press.hasMasteryBehavior("QUICK_CHANGE")) && press.cool(stats)) {
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
        press.rememberHeatEnvelope(recipe, stats);

        if (press.effectiveHeat(stats) < recipe.targetTemperature()
                || press.crudePressBlocksUnsafeRecipe(recipe, stats)
                || !press.canMergeOutput(recipe.outputStack())) {
            if (press.cool(stats)) {
                press.setChanged();
            }
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        int energyCost = press.energyCostPerTick(recipe, stats);
        if (press.progress == 0 && press.currentTemperature < recipe.targetTemperature()) {
            press.targetReached = false;
        }
        int requiredTemperature = press.requiredTemperatureForProgress(recipe, stats);
        if (press.currentTemperature < requiredTemperature) {
            if (press.consumeWorkingEnergy(energyCost, true) >= energyCost) {
                press.consumeWorkingEnergy(energyCost, false);
                if (press.warm(recipe, stats, requiredTemperature)) {
                    press.setChanged();
                }
                BaseMachineBlock.setActive(level, pos, state, true);
            } else {
                if (!press.isCrudePress() && press.progress > 0 && press.registerPowerDrop(recipe)) {
                    press.tryFailCycle(recipe);
                }
                if (press.cool(stats)) {
                    press.setChanged();
                }
                BaseMachineBlock.setActive(level, pos, state, false);
            }
            return;
        }
        press.targetReached = true;

        if (press.progress == 0 && ProcessingChance.rollInstant(level, stats)) {
            int fullEnergyCost = press.energyCostPerCraft(recipe, stats) * press.batchJobs(recipe, stats);
            if (press.consumeWorkingEnergy(fullEnergyCost, true) >= fullEnergyCost) {
                press.startCycleIfNeeded(recipe, stats);
                press.consumeWorkingEnergy(fullEnergyCost, false);
                press.decayPowerDrop();
                if (press.process(recipe, stats)) {
                    press.bulkSpeed.recordProcess(press.activeTraits());
                }
                BaseMachineBlock.setActive(level, pos, state, true);
                press.setChanged();
                return;
            }
        }
        // Each batched job pays its share of the FE.
        energyCost *= press.batchJobs(recipe, stats);
        if (press.consumeWorkingEnergy(energyCost, true) < energyCost) {
            if (!press.isCrudePress() && press.progress > 0 && press.registerPowerDrop(recipe)) {
                press.tryFailCycle(recipe);
            }
            if (press.cool(stats)) {
                press.setChanged();
            }
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        press.startCycleIfNeeded(recipe, stats);
        press.consumeWorkingEnergy(energyCost, false);
        press.decayPowerDrop();
        press.progress++;
        press.updateFailureStrain(recipe, stats);

        if (!press.isCrudePress() && press.failureStrain >= FAILURE_STRAIN_THRESHOLD) {
            if (press.tryFailCycle(recipe)) {
                BaseMachineBlock.setActive(level, pos, state, false);
            } else {
                BaseMachineBlock.setActive(level, pos, state, false);
            }
            press.setChanged();
            return;
        }

        if (press.progress >= press.processingTicks(recipe, stats)) {
            if (press.process(recipe, stats)) {
                press.bulkSpeed.recordProcess(press.activeTraits());
            }
        }

        BaseMachineBlock.setActive(level, pos, state, true);
        press.setChanged();
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

    @Override
    public MachineInfoSnapshot machineInfo() {
        MetalPressRecipe recipe = nextRecipe();
        MachineStatAccumulator stats = routeStats(recipe, effectiveStats());
        int status = statusCode(recipe, stats);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("metal_press")
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY || status == STATUS_WARMING,
                                getBlockState().getValue(BaseMachineBlock.ACTIVE),
                                status == STATUS_NO_INPUT
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(
                        statusKey(status),
                        status == STATUS_WARMING ? currentTargetTemperature(recipe) : currentMinimumTemperature(recipe, stats)
                )
                .progress(progress, currentProcessingTicks(recipe, stats))
                .energy(energyStored(), energyCapacity(), -energyDemand(status, recipe, stats))
                .energyTelemetry(energyFlow.lastInput(), energyFlow.lastOutput(), connector.transferRate(), MachineInfoSnapshot.EnergyBottleneck.NONE)
                .heat(currentTemperature, currentTargetTemperature(recipe))
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
            case STATUS_READY -> energyCostPerTick(recipe, stats) * batchJobs(recipe, stats);
            case STATUS_WARMING, STATUS_NO_POWER -> energyCostPerTick(recipe, stats);
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
        MachineStatAccumulator stats = MachineBaseStatCatalog.metalPress();
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
        MegaPassiveTree.applyStats(stats, machineProgression(), MachineMasteryFamily.METAL_PRESS);
        applyHeatCoreStats(stats);
        applyServoStats(stats);
        if (!hasBatteryCell()) {
            stats.apply(new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.PROCESSING_SPEED,
                    ModifierOperation.LESS,
                    NO_BATTERY_PROCESSING_SPEED
            ));
            stats.apply(new MachineModifier(
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
        return MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("Energy", internalEnergyStored());
        tag.putInt("CurrentTemperature", currentTemperature);
        tag.putInt("LastMinimumTemperature", lastMinimumTemperature);
        tag.putInt("LastTargetTemperature", lastTargetTemperature);
        tag.putInt("LastSafeMaximumTemperature", lastSafeMaximumTemperature);
        tag.putInt("FailureStrain", failureStrain);
        tag.putInt("PowerDropTicks", powerDropTicks);
        tag.putBoolean("TargetReached", targetReached);
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
        progress = tag.getInt("Progress");
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        currentTemperature = Math.max(0, tag.getInt("CurrentTemperature"));
        lastMinimumTemperature = Math.max(0, tag.getInt("LastMinimumTemperature"));
        lastTargetTemperature = Math.max(0, tag.getInt("LastTargetTemperature"));
        lastSafeMaximumTemperature = Math.max(0, tag.getInt("LastSafeMaximumTemperature"));
        failureStrain = Math.max(0, tag.getInt("FailureStrain"));
        powerDropTicks = Math.max(0, tag.getInt("PowerDropTicks"));
        targetReached = tag.getBoolean("TargetReached") || progress > 0;
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
        return stack.getItem() instanceof SolidFuelBurnerPartItem part
                && part.partType() == MachinePartType.HEAT_CORE
                && part.stage() <= 6;
    }

    public static boolean isServo(ItemStack stack) {
        return stack.getItem() instanceof ServoItem;
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
                : MetalPressRecipes.find(level, inputStack(), moldStack()).filter(recipe -> !routeDisabled(recipe)).orElse(null);
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

    /** Forge Line disables circuit recipes. */
    private boolean routeDisabled(MetalPressRecipe recipe) {
        return isCircuit(recipe) && hasMasteryBehavior("FORGE_LINE");
    }

    /** Circuit Discipline: circuit recipes gain Stability and use less FE. */
    private MachineStatAccumulator routeStats(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        if (recipe != null && isCircuit(recipe) && hasMasteryBehavior("CIRCUIT_DISCIPLINE")) {
            stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.STABILITY, ModifierOperation.INCREASED_PERCENT, 20.0));
            stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.ENERGY_USAGE, ModifierOperation.LESS, 0.85));
        }
        return stats;
    }

    /** Production Die speeds gears and casings and slows circuits; Hot Stamping speeds plates above their target. */
    private double routeSpeed(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        double speed = 1.0;
        if (hasMasteryBehavior("PRODUCTION_DIE")) {
            speed *= isGearOrCasing(recipe) ? 1.4 : isCircuit(recipe) ? 0.75 : 1.0;
        }
        if (hasMasteryBehavior("HOT_STAMPING") && isPlate(recipe) && currentTemperature > recipe.targetTemperature()) {
            speed *= 1.2;
        }
        return speed;
    }

    /** The batch locked when the cycle started, shrunk only if its input has since gone. */
    private int batchJobs(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        int available = availableBatch(recipe, stats);
        return progress > 0 && lockedBatch > 0 ? Math.min(lockedBatch, available) : available;
    }

    /** Plates, gears, and casings press one input set per batched item, as far as input and output allow. Circuits never batch. */
    private int availableBatch(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        if (isCircuit(recipe) || !(isPlate(recipe) || isGearOrCasing(recipe))) {
            return 1;
        }
        int limit = Math.min(BatchProcessing.batchSize(stats, false), inputStack().getCount() / Math.max(1, recipe.inputCount()));
        ItemStack output = recipe.outputStack();
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
            if (index != selectedMold && isMetalPressMold(mold) && MetalPressRecipes.find(level, inputStack(), mold).filter(recipe -> !routeDisabled(recipe)).isPresent()) {
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
        ItemStack baseResult = recipe.outputStack();
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
        result = withBatchLedger(recipe, stats, result, jobs);

        consumeInput(recipe.inputCount() * jobs);
        mergeOutput(result);
        for (int job = 0; job < jobs; job++) {
            grantRecipeXp(recipe);
        }
        resetCycle();
        setChanged();
        return true;
    }

    /** The Batch Ledger banks a share of every eligible job and pays whole items once the output has room. */
    private ItemStack withBatchLedger(MetalPressRecipe recipe, MachineStatAccumulator stats, ItemStack result, int jobs) {
        if (!ledgerActive(recipe, stats)) {
            return result;
        }
        ItemStack base = recipe.outputStack();
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

    /** The Crude Metal Press is the Stage 3 bootstrap body; the Steel Metal Press is Stage 4. */
    @Override
    public int ascendancyEntryStage() {
        return isCrudePress() ? 3 : 4;
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
        resetCycle();
        resetBulkSpeed();
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

    private int processingTicks(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        int ticks = Math.max(1, (int) Math.ceil(stats.adjustedHeatProcessingTicks(recipe.processingTicks()) / routeSpeed(recipe, stats)));
        return BatchProcessing.batchTicks(ticks, stats, batchJobs(recipe, stats));
    }

    private int energyCostPerTick(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        int adjustedTicks = processingTicks(recipe, stats);
        int totalEnergy = stats.adjustedEnergyCost(recipe.energy());
        return Math.max(1, (int) Math.ceil(totalEnergy / (double) adjustedTicks));
    }

    private int energyCostPerCraft(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return stats.adjustedEnergyCost(recipe.energy());
    }

    private int currentProcessingTicks(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null ? 0 : processingTicks(recipe, stats);
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
                : HeatControl.effectiveOverheatTemperature(safeMaximumTemperature, stats);
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
            return level != null && MetalPressRecipes.find(level, inputStack(), moldStack()).isPresent() ? STATUS_ROUTE_DISABLED : STATUS_INVALID_RECIPE;
        }
        if (isCrudePress() && currentTemperature > effectiveSafeMaximumTemperature(recipe, stats)) {
            return STATUS_HEAT_HIGH;
        }
        if (effectiveHeat(stats) < recipe.targetTemperature()) {
            return STATUS_HEAT_LOW;
        }
        if (isCrudePress() && stats.value(MachineStat.TEMPERATURE_STABILITY) < recipe.requiredTemperatureStability()) {
            return STATUS_STABILITY_LOW;
        }
        if (!canMergeOutput(recipe.outputStack())
                || (!isCrudePress()
                        && failureStrain >= FAILURE_STRAIN_THRESHOLD
                        && !canMergeOutput(recipe.failureStack()))) {
            return STATUS_OUTPUT_FULL;
        }
        if (powerDropTicks > 0) {
            return STATUS_POWER_DROP;
        }
        if (currentTemperature < requiredTemperatureForProgress(recipe, stats)) {
            int energyCost = energyCostPerTick(recipe, stats);
            return consumeWorkingEnergy(energyCost, true) < energyCost ? STATUS_NO_POWER : STATUS_WARMING;
        }
        int energyCost = energyCostPerTick(recipe, stats);
        return consumeWorkingEnergy(energyCost, true) < energyCost ? STATUS_NO_POWER : STATUS_READY;
    }

    private boolean crudePressBlocksUnsafeRecipe(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return isCrudePress()
                && (currentTemperature > effectiveSafeMaximumTemperature(recipe, stats)
                        || stats.value(MachineStat.TEMPERATURE_STABILITY) < recipe.requiredTemperatureStability());
    }

    private int requiredTemperatureForProgress(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        return targetReached ? minimumTemperature(recipe, stats) : recipe.targetTemperature();
    }

    private boolean warm(MetalPressRecipe recipe, MachineStatAccumulator stats, int requiredTemperature) {
        int nextTemperature = Math.min(
                Math.min(requiredTemperature, stats.intValue(MachineStat.MAX_TEMPERATURE)),
                currentTemperature + HeatControl.warmupRate(stats)
        );
        if (nextTemperature == currentTemperature) {
            return false;
        }
        currentTemperature = nextTemperature;
        if (currentTemperature >= recipe.targetTemperature()) {
            targetReached = true;
        }
        return true;
    }

    private boolean cool(MachineStatAccumulator stats) {
        int nextTemperature = Math.max(HeatControl.AMBIENT_TEMPERATURE, currentTemperature - HeatControl.coolingRate(stats));
        boolean changed = false;
        if (nextTemperature != currentTemperature) {
            currentTemperature = nextTemperature;
            changed = true;
        }
        if (currentTemperature <= HeatControl.AMBIENT_TEMPERATURE) {
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
        return currentTemperature > HeatControl.AMBIENT_TEMPERATURE && lastSafeMaximumTemperature > 0;
    }

    private void updateFailureStrain(MetalPressRecipe recipe, MachineStatAccumulator stats) {
        if (!failureEnabled(recipe)) {
            failureStrain = 0;
            return;
        }

        // Master Die: only heat outside the window adds strain, measured without the stability wobble.
        boolean masterDie = hasMasteryBehavior("MASTER_DIE");
        double requiredStability = masterDie ? 0.0 : recipe.requiredTemperatureStability();
        int simulatedTemperature = HeatControl.simulatedTemperature(
                currentTemperature,
                recipe.targetTemperature(),
                requiredStability,
                stats,
                worldPosition,
                level == null ? 0L : level.getGameTime(),
                0
        );
        int addedStrain = HeatControl.failureStrainFromTemperature(
                simulatedTemperature,
                minimumTemperature(recipe, stats),
                HeatControl.effectiveOverheatTemperature(safeMaximumTemperature(recipe, stats), stats),
                requiredStability,
                stats
        );
        if (hasMasteryBehavior("TOLERANCE_MAP")) {
            addedStrain = (int) Math.round(addedStrain * 0.6);
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
        if (progress != 0) {
            return;
        }
        lockedBatch = availableBatch(recipe, stats);
        activeInput = singleCopy(inputStack());
        activeMold = singleCopy(moldStack());
        failureStrain = 0;
        powerDropTicks = 0;
    }

    private boolean activeCycleMatches() {
        if (progress == 0) {
            return true;
        }
        return !activeInput.isEmpty()
                && !activeMold.isEmpty()
                && ItemStack.isSameItemSameComponents(activeInput, inputStack())
                && ItemStack.isSameItemSameComponents(activeMold, moldStack());
    }

    private void resetCycleIfActive() {
        if (progress != 0
                || failureStrain != 0
                || powerDropTicks != 0
                || targetReached
                || !activeInput.isEmpty()
                || !activeMold.isEmpty()) {
            resetCycle();
            resetBulkSpeed();
            setChanged();
        }
    }

    private void resetCycle() {
        progress = 0;
        lockedBatch = 0;
        failureStrain = 0;
        powerDropTicks = 0;
        targetReached = false;
        activeInput = ItemStack.EMPTY;
        activeMold = ItemStack.EMPTY;
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
        if (!(stack.getItem() instanceof SolidFuelBurnerPartItem part) || !isHeatCore(stack)) {
            return;
        }
        stats.apply(new MachineModifier(
                ModifierSlot.IMPLICIT,
                MachineStat.MAX_TEMPERATURE,
                ModifierOperation.ADD,
                FORMING_BED_TEMPERATURE
        ));
        ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
    }

    private void applyServoStats(MachineStatAccumulator stats) {
        ItemStack stack = servoStack();
        if (isServo(stack) && stack.getItem() instanceof MachinePartItem part) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        }
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
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

    private int effectiveMaxEnergyInput() {
        return Math.max(1, (int) Math.round(effectiveStats().value(MachineStat.ENERGY_TRANSFER)));
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
        ItemStack cell = batteryCellStack();
        return cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
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
            if (!allowInsert) {
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
            return allowInsert && processInventory.isItemValid(mappedSlot(slot), stack);
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

            int remaining = Math.min(toReceive, effectiveMaxEnergyInput());
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
