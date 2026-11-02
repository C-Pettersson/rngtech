package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.affixsorter.AffixSorterCosts;
import com.rngtech.content.affixsorter.SorterRule;
import com.rngtech.content.affixsorter.SorterRules;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.AffixSorterBlockEntity;
import com.rngtech.content.blockentity.IdentifierBlockEntity;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.item.CraftedTraitOutputs;
import com.rngtech.content.item.RuleCardItem;
import com.rngtech.content.machine.FluidPumpMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModFluids;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

/** The Identifier rolls items as a crafting grid would, and the Affix Sorter routes them by its first matching rule. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AffixSorterGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 2, 2);
    private static final BlockPos SECOND = new BlockPos(4, 2, 2);

    private AffixSorterGameTests() {
    }

    /** An unidentified part fed by automation comes out identified, paying fluid by its stage; identified items are refused. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void identifierIdentifiesFedItems(GameTestHelper helper) {
        IdentifierBlockEntity identifier = identifier(helper, MACHINE);
        IItemHandler input = identifier.getItemHandler(Direction.UP);
        ItemStack identified = CraftedTraitOutputs.rollIfMissing(
                CraftedTraitOutputs.unidentifiedVariant(new ItemStack(ModItems.servo(ServoMaterial.STEEL).get())), helper.getLevel().random);
        check(!input.insertItem(0, identified, true).isEmpty(), "The Identifier should refuse an identified item");
        ItemStack part = unidentified(new ItemStack(ModItems.servo(ServoMaterial.STEEL).get()));
        check(input.insertItem(0, part, false).isEmpty(), "The Identifier should take an unidentified part from automation");
        int before = identifier.getTank().getFluidAmount();
        // The Identifier's inherent Reserve already lowers Fluid Use a little, so read the cost from the machine.
        int expectedFluid = identifier.fluidCost(identifier.effectiveStats());
        check(expectedFluid > 0 && expectedFluid <= AffixSorterCosts.identifierFluidPerItem(ServoMaterial.STEEL.stage()),
                "A Stage 4 part should cost at most its base fluid, cost " + expectedFluid);
        helper.succeedWhen(() -> {
            ItemStack output = identifier.getItemHandler(Direction.DOWN).getStackInSlot(0);
            check(!output.isEmpty(), "The identified part should reach the output slot");
            check(output.has(ModDataComponents.MACHINE_TRAITS.get()) && !CraftedTraitOutputs.isUnidentified(output),
                    "The output should carry rolled traits");
            check(before - identifier.getTank().getFluidAmount() == expectedFluid,
                    "A Stage 4 part should cost " + expectedFluid + " mB, cost " + (before - identifier.getTank().getFluidAmount()));
        });
    }

    /** Without Identifying Fluid the Identifier waits and keeps the item. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void identifierWaitsForFluid(GameTestHelper helper) {
        IdentifierBlockEntity identifier = identifier(helper, MACHINE);
        identifier.getTank().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        identifier.getItemHandler(Direction.UP).insertItem(0, unidentified(new ItemStack(ModItems.servo(ServoMaterial.STEEL).get())), false);
        helper.runAfterDelay(120, () -> {
            check(identifier.statusCode(identifier.effectiveStats()) == IdentifierBlockEntity.STATUS_NO_FLUID, "The Identifier should wait for fluid");
            check(CraftedTraitOutputs.isUnidentified(identifier.getProcessInventory().getStackInSlot(IdentifierBlockEntity.SLOT_INPUT)),
                    "The waiting item should stay unidentified in the input slot");
            helper.succeed();
        });
    }

    /** The first enabled matching rule decides; the fallback takes the rest; each face exposes only its own buffer. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void sorterRoutesByFirstMatch(GameTestHelper helper) {
        AffixSorterBlockEntity sorter = sorter(helper, MACHINE);
        SorterRule rejectMagic = SorterRule.DEFAULT.withRoute(SorterRule.Route.REJECT).withRarities(List.of(Rarity.MAGIC));
        SorterRule passMagicOrRare = SorterRule.DEFAULT.withRarities(List.of(Rarity.MAGIC, Rarity.RARE)).withPulse(true);
        sorter.setSorterRules(SorterRules.EMPTY.withAdded(rejectMagic).withAdded(passMagicOrRare));

        Direction facing = sorter.getBlockState().getValue(BaseMachineBlock.FACING);
        Direction left = AffixSorterBlockEntity.RelativeFace.LEFT.absolute(facing);
        Direction right = AffixSorterBlockEntity.RelativeFace.RIGHT.absolute(facing);
        IItemHandler input = sorter.getItemHandler(Direction.UP);
        check(!input.insertItem(0, unidentified(new ItemStack(ModItems.servo(ServoMaterial.STEEL).get())), true).isEmpty(),
                "The sorter should refuse an unidentified item");
        check(input.insertItem(0, withRarity(Rarity.RARE), false).isEmpty(), "The sorter should take an identified Rare part");
        helper.runAfterDelay(60, () -> {
            check(!sorter.getItemHandler(left).getStackInSlot(0).isEmpty(), "A Rare part should pass through the left face");
            check(sorter.getItemHandler(right).getStackInSlot(0).isEmpty(), "Nothing should reach the Reject buffer yet");
            check(sorter.getItemHandler(left).insertItem(0, withRarity(Rarity.RARE), true).getCount() == 1, "A buffer face takes no items");
            input.insertItem(0, withRarity(Rarity.MAGIC), false);
            helper.runAfterDelay(60, () -> {
                check(!sorter.getItemHandler(right).getStackInSlot(0).isEmpty(), "The Reject rule above should win for a Magic part");
                input.insertItem(0, withRarity(Rarity.NORMAL), false);
                helper.runAfterDelay(60, () -> {
                    check(!sorter.getItemHandler(right).getStackInSlot(1).isEmpty()
                                    || sorter.getItemHandler(right).getStackInSlot(0).getCount() > 1,
                            "A Normal part matches no rule and should take the Reject fallback");
                    helper.succeed();
                });
            });
        });
    }

    /** Enabled rules past Rule Capacity are switched off from the bottom, and a Rule Card carries a list between sorters. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 100)
    public static void ruleCardCopiesAndCapacityLimits(GameTestHelper helper) {
        AffixSorterBlockEntity source = sorter(helper, MACHINE);
        SorterRules many = SorterRules.EMPTY;
        for (int index = 0; index < 12; index++) {
            many = many.withAdded(SorterRule.DEFAULT);
        }
        source.setSorterRules(many);
        int capacity = AffixSorterCosts.baseRuleCapacity();
        check(source.sorterRules().rules().size() == 12, "The sorter should store every rule");
        check(source.sorterRules().enabledCount() == capacity, "Only " + capacity + " rules should stay enabled, got "
                + source.sorterRules().enabledCount());
        check(source.energyCost(source.effectiveStats()) == AffixSorterCosts.sorterFePerItem(capacity),
                "FE per item should follow the enabled rule count");

        AffixSorterBlockEntity target = sorter(helper, SECOND);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack card = new ItemStack(ModItems.RULE_CARD.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, card);
        use(helper, player, card, MACHINE);
        check(RuleCardItem.rules(card) != null && RuleCardItem.rules(card).rules().size() == 12, "An empty card should copy the rules");
        use(helper, player, card, SECOND);
        check(target.sorterRules().equals(source.sorterRules()), "A written card should replace the target's rules");
        helper.succeed();
    }

    private static void use(GameTestHelper helper, Player player, ItemStack card, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        card.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, card,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }

    private static IdentifierBlockEntity identifier(GameTestHelper helper, BlockPos pos) {
        IdentifierBlockEntity identifier = MachineTestSupport.place(helper, pos, ModBlocks.IDENTIFIER.get(), IdentifierBlockEntity.class);
        identifier.getGearInventory().setStackInSlot(IdentifierBlockEntity.SLOT_FLUID_PUMP, new ItemStack(ModItems.fluidPump(FluidPumpMaterial.EXOTIC).get()));
        identifier.getGearInventory().setStackInSlot(IdentifierBlockEntity.SLOT_SERVO, new ItemStack(ModItems.servo(ServoMaterial.EXOTIC).get()));
        identifier.getGearInventory().setStackInSlot(IdentifierBlockEntity.SLOT_BATTERY_CELL, new ItemStack(ModItems.batteryCell(BatteryCellMaterial.EXOTIC).get()));
        identifier.getTank().fill(new FluidStack(ModFluids.IDENTIFYING_FLUID_SOURCE.get(), 8000), IFluidHandler.FluidAction.EXECUTE);
        MachineTestSupport.powerEachTick(helper, identifier.getEnergyStorage(null));
        return identifier;
    }

    private static AffixSorterBlockEntity sorter(GameTestHelper helper, BlockPos pos) {
        AffixSorterBlockEntity sorter = MachineTestSupport.place(helper, pos, ModBlocks.AFFIX_SORTER.get(), AffixSorterBlockEntity.class);
        sorter.getGearInventory().setStackInSlot(AffixSorterBlockEntity.SLOT_RULE_CORE, new ItemStack(ModItems.EXOTIC_RULE_CORE.get()));
        sorter.getGearInventory().setStackInSlot(AffixSorterBlockEntity.SLOT_BATTERY_CELL, new ItemStack(ModItems.batteryCell(BatteryCellMaterial.EXOTIC).get()));
        MachineTestSupport.powerEachTick(helper, sorter.getEnergyStorage(null));
        return sorter;
    }

    /** An identified Steel Servo with the given rarity and no affixes. */
    private static ItemStack withRarity(Rarity rarity) {
        ItemStack stack = new ItemStack(ModItems.servo(ServoMaterial.STEEL).get());
        stack.set(ModDataComponents.MACHINE_TRAITS.get(), new MachineTraits(rarity, 10, List.of()));
        return stack;
    }

    private static ItemStack unidentified(ItemStack stack) {
        ItemStack variant = CraftedTraitOutputs.unidentifiedVariant(stack);
        check(CraftedTraitOutputs.isUnidentified(variant), "Test setup should make an unidentified " + stack.getHoverName().getString());
        return variant;
    }
}
