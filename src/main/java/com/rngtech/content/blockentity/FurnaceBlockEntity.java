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
import com.rngtech.content.recipe.ProcessingEnergyScaling;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModTags;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class FurnaceBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider, MachineMasteryHost {
    public static final int MAX_PROCESSING_SLOTS = 4;
    public static final int MAX_GEAR_SLOTS = 4;
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
    private static final int DATA_REFINEMENT_POTENTIAL = DATA_FUEL_EFFICIENCY + 1;
    private static final int DATA_ENERGY = DATA_REFINEMENT_POTENTIAL + 1;
    private static final int DATA_ENERGY_CAPACITY = DATA_ENERGY + 1;
    private static final int DATA_ENERGY_USAGE = DATA_ENERGY_CAPACITY + 1;
    private static final int DATA_ENERGY_CAPACITY_STAT = DATA_ENERGY_USAGE + 1;
    private static final int DATA_ENERGY_TRANSFER = DATA_ENERGY_CAPACITY_STAT + 1;
    private static final int DATA_WARMUP_TIME = DATA_ENERGY_TRANSFER + 1;
    private static final int DATA_COOLING_RATE = DATA_WARMUP_TIME + 1;
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
    private static final int DATA_MACHINE_PROGRESSION_START = DATA_LEDGER_START + MAX_PROCESSING_SLOTS;
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

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            if (index >= DATA_PROGRESS_START && index < DATA_PROGRESS_START + MAX_PROCESSING_SLOTS) {
                return progress[index - DATA_PROGRESS_START];
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
                return currentTemperature[index - DATA_HEAT_START];
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
                return failureEnabled(lane, statsForLane(lane)) ? 1 : 0;
            }
            if (index >= DATA_POWER_SENSITIVE_START && index < DATA_POWER_SENSITIVE_START + MAX_PROCESSING_SLOTS) {
                int lane = index - DATA_POWER_SENSITIVE_START;
                return powerSensitiveActive(lane, statsForLane(lane)) ? 1 : 0;
            }
            if (index >= DATA_LEDGER_START && index < DATA_LEDGER_START + MAX_PROCESSING_SLOTS) {
                return ledgerDisplay(index - DATA_LEDGER_START);
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
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_WARMUP_TIME -> scaledStat(stats, MachineStat.WARMUP_TIME);
                case DATA_COOLING_RATE -> scaledStat(stats, MachineStat.COOLING_RATE);
                case DATA_OVERHEAT_TOLERANCE -> scaledStat(stats, MachineStat.OVERHEAT_TOLERANCE);
                case DATA_INPUT_SLOTS -> scaledStat(stats, MachineStat.INPUT_SLOTS);
                case DATA_GEAR_SLOTS -> activeGearSlots();
                case DATA_MIN_TEMPERATURE -> currentMinimumTemperature(stats);
                case DATA_STATUS -> statusCode(stats);
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
    private final int[] progress = new int[MAX_PROCESSING_SLOTS];
    private final int[] currentTemperature = new int[MAX_PROCESSING_SLOTS];
    private final int[] lastTargetTemperature = new int[MAX_PROCESSING_SLOTS];
    private final int[] lastSafeMaximumTemperature = new int[MAX_PROCESSING_SLOTS];
    private final int[] failureStrain = new int[MAX_PROCESSING_SLOTS];
    private final boolean[] targetReached = new boolean[MAX_PROCESSING_SLOTS];
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
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FurnaceBlockEntity furnace) {
        furnace.beginEnergyTelemetryTick();
        MachineStatAccumulator baseStats = furnace.baseEffectiveStats();
        int activeLanes = furnace.activeProcessingSlots(baseStats);
        boolean running = false;
        boolean changed = false;

        for (int lane = 0; lane < activeLanes; lane++) {
            MachineStatAccumulator laneStats = furnace.statsForLane(lane);
            FurnaceRecipe recipe = furnace.findNextRecipe(lane, laneStats);
            if (recipe == null) {
                changed |= furnace.resetCycle(lane);
                changed |= furnace.coolLane(lane, laneStats);
                continue;
            }
            if (!furnace.activeCycleMatches(lane)) {
                changed |= furnace.resetCycle(lane);
                changed |= furnace.resetBulkSpeed();
            }
            changed |= furnace.rememberHeatEnvelope(lane, recipe);

            if (furnace.progress[lane] == 0 && furnace.currentTemperature[lane] < furnace.targetTemperature(recipe)) {
                furnace.targetReached[lane] = false;
            }
            int requiredTemperature = furnace.requiredTemperatureForProgress(lane, recipe);
            if (furnace.currentTemperature[lane] < requiredTemperature) {
                if (furnace.payHeatTick(lane, recipe, laneStats)) {
                    changed |= furnace.warmLane(lane, recipe, laneStats, requiredTemperature);
                    running = true;
                } else {
                    changed |= furnace.registerPowerDrop(lane, recipe, laneStats);
                    if (furnace.failureStrain[lane] >= HeatControl.FAILURE_STRAIN_THRESHOLD) {
                        changed |= furnace.tryFailCycle(lane, recipe);
                    }
                    changed |= furnace.coolLane(lane, laneStats);
                }
                continue;
            }
            furnace.targetReached[lane] = true;

            if (furnace.isElectric()) {
                int energyCost = furnace.energyCostForProgress(lane, recipe, laneStats);
                if (furnace.progress[lane] == 0 && ProcessingChance.rollInstant(level, laneStats)) {
                    int fullEnergyCost = furnace.energyCostPerCraft(recipe, laneStats);
                    if (furnace.consumeWorkingEnergy(fullEnergyCost, true) >= fullEnergyCost) {
                        furnace.startCycleIfNeeded(lane);
                        furnace.consumeWorkingEnergy(fullEnergyCost, false);
                        if (furnace.process(lane, recipe, laneStats)) {
                            changed |= furnace.bulkSpeed.recordProcess(furnace.activeTraits());
                        }
                        running = true;
                        changed = true;
                        continue;
                    }
                }
                if (furnace.consumeWorkingEnergy(energyCost, true) < energyCost) {
                    changed |= furnace.registerPowerDrop(lane, recipe, laneStats);
                    if (furnace.failureStrain[lane] >= HeatControl.FAILURE_STRAIN_THRESHOLD) {
                        changed |= furnace.tryFailCycle(lane, recipe);
                    }
                    changed |= furnace.coolLane(lane, laneStats);
                    continue;
                }
                furnace.consumeWorkingEnergy(energyCost, false);
            } else {
                if (!furnace.hasBurnTime() && !furnace.tryStartBurningFuel()) {
                    changed |= furnace.coolLane(lane, laneStats);
                    continue;
                }
                furnace.burnTime--;
                if (furnace.progress[lane] == 0 && ProcessingChance.rollInstant(level, laneStats)) {
                    furnace.startCycleIfNeeded(lane);
                    if (furnace.process(lane, recipe, laneStats)) {
                        changed |= furnace.bulkSpeed.recordProcess(furnace.activeTraits());
                    }
                    running = true;
                    changed = true;
                    continue;
                }
            }

            furnace.startCycleIfNeeded(lane);
            changed |= furnace.updateFailureStrain(lane, recipe, laneStats);
            if (furnace.failureStrain[lane] >= HeatControl.FAILURE_STRAIN_THRESHOLD) {
                changed |= furnace.tryFailCycle(lane, recipe);
                continue;
            }

            furnace.progress[lane]++;
            running = true;
            changed = true;
            changed |= furnace.overdrive(lane, recipe, laneStats);

            if (furnace.progress[lane] >= furnace.processingTicks(lane, recipe, laneStats)) {
                if (furnace.process(lane, recipe, laneStats)) {
                    changed |= furnace.bulkSpeed.recordProcess(furnace.activeTraits());
                }
            }
        }

        for (int lane = activeLanes; lane < MAX_PROCESSING_SLOTS; lane++) {
            changed |= furnace.resetCycle(lane);
            changed |= furnace.coolLane(lane, baseStats);
        }

        if (!running) {
            changed |= furnace.bulkSpeed.reset();
        }
        BaseMachineBlock.setActive(level, pos, state, running);
        if (changed) {
            furnace.setChanged();
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
                .progress(lane < 0 ? 0 : progress[lane], lane < 0 ? 0 : currentProcessingTicks(lane))
                .energy(energyStored(), energyCapacity(), energyRate)
                .energyTelemetry(
                        lastEnergyInput(),
                        0,
                        connector.transferRate(),
                        energyBottleneck(connector, connectorDemand)
                )
                .fuel(burnTime, totalBurnTime)
                .heat(lane < 0 ? 0 : currentTemperature[lane], lane < 0 ? 0 : currentTargetTemperature(lane))
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
        tag.putIntArray("Progresses", progress);
        tag.putIntArray("CurrentTemperatures", currentTemperature);
        tag.putIntArray("LastTargetTemperatures", lastTargetTemperature);
        tag.putIntArray("LastSafeMaximumTemperatures", lastSafeMaximumTemperature);
        tag.putIntArray("FailureStrains", failureStrain);
        for (int lane = 0; lane < MAX_PROCESSING_SLOTS; lane++) {
            tag.put("ActiveInput" + lane, activeInputs[lane].saveOptional(registries));
        }
        tag.putInt("Progress", progress[0]);
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
        int[] savedProgress = tag.getIntArray("Progresses");
        if (savedProgress.length > 0) {
            System.arraycopy(savedProgress, 0, progress, 0, Math.min(savedProgress.length, progress.length));
        } else {
            progress[0] = tag.getInt("Progress");
        }
        int[] savedTemperatures = tag.getIntArray("CurrentTemperatures");
        if (savedTemperatures.length > 0) {
            System.arraycopy(savedTemperatures, 0, currentTemperature, 0, Math.min(savedTemperatures.length, currentTemperature.length));
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
        int[] savedFailureStrains = tag.getIntArray("FailureStrains");
        if (savedFailureStrains.length > 0) {
            System.arraycopy(savedFailureStrains, 0, failureStrain, 0, Math.min(savedFailureStrains.length, failureStrain.length));
        }
        for (int lane = 0; lane < activeInputs.length; lane++) {
            activeInputs[lane] = ItemStack.parseOptional(registries, tag.getCompound("ActiveInput" + lane));
            if (activeInputs[lane].isEmpty() && progress[lane] > 0) {
                activeInputs[lane] = singleCopy(inventory.getStackInSlot(inputSlot(lane)));
            }
        }
        for (int lane = 0; lane < targetReached.length; lane++) {
            targetReached[lane] = progress[lane] > 0;
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
        MachineStatAccumulator stats = baseEffectiveStats();
        applyDisplayGearStats(stats);
        return stats;
    }

    private MachineStatAccumulator baseEffectiveStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.furnace(furnaceMaterial());
        MachineTraits activeTraits = MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock());
        stats.apply(activeTraits);
        FurnacePassiveTree.applyStats(stats, machineProgression());
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

    private void applyDisplayGearStats(MachineStatAccumulator stats) {
        if (usesHeatCores()) {
            applyBestHeatCoreStats(stats);
            return;
        }
        for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
            applyGearStats(stats, slot);
        }
    }

    private void applyBestHeatCoreStats(MachineStatAccumulator stats) {
        ItemStack bestCore = ItemStack.EMPTY;
        double bestTemperature = Double.NEGATIVE_INFINITY;
        for (int slot = 0; slot < activeGearSlots(); slot++) {
            ItemStack gear = gearInventory.getStackInSlot(slot);
            if (!isAllowedGearComponent(slot, gear)) {
                continue;
            }
            MachineStatAccumulator gearStats = ComponentBaseStatCatalog.effectiveStats(gear);
            if (gearStats == null) {
                continue;
            }
            double temperature = gearStats.value(MachineStat.MAX_TEMPERATURE);
            if (temperature > bestTemperature) {
                bestTemperature = temperature;
                bestCore = gear;
            }
        }
        if (!bestCore.isEmpty()) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, bestCore);
        }
    }

    private MachineTraits activeTraits() {
        return MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock());
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
        FurnaceRecipe recipe = FurnaceRecipes.find(level, input).orElse(null);
        if (recipe == null || !meetsRecipeRequirements(recipe, stats, input) || !canAcceptOutput(lane, recipe)) {
            return null;
        }
        return recipe;
    }

    private FurnaceRecipe findRecipeWithoutGates(int lane) {
        if (level == null || lane < 0 || lane >= MAX_PROCESSING_SLOTS) {
            return null;
        }
        ItemStack input = inventory.getStackInSlot(inputSlot(lane));
        return input.isEmpty() ? null : FurnaceRecipes.find(level, input).orElse(null);
    }

    public boolean isSmeltable(ItemStack stack) {
        return level != null && FurnaceRecipes.find(level, stack).isPresent();
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
            FurnaceRecipe recipe = level == null ? null : FurnaceRecipes.find(level, input).orElse(null);
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
            FurnaceRecipe recipe = level == null ? null : FurnaceRecipes.find(level, input).orElse(null);
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
            if (failureEnabled(lane, laneStats)
                    && failureStrain[lane] >= HeatControl.FAILURE_STRAIN_THRESHOLD
                    && !canAcceptFailureOutput(lane, recipe)) {
                blockedStatus = STATUS_OUTPUT_FULL;
                continue;
            }
            if (powerSensitiveActive(lane, laneStats)) {
                blockedStatus = STATUS_POWER_DROP;
                continue;
            }
            if (failureStrain[lane] > 0 && failureEnabled(lane, laneStats)) {
                blockedStatus = STATUS_FAILURE_RISK;
                continue;
            }
            if (currentTemperature[lane] < requiredTemperatureForProgress(lane, recipe)) {
                if (isElectric()) {
                    int energyStatus = laneEnergyStatus(lane, recipe, laneStats);
                    if (energyStatus != STATUS_READY) {
                        blockedStatus = energyStatus;
                        continue;
                    }
                }
                if (!isElectric() && !hasBurnTime() && !isFuel(inventory.getStackInSlot(SLOT_FUEL))) {
                    blockedStatus = STATUS_NO_FUEL;
                    continue;
                }
                return STATUS_WARMING;
            }
            if (isElectric()) {
                int energyStatus = laneEnergyStatus(lane, recipe, laneStats);
                if (energyStatus == STATUS_READY) {
                    return STATUS_READY;
                }
                blockedStatus = energyStatus;
                continue;
            }
            if (hasBurnTime() || isFuel(inventory.getStackInSlot(SLOT_FUEL))) {
                return STATUS_READY;
            }
            blockedStatus = STATUS_NO_FUEL;
        }
        return hasInput ? blockedStatus : STATUS_NO_INPUT;
    }

    private int machineInfoLane(MachineStatAccumulator baseStats) {
        int activeLanes = activeProcessingSlots(baseStats);
        for (int lane = 0; lane < activeLanes; lane++) {
            if (progress[lane] > 0) {
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
        if (status == STATUS_READY || status == STATUS_WARMING || status == STATUS_FAILURE_RISK) {
            return getBlockState().getValue(BaseMachineBlock.ACTIVE)
                    ? MachineInfoSnapshot.WorkState.RUNNING
                    : MachineInfoSnapshot.WorkState.IDLE;
        }
        return status == STATUS_NO_INPUT ? MachineInfoSnapshot.WorkState.IDLE : MachineInfoSnapshot.WorkState.BLOCKED;
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
        if (level == null || !craft(lane, recipe, stats)) {
            return false;
        }
        // Crucible Heart finishes a second input when the lane runs at twice the recipe target or hotter.
        if (hasMasteryBehavior("CRUCIBLE_HEART")
                && currentTemperature[lane] >= 2 * targetTemperature(recipe)
                && recipe.matches(new SingleRecipeInput(inventory.getStackInSlot(inputSlot(lane))), level)
                && canMergeOutput(outputSlot(lane), recipe.outputStack())
                && payExtraCraft(lane, recipe, stats)) {
            craft(lane, recipe, stats);
        }
        resetCycle(lane);
        setChanged();
        return true;
    }

    private boolean craft(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        ItemStack input = inventory.getStackInSlot(inputSlot(lane));
        if (!recipe.matches(new SingleRecipeInput(input), level)) {
            return false;
        }

        ItemStack baseResult = recipe.outputStack();
        ItemStack result = ProcessingChance.applySuperOutput(level, stats, recipe, baseResult, baseResult);
        int outputSlot = outputSlot(lane);
        if (!canMergeOutput(outputSlot, result)) {
            result = baseResult;
            if (!canMergeOutput(outputSlot, result)) {
                return false;
            }
        }

        result = withLedger(recipe, stats, input, result, outputSlot);
        input.shrink(1);
        mergeOutput(outputSlot, result);
        grantRecipeXp(recipe);
        refundClosedLoopEnergy(recipe, stats);
        return true;
    }

    /** The Bloom Ledger banks a share of each eligible smelt and pays whole items once the output has room. */
    private ItemStack withLedger(FurnaceRecipe recipe, MachineStatAccumulator stats, ItemStack input, ItemStack result, int outputSlot) {
        if (!ledgerEligible(recipe, input, stats)) {
            return result;
        }
        ItemStack base = recipe.outputStack();
        Item item = base.getItem();
        boolean crushed = hasMasteryBehavior("CRUSHER_LINE") && input.is(ModTags.Items.CRUSHED_MATERIALS);
        bloomLedger.add(item, AscendancyFormulas.ledgerShare(stats, base.getCount(), crushed));
        int payable = bloomLedger.payable(item);
        ItemStack paid = ProcessingChance.grow(result, base, payable);
        if (paid == result || !canMergeOutput(outputSlot, paid)) {
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

    /** Crucible Heart's second input costs a second craft's FE, or its burn time on a fuel furnace. */
    private boolean payExtraCraft(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (isElectric()) {
            int cost = energyCostPerCraft(recipe, stats);
            if (consumeWorkingEnergy(cost, true) < cost) {
                return false;
            }
            consumeWorkingEnergy(cost, false);
            return true;
        }
        int ticks = processingTicks(lane, recipe, stats);
        if (burnTime < ticks) {
            return false;
        }
        burnTime -= ticks;
        return true;
    }

    private void refundClosedLoopEnergy(FurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (!isElectric() || !FurnacePassiveTree.usesClosedLoopRecuperator(machineProgression())) {
            return;
        }
        int refund = Math.max(1, (int) Math.ceil(energyCostPerCraft(recipe, stats) * 0.05D));
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
        if (!failureEnabled(lane, statsForLane(lane)) || level == null) {
            return false;
        }
        ItemStack input = inventory.getStackInSlot(inputSlot(lane));
        if (!recipe.matches(new SingleRecipeInput(input), level)) {
            return false;
        }

        ItemStack result = recipe.failureStack();
        int outputSlot = outputSlot(lane);
        if (!canMergeOutput(outputSlot, result)) {
            return false;
        }

        input.shrink(1);
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
        return recipe.hasFailureOutput() && canMergeOutput(outputSlot(lane), recipe.failureStack());
    }

    private boolean canMergeOutput(int slot, ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = inventory.getStackInSlot(slot);
        int slotLimit = Math.min(result.getMaxStackSize(), inventory.getSlotLimit(slot));
        if (output.isEmpty()) {
            return result.getCount() <= slotLimit;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= slotLimit;
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
        return recipe == null ? 0 : processingTicks(lane, recipe, statsForLane(lane));
    }

    private int currentEnergyCostPerTick(int lane) {
        if (!isElectric()) {
            return 0;
        }
        FurnaceRecipe recipe = findNextRecipe(lane);
        return recipe == null ? 0 : energyCostForProgress(lane, recipe, statsForLane(lane));
    }

    private boolean canPayLaneEnergy(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (!isElectric()) {
            return true;
        }
        int energyCost = energyCostForProgress(lane, recipe, stats);
        return consumeWorkingEnergy(energyCost, true) >= energyCost;
    }

    private int laneEnergyStatus(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        int energyCost = energyCostForProgress(lane, recipe, stats);
        if (consumeWorkingEnergy(energyCost, true) >= energyCost) {
            return STATUS_READY;
        }
        return energyStored() >= energyCost ? STATUS_POWER_LIMITED : STATUS_NO_POWER;
    }

    private int currentEnergyCostPerCraft(int lane) {
        if (!isElectric()) {
            return 0;
        }
        FurnaceRecipe recipe = findNextRecipe(lane);
        return recipe == null ? 0 : energyCostPerCraft(recipe, statsForLane(lane));
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
        return HeatControl.effectiveOverheatTemperature(safeMaximumTemperature, statsForLane(lane));
    }

    /** Overdrive shortens a lane's cycle while it runs above the recipe target; the cycle's FE stays the same. */
    private int processingTicks(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        int ticks = stats.adjustedHeatProcessingTicks(recipe.processingTicks());
        double overdrive = AscendancyFormulas.overdriveSpeedMultiplier(currentTemperature[lane], targetTemperature(recipe), stats);
        return Math.max(1, (int) Math.ceil(ticks / overdrive));
    }

    private int energyCostForProgress(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        int adjustedTicks = processingTicks(lane, recipe, stats);
        return stats.adjustedEnergyCostForProgress(ProcessingEnergyScaling.furnaceEnergy(recipe), adjustedTicks, progress[lane]);
    }

    /** Overdrive heats toward the lane maximum, or stops Overdrive Margin below the recipe's safe maximum. */
    private int overdriveTarget(FurnaceRecipe recipe, MachineStatAccumulator stats) {
        int ceiling = stats.intValue(MachineStat.MAX_TEMPERATURE);
        int margin = stats.intValue(MachineStat.OVERDRIVE_MARGIN);
        if (margin > 0 || hasMasteryBehavior("SAFE_OVERDRIVE")) {
            ceiling = Math.min(ceiling, safeMaximumTemperature(recipe) - margin);
        }
        return Math.max(targetTemperature(recipe), ceiling);
    }

    /** Overdrive keeps heating a working lane past the recipe target; each step costs another heat tick. */
    private boolean overdrive(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (stats.value(MachineStat.OVERDRIVE_CAP) <= 0.0 || stats.value(MachineStat.OVERDRIVE_SPEED) <= 0.0) {
            return false;
        }
        int target = overdriveTarget(recipe, stats);
        if (currentTemperature[lane] >= target || !payHeatTick(lane, recipe, stats)) {
            return false;
        }
        return warmLane(lane, recipe, stats, target);
    }

    private int energyCostPerCraft(FurnaceRecipe recipe, MachineStatAccumulator stats) {
        return stats.adjustedEnergyCost(ProcessingEnergyScaling.furnaceEnergy(recipe));
    }

    private MachineStatAccumulator statsForLane(int lane) {
        MachineStatAccumulator stats = baseEffectiveStats();
        applyLaneGearStats(stats, lane);
        applySharedHearth(stats);
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

    /** Shared Hearth: on a Heat Core furnace, every lane reaches at least 90% of the hottest core's maximum. */
    private void applySharedHearth(MachineStatAccumulator stats) {
        if (!usesHeatCores() || !hasMasteryBehavior("SHARED_HEARTH")) {
            return;
        }
        MachineStatAccumulator hottest = baseEffectiveStats();
        applyBestHeatCoreStats(hottest);
        double shared = Math.floor(hottest.value(MachineStat.MAX_TEMPERATURE) * AscendancyFormulas.SHARED_HEARTH_SHARE);
        if (shared > stats.value(MachineStat.MAX_TEMPERATURE)) {
            try (MachineStatAccumulator.Source ignored = stats.source(MegaPassiveTree.behaviorSource(machineProgression(), "SHARED_HEARTH"))) {
                stats.setAbsolute(MachineStat.MAX_TEMPERATURE, shared);
            }
        }
    }

    private void applyLaneGearStats(MachineStatAccumulator stats, int lane) {
        if (!usesHeatCores()) {
            for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
                applyGearStats(stats, slot);
            }
            return;
        }
        int slot = Math.max(0, Math.min(lane, activeGearSlots() - 1));
        applyGearStats(stats, slot);
    }

    private void applyGearStats(MachineStatAccumulator stats, int slot) {
        ItemStack gear = gearInventory.getStackInSlot(slot);
        if (isAllowedGearComponent(slot, gear)) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, gear);
        }
    }

    private boolean alloyBlendActive(int lane) {
        if (!MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock())
                .hasBehavior(MachineBehavior.ALLOY_BLEND)) {
            return false;
        }
        return inventory.getStackInSlot(inputSlot(lane)).is(ModTags.Items.ALLOY_BLEND_SMELTABLES);
    }

    private int energyStored() {
        return internalEnergyStored() + BatteryCellItem.energyStored(batteryCellStack());
    }

    private int energyCapacity() {
        return internalEnergyCapacity() + BatteryCellItem.energyCapacity(batteryCellStack());
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

    private int requiredTemperatureForProgress(int lane, FurnaceRecipe recipe) {
        return targetReached[lane] ? minimumTemperature(recipe) : targetTemperature(recipe);
    }

    private boolean warmLane(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats, int requiredTemperature) {
        int nextTemperature = Math.min(
                Math.min(requiredTemperature, stats.intValue(MachineStat.MAX_TEMPERATURE)),
                currentTemperature[lane] + HeatControl.warmupRate(stats)
        );
        if (nextTemperature == currentTemperature[lane]) {
            return false;
        }
        currentTemperature[lane] = nextTemperature;
        if (currentTemperature[lane] >= targetTemperature(recipe)) {
            targetReached[lane] = true;
        }
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
        return currentTemperature[lane] > HeatControl.AMBIENT_TEMPERATURE && lastSafeMaximumTemperature[lane] > 0;
    }

    private boolean coolLane(int lane, MachineStatAccumulator stats) {
        if (hasMasteryBehavior("HOLD_THE_FIRE") && findRecipeWithoutGates(lane) != null) {
            return false;
        }
        int nextTemperature = Math.max(HeatControl.AMBIENT_TEMPERATURE, currentTemperature[lane] - HeatControl.coolingRate(stats));
        boolean changed = false;
        if (nextTemperature != currentTemperature[lane]) {
            currentTemperature[lane] = nextTemperature;
            changed = true;
        }
        if (currentTemperature[lane] <= HeatControl.AMBIENT_TEMPERATURE) {
            changed |= clearHeatEnvelope(lane);
        }
        return changed;
    }

    private boolean payHeatTick(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (isElectric()) {
            int energyCost = energyCostForProgress(lane, recipe, stats);
            if (consumeWorkingEnergy(energyCost, true) < energyCost) {
                return false;
            }
            consumeWorkingEnergy(energyCost, false);
            return true;
        }
        if (!hasBurnTime() && !tryStartBurningFuel()) {
            return false;
        }
        burnTime--;
        return true;
    }

    private boolean updateFailureStrain(int lane, FurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (!failureEnabled(lane, stats)) {
            if (failureStrain[lane] == 0) {
                return false;
            }
            failureStrain[lane] = 0;
            return true;
        }

        int simulatedTemperature = HeatControl.simulatedTemperature(
                currentTemperature[lane],
                targetTemperature(recipe),
                recipe.requiredTemperatureStability(),
                stats,
                worldPosition,
                level == null ? 0L : level.getGameTime(),
                lane
        );
        int overheatTemperature = HeatControl.effectiveOverheatTemperature(safeMaximumTemperature(recipe), stats);
        if (hasMasteryBehavior("SAFE_OVERDRIVE") && currentTemperature[lane] > targetTemperature(recipe)) {
            simulatedTemperature = Math.min(simulatedTemperature, overheatTemperature);
        }
        int addedStrain = HeatControl.failureStrainFromTemperature(
                simulatedTemperature,
                minimumTemperature(recipe),
                overheatTemperature,
                recipe.requiredTemperatureStability(),
                stats
        );
        if (addedStrain > 0 && FurnacePassiveTree.usesQuenchProtocol(machineProgression())) {
            addedStrain = Math.max(1, (int) Math.ceil(addedStrain * 0.5D));
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
        if (!recipe.powerSensitive() || progress[lane] <= 0 || !failureEnabled(lane, stats)) {
            return false;
        }
        failureStrain[lane] = Math.min(
                HeatControl.FAILURE_STRAIN_THRESHOLD,
                failureStrain[lane] + (hasPowerGrace(lane) ? HeatControl.POWER_DROP_STRAIN / 2 : HeatControl.POWER_DROP_STRAIN)
        );
        return true;
    }

    private boolean failureEnabled(int lane, MachineStatAccumulator stats) {
        FurnaceRecipe recipe = level == null ? null : FurnaceRecipes.find(level, inventory.getStackInSlot(inputSlot(lane))).orElse(null);
        return recipe != null
                && recipe.hasFailureOutput()
                && furnaceMaterial().stage() >= 4
                && !cleanBloom(inventory.getStackInSlot(inputSlot(lane)))
                && stats.intValue(MachineStat.MAX_TEMPERATURE) >= targetTemperature(recipe);
    }

    private boolean powerSensitiveActive(int lane, MachineStatAccumulator stats) {
        FurnaceRecipe recipe = level == null ? null : FurnaceRecipes.find(level, inventory.getStackInSlot(inputSlot(lane))).orElse(null);
        return recipe != null
                && recipe.powerSensitive()
                && progress[lane] > 0
                && failureEnabled(lane, stats)
                && !canPayLaneEnergy(lane, recipe, stats);
    }

    private boolean hasPowerGrace(int lane) {
        if (activeTraits().hasBehavior(MachineBehavior.POWER_GRACE)) {
            return true;
        }
        if (!usesHeatCores()) {
            for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
                if (MachineImplicitCatalog.hasBehavior(gearInventory.getStackInSlot(slot), MachineBehavior.POWER_GRACE)) {
                    return true;
                }
            }
            return false;
        }
        int slot = Math.max(0, Math.min(lane, activeGearSlots() - 1));
        return MachineImplicitCatalog.hasBehavior(gearInventory.getStackInSlot(slot), MachineBehavior.POWER_GRACE);
    }

    private void startCycleIfNeeded(int lane) {
        if (progress[lane] != 0) {
            return;
        }
        activeInputs[lane] = singleCopy(inventory.getStackInSlot(inputSlot(lane)));
        failureStrain[lane] = 0;
    }

    private boolean activeCycleMatches(int lane) {
        if (progress[lane] == 0) {
            return true;
        }
        return !activeInputs[lane].isEmpty()
                && ItemStack.isSameItemSameComponents(activeInputs[lane], inventory.getStackInSlot(inputSlot(lane)));
    }

    private boolean resetCycle(int lane) {
        boolean changed = progress[lane] != 0 || failureStrain[lane] != 0 || targetReached[lane] || !activeInputs[lane].isEmpty();
        progress[lane] = 0;
        failureStrain[lane] = 0;
        targetReached[lane] = false;
        activeInputs[lane] = ItemStack.EMPTY;
        return changed;
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
        ItemStack cell = batteryCellStack();
        return cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
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

        ItemStackHandler legacyGear = new ItemStackHandler(Math.max(0, savedSize));
        legacyGear.deserializeNBT(registries, gearTag);
        for (int slot = 0; slot < Math.min(legacyGear.getSlots(), MAX_GEAR_SLOTS); slot++) {
            gearInventory.setStackInSlot(slot, legacyGear.getStackInSlot(slot));
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
            return inventory.insertItem(mappedSlot(slot), stack, simulate);
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
