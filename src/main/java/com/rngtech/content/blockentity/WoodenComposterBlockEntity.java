package com.rngtech.content.blockentity;

import com.rngtech.content.block.WoodenComposterBlock;
import com.rngtech.content.menu.WoodenComposterMenu;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModTags;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WoodenComposterBlockEntity extends BlockEntity
        implements MenuProvider, PurgeableFluidStorage, MachineInfoProvider {
    public static final int INPUT_SLOT_COUNT = 27;
    public static final int SLOT_OUTPUT = 27;
    public static final int SLOT_WATER_INPUT_CONTAINER = 28;
    public static final int SLOT_WATER_OUTPUT_CONTAINER = 29;
    public static final int SLOT_COUNT = 30;

    public static final int STATUS_READY = 0;
    public static final int STATUS_NO_INPUT = 1;
    public static final int STATUS_OUTPUT_FULL = 2;
    public static final int STATUS_COMPOSTING = 3;
    public static final int PURGE_WATER_TANK = 0;

    public static final int COMPOST_UNITS_PER_BATCH = 8;
    public static final int PREPARED_INPUT_COMPOST_UNITS = 2;
    public static final int DRY_COMPOST_TICKS = 2400;
    public static final int WET_COMPOST_TICKS = 1200;
    public static final int WATER_PER_BATCH = 10;
    private static final int WATER_CAPACITY = 4 * FluidType.BUCKET_VOLUME;
    public static final int BULK_BONUS_UNIT_THRESHOLD = 200;
    public static final int VARIETY_BONUS_ITEM_THRESHOLD = 5;
    private static final double SPEED_BONUS_PER_ACTIVE = 0.1D;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_REQUIRED_TICKS = 1;
    private static final int DATA_AVAILABLE_ITEMS = 2;
    private static final int DATA_WATER = 3;
    private static final int DATA_WATER_CAPACITY = 4;
    private static final int DATA_STATUS = 5;
    private static final int DATA_WET_BATCH = 6;
    private static final int DATA_VARIETY_COUNT = 7;
    private static final int DATA_SPEED_BONUS_PERCENT = 8;
    private static final int DATA_COUNT = 9;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= 0 && slot < INPUT_SLOT_COUNT) {
                return isCompostable(stack);
            }
            return slot == SLOT_WATER_INPUT_CONTAINER && isFluidInputContainer(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_WATER_INPUT_CONTAINER ? 1 : super.getSlotLimit(slot);
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
    private final FluidTank waterTank = new FluidTank(WATER_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return isWater(stack);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IItemHandler inputHandler = new InputItemHandler();
    private final IItemHandler outputHandler = new OutputItemHandler();
    private final IFluidHandler fluidInputHandler = new WaterInputFluidHandler();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_REQUIRED_TICKS -> currentRequiredTicks();
                case DATA_AVAILABLE_ITEMS -> availableCompostUnits();
                case DATA_WATER -> waterTank.getFluidAmount();
                case DATA_WATER_CAPACITY -> waterTank.getCapacity();
                case DATA_STATUS -> statusCode();
                case DATA_WET_BATCH -> wateredBatch ? 1 : 0;
                case DATA_VARIETY_COUNT -> varietyCount();
                case DATA_SPEED_BONUS_PERCENT -> speedBonusPercent();
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

    private int progress;
    private boolean wateredBatch;

    public WoodenComposterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.WOODEN_COMPOSTER.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WoodenComposterBlockEntity composter) {
        boolean changed = composter.drainWaterInputContainer();
        changed |= composter.tickComposting();
        WoodenComposterBlock.setComposting(level, pos, state, composter.isActivelyComposting());
        if (changed) {
            composter.setChanged();
        }
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public IItemHandler getItemHandler(Direction side) {
        if (side == null) {
            return inventory;
        }
        return side == Direction.DOWN ? outputHandler : inputHandler;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return side == Direction.DOWN ? null : fluidInputHandler;
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        return List.of(FluidPurgeTarget.of(
                PURGE_WATER_TANK,
                Component.translatable("rngtech.purge.target.water_tank"),
                FluidPurgeRole.INPUT,
                waterTank::getFluid,
                waterTank::drain,
                this::setChanged
        ).withCapacity(waterTank::getCapacity));
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        int status = statusCode();
        return MachineInfoSnapshot.builder("wooden_composter")
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY || status == STATUS_COMPOSTING,
                                status == STATUS_COMPOSTING,
                                status == STATUS_NO_INPUT
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .progress(progress, currentRequiredTicks())
                .output(status == STATUS_OUTPUT_FULL
                        ? MachineInfoSnapshot.OutputSummary.OUTPUT_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .build();
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_NO_INPUT -> "no_input";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.wooden_composter.status." + name;
    }

    public static boolean isCompostable(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModTags.Items.WOODEN_COMPOSTER_INPUTS)
                        || stack.is(ModTags.Items.WOODEN_COMPOSTER_PREPARED_INPUTS)
                        || ComposterBlock.getValue(stack) > 0.0F);
    }

    public static int compostUnits(ItemStack stack) {
        if (!isCompostable(stack)) {
            return 0;
        }
        return stack.is(ModTags.Items.WOODEN_COMPOSTER_PREPARED_INPUTS) ? PREPARED_INPUT_COMPOST_UNITS : 1;
    }

    public static boolean isFluidInputContainer(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).filter(WoodenComposterBlockEntity::isWater).isPresent();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.wooden_composter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WoodenComposterMenu(containerId, playerInventory, this, menuData);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }

    public void dropInventory(Level level) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
        tag.putBoolean("WateredBatch", wateredBatch);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        progress = Math.max(0, tag.getInt("Progress"));
        wateredBatch = tag.getBoolean("WateredBatch");
    }

    private boolean tickComposting() {
        if (!canOutputCompostedBiomass()) {
            return false;
        }
        if (availableCompostUnits() < COMPOST_UNITS_PER_BATCH) {
            return resetBatch();
        }
        if (progress <= 0) {
            startBatch();
        }
        progress++;
        if (progress < currentRequiredTicks()) {
            return true;
        }
        if (consumeCompostUnits(COMPOST_UNITS_PER_BATCH)) {
            addCompostedBiomass();
        }
        progress = 0;
        wateredBatch = false;
        return true;
    }

    private void startBatch() {
        if (waterTank.getFluidAmount() >= WATER_PER_BATCH) {
            waterTank.drain(WATER_PER_BATCH, IFluidHandler.FluidAction.EXECUTE);
            wateredBatch = true;
        } else {
            wateredBatch = false;
        }
    }

    private boolean resetBatch() {
        if (progress == 0 && !wateredBatch) {
            return false;
        }
        progress = 0;
        wateredBatch = false;
        return true;
    }

    private int currentRequiredTicks() {
        int baseTicks;
        if (progress <= 0) {
            baseTicks = waterTank.getFluidAmount() >= WATER_PER_BATCH ? WET_COMPOST_TICKS : DRY_COMPOST_TICKS;
        } else {
            baseTicks = wateredBatch ? WET_COMPOST_TICKS : DRY_COMPOST_TICKS;
        }
        return Math.max(1, (int) Math.ceil(baseTicks / speedMultiplier()));
    }

    private int statusCode() {
        if (!canOutputCompostedBiomass()) {
            return STATUS_OUTPUT_FULL;
        }
        if (progress > 0) {
            return STATUS_COMPOSTING;
        }
        return availableCompostUnits() >= COMPOST_UNITS_PER_BATCH ? STATUS_READY : STATUS_NO_INPUT;
    }

    private boolean isActivelyComposting() {
        return statusCode() == STATUS_COMPOSTING;
    }

    private int availableCompostUnits() {
        int total = 0;
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (isCompostable(stack)) {
                total += stack.getCount() * compostUnits(stack);
            }
        }
        return total;
    }

    private int varietyCount() {
        Set<Item> items = new HashSet<>();
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (isCompostable(stack)) {
                items.add(stack.getItem());
            }
        }
        return items.size();
    }

    private boolean hasBulkBonus() {
        return availableCompostUnits() >= BULK_BONUS_UNIT_THRESHOLD;
    }

    private boolean hasVarietyBonus() {
        return varietyCount() >= VARIETY_BONUS_ITEM_THRESHOLD;
    }

    private double speedMultiplier() {
        return 1.0D + activeBonusCount() * SPEED_BONUS_PER_ACTIVE;
    }

    private int speedBonusPercent() {
        return activeBonusCount() * 10;
    }

    private int activeBonusCount() {
        int active = 0;
        if (hasBulkBonus()) {
            active++;
        }
        if (hasVarietyBonus()) {
            active++;
        }
        return active;
    }

    private boolean consumeCompostUnits(int amount) {
        if (availableCompostUnits() < amount) {
            return false;
        }
        int preparedToConsume = preparedItemsToConsume(amount);
        if (preparedToConsume < 0) {
            return false;
        }
        int remainingPrepared = preparedToConsume;
        int remainingOrdinary = amount - preparedToConsume * PREPARED_INPUT_COMPOST_UNITS;
        for (int slot = 0; slot < INPUT_SLOT_COUNT && remainingPrepared > 0; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!isPreparedCompostInput(stack)) {
                continue;
            }
            int consumed = Math.min(remainingPrepared, stack.getCount());
            stack.shrink(consumed);
            remainingPrepared -= consumed;
            if (stack.isEmpty()) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        for (int slot = 0; slot < INPUT_SLOT_COUNT && remainingOrdinary > 0; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!isOrdinaryCompostInput(stack)) {
                continue;
            }
            int consumed = Math.min(remainingOrdinary, stack.getCount());
            stack.shrink(consumed);
            remainingOrdinary -= consumed;
            if (stack.isEmpty()) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        return remainingPrepared <= 0 && remainingOrdinary <= 0;
    }

    private int preparedItemsToConsume(int amount) {
        int preparedItems = 0;
        int ordinaryUnits = 0;
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (isPreparedCompostInput(stack)) {
                preparedItems += stack.getCount();
            } else if (isOrdinaryCompostInput(stack)) {
                ordinaryUnits += stack.getCount();
            }
        }
        int maximumPrepared = Math.min(preparedItems, amount / PREPARED_INPUT_COMPOST_UNITS);
        for (int prepared = maximumPrepared; prepared >= 0; prepared--) {
            int remaining = amount - prepared * PREPARED_INPUT_COMPOST_UNITS;
            if (remaining <= ordinaryUnits) {
                return prepared;
            }
        }
        return -1;
    }

    private static boolean isPreparedCompostInput(ItemStack stack) {
        return isCompostable(stack) && stack.is(ModTags.Items.WOODEN_COMPOSTER_PREPARED_INPUTS);
    }

    private static boolean isOrdinaryCompostInput(ItemStack stack) {
        return isCompostable(stack) && !stack.is(ModTags.Items.WOODEN_COMPOSTER_PREPARED_INPUTS);
    }

    private boolean canOutputCompostedBiomass() {
        ItemStack result = new ItemStack(ModItems.COMPOSTED_BIOMASS.get());
        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), inventory.getSlotLimit(SLOT_OUTPUT));
    }

    private void addCompostedBiomass() {
        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, new ItemStack(ModItems.COMPOSTED_BIOMASS.get()));
        } else {
            output.grow(1);
        }
    }

    private boolean drainWaterInputContainer() {
        ItemStack stack = inventory.getStackInSlot(SLOT_WATER_INPUT_CONTAINER);
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack single = stack.copyWithCount(1);
        FluidActionResult simulated = FluidUtil.tryEmptyContainer(single, waterTank, Integer.MAX_VALUE, null, false);
        if (!simulated.isSuccess() || !canPlaceWaterRemainder(simulated.getResult())) {
            return false;
        }
        FluidActionResult result = FluidUtil.tryEmptyContainer(single, waterTank, Integer.MAX_VALUE, null, true);
        if (!result.isSuccess()) {
            return false;
        }
        stack.shrink(1);
        inventory.setStackInSlot(SLOT_WATER_INPUT_CONTAINER, stack.isEmpty() ? ItemStack.EMPTY : stack);
        placeWaterRemainder(result.getResult());
        return true;
    }

    private boolean canPlaceWaterRemainder(ItemStack remainder) {
        if (remainder.isEmpty()) {
            return true;
        }
        ItemStack existing = inventory.getStackInSlot(SLOT_WATER_OUTPUT_CONTAINER);
        int slotLimit = Math.min(remainder.getMaxStackSize(), inventory.getSlotLimit(SLOT_WATER_OUTPUT_CONTAINER));
        if (existing.isEmpty()) {
            return remainder.getCount() <= slotLimit;
        }
        return ItemStack.isSameItemSameComponents(existing, remainder) && existing.getCount() + remainder.getCount() <= slotLimit;
    }

    private void placeWaterRemainder(ItemStack remainder) {
        if (remainder.isEmpty()) {
            return;
        }
        ItemStack existing = inventory.getStackInSlot(SLOT_WATER_OUTPUT_CONTAINER);
        if (existing.isEmpty()) {
            inventory.setStackInSlot(SLOT_WATER_OUTPUT_CONTAINER, remainder.copy());
        } else {
            existing.grow(remainder.getCount());
        }
    }

    private static boolean isWater(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == Fluids.WATER;
    }

    private final class InputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return INPUT_SLOT_COUNT + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(mappedSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return inventory.insertItem(mappedSlot(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(mappedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inventory.isItemValid(mappedSlot(slot), stack);
        }

        private int mappedSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
            }
            return slot < INPUT_SLOT_COUNT ? slot : SLOT_WATER_INPUT_CONTAINER;
        }
    }

    private final class OutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(mappedSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return inventory.extractItem(mappedSlot(slot), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(mappedSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        private int mappedSlot(int slot) {
            return switch (slot) {
                case 0 -> SLOT_OUTPUT;
                case 1 -> SLOT_WATER_OUTPUT_CONTAINER;
                default -> throw new RuntimeException("Slot " + slot + " not in valid range - [0,2)");
            };
        }
    }

    private final class WaterInputFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? waterTank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? waterTank.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && waterTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return waterTank.fill(resource, action);
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
}
