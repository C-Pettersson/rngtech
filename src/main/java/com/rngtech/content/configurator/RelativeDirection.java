package com.rngtech.content.configurator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RelativeDirection(int kind, int directionOrdinal) {
    private static final int KIND_NONE = 0;
    private static final int KIND_DEFAULT = 1;
    private static final int KIND_ABSOLUTE = 2;

    public static final RelativeDirection NONE = new RelativeDirection(KIND_NONE, -1);
    public static final RelativeDirection DEFAULT = new RelativeDirection(KIND_DEFAULT, -1);

    public static final Codec<RelativeDirection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(KIND_NONE, KIND_ABSOLUTE).fieldOf("kind").forGetter(RelativeDirection::kind),
            Codec.INT.optionalFieldOf("direction", -1).forGetter(RelativeDirection::directionOrdinal)
    ).apply(instance, RelativeDirection::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RelativeDirection> STREAM_CODEC =
            StreamCodec.ofMember(RelativeDirection::write, RelativeDirection::read);

    public RelativeDirection {
        if (kind < KIND_NONE || kind > KIND_ABSOLUTE) {
            kind = KIND_DEFAULT;
        }
        if (kind != KIND_ABSOLUTE || directionOrdinal < 0 || directionOrdinal >= Direction.values().length) {
            directionOrdinal = -1;
        }
    }

    public static RelativeDirection capture(Direction direction, Direction defaultDirection) {
        if (direction == null) {
            return NONE;
        }
        if (direction == defaultDirection) {
            return DEFAULT;
        }
        return absolute(direction);
    }

    public static RelativeDirection absolute(Direction direction) {
        return direction == null ? NONE : new RelativeDirection(KIND_ABSOLUTE, direction.ordinal());
    }

    public Direction resolve(Direction defaultDirection) {
        if (kind == KIND_NONE) {
            return null;
        }
        if (kind == KIND_DEFAULT) {
            return defaultDirection;
        }
        Direction[] values = Direction.values();
        return directionOrdinal >= 0 && directionOrdinal < values.length ? values[directionOrdinal] : defaultDirection;
    }

    public RelativeDirection resolvedAgainst(Direction defaultDirection) {
        Direction resolved = resolve(defaultDirection);
        return resolved == null ? NONE : absolute(resolved);
    }

    public boolean isNone() {
        return kind == KIND_NONE;
    }

    public boolean isDefault() {
        return kind == KIND_DEFAULT;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        ByteBufCodecs.VAR_INT.encode(buffer, kind);
        ByteBufCodecs.VAR_INT.encode(buffer, directionOrdinal);
    }

    private static RelativeDirection read(RegistryFriendlyByteBuf buffer) {
        return new RelativeDirection(ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer));
    }
}
