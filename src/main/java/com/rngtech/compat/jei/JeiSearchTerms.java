package com.rngtech.compat.jei;

import com.rngtech.RNGTech;
import com.rngtech.content.gear.GearMachineSpec;
import com.rngtech.content.gear.GearSlotCatalog;
import com.rngtech.content.gear.GearSlotSpec;
import com.rngtech.content.item.EnergyConnectorItem;
import com.rngtech.content.item.FluidConnectorItem;
import com.rngtech.content.item.ForestryCartItem;
import com.rngtech.content.item.ItemConnectorItem;
import com.rngtech.content.item.JewelItem;
import com.rngtech.content.item.MachineBlockItem;
import com.rngtech.content.item.OreBlockItem;
import com.rngtech.content.item.PruningShearsItem;
import com.rngtech.content.itemfilter.ItemFilterMatcher;
import com.rngtech.content.material.MaterialCatalog;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineType;
import com.rngtech.rpg.jewel.JewelColour;
import com.rngtech.rpg.unique.UniqueItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/** The search terms RNGTech adds to JEI, and the items each one finds. */
public final class JeiSearchTerms {
    public static final String CURRENCY = "currency";
    public static final String AFFIX_FORGE = "affix forge";
    public static final String PHASE_ARRAY = "phase array";

    /** Machines whose type name differs from the name players see. */
    private static final Map<MachineType, String> MACHINE_NAMES = Map.of(
            MachineType.ELECTRIC_FURNACE, "furnace",
            MachineType.AMMONIA_FUEL_CELL, "fuel cell"
    );
    /** JEI matches terms as substrings, so the station's term must not contain {@code gear forestry cart}. */
    private static final String FORESTRY_CART_STATION_SPEC = "forestry_cart_station";
    private static final String FORESTRY_CART_STATION_NAME = "cart station";

    private JeiSearchTerms() {
    }

    public static Map<String, List<ItemStack>> all() {
        Map<String, Set<Item>> terms = new LinkedHashMap<>();
        addStages(terms);
        addAffixForge(terms);
        addPhaseArray(terms);
        addGear(terms);
        Map<String, List<ItemStack>> stacks = new LinkedHashMap<>();
        terms.forEach((term, items) -> stacks.put(term, items.stream().map(ItemStack::new).toList()));
        return stacks;
    }

    public static String stageTerm(int stage) {
        return "stage " + stage;
    }

    public static String gearTerm(String machineName) {
        return "gear " + machineName;
    }

    public static String gearTerm(GearMachineSpec spec) {
        return gearTerm(machineName(spec));
    }

    private static void addStages(Map<String, Set<Item>> terms) {
        for (Item item : BuiltInRegistries.ITEM) {
            OptionalInt stage = stage(new ItemStack(item));
            if (stage.isPresent()) {
                add(terms, stageTerm(stage.getAsInt()), item);
            }
        }
    }

    private static OptionalInt stage(ItemStack stack) {
        if (UniqueItems.definition(stack) != null) {
            return OptionalInt.of(UniqueItems.slotStage(stack));
        }
        OptionalInt classified = ItemFilterMatcher.componentStage(stack);
        if (classified.isPresent()) {
            return classified;
        }
        Item item = stack.getItem();
        if (item instanceof JewelItem) {
            return OptionalInt.of(JewelColour.COMPONENT_STAGE);
        }
        if (item instanceof ForestryCartItem) {
            return OptionalInt.of(ForestryCartItem.COMPONENT_STAGE);
        }
        if (item instanceof OreBlockItem ore) {
            int stage = MaterialCatalog.materialStage(ore.definition().materialId());
            return stage < 0 ? OptionalInt.empty() : OptionalInt.of(stage);
        }
        if (item instanceof PruningShearsItem shears) {
            return OptionalInt.of(shears.material().stage());
        }
        if (item instanceof EnergyConnectorItem connector) {
            return connectorStage(connector.tier().stage());
        }
        if (item instanceof FluidConnectorItem connector) {
            return connectorStage(connector.tier().stage());
        }
        if (item instanceof ItemConnectorItem connector) {
            return connectorStage(connector.tier().stage());
        }
        return OptionalInt.empty();
    }

    /** Stage 0 connector tiers state no stage on their tooltips, so they get no term either. */
    private static OptionalInt connectorStage(int stage) {
        return stage > 0 ? OptionalInt.of(stage) : OptionalInt.empty();
    }

    private static void addAffixForge(Map<String, Set<Item>> terms) {
        for (AffixForgeJeiRecipe recipe : AffixForgeJeiRecipe.recipes()) {
            addForgeItem(terms, recipe.catalyst());
            recipe.optionalModifiers().forEach(modifier -> addForgeItem(terms, modifier));
        }
        addForgeItem(terms, new ItemStack(ModItems.AFFIX_LENS_ARRAY.get()));
        addForgeItem(terms, new ItemStack(ModItems.AFFIX_MODIFIER_SOCKET.get()));
        addForgeItem(terms, new ItemStack(ModItems.AFFIX_RESONANCE_MATRIX.get()));
    }

    private static void addForgeItem(Map<String, Set<Item>> terms, ItemStack stack) {
        add(terms, AFFIX_FORGE, stack.getItem());
        add(terms, CURRENCY, stack.getItem());
    }

    private static void addPhaseArray(Map<String, Set<Item>> terms) {
        add(terms, PHASE_ARRAY, ModItems.EXOTIC_AFFIX_FORGE.get());
        add(terms, PHASE_ARRAY, ModItems.PHASE_EMITTER.get());
        add(terms, PHASE_ARRAY, ModItems.PHASE_COIL.get());
        add(terms, PHASE_ARRAY, ModItems.GLYPH.get());
        add(terms, CURRENCY, ModItems.GLYPH.get());
    }

    private static void addGear(Map<String, Set<Item>> terms) {
        for (GearMachineSpec spec : GearSlotCatalog.all()) {
            String term = gearTerm(spec);
            for (GearSlotSpec slot : spec.slots()) {
                slot.validStacks().forEach(stack -> add(terms, term, stack.getItem()));
            }
            if (takesJewels(spec)) {
                ModItems.JEWELS.values().forEach(jewel -> add(terms, term, jewel.get()));
            }
        }
    }

    /** Jewels fit a separate tab rather than a Gear slot, on every machine whose item is a {@link MachineBlockItem}. */
    private static boolean takesJewels(GearMachineSpec spec) {
        return spec.displayStack().getItem() instanceof MachineBlockItem;
    }

    private static String machineName(GearMachineSpec spec) {
        if (spec.displayStack().getItem() instanceof MachineBlockItem machine) {
            MachineType type = machine.machineType();
            return MACHINE_NAMES.getOrDefault(type, words(type.name()));
        }
        String path = spec.id().getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.equals(FORESTRY_CART_STATION_SPEC) ? FORESTRY_CART_STATION_NAME : words(name);
    }

    private static String words(String id) {
        return id.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static void add(Map<String, Set<Item>> terms, String term, Item item) {
        if (RNGTech.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
            terms.computeIfAbsent(term, unused -> new LinkedHashSet<>()).add(item);
        }
    }
}
