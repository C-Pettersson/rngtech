package com.rngtech.content.menu;

import com.rngtech.content.item.AdvancedItemFilterItem;
import com.rngtech.content.itemfilter.AdvancedItemFilterSettings;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;
import com.rngtech.content.registry.ModMenus;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class AdvancedItemFilterMenu extends AbstractContainerMenu {
    public static final int BUTTON_MODE = 0;
    public static final int BUTTON_EXACT = 1;
    public static final int BUTTON_TAGS = 2;
    public static final int BUTTON_NAMESPACES = 3;
    public static final int BUTTON_STAGE = 4;
    public static final int BUTTON_STABILITY = 5;
    public static final int BUTTON_STRICT = 6;
    public static final int BUTTON_IDENTITY = 7;
    public static final int BUTTON_RARITY = 8;
    public static final int BUTTON_MIN_STAGE_DOWN = 20;
    public static final int BUTTON_MIN_STAGE_UP = 21;
    public static final int BUTTON_MAX_STAGE_DOWN = 22;
    public static final int BUTTON_MAX_STAGE_UP = 23;
    public static final int BUTTON_MIN_STABILITY_DOWN = 24;
    public static final int BUTTON_MIN_STABILITY_UP = 25;
    public static final int BUTTON_MAX_STABILITY_DOWN = 26;
    public static final int BUTTON_MAX_STABILITY_UP = 27;
    public static final int BUTTON_TAG_CYCLE_BASE = 100;
    public static final int BUTTON_TAG_CLEAR_BASE = 110;
    public static final int BUTTON_NAMESPACE_CYCLE_BASE = 120;
    public static final int BUTTON_NAMESPACE_CLEAR_BASE = 130;
    public static final int STABILITY_STEP = 5;

    private static final int SAMPLE_SLOT_COUNT = AdvancedItemFilterSettings.SAMPLE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = SAMPLE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;
    public static final int PLAYER_INVENTORY_X = 8;
    public static final int PLAYER_INVENTORY_Y = 188;
    public static final int HOTBAR_Y = 244;

    private final Inventory inventory;
    private final InteractionHand hand;
    private final ItemStackHandler sampleInventory;
    private boolean loading;

    public AdvancedItemFilterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    public AdvancedItemFilterMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenus.ADVANCED_ITEM_FILTER.get(), containerId);
        this.inventory = playerInventory;
        this.hand = hand;
        this.sampleInventory = createSampleInventory();
        loadFromStack();

        for (int index = 0; index < SAMPLE_SLOT_COUNT; index++) {
            int row = index / 3;
            int column = index % 3;
            addSlot(new GhostSampleSlot(sampleInventory, index, 20 + column * 18, 36 + row * 18, index));
        }
        addPlayerInventory(playerInventory);
    }

    public InteractionHand hand() {
        return hand;
    }

    public ItemStack filterStack() {
        return inventory.player.getItemInHand(hand);
    }

    public AdvancedItemFilterSettings settings() {
        return AdvancedItemFilterItem.settings(filterStack());
    }

    public List<ResourceLocation> tagCandidates() {
        TreeSet<ResourceLocation> candidates = new TreeSet<>(java.util.Comparator.comparing(ResourceLocation::toString));
        candidates.addAll(settings().tags());
        for (ItemStack sample : sampleStacks()) {
            if (sample.isEmpty()) {
                continue;
            }
            sample.getItem().builtInRegistryHolder().tags().map(TagKey::location).forEach(candidates::add);
        }
        return List.copyOf(candidates);
    }

    public List<String> namespaceCandidates() {
        TreeSet<String> candidates = new TreeSet<>();
        candidates.addAll(settings().namespaces());
        for (ItemStack sample : sampleStacks()) {
            if (sample.isEmpty()) {
                continue;
            }
            candidates.add(BuiltInRegistries.ITEM.getKey(sample.getItem()).getNamespace());
        }
        return List.copyOf(candidates);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!isKnownButton(id) || !holdsAdvancedFilter(player)) {
            return false;
        }
        AdvancedItemFilterSettings current = settings();
        AdvancedItemFilterSettings updated = switch (id) {
            case BUTTON_MODE -> current.withMode(current.mode().next());
            case BUTTON_EXACT -> current.withExactItemsEnabled(!current.exactItemsEnabled());
            case BUTTON_TAGS -> current.withTagEnabled(!current.tagEnabled());
            case BUTTON_NAMESPACES -> current.withNamespaceEnabled(!current.namespaceEnabled());
            case BUTTON_STAGE -> current.withStageEnabled(!current.stageEnabled());
            case BUTTON_STABILITY -> current.withStabilityEnabled(!current.stabilityEnabled());
            case BUTTON_STRICT -> current.withStrictComponents(!current.strictComponents());
            case BUTTON_IDENTITY -> current.withIdentityMode(current.identityMode().next());
            case BUTTON_RARITY -> current.withRarityMode(current.rarityMode().next());
            case BUTTON_MIN_STAGE_DOWN -> current.withStageRange(current.minStage() - 1, current.maxStage());
            case BUTTON_MIN_STAGE_UP -> current.withStageRange(Math.min(current.minStage() + 1, current.maxStage()), current.maxStage());
            case BUTTON_MAX_STAGE_DOWN -> current.withStageRange(current.minStage(), Math.max(current.maxStage() - 1, current.minStage()));
            case BUTTON_MAX_STAGE_UP -> current.withStageRange(current.minStage(), current.maxStage() + 1);
            case BUTTON_MIN_STABILITY_DOWN -> current.withStabilityRange(current.minStability() - STABILITY_STEP, current.maxStability());
            case BUTTON_MIN_STABILITY_UP -> current.withStabilityRange(
                    Math.min(current.minStability() + STABILITY_STEP, current.maxStability()),
                    current.maxStability()
            );
            case BUTTON_MAX_STABILITY_DOWN -> current.withStabilityRange(
                    current.minStability(),
                    Math.max(current.maxStability() - STABILITY_STEP, current.minStability())
            );
            case BUTTON_MAX_STABILITY_UP -> current.withStabilityRange(current.minStability(), current.maxStability() + STABILITY_STEP);
            default -> handleIndexedButton(id, current);
        };
        if (updated == null) {
            return false;
        }
        filterStack().set(ModDataComponents.ADVANCED_ITEM_FILTER.get(), updated);
        syncHeldStack(player);
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof GhostSampleSlot sampleSlot) {
            handleGhostSampleClick(sampleSlot, button, clickType);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return holdsAdvancedFilter(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private AdvancedItemFilterSettings handleIndexedButton(int id, AdvancedItemFilterSettings current) {
        int tagCycleSlot = id - BUTTON_TAG_CYCLE_BASE;
        if (tagCycleSlot >= 0 && tagCycleSlot < AdvancedItemFilterSettings.MAX_TAGS) {
            return current.cycleTagSlot(tagCycleSlot, tagCandidates());
        }
        int tagClearSlot = id - BUTTON_TAG_CLEAR_BASE;
        if (tagClearSlot >= 0 && tagClearSlot < AdvancedItemFilterSettings.MAX_TAGS) {
            return current.clearTagSlot(tagClearSlot);
        }
        int namespaceCycleSlot = id - BUTTON_NAMESPACE_CYCLE_BASE;
        if (namespaceCycleSlot >= 0 && namespaceCycleSlot < AdvancedItemFilterSettings.MAX_NAMESPACES) {
            return current.cycleNamespaceSlot(namespaceCycleSlot, namespaceCandidates());
        }
        int namespaceClearSlot = id - BUTTON_NAMESPACE_CLEAR_BASE;
        if (namespaceClearSlot >= 0 && namespaceClearSlot < AdvancedItemFilterSettings.MAX_NAMESPACES) {
            return current.clearNamespaceSlot(namespaceClearSlot);
        }
        return null;
    }

    private void handleGhostSampleClick(GhostSampleSlot slot, int button, ClickType clickType) {
        if (clickType != ClickType.PICKUP) {
            return;
        }
        ItemStack carried = getCarried();
        if (button == 1 || carried.isEmpty()) {
            slot.clearSample();
            return;
        }
        slot.setSample(carried);
    }

    private ItemStackHandler createSampleInventory() {
        return new ItemStackHandler(SAMPLE_SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !stack.isEmpty();
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                saveSamplesToStack();
            }
        };
    }

    private void loadFromStack() {
        loading = true;
        try {
            AdvancedItemFilterSettings settings = settings();
            for (int index = 0; index < SAMPLE_SLOT_COUNT; index++) {
                sampleInventory.setStackInSlot(index, settings.samples().get(index));
            }
        } finally {
            loading = false;
        }
    }

    private void saveSamplesToStack() {
        if (loading || inventory.player.level().isClientSide || !holdsAdvancedFilter(inventory.player)) {
            return;
        }
        filterStack().set(ModDataComponents.ADVANCED_ITEM_FILTER.get(), settings().withSamples(sampleStacks()));
        syncHeldStack(inventory.player);
    }

    private List<ItemStack> sampleStacks() {
        List<ItemStack> samples = new ArrayList<>(SAMPLE_SLOT_COUNT);
        for (int index = 0; index < SAMPLE_SLOT_COUNT; index++) {
            samples.add(sampleInventory.getStackInSlot(index));
        }
        return samples;
    }

    private void setSample(int index, ItemStack stack) {
        sampleInventory.setStackInSlot(index, stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    private boolean holdsAdvancedFilter(Player player) {
        return player.getItemInHand(hand).is(ModItems.ADVANCED_ITEM_FILTER.get());
    }

    private static boolean isKnownButton(int id) {
        if (id >= BUTTON_MODE && id <= BUTTON_RARITY) {
            return true;
        }
        if (id >= BUTTON_MIN_STAGE_DOWN && id <= BUTTON_MAX_STABILITY_UP) {
            return true;
        }
        int tagCycle = id - BUTTON_TAG_CYCLE_BASE;
        if (tagCycle >= 0 && tagCycle < AdvancedItemFilterSettings.MAX_TAGS) {
            return true;
        }
        int tagClear = id - BUTTON_TAG_CLEAR_BASE;
        if (tagClear >= 0 && tagClear < AdvancedItemFilterSettings.MAX_TAGS) {
            return true;
        }
        int namespaceCycle = id - BUTTON_NAMESPACE_CYCLE_BASE;
        if (namespaceCycle >= 0 && namespaceCycle < AdvancedItemFilterSettings.MAX_NAMESPACES) {
            return true;
        }
        int namespaceClear = id - BUTTON_NAMESPACE_CLEAR_BASE;
        return namespaceClear >= 0 && namespaceClear < AdvancedItemFilterSettings.MAX_NAMESPACES;
    }

    private void syncHeldStack(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        int slot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : serverPlayer.getInventory().selected;
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                -2,
                0,
                slot,
                serverPlayer.getItemInHand(hand).copy()
        ));
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        PLAYER_INVENTORY_X + column * 18,
                        PLAYER_INVENTORY_Y + row * 18
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, PLAYER_INVENTORY_X + column * 18, HOTBAR_Y));
        }
    }

    private final class GhostSampleSlot extends SlotItemHandler {
        private final int sampleIndex;

        private GhostSampleSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition, int sampleIndex) {
            super(itemHandler, index, xPosition, yPosition);
            this.sampleIndex = sampleIndex;
        }

        private void setSample(ItemStack stack) {
            AdvancedItemFilterMenu.this.setSample(sampleIndex, stack);
            setChanged();
        }

        private void clearSample() {
            AdvancedItemFilterMenu.this.setSample(sampleIndex, ItemStack.EMPTY);
            setChanged();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
