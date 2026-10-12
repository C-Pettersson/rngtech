package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.MelterBlockEntity;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Automated input never jams a machine, including on the crafting remainders its ingredients leave behind. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AutomationInputGameTests {
    private static final BlockPos MELTER = new BlockPos(2, 2, 2);
    private static final int MELT_TIMEOUT = 3000;

    private AutomationInputGameTests() {
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

    private static MelterBlockEntity lubricantMelter(GameTestHelper helper, int honeyBottles) {
        MelterBlockEntity melter = MachineTestSupport.place(helper, MELTER, ModBlocks.MELTER.get(), MelterBlockEntity.class);
        melter.getGearInventory().setStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        melter.getGearInventory().setStackInSlot(MelterBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.TITANIUM).get()));
        melter.getInputTank().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        IItemHandler top = melter.getItemHandler(Direction.UP);
        insertAll(top, new ItemStack(Items.HONEY_BOTTLE, honeyBottles));
        insertAll(top, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, "plant_reagent")), 4));
        MachineTestSupport.powerEachTick(helper, melter.getEnergyStorage(null));
        return melter;
    }

    private static void insertAll(IItemHandler handler, ItemStack stack) {
        ItemStack rest = stack;
        for (int slot = 0; slot < handler.getSlots() && !rest.isEmpty(); slot++) {
            rest = handler.insertItem(slot, rest, false);
        }
        check(rest.isEmpty(), "The top face rejected " + stack);
    }

    private static void assertInputStaysOpen(MelterBlockEntity melter) {
        for (int slot = MelterBlockEntity.SLOT_PRIMARY_INPUT; slot <= MelterBlockEntity.SLOT_SECONDARY_INPUT; slot++) {
            ItemStack stack = melter.getProcessInventory().getStackInSlot(slot);
            check(!stack.is(Items.GLASS_BOTTLE), "A Glass Bottle stayed in input slot " + slot);
        }
        IItemHandler top = melter.getItemHandler(Direction.UP);
        ItemStack rest = new ItemStack(Items.HONEY_BOTTLE);
        for (int slot = 0; slot < top.getSlots() && !rest.isEmpty(); slot++) {
            rest = top.insertItem(slot, rest, true);
        }
        check(rest.isEmpty(), "The top face rejected another Honey Bottle");
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
}
