package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.item.AmmoniaPartItem;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.menu.AmmoniaFuelCellMenu;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.AmmoniaPowerCycleRecipe;
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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public class AmmoniaFuelCellBlockEntity extends BaseMachineBlockEntity
        implements MenuProvider, PurgeableFluidStorage, MachineInfoProvider {
    public static final int SLOT_RESIDUE = 0;
    public static final int SLOT_MEMBRANE = 0;
    public static final int SLOT_BATTERY_CELL = 1;
    public static final int GEAR_SLOT_COUNT = 2;
    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_MEMBRANE = 1;
    public static final int STATUS_NO_RECIPE = 2;
    public static final int STATUS_ENERGY_FULL = 3;
    public static final int STATUS_OUTPUT_FULL = 4;
    public static final int STATUS_NO_FLUID = 5;
    public static final int PURGE_AMMONIA_TANK = 0;

    private static final int TANK_CAPACITY = 8000;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_AMMONIA = 4;
    private static final int DATA_TANK_CAPACITY = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_ENERGY_PER_TICK = 7;
    private static final int DATA_ENERGY_GENERATION = 8;
    private static final int DATA_ENERGY_TRANSFER = 9;
    private static final int DATA_EFFICIENCY = 10;
    private static final int DATA_PROCESSING_SPEED = 11;
    private static final int DATA_REFINEMENT_POTENTIAL = 12;
    private static final int DATA_FLAT_ENERGY_GENERATION = 13;
    private static final int DATA_BASE_ENERGY_GENERATION = 14;
    private static final int STAT_SCALE = 100;
    private static final Direction[] ENERGY_OUTPUT_DIRECTIONS = {
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    private final ItemStackHandler processInventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
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
                case SLOT_MEMBRANE -> isMembrane(stack);
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
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
    private final MachineEnergyStorage energyStorage =
            new MachineEnergyStorage(this::internalEnergyCapacity, () -> 0, () -> Integer.MAX_VALUE, this::setChanged);
    private final FluidTank ammoniaTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.is(ModFluids.AMMONIA_SOURCE.get());
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler fluidHandler = new AmmoniaInputHandler();
    private final IItemHandler outputHandler = new OutputItemHandler();
    private final IItemHandler emptyItemHandler = new EmptyItemHandler();
    private final IEnergyStorage energyView = new GeneratorEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyView = energyFlow.track(energyView);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> activeTicks;
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_AMMONIA -> ammoniaTank.getFluidAmount();
                case DATA_TANK_CAPACITY -> TANK_CAPACITY;
                case DATA_STATUS -> statusCode();
                case DATA_ENERGY_PER_TICK -> currentEnergyPerTick();
                case DATA_ENERGY_GENERATION -> (int) Math.round(stats.effectiveEnergyGenerationMultiplier() * STAT_SCALE);
                case DATA_FLAT_ENERGY_GENERATION -> (int) Math.round(stats.effectiveFlatEnergyGenerationBonus() * STAT_SCALE);
                case DATA_BASE_ENERGY_GENERATION -> (int) Math.round(baseEnergyPerTick() * STAT_SCALE);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_BASE_ENERGY_GENERATION + 1;
        }
    };

    private int progress;
    private int activeTicks;
    private long activeEnergy;
    private double energyCarry;

    public AmmoniaFuelCellBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.AMMONIA_FUEL_CELL.get(), pos, blockState, MachineType.AMMONIA_FUEL_CELL, SLOT_RESIDUE, SLOT_RESIDUE, SLOT_RESIDUE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmmoniaFuelCellBlockEntity fuelCell) {
        boolean worked = fuelCell.tickRecipe();
        boolean exported = fuelCell.exportEnergy(level, pos);
        BaseMachineBlock.setActive(level, pos, state, worked);
        if (worked || exported) {
            fuelCell.setChanged();
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

    public IEnergyStorage getEnergyStorage(Direction side) {
        return side == Direction.DOWN ? null : trackedEnergyView;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return fluidHandler;
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        return List.of(FluidPurgeTarget.of(
                PURGE_AMMONIA_TANK,
                Component.translatable("rngtech.purge.target.ammonia_tank"),
                FluidPurgeRole.INPUT,
                ammoniaTank::getFluid,
                ammoniaTank::drain,
                this::clearActiveRecipe,
                this::setChanged
        ).withCapacity(ammoniaTank::getCapacity));
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        int status = statusCode();
        int generation = status == STATUS_READY ? currentEnergyPerTick() : 0;
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSource(level, worldPosition);
        return MachineInfoSnapshot.builder("ammonia_fuel_cell")
                .state(
                        MachineInfoSnapshot.workState(status == STATUS_READY, isActive(), status == STATUS_NO_FLUID),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .progress(progress, activeTicks)
                .energy(energyStored(), energyCapacity(), generation)
                .energyTelemetry(
                        energyFlow.lastInput(),
                        energyFlow.lastOutput(),
                        connector.transferRate(),
                        connector.present() && generation > connector.transferRate()
                                ? MachineInfoSnapshot.EnergyBottleneck.CONNECTOR_OUTPUT
                                : MachineInfoSnapshot.EnergyBottleneck.NONE
                )
                .gear(batteryCellStorage() != null
                        ? MachineInfoSnapshot.GearSummary.BATTERY_CELL_INSTALLED
                        : MachineInfoSnapshot.GearSummary.NONE)
                .output(outputSummary(status))
                .refinement(machineTraits())
                .build();
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_MISSING_MEMBRANE -> "no_membrane";
            case STATUS_NO_RECIPE -> "no_recipe";
            case STATUS_ENERGY_FULL -> "energy_full";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_NO_FLUID -> "no_ammonia";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.ammonia_fuel_cell.status." + name;
    }

    private static MachineInfoSnapshot.OutputSummary outputSummary(int status) {
        return switch (status) {
            case STATUS_OUTPUT_FULL -> MachineInfoSnapshot.OutputSummary.OUTPUT_FULL;
            case STATUS_ENERGY_FULL -> MachineInfoSnapshot.OutputSummary.ENERGY_FULL;
            default -> MachineInfoSnapshot.OutputSummary.NONE;
        };
    }

    private boolean isActive() {
        BlockState state = getBlockState();
        return state.hasProperty(BaseMachineBlock.ACTIVE) && state.getValue(BaseMachineBlock.ACTIVE);
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        return side == Direction.DOWN ? outputHandler : emptyItemHandler;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return processInventory;
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(machineTraits(), Component.translatable("container.rngtech.ammonia_fuel_cell"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AmmoniaFuelCellMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        MachineTraits.STREAM_CODEC.encode(buffer, machineTraits());
    }

    public void dropInventory(Level level) {
        dropSlot(level, processInventory, SLOT_RESIDUE);
        for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
            dropSlot(level, gearInventory, slot);
        }
        dropRefinementInventory(level);
    }

    public boolean isMembrane(ItemStack stack) {
        return stack.getItem() instanceof AmmoniaPartItem part
                && part.partType() == MachinePartType.FUEL_CELL_MEMBRANE
                && part.machineType() == MachineType.AMMONIA_FUEL_CELL;
    }

    public boolean isBatteryCell(ItemStack stack) {
        return stack.getItem() instanceof BatteryCellItem cell && cell.material().stage() >= 6;
    }

    private boolean tickRecipe() {
        AmmoniaPowerCycleRecipe recipe = currentRecipe();
        if (recipe == null || !canRun(recipe)) {
            progress = 0;
            activeTicks = recipe == null ? 0 : recipe.processingTicks();
            activeEnergy = recipe == null ? 0 : recipe.energy();
            return false;
        }
        activeTicks = adjustedTicks(recipe);
        activeEnergy = adjustedEnergy(recipe);
        int generated = generateEnergyThisTick();
        if (storeEnergy(generated, true) < generated) {
            return false;
        }
        storeEnergy(generated, false);
        progress++;
        if (progress >= activeTicks) {
            ammoniaTank.drain(recipe.ammoniaInput().amount(), IFluidHandler.FluidAction.EXECUTE);
            mergeResidue(recipe.residue());
            progress = 0;
            energyCarry = 0.0D;
        }
        return true;
    }

    private AmmoniaPowerCycleRecipe currentRecipe() {
        return AmmoniaRecipes.powerCycle(level, membraneStack(), ammoniaTank.getFluid()).orElse(null);
    }

    private boolean canRun(AmmoniaPowerCycleRecipe recipe) {
        return membraneStage() >= recipe.minimumMembraneStage()
                && ammoniaTank.getFluidAmount() >= recipe.ammoniaInput().amount()
                && canAcceptResidue(recipe.residue());
    }

    private void clearActiveRecipe() {
        progress = 0;
        activeTicks = 0;
        activeEnergy = 0L;
        energyCarry = 0.0D;
    }

    private boolean canAcceptResidue(ItemStack residue) {
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

    private int statusCode() {
        if (!isMembrane(membraneStack())) {
            return STATUS_MISSING_MEMBRANE;
        }
        if (ammoniaTank.isEmpty()) {
            return STATUS_NO_FLUID;
        }
        AmmoniaPowerCycleRecipe recipe = currentRecipe();
        if (recipe == null || membraneStage() < recipe.minimumMembraneStage()) {
            return STATUS_NO_RECIPE;
        }
        if (!canAcceptResidue(recipe.residue())) {
            return STATUS_OUTPUT_FULL;
        }
        int generated = currentEnergyPerTick();
        return generated > 0 && storeEnergy(generated, true) < generated ? STATUS_ENERGY_FULL : STATUS_READY;
    }

    private int currentEnergyPerTick() {
        int ticks = activeTicks;
        long energy = activeEnergy;
        if (ticks <= 0 || energy <= 0) {
            AmmoniaPowerCycleRecipe recipe = currentRecipe();
            if (recipe != null && canRun(recipe)) {
                ticks = adjustedTicks(recipe);
                energy = adjustedEnergy(recipe);
            }
        }
        if (ticks <= 0 || energy <= 0) {
            return 0;
        }
        return Math.max(1, Mth.ceil(energy / (double) ticks));
    }

    private int generateEnergyThisTick() {
        if (activeTicks <= 0 || activeEnergy <= 0) {
            return 0;
        }
        double exact = activeEnergy / (double) activeTicks + energyCarry;
        int whole = Mth.floor(exact);
        energyCarry = exact - whole;
        return Math.max(1, whole);
    }

    private int storeEnergy(int amount, boolean simulate) {
        int remaining = amount;
        int internal = energyStorage.generateEnergy(remaining, simulate);
        remaining -= internal;
        IEnergyStorage cell = batteryCellStorage();
        if (remaining > 0 && cell != null && cell.canReceive()) {
            remaining -= cell.receiveEnergy(remaining, simulate);
        }
        return amount - remaining;
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        int remaining = energyStored();
        boolean moved = false;
        for (Direction direction : ENERGY_OUTPUT_DIRECTIONS) {
            if (remaining <= 0) {
                break;
            }
            IEnergyStorage target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, pos.relative(direction), direction.getOpposite());
            if (target == null || !target.canReceive()) {
                continue;
            }
            int offered = extractEnergyInternal(remaining, true);
            int received = target.receiveEnergy(offered, false);
            int delivered = extractEnergyInternal(received, false);
            energyFlow.recordOutput(delivered);
            remaining -= delivered;
            moved |= delivered > 0;
        }
        return moved;
    }

    private int extractEnergyInternal(int amount, boolean simulate) {
        int remaining = amount;
        int internal = energyStorage.extractEnergy(remaining, simulate);
        remaining -= internal;
        IEnergyStorage cell = batteryCellStorage();
        if (remaining > 0 && cell != null && cell.canExtract()) {
            remaining -= cell.extractEnergy(remaining, simulate);
        }
        return amount - remaining;
    }

    private int energyStored() {
        IEnergyStorage cell = batteryCellStorage();
        return energyStorage.getEnergyStored() + (cell == null ? 0 : cell.getEnergyStored());
    }

    private int energyCapacity() {
        IEnergyStorage cell = batteryCellStorage();
        return energyStorage.getMaxEnergyStored() + (cell == null ? 0 : cell.getMaxEnergyStored());
    }

    private int internalEnergyCapacity() {
        return Math.max(1, Mth.floor(effectiveStats().value(MachineStat.ENERGY_CAPACITY)));
    }

    private IEnergyStorage batteryCellStorage() {
        ItemStack stack = gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
        return stack.getItem() instanceof BatteryCellItem ? BatteryCellItem.energyStorage(stack) : null;
    }

    private ItemStack membraneStack() {
        return gearInventory.getStackInSlot(SLOT_MEMBRANE);
    }

    private int membraneStage() {
        return membraneStack().getItem() instanceof AmmoniaPartItem part ? part.stage() : 0;
    }

    private int adjustedTicks(AmmoniaPowerCycleRecipe recipe) {
        return Math.max(1, Mth.ceil(recipe.processingTicks() / effectiveStats().value(MachineStat.PROCESSING_SPEED)));
    }

    private long adjustedEnergy(AmmoniaPowerCycleRecipe recipe) {
        MachineStatAccumulator stats = effectiveStats();
        int ticks = adjustedTicks(recipe);
        return Math.max(1L, Mth.lfloor(stats.generatedEnergyTotal(recipe.energy(), ticks) * stats.value(MachineStat.EFFICIENCY)));
    }

    private MachineStatAccumulator effectiveStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.ammoniaFuelCell();
        machineTraits().modifiers().forEach(stats::apply);
        ComponentBaseStatCatalog.applyEffectiveContribution(stats, membraneStack());
        return stats;
    }

    private static int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return Mth.floor(stats.value(stat) * STAT_SCALE);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.put("AmmoniaTank", ammoniaTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
        tag.putInt("ActiveTicks", activeTicks);
        tag.putLong("ActiveEnergy", activeEnergy);
        tag.putDouble("EnergyCarry", energyCarry);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        energyStorage.setEnergy(tag.getInt("Energy"));
        ammoniaTank.readFromNBT(registries, tag.getCompound("AmmoniaTank"));
        progress = tag.getInt("Progress");
        activeTicks = tag.getInt("ActiveTicks");
        activeEnergy = tag.getLong("ActiveEnergy");
        energyCarry = tag.getDouble("EnergyCarry");
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private final class AmmoniaInputHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? ammoniaTank.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return TANK_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && ammoniaTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return ammoniaTank.fill(resource, action);
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
            return energyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyCapacity();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

    private final class OutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(SLOT_RESIDUE);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return processInventory.extractItem(SLOT_RESIDUE, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(SLOT_RESIDUE);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    private static final class EmptyItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 0;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    /** Generation before machine and part stats, for the Stats tab breakdown; 0 when unknown. */
    private double baseEnergyPerTick() {
        AmmoniaPowerCycleRecipe recipe = currentRecipe();
        return recipe == null ? 0.0 : recipe.energy() / (double) Math.max(1, recipe.processingTicks());
    }
}
