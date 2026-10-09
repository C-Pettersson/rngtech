package com.rngtech.content.menu;

import com.rngtech.content.block.CrusherBlock;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.progression.CrusherPassiveTree;
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

public class CrusherMenu extends AbstractContainerMenu implements MasteryMenuView<MegaPassiveNode> {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_CONFIGURATION = 2;
    public static final int TAB_REFINEMENT = 3;
    public static final int TAB_MASTERY = 4;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_INPUT_SLOTS = 4;
    private static final int DATA_OUTPUT_AMOUNT = 5;
    private static final int DATA_PROCESSING_SPEED = 6;
    private static final int DATA_PROCESSING_LEVEL = 7;
    private static final int DATA_ENERGY_USAGE = 8;
    private static final int DATA_ENERGY_CAPACITY_STAT = 9;
    private static final int DATA_ENERGY_TRANSFER = 10;
    private static final int DATA_REFINEMENT_POTENTIAL = 11;
    private static final int DATA_OUTPUT_BONUS_PROGRESS = 12;
    private static final int DATA_OUTPUT_BONUS_INCREMENT = 13;
    private static final int DATA_ENERGY_PER_TICK = 14;
    private static final int DATA_ENERGY_PER_CRAFT = 15;
    private static final int DATA_ACTIVE_JOBS = 16;
    private static final int DATA_BATCH_SIZE = 17;
    private static final int DATA_STATUS = 18;
    private static final int DATA_OUTPUT_GUARD_GRACE = 19;
    private static final int DATA_NO_BATTERY_OUTPUT_RETENTION = 20;
    private static final int DATA_HIGH_HARDNESS_ENERGY_MITIGATION = 21;
    private static final int DATA_CRUSHER_INPUT_FILTER = 22;
    private static final int DATA_CRUSHER_SALVAGE_CHANCE = 23;
    private static final int DATA_JAM_TICKS = 24;
    private static final int DATA_REQUIRED_PROCESSING_LEVEL = 25;
    private static final int DATA_HARDNESS_DEFICIT = 26;
    private static final int DATA_JAM_CHANCE = 27;
    private static final int DATA_UNDER_LEVEL_PENALTY_MULTIPLIER = 28;
    private static final int DATA_MACHINE_PROGRESSION_START = 29;
    private static final int DATA_BATTERY_SLOT_BLOCKED = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int DATA_CRUSH_HEAD_MATCHING_STAGE_REQUIRED = DATA_BATTERY_SLOT_BLOCKED + 1;
    private static final int DATA_CHASSIS_STAGE = DATA_CRUSH_HEAD_MATCHING_STAGE_REQUIRED + 1;
    private static final int DATA_COUNT = DATA_CHASSIS_STAGE + 1;
    private static final int STAT_SCALE = 100;
    private static final int OUTPUT_BONUS_SCALE = 1000;
    private static final int MACHINE_SLOT_COUNT = CrusherBlockEntity.SLOT_COUNT;
    private static final int REFINEMENT_CONSUMABLE_SLOT = MACHINE_SLOT_COUNT;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PROCESSING_TARGET_SLOT = REFINEMENT_TARGET_SLOT + 1;
    private static final int MENU_MACHINE_SLOT_COUNT = PROCESSING_TARGET_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = MENU_MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final CrusherBlockEntity crusher;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private final PassiveProgressionView passiveProgressionView =
            MasteryMenuSupport.progressionView(this::hasPassiveNodeIndex, this::machineLevel, this::unspentPassivePoints, MachineMasteryFamily.CRUSHER.startNodeId());
    private int selectedTab = TAB_PROCESSING;

    public CrusherMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public CrusherMenu(int containerId, Inventory playerInventory, CrusherBlockEntity crusher, ContainerData data) {
        this(containerId, playerInventory, crusher, data, crusher.machineTraits());
    }

    private CrusherMenu(
            int containerId,
            Inventory playerInventory,
            CrusherBlockEntity crusher,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.CRUSHER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(crusher.getLevel(), crusher.getBlockPos());
        this.data = data;
        this.crusher = crusher;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, crusher.getBlockState().getBlock(), machineTraits);

        ItemStackHandler inventory = crusher.getInventory();
        this.addSlot(new TabbedSlot(inventory, CrusherBlockEntity.SLOT_INPUT_A, 56, 42, () -> selectedTab == TAB_PROCESSING));
        this.addSlot(new TabbedSlot(inventory, CrusherBlockEntity.SLOT_FUEL, 80, 48, () -> selectedTab == TAB_GEAR));
        this.addSlot(new TabbedSlot(inventory, CrusherBlockEntity.SLOT_CRUSH_HEAD, 44, 48, () -> selectedTab == TAB_GEAR));
        this.addSlot(new TabbedSlot(inventory, CrusherBlockEntity.SLOT_OUTPUT, 170, 52, () -> selectedTab == TAB_PROCESSING));
        this.addSlot(RefinementMenuSupport.consumableSlot(
                crusher.getRefinementInventory(),
                0,
                RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X,
                RefinementMenuSupport.REFINEMENT_SLOT_Y,
                () -> selectedTab == TAB_REFINEMENT
        ));
        this.addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, RefinementMenuSupport.REFINEMENT_TARGET_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));
        this.addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, 204, 42, () -> selectedTab == TAB_PROCESSING));

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

    public float outputBonusProgress() {
        return Mth.clamp((float) data.get(DATA_OUTPUT_BONUS_PROGRESS) / (float) OUTPUT_BONUS_SCALE, 0.0F, 1.0F);
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

    public int activeJobs() {
        return data.get(DATA_ACTIVE_JOBS);
    }

    public int batchSize() {
        return data.get(DATA_BATCH_SIZE);
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    public int jamTicks() {
        return data.get(DATA_JAM_TICKS);
    }

    public int processingLevel() {
        return (int) statValue(DATA_PROCESSING_LEVEL);
    }

    public int requiredProcessingLevel() {
        return data.get(DATA_REQUIRED_PROCESSING_LEVEL);
    }

    public int hardnessDeficit() {
        return data.get(DATA_HARDNESS_DEFICIT);
    }

    public int jamChancePerThousand() {
        return data.get(DATA_JAM_CHANCE);
    }

    public double underLevelPenaltyMultiplier() {
        return data.get(DATA_UNDER_LEVEL_PENALTY_MULTIPLIER) / (double) STAT_SCALE;
    }

    public boolean hasBatteryCell() {
        return !batterySlotBlocked() && CrusherBlockEntity.isBatteryCell(getSlot(CrusherBlockEntity.SLOT_FUEL).getItem());
    }

    public boolean batterySlotBlocked() {
        return data.get(DATA_BATTERY_SLOT_BLOCKED) != 0;
    }

    public boolean matchingStageCrushHeadRequired() {
        return data.get(DATA_CRUSH_HEAD_MATCHING_STAGE_REQUIRED) != 0;
    }

    public int chassisStage() {
        return data.get(DATA_CHASSIS_STAGE);
    }

    public long machineXp() {
        return MasteryMenuSupport.machineXp(data, DATA_MACHINE_PROGRESSION_START);
    }

    public int machineLevel() {
        return MasteryMenuSupport.machineLevel(data, DATA_MACHINE_PROGRESSION_START);
    }

    public int machineXpInLevel() {
        return MasteryMenuSupport.machineXpInLevel(data, DATA_MACHINE_PROGRESSION_START);
    }

    public int machineXpToNextLevel() {
        return MasteryMenuSupport.machineXpToNextLevel(data, DATA_MACHINE_PROGRESSION_START);
    }

    public float machineXpProgress() {
        return MasteryMenuSupport.machineXpProgress(data, DATA_MACHINE_PROGRESSION_START);
    }

    public int unspentPassivePoints() {
        return MasteryMenuSupport.unspentPassivePoints(data, DATA_MACHINE_PROGRESSION_START);
    }

    public long unlockedPassiveNodeMask() {
        return MasteryMenuSupport.unlockedPassiveNodeMask(data, DATA_MACHINE_PROGRESSION_START);
    }

    public long unlockedPassiveNodeMaskHigh() {
        return MasteryMenuSupport.unlockedPassiveNodeMaskHigh(data, DATA_MACHINE_PROGRESSION_START);
    }

    public boolean hasPassiveNode(PassiveNode node) {
        return node != null && node.isUnlocked(passiveProgressionView);
    }

    public boolean canUnlockPassiveNode(MegaPassiveNode node) {
        return CrusherPassiveTree.TREE.canUnlock(node, passiveProgressionView);
    }

    public boolean hasUnlockedPassiveConnection(PassiveNode node) {
        return node != null && node.parentUnlocked(passiveProgressionView);
    }

    private boolean hasPassiveNodeIndex(int index) {
        return MasteryMenuSupport.hasPassiveNodeIndex(data, DATA_MACHINE_PROGRESSION_START, index);
    }

    public int outputBonusProgressRaw() {
        return data.get(DATA_OUTPUT_BONUS_PROGRESS);
    }

    public int outputBonusIncrementRaw() {
        return data.get(DATA_OUTPUT_BONUS_INCREMENT);
    }

    public int outputBonusScale() {
        return OUTPUT_BONUS_SCALE;
    }

    public int outputBonusPayoutsNextCraft() {
        int increment = outputBonusIncrementRaw();
        if (increment <= 0) {
            return 0;
        }
        return (outputBonusProgressRaw() + increment) / OUTPUT_BONUS_SCALE;
    }

    public int outputBonusProgressAfterNextCraftRaw() {
        int increment = outputBonusIncrementRaw();
        if (increment <= 0) {
            return outputBonusProgressRaw();
        }
        return (outputBonusProgressRaw() + increment) % OUTPUT_BONUS_SCALE;
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

    public int effectiveInputSlots() {
        return Math.max(1, data.get(DATA_INPUT_SLOTS));
    }

    public double statValue(int dataIndex) {
        return switch (dataIndex) {
            case DATA_INPUT_SLOTS -> effectiveInputSlots();
            case DATA_BATCH_SIZE -> batchSize();
            default -> data.get(dataIndex) / (double) STAT_SCALE;
        };
    }

    public static int inputSlotsDataIndex() {
        return DATA_INPUT_SLOTS;
    }

    public static int outputAmountDataIndex() {
        return DATA_OUTPUT_AMOUNT;
    }

    public static int processingSpeedDataIndex() {
        return DATA_PROCESSING_SPEED;
    }

    public static int processingLevelDataIndex() {
        return DATA_PROCESSING_LEVEL;
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

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    public static int batchSizeDataIndex() {
        return DATA_BATCH_SIZE;
    }

    public static int outputGuardGraceDataIndex() {
        return DATA_OUTPUT_GUARD_GRACE;
    }

    public static int noBatteryOutputRetentionDataIndex() {
        return DATA_NO_BATTERY_OUTPUT_RETENTION;
    }

    public static int highHardnessEnergyMitigationDataIndex() {
        return DATA_HIGH_HARDNESS_ENERGY_MITIGATION;
    }

    public static int crusherInputFilterDataIndex() {
        return DATA_CRUSHER_INPUT_FILTER;
    }

    public static int crusherSalvageChanceDataIndex() {
        return DATA_CRUSHER_SALVAGE_CHANCE;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        MegaPassiveNode passiveNode = MegaPassiveTree.byButtonId(id);
        if (passiveNode != null) {
            if (player.level().isClientSide) {
                return true;
            }
            return crusher.unlockPassiveNode(passiveNode);
        }
        if (id == RefinementMenuSupport.BUTTON_APPLY) {
            if (player.level().isClientSide) {
                return true;
            }
            return RefinementMenuSupport.applyToMachine(player, crusher, refinementTarget, crusher.getBlockState().getBlock());
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof CrusherBlock
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
                true
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return moved;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();

        if (index == CrusherBlockEntity.SLOT_OUTPUT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, moved);
        } else if (index == REFINEMENT_CONSUMABLE_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_START) {
            return ItemStack.EMPTY;
        } else if (RefinementMenuSupport.isConsumable(stack)) {
            if (!moveItemStackTo(stack, REFINEMENT_CONSUMABLE_SLOT, REFINEMENT_CONSUMABLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (CrusherBlockEntity.isGearComponent(stack) && crusher.canInstallCrushHead(stack)) {
            if (!moveItemStackTo(stack, CrusherBlockEntity.SLOT_CRUSH_HEAD, CrusherBlockEntity.SLOT_CRUSH_HEAD + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (crusher.canInstallBatteryCell(stack)) {
            if (!moveItemStackTo(stack, CrusherBlockEntity.SLOT_FUEL, CrusherBlockEntity.SLOT_FUEL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, CrusherBlockEntity.SLOT_INPUT_A, CrusherBlockEntity.SLOT_INPUT_A + 1, false)) {
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

    private static CrusherBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof CrusherBlockEntity crusher) {
            return crusher;
        }
        throw new IllegalStateException("Expected crusher block entity at " + pos);
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

    @Override public MachineMasteryHost masteryHost() { return crusher; }
    @Override public double masteryAttribute(MachineStat stat) { return MasteryMenuSupport.attribute(data, DATA_MACHINE_PROGRESSION_START, stat); }
    @Override public MachineMasteryFamily masteryFamily() { return MachineMasteryFamily.CRUSHER; }
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
