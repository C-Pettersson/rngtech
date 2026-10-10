package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.SolidFuelBurnerBlock;
import com.rngtech.content.energy.SolidFuelBurnerFuelRules;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.GearParts;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.menu.SolidFuelBurnerMenu;
import com.rngtech.content.registry.ModBlockEntities;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SolidFuelBurnerBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider {
    public static final int SLOT_FUEL_0 = 0;
    public static final int SLOT_FUEL_1 = 1;
    public static final int FUEL_SLOT_COUNT = 2;
    public static final int SLOT_HEAT_CORE = 0;
    public static final int SLOT_BATTERY_CELL = 1;
    public static final int SLOT_FUEL_BOX = 2;
    public static final int GEAR_SLOT_COUNT = 3;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_HEAT_CORE = 1;
    public static final int STATUS_MISSING_BATTERY_CELL = 2;
    public static final int STATUS_MISSING_FUEL_BOX = 3;
    public static final int STATUS_NO_FUEL = 4;
    public static final int STATUS_BLOCKED_TIER = 5;
    public static final int STATUS_BLOCKED_FORM = 6;
    public static final int STATUS_FULL_GOVERNED = 7;
    public static final int STATUS_NOT_BURNABLE = 8;

    /** FE per effective fuel burn tick. Heat Cores set FE/t, so a stronger core burns the same FE faster. */
    public static final int FUEL_ENERGY_PER_BURN_TICK = 10;

    private static final int DATA_BURN_TIME = 0;
    private static final int DATA_TOTAL_BURN_TIME = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_ENERGY_PER_TICK = 4;
    private static final int DATA_MAX_OUTPUT = 5;
    private static final int DATA_ACTIVE_FUEL_SLOTS = 6;
    private static final int DATA_MAX_FUEL_TIER = 7;
    private static final int DATA_FUEL_FORMS = 8;
    private static final int DATA_STATUS = 9;
    private static final int DATA_ENERGY_GENERATION = 10;
    private static final int DATA_ENERGY_TRANSFER = 11;
    private static final int DATA_EFFICIENCY = 12;
    private static final int DATA_FUEL_EFFICIENCY = 13;
    private static final int DATA_HEAT_ISOLATION = 14;
    private static final int DATA_STABILITY = 15;
    private static final int DATA_FUEL_DURATION = 16;
    private static final int DATA_REFINEMENT_POTENTIAL = 17;
    private static final int DATA_LAST_OUTPUT = 18;
    private static final int DATA_CONNECTOR_CAP = 19;
    private static final int DATA_COUNT = 20;
    private static final int STAT_SCALE = 100;

    private final SolidFuelBurnerBlock block;
    private final ItemStackHandler fuelInventory = new ItemStackHandler(FUEL_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 0 && slot < activeFuelSlots() && isAcceptedFuel(stack);
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
                case SLOT_HEAT_CORE -> isHeatCore(stack);
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_FUEL_BOX -> isFuelBox(stack);
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
    private final IItemHandler sidedFuelHandler = new FuelItemHandler();
    private final IEnergyStorage energyStorage = new BurnerEnergyStorage();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_BURN_TIME -> burnTime;
                case DATA_TOTAL_BURN_TIME -> totalBurnTime;
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_ENERGY_PER_TICK -> effectiveGeneration(stats);
                case DATA_MAX_OUTPUT -> effectiveOutputRate();
                case DATA_ACTIVE_FUEL_SLOTS -> activeFuelSlots();
                case DATA_MAX_FUEL_TIER -> maxFuelTier();
                case DATA_FUEL_FORMS -> fuelFormsCode();
                case DATA_STATUS -> statusCode();
                case DATA_ENERGY_GENERATION -> scaledStat(stats, MachineStat.ENERGY_GENERATION);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_FUEL_EFFICIENCY -> scaledStat(stats, MachineStat.FUEL_EFFICIENCY);
                case DATA_HEAT_ISOLATION -> scaledStat(stats, MachineStat.HEAT_ISOLATION);
                case DATA_STABILITY -> scaledStat(stats, MachineStat.STABILITY);
                case DATA_FUEL_DURATION -> scaledStat(stats, MachineStat.FUEL_DURATION);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_LAST_OUTPUT -> lastEnergyOutput();
                case DATA_CONNECTOR_CAP -> connectorOutputCap();
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

    private int burnTime;
    private int totalBurnTime;
    private double remainingFuelEnergy;
    private double generationCarry;
    private double heatWasteCarry;
    private long energyTelemetryTick = Long.MIN_VALUE;
    private int energyOutputThisTick;
    private int lastEnergyOutput;

    public SolidFuelBurnerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SOLID_FUEL_BURNER.get(), pos, blockState, MachineType.SOLID_FUEL_BURNER, SLOT_FUEL_0, SLOT_FUEL_0, SLOT_FUEL_0);
        trackStatSlots(gearInventory);
        if (!(blockState.getBlock() instanceof SolidFuelBurnerBlock burnerBlock)) {
            throw new IllegalStateException("Solid fuel burner block entity created for non-burner block: " + blockState);
        }
        block = burnerBlock;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolidFuelBurnerBlockEntity burner) {
        burner.beginEnergyTelemetryTick();
        boolean generated = burner.tickFuel();
        boolean exported = burner.exportEnergy(level, pos);
        BaseMachineBlock.setActive(level, pos, state, generated);
        if (generated || exported) {
            burner.setChanged();
        }
    }

    public ItemStackHandler getFuelInventory() {
        return fuelInventory;
    }

    public ItemStackHandler getGearInventory() {
        return gearInventory;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public SolidFuelBurnerBlock block() {
        return block;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return energyStorage;
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        int status = statusCode();
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSource(level, worldPosition);
        return MachineInfoSnapshot.builder("solid_fuel_burner")
                .stage(block.chassis().stage())
                .state(machineInfoState(status), burnerBlockedReason(status))
                .energy(energyStored(), energyCapacity(), status == STATUS_READY ? effectiveGeneration(stats) : 0)
                .energyTelemetry(
                        0,
                        lastEnergyOutput(),
                        connector.transferRate(),
                        connector.present() && effectiveGeneration(stats) > connector.transferRate()
                                ? MachineInfoSnapshot.EnergyBottleneck.CONNECTOR_OUTPUT
                                : MachineInfoSnapshot.EnergyBottleneck.NONE
                )
                .fuel(burnTime, totalBurnTime)
                .slots(activeFuelSlots(), FUEL_SLOT_COUNT)
                .gear(burnerGearSummary(status))
                .output(status == STATUS_FULL_GOVERNED
                        ? MachineInfoSnapshot.OutputSummary.ENERGY_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .refinement(machineTraits())
                .build();
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        return sidedFuelHandler;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return fuelInventory;
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(
                machineTraits(),
                Component.translatable("container.rngtech." + block.chassis().blockId())
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SolidFuelBurnerMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        MachineTraits.STREAM_CODEC.encode(buffer, machineTraits());
    }

    public void dropInventory(Level level) {
        for (int slot = 0; slot < fuelInventory.getSlots(); slot++) {
            dropSlot(level, fuelInventory, slot);
        }
        for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
            dropSlot(level, gearInventory, slot);
        }
        dropRefinementInventory(level);
    }

    public boolean isAcceptedFuel(ItemStack stack) {
        return fuelAcceptanceStatus(stack) == STATUS_READY;
    }

    public boolean isHeatCore(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.HEAT_CORE, block.chassis().maxPartStage());
    }

    public boolean isFuelBox(ItemStack stack) {
        return stack.getItem() instanceof SolidFuelBurnerPartItem part
                && part.partType() == MachinePartType.FUEL_BOX
                && part.stage() <= block.chassis().maxPartStage();
    }

    public boolean isBatteryCell(ItemStack stack) {
        if (!(stack.getItem() instanceof BatteryCellItem cell)) {
            return false;
        }
        int stage = cell.material().stage();
        return stage >= 0 && stage <= Math.min(4, block.chassis().maxPartStage());
    }

    public MachineStatAccumulator effectiveStats() {
        return cachedStats(0L);
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.solidFuelBurner(block.chassis());
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
        applyPartStats(stats, gearInventory.getStackInSlot(SLOT_HEAT_CORE));
        applyPartStats(stats, gearInventory.getStackInSlot(SLOT_FUEL_BOX));
        return stats;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FuelInventory", fuelInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.putInt("BurnTime", burnTime);
        tag.putInt("TotalBurnTime", totalBurnTime);
        tag.putDouble("RemainingFuelEnergy", remainingFuelEnergy);
        tag.putDouble("GenerationCarry", generationCarry);
        tag.putDouble("HeatWasteCarry", heatWasteCarry);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuelInventory.deserializeNBT(registries, tag.getCompound("FuelInventory"));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        burnTime = tag.getInt("BurnTime");
        totalBurnTime = tag.getInt("TotalBurnTime");
        remainingFuelEnergy = tag.getDouble("RemainingFuelEnergy");
        generationCarry = tag.getDouble("GenerationCarry");
        heatWasteCarry = tag.getDouble("HeatWasteCarry");
    }

    private boolean tickFuel() {
        if (!hasRequiredComponents()) {
            return false;
        }
        if (fuelGovernorActive() && isBufferFull()) {
            return false;
        }

        MachineStatAccumulator stats = effectiveStats();
        if (!hasBurnTime() && !tryStartBurningFuel(stats)) {
            return false;
        }
        if (!hasBurnTime()) {
            return false;
        }

        int generation = effectiveGeneration(stats);
        burnTime--;
        boolean produced = generateFromActiveFuel(generation);
        applyHeatWaste(stats, generation);
        finishFuelIfSpent();
        return produced || generation > 0;
    }

    private boolean generateFromActiveFuel(int generation) {
        double potential = Math.min(generation, remainingFuelEnergy);
        if (potential <= 0.0) {
            return false;
        }

        int freeSpace = availableCellSpace();
        if (freeSpace <= 0) {
            remainingFuelEnergy = Math.max(0.0, remainingFuelEnergy - potential);
            generationCarry = 0.0;
            return false;
        }

        double rawAccepted = Math.min(potential, freeSpace);
        generationCarry += rawAccepted;
        int rawToStore = (int) Math.floor(generationCarry);
        if (rawToStore <= 0) {
            remainingFuelEnergy = Math.max(0.0, remainingFuelEnergy - rawAccepted);
            return false;
        }

        int stored = storeGeneratedEnergy(rawToStore);
        generationCarry -= rawToStore;
        remainingFuelEnergy = Math.max(0.0, remainingFuelEnergy - rawAccepted);
        return stored > 0;
    }

    private boolean tryStartBurningFuel(MachineStatAccumulator stats) {
        if (level == null || fuelGovernorActive() && isBufferFull()) {
            return false;
        }

        int slot = nextFuelSlot();
        if (slot < 0) {
            totalBurnTime = 0;
            remainingFuelEnergy = 0.0;
            generationCarry = 0.0;
            heatWasteCarry = 0.0;
            return false;
        }

        ItemStack fuel = fuelInventory.getStackInSlot(slot);
        int fuelBurnTime = SolidFuelBurnerFuelRules.burnTicks(fuel);
        if (fuelBurnTime <= 0) {
            return false;
        }

        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (!remainder.isEmpty()) {
            if (fuel.isEmpty()) {
                fuelInventory.setStackInSlot(slot, remainder);
            } else {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder);
            }
        }

        int generation = effectiveGeneration(stats);
        double fuelEnergy = effectiveFuelDurationTicks(fuelBurnTime, stats) * (double) FUEL_ENERGY_PER_BURN_TICK;
        int effectiveFuelBurnTime = generation <= 0 ? 0 : Math.max(1, Mth.ceil(fuelEnergy / generation));
        burnTime = effectiveFuelBurnTime;
        totalBurnTime = effectiveFuelBurnTime;
        remainingFuelEnergy = generation <= 0 ? 0.0 : fuelEnergy;
        generationCarry = 0.0;
        heatWasteCarry = 0.0;
        setChanged();
        return remainingFuelEnergy > 0.0;
    }

    private int effectiveFuelDurationTicks(int baseFuelBurnTime, MachineStatAccumulator stats) {
        double fuelEfficiency = Math.max(0.1, stats.value(MachineStat.FUEL_EFFICIENCY));
        double efficiency = Math.max(0.1, stats.value(MachineStat.EFFICIENCY));
        double fuelDuration = Math.max(0.1, stats.value(MachineStat.FUEL_DURATION));
        return Math.max(1, Mth.ceil(baseFuelBurnTime * fuelEfficiency * efficiency * fuelDuration));
    }

    private int nextFuelSlot() {
        if (hasBehavior(MachineBehavior.FUEL_RESERVE) && acceptedFuelItemCount() <= 1) {
            return -1;
        }
        for (int slot = 0; slot < activeFuelSlots(); slot++) {
            if (isAcceptedFuel(fuelInventory.getStackInSlot(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private int acceptedFuelItemCount() {
        int count = 0;
        for (int slot = 0; slot < activeFuelSlots(); slot++) {
            ItemStack stack = fuelInventory.getStackInSlot(slot);
            if (isAcceptedFuel(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void applyHeatWaste(MachineStatAccumulator stats, int generation) {
        if (burnTime <= 0) {
            heatWasteCarry = 0.0;
            return;
        }

        double heatIsolation = stats.value(MachineStat.HEAT_ISOLATION);
        if (heatIsolation >= 1.0) {
            heatWasteCarry = 0.0;
            return;
        }

        heatWasteCarry += Math.min(1.0, 1.0 - Math.max(0.0, heatIsolation));
        int wasteTicks = (int) Math.floor(heatWasteCarry);
        if (wasteTicks <= 0) {
            return;
        }

        burnTime = Math.max(0, burnTime - wasteTicks);
        remainingFuelEnergy = Math.max(0.0, remainingFuelEnergy - (generation * (double) wasteTicks));
        heatWasteCarry -= wasteTicks;
    }

    private void finishFuelIfSpent() {
        if (burnTime <= 0 || remainingFuelEnergy <= 0.0001) {
            burnTime = 0;
            totalBurnTime = 0;
            remainingFuelEnergy = 0.0;
            generationCarry = 0.0;
            heatWasteCarry = 0.0;
        }
    }

    private int storeGeneratedEnergy(int rawEnergy) {
        ItemStack cell = cellStack();
        if (rawEnergy <= 0 || cell.isEmpty()) {
            return 0;
        }

        int stored = Math.min(rawEnergy, availableCellSpace());
        if (stored > 0) {
            BatteryCellItem.setEnergy(cell, energyStored() + stored, energyCapacity());
            setChanged();
        }
        return stored;
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        if (energyStored() <= 0) {
            return false;
        }

        int remainingOutput = energyStored();
        boolean exported = false;
        for (Direction direction : Direction.values()) {
            if (remainingOutput <= 0 || energyStored() <= 0) {
                return exported;
            }

            IEnergyStorage target = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    pos.relative(direction),
                    direction.getOpposite()
            );
            if (target == null || !target.canReceive()) {
                continue;
            }

            int offered = extractEnergyInternal(remainingOutput, true);
            int received = target.receiveEnergy(offered, false);
            int delivered = extractEnergyInternal(received, false);
            remainingOutput -= delivered;
            exported |= delivered > 0;
        }
        return exported;
    }

    private int extractEnergyInternal(int toExtract, boolean simulate) {
        ItemStack cell = cellStack();
        if (toExtract <= 0 || cell.isEmpty() || energyStored() <= 0) {
            return 0;
        }

        int delivered = Math.min(toExtract, energyStored());
        if (delivered <= 0) {
            return 0;
        }

        if (!simulate) {
            BatteryCellItem.setEnergy(cell, energyStored() - delivered, energyCapacity());
            recordEnergyOutput(delivered);
            setChanged();
        }
        return delivered;
    }

    private boolean hasRequiredComponents() {
        return !heatCoreStack().isEmpty() && !cellStack().isEmpty() && !fuelBoxStack().isEmpty();
    }

    private boolean hasBurnTime() {
        return burnTime > 0 && remainingFuelEnergy > 0.0;
    }

    private boolean fuelGovernorActive() {
        return hasBehavior(MachineBehavior.FUEL_GOVERNOR);
    }

    private boolean isBufferFull() {
        return energyCapacity() > 0 && energyStored() >= energyCapacity();
    }

    private int energyStored() {
        return BatteryCellItem.energyStored(cellStack());
    }

    private int energyCapacity() {
        return BatteryCellItem.energyCapacity(cellStack());
    }

    private int availableCellSpace() {
        return Math.max(0, energyCapacity() - energyStored());
    }

    private int cellInputCeiling() {
        ItemStack cell = cellStack();
        return cell.isEmpty() || BatteryCellItem.energyStored(cell) >= BatteryCellItem.energyCapacity(cell)
                ? 0
                : BatteryCellItem.maxInput(cell);
    }

    private int effectiveOutputRate() {
        return cellStack().isEmpty() ? 0 : energyStored();
    }

    private int effectiveGeneration(MachineStatAccumulator stats) {
        return Math.max(0, (int) Math.round(stats.value(MachineStat.ENERGY_GENERATION)));
    }

    private int connectorOutputCap() {
        return AdjacentEnergyConnector.forSource(level, worldPosition).transferRate();
    }

    private int lastEnergyOutput() {
        beginEnergyTelemetryTick();
        return lastEnergyOutput;
    }

    private void recordEnergyOutput(int amount) {
        if (amount <= 0) {
            return;
        }
        beginEnergyTelemetryTick();
        energyOutputThisTick += amount;
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
        lastEnergyOutput = energyOutputThisTick;
        energyOutputThisTick = 0;
    }

    private int activeFuelSlots() {
        if (fuelBox() == null) {
            return 0;
        }
        return Mth.clamp(effectiveStats().intValue(MachineStat.INPUT_SLOTS), 0, FUEL_SLOT_COUNT);
    }

    private int maxFuelTier() {
        return GearParts.heatCoreFuelTier(heatCoreStack());
    }

    private int fuelFormsCode() {
        SolidFuelBurnerPartItem box = fuelBox();
        if (box == null) {
            return 0;
        }
        int code = 0;
        if (box.acceptsItemFuels()) {
            code |= 1;
        }
        if (box.acceptsBlockFuels()) {
            code |= 2;
        }
        return code;
    }

    private int statusCode() {
        if (heatCoreStack().isEmpty()) {
            return STATUS_MISSING_HEAT_CORE;
        }
        if (cellStack().isEmpty()) {
            return STATUS_MISSING_BATTERY_CELL;
        }
        if (fuelBoxStack().isEmpty()) {
            return STATUS_MISSING_FUEL_BOX;
        }
        if (fuelGovernorActive() && isBufferFull()) {
            return STATUS_FULL_GOVERNED;
        }
        for (int slot = 0; slot < activeFuelSlots(); slot++) {
            ItemStack stack = fuelInventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int status = fuelAcceptanceStatus(stack);
            return status == STATUS_READY ? STATUS_READY : status;
        }
        return hasBurnTime() ? STATUS_READY : STATUS_NO_FUEL;
    }

    private MachineInfoSnapshot.WorkState machineInfoState(int status) {
        if (status == STATUS_READY) {
            return getBlockState().getValue(BaseMachineBlock.ACTIVE) || hasBurnTime()
                    ? MachineInfoSnapshot.WorkState.RUNNING
                    : MachineInfoSnapshot.WorkState.IDLE;
        }
        return MachineInfoSnapshot.WorkState.BLOCKED;
    }

    private MachineInfoSnapshot.BlockedReason burnerBlockedReason(int status) {
        return switch (status) {
            case STATUS_MISSING_HEAT_CORE -> MachineInfoSnapshot.BlockedReason.MISSING_HEAT_CORE;
            case STATUS_MISSING_BATTERY_CELL -> MachineInfoSnapshot.BlockedReason.MISSING_BATTERY_CELL;
            case STATUS_MISSING_FUEL_BOX -> MachineInfoSnapshot.BlockedReason.MISSING_FUEL_BOX;
            case STATUS_NO_FUEL -> MachineInfoSnapshot.BlockedReason.NO_FUEL;
            case STATUS_BLOCKED_TIER -> MachineInfoSnapshot.BlockedReason.BLOCKED_TIER;
            case STATUS_BLOCKED_FORM -> MachineInfoSnapshot.BlockedReason.BLOCKED_FORM;
            case STATUS_FULL_GOVERNED -> MachineInfoSnapshot.BlockedReason.ENERGY_FULL;
            case STATUS_NOT_BURNABLE -> MachineInfoSnapshot.BlockedReason.NOT_BURNABLE;
            default -> MachineInfoSnapshot.BlockedReason.NONE;
        };
    }

    private MachineInfoSnapshot.GearSummary burnerGearSummary(int status) {
        return switch (status) {
            case STATUS_MISSING_HEAT_CORE -> MachineInfoSnapshot.GearSummary.MISSING_HEAT_CORE;
            case STATUS_MISSING_BATTERY_CELL -> MachineInfoSnapshot.GearSummary.MISSING_BATTERY_CELL;
            case STATUS_MISSING_FUEL_BOX -> MachineInfoSnapshot.GearSummary.MISSING_FUEL_BOX;
            default -> MachineInfoSnapshot.GearSummary.BATTERY_CELL_INSTALLED;
        };
    }

    private int fuelAcceptanceStatus(ItemStack stack) {
        if (stack.isEmpty()) {
            return STATUS_NO_FUEL;
        }
        if (SolidFuelBurnerFuelRules.burnTicks(stack) <= 0) {
            return STATUS_NOT_BURNABLE;
        }

        int tier = SolidFuelBurnerFuelRules.tier(stack);
        if (tier <= 0 || tier > maxFuelTier()) {
            return STATUS_BLOCKED_TIER;
        }

        boolean itemFuel = SolidFuelBurnerFuelRules.isItemFuel(stack);
        boolean blockFuel = SolidFuelBurnerFuelRules.isBlockFuel(stack);
        SolidFuelBurnerPartItem box = fuelBox();
        boolean acceptedForm = box != null
                && (itemFuel && box.acceptsItemFuels() || blockFuel && box.acceptsBlockFuels());
        return acceptedForm ? STATUS_READY : STATUS_BLOCKED_FORM;
    }

    private void applyPartStats(MachineStatAccumulator stats, ItemStack stack) {
        if (stack.getItem() instanceof MachinePartItem part && part.machineType() == MachineType.SOLID_FUEL_BURNER) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        }
    }

    private SolidFuelBurnerPartItem fuelBox() {
        ItemStack stack = fuelBoxStack();
        return stack.getItem() instanceof SolidFuelBurnerPartItem part
                && part.partType() == MachinePartType.FUEL_BOX
                ? part
                : null;
    }

    private boolean hasBehavior(MachineBehavior behavior) {
        return MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()).hasBehavior(behavior)
                || MachineImplicitCatalog.hasBehavior(heatCoreStack(), behavior)
                || MachineImplicitCatalog.hasBehavior(fuelBoxStack(), behavior);
    }

    private ItemStack heatCoreStack() {
        return gearInventory.getStackInSlot(SLOT_HEAT_CORE);
    }

    private ItemStack cellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private ItemStack fuelBoxStack() {
        return gearInventory.getStackInSlot(SLOT_FUEL_BOX);
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private final class FuelItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return activeFuelSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return fuelInventory.getStackInSlot(mappedSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return fuelInventory.insertItem(mappedSlot(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return fuelInventory.getSlotLimit(mappedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return fuelInventory.isItemValid(mappedSlot(slot), stack);
        }

        private int mappedSlot(int slot) {
            if (slot < 0 || slot >= activeFuelSlots()) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0," + activeFuelSlots() + ")");
            }
            return slot;
        }
    }

    private final class BurnerEnergyStorage implements IEnergyStorage {
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
            return energyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyCapacity();
        }

        @Override
        public boolean canExtract() {
            return effectiveOutputRate() > 0 && energyStored() > 0;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
