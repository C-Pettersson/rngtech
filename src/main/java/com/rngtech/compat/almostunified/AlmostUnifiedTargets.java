package com.rngtech.compat.almostunified;

import com.almostreliable.unified.api.AlmostUnified;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

import java.util.Optional;

/** AlmostUnified's chosen item for a tag. Only this class touches the AlmostUnified API, so it loads only with the mod. */
public final class AlmostUnifiedTargets {
    private static final boolean LOADED = ModList.get() != null && ModList.get().isLoaded("almostunified");

    public static Optional<Item> target(TagKey<Item> tag) {
        return LOADED ? Api.target(tag) : Optional.empty();
    }

    private static final class Api {
        static Optional<Item> target(TagKey<Item> tag) {
            if (!AlmostUnified.INSTANCE.isRuntimeLoaded()) {
                return Optional.empty();
            }
            return Optional.ofNullable(AlmostUnified.INSTANCE.getTagTargetItem(tag));
        }
    }

    private AlmostUnifiedTargets() {
    }
}
