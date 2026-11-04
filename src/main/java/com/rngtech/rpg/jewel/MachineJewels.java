package com.rngtech.rpg.jewel;

import com.rngtech.util.ItemStackValues;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The jewels a machine holds: {@code tree} sits in its Mastery tree's Jewel Sockets, {@code body} in the sockets on
 * the machine itself. Each list is a slot row; a slot past the machine's current socket count keeps its jewel but does
 * nothing, so a refund never drops an item. Both rows travel with the machine when it is broken; Soul Transfer carries
 * the tree row with the Mastery.
 */
public record MachineJewels(List<ItemStack> tree, List<ItemStack> body) {
    public static final int TREE_SLOTS = 9;
    public static final int BODY_SLOTS = 4;
    public static final MachineJewels EMPTY = new MachineJewels(List.of(), List.of());

    public static final Codec<MachineJewels> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.sizeLimitedListOf(TREE_SLOTS).optionalFieldOf("tree", List.of()).forGetter(MachineJewels::tree),
            ItemStack.OPTIONAL_CODEC.sizeLimitedListOf(BODY_SLOTS).optionalFieldOf("body", List.of()).forGetter(MachineJewels::body)
    ).apply(instance, MachineJewels::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineJewels> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, MachineJewels::tree,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, MachineJewels::body,
            MachineJewels::new
    );

    public MachineJewels {
        tree = trimmed(tree, TREE_SLOTS);
        body = trimmed(body, BODY_SLOTS);
    }

    /** Copies at most {@code limit} stacks and drops the empty tail, so an untouched row stays an empty list. */
    private static List<ItemStack> trimmed(List<ItemStack> stacks, int limit) {
        List<ItemStack> copy = new ArrayList<>();
        for (int index = 0; index < Math.min(stacks.size(), limit); index++) {
            copy.add(stacks.get(index).copy());
        }
        while (!copy.isEmpty() && copy.getLast().isEmpty()) {
            copy.removeLast();
        }
        return List.copyOf(copy);
    }

    public ItemStack tree(int slot) {
        return slot >= 0 && slot < tree.size() ? tree.get(slot) : ItemStack.EMPTY;
    }

    public ItemStack body(int slot) {
        return slot >= 0 && slot < body.size() ? body.get(slot) : ItemStack.EMPTY;
    }

    public MachineJewels withTree(int slot, ItemStack stack) {
        return new MachineJewels(withSlot(tree, slot, stack, TREE_SLOTS), body);
    }

    public MachineJewels withBody(int slot, ItemStack stack) {
        return new MachineJewels(tree, withSlot(body, slot, stack, BODY_SLOTS));
    }

    /** The tree row moved onto another machine's jewels, for Soul Transfer. */
    public MachineJewels withTree(List<ItemStack> stacks) {
        return new MachineJewels(stacks, body);
    }

    private static List<ItemStack> withSlot(List<ItemStack> stacks, int slot, ItemStack stack, int limit) {
        if (slot < 0 || slot >= limit) {
            throw new IllegalArgumentException("Jewel slot out of range: " + slot);
        }
        List<ItemStack> copy = new ArrayList<>(stacks);
        while (copy.size() <= slot) {
            copy.add(ItemStack.EMPTY);
        }
        copy.set(slot, stack.copy());
        return copy;
    }

    public boolean isEmpty() {
        return tree.isEmpty() && body.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MachineJewels jewels && ItemStackValues.matches(tree, jewels.tree) && ItemStackValues.matches(body, jewels.body);
    }

    @Override
    public int hashCode() {
        return 31 * ItemStackValues.hash(tree) + ItemStackValues.hash(body);
    }
}
