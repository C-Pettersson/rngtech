package com.rngtech.rpg.jewel;

import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * How a machine reads its socketed jewels. A jewel's lines are machine-wide and apply with the tree's nodes, before
 * Gear and the attribute conversions, where the family reads the stat; its attribute-threshold lines wait until the
 * attribute totals are final.
 */
public final class JewelEffects {
    private JewelEffects() {
    }

    /** One socketed jewel as the stat pipeline sees it: a breakdown label and its rolled lines. */
    public record Jewel(Component name, MachineTraits traits) {
    }

    /** The jewels the machine reads: identified jewels in tree slots under the allocated socket count, then body slots under the body socket count. */
    public static List<Jewel> active(MachineJewels jewels, MachineProgressionState state, int bodySockets) {
        List<Jewel> active = new ArrayList<>();
        int treeSockets = MegaPassiveTree.allocatedSocketCount(state);
        for (int slot = 0; slot < treeSockets; slot++) {
            addIdentified(active, jewels.tree(slot));
        }
        for (int slot = 0; slot < bodySockets; slot++) {
            addIdentified(active, jewels.body(slot));
        }
        return active;
    }

    private static void addIdentified(List<Jewel> active, ItemStack stack) {
        if (!stack.isEmpty() && stack.has(ModDataComponents.MACHINE_TRAITS.get())) {
            active.add(new Jewel(stack.getHoverName(), traits(stack)));
        }
    }

    /** Whether the tree slot at {@code slot} is open on a machine with {@code state}. */
    public static boolean treeSlotOpen(MachineProgressionState state, int slot) {
        return slot < MegaPassiveTree.allocatedSocketCount(state);
    }

    /** Applies every active jewel's ordinary lines; {@code family} is null on a machine without Mastery, which reads them all. */
    public static void applyLines(MachineStatAccumulator stats, JewelHost host, MachineProgressionState state, MachineMasteryFamily family) {
        applyLines(stats, active(host.jewels(), state, host.bodySocketCount()), family);
    }

    public static void applyLines(MachineStatAccumulator stats, List<Jewel> jewels, MachineMasteryFamily family) {
        for (Jewel jewel : jewels) {
            try (MachineStatAccumulator.Source ignored = stats.source(jewel.name())) {
                for (MachineModifier modifier : jewel.traits().modifiers()) {
                    if (JewelAffixes.thresholdAttribute(modifier.affixId()) == null && applies(modifier, family)) {
                        stats.apply(modifier);
                    }
                }
            }
        }
    }

    /** Applies the threshold lines whose attribute total, read now that it is final, reaches {@link JewelAffixes#ATTRIBUTE_THRESHOLD}. */
    public static void applyThresholds(MachineStatAccumulator stats, JewelHost host, MachineProgressionState state, MachineMasteryFamily family) {
        applyThresholds(stats, active(host.jewels(), state, host.bodySocketCount()), family);
    }

    public static void applyThresholds(MachineStatAccumulator stats, List<Jewel> jewels, MachineMasteryFamily family) {
        List<Runnable> pending = new ArrayList<>();
        for (Jewel jewel : jewels) {
            for (MachineModifier modifier : jewel.traits().modifiers()) {
                MachineStat attribute = JewelAffixes.thresholdAttribute(modifier.affixId());
                if (attribute != null && applies(modifier, family) && stats.value(attribute) >= JewelAffixes.ATTRIBUTE_THRESHOLD) {
                    pending.add(() -> {
                        try (MachineStatAccumulator.Source ignored = stats.source(jewel.name())) {
                            stats.apply(modifier);
                        }
                    });
                }
            }
        }
        // Every threshold is read before any threshold line lands, so no line can count toward another's attribute.
        pending.forEach(Runnable::run);
    }

    /** Whether {@code family}'s machines read the line; a jewel line never counts toward a Mastery machine that ignores its stat. */
    public static boolean applies(MachineModifier modifier, MachineMasteryFamily family) {
        return family == null || family.supports(modifier.stat());
    }

    public static MachineTraits traits(ItemStack jewel) {
        MachineTraits traits = jewel.get(ModDataComponents.MACHINE_TRAITS.get());
        return traits == null ? MachineTraits.EMPTY : traits;
    }
}
