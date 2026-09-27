package com.rngtech.rpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MachineModifier(
        String affixId,
        String modGroup,
        ModifierSlot slot,
        MachineStat stat,
        ModifierOperation operation,
        int tier,
        ModifierValueRange range,
        double value,
        List<MachineModifierEffect> effects
) {
    public static final Codec<MachineModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("affix_id", "").forGetter(MachineModifier::affixId),
            Codec.STRING.optionalFieldOf("mod_group", "").forGetter(MachineModifier::modGroup),
            ModifierSlot.CODEC.fieldOf("slot").forGetter(MachineModifier::slot),
            MachineStat.CODEC.fieldOf("stat").forGetter(MachineModifier::stat),
            ModifierOperation.CODEC.fieldOf("operation").forGetter(MachineModifier::operation),
            Codec.INT.optionalFieldOf("tier", 0).forGetter(MachineModifier::tier),
            ModifierValueRange.CODEC.optionalFieldOf("range").forGetter(modifier -> Optional.of(modifier.range())),
            Codec.DOUBLE.fieldOf("value").forGetter(MachineModifier::value),
            MachineModifierEffect.CODEC.listOf().optionalFieldOf("effects", List.of())
                    .forGetter(MachineModifier::effects)
    ).apply(instance, MachineModifier::fromCodec));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineModifier> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public MachineModifier decode(RegistryFriendlyByteBuf buffer) {
                    String affixId = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    String modGroup = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    ModifierSlot slot = ModifierSlot.STREAM_CODEC.decode(buffer);
                    MachineStat stat = MachineStat.STREAM_CODEC.decode(buffer);
                    ModifierOperation operation = ModifierOperation.STREAM_CODEC.decode(buffer);
                    int tier = ByteBufCodecs.VAR_INT.decode(buffer);
                    ModifierValueRange range = ModifierValueRange.STREAM_CODEC.decode(buffer);
                    double value = ByteBufCodecs.DOUBLE.decode(buffer);
                    List<MachineModifierEffect> effects =
                            MachineModifierEffect.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
                    return new MachineModifier(affixId, modGroup, slot, stat, operation, tier, range, value, effects);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, MachineModifier modifier) {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, modifier.affixId());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, modifier.modGroup());
                    ModifierSlot.STREAM_CODEC.encode(buffer, modifier.slot());
                    MachineStat.STREAM_CODEC.encode(buffer, modifier.stat());
                    ModifierOperation.STREAM_CODEC.encode(buffer, modifier.operation());
                    ByteBufCodecs.VAR_INT.encode(buffer, modifier.tier());
                    ModifierValueRange.STREAM_CODEC.encode(buffer, modifier.range());
                    ByteBufCodecs.DOUBLE.encode(buffer, modifier.value());
                    MachineModifierEffect.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buffer, modifier.effects());
                }
            };

    public MachineModifier {
        affixId = Objects.requireNonNullElse(affixId, "");
        modGroup = normalizeModGroup(modGroup, stat, operation);
        tier = Math.max(0, tier);
        range = Objects.requireNonNull(range, "range");
        effects = normalizeEffects(effects, stat, operation, range, value);
    }

    public MachineModifier(ModifierSlot slot, MachineStat stat, ModifierOperation operation, double value) {
        this("", "", slot, stat, operation, 0, ModifierValueRange.fixed(value), value, List.of());
    }

    public MachineModifier(
            ModifierSlot slot,
            MachineStat stat,
            ModifierOperation operation,
            int tier,
            ModifierValueRange range,
            double value
    ) {
        this("", "", slot, stat, operation, tier, range, value, List.of());
    }

    public static MachineModifier roll(
            String affixId,
            String modGroup,
            ModifierSlot slot,
            MachineStat stat,
            ModifierOperation operation,
            int tier,
            ModifierValueRange range,
            RandomSource random
    ) {
        return new MachineModifier(affixId, modGroup, slot, stat, operation, tier, range, range.roll(random), List.of());
    }

    public static MachineModifier roll(
            String affixId,
            String modGroup,
            ModifierSlot slot,
            int tier,
            List<MachineModifierEffect> effects
    ) {
        if (effects.isEmpty()) {
            throw new IllegalArgumentException("Machine modifier effects cannot be empty.");
        }
        MachineModifierEffect primary = effects.getFirst();
        return new MachineModifier(
                affixId,
                modGroup,
                slot,
                primary.stat(),
                primary.operation(),
                tier,
                primary.range(),
                primary.value(),
                effects
        );
    }

    public boolean hasTier() {
        return tier > 0;
    }

    public boolean hasAffixId() {
        return !affixId.isBlank();
    }

    public boolean matchesTargetStat(MachineStat targetStat) {
        return effects.stream().anyMatch(effect -> effect.stat() == targetStat);
    }

    private static MachineModifier fromCodec(
            String affixId,
            String modGroup,
            ModifierSlot slot,
            MachineStat stat,
            ModifierOperation operation,
            int tier,
            Optional<ModifierValueRange> range,
            double value,
            List<MachineModifierEffect> effects
    ) {
        ModifierValueRange resolvedRange = range.orElseGet(() -> ModifierValueRange.fixed(value));
        return new MachineModifier(affixId, modGroup, slot, stat, operation, tier, resolvedRange, value, effects);
    }

    private static List<MachineModifierEffect> normalizeEffects(
            List<MachineModifierEffect> effects,
            MachineStat stat,
            ModifierOperation operation,
            ModifierValueRange range,
            double value
    ) {
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(operation, "operation");
        if (effects == null || effects.isEmpty()) {
            return List.of(new MachineModifierEffect(stat, operation, range, value));
        }
        return List.copyOf(effects);
    }

    private static String normalizeModGroup(String modGroup, MachineStat stat, ModifierOperation operation) {
        if (modGroup != null && !modGroup.isBlank()) {
            if (isLegacyVariantGroup(modGroup, stat)) {
                return stat == MachineStat.PROCESSING_SPEED
                        ? "processing_speed"
                        : operation.getSerializedName() + ":" + stat.getSerializedName();
            }
            return modGroup;
        }
        if (stat == null || operation == null) {
            return "";
        }
        return operation.getSerializedName() + ":" + stat.getSerializedName();
    }

    private static boolean isLegacyVariantGroup(String modGroup, MachineStat stat) {
        if (stat == null) {
            return false;
        }
        String prefix = stat.getSerializedName() + ":";
        if (!modGroup.startsWith(prefix)) {
            return false;
        }
        String suffix = modGroup.substring(prefix.length());
        return suffix.equals("tuning")
                || suffix.equals("structure")
                || suffix.equals("harmony")
                || suffix.equals("focus")
                || suffix.equals("amplifier");
    }
}
