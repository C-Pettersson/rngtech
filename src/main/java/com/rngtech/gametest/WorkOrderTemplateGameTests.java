package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.item.JobChipItem;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.workorder.WorkOrderJobs;
import com.rngtech.content.workorder.WorkOrderRewards;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.workorder.WorkOrder;
import com.rngtech.rpg.workorder.WorkOrderTemplate;

import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.SplittableRandom;

/** Job templates in a world: the objective they name, fixed rewards, their own reward table, and the chip's name. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorkOrderTemplateGameTests {
    private WorkOrderTemplateGameTests() {
    }

    /** A template with fixed rewards lists exactly those; one naming a shipped objective rolls its job and the default table. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void templatesListTheirRewards(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        WorkOrderTemplate fixed = template("fixed", """
                {"objective": "rngtech:crusher_any", "tier": 2, "name": "Quarry Contract",
                 "rewards": [{"item": "rngtech:chaos_crystal", "count": 7}]}""");
        WorkOrder order = WorkOrderJobs.roll(helper.getLevel(), pos, fixed, 5, new SplittableRandom(3L));
        check(order != null && order.tier() == 2 && order.family().equals("crusher") && order.template().equals("rngtech:fixed"),
                "The template did not roll its job: " + order);
        List<ItemStack> rewards = WorkOrderRewards.items(order.rewards());
        check(rewards.size() == 1 && rewards.getFirst().is(ModItems.CHAOS_CRYSTAL.get()) && rewards.getFirst().getCount() == 7,
                "The template did not list its fixed reward: " + rewards);
        ItemStack chip = JobChipItem.of(ModItems.JOB_CHIP.get(), order, MachineTraits.EMPTY);
        check(chip.getHoverName().getContents() instanceof TranslatableContents name && name.getKey().equals("item.rngtech.job_chip.named")
                && name.getArgs()[0] instanceof Component shown && shown.getString().equals("Quarry Contract"),
                "The chip did not take the template's name: " + chip.getHoverName());

        WorkOrder rolled = WorkOrderJobs.roll(helper.getLevel(), pos, template("rolled", "{\"objective\": \"rngtech:furnace_stage\"}"), 3, new SplittableRandom(4L));
        check(rolled != null && rolled.tier() == 3 && WorkOrderRewards.items(rolled.rewards()).size() == 1,
                "A template without fixed rewards did not roll one reward kind from the default table: " + rolled);
        helper.succeed();
    }

    /** A template's reward table replaces the default one: a table that is not loaded pays nothing. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void templateRewardTablesReplaceTheDefault(GameTestHelper helper) {
        WorkOrderTemplate missing = template("missing_table", "{\"objective\": \"rngtech:crusher_any\", \"reward_table\": \"rngtech:no_such_table\"}");
        WorkOrder order = WorkOrderJobs.roll(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), missing, 1, new SplittableRandom(5L));
        check(order != null && order.rewardTable().equals("rngtech:no_such_table") && WorkOrderRewards.items(order.rewards()).isEmpty(),
                "The template did not roll its own reward table: " + order);
        helper.succeed();
    }

    private static WorkOrderTemplate template(String path, String json) {
        return WorkOrderTemplate.parse(RNGTech.id(path), JsonParser.parseString(json).getAsJsonObject());
    }
}
