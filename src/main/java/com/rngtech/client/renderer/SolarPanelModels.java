package com.rngtech.client.renderer;

import com.rngtech.RNGTech;
import com.rngtech.RNGTechClientConfig;
import com.rngtech.content.block.SolarPanelBlock;
import com.rngtech.content.energy.SolarPanelMaterial;
import com.rngtech.content.energy.SolarTracker;
import com.rngtech.content.registry.ModBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;

/**
 * The Solar Panel's extra models. Every panel blockstate bakes only the mount and SolarPanelRenderer draws the deck
 * model on top. With animation off in the client config, each blockstate bakes the full stepped model instead (flat,
 * or tilted 22.5 degrees east or west) and the renderer draws nothing.
 */
public final class SolarPanelModels {
    private SolarPanelModels() {
    }

    public static ModelResourceLocation deck(SolarPanelMaterial material) {
        return ModelResourceLocation.standalone(RNGTech.id("block/" + material.blockId() + "_deck"));
    }

    private static ModelResourceLocation stepped(SolarPanelMaterial material, SolarTracker tracker) {
        String suffix = tracker == SolarTracker.STOWED ? "" : "_" + tracker.getSerializedName();
        return ModelResourceLocation.standalone(RNGTech.id("block/" + material.blockId() + suffix));
    }

    public static BakedModel deckModel(SolarPanelMaterial material) {
        return Minecraft.getInstance().getModelManager().getModel(deck(material));
    }

    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        for (SolarPanelMaterial material : SolarPanelMaterial.values()) {
            event.register(deck(material));
            for (SolarTracker tracker : SolarTracker.values()) {
                event.register(stepped(material, tracker));
            }
        }
    }

    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (RNGTechClientConfig.animatedSolarPanels()) {
            return;
        }
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (SolarPanelMaterial material : SolarPanelMaterial.values()) {
            for (BlockState state : ModBlocks.solarPanel(material).get().getStateDefinition().getPossibleStates()) {
                BakedModel model = models.get(stepped(material, state.getValue(SolarPanelBlock.TRACKER)));
                if (model != null) {
                    models.put(BlockModelShaper.stateToModelLocation(state), model);
                }
            }
        }
    }
}
