package com.rngtech.content.menu;

import com.rngtech.content.blockentity.SilicaGelDehumidifierBlockEntity;
import com.rngtech.content.purge.FluidPurgeSupport;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModMenus;

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

public class SilicaGelDehumidifierMenu extends AbstractContainerMenu {
    private static final int DATA_WATER = 0;
    private static final int DATA_WATER_CAPACITY = 1;
    private static final int DATA_STRUCTURE_VALID = 2;
    private static final int DATA_LANES_START = 3;
    private static final int DATA_PER_LANE = 4;
    private static final int DATA_COUNT = DATA_LANES_START + SilicaGelDehumidifierBlockEntity.LANE_COUNT * DATA_PER_LANE;
    private static final int DATA_LANE_PROGRESS = 0;
    private static final int DATA_LANE_REQUIRED_TICKS = 1;
    private static final int DATA_LANE_STATUS = 2;
    private static final int DATA_LANE_WATER_OUTPUT = 3;

    private static final int PLAYER_INVENTORY_START = SilicaGelDehumidifierBlockEntity.SLOT_COUNT;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final SilicaGelDehumidifierBlockEntity dehumidifier;
    private final ContainerData data;

    public SilicaGelDehumidifierMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public SilicaGelDehumidifierMenu(
            int containerId,
            Inventory playerInventory,
            SilicaGelDehumidifierBlockEntity dehumidifier,
            ContainerData data
    ) {
        super(ModMenus.SILICA_GEL_DEHUMIDIFIER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(dehumidifier.getLevel(), dehumidifier.getBlockPos());
        this.dehumidifier = dehumidifier;
        this.data = data;
        addDataSlots(data);

        ItemStackHandler inventory = dehumidifier.getInventory();
        for (int lane = 0; lane < SilicaGelDehumidifierBlockEntity.LANE_COUNT; lane++) {
            addSlot(new SlotItemHandler(inventory, SilicaGelDehumidifierBlockEntity.SLOT_DRY_START + lane, 40, 28 + lane * 24));
        }
        for (int lane = 0; lane < SilicaGelDehumidifierBlockEntity.LANE_COUNT; lane++) {
            addSlot(new SlotItemHandler(inventory, SilicaGelDehumidifierBlockEntity.SLOT_SATURATED_START + lane, 150, 28 + lane * 24));
        }

        addPlayerInventory(playerInventory);
    }

    public float laneProgress(int lane) {
        int required = laneRequiredTicks(lane);
        return required <= 0 ? 0.0F : Mth.clamp(laneProgressTicks(lane) / (float) required, 0.0F, 1.0F);
    }

    public float waterProgress() {
        int capacity = waterCapacity();
        return capacity <= 0 ? 0.0F : Mth.clamp(waterAmount() / (float) capacity, 0.0F, 1.0F);
    }

    public int waterAmount() {
        return data.get(DATA_WATER);
    }

    public int waterCapacity() {
        return data.get(DATA_WATER_CAPACITY);
    }

    public boolean structureValid() {
        return data.get(DATA_STRUCTURE_VALID) != 0;
    }

    public int laneProgressTicks(int lane) {
        return data.get(laneDataIndex(lane, DATA_LANE_PROGRESS));
    }

    public int laneRequiredTicks(int lane) {
        return data.get(laneDataIndex(lane, DATA_LANE_REQUIRED_TICKS));
    }

    public int laneStatus(int lane) {
        return data.get(laneDataIndex(lane, DATA_LANE_STATUS));
    }

    public int laneWaterOutput(int lane) {
        return data.get(laneDataIndex(lane, DATA_LANE_WATER_OUTPUT));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return FluidPurgeSupport.handleMenuButton(player, dehumidifier, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SILICA_GEL_DEHUMIDIFIER.get());
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
        } else if (dehumidifier.isDryInput(stack)) {
            if (!moveItemStackTo(
                    stack,
                    SilicaGelDehumidifierBlockEntity.SLOT_DRY_START,
                    SilicaGelDehumidifierBlockEntity.SLOT_SATURATED_START,
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

    private static int laneDataIndex(int lane, int field) {
        return DATA_LANES_START + lane * DATA_PER_LANE + field;
    }

    private static SilicaGelDehumidifierBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof SilicaGelDehumidifierBlockEntity dehumidifier) {
            return dehumidifier;
        }
        throw new IllegalStateException("Expected silica gel dehumidifier block entity at " + pos);
    }
}
