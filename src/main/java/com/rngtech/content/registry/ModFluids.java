package com.rngtech.content.registry;

import com.rngtech.RNGTech;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModFluids {
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, RNGTech.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, RNGTech.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> LUBRICANT_TYPE =
            FLUID_TYPES.register("lubricant", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.rngtech.lubricant")
                    .density(1100)
                    .viscosity(1800)
                    .temperature(330)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            ));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LUBRICANT_SOURCE =
            FLUIDS.register("lubricant", () -> new BaseFlowingFluid.Source(lubricantProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LUBRICANT_FLOWING =
            FLUIDS.register("flowing_lubricant", () -> new BaseFlowingFluid.Flowing(lubricantProperties()));
    public static final DeferredHolder<FluidType, FluidType> ELECTROLYTE_SOLUTION_TYPE =
            FLUID_TYPES.register("electrolyte_solution", () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.rngtech.electrolyte_solution")
                    .density(1050)
                    .viscosity(1200)
                    .temperature(300)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            ));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> ELECTROLYTE_SOLUTION_SOURCE =
            FLUIDS.register("electrolyte_solution", () -> new BaseFlowingFluid.Source(electrolyteSolutionProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> ELECTROLYTE_SOLUTION_FLOWING =
            FLUIDS.register("flowing_electrolyte_solution", () -> new BaseFlowingFluid.Flowing(electrolyteSolutionProperties()));
    public static final DeferredHolder<FluidType, FluidType> CARBON_EXHAUST_TYPE =
            FLUID_TYPES.register("carbon_exhaust", () -> gasType("fluid.rngtech.carbon_exhaust", 900, 300));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CARBON_EXHAUST_SOURCE =
            FLUIDS.register("carbon_exhaust", () -> new BaseFlowingFluid.Source(carbonExhaustProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> CARBON_EXHAUST_FLOWING =
            FLUIDS.register("flowing_carbon_exhaust", () -> new BaseFlowingFluid.Flowing(carbonExhaustProperties()));
    public static final DeferredHolder<FluidType, FluidType> SYNGAS_TYPE =
            FLUID_TYPES.register("syngas", () -> gasType("fluid.rngtech.syngas", 650, 420));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SYNGAS_SOURCE =
            FLUIDS.register("syngas", () -> new BaseFlowingFluid.Source(syngasProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> SYNGAS_FLOWING =
            FLUIDS.register("flowing_syngas", () -> new BaseFlowingFluid.Flowing(syngasProperties()));
    public static final DeferredHolder<FluidType, FluidType> METHANE_TYPE =
            FLUID_TYPES.register("methane", () -> gasType("fluid.rngtech.methane", 520, 180));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> METHANE_SOURCE =
            FLUIDS.register("methane", () -> new BaseFlowingFluid.Source(methaneProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> METHANE_FLOWING =
            FLUIDS.register("flowing_methane", () -> new BaseFlowingFluid.Flowing(methaneProperties()));
    public static final DeferredHolder<FluidType, FluidType> CARBON_MONOXIDE_TYPE =
            FLUID_TYPES.register("carbon_monoxide", () -> gasType("fluid.rngtech.carbon_monoxide", 600, 300));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CARBON_MONOXIDE_SOURCE =
            FLUIDS.register("carbon_monoxide", () -> new BaseFlowingFluid.Source(carbonMonoxideProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> CARBON_MONOXIDE_FLOWING =
            FLUIDS.register("flowing_carbon_monoxide", () -> new BaseFlowingFluid.Flowing(carbonMonoxideProperties()));
    public static final DeferredHolder<FluidType, FluidType> NITROGEN_TYPE =
            FLUID_TYPES.register("nitrogen", () -> gasType("fluid.rngtech.nitrogen", 700, 80));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> NITROGEN_SOURCE =
            FLUIDS.register("nitrogen", () -> new BaseFlowingFluid.Source(nitrogenProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> NITROGEN_FLOWING =
            FLUIDS.register("flowing_nitrogen", () -> new BaseFlowingFluid.Flowing(nitrogenProperties()));
    public static final DeferredHolder<FluidType, FluidType> HYDROGEN_TYPE =
            FLUID_TYPES.register("hydrogen", () -> gasType("fluid.rngtech.hydrogen", 120, 120));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> HYDROGEN_SOURCE =
            FLUIDS.register("hydrogen", () -> new BaseFlowingFluid.Source(hydrogenProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> HYDROGEN_FLOWING =
            FLUIDS.register("flowing_hydrogen", () -> new BaseFlowingFluid.Flowing(hydrogenProperties()));
    public static final DeferredHolder<FluidType, FluidType> AMMONIA_TYPE =
            FLUID_TYPES.register("ammonia", () -> gasType("fluid.rngtech.ammonia", 650, 240));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> AMMONIA_SOURCE =
            FLUIDS.register("ammonia", () -> new BaseFlowingFluid.Source(ammoniaProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> AMMONIA_FLOWING =
            FLUIDS.register("flowing_ammonia", () -> new BaseFlowingFluid.Flowing(ammoniaProperties()));

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
    }

    private static BaseFlowingFluid.Properties lubricantProperties() {
        return new BaseFlowingFluid.Properties(LUBRICANT_TYPE, LUBRICANT_SOURCE, LUBRICANT_FLOWING)
                .bucket(ModItems.LUBRICANT_BUCKET)
                .block(ModBlocks.LUBRICANT_BLOCK)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(2)
                .tickRate(8);
    }

    private static BaseFlowingFluid.Properties electrolyteSolutionProperties() {
        return new BaseFlowingFluid.Properties(
                ELECTROLYTE_SOLUTION_TYPE,
                ELECTROLYTE_SOLUTION_SOURCE,
                ELECTROLYTE_SOLUTION_FLOWING
        )
                .bucket(ModItems.ELECTROLYTE_SOLUTION_BUCKET)
                .block(ModBlocks.ELECTROLYTE_SOLUTION_BLOCK)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(2)
                .tickRate(8);
    }

    private static FluidType gasType(String descriptionId, int density, int temperature) {
        return new FluidType(FluidType.Properties.create()
                .descriptionId(descriptionId)
                .density(density)
                .viscosity(200)
                .temperature(temperature)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
        );
    }

    private static BaseFlowingFluid.Properties carbonExhaustProperties() {
        return gasProperties(CARBON_EXHAUST_TYPE, CARBON_EXHAUST_SOURCE, CARBON_EXHAUST_FLOWING)
                .bucket(ModItems.CARBON_EXHAUST_BUCKET)
                .block(ModBlocks.CARBON_EXHAUST_BLOCK);
    }

    private static BaseFlowingFluid.Properties syngasProperties() {
        return gasProperties(SYNGAS_TYPE, SYNGAS_SOURCE, SYNGAS_FLOWING)
                .bucket(ModItems.SYNGAS_BUCKET)
                .block(ModBlocks.SYNGAS_BLOCK);
    }

    private static BaseFlowingFluid.Properties methaneProperties() {
        return gasProperties(METHANE_TYPE, METHANE_SOURCE, METHANE_FLOWING)
                .bucket(ModItems.METHANE_BUCKET)
                .block(ModBlocks.METHANE_BLOCK);
    }

    private static BaseFlowingFluid.Properties carbonMonoxideProperties() {
        return gasProperties(CARBON_MONOXIDE_TYPE, CARBON_MONOXIDE_SOURCE, CARBON_MONOXIDE_FLOWING)
                .bucket(ModItems.CARBON_MONOXIDE_BUCKET)
                .block(ModBlocks.CARBON_MONOXIDE_BLOCK);
    }

    private static BaseFlowingFluid.Properties nitrogenProperties() {
        return gasProperties(NITROGEN_TYPE, NITROGEN_SOURCE, NITROGEN_FLOWING)
                .bucket(ModItems.NITROGEN_BUCKET)
                .block(ModBlocks.NITROGEN_BLOCK);
    }

    private static BaseFlowingFluid.Properties hydrogenProperties() {
        return gasProperties(HYDROGEN_TYPE, HYDROGEN_SOURCE, HYDROGEN_FLOWING)
                .bucket(ModItems.HYDROGEN_BUCKET)
                .block(ModBlocks.HYDROGEN_BLOCK);
    }

    private static BaseFlowingFluid.Properties ammoniaProperties() {
        return gasProperties(AMMONIA_TYPE, AMMONIA_SOURCE, AMMONIA_FLOWING)
                .bucket(ModItems.AMMONIA_BUCKET)
                .block(ModBlocks.AMMONIA_BLOCK);
    }

    private static BaseFlowingFluid.Properties gasProperties(
            DeferredHolder<FluidType, FluidType> type,
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing
    ) {
        return new BaseFlowingFluid.Properties(type, source, flowing)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5);
    }

    private ModFluids() {
    }
}
