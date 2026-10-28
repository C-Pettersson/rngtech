package com.rngtech.content.blockentity;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.VacuumCollapseGeneratorBlock;
import com.rngtech.content.item.CollapseNozzleItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.VacuumCollapsePartItem;
import com.rngtech.content.menu.VacuumCollapseGeneratorMenu;
import com.rngtech.content.recipe.VacuumCollapseRecipe;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class VacuumCollapseGeneratorBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider {
    private final EnergyExportTargets exportTargets = new EnergyExportTargets(this);
    private static final int RECIPE_INPUT = 0;
    private static final int RECIPE_CANDIDATE = 1;
    private static final RecipeCache.Search<VacuumCollapseRecipe> RECIPE_SEARCH =
            (level, input, unused) -> VacuumCollapseRecipes.find(level, input);
    private final RecipeCache<VacuumCollapseRecipe> recipes = new RecipeCache<>(2);
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_RESIDUE = 1;
    public static final int PROCESS_SLOT_COUNT = 2;
    public static final int SLOT_VOID_CHAMBER = 0;
    public static final int SLOT_COLLAPSE_NOZZLE = 1;
    public static final int SLOT_DIMENSIONAL_STABILIZER = 2;
    public static final int GEAR_SLOT_COUNT = 3;
    private static final int LEGACY_SLOT_ENERGY_CONNECTOR = 3;

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
    private static final int DATA_EFFICIENCY = 16;
    private static final int DATA_PROCESSING_SPEED = 17;
    private static final int DATA_STABILITY = 18;
    private static final int DATA_REFINEMENT_POTENTIAL = 19;
    private static final int DATA_FLAT_ENERGY_GENERATION = 20;
    private static final int DATA_BASE_ENERGY_GENERATION = 21;
    private static final int DATA_DEPTH = 22;
    private static final int DATA_CURVE_DEPTH = 23;
    private static final int DATA_PEAK_LOW = 24;
    private static final int DATA_PEAK_HIGH = 25;
    private static final int DATA_AVERAGE_LOW = 26;
    private static final int DATA_AVERAGE_HIGH = 27;
    private static final int STAT_SCALE = 100;
    private static final int BASE_TRANSFER = 8192;
    private static final int VISUAL_SYNC_INTERVAL = 5;

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
                case DATA_MAX_OUTPUT -> energyFlow.lastOutput();
                case DATA_RECIPE_ENERGY_LOW -> lowInt(currentRecipeEnergy(stats));
                case DATA_RECIPE_ENERGY_HIGH -> highInt(currentRecipeEnergy(stats));
                case DATA_PROCESSING_LEVEL -> stats.intValue(MachineStat.PROCESSING_LEVEL);
                case DATA_STATUS -> statusCode(stats);
                case DATA_INSTABILITY -> (int) Math.round(activeInstability * STAT_SCALE);
                case DATA_ENERGY_GENERATION -> (int) Math.round(stats.effectiveEnergyGenerationMultiplier() * STAT_SCALE);
                case DATA_FLAT_ENERGY_GENERATION -> (int) Math.round(stats.effectiveFlatEnergyGenerationBonus() * STAT_SCALE);
                case DATA_BASE_ENERGY_GENERATION -> (int) Math.round(baseEnergyPerTick() * STAT_SCALE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_STABILITY -> scaledStat(stats, MachineStat.STABILITY);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_DEPTH -> depth;
                case DATA_CURVE_DEPTH -> curveDepth();
                case DATA_PEAK_LOW -> lowInt(peakEnergyPerTick(stats));
                case DATA_PEAK_HIGH -> highInt(peakEnergyPerTick(stats));
                case DATA_AVERAGE_LOW -> lowInt(collapseAverageEnergyPerTick(stats));
                case DATA_AVERAGE_HIGH -> highInt(collapseAverageEnergyPerTick(stats));
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_AVERAGE_HIGH + 1;
        }
    };

    private int progress;
    private int activeProcessingTicks;
    private long energyStored;
    private long activeTotalEnergy;
    private long remainingEnergy;
    private double activeInstability;
    private int depth = VacuumCollapseDynamics.RATED_DEPTH;
    private int activeDepth = VacuumCollapseDynamics.RATED_DEPTH;
    private ItemStack pendingResidue = ItemStack.EMPTY;
    private VacuumCollapseVisualState visualState = VacuumCollapseVisualState.IDLE;
    private VacuumCollapseVisualState.Key lastSyncedVisual;
    private final VacuumCollapseVisualState.Animation animation = new VacuumCollapseVisualState.Animation();

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
        trackStatSlots(gearInventory);
        if (!(blockState.getBlock() instanceof VacuumCollapseGeneratorBlock)) {
            throw new IllegalStateException("Vacuum collapse generator block entity created for non-generator block: " + blockState);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VacuumCollapseGeneratorBlockEntity generator) {
        generator.releaseRemovedGear(level);
        // Export first so downstream demand creates headroom before generation is capacity-gated.
        boolean exported = generator.exportEnergy(level, pos);
        boolean generated = generator.tickGenerator();
        BaseMachineBlock.setActive(level, pos, state, generator.isWorking());
        if (generated || exported) {
            generator.setChanged();
        }
        generator.syncVisualState(level);
    }

    /**
     * Generators saved while the block was a solid cube stored no light inside it, which leaves the renderer's chamber
     * and pylons unlit. Rechecking the block's own light once on load fixes those saves.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            level.getLightEngine().checkBlock(worldPosition);
        }
    }

    /** Client copy of the state the renderer draws; synced from the server in the block entity update tag. */
    public VacuumCollapseVisualState visualState() {
        return visualState;
    }

    public VacuumCollapseVisualState.Animation animation() {
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
        visualState = VacuumCollapseVisualState.load(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        visualState = VacuumCollapseVisualState.load(packet.getTag());
    }

    private void syncVisualState(Level level) {
        if (level.getGameTime() % VISUAL_SYNC_INTERVAL != 0) {
            return;
        }
        VacuumCollapseVisualState.Key key = currentVisualState().key();
        if (!key.equals(lastSyncedVisual)) {
            lastSyncedVisual = key;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private VacuumCollapseVisualState currentVisualState() {
        boolean active = hasActiveRecipe();
        return new VacuumCollapseVisualState(
                isVoidChamber(chamberStack()) ? partMaterial(chamberStack()) : "",
                isCollapseNozzle(nozzleStack()) ? ((CollapseNozzleItem) nozzleStack().getItem()).material().getSerializedName() : "",
                isDimensionalStabilizer(stabilizerStack()) ? partMaterial(stabilizerStack()) : "",
                isWorking(),
                active ? Mth.clamp(progress / (float) Math.max(1, activeProcessingTicks), 0.0F, 1.0F) : 0.0F,
                active ? (float) activeInstability : 0.0F
        );
    }

    private static String partMaterial(ItemStack stack) {
        return ((VacuumCollapsePartItem) stack.getItem()).material().getSerializedName();
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
                .energyTelemetry(
                        energyFlow.lastInput(),
                        energyFlow.lastOutput(),
                        connector.transferRate(),
                        AdjacentEnergyConnector.outputBottleneck(connector, isWorking() ? clampInt(currentEnergyPerTick(stats)) : 0)
                )
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
        dropRemovedGear(level);
        dropRefinementInventory(level);
    }

    public boolean isKnownCatalyst(ItemStack stack) {
        return level != null && recipes.find(RECIPE_CANDIDATE, level, stack, RECIPE_SEARCH) != null;
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

    public MachineStatAccumulator effectiveStats() {
        return cachedStats(0L);
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.vacuumCollapseGenerator();
        stats.apply(effectiveTraits());
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
        tag.putInt("Depth", depth);
        tag.putInt("ActiveDepth", activeDepth);
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
        depth = tag.contains("Depth") ? VacuumCollapseDynamics.clampDepth(tag.getInt("Depth")) : VacuumCollapseDynamics.RATED_DEPTH;
        activeDepth = tag.contains("ActiveDepth") ? VacuumCollapseDynamics.clampDepth(tag.getInt("ActiveDepth")) : VacuumCollapseDynamics.RATED_DEPTH;
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
        ItemStack residue = residueFor(recipe, depth);
        if (!canMergeResidue(residue)) {
            return false;
        }

        consumeInput();
        activeDepth = depth;
        pendingResidue = residue;
        activeProcessingTicks = adjustedProcessingTicks(recipe, stats, activeDepth);
        activeTotalEnergy = effectiveRecipeEnergy(recipe, stats, activeDepth);
        remainingEnergy = activeTotalEnergy;
        activeInstability = instabilityPressure(recipe, stats, activeDepth);
        progress = 0;
        setChanged();
        return remainingEnergy > 0L;
    }

    private boolean generateActiveRecipe(MachineStatAccumulator stats) {
        if (!hasActiveRecipe() || !canMergeResidue(pendingResidue)) {
            return false;
        }

        // Output follows the collapse curve: low at the start, peaking at the end.
        int ticks = Math.max(1, activeProcessingTicks);
        long generated = activeTotalEnergy - remainingEnergy;
        long due = progress + 1 >= ticks
                ? remainingEnergy
                : (long) Math.floor(activeTotalEnergy * VacuumCollapseDynamics.cumulative(activeDepth, (progress + 1) / (double) ticks)) - generated;
        long wholeEnergy = Math.min(remainingEnergy, Math.max(0L, due));
        if (wholeEnergy > 0L) {
            // A collapse cannot pause: FE the buffer cannot hold is vented.
            long freeSpace = Math.max(0L, energyCapacity(stats) - energyStored);
            if (freeSpace > 0L) {
                receiveInternalEnergy(Math.min(wholeEnergy, freeSpace), stats, false);
            }
            remainingEnergy -= wholeEnergy;
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
        return level == null ? null : recipes.find(RECIPE_INPUT, level, processInventory.getStackInSlot(SLOT_INPUT), RECIPE_SEARCH);
    }

    /** How many recipe lookups searched the recipes instead of reusing a remembered result. */
    public int recipeSearches() {
        return recipes.searches();
    }

    private boolean canProcessRecipe(VacuumCollapseRecipe recipe, MachineStatAccumulator stats) {
        return stats.intValue(MachineStat.PROCESSING_LEVEL) >= recipe.minimumChamberStage();
    }

    private boolean hasMinimumStability(VacuumCollapseRecipe recipe, MachineStatAccumulator stats) {
        return stats.value(MachineStat.STABILITY) >= recipe.minimumStability();
    }

    private ItemStack residueFor(VacuumCollapseRecipe recipe, int depth) {
        if (recipe.residue().isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (recipe.residueRequiresStabilizer() && !isDimensionalStabilizer(stabilizerStack())) {
            return ItemStack.EMPTY;
        }
        ItemStack residue = recipe.residue().copy();
        double instability = instabilityPressure(recipe, effectiveStats(), depth);
        int extra = (int) Math.floor(instability);
        if (extra > 0) {
            residue.grow(Math.min(extra, residue.getMaxStackSize() - residue.getCount()));
        }
        return residue;
    }

    private long effectiveRecipeEnergy(VacuumCollapseRecipe recipe, MachineStatAccumulator stats, int depth) {
        double efficiency = Math.max(0.1, stats.value(MachineStat.EFFICIENCY));
        double instabilityPenalty = 1.0 - Math.min(0.35, instabilityPressure(recipe, stats, depth) * 0.08);
        int ticks = adjustedProcessingTicks(recipe, stats, depth);
        double baseEnergy = recipe.energy() * VacuumCollapseDynamics.energy(depth);
        return Math.max(1L, Math.round(RNGTechConfig.generatedEnergy(stats.generatedEnergyTotal(baseEnergy, ticks) * efficiency * instabilityPenalty)));
    }

    private int adjustedProcessingTicks(VacuumCollapseRecipe recipe, MachineStatAccumulator stats, int depth) {
        int depthTicks = Math.max(1, (int) Math.round(recipe.processingTicks() * VacuumCollapseDynamics.duration(depth)));
        int baseTicks = stats.adjustedProcessingTicks(depthTicks);
        double instability = instabilityPressure(recipe, stats, depth);
        return Math.max(1, (int) Math.round(baseTicks * (1.0 + Math.min(0.75, instability * 0.10))));
    }

    /** Depth multiplies the recipe's instability before stability divides it; an under-stage nozzle adds on top. */
    private double instabilityPressure(VacuumCollapseRecipe recipe, MachineStatAccumulator stats, int depth) {
        double stability = Math.max(0.1, stats.value(MachineStat.STABILITY));
        return Math.max(0.0, recipe.instability() * VacuumCollapseDynamics.instability(depth) / stability) + nozzleStagePenalty(recipe);
    }

    /** How many times an energy export looked a neighbour up instead of reusing its cached capability. */
    public int exportTargetLookups() {
        return exportTargets.lookups();
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        if (energyStored <= 0L) {
            return false;
        }

        int remainingOutput = clampInt(energyStored);
        boolean exported = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (remainingOutput <= 0 || energyStored <= 0L) {
                return exported;
            }

            IEnergyStorage target = exportTargets.get(level, pos, direction);
            if (target == null || !target.canReceive()) {
                continue;
            }

            int offered = extractEnergyInternal(remainingOutput, true);
            int received = target.receiveEnergy(offered, false);
            int delivered = extractEnergyInternal(received, false);
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

    private int extractEnergyInternal(int toExtract, boolean simulate) {
        if (toExtract <= 0) {
            return 0;
        }

        int extracted = Math.min(toExtract, clampInt(energyStored));
        if (!simulate && extracted > 0) {
            energyStored -= extracted;
            setChanged();
        }
        return extracted;
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
        return recipe == null || !canProcessRecipe(recipe, stats) ? 0 : adjustedProcessingTicks(recipe, stats, depth);
    }

    /** The FE/t at this point of the running collapse, or the next collapse's average while idle. */
    private long currentEnergyPerTick(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            int ticks = Math.max(1, activeProcessingTicks);
            double rate = VacuumCollapseDynamics.rate(activeDepth, progress / (double) ticks);
            return Math.max(0L, Math.round(activeTotalEnergy / (double) ticks * rate));
        }
        return averageEnergyPerTick(stats);
    }

    /** The FE/t the running or next collapse reaches at its end. */
    private long peakEnergyPerTick(MachineStatAccumulator stats) {
        return Math.round(collapseAverageEnergyPerTick(stats) * VacuumCollapseDynamics.peakRate(curveDepth()));
    }

    /** The running collapse's average FE/t, or the next collapse's while idle. */
    private long collapseAverageEnergyPerTick(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return Math.round(activeTotalEnergy / (double) Math.max(1, activeProcessingTicks));
        }
        return averageEnergyPerTick(stats);
    }

    private long averageEnergyPerTick(MachineStatAccumulator stats) {
        VacuumCollapseRecipe recipe = nextRecipe();
        if (recipe == null || !canProcessRecipe(recipe, stats)) {
            return 0L;
        }
        return Math.max(1L, Math.round(effectiveRecipeEnergy(recipe, stats, depth) / (double) Math.max(1, adjustedProcessingTicks(recipe, stats, depth))));
    }

    /** The depth whose curve the screen shows: the running collapse's, else the selected depth. */
    private int curveDepth() {
        return hasActiveRecipe() ? activeDepth : depth;
    }

    public int depth() {
        return depth;
    }

    /** FE the running collapse makes in total, or the next one at the selected depth; 0 without a usable catalyst. */
    public long collapseEnergy() {
        return currentRecipeEnergy(effectiveStats());
    }

    /** Sets the depth for the next collapse; a running collapse keeps the depth it started with. */
    public void setDepth(int depth) {
        int clamped = VacuumCollapseDynamics.clampDepth(depth);
        if (clamped != this.depth) {
            this.depth = clamped;
            setChanged();
        }
    }

    private long currentRecipeEnergy(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return activeTotalEnergy;
        }
        VacuumCollapseRecipe recipe = nextRecipe();
        return recipe == null || !canProcessRecipe(recipe, stats) ? 0L : effectiveRecipeEnergy(recipe, stats, depth);
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
        if (!canMergeResidue(residueFor(recipe, depth))) {
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
        activeInstability = 0.0;
        pendingResidue = ItemStack.EMPTY;
    }

    private long energyCapacity(MachineStatAccumulator stats) {
        return Math.max(1L, Math.round(stats.value(MachineStat.ENERGY_CAPACITY)));
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
        loadGearWithoutSlot(gearInventory, gearTag, registries, LEGACY_SLOT_ENERGY_CONNECTOR);
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
            return extractEnergyInternal(toExtract, simulate);
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
            return energyStored > 0L;
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
