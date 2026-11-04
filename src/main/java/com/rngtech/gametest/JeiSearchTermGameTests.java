package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.compat.jei.JeiSearchTerms;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.gear.GearMachineSpec;
import com.rngtech.content.gear.GearSlotCatalog;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.jewel.JewelColour;
import com.rngtech.rpg.jewel.JewelHost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The search terms JEI registers find the items the wiki says they find. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JeiSearchTermGameTests {
    private JeiSearchTermGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void stageTermsMatchTheItemsStage(GameTestHelper helper) {
        Map<String, List<ItemStack>> terms = JeiSearchTerms.all();
        String three = JeiSearchTerms.stageTerm(3);
        check(finds(terms, three, ModItems.materialItem("tin_ingot").get()), "A Tin Ingot should be Stage 3");
        check(finds(terms, three, BuiltInRegistries.ITEM.get(RNGTech.id("tin_ore"))), "Tin Ore should be Stage 3");
        check(finds(terms, three, ModItems.jewel(JewelColour.values()[0]).get()), "A jewel should be Stage 3");
        check(finds(terms, JeiSearchTerms.stageTerm(1), ModItems.crusherChassis(CrusherChassisMaterial.IRON).get()),
                "An Iron Crusher should be Stage 1");
        check(!terms.containsKey(JeiSearchTerms.stageTerm(9)), "No item should be above Stage 8");

        Set<Item> seen = new HashSet<>();
        for (int stage = 0; stage <= 8; stage++) {
            for (ItemStack stack : terms.getOrDefault(JeiSearchTerms.stageTerm(stage), List.of())) {
                check(seen.add(stack.getItem()), stack.getItem() + " should have only one stage");
            }
        }
        helper.succeed();
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void currencyTermsCoverWhatYouSpend(GameTestHelper helper) {
        Map<String, List<ItemStack>> terms = JeiSearchTerms.all();
        check(finds(terms, JeiSearchTerms.CURRENCY, ModItems.CHAOS_CRYSTAL.get()), "currency should find the Chaos Crystal");
        check(finds(terms, JeiSearchTerms.CURRENCY, ModItems.modifierLenses().getFirst().get()), "currency should find modifier lenses");
        check(finds(terms, JeiSearchTerms.CURRENCY, ModItems.AFFIX_RESONANCE_MATRIX.get()), "currency should find forge upgrades");
        check(finds(terms, JeiSearchTerms.CURRENCY, ModItems.GLYPH.get()), "currency should find Glyphs");
        check(!finds(terms, JeiSearchTerms.CURRENCY, ModItems.PHASE_COIL.get()), "currency should not find Phase Coils");
        check(finds(terms, JeiSearchTerms.AFFIX_FORGE, ModItems.STABILIZATION_CRYSTAL.get()), "affix forge should find its crystals");
        check(finds(terms, JeiSearchTerms.PHASE_ARRAY, ModItems.PHASE_COIL.get()), "phase array should find Phase Coils");
        helper.succeed();
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void gearTermsMergeEveryMachineTier(GameTestHelper helper) {
        Map<String, List<ItemStack>> terms = JeiSearchTerms.all();
        String crusher = JeiSearchTerms.gearTerm("crusher");
        check(finds(terms, crusher, ModItems.crushHead(CrushHeadMaterial.values()[0]).get()), "gear crusher should find Crush Heads");
        check(finds(terms, crusher, ModItems.batteryCell(BatteryCellMaterial.values()[0]).get()), "gear crusher should find Battery Cells");
        check(terms.containsKey(JeiSearchTerms.gearTerm("furnace")), "Every Furnace tier should use gear furnace");
        check(terms.containsKey(JeiSearchTerms.gearTerm("fuel cell")), "The Fuel Cell should use the name players see");

        String tiers = ".*\\b(wooden|iron|copper|bronze|gold|steel|lead|aluminum|titanium|tungstensteel|exotic|crude)\\b.*";
        for (String term : terms.keySet()) {
            check(!term.startsWith("gear ") || !term.matches(tiers), "Gear term should not name a material tier: " + term);
        }
        List<String> gearTerms = terms.keySet().stream().filter(term -> term.startsWith("gear ")).toList();
        for (String term : gearTerms) {
            for (String other : gearTerms) {
                check(term.equals(other) || !other.contains(term), "JEI would also match " + other + " when searching " + term);
            }
        }
        helper.succeed();
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void jewelsListUnderEveryMachineThatHoldsThem(GameTestHelper helper) {
        Map<String, List<ItemStack>> terms = JeiSearchTerms.all();
        Item jewel = ModItems.jewel(JewelColour.values()[0]).get();
        for (GearMachineSpec spec : GearSlotCatalog.all()) {
            String term = JeiSearchTerms.gearTerm(spec);
            check(finds(terms, term, jewel) == holdsJewels(spec), term + " should list jewels only if that machine holds them");
        }
        check(finds(terms, JeiSearchTerms.gearTerm("crusher"), jewel), "gear crusher should list jewels");
        check(!finds(terms, JeiSearchTerms.gearTerm("affix forge"), jewel), "gear affix forge should not list jewels");
        helper.succeed();
    }

    private static boolean holdsJewels(GearMachineSpec spec) {
        return spec.displayStack().getItem() instanceof BlockItem item
                && item.getBlock() instanceof EntityBlock block
                && block.newBlockEntity(BlockPos.ZERO, item.getBlock().defaultBlockState()) instanceof JewelHost;
    }

    private static boolean finds(Map<String, List<ItemStack>> terms, String term, Item item) {
        return terms.getOrDefault(term, List.of()).stream().anyMatch(stack -> stack.is(item));
    }
}
