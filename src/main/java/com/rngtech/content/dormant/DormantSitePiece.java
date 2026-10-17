package com.rngtech.content.dormant;

import com.rngtech.content.block.DormantMachineBlock;
import com.rngtech.content.blockentity.DormantMachineBlockEntity;
import com.rngtech.content.entity.DerelictCartEntity;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Builds one Dormant Machine site from code. Wear is a hash of the world position, so a ruin that spans chunks decays
 * the same way in each of them.
 */
public class DormantSitePiece extends StructurePiece {
    private final DormantLayout layout;
    private final ResourceLocation site;

    public DormantSitePiece(DormantLayout layout, ResourceLocation site, int x, int y, int z, Direction facing) {
        super(ModStructures.DORMANT_SITE_PIECE.get(), 0, makeBoundingBox(x, y, z, facing, layout.width(), layout.height(), layout.depth()));
        this.layout = layout;
        this.site = site;
        setOrientation(facing);
    }

    public DormantSitePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.DORMANT_SITE_PIECE.get(), tag);
        this.layout = DormantLayout.valueOf(tag.getString("Layout"));
        ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("Site"));
        this.site = parsed == null ? DormantSiteCatalog.DEFAULT_SITE : parsed;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Layout", layout.name());
        tag.putString("Site", site.toString());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        switch (layout) {
            case WORKSHOP -> workshop(level, box);
            case SIDING -> siding(level, box);
            case VAULT -> vault(level, box);
            case FOUNDRY -> foundry(level, box);
            case SMELTWORKS -> smeltworks(level, box);
            case RELAY -> relay(level, box);
        }
    }

    /** A roofless stone-and-timber shed with a fallen chimney. */
    private void workshop(WorldGenLevel level, BoundingBox box) {
        int max = layout.width() - 1;
        generateAirBox(level, box, 0, 1, 0, max, layout.height() - 1, max);
        for (int x = 0; x <= max; x++) {
            for (int z = 0; z <= max; z++) {
                fillColumnDown(level, Blocks.COBBLESTONE.defaultBlockState(), x, -1, z, box);
                placeBlock(level, pick(x, 0, z, Blocks.COBBLESTONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
                        Blocks.GRAVEL.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState()), x, 0, z, box);
            }
        }
        for (int y = 1; y <= 3; y++) {
            int wear = y == 1 ? 10 : y == 2 ? 35 : 60;
            for (int i = 1; i <= max - 1; i++) {
                wall(level, box, i, y, 1, wear);
                wall(level, box, i, y, max - 1, wear);
                wall(level, box, 1, y, i, wear);
                wall(level, box, max - 1, y, i, wear);
            }
        }
        generateAirBox(level, box, 4, 1, 1, 6, 2, 1);
        for (int[] post : new int[][] {{1, 1}, {max - 1, 1}, {1, max - 1}, {max - 1, max - 1}}) {
            for (int y = 1; y <= 4; y++) {
                if (y < 4 || !worn(post[0], y, post[1], 50)) {
                    placeBlock(level, Blocks.OAK_LOG.defaultBlockState(), post[0], y, post[1], box);
                }
            }
        }
        // The chimney stands in one back corner, and its top lies across the floor where it fell.
        for (int y = 1; y <= 6; y++) {
            if (y <= 3 || !worn(max - 2, y, max - 2, 40)) {
                placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), max - 2, y, max - 2, box);
            }
        }
        for (int x = 2; x <= 5; x++) {
            ruinBlock(level, box, pick(x, 1, max - 2, Blocks.COBBLESTONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState()), x, 1, max - 2, 25);
        }
        BlockState beam = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        for (int x = 2; x <= max - 2; x++) {
            ruinBlock(level, box, beam, x, 4, 4, 55);
        }
        placeBlock(level, Blocks.IRON_BARS.defaultBlockState(), 1, 2, 5, box);
        placeBlock(level, Blocks.DAMAGED_ANVIL.defaultBlockState(), 3, 1, 3, box);
        placeBlock(level, Blocks.COBWEB.defaultBlockState(), 2, 3, max - 2, box);
        placeBlock(level, Blocks.COBWEB.defaultBlockState(), max - 2, 2, 2, box);
        machine(level, box, 5, 1, 5);
    }

    private void wall(WorldGenLevel level, BoundingBox box, int x, int y, int z, int wear) {
        ruinBlock(level, box, pick(x, y, z, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(),
                Blocks.MOSSY_STONE_BRICKS.defaultBlockState()), x, y, z, wear);
    }

    /** A short dead-end rail tunnel with timber supports; the machine sits where the rails stop. */
    private void siding(WorldGenLevel level, BoundingBox box) {
        int end = layout.depth() - 1;
        generateAirBox(level, box, 1, 1, 0, 3, 3, end - 1);
        for (int z = 0; z < end; z++) {
            for (int x = 1; x <= 3; x++) {
                placeBlock(level, pick(x, 0, z, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(),
                        Blocks.COBBLESTONE.defaultBlockState()), x, 0, z, box);
            }
        }
        for (int z = 1; z < end; z += 4) {
            placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 1, 1, z, box);
            placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 1, 2, z, box);
            placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 3, 1, z, box);
            placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 3, 2, z, box);
            for (int x = 1; x <= 3; x++) {
                ruinBlock(level, box, Blocks.OAK_PLANKS.defaultBlockState(), x, 3, z, 20);
            }
        }
        BlockState rail = Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH);
        for (int z = 0; z < end - 2; z++) {
            ruinBlock(level, box, rail, 2, 1, z, 15);
        }
        placeBlock(level, rail, 2, 1, end - 2, box);
        placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 2, 1, end - 1, box);
        placeBlock(level, Blocks.COBWEB.defaultBlockState(), 1, 3, 2, box);
        placeBlock(level, Blocks.COBWEB.defaultBlockState(), 3, 3, end - 3, box);
        derelictCart(level, box, 2, 1, end - 2);
    }

    /** A sealed deepslate chamber with cracked calibration benches. */
    private void vault(WorldGenLevel level, BoundingBox box) {
        int maxX = layout.width() - 1;
        int maxY = layout.height() - 1;
        int maxZ = layout.depth() - 1;
        for (int x = 0; x <= maxX; x++) {
            for (int y = 0; y <= maxY; y++) {
                for (int z = 0; z <= maxZ; z++) {
                    boolean shell = x == 0 || x == maxX || y == 0 || y == maxY || z == 0 || z == maxZ;
                    placeBlock(level, shell ? pick(x, y, z, Blocks.DEEPSLATE_TILES.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState(),
                            Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState()) : Blocks.CAVE_AIR.defaultBlockState(), x, y, z, box);
                }
            }
        }
        for (int[] corner : new int[][] {{1, 1}, {maxX - 1, 1}, {1, maxZ - 1}, {maxX - 1, maxZ - 1}}) {
            for (int y = 1; y < maxY; y++) {
                placeBlock(level, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), corner[0], y, corner[1], box);
            }
        }
        BlockState bench = Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        for (int z = 2; z <= 3; z++) {
            placeBlock(level, bench, 2, 1, z, box);
            placeBlock(level, bench, maxX - 2, 1, z, box);
        }
        placeBlock(level, Blocks.AMETHYST_CLUSTER.defaultBlockState(), 2, 2, 2, box);
        ruinBlock(level, box, Blocks.SMALL_AMETHYST_BUD.defaultBlockState(), maxX - 2, 2, 3, 40);
        ruinBlock(level, box, Blocks.CHAIN.defaultBlockState(), 4, maxY - 1, 2, 30);
        machine(level, box, 4, 1, 5);
    }

    /** A half-buried brick foundry on the sea floor; the machine waits in a glass bell of air. */
    private void foundry(WorldGenLevel level, BoundingBox box) {
        int max = layout.width() - 1;
        for (int x = 0; x <= max; x++) {
            for (int z = 0; z <= max; z++) {
                fillColumnDown(level, Blocks.GRAVEL.defaultBlockState(), x, -1, z, box);
                ruinBlock(level, box, pick(x, 0, z, Blocks.BRICKS.defaultBlockState(), Blocks.MUD_BRICKS.defaultBlockState(),
                        Blocks.PACKED_MUD.defaultBlockState()), x, 0, z, 10);
            }
        }
        for (int y = 1; y <= 3; y++) {
            int wear = y == 1 ? 15 : y == 2 ? 40 : 70;
            for (int i = 0; i <= max; i++) {
                ruinBlock(level, box, Blocks.BRICKS.defaultBlockState(), i, y, 0, wear);
                ruinBlock(level, box, Blocks.BRICKS.defaultBlockState(), i, y, max, wear);
                ruinBlock(level, box, Blocks.BRICKS.defaultBlockState(), 0, y, i, wear);
                ruinBlock(level, box, Blocks.BRICKS.defaultBlockState(), max, y, i, wear);
            }
        }
        placeBlock(level, Blocks.CAULDRON.defaultBlockState(), 2, 1, 2, box);
        ruinBlock(level, box, Blocks.CAULDRON.defaultBlockState(), 3, 1, 2, 30);
        placeBlock(level, Blocks.IRON_BARS.defaultBlockState(), max - 2, 1, 2, box);
        placeBlock(level, Blocks.IRON_BARS.defaultBlockState(), max - 2, 2, 2, box);
        int center = max / 2;
        for (int x = center - 2; x <= center + 2; x++) {
            for (int y = 1; y <= 4; y++) {
                for (int z = center - 2; z <= center + 2; z++) {
                    boolean shell = Math.abs(x - center) == 2 || Math.abs(z - center) == 2 || y == 4;
                    placeBlock(level, shell ? Blocks.GLASS.defaultBlockState() : Blocks.AIR.defaultBlockState(), x, y, z, box);
                }
            }
        }
        machine(level, box, center, 1, center);
    }

    /** A blackstone smelter platform with a broken walkway and soul lanterns. */
    private void smeltworks(WorldGenLevel level, BoundingBox box) {
        int max = layout.width() - 1;
        generateAirBox(level, box, 0, 1, 0, max, layout.height() - 1, max);
        for (int x = 0; x <= max; x++) {
            for (int z = 0; z <= max; z++) {
                fillColumnDown(level, Blocks.BLACKSTONE.defaultBlockState(), x, -1, z, box);
                placeBlock(level, pick(x, 0, z, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
                        Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState(),
                        Blocks.MAGMA_BLOCK.defaultBlockState()), x, 0, z, box);
            }
        }
        for (int y = 1; y <= 3; y++) {
            int wear = y == 1 ? 20 : y == 2 ? 45 : 70;
            for (int i = 0; i <= max; i++) {
                ruinBlock(level, box, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), i, y, max, wear);
                ruinBlock(level, box, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), max, y, i, wear);
            }
        }
        BlockState walkway = Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        for (int x = 1; x <= max - 1; x++) {
            ruinBlock(level, box, walkway, x, 4, max - 2, 45);
        }
        BlockState chain = Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);
        placeBlock(level, chain, 3, 3, max - 2, box);
        placeBlock(level, chain, 3, 2, max - 2, box);
        placeBlock(level, Blocks.LAVA_CAULDRON.defaultBlockState(), 2, 1, max - 2, box);
        ruinBlock(level, box, Blocks.LAVA_CAULDRON.defaultBlockState(), 3, 1, max - 3, 40);
        placeBlock(level, Blocks.BLAST_FURNACE.defaultBlockState(), max - 2, 1, max - 2, box);
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false);
        placeBlock(level, lantern, 1, 1, 1, box);
        ruinBlock(level, box, lantern, max - 1, 1, 1, 50);
        machine(level, box, 5, 1, 5);
    }

    /** A small purpur relay pylon on an end stone platform. */
    private void relay(WorldGenLevel level, BoundingBox box) {
        int max = layout.width() - 1;
        int center = max / 2;
        generateAirBox(level, box, 0, 1, 0, max, layout.height() - 1, max);
        for (int x = 0; x <= max; x++) {
            for (int z = 0; z <= max; z++) {
                int dx = x - center;
                int dz = z - center;
                if (dx * dx + dz * dz > center * center + 2) { continue; }
                fillColumnDown(level, Blocks.END_STONE.defaultBlockState(), x, -1, z, box);
                boolean rim = dx * dx + dz * dz > (center - 1) * (center - 1);
                if (!rim || !worn(x, 0, z, 35)) {
                    placeBlock(level, Blocks.END_STONE_BRICKS.defaultBlockState(), x, 0, z, box);
                }
            }
        }
        placeBlock(level, Blocks.OBSIDIAN.defaultBlockState(), center, 0, center, box);
        placeBlock(level, Blocks.CRYING_OBSIDIAN.defaultBlockState(), center - 1, 0, center, box);
        placeBlock(level, Blocks.CRYING_OBSIDIAN.defaultBlockState(), center + 1, 0, center, box);
        BlockState pillar = Blocks.PURPUR_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        int back = max - 2;
        for (int y = 1; y <= 6; y++) {
            placeBlock(level, pillar, center, y, back, box);
        }
        placeBlock(level, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP), center, 7, back, box);
        for (int y = 1; y <= 3; y++) {
            ruinBlock(level, box, Blocks.PURPUR_BLOCK.defaultBlockState(), center - 2, y, back, y * 25);
            ruinBlock(level, box, Blocks.PURPUR_BLOCK.defaultBlockState(), center + 2, y, back, y * 25);
        }
        placeBlock(level, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.EAST), 2, 1, 2, box);
        machine(level, box, center, 1, center);
    }

    private void derelictCart(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockPos pos = getWorldPos(x, y, z);
        if (!box.isInside(pos)) { return; }
        DerelictCartEntity cart = DormantSites.spawnCart(level.getLevel(), pos, getOrientation());
        cart.core().setSite(site);
        level.addFreshEntity(cart);
    }

    private void machine(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockState state = ModBlocks.DORMANT_MACHINE.get().defaultBlockState().setValue(DormantMachineBlock.FACING, Direction.NORTH);
        placeBlock(level, state, x, y, z, box);
        BlockPos pos = getWorldPos(x, y, z);
        if (box.isInside(pos) && level.getBlockEntity(pos) instanceof DormantMachineBlockEntity dormant) {
            dormant.core().setSite(site);
        }
    }

    private void ruinBlock(WorldGenLevel level, BoundingBox box, BlockState state, int x, int y, int z, int wear) {
        if (!worn(x, y, z, wear)) {
            placeBlock(level, state, x, y, z, box);
        }
    }

    /** Whether the ruin has lost the block at this spot; the same spot always gives the same answer. */
    private boolean worn(int x, int y, int z, int percent) {
        return Math.floorMod(hash(x, y, z), 100) < percent;
    }

    private BlockState pick(int x, int y, int z, BlockState... states) {
        return states[Math.floorMod(hash(x, y, z) >>> 7, states.length)];
    }

    private long hash(int x, int y, int z) {
        long seed = Mth.getSeed(getWorldX(x, z), getWorldY(y), getWorldZ(x, z));
        return seed ^ seed >>> 29;
    }
}
