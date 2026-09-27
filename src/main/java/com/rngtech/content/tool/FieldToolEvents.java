package com.rngtech.content.tool;

import com.rngtech.content.item.ModularToolItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;
import java.util.List;

public final class FieldToolEvents {
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getLevel().isClientSide
                || !(stack.getItem() instanceof ModularToolItem tool)
                || ModularToolItem.isBroken(stack)) {
            return;
        }
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.ABORT) {
            ModularToolItem.clearActiveUse(player, stack);
            ModularToolItem.forgetAreaHit(player, pos);
            return;
        }
        ModularToolItem.markActiveUse(player, stack);
        if (!tool.family().minesArea()) {
            return;
        }
        Direction face = event.getFace();
        if (face != null) {
            ModularToolItem.rememberAreaHit(player, pos, face);
        }
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        if (!ModularToolItem.isModularTool(stack)
                || ModularToolItem.isBroken(stack)) {
            return;
        }
        ModularToolItem.markActiveUse(event.getEntity(), stack);
        if (!ToolBaseStatCatalog.hasOreBurstMode(stack)) {
            return;
        }
        BlockState state = event.getState();
        if (!state.is(FieldToolTags.ORE_BURST_TARGETS)
                || !stack.isCorrectToolForDrops(state)
                || !ModularToolItem.canSpendEnergy(stack, ToolBaseStatCatalog.oreBurstFeUsage(stack))) {
            return;
        }
        double speedBonus = ToolBaseStatCatalog.oreBurstSpeed(stack);
        if (speedBonus > 0.0) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), (float) (event.getOriginalSpeed() * (1.0 + speedBonus))));
        }
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        if (!(event.getBreaker() instanceof Player)) {
            return;
        }
        if (OreHardness.hasHardness(event.getState())
                && (!event.getTool().isCorrectToolForDrops(event.getState())
                || !OreHardness.canHarvest(event.getTool(), event.getState()))) {
            event.getDrops().clear();
            return;
        }
        if (!ModularToolItem.isModularTool(event.getTool()) || ModularToolItem.isBroken(event.getTool())) {
            return;
        }
        if (hasSilkTouch(level, event.getTool())) {
            return;
        }
        int luck = ToolBaseStatCatalog.luck(event.getTool());
        if (luck <= 0 || !isLuckyTarget(event.getState())) {
            return;
        }
        List<ItemEntity> extras = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            int bonusCount = bonusCount(level, luck);
            if (bonusCount <= 0) {
                continue;
            }
            ItemStack extraStack = stack.copy();
            extraStack.setCount(Math.min(extraStack.getMaxStackSize(), bonusCount));
            BlockPos pos = event.getPos();
            extras.add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, extraStack));
        }
        event.getDrops().addAll(extras);
    }

    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        FieldToolEnchantments.mergeBookInAnvil(
                        event.getLeft(),
                        event.getRight(),
                        event.getName(),
                        event.getPlayer()
                )
                .ifPresent(result -> {
                    if (result.blocked()) {
                        event.setCanceled(true);
                    } else {
                        event.setOutput(result.output());
                        event.setCost(result.cost());
                        event.setMaterialCost(result.materialCost());
                    }
                });
    }

    public static void onGrindstonePlace(GrindstoneEvent.OnPlaceItem event) {
        FieldToolEnchantments.stripWithGrindstone(event.getTopItem(), event.getBottomItem())
                .ifPresent(result -> {
                    event.setOutput(result.output());
                    event.setXp((int) result.cost());
                });
    }

    private static boolean hasSilkTouch(ServerLevel level, ItemStack stack) {
        Holder<Enchantment> silkTouch = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        return stack.getEnchantmentLevel(silkTouch) > 0;
    }

    private static boolean isLuckyTarget(BlockState state) {
        return state.is(FieldToolTags.ORE_BURST_TARGETS) || state.is(net.minecraft.tags.BlockTags.LOGS);
    }

    private static int bonusCount(ServerLevel level, int luck) {
        int bonus = 0;
        for (int i = 0; i < Math.min(luck, 5); i++) {
            if (level.random.nextFloat() < 0.15F) {
                bonus++;
            }
        }
        return bonus;
    }

    private FieldToolEvents() {
    }
}
