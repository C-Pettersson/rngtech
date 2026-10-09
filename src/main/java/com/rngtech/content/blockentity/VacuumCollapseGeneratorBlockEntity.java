package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.VacuumCollapseGeneratorBlock;
import com.rngtech.content.item.CollapseNozzleItem;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.VacuumCollapsePartItem;
import com.rngtech.content.menu.VacuumCollapseGeneratorMenu;
import com.rngtech.content.recipe.VacuumCollapseRecipe;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.util.TickTransferCounter;

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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class VacuumCollapseGeneratorBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_RESIDUE = 1;
    public static final int PROCESS_SLOT_COUNT = 2;
    public static final int SLOT_VOID_CHAMBER = 0;
    public static final int SLOT_COLLAPSE_NOZZLE = 1;
    public static final int SLOT_DIMENSIONAL_STABILIZER = 2;
    public static final int SLOT_ENERGY_CONNECTOR = 3;
    public static final int GEAR_SLOT_COUNT = 4;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_GEAR = 1;
    public static final int STATUS_NO_CATALYST = 2;
    public static final int STATUS_INVALID_CATALYST = 3;
    public static final int STATUS_BLOCKED_STAGE = 4;
    public static final int STATUS_LOW_STABILITY = 5;
    public static final int STATUS_OUTPUT_FULL = 6;
    public static final int STATUS_ENERGY_FULL = 7;
    public static final int STATUS_REDSTONE_DISABLED = 8;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY_LOW = 2;
    private static final int DATA_ENERGY_HIGH = 3;
    private static final int DATA_ENERGY_CAPACITY_LOW = 4;
    private static final int DATA_ENERGY_CAPACITY_HIGH = 5;
    private static final int DATA_ENERGY_PER_TICK_LOW = 6;
    private static final int DATA_ENERGY_PER_TICK_HIGH = 7;
    private static final int DATA_MAX_OUTPUT = 8;
    private static final int DATA_RECIPE_ENERGY_LOW = 9;
    private static final int DATA_RECIPE_ENERGY_HIGH = 10;
    private static final int DATA_PROCESSING_LEVEL = 11;
    private static final int DATA_STATUS = 12;
    private static final int DATA_INSTABILITY = 13;
    private static final int DATA_ENERGY_GENERATION = 14;
    private static final int DATA_ENERGY_CAPACITY_STAT = 15;
    private static final int DATA_ENERGY_TRANSFER = 16;
    private static final int DATA_EFFICIENCY = 17;
    private static final int DATA_PROCESSING_SPEED = 18;
    private static final int DATA_STABILITY = 19;
    private static final int DATA_REFINEMENT_POTENTIAL = 20;
    private static final int DATA_FLAT_ENERGY_GENERATION = 21;
    private static final int DATA_BASE_ENERGY_GENERATION = 22;
    private static final int STAT_SCALE = 100;
    private static final int BASE_TRANSFER = 8192;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT && isKnownCatalyst(stack);
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
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_VOID_CHAMBER -> isVoidChamber(stack);
                case SLOT_COLLAPSE_NOZZLE -> isCollapseNozzle(stack);
                case SLOT_DIMENSIONAL_STABILIZER -> isDimensionalStabilizer(stack);
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
            return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler inputHandler = new InputItemHandler();
    private final IItemHandler residueHandler = new ResidueItemHandler();
    private final IEnergyStorage energyStorage = new GeneratorEnergyStorage();
    private final TickTransferCounter exportBudget = new TickTransferCounter();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> currentProcessingTicks(stats);
                case DATA_ENERGY_LOW -> lowInt(energyStored);
                case DATA_ENERGY_HIGH -> highInt(energyStored);
                case DATA_ENERGY_CAPACITY_LOW -> lowInt(energyCapacity(stats));
                case DATA_ENERGY_CAPACITY_HIGH -> highInt(energyCapacity(stats));
                case DATA_ENERGY_PER_TICK_LOW -> lowInt(currentEnergyPerTick(stats));
                case DATA_ENERGY_PER_TICK_HIGH -> highInt(currentEnergyPerTick(stats));
                case DATA_MAX_OUTPUT -> effectiveOutputRate(stats);
                case DATA_RECIPE_ENERGY_LOW -> lowInt(currentRecipeEnergy(stats));
                case DATA_RECIPE_ENERGY_HIGH -> highInt(currentRecipeEnergy(stats));
                case DATA_PROCESSING_LEVEL -> stats.intValue(MachineStat.PROCESSING_LEVEL);
                case DATA_STATUS -> statusCode(stats);
                case DATA_INSTABILITY -> (int) Math.round(activeInstability * STAT_SCALE);
                case DATA_ENERGY_GENERATION -> (int) Math.round(stats.effectiveEnergyGenerationMultiplier() * STAT_SCALE);
                case DATA_FLAT_ENERGY_GENERATION -> (int) Math.round(stats.effectiveFlatEnergyGenerationBonus() * STAT_SCALE);
                case DATA_BASE_ENERGY_GENERATION -> (int) Math.round(baseEnergyPerTick() * STAT_SCALE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> effectiveOutputRate(stats);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_STABILITY -> scaledStat(stats, MachineStat.STABILITY);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_BASE_ENERGY_GENERATION + 1;
        }
    };

    private int progress;
    private int activeProcessingTicks;
    private long energyStored;
    private long activeTotalEnergy;
    private long remainingEnergy;
    private double generationCarry;
    private double activeInstability;
    private ItemStack pendingResidue = ItemStack.EMPTY;

    public VacuumCollapseGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.VACUUM_COLLAPSE_GENERATOR.get(),
                pos,
                blockState,
                MachineType.VACUUM_COLLAPSE_GENERATOR,
                SLOT_INPUT,
                SLOT_INPUT,
                SLOT_RESIDUE
        );
        if (!(blockState.getBlock() instanceof VacuumCollapseGeneratorBlock)) {
            throw new IllegalStateException("Vacuum collapse generator block entity created for non-generator block: " + blockState);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VacuumCollapseGeneratorBlockEntity generator) {
        // Export first so downstream demand creates headroom before generation is capacity-gated.
        boolean exported = generator.exportEnergy(level, pos);
        boolean generated = generator.tickGenerator();
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

    public ContainerData getMenuData() {
        return menuData;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return side == Direction.UP || side == Direction.DOWN ? null : trackedEnergyStorage;
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        VacuumCollapseRecipe recipe = nextRecipe();
        int status = statusCode(stats);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSource(level, worldPosition);
        return MachineInfoSnapshot.builder("vacuum_collapse_generator")
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY,
                                getBlockState().getValue(BaseMachineBlock.ACTIVE),
                                status == STATUS_NO_CATALYST
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .progress(progress, currentProcessingTicks(stats))
                .energy(energyStored, energyCapacity(stats), isWorking() ? currentEnergyPerTick(stats) : 0L)
                .energyTelemetry(energyFlow.lastInput(), energyFlow.lastOutput(), connector.transferRate(), MachineInfoSnapshot.EnergyBottleneck.NONE)
                .processingLevel(
                        stats.intValue(MachineStat.PROCESSING_LEVEL),
                        recipe == null ? MachineInfoSnapshot.UNSET : recipe.minimumChamberStage()
                )
                .output(outputSummary(status))
                .refinement(machineTraits())
                .build();
    }

    private static MachineInfoSnapshot.OutputSummary outputSummary(int status) {
        return switch (status) {
            case STATUS_OUTPUT_FULL -> MachineInfoSnapshot.OutputSummary.OUTPUT_FULL;
            case STATUS_ENERGY_FULL -> MachineInfoSnapshot.OutputSummary.ENERGY_FULL;
            default -> MachineInfoSnapshot.OutputSummary.NONE;
        };
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_MISSING_GEAR -> "missing_gear";
            case STATUS_NO_CATALYST -> "no_catalyst";
            case STATUS_INVALID_CATALYST -> "invalid_catalyst";
            case STATUS_BLOCKED_STAGE -> "blocked_stage";
            case STATUS_LOW_STABILITY -> "low_stability";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_ENERGY_FULL -> "energy_full";
            case STATUS_REDSTONE_DISABLED -> "redstone";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.collapse.status." + name;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == Direction.UP) {
            return inputHandler;
        }
        if (side == Direction.DOWN) {
            return residueHandler;
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
                Component.translatable("container.rngtech.vacuum_collapse_generator")
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new VacuumCollapseGeneratorMenu(containerId, playerInventory, this, menuData);
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
        if (!pendingResidue.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), pendingResidue.copy());
            pendingResidue = ItemStack.EMPTY;
        }
        dropRefinementInventory(level);
    }

    public boolean isKnownCatalyst(ItemStack stack) {
        return level != null && VacuumCollapseRecipes.find(level, stack).isPresent();
    }

    public boolean isVoidChamber(ItemStack stack) {
        return isPart(stack, MachinePartType.VOID_CHAMBER);
    }

    public boolean isCollapseNozzle(ItemStack stack) {
        return stack.getItem() instanceof CollapseNozzleItem nozzle
                && nozzle.material().vacuumCollapseCompatible();
    }

    public boolean isDimensionalStabilizer(ItemStack stack) {
        return isPart(stack, MachinePartType.DIMENSIONAL_STABILIZER);
    }

    public boolean isEnergyConnector(ItemStack stack) {
        return stack.getItem() instanceof EnergyConnectorItem;
    }

    public MachineStatAccumulator effectiveStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.vacuumCollapseGenerator();
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
        applyPartStats(stats, chamberStack());
        applyPartStats(stats, nozzleStack());
        applyPartStats(stats, stabilizerStack());
        return stats;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("ActiveProcessingTicks", activeProcessingTicks);
        tag.putLong("Energy", energyStored);
        tag.putLong("ActiveTotalEnergy", activeTotalEnergy);
        tag.putLong("RemainingEnergy", remainingEnergy);
        tag.putDouble("GenerationCarry", generationCarry);
        tag.putDouble("ActiveInstability", activeInstability);
        tag.put("PendingResidue", pendingResidue.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        loadGearInventory(tag.getCompound("GearInventory"), registries);
        progress = tag.getInt("Progress");
        activeProcessingTicks = tag.getInt("ActiveProcessingTicks");
        energyStored = Math.max(0L, tag.getLong("Energy"));
        activeTotalEnergy = Math.max(0L, tag.getLong("ActiveTotalEnergy"));
        remainingEnergy = Math.max(0L, tag.getLong("RemainingEnergy"));
        generationCarry = tag.getDouble("GenerationCarry");
        activeInstability = tag.getDouble("ActiveInstability");
        pendingResidue = ItemStack.parseOptional(registries, tag.getCompound("PendingResidue"));
        clampInternalEnergy();
    }

    private boolean tickGenerator() {
        if (level == null || redstoneDisabled() || !hasRequiredComponents()) {
            return false;
        }

        MachineStatAccumulator stats = effectiveStats();
        if (!hasActiveRecipe() && !tryStartRecipe(stats)) {
            return false;
        }
        return generateActiveRecipe(stats);
    }

    private boolean tryStartRecipe(MachineStatAccumulator stats) {
        VacuumCollapseRecipe recipe = nextRecipe();
        if (recipe == null) {
            resetInactiveProgress();
            return false;
        }
        if (!canProcessRecipe(recipe, stats) || !hasMinimumStability(recipe, stats)) {
            resetInactiveProgress();
            return false;
        }
        ItemStack residue = residueFor(recipe);
        if (!canMergeResidue(residue)) {
            return false;
        }

        consumeInput();
        pendingResidue = residue;
        activeProcessingTicks = adjustedProcessingTicks(recipe, stats);
        activeTotalEnergy = effectiveRecipeEnergy(recipe, stats);
        remainingEnergy = activeTotalEnergy;
        generationCarry = 0.0;
        activeInstability = instabilityPressure(recipe, stats);
        progress = 0;
        setChanged();
        return remainingEnergy > 0L;
    }

    private boolean generateActiveRecipe(MachineStatAccumulator stats) {
        if (!hasActiveRecipe() || !canMergeResidue(pendingResidue)) {
            return false;
        }

        double perTick = activeTotalEnergy / (double) Math.max(1, activeProcessingTicks);
        generationCarry += Math.min(perTick, remainingEnergy);
        long wholeEnergy = (long) Math.floor(generationCarry);
        if (wholeEnergy > 0L) {
            // A collapse cannot pause: FE the buffer cannot hold is vented.
            long freeSpace = Math.max(0L, energyCapacity(stats) - energyStored);
            if (freeSpace > 0L) {
                receiveInternalEnergy(Math.min(wholeEnergy, freeSpace), stats, false);
            }
            generationCarry -= wholeEnergy;
            remainingEnergy = Math.max(0L, remainingEnergy - wholeEnergy);
        }
        progress++;
        finishIfComplete();
        return true;
    }

    private void finishIfComplete() {
        if (progress < activeProcessingTicks && remainingEnergy > 0L) {
            return;
        }
        if (!canMergeResidue(pendingResidue)) {
            return;
        }
        mergeResidue(pendingResidue);
        clearActiveRecipe();
        setChanged();
    }

    private void consumeInput() {
        ItemStack input = processInventory.getStackInSlot(SLOT_INPUT);
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

    private VacuumCollapseRecipe nextRecipe() {
        return level == null ? null : VacuumCollapseRecipes.find(level, processInventory.getStackInSlot(SLOT_INPUT)).orElse(null);
    }

    private boolean canProcessRecipe(VacuumCollapseRecipe recipe, MachineStatAccumulator stats) {
        return stats.intValue(MachineStat.PROCESSING_LEVEL) >= recipe.minimumChamberStage();
    }

    private boolean hasMinimumStability(VacuumCollapseRecipe recipe, MachineStatAccumulator stats) {
        return stats.value(MachineStat.STABILITY) >= recipe.minimumStability();
    }

    private ItemStack residueFor(VacuumCollapseRecipe recipe) {
        if (recipe.residue().isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (recipe.residueRequiresStabilizer() && !isDimensionalStabilizer(stabilizerStack())) {
            return ItemStack.EMPTY;
        }
        ItemStack residue = recipe.residue().copy();
        double instability = instabilityPressure(recipe, effectiveStats());
        int extra = (int) Math.floor(instability);
        if (extra > 0) {
            residue.grow(Math.min(extra, residue.getMaxStackSize() - residue.getCount()));
        }
        return residue;
    }

    private long effectiveRecipeEnergy(VacuumCollapseRecipe recipe, MachineStatAccumulator stats) {
        double efficiency = Math.max(0.1, stats.value(MachineStat.EFFICIENCY));
        double instabilityPenalty = 1.0 - Math.min(0.35, instabilityPressure(recipe, stats) * 0.08);
        int ticks = adjustedProcessingTicks(recipe, stats);
        return Math.max(1L, Math.round(stats.generatedEnergyTotal(recipe.energy(), ticks) * efficiency * instabilityPenalty));
    }

    private int adjustedProcessingTicks(VacuumCollapseRecipe recipe, MachineStatAccumulator stats) {
        int baseTicks = stats.adjustedProcessingTicks(recipe.processingTicks());
        double instability = instabilityPressure(recipe, stats);
        return Math.max(1, (int) Math.round(baseTicks * (1.0 + Math.min(0.75, instability * 0.10))));
    }

    private double instabilityPressure(VacuumCollapseRecipe recipe, MachineStatAccumulator stats) {
        double stability = Math.max(0.1, stats.value(MachineStat.STABILITY));
        return Math.max(0.0, recipe.instability() / stability) + nozzleStagePenalty(recipe);
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        if (energyStored <= 0L) {
            return false;
        }

        MachineStatAccumulator stats = effectiveStats();
        int remainingOutput = Math.min(effectiveOutputRate(stats), clampInt(energyStored));
        boolean exported = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (remainingOutput <= 0 || energyStored <= 0L) {
                return exported;
            }

            IEnergyStorage target = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                    pos.relative(direction),
                    direction.getOpposite()
            );
            if (target == null || !target.canReceive()) {
                continue;
            }

            int offered = extractEnergyInternal(remainingOutput, stats, true);
            int received = target.receiveEnergy(offered, false);
            int delivered = extractEnergyInternal(received, stats, false);
            energyFlow.recordOutput(delivered);
            remainingOutput -= delivered;
            exported |= delivered > 0;
        }
        return exported;
    }

    private long receiveInternalEnergy(long toReceive, MachineStatAccumulator stats, boolean simulate) {
        if (toReceive <= 0L) {
            return 0L;
        }

        long received = Math.min(toReceive, energyCapacity(stats) - energyStored);
        if (!simulate && received > 0L) {
            energyStored += received;
            setChanged();
        }
        return received;
    }

    private int extractEnergyInternal(int toExtract, MachineStatAccumulator stats, boolean simulate) {
        if (toExtract <= 0) {
            return 0;
        }

        int extracted = Math.min(toExtract, Math.min(clampInt(energyStored), exportAllowance(stats)));
        if (!simulate && extracted > 0) {
            energyStored -= extracted;
            recordExport(extracted);
            setChanged();
        }
        return extracted;
    }

    private int exportAllowance(MachineStatAccumulator stats) {
        return exportBudget.remaining(level == null ? 0L : level.getGameTime(), effectiveOutputRate(stats));
    }

    private void recordExport(int amount) {
        exportBudget.add(level == null ? 0L : level.getGameTime(), amount);
    }

    private boolean canMergeResidue(ItemStack residue) {
        if (residue.isEmpty()) {
            return true;
        }
        ItemStack output = processInventory.getStackInSlot(SLOT_RESIDUE);
        int slotLimit = Math.min(residue.getMaxStackSize(), processInventory.getSlotLimit(SLOT_RESIDUE));
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
        ItemStack output = processInventory.getStackInSlot(SLOT_RESIDUE);
        if (output.isEmpty()) {
            processInventory.setStackInSlot(SLOT_RESIDUE, residue.copy());
            return;
        }
        ItemStack merged = output.copy();
        merged.grow(residue.getCount());
        processInventory.setStackInSlot(SLOT_RESIDUE, merged);
    }

    private int currentProcessingTicks(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return activeProcessingTicks;
        }
        VacuumCollapseRecipe recipe = nextRecipe();
        return recipe == null || !canProcessRecipe(recipe, stats) ? 0 : adjustedProcessingTicks(recipe, stats);
    }

    private long currentEnergyPerTick(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return Math.max(0L, Math.round(activeTotalEnergy / (double) Math.max(1, activeProcessingTicks)));
        }
        VacuumCollapseRecipe recipe = nextRecipe();
        if (recipe == null || !canProcessRecipe(recipe, stats)) {
            return 0L;
        }
        return Math.max(1L, Math.round(effectiveRecipeEnergy(recipe, stats) / (double) Math.max(1, adjustedProcessingTicks(recipe, stats))));
    }

    private long currentRecipeEnergy(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return activeTotalEnergy;
        }
        VacuumCollapseRecipe recipe = nextRecipe();
        return recipe == null || !canProcessRecipe(recipe, stats) ? 0L : effectiveRecipeEnergy(recipe, stats);
    }

    private int statusCode(MachineStatAccumulator stats) {
        if (redstoneDisabled()) {
            return STATUS_REDSTONE_DISABLED;
        }
        if (!hasRequiredComponents()) {
            return STATUS_MISSING_GEAR;
        }
        if (hasActiveRecipe()) {
            if (!canMergeResidue(pendingResidue)) {
                return STATUS_OUTPUT_FULL;
            }
            return energyStored >= energyCapacity(stats) ? STATUS_ENERGY_FULL : STATUS_READY;
        }

        ItemStack input = processInventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) {
            return STATUS_NO_CATALYST;
        }

        VacuumCollapseRecipe recipe = nextRecipe();
        if (recipe == null) {
            return STATUS_INVALID_CATALYST;
        }
        if (!canProcessRecipe(recipe, stats)) {
            return STATUS_BLOCKED_STAGE;
        }
        if (!hasMinimumStability(recipe, stats)) {
            return STATUS_LOW_STABILITY;
        }
        if (!canMergeResidue(residueFor(recipe))) {
            return STATUS_OUTPUT_FULL;
        }
        return energyStored >= energyCapacity(stats) ? STATUS_ENERGY_FULL : STATUS_READY;
    }

    private boolean hasRequiredComponents() {
        return isVoidChamber(chamberStack()) && isCollapseNozzle(nozzleStack());
    }

    private boolean hasActiveRecipe() {
        return activeProcessingTicks > 0 && remainingEnergy > 0L;
    }

    private boolean isWorking() {
        return hasActiveRecipe() && !redstoneDisabled();
    }

    private boolean redstoneDisabled() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    private void resetInactiveProgress() {
        if (!hasActiveRecipe() && progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    private void clearActiveRecipe() {
        progress = 0;
        activeProcessingTicks = 0;
        activeTotalEnergy = 0L;
        remainingEnergy = 0L;
        generationCarry = 0.0;
        activeInstability = 0.0;
        pendingResidue = ItemStack.EMPTY;
    }

    private long energyCapacity(MachineStatAccumulator stats) {
        return Math.max(1L, Math.round(stats.value(MachineStat.ENERGY_CAPACITY)));
    }

    private int effectiveOutputRate(MachineStatAccumulator stats) {
        ItemStack connectorStack = energyConnectorStack();
        if (connectorStack.getItem() instanceof EnergyConnectorItem connector) {
            return connector.tier().transferRate();
        }
        return Math.max(0, (int) Math.round(stats.value(MachineStat.ENERGY_TRANSFER)));
    }

    private void clampInternalEnergy() {
        energyStored = Math.min(energyStored, energyCapacity(effectiveStats()));
    }

    private ItemStack chamberStack() {
        return gearInventory.getStackInSlot(SLOT_VOID_CHAMBER);
    }

    private ItemStack nozzleStack() {
        return gearInventory.getStackInSlot(SLOT_COLLAPSE_NOZZLE);
    }

    private ItemStack stabilizerStack() {
        return gearInventory.getStackInSlot(SLOT_DIMENSIONAL_STABILIZER);
    }

    private ItemStack energyConnectorStack() {
        return gearInventory.getStackInSlot(SLOT_ENERGY_CONNECTOR);
    }

    private boolean isPart(ItemStack stack, MachinePartType partType) {
        return stack.getItem() instanceof VacuumCollapsePartItem part
                && part.partType() == partType
                && part.machineType() == MachineType.VACUUM_COLLAPSE_GENERATOR;
    }

    private void applyPartStats(MachineStatAccumulator stats, ItemStack stack) {
        if (isCollapseNozzle(stack)) {
            ComponentBaseStatCatalog.applyVacuumCollapseNozzleContribution(stats, stack);
            return;
        }
        if (stack.getItem() instanceof MachinePartItem part && part.machineType() == MachineType.VACUUM_COLLAPSE_GENERATOR) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        }
    }

    private int nozzleStagePenalty(VacuumCollapseRecipe recipe) {
        if (!(nozzleStack().getItem() instanceof CollapseNozzleItem nozzle)) {
            return 0;
        }
        return Math.max(0, recipe.minimumChamberStage() - nozzle.stage());
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

    private static int lowInt(long value) {
        return (int) value;
    }

    private static int highInt(long value) {
        return (int) (value >>> 32);
    }

    private static int clampInt(long value) {
        return (int) Mth.clamp(value, 0L, Integer.MAX_VALUE);
    }

    private final class InputItemHandler implements IItemHandler {
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
            if (slot != 0) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
            }
            return SLOT_INPUT;
        }
    }

    private final class ResidueItemHandler implements IItemHandler {
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
            return SLOT_RESIDUE;
        }
    }

    private final class GeneratorEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return extractEnergyInternal(toExtract, effectiveStats(), simulate);
        }

        @Override
        public int getEnergyStored() {
            return clampInt(energyStored);
        }

        @Override
        public int getMaxEnergyStored() {
            return clampInt(energyCapacity(effectiveStats()));
        }

        @Override
        public boolean canExtract() {
            return energyStored > 0L && effectiveOutputRate(effectiveStats()) > 0;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

    /** Generation before machine and part stats, for the Stats tab breakdown; 0 when unknown. */
    private double baseEnergyPerTick() {
        VacuumCollapseRecipe recipe = nextRecipe();
        return recipe == null ? 0.0 : recipe.energy() / (double) Math.max(1, recipe.processingTicks());
    }
}
