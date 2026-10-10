package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.CathodeItem;
import com.rngtech.content.item.GearParts;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.menu.CorrosionCellMenu;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.CorrosionCellRecipe;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModTags;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
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
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public class CorrosionCellBlockEntity extends BaseMachineBlockEntity
        implements MenuProvider, PurgeableFluidStorage, MachineInfoProvider {
    public static final int SLOT_PLATE = 0;
    public static final int SLOT_ELECTROLYTE = 1;
    public static final int SLOT_RESIDUE = 2;
    public static final int PROCESS_SLOT_COUNT = 3;
    public static final int SLOT_BATTERY_CELL = 0;
    public static final int SLOT_FLUID_PUMP = 1;
    public static final int SLOT_CATHODE = 2;
    public static final int GEAR_SLOT_COUNT = 3;

    public static final int STATUS_READY = 0;
    public static final int STATUS_NO_PLATE = 1;
    public static final int STATUS_NO_ELECTROLYTE = 2;
    public static final int STATUS_INVALID_RECIPE = 3;
    public static final int STATUS_BLOCKED_STAGE = 4;
    public static final int STATUS_OUTPUT_FULL = 5;
    public static final int STATUS_ENERGY_FULL = 6;
    public static final int STATUS_REDSTONE_DISABLED = 7;
    public static final int STATUS_BLOCKED_CATHODE = 8;
    public static final int PURGE_ELECTROLYTE_TANK = 0;
    public static final int ELECTROLYTE_PER_REAGENT = BatteryAssemblerBlockEntity.ELECTROLYTE_PER_REAGENT;

    private static final int TANK_CAPACITY = FluidType.BUCKET_VOLUME * 4;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_ENERGY_PER_TICK = 4;
    private static final int DATA_MAX_OUTPUT = 5;
    private static final int DATA_RECIPE_ENERGY = 6;
    private static final int DATA_MINIMUM_STAGE = 7;
    private static final int DATA_STATUS = 8;
    private static final int DATA_ELECTROLYTE_FLUID = 9;
    private static final int DATA_ELECTROLYTE_FLUID_CAPACITY = 10;
    private static final int DATA_FLUID_TRANSFER = 11;
    private static final int DATA_ENERGY_GENERATION = 12;
    private static final int DATA_ENERGY_CAPACITY_STAT = 13;
    private static final int DATA_ENERGY_TRANSFER = 14;
    private static final int DATA_EFFICIENCY = 15;
    private static final int DATA_PROCESSING_SPEED = 16;
    private static final int DATA_STABILITY = 17;
    private static final int DATA_REFINEMENT_POTENTIAL = 18;
    private static final int DATA_FLAT_ENERGY_GENERATION = 19;
    private static final int DATA_BASE_ENERGY_GENERATION = 20;
    private static final int DATA_ELECTROLYTE_FLUID_ID = 21;
    private static final int STAT_SCALE = 100;
    private static final int COMPONENT_STAGE = 4;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_PLATE -> isKnownPlateInput(stack);
                case SLOT_ELECTROLYTE -> isKnownElectrolyteInput(stack);
                default -> false;
            };
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
            if (slot == SLOT_PLATE || slot == SLOT_ELECTROLYTE) {
                resetBulkSpeed();
            }
            setChanged();
        }
    };
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_FLUID_PUMP -> isFluidPump(stack);
                case SLOT_CATHODE -> isCathode(stack);
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
            clampInternalEnergy();
            setChanged();
        }
    };
    private final FluidTank electrolyteTank = new FluidTank(TANK_CAPACITY, CorrosionCellBlockEntity::isValidElectrolyte) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IItemHandler plateHandler = new SingleSlotInsertHandler(SLOT_PLATE);
    private final IItemHandler sideInputHandler = new SideInputItemHandler();
    private final IItemHandler residueHandler = new ResidueItemHandler();
    private final IEnergyStorage energyStorage = new CellEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final IFluidHandler fluidHandler = new ElectrolyteFluidHandler();
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
                case DATA_MINIMUM_STAGE -> currentMinimumStage();
                case DATA_STATUS -> statusCode(stats);
                case DATA_ELECTROLYTE_FLUID -> electrolyteTank.getFluidAmount();
                case DATA_ELECTROLYTE_FLUID_CAPACITY -> electrolyteTank.getCapacity();
                case DATA_FLUID_TRANSFER -> effectiveFluidTransfer(stats);
                case DATA_ENERGY_GENERATION -> (int) Math.round(stats.effectiveEnergyGenerationMultiplier() * STAT_SCALE);
                case DATA_FLAT_ENERGY_GENERATION -> (int) Math.round(stats.effectiveFlatEnergyGenerationBonus() * STAT_SCALE);
                case DATA_BASE_ENERGY_GENERATION -> (int) Math.round(baseEnergyPerTick() * STAT_SCALE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_STABILITY -> scaledStat(stats, MachineStat.STABILITY);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_ELECTROLYTE_FLUID_ID -> BuiltInRegistries.FLUID.getId(electrolyteTank.getFluid().getFluid());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_ELECTROLYTE_FLUID_ID + 1;
        }
    };

    private final BulkSpeedState bulkSpeed = new BulkSpeedState();
    private int progress;
    private int activeProcessingTicks;
    private int internalEnergy;
    private double activeTotalEnergy;
    private double remainingEnergy;
    private double generationCarry;
    private ItemStack pendingResidue = ItemStack.EMPTY;

    public CorrosionCellBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CORROSION_CELL.get(), pos, blockState, MachineType.CORROSION_CELL, SLOT_PLATE, SLOT_ELECTROLYTE, SLOT_RESIDUE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CorrosionCellBlockEntity cell) {
        boolean generated = cell.tickCell();
        boolean exported = cell.exportEnergy(level, pos);
        BaseMachineBlock.setActive(level, pos, state, cell.isWorking());
        if (generated || exported) {
            cell.setChanged();
        }
    }

    public ItemStackHandler getProcessInventory() {
        return processInventory;
    }

    public ItemStackHandler getGearInventory() {
        return gearInventory;
    }

    public FluidTank getElectrolyteTank() {
        return electrolyteTank;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return side == Direction.UP || side == Direction.DOWN ? null : trackedEnergyStorage;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return side != null && side.getAxis().isHorizontal() && hasFluidPump() ? fluidHandler : null;
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        return List.of(FluidPurgeTarget.of(
                PURGE_ELECTROLYTE_TANK,
                Component.translatable("rngtech.purge.target.electrolyte_tank"),
                FluidPurgeRole.INPUT,
                electrolyteTank::getFluid,
                electrolyteTank::drain,
                this::resetInactiveProgress,
                this::setChanged
        ).withCapacity(electrolyteTank::getCapacity));
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        CorrosionCellRecipe recipe = nextRecipe();
        int status = statusCode(stats);
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSource(level, worldPosition);
        return MachineInfoSnapshot.builder("corrosion_cell")
                .stage(COMPONENT_STAGE)
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY,
                                getBlockState().getValue(BaseMachineBlock.ACTIVE),
                                status == STATUS_NO_PLATE || status == STATUS_NO_ELECTROLYTE
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status), recipe == null ? 0 : recipe.minimumMaterialStage())
                .progress(progress, currentProcessingTicks(stats))
                .energy(energyStored(), energyCapacity(stats), isWorking() ? currentEnergyPerTick(stats) : 0)
                .energyTelemetry(
                        energyFlow.lastInput(),
                        energyFlow.lastOutput(),
                        connector.transferRate(),
                        AdjacentEnergyConnector.outputBottleneck(connector, isWorking() ? currentEnergyPerTick(stats) : 0)
                )
                .processingLevel(
                        COMPONENT_STAGE,
                        recipe == null ? MachineInfoSnapshot.UNSET : recipe.minimumMaterialStage()
                )
                .output(outputSummary(status))
                .refinement(machineTraits())
                .build();
    }

    private static MachineInfoSnapshot.OutputSummary outputSummary(int status) {
        return switch (status) {
            case STATUS_OUTPUT_FULL -> MachineInfoSnapshot.OutputSummary.OUTPUT_FULL;
            case STATUS_ENERGY_FULL -> MachineInfoSnapshot.OutputSummary.ENERGY_FULL;
            default -> MachineInfoSnapshot.OutputSummary.NONE;
        };
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_NO_PLATE -> "no_plate";
            case STATUS_NO_ELECTROLYTE -> "no_electrolyte";
            case STATUS_INVALID_RECIPE -> "invalid_recipe";
            case STATUS_BLOCKED_STAGE -> "blocked_stage";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_ENERGY_FULL -> "energy_full";
            case STATUS_REDSTONE_DISABLED -> "redstone";
            case STATUS_BLOCKED_CATHODE -> "blocked_cathode";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.corrosion_cell.status." + name;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == Direction.UP) {
            return plateHandler;
        }
        if (side == Direction.DOWN) {
            return residueHandler;
        }
        if (side != null) {
            return sideInputHandler;
        }
        return processInventory;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return processInventory;
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(machineTraits(), Component.translatable("container.rngtech.corrosion_cell"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CorrosionCellMenu(containerId, playerInventory, this, menuData);
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

    public boolean isKnownPlateInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return level == null || CorrosionCellRecipes.isPlateInput(level, stack);
    }

    public boolean isKnownElectrolyteInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return level == null || CorrosionCellRecipes.isElectrolyteInput(level, stack);
    }

    public boolean isBatteryCell(ItemStack stack) {
        return stack.getItem() instanceof BatteryCellItem cell && cell.material().stage() <= 5;
    }

    public static boolean isFluidPump(ItemStack stack) {
        return GearParts.is(stack, MachinePartType.FLUID_PUMP);
    }

    public static boolean isCathode(ItemStack stack) {
        return stack.getItem() instanceof CathodeItem;
    }

    public MachineStatAccumulator effectiveStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.corrosionCell();
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
        applyFluidPumpStats(stats);
        if (isCathode(cathodeStack())) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, cathodeStack());
        }
        bulkSpeed.apply(stats, activeTraits());
        return stats;
    }

    private MachineTraits activeTraits() {
        return MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.put("ElectrolyteTank", electrolyteTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
        tag.putInt("ActiveProcessingTicks", activeProcessingTicks);
        tag.putInt("Energy", internalEnergy);
        tag.putDouble("ActiveTotalEnergy", activeTotalEnergy);
        tag.putDouble("RemainingEnergy", remainingEnergy);
        tag.putDouble("GenerationCarry", generationCarry);
        tag.put("PendingResidue", pendingResidue.saveOptional(registries));
        bulkSpeed.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        loadGearInventory(tag.getCompound("GearInventory"), registries);
        electrolyteTank.readFromNBT(registries, tag.getCompound("ElectrolyteTank"));
        progress = tag.getInt("Progress");
        activeProcessingTicks = tag.getInt("ActiveProcessingTicks");
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        activeTotalEnergy = tag.getDouble("ActiveTotalEnergy");
        remainingEnergy = tag.getDouble("RemainingEnergy");
        generationCarry = tag.getDouble("GenerationCarry");
        pendingResidue = ItemStack.parseOptional(registries, tag.getCompound("PendingResidue"));
        bulkSpeed.load(tag);
        clampInternalEnergy();
    }

    /** Fluid Capacity, from Fluid Pump affixes, sizes the electrolyte tank; fluid above a smaller capacity stays until used. */
    private void updateTankCapacity(MachineStatAccumulator stats) {
        int capacity = Math.max(TANK_CAPACITY, (int) Math.round(stats.value(MachineStat.FLUID_CAPACITY)));
        if (electrolyteTank.getCapacity() != capacity) {
            electrolyteTank.setCapacity(capacity);
        }
    }

    private boolean tickCell() {
        if (level == null) {
            resetBulkSpeed();
            return false;
        }
        MachineStatAccumulator stats = effectiveStats();
        updateTankCapacity(stats);
        if (redstoneDisabled()) {
            resetBulkSpeed();
            return false;
        }

        if (!hasActiveRecipe() && !tryStartRecipe(stats)) {
            return false;
        }
        if (energyStored() >= energyCapacity(stats)) {
            return false;
        }
        return generateActiveRecipe(stats);
    }

    private boolean tryStartRecipe(MachineStatAccumulator stats) {
        CorrosionCellRecipe recipe = nextRecipe();
        if (recipe == null || !canProcessStage(recipe)) {
            resetInactiveProgress();
            return false;
        }

        ItemStack residue = recipe.residue().copy();
        if (!canMergeResidue(residue) || energyStored() >= energyCapacity(stats)) {
            return false;
        }

        consumeSlot(SLOT_PLATE);
        consumeElectrolyte(recipe);
        pendingResidue = residue;
        activeProcessingTicks = stats.adjustedProcessingTicks(recipe.processingTicks());
        activeTotalEnergy = effectiveRecipeEnergy(recipe, stats);
        remainingEnergy = activeTotalEnergy;
        generationCarry = 0.0;
        progress = 0;
        setChanged();
        return remainingEnergy > 0.0;
    }

    private boolean generateActiveRecipe(MachineStatAccumulator stats) {
        if (!hasActiveRecipe() || !canMergeResidue(pendingResidue)) {
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

        int stored = storeGeneratedEnergy(wholeEnergy, stats);
        if (stored <= 0) {
            return false;
        }
        generationCarry -= stored;
        remainingEnergy = Math.max(0.0, remainingEnergy - stored);
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

    private void consumeSlot(int slot) {
        ItemStack input = processInventory.getStackInSlot(slot);
        ItemStack remainder = input.getCraftingRemainingItem();
        input.shrink(1);
        if (!remainder.isEmpty()) {
            if (input.isEmpty()) {
                processInventory.setStackInSlot(slot, remainder);
            } else if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder);
            }
        }
    }

    private void consumeElectrolyte(CorrosionCellRecipe recipe) {
        ItemStack input = processInventory.getStackInSlot(SLOT_ELECTROLYTE);
        if (!input.isEmpty() && recipe.electrolyte().test(input)) {
            consumeSlot(SLOT_ELECTROLYTE);
            return;
        }
        if (canUseFluidElectrolyte(recipe)) {
            electrolyteTank.drain(electrolyteTank.getFluid().copyWithAmount(ELECTROLYTE_PER_REAGENT), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private CorrosionCellRecipe nextRecipe() {
        return level == null
                ? null
                : CorrosionCellRecipes.find(
                                level,
                                processInventory.getStackInSlot(SLOT_PLATE),
                                availableElectrolyteStack()
                        )
                        .orElse(null);
    }

    private boolean canProcessStage(CorrosionCellRecipe recipe) {
        return COMPONENT_STAGE >= recipe.minimumMaterialStage() && cathodeStage() >= recipe.minimumCathodeStage();
    }

    private double effectiveRecipeEnergy(CorrosionCellRecipe recipe, MachineStatAccumulator stats) {
        double efficiency = Math.max(0.1, stats.value(MachineStat.EFFICIENCY));
        double stability = Mth.clamp(stats.value(MachineStat.STABILITY), 0.25, 1.50);
        int ticks = stats.adjustedProcessingTicks(recipe.processingTicks());
        return Math.max(1.0, stats.generatedEnergyTotal(recipe.energy(), ticks) * efficiency * stability);
    }

    private ItemStack availableElectrolyteStack() {
        ItemStack item = processInventory.getStackInSlot(SLOT_ELECTROLYTE);
        if (!item.isEmpty()) {
            return item;
        }
        return hasFluidElectrolyte() ? electrolyteRecipeStack() : ItemStack.EMPTY;
    }

    private boolean hasFluidElectrolyte() {
        return isValidElectrolyte(electrolyteTank.getFluid()) && electrolyteTank.getFluidAmount() >= ELECTROLYTE_PER_REAGENT;
    }

    private boolean canUseFluidElectrolyte(CorrosionCellRecipe recipe) {
        return processInventory.getStackInSlot(SLOT_ELECTROLYTE).isEmpty()
                && hasFluidElectrolyte()
                && recipe.electrolyte().test(electrolyteRecipeStack());
    }

    private static ItemStack electrolyteRecipeStack() {
        return new ItemStack(ModItems.materialItem("electrolyte").get());
    }

    private static boolean isValidElectrolyte(FluidStack stack) {
        return !stack.isEmpty() && stack.is(ModTags.Fluids.ELECTROLYTES);
    }

    private int storeGeneratedEnergy(int energy, MachineStatAccumulator stats) {
        int remaining = Math.min(energy, energyCapacity(stats) - energyStored());
        if (remaining <= 0) {
            return 0;
        }

        int stored = receiveInternalEnergy(remaining, stats, false);
        remaining -= stored;

        IEnergyStorage cell = batteryCellEnergyStorage();
        if (cell != null && cell.canReceive() && remaining > 0) {
            int cellStored = cell.receiveEnergy(remaining, false);
            if (cellStored > 0) {
                stored += cellStored;
                setChanged();
            }
        }
        return stored;
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        MachineStatAccumulator stats = effectiveStats();
        if (energyStored() <= 0) {
            return false;
        }

        int remainingOutput = energyStored();
        boolean exported = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
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
        int received = Math.min(toReceive, internalEnergyCapacity(stats) - internalEnergyStored(stats));
        if (!simulate && received > 0) {
            internalEnergy = internalEnergyStored(stats) + received;
            setChanged();
        }
        return received;
    }

    private int extractEnergyInternal(int toExtract, MachineStatAccumulator stats, boolean simulate) {
        if (toExtract <= 0) {
            return 0;
        }

        int remaining = toExtract;
        int extracted = Math.min(internalEnergyStored(stats), remaining);
        if (!simulate && extracted > 0) {
            internalEnergy = internalEnergyStored(stats) - extracted;
            setChanged();
        }
        remaining -= extracted;

        IEnergyStorage cell = batteryCellEnergyStorage();
        if (cell != null && cell.canExtract() && remaining > 0) {
            int cellExtracted = cell.extractEnergy(remaining, simulate);
            extracted += cellExtracted;
            if (!simulate && cellExtracted > 0) {
                setChanged();
            }
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
        CorrosionCellRecipe recipe = nextRecipe();
        return recipe == null || !canProcessStage(recipe) ? 0 : stats.adjustedProcessingTicks(recipe.processingTicks());
    }

    private int currentEnergyPerTick(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return Math.max(0, (int) Math.round(activeTotalEnergy / Math.max(1, activeProcessingTicks)));
        }
        CorrosionCellRecipe recipe = nextRecipe();
        if (recipe == null || !canProcessStage(recipe)) {
            return 0;
        }
        return Math.max(1, (int) Math.round(effectiveRecipeEnergy(recipe, stats) / Math.max(1, stats.adjustedProcessingTicks(recipe.processingTicks()))));
    }

    private int currentRecipeEnergy(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return Math.max(0, (int) Math.round(activeTotalEnergy));
        }
        CorrosionCellRecipe recipe = nextRecipe();
        return recipe == null || !canProcessStage(recipe) ? 0 : Math.max(1, (int) Math.round(effectiveRecipeEnergy(recipe, stats)));
    }

    private int currentMinimumStage() {
        CorrosionCellRecipe recipe = nextRecipe();
        if (recipe == null) {
            return 0;
        }
        return recipe.minimumCathodeStage() > 0 ? recipe.minimumCathodeStage() : recipe.minimumMaterialStage();
    }

    private int statusCode(MachineStatAccumulator stats) {
        if (redstoneDisabled()) {
            return STATUS_REDSTONE_DISABLED;
        }
        if (hasActiveRecipe()) {
            if (!canMergeResidue(pendingResidue)) {
                return STATUS_OUTPUT_FULL;
            }
            return energyStored() >= energyCapacity(stats) ? STATUS_ENERGY_FULL : STATUS_READY;
        }
        if (processInventory.getStackInSlot(SLOT_PLATE).isEmpty()) {
            return STATUS_NO_PLATE;
        }
        if (processInventory.getStackInSlot(SLOT_ELECTROLYTE).isEmpty() && !hasFluidElectrolyte()) {
            return STATUS_NO_ELECTROLYTE;
        }
        CorrosionCellRecipe recipe = nextRecipe();
        if (recipe == null) {
            return STATUS_INVALID_RECIPE;
        }
        if (cathodeStage() < recipe.minimumCathodeStage()) {
            return STATUS_BLOCKED_CATHODE;
        }
        if (!canProcessStage(recipe)) {
            return STATUS_BLOCKED_STAGE;
        }
        if (!canMergeResidue(recipe.residue())) {
            return STATUS_OUTPUT_FULL;
        }
        return energyStored() >= energyCapacity(stats) ? STATUS_ENERGY_FULL : STATUS_READY;
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
        return internalEnergyStored(effectiveStats()) + BatteryCellItem.energyStored(batteryCellStack());
    }

    private int energyCapacity(MachineStatAccumulator stats) {
        return internalEnergyCapacity(stats) + BatteryCellItem.energyCapacity(batteryCellStack());
    }

    private int internalEnergyCapacity(MachineStatAccumulator stats) {
        return Math.max(1, (int) Math.round(stats.value(MachineStat.ENERGY_CAPACITY)));
    }

    private int internalEnergyStored(MachineStatAccumulator stats) {
        return Mth.clamp(internalEnergy, 0, internalEnergyCapacity(stats));
    }

    private int effectiveFluidTransfer(MachineStatAccumulator stats) {
        return hasFluidPump() ? Math.max(1, stats.intValue(MachineStat.FLUID_TRANSFER)) : 0;
    }

    private void applyFluidPumpStats(MachineStatAccumulator stats) {
        ItemStack stack = fluidPumpStack();
        if (isFluidPump(stack) && stack.getItem() instanceof MachinePartItem) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        }
    }

    private IEnergyStorage batteryCellEnergyStorage() {
        ItemStack stack = batteryCellStack();
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private boolean hasFluidPump() {
        return isFluidPump(fluidPumpStack());
    }

    private ItemStack fluidPumpStack() {
        return gearInventory.getStackInSlot(SLOT_FLUID_PUMP);
    }

    private ItemStack cathodeStack() {
        return gearInventory.getStackInSlot(SLOT_CATHODE);
    }

    private int cathodeStage() {
        return cathodeStack().getItem() instanceof CathodeItem cathode ? cathode.stage() : 0;
    }

    /** Older saves have a two-slot Gear inventory without the Cathode slot. */
    private void loadGearInventory(CompoundTag gearTag, HolderLookup.Provider registries) {
        int savedSize = gearTag.getInt("Size");
        if (savedSize == GEAR_SLOT_COUNT) {
            gearInventory.deserializeNBT(registries, gearTag);
            return;
        }

        ItemStackHandler legacyGear = new ItemStackHandler(Math.max(0, savedSize));
        legacyGear.deserializeNBT(registries, gearTag);
        gearInventory.setSize(GEAR_SLOT_COUNT);
        for (int slot = 0; slot < Math.min(legacyGear.getSlots(), GEAR_SLOT_COUNT); slot++) {
            gearInventory.setStackInSlot(slot, legacyGear.getStackInSlot(slot));
        }
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
    }

    private void clampInternalEnergy() {
        MachineStatAccumulator stats = effectiveStats();
        internalEnergy = internalEnergyStored(stats);
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private final class SingleSlotInsertHandler implements IItemHandler {
        private final int mappedSlot;

        private SingleSlotInsertHandler(int mappedSlot) {
            this.mappedSlot = mappedSlot;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(mapSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return processInventory.insertItem(mapSlot(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(mapSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return processInventory.isItemValid(mapSlot(slot), stack);
        }

        private int mapSlot(int slot) {
            if (slot != 0) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
            }
            return mappedSlot;
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

    private final class SideInputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
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
            return switch (slot) {
                case 0 -> SLOT_PLATE;
                case 1 -> SLOT_ELECTROLYTE;
                default -> throw new RuntimeException("Slot " + slot + " not in valid range - [0,2)");
            };
        }
    }

    private final class ElectrolyteFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? electrolyteTank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? electrolyteTank.getTankCapacity(0) : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && electrolyteTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!hasFluidPump() || resource.isEmpty()) {
                return 0;
            }
            int capped = Math.min(resource.getAmount(), effectiveFluidTransfer(effectiveStats()));
            return electrolyteTank.fill(resource.copyWithAmount(capped), action);
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

    private final class CellEnergyStorage implements IEnergyStorage {
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
        CorrosionCellRecipe recipe = nextRecipe();
        return recipe == null ? 0.0 : recipe.energy() / (double) Math.max(1, recipe.processingTicks());
    }
}
