package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum MachineStat implements StringRepresentable {
    INPUT_SLOTS,
    OUTPUT_SLOTS,
    ADDON_SLOTS,
    OUTPUT_AMOUNT,
    SUPER_OUTPUT_CHANCE,
    MAGIC_FIND,
    PROCESSING_SPEED,
    INSTANT_PROCESS_CHANCE,
    PROCESSING_LEVEL,
    ENERGY_USAGE,
    ENERGY_CAPACITY,
    ENERGY_CAPACITY_FLAT,
    ENERGY_GENERATION,
    ENERGY_TRANSFER,
    EFFICIENCY,
    PARALLEL_JOBS,
    BUFFER_SIZE,
    STABILITY,
    UPGRADE_LIMIT,
    MAX_TEMPERATURE,
    HEAT_TRANSFER,
    HEAT_ISOLATION,
    TEMPERATURE_STABILITY,
    FUEL_EFFICIENCY,
    WARMUP_TIME,
    COOLING_RATE,
    OVERHEAT_TOLERANCE,
    REFINEMENT_POTENTIAL,
    BURST_TRANSFER,
    BURST_DURATION,
    IDLE_LOSS,
    BATTERY_SLOTS,
    GLOBAL_MODIFIER_STRENGTH,
    FLUID_CAPACITY,
    COMPRESSION_RATIO,
    FLUID_TRANSFER,
    CALIBRATION_QUALITY,
    CALIBRATION_PRECISION,
    CATALYST_EFFICIENCY,
    REFINEMENT_POTENTIAL_BONUS,
    MINING_SPEED,
    MINING_LEVEL,
    DURABILITY,
    FE_USAGE,
    FE_TRANSFER,
    AREA_WIDTH,
    AREA_HEIGHT,
    TREE_FELL_LIMIT,
    VEIN_MINE_LIMIT,
    VEIN_MINE_FE_USAGE,
    ORE_BURST_SPEED,
    ORE_BURST_DURATION,
    ORE_BURST_FE_USAGE,
    ORE_BURST_COOLDOWN,
    LUCK,
    CONTROL,
    BATTERY_SUPPORT,
    POTATO_POWER,
    CARROT_POWER,
    BREAD_POWER,
    SAPLING_POWER,
    SEED_POWER,
    PLANT_POWER,
    ORGANIC_REAGENT_POWER,
    COMPOSTED_BIOMASS_POWER,
    ALGAE_POWER,
    RICH_BIOMASS_POWER,
    FUEL_DURATION,
    SOLAR_PANEL_LIMIT,
    MOONLIGHT_CONVERSION,
    WEATHER_RECOVERY,
    SOLAR_PANEL_SYNCHRONIZATION,
    SOLAR_PANEL_ARBITRATION,
    OVERFLOW_SHUNTING,
    CLEAR_SKY_AMPLIFICATION,
    LUNAR_INVERSION,
    PEAK_SOLAR_GENERATION,
    SELF_REPAIR,
    OUTPUT_GUARD_GRACE,
    NO_BATTERY_OUTPUT_RETENTION,
    HIGH_HARDNESS_ENERGY_MITIGATION,
    CRUSHER_INPUT_FILTER,
    CRUSHER_SALVAGE_CHANCE,
    ATTACK_SPEED,
    BLOCK_FILTER_SLOTS,
    DRIVE,
    RESERVE,
    HARDNESS_TOLERANCE,
    JAM_CHANCE,
    JAM_RECOVERY,
    UNDER_LEVEL_EFFICIENCY,
    BANK_MEMORY,
    AT_LEVEL_OUTPUT,
    SUPER_OUTPUT_CADENCE,
    OVERDRIVE_SPEED,
    OVERDRIVE_CAP,
    OVERDRIVE_MARGIN,
    STRAIN_RECOVERY,
    LEDGER_RATE,
    FLUX_RATE,
    BLEND_SPEED,
    BLEND_HEAT_REDUCTION,
    MOLD_SWAP_TIME,
    HEAT_WINDOW,
    STREAK_FLOOR,
    STREAK_CAP,
    COIL_REACH,
    FLUID_YIELD,
    OVERLEVEL_SPEED,
    CART_SPEED,
    GROWTH_PULSE,
    WORK_RANGE,
    IDLE_CART_SPEED;

    public static final Codec<MachineStat> CODEC = StringRepresentable.fromEnum(MachineStat::values);
    public static final StreamCodec<ByteBuf, MachineStat> STREAM_CODEC =
            ByteBufCodecs.idMapper(MachineStat::byId, MachineStat::ordinal);
    private static final MachineStat[] VALUES = values();

    private final String serializedName;

    MachineStat() {
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public static MachineStat byId(int id) {
        return VALUES[id];
    }

    public String translationKey() {
        return "rngtech.stat." + serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
