package com.rngtech.content.menu;

import com.rngtech.content.blockentity.SolarArrayControllerBlockEntity;
import com.rngtech.content.registry.ModBlocks;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class SolarArrayControllerMenu extends AbstractContainerMenu implements StatBreakdownMenu {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;

    private static final int DATA_ENERGY = 0;
    private static final int DATA_ENERGY_CAPACITY = 1;
    private static final int DATA_ENERGY_PER_TICK = 2;
    private static final int DATA_MAX_OUTPUT = 3;
    private static final int DATA_ACTIVE_PANELS = 4;
    private static final int DATA_BLOCKED_PANELS = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_ENERGY_GENERATION = 7;
    private static final int DATA_ENERGY_CAPACITY_STAT = 8;
    private static final int DATA_EFFICIENCY = 9;
    private static final int DATA_STABILITY = 10;
    private static final int DATA_REFINEMENT_POTENTIAL = 11;
    private static final int DATA_PANEL_LIMIT = 12;
    private static final int DATA_MOONLIGHT_CONVERSION = 13;
    private static final int DATA_WEATHER_RECOVERY = 14;
    private static final int DATA_PANEL_SYNCHRONIZATION = 15;
    private static final int DATA_OVERFLOW_SHUNTING = 16;
    private static final int DATA_CLEAR_SKY_AMPLIFICATION = 17;
    private static final int DATA_LUNAR_INVERSION = 18;
    private static final int DATA_PREVIEW_RANGE = 19;
    private static final int DATA_PANEL_RANGE = 20;
    private static final int DATA_FLAT_ENERGY_GENERATION = 21;
    private static final int DATA_BASE_ENERGY_GENERATION = 22;
    private static final int DATA_COUNT = 23;
    private static final int STAT_SCALE = 100;
    public static final int BUTTON_TOGGLE_PREVIEW_RANGE = 10;
    private static final int BATTERY_CELL_SLOT = 0;
    private static final int SOLAR_ARRAY_EXTENDER_SLOT = BATTERY_CELL_SLOT + 1;
    private static final int REFINEMENT_CONSUMABLE_SLOT = SOLAR_ARRAY_EXTENDER_SLOT + 1;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = REFINEMENT_TARGET_SLOT + 1;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final SolarArrayControllerBlockEntity controller;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private int selectedTab = TAB_PROCESSING;

    public SolarArrayControllerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public SolarArrayControllerMenu(
            int containerId,
            Inventory playerInventory,
            SolarArrayControllerBlockEntity controller,
            ContainerData data
    ) {
        this(containerId, playerInventory, controller, data, controller.machineTraits());
    }

    private SolarArrayControllerMenu(
            int containerId,
            Inventory playerInventory,
            SolarArrayControllerBlockEntity controller,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.SOLAR_ARRAY_CONTROLLER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(controller.getLevel(), controller.getBlockPos());
        this.data = data;
        this.controller = controller;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, ModBlocks.SOLAR_ARRAY_CONTROLLER.get(), machineTraits);

        addSlot(new TabbedSlot(
                controller.getGearInventory(),
                SolarArrayControllerBlockEntity.SLOT_BATTERY_CELL,
                93,
                48,
                () -> selectedTab == TAB_GEAR
        ));
        addSlot(new TabbedSlot(
                controller.getGearInventory(),
                SolarArrayControllerBlockEntity.SLOT_SOLAR_ARRAY_EXTENDER,
                57,
                48,
                () -> selectedTab == TAB_GEAR
        ));
        addSlot(RefinementMenuSupport.consumableSlot(
                controller.getRefinementInventory(),
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

    public float energyProgress() {
        int capacity = energyCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp((float) energy() / (float) capacity, 0.0F, 1.0F);
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

    public int activePanels() {
        return data.get(DATA_ACTIVE_PANELS);
    }

    public int blockedPanels() {
        return data.get(DATA_BLOCKED_PANELS);
    }

    public int panelLimit() {
        return data.get(DATA_PANEL_LIMIT);
    }

    public int panelRange() {
        return data.get(DATA_PANEL_RANGE);
    }

    public BlockPos controllerPos() {
        return controller.getBlockPos();
    }

    public boolean previewRange() {
        return data.get(DATA_PREVIEW_RANGE) != 0;
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
        if (dataIndex == DATA_REFINEMENT_POTENTIAL || dataIndex == DATA_PANEL_RANGE) {
            return data.get(dataIndex);
        }
        return data.get(dataIndex) / (double) STAT_SCALE;
    }

    public static int energyGenerationDataIndex() {
        return DATA_ENERGY_GENERATION;
    }

    public static int flatEnergyGenerationDataIndex() {
        return DATA_FLAT_ENERGY_GENERATION;
    }

    public static int baseEnergyGenerationDataIndex() {
        return DATA_BASE_ENERGY_GENERATION;
    }

    public static int energyCapacityStatDataIndex() {
        return DATA_ENERGY_CAPACITY_STAT;
    }

    public static int efficiencyDataIndex() {
        return DATA_EFFICIENCY;
    }

    public static int stabilityDataIndex() {
        return DATA_STABILITY;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    public static int panelLimitDataIndex() {
        return DATA_PANEL_RANGE;
    }

    public static int moonlightConversionDataIndex() {
        return DATA_MOONLIGHT_CONVERSION;
    }

    public static int weatherRecoveryDataIndex() {
        return DATA_WEATHER_RECOVERY;
    }

    public static int panelSynchronizationDataIndex() {
        return DATA_PANEL_SYNCHRONIZATION;
    }

    public static int overflowShuntingDataIndex() {
        return DATA_OVERFLOW_SHUNTING;
    }

    public static int clearSkyAmplificationDataIndex() {
        return DATA_CLEAR_SKY_AMPLIFICATION;
    }

    public static int lunarInversionDataIndex() {
        return DATA_LUNAR_INVERSION;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE_PREVIEW_RANGE) {
            if (!player.level().isClientSide) {
                controller.togglePreviewRange();
            }
            return true;
        }
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return RefinementMenuSupport.applyToMachine(
                player,
                controller,
                refinementTarget,
                ModBlocks.SOLAR_ARRAY_CONTROLLER.get()
        );
    }

    @Override
    public MachineStatAccumulator breakdownStats() {
        return controller.effectiveStats();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SOLAR_ARRAY_CONTROLLER.get());
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
        } else if (controller.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, BATTERY_CELL_SLOT, BATTERY_CELL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (controller.isSolarArrayExtender(stack)) {
            if (!moveItemStackTo(stack, SOLAR_ARRAY_EXTENDER_SLOT, SOLAR_ARRAY_EXTENDER_SLOT + 1, false)) {
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

    private static SolarArrayControllerBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof SolarArrayControllerBlockEntity controller) {
            return controller;
        }
        throw new IllegalStateException("Expected solar array controller block entity at " + pos);
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
