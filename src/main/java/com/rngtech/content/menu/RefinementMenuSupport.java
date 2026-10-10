package com.rngtech.content.menu;

import com.rngtech.content.blockentity.RefinableMachine;
import com.rngtech.content.item.RefinementConsumableItem;
import com.rngtech.content.item.RefinementLensItem;
import com.rngtech.content.item.RefinementModifierItem;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineImplicitCatalog;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierEligibilityProfiles;
import com.rngtech.rpg.ModifierLensTag;
import com.rngtech.rpg.refinement.RefinementAction;
import com.rngtech.rpg.refinement.RefinementEngine;
import com.rngtech.rpg.refinement.RefinementModifier;
import com.rngtech.rpg.refinement.RefinementOperation;
import com.rngtech.rpg.refinement.RefinementResult;
import com.rngtech.rpg.refinement.RefinementSelection;
import com.rngtech.rpg.refinement.RefinementTargets;
import com.rngtech.rpg.unique.UniqueItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Set;
import java.util.function.BooleanSupplier;

public final class RefinementMenuSupport {
    public static final int BUTTON_APPLY = 0;
    public static final int SLOT_TARGET = 0;
    public static final int SLOT_CONSUMABLE = 1;
    public static final int SLOT_MODIFIER = 2;
    public static final int REFINEMENT_TARGET_SLOT_X = 24;
    public static final int REFINEMENT_CONSUMABLE_SLOT_X = 68;
    public static final int REFINEMENT_SLOT_Y = 42;

    public static ItemStack machineDisplayStack(ItemLike item, MachineTraits traits) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.MACHINE_TRAITS.get(), MachineImplicitCatalog.effectiveTraits(traits, item));
        return stack;
    }

    public static MachineTraits displayTraits(ItemStack stack) {
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        return traits == null ? MachineTraits.EMPTY : traits;
    }

    public static void setMachineDisplay(ItemStackHandler displayInventory, ItemLike item, MachineTraits traits) {
        displayInventory.setStackInSlot(0, machineDisplayStack(item, traits));
    }

    public static boolean isConsumable(ItemStack stack) {
        return stack.getItem() instanceof RefinementConsumableItem;
    }

    public static boolean isModifier(ItemStack stack) {
        return stack.getItem() instanceof RefinementModifierItem;
    }

    public static boolean isLens(ItemStack stack) {
        return stack.getItem() instanceof RefinementLensItem;
    }

    public static boolean applyToMachine(Player player, RefinableMachine machine, ItemStackHandler displayInventory, ItemLike item) {
        ItemStack consumable = machine.getRefinementInventory().getStackInSlot(0);
        if (!(consumable.getItem() instanceof RefinementConsumableItem refinementItem)) {
            showFailure(player, "rngtech.refinement.failure.invalid_consumable");
            return false;
        }

        RefinementResult result = RefinementEngine.apply(
                ModifierEligibilityProfiles.forMachine(machine.refinementMachineType()),
                MachineImplicitCatalog.storedTraits(machine.machineTraits()),
                refinementItem.operation(),
                machine.refinementModifierRollComponentStage(),
                player.level().random
        );
        if (!result.success()) {
            showFailure(player, result.messageKey());
            return false;
        }

        machine.setMachineTraits(result.traits());
        if (result.consumeCatalyst()) {
            shrinkStack(machine.getRefinementInventory(), 0);
        }
        setMachineDisplay(displayInventory, item, result.traits());
        showSuccess(player, result);
        return true;
    }

    public static boolean applyToTargetStack(Player player, ItemStackHandler inventory) {
        return applyToTargetStack(player, inventory, RefinementSelection.none(), false);
    }

    public static boolean applyToTargetStack(
            Player player,
            ItemStackHandler inventory,
            RefinementSelection selection,
            boolean requireSelection
    ) {
        ItemStack target = inventory.getStackInSlot(SLOT_TARGET);
        if (!RefinementTargets.canRefine(target)) {
            showFailure(player, "rngtech.refinement.failure.invalid_target");
            return false;
        }

        ItemStack consumable = inventory.getStackInSlot(SLOT_CONSUMABLE);
        if (!(consumable.getItem() instanceof RefinementConsumableItem refinementItem)) {
            showFailure(player, "rngtech.refinement.failure.invalid_consumable");
            return false;
        }
        if (refinementItem.operation() == RefinementOperation.CORRUPT
                && !RefinementTargets.storedTraits(target).isCorrupted()
                && !RefinementTargets.canCorrupt(target)) {
            showFailure(player, "rngtech.refinement.failure.cannot_corrupt");
            return false;
        }
        if (requireSelection && refinementItem.operation().requiresForgeSelection()) {
            RefinementAction action = refinementItem.operation().action();
            if (action == RefinementAction.RANDOM_ADD && selection.kind() != RefinementSelection.Kind.EMPTY_SLOT) {
                showFailure(
                        player,
                        RefinementEngine.hasAnyOpenAffixSlot(RefinementTargets.storedTraits(target))
                                ? "rngtech.refinement.failure.select_empty_slot"
                                : "rngtech.refinement.failure.max_affixes"
                );
                return false;
            }
            if (action == RefinementAction.SELECTED_UPGRADE
                    && selection.kind() != RefinementSelection.Kind.EXISTING_MODIFIER) {
                showFailure(player, "rngtech.refinement.failure.select_modifier_or_slot");
                return false;
            }
            if (selection.isNone()) {
                showFailure(player, "rngtech.refinement.failure.select_modifier_or_slot");
                return false;
            }
        }

        RefinementResult result = RefinementEngine.apply(
                RefinementTargets.eligibilityProfile(target),
                RefinementTargets.storedTraits(target),
                refinementItem.operation(),
                RefinementTargets.modifierRollComponentStage(target),
                selection,
                modifier(inventory),
                lensTags(inventory),
                player.level().random,
                UniqueItems.definition(target)
        );
        if (!result.success()) {
            showFailure(player, result.messageKey());
            return false;
        }

        ItemStack refinedTarget = target.copy();
        RefinementTargets.setTraits(refinedTarget, result.traits());
        inventory.setStackInSlot(SLOT_TARGET, refinedTarget);
        if (result.consumeCatalyst()) {
            shrinkStack(inventory, SLOT_CONSUMABLE);
        }
        if (result.consumeModifier()) {
            shrinkStack(inventory, SLOT_MODIFIER);
        }
        showSuccess(player, result);
        return true;
    }

    public static SlotItemHandler targetDisplaySlot(ItemStackHandler itemHandler, int x, int y, BooleanSupplier activeSupplier) {
        return new DisplaySlot(itemHandler, 0, x, y, activeSupplier);
    }

    public static SlotItemHandler consumableSlot(ItemStackHandler itemHandler, int index, int x, int y, BooleanSupplier activeSupplier) {
        return new ConsumableSlot(itemHandler, index, x, y, activeSupplier);
    }

    public static SlotItemHandler modifierSlot(ItemStackHandler itemHandler, int index, int x, int y, BooleanSupplier activeSupplier) {
        return new ModifierSlot(itemHandler, index, x, y, activeSupplier);
    }

    private static RefinementModifier modifier(ItemStackHandler inventory) {
        if (inventory.getSlots() <= SLOT_MODIFIER) {
            return RefinementModifier.NONE;
        }
        ItemStack stack = inventory.getStackInSlot(SLOT_MODIFIER);
        return stack.getItem() instanceof RefinementModifierItem modifierItem
                ? modifierItem.modifier()
                : RefinementModifier.NONE;
    }

    private static Set<ModifierLensTag> lensTags(ItemStackHandler inventory) {
        if (inventory.getSlots() <= SLOT_MODIFIER) {
            return Set.of();
        }
        ItemStack stack = inventory.getStackInSlot(SLOT_MODIFIER);
        return stack.getItem() instanceof RefinementLensItem lensItem ? lensItem.lensTags() : Set.of();
    }

    private static void shrinkStack(ItemStackHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot).copy();
        stack.shrink(1);
        inventory.setStackInSlot(slot, stack);
    }

    private static void showSuccess(Player player, RefinementResult result) {
        player.displayClientMessage(Component.translatable(result.messageKey(), result.consumedPotential()), true);
    }

    private static void showFailure(Player player, String messageKey) {
        player.displayClientMessage(Component.translatable(messageKey), true);
    }

    private static final class DisplaySlot extends SlotItemHandler {
        private final BooleanSupplier activeSupplier;

        private DisplaySlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition, BooleanSupplier activeSupplier) {
            super(itemHandler, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }

    private static final class ConsumableSlot extends SlotItemHandler {
        private final BooleanSupplier activeSupplier;

        private ConsumableSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition, BooleanSupplier activeSupplier) {
            super(itemHandler, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isConsumable(stack);
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }

    private static final class ModifierSlot extends SlotItemHandler {
        private final BooleanSupplier activeSupplier;

        private ModifierSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition, BooleanSupplier activeSupplier) {
            super(itemHandler, index, xPosition, yPosition);
            this.activeSupplier = activeSupplier;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isModifier(stack);
        }

        @Override
        public boolean isActive() {
            return activeSupplier.getAsBoolean();
        }
    }

    private RefinementMenuSupport() {
    }
}
