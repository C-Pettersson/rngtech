package com.rngtech.content.menu;

import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.MinersCompanionItem;
import com.rngtech.content.minerscompanion.MinersCompanionState;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class MinersCompanionMenu extends AbstractContainerMenu {
    public static final int TAB_PROCESS = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int BUTTON_TAB_BASE = 100;
    public static final int BUTTON_TOGGLE_BLOCK_CHEW = 200;
    public static final int BUTTON_TOGGLE_MAGNET = 201;
    public static final int BUTTON_TOGGLE_MINING_LAMP = 202;
    public static final int SLOT_CRUSH_HEAD = 0;
    public static final int SLOT_BATTERY_CELL = 1;
    public static final int SLOT_RECOVERY_FILTER = 2;
    public static final int SLOT_MAGNET = 3;
    public static final int SLOT_MINING_LAMP = 4;
    public static final int STATUS_READY = 0;
    public static final int STATUS_DISABLED = 1;
    public static final int STATUS_MISSING_HEAD = 2;
    public static final int STATUS_MISSING_CELL = 3;
    public static final int STATUS_NO_FILTERS = 4;
    public static final int STATUS_NO_POWER = 5;
    private static final int GEAR_SLOT_COUNT = 5;
    private static final int FILTER_SLOT_COUNT = MinersCompanionState.FILTER_SLOT_COUNT;
    private static final int FILTER_MENU_SLOT_START = GEAR_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = FILTER_MENU_SLOT_START + FILTER_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Inventory inventory;
    private final InteractionHand hand;
    private final ItemStackHandler gearInventory;
    private final ItemStackHandler filterInventory;
    private boolean loading;
    private int selectedTab = TAB_PROCESS;

    public MinersCompanionMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    public MinersCompanionMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenus.MINERS_COMPANION.get(), containerId);
        this.inventory = playerInventory;
        this.hand = hand;
        this.gearInventory = createGearInventory();
        this.filterInventory = createFilterInventory();
        loadFromStack();

        addSlot(new TabbedSlot(gearInventory, SLOT_CRUSH_HEAD, 8, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, SLOT_BATTERY_CELL, 42, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, SLOT_RECOVERY_FILTER, 76, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, SLOT_MAGNET, 110, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, SLOT_MINING_LAMP, 144, 48, () -> selectedTab == TAB_GEAR));
        for (int index = 0; index < FILTER_SLOT_COUNT; index++) {
            int slotIndex = index;
            int row = index / 5;
            int column = index % 5;
            addSlot(new GhostFilterSlot(
                    filterInventory,
                    index,
                    41 + column * 18,
                    35 + row * 18,
                    index,
                    () -> selectedTab == TAB_PROCESS && slotIndex < activeFilterSlots()
            ));
        }
        addPlayerInventory(playerInventory);
    }

    public InteractionHand hand() {
        return hand;
    }

    public int selectedTab() {
        return selectedTab;
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_STATS -> TAB_STATS;
            default -> TAB_PROCESS;
        };
    }

    public ItemStack minersCompanionStack() {
        return inventory.player.getItemInHand(hand);
    }

    public MinersCompanionState state() {
        return MinersCompanionItem.state(minersCompanionStack());
    }

    public boolean enabled() {
        return blockChewEnabled();
    }

    public boolean blockChewEnabled() {
        return state().blockChewEnabled();
    }

    public boolean magnetEnabled() {
        return state().magnetEnabled();
    }

    public boolean miningLampEnabled() {
        return state().miningLampEnabled();
    }

    public boolean canToggleBlockChew() {
        return MinersCompanionState.validCrushHead(gearInventory.getStackInSlot(SLOT_CRUSH_HEAD))
                && BatteryCellItem.isBatteryCell(gearInventory.getStackInSlot(SLOT_BATTERY_CELL));
    }

    public boolean canToggleMagnet() {
        return MinersCompanionState.validMagnet(gearInventory.getStackInSlot(SLOT_MAGNET))
                && BatteryCellItem.isBatteryCell(gearInventory.getStackInSlot(SLOT_BATTERY_CELL));
    }

    public boolean canToggleMiningLamp() {
        return MinersCompanionState.validMiningLamp(gearInventory.getStackInSlot(SLOT_MINING_LAMP))
                && BatteryCellItem.isBatteryCell(gearInventory.getStackInSlot(SLOT_BATTERY_CELL));
    }

    public int activeFilterSlots() {
        return MinersCompanionItem.activeFilterSlots(minersCompanionStack());
    }

    public int energyCostPerItem() {
        return MinersCompanionItem.energyCostPerItem(minersCompanionStack());
    }

    public int magnetPassiveCost() {
        return MinersCompanionItem.MAGNET_PASSIVE_FE_PER_TICK;
    }

    public int magnetItemCost() {
        return MinersCompanionItem.MAGNET_FE_PER_ITEM_ENTITY;
    }

    public int miningLampCost() {
        return MinersCompanionItem.MINING_LAMP_FE_PER_TICK;
    }

    public int storedEnergy() {
        return MinersCompanionItem.storedEnergy(minersCompanionStack());
    }

    public int energyCapacity() {
        return MinersCompanionItem.energyCapacity(minersCompanionStack());
    }

    public int statusCode() {
        MinersCompanionState currentState = state();
        if (!MinersCompanionState.validCrushHead(currentState.crushHead())) {
            return STATUS_MISSING_HEAD;
        }
        if (!BatteryCellItem.isBatteryCell(currentState.batteryCell())) {
            return STATUS_MISSING_CELL;
        }
        if (!currentState.blockChewEnabled()) {
            return STATUS_DISABLED;
        }
        if (!hasActiveFilter(currentState)) {
            return STATUS_NO_FILTERS;
        }
        int cost = energyCostPerItem();
        if (cost > 0 && storedEnergy() < cost) {
            return STATUS_NO_POWER;
        }
        return STATUS_READY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int tab = id - BUTTON_TAB_BASE;
        if (tab >= TAB_PROCESS && tab <= TAB_STATS) {
            selectTab(tab);
            return true;
        }
        if (id == BUTTON_TOGGLE_BLOCK_CHEW && holdsMinersCompanion(player) && canToggleBlockChew()) {
            MinersCompanionItem.setState(minersCompanionStack(), state().withBlockChewEnabled(!blockChewEnabled()));
            syncHeldStack(player);
            return true;
        }
        if (id == BUTTON_TOGGLE_MAGNET && holdsMinersCompanion(player) && canToggleMagnet()) {
            MinersCompanionItem.setState(minersCompanionStack(), state().withMagnetEnabled(!magnetEnabled()));
            syncHeldStack(player);
            return true;
        }
        if (id == BUTTON_TOGGLE_MINING_LAMP && holdsMinersCompanion(player) && canToggleMiningLamp()) {
            MinersCompanionItem.setState(minersCompanionStack(), state().withMiningLampEnabled(!miningLampEnabled()));
            syncHeldStack(player);
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof GhostFilterSlot filterSlot) {
            handleGhostFilterClick(filterSlot, button, clickType);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void handleGhostFilterClick(GhostFilterSlot slot, int button, ClickType clickType) {
        if (clickType != ClickType.PICKUP || !slot.isActive()) {
            return;
        }
        ItemStack carried = getCarried();
        if (button == 1 || carried.isEmpty()) {
            slot.clearFilter();
            return;
        }
        slot.setFilter(carried);
    }

    @Override
    public boolean stillValid(Player player) {
        return holdsMinersCompanion(player);
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

        if (slot instanceof GhostFilterSlot || isHeldMinersCompanionPlayerSlot(index)) {
            return ItemStack.EMPTY;
        }

        if (index < FILTER_MENU_SLOT_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_START) {
            return ItemStack.EMPTY;
        } else if (MinersCompanionState.validCrushHead(stack)) {
            if (!moveItemStackTo(stack, SLOT_CRUSH_HEAD, SLOT_CRUSH_HEAD + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BatteryCellItem.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, SLOT_BATTERY_CELL, SLOT_BATTERY_CELL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MinersCompanionState.validRecoveryFilter(stack)) {
            if (!moveItemStackTo(stack, SLOT_RECOVERY_FILTER, SLOT_RECOVERY_FILTER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MinersCompanionState.validMagnet(stack)) {
            if (!moveItemStackTo(stack, SLOT_MAGNET, SLOT_MAGNET + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MinersCompanionState.validMiningLamp(stack)) {
            if (!moveItemStackTo(stack, SLOT_MINING_LAMP, SLOT_MINING_LAMP + 1, false)) {
                return ItemStack.EMPTY;
            }
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

    private ItemStackHandler createGearInventory() {
        return new ItemStackHandler(GEAR_SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return switch (slot) {
                    case SLOT_CRUSH_HEAD -> MinersCompanionState.validCrushHead(stack);
                    case SLOT_BATTERY_CELL -> BatteryCellItem.isBatteryCell(stack);
                    case SLOT_RECOVERY_FILTER -> MinersCompanionState.validRecoveryFilter(stack);
                    case SLOT_MAGNET -> MinersCompanionState.validMagnet(stack);
                    case SLOT_MINING_LAMP -> MinersCompanionState.validMiningLamp(stack);
                    default -> false;
                };
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                saveToStack();
            }
        };
    }

    private ItemStackHandler createFilterInventory() {
        return new ItemStackHandler(FILTER_SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return MinersCompanionState.validFilterStack(stack);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                saveToStack();
            }
        };
    }

    private void loadFromStack() {
        loading = true;
        try {
            MinersCompanionState state = state();
            gearInventory.setStackInSlot(SLOT_CRUSH_HEAD, state.crushHead());
            gearInventory.setStackInSlot(SLOT_BATTERY_CELL, state.batteryCell());
            gearInventory.setStackInSlot(SLOT_RECOVERY_FILTER, state.recoveryFilter());
            gearInventory.setStackInSlot(SLOT_MAGNET, state.magnet());
            gearInventory.setStackInSlot(SLOT_MINING_LAMP, state.miningLamp());
            for (int index = 0; index < FILTER_SLOT_COUNT; index++) {
                filterInventory.setStackInSlot(index, state.filter(index));
            }
        } finally {
            loading = false;
        }
    }

    private void saveToStack() {
        if (loading || inventory.player.level().isClientSide || !holdsMinersCompanion(inventory.player)) {
            return;
        }
        MinersCompanionState current = state();
        MinersCompanionState updated = new MinersCompanionState(
                current.blockChewEnabled(),
                current.magnetEnabled(),
                current.miningLampEnabled(),
                gearInventory.getStackInSlot(SLOT_CRUSH_HEAD),
                gearInventory.getStackInSlot(SLOT_BATTERY_CELL),
                gearInventory.getStackInSlot(SLOT_RECOVERY_FILTER),
                gearInventory.getStackInSlot(SLOT_MAGNET),
                gearInventory.getStackInSlot(SLOT_MINING_LAMP),
                filtersFromHandler()
        );
        minersCompanionStack().set(ModDataComponents.MINERS_COMPANION_STATE.get(), updated);
        syncHeldStack(inventory.player);
    }

    private java.util.List<ItemStack> filtersFromHandler() {
        java.util.List<ItemStack> filters = new java.util.ArrayList<>(FILTER_SLOT_COUNT);
        for (int index = 0; index < FILTER_SLOT_COUNT; index++) {
            filters.add(filterInventory.getStackInSlot(index));
        }
        return filters;
    }

    private void setFilter(int index, ItemStack stack) {
        filterInventory.setStackInSlot(index, MinersCompanionState.normalizeFilterStack(stack));
    }

    private boolean hasActiveFilter(MinersCompanionState state) {
        for (int index = 0; index < activeFilterSlots(); index++) {
            if (!state.filter(index).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean holdsMinersCompanion(Player player) {
        return player.getItemInHand(hand).is(ModItems.MINERS_COMPANION.get());
    }

    private boolean isHeldMinersCompanionPlayerSlot(int menuIndex) {
        if (hand == InteractionHand.OFF_HAND || menuIndex < PLAYER_INVENTORY_START) {
            return false;
        }
        int hotbarIndex = menuIndex - PLAYER_INVENTORY_END;
        return hotbarIndex >= 0 && hotbarIndex < 9 && hotbarIndex == inventory.selected;
    }

    private void syncHeldStack(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        int slot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : serverPlayer.getInventory().selected;
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                -2,
                0,
                slot,
                serverPlayer.getItemInHand(hand).copy()
        ));
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 116 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 174));
        }
    }

    private static final class TabbedSlot extends SlotItemHandler {
        private final BooleanSupplier activeSupplier;

        private TabbedSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition, BooleanSupplier activeSupplier) {
            super(itemHandler, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }

    private final class GhostFilterSlot extends SlotItemHandler {
        private final int filterIndex;
        private final BooleanSupplier activeSupplier;

        private GhostFilterSlot(
                ItemStackHandler itemHandler,
                int index,
                int xPosition,
                int yPosition,
                int filterIndex,
                BooleanSupplier activeSupplier
        ) {
            super(itemHandler, index, xPosition, yPosition);
            this.filterIndex = filterIndex;
            this.activeSupplier = activeSupplier;
        }

        private void setFilter(ItemStack stack) {
            MinersCompanionMenu.this.setFilter(filterIndex, stack);
            setChanged();
        }

        private void clearFilter() {
            MinersCompanionMenu.this.setFilter(filterIndex, ItemStack.EMPTY);
            setChanged();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }
}
