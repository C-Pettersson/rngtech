package com.rngtech.content.recipe;

import com.rngtech.RNGTech;
import com.rngtech.compat.almostunified.AlmostUnifiedTargets;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * A recipe result written as an item stack or as an item tag. A tag result lets one recipe output whichever mod's item
 * a pack has, such as {@code c:dusts/quartz}. It resolves once item tags are bound: AlmostUnified's chosen item when
 * that mod is loaded, else a vanilla item, then an RNGTech item, then the first entry of the tag.
 */
public final class TaggedRecipeOutput {
    private static final List<String> PREFERRED_NAMESPACES = List.of("minecraft", RNGTech.MOD_ID);

    private static final Codec<TaggedRecipeOutput> TAG_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(output -> output.tag),
                    Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(output -> output.count)
            )
            .apply(instance, TaggedRecipeOutput::new));

    public static final Codec<TaggedRecipeOutput> CODEC = Codec.either(ItemStack.STRICT_CODEC, TAG_CODEC)
            .xmap(
                    either -> either.map(TaggedRecipeOutput::of, output -> output),
                    output -> output.tag == null ? Either.left(output.stack) : Either.right(output)
            );

    private final ItemStack stack;
    private final TagKey<Item> tag;
    private final int count;
    private volatile ItemStack resolved;

    private TaggedRecipeOutput(ItemStack stack, TagKey<Item> tag, int count) {
        this.stack = stack;
        this.tag = tag;
        this.count = count;
    }

    private TaggedRecipeOutput(TagKey<Item> tag, int count) {
        this(ItemStack.EMPTY, tag, count);
    }

    public static TaggedRecipeOutput of(ItemStack stack) {
        return new TaggedRecipeOutput(stack, null, stack.getCount());
    }

    /** The result stack, or empty while the tag is unbound or holds no item. Callers copy it before changing it. */
    public ItemStack stack() {
        if (tag == null) {
            return stack;
        }
        ItemStack cached = resolved;
        if (cached != null) {
            return cached;
        }
        ItemStack found = BuiltInRegistries.ITEM.getTag(tag)
                .flatMap(TaggedRecipeOutput::preferredItem)
                .map(item -> new ItemStack(item, count))
                .orElse(ItemStack.EMPTY);
        if (!found.isEmpty()) {
            resolved = found;
        }
        return found;
    }

    private static Optional<Holder<Item>> preferredItem(HolderSet.Named<Item> items) {
        Optional<Item> unified = AlmostUnifiedTargets.target(items.key());
        if (unified.isPresent() && items.contains(unified.get().builtInRegistryHolder())) {
            return Optional.of(unified.get().builtInRegistryHolder());
        }
        for (String namespace : PREFERRED_NAMESPACES) {
            for (Holder<Item> item : items) {
                if (item.unwrapKey().map(key -> key.location().getNamespace().equals(namespace)).orElse(false)) {
                    return Optional.of(item);
                }
            }
        }
        return items.stream().findFirst();
    }
}
