package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.ComponentRecyclerBlock;
import com.rngtech.content.block.HandCrankBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.DisassemblyHeadItem;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.item.PotentialReactorPartItem;
import com.rngtech.content.menu.ComponentRecyclerMenu;
import com.rngtech.content.recipe.ComponentRecyclingRecipe;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.content.recycling.RecyclingData;
import com.rngtech.content.registry.ModBlockEntities;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.util.List;

public class ComponentRecyclerBlockEntity extends BaseMachineBlockEntity implements MenuProvider, MachineInfoProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_PRIMARY = 1;
    public static final int SLOT_OUTPUT_SECONDARY = 2;
    public static final int SLOT_OUTPUT_TERTIARY = 3;
    public static final int PROCESS_SLOT_COUNT = 4;
    public static final int SLOT_BATTERY_CELL = 0;
    public static final int SLOT_DISASSEMBLY_HEAD = 1;
    public static final int SLOT_RECOVERY_FILTER = 2;
    public static final int GEAR_SLOT_COUNT = 3;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_HEAD = 1;
    public static final int STATUS_NO_INPUT = 2;
    public static final int STATUS_INVALID_RECIPE = 3;
    public static final int STATUS_BLOCKED_STAGE = 4;
    public static final int STATUS_OUTPUT_FULL = 5;
    public static final int STATUS_NO_POWER = 6;
    public static final int STATUS_MISSING_CRANK = 7;
    public static final int MANUAL_MIN_PROCESSING_TICKS = 80;
    private static final int MANUAL_RECIPE_TICK_MULTIPLIER = 2;
    private static final int MANUAL_CRANK_INTERVAL_TICKS = 4;
    private static final int MANUAL_ACTIVE_TICKS = 6;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_ENERGY_PER_TICK = 4;
    private static final int DATA_PROCESSING_LEVEL = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_PROCESSING_SPEED = 7;
    private static final int DATA_EFFICIENCY = 8;
    private static final int DATA_ENERGY_USAGE = 9;
    private static final int DATA_ENERGY_CAPACITY_STAT = 10;
    private static final int DATA_ENERGY_TRANSFER = 11;
    private static final int DATA_STABILITY = 12;
    private static final int DATA_REFINEMENT_POTENTIAL = 13;
    private static final int STAT_SCALE = 100;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT && isKnownInput(stack);
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
            if (slot == SLOT_INPUT) {
                resetCycle();
                resetBulkSpeed();
            }
            setChanged();
        }
    };
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (isManual()) {
                return false;
            }
            return switch (slot) {
                case SLOT_BATTERY_CELL -> isBatteryCell(stack);
                case SLOT_DISASSEMBLY_HEAD -> isDisassemblyHead(stack);
                case SLOT_RECOVERY_FILTER -> isRecoveryFilter(stack);
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
                resetBulkSpeed();
            }
            clampInternalEnergy();
            setChanged();
        }
    };
    private final IItemHandler inputHandler = new InputItemHandler();
    private final IItemHandler outputHandler = new OutputItemHandler();
    private final IEnergyStorage energyStorage = new RecyclerEnergyStorage();
    private final EnergyTelemetry energyFlow = new EnergyTelemetry(this::getLevel);
    private final IEnergyStorage trackedEnergyStorage = energyFlow.track(energyStorage);
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            ComponentRecyclingRecipe recipe = nextRecipe();
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> currentProcessingTicks(recipe, stats);
                case DATA_ENERGY -> isManual() ? 0 : energyStored();
                case DATA_ENERGY_CAPACITY -> isManual() ? 0 : energyCapacity();
                case DATA_ENERGY_PER_TICK -> recipe == null || isManual() ? 0 : energyCostPerTick(recipe, stats);
                case DATA_PROCESSING_LEVEL -> stats.intValue(MachineStat.PROCESSING_LEVEL);
                case DATA_STATUS -> statusCode(recipe, stats);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_EFFICIENCY -> scaledStat(stats, MachineStat.EFFICIENCY);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_STABILITY -> scaledStat(stats, MachineStat.STABILITY);
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

    private final BulkSpeedState bulkSpeed = new BulkSpeedState();
    private int progress;
    private int internalEnergy;
    private long nextManualCrankTick;
    private long lastManualCrankTick = Long.MIN_VALUE;

    public ComponentRecyclerBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.COMPONENT_RECYCLER.get(),
                pos,
                blockState,
                MachineType.COMPONENT_RECYCLER,
                SLOT_INPUT,
                SLOT_INPUT,
                SLOT_OUTPUT_PRIMARY
        );
        if (!(blockState.getBlock() instanceof ComponentRecyclerBlock)) {
            throw new IllegalStateException("Component recycler block entity created for non-recycler block: " + blockState);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ComponentRecyclerBlockEntity recycler) {
        if (recycler.isManual()) {
            recycler.serverTickManual(level, pos, state);
            return;
        }
        MachineStatAccumulator stats = recycler.effectiveStats();
        ComponentRecyclingRecipe recipe = recycler.nextRecipe();
        if (recipe == null || !recycler.hasRequiredGear() || !recycler.canProcessStage(recipe, stats)) {
            recycler.resetCycleIfActive();
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        List<ItemStack> outputs = recipe.outputStacks(recycler.hasRecoveryFilter());
        if (outputs.isEmpty() || !recycler.canMergeOutputs(outputs)) {
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        int energyCost = recycler.energyCostPerTick(recipe, stats);
        if (recycler.progress == 0 && ProcessingChance.rollInstant(level, stats)) {
            int fullEnergyCost = recycler.energyCostPerCraft(recipe, stats);
            if (recycler.consumeWorkingEnergy(fullEnergyCost, true) >= fullEnergyCost) {
                recycler.consumeWorkingEnergy(fullEnergyCost, false);
                if (recycler.process(recipe)) {
                    recycler.bulkSpeed.recordProcess(recycler.activeTraits());
                }
                BaseMachineBlock.setActive(level, pos, state, true);
                recycler.setChanged();
                return;
            }
        }
        if (recycler.consumeWorkingEnergy(energyCost, true) < energyCost) {
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        recycler.consumeWorkingEnergy(energyCost, false);
        recycler.progress++;
        if (recycler.progress >= recycler.processingTicks(recipe, stats)) {
            if (recycler.process(recipe)) {
                recycler.bulkSpeed.recordProcess(recycler.activeTraits());
            }
        }

        BaseMachineBlock.setActive(level, pos, state, true);
        recycler.setChanged();
    }

    private void serverTickManual(Level level, BlockPos pos, BlockState state) {
        MachineStatAccumulator stats = effectiveStats();
        ComponentRecyclingRecipe recipe = nextRecipe();
        if (recipe == null || !hasHandCrank() || !canProcessStage(recipe, stats)) {
            resetCycleIfActive();
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        List<ItemStack> outputs = recipe.outputStacks(false);
        if (outputs.isEmpty() || !canMergeOutputs(outputs)) {
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        BaseMachineBlock.setActive(level, pos, state, level.getGameTime() - lastManualCrankTick <= MANUAL_ACTIVE_TICKS);
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

    public boolean isManual() {
        return chassis().manual();
    }

    public boolean turnManualCrank(BlockPos crankPos) {
        if (level == null || !isManual() || !hasHandCrank()) {
            return false;
        }
        long gameTime = level.getGameTime();
        if (gameTime < nextManualCrankTick) {
            return false;
        }

        MachineStatAccumulator stats = effectiveStats();
        ComponentRecyclingRecipe recipe = nextRecipe();
        if (!canManualCrank(recipe, stats)) {
            resetCycleIfActive();
            BaseMachineBlock.setActive(level, worldPosition, getBlockState(), false);
            return false;
        }

        nextManualCrankTick = gameTime + MANUAL_CRANK_INTERVAL_TICKS;
        lastManualCrankTick = gameTime;
        int processingTicks = manualProcessingTicks(recipe);
        progress = Math.min(processingTicks, progress + MANUAL_CRANK_INTERVAL_TICKS);
        playManualGrindSound(crankPos);

        if (progress >= processingTicks && process(recipe)) {
            playManualCompleteSound();
        }

        BaseMachineBlock.setActive(level, worldPosition, getBlockState(), true);
        setChanged();
        return true;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        if (isManual()) {
            return null;
        }
        return side == Direction.UP || side == Direction.DOWN ? null : trackedEnergyStorage;
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == Direction.UP) {
            return inputHandler;
        }
        if (side == Direction.DOWN) {
            return outputHandler;
        }
        return side == null ? processInventory : null;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return processInventory;
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        ComponentRecyclingRecipe recipe = nextRecipe();
        int status = statusCode(recipe, stats);
        boolean manual = isManual();
        int energyDemand = !manual && (status == STATUS_READY || status == STATUS_NO_POWER)
                ? energyCostPerTick(recipe, stats)
                : 0;
        AdjacentEnergyConnector.Info connector = AdjacentEnergyConnector.forSink(level, worldPosition);
        return MachineInfoSnapshot.builder("component_recycler")
                .stage(chassis().stage())
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY,
                                getBlockState().getValue(BaseMachineBlock.ACTIVE),
                                status == STATUS_NO_INPUT
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .progress(progress, currentProcessingTicks(recipe, stats))
                .energy(manual ? 0 : energyStored(), manual ? 0 : energyCapacity(), -energyDemand)
                .energyTelemetry(
                        energyFlow.lastInput(),
                        energyFlow.lastOutput(),
                        manual ? 0 : connector.transferRate(),
                        manual ? MachineInfoSnapshot.EnergyBottleneck.NONE : AdjacentEnergyConnector.inputBottleneck(connector, energyDemand)
                )
                .processingLevel(
                        stats.intValue(MachineStat.PROCESSING_LEVEL),
                        recipe == null ? MachineInfoSnapshot.UNSET : recipe.minimumProcessingLevel()
                )
                .output(status == STATUS_OUTPUT_FULL
                        ? MachineInfoSnapshot.OutputSummary.OUTPUT_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .refinement(machineTraits())
                .build();
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_MISSING_HEAD -> "missing_head";
            case STATUS_NO_INPUT -> "no_input";
            case STATUS_INVALID_RECIPE -> "invalid_recipe";
            case STATUS_BLOCKED_STAGE -> "blocked_stage";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_NO_POWER -> "no_power";
            case STATUS_MISSING_CRANK -> "missing_crank";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.recycler.status." + name;
    }

    @Override
    public Component getDisplayName() {
        return MachineNameGenerator.generatedName(
                machineTraits(),
                Component.translatable("container.rngtech." + chassis().blockId())
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ComponentRecyclerMenu(containerId, playerInventory, this, menuData);
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

    public boolean isKnownInput(ItemStack stack) {
        if (level == null) {
            return !isManual() || isManualInput(stack);
        }
        return ComponentRecyclingRecipes.find(level, stack)
                .filter(recipe -> !isManual() || isManualRecipeInput(stack, recipe))
                .isPresent();
    }

    public static boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public boolean isDisassemblyHead(ItemStack stack) {
        return stack.getItem() instanceof DisassemblyHeadItem head
                && !RecyclingData.isStripped(stack)
                && head.stage() <= chassis().stage();
    }

    public boolean isRecoveryFilter(ItemStack stack) {
        return stack.getItem() instanceof PotentialReactorPartItem part
                && !RecyclingData.isStripped(stack)
                && part.partType() == MachinePartType.RECOVERY_FILTER
                && part.machineType() == MachineType.POTENTIAL_REACTOR
                && part.stage() <= chassis().stage();
    }

    public MachineStatAccumulator effectiveStats() {
        ComponentRecyclerChassis chassis = chassis();
        MachineStatAccumulator stats = MachineBaseStatCatalog.componentRecycler(chassis);
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
        if (!chassis.manual()) {
            applyGearStats(stats, headStack());
            applyGearStats(stats, filterStack());
            bulkSpeed.apply(stats, activeTraits());
        }
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
        tag.putInt("Progress", progress);
        tag.putInt("Energy", internalEnergyStored());
        bulkSpeed.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        progress = tag.getInt("Progress");
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        bulkSpeed.load(tag);
        clampInternalEnergy();
    }

    private ComponentRecyclerChassis chassis() {
        return getBlockState().getBlock() instanceof ComponentRecyclerBlock block
                ? block.chassis()
                : ComponentRecyclerChassis.IRON;
    }

    private ComponentRecyclingRecipe nextRecipe() {
        return level == null
                ? null
                : ComponentRecyclingRecipes.find(level, processInventory.getStackInSlot(SLOT_INPUT))
                        .filter(recipe -> !isManual() || isManualRecipeInput(inputStack(), recipe))
                        .orElse(null);
    }

    private boolean process(ComponentRecyclingRecipe recipe) {
        if (level == null || !recipe.matches(new net.minecraft.world.item.crafting.SingleRecipeInput(inputStack()), level)) {
            return false;
        }
        if (isManual() && !isManualRecipeInput(inputStack(), recipe)) {
            return false;
        }
        // Recycling returns the craft's own inputs, so Super Output would duplicate them.
        List<ItemStack> outputs = recipe.outputStacks(hasRecoveryFilter());
        if (outputs.isEmpty() || !canMergeOutputs(outputs)) {
            return false;
        }
        consumeInput();
        mergeOutputs(outputs);
        resetCycle();
        setChanged();
        return true;
    }

    private void consumeInput() {
        ItemStack input = inputStack();
        ItemStack remainder = input.getCraftingRemainingItem();
        input.shrink(1);
        if (!remainder.isEmpty()) {
            if (input.isEmpty()) {
                processInventory.setStackInSlot(SLOT_INPUT, remainder);
            } else if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder);
            }
        }
    }

    private boolean canMergeOutputs(List<ItemStack> outputs) {
        ItemStack[] simulated = {
                processInventory.getStackInSlot(SLOT_OUTPUT_PRIMARY).copy(),
                processInventory.getStackInSlot(SLOT_OUTPUT_SECONDARY).copy(),
                processInventory.getStackInSlot(SLOT_OUTPUT_TERTIARY).copy()
        };
        for (ItemStack output : outputs) {
            if (!mergeIntoSimulatedOutputs(simulated, output.copy())) {
                return false;
            }
        }
        return true;
    }

    private boolean mergeIntoSimulatedOutputs(ItemStack[] slots, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        for (int index = 0; index < slots.length; index++) {
            ItemStack existing = slots[index];
            int slotLimit = Math.min(existing.getMaxStackSize(), processInventory.getSlotLimit(outputSlot(index)));
            if (!existing.isEmpty()
                    && ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < slotLimit) {
                int room = slotLimit - existing.getCount();
                int moved = Math.min(room, stack.getCount());
                existing.grow(moved);
                stack.shrink(moved);
                if (stack.isEmpty()) {
                    return true;
                }
            }
        }
        for (int index = 0; index < slots.length; index++) {
            if (slots[index].isEmpty()) {
                int slotLimit = Math.min(stack.getMaxStackSize(), processInventory.getSlotLimit(outputSlot(index)));
                int moved = Math.min(slotLimit, stack.getCount());
                if (moved <= 0) {
                    continue;
                }
                slots[index] = stack.copyWithCount(moved);
                stack.shrink(moved);
                if (stack.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void mergeOutputs(List<ItemStack> outputs) {
        for (ItemStack output : outputs) {
            mergeOutput(output.copy());
        }
    }

    private void mergeOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (int slot = SLOT_OUTPUT_PRIMARY; slot <= SLOT_OUTPUT_TERTIARY; slot++) {
            ItemStack existing = processInventory.getStackInSlot(slot);
            if (!existing.isEmpty()
                    && ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < Math.min(existing.getMaxStackSize(), processInventory.getSlotLimit(slot))) {
                int room = Math.min(existing.getMaxStackSize(), processInventory.getSlotLimit(slot)) - existing.getCount();
                int moved = Math.min(room, stack.getCount());
                existing.grow(moved);
                stack.shrink(moved);
                processInventory.setStackInSlot(slot, existing);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
        for (int slot = SLOT_OUTPUT_PRIMARY; slot <= SLOT_OUTPUT_TERTIARY; slot++) {
            if (processInventory.getStackInSlot(slot).isEmpty()) {
                int slotLimit = Math.min(stack.getMaxStackSize(), processInventory.getSlotLimit(slot));
                int moved = Math.min(slotLimit, stack.getCount());
                if (moved <= 0) {
                    continue;
                }
                processInventory.setStackInSlot(slot, stack.copyWithCount(moved));
                stack.shrink(moved);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
    }

    private static int outputSlot(int index) {
        return SLOT_OUTPUT_PRIMARY + index;
    }

    private int processingTicks(ComponentRecyclingRecipe recipe, MachineStatAccumulator stats) {
        if (isManual()) {
            return manualProcessingTicks(recipe);
        }
        return stats.adjustedProcessingTicks(recipe.processingTicks());
    }

    private int manualProcessingTicks(ComponentRecyclingRecipe recipe) {
        if (recipe == null || recipe.processingTicks() <= MANUAL_MIN_PROCESSING_TICKS) {
            return MANUAL_MIN_PROCESSING_TICKS;
        }
        return recipe.processingTicks() * MANUAL_RECIPE_TICK_MULTIPLIER;
    }

    private int energyCostPerTick(ComponentRecyclingRecipe recipe, MachineStatAccumulator stats) {
        int adjustedTicks = processingTicks(recipe, stats);
        int totalEnergy = stats.adjustedEnergyCost(recipe.energy());
        return Math.max(1, (int) Math.ceil(totalEnergy / (double) adjustedTicks));
    }

    private int energyCostPerCraft(ComponentRecyclingRecipe recipe, MachineStatAccumulator stats) {
        return stats.adjustedEnergyCost(recipe.energy());
    }

    private int currentProcessingTicks(ComponentRecyclingRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null || !canProcessStage(recipe, stats) ? 0 : processingTicks(recipe, stats);
    }

    private int statusCode(ComponentRecyclingRecipe recipe, MachineStatAccumulator stats) {
        if (isManual()) {
            return manualStatusCode(recipe, stats);
        }
        if (!hasRequiredGear()) {
            return STATUS_MISSING_HEAD;
        }
        if (inputStack().isEmpty()) {
            return STATUS_NO_INPUT;
        }
        if (recipe == null) {
            return STATUS_INVALID_RECIPE;
        }
        if (!canProcessStage(recipe, stats)) {
            return STATUS_BLOCKED_STAGE;
        }
        if (!canMergeOutputs(recipe.outputStacks(hasRecoveryFilter()))) {
            return STATUS_OUTPUT_FULL;
        }
        int energyCost = energyCostPerTick(recipe, stats);
        return consumeWorkingEnergy(energyCost, true) < energyCost ? STATUS_NO_POWER : STATUS_READY;
    }

    private int manualStatusCode(ComponentRecyclingRecipe recipe, MachineStatAccumulator stats) {
        if (!hasHandCrank()) {
            return STATUS_MISSING_CRANK;
        }
        if (inputStack().isEmpty()) {
            return STATUS_NO_INPUT;
        }
        if (recipe == null) {
            return STATUS_INVALID_RECIPE;
        }
        if (!canProcessStage(recipe, stats)) {
            return STATUS_BLOCKED_STAGE;
        }
        if (!canMergeOutputs(recipe.outputStacks(false))) {
            return STATUS_OUTPUT_FULL;
        }
        return STATUS_READY;
    }

    private boolean canManualCrank(ComponentRecyclingRecipe recipe, MachineStatAccumulator stats) {
        return recipe != null
                && hasHandCrank()
                && canProcessStage(recipe, stats)
                && canMergeOutputs(recipe.outputStacks(false));
    }

    private boolean canProcessStage(ComponentRecyclingRecipe recipe, MachineStatAccumulator stats) {
        if (isManual()) {
            return recipe.minimumProcessingLevel() <= chassis().stage();
        }
        return recipe.minimumProcessingLevel() <= chassis().stage()
                && stats.intValue(MachineStat.PROCESSING_LEVEL) >= recipe.minimumProcessingLevel();
    }

    private boolean hasRequiredGear() {
        return isDisassemblyHead(headStack());
    }

    private boolean hasRecoveryFilter() {
        return !isManual() && isRecoveryFilter(filterStack());
    }

    private boolean hasHandCrank() {
        return level != null && HandCrankBlock.isHandCrank(level.getBlockState(worldPosition.above()));
    }

    private void playManualGrindSound(BlockPos crankPos) {
        if (level != null) {
            float pitch = 0.82F + (progress % 16) * 0.01F;
            level.playSound(null, crankPos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.55F, pitch);
        }
    }

    private void playManualCompleteSound() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.45F, 1.25F);
        }
    }

    private boolean isManualInput(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModTags.Items.MANUAL_RECYCLER_INPUTS);
    }

    private boolean isManualRecipeInput(ItemStack stack, ComponentRecyclingRecipe recipe) {
        return isManualInput(stack) && recipe.minimumProcessingLevel() <= chassis().stage();
    }

    private void resetCycleIfActive() {
        boolean changed = false;
        if (progress != 0) {
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
    }

    private void resetBulkSpeed() {
        if (bulkSpeed.reset()) {
            setChanged();
        }
    }

    private void applyGearStats(MachineStatAccumulator stats, ItemStack stack) {
        if (stack.getItem() instanceof MachinePartItem part) {
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

    private IEnergyStorage batteryCellEnergyStorage() {
        ItemStack cell = batteryCellStack();
        return cell.isEmpty() ? null : cell.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
    }

    private ItemStack inputStack() {
        return processInventory.getStackInSlot(SLOT_INPUT);
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
    }

    private ItemStack headStack() {
        return gearInventory.getStackInSlot(SLOT_DISASSEMBLY_HEAD);
    }

    private ItemStack filterStack() {
        return gearInventory.getStackInSlot(SLOT_RECOVERY_FILTER);
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private final class InputItemHandler implements IItemHandler {
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
            if (slot != 0) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
            }
            return SLOT_INPUT;
        }
    }

    private final class OutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 3;
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
            if (slot < 0 || slot >= 3) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0,3)");
            }
            return SLOT_OUTPUT_PRIMARY + slot;
        }
    }

    private final class RecyclerEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (!canReceive() || toReceive <= 0) {
                return 0;
            }
            int remaining = toReceive;
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
