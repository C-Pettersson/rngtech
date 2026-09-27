package com.rngtech.content.minerscompanion;

import com.rngtech.content.item.AdvancedItemFilterItem;
import com.rngtech.content.item.BatteryCellItem;
import com.rngtech.content.item.CrushHeadItem;
import com.rngtech.content.item.PotentialReactorPartItem;
import com.rngtech.content.registry.ModItems;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record MinersCompanionState(
        boolean blockChewEnabled,
        boolean magnetEnabled,
        boolean miningLampEnabled,
        ItemStack crushHead,
        ItemStack batteryCell,
        ItemStack recoveryFilter,
        ItemStack magnet,
        ItemStack miningLamp,
        List<ItemStack> filters
) {
    public static final int FILTER_SLOT_COUNT = 10;
    public static final MinersCompanionState EMPTY = new MinersCompanionState(
            false,
            false,
            false,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            emptyFilters()
    );

    public static final Codec<MinersCompanionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(MinersCompanionState::blockChewEnabled),
            Codec.BOOL.optionalFieldOf("magnet_enabled", false).forGetter(MinersCompanionState::magnetEnabled),
            Codec.BOOL.optionalFieldOf("mining_lamp_enabled", false).forGetter(MinersCompanionState::miningLampEnabled),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("crush_head", ItemStack.EMPTY).forGetter(MinersCompanionState::crushHead),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("battery_cell", ItemStack.EMPTY).forGetter(MinersCompanionState::batteryCell),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("recovery_filter", ItemStack.EMPTY).forGetter(MinersCompanionState::recoveryFilter),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("magnet", ItemStack.EMPTY).forGetter(MinersCompanionState::magnet),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("mining_lamp", ItemStack.EMPTY).forGetter(MinersCompanionState::miningLamp),
            ItemStack.OPTIONAL_CODEC.listOf()
                    .optionalFieldOf("filters", emptyFilters())
                    .forGetter(MinersCompanionState::filters)
    ).apply(instance, MinersCompanionState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MinersCompanionState> STREAM_CODEC =
            StreamCodec.ofMember(MinersCompanionState::write, MinersCompanionState::read);

    public MinersCompanionState {
        crushHead = validCrushHead(crushHead) ? crushHead.copyWithCount(1) : ItemStack.EMPTY;
        batteryCell = BatteryCellItem.isBatteryCell(batteryCell) ? batteryCell.copyWithCount(1) : ItemStack.EMPTY;
        recoveryFilter = validRecoveryFilter(recoveryFilter) ? recoveryFilter.copyWithCount(1) : ItemStack.EMPTY;
        magnet = validMagnet(magnet) ? magnet.copyWithCount(1) : ItemStack.EMPTY;
        miningLamp = validMiningLamp(miningLamp) ? miningLamp.copyWithCount(1) : ItemStack.EMPTY;
        boolean hasCell = BatteryCellItem.isBatteryCell(batteryCell);
        blockChewEnabled = blockChewEnabled && validCrushHead(crushHead) && hasCell;
        magnetEnabled = magnetEnabled && validMagnet(magnet) && hasCell;
        miningLampEnabled = miningLampEnabled && validMiningLamp(miningLamp) && hasCell;
        filters = normalizeFilters(filters);
    }

    public boolean enabled() {
        return blockChewEnabled;
    }

    public MinersCompanionState withEnabled(boolean nextEnabled) {
        return withBlockChewEnabled(nextEnabled);
    }

    public MinersCompanionState withBlockChewEnabled(boolean nextEnabled) {
        return new MinersCompanionState(nextEnabled, magnetEnabled, miningLampEnabled, crushHead, batteryCell, recoveryFilter, magnet, miningLamp, filters);
    }

    public MinersCompanionState withMagnetEnabled(boolean nextEnabled) {
        return new MinersCompanionState(blockChewEnabled, nextEnabled, miningLampEnabled, crushHead, batteryCell, recoveryFilter, magnet, miningLamp, filters);
    }

    public MinersCompanionState withMiningLampEnabled(boolean nextEnabled) {
        return new MinersCompanionState(blockChewEnabled, magnetEnabled, nextEnabled, crushHead, batteryCell, recoveryFilter, magnet, miningLamp, filters);
    }

    public MinersCompanionState withCrushHead(ItemStack stack) {
        return new MinersCompanionState(blockChewEnabled, magnetEnabled, miningLampEnabled, stack, batteryCell, recoveryFilter, magnet, miningLamp, filters);
    }

    public MinersCompanionState withBatteryCell(ItemStack stack) {
        return new MinersCompanionState(blockChewEnabled, magnetEnabled, miningLampEnabled, crushHead, stack, recoveryFilter, magnet, miningLamp, filters);
    }

    public MinersCompanionState withRecoveryFilter(ItemStack stack) {
        return new MinersCompanionState(blockChewEnabled, magnetEnabled, miningLampEnabled, crushHead, batteryCell, stack, magnet, miningLamp, filters);
    }

    public MinersCompanionState withMagnet(ItemStack stack) {
        return new MinersCompanionState(blockChewEnabled, magnetEnabled, miningLampEnabled, crushHead, batteryCell, recoveryFilter, stack, miningLamp, filters);
    }

    public MinersCompanionState withMiningLamp(ItemStack stack) {
        return new MinersCompanionState(blockChewEnabled, magnetEnabled, miningLampEnabled, crushHead, batteryCell, recoveryFilter, magnet, stack, filters);
    }

    public MinersCompanionState withFilter(int index, ItemStack stack) {
        if (index < 0 || index >= FILTER_SLOT_COUNT) {
            return this;
        }
        List<ItemStack> nextFilters = new ArrayList<>(filters);
        nextFilters.set(index, normalizeFilterStack(stack));
        return new MinersCompanionState(blockChewEnabled, magnetEnabled, miningLampEnabled, crushHead, batteryCell, recoveryFilter, magnet, miningLamp, nextFilters);
    }

    public ItemStack filter(int index) {
        return index >= 0 && index < filters.size() ? filters.get(index) : ItemStack.EMPTY;
    }

    public boolean hasValidGear() {
        return validCrushHead(crushHead) && BatteryCellItem.isBatteryCell(batteryCell);
    }

    public boolean hasValidMagnetGear() {
        return validMagnet(magnet) && BatteryCellItem.isBatteryCell(batteryCell);
    }

    public boolean hasValidMiningLampGear() {
        return validMiningLamp(miningLamp) && BatteryCellItem.isBatteryCell(batteryCell);
    }

    public int processingLevel() {
        return crushHead.getItem() instanceof CrushHeadItem head ? head.material().processingLevel() : 0;
    }

    public int recoveryFilterStage() {
        return recoveryFilter.getItem() instanceof PotentialReactorPartItem part && part.filterMaterial() != null
                ? part.stage()
                : 0;
    }

    public static boolean validCrushHead(ItemStack stack) {
        return stack.getItem() instanceof CrushHeadItem;
    }

    public static boolean validRecoveryFilter(ItemStack stack) {
        return stack.getItem() instanceof PotentialReactorPartItem part && part.filterMaterial() != null;
    }

    public static boolean validMagnet(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.MINERS_COMPANION_MAGNET.get());
    }

    public static boolean validMiningLamp(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.MINING_LAMP.get());
    }

    public static boolean validFilterStack(ItemStack stack) {
        return stack.getItem() instanceof BlockItem || AdvancedItemFilterItem.isAdvancedFilter(stack);
    }

    public static ItemStack normalizeFilterStack(ItemStack stack) {
        return stack == null || stack.isEmpty() || !validFilterStack(stack) ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    public static List<ItemStack> emptyFilters() {
        List<ItemStack> filters = new ArrayList<>(FILTER_SLOT_COUNT);
        for (int index = 0; index < FILTER_SLOT_COUNT; index++) {
            filters.add(ItemStack.EMPTY);
        }
        return List.copyOf(filters);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        ByteBufCodecs.BOOL.encode(buffer, blockChewEnabled);
        ByteBufCodecs.BOOL.encode(buffer, magnetEnabled);
        ByteBufCodecs.BOOL.encode(buffer, miningLampEnabled);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, crushHead);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, batteryCell);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recoveryFilter);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, magnet);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, miningLamp);
        writeFilters(buffer, filters);
    }

    private static MinersCompanionState read(RegistryFriendlyByteBuf buffer) {
        return new MinersCompanionState(
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                readFilters(buffer)
        );
    }

    private static List<ItemStack> normalizeFilters(List<ItemStack> filters) {
        List<ItemStack> normalized = new ArrayList<>(FILTER_SLOT_COUNT);
        if (filters != null) {
            for (ItemStack filter : filters) {
                if (normalized.size() >= FILTER_SLOT_COUNT) {
                    break;
                }
                normalized.add(normalizeFilterStack(filter));
            }
        }
        while (normalized.size() < FILTER_SLOT_COUNT) {
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
        List<ItemStack> filters = new ArrayList<>(Math.min(count, FILTER_SLOT_COUNT));
        for (int index = 0; index < count; index++) {
            ItemStack filter = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            if (index < FILTER_SLOT_COUNT) {
                filters.add(filter);
            }
        }
        return normalizeFilters(filters);
    }
}
