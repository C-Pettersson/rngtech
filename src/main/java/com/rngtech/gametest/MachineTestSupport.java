package com.rngtech.gametest;

import com.rngtech.content.blockentity.MachineInfoProvider;
import com.rngtech.content.blockentity.MachineInfoSnapshot;
import com.rngtech.rpg.MachineBehavior;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.Rarity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Shared setup for RNGTech GameTests. Tests run on {@code gameTestServer} from {@code @GameTestHolder(RNGTech.MOD_ID)}
 * classes in this package, using the {@link #EMPTY} template: an empty 5x5x5 box.
 */
public final class MachineTestSupport {
    /** Use with {@code @PrefixGameTestTemplate(false)}. */
    public static final String EMPTY = "gametest_empty";
    public static final MachineTraits BULK_SPEED = new MachineTraits(Rarity.MAGIC, 0, List.of(), List.of(MachineBehavior.BULK_SPEED));

    private MachineTestSupport() {
    }

    public static <T extends BlockEntity> T place(GameTestHelper helper, BlockPos pos, Block block, Class<T> type) {
        helper.setBlock(pos, block);
        BlockEntity blockEntity = helper.getBlockEntity(pos);
        if (!type.isInstance(blockEntity)) {
            throw new GameTestAssertException("Expected " + type.getSimpleName() + " at " + pos + ", found " + blockEntity);
        }
        return type.cast(blockEntity);
    }

    /** Fills the storage at the end of every tick, so the machine never waits for power. */
    public static void powerEachTick(GameTestHelper helper, IEnergyStorage storage) {
        helper.onEachTick(() -> storage.receiveEnergy(Integer.MAX_VALUE, false));
    }

    /** A one-line machine state for failure messages. */
    public static String describe(MachineInfoProvider machine) {
        MachineInfoSnapshot info = machine.machineInfo();
        return "status " + info.statusKey() + " " + info.blockedReason().serializedName()
                + ", progress " + info.progress() + "/" + info.progressMax()
                + ", heat " + info.heat() + "/" + info.requiredHeat()
                + ", energy " + info.energy() + "/" + info.energyCapacity();
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    /**
     * Samples a processing machine once per tick and records each completed craft. Progress that falls without a craft
     * fails the test, since only a real restart may discard progress.
     */
    public static final class CraftLog {
        private final GameTestHelper helper;
        private final MachineInfoProvider machine;
        private final IntSupplier outputCount;
        private final List<Craft> crafts = new ArrayList<>();
        private int lastProgress;
        private int lastOutput;
        private long craftStart = -1L;
        private int craftTicks;

        public CraftLog(GameTestHelper helper, MachineInfoProvider machine, IntSupplier outputCount) {
            this.helper = helper;
            this.machine = machine;
            this.outputCount = outputCount;
            lastOutput = outputCount.getAsInt();
        }

        public void sample(double processingSpeed) {
            MachineInfoSnapshot info = machine.machineInfo();
            int output = outputCount.getAsInt();
            long tick = helper.getTick();
            if (output > lastOutput) {
                crafts.add(new Craft(craftStart, tick, craftTicks, processingSpeed));
                craftStart = -1L;
            } else if (info.progress() < lastProgress) {
                throw new GameTestAssertException("Progress fell from " + lastProgress + " to " + info.progress()
                        + " at tick " + tick + " without a completed craft");
            }
            if (info.progress() > 0 && craftStart < 0L) {
                craftStart = tick - (info.progress() - 1);
                craftTicks = info.progressMax();
            }
            lastProgress = info.progress();
            lastOutput = output;
        }

        public List<Craft> crafts() {
            return crafts;
        }
    }

    /**
     * One completed craft: the tick its progress started, the tick it completed, the processing ticks the machine
     * reported when it started, and the Processing Speed right after it completed.
     */
    public record Craft(long startTick, long endTick, int expectedTicks, double speedAfter) {
        public long ticks() {
            return endTick - startTick + 1;
        }
    }
}
