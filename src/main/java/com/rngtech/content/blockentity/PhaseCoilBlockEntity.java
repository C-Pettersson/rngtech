package com.rngtech.content.blockentity;

import com.rngtech.content.block.PhaseCoilBlock;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineBaseStatCatalog;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.PhaseArrayRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** A Phase Coil: rolled traits like any Stage 8 part, plus the Glyph tier inscribed into it. */
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

    /** The inscribed Glyph tier, 1 to {@link PhaseArrayRules#GLYPH_TIERS}, or 0 while blank. */
    public int coilTier() {
        Integer tier = components().get(ModDataComponents.COIL_TIER.get());
        return tier == null ? 0 : tier;
    }

    public boolean isBlank() {
        return coilTier() == 0;
    }

    /** Inscribes a Glyph tier for good. A Coil never goes back to blank, and a lower tier never replaces a higher one. */
    public boolean inscribe(int tier) {
        int clamped = PhaseArrayRules.clampGlyphTier(tier);
        if (clamped <= coilTier()) {
            return false;
        }
        setComponents(DataComponentMap.builder()
                .addAll(components())
                .set(ModDataComponents.COIL_TIER.get(), clamped)
                .build());
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(PhaseCoilBlock.TIER) && state.getValue(PhaseCoilBlock.TIER) != clamped) {
                level.setBlock(worldPosition, state.setValue(PhaseCoilBlock.TIER, clamped), Block.UPDATE_ALL);
            }
            PhaseLine.requestNearbyCradleRescans(level, worldPosition);
        }
        return true;
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
