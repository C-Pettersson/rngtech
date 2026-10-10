package com.rngtech.content.menu;

import com.rngtech.content.block.MelterBlock;
import com.rngtech.content.blockentity.MelterBlockEntity;
import com.rngtech.content.purge.FluidPurgeSupport;
import com.rngtech.content.registry.ModMenus;
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

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;
import java.util.function.BooleanSupplier;

public class MelterMenu extends AbstractContainerMenu implements MasteryMenuView<MegaPassiveNode>, StatBreakdownMenu {
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
    private static final int DATA_PROCESSING_LEVEL = 6;
    private static final int DATA_REQUIRED_PROCESSING_LEVEL = 7;
    private static final int DATA_INPUT_FLUID = 8;
    private static final int DATA_INPUT_FLUID_CAPACITY = 9;
    private static final int DATA_OUTPUT_FLUID = 10;
    private static final int DATA_OUTPUT_FLUID_CAPACITY = 11;
    private static final int DATA_STATUS = 12;
    private static final int DATA_PROCESSING_SPEED = 13;
    private static final int DATA_EFFICIENCY = 14;
    private static final int DATA_ENERGY_USAGE = 15;
    private static final int DATA_ENERGY_CAPACITY_STAT = 16;
    private static final int DATA_ENERGY_TRANSFER = 17;
    private static final int DATA_HEAT_TRANSFER = 18;
    private static final int DATA_MAX_TEMPERATURE = 19;
    private static final int DATA_FLUID_TRANSFER = 20;
    private static final int DATA_REFINEMENT_POTENTIAL = 21;
    private static final int DATA_BATCH_SIZE = DATA_REFINEMENT_POTENTIAL + 1;
    private static final int DATA_INPUT_FLUID_ID = DATA_BATCH_SIZE + 1;
    private static final int DATA_OUTPUT_FLUID_ID = DATA_INPUT_FLUID_ID + 1;
    private static final int DATA_MACHINE_PROGRESSION_START = DATA_OUTPUT_FLUID_ID + 1;
    private static final int DATA_COUNT = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int STAT_SCALE = 100;
    private static final int PROCESS_SLOT_COUNT = MelterBlockEntity.PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_START = PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_END = GEAR_SLOT_START + MelterBlockEntity.GEAR_SLOT_COUNT;
    private static final int REFINEMENT_CONSUMABLE_SLOT = GEAR_SLOT_END;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PROCESSING_TARGET_SLOT = REFINEMENT_TARGET_SLOT + 1;
    private static final int MENU_MACHINE_SLOT_COUNT = PROCESSING_TARGET_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = MENU_MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final MelterBlockEntity melter;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private final PassiveProgressionView passiveProgressionView =
            MasteryMenuSupport.progressionView(this::hasPassiveNodeIndex, this::machineLevel, this::unspentPassivePoints, MachineMasteryFamily.MELTER.startNodeId());
    private int selectedTab = TAB_PROCESSING;

    public MelterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public MelterMenu(int containerId, Inventory playerInventory, MelterBlockEntity melter, ContainerData data) {
        this(containerId, playerInventory, melter, data, melter.machineTraits());
    }

    private MelterMenu(
            int containerId,
            Inventory playerInventory,
            MelterBlockEntity melter,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.MELTER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(melter.getLevel(), melter.getBlockPos());
        this.data = data;
        this.melter = melter;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, melter.getBlockState().getBlock(), machineTraits);

        ItemStackHandler processInventory = melter.getProcessInventory();
        addSlot(new TabbedSlot(processInventory, MelterBlockEntity.SLOT_PRIMARY_INPUT, 58, 29, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, MelterBlockEntity.SLOT_SECONDARY_INPUT, 82, 29, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, MelterBlockEntity.SLOT_FLUID_INPUT_CONTAINER, 38, 81, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, MelterBlockEntity.SLOT_FLUID_OUTPUT_CONTAINER, 176, 81, () -> selectedTab == TAB_PROCESSING));

        ItemStackHandler gearInventory = melter.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, MelterBlockEntity.SLOT_HEAT_CORE, 20, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, MelterBlockEntity.SLOT_CRUSH_HEAD, 68, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, MelterBlockEntity.SLOT_BATTERY_CELL, 116, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, MelterBlockEntity.SLOT_SERVO, 164, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, MelterBlockEntity.SLOT_FLUID_PUMP, 212, 48, () -> selectedTab == TAB_GEAR));

        addSlot(RefinementMenuSupport.consumableSlot(
                melter.getRefinementInventory(),
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

    public float heatProgress() {
        int target = Math.max(data.get(DATA_MIN_TEMPERATURE), data.get(DATA_HEAT));
        return target <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_HEAT) / (float) target, 0.0F, 1.0F);
    }

    public float inputFluidProgress() {
        int capacity = data.get(DATA_INPUT_FLUID_CAPACITY);
        return capacity <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_INPUT_FLUID) / (float) capacity, 0.0F, 1.0F);
    }

    public float outputFluidProgress() {
        int capacity = data.get(DATA_OUTPUT_FLUID_CAPACITY);
        return capacity <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_OUTPUT_FLUID) / (float) capacity, 0.0F, 1.0F);
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

    public int heat() {
        return data.get(DATA_HEAT);
    }

    public int minimumTemperature() {
        return data.get(DATA_MIN_TEMPERATURE);
    }

    public int processingLevel() {
        return data.get(DATA_PROCESSING_LEVEL);
    }

    public int requiredProcessingLevel() {
        return data.get(DATA_REQUIRED_PROCESSING_LEVEL);
    }

    public int inputFluid() {
        return data.get(DATA_INPUT_FLUID);
    }

    public int inputFluidCapacity() {
        return data.get(DATA_INPUT_FLUID_CAPACITY);
    }

    public int outputFluid() {
        return data.get(DATA_OUTPUT_FLUID);
    }

    public int outputFluidCapacity() {
        return data.get(DATA_OUTPUT_FLUID_CAPACITY);
    }

    public Fluid inputFluidType() {
        return FluidMenuSupport.fluid(data.get(DATA_INPUT_FLUID_ID));
    }

    public Fluid outputFluidType() {
        return FluidMenuSupport.fluid(data.get(DATA_OUTPUT_FLUID_ID));
    }

    public Component inputFluidName() {
        return FluidMenuSupport.fluidName(data.get(DATA_INPUT_FLUID_ID), Component.translatable("block.minecraft.water"));
    }

    public Component outputFluidName() {
        return FluidMenuSupport.fluidName(data.get(DATA_OUTPUT_FLUID_ID), Component.translatable("rngtech.melter.product"));
    }

    public int fluidTransfer() {
        return data.get(DATA_FLUID_TRANSFER);
    }

    public int status() {
        return data.get(DATA_STATUS);
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

    public boolean hasPassiveNode(PassiveNode node) {
        return node != null && node.isUnlocked(passiveProgressionView);
    }

    public boolean canUnlockPassiveNode(MegaPassiveNode node) {
        return MegaPassiveTree.TREE.canUnlock(node, passiveProgressionView);
    }

    public boolean hasUnlockedPassiveConnection(PassiveNode node) {
        return node != null && node.parentUnlocked(passiveProgressionView);
    }

    private boolean hasPassiveNodeIndex(int index) {
        return MasteryMenuSupport.hasPassiveNodeIndex(data, DATA_MACHINE_PROGRESSION_START, index);
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

    public static int batchSizeDataIndex() {
        return DATA_BATCH_SIZE;
    }

    public static int fluidTransferDataIndex() {
        return DATA_FLUID_TRANSFER;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (FluidPurgeSupport.handleMenuButton(player, melter, id)) {
            return true;
        }
        MegaPassiveNode passiveNode = MegaPassiveTree.byButtonId(id);
        if (passiveNode != null) {
            if (player.level().isClientSide) {
                return true;
            }
            return melter.unlockPassiveNode(passiveNode);
        }
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return RefinementMenuSupport.applyToMachine(player, melter, refinementTarget, melter.getBlockState().getBlock());
    }

    @Override
    public MachineStatAccumulator breakdownStats() {
        return melter.effectiveStats();
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof MelterBlock
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

        if (index == REFINEMENT_CONSUMABLE_SLOT) {
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
        } else if (MelterBlockEntity.isHeatCore(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + MelterBlockEntity.SLOT_HEAT_CORE, GEAR_SLOT_START + MelterBlockEntity.SLOT_HEAT_CORE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MelterBlockEntity.isCrushHead(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + MelterBlockEntity.SLOT_CRUSH_HEAD, GEAR_SLOT_START + MelterBlockEntity.SLOT_CRUSH_HEAD + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MelterBlockEntity.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + MelterBlockEntity.SLOT_BATTERY_CELL, GEAR_SLOT_START + MelterBlockEntity.SLOT_BATTERY_CELL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MelterBlockEntity.isServo(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + MelterBlockEntity.SLOT_SERVO, GEAR_SLOT_START + MelterBlockEntity.SLOT_SERVO + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MelterBlockEntity.isFluidPump(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + MelterBlockEntity.SLOT_FLUID_PUMP, GEAR_SLOT_START + MelterBlockEntity.SLOT_FLUID_PUMP + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MelterBlockEntity.isFluidInputContainer(stack)) {
            if (!moveItemStackTo(stack, MelterBlockEntity.SLOT_FLUID_INPUT_CONTAINER, MelterBlockEntity.SLOT_FLUID_INPUT_CONTAINER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MelterBlockEntity.isFluidOutputContainer(stack)) {
            if (!moveItemStackTo(stack, MelterBlockEntity.SLOT_FLUID_OUTPUT_CONTAINER, MelterBlockEntity.SLOT_FLUID_OUTPUT_CONTAINER + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!melter.isMeltingInput(stack)
                || !moveItemStackTo(stack, MelterBlockEntity.SLOT_PRIMARY_INPUT, MelterBlockEntity.SLOT_PRIMARY_INPUT + 1, false)
                && !moveItemStackTo(stack, MelterBlockEntity.SLOT_SECONDARY_INPUT, MelterBlockEntity.SLOT_SECONDARY_INPUT + 1, false)) {
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

    private static MelterBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MelterBlockEntity melter) {
            return melter;
        }
        throw new IllegalStateException("Expected melter block entity at " + pos);
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

    @Override
    public MachineMasteryHost masteryHost() {
        return melter;
    }

    @Override
    public double masteryAttribute(MachineStat stat) {
        return MasteryMenuSupport.attribute(data, DATA_MACHINE_PROGRESSION_START, stat);
    }

    @Override
    public MachineMasteryFamily masteryFamily() {
        return MachineMasteryFamily.MELTER;
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
}
