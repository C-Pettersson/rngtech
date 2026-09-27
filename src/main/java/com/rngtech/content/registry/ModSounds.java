package com.rngtech.content.registry;

import com.rngtech.RNGTech;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, RNGTech.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GUI_BUTTON_CLICK =
            register("gui.button_click");

    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_KINETIC_LOOP =
            register("machine.kinetic.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_KINETIC_START =
            register("machine.kinetic.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_KINETIC_STOP =
            register("machine.kinetic.stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_THERMAL_LOOP =
            register("machine.thermal.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_THERMAL_START =
            register("machine.thermal.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_THERMAL_STOP =
            register("machine.thermal.stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_ELECTRIC_LOOP =
            register("machine.electric.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_ELECTRIC_START =
            register("machine.electric.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_ELECTRIC_STOP =
            register("machine.electric.stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_FLUID_LOOP =
            register("machine.fluid.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_FLUID_START =
            register("machine.fluid.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_FLUID_STOP =
            register("machine.fluid.stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_CHEMICAL_LOOP =
            register("machine.chemical.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_CHEMICAL_START =
            register("machine.chemical.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_CHEMICAL_STOP =
            register("machine.chemical.stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_REACTOR_LOOP =
            register("machine.reactor.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_REACTOR_START =
            register("machine.reactor.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_REACTOR_STOP =
            register("machine.reactor.stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORESTRY_COMPANION_MOVE =
            register("forestry_companion.move");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORESTRY_COMPANION_SCAN =
            register("forestry_companion.scan");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORESTRY_COMPANION_PLANT =
            register("forestry_companion.plant");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORESTRY_COMPANION_LEAF_CUT =
            register("forestry_companion.leaf_cut");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORESTRY_COMPANION_LOG_CUT =
            register("forestry_companion.log_cut");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORESTRY_COMPANION_TREEFELLER =
            register("forestry_companion.treefeller");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORESTRY_COMPANION_TRANSFER =
            register("forestry_companion.transfer");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORESTRY_COMPANION_BLOCKED =
            register("forestry_companion.blocked");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        return SOUND_EVENTS.register(
                path,
                () -> SoundEvent.createVariableRangeEvent(RNGTech.id(path))
        );
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
