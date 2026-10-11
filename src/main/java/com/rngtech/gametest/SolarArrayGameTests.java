package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.SolarArrayControllerBlockEntity;
import com.rngtech.content.energy.SolarPanelMaterial;
import com.rngtech.content.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Solar arrays scan their layout once a second; output follows block changes within that time. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SolarArrayGameTests {
    private static final int DATA_ENERGY_PER_TICK = 2;
    /** One scan interval plus a tick for the change to land. */
    private static final int RESCAN_TICKS = 21;
    private static final BlockPos LEFT = new BlockPos(1, 1, 2);
    private static final BlockPos RIGHT = new BlockPos(3, 1, 2);

    private SolarArrayGameTests() {
    }

    /**
     * Two controllers share the middle column of a 5x3 array. Each claims half of the shared panels; removing one
     * controller hands the whole column to the other, and covering a panel removes it, each within one rescan.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200, batch = "solar_noon")
    public static void tiledControllersShareAndFollowBlockChanges(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000L);
        helper.getLevel().setWeatherParameters(6000, 0, false, false);
        for (int x = 0; x <= 4; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), ModBlocks.solarPanel(SolarPanelMaterial.CRUDE).get());
            }
        }
        SolarArrayControllerBlockEntity left = MachineTestSupport.place(helper, LEFT, ModBlocks.SOLAR_ARRAY_CONTROLLER.get(), SolarArrayControllerBlockEntity.class);
        SolarArrayControllerBlockEntity right = MachineTestSupport.place(helper, RIGHT, ModBlocks.SOLAR_ARRAY_CONTROLLER.get(), SolarArrayControllerBlockEntity.class);
        int[] shared = new int[1];
        int[] alone = new int[1];
        helper.startSequence()
                .thenIdle(RESCAN_TICKS)
                .thenExecute(() -> {
                    shared[0] = output(left);
                    RNGTech.LOGGER.info("Solar GameTest: tiled controllers output {} and {} FE/t", output(left), output(right));
                    check(shared[0] > 0, "The array generated nothing");
                    check(output(right) == shared[0], "Mirrored controllers generated " + shared[0] + " and " + output(right));
                    helper.setBlock(RIGHT, ModBlocks.solarPanel(SolarPanelMaterial.CRUDE).get());
                })
                .thenIdle(RESCAN_TICKS)
                .thenExecute(() -> {
                    alone[0] = output(left);
                    RNGTech.LOGGER.info("Solar GameTest: lone controller outputs {} FE/t", alone[0]);
                    check(alone[0] > shared[0], "Removing the other controller did not hand over the shared panels: " + alone[0]);
                    helper.setBlock(new BlockPos(0, 2, 1), Blocks.STONE);
                })
                .thenIdle(RESCAN_TICKS)
                .thenExecute(() -> check(output(left) < alone[0], "Covering a panel kept its output: " + output(left)))
                .thenSucceed();
    }

    private static int output(SolarArrayControllerBlockEntity controller) {
        return controller.getMenuData().get(DATA_ENERGY_PER_TICK);
    }
}
