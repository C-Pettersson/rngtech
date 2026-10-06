package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.CrushHeadItem;
import com.rngtech.content.item.FluidPumpItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.ServoItem;
import com.rngtech.content.item.SolidFuelBurnerPartItem;
import com.rngtech.content.menu.MasteryMenuSupport;
import com.rngtech.content.menu.MelterMenu;
import com.rngtech.content.purge.FluidOutputOverflow;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.MelterRecipe;
import com.rngtech.content.recipe.MelterRecipeInput;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModTags;
import com.rngtech.rpg.BatchProcessing;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.progression.AscendancyFormulas;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.material.Fluids;
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

import java.util.List;

public class MelterBlockEntity extends BaseMachineBlockEntity
        implements MenuProvider, PurgeableFluidStorage, MachineMasteryHost, MachineInfoProvider {
    public static final int SLOT_PRIMARY_INPUT = 0;
    public static final int SLOT_SECONDARY_INPUT = 1;
    public static final int SLOT_FLUID_INPUT_CONTAINER = 2;
    public static final int SLOT_FLUID_OUTPUT_CONTAINER = 3;
    public static final int PROCESS_SLOT_COUNT = 4;
    public static final int SLOT_HEAT_CORE = 0;
    public static final int SLOT_CRUSH_HEAD = 1;
    public static final int SLOT_BATTERY_CELL = 2;
    public static final int SLOT_SERVO = 3;
    public static final int SLOT_FLUID_PUMP = 4;
    public static final int GEAR_SLOT_COUNT = 5;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_HEAT_CORE = 1;
    public static final int STATUS_MISSING_CRUSH_HEAD = 2;
    public static final int STATUS_NO_INPUT = 3;
    public static final int STATUS_INVALID_RECIPE = 4;
    public static final int STATUS_HEAT_LOW = 5;
    public static final int STATUS_LEVEL_LOW = 6;
    public static final int STATUS_OUTPUT_TANK_FULL = 7;
    public static final int STATUS_NO_POWER = 8;
    public static final int STATUS_NO_FLUID = 9;
    public static final int PURGE_INPUT_TANK = 0;
    public static final int PURGE_OUTPUT_TANK = 1;

    private static final int TANK_CAPACITY = FluidType.BUCKET_VOLUME * 4;
    private static final double NO_BATTERY_PROCESSING_SPEED = 0.80;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_HEAT = 4;
    private static final int DATA_MIN_TEMPERATURE = 5;
    private static final int DATA_PROCESSING_LEVEL = 6;
    private static final int DATA_REQUIRED_PROCESSING_LEVEL = 7;
    private static final int DATA_INPUT_FLUID = 8;
    private static final int DATA_INPUT_FLUID_CAPACITY = 9;
    private static final int DATA_OUTPUT_FLUID = 10;
    private static final int DATA_OUTPUT_FLUID_CAPACITY = 11;
    private static final int DATA_STATUS = 12;
    private static final int DATA_PROCESSING_SPEED = 13;
    private static final int DATA_EFFICIENCY = 14;
    private static final int DATA_ENERGY_USAGE = 15;
    private static final int DATA_ENERGY_CAPACITY_STAT = 16;
    private static final int DATA_ENERGY_TRANSFER = 17;
    private static final int DATA_HEAT_TRANSFER = 18;
    private static final int DATA_MAX_TEMPERATURE = 19;
    private static final int DATA_FLUID_TRANSFER = 20;
    private static final int DATA_REFINEMENT_POTENTIAL = 21;
    private static final int DATA_BATCH_SIZE = DATA_REFINEMENT_POTENTIAL + 1;
    private static final int DATA_MACHINE_PROGRESSION_START = DATA_BATCH_SIZE + 1;
    private static final int DATA_COUNT = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int STAT_SCALE = 100;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_PRIMARY_INPUT, SLOT_SECONDARY_INPUT -> !stack.isEmpty();
                case SLOT_FLUID_INPUT_CONTAINER -> isFluidInputContainer(stack);
                case SLOT_FLUID_OUTPUT_CONTAINER -> isFluidOutputContainer(stack);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_FLUID_INPUT_CONTAINER || slot == SLOT_FLUID_OUTPUT_CONTAINER ? 1 : super.getSlotLimit(slot);
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
            if (slot == SLOT_PRIMARY_INPUT || slot == SLOT_SECONDARY_INPUT) {
                resetCycle();
                resetBulkSpeed();
            }
            setChanged();
        }
    };
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_HEAT_CORE -> isHeatCore(stack);
                case SLOT_CRUSH_HEAD -> isCrushHead(stack);
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_SERVO -> isServo(stack);
                case SLOT_FLUID_PUMP -> isFluidPump(stack);
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
            if (slot != SLOT_BATTERY_CELL && slot != SLOT_FLUID_PUMP) {
                resetCycle();
                resetBulkSpeed();
            }
            clampInternalEnergy();
            setChanged();
        }
    };
    private final FluidTank inputTank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY, MelterBlockEntity::isValidOutputFluid) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IItemHandler ingredientInputHandler = new ProcessItemHandler(SLOT_PRIMARY_INPUT, SLOT_SECONDARY_INPUT, true, false);
    private final IItemHandler containerInputHandler = new ProcessItemHandler(SLOT_FLUID_INPUT_CONTAINER, SLOT_FLUID_OUTPUT_CONTAINER, true, false);
    private final IItemHandler containerOutputHandler = new ProcessItemHandler(SLOT_FLUID_OUTPUT_CONTAINER, SLOT_FLUID_OUTPUT_CONTAINER, false, true);
    private final IEnergyStorage energyStorage = new MelterEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final IFluidHandler fluidHandler = new MelterFluidHandler();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_MACHINE_PROGRESSION_START && index < DATA_COUNT) {
                return MasteryMenuSupport.get(MelterBlockEntity.this, index - DATA_MACHINE_PROGRESSION_START, MelterBlockEntity.this::effectiveStats);
            }
            MachineStatAccumulator stats = effectiveStats();
            MelterRecipe recipe = nextRecipe();
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> currentProcessingTicks(recipe, stats);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_HEAT -> effectiveHeat(stats);
                case DATA_MIN_TEMPERATURE -> recipe == null ? 0 : recipe.minimumTemperature();
                case DATA_PROCESSING_LEVEL -> processingLevel(stats);
                case DATA_REQUIRED_PROCESSING_LEVEL -> recipe == null ? 0 : recipe.requiredProcessingLevel();
                case DATA_INPUT_FLUID -> inputTank.getFluidAmount();
                case DATA_INPUT_FLUID_CAPACITY -> inputTank.getCapacity();
                case DATA_OUTPUT_FLUID -> outputTank.getFluidAmount();
                case DATA_OUTPUT_FLUID_CAPACITY -> outputTank.getCapacity();
                case DATA_STATUS -> statusCode(recipe, stats);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_HEAT_TRANSFER -> scaledStat(stats, MachineStat.HEAT_TRANSFER);
                case DATA_MAX_TEMPERATURE -> scaledStat(stats, MachineStat.MAX_TEMPERATURE);
                case DATA_FLUID_TRANSFER -> effectiveFluidTransfer(stats);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_BATCH_SIZE -> BatchProcessing.statBatchSize(stats) * STAT_SCALE;
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

    private final BulkSpeedState bulkSpeed = new BulkSpeedState();
    private int progress;
    private int activeProcessingTicks;
    private int lockedBatch;
    private int internalEnergy;
    private ItemStack activePrimary = ItemStack.EMPTY;
    private ItemStack activeSecondary = ItemStack.EMPTY;
    private FluidStack activeFluid = FluidStack.EMPTY;

    public MelterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MELTER.get(), pos, blockState, MachineType.MELTER, SLOT_PRIMARY_INPUT, SLOT_FLUID_INPUT_CONTAINER, SLOT_FLUID_OUTPUT_CONTAINER);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MelterBlockEntity melter) {
        melter.drainInputContainer();
        melter.fillOutputContainer();

        MachineStatAccumulator stats = melter.effectiveStats();
        melter.updateTankCapacity(stats);
        MelterRecipe recipe = melter.nextRecipe();
        if (recipe == null || !melter.hasRequiredComponents()) {
            melter.resetCycleIfActive();
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }
        if (!melter.activeCycleMatches()) {
            melter.resetCycle();
            melter.resetBulkSpeed();
        }
        if (melter.effectiveHeat(stats) < recipe.minimumTemperature()
                || melter.processingLevel(stats) < recipe.requiredProcessingLevel()
                || !melter.canAcceptOutputFluid(melter.yieldedOutput(recipe, stats))) {
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        int energyCost = melter.scaledForJobs(melter.energyCostPerTick(recipe, stats), recipe, stats);
        if (melter.progress == 0 && ProcessingChance.rollInstant(level, stats)) {
            int fullEnergyCost = melter.scaledForJobs(melter.energyCostPerCraft(recipe, stats), recipe, stats);
            if (melter.consumeWorkingEnergy(fullEnergyCost, true) >= fullEnergyCost) {
                melter.startCycleIfNeeded(recipe, stats);
                melter.consumeWorkingEnergy(fullEnergyCost, false);
                if (melter.process(recipe, stats)) {
                    melter.bulkSpeed.recordProcess(melter.activeTraits());
                }
                BaseMachineBlock.setActive(level, pos, state, true);
                melter.setChanged();
                return;
            }
        }
        if (melter.consumeWorkingEnergy(energyCost, true) < energyCost) {
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        melter.startCycleIfNeeded(recipe, stats);
        melter.consumeWorkingEnergy(energyCost, false);
        melter.progress++;
        if (melter.progress >= melter.activeCycleProcessingTicks(recipe, stats)) {
            if (melter.process(recipe, stats)) {
                melter.bulkSpeed.recordProcess(melter.activeTraits());
            }
        }

        BaseMachineBlock.setActive(level, pos, state, true);
        melter.setChanged();
    }

    public ItemStackHandler getProcessInventory() {
        return processInventory;
    }

    public ItemStackHandler getGearInventory() {
        return gearInventory;
    }

    public FluidTank getInputTank() {
        return inputTank;
    }

    public FluidTank getOutputTank() {
        return outputTank;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return trackedEnergyStorage;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return side == null ? null : fluidHandler;
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        return List.of(
                FluidPurgeTarget.of(
                        PURGE_INPUT_TANK,
                        Component.translatable("rngtech.purge.target.input_tank"),
                        FluidPurgeRole.INPUT,
                        inputTank::getFluid,
                        inputTank::drain,
                        this::resetCycleAndBulkSpeed,
                        this::setChanged
                ).withCapacity(inputTank::getCapacity),
                FluidPurgeTarget.of(
                        PURGE_OUTPUT_TANK,
                        Component.translatable("rngtech.purge.target.output_tank"),
                        FluidPurgeRole.OUTPUT,
                        outputTank::getFluid,
                        outputTank::drain,
                        this::setChanged
                ).withCapacity(outputTank::getCapacity)
        );
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        MelterRecipe recipe = nextRecipe();
        int status = statusCode(recipe, stats);
        int energyDemand = status == STATUS_READY || status == STATUS_NO_POWER ? energyCostPerTick(recipe, stats) : 0;
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("melter")
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY,
                                getBlockState().getValue(BaseMachineBlock.ACTIVE),
                                status == STATUS_NO_INPUT
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status), recipe == null ? 0 : recipe.minimumTemperature())
                .progress(progress, currentProcessingTicks(recipe, stats))
                .energy(energyStored(), energyCapacity(), -energyDemand)
                .energyTelemetry(energyFlow.lastInput(), energyFlow.lastOutput(), connector.transferRate(), MachineInfoSnapshot.EnergyBottleneck.NONE)
                .processingLevel(
                        processingLevel(stats),
                        recipe == null ? MachineInfoSnapshot.UNSET : recipe.requiredProcessingLevel()
                )
                .heat(effectiveHeat(stats), recipe == null ? 0 : recipe.minimumTemperature())
                .gear(hasHeatCore()
                        ? MachineInfoSnapshot.GearSummary.HEAT_CORE_INSTALLED
                        : MachineInfoSnapshot.GearSummary.MISSING_HEAT_CORE)
                .output(status == STATUS_OUTPUT_TANK_FULL
                        ? MachineInfoSnapshot.OutputSummary.OUTPUT_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .refinement(machineTraits())
                .build();
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_MISSING_HEAT_CORE -> "missing_heat_core";
            case STATUS_MISSING_CRUSH_HEAD -> "missing_crush_head";
            case STATUS_NO_INPUT -> "no_input";
            case STATUS_INVALID_RECIPE -> "invalid_recipe";
            case STATUS_HEAT_LOW -> "heat_low";
            case STATUS_LEVEL_LOW -> "level_low";
            case STATUS_OUTPUT_TANK_FULL -> "output_full";
            case STATUS_NO_POWER -> "no_power";
            case STATUS_NO_FLUID -> "no_fluid";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.melter.status." + name;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == Direction.UP) {
            return ingredientInputHandler;
        }
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
                Component.translatable("container.rngtech.melter")
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MelterMenu(containerId, playerInventory, this, menuData);
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
        MachineStatAccumulator stats = MachineBaseStatCatalog.melter();
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
        MegaPassiveTree.applyStats(stats, machineProgression(), MachineMasteryFamily.MELTER);
        applyHeatCoreStats(stats);
        applyCrushHeadStats(stats);
        applyServoStats(stats);
        applyFluidPumpStats(stats);
        if (!hasBatteryCell()) {
            stats.apply(new MachineModifier(
                    ModifierSlot.IMPLICIT,
                    MachineStat.PROCESSING_SPEED,
                    ModifierOperation.LESS,
                    NO_BATTERY_PROCESSING_SPEED
            ));
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
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
        tag.putInt("ActiveProcessingTicks", activeProcessingTicks);
        tag.putInt("LockedBatch", lockedBatch);
        tag.putInt("Energy", internalEnergyStored());
        tag.put("ActivePrimary", activePrimary.saveOptional(registries));
        tag.put("ActiveSecondary", activeSecondary.saveOptional(registries));
        tag.put("ActiveFluid", activeFluid.saveOptional(registries));
        bulkSpeed.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        progress = tag.getInt("Progress");
        activeProcessingTicks = Math.max(0, tag.getInt("ActiveProcessingTicks"));
        lockedBatch = Math.max(0, tag.getInt("LockedBatch"));
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        activePrimary = ItemStack.parseOptional(registries, tag.getCompound("ActivePrimary"));
        activeSecondary = ItemStack.parseOptional(registries, tag.getCompound("ActiveSecondary"));
        activeFluid = FluidStack.parseOptional(registries, tag.getCompound("ActiveFluid"));
        bulkSpeed.load(tag);
        clampInternalEnergy();
    }

    public static boolean isHeatCore(ItemStack stack) {
        return stack.getItem() instanceof SolidFuelBurnerPartItem part
                && part.partType() == MachinePartType.HEAT_CORE
                && part.stage() <= 6;
    }

    public static boolean isCrushHead(ItemStack stack) {
        return stack.getItem() instanceof CrushHeadItem;
    }

    public static boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public static boolean isServo(ItemStack stack) {
        return stack.getItem() instanceof ServoItem;
    }

    public static boolean isFluidPump(ItemStack stack) {
        return stack.getItem() instanceof FluidPumpItem;
    }

    public static boolean isFluidInputContainer(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).isPresent();
    }

    public static boolean isFluidOutputContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack copy = stack.copyWithCount(1);
        FluidStack lubricant = new FluidStack(ModFluids.LUBRICANT_SOURCE.get(), FluidType.BUCKET_VOLUME);
        FluidStack lava = new FluidStack(Fluids.LAVA, FluidType.BUCKET_VOLUME);
        return FluidUtil.getFluidHandler(copy)
                .map(handler -> handler.fill(lubricant, IFluidHandler.FluidAction.SIMULATE) > 0
                        || handler.fill(lava, IFluidHandler.FluidAction.SIMULATE) > 0)
                .orElse(false);
    }

    private static boolean isValidOutputFluid(FluidStack stack) {
        return stack.is(ModTags.Fluids.MELTER_OUTPUTS);
    }

    public static boolean isGearComponent(ItemStack stack) {
        return isHeatCore(stack) || isCrushHead(stack) || isServo(stack) || isFluidPump(stack);
    }

    private MelterRecipe nextRecipe() {
        return level == null
                ? null
                : MelterRecipes.find(level, primaryStack(), secondaryStack(), inputTank.getFluid()).orElse(null);
    }

    private boolean process(MelterRecipe recipe, MachineStatAccumulator stats) {
        int jobs = parallelMelts(recipe, stats);
        boolean melted = false;
        for (int job = 0; job < jobs; job++) {
            if (level == null || !recipe.matches(new MelterRecipeInput(primaryStack(), secondaryStack(), inputTank.getFluid()), level)) {
                break;
            }
            FluidStack output = yieldedOutput(recipe, stats);
            if (!canAcceptOutputFluid(output)) {
                break;
            }
            consumeInput(SLOT_PRIMARY_INPUT);
            consumeInput(SLOT_SECONDARY_INPUT);
            FluidStack drained = inputTank.drain(inputTank.getFluid().copyWithAmount(recipe.fluidInput().amount()), IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() < recipe.fluidInput().amount()) {
                break;
            }
            if (outputTank.fill(output, IFluidHandler.FluidAction.EXECUTE) > 0) {
                grantRecipeXp(recipe);
            }
            melted = true;
        }
        resetCycle();
        if (!melted) {
            resetBulkSpeed();
        }
        setChanged();
        return melted;
    }

    /** One melt's output with Fluid Yield on eligible recipes; Methane Trap and Brine Loop add to their fluids. */
    private FluidStack yieldedOutput(MelterRecipe recipe, MachineStatAccumulator stats) {
        FluidStack output = recipe.outputFluid();
        if (!recipe.allowsBonusOutput()) {
            return output;
        }
        String fluid = BuiltInRegistries.FLUID.getKey(output.getFluid()).toString();
        double yield = stats.value(MachineStat.FLUID_YIELD)
                + (hasMasteryBehavior("METHANE_TRAP") && fluid.equals("rngtech:methane") ? 25.0 : 0.0)
                + (hasMasteryBehavior("BRINE_LOOP") && fluid.equals("rngtech:electrolyte_solution") ? 25.0 : 0.0);
        return output.copyWithAmount(AscendancyFormulas.yieldedFluid(output.getAmount(), yield));
    }

    /** The batch locked when the cycle started, shrunk only if its inputs have since gone. */
    private int parallelMelts(MelterRecipe recipe, MachineStatAccumulator stats) {
        int available = availableMelts(recipe, stats);
        return progress > 0 && lockedBatch > 0 ? Math.min(lockedBatch, available) : available;
    }

    /** Each batched melt needs its own inputs, fluid, and tank room. Fused Crucibles gives up batching. */
    private int availableMelts(MelterRecipe recipe, MachineStatAccumulator stats) {
        int limit = BatchProcessing.batchSize(stats, hasMasteryBehavior("FUSED_CRUCIBLES"));
        limit = Math.min(limit, Math.min(primaryStack().getCount(), secondaryStack().getCount()));
        limit = Math.min(limit, inputTank.getFluidAmount() / Math.max(1, recipe.fluidInput().amount()));
        FluidStack output = yieldedOutput(recipe, stats);
        return Math.max(1, BatchProcessing.largestFitting(limit, batch -> batch == 1 || canAcceptOutputFluid(output.copyWithAmount(output.getAmount() * batch))));
    }

    /** Batched melts each pay FE; Shared Heat makes every melt after the first 20% cheaper. */
    private int scaledForJobs(int energy, MelterRecipe recipe, MachineStatAccumulator stats) {
        int extra = parallelMelts(recipe, stats) - 1;
        double scale = 1.0 + extra * (hasMasteryBehavior("SHARED_HEAT") ? 0.8 : 1.0);
        return Math.max(1, (int) Math.ceil(energy * scale));
    }

    /** Overpressure, Lava Tap, Crush Feed, and Fused Crucibles speed up a melt; set when its cycle starts. */
    private double meltSpeed(MelterRecipe recipe, MachineStatAccumulator stats) {
        double speed = 1.0;
        if (hasMasteryBehavior("OVERPRESSURE") && outputTank.getFluidAmount() * 2 > outputTank.getCapacity()) {
            speed *= 1.3;
        }
        if (hasMasteryBehavior("LAVA_TAP") && recipe.fluidOutput().getFluid().isSame(Fluids.LAVA)) {
            speed *= 1.5;
        }
        int overlevel = Math.max(0, processingLevel(stats) - recipe.requiredProcessingLevel());
        speed *= 1.0 + Math.max(0.0, stats.value(MachineStat.OVERLEVEL_SPEED)) / 100.0 * overlevel;
        if (hasMasteryBehavior("FUSED_CRUCIBLES")) {
            speed *= 1.0 + AscendancyFormulas.FUSED_CRUCIBLES_SPEED_PER_BATCH * BatchProcessing.statBatchSize(stats);
        }
        return speed;
    }

    /** Fluid Capacity sizes both tanks; fluid above a smaller capacity stays until it drains. */
    private void updateTankCapacity(MachineStatAccumulator stats) {
        int capacity = Math.max(TANK_CAPACITY, (int) Math.round(stats.value(MachineStat.FLUID_CAPACITY)));
        if (inputTank.getCapacity() != capacity) {
            inputTank.setCapacity(capacity);
            outputTank.setCapacity(capacity);
        }
    }

    private void grantRecipeXp(MelterRecipe recipe) {
        if (recipe.machineXp() <= 0) {
            return;
        }
        int xpQuarters = MachineProgressionState.xpQuarters(machineProgression().level(), recipe.machineXpBand());
        if (xpQuarters <= 0) {
            return;
        }
        grantMasteryXp(MachineProgressionState.workXp(recipe.machineXp(), recipe.machineXpBand()), xpQuarters);
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
        return MachineMasteryFamily.MELTER;
    }

    /** The Melter has a single Titanium-stage body. */
    @Override
    public int ascendancyEntryStage() {
        return 6;
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

    private void consumeInput(int slot) {
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

    /** Sealed Lines never vents excess output, so a full tank pauses the melt instead. */
    private boolean canAcceptOutputFluid(FluidStack result) {
        return !result.isEmpty()
                && FluidOutputOverflow.canAcceptOrVoidExcess(outputTank, result, autoPurgesFluidOutput() && !hasMasteryBehavior("SEALED_LINES"));
    }

    private int processingTicks(MelterRecipe recipe, MachineStatAccumulator stats) {
        int ticks = Math.max(1, (int) Math.ceil(stats.adjustedHeatProcessingTicks(recipe.processingTicks()) / meltSpeed(recipe, stats)));
        return BatchProcessing.batchTicks(ticks, stats, parallelMelts(recipe, stats));
    }

    private int energyCostPerTick(MelterRecipe recipe, MachineStatAccumulator stats) {
        int adjustedTicks = processingTicks(recipe, stats);
        int totalEnergy = stats.adjustedEnergyCost(recipe.energy());
        return Math.max(1, (int) Math.ceil(totalEnergy / (double) adjustedTicks));
    }

    private int energyCostPerCraft(MelterRecipe recipe, MachineStatAccumulator stats) {
        return stats.adjustedEnergyCost(recipe.energy());
    }

    private int currentProcessingTicks(MelterRecipe recipe, MachineStatAccumulator stats) {
        if (hasActiveCycle() && activeProcessingTicks > 0) {
            return activeProcessingTicks;
        }
        return recipe == null ? 0 : processingTicks(recipe, stats);
    }

    private int statusCode(MelterRecipe recipe, MachineStatAccumulator stats) {
        if (!hasHeatCore()) {
            return STATUS_MISSING_HEAT_CORE;
        }
        if (!hasCrushHead()) {
            return STATUS_MISSING_CRUSH_HEAD;
        }
        if (primaryStack().isEmpty() || secondaryStack().isEmpty()) {
            return STATUS_NO_INPUT;
        }
        if (inputTank.isEmpty()) {
            return STATUS_NO_FLUID;
        }
        if (recipe == null) {
            return STATUS_INVALID_RECIPE;
        }
        if (effectiveHeat(stats) < recipe.minimumTemperature()) {
            return STATUS_HEAT_LOW;
        }
        if (processingLevel(stats) < recipe.requiredProcessingLevel()) {
            return STATUS_LEVEL_LOW;
        }
        if (!canAcceptOutputFluid(yieldedOutput(recipe, stats))) {
            return STATUS_OUTPUT_TANK_FULL;
        }
        int energyCost = energyCostPerTick(recipe, stats);
        return consumeWorkingEnergy(energyCost, true) < energyCost ? STATUS_NO_POWER : STATUS_READY;
    }

    private void startCycleIfNeeded(MelterRecipe recipe, MachineStatAccumulator stats) {
        if (progress != 0) {
            return;
        }
        lockedBatch = availableMelts(recipe, stats);
        activePrimary = singleCopy(primaryStack());
        activeSecondary = singleCopy(secondaryStack());
        activeFluid = inputTank.getFluid().copyWithAmount(recipe.fluidInput().amount());
        activeProcessingTicks = processingTicks(recipe, stats);
    }

    private int activeCycleProcessingTicks(MelterRecipe recipe, MachineStatAccumulator stats) {
        if (activeProcessingTicks <= 0) {
            activeProcessingTicks = processingTicks(recipe, stats);
        }
        return activeProcessingTicks;
    }

    private boolean activeCycleMatches() {
        if (progress == 0) {
            return true;
        }
        return !activePrimary.isEmpty()
                && !activeSecondary.isEmpty()
                && !activeFluid.isEmpty()
                && ItemStack.isSameItemSameComponents(activePrimary, primaryStack())
                && ItemStack.isSameItemSameComponents(activeSecondary, secondaryStack())
                && FluidStack.isSameFluidSameComponents(activeFluid, inputTank.getFluid())
                && inputTank.getFluidAmount() >= activeFluid.getAmount();
    }

    private void resetCycleIfActive() {
        if (progress != 0 || !activePrimary.isEmpty() || !activeSecondary.isEmpty() || !activeFluid.isEmpty()) {
            resetCycle();
            resetBulkSpeed();
            setChanged();
        }
    }

    private void resetCycle() {
        progress = 0;
        activeProcessingTicks = 0;
        lockedBatch = 0;
        activePrimary = ItemStack.EMPTY;
        activeSecondary = ItemStack.EMPTY;
        activeFluid = FluidStack.EMPTY;
    }

    private boolean hasActiveCycle() {
        return progress != 0 || !activePrimary.isEmpty() || !activeSecondary.isEmpty() || !activeFluid.isEmpty();
    }

    private void resetBulkSpeed() {
        if (bulkSpeed.reset()) {
            setChanged();
        }
    }

    private void resetCycleAndBulkSpeed() {
        resetCycle();
        resetBulkSpeed();
    }

    private int effectiveHeat(MachineStatAccumulator stats) {
        return hasHeatCore() ? Math.max(0, stats.intValue(MachineStat.MAX_TEMPERATURE)) : 0;
    }

    private int processingLevel(MachineStatAccumulator stats) {
        return hasCrushHead() ? Math.max(0, stats.intValue(MachineStat.PROCESSING_LEVEL)) : 0;
    }

    private int effectiveFluidTransfer(MachineStatAccumulator stats) {
        return hasFluidPump() ? Math.max(1, stats.intValue(MachineStat.FLUID_TRANSFER)) : 0;
    }

    private void applyHeatCoreStats(MachineStatAccumulator stats) {
        ItemStack stack = heatCoreStack();
        if (!(stack.getItem() instanceof SolidFuelBurnerPartItem part) || !isHeatCore(stack)) {
            return;
        }
        ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
    }

    private void applyCrushHeadStats(MachineStatAccumulator stats) {
        ItemStack stack = crushHeadStack();
        if (isCrushHead(stack) && stack.getItem() instanceof MachinePartItem part) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        }
    }

    private void applyServoStats(MachineStatAccumulator stats) {
        ItemStack stack = servoStack();
        if (isServo(stack) && stack.getItem() instanceof MachinePartItem part) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        }
    }

    private void applyFluidPumpStats(MachineStatAccumulator stats) {
        ItemStack stack = fluidPumpStack();
        if (isFluidPump(stack) && stack.getItem() instanceof MachinePartItem part) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, stack);
        }
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
    }

    private int energyStored() {
        return internalEnergyStored() + BatteryCellItem.energyStored(batteryCellStack());
    }

    private int energyCapacity() {
        return internalEnergyCapacity() + BatteryCellItem.energyCapacity(batteryCellStack());
    }

    private int internalEnergyCapacity() {
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
        return Math.max(1, (int) Math.round(effectiveStats().value(MachineStat.ENERGY_TRANSFER)));
    }

    private void drainInputContainer() {
        ItemStack stack = processInventory.getStackInSlot(SLOT_FLUID_INPUT_CONTAINER);
        if (stack.isEmpty()) {
            return;
        }
        FluidActionResult result = FluidUtil.tryEmptyContainer(stack, inputTank, Integer.MAX_VALUE, null, true);
        if (result.isSuccess()) {
            processInventory.setStackInSlot(SLOT_FLUID_INPUT_CONTAINER, result.getResult());
            setChanged();
        }
    }

    private void fillOutputContainer() {
        if (!hasSideFluidOutput()) {
            return;
        }
        ItemStack stack = processInventory.getStackInSlot(SLOT_FLUID_OUTPUT_CONTAINER);
        if (stack.isEmpty() || outputTank.isEmpty()) {
            return;
        }
        int maxFill = Math.max(FluidType.BUCKET_VOLUME, effectiveFluidTransfer(effectiveStats()));
        FluidActionResult result = FluidUtil.tryFillContainer(stack, outputTank, maxFill, null, true);
        if (result.isSuccess()) {
            processInventory.setStackInSlot(SLOT_FLUID_OUTPUT_CONTAINER, result.getResult());
            setChanged();
        }
    }

    private boolean hasRequiredComponents() {
        return hasHeatCore() && hasCrushHead();
    }

    private boolean hasHeatCore() {
        return isHeatCore(heatCoreStack());
    }

    private boolean hasCrushHead() {
        return isCrushHead(crushHeadStack());
    }

    private boolean hasBatteryCell() {
        return isBatteryCell(batteryCellStack());
    }

    private boolean hasFluidPump() {
        return isFluidPump(fluidPumpStack());
    }

    private boolean hasSideFluidOutput() {
        return hasFluidPump() && MachineImplicitCatalog.hasBehavior(fluidPumpStack(), MachineBehavior.SIDE_FLUID_OUTPUT);
    }

    private boolean autoPurgesFluidOutput() {
        return MachineImplicitCatalog.hasBehavior(servoStack(), MachineBehavior.AUTO_PURGE);
    }

    private ItemStack primaryStack() {
        return processInventory.getStackInSlot(SLOT_PRIMARY_INPUT);
    }

    private ItemStack secondaryStack() {
        return processInventory.getStackInSlot(SLOT_SECONDARY_INPUT);
    }

    private ItemStack heatCoreStack() {
        return gearInventory.getStackInSlot(SLOT_HEAT_CORE);
    }

    private ItemStack crushHeadStack() {
        return gearInventory.getStackInSlot(SLOT_CRUSH_HEAD);
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private ItemStack servoStack() {
        return gearInventory.getStackInSlot(SLOT_SERVO);
    }

    private ItemStack fluidPumpStack() {
        return gearInventory.getStackInSlot(SLOT_FLUID_PUMP);
    }

    private IEnergyStorage batteryCellEnergyStorage() {
        ItemStack cell = batteryCellStack();
        return cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
    }

    private static ItemStack singleCopy(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
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

    private final class MelterFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> inputTank.getFluidInTank(0);
                case 1 -> outputTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> inputTank.getTankCapacity(0);
                case 1 -> outputTank.getTankCapacity(0);
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && inputTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return resource.isEmpty() ? 0 : inputTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!hasSideFluidOutput()) {
                return FluidStack.EMPTY;
            }
            if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, outputTank.getFluid())) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!hasSideFluidOutput()) {
                return FluidStack.EMPTY;
            }
            int capped = Math.min(maxDrain, effectiveFluidTransfer(effectiveStats()));
            return outputTank.drain(capped, action);
        }
    }

    private final class MelterEnergyStorage implements IEnergyStorage {
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
            if (internalEnergyStored() < internalEnergyCapacity()) {
                return true;
            }
            IEnergyStorage cell = batteryCellEnergyStorage();
            return cell != null && cell.canReceive() && cell.getEnergyStored() < cell.getMaxEnergyStored();
        }
    }
}
