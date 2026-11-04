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
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Unidentified Gear, Battery Cells, and tool parts roll their traits when a player installs them. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IdentifyOnInstallGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    private IdentifyOnInstallGameTests() {
    }

    /** Automation inserts leave Gear unidentified; a player putting it in a slot identifies the installed copy. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void onlyPlayerInstallIdentifies(GameTestHelper helper) {
        MelterBlockEntity melter = MachineTestSupport.place(helper, MACHINE, ModBlocks.MELTER.get(), MelterBlockEntity.class);
        ItemStackHandler gear = melter.getGearInventory();
        ItemStack heatCores = unidentified(new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()), 2);

        ItemStack rest = gear.insertItem(MelterBlockEntity.SLOT_HEAT_CORE, heatCores, false);
        ItemStack inserted = gear.getStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE);
        check(inserted.getCount() == 1 && CraftedTraitOutputs.isUnidentified(inserted),
                "A Heat Core inserted by automation should stay unidentified");
        check(rest.getCount() == 1 && CraftedTraitOutputs.isUnidentified(rest), "The Heat Core left over should stay unidentified");

        gear.setStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE, ItemStack.EMPTY);
        gear.setStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE, inserted);
        ItemStack installed = gear.getStackInSlot(MelterBlockEntity.SLOT_HEAT_CORE);
        check(identified(installed), "A Heat Core a player puts in the slot should be identified");

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

    /** A machine placed without a player, as a dispenser or block placer would, is refused while it is unidentified. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void automatedPlacementRefusesUnidentified(GameTestHelper helper) {
        ItemStack chassis = unidentified(new ItemStack(ModBlocks.crusherChassis(CrusherChassisMaterial.IRON).get()), 1);
        BlockPos target = helper.absolutePos(MACHINE);
        // Clicking the empty target space places the chassis there, as a click on a replaceable block does.
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);

        InteractionResult automated = ((BlockItem) chassis.getItem()).place(
                new BlockPlaceContext(helper.getLevel(), null, InteractionHand.MAIN_HAND, chassis, hit));
        check(automated == InteractionResult.FAIL, "Placing without a player should be refused, got " + automated);
        check(helper.getLevel().getBlockState(target).isAir(), "Nothing should be placed without a player");
        check(chassis.getCount() == 1 && CraftedTraitOutputs.isUnidentified(chassis), "The refused chassis should stay unidentified");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InteractionResult placed = ((BlockItem) chassis.getItem()).place(
                new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, chassis, hit));
        check(placed.consumesAction(), "A player should place the chassis, got " + placed);
        check(helper.getLevel().getBlockEntity(target) instanceof CrusherBlockEntity, "The player should place a Crusher");
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
