package com.rngtech.content.blockentity;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.ResonanceCalibratorBlock;
import com.rngtech.content.calibration.CalibrationRecipeResult;
import com.rngtech.content.calibration.CalibrationValueRange;
import com.rngtech.content.calibration.ResonanceCalibratorChassis;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.CalibrationGearItem;
import com.rngtech.content.item.CalibrationPatternItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.content.menu.ResonanceCalibratorMenu;
import com.rngtech.content.recipe.CalibrationRecipe;
import com.rngtech.content.recipe.CalibrationRecipeInput;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ResonanceCalibratorBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineMasteryHost {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_PATTERN = 1;
    public static final int SLOT_CATALYST = 2;
    public static final int SLOT_STABILIZER = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int PROCESS_SLOT_COUNT = 5;
    public static final int SLOT_BATTERY_CELL = 0;
    public static final int SLOT_RESONANCE_COIL = 1;
    public static final int SLOT_CONTROL_BOARD = 2;
    public static final int SLOT_STABILIZER_MATRIX = 3;
    public static final int SLOT_PATTERN_STORAGE = 4;
    public static final int PATTERN_SLOT_COUNT = 6;
    public static final int GEAR_SLOT_COUNT = SLOT_PATTERN_STORAGE + PATTERN_SLOT_COUNT;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_RESONANCE_COIL = 1;
    public static final int STATUS_MISSING_CONTROL_BOARD = 2;
    public static final int STATUS_MISSING_PATTERN = 3;
    public static final int STATUS_MISSING_CATALYST = 4;
    public static final int STATUS_NO_INPUT = 5;
    public static final int STATUS_INVALID_RECIPE = 6;
    public static final int STATUS_INSUFFICIENT_STAGE = 7;
    public static final int STATUS_OUTPUT_FULL = 8;
    public static final int STATUS_NO_POWER = 9;

    private static final double NO_BATTERY_PROCESSING_SPEED = 0.85;
    private static final int NO_BATTERY_QUALITY_PENALTY = 10;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_STABILITY_MIN = 4;
    private static final int DATA_STABILITY_MAX = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_FAMILY = 7;
    private static final int DATA_LANES = 8;
    private static final int DATA_PROCESSING_SPEED = 9;
    private static final int DATA_EFFICIENCY = 10;
    private static final int DATA_ENERGY_USAGE = 11;
    private static final int DATA_ENERGY_CAPACITY_STAT = 12;
    private static final int DATA_ENERGY_TRANSFER = 13;
    private static final int DATA_STABILITY = 14;
    private static final int DATA_CALIBRATION_QUALITY = 15;
    private static final int DATA_CALIBRATION_PRECISION = 16;
    private static final int DATA_CATALYST_EFFICIENCY = 17;
    private static final int DATA_REFINEMENT_POTENTIAL_BONUS = 18;
    private static final int DATA_REFINEMENT_POTENTIAL = 19;
    private static final int DATA_SELECTED_PATTERN = 20;
    private static final int DATA_MACHINE_PROGRESSION_START = DATA_SELECTED_PATTERN + 1;
    private static final int DATA_COUNT = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int STAT_SCALE = 100;
    private static final int[] AUTOMATION_INPUT_SLOTS = {SLOT_INPUT, SLOT_CATALYST, SLOT_STABILIZER};

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_INPUT, SLOT_CATALYST, SLOT_STABILIZER -> true;
                default -> false;
            };
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
            if (slot != SLOT_OUTPUT) {
                resetCycle();
                resetBulkSpeed();
            }
            setChanged();
        }
    };
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (isPatternSlot(slot)) {
                return isCalibrationPattern(stack);
            }
            return switch (slot) {
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_RESONANCE_COIL -> isResonanceCoil(stack);
                case SLOT_CONTROL_BOARD -> isControlBoard(stack);
                case SLOT_STABILIZER_MATRIX -> isStabilizerMatrix(stack);
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
    private final IItemHandler inputHandler = new AutomationInputHandler();
    private final IItemHandler outputHandler = new ProcessItemHandler(SLOT_OUTPUT, false, true);
    private final IEnergyStorage energyStorage = new CalibratorEnergyStorage();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_MACHINE_PROGRESSION_START
                    && index < DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT) {
                return MasteryMenuSupport.get(
                        machineProgression(),
                        index - DATA_MACHINE_PROGRESSION_START,
                        ResonanceCalibratorBlockEntity.this::effectiveStats
                );
            }
            MachineStatAccumulator stats = effectiveStats();
            CalibrationRecipe recipe = nextRecipe();
            CalibrationValueRange range = currentStabilityRange(recipe, stats);
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> currentProcessingTicks(recipe, stats);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_STABILITY_MIN -> range.min();
                case DATA_STABILITY_MAX -> range.max();
                case DATA_STATUS -> statusCode(recipe, stats);
                case DATA_FAMILY -> recipe == null ? -1 : recipe.family().ordinal();
                case DATA_LANES -> chassis().lanes();
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_STABILITY -> scaledStat(stats, MachineStat.STABILITY);
                case DATA_CALIBRATION_QUALITY -> scaledStat(stats, MachineStat.CALIBRATION_QUALITY);
                case DATA_CALIBRATION_PRECISION -> scaledStat(stats, MachineStat.CALIBRATION_PRECISION);
                case DATA_CATALYST_EFFICIENCY -> scaledStat(stats, MachineStat.CATALYST_EFFICIENCY);
                case DATA_REFINEMENT_POTENTIAL_BONUS -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL_BONUS);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_SELECTED_PATTERN -> selectedPattern;
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
    private int selectedPattern;
    private ItemStack activeInput = ItemStack.EMPTY;
    private ItemStack activePattern = ItemStack.EMPTY;
    private ItemStack activeCatalyst = ItemStack.EMPTY;
    private ItemStack activeStabilizer = ItemStack.EMPTY;

    public ResonanceCalibratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.RESONANCE_CALIBRATOR.get(),
                pos,
                blockState,
                MachineType.RESONANCE_CALIBRATOR,
                SLOT_INPUT,
                SLOT_CATALYST,
                SLOT_OUTPUT
        );
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResonanceCalibratorBlockEntity calibrator) {
        MachineStatAccumulator stats = calibrator.effectiveStats();
        CalibrationRecipe recipe = calibrator.nextRecipe();
        if (recipe == null || !calibrator.hasRequiredGear() || !calibrator.hasRequiredRecipeStage(recipe)) {
            calibrator.resetCycleIfActive();
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }
        if (!calibrator.activeCycleMatches()) {
            calibrator.resetCycle();
            calibrator.resetBulkSpeed();
        }
        if (!calibrator.canMergeOutput(recipe.outputStack())) {
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        int energyCost = calibrator.energyCostPerTick(recipe, stats);
        if (calibrator.progress == 0 && ProcessingChance.rollInstant(level, stats)) {
            int fullEnergyCost = calibrator.energyCostPerCraft(recipe, stats);
            if (calibrator.consumeWorkingEnergy(fullEnergyCost, true) >= fullEnergyCost) {
                calibrator.startCycleIfNeeded();
                calibrator.consumeWorkingEnergy(fullEnergyCost, false);
                if (calibrator.process(recipe, stats)) {
                    calibrator.bulkSpeed.recordProcess(calibrator.activeTraits());
                }
                BaseMachineBlock.setActive(level, pos, state, true);
                calibrator.setChanged();
                return;
            }
        }
        if (calibrator.consumeWorkingEnergy(energyCost, true) < energyCost) {
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        calibrator.startCycleIfNeeded();
        calibrator.consumeWorkingEnergy(energyCost, false);
        calibrator.progress++;
        if (calibrator.progress >= calibrator.processingTicks(recipe, stats)) {
            if (calibrator.process(recipe, stats)) {
                calibrator.bulkSpeed.recordProcess(calibrator.activeTraits());
            }
        }

        BaseMachineBlock.setActive(level, pos, state, true);
        calibrator.setChanged();
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

    public int selectedPattern() {
        return selectedPattern;
    }

    public void selectPattern(int patternIndex) {
        int clampedIndex = clampPatternIndex(patternIndex);
        if (selectedPattern == clampedIndex) {
            return;
        }
        selectedPattern = clampedIndex;
        resetCycle();
        resetBulkSpeed();
        setChanged();
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return energyStorage;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == Direction.DOWN) {
            return outputHandler;
        }
        return side == null ? processInventory : inputHandler;
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
        return new ResonanceCalibratorMenu(containerId, playerInventory, this, menuData);
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
        ResonanceCalibratorChassis chassis = chassis();
        MachineStatAccumulator stats = MachineBaseStatCatalog.resonanceCalibrator(chassis);
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
        MegaPassiveTree.applyStats(stats, machineProgression(), masteryFamily());
        applyGearStats(stats, resonanceCoilStack());
        applyGearStats(stats, controlBoardStack());
        applyGearStats(stats, stabilizerMatrixStack());
        if (!hasBatteryCell()) {
            stats.apply(new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.PROCESSING_SPEED,
                    ModifierOperation.LESS,
                    NO_BATTERY_PROCESSING_SPEED
            ));
            stats.apply(new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.CALIBRATION_QUALITY,
                    ModifierOperation.DECREASED_PERCENT,
                    NO_BATTERY_QUALITY_PENALTY
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
        tag.putInt("SelectedPattern", selectedPattern);
        tag.put("ActiveInput", activeInput.saveOptional(registries));
        tag.put("ActivePattern", activePattern.saveOptional(registries));
        tag.put("ActiveCatalyst", activeCatalyst.saveOptional(registries));
        tag.put("ActiveStabilizer", activeStabilizer.saveOptional(registries));
        bulkSpeed.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        CompoundTag gearTag = expandedGearInventoryTag(tag.getCompound("GearInventory"));
        boolean legacyProcessPattern = !tag.contains("SelectedPattern")
                && isCalibrationPattern(processInventory.getStackInSlot(SLOT_PATTERN));
        gearInventory.deserializeNBT(registries, gearTag);
        progress = tag.getInt("Progress");
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        selectedPattern = clampPatternIndex(tag.getInt("SelectedPattern"));
        if (legacyProcessPattern) {
            migrateLegacyProcessPattern();
        }
        activeInput = ItemStack.parseOptional(registries, tag.getCompound("ActiveInput"));
        activePattern = ItemStack.parseOptional(registries, tag.getCompound("ActivePattern"));
        activeCatalyst = ItemStack.parseOptional(registries, tag.getCompound("ActiveCatalyst"));
        activeStabilizer = ItemStack.parseOptional(registries, tag.getCompound("ActiveStabilizer"));
        bulkSpeed.load(tag);
        clampInternalEnergy();
    }

    private static CompoundTag expandedGearInventoryTag(CompoundTag gearTag) {
        CompoundTag expandedTag = gearTag.copy();
        if (expandedTag.getInt("Size") < GEAR_SLOT_COUNT) {
            expandedTag.putInt("Size", GEAR_SLOT_COUNT);
        }
        return expandedTag;
    }

    private void migrateLegacyProcessPattern() {
        ItemStack legacyPattern = processInventory.getStackInSlot(SLOT_PATTERN);
        for (int index = 0; index < PATTERN_SLOT_COUNT; index++) {
            int slot = SLOT_PATTERN_STORAGE + index;
            if (gearInventory.getStackInSlot(slot).isEmpty()) {
                gearInventory.setStackInSlot(slot, legacyPattern.copy());
                processInventory.setStackInSlot(SLOT_PATTERN, ItemStack.EMPTY);
                selectedPattern = index;
                return;
            }
        }
    }

    public static boolean isCalibrationPattern(ItemStack stack) {
        return stack.getItem() instanceof CalibrationPatternItem;
    }

    public static boolean isPatternSlot(int slot) {
        return slot >= SLOT_PATTERN_STORAGE && slot < SLOT_PATTERN_STORAGE + PATTERN_SLOT_COUNT;
    }

    public static boolean isResonanceCoil(ItemStack stack) {
        return stack.getItem() instanceof CalibrationGearItem gear && gear.partType() == MachinePartType.RESONANCE_COIL;
    }

    public static boolean isControlBoard(ItemStack stack) {
        return stack.getItem() instanceof CalibrationGearItem gear && gear.partType() == MachinePartType.CONTROL_BOARD;
    }

    public static boolean isStabilizerMatrix(ItemStack stack) {
        return stack.getItem() instanceof CalibrationGearItem gear && gear.partType() == MachinePartType.STABILIZER_MATRIX;
    }

    public static boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    private ResonanceCalibratorChassis chassis() {
        return getBlockState().getBlock() instanceof ResonanceCalibratorBlock block
                ? block.chassis()
                : ResonanceCalibratorChassis.IRON;
    }

    private CalibrationRecipe nextRecipe() {
        return level == null
                ? null
                : CalibrationRecipes.find(level, inputStack(), patternStack(), catalystStack(), stabilizerStack()).orElse(null);
    }

    private boolean process(CalibrationRecipe recipe, MachineStatAccumulator stats) {
        if (level == null
                || !recipe.matches(new CalibrationRecipeInput(inputStack(), patternStack(), catalystStack(), stabilizerStack()), level)) {
            return false;
        }

        int operations = Math.max(1, Math.min(chassis().lanes(), maximumOperations(recipe)));
        ItemStack baseResult = createOutput(recipe, stats, operations);
        ItemStack result = ProcessingChance.applySuperOutput(level, stats, recipe, baseResult, baseResult.copyWithCount(1));
        if (!canMergeOutput(result)) {
            result = baseResult;
            if (!canMergeOutput(result)) {
                return false;
            }
        }

        for (int index = 0; index < operations; index++) {
            consumeInput();
            consumeCatalyst(stats);
            recipe.stabilizer().ifPresent(ignored -> stabilizerStack().shrink(1));
        }
        mergeOutput(result);
        for (int index = 0; index < operations; index++) {
            grantRecipeXp(recipe);
        }
        resetCycle();
        setChanged();
        return true;
    }

    private void grantRecipeXp(CalibrationRecipe recipe) {
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
        return MachineMasteryFamily.RESONANCE_CALIBRATOR;
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

    private ItemStack createOutput(CalibrationRecipe recipe, MachineStatAccumulator stats, int operations) {
        CalibrationRecipeResult recipeResult = recipe.result();
        CalibrationValueRange stabilityRange = currentStabilityRange(recipe, stats);
        int stability = stabilityRange.roll(level.random);
        int rp = currentRefinementPotentialRange(recipe, stats).roll(level.random);
        ItemStack result = recipeResult.stackWithState(recipe.family(), stability, rp);
        result.setCount(Math.min(result.getMaxStackSize(), result.getCount() * operations));
        return result;
    }

    private int maximumOperations(CalibrationRecipe recipe) {
        int operations = inputStack().getCount();
        operations = Math.min(operations, catalystStack().getCount());
        if (recipe.stabilizer().isPresent()) {
            operations = Math.min(operations, stabilizerStack().getCount());
        }
        return Math.max(1, operations);
    }

    private void consumeInput() {
        ItemStack input = inputStack();
        ItemStack remainder = input.getCraftingRemainingItem();
        input.shrink(1);
        if (!remainder.isEmpty()) {
            if (input.isEmpty()) {
                processInventory.setStackInSlot(SLOT_INPUT, remainder);
            } else if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder);
            }
        }
    }

    private void consumeCatalyst(MachineStatAccumulator stats) {
        double catalystEfficiency = Math.max(1.0, stats.value(MachineStat.CATALYST_EFFICIENCY));
        double preserveChance = Math.min(0.45, (catalystEfficiency - 1.0) * 0.35);
        if (level == null || level.random.nextDouble() >= preserveChance) {
            catalystStack().shrink(1);
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

    private int processingTicks(CalibrationRecipe recipe, MachineStatAccumulator stats) {
        int baseTicks = Math.max(1, (int) Math.ceil(recipe.processingTicks() * RNGTechConfig.CALIBRATION_TIME_MULTIPLIER.get()));
        return stats.adjustedProcessingTicks(baseTicks);
    }

    private int energyCostPerTick(CalibrationRecipe recipe, MachineStatAccumulator stats) {
        int adjustedTicks = processingTicks(recipe, stats);
        int baseEnergy = Math.max(1, (int) Math.ceil(recipe.energy() * RNGTechConfig.CALIBRATION_ENERGY_MULTIPLIER.get()));
        int totalEnergy = stats.adjustedEnergyCost(baseEnergy);
        return Math.max(1, (int) Math.ceil(totalEnergy / (double) adjustedTicks));
    }

    private int energyCostPerCraft(CalibrationRecipe recipe, MachineStatAccumulator stats) {
        int baseEnergy = Math.max(1, (int) Math.ceil(recipe.energy() * RNGTechConfig.CALIBRATION_ENERGY_MULTIPLIER.get()));
        return stats.adjustedEnergyCost(baseEnergy);
    }

    private int currentProcessingTicks(CalibrationRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null ? 0 : processingTicks(recipe, stats);
    }

    private CalibrationValueRange currentStabilityRange(CalibrationRecipe recipe, MachineStatAccumulator stats) {
        if (recipe == null) {
            return new CalibrationValueRange(0, 0);
        }
        CalibrationValueRange base = recipe.result().stability();
        double quality = Math.max(0.1, stats.value(MachineStat.CALIBRATION_QUALITY));
        double precision = Math.max(0.1, stats.value(MachineStat.CALIBRATION_PRECISION));
        double stability = Math.max(0.1, stats.value(MachineStat.STABILITY));
        int floorBonus = (int) Math.round((quality - 1.0) * 18.0 + (stability - 1.0) * 12.0);
        int ceilingBonus = (int) Math.round((quality - 1.0) * 10.0);
        int spread = Math.max(0, base.max() - base.min());
        int precisionReduction = Math.max(0, (int) Math.round((precision - 1.0) * spread * 0.35));
        int min = base.min() + floorBonus + RNGTechConfig.CALIBRATION_STABILITY_BONUS.get();
        int max = base.max() + ceilingBonus - precisionReduction + RNGTechConfig.CALIBRATION_STABILITY_BONUS.get();
        if (max < min) {
            max = min;
        }
        return new CalibrationValueRange(min, max).clamped(0, 100);
    }

    private CalibrationValueRange currentRefinementPotentialRange(CalibrationRecipe recipe, MachineStatAccumulator stats) {
        if (recipe == null) {
            return new CalibrationValueRange(0, 0);
        }
        int bonus = (int) Math.floor(stats.value(MachineStat.REFINEMENT_POTENTIAL_BONUS))
                + RNGTechConfig.CALIBRATION_REFINEMENT_POTENTIAL_BONUS.get();
        CalibrationValueRange base = recipe.result().refinementPotential();
        return new CalibrationValueRange(base.min() + bonus, base.max() + bonus).clamped(0, 64);
    }

    private int statusCode(CalibrationRecipe recipe, MachineStatAccumulator stats) {
        if (!hasResonanceCoil()) {
            return STATUS_MISSING_RESONANCE_COIL;
        }
        if (!hasControlBoard()) {
            return STATUS_MISSING_CONTROL_BOARD;
        }
        if (inputStack().isEmpty()) {
            return STATUS_NO_INPUT;
        }
        if (patternStack().isEmpty()) {
            return STATUS_MISSING_PATTERN;
        }
        if (catalystStack().isEmpty()) {
            return STATUS_MISSING_CATALYST;
        }
        if (recipe == null) {
            return STATUS_INVALID_RECIPE;
        }
        if (!hasRequiredRecipeStage(recipe)) {
            return STATUS_INSUFFICIENT_STAGE;
        }
        if (!canMergeOutput(recipe.outputStack())) {
            return STATUS_OUTPUT_FULL;
        }
        int energyCost = energyCostPerTick(recipe, stats);
        return consumeWorkingEnergy(energyCost, true) < energyCost ? STATUS_NO_POWER : STATUS_READY;
    }

    private boolean hasRequiredRecipeStage(CalibrationRecipe recipe) {
        return recipe.minimumStage() <= chassis().stage() && coilStage() >= recipe.minimumStage();
    }

    private int coilStage() {
        ItemStack stack = resonanceCoilStack();
        return stack.getItem() instanceof CalibrationGearItem gear ? gear.stage() : 0;
    }

    private void startCycleIfNeeded() {
        if (progress != 0) {
            return;
        }
        activeInput = singleCopy(inputStack());
        activePattern = singleCopy(patternStack());
        activeCatalyst = singleCopy(catalystStack());
        activeStabilizer = singleCopy(stabilizerStack());
    }

    private boolean activeCycleMatches() {
        if (progress == 0) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(activeInput, inputStack())
                && ItemStack.isSameItemSameComponents(activePattern, patternStack())
                && ItemStack.isSameItemSameComponents(activeCatalyst, catalystStack())
                && ItemStack.isSameItemSameComponents(activeStabilizer, stabilizerStack());
    }

    private void resetCycleIfActive() {
        if (progress != 0 || !activeInput.isEmpty() || !activePattern.isEmpty() || !activeCatalyst.isEmpty() || !activeStabilizer.isEmpty()) {
            resetCycle();
            resetBulkSpeed();
            setChanged();
        }
    }

    private void resetCycle() {
        progress = 0;
        activeInput = ItemStack.EMPTY;
        activePattern = ItemStack.EMPTY;
        activeCatalyst = ItemStack.EMPTY;
        activeStabilizer = ItemStack.EMPTY;
    }

    private void resetBulkSpeed() {
        if (bulkSpeed.reset()) {
            setChanged();
        }
    }

    private void applyGearStats(MachineStatAccumulator stats, ItemStack stack) {
        if (stack.getItem() instanceof MachinePartItem part) {
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

    private boolean hasRequiredGear() {
        return hasResonanceCoil() && hasControlBoard();
    }

    private boolean hasResonanceCoil() {
        return isResonanceCoil(resonanceCoilStack());
    }

    private boolean hasControlBoard() {
        return isControlBoard(controlBoardStack());
    }

    private boolean hasBatteryCell() {
        return isBatteryCell(batteryCellStack());
    }

    private ItemStack inputStack() {
        return processInventory.getStackInSlot(SLOT_INPUT);
    }

    private ItemStack patternStack() {
        return gearInventory.getStackInSlot(activePatternSlot());
    }

    private ItemStack catalystStack() {
        return processInventory.getStackInSlot(SLOT_CATALYST);
    }

    private ItemStack stabilizerStack() {
        return processInventory.getStackInSlot(SLOT_STABILIZER);
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private ItemStack resonanceCoilStack() {
        return gearInventory.getStackInSlot(SLOT_RESONANCE_COIL);
    }

    private ItemStack controlBoardStack() {
        return gearInventory.getStackInSlot(SLOT_CONTROL_BOARD);
    }

    private ItemStack stabilizerMatrixStack() {
        return gearInventory.getStackInSlot(SLOT_STABILIZER_MATRIX);
    }

    private IEnergyStorage batteryCellEnergyStorage() {
        ItemStack cell = batteryCellStack();
        return cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
    }

    private int activePatternSlot() {
        return SLOT_PATTERN_STORAGE + selectedPattern;
    }

    private static int clampPatternIndex(int patternIndex) {
        return Math.max(0, Math.min(PATTERN_SLOT_COUNT - 1, patternIndex));
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

    private final class AutomationInputHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return AUTOMATION_INPUT_SLOTS.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(mappedSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return processInventory.insertItem(mappedSlot(slot), stack, simulate);
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
            return processInventory.isItemValid(mappedSlot(slot), stack);
        }

        private int mappedSlot(int slot) {
            if (slot < 0 || slot >= AUTOMATION_INPUT_SLOTS.length) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0," + AUTOMATION_INPUT_SLOTS.length + ")");
            }
            return AUTOMATION_INPUT_SLOTS[slot];
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

    private final class CalibratorEnergyStorage implements IEnergyStorage {
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
