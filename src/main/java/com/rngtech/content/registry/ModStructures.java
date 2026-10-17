package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.dormant.DormantSitePiece;
import com.rngtech.content.dormant.DormantSiteStructure;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, RNGTech.MOD_ID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, RNGTech.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<DormantSiteStructure>> DORMANT_SITE =
            STRUCTURE_TYPES.register("dormant_site", () -> () -> DormantSiteStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> DORMANT_SITE_PIECE =
            STRUCTURE_PIECES.register("dormant_site", () -> DormantSitePiece::new);

    private ModStructures() {
    }

    public static void register(IEventBus bus) {
        STRUCTURE_TYPES.register(bus);
        STRUCTURE_PIECES.register(bus);
    }
}
