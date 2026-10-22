package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.AffixForgeBlock;
import com.rngtech.content.blockentity.AffixForgeBlockEntity;
import com.rngtech.content.blockentity.AffixForgeVisualState;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.refinement.RefinementSelection;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/** The world view of the Affix Forge: its items reach the synced visual state, upgrades its blockstate, and a Reforge steps the flash. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AffixForgeVisualGameTests {
    private static final BlockPos FORGE = new BlockPos(2, 1, 2);

    private AffixForgeVisualGameTests() {
    }

    /** An empty forge shows nothing and no upgrades; filled slots show the target, catalyst, focus, and upgrades. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void forgeShowsItsItemsAndUpgrades(GameTestHelper helper) {
        AffixForgeBlockEntity forge = place(helper);
        check(forge.currentVisualState().equals(AffixForgeVisualState.IDLE), "An empty forge shows nothing: " + forge.currentVisualState());
        ItemStackHandler inventory = forge.getInventory();
        inventory.setStackInSlot(AffixForgeBlockEntity.SLOT_TARGET, target());
        inventory.setStackInSlot(AffixForgeBlockEntity.SLOT_CONSUMABLE, new ItemStack(ModItems.AFFIX_INJECTOR.get()));
        inventory.setStackInSlot(AffixForgeBlockEntity.SLOT_MODIFIER, new ItemStack(ModItems.CONSERVATION_CRYSTAL.get()));
        inventory.setStackInSlot(AffixForgeBlockEntity.SLOT_MODIFIER_SOCKET, new ItemStack(ModItems.AFFIX_MODIFIER_SOCKET.get()));
        inventory.setStackInSlot(AffixForgeBlockEntity.SLOT_RESONANCE_MATRIX, new ItemStack(ModItems.AFFIX_RESONANCE_MATRIX.get()));

        AffixForgeVisualState state = forge.currentVisualState();
        check(state.target() == ModItems.batteryCell(BatteryCellMaterial.IRON).get(), "The plate should hold the target: " + state);
        check(state.catalyst() == ModItems.AFFIX_INJECTOR.get(), "The dish should hold the catalyst: " + state);
        check(state.focus() == ModItems.CONSERVATION_CRYSTAL.get(), "The socket should hold the crystal: " + state);
        check(AffixForgeVisualState.load(state.save()).equals(state), "The visual state should survive syncing: " + state);
        BlockState blockState = helper.getBlockState(FORGE);
        check(blockState.getValue(AffixForgeBlock.HAS_MODIFIER_SOCKET) && blockState.getValue(AffixForgeBlock.HAS_RESONANCE_MATRIX)
                && !blockState.getValue(AffixForgeBlock.HAS_LENS_ARRAY), "The blockstate should show the installed upgrades: " + blockState);
        helper.succeed();
    }

    /** A successful Reforge steps the flash counter; a refused one does not. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void reforgeStepsTheFlash(GameTestHelper helper) {
        AffixForgeBlockEntity forge = place(helper);
        ItemStackHandler inventory = forge.getInventory();
        inventory.setStackInSlot(AffixForgeBlockEntity.SLOT_TARGET, target());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        check(!forge.applyRefinement(player, RefinementSelection.none()), "Reforge without a catalyst should fail");
        check(forge.currentVisualState().reforges() == 0, "A refused Reforge should not flash: " + forge.currentVisualState());

        inventory.setStackInSlot(AffixForgeBlockEntity.SLOT_CONSUMABLE, new ItemStack(ModItems.AFFIX_INJECTOR.get()));
        check(forge.applyRefinement(player, RefinementSelection.none()), "The Affix Injector should add an affix");
        AffixForgeVisualState state = forge.currentVisualState();
        check(state.reforges() == 1 && state.catalyst() == Items.AIR, "A Reforge should flash and use the catalyst: " + state);
        helper.succeed();
    }

    private static AffixForgeBlockEntity place(GameTestHelper helper) {
        helper.setBlock(FORGE, ModBlocks.AFFIX_FORGE.get());
        AffixForgeBlockEntity forge = helper.getBlockEntity(FORGE);
        check(forge != null, "The Affix Forge should have a block entity");
        return forge;
    }

    private static ItemStack target() {
        ItemStack cell = new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get());
        cell.set(ModDataComponents.MACHINE_TRAITS.get(), new MachineTraits(Rarity.NORMAL, 20, List.of()));
        return cell;
    }
}
