package com.rngtech.content.menu;

import com.rngtech.content.blockentity.WoodenDehumidifierBlockEntity;
import com.rngtech.content.purge.FluidPurgeSupport;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class WoodenDehumidifierMenu extends AbstractContainerMenu {
    public static final int BUTTON_CYCLE_OUTPUT = 0;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_REQUIRED_TICKS = 1;
    private static final int DATA_WATER = 2;
    private static final int DATA_WATER_CAPACITY = 3;
    private static final int DATA_CONVERTED_OUTPUT = 4;
    private static final int DATA_CONVERTED_OUTPUT_CAPACITY = 5;
    private static final int DATA_CONVERTED_OUTPUT_FLUID = 6;
    private static final int DATA_FRAME_COUNT = 7;
    private static final int DATA_STRUCTURE_VALID = 8;
    private static final int DATA_HUMIDITY_BAND = 9;
    private static final int DATA_RAIN_BONUS = 10;
    private static final int DATA_SOLAR_ACCESS = 11;
    private static final int DATA_PRODUCTION_RATE = 12;
    private static final int DATA_SELECTED_OUTPUT = 13;
    private static final int DATA_STATUS = 14;
    private static final int DATA_COUNT = 15;

    private static final int PLAYER_INVENTORY_START = WoodenDehumidifierBlockEntity.SLOT_COUNT;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final WoodenDehumidifierBlockEntity dehumidifier;
    private final ContainerData data;

    public WoodenDehumidifierMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public WoodenDehumidifierMenu(
            int containerId,
            Inventory playerInventory,
            WoodenDehumidifierBlockEntity dehumidifier,
            ContainerData data
    ) {
        super(ModMenus.WOODEN_DEHUMIDIFIER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(dehumidifier.getLevel(), dehumidifier.getBlockPos());
        this.dehumidifier = dehumidifier;
        this.data = data;
        addDataSlots(data);

        ItemStackHandler inventory = dehumidifier.getInventory();
        addSlot(new SlotItemHandler(inventory, WoodenDehumidifierBlockEntity.SLOT_CONVERSION_INPUT, 61, 55));
        addSlot(new SlotItemHandler(inventory, WoodenDehumidifierBlockEntity.SLOT_EMPTY_CONTAINER, 177, 79));
        addSlot(new SlotItemHandler(inventory, WoodenDehumidifierBlockEntity.SLOT_FILLED_CONTAINER, 199, 79));

        addPlayerInventory(playerInventory);
    }

    public float conversionProgress() {
        int required = requiredTicks();
        return required <= 0 ? 0.0F : Mth.clamp(progressTicks() / (float) required, 0.0F, 1.0F);
    }

    public float waterProgress() {
        int capacity = waterCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp(waterAmount() / (float) capacity, 0.0F, 1.0F);
    }

    public float convertedOutputProgress() {
        int capacity = convertedOutputCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp(convertedOutputAmount() / (float) capacity, 0.0F, 1.0F);
    }

    public int progressTicks() {
        return data.get(DATA_PROGRESS);
    }

    public int requiredTicks() {
        return data.get(DATA_REQUIRED_TICKS);
    }

    public int waterAmount() {
        return data.get(DATA_WATER);
    }

    public int waterCapacity() {
        return data.get(DATA_WATER_CAPACITY);
    }

    public int convertedOutputAmount() {
        return data.get(DATA_CONVERTED_OUTPUT);
    }

    public int convertedOutputCapacity() {
        return data.get(DATA_CONVERTED_OUTPUT_CAPACITY);
    }

    public Fluid convertedOutputFluid() {
        return fluidById(data.get(DATA_CONVERTED_OUTPUT_FLUID));
    }

    public int frameCount() {
        return data.get(DATA_FRAME_COUNT);
    }

    public boolean structureValid() {
        return data.get(DATA_STRUCTURE_VALID) != 0;
    }

    public int humidityBand() {
        return data.get(DATA_HUMIDITY_BAND);
    }

    public boolean rainBonusActive() {
        return data.get(DATA_RAIN_BONUS) != 0;
    }

    public boolean solarAccess() {
        return data.get(DATA_SOLAR_ACCESS) != 0;
    }

    public int productionRatePerMinute() {
        return data.get(DATA_PRODUCTION_RATE);
    }

    public Fluid selectedOutput() {
        return fluidById(data.get(DATA_SELECTED_OUTPUT));
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (FluidPurgeSupport.handleMenuButton(player, dehumidifier, id)) {
            return true;
        }
        if (id == BUTTON_CYCLE_OUTPUT) {
            return dehumidifier.cycleSelectedOutput();
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.WOODEN_DEHUMIDIFIER.get());
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
        } else if (dehumidifier.isConversionIngredient(stack)) {
            if (!moveItemStackTo(
                    stack,
                    WoodenDehumidifierBlockEntity.SLOT_CONVERSION_INPUT,
                    WoodenDehumidifierBlockEntity.SLOT_CONVERSION_INPUT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (WoodenDehumidifierBlockEntity.isFluidOutputContainer(stack)) {
            if (!moveItemStackTo(
                    stack,
                    WoodenDehumidifierBlockEntity.SLOT_EMPTY_CONTAINER,
                    WoodenDehumidifierBlockEntity.SLOT_EMPTY_CONTAINER + 1,
                    false
            )) {
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
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 35 + column * 18, 114 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 35 + column * 18, 172));
        }
    }

    private static Fluid fluidById(int id) {
        Fluid fluid = BuiltInRegistries.FLUID.byId(id);
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    private static WoodenDehumidifierBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof WoodenDehumidifierBlockEntity dehumidifier) {
            return dehumidifier;
        }
        throw new IllegalStateException("Expected wooden dehumidifier block entity at " + pos);
    }
}
