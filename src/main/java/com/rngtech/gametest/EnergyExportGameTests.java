package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.DebugBatteryBlockEntity;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Energy exports keep their neighbour lookups until a neighbour changes, and see a change on the next tick. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnergyExportGameTests {
    private static final int STEADY_TICKS = 100;

    private EnergyExportGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = STEADY_TICKS + 100)
    public static void exportReusesNeighbourLookupsUntilANeighbourChanges(GameTestHelper helper) {
        BlockPos batteryPos = new BlockPos(2, 1, 2);
        DebugBatteryBlockEntity battery = MachineTestSupport.place(helper, batteryPos, ModBlocks.DEBUG_BATTERY.get(), DebugBatteryBlockEntity.class);
        CrusherBlockEntity east = crusher(helper, batteryPos.east());
        int[] lookups = new int[1];
        CrusherBlockEntity[] west = new CrusherBlockEntity[1];
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    check(east.machineInfo().energy() > 0, "The Debug Battery did not power its neighbour");
                    lookups[0] = battery.exportTargetLookups();
                })
                .thenExecuteFor(STEADY_TICKS, () -> check(battery.exportTargetLookups() == lookups[0],
                        "Looked up neighbours again at tick " + helper.getTick() + " with nothing changed"))
                .thenExecute(() -> west[0] = crusher(helper, batteryPos.west()))
                .thenExecuteAfter(2, () -> {
                    check(west[0].machineInfo().energy() > 0, "A newly placed neighbour got no power");
                    check(battery.exportTargetLookups() > lookups[0], "Placing a neighbour did not refresh its lookup");
                    helper.setBlock(batteryPos.west(), Blocks.AIR);
                })
                .thenExecuteAfter(2, () -> check(east.machineInfo().energy() > 0, "The remaining neighbour lost power"))
                .thenSucceed();
    }

    private static CrusherBlockEntity crusher(GameTestHelper helper, BlockPos pos) {
        return MachineTestSupport.place(helper, pos, ModBlocks.crusherChassis(CrusherChassisMaterial.COPPER).get(), CrusherBlockEntity.class);
    }
}
