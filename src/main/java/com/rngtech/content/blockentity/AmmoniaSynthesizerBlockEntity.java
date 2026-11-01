package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.gear.IdentifyingItemHandler;
import com.rngtech.content.item.AmmoniaPartItem;
import com.rngtech.content.menu.AmmoniaSynthesizerMenu;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.AmmoniaSynthesisRecipe;
import com.rngtech.content.recipe.UreaSynthesisRecipe;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.util.MachineEnergyStorage;

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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public class AmmoniaSynthesizerBlockEntity extends BaseMachineBlockEntity
        implements MenuProvider, PurgeableFluidStorage, MachineInfoProvider {
    private final RecipeCache<AmmoniaSynthesisRecipe> recipes = new RecipeCache<>(1);
    private final RecipeCache<UreaSynthesisRecipe> ureaRecipes = new RecipeCache<>(1);
    private final BulkSpeedState bulkSpeed = new BulkSpeedState();
    public static final int SLOT_CATALYST_BED = 0;
    public static final int GEAR_SLOT_COUNT = 1;
    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_CATALYST = 1;
    public static final int STATUS_NO_RECIPE = 2;
    public static final int STATUS_NO_POWER = 3;
    public static final int STATUS_OUTPUT_FULL = 4;
    public static final int STATUS_NO_FLUID = 5;
    public static final int PURGE_NITROGEN_TANK = 0;
    public static final int PURGE_HYDROGEN_TANK = 1;
    public static final int PURGE_OUTPUT_TANK = 2;
    public static final int PURGE_CARBON_TANK = 3;
    public static final int SLOT_UREA = 0;

    private static final int TANK_CAPACITY = 8000;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_NITROGEN = 4;
    private static final int DATA_HYDROGEN = 5;
    private static final int DATA_AMMONIA = 6;
    private static final int DATA_TANK_CAPACITY = 7;
    private static final int DATA_STATUS = 8;
    private static final int DATA_ENERGY_USAGE = 9;
    private static final int DATA_ENERGY_TRANSFER = 10;
    private static final int DATA_PROCESSING_SPEED = 11;
    private static final int DATA_REFINEMENT_POTENTIAL = 12;
    private static final int DATA_CARBON = 13;
    private static final int STAT_SCALE = 100;
    private static final int VISUAL_SYNC_INTERVAL = 5;

    private final ItemStackHandler gearInventory = new IdentifyingItemHandler(GEAR_SLOT_COUNT, this::getLevel) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_CATALYST_BED && isCatalystBed(stack);
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
    private final MachineEnergyStorage energyStorage =
            new MachineEnergyStorage(this::internalEnergyCapacity, () -> Integer.MAX_VALUE, () -> 0, this::setChanged);
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final FluidTank nitrogenTank = tank(ModFluids.NITROGEN_SOURCE.get());
    private final FluidTank hydrogenTank = tank(ModFluids.HYDROGEN_SOURCE.get());
    private final FluidTank outputTank = tank(ModFluids.AMMONIA_SOURCE.get());
    private final FluidTank carbonTank = tank(ModFluids.CARBON_EXHAUST_SOURCE.get());
    /** Urea from the second recipe. Players and automation only take from it. */
    private final ItemStackHandler ureaOutput = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler ureaExtractor = new UreaExtractHandler();
    private final IFluidHandler fluidHandler = new SynthesisFluidHandler();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_PROGRESS -> syncedProgress(0, progress);
                case DATA_PROCESSING_TICKS -> activeTicks;
                case DATA_ENERGY -> energyStorage.getEnergyStored();
                case DATA_ENERGY_CAPACITY -> energyStorage.getMaxEnergyStored();
                case DATA_NITROGEN -> nitrogenTank.getFluidAmount();
                case DATA_HYDROGEN -> hydrogenTank.getFluidAmount();
                case DATA_AMMONIA -> outputTank.getFluidAmount();
                case DATA_TANK_CAPACITY -> nitrogenTank.getCapacity();
                case DATA_STATUS -> statusCode();
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_CARBON -> carbonTank.getFluidAmount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_CARBON + 1;
        }
    };

    private int progress;
    /** Whether the running cycle makes Urea; switching recipes restarts the cycle. */
    private boolean ureaCycle;
    private int activeTicks;
    private int activeEnergy;
    private AmmoniaSynthesizerVisualState visualState = AmmoniaSynthesizerVisualState.IDLE;
    private AmmoniaSynthesizerVisualState.Key lastSyncedVisual;
    private final AmmoniaSynthesizerVisualState.Animation animation = new AmmoniaSynthesizerVisualState.Animation();

    public AmmoniaSynthesizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.AMMONIA_SYNTHESIZER.get(), pos, blockState, MachineType.AMMONIA_SYNTHESIZER, 0, 0, 0);
        trackStatSlots(gearInventory);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmmoniaSynthesizerBlockEntity synthesizer) {
        synthesizer.updateTankCapacity(synthesizer.effectiveStats());
        synthesizer.repairMisroutedInputFluids();
        boolean worked = synthesizer.tickRecipe();
        BaseMachineBlock.setActive(level, pos, state, worked);
        if (worked) {
            synthesizer.setChanged();
        }
        synthesizer.syncVisualState(level);
    }

    /**
     * Synthesizers saved while the block was a solid cube stored no light inside it, which leaves the renderer's parts
     * unlit. Rechecking the block's own light once on load fixes those saves.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            level.getLightEngine().checkBlock(worldPosition);
        }
    }

    /** Client copy of the state the renderer draws; synced from the server in the block entity update tag. */
    public AmmoniaSynthesizerVisualState visualState() {
        return visualState;
    }

    public AmmoniaSynthesizerVisualState.Animation animation() {
        return animation;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return withNoiseFactor(currentVisualState().save());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readNoiseFactor(tag);
        visualState = AmmoniaSynthesizerVisualState.load(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        readNoiseFactor(packet.getTag());
        visualState = AmmoniaSynthesizerVisualState.load(packet.getTag());
    }

    private void syncVisualState(Level level) {
        if (level.getGameTime() % VISUAL_SYNC_INTERVAL != 0) {
            return;
        }
        AmmoniaSynthesizerVisualState.Key key = currentVisualState().key();
        if (!key.equals(lastSyncedVisual)) {
            lastSyncedVisual = key;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    AmmoniaSynthesizerVisualState currentVisualState() {
        return new AmmoniaSynthesizerVisualState(
                isActive(),
                (float) effectiveStats().value(MachineStat.PROCESSING_SPEED),
                activeTicks <= 0 ? 0.0F : Mth.clamp(progress / (float) activeTicks, 0.0F, 1.0F),
                nitrogenTank.getFluidAmount() / (float) nitrogenTank.getCapacity(),
                hydrogenTank.getFluidAmount() / (float) hydrogenTank.getCapacity(),
                outputTank.getFluidAmount() / (float) outputTank.getCapacity(),
                isCatalystBed(catalystStack())
        );
    }

    public ItemStackHandler getGearInventory() {
        return gearInventory;
    }

    public ItemStackHandler getUreaOutput() {
        return ureaOutput;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return side == null || side == Direction.DOWN ? null : trackedEnergyStorage;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return fluidHandler;
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        return List.of(
                FluidPurgeTarget.of(
                        PURGE_NITROGEN_TANK,
                        Component.translatable("rngtech.purge.target.nitrogen_tank"),
                        FluidPurgeRole.INPUT,
                        nitrogenTank::getFluid,
                        nitrogenTank::drain,
                        this::clearActiveRecipe,
                        this::setChanged
                ).withCapacity(nitrogenTank::getCapacity),
                FluidPurgeTarget.of(
                        PURGE_HYDROGEN_TANK,
                        Component.translatable("rngtech.purge.target.hydrogen_tank"),
                        FluidPurgeRole.INPUT,
                        hydrogenTank::getFluid,
                        hydrogenTank::drain,
                        this::clearActiveRecipe,
                        this::setChanged
                ).withCapacity(hydrogenTank::getCapacity),
                FluidPurgeTarget.of(
                        PURGE_OUTPUT_TANK,
                        Component.translatable("rngtech.purge.target.output_tank"),
                        FluidPurgeRole.OUTPUT,
                        outputTank::getFluid,
                        outputTank::drain,
                        this::setChanged
                ).withCapacity(outputTank::getCapacity),
                FluidPurgeTarget.of(
                        PURGE_CARBON_TANK,
                        Component.translatable("rngtech.purge.target.carbon_tank"),
                        FluidPurgeRole.INPUT,
                        carbonTank::getFluid,
                        carbonTank::drain,
                        this::clearActiveRecipe,
                        this::setChanged
                ).withCapacity(carbonTank::getCapacity)
        );
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        int status = statusCode();
        AmmoniaSynthesisRecipe recipe = status == STATUS_READY || status == STATUS_NO_POWER ? currentRecipe() : null;
        int energyDemand = recipe == null ? 0 : energyCostPerTick(recipe);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("ammonia_synthesizer")
                .state(
                        MachineInfoSnapshot.workState(status == STATUS_READY, isActive(), status == STATUS_NO_FLUID),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .progress(progress, activeTicks)
                .energy(energyStorage.getEnergyStored(), energyStorage.getMaxEnergyStored(), -energyDemand)
                .energyTelemetry(
                        energyFlow.lastInput(),
                        energyFlow.lastOutput(),
                        connector.transferRate(),
                        AdjacentEnergyConnector.inputBottleneck(connector, energyDemand)
                )
                .output(status == STATUS_OUTPUT_FULL
                        ? MachineInfoSnapshot.OutputSummary.OUTPUT_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .refinement(machineTraits())
                .build();
    }

    private String statusKey(int status) {
        String name = switch (status) {
            case STATUS_MISSING_CATALYST -> "no_catalyst";
            case STATUS_NO_RECIPE -> "no_recipe";
            case STATUS_NO_POWER -> "no_power";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_NO_FLUID -> nitrogenTank.isEmpty() ? "no_nitrogen" : "no_hydrogen";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.ammonia_synthesizer.status." + name;
    }

    private boolean isActive() {
        BlockState state = getBlockState();
        return state.hasProperty(BaseMachineBlock.ACTIVE) && state.getValue(BaseMachineBlock.ACTIVE);
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        return side == Direction.UP || side == null ? gearInventory : ureaExtractor;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return gearInventory;
    }

    @Override
    public boolean isGearSlot(int slot) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(machineTraits(), Component.translatable("container.rngtech.ammonia_synthesizer"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AmmoniaSynthesizerMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        MachineTraits.STREAM_CODEC.encode(buffer, machineTraits());
    }

    public void dropInventory(Level level) {
        dropSlot(level, gearInventory, SLOT_CATALYST_BED);
        dropSlot(level, ureaOutput, SLOT_UREA);
        dropRefinementInventory(level);
    }

    public boolean isCatalystBed(ItemStack stack) {
        return stack.getItem() instanceof AmmoniaPartItem part
                && part.partType() == MachinePartType.AMMONIA_CATALYST_BED
                && part.machineType() == MachineType.AMMONIA_SYNTHESIZER;
    }

    public float processingProgress() {
        return activeTicks <= 0 ? 0.0F : Mth.clamp((float) progress / (float) activeTicks, 0.0F, 1.0F);
    }

    /** Urea runs whenever its inputs and output room allow, using Ammonia from the output tank; otherwise Ammonia. */
    private boolean tickRecipe() {
        UreaSynthesisRecipe urea = currentUreaRecipe();
        if (urea != null && canRunUrea(urea)) {
            return runCycle(urea, true, urea.processingTicks(), urea.energy(), () -> {
                outputTank.drain(urea.ammoniaInput().amount(), IFluidHandler.FluidAction.EXECUTE);
                carbonTank.drain(urea.carbonInput().amount(), IFluidHandler.FluidAction.EXECUTE);
                ItemStack made = urea.resultStack();
                ItemStack held = ureaOutput.getStackInSlot(SLOT_UREA);
                ureaOutput.setStackInSlot(SLOT_UREA, held.isEmpty() ? made : held.copyWithCount(held.getCount() + made.getCount()));
            });
        }
        AmmoniaSynthesisRecipe recipe = currentRecipe();
        if (recipe == null || !canRun(recipe)) {
            // Losing the recipe part-way through a job breaks the Bulk Speed chain; a full output only pauses it.
            if (recipe == null && progress > 0 && bulkSpeed.reset()) {
                setChanged();
            }
            progress = 0;
            activeTicks = recipe == null ? 0 : recipe.processingTicks();
            activeEnergy = recipe == null ? 0 : recipe.energy();
            return false;
        }
        return runCycle(recipe, false, recipe.processingTicks(), recipe.energy(), () -> {
            nitrogenTank.drain(recipe.nitrogenInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            hydrogenTank.drain(recipe.hydrogenInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            outputTank.fill(recipe.outputFluid(), IFluidHandler.FluidAction.EXECUTE);
        });
    }

    /** A job may finish at once through Instant Process, paying its whole FE, and each finished job feeds Bulk Speed. */
    private boolean runCycle(Object recipe, boolean urea, int ticks, int energy, Runnable complete) {
        if (urea != ureaCycle) {
            ureaCycle = urea;
            progress = 0;
        }
        if (progress == 0 && bulkSpeed.startRecipe(recipe)) {
            setChanged();
        }
        activeTicks = adjustedTicks(ticks);
        activeEnergy = adjustedEnergy(energy);
        if (progress == 0 && level != null && ProcessingChance.rollInstant(level, effectiveStats())
                && energyStorage.consumeEnergy(activeEnergy, true) >= activeEnergy) {
            energyStorage.consumeEnergy(activeEnergy, false);
            markInstantProcess();
            completeCycle(complete);
            return true;
        }
        int cost = Math.max(1, Mth.ceil(activeEnergy / (double) activeTicks));
        if (energyStorage.consumeEnergy(cost, true) < cost) {
            return false;
        }
        energyStorage.consumeEnergy(cost, false);
        progress++;
        if (progress >= activeTicks) {
            completeCycle(complete);
        }
        return true;
    }

    private void completeCycle(Runnable complete) {
        complete.run();
        progress = 0;
        bulkSpeed.recordProcess(effectiveTraits());
    }

    private UreaSynthesisRecipe currentUreaRecipe() {
        if (level == null) {
            return null;
        }
        ItemStack catalyst = catalystStack();
        FluidStack ammonia = outputTank.getFluid();
        FluidStack carbon = carbonTank.getFluid();
        return ureaRecipes.lookup(0, level).item(catalyst).fluid(ammonia).fluid(carbon)
                .find(() -> AmmoniaRecipes.urea(level, catalyst, ammonia, carbon));
    }

    private boolean canRunUrea(UreaSynthesisRecipe recipe) {
        ItemStack held = ureaOutput.getStackInSlot(SLOT_UREA);
        ItemStack made = recipe.resultStack();
        return catalystStage() >= recipe.minimumCatalystStage()
                && outputTank.getFluidAmount() >= recipe.ammoniaInput().amount()
                && carbonTank.getFluidAmount() >= recipe.carbonInput().amount()
                && (held.isEmpty() || ItemStack.isSameItemSameComponents(held, made)
                        && held.getCount() + made.getCount() <= held.getMaxStackSize());
    }

    private AmmoniaSynthesisRecipe currentRecipe() {
        if (level == null) {
            return null;
        }
        ItemStack catalyst = catalystStack();
        FluidStack nitrogen = nitrogenTank.getFluid();
        FluidStack hydrogen = hydrogenTank.getFluid();
        return recipes.lookup(0, level).item(catalyst).fluid(nitrogen).fluid(hydrogen)
                .find(() -> AmmoniaRecipes.synthesis(level, catalyst, nitrogen, hydrogen));
    }

    /** How many recipe lookups searched the recipes instead of reusing a remembered result. */
    public int recipeSearches() {
        return recipes.searches();
    }

    private boolean canRun(AmmoniaSynthesisRecipe recipe) {
        return catalystStage() >= recipe.minimumCatalystStage()
                && nitrogenTank.getFluidAmount() >= recipe.nitrogenInput().amount()
                && hydrogenTank.getFluidAmount() >= recipe.hydrogenInput().amount()
                && outputTank.fill(recipe.outputFluid(), IFluidHandler.FluidAction.SIMULATE) >= recipe.output().getAmount();
    }

    private void clearActiveRecipe() {
        progress = 0;
        activeTicks = 0;
        activeEnergy = 0;
    }

    private int statusCode() {
        if (!isCatalystBed(catalystStack())) {
            return STATUS_MISSING_CATALYST;
        }
        UreaSynthesisRecipe urea = currentUreaRecipe();
        if (urea != null && canRunUrea(urea)) {
            int cost = Math.max(1, Mth.ceil(adjustedEnergy(urea.energy()) / (double) adjustedTicks(urea.processingTicks())));
            return energyStorage.getEnergyStored() < cost ? STATUS_NO_POWER : STATUS_READY;
        }
        if (nitrogenTank.isEmpty() || hydrogenTank.isEmpty()) {
            return STATUS_NO_FLUID;
        }
        AmmoniaSynthesisRecipe recipe = currentRecipe();
        if (recipe == null || catalystStage() < recipe.minimumCatalystStage()) {
            return STATUS_NO_RECIPE;
        }
        if (outputTank.fill(recipe.outputFluid(), IFluidHandler.FluidAction.SIMULATE) < recipe.output().getAmount()) {
            return STATUS_OUTPUT_FULL;
        }
        return energyStorage.getEnergyStored() < energyCostPerTick(recipe) ? STATUS_NO_POWER : STATUS_READY;
    }

    private int energyCostPerTick(AmmoniaSynthesisRecipe recipe) {
        return Math.max(1, Mth.ceil(adjustedEnergy(recipe) / (double) adjustedTicks(recipe)));
    }

    private ItemStack catalystStack() {
        return gearInventory.getStackInSlot(SLOT_CATALYST_BED);
    }

    private int catalystStage() {
        return catalystStack().getItem() instanceof AmmoniaPartItem part ? part.stage() : 0;
    }

    private int adjustedTicks(AmmoniaSynthesisRecipe recipe) {
        return adjustedTicks(recipe.processingTicks());
    }

    private int adjustedEnergy(AmmoniaSynthesisRecipe recipe) {
        return adjustedEnergy(recipe.energy());
    }

    private int adjustedTicks(int ticks) {
        return Math.max(1, Mth.ceil(ticks / effectiveStats().value(MachineStat.PROCESSING_SPEED)));
    }

    private int adjustedEnergy(int energy) {
        return Math.max(1, Mth.ceil(energy * effectiveStats().value(MachineStat.ENERGY_USAGE)));
    }

    private int internalEnergyCapacity() {
        return Math.max(1, Mth.floor(effectiveStats().value(MachineStat.ENERGY_CAPACITY)));
    }

    public MachineStatAccumulator effectiveStats() {
        return cachedStats(bulkSpeed.count());
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.ammoniaSynthesizer();
        machineTraits().activeModifiers().forEach(stats::apply);
        if (isCatalystBed(catalystStack())) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, catalystStack());
        }
        bulkSpeed.apply(stats, effectiveTraits());
        return stats;
    }

    private static int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return Mth.floor(stats.value(stat) * STAT_SCALE);
    }

    /** Fluid Capacity sizes all four tanks; fluid above a smaller capacity stays until it drains. */
    private void updateTankCapacity(MachineStatAccumulator stats) {
        int capacity = Math.max(TANK_CAPACITY, (int) Math.round(stats.value(MachineStat.FLUID_CAPACITY)));
        if (nitrogenTank.getCapacity() != capacity) {
            nitrogenTank.setCapacity(capacity);
            hydrogenTank.setCapacity(capacity);
            outputTank.setCapacity(capacity);
            carbonTank.setCapacity(capacity);
        }
    }

    private FluidTank tank(net.minecraft.world.level.material.Fluid acceptedFluid) {
        return new FluidTank(TANK_CAPACITY) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.is(acceptedFluid);
            }

            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    private void repairMisroutedInputFluids() {
        FluidStack nitrogen = nitrogenTank.getFluid();
        FluidStack hydrogen = hydrogenTank.getFluid();
        if (nitrogen.is(ModFluids.HYDROGEN_SOURCE.get()) && hydrogen.is(ModFluids.NITROGEN_SOURCE.get())) {
            nitrogenTank.setFluid(hydrogen.copy());
            hydrogenTank.setFluid(nitrogen.copy());
            return;
        }
        moveMisroutedFluid(nitrogenTank, hydrogenTank, ModFluids.HYDROGEN_SOURCE.get());
        moveMisroutedFluid(hydrogenTank, nitrogenTank, ModFluids.NITROGEN_SOURCE.get());
    }

    private void moveMisroutedFluid(FluidTank source, FluidTank target, net.minecraft.world.level.material.Fluid expectedFluid) {
        FluidStack sourceFluid = source.getFluid();
        if (!sourceFluid.is(expectedFluid)) {
            return;
        }
        int moved = target.fill(sourceFluid.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (moved <= 0) {
            return;
        }
        sourceFluid.shrink(moved);
        source.setFluid(sourceFluid.isEmpty() ? FluidStack.EMPTY : sourceFluid.copy());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.put("NitrogenTank", nitrogenTank.writeToNBT(registries, new CompoundTag()));
        tag.put("HydrogenTank", hydrogenTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("CarbonTank", carbonTank.writeToNBT(registries, new CompoundTag()));
        tag.put("UreaOutput", ureaOutput.serializeNBT(registries));
        tag.putBoolean("UreaCycle", ureaCycle);
        tag.putInt("Progress", progress);
        tag.putInt("ActiveTicks", activeTicks);
        tag.putInt("ActiveEnergy", activeEnergy);
        bulkSpeed.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        energyStorage.setEnergy(tag.getInt("Energy"));
        nitrogenTank.readFromNBT(registries, tag.getCompound("NitrogenTank"));
        hydrogenTank.readFromNBT(registries, tag.getCompound("HydrogenTank"));
        outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        carbonTank.setFluid(FluidStack.EMPTY);
        carbonTank.readFromNBT(registries, tag.getCompound("CarbonTank"));
        if (tag.contains("UreaOutput")) {
            ureaOutput.deserializeNBT(registries, tag.getCompound("UreaOutput"));
        }
        ureaCycle = tag.getBoolean("UreaCycle");
        progress = tag.getInt("Progress");
        activeTicks = tag.getInt("ActiveTicks");
        activeEnergy = tag.getInt("ActiveEnergy");
        bulkSpeed.load(tag);
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private final class SynthesisFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 4;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> nitrogenTank.getFluid();
                case 1 -> hydrogenTank.getFluid();
                case 2 -> outputTank.getFluid();
                case 3 -> carbonTank.getFluid();
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return nitrogenTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return switch (tank) {
                case 0 -> nitrogenTank.isFluidValid(stack);
                case 1 -> hydrogenTank.isFluidValid(stack);
                case 2 -> outputTank.isFluidValid(stack);
                case 3 -> carbonTank.isFluidValid(stack);
                default -> false;
            };
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            if (nitrogenTank.isFluidValid(resource)) {
                return nitrogenTank.fill(resource, action);
            }
            if (hydrogenTank.isFluidValid(resource)) {
                return hydrogenTank.fill(resource, action);
            }
            if (carbonTank.isFluidValid(resource)) {
                return carbonTank.fill(resource, action);
            }
            // Ammonia piped in feeds the Urea recipe on a synthesizer without its own Nitrogen and Hydrogen.
            if (outputTank.isFluidValid(resource)) {
                return outputTank.fill(resource, action);
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return outputTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return outputTank.drain(maxDrain, action);
        }
    }

    /** Sides other than the top only take Urea out. */
    private final class UreaExtractHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ureaOutput.getStackInSlot(SLOT_UREA);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ureaOutput.extractItem(SLOT_UREA, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

}
