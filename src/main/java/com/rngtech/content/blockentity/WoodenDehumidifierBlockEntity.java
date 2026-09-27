package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.WoodenDehumidifierFrameBlock;
import com.rngtech.content.menu.WoodenDehumidifierMenu;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.WoodenDehumidifierConversionRecipe;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.content.registry.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class WoodenDehumidifierBlockEntity extends BlockEntity implements MenuProvider, PurgeableFluidStorage {
    public static final int SLOT_CONVERSION_INPUT = 0;
    public static final int SLOT_EMPTY_CONTAINER = 1;
    public static final int SLOT_FILLED_CONTAINER = 2;
    public static final int SLOT_COUNT = 3;

    public static final int STATUS_READY = 0;
    public static final int STATUS_INVALID_STRUCTURE = 1;
    public static final int STATUS_NO_RECIPE = 2;
    public static final int STATUS_NO_WATER = 3;
    public static final int STATUS_OUTPUT_FULL = 4;
    public static final int PURGE_WATER_TANK = 0;
    public static final int PURGE_CONVERTED_OUTPUT_TANK = 1;

    public static final int HUMIDITY_WET = 0;
    public static final int HUMIDITY_NORMAL = 1;
    public static final int HUMIDITY_DRY = 2;
    public static final int HUMIDITY_BLOCKED = 3;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_REQUIRED_TICKS = 1;
    private static final int DATA_WATER = 2;
    private static final int DATA_WATER_CAPACITY = 3;
    private static final int DATA_CONVERTED_OUTPUT = 4;
    private static final int DATA_CONVERTED_OUTPUT_CAPACITY = 5;
    private static final int DATA_CONVERTED_OUTPUT_FLUID = 6;
    private static final int DATA_FRAME_COUNT = 7;
    private static final int DATA_STRUCTURE_VALID = 8;
    private static final int DATA_HUMIDITY_BAND = 9;
    private static final int DATA_RAIN_BONUS = 10;
    private static final int DATA_SOLAR_ACCESS = 11;
    private static final int DATA_PRODUCTION_RATE = 12;
    private static final int DATA_SELECTED_OUTPUT = 13;
    private static final int DATA_STATUS = 14;
    private static final int DATA_COUNT = 15;

    private static final int MIN_FRAMES = 4;
    private static final int MAX_FRAMES = 25;
    private static final int SCAN_RADIUS = 4;
    private static final int RESCAN_INTERVAL = 80;
    private static final int WATER_PER_FRAME_CAPACITY = FluidType.BUCKET_VOLUME;
    private static final int BASE_WATER_PER_MINUTE_PER_FRAME = 60;
    private static final int MAX_WATER_PER_MINUTE = 1500;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_CONVERSION_INPUT -> isConversionIngredient(stack);
                case SLOT_EMPTY_CONTAINER -> isFluidOutputContainer(stack);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_EMPTY_CONTAINER || slot == SLOT_FILLED_CONTAINER ? 1 : super.getSlotLimit(slot);
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
    private final MutableFluidTank waterTank = new MutableFluidTank(WoodenDehumidifierBlockEntity::isWater);
    private final MutableFluidTank convertedOutputTank = new MutableFluidTank(stack -> !isWater(stack));
    private final IItemHandler inputHandler = new InputItemHandler();
    private final IItemHandler outputHandler = new OutputItemHandler();
    private final IFluidHandler selectedOutputHandler = new SelectedOutputFluidHandler();
    private final Set<BlockPos> formedFramePositions = new HashSet<>();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            Optional<WoodenDehumidifierConversionRecipe> recipe = nextRecipe().map(RecipeHolder::value);
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_REQUIRED_TICKS -> recipe.map(WoodenDehumidifierConversionRecipe::processingTicks).orElse(0);
                case DATA_WATER -> waterTank.getFluidAmount();
                case DATA_WATER_CAPACITY -> waterTank.getCapacity();
                case DATA_CONVERTED_OUTPUT -> convertedOutputTank.getFluidAmount();
                case DATA_CONVERTED_OUTPUT_CAPACITY -> convertedOutputTank.getCapacity();
                case DATA_CONVERTED_OUTPUT_FLUID -> fluidId(convertedOutputTank.getFluid().getFluid());
                case DATA_FRAME_COUNT -> frameCount;
                case DATA_STRUCTURE_VALID -> structureValid ? 1 : 0;
                case DATA_HUMIDITY_BAND -> humidityBand();
                case DATA_RAIN_BONUS -> rainingBonusActive() ? 1 : 0;
                case DATA_SOLAR_ACCESS -> hasSolarAccess() ? 1 : 0;
                case DATA_PRODUCTION_RATE -> productionRatePerMinute();
                case DATA_SELECTED_OUTPUT -> fluidId(selectedOutput);
                case DATA_STATUS -> statusCode(recipe.orElse(null));
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

    private int frameCount;
    private boolean structureValid;
    private boolean rescanRequested = true;
    private int rescanTimer;
    private int progress;
    private double waterGenerationRemainder;
    private Fluid selectedOutput = Fluids.WATER;

    public WoodenDehumidifierBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.WOODEN_DEHUMIDIFIER.get(), pos, blockState);
        waterTank.setCapacity(0, false);
        convertedOutputTank.setCapacity(0, false);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WoodenDehumidifierBlockEntity dehumidifier) {
        boolean changed = false;
        if (dehumidifier.rescanRequested || dehumidifier.rescanTimer-- <= 0) {
            changed |= dehumidifier.scanStructure();
            dehumidifier.rescanTimer = RESCAN_INTERVAL;
        }
        changed |= dehumidifier.fillOutputContainer();
        changed |= dehumidifier.generateWater();
        changed |= dehumidifier.tickConversion();
        BaseMachineBlock.setActive(level, pos, state, dehumidifier.structureValid && (dehumidifier.progress > 0 || dehumidifier.productionRatePerMinute() > 0));
        if (changed) {
            dehumidifier.setChanged();
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
        if (side == Direction.UP) {
            return inputHandler;
        }
        if (side == Direction.DOWN) {
            return outputHandler;
        }
        return null;
    }

    public IItemHandler getFrameItemHandler(Direction side) {
        return structureValid ? inputHandler : null;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return side == Direction.UP ? null : selectedOutputHandler;
    }

    public IFluidHandler getFrameFluidHandler(Direction side) {
        return structureValid ? selectedOutputHandler : null;
    }

    @Override
    public List<FluidPurgeTarget> fluidPurgeTargets() {
        return List.of(
                FluidPurgeTarget.of(
                        PURGE_WATER_TANK,
                        Component.translatable("rngtech.purge.target.water_tank"),
                        FluidPurgeRole.INPUT,
                        waterTank::getFluid,
                        waterTank::drain,
                        this::resetConversionProgress,
                        this::setChanged
                ),
                FluidPurgeTarget.of(
                        PURGE_CONVERTED_OUTPUT_TANK,
                        Component.translatable("rngtech.purge.target.converted_output_tank"),
                        FluidPurgeRole.OUTPUT,
                        convertedOutputTank::getFluid,
                        convertedOutputTank::drain,
                        this::setChanged
                )
        );
    }

    public void requestStructureRescan() {
        rescanRequested = true;
    }

    public boolean ownsFrame(BlockPos framePos) {
        return structureValid && formedFramePositions.contains(framePos);
    }

    public static Optional<WoodenDehumidifierBlockEntity> controllerForFrame(Level level, BlockPos framePos) {
        BlockState frameState = level.getBlockState(framePos);
        if (!frameState.is(ModBlocks.WOODEN_DEHUMIDIFIER_FRAME.get()) || !frameState.getValue(WoodenDehumidifierFrameBlock.FORMED)) {
            return Optional.empty();
        }
        WoodenDehumidifierBlockEntity nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(
                framePos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                framePos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS)
        )) {
            if (!(level.getBlockEntity(candidate) instanceof WoodenDehumidifierBlockEntity dehumidifier) || !dehumidifier.ownsFrame(framePos)) {
                continue;
            }
            long distance = squaredDistance(candidate, framePos);
            if (distance < nearestDistance) {
                nearest = dehumidifier;
                nearestDistance = distance;
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static long squaredDistance(BlockPos first, BlockPos second) {
        long dx = first.getX() - second.getX();
        long dy = first.getY() - second.getY();
        long dz = first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public boolean cycleSelectedOutput() {
        List<Fluid> options = outputOptions();
        if (options.isEmpty()) {
            selectedOutput = Fluids.WATER;
            return true;
        }
        int index = options.indexOf(selectedOutput);
        selectedOutput = options.get(Math.floorMod(index + 1, options.size()));
        setChanged();
        return true;
    }

    public boolean isConversionIngredient(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (level == null) {
            return stack.is(ModTags.Items.COMMON_COAL_DUSTS);
        }
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.WOODEN_DEHUMIDIFIER_CONVERSION_TYPE.get())
                .stream()
                .anyMatch(recipe -> recipe.value().ingredient().test(stack));
    }

    public static boolean isFluidOutputContainer(ItemStack stack) {
        return !stack.isEmpty()
                && FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent()
                && FluidUtil.getFluidContained(stack).isEmpty();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.wooden_dehumidifier");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WoodenDehumidifierMenu(containerId, playerInventory, this, menuData);
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

    public void clearFormedFrames() {
        if (level == null) {
            return;
        }
        for (BlockPos framePos : Set.copyOf(formedFramePositions)) {
            setFrameFormed(framePos, false);
        }
        formedFramePositions.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("WaterTank", waterTank.writeToNBT(registries));
        tag.put("ConvertedOutputTank", convertedOutputTank.writeToNBT(registries));
        tag.putInt("FrameCount", frameCount);
        tag.putBoolean("StructureValid", structureValid);
        tag.putInt("Progress", progress);
        tag.putDouble("WaterGenerationRemainder", waterGenerationRemainder);
        tag.putString("SelectedOutput", BuiltInRegistries.FLUID.getKey(selectedOutput).toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        convertedOutputTank.readFromNBT(registries, tag.getCompound("ConvertedOutputTank"));
        frameCount = Math.max(0, tag.getInt("FrameCount"));
        structureValid = tag.getBoolean("StructureValid");
        progress = Math.max(0, tag.getInt("Progress"));
        waterGenerationRemainder = Math.max(0.0D, tag.getDouble("WaterGenerationRemainder"));
        selectedOutput = fluidFromString(tag.getString("SelectedOutput"), Fluids.WATER);
        rescanRequested = true;
        applyCapacity(false);
    }

    private boolean scanStructure() {
        if (level == null) {
            return false;
        }
        rescanRequested = false;
        Set<BlockPos> countedFrames = connectedFrames();
        int previousCount = frameCount;
        boolean previousValid = structureValid;
        frameCount = countedFrames.size();
        structureValid = frameCount >= MIN_FRAMES;
        updateFormedFrameStates(countedFrames);
        applyCapacity(structureValid);
        if (!outputOptions().contains(selectedOutput)) {
            selectedOutput = Fluids.WATER;
        }
        return previousCount != frameCount || previousValid != structureValid;
    }

    private Set<BlockPos> connectedFrames() {
        Set<BlockPos> counted = new LinkedHashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(direction);
            if (isFrameWithinScan(neighbor)) {
                queue.add(neighbor.immutable());
            }
        }
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!visited.add(current) || !isFrameWithinScan(current)) {
                continue;
            }
            counted.add(current);
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!visited.contains(neighbor) && isFrameWithinScan(neighbor)) {
                    queue.add(neighbor.immutable());
                }
            }
        }
        return counted;
    }

    private boolean isFrameWithinScan(BlockPos pos) {
        if (level == null || Math.abs(pos.getX() - worldPosition.getX()) > SCAN_RADIUS
                || Math.abs(pos.getY() - worldPosition.getY()) > SCAN_RADIUS
                || Math.abs(pos.getZ() - worldPosition.getZ()) > SCAN_RADIUS) {
            return false;
        }
        return level.getBlockState(pos).is(ModBlocks.WOODEN_DEHUMIDIFIER_FRAME.get());
    }

    private void updateFormedFrameStates(Set<BlockPos> countedFrames) {
        if (level == null) {
            return;
        }
        Set<BlockPos> previousFormed = Set.copyOf(formedFramePositions);
        Set<BlockPos> nextFormed = structureValid ? Set.copyOf(countedFrames) : Set.of();
        for (BlockPos framePos : previousFormed) {
            if (!nextFormed.contains(framePos)) {
                setFrameFormed(framePos, false);
                level.invalidateCapabilities(framePos);
            }
        }
        for (BlockPos framePos : nextFormed) {
            setFrameFormed(framePos, true);
            if (!previousFormed.contains(framePos)) {
                level.invalidateCapabilities(framePos);
            }
        }
        formedFramePositions.clear();
        formedFramePositions.addAll(nextFormed);
    }

    private void setFrameFormed(BlockPos framePos, boolean formed) {
        if (level == null) {
            return;
        }
        BlockState state = level.getBlockState(framePos);
        if (state.is(ModBlocks.WOODEN_DEHUMIDIFIER_FRAME.get()) && state.getValue(WoodenDehumidifierFrameBlock.FORMED) != formed) {
            level.setBlock(framePos, state.setValue(WoodenDehumidifierFrameBlock.FORMED, formed), Block.UPDATE_ALL);
            level.invalidateCapabilities(framePos);
        }
    }

    private void applyCapacity(boolean clamp) {
        int nextCapacity = structureValid ? effectiveFrameCount() * WATER_PER_FRAME_CAPACITY : preservedCapacity();
        waterTank.setCapacity(nextCapacity, clamp);
        convertedOutputTank.setCapacity(nextCapacity, clamp);
    }

    private int effectiveFrameCount() {
        return Math.min(MAX_FRAMES, frameCount);
    }

    private int preservedCapacity() {
        return Math.max(
                Math.max(waterTank.getCapacity(), convertedOutputTank.getCapacity()),
                Math.max(waterTank.getFluidAmount(), convertedOutputTank.getFluidAmount())
        );
    }

    private boolean generateWater() {
        int rate = productionRatePerMinute();
        if (!structureValid || rate <= 0 || waterTank.getSpace() <= 0) {
            return false;
        }
        waterGenerationRemainder += rate / 1200.0D;
        int amount = Math.min(waterTank.getSpace(), (int) Math.floor(waterGenerationRemainder));
        if (amount <= 0) {
            return false;
        }
        waterGenerationRemainder -= amount;
        waterTank.fillInternal(new FluidStack(Fluids.WATER, amount));
        return true;
    }

    private boolean tickConversion() {
        Optional<RecipeHolder<WoodenDehumidifierConversionRecipe>> recipeHolder = nextRecipe();
        if (recipeHolder.isEmpty()) {
            return false;
        }
        WoodenDehumidifierConversionRecipe recipe = recipeHolder.get().value();
        if (!canRunConversion(recipe)) {
            return false;
        }
        progress++;
        if (progress < recipe.processingTicks()) {
            return true;
        }
        completeConversion(recipe);
        progress = 0;
        return true;
    }

    private Optional<RecipeHolder<WoodenDehumidifierConversionRecipe>> nextRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        return WoodenDehumidifierConversionRecipes.find(level, inventory.getStackInSlot(SLOT_CONVERSION_INPUT), waterTank.getFluid());
    }

    private boolean canRunConversion(WoodenDehumidifierConversionRecipe recipe) {
        return structureValid
                && recipe.waterInput().test(waterTank.getFluid())
                && waterTank.getFluidAmount() >= recipe.waterInput().amount()
                && convertedOutputTank.canAccept(recipe.fluidOutput());
    }

    private void completeConversion(WoodenDehumidifierConversionRecipe recipe) {
        ItemStack input = inventory.getStackInSlot(SLOT_CONVERSION_INPUT);
        if (input.isEmpty()) {
            return;
        }
        waterTank.drain(recipe.waterInput().amount(), IFluidHandler.FluidAction.EXECUTE);
        input.shrink(1);
        if (input.isEmpty()) {
            inventory.setStackInSlot(SLOT_CONVERSION_INPUT, ItemStack.EMPTY);
        }
        convertedOutputTank.fillInternal(recipe.fluidOutput());
    }

    private void resetConversionProgress() {
        progress = 0;
    }

    private boolean fillOutputContainer() {
        ItemStack stack = inventory.getStackInSlot(SLOT_EMPTY_CONTAINER);
        if (stack.isEmpty()) {
            return false;
        }
        FluidActionResult result = FluidUtil.tryFillContainer(stack, selectedOutputHandler, Integer.MAX_VALUE, null, true);
        if (!result.isSuccess() || !canPlaceFilledContainer(result.getResult())) {
            return false;
        }
        stack.shrink(1);
        inventory.setStackInSlot(SLOT_EMPTY_CONTAINER, stack.isEmpty() ? ItemStack.EMPTY : stack);
        placeFilledContainer(result.getResult());
        return true;
    }

    private boolean canPlaceFilledContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack existing = inventory.getStackInSlot(SLOT_FILLED_CONTAINER);
        int limit = Math.min(stack.getMaxStackSize(), inventory.getSlotLimit(SLOT_FILLED_CONTAINER));
        if (existing.isEmpty()) {
            return stack.getCount() <= limit;
        }
        return ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() + stack.getCount() <= limit;
    }

    private void placeFilledContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack existing = inventory.getStackInSlot(SLOT_FILLED_CONTAINER);
        if (existing.isEmpty()) {
            inventory.setStackInSlot(SLOT_FILLED_CONTAINER, stack.copy());
        } else {
            existing.grow(stack.getCount());
        }
    }

    private int statusCode(WoodenDehumidifierConversionRecipe recipe) {
        if (!structureValid) {
            return STATUS_INVALID_STRUCTURE;
        }
        if (recipe == null) {
            return STATUS_NO_RECIPE;
        }
        if (waterTank.getFluidAmount() < recipe.waterInput().amount()) {
            return STATUS_NO_WATER;
        }
        if (!convertedOutputTank.canAccept(recipe.fluidOutput())) {
            return STATUS_OUTPUT_FULL;
        }
        return STATUS_READY;
    }

    private int productionRatePerMinute() {
        int effectiveFrames = effectiveFrameCount();
        if (!structureValid || effectiveFrames <= 0 || !hasSolarAccess()) {
            return 0;
        }
        double multiplier = humidityMultiplier();
        if (multiplier <= 0.0D) {
            return 0;
        }
        return Math.min(MAX_WATER_PER_MINUTE, (int) Math.round(BASE_WATER_PER_MINUTE_PER_FRAME * effectiveFrames * multiplier));
    }

    private boolean hasSolarAccess() {
        if (level == null || !structureValid) {
            return false;
        }
        if (level.canSeeSky(worldPosition.above())) {
            return true;
        }
        for (BlockPos framePos : formedFramePositions) {
            if (level.canSeeSky(framePos.above())) {
                return true;
            }
        }
        return false;
    }

    private double humidityMultiplier() {
        double multiplier = switch (humidityBand()) {
            case HUMIDITY_WET -> 1.5D;
            case HUMIDITY_DRY -> 0.35D;
            case HUMIDITY_BLOCKED -> 0.0D;
            default -> 1.0D;
        };
        if (multiplier > 0.0D && rainingBonusActive()) {
            multiplier += 0.5D;
        }
        return multiplier;
    }

    private int humidityBand() {
        if (level == null || level.dimension() == Level.NETHER || level.dimension() == Level.END) {
            return HUMIDITY_BLOCKED;
        }
        var biome = level.getBiome(worldPosition);
        if (biome.is(ModTags.Biomes.DEHUMIDIFIER_BLOCKED)) {
            return HUMIDITY_BLOCKED;
        }
        if (biome.is(ModTags.Biomes.DEHUMIDIFIER_WET)) {
            return HUMIDITY_WET;
        }
        if (biome.is(ModTags.Biomes.DEHUMIDIFIER_DRY)) {
            return HUMIDITY_DRY;
        }
        return HUMIDITY_NORMAL;
    }

    private boolean rainingBonusActive() {
        if (level == null || humidityBand() == HUMIDITY_BLOCKED || !hasSolarAccess()) {
            return false;
        }
        if (level.isRainingAt(worldPosition.above())) {
            return true;
        }
        for (BlockPos framePos : formedFramePositions) {
            if (level.isRainingAt(framePos.above())) {
                return true;
            }
        }
        return false;
    }

    private List<Fluid> outputOptions() {
        LinkedHashSet<Fluid> fluids = new LinkedHashSet<>();
        fluids.add(Fluids.WATER);
        if (level == null) {
            fluids.add(ModFluids.CARBON_EXHAUST_SOURCE.get());
        } else {
            level.getRecipeManager()
                    .getAllRecipesFor(ModRecipes.WOODEN_DEHUMIDIFIER_CONVERSION_TYPE.get())
                    .stream()
                    .map(recipe -> recipe.value().fluidOutput().getFluid())
                    .filter(fluid -> fluid != Fluids.EMPTY)
                    .forEach(fluids::add);
        }
        return List.copyOf(fluids);
    }

    private MutableFluidTank selectedTank() {
        return selectedOutput == Fluids.WATER ? waterTank : convertedOutputTank;
    }

    private static boolean isWater(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == Fluids.WATER;
    }

    private static int fluidId(Fluid fluid) {
        return BuiltInRegistries.FLUID.getId(fluid);
    }

    private static Fluid fluidFromString(String value, Fluid fallback) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !BuiltInRegistries.FLUID.containsKey(id)) {
            return fallback;
        }
        return BuiltInRegistries.FLUID.get(id);
    }

    private final class InputItemHandler implements IItemHandler {
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
            return switch (slot) {
                case 0 -> SLOT_CONVERSION_INPUT;
                case 1 -> SLOT_EMPTY_CONTAINER;
                default -> throw new RuntimeException("Slot " + slot + " not in valid range - [0,2)");
            };
        }
    }

    private final class OutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(SLOT_FILLED_CONTAINER);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? inventory.extractItem(SLOT_FILLED_CONTAINER, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(SLOT_FILLED_CONTAINER);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    private final class SelectedOutputFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0) {
                return FluidStack.EMPTY;
            }
            FluidStack stack = selectedTank().getFluid();
            return stack.getFluid() == selectedOutput ? stack : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? selectedTank().getCapacity() : 0;
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
            if (resource.isEmpty() || resource.getFluid() != selectedOutput) {
                return FluidStack.EMPTY;
            }
            return selectedTank().drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            FluidStack stack = selectedTank().getFluid();
            if (stack.getFluid() != selectedOutput) {
                return FluidStack.EMPTY;
            }
            return selectedTank().drain(maxDrain, action);
        }
    }

    private static final class MutableFluidTank {
        private final Predicate<FluidStack> validator;
        private FluidStack fluid = FluidStack.EMPTY;
        private int capacity;

        private MutableFluidTank(Predicate<FluidStack> validator) {
            this.validator = validator;
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

        void setCapacity(int capacity, boolean clamp) {
            this.capacity = Math.max(0, capacity);
            if (clamp && fluid.getAmount() > this.capacity) {
                fluid.setAmount(this.capacity);
                normalize();
            }
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
            tag.putInt("Capacity", capacity);
            tag.put("Fluid", fluid.saveOptional(registries));
            return tag;
        }

        void readFromNBT(HolderLookup.Provider registries, CompoundTag tag) {
            capacity = Math.max(0, tag.getInt("Capacity"));
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
