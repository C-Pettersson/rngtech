package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.PhaseCoilBlock;
import com.rngtech.content.block.PhaseEmitterBlock;
import com.rngtech.content.blockentity.PhaseCradleBlockEntity;
import com.rngtech.content.item.GlyphItem;
import com.rngtech.content.recipe.PhaseArrayRecipe;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModRecipes;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierEligibilityProfiles;
import com.rngtech.rpg.ModifierFlag;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.refinement.PhaseArrayAction;
import com.rngtech.rpg.refinement.PhaseStrain;
import com.rngtech.rpg.refinement.RefinementTargets;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase Array line: a Cradle, three to nine Coils, and an Emitter in a straight horizontal row form; anything else does
 * not. The empty 5x5x5 template fits a three-Coil line, the shortest legal one.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PhaseArrayGameTests {
    private static final BlockPos CRADLE = new BlockPos(0, 1, 2);
    private static final int TIMEOUT = 100;

    private PhaseArrayGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void threeCoilLineForms(GameTestHelper helper) {
        line(helper, Direction.EAST, 3);
        PhaseCradleBlockEntity cradle = cradle(helper);
        helper.succeedWhen(() -> {
            check(cradle.isLineFormed(), "A Cradle, three Coils and an Emitter should form, found " + cradle.lineCoilCount() + " Coils");
            check(cradle.lineCoilCount() == 3, "Three Coils should be counted, found " + cradle.lineCoilCount());
            check(cradle.lineAxis() == Direction.EAST, "The line runs east of the Cradle, found " + cradle.lineAxis());
            check(cradle.ownsMember(helper.absolutePos(CRADLE.east(4))), "The Emitter should be a formed member");
            check(helper.getBlockState(CRADLE.east(1)).getValue(PhaseCoilBlock.FORMED), "Coils should light when formed");
            check(helper.getBlockState(CRADLE.east(4)).getValue(PhaseEmitterBlock.FORMED), "The Emitter should light when formed");
        });
    }

    /** A line with no Glyph seated fires nothing; a Tier I Glyph in seat I opens positions 1 to 3 and the Coils show it. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void seatedGlyphsOpenPositions(GameTestHelper helper) {
        line(helper, Direction.EAST, 3);
        PhaseCradleBlockEntity cradle = cradle(helper);
        helper.runAfterDelay(5, () -> {
            check(cradle.isLineFormed(), "The line should form with no Glyph seated");
            check(cradle.usableCoils() == 0, "No seated Glyph opens no positions, found " + cradle.usableCoils());
            check(helper.getBlockState(CRADLE.east(1)).getValue(PhaseCoilBlock.TIER) == 0, "A closed position shows no tier");
            seat(cradle, 1);
        });
        helper.succeedWhen(() -> {
            check(cradle.usableCoils() == 3, "A Tier I Glyph opens positions 1 to 3, found " + cradle.usableCoils());
            check(helper.getBlockState(CRADLE.east(1)).getValue(PhaseCoilBlock.TIER) == 1, "An open position shows the tier it runs at");
        });
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void twoCoilsDoNotForm(GameTestHelper helper) {
        helper.setBlock(CRADLE.east(1), ModBlocks.PHASE_COIL.get());
        helper.setBlock(CRADLE.east(2), ModBlocks.PHASE_COIL.get());
        helper.setBlock(CRADLE.east(3), ModBlocks.PHASE_EMITTER.get());
        assertNeverForms(helper, cradle(helper), 2);
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void missingEmitterDoesNotForm(GameTestHelper helper) {
        line(helper, Direction.EAST, 3);
        helper.setBlock(CRADLE.east(4), Blocks.AIR);
        assertNeverForms(helper, cradle(helper), 3);
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void lineOnAnySideForms(GameTestHelper helper) {
        BlockPos cradlePos = new BlockPos(4, 1, 2);
        for (int i = 1; i <= 3; i++) {
            helper.setBlock(cradlePos.west(i), ModBlocks.PHASE_COIL.get());
        }
        helper.setBlock(cradlePos.west(4), ModBlocks.PHASE_EMITTER.get());
        helper.setBlock(cradlePos, ModBlocks.EXOTIC_AFFIX_FORGE.get());
        PhaseCradleBlockEntity cradle = (PhaseCradleBlockEntity) helper.getBlockEntity(cradlePos);
        helper.succeedWhen(() -> {
            check(cradle.isLineFormed(), "A line west of the Cradle should form");
            check(cradle.lineAxis() == Direction.WEST, "The axis should point west, found " + cradle.lineAxis());
        });
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void brokenCoilUnforms(GameTestHelper helper) {
        line(helper, Direction.EAST, 3);
        PhaseCradleBlockEntity cradle = cradle(helper);
        helper.runAfterDelay(5, () -> {
            check(cradle.isLineFormed(), "The line should form before a Coil is broken");
            helper.setBlock(CRADLE.east(2), Blocks.AIR);
        });
        helper.succeedWhen(() -> {
            check(!cradle.isLineFormed(), "Breaking a Coil should unform the line");
            check(!helper.getBlockState(CRADLE.east(1)).getValue(PhaseCoilBlock.FORMED), "The other Coils should go dark");
            check(!helper.getBlockState(CRADLE.east(4)).getValue(PhaseEmitterBlock.FORMED), "The Emitter should go dark");
        });
    }

    /** A charged Reforge on an inscribed three-Coil line fires: the target moves to the output with one Strain. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 600)
    public static void reforgePulseFiresOnce(GameTestHelper helper) {
        line(helper, Direction.EAST, 3);
        PhaseCradleBlockEntity cradle = cradle(helper);
        ItemStack target = new ItemStack(ModItems.PHASE_COIL.get());
        // A bare part has Quality 0, so the pulse costs 3 MFE and charges well inside the timeout.
        RefinementTargets.setTraits(target, cheapTraits(0));
        // Registered up front: a delayed task must not add tick tasks while the harness iterates them. The charge
        // storage accepts nothing until the Cradle is charging, so early power is harmless.
        MachineTestSupport.powerEachTick(helper, cradle.getEnergyStorage(Direction.UP));
        helper.runAfterDelay(5, () -> seat(cradle, 1));
        helper.runAfterDelay(10, () -> {
            check(cradle.usableCoils() == 3, "Three Coils open on a seated Tier I Glyph, found " + cradle.usableCoils());
            cradle.getItems().setStackInSlot(PhaseCradleBlockEntity.SLOT_TARGET, target);
            cradle.setAction(PhaseArrayAction.REFORGE);
            check(cradle.status() == PhaseCradleBlockEntity.STATUS_IDLE, "A Reforge on a fresh target should be ready to charge, status " + cradle.status());
            cradle.toggleCharging();
        });
        helper.runAfterDelay(15, () -> check(cradle.charge() > 0, "Supply should charge the line"));
        boolean[] sawPulse = { false };
        helper.succeedWhen(() -> {
            if (cradle.status() == PhaseCradleBlockEntity.STATUS_READY) {
                check(cradle.charge() >= cradle.cost(cradle.recipe()), "Ready means the charge covers the cost");
                check(cradle.fire(), "A ready Cradle should fire");
                check(cradle.isPulsing(), "Fire starts the hold sequence");
                check(cradle.outputStack().isEmpty(), "The outcome waits for the sequence to end");
            }
            if (cradle.isPulsing()) {
                sawPulse[0] |= helper.getBlockState(CRADLE.east(4)).getValue(BaseMachineBlock.ACTIVE);
            }
            check(sawPulse[0], "The Emitter should light while the pulse runs");
            check(!cradle.isPulsing(), "The sequence should end");
            check(!helper.getBlockState(CRADLE.east(1)).getValue(BaseMachineBlock.ACTIVE), "Coils go dark after the pulse");
            ItemStack output = cradle.outputStack();
            check(!output.isEmpty(), "The refined item should be in the output");
            check(cradle.targetStack().isEmpty(), "The target slot should be empty after the pulse");
            check(PhaseStrain.get(output) >= 1, "A pulse adds Strain, found " + PhaseStrain.get(output));
            check(RefinementTargets.canRefine(output), "The output should still be refinable");
        });
    }

    /** Attune on a four-Coil line keeps every affix's identity and tier, rolls their values, and adds one Strain. */
    @GameTest(template = MachineTestSupport.LINE, timeoutTicks = 600)
    public static void attunePulseKeepsTiers(GameTestHelper helper) {
        BlockPos origin = new BlockPos(0, 1, 2);
        line(helper, origin, Direction.EAST, 4);
        PhaseCradleBlockEntity cradle = cradle(helper, origin);
        ItemStack target = new ItemStack(ModItems.PHASE_COIL.get());
        // Attune needs affixes to reroll; four Tier 1 affixes keep Quality 4 and the pulse cheap.
        MachineTraits traits = cheapTraits(4);
        RefinementTargets.setTraits(target, traits);
        List<String> before = affixSummary(traits);
        MachineTestSupport.powerEachTick(helper, cradle.getEnergyStorage(Direction.UP));
        helper.runAfterDelay(5, () -> {
            // Position 4 needs Tier II, so seats I and II open all four.
            seat(cradle, 1);
            seat(cradle, 2);
        });
        helper.runAfterDelay(10, () -> {
            check(cradle.usableCoils() == 4, "Seats I and II open four positions, found " + cradle.usableCoils());
            cradle.getItems().setStackInSlot(PhaseCradleBlockEntity.SLOT_TARGET, target);
            cradle.setAction(PhaseArrayAction.ATTUNE);
            check(cradle.status() == PhaseCradleBlockEntity.STATUS_IDLE, "Attune on a fresh part should be ready to charge, status " + cradle.status());
            cradle.toggleCharging();
        });
        helper.succeedWhen(() -> {
            if (cradle.status() == PhaseCradleBlockEntity.STATUS_READY) {
                check(cradle.fire(), "A ready Cradle should fire");
            }
            check(!cradle.isPulsing(), "The sequence should end");
            ItemStack output = cradle.outputStack();
            check(!output.isEmpty(), "The attuned item should be in the output");
            check(PhaseStrain.get(output) == 1, "One pulse adds one Strain, found " + PhaseStrain.get(output));
            check(affixSummary(RefinementTargets.storedTraits(output)).equals(before), "Attune keeps every affix and its tier");
        });
    }

    /** Breaking a Coil mid-charge unforms the line; the charge already gathered waits for it to re-form. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 600)
    public static void brokenCoilKeepsCharge(GameTestHelper helper) {
        line(helper, Direction.EAST, 3);
        PhaseCradleBlockEntity cradle = cradle(helper);
        ItemStack target = new ItemStack(ModItems.PHASE_COIL.get());
        RefinementTargets.setTraits(target, cheapTraits(0));
        MachineTestSupport.powerEachTick(helper, cradle.getEnergyStorage(Direction.UP));
        helper.runAfterDelay(5, () -> seat(cradle, 1));
        helper.runAfterDelay(10, () -> {
            cradle.getItems().setStackInSlot(PhaseCradleBlockEntity.SLOT_TARGET, target);
            cradle.setAction(PhaseArrayAction.REFORGE);
            cradle.toggleCharging();
        });
        long[] kept = { -1L };
        helper.runAfterDelay(20, () -> {
            check(cradle.charge() > 0, "Supply should have charged the line");
            kept[0] = cradle.charge();
            helper.setBlock(CRADLE.east(2), Blocks.AIR);
        });
        // The Cradle notices the break on its next scan, so one more tick of supply may land first; the charge
        // must then hold still at or above what was read.
        long[] settled = { -1L };
        helper.succeedWhen(() -> {
            check(kept[0] > 0, "The charge is read before the Coil breaks");
            check(!cradle.isLineFormed(), "Breaking a Coil should unform the line");
            check(!cradle.isCharging(), "An unformed line stops charging");
            check(cradle.charge() >= kept[0], "The charge should be kept, found " + cradle.charge() + " of " + kept[0]);
            if (settled[0] < 0) {
                settled[0] = cradle.charge();
                check(false, "Waiting a tick to see the charge hold still");
            }
            check(cradle.charge() == settled[0], "An unformed line takes no more FE, found " + cradle.charge() + " after " + settled[0]);
        });
    }

    /** Strain is an optional component: absent reads 0, a value round-trips, and 0 removes it again. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void strainRoundTrips(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.PHASE_COIL.get());
        check(PhaseStrain.get(stack) == 0, "A fresh item has no Strain");
        PhaseStrain.set(stack, 3);
        check(PhaseStrain.get(stack) == 3, "Strain reads back");
        PhaseStrain.add(stack, 2);
        check(PhaseStrain.get(stack) == 5, "Strain adds up");
        PhaseStrain.set(stack, 0);
        check(!stack.has(ModDataComponents.STRAIN.get()), "Strain 0 leaves no component behind");
        helper.succeed();
    }

    /** A pack retunes an operation by overriding its recipe; the nine defaults load with the PRD's numbers. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = TIMEOUT)
    public static void datapackRetunesOperations(GameTestHelper helper) {
        List<RecipeHolder<PhaseArrayRecipe>> loaded = helper.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.PHASE_ARRAY_TYPE.get());
        check(loaded.size() == PhaseArrayAction.values().length, "One default recipe per operation, found " + loaded.size());
        PhaseArrayRecipe resonate = loaded.stream().map(RecipeHolder::value)
                .filter(recipe -> recipe.action() == PhaseArrayAction.RESONATE).findFirst().orElse(null);
        check(resonate != null && resonate.baseFe() == 2_000_000L && resonate.refinementPotentialCost() == 2, "Resonate loads with 2 MFE and 2 RP");
        JsonObject json = new JsonObject();
        json.addProperty("action", "resonate");
        json.addProperty("coils", 5);
        json.addProperty("base_fe", 3_000_000L);
        json.addProperty("refinement_potential_cost", 0);
        PhaseArrayRecipe retuned = ModRecipes.PHASE_ARRAY_SERIALIZER.get().codec().codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        check(retuned.baseFe() == 3_000_000L && retuned.refinementPotentialCost() == 0 && retuned.glyphTier() == 0,
                "A recipe with a changed cost parses, with defaults for what it leaves out");
        helper.succeed();
    }

    /** Rare Coil traits with {@code affixes} Tier 1 affixes, half prefixes and half suffixes, so a pulse's cost is known. */
    private static MachineTraits cheapTraits(int affixes) {
        RandomSource random = RandomSource.create(1L);
        List<MachineModifier> modifiers = new ArrayList<>();
        for (ModifierSlot slot : List.of(ModifierSlot.PREFIX, ModifierSlot.SUFFIX)) {
            int wanted = slot == ModifierSlot.PREFIX ? (affixes + 1) / 2 : affixes / 2;
            ModifierEligibilityProfiles.PHASE_COIL.definitions().stream()
                    .filter(definition -> definition.slot() == slot && definition.isTiered() && definition.flags().contains(ModifierFlag.CAN_ROLL))
                    .limit(wanted)
                    .forEach(definition -> modifiers.add(definition.roll(1, random)));
        }
        return new MachineTraits(affixes == 0 ? Rarity.NORMAL : Rarity.RARE, 20, List.copyOf(modifiers), List.of());
    }

    private static List<String> affixSummary(MachineTraits traits) {
        return traits.modifiers().stream().map(modifier -> modifier.affixId() + ":" + modifier.tier()).toList();
    }

    private static void line(GameTestHelper helper, Direction axis, int coils) {
        line(helper, CRADLE, axis, coils);
    }

    private static void line(GameTestHelper helper, BlockPos origin, Direction axis, int coils) {
        for (int i = 1; i <= coils; i++) {
            helper.setBlock(origin.relative(axis, i), ModBlocks.PHASE_COIL.get());
        }
        helper.setBlock(origin.relative(axis, coils + 1), ModBlocks.PHASE_EMITTER.get());
    }

    private static PhaseCradleBlockEntity cradle(GameTestHelper helper) {
        return cradle(helper, CRADLE);
    }

    private static PhaseCradleBlockEntity cradle(GameTestHelper helper, BlockPos origin) {
        helper.setBlock(origin, ModBlocks.EXOTIC_AFFIX_FORGE.get());
        return (PhaseCradleBlockEntity) helper.getBlockEntity(origin);
    }

    /** Seats a Glyph of {@code tier} in the Cradle's seat for that tier. */
    private static void seat(PhaseCradleBlockEntity cradle, int tier) {
        cradle.getSeats().setStackInSlot(tier - 1, GlyphItem.of(ModItems.GLYPH.get(), tier));
    }

    private static void assertNeverForms(GameTestHelper helper, PhaseCradleBlockEntity cradle, int coils) {
        helper.runAfterDelay(TIMEOUT - 10, () -> {
            check(!cradle.isLineFormed(), "An incomplete line should not form");
            check(cradle.lineCoilCount() == coils, "Expected " + coils + " Coils counted, found " + cradle.lineCoilCount());
            check(!helper.getBlockState(CRADLE.east(1)).getValue(PhaseCoilBlock.FORMED), "Coils should stay dark");
            helper.succeed();
        });
    }
}
