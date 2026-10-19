package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.MelterBlockEntity;
import com.rngtech.content.blockentity.MelterVisualState;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.FluidPumpMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The world view of a Melter: its Gear, its ingredients and tanks, its heat, and the melt in its crucible. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MelterVisualGameTests {
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private MelterVisualGameTests() {
    }

    /** A bare Melter shows bare shafts, empty rails, cradle, and pad, empty columns, and a cold crucible. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void bareMelterShowsNoGear(GameTestHelper helper) {
        MelterBlockEntity melter = melter(helper);
        helper.succeedWhen(() -> {
            MelterVisualState state = melter.currentVisualState();
            check(state.crushHead().isEmpty() && state.heatCore().isEmpty() && state.pump().isEmpty() && state.servo().isEmpty(),
                    "A bare Melter shows no Gear: " + state.key());
            check(state.cell().isEmpty() && state.charge() < 0.0F, "A bare Melter shows no cell: " + state.key());
            check(state.inputFluid().isEmpty() && state.outputFluid().isEmpty() && state.product().isEmpty(),
                    "A bare Melter shows no fluid: " + state.key());
            check(!state.working() && state.progress() == 0.0F && state.target() == 0, "A bare Melter is idle and cold: " + state.key());
            check(MelterVisualState.load(state.save()).key().equals(state.key()), "The visual state should survive syncing: " + state.key());
        });
    }

    /**
     * A Melter making lava shows its rolls, fins, pump, servo, and cell, the stone and blaze powder on the rolls, water in
     * the input column, a melt of lava rising in the crucible at its melting point, and lava in the output column once a
     * melt finishes.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 2400)
    public static void meltingMelterShowsItsMelt(GameTestHelper helper) {
        MelterBlockEntity melter = melter(helper);
        melter.getGearInventory().setStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        melter.getGearInventory().setStackInSlot(MelterBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.TITANIUM).get()));
        melter.getGearInventory().setStackInSlot(MelterBlockEntity.SLOT_FLUID_PUMP, new ItemStack(ModItems.fluidPump(FluidPumpMaterial.OSMIUM).get()));
        melter.getGearInventory().setStackInSlot(MelterBlockEntity.SLOT_SERVO, new ItemStack(ModItems.STEEL_SERVO.get()));
        melter.getGearInventory().setStackInSlot(MelterBlockEntity.SLOT_BATTERY_CELL, new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get()));
        melter.getProcessInventory().setStackInSlot(MelterBlockEntity.SLOT_PRIMARY_INPUT, new ItemStack(Items.STONE, 4));
        melter.getProcessInventory().setStackInSlot(MelterBlockEntity.SLOT_SECONDARY_INPUT, new ItemStack(Items.BLAZE_POWDER, 4));
        int filled = melter.getInputTank().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        check(filled == 4000, "The input tank should take water, took " + filled + " mB");
        MachineTestSupport.powerEachTick(helper, melter.getEnergyStorage(null));
        boolean[] seenMelt = new boolean[1];
        helper.onEachTick(() -> {
            MelterVisualState state = melter.currentVisualState();
            seenMelt[0] |= state.working() && state.progress() > 0.0F && state.cycleTicks() > 0
                    && state.product().equals("minecraft:lava") && state.heat() >= state.target();
        });

        helper.succeedWhen(() -> {
            MelterVisualState state = melter.currentVisualState();
            check(state.outputFluid().equals("minecraft:lava") && state.outputFill() > 0.0F, "Waiting for lava in the output column: " + state.key());
            check(seenMelt[0], "A melt of lava should have risen in the crucible while it worked");
            check(state.crushHead().equals("titanium") && state.heatCore().equals("titanium") && state.pump().equals("osmium")
                    && state.servo().equals("steel"), "Wrong parts shown: " + state.key());
            check(state.cell().equals("iron") && state.charge() >= 0.0F, "The cell should show with its meter: " + state.key());
            check(state.primary() == Items.STONE && state.secondary() == Items.BLAZE_POWDER, "The ingredients should lie on the rolls: " + state.key());
            check(state.inputFluid().equals("minecraft:water") && state.inputFill() > 0.0F && state.inputFill() < 1.0F,
                    "The input column should show the water left: " + state.key());
            check(state.target() > 0 && state.maxHeat() >= state.target(), "The bar should read the melting point under the core's limit: " + state.key());
            check(MelterVisualState.load(state.save()).key().equals(state.key()), "The visual state should survive syncing: " + state.key());
        });
    }

    private static MelterBlockEntity melter(GameTestHelper helper) {
        return MachineTestSupport.place(helper, POS, ModBlocks.MELTER.get(), MelterBlockEntity.class);
    }
}
