package com.rngtech.content.menu;

import com.rngtech.content.blockentity.AlgaePhotobioreactorBlockEntity;
import com.rngtech.content.purge.FluidPurgeSupport;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModMenus;

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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class AlgaePhotobioreactorMenu extends AbstractContainerMenu {
    public static final int TAB_PROCESSING = 0;
    public static final int TAB_GEAR = 1;
    public static final int TAB_STATS = 2;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_REQUIRED_TICKS = 1;
    private static final int DATA_WATER = 2;
    private static final int DATA_WATER_CAPACITY = 3;
    private static final int DATA_CARBON = 4;
    private static final int DATA_CARBON_CAPACITY = 5;
    private static final int DATA_STATUS = 6;
    private static final int DATA_LIGHT = 7;
    private static final int DATA_MIN_LIGHT = 8;
    private static final int DATA_BIO_CONVERSION = 9;
    private static final int DATA_OUTPUT_BONUS_PROGRESS = 10;
    private static final int DATA_COUNT = 11;
    private static final int STAT_SCALE = 100;
    private static final int OUTPUT_BONUS_PROGRESS_SCALE = 1000;
    private static final int BIO_CHAMBER_SLOT = 5;
    private static final int PLAYER_INVENTORY_START = BIO_CHAMBER_SLOT + 1;
    private static final int HOTBAR_END = PLAYER_INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final AlgaePhotobioreactorBlockEntity reactor;
    private int selectedTab = TAB_PROCESSING;

    public AlgaePhotobioreactorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                blockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(DATA_COUNT)
        );
    }

    public AlgaePhotobioreactorMenu(
            int containerId,
            Inventory playerInventory,
            AlgaePhotobioreactorBlockEntity reactor,
            ContainerData data
    ) {
        super(ModMenus.ALGAE_PHOTOBIOREACTOR.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        access = ContainerLevelAccess.create(reactor.getLevel(), reactor.getBlockPos());
        this.data = data;
        this.reactor = reactor;
        addDataSlots(data);

        ItemStackHandler inventory = reactor.getInventory();
        addSlot(new TabbedSlot(inventory, AlgaePhotobioreactorBlockEntity.SLOT_OUTPUT, 139, 66, () -> selectedTab == TAB_PROCESSING));
        addSlot(new TabbedSlot(
                inventory,
                AlgaePhotobioreactorBlockEntity.SLOT_WATER_INPUT_CONTAINER,
                9,
                66,
                () -> selectedTab == TAB_PROCESSING
        ));
        addSlot(new TabbedSlot(
                inventory,
                AlgaePhotobioreactorBlockEntity.SLOT_WATER_OUTPUT_CONTAINER,
                31,
                66,
                () -> selectedTab == TAB_PROCESSING
        ));
        addSlot(new TabbedSlot(
                inventory,
                AlgaePhotobioreactorBlockEntity.SLOT_CARBON_INPUT_CONTAINER,
                55,
                66,
                () -> selectedTab == TAB_PROCESSING
        ));
        addSlot(new TabbedSlot(
                inventory,
                AlgaePhotobioreactorBlockEntity.SLOT_CARBON_OUTPUT_CONTAINER,
                77,
                66,
                () -> selectedTab == TAB_PROCESSING
        ));

        ItemStackHandler gearInventory = reactor.getGearInventory();
        addSlot(new TabbedSlot(gearInventory, AlgaePhotobioreactorBlockEntity.SLOT_BIO_CHAMBER, 110, 48, () -> selectedTab == TAB_GEAR));

        addPlayerInventory(playerInventory);
    }

    public void selectTab(int tab) {
        selectedTab = switch (tab) {
            case TAB_GEAR -> TAB_GEAR;
            case TAB_STATS -> TAB_STATS;
            default -> TAB_PROCESSING;
        };
    }

    public int selectedTab() {
        return selectedTab;
    }

    public float growthProgress() {
        int required = requiredTicks();
        return required <= 0 ? 0.0F : Mth.clamp(progressTicks() / (float) required, 0.0F, 1.0F);
    }

    public float waterProgress() {
        return tankProgress(waterAmount(), waterCapacity());
    }

    public float carbonProgress() {
        return tankProgress(carbonAmount(), carbonCapacity());
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

    public int carbonAmount() {
        return data.get(DATA_CARBON);
    }

    public int carbonCapacity() {
        return data.get(DATA_CARBON_CAPACITY);
    }

    public Component waterFluidName() {
        return fluidName(reactor.getWaterFluid(), Component.translatable("block.minecraft.water"));
    }

    public Component carbonFluidName() {
        return fluidName(reactor.getCarbonFluid(), Component.translatable("rngtech.algae_photobioreactor.carbon"));
    }

    public int statusCode() {
        return data.get(DATA_STATUS);
    }

    public int lightLevel() {
        return data.get(DATA_LIGHT);
    }

    public int minimumLight() {
        return data.get(DATA_MIN_LIGHT);
    }

    public double statValue(int dataIndex) {
        return data.get(dataIndex) / (double) STAT_SCALE;
    }

    public double outputBonusProgress() {
        return data.get(DATA_OUTPUT_BONUS_PROGRESS) / (double) OUTPUT_BONUS_PROGRESS_SCALE;
    }

    public static int bioConversionDataIndex() {
        return DATA_BIO_CONVERSION;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return FluidPurgeSupport.handleMenuButton(player, reactor, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ALGAE_PHOTOBIOREACTOR.get());
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
        } else if (reactor.isBioChamber(stack)) {
            if (!moveItemStackTo(stack, BIO_CHAMBER_SLOT, BIO_CHAMBER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (AlgaePhotobioreactorBlockEntity.isWaterInputContainer(stack)) {
            if (!moveItemStackTo(
                    stack,
                    AlgaePhotobioreactorBlockEntity.SLOT_WATER_INPUT_CONTAINER,
                    AlgaePhotobioreactorBlockEntity.SLOT_WATER_INPUT_CONTAINER + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (AlgaePhotobioreactorBlockEntity.isCarbonInputContainer(stack)) {
            if (!moveItemStackTo(
                    stack,
                    AlgaePhotobioreactorBlockEntity.SLOT_CARBON_INPUT_CONTAINER,
                    AlgaePhotobioreactorBlockEntity.SLOT_CARBON_INPUT_CONTAINER + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (AlgaePhotobioreactorBlockEntity.isAlgaeOutputStack(stack)) {
            if (!moveItemStackTo(
                    stack,
                    AlgaePhotobioreactorBlockEntity.SLOT_OUTPUT,
                    AlgaePhotobioreactorBlockEntity.SLOT_OUTPUT + 1,
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
                addSlot(new TabbedInventorySlot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        102 + row * 18,
                        () -> selectedTab != TAB_STATS
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new TabbedInventorySlot(
                    playerInventory,
                    column,
                    8 + column * 18,
                    160,
                    () -> selectedTab != TAB_STATS
            ));
        }
    }

    private static float tankProgress(int amount, int capacity) {
        return capacity <= 0 ? 0.0F : Mth.clamp(amount / (float) capacity, 0.0F, 1.0F);
    }

    private static Component fluidName(FluidStack stack, Component emptyName) {
        return stack.isEmpty() ? emptyName : stack.getHoverName();
    }

    private static AlgaePhotobioreactorBlockEntity blockEntity(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof AlgaePhotobioreactorBlockEntity reactor) {
            return reactor;
        }
        throw new IllegalStateException("Expected algae photobioreactor block entity at " + pos);
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
