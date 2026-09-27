package com.rngtech.content.configurator;

import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;
import com.rngtech.content.cable.CableConnectorMode;
import com.rngtech.content.cable.FluidConnectorMode;
import com.rngtech.content.cable.ItemConnectorMode;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record ConfiguratorPreset(
        ConnectorSetting energy,
        List<ConnectorSetting> fluidModules,
        List<ConnectorSetting> itemModules,
        boolean pasteModules,
        boolean gearHelper
) {
    public static final int MODULE_COUNT = UniversalConnectorBlockEntity.FLUID_MODULE_SLOT_COUNT;
    public static final ConfiguratorPreset EMPTY = new ConfiguratorPreset(
            ConnectorSetting.ABSENT,
            absentModules(),
            absentModules(),
            false,
            false
    );

    public static final Codec<ConfiguratorPreset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConnectorSetting.CODEC.optionalFieldOf("energy", ConnectorSetting.ABSENT)
                    .forGetter(ConfiguratorPreset::energy),
            ConnectorSetting.CODEC.listOf()
                    .optionalFieldOf("fluid_modules", absentModules())
                    .forGetter(ConfiguratorPreset::fluidModules),
            ConnectorSetting.CODEC.listOf()
                    .optionalFieldOf("item_modules", absentModules())
                    .forGetter(ConfiguratorPreset::itemModules),
            Codec.BOOL.optionalFieldOf("paste_modules", false).forGetter(ConfiguratorPreset::pasteModules),
            Codec.BOOL.optionalFieldOf("gear_helper", false).forGetter(ConfiguratorPreset::gearHelper)
    ).apply(instance, ConfiguratorPreset::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfiguratorPreset> STREAM_CODEC =
            StreamCodec.ofMember(ConfiguratorPreset::write, ConfiguratorPreset::read);

    public ConfiguratorPreset {
        energy = energy == null ? ConnectorSetting.ABSENT : energy;
        fluidModules = normalizeModules(fluidModules);
        itemModules = normalizeModules(itemModules);
    }

    public boolean hasAnySettings() {
        return energy.present() || hasPresentModule(fluidModules) || hasPresentModule(itemModules);
    }

    public ConfiguratorPreset withEnergy(ConnectorSetting setting) {
        return new ConfiguratorPreset(setting, fluidModules, itemModules, pasteModules, gearHelper);
    }

    public ConfiguratorPreset withFluidModules(List<ConnectorSetting> modules) {
        return new ConfiguratorPreset(energy, modules, itemModules, pasteModules, gearHelper);
    }

    public ConfiguratorPreset withItemModules(List<ConnectorSetting> modules) {
        return new ConfiguratorPreset(energy, fluidModules, modules, pasteModules, gearHelper);
    }

    public ConfiguratorPreset withPasteModules(boolean enabled) {
        return new ConfiguratorPreset(energy, fluidModules, itemModules, enabled, gearHelper);
    }

    public ConfiguratorPreset withGearHelper(boolean enabled) {
        return new ConfiguratorPreset(energy, fluidModules, itemModules, pasteModules, enabled);
    }

    public ConfiguratorPreset withoutModules() {
        return new ConfiguratorPreset(
                energy.withoutModule(),
                mapModules(fluidModules, ConnectorSetting::withoutModule),
                mapModules(itemModules, ConnectorSetting::withoutModule),
                pasteModules,
                gearHelper
        );
    }

    public ConfiguratorPreset resetAll() {
        return EMPTY;
    }

    public ConfiguratorPreset resetEnergy() {
        return withEnergy(ConnectorSetting.ABSENT);
    }

    public ConfiguratorPreset resetFluid() {
        return withFluidModules(absentModules());
    }

    public ConfiguratorPreset resetItem() {
        return withItemModules(absentModules());
    }

    public ConfiguratorPreset flipModes() {
        return new ConfiguratorPreset(
                flipEnergy(energy),
                mapModules(fluidModules, setting -> flipModule(setting, FluidConnectorMode.values().length)),
                mapModules(itemModules, setting -> flipModule(setting, ItemConnectorMode.values().length)),
                pasteModules,
                gearHelper
        );
    }

    public ConfiguratorPreset setCategoryChannel(ConfiguratorPresetCategory category, int channel) {
        return switch (category) {
            case ENERGY -> withEnergy(presentEnergy().withChannel(channel));
            case FLUID -> withFluidModules(mapModules(presentFluidModules(), setting -> setting.withChannel(channel)));
            case ITEM -> withItemModules(mapModules(presentItemModules(), setting -> setting.withChannel(channel)));
            case ALL -> withEnergy(presentEnergy().withChannel(channel))
                    .withFluidModules(mapModules(presentFluidModules(), setting -> setting.withChannel(channel)))
                    .withItemModules(mapModules(presentItemModules(), setting -> setting.withChannel(channel)));
        };
    }

    public ConfiguratorPreset cycleCategoryMode(ConfiguratorPresetCategory category) {
        return switch (category) {
            case ENERGY -> withEnergy(cycleEnergy(presentEnergy()));
            case FLUID -> withFluidModules(mapModules(presentFluidModules(), ConfiguratorPreset::cycleFluid));
            case ITEM -> withItemModules(mapModules(presentItemModules(), ConfiguratorPreset::cycleItem));
            case ALL -> withEnergy(cycleEnergy(presentEnergy()))
                    .withFluidModules(mapModules(presentFluidModules(), ConfiguratorPreset::cycleFluid))
                    .withItemModules(mapModules(presentItemModules(), ConfiguratorPreset::cycleItem));
        };
    }

    public ConfiguratorPreset setCategoryAttach(ConfiguratorPresetCategory category, RelativeDirection attachAs) {
        return switch (category) {
            case ENERGY -> attachAs.isNone() ? this : withEnergy(presentEnergy().withAttachAs(attachAs));
            case FLUID -> withFluidModules(mapModules(presentFluidModules(), setting -> setting.withAttachAs(attachAs)));
            case ITEM -> withItemModules(mapModules(presentItemModules(), setting -> setting.withAttachAs(attachAs)));
            case ALL -> {
                ConfiguratorPreset updated = attachAs.isNone() ? this : withEnergy(presentEnergy().withAttachAs(attachAs));
                yield updated
                        .withFluidModules(mapModules(presentFluidModules(), setting -> setting.withAttachAs(attachAs)))
                        .withItemModules(mapModules(presentItemModules(), setting -> setting.withAttachAs(attachAs)));
            }
        };
    }

    public ConfiguratorPreset resetCategory(ConfiguratorPresetCategory category) {
        return switch (category) {
            case ENERGY -> resetEnergy();
            case FLUID -> resetFluid();
            case ITEM -> resetItem();
            case ALL -> resetAll();
        };
    }

    private ConnectorSetting presentEnergy() {
        return energy.present() ? energy : ConnectorSetting.defaults(CableConnectorMode.BOTH.ordinal());
    }

    private List<ConnectorSetting> presentFluidModules() {
        return ensurePresentModules(fluidModules, FluidConnectorMode.INPUT.ordinal());
    }

    private List<ConnectorSetting> presentItemModules() {
        return ensurePresentModules(itemModules, ItemConnectorMode.INPUT.ordinal());
    }

    private static ConnectorSetting flipEnergy(ConnectorSetting setting) {
        if (!setting.present()) {
            return setting;
        }
        CableConnectorMode[] modes = CableConnectorMode.values();
        CableConnectorMode mode = setting.modeOrdinal() >= 0 && setting.modeOrdinal() < modes.length
                ? modes[setting.modeOrdinal()]
                : CableConnectorMode.BOTH;
        CableConnectorMode flipped = switch (mode) {
            case INPUT -> CableConnectorMode.OUTPUT;
            case OUTPUT -> CableConnectorMode.INPUT;
            case BOTH -> CableConnectorMode.BOTH;
        };
        return setting.withModeOrdinal(flipped.ordinal());
    }

    private static ConnectorSetting flipModule(ConnectorSetting setting, int modeCount) {
        if (!setting.present()) {
            return setting;
        }
        int next = setting.modeOrdinal() == 0 ? 1 : 0;
        return setting.withModeOrdinal(next % Math.max(1, modeCount));
    }

    private static ConnectorSetting cycleEnergy(ConnectorSetting setting) {
        return setting.withModeOrdinal((setting.modeOrdinal() + 1) % CableConnectorMode.values().length);
    }

    private static ConnectorSetting cycleFluid(ConnectorSetting setting) {
        return setting.withModeOrdinal((setting.modeOrdinal() + 1) % FluidConnectorMode.values().length);
    }

    private static ConnectorSetting cycleItem(ConnectorSetting setting) {
        return setting.withModeOrdinal((setting.modeOrdinal() + 1) % ItemConnectorMode.values().length);
    }

    private static boolean hasPresentModule(List<ConnectorSetting> modules) {
        for (ConnectorSetting module : modules) {
            if (module.present()) {
                return true;
            }
        }
        return false;
    }

    private static List<ConnectorSetting> absentModules() {
        List<ConnectorSetting> modules = new ArrayList<>(MODULE_COUNT);
        for (int index = 0; index < MODULE_COUNT; index++) {
            modules.add(ConnectorSetting.ABSENT);
        }
        return List.copyOf(modules);
    }

    private static List<ConnectorSetting> normalizeModules(List<ConnectorSetting> modules) {
        List<ConnectorSetting> normalized = new ArrayList<>(MODULE_COUNT);
        if (modules != null) {
            for (ConnectorSetting module : modules) {
                if (normalized.size() >= MODULE_COUNT) {
                    break;
                }
                normalized.add(module == null ? ConnectorSetting.ABSENT : module);
            }
        }
        while (normalized.size() < MODULE_COUNT) {
            normalized.add(ConnectorSetting.ABSENT);
        }
        return List.copyOf(normalized);
    }

    private static List<ConnectorSetting> ensurePresentModules(List<ConnectorSetting> modules, int defaultMode) {
        List<ConnectorSetting> present = new ArrayList<>(MODULE_COUNT);
        for (ConnectorSetting module : normalizeModules(modules)) {
            present.add(module.present() ? module : ConnectorSetting.defaults(defaultMode));
        }
        return List.copyOf(present);
    }

    private static List<ConnectorSetting> mapModules(
            List<ConnectorSetting> modules,
            Function<ConnectorSetting, ConnectorSetting> mapper
    ) {
        List<ConnectorSetting> mapped = new ArrayList<>(MODULE_COUNT);
        for (ConnectorSetting module : normalizeModules(modules)) {
            mapped.add(mapper.apply(module));
        }
        return List.copyOf(mapped);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        ConnectorSetting.STREAM_CODEC.encode(buffer, energy);
        writeModules(buffer, fluidModules);
        writeModules(buffer, itemModules);
        ByteBufCodecs.BOOL.encode(buffer, pasteModules);
        ByteBufCodecs.BOOL.encode(buffer, gearHelper);
    }

    private static ConfiguratorPreset read(RegistryFriendlyByteBuf buffer) {
        ConnectorSetting energy = ConnectorSetting.STREAM_CODEC.decode(buffer);
        List<ConnectorSetting> fluidModules = readModules(buffer);
        List<ConnectorSetting> itemModules = readModules(buffer);
        boolean pasteModules = ByteBufCodecs.BOOL.decode(buffer);
        boolean gearHelper = ByteBufCodecs.BOOL.decode(buffer);
        return new ConfiguratorPreset(energy, fluidModules, itemModules, pasteModules, gearHelper);
    }

    private static void writeModules(RegistryFriendlyByteBuf buffer, List<ConnectorSetting> modules) {
        List<ConnectorSetting> normalized = normalizeModules(modules);
        ByteBufCodecs.VAR_INT.encode(buffer, normalized.size());
        for (ConnectorSetting module : normalized) {
            ConnectorSetting.STREAM_CODEC.encode(buffer, module);
        }
    }

    private static List<ConnectorSetting> readModules(RegistryFriendlyByteBuf buffer) {
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        List<ConnectorSetting> modules = new ArrayList<>(Math.min(count, MODULE_COUNT));
        for (int index = 0; index < count; index++) {
            ConnectorSetting module = ConnectorSetting.STREAM_CODEC.decode(buffer);
            if (index < MODULE_COUNT) {
                modules.add(module);
            }
        }
        return normalizeModules(modules);
    }
}
