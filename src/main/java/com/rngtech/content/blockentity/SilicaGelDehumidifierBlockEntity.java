package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.SilicaGelColumnCasingBlock;
import com.rngtech.content.menu.SilicaGelDehumidifierMenu;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.DesiccantAbsorptionRecipe;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class SilicaGelDehumidifierBlockEntity extends BlockEntity implements MenuProvider, PurgeableFluidStorage {
    public static final int LANE_COUNT = 3;
    public static final int SLOT_DRY_START = 0;
    public static final int SLOT_SATURATED_START = SLOT_DRY_START + LANE_COUNT;
    public static final int SLOT_COUNT = SLOT_SATURATED_START + LANE_COUNT;

    public static final int STATUS_READY = 0;
    public static final int STATUS_INVALID_STRUCTURE = 1;
    public static final int STATUS_NO_INPUT = 2;
    public static final int STATUS_NO_RECIPE = 3;
    public static final int STATUS_WATER_FULL = 4;
    public static final int STATUS_OUTPUT_FULL = 5;
    public static final int PURGE_WATER_TANK = 0;

    private static final int DATA_WATER = 0;
    private static final int DATA_WATER_CAPACITY = 1;
    private static final int DATA_STRUCTURE_VALID = 2;
    private static final int DATA_LANES_START = 3;
    private static final int DATA_PER_LANE = 4;
    private static final int DATA_COUNT = DATA_LANES_START + LANE_COUNT * DATA_PER_LANE;
    private static final int DATA_LANE_PROGRESS = 0;
    private static final int DATA_LANE_REQUIRED_TICKS = 1;
    private static final int DATA_LANE_STATUS = 2;
    private static final int DATA_LANE_WATER_OUTPUT = 3;

    private static final int RESCAN_INTERVAL = 80;
    private static final int TANK_CAPACITY = 10 * FluidType.BUCKET_VOLUME;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= SLOT_DRY_START && slot < SLOT_SATURATED_START && isDryInput(stack);
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
    private final MutableFluidTank waterTank = new MutableFluidTank(stack -> !stack.isEmpty(), TANK_CAPACITY);
    private final IItemHandler inputHandler = new LaneInputItemHandler();
    private final IItemHandler outputHandler = new LaneOutputItemHandler();
    private final IFluidHandler drainOnlyFluidHandler = new DrainOnlyFluidHandler();
    private final int[] progress = new int[LANE_COUNT];
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_WATER) {
                return waterTank.getFluidAmount();
            }
            if (index == DATA_WATER_CAPACITY) {
                return waterTank.getCapacity();
            }
            if (index == DATA_STRUCTURE_VALID) {
                return structureValid ? 1 : 0;
            }
            int lane = laneForData(index);
            if (lane < 0) {
                return 0;
            }
            Optional<DesiccantAbsorptionRecipe> recipe = nextRecipe(lane).map(RecipeHolder::value);
            return switch (laneDataIndex(index)) {
                case DATA_LANE_PROGRESS -> progress[lane];
                case DATA_LANE_REQUIRED_TICKS -> recipe.map(DesiccantAbsorptionRecipe::processingTicks).orElse(0);
                case DATA_LANE_STATUS -> statusCode(lane, recipe.orElse(null));
                case DATA_LANE_WATER_OUTPUT -> recipe.map(recipeValue -> recipeValue.waterOutput().getAmount()).orElse(0);
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

    private boolean structureValid;
    private boolean rescanRequested = true;
    private int rescanTimer;

    public SilicaGelDehumidifierBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SILICA_GEL_DEHUMIDIFIER.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SilicaGelDehumidifierBlockEntity dehumidifier) {
        boolean changed = false;
        if (dehumidifier.rescanRequested || dehumidifier.rescanTimer-- <= 0) {
            changed |= dehumidifier.scanStructure();
            dehumidifier.rescanTimer = RESCAN_INTERVAL;
        }
        boolean active = false;
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            changed |= dehumidifier.tickLane(lane);
            active |= dehumidifier.progress[lane] > 0;
        }
        BaseMachineBlock.setActive(level, pos, state, dehumidifier.structureValid && active);
        if (changed) {
            dehumidifier.setChanged();
        }
    }

    public static void requestControllerRescan(Level level, BlockPos casingPos) {
        for (int offset = 1; offset <= 2; offset++) {
            if (level.getBlockEntity(casingPos.below(offset)) instanceof SilicaGelDehumidifierBlockEntity dehumidifier) {
                dehumidifier.requestStructureRescan();
            }
        }
    }

    public static Optional<SilicaGelDehumidifierBlockEntity> controllerForCasing(Level level, BlockPos casingPos) {
        if (!level.getBlockState(casingPos).is(ModBlocks.SILICA_GEL_COLUMN_CASING.get())) {
            return Optional.empty();
        }
        for (int offset = 1; offset <= 2; offset++) {
            if (level.getBlockEntity(casingPos.below(offset)) instanceof SilicaGelDehumidifierBlockEntity dehumidifier
                    && dehumidifier.ownsCasing(casingPos)) {
                return Optional.of(dehumidifier);
            }
        }
        return Optional.empty();
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
        if (side == Direction.UP) {
            return inputHandler;
        }
        if (side == Direction.DOWN) {
            return outputHandler;
        }
        return null;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return side != null && side.getAxis().isHorizontal() ? drainOnlyFluidHandler : null;
    }

    public IFluidHandler getCasingFluidHandler(Direction side) {
        return structureValid && side != null && side.getAxis().isHorizontal() ? drainOnlyFluidHandler : null;
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        return List.of(FluidPurgeTarget.of(
                PURGE_WATER_TANK,
                Component.translatable("rngtech.purge.target.water_tank"),
                FluidPurgeRole.OUTPUT,
                waterTank::getFluid,
                waterTank::drain,
                this::setChanged
        ));
    }

    public void requestStructureRescan() {
        rescanRequested = true;
    }

    public void clearColumnState() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean changed = setColumnPart(worldPosition.above(1), SilicaGelColumnCasingBlock.ColumnPart.SINGLE);
        changed |= setColumnPart(worldPosition.above(2), SilicaGelColumnCasingBlock.ColumnPart.SINGLE);
        if (changed) {
            level.invalidateCapabilities(worldPosition.above(1));
            level.invalidateCapabilities(worldPosition.above(2));
        }
    }

    public boolean ownsCasing(BlockPos casingPos) {
        return structureValid && (worldPosition.above(1).equals(casingPos) || worldPosition.above(2).equals(casingPos));
    }

    public boolean isDryInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (level == null) {
            return stack.is(ModItems.SILICA_GEL_BEADS.get()) || stack.is(ModItems.SUPERCHARGED_SILICA_GEL_BEADS.get());
        }
        return DesiccantAbsorptionRecipes.hasRecipe(level, stack);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.silica_gel_dehumidifier");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SilicaGelDehumidifierMenu(containerId, playerInventory, this, menuData);
    }

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
        tag.put("WaterTank", waterTank.writeToNBT(registries));
        tag.putIntArray("LaneProgress", progress);
        tag.putBoolean("StructureValid", structureValid);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        int[] savedProgress = tag.getIntArray("LaneProgress");
        Arrays.fill(progress, 0);
        System.arraycopy(savedProgress, 0, progress, 0, Math.min(savedProgress.length, progress.length));
        structureValid = tag.getBoolean("StructureValid");
        rescanRequested = true;
    }

    private boolean tickLane(int lane) {
        if (!structureValid) {
            return false;
        }
        Optional<RecipeHolder<DesiccantAbsorptionRecipe>> recipeHolder = nextRecipe(lane);
        if (recipeHolder.isEmpty()) {
            if (progress[lane] == 0) {
                return false;
            }
            progress[lane] = 0;
            return true;
        }
        DesiccantAbsorptionRecipe recipe = recipeHolder.get().value();
        if (!canPlaceOutput(lane, recipe.saturatedOutput()) || !waterTank.canAccept(recipe.waterOutput())) {
            return false;
        }
        progress[lane]++;
        if (progress[lane] < recipe.processingTicks()) {
            return true;
        }
        completeLane(lane, recipe);
        progress[lane] = 0;
        return true;
    }

    private Optional<RecipeHolder<DesiccantAbsorptionRecipe>> nextRecipe(int lane) {
        return level == null ? Optional.empty() : DesiccantAbsorptionRecipes.find(level, inventory.getStackInSlot(inputSlot(lane)));
    }

    private void completeLane(int lane, DesiccantAbsorptionRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(inputSlot(lane));
        if (input.isEmpty()) {
            return;
        }
        input.shrink(1);
        if (input.isEmpty()) {
            inventory.setStackInSlot(inputSlot(lane), ItemStack.EMPTY);
        }
        placeOutput(lane, recipe.outputStack());
        waterTank.fillInternal(recipe.outputFluid());
    }

    private boolean scanStructure() {
        if (level == null) {
            return false;
        }
        rescanRequested = false;
        boolean previousValid = structureValid;
        structureValid = level.getBlockState(worldPosition.above(1)).is(ModBlocks.SILICA_GEL_COLUMN_CASING.get())
                && level.getBlockState(worldPosition.above(2)).is(ModBlocks.SILICA_GEL_COLUMN_CASING.get());
        boolean visualChanged = updateColumnState();
        if (previousValid != structureValid || visualChanged) {
            level.invalidateCapabilities(worldPosition);
            level.invalidateCapabilities(worldPosition.above(1));
            level.invalidateCapabilities(worldPosition.above(2));
        }
        return previousValid != structureValid || visualChanged;
    }

    private boolean updateColumnState() {
        SilicaGelColumnCasingBlock.ColumnPart lowerPart = structureValid
                ? SilicaGelColumnCasingBlock.ColumnPart.LOWER
                : SilicaGelColumnCasingBlock.ColumnPart.SINGLE;
        SilicaGelColumnCasingBlock.ColumnPart upperPart = structureValid
                ? SilicaGelColumnCasingBlock.ColumnPart.UPPER
                : SilicaGelColumnCasingBlock.ColumnPart.SINGLE;
        boolean changed = setColumnPart(worldPosition.above(1), lowerPart);
        changed |= setColumnPart(worldPosition.above(2), upperPart);
        return changed;
    }

    private boolean setColumnPart(BlockPos pos, SilicaGelColumnCasingBlock.ColumnPart part) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.SILICA_GEL_COLUMN_CASING.get()) || state.getValue(SilicaGelColumnCasingBlock.PART) == part) {
            return false;
        }
        level.setBlock(pos, state.setValue(SilicaGelColumnCasingBlock.PART, part), Block.UPDATE_ALL);
        return true;
    }

    private int statusCode(int lane, DesiccantAbsorptionRecipe recipe) {
        if (!structureValid) {
            return STATUS_INVALID_STRUCTURE;
        }
        ItemStack input = inventory.getStackInSlot(inputSlot(lane));
        if (input.isEmpty()) {
            return STATUS_NO_INPUT;
        }
        if (recipe == null) {
            return STATUS_NO_RECIPE;
        }
        if (!canPlaceOutput(lane, recipe.saturatedOutput())) {
            return STATUS_OUTPUT_FULL;
        }
        if (!waterTank.canAccept(recipe.waterOutput())) {
            return STATUS_WATER_FULL;
        }
        return STATUS_READY;
    }

    private boolean canPlaceOutput(int lane, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack existing = inventory.getStackInSlot(outputSlot(lane));
        int limit = Math.min(stack.getMaxStackSize(), inventory.getSlotLimit(outputSlot(lane)));
        if (existing.isEmpty()) {
            return stack.getCount() <= limit;
        }
        return ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() + stack.getCount() <= limit;
    }

    private void placeOutput(int lane, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack existing = inventory.getStackInSlot(outputSlot(lane));
        if (existing.isEmpty()) {
            inventory.setStackInSlot(outputSlot(lane), stack.copy());
        } else {
            existing.grow(stack.getCount());
        }
    }

    private static int inputSlot(int lane) {
        return SLOT_DRY_START + lane;
    }

    private static int outputSlot(int lane) {
        return SLOT_SATURATED_START + lane;
    }

    private static int laneForData(int index) {
        if (index < DATA_LANES_START || index >= DATA_COUNT) {
            return -1;
        }
        return (index - DATA_LANES_START) / DATA_PER_LANE;
    }

    private static int laneDataIndex(int index) {
        return (index - DATA_LANES_START) % DATA_PER_LANE;
    }

    private final class LaneInputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return LANE_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(inputSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return inventory.insertItem(inputSlot(slot), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(inputSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inventory.isItemValid(inputSlot(slot), stack);
        }
    }

    private final class LaneOutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return LANE_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(outputSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return inventory.extractItem(outputSlot(slot), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(outputSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    private final class DrainOnlyFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? waterTank.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? waterTank.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, waterTank.getFluid())) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = waterTank.drain(resource.getAmount(), action);
            if (action.execute() && !drained.isEmpty()) {
                setChanged();
            }
            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = waterTank.drain(maxDrain, action);
            if (action.execute() && !drained.isEmpty()) {
                setChanged();
            }
            return drained;
        }
    }

    private static final class MutableFluidTank {
        private final Predicate<FluidStack> validator;
        private FluidStack fluid = FluidStack.EMPTY;
        private final int capacity;

        private MutableFluidTank(Predicate<FluidStack> validator, int capacity) {
            this.validator = validator;
            this.capacity = Math.max(0, capacity);
        }

        int getCapacity() {
            return capacity;
        }

        int getFluidAmount() {
            return fluid.getAmount();
        }

        int getSpace() {
            return Math.max(0, capacity - getFluidAmount());
        }

        FluidStack getFluid() {
            return fluid.copy();
        }

        boolean canAccept(FluidStack stack) {
            if (stack.isEmpty() || !validator.test(stack)) {
                return false;
            }
            if (!fluid.isEmpty() && !FluidStack.isSameFluidSameComponents(fluid, stack)) {
                return false;
            }
            return stack.getAmount() <= getSpace();
        }

        void fillInternal(FluidStack stack) {
            if (stack.isEmpty() || !validator.test(stack)) {
                return;
            }
            if (fluid.isEmpty()) {
                fluid = stack.copyWithAmount(Math.min(stack.getAmount(), capacity));
            } else if (FluidStack.isSameFluidSameComponents(fluid, stack)) {
                fluid.setAmount(Math.min(capacity, fluid.getAmount() + stack.getAmount()));
            }
            normalize();
        }

        FluidStack drain(int amount, IFluidHandler.FluidAction action) {
            if (amount <= 0 || fluid.isEmpty()) {
                return FluidStack.EMPTY;
            }
            int drained = Math.min(amount, fluid.getAmount());
            FluidStack result = fluid.copyWithAmount(drained);
            if (action.execute()) {
                fluid.setAmount(fluid.getAmount() - drained);
                normalize();
            }
            return result;
        }

        CompoundTag writeToNBT(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            tag.put("Fluid", fluid.saveOptional(registries));
            return tag;
        }

        void readFromNBT(HolderLookup.Provider registries, CompoundTag tag) {
            fluid = FluidStack.parseOptional(registries, tag.getCompound("Fluid"));
            normalize();
        }

        private void normalize() {
            if (fluid.isEmpty() || fluid.getAmount() <= 0) {
                fluid = FluidStack.EMPTY;
            } else if (fluid.getAmount() > capacity) {
                fluid.setAmount(capacity);
            }
        }
    }
}
