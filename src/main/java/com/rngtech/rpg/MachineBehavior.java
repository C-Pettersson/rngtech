package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum MachineBehavior implements StringRepresentable {
    FUEL_GOVERNOR,
    QUICK_FEED,
    FUEL_RESERVE,
    BLOCK_FEED,
    ALLOY_BLEND,
    CELL_LEAKAGE_DAMPING,
    CHARGE_BALANCER,
    BURST_RELEASE,
    /** Retired: batching needs no behavior. Kept so stored traits that name it still decode. */
    DENSE_PARALLEL,
    BULK_SPEED,
    OUTPUT_GUARD,
    THERMAL_BUFFER,
    POWER_GRACE,
    RESIDUE_SCREEN,
    SIDE_FLUID_OUTPUT,
    AUTO_PURGE,
    ESCAPEMENT,
    REFLUX,
    ECHO_STREAK;

    public static final Codec<MachineBehavior> CODEC = StringRepresentable.fromEnum(MachineBehavior::values);
    public static final StreamCodec<ByteBuf, MachineBehavior> STREAM_CODEC =
            ByteBufCodecs.idMapper(MachineBehavior::byId, MachineBehavior::ordinal);
    private static final MachineBehavior[] VALUES = values();

    private final String serializedName;

    MachineBehavior() {
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static MachineBehavior byId(int id) {
        return VALUES[id];
    }

    public String translationKey() {
        return "rngtech.behavior." + serializedName;
    }

    public String descriptionKey() {
        return translationKey() + ".description";
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
