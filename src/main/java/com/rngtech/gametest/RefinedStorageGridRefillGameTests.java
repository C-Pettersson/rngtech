package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.calibration.CalibrationFamily;
import com.rngtech.content.calibration.CalibrationState;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A Refined Storage crafting grid refills a calibrated slot from any stored calibration the recipe still accepts, not
 * only from the exact Stability it just used.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RefinedStorageGridRefillGameTests {
    private static final int KINETIC_SLOT = 4;

    private RefinedStorageGridRefillGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void gridRefillsWithClosestValidCalibration(GameTestHelper helper) throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("refinedstorage")) {
            helper.succeed();
            return;
        }
        RefinedStorageGridRefillSupport grid = exoticCrushHeadGrid();
        grid.store(kinetic(80));
        grid.store(kinetic(90));
        grid.store(kinetic(99));

        grid.craft(player(helper), crushHead());

        check(stability(grid.matrix(KINETIC_SLOT)) == 90, "The grid did not refill with the closest valid calibration");
        check(grid.stored(kinetic(90)) == 0, "The refill did not take the component from storage");
        check(grid.stored(kinetic(80)) == 1 && grid.stored(kinetic(99)) == 1, "The refill took other calibrations");
        helper.succeed();
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void gridSkipsCalibrationTheRecipeRejects(GameTestHelper helper) throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("refinedstorage")) {
            helper.succeed();
            return;
        }
        RefinedStorageGridRefillSupport grid = exoticCrushHeadGrid();
        grid.store(kinetic(80));

        grid.craft(player(helper), crushHead());

        check(grid.matrix(KINETIC_SLOT).isEmpty(), "The grid refilled with a calibration below the recipe's Stability");
        check(grid.stored(kinetic(80)) == 1, "The grid took a calibration the recipe rejects");
        helper.succeed();
    }

    private static RefinedStorageGridRefillSupport exoticCrushHeadGrid() {
        RefinedStorageGridRefillSupport grid = new RefinedStorageGridRefillSupport();
        for (int slot = 0; slot < 3; slot++) {
            grid.setMatrix(slot, item("naquadah_plate", 2));
        }
        grid.setMatrix(3, item("tungstensteel_plate", 2));
        grid.setMatrix(KINETIC_SLOT, kinetic(94));
        grid.setMatrix(5, item("tungstensteel_plate", 2));
        grid.setMatrix(7, new ItemStack(ModItems.ASCENSION_MATRIX.get(), 2));
        return grid;
    }

    private static ItemStack kinetic(int stability) {
        ItemStack component = new ItemStack(ModItems.CALIBRATED_COMPONENTS.get(CalibrationFamily.KINETIC).get());
        component.set(ModDataComponents.CALIBRATION_STATE.get(), new CalibrationState(CalibrationFamily.KINETIC, 7, stability, 0));
        return component;
    }

    private static int stability(ItemStack stack) {
        CalibrationState state = stack.get(ModDataComponents.CALIBRATION_STATE.get());
        return state == null ? -1 : state.stability();
    }

    private static ItemStack item(String path, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(RNGTech.id(path)), count);
    }

    private static ItemStack crushHead() {
        return item("exotic_crush_head", 1);
    }

    private static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }
}
