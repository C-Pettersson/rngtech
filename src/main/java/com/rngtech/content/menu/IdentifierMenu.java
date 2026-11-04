package com.rngtech.content.menu;

import com.rngtech.content.block.IdentifierBlock;
import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.IdentifierBlockEntity;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.jewel.JewelSockets;
import com.rngtech.content.purge.FluidPurgeSupport;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.content.workorder.DriveBays;
import com.rngtech.content.workorder.WorkOrderDiscovery;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveNode;
import com.rngtech.rpg.progression.PassiveProgressionView;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkOrderFamilies;
import com.rngtech.rpg.workorder.WorkOrderHost;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;
import java.util.function.BooleanSupplier;

public class IdentifierMenu extends AbstractContainerMenu implements MasteryMenuView<MegaPassiveNode>, StatBreakdownMenu, WorkOrderMenuView, JewelMenuView {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;
    public static final int TAB_MASTERY = 4;
    public static final int TAB_ORDERS = 5;
    public static final int TAB_JEWELS = 6;

    private static final int DATA_COUNT = IdentifierBlockEntity.DATA_COUNT;
    private static final int PROGRESSION_START = IdentifierBlockEntity.DATA_MACHINE_PROGRESSION_START;
    private static final int STAT_SCALE = 100;
    private static final int PROCESS_SLOT_COUNT = IdentifierBlockEntity.PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_START = PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_END = GEAR_SLOT_START + IdentifierBlockEntity.GEAR_SLOT_COUNT;
    private static final int REFINEMENT_CONSUMABLE_SLOT = GEAR_SLOT_END;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = REFINEMENT_TARGET_SLOT + 1;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final IdentifierBlockEntity identifier;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private final PassiveProgressionView passiveProgressionView = MasteryMenuSupport.progressionView(
            this::hasPassiveNodeIndex, this::machineLevel, this::unspentPassivePoints, MachineMasteryFamily.IDENTIFIER.startNodeId());
    private final DataSlot ordersShown;
    private int selectedTab = TAB_PROCESSING;

    public IdentifierMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public IdentifierMenu(int containerId, Inventory playerInventory, IdentifierBlockEntity identifier, ContainerData data) {
        this(containerId, playerInventory, identifier, data, identifier.machineTraits());
    }

    private IdentifierMenu(
            int containerId,
            Inventory playerInventory,
            IdentifierBlockEntity identifier,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.IDENTIFIER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(identifier.getLevel(), identifier.getBlockPos());
        this.data = data;
        this.identifier = identifier;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, identifier.getBlockState().getBlock(), machineTraits);

        ItemStackHandler processInventory = identifier.getProcessInventory();
        addSlot(new TabbedSlot(processInventory, IdentifierBlockEntity.SLOT_INPUT, 72, 52, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, IdentifierBlockEntity.SLOT_OUTPUT, 150, 52, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, IdentifierBlockEntity.SLOT_FLUID_CONTAINER, 22, 86, () -> selectedTab == TAB_PROCESSING));

        ItemStackHandler gearInventory = identifier.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, IdentifierBlockEntity.SLOT_BATTERY_CELL, 56, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, IdentifierBlockEntity.SLOT_FLUID_PUMP, 92, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, IdentifierBlockEntity.SLOT_SERVO, 128, 48, () -> selectedTab == TAB_GEAR));

        addSlot(RefinementMenuSupport.consumableSlot(
                identifier.getRefinementInventory(),
                0,
                RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X,
                RefinementMenuSupport.REFINEMENT_SLOT_Y,
                () -> selectedTab == TAB_REFINEMENT
        ));
        addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, RefinementMenuSupport.REFINEMENT_TARGET_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));

        addPlayerInventory(playerInventory);
        IItemHandler bays = playerInventory.player.level().isClientSide ? DriveBays.client(this) : new DriveBays(identifier);
        for (int bay = 0; bay < MachineWorkOrders.SLOTS; bay++) {
            addSlot(new DriveBaySlot(bays, bay, () -> selectedTab == TAB_ORDERS));
        }
        IItemHandler sockets = playerInventory.player.level().isClientSide ? JewelSockets.client() : new JewelSockets(identifier);
        for (int index = 0; index < JewelSockets.SLOTS; index++) {
            int slot = index;
            addSlot(new JewelSocketSlot(sockets, slot, () -> jewelsTabSelected() && jewelSlotOpen(slot)));
        }
        addDataSlots(data);
        ordersShown = addDataSlot(WorkOrderDiscovery.shownSlot(playerInventory.player, identifier));
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_STATS -> TAB_STATS;
            case TAB_REFINEMENT -> TAB_REFINEMENT;
            case TAB_MASTERY -> TAB_MASTERY;
            case TAB_ORDERS -> TAB_ORDERS;
            case TAB_JEWELS -> TAB_JEWELS;
            default -> TAB_PROCESSING;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    @Override public WorkOrderHost workOrderHost() { return identifier; }
    @Override public boolean workOrdersShown() { return ordersShown.get() != 0; }
    @Override public String workOrderFamily() { return WorkOrderFamilies.of(masteryFamily()); }
    @Override public int workOrderStage() { return IdentifierBlockEntity.STAGE; }
    @Override public int workOrderMasteryLevel() { return machineLevel(); }
    @Override public WorkOrderMenuSupport.View workOrderView() {
        return MasteryMenuSupport.workOrders(data, PROGRESSION_START);
    }
    @Override public ItemStack workOrderChip(int bay) {
        return bay >= 0 && bay < MachineWorkOrders.SLOTS ? getSlot(HOTBAR_END + bay).getItem() : ItemStack.EMPTY;
    }

    private static final int JEWEL_SLOT_START = HOTBAR_END + MachineWorkOrders.SLOTS;

    @Override public ItemStack jewel(int index) {
        return index >= 0 && index < JewelSockets.SLOTS ? getSlot(JEWEL_SLOT_START + index).getItem() : ItemStack.EMPTY;
    }
    @Override public int jewelTreeCapacity() { return MegaPassiveTree.allocatedSocketCount(masterySnapshot()); }
    @Override public boolean jewelsTabSelected() { return selectedTab == TAB_JEWELS; }
    @Override public boolean hasTreeSockets() { return true; }
    @Override public int jewelBodyCapacity() { return JewelSockets.bodySockets(machineTraits()); }
    private boolean jewelSlotOpen(int index) {
        return JewelSocketSlot.isTreeSlot(index) ? index < jewelTreeCapacity() : index - JewelSockets.BODY_START < jewelBodyCapacity();
    }

    public float processingProgress() {
        if (instantFlash()) {
            return 1.0F;
        }
        int ticks = data.get(IdentifierBlockEntity.DATA_PROCESSING_TICKS);
        return ticks <= 0 ? 0.0F : Mth.clamp((float) progress() / (float) ticks, 0.0F, 1.0F);
    }

    public boolean instantFlash() {
        return data.get(IdentifierBlockEntity.DATA_PROGRESS) == BaseMachineBlockEntity.INSTANT_FLASH_PROGRESS;
    }

    public int progress() {
        return Math.max(0, data.get(IdentifierBlockEntity.DATA_PROGRESS));
    }

    public int processingTicks() {
        return data.get(IdentifierBlockEntity.DATA_PROCESSING_TICKS);
    }

    public float energyProgress() {
        int capacity = energyCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp((float) energy() / (float) capacity, 0.0F, 1.0F);
    }

    public int energy() {
        return data.get(IdentifierBlockEntity.DATA_ENERGY);
    }

    public int energyCapacity() {
        return data.get(IdentifierBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public int energyPerTick() {
        return data.get(IdentifierBlockEntity.DATA_ENERGY_PER_TICK);
    }

    public int energyPerItem() {
        return data.get(IdentifierBlockEntity.DATA_ENERGY_PER_ITEM);
    }

    public int statusCode() {
        return data.get(IdentifierBlockEntity.DATA_STATUS);
    }

    public int fluidAmount() {
        return data.get(IdentifierBlockEntity.DATA_FLUID);
    }

    public int fluidCapacity() {
        return data.get(IdentifierBlockEntity.DATA_FLUID_CAPACITY);
    }

    public int fluidPerItem() {
        return data.get(IdentifierBlockEntity.DATA_FLUID_PER_ITEM);
    }

    public float fluidProgress() {
        int capacity = fluidCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp((float) fluidAmount() / (float) capacity, 0.0F, 1.0F);
    }

    public double statValue(int dataIndex) {
        return dataIndex == IdentifierBlockEntity.DATA_FLUID_TRANSFER ? data.get(dataIndex) : data.get(dataIndex) / (double) STAT_SCALE;
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
    public long machineXp() {
        return MasteryMenuSupport.machineXp(data, PROGRESSION_START);
    }

    @Override
    public int machineLevel() {
        return MasteryMenuSupport.machineLevel(data, PROGRESSION_START);
    }

    @Override
    public int machineXpInLevel() {
        return MasteryMenuSupport.machineXpInLevel(data, PROGRESSION_START);
    }

    @Override
    public int machineXpToNextLevel() {
        return MasteryMenuSupport.machineXpToNextLevel(data, PROGRESSION_START);
    }

    @Override
    public float machineXpProgress() {
        return MasteryMenuSupport.machineXpProgress(data, PROGRESSION_START);
    }

    @Override
    public int unspentPassivePoints() {
        return MasteryMenuSupport.unspentPassivePoints(data, PROGRESSION_START);
    }

    @Override
    public boolean hasPassiveNode(PassiveNode node) {
        return node != null && node.isUnlocked(passiveProgressionView);
    }

    @Override
    public boolean canUnlockPassiveNode(MegaPassiveNode node) {
        return MegaPassiveTree.TREE.canUnlock(node, passiveProgressionView);
    }

    public boolean hasUnlockedPassiveConnection(PassiveNode node) {
        return node != null && node.parentUnlocked(passiveProgressionView);
    }

    private boolean hasPassiveNodeIndex(int index) {
        return MasteryMenuSupport.hasPassiveNodeIndex(data, PROGRESSION_START, index);
    }

    @Override
    public MachineMasteryHost masteryHost() {
        return identifier;
    }

    @Override
    public double masteryAttribute(MachineStat stat) {
        return MasteryMenuSupport.attribute(data, PROGRESSION_START, stat);
    }

    @Override
    public MachineMasteryFamily masteryFamily() {
        return MachineMasteryFamily.IDENTIFIER;
    }

    @Override
    public MachineProgressionState masterySnapshot() {
        return MasteryMenuSupport.snapshot(data, PROGRESSION_START, masteryFamily());
    }

    @Override
    public int ascendancyEntryStage() {
        return MasteryMenuSupport.ascendancyEntryStage(data, PROGRESSION_START);
    }

    @Override
    public List<MasteryMenuSupport.GrantedStat> ascendancyStats() {
        return MasteryMenuSupport.grantedStats(data, PROGRESSION_START);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= WorkOrderMenuSupport.BUTTON_SELECT_TAB && id <= WorkOrderMenuSupport.BUTTON_SELECT_TAB + TAB_JEWELS) {
            selectTab(id - WorkOrderMenuSupport.BUTTON_SELECT_TAB);
            return true;
        }
        if (FluidPurgeSupport.handleMenuButton(player, identifier, id)) {
            return true;
        }
        MegaPassiveNode passiveNode = MegaPassiveTree.byButtonId(id);
        if (passiveNode != null) {
            if (player.level().isClientSide) {
                return true;
            }
            return identifier.unlockPassiveNode(passiveNode);
        }
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return RefinementMenuSupport.applyToMachine(player, identifier, refinementTarget, identifier.getBlockState().getBlock());
    }

    @Override
    public MachineStatAccumulator breakdownStats() {
        return identifier.effectiveStats();
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof IdentifierBlock
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
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (selectedTab == TAB_ORDERS && index >= PLAYER_INVENTORY_START && JobChipItem.isWorkOrder(stack)) {
            if (!moveItemStackTo(stack, HOTBAR_END, HOTBAR_END + MachineWorkOrders.SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else if (selectedTab == TAB_JEWELS && index >= PLAYER_INVENTORY_START && JewelSockets.accepts(stack)) {
            if (!moveItemStackTo(stack, JEWEL_SLOT_START, JEWEL_SLOT_START + JewelSockets.SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, index == IdentifierBlockEntity.SLOT_OUTPUT)) {
                return ItemStack.EMPTY;
            }
        } else if (RefinementMenuSupport.isConsumable(stack)) {
            if (!moveItemStackTo(stack, REFINEMENT_CONSUMABLE_SLOT, REFINEMENT_CONSUMABLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (IdentifierBlockEntity.isBatteryCell(stack) && selectedTab == TAB_GEAR) {
            if (!moveToGear(stack, IdentifierBlockEntity.SLOT_BATTERY_CELL)) {
                return ItemStack.EMPTY;
            }
        } else if (IdentifierBlockEntity.isFluidPump(stack) && selectedTab == TAB_GEAR) {
            if (!moveToGear(stack, IdentifierBlockEntity.SLOT_FLUID_PUMP)) {
                return ItemStack.EMPTY;
            }
        } else if (IdentifierBlockEntity.isServo(stack) && selectedTab == TAB_GEAR) {
            if (!moveToGear(stack, IdentifierBlockEntity.SLOT_SERVO)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.IDENTIFYING_FLUID_BUCKET.get())) {
            if (!moveItemStackTo(stack, IdentifierBlockEntity.SLOT_FLUID_CONTAINER, IdentifierBlockEntity.SLOT_FLUID_CONTAINER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (IdentifierBlockEntity.canIdentify(stack)) {
            if (!moveItemStackTo(stack, IdentifierBlockEntity.SLOT_INPUT, IdentifierBlockEntity.SLOT_INPUT + 1, false)) {
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

    private boolean moveToGear(ItemStack stack, int gearSlot) {
        return moveItemStackTo(stack, GEAR_SLOT_START + gearSlot, GEAR_SLOT_START + gearSlot + 1, false);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new TabbedInventorySlot(playerInventory, column + row * 9 + 9, 39 + column * 18, 116 + row * 18,
                        () -> selectedTab != TAB_STATS && selectedTab != TAB_MASTERY));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(playerInventory, column, 39 + column * 18, 174,
                    () -> selectedTab != TAB_STATS && selectedTab != TAB_MASTERY));
        }
    }

    private static IdentifierBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof IdentifierBlockEntity identifier) {
            return identifier;
        }
        throw new IllegalStateException("Expected identifier block entity at " + pos);
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
