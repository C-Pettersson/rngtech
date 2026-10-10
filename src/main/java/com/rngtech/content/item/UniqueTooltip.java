package com.rngtech.content.item;

import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.unique.UniqueDefinition;
import com.rngtech.rpg.unique.UniqueHost;
import com.rngtech.rpg.unique.UniqueLineText;
import com.rngtech.rpg.unique.UniqueStatLine;
import com.rngtech.rpg.unique.UniqueStatReaders;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Unique tooltips: ranges while unidentified, rolled values once identified, and with Shift each line's range and roll
 * quality. Signature, ascendancy hook, and drawback lines get their own sections. Inside a machine screen, stats that
 * machine does not read are dimmed.
 */
public final class UniqueTooltip {
    /** Path of Exile's unique item colour, used for Unique names, the rarity header, and flavour text. */
    public static final TextColor UNIQUE_COLOR = TextColor.fromRgb(0xAF6025);

    private static Supplier<OpenHost> openHost = () -> null;

    /**
     * The machine whose screen is open: what it reads, and its chosen ascendancy, which is empty for none and null when the
     * screen does not show Mastery.
     */
    public record OpenHost(UniqueStatReaders.Reader reader, String ascendancy) {
    }

    private UniqueTooltip() {
    }

    /** Set by the client: the machine whose screen is open, or null. */
    public static void setOpenHost(Supplier<OpenHost> host) {
        openHost = host;
    }

    static void append(ItemStack stack, UniqueDefinition definition, List<Component> tooltip) {
        boolean unidentified = CraftedTraitOutputs.isUnidentified(stack);
        List<Component> lines = lines(definition, unidentified ? null : rolls(stack), unidentified, TooltipKeyState.hasShiftDown(), openHost.get());
        MachineTraits stored = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        if (stored != null && stored.isCorrupted()) {
            List<Component> corruption = new ArrayList<>();
            MachineTraitTooltip.appendCorruptedLine(stored, corruption);
            lines.addAll(1, corruption);
            MachineTraitTooltip.appendCorruptionSection(stored, lines);
        }
        tooltip.addAll(lines);
    }

    /**
     * Every line of a Unique's tooltip. {@code rolls} is null while unidentified, so lines show their ranges; {@code host}
     * dims stats that machine does not read, or is null.
     */
    public static List<Component> lines(
            UniqueDefinition definition,
            List<MachineModifier> rolls,
            boolean unidentified,
            boolean details,
            OpenHost host
    ) {
        Display display = new Display(definition, rolls, details, host);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "rngtech.tooltip.rarity",
                Component.translatable(Rarity.UNIQUE.translationKey()).withStyle(style -> style.withColor(UNIQUE_COLOR))
        ).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                "rngtech.tooltip.unique.slot",
                Component.translatable("rngtech.unique_host." + definition.host().serializedName()),
                definition.slotStage()
        ).withStyle(ChatFormatting.GRAY));
        if (unidentified) {
            lines.add(Component.translatable("rngtech.tooltip.unique.unidentified").withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable(definition.descriptionKey()).withStyle(style -> style.withColor(UNIQUE_COLOR).withItalic(true)));
        display.section(lines, "rngtech.tooltip.unique.signature", ChatFormatting.BLUE, UniqueStatLine.Role.IDENTITY, UniqueStatLine.Role.SIGNATURE);
        for (MachineBehavior behavior : definition.behaviors()) {
            MutableComponent line = Component.translatable(
                    "rngtech.tooltip.behavior",
                    Component.translatable(behavior.translationKey()),
                    Component.translatable(behavior.descriptionKey())
            );
            lines.add(display.reads(behavior) ? line.withStyle(ChatFormatting.BLUE) : unread(line));
        }
        display.section(lines, "rngtech.tooltip.unique.ascendancy", ChatFormatting.BLUE, UniqueStatLine.Role.HOOK);
        display.section(lines, "rngtech.tooltip.unique.drawbacks", ChatFormatting.RED, UniqueStatLine.Role.DRAWBACK);
        if (rolls != null && details && definition.hasRangedLines()) {
            lines.add(Component.translatable("rngtech.tooltip.unique.quality", UniqueLineText.quality(definition.quality(rolls)))
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.empty());
        lines.add(Component.translatable("rngtech.tooltip.unique.source", Component.translatable(definition.sourceKey()))
                .withStyle(ChatFormatting.DARK_GRAY));
        if (rolls != null && !details && definition.hasRangedLines()) {
            lines.add(Component.translatable("rngtech.tooltip.unique.hint.shift").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    /** A Unique's item name in the Unique colour. */
    public static Component name(Component name) {
        return name.copy().withStyle(style -> style.withColor(UNIQUE_COLOR));
    }

    /** A stat line as shown on the item: its rolled value, or its range while {@code rolls} is null. */
    public static MutableComponent lineText(UniqueDefinition definition, UniqueStatLine line, List<MachineModifier> rolls) {
        MutableComponent text = rolls == null ? UniqueLineText.range(line) : UniqueLineText.rolled(line, definition.value(line, rolls));
        if (line.role() == UniqueStatLine.Role.HOOK) {
            text = Component.translatable("rngtech.tooltip.unique.hook", text,
                    Component.translatable("rngtech.mastery.ascendancy." + line.ascendancy()));
        }
        return text;
    }

    private static MutableComponent unread(MutableComponent line) {
        return Component.translatable("rngtech.tooltip.unique.unread", line).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static List<MachineModifier> rolls(ItemStack stack) {
        MachineTraits traits = stack.get(ModDataComponents.MACHINE_TRAITS.get());
        return UniqueDefinition.storedRolls(traits == null ? List.of() : traits.modifiers());
    }

    private record Display(UniqueDefinition definition, List<MachineModifier> rolls, boolean details, OpenHost host) {
        void section(List<Component> lines, String header, ChatFormatting color, UniqueStatLine.Role... roles) {
            List<UniqueStatLine.Role> wanted = List.of(roles);
            List<UniqueStatLine> matching = definition.lines().stream().filter(line -> wanted.contains(line.role())).toList();
            boolean behaviorsFollow = wanted.contains(UniqueStatLine.Role.SIGNATURE) && !definition.behaviors().isEmpty();
            if (matching.isEmpty() && !behaviorsFollow) {
                return;
            }
            lines.add(Component.empty());
            lines.add(Component.translatable(header).withStyle(ChatFormatting.DARK_AQUA));
            for (UniqueStatLine line : matching) {
                MutableComponent text = lineText(definition, line, rolls);
                if (!reads(line.stat())) {
                    text = unread(text);
                } else if (inactiveHook(line)) {
                    text = text.withStyle(ChatFormatting.DARK_GRAY);
                } else {
                    text = text.withStyle(color);
                }
                if (rolls != null && details && line.ranged()) {
                    text.append(Component.translatable(
                            "rngtech.tooltip.unique.roll",
                            UniqueLineText.rangeAmount(line),
                            UniqueLineText.quality(line.quality(definition.value(line, rolls)))
                    ).withStyle(ChatFormatting.DARK_GRAY));
                }
                lines.add(text);
            }
        }

        boolean reads(MachineStat stat) {
            return host == null || UniqueStatReaders.reads(host.reader(), stat)
                    || cellReads() && UniqueStatReaders.reads(UniqueStatReaders.Reader.BATTERY_CELL, stat);
        }

        boolean reads(MachineBehavior behavior) {
            return host == null || UniqueStatReaders.reads(host.reader(), behavior);
        }

        /** A hook line in an open machine that has not chosen the hook's ascendancy. */
        boolean inactiveHook(UniqueStatLine line) {
            return line.role() == UniqueStatLine.Role.HOOK && host != null && host.ascendancy() != null
                    && !host.ascendancy().equals(line.ascendancy());
        }

        /** A Battery Cell's own stats work in any machine that holds it. */
        private boolean cellReads() {
            return definition.host() == UniqueHost.BATTERY_CELL;
        }
    }
}
