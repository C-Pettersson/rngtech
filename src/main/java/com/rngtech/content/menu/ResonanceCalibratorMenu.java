package com.rngtech.content.menu;

import com.rngtech.content.block.ResonanceCalibratorBlock;
import com.rngtech.content.blockentity.ResonanceCalibratorBlockEntity;
import com.rngtech.content.calibration.CalibrationFamily;
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

public class ResonanceCalibratorMenu extends AbstractContainerMenu implements MasteryMenuView<MegaPassiveNode> {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_CONFIGURATION = 2;
    public static final int TAB_REFINEMENT = 3;
    public static final int TAB_MASTERY = 4;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_STABILITY_MIN = 4;
    private static final int DATA_STABILITY_MAX = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_FAMILY = 7;
    private static final int DATA_BATCH_SIZE = 8;
    private static final int DATA_PROCESSING_SPEED = 9;
    private static final int DATA_EFFICIENCY = 10;
    private static final int DATA_ENERGY_USAGE = 11;
    private static final int DATA_ENERGY_CAPACITY_STAT = 12;
    private static final int DATA_ENERGY_TRANSFER = 13;
    private static final int DATA_STABILITY = 14;
    private static final int DATA_CALIBRATION_QUALITY = 15;
    private static final int DATA_CALIBRATION_PRECISION = 16;
    private static final int DATA_CATALYST_EFFICIENCY = 17;
    private static final int DATA_REFINEMENT_POTENTIAL_BONUS = 18;
    private static final int DATA_REFINEMENT_POTENTIAL = 19;
    private static final int DATA_SELECTED_PATTERN = 20;
    private static final int DATA_STREAK = DATA_SELECTED_PATTERN + 1;
    private static final int DATA_MACHINE_PROGRESSION_START = DATA_STREAK + 1;
    private static final int DATA_COUNT = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int STAT_SCALE = 100;
    /** Kept below 100 so pattern selection never overlaps Mastery node button ids. */
    public static final int BUTTON_SELECT_PATTERN_BASE = 10;
    private static final int PROCESS_SLOT_COUNT = ResonanceCalibratorBlockEntity.PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_START = PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_END = GEAR_SLOT_START + ResonanceCalibratorBlockEntity.GEAR_SLOT_COUNT;
    private static final int REFINEMENT_CONSUMABLE_SLOT = GEAR_SLOT_END;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PROCESSING_TARGET_SLOT = REFINEMENT_TARGET_SLOT + 1;
    private static final int MENU_MACHINE_SLOT_COUNT = PROCESSING_TARGET_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = MENU_MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final ResonanceCalibratorBlockEntity calibrator;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private final PassiveProgressionView passiveProgressionView = MasteryMenuSupport.progressionView(
            this::hasPassiveNodeIndex,
            this::machineLevel,
            this::unspentPassivePoints,
            MachineMasteryFamily.RESONANCE_CALIBRATOR.startNodeId()
    );
    private int selectedTab = TAB_PROCESSING;

    public ResonanceCalibratorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public ResonanceCalibratorMenu(
            int containerId,
            Inventory playerInventory,
            ResonanceCalibratorBlockEntity calibrator,
            ContainerData data
    ) {
        this(containerId, playerInventory, calibrator, data, calibrator.machineTraits());
    }

    private ResonanceCalibratorMenu(
            int containerId,
            Inventory playerInventory,
            ResonanceCalibratorBlockEntity calibrator,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.RESONANCE_CALIBRATOR.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(calibrator.getLevel(), calibrator.getBlockPos());
        this.data = data;
        this.calibrator = calibrator;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, calibrator.getBlockState().getBlock(), machineTraits);

        ItemStackHandler processInventory = calibrator.getProcessInventory();
        addSlot(new TabbedSlot(
                processInventory,
                ResonanceCalibratorBlockEntity.SLOT_INPUT,
                38,
                42,
                () -> selectedTab == TAB_PROCESSING
        ));
        addSlot(new TabbedSlot(
                processInventory,
                ResonanceCalibratorBlockEntity.SLOT_PATTERN,
                0,
                0,
                () -> false
        ));
        addSlot(new TabbedSlot(
                processInventory,
                ResonanceCalibratorBlockEntity.SLOT_CATALYST,
                80,
                42,
                () -> selectedTab == TAB_PROCESSING
        ));
        addSlot(new TabbedSlot(
                processInventory,
                ResonanceCalibratorBlockEntity.SLOT_STABILIZER,
                108,
                42,
                () -> selectedTab == TAB_PROCESSING
        ));
        addSlot(new TabbedSlot(
                processInventory,
                ResonanceCalibratorBlockEntity.SLOT_OUTPUT,
                176,
                42,
                () -> selectedTab == TAB_PROCESSING
        ));

        ItemStackHandler gearInventory = calibrator.getGearInventory();
        addSlot(new TabbedSlot(
                gearInventory,
                ResonanceCalibratorBlockEntity.SLOT_BATTERY_CELL,
                30,
                48,
                () -> selectedTab == TAB_GEAR
        ));
        addSlot(new TabbedSlot(
                gearInventory,
                ResonanceCalibratorBlockEntity.SLOT_RESONANCE_COIL,
                70,
                48,
                () -> selectedTab == TAB_GEAR
        ));
        addSlot(new TabbedSlot(
                gearInventory,
                ResonanceCalibratorBlockEntity.SLOT_CONTROL_BOARD,
                110,
                48,
                () -> selectedTab == TAB_GEAR
        ));
        addSlot(new TabbedSlot(
                gearInventory,
                ResonanceCalibratorBlockEntity.SLOT_STABILIZER_MATRIX,
                150,
                48,
                () -> selectedTab == TAB_GEAR
        ));
        for (int index = 0; index < ResonanceCalibratorBlockEntity.PATTERN_SLOT_COUNT; index++) {
            addSlot(new TabbedSlot(
                    gearInventory,
                    ResonanceCalibratorBlockEntity.SLOT_PATTERN_STORAGE + index,
                    24 + index * 36,
                    76,
                    () -> selectedTab == TAB_GEAR
            ));
        }

        addSlot(RefinementMenuSupport.consumableSlot(
                calibrator.getRefinementInventory(),
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

    public float processingProgress() {
        int ticks = data.get(DATA_PROCESSING_TICKS);
        return ticks <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_PROGRESS) / (float) ticks, 0.0F, 1.0F);
    }

    public float energyProgress() {
        int capacity = data.get(DATA_ENERGY_CAPACITY);
        return capacity <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_ENERGY) / (float) capacity, 0.0F, 1.0F);
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

    public int stabilityMin() {
        return data.get(DATA_STABILITY_MIN);
    }

    public int stabilityMax() {
        return data.get(DATA_STABILITY_MAX);
    }

    public int status() {
        return data.get(DATA_STATUS);
    }

    /** The Resonant Streak's current stability floor bonus. */
    public int streakFloor() {
        return data.get(DATA_STREAK);
    }

    public int selectedPattern() {
        return data.get(DATA_SELECTED_PATTERN);
    }


    public CalibrationFamily family() {
        int family = data.get(DATA_FAMILY);
        return family < 0 ? null : CalibrationFamily.byId(family);
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
        return calibrator;
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
    public MachineMasteryFamily masteryFamily() {
        return MachineMasteryFamily.RESONANCE_CALIBRATOR;
    }

    @Override
    public double masteryAttribute(MachineStat stat) {
        return MasteryMenuSupport.attribute(data, DATA_MACHINE_PROGRESSION_START, stat);
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

    public static int batchSizeDataIndex() {
        return DATA_BATCH_SIZE;
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

    public static int stabilityDataIndex() {
        return DATA_STABILITY;
    }

    public static int calibrationQualityDataIndex() {
        return DATA_CALIBRATION_QUALITY;
    }

    public static int calibrationPrecisionDataIndex() {
        return DATA_CALIBRATION_PRECISION;
    }

    public static int catalystEfficiencyDataIndex() {
        return DATA_CATALYST_EFFICIENCY;
    }

    public static int refinementPotentialBonusDataIndex() {
        return DATA_REFINEMENT_POTENTIAL_BONUS;
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
        MegaPassiveNode passiveNode = MegaPassiveTree.byButtonId(id);
        if (passiveNode != null) {
            if (player.level().isClientSide) {
                return true;
            }
            return calibrator.unlockPassiveNode(passiveNode);
        }
        if (id >= BUTTON_SELECT_PATTERN_BASE
                && id < BUTTON_SELECT_PATTERN_BASE + ResonanceCalibratorBlockEntity.PATTERN_SLOT_COUNT) {
            int selectedPattern = id - BUTTON_SELECT_PATTERN_BASE;
            data.set(DATA_SELECTED_PATTERN, selectedPattern);
            calibrator.selectPattern(selectedPattern);
            return true;
        }
        if (id == RefinementMenuSupport.BUTTON_APPLY) {
            if (player.level().isClientSide) {
                return true;
            }
            return RefinementMenuSupport.applyToMachine(player, calibrator, refinementTarget, calibrator.getBlockState().getBlock());
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof ResonanceCalibratorBlock
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

        if (index == ResonanceCalibratorBlockEntity.SLOT_OUTPUT) {
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
        } else if (ResonanceCalibratorBlockEntity.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + ResonanceCalibratorBlockEntity.SLOT_BATTERY_CELL, GEAR_SLOT_START + ResonanceCalibratorBlockEntity.SLOT_BATTERY_CELL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ResonanceCalibratorBlockEntity.isResonanceCoil(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + ResonanceCalibratorBlockEntity.SLOT_RESONANCE_COIL, GEAR_SLOT_START + ResonanceCalibratorBlockEntity.SLOT_RESONANCE_COIL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ResonanceCalibratorBlockEntity.isControlBoard(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + ResonanceCalibratorBlockEntity.SLOT_CONTROL_BOARD, GEAR_SLOT_START + ResonanceCalibratorBlockEntity.SLOT_CONTROL_BOARD + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ResonanceCalibratorBlockEntity.isStabilizerMatrix(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + ResonanceCalibratorBlockEntity.SLOT_STABILIZER_MATRIX, GEAR_SLOT_START + ResonanceCalibratorBlockEntity.SLOT_STABILIZER_MATRIX + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ResonanceCalibratorBlockEntity.isCalibrationPattern(stack)) {
            if (!moveItemStackTo(
                    stack,
                    GEAR_SLOT_START + ResonanceCalibratorBlockEntity.SLOT_PATTERN_STORAGE,
                    GEAR_SLOT_START + ResonanceCalibratorBlockEntity.SLOT_PATTERN_STORAGE + ResonanceCalibratorBlockEntity.PATTERN_SLOT_COUNT,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, ResonanceCalibratorBlockEntity.SLOT_INPUT, ResonanceCalibratorBlockEntity.SLOT_STABILIZER + 1, false)) {
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

    private static ResonanceCalibratorBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof ResonanceCalibratorBlockEntity calibrator) {
            return calibrator;
        }
        throw new IllegalStateException("Expected resonance calibrator block entity at " + pos);
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
