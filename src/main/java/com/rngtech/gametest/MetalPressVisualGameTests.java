package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.InertAtmosphere;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.blockentity.MetalPressVisualState;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.progression.MachineProgressionState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** The world view of the Metal Press: the die, the stored Molds, the Gear, the heat, and the presses reach the synced visual state. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MetalPressVisualGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private MetalPressVisualGameTests() {
    }

    /**
     * A working Steel press shows the selected Mold on the bed and the others in the rack, its Heat Core, Servo, and cell,
     * the ingot on the die, its heat against the target, a strain meter, and its plates in the tray once it has pressed.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 1200)
    public static void steelPressShowsItsDieRackGearAndPresses(GameTestHelper helper) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, MACHINE, ModBlocks.METAL_PRESS.get(), MetalPressBlockEntity.class);
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_SERVO, new ItemStack(ModItems.STEEL_SERVO.get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_MOLD, new ItemStack(ModItems.PLATE_MOLD.get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_MOLD + 2, new ItemStack(ModItems.GEAR_MOLD.get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_MOLD + 4, new ItemStack(ModItems.CIRCUIT_MOLD.get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_BATTERY_CELL, new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get()));
        press.getProcessInventory().setStackInSlot(MetalPressBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT, 16));
        MachineTestSupport.powerEachTick(helper, press.getEnergyStorage(null));

        helper.succeedWhen(() -> {
            MetalPressVisualState state = press.currentVisualState();
            check(state.die().equals("plate"), "The Plate Mold should be on the bed: " + state.key());
            check(state.rack(0).isEmpty() && state.rack(2).equals("gear") && state.rack(4).equals("circuit"),
                    "The rack should hold the stored Molds but not the selected one: " + state.key());
            check(state.heatCore().equals("titanium") && state.servo().equals("steel") && state.cell().equals("iron"),
                    "Wrong parts shown: " + state.key());
            check(state.charge() >= 0.0F, "A cell should light the charge meter: " + state.key());
            check(state.input() == Items.IRON_INGOT, "The die should show the ingot: " + state.key());
            check(state.target() > 0 && state.maxHeat() >= state.target() && state.heat() > 400,
                    "The bar should show a warm press under the core's limit: " + state.key());
            check(state.strain() >= 0.0F, "The Steel press should show its strain meter: " + state.key());
            check(state.gas() == 0.0F && !state.hydrogen() && !state.atmosphere(), "An empty tank should show empty and unlit: " + state.key());
            check(state.presses() > 0 && state.output() == item("iron_plate") && state.outputFill() > 0.0F,
                    "Pressed plates should count as presses and lie in the tray: " + state.key());
            check(MetalPressVisualState.load(state.save()).key().equals(state.key()), "The visual state should survive syncing: " + state.key());
        });
    }

    /** A bare Crude press shows no Gear, no cell, and no strain meter, because it never fails. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void bareCrudePressShowsNoGear(GameTestHelper helper) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, MACHINE, ModBlocks.CRUDE_METAL_PRESS.get(), MetalPressBlockEntity.class);
        helper.succeedWhen(() -> {
            MetalPressVisualState state = press.currentVisualState();
            check(state.die().isEmpty() && state.heatCore().isEmpty() && state.servo().isEmpty() && state.cell().isEmpty(),
                    "A bare press should show no Gear: " + state.key());
            check(state.charge() < 0.0F && state.strain() < 0.0F, "A bare Crude press has no charge or strain to show: " + state.key());
            check(state.maxHeat() == 0 && state.target() == 0 && !state.working(), "A bare press should be cold: " + state.key());
            check(MetalPressVisualState.load(state.save()).key().equals(state.key()), "The visual state should survive syncing: " + state.key());
        });
    }

    /** A Hydrogen Quench press with Hydrogen in its tank shows the tank in Hydrogen's colour. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void annealingPressShowsItsHydrogen(GameTestHelper helper) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, MACHINE, ModBlocks.METAL_PRESS.get(), MetalPressBlockEntity.class);
        press.setMachineProgression(new MachineProgressionState(
                MachineProgressionState.xpForLevel(MachineProgressionState.MAX_LEVEL), 0, MachineProgressionState.MAX_LEVEL,
                List.of(), "", List.of(), false, "drop_forge", List.of("drop_hammer", "quench_line", "hydrogen_quench"), 2));
        int filled = press.getFluidHandler(Direction.NORTH).fill(new FluidStack(ModFluids.HYDROGEN_SOURCE.get(), InertAtmosphere.CAPACITY / 2),
                IFluidHandler.FluidAction.EXECUTE);
        check(filled > 0, "A Hydrogen Quench press should take Hydrogen");
        helper.succeedWhen(() -> {
            MetalPressVisualState state = press.currentVisualState();
            check(state.hydrogen() && state.gas() > 0.4F && state.gas() < 0.6F, "The cylinder should show half a tank of Hydrogen: " + state.key());
        });
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, path));
    }
}
