package com.rngtech.benchmark;

import com.rngtech.RNGTech;
import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.block.CableBlock;
import com.rngtech.content.blockentity.CableBlockEntity;
import com.rngtech.content.blockentity.CrusherBlockEntity;
import com.rngtech.content.blockentity.DebugBatteryBlockEntity;
import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.cable.EnergyConnectorTier;
import com.rngtech.content.energy.SolarPanelMaterial;
import com.rngtech.content.machine.CrushHeadMaterial;
import com.rngtech.content.machine.CrusherChassisMaterial;
import com.rngtech.content.registry.ModBlocks;
import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The fixed scene the tick benchmark measures: one cable network that joins a grid of machines, debug batteries and a
 * tiled solar array, all laid out from constants so every run builds the same world.
 *
 * <p>Layout, in blocks from the scene origin. The main grid is {@value #GRID} by {@value #GRID} with a cable line every
 * {@value #LINE_SPACING} blocks. One machine sits in each cell, beside a line cable that carries a Universal Connector
 * facing it. The solar array lies east of the grid on the same plane, joined by one cable line.
 */
final class BenchmarkScene {
    static final int GRID = 65;
    static final int LINE_SPACING = 4;
    /** Every n-th cell holds a Crusher that is fed coal, so a third of the machines work and the rest idle. */
    private static final int WORKING_CELL_PERIOD = 3;
    /** Every n-th cell holds a Debug Battery instead of a machine. */
    private static final int BATTERY_CELL_PERIOD = 25;
    private static final int BATTERY_OUTPUT = 100_000;
    private static final EnergyConnectorTier MACHINE_CONNECTOR = EnergyConnectorTier.COPPER;
    private static final EnergyConnectorTier SOURCE_CONNECTOR = EnergyConnectorTier.ARCLITE;
    private static final int SOLAR_TILES = 7;
    private static final int SOLAR_TILE = 5;
    private static final int SOLAR_SIZE = SOLAR_TILES * SOLAR_TILE;
    private static final int SOLAR_GAP = 8;
    private static final int BRIDGE_LINE_Z = 32;
    private static final long DAY_TIME = 6000L;
    private static final int COAL_STACK = 64;

    private final ServerLevel level;
    private final BlockPos origin;
    private final List<CrusherBlockEntity> workingCrushers = new ArrayList<>();
    private final Set<BlockPos> cables = new HashSet<>();
    private final Counts counts = new Counts();
    private long itemsProduced;

    BenchmarkScene(ServerLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
    }

    static final class Counts {
        int cables;
        int connectors;
        int workingCrushers;
        int idleMachines;
        int batteries;
        int solarControllers;
        int solarPanels;
    }

    Counts counts() {
        return counts;
    }

    /** Builds everything and freezes time and weather. Call once, on the server thread. */
    void build() {
        fixTimeAndWeather();
        forceLoad();
        platform();
        List<Block> idleBlocks = idleMachineBlocks();
        List<Placement> connectors = new ArrayList<>();
        gridMachines(idleBlocks, connectors);
        solarArray(connectors);
        cables();
        connectors.forEach(this::connect);
        counts.cables = cables.size();
    }

    /** Keeps the working Crushers running: refills coal and empties output, as a hopper line would. */
    void service() {
        for (CrusherBlockEntity crusher : workingCrushers) {
            var inventory = crusher.getInventory();
            if (inventory.getStackInSlot(CrusherBlockEntity.SLOT_INPUT_A).getCount() < COAL_STACK / 2) {
                inventory.setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.COAL, COAL_STACK));
            }
            itemsProduced += inventory.getStackInSlot(CrusherBlockEntity.SLOT_OUTPUT).getCount();
            inventory.setStackInSlot(CrusherBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);
        }
    }

    CableBlockEntity.NetworkDebugSnapshot networkSnapshot() {
        return CableBlockEntity.networkDebugSnapshot(level, at(0, 0, 0));
    }

    long workingEnergy() {
        long total = 0;
        for (CrusherBlockEntity crusher : workingCrushers) {
            total += crusher.getEnergyStorage(null).getEnergyStored();
        }
        return total;
    }

    /** Items the working Crushers have produced so far; zero means the machines never ran. */
    long itemsProduced() {
        return itemsProduced;
    }

    private void fixTimeAndWeather() {
        var server = level.getServer();
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.setDayTime(DAY_TIME);
        level.setWeatherParameters(1_000_000, 0, false, false);
    }

    private void forceLoad() {
        ChunkPos min = new ChunkPos(at(-2, 0, -2));
        ChunkPos max = new ChunkPos(at(GRID + SOLAR_GAP + SOLAR_SIZE + 2, 0, GRID + 2));
        for (int x = min.x; x <= max.x; x++) {
            for (int z = min.z; z <= max.z; z++) {
                level.setChunkForced(x, z, true);
            }
        }
    }

    /** A stone floor under the scene and clear air above it, whatever the test world's terrain is. */
    private void platform() {
        BlockPos from = at(-2, -1, -2);
        BlockPos to = at(GRID + SOLAR_GAP + SOLAR_SIZE + 2, -1, GRID + 2);
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        for (BlockPos pos : BlockPos.betweenClosed(at(-2, 0, -2), at(GRID + SOLAR_GAP + SOLAR_SIZE + 2, 4, GRID + 2))) {
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    /** Every machine block the mod registers, in registry order, used for the machines that stay idle. */
    private static List<Block> idleMachineBlocks() {
        return BuiltInRegistries.BLOCK.stream()
                .filter(block -> BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(RNGTech.MOD_ID))
                .filter(block -> block instanceof BaseMachineBlock)
                .filter(block -> block != ModBlocks.SOLAR_ARRAY_CONTROLLER.get() && !(block instanceof com.rngtech.content.block.SolarPanelBlock))
                .sorted(java.util.Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()))
                .toList();
    }

    private void gridMachines(List<Block> idleBlocks, List<Placement> connectors) {
        int cell = 0;
        int idle = 0;
        for (int x = 0; x + LINE_SPACING < GRID; x += LINE_SPACING) {
            for (int z = 0; z + LINE_SPACING < GRID; z += LINE_SPACING) {
                BlockPos machinePos = at(x + 1, 0, z + 2);
                BlockPos cablePos = at(x, 0, z + 2);
                EnergyConnectorTier tier = MACHINE_CONNECTOR;
                if (cell % BATTERY_CELL_PERIOD == 0) {
                    tier = SOURCE_CONNECTOR;
                    placeBattery(machinePos);
                } else if (cell % WORKING_CELL_PERIOD == 0) {
                    placeWorkingCrusher(machinePos);
                } else {
                    placeIdleMachine(machinePos, idleBlocks.get(idle++ % idleBlocks.size()));
                }
                connectors.add(new Placement(cablePos, Direction.EAST, tier));
                cell++;
            }
        }
    }

    private void placeBattery(BlockPos pos) {
        level.setBlock(pos, ModBlocks.DEBUG_BATTERY.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof DebugBatteryBlockEntity battery) {
            battery.setOutputRate(BATTERY_OUTPUT);
        }
        counts.batteries++;
    }

    private void placeWorkingCrusher(BlockPos pos) {
        level.setBlock(pos, ModBlocks.crusherChassis(CrusherChassisMaterial.WOODEN).get().defaultBlockState(), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof CrusherBlockEntity crusher) {
            crusher.getInventory().setStackInSlot(
                    CrusherBlockEntity.SLOT_CRUSH_HEAD,
                    new ItemStack(ModItems.crushHead(CrushHeadMaterial.FLINT).get())
            );
            crusher.getInventory().setStackInSlot(CrusherBlockEntity.SLOT_INPUT_A, new ItemStack(Items.COAL, COAL_STACK));
            workingCrushers.add(crusher);
            counts.workingCrushers++;
        }
    }

    private void placeIdleMachine(BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
        counts.idleMachines++;
    }

    /** Controllers every {@value #SOLAR_TILE} blocks, panels everywhere else, one cable under each controller. */
    private void solarArray(List<Placement> connectors) {
        int x0 = GRID + SOLAR_GAP;
        int z0 = BRIDGE_LINE_Z - SOLAR_SIZE / 2;
        BlockState panel = ModBlocks.solarPanel(SolarPanelMaterial.CRUDE).get().defaultBlockState();
        BlockState controller = ModBlocks.SOLAR_ARRAY_CONTROLLER.get().defaultBlockState();
        int half = SOLAR_TILE / 2;
        for (int x = 0; x < SOLAR_SIZE; x++) {
            for (int z = 0; z < SOLAR_SIZE; z++) {
                boolean isController = x % SOLAR_TILE == half && z % SOLAR_TILE == half;
                level.setBlock(at(x0 + x, 1, z0 + z), isController ? controller : panel, Block.UPDATE_CLIENTS);
                if (isController) {
                    counts.solarControllers++;
                    connectors.add(new Placement(at(x0 + x, 0, z0 + z), Direction.UP, SOURCE_CONNECTOR));
                } else {
                    counts.solarPanels++;
                }
            }
        }
    }

    /** Cable lines of the machine grid, the collector rows and column under the solar array, and the bridge between them. */
    private void cables() {
        for (int x = 0; x < GRID; x++) {
            for (int z = 0; z < GRID; z++) {
                if (x % LINE_SPACING == 0 || z % LINE_SPACING == 0) {
                    cables.add(at(x, 0, z));
                }
            }
        }
        int x0 = GRID + SOLAR_GAP;
        int z0 = BRIDGE_LINE_Z - SOLAR_SIZE / 2;
        int half = SOLAR_TILE / 2;
        for (int x = GRID; x <= x0 + half; x++) {
            cables.add(at(x, 0, BRIDGE_LINE_Z));
        }
        for (int x = 0; x < SOLAR_SIZE; x++) {
            for (int z = 0; z < SOLAR_SIZE; z++) {
                if (z % SOLAR_TILE == half || x == half) {
                    cables.add(at(x0 + x, 0, z0 + z));
                }
            }
        }
        for (BlockPos pos : cables) {
            level.setBlock(pos, linkedCable(pos), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private BlockState linkedCable(BlockPos pos) {
        BlockState state = ModBlocks.CABLE.get().defaultBlockState();
        for (Direction direction : Direction.values()) {
            if (cables.contains(pos.relative(direction))) {
                state = state.setValue(CableBlock.cableProperty(direction), true);
            }
        }
        return state;
    }

    private void connect(Placement placement) {
        if (level.getBlockEntity(placement.cable()) instanceof CableBlockEntity cable
                && cable.installUniversalConnector(placement.face())) {
            cable.universalConnector(placement.face()).getInventory().setStackInSlot(
                    UniversalConnectorBlockEntity.SLOT_ENERGY_CONNECTOR,
                    new ItemStack(ModItems.energyConnector(placement.tier()).get())
            );
            BlockState state = level.getBlockState(placement.cable());
            level.setBlock(
                    placement.cable(),
                    state.setValue(CableBlock.connectorProperty(placement.face()), true),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
            );
            counts.connectors++;
        }
    }

    private BlockPos at(int x, int y, int z) {
        return origin.offset(x, y, z);
    }

    private record Placement(BlockPos cable, Direction face, EnergyConnectorTier tier) {
    }
}
