package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.BatteryChassisBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.menu.BatteryChassisMenu;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineModifierEffect;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierEligibilityProfiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class BatteryChassisBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider {
    private static final int DATA_ENERGY = 0;
    private static final int DATA_ENERGY_CAPACITY = 1;
    private static final int DATA_MAX_INPUT = 2;
    private static final int DATA_MAX_OUTPUT = 3;
    private static final int DATA_BURST_OUTPUT = 4;
    private static final int DATA_CELL_SLOTS = 5;
    private static final int DATA_ENERGY_TRANSFER = 6;
    private static final int DATA_BURST_TRANSFER = 7;
    private static final int DATA_BURST_DURATION = 8;
    private static final int DATA_EFFICIENCY = 9;
    private static final int DATA_STABILITY = 10;
    private static final int DATA_IDLE_LOSS = 11;
    private static final int DATA_GLOBAL_MODIFIER_STRENGTH = 12;
    private static final int DATA_REFINEMENT_POTENTIAL = 13;
    private static final int DATA_CELL_INPUT_SUM = 14;
    private static final int DATA_CELL_OUTPUT_SUM = 15;
    private static final int DATA_CONNECTOR_INPUT_CAP = 16;
    private static final int DATA_CONNECTOR_OUTPUT_CAP = 17;
    private static final int DATA_LAST_INPUT = 18;
    private static final int DATA_LAST_OUTPUT = 19;
    private static final int DATA_COUNT = 20;
    private static final int STAT_SCALE = 100;
    private static final int LOSS_TICKS_PER_MINUTE = 20 * 60;
    private static final int CHARGED_STORAGE_REQUIRED_TICKS = 20 * 60 * 10;
    private static final double CELL_LEAKAGE_DAMPING = 0.02;
    private static final Direction[] DIRECTIONS = Direction.values();

    private final BatteryChassisBlock block;
    private final ItemStackHandler inventory;
    private final IEnergyStorage unsidedEnergyStorage = new ChassisEnergyStorage(null);
    private final IEnergyStorage[] sidedEnergyStorages = new IEnergyStorage[DIRECTIONS.length];
    private final EnumSet<Direction> receivedSides = EnumSet.noneOf(Direction.class);
    private int burstTicksRemaining;
    private int energizedTicks;
    private boolean burstUsedThisTick;
    private double idleLossCarry;
    private long energyTelemetryTick = Long.MIN_VALUE;
    private int energyInputThisTick;
    private int energyOutputThisTick;
    private int lastEnergyInput;
    private int lastEnergyOutput;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_CONNECTOR_INPUT_CAP -> attachedInputConnectorCap();
                case DATA_CONNECTOR_OUTPUT_CAP -> attachedOutputConnectorCap();
                case DATA_LAST_INPUT -> lastEnergyInput();
                case DATA_LAST_OUTPUT -> lastEnergyOutput();
                default -> getRuntimeData(index);
            };
        }

        private int getRuntimeData(int index) {
            RuntimeContext context = runtimeContext();
            return switch (index) {
                case DATA_ENERGY -> energyStored(context);
                case DATA_ENERGY_CAPACITY -> energyCapacity(context);
                case DATA_MAX_INPUT -> effectiveInputRate(context);
                case DATA_MAX_OUTPUT -> effectiveOutputRate(context);
                case DATA_BURST_OUTPUT -> effectiveBurstOutputRate(context);
                case DATA_CELL_SLOTS -> context.activeSlots();
                case DATA_ENERGY_TRANSFER -> effectiveChassisTransfer(context);
                case DATA_BURST_TRANSFER -> scaledStat(context.stats(), MachineStat.BURST_TRANSFER);
                case DATA_BURST_DURATION -> Math.max(0, (int) Math.round(context.stats().value(MachineStat.BURST_DURATION)));
                case DATA_EFFICIENCY -> scaledStat(context.stats(), MachineStat.EFFICIENCY);
                case DATA_STABILITY -> scaledStat(context.stats(), MachineStat.STABILITY);
                case DATA_IDLE_LOSS -> scaledStat(context.stats(), MachineStat.IDLE_LOSS);
                case DATA_GLOBAL_MODIFIER_STRENGTH -> scaledStat(context.stats(), MachineStat.GLOBAL_MODIFIER_STRENGTH);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(context.stats(), MachineStat.REFINEMENT_POTENTIAL);
                case DATA_CELL_INPUT_SUM -> cellInputCeiling(context);
                case DATA_CELL_OUTPUT_SUM -> cellOutputCeiling(context);
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

    public BatteryChassisBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BATTERY_CHASSIS.get(), pos, blockState, MachineType.BATTERY_CHASSIS, 0, 0, 0);
        if (!(blockState.getBlock() instanceof BatteryChassisBlock chassisBlock)) {
            throw new IllegalStateException("Battery chassis block entity created for non-chassis block: " + blockState);
        }
        block = chassisBlock;
        burstTicksRemaining = block.material().burstDuration();
        inventory = new ItemStackHandler(maximumCellSlots()) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot >= 0 && slot < activeCellSlots() && isCell(stack);
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
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                ItemStack extracted = super.extractItem(slot, amount, simulate);
                if (!simulate && !extracted.isEmpty()) {
                    clampToIntrinsicCellCapacity(extracted);
                }
                return extracted;
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                syncInventoryToClient();
            }
        };
        for (Direction direction : DIRECTIONS) {
            sidedEnergyStorages[direction.ordinal()] = new ChassisEnergyStorage(direction);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryChassisBlockEntity chassis) {
        chassis.beginEnergyTelemetryTick();
        boolean exported = chassis.exportEnergy(level, pos);
        boolean lost = chassis.applyIdleLoss();
        chassis.updateEnergizedTicks();
        boolean clamped = chassis.clampCellEnergyToEffectiveCapacity();
        chassis.recoverBurstWindow();
        BaseMachineBlock.setActive(level, pos, state, chassis.energyStored() > 0 && (exported || lost));
        if (clamped) {
            chassis.setChanged();
        }
        chassis.receivedSides.clear();
    }

    public static boolean isCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return side == null ? unsidedEnergyStorage : sidedEnergyStorages[side.ordinal()];
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        return inventory;
    }

    public ItemStackHandler getInventory() {
        return inventory;
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
        RuntimeContext context = runtimeContext();
        int installedCells = installedCellCount(context);
        boolean hasCells = installedCells > 0;
        AdjacentEnergyConnector.Info sourceConnector = AdjacentEnergyConnector.forSource(level, worldPosition);
        AdjacentEnergyConnector.Info sinkConnector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("battery_chassis")
                .stage(block.material().stage())
                .state(
                        hasCells
                                ? getBlockState().getValue(BaseMachineBlock.ACTIVE)
                                        ? MachineInfoSnapshot.WorkState.RUNNING
                                        : MachineInfoSnapshot.WorkState.IDLE
                                : MachineInfoSnapshot.WorkState.BLOCKED,
                        hasCells
                                ? MachineInfoSnapshot.BlockedReason.NONE
                                : MachineInfoSnapshot.BlockedReason.MISSING_BATTERY_CELL
                )
                .energy(
                        energyStored(context),
                        energyCapacity(context),
                        Math.max(effectiveInputRate(context), effectiveOutputRate(context))
                )
                .energyTelemetry(
                        lastEnergyInput(),
                        lastEnergyOutput(),
                        Math.max(sourceConnector.transferRate(), sinkConnector.transferRate()),
                        energyBottleneck(context, sourceConnector, sinkConnector)
                )
                .slots(installedCells, context.activeSlots())
                .gear(hasCells ? MachineInfoSnapshot.GearSummary.CELLS_INSTALLED : MachineInfoSnapshot.GearSummary.NO_CELLS)
                .refinement(machineTraits())
                .build();
    }

    public BatteryChassisBlock block() {
        return block;
    }

    public ItemStack blockDisplayStack() {
        return new ItemStack(block);
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(
                machineTraits(),
                Component.translatable("container.rngtech." + block.material().blockId())
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BatteryChassisMenu(containerId, playerInventory, this, menuData);
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
                ItemStack drop = stack.copy();
                clampToIntrinsicCellCapacity(drop);
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), drop);
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        dropRefinementInventory(level);
    }

    public MachineStatAccumulator effectiveStats() {
        return effectiveStats(effectiveTraits());
    }

    private MachineTraits effectiveTraits() {
        return MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock());
    }

    private MachineStatAccumulator effectiveStats(MachineTraits traits) {
        MachineStatAccumulator stats = MachineBaseStatCatalog.batteryChassis(block.material());
        stats.apply(traits);
        return stats;
    }

    private RuntimeContext runtimeContext() {
        MachineTraits traits = effectiveTraits();
        MachineStatAccumulator stats = effectiveStats(traits);
        return new RuntimeContext(
                stats,
                traits,
                effectiveCellSlots(stats),
                energyCapacityMultiplier(traits)
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("BurstTicksRemaining", burstTicksRemaining);
        tag.putInt("EnergizedTicks", energizedTicks);
        tag.putDouble("IdleLossCarry", idleLossCarry);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        resizeInventoryToMaximumSlots();
        burstTicksRemaining = tag.getInt("BurstTicksRemaining");
        energizedTicks = Math.max(0, tag.getInt("EnergizedTicks"));
        idleLossCarry = tag.getDouble("IdleLossCarry");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private void syncInventoryToClient() {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void resizeInventoryToMaximumSlots() {
        int maximumSlots = maximumCellSlots();
        if (inventory.getSlots() == maximumSlots) {
            return;
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            stacks.add(inventory.getStackInSlot(slot).copy());
        }
        inventory.setSize(maximumSlots);
        for (int slot = 0; slot < Math.min(stacks.size(), maximumSlots); slot++) {
            inventory.setStackInSlot(slot, stacks.get(slot));
        }
    }

    private int receiveEnergyInternal(int toReceive, boolean simulate, Direction side) {
        return receiveEnergyInternal(toReceive, simulate, side, runtimeContext());
    }

    private int receiveEnergyInternal(int toReceive, boolean simulate, Direction side, RuntimeContext context) {
        if (toReceive <= 0 || !canReceiveEnergy(context)) {
            return 0;
        }

        int accepted = Math.min(toReceive, Math.min(effectiveInputRate(context), availableCellSpace(context)));
        if (accepted <= 0) {
            return 0;
        }

        int stored = storedAfterChargeEfficiency(accepted, context);
        if (!simulate && stored > 0) {
            distributeEnergyToCells(stored, context);
            if (side != null) {
                receivedSides.add(side);
            }
            recordEnergyInput(accepted);
            setChanged();
        }
        return accepted;
    }

    private int extractEnergyInternal(int toExtract, boolean simulate) {
        return extractEnergyInternal(toExtract, simulate, runtimeContext());
    }

    private int extractEnergyInternal(int toExtract, boolean simulate, RuntimeContext context) {
        if (toExtract <= 0 || !canExtractEnergy(context)) {
            return 0;
        }

        double efficiency = transferEfficiency(context);
        int cellDrawCeiling = Math.min(cellOutputCeiling(context), energyStored(context));
        int maxDeliveredByCells = deliveredAfterDischargeLoss(cellDrawCeiling, efficiency);
        int delivered = Math.min(toExtract, Math.min(effectiveOutputRate(context), maxDeliveredByCells));
        if (delivered <= 0) {
            return 0;
        }

        int toDraw = drawRequiredForDelivery(delivered, efficiency);
        if (!simulate) {
            int drawn = drainEnergyFromCells(toDraw, true, context);
            delivered = deliveredAfterDischargeLoss(drawn, efficiency);
            if (drawn > 0) {
                recordEnergyOutput(delivered);
                setChanged();
            }
        }
        return delivered;
    }

    private boolean canReceiveEnergy() {
        return canReceiveEnergy(runtimeContext());
    }

    private boolean canReceiveEnergy(RuntimeContext context) {
        return effectiveInputRate(context) > 0 && availableCellSpace(context) > 0;
    }

    private boolean canExtractEnergy() {
        return canExtractEnergy(runtimeContext());
    }

    private boolean canExtractEnergy(RuntimeContext context) {
        return effectiveOutputRate(context) > 0 && energyStored(context) > 0;
    }

    private int energyStored() {
        return energyStored(runtimeContext());
    }

    private int energyStored(RuntimeContext context) {
        int stored = 0;
        int activeSlots = context.activeSlots();
        for (int slot = 0; slot < activeSlots; slot++) {
            stored = saturatedAdd(stored, cellEnergyStored(slot, context));
        }
        return stored;
    }

    private int energyCapacity() {
        return energyCapacity(runtimeContext());
    }

    private int energyCapacity(RuntimeContext context) {
        int capacity = 0;
        int activeSlots = context.activeSlots();
        for (int slot = 0; slot < activeSlots; slot++) {
            capacity = saturatedAdd(capacity, effectiveCellCapacity(slot, context));
        }
        return capacity;
    }

    private int availableCellSpace() {
        return availableCellSpace(runtimeContext());
    }

    private int availableCellSpace(RuntimeContext context) {
        int space = 0;
        int activeSlots = context.activeSlots();
        for (int slot = 0; slot < activeSlots; slot++) {
            int capacity = effectiveCellCapacity(slot, context);
            int stored = cellEnergyStored(slot, context);
            if (stored < capacity) {
                space = saturatedAdd(space, capacity - stored);
            }
        }
        return space;
    }

    private int activeCellSlots() {
        return runtimeContext().activeSlots();
    }

    private int installedCellCount() {
        return installedCellCount(runtimeContext());
    }

    private int installedCellCount(RuntimeContext context) {
        int count = 0;
        for (int slot = 0; slot < context.activeSlots(); slot++) {
            if (isCell(inventory.getStackInSlot(slot))) {
                count++;
            }
        }
        return count;
    }

    private int effectiveCellSlots(MachineStatAccumulator stats) {
        return Mth.clamp((int) Math.floor(stats.value(MachineStat.BATTERY_SLOTS)), 0, maximumCellSlots());
    }

    private int maximumCellSlots() {
        return block.material().slots() + ModifierEligibilityProfiles.maxBatteryChassisAdditionalSlots();
    }

    private int cellEnergyStored(int slot) {
        return cellEnergyStored(slot, runtimeContext());
    }

    private int cellEnergyStored(int slot, RuntimeContext context) {
        return BatteryCellItem.energyStored(inventory.getStackInSlot(slot), effectiveCellCapacity(slot, context));
    }

    private int effectiveCellCapacity(int slot) {
        return effectiveCellCapacity(slot, runtimeContext());
    }

    private int effectiveCellCapacity(int slot, RuntimeContext context) {
        ItemStack stack = inventory.getStackInSlot(slot);
        int baseCapacity = BatteryCellItem.energyCapacity(stack);
        if (baseCapacity <= 0 || slot >= context.activeSlots()) {
            return baseCapacity;
        }
        return Math.max(1, saturatedRoundToInt(baseCapacity * context.capacityMultiplier()));
    }

    private double energyCapacityMultiplier(MachineTraits traits) {
        double base = 1.0;
        double added = 0.0;
        double increased = 0.0;
        double more = 1.0;
        for (MachineModifier modifier : traits.modifiers()) {
            boolean inactiveChargedStorage = isChargedStorageModifier(modifier) && !chargedStorageActive();
            if (inactiveChargedStorage) {
                continue;
            }
            for (MachineModifierEffect effect : modifier.effects()) {
                if (effect.stat() != MachineStat.ENERGY_CAPACITY
                        && effect.stat() != MachineStat.ENERGY_CAPACITY_FLAT) {
                    continue;
                }
                switch (effect.operation()) {
                    case ADD -> added += effect.value();
                    case INCREASED_PERCENT -> increased += effect.value();
                    case DECREASED_PERCENT -> increased -= effect.value();
                    case MORE, LESS -> more *= effect.value();
                }
            }
        }
        return Math.max(0.0, (base + added) * (1.0 + increased / 100.0) * more);
    }

    private boolean isChargedStorageModifier(MachineModifier modifier) {
        return ModifierEligibilityProfiles.BATTERY_CHASSIS_CHARGED_STORAGE_AFFIX_ID.equals(modifier.affixId());
    }

    private boolean chargedStorageActive() {
        return energizedTicks >= CHARGED_STORAGE_REQUIRED_TICKS;
    }

    private int effectiveInputRate() {
        return effectiveInputRate(runtimeContext());
    }

    private int effectiveInputRate(RuntimeContext context) {
        return cellInputCeiling(context);
    }

    private int effectiveOutputRate() {
        return effectiveOutputRate(runtimeContext());
    }

    private int effectiveOutputRate(RuntimeContext context) {
        return cellOutputCeiling(context);
    }

    private int effectiveChassisTransfer(MachineStatAccumulator stats) {
        return Math.max(0, saturatedRoundToInt(stats.value(MachineStat.ENERGY_TRANSFER)));
    }

    private int effectiveChassisTransfer(RuntimeContext context) {
        return effectiveChassisTransfer(context.stats());
    }

    private int effectiveBurstOutputRate(RuntimeContext context) {
        if (!context.hasBehavior(MachineBehavior.BURST_RELEASE)) {
            return 0;
        }
        double burstRating = effectiveChassisTransfer(context) * Math.max(0.0, context.stats().value(MachineStat.BURST_TRANSFER));
        return Math.max(0, saturatedRoundToInt(burstRating));
    }

    private int cellInputCeiling() {
        return cellInputCeiling(runtimeContext());
    }

    private int cellInputCeiling(RuntimeContext context) {
        int ceiling = 0;
        int activeSlots = context.activeSlots();
        for (int slot = 0; slot < activeSlots; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            int capacity = effectiveCellCapacity(slot, context);
            int stored = cellEnergyStored(slot, context);
            if (stored < capacity) {
                ceiling = saturatedAdd(ceiling, Math.min(BatteryCellItem.maxInput(stack), capacity - stored));
            }
        }
        return ceiling;
    }

    private int cellOutputCeiling() {
        return cellOutputCeiling(runtimeContext());
    }

    private int cellOutputCeiling(RuntimeContext context) {
        int ceiling = 0;
        int activeSlots = context.activeSlots();
        for (int slot = 0; slot < activeSlots; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            int stored = cellEnergyStored(slot, context);
            if (stored > 0) {
                ceiling = saturatedAdd(ceiling, Math.min(BatteryCellItem.maxOutput(stack), stored));
            }
        }
        return ceiling;
    }

    private int attachedInputConnectorCap() {
        return AdjacentEnergyConnector.forSink(level, worldPosition).transferRate();
    }

    private int attachedOutputConnectorCap() {
        return AdjacentEnergyConnector.forSource(level, worldPosition).transferRate();
    }

    private MachineInfoSnapshot.EnergyBottleneck energyBottleneck(
            RuntimeContext context,
            AdjacentEnergyConnector.Info sourceConnector,
            AdjacentEnergyConnector.Info sinkConnector
    ) {
        if (sourceConnector.present() && effectiveOutputRate(context) > sourceConnector.transferRate()) {
            return MachineInfoSnapshot.EnergyBottleneck.CONNECTOR_OUTPUT;
        }
        if (sinkConnector.present() && effectiveInputRate(context) > sinkConnector.transferRate()) {
            return MachineInfoSnapshot.EnergyBottleneck.CONNECTOR_INPUT;
        }
        return MachineInfoSnapshot.EnergyBottleneck.NONE;
    }

    private int lastEnergyInput() {
        beginEnergyTelemetryTick();
        return lastEnergyInput;
    }

    private int lastEnergyOutput() {
        beginEnergyTelemetryTick();
        return lastEnergyOutput;
    }

    private void recordEnergyInput(int amount) {
        if (amount <= 0) {
            return;
        }
        beginEnergyTelemetryTick();
        energyInputThisTick = saturatedAdd(energyInputThisTick, amount);
    }

    private void recordEnergyOutput(int amount) {
        if (amount <= 0) {
            return;
        }
        beginEnergyTelemetryTick();
        energyOutputThisTick = saturatedAdd(energyOutputThisTick, amount);
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
        lastEnergyOutput = energyOutputThisTick;
        energyInputThisTick = 0;
        energyOutputThisTick = 0;
    }

    private int storedAfterChargeEfficiency(int accepted) {
        return storedAfterChargeEfficiency(accepted, runtimeContext());
    }

    private int storedAfterChargeEfficiency(int accepted, RuntimeContext context) {
        double efficiency = Math.min(1.0, transferEfficiency(context));
        return Mth.clamp((int) Math.floor(accepted * efficiency), accepted > 0 ? 1 : 0, accepted);
    }

    private int deliveredAfterDischargeLoss(int drawn, double efficiency) {
        if (drawn <= 0) {
            return 0;
        }
        return Mth.clamp((int) Math.floor(drawn * Math.min(1.0, efficiency)), 1, drawn);
    }

    private int drawRequiredForDelivery(int delivered, double efficiency) {
        if (delivered <= 0) {
            return 0;
        }
        return Math.max(1, saturatedCeilToInt(delivered / Math.min(1.0, efficiency)));
    }

    private double transferEfficiency() {
        return transferEfficiency(runtimeContext());
    }

    private double transferEfficiency(RuntimeContext context) {
        return Math.max(0.01, context.stats().value(MachineStat.EFFICIENCY));
    }

    private void distributeEnergyToCells(int amount) {
        distributeEnergyToCells(amount, runtimeContext());
    }

    private void distributeEnergyToCells(int amount, RuntimeContext context) {
        if (context.hasBehavior(MachineBehavior.CHARGE_BALANCER)) {
            distributeEnergyToCellsEvenly(amount, context);
            return;
        }

        int remaining = amount;
        for (int slot : sortedSlotsByFill(true, context)) {
            if (remaining <= 0) {
                return;
            }
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!isCell(stack)) {
                continue;
            }
            int capacity = effectiveCellCapacity(slot, context);
            int stored = cellEnergyStored(slot, context);
            int received = Math.min(remaining, Math.min(BatteryCellItem.maxInput(stack), capacity - stored));
            if (received <= 0) {
                continue;
            }
            BatteryCellItem.setEnergy(stack, stored + received, capacity);
            remaining -= received;
        }
    }

    private void distributeEnergyToCellsEvenly(int amount, RuntimeContext context) {
        int remaining = amount;
        List<CellTransfer> cells = chargeableCells(context);
        List<CellTransfer> allCells = new ArrayList<>(cells);
        while (remaining > 0 && !cells.isEmpty()) {
            cells.sort(cellFillComparator(true));
            int share = Math.max(1, remaining / cells.size());
            int remainder = remaining % cells.size();
            boolean moved = false;
            for (CellTransfer cell : cells) {
                if (remaining <= 0) {
                    break;
                }
                int requested = Math.min(remaining, share + (remainder > 0 ? 1 : 0));
                if (remainder > 0) {
                    remainder--;
                }
                int received = cell.receive(requested);
                remaining -= received;
                moved |= received > 0;
            }
            cells.removeIf(cell -> !cell.canReceive());
            if (!moved) {
                break;
            }
        }
        applyCellTransfers(allCells);
    }

    private int drainEnergyFromCells(int amount, boolean respectTransferLimit) {
        return drainEnergyFromCells(amount, respectTransferLimit, runtimeContext());
    }

    private int drainEnergyFromCells(int amount, boolean respectTransferLimit, RuntimeContext context) {
        if (context.hasBehavior(MachineBehavior.CHARGE_BALANCER)) {
            return drainEnergyFromCellsEvenly(amount, respectTransferLimit, context);
        }

        int remaining = amount;
        int removed = 0;
        for (int slot : sortedSlotsByFill(false, context)) {
            if (remaining <= 0) {
                break;
            }
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!isCell(stack)) {
                continue;
            }
            int stored = cellEnergyStored(slot, context);
            int limit = respectTransferLimit ? Math.min(remaining, BatteryCellItem.maxOutput(stack)) : remaining;
            int extracted = Math.min(stored, limit);
            if (extracted > 0) {
                BatteryCellItem.setEnergy(stack, stored - extracted, effectiveCellCapacity(slot, context));
            }
            remaining -= extracted;
            removed += extracted;
        }
        return removed;
    }

    private int drainEnergyFromCellsEvenly(int amount, boolean respectTransferLimit, RuntimeContext context) {
        int remaining = amount;
        int removed = 0;
        List<CellTransfer> cells = drawableCells(respectTransferLimit, context);
        List<CellTransfer> allCells = new ArrayList<>(cells);
        while (remaining > 0 && !cells.isEmpty()) {
            cells.sort(cellFillComparator(false));
            int share = Math.max(1, remaining / cells.size());
            int remainder = remaining % cells.size();
            boolean moved = false;
            for (CellTransfer cell : cells) {
                if (remaining <= 0) {
                    break;
                }
                int requested = Math.min(remaining, share + (remainder > 0 ? 1 : 0));
                if (remainder > 0) {
                    remainder--;
                }
                int extracted = cell.extract(requested);
                remaining -= extracted;
                removed += extracted;
                moved |= extracted > 0;
            }
            cells.removeIf(cell -> !cell.canExtract());
            if (!moved) {
                break;
            }
        }
        applyCellTransfers(allCells);
        return removed;
    }

    private List<CellTransfer> chargeableCells() {
        return chargeableCells(runtimeContext());
    }

    private List<CellTransfer> chargeableCells(RuntimeContext context) {
        List<CellTransfer> cells = new ArrayList<>();
        int activeSlots = context.activeSlots();
        for (int slot = 0; slot < activeSlots; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!isCell(stack)) {
                continue;
            }
            int capacity = effectiveCellCapacity(slot, context);
            int stored = cellEnergyStored(slot, context);
            int transferLimit = Math.min(BatteryCellItem.maxInput(stack), capacity - stored);
            if (transferLimit > 0) {
                cells.add(new CellTransfer(slot, capacity, stored, transferLimit));
            }
        }
        return cells;
    }

    private List<CellTransfer> drawableCells(boolean respectTransferLimit) {
        return drawableCells(respectTransferLimit, runtimeContext());
    }

    private List<CellTransfer> drawableCells(boolean respectTransferLimit, RuntimeContext context) {
        List<CellTransfer> cells = new ArrayList<>();
        int activeSlots = context.activeSlots();
        for (int slot = 0; slot < activeSlots; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!isCell(stack)) {
                continue;
            }
            int stored = cellEnergyStored(slot, context);
            if (stored <= 0) {
                continue;
            }
            int transferLimit = respectTransferLimit ? Math.min(BatteryCellItem.maxOutput(stack), stored) : stored;
            if (transferLimit > 0) {
                cells.add(new CellTransfer(slot, effectiveCellCapacity(slot, context), stored, transferLimit));
            }
        }
        return cells;
    }

    private Comparator<CellTransfer> cellFillComparator(boolean ascending) {
        if (ascending) {
            return Comparator
                    .comparingDouble(CellTransfer::fillRatio)
                    .thenComparingInt(CellTransfer::slot);
        }
        return Comparator
                .comparingDouble((CellTransfer cell) -> -cell.fillRatio())
                .thenComparingInt(CellTransfer::slot);
    }

    private void applyCellTransfers(List<CellTransfer> cells) {
        for (CellTransfer cell : cells) {
            if (!cell.changed()) {
                continue;
            }
            ItemStack stack = inventory.getStackInSlot(cell.slot());
            if (isCell(stack)) {
                BatteryCellItem.setEnergy(stack, cell.energy(), cell.capacity());
            }
        }
    }

    private List<Integer> sortedSlotsByFill(boolean ascending) {
        return sortedSlotsByFill(ascending, runtimeContext());
    }

    private List<Integer> sortedSlotsByFill(boolean ascending, RuntimeContext context) {
        List<Integer> slots = new ArrayList<>();
        int activeSlots = context.activeSlots();
        for (int slot = 0; slot < activeSlots; slot++) {
            if (isCell(inventory.getStackInSlot(slot))) {
                slots.add(slot);
            }
        }
        if (!context.hasBehavior(MachineBehavior.CHARGE_BALANCER)) {
            return slots;
        }

        Comparator<Integer> comparator = Comparator
                .comparingDouble((Integer slot) -> fillRatio(slot, context))
                .thenComparingInt(Integer::intValue);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        slots.sort(comparator);
        return slots;
    }

    private double fillRatio(int slot) {
        return fillRatio(slot, runtimeContext());
    }

    private double fillRatio(int slot, RuntimeContext context) {
        int capacity = effectiveCellCapacity(slot, context);
        return capacity <= 0 ? 0.0 : cellEnergyStored(slot, context) / (double) capacity;
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        RuntimeContext context = runtimeContext();
        if (energyStored(context) <= 0) {
            return false;
        }

        int remainingOutput = Math.min(effectiveOutputRate(context), energyStored(context));
        boolean exported = false;
        for (Direction direction : DIRECTIONS) {
            if (remainingOutput <= 0 || energyStored(context) <= 0) {
                return exported;
            }
            if (receivedSides.contains(direction)) {
                continue;
            }

            BlockPos targetPos = pos.relative(direction);
            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (blockEntity instanceof BatteryChassisBlockEntity targetChassis) {
                int delivered = equalizeWithAdjacentChassis(targetChassis, direction, remainingOutput, context);
                remainingOutput -= delivered;
                exported |= delivered > 0;
                continue;
            }

            IEnergyStorage target = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    targetPos,
                    direction.getOpposite()
            );
            if (target == null || !target.canReceive()) {
                continue;
            }

            int offered = extractEnergyInternal(remainingOutput, true, context);
            int received = target.receiveEnergy(offered, false);
            int delivered = extractEnergyInternal(received, false, context);
            remainingOutput -= delivered;
            exported |= delivered > 0;
        }
        return exported;
    }

    private int equalizeWithAdjacentChassis(
            BatteryChassisBlockEntity target,
            Direction direction,
            int remainingOutput,
            RuntimeContext sourceContext
    ) {
        RuntimeContext targetContext = target.runtimeContext();
        int offer = adjacentChassisEqualizationOffer(target, targetContext, remainingOutput, sourceContext);
        if (offer <= 0) {
            return 0;
        }

        Direction targetSide = direction.getOpposite();
        int accepted = target.receiveEnergyInternal(offer, true, targetSide, targetContext);
        int delivered = extractEnergyInternal(accepted, false, sourceContext);
        target.receiveEnergyInternal(delivered, false, targetSide, targetContext);
        return delivered;
    }

    private int adjacentChassisEqualizationOffer(BatteryChassisBlockEntity target, int remainingOutput) {
        return adjacentChassisEqualizationOffer(target, target.runtimeContext(), remainingOutput, runtimeContext());
    }

    private int adjacentChassisEqualizationOffer(
            BatteryChassisBlockEntity target,
            RuntimeContext targetContext,
            int remainingOutput,
            RuntimeContext sourceContext
    ) {
        if (remainingOutput <= 0 || !canExtractEnergy(sourceContext) || !target.canReceiveEnergy(targetContext)) {
            return 0;
        }

        int maxOffer = extractEnergyInternal(remainingOutput, true, sourceContext);
        maxOffer = Math.min(maxOffer, target.receiveEnergyInternal(maxOffer, true, null, targetContext));
        if (maxOffer <= 0) {
            return 0;
        }

        return adjacentChassisEqualizationLimit(target, targetContext, maxOffer, sourceContext);
    }

    private int adjacentChassisEqualizationLimit(BatteryChassisBlockEntity target, int maxOffer) {
        return adjacentChassisEqualizationLimit(target, target.runtimeContext(), maxOffer, runtimeContext());
    }

    private int adjacentChassisEqualizationLimit(
            BatteryChassisBlockEntity target,
            RuntimeContext targetContext,
            int maxOffer,
            RuntimeContext sourceContext
    ) {
        int sourceEnergy = energyStored(sourceContext);
        int sourceCapacity = energyCapacity(sourceContext);
        int targetEnergy = target.energyStored(targetContext);
        int targetCapacity = target.energyCapacity(targetContext);
        if (sourceCapacity <= 0
                || targetCapacity <= 0
                || (long) sourceEnergy * targetCapacity <= (long) targetEnergy * sourceCapacity) {
            return 0;
        }

        int low = 0;
        int high = maxOffer;
        while (low < high) {
            int mid = low + (high - low + 1) / 2;
            if (keepsSourceAtLeastAsFullAsTargetAfterTransfer(
                    sourceEnergy,
                    sourceCapacity,
                    target,
                    targetContext,
                    targetEnergy,
                    targetCapacity,
                    mid,
                    sourceContext
            )) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    private boolean keepsSourceAtLeastAsFullAsTargetAfterTransfer(
            int sourceEnergy,
            int sourceCapacity,
            BatteryChassisBlockEntity target,
            RuntimeContext targetContext,
            int targetEnergy,
            int targetCapacity,
            int delivered,
            RuntimeContext sourceContext
    ) {
        int sourceDraw = drawRequiredForDelivery(delivered, transferEfficiency(sourceContext));
        int targetStored = target.storedAfterChargeEfficiency(delivered, targetContext);
        int remainingSourceEnergy = Math.max(0, sourceEnergy - sourceDraw);
        int nextTargetEnergy = Math.min(targetCapacity, saturatedAdd(targetEnergy, targetStored));
        return (long) remainingSourceEnergy * targetCapacity >= (long) nextTargetEnergy * sourceCapacity;
    }

    private boolean applyIdleLoss() {
        RuntimeContext context = runtimeContext();
        if (energyStored(context) <= 0) {
            idleLossCarry = 0.0;
            return false;
        }

        double chassisLoss = Math.max(0.0, context.stats().value(MachineStat.IDLE_LOSS));
        double lossThisTick = 0.0;
        int activeSlots = context.activeSlots();
        for (int slot = 0; slot < activeSlots; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            int stored = cellEnergyStored(slot, context);
            if (stored <= 0) {
                continue;
            }
            double cellLoss = effectiveCellIdleLoss(stack, context);
            lossThisTick += stored * ((cellLoss + chassisLoss) / 100.0) / LOSS_TICKS_PER_MINUTE;
        }

        if (lossThisTick <= 0.0) {
            return false;
        }

        idleLossCarry += lossThisTick;
        int loss = saturatedFloorToInt(idleLossCarry);
        if (loss <= 0) {
            return false;
        }

        int removed = drainEnergyFromCells(loss, false, context);
        idleLossCarry -= removed;
        if (removed > 0) {
            setChanged();
            return true;
        }
        return false;
    }

    private void updateEnergizedTicks() {
        if (energyStored() <= 0) {
            energizedTicks = 0;
            return;
        }
        if (energizedTicks < CHARGED_STORAGE_REQUIRED_TICKS) {
            energizedTicks++;
        }
    }

    private boolean clampCellEnergyToEffectiveCapacity() {
        RuntimeContext context = runtimeContext();
        boolean changed = false;
        int activeSlots = context.activeSlots();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!isCell(stack)) {
                continue;
            }
            int capacity = slot < activeSlots ? effectiveCellCapacity(slot, context) : BatteryCellItem.energyCapacity(stack);
            changed |= BatteryCellItem.clampEnergy(stack, capacity);
        }
        return changed;
    }

    private void clampToIntrinsicCellCapacity(ItemStack stack) {
        BatteryCellItem.clampEnergy(stack, BatteryCellItem.energyCapacity(stack));
    }

    private void recoverBurstWindow() {
        RuntimeContext context = runtimeContext();
        int burstDuration = Math.max(0, (int) Math.round(context.stats().value(MachineStat.BURST_DURATION)));
        if (!context.hasBehavior(MachineBehavior.BURST_RELEASE) || burstDuration <= 0) {
            burstTicksRemaining = 0;
            burstUsedThisTick = false;
            return;
        }
        if (burstUsedThisTick) {
            burstUsedThisTick = false;
            return;
        }
        if (burstTicksRemaining < burstDuration) {
            burstTicksRemaining++;
            setChanged();
        }
    }

    private double effectiveCellIdleLoss(ItemStack stack) {
        return effectiveCellIdleLoss(stack, runtimeContext());
    }

    private double effectiveCellIdleLoss(ItemStack stack, RuntimeContext context) {
        double loss = BatteryCellItem.idleLossPercentPerMinute(stack);
        if (context.hasBehavior(MachineBehavior.CELL_LEAKAGE_DAMPING)) {
            if (block.material().sealsCellLeakage()) {
                return 0.0;
            }
            loss -= CELL_LEAKAGE_DAMPING;
        }
        return Math.max(0.0, loss);
    }

    private boolean hasBehavior(MachineBehavior behavior) {
        return runtimeContext().hasBehavior(behavior);
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return saturatedRoundToInt(stats.value(stat) * STAT_SCALE);
    }

    private static int saturatedAdd(int left, int right) {
        if (left >= Integer.MAX_VALUE || right >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, left) + Math.max(0, right));
    }

    private static int saturatedRoundToInt(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) Math.round(value);
    }

    private static int saturatedCeilToInt(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) Math.ceil(value);
    }

    private static int saturatedFloorToInt(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) Math.floor(value);
    }

    private record RuntimeContext(
            MachineStatAccumulator stats,
            MachineTraits traits,
            int activeSlots,
            double capacityMultiplier
    ) {
        private boolean hasBehavior(MachineBehavior behavior) {
            return traits.hasBehavior(behavior);
        }
    }

    private static final class CellTransfer {
        private final int slot;
        private final int capacity;
        private final int initialEnergy;
        private int energy;
        private int transferRemaining;

        private CellTransfer(int slot, int capacity, int energy, int transferLimit) {
            this.slot = slot;
            this.capacity = capacity;
            this.initialEnergy = energy;
            this.energy = energy;
            this.transferRemaining = Math.max(0, transferLimit);
        }

        private int slot() {
            return slot;
        }

        private int capacity() {
            return capacity;
        }

        private int energy() {
            return energy;
        }

        private double fillRatio() {
            return capacity <= 0 ? 0.0 : energy / (double) capacity;
        }

        private boolean canReceive() {
            return transferRemaining > 0 && energy < capacity;
        }

        private boolean canExtract() {
            return transferRemaining > 0 && energy > 0;
        }

        private int receive(int amount) {
            int received = Math.min(Math.max(0, amount), Math.min(transferRemaining, capacity - energy));
            energy += received;
            transferRemaining -= received;
            return received;
        }

        private int extract(int amount) {
            int extracted = Math.min(Math.max(0, amount), Math.min(transferRemaining, energy));
            energy -= extracted;
            transferRemaining -= extracted;
            return extracted;
        }

        private boolean changed() {
            return energy != initialEnergy;
        }
    }

    private final class ChassisEnergyStorage implements IEnergyStorage {
        private final Direction side;

        private ChassisEnergyStorage(Direction side) {
            this.side = side;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return receiveEnergyInternal(toReceive, simulate, side);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return extractEnergyInternal(toExtract, simulate);
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
            return canExtractEnergy();
        }

        @Override
        public boolean canReceive() {
            return canReceiveEnergy();
        }
    }
}
