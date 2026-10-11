package com.rngtech.content.blockentity;

import com.rngtech.content.block.AlloyFurnaceBlock;
import com.rngtech.content.block.AmmoniaFuelCellBlock;
import com.rngtech.content.block.AmmoniaSynthesizerBlock;
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
import com.rngtech.content.block.PotentialReactorBlock;
import com.rngtech.content.block.ResonanceCalibratorBlock;
import com.rngtech.content.block.SolarArrayControllerBlock;
import com.rngtech.content.block.SolarPanelBlock;
import com.rngtech.content.block.SolidFuelBurnerBlock;
import com.rngtech.content.item.RefinementConsumableItem;
import com.rngtech.content.loot.ChallengeContext;
import com.rngtech.content.loot.ChallengeLoot;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

public abstract class BaseMachineBlockEntity extends BlockEntity implements RefinableMachine {
    private static final long REMOVED_GEAR_CHECK_INTERVAL = 20L;
    private static final double REMOVED_GEAR_PLAYER_RANGE = 16.0D;

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
    private final Supplier<MachineStatAccumulator> statsBuilder = this::buildStats;

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

    @Override
    public MachineType refinementMachineType() {
        return machineType;
    }

    @Override
    public MachineTraits machineTraits() {
        MachineTraits traits = components().get(ModDataComponents.MACHINE_TRAITS.get());
        return traits == null ? MachineTraits.EMPTY : traits;
    }

    @Override
    public void setMachineTraits(MachineTraits traits) {
        setComponents(DataComponentMap.builder()
                .addAll(components())
                .set(ModDataComponents.MACHINE_TRAITS.get(), traits)
                .build()
        );
        setChanged();
    }

    /** Builds this machine's stats from scratch. Machines that cache stats through {@link #cachedStats} override it. */
    protected MachineStatAccumulator buildStats() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not build cached stats");
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

    /** Whether allocated Mastery grants {@code behavior}, cached until the progression changes. */
    protected boolean hasMasteryBehavior(String behavior) {
        MachineProgressionState state = machineProgression();
        if (state != behaviorState) {
            behaviorState = state;
            behaviors = MegaPassiveTree.behaviors(state);
        }
        return behaviors.contains(behavior);
    }

    public void setMachineProgression(MachineProgressionState progression) {
        setComponents(DataComponentMap.builder()
                .addAll(components())
                .set(ModDataComponents.MACHINE_PROGRESSION.get(), progression)
                .build()
        );
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

    @Override
    public ItemStackHandler getRefinementInventory() {
        return refinementInventory;
    }

    /** Rolls the Mastery level challenge loot on top of the machine; this satisfies {@link MachineMasteryHost}. */
    public void masteryLevelGained(MachineProgressionState state) {
        if (this instanceof MachineMasteryHost host && level != null && !level.isClientSide) {
            ChallengeLoot.drop(level, worldPosition, ChallengeLoot.MASTERY_LEVEL, ChallengeContext.of(host).withMasteryLevel(state.level()));
        }
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
