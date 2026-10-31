package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.CableBlock;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.BatteryChassisBlockEntity;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.DebugBatteryBlockEntity;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.EnergyConnectorTier;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.energy.BatteryChassisMaterial;
import com.rngtech.content.menu.UniversalConnectorAccess;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Each Energy row of a Universal Connector is its own endpoint, and old single-connector saves load into row 1. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UniversalConnectorEnergyRowGameTests {
    private static final BlockPos SOURCE = new BlockPos(0, 1, 1);
    private static final BlockPos CABLE = new BlockPos(1, 1, 1);
    private static final BlockPos SINK = new BlockPos(2, 1, 1);
    private static final BlockPos STANDALONE = new BlockPos(1, 1, 3);

    private UniversalConnectorEnergyRowGameTests() {
    }

    /**
     * A Debug Battery feeds an empty Battery Chassis through one cable. The chassis side has two Basic Energy Connector
     * rows set to Out, so each row delivers up to its own 128 FE/t cap and together they deliver more than one row could.
     */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 60)
    public static void energyRowsDeliverUpToTheirOwnCaps(GameTestHelper helper) {
        DebugBatteryBlockEntity battery = MachineTestSupport.place(helper, SOURCE, ModBlocks.DEBUG_BATTERY.get(), DebugBatteryBlockEntity.class);
        battery.setOutputRate(10_000);
        BatteryChassisBlockEntity chassis = MachineTestSupport.place(
                helper, SINK, ModBlocks.batteryChassis(BatteryChassisMaterial.SPARKSTEEL).get(), BatteryChassisBlockEntity.class);
        chassis.getInventory().setStackInSlot(0, new ItemStack(ModItems.batteryCell(BatteryCellMaterial.COPPER).get()));
        chassis.getInventory().setStackInSlot(1, new ItemStack(ModItems.batteryCell(BatteryCellMaterial.COPPER).get()));

        CableBlockEntity cable = MachineTestSupport.place(helper, CABLE, ModBlocks.CABLE.get(), CableBlockEntity.class);
        UniversalConnectorAccess source = install(helper, cable, Direction.WEST);
        UniversalConnectorAccess sink = install(helper, cable, Direction.EAST);

        source.getInventory().setStackInSlot(
                UniversalConnectorBlockEntity.energyConnectorSlot(0),
                new ItemStack(ModItems.energyConnector(EnergyConnectorTier.COPPER).get())
        );
        source.setEnergyMode(0, CableConnectorMode.INPUT);
        for (int row = 0; row < 2; row++) {
            sink.getInventory().setStackInSlot(
                    UniversalConnectorBlockEntity.energyConnectorSlot(row),
                    new ItemStack(ModItems.energyConnector(EnergyConnectorTier.BASIC).get())
            );
            sink.setEnergyMode(row, CableConnectorMode.OUTPUT);
        }

        int basicCap = EnergyConnectorTier.BASIC.transferRate();
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    ContainerData data = sink.menuData();
                    check(data.get(UniversalConnectorBlockEntity.dataEnergyTargetAccessIndex(0)) != 0,
                            "The Battery Chassis has no FE on the sink rows' Attach side");
                    int first = data.get(UniversalConnectorBlockEntity.dataEnergyLastOutputIndex(0));
                    int second = data.get(UniversalConnectorBlockEntity.dataEnergyLastOutputIndex(1));
                    int third = data.get(UniversalConnectorBlockEntity.dataEnergyLastOutputIndex(2));
                    ContainerData sourceData = source.menuData();
                    check(first > 0 && second > 0, "Both Out rows should deliver, got " + first + " and " + second
                            + "; sink caps " + data.get(UniversalConnectorBlockEntity.dataEnergyTransferRateIndex(0))
                            + "/" + data.get(UniversalConnectorBlockEntity.dataEnergyTransferRateIndex(1))
                            + ", source cap " + sourceData.get(UniversalConnectorBlockEntity.dataEnergyTransferRateIndex(0))
                            + ", source in " + sourceData.get(UniversalConnectorBlockEntity.dataEnergyLastInputIndex(0))
                            + ", source access " + sourceData.get(UniversalConnectorBlockEntity.dataEnergyTargetAccessIndex(0))
                            + ", source mode " + sourceData.get(UniversalConnectorBlockEntity.dataEnergyModeIndex(0)));
                    check(first <= basicCap && second <= basicCap,
                            "A row delivered past its " + basicCap + " FE/t cap: " + first + " and " + second);
                    check(first + second > basicCap, "Two rows delivered no more than one row's cap: " + (first + second));
                    check(third == 0, "The empty third row delivered " + third + " FE");
                })
                .thenSucceed();
    }

    /** A save from before Energy rows keeps its one connector's settings at the top level; they become row 1. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 20)
    public static void singleConnectorSaveLoadsIntoTheFirstEnergyRow(GameTestHelper helper) {
        helper.setBlock(STANDALONE, ModBlocks.UNIVERSAL_CONNECTOR.get().defaultBlockState()
                .setValue(UniversalConnectorBlock.FACING, Direction.EAST));
        if (!(helper.getBlockEntity(STANDALONE) instanceof UniversalConnectorBlockEntity standalone)) {
            throw new IllegalStateException("The standalone Universal Connector has no block entity");
        }

        ItemStackHandler legacyInventory = new ItemStackHandler(UniversalConnectorBlockEntity.LEGACY_SLOT_COUNT);
        legacyInventory.setStackInSlot(0, new ItemStack(ModItems.energyConnector(EnergyConnectorTier.COPPER).get()));
        CompoundTag legacy = new CompoundTag();
        legacy.put("Inventory", legacyInventory.serializeNBT(helper.getLevel().registryAccess()));
        legacy.putInt("Channel", 5);
        legacy.putString("AttachAs", Direction.UP.getSerializedName());
        legacy.putString("Mode", CableConnectorMode.OUTPUT.getSerializedName());
        standalone.universalConnector(Direction.EAST).load(legacy, helper.getLevel().registryAccess());

        ContainerData data = standalone.menuData();
        check(data.get(UniversalConnectorBlockEntity.dataEnergyChannelIndex(0)) == 5, "Row 1 lost the saved channel");
        check(data.get(UniversalConnectorBlockEntity.dataEnergyAttachAsIndex(0)) == Direction.UP.ordinal(),
                "Row 1 lost the saved Attach side");
        check(data.get(UniversalConnectorBlockEntity.dataEnergyModeIndex(0)) == CableConnectorMode.OUTPUT.ordinal(),
                "Row 1 lost the saved mode");
        check(data.get(UniversalConnectorBlockEntity.dataEnergyTransferRateIndex(0)) == EnergyConnectorTier.COPPER.transferRate(),
                "Row 1 lost the saved Energy Connector");
        for (int row = 1; row < UniversalConnectorBlockEntity.ENERGY_MODULE_SLOT_COUNT; row++) {
            check(data.get(UniversalConnectorBlockEntity.dataEnergyTransferRateIndex(row)) == 0,
                    "Row " + (row + 1) + " gained a connector from the old save");
            check(data.get(UniversalConnectorBlockEntity.dataEnergyChannelIndex(row)) == 0,
                    "Row " + (row + 1) + " gained the old channel");
        }
        helper.succeed();
    }

    private static UniversalConnectorAccess install(GameTestHelper helper, CableBlockEntity cable, Direction face) {
        if (!cable.installUniversalConnector(face)) {
            throw new IllegalStateException("Could not install a Universal Connector on the cable's " + face + " face");
        }
        BlockPos cablePos = helper.absolutePos(CABLE);
        BlockState state = helper.getLevel().getBlockState(cablePos);
        helper.getLevel().setBlock(cablePos, state.setValue(CableBlock.connectorProperty(face), true), Block.UPDATE_ALL);
        return cable.universalConnectorAccess(face);
    }
}
