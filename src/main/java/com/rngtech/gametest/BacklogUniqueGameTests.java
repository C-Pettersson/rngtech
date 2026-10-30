package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.ComponentRecyclerBlockEntity;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.recycling.ComponentRecyclerChassis;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.rpg.MachineStat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Trial Vault Tempered Pawl installs in a Stage 3 Component Recycler, and the Shipwreck Salt-Zinc Anode in an early Crusher. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BacklogUniqueGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private BacklogUniqueGameTests() {
    }

    /** A Bronze Recycler takes the Pawl and runs at Processing Level 4; a Copper Recycler refuses it. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void temperedPawlReachesStageFourInBronzeRecycler(GameTestHelper helper) {
        ComponentRecyclerBlockEntity copper = MachineTestSupport.place(helper, MACHINE.east(2),
                ModBlocks.componentRecycler(ComponentRecyclerChassis.COPPER).get(), ComponentRecyclerBlockEntity.class);
        check(!copper.isDisassemblyHead(unique("trial_vault_tempered_pawl")), "A Stage 2 Recycler should refuse the Stage 3 Pawl");

        ComponentRecyclerBlockEntity recycler = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.componentRecycler(ComponentRecyclerChassis.BRONZE).get(), ComponentRecyclerBlockEntity.class);
        ItemStack rest = recycler.getGearInventory().insertItem(ComponentRecyclerBlockEntity.SLOT_DISASSEMBLY_HEAD,
                unique("trial_vault_tempered_pawl"), false);
        check(rest.isEmpty(), "A Bronze Recycler should take the Pawl in its head slot");
        int level = recycler.effectiveStats().intValue(MachineStat.PROCESSING_LEVEL);
        check(level == 4, "The Pawl should run the Recycler at Processing Level 4, was " + level);
        helper.succeed();
    }

    /** A Copper Crusher takes the Stage 1 Anode as its Battery Cell. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void saltZincAnodeFitsCopperCrusher(GameTestHelper helper) {
        CrusherBlockEntity crusher = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.crusherChassis(CrusherChassisMaterial.COPPER).get(), CrusherBlockEntity.class);
        check(crusher.canInstallBatteryCell(unique("shipwreck_salt_zinc_anode")), "A Copper Crusher should take the Anode");
        helper.succeed();
    }

    private static ItemStack unique(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(RNGTech.id(id)));
    }
}
