package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.AlloyFurnaceBlockEntity;
import com.rngtech.content.blockentity.FurnaceBlockEntity;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.AlloyCrucibleMaterial;
import com.rngtech.content.machine.AlloyFurnaceChassisMaterial;
import com.rngtech.content.machine.FurnaceChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;
import com.rngtech.rpg.progression.MachineMasteryFamily;
import com.rngtech.rpg.progression.MachineProgressionState;
import com.rngtech.rpg.progression.MegaPassiveNode;
import com.rngtech.rpg.progression.MegaPassiveTree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/** Furnace lanes share one Heat Core and batch items from their input stack; the Alloy Furnace batches whole recipe sets. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FurnaceBatchGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    /** Batch Size 4: the base 1 plus 3. */
    private static final MachineTraits BATCH_OF_FOUR = new MachineTraits(Rarity.MAGIC, 0, List.of(
            new MachineModifier(ModifierSlot.PREFIX, MachineStat.BATCH_SIZE, ModifierOperation.ADD, 3.0)));

    private FurnaceBatchGameTests() {
    }

    /** One Heat Core in the Lead Furnace's only Gear slot heats all four lanes. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 3000)
    public static void leadFurnaceRunsEveryLaneOnOneHeatCore(GameTestHelper helper) {
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.furnaceChassis(FurnaceChassisMaterial.LEAD).get(), FurnaceBlockEntity.class);
        check(furnace.getGearInventory().getSlots() == 1, "The Lead Furnace should have one Gear slot");
        furnace.getGearInventory().setStackInSlot(0, new ItemStack(ModItems.heatCore(HeatCoreMaterial.STEEL).get()));
        for (int lane = 0; lane < 4; lane++) {
            furnace.getInventory().setStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START + lane, new ItemStack(Items.POTATO));
        }
        MachineTestSupport.powerEachTick(helper, furnace.getEnergyStorage(null));

        helper.succeedWhen(() -> {
            for (int lane = 0; lane < 4; lane++) {
                check(furnace.getInventory().getStackInSlot(FurnaceBlockEntity.SLOT_OUTPUT_START + lane).is(Items.BAKED_POTATO),
                        "Lane " + (lane + 1) + " has not smelted yet; " + MachineTestSupport.describe(furnace));
            }
        });
    }

    /** A lane locks a batch of four from its stack and finishes all four in one cycle. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 3000)
    public static void furnaceLaneSmeltsAWholeBatchAtOnce(GameTestHelper helper) {
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.furnaceChassis(FurnaceChassisMaterial.STEEL).get(), FurnaceBlockEntity.class);
        furnace.setMachineTraits(BATCH_OF_FOUR);
        furnace.getGearInventory().setStackInSlot(0, new ItemStack(ModItems.heatCore(HeatCoreMaterial.STEEL).get()));
        furnace.getInventory().setStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START, new ItemStack(Items.POTATO, 6));
        MachineTestSupport.powerEachTick(helper, furnace.getEnergyStorage(null));

        ItemStackHandler inventory = furnace.getInventory();
        helper.onEachTick(() -> {
            int input = inventory.getStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START).getCount();
            int output = inventory.getStackInSlot(FurnaceBlockEntity.SLOT_OUTPUT_START).getCount();
            check(input + output == 6, "Items went missing: " + input + " in, " + output + " out");
            check(output == 0 || output == 4 || output == 6, "The lane smelted " + output + " items, not whole batches");
        });
        // Four, then the two left over.
        helper.succeedWhen(() -> check(inventory.getStackInSlot(FurnaceBlockEntity.SLOT_OUTPUT_START).getCount() == 6,
                "The batches have not finished; " + MachineTestSupport.describe(furnace)));
    }

    /** Dense Batching (+3 Batch Size) holds three potatoes until a fourth arrives, then smelts all four at once. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 3000)
    public static void denseBatchingWaitsForAFullBatch(GameTestHelper helper) {
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.furnaceChassis(FurnaceChassisMaterial.STEEL).get(), FurnaceBlockEntity.class);
        String start = MachineMasteryFamily.FURNACE.startNodeId();
        List<String> path = MegaPassiveTree.allocationPath(node -> node.id().equals(start), MegaPassiveTree.node("dense_batching"))
                .stream().map(MegaPassiveNode::id).toList();
        check(!path.isEmpty(), "Dense Batching is unreachable from the Furnace start");
        furnace.setMachineProgression(new MachineProgressionState(MachineProgressionState.xpForLevel(MachineProgressionState.MAX_LEVEL), 0,
                MachineProgressionState.MAX_LEVEL, path, start, List.of(), false));
        furnace.getGearInventory().setStackInSlot(0, new ItemStack(ModItems.heatCore(HeatCoreMaterial.STEEL).get()));
        ItemStackHandler inventory = furnace.getInventory();
        inventory.setStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START, new ItemStack(Items.POTATO, 3));
        MachineTestSupport.powerEachTick(helper, furnace.getEnergyStorage(null));

        helper.runAfterDelay(400, () -> {
            check(inventory.getStackInSlot(FurnaceBlockEntity.SLOT_OUTPUT_START).isEmpty(),
                    "A short batch started; " + MachineTestSupport.describe(furnace));
            check(furnace.machineInfo().progress() == 0, "A short batch made progress; " + MachineTestSupport.describe(furnace));
            inventory.setStackInSlot(FurnaceBlockEntity.SLOT_INPUT_START, new ItemStack(Items.POTATO, 4));
        });
        helper.onEachTick(() -> {
            int output = inventory.getStackInSlot(FurnaceBlockEntity.SLOT_OUTPUT_START).getCount();
            check(output == 0 || output == 4, "The lane smelted " + output + " items, not one full batch");
        });
        helper.succeedWhen(() -> check(inventory.getStackInSlot(FurnaceBlockEntity.SLOT_OUTPUT_START).getCount() == 4,
                "The full batch has not finished; " + MachineTestSupport.describe(furnace)));
    }

    /** A batch takes only whole recipe sets: with ingredients for two bronze crafts and one spare copper, it crafts two. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 3000)
    public static void alloyFurnaceBatchesWholeRecipeSets(GameTestHelper helper) {
        AlloyFurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.alloyFurnaceChassis(AlloyFurnaceChassisMaterial.TITANIUM).get(), AlloyFurnaceBlockEntity.class);
        furnace.setMachineTraits(BATCH_OF_FOUR);
        ItemStackHandler gear = furnace.getGearInventory();
        gear.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.TITANIUM).get()));
        gear.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_CRUCIBLE, new ItemStack(ModItems.alloyCrucible(AlloyCrucibleMaterial.TITANIUM).get()));
        ItemStackHandler process = furnace.getProcessInventory();
        process.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START, new ItemStack(Items.COPPER_INGOT, 7));
        process.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START + 1, new ItemStack(item("tin_ingot"), 2));
        process.setStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START + 2, new ItemStack(Items.CHARCOAL, 2));
        MachineTestSupport.powerEachTick(helper, furnace.getEnergyStorage(null));

        helper.onEachTick(() -> {
            int output = process.getStackInSlot(AlloyFurnaceBlockEntity.SLOT_OUTPUT).getCount();
            check(output == 0 || output == 8, "The furnace made " + output + " bronze, not one batch of two crafts");
        });
        helper.succeedWhen(() -> {
            check(process.getStackInSlot(AlloyFurnaceBlockEntity.SLOT_OUTPUT).is(item("bronze_ingot")), "No bronze yet");
            check(process.getStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START).getCount() == 1,
                    "The spare copper ingot should stay; found " + process.getStackInSlot(AlloyFurnaceBlockEntity.SLOT_INPUT_START));
        });
    }

    /** A Lead Furnace saved with four Heat Cores keeps one and moves the other three to a chest beside it. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 200)
    public static void legacyLeadFurnaceMovesExtraHeatCoresOut(GameTestHelper helper) {
        FurnaceBlockEntity furnace = MachineTestSupport.place(helper, MACHINE,
                ModBlocks.furnaceChassis(FurnaceChassisMaterial.LEAD).get(), FurnaceBlockEntity.class);
        helper.setBlock(MACHINE.east(), Blocks.CHEST);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        ItemStackHandler legacyGear = new ItemStackHandler(4);
        for (int slot = 0; slot < 4; slot++) {
            legacyGear.setStackInSlot(slot, new ItemStack(ModItems.heatCore(HeatCoreMaterial.STEEL).get()));
        }
        CompoundTag tag = furnace.saveWithFullMetadata(registries);
        tag.put("GearInventory", legacyGear.serializeNBT(registries));
        furnace.loadWithComponents(tag, registries);

        check(furnace.getGearInventory().getStackInSlot(0).is(ModItems.heatCore(HeatCoreMaterial.STEEL).get()),
                "The first Heat Core should keep the Gear slot");
        Container chest = (Container) helper.getBlockEntity(MACHINE.east());
        helper.succeedWhen(() -> {
            int moved = 0;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                moved += chest.getItem(slot).getCount();
            }
            check(moved == 3, "Expected 3 Heat Cores in the chest, found " + moved);
        });
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RNGTech.MOD_ID, id));
    }
}
