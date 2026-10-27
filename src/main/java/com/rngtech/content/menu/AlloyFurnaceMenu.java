package com.rngtech.content.menu;

import com.rngtech.content.block.AlloyFurnaceBlock;
import com.rngtech.content.blockentity.AlloyFurnaceBlockEntity;
import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.item.JobChipItem;
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

public class AlloyFurnaceMenu extends AbstractContainerMenu implements MasteryMenuView<MegaPassiveNode>, StatBreakdownMenu, WorkOrderMenuView {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;
    public static final int TAB_MASTERY = 4;
    public static final int TAB_ORDERS = 5;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_ENERGY_PER_TICK = 4;
    private static final int DATA_MIN_TEMPERATURE = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_PROCESSING_SPEED = 7;
    private static final int DATA_ENERGY_USAGE = 8;
    private static final int DATA_ENERGY_CAPACITY_STAT = 9;
    private static final int DATA_ENERGY_TRANSFER = 10;
    private static final int DATA_HEAT_TRANSFER = 11;
    private static final int DATA_MAX_TEMPERATURE = 12;
    private static final int DATA_TEMPERATURE_STABILITY = 13;
    private static final int DATA_STABILITY = 14;
    private static final int DATA_INPUT_SLOTS = 15;
    private static final int DATA_REFINEMENT_POTENTIAL = 16;
    private static final int DATA_CURRENT_TEMPERATURE = 17;
    private static final int DATA_TARGET_TEMPERATURE = 18;
    private static final int DATA_SAFE_MAX_TEMPERATURE = 19;
    private static final int DATA_OVERHEAT_TEMPERATURE = 20;
    private static final int DATA_FAILURE_STRAIN = 21;
    private static final int DATA_FAILURE_ENABLED = 22;
    private static final int DATA_POWER_SENSITIVE = 23;
    private static final int DATA_THERMAL_MASS = 24;
    private static final int DATA_COOLING_RATE = 25;
    private static final int DATA_OVERHEAT_TOLERANCE = 26;
    private static final int DATA_LEDGER = 27;
    private static final int DATA_LEDGER_BLEND = 28;
    private static final int DATA_BATCH_SIZE = 29;
    private static final int DATA_HEAT_SLIDER = 30;
    private static final int DATA_SETPOINT = 31;
    private static final int DATA_THERMAL_CONDUCTIVITY = 32;
    private static final int DATA_HOLDING_ENERGY = DATA_THERMAL_CONDUCTIVITY + 1;
    private static final int DATA_HEATING_ENERGY = DATA_HOLDING_ENERGY + 1;
    private static final int DATA_HEAT_FACTOR = DATA_HEATING_ENERGY + 1;
    private static final int DATA_WOBBLE = DATA_HEAT_FACTOR + 1;
    private static final int DATA_HEAT_REACH = DATA_WOBBLE + 1;
    private static final int DATA_HEAT_MAXIMUM = DATA_HEAT_REACH + 1;
    private static final int DATA_INPUT_ENERGY = DATA_HEAT_MAXIMUM + 1;
    private static final int DATA_NITROGEN = DATA_INPUT_ENERGY + 1;
    private static final int DATA_NITROGEN_USE = DATA_NITROGEN + 1;
    private static final int DATA_NITROGEN_BONUS = DATA_NITROGEN_USE + 1;
    private static final int DATA_MACHINE_PROGRESSION_START = DATA_NITROGEN_BONUS + 1;
    private static final int DATA_OUTPUT_PER_MINUTE = DATA_MACHINE_PROGRESSION_START + MasteryMenuSupport.FIELD_COUNT;
    private static final int DATA_COUNT = DATA_OUTPUT_PER_MINUTE + 1;
    private static final int STAT_SCALE = 100;
    private static final int PROCESS_SLOT_COUNT = AlloyFurnaceBlockEntity.PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_START = PROCESS_SLOT_COUNT;
    private static final int GEAR_SLOT_END = GEAR_SLOT_START + AlloyFurnaceBlockEntity.GEAR_SLOT_COUNT;
    private static final int REFINEMENT_CONSUMABLE_SLOT = GEAR_SLOT_END;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int MENU_MACHINE_SLOT_COUNT = REFINEMENT_TARGET_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = MENU_MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;
    private static final int[] INPUT_X = {50, 70, 50, 70};
    private static final int[] INPUT_Y = {29, 29, 49, 49};

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final AlloyFurnaceBlockEntity furnace;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private final PassiveProgressionView passiveProgressionView = MasteryMenuSupport.progressionView(
            this::hasPassiveNodeIndex,
            this::machineLevel,
            this::unspentPassivePoints,
            MachineMasteryFamily.ALLOY_FURNACE.startNodeId()
    );
    private final DataSlot ordersShown;
    private int selectedTab = TAB_PROCESSING;

    public AlloyFurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public AlloyFurnaceMenu(int containerId, Inventory playerInventory, AlloyFurnaceBlockEntity furnace, ContainerData data) {
        this(containerId, playerInventory, furnace, data, furnace.machineTraits());
    }

    private AlloyFurnaceMenu(
            int containerId,
            Inventory playerInventory,
            AlloyFurnaceBlockEntity furnace,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.ALLOY_FURNACE.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.access = ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos());
        this.data = data;
        this.furnace = furnace;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, furnace.getBlockState().getBlock(), machineTraits);

        ItemStackHandler processInventory = furnace.getProcessInventory();
        for (int slot = 0; slot < AlloyFurnaceBlockEntity.MAX_INPUT_SLOTS; slot++) {
            final int inputSlot = slot;
            addSlot(new TabbedSlot(
                    processInventory,
                    AlloyFurnaceBlockEntity.SLOT_INPUT_START + slot,
                    INPUT_X[slot],
                    INPUT_Y[slot],
                    () -> selectedTab == TAB_PROCESSING && inputSlot < activeInputSlots()
            ));
        }
        addSlot(new TabbedSlot(
                processInventory,
                AlloyFurnaceBlockEntity.SLOT_OUTPUT,
                204,
                42,
                () -> selectedTab == TAB_PROCESSING
        ));

        ItemStackHandler gearInventory = furnace.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, AlloyFurnaceBlockEntity.SLOT_HEAT_CORE, 44, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, AlloyFurnaceBlockEntity.SLOT_CRUCIBLE, 92, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, AlloyFurnaceBlockEntity.SLOT_BATTERY_CELL, 140, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, AlloyFurnaceBlockEntity.SLOT_SERVO, 188, 48, () -> selectedTab == TAB_GEAR));

        addSlot(RefinementMenuSupport.consumableSlot(
                furnace.getRefinementInventory(),
                0,
                RefinementMenuSupport.REFINEMENT_CONSUMABLE_SLOT_X,
                RefinementMenuSupport.REFINEMENT_SLOT_Y,
                () -> selectedTab == TAB_REFINEMENT
        ));
        addSlot(RefinementMenuSupport.targetDisplaySlot(refinementTarget, RefinementMenuSupport.REFINEMENT_TARGET_SLOT_X, RefinementMenuSupport.REFINEMENT_SLOT_Y, () -> selectedTab == TAB_REFINEMENT));

        addPlayerInventory(playerInventory);
        IItemHandler bays = playerInventory.player.level().isClientSide ? DriveBays.client(this) : new DriveBays(furnace);
        for (int bay = 0; bay < MachineWorkOrders.SLOTS; bay++) {
            addSlot(new DriveBaySlot(bays, bay, () -> selectedTab == TAB_ORDERS));
        }
        addDataSlots(data);
        ordersShown = addDataSlot(WorkOrderDiscovery.shownSlot(playerInventory.player, furnace));
    }

    @Override public WorkOrderHost workOrderHost() { return furnace; }
    @Override public boolean workOrdersShown() { return ordersShown.get() != 0; }
    @Override public String workOrderFamily() { return WorkOrderFamilies.of(masteryFamily()); }
    @Override public int workOrderStage() { return ascendancyEntryStage(); }
    @Override public int workOrderMasteryLevel() { return machineLevel(); }
    @Override public WorkOrderMenuSupport.View workOrderView() {
        return MasteryMenuSupport.workOrders(data, DATA_MACHINE_PROGRESSION_START);
    }
    @Override public ItemStack workOrderChip(int bay) {
        return bay >= 0 && bay < MachineWorkOrders.SLOTS ? getSlot(HOTBAR_END + bay).getItem() : ItemStack.EMPTY;
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_STATS -> TAB_STATS;
            case TAB_REFINEMENT -> TAB_REFINEMENT;
            case TAB_MASTERY -> TAB_MASTERY;
            case TAB_ORDERS -> TAB_ORDERS;
            default -> TAB_PROCESSING;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    /** The flux or blend ledger's progress toward its next unit, from 0 to 1, or -1 when neither applies. */
    public float ledgerProgress() {
        int value = data.get(DATA_LEDGER);
        return value < 0 ? -1.0F : Math.min(1.0F, value / 1000.0F);
    }

    public boolean ledgerIsBlend() {
        return data.get(DATA_LEDGER_BLEND) != 0;
    }

    public float processingProgress() {
        if (instantFlash()) {
            return 1.0F;
        }
        int ticks = processingTicks();
        return ticks <= 0 ? 0.0F : Mth.clamp((float) progress() / (float) ticks, 0.0F, 1.0F);
    }

    public float energyProgress() {
        int capacity = energyCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp((float) energy() / (float) capacity, 0.0F, 1.0F);
    }

    public float heatProgress() {
        int overheat = overheatTemperature();
        return overheat <= 0 ? 0.0F : Mth.clamp((float) heat() / (float) overheat, 0.0F, 1.0F);
    }

    public float failureProgress() {
        return Mth.clamp(failureStrain() / 100.0F, 0.0F, 1.0F);
    }

    public int activeInputSlots() {
        return Math.max(1, Math.min(AlloyFurnaceBlockEntity.MAX_INPUT_SLOTS, data.get(DATA_INPUT_SLOTS)));
    }

    /** Whether the bar should flash for a job that just finished instantly. */
    public boolean instantFlash() {
        return data.get(DATA_PROGRESS) == BaseMachineBlockEntity.INSTANT_FLASH_PROGRESS;
    }

    public int progress() {
        return Math.max(0, data.get(DATA_PROGRESS));
    }

    /** Expected items per minute for the current cycle, in tenths. */
    public int outputPerMinuteTenths() {
        return data.get(DATA_OUTPUT_PER_MINUTE);
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

    public int minimumTemperature() {
        return data.get(DATA_MIN_TEMPERATURE);
    }

    /** The overheat slider, 0 to 100 percent of the band from the recipe target to Max Temperature. */
    public int heatSlider() {
        return data.get(DATA_HEAT_SLIDER);
    }

    public int setpoint() {
        return data.get(DATA_SETPOINT);
    }

    public int heat() {
        return data.get(DATA_CURRENT_TEMPERATURE);
    }

    public int targetTemperature() {
        return data.get(DATA_TARGET_TEMPERATURE);
    }

    public int safeMaximumTemperature() {
        return data.get(DATA_SAFE_MAX_TEMPERATURE);
    }

    public int nitrogen() {
        return data.get(DATA_NITROGEN);
    }

    public int nitrogenUse() {
        return data.get(DATA_NITROGEN_USE);
    }

    public int nitrogenBonusPercent() {
        return data.get(DATA_NITROGEN_BONUS);
    }

    public int overheatTemperature() {
        return data.get(DATA_OVERHEAT_TEMPERATURE);
    }

    public int failureStrain() {
        return data.get(DATA_FAILURE_STRAIN);
    }

    public boolean hasFailureRecipe() {
        return data.get(DATA_FAILURE_ENABLED) > 0;
    }

    public boolean powerSensitiveActive() {
        return data.get(DATA_POWER_SENSITIVE) > 0;
    }

    public int status() {
        return data.get(DATA_STATUS);
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

    @Override
    public MachineMasteryHost masteryHost() {
        return furnace;
    }

    @Override
    public MachineMasteryFamily masteryFamily() {
        return MachineMasteryFamily.ALLOY_FURNACE;
    }

    @Override
    public double masteryAttribute(MachineStat stat) {
        return MasteryMenuSupport.attribute(data, DATA_MACHINE_PROGRESSION_START, stat);
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
        if (dataIndex == DATA_INPUT_SLOTS) {
            return activeInputSlots();
        }
        return data.get(dataIndex) / (double) STAT_SCALE;
    }

    public static int processingSpeedDataIndex() {
        return DATA_PROCESSING_SPEED;
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

    public static int stabilityDataIndex() {
        return DATA_STABILITY;
    }

    public static int inputSlotsDataIndex() {
        return DATA_INPUT_SLOTS;
    }

    public static int thermalConductivityDataIndex() {
        return DATA_THERMAL_CONDUCTIVITY;
    }

    /** FE/t the heater spent last tick replacing heat lost through the walls. */
    public int holdingEnergy() {
        return data.get(DATA_HOLDING_ENERGY);
    }

    /** FE/t the heater spent last tick raising the chamber's temperature. */
    public int heatingEnergy() {
        return data.get(DATA_HEATING_ENERGY);
    }

    /** FE/t the input took last tick: the heat the recipe consumes. */
    public int inputEnergy() {
        return data.get(DATA_INPUT_ENERGY);
    }

    /** How fast the input takes heat at the current temperature, in percent. */
    public int heatFactor() {
        return data.get(DATA_HEAT_FACTOR);
    }

    /** How far the working temperature swings around the lane temperature, in degrees. */
    public int wobble() {
        return data.get(DATA_WOBBLE);
    }

    /** The highest setpoint the slider reaches for the current recipe: its 100%. */
    public int heatReach() {
        return data.get(DATA_HEAT_REACH);
    }

    /** The machine's Max Temperature, the top of the slider track. */
    public int heatMaximum() {
        return data.get(DATA_HEAT_MAXIMUM);
    }

    public static int batchSizeDataIndex() {
        return DATA_BATCH_SIZE;
    }

    public static int refinementPotentialDataIndex() {
        return DATA_REFINEMENT_POTENTIAL;
    }

    public static int warmupTimeDataIndex() {
        return DATA_THERMAL_MASS;
    }

    public static int coolingRateDataIndex() {
        return DATA_COOLING_RATE;
    }

    public static int overheatToleranceDataIndex() {
        return DATA_OVERHEAT_TOLERANCE;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= WorkOrderMenuSupport.BUTTON_SELECT_TAB && id <= WorkOrderMenuSupport.BUTTON_SELECT_TAB + TAB_ORDERS) {
            selectTab(id - WorkOrderMenuSupport.BUTTON_SELECT_TAB);
            return true;
        }
        if (HeatSliderSupport.handle(player, id, furnace::setHeatSlider)) {
            return true;
        }
        MegaPassiveNode passiveNode = MegaPassiveTree.byButtonId(id);
        if (passiveNode != null) {
            if (player.level().isClientSide) {
                return true;
            }
            return furnace.unlockPassiveNode(passiveNode);
        }
        if (id != RefinementMenuSupport.BUTTON_APPLY) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        return RefinementMenuSupport.applyToMachine(player, furnace, refinementTarget, furnace.getBlockState().getBlock());
    }

    @Override
    public MachineStatAccumulator breakdownStats() {
        return furnace.effectiveStats();
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof AlloyFurnaceBlock
                        && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D,
                true
        );
    }

    /** Tops up a held stack first, then opens an empty slot only where automation could. */
    private boolean moveToAlloyInputs(ItemStack stack) {
        boolean moved = false;
        int end = AlloyFurnaceBlockEntity.SLOT_INPUT_START + activeInputSlots();
        for (int pass = 0; pass < 2; pass++) {
            for (int input = AlloyFurnaceBlockEntity.SLOT_INPUT_START; input < end && !stack.isEmpty(); input++) {
                if (slots.get(input).hasItem() == (pass == 0) && furnace.acceptsAutomatedInput(input, stack)) {
                    moved |= moveItemStackTo(stack, input, input + 1, false);
                }
            }
        }
        return moved;
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

        if (index >= HOTBAR_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (selectedTab == TAB_ORDERS && index >= PLAYER_INVENTORY_START && JobChipItem.isWorkOrder(stack)) {
            if (!moveItemStackTo(stack, HOTBAR_END, HOTBAR_END + MachineWorkOrders.SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index == AlloyFurnaceBlockEntity.SLOT_OUTPUT) {
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
        } else if (furnace.isHeatCore(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + AlloyFurnaceBlockEntity.SLOT_HEAT_CORE, GEAR_SLOT_START + AlloyFurnaceBlockEntity.SLOT_HEAT_CORE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (furnace.isAlloyCrucible(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + AlloyFurnaceBlockEntity.SLOT_CRUCIBLE, GEAR_SLOT_START + AlloyFurnaceBlockEntity.SLOT_CRUCIBLE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (AlloyFurnaceBlockEntity.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + AlloyFurnaceBlockEntity.SLOT_BATTERY_CELL, GEAR_SLOT_START + AlloyFurnaceBlockEntity.SLOT_BATTERY_CELL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (furnace.isServo(stack)) {
            if (!moveItemStackTo(stack, GEAR_SLOT_START + AlloyFurnaceBlockEntity.SLOT_SERVO, GEAR_SLOT_START + AlloyFurnaceBlockEntity.SLOT_SERVO + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveToAlloyInputs(stack)) {
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
                        () -> selectedTab != TAB_STATS && selectedTab != TAB_MASTERY
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(
                    playerInventory,
                    column,
                    39 + column * 18,
                    174,
                    () -> selectedTab != TAB_STATS && selectedTab != TAB_MASTERY
            ));
        }
    }

    private static AlloyFurnaceBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof AlloyFurnaceBlockEntity furnace) {
            return furnace;
        }
        throw new IllegalStateException("Expected alloy furnace block entity at " + pos);
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
