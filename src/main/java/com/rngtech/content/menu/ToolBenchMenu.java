package com.rngtech.content.menu;

import com.rngtech.content.blockentity.ToolBenchBlockEntity;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.item.ToolHeadItem;
import com.rngtech.content.item.ToolRodItem;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class ToolBenchMenu extends AbstractContainerMenu {
    public static final int BUTTON_APPLY = 0;
    public static final int BUTTON_REMOVE_BATTERY = 1;
    public static final int BUTTON_DISASSEMBLE = 2;
    public static final int BUTTON_REFINE = 3;
    public static final int BUTTON_SELECT_HEAD = 4;
    public static final int BUTTON_SELECT_ROD = 5;
    public static final int BUTTON_TAB_BASE = 100;
    public static final int TAB_ASSEMBLY = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;

    private static final int BENCH_STORAGE_SLOT_COUNT = ToolBenchBlockEntity.SLOT_COUNT;
    private static final int REFINEMENT_ENTRY_SLOT_COUNT = 3;
    private static final int BENCH_MENU_SLOT_COUNT = BENCH_STORAGE_SLOT_COUNT + REFINEMENT_ENTRY_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = BENCH_MENU_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;
    private static final int PLAYER_INVENTORY_X = 132;
    private static final int PLAYER_INVENTORY_Y = 143;
    private static final int HOTBAR_Y = 201;

    private final ContainerLevelAccess access;
    private final ToolBenchBlockEntity bench;
    private int selectedTab = TAB_ASSEMBLY;
    private boolean selectedHeadTarget = true;

    public ToolBenchMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, blockEntity(playerInventory, extraData.readBlockPos()));
    }

    public ToolBenchMenu(int containerId, Inventory playerInventory, ToolBenchBlockEntity bench) {
        super(ModMenus.TOOL_BENCH.get(), containerId);
        this.bench = bench;
        access = ContainerLevelAccess.create(bench.getLevel(), bench.getBlockPos());

        ItemStackHandler inventory = bench.getInventory();
        addSlot(new TabbedSlot(inventory, ToolBenchBlockEntity.SLOT_TOOL, 25, 43, () -> selectedTab == TAB_ASSEMBLY));
        addSlot(new TabbedSlot(inventory, ToolBenchBlockEntity.SLOT_HEAD, 72, 30, () -> selectedTab == TAB_ASSEMBLY));
        addSlot(new TabbedSlot(inventory, ToolBenchBlockEntity.SLOT_ROD, 72, 56, () -> selectedTab == TAB_ASSEMBLY));
        addSlot(new TabbedSlot(inventory, ToolBenchBlockEntity.SLOT_BATTERY_CELL, 119, 43, () -> selectedTab == TAB_ASSEMBLY));
        addSlot(new TabbedSlot(inventory, ToolBenchBlockEntity.SLOT_TINY_ANVIL, 83, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(inventory, ToolBenchBlockEntity.SLOT_REPAIR_MATERIAL, 119, 69, () -> selectedTab == TAB_ASSEMBLY));
        addSlot(new TabbedSlot(inventory, ToolBenchBlockEntity.SLOT_REFINEMENT_CONSUMABLE, 101, 32, () -> selectedTab == TAB_REFINEMENT));
        addSlot(new TabbedSlot(inventory, ToolBenchBlockEntity.SLOT_ENERGY_CONNECTOR, 119, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(inventory, ToolBenchBlockEntity.SLOT_TOOL, 14, 32, () -> selectedTab == TAB_REFINEMENT));
        addSlot(new TabbedSlot(
                inventory,
                ToolBenchBlockEntity.SLOT_HEAD,
                43,
                32,
                () -> selectedTab == TAB_REFINEMENT && !hasValidToolForRefinement()
        ));
        addSlot(new TabbedSlot(
                inventory,
                ToolBenchBlockEntity.SLOT_ROD,
                72,
                32,
                () -> selectedTab == TAB_REFINEMENT && !hasValidToolForRefinement()
        ));
        addPlayerInventory(playerInventory);
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_STATS -> TAB_STATS;
            case TAB_REFINEMENT -> TAB_REFINEMENT;
            default -> TAB_ASSEMBLY;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    public ItemStack toolStack() {
        return bench.getInventory().getStackInSlot(ToolBenchBlockEntity.SLOT_TOOL);
    }

    public boolean hasValidToolForRefinement() {
        ItemStack tool = toolStack();
        return tool.getItem() instanceof ModularToolItem toolItem
                && ModularToolItem.assembly(tool).isValidFor(toolItem.family());
    }

    public boolean selectedHeadTarget() {
        return selectedHeadTarget;
    }

    public void selectRefinementTarget(boolean headTarget) {
        selectedHeadTarget = headTarget;
    }

    public ItemStack selectedRefinementTarget() {
        return bench.refinementTarget(selectedHeadTarget);
    }

    public ItemStack headRefinementTarget() {
        return bench.refinementTarget(true);
    }

    public ItemStack rodRefinementTarget() {
        return bench.refinementTarget(false);
    }

    public ItemStack refinementConsumableStack() {
        return bench.getInventory().getStackInSlot(ToolBenchBlockEntity.SLOT_REFINEMENT_CONSUMABLE);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_TAB_BASE && id <= BUTTON_TAB_BASE + TAB_REFINEMENT) {
            selectTab(id - BUTTON_TAB_BASE);
            return true;
        }
        if (id == BUTTON_SELECT_HEAD || id == BUTTON_SELECT_ROD) {
            selectRefinementTarget(id == BUTTON_SELECT_HEAD);
            return true;
        }
        if (player.level().isClientSide) {
            return id == BUTTON_APPLY
                    || id == BUTTON_REMOVE_BATTERY
                    || id == BUTTON_DISASSEMBLE
                    || id == BUTTON_REFINE;
        }
        if (id == BUTTON_REFINE) {
            return bench.applyRefinement(player, selectedHeadTarget);
        }
        String message = switch (id) {
            case BUTTON_APPLY -> bench.apply(player);
            case BUTTON_REMOVE_BATTERY -> bench.removeBattery(player);
            case BUTTON_DISASSEMBLE -> bench.disassemble(player);
            default -> "";
        };
        if (message.isEmpty()) {
            return false;
        }
        player.displayClientMessage(Component.translatable(message), true);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.TOOL_BENCH.get());
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

        if (index < BENCH_MENU_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ToolBenchBlockEntity.acceptsToolSlot(stack)) {
            if (!canQuickMoveToBenchSlot(ToolBenchBlockEntity.SLOT_TOOL)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, ToolBenchBlockEntity.SLOT_TOOL, ToolBenchBlockEntity.SLOT_TOOL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ToolHeadItem) {
            if (!canQuickMoveToBenchSlot(ToolBenchBlockEntity.SLOT_HEAD)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, ToolBenchBlockEntity.SLOT_HEAD, ToolBenchBlockEntity.SLOT_HEAD + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof ToolRodItem) {
            if (!canQuickMoveToBenchSlot(ToolBenchBlockEntity.SLOT_ROD)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, ToolBenchBlockEntity.SLOT_ROD, ToolBenchBlockEntity.SLOT_ROD + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BatteryCellItem.isBatteryCell(stack)) {
            if (!canQuickMoveToBenchSlot(ToolBenchBlockEntity.SLOT_BATTERY_CELL)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, ToolBenchBlockEntity.SLOT_BATTERY_CELL, ToolBenchBlockEntity.SLOT_BATTERY_CELL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ToolBenchBlockEntity.isRefinementConsumable(stack)) {
            if (!canQuickMoveToBenchSlot(ToolBenchBlockEntity.SLOT_REFINEMENT_CONSUMABLE)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, ToolBenchBlockEntity.SLOT_REFINEMENT_CONSUMABLE, ToolBenchBlockEntity.SLOT_REFINEMENT_CONSUMABLE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof EnergyConnectorItem) {
            if (!canQuickMoveToBenchSlot(ToolBenchBlockEntity.SLOT_ENERGY_CONNECTOR)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, ToolBenchBlockEntity.SLOT_ENERGY_CONNECTOR, ToolBenchBlockEntity.SLOT_ENERGY_CONNECTOR + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.TINY_ANVIL.get())) {
            if (!canQuickMoveToBenchSlot(ToolBenchBlockEntity.SLOT_TINY_ANVIL)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, ToolBenchBlockEntity.SLOT_TINY_ANVIL, ToolBenchBlockEntity.SLOT_TINY_ANVIL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ModularToolItem.isKnownRepairMaterial(stack) || stack.is(ModItems.DIAMOND_TIP.get())) {
            if (!canQuickMoveToBenchSlot(ToolBenchBlockEntity.SLOT_REPAIR_MATERIAL)) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(stack, ToolBenchBlockEntity.SLOT_REPAIR_MATERIAL, ToolBenchBlockEntity.SLOT_REPAIR_MATERIAL + 1, false)) {
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

    private boolean canQuickMoveToBenchSlot(int slot) {
        return switch (slot) {
            case ToolBenchBlockEntity.SLOT_TOOL -> selectedTab == TAB_ASSEMBLY || selectedTab == TAB_REFINEMENT;
            case ToolBenchBlockEntity.SLOT_HEAD, ToolBenchBlockEntity.SLOT_ROD ->
                selectedTab == TAB_ASSEMBLY || selectedTab == TAB_REFINEMENT && !hasValidToolForRefinement();
            case ToolBenchBlockEntity.SLOT_BATTERY_CELL, ToolBenchBlockEntity.SLOT_REPAIR_MATERIAL ->
                selectedTab == TAB_ASSEMBLY;
            case ToolBenchBlockEntity.SLOT_TINY_ANVIL -> selectedTab == TAB_GEAR;
            case ToolBenchBlockEntity.SLOT_ENERGY_CONNECTOR -> selectedTab == TAB_GEAR;
            case ToolBenchBlockEntity.SLOT_REFINEMENT_CONSUMABLE -> selectedTab == TAB_REFINEMENT;
            default -> false;
        };
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new TabbedInventorySlot(
                        playerInventory,
                        column + row * 9 + 9,
                        PLAYER_INVENTORY_X + column * 18,
                        PLAYER_INVENTORY_Y + row * 18,
                        () -> selectedTab != TAB_STATS
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(
                    playerInventory,
                    column,
                    PLAYER_INVENTORY_X + column * 18,
                    HOTBAR_Y,
                    () -> selectedTab != TAB_STATS
            ));
        }
    }

    private static ToolBenchBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof ToolBenchBlockEntity bench) {
            return bench;
        }
        throw new IllegalStateException("Expected tool bench block entity at " + pos);
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
}
