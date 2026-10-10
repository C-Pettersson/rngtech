package com.rngtech.content.blockentity;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.CrusherBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.CrushHeadItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.menu.CrusherMenu;
import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.content.recipe.CrusherRecipe;
import com.rngtech.content.recipe.ProcessingEnergyScaling;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.BatchProcessing;
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
import com.rngtech.rpg.OutputAmountTracker;
import com.rngtech.rpg.progression.AscendancyFormulas;
import com.rngtech.rpg.progression.CrusherPassiveTree;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveStatType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public class CrusherBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider, MachineMasteryHost {
    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_CRUSH_HEAD = 1;
    public static final int STATUS_NO_INPUT = 2;
    public static final int STATUS_INVALID_RECIPE = 3;
    public static final int STATUS_BLOCKED_LEVEL = 4;
    public static final int STATUS_OUTPUT_FULL = 5;
    public static final int STATUS_NO_POWER = 6;
    public static final int STATUS_INSUFFICIENT_INPUT = 7;
    public static final int STATUS_JAMMED = 8;
    public static final int STATUS_UNDER_LEVEL_PENALTY = 9;

    public static final int SLOT_INPUT_A = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_CRUSH_HEAD = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_COUNT = 4;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_INPUT_SLOTS = 4;
    private static final int DATA_OUTPUT_AMOUNT = 5;
    private static final int DATA_PROCESSING_SPEED = 6;
    private static final int DATA_PROCESSING_LEVEL = 7;
    private static final int DATA_ENERGY_USAGE = 8;
    private static final int DATA_ENERGY_CAPACITY_STAT = 9;
    private static final int DATA_ENERGY_TRANSFER = 10;
    private static final int DATA_REFINEMENT_POTENTIAL = 11;
    private static final int DATA_OUTPUT_BONUS_PROGRESS = 12;
    private static final int DATA_OUTPUT_BONUS_INCREMENT = 13;
    private static final int DATA_ENERGY_PER_TICK = 14;
    private static final int DATA_ENERGY_PER_CRAFT = 15;
    private static final int DATA_ACTIVE_JOBS = 16;
    private static final int DATA_BATCH_SIZE = 17;
    private static final int DATA_STATUS = 18;
    private static final int DATA_OUTPUT_GUARD_GRACE = 19;
    private static final int DATA_NO_BATTERY_OUTPUT_RETENTION = 20;
    private static final int DATA_HIGH_HARDNESS_ENERGY_MITIGATION = 21;
    private static final int DATA_CRUSHER_INPUT_FILTER = 22;
    private static final int DATA_CRUSHER_SALVAGE_CHANCE = 23;
    private static final int DATA_JAM_TICKS = 24;
    private static final int DATA_REQUIRED_PROCESSING_LEVEL = 25;
    private static final int DATA_HARDNESS_DEFICIT = 26;
    private static final int DATA_JAM_CHANCE = 27;
    private static final int DATA_UNDER_LEVEL_PENALTY_MULTIPLIER = 28;
    private static final int DATA_MACHINE_PROGRESSION_START = 29;
    private static final int DATA_BATTERY_SLOT_BLOCKED = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int DATA_CRUSH_HEAD_MATCHING_STAGE_REQUIRED = DATA_BATTERY_SLOT_BLOCKED + 1;
    private static final int DATA_CHASSIS_STAGE = DATA_CRUSH_HEAD_MATCHING_STAGE_REQUIRED + 1;
    private static final int DATA_COUNT = DATA_CHASSIS_STAGE + 1;
    private static final int STAT_SCALE = 100;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_OUTPUT) {
                return false;
            }
            if (slot == SLOT_FUEL) {
                return isAllowedBatteryCell(stack);
            }
            if (slot == SLOT_CRUSH_HEAD) {
                return isAllowedCrushHead(stack);
            }
            return slot == SLOT_INPUT_A;
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
            if (slot == SLOT_INPUT_A) {
                outputAmountTracker.updateInput(getStackInSlot(slot));
            } else if (slot == SLOT_FUEL) {
                clampInternalEnergy();
            }
            setChanged();
        }
    };

    private final IEnergyStorage energy = new CrusherEnergyStorage();
    private final IItemHandler filteredInputHandler = new CrusherInputItemHandler();

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_MACHINE_PROGRESSION_START
                    && index < DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT) {
                return MasteryMenuSupport.get(CrusherBlockEntity.this, index - DATA_MACHINE_PROGRESSION_START, CrusherBlockEntity.this::effectiveStats);
            }
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> currentProcessingTicks(stats);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_INPUT_SLOTS -> getEffectiveInputSlots();
                case DATA_OUTPUT_AMOUNT -> scaledStat(stats, MachineStat.OUTPUT_AMOUNT);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_PROCESSING_LEVEL -> scaledStat(stats, MachineStat.PROCESSING_LEVEL);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_OUTPUT_BONUS_PROGRESS -> currentOutputBonusProgress(stats);
                case DATA_OUTPUT_BONUS_INCREMENT -> currentBonusOutputIncrement(stats);
                case DATA_ENERGY_PER_TICK -> currentEnergyCostPerTick(stats);
                case DATA_ENERGY_PER_CRAFT -> currentEnergyCostPerCraft(stats);
                case DATA_ACTIVE_JOBS -> currentActiveJobs(stats);
                case DATA_BATCH_SIZE -> batchSize(stats);
                case DATA_STATUS -> statusCode(stats);
                case DATA_OUTPUT_GUARD_GRACE -> scaledStat(stats, MachineStat.OUTPUT_GUARD_GRACE);
                case DATA_NO_BATTERY_OUTPUT_RETENTION -> scaledStat(stats, MachineStat.NO_BATTERY_OUTPUT_RETENTION);
                case DATA_HIGH_HARDNESS_ENERGY_MITIGATION -> scaledStat(stats, MachineStat.HIGH_HARDNESS_ENERGY_MITIGATION);
                case DATA_CRUSHER_INPUT_FILTER -> scaledStat(stats, MachineStat.CRUSHER_INPUT_FILTER);
                case DATA_CRUSHER_SALVAGE_CHANCE -> scaledStat(stats, MachineStat.CRUSHER_SALVAGE_CHANCE);
                case DATA_JAM_TICKS -> jawJamTicks;
                case DATA_REQUIRED_PROCESSING_LEVEL -> currentRequiredProcessingLevel(stats);
                case DATA_HARDNESS_DEFICIT -> currentHardnessDeficit(stats);
                case DATA_JAM_CHANCE -> currentJamChancePerThousand(stats);
                case DATA_UNDER_LEVEL_PENALTY_MULTIPLIER -> scaledUnderLevelPenaltyMultiplier(stats);
                case DATA_BATTERY_SLOT_BLOCKED -> batteryCellSlotBlocked() ? 1 : 0;
                case DATA_CRUSH_HEAD_MATCHING_STAGE_REQUIRED -> matchingStageCrushHeadRequired() ? 1 : 0;
                case DATA_CHASSIS_STAGE -> chassisMaterial().stage();
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

    private final OutputAmountTracker outputAmountTracker = new OutputAmountTracker();
    private final BulkSpeedState bulkSpeed = new BulkSpeedState();
    private int progress;
    private int batchJobs;
    private int internalEnergy;
    private int outputBlockedTicks;
    private int jawJamTicks;
    private long energyTelemetryTick = Long.MIN_VALUE;
    private int energyInputThisTick;
    private int lastEnergyInput;

    public CrusherBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CRUSHER.get(), pos, blockState, MachineType.CRUSHER, SLOT_INPUT_A, SLOT_FUEL, SLOT_OUTPUT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrusherBlockEntity crusher) {
        crusher.beginEnergyTelemetryTick();
        if (crusher.jawJamTicks > 0) {
            crusher.jawJamTicks--;
            BaseMachineBlock.setActive(level, pos, state, false);
            crusher.setChanged();
            return;
        }

        MachineStatAccumulator stats = crusher.effectiveStats();
        crusher.outputAmountTracker.setMemory(stats.intValue(MachineStat.BANK_MEMORY));
        crusher.outputAmountTracker.updateInput(crusher.inventory.getStackInSlot(SLOT_INPUT_A));
        CrusherRecipe recipe = crusher.findNextRecipe(stats);
        if (recipe == null) {
            if (crusher.preserveOutputBlockedProgress(stats)) {
                BaseMachineBlock.setActive(level, pos, state, false);
                crusher.setChanged();
                return;
            }
            crusher.resetProgress();
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }
        if (!recipe.allowsBonusOutput()) {
            crusher.outputAmountTracker.reset();
        }

        crusher.outputBlockedTicks = 0;

        int jobs = crusher.prepareBatchJobs(recipe, stats);
        if (jobs <= 0) {
            crusher.resetProgress();
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        int energyCost = crusher.energyCostForProgress(recipe, stats, crusher.progress, jobs);
        if (crusher.progress == 0 && crusher.rollUnderLevelJam(level, recipe, stats)) {
            BaseMachineBlock.setActive(level, pos, state, false);
            crusher.setChanged();
            return;
        }

        if (crusher.progress == 0 && !underLevel(recipe, stats) && ProcessingChance.rollInstant(level, stats)) {
            int fullEnergyCost = crusher.energyCostPerBatch(recipe, stats, jobs);
            if (crusher.consumeWorkingEnergy(fullEnergyCost, true) >= fullEnergyCost) {
                BaseMachineBlock.setActive(level, pos, state, true);
                crusher.consumeWorkingEnergy(fullEnergyCost, false);
                int completed = crusher.process(recipe, stats, level, jobs);
                crusher.grantRecipeXp(recipe, completed);
                crusher.bulkSpeed.recordProcesses(crusher.activeTraits(), completed);
                crusher.progress = 0;
                crusher.batchJobs = 0;
                crusher.setChanged();
                return;
            }
        }
        if (crusher.consumeWorkingEnergy(energyCost, true) < energyCost) {
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        BaseMachineBlock.setActive(level, pos, state, true);
        crusher.consumeWorkingEnergy(energyCost, false);
        crusher.progress++;

        if (crusher.progress >= adjustedProcessingTicks(recipe, stats, jobs)) {
            int completed = crusher.process(recipe, stats, level, jobs);
            crusher.grantRecipeXp(recipe, completed);
            crusher.bulkSpeed.recordProcesses(crusher.activeTraits(), completed);
            crusher.progress = 0;
            crusher.batchJobs = 0;
        }
        crusher.setChanged();
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return energy;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == Direction.UP) {
            return filteredInputHandler;
        }
        return super.getItemHandler(side);
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return inventory;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        int status = statusCode(stats);
        CrusherRecipe recipe = level == null ? null : CrusherRecipes.find(level, inventory.getStackInSlot(SLOT_INPUT_A)).orElse(null);
        int requiredLevel = recipe == null ? MachineInfoSnapshot.UNSET : recipe.requiredProcessingLevel();
        int energyDemand = isRunnableStatus(status) || status == STATUS_NO_POWER ? currentEnergyCostPerTick(stats) : 0;
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("crusher")
                .stage(chassisMaterial().stage())
                .state(machineInfoState(status), crusherBlockedReason(status))
                .progress(progress, currentProcessingTicks(stats))
                .energy(energyStored(), energyCapacity(), energyDemand > 0 ? -energyDemand : 0)
                .energyTelemetry(
                        lastEnergyInput(),
                        0,
                        connector.transferRate(),
                        connectorInputBottleneck(connector, energyDemand)
                )
                .processingLevel(stats.intValue(MachineStat.PROCESSING_LEVEL), requiredLevel)
                .gear(crusherGearSummary(status))
                .output(crusherOutputSummary(status))
                .refinement(machineTraits())
                .build();
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(
                machineTraits(),
                Component.translatable("container.rngtech." + chassisMaterial().blockId())
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrusherMenu(containerId, playerInventory, this, menuData);
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
        dropRefinementInventory(level);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Energy", internalEnergyStored());
        tag.putInt("Progress", progress);
        tag.putInt("BatchJobs", batchJobs);
        tag.putInt("OutputBlockedTicks", outputBlockedTicks);
        tag.putInt("JawJamTicks", jawJamTicks);
        bulkSpeed.save(tag);
        outputAmountTracker.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        clampInternalEnergy();
        progress = tag.getInt("Progress");
        batchJobs = Math.max(0, tag.getInt("BatchJobs"));
        outputBlockedTicks = Math.max(0, tag.getInt("OutputBlockedTicks"));
        jawJamTicks = Math.max(0, tag.getInt("JawJamTicks"));
        bulkSpeed.load(tag);
        outputAmountTracker.load(tag, registries);
    }

    private CrusherRecipe findNextRecipe(MachineStatAccumulator stats) {
        return findNextRecipe(stats, true);
    }

    private CrusherRecipe findNextRecipe(MachineStatAccumulator stats, boolean requireOutputSpace) {
        if (level == null || !hasCrushHead()) {
            return null;
        }
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT_A);
        CrusherRecipe recipe = CrusherRecipes.find(level, input).orElse(null);
        if (recipe != null && MegaPassiveTree.acceptsHardness(machineProgression(), recipe.requiredProcessingLevel()) && (!requireOutputSpace || canAcceptOutput(recipe, stats))) {
            return recipe;
        }
        return null;
    }

    private int process(CrusherRecipe recipe, MachineStatAccumulator stats, Level level, int jobs) {
        int completed = 0;
        ItemStack baseOutput = recipe.outputStack(recipe.baseOutputCount());
        for (int job = 0; job < jobs; job++) {
            ItemStack input = inventory.getStackInSlot(SLOT_INPUT_A);
            if (!recipe.hasRequiredInput(input)) {
                return completed;
            }

            double outputAmount = outputAmountFor(recipe, stats);
            ItemStack result = recipe.outputStack(outputAmountTracker.nextOutputCount(
                    recipe.baseOutputCount(),
                    outputAmount
            ));
            if (!canMergeOutput(result)) {
                return completed;
            }

            OutputAmountTracker.Payout payout = outputAmountTracker.consume(recipe.baseOutputCount(), outputAmount);
            result = recipe.outputStack(payout.total());
            if (allowsOutputBonusEffects(recipe, stats)) {
                result = withSuperOutput(level, stats, recipe, result, baseOutput, payout);
            }
            result = withSalvage(level, stats, recipe, result, baseOutput);
            input.shrink(recipe.inputCount());
            mergeOutput(result);
            completed++;
        }
        setChanged();
        return completed;
    }

    /** Super Output, guaranteed on each Super Output Cadence cycle, plus a Compound Yield roll to copy what the bank paid. */
    private ItemStack withSuperOutput(
            Level level,
            MachineStatAccumulator stats,
            CrusherRecipe recipe,
            ItemStack result,
            ItemStack baseOutput,
            OutputAmountTracker.Payout payout
    ) {
        boolean guaranteed = outputAmountTracker.countCadence(stats.intValue(MachineStat.SUPER_OUTPUT_CADENCE));
        if (guaranteed || ProcessingChance.rollSuperOutput(level, stats, recipe, baseOutput)) {
            result = mergeableOr(result, ProcessingChance.grow(result, baseOutput, baseOutput.getCount()));
        }
        if (payout.banked() > 0 && hasMasteryBehavior("COMPOUND_YIELD") && ProcessingChance.rollSuperOutput(level, stats, recipe, baseOutput)) {
            result = mergeableOr(result, ProcessingChance.grow(result, baseOutput, payout.banked()));
        }
        return result;
    }

    /**
     * Salvage adds one base output. Under level it needs Rubble Reclaimer and rolls at half chance, and Tailings Recovery
     * banks salvage that does not fit the output.
     */
    private ItemStack withSalvage(Level level, MachineStatAccumulator stats, CrusherRecipe recipe, ItemStack result, ItemStack baseOutput) {
        boolean under = underLevel(recipe, stats);
        if (under && !hasMasteryBehavior("RUBBLE_RECLAIMER")
                || !ProcessingChance.rollCrusherSalvage(level, stats, recipe, baseOutput, under ? 0.5D : 1.0D)) {
            return result;
        }
        ItemStack salvaged = ProcessingChance.grow(result, baseOutput, 1);
        if (salvaged != result && canMergeOutput(salvaged)) {
            return salvaged;
        }
        if (hasMasteryBehavior("TAILINGS_RECOVERY")) {
            outputAmountTracker.bank(1.0D);
        }
        return result;
    }

    private ItemStack mergeableOr(ItemStack current, ItemStack candidate) {
        return canMergeOutput(candidate) ? candidate : current;
    }

    private void grantRecipeXp(CrusherRecipe recipe, int completedJobs) {
        if (completedJobs <= 0 || recipe.machineXp() <= 0) {
            return;
        }
        MachineProgressionState progression = machineProgression();
        int xpQuarters = recipeXpQuarters(recipe, progression);
        if (xpQuarters <= 0) {
            return;
        }
        grantMasteryXp(MachineProgressionState.workXp((long) recipe.machineXp() * completedJobs, recipe.machineXpBand()), xpQuarters);
    }

    private static int recipeXpQuarters(CrusherRecipe recipe, MachineProgressionState progression) {
        int recipeBand = Math.max(1, recipe.machineXpBand());
        int levelDelta = progression.level() - recipeBand;
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

    public boolean canInstallCrushHead(ItemStack stack) {
        return isAllowedCrushHead(stack);
    }

    public boolean canInstallBatteryCell(ItemStack stack) {
        return isAllowedBatteryCell(stack);
    }

    @Override
    public boolean mutesMachineSound() {
        return CrusherPassiveTree.mutesMachineSound(machineProgression());
    }

    private boolean canAcceptOutput(CrusherRecipe recipe, MachineStatAccumulator stats) {
        return availableJobs(recipe, stats) > 0;
    }

    private int prepareBatchJobs(CrusherRecipe recipe, MachineStatAccumulator stats) {
        if (progress <= 0) {
            batchJobs = availableJobs(recipe, stats);
            return batchJobs;
        }
        if (batchJobs <= 0) {
            batchJobs = availableJobs(recipe, stats);
        }
        return canProcessBatch(recipe, stats, batchJobs) ? batchJobs : 0;
    }

    private int currentActiveJobs(MachineStatAccumulator stats) {
        if (jawJamTicks > 0) {
            return 0;
        }
        CrusherRecipe recipe = findNextRecipe(stats);
        if (recipe == null) {
            return 0;
        }
        return progress > 0 && batchJobs > 0 ? batchJobs : availableJobs(recipe, stats);
    }

    private int availableJobs(CrusherRecipe recipe, MachineStatAccumulator stats) {
        if (level == null) {
            return 0;
        }
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT_A);
        if (input.isEmpty() || !recipe.matchesInput(input) || input.getCount() < recipe.inputCount()) {
            return 0;
        }
        int maxJobs = Math.min(batchSize(stats), input.getCount() / recipe.inputCount());
        for (int jobs = maxJobs; jobs > 0; jobs--) {
            if (canAcceptBatchOutput(recipe, stats, jobs)) {
                return jobs;
            }
        }
        return 0;
    }

    private boolean canProcessBatch(CrusherRecipe recipe, MachineStatAccumulator stats, int jobs) {
        if (jobs <= 0 || jobs > batchSize(stats) || level == null) {
            return false;
        }
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT_A);
        return input.getCount() >= jobs * recipe.inputCount()
                && recipe.matchesInput(input)
                && canAcceptBatchOutput(recipe, stats, jobs);
    }

    private boolean canAcceptBatchOutput(CrusherRecipe recipe, MachineStatAccumulator stats, int jobs) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT_A);
        ItemStack result = recipe.outputStack(outputAmountTracker.nextOutputCount(
                input,
                recipe.baseOutputCount(),
                outputAmountFor(recipe, stats),
                jobs
        ));
        return canMergeOutput(result);
    }

    private boolean canAcceptCandidateOutput(CrusherRecipe recipe, MachineStatAccumulator stats, ItemStack input, int jobs) {
        ItemStack result = recipe.outputStack(outputAmountTracker.nextOutputCount(
                input,
                recipe.baseOutputCount(),
                outputAmountFor(recipe, stats),
                jobs
        ));
        return canMergeOutput(result);
    }

    private boolean canMergeOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        int slotLimit = Math.min(result.getMaxStackSize(), inventory.getSlotLimit(SLOT_OUTPUT));
        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return result.getCount() <= slotLimit;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= slotLimit;
    }

    private void mergeOutput(ItemStack result) {
        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
            return;
        }
        ItemStack merged = output.copy();
        merged.grow(result.getCount());
        inventory.setStackInSlot(SLOT_OUTPUT, merged);
    }

    private int getEffectiveInputSlots() {
        return 1;
    }

    public MachineStatAccumulator effectiveStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.crusher(chassisMaterial());
        MachineTraits activeTraits = MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock());
        stats.apply(activeTraits);

        ItemStack crushHead = crushHeadStack();
        if (isAllowedCrushHead(crushHead)) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, crushHead);
        }
        CrusherPassiveTree.applyStats(stats, machineProgression());
        if (hasMasteryBehavior("REFINERS_OATH")) {
            stats.apply(MegaPassiveTree.behaviorSource(machineProgression(), "REFINERS_OATH"), new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.OUTPUT_AMOUNT,
                    ModifierOperation.MORE,
                    AscendancyFormulas.refinersOathMultiplier(BatchProcessing.statBatchSize(stats))
            ));
        }
        if (!hasBatteryCell()) {
            stats.apply(MachineStatAccumulator.NO_BATTERY_SOURCE, new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.OUTPUT_AMOUNT,
                    ModifierOperation.LESS,
                    noBatteryOutputMultiplier(stats)
            ));
        }
        bulkSpeed.apply(stats, activeTraits);
        return stats;
    }

    private static double noBatteryOutputMultiplier(MachineStatAccumulator stats) {
        double baseMultiplier = RNGTechConfig.CRUSHER_NO_BATTERY_CELL_OUTPUT_MULTIPLIER.get();
        double retainedLoss = Math.max(0.0D, Math.min(100.0D, stats.value(MachineStat.NO_BATTERY_OUTPUT_RETENTION))) / 100.0D;
        return baseMultiplier + (1.0D - baseMultiplier) * retainedLoss;
    }

    private MachineTraits activeTraits() {
        return MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock());
    }

    private int currentProcessingTicks(MachineStatAccumulator stats) {
        CrusherRecipe recipe = findNextRecipe(stats);
        return recipe == null ? 0 : adjustedProcessingTicks(recipe, stats, Math.max(1, currentActiveJobs(stats)));
    }

    private int currentBonusOutputIncrement(MachineStatAccumulator stats) {
        CrusherRecipe recipe = findNextRecipe(stats);
        return recipe == null || bonusSuppressed(recipe, stats)
                ? 0
                : OutputAmountTracker.scaledBonusIncrement(recipe.baseOutputCount(), outputAmountFor(recipe, stats));
    }

    private int currentOutputBonusProgress(MachineStatAccumulator stats) {
        CrusherRecipe recipe = findNextRecipe(stats);
        return recipe != null && bonusSuppressed(recipe, stats) ? 0 : outputAmountTracker.scaledBonusProgress();
    }

    /** Under level the bank is paused, unless Fault Lines keeps Output Amount. */
    private boolean bonusSuppressed(CrusherRecipe recipe, MachineStatAccumulator stats) {
        return underLevel(recipe, stats) && !hasMasteryBehavior("FAULT_LINES");
    }

    private int currentEnergyCostPerTick(MachineStatAccumulator stats) {
        if (jawJamTicks > 0) {
            return 0;
        }
        CrusherRecipe recipe = findNextRecipe(stats);
        if (recipe == null) {
            return 0;
        }
        int jobs = currentActiveJobs(stats);
        return jobs <= 0 ? 0 : energyCostForProgress(recipe, stats, progress, jobs);
    }

    private int currentEnergyCostPerCraft(MachineStatAccumulator stats) {
        CrusherRecipe recipe = findNextRecipe(stats);
        if (recipe == null) {
            return 0;
        }
        return energyCostPerCraft(recipe, stats);
    }

    private int statusCode(MachineStatAccumulator stats) {
        if (level == null) {
            return STATUS_INVALID_RECIPE;
        }
        if (!hasCrushHead()) {
            return STATUS_MISSING_CRUSH_HEAD;
        }
        if (jawJamTicks > 0) {
            return STATUS_JAMMED;
        }

        ItemStack input = inventory.getStackInSlot(SLOT_INPUT_A);
        if (input.isEmpty()) {
            return STATUS_NO_INPUT;
        }

        CrusherRecipe recipe = CrusherRecipes.find(level, input).orElse(null);
        if (recipe == null) {
            return STATUS_INVALID_RECIPE;
        }
        if (!MegaPassiveTree.acceptsHardness(machineProgression(), recipe.requiredProcessingLevel())) {
            return STATUS_BLOCKED_LEVEL;
        }
        if (input.getCount() < recipe.inputCount()) {
            return STATUS_INSUFFICIENT_INPUT;
        }
        if (!canAcceptOutput(recipe, stats)) {
            return STATUS_OUTPUT_FULL;
        }
        int energyCost = currentEnergyCostPerTick(stats);
        if (energyCost > 0 && consumeWorkingEnergy(energyCost, true) < energyCost) {
            return STATUS_NO_POWER;
        }
        return hardnessDeficit(recipe, stats) > 0 ? STATUS_UNDER_LEVEL_PENALTY : STATUS_READY;
    }

    private MachineInfoSnapshot.WorkState machineInfoState(int status) {
        if (status == STATUS_JAMMED) {
            return MachineInfoSnapshot.WorkState.PAUSED;
        }
        if (isRunnableStatus(status)) {
            return getBlockState().getValue(BaseMachineBlock.ACTIVE)
                    ? MachineInfoSnapshot.WorkState.RUNNING
                    : MachineInfoSnapshot.WorkState.IDLE;
        }
        return status == STATUS_NO_INPUT ? MachineInfoSnapshot.WorkState.IDLE : MachineInfoSnapshot.WorkState.BLOCKED;
    }

    private static boolean isRunnableStatus(int status) {
        return status == STATUS_READY || status == STATUS_UNDER_LEVEL_PENALTY;
    }

    private MachineInfoSnapshot.BlockedReason crusherBlockedReason(int status) {
        return switch (status) {
            case STATUS_MISSING_CRUSH_HEAD -> MachineInfoSnapshot.BlockedReason.MISSING_CRUSH_HEAD;
            case STATUS_NO_INPUT -> MachineInfoSnapshot.BlockedReason.NO_INPUT;
            case STATUS_INVALID_RECIPE -> MachineInfoSnapshot.BlockedReason.INVALID_RECIPE;
            case STATUS_BLOCKED_LEVEL -> MachineInfoSnapshot.BlockedReason.BLOCKED_LEVEL;
            case STATUS_INSUFFICIENT_INPUT -> MachineInfoSnapshot.BlockedReason.INSUFFICIENT_INPUT;
            case STATUS_OUTPUT_FULL -> MachineInfoSnapshot.BlockedReason.OUTPUT_FULL;
            case STATUS_NO_POWER -> MachineInfoSnapshot.BlockedReason.NO_POWER;
            case STATUS_JAMMED -> MachineInfoSnapshot.BlockedReason.JAMMED;
            default -> MachineInfoSnapshot.BlockedReason.NONE;
        };
    }

    private MachineInfoSnapshot.GearSummary crusherGearSummary(int status) {
        if (status == STATUS_MISSING_CRUSH_HEAD) {
            return MachineInfoSnapshot.GearSummary.MISSING_CRUSH_HEAD;
        }
        if (!hasBatteryCell()) {
            return MachineInfoSnapshot.GearSummary.MISSING_BATTERY_CELL;
        }
        return MachineInfoSnapshot.GearSummary.BATTERY_CELL_INSTALLED;
    }

    private MachineInfoSnapshot.OutputSummary crusherOutputSummary(int status) {
        if (status == STATUS_OUTPUT_FULL) {
            return MachineInfoSnapshot.OutputSummary.OUTPUT_FULL;
        }
        if (status == STATUS_UNDER_LEVEL_PENALTY) {
            return MachineInfoSnapshot.OutputSummary.NONE;
        }
        return outputAmountTracker.scaledBonusProgress() > 0
                ? MachineInfoSnapshot.OutputSummary.BONUS_PENDING
                : MachineInfoSnapshot.OutputSummary.NONE;
    }

    private int energyCostForProgress(CrusherRecipe recipe, MachineStatAccumulator stats, int progress, int jobs) {
        int adjustedTicks = adjustedProcessingTicks(recipe, stats, jobs);
        return multiplyEnergy(
                distributedEnergyCostForProgress(energyCostPerCraft(recipe, stats), adjustedTicks, progress),
                jobs
        );
    }

    private static int adjustedProcessingTicks(CrusherRecipe recipe, MachineStatAccumulator stats, int jobs) {
        int baseTicks = BatchProcessing.batchTicks(stats.adjustedProcessingTicks(recipe.processingTicks()), stats, jobs);
        double multiplier = underLevelPenaltyMultiplier(recipe, stats);
        long adjustedTicks = (long) Math.ceil(baseTicks * multiplier);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, adjustedTicks));
    }

    private static double underLevelPenaltyMultiplier(CrusherRecipe recipe, MachineStatAccumulator stats) {
        return AscendancyFormulas.underLevelPenaltyMultiplier(
                penalizedDeficit(recipe, stats),
                RNGTechConfig.CRUSHER_UNDER_LEVEL_PENALTY_MULTIPLIER_PER_LEVEL.get(),
                stats
        );
    }

    /** Missing levels beyond Hardness Tolerance, which alone add time, FE, and jam risk. */
    private static int penalizedDeficit(CrusherRecipe recipe, MachineStatAccumulator stats) {
        return AscendancyFormulas.penalizedDeficit(hardnessDeficit(recipe, stats), stats);
    }

    private static int hardnessDeficit(CrusherRecipe recipe, MachineStatAccumulator stats) {
        return Math.max(0, recipe.requiredProcessingLevel() - stats.intValue(MachineStat.PROCESSING_LEVEL));
    }

    private static boolean underLevel(CrusherRecipe recipe, MachineStatAccumulator stats) {
        return hardnessDeficit(recipe, stats) > 0;
    }

    private static int underLevelJamChancePerThousand(CrusherRecipe recipe, MachineStatAccumulator stats) {
        return AscendancyFormulas.jamChancePerThousand(penalizedDeficit(recipe, stats), RNGTechConfig.CRUSHER_UNDER_LEVEL_JAM_CHANCE_PER_LEVEL.get(), stats);
    }

    private static int underLevelJamTicks(CrusherRecipe recipe, MachineStatAccumulator stats) {
        return AscendancyFormulas.jamTicks(penalizedDeficit(recipe, stats), RNGTechConfig.CRUSHER_UNDER_LEVEL_JAM_TICKS_PER_LEVEL.get(), stats);
    }

    /** Shatter Point halves the high-hardness FE surcharge. */
    private int energyCostPerCraft(CrusherRecipe recipe, MachineStatAccumulator stats) {
        double surchargeScale = hasMasteryBehavior("SHATTER_POINT") ? 0.5D : 1.0D;
        int baseEnergy = stats.adjustedEnergyCost(ProcessingEnergyScaling.crusherEnergy(recipe, stats, surchargeScale));
        return scaledEnergyCost(baseEnergy, underLevelPenaltyMultiplier(recipe, stats));
    }

    private int energyCostPerBatch(CrusherRecipe recipe, MachineStatAccumulator stats, int jobs) {
        return multiplyEnergy(energyCostPerCraft(recipe, stats), jobs);
    }

    /** At-Level Output adds to the increased bucket on recipes exactly at the Crush Head's hardness. */
    private double outputAmountFor(CrusherRecipe recipe, MachineStatAccumulator stats) {
        if (!recipe.allowsBonusOutput()) {
            return 1.0D;
        }
        double outputAmount = recipe.requiredProcessingLevel() == stats.intValue(MachineStat.PROCESSING_LEVEL)
                ? stats.valueWithIncreased(MachineStat.OUTPUT_AMOUNT, stats.value(MachineStat.AT_LEVEL_OUTPUT))
                : stats.value(MachineStat.OUTPUT_AMOUNT);
        return bonusSuppressed(recipe, stats) ? Math.min(1.0D, outputAmount) : outputAmount;
    }

    private static boolean allowsOutputBonusEffects(CrusherRecipe recipe, MachineStatAccumulator stats) {
        return recipe.allowsBonusOutput() && !underLevel(recipe, stats);
    }

    private static int multiplyEnergy(int energy, int jobs) {
        long total = (long) Math.max(1, energy) * Math.max(1, jobs);
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private static int scaledEnergyCost(int energy, double multiplier) {
        long total = (long) Math.ceil(Math.max(1, energy) * Math.max(0.0D, multiplier));
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, total));
    }

    private static int distributedEnergyCostForProgress(int totalCost, int adjustedTicks, int progress) {
        int safeTicks = Math.max(1, adjustedTicks);
        int currentProgress = Math.max(0, Math.min(progress, safeTicks - 1));
        return cumulativeProcessingEnergyCost(totalCost, safeTicks, currentProgress + 1)
                - cumulativeProcessingEnergyCost(totalCost, safeTicks, currentProgress);
    }

    private static int cumulativeProcessingEnergyCost(int totalCost, int adjustedTicks, int progress) {
        return (int) Math.ceil(totalCost * (double) progress / adjustedTicks);
    }

    private boolean rollUnderLevelJam(Level level, CrusherRecipe recipe, MachineStatAccumulator stats) {
        int chance = underLevelJamChancePerThousand(recipe, stats);
        int ticks = underLevelJamTicks(recipe, stats);
        if (chance <= 0 || ticks <= 0 || level.random.nextInt(1000) >= chance) {
            return false;
        }
        resetProgress();
        jawJamTicks = ticks;
        return true;
    }

    private int currentRequiredProcessingLevel(MachineStatAccumulator stats) {
        CrusherRecipe recipe = visibleRecipe(stats);
        return recipe == null ? 0 : recipe.requiredProcessingLevel();
    }

    private int currentHardnessDeficit(MachineStatAccumulator stats) {
        CrusherRecipe recipe = visibleRecipe(stats);
        return recipe == null ? 0 : hardnessDeficit(recipe, stats);
    }

    private int currentJamChancePerThousand(MachineStatAccumulator stats) {
        CrusherRecipe recipe = visibleRecipe(stats);
        return recipe == null ? 0 : underLevelJamChancePerThousand(recipe, stats);
    }

    private int scaledUnderLevelPenaltyMultiplier(MachineStatAccumulator stats) {
        CrusherRecipe recipe = visibleRecipe(stats);
        return recipe == null ? STAT_SCALE : (int) Math.round(underLevelPenaltyMultiplier(recipe, stats) * STAT_SCALE);
    }

    private CrusherRecipe visibleRecipe(MachineStatAccumulator stats) {
        if (level == null || !hasCrushHead()) {
            return null;
        }
        return CrusherRecipes.find(level, inventory.getStackInSlot(SLOT_INPUT_A)).orElse(null);
    }

    /** Refiner's Oath gives up batching for Output Amount. */
    private int batchSize(MachineStatAccumulator stats) {
        return BatchProcessing.batchSize(stats, hasMasteryBehavior("REFINERS_OATH"));
    }

    private boolean preserveOutputBlockedProgress(MachineStatAccumulator stats) {
        if (progress <= 0) {
            outputBlockedTicks = 0;
            return false;
        }
        CrusherRecipe recipe = findNextRecipe(stats, false);
        if (recipe == null || canAcceptOutput(recipe, stats)) {
            outputBlockedTicks = 0;
            return false;
        }
        int grace = outputGuardGraceTicks(stats);
        if (grace <= 0) {
            outputBlockedTicks = 0;
            return false;
        }
        outputBlockedTicks++;
        if (outputBlockedTicks <= grace) {
            return true;
        }
        outputBlockedTicks = 0;
        return false;
    }

    private int outputGuardGraceTicks(MachineStatAccumulator stats) {
        return Math.max(0, stats.intValue(MachineStat.OUTPUT_GUARD_GRACE));
    }

    private int inputFilterTier(MachineStatAccumulator stats) {
        return Math.max(0, Math.min(4, stats.intValue(MachineStat.CRUSHER_INPUT_FILTER)));
    }

    private boolean canAutomationInsertInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        MachineStatAccumulator stats = effectiveStats();
        int filterTier = inputFilterTier(stats);
        if (filterTier <= 0 || level == null) {
            return true;
        }
        CrusherRecipe recipe = CrusherRecipes.find(level, stack).orElse(null);
        if (recipe == null) {
            return false;
        }
        if (filterTier >= 2 && !canAcceptCandidateOutput(recipe, stats, stack, 1)) {
            return false;
        }
        if (filterTier >= 3 && outputAmountTracker.hasBankedProgressForDifferentInput(stack)) {
            return false;
        }
        return filterTier < 4 || canAcceptCandidateOutput(recipe, stats, stack, candidateAutomationJobs(recipe, stats, stack));
    }

    private int candidateAutomationJobs(CrusherRecipe recipe, MachineStatAccumulator stats, ItemStack stack) {
        int candidateCount = stack.getCount();
        ItemStack existingInput = inventory.getStackInSlot(SLOT_INPUT_A);
        if (!existingInput.isEmpty() && ItemStack.isSameItemSameComponents(existingInput, stack)) {
            candidateCount += existingInput.getCount();
        }
        int jobsByInput = Math.max(1, candidateCount / Math.max(1, recipe.inputCount()));
        return Math.max(1, Math.min(batchSize(stats), jobsByInput));
    }

    private int energyStored() {
        return internalEnergyStored() + BatteryCellItem.energyStored(batteryCellStack());
    }

    private int energyCapacity() {
        return internalEnergyCapacity() + BatteryCellItem.energyCapacity(batteryCellStack());
    }

    private int internalEnergyCapacity() {
        double capacity = effectiveStats().value(MachineStat.ENERGY_CAPACITY);
        return Math.max(1, (int) Math.round(capacity));
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

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
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

    private void resetProgress() {
        boolean changed = false;
        if (progress != 0) {
            progress = 0;
            changed = true;
        }
        if (batchJobs != 0) {
            batchJobs = 0;
            changed = true;
        }
        changed |= bulkSpeed.reset();
        if (changed) {
            setChanged();
        }
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
    }

    private ItemStack batteryCellStack() {
        if (batteryCellSlotBlocked()) {
            return ItemStack.EMPTY;
        }
        return inventory.getStackInSlot(SLOT_FUEL);
    }

    private ItemStack crushHeadStack() {
        return inventory.getStackInSlot(SLOT_CRUSH_HEAD);
    }

    private boolean hasBatteryCell() {
        return isBatteryCell(batteryCellStack());
    }

    private boolean hasCrushHead() {
        return isAllowedCrushHead(crushHeadStack());
    }

    private IEnergyStorage batteryCellEnergyStorage() {
        ItemStack cell = batteryCellStack();
        return cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    public static boolean isGearComponent(ItemStack stack) {
        return stack.getItem() instanceof MachinePartItem part && part.partType() == MachinePartType.CRUSH_HEAD;
    }

    private boolean isAllowedCrushHead(ItemStack stack) {
        if (!(stack.getItem() instanceof CrushHeadItem head) || head.material().stage() > supportedCrushHeadStage()) {
            return false;
        }
        return !matchingStageCrushHeadRequired() || head.material().stage() == chassisMaterial().stage();
    }

    private int supportedCrushHeadStage() {
        return Math.min(
                CrushHeadMaterial.EXOTIC.stage(),
                chassisMaterial().stage() + CrusherPassiveTree.componentStageSupport(machineProgression())
        );
    }

    private CrusherChassisMaterial chassisMaterial() {
        return getBlockState().getBlock() instanceof CrusherBlock crusher ? crusher.material() : CrusherChassisMaterial.IRON;
    }

    private boolean isAllowedBatteryCell(ItemStack stack) {
        return !batteryCellSlotBlocked() && isBatteryCell(stack);
    }

    private boolean batteryCellSlotBlocked() {
        return CrusherPassiveTree.blocksBatteryCell(machineProgression());
    }

    private boolean matchingStageCrushHeadRequired() {
        return CrusherPassiveTree.requiresMatchingCrushHeadStage(machineProgression());
    }

    private boolean gearAllowsPassive(MegaPassiveNode node) {
        if (node.blocksBatteryCell() && isBatteryCell(inventory.getStackInSlot(SLOT_FUEL))) {
            return false;
        }
        if (!node.requiresMatchingCrushHeadStage()) {
            return true;
        }
        ItemStack crushHead = crushHeadStack();
        return crushHead.isEmpty()
                || crushHead.getItem() instanceof CrushHeadItem head && head.material().stage() == chassisMaterial().stage();
    }

    private static int clampLongToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    public static boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    private final class CrusherInputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(mappedSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!canAutomationInsertInput(stack)) {
                return stack;
            }
            return inventory.insertItem(mappedSlot(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(mappedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && canAutomationInsertInput(stack);
        }

        private int mappedSlot(int slot) {
            if (slot != 0) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
            }
            return SLOT_INPUT_A;
        }
    }

    private final class CrusherEnergyStorage implements IEnergyStorage {
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
    /** Wide Ledger keeps remembered banks on the dropped or picked machine. */
    public List<OutputAmountTracker.SavedBank> persistentBanks() {
        return hasMasteryBehavior("PERSISTENT_BANKS") ? outputAmountTracker.saved() : List.of();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        List<OutputAmountTracker.SavedBank> banks = persistentBanks();
        if (!banks.isEmpty()) {
            components.set(ModDataComponents.OUTPUT_BANKS.get(), banks);
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        List<OutputAmountTracker.SavedBank> banks = componentInput.get(ModDataComponents.OUTPUT_BANKS.get());
        if (banks != null) {
            // Keep every saved bank until the first tick applies this machine's Bank Memory.
            outputAmountTracker.setMemory(banks.size());
            outputAmountTracker.restore(banks);
        }
    }

    @Override public MachineMasteryFamily masteryFamily() { return MachineMasteryFamily.CRUSHER; }
    @Override public int ascendancyEntryStage() { return chassisMaterial().stage(); }

    @Override public MachineProgressionState machineProgression() { return super.machineProgression().forFamily(masteryFamily()); }

    @Override public boolean masteryGearAllows(MachineProgressionState state) {
        if (MegaPassiveTree.has(state, "BLOCK_BATTERY") && isBatteryCell(inventory.getStackInSlot(SLOT_FUEL))) { return false; }
        ItemStack headStack = crushHeadStack();
        if (headStack.isEmpty()) { return true; }
        if (!(headStack.getItem() instanceof CrushHeadItem head)) { return false; }
        int stage = head.material().stage();
        return MegaPassiveTree.has(state, "MATCHING_HEAD") ? stage == chassisMaterial().stage()
                : stage <= chassisMaterial().stage() + MegaPassiveTree.passive(state, PassiveStatType.COMPONENT_STAGE_SUPPORT);
    }
    @Override public void masteryChanged() { progress = 0; batchJobs = 0; clampInternalEnergy(); setChanged(); }

}
