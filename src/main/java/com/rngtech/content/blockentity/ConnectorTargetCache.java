package com.rngtech.content.blockentity;

import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.cable.CableStats;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

/**
 * One connector's capability lookup on the block it is attached to, kept until NeoForge invalidates it: the block
 * entity is placed, removed or reloaded, its chunk loads or unloads, or the block calls
 * {@code invalidateCapabilities}. Cables and Universal Connectors are never targets, and an unloaded target reads as
 * absent without loading its chunk.
 */
final class ConnectorTargetCache<T> {
    private final BlockCapability<T, Direction> capability;
    private BlockCapabilityCache<T, Direction> cache;
    private boolean targetChecked;
    private boolean targetIsNetworkBlock;

    ConnectorTargetCache(BlockCapability<T, Direction> capability) {
        this.capability = capability;
    }

    T get(Level level, BlockPos pos, Direction side) {
        if (level == null || pos == null || side == null) {
            return null;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return level.isLoaded(pos) && !isNetworkBlock(level.getBlockState(pos))
                    ? level.getCapability(capability, pos, side)
                    : null;
        }
        if (cache == null || cache.level() != serverLevel || cache.context() != side || !cache.pos().equals(pos)) {
            cache = BlockCapabilityCache.create(capability, serverLevel, pos, side, () -> true, () -> targetChecked = false);
            targetChecked = false;
        }
        CableStats.increment(CableStats.Counter.TARGET_QUERIES);
        T target = cache.getCapability();
        if (!targetChecked) {
            CableStats.increment(CableStats.Counter.TARGET_LOOKUPS);
            targetIsNetworkBlock = serverLevel.isLoaded(pos) && isNetworkBlock(serverLevel.getBlockState(pos));
            targetChecked = true;
        }
        return targetIsNetworkBlock ? null : target;
    }

    private static boolean isNetworkBlock(BlockState state) {
        return state.getBlock() instanceof CableBlock || state.getBlock() instanceof UniversalConnectorBlock;
    }
}
