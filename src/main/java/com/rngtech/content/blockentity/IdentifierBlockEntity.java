package com.rngtech.content.blockentity;

import com.rngtech.content.affixsorter.AffixSorterCosts;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.gear.IdentifyingItemHandler;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.item.GearParts;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.itemfilter.ItemFilterMatcher;
import com.rngtech.content.menu.IdentifierMenu;
import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModSounds;
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
import com.rngtech.rpg.jewel.JewelEffects;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrderConditions;
import com.rngtech.rpg.workorder.WorkOrderFamilies;
import com.rngtech.rpg.workorder.WorkOrderHost;
import com.rngtech.rpg.workorder.WorkReport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * Identifies unidentified items for FE and Identifying Fluid, rolling them exactly as a crafting grid would. It is the
 * one way automation identifies items.
 */
public class IdentifierBlockEntity extends BaseMachineBlockEntity
        implements MenuProvider, PurgeableFluidStorage, MachineMasteryHost, MachineInfoProvider, WorkOrderHost {
    public static final int STAGE = 8;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_FLUID_CONTAINER = 2;
    public static final int PROCESS_SLOT_COUNT = 3;
    public static final int SLOT_BATTERY_CELL = 0;
    public static final int SLOT_FLUID_PUMP = 1;
    public static final int SLOT_SERVO = 2;
    public static final int GEAR_SLOT_COUNT = 3;
    public static final int TANK_CAPACITY = 8000;
    public static final int PURGE_FLUID_TANK = 0;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_GEAR = 1;
    public static final int STATUS_NO_INPUT = 2;
    public static final int STATUS_NO_FLUID = 3;
    public static final int STATUS_OUTPUT_FULL = 4;
    public static final int STATUS_NO_POWER = 5;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_PROCESSING_TICKS = 1;
    public static final int DATA_ENERGY = 2;
    public static final int DATA_ENERGY_CAPACITY = 3;
    public static final int DATA_ENERGY_PER_TICK = 4;
    public static final int DATA_STATUS = 5;
    public static final int DATA_FLUID = 6;
    public static final int DATA_FLUID_CAPACITY = 7;
    public static final int DATA_FLUID_PER_ITEM = 8;
    public static final int DATA_ENERGY_PER_ITEM = 9;
    public static final int DATA_PROCESSING_SPEED = 10;
    public static final int DATA_ENERGY_USAGE = 11;
    public static final int DATA_FLUID_USAGE = 12;
    public static final int DATA_FLUID_TRANSFER = 13;
    public static final int DATA_ENERGY_CAPACITY_STAT = 14;
    public static final int DATA_MACHINE_PROGRESSION_START = 15;
    public static final int DATA_COUNT = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int STAT_SCALE = 100;
    private static final int BUCKET = 1000;
    private static final double NO_BATTERY_PROCESSING_SPEED = 0.80;
    private static final int XP_PER_ITEM = 2;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_INPUT -> canIdentify(stack);
                case SLOT_FLUID_CONTAINER -> stack.is(ModItems.IDENTIFYING_FLUID_BUCKET.get()) || stack.is(Items.BUCKET);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_FLUID_CONTAINER ? 1 : super.getSlotLimit(slot);
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
                case SLOT_FLUID_PUMP -> isFluidPump(stack);
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
            if (slot == SLOT_FLUID_PUMP && level != null) {
                level.invalidateCapabilities(worldPosition);
            }
            if (slot != SLOT_BATTERY_CELL) {
                resetCycle();
                resetBulkSpeed();
            }
            clampInternalEnergy();
            setChanged();
        }
    };
    private final FluidTank tank = new FluidTank(TANK_CAPACITY, IdentifierBlockEntity::isIdentifyingFluid) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IItemHandler inputHandler = new InputItemHandler();
    private final IItemHandler outputHandler = new OutputItemHandler();
    private final IFluidHandler fluidHandler = new PumpFluidHandler();
    private final IEnergyStorage energyStorage = new IdentifierEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final InstalledBatteryCell batteryCell = new InstalledBatteryCell(this::batteryCellStack);
    private final BulkSpeedState bulkSpeed = new BulkSpeedState();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_MACHINE_PROGRESSION_START && index < DATA_COUNT) {
                return MasteryMenuSupport.get(IdentifierBlockEntity.this, index - DATA_MACHINE_PROGRESSION_START, IdentifierBlockEntity.this::effectiveStats);
            }
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_PROGRESS -> syncedProgress(0, progress);
                case DATA_PROCESSING_TICKS -> inputStack().isEmpty() ? 0 : processingTicks(stats);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_ENERGY_PER_TICK -> inputStack().isEmpty() ? 0 : energyCostPerTick(stats);
                case DATA_STATUS -> statusCode(stats);
                case DATA_FLUID -> tank.getFluidAmount();
                case DATA_FLUID_CAPACITY -> tank.getCapacity();
                case DATA_FLUID_PER_ITEM -> fluidCost(stats);
                case DATA_ENERGY_PER_ITEM -> (int) Math.min(Integer.MAX_VALUE, energyCost(stats));
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_FLUID_USAGE -> scaledStat(stats, MachineStat.FLUID_USAGE);
                case DATA_FLUID_TRANSFER -> effectiveFluidTransfer(stats);
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

    private static final int VISUAL_SYNC_INTERVAL = 5;

    private int progress;
    private ItemStack activeInput = ItemStack.EMPTY;
    private int internalEnergy;
    private IdentifierVisualState visualState = IdentifierVisualState.IDLE;
    private IdentifierVisualState.Key lastSyncedVisual;
    private final IdentifierVisualState.Animation animation = new IdentifierVisualState.Animation();

    public IdentifierBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.IDENTIFIER.get(), pos, blockState, MachineType.IDENTIFIER, SLOT_INPUT, SLOT_INPUT, SLOT_OUTPUT);
        trackStatSlots(gearInventory);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IdentifierBlockEntity identifier) {
        identifier.tickWorkOrders(level);
        identifier.drainFluidContainer();
        identifier.tickWork(level, pos, state);
        identifier.syncVisualState(level);
    }

    public IdentifierVisualState visualState() {
        return visualState;
    }

    public IdentifierVisualState.Animation animation() {
        return animation;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return withNoiseFactor(currentVisualState().save(registries));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readNoiseFactor(tag);
        visualState = IdentifierVisualState.load(tag, registries);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        readNoiseFactor(packet.getTag());
        visualState = IdentifierVisualState.load(packet.getTag(), registries);
    }

    private void syncVisualState(Level level) {
        if (level.getGameTime() % VISUAL_SYNC_INTERVAL != 0) {
            return;
        }
        IdentifierVisualState.Key key = currentVisualState().key();
        if (!key.equals(lastSyncedVisual)) {
            lastSyncedVisual = key;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public IdentifierVisualState currentVisualState() {
        ItemStack input = activeInput.isEmpty() ? inputStack() : activeInput;
        boolean working = getBlockState().getValue(BaseMachineBlock.ACTIVE);
        float fill = tank.getCapacity() <= 0 ? 0.0F : tank.getFluidAmount() / (float) tank.getCapacity();
        return new IdentifierVisualState(input.isEmpty() ? ItemStack.EMPTY : input.copyWithCount(1), working,
                working ? processingTicks(effectiveStats()) : 0, fill);
    }

    private void tickWork(Level level, BlockPos pos, BlockState state) {
        MachineStatAccumulator stats = effectiveStats();
        updateTankCapacity(stats);
        restartIfInputSwapped();
        if (statusCode(stats) != STATUS_READY) {
            if (inputStack().isEmpty() || !hasRequiredGear()) {
                resetCycleIfActive();
            }
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }
        if (progress == 0) {
            activeInput = inputStack().copyWithCount(1);
            bulkSpeed.startRecipe(activeInput.getItem());
            if (ProcessingChance.rollInstant(level, stats)) {
                long fullCost = energyCost(stats);
                if (consumeWorkingEnergy(fullCost, true) >= fullCost) {
                    markInstantProcess();
                    consumeWorkingEnergy(fullCost, false);
                    identifyOne(stats);
                    BaseMachineBlock.setActive(level, pos, state, true);
                    setChanged();
                    return;
                }
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
            identifyOne(stats);
        }
        BaseMachineBlock.setActive(level, pos, state, true);
        setChanged();
    }

    /** Rolls one input item into the output slot and pays its Identifying Fluid. */
    private void identifyOne(MachineStatAccumulator stats) {
        if (level == null) {
            return;
        }
        ItemStack input = inputStack();
        int stage = itemStage(input);
        ItemStack identified = CraftedTraitOutputs.rollIfMissing(input.copyWithCount(1), level.random);
        if (CraftedTraitOutputs.isUnidentified(identified) || !canOutput(identified)) {
            resetCycle();
            return;
        }
        tank.drain(fluidCost(stats), IFluidHandler.FluidAction.EXECUTE);
        String itemId = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
        input.shrink(1);
        ItemStack output = processInventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            processInventory.setStackInSlot(SLOT_OUTPUT, identified);
        } else {
            output.grow(1);
            processInventory.setStackInSlot(SLOT_OUTPUT, output);
        }
        bulkSpeed.recordProcess(effectiveTraits());
        grantIdentifyXp(stage);
        workOrderWork(new WorkReport(WorkObjectiveType.IDENTIFY, stage, 0, itemId, 1).withTicks(AffixSorterCosts.identifierTicksPerItem()));
        playIdentifySound(identified);
        resetCycle();
        setChanged();
    }

    /** A chime as each item is read, higher for a rarer roll; a Discreet body plays it quieter. */
    private void playIdentifySound(ItemStack identified) {
        float volume = noiseFactor();
        if (level == null || volume <= 0.0F) {
            return;
        }
        MachineTraits traits = identified.get(ModDataComponents.MACHINE_TRAITS.get());
        int rarity = traits == null ? 0 : traits.rarity().ordinal();
        level.playSound(null, worldPosition, ModSounds.IDENTIFIER_IDENTIFY.get(), SoundSource.BLOCKS, volume, 0.9F + rarity * 0.15F);
    }

    private void grantIdentifyXp(int stage) {
        int band = MachineProgressionState.progressionBand(1 + stage * 3);
        int quarters = MachineProgressionState.xpQuarters(machineProgression().level(), band);
        if (quarters > 0) {
            grantMasteryXp(MachineProgressionState.workXp(XP_PER_ITEM + stage, band), quarters);
        }
    }

    /** A full Identifying Fluid bucket in the container slot empties into the tank and leaves the bucket behind. */
    private void drainFluidContainer() {
        ItemStack container = processInventory.getStackInSlot(SLOT_FLUID_CONTAINER);
        if (!container.is(ModItems.IDENTIFYING_FLUID_BUCKET.get())) {
            return;
        }
        FluidStack bucket = new FluidStack(ModFluids.IDENTIFYING_FLUID_SOURCE.get(), BUCKET);
        if (tank.fill(bucket, IFluidHandler.FluidAction.SIMULATE) == BUCKET) {
            tank.fill(bucket, IFluidHandler.FluidAction.EXECUTE);
            processInventory.setStackInSlot(SLOT_FLUID_CONTAINER, new ItemStack(Items.BUCKET));
        }
    }

    public static boolean isIdentifyingFluid(FluidStack stack) {
        return stack.getFluid().isSame(ModFluids.IDENTIFYING_FLUID_SOURCE.get());
    }

    /** Only items that still have a roll to make; identified, stripped, and trait-less items are refused. */
    public static boolean canIdentify(ItemStack stack) {
        return !stack.isEmpty() && CraftedTraitOutputs.isUnidentified(stack);
    }

    public static boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public static boolean isFluidPump(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.FLUID_PUMP);
    }

    public static boolean isServo(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.SERVO);
    }

    /** The stage that prices an item: its component stage, or Stage 8 when it has none. */
    public static int itemStage(ItemStack stack) {
        return ItemFilterMatcher.componentStage(stack).orElse(STAGE);
    }

    public long energyCost(MachineStatAccumulator stats) {
        return stats.adjustedEnergyCost((int) Math.min(Integer.MAX_VALUE, AffixSorterCosts.identifierFePerItem(itemStage(inputStack()))));
    }

    public int fluidCost(MachineStatAccumulator stats) {
        int base = AffixSorterCosts.identifierFluidPerItem(itemStage(inputStack()));
        return Math.max(1, (int) Math.round(base * Math.max(0.0, stats.value(MachineStat.FLUID_USAGE))));
    }

    private int processingTicks(MachineStatAccumulator stats) {
        return stats.adjustedProcessingTicks(AffixSorterCosts.identifierTicksPerItem());
    }

    private int energyCostPerTick(MachineStatAccumulator stats) {
        return Math.max(1, (int) Math.ceil(energyCost(stats) / (double) processingTicks(stats)));
    }

    public int statusCode(MachineStatAccumulator stats) {
        if (!hasRequiredGear()) {
            return STATUS_MISSING_GEAR;
        }
        ItemStack input = inputStack();
        if (input.isEmpty()) {
            return STATUS_NO_INPUT;
        }
        if (tank.getFluidAmount() < fluidCost(stats)) {
            return STATUS_NO_FLUID;
        }
        if (!canOutputType(input)) {
            return STATUS_OUTPUT_FULL;
        }
        int perTick = energyCostPerTick(stats);
        return consumeWorkingEnergy(perTick, true) < perTick ? STATUS_NO_POWER : STATUS_READY;
    }

    /** The output slot holds identified items; it waits while it holds anything a new roll cannot stack with. */
    private boolean canOutputType(ItemStack input) {
        ItemStack output = processInventory.getStackInSlot(SLOT_OUTPUT);
        return output.isEmpty() || output.getMaxStackSize() > 1 && output.is(input.getItem()) && output.getCount() < output.getMaxStackSize();
    }

    private boolean canOutput(ItemStack identified) {
        ItemStack output = processInventory.getStackInSlot(SLOT_OUTPUT);
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, identified) && output.getCount() < output.getMaxStackSize();
    }

    private boolean hasRequiredGear() {
        return isFluidPump(gearInventory.getStackInSlot(SLOT_FLUID_PUMP)) && isServo(gearInventory.getStackInSlot(SLOT_SERVO));
    }

    private boolean hasBatteryCell() {
        return isBatteryCell(batteryCellStack());
    }

    public MachineStatAccumulator effectiveStats() {
        return cachedStats(bulkSpeed.count());
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.identifier();
        stats.apply(effectiveTraits());
        MegaPassiveTree.applyNodeStats(stats, machineProgression(), MachineMasteryFamily.IDENTIFIER);
        JewelEffects.applyLines(stats, this, machineProgression(), MachineMasteryFamily.IDENTIFIER);
        applyPartStats(stats, gearInventory.getStackInSlot(SLOT_FLUID_PUMP), true);
        applyPartStats(stats, gearInventory.getStackInSlot(SLOT_SERVO), true);
        MegaPassiveTree.applyAttributeStats(stats, machineProgression(), MachineMasteryFamily.IDENTIFIER);
        JewelEffects.applyThresholds(stats, this, machineProgression(), MachineMasteryFamily.IDENTIFIER);
        WorkOrderConditions.apply(stats, workOrders().activeOrder());
        if (!hasBatteryCell()) {
            stats.apply(MachineStatAccumulator.NO_BATTERY_SOURCE, new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.PROCESSING_SPEED,
                    ModifierOperation.LESS,
                    NO_BATTERY_PROCESSING_SPEED
            ));
        }
        bulkSpeed.apply(stats, effectiveTraits());
        return stats;
    }

    private static void applyPartStats(MachineStatAccumulator stats, ItemStack stack, boolean attributes) {
        if (stack.getItem() instanceof MachinePartItem) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
            if (attributes) {
                ComponentBaseStatCatalog.applyAttributeAffixes(stats, stack);
            }
        }
    }

    /** Fluid Capacity, from the body and Fluid Pump rolls, sizes the tank; fluid above a smaller capacity stays until used. */
    private void updateTankCapacity(MachineStatAccumulator stats) {
        int capacity = Math.max(TANK_CAPACITY, (int) Math.round(stats.value(MachineStat.FLUID_CAPACITY)));
        if (tank.getCapacity() != capacity) {
            tank.setCapacity(capacity);
        }
    }

    private int effectiveFluidTransfer(MachineStatAccumulator stats) {
        return isFluidPump(gearInventory.getStackInSlot(SLOT_FLUID_PUMP)) ? Math.max(1, stats.intValue(MachineStat.FLUID_TRANSFER)) : 0;
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        int status = statusCode(stats);
        int energyDemand = status == STATUS_READY || status == STATUS_NO_POWER ? energyCostPerTick(stats) : 0;
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("identifier")
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

    public static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_MISSING_GEAR -> "missing_gear";
            case STATUS_NO_INPUT -> "no_input";
            case STATUS_NO_FLUID -> "no_fluid";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_NO_POWER -> "no_power";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.identifier.status." + name;
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(machineTraits(), Component.translatable("container.rngtech.identifier"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new IdentifierMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        MachineTraits.STREAM_CODEC.encode(buffer, machineTraits());
    }

    public ItemStackHandler getProcessInventory() {
        return processInventory;
    }

    public ItemStackHandler getGearInventory() {
        return gearInventory;
    }

    public FluidTank getTank() {
        return tank;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return trackedEnergyStorage;
    }

    /** With a Fluid Pump installed, Identifying Fluid fills through every face. */
    public IFluidHandler getFluidHandler(Direction side) {
        return isFluidPump(gearInventory.getStackInSlot(SLOT_FLUID_PUMP)) ? fluidHandler : null;
    }

    /** Unidentified items go in from any side but the bottom; identified items come out of the bottom. */
    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == null) {
            return processInventory;
        }
        return side == Direction.DOWN ? outputHandler : inputHandler;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return processInventory;
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        return List.of(FluidPurgeTarget.of(
                PURGE_FLUID_TANK,
                Component.translatable("rngtech.purge.target.identifying_fluid_tank"),
                FluidPurgeRole.INPUT,
                tank::getFluid,
                tank::drain,
                this::resetCycleIfActive,
                this::setChanged
        ).withCapacity(tank::getCapacity));
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

    public boolean unlockPassiveNode(MegaPassiveNode node) {
        return allocateMastery(node);
    }

    @Override
    public boolean mutesMachineSound() {
        return MegaPassiveTree.has(machineProgression(), "MUTE_MACHINE_SOUND");
    }

    @Override
    public MachineMasteryFamily masteryFamily() {
        return MachineMasteryFamily.IDENTIFIER;
    }

    @Override
    public double masteryXpGain() {
        return MachineMasteryHost.xpGain(effectiveStats());
    }

    /** The Identifier has a single Exotic body. */
    @Override
    public int ascendancyEntryStage() {
        return STAGE;
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

    @Override public String workOrderFamily() { return WorkOrderFamilies.of(masteryFamily()); }
    @Override public int workOrderStage() { return STAGE; }
    @Override public int workOrderMasteryLevel() { return masteryState().level(); }
    @Override public String workOrderAscendancy() { return masteryState().ascendancy(); }
    @Override public double workOrderStat(String stat) {
        MachineStat value = MachineStat.bySerializedName(stat);
        return value == null ? 0 : effectiveStats().value(value);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
        tag.put("ActiveInput", activeInput.saveOptional(registries));
        tag.putInt("Energy", internalEnergyStored());
        bulkSpeed.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        progress = tag.getInt("Progress");
        activeInput = ItemStack.parseOptional(registries, tag.getCompound("ActiveInput"));
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        bulkSpeed.load(tag);
        clampInternalEnergy();
    }

    private void resetCycleIfActive() {
        boolean changed = false;
        if (progress != 0 || !activeInput.isEmpty()) {
            resetCycle();
            changed = true;
        }
        changed |= bulkSpeed.reset();
        if (changed) {
            setChanged();
        }
    }

    private void resetCycle() {
        progress = 0;
        activeInput = ItemStack.EMPTY;
    }

    /** A swapped input restarts the cycle; a top-up of the same item keeps it. */
    private void restartIfInputSwapped() {
        if (progress > 0 && !ItemStack.isSameItemSameComponents(activeInput, inputStack())) {
            resetCycle();
            resetBulkSpeed();
            setChanged();
        }
    }

    private void resetBulkSpeed() {
        if (bulkSpeed.reset()) {
            setChanged();
        }
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

    private long consumeWorkingEnergy(long toConsume, boolean simulate) {
        if (toConsume <= 0) {
            return 0;
        }
        int request = (int) Math.min(Integer.MAX_VALUE, toConsume);
        int internal = Math.min(request, internalEnergyStored());
        int remaining = request - internal;
        IEnergyStorage cell = batteryCell.storage();
        int cellExtracted = 0;
        if (remaining > 0 && cell != null && cell.canExtract()) {
            cellExtracted = cell.extractEnergy(remaining, true);
        }
        int consumed = internal + cellExtracted;
        if (simulate || consumed < request) {
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

    private final class OutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(SLOT_OUTPUT);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return processInventory.extractItem(SLOT_OUTPUT, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(SLOT_OUTPUT);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    private final class PumpFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return index == 0 ? tank.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int index) {
            return index == 0 ? tank.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return index == 0 && isIdentifyingFluid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isIdentifyingFluid(resource)) {
                return 0;
            }
            int capped = Math.min(resource.getAmount(), effectiveFluidTransfer(effectiveStats()));
            return capped <= 0 ? 0 : tank.fill(resource.copyWithAmount(capped), action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private final class IdentifierEnergyStorage implements IEnergyStorage {
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
