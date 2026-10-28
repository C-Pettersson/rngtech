package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.blockentity.CompressorTankBlockEntity;
import com.rngtech.content.energy.BatteryCellMaterial;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.TankContents;
import com.rngtech.content.machine.CompressorTankMaterial;
import com.rngtech.content.machine.ServoMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** A broken Fluid or Compressor Tank keeps its fluid on its item, and placing the item puts back exactly that fluid. */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TankContentsGameTests {
    private static final BlockPos TANK = new BlockPos(2, 1, 2);

    private TankContentsGameTests() {
    }

    /** Breaking a running Compressor Tank drops its Gear once and keeps loose and compressed fluid on the tank item. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = 60)
    public static void compressorTankKeepsFluidWhenBroken(GameTestHelper helper) {
        CompressorTankBlockEntity tank = tank(helper, CompressorTankMaterial.STEEL);
        tank.getGearInventory().setStackInSlot(CompressorTankBlockEntity.SLOT_BATTERY_CELL, chargedCell());
        tank.getGearInventory().setStackInSlot(CompressorTankBlockEntity.SLOT_SERVO_0, new ItemStack(ModItems.servo(ServoMaterial.STEEL).get()));
        tank.getFluidHandler(null).fill(new FluidStack(Fluids.WATER, 8 * FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
        helper.runAfterDelay(20, () -> {
            FluidStack loose = tank.getLooseTank().getFluid().copy();
            FluidStack compressed = tank.getCompressedFluid();
            check(!loose.isEmpty() && !compressed.isEmpty(), "The tank should hold loose and compressed water: " + loose + ", " + compressed);

            BlockPos pos = helper.absolutePos(TANK);
            helper.getLevel().destroyBlock(pos, true);
            List<ItemStack> drops = collectDrops(helper, pos);
            ItemStack item = only(drops, ModItems.compressorTank(CompressorTankMaterial.STEEL).get().getDefaultInstance());
            only(drops, new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get()));
            only(drops, new ItemStack(ModItems.servo(ServoMaterial.STEEL).get()));
            check(drops.size() == 3, "The tank should drop itself, its cell and its Servo only: " + drops);
            check(TankContents.of(loose, compressed).equals(item.get(ModDataComponents.TANK_CONTENTS.get())),
                    "The tank item should carry its fluid: " + item.getComponents());

            CompressorTankBlockEntity placed = placeFromItem(helper, item);
            checkHolds(placed, loose, compressed);
            check(placed.getGearInventory().getStackInSlot(CompressorTankBlockEntity.SLOT_BATTERY_CELL).isEmpty(),
                    "The placed tank should not get its Gear back from the item");
            helper.succeed();
        });
    }

    /** A tank caught in a decaying explosion still drops itself with its fluid. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void fluidTankKeepsFluidInAnExplosion(GameTestHelper helper) {
        CompressorTankBlockEntity tank = tank(helper, CompressorTankMaterial.IRON);
        tank.getFluidHandler(null).fill(new FluidStack(Fluids.LAVA, 5_250), IFluidHandler.FluidAction.EXECUTE);
        FluidStack loose = tank.getLooseTank().getFluid().copy();
        BlockPos pos = helper.absolutePos(TANK);
        Vec3 center = Vec3.atCenterOf(pos);
        Explosion explosion = new Explosion(helper.getLevel(), null, center.x, center.y, center.z, 16.0F, false,
                Explosion.BlockInteraction.DESTROY_WITH_DECAY);
        List<ItemStack> drops = new ArrayList<>();
        helper.getBlockState(TANK).onExplosionHit(helper.getLevel(), pos, explosion, (stack, at) -> drops.add(stack));
        check(helper.getBlockState(TANK).isAir(), "The explosion should remove the tank");
        ItemStack item = only(drops, ModItems.compressorTank(CompressorTankMaterial.IRON).get().getDefaultInstance());
        check(TankContents.of(loose, FluidStack.EMPTY).equals(item.get(ModDataComponents.TANK_CONTENTS.get())),
                "The exploded tank should carry its lava: " + item.getComponents());
        checkHolds(placeFromItem(helper, item), loose, FluidStack.EMPTY);
        helper.succeed();
    }

    /** A tank item holding only compressed fluid places with an empty loose tank and the same compressed fluid. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void compressedOnlyItemRestores(GameTestHelper helper) {
        FluidStack compressed = new FluidStack(Fluids.WATER, 123_456);
        ItemStack item = ModItems.compressorTank(CompressorTankMaterial.STEEL).get().getDefaultInstance();
        item.set(ModDataComponents.TANK_CONTENTS.get(), TankContents.of(FluidStack.EMPTY, compressed));
        checkHolds(placeFromItem(helper, item), FluidStack.EMPTY, compressed);
        helper.succeed();
    }

    /** An empty tank's item has no contents, so it matches a freshly crafted one. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void emptyTankDropsPlainItem(GameTestHelper helper) {
        tank(helper, CompressorTankMaterial.COPPER);
        BlockPos pos = helper.absolutePos(TANK);
        helper.getLevel().destroyBlock(pos, true);
        ItemStack item = only(collectDrops(helper, pos), ModItems.compressorTank(CompressorTankMaterial.COPPER).get().getDefaultInstance());
        check(!item.has(ModDataComponents.TANK_CONTENTS.get()), "An empty tank should drop without contents: " + item.getComponents());
        helper.succeed();
    }

    /** A creative pick keeps the fluid on the item component only, so placing the copy restores it once. */
    @GameTest(template = MachineTestSupport.EMPTY)
    public static void creativePickCopiesFluidOnce(GameTestHelper helper) {
        CompressorTankBlockEntity tank = tank(helper, CompressorTankMaterial.BRONZE);
        tank.getFluidHandler(null).fill(new FluidStack(Fluids.WATER, 3_000), IFluidHandler.FluidAction.EXECUTE);
        FluidStack loose = tank.getLooseTank().getFluid().copy();
        ItemStack plain = helper.getBlockState(TANK).getCloneItemStack(
                new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(TANK)), Direction.UP, helper.absolutePos(TANK), false),
                helper.getLevel(), helper.absolutePos(TANK), helper.makeMockPlayer(GameType.CREATIVE));
        check(!plain.has(ModDataComponents.TANK_CONTENTS.get()), "A plain pick should give an empty tank: " + plain.getComponents());

        ItemStack picked = plain.copy();
        CompoundTag data = tank.saveCustomAndMetadata(helper.getLevel().registryAccess());
        tank.removeComponentsFromTag(data);
        check(!data.contains("LooseTank") && !data.contains("CompressedFluid"), "The picked block data should not repeat the fluid: " + data);
        BlockItem.setBlockEntityData(picked, tank.getType(), data);
        picked.applyComponents(tank.collectComponents());
        helper.setBlock(TANK, Blocks.AIR);
        checkHolds(placeFromItem(helper, picked), loose, FluidStack.EMPTY);
        helper.succeed();
    }

    private static CompressorTankBlockEntity tank(GameTestHelper helper, CompressorTankMaterial material) {
        return MachineTestSupport.place(helper, TANK, ModBlocks.compressorTank(material).get(), CompressorTankBlockEntity.class);
    }

    private static ItemStack chargedCell() {
        ItemStack cell = new ItemStack(ModItems.batteryCell(BatteryCellMaterial.IRON).get());
        int capacity = BatteryCellItem.energyCapacity(cell);
        BatteryCellItem.setEnergy(cell, capacity, capacity);
        return cell;
    }

    /** Picks up and removes every item entity dropped around {@code pos}. */
    private static List<ItemStack> collectDrops(GameTestHelper helper, BlockPos pos) {
        List<ItemStack> drops = new ArrayList<>();
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D))) {
            drops.add(entity.getItem().copy());
            entity.remove(Entity.RemovalReason.DISCARDED);
        }
        return drops;
    }

    private static ItemStack only(List<ItemStack> drops, ItemStack kind) {
        List<ItemStack> matching = drops.stream().filter(stack -> stack.is(kind.getItem())).toList();
        check(matching.size() == 1 && matching.getFirst().getCount() == 1, "Expected one " + kind + " in " + drops);
        return matching.getFirst();
    }

    /** Places {@code item} the way a survival player does and returns the new tank. */
    private static CompressorTankBlockEntity placeFromItem(GameTestHelper helper, ItemStack item) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        BlockPos pos = helper.absolutePos(TANK);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        InteractionResult result = ((BlockItem) item.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, item, hit));
        check(result.consumesAction(), "The tank item should place, was " + result);
        check(item.isEmpty(), "Placing should use up the tank item");
        if (!(helper.getBlockEntity(TANK) instanceof CompressorTankBlockEntity placed)) {
            throw new IllegalStateException("No tank was placed at " + TANK);
        }
        return placed;
    }

    private static void checkHolds(CompressorTankBlockEntity tank, FluidStack loose, FluidStack compressed) {
        check(FluidStack.matches(tank.getLooseTank().getFluid(), loose), "Loose fluid should be " + loose + ", was " + tank.getLooseTank().getFluid());
        check(FluidStack.matches(tank.getCompressedFluid(), compressed), "Compressed fluid should be " + compressed + ", was " + tank.getCompressedFluid());
    }
}
