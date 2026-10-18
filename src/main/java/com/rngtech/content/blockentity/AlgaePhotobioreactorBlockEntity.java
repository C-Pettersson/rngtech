package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.item.MachinePartItem;
import com.rngtech.content.menu.AlgaePhotobioreactorMenu;
import com.rngtech.content.purge.FluidPurgeRole;
import com.rngtech.content.purge.FluidPurgeTarget;
import com.rngtech.content.purge.PurgeableFluidStorage;
import com.rngtech.content.recipe.AlgaeGrowthRecipe;
import com.rngtech.content.recipe.AlgaeGrowthRecipeInput;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachinePartType;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
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
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
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

import java.util.List;

public class AlgaePhotobioreactorBlockEntity extends BlockEntity
        implements MenuProvider, PurgeableFluidStorage, MachineInfoProvider {
    private final RecipeCache<AlgaeGrowthRecipe> recipes = new RecipeCache<>(1);
    public static final int SLOT_OUTPUT = 0;
    public static final int SLOT_WATER_INPUT_CONTAINER = 1;
    public static final int SLOT_WATER_OUTPUT_CONTAINER = 2;
    public static final int SLOT_CARBON_INPUT_CONTAINER = 3;
    public static final int SLOT_CARBON_OUTPUT_CONTAINER = 4;
    public static final int SLOT_UREA = 5;
    public static final int SLOT_COUNT = 6;
    /** Biomass one Urea grows at twice the speed. */
    public static final int UREA_BOOSTED_HARVESTS = 4;
    public static final int SLOT_BIO_CHAMBER = 0;
    public static final int GEAR_SLOT_COUNT = 1;

    public static final int STATUS_READY = 0;
    public static final int STATUS_NO_RECIPE = 1;
    public static final int STATUS_LOW_LIGHT = 2;
    public static final int STATUS_OUTPUT_FULL = 3;
    public static final int STATUS_NO_WATER = 4;
    public static final int STATUS_NO_CARBON = 5;
    public static final int PURGE_WATER_TANK = 0;
    public static final int PURGE_CARBON_TANK = 1;

    private static final int TANK_CAPACITY = 4 * FluidType.BUCKET_VOLUME;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_REQUIRED_TICKS = 1;
    private static final int DATA_WATER = 2;
    private static final int DATA_WATER_CAPACITY = 3;
    private static final int DATA_CARBON = 4;
    private static final int DATA_CARBON_CAPACITY = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_LIGHT = 7;
    private static final int DATA_MIN_LIGHT = 8;
    private static final int DATA_BIO_CONVERSION = 9;
    private static final int DATA_OUTPUT_BONUS_PROGRESS = 10;
    private static final int DATA_WATER_FLUID_ID = 11;
    private static final int DATA_CARBON_FLUID_ID = 12;
    private static final int DATA_AERATION = 13;
    private static final int DATA_DENSITY = 14;
    private static final int DATA_CARBON_PER_HARVEST = 15;
    private static final int DATA_UREA_BOOST = 16;
    private static final int DATA_COUNT = 17;
    private static final int STAT_SCALE = 100;
    private static final int OUTPUT_BONUS_PROGRESS_SCALE = 1000;
    private static final int DENSITY_SCALE = 1000;
    private static final int DEFAULT_MINIMUM_LIGHT = 12;
    private static final int VISUAL_SYNC_INTERVAL = 5;
    private static final double EPSILON = 1.0E-9;
    private static final double CARBON_EPSILON = 1.0E-6;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_OUTPUT -> isAlgaeOutputStack(stack);
                case SLOT_WATER_INPUT_CONTAINER -> isWaterInputContainer(stack);
                case SLOT_CARBON_INPUT_CONTAINER -> isCarbonInputContainer(stack);
                case SLOT_UREA -> stack.is(ModItems.UREA.get());
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_WATER_INPUT_CONTAINER
                            || slot == SLOT_WATER_OUTPUT_CONTAINER
                            || slot == SLOT_CARBON_INPUT_CONTAINER
                            || slot == SLOT_CARBON_OUTPUT_CONTAINER
                    ? 1
                    : super.getSlotLimit(slot);
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
            return slot == SLOT_BIO_CHAMBER && isBioChamber(stack);
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
    private final FluidTank waterTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return isWater(stack);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank carbonTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return isCarbonInput(stack);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IItemHandler inputHandler = new InputItemHandler();
    private final IItemHandler outputHandler = new OutputItemHandler();
    private final IFluidHandler fluidHandler = new AlgaeFluidHandler();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            AlgaeGrowthRecipe recipe = nextRecipe();
            MachineStatAccumulator stats = effectiveStats();
            return switch (index) {
                case DATA_PROGRESS -> (int) progress;
                case DATA_REQUIRED_TICKS -> recipe == null ? 0 : currentProcessingTicks(recipe, stats);
                case DATA_WATER -> waterTank.getFluidAmount();
                case DATA_WATER_CAPACITY -> waterTank.getCapacity();
                case DATA_CARBON -> carbonTank.getFluidAmount();
                case DATA_CARBON_CAPACITY -> carbonTank.getCapacity();
                case DATA_STATUS -> statusCode(recipe, stats);
                case DATA_LIGHT -> lightLevel();
                case DATA_MIN_LIGHT -> recipe == null ? 0 : recipe.minimumLight();
                case DATA_BIO_CONVERSION -> scaledStat(stats, MachineStat.FUEL_EFFICIENCY);
                case DATA_OUTPUT_BONUS_PROGRESS -> scaledOutputBonusProgress();
                case DATA_WATER_FLUID_ID -> BuiltInRegistries.FLUID.getId(waterTank.getFluid().getFluid());
                case DATA_CARBON_FLUID_ID -> BuiltInRegistries.FLUID.getId(carbonTank.getFluid().getFluid());
                case DATA_AERATION -> aeration;
                case DATA_DENSITY -> (int) Math.round(density * DENSITY_SCALE);
                case DATA_CARBON_PER_HARVEST -> recipe == null ? 0 : AlgaeCultureDynamics.carbonAmount(recipe.carbonInput().amount(), aeration);
                case DATA_UREA_BOOST -> ureaBoost;
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

    private double progress;
    /** Harvests left that grow at twice the speed from the last Urea. */
    private int ureaBoost;
    private FluidStack activeWater = FluidStack.EMPTY;
    private FluidStack activeCarbon = FluidStack.EMPTY;
    private double outputBonusProgress;
    private double density;
    private double carbonCharge;
    private int aeration = AlgaeCultureDynamics.RATED_AERATION;
    private AlgaeVisualState visualState = AlgaeVisualState.IDLE;
    private AlgaeVisualState.Key lastSyncedVisual;
    private final AlgaeVisualState.Animation animation = new AlgaeVisualState.Animation();

    public AlgaePhotobioreactorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ALGAE_PHOTOBIOREACTOR.get(), pos, blockState);
    }

    /**
     * Growth runs at culture density times the aeration factor. Without light, water or carbon gas the culture dies
     * back toward the seed density; a full output only pauses the harvest. Neither discards harvest progress.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, AlgaePhotobioreactorBlockEntity reactor) {
        boolean changed = reactor.drainInputContainer(SLOT_WATER_INPUT_CONTAINER, SLOT_WATER_OUTPUT_CONTAINER, reactor.waterTank);
        changed |= reactor.drainInputContainer(SLOT_CARBON_INPUT_CONTAINER, SLOT_CARBON_OUTPUT_CONTAINER, reactor.carbonTank);

        MachineStatAccumulator stats = reactor.effectiveStats();
        AlgaeGrowthRecipe recipe = reactor.nextRecipe();
        boolean fed = recipe != null && reactor.hasRequiredLight(recipe) && reactor.hasCarbonForHarvest(recipe, stats);
        boolean growing = fed && reactor.canAcceptOutput(recipe, stats);
        if (growing) {
            changed |= reactor.grow(recipe, stats);
        } else if (!fed) {
            changed |= reactor.starve();
        }
        BaseMachineBlock.setActive(level, pos, state, growing);
        if (changed) {
            reactor.setChanged();
        }
        reactor.syncVisualState(level);
    }

    private boolean grow(AlgaeGrowthRecipe recipe, MachineStatAccumulator stats) {
        if (!activeCycleMatches()) {
            resetCycle();
        }
        startCycleIfNeeded(recipe);
        int ticks = currentProcessingTicks(recipe, stats);
        if (ureaBoost <= 0 && !inventory.getStackInSlot(SLOT_UREA).isEmpty()) {
            inventory.extractItem(SLOT_UREA, 1, false);
            ureaBoost = UREA_BOOSTED_HARVESTS;
        }
        // Urea speeds growth only; carbon and water per biomass stay the same.
        double step = AlgaeCultureDynamics.progressPerTick(density, aeration) * (ureaBoost > 0 ? 2.0 : 1.0);
        progress += step;
        density = AlgaeCultureDynamics.grow(density, aeration, ticks);
        carbonCharge += carbonRate(recipe, ticks) * step;
        if (progress >= ticks - EPSILON) {
            process(recipe, stats, ticks);
        }
        return true;
    }

    /** Carbon gas fed per tick of harvest progress at the current aeration. */
    private double carbonRate(AlgaeGrowthRecipe recipe, int ticks) {
        return recipe.carbonInput().amount() * AlgaeCultureDynamics.carbonPerBiomass(aeration) / ticks;
    }

    /** The tank must hold the gas this harvest will have used by the end, at the current aeration. */
    private boolean hasCarbonForHarvest(AlgaeGrowthRecipe recipe, MachineStatAccumulator stats) {
        int ticks = currentProcessingTicks(recipe, stats);
        double remaining = Math.max(0.0, ticks - progress) * carbonRate(recipe, ticks);
        return carbonTank.getFluidAmount() >= (int) Math.ceil(carbonCharge + remaining - CARBON_EPSILON);
    }

    private boolean starve() {
        double next = AlgaeCultureDynamics.dieBack(density);
        if (next == density) {
            return false;
        }
        density = next;
        return true;
    }

    /**
     * Reactors saved while the block was a solid cube stored no light inside it, which leaves the renderer's culture and
     * tanks unlit. Rechecking the block's own light once on load fixes those saves.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            level.getLightEngine().checkBlock(worldPosition);
        }
    }

    public int aeration() {
        return aeration;
    }

    public void setAeration(int aeration) {
        int clamped = AlgaeCultureDynamics.clampAeration(aeration);
        if (clamped != this.aeration) {
            this.aeration = clamped;
            setChanged();
        }
    }

    public double cultureDensity() {
        return density;
    }

    /** Client copy of the state the renderer draws; synced from the server in the block entity update tag. */
    public AlgaeVisualState visualState() {
        return visualState;
    }

    public AlgaeVisualState.Animation animation() {
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
        visualState = AlgaeVisualState.load(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        visualState = AlgaeVisualState.load(packet.getTag());
    }

    private void syncVisualState(Level level) {
        if (level.getGameTime() % VISUAL_SYNC_INTERVAL != 0) {
            return;
        }
        AlgaeVisualState.Key key = currentVisualState().key();
        if (!key.equals(lastSyncedVisual)) {
            lastSyncedVisual = key;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private AlgaeVisualState currentVisualState() {
        AlgaeGrowthRecipe recipe = nextRecipe();
        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        return new AlgaeVisualState(
                (float) density,
                aeration,
                isActive(),
                lightLevel() >= (recipe == null ? DEFAULT_MINIMUM_LIGHT : recipe.minimumLight()),
                waterTank.getFluidAmount() / (float) waterTank.getCapacity(),
                carbonTank.getFluidAmount() / (float) carbonTank.getCapacity(),
                carbonTank.getFluid().getFluid(),
                output.isEmpty() ? 0.0F : output.getCount() / (float) output.getMaxStackSize()
        );
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ItemStackHandler getGearInventory() {
        return gearInventory;
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
        return side == Direction.DOWN ? null : fluidHandler;
    }

    public FluidStack getWaterFluid() {
        return waterTank.getFluid();
    }

    public FluidStack getCarbonFluid() {
        return carbonTank.getFluid();
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
                        this::resetCycle,
                        this::setChanged
                ).withCapacity(waterTank::getCapacity),
                FluidPurgeTarget.of(
                        PURGE_CARBON_TANK,
                        Component.translatable("rngtech.purge.target.carbon_tank"),
                        FluidPurgeRole.INPUT,
                        carbonTank::getFluid,
                        carbonTank::drain,
                        this::resetCycle,
                        this::setChanged
                ).withCapacity(carbonTank::getCapacity)
        );
    }

    @Override
    public MachineInfoSnapshot machineInfo() {
        MachineStatAccumulator stats = effectiveStats();
        AlgaeGrowthRecipe recipe = nextRecipe();
        int status = statusCode(recipe, stats);
        return MachineInfoSnapshot.builder("algae_photobioreactor")
                .state(
                        MachineInfoSnapshot.workState(
                                status == STATUS_READY,
                                isActive(),
                                status == STATUS_NO_WATER || status == STATUS_NO_CARBON
                        ),
                        MachineInfoSnapshot.BlockedReason.NONE
                )
                .status(statusKey(status))
                .progress((int) progress, recipe == null ? 0 : currentProcessingTicks(recipe, stats))
                .output(status == STATUS_OUTPUT_FULL
                        ? MachineInfoSnapshot.OutputSummary.OUTPUT_FULL
                        : MachineInfoSnapshot.OutputSummary.NONE)
                .build();
    }

    private static String statusKey(int status) {
        String name = switch (status) {
            case STATUS_NO_RECIPE -> "no_recipe";
            case STATUS_LOW_LIGHT -> "low_light";
            case STATUS_OUTPUT_FULL -> "output_full";
            case STATUS_NO_WATER -> "no_water";
            case STATUS_NO_CARBON -> "no_carbon";
            default -> "";
        };
        return name.isEmpty() ? "" : "rngtech.algae_photobioreactor.status." + name;
    }

    private boolean isActive() {
        BlockState state = getBlockState();
        return state.hasProperty(BaseMachineBlock.ACTIVE) && state.getValue(BaseMachineBlock.ACTIVE);
    }

    public static boolean isWaterInputContainer(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).filter(AlgaePhotobioreactorBlockEntity::isWater).isPresent();
    }

    public static boolean isCarbonInputContainer(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).filter(AlgaePhotobioreactorBlockEntity::isCarbonInput).isPresent();
    }

    public static boolean isAlgaeOutputStack(ItemStack stack) {
        return stack.is(ModItems.ALGAE_BIOMASS.get());
    }

    public boolean isBioChamber(ItemStack stack) {
        return stack.getItem() instanceof MachinePartItem part
                && part.partType() == MachinePartType.BIO_CHAMBER
                && part.machineType() == MachineType.BIO_GENERATOR;
    }

    public MachineStatAccumulator effectiveStats() {
        MachineStatAccumulator stats = MachineStatAccumulator.bioOrganicProcessorBase();
        ItemStack chamber = bioChamberStack();
        if (isBioChamber(chamber)) {
            ComponentBaseStatCatalog.applyEffectiveContribution(stats, chamber);
        }
        return stats;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.algae_photobioreactor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AlgaePhotobioreactorMenu(containerId, playerInventory, this, menuData);
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
        for (int slot = 0; slot < gearInventory.getSlots(); slot++) {
            ItemStack stack = gearInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
                gearInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("GearInventory", gearInventory.serializeNBT(registries));
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
        tag.put("CarbonTank", carbonTank.writeToNBT(registries, new CompoundTag()));
        tag.putDouble("Progress", progress);
        tag.put("ActiveWater", activeWater.saveOptional(registries));
        tag.put("ActiveCarbon", activeCarbon.saveOptional(registries));
        tag.putDouble("OutputBonusProgress", outputBonusProgress);
        tag.putDouble("Density", density);
        tag.putDouble("CarbonCharge", carbonCharge);
        tag.putInt("Aeration", aeration);
        tag.putInt("UreaBoost", ureaBoost);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Reactors saved before the Urea slot load with their old five slots, grown to today's size.
        CompoundTag inventoryTag = tag.getCompound("Inventory").copy();
        inventoryTag.putInt("Size", Math.max(SLOT_COUNT, inventoryTag.getInt("Size")));
        inventory.deserializeNBT(registries, inventoryTag);
        gearInventory.deserializeNBT(registries, tag.getCompound("GearInventory"));
        waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        carbonTank.readFromNBT(registries, tag.getCompound("CarbonTank"));
        progress = Math.max(0.0, tag.getDouble("Progress"));
        activeWater = FluidStack.parseOptional(registries, tag.getCompound("ActiveWater"));
        activeCarbon = FluidStack.parseOptional(registries, tag.getCompound("ActiveCarbon"));
        outputBonusProgress = clampUnit(tag.getDouble("OutputBonusProgress"));
        // Reactors saved before cultures existed were already growing, so they keep a full culture.
        density = tag.contains("Density") ? clampUnit(tag.getDouble("Density")) : 1.0;
        carbonCharge = Math.max(0.0, tag.getDouble("CarbonCharge"));
        aeration = tag.contains("Aeration") ? AlgaeCultureDynamics.clampAeration(tag.getInt("Aeration")) : AlgaeCultureDynamics.RATED_AERATION;
        ureaBoost = Math.max(0, tag.getInt("UreaBoost"));
    }

    private AlgaeGrowthRecipe nextRecipe() {
        if (level == null) {
            return null;
        }
        FluidStack water = waterTank.getFluid();
        FluidStack carbon = carbonTank.getFluid();
        return recipes.lookup(0, level).fluid(water).fluid(carbon).find(() -> AlgaeGrowthRecipes.find(level, water, carbon));
    }

    /** How many recipe lookups searched the recipes instead of reusing a remembered result. */
    public int recipeSearches() {
        return recipes.searches();
    }

    /** Harvests one cycle. Progress and fed carbon past the cycle carry into the next, so the rate stays exact. */
    private void process(AlgaeGrowthRecipe recipe, MachineStatAccumulator stats, int ticks) {
        if (level == null || !recipe.matches(new AlgaeGrowthRecipeInput(waterTank.getFluid(), carbonTank.getFluid()), level)) {
            return;
        }
        ItemStack output = effectiveOutput(recipe, stats, false);
        int carbon = (int) Math.floor(carbonCharge + CARBON_EPSILON);
        if (!canAcceptOutput(output) || carbonTank.getFluidAmount() < carbon) {
            return;
        }
        FluidStack drainedWater = waterTank.drain(recipe.waterInput().amount(), IFluidHandler.FluidAction.EXECUTE);
        FluidStack drainedCarbon = carbonTank.drain(carbon, IFluidHandler.FluidAction.EXECUTE);
        if (drainedWater.getAmount() < recipe.waterInput().amount() || drainedCarbon.getAmount() < carbon) {
            resetCycle();
            return;
        }
        addOutput(effectiveOutput(recipe, stats, true));
        if (ureaBoost > 0) {
            ureaBoost--;
        }
        progress = Math.max(0.0, progress - ticks);
        carbonCharge = Math.max(0.0, carbonCharge - carbon);
        activeWater = FluidStack.EMPTY;
        activeCarbon = FluidStack.EMPTY;
    }

    private boolean canAcceptOutput(AlgaeGrowthRecipe recipe, MachineStatAccumulator stats) {
        return canAcceptOutput(effectiveOutput(recipe, stats, false));
    }

    private boolean canAcceptOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        int slotLimit = Math.min(result.getMaxStackSize(), inventory.getSlotLimit(SLOT_OUTPUT));
        if (output.isEmpty()) {
            return result.getCount() <= slotLimit;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= slotLimit;
    }

    private void addOutput(ItemStack result) {
        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
        }
    }

    private boolean hasRequiredLight(AlgaeGrowthRecipe recipe) {
        return lightLevel() >= recipe.minimumLight();
    }

    private int lightLevel() {
        if (level == null) {
            return 0;
        }
        return Math.max(level.getBrightness(LightLayer.SKY, worldPosition.above()), level.getBrightness(LightLayer.BLOCK, worldPosition.above()));
    }

    private int statusCode(AlgaeGrowthRecipe recipe, MachineStatAccumulator stats) {
        if (waterTank.isEmpty()) {
            return STATUS_NO_WATER;
        }
        if (carbonTank.isEmpty()) {
            return STATUS_NO_CARBON;
        }
        if (recipe == null) {
            return STATUS_NO_RECIPE;
        }
        if (!hasRequiredLight(recipe)) {
            return STATUS_LOW_LIGHT;
        }
        if (!hasCarbonForHarvest(recipe, stats)) {
            return STATUS_NO_CARBON;
        }
        if (!canAcceptOutput(recipe, stats)) {
            return STATUS_OUTPUT_FULL;
        }
        return STATUS_READY;
    }

    private void startCycleIfNeeded(AlgaeGrowthRecipe recipe) {
        if (!activeWater.isEmpty()) {
            return;
        }
        activeWater = waterTank.getFluid().copyWithAmount(recipe.waterInput().amount());
        activeCarbon = carbonTank.getFluid().copyWithAmount(recipe.carbonInput().amount());
    }

    /** A cycle keeps its progress only while the same water and carbon gas are still there to finish it. */
    private boolean activeCycleMatches() {
        if (activeWater.isEmpty()) {
            return true;
        }
        return !activeCarbon.isEmpty()
                && FluidStack.isSameFluidSameComponents(activeWater, waterTank.getFluid())
                && FluidStack.isSameFluidSameComponents(activeCarbon, carbonTank.getFluid())
                && waterTank.getFluidAmount() >= activeWater.getAmount()
                && carbonTank.getFluidAmount() >= activeCarbon.getAmount();
    }

    private void resetCycle() {
        progress = 0.0;
        carbonCharge = 0.0;
        activeWater = FluidStack.EMPTY;
        activeCarbon = FluidStack.EMPTY;
    }

    private int currentProcessingTicks(AlgaeGrowthRecipe recipe, MachineStatAccumulator stats) {
        return stats.adjustedProcessingTicks(recipe.processingTicks());
    }

    private ItemStack effectiveOutput(AlgaeGrowthRecipe recipe, MachineStatAccumulator stats, boolean consume) {
        ItemStack output = recipe.output();
        if (output.isEmpty()) {
            return output;
        }
        output.setCount(outputCount(output.getCount(), stats, consume));
        return output;
    }

    private int outputCount(int baseCount, MachineStatAccumulator stats, boolean consume) {
        double scaledOutput = Math.max(1.0, Math.max(0, baseCount) * bioConversion(stats));
        int guaranteed = Math.max(1, (int) Math.floor(scaledOutput + EPSILON));
        double nextProgress = clampUnit(outputBonusProgress) + Math.max(0.0, scaledOutput - guaranteed);
        int bonus = (int) Math.floor(nextProgress + EPSILON);
        nextProgress -= bonus;
        if (nextProgress >= 1.0 - EPSILON) {
            bonus++;
            nextProgress = 0.0;
        }
        if (consume) {
            outputBonusProgress = clampUnit(nextProgress);
        }
        return guaranteed + bonus;
    }

    private double bioConversion(MachineStatAccumulator stats) {
        return Math.max(0.1, stats.value(MachineStat.FUEL_EFFICIENCY));
    }

    private ItemStack bioChamberStack() {
        return gearInventory.getStackInSlot(SLOT_BIO_CHAMBER);
    }

    private int scaledStat(MachineStatAccumulator stats, MachineStat stat) {
        return (int) Math.round(stats.value(stat) * STAT_SCALE);
    }

    private int scaledOutputBonusProgress() {
        return (int) Math.round(clampUnit(outputBonusProgress) * OUTPUT_BONUS_PROGRESS_SCALE);
    }

    private static double clampUnit(double value) {
        if (value <= 0.0) {
            return 0.0;
        }
        return Math.min(1.0, value);
    }

    private boolean drainInputContainer(int inputSlot, int outputSlot, FluidTank tank) {
        ItemStack stack = inventory.getStackInSlot(inputSlot);
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack single = stack.copyWithCount(1);
        FluidActionResult simulated = FluidUtil.tryEmptyContainer(single, tank, Integer.MAX_VALUE, null, false);
        if (!simulated.isSuccess() || !canPlaceRemainder(outputSlot, simulated.getResult())) {
            return false;
        }
        FluidActionResult result = FluidUtil.tryEmptyContainer(single, tank, Integer.MAX_VALUE, null, true);
        if (!result.isSuccess()) {
            return false;
        }
        stack.shrink(1);
        inventory.setStackInSlot(inputSlot, stack.isEmpty() ? ItemStack.EMPTY : stack);
        placeRemainder(outputSlot, result.getResult());
        return true;
    }

    private boolean canPlaceRemainder(int slot, ItemStack remainder) {
        if (remainder.isEmpty()) {
            return true;
        }
        ItemStack existing = inventory.getStackInSlot(slot);
        int slotLimit = Math.min(remainder.getMaxStackSize(), inventory.getSlotLimit(slot));
        if (existing.isEmpty()) {
            return remainder.getCount() <= slotLimit;
        }
        return ItemStack.isSameItemSameComponents(existing, remainder) && existing.getCount() + remainder.getCount() <= slotLimit;
    }

    private void placeRemainder(int slot, ItemStack remainder) {
        if (remainder.isEmpty()) {
            return;
        }
        ItemStack existing = inventory.getStackInSlot(slot);
        if (existing.isEmpty()) {
            inventory.setStackInSlot(slot, remainder.copy());
        } else {
            existing.grow(remainder.getCount());
        }
    }

    private static boolean isWater(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == Fluids.WATER;
    }

    private static boolean isCarbonInput(FluidStack stack) {
        return !stack.isEmpty()
                && (stack.getFluid() == ModFluids.CARBON_EXHAUST_SOURCE.get()
                        || stack.getFluid() == ModFluids.SYNGAS_SOURCE.get()
                        || stack.getFluid() == ModFluids.CARBON_MONOXIDE_SOURCE.get());
    }

    private final class InputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 4;
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
                case 0 -> SLOT_WATER_INPUT_CONTAINER;
                case 1 -> SLOT_CARBON_INPUT_CONTAINER;
                case 2 -> SLOT_OUTPUT;
                case 3 -> SLOT_UREA;
                default -> throw new RuntimeException("Slot " + slot + " not in valid range - [0,4)");
            };
        }
    }

    private final class OutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 3;
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
                case 2 -> SLOT_CARBON_OUTPUT_CONTAINER;
                default -> throw new RuntimeException("Slot " + slot + " not in valid range - [0,3)");
            };
        }
    }

    private final class AlgaeFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> waterTank.getFluidInTank(0);
                case 1 -> carbonTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> waterTank.getCapacity();
                case 1 -> carbonTank.getCapacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return switch (tank) {
                case 0 -> waterTank.isFluidValid(stack);
                case 1 -> carbonTank.isFluidValid(stack);
                default -> false;
            };
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            if (isWater(resource)) {
                return waterTank.fill(resource, action);
            }
            if (isCarbonInput(resource)) {
                return carbonTank.fill(resource, action);
            }
            return 0;
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
