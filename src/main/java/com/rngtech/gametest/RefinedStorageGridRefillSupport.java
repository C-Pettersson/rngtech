package com.rngtech.gametest;

import com.refinedmods.refinedstorage.api.autocrafting.calculation.CancellationToken;
import com.refinedmods.refinedstorage.api.autocrafting.preview.Preview;
import com.refinedmods.refinedstorage.api.autocrafting.preview.TreePreview;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskId;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.node.grid.GridOperations;
import com.refinedmods.refinedstorage.api.network.node.grid.GridWatcher;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.api.storage.TrackedResourceAmount;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;
import com.refinedmods.refinedstorage.common.grid.CraftingGrid;
import com.refinedmods.refinedstorage.common.grid.ExtractTransaction;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** A Refined Storage crafting grid on an in-memory network storage, crafted through Refined Storage's own result slot. */
final class RefinedStorageGridRefillSupport {
    private static final String RESULT_SLOT = "com.refinedmods.refinedstorage.common.grid.CraftingGridResultSlot";

    private final TestGrid grid = new TestGrid();

    void store(ItemStack stack) {
        grid.storage.insert(ItemResource.ofItemStack(stack), stack.getCount(), Action.EXECUTE, new PlayerActor("test"));
    }

    void setMatrix(int slot, ItemStack stack) {
        grid.matrix.setItem(slot, stack);
    }

    ItemStack matrix(int slot) {
        return grid.matrix.getItem(slot);
    }

    long stored(ItemStack stack) {
        ItemResource resource = ItemResource.ofItemStack(stack);
        return grid.storage.getAll().stream()
                .filter(stored -> stored.resource().equals(resource))
                .mapToLong(stored -> stored.amount())
                .sum();
    }

    void craft(Player player, ItemStack result) throws ReflectiveOperationException {
        var constructor = Class.forName(RESULT_SLOT).getDeclaredConstructor(Player.class, CraftingGrid.class, int.class, int.class);
        constructor.setAccessible(true);
        ((Slot) constructor.newInstance(player, grid, 0, 0)).onTake(player, result);
    }

    private static final class TestGrid implements CraftingGrid {
        private final Storage storage = new StorageImpl();
        private final RecipeMatrixContainer matrix = new RecipeMatrixContainer(() -> {
        }, 3, 3);
        private final ResultContainer result = new ResultContainer();

        @Override
        public RecipeMatrixContainer getCraftingMatrix() {
            return matrix;
        }

        @Override
        public ResultContainer getCraftingResult() {
            return result;
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(Player player, CraftingInput input) {
            return NonNullList.withSize(input.size(), ItemStack.EMPTY);
        }

        @Override
        public ExtractTransaction startExtractTransaction(Player player, boolean directCommit) {
            return new ExtractTransaction() {
                @Override
                public boolean extract(ItemResource resource, Player extractor) {
                    return storage.extract(resource, 1, Action.EXECUTE, new PlayerActor(extractor)) == 1;
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public boolean clearMatrix(Player player, boolean toPlayerInventory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void transferRecipe(Player player, List<List<ItemResource>> recipe) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void acceptQuickCraft(Player player, ItemStack craftedStack) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addWatcher(GridWatcher watcher, Class<? extends Actor> actorType) {
        }

        @Override
        public void removeWatcher(GridWatcher watcher) {
        }

        @Override
        public Storage getItemStorage() {
            return storage;
        }

        @Override
        public boolean isGridActive() {
            return true;
        }

        @Override
        public List<TrackedResourceAmount> getResources(Class<? extends Actor> actorType) {
            return List.of();
        }

        @Override
        public Set<PlatformResourceKey> getAutocraftableResources() {
            return Set.of();
        }

        @Override
        public GridOperations createOperations(ResourceType resourceType, ServerPlayer player) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canMenuStayOpen(Player player) {
            return true;
        }

        @Override
        public CompletableFuture<Optional<Preview>> getPreview(ResourceKey resource, long amount, CancellationToken token) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public CompletableFuture<Optional<TreePreview>> getTreePreview(
                ResourceKey resource, long amount, CancellationToken token) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public CompletableFuture<Long> getMaxAmount(ResourceKey resource, CancellationToken token) {
            return CompletableFuture.completedFuture(0L);
        }

        @Override
        public Optional<TaskId> startTask(
                ResourceKey resource, long amount, Actor actor, boolean notify, CancellationToken token) {
            return Optional.empty();
        }
    }
}
