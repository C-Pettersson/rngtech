package com.rngtech.content.blockentity;

import com.rngtech.content.block.AlloyFurnaceBlock;
import com.rngtech.content.block.AmmoniaFuelCellBlock;
import com.rngtech.content.block.AmmoniaSynthesizerBlock;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.BatteryAssemblerBlock;
import com.rngtech.content.block.BatteryChassisBlock;
import com.rngtech.content.block.BioGeneratorBlock;
import com.rngtech.content.block.ComponentRecyclerBlock;
import com.rngtech.content.block.CompressorTankBlock;
import com.rngtech.content.block.CorrosionCellBlock;
import com.rngtech.content.block.CrusherBlock;
import com.rngtech.content.block.FurnaceBlock;
import com.rngtech.content.block.GasChemistryBlock;
import com.rngtech.content.block.MelterBlock;
import com.rngtech.content.block.MetalPressBlock;
import com.rngtech.content.block.PhaseCoilBlock;
import com.rngtech.content.block.PhaseEmitterBlock;
import com.rngtech.content.block.PotentialReactorBlock;
import com.rngtech.content.block.ResonanceCalibratorBlock;
import com.rngtech.content.block.SolarArrayControllerBlock;
import com.rngtech.content.block.SolarPanelBlock;
import com.rngtech.content.block.SolidFuelBurnerBlock;
import com.rngtech.content.block.WorkOrderMachineBlock;
import com.rngtech.content.item.RefinementConsumableItem;
import com.rngtech.content.jewel.JewelSockets;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.PhaseArrayRules;
import com.rngtech.rpg.TelemetryReadout;
import com.rngtech.rpg.jewel.JewelEffects;
import com.rngtech.rpg.jewel.JewelHost;
import com.rngtech.rpg.jewel.MachineJewels;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.workorder.LoadedWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkOrderHost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Supplier;

public abstract class BaseMachineBlockEntity extends BlockEntity implements RefinableMachine, JewelHost {
    private static final long REMOVED_GEAR_CHECK_INTERVAL = 20L;
    private static final double REMOVED_GEAR_PLAYER_RANGE = 16.0D;
    private static final int BODY_CUE_INTERVAL = 20;
    private static final double GLOW_ON_FILL = 0.10D;
    private static final double GLOW_OFF_FILL = 0.05D;
    private static final long GLOW_HOLD_TICKS = 100L;
    private static final String TELEMETRY_GROUP = "comparator_readout";
    private static final String NOISE_FACTOR_TAG = "NoiseFactor";

    private final IItemHandler topItemHandler;
    private final IItemHandler sideItemHandler;
    private final IItemHandler bottomItemHandler;
    private final MachineType machineType;
    private MachineProgressionState behaviorState;
    private Set<String> behaviors = Set.of();
    private final ItemStackHandler refinementInventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.getItem() instanceof RefinementConsumableItem;
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
    private final List<ItemStack> removedGear = new ArrayList<>();
    private final MachineStatsCache statsCache = new MachineStatsCache();
    private final Supplier<MachineStatAccumulator> statsBuilder = this::buildStatsWithBodyJewels;
    private MachineTraits effectiveTraits;
    private MachineTraits effectiveTraitsStored;
    private Block effectiveTraitsBlock;
    private long glowChangedAt = -GLOW_HOLD_TICKS;
    private int lastTelemetrySignal = -1;
    private float syncedNoiseFactor = 1.0F;

    protected BaseMachineBlockEntity(
            BlockEntityType<?> blockEntityType,
            BlockPos pos,
            BlockState blockState,
            MachineType machineType,
            int inputSlot,
            int fuelSlot,
            int outputSlot
    ) {
        super(blockEntityType, pos, blockState);
        this.machineType = machineType;
        topItemHandler = new SidedItemHandler(inputSlot, true, false);
        sideItemHandler = new SidedItemHandler(fuelSlot, true, false);
        bottomItemHandler = new SidedItemHandler(outputSlot, false, true);
    }

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

    protected abstract ItemStackHandler getMachineInventory();

    /** Whether {@code slot} of the inventory {@link #getItemHandler} returns for no side holds installed Gear. */
    public boolean isGearSlot(int slot) {
        return false;
    }

    @Override
    public MachineType refinementMachineType() {
        return machineType;
    }

    @Override
    public MachineTraits machineTraits() {
        MachineTraits traits = components().get(ModDataComponents.MACHINE_TRAITS.get());
        return traits == null ? MachineTraits.EMPTY : traits;
    }

    /** Placing a Mirrored machine item keeps its component, and the block loot table copies it back to the drop. */
    @Override
    public boolean isMirrored() {
        return components().has(ModDataComponents.MIRRORED.get());
    }

    @Override
    public void setMachineTraits(MachineTraits traits) {
        setComponents(DataComponentMap.builder()
                .addAll(components())
                .set(ModDataComponents.MACHINE_TRAITS.get(), traits)
                .build()
        );
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * The stored traits merged with this block's identity traits, rebuilt only when the stored traits component or the
     * block changes.
     */
    public final MachineTraits effectiveTraits() {
        MachineTraits stored = machineTraits();
        Block block = getBlockState().getBlock();
        if (effectiveTraits == null || stored != effectiveTraitsStored || block != effectiveTraitsBlock) {
            effectiveTraits = MachineImplicitCatalog.effectiveTraits(stored, block);
            effectiveTraitsStored = stored;
            effectiveTraitsBlock = block;
        }
        return effectiveTraits;
    }

    /** Builds this machine's stats from scratch. Machines that cache stats through {@link #cachedStats} override it. */
    protected MachineStatAccumulator buildStats() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not build cached stats");
    }

    /**
     * {@link #buildStats} plus the body sockets' jewels on a machine without Mastery. Mastery machines read their jewels
     * inside {@code buildStats}, with the tree's nodes, so their attribute lines land before the conversions.
     */
    private MachineStatAccumulator buildStatsWithBodyJewels() {
        MachineStatAccumulator stats = buildStats();
        if (!(this instanceof MachineMasteryHost) && bodySocketCount() > 0) {
            JewelEffects.applyLines(stats, this, MachineProgressionState.EMPTY, null);
        }
        return stats;
    }

    /** Body sockets from the body's own rolls and identity: the Bedazzling suffix plus a Gold body's setting. */
    @Override
    public int bodySocketCount() {
        return JewelSockets.bodySockets(effectiveTraits());
    }

    /**
     * {@link #buildStats}, rebuilt only when the traits, Mastery, a tracked stack, {@code localKey}, or the config or
     * tags change. The result is read-only; add conditional effects to {@link MachineStatAccumulator#mutable()}.
     */
    protected final MachineStatAccumulator cachedStats(long localKey) {
        return statsCache.get(
                components().get(ModDataComponents.MACHINE_TRAITS.get()),
                components().get(ModDataComponents.MACHINE_PROGRESSION.get()),
                localKey,
                activeWorkOrderConditions(),
                statsBuilder
        );
    }

    /** Rebuilds the stats when one of {@code slots} changes; no slots means the whole inventory. */
    protected final void trackStatSlots(ItemStackHandler inventory, int... slots) {
        statsCache.track(inventory, slots.length == 0 ? null : slots.clone());
    }

    /** How many times this machine built its cached stats, for checks that it does not rebuild them every tick. */
    public final int statsBuilds() {
        return statsCache.builds();
    }

    public MachineProgressionState machineProgression() {
        MachineProgressionState progression = components().get(ModDataComponents.MACHINE_PROGRESSION.get());
        return progression == null ? MachineProgressionState.EMPTY : progression;
    }

    /** Synced in place of a job's progress while the Process tab flashes a skipped job. */
    public static final int INSTANT_FLASH_PROGRESS = -1;
    /** How long the Process tab's progress bar flashes after an Instant Process. */
    private static final int INSTANT_FLASH_TICKS = 10;
    private final long[] instantFlashUntil = new long[4];

    /** Records an Instant Process so the Process tab can flash the bar of {@code lane}. */
    protected final void markInstantProcess(int lane) {
        if (level != null && lane >= 0 && lane < instantFlashUntil.length) {
            instantFlashUntil[lane] = level.getGameTime() + INSTANT_FLASH_TICKS;
        }
    }

    protected final void markInstantProcess() {
        markInstantProcess(0);
    }

    /** {@code progress}, or the flash marker while {@code lane} just finished a job instantly. */
    protected final int syncedProgress(int lane, int progress) {
        return level != null && lane >= 0 && lane < instantFlashUntil.length && level.getGameTime() < instantFlashUntil[lane]
                ? INSTANT_FLASH_PROGRESS : progress;
    }

    /** Whether allocated Mastery grants {@code behavior}, cached until the progression changes. */
    protected boolean hasMasteryBehavior(String behavior) {
        MachineProgressionState state = machineProgression();
        if (state != behaviorState) {
            behaviorState = state;
            behaviors = MegaPassiveTree.behaviors(state);
        }
        return behaviors.contains(behavior);
    }

    /** The Work Orders loaded into this machine, stored as a component so they drop with it. */
    public MachineWorkOrders workOrders() {
        MachineWorkOrders orders = components().get(ModDataComponents.WORK_ORDERS.get());
        return orders == null ? MachineWorkOrders.EMPTY : orders;
    }

    /** The active Job Chip's traits while it has conditions to apply, else null; a stats cache key. */
    private Object activeWorkOrderConditions() {
        LoadedWorkOrder active = workOrders().activeOrder();
        return active == null || active.fulfilled() || active.traits().modifiers().isEmpty() ? null : active.traits();
    }

    /** The world's game time for Rush deadlines; 0 before the machine is in a world. */
    public long workOrderGameTime() {
        return level == null ? 0 : level.getGameTime();
    }

    /**
     * Drops expired Rush chips about once a second, and lights the Job Chip lamp to match, which also catches a machine
     * placed from an item that carries an active chip.
     */
    protected void tickWorkOrders(Level level) {
        if (this instanceof WorkOrderHost host && level.getGameTime() % 20 == 0) {
            host.workOrderTick();
            syncWorkOrderLamp();
        }
    }

    public void setWorkOrders(MachineWorkOrders orders) {
        DataComponentMap.Builder builder = DataComponentMap.builder().addAll(components());
        builder.set(ModDataComponents.WORK_ORDERS.get(), orders == null || orders.isEmpty() ? null : orders);
        setComponents(builder.build());
        setChanged();
        syncWorkOrderLamp();
    }

    /** Lights the model's Job Chip lamp while a chip is active, on machines that have one. */
    private void syncWorkOrderLamp() {
        BlockState state = getBlockState();
        if (level == null || level.isClientSide || !state.hasProperty(WorkOrderMachineBlock.WORK_ORDER)) {
            return;
        }
        boolean lit = workOrders().activeOrder() != null;
        if (state.getValue(WorkOrderMachineBlock.WORK_ORDER) != lit) {
            level.setBlock(worldPosition, state.setValue(WorkOrderMachineBlock.WORK_ORDER, lit), Block.UPDATE_CLIENTS);
        }
    }

    public void setMachineProgression(MachineProgressionState progression) {
        setComponents(DataComponentMap.builder()
                .addAll(components())
                .set(ModDataComponents.MACHINE_PROGRESSION.get(), progression)
                .build()
        );
        setChanged();
    }

    /** The jewels in this machine's tree and body sockets, stored as a component so they drop with it. */
    @Override
    public MachineJewels jewels() {
        MachineJewels jewels = components().get(ModDataComponents.JEWELS.get());
        return jewels == null ? MachineJewels.EMPTY : jewels;
    }

    @Override
    public void setJewels(MachineJewels jewels) {
        DataComponentMap.Builder builder = DataComponentMap.builder().addAll(components());
        builder.set(ModDataComponents.JEWELS.get(), jewels == null || jewels.isEmpty() ? null : jewels);
        setComponents(builder.build());
        // The stats cache keys on traits, progression, and orders; jewels change beside them, so bump it by hand.
        statsCache.invalidate();
        setChanged();
    }

    @Override
    public int refinementComponentStage() {
        Block block = getBlockState().getBlock();
        if (block instanceof CrusherBlock crusher) {
            return crusher.material().stage();
        }
        if (block instanceof FurnaceBlock furnace) {
            return furnace.material().stage();
        }
        if (block instanceof AlloyFurnaceBlock furnace) {
            return furnace.material().stage();
        }
        if (block instanceof BatteryChassisBlock chassis) {
            return chassis.material().stage();
        }
        if (block instanceof SolidFuelBurnerBlock burner) {
            return burner.chassis().stage();
        }
        if (block instanceof BioGeneratorBlock) {
            return 2;
        }
        if (block instanceof SolarPanelBlock panel) {
            return panel.material().stage();
        }
        if (block instanceof SolarArrayControllerBlock) {
            return 4;
        }
        if (block instanceof PotentialReactorBlock) {
            return 3;
        }
        if (block instanceof CorrosionCellBlock) {
            return 4;
        }
        if (block instanceof ComponentRecyclerBlock recycler) {
            return recycler.chassis().stage();
        }
        if (block instanceof CompressorTankBlock tank) {
            return tank.material().stage();
        }
        if (block instanceof MetalPressBlock press) {
            return press.componentStage();
        }
        if (block instanceof ResonanceCalibratorBlock calibrator) {
            return calibrator.chassis().stage();
        }
        if (block instanceof MelterBlock) {
            return 6;
        }
        if (block instanceof BatteryAssemblerBlock) {
            return 2;
        }
        if (block instanceof GasChemistryBlock gasChemistry) {
            return gasChemistry.machine().stage();
        }
        if (block instanceof AmmoniaSynthesizerBlock || block instanceof AmmoniaFuelCellBlock) {
            return 6;
        }
        if (block instanceof PhaseEmitterBlock || block instanceof PhaseCoilBlock) {
            return PhaseArrayRules.PART_STAGE;
        }
        return 0;
    }

    @Override
    public int refinementModifierRollComponentStage() {
        Block block = getBlockState().getBlock();
        if (block instanceof MetalPressBlock press) {
            return press.modifierRollComponentStage();
        }
        return refinementComponentStage();
    }

    public boolean mutesMachineSound() {
        return false;
    }

    /** How loud this machine's sound and ambient particles are: 1, or less on a Discreet body. Clients read the synced value. */
    public final float noiseFactor() {
        return level != null && level.isClientSide ? syncedNoiseFactor : bodyNoiseFactor();
    }

    /** Whether a Discreet machine skips this ambient particle, so its particles thin out with its sound. */
    public static boolean skipsParticle(Level level, BlockPos pos, RandomSource random) {
        return level.getBlockEntity(pos) instanceof BaseMachineBlockEntity machine && random.nextFloat() >= machine.noiseFactor();
    }

    /** Adds the Discreet noise factor to an update tag, for hosts that sync only their visual state. */
    protected final CompoundTag withNoiseFactor(CompoundTag tag) {
        float factor = bodyNoiseFactor();
        if (factor < 1.0F) {
            tag.putFloat(NOISE_FACTOR_TAG, factor);
        }
        return tag;
    }

    protected final void readNoiseFactor(CompoundTag tag) {
        syncedNoiseFactor = tag.contains(NOISE_FACTOR_TAG) ? tag.getFloat(NOISE_FACTOR_TAG) : 1.0F;
    }

    private float bodyNoiseFactor() {
        return (float) Mth.clamp(1.0D - bodyEffect(MachineStat.MACHINE_NOISE, ModifierOperation.DECREASED_PERCENT) / 100.0D, 0.0D, 1.0D);
    }

    /**
     * Every second, lights or darkens a Glow body and tells comparators when a Telemetry gauge moved. The light comes on
     * at 10% fill and goes off below 5%, and holds each change for 5 seconds, so it never flashes between recipes.
     */
    public final void tickBodyCues(Level level, BlockPos pos) {
        if (Math.floorMod(level.getGameTime() + pos.hashCode(), BODY_CUE_INTERVAL) != 0) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BaseMachineBlock.GLOW)) {
            updateGlow(level, pos, state);
        }
        int signal = telemetrySignal();
        if (signal != lastTelemetrySignal) {
            lastTelemetrySignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    private void updateGlow(Level level, BlockPos pos, BlockState state) {
        int target = glowIndex(bodyEffect(MachineStat.LIGHT_LEVEL, ModifierOperation.ADD));
        int current = state.getValue(BaseMachineBlock.GLOW);
        if (target == 0 && current == 0) {
            return;
        }
        boolean lit = current > 0;
        boolean wantLit = target > 0 && glowFill() >= (lit ? GLOW_OFF_FILL : GLOW_ON_FILL);
        int next = wantLit ? target : 0;
        if (next == current) {
            return;
        }
        long time = level.getGameTime();
        if (target > 0 && lit != wantLit) {
            if (time - glowChangedAt < GLOW_HOLD_TICKS) {
                return;
            }
            glowChangedAt = time;
        }
        level.setBlock(pos, state.setValue(BaseMachineBlock.GLOW, next), Block.UPDATE_CLIENTS);
    }

    private static int glowIndex(double lightLevel) {
        if (lightLevel <= 0.0D) {
            return 0;
        }
        int index = 1;
        for (int candidate = 2; candidate < BaseMachineBlock.GLOW_LIGHT_LEVELS.length; candidate++) {
            if (lightLevel >= BaseMachineBlock.GLOW_LIGHT_LEVELS[candidate]) {
                index = candidate;
            }
        }
        return index;
    }

    /** How full the FE buffer is, or for a machine without one, how hot it runs against its Max Temperature. */
    private double glowFill() {
        double energy = energyFill();
        return energy >= 0.0D ? energy : heatFraction();
    }

    private double energyFill() {
        if (this instanceof MachineInfoProvider provider) {
            MachineInfoSnapshot info = provider.machineInfo();
            if (info.energyCapacity() > 0L) {
                return Math.max(0L, info.energy()) / (double) info.energyCapacity();
            }
        }
        return -1.0D;
    }

    /** The machine's temperature against its Max Temperature, or -1 for a machine without heat. */
    protected double heatFraction() {
        return -1.0D;
    }

    /** The comparator signal of a Telemetry body's gauge, or -1 when the body has no Telemetry. */
    public int telemetrySignal() {
        TelemetryReadout readout = telemetryReadout();
        if (readout == null) {
            return -1;
        }
        double fill = switch (readout) {
            case ENERGY -> energyFill();
            case PROGRESS -> progressFill();
            case OUTPUT -> {
                IItemHandler output = getItemHandler(Direction.DOWN);
                yield output == null ? 0.0D : ItemHandlerHelper.calcRedstoneFromInventory(output) / 15.0D;
            }
            case HEAT -> heatFraction();
        };
        return fillSignal(fill);
    }

    private double progressFill() {
        if (this instanceof MachineInfoProvider provider) {
            MachineInfoSnapshot info = provider.machineInfo();
            if (info.progressMax() > 0 && info.progress() > 0) {
                return info.progress() / (double) info.progressMax();
            }
        }
        return 0.0D;
    }

    /** Scales a fill to a comparator signal the way vanilla containers do: empty is 0, any fill at least 1, full 15. */
    public static int fillSignal(double fill) {
        if (!(fill > 0.0D)) {
            return 0;
        }
        return Math.min(15, 1 + (int) Math.floor(Math.min(1.0D, fill) * 14.0D));
    }

    private TelemetryReadout telemetryReadout() {
        for (MachineModifier modifier : effectiveTraits().activeModifiers()) {
            if (TELEMETRY_GROUP.equals(modifier.modGroup())) {
                return TelemetryReadout.byAffixId(modifier.affixId());
            }
        }
        return null;
    }

    /** The summed value of {@code stat} with {@code operation} across the machine body's own rolls. */
    private double bodyEffect(MachineStat stat, ModifierOperation operation) {
        double total = 0.0D;
        for (MachineModifier modifier : effectiveTraits().activeModifiers()) {
            for (MachineModifierEffect effect : modifier.effects()) {
                if (effect.stat() == stat && effect.operation() == operation) {
                    total += effect.value();
                }
            }
        }
        return total;
    }

    @Override
    public ItemStackHandler getRefinementInventory() {
        return refinementInventory;
    }

    public void dropRefinementInventory(Level level) {
        ItemStack stack = refinementInventory.getStackInSlot(0);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            refinementInventory.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    /**
     * Loads a Gear inventory that may have been saved with one more slot, at {@code removedSlot}. Later slots shift
     * down by one, and items with no slot left are held until {@link #releaseRemovedGear} or {@link #dropRemovedGear}.
     */
    protected void loadGearWithoutSlot(
            ItemStackHandler gearInventory,
            CompoundTag gearTag,
            HolderLookup.Provider registries,
            int removedSlot
    ) {
        int slotCount = gearInventory.getSlots();
        int savedSize = Math.max(0, gearTag.getInt("Size"));
        if (savedSize == slotCount) {
            gearInventory.deserializeNBT(registries, gearTag);
            return;
        }

        boolean hasRemovedSlot = savedSize == slotCount + 1;
        ItemStackHandler saved = new ItemStackHandler(savedSize);
        saved.deserializeNBT(registries, gearTag);
        ItemStackHandler current = new ItemStackHandler(slotCount);
        for (int savedSlot = 0; savedSlot < savedSize; savedSlot++) {
            ItemStack stack = saved.getStackInSlot(savedSlot);
            int slot = hasRemovedSlot && savedSlot > removedSlot ? savedSlot - 1 : savedSlot;
            if ((hasRemovedSlot && savedSlot == removedSlot) || slot >= slotCount) {
                if (!stack.isEmpty()) {
                    removedGear.add(stack);
                }
            } else {
                current.setStackInSlot(slot, stack);
            }
        }
        gearInventory.deserializeNBT(registries, current.serializeNBT(registries));
    }

    /** Holds Gear that lost its slot on load until {@link #releaseRemovedGear} moves it out. */
    protected void holdRemovedGear(ItemStack stack) {
        if (!stack.isEmpty()) {
            removedGear.add(stack);
        }
    }

    /**
     * Moves held Gear from {@link #loadGearWithoutSlot} into an adjacent item handler, then drops the rest once a player
     * is close enough to collect it before it despawns. Until then it stays saved on the machine.
     */
    protected void releaseRemovedGear(Level level) {
        if (removedGear.isEmpty() || level.getGameTime() % REMOVED_GEAR_CHECK_INTERVAL != 0L) {
            return;
        }
        boolean inserted = insertRemovedGearIntoNeighbors(level);
        if (!removedGear.isEmpty() && level.hasNearbyAlivePlayer(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D,
                REMOVED_GEAR_PLAYER_RANGE
        )) {
            dropRemovedGear(level);
        } else if (inserted) {
            setChanged();
        }
    }

    private boolean insertRemovedGearIntoNeighbors(Level level) {
        boolean inserted = false;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(direction);
            // Machine inputs could consume the Gear, for example a Component Recycler.
            if (level.getBlockEntity(neighbor) instanceof BaseMachineBlockEntity) {
                continue;
            }
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, direction.getOpposite());
            if (handler == null) {
                continue;
            }
            ListIterator<ItemStack> stacks = removedGear.listIterator();
            while (stacks.hasNext()) {
                ItemStack stack = stacks.next();
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
                if (remainder.getCount() == stack.getCount()) {
                    continue;
                }
                inserted = true;
                if (remainder.isEmpty()) {
                    stacks.remove();
                } else {
                    stacks.set(remainder);
                }
            }
            if (removedGear.isEmpty()) {
                break;
            }
        }
        return inserted;
    }

    protected void dropRemovedGear(Level level) {
        if (removedGear.isEmpty()) {
            return;
        }
        for (ItemStack stack : removedGear) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        }
        removedGear.clear();
        setChanged();
    }

    /** Moves an item out of the machine: into the inventory below, like a hopper or chest, or onto the ground. */
    protected void ejectBelowOrDrop(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return;
        }
        IItemHandler below = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.below(), Direction.UP);
        ItemStack rest = below == null ? stack : ItemHandlerHelper.insertItemStacked(below, stack, false);
        if (!rest.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), rest);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("RefinementInventory", refinementInventory.serializeNBT(registries));
        if (!removedGear.isEmpty()) {
            ListTag removed = new ListTag();
            removedGear.forEach(stack -> removed.add(stack.save(registries)));
            tag.put("RemovedGear", removed);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        statsCache.invalidate();
        readNoiseFactor(tag);
        refinementInventory.deserializeNBT(registries, tag.getCompound("RefinementInventory"));
        removedGear.clear();
        ListTag removed = tag.getList("RemovedGear", Tag.TAG_COMPOUND);
        for (int index = 0; index < removed.size(); index++) {
            ItemStack stack = ItemStack.parseOptional(registries, removed.getCompound(index));
            if (!stack.isEmpty()) {
                removedGear.add(stack);
            }
        }
    }

    private final class SidedItemHandler implements IItemHandler {
        private final int slot;
        private final boolean allowInsert;
        private final boolean allowExtract;

        private SidedItemHandler(int slot, boolean allowInsert, boolean allowExtract) {
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
            return getMachineInventory().getStackInSlot(mappedSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!allowInsert) {
                return stack;
            }
            return getMachineInventory().insertItem(mappedSlot(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!allowExtract) {
                return ItemStack.EMPTY;
            }
            return getMachineInventory().extractItem(mappedSlot(slot), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return getMachineInventory().getSlotLimit(mappedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return allowInsert && getMachineInventory().isItemValid(mappedSlot(slot), stack);
        }

        private int mappedSlot(int slot) {
            if (slot != 0) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
            }
            return this.slot;
        }
    }
}
