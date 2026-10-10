package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.MachineInfoSnapshot;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineStat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Craft-cycle regressions: top-ups keep a craft, swaps restart it, and per-craft chances roll once. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CraftCycleGameTests {
    private static final int FED_CRAFTS = 3;
    private static final int TOP_UP_PROGRESS = 20;
    private static final int SWAP_PROGRESS = 20;
    private static final int UNPOWERED_WAIT_TICKS = 200;

    private CraftCycleGameTests() {
    }

    /** A hopper tops up the input mid-craft; each craft must run its full length and Bulk Speed must climb. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 3000)
    public static void hopperFedMetalPressKeepsItsCycle(GameTestHelper helper) {
        BlockPos pressPos = new BlockPos(2, 1, 2);
        MetalPressBlockEntity press = MachineTestSupport.place(helper, pressPos, ModBlocks.CRUDE_METAL_PRESS.get(), MetalPressBlockEntity.class);
        press.setMachineTraits(MachineTestSupport.BULK_SPEED);
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.STEEL).get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_MOLD, new ItemStack(ModItems.PLATE_MOLD.get()));
        BlockPos hopperPos = pressPos.above();
        helper.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos);
        MachineTestSupport.powerEachTick(helper, press.getEnergyStorage(null));

        double baseSpeed = press.effectiveStats().value(MachineStat.PROCESSING_SPEED);
        MachineTestSupport.CraftLog log = new MachineTestSupport.CraftLog(helper, press,
                () -> press.getProcessInventory().getStackInSlot(MetalPressBlockEntity.SLOT_OUTPUT).getCount());
        helper.onEachTick(() -> {
            // Refill the hopper only mid-craft, so its arrivals top up a running craft.
            int input = press.getProcessInventory().getStackInSlot(MetalPressBlockEntity.SLOT_INPUT).getCount();
            int queued = input + hopper.getItem(0).getCount();
            if (queued < 3 && (input == 0 || press.machineInfo().progress() >= TOP_UP_PROGRESS)) {
                hopper.setItem(0, new ItemStack(Items.IRON_INGOT, hopper.getItem(0).getCount() + 1));
            }
            log.sample(press.effectiveStats().value(MachineStat.PROCESSING_SPEED));
        });
        helper.succeedWhen(() -> {
            List<MachineTestSupport.Craft> crafts = log.crafts();
            check(crafts.size() >= FED_CRAFTS, "Completed " + crafts.size() + " of " + FED_CRAFTS + " crafts; " + MachineTestSupport.describe(press));
            double previousSpeed = baseSpeed;
            for (MachineTestSupport.Craft craft : crafts) {
                check(craft.ticks() == craft.expectedTicks(), "A craft took " + craft.ticks() + " ticks, expected " + craft.expectedTicks());
                check(craft.speedAfter() > previousSpeed, "Bulk Speed did not rise: " + previousSpeed + " -> " + craft.speedAfter());
                previousSpeed = craft.speedAfter();
            }
            check(crafts.getLast().expectedTicks() < crafts.getFirst().expectedTicks(), "Bulk Speed did not shorten the crafts");
        });
    }

    /** Topping up keeps the craft; a different input or Crush Head restarts it and resets Bulk Speed. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 1200)
    public static void crusherSwapRestartsTheCraft(GameTestHelper helper) {
        CrusherBlockEntity crusher = bulkSpeedCrusher(helper, new BlockPos(1, 1, 2), CrushHeadMaterial.IRON);
        CrusherBlockEntity twin = bulkSpeedCrusher(helper, new BlockPos(3, 1, 2), CrushHeadMaterial.IRON);
        MachineTestSupport.powerEachTick(helper, crusher.getEnergyStorage(null));
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.RAW_IRON, 2));
        int[] marker = new int[1];

        helper.startSequence()
                .thenWaitUntil(() -> check(outputCount(crusher) > 0, "Waiting for the first craft"))
                .thenWaitUntil(() -> check(progress(crusher) >= SWAP_PROGRESS, "Waiting for the second craft"))
                .thenExecute(() -> {
                    check(speed(crusher) > speed(twin), "Bulk Speed did not build up");
                    marker[0] = progress(crusher);
                    crusher.getItemHandler(Direction.UP).insertItem(0, new ItemStack(Items.RAW_IRON), false);
                })
                .thenExecuteAfter(2, () -> {
                    check(progress(crusher) > marker[0], "Topping up the input restarted the craft");
                    check(speed(crusher) > speed(twin), "Topping up the input reset Bulk Speed");
                    marker[0] = progress(crusher);
                    crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.IRON_ORE, 2));
                })
                .thenExecuteAfter(2, () -> {
                    check(progress(crusher) <= 2, "Swapping the input kept progress " + progress(crusher) + " from " + marker[0]);
                    check(speed(crusher) == speed(twin), "Swapping the input kept Bulk Speed");
                })
                .thenWaitUntil(() -> check(progress(crusher) >= SWAP_PROGRESS, "Waiting for progress on the new input"))
                .thenExecute(() -> {
                    marker[0] = progress(crusher);
                    crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.COPPER).get()));
                })
                .thenExecuteAfter(2, () -> check(progress(crusher) <= 2, "Swapping the Crush Head kept progress " + progress(crusher) + " from " + marker[0]))
                .thenSucceed();
    }

    /** An unpowered Crusher on an under-level recipe waits without rolling its jam chance. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = UNPOWERED_WAIT_TICKS + 100)
    public static void unpoweredCrusherNeverJams(GameTestHelper helper) {
        CrusherBlockEntity crusher = MachineTestSupport.place(helper, new BlockPos(2, 1, 2),
                ModBlocks.crusherChassis(CrusherChassisMaterial.IRON).get(), CrusherBlockEntity.class);
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.FLINT).get()));
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.IRON_ORE, 4));
        helper.onEachTick(() -> check(crusher.machineInfo().blockedReason() != MachineInfoSnapshot.BlockedReason.JAMMED,
                "Jammed at tick " + helper.getTick() + " while waiting for power"));
        helper.runAtTickTime(UNPOWERED_WAIT_TICKS, () -> {
            check(crusher.machineInfo().blockedReason() == MachineInfoSnapshot.BlockedReason.NO_POWER, "Expected the Crusher to wait for power");
            helper.succeed();
        });
    }

    private static CrusherBlockEntity bulkSpeedCrusher(GameTestHelper helper, BlockPos pos, CrushHeadMaterial head) {
        CrusherBlockEntity crusher = MachineTestSupport.place(helper, pos, ModBlocks.crusherChassis(CrusherChassisMaterial.COPPER).get(), CrusherBlockEntity.class);
        crusher.setMachineTraits(MachineTestSupport.BULK_SPEED);
        ItemStack headStack = new ItemStack(ModItems.crushHead(head).get());
        check(crusher.canInstallCrushHead(headStack), "Crusher rejects the " + head + " Crush Head");
        crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, headStack);
        return crusher;
    }

    private static int progress(CrusherBlockEntity crusher) {
        return crusher.machineInfo().progress();
    }

    private static double speed(CrusherBlockEntity crusher) {
        return crusher.effectiveStats().value(MachineStat.PROCESSING_SPEED);
    }

    private static int outputCount(CrusherBlockEntity crusher) {
        return crusher.getInventory().getStackInSlot(CrusherBlockEntity.SLOT_OUTPUT).getCount();
    }
}
