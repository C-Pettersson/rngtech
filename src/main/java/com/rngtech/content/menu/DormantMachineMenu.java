package com.rngtech.content.menu;

import com.rngtech.RNGTechConfig;
import com.rngtech.content.blockentity.DormantMachineBlockEntity;
import com.rngtech.content.dormant.DormantCore;
import com.rngtech.content.dormant.DormantPhase;
import com.rngtech.content.entity.DerelictCartEntity;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.rpg.progression.MachineMasteryFamily;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.Predicate;

/** Nine Battery Cell sockets over the player inventory, plus the wake state the screen draws. */
public class DormantMachineMenu extends AbstractContainerMenu {
    public static final int CELL_X = 13;
    public static final int CELL_Y = 35;
    public static final int CELL_SPACING = 19;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 114;
    public static final int HOTBAR_Y = 172;

    private static final int PLAYER_START = DormantCore.CELL_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final Predicate<Player> valid;
    private final ContainerData data;
    private final IItemHandler cells;

    /** Client side: the buffer names either a block position or a derelict cart's entity id. */
    public DormantMachineMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, clientCore(inventory, extraData), new SimpleContainerData(DormantCore.DATA_COUNT), player -> true);
    }

    public DormantMachineMenu(int containerId, Inventory inventory, DormantCore core, ContainerData data, Predicate<Player> valid) {
        super(ModMenus.DORMANT_MACHINE.get(), containerId);
        checkContainerDataCount(data, DormantCore.DATA_COUNT);
        this.valid = valid;
        this.data = data;
        this.cells = core == null ? new ItemStackHandler(DormantCore.CELL_SLOTS) : core.cells();
        for (int slot = 0; slot < DormantCore.CELL_SLOTS; slot++) {
            addSlot(new SlotItemHandler(cells, slot, CELL_X + slot % 3 * CELL_SPACING, CELL_Y + slot / 3 * CELL_SPACING) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return BatteryCellItem.isBatteryCell(stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    public long charge() {
        return Integer.toUnsignedLong(data.get(DormantCore.DATA_CHARGE));
    }

    public long wakeFe() {
        return Integer.toUnsignedLong(data.get(DormantCore.DATA_WAKE_FE));
    }

    public float chargeFraction() {
        long wake = wakeFe();
        return wake <= 0 ? 0.0F : Math.min(1.0F, charge() / (float) wake);
    }

    public int lastInput() {
        return data.get(DormantCore.DATA_LAST_INPUT);
    }

    public int wakeInput() {
        return data.get(DormantCore.DATA_WAKE_INPUT);
    }

    public DormantPhase phase() {
        int ordinal = data.get(DormantCore.DATA_PHASE);
        return DormantPhase.values()[Math.clamp(ordinal, 0, DormantPhase.values().length - 1)];
    }

    public int soulLevel() {
        return data.get(DormantCore.DATA_LEVEL);
    }

    public int releaseStage() {
        return data.get(DormantCore.DATA_RELEASE_STAGE);
    }

    public MachineMasteryFamily family() {
        int ordinal = data.get(DormantCore.DATA_FAMILY);
        return ordinal < 0 || ordinal >= MachineMasteryFamily.values().length ? null : MachineMasteryFamily.values()[ordinal];
    }

    public RNGTechConfig.DormantFailedAbsorb failedAbsorb() {
        int ordinal = data.get(DormantCore.DATA_FAILED_ABSORB);
        RNGTechConfig.DormantFailedAbsorb[] values = RNGTechConfig.DormantFailedAbsorb.values();
        return values[Math.clamp(ordinal, 0, values.length - 1)];
    }

    /** FE per tick the installed cells can give together. */
    public int cellOutput() {
        int total = 0;
        for (int slot = 0; slot < DormantCore.CELL_SLOTS; slot++) {
            ItemStack cell = cells.getStackInSlot(slot);
            if (BatteryCellItem.isBatteryCell(cell) && BatteryCellItem.energyStored(cell) > 0) {
                total += BatteryCellItem.maxOutput(cell);
            }
        }
        return total;
    }

    @Override
    public boolean stillValid(Player player) {
        return valid.test(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) { return ItemStack.EMPTY; }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) { return ItemStack.EMPTY; }
        } else if (BatteryCellItem.isBatteryCell(stack)) {
            if (!moveItemStackTo(stack, 0, PLAYER_START, false)) { return ItemStack.EMPTY; }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == moved.getCount()) { return ItemStack.EMPTY; }
        slot.onTake(player, stack);
        return moved;
    }

    public static Component siteName(ResourceLocation site) {
        return Component.translatable("rngtech.dormant_site." + site.getNamespace() + "." + site.getPath());
    }

    private static DormantCore clientCore(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        var level = inventory.player.level();
        if (buffer.readBoolean()) {
            return level.getEntity(buffer.readVarInt()) instanceof DerelictCartEntity cart ? cart.core() : null;
        }
        return level.getBlockEntity(buffer.readBlockPos()) instanceof DormantMachineBlockEntity machine ? machine.core() : null;
    }
}
