package com.rngtech.content.item;

import com.rngtech.content.itemfilter.ItemFilterMatcher;
import com.rngtech.content.menu.MinersCompanionMenu;
import com.rngtech.content.minerscompanion.MinersCompanionState;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineNameGenerator;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.ModifierEligibilityProfiles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

public class MinersCompanionItem extends Item {
    public static final int BASE_FE_PER_ITEM = 16;
    public static final int MAGNET_PASSIVE_FE_PER_TICK = 1;
    public static final int MAGNET_FE_PER_ITEM_ENTITY = 2;
    public static final int MINING_LAMP_FE_PER_TICK = 1;
    public static final int MINING_LAMP_LIGHT_LEVEL = 14;
    public static final int BASE_FILTER_SLOTS = 3;
    public static final int MAX_FILTER_SLOTS = MinersCompanionState.FILTER_SLOT_COUNT;
    public static final int COMPONENT_STAGE = 2;

    public MinersCompanionItem(Properties properties) {
        super(properties);
    }

    public static boolean isMinersCompanion(ItemStack stack) {
        return stack.getItem() instanceof MinersCompanionItem;
    }

    public static MinersCompanionState state(ItemStack stack) {
        MinersCompanionState state = stack.get(ModDataComponents.MINERS_COMPANION_STATE.get());
        return state == null ? MinersCompanionState.EMPTY : state;
    }

    public static void setState(ItemStack stack, MinersCompanionState state) {
        if (isMinersCompanion(stack)) {
            stack.set(ModDataComponents.MINERS_COMPANION_STATE.get(), state);
        }
    }

    public static MachineTraits storedTraits(ItemStack stack) {
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        return traits == null ? MachineTraits.EMPTY : traits;
    }

    public static MachineTraits traits(ItemStack stack) {
        return storedTraits(stack);
    }

    public static MachineStatAccumulator effectiveStats(ItemStack stack) {
        MachineStatAccumulator stats = MachineStatAccumulator.minersCompanionBase();
        stats.apply(traits(stack));
        return stats;
    }

    public static int activeFilterSlots(ItemStack stack) {
        int slots = (int) Math.floor(effectiveStats(stack).value(MachineStat.BLOCK_FILTER_SLOTS));
        return Mth.clamp(slots, 1, MAX_FILTER_SLOTS);
    }

    public static int energyCostPerItem(ItemStack stack) {
        return effectiveStats(stack).adjustedEnergyCost(BASE_FE_PER_ITEM);
    }

    public static boolean isEnabledAndReady(ItemStack stack) {
        MinersCompanionState state = state(stack);
        return isMinersCompanion(stack) && state.enabled() && state.hasValidGear();
    }

    public static boolean isMagnetEnabledAndReady(ItemStack stack) {
        MinersCompanionState state = state(stack);
        return isMinersCompanion(stack) && state.magnetEnabled() && state.hasValidMagnetGear() && storedEnergy(stack) > 0;
    }

    public static boolean isMiningLampEnabledAndReady(ItemStack stack) {
        MinersCompanionState state = state(stack);
        return isMinersCompanion(stack) && state.miningLampEnabled() && state.hasValidMiningLampGear() && storedEnergy(stack) > 0;
    }

    public static boolean hasActiveMiningLamp(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isMiningLampEnabledAndReady(stack)) {
                return true;
            }
        }
        return isMiningLampEnabledAndReady(player.getOffhandItem());
    }

    public static boolean hasMatchingFilter(ItemStack companionStack, ItemStack dropStack) {
        return hasMatchingFilter(companionStack, dropStack, Items.AIR);
    }

    public static boolean hasMatchingFilter(ItemStack companionStack, ItemStack dropStack, Item brokenBlockItem) {
        if (dropStack.isEmpty() && brokenBlockItem == Items.AIR) {
            return false;
        }
        MinersCompanionState state = state(companionStack);
        int activeSlots = activeFilterSlots(companionStack);
        List<ItemStack> filters = new java.util.ArrayList<>(activeSlots);
        for (int index = 0; index < activeSlots; index++) {
            filters.add(state.filter(index));
        }
        return ItemFilterMatcher.passesCompanionFilters(filters, dropStack, brokenBlockItem);
    }

    public static int chewCount(ItemStack companionStack, int requestedCount) {
        if (!isEnabledAndReady(companionStack) || requestedCount <= 0) {
            return 0;
        }
        MinersCompanionState state = state(companionStack);
        ItemStack cell = state.batteryCell();
        int cost = energyCostPerItem(companionStack);
        if (cost <= 0) {
            return requestedCount;
        }
        int affordable = BatteryCellItem.energyStored(cell) / cost;
        int chewed = Math.min(requestedCount, affordable);
        if (chewed <= 0) {
            return 0;
        }
        int removed = BatteryCellItem.removeEnergy(cell, chewed * cost);
        int paidCount = removed / cost;
        if (paidCount > 0) {
            setState(companionStack, state.withBatteryCell(cell));
        }
        return paidCount;
    }

    public static boolean consumeEnergy(ItemStack companionStack, int amount) {
        if (!isMinersCompanion(companionStack) || amount <= 0) {
            return amount <= 0;
        }
        MinersCompanionState state = state(companionStack);
        ItemStack cell = state.batteryCell();
        if (!BatteryCellItem.isBatteryCell(cell) || BatteryCellItem.energyStored(cell) < amount) {
            return false;
        }
        int removed = BatteryCellItem.removeEnergy(cell, amount);
        if (removed < amount) {
            return false;
        }
        setState(companionStack, state.withBatteryCell(cell));
        return true;
    }

    public static int storedEnergy(ItemStack stack) {
        return BatteryCellItem.energyStored(state(stack).batteryCell());
    }

    public static int energyCapacity(ItemStack stack) {
        return BatteryCellItem.energyCapacity(state(stack).batteryCell());
    }

    public static IEnergyStorage energyStorage(ItemStack stack) {
        if (!isMinersCompanion(stack) || !BatteryCellItem.isBatteryCell(state(stack).batteryCell())) {
            return null;
        }
        return new InstalledCellEnergyStorage(stack);
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        super.onCraftedPostProcess(stack, level);
        CraftedTraitOutputs.handleCrafted(stack, level.random);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (CraftedTraitOutputs.isUnidentified(stack)) {
            return CraftedTraitOutputs.unidentifiedName(super.getName(stack));
        }
        MachineTraits traits = traits(stack);
        if (traits.isEmpty()) {
            return super.getName(stack);
        }
        return MachineNameGenerator.generatedName(traits, super.getName(stack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                toggle(stack, player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!context.getLevel().isClientSide) {
                toggle(context.getItemInHand(), player);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (!context.getLevel().isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, context.getHand());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return energyCapacity(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = energyCapacity(stack);
        return capacity <= 0 ? 0 : Math.round(13.0F * storedEnergy(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(0.48F, 0.85F, 0.95F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        MachineTraitTooltip.appendUnidentified(stack, tooltipComponents);
        MachineTraits traits = traits(stack);
        if (stack.has(ModDataComponents.MACHINE_TRAITS.get())) {
            MachineTraitTooltip.appendTraitHeader(traits, tooltipComponents, true);
        } else {
            MachineTraitTooltip.appendUnrolledTraitHeader(
                    MachineTraitRoller.refinementPotentialRange(
                            ModifierEligibilityProfiles.forMachine(MachineType.MINERS_COMPANION),
                            COMPONENT_STAGE
                    ),
                    tooltipComponents
            );
        }
        MinersCompanionState state = state(stack);
        tooltipComponents.add(Component.translatable(
                state.enabled() ? "rngtech.tooltip.miners_companion.block_chew_enabled" : "rngtech.tooltip.miners_companion.block_chew_disabled"
        ).withStyle(state.enabled() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                state.magnetEnabled() ? "rngtech.tooltip.miners_companion.magnet_enabled" : "rngtech.tooltip.miners_companion.magnet_disabled"
        ).withStyle(state.magnetEnabled() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                state.miningLampEnabled() ? "rngtech.tooltip.miners_companion.lamp_enabled" : "rngtech.tooltip.miners_companion.lamp_disabled"
        ).withStyle(state.miningLampEnabled() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.miners_companion.filters",
                activeFilterSlots(stack),
                MAX_FILTER_SLOTS
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "rngtech.tooltip.miners_companion.energy",
                storedEnergy(stack),
                energyCapacity(stack),
                energyCostPerItem(stack)
        ).withStyle(ChatFormatting.GRAY));
        if (MachineTraitTooltip.shouldShowMaterialData()) {
            tooltipComponents.add(Component.translatable("rngtech.tooltip.miners_companion.gear").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("rngtech.tooltip.miners_companion.utility_gear").withStyle(ChatFormatting.GRAY));
        }
        MachineTraitTooltip.appendTraitDetails(traits, tooltipComponents);
        MachineTraitTooltip.appendTraitKeyHints(tooltipComponents);
    }

    private static void toggle(ItemStack stack, Player player) {
        MinersCompanionState state = state(stack);
        if (!state.hasValidGear()) {
            player.displayClientMessage(Component.translatable("rngtech.miners_companion.block_chew_missing_gear"), true);
            return;
        }
        MinersCompanionState updated = state.withEnabled(!state.enabled());
        setState(stack, updated);
        player.displayClientMessage(Component.translatable(
                updated.enabled() ? "rngtech.miners_companion.block_chew_enabled" : "rngtech.miners_companion.block_chew_disabled"
        ), true);
    }

    private static void openMenu(ServerPlayer player, InteractionHand hand) {
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.rngtech.miners_companion");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new MinersCompanionMenu(containerId, inventory, hand);
            }
        };
        player.openMenu(provider, buffer -> buffer.writeBoolean(hand == InteractionHand.OFF_HAND));
    }

    private static final class InstalledCellEnergyStorage implements IEnergyStorage {
        private final ItemStack companionStack;

        private InstalledCellEnergyStorage(ItemStack companionStack) {
            this.companionStack = companionStack;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            IEnergyStorage cellStorage = cellStorage();
            if (cellStorage == null || !cellStorage.canReceive() || toReceive <= 0) {
                return 0;
            }
            int received = cellStorage.receiveEnergy(toReceive, simulate);
            if (!simulate && received > 0) {
                saveCell();
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            IEnergyStorage cellStorage = cellStorage();
            if (cellStorage == null || !cellStorage.canExtract() || toExtract <= 0) {
                return 0;
            }
            int extracted = cellStorage.extractEnergy(toExtract, simulate);
            if (!simulate && extracted > 0) {
                saveCell();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return BatteryCellItem.energyStored(cell());
        }

        @Override
        public int getMaxEnergyStored() {
            return BatteryCellItem.energyCapacity(cell());
        }

        @Override
        public boolean canExtract() {
            return BatteryCellItem.maxOutput(cell()) > 0;
        }

        @Override
        public boolean canReceive() {
            return BatteryCellItem.maxInput(cell()) > 0;
        }

        private IEnergyStorage cellStorage() {
            ItemStack cell = cell();
            return cell.isEmpty() ? null : BatteryCellItem.energyStorage(cell);
        }

        private ItemStack cell() {
            return state(companionStack).batteryCell();
        }

        private void saveCell() {
            MinersCompanionState state = state(companionStack);
            setState(companionStack, state.withBatteryCell(cell()));
        }
    }
}
