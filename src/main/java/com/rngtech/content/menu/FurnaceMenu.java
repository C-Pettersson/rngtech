package com.rngtech.content.menu;

import com.rngtech.content.block.FurnaceBlock;
import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.progression.FurnacePassiveTree;
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

public class FurnaceMenu extends AbstractContainerMenu implements MasteryMenuView<MegaPassiveNode>, StatBreakdownMenu {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_CONFIGURATION = 2;
    public static final int TAB_REFINEMENT = 3;
    public static final int TAB_MASTERY = 4;

    private static final int MAX_PROCESSING_SLOTS = FurnaceBlockEntity.MAX_PROCESSING_SLOTS;
    private static final int MAX_GEAR_SLOTS = FurnaceBlockEntity.MAX_GEAR_SLOTS;
    private static final int DATA_PROGRESS_START = 0;
    private static final int DATA_PROCESSING_TICKS_START = DATA_PROGRESS_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_ENERGY_PER_TICK_START = DATA_PROCESSING_TICKS_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_ENERGY_PER_CRAFT_START = DATA_ENERGY_PER_TICK_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_BURN_TIME = DATA_ENERGY_PER_CRAFT_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_TOTAL_BURN_TIME = DATA_BURN_TIME + 1;
    private static final int DATA_PROCESSING_SPEED = DATA_TOTAL_BURN_TIME + 1;
    private static final int DATA_EFFICIENCY = DATA_PROCESSING_SPEED + 1;
    private static final int DATA_HEAT_TRANSFER = DATA_EFFICIENCY + 1;
    private static final int DATA_MAX_TEMPERATURE = DATA_HEAT_TRANSFER + 1;
    private static final int DATA_TEMPERATURE_STABILITY = DATA_MAX_TEMPERATURE + 1;
    private static final int DATA_FUEL_EFFICIENCY = DATA_TEMPERATURE_STABILITY + 1;
    private static final int DATA_REFINEMENT_POTENTIAL = DATA_FUEL_EFFICIENCY + 1;
    private static final int DATA_ENERGY = DATA_REFINEMENT_POTENTIAL + 1;
    private static final int DATA_ENERGY_CAPACITY = DATA_ENERGY + 1;
    private static final int DATA_ENERGY_USAGE = DATA_ENERGY_CAPACITY + 1;
    private static final int DATA_ENERGY_CAPACITY_STAT = DATA_ENERGY_USAGE + 1;
    private static final int DATA_ENERGY_TRANSFER = DATA_ENERGY_CAPACITY_STAT + 1;
    private static final int DATA_WARMUP_TIME = DATA_ENERGY_TRANSFER + 1;
    private static final int DATA_COOLING_RATE = DATA_WARMUP_TIME + 1;
    private static final int DATA_OVERHEAT_TOLERANCE = DATA_COOLING_RATE + 1;
    private static final int DATA_INPUT_SLOTS = DATA_OVERHEAT_TOLERANCE + 1;
    private static final int DATA_GEAR_SLOTS = DATA_INPUT_SLOTS + 1;
    private static final int DATA_MIN_TEMPERATURE = DATA_GEAR_SLOTS + 1;
    private static final int DATA_STATUS = DATA_MIN_TEMPERATURE + 1;
    private static final int DATA_HEAT_START = DATA_STATUS + 1;
    private static final int DATA_TARGET_TEMPERATURE_START = DATA_HEAT_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_SAFE_MAX_TEMPERATURE_START = DATA_TARGET_TEMPERATURE_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_OVERHEAT_TEMPERATURE_START = DATA_SAFE_MAX_TEMPERATURE_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_FAILURE_STRAIN_START = DATA_OVERHEAT_TEMPERATURE_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_FAILURE_ENABLED_START = DATA_FAILURE_STRAIN_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_POWER_SENSITIVE_START = DATA_FAILURE_ENABLED_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_LEDGER_START = DATA_POWER_SENSITIVE_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_MACHINE_PROGRESSION_START = DATA_LEDGER_START + MAX_PROCESSING_SLOTS;
    private static final int DATA_COUNT = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int STAT_SCALE = 100;
    private static final int MACHINE_SLOT_COUNT = FurnaceBlockEntity.SLOT_COUNT;
    private static final int GEAR_SLOT_START = MACHINE_SLOT_COUNT;
    private static final int GEAR_SLOT_END = GEAR_SLOT_START + MAX_GEAR_SLOTS;
    private static final int REFINEMENT_CONSUMABLE_SLOT = GEAR_SLOT_END;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PROCESSING_TARGET_SLOT = REFINEMENT_TARGET_SLOT + 1;
    private static final int MENU_MACHINE_SLOT_COUNT = PROCESSING_TARGET_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = MENU_MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;
    private static final int[] PROCESS_INPUT_X = {56, 38, 56, 38};
    private static final int[] PROCESS_INPUT_Y = {42, 42, 60, 60};
    private static final int[] PROCESS_OUTPUT_X = {164, 182, 164, 182};
    private static final int[] PROCESS_OUTPUT_Y = {42, 42, 60, 60};
    private static final int[] GEAR_X = {92, 116, 140, 164};
    private static final int[] GEAR_Y = {66, 66, 66, 66};
    private static final int FUEL_SLOT_X = 56;
    private static final int FUEL_SLOT_Y = 84;
    private static final int BATTERY_SLOT_Y = 66;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final FurnaceBlockEntity furnace;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private final PassiveProgressionView passiveProgressionView =
            MasteryMenuSupport.progressionView(this::hasPassiveNodeIndex, this::machineLevel, this::unspentPassivePoints, MachineMasteryFamily.FURNACE.startNodeId());
    private int selectedTab = TAB_PROCESSING;

    public FurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public FurnaceMenu(int containerId, Inventory playerInventory, FurnaceBlockEntity furnace, ContainerData data) {
        this(containerId, playerInventory, furnace, data, furnace.machineTraits());
    }

    private FurnaceMenu(
            int containerId,
            Inventory playerInventory,
            FurnaceBlockEntity furnace,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.FURNACE.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos());
        this.data = data;
        this.furnace = furnace;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, furnace.getBlockState().getBlock(), machineTraits);

        ItemStackHandler inventory = furnace.getInventory();
        for (int lane = 0; lane < MAX_PROCESSING_SLOTS; lane++) {
            final int slotLane = lane;
            addSlot(new TabbedSlot(
                    inventory,
                    FurnaceBlockEntity.SLOT_INPUT_START + lane,
                    PROCESS_INPUT_X[lane],
                    PROCESS_INPUT_Y[lane],
                    () -> selectedTab == TAB_PROCESSING && slotLane < activeProcessingSlots()
            ));
        }
        addSlot(new TabbedSlot(
                inventory,
                FurnaceBlockEntity.SLOT_FUEL,
                FUEL_SLOT_X,
                furnace.isElectric() ? BATTERY_SLOT_Y : FUEL_SLOT_Y,
                () -> selectedTab == (furnace.isElectric() ? TAB_GEAR : TAB_PROCESSING)
        ));
        for (int lane = 0; lane < MAX_PROCESSING_SLOTS; lane++) {
            final int slotLane = lane;
            addSlot(new TabbedSlot(
                    inventory,
                    FurnaceBlockEntity.SLOT_OUTPUT_START + lane,
                    PROCESS_OUTPUT_X[lane],
                    PROCESS_OUTPUT_Y[lane],
                    () -> selectedTab == TAB_PROCESSING && slotLane < activeProcessingSlots()
            ));
        }
        for (int slot = 0; slot < MAX_GEAR_SLOTS; slot++) {
            final int gearSlot = slot;
            addSlot(new TabbedSlot(
                    furnace.getGearInventory(),
                    slot,
                    GEAR_X[slot],
                    GEAR_Y[slot],
                    () -> selectedTab == TAB_GEAR && gearSlot < activeGearSlots()
            ));
        }
        addSlot(RefinementMenuSupport.consumableSlot(
                furnace.getRefinementInventory(),
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
        return processingProgress(0);
    }

    public float processingProgress(int lane) {
        int ticks = processingTicks(lane);
        return ticks <= 0 ? 0.0F : Mth.clamp((float) progress(lane) / (float) ticks, 0.0F, 1.0F);
    }

    public float fuelProgress() {
        int total = data.get(DATA_TOTAL_BURN_TIME);
        return total <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_BURN_TIME) / (float) total, 0.0F, 1.0F);
    }

    public float energyProgress() {
        int capacity = data.get(DATA_ENERGY_CAPACITY);
        return capacity <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_ENERGY) / (float) capacity, 0.0F, 1.0F);
    }

    public float heatProgress() {
        int overheat = overheatTemperature();
        return overheat <= 0 ? 0.0F : Mth.clamp((float) heat() / (float) overheat, 0.0F, 1.0F);
    }

    public float failureProgress() {
        return Mth.clamp(failureStrain() / 100.0F, 0.0F, 1.0F);
    }

    public boolean isElectric() {
        return furnace.isElectric();
    }

    public boolean usesHeatCores() {
        return furnace.usesHeatCores();
    }

    public int activeProcessingSlots() {
        return Math.max(1, Math.min(MAX_PROCESSING_SLOTS, (int) Math.floor(statValue(DATA_INPUT_SLOTS))));
    }

    public int activeGearSlots() {
        return Math.max(1, Math.min(MAX_GEAR_SLOTS, data.get(DATA_GEAR_SLOTS)));
    }

    public int progress() {
        return progress(0);
    }

    public int progress(int lane) {
        return data.get(DATA_PROGRESS_START + Mth.clamp(lane, 0, MAX_PROCESSING_SLOTS - 1));
    }

    public int processingTicks() {
        return processingTicks(0);
    }

    public int processingTicks(int lane) {
        return data.get(DATA_PROCESSING_TICKS_START + Mth.clamp(lane, 0, MAX_PROCESSING_SLOTS - 1));
    }

    public int energyPerTick(int lane) {
        return data.get(DATA_ENERGY_PER_TICK_START + Mth.clamp(lane, 0, MAX_PROCESSING_SLOTS - 1));
    }

    public int energyPerCraft(int lane) {
        return data.get(DATA_ENERGY_PER_CRAFT_START + Mth.clamp(lane, 0, MAX_PROCESSING_SLOTS - 1));
    }

    public int totalEnergyPerTick() {
        int total = 0;
        for (int lane = 0; lane < activeProcessingSlots(); lane++) {
            total += energyPerTick(lane);
        }
        return total;
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

    public int minimumTemperature() {
        return data.get(DATA_MIN_TEMPERATURE);
    }

    public int heat() {
        int heat = 0;
        for (int lane = 0; lane < activeProcessingSlots(); lane++) {
            heat = Math.max(heat, data.get(DATA_HEAT_START + lane));
        }
        return heat;
    }

    public int targetTemperature() {
        int targetTemperature = 0;
        for (int lane = 0; lane < activeProcessingSlots(); lane++) {
            targetTemperature = Math.max(targetTemperature, data.get(DATA_TARGET_TEMPERATURE_START + lane));
        }
        return targetTemperature;
    }

    public int safeMaximumTemperature() {
        int safeMaximumTemperature = 0;
        for (int lane = 0; lane < activeProcessingSlots(); lane++) {
            safeMaximumTemperature = Math.max(safeMaximumTemperature, data.get(DATA_SAFE_MAX_TEMPERATURE_START + lane));
        }
        return safeMaximumTemperature;
    }

    public int overheatTemperature() {
        int overheatTemperature = 0;
        for (int lane = 0; lane < activeProcessingSlots(); lane++) {
            overheatTemperature = Math.max(overheatTemperature, data.get(DATA_OVERHEAT_TEMPERATURE_START + lane));
        }
        return overheatTemperature;
    }

    public int failureStrain() {
        int failureStrain = 0;
        for (int lane = 0; lane < activeProcessingSlots(); lane++) {
            failureStrain = Math.max(failureStrain, data.get(DATA_FAILURE_STRAIN_START + lane));
        }
        return failureStrain;
    }

    public boolean hasFailureRecipe() {
        for (int lane = 0; lane < activeProcessingSlots(); lane++) {
            if (data.get(DATA_FAILURE_ENABLED_START + lane) > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean powerSensitiveActive() {
        for (int lane = 0; lane < activeProcessingSlots(); lane++) {
            if (data.get(DATA_POWER_SENSITIVE_START + lane) > 0) {
                return true;
            }
        }
        return false;
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    /** The lane's Bloom Ledger progress toward its next item, from 0 to 1, or -1 when its smelt does not feed the ledger. */
    public float ledgerProgress(int lane) {
        int value = data.get(DATA_LEDGER_START + lane);
        return value < 0 ? -1.0F : Mth.clamp(value / 1000.0F, 0.0F, 1.0F);
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
        return FurnacePassiveTree.TREE.canUnlock(node, passiveProgressionView);
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

    public static int heatTransferDataIndex() {
        return DATA_HEAT_TRANSFER;
    }

    public static int maxTemperatureDataIndex() {
        return DATA_MAX_TEMPERATURE;
    }

    public static int temperatureStabilityDataIndex() {
        return DATA_TEMPERATURE_STABILITY;
    }

    public static int fuelEfficiencyDataIndex() {
        return DATA_FUEL_EFFICIENCY;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
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

    public static int warmupTimeDataIndex() {
        return DATA_WARMUP_TIME;
    }

    public static int coolingRateDataIndex() {
        return DATA_COOLING_RATE;
    }

    public static int overheatToleranceDataIndex() {
        return DATA_OVERHEAT_TOLERANCE;
    }

    public static int inputSlotsDataIndex() {
        return DATA_INPUT_SLOTS;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        MegaPassiveNode passiveNode = MegaPassiveTree.byButtonId(id);
        if (passiveNode != null) {
            if (player.level().isClientSide) {
                return true;
            }
            return furnace.unlockPassiveNode(passiveNode);
        }
        if (id == RefinementMenuSupport.BUTTON_APPLY) {
            if (player.level().isClientSide) {
                return true;
            }
            return RefinementMenuSupport.applyToMachine(player, furnace, refinementTarget, furnace.getBlockState().getBlock());
        }
        return false;
    }

    @Override
    public MachineStatAccumulator breakdownStats() {
        return furnace.effectiveStats();
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof FurnaceBlock
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

        if (index >= FurnaceBlockEntity.SLOT_OUTPUT_START
                && index < FurnaceBlockEntity.SLOT_OUTPUT_START + MAX_PROCESSING_SLOTS) {
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
        } else if (index >= GEAR_SLOT_START && index < GEAR_SLOT_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_START) {
            return ItemStack.EMPTY;
        } else if (RefinementMenuSupport.isConsumable(stack)) {
            if (!moveItemStackTo(stack, REFINEMENT_CONSUMABLE_SLOT, REFINEMENT_CONSUMABLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (FurnaceBlockEntity.isGearComponent(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START, GEAR_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (furnace.isElectric() && FurnaceBlockEntity.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, FurnaceBlockEntity.SLOT_FUEL, FurnaceBlockEntity.SLOT_FUEL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!furnace.isElectric() && FurnaceBlockEntity.isFuel(stack)) {
            if (!moveItemStackTo(stack, FurnaceBlockEntity.SLOT_FUEL, FurnaceBlockEntity.SLOT_FUEL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!furnace.isSmeltable(stack) || !moveItemStackTo(
                stack,
                FurnaceBlockEntity.SLOT_INPUT_START,
                FurnaceBlockEntity.SLOT_INPUT_START + MAX_PROCESSING_SLOTS,
                false
        )) {
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

    private static FurnaceBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof FurnaceBlockEntity furnace) {
            return furnace;
        }
        throw new IllegalStateException("Expected furnace block entity at " + pos);
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
    @Override public MachineMasteryHost masteryHost() { return furnace; }
    @Override public double masteryAttribute(MachineStat stat) { return MasteryMenuSupport.attribute(data, DATA_MACHINE_PROGRESSION_START, stat); }
    @Override public boolean masterySupports(MachineStat stat) {
        return masteryFamily().supports(stat) && switch (stat) {
            case FUEL_DURATION, FUEL_EFFICIENCY -> !isElectric();
            case ENERGY_USAGE, ENERGY_CAPACITY, ENERGY_CAPACITY_FLAT -> isElectric();
            default -> true;
        };
    }
    @Override public boolean masterySupportsBehavior(String behavior) {
        return masteryFamily().supportsBehavior(behavior) && (!behavior.equals("CLOSED_LOOP_RECUPERATOR") || isElectric());
    }
    @Override public MachineMasteryFamily masteryFamily() { return MachineMasteryFamily.FURNACE; }
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
