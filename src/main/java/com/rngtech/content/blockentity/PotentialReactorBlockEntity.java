package com.rngtech.content.blockentity;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.PotentialReactorBlock;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.PotentialReactorPartItem;
import com.rngtech.content.menu.PotentialReactorMenu;
import com.rngtech.content.recipe.PotentialReactorRecipe;
import com.rngtech.content.recycling.RecyclingData;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class PotentialReactorBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider {
    private final EnergyExportTargets exportTargets = new EnergyExportTargets(this);
    private static final int RECIPE_INPUT = 0;
    private static final int RECIPE_CANDIDATE = 1;
    private static final RecipeCache.Search<PotentialReactorRecipe> RECIPE_SEARCH =
            (level, input, unused) -> PotentialReactorRecipes.find(level, input);
    private final RecipeCache<PotentialReactorRecipe> recipes = new RecipeCache<>(2);
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_RESIDUE = 1;
    public static final int PROCESS_SLOT_COUNT = 2;
    public static final int SLOT_REACTOR_CHAMBER = 0;
    public static final int SLOT_RECOVERY_FILTER = 1;
    public static final int SLOT_CONTAINMENT_LINING = 2;
    public static final int GEAR_SLOT_COUNT = 3;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_CHAMBER = 1;
    public static final int STATUS_NO_FUEL = 2;
    public static final int STATUS_INVALID_FUEL = 3;
    public static final int STATUS_BLOCKED_STAGE = 4;
    public static final int STATUS_OUTPUT_FULL = 5;
    public static final int STATUS_ENERGY_FULL = 6;
    public static final int STATUS_REDSTONE_DISABLED = 7;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_ENERGY_PER_TICK = 4;
    private static final int DATA_MAX_OUTPUT = 5;
    private static final int DATA_RECIPE_ENERGY = 6;
    private static final int DATA_PROCESSING_LEVEL = 7;
    private static final int DATA_STATUS = 8;
    private static final int DATA_ENERGY_GENERATION = 9;
    private static final int DATA_ENERGY_CAPACITY_STAT = 10;
    private static final int DATA_ENERGY_TRANSFER = 11;
    private static final int DATA_EFFICIENCY = 12;
    private static final int DATA_PROCESSING_SPEED = 13;
    private static final int DATA_STABILITY = 14;
    private static final int DATA_REFINEMENT_POTENTIAL = 15;
    private static final int DATA_FLAT_ENERGY_GENERATION = 16;
    private static final int DATA_BASE_ENERGY_GENERATION = 17;
    private static final int DATA_REPEAT_PAYOUT = 18;
    private static final int DATA_BURN_HISTORY = 19;
    private static final int STAT_SCALE = 100;
    private static final int VISUAL_SYNC_INTERVAL = 5;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT && isKnownReactorFuel(stack);
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
            if (slot == SLOT_INPUT && inputWatch.changedTo(getStackInSlot(slot))) {
                resetBulkSpeed();
            }
            setChanged();
        }
    };
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_REACTOR_CHAMBER -> isReactorChamber(stack);
                case SLOT_RECOVERY_FILTER -> isRecoveryFilter(stack);
                case SLOT_CONTAINMENT_LINING -> isContainmentLining(stack);
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
            resetBulkSpeed();
            setChanged();
        }
    };
    private final IItemHandler inputHandler = new InputItemHandler();
    private final IItemHandler residueHandler = new ResidueItemHandler();
    private final IEnergyStorage energyStorage = new ReactorEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> currentProcessingTicks(stats);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity(stats);
                case DATA_ENERGY_PER_TICK -> currentEnergyPerTick(stats);
                case DATA_MAX_OUTPUT -> energyFlow.lastOutput();
                case DATA_RECIPE_ENERGY -> currentRecipeEnergy(stats);
                case DATA_PROCESSING_LEVEL -> stats.intValue(MachineStat.PROCESSING_LEVEL);
                case DATA_STATUS -> statusCode(stats);
                case DATA_ENERGY_GENERATION -> (int) Math.round(stats.effectiveEnergyGenerationMultiplier() * STAT_SCALE);
                case DATA_FLAT_ENERGY_GENERATION -> (int) Math.round(stats.effectiveFlatEnergyGenerationBonus() * STAT_SCALE);
                case DATA_BASE_ENERGY_GENERATION -> (int) Math.round(baseEnergyPerTick() * STAT_SCALE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_STABILITY -> scaledStat(stats, MachineStat.STABILITY);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_REPEAT_PAYOUT -> repeatPayoutPercent();
                default -> index >= DATA_BURN_HISTORY ? burnHistoryItemId(index - DATA_BURN_HISTORY) : 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_BURN_HISTORY + BURN_MEMORY;
        }
    };

    private final BulkSpeedState bulkSpeed = new BulkSpeedState();
    private final BulkSpeedState.InputWatch inputWatch = new BulkSpeedState.InputWatch();
    private int progress;
    private int activeProcessingTicks;
    private int internalEnergy;
    private double activeTotalEnergy;
    private double remainingEnergy;
    private double generationCarry;
    private ItemStack pendingResidue = ItemStack.EMPTY;
    /** What the active run burns, for the core colour: salvage fuel, or the rarity of the recycled item. */
    private int burn = PotentialReactorVisualState.BURN_SALVAGE;
    private PotentialReactorVisualState visualState = PotentialReactorVisualState.IDLE;
    private PotentialReactorVisualState.Key lastSyncedVisual;
    private final PotentialReactorVisualState.Animation animation = new PotentialReactorVisualState.Animation();

    private record ReactorWork(
            int energy,
            int processingTicks,
            int minimumMaterialStage,
            ItemStack residue,
            boolean requiresRecoveryFilter,
            String gearId,
            Rarity rarity
    ) {
    }

    /** How many recent gear burns the reactor remembers. */
    public static final int BURN_MEMORY = 15;
    private static final double GEAR_FATIGUE_PER_REPEAT = 0.75;
    private static final double GEAR_FATIGUE_FLOOR = 0.10;
    /** Item ids of the most recent gear burns, oldest first; each repeat of an id lowers that item's FE. */
    private final List<String> recentGear = new ArrayList<>();

    public PotentialReactorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.POTENTIAL_REACTOR.get(), pos, blockState, MachineType.POTENTIAL_REACTOR, SLOT_INPUT, SLOT_INPUT, SLOT_RESIDUE);
        trackStatSlots(gearInventory);
        if (!(blockState.getBlock() instanceof PotentialReactorBlock)) {
            throw new IllegalStateException("Potential reactor block entity created for non-reactor block: " + blockState);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PotentialReactorBlockEntity reactor) {
        boolean generated = reactor.tickReactor();
        boolean exported = reactor.exportEnergy(level, pos);
        BaseMachineBlock.setActive(level, pos, state, reactor.isWorking());
        if (generated || exported) {
            reactor.setChanged();
        }
        reactor.syncVisualState(level);
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

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        int status = statusCode(stats);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSource(level, worldPosition);
        int generation = currentEnergyPerTick(stats);
        return MachineInfoSnapshot.builder("potential_reactor")
                .stage(refinementComponentStage())
                .state(machineInfoState(status), reactorBlockedReason(status))
                .progress(progress, currentProcessingTicks(stats))
                .energy(energyStored(), energyCapacity(stats), generation)
                .energyTelemetry(
                        energyFlow.lastInput(),
                        energyFlow.lastOutput(),
                        connector.transferRate(),
                        AdjacentEnergyConnector.outputBottleneck(connector, generation)
                )
                .processingLevel(stats.intValue(MachineStat.PROCESSING_LEVEL), currentRequiredProcessingLevel())
                .gear(status == STATUS_MISSING_CHAMBER
                        ? MachineInfoSnapshot.GearSummary.MISSING_REACTOR_CHAMBER
                        : MachineInfoSnapshot.GearSummary.REACTOR_CHAMBER_INSTALLED)
                .output(reactorOutputSummary(status))
                .refinement(machineTraits())
                .build();
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return side == Direction.UP || side == Direction.DOWN ? null : trackedEnergyStorage;
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
                Component.translatable("container.rngtech.potential_reactor")
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PotentialReactorMenu(containerId, playerInventory, this, menuData);
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

    public boolean isKnownReactorFuel(ItemStack stack) {
        return RecyclingData.hasRecyclableRpgValue(stack)
                || level != null && recipes.find(RECIPE_CANDIDATE, level, stack, RECIPE_SEARCH) != null;
    }

    public boolean isReactorChamber(ItemStack stack) {
        return stack.getItem() instanceof PotentialReactorPartItem part
                && part.partType() == MachinePartType.REACTOR_CHAMBER
                && part.machineType() == MachineType.POTENTIAL_REACTOR;
    }

    public boolean isRecoveryFilter(ItemStack stack) {
        return stack.getItem() instanceof PotentialReactorPartItem part
                && part.partType() == MachinePartType.RECOVERY_FILTER
                && part.machineType() == MachineType.POTENTIAL_REACTOR;
    }

    public boolean isContainmentLining(ItemStack stack) {
        return stack.getItem() instanceof PotentialReactorPartItem part
                && part.partType() == MachinePartType.CONTAINMENT_LINING
                && part.machineType() == MachineType.POTENTIAL_REACTOR;
    }

    public MachineStatAccumulator effectiveStats() {
        return cachedStats(bulkSpeed.count());
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.potentialReactor();
        stats.apply(effectiveTraits());
        applyPartStats(stats, chamberStack());
        applyPartStats(stats, filterStack());
        applyPartStats(stats, liningStack());
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
        tag.putInt("Progress", progress);
        tag.putInt("ActiveProcessingTicks", activeProcessingTicks);
        tag.putInt("Energy", internalEnergy);
        tag.putDouble("ActiveTotalEnergy", activeTotalEnergy);
        tag.putDouble("RemainingEnergy", remainingEnergy);
        tag.putDouble("GenerationCarry", generationCarry);
        tag.put("PendingResidue", pendingResidue.saveOptional(registries));
        tag.putInt("Burn", burn);
        ListTag gear = new ListTag();
        recentGear.forEach(id -> gear.add(StringTag.valueOf(id)));
        tag.put("RecentGear", gear);
        bulkSpeed.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        inputWatch.prime(processInventory.getStackInSlot(SLOT_INPUT));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        progress = tag.getInt("Progress");
        activeProcessingTicks = tag.getInt("ActiveProcessingTicks");
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        activeTotalEnergy = tag.getDouble("ActiveTotalEnergy");
        remainingEnergy = tag.getDouble("RemainingEnergy");
        generationCarry = tag.getDouble("GenerationCarry");
        pendingResidue = ItemStack.parseOptional(registries, tag.getCompound("PendingResidue"));
        burn = tag.contains("Burn", Tag.TAG_INT) ? tag.getInt("Burn") : PotentialReactorVisualState.BURN_SALVAGE;
        recentGear.clear();
        ListTag gear = tag.getList("RecentGear", Tag.TAG_STRING);
        for (int index = 0; index < gear.size(); index++) {
            recordGear(gear.getString(index));
        }
        bulkSpeed.load(tag);
        clampInternalEnergy();
    }

    private boolean tickReactor() {
        if (level == null || redstoneDisabled() || !hasRequiredComponents()) {
            resetBulkSpeed();
            return false;
        }

        MachineStatAccumulator stats = effectiveStats();
        if (!hasActiveRecipe() && !tryStartRecipe(stats)) {
            return false;
        }
        if (energyStored() >= energyCapacity(stats)) {
            return false;
        }
        return generateActiveRecipe(stats);
    }

    private boolean tryStartRecipe(MachineStatAccumulator stats) {
        ReactorWork work = nextWork();
        if (work == null || !canProcessStage(work, stats)) {
            resetInactiveProgress();
            return false;
        }

        ItemStack residue = residueFor(work);
        if (!canMergeResidue(residue) || energyStored() >= energyCapacity(stats)) {
            return false;
        }

        if (work.gearId() != null) {
            recordGear(work.gearId());
        }
        consumeInput();
        pendingResidue = residue;
        burn = work.rarity() == null ? PotentialReactorVisualState.BURN_SALVAGE : work.rarity().ordinal();
        activeProcessingTicks = stats.adjustedProcessingTicks(work.processingTicks());
        activeTotalEnergy = effectiveRecipeEnergy(work, stats);
        remainingEnergy = activeTotalEnergy;
        generationCarry = 0.0;
        progress = 0;
        setChanged();
        return remainingEnergy > 0.0;
    }

    private boolean generateActiveRecipe(MachineStatAccumulator stats) {
        if (!hasActiveRecipe()) {
            return false;
        }
        if (!canMergeResidue(pendingResidue)) {
            return false;
        }

        int capacity = energyCapacity(stats);
        int freeSpace = capacity - energyStored();
        if (freeSpace <= 0) {
            return false;
        }

        double perTick = activeTotalEnergy / Math.max(1, activeProcessingTicks);
        generationCarry += Math.min(perTick, remainingEnergy);
        int wholeEnergy = Math.min((int) Math.floor(generationCarry), freeSpace);
        if (wholeEnergy <= 0) {
            progress++;
            finishIfComplete();
            return true;
        }

        int accepted = receiveInternalEnergy(wholeEnergy, stats, false);
        if (accepted <= 0) {
            return false;
        }
        generationCarry -= accepted;
        remainingEnergy = Math.max(0.0, remainingEnergy - accepted);
        progress++;
        finishIfComplete();
        return true;
    }

    private void finishIfComplete() {
        if (progress < activeProcessingTicks && remainingEnergy > 0.0001) {
            return;
        }
        if (!canMergeResidue(pendingResidue)) {
            return;
        }
        mergeResidue(pendingResidue);
        bulkSpeed.recordProcess(activeTraits());
        clearActiveRecipe();
        setChanged();
    }

    /** The share of its FE a gear burn pays after this many earlier burns of the same item. */
    public static double repeatPayout(long repeats) {
        return Math.max(GEAR_FATIGUE_FLOOR, Math.pow(GEAR_FATIGUE_PER_REPEAT, repeats));
    }

    private double gearFatigue(String gearId) {
        return repeatPayout(recentGear.stream().filter(gearId::equals).count());
    }

    private void recordGear(String gearId) {
        recentGear.add(gearId);
        while (recentGear.size() > BURN_MEMORY) {
            recentGear.removeFirst();
        }
    }

    /** Item ids of the remembered gear burns, oldest first. */
    public List<String> recentGearIds() {
        return List.copyOf(recentGear);
    }

    /** The payout percent the gear in the input slot gets from its earlier burns, or -1 when the input holds no gear. */
    public int repeatPayoutPercent() {
        String gearId = inputGearId();
        return gearId == null ? -1 : (int) Math.round(gearFatigue(gearId) * 100.0);
    }

    /** How many remembered burns match the gear in the input slot; 0 when it holds none. */
    public int inputGearRepeats() {
        String gearId = inputGearId();
        return gearId == null ? 0 : (int) recentGear.stream().filter(gearId::equals).count();
    }

    private String inputGearId() {
        ItemStack input = processInventory.getStackInSlot(SLOT_INPUT);
        return RecyclingData.hasRecyclableRpgValue(input) ? BuiltInRegistries.ITEM.getKey(input.getItem()).toString() : null;
    }

    /** The registry id + 1 of a remembered burn for the menu, or 0 for an empty or unknown entry. */
    private int burnHistoryItemId(int index) {
        if (index >= recentGear.size()) {
            return 0;
        }
        ResourceLocation id = ResourceLocation.tryParse(recentGear.get(index));
        return id == null ? 0 : BuiltInRegistries.ITEM.getOptional(id).map(item -> BuiltInRegistries.ITEM.getId(item) + 1).orElse(0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!recentGear.isEmpty()) {
            components.set(ModDataComponents.REACTOR_BURN_HISTORY.get(), List.copyOf(recentGear));
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        List<String> history = componentInput.get(ModDataComponents.REACTOR_BURN_HISTORY.get());
        if (history != null) {
            recentGear.clear();
            history.forEach(this::recordGear);
        }
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

    private PotentialReactorRecipe nextRecipe() {
        return level == null ? null : recipes.find(RECIPE_INPUT, level, processInventory.getStackInSlot(SLOT_INPUT), RECIPE_SEARCH);
    }

    /** How many recipe lookups searched the recipes instead of reusing a remembered result. */
    public int recipeSearches() {
        return recipes.searches();
    }

    private ReactorWork nextWork() {
        ItemStack input = processInventory.getStackInSlot(SLOT_INPUT);
        if (RecyclingData.hasRecyclableRpgValue(input)) {
            String gearId = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
            int energy = (int) Math.round(RecyclingData.reactorFuelValue(input) * gearFatigue(gearId));
            int stage = Math.max(1, RecyclingData.componentStage(input));
            int ticks = Mth.clamp(80 + stage * 15 + energy / 240, 100, 280);
            Rarity rarity = RefinementTargets.storedTraits(input).rarity();
            return new ReactorWork(energy, ticks, 1, RecyclingData.strippedCopy(input), false, gearId, rarity);
        }
        PotentialReactorRecipe recipe = nextRecipe();
        return recipe == null
                ? null
                : new ReactorWork(
                        recipe.energy(),
                        recipe.processingTicks(),
                        recipe.minimumMaterialStage(),
                        recipe.residue().copy(),
                        true,
                        null,
                        null
                );
    }

    private boolean canProcessStage(ReactorWork work, MachineStatAccumulator stats) {
        return stats.intValue(MachineStat.PROCESSING_LEVEL) >= work.minimumMaterialStage();
    }

    private ItemStack residueFor(ReactorWork work) {
        if ((work.requiresRecoveryFilter() && !hasRecoveryFilter()) || work.residue().isEmpty()) {
            return ItemStack.EMPTY;
        }
        return work.residue().copy();
    }

    private double effectiveRecipeEnergy(ReactorWork work, MachineStatAccumulator stats) {
        double efficiency = Math.max(0.1, stats.value(MachineStat.EFFICIENCY));
        double stability = Mth.clamp(stats.value(MachineStat.STABILITY), 0.25, 1.50);
        int ticks = stats.adjustedProcessingTicks(work.processingTicks());
        return Math.max(1.0, RNGTechConfig.generatedEnergy(stats.generatedEnergyTotal(work.energy(), ticks) * efficiency * stability));
    }

    /** How many times an energy export looked a neighbour up instead of reusing its cached capability. */
    public int exportTargetLookups() {
        return exportTargets.lookups();
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        if (energyStored() <= 0) {
            return false;
        }

        MachineStatAccumulator stats = effectiveStats();
        int remainingOutput = energyStored();
        boolean exported = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (remainingOutput <= 0 || energyStored() <= 0) {
                return exported;
            }

            IEnergyStorage target = exportTargets.get(level, pos, direction);
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

    private int receiveInternalEnergy(int toReceive, MachineStatAccumulator stats, boolean simulate) {
        if (toReceive <= 0) {
            return 0;
        }

        int received = Math.min(toReceive, energyCapacity(stats) - energyStored());
        if (!simulate && received > 0) {
            internalEnergy = energyStored() + received;
            setChanged();
        }
        return received;
    }

    private int extractEnergyInternal(int toExtract, MachineStatAccumulator stats, boolean simulate) {
        if (toExtract <= 0) {
            return 0;
        }

        int extracted = Math.min(toExtract, energyStored());
        if (!simulate && extracted > 0) {
            internalEnergy = energyStored() - extracted;
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
        ReactorWork work = nextWork();
        return work == null || !canProcessStage(work, stats) ? 0 : stats.adjustedProcessingTicks(work.processingTicks());
    }

    private int currentEnergyPerTick(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return Math.max(0, (int) Math.round(activeTotalEnergy / Math.max(1, activeProcessingTicks)));
        }
        ReactorWork work = nextWork();
        if (work == null || !canProcessStage(work, stats)) {
            return 0;
        }
        return Math.max(1, (int) Math.round(effectiveRecipeEnergy(work, stats) / Math.max(1, stats.adjustedProcessingTicks(work.processingTicks()))));
    }

    private int currentRecipeEnergy(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return Math.max(0, (int) Math.round(activeTotalEnergy));
        }
        ReactorWork work = nextWork();
        return work == null || !canProcessStage(work, stats) ? 0 : Math.max(1, (int) Math.round(effectiveRecipeEnergy(work, stats)));
    }

    private int statusCode(MachineStatAccumulator stats) {
        if (redstoneDisabled()) {
            return STATUS_REDSTONE_DISABLED;
        }
        if (!hasRequiredComponents()) {
            return STATUS_MISSING_CHAMBER;
        }
        if (hasActiveRecipe()) {
            if (!canMergeResidue(pendingResidue)) {
                return STATUS_OUTPUT_FULL;
            }
            return energyStored() >= energyCapacity(stats) ? STATUS_ENERGY_FULL : STATUS_READY;
        }

        ItemStack input = processInventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) {
            return STATUS_NO_FUEL;
        }

        ReactorWork work = nextWork();
        if (work == null) {
            return STATUS_INVALID_FUEL;
        }
        if (!canProcessStage(work, stats)) {
            return STATUS_BLOCKED_STAGE;
        }
        if (!canMergeResidue(residueFor(work))) {
            return STATUS_OUTPUT_FULL;
        }
        return energyStored() >= energyCapacity(stats) ? STATUS_ENERGY_FULL : STATUS_READY;
    }

    private int currentRequiredProcessingLevel() {
        ReactorWork work = hasActiveRecipe() ? null : nextWork();
        return work == null ? MachineInfoSnapshot.UNSET : work.minimumMaterialStage();
    }

    private MachineInfoSnapshot.WorkState machineInfoState(int status) {
        if (status == STATUS_REDSTONE_DISABLED) {
            return MachineInfoSnapshot.WorkState.PAUSED;
        }
        if (status == STATUS_READY) {
            return getBlockState().getValue(BaseMachineBlock.ACTIVE) || isWorking()
                    ? MachineInfoSnapshot.WorkState.RUNNING
                    : MachineInfoSnapshot.WorkState.IDLE;
        }
        return status == STATUS_NO_FUEL ? MachineInfoSnapshot.WorkState.IDLE : MachineInfoSnapshot.WorkState.BLOCKED;
    }

    private MachineInfoSnapshot.BlockedReason reactorBlockedReason(int status) {
        return switch (status) {
            case STATUS_MISSING_CHAMBER -> MachineInfoSnapshot.BlockedReason.MISSING_REACTOR_CHAMBER;
            case STATUS_NO_FUEL -> MachineInfoSnapshot.BlockedReason.NO_FUEL;
            case STATUS_INVALID_FUEL -> MachineInfoSnapshot.BlockedReason.INVALID_FUEL;
            case STATUS_BLOCKED_STAGE -> MachineInfoSnapshot.BlockedReason.BLOCKED_LEVEL;
            case STATUS_OUTPUT_FULL -> MachineInfoSnapshot.BlockedReason.OUTPUT_FULL;
            case STATUS_ENERGY_FULL -> MachineInfoSnapshot.BlockedReason.ENERGY_FULL;
            case STATUS_REDSTONE_DISABLED -> MachineInfoSnapshot.BlockedReason.REDSTONE_PAUSED;
            default -> MachineInfoSnapshot.BlockedReason.NONE;
        };
    }

    private MachineInfoSnapshot.OutputSummary reactorOutputSummary(int status) {
        if (status == STATUS_OUTPUT_FULL) {
            return MachineInfoSnapshot.OutputSummary.RESIDUE_BLOCKED;
        }
        if (status == STATUS_ENERGY_FULL) {
            return MachineInfoSnapshot.OutputSummary.ENERGY_FULL;
        }
        return hasRecoveryFilter() ? MachineInfoSnapshot.OutputSummary.RESIDUE_ENABLED : MachineInfoSnapshot.OutputSummary.NONE;
    }

    private boolean hasRequiredComponents() {
        return isReactorChamber(chamberStack());
    }

    private boolean hasRecoveryFilter() {
        return isRecoveryFilter(filterStack()) && MachineImplicitCatalog.hasBehavior(filterStack(), MachineBehavior.RESIDUE_SCREEN);
    }

    private boolean hasActiveRecipe() {
        return activeProcessingTicks > 0 && remainingEnergy > 0.0001;
    }

    private boolean isWorking() {
        return hasActiveRecipe() && !redstoneDisabled();
    }

    private boolean redstoneDisabled() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    private void resetInactiveProgress() {
        resetBulkSpeed();
        if (!hasActiveRecipe() && progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    private void clearActiveRecipe() {
        progress = 0;
        activeProcessingTicks = 0;
        activeTotalEnergy = 0.0;
        remainingEnergy = 0.0;
        generationCarry = 0.0;
        pendingResidue = ItemStack.EMPTY;
    }

    private void resetBulkSpeed() {
        if (bulkSpeed.reset()) {
            setChanged();
        }
    }

    private int energyStored() {
        return Math.max(0, internalEnergy);
    }

    private int energyCapacity(MachineStatAccumulator stats) {
        return Math.max(1, (int) Math.round(stats.value(MachineStat.ENERGY_CAPACITY)));
    }

    private void clampInternalEnergy() {
        internalEnergy = Math.min(energyStored(), energyCapacity(effectiveStats()));
    }

    private ItemStack chamberStack() {
        return gearInventory.getStackInSlot(SLOT_REACTOR_CHAMBER);
    }

    private ItemStack filterStack() {
        return gearInventory.getStackInSlot(SLOT_RECOVERY_FILTER);
    }

    private ItemStack liningStack() {
        return gearInventory.getStackInSlot(SLOT_CONTAINMENT_LINING);
    }

    private void applyPartStats(MachineStatAccumulator stats, ItemStack stack) {
        if (stack.getItem() instanceof MachinePartItem part && part.machineType() == MachineType.POTENTIAL_REACTOR) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        }
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
    }

    /**
     * Reactors saved while the block was a solid cube stored no light inside it, which leaves the renderer's core
     * assembly unlit. Rechecking the block's own light once on load fixes those saves.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            level.getLightEngine().checkBlock(worldPosition);
        }
    }

    /** Client copy of the state the renderer draws; synced from the server in the block entity update tag. */
    public PotentialReactorVisualState visualState() {
        return visualState;
    }

    public PotentialReactorVisualState.Animation animation() {
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
        visualState = PotentialReactorVisualState.load(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        visualState = PotentialReactorVisualState.load(packet.getTag());
    }

    private void syncVisualState(Level level) {
        if (level.getGameTime() % VISUAL_SYNC_INTERVAL != 0) {
            return;
        }
        PotentialReactorVisualState.Key key = currentVisualState().key();
        if (!key.equals(lastSyncedVisual)) {
            lastSyncedVisual = key;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The visual state the server would send now. */
    public PotentialReactorVisualState currentVisualState() {
        MachineStatAccumulator stats = effectiveStats();
        int core = PotentialReactorVisualState.CORE_OFF;
        if (isWorking()) {
            core = statusCode(stats) == STATUS_READY ? PotentialReactorVisualState.CORE_REACTING : PotentialReactorVisualState.CORE_HELD;
        }
        ItemStack residue = processInventory.getStackInSlot(SLOT_RESIDUE);
        int residueLimit = Math.max(1, Math.min(residue.getMaxStackSize(), processInventory.getSlotLimit(SLOT_RESIDUE)));
        return new PotentialReactorVisualState(
                installedMaterial(SLOT_REACTOR_CHAMBER),
                installedMaterial(SLOT_RECOVERY_FILTER),
                installedMaterial(SLOT_CONTAINMENT_LINING),
                core,
                burn,
                core == PotentialReactorVisualState.CORE_OFF ? 0.0F : PotentialReactorVisualState.intensity(activeTotalEnergy / Math.max(1, activeProcessingTicks)),
                (float) Mth.clamp(stats.value(MachineStat.STABILITY), 0.25, 1.50),
                energyStored() / (float) energyCapacity(stats),
                residue.isEmpty() ? 0.0F : Math.min(1.0F, residue.getCount() / (float) residueLimit)
        );
    }

    /** The material of the part installed in a Gear slot, or an empty string when the slot holds none. */
    private String installedMaterial(int slot) {
        ItemStack stack = gearInventory.getStackInSlot(slot);
        if (!gearInventory.isItemValid(slot, stack) || !(stack.getItem() instanceof PotentialReactorPartItem part)) {
            return "";
        }
        if (part.chamberMaterial() != null) {
            return part.chamberMaterial().getSerializedName();
        }
        return part.filterMaterial() != null ? part.filterMaterial().getSerializedName() : part.liningMaterial().getSerializedName();
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

    private final class ReactorEnergyStorage implements IEnergyStorage {
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
            return energyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyCapacity(effectiveStats());
        }

        @Override
        public boolean canExtract() {
            return energyStored() > 0;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

    /** Generation before machine and part stats, for the Stats tab breakdown; 0 when unknown. */
    private double baseEnergyPerTick() {
        ReactorWork work = nextWork();
        return work == null ? 0.0 : work.energy() / (double) Math.max(1, work.processingTicks());
    }
}
