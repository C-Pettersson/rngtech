package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;

import java.util.Objects;

public record MachineModifierEffect(
        MachineStat stat,
        ModifierOperation operation,
        ModifierValueRange range,
        double value
) {
    public static final Codec<MachineModifierEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MachineStat.CODEC.fieldOf("stat").forGetter(MachineModifierEffect::stat),
            ModifierOperation.CODEC.fieldOf("operation").forGetter(MachineModifierEffect::operation),
            ModifierValueRange.CODEC.fieldOf("range").forGetter(MachineModifierEffect::range),
            Codec.DOUBLE.fieldOf("value").forGetter(MachineModifierEffect::value)
    ).apply(instance, MachineModifierEffect::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineModifierEffect> STREAM_CODEC = StreamCodec.composite(
            MachineStat.STREAM_CODEC,
            MachineModifierEffect::stat,
            ModifierOperation.STREAM_CODEC,
            MachineModifierEffect::operation,
            ModifierValueRange.STREAM_CODEC,
            MachineModifierEffect::range,
            ByteBufCodecs.DOUBLE,
            MachineModifierEffect::value,
            MachineModifierEffect::new
    );

    public MachineModifierEffect {
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(operation, "operation");
        range = Objects.requireNonNull(range, "range");
    }

    public static MachineModifierEffect fixed(MachineStat stat, ModifierOperation operation, double value) {
        return new MachineModifierEffect(stat, operation, ModifierValueRange.fixed(value), value);
    }

    public static MachineModifierEffect roll(
            MachineStat stat,
            ModifierOperation operation,
            ModifierValueRange range,
            RandomSource random
    ) {
        return new MachineModifierEffect(stat, operation, range, range.roll(random));
    }
}
