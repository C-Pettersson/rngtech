package com.rngtech.content.wrench;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record WrenchOverlayTarget(BlockPos pos, int sideOrdinal) {
    public static final int STANDALONE_SIDE = -1;
    public static final StreamCodec<RegistryFriendlyByteBuf, WrenchOverlayTarget> STREAM_CODEC =
            StreamCodec.ofMember(WrenchOverlayTarget::write, WrenchOverlayTarget::read);

    public WrenchOverlayTarget(BlockPos pos, Direction side) {
        this(pos, side == null ? STANDALONE_SIDE : side.ordinal());
    }

    public boolean isStandalone() {
        return sideOrdinal == STANDALONE_SIDE;
    }

    public Direction side() {
        Direction[] values = Direction.values();
        return sideOrdinal >= 0 && sideOrdinal < values.length ? values[sideOrdinal] : null;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeByte(sideOrdinal);
    }

    private static WrenchOverlayTarget read(RegistryFriendlyByteBuf buffer) {
        return new WrenchOverlayTarget(buffer.readBlockPos(), buffer.readByte());
    }
}
