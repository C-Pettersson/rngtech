package com.rngtech.content.dormant;

import com.rngtech.content.blockentity.DormantMachineBlockEntity;
import com.rngtech.content.entity.DerelictCartEntity;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the rerolls the reset and restore commands mark. Sites in loaded chunks change at once; the rest change the
 * next time their chunk loads. A mined-out site comes back only on empty ground.
 */
public final class DormantSites {
    private static final int PENDING_INTERVAL = 100;

    public record Outcome(int changed, int waiting, List<BlockPos> skipped) {
    }

    private DormantSites() {
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % PENDING_INTERVAL != 0) {
            return;
        }
        DormantSiteIndex index = DormantSiteIndex.get(level.getServer());
        if (index.hasPending()) {
            apply(level, index, new ArrayList<>());
        }
    }

    /** Applies every pending reroll in loaded chunks across all dimensions. */
    public static Outcome applyLoaded(MinecraftServer server) {
        DormantSiteIndex index = DormantSiteIndex.get(server);
        List<BlockPos> skipped = new ArrayList<>();
        int changed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            changed += apply(level, index, skipped);
        }
        int waiting = 0;
        for (ServerLevel level : server.getAllLevels()) {
            waiting += index.pending(level.dimension()).size();
        }
        return new Outcome(changed, waiting, List.copyOf(skipped));
    }

    public static int apply(ServerLevel level, DormantSiteIndex index, List<BlockPos> skipped) {
        int changed = 0;
        for (DormantSiteIndex.Entry entry : index.pending(level.dimension())) {
            BlockPos pos = entry.pos().pos();
            if (!level.isLoaded(pos)) { continue; }
            if (entry.entity()) {
                if (applyToCart(level, entry, pos)) {
                    index.applied(entry.pos());
                    changed++;
                } else {
                    index.skipped(entry.pos());
                    skipped.add(pos);
                }
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof DormantMachineBlockEntity)) {
                if (!level.getBlockState(pos).isAir()) {
                    index.skipped(entry.pos());
                    skipped.add(pos);
                    continue;
                }
                level.setBlock(pos, ModBlocks.DORMANT_MACHINE.get().defaultBlockState(), Block.UPDATE_ALL);
            }
            if (level.getBlockEntity(pos) instanceof DormantMachineBlockEntity machine) {
                machine.core().reroll(entry);
                index.applied(entry.pos());
                changed++;
            }
        }
        return changed;
    }

    /** Rerolls the cart anchored at {@code pos}, or puts a new one there when the old cart was broken and the rail is free. */
    private static boolean applyToCart(ServerLevel level, DormantSiteIndex.Entry entry, BlockPos pos) {
        List<DerelictCartEntity> carts = level.getEntitiesOfClass(DerelictCartEntity.class, new AABB(pos).inflate(1.0D),
                cart -> cart.sitePos().equals(pos));
        DerelictCartEntity cart = carts.isEmpty() ? null : carts.getFirst();
        if (cart == null) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !(state.getBlock() instanceof BaseRailBlock)) { return false; }
            cart = spawnCart(level, pos, Direction.NORTH);
            level.addFreshEntity(cart);
        }
        cart.core().reroll(entry);
        return true;
    }

    /** A derelict cart standing on {@code pos}, anchored there so the site index can find it again. */
    public static DerelictCartEntity spawnCart(ServerLevel level, BlockPos pos, Direction facing) {
        DerelictCartEntity cart = new DerelictCartEntity(ModEntityTypes.DERELICT_CART.get(), level);
        cart.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, facing == null ? 0.0F : facing.toYRot(), 0.0F);
        cart.anchor(pos);
        return cart;
    }
}
