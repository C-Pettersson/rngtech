package com.rngtech.content.menu;

import com.rngtech.content.entity.ForestryCartEntity;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.FluidConnectorItem;
import com.rngtech.content.item.ItemConnectorItem;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.progression.ForestryCompanionPassiveTree;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveNode;
import com.rngtech.rpg.progression.PassiveProgressionView;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;
import java.util.function.BooleanSupplier;

public class ForestryCartMenu extends AbstractContainerMenu implements MasteryMenuView<MegaPassiveNode> {
    public static final int TAB_CART = 0;
    public static final int TAB_STATS = 1;
    public static final int TAB_MASTERY = 2;
    public static final int BUTTON_TOGGLE_SCAN_DEBUG = 0;
    public static final int BUTTON_RELEASE_MANAGEMENT_VIEW = 1;
    public static final int BUTTON_TOGGLE_MANUAL_SPEED = 2;
    public static final int BUTTON_RESET_MANAGED_CELLS = 3;

    public static final int DATA_STATUS = 0;
    public static final int DATA_CURRENT_ACTION = 1;
    public static final int DATA_MANAGED_CELLS = 2;
    public static final int DATA_ACTIVE_CELLS = 3;
    public static final int DATA_MAX_CONNECTED_LOGS = 4;
    public static final int DATA_MAX_TREE_HEIGHT = 5;
    public static final int DATA_MOVEMENT_FE = 6;
    public static final int DATA_SCAN_FE = 7;
    public static final int DATA_PLANT_FE = 8;
    public static final int DATA_CUT_FE = 9;
    public static final int DATA_WORK_COOLDOWN = 10;
    public static final int DATA_CART_ENERGY = 11;
    public static final int DATA_CART_ENERGY_CAPACITY = 12;
    public static final int DATA_SCAN_DEBUG_VISIBLE = 13;
    public static final int DATA_WORKFLOW_STATE = 14;
    public static final int DATA_MAX_MANAGED_CELLS = 15;
    public static final int DATA_LOGS_PER_ACTION = 16;
    public static final int DATA_SAPLING_CARGO = 17;
    public static final int DATA_SAPLING_CARGO_CAPACITY = 18;
    public static final int DATA_OUTPUT_CARGO = 19;
    public static final int DATA_OUTPUT_CARGO_CAPACITY = 20;
    public static final int DATA_TOOL_CONDITION = 21;
    public static final int DATA_TOOL_DURABILITY = 22;
    public static final int DATA_TOOL_MAX_DURABILITY = 23;
    public static final int DATA_LEAF_COLLECTION_ENABLED = 24;
    public static final int DATA_SHEARS_CONDITION = 25;
    public static final int DATA_SHEARS_DURABILITY = 26;
    public static final int DATA_SHEARS_MAX_DURABILITY = 27;
    public static final int DATA_SHEARS_LEAF_BUDGET = 28;
    public static final int DATA_WORK_INTERVAL = 29;
    public static final int DATA_SHEAR_INTERVAL = 30;
    public static final int DATA_POWERED_SPEED_MILLI = 31;
    public static final int DATA_TRANSFER_SEEKING_SPEED_MILLI = 32;
    public static final int DATA_UNPOWERED_SPEED_MILLI = 33;
    public static final int DATA_UNSHEARED_LEAF_FE = 34;
    public static final int DATA_MANUAL_SPEED_ENABLED = 35;
    public static final int DATA_MANUAL_SPEED_UNLOCKED = 36;
    public static final int DATA_CORE_CONTROL = 37;
    public static final int DATA_CORE_DRIVE = 38;
    public static final int DATA_CORE_RESERVE = 39;
    public static final int DATA_FERTILIZER = 40;
    public static final int DATA_FERTILIZER_CAPACITY = 41;
    public static final int DATA_WATER = 42;
    public static final int DATA_WATER_CAPACITY = 43;
    public static final int DATA_WORK_RANGE = 44;
    public static final int DATA_IDLE_SPEED_ACTIVE = 45;
    public static final int DATA_RESERVED_PLANTABLES = 46;
    public static final int DATA_MACHINE_PROGRESSION_START = 47;
    public static final int DATA_COUNT = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;

    private static final int CART_BATTERY_SLOT = 0;
    private static final int CART_TOOL_SLOT = 1;
    private static final int CART_SHEARS_SLOT = 2;
    private static final int CART_CARGO_SLOT_START = 3;
    private static final int CART_OUTPUT_SLOT_START = CART_CARGO_SLOT_START + ForestryCartEntity.SAPLING_SLOT_COUNT;
    private static final int CART_PUMP_SLOT = CART_OUTPUT_SLOT_START + ForestryCartEntity.OUTPUT_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = CART_PUMP_SLOT + 1;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerData data;
    private final ForestryCartEntity cart;
    private final PassiveProgressionView passiveProgressionView =
            MasteryMenuSupport.progressionView(this::hasPassiveNodeIndex, this::machineLevel, this::unspentPassivePoints, MachineMasteryFamily.FORESTRY.startNodeId());
    private int selectedTab = TAB_CART;
    private boolean managementViewer = true;

    public ForestryCartMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, cartEntity(playerInventory, extraData.readInt()), new SimpleContainerData(DATA_COUNT));
    }

    public ForestryCartMenu(int containerId, Inventory playerInventory, ForestryCartEntity cart, ContainerData data) {
        super(ModMenus.FORESTRY_CART.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.cart = cart;
        this.data = data;

        addSlot(new TabbedSlot(
                cart.gearInventory(),
                ForestryCartEntity.SLOT_BATTERY_CELL,
                43,
                52,
                () -> selectedTab == TAB_CART
        ));
        addSlot(new TabbedSlot(
                cart.gearInventory(),
                ForestryCartEntity.SLOT_TOOL,
                73,
                52,
                () -> selectedTab == TAB_CART
        ));
        addSlot(new TabbedSlot(
                cart.gearInventory(),
                ForestryCartEntity.SLOT_SHEARS,
                103,
                52,
                () -> selectedTab == TAB_CART
        ));

        for (int slot = 0; slot < ForestryCartEntity.SAPLING_SLOT_COUNT; slot++) {
            addSlot(new TabbedSlot(
                    cart.cargoInventory(),
                    ForestryCartEntity.SAPLING_SLOT_START + slot,
                    43 + slot * 18,
                    82,
                    () -> selectedTab == TAB_CART
            ));
        }
        for (int slot = 0; slot < ForestryCartEntity.OUTPUT_SLOT_COUNT; slot++) {
            addSlot(new TabbedSlot(
                    cart.cargoInventory(),
                    ForestryCartEntity.OUTPUT_SLOT_START + slot,
                    92 + slot * 18,
                    82,
                    () -> selectedTab == TAB_CART
            ));
        }

        addSlot(new TabbedSlot(
                cart.gearInventory(),
                ForestryCartEntity.SLOT_PUMP,
                13,
                52,
                () -> selectedTab == TAB_CART
        ));

        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_STATS -> TAB_STATS;
            case TAB_MASTERY -> TAB_MASTERY;
            default -> TAB_CART;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    public int currentAction() {
        return data.get(DATA_CURRENT_ACTION);
    }

    public int managedCells() {
        return data.get(DATA_MANAGED_CELLS);
    }

    public int activeCells() {
        return data.get(DATA_ACTIVE_CELLS);
    }

    public int maxConnectedLogs() {
        return data.get(DATA_MAX_CONNECTED_LOGS);
    }

    public int maxTreeHeight() {
        return data.get(DATA_MAX_TREE_HEIGHT);
    }

    public int movementFe() {
        return data.get(DATA_MOVEMENT_FE);
    }

    public int scanFe() {
        return data.get(DATA_SCAN_FE);
    }

    public int plantFe() {
        return data.get(DATA_PLANT_FE);
    }

    public int cutFe() {
        return data.get(DATA_CUT_FE);
    }

    public int workCooldown() {
        return data.get(DATA_WORK_COOLDOWN);
    }

    public int cartEnergy() {
        return data.get(DATA_CART_ENERGY);
    }

    public int cartEnergyCapacity() {
        return data.get(DATA_CART_ENERGY_CAPACITY);
    }

    public boolean scanDebugVisible() {
        return data.get(DATA_SCAN_DEBUG_VISIBLE) != 0;
    }

    public int workflowState() {
        return data.get(DATA_WORKFLOW_STATE);
    }

    public int maxManagedCells() {
        return data.get(DATA_MAX_MANAGED_CELLS);
    }

    public int logsPerAction() {
        return data.get(DATA_LOGS_PER_ACTION);
    }

    public int saplingCargo() {
        return data.get(DATA_SAPLING_CARGO);
    }

    public int saplingCargoCapacity() {
        return data.get(DATA_SAPLING_CARGO_CAPACITY);
    }

    public int fertilizer() {
        return data.get(DATA_FERTILIZER);
    }

    /** Zero unless the cart has Growth Pulse. */
    public int fertilizerCapacity() {
        return data.get(DATA_FERTILIZER_CAPACITY);
    }

    public int water() {
        return data.get(DATA_WATER);
    }

    /** Gear slot hints apply only to the cart's own Gear, not to supply cargo slots that share handler indices. */
    public boolean isGearSlot(Slot slot) {
        return slot instanceof SlotItemHandler handlerSlot && handlerSlot.getItemHandler() == cart.gearInventory();
    }

    public int waterCapacity() {
        return data.get(DATA_WATER_CAPACITY);
    }

    public int workRange() {
        return data.get(DATA_WORK_RANGE);
    }

    public boolean idleSpeedActive() {
        return data.get(DATA_IDLE_SPEED_ACTIVE) != 0;
    }

    public int reservedPlantables() {
        return data.get(DATA_RESERVED_PLANTABLES);
    }

    public int outputCargo() {
        return data.get(DATA_OUTPUT_CARGO);
    }

    public int outputCargoCapacity() {
        return data.get(DATA_OUTPUT_CARGO_CAPACITY);
    }

    public int toolCondition() {
        return data.get(DATA_TOOL_CONDITION);
    }

    public int toolDurability() {
        return data.get(DATA_TOOL_DURABILITY);
    }

    public int toolMaxDurability() {
        return data.get(DATA_TOOL_MAX_DURABILITY);
    }

    public boolean leafCollectionEnabled() {
        return data.get(DATA_LEAF_COLLECTION_ENABLED) != 0;
    }

    public int shearsCondition() {
        return data.get(DATA_SHEARS_CONDITION);
    }

    public int shearsDurability() {
        return data.get(DATA_SHEARS_DURABILITY);
    }

    public int shearsMaxDurability() {
        return data.get(DATA_SHEARS_MAX_DURABILITY);
    }

    public int shearsLeafBudget() {
        return data.get(DATA_SHEARS_LEAF_BUDGET);
    }

    public int workInterval() {
        return data.get(DATA_WORK_INTERVAL);
    }

    public int shearInterval() {
        return data.get(DATA_SHEAR_INTERVAL);
    }

    public int poweredSpeedMilli() {
        return data.get(DATA_POWERED_SPEED_MILLI);
    }

    public int transferSeekingSpeedMilli() {
        return data.get(DATA_TRANSFER_SEEKING_SPEED_MILLI);
    }

    public int unpoweredSpeedMilli() {
        return data.get(DATA_UNPOWERED_SPEED_MILLI);
    }

    public int unshearedLeafFe() {
        return data.get(DATA_UNSHEARED_LEAF_FE);
    }

    public boolean manualSpeedEnabled() {
        return data.get(DATA_MANUAL_SPEED_ENABLED) != 0;
    }

    public boolean manualSpeedUnlocked() {
        return data.get(DATA_MANUAL_SPEED_UNLOCKED) != 0;
    }

    public int coreControl() {
        return data.get(DATA_CORE_CONTROL);
    }

    public int coreDrive() {
        return data.get(DATA_CORE_DRIVE);
    }

    public int coreReserve() {
        return data.get(DATA_CORE_RESERVE);
    }

    public float cartEnergyProgress() {
        int capacity = cartEnergyCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp((float) cartEnergy() / (float) capacity, 0.0F, 1.0F);
    }

    @Override
    public long machineXp() {
        return MasteryMenuSupport.machineXp(data, DATA_MACHINE_PROGRESSION_START);
    }

    @Override
    public int machineLevel() {
        return MasteryMenuSupport.machineLevel(data, DATA_MACHINE_PROGRESSION_START);
    }

    @Override
    public int machineXpInLevel() {
        return MasteryMenuSupport.machineXpInLevel(data, DATA_MACHINE_PROGRESSION_START);
    }

    @Override
    public int machineXpToNextLevel() {
        return MasteryMenuSupport.machineXpToNextLevel(data, DATA_MACHINE_PROGRESSION_START);
    }

    @Override
    public float machineXpProgress() {
        return MasteryMenuSupport.machineXpProgress(data, DATA_MACHINE_PROGRESSION_START);
    }

    @Override
    public int unspentPassivePoints() {
        return MasteryMenuSupport.unspentPassivePoints(data, DATA_MACHINE_PROGRESSION_START);
    }

    public long unlockedPassiveNodeMask() {
        return MasteryMenuSupport.unlockedPassiveNodeMask(data, DATA_MACHINE_PROGRESSION_START);
    }

    public long unlockedPassiveNodeMaskHigh() {
        return MasteryMenuSupport.unlockedPassiveNodeMaskHigh(data, DATA_MACHINE_PROGRESSION_START);
    }

    @Override
    public boolean hasPassiveNode(PassiveNode node) {
        return node != null && node.isUnlocked(passiveProgressionView);
    }

    @Override
    public boolean canUnlockPassiveNode(MegaPassiveNode node) {
        return ForestryCompanionPassiveTree.TREE.canUnlock(node, passiveProgressionView);
    }

    @Override
    public boolean hasUnlockedPassiveConnection(PassiveNode node) {
        return node != null && node.parentUnlocked(passiveProgressionView);
    }

    private boolean hasPassiveNodeIndex(int index) {
        return MasteryMenuSupport.hasPassiveNodeIndex(data, DATA_MACHINE_PROGRESSION_START, index);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        MegaPassiveNode passiveNode = MegaPassiveTree.byButtonId(id);
        if (passiveNode != null) {
            if (player.level().isClientSide) {
                return true;
            }
            return cart.unlockPassiveNode(passiveNode);
        }
        if (player.level().isClientSide) {
            return id == BUTTON_TOGGLE_SCAN_DEBUG
                    || id == BUTTON_RELEASE_MANAGEMENT_VIEW
                    || (id == BUTTON_TOGGLE_MANUAL_SPEED && manualSpeedUnlocked())
                    || (id == BUTTON_RESET_MANAGED_CELLS && managedCells() > 0);
        }
        if (id == BUTTON_TOGGLE_SCAN_DEBUG) {
            cart.toggleScanDebugVisible();
            return true;
        }
        if (id == BUTTON_TOGGLE_MANUAL_SPEED) {
            return cart.toggleManualSpeedEnabled();
        }
        if (id == BUTTON_RELEASE_MANAGEMENT_VIEW) {
            releaseManagementView();
            return true;
        }
        if (id == BUTTON_RESET_MANAGED_CELLS) {
            return cart.resetManagedCells();
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return !cart.isRemoved() && player.distanceToSqr(cart) <= 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && managementViewer) {
            releaseManagementView();
        }
    }

    private void releaseManagementView() {
        if (managementViewer) {
            managementViewer = false;
            cart.closeManagementMenu();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return moved;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (cart.isPlantableStack(stack) || ForestryCartEntity.isFertilizerStack(stack)) {
            if (!moveItemStackTo(
                    stack,
                    CART_CARGO_SLOT_START,
                    CART_CARGO_SLOT_START + ForestryCartEntity.SAPLING_SLOT_COUNT,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (ForestryCartEntity.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, CART_BATTERY_SLOT, CART_BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ForestryCartEntity.isToolCandidate(stack)) {
            if (!moveItemStackTo(stack, CART_TOOL_SLOT, CART_TOOL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ForestryCartEntity.isShearsCandidate(stack)) {
            if (!moveItemStackTo(stack, CART_SHEARS_SLOT, CART_SHEARS_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ForestryCartEntity.isPumpCandidate(stack)) {
            if (!moveItemStackTo(stack, CART_PUMP_SLOT, CART_PUMP_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof EnergyConnectorItem
                || stack.getItem() instanceof ItemConnectorItem
                || stack.getItem() instanceof FluidConnectorItem) {
            return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return moved;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new TabbedInventorySlot(
                        playerInventory,
                        column + row * 9 + 9,
                        39 + column * 18,
                        116 + row * 18,
                        () -> selectedTab == TAB_CART
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(
                    playerInventory,
                    column,
                    39 + column * 18,
                    174,
                    () -> selectedTab == TAB_CART
            ));
        }
    }

    private static ForestryCartEntity cartEntity(Inventory playerInventory, int entityId) {
        Entity entity = playerInventory.player.level().getEntity(entityId);
        if (entity instanceof ForestryCartEntity cart) {
            return cart;
        }
        throw new IllegalStateException("Expected forestry cart entity with id " + entityId);
    }

    private static final class TabbedSlot extends SlotItemHandler {
        private final BooleanSupplier activeSupplier;

        private TabbedSlot(
                net.neoforged.neoforge.items.IItemHandler itemHandler,
                int index,
                int xPosition,
                int yPosition,
                BooleanSupplier activeSupplier
        ) {
            super(itemHandler, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }

    private static final class TabbedInventorySlot extends Slot {
        private final BooleanSupplier activeSupplier;

        private TabbedInventorySlot(Inventory inventory, int index, int xPosition, int yPosition, BooleanSupplier activeSupplier) {
            super(inventory, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }
    @Override public MachineMasteryHost masteryHost() { return cart; }
    @Override public double masteryAttribute(MachineStat stat) { return MasteryMenuSupport.attribute(data, DATA_MACHINE_PROGRESSION_START, stat); }
    @Override public MachineMasteryFamily masteryFamily() { return MachineMasteryFamily.FORESTRY; }
    @Override public MachineProgressionState masterySnapshot() {
        return MasteryMenuSupport.snapshot(data, DATA_MACHINE_PROGRESSION_START, masteryFamily());
    }
    @Override public int ascendancyEntryStage() {
        return MasteryMenuSupport.ascendancyEntryStage(data, DATA_MACHINE_PROGRESSION_START);
    }
    @Override public List<MasteryMenuSupport.GrantedStat> ascendancyStats() {
        return MasteryMenuSupport.grantedStats(data, DATA_MACHINE_PROGRESSION_START);
    }

}
