package com.rngtech;

import com.rngtech.content.item.CableItem;
import com.rngtech.content.minerscompanion.MinersCompanionEvents;
import com.rngtech.content.registry.ModBlockEntities;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModCapabilities;
import com.rngtech.content.registry.ModConditions;
import com.rngtech.content.registry.ModCreativeTabs;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModEntityTypes;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModMenus;
import com.rngtech.content.registry.ModNetworking;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.content.registry.ModSounds;
import com.rngtech.content.tool.FieldToolEvents;

import com.mojang.logging.LogUtils;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(RNGTech.MOD_ID)
public final class RNGTech {
    public static final String MOD_ID = "rngtech";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isDebugContentEnabled() {
        return !FMLEnvironment.production;
    }

    public RNGTech(IEventBus modBus, ModContainer modContainer) {
        ModFluids.register(modBus);
        ModEntityTypes.register(modBus);
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModDataComponents.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModRecipes.register(modBus);
        ModSounds.register(modBus);
        ModConditions.register(modBus);
        ModCreativeTabs.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(ModCapabilities::register);
        modBus.addListener(ModNetworking::register);
        NeoForge.EVENT_BUS.addListener(FieldToolEvents::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(FieldToolEvents::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(FieldToolEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(FieldToolEvents::onAnvilUpdate);
        NeoForge.EVENT_BUS.addListener(FieldToolEvents::onGrindstonePlace);
        NeoForge.EVENT_BUS.addListener(MinersCompanionEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(MinersCompanionEvents::onPlayerTick);

        modContainer.registerConfig(ModConfig.Type.COMMON, RNGTechConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CauldronInteraction.WATER.map().put(ModItems.CABLE.get(), CableItem::washInCauldron));
        LOGGER.info("RNGTech loaded");
    }
}
