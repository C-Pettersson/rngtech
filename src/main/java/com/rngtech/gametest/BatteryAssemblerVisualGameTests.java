package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.BatteryAssemblerBlockEntity;
import com.rngtech.content.blockentity.BatteryAssemblerVisualState;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The world view of a Component Assembler: its pockets, the part on the nest, the output rack, the reservoir, and the cell. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BatteryAssemblerVisualGameTests {
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private BatteryAssemblerVisualGameTests() {
    }

    /** A bare Assembler shows empty pockets, an empty nest, rack, and reservoir, and an empty cradle. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void bareAssemblerShowsNothing(GameTestHelper helper) {
        BatteryAssemblerBlockEntity assembler = assembler(helper);
        helper.succeedWhen(() -> {
            BatteryAssemblerVisualState state = assembler.currentVisualState();
            check(state.inputs().stream().allMatch(ItemStack::isEmpty) && state.outputs().stream().allMatch(ItemStack::isEmpty),
                    "A bare Assembler shows no items: " + state.key());
            check(state.product().isEmpty() && state.fluid().isEmpty() && !state.working() && !state.doses(),
                    "A bare Assembler shows no work: " + state.key());
            check(state.cell().isEmpty() && state.charge() < 0.0F, "A bare Assembler shows no cell: " + state.key());
            HolderLookup.Provider registries = helper.getLevel().registryAccess();
            check(BatteryAssemblerVisualState.load(state.save(registries), registries).key().equals(state.key()),
                    "The visual state should survive syncing: " + state.key());
        });
    }

    /**
     * An Assembler making a Void Catalyst shows the ingot and plate in their pockets, the catalyst on the nest with a
     * dose to end the cycle while it works, Ender Slurry in the reservoir, its cell, and the catalyst in the rack.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 600)
    public static void workingAssemblerShowsItsPartsAndRack(GameTestHelper helper) {
        BatteryAssemblerBlockEntity assembler = assembler(helper);
        Item ingot = ModItems.materialItem("nullite_ingot").get();
        Item plate = ModItems.materialItem("tungstensteel_plate").get();
        assembler.getProcessInventory().setStackInSlot(BatteryAssemblerBlockEntity.SLOT_INPUT_0, new ItemStack(ingot, 2));
        assembler.getProcessInventory().setStackInSlot(BatteryAssemblerBlockEntity.SLOT_INPUT_1, new ItemStack(plate, 2));
        assembler.getGearInventory().setStackInSlot(BatteryAssemblerBlockEntity.SLOT_BATTERY_CELL,
                new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get()));
        int filled = assembler.getInputTank().fill(new FluidStack(ModFluids.ENDER_SLURRY_SOURCE.get(), 3000), IFluidHandler.FluidAction.EXECUTE);
        check(filled == 3000, "The reservoir should take Ender Slurry, took " + filled + " mB");
        MachineTestSupport.powerEachTick(helper, assembler.getEnergyStorage(null));
        boolean[] seenWork = new boolean[1];
        helper.onEachTick(() -> {
            BatteryAssemblerVisualState state = assembler.currentVisualState();
            seenWork[0] |= state.working() && state.doses() && state.craftTicks() > 0 && state.progress() > 0.0F
                    && state.product().is(ModItems.VOID_CATALYST.get())
                    && state.inputs().get(0).is(ingot) && state.inputs().get(1).is(plate) && state.inputs().get(2).isEmpty();
        });

        helper.succeedWhen(() -> {
            BatteryAssemblerVisualState state = assembler.currentVisualState();
            check(state.outputs().get(0).is(ModItems.VOID_CATALYST.get()), "Waiting for a catalyst in the rack: " + state.key());
            check(seenWork[0], "The catalyst should have stood on the nest, with a dose to come, while it worked");
            check(state.fluid().equals(RNGTech.id("ender_slurry").toString()) && state.fill() > 0.0F && state.fill() < 1.0F,
                    "The reservoir should show the Ender Slurry left: " + state.key());
            check(state.cell().equals("iron") && state.charge() >= 0.0F, "The cell should show with its meter: " + state.key());
            HolderLookup.Provider registries = helper.getLevel().registryAccess();
            check(BatteryAssemblerVisualState.load(state.save(registries), registries).key().equals(state.key()),
                    "The visual state should survive syncing: " + state.key());
        });
    }

    private static BatteryAssemblerBlockEntity assembler(GameTestHelper helper) {
        return MachineTestSupport.place(helper, POS, ModBlocks.BATTERY_ASSEMBLER.get(), BatteryAssemblerBlockEntity.class);
    }
}
