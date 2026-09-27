package com.rngtech.client.sound;

import com.rngtech.content.block.AlgaePhotobioreactorBlock;
import com.rngtech.content.block.AlloyFurnaceBlock;
import com.rngtech.content.block.AmmoniaFuelCellBlock;
import com.rngtech.content.block.AmmoniaSynthesizerBlock;
import com.rngtech.content.block.BatteryAssemblerBlock;
import com.rngtech.content.block.BatteryChassisBlock;
import com.rngtech.content.block.BioGeneratorBlock;
import com.rngtech.content.block.CavitationGeneratorBlock;
import com.rngtech.content.block.ComponentRecyclerBlock;
import com.rngtech.content.block.CompressorTankBlock;
import com.rngtech.content.block.CorrosionCellBlock;
import com.rngtech.content.block.CrusherBlock;
import com.rngtech.content.block.FurnaceBlock;
import com.rngtech.content.block.GasChemistryBlock;
import com.rngtech.content.block.MelterBlock;
import com.rngtech.content.block.MetalPressBlock;
import com.rngtech.content.block.PotentialReactorBlock;
import com.rngtech.content.block.ResonanceCalibratorBlock;
import com.rngtech.content.block.SilicaGelDehumidifierBlock;
import com.rngtech.content.block.SolarArrayControllerBlock;
import com.rngtech.content.block.SolarPanelBlock;
import com.rngtech.content.block.SolidFuelBurnerBlock;
import com.rngtech.content.block.VacuumCollapseGeneratorBlock;
import com.rngtech.content.block.WoodenDehumidifierBlock;
import com.rngtech.content.registry.ModSounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

enum MachineSoundProfile {
    KINETIC(
            ModSounds.MACHINE_KINETIC_LOOP,
            ModSounds.MACHINE_KINETIC_START,
            ModSounds.MACHINE_KINETIC_STOP,
            0.14F,
            0.20F,
            0.32F,
            0.92F
    ),
    THERMAL(
            ModSounds.MACHINE_THERMAL_LOOP,
            ModSounds.MACHINE_THERMAL_START,
            ModSounds.MACHINE_THERMAL_STOP,
            0.12F,
            0.18F,
            0.30F,
            0.88F
    ),
    ELECTRIC(
            ModSounds.MACHINE_ELECTRIC_LOOP,
            ModSounds.MACHINE_ELECTRIC_START,
            ModSounds.MACHINE_ELECTRIC_STOP,
            0.10F,
            0.15F,
            0.24F,
            0.95F
    ),
    FLUID(
            ModSounds.MACHINE_FLUID_LOOP,
            ModSounds.MACHINE_FLUID_START,
            ModSounds.MACHINE_FLUID_STOP,
            0.10F,
            0.15F,
            0.24F,
            0.90F
    ),
    CHEMICAL(
            ModSounds.MACHINE_CHEMICAL_LOOP,
            ModSounds.MACHINE_CHEMICAL_START,
            ModSounds.MACHINE_CHEMICAL_STOP,
            0.11F,
            0.16F,
            0.26F,
            0.93F
    ),
    REACTOR(
            ModSounds.MACHINE_REACTOR_LOOP,
            ModSounds.MACHINE_REACTOR_START,
            ModSounds.MACHINE_REACTOR_STOP,
            0.13F,
            0.19F,
            0.28F,
            0.85F
    );

    private static final float STAGE_VOLUME_STEP = 0.005F;
    private static final float STAGE_PITCH_STEP = 0.025F;

    private final DeferredHolder<SoundEvent, SoundEvent> loopSound;
    private final DeferredHolder<SoundEvent, SoundEvent> startSound;
    private final DeferredHolder<SoundEvent, SoundEvent> stopSound;
    private final float baseLoopVolume;
    private final float maxLoopVolume;
    private final float cueVolume;
    private final float basePitch;

    MachineSoundProfile(
            DeferredHolder<SoundEvent, SoundEvent> loopSound,
            DeferredHolder<SoundEvent, SoundEvent> startSound,
            DeferredHolder<SoundEvent, SoundEvent> stopSound,
            float baseLoopVolume,
            float maxLoopVolume,
            float cueVolume,
            float basePitch
    ) {
        this.loopSound = loopSound;
        this.startSound = startSound;
        this.stopSound = stopSound;
        this.baseLoopVolume = baseLoopVolume;
        this.maxLoopVolume = maxLoopVolume;
        this.cueVolume = cueVolume;
        this.basePitch = basePitch;
    }

    static MachineSoundProfile forBlock(Block block) {
        if (block instanceof CrusherBlock
                || block instanceof ComponentRecyclerBlock
                || block instanceof MetalPressBlock
                || block instanceof ResonanceCalibratorBlock) {
            return KINETIC;
        }
        if (block instanceof FurnaceBlock
                || block instanceof AlloyFurnaceBlock
                || block instanceof SolidFuelBurnerBlock
                || block instanceof BioGeneratorBlock
                || block instanceof MelterBlock) {
            return THERMAL;
        }
        if (block instanceof GasChemistryBlock gasChemistry) {
            return switch (gasChemistry.machine()) {
                case COAL_GASIFIER -> THERMAL;
                case SYNGAS_COMBUSTOR, STEAM_METHANE_REFORMER -> CHEMICAL;
            };
        }
        if (block instanceof BatteryChassisBlock
                || block instanceof BatteryAssemblerBlock
                || block instanceof SolarPanelBlock
                || block instanceof SolarArrayControllerBlock
                || block instanceof AmmoniaSynthesizerBlock) {
            return ELECTRIC;
        }
        if (block instanceof WoodenDehumidifierBlock
                || block instanceof SilicaGelDehumidifierBlock
                || block instanceof AlgaePhotobioreactorBlock
                || block instanceof CompressorTankBlock
                || block instanceof CavitationGeneratorBlock) {
            return FLUID;
        }
        if (block instanceof CorrosionCellBlock || block instanceof AmmoniaFuelCellBlock) {
            return CHEMICAL;
        }
        if (block instanceof PotentialReactorBlock || block instanceof VacuumCollapseGeneratorBlock) {
            return REACTOR;
        }
        return null;
    }

    SoundEvent loopSound() {
        return loopSound.get();
    }

    SoundEvent startSound() {
        return startSound.get();
    }

    SoundEvent stopSound() {
        return stopSound.get();
    }

    float loopVolume(int stage) {
        return Math.min(maxLoopVolume, baseLoopVolume + stageFactor(stage) * STAGE_VOLUME_STEP);
    }

    float cueVolume(int stage) {
        return Math.min(0.35F, cueVolume + stageFactor(stage) * STAGE_VOLUME_STEP);
    }

    float pitch(int stage) {
        return Math.min(1.18F, basePitch + stageFactor(stage) * STAGE_PITCH_STEP);
    }

    private static int stageFactor(int stage) {
        return Math.max(0, Math.min(8, stage));
    }
}
