package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.menu.BatteryAssemblerMenu;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.BatteryAssemblyRecipe;
import com.rngtech.content.recipe.BatteryAssemblyRecipeInput;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModTags;
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

import java.util.Arrays;
import java.util.List;

public class BatteryAssemblerBlockEntity extends BaseMachineBlockEntity implements MenuProvider, PurgeableFluidStorage {
    public static final int SLOT_INPUT_0 = 0;
    public static final int SLOT_INPUT_1 = 1;
    public static final int SLOT_INPUT_2 = 2;
    public static final int SLOT_INPUT_3 = 3;
    public static final int SLOT_ELECTROLYTE_INPUT = 4;
    public static final int SLOT_OUTPUT_0 = 5;
    public static final int SLOT_OUTPUT_1 = 6;
    public static final int SLOT_OUTPUT_2 = 7;
    public static final int SLOT_OUTPUT_3 = 8;
    public static final int ITEM_INPUT_SLOT_COUNT = 4;
    public static final int OUTPUT_SLOT_COUNT = 4;
    public static final int PROCESS_SLOT_COUNT = 9;
    public static final int SLOT_BATTERY_CELL = 0;
    public static final int GEAR_SLOT_COUNT = 1;

    public static final int STATUS_READY = 0;
    public static final int STATUS_NO_INPUT = 1;
    public static final int STATUS_INVALID_RECIPE = 2;
    public static final int STATUS_NO_FLUID = 3;
    public static final int STATUS_OUTPUT_FULL = 4;
    public static final int STATUS_NO_POWER = 5;
    public static final int PURGE_ASSEMBLY_FLUID = 0;

    public static final int ELECTROLYTE_PER_REAGENT = 125;

    private static final int TANK_CAPACITY = FluidType.BUCKET_VOLUME * 4;
    private static final double NO_BATTERY_PROCESSING_SPEED = 0.85;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_FLUID = 4;
    private static final int DATA_FLUID_CAPACITY = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_PROCESSING_SPEED = 7;
    private static final int DATA_ENERGY_USAGE = 8;
    private static final int DATA_ENERGY_CAPACITY_STAT = 9;
    private static final int DATA_ENERGY_TRANSFER = 10;
    private static final int DATA_FLUID_TRANSFER = 11;
    private static final int DATA_REFINEMENT_POTENTIAL = 12;
    private static final int DATA_FLUID_ID = 13;
    private static final int STAT_SCALE = 100;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_INPUT_0, SLOT_INPUT_1, SLOT_INPUT_2, SLOT_INPUT_3 -> !stack.isEmpty();
                case SLOT_ELECTROLYTE_INPUT -> isElectrolyteReagent(stack) || isFluidInputContainer(stack);
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
            if (slot >= SLOT_INPUT_0 && slot <= SLOT_INPUT_3) {
                resetCycle();
                resetBulkSpeed();
            }
            setChanged();
        }
    };
    private final ItemStackHandler gearInventory = new ItemStackHandler(GEAR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_BATTERY_CELL && BatteryCellItem.isBatteryCell(stack);
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
    private final FluidTank inputTank = new FluidTank(TANK_CAPACITY, BatteryAssemblerBlockEntity::isValidAssemblyFluid) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IItemHandler itemInputHandler = new RangedItemHandler(SLOT_INPUT_0, SLOT_INPUT_3, true, false);
    private final IItemHandler electrolyteInputHandler = new ElectrolyteItemHandler();
    private final IItemHandler outputHandler = new RangedItemHandler(SLOT_OUTPUT_0, SLOT_OUTPUT_3, false, true);
    private final IEnergyStorage energyStorage = new BatteryAssemblerEnergyStorage();
    private final IFluidHandler fluidHandler = new BatteryAssemblerFluidHandler();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            MachineStatAccumulator stats = effectiveStats();
            BatteryAssemblyRecipe recipe = nextRecipe();
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESSING_TICKS -> currentProcessingTicks(recipe, stats);
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_FLUID -> inputTank.getFluidAmount();
                case DATA_FLUID_CAPACITY -> inputTank.getCapacity();
                case DATA_STATUS -> statusCode(recipe, stats);
                case DATA_PROCESSING_SPEED -> scaledStat(stats, MachineStat.PROCESSING_SPEED);
                case DATA_ENERGY_USAGE -> scaledStat(stats, MachineStat.ENERGY_USAGE);
                case DATA_ENERGY_CAPACITY_STAT -> scaledStat(stats, MachineStat.ENERGY_CAPACITY);
                case DATA_ENERGY_TRANSFER -> scaledStat(stats, MachineStat.ENERGY_TRANSFER);
                case DATA_FLUID_TRANSFER -> scaledStat(stats, MachineStat.FLUID_TRANSFER);
                case DATA_REFINEMENT_POTENTIAL -> scaledStat(stats, MachineStat.REFINEMENT_POTENTIAL);
                case DATA_FLUID_ID -> BuiltInRegistries.FLUID.getId(inputTank.getFluid().getFluid());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_FLUID_ID + 1;
        }
    };

    private final BulkSpeedState bulkSpeed = new BulkSpeedState();
    private int progress;
    private int internalEnergy;
    private ItemStack[] activeInputs = emptyInputs();
    private FluidStack activeFluid = FluidStack.EMPTY;

    public BatteryAssemblerBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.BATTERY_ASSEMBLER.get(),
                pos,
                blockState,
                MachineType.BATTERY_ASSEMBLER,
                SLOT_INPUT_0,
                SLOT_ELECTROLYTE_INPUT,
                SLOT_OUTPUT_0
        );
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryAssemblerBlockEntity assembler) {
        assembler.dissolveElectrolyteReagent();
        assembler.drainInputContainer();

        MachineStatAccumulator stats = assembler.effectiveStats();
        BatteryAssemblyRecipe recipe = assembler.nextRecipe();
        if (recipe == null) {
            assembler.resetCycleIfActive();
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }
        if (!assembler.activeCycleMatches()) {
            assembler.resetCycle();
            assembler.resetBulkSpeed();
        }
        if (!assembler.canAcceptOutput(recipe.outputStack())) {
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        int energyCost = assembler.energyCostPerTick(recipe, stats);
        if (assembler.progress == 0 && ProcessingChance.rollInstant(level, stats)) {
            int fullEnergyCost = assembler.energyCostPerCraft(recipe, stats);
            if (assembler.consumeWorkingEnergy(fullEnergyCost, true) >= fullEnergyCost) {
                assembler.startCycleIfNeeded(recipe);
                assembler.consumeWorkingEnergy(fullEnergyCost, false);
                if (assembler.process(recipe)) {
                    assembler.bulkSpeed.recordProcess(assembler.activeTraits());
                }
                BaseMachineBlock.setActive(level, pos, state, true);
                assembler.setChanged();
                return;
            }
        }
        if (assembler.consumeWorkingEnergy(energyCost, true) < energyCost) {
            BaseMachineBlock.setActive(level, pos, state, false);
            return;
        }

        assembler.startCycleIfNeeded(recipe);
        assembler.consumeWorkingEnergy(energyCost, false);
        assembler.progress++;
        if (assembler.progress >= assembler.processingTicks(recipe, stats)) {
            if (assembler.process(recipe)) {
                assembler.bulkSpeed.recordProcess(assembler.activeTraits());
            }
        }

        BaseMachineBlock.setActive(level, pos, state, true);
        assembler.setChanged();
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

    public ContainerData getMenuData() {
        return menuData;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return energyStorage;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return side == null ? null : fluidHandler;
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        return List.of(FluidPurgeTarget.of(
                PURGE_ASSEMBLY_FLUID,
                Component.translatable("rngtech.purge.target.assembly_fluid"),
                FluidPurgeRole.INPUT,
                inputTank::getFluid,
                inputTank::drain,
                this::resetCycleAndBulkSpeed,
                this::setChanged
        ));
    }

    @Override
    public IItemHandler getItemHandler(Direction side) {
        if (side == Direction.UP) {
            return itemInputHandler;
        }
        if (side == Direction.DOWN) {
            return outputHandler;
        }
        if (side != null) {
            return electrolyteInputHandler;
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
                Component.translatable("container.rngtech.battery_assembler")
        );
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BatteryAssemblerMenu(containerId, playerInventory, this, menuData);
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
        MachineStatAccumulator stats = MachineBaseStatCatalog.batteryAssembler();
        stats.apply(MachineImplicitCatalog.effectiveTraits(machineTraits(), getBlockState().getBlock()));
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

    public static boolean isBatteryCell(ItemStack stack) {
        return BatteryCellItem.isBatteryCell(stack);
    }

    public static boolean isElectrolyteReagent(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ModItems.materialItem("electrolyte").get();
    }

    public static boolean isFluidInputContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return FluidUtil.getFluidContained(stack).map(BatteryAssemblerBlockEntity::isValidAssemblyFluid).orElse(false);
    }

    private static boolean isValidElectrolyte(FluidStack stack) {
        return !stack.isEmpty() && stack.is(ModTags.Fluids.ELECTROLYTES);
    }

    private static boolean isValidAssemblyFluid(FluidStack stack) {
        return isValidElectrolyte(stack) || (!stack.isEmpty() && stack.is(ModTags.Fluids.LUBRICANTS));
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
        tag.putInt("Progress", progress);
        tag.putInt("Energy", internalEnergyStored());
        for (int slot = 0; slot < activeInputs.length; slot++) {
            tag.put("ActiveInput" + slot, activeInputs[slot].saveOptional(registries));
        }
        tag.put("ActiveFluid", activeFluid.saveOptional(registries));
        bulkSpeed.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        progress = tag.getInt("Progress");
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        activeInputs = emptyInputs();
        for (int slot = 0; slot < activeInputs.length; slot++) {
            activeInputs[slot] = ItemStack.parseOptional(registries, tag.getCompound("ActiveInput" + slot));
        }
        activeFluid = FluidStack.parseOptional(registries, tag.getCompound("ActiveFluid"));
        bulkSpeed.load(tag);
        clampInternalEnergy();
    }

    private BatteryAssemblyRecipe nextRecipe() {
        return BatteryAssemblyRecipes.find(level, itemInputs(), inputTank.getFluid()).orElse(null);
    }

    private boolean process(BatteryAssemblyRecipe recipe) {
        if (level == null || !recipe.matches(new BatteryAssemblyRecipeInput(Arrays.asList(itemInputs()), inputTank.getFluid()), level)) {
            return false;
        }

        ItemStack result = recipe.outputStack();
        if (recipe.rollResultTraits()) {
            BatteryCellItem.rollTraitsIfMissing(result, level);
        }
        if (!canAcceptOutput(result)) {
            return false;
        }

        for (int slot = 0; slot < recipe.ingredients().size(); slot++) {
            consumeInput(slot, recipe.ingredients().get(slot).count());
        }
        recipe.consumeFluid(inputTank);
        addOutput(result);
        resetCycle();
        setChanged();
        return true;
    }

    private void consumeInput(int slot, int count) {
        for (int index = 0; index < count; index++) {
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
    }

    private boolean canAcceptOutput(ItemStack result) {
        return remainingAfterInsert(result, true).isEmpty();
    }

    private void addOutput(ItemStack result) {
        remainingAfterInsert(result, false);
    }

    private ItemStack remainingAfterInsert(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = SLOT_OUTPUT_0; slot <= SLOT_OUTPUT_3 && !remaining.isEmpty(); slot++) {
            ItemStack existing = processInventory.getStackInSlot(slot);
            int limit = Math.min(processInventory.getSlotLimit(slot), remaining.getMaxStackSize());
            if (existing.isEmpty()) {
                int moved = Math.min(limit, remaining.getCount());
                if (!simulate) {
                    ItemStack inserted = remaining.copyWithCount(moved);
                    processInventory.setStackInSlot(slot, inserted);
                }
                remaining.shrink(moved);
            } else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int moved = Math.min(limit - existing.getCount(), remaining.getCount());
                if (moved > 0) {
                    if (!simulate) {
                        existing.grow(moved);
                        processInventory.setStackInSlot(slot, existing);
                    }
                    remaining.shrink(moved);
                }
            }
        }
        return remaining;
    }

    private int processingTicks(BatteryAssemblyRecipe recipe, MachineStatAccumulator stats) {
        return stats.adjustedProcessingTicks(recipe.processingTicks());
    }

    private int energyCostPerTick(BatteryAssemblyRecipe recipe, MachineStatAccumulator stats) {
        int adjustedTicks = processingTicks(recipe, stats);
        int totalEnergy = stats.adjustedEnergyCost(recipe.energy());
        return Math.max(1, (int) Math.ceil(totalEnergy / (double) adjustedTicks));
    }

    private int energyCostPerCraft(BatteryAssemblyRecipe recipe, MachineStatAccumulator stats) {
        return stats.adjustedEnergyCost(recipe.energy());
    }

    private int currentProcessingTicks(BatteryAssemblyRecipe recipe, MachineStatAccumulator stats) {
        return recipe == null ? 0 : processingTicks(recipe, stats);
    }

    private int statusCode(BatteryAssemblyRecipe recipe, MachineStatAccumulator stats) {
        if (Arrays.stream(itemInputs()).allMatch(ItemStack::isEmpty)) {
            return STATUS_NO_INPUT;
        }
        if (recipe == null) {
            return inputTank.isEmpty() ? STATUS_NO_FLUID : STATUS_INVALID_RECIPE;
        }
        if (!canAcceptOutput(recipe.outputStack())) {
            return STATUS_OUTPUT_FULL;
        }
        int energyCost = energyCostPerTick(recipe, stats);
        return consumeWorkingEnergy(energyCost, true) < energyCost ? STATUS_NO_POWER : STATUS_READY;
    }

    private void startCycleIfNeeded(BatteryAssemblyRecipe recipe) {
        if (progress != 0) {
            return;
        }
        activeInputs = emptyInputs();
        for (int slot = 0; slot < ITEM_INPUT_SLOT_COUNT; slot++) {
            activeInputs[slot] = singleCopy(inputStack(slot));
        }
        activeFluid = recipe.fluidInput()
                .map(fluid -> inputTank.getFluid().copyWithAmount(fluid.amount()))
                .orElse(FluidStack.EMPTY);
    }

    private boolean activeCycleMatches() {
        if (progress == 0) {
            return true;
        }
        for (int slot = 0; slot < ITEM_INPUT_SLOT_COUNT; slot++) {
            ItemStack active = activeInputs[slot];
            ItemStack current = inputStack(slot);
            if (active.isEmpty()) {
                if (!current.isEmpty()) {
                    return false;
                }
            } else if (!ItemStack.isSameItemSameComponents(active, current) || current.getCount() < active.getCount()) {
                return false;
            }
        }
        return activeFluid.isEmpty()
                || (FluidStack.isSameFluidSameComponents(activeFluid, inputTank.getFluid())
                        && inputTank.getFluidAmount() >= activeFluid.getAmount());
    }

    private void resetCycleIfActive() {
        if (progress != 0 || Arrays.stream(activeInputs).anyMatch(stack -> !stack.isEmpty()) || !activeFluid.isEmpty()) {
            resetCycle();
            resetBulkSpeed();
            setChanged();
        }
    }

    private void resetCycle() {
        progress = 0;
        activeInputs = emptyInputs();
        activeFluid = FluidStack.EMPTY;
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

    private void dissolveElectrolyteReagent() {
        ItemStack stack = processInventory.getStackInSlot(SLOT_ELECTROLYTE_INPUT);
        if (!isElectrolyteReagent(stack)) {
            return;
        }
        FluidStack electrolyte = new FluidStack(ModFluids.ELECTROLYTE_SOLUTION_SOURCE.get(), ELECTROLYTE_PER_REAGENT);
        if (inputTank.fill(electrolyte, IFluidHandler.FluidAction.SIMULATE) < ELECTROLYTE_PER_REAGENT) {
            return;
        }
        stack.shrink(1);
        inputTank.fill(electrolyte, IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    private void drainInputContainer() {
        ItemStack stack = processInventory.getStackInSlot(SLOT_ELECTROLYTE_INPUT);
        if (stack.isEmpty() || !isFluidInputContainer(stack)) {
            return;
        }
        FluidActionResult result = FluidUtil.tryEmptyContainer(stack, inputTank, Integer.MAX_VALUE, null, true);
        if (result.isSuccess()) {
            processInventory.setStackInSlot(SLOT_ELECTROLYTE_INPUT, result.getResult());
            setChanged();
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

    private int effectiveFluidTransfer() {
        return Math.max(1, (int) Math.round(effectiveStats().value(MachineStat.FLUID_TRANSFER)));
    }

    private boolean hasBatteryCell() {
        return BatteryCellItem.isBatteryCell(batteryCellStack());
    }

    private ItemStack[] itemInputs() {
        ItemStack[] inputs = new ItemStack[ITEM_INPUT_SLOT_COUNT];
        for (int slot = 0; slot < ITEM_INPUT_SLOT_COUNT; slot++) {
            inputs[slot] = inputStack(slot);
        }
        return inputs;
    }

    private ItemStack inputStack(int slot) {
        return processInventory.getStackInSlot(slot);
    }

    private ItemStack batteryCellStack() {
        return gearInventory.getStackInSlot(SLOT_BATTERY_CELL);
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
        copy.setCount(Math.min(copy.getCount(), 1));
        return copy;
    }

    private static ItemStack[] emptyInputs() {
        ItemStack[] stacks = new ItemStack[ITEM_INPUT_SLOT_COUNT];
        Arrays.fill(stacks, ItemStack.EMPTY);
        return stacks;
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private final class RangedItemHandler implements IItemHandler {
        private final int firstSlot;
        private final int lastSlot;
        private final boolean allowInsert;
        private final boolean allowExtract;

        private RangedItemHandler(int firstSlot, int lastSlot, boolean allowInsert, boolean allowExtract) {
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
            return allowInsert ? processInventory.insertItem(mappedSlot(slot), stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return allowExtract ? processInventory.extractItem(mappedSlot(slot), amount, simulate) : ItemStack.EMPTY;
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

    private final class ElectrolyteItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(SLOT_ELECTROLYTE_INPUT);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return processInventory.insertItem(SLOT_ELECTROLYTE_INPUT, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = processInventory.getStackInSlot(SLOT_ELECTROLYTE_INPUT);
            if (isElectrolyteReagent(stack) || isFluidInputContainer(stack)) {
                return ItemStack.EMPTY;
            }
            return processInventory.extractItem(SLOT_ELECTROLYTE_INPUT, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(SLOT_ELECTROLYTE_INPUT);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return processInventory.isItemValid(SLOT_ELECTROLYTE_INPUT, stack);
        }
    }

    private final class BatteryAssemblerFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? inputTank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? inputTank.getTankCapacity(0) : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && inputTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            return inputTank.fill(resource.copyWithAmount(Math.min(resource.getAmount(), effectiveFluidTransfer())), action);
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

    private final class BatteryAssemblerEnergyStorage implements IEnergyStorage {
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
