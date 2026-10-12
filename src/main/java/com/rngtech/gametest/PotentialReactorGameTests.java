package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.PotentialReactorBlockEntity;
import com.rngtech.content.blockentity.PotentialReactorVisualState;
import com.rngtech.content.energy.ContainmentLiningMaterial;
import com.rngtech.content.energy.ReactorChamberMaterial;
import com.rngtech.content.energy.RecoveryFilterMaterial;
import com.rngtech.content.recycling.RecyclingData;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** What a Potential Reactor shows: its installed Gear, a reacting or banked core, what is burning, and its residue. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PotentialReactorGameTests {
    private static final BlockPos REACTOR = new BlockPos(2, 1, 2);

    private PotentialReactorGameTests() {
    }

    /** Each installed part shows by material, and salvage fuel burns as a reacting violet core. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 100)
    public static void salvageFuelShowsInstalledGearAndAReactingCore(GameTestHelper helper) {
        PotentialReactorBlockEntity reactor = reactor(helper, ReactorChamberMaterial.STEEL);
        reactor.getGearInventory().setStackInSlot(PotentialReactorBlockEntity.SLOT_RECOVERY_FILTER,
                new ItemStack(ModItems.recoveryFilter(RecoveryFilterMaterial.COPPER).get()));
        reactor.getGearInventory().setStackInSlot(PotentialReactorBlockEntity.SLOT_CONTAINMENT_LINING,
                new ItemStack(ModItems.containmentLining(ContainmentLiningMaterial.BRONZE).get()));
        PotentialReactorVisualState idle = reactor.currentVisualState();
        check(idle.core() == PotentialReactorVisualState.CORE_OFF, "An unfuelled reactor should show no core, was " + idle);
        reactor.getProcessInventory().setStackInSlot(PotentialReactorBlockEntity.SLOT_INPUT, new ItemStack(item("broken_circuit"), 4));
        helper.runAfterDelay(10, () -> {
            PotentialReactorVisualState state = reactor.currentVisualState();
            check(state.chamber().equals("steel") && state.filter().equals("copper") && state.lining().equals("bronze"),
                    "Installed Gear should show by material: " + state);
            check(state.core() == PotentialReactorVisualState.CORE_REACTING, "Burning fuel should react: " + state);
            check(state.burn() == PotentialReactorVisualState.BURN_SALVAGE, "Salvage fuel should burn as salvage: " + state);
            check(state.intensity() > 0.0F && state.charge() > 0.0F, "A reacting core should show intensity and charge: " + state);
            helper.succeed();
        });
    }

    /** With nowhere to send its FE the buffer fills, and the core banks while the run waits. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void fullBufferBanksTheCore(GameTestHelper helper) {
        PotentialReactorBlockEntity reactor = reactor(helper, ReactorChamberMaterial.STEEL);
        reactor.getProcessInventory().setStackInSlot(PotentialReactorBlockEntity.SLOT_INPUT, new ItemStack(item("spent_catalyst"), 2));
        helper.onEachTick(() -> {
            PotentialReactorVisualState state = reactor.currentVisualState();
            if (state.charge() < 1.0F) {
                return;
            }
            check(state.core() == PotentialReactorVisualState.CORE_HELD, "A full buffer should bank the core: " + state);
            check(helper.getBlockState(REACTOR).getValue(BaseMachineBlock.ACTIVE), "The paused run should keep the reactor active");
            helper.succeed();
        });
    }

    /** Recycled gear burns in its rarity colour, and the stripped item fills the residue bin until it is taken out. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 400)
    public static void recycledGearBurnsInItsRarityAndFillsTheBin(GameTestHelper helper) {
        PotentialReactorBlockEntity reactor = reactor(helper, ReactorChamberMaterial.IRON);
        ItemStack gear = new ItemStack(ModItems.containmentLining(ContainmentLiningMaterial.COPPER).get());
        gear.set(ModDataComponents.MACHINE_TRAITS.get(), new MachineTraits(Rarity.RARE, 3, List.of(), List.of()));
        check(RecyclingData.hasRecyclableRpgValue(gear), "The test gear should be recyclable");
        reactor.getProcessInventory().setStackInSlot(PotentialReactorBlockEntity.SLOT_INPUT, gear);
        boolean[] sawRareCore = {false};
        helper.onEachTick(() -> {
            PotentialReactorVisualState state = reactor.currentVisualState();
            if (state.core() == PotentialReactorVisualState.CORE_REACTING) {
                check(state.burn() == Rarity.RARE.ordinal(), "Recycled gear should burn in its rarity: " + state);
                sawRareCore[0] = true;
                return;
            }
            if (!sawRareCore[0] || state.residue() < 1.0F) {
                return;
            }
            ItemStack residue = reactor.getProcessInventory().getStackInSlot(PotentialReactorBlockEntity.SLOT_RESIDUE);
            check(RecyclingData.isStripped(residue), "The residue should be the stripped item, was " + residue);
            check(state.core() == PotentialReactorVisualState.CORE_OFF, "The core should go out after the run: " + state);
            helper.succeed();
        });
    }

    private static PotentialReactorBlockEntity reactor(GameTestHelper helper, ReactorChamberMaterial chamber) {
        PotentialReactorBlockEntity reactor = MachineTestSupport.place(
                helper, REACTOR, ModBlocks.POTENTIAL_REACTOR.get(), PotentialReactorBlockEntity.class);
        reactor.getGearInventory().setStackInSlot(PotentialReactorBlockEntity.SLOT_REACTOR_CHAMBER,
                new ItemStack(ModItems.reactorChamber(chamber).get()));
        return reactor;
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, path));
    }
}
