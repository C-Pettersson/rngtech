package com.rngtech.content.tool;

import com.rngtech.RNGTech;
import com.rngtech.RNGTechConfig;
import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.registry.ModDataMaps;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class OreHardness {
    public static final int MAX_LEVEL = 8;
    private static final List<TagKey<Block>> HARDNESS_TAGS = createHardnessTags();

    public static TagKey<Block> tagForLevel(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Ore hardness level out of range: " + level);
        }
        return HARDNESS_TAGS.get(level - 1);
    }

    public static int level(BlockState state) {
        for (int level = MAX_LEVEL; level >= 1; level--) {
            if (state.is(tagForLevel(level))) {
                return level;
            }
        }
        return 0;
    }

    public static boolean hasHardness(BlockState state) {
        return level(state) > 0;
    }

    public static boolean canHarvest(ItemStack stack, BlockState state) {
        int level = level(state);
        return level <= 0 || oreReach(stack) >= level;
    }

    /**
     * Modular tools reach their head's mining level. Other items use their {@code rngtech:ore_reach} data map entry,
     * or else the vanilla bridge policy applied to the vanilla tier they harvest at.
     */
    public static int oreReach(ItemStack stack) {
        if (stack.getItem() instanceof ModularToolItem) {
            return ToolBaseStatCatalog.miningLevel(stack);
        }
        if (RestrictedTools.useless(stack)) {
            return 0;
        }
        OreReach reach = stack.getItemHolder().getData(ModDataMaps.ORE_REACH);
        if (reach != null) {
            return reach.level();
        }
        return vanillaBridgeReach(stack);
    }

    private static int vanillaBridgeReach(ItemStack stack) {
        int tier = vanillaPickTier(stack);
        if (tier <= 0) {
            return 0;
        }
        return switch (RNGTechConfig.ORE_VANILLA_BRIDGE_POLICY.get()) {
            case BOOTSTRAP_ONLY -> 1;
            case EARLY_BRIDGE -> tier >= 2 ? 2 : 1;
            case VANILLA_TIER_MAPPING -> {
                if (tier >= 4) {
                    yield 5;
                }
                if (tier == 3) {
                    yield 4;
                }
                if (tier == 2) {
                    yield 2;
                }
                yield 1;
            }
            case MODULAR_ONLY -> 0;
        };
    }

    /**
     * The vanilla pickaxe tier an item harvests at, read from the vanilla blocks it can drop: 1 stone, 2 iron,
     * 3 diamond, 4 netherite. Vanilla has no block only netherite harvests, so only the Netherite Pickaxe reads as 4.
     */
    public static int vanillaPickTier(ItemStack stack) {
        if (stack.is(Items.NETHERITE_PICKAXE)) {
            return 4;
        }
        if (!stack.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState())) {
            return 0;
        }
        if (stack.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())) {
            return 3;
        }
        if (stack.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())) {
            return 2;
        }
        return 1;
    }

    private static List<TagKey<Block>> createHardnessTags() {
        List<TagKey<Block>> tags = new ArrayList<>();
        for (int level = 1; level <= MAX_LEVEL; level++) {
            ResourceLocation id = RNGTech.id("ore_hardness/level_" + level);
            tags.add(TagKey.create(Registries.BLOCK, id));
        }
        return List.copyOf(tags);
    }

    private OreHardness() {
    }
}
