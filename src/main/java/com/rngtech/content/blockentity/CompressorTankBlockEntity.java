package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.CompressorTankBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.content.menu.CompressorTankMenu;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class CompressorTankBlockEntity extends BaseMachineBlockEntity implements MenuProvider, PurgeableFluidStorage {
    public static final int SLOT_FLUID_INPUT_CONTAINER = 0;
    public static final int SLOT_FLUID_OUTPUT_CONTAINER = 1;
    public static final int PROCESS_SLOT_COUNT = 2;
    public static final int SLOT_BATTERY_CELL = 0;
    public static final int SLOT_SERVO_0 = 1;
    public static final int SLOT_SERVO_1 = 2;
    public static final int SLOT_SERVO_2 = 3;
    public static final int SLOT_SERVO_3 = 4;
    public static final int GEAR_SLOT_COUNT = 5;

    public static final int STATUS_READY = 0;
    public static final int STATUS_PLAIN_TANK = 1;
    public static final int STATUS_NO_FLUID = 2;
    public static final int STATUS_COMPRESSED_FULL = 3;
    public static final int STATUS_MISSING_BATTERY_CELL = 4;
    public static final int STATUS_MISSING_SERVO = 5;
    public static final int STATUS_NO_POWER = 6;
    public static final int PURGE_LOOSE_TANK = 0;
    public static final int PURGE_COMPRESSED_TANK = 1;

    private static final int DATA_LOOSE_FLUID = 0;
    private static final int DATA_LOOSE_FLUID_CAPACITY = 1;
    private static final int DATA_COMPRESSED_PHYSICAL = 2;
    private static final int DATA_COMPRESSED_PHYSICAL_CAPACITY = 3;
    private static final int DATA_COMPRESSED_EQUIVALENT = 4;
    private static final int DATA_COMPRESSED_EQUIVALENT_CAPACITY = 5;
    private static final int DATA_ENERGY = 6;
    private static final int DATA_ENERGY_CAPACITY = 7;
    private static final int DATA_STATUS = 8;
    private static final int DATA_COMPRESSION_RATE = 9;
    private static final int DATA_COMPRESS_FE_PER_BUCKET = 10;
    private static final int DATA_DECOMPRESS_FE_PER_BUCKET = 11;
    private static final int DATA_FLUID_CAPACITY_STAT = 12;
    private static final int DATA_COMPRESSION_RATIO = 13;
    private static final int DATA_PROCESSING_SPEED = 14;
    private static final int DATA_ENERGY_USAGE = 15;
    private static final int DATA_ENERGY_CAPACITY_STAT = 16;
    private static final int DATA_ENERGY_TRANSFER = 17;
    private static final int DATA_FLUID_TRANSFER = 18;
    private static final int DATA_REFINEMENT_POTENTIAL = 19;
    private static final int STAT_SCALE = 100;

    private final CompressorTankBlock block;
    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_FLUID_INPUT_CONTAINER -> isFluidInputContainer(stack);
                case SLOT_FLUID_OUTPUT_CONTAINER -> isFluidOutputContainer(stack);
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
            setChanged();
        }
    };
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_SERVO_0, SLOT_SERVO_1, SLOT_SERVO_2, SLOT_SERVO_3 -> isServo(stack);
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
            clampInternalEnergy();
            setChanged();
        }
    };
    private final FluidTank looseTank;
    private final IItemHandler containerInputHandler = new ProcessItemHandler(
            SLOT_FLUID_INPUT_CONTAINER,
            SLOT_FLUID_OUTPUT_CONTAINER,
            true,
            false
    );
    private final IItemHandler containerOutputHandler = new ProcessItemHandler(
            SLOT_FLUID_OUTPUT_CONTAINER,
            SLOT_FLUID_OUTPUT_CONTAINER,
            false,
            true
    );
    private final IEnergyStorage energyStorage = new CompressorTankEnergyStorage();
    private final IFluidHandler fluidHandler = new OrdinaryFluidHandler();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_LOOSE_FLUID -> looseTank.getFluidAmount();
                case DATA_LOOSE_FLUID_CAPACITY -> looseTank.getCapacity();
                case DATA_COMPRESSED_PHYSICAL -> compressedPhysicalAmount();
                case DATA_COMPRESSED_PHYSICAL_CAPACITY -> compressedPhysicalCapacity();
                case DATA_COMPRESSED_EQUIVALENT -> compressedFluid.getAmount();
                case DATA_COMPRESSED_EQUIVALENT_CAPACITY -> compressedEquivalentCapacity();
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_STATUS -> statusCode(stats);
                case DATA_COMPRESSION_RATE -> effectiveCompressionRate(stats);
                case DATA_COMPRESS_FE_PER_BUCKET -> adjustedCompressFePerBucket(stats);
                case DATA_DECOMPRESS_FE_PER_BUCKET -> adjustedDecompressFePerBucket(stats);
                case DATA_FLUID_CAPACITY_STAT -> scaledStat(stats, MachineStat.FLUID_CAPACITY);
                case DATA_COMPRESSION_RATIO -> scaledStat(stats, MachineStat.COMPRESSION_RATIO);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_FLUID_TRANSFER -> scaledStat(stats, MachineStat.FLUID_TRANSFER);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_REFINEMENT_POTENTIAL + 1;
        }
    };

    private FluidStack compressedFluid = FluidStack.EMPTY;
    private int internalEnergy;
    private boolean workedThisTick;

    public CompressorTankBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COMPRESSOR_TANK.get(), pos, blockState, MachineType.COMPRESSOR_TANK, 0, 0, 0);
        if (!(blockState.getBlock() instanceof CompressorTankBlock tankBlock)) {
            throw new IllegalStateException("Compressor tank block entity created for non-tank block: " + blockState);
        }
        block = tankBlock;
        looseTank = new FluidTank(looseCapacity()) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return canAcceptFluid(stack);
            }

            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CompressorTankBlockEntity tank) {
        tank.workedThisTick = false;
        tank.drainInputContainer();
        tank.fillOutputContainer();
        if (tank.supportsCompression()) {
            tank.workedThisTick |= tank.compressLooseFluid();
        }
        BaseMachineBlock.setActive(level, pos, state, tank.workedThisTick);
        if (tank.workedThisTick) {
            tank.setChanged();
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

    public FluidTank getLooseTank() {
        return looseTank;
    }

    public FluidStack getCompressedFluid() {
        return compressedFluid.copy();
    }

    public CompressorTankMaterial material() {
        return block.material();
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return supportsCompression() ? energyStorage : null;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return fluidHandler;
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        List<FluidPurgeTarget> targets = new ArrayList<>();
        targets.add(FluidPurgeTarget.of(
                PURGE_LOOSE_TANK,
                Component.translatable("rngtech.purge.target.loose_tank"),
                FluidPurgeRole.STORAGE,
                looseTank::getFluid,
                looseTank::drain,
                this::setChanged
        ));
        if (supportsCompression()) {
            targets.add(FluidPurgeTarget.of(
                    PURGE_COMPRESSED_TANK,
                    Component.translatable("rngtech.purge.target.compressed_tank"),
                    FluidPurgeRole.OUTPUT,
                    this::getCompressedFluid,
                    this::purgeCompressedFluid,
                    this::setChanged
            ));
        }
        return List.copyOf(targets);
    }

    @Override
    public MachineType refinementMachineType() {
        return supportsCompression() ? MachineType.COMPRESSOR_TANK : MachineType.FLUID_TANK;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == Direction.DOWN) {
            return containerOutputHandler;
        }
        if (side != null) {
            return containerInputHandler;
        }
        return processInventory;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return processInventory;
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(
                machineTraits(),
                Component.translatable("container.rngtech." + material().blockId())
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CompressorTankMenu(containerId, playerInventory, this, menuData);
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
        dropRefinementInventory(level);
    }

    public MachineStatAccumulator effectiveStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.compressorTank(material());
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
        applyAverageServoStats(stats);
        return stats;
    }

    public boolean isBatteryCell(ItemStack stack) {
        return supportsCompression() && stack.getItem() instanceof BatteryCellItem cell && cell.material().stage() <= material().stage();
    }

    public boolean isServo(ItemStack stack) {
        return supportsCompression() && stack.getItem() instanceof ServoItem servo && servo.stage() <= material().stage();
    }

    public static boolean isFluidInputContainer(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).isPresent();
    }

    public static boolean isFluidOutputContainer(ItemStack stack) {
        return !stack.isEmpty() && FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.put("LooseTank", looseTank.writeToNBT(registries, new CompoundTag()));
        tag.put("CompressedFluid", compressedFluid.saveOptional(registries));
        tag.putInt("Energy", internalEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        looseTank.readFromNBT(registries, tag.getCompound("LooseTank"));
        compressedFluid = FluidStack.parseOptional(registries, tag.getCompound("CompressedFluid"));
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        normalizeCompressedFluid();
        clampInternalEnergy();
    }

    private boolean compressLooseFluid() {
        MachineStatAccumulator stats = effectiveStats();
        if (!canCompress(stats) || looseTank.isEmpty() || compressedRemaining() <= 0) {
            return false;
        }
        FluidStack loose = looseTank.getFluid();
        if (!compressedFluid.isEmpty() && !FluidStack.isSameFluidSameComponents(loose, compressedFluid)) {
            return false;
        }

        int requested = Math.min(loose.getAmount(), Math.min(compressedRemaining(), effectiveCompressionRate(stats)));
        int amount = workAmountForEnergy(requested, adjustedCompressFePerBucket(stats));
        if (amount <= 0) {
            return false;
        }

        FluidStack drained = looseTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return false;
        }
        consumeWorkingEnergy(energyCost(drained.getAmount(), adjustedCompressFePerBucket(stats)), false);
        addCompressedFluid(drained);
        return true;
    }

    private boolean canCompress(MachineStatAccumulator stats) {
        return supportsCompression() && hasBatteryCell() && validServoCount() > 0 && effectiveCompressionRate(stats) > 0;
    }

    public boolean supportsCompression() {
        return material().supportsCompression();
    }

    private int statusCode(MachineStatAccumulator stats) {
        if (!supportsCompression()) {
            return STATUS_PLAIN_TANK;
        }
        if (!hasBatteryCell()) {
            return STATUS_MISSING_BATTERY_CELL;
        }
        if (validServoCount() <= 0) {
            return STATUS_MISSING_SERVO;
        }
        if (looseTank.isEmpty()) {
            return compressedFluid.isEmpty() ? STATUS_NO_FLUID : STATUS_READY;
        }
        if (compressedRemaining() <= 0) {
            return STATUS_COMPRESSED_FULL;
        }
        if (workAmountForEnergy(Math.min(looseTank.getFluidAmount(), effectiveCompressionRate(stats)), adjustedCompressFePerBucket(stats)) <= 0) {
            return STATUS_NO_POWER;
        }
        return STATUS_READY;
    }

    private boolean canAcceptFluid(FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!looseTank.isEmpty() && !FluidStack.isSameFluidSameComponents(stack, looseTank.getFluid())) {
            return false;
        }
        return compressedFluid.isEmpty() || FluidStack.isSameFluidSameComponents(stack, compressedFluid);
    }

    private int drainCompressed(int maxDrain, FluidStack requested, IFluidHandler.FluidAction action) {
        if (maxDrain <= 0 || compressedFluid.isEmpty()) {
            return 0;
        }
        if (!requested.isEmpty() && !FluidStack.isSameFluidSameComponents(requested, compressedFluid)) {
            return 0;
        }
        MachineStatAccumulator stats = effectiveStats();
        if (!canCompress(stats)) {
            return 0;
        }
        int requestedAmount = Math.min(maxDrain, Math.min(compressedFluid.getAmount(), effectiveCompressionRate(stats)));
        int amount = workAmountForEnergy(requestedAmount, adjustedDecompressFePerBucket(stats));
        if (amount <= 0) {
            return 0;
        }
        if (action.execute()) {
            consumeWorkingEnergy(energyCost(amount, adjustedDecompressFePerBucket(stats)), false);
            removeCompressedFluid(amount);
            workedThisTick = true;
            setChanged();
        }
        return amount;
    }

    private int effectiveCompressionRate(MachineStatAccumulator stats) {
        if (!supportsCompression() || validServoCount() <= 0) {
            return 0;
        }
        double rate = stats.value(MachineStat.FLUID_TRANSFER) * stats.value(MachineStat.PROCESSING_SPEED) * servoCountMultiplier();
        return Math.max(0, (int) Math.round(rate));
    }

    private double servoCountMultiplier() {
        return switch (validServoCount()) {
            case 1 -> 1.0D;
            case 2 -> 1.75D;
            case 3 -> 2.5D;
            case 4 -> 3.5D;
            default -> 0.0D;
        };
    }

    private int adjustedCompressFePerBucket(MachineStatAccumulator stats) {
        if (material().compressFePerBucket() <= 0) {
            return 0;
        }
        return stats.adjustedEnergyCost(material().compressFePerBucket());
    }

    private int adjustedDecompressFePerBucket(MachineStatAccumulator stats) {
        if (material().decompressFePerBucket() <= 0) {
            return 0;
        }
        return stats.adjustedEnergyCost(material().decompressFePerBucket());
    }

    private int workAmountForEnergy(int requested, int fePerBucket) {
        if (requested <= 0) {
            return 0;
        }
        if (fePerBucket <= 0) {
            return requested;
        }
        return Math.min(requested, (int) Math.floor(workingEnergyAvailable() * (double) FluidType.BUCKET_VOLUME / fePerBucket));
    }

    private int energyCost(int amount, int fePerBucket) {
        if (amount <= 0 || fePerBucket <= 0) {
            return 0;
        }
        return (int) Math.ceil(amount * (double) fePerBucket / FluidType.BUCKET_VOLUME);
    }

    private void addCompressedFluid(FluidStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (compressedFluid.isEmpty()) {
            compressedFluid = stack.copy();
        } else if (FluidStack.isSameFluidSameComponents(compressedFluid, stack)) {
            compressedFluid.setAmount(Math.min(compressedEquivalentCapacity(), compressedFluid.getAmount() + stack.getAmount()));
        }
        normalizeCompressedFluid();
    }

    private void removeCompressedFluid(int amount) {
        if (amount <= 0 || compressedFluid.isEmpty()) {
            return;
        }
        compressedFluid.setAmount(Math.max(0, compressedFluid.getAmount() - amount));
        normalizeCompressedFluid();
    }

    private FluidStack purgeCompressedFluid(int amount, IFluidHandler.FluidAction action) {
        if (amount <= 0 || compressedFluid.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int drained = Math.min(amount, compressedFluid.getAmount());
        FluidStack result = compressedFluid.copyWithAmount(drained);
        if (action.execute()) {
            removeCompressedFluid(drained);
            setChanged();
        }
        return result;
    }

    private void normalizeCompressedFluid() {
        if (compressedFluid.isEmpty() || compressedFluid.getAmount() <= 0) {
            compressedFluid = FluidStack.EMPTY;
        } else if (compressedFluid.getAmount() > compressedEquivalentCapacity()) {
            compressedFluid.setAmount(compressedEquivalentCapacity());
        }
    }

    private int compressedRemaining() {
        return Math.max(0, compressedEquivalentCapacity() - compressedFluid.getAmount());
    }

    private int looseCapacity() {
        return material().looseBuckets() * FluidType.BUCKET_VOLUME;
    }

    private int compressedPhysicalCapacity() {
        return material().compressedPhysicalBuckets() * FluidType.BUCKET_VOLUME;
    }

    private int compressedEquivalentCapacity() {
        return compressedPhysicalCapacity() * material().compressionRatio();
    }

    private int compressedPhysicalAmount() {
        if (compressedFluid.isEmpty() || material().compressionRatio() <= 0) {
            return 0;
        }
        return (int) Math.ceil(compressedFluid.getAmount() / (double) material().compressionRatio());
    }

    private void drainInputContainer() {
        ItemStack stack = processInventory.getStackInSlot(SLOT_FLUID_INPUT_CONTAINER);
        if (stack.isEmpty()) {
            return;
        }
        FluidActionResult result = FluidUtil.tryEmptyContainer(stack, looseTank, Integer.MAX_VALUE, null, true);
        if (result.isSuccess()) {
            processInventory.setStackInSlot(SLOT_FLUID_INPUT_CONTAINER, result.getResult());
            setChanged();
        }
    }

    private boolean fillOutputContainer() {
        ItemStack stack = processInventory.getStackInSlot(SLOT_FLUID_OUTPUT_CONTAINER);
        if (stack.isEmpty()) {
            return false;
        }
        int compressedBefore = compressedFluid.isEmpty() ? 0 : compressedFluid.getAmount();
        FluidActionResult result = FluidUtil.tryFillContainer(stack, fluidHandler, Integer.MAX_VALUE, null, true);
        if (!result.isSuccess()) {
            return false;
        }
        processInventory.setStackInSlot(SLOT_FLUID_OUTPUT_CONTAINER, result.getResult());
        setChanged();
        return compressedBefore > (compressedFluid.isEmpty() ? 0 : compressedFluid.getAmount());
    }

    private boolean hasBatteryCell() {
        return isBatteryCell(batteryCellStack());
    }

    private int validServoCount() {
        int count = 0;
        for (int slot = SLOT_SERVO_0; slot <= SLOT_SERVO_3; slot++) {
            if (isServo(gearInventory.getStackInSlot(slot))) {
                count++;
            }
        }
        return count;
    }

    private void applyAverageServoStats(MachineStatAccumulator stats) {
        int count = 0;
        double processingSpeed = 0.0D;
        double energyUsage = 0.0D;
        double stability = 0.0D;
        int refinementPotential = 0;
        for (int slot = SLOT_SERVO_0; slot <= SLOT_SERVO_3; slot++) {
            ItemStack stack = gearInventory.getStackInSlot(slot);
            if (!isServo(stack)) {
                continue;
            }
            MachineStatAccumulator servoStats = ComponentBaseStatCatalog.effectiveStats(stack);
            if (servoStats == null) {
                continue;
            }
            count++;
            processingSpeed += servoStats.value(MachineStat.PROCESSING_SPEED);
            energyUsage += servoStats.value(MachineStat.ENERGY_USAGE);
            stability += servoStats.value(MachineStat.STABILITY);
            refinementPotential += stack.getItem() instanceof ServoItem servo ? servo.material().refinementPotential() : 0;
        }
        if (count <= 0) {
            return;
        }
        stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.PROCESSING_SPEED, ModifierOperation.MORE, processingSpeed / count));
        stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.ENERGY_USAGE, ModifierOperation.MORE, energyUsage / count));
        stats.apply(new MachineModifier(ModifierSlot.IMPLICIT, MachineStat.STABILITY, ModifierOperation.MORE, stability / count));
        if (refinementPotential > 0) {
            stats.apply(new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.REFINEMENT_POTENTIAL,
                    ModifierOperation.ADD,
                    refinementPotential
            ));
        }
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
    }

    private int energyStored() {
        if (!supportsCompression()) {
            return 0;
        }
        return internalEnergyStored() + BatteryCellItem.energyStored(batteryCellStack());
    }

    private int energyCapacity() {
        if (!supportsCompression()) {
            return 0;
        }
        return internalEnergyCapacity() + BatteryCellItem.energyCapacity(batteryCellStack());
    }

    private int internalEnergyCapacity() {
        if (!supportsCompression()) {
            return 0;
        }
        return Math.max(1, (int) Math.round(effectiveStats().value(MachineStat.ENERGY_CAPACITY)));
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

    private int workingEnergyAvailable() {
        return consumeWorkingEnergy(Integer.MAX_VALUE, true);
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

    private int effectiveMaxEnergyInput() {
        if (!supportsCompression()) {
            return 0;
        }
        return Math.max(0, (int) Math.round(effectiveStats().value(MachineStat.ENERGY_TRANSFER)));
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private IEnergyStorage batteryCellEnergyStorage() {
        if (!supportsCompression()) {
            return null;
        }
        ItemStack cell = batteryCellStack();
        return cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private final class ProcessItemHandler implements IItemHandler {
        private final int firstSlot;
        private final int lastSlot;
        private final boolean allowInsert;
        private final boolean allowExtract;

        private ProcessItemHandler(int firstSlot, int lastSlot, boolean allowInsert, boolean allowExtract) {
            this.firstSlot = firstSlot;
            this.lastSlot = lastSlot;
            this.allowInsert = allowInsert;
            this.allowExtract = allowExtract;
        }

        @Override
        public int getSlots() {
            return lastSlot - firstSlot + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(mappedSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!allowInsert) {
                return stack;
            }
            return processInventory.insertItem(mappedSlot(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!allowExtract) {
                return ItemStack.EMPTY;
            }
            return processInventory.extractItem(mappedSlot(slot), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(mappedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return allowInsert && processInventory.isItemValid(mappedSlot(slot), stack);
        }

        private int mappedSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
            }
            return firstSlot + slot;
        }
    }

    private final class OrdinaryFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0) {
                return FluidStack.EMPTY;
            }
            return looseTank.isEmpty() ? compressedFluid.copy() : looseTank.getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? looseTank.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && canAcceptFluid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !canAcceptFluid(resource)) {
                return 0;
            }
            return looseTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            int drained = drainMatching(resource, resource.getAmount(), action);
            return drained <= 0 ? FluidStack.EMPTY : resource.copyWithAmount(drained);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            FluidStack template = !looseTank.isEmpty() ? looseTank.getFluid() : compressedFluid;
            if (template.isEmpty()) {
                return FluidStack.EMPTY;
            }
            int drained = drainMatching(template, maxDrain, action);
            return drained <= 0 ? FluidStack.EMPTY : template.copyWithAmount(drained);
        }

        private int drainMatching(FluidStack template, int maxDrain, FluidAction action) {
            int drained = 0;
            if (!looseTank.isEmpty() && FluidStack.isSameFluidSameComponents(template, looseTank.getFluid())) {
                FluidStack looseDrained = looseTank.drain(maxDrain, action);
                drained += looseDrained.getAmount();
            }
            int remaining = maxDrain - drained;
            if (remaining > 0) {
                drained += drainCompressed(remaining, template, action);
            }
            return drained;
        }
    }

    private final class CompressorTankEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (!canReceive() || toReceive <= 0) {
                return 0;
            }

            int remaining = Math.min(toReceive, effectiveMaxEnergyInput());
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
            if (effectiveMaxEnergyInput() <= 0) {
                return false;
            }
            if (internalEnergyStored() < internalEnergyCapacity()) {
                return true;
            }
            IEnergyStorage cell = batteryCellEnergyStorage();
            return cell != null && cell.canReceive() && cell.getEnergyStored() < cell.getMaxEnergyStored();
        }
    }
}
