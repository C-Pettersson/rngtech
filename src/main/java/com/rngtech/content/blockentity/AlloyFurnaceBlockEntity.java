package com.rngtech.content.blockentity;

import com.rngtech.content.block.AlloyFurnaceBlock;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.item.AlloyCrucibleItem;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.menu.AlloyFurnaceMenu;
import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.content.recipe.AlloyFurnaceRecipe;
import com.rngtech.content.recipe.AlloyFurnaceRecipeInput;
import com.rngtech.content.recipe.CountedIngredient;
import com.rngtech.content.recipe.FurnaceRecipe;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModRecipes;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AlloyFurnaceBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider, MachineMasteryHost {
    public static final int MAX_INPUT_SLOTS = 4;
    public static final int SLOT_INPUT_START = 0;
    public static final int SLOT_OUTPUT = SLOT_INPUT_START + MAX_INPUT_SLOTS;
    public static final int PROCESS_SLOT_COUNT = SLOT_OUTPUT + 1;
    public static final int SLOT_HEAT_CORE = 0;
    public static final int SLOT_CRUCIBLE = 1;
    public static final int SLOT_BATTERY_CELL = 2;
    public static final int SLOT_SERVO = 3;
    public static final int GEAR_SLOT_COUNT = 4;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_HEAT_CORE = 1;
    public static final int STATUS_MISSING_CRUCIBLE = 2;
    public static final int STATUS_NO_INPUT = 3;
    public static final int STATUS_INVALID_RECIPE = 4;
    public static final int STATUS_BLOCKED_STAGE = 5;
    public static final int STATUS_HEAT_LOW = 6;
    public static final int STATUS_STABILITY_LOW = 7;
    public static final int STATUS_OUTPUT_FULL = 8;
    public static final int STATUS_NO_POWER = 9;
    public static final int STATUS_WARMING = 10;
    public static final int STATUS_POWER_DROP = 11;
    public static final int STATUS_FAILURE_RISK = 12;
    public static final int STATUS_ROUTE_DISABLED = 13;

    private static final double NO_BATTERY_PROCESSING_SPEED = 0.85;
    private static final int NO_BATTERY_STABILITY_PENALTY = 10;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_ENERGY_PER_TICK = 4;
    private static final int DATA_MIN_TEMPERATURE = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_PROCESSING_SPEED = 7;
    private static final int DATA_ENERGY_USAGE = 8;
    private static final int DATA_ENERGY_CAPACITY_STAT = 9;
    private static final int DATA_ENERGY_TRANSFER = 10;
    private static final int DATA_HEAT_TRANSFER = 11;
    private static final int DATA_MAX_TEMPERATURE = 12;
    private static final int DATA_TEMPERATURE_STABILITY = 13;
    private static final int DATA_STABILITY = 14;
    private static final int DATA_INPUT_SLOTS = 15;
    private static final int DATA_REFINEMENT_POTENTIAL = 16;
    private static final int DATA_CURRENT_TEMPERATURE = 17;
    private static final int DATA_TARGET_TEMPERATURE = 18;
    private static final int DATA_SAFE_MAX_TEMPERATURE = 19;
    private static final int DATA_OVERHEAT_TEMPERATURE = 20;
    private static final int DATA_FAILURE_STRAIN = 21;
    private static final int DATA_FAILURE_ENABLED = 22;
    private static final int DATA_POWER_SENSITIVE = 23;
    private static final int DATA_WARMUP_TIME = 24;
    private static final int DATA_COOLING_RATE = 25;
    private static final int DATA_OVERHEAT_TOLERANCE = 26;
    private static final int DATA_LEDGER = 27;
    private static final int DATA_LEDGER_BLEND = 28;
    private static final int DATA_MACHINE_PROGRESSION_START = 29;
    private static final int DATA_COUNT = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int STAT_SCALE = 100;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= SLOT_INPUT_START && slot < SLOT_INPUT_START + activeInputSlots() && slot != SLOT_OUTPUT;
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
            if (isInputSlot(slot)) {
                resetCycle();
                resetBulkSpeed();
            }
            setChanged();
        }
    };
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_HEAT_CORE -> isHeatCore(stack);
                case SLOT_CRUCIBLE -> isAlloyCrucible(stack);
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_SERVO -> isServo(stack);
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
    private final IItemHandler inputHandler = new InputItemHandler();
    private final IItemHandler outputHandler = new OutputItemHandler();
    private final IEnergyStorage energyStorage = new AlloyFurnaceEnergyStorage();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_MACHINE_PROGRESSION_START
                    && index < DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT) {
                return MasteryMenuSupport.get(AlloyFurnaceBlockEntity.this, index - DATA_MACHINE_PROGRESSION_START, AlloyFurnaceBlockEntity.this::effectiveStats);
            }
            AlloyFurnaceRecipe recipe = nextRecipe();
            MachineStatAccumulator stats = routeStats(recipe, effectiveStats());
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> currentProcessingTicks(recipe, stats);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_ENERGY_PER_TICK -> recipe == null ? 0 : energyCostPerTick(recipe, stats);
                case DATA_MIN_TEMPERATURE -> recipe == null ? 0 : minimumTemperature(recipe, stats);
                case DATA_STATUS -> statusCode(recipe, stats);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_HEAT_TRANSFER -> scaledStat(stats, MachineStat.HEAT_TRANSFER);
                case DATA_MAX_TEMPERATURE -> scaledStat(stats, MachineStat.MAX_TEMPERATURE);
                case DATA_TEMPERATURE_STABILITY -> scaledStat(stats, MachineStat.TEMPERATURE_STABILITY);
                case DATA_STABILITY -> scaledStat(stats, MachineStat.STABILITY);
                case DATA_INPUT_SLOTS -> activeInputSlots(stats);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_CURRENT_TEMPERATURE -> currentTemperature;
                case DATA_TARGET_TEMPERATURE -> currentTargetTemperature(recipe, stats);
                case DATA_SAFE_MAX_TEMPERATURE -> currentSafeMaximumTemperature(recipe, stats);
                case DATA_OVERHEAT_TEMPERATURE -> currentOverheatTemperature(recipe, stats);
                case DATA_FAILURE_STRAIN -> HeatControl.failureProgress(failureStrain);
                case DATA_FAILURE_ENABLED -> failureEnabled(recipe, stats) ? 1 : 0;
                case DATA_POWER_SENSITIVE -> powerSensitiveActive(recipe, stats) ? 1 : 0;
                case DATA_WARMUP_TIME -> scaledStat(stats, MachineStat.WARMUP_TIME);
                case DATA_COOLING_RATE -> scaledStat(stats, MachineStat.COOLING_RATE);
                case DATA_OVERHEAT_TOLERANCE -> scaledStat(stats, MachineStat.OVERHEAT_TOLERANCE);
                case DATA_LEDGER -> ledgerDisplay(recipe, stats);
                case DATA_LEDGER_BLEND -> recipe != null && blendLedgerActive(recipe, stats) ? 1 : 0;
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
    private int lastTargetTemperature;
    private int lastSafeMaximumTemperature;
    private int failureStrain;
    private boolean targetReached;
    private ItemStack[] activeInputs = emptyInputs();
    private final OutputLedger<Item> fluxLedger = new OutputLedger<>();
    private final OutputLedger<Item> blendLedger = new OutputLedger<>();
    private final Map<AlloyFurnaceRecipe, ItemStack> reversalBlends = new IdentityHashMap<>();
    private AlloyFurnaceRecipe lastRecipe;
    private int cadenceCycles;
    private boolean pourContinues;
    private long energyTelemetryTick = Long.MIN_VALUE;
    private int energyInputThisTick;
    private int lastEnergyInput;

    public AlloyFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ALLOY_FURNACE.get(), pos, blockState, MachineType.ALLOY_FURNACE, SLOT_INPUT_START, SLOT_INPUT_START, SLOT_OUTPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlloyFurnaceBlockEntity furnace) {
        furnace.beginEnergyTelemetryTick();
        AlloyFurnaceRecipe recipe = furnace.nextRecipe();
        MachineStatAccumulator stats = furnace.routeStats(recipe, furnace.effectiveStats());
        if (recipe == null || !furnace.hasRequiredGear() || !furnace.meetsRecipeRequirements(recipe, stats)) {
            furnace.pourContinues = false;
            furnace.resetCycleIfActive();
            furnace.cool(stats);
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        if (!furnace.activeCycleMatches()) {
            furnace.resetCycle();
            furnace.resetBulkSpeed();
        }
        furnace.rememberHeatEnvelope(recipe, stats);

        if (!furnace.canMergeOutput(recipe.outputStack())) {
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        if (furnace.progress == 0) {
            if (furnace.pourContinues(recipe)) {
                furnace.targetReached = true;
            } else if (furnace.currentTemperature < furnace.targetTemperature(recipe, stats)) {
                furnace.targetReached = false;
            }
        }
        int requiredTemperature = furnace.requiredTemperatureForProgress(recipe, stats);
        if (furnace.currentTemperature < requiredTemperature) {
            if (furnace.payHeatTick(recipe, stats)) {
                furnace.warm(recipe, stats, requiredTemperature);
                BaseMachineBlock.setActive(level, pos, state, true);
                furnace.setChanged();
            } else {
                furnace.registerPowerDrop(recipe, stats);
                if (furnace.failureStrain >= HeatControl.FAILURE_STRAIN_THRESHOLD) {
                    furnace.tryFailCycle(recipe);
                }
                furnace.cool(stats);
                BaseMachineBlock.setActive(level, pos, state, false);
                furnace.setChanged();
            }
            return;
        }
        furnace.targetReached = true;

        int energyCost = furnace.energyCostPerTick(recipe, stats);
        if (furnace.progress == 0 && ProcessingChance.rollInstant(level, stats)) {
            int fullEnergyCost = furnace.energyCostPerCraft(recipe, stats);
            if (furnace.consumeWorkingEnergy(fullEnergyCost, true) >= fullEnergyCost) {
                furnace.startCycleIfNeeded();
                furnace.consumeWorkingEnergy(fullEnergyCost, false);
                if (furnace.process(recipe, stats)) {
                    furnace.bulkSpeed.recordProcess(furnace.activeTraits());
                }
                BaseMachineBlock.setActive(level, pos, state, true);
                furnace.setChanged();
                return;
            }
        }
        if (furnace.consumeWorkingEnergy(energyCost, true) < energyCost) {
            furnace.registerPowerDrop(recipe, stats);
            if (furnace.failureStrain >= HeatControl.FAILURE_STRAIN_THRESHOLD) {
                furnace.tryFailCycle(recipe);
            }
            furnace.cool(stats);
            BaseMachineBlock.setActive(level, pos, state, false);
            furnace.setChanged();
            return;
        }

        furnace.startCycleIfNeeded();
        furnace.consumeWorkingEnergy(energyCost, false);
        furnace.updateFailureStrain(recipe, stats);
        if (furnace.failureStrain >= HeatControl.FAILURE_STRAIN_THRESHOLD) {
            furnace.tryFailCycle(recipe);
            BaseMachineBlock.setActive(level, pos, state, false);
            furnace.setChanged();
            return;
        }
        furnace.progress++;
        if (furnace.progress >= furnace.processingTicks(recipe, stats) && furnace.process(recipe, stats)) {
            furnace.bulkSpeed.recordProcess(furnace.activeTraits());
        }

        BaseMachineBlock.setActive(level, pos, state, true);
        furnace.setChanged();
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
        return side == Direction.UP || side == Direction.DOWN ? null : energyStorage;
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        AlloyFurnaceRecipe recipe = nextRecipe();
        MachineStatAccumulator stats = routeStats(recipe, effectiveStats());
        int status = statusCode(recipe, stats);
        int energyDemand = recipe == null || !hasEnergyDemandStatus(status) ? 0 : energyCostPerTick(recipe, stats);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("alloy_furnace")
                .stage(chassis().stage())
                .state(machineInfoState(status), alloyBlockedReason(status))
                .progress(progress, currentProcessingTicks(recipe, stats))
                .energy(energyStored(), energyCapacity(), energyDemand > 0 ? -energyDemand : 0)
                .energyTelemetry(
                        lastEnergyInput(),
                        0,
                        connector.transferRate(),
                        connectorInputBottleneck(connector, energyDemand)
                )
                .heat(currentTemperature, currentTargetTemperature(recipe, stats))
                .slots(activeInputSlots(stats), MAX_INPUT_SLOTS)
                .gear(alloyGearSummary(status))
                .output(status == STATUS_OUTPUT_FULL
                        ? MachineInfoSnapshot.OutputSummary.OUTPUT_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .refinement(machineTraits())
                .build();
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
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(
                machineTraits(),
                Component.translatable("container.rngtech." + chassis().blockId())
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AlloyFurnaceMenu(containerId, playerInventory, this, menuData);
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

    public static boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public boolean isHeatCore(ItemStack stack) {
        return stack.getItem() instanceof SolidFuelBurnerPartItem part
                && part.partType() == MachinePartType.HEAT_CORE
                && part.stage() <= chassis().stage();
    }

    public boolean isAlloyCrucible(ItemStack stack) {
        return stack.getItem() instanceof AlloyCrucibleItem crucible && crucible.stage() <= chassis().stage();
    }

    public boolean isServo(ItemStack stack) {
        return stack.getItem() instanceof ServoItem servo && servo.material().stage() <= chassis().stage();
    }

    public static boolean isGearComponent(ItemStack stack) {
        return (stack.getItem() instanceof SolidFuelBurnerPartItem part && part.partType() == MachinePartType.HEAT_CORE)
                || stack.getItem() instanceof AlloyCrucibleItem
                || stack.getItem() instanceof ServoItem;
    }

    public MachineStatAccumulator effectiveStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.alloyFurnace(chassis());
        stats.apply(activeTraits());
        MegaPassiveTree.applyStats(stats, machineProgression(), MachineMasteryFamily.ALLOY_FURNACE);
        applyGearStats(stats, heatCoreStack());
        applyGearStats(stats, crucibleStack());
        applyGearStats(stats, servoStack());
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("Energy", internalEnergyStored());
        tag.putInt("CurrentTemperature", currentTemperature);
        tag.putInt("LastTargetTemperature", lastTargetTemperature);
        tag.putInt("LastSafeMaximumTemperature", lastSafeMaximumTemperature);
        tag.putInt("FailureStrain", failureStrain);
        tag.putBoolean("TargetReached", targetReached);
        for (int slot = 0; slot < activeInputs.length; slot++) {
            tag.put("ActiveInput" + slot, activeInputs[slot].saveOptional(registries));
        }
        bulkSpeed.save(tag);
        LedgerNbt.save(tag, "FluxLedger", fluxLedger);
        LedgerNbt.save(tag, "BlendLedger", blendLedger);
        tag.putInt("CadenceCycles", cadenceCycles);
        tag.putBoolean("PourContinues", pourContinues);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        progress = tag.getInt("Progress");
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        currentTemperature = Math.max(0, tag.getInt("CurrentTemperature"));
        lastTargetTemperature = Math.max(0, tag.getInt("LastTargetTemperature"));
        lastSafeMaximumTemperature = Math.max(0, tag.getInt("LastSafeMaximumTemperature"));
        failureStrain = Math.max(0, tag.getInt("FailureStrain"));
        targetReached = tag.getBoolean("TargetReached") || progress > 0;
        activeInputs = emptyInputs();
        for (int slot = 0; slot < activeInputs.length; slot++) {
            activeInputs[slot] = ItemStack.parseOptional(registries, tag.getCompound("ActiveInput" + slot));
        }
        bulkSpeed.load(tag);
        LedgerNbt.load(tag, "FluxLedger", fluxLedger);
        LedgerNbt.load(tag, "BlendLedger", blendLedger);
        cadenceCycles = Math.max(0, tag.getInt("CadenceCycles"));
        pourContinues = tag.getBoolean("PourContinues");
        clampInternalEnergy();
    }

    private MachineTraits activeTraits() {
        return MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock());
    }

    private AlloyFurnaceChassisMaterial chassis() {
        return getBlockState().getBlock() instanceof AlloyFurnaceBlock block
                ? block.material()
                : AlloyFurnaceChassisMaterial.BRONZE;
    }

    private AlloyFurnaceRecipe nextRecipe() {
        return level == null ? null : AlloyFurnaceRecipes.find(level, inputStacks()).orElse(null);
    }

    private boolean process(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (level == null) {
            return false;
        }
        ItemStack[] stacks = inputStacks();
        AlloyFurnaceRecipeInput input = new AlloyFurnaceRecipeInput(stacks);
        AlloyFurnaceRecipe.Match match = recipe.match(input).orElse(null);
        if (match == null || !recipe.matches(input, level)) {
            return false;
        }

        ItemStack baseResult = recipe.outputStack();
        ItemStack result = withSuperOutput(recipe, stats, baseResult);
        if (!canMergeOutput(result)) {
            result = baseResult;
            if (!canMergeOutput(result)) {
                return false;
            }
        }

        result = withBlendLedger(recipe, stats, result);
        int[] consumed = match.consumed().clone();
        applyFlux(recipe, stats, stacks, consumed);
        consumeInputs(consumed);
        mergeOutput(result);
        grantRecipeXp(recipe);
        lastRecipe = recipe;
        resetCycle();
        // Continuous Pour: the next blend craft starts without warming back up to its target.
        pourContinues = blend(recipe) && hasMasteryBehavior("CONTINUOUS_POUR");
        targetReached = pourContinues;
        setChanged();
        return true;
    }

    /** Super Output, guaranteed on every Super Output Cadence craft; a failure restarts the count. */
    private ItemStack withSuperOutput(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats, ItemStack baseResult) {
        int cadence = stats.intValue(MachineStat.SUPER_OUTPUT_CADENCE);
        if (cadence > 0 && recipe.allowsBonusOutput() && ++cadenceCycles >= cadence) {
            cadenceCycles = 0;
            return ProcessingChance.grow(baseResult, baseResult, baseResult.getCount());
        }
        return ProcessingChance.applySuperOutput(level, stats, recipe, baseResult, baseResult);
    }

    /** The Blend Ledger banks a share of each eligible blend craft and pays whole items once the output has room. */
    private ItemStack withBlendLedger(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats, ItemStack result) {
        if (!blendLedgerActive(recipe, stats)) {
            return result;
        }
        ItemStack base = recipe.outputStack();
        blendLedger.add(base.getItem(), AscendancyFormulas.ledgerShare(stats, base.getCount(), false));
        int payable = blendLedger.payable(base.getItem());
        ItemStack paid = ProcessingChance.grow(result, base, payable);
        if (paid == result || !canMergeOutput(paid)) {
            return result;
        }
        blendLedger.pay(base.getItem(), payable);
        return paid;
    }

    private boolean blendLedgerActive(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return blend(recipe) && recipe.allowsBonusOutput() && stats.value(MachineStat.LEDGER_RATE) > 0.0;
    }

    /** Flux banks a share of one unit of the largest input and skips a whole unit once banked. */
    private void applyFlux(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats, ItemStack[] stacks, int[] consumed) {
        if (!recipe.allowsBonusOutput() || stats.value(MachineStat.FLUX_RATE) <= 0.0) {
            return;
        }
        int slot = savableSlot(recipe, stacks, consumed);
        if (slot < 0) {
            return;
        }
        Item item = stacks[slot].getItem();
        fluxLedger.add(item, AscendancyFormulas.fluxShare(stats, directIngot(recipe) && hasMasteryBehavior("REACTIVE_FLUX")));
        if (fluxLedger.payable(item) >= 1) {
            consumed[slot]--;
            fluxLedger.pay(item, 1);
        }
    }

    /** A slot the craft takes the largest ingredient from, when that ingredient needs two or more units. */
    private static int savableSlot(AlloyFurnaceRecipe recipe, ItemStack[] stacks, int[] consumed) {
        int ingredient = AscendancyFormulas.savableIngredient(recipe.ingredients().stream().mapToInt(CountedIngredient::count).toArray());
        if (ingredient < 0) {
            return -1;
        }
        CountedIngredient counted = recipe.ingredients().get(ingredient);
        for (int slot = 0; slot < Math.min(consumed.length, stacks.length); slot++) {
            if (consumed[slot] > 0 && counted.test(stacks[slot])) {
                return slot;
            }
        }
        return -1;
    }

    /** The flux or blend ledger's progress toward its next unit, per thousand, or -1 when neither applies. */
    private int ledgerDisplay(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (recipe == null || !recipe.allowsBonusOutput()) {
            return -1;
        }
        if (blendLedgerActive(recipe, stats)) {
            return LedgerNbt.permille(blendLedger.progress(recipe.outputStack().getItem()));
        }
        if (stats.value(MachineStat.FLUX_RATE) <= 0.0) {
            return -1;
        }
        ItemStack[] stacks = inputStacks();
        AlloyFurnaceRecipe.Match match = recipe.match(new AlloyFurnaceRecipeInput(stacks)).orElse(null);
        int slot = match == null ? -1 : savableSlot(recipe, stacks, match.consumed());
        return slot < 0 ? -1 : LedgerNbt.permille(fluxLedger.progress(stacks[slot].getItem()));
    }

    private static boolean blend(AlloyFurnaceRecipe recipe) {
        return "blend".equals(recipe.mode());
    }

    private static boolean directIngot(AlloyFurnaceRecipe recipe) {
        return "direct_ingot".equals(recipe.mode());
    }

    /** Tempered Crucible's stability on blend recipes; recipe speed applies to processing time instead. */
    private MachineStatAccumulator routeStats(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (recipe != null && blend(recipe) && hasMasteryBehavior("TEMPERED_CRUCIBLE")) {
            stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.TEMPERATURE_STABILITY, ModifierOperation.MORE, 1.4));
        }
        return stats;
    }

    /** Blend Speed on blend recipes; Blend Reversal slows direct-ingot recipes by 15%. */
    private double routeSpeed(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (blend(recipe)) {
            return 1.0 + Math.max(0.0, stats.value(MachineStat.BLEND_SPEED)) / 100.0;
        }
        return directIngot(recipe) && hasMasteryBehavior("BLEND_REVERSAL") ? 0.85 : 1.0;
    }

    /** Master Blend disables direct-ingot recipes. */
    private boolean routeDisabled(AlloyFurnaceRecipe recipe) {
        return directIngot(recipe) && hasMasteryBehavior("MASTER_BLEND");
    }

    private boolean pourContinues(AlloyFurnaceRecipe recipe) {
        return pourContinues && blend(recipe) && hasMasteryBehavior("CONTINUOUS_POUR");
    }

    /** Cold Mixing lowers every temperature of a blend recipe. */
    private static int heatRelief(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return blend(recipe) ? Math.max(0, stats.intValue(MachineStat.BLEND_HEAT_REDUCTION)) : 0;
    }

    private static int targetTemperature(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return Math.max(0, recipe.targetTemperature() - heatRelief(recipe, stats));
    }

    private static int minimumTemperature(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return Math.max(0, recipe.minimumTemperature() - heatRelief(recipe, stats));
    }

    private static int safeMaximumTemperature(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return Math.max(targetTemperature(recipe, stats), recipe.safeMaximumTemperature() - heatRelief(recipe, stats));
    }

    /** Blend Reversal: the blend recipe for a direct-ingot recipe's output, at no more than the direct-ingot recipe makes. */
    private ItemStack reversalBlend(AlloyFurnaceRecipe direct) {
        if (level == null) {
            return ItemStack.EMPTY;
        }
        return reversalBlends.computeIfAbsent(direct, recipe -> {
            for (RecipeHolder<AlloyFurnaceRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOY_FURNACE_TYPE.get())) {
                ItemStack blend = holder.value().outputStack();
                Optional<FurnaceRecipe> smelt = blend(holder.value()) ? FurnaceRecipes.find(level, blend) : Optional.empty();
                if (smelt.isPresent() && smelt.get().outputStack().is(recipe.outputStack().getItem())) {
                    return blend.copyWithCount(Math.min(blend.getCount(), recipe.outputStack().getCount()));
                }
            }
            return ItemStack.EMPTY;
        });
    }

    /**
     * Recipe Lock: automation may add only the current recipe's ingredients, or the last recipe's, each until it is one
     * craft ahead of the scarcest other ingredient.
     */
    private int recipeLockAllowance(ItemStack stack) {
        AlloyFurnaceRecipe locked = hasMasteryBehavior("RECIPE_LOCK") ? Optional.ofNullable(nextRecipe()).orElse(lastRecipe) : null;
        if (locked == null) {
            return stack.getCount();
        }
        List<CountedIngredient> ingredients = locked.ingredients();
        int matched = -1;
        for (int index = 0; index < ingredients.size() && matched < 0; index++) {
            if (ingredients.get(index).test(stack)) {
                matched = index;
            }
        }
        if (matched < 0) {
            return 0;
        }
        ItemStack[] stacks = inputStacks();
        int scarcest = Integer.MAX_VALUE;
        for (int index = 0; index < ingredients.size(); index++) {
            if (index != matched) {
                scarcest = Math.min(scarcest, held(ingredients.get(index), stacks) / ingredients.get(index).count());
            }
        }
        if (scarcest == Integer.MAX_VALUE) {
            return stack.getCount();
        }
        int limit = (scarcest + 1) * ingredients.get(matched).count() - held(ingredients.get(matched), stacks);
        return Math.max(0, Math.min(stack.getCount(), limit));
    }

    private static int held(CountedIngredient ingredient, ItemStack[] stacks) {
        int held = 0;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && ingredient.test(stack)) {
                held += stack.getCount();
            }
        }
        return held;
    }

    private void grantRecipeXp(AlloyFurnaceRecipe recipe) {
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
        return MachineMasteryFamily.ALLOY_FURNACE;
    }

    @Override
    public int ascendancyEntryStage() {
        return chassis().stage();
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

    private boolean tryFailCycle(AlloyFurnaceRecipe recipe) {
        if (level == null || !failureEnabled(recipe, effectiveStats())) {
            return false;
        }
        ItemStack[] stacks = inputStacks();
        AlloyFurnaceRecipeInput input = new AlloyFurnaceRecipeInput(stacks);
        AlloyFurnaceRecipe.Match match = recipe.match(input).orElse(null);
        if (match == null || !recipe.matches(input, level)) {
            return false;
        }

        ItemStack result = failureResult(recipe);
        if (!canMergeOutput(result)) {
            return false;
        }

        int[] consumed = match.consumed().clone();
        // Dross Skimming keeps one unit of the largest input.
        int skimmed = hasMasteryBehavior("DROSS_SKIMMING") ? savableSlot(recipe, stacks, consumed) : -1;
        if (skimmed >= 0) {
            consumed[skimmed]--;
        }
        consumeInputs(consumed);
        mergeOutput(result);
        cadenceCycles = 0;
        pourContinues = false;
        resetCycle();
        resetBulkSpeed();
        setChanged();
        return true;
    }

    /** Blend Reversal turns a failed direct-ingot craft into the blend its inputs would make. */
    private ItemStack failureResult(AlloyFurnaceRecipe recipe) {
        ItemStack blend = directIngot(recipe) && hasMasteryBehavior("BLEND_REVERSAL") ? reversalBlend(recipe) : ItemStack.EMPTY;
        return blend.isEmpty() ? recipe.failureStack() : blend.copy();
    }

    private boolean meetsRecipeRequirements(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return !routeDisabled(recipe)
                && chassis().stage() >= recipe.minimumComponentStage()
                && crucibleStage() >= recipe.minimumComponentStage()
                && effectiveHeat(stats) >= targetTemperature(recipe, stats)
                && (failureEnabled(recipe, stats)
                        || stats.value(MachineStat.TEMPERATURE_STABILITY) >= recipe.requiredTemperatureStability());
    }

    private boolean hasRequiredGear() {
        return hasHeatCore() && hasCrucible();
    }

    private boolean hasHeatCore() {
        return isHeatCore(heatCoreStack());
    }

    private boolean hasCrucible() {
        return isAlloyCrucible(crucibleStack());
    }

    private boolean hasBatteryCell() {
        return isBatteryCell(batteryCellStack());
    }

    private boolean hasPowerGrace() {
        return activeTraits().hasBehavior(MachineBehavior.POWER_GRACE)
                || MachineImplicitCatalog.hasBehavior(heatCoreStack(), MachineBehavior.POWER_GRACE)
                || MachineImplicitCatalog.hasBehavior(crucibleStack(), MachineBehavior.POWER_GRACE)
                || MachineImplicitCatalog.hasBehavior(servoStack(), MachineBehavior.POWER_GRACE);
    }

    private int crucibleStage() {
        return crucibleStack().getItem() instanceof AlloyCrucibleItem crucible ? crucible.stage() : 0;
    }

    private int effectiveHeat(MachineStatAccumulator stats) {
        return hasHeatCore() ? Math.max(0, stats.intValue(MachineStat.MAX_TEMPERATURE)) : 0;
    }

    private int processingTicks(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return Math.max(1, (int) Math.ceil(stats.adjustedHeatProcessingTicks(recipe.processingTicks()) / routeSpeed(recipe, stats)));
    }

    private int currentProcessingTicks(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null || !meetsRecipeRequirements(recipe, stats) ? 0 : processingTicks(recipe, stats);
    }

    private int energyCostPerTick(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        int adjustedTicks = processingTicks(recipe, stats);
        int totalEnergy = stats.adjustedEnergyCost(recipe.energy());
        return Math.max(1, (int) Math.ceil(totalEnergy / (double) adjustedTicks));
    }

    private int energyCostPerCraft(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return stats.adjustedEnergyCost(recipe.energy());
    }

    private int statusCode(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (!hasHeatCore()) {
            return STATUS_MISSING_HEAT_CORE;
        }
        if (!hasCrucible()) {
            return STATUS_MISSING_CRUCIBLE;
        }
        if (!hasAnyInput()) {
            return STATUS_NO_INPUT;
        }
        if (recipe == null) {
            return STATUS_INVALID_RECIPE;
        }
        if (routeDisabled(recipe)) {
            return STATUS_ROUTE_DISABLED;
        }
        if (chassis().stage() < recipe.minimumComponentStage() || crucibleStage() < recipe.minimumComponentStage()) {
            return STATUS_BLOCKED_STAGE;
        }
        if (effectiveHeat(stats) < targetTemperature(recipe, stats)) {
            return STATUS_HEAT_LOW;
        }
        if (!failureEnabled(recipe, stats)
                && stats.value(MachineStat.TEMPERATURE_STABILITY) < recipe.requiredTemperatureStability()) {
            return STATUS_STABILITY_LOW;
        }
        if (!canMergeOutput(recipe.outputStack())) {
            return STATUS_OUTPUT_FULL;
        }
        if (failureEnabled(recipe, stats)
                && failureStrain >= HeatControl.FAILURE_STRAIN_THRESHOLD
                && !canMergeOutput(recipe.failureStack())) {
            return STATUS_OUTPUT_FULL;
        }
        if (powerSensitiveActive(recipe, stats)) {
            return STATUS_POWER_DROP;
        }
        if (failureStrain > 0 && failureEnabled(recipe, stats)) {
            return STATUS_FAILURE_RISK;
        }
        if (currentTemperature < requiredTemperatureForProgress(recipe, stats)) {
            int energyCost = energyCostPerTick(recipe, stats);
            return consumeWorkingEnergy(energyCost, true) < energyCost ? STATUS_NO_POWER : STATUS_WARMING;
        }
        int energyCost = energyCostPerTick(recipe, stats);
        return consumeWorkingEnergy(energyCost, true) < energyCost ? STATUS_NO_POWER : STATUS_READY;
    }

    private MachineInfoSnapshot.WorkState machineInfoState(int status) {
        return switch (status) {
            case STATUS_READY, STATUS_WARMING, STATUS_FAILURE_RISK -> getBlockState().getValue(BaseMachineBlock.ACTIVE)
                    ? MachineInfoSnapshot.WorkState.RUNNING
                    : MachineInfoSnapshot.WorkState.IDLE;
            case STATUS_NO_INPUT -> MachineInfoSnapshot.WorkState.IDLE;
            case STATUS_POWER_DROP -> MachineInfoSnapshot.WorkState.PAUSED;
            default -> MachineInfoSnapshot.WorkState.BLOCKED;
        };
    }

    private static boolean hasEnergyDemandStatus(int status) {
        return status == STATUS_READY
                || status == STATUS_WARMING
                || status == STATUS_NO_POWER
                || status == STATUS_POWER_DROP
                || status == STATUS_FAILURE_RISK;
    }

    private MachineInfoSnapshot.BlockedReason alloyBlockedReason(int status) {
        return switch (status) {
            case STATUS_MISSING_HEAT_CORE -> MachineInfoSnapshot.BlockedReason.MISSING_HEAT_CORE;
            case STATUS_MISSING_CRUCIBLE -> MachineInfoSnapshot.BlockedReason.MISSING_CRUCIBLE;
            case STATUS_NO_INPUT -> MachineInfoSnapshot.BlockedReason.NO_INPUT;
            case STATUS_INVALID_RECIPE -> MachineInfoSnapshot.BlockedReason.INVALID_RECIPE;
            case STATUS_BLOCKED_STAGE -> MachineInfoSnapshot.BlockedReason.BLOCKED_TIER;
            case STATUS_HEAT_LOW -> MachineInfoSnapshot.BlockedReason.HEAT_LOW;
            case STATUS_STABILITY_LOW -> MachineInfoSnapshot.BlockedReason.STABILITY_LOW;
            case STATUS_OUTPUT_FULL -> MachineInfoSnapshot.BlockedReason.OUTPUT_FULL;
            case STATUS_NO_POWER, STATUS_POWER_DROP -> MachineInfoSnapshot.BlockedReason.NO_POWER;
            default -> MachineInfoSnapshot.BlockedReason.NONE;
        };
    }

    private MachineInfoSnapshot.GearSummary alloyGearSummary(int status) {
        if (status == STATUS_MISSING_HEAT_CORE) {
            return MachineInfoSnapshot.GearSummary.MISSING_HEAT_CORE;
        }
        if (!hasBatteryCell()) {
            return MachineInfoSnapshot.GearSummary.MISSING_BATTERY_CELL;
        }
        return MachineInfoSnapshot.GearSummary.BATTERY_CELL_INSTALLED;
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

    private void consumeInputs(int[] consumed) {
        for (int slot = 0; slot < Math.min(consumed.length, MAX_INPUT_SLOTS); slot++) {
            for (int count = 0; count < consumed[slot]; count++) {
                consumeOne(slot);
            }
        }
    }

    private void consumeOne(int slot) {
        ItemStack input = processInventory.getStackInSlot(slot);
        ItemStack remainder = input.getCraftingRemainingItem();
        input.shrink(1);
        if (!remainder.isEmpty()) {
            if (input.isEmpty()) {
                processInventory.setStackInSlot(slot, remainder);
            } else if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder);
            }
        }
    }

    private int requiredTemperatureForProgress(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return targetReached ? minimumTemperature(recipe, stats) : targetTemperature(recipe, stats);
    }

    private void warm(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats, int requiredTemperature) {
        currentTemperature = Math.min(
                Math.min(requiredTemperature, stats.intValue(MachineStat.MAX_TEMPERATURE)),
                currentTemperature + HeatControl.warmupRate(stats)
        );
        if (currentTemperature >= targetTemperature(recipe, stats)) {
            targetReached = true;
        }
    }

    private void rememberHeatEnvelope(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        lastTargetTemperature = targetTemperature(recipe, stats);
        lastSafeMaximumTemperature = safeMaximumTemperature(recipe, stats);
    }

    private void clearHeatEnvelope() {
        lastTargetTemperature = 0;
        lastSafeMaximumTemperature = 0;
    }

    private boolean hasCooldownHeat() {
        return currentTemperature > HeatControl.AMBIENT_TEMPERATURE && lastSafeMaximumTemperature > 0;
    }

    private int currentTargetTemperature(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null ? hasCooldownHeat() ? lastTargetTemperature : 0 : targetTemperature(recipe, stats);
    }

    private int currentSafeMaximumTemperature(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null ? hasCooldownHeat() ? lastSafeMaximumTemperature : 0 : safeMaximumTemperature(recipe, stats);
    }

    private int currentOverheatTemperature(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        int safeMaximumTemperature = currentSafeMaximumTemperature(recipe, stats);
        return safeMaximumTemperature <= 0
                ? 0
                : HeatControl.effectiveOverheatTemperature(safeMaximumTemperature, stats);
    }

    private void cool(MachineStatAccumulator stats) {
        currentTemperature = Math.max(HeatControl.AMBIENT_TEMPERATURE, currentTemperature - HeatControl.coolingRate(stats));
        if (currentTemperature <= HeatControl.AMBIENT_TEMPERATURE) {
            clearHeatEnvelope();
        }
    }

    /** Heat Economy makes warmup ticks 30% cheaper. */
    private boolean payHeatTick(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        int energyCost = energyCostPerTick(recipe, stats);
        if (hasMasteryBehavior("HEAT_ECONOMY")) {
            energyCost = Math.max(1, (int) Math.ceil(energyCost * 0.7));
        }
        if (consumeWorkingEnergy(energyCost, true) < energyCost) {
            return false;
        }
        consumeWorkingEnergy(energyCost, false);
        return true;
    }

    private void updateFailureStrain(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (!failureEnabled(recipe, stats)) {
            failureStrain = 0;
            return;
        }
        int simulatedTemperature = HeatControl.simulatedTemperature(
                currentTemperature,
                targetTemperature(recipe, stats),
                recipe.requiredTemperatureStability(),
                stats,
                worldPosition,
                level == null ? 0L : level.getGameTime(),
                0
        );
        int addedStrain = HeatControl.failureStrainFromTemperature(
                simulatedTemperature,
                minimumTemperature(recipe, stats),
                HeatControl.effectiveOverheatTemperature(safeMaximumTemperature(recipe, stats), stats),
                recipe.requiredTemperatureStability(),
                stats
        );
        if (addedStrain > 0) {
            failureStrain = Math.min(HeatControl.FAILURE_STRAIN_THRESHOLD, failureStrain + addedStrain);
            return;
        }
        failureStrain = Math.max(0, failureStrain - HeatControl.strainRecovery(stats));
    }

    private void registerPowerDrop(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (!recipe.powerSensitive() || progress <= 0 || !failureEnabled(recipe, stats)) {
            return;
        }
        int strain = hasPowerGrace() ? HeatControl.POWER_DROP_STRAIN / 2 : HeatControl.POWER_DROP_STRAIN;
        // Steady Supply halves power-drop strain.
        failureStrain = Math.min(HeatControl.FAILURE_STRAIN_THRESHOLD, failureStrain + (hasMasteryBehavior("STEADY_SUPPLY") ? strain / 2 : strain));
    }

    private boolean failureEnabled(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        return recipe != null
                && recipe.hasFailureOutput()
                && recipe.minimumComponentStage() >= 4
                && stats.intValue(MachineStat.MAX_TEMPERATURE) >= targetTemperature(recipe, stats);
    }

    private boolean powerSensitiveActive(AlloyFurnaceRecipe recipe, MachineStatAccumulator stats) {
        if (recipe == null || !recipe.powerSensitive() || progress <= 0 || !failureEnabled(recipe, stats)) {
            return false;
        }
        int energyCost = energyCostPerTick(recipe, stats);
        return consumeWorkingEnergy(energyCost, true) < energyCost;
    }

    private void startCycleIfNeeded() {
        if (progress != 0) {
            return;
        }
        activeInputs = Arrays.stream(inputStacks())
                .map(AlloyFurnaceBlockEntity::singleCopy)
                .toArray(ItemStack[]::new);
        failureStrain = 0;
    }

    private boolean activeCycleMatches() {
        if (progress == 0) {
            return true;
        }
        ItemStack[] current = inputStacks();
        for (int slot = 0; slot < activeInputs.length; slot++) {
            if (activeInputs[slot].isEmpty() != current[slot].isEmpty()) {
                return false;
            }
            if (!activeInputs[slot].isEmpty() && !ItemStack.isSameItemSameComponents(activeInputs[slot], current[slot])) {
                return false;
            }
        }
        return true;
    }

    private void resetCycleIfActive() {
        boolean changed = false;
        if (progress != 0 || failureStrain != 0 || targetReached || hasActiveInputSnapshot()) {
            resetCycle();
            changed = true;
        }
        changed |= bulkSpeed.reset();
        if (changed) {
            setChanged();
        }
    }

    private void resetCycle() {
        progress = 0;
        failureStrain = 0;
        targetReached = false;
        activeInputs = emptyInputs();
    }

    private void resetBulkSpeed() {
        if (bulkSpeed.reset()) {
            setChanged();
        }
    }

    private void applyGearStats(MachineStatAccumulator stats, ItemStack stack) {
        if (stack.getItem() instanceof MachinePartItem) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        }
    }

    private int activeInputSlots() {
        return activeInputSlots(effectiveStats());
    }

    private int activeInputSlots(MachineStatAccumulator stats) {
        return Math.max(1, Math.min(MAX_INPUT_SLOTS, stats.intValue(MachineStat.INPUT_SLOTS)));
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

    private MachineInfoSnapshot.EnergyBottleneck connectorInputBottleneck(
            AdjacentEnergyConnector.Info connector,
            int energyDemand
    ) {
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

    private IEnergyStorage batteryCellEnergyStorage() {
        ItemStack cell = batteryCellStack();
        return cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
    }

    private boolean hasAnyInput() {
        for (int slot = 0; slot < activeInputSlots(); slot++) {
            if (!processInventory.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasActiveInputSnapshot() {
        return Arrays.stream(activeInputs).anyMatch(stack -> !stack.isEmpty());
    }

    private ItemStack[] inputStacks() {
        ItemStack[] stacks = emptyInputs();
        int activeSlots = activeInputSlots();
        for (int slot = 0; slot < activeSlots; slot++) {
            stacks[slot] = processInventory.getStackInSlot(slot);
        }
        return stacks;
    }

    private ItemStack heatCoreStack() {
        return gearInventory.getStackInSlot(SLOT_HEAT_CORE);
    }

    private ItemStack crucibleStack() {
        return gearInventory.getStackInSlot(SLOT_CRUCIBLE);
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private ItemStack servoStack() {
        return gearInventory.getStackInSlot(SLOT_SERVO);
    }

    private static ItemStack[] emptyInputs() {
        ItemStack[] stacks = new ItemStack[MAX_INPUT_SLOTS];
        Arrays.fill(stacks, ItemStack.EMPTY);
        return stacks;
    }

    private static ItemStack singleCopy(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static boolean isInputSlot(int slot) {
        return slot >= SLOT_INPUT_START && slot < SLOT_INPUT_START + MAX_INPUT_SLOTS;
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private final class InputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return activeInputSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(mappedSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int allowed = recipeLockAllowance(stack);
            if (allowed <= 0) {
                return stack;
            }
            if (allowed >= stack.getCount()) {
                return processInventory.insertItem(mappedSlot(slot), stack, simulate);
            }
            ItemStack rejected = processInventory.insertItem(mappedSlot(slot), stack.copyWithCount(allowed), simulate);
            return stack.copyWithCount(stack.getCount() - allowed + rejected.getCount());
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(mappedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return processInventory.isItemValid(mappedSlot(slot), stack) && recipeLockAllowance(stack) > 0;
        }

        private int mappedSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
            }
            return SLOT_INPUT_START + slot;
        }
    }

    private final class OutputItemHandler implements IItemHandler {
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
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return processInventory.extractItem(mappedSlot(slot), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(mappedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        private int mappedSlot(int slot) {
            if (slot != 0) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
            }
            return SLOT_OUTPUT;
        }
    }

    private final class AlloyFurnaceEnergyStorage implements IEnergyStorage {
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
            if (internalEnergyStored() < internalEnergyCapacity()) {
                return true;
            }
            IEnergyStorage cell = batteryCellEnergyStorage();
            return cell != null && cell.canReceive() && cell.getEnergyStored() < cell.getMaxEnergyStored();
        }
    }
}
