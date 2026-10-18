package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AlloyFurnaceBlockEntity;
import com.rngtech.content.blockentity.AlloyFurnaceVisualState;
import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.blockentity.FurnaceVisualState;
import com.rngtech.content.blockentity.InertAtmosphere;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/** The world view of the Furnace and Alloy Furnace: lane heat, items, and installed Gear reach the synced visual state. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FurnaceVisualGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private FurnaceVisualGameTests() {
    }

    /** A warming electric Furnace shows its Heat Core, its cell, the item on the hearth, and the lane's heat and target. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void furnaceShowsItsLaneHeatAndGear(GameTestHelper helper) {
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.furnaceChassis(FurnaceChassisMaterial.STEEL).get(), FurnaceBlockEntity.class);
        furnace.getGearInventory().setStackInSlot(0, new ItemStack(ModItems.heatCore(HeatCoreMaterial.STEEL).get()));
        furnace.getInventory().setStackInSlot(FurnaceBlockEntity.SLOT_FUEL, new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get()));
        furnace.getInventory().setStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START, new ItemStack(Items.RAW_IRON, 16));
        MachineTestSupport.powerEachTick(helper, furnace.getEnergyStorage(null));

        helper.succeedWhen(() -> {
            FurnaceVisualState state = furnace.currentVisualState();
            check(state.lanes() == 1, "A Steel Furnace has one lane: " + state.lanes());
            check(state.heatCore().equals("steel") && state.cell().equals("iron"), "Wrong parts shown: " + state.key());
            check(state.input(0) == Items.RAW_IRON, "The hearth should show Raw Iron: " + state.key());
            check(state.target(0) > 0 && state.maxHeat() >= state.target(0), "The bar should show the target under the core's limit: " + state.key());
            check(state.heat(0) > 400, "The lane should have warmed up: " + state.key());
            check(state.charge() >= 0.0F, "A cell should light the charge meter: " + state.key());
            check(state.setpoint(0) >= state.target(0) && state.overheat(0) > state.target(0), "The bar should show the setpoint and overheat line: " + state.key());
            check(state.nitrogen() == 0.0F && !state.atmosphere(), "An empty Nitrogen tank should show empty and unlit: " + state.key());
            check(FurnaceVisualState.load(state.save()).key().equals(state.key()), "The visual state should survive syncing: " + state.key());
        });
    }

    /**
     * A Blast Draft Furnace pushed to the top of its slider shows its setpoint past the target, and its cylinder shows the
     * Nitrogen draining and the valve lit while the lane works above its old overheat line.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 1200)
    public static void overheatedFurnaceShowsItsNitrogen(GameTestHelper helper) {
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.furnaceChassis(FurnaceChassisMaterial.TUNGSTENSTEEL).get(), FurnaceBlockEntity.class);
        furnace.getGearInventory().setStackInSlot(0, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        String start = MachineMasteryFamily.FURNACE.startNodeId();
        List<String> path = MegaPassiveTree.allocationPath(node -> node.id().equals(start), MegaPassiveTree.node("blast_draft"))
                .stream().map(MegaPassiveNode::id).toList();
        furnace.setMachineProgression(new MachineProgressionState(MachineProgressionState.xpForLevel(MachineProgressionState.MAX_LEVEL), 0,
                MachineProgressionState.MAX_LEVEL, path, start, List.of(), false));
        furnace.setHeatSlider(100);
        furnace.getInventory().setStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START, new ItemStack(item("tin_dust"), 64));
        furnace.getFluidHandler(null).fill(new FluidStack(ModFluids.NITROGEN_SOURCE.get(), InertAtmosphere.CAPACITY),
                IFluidHandler.FluidAction.EXECUTE);
        MachineTestSupport.powerEachTick(helper, furnace.getEnergyStorage(null));

        helper.succeedWhen(() -> {
            FurnaceVisualState state = furnace.currentVisualState();
            check(state.setpoint(0) > state.target(0) && state.target(0) > 0, "The slider should push the setpoint past the target: " + state.key());
            check(state.atmosphere(), "The valve should light while the lane uses Nitrogen: " + state.key());
            check(state.nitrogen() > 0.0F && state.nitrogen() < 1.0F, "The cylinder should show Nitrogen draining: " + state.key());
            check(FurnaceVisualState.load(state.save()).key().equals(state.key()), "The visual state should survive syncing: " + state.key());
        });
    }

    /** Each Lead Furnace chamber shows its own lane: an item and heat where there is work, a cold empty chamber elsewhere. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void leadFurnaceShowsEachChamber(GameTestHelper helper) {
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.furnaceChassis(FurnaceChassisMaterial.LEAD).get(), FurnaceBlockEntity.class);
        furnace.getGearInventory().setStackInSlot(0, new ItemStack(ModItems.heatCore(HeatCoreMaterial.STEEL).get()));
        furnace.getInventory().setStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START, new ItemStack(Items.RAW_COPPER, 16));
        furnace.getInventory().setStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START + 3, new ItemStack(Items.RAW_IRON, 16));
        MachineTestSupport.powerEachTick(helper, furnace.getEnergyStorage(null));

        helper.succeedWhen(() -> {
            FurnaceVisualState state = furnace.currentVisualState();
            check(state.lanes() == 4, "The Lead Furnace has four lanes: " + state.lanes());
            check(state.input(0) == Items.RAW_COPPER && state.input(3) == Items.RAW_IRON, "Each chamber should show its own item: " + state.key());
            check(state.input(1) == Items.AIR && state.target(1) == 0, "An empty chamber should show nothing: " + state.key());
            check(state.heat(0) > 300 && state.heat(3) > 300, "The working chambers should warm up: " + state.key());
            check(state.heat(1) < 100, "An empty chamber should stay cold: " + state.key());
            check(state.cell().isEmpty() && state.charge() < 0.0F, "Without a cell the meter should be dark: " + state.key());
        });
    }

    /** The Stage 0 Furnace shows its fire while fuel burns, and has no Gear to show. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 600)
    public static void stageZeroFurnaceShowsItsFire(GameTestHelper helper) {
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE, ModBlocks.FURNACE.get(), FurnaceBlockEntity.class);
        furnace.getInventory().setStackInSlot(FurnaceBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));
        furnace.getInventory().setStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START, new ItemStack(Items.RAW_COPPER, 8));

        helper.succeedWhen(() -> {
            FurnaceVisualState state = furnace.currentVisualState();
            check(state.working() && state.burn() > 0.0F, "The fire should burn: " + state.key());
            check(state.input(0) == Items.RAW_COPPER && state.heat(0) > 300, "The oven should warm up for its Raw Copper: " + state.key());
            check(state.heatCore().isEmpty() && state.cell().isEmpty(), "A Stage 0 Furnace has no Gear to show: " + state.key());
            check(state.nitrogen() < 0.0F, "A Stage 0 Furnace has no Nitrogen tank: " + state.key());
        });
    }

    /** A working Alloy Furnace shows every installed part, the items in its hoppers, and a pool that melts in. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 600)
    public static void alloyFurnaceShowsItsGearHoppersAndMelt(GameTestHelper helper) {
        AlloyFurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.alloyFurnaceChassis(AlloyFurnaceChassisMaterial.TITANIUM).get(), AlloyFurnaceBlockEntity.class);
        ItemStackHandler gear = furnace.getGearInventory();
        gear.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        gear.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_CRUCIBLE, new ItemStack(ModItems.alloyCrucible(AlloyCrucibleMaterial.TITANIUM).get()));
        gear.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_SERVO, new ItemStack(ModItems.servo(ServoMaterial.STEEL).get()));
        gear.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_BATTERY_CELL, new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get()));
        ItemStackHandler process = furnace.getProcessInventory();
        process.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START, new ItemStack(Items.COPPER_INGOT, 12));
        process.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START + 1, new ItemStack(item("tin_ingot"), 4));
        process.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START + 2, new ItemStack(Items.CHARCOAL, 4));
        MachineTestSupport.powerEachTick(helper, furnace.getEnergyStorage(null));

        helper.succeedWhen(() -> {
            AlloyFurnaceVisualState state = furnace.currentVisualState();
            check(state.heatCore().equals("titanium") && state.crucible().equals("titanium") && state.servo().equals("steel")
                    && state.cell().equals("iron"), "Wrong parts shown: " + state.key());
            check(state.hoppers() == 4, "A Titanium Crucible feeds four hoppers: " + state.key());
            check(state.input(0) == Items.COPPER_INGOT && state.input(1) == item("tin_ingot") && state.input(3) == Items.AIR,
                    "Each hopper should show its own input: " + state.key());
            check(state.target() > 0 && state.heat() > 400, "The crucible should warm toward its target: " + state.key());
            check(state.working() && state.melt() > 0.2F, "The charge should be melting: " + state.key());
            check(state.nitrogen() == 0.0F && !state.atmosphere(), "An empty Nitrogen tank should show empty and unlit: " + state.key());
            check(AlloyFurnaceVisualState.load(state.save()).key().equals(state.key()), "The visual state should survive syncing: " + state.key());
        });
    }

    /** Without Gear an Alloy Furnace shows an empty well: no parts, no heat, and a dark meter, but its hopper item. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void bareAlloyFurnaceShowsAnEmptyWell(GameTestHelper helper) {
        AlloyFurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.alloyFurnaceChassis(AlloyFurnaceChassisMaterial.BRONZE).get(), AlloyFurnaceBlockEntity.class);
        furnace.getProcessInventory().setStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START, new ItemStack(Items.COPPER_INGOT, 4));
        helper.runAfterDelay(10, () -> {
            AlloyFurnaceVisualState state = furnace.currentVisualState();
            check(state.heatCore().isEmpty() && state.crucible().isEmpty() && state.servo().isEmpty() && state.cell().isEmpty(),
                    "A bare furnace has no parts to show: " + state.key());
            check(!state.working() && state.maxHeat() == 0 && state.charge() < 0.0F, "A bare furnace shows no heat or charge: " + state.key());
            check(state.input(0) == Items.COPPER_INGOT, "The hopper should still show its item: " + state.key());
            helper.succeed();
        });
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, path));
    }
}
