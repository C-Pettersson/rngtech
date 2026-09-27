package com.rngtech.content.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record ComponentRecyclingOutput(ItemStack stack, boolean requiresFilter) {
    public static final Codec<ComponentRecyclingOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("stack").forGetter(ComponentRecyclingOutput::stack),
            Codec.BOOL.fieldOf("requires_filter").orElse(false).forGetter(ComponentRecyclingOutput::requiresFilter)
    ).apply(instance, ComponentRecyclingOutput::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ComponentRecyclingOutput> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            ComponentRecyclingOutput::stack,
            ByteBufCodecs.BOOL,
            ComponentRecyclingOutput::requiresFilter,
            ComponentRecyclingOutput::new
    );

    public ItemStack copyStack() {
        return stack.copy();
    }
}
