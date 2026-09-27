package com.rngtech.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

final class MachineLoopSound extends AbstractTickableSoundInstance {
    private static final int FADE_IN_TICKS = 20;
    private static final int FADE_OUT_TICKS = 16;

    private final BlockPos pos;
    private final MachineSoundProfile profile;
    private final int stage;
    private final float targetVolume;
    private final float targetPitch;
    private int age;
    private int fadeOutTicks = -1;

    MachineLoopSound(BlockPos pos, MachineSoundProfile profile, int stage) {
        super(profile.loopSound(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.pos = pos.immutable();
        this.profile = profile;
        this.stage = stage;
        targetVolume = profile.loopVolume(stage);
        targetPitch = profile.pitch(stage);
        volume = 0.0F;
        pitch = targetPitch;
        x = pos.getX() + 0.5D;
        y = pos.getY() + 0.5D;
        z = pos.getZ() + 0.5D;
        looping = true;
        delay = 0;
        attenuation = SoundInstance.Attenuation.LINEAR;
    }

    @Override
    public void tick() {
        age++;
        if (fadeOutTicks >= 0) {
            fadeOutTicks++;
            if (fadeOutTicks >= FADE_OUT_TICKS) {
                stop();
                return;
            }
        }
        float fadeIn = Math.min(1.0F, age / (float) FADE_IN_TICKS);
        float fadeOut = fadeOutTicks < 0 ? 1.0F : Math.max(0.0F, 1.0F - fadeOutTicks / (float) FADE_OUT_TICKS);
        volume = targetVolume * Math.min(fadeIn, fadeOut);
        pitch = targetPitch;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    BlockPos pos() {
        return pos;
    }

    MachineSoundProfile profile() {
        return profile;
    }

    int stage() {
        return stage;
    }

    boolean isStopping() {
        return fadeOutTicks >= 0;
    }

    void beginStopping() {
        if (fadeOutTicks < 0) {
            fadeOutTicks = 0;
        }
    }

    void stopNow() {
        stop();
    }
}
