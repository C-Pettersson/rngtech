package com.rngtech.client;

import com.rngtech.content.item.MinersCompanionItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class MinersCompanionLampClient {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final BlockState LAMP_LIGHT = Blocks.LIGHT.defaultBlockState()
            .setValue(LightBlock.LEVEL, MinersCompanionItem.MINING_LAMP_LIGHT_LEVEL)
            .setValue(LightBlock.WATERLOGGED, false);

    private static ClientLevel litLevel;
    private static BlockPos litPos;
    private static BlockState replacedState;

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || !MinersCompanionItem.hasActiveMiningLamp(player)) {
            clearLight();
            return;
        }

        BlockPos pos = lightPosition(level, player);
        if (pos == null) {
            clearLight();
            return;
        }

        if (level != litLevel || !pos.equals(litPos) || !isLampLight(level, pos)) {
            clearLight();
            if (!placeLight(level, pos)) {
                return;
            }
        }

        litLevel = level;
        litPos = pos.immutable();
    }

    private static void clearLight() {
        if (litLevel != null && litPos != null && litLevel.isLoaded(litPos) && isLampLight(litLevel, litPos)) {
            litLevel.setBlock(litPos, replacedState == null ? Blocks.AIR.defaultBlockState() : replacedState, UPDATE_FLAGS);
        }
        litLevel = null;
        litPos = null;
        replacedState = null;
    }

    private static BlockPos lightPosition(ClientLevel level, LocalPlayer player) {
        BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        if (canUseLightPosition(level, eyePos)) {
            return eyePos.immutable();
        }

        BlockPos bodyPos = player.blockPosition();
        if (!bodyPos.equals(eyePos) && canUseLightPosition(level, bodyPos)) {
            return bodyPos.immutable();
        }

        return null;
    }

    private static boolean canUseLightPosition(ClientLevel level, BlockPos pos) {
        return level.isLoaded(pos) && (pos.equals(litPos) || level.getBlockState(pos).isAir());
    }

    private static boolean placeLight(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            return false;
        }
        if (!level.setBlock(pos, LAMP_LIGHT, UPDATE_FLAGS)) {
            return false;
        }
        replacedState = state;
        return true;
    }

    private static boolean isLampLight(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.LIGHT)
                && state.getValue(LightBlock.LEVEL) == MinersCompanionItem.MINING_LAMP_LIGHT_LEVEL
                && !state.getValue(LightBlock.WATERLOGGED);
    }

    private MinersCompanionLampClient() {
    }
}
