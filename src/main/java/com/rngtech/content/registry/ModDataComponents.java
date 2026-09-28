package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.configurator.ConfiguratorPreset;
import com.rngtech.content.item.UnidentifiedTraitRoll;
import com.rngtech.content.itemfilter.AdvancedItemFilterSettings;
import com.rngtech.content.minerscompanion.MinersCompanionState;
import com.rngtech.content.tool.FieldToolAssembly;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.refinement.ExoticAffixForgeHistory;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RNGTech.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MASTERY_BUILD =
            DATA_COMPONENTS.registerComponentType("mastery_build", builder -> builder
                    .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> MASTERY_CONFIGURATOR_MODE =
            DATA_COMPONENTS.registerComponentType("mastery_configurator_mode", builder -> builder
                    .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MachineTraits>> MACHINE_TRAITS =
            DATA_COMPONENTS.registerComponentType(
                    "machine_traits",
                    builder -> builder
                            .persistent(MachineTraits.CODEC)
                            .networkSynchronized(MachineTraits.STREAM_CODEC)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MachineProgressionState>>
            MACHINE_PROGRESSION =
                    DATA_COMPONENTS.registerComponentType(
                            "machine_progression",
                            builder -> builder
                                    .persistent(MachineProgressionState.CODEC)
                                    .networkSynchronized(MachineProgressionState.STREAM_CODEC)
                                    .cacheEncoding()
                    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UnidentifiedTraitRoll>>
            UNIDENTIFIED_TRAIT_ROLL =
                    DATA_COMPONENTS.registerComponentType(
                            "unidentified_trait_roll",
                            builder -> builder
                                    .persistent(UnidentifiedTraitRoll.CODEC)
                                    .networkSynchronized(UnidentifiedTraitRoll.STREAM_CODEC)
                                    .cacheEncoding()
                    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IDENTIFY_TRAIT_ROLL_ON_CRAFT =
            DATA_COMPONENTS.registerComponentType(
                    "identify_trait_roll_on_craft",
                    builder -> builder
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CalibrationState>> CALIBRATION_STATE =
            DATA_COMPONENTS.registerComponentType(
                    "calibration_state",
                    builder -> builder
                            .persistent(CalibrationState.CODEC)
                            .networkSynchronized(CalibrationState.STREAM_CODEC)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FieldToolAssembly>> FIELD_TOOL_ASSEMBLY =
            DATA_COMPONENTS.registerComponentType(
                    "field_tool_assembly",
                    builder -> builder
                            .persistent(FieldToolAssembly.CODEC)
                            .networkSynchronized(FieldToolAssembly.STREAM_CODEC)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MinersCompanionState>> MINERS_COMPANION_STATE =
            DATA_COMPONENTS.registerComponentType(
                    "miners_companion_state",
                    builder -> builder
                            .persistent(MinersCompanionState.CODEC)
                            .networkSynchronized(MinersCompanionState.STREAM_CODEC)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ConfiguratorPreset>> CONFIGURATOR_PRESET =
            DATA_COMPONENTS.registerComponentType(
                    "configurator_preset",
                    builder -> builder
                            .persistent(ConfiguratorPreset.CODEC)
                            .networkSynchronized(ConfiguratorPreset.STREAM_CODEC)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AdvancedItemFilterSettings>> ADVANCED_ITEM_FILTER =
            DATA_COMPONENTS.registerComponentType(
                    "advanced_item_filter",
                    builder -> builder
                            .persistent(AdvancedItemFilterSettings.CODEC)
                            .networkSynchronized(AdvancedItemFilterSettings.STREAM_CODEC)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ExoticAffixForgeHistory>>
            EXOTIC_AFFIX_FORGE_HISTORY =
                    DATA_COMPONENTS.registerComponentType(
                            "exotic_affix_forge_history",
                            builder -> builder
                                    .persistent(ExoticAffixForgeHistory.CODEC)
                                    .networkSynchronized(ExoticAffixForgeHistory.STREAM_CODEC)
                                    .cacheEncoding()
                    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BATTERY_CELL_ENERGY =
            DATA_COMPONENTS.registerComponentType(
                    "battery_cell_energy",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CAVITATION_ROTOR_WEAR =
            DATA_COMPONENTS.registerComponentType(
                    "cavitation_rotor_wear",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FIELD_TOOL_WEAR =
            DATA_COMPONENTS.registerComponentType(
                    "field_tool_wear",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FIELD_TOOL_DAMAGE_REMAINDER =
            DATA_COMPONENTS.registerComponentType(
                    "field_tool_damage_remainder",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CART_SHEARS_WEAR_PROGRESS =
            DATA_COMPONENTS.registerComponentType(
                    "cart_shears_wear_progress",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MATERIAL =
            DATA_COMPONENTS.registerComponentType(
                    "material",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> RECYCLING_STRIPPED =
            DATA_COMPONENTS.registerComponentType(
                    "recycling_stripped",
                    builder -> builder
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> DIAMOND_TIPPED =
            DATA_COMPONENTS.registerComponentType(
                    "diamond_tipped",
                    builder -> builder
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .cacheEncoding()
            );

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }

    private ModDataComponents() {
    }
}
