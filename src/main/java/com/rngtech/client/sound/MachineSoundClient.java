package com.rngtech.client.sound;

import com.rngtech.content.block.BaseMachineBlock;
import com.rngtech.content.blockentity.BaseMachineBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class MachineSoundClient {
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int START_RANGE_BLOCKS = 32;
    private static final int SCAN_RADIUS_CHUNKS = (START_RANGE_BLOCKS + 15) / 16 + 1;
    private static final int CUE_COOLDOWN_TICKS = 12;
    private static final double START_RANGE_SQR = START_RANGE_BLOCKS * START_RANGE_BLOCKS;
    private static final Map<BlockPos, MachineLoopSound> ACTIVE_LOOPS = new HashMap<>();
    private static final Map<BlockPos, Integer> CUE_COOLDOWNS = new HashMap<>();

    private static Level currentLevel;
    private static int nextScanTick;

    private MachineSoundClient() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            clearSounds();
            currentLevel = null;
            return;
        }

        if (level != currentLevel) {
            clearSounds();
            currentLevel = level;
        }

        removeStoppedSounds();
        int tick = player.tickCount;
        if (tick < nextScanTick) {
            return;
        }

        nextScanTick = tick + SCAN_INTERVAL_TICKS;
        Set<BlockPos> activePositions = new HashSet<>();
        scanActiveMachines(minecraft, level, player, tick, activePositions);
        stopMissingLoops(minecraft, tick, activePositions);
        pruneCueCooldowns(tick);
    }

    private static void scanActiveMachines(
            Minecraft minecraft,
            Level level,
            LocalPlayer player,
            int tick,
            Set<BlockPos> activePositions
    ) {
        BlockPos playerPos = player.blockPosition();
        int centerChunkX = SectionPos.blockToSectionCoord(playerPos.getX());
        int centerChunkZ = SectionPos.blockToSectionCoord(playerPos.getZ());
        for (int chunkX = centerChunkX - SCAN_RADIUS_CHUNKS; chunkX <= centerChunkX + SCAN_RADIUS_CHUNKS; chunkX++) {
            for (int chunkZ = centerChunkZ - SCAN_RADIUS_CHUNKS; chunkZ <= centerChunkZ + SCAN_RADIUS_CHUNKS; chunkZ++) {
                ChunkAccess chunkAccess = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunkAccess instanceof LevelChunk chunk) {
                    scanChunk(minecraft, player, tick, activePositions, chunk);
                }
            }
        }
    }

    private static void scanChunk(
            Minecraft minecraft,
            LocalPlayer player,
            int tick,
            Set<BlockPos> activePositions,
            LevelChunk chunk
    ) {
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof BaseMachineBlockEntity machine) {
                trackMachine(minecraft, player, tick, activePositions, machine);
            }
        }
    }

    private static void trackMachine(
            Minecraft minecraft,
            LocalPlayer player,
            int tick,
            Set<BlockPos> activePositions,
            BaseMachineBlockEntity machine
    ) {
        BlockState state = machine.getBlockState();
        if (!(state.getBlock() instanceof BaseMachineBlock) || !state.getValue(BaseMachineBlock.ACTIVE)) {
            return;
        }

        MachineSoundProfile profile = MachineSoundProfile.forBlock(state.getBlock());
        if (profile == null) {
            return;
        }

        BlockPos pos = machine.getBlockPos();
        if (distanceSqr(player, pos) > START_RANGE_SQR) {
            return;
        }

        BlockPos key = pos.immutable();
        if (machine.mutesMachineSound()) {
            stopMutedLoop(key);
            return;
        }

        activePositions.add(key);
        MachineLoopSound current = ACTIVE_LOOPS.get(key);
        if (current != null && (current.profile() != profile || current.isStopping())) {
            current.stopNow();
            ACTIVE_LOOPS.remove(key);
            current = null;
        }

        if (current == null) {
            MachineLoopSound sound = new MachineLoopSound(key, profile, machine.refinementComponentStage());
            ACTIVE_LOOPS.put(key, sound);
            minecraft.getSoundManager().play(sound);
            playCue(minecraft, profile.startSound(), key, profile, machine.refinementComponentStage(), tick);
        }
    }

    private static void stopMutedLoop(BlockPos key) {
        MachineLoopSound current = ACTIVE_LOOPS.remove(key);
        if (current != null) {
            current.stopNow();
        }
        CUE_COOLDOWNS.remove(key);
    }

    private static void stopMissingLoops(Minecraft minecraft, int tick, Set<BlockPos> activePositions) {
        for (Map.Entry<BlockPos, MachineLoopSound> entry : ACTIVE_LOOPS.entrySet()) {
            if (!activePositions.contains(entry.getKey())) {
                MachineLoopSound sound = entry.getValue();
                if (!sound.isStopping()) {
                    sound.beginStopping();
                    playCue(minecraft, sound.profile().stopSound(), entry.getKey(), sound.profile(), sound.stage(), tick);
                }
            }
        }
    }

    private static void playCue(
            Minecraft minecraft,
            SoundEvent sound,
            BlockPos pos,
            MachineSoundProfile profile,
            int stage,
            int tick
    ) {
        Integer cooldownUntil = CUE_COOLDOWNS.get(pos);
        if (cooldownUntil != null && cooldownUntil > tick) {
            return;
        }
        CUE_COOLDOWNS.put(pos, tick + CUE_COOLDOWN_TICKS);
        minecraft.getSoundManager().play(new SimpleSoundInstance(
                sound,
                SoundSource.BLOCKS,
                profile.cueVolume(stage),
                profile.pitch(stage),
                SoundInstance.createUnseededRandom(),
                pos
        ));
    }

    private static double distanceSqr(LocalPlayer player, BlockPos pos) {
        double dx = player.getX() - (pos.getX() + 0.5D);
        double dy = player.getY() - (pos.getY() + 0.5D);
        double dz = player.getZ() - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static void removeStoppedSounds() {
        Iterator<Map.Entry<BlockPos, MachineLoopSound>> iterator = ACTIVE_LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isStopped()) {
                iterator.remove();
            }
        }
    }

    private static void pruneCueCooldowns(int tick) {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = CUE_COOLDOWNS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= tick) {
                iterator.remove();
            }
        }
    }

    private static void clearSounds() {
        for (MachineLoopSound sound : ACTIVE_LOOPS.values()) {
            sound.stopNow();
        }
        ACTIVE_LOOPS.clear();
        CUE_COOLDOWNS.clear();
        nextScanTick = 0;
    }
}
