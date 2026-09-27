package com.rngtech.content.tool;

import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.item.ToolHeadItem;
import com.rngtech.content.item.ToolRodItem;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineStatAccumulator;
import com.rngtech.rpg.MachineTraits;

import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ToolBaseStatCatalog {
    private static final double EARLY_TREEFELLER_DURABILITY_SCALE = 0.80;
    private static final double MIN_ATTACK_SPEED = 0.20;
    private static final double MAX_CONTROL_DURABILITY_PROTECTION_SHARE = 1.0;
    private static final Set<MachineStat> PART_HARD_GATE_STATS = EnumSet.of(MachineStat.MINING_LEVEL);
    private static final List<MachineStat> HEAD_SUMMARY = List.of(
            MachineStat.MINING_LEVEL,
            MachineStat.MINING_SPEED,
            MachineStat.ATTACK_SPEED,
            MachineStat.DURABILITY,
            MachineStat.SELF_REPAIR,
            MachineStat.AREA_WIDTH,
            MachineStat.AREA_HEIGHT,
            MachineStat.TREE_FELL_LIMIT,
            MachineStat.VEIN_MINE_LIMIT,
            MachineStat.VEIN_MINE_FE_USAGE,
            MachineStat.FE_USAGE,
            MachineStat.STABILITY
    );
    private static final List<MachineStat> ROD_SUMMARY = List.of(
            MachineStat.BATTERY_SUPPORT,
            MachineStat.FE_TRANSFER,
            MachineStat.DURABILITY,
            MachineStat.SELF_REPAIR,
            MachineStat.MINING_SPEED,
            MachineStat.ATTACK_SPEED,
            MachineStat.FE_USAGE,
            MachineStat.STABILITY,
            MachineStat.CONTROL
    );
    private static final List<MachineStat> TOOL_SUMMARY = List.of(
            MachineStat.MINING_LEVEL,
            MachineStat.MINING_SPEED,
            MachineStat.ATTACK_SPEED,
            MachineStat.DURABILITY,
            MachineStat.SELF_REPAIR,
            MachineStat.BATTERY_SUPPORT,
            MachineStat.FE_TRANSFER,
            MachineStat.FE_USAGE,
            MachineStat.AREA_WIDTH,
            MachineStat.AREA_HEIGHT,
            MachineStat.TREE_FELL_LIMIT,
            MachineStat.VEIN_MINE_LIMIT,
            MachineStat.VEIN_MINE_FE_USAGE,
            MachineStat.ORE_BURST_SPEED,
            MachineStat.LUCK,
            MachineStat.STABILITY,
            MachineStat.CONTROL
    );

    public static MachineStatAccumulator baseStats(ItemStack stack) {
        if (stack.getItem() instanceof ToolHeadItem head) {
            return headBase(head.family(), head.material(), stack);
        }
        if (stack.getItem() instanceof ToolRodItem rod) {
            return rodBase(rod.material());
        }
        if (stack.getItem() instanceof ModularToolItem) {
            return assembledBase(stack);
        }
        return null;
    }

    public static MachineStatAccumulator effectiveStats(ItemStack stack) {
        if (stack.getItem() instanceof ToolHeadItem || stack.getItem() instanceof ToolRodItem) {
            MachineStatAccumulator stats = baseStats(stack);
            if (stats == null) {
                return null;
            }
            for (MachineModifier modifier : traits(stack).modifiers()) {
                if (modifier.slot().isAffix() && !PART_HARD_GATE_STATS.contains(modifier.stat())) {
                    stats.apply(modifier);
                }
            }
            return stats;
        }
        if (stack.getItem() instanceof ModularToolItem) {
            return assembledBase(stack);
        }
        return null;
    }

    public static List<MachineStat> summaryStats(ItemStack stack) {
        if (stack.getItem() instanceof ToolHeadItem) {
            return HEAD_SUMMARY;
        }
        if (stack.getItem() instanceof ToolRodItem) {
            return ROD_SUMMARY;
        }
        if (stack.getItem() instanceof ModularToolItem) {
            return TOOL_SUMMARY;
        }
        return List.of();
    }

    public static int maxDurability(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 1 : Math.max(1, (int) Math.round(stats.value(MachineStat.DURABILITY)));
    }

    public static int selfRepairAmount(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 0 : Math.max(0, (int) Math.floor(stats.value(MachineStat.SELF_REPAIR)));
    }

    public static int miningLevel(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 0 : Math.max(0, (int) Math.floor(stats.value(MachineStat.MINING_LEVEL)));
    }

    public static float miningSpeed(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 1.0F : Math.max(1.0F, (float) stats.value(MachineStat.MINING_SPEED));
    }

    public static float attackSpeed(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 1.0F : Math.max((float) MIN_ATTACK_SPEED, (float) stats.value(MachineStat.ATTACK_SPEED));
    }

    public static int feUsage(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 0 : Math.max(0, (int) Math.round(stats.value(MachineStat.FE_USAGE)));
    }

    public static int feTransfer(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 0 : Math.max(0, (int) Math.round(stats.value(MachineStat.FE_TRANSFER)));
    }

    public static int batterySupport(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 0 : Math.max(0, (int) Math.floor(stats.value(MachineStat.BATTERY_SUPPORT)));
    }

    public static int areaWidth(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 1 : oddAtLeastOne(stats.value(MachineStat.AREA_WIDTH));
    }

    public static int areaHeight(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 1 : oddAtLeastOne(stats.value(MachineStat.AREA_HEIGHT));
    }

    public static int treeFellLimit(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 1 : Math.max(1, (int) Math.floor(stats.value(MachineStat.TREE_FELL_LIMIT)));
    }

    public static int veinMineLimit(ItemStack stack) {
        if (!isPickHeadStack(stack)) {
            return 1;
        }
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 1 : Math.max(1, (int) Math.floor(stats.value(MachineStat.VEIN_MINE_LIMIT)));
    }

    public static int veinMineFeUsage(ItemStack stack) {
        if (!isPickHeadStack(stack) || veinMineLimit(stack) <= 1) {
            return 0;
        }
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 0 : Math.max(0, (int) Math.round(stats.value(MachineStat.VEIN_MINE_FE_USAGE)));
    }

    public static boolean hasOreBurstMode(ItemStack stack) {
        if (!(stack.getItem() instanceof ModularToolItem tool)) {
            return false;
        }
        FieldToolAssembly assembly = ModularToolItem.assembly(stack);
        return assembly.isValidFor(tool.family())
                && tool.family().supportsOreBurst()
                && (assembly.headMaterial().oreBurst() || assembly.rodMaterial().oreBurst());
    }

    public static double oreBurstSpeed(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null || !hasOreBurstMode(stack) ? 0.0 : Math.max(0.0, stats.value(MachineStat.ORE_BURST_SPEED));
    }

    public static int oreBurstFeUsage(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null || !hasOreBurstMode(stack) ? 0 : Math.max(0, (int) Math.round(stats.value(MachineStat.ORE_BURST_FE_USAGE)));
    }

    public static int luck(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 0 : Math.max(0, (int) Math.floor(stats.value(MachineStat.LUCK)));
    }

    public static double stability(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 1.0 : Math.max(0.0, stats.value(MachineStat.STABILITY));
    }

    public static double control(ItemStack stack) {
        MachineStatAccumulator stats = effectiveStats(stack);
        return stats == null ? 1.0 : Math.max(0.0, stats.value(MachineStat.CONTROL));
    }

    public static double stabilityCostAvoidanceChance(ItemStack stack) {
        return Math.max(0.0, Math.min(1.0, stability(stack) - 1.0));
    }

    public static double controlDurabilityProtectionShare(ItemStack stack) {
        return Math.max(0.0, Math.min(MAX_CONTROL_DURABILITY_PROTECTION_SHARE, control(stack) - 1.0));
    }

    public static int controlDurabilityProtectionFeCost(ItemStack stack) {
        if (!(stack.getItem() instanceof ModularToolItem tool) || !ModularToolItem.hasValidAssembly(stack)) {
            return 0;
        }
        double protectionShare = controlDurabilityProtectionShare(stack);
        return protectionShare <= 0.0
                ? 0
                : Math.max(1, (int) Math.round(tool.family().durabilityProtectionFeCost() * protectionShare));
    }

    private static MachineStatAccumulator assembledBase(ItemStack stack) {
        if (!(stack.getItem() instanceof ModularToolItem tool)) {
            return MachineStatAccumulator.componentBase(Map.of(MachineStat.DURABILITY, 1.0));
        }
        FieldToolAssembly assembly = ModularToolItem.assembly(stack);
        if (!assembly.isValidFor(tool.family())) {
            return MachineStatAccumulator.componentBase(Map.of(MachineStat.DURABILITY, 1.0));
        }
        ToolHeadFamily family = tool.family();

        MachineStatAccumulator head = effectiveStats(assembly.head());
        MachineStatAccumulator rod = effectiveStats(assembly.rod());
        Map<MachineStat, Double> values = new EnumMap<>(MachineStat.class);
        put(values, MachineStat.MINING_LEVEL, value(head, MachineStat.MINING_LEVEL));
        put(values, MachineStat.MINING_SPEED, Math.max(1.0, value(head, MachineStat.MINING_SPEED) + value(rod, MachineStat.MINING_SPEED)));
        put(values, MachineStat.ATTACK_SPEED, Math.max(MIN_ATTACK_SPEED, value(head, MachineStat.ATTACK_SPEED) + value(rod, MachineStat.ATTACK_SPEED)));
        put(values, MachineStat.DURABILITY, Math.max(1.0, value(head, MachineStat.DURABILITY) + value(rod, MachineStat.DURABILITY)));
        put(values, MachineStat.SELF_REPAIR, value(head, MachineStat.SELF_REPAIR) + value(rod, MachineStat.SELF_REPAIR));
        put(values, MachineStat.FE_USAGE, Math.max(0.0, value(head, MachineStat.FE_USAGE) + value(rod, MachineStat.FE_USAGE)));
        put(values, MachineStat.FE_TRANSFER, Math.max(0.0, value(rod, MachineStat.FE_TRANSFER)));
        put(values, MachineStat.BATTERY_SUPPORT, Math.max(0.0, value(rod, MachineStat.BATTERY_SUPPORT)));
        put(values, MachineStat.AREA_WIDTH, family.minesArea() ? value(head, MachineStat.AREA_WIDTH) : 1.0);
        put(values, MachineStat.AREA_HEIGHT, family.minesArea() ? value(head, MachineStat.AREA_HEIGHT) : 1.0);
        put(values, MachineStat.TREE_FELL_LIMIT, family == ToolHeadFamily.TREEFELLER
                ? value(head, MachineStat.TREE_FELL_LIMIT) + value(rod, MachineStat.TREE_FELL_LIMIT)
                : 1.0);
        put(values, MachineStat.VEIN_MINE_LIMIT, family == ToolHeadFamily.PICK
                ? value(head, MachineStat.VEIN_MINE_LIMIT)
                : 0.0);
        put(values, MachineStat.VEIN_MINE_FE_USAGE, family == ToolHeadFamily.PICK
                ? value(head, MachineStat.VEIN_MINE_FE_USAGE)
                : 0.0);
        put(values, MachineStat.STABILITY, Math.max(0.1, (value(head, MachineStat.STABILITY) + value(rod, MachineStat.STABILITY)) / 2.0));
        put(values, MachineStat.CONTROL, Math.max(0.1, value(rod, MachineStat.CONTROL)));
        put(values, MachineStat.LUCK, value(head, MachineStat.LUCK) + value(rod, MachineStat.LUCK));
        put(values, MachineStat.ORE_BURST_SPEED, Math.max(value(head, MachineStat.ORE_BURST_SPEED), value(rod, MachineStat.ORE_BURST_SPEED)));
        put(values, MachineStat.ORE_BURST_DURATION, value(head, MachineStat.ORE_BURST_DURATION) + value(rod, MachineStat.ORE_BURST_DURATION));
        put(values, MachineStat.ORE_BURST_FE_USAGE, Math.max(0.0, value(head, MachineStat.ORE_BURST_FE_USAGE) + value(rod, MachineStat.ORE_BURST_FE_USAGE)));
        return MachineStatAccumulator.componentBase(values);
    }

    private static MachineStatAccumulator headBase(ToolHeadFamily family, ToolHeadMaterial material, ItemStack stack) {
        Map<MachineStat, Double> values = new EnumMap<>(MachineStat.class);
        put(values, MachineStat.MINING_LEVEL, material.miningLevel() + (ToolHeadItem.isDiamondTipped(stack) ? 1.0 : 0.0));
        put(values, MachineStat.MINING_SPEED, material.miningSpeed() * family.speedScale());
        put(values, MachineStat.ATTACK_SPEED, family.attackSpeed());
        put(values, MachineStat.DURABILITY, headDurability(family, material));
        put(values, MachineStat.FE_USAGE, family.feUsage());
        put(values, MachineStat.AREA_WIDTH, family.minesArea() ? 3.0 : 1.0);
        put(values, MachineStat.AREA_HEIGHT, family.minesArea() ? 3.0 : 1.0);
        put(values, MachineStat.TREE_FELL_LIMIT, family == ToolHeadFamily.TREEFELLER ? 12.0 + material.stage() * 4.0 : 1.0);
        put(values, MachineStat.VEIN_MINE_LIMIT, 0.0);
        put(values, MachineStat.VEIN_MINE_FE_USAGE, 0.0);
        put(values, MachineStat.STABILITY, material.stability());
        put(values, MachineStat.CONTROL, 1.0);
        put(values, MachineStat.LUCK, 0.0);
        if (family.supportsOreBurst() && material.oreBurst()) {
            put(values, MachineStat.ORE_BURST_SPEED, 0.20 + material.stage() * 0.03);
            put(values, MachineStat.ORE_BURST_DURATION, 80.0);
            put(values, MachineStat.ORE_BURST_FE_USAGE, 20.0 + material.stage() * 4.0);
        }
        return MachineStatAccumulator.componentBase(values);
    }

    private static double headDurability(ToolHeadFamily family, ToolHeadMaterial material) {
        double durability = material.durability() * family.durabilityScale();
        if (family == ToolHeadFamily.TREEFELLER
                && (material == ToolHeadMaterial.IRON || material == ToolHeadMaterial.COPPER)) {
            return durability * EARLY_TREEFELLER_DURABILITY_SCALE;
        }
        return durability;
    }

    private static MachineStatAccumulator rodBase(ToolRodMaterial material) {
        Map<MachineStat, Double> values = new EnumMap<>(MachineStat.class);
        put(values, MachineStat.BATTERY_SUPPORT, material.batterySupport());
        put(values, MachineStat.FE_TRANSFER, material.feTransfer());
        put(values, MachineStat.DURABILITY, material.durability());
        put(values, MachineStat.MINING_SPEED, material.miningSpeed());
        put(values, MachineStat.ATTACK_SPEED, material.attackSpeed());
        put(values, MachineStat.FE_USAGE, material.feUsageAdjustment());
        put(values, MachineStat.TREE_FELL_LIMIT, material.treeLimitBonus());
        put(values, MachineStat.STABILITY, material.stability());
        put(values, MachineStat.CONTROL, material.control());
        put(values, MachineStat.LUCK, 0.0);
        if (material.oreBurst()) {
            put(values, MachineStat.ORE_BURST_SPEED, 0.12 + material.stage() * 0.02);
            put(values, MachineStat.ORE_BURST_DURATION, 20.0 + material.stage() * 4.0);
            put(values, MachineStat.ORE_BURST_FE_USAGE, 16.0 + material.stage() * 3.0);
        }
        return MachineStatAccumulator.componentBase(values);
    }

    private static MachineTraits traits(ItemStack stack) {
        if (stack.getItem() instanceof ToolHeadItem head) {
            return head.traits(stack);
        }
        if (stack.getItem() instanceof ToolRodItem rod) {
            return rod.traits(stack);
        }
        return MachineTraits.EMPTY;
    }

    private static double value(MachineStatAccumulator stats, MachineStat stat) {
        return stats == null ? 0.0 : stats.value(stat);
    }

    private static boolean isPickHeadStack(ItemStack stack) {
        if (stack.getItem() instanceof ToolHeadItem head) {
            return head.family() == ToolHeadFamily.PICK;
        }
        if (stack.getItem() instanceof ModularToolItem tool) {
            return tool.family() == ToolHeadFamily.PICK;
        }
        return false;
    }

    private static void put(Map<MachineStat, Double> values, MachineStat stat, double value) {
        values.put(stat, value);
    }

    private static int oddAtLeastOne(double value) {
        int rounded = Math.max(1, (int) Math.round(value));
        return rounded % 2 == 0 ? rounded + 1 : rounded;
    }

    private ToolBaseStatCatalog() {
    }
}
