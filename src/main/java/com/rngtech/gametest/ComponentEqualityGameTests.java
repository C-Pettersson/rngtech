package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.calibration.CalibrationStreak;
import com.rngtech.content.configurator.ConnectorSetting;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.itemfilter.AdvancedItemFilterSettings;
import com.rngtech.content.minerscompanion.MinersCompanionState;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.tool.FieldToolAssembly;
import com.rngtech.content.tool.ToolHeadFamily;
import com.rngtech.content.tool.ToolHeadMaterial;
import com.rngtech.content.tool.ToolRodMaterial;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.OutputAmountTracker;
import com.rngtech.rpg.Rarity;

import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Components that hold ItemStacks compare by value, so a stack equals its own saved or synced copy. Storage grids, stacking
 * and filters match stacks by their components and fail on a component that only equals itself.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ComponentEqualityGameTests {
    private ComponentEqualityGameTests() {
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void modularToolEqualsItsSavedAndSyncedCopy(GameTestHelper helper) {
        ItemStack head = new ItemStack(ModItems.toolHead(ToolHeadFamily.HAMMER, ToolHeadMaterial.FLINT).get());
        head.set(ModDataComponents.MACHINE_TRAITS.get(), new MachineTraits(Rarity.MAGIC, 1, List.of(
                new MachineModifier(ModifierSlot.PREFIX, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, 10)
        )));
        ItemStack hammer = new ItemStack(ModItems.modularTool(ToolHeadFamily.HAMMER).get());
        hammer.set(ModDataComponents.FIELD_TOOL_ASSEMBLY.get(), new FieldToolAssembly(
                head,
                new ItemStack(ModItems.toolRod(ToolRodMaterial.WOODEN).get()),
                ItemStack.EMPTY
        ));

        ItemStack saved = saveAndLoad(helper, hammer);
        check(ItemStack.isSameItemSameComponents(hammer, saved), "A Modular Tool does not match its saved copy");
        check(ItemStack.hashItemAndComponents(hammer) == ItemStack.hashItemAndComponents(saved),
                "A Modular Tool hashes differently from its saved copy");

        ItemStack synced = sync(helper, hammer);
        check(ItemStack.isSameItemSameComponents(hammer, synced), "A Modular Tool does not match its synced copy");
        check(ItemStack.isSameItemSameComponents(hammer, hammer.copy()), "A Modular Tool does not match its copy");

        ItemStack otherHead = hammer.copy();
        otherHead.set(ModDataComponents.FIELD_TOOL_ASSEMBLY.get(), new FieldToolAssembly(
                new ItemStack(ModItems.toolHead(ToolHeadFamily.HAMMER, ToolHeadMaterial.FLINT).get()),
                new ItemStack(ModItems.toolRod(ToolRodMaterial.WOODEN).get()),
                ItemStack.EMPTY
        ));
        check(!ItemStack.isSameItemSameComponents(hammer, otherHead), "Modular Tools with different heads still match");
        helper.succeed();
    }

    @GameTest(template = MachineTestSupport.EMPTY)
    public static void stackHoldingComponentsEqualTheirSavedCopy(GameTestHelper helper) {
        ItemStack cell = new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get());
        ItemStack sample = new ItemStack(ModItems.toolRod(ToolRodMaterial.WOODEN).get());

        checkRoundTrip(helper, CalibrationStreak.CODEC, new CalibrationStreak(3, "crusher", sample, false, 1, false));
        checkRoundTrip(helper, ConnectorSetting.CODEC, ConnectorSetting.of(2, 1, Direction.NORTH, Direction.NORTH, sample));
        checkRoundTrip(helper, MinersCompanionState.CODEC, MinersCompanionState.EMPTY.withBatteryCell(cell));
        checkRoundTrip(helper, AdvancedItemFilterSettings.CODEC, AdvancedItemFilterSettings.EMPTY.withSample(0, sample));
        checkRoundTrip(helper, OutputAmountTracker.SavedBank.LIST_CODEC, List.of(new OutputAmountTracker.SavedBank(sample, 0.5, 2)));
        helper.succeed();
    }

    private static <T> void checkRoundTrip(GameTestHelper helper, Codec<T> codec, T value) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        T loaded = codec.parse(ops, codec.encodeStart(ops, value).getOrThrow()).getOrThrow();
        check(value.equals(loaded), value.getClass().getSimpleName() + " does not equal its saved copy");
        check(value.hashCode() == loaded.hashCode(), value.getClass().getSimpleName() + " hashes differently from its saved copy");
    }

    private static ItemStack saveAndLoad(GameTestHelper helper, ItemStack stack) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        Tag saved = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        return ItemStack.CODEC.parse(ops, saved).getOrThrow();
    }

    private static ItemStack sync(GameTestHelper helper, ItemStack stack) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, stack);
            return ItemStack.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }
}
