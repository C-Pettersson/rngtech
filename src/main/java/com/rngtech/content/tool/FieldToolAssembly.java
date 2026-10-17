package com.rngtech.content.tool;

import com.rngtech.content.item.ToolHeadItem;
import com.rngtech.content.item.ToolRodItem;
import com.rngtech.util.ItemStackValues;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public record FieldToolAssembly(ItemStack head, ItemStack rod, ItemStack batteryCell) {
    public static final FieldToolAssembly EMPTY = new FieldToolAssembly(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);

    public static final Codec<FieldToolAssembly> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.OPTIONAL_CODEC.fieldOf("head").forGetter(FieldToolAssembly::head),
                    ItemStack.OPTIONAL_CODEC.fieldOf("rod").forGetter(FieldToolAssembly::rod),
                    ItemStack.OPTIONAL_CODEC.fieldOf("battery_cell").forGetter(FieldToolAssembly::batteryCell)
            )
            .apply(instance, FieldToolAssembly::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FieldToolAssembly> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,
            FieldToolAssembly::head,
            ItemStack.OPTIONAL_STREAM_CODEC,
            FieldToolAssembly::rod,
            ItemStack.OPTIONAL_STREAM_CODEC,
            FieldToolAssembly::batteryCell,
            FieldToolAssembly::new
    );

    public FieldToolAssembly {
        head = copySingle(head);
        rod = copySingle(rod);
        batteryCell = copySingle(batteryCell);
    }

    public boolean isValid() {
        return head.getItem() instanceof ToolHeadItem && rod.getItem() instanceof ToolRodItem;
    }

    public boolean isValidFor(ToolHeadFamily family) {
        return isValid() && family() == family;
    }

    public ToolHeadFamily family() {
        return head.getItem() instanceof ToolHeadItem headItem ? headItem.family() : ToolHeadFamily.PICK;
    }

    public ToolHeadMaterial headMaterial() {
        return head.getItem() instanceof ToolHeadItem headItem ? headItem.material() : ToolHeadMaterial.FLINT;
    }

    public ToolRodMaterial rodMaterial() {
        return rod.getItem() instanceof ToolRodItem rodItem ? rodItem.material() : ToolRodMaterial.WOODEN;
    }

    public FieldToolAssembly withBatteryCell(ItemStack stack) {
        return new FieldToolAssembly(head, rod, stack);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FieldToolAssembly that
                && ItemStackValues.matches(head, that.head)
                && ItemStackValues.matches(rod, that.rod)
                && ItemStackValues.matches(batteryCell, that.batteryCell);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ItemStackValues.hash(head), ItemStackValues.hash(rod), ItemStackValues.hash(batteryCell));
    }

    private static ItemStack copySingle(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }
}
