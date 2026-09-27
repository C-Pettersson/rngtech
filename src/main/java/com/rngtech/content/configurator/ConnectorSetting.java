package com.rngtech.content.configurator;

import com.rngtech.content.blockentity.UniversalConnectorBlockEntity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record ConnectorSetting(
        boolean present,
        int channel,
        int modeOrdinal,
        int distributionOrdinal,
        RelativeDirection attachAs,
        ItemStack module,
        List<ItemStack> filters
) {
    public static final ConnectorSetting ABSENT =
            new ConnectorSetting(false, 0, 0, 0, RelativeDirection.DEFAULT, ItemStack.EMPTY, emptyFilters());

    public static final Codec<ConnectorSetting> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("present", false).forGetter(ConnectorSetting::present),
            Codec.intRange(0, UniversalConnectorBlockEntity.MAX_CHANNEL)
                    .optionalFieldOf("channel", 0)
                    .forGetter(ConnectorSetting::channel),
            Codec.INT.optionalFieldOf("mode", 0).forGetter(ConnectorSetting::modeOrdinal),
            Codec.INT.optionalFieldOf("distribution", 0).forGetter(ConnectorSetting::distributionOrdinal),
            RelativeDirection.CODEC.optionalFieldOf("attach_as", RelativeDirection.DEFAULT)
                    .forGetter(ConnectorSetting::attachAs),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("module", ItemStack.EMPTY).forGetter(ConnectorSetting::module),
            ItemStack.OPTIONAL_CODEC.listOf()
                    .optionalFieldOf("filters", emptyFilters())
                    .forGetter(ConnectorSetting::filters)
    ).apply(instance, ConnectorSetting::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConnectorSetting> STREAM_CODEC =
            StreamCodec.ofMember(ConnectorSetting::write, ConnectorSetting::read);

    public ConnectorSetting {
        channel = Mth.clamp(channel, 0, UniversalConnectorBlockEntity.MAX_CHANNEL);
        modeOrdinal = Math.max(0, modeOrdinal);
        distributionOrdinal = Math.max(0, distributionOrdinal);
        attachAs = attachAs == null ? RelativeDirection.DEFAULT : attachAs;
        module = module == null || module.isEmpty() ? ItemStack.EMPTY : module.copyWithCount(1);
        filters = normalizeFilters(filters);
    }

    public static ConnectorSetting of(int channel, int modeOrdinal, Direction attachAs, Direction defaultAttachAs, ItemStack module) {
        return of(channel, modeOrdinal, 0, attachAs, defaultAttachAs, module, emptyFilters());
    }

    public static ConnectorSetting of(
            int channel,
            int modeOrdinal,
            int distributionOrdinal,
            Direction attachAs,
            Direction defaultAttachAs,
            ItemStack module
    ) {
        return of(channel, modeOrdinal, distributionOrdinal, attachAs, defaultAttachAs, module, emptyFilters());
    }

    public static ConnectorSetting of(
            int channel,
            int modeOrdinal,
            int distributionOrdinal,
            Direction attachAs,
            Direction defaultAttachAs,
            ItemStack module,
            List<ItemStack> filters
    ) {
        return new ConnectorSetting(
                true,
                channel,
                modeOrdinal,
                distributionOrdinal,
                RelativeDirection.capture(attachAs, defaultAttachAs),
                module,
                filters
        );
    }

    public static ConnectorSetting defaults(int modeOrdinal) {
        return new ConnectorSetting(true, 0, modeOrdinal, 0, RelativeDirection.DEFAULT, ItemStack.EMPTY, emptyFilters());
    }

    public ConnectorSetting withChannel(int nextChannel) {
        return new ConnectorSetting(true, nextChannel, modeOrdinal, distributionOrdinal, attachAs, module, filters);
    }

    public ConnectorSetting withModeOrdinal(int nextModeOrdinal) {
        return new ConnectorSetting(true, channel, nextModeOrdinal, distributionOrdinal, attachAs, module, filters);
    }

    public ConnectorSetting withDistributionOrdinal(int nextDistributionOrdinal) {
        return new ConnectorSetting(true, channel, modeOrdinal, nextDistributionOrdinal, attachAs, module, filters);
    }

    public ConnectorSetting withAttachAs(RelativeDirection nextAttachAs) {
        return new ConnectorSetting(true, channel, modeOrdinal, distributionOrdinal, nextAttachAs, module, filters);
    }

    public ConnectorSetting withoutModule() {
        return new ConnectorSetting(present, channel, modeOrdinal, distributionOrdinal, attachAs, ItemStack.EMPTY, filters);
    }

    public ItemStack filter(int index) {
        return index >= 0 && index < filters.size() ? filters.get(index) : ItemStack.EMPTY;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        ByteBufCodecs.BOOL.encode(buffer, present);
        ByteBufCodecs.VAR_INT.encode(buffer, channel);
        ByteBufCodecs.VAR_INT.encode(buffer, modeOrdinal);
        ByteBufCodecs.VAR_INT.encode(buffer, distributionOrdinal);
        RelativeDirection.STREAM_CODEC.encode(buffer, attachAs);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, module);
        writeFilters(buffer, filters);
    }

    private static ConnectorSetting read(RegistryFriendlyByteBuf buffer) {
        return new ConnectorSetting(
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                RelativeDirection.STREAM_CODEC.decode(buffer),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                readFilters(buffer)
        );
    }

    private static List<ItemStack> emptyFilters() {
        List<ItemStack> filters = new ArrayList<>(UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE);
        for (int index = 0; index < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE; index++) {
            filters.add(ItemStack.EMPTY);
        }
        return List.copyOf(filters);
    }

    private static List<ItemStack> normalizeFilters(List<ItemStack> filters) {
        List<ItemStack> normalized = new ArrayList<>(UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE);
        if (filters != null) {
            for (ItemStack filter : filters) {
                if (normalized.size() >= UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE) {
                    break;
                }
                normalized.add(filter == null || filter.isEmpty() ? ItemStack.EMPTY : filter.copyWithCount(1));
            }
        }
        while (normalized.size() < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE) {
            normalized.add(ItemStack.EMPTY);
        }
        return List.copyOf(normalized);
    }

    private static void writeFilters(RegistryFriendlyByteBuf buffer, List<ItemStack> filters) {
        List<ItemStack> normalized = normalizeFilters(filters);
        ByteBufCodecs.VAR_INT.encode(buffer, normalized.size());
        for (ItemStack filter : normalized) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, filter);
        }
    }

    private static List<ItemStack> readFilters(RegistryFriendlyByteBuf buffer) {
        int count = ByteBufCodecs.VAR_INT.decode(buffer);
        List<ItemStack> filters = new ArrayList<>(Math.min(count, UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE));
        for (int index = 0; index < count; index++) {
            ItemStack filter = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            if (index < UniversalConnectorBlockEntity.FILTER_SLOTS_PER_MODULE) {
                filters.add(filter);
            }
        }
        return normalizeFilters(filters);
    }
}
