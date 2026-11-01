package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.MelterBlockEntity;
import com.rngtech.content.blockentity.ToolBenchBlockEntity;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.item.ModularToolItem;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.tool.FieldToolAssembly;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.content.tool.ToolHeadMaterial;
import com.rngtech.content.tool.ToolRodMaterial;
import com.rngtech.rpg.MachineTraits;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Unidentified Gear, Battery Cells, and tool parts roll their traits when they are installed. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IdentifyOnInstallGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    private IdentifyOnInstallGameTests() {
    }

    /** Inserting one item of an unidentified stack identifies the installed copy and leaves the rest unidentified. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void insertedGearIdentifies(GameTestHelper helper) {
        MelterBlockEntity melter = MachineTestSupport.place(helper, MACHINE, ModBlocks.MELTER.get(), MelterBlockEntity.class);
        ItemStackHandler gear = melter.getGearInventory();
        ItemStack heatCores = unidentified(new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()), 2);

        gear.insertItem(MelterBlockEntity.SLOT_HEAT_CORE, heatCores, true);
        check(gear.getStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE).isEmpty(), "A simulated insert should install nothing");
        check(CraftedTraitOutputs.isUnidentified(heatCores), "A simulated insert should not identify the offered stack");

        ItemStack rest = gear.insertItem(MelterBlockEntity.SLOT_HEAT_CORE, heatCores, false);
        ItemStack installed = gear.getStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE);
        check(installed.getCount() == 1 && identified(installed), "The installed Heat Core should be identified");
        check(rest.getCount() == 1 && CraftedTraitOutputs.isUnidentified(rest), "The Heat Core left over should stay unidentified");

        MachineTraits rolled = installed.get(ModDataComponents.MACHINE_TRAITS.get());
        gear.setStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE, ItemStack.EMPTY);
        gear.setStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE, installed);
        check(rolled.equals(gear.getStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE).get(ModDataComponents.MACHINE_TRAITS.get())),
                "Reinstalling an identified Heat Core should keep its traits");
        helper.succeed();
    }

    /** A Crusher identifies its Crush Head and Battery Cell, but not an item waiting to be crushed. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void crusherIdentifiesOnlyGear(GameTestHelper helper) {
        CrusherBlockEntity crusher = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.crusherChassis(CrusherChassisMaterial.IRON).get(), CrusherBlockEntity.class);
        ItemStackHandler inventory = crusher.getInventory();
        inventory.setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD,
                unidentified(new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get()), 1));
        inventory.setStackInSlot(CrusherBlockEntity.SLOT_FUEL,
                unidentified(new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get()), 1));
        inventory.setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A,
                unidentified(new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get()), 1));

        check(identified(inventory.getStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD)), "The installed Crush Head should be identified");
        check(identified(inventory.getStackInSlot(CrusherBlockEntity.SLOT_FUEL)), "The installed Battery Cell should be identified");
        check(CraftedTraitOutputs.isUnidentified(inventory.getStackInSlot(CrusherBlockEntity.SLOT_INPUT_A)),
                "An item in the input slot should stay unidentified");
        helper.succeed();
    }

    /** Assembling a tool from unidentified parts identifies the head, rod, and Battery Cell it stores. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void toolBenchIdentifiesParts(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.TOOL_BENCH.get());
        ToolBenchBlockEntity bench = (ToolBenchBlockEntity) helper.getBlockEntity(MACHINE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStackHandler inventory = bench.getInventory();
        inventory.setStackInSlot(ToolBenchBlockEntity.SLOT_HEAD,
                unidentified(new ItemStack(ModItems.toolHead(ToolHeadFamily.PICK, ToolHeadMaterial.IRON).get()), 1));
        inventory.setStackInSlot(ToolBenchBlockEntity.SLOT_ROD,
                unidentified(new ItemStack(ModItems.toolRod(ToolRodMaterial.IRON).get()), 1));
        inventory.setStackInSlot(ToolBenchBlockEntity.SLOT_BATTERY_CELL,
                unidentified(new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get()), 1));

        String result = bench.apply(player);
        check(result.equals("rngtech.tool_bench.success.assembled"), "The bench should assemble the tool, got " + result);
        FieldToolAssembly assembly = ModularToolItem.assembly(inventory.getStackInSlot(ToolBenchBlockEntity.SLOT_TOOL));
        check(identified(assembly.head()), "The stored Tool Head should be identified");
        check(identified(assembly.rod()), "The stored Tool Rod should be identified");
        check(identified(assembly.batteryCell()), "The stored Battery Cell should be identified");
        helper.succeed();
    }

    private static ItemStack unidentified(ItemStack stack, int count) {
        ItemStack variant = CraftedTraitOutputs.unidentifiedVariant(stack);
        variant.setCount(count);
        check(CraftedTraitOutputs.isUnidentified(variant), "Test setup should make an unidentified " + stack.getHoverName().getString());
        return variant;
    }

    private static boolean identified(ItemStack stack) {
        return !stack.isEmpty() && !CraftedTraitOutputs.isUnidentified(stack) && stack.has(ModDataComponents.MACHINE_TRAITS.get());
    }
}
