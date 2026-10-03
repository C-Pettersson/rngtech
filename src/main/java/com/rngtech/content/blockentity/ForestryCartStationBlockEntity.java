package com.rngtech.content.blockentity;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.entity.ForestryCartEntity;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.FluidConnectorItem;
import com.rngtech.content.item.ItemConnectorItem;
import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.menu.ForestryCartStationMenu;
import com.rngtech.content.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ForestryCartStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_SAPLING = 0;
    public static final int SLOT_OUTPUT_START = 1;
    public static final int OUTPUT_SLOT_COUNT = 6;
    public static final int SLOT_SHEARS_INPUT = SLOT_OUTPUT_START + OUTPUT_SLOT_COUNT;
    public static final int SLOT_FERTILIZER_INPUT = SLOT_SHEARS_INPUT + 1;
    public static final int SLOT_TOOL_INPUT = SLOT_FERTILIZER_INPUT + 1;
    /** More plantables slots, appended so older saves keep their items; with the first slot they form a 3x3 buffer. */
    public static final int SLOT_PLANTABLES_EXTRA_START = SLOT_TOOL_INPUT + 1;
    public static final int PLANTABLES_EXTRA_COUNT = 8;
    public static final int PROCESS_SLOT_COUNT = SLOT_PLANTABLES_EXTRA_START + PLANTABLES_EXTRA_COUNT;
    public static final int SLOT_ENERGY_CONNECTOR = 0;
    public static final int SLOT_ITEM_CONNECTOR = 1;
    public static final int SLOT_FLUID_CONNECTOR = 2;
    public static final int SLOT_STATION_BATTERY_CELL = 3;
    public static final int CONNECTOR_SLOT_COUNT = 4;

    public static final int STATUS_READY = 0;
    public static final int STATUS_MISSING_CART = 1;
    public static final int STATUS_MISSING_RAIL = 2;
    public static final int STATUS_MISSING_BATTERY_CELL = 3;
    public static final int STATUS_MISSING_TOOL = 4;
    public static final int STATUS_INVALID_TOOL = 5;
    public static final int STATUS_BROKEN_TOOL = 6;
    public static final int STATUS_NO_SAPLINGS = 7;
    public static final int STATUS_OUTPUT_FULL = 8;
    public static final int STATUS_NO_POWER = 9;
    public static final int STATUS_ROUTE_BLOCKED = 10;
    public static final int STATUS_CART_OFF_RAIL = 11;
    public static final int STATUS_CART_UNLOADED = 12;
    public static final int STATUS_TREE_TOO_LARGE = 13;
    public static final int STATUS_INVALID_SOIL = 14;
    public static final int STATUS_PLANTING_BLOCKED = 15;
    public static final int STATUS_HARVEST_BLOCKED = 16;
    public static final int STATUS_HOLDING_CART = 17;
    public static final int STATUS_PATH_BLOCKED = 18;

    public static final int ACTION_IDLE = 0;
    public static final int ACTION_DOCK_TRANSFER = 1;
    public static final int ACTION_RETURNING_CARGO = 2;
    public static final int ACTION_HOLDING = 3;
    public static final int ACTION_SETUP_BLOCKED = 4;
    public static final int ACTION_DOCKED_READY = 5;
    public static final int ACTION_WAITING_COOLDOWN = 6;
    public static final int ACTION_PROCESSING_SNAPSHOT = 7;
    public static final int ACTION_SNAPSHOT_OUT_OF_RANGE = 8;
    public static final int ACTION_SCANNING_LOG_BASES = 9;
    public static final int ACTION_CREATING_SNAPSHOT = 10;
    public static final int ACTION_HARVESTING_LEAVES = 11;
    public static final int ACTION_HARVESTING_LOG = 12;
    public static final int ACTION_TREEFELLER_BATCH = 13;
    public static final int ACTION_PLANTING = 14;
    public static final int ACTION_NO_SAPLINGS = 15;
    public static final int ACTION_MOVING = 16;
    public static final int ACTION_UNLOADING = 17;
    public static final int ACTION_PATH_BLOCKED = 18;
    public static final int ACTION_NO_POWER = 19;
    public static final int ACTION_OUTPUT_FULL = 20;
    public static final int ACTION_HARVEST_BLOCKED = 21;
    public static final int ACTION_PLANTING_BLOCKED = 22;
    public static final int ACTION_MANAGED = 23;
    public static final int ACTION_SEEKING_TRANSFER = 24;
    public static final int ACTION_CLEARING_PATH = 25;
    public static final int ACTION_PLAYER_BLOCKING_PATH = 26;
    public static final int ACTION_HARVESTING_CROP = 27;

    private static final int DATA_ENERGY = 0;
    private static final int DATA_ENERGY_CAPACITY = 1;
    private static final int DATA_STATUS = 2;
    private static final int DATA_HOLD_CART = 3;
    private static final int DATA_CART_DOCKED = 4;
    private static final int DATA_ENERGY_TRANSFER = 5;
    private static final int DATA_ITEM_TRANSFER = 6;
    private static final int DATA_FLUID_TRANSFER = 7;
    private static final int DATA_CURRENT_ACTION = 8;
    private static final int DATA_FERTILIZER = 9;
    private static final int DATA_WATER = 10;
    private static final int DATA_CART_TOOL_CONDITION = 11;
    private static final int DATA_CART_ENERGY_PERCENT = 12;
    private static final int DATA_CART_WORK_RANGE = 13;
    private static final int DATA_CART_WATER = 14;
    private static final int DATA_CART_CROPS = 15;
    private static final int DATA_CART_WAITING_CELLS = 16;
    private static final int DATA_COUNT = 17;

    private static final int INTERNAL_ENERGY_CAPACITY = 256;
    public static final int WATER_CAPACITY = 16000;
    private static final int CART_TRANSFER_LIMIT = ForestryCartEntity.CARGO_SLOT_COUNT * 64;

    private final ItemStackHandler processInventory = new ItemStackHandler(PROCESS_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (isPlantablesSlot(slot)) {
                return isPlantableStack(stack);
            }
            return switch (slot) {
                case SLOT_FERTILIZER_INPUT -> ForestryCartEntity.isFertilizerStack(stack);
                case SLOT_SHEARS_INPUT -> ForestryCartEntity.isShearsCandidate(stack);
                case SLOT_TOOL_INPUT -> ForestryCartEntity.isToolCandidate(stack);
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_SHEARS_INPUT || slot == SLOT_TOOL_INPUT ? 1 : super.getSlotLimit(slot);
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
    private final ItemStackHandler connectorInventory = new ItemStackHandler(CONNECTOR_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_ENERGY_CONNECTOR -> isEnergyConnector(stack);
                case SLOT_ITEM_CONNECTOR -> isItemConnector(stack);
                case SLOT_FLUID_CONNECTOR -> isFluidConnector(stack);
                case SLOT_STATION_BATTERY_CELL -> isBatteryCell(stack);
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
    private final IItemHandler topItemHandler = new StationInputItemHandler();
    private final IItemHandler bottomItemHandler = new OutputItemHandler();
    private final IEnergyStorage energyStorage = new StationEnergyStorage();
    private final FluidTank waterTank = new FluidTank(WATER_CAPACITY, stack -> stack.getFluid().isSame(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler fluidHandler = new StationFluidHandler();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY -> energyStored();
                case DATA_ENERGY_CAPACITY -> energyCapacity();
                case DATA_STATUS -> status;
                case DATA_HOLD_CART -> holdCartAtStation ? 1 : 0;
                case DATA_CART_DOCKED -> hasDockedCart() ? 1 : 0;
                case DATA_ENERGY_TRANSFER -> energyConnectorTransferRate();
                case DATA_ITEM_TRANSFER -> itemConnectorTransferLimit();
                case DATA_FLUID_TRANSFER -> fluidConnectorTransferLimit();
                case DATA_CURRENT_ACTION -> currentAction;
                case DATA_FERTILIZER -> fertilizer;
                case DATA_WATER -> waterTank.getFluidAmount();
                case DATA_CART_TOOL_CONDITION -> dockedCartToolCondition;
                case DATA_CART_ENERGY_PERCENT -> dockedCartEnergyPercent;
                case DATA_CART_WORK_RANGE -> dockedCartWorkRange;
                case DATA_CART_WATER -> dockedCartWater;
                case DATA_CART_CROPS -> dockedCartCrops ? 1 : 0;
                case DATA_CART_WAITING_CELLS -> dockedCartWaitingCells;
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

    private int internalEnergy;
    private int status = STATUS_READY;
    private int currentAction = ACTION_IDLE;
    private boolean holdCartAtStation;
    private int fertilizer;
    private int dockedCartToolCondition;
    private int dockedCartEnergyPercent;
    private int dockedCartWorkRange;
    private int dockedCartWater;
    private boolean dockedCartCrops;
    private int dockedCartWaitingCells;

    public ForestryCartStationBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.FORESTRY_CART_STATION.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ForestryCartStationBlockEntity station) {
        boolean active = station.tickStation();
        BaseMachineBlock.setActive(level, pos, state, active);
    }

    public static ForestryCartStationBlockEntity stationAtDock(Level level, BlockPos railPos) {
        if (level == null) {
            return null;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos sameLevel = railPos.relative(direction);
            if (level.getBlockEntity(sameLevel) instanceof ForestryCartStationBlockEntity station && station.isDockRail(railPos)) {
                return station;
            }
            BlockPos above = railPos.above().relative(direction);
            if (level.getBlockEntity(above) instanceof ForestryCartStationBlockEntity station && station.isDockRail(railPos)) {
                return station;
            }
        }
        return null;
    }

    public ItemStackHandler getProcessInventory() {
        return processInventory;
    }

    public ItemStackHandler getConnectorInventory() {
        return connectorInventory;
    }

    public ContainerData getMenuData() {
        return menuData;
    }

    public IItemHandler getItemHandler(Direction side) {
        if (!hasItemConnector()) {
            return null;
        }
        if (side == Direction.UP) {
            return topItemHandler;
        }
        if (side == Direction.DOWN) {
            return bottomItemHandler;
        }
        return null;
    }

    public IEnergyStorage getEnergyStorage(Direction side) {
        return hasEnergyConnector() ? energyStorage : null;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return hasFluidConnector() ? fluidHandler : null;
    }

    public boolean isBatteryCell(ItemStack stack) {
        return ForestryCartEntity.isBatteryCell(stack);
    }

    public boolean isEnergyConnector(ItemStack stack) {
        return stack.getItem() instanceof EnergyConnectorItem connector && connector.tier().enabled();
    }

    public boolean isItemConnector(ItemStack stack) {
        return stack.getItem() instanceof ItemConnectorItem connector && connector.tier().enabled();
    }

    public boolean isFluidConnector(ItemStack stack) {
        return stack.getItem() instanceof FluidConnectorItem connector && connector.tier().enabled();
    }

    public boolean isSaplingStack(ItemStack stack) {
        return isPlantableStack(stack);
    }

    /** The plantables slot takes saplings and Field Hand crops; the station cannot know which cart will dock next. */
    public static boolean isPlantableStack(ItemStack stack) {
        return ForestryCartEntity.isSaplingStack(stack) || ForestryCartEntity.isCropStack(stack);
    }

    public int waterStored() {
        return waterTank.getFluidAmount();
    }

    public boolean isShearsStack(ItemStack stack) {
        return ForestryCartEntity.isShearsCandidate(stack);
    }

    public void setOwner(Player player) {
        // Stations no longer own cart work; retained for old block placement call sites.
    }

    public boolean toggleHoldCartAtStation() {
        holdCartAtStation = !holdCartAtStation;
        setChanged();
        return true;
    }

    public boolean holdCartAtStation() {
        return holdCartAtStation;
    }

    public boolean hasSaplingsQueued() {
        for (int slot : plantablesSlots()) {
            if (!processInventory.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean canLoadQueuedSaplingsInto(ForestryCartEntity cart) {
        for (int slot : plantablesSlots()) {
            ItemStack stack = processInventory.getStackInSlot(slot);
            if (!stack.isEmpty() && cart.canAcceptSaplings(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPlantablesSlot(int slot) {
        return slot == SLOT_SAPLING || slot >= SLOT_PLANTABLES_EXTRA_START && slot < SLOT_PLANTABLES_EXTRA_START + PLANTABLES_EXTRA_COUNT;
    }

    private static int[] plantablesSlots() {
        int[] slots = new int[PLANTABLES_EXTRA_COUNT + 1];
        slots[0] = SLOT_SAPLING;
        for (int index = 0; index < PLANTABLES_EXTRA_COUNT; index++) {
            slots[index + 1] = SLOT_PLANTABLES_EXTRA_START + index;
        }
        return slots;
    }

    public boolean isDockRail(BlockPos railPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = worldPosition.relative(direction);
            if (railPos.equals(side) || railPos.equals(side.below())) {
                return true;
            }
        }
        return false;
    }

    public boolean serviceCart(ForestryCartEntity cart, BlockPos railPos) {
        if (!isDockRail(railPos)) {
            return false;
        }
        boolean changed = unloadCartOutputs(cart);
        changed |= chargeDockedCart(cart);
        changed |= loadCartSaplings(cart);
        changed |= loadCartFertilizer(cart);
        changed |= loadCartWater(cart);
        changed |= loadCartShears(cart);
        changed |= loadCartTool(cart);
        if (changed) {
            setChanged();
        }
        return changed;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.forestry_cart_station");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ForestryCartStationMenu(containerId, playerInventory, this, menuData);
    }

    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }

    public void dropInventory(Level level) {
        for (int slot = 0; slot < processInventory.getSlots(); slot++) {
            dropSlot(level, processInventory, slot);
        }
        for (int slot = 0; slot < connectorInventory.getSlots(); slot++) {
            dropSlot(level, connectorInventory, slot);
        }
        while (fertilizer > 0) {
            int count = Math.min(fertilizer, Items.BONE_MEAL.getDefaultMaxStackSize());
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), new ItemStack(Items.BONE_MEAL, count));
            fertilizer -= count;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ProcessInventory", processInventory.serializeNBT(registries));
        tag.put("ConnectorInventory", connectorInventory.serializeNBT(registries));
        tag.putInt("Energy", internalEnergy);
        tag.putBoolean("HoldCartAtStation", holdCartAtStation);
        tag.putInt("Fertilizer", fertilizer);
        tag.put("WaterTank", waterTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        processInventory.deserializeNBT(registries, tag.getCompound("ProcessInventory"));
        connectorInventory.deserializeNBT(registries, tag.getCompound("ConnectorInventory"));
        ensureInventorySize(processInventory, PROCESS_SLOT_COUNT);
        ensureConnectorInventorySize();
        internalEnergy = Math.max(0, tag.getInt("Energy"));
        holdCartAtStation = tag.getBoolean("HoldCartAtStation");
        fertilizer = Math.max(0, Math.min(ForestryCartEntity.FERTILIZER_CAPACITY, tag.getInt("Fertilizer")));
        waterTank.setFluid(FluidStack.EMPTY);
        if (tag.contains("WaterTank")) {
            waterTank.readFromNBT(registries, tag.getCompound("WaterTank"));
        }
        clampInternalEnergy();
    }

    private boolean tickStation() {
        if (level == null) {
            setStatus(STATUS_MISSING_RAIL);
            setCurrentAction(ACTION_SETUP_BLOCKED);
            return false;
        }
        if (dockRailPos() == null) {
            setStatus(STATUS_MISSING_RAIL);
            setCurrentAction(ACTION_SETUP_BLOCKED);
            return false;
        }

        absorbSlotFertilizer();
        boolean docked = false;
        boolean transferred = false;
        boolean cartNeedsRecharge = false;
        clearDockedCartSummary();
        for (ForestryCartEntity cart : dockedCarts()) {
            BlockPos railPos = cart.railPosition();
            if (railPos == null) {
                continue;
            }
            docked = true;
            recordDockedCartSummary(cart);
            transferred |= serviceCart(cart, railPos);
            cartNeedsRecharge |= cart.energyCapacity() > 0 && cart.energyStored() < cart.energyCapacity();
        }

        if (holdCartAtStation && docked) {
            setStatus(STATUS_HOLDING_CART);
            setCurrentAction(ACTION_HOLDING);
            return true;
        }
        setStatus(STATUS_READY);
        int action = ACTION_IDLE;
        if (transferred || cartNeedsRecharge) {
            action = ACTION_DOCK_TRANSFER;
        } else if (docked) {
            action = ACTION_DOCKED_READY;
        }
        setCurrentAction(action);
        return docked || transferred;
    }

    private List<ForestryCartEntity> dockedCarts() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        AABB search = new AABB(worldPosition).inflate(2.0D, 1.0D, 2.0D);
        List<ForestryCartEntity> carts = new ArrayList<>();
        for (ForestryCartEntity cart : serverLevel.getEntitiesOfClass(ForestryCartEntity.class, search)) {
            BlockPos railPos = cart.railPosition();
            if (railPos != null && isDockRail(railPos)) {
                carts.add(cart);
            }
        }
        return carts;
    }

    private boolean hasDockedCart() {
        if (level == null) {
            return false;
        }
        if (level.isClientSide) {
            return status == STATUS_HOLDING_CART || currentAction == ACTION_DOCK_TRANSFER || currentAction == ACTION_DOCKED_READY;
        }
        return !dockedCarts().isEmpty();
    }

    private BlockPos dockRailPos() {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = worldPosition.relative(direction);
            if (isRail(side)) {
                return side;
            }
            BlockPos below = side.below();
            if (isRail(below)) {
                return below;
            }
        }
        return null;
    }

    private boolean isRail(BlockPos pos) {
        return level != null && level.getBlockState(pos).is(BlockTags.RAILS);
    }

    private boolean unloadCartOutputs(ForestryCartEntity cart) {
        ItemStackHandler cargo = cart.cargoInventory();
        int remainingTransfer = CART_TRANSFER_LIMIT;
        boolean moved = false;
        for (int slot = ForestryCartEntity.OUTPUT_SLOT_START;
                slot < ForestryCartEntity.CARGO_SLOT_COUNT && remainingTransfer > 0;
                slot++) {
            ItemStack stack = cargo.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack candidate = stack.copyWithCount(Math.min(remainingTransfer, stack.getCount()));
            ItemStack simulatedRemainder = insertStationOutput(candidate, true);
            int accepted = candidate.getCount() - simulatedRemainder.getCount();
            if (accepted <= 0) {
                continue;
            }

            ItemStack extracted = cargo.extractItem(slot, accepted, false);
            ItemStack leftover = insertStationOutput(extracted, false);
            if (!leftover.isEmpty() && level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1.0D, worldPosition.getZ(), leftover);
            }
            remainingTransfer -= accepted;
            moved = true;
        }
        return moved;
    }

    /**
     * Loads what the docked cart's Seed Library cells are waiting for first, making room by moving supply nobody waits
     * for into the plantables buffer. Other plantables top up only species the cart already carries while cells wait.
     */
    private boolean loadCartSaplings(ForestryCartEntity cart) {
        if (!hasSaplingsQueued()) {
            return false;
        }
        boolean cartWaitingForSaplings = !cart.hasSaplings() || cart.isWaitingForStationSaplings(worldPosition);
        Map<Item, Integer> demand = cart.plantDemand();
        int moved = 0;
        for (Map.Entry<Item, Integer> entry : demand.entrySet()) {
            Item item = entry.getKey();
            int wanted = Math.min(entry.getValue() - cart.supplyCount(item), plantablesCount(item));
            if (wanted <= 0) {
                continue;
            }
            ItemStack candidate = new ItemStack(item, Math.min(CART_TRANSFER_LIMIT, wanted));
            if (!cart.canAcceptSaplings(candidate)) {
                freeCartSupplySlot(cart, demand);
            }
            int loaded = candidate.getCount() - cart.insertSaplings(candidate).getCount();
            takePlantables(item, loaded);
            moved += loaded;
        }
        for (int slot : plantablesSlots()) {
            ItemStack stack = processInventory.getStackInSlot(slot);
            if (stack.isEmpty() || !demand.isEmpty() && cart.supplyCount(stack.getItem()) == 0) {
                continue;
            }
            ItemStack candidate = stack.copyWithCount(Math.min(CART_TRANSFER_LIMIT, stack.getCount()));
            int loaded = candidate.getCount() - cart.insertSaplings(candidate).getCount();
            if (loaded > 0) {
                processInventory.extractItem(slot, loaded, false);
                moved += loaded;
            }
        }
        if (moved > 0 && cartWaitingForSaplings) {
            cart.recordStationSaplingTransfer(worldPosition);
        }
        return moved > 0;
    }

    /** Moves one cart supply stack that no waiting cell needs into the plantables buffer, or the outputs. */
    private void freeCartSupplySlot(ForestryCartEntity cart, Map<Item, Integer> demand) {
        var supply = cart.cargoInventory();
        for (int slot = ForestryCartEntity.SAPLING_SLOT_START; slot < ForestryCartEntity.OUTPUT_SLOT_START; slot++) {
            ItemStack stack = supply.getStackInSlot(slot);
            if (stack.isEmpty() || demand.containsKey(stack.getItem())) {
                continue;
            }
            if (insertPlantables(stack, true).isEmpty()) {
                insertPlantables(stack, false);
            } else if (insertStationOutput(stack, true).isEmpty()) {
                insertStationOutput(stack, false);
            } else {
                continue;
            }
            supply.setStackInSlot(slot, ItemStack.EMPTY);
            return;
        }
    }

    private int plantablesCount(Item item) {
        int count = 0;
        for (int slot : plantablesSlots()) {
            ItemStack stack = processInventory.getStackInSlot(slot);
            count += stack.is(item) ? stack.getCount() : 0;
        }
        return count;
    }

    private void takePlantables(Item item, int count) {
        for (int slot : plantablesSlots()) {
            if (count <= 0) {
                return;
            }
            if (processInventory.getStackInSlot(slot).is(item)) {
                count -= processInventory.extractItem(slot, count, false).getCount();
            }
        }
    }

    private ItemStack insertPlantables(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
            for (int slot : plantablesSlots()) {
                boolean empty = processInventory.getStackInSlot(slot).isEmpty();
                if (pass == 0 && !empty || pass == 1 && empty) {
                    remaining = processInventory.insertItem(slot, remaining, simulate);
                }
            }
        }
        return remaining;
    }

    /** Field Hand sprinklers take water from the station tank, as fast as the cart's Fluid Pump allows. */
    private boolean loadCartWater(ForestryCartEntity cart) {
        int available = waterTank.getFluidAmount();
        int moved = available > 0 ? cart.acceptWater(available, false) : 0;
        if (moved > 0) {
            waterTank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
        }
        return moved > 0;
    }

    /** Grove Warden carts take bone meal from the station's fertilizer store. */
    private boolean loadCartFertilizer(ForestryCartEntity cart) {
        int moved = fertilizer > 0 ? cart.acceptFertilizer(fertilizer) : 0;
        fertilizer -= moved;
        return moved > 0;
    }

    private int storeFertilizer(int count, boolean simulate) {
        int accepted = Math.max(0, Math.min(count, ForestryCartEntity.FERTILIZER_CAPACITY - fertilizer));
        if (!simulate && accepted > 0) {
            fertilizer += accepted;
            setChanged();
        }
        return accepted;
    }

    /**
     * Bone meal in the bone meal slot drains into the fertilizer store. Bone meal an older station left in the
     * plantables slot drains too, so it never blocks saplings.
     */
    private void absorbSlotFertilizer() {
        for (int slot : new int[] {SLOT_FERTILIZER_INPUT, SLOT_SAPLING}) {
            ItemStack stack = processInventory.getStackInSlot(slot);
            if (ForestryCartEntity.isFertilizerStack(stack)) {
                int accepted = storeFertilizer(stack.getCount(), false);
                if (accepted > 0) {
                    processInventory.setStackInSlot(slot, stack.copyWithCount(stack.getCount() - accepted));
                }
            }
        }
    }

    /**
     * A tree-working cart with no tool, or a broken one, takes the station's spare. The broken tool goes to the
     * station outputs, and nothing is swapped while they have no room for it.
     */
    private boolean loadCartTool(ForestryCartEntity cart) {
        ItemStack spare = processInventory.getStackInSlot(SLOT_TOOL_INPUT);
        if (spare.isEmpty() || !cart.worksTrees()) {
            return false;
        }
        ItemStack installed = cart.toolStack();
        if (!installed.isEmpty() && !ModularToolItem.isBroken(installed)) {
            return false;
        }
        if (!installed.isEmpty() && !insertStationOutput(installed, true).isEmpty()) {
            return false;
        }
        if (!installed.isEmpty()) {
            insertStationOutput(installed, false);
        }
        cart.setToolStack(processInventory.extractItem(SLOT_TOOL_INPUT, 1, false));
        return true;
    }

    private void clearDockedCartSummary() {
        dockedCartToolCondition = 0;
        dockedCartEnergyPercent = 0;
        dockedCartWorkRange = 0;
        dockedCartWater = 0;
        dockedCartCrops = false;
        dockedCartWaitingCells = 0;
    }

    private void recordDockedCartSummary(ForestryCartEntity cart) {
        dockedCartToolCondition = cart.toolCondition();
        dockedCartEnergyPercent = cart.energyCapacity() <= 0 ? 0 : (int) Math.floor(100.0 * cart.energyStored() / cart.energyCapacity());
        dockedCartWorkRange = cart.workRange();
        dockedCartWater = cart.waterStored();
        dockedCartCrops = cart.tendsCrops();
        dockedCartWaitingCells = cart.plantDemand().values().stream().mapToInt(Integer::intValue).sum();
    }

    private boolean loadCartShears(ForestryCartEntity cart) {
        ItemStack stack = shearsInputStack();
        if (stack.isEmpty() || !cart.shearsStack().isEmpty() || !ForestryCartEntity.isShearsCandidate(stack)) {
            return false;
        }

        ItemStack extracted = processInventory.extractItem(SLOT_SHEARS_INPUT, 1, false);
        if (extracted.isEmpty()) {
            return false;
        }
        cart.setShearsStack(extracted);
        return true;
    }

    private boolean chargeDockedCart(ForestryCartEntity cart) {
        int requested = Math.min(cartChargeTransferRate(), cart.energyCapacity() - cart.energyStored());
        if (requested <= 0) {
            return false;
        }

        int available = extractEnergyInternal(requested, true);
        if (available <= 0) {
            return false;
        }

        int accepted = cart.receiveEnergy(available, true);
        if (accepted <= 0) {
            return false;
        }

        int moved = Math.min(available, accepted);
        extractEnergyInternal(moved, false);
        cart.receiveEnergy(moved, false);
        return true;
    }

    private ItemStack saplingStack() {
        return processInventory.getStackInSlot(SLOT_SAPLING);
    }

    private ItemStack shearsInputStack() {
        return processInventory.getStackInSlot(SLOT_SHEARS_INPUT);
    }

    private ItemStack energyConnectorStack() {
        return connectorInventory.getStackInSlot(SLOT_ENERGY_CONNECTOR);
    }

    private ItemStack itemConnectorStack() {
        return connectorInventory.getStackInSlot(SLOT_ITEM_CONNECTOR);
    }

    private ItemStack fluidConnectorStack() {
        return connectorInventory.getStackInSlot(SLOT_FLUID_CONNECTOR);
    }

    private ItemStack stationBatteryCellStack() {
        return connectorInventory.getStackInSlot(SLOT_STATION_BATTERY_CELL);
    }

    private IEnergyStorage stationBatteryCellEnergyStorage() {
        ItemStack stack = stationBatteryCellStack();
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    private boolean hasEnergyConnector() {
        return isEnergyConnector(energyConnectorStack());
    }

    private boolean hasItemConnector() {
        return isItemConnector(itemConnectorStack());
    }

    private boolean hasFluidConnector() {
        return isFluidConnector(fluidConnectorStack());
    }

    private int energyStored() {
        return internalEnergyStored() + BatteryCellItem.energyStored(stationBatteryCellStack());
    }

    private int energyCapacity() {
        return internalEnergyCapacity() + BatteryCellItem.energyCapacity(stationBatteryCellStack());
    }

    private int internalEnergyCapacity() {
        return INTERNAL_ENERGY_CAPACITY;
    }

    private int internalEnergyStored() {
        return Math.max(0, Math.min(internalEnergy, internalEnergyCapacity()));
    }

    private int energyConnectorTransferRate() {
        ItemStack stack = energyConnectorStack();
        if (stack.getItem() instanceof EnergyConnectorItem connector && connector.tier().enabled()) {
            return connector.tier().transferRate();
        }
        return 0;
    }

    private int cartChargeTransferRate() {
        return Math.max(energyConnectorTransferRate(), BatteryCellItem.maxOutput(stationBatteryCellStack()));
    }

    private int itemConnectorTransferLimit() {
        ItemStack stack = itemConnectorStack();
        if (stack.getItem() instanceof ItemConnectorItem connector && connector.tier().enabled()) {
            return connector.tier().itemsPerShipment();
        }
        return 0;
    }

    private int fluidConnectorTransferLimit() {
        ItemStack stack = fluidConnectorStack();
        if (stack.getItem() instanceof FluidConnectorItem connector && connector.tier().enabled()) {
            return connector.tier().fluidPerShipment();
        }
        return 0;
    }

    private int extractEnergyInternal(int amount, boolean simulate) {
        int transferLimit = cartChargeTransferRate();
        if (amount <= 0 || transferLimit <= 0) {
            return 0;
        }

        int remaining = Math.min(amount, transferLimit);
        int internal = Math.min(internalEnergyStored(), remaining);
        remaining -= internal;

        IEnergyStorage cell = stationBatteryCellEnergyStorage();
        int cellExtracted = 0;
        if (remaining > 0 && cell != null && cell.canExtract()) {
            cellExtracted = cell.extractEnergy(remaining, true);
        }

        int extracted = internal + cellExtracted;
        if (!simulate && extracted > 0) {
            if (internal > 0) {
                internalEnergy = internalEnergyStored() - internal;
            }
            if (cellExtracted > 0) {
                cell.extractEnergy(cellExtracted, false);
            }
            setChanged();
        }
        return extracted;
    }

    private int receiveEnergyInternal(int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        int remaining = Math.min(amount, energyConnectorTransferRate());
        int received = Math.min(internalEnergyCapacity() - internalEnergyStored(), remaining);
        remaining -= received;

        IEnergyStorage cell = stationBatteryCellEnergyStorage();
        int cellReceived = 0;
        if (remaining > 0 && cell != null && cell.canReceive()) {
            cellReceived = cell.receiveEnergy(remaining, simulate);
        }

        if (!simulate && (received > 0 || cellReceived > 0)) {
            if (received > 0) {
                internalEnergy = internalEnergyStored() + received;
            }
            setChanged();
        }
        return received + cellReceived;
    }

    private ItemStack insertStationOutput(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        remaining = mergeStationOutputStacks(remaining, simulate);
        return fillEmptyStationOutputSlots(remaining, simulate);
    }

    private ItemStack mergeStationOutputStacks(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = SLOT_OUTPUT_START; slot < SLOT_OUTPUT_START + OUTPUT_SLOT_COUNT && !remaining.isEmpty(); slot++) {
            ItemStack existing = processInventory.getStackInSlot(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) {
                continue;
            }
            int room = Math.min(existing.getMaxStackSize(), processInventory.getSlotLimit(slot)) - existing.getCount();
            if (room <= 0) {
                continue;
            }
            int moved = Math.min(room, remaining.getCount());
            if (!simulate) {
                ItemStack merged = existing.copy();
                merged.grow(moved);
                processInventory.setStackInSlot(slot, merged);
            }
            remaining.shrink(moved);
        }
        return remaining;
    }

    private ItemStack fillEmptyStationOutputSlots(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = SLOT_OUTPUT_START; slot < SLOT_OUTPUT_START + OUTPUT_SLOT_COUNT && !remaining.isEmpty(); slot++) {
            if (!processInventory.getStackInSlot(slot).isEmpty()) {
                continue;
            }
            int slotLimit = Math.min(remaining.getMaxStackSize(), processInventory.getSlotLimit(slot));
            int moved = Math.min(slotLimit, remaining.getCount());
            if (moved <= 0) {
                continue;
            }
            if (!simulate) {
                processInventory.setStackInSlot(slot, remaining.copyWithCount(moved));
            }
            remaining.shrink(moved);
        }
        return remaining;
    }

    private void clampInternalEnergy() {
        internalEnergy = internalEnergyStored();
    }

    private void ensureConnectorInventorySize() {
        ensureInventorySize(connectorInventory, CONNECTOR_SLOT_COUNT);
    }

    /** Older saves have fewer slots; new slots are appended so existing items keep their positions. */
    private static void ensureInventorySize(ItemStackHandler inventory, int size) {
        if (inventory.getSlots() >= size) {
            return;
        }
        List<ItemStack> existing = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            existing.add(inventory.getStackInSlot(slot).copy());
        }
        inventory.setSize(size);
        for (int slot = 0; slot < existing.size(); slot++) {
            inventory.setStackInSlot(slot, existing.get(slot));
        }
    }

    private void setStatus(int status) {
        if (this.status != status) {
            this.status = status;
            setChanged();
        }
    }

    private void setCurrentAction(int currentAction) {
        if (this.currentAction != currentAction) {
            this.currentAction = currentAction;
            setChanged();
        }
    }

    private void dropSlot(Level level, ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private final class StationInputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 4;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(inputSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            inputSlot(slot);
            if (stack.isEmpty()) {
                return stack;
            }
            int mappedSlot = targetInputSlot(stack);
            if (mappedSlot < 0) {
                return stack;
            }
            int limit = Math.min(stack.getCount(), Math.max(0, itemConnectorTransferLimit()));
            if (limit <= 0) {
                return stack;
            }
            ItemStack candidate = stack.copyWithCount(limit);
            ItemStack remainder = mappedSlot == SLOT_SAPLING
                    ? insertPlantables(candidate, simulate)
                    : processInventory.insertItem(mappedSlot, candidate, simulate);
            int accepted = candidate.getCount() - remainder.getCount();
            if (accepted <= 0) {
                return stack;
            }
            ItemStack result = stack.copy();
            result.shrink(accepted);
            return result;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            inputSlot(slot);
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(inputSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            inputSlot(slot);
            return targetInputSlot(stack) >= 0;
        }

        private int inputSlot(int slot) {
            return switch (slot) {
                case 0 -> SLOT_SAPLING;
                case 1 -> SLOT_SHEARS_INPUT;
                case 2 -> SLOT_FERTILIZER_INPUT;
                case 3 -> SLOT_TOOL_INPUT;
                default -> throw new RuntimeException("Slot " + slot + " not in valid range - [0,4)");
            };
        }

        private int targetInputSlot(ItemStack stack) {
            for (int slot : new int[] {SLOT_SAPLING, SLOT_FERTILIZER_INPUT, SLOT_SHEARS_INPUT, SLOT_TOOL_INPUT}) {
                if (processInventory.isItemValid(slot, stack)) {
                    return slot;
                }
            }
            return -1;
        }

    }

    private final class OutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return OUTPUT_SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return processInventory.getStackInSlot(outputSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            outputSlot(slot);
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int limit = Math.min(amount, itemConnectorTransferLimit());
            return limit <= 0 ? ItemStack.EMPTY : processInventory.extractItem(outputSlot(slot), limit, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return processInventory.getSlotLimit(outputSlot(slot));
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            outputSlot(slot);
            return false;
        }

        private int outputSlot(int slot) {
            if (slot < 0 || slot >= OUTPUT_SLOT_COUNT) {
                throw new RuntimeException("Slot " + slot + " not in valid range - [0," + OUTPUT_SLOT_COUNT + ")");
            }
            return SLOT_OUTPUT_START + slot;
        }
    }

    private final class StationEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return receiveEnergyInternal(toReceive, simulate);
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
            return hasEnergyConnector();
        }
    }

    /** Water-only input for Field Hand sprinklers, capped per call by the installed Fluid Connector. */
    private final class StationFluidHandler implements IFluidHandler {
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
            return tank == 0 ? WATER_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && waterTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int limit = fluidConnectorTransferLimit();
            if (resource.isEmpty() || limit <= 0) {
                return 0;
            }
            return waterTank.fill(resource.copyWithAmount(Math.min(resource.getAmount(), limit)), action);
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
