package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AffixForgeBlockEntity;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.DebugRerollerBlockEntity;
import com.rngtech.content.blockentity.PhaseCradleBlockEntity;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.item.RefinementConsumableItem;
import com.rngtech.content.item.TankContents;
import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.menu.RefinementMenuSupport;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.workorder.DriveBays;
import com.rngtech.rpg.MachineTraitRoller;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.refinement.RefinementSelection;
import com.rngtech.rpg.refinement.RefinementTargets;
import com.rngtech.rpg.unique.UniqueItems;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderConditions;
import com.rngtech.rpg.workorder.WorkPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * The Mirror Catalyst: it copies a part, cell, machine item, or Job Chip and leaves the original as it was; the copy is
 * Mirrored, every refinement path refuses it, and it cannot be mirrored again.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MirrorCatalystGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private MirrorCatalystGameTests() {
    }

    /** Each kind of target comes back unchanged, and the player gets one exact copy that only adds the Mirrored mark. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void mirrorCopiesEveryTargetKind(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        List<ItemStack> targets = List.of(
                rolled(new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get())),
                rolled(new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get())),
                rolled(new ItemStack(ModItems.crusherChassis(CrusherChassisMaterial.STEEL).get())),
                chip()
        );
        for (ItemStack target : targets) {
            player.getInventory().clearContent();
            ItemStackHandler forge = forge(target, new ItemStack(ModItems.MIRROR_CATALYST.get()));
            check(RefinementTargets.canMirror(target), target + " cannot be mirrored");
            check(RefinementMenuSupport.applyToTargetStack(player, forge, RefinementSelection.none(), false), "The Mirror failed on " + target);
            check(ItemStack.matches(forge.getStackInSlot(RefinementMenuSupport.SLOT_TARGET), target), "The Mirror changed the original " + target);
            check(forge.getStackInSlot(RefinementMenuSupport.SLOT_CONSUMABLE).isEmpty(), "The Mirror was not spent on " + target);
            ItemStack copy = carried(player, target);
            check(RefinementTargets.isMirrored(copy) && !RefinementTargets.isMirrored(target), "The copy of " + target + " is not Mirrored");
            ItemStack unmarked = copy.copy();
            unmarked.remove(ModDataComponents.MIRRORED.get());
            check(ItemStack.matches(unmarked, target), "The copy differs from " + target + " beyond the Mirrored mark: " + copy);
            check(!RefinementTargets.canMirror(copy), "A Mirrored copy of " + target + " can be mirrored again");
        }
        helper.succeed();
    }

    /** A filled tank's Mirrored copy is empty, so the Mirror never copies fluid; the original keeps its fluid. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void mirrorLeavesTankFluidOnTheOriginal(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack target = rolled(new ItemStack(ModItems.compressorTank(CompressorTankMaterial.STEEL).get()));
        TankContents contents = TankContents.of(new FluidStack(Fluids.WATER, 40_000), new FluidStack(Fluids.WATER, 200_000));
        target.set(ModDataComponents.TANK_CONTENTS.get(), contents);
        ItemStackHandler forge = forge(target, new ItemStack(ModItems.MIRROR_CATALYST.get()));
        check(RefinementMenuSupport.applyToTargetStack(player, forge, RefinementSelection.none(), false), "The Mirror failed on a filled tank");
        check(contents.equals(forge.getStackInSlot(RefinementMenuSupport.SLOT_TARGET).get(ModDataComponents.TANK_CONTENTS.get())),
                "The original tank lost its fluid");
        ItemStack copy = carried(player, target);
        check(RefinementTargets.isMirrored(copy) && !copy.has(ModDataComponents.TANK_CONTENTS.get()), "The Mirrored tank copied the fluid: " + copy);
        helper.succeed();
    }

    /** Every refinement consumable, the Mirror included, fails on a Mirrored copy, leaves it alone, and is not spent. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void mirroredCopiesRefuseEveryRefinement(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack original = rolled(new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get()));
        ItemStack copy = mirrored(original);
        ItemStackHandler control = forge(original, new ItemStack(ModItems.CHAOS_CRYSTAL.get()));
        check(RefinementMenuSupport.applyToTargetStack(player, control, RefinementSelection.none(), false), "The unmirrored original refused a Chaos Crystal");

        List<RefinementConsumableItem> consumables = BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof RefinementConsumableItem consumable && consumable.operation() != null)
                .map(RefinementConsumableItem.class::cast)
                .toList();
        check(consumables.contains(ModItems.VOLATILE_CATALYST.get()) && consumables.contains(ModItems.MIRROR_CATALYST.get())
                && consumables.contains(ModItems.NULL_CRYSTAL.get()), "The consumable list is missing catalysts: " + consumables);
        for (RefinementConsumableItem consumable : consumables) {
            ItemStackHandler forge = forge(copy, new ItemStack(consumable));
            check(!RefinementMenuSupport.applyToTargetStack(player, forge, RefinementSelection.none(), false), consumable + " refined a Mirrored copy");
            check(ItemStack.matches(forge.getStackInSlot(RefinementMenuSupport.SLOT_TARGET), copy), consumable + " changed a Mirrored copy");
            check(forge.getStackInSlot(RefinementMenuSupport.SLOT_CONSUMABLE).getCount() == 1, consumable + " was spent on a Mirrored copy");
        }

        check(PhaseCradleBlockEntity.donorAffixes(copy).isEmpty() && !PhaseCradleBlockEntity.donorAffixes(original).isEmpty(),
                "A Mirrored donor offers the Phase Array affixes to graft");
        check(!DebugRerollerBlockEntity.canReroll(copy), "The Debug Reroller takes a Mirrored copy");

        ItemStack unique = BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(stack -> UniqueItems.isUnique(stack) && RefinementTargets.canRefine(stack))
                .findFirst()
                .orElseThrow();
        ItemStackHandler forge = forge(unique, new ItemStack(ModItems.MIRROR_CATALYST.get()));
        check(!RefinementMenuSupport.applyToTargetStack(player, forge, RefinementSelection.none(), false)
                && forge.getStackInSlot(RefinementMenuSupport.SLOT_CONSUMABLE).getCount() == 1, "The Mirror copied a Unique");
        helper.succeed();
    }

    /** The placed Affix Forge needs an Affix Resonance Matrix for the Mirror, and turns away a second Mirror on the copy. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void affixForgeMirrorsWithMatrixOnce(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        helper.setBlock(MACHINE, ModBlocks.AFFIX_FORGE.get());
        AffixForgeBlockEntity forge = (AffixForgeBlockEntity) helper.getBlockEntity(MACHINE);
        ItemStack target = rolled(new ItemStack(ModItems.crushHead(CrushHeadMaterial.IRON).get()));
        forge.getInventory().setStackInSlot(AffixForgeBlockEntity.SLOT_TARGET, target);
        forge.getInventory().setStackInSlot(AffixForgeBlockEntity.SLOT_CONSUMABLE, new ItemStack(ModItems.MIRROR_CATALYST.get(), 2));
        check(!forge.applyRefinement(player, RefinementSelection.none()), "The Mirror worked without an Affix Resonance Matrix");

        forge.getInventory().setStackInSlot(AffixForgeBlockEntity.SLOT_RESONANCE_MATRIX, new ItemStack(ModItems.AFFIX_RESONANCE_MATRIX.get()));
        forge.getInventory().setStackInSlot(AffixForgeBlockEntity.SLOT_MODIFIER, new ItemStack(ModItems.CONSERVATION_CRYSTAL.get()));
        check(!forge.applyRefinement(player, RefinementSelection.none()), "The Mirror worked with a crystal in the focus slot");

        forge.getInventory().setStackInSlot(AffixForgeBlockEntity.SLOT_MODIFIER, ItemStack.EMPTY);
        check(forge.applyRefinement(player, RefinementSelection.none()), "The Mirror failed with an Affix Resonance Matrix");
        ItemStack copy = carried(player, target);
        check(forge.getInventory().getStackInSlot(AffixForgeBlockEntity.SLOT_CONSUMABLE).getCount() == 1, "The Mirror was not spent once");

        forge.getInventory().setStackInSlot(AffixForgeBlockEntity.SLOT_TARGET, copy);
        check(!forge.applyRefinement(player, RefinementSelection.none()), "A Mirrored copy was mirrored again");
        check(forge.getInventory().getStackInSlot(AffixForgeBlockEntity.SLOT_CONSUMABLE).getCount() == 1, "The refused Mirror was spent");
        helper.succeed();
    }

    /** A Mirrored machine item placed down keeps the mark: its Refinement tab refuses it, and it drops Mirrored. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void placedMirroredMachineStaysLocked(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        Block block = ModBlocks.crusherChassis(CrusherChassisMaterial.STEEL).get();
        ItemStack item = mirrored(rolled(new ItemStack(block.asItem())));
        MachineTraits traits = RefinementTargets.storedTraits(item);
        CrusherBlockEntity crusher = MachineTestSupport.place(helper, MACHINE, block, CrusherBlockEntity.class);
        crusher.applyComponentsFromItemStack(item);
        check(crusher.isMirrored() && crusher.machineTraits().equals(traits), "The placed crusher lost the Mirrored mark or its traits");

        crusher.getRefinementInventory().setStackInSlot(0, new ItemStack(ModItems.CHAOS_CRYSTAL.get()));
        check(!RefinementMenuSupport.applyToMachine(player, crusher, new ItemStackHandler(1), block), "A Mirrored machine took a Chaos Crystal");
        check(crusher.machineTraits().equals(traits) && crusher.getRefinementInventory().getStackInSlot(0).getCount() == 1,
                "The refused Chaos Crystal changed the machine or was spent");

        List<ItemStack> drops = Block.getDrops(helper.getBlockState(MACHINE), helper.getLevel(), helper.absolutePos(MACHINE), crusher);
        check(drops.size() == 1 && RefinementTargets.isMirrored(drops.getFirst()) && RefinementTargets.storedTraits(drops.getFirst()).equals(traits),
                "The broken machine did not drop as the Mirrored copy: " + drops);

        CrusherBlockEntity plain = MachineTestSupport.place(helper, MACHINE.above(), block, CrusherBlockEntity.class);
        plain.getRefinementInventory().setStackInSlot(0, new ItemStack(ModItems.MIRROR_CATALYST.get()));
        check(!RefinementMenuSupport.applyToMachine(player, plain, new ItemStackHandler(1), block)
                && plain.getRefinementInventory().getStackInSlot(0).getCount() == 1, "A Refinement tab used a Mirror Catalyst");
        helper.succeed();
    }

    /** A Mirrored Job Chip loaded into a drive bay comes back out Mirrored. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void mirroredChipStaysMirroredInDriveBays(GameTestHelper helper) {
        CrusherBlockEntity crusher = MachineTestSupport.place(helper, MACHINE, ModBlocks.crusherChassis(CrusherChassisMaterial.STEEL).get(),
                CrusherBlockEntity.class);
        DriveBays bays = new DriveBays(crusher);
        ItemStack chip = mirrored(chip());
        check(bays.insertItem(0, chip, false).isEmpty(), "The drive bay refused the Mirrored chip");
        check(crusher.workOrders().orders().getFirst().mirrored(), "The loaded chip forgot it is Mirrored");
        ItemStack unloaded = bays.extractItem(0, 1, false);
        check(RefinementTargets.isMirrored(unloaded) && JobChipItem.data(unloaded).equals(JobChipItem.data(chip)),
                "The unloaded chip is not the Mirrored chip: " + unloaded);
        helper.succeed();
    }

    private static ItemStackHandler forge(ItemStack target, ItemStack consumable) {
        ItemStackHandler forge = new ItemStackHandler(3);
        forge.setStackInSlot(RefinementMenuSupport.SLOT_TARGET, target.copy());
        forge.setStackInSlot(RefinementMenuSupport.SLOT_CONSUMABLE, consumable);
        return forge;
    }

    /** {@code stack} with rolled traits that have Refinement Potential and at least one affix. */
    private static ItemStack rolled(ItemStack stack) {
        for (long seed = 0; seed < 500; seed++) {
            MachineTraits traits = MachineTraitRoller.roll(RefinementTargets.eligibilityProfile(stack),
                    RefinementTargets.modifierRollComponentStage(stack), RandomSource.create(seed));
            if (traits.refinementPotential() > 0 && traits.modifiers().stream().anyMatch(modifier -> modifier.slot().isAffix())) {
                ItemStack rolled = stack.copy();
                RefinementTargets.setTraits(rolled, traits);
                return rolled;
            }
        }
        throw new IllegalStateException("No roll with an affix for " + stack);
    }

    private static ItemStack chip() {
        WorkOrder job = new WorkOrder("crusher", 1, new WorkObjective(RNGTech.id("mirror"),
                new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.STAGE, 3, "", 200)), 4, 0, WorkOrder.BASE_BOUNTY, 0);
        WorkOrderConditions.Rolled rolled = WorkOrderConditions.roll(job, Rarity.MAGIC, RandomSource.create(4L));
        return JobChipItem.of(ModItems.JOB_CHIP.get(), rolled.order(), rolled.traits());
    }

    private static ItemStack mirrored(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.set(ModDataComponents.MIRRORED.get(), Unit.INSTANCE);
        return copy;
    }

    /** The one copy of {@code target}'s item the player carries. */
    private static ItemStack carried(Player player, ItemStack target) {
        List<ItemStack> copies = player.getInventory().items.stream().filter(stack -> stack.is(target.getItem())).toList();
        check(copies.size() == 1 && copies.getFirst().getCount() == 1, "The player should carry one copy of " + target + ": " + copies);
        return copies.getFirst();
    }
}
