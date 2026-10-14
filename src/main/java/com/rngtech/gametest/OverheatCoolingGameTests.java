package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Consumer;

/**
 * Heat left over from a hotter recipe cools to the new recipe's overheat line before work starts, instead of failing
 * every job while the machine keeps that heat.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OverheatCoolingGameTests {
    private static final int LEFTOVER_HEAT = 1750;
    /** Iron plate safe maximum 1100, raised 10% by the Steel Servo's Overheat Tolerance. */
    private static final int IRON_PLATE_OVERHEAT = 1210;
    /** Lead's default safe maximum is 115% of its 1100 target; nothing raises Overheat Tolerance. */
    private static final int LEAD_OVERHEAT = 1265;

    private OverheatCoolingGameTests() {
    }

    /** A Steel press left at tungsten heat cools before pressing iron, so no plate fails. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 1200)
    public static void steelPressCoolsLeftoverHeatBeforePressing(GameTestHelper helper) {
        MetalPressBlockEntity press = MachineTestSupport.place(helper, new BlockPos(2, 1, 2), ModBlocks.METAL_PRESS.get(), MetalPressBlockEntity.class);
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_SERVO, new ItemStack(ModItems.servo(ServoMaterial.STEEL).get()));
        press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_MOLD, new ItemStack(ModItems.PLATE_MOLD.get()));
        press.getProcessInventory().setStackInSlot(MetalPressBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT, 2));
        withSaved(helper, press, tag -> tag.putInt("CurrentTemperature", LEFTOVER_HEAT));
        MachineTestSupport.powerEachTick(helper, press.getEnergyStorage(null));

        helper.onEachTick(() -> {
            ItemStack output = press.getProcessInventory().getStackInSlot(MetalPressBlockEntity.SLOT_OUTPUT);
            check(!output.is(ModItems.MALFORMED_INGOT.get()), "The press failed a plate at leftover heat; " + MachineTestSupport.describe(press));
            check(press.machineInfo().progress() == 0 || press.machineInfo().heat() <= IRON_PLATE_OVERHEAT,
                    "The press worked above the iron overheat line; " + MachineTestSupport.describe(press));
        });
        helper.succeedWhen(() -> check(press.getProcessInventory().getStackInSlot(MetalPressBlockEntity.SLOT_OUTPUT).is(item("iron_plate")),
                "No iron plate yet; " + MachineTestSupport.describe(press)));
    }

    /** A Stage 6 Furnace lane left at tungsten heat cools before smelting lead, so no ingot fails. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 1200)
    public static void furnaceLaneCoolsLeftoverHeatBeforeSmelting(GameTestHelper helper) {
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, new BlockPos(2, 1, 2),
                ModBlocks.furnaceChassis(FurnaceChassisMaterial.TITANIUM).get(), FurnaceBlockEntity.class);
        furnace.getGearInventory().setStackInSlot(0, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        furnace.getInventory().setStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START, new ItemStack(item("lead_dust"), 2));
        withSaved(helper, furnace, tag -> {
            int[] temperatures = tag.getIntArray("CurrentTemperatures");
            temperatures[0] = LEFTOVER_HEAT;
            tag.putIntArray("CurrentTemperatures", temperatures);
        });
        MachineTestSupport.powerEachTick(helper, furnace.getEnergyStorage(null));

        IItemHandler inventory = furnace.getInventory();
        helper.onEachTick(() -> {
            ItemStack output = inventory.getStackInSlot(FurnaceBlockEntity.SLOT_OUTPUT_START);
            check(!output.is(ModItems.MALFORMED_INGOT.get()), "The furnace failed a smelt at leftover heat; " + MachineTestSupport.describe(furnace));
            check(furnace.machineInfo().progress() == 0 || furnace.machineInfo().heat() <= LEAD_OVERHEAT,
                    "The furnace smelted above the lead overheat line; " + MachineTestSupport.describe(furnace));
        });
        helper.succeedWhen(() -> check(inventory.getStackInSlot(FurnaceBlockEntity.SLOT_OUTPUT_START).is(item("lead_ingot")),
                "No lead ingot yet; " + MachineTestSupport.describe(furnace)));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, id));
    }

    private static void withSaved(GameTestHelper helper, BlockEntity machine, Consumer<CompoundTag> edit) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag tag = machine.saveWithFullMetadata(registries);
        edit.accept(tag);
        machine.loadWithComponents(tag, registries);
    }
}
