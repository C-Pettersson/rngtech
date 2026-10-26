package com.rngtech.content.loot;

import com.rngtech.RNGTech;
import com.rngtech.content.item.GlyphItem;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.PhaseArrayRules;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;

/**
 * Where Glyphs come from by default: a woken Dormant Machine. Tier V has no default source; Work Orders add it.
 */
public final class GlyphSources {
    /** A woken site's Glyph tier. Unknown sites fall back to the soul's level. */
    private static final Map<ResourceLocation, Integer> SITE_TIERS = Map.of(
            RNGTech.id("collapsed_workshop"), 1,
            RNGTech.id("abandoned_cart_siding"), 1,
            RNGTech.id("buried_calibration_vault"), 2,
            RNGTech.id("flooded_foundry"), 2,
            RNGTech.id("nether_smeltworks"), 3,
            RNGTech.id("end_relay"), 4
    );

    private GlyphSources() {
    }

    public static int dormantTier(ResourceLocation site, int soulLevel) {
        Integer tier = SITE_TIERS.get(site);
        if (tier != null) {
            return tier;
        }
        if (soulLevel <= 25) {
            return 1;
        }
        if (soulLevel <= 45) {
            return 2;
        }
        return soulLevel <= 55 ? 3 : 4;
    }

    /** Drops the Glyph a woken Dormant Machine gives, on top of the site. */
    public static void dropDormantGlyph(Level level, BlockPos site, ResourceLocation siteId, int soulLevel) {
        int tier = PhaseArrayRules.clampGlyphTier(dormantTier(siteId, soulLevel));
        ItemStack glyph = GlyphItem.of(ModItems.GLYPH.get(), tier);
        Containers.dropItemStack(level, site.getX() + 0.5, site.getY() + 1.0, site.getZ() + 0.5, glyph);
    }
}
