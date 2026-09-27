package com.rngtech.content.minerscompanion;

import com.rngtech.content.item.MinersCompanionItem;
import com.rngtech.content.item.PotentialReactorPartItem;
import com.rngtech.rpg.ComponentBaseStatCatalog;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class MinersCompanionEvents {
    private static final double MAGNET_RANGE = 6.0D;
    private static final double MAGNET_PICKUP_DISTANCE = 1.25D;
    private static final double MAGNET_BASE_SPEED = 0.12D;
    private static final double MAGNET_MAX_SPEED = 0.34D;

    public static void onBlockDrops(BlockDropsEvent event) {
        ServerLevel level = event.getLevel();
        if (!(event.getBreaker() instanceof Player player)) {
            return;
        }
        List<ItemStack> companions = activeCompanions(player);
        if (companions.isEmpty()) {
            return;
        }

        boolean changed = false;
        Item brokenBlockItem = event.getState().getBlock().asItem();
        Iterator<ItemEntity> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemEntity drop = iterator.next();
            ItemStack dropStack = drop.getItem();
            if (dropStack.isEmpty()) {
                continue;
            }

            int remaining = dropStack.getCount();
            for (ItemStack companion : companions) {
                if (remaining <= 0) {
                    break;
                }
                if (!MinersCompanionItem.hasMatchingFilter(companion, dropStack, brokenBlockItem)) {
                    continue;
                }
                int chewed = MinersCompanionItem.chewCount(companion, remaining);
                if (chewed <= 0) {
                    continue;
                }
                remaining -= chewed;
                changed = true;
                rollRecoveries(level, player, event.getPos(), companion, dropStack, chewed);
            }

            if (remaining <= 0) {
                iterator.remove();
            } else if (remaining != dropStack.getCount()) {
                dropStack.setCount(remaining);
            }
        }

        if (changed) {
            player.getInventory().setChanged();
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }

        boolean changed = false;
        for (ItemStack companion : companionStacks(player)) {
            MinersCompanionState state = MinersCompanionItem.state(companion);
            if (state.magnetEnabled() && state.hasValidMagnetGear()
                    && MinersCompanionItem.consumeEnergy(companion, MinersCompanionItem.MAGNET_PASSIVE_FE_PER_TICK)) {
                changed = true;
                changed |= pullNearbyItems(player, companion);
            }
            state = MinersCompanionItem.state(companion);
            if (state.miningLampEnabled()
                    && state.hasValidMiningLampGear()
                    && MinersCompanionItem.consumeEnergy(companion, MinersCompanionItem.MINING_LAMP_FE_PER_TICK)) {
                changed = true;
            }
        }

        if (changed) {
            player.getInventory().setChanged();
        }
    }

    private static List<ItemStack> activeCompanions(Player player) {
        List<ItemStack> active = new ArrayList<>();
        for (ItemStack stack : companionStacks(player)) {
            if (MinersCompanionItem.isEnabledAndReady(stack)) {
                active.add(stack);
            }
        }
        return active;
    }

    private static List<ItemStack> companionStacks(Player player) {
        List<ItemStack> companions = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (MinersCompanionItem.isMinersCompanion(stack)) {
                companions.add(stack);
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (MinersCompanionItem.isMinersCompanion(offhand)) {
            companions.add(offhand);
        }
        return companions;
    }

    private static boolean pullNearbyItems(ServerPlayer player, ItemStack companion) {
        ServerLevel level = player.serverLevel();
        AABB searchArea = player.getBoundingBox().inflate(MAGNET_RANGE);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, searchArea, drop -> canMagnetPull(player, drop));
        boolean moved = false;
        for (ItemEntity drop : drops) {
            if (!MinersCompanionItem.consumeEnergy(companion, MinersCompanionItem.MAGNET_FE_PER_ITEM_ENTITY)) {
                break;
            }
            pullItem(player, drop);
            moved = true;
        }
        return moved;
    }

    private static boolean canMagnetPull(ServerPlayer player, ItemEntity drop) {
        if (!drop.isAlive() || drop.getItem().isEmpty()) {
            return false;
        }
        UUID target = drop.getTarget();
        return (target == null || target.equals(player.getUUID())) && drop.distanceToSqr(player) <= MAGNET_RANGE * MAGNET_RANGE;
    }

    private static void pullItem(ServerPlayer player, ItemEntity drop) {
        Vec3 target = player.position().add(0.0D, player.getBbHeight() * 0.35D, 0.0D);
        Vec3 delta = target.subtract(drop.position());
        double distance = delta.length();
        if (distance <= MAGNET_PICKUP_DISTANCE) {
            drop.playerTouch(player);
            return;
        }
        double pullRatio = 1.0D - Math.min(distance / MAGNET_RANGE, 1.0D);
        double speed = MAGNET_BASE_SPEED + (MAGNET_MAX_SPEED - MAGNET_BASE_SPEED) * pullRatio;
        Vec3 velocity = delta.normalize().scale(speed);
        drop.setDeltaMovement(drop.getDeltaMovement().scale(0.55D).add(velocity));
        drop.hasImpulse = true;
    }

    private static void rollRecoveries(
            ServerLevel level,
            Player player,
            BlockPos pos,
            ItemStack companion,
            ItemStack chewedStack,
            int chewedCount
    ) {
        MinersCompanionState state = MinersCompanionItem.state(companion);
        ItemStack filter = state.recoveryFilter();
        if (!(filter.getItem() instanceof PotentialReactorPartItem part) || part.filterMaterial() == null) {
            return;
        }
        int filterStage = part.stage();
        int processingLevel = state.processingLevel();
        double filterEfficiency = filterEfficiency(filter);
        ItemStack sample = chewedStack.copyWithCount(1);
        for (int index = 0; index < chewedCount; index++) {
            ItemStack recovered = MinersCompanionRecoveryRecipes.rollRecovery(
                    level,
                    level.random,
                    sample,
                    filterStage,
                    processingLevel,
                    filterEfficiency
            );
            if (!recovered.isEmpty()) {
                giveOrDrop(level, player, pos, recovered);
            }
        }
    }

    private static double filterEfficiency(ItemStack filter) {
        MachineStatAccumulator stats = ComponentBaseStatCatalog.effectiveStats(filter);
        return stats == null ? 1.0D : stats.value(MachineStat.EFFICIENCY);
    }

    private static void giveOrDrop(ServerLevel level, Player player, BlockPos pos, ItemStack stack) {
        ItemStack remaining = stack.copy();
        player.getInventory().add(remaining);
        if (remaining.isEmpty()) {
            return;
        }
        level.addFreshEntity(new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                remaining
        ));
    }

    private MinersCompanionEvents() {
    }
}
