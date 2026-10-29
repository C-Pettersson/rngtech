package com.rngtech.content.blockentity;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.energy.CorrosionCellLoad;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.CathodeItem;
import com.rngtech.content.item.GearParts;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.menu.CorrosionCellMenu;
import com.rngtech.content.menu.GenerationMenuSupport;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.CorrosionCellRecipe;
import com.rngtech.content.recipe.CorrosionCellRecipeInput;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.content.registry.ModTags;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
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
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public class CorrosionCellBlockEntity extends BaseMachineBlockEntity
        implements MenuProvider, PurgeableFluidStorage, MachineInfoProvider {
    private final EnergyExportTargets exportTargets = new EnergyExportTargets(this);
    private static final RecipeCache.Search<CorrosionCellRecipe> RECIPE_SEARCH = CorrosionCellRecipes::find;
    private static final RecipeInputFilter<CorrosionCellRecipeInput, CorrosionCellRecipe> PLATE_FILTER = new RecipeInputFilter<>(
            ModRecipes.CORROSION_CELL_TYPE::get,
            (recipe, item) -> recipe.plate().test(item));
    private static final RecipeInputFilter<CorrosionCellRecipeInput, CorrosionCellRecipe> ELECTROLYTE_FILTER = new RecipeInputFilter<>(
            ModRecipes.CORROSION_CELL_TYPE::get,
            (recipe, item) -> recipe.electrolyte().test(item));
    private final RecipeCache<CorrosionCellRecipe> recipes = new RecipeCache<>(1);
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
    private static final int DATA_LOAD = 22;
    private static final int DATA_LIVE_LOAD = 23;
    private static final int DATA_PACK_GENERATION_MULTIPLIER = 24;
    private static final int STAT_SCALE = 100;
    private static final int COMPONENT_STAGE = 4;
    private static final int VISUAL_SYNC_INTERVAL = 2;

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
            if (slot == SLOT_PLATE && plateWatch.changedTo(getStackInSlot(slot))
                    || slot == SLOT_ELECTROLYTE && electrolyteWatch.changedTo(getStackInSlot(slot))) {
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
    private final InstalledBatteryCell batteryCell = new InstalledBatteryCell(this::batteryCellStack);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_PROGRESS -> displayProgress();
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
                case DATA_LOAD -> load;
                case DATA_LIVE_LOAD -> (int) Math.round(liveCurrent * 100.0);
                case DATA_PACK_GENERATION_MULTIPLIER -> GenerationMenuSupport.packMultiplierData(COMPONENT_STAGE);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_PACK_GENERATION_MULTIPLIER + 1;
        }
    };

    private final BulkSpeedState bulkSpeed = new BulkSpeedState();
    private final BulkSpeedState.InputWatch plateWatch = new BulkSpeedState.InputWatch();
    private final BulkSpeedState.InputWatch electrolyteWatch = new BulkSpeedState.InputWatch();
    /** Rated ticks of the active fuel already dissolved; the cycle ends at {@code activeProcessingTicks}. */
    private double progress;
    private int activeProcessingTicks;
    private int internalEnergy;
    private double activeTotalEnergy;
    private double generationCarry;
    private ItemStack pendingResidue = ItemStack.EMPTY;
    private String activeFuel = "";
    private int load = CorrosionCellLoad.RATED_LOAD;
    private double liveCurrent;
    private CorrosionVisualState visualState = CorrosionVisualState.IDLE;
    private CorrosionVisualState.Key lastSyncedVisual;
    private final CorrosionVisualState.Animation animation = new CorrosionVisualState.Animation();

    public CorrosionCellBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CORROSION_CELL.get(), pos, blockState, MachineType.CORROSION_CELL, SLOT_PLATE, SLOT_ELECTROLYTE, SLOT_RESIDUE);
        trackStatSlots(gearInventory);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CorrosionCellBlockEntity cell) {
        boolean generated = cell.tickCell();
        boolean exported = cell.exportEnergy(level, pos);
        BaseMachineBlock.setActive(level, pos, state, cell.isWorking());
        if (generated || exported) {
            cell.setChanged();
        }
        cell.syncVisualState(level);
    }

    /** The selected share of rated current, in percent. */
    public int load() {
        return load;
    }

    public void setLoad(int load) {
        int snapped = CorrosionCellLoad.snap(load);
        if (snapped != this.load) {
            this.load = snapped;
            setChanged();
        }
    }

    /** Client copy of the state the renderer draws; synced from the server in the block entity update tag. */
    public CorrosionVisualState visualState() {
        return visualState;
    }

    public CorrosionVisualState.Animation animation() {
        return animation;
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
        visualState = CorrosionVisualState.load(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        visualState = CorrosionVisualState.load(packet.getTag());
    }

    private void syncVisualState(Level level) {
        if (level.getGameTime() % VISUAL_SYNC_INTERVAL != 0) {
            return;
        }
        CorrosionVisualState.Key key = currentVisualState().key();
        if (!key.equals(lastSyncedVisual)) {
            lastSyncedVisual = key;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** What the renderer shows, computed on the server. */
    public CorrosionVisualState currentVisualState() {
        ItemStack residue = processInventory.getStackInSlot(SLOT_RESIDUE);
        boolean active = hasActiveRecipe();
        return new CorrosionVisualState(
                active ? activeFuel : "",
                active ? (float) Mth.clamp(1.0 - progress / activeProcessingTicks, 0.0, 1.0) : 0.0F,
                (float) liveCurrent,
                cathodeStack().getItem() instanceof CathodeItem cathode ? cathode.material().getSerializedName() : "",
                active || !processInventory.getStackInSlot(SLOT_ELECTROLYTE).isEmpty() || hasFluidElectrolyte(),
                electrolyteTank.getFluidAmount() / (float) Math.max(1, electrolyteTank.getCapacity()),
                electrolyteTank.getFluid().getFluid(),
                residue.isEmpty() ? 0.0F : Mth.clamp(residue.getCount() / (float) residue.getMaxStackSize(), 0.0F, 1.0F)
        );
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

    /** FE leaves through every face, so a connector can attach on any side. */
    public IEnergyStorage getEnergyStorage(Direction side) {
        return trackedEnergyStorage;
    }

    /** With a Fluid Pump installed, Electrolyte Solution fills through every face. */
    public IFluidHandler getFluidHandler(Direction side) {
        return hasFluidPump() ? fluidHandler : null;
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
                .progress(displayProgress(), currentProcessingTicks(stats))
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
        return level == null || PLATE_FILTER.accepts(level, stack);
    }

    public boolean isKnownElectrolyteInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return level == null || ELECTROLYTE_FILTER.accepts(level, stack);
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
        return cachedStats(bulkSpeed.count());
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.corrosionCell();
        stats.apply(effectiveTraits());
        applyFluidPumpStats(stats);
        if (isCathode(cathodeStack())) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, cathodeStack());
        }
        bulkSpeed.apply(stats, activeTraits());
        return stats;
    }

    private MachineTraits activeTraits() {
        return effectiveTraits();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.put("ElectrolyteTank", electrolyteTank.writeToNBT(registries, new CompoundTag()));
        tag.putDouble("Progress", progress);
        tag.putInt("ActiveProcessingTicks", activeProcessingTicks);
        tag.putInt("Energy", internalEnergy);
        tag.putDouble("ActiveTotalEnergy", activeTotalEnergy);
        tag.putDouble("GenerationCarry", generationCarry);
        tag.put("PendingResidue", pendingResidue.saveOptional(registries));
        tag.putString("ActiveFuel", activeFuel);
        tag.putInt("LoadPercent", load);
        bulkSpeed.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        plateWatch.prime(processInventory.getStackInSlot(SLOT_PLATE));
        electrolyteWatch.prime(processInventory.getStackInSlot(SLOT_ELECTROLYTE));
        loadGearInventory(tag.getCompound("GearInventory"), registries);
        electrolyteTank.readFromNBT(registries, tag.getCompound("ElectrolyteTank"));
        progress = Math.max(0.0, tag.getDouble("Progress"));
        activeProcessingTicks = Math.max(0, tag.getInt("ActiveProcessingTicks"));
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        activeTotalEnergy = tag.getDouble("ActiveTotalEnergy");
        generationCarry = tag.getDouble("GenerationCarry");
        pendingResidue = ItemStack.parseOptional(registries, tag.getCompound("PendingResidue"));
        activeFuel = tag.getString("ActiveFuel");
        load = tag.contains("LoadPercent") ? CorrosionCellLoad.snap(tag.getInt("LoadPercent")) : CorrosionCellLoad.RATED_LOAD;
        bulkSpeed.load(tag);
        clampInternalEnergy();
    }

    /** Fluid Capacity, from the chassis and Fluid Pump affixes, sizes the electrolyte tank; fluid above a smaller capacity stays until used. */
    private void updateTankCapacity(MachineStatAccumulator stats) {
        int capacity = Math.max(TANK_CAPACITY, (int) Math.round(stats.value(MachineStat.FLUID_CAPACITY)));
        if (electrolyteTank.getCapacity() != capacity) {
            electrolyteTank.setCapacity(capacity);
        }
    }

    private boolean tickCell() {
        liveCurrent = 0.0;
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

        activeFuel = BuiltInRegistries.ITEM.getKey(processInventory.getStackInSlot(SLOT_PLATE).getItem()).toString();
        consumeSlot(SLOT_PLATE);
        consumeElectrolyte(recipe);
        pendingResidue = residue;
        activeProcessingTicks = Math.max(1, stats.adjustedProcessingTicks(recipe.processingTicks()));
        activeTotalEnergy = effectiveRecipeEnergy(recipe, stats);
        generationCarry = 0.0;
        progress = 0.0;
        setChanged();
        return true;
    }

    /**
     * Dissolves the fuel at the selected load: the load sets how much of the cycle passes this tick, and the cell voltage
     * at that load sets the FE it is worth. Only as much fuel dissolves as the buffer can take, so a full buffer pauses the
     * cycle without losing FE. At rated load this is one tick of the recipe per tick, at the recipe's FE/t.
     */
    private boolean generateActiveRecipe(MachineStatAccumulator stats) {
        if (!hasActiveRecipe() || !canMergeResidue(pendingResidue)) {
            return false;
        }
        if (CorrosionCellLoad.finished(progress, activeProcessingTicks)) {
            finishIfComplete();
            return true;
        }

        int freeSpace = energyCapacity(stats) - energyStored();
        if (freeSpace <= 0) {
            return false;
        }

        double step = CorrosionCellLoad.dissolve(progress, activeProcessingTicks, load);
        double energy = activeTotalEnergy / activeProcessingTicks * CorrosionCellLoad.fePerFuel(load) * step;
        double room = Math.max(0.0, freeSpace - generationCarry);
        if (energy > room) {
            step = energy <= 0.0 ? 0.0 : step * room / energy;
            energy = room;
        }
        generationCarry += energy;
        int wholeEnergy = Math.min((int) Math.floor(generationCarry), freeSpace);
        if (wholeEnergy > 0) {
            generationCarry -= storeGeneratedEnergy(wholeEnergy, stats);
        }
        if (step <= 0.0) {
            return wholeEnergy > 0;
        }
        progress += step;
        liveCurrent = step;
        finishIfComplete();
        return true;
    }

    private void finishIfComplete() {
        if (!CorrosionCellLoad.finished(progress, activeProcessingTicks)) {
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
                : recipes.find(0, level, processInventory.getStackInSlot(SLOT_PLATE), availableElectrolyteStack(), RECIPE_SEARCH);
    }

    /** How many recipe lookups searched the recipes instead of reusing a remembered result. */
    public int recipeSearches() {
        return recipes.searches();
    }

    private boolean canProcessStage(CorrosionCellRecipe recipe) {
        return COMPONENT_STAGE >= recipe.minimumMaterialStage() && cathodeStage() >= recipe.minimumCathodeStage();
    }

    private double effectiveRecipeEnergy(CorrosionCellRecipe recipe, MachineStatAccumulator stats) {
        double efficiency = Math.max(0.1, stats.value(MachineStat.EFFICIENCY));
        double stability = Mth.clamp(stats.value(MachineStat.STABILITY), 0.25, 1.50);
        int ticks = stats.adjustedProcessingTicks(recipe.processingTicks());
        return Math.max(1.0, RNGTechConfig.generatedEnergy(stats.generatedEnergyTotal(recipe.energy(), ticks) * efficiency * stability,
                COMPONENT_STAGE));
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

    /** How many times an energy export looked a neighbour up instead of reusing its cached capability. */
    public int exportTargetLookups() {
        return exportTargets.lookups();
    }

    private boolean exportEnergy(Level level, BlockPos pos) {
        MachineStatAccumulator stats = effectiveStats();
        if (energyStored() <= 0) {
            return false;
        }

        int remainingOutput = energyStored();
        boolean exported = false;
        for (Direction direction : Direction.values()) {
            if (remainingOutput <= 0 || energyStored() <= 0) {
                return exported;
            }

            IEnergyStorage target = exportTargets.get(level, pos, direction);
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

    /** Cycle length at the selected load, for the active fuel or the next one. */
    private int currentProcessingTicks(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return CorrosionCellLoad.displayTicks(activeProcessingTicks, load);
        }
        CorrosionCellRecipe recipe = nextRecipe();
        return recipe == null || !canProcessStage(recipe)
                ? 0
                : CorrosionCellLoad.displayTicks(stats.adjustedProcessingTicks(recipe.processingTicks()), load);
    }

    private int displayProgress() {
        return hasActiveRecipe() ? (int) Math.floor(progress / CorrosionCellLoad.current(load) + 1.0e-6) : 0;
    }

    /** FE/t at the selected load. */
    private int currentEnergyPerTick(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return Math.max(0, (int) Math.round(activeTotalEnergy / activeProcessingTicks * CorrosionCellLoad.fePerTick(load)));
        }
        CorrosionCellRecipe recipe = nextRecipe();
        if (recipe == null || !canProcessStage(recipe)) {
            return 0;
        }
        double ratedPerTick = effectiveRecipeEnergy(recipe, stats) / Math.max(1, stats.adjustedProcessingTicks(recipe.processingTicks()));
        return Math.max(1, (int) Math.round(ratedPerTick * CorrosionCellLoad.fePerTick(load)));
    }

    /** FE the active or next fuel is worth at the selected load. */
    private int currentRecipeEnergy(MachineStatAccumulator stats) {
        if (hasActiveRecipe()) {
            return Math.max(0, (int) Math.round(activeTotalEnergy * CorrosionCellLoad.fePerFuel(load)));
        }
        CorrosionCellRecipe recipe = nextRecipe();
        return recipe == null || !canProcessStage(recipe)
                ? 0
                : Math.max(1, (int) Math.round(effectiveRecipeEnergy(recipe, stats) * CorrosionCellLoad.fePerFuel(load)));
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

    /** A fuel is in the bath; a finished one stays until its residue fits the output. */
    private boolean hasActiveRecipe() {
        return activeProcessingTicks > 0;
    }

    private boolean isWorking() {
        return hasActiveRecipe() && !CorrosionCellLoad.finished(progress, activeProcessingTicks) && !redstoneDisabled();
    }

    private boolean redstoneDisabled() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    private void resetInactiveProgress() {
        resetBulkSpeed();
        if (!hasActiveRecipe() && progress != 0.0) {
            progress = 0.0;
            setChanged();
        }
    }

    private void clearActiveRecipe() {
        progress = 0.0;
        activeProcessingTicks = 0;
        activeTotalEnergy = 0.0;
        generationCarry = 0.0;
        pendingResidue = ItemStack.EMPTY;
        activeFuel = "";
    }

    private void resetBulkSpeed() {
        if (bulkSpeed.reset()) {
            setChanged();
        }
    }

    private int energyStored() {
        return internalEnergyStored(effectiveStats()) + batteryCell.energyStored();
    }

    private int energyCapacity(MachineStatAccumulator stats) {
        return internalEnergyCapacity(stats) + batteryCell.capacity();
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
        return batteryCell.storage();
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
