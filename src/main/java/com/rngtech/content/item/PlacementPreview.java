package com.rngtech.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import java.util.function.Function;

/** The block state that using an item on a block would leave at {@code pos}, for the client placement ghost. */
public record PlacementPreview(BlockPos pos, BlockState state) {
    /** Items whose {@code useOn} decision can be asked without side effects. */
    public interface Source {
        /** Returns null when the use would fail or would not change a block. */
        @Nullable
        PlacementPreview placementPreview(UseOnContext context);
    }

    /** Mirrors {@link BlockItem#place} up to the point where it writes the block. */
    @Nullable
    static PlacementPreview placed(
            BlockItem item,
            BlockPlaceContext context,
            Function<BlockPlaceContext, BlockState> placementState
    ) {
        if (!item.getBlock().isEnabled(context.getLevel().enabledFeatures()) || !context.canPlace()) {
            return null;
        }
        BlockPlaceContext placeContext = item.updatePlacementContext(context);
        if (placeContext == null || placeContext.getLevel().isOutsideBuildHeight(placeContext.getClickedPos())) {
            return null;
        }
        BlockState state = placementState.apply(placeContext);
        return state == null ? null : new PlacementPreview(placeContext.getClickedPos(), state);
    }
}
