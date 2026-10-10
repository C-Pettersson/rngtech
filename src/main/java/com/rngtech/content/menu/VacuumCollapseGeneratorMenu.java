package com.rngtech.content.menu;

import com.rngtech.content.blockentity.VacuumCollapseGeneratorBlockEntity;
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

public class VacuumCollapseGeneratorMenu extends AbstractContainerMenu implements StatBreakdownMenu {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;
    public static final int TAB_REFINEMENT = 3;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_PROCESSING_TICKS = 1;
    private static final int DATA_ENERGY_LOW = 2;
    private static final int DATA_ENERGY_HIGH = 3;
    private static final int DATA_ENERGY_CAPACITY_LOW = 4;
    private static final int DATA_ENERGY_CAPACITY_HIGH = 5;
    private static final int DATA_ENERGY_PER_TICK_LOW = 6;
    private static final int DATA_ENERGY_PER_TICK_HIGH = 7;
    private static final int DATA_MAX_OUTPUT = 8;
    private static final int DATA_RECIPE_ENERGY_LOW = 9;
    private static final int DATA_RECIPE_ENERGY_HIGH = 10;
    private static final int DATA_PROCESSING_LEVEL = 11;
    private static final int DATA_STATUS = 12;
    private static final int DATA_INSTABILITY = 13;
    private static final int DATA_ENERGY_GENERATION = 14;
    private static final int DATA_ENERGY_CAPACITY_STAT = 15;
    private static final int DATA_EFFICIENCY = 16;
    private static final int DATA_PROCESSING_SPEED = 17;
    private static final int DATA_STABILITY = 18;
    private static final int DATA_REFINEMENT_POTENTIAL = 19;
    private static final int DATA_FLAT_ENERGY_GENERATION = 20;
    private static final int DATA_BASE_ENERGY_GENERATION = 21;
    private static final int DATA_COUNT = 22;
    private static final int STAT_SCALE = 100;
    private static final int INPUT_SLOT = 0;
    private static final int RESIDUE_SLOT = 1;
    private static final int VOID_CHAMBER_SLOT = RESIDUE_SLOT + 1;
    private static final int COLLAPSE_NOZZLE_SLOT = VOID_CHAMBER_SLOT + 1;
    private static final int DIMENSIONAL_STABILIZER_SLOT = COLLAPSE_NOZZLE_SLOT + 1;
    private static final int REFINEMENT_CONSUMABLE_SLOT = DIMENSIONAL_STABILIZER_SLOT + 1;
    private static final int REFINEMENT_TARGET_SLOT = REFINEMENT_CONSUMABLE_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = REFINEMENT_TARGET_SLOT + 1;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final VacuumCollapseGeneratorBlockEntity generator;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private int selectedTab = TAB_PROCESSING;

    public VacuumCollapseGeneratorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public VacuumCollapseGeneratorMenu(
            int containerId,
            Inventory playerInventory,
            VacuumCollapseGeneratorBlockEntity generator,
            ContainerData data
    ) {
        this(containerId, playerInventory, generator, data, generator.machineTraits());
    }

    private VacuumCollapseGeneratorMenu(
            int containerId,
            Inventory playerInventory,
            VacuumCollapseGeneratorBlockEntity generator,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.VACUUM_COLLAPSE_GENERATOR.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(generator.getLevel(), generator.getBlockPos());
        this.data = data;
        this.generator = generator;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, ModBlocks.VACUUM_COLLAPSE_GENERATOR.get(), machineTraits);

        ItemStackHandler processInventory = generator.getProcessInventory();
        addSlot(new TabbedSlot(processInventory, VacuumCollapseGeneratorBlockEntity.SLOT_INPUT, 43, 55, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(processInventory, VacuumCollapseGeneratorBlockEntity.SLOT_RESIDUE, 195, 55, () -> selectedTab == TAB_PROCESSING));

        ItemStackHandler gearInventory = generator.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, VacuumCollapseGeneratorBlockEntity.SLOT_VOID_CHAMBER, 25, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, VacuumCollapseGeneratorBlockEntity.SLOT_COLLAPSE_NOZZLE, 75, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, VacuumCollapseGeneratorBlockEntity.SLOT_DIMENSIONAL_STABILIZER, 125, 48, () -> selectedTab == TAB_GEAR));

        addSlot(RefinementMenuSupport.consumableSlot(
                generator.getRefinementInventory(),
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

    public float processingProgress() {
        int total = data.get(DATA_PROCESSING_TICKS);
        return total <= 0 ? 0.0F : Mth.clamp((float) data.get(DATA_PROGRESS) / (float) total, 0.0F, 1.0F);
    }

    public float energyProgress() {
        long capacity = energyCapacity();
        return capacity <= 0L ? 0.0F : Mth.clamp((float) energy() / (float) capacity, 0.0F, 1.0F);
    }

    public int progress() {
        return data.get(DATA_PROGRESS);
    }

    public int processingTicks() {
        return data.get(DATA_PROCESSING_TICKS);
    }

    public long energy() {
        return composeLong(DATA_ENERGY_LOW, DATA_ENERGY_HIGH);
    }

    public long energyCapacity() {
        return composeLong(DATA_ENERGY_CAPACITY_LOW, DATA_ENERGY_CAPACITY_HIGH);
    }

    public long energyPerTick() {
        return composeLong(DATA_ENERGY_PER_TICK_LOW, DATA_ENERGY_PER_TICK_HIGH);
    }

    public int maxOutput() {
        return data.get(DATA_MAX_OUTPUT);
    }

    public long recipeEnergy() {
        return composeLong(DATA_RECIPE_ENERGY_LOW, DATA_RECIPE_ENERGY_HIGH);
    }

    public int processingLevel() {
        return data.get(DATA_PROCESSING_LEVEL);
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    public double instability() {
        return data.get(DATA_INSTABILITY) / (double) STAT_SCALE;
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

    public static int flatEnergyGenerationDataIndex() {
        return DATA_FLAT_ENERGY_GENERATION;
    }

    public static int baseEnergyGenerationDataIndex() {
        return DATA_BASE_ENERGY_GENERATION;
    }

    public static int energyCapacityDataIndex() {
        return DATA_ENERGY_CAPACITY_STAT;
    }

    public static int efficiencyDataIndex() {
        return DATA_EFFICIENCY;
    }

    public static int processingSpeedDataIndex() {
        return DATA_PROCESSING_SPEED;
    }

    public static int processingLevelDataIndex() {
        return DATA_PROCESSING_LEVEL;
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
        return RefinementMenuSupport.applyToMachine(player, generator, refinementTarget, ModBlocks.VACUUM_COLLAPSE_GENERATOR.get());
    }

    @Override
    public MachineStatAccumulator breakdownStats() {
        return generator.effectiveStats();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.VACUUM_COLLAPSE_GENERATOR.get());
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
        } else if (generator.isVoidChamber(stack)) {
            if (!moveItemStackTo(stack, VOID_CHAMBER_SLOT, VOID_CHAMBER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (generator.isCollapseNozzle(stack)) {
            if (!moveItemStackTo(stack, COLLAPSE_NOZZLE_SLOT, COLLAPSE_NOZZLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (generator.isDimensionalStabilizer(stack)) {
            if (!moveItemStackTo(stack, DIMENSIONAL_STABILIZER_SLOT, DIMENSIONAL_STABILIZER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (generator.isKnownCatalyst(stack)) {
            if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
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
                addSlot(new TabbedInventorySlot(playerInventory, column + row * 9 + 9, 39 + column * 18, 116 + row * 18, () -> selectedTab != TAB_STATS));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(playerInventory, column, 39 + column * 18, 174, () -> selectedTab != TAB_STATS));
        }
    }

    private long composeLong(int lowIndex, int highIndex) {
        return Integer.toUnsignedLong(data.get(lowIndex)) | ((long) data.get(highIndex) << 32);
    }

    private static VacuumCollapseGeneratorBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof VacuumCollapseGeneratorBlockEntity generator) {
            return generator;
        }
        throw new IllegalStateException("Expected vacuum collapse generator block entity at " + pos);
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
