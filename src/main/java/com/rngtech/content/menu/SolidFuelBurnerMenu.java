package com.rngtech.content.menu;

import com.rngtech.content.blockentity.SolidFuelBurnerBlockEntity;
import com.rngtech.content.registry.ModMenus;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class SolidFuelBurnerMenu extends AbstractContainerMenu {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;

    private static final int DATA_BURN_TIME = 0;
    private static final int DATA_TOTAL_BURN_TIME = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_ENERGY_PER_TICK = 4;
    private static final int DATA_MAX_OUTPUT = 5;
    private static final int DATA_ACTIVE_FUEL_SLOTS = 6;
    private static final int DATA_MAX_FUEL_TIER = 7;
    private static final int DATA_FUEL_FORMS = 8;
    private static final int DATA_STATUS = 9;
    private static final int DATA_ENERGY_GENERATION = 10;
    private static final int DATA_ENERGY_TRANSFER = 11;
    private static final int DATA_EFFICIENCY = 12;
    private static final int DATA_FUEL_EFFICIENCY = 13;
    private static final int DATA_HEAT_ISOLATION = 14;
    private static final int DATA_STABILITY = 15;
    private static final int DATA_FUEL_DURATION = 16;
    private static final int DATA_REFINEMENT_POTENTIAL = 17;
    private static final int DATA_LAST_OUTPUT = 18;
    private static final int DATA_CONNECTOR_CAP = 19;
    private static final int DATA_COUNT = 20;
    private static final int STAT_SCALE = 100;
    private static final int FUEL_SLOT_START = 0;
    private static final int HEAT_CORE_SLOT = FUEL_SLOT_START + SolidFuelBurnerBlockEntity.FUEL_SLOT_COUNT;
    private static final int BATTERY_CELL_SLOT = HEAT_CORE_SLOT + 1;
    private static final int FUEL_BOX_SLOT = BATTERY_CELL_SLOT + 1;
    private static final int REFINEMENT_CONSUMABLE_SLOT = FUEL_BOX_SLOT + 1;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = REFINEMENT_TARGET_SLOT + 1;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final SolidFuelBurnerBlockEntity burner;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private int selectedTab = TAB_PROCESSING;

    public SolidFuelBurnerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public SolidFuelBurnerMenu(int containerId, Inventory playerInventory, SolidFuelBurnerBlockEntity burner, ContainerData data) {
        this(containerId, playerInventory, burner, data, burner.machineTraits());
    }

    private SolidFuelBurnerMenu(
            int containerId,
            Inventory playerInventory,
            SolidFuelBurnerBlockEntity burner,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.SOLID_FUEL_BURNER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(burner.getLevel(), burner.getBlockPos());
        this.data = data;
        this.burner = burner;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, burner.block().asItem(), machineTraits);

        ItemStackHandler fuelInventory = burner.getFuelInventory();
        addSlot(new TabbedSlot(fuelInventory, 0, 51, 55, () -> selectedTab == TAB_PROCESSING && fuelSlotActive(0)));
        addSlot(new TabbedSlot(fuelInventory, 1, 73, 55, () -> selectedTab == TAB_PROCESSING && fuelSlotActive(1)));

        ItemStackHandler gearInventory = burner.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, SolidFuelBurnerBlockEntity.SLOT_HEAT_CORE, 44, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, SolidFuelBurnerBlockEntity.SLOT_BATTERY_CELL, 80, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, SolidFuelBurnerBlockEntity.SLOT_FUEL_BOX, 116, 48, () -> selectedTab == TAB_GEAR));

        addSlot(RefinementMenuSupport.consumableSlot(
                burner.getRefinementInventory(),
                0,
                RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X,
                RefinementMenuSupport.REFINEMENT_SLOT_Y,
                () -> selectedTab == TAB_REFINEMENT
        ));
        addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, RefinementMenuSupport.REFINEMENT_TARGET_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_STATS -> TAB_STATS;
            case TAB_REFINEMENT -> TAB_REFINEMENT;
            default -> TAB_PROCESSING;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    public float fuelProgress() {
        int total = data.get(DATA_TOTAL_BURN_TIME);
        return total <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_BURN_TIME) / (float) total, 0.0F, 1.0F);
    }

    public float energyProgress() {
        int capacity = energyCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp((float) energy() / (float) capacity, 0.0F, 1.0F);
    }

    public int burnTime() {
        return data.get(DATA_BURN_TIME);
    }

    public int totalBurnTime() {
        return data.get(DATA_TOTAL_BURN_TIME);
    }

    public int energy() {
        return data.get(DATA_ENERGY);
    }

    public int energyCapacity() {
        return data.get(DATA_ENERGY_CAPACITY);
    }

    public int energyPerTick() {
        return data.get(DATA_ENERGY_PER_TICK);
    }

    public int maxOutput() {
        return data.get(DATA_MAX_OUTPUT);
    }

    public int lastOutput() {
        return data.get(DATA_LAST_OUTPUT);
    }

    public int connectorOutputCap() {
        return data.get(DATA_CONNECTOR_CAP);
    }

    public int activeFuelSlots() {
        return data.get(DATA_ACTIVE_FUEL_SLOTS);
    }

    public boolean fuelSlotActive(int slot) {
        return slot >= 0 && slot < activeFuelSlots();
    }

    public int maxFuelTier() {
        return data.get(DATA_MAX_FUEL_TIER);
    }

    public int fuelFormsCode() {
        return data.get(DATA_FUEL_FORMS);
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
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

    public double statValue(int dataIndex) {
        if (dataIndex == DATA_REFINEMENT_POTENTIAL) {
            return data.get(dataIndex);
        }
        return data.get(dataIndex) / (double) STAT_SCALE;
    }

    public static int energyGenerationDataIndex() {
        return DATA_ENERGY_GENERATION;
    }

    public static int energyTransferDataIndex() {
        return DATA_ENERGY_TRANSFER;
    }

    public static int efficiencyDataIndex() {
        return DATA_EFFICIENCY;
    }

    public static int fuelEfficiencyDataIndex() {
        return DATA_FUEL_EFFICIENCY;
    }

    public static int fuelDurationDataIndex() {
        return DATA_FUEL_DURATION;
    }

    public static int heatIsolationDataIndex() {
        return DATA_HEAT_ISOLATION;
    }

    public static int stabilityDataIndex() {
        return DATA_STABILITY;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return RefinementMenuSupport.applyToMachine(player, burner, refinementTarget, burner.block().asItem());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, burner.block());
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
        } else if (RefinementMenuSupport.isConsumable(stack)) {
            if (!moveItemStackTo(stack, REFINEMENT_CONSUMABLE_SLOT, REFINEMENT_CONSUMABLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (burner.isHeatCore(stack)) {
            if (!moveItemStackTo(stack, HEAT_CORE_SLOT, HEAT_CORE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (burner.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, BATTERY_CELL_SLOT, BATTERY_CELL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (burner.isFuelBox(stack)) {
            if (!moveItemStackTo(stack, FUEL_BOX_SLOT, FUEL_BOX_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (burner.isAcceptedFuel(stack)) {
            if (!moveItemStackTo(stack, FUEL_SLOT_START, FUEL_SLOT_START + activeFuelSlots(), false)) {
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

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new TabbedInventorySlot(
                        playerInventory,
                        column + row * 9 + 9,
                        39 + column * 18,
                        116 + row * 18,
                        () -> selectedTab != TAB_STATS
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(
                    playerInventory,
                    column,
                    39 + column * 18,
                    174,
                    () -> selectedTab != TAB_STATS
            ));
        }
    }

    private static SolidFuelBurnerBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof SolidFuelBurnerBlockEntity burner) {
            return burner;
        }
        throw new IllegalStateException("Expected solid fuel burner block entity at " + pos);
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
