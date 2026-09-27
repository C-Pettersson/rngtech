package com.rngtech.content.blockentity;

import com.rngtech.rpg.MachineTraits;

import net.minecraft.nbt.CompoundTag;

import java.util.Locale;

public record MachineInfoSnapshot(
        String family,
        int stage,
        WorkState state,
        BlockedReason blockedReason,
        int progress,
        int progressMax,
        long energy,
        long energyCapacity,
        long energyRate,
        long lastEnergyInput,
        long lastEnergyOutput,
        long connectorCap,
        EnergyBottleneck energyBottleneck,
        int fuel,
        int fuelMax,
        int processingLevel,
        int requiredProcessingLevel,
        int heat,
        int requiredHeat,
        int activeSlots,
        int maxSlots,
        GearSummary gearSummary,
        OutputSummary outputSummary,
        String refinementRarity,
        int refinementPotential,
        int affixCount
) {
    public static final int UNSET = -1;

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("family", family);
        tag.putInt("stage", stage);
        tag.putString("state", state.serializedName());
        tag.putString("blocked_reason", blockedReason.serializedName());
        tag.putInt("progress", progress);
        tag.putInt("progress_max", progressMax);
        tag.putLong("energy", energy);
        tag.putLong("energy_capacity", energyCapacity);
        tag.putLong("energy_rate", energyRate);
        tag.putLong("last_energy_input", lastEnergyInput);
        tag.putLong("last_energy_output", lastEnergyOutput);
        tag.putLong("connector_cap", connectorCap);
        tag.putString("energy_bottleneck", energyBottleneck.serializedName());
        tag.putInt("fuel", fuel);
        tag.putInt("fuel_max", fuelMax);
        tag.putInt("processing_level", processingLevel);
        tag.putInt("required_processing_level", requiredProcessingLevel);
        tag.putInt("heat", heat);
        tag.putInt("required_heat", requiredHeat);
        tag.putInt("active_slots", activeSlots);
        tag.putInt("max_slots", maxSlots);
        tag.putString("gear_summary", gearSummary.serializedName());
        tag.putString("output_summary", outputSummary.serializedName());
        tag.putString("refinement_rarity", refinementRarity);
        tag.putInt("refinement_potential", refinementPotential);
        tag.putInt("affix_count", affixCount);
        return tag;
    }

    public static MachineInfoSnapshot load(CompoundTag tag) {
        return new MachineInfoSnapshot(
                tag.getString("family"),
                tag.getInt("stage"),
                WorkState.byName(tag.getString("state")),
                BlockedReason.byName(tag.getString("blocked_reason")),
                tag.getInt("progress"),
                tag.getInt("progress_max"),
                tag.getLong("energy"),
                tag.getLong("energy_capacity"),
                tag.getLong("energy_rate"),
                tag.contains("last_energy_input") ? tag.getLong("last_energy_input") : 0,
                tag.contains("last_energy_output") ? tag.getLong("last_energy_output") : 0,
                tag.contains("connector_cap") ? tag.getLong("connector_cap") : 0,
                EnergyBottleneck.byName(tag.getString("energy_bottleneck")),
                tag.getInt("fuel"),
                tag.getInt("fuel_max"),
                tag.contains("processing_level") ? tag.getInt("processing_level") : UNSET,
                tag.contains("required_processing_level") ? tag.getInt("required_processing_level") : UNSET,
                tag.contains("heat") ? tag.getInt("heat") : UNSET,
                tag.contains("required_heat") ? tag.getInt("required_heat") : UNSET,
                tag.contains("active_slots") ? tag.getInt("active_slots") : UNSET,
                tag.contains("max_slots") ? tag.getInt("max_slots") : UNSET,
                GearSummary.byName(tag.getString("gear_summary")),
                OutputSummary.byName(tag.getString("output_summary")),
                tag.getString("refinement_rarity"),
                tag.getInt("refinement_potential"),
                tag.getInt("affix_count")
        );
    }

    public static Builder builder(String family) {
        return new Builder(family);
    }

    public enum WorkState {
        RUNNING,
        IDLE,
        BLOCKED,
        PAUSED;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static WorkState byName(String name) {
            return enumByName(values(), name, IDLE);
        }
    }

    public enum BlockedReason {
        NONE,
        MISSING_CRUSH_HEAD,
        NO_INPUT,
        INVALID_RECIPE,
        BLOCKED_LEVEL,
        OUTPUT_FULL,
        NO_POWER,
        NO_FUEL,
        HEAT_LOW,
        STABILITY_LOW,
        MISSING_HEAT_CORE,
        MISSING_CRUCIBLE,
        MISSING_BATTERY_CELL,
        MISSING_FUEL_BOX,
        BLOCKED_TIER,
        BLOCKED_FORM,
        FULL_GOVERNED,
        NOT_BURNABLE,
        MISSING_REACTOR_CHAMBER,
        INVALID_FUEL,
        ENERGY_FULL,
        REDSTONE_PAUSED,
        INSUFFICIENT_INPUT,
        JAMMED;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static BlockedReason byName(String name) {
            return enumByName(values(), name, NONE);
        }
    }

    public enum GearSummary {
        NONE,
        CRUSH_HEAD_INSTALLED,
        MISSING_CRUSH_HEAD,
        BATTERY_CELL_INSTALLED,
        MISSING_BATTERY_CELL,
        HEAT_CORE_INSTALLED,
        MISSING_HEAT_CORE,
        REACTOR_CHAMBER_INSTALLED,
        MISSING_REACTOR_CHAMBER,
        FUEL_BOX_INSTALLED,
        MISSING_FUEL_BOX,
        CELLS_INSTALLED,
        NO_CELLS;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static GearSummary byName(String name) {
            return enumByName(values(), name, NONE);
        }
    }

    public enum OutputSummary {
        NONE,
        OUTPUT_FULL,
        ENERGY_FULL,
        BONUS_PENDING,
        RESIDUE_ENABLED,
        RESIDUE_BLOCKED;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static OutputSummary byName(String name) {
            return enumByName(values(), name, NONE);
        }
    }

    public enum EnergyBottleneck {
        NONE,
        CONNECTOR_INPUT,
        CONNECTOR_OUTPUT,
        CELL_RATE;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static EnergyBottleneck byName(String name) {
            return enumByName(values(), name, NONE);
        }
    }

    public static final class Builder {
        private final String family;
        private int stage = UNSET;
        private WorkState state = WorkState.IDLE;
        private BlockedReason blockedReason = BlockedReason.NONE;
        private int progress;
        private int progressMax;
        private long energy;
        private long energyCapacity;
        private long energyRate;
        private long lastEnergyInput;
        private long lastEnergyOutput;
        private long connectorCap;
        private EnergyBottleneck energyBottleneck = EnergyBottleneck.NONE;
        private int fuel;
        private int fuelMax;
        private int processingLevel = UNSET;
        private int requiredProcessingLevel = UNSET;
        private int heat = UNSET;
        private int requiredHeat = UNSET;
        private int activeSlots = UNSET;
        private int maxSlots = UNSET;
        private GearSummary gearSummary = GearSummary.NONE;
        private OutputSummary outputSummary = OutputSummary.NONE;
        private String refinementRarity = "normal";
        private int refinementPotential;
        private int affixCount;

        private Builder(String family) {
            this.family = family;
        }

        public Builder stage(int stage) {
            this.stage = stage;
            return this;
        }

        public Builder state(WorkState state, BlockedReason blockedReason) {
            this.state = state;
            this.blockedReason = blockedReason;
            return this;
        }

        public Builder progress(int progress, int progressMax) {
            this.progress = Math.max(0, progress);
            this.progressMax = Math.max(0, progressMax);
            return this;
        }

        public Builder energy(long energy, long energyCapacity, long energyRate) {
            this.energy = Math.max(0, energy);
            this.energyCapacity = Math.max(0, energyCapacity);
            this.energyRate = energyRate;
            return this;
        }

        public Builder energyTelemetry(
                long lastEnergyInput,
                long lastEnergyOutput,
                long connectorCap,
                EnergyBottleneck energyBottleneck
        ) {
            this.lastEnergyInput = Math.max(0, lastEnergyInput);
            this.lastEnergyOutput = Math.max(0, lastEnergyOutput);
            this.connectorCap = Math.max(0, connectorCap);
            this.energyBottleneck = energyBottleneck == null ? EnergyBottleneck.NONE : energyBottleneck;
            return this;
        }

        public Builder fuel(int fuel, int fuelMax) {
            this.fuel = Math.max(0, fuel);
            this.fuelMax = Math.max(0, fuelMax);
            return this;
        }

        public Builder processingLevel(int processingLevel, int requiredProcessingLevel) {
            this.processingLevel = processingLevel;
            this.requiredProcessingLevel = requiredProcessingLevel;
            return this;
        }

        public Builder heat(int heat, int requiredHeat) {
            this.heat = heat;
            this.requiredHeat = requiredHeat;
            return this;
        }

        public Builder slots(int activeSlots, int maxSlots) {
            this.activeSlots = activeSlots;
            this.maxSlots = maxSlots;
            return this;
        }

        public Builder gear(GearSummary gearSummary) {
            this.gearSummary = gearSummary;
            return this;
        }

        public Builder output(OutputSummary outputSummary) {
            this.outputSummary = outputSummary;
            return this;
        }

        public Builder refinement(MachineTraits traits) {
            refinementRarity = traits.rarity().getSerializedName();
            refinementPotential = traits.refinementPotential();
            affixCount = (int) traits.modifiers().stream().filter(modifier -> modifier.slot().isAffix()).count();
            return this;
        }

        public MachineInfoSnapshot build() {
            return new MachineInfoSnapshot(
                    family,
                    stage,
                    state,
                    blockedReason,
                    progress,
                    progressMax,
                    energy,
                    energyCapacity,
                    energyRate,
                    lastEnergyInput,
                    lastEnergyOutput,
                    connectorCap,
                    energyBottleneck,
                    fuel,
                    fuelMax,
                    processingLevel,
                    requiredProcessingLevel,
                    heat,
                    requiredHeat,
                    activeSlots,
                    maxSlots,
                    gearSummary,
                    outputSummary,
                    refinementRarity,
                    refinementPotential,
                    affixCount
            );
        }
    }

    private static <T extends Enum<T>> T enumByName(T[] values, String name, T fallback) {
        for (T value : values) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }
}
