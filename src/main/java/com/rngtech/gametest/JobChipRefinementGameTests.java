package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.menu.RefinementMenuSupport;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.workorder.WorkOrderRewards;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.refinement.RefinementSelection;
import com.rngtech.rpg.refinement.RefinementTargets;
import com.rngtech.rpg.workorder.WorkFilter;
import com.rngtech.rpg.workorder.WorkObjective;
import com.rngtech.rpg.workorder.WorkObjectiveType;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderConditions;
import com.rngtech.rpg.workorder.WorkPart;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/** Job Chips through the item refinement path the Affix Forge uses, and the Phase Array turning them away. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JobChipRefinementGameTests {
    private JobChipRefinementGameTests() {
    }

    /** Refinement items work on a chip: its Bounty and listed reward follow its conditions, and a Volatile Catalyst corrupts it. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void refinementItemsWorkOnChips(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        WorkOrder job = new WorkOrder("crusher", 2, new WorkObjective(RNGTech.id("refine"),
                new WorkPart(WorkObjectiveType.CRUSH, WorkFilter.STAGE, 4, "", 200)), 5, 0, WorkOrder.BASE_BOUNTY, 0);
        WorkOrderConditions.Rolled rolled = WorkOrderConditions.roll(job, Rarity.MAGIC, RandomSource.create(4L));
        WorkOrder listed = rolled.order().withRewards(ItemContainerContents.fromItems(List.of(new ItemStack(ModItems.CHAOS_CRYSTAL.get(), 10))));
        ItemStack chip = JobChipItem.of(ModItems.JOB_CHIP.get(), listed, rolled.traits());
        check(RefinementTargets.canRefine(chip) && RefinementTargets.canCorrupt(chip), "A chip is not a refinement target");
        check(!RefinementTargets.canPhase(chip), "The Phase Array takes a Job Chip");
        check(!RefinementTargets.canRefine(new ItemStack(ModItems.JOB_CHIP.get())), "A blank chip is a refinement target");

        ItemStackHandler forge = new ItemStackHandler(3);
        forge.setStackInSlot(RefinementMenuSupport.SLOT_TARGET, chip);
        forge.setStackInSlot(RefinementMenuSupport.SLOT_CONSUMABLE, new ItemStack(ModItems.CHAOS_CRYSTAL.get()));
        check(RefinementMenuSupport.applyToTargetStack(player, forge, RefinementSelection.none(), false), "A Chaos Crystal failed on a chip");
        ItemStack rerolled = forge.getStackInSlot(RefinementMenuSupport.SLOT_TARGET);
        MachineTraits traits = JobChipItem.traits(rerolled);
        WorkOrder after = JobChipItem.data(rerolled);
        check(after.bounty() == WorkOrder.BASE_BOUNTY + WorkOrderConditions.bounty(traits), "The chip's Bounty did not follow its new conditions: " + after.bounty());
        int count = WorkOrderRewards.items(after.rewards()).getFirst().getCount();
        check(count == Math.max(1, (int) Math.round(10 * after.bounty() / (double) listed.bounty())), "The listed reward did not scale with Bounty: " + count);
        check(traits.refinementPotential() < rolled.traits().refinementPotential(), "The Chaos Crystal spent no Refinement Potential");

        forge.setStackInSlot(RefinementMenuSupport.SLOT_CONSUMABLE, new ItemStack(ModItems.VOLATILE_CATALYST.get()));
        check(RefinementMenuSupport.applyToTargetStack(player, forge, RefinementSelection.none(), false), "A Volatile Catalyst failed on a chip");
        ItemStack corrupted = forge.getStackInSlot(RefinementMenuSupport.SLOT_TARGET);
        check(JobChipItem.traits(corrupted).isCorrupted() && forge.getStackInSlot(RefinementMenuSupport.SLOT_CONSUMABLE).isEmpty(),
                "The Volatile Catalyst did not corrupt the chip and spend itself");
        forge.setStackInSlot(RefinementMenuSupport.SLOT_CONSUMABLE, new ItemStack(ModItems.VOLATILE_CATALYST.get()));
        check(!RefinementMenuSupport.applyToTargetStack(player, forge, RefinementSelection.none(), false), "A corrupted chip took a second Volatile Catalyst");
        helper.succeed();
    }
}
