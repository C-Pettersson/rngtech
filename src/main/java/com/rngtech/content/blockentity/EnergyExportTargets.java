package com.rngtech.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The FE storages next to a machine that pushes energy out, one {@link BlockCapabilityCache} per side, so an export
 * only looks a neighbour up again after NeoForge invalidates it: a block entity there is placed, removed or reloaded,
 * its chunk loads or unloads, or the block calls {@code invalidateCapabilities}.
 */
final class EnergyExportTargets {
    private static final Direction[] SIDES = Direction.values();

    private final BlockEntity owner;
    @SuppressWarnings("unchecked")
    private final BlockCapabilityCache<IEnergyStorage, Direction>[] caches = new BlockCapabilityCache[SIDES.length];
    private int lookups;

    EnergyExportTargets(BlockEntity owner) {
        this.owner = owner;
    }

    /** The storage on the block at {@code side} of {@code pos}, seen from the machine, or {@code null}. */
    IEnergyStorage get(Level level, BlockPos pos, Direction side) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(side), side.getOpposite());
        }
        BlockCapabilityCache<IEnergyStorage, Direction> cache = caches[side.ordinal()];
        if (cache == null || cache.level() != serverLevel || !isNeighbour(cache.pos(), pos, side)) {
            cache = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, serverLevel, pos.relative(side), side.getOpposite(),
                    () -> !owner.isRemoved(), () -> lookups++);
            caches[side.ordinal()] = cache;
            lookups++;
        }
        return cache.getCapability();
    }

    private static boolean isNeighbour(BlockPos target, BlockPos pos, Direction side) {
        return target.getX() == pos.getX() + side.getStepX()
                && target.getY() == pos.getY() + side.getStepY()
                && target.getZ() == pos.getZ() + side.getStepZ();
    }

    /** How many caches were created or invalidated, for checks that exports stop looking neighbours up. */
    int lookups() {
        return lookups;
    }
}
