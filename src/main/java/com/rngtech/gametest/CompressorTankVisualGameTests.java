package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.UniversalConnectorBlock;
import com.rngtech.content.blockentity.CompressorTankBlockEntity;
import com.rngtech.content.blockentity.CompressorTankVisualState;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** The world view of a Fluid or Compressor Tank: its fluid and fills, its installed Gear, and its connector ports. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CompressorTankVisualGameTests {
    private CompressorTankVisualGameTests() {
    }

    /** A plain Fluid Tank shows its fluid and loose fill, and has no compressed store or Gear to show. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void fluidTankShowsItsFluid(GameTestHelper helper) {
        CompressorTankBlockEntity tank = tank(helper, new BlockPos(2, 1, 2), CompressorTankMaterial.IRON);
        int half = CompressorTankMaterial.IRON.looseBuckets() * FluidType.BUCKET_VOLUME / 2;
        tank.getFluidHandler(null).fill(new FluidStack(Fluids.WATER, half), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(2, () -> {
            CompressorTankVisualState state = tank.currentVisualState();
            check(state.fluid().equals("minecraft:water"), "The tank should show water: " + state);
            check(Math.abs(state.loose() - 0.5F) < 0.01F, "The tank should read half full: " + state);
            check(!state.hasCompressedStore(), "A Fluid Tank has no compressed store: " + state);
            check(state.servos().stream().allMatch(String::isEmpty) && state.cell().isEmpty(), "A Fluid Tank shows no Gear: " + state);
            check(state.connectors() == 0, "A lone tank has no ports: " + state);
            check(CompressorTankVisualState.load(state.save()).equals(state), "The visual state should survive syncing: " + state);
            helper.succeed();
        });
    }

    /** A running Compressor Tank shows its Servos and cell, and its compressed fill rises as loose fluid moves over. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 60)
    public static void compressorTankShowsGearAndCompressedFill(GameTestHelper helper) {
        CompressorTankBlockEntity tank = tank(helper, new BlockPos(2, 1, 2), CompressorTankMaterial.STEEL);
        tank.getGearInventory().setStackInSlot(CompressorTankBlockEntity.SLOT_BATTERY_CELL, chargedCell(BatteryCellMaterial.IRON));
        tank.getGearInventory().setStackInSlot(CompressorTankBlockEntity.SLOT_SERVO_0, new ItemStack(ModItems.servo(ServoMaterial.STEEL).get()));
        tank.getGearInventory().setStackInSlot(CompressorTankBlockEntity.SLOT_SERVO_2, new ItemStack(ModItems.servo(ServoMaterial.STEEL).get()));
        tank.getFluidHandler(null).fill(new FluidStack(Fluids.WATER, 8 * FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(20, () -> {
            CompressorTankVisualState state = tank.currentVisualState();
            check(state.fluid().equals("minecraft:water"), "The tank should show water: " + state);
            check(state.hasCompressedStore() && state.compressed() > 0.0F, "Compression should fill the compressed store: " + state);
            check(state.servos().equals(List.of("steel", "", "steel", "")), "Each Servo shows in its own slot: " + state);
            check(state.cell().equals("iron"), "The Iron cell should show: " + state);
            check(CompressorTankVisualState.load(state.save()).equals(state), "The visual state should survive syncing: " + state);
            helper.succeed();
        });
    }

    /** Universal Connectors facing the tank give connector ports; one facing away gives none. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void connectorsGetPorts(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        CompressorTankBlockEntity tank = tank(helper, pos, CompressorTankMaterial.COPPER);
        helper.setBlock(pos.above(), ModBlocks.UNIVERSAL_CONNECTOR.get().defaultBlockState().setValue(UniversalConnectorBlock.FACING, Direction.DOWN));
        helper.setBlock(pos.east(), ModBlocks.UNIVERSAL_CONNECTOR.get().defaultBlockState().setValue(UniversalConnectorBlock.FACING, Direction.WEST));
        helper.setBlock(pos.south(), ModBlocks.UNIVERSAL_CONNECTOR.get().defaultBlockState().setValue(UniversalConnectorBlock.FACING, Direction.SOUTH));
        helper.runAfterDelay(2, () -> {
            CompressorTankVisualState state = tank.currentVisualState();
            check(state.connector(Direction.UP), "A connector above should give a top port: " + state);
            check(state.connector(Direction.EAST), "A connector to the east should give a port: " + state);
            check(!state.connector(Direction.SOUTH), "A connector facing away gives no port: " + state);
            check(!state.connector(Direction.DOWN) && !state.connector(Direction.NORTH), "Open sides give no port: " + state);
            helper.succeed();
        });
    }

    private static CompressorTankBlockEntity tank(GameTestHelper helper, BlockPos pos, CompressorTankMaterial material) {
        return MachineTestSupport.place(helper, pos, ModBlocks.compressorTank(material).get(), CompressorTankBlockEntity.class);
    }

    private static ItemStack chargedCell(BatteryCellMaterial material) {
        ItemStack cell = new ItemStack(ModItems.batteryCell(material).get());
        int capacity = BatteryCellItem.energyCapacity(cell);
        BatteryCellItem.setEnergy(cell, capacity, capacity);
        return cell;
    }
}
