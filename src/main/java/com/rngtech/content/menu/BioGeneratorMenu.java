package com.rngtech.content.menu;

import com.rngtech.content.blockentity.BioGeneratorBlockEntity;
import com.rngtech.content.registry.ModBlocks;
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

public class BioGeneratorMenu extends AbstractContainerMenu {
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
    private static final int DATA_RECIPE_ENERGY = 6;
    private static final int DATA_STATUS = 7;
    private static final int DATA_ENERGY_GENERATION = 8;
    private static final int DATA_ENERGY_CAPACITY_STAT = 9;
    private static final int DATA_ENERGY_TRANSFER = 10;
    private static final int DATA_EFFICIENCY = 11;
    private static final int DATA_FUEL_EFFICIENCY = 12;
    private static final int DATA_POTATO_POWER = 13;
    private static final int DATA_CARROT_POWER = 14;
    private static final int DATA_BREAD_POWER = 15;
    private static final int DATA_SAPLING_POWER = 16;
    private static final int DATA_SEED_POWER = 17;
    private static final int DATA_PLANT_POWER = 18;
    private static final int DATA_ORGANIC_REAGENT_POWER = 19;
    private static final int DATA_COMPOSTED_BIOMASS_POWER = 20;
    private static final int DATA_ALGAE_POWER = 21;
    private static final int DATA_RICH_BIOMASS_POWER = 22;
    private static final int DATA_FUEL_DURATION = 23;
    private static final int DATA_REFINEMENT_POTENTIAL = 24;
    private static final int DATA_COUNT = 25;
    private static final int STAT_SCALE = 100;
    private static final int FUEL_SLOT = 0;
    private static final int BATTERY_CELL_SLOT = 1;
    private static final int BIO_CHAMBER_SLOT = 2;
    private static final int REFINEMENT_CONSUMABLE_SLOT = 3;
    private static final int REFINEMENT_TARGET_SLOT = 4;
    private static final int PLAYER_INVENTORY_START = 5;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final BioGeneratorBlockEntity generator;
    private final ItemStackHandler refinementTarget = new ItemStackHandler(1);
    private int selectedTab = TAB_PROCESSING;

    public BioGeneratorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT),
                MachineTraits.STREAM_CODEC.decode(extraData)
        );
    }

    public BioGeneratorMenu(int containerId, Inventory playerInventory, BioGeneratorBlockEntity generator, ContainerData data) {
        this(containerId, playerInventory, generator, data, generator.machineTraits());
    }

    private BioGeneratorMenu(
            int containerId,
            Inventory playerInventory,
            BioGeneratorBlockEntity generator,
            ContainerData data,
            MachineTraits machineTraits
    ) {
        super(ModMenus.BIO_GENERATOR.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(generator.getLevel(), generator.getBlockPos());
        this.data = data;
        this.generator = generator;
        RefinementMenuSupport.setMachineDisplay(refinementTarget, ModBlocks.BIO_GENERATOR.get().asItem(), machineTraits);

        addSlot(new TabbedSlot(generator.getFuelInventory(), BioGeneratorBlockEntity.SLOT_FUEL, 53, 55, () -> selectedTab == TAB_PROCESSING));

        ItemStackHandler gearInventory = generator.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, BioGeneratorBlockEntity.SLOT_BATTERY_CELL, 66, 48, () -> selectedTab == TAB_GEAR));
        addSlot(new TabbedSlot(gearInventory, BioGeneratorBlockEntity.SLOT_BIO_CHAMBER, 126, 48, () -> selectedTab == TAB_GEAR));

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

    public float burnProgress() {
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

    public int recipeEnergy() {
        return data.get(DATA_RECIPE_ENERGY);
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

    public static int energyCapacityStatDataIndex() {
        return DATA_ENERGY_CAPACITY_STAT;
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

    public static int potatoPowerDataIndex() {
        return DATA_POTATO_POWER;
    }

    public static int carrotPowerDataIndex() {
        return DATA_CARROT_POWER;
    }

    public static int breadPowerDataIndex() {
        return DATA_BREAD_POWER;
    }

    public static int saplingPowerDataIndex() {
        return DATA_SAPLING_POWER;
    }

    public static int seedPowerDataIndex() {
        return DATA_SEED_POWER;
    }

    public static int plantPowerDataIndex() {
        return DATA_PLANT_POWER;
    }

    public static int organicReagentPowerDataIndex() {
        return DATA_ORGANIC_REAGENT_POWER;
    }

    public static int compostedBiomassPowerDataIndex() {
        return DATA_COMPOSTED_BIOMASS_POWER;
    }

    public static int algaePowerDataIndex() {
        return DATA_ALGAE_POWER;
    }

    public static int richBiomassPowerDataIndex() {
        return DATA_RICH_BIOMASS_POWER;
    }

    public static int fuelDurationDataIndex() {
        return DATA_FUEL_DURATION;
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
        return RefinementMenuSupport.applyToMachine(player, generator, refinementTarget, ModBlocks.BIO_GENERATOR.get().asItem());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.BIO_GENERATOR.get());
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
        } else if (generator.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, BATTERY_CELL_SLOT, BATTERY_CELL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (generator.isBioChamber(stack)) {
            if (!moveItemStackTo(stack, BIO_CHAMBER_SLOT, BIO_CHAMBER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (generator.isAcceptedFuel(stack)) {
            if (!moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
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

    private static BioGeneratorBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof BioGeneratorBlockEntity generator) {
            return generator;
        }
        throw new IllegalStateException("Expected bio generator block entity at " + pos);
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
