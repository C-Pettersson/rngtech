package com.rngtech.content.dormant;

import com.rngtech.content.registry.ModStructures;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** One Dormant Machine site: a small ruin from {@link DormantSitePiece} with one Dormant Machine naming {@code site}. */
public class DormantSiteStructure extends Structure {
    public static final MapCodec<DormantSiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("site").forGetter(structure -> structure.site),
            DormantLayout.CODEC.fieldOf("layout").forGetter(structure -> structure.layout)
    ).apply(instance, DormantSiteStructure::new));

    private final ResourceLocation site;
    private final DormantLayout layout;

    public DormantSiteStructure(StructureSettings settings, ResourceLocation site, DormantLayout layout) {
        super(settings);
        this.site = site;
        this.layout = layout;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        Optional<Integer> floor = floorY(context, x, z);
        if (floor.isEmpty()) { return Optional.empty(); }
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        BlockPos center = new BlockPos(x, floor.get(), z);
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(
                new DormantSitePiece(layout, site, x - layout.width() / 2, floor.get(), z - layout.depth() / 2, facing))));
    }

    /** The height of the ruin's floor, or nothing when this chunk has no fitting ground. */
    private Optional<Integer> floorY(GenerationContext context, int x, int z) {
        ChunkGenerator generator = context.chunkGenerator();
        return switch (layout.placement()) {
            case SURFACE -> {
                int free = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                NoiseColumn column = generator.getBaseColumn(x, z, context.heightAccessor(), context.randomState());
                boolean dry = column.getBlock(free - 1).getFluidState().isEmpty();
                yield dry && free > generator.getSeaLevel() ? Optional.of(free - 1) : Optional.empty();
            }
            case UNDERGROUND_SHALLOW -> Optional.of(-30 + context.random().nextInt(60));
            case UNDERGROUND_DEEP -> Optional.of(-52 + context.random().nextInt(40));
            case OCEAN_FLOOR -> {
                int floor = generator.getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                yield floor < generator.getSeaLevel() - layout.height() ? Optional.of(floor - 1) : Optional.empty();
            }
            case NETHER_GROUND -> netherGround(generator.getBaseColumn(x, z, context.heightAccessor(), context.randomState()));
            case END_ISLAND -> {
                int free = generator.getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                yield free >= 40 ? Optional.of(free - 1) : Optional.empty();
            }
        };
    }

    /** The highest solid block between Y 32 and 100 with open air, not lava, above it. */
    private Optional<Integer> netherGround(NoiseColumn column) {
        for (int y = 100; y >= 32; y--) {
            BlockState below = column.getBlock(y);
            if (below.isAir() || !below.getFluidState().isEmpty()) { continue; }
            boolean open = true;
            for (int above = 1; above <= layout.height() && open; above++) {
                open = column.getBlock(y + above).isAir();
            }
            if (open) { return Optional.of(y); }
        }
        return Optional.empty();
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.DORMANT_SITE.get();
    }
}
