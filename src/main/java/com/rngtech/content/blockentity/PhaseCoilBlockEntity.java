package com.rngtech.content.blockentity;

import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** A Phase Coil: rolled traits like any Stage 8 part. */
public class PhaseCoilBlockEntity extends BaseMachineBlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    public PhaseCoilBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.PHASE_COIL.get(), pos, blockState, MachineType.PHASE_COIL, 0, 0, 0);
    }

    public MachineStatAccumulator effectiveStats() {
        return cachedStats(0L);
    }

    @Override
    protected MachineStatAccumulator buildStats() {
        MachineStatAccumulator stats = MachineBaseStatCatalog.phaseCoil();
        stats.apply(effectiveTraits());
        return stats;
    }


    // -- Client sync: the Cradle screen lists a part's affixes from its client-side block entity -----------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        MachineTraits traits = machineTraits();
        if (!traits.isEmpty()) {
            MachineTraits.CODEC.encodeStart(NbtOps.INSTANCE, traits).result().ifPresent(encoded -> tag.put("Traits", encoded));
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("Traits")) {
            MachineTraits.CODEC.parse(NbtOps.INSTANCE, tag.get("Traits")).result().ifPresent(this::setMachineTraits);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected ItemStackHandler getMachineInventory() {
        return inventory;
    }
}
