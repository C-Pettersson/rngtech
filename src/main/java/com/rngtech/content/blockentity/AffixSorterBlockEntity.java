package com.rngtech.content.blockentity;

import com.rngtech.content.affixsorter.AffixSorterCosts;
import com.rngtech.content.affixsorter.SorterMatcher;
import com.rngtech.content.affixsorter.SorterRule;
import com.rngtech.content.affixsorter.SorterRules;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.gear.IdentifyingItemHandler;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.GearParts;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.menu.AffixSorterMenu;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Arrays;

/**
 * Checks identified items against an ordered rule list and routes each to its Pass or Reject buffer. Each face is set
 * to Pass, Reject, or neither; cables pull from the faces, and the sorter never pushes or voids an item.
 */
public class AffixSorterBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider {
    public static final int STAGE = 8;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_PASS_START = 1;
    public static final int BUFFER_SLOTS = 3;
    public static final int SLOT_REJECT_START = SLOT_PASS_START + BUFFER_SLOTS;
    public static final int PROCESS_SLOT_COUNT = SLOT_REJECT_START + BUFFER_SLOTS;
    public static final int SLOT_BATTERY_CELL = 0;
    public static final int SLOT_RULE_CORE = 1;
    public static final int SLOT_SERVO = 2;
    public static final int GEAR_SLOT_COUNT = 3;
    public static final int PULSE_TICKS = 4;
    /** Block event sent when an item is sorted; its parameter is the {@link SorterRule.Route} ordinal. */
    public static final int EVENT_SORTED = 1;
    private static final int VISUAL_SYNC_INTERVAL = 5;

    /** What a face does, by its side relative to the sorter's front. */
    public enum FaceRoute {
        NONE,
        PASS,
        REJECT;

        public FaceRoute next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Faces in the order the screen shows them, relative to the sorter's front. */
    public enum RelativeFace {
        FRONT,
        BACK,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM;

        public Direction absolute(Direction facing) {
            return switch (this) {
                case FRONT -> facing;
                case BACK -> facing.getOpposite();
                case LEFT -> facing.getClockWise();
                case RIGHT -> facing.getCounterClockWise();
                case TOP -> Direction.UP;
                case BOTTOM -> Direction.DOWN;
            };
        }
    }

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_CORE = 1;
    public static final int STATUS_NO_INPUT = 2;
    public static final int STATUS_OUTPUT_FULL = 3;
    public static final int STATUS_NO_POWER = 4;
    public static final int STATUS_NOT_IDENTIFIED = 5;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_PROCESSING_TICKS = 1;
    public static final int DATA_ENERGY = 2;
    public static final int DATA_ENERGY_CAPACITY = 3;
    public static final int DATA_ENERGY_PER_TICK = 4;
    public static final int DATA_STATUS = 5;
    public static final int DATA_RULE_CAPACITY = 6;
    public static final int DATA_ENABLED_RULES = 7;
    public static final int DATA_FE_PER_ITEM = 8;
    public static final int DATA_LAST_RULE = 9;
    public static final int DATA_LAST_ROUTE = 10;
    public static final int DATA_PROCESSING_SPEED = 11;
    public static final int DATA_ENERGY_USAGE = 12;
    public static final int DATA_ENERGY_CAPACITY_STAT = 13;
    public static final int DATA_FACE_START = 14;
    public static final int DATA_COUNT = DATA_FACE_START + RelativeFace.values().length;
    private static final int STAT_SCALE = 100;
    private static final double NO_BATTERY_PROCESSING_SPEED = 0.80;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT && SorterMatcher.canSort(stack);
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
    private final ItemStackHandler gearInventory = new IdentifyingItemHandler(GEAR_SLOT_COUNT, this::getLevel) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_RULE_CORE -> isRuleCore(stack);
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
            }
            clampInternalEnergy();
            setChanged();
        }
    };
    private final IEnergyStorage energyStorage = new SorterEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final InstalledBatteryCell batteryCell = new InstalledBatteryCell(this::batteryCellStack);
    private final IItemHandler inputHandler = new InputItemHandler();
    private final IItemHandler passHandler = new BufferItemHandler(SLOT_PASS_START);
    private final IItemHandler rejectHandler = new BufferItemHandler(SLOT_REJECT_START);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_FACE_START && index < DATA_COUNT) {
                return faces[index - DATA_FACE_START].ordinal();
            }
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_PROGRESS -> syncedProgress(0, progress);
                case DATA_PROCESSING_TICKS -> inputStack().isEmpty() ? 0 : processingTicks(stats);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_ENERGY_PER_TICK -> inputStack().isEmpty() ? 0 : energyCostPerTick(stats);
                case DATA_STATUS -> statusCode(stats);
                case DATA_RULE_CAPACITY -> ruleCapacity(stats);
                case DATA_ENABLED_RULES -> activeRules(stats).enabledCount();
                case DATA_FE_PER_ITEM -> (int) Math.min(Integer.MAX_VALUE, energyCost(stats));
                case DATA_LAST_RULE -> lastRule;
                case DATA_LAST_ROUTE -> lastRoute;
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
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

    private SorterRules rules = SorterRules.EMPTY;
    private final FaceRoute[] faces = defaultFaces();
    private int progress;
    private int internalEnergy;
    private int pulseTicks;
    private int lastRule = -2;
    private int lastRoute = -1;
    private AffixSorterVisualState visualState = AffixSorterVisualState.IDLE;
    private AffixSorterVisualState lastSyncedVisual;
    private ItemStack displayInput = ItemStack.EMPTY;
    private FaceRoute flashRoute = FaceRoute.NONE;
    private long flashTime = Long.MIN_VALUE;

    public AffixSorterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.AFFIX_SORTER.get(), pos, blockState, MachineType.AFFIX_SORTER, SLOT_INPUT, SLOT_INPUT, SLOT_PASS_START);
        trackStatSlots(gearInventory);
    }

    /** Pass on the left, Reject on the right, as seen from the front; input everywhere else. */
    private static FaceRoute[] defaultFaces() {
        FaceRoute[] faces = new FaceRoute[RelativeFace.values().length];
        Arrays.fill(faces, FaceRoute.NONE);
        faces[RelativeFace.LEFT.ordinal()] = FaceRoute.PASS;
        faces[RelativeFace.RIGHT.ordinal()] = FaceRoute.REJECT;
        return faces;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AffixSorterBlockEntity sorter) {
        sorter.tickPulse(level, pos, state);
        sorter.tickWork(level, pos, state);
        if (level.getGameTime() % VISUAL_SYNC_INTERVAL == 0) {
            sorter.syncVisualState(level);
        }
    }

    private void tickWork(Level level, BlockPos pos, BlockState state) {
        MachineStatAccumulator stats = effectiveStats();
        if (statusCode(stats) != STATUS_READY) {
            if (inputStack().isEmpty() || !hasRuleCore()) {
                resetCycle();
            }
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }
        if (progress == 0 && ProcessingChance.rollInstant(level, stats)) {
            int fullCost = (int) Math.min(Integer.MAX_VALUE, energyCost(stats));
            if (consumeWorkingEnergy(fullCost, true) >= fullCost) {
                markInstantProcess();
                consumeWorkingEnergy(fullCost, false);
                sortOne(level, pos, state, stats);
                BaseMachineBlock.setActive(level, pos, state, true);
                setChanged();
                return;
            }
        }
        int perTick = energyCostPerTick(stats);
        if (consumeWorkingEnergy(perTick, true) < perTick) {
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }
        consumeWorkingEnergy(perTick, false);
        progress++;
        if (progress >= processingTicks(stats)) {
            sortOne(level, pos, state, stats);
        }
        BaseMachineBlock.setActive(level, pos, state, true);
        setChanged();
    }

    private void sortOne(Level level, BlockPos pos, BlockState state, MachineStatAccumulator stats) {
        ItemStack item = inputStack().copyWithCount(1);
        SorterMatcher.Decision decision = SorterMatcher.decide(activeRules(stats), item);
        int start = decision.route() == SorterRule.Route.PASS ? SLOT_PASS_START : SLOT_REJECT_START;
        if (!insertIntoBuffer(start, item, true)) {
            return;
        }
        insertIntoBuffer(start, item, false);
        inputStack().shrink(1);
        processInventory.setStackInSlot(SLOT_INPUT, inputStack());
        lastRule = decision.ruleIndex();
        lastRoute = decision.route().ordinal();
        level.blockEvent(pos, state.getBlock(), EVENT_SORTED, lastRoute);
        if (decision.pulse()) {
            startPulse(level, pos, state);
        }
        resetCycle();
    }

    private boolean insertIntoBuffer(int start, ItemStack item, boolean simulate) {
        ItemStack remaining = item.copy();
        for (int slot = start; slot < start + BUFFER_SLOTS && !remaining.isEmpty(); slot++) {
            remaining = forceInsert(slot, remaining, simulate);
        }
        return remaining.isEmpty();
    }

    /** Buffer slots refuse outside inserts, so the sorter fills them directly. */
    private ItemStack forceInsert(int slot, ItemStack stack, boolean simulate) {
        ItemStack existing = processInventory.getStackInSlot(slot);
        int limit = Math.min(stack.getMaxStackSize(), processInventory.getSlotLimit(slot));
        if (existing.isEmpty()) {
            if (!simulate) {
                processInventory.setStackInSlot(slot, stack.copyWithCount(Math.min(limit, stack.getCount())));
            }
            return stack.getCount() > limit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
        }
        if (!ItemStack.isSameItemSameComponents(existing, stack) || existing.getCount() >= limit) {
            return stack;
        }
        int moved = Math.min(limit - existing.getCount(), stack.getCount());
        if (!simulate) {
            processInventory.setStackInSlot(slot, existing.copyWithCount(existing.getCount() + moved));
        }
        return moved >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
    }

    private void startPulse(Level level, BlockPos pos, BlockState state) {
        boolean wasOff = pulseTicks <= 0;
        pulseTicks = PULSE_TICKS;
        if (wasOff) {
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    private void tickPulse(Level level, BlockPos pos, BlockState state) {
        if (pulseTicks > 0 && --pulseTicks == 0) {
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    /** Redstone the sorter sends out of {@code side}: a short pulse after a pulsing rule, from faces that route no items. */
    public int pulseSignal(Direction side) {
        return pulseTicks > 0 && route(side) == FaceRoute.NONE ? 15 : 0;
    }

    public FaceRoute route(Direction side) {
        Direction facing = getBlockState().getValue(BaseMachineBlock.FACING);
        for (RelativeFace face : RelativeFace.values()) {
            if (face.absolute(facing) == side) {
                return faces[face.ordinal()];
            }
        }
        return FaceRoute.NONE;
    }

    public void cycleFace(int face) {
        if (face >= 0 && face < faces.length) {
            faces[face] = faces[face].next();
            if (level != null) {
                level.invalidateCapabilities(worldPosition);
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
                if (!level.isClientSide) {
                    syncVisualState(level);
                }
            }
            setChanged();
        }
    }

    /** Client copy of the state the renderer draws; synced from the server in the block entity update tag. */
    public AffixSorterVisualState visualState() {
        return visualState;
    }

    /** The input the renderer draws on the reader; client side. */
    public ItemStack displayInput() {
        return displayInput;
    }

    /** The route the last sorted item took, which flashes on its collars; client side. */
    public FaceRoute flashRoute() {
        return flashRoute;
    }

    /** Game time of the last sort the client saw. */
    public long flashTime() {
        return flashTime;
    }

    public AffixSorterVisualState currentVisualState() {
        return new AffixSorterVisualState(AffixSorterVisualState.pack(faces), connectedFaces(), inputStack().getItem());
    }

    /** Faces with something attached: a cable or Universal Connector port turned toward the sorter, or any item handler. */
    private int connectedFaces() {
        if (level == null) {
            return 0;
        }
        Direction facing = getBlockState().getValue(BaseMachineBlock.FACING);
        int connected = 0;
        for (RelativeFace face : RelativeFace.values()) {
            Direction side = face.absolute(facing);
            BlockPos neighbor = worldPosition.relative(side);
            if (!level.isLoaded(neighbor)) {
                continue;
            }
            BlockState state = level.getBlockState(neighbor);
            Direction back = side.getOpposite();
            boolean attached = state.getBlock() instanceof CableBlock
                    ? state.getValue(CableBlock.connectorProperty(back))
                    : UniversalConnectorBlock.hasConnector(state, back)
                            || level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, back) != null;
            if (attached) {
                connected |= 1 << face.ordinal();
            }
        }
        return connected;
    }

    private void syncVisualState(Level level) {
        AffixSorterVisualState current = currentVisualState();
        if (!current.equals(lastSyncedVisual)) {
            lastSyncedVisual = current;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void applyVisualState(AffixSorterVisualState state) {
        if (displayInput.getItem() != state.input()) {
            displayInput = state.input() == Items.AIR ? ItemStack.EMPTY : new ItemStack(state.input());
        }
        visualState = state;
        for (RelativeFace face : RelativeFace.values()) {
            faces[face.ordinal()] = state.route(face);
        }
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
        applyVisualState(AffixSorterVisualState.load(tag));
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        applyVisualState(AffixSorterVisualState.load(packet.getTag()));
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id != EVENT_SORTED) {
            return super.triggerEvent(id, type);
        }
        if (level != null && level.isClientSide) {
            flashRoute = type == SorterRule.Route.PASS.ordinal() ? FaceRoute.PASS : FaceRoute.REJECT;
            flashTime = level.getGameTime();
        }
        return true;
    }

    public SorterRules sorterRules() {
        return rules;
    }

    /** Stores a new list; rules past the current Rule Capacity are switched off, from the bottom. */
    public void setSorterRules(SorterRules next) {
        rules = (next == null ? SorterRules.EMPTY : next).limitedTo(ruleCapacity(effectiveStats()));
        resetCycle();
        setChanged();
    }

    /** The list as it runs: a smaller capacity, such as after swapping the Rule Core, switches off the rules past it. */
    public SorterRules activeRules(MachineStatAccumulator stats) {
        return rules.limitedTo(ruleCapacity(stats));
    }

    public int ruleCapacity(MachineStatAccumulator stats) {
        return hasRuleCore() ? Math.max(0, stats.intValue(MachineStat.RULE_CAPACITY)) : 0;
    }

    public long energyCost(MachineStatAccumulator stats) {
        long base = AffixSorterCosts.sorterFePerItem(activeRules(stats).enabledCount());
        return stats.adjustedEnergyCost((int) Math.min(Integer.MAX_VALUE, base));
    }

    private int processingTicks(MachineStatAccumulator stats) {
        return stats.adjustedProcessingTicks(AffixSorterCosts.sorterTicksPerItem());
    }

    private int energyCostPerTick(MachineStatAccumulator stats) {
        return Math.max(1, (int) Math.ceil(energyCost(stats) / (double) processingTicks(stats)));
    }

    public int statusCode(MachineStatAccumulator stats) {
        if (!hasRuleCore()) {
            return STATUS_MISSING_CORE;
        }
        ItemStack input = inputStack();
        if (input.isEmpty()) {
            return STATUS_NO_INPUT;
        }
        if (!SorterMatcher.canSort(input)) {
            return STATUS_NOT_IDENTIFIED;
        }
        SorterMatcher.Decision decision = SorterMatcher.decide(activeRules(stats), input.copyWithCount(1));
        int start = decision.route() == SorterRule.Route.PASS ? SLOT_PASS_START : SLOT_REJECT_START;
        if (!insertIntoBuffer(start, input.copyWithCount(1), true)) {
            return STATUS_OUTPUT_FULL;
        }
        int perTick = energyCostPerTick(stats);
        return consumeWorkingEnergy(perTick, true) < perTick ? STATUS_NO_POWER : STATUS_READY;
    }

    public static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_MISSING_CORE -> "missing_core";
            case STATUS_NO_INPUT -> "no_input";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_NO_POWER -> "no_power";
            case STATUS_NOT_IDENTIFIED -> "not_identified";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.affix_sorter.status." + name;
    }

    public static boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public static boolean isRuleCore(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.RULE_CORE);
    }

    public static boolean isServo(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.SERVO);
    }

    private boolean hasRuleCore() {
        return isRuleCore(gearInventory.getStackInSlot(SLOT_RULE_CORE));
    }

    public MachineStatAccumulator effectiveStats() {
        return cachedStats(0);
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.affixSorter();
        stats.apply(effectiveTraits());
        applyPartStats(stats, gearInventory.getStackInSlot(SLOT_RULE_CORE));
        applyPartStats(stats, gearInventory.getStackInSlot(SLOT_SERVO));
        if (!isBatteryCell(batteryCellStack())) {
            stats.apply(MachineStatAccumulator.NO_BATTERY_SOURCE, new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.PROCESSING_SPEED,
                    ModifierOperation.LESS,
                    NO_BATTERY_PROCESSING_SPEED
            ));
        }
        return stats;
    }

    private static void applyPartStats(MachineStatAccumulator stats, ItemStack stack) {
        if (stack.getItem() instanceof MachinePartItem) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        }
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        int status = statusCode(stats);
        int energyDemand = status == STATUS_READY || status == STATUS_NO_POWER ? energyCostPerTick(stats) : 0;
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("affix_sorter")
                .stage(STAGE)
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY,
                                getBlockState().getValue(BaseMachineBlock.ACTIVE),
                                status == STATUS_NO_INPUT
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .progress(progress, inputStack().isEmpty() ? 0 : processingTicks(stats))
                .energy(energyStored(), energyCapacity(), -energyDemand)
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

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(machineTraits(), Component.translatable("container.rngtech.affix_sorter"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AffixSorterMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        MachineTraits.STREAM_CODEC.encode(buffer, machineTraits());
        SorterRules.STREAM_CODEC.encode(buffer, rules);
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
        return trackedEnergyStorage;
    }

    /** Each face is a Pass or Reject output, or takes items in. */
    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == null) {
            return processInventory;
        }
        return switch (route(side)) {
            case PASS -> passHandler;
            case REJECT -> rejectHandler;
            case NONE -> inputHandler;
        };
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return processInventory;
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

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!rules.rules().isEmpty()) {
            components.set(ModDataComponents.SORTER_RULES.get(), rules);
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        SorterRules stored = componentInput.get(ModDataComponents.SORTER_RULES.get());
        if (stored != null) {
            rules = stored;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Rules");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        SorterRules.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), rules)
                .result().ifPresent(encoded -> tag.put("Rules", encoded));
        int[] routes = new int[faces.length];
        for (int face = 0; face < faces.length; face++) {
            routes[face] = faces[face].ordinal();
        }
        tag.putIntArray("Faces", routes);
        tag.putInt("Progress", progress);
        tag.putInt("Energy", internalEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        if (tag.contains("Rules")) {
            rules = SorterRules.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("Rules"))
                    .result().orElse(SorterRules.EMPTY);
        }
        int[] routes = tag.getIntArray("Faces");
        if (routes.length == faces.length) {
            for (int face = 0; face < faces.length; face++) {
                faces[face] = FaceRoute.values()[Math.floorMod(routes[face], FaceRoute.values().length)];
            }
        }
        progress = tag.getInt("Progress");
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        clampInternalEnergy();
    }

    private void resetCycle() {
        progress = 0;
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
    }

    private ItemStack inputStack() {
        return processInventory.getStackInSlot(SLOT_INPUT);
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private int energyStored() {
        return internalEnergyStored() + batteryCell.energyStored();
    }

    private int energyCapacity() {
        return internalEnergyCapacity() + batteryCell.capacity();
    }

    private int internalEnergyCapacity() {
        return Math.max(1, (int) Math.round(effectiveStats().value(MachineStat.ENERGY_CAPACITY)));
    }

    private int internalEnergyStored() {
        return Math.max(0, Math.min(internalEnergy, internalEnergyCapacity()));
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
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
        IEnergyStorage cell = batteryCell.storage();
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
        setChanged();
        return consumed;
    }

    private final class InputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(SLOT_INPUT);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return processInventory.insertItem(SLOT_INPUT, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(SLOT_INPUT);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return processInventory.isItemValid(SLOT_INPUT, stack);
        }
    }

    /** One route's buffer: extract only. */
    private final class BufferItemHandler implements IItemHandler {
        private final int start;

        private BufferItemHandler(int start) {
            this.start = start;
        }

        @Override
        public int getSlots() {
            return BUFFER_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(start + slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return processInventory.extractItem(start + slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(start + slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    private final class SorterEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (toReceive <= 0) {
                return 0;
            }
            int received = receiveInternalEnergy(toReceive, simulate);
            int remaining = toReceive - received;
            IEnergyStorage cell = batteryCell.storage();
            if (cell != null && cell.canReceive() && remaining > 0) {
                received += cell.receiveEnergy(remaining, simulate);
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
            IEnergyStorage cell = batteryCell.storage();
            return cell != null && cell.canReceive() && cell.getEnergyStored() < cell.getMaxEnergyStored();
        }
    }
}
