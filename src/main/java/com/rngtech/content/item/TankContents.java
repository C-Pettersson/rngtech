package com.rngtech.content.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/** The loose and compressed fluid a broken Fluid or Compressor Tank carries on its item. */
public record TankContents(SimpleFluidContent loose, SimpleFluidContent compressed) {
    public static final Codec<TankContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SimpleFluidContent.CODEC.optionalFieldOf("loose", SimpleFluidContent.EMPTY).forGetter(TankContents::loose),
            SimpleFluidContent.CODEC.optionalFieldOf("compressed", SimpleFluidContent.EMPTY).forGetter(TankContents::compressed)
    ).apply(instance, TankContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TankContents> STREAM_CODEC = StreamCodec.composite(
            SimpleFluidContent.STREAM_CODEC,
            TankContents::loose,
            SimpleFluidContent.STREAM_CODEC,
            TankContents::compressed,
            TankContents::new
    );

    public static TankContents of(FluidStack loose, FluidStack compressed) {
        return new TankContents(SimpleFluidContent.copyOf(loose), SimpleFluidContent.copyOf(compressed));
    }

    public boolean isEmpty() {
        return loose.isEmpty() && compressed.isEmpty();
    }
}
