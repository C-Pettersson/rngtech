package com.rngtech.content.blockentity;

import com.rngtech.content.block.AffixForgeBlock;
import com.rngtech.content.item.RefinementConsumableItem;
import com.rngtech.content.item.RefinementLensItem;
import com.rngtech.content.item.RefinementModifierItem;
import com.rngtech.content.menu.AffixForgeMenu;
import com.rngtech.content.menu.RefinementMenuSupport;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.refinement.RefinementOperation;
import com.rngtech.rpg.refinement.RefinementSelection;
import com.rngtech.rpg.refinement.RefinementTargets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class AffixForgeBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_TARGET = 0;
    public static final int SLOT_CONSUMABLE = 1;
    public static final int SLOT_MODIFIER = 2;
    public static final int SLOT_LENS_ARRAY = 3;
    public static final int SLOT_MODIFIER_SOCKET = 4;
    public static final int SLOT_RESONANCE_MATRIX = 5;
    public static final int SLOT_COUNT = 6;

    private boolean suppressUpgradeStateSync;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_TARGET -> RefinementTargets.canRefine(stack);
                case SLOT_CONSUMABLE -> isRefinementConsumable(stack);
                case SLOT_MODIFIER -> isFocusItem(stack);
                case SLOT_LENS_ARRAY -> stack.is(ModItems.AFFIX_LENS_ARRAY.get());
                case SLOT_MODIFIER_SOCKET -> stack.is(ModItems.AFFIX_MODIFIER_SOCKET.get());
                case SLOT_RESONANCE_MATRIX -> stack.is(ModItems.AFFIX_RESONANCE_MATRIX.get());
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return isUpgradeSlot(slot) ? 1 : super.getSlotLimit(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (isUpgradeSlot(slot)) {
                syncUpgradeBlockState();
            }
        }
    };

    public AffixForgeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.AFFIX_FORGE.get(), pos, blockState);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public boolean hasLensArray() {
        return inventory.getStackInSlot(SLOT_LENS_ARRAY).is(ModItems.AFFIX_LENS_ARRAY.get());
    }

    public boolean hasModifierSocket() {
        return inventory.getStackInSlot(SLOT_MODIFIER_SOCKET).is(ModItems.AFFIX_MODIFIER_SOCKET.get());
    }

    public boolean hasResonanceMatrix() {
        return inventory.getStackInSlot(SLOT_RESONANCE_MATRIX).is(ModItems.AFFIX_RESONANCE_MATRIX.get());
    }

    public boolean isRefinementConsumable(ItemStack stack) {
        return stack.getItem() instanceof RefinementConsumableItem item && item.operation() != null;
    }

    public boolean isFocusItem(ItemStack stack) {
        return stack.getItem() instanceof RefinementLensItem || stack.getItem() instanceof RefinementModifierItem;
    }

    public boolean supportsOperation(RefinementOperation operation) {
        return operation != null && lockedMessage(operation) == null;
    }

    public String lockedMessage(RefinementOperation operation) {
        return lockedMessage(operation, hasLensArray(), hasResonanceMatrix());
    }

    public static String lockedMessage(RefinementOperation operation, boolean hasLensArray, boolean hasResonanceMatrix) {
        if (operation == null) {
            return "rngtech.refinement.failure.invalid_consumable";
        }
        switch (operation) {
            case ADD_MODIFIER, UPGRADE_RANDOM_MODIFIER, REMOVE_MODIFIER:
                return null;
            case KINETIC_LENS, EFFICIENCY_LENS:
                return hasLensArray ? null : "rngtech.refinement.failure.requires_lens_array";
            case UPGRADE_SELECTED_MODIFIER, ASCEND_RARITY, ASCENSION_CATALYST, CHAOS_CRYSTAL, EXPANSION_CRYSTAL, NULL_CRYSTAL, CORRUPT:
                return hasResonanceMatrix ? null : "rngtech.refinement.failure.requires_resonance_matrix";
            default:
                return "rngtech.refinement.failure.invalid_consumable";
        }
    }

    public static int upgradeSlot(ItemStack stack) {
        if (stack.is(ModItems.AFFIX_LENS_ARRAY.get())) {
            return SLOT_LENS_ARRAY;
        }
        if (stack.is(ModItems.AFFIX_MODIFIER_SOCKET.get())) {
            return SLOT_MODIFIER_SOCKET;
        }
        if (stack.is(ModItems.AFFIX_RESONANCE_MATRIX.get())) {
            return SLOT_RESONANCE_MATRIX;
        }
        return -1;
    }

    public boolean applyRefinement(Player player, RefinementSelection selection) {
        ItemStack consumable = inventory.getStackInSlot(SLOT_CONSUMABLE);
        if (consumable.getItem() instanceof RefinementConsumableItem refinementItem) {
            String lockedMessage = lockedMessage(refinementItem.operation());
            if (lockedMessage != null) {
                showFailure(player, lockedMessage);
                return false;
            }
        }
        String focusMessage = focusLockedMessage(inventory.getStackInSlot(SLOT_MODIFIER));
        if (focusMessage != null) {
            showFailure(player, focusMessage);
            return false;
        }
        return RefinementMenuSupport.applyToTargetStack(player, inventory, selection, true);
    }

    private String focusLockedMessage(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof RefinementLensItem) {
            return hasLensArray() ? null : "rngtech.refinement.failure.requires_lens_array";
        }
        if (stack.getItem() instanceof RefinementModifierItem) {
            return hasModifierSocket() ? null : "rngtech.refinement.failure.requires_modifier_socket";
        }
        return "rngtech.refinement.failure.invalid_focus";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rngtech.affix_forge");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AffixForgeMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
    }

    public void dropInventory(Level level) {
        suppressUpgradeStateSync = true;
        try {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
                    inventory.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        } finally {
            suppressUpgradeStateSync = false;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            CompoundTag inventoryTag = tag.getCompound("Inventory");
            int savedSize = inventoryTag.getInt("Size");
            boolean legacyInventory = savedSize > 0 && savedSize < SLOT_COUNT;
            if (savedSize < SLOT_COUNT) {
                inventoryTag = inventoryTag.copy();
                inventoryTag.putInt("Size", SLOT_COUNT);
            }
            inventory.deserializeNBT(registries, inventoryTag);
            if (legacyInventory) {
                installLegacyUpgrades();
            }
        }
        syncUpgradeBlockState();
    }

    private void installLegacyUpgrades() {
        if (inventory.getStackInSlot(SLOT_LENS_ARRAY).isEmpty()) {
            inventory.setStackInSlot(SLOT_LENS_ARRAY, new ItemStack(ModItems.AFFIX_LENS_ARRAY.get()));
        }
        if (inventory.getStackInSlot(SLOT_MODIFIER_SOCKET).isEmpty()) {
            inventory.setStackInSlot(SLOT_MODIFIER_SOCKET, new ItemStack(ModItems.AFFIX_MODIFIER_SOCKET.get()));
        }
        if (inventory.getStackInSlot(SLOT_RESONANCE_MATRIX).isEmpty()) {
            inventory.setStackInSlot(SLOT_RESONANCE_MATRIX, new ItemStack(ModItems.AFFIX_RESONANCE_MATRIX.get()));
        }
    }

    private void syncUpgradeBlockState() {
        if (suppressUpgradeStateSync || level == null || level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (!state.hasProperty(AffixForgeBlock.HAS_LENS_ARRAY)
                || !state.hasProperty(AffixForgeBlock.HAS_MODIFIER_SOCKET)
                || !state.hasProperty(AffixForgeBlock.HAS_RESONANCE_MATRIX)) {
            return;
        }
        BlockState nextState = state
                .setValue(AffixForgeBlock.HAS_LENS_ARRAY, hasLensArray())
                .setValue(AffixForgeBlock.HAS_MODIFIER_SOCKET, hasModifierSocket())
                .setValue(AffixForgeBlock.HAS_RESONANCE_MATRIX, hasResonanceMatrix());
        if (nextState != state) {
            level.setBlock(worldPosition, nextState, Block.UPDATE_ALL);
        }
    }

    private static boolean isUpgradeSlot(int slot) {
        return slot == SLOT_LENS_ARRAY || slot == SLOT_MODIFIER_SOCKET || slot == SLOT_RESONANCE_MATRIX;
    }

    private void showFailure(Player player, String messageKey) {
        player.displayClientMessage(Component.translatable(messageKey), true);
    }
}
