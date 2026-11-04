package com.rngtech.content.menu;

import com.rngtech.content.affixsorter.SorterRules;
import com.rngtech.content.affixsorter.SorterRulesHost;
import com.rngtech.content.block.AffixSorterBlock;
import com.rngtech.content.blockentity.AffixSorterBlockEntity;
import com.rngtech.content.jewel.JewelSockets;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class AffixSorterMenu extends AbstractContainerMenu implements StatBreakdownMenu, SorterRulesHost, JewelMenuView {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;
    public static final int TAB_RULES = 4;
    public static final int TAB_JEWELS = 5;
    /** Clicking a face button cycles that face between input, Pass, and Reject. */
    public static final int BUTTON_FACE_BASE = 8000;

    private static final int DATA_COUNT = AffixSorterBlockEntity.DATA_COUNT;
    private static final int STAT_SCALE = 100;
    private static final int PROCESS_SLOT_COUNT = AffixSorterBlockEntity.PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_START = PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_END = GEAR_SLOT_START + AffixSorterBlockEntity.GEAR_SLOT_COUNT;
    private static final int REFINEMENT_CONSUMABLE_SLOT = GEAR_SLOT_END;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = REFINEMENT_TARGET_SLOT + 1;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final AffixSorterBlockEntity sorter;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private SorterRules rules;
    private int selectedTab = TAB_PROCESSING;

    public AffixSorterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData),
                SorterRules.STREAM_CODEC.decode(extraData)
        );
    }

    public AffixSorterMenu(int containerId, Inventory playerInventory, AffixSorterBlockEntity sorter, ContainerData data) {
        this(containerId, playerInventory, sorter, data, sorter.machineTraits(), sorter.sorterRules());
    }

    private AffixSorterMenu(
            int containerId,
            Inventory playerInventory,
            AffixSorterBlockEntity sorter,
            ContainerData data,
            MachineTraits machineTraits,
            SorterRules rules
    ) {
        super(ModMenus.AFFIX_SORTER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(sorter.getLevel(), sorter.getBlockPos());
        this.data = data;
        this.sorter = sorter;
        this.rules = rules;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, sorter.getBlockState().getBlock(), machineTraits);

        ItemStackHandler processInventory = sorter.getProcessInventory();
        addSlot(new TabbedSlot(processInventory, AffixSorterBlockEntity.SLOT_INPUT, 45, 52, () -> selectedTab == TAB_PROCESSING));
        for (int slot = 0; slot < AffixSorterBlockEntity.BUFFER_SLOTS; slot++) {
            addSlot(new TabbedSlot(processInventory, AffixSorterBlockEntity.SLOT_PASS_START + slot, 141 + slot * 18, 30,
                    () -> selectedTab == TAB_PROCESSING));
        }
        for (int slot = 0; slot < AffixSorterBlockEntity.BUFFER_SLOTS; slot++) {
            addSlot(new TabbedSlot(processInventory, AffixSorterBlockEntity.SLOT_REJECT_START + slot, 141 + slot * 18, 74,
                    () -> selectedTab == TAB_PROCESSING));
        }

        ItemStackHandler gearInventory = sorter.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, AffixSorterBlockEntity.SLOT_BATTERY_CELL, 56, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, AffixSorterBlockEntity.SLOT_RULE_CORE, 92, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, AffixSorterBlockEntity.SLOT_SERVO, 128, 48, () -> selectedTab == TAB_GEAR));

        addSlot(RefinementMenuSupport.consumableSlot(
                sorter.getRefinementInventory(),
                0,
                RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X,
                RefinementMenuSupport.REFINEMENT_SLOT_Y,
                () -> selectedTab == TAB_REFINEMENT
        ));
        addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, RefinementMenuSupport.REFINEMENT_TARGET_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));

        addPlayerInventory(playerInventory);
        IItemHandler sockets = playerInventory.player.level().isClientSide ? JewelSockets.client() : new JewelSockets(sorter);
        for (int index = 0; index < JewelSockets.SLOTS; index++) {
            int slot = index;
            addSlot(new JewelSocketSlot(sockets, slot, () -> jewelsTabSelected() && jewelSlotOpen(slot)));
        }
        addDataSlots(data);
    }

    private static final int JEWEL_SLOT_START = HOTBAR_END;

    @Override public ItemStack jewel(int index) {
        return index >= 0 && index < JewelSockets.SLOTS ? getSlot(JEWEL_SLOT_START + index).getItem() : ItemStack.EMPTY;
    }
    @Override public int jewelTreeCapacity() { return 0; }
    @Override public int jewelBodyCapacity() { return JewelSockets.bodySockets(machineTraits()); }
    @Override public boolean jewelsTabSelected() { return selectedTab == TAB_JEWELS; }
    private boolean jewelSlotOpen(int index) {
        return !JewelSocketSlot.isTreeSlot(index) && index - JewelSockets.BODY_START < jewelBodyCapacity();
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_STATS -> TAB_STATS;
            case TAB_REFINEMENT -> TAB_REFINEMENT;
            case TAB_RULES -> TAB_RULES;
            case TAB_JEWELS -> TAB_JEWELS;
            default -> TAB_PROCESSING;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    @Override
    public SorterRules sorterRules() {
        return rules;
    }

    /** The screen calls this with the edited list before it sends the list to the server, which clamps it the same way. */
    @Override
    public void applySorterRules(SorterRules next) {
        if (sorter.getLevel() != null && !sorter.getLevel().isClientSide) {
            sorter.setSorterRules(next);
            rules = sorter.sorterRules();
        } else {
            rules = next.limitedTo(ruleCapacity());
        }
    }

    public float processingProgress() {
        int ticks = data.get(AffixSorterBlockEntity.DATA_PROCESSING_TICKS);
        return ticks <= 0 ? 0.0F : Mth.clamp((float) Math.max(0, data.get(AffixSorterBlockEntity.DATA_PROGRESS)) / ticks, 0.0F, 1.0F);
    }

    public float energyProgress() {
        int capacity = energyCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp((float) energy() / (float) capacity, 0.0F, 1.0F);
    }

    public int energy() {
        return data.get(AffixSorterBlockEntity.DATA_ENERGY);
    }

    public int energyCapacity() {
        return data.get(AffixSorterBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public int energyPerTick() {
        return data.get(AffixSorterBlockEntity.DATA_ENERGY_PER_TICK);
    }

    public int statusCode() {
        return data.get(AffixSorterBlockEntity.DATA_STATUS);
    }

    public int ruleCapacity() {
        return data.get(AffixSorterBlockEntity.DATA_RULE_CAPACITY);
    }

    public int enabledRules() {
        return data.get(AffixSorterBlockEntity.DATA_ENABLED_RULES);
    }

    public int fePerItem() {
        return data.get(AffixSorterBlockEntity.DATA_FE_PER_ITEM);
    }

    /** The rule that routed the last item, {@code -1} for the fallback, or {@code -2} before any item. */
    public int lastRule() {
        return data.get(AffixSorterBlockEntity.DATA_LAST_RULE);
    }

    public int lastRoute() {
        return data.get(AffixSorterBlockEntity.DATA_LAST_ROUTE);
    }

    public AffixSorterBlockEntity.FaceRoute faceRoute(AffixSorterBlockEntity.RelativeFace face) {
        int value = data.get(AffixSorterBlockEntity.DATA_FACE_START + face.ordinal());
        return AffixSorterBlockEntity.FaceRoute.values()[Math.floorMod(value, AffixSorterBlockEntity.FaceRoute.values().length)];
    }

    public double statValue(int dataIndex) {
        return data.get(dataIndex) / (double) STAT_SCALE;
    }

    public MachineTraits machineTraits() {
        return RefinementMenuSupport.displayTraits(getSlot(REFINEMENT_TARGET_SLOT).getItem());
    }

    public int refinementConsumableSlot() {
        return REFINEMENT_CONSUMABLE_SLOT;
    }

    public int refinementTargetSlot() {
        return REFINEMENT_TARGET_SLOT;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= WorkOrderMenuSupport.BUTTON_SELECT_TAB && id <= WorkOrderMenuSupport.BUTTON_SELECT_TAB + TAB_JEWELS) {
            selectTab(id - WorkOrderMenuSupport.BUTTON_SELECT_TAB);
            return true;
        }
        int face = id - BUTTON_FACE_BASE;
        if (face >= 0 && face < AffixSorterBlockEntity.RelativeFace.values().length) {
            if (!player.level().isClientSide) {
                sorter.cycleFace(face);
            }
            return true;
        }
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return RefinementMenuSupport.applyToMachine(player, sorter, refinementTarget, sorter.getBlockState().getBlock());
    }

    @Override
    public MachineStatAccumulator breakdownStats() {
        return sorter.effectiveStats();
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof AffixSorterBlock
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
                true
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();

        if (index >= HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (selectedTab == TAB_JEWELS && index >= PLAYER_INVENTORY_START && JewelSockets.accepts(stack) && jewelBodyCapacity() > 0) {
            int first = JEWEL_SLOT_START + JewelSockets.BODY_START;
            if (!moveItemStackTo(stack, first, first + jewelBodyCapacity(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, index != AffixSorterBlockEntity.SLOT_INPUT)) {
                return ItemStack.EMPTY;
            }
        } else if (RefinementMenuSupport.isConsumable(stack)) {
            if (!moveItemStackTo(stack, REFINEMENT_CONSUMABLE_SLOT, REFINEMENT_CONSUMABLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (selectedTab == TAB_GEAR && AffixSorterBlockEntity.isBatteryCell(stack)) {
            if (!moveToGear(stack, AffixSorterBlockEntity.SLOT_BATTERY_CELL)) {
                return ItemStack.EMPTY;
            }
        } else if (selectedTab == TAB_GEAR && AffixSorterBlockEntity.isRuleCore(stack)) {
            if (!moveToGear(stack, AffixSorterBlockEntity.SLOT_RULE_CORE)) {
                return ItemStack.EMPTY;
            }
        } else if (selectedTab == TAB_GEAR && AffixSorterBlockEntity.isServo(stack)) {
            if (!moveToGear(stack, AffixSorterBlockEntity.SLOT_SERVO)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, AffixSorterBlockEntity.SLOT_INPUT, AffixSorterBlockEntity.SLOT_INPUT + 1, false)) {
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

    private boolean moveToGear(ItemStack stack, int gearSlot) {
        return moveItemStackTo(stack, GEAR_SLOT_START + gearSlot, GEAR_SLOT_START + gearSlot + 1, false);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new TabbedInventorySlot(playerInventory, column + row * 9 + 9, 39 + column * 18, 116 + row * 18,
                        () -> selectedTab != TAB_STATS && selectedTab != TAB_RULES));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(playerInventory, column, 39 + column * 18, 174,
                    () -> selectedTab != TAB_STATS && selectedTab != TAB_RULES));
        }
    }

    private static AffixSorterBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof AffixSorterBlockEntity sorter) {
            return sorter;
        }
        throw new IllegalStateException("Expected affix sorter block entity at " + pos);
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
