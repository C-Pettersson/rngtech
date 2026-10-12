package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AlloyFurnaceBlockEntity;
import com.rngtech.content.blockentity.BatteryAssemblerBlockEntity;
import com.rngtech.content.blockentity.MelterBlockEntity;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.recipe.MelterRecipe;
import com.rngtech.content.recipe.MelterRecipeInput;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Arrays;

/**
 * Automation fills input slots without letting one ingredient take the slots another one needs, and the crafting
 * remainders ingredients leave behind never stay in an input slot.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AutomationInputGameTests {
    private static final BlockPos MELTER = new BlockPos(2, 2, 2);
    private static final int MELT_TIMEOUT = 3000;

    private AutomationInputGameTests() {
    }

    /** Whichever Melter ingredient arrives first, a flood of it leaves the other slot free for its partner. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void melterKeepsASlotForTheOtherIngredient(GameTestHelper helper) {
        MelterBlockEntity sugarFirst = MachineTestSupport.place(helper, new BlockPos(1, 1, 2), ModBlocks.MELTER.get(), MelterBlockEntity.class);
        MelterBlockEntity redstoneFirst = MachineTestSupport.place(helper, new BlockPos(3, 1, 2), ModBlocks.MELTER.get(), MelterBlockEntity.class);

        flood(sugarFirst.getItemHandler(Direction.UP), new ItemStack(Items.SUGAR, 64), 3);
        check(slots(sugarFirst.getItemHandler(Direction.UP)).equals("[64 sugar, 0 air]"),
                "Sugar took both slots: " + slots(sugarFirst.getItemHandler(Direction.UP)));
        flood(sugarFirst.getItemHandler(Direction.UP), new ItemStack(Items.REDSTONE, 16), 1);
        check(sugarFirst.getProcessInventory().getStackInSlot(MelterBlockEntity.SLOT_SECONDARY_INPUT).is(Items.REDSTONE),
                "Redstone could not join the sugar: " + slots(sugarFirst.getItemHandler(Direction.UP)));

        flood(redstoneFirst.getItemHandler(Direction.UP), new ItemStack(Items.REDSTONE, 64), 3);
        check(slots(redstoneFirst.getItemHandler(Direction.UP)).equals("[64 redstone, 0 air]"),
                "Redstone took both slots: " + slots(redstoneFirst.getItemHandler(Direction.UP)));
        flood(redstoneFirst.getItemHandler(Direction.UP), new ItemStack(Items.SUGAR, 16), 1);
        check(redstoneFirst.getProcessInventory().getStackInSlot(MelterBlockEntity.SLOT_SECONDARY_INPUT).is(Items.SUGAR),
                "Sugar could not join the redstone: " + slots(redstoneFirst.getItemHandler(Direction.UP)));
        helper.succeed();
    }

    /** Ender Pearls take one slot, since Pearl and Pearl melts from it, and leave the other for Collapse Residue. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void melterKeepsASlotForCollapseResidue(GameTestHelper helper) {
        MelterBlockEntity melter = MachineTestSupport.place(helper, new BlockPos(2, 1, 2), ModBlocks.MELTER.get(), MelterBlockEntity.class);
        IItemHandler top = melter.getItemHandler(Direction.UP);

        flood(top, new ItemStack(Items.ENDER_PEARL, 16), 4);
        check(slots(top).equals("[16 ender_pearl, 0 air]"), "Ender Pearls took the residue's slot: " + slots(top));
        flood(top, item("rngtech:collapse_residue", 8), 1);
        check(slots(top).equals("[16 ender_pearl, 8 collapse_residue]"), "Collapse Residue could not join the pearls: " + slots(top));
        helper.succeed();
    }

    /** Two items from the same ingredient tag cannot fill both slots, since the recipe still needs its other item. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void melterRejectsASecondItemForTheSameIngredient(GameTestHelper helper) {
        MelterBlockEntity melter = MachineTestSupport.place(helper, new BlockPos(2, 1, 2), ModBlocks.MELTER.get(), MelterBlockEntity.class);
        IItemHandler top = melter.getItemHandler(Direction.UP);

        check(top.insertItem(0, item("rngtech:resin", 4), false).isEmpty(), "Resin was rejected from an empty Melter");
        check(top.insertItem(1, new ItemStack(Items.SLIME_BALL, 4), true).getCount() == 4,
                "A second lubricant base took the additive's slot");
        check(top.insertItem(1, item("rngtech:organic_reagent", 4), true).isEmpty(),
                "The lubricant additive was rejected");
        helper.succeed();
    }

    /** A recipe that takes two of an item melts from one slot, so the item never needs the second slot. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void melterRecipesMatchShapeless(GameTestHelper helper) {
        MelterRecipe pearls = melterRecipe(Ingredient.of(Items.ENDER_PEARL), Ingredient.of(Items.ENDER_PEARL));
        FluidStack water = new FluidStack(Fluids.WATER, 1000);

        check(Arrays.equals(pearls.consumed(new MelterRecipeInput(new ItemStack(Items.ENDER_PEARL, 2), ItemStack.EMPTY, water), 1), new int[] {2, 0}),
                "Two pearls in one slot did not match a pearl and pearl recipe");
        check(pearls.consumed(new MelterRecipeInput(new ItemStack(Items.ENDER_PEARL, 1), ItemStack.EMPTY, water), 1) == null,
                "One pearl matched a recipe that needs two");
        check(Arrays.equals(pearls.consumed(new MelterRecipeInput(new ItemStack(Items.ENDER_PEARL, 3), new ItemStack(Items.ENDER_PEARL, 3), water), 3), new int[] {3, 3}),
                "Three batched melts could not draw from both slots");

        MelterRecipe mixed = melterRecipe(Ingredient.of(Items.SUGAR), Ingredient.of(Items.REDSTONE));
        check(mixed.matches(new MelterRecipeInput(new ItemStack(Items.REDSTONE), new ItemStack(Items.SUGAR), water), helper.getLevel()),
                "Swapped ingredients did not match");
        check(!mixed.matches(new MelterRecipeInput(new ItemStack(Items.SUGAR, 2), ItemStack.EMPTY, water), helper.getLevel()),
                "Sugar alone matched a sugar and redstone recipe");
        helper.succeed();
    }

    /** An Alloy Furnace keeps an empty slot for every ingredient a recipe still needs, in any arrival order. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void alloyFurnaceKeepsSlotsForMissingIngredients(GameTestHelper helper) {
        AlloyFurnaceBlockEntity furnace = MachineTestSupport.place(helper, new BlockPos(2, 1, 2),
                ModBlocks.alloyFurnaceChassis(AlloyFurnaceChassisMaterial.BRONZE).get(), AlloyFurnaceBlockEntity.class);
        furnace.getGearInventory().setStackInSlot(AlloyFurnaceBlockEntity.SLOT_CRUCIBLE,
                new ItemStack(ModItems.alloyCrucible(AlloyCrucibleMaterial.BRONZE).get()));
        IItemHandler top = furnace.getItemHandler(Direction.UP);
        check(top.getSlots() == 3, "Expected a Bronze Alloy Crucible to open 3 input slots, got " + top.getSlots());

        flood(top, new ItemStack(Items.COPPER_INGOT, 64), 4);
        check(slots(top).equals("[64 copper_ingot, 0 air, 0 air]"), "Copper took a slot the other ingredients need: " + slots(top));
        flood(top, item("rngtech:tin_ingot", 16), 1);
        flood(top, new ItemStack(Items.CHARCOAL, 16), 1);
        check(slots(top).equals("[64 copper_ingot, 16 tin_ingot, 16 charcoal]"), "Bronze ingredients did not all fit: " + slots(top));

        furnace.getProcessInventory().setStackInSlot(0, ItemStack.EMPTY);
        furnace.getProcessInventory().setStackInSlot(1, ItemStack.EMPTY);
        furnace.getProcessInventory().setStackInSlot(2, ItemStack.EMPTY);
        flood(top, new ItemStack(Items.CHARCOAL, 64), 4);
        check(slots(top).equals("[64 charcoal, 0 air, 0 air]"), "Charcoal took a slot the other ingredients need: " + slots(top));
        helper.succeed();
    }

    /** Assembler slots only take what a recipe that fits the other slots needs there. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void assemblerSlotsFollowOneRecipe(GameTestHelper helper) {
        BatteryAssemblerBlockEntity assembler = MachineTestSupport.place(helper, new BlockPos(2, 1, 2),
                ModBlocks.BATTERY_ASSEMBLER.get(), BatteryAssemblerBlockEntity.class);
        IItemHandler top = assembler.getItemHandler(Direction.UP);

        check(top.insertItem(0, new ItemStack(Items.REDSTONE, 64), false).isEmpty(), "A stack of redstone did not fit the first slot at once");
        check(top.insertItem(1, item("rngtech:iron_plate", 1), true).getCount() == 1,
                "A Battery Cell's plate joined a Servo's redstone");
        check(top.insertItem(1, item("rngtech:titanium_ingot", 2), true).isEmpty(),
                "The Servo's ingots were rejected after its redstone");
        helper.succeed();
    }

    /** A recipe that needs the same item in two positions fills both evenly, so it can start before one slot fills. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void assemblerFillsRepeatedPositionsEvenly(GameTestHelper helper) {
        BatteryAssemblerBlockEntity assembler = MachineTestSupport.place(helper, new BlockPos(2, 1, 2),
                ModBlocks.BATTERY_ASSEMBLER.get(), BatteryAssemblerBlockEntity.class);
        IItemHandler top = assembler.getItemHandler(Direction.UP);

        for (int arrival = 0; arrival < 40; arrival++) {
            flood(top, item("rngtech:aluminum_plate", 1), 1);
        }
        int first = assembler.getProcessInventory().getStackInSlot(BatteryAssemblerBlockEntity.SLOT_INPUT_0).getCount();
        int second = assembler.getProcessInventory().getStackInSlot(BatteryAssemblerBlockEntity.SLOT_INPUT_1).getCount();
        check(first + second == 40 && Math.abs(first - second) <= 1, "Anode plates filled unevenly: " + slots(top));
        check(assembler.getProcessInventory().getStackInSlot(BatteryAssemblerBlockEntity.SLOT_INPUT_2).isEmpty()
                && assembler.getProcessInventory().getStackInSlot(BatteryAssemblerBlockEntity.SLOT_INPUT_3).isEmpty(),
                "Anode plates spilled into positions no recipe pairs with them: " + slots(top));
        helper.succeed();
    }

    /** Glass Bottles left by Honey Bottles go into the hopper below, and the top face keeps taking Honey Bottles. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = MELT_TIMEOUT)
    public static void melterPassesRemaindersToTheInventoryBelow(GameTestHelper helper) {
        BlockPos hopperPos = MELTER.below();
        helper.setBlock(hopperPos, Blocks.HOPPER);
        MelterBlockEntity melter = lubricantMelter(helper, 2);
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getBlockEntity(hopperPos);
        helper.startSequence()
                .thenWaitUntil(() -> check(glassBottles(hopper) == 2,
                        "Expected 2 Glass Bottles in the hopper, found " + glassBottles(hopper) + "; " + MachineTestSupport.describe(melter)))
                .thenExecute(() -> assertInputStaysOpen(melter))
                .thenSucceed();
    }

    /** With no inventory below, the Glass Bottle drops instead of sitting in the input slot. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = MELT_TIMEOUT)
    public static void melterDropsRemaindersWithNoInventoryBelow(GameTestHelper helper) {
        MelterBlockEntity melter = lubricantMelter(helper, 1);
        helper.startSequence()
                .thenWaitUntil(() -> check(melter.getOutputTank().getFluidAmount() >= 1000,
                        "The Honey Bottle never melted; " + MachineTestSupport.describe(melter)))
                .thenExecute(() -> {
                    helper.assertItemEntityPresent(Items.GLASS_BOTTLE, MELTER, 3.0D);
                    assertInputStaysOpen(melter);
                })
                .thenSucceed();
    }

    /** Offers a stack to every slot in order, the way hoppers and Universal Cables do, {@code rounds} times. */
    private static void flood(IItemHandler handler, ItemStack stack, int rounds) {
        for (int round = 0; round < rounds; round++) {
            ItemStack remaining = stack.copy();
            for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
                remaining = handler.insertItem(slot, remaining, false);
            }
        }
    }

    private static String slots(IItemHandler handler) {
        StringBuilder text = new StringBuilder("[");
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            text.append(slot == 0 ? "" : ", ").append(stack.getCount()).append(' ')
                    .append(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        }
        return text.append(']').toString();
    }

    private static ItemStack item(String id, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)), count);
    }

    private static MelterBlockEntity lubricantMelter(GameTestHelper helper, int honeyBottles) {
        MelterBlockEntity melter = MachineTestSupport.place(helper, MELTER, ModBlocks.MELTER.get(), MelterBlockEntity.class);
        melter.getGearInventory().setStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        melter.getGearInventory().setStackInSlot(MelterBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.TITANIUM).get()));
        melter.getInputTank().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        IItemHandler top = melter.getItemHandler(Direction.UP);
        check(accepts(top, new ItemStack(Items.HONEY_BOTTLE, honeyBottles), false), "The top face rejected Honey Bottles");
        check(accepts(top, item("rngtech:plant_reagent", 4), false), "The top face rejected Plant Reagent");
        MachineTestSupport.powerEachTick(helper, melter.getEnergyStorage(null));
        return melter;
    }

    private static boolean accepts(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack rest = stack;
        for (int slot = 0; slot < handler.getSlots() && !rest.isEmpty(); slot++) {
            rest = handler.insertItem(slot, rest, simulate);
        }
        return rest.isEmpty();
    }

    private static void assertInputStaysOpen(MelterBlockEntity melter) {
        for (int slot = MelterBlockEntity.SLOT_PRIMARY_INPUT; slot <= MelterBlockEntity.SLOT_SECONDARY_INPUT; slot++) {
            check(!melter.getProcessInventory().getStackInSlot(slot).is(Items.GLASS_BOTTLE), "A Glass Bottle stayed in input slot " + slot);
        }
        check(accepts(melter.getItemHandler(Direction.UP), new ItemStack(Items.HONEY_BOTTLE), true), "The top face rejected another Honey Bottle");
    }

    private static int glassBottles(HopperBlockEntity hopper) {
        int count = 0;
        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
            ItemStack stack = hopper.getItem(slot);
            if (stack.is(Items.GLASS_BOTTLE)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static MelterRecipe melterRecipe(Ingredient primary, Ingredient secondary) {
        return new MelterRecipe("", primary, secondary, SizedFluidIngredient.of(Fluids.WATER, 1000),
                new FluidStack(ModFluids.LUBRICANT_SOURCE.get(), 1000), 100, 100, 0, 0, false, 0, 0);
    }
}
