package com.rngtech.gametest;

import static com.rngtech.gametest.MachineTestSupport.check;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.BaseMachineBlockEntity;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.MachineInfoProvider;
import com.rngtech.content.blockentity.MetalPressBlockEntity;
import com.rngtech.content.energy.HeatCoreMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.registry.ModItems;
import com.rngtech.rpg.MachineModifier;
import com.rngtech.rpg.MachineStat;
import com.rngtech.rpg.MachineTraits;
import com.rngtech.rpg.ModifierOperation;
import com.rngtech.rpg.ModifierSlot;
import com.rngtech.rpg.Rarity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Runs each Machine Block powered and stocked, saves it, loads the save into a fresh block entity of the same block, and
 * ticks both. A field that is not saved, or a cache that is stale after a load, makes the two diverge.
 *
 * <p>Machines are stocked through their own item handlers: every slot that accepts items gets one, and many variants of
 * each machine take random accepted items (the seed is the variant number), because a machine only crafts when its inputs and gear match. The log
 * lists the machines that were mid-craft when they were saved.
 */
@GameTestHolder(RNGTech.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SaveLoadGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int VARIANTS = 3;
    private static final int STOCK_COUNT = 16;
    private static final int RUN_BEFORE_SAVE = 200;
    private static final int RUN_AFTER_LOAD = 80;
    private static final int SPACING = 4;
    private static final int ROW_LENGTH = 12;
    private static final Set<String> POSITION_KEYS = Set.of("x", "y", "z");
    private static final MachineTraits TRAITS = new MachineTraits(Rarity.RARE, 2, List.of(
            new MachineModifier(ModifierSlot.PREFIX, MachineStat.PROCESSING_SPEED, ModifierOperation.INCREASED_PERCENT, 40),
            new MachineModifier(ModifierSlot.SUFFIX, MachineStat.ENERGY_USAGE, ModifierOperation.DECREASED_PERCENT, 15)
    ));

    /** Setups that make a machine craft, which a random stocking rarely finds. They replace variant 0 of the machine. */
    private static final Map<String, Consumer<BlockEntity>> SCENARIOS = Map.of(
            "wooden_crusher_chassis", machine -> {
                CrusherBlockEntity crusher = (CrusherBlockEntity) machine;
                crusher.getRefinementInventory().setStackInSlot(0, ItemStack.EMPTY);
                crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_FUEL, ItemStack.EMPTY);
                crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_CRUSH_HEAD, new ItemStack(ModItems.crushHead(CrushHeadMaterial.FLINT).get()));
                crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.COAL, 32));
            },
            "crude_metal_press", machine -> {
                MetalPressBlockEntity press = (MetalPressBlockEntity) machine;
                press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_BATTERY_CELL, ItemStack.EMPTY);
                press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_HEAT_CORE, new ItemStack(ModItems.heatCore(HeatCoreMaterial.STEEL).get()));
                press.getGearInventory().setStackInSlot(MetalPressBlockEntity.SLOT_MOLD, new ItemStack(ModItems.PLATE_MOLD.get()));
                press.getProcessInventory().setStackInSlot(MetalPressBlockEntity.SLOT_INPUT, new ItemStack(Items.IRON_INGOT, 32));
            }
    );

    private SaveLoadGameTests() {
    }

    /** Every Machine Block, in several stockings, saved mid-run and loaded into a twin, ticks on to the same state. */
    @GameTest(template = MachineTestSupport.EMPTY, timeoutTicks = RUN_BEFORE_SAVE + RUN_AFTER_LOAD + 40)
    public static void everyMachineSurvivesSaveAndLoad(GameTestHelper helper) {
        List<Block> blocks = machineBlocks();
        List<Pair> pairs = new ArrayList<>();
        forceLoad(helper, blocks.size() * VARIANTS);
        int index = 0;
        for (Block block : blocks) {
            for (int variant = 0; variant < VARIANTS; variant++) {
                BlockPos original = new BlockPos((index % ROW_LENGTH) * SPACING, 1, (index / ROW_LENGTH) * SPACING * 2);
                pairs.add(new Pair(block, variant, original, original.offset(0, 0, SPACING)));
                index++;
            }
        }

        Map<Block, List<List<ItemStack>>> accepted = new HashMap<>();
        for (Pair pair : pairs) {
            helper.setBlock(pair.original(), pair.block());
            BlockEntity machine = helper.getBlockEntity(pair.original());
            if (machine instanceof BaseMachineBlockEntity base) {
                base.setMachineTraits(TRAITS);
            }
            stock(machine, pair.variant(), accepted.computeIfAbsent(pair.block(), ignored -> acceptedItems(machine)));
            Consumer<BlockEntity> scenario = SCENARIOS.get(BuiltInRegistries.BLOCK.getKey(pair.block()).getPath());
            if (scenario != null && pair.variant() == 0) {
                scenario.accept(machine);
            }
        }
        helper.onEachTick(() -> {
            for (Pair pair : pairs) {
                power(helper, pair.original());
                if (pair.loaded) {
                    power(helper, pair.copy());
                }
            }
        });
        helper.runAfterDelay(RUN_BEFORE_SAVE, () -> {
            var registries = helper.getLevel().registryAccess();
            for (Pair pair : pairs) {
                BlockEntity machine = helper.getBlockEntity(pair.original());
                pair.crafting = machine instanceof MachineInfoProvider info && info.machineInfo().progress() > 0;
                CompoundTag saved = machine.saveWithFullMetadata(registries);
                helper.setBlock(pair.copy(), helper.getBlockState(pair.original()));
                helper.getBlockEntity(pair.copy()).loadWithComponents(saved, registries);
                pair.loaded = true;
            }
        });
        helper.runAfterDelay(RUN_BEFORE_SAVE + RUN_AFTER_LOAD, () -> {
            List<String> failures = new ArrayList<>();
            Set<String> crafting = new TreeSet<>();
            for (Pair pair : pairs) {
                String name = BuiltInRegistries.BLOCK.getKey(pair.block()).getPath();
                if (pair.crafting) {
                    crafting.add(name);
                }
                String difference = difference(helper, pair);
                if (difference != null) {
                    failures.add(name + " variant " + pair.variant() + ": " + difference);
                }
            }
            LOGGER.info("Save and load: {} of {} machine blocks were mid-craft when saved: {}", crafting.size(), blocks.size(), crafting);
            check(failures.isEmpty(), failures.size() + " of " + pairs.size() + " machines differ after a save and load:\n"
                    + String.join("\n", failures));
            helper.succeed();
        });
    }

    /** Block entities only tick in loaded chunks that are being simulated, and the layout reaches beyond the test's own chunk. */
    private static void forceLoad(GameTestHelper helper, int machines) {
        ChunkPos from = new ChunkPos(helper.absolutePos(BlockPos.ZERO));
        ChunkPos to = new ChunkPos(helper.absolutePos(new BlockPos(ROW_LENGTH * SPACING, 0, (machines / ROW_LENGTH + 1) * SPACING * 2)));
        for (int x = from.x; x <= to.x; x++) {
            for (int z = from.z; z <= to.z; z++) {
                helper.getLevel().setChunkForced(x, z, true);
            }
        }
    }

    private static List<Block> machineBlocks() {
        return BuiltInRegistries.BLOCK.stream()
                .filter(block -> BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(RNGTech.MOD_ID))
                .filter(block -> block instanceof BaseMachineBlock)
                .sorted(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()))
                .toList();
    }

    /** One entry per item handler slot: the items that slot accepts, found once per block. */
    private static List<List<ItemStack>> acceptedItems(BlockEntity machine) {
        List<List<ItemStack>> slots = new ArrayList<>();
        for (ItemStackHandler handler : itemHandlers(machine)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                List<ItemStack> valid = new ArrayList<>();
                for (var item : BuiltInRegistries.ITEM) {
                    ItemStack stack = new ItemStack(item, 1);
                    if (handler.isItemValid(slot, stack)) {
                        valid.add(stack);
                    }
                }
                slots.add(valid);
            }
        }
        return slots;
    }

    /** Puts the variant's pick of the accepted items into every slot; variants spread over the accepted list. */
    private static void stock(BlockEntity machine, int variant, List<List<ItemStack>> accepted) {
        int slotIndex = 0;
        RandomSource random = RandomSource.create(variant * 31L + 7L);
        for (ItemStackHandler handler : itemHandlers(machine)) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                List<ItemStack> valid = accepted.get(slotIndex++);
                if (!valid.isEmpty()) {
                    ItemStack stack = valid.get(random.nextInt(valid.size())).copy();
                    stack.setCount(stack.getCapability(Capabilities.EnergyStorage.ITEM) != null ? 1 : Math.min(STOCK_COUNT, handler.getSlotLimit(slot)));
                    handler.setStackInSlot(slot, stack);
                }
            }
        }
    }

    /** The machine's own item handlers (process, gear and fuel inventories), found through its fields. */
    private static List<ItemStackHandler> itemHandlers(BlockEntity machine) {
        List<ItemStackHandler> handlers = new ArrayList<>();
        for (Class<?> type = machine.getClass(); type != null && type != BlockEntity.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !ItemStackHandler.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    handlers.add((ItemStackHandler) field.get(machine));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot read " + field, e);
                }
            }
        }
        return handlers;
    }

    private static void power(GameTestHelper helper, BlockPos pos) {
        IEnergyStorage energy = capability(helper, Capabilities.EnergyStorage.BLOCK, pos, null);
        for (Direction side : Direction.values()) {
            if (energy != null) {
                break;
            }
            energy = capability(helper, Capabilities.EnergyStorage.BLOCK, pos, side);
        }
        if (energy != null) {
            energy.receiveEnergy(Integer.MAX_VALUE, false);
        }
    }

    private static <T> T capability(GameTestHelper helper, BlockCapability<T, Direction> capability, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(capability, helper.absolutePos(pos), side);
    }

    /** Null when both machines saved the same data after running on, otherwise the keys that differ. */
    private static String difference(GameTestHelper helper, Pair pair) {
        var registries = helper.getLevel().registryAccess();
        CompoundTag original = helper.getBlockEntity(pair.original()).saveWithFullMetadata(registries);
        CompoundTag copy = helper.getBlockEntity(pair.copy()).saveWithFullMetadata(registries);
        Set<String> keys = new HashSet<>(original.getAllKeys());
        keys.addAll(copy.getAllKeys());
        keys.removeAll(POSITION_KEYS);
        List<String> differing = new ArrayList<>();
        for (String key : keys.stream().sorted().toList()) {
            if (!Objects.equals(original.get(key), copy.get(key))) {
                differing.add(key + " (" + abbreviate(original.get(key)) + " vs " + abbreviate(copy.get(key)) + ")");
            }
        }
        return differing.isEmpty() ? null : String.join(", ", differing);
    }

    private static String abbreviate(Object tag) {
        String text = String.valueOf(tag);
        return text.length() > 60 ? text.substring(0, 57) + "..." : text;
    }

    private static final class Pair {
        private final Block block;
        private final int variant;
        private final BlockPos original;
        private final BlockPos copy;
        private boolean loaded;
        private boolean crafting;

        private Pair(Block block, int variant, BlockPos original, BlockPos copy) {
            this.block = block;
            this.variant = variant;
            this.original = original;
            this.copy = copy;
        }

        Block block() {
            return block;
        }

        int variant() {
            return variant;
        }

        BlockPos original() {
            return original;
        }

        BlockPos copy() {
            return copy;
        }
    }
}
