package com.rngtech.content.registry;

import com.rngtech.RNGTech;
import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.calibration.CalibrationStreak;
import com.rngtech.content.configurator.ConfiguratorPreset;
import com.rngtech.content.item.TankContents;
import com.rngtech.content.item.UnidentifiedTraitRoll;
import com.rngtech.content.itemfilter.AdvancedItemFilterSettings;
import com.rngtech.content.minerscompanion.MinersCompanionState;
import com.rngtech.content.tool.FieldToolAssembly;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.OutputAmountTracker;
import com.rngtech.rpg.jewel.MachineJewels;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.SoulTransfer;
import com.rngtech.rpg.workorder.FulfilledWorkOrder;
import com.rngtech.rpg.workorder.MachineWorkOrders;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderProgress;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

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

    /** The Harmonist's streak, kept on a dropped or picked Resonance Calibrator by Pattern Memory. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CalibrationStreak>> CALIBRATION_STREAK =
            DATA_COMPONENTS.registerComponentType(
                    "calibration_streak",
                    builder -> builder
                            .persistent(CalibrationStreak.CODEC)
                            .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(CalibrationStreak.CODEC))
            );

    /** The jewels in a machine's tree and body sockets, kept on the dropped or picked machine. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MachineJewels>> JEWELS =
            DATA_COMPONENTS.registerComponentType(
                    "jewels",
                    builder -> builder.persistent(MachineJewels.CODEC).networkSynchronized(MachineJewels.STREAM_CODEC)
            );

    /** Crusher bonus banks kept on a dropped or picked machine by the Assayer's Wide Ledger. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<OutputAmountTracker.SavedBank>>> OUTPUT_BANKS =
            DATA_COMPONENTS.registerComponentType(
                    "output_banks",
                    builder -> builder
                            .persistent(OutputAmountTracker.SavedBank.LIST_CODEC)
                            .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(OutputAmountTracker.SavedBank.LIST_CODEC))
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

    /** A copy made by a Mirror Catalyst. Every refinement path refuses it, and it cannot be mirrored again. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> MIRRORED =
            DATA_COMPONENTS.registerComponentType(
                    "mirrored",
                    builder -> builder
                            .persistent(Unit.CODEC)
                            .networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IDENTIFY_TRAIT_ROLL_ON_CRAFT =
            DATA_COMPONENTS.registerComponentType(
                    "identify_trait_roll_on_craft",
                    builder -> builder
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .cacheEncoding()
            );

    /** Pressed under Hydrogen: on plates and Basic Electric Circuits, and on Gear crafted from all-Annealed plates. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ANNEALED =
            DATA_COMPONENTS.registerComponentType(
                    "annealed",
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


    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BATTERY_CELL_ENERGY =
            DATA_COMPONENTS.registerComponentType(
                    "battery_cell_energy",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .cacheEncoding()
            );

    /** Strain from Phase Array pulses. Absent means 0. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STRAIN =
            DATA_COMPONENTS.registerComponentType(
                    "strain",
                    builder -> builder
                            .persistent(Codec.intRange(0, Integer.MAX_VALUE))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .cacheEncoding()
            );
    /** A Glyph's tier, 1 to 5. A plain stack with no tier reads as Tier I. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GLYPH_TIER =
            DATA_COMPONENTS.registerComponentType(
                    "glyph_tier",
                    builder -> builder
                            .persistent(Codec.intRange(1, 5))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .cacheEncoding()
            );
    /** A Work Order item's family, tier, job, requirements, Bounty, and Prestige. A stack without it is blank and never loads. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WorkOrder>> WORK_ORDER =
            DATA_COMPONENTS.registerComponentType("work_order", builder -> builder
                    .persistent(WorkOrder.CODEC).networkSynchronized(WorkOrder.STREAM_CODEC).cacheEncoding());
    /** The progress an unloaded Work Order keeps until it is loaded again. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WorkOrderProgress>> WORK_ORDER_PROGRESS =
            DATA_COMPONENTS.registerComponentType("work_order_progress", builder -> builder
                    .persistent(WorkOrderProgress.CODEC).networkSynchronized(WorkOrderProgress.STREAM_CODEC).cacheEncoding());
    /** The Work Orders loaded into a machine, kept on its dropped item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MachineWorkOrders>> WORK_ORDERS =
            DATA_COMPONENTS.registerComponentType("work_orders", builder -> builder
                    .persistent(MachineWorkOrders.CODEC).networkSynchronized(MachineWorkOrders.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FulfilledWorkOrder>> FULFILLED_WORK_ORDER =
            DATA_COMPONENTS.registerComponentType("fulfilled_work_order", builder -> builder
                    .persistent(FulfilledWorkOrder.CODEC).networkSynchronized(FulfilledWorkOrder.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CAVITATION_ROTOR_WEAR =
            DATA_COMPONENTS.registerComponentType(
                    "cavitation_rotor_wear",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .cacheEncoding()
            );

    /** Resonance as a whole throttle percent; deliberately absent from tooltips so players find it by tuning. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CAVITATION_ROTOR_RESONANCE =
            DATA_COMPONENTS.registerComponentType(
                    "cavitation_rotor_resonance",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .cacheEncoding()
            );

    /** A calibrated recipe input's state, recorded on a crafted result for a post-craft roll or, on a Soul Transfer, its success chance. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CalibrationState>> SOURCE_CALIBRATION =
            DATA_COMPONENTS.registerComponentType(
                    "source_calibration",
                    builder -> builder
                            .persistent(CalibrationState.CODEC)
                            .networkSynchronized(CalibrationState.STREAM_CODEC)
                            .cacheEncoding()
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SoulTransfer.Charge>> SOUL_CHARGE =
            DATA_COMPONENTS.registerComponentType("soul_charge", builder -> builder
                    .persistent(SoulTransfer.Charge.CODEC).networkSynchronized(SoulTransfer.Charge.STREAM_CODEC).cacheEncoding());

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

    /** A Potential Reactor's recent gear burns, oldest first, kept on the dropped reactor so replacing it keeps the repeat penalty. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> REACTOR_BURN_HISTORY =
            DATA_COMPONENTS.registerComponentType(
                    "reactor_burn_history",
                    builder -> builder
                            .persistent(Codec.STRING.listOf())
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
                            .cacheEncoding()
            );

    /** The loose and compressed fluid kept on a broken Fluid or Compressor Tank, restored when it is placed again. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TankContents>> TANK_CONTENTS =
            DATA_COMPONENTS.registerComponentType(
                    "tank_contents",
                    builder -> builder
                            .persistent(TankContents.CODEC)
                            .networkSynchronized(TankContents.STREAM_CODEC)
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
