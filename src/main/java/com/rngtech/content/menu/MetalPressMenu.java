package com.rngtech.content.menu;

import com.rngtech.content.block.MetalPressBlock;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineMasteryHost;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;
import com.rngtech.rpg.progression.PassiveNode;
import com.rngtech.rpg.progression.PassiveProgressionView;

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

import java.util.List;
import java.util.function.BooleanSupplier;

public class MetalPressMenu extends AbstractContainerMenu implements MasteryMenuView<MegaPassiveNode> {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_CONFIGURATION = 2;
    public static final int TAB_REFINEMENT = 3;
    public static final int TAB_MASTERY = 4;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_HEAT = 4;
    private static final int DATA_MIN_TEMPERATURE = 5;
    private static final int DATA_TARGET_TEMPERATURE = 6;
    private static final int DATA_SAFE_MAX_TEMPERATURE = 7;
    private static final int DATA_OVERHEAT_TEMPERATURE = 8;
    private static final int DATA_FAILURE_RISK = 9;
    private static final int DATA_STATUS = 10;
    private static final int DATA_PROCESSING_SPEED = 11;
    private static final int DATA_EFFICIENCY = 12;
    private static final int DATA_ENERGY_USAGE = 13;
    private static final int DATA_ENERGY_CAPACITY_STAT = 14;
    private static final int DATA_ENERGY_TRANSFER = 15;
    private static final int DATA_HEAT_TRANSFER = 16;
    private static final int DATA_MAX_TEMPERATURE = 17;
    private static final int DATA_TEMPERATURE_STABILITY = 18;
    private static final int DATA_WARMUP_TIME = 19;
    private static final int DATA_COOLING_RATE = 20;
    private static final int DATA_OVERHEAT_TOLERANCE = 21;
    private static final int DATA_STABILITY = 22;
    private static final int DATA_REFINEMENT_POTENTIAL = 23;
    private static final int DATA_SELECTED_MOLD = 24;
    private static final int DATA_ENERGY_PER_TICK = 25;
    private static final int DATA_ENERGY_PER_CRAFT = 26;
    private static final int DATA_LEDGER = DATA_ENERGY_PER_CRAFT + 1;
    private static final int DATA_BATCH_SIZE = DATA_LEDGER + 1;
    private static final int DATA_MACHINE_PROGRESSION_START = DATA_BATCH_SIZE + 1;
    private static final int DATA_COUNT = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int STAT_SCALE = 100;
    public static final int BUTTON_SELECT_MOLD_BASE = 100;
    private static final int PROCESS_SLOT_COUNT = MetalPressBlockEntity.PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_START = PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_END = GEAR_SLOT_START + MetalPressBlockEntity.GEAR_SLOT_COUNT;
    private static final int REFINEMENT_CONSUMABLE_SLOT = GEAR_SLOT_END;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PROCESSING_TARGET_SLOT = REFINEMENT_TARGET_SLOT + 1;
    private static final int MENU_MACHINE_SLOT_COUNT = PROCESSING_TARGET_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = MENU_MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final MetalPressBlockEntity press;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private final PassiveProgressionView passiveProgressionView = MasteryMenuSupport.progressionView(
            this::hasPassiveNodeIndex,
            this::machineLevel,
            this::unspentPassivePoints,
            MachineMasteryFamily.METAL_PRESS.startNodeId()
    );
    private int selectedTab = TAB_PROCESSING;

    public MetalPressMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public MetalPressMenu(int containerId, Inventory playerInventory, MetalPressBlockEntity press, ContainerData data) {
        this(containerId, playerInventory, press, data, press.machineTraits());
    }

    private MetalPressMenu(
            int containerId,
            Inventory playerInventory,
            MetalPressBlockEntity press,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.METAL_PRESS.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(press.getLevel(), press.getBlockPos());
        this.data = data;
        this.press = press;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, press.getBlockState().getBlock(), machineTraits);

        ItemStackHandler processInventory = press.getProcessInventory();
        addSlot(new TabbedSlot(processInventory, MetalPressBlockEntity.SLOT_INPUT, 56, 42, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, MetalPressBlockEntity.SLOT_OUTPUT, 170, 52, () -> selectedTab == TAB_PROCESSING));

        ItemStackHandler gearInventory = press.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, MetalPressBlockEntity.SLOT_HEAT_CORE, 30, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, MetalPressBlockEntity.SLOT_SERVO, 78, 48, () -> selectedTab == TAB_GEAR));
        for (int index = 0; index < MetalPressBlockEntity.MOLD_SLOT_COUNT; index++) {
            addSlot(new TabbedSlot(gearInventory, MetalPressBlockEntity.SLOT_MOLD + index, 24 + index * 44, 76, () -> selectedTab == TAB_GEAR));
        }
        addSlot(new TabbedSlot(gearInventory, MetalPressBlockEntity.SLOT_BATTERY_CELL, 126, 48, () -> selectedTab == TAB_GEAR));

        addSlot(RefinementMenuSupport.consumableSlot(
                press.getRefinementInventory(),
                0,
                RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X,
                RefinementMenuSupport.REFINEMENT_SLOT_Y,
                () -> selectedTab == TAB_REFINEMENT
        ));
        addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, RefinementMenuSupport.REFINEMENT_TARGET_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));
        addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, 204, 42, () -> selectedTab == TAB_PROCESSING));

        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_CONFIGURATION -> TAB_CONFIGURATION;
            case TAB_REFINEMENT -> TAB_REFINEMENT;
            case TAB_MASTERY -> TAB_MASTERY;
            default -> TAB_PROCESSING;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    /** The Batch Ledger's progress toward its next item, from 0 to 1, or -1 when it does not apply. */
    public float ledgerProgress() {
        int value = data.get(DATA_LEDGER);
        return value < 0 ? -1.0F : Math.min(1.0F, value / 1000.0F);
    }

    public float processingProgress() {
        int ticks = data.get(DATA_PROCESSING_TICKS);
        return ticks <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_PROGRESS) / (float) ticks, 0.0F, 1.0F);
    }

    public float energyProgress() {
        int capacity = data.get(DATA_ENERGY_CAPACITY);
        return capacity <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_ENERGY) / (float) capacity, 0.0F, 1.0F);
    }

    public float heatProgress() {
        int overheat = data.get(DATA_OVERHEAT_TEMPERATURE);
        return overheat <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_HEAT) / (float) overheat, 0.0F, 1.0F);
    }

    public float failureRiskProgress() {
        return Mth.clamp(data.get(DATA_FAILURE_RISK) / 100.0F, 0.0F, 1.0F);
    }

    public int progress() {
        return data.get(DATA_PROGRESS);
    }

    public int processingTicks() {
        return data.get(DATA_PROCESSING_TICKS);
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

    public int energyPerCraft() {
        return data.get(DATA_ENERGY_PER_CRAFT);
    }

    public int heat() {
        return data.get(DATA_HEAT);
    }

    public int minimumTemperature() {
        return data.get(DATA_MIN_TEMPERATURE);
    }

    public int targetTemperature() {
        return data.get(DATA_TARGET_TEMPERATURE);
    }

    public int safeMaximumTemperature() {
        return data.get(DATA_SAFE_MAX_TEMPERATURE);
    }

    public int overheatTemperature() {
        return data.get(DATA_OVERHEAT_TEMPERATURE);
    }

    public int failureRisk() {
        return data.get(DATA_FAILURE_RISK);
    }

    public int status() {
        return data.get(DATA_STATUS);
    }

    public int selectedMold() {
        return data.get(DATA_SELECTED_MOLD);
    }

    public boolean isCrudePress() {
        return press.isCrudePress();
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

    @Override
    public boolean hasPassiveNode(PassiveNode node) {
        return node != null && node.isUnlocked(passiveProgressionView);
    }

    @Override
    public boolean canUnlockPassiveNode(MegaPassiveNode node) {
        return MegaPassiveTree.TREE.canUnlock(node, passiveProgressionView);
    }

    @Override
    public boolean hasUnlockedPassiveConnection(PassiveNode node) {
        return node != null && node.parentUnlocked(passiveProgressionView);
    }

    private boolean hasPassiveNodeIndex(int index) {
        return MasteryMenuSupport.hasPassiveNodeIndex(data, DATA_MACHINE_PROGRESSION_START, index);
    }

    @Override
    public MachineMasteryHost masteryHost() {
        return press;
    }

    @Override
    public MachineMasteryFamily masteryFamily() {
        return MachineMasteryFamily.METAL_PRESS;
    }

    @Override
    public MachineProgressionState masterySnapshot() {
        return MasteryMenuSupport.snapshot(data, DATA_MACHINE_PROGRESSION_START, masteryFamily());
    }

    @Override
    public int ascendancyEntryStage() {
        return MasteryMenuSupport.ascendancyEntryStage(data, DATA_MACHINE_PROGRESSION_START);
    }

    @Override
    public List<MasteryMenuSupport.GrantedStat> ascendancyStats() {
        return MasteryMenuSupport.grantedStats(data, DATA_MACHINE_PROGRESSION_START);
    }

    @Override
    public double masteryAttribute(MachineStat stat) {
        return MasteryMenuSupport.attribute(data, DATA_MACHINE_PROGRESSION_START, stat);
    }

    @Override
    public boolean masterySupports(MachineStat stat) {
        return masteryFamily().supports(stat) && (stat != MachineStat.STABILITY || !isCrudePress());
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
        return data.get(dataIndex) / (double) STAT_SCALE;
    }

    public static int processingSpeedDataIndex() {
        return DATA_PROCESSING_SPEED;
    }

    public static int efficiencyDataIndex() {
        return DATA_EFFICIENCY;
    }

    public static int energyUsageDataIndex() {
        return DATA_ENERGY_USAGE;
    }

    public static int energyCapacityStatDataIndex() {
        return DATA_ENERGY_CAPACITY_STAT;
    }

    public static int energyTransferDataIndex() {
        return DATA_ENERGY_TRANSFER;
    }

    public static int heatTransferDataIndex() {
        return DATA_HEAT_TRANSFER;
    }

    public static int maxTemperatureDataIndex() {
        return DATA_MAX_TEMPERATURE;
    }

    public static int temperatureStabilityDataIndex() {
        return DATA_TEMPERATURE_STABILITY;
    }

    public static int warmupTimeDataIndex() {
        return DATA_WARMUP_TIME;
    }

    public static int coolingRateDataIndex() {
        return DATA_COOLING_RATE;
    }

    public static int overheatToleranceDataIndex() {
        return DATA_OVERHEAT_TOLERANCE;
    }

    public static int batchSizeDataIndex() {
        return DATA_BATCH_SIZE;
    }

    public static int stabilityDataIndex() {
        return DATA_STABILITY;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    public static int menuSlotForGearSlot(int gearSlot) {
        return GEAR_SLOT_START + gearSlot;
    }

    public static int playerInventoryStart() {
        return PLAYER_INVENTORY_START;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_SELECT_MOLD_BASE && id < BUTTON_SELECT_MOLD_BASE + MetalPressBlockEntity.MOLD_SLOT_COUNT) {
            int selectedMold = id - BUTTON_SELECT_MOLD_BASE;
            data.set(DATA_SELECTED_MOLD, selectedMold);
            press.selectMold(selectedMold);
            return true;
        }
        MegaPassiveNode passiveNode = MegaPassiveTree.byButtonId(id);
        if (passiveNode != null) {
            if (player.level().isClientSide) {
                return true;
            }
            return press.unlockPassiveNode(passiveNode);
        }
        if (id == RefinementMenuSupport.BUTTON_APPLY) {
            if (player.level().isClientSide) {
                return true;
            }
            return RefinementMenuSupport.applyToMachine(player, press, refinementTarget, press.getBlockState().getBlock());
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof MetalPressBlock
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
                true
        );
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

        if (index == MetalPressBlockEntity.SLOT_OUTPUT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, moved);
        } else if (index == REFINEMENT_CONSUMABLE_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (RefinementMenuSupport.isConsumable(stack)) {
            if (!moveItemStackTo(stack, REFINEMENT_CONSUMABLE_SLOT, REFINEMENT_CONSUMABLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MetalPressBlockEntity.isHeatCore(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + MetalPressBlockEntity.SLOT_HEAT_CORE, GEAR_SLOT_START + MetalPressBlockEntity.SLOT_HEAT_CORE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MetalPressBlockEntity.isServo(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + MetalPressBlockEntity.SLOT_SERVO, GEAR_SLOT_START + MetalPressBlockEntity.SLOT_SERVO + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MetalPressBlockEntity.isMetalPressMold(stack)) {
            if (!moveItemStackTo(
                    stack,
                    GEAR_SLOT_START + MetalPressBlockEntity.SLOT_MOLD,
                    GEAR_SLOT_START + MetalPressBlockEntity.SLOT_MOLD + MetalPressBlockEntity.MOLD_SLOT_COUNT,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (MetalPressBlockEntity.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + MetalPressBlockEntity.SLOT_BATTERY_CELL, GEAR_SLOT_START + MetalPressBlockEntity.SLOT_BATTERY_CELL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, MetalPressBlockEntity.SLOT_INPUT, MetalPressBlockEntity.SLOT_INPUT + 1, false)) {
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
                        () -> selectedTab != TAB_CONFIGURATION && selectedTab != TAB_MASTERY
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(
                    playerInventory,
                    column,
                    39 + column * 18,
                    174,
                    () -> selectedTab != TAB_CONFIGURATION && selectedTab != TAB_MASTERY
            ));
        }
    }

    private static MetalPressBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MetalPressBlockEntity press) {
            return press;
        }
        throw new IllegalStateException("Expected metal press block entity at " + pos);
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
