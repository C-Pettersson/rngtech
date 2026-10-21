package com.rngtech.content.blockentity;

import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** A Phase Emitter: the start of a line, with rolled traits like any Stage 8 part. */
public class PhaseEmitterBlockEntity extends BaseMachineBlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    public PhaseEmitterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.PHASE_EMITTER.get(), pos, blockState, MachineType.PHASE_EMITTER, 0, 0, 0);
    }

    public MachineStatAccumulator effectiveStats() {
        return cachedStats(0L);
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.phaseEmitter();
        stats.apply(effectiveTraits());
        return stats;
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return inventory;
    }
}
