package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The blocked flag in a machine's block state, which the client draws as a warning sign on the front. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineStatusGameTests {
    private static final BlockPos BLOCKED = new BlockPos(1, 1, 2);
    private static final BlockPos IDLE = new BlockPos(3, 1, 2);
    /** Two status checks, so each Crusher has been checked at least once whatever its stagger. */
    private static final int SETTLE_TICKS = 41;

    private MachineStatusGameTests() {
    }

    /** A Crusher without a Crush Head is blocked and an empty one is idle; installing the head clears the flag. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void blockedFlagFollowsTheWorkState(GameTestHelper helper) {
        CrusherBlockEntity blocked = crusher(helper, BLOCKED);
        CrusherBlockEntity idle = crusher(helper, IDLE);
        idle.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get()));
        helper.startSequence()
                .thenExecuteAfter(SETTLE_TICKS, () -> {
                    check(isBlocked(helper, BLOCKED), "A Crusher without a Crush Head should show blocked: " + MachineTestSupport.describe(blocked));
                    check(!isBlocked(helper, IDLE), "An empty Crusher is idle and should not show blocked: " + MachineTestSupport.describe(idle));
                    blocked.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD,
                            new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get()));
                })
                .thenExecuteAfter(SETTLE_TICKS, () -> check(!isBlocked(helper, BLOCKED),
                        "Installing the Crush Head should clear the blocked flag: " + MachineTestSupport.describe(blocked)))
                .thenSucceed();
    }

    private static boolean isBlocked(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(BaseMachineBlock.BLOCKED);
    }

    private static CrusherBlockEntity crusher(GameTestHelper helper, BlockPos pos) {
        return MachineTestSupport.place(helper, pos, ModBlocks.crusherChassis(CrusherChassisMaterial.IRON).get(), CrusherBlockEntity.class);
    }
}
