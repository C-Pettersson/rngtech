package com.rngtech.mixin.refinedstorage;

import com.rngtech.compat.refinedstorage.RefinedStorageGridRefill;

import com.refinedmods.refinedstorage.common.grid.CraftingGrid;
import com.refinedmods.refinedstorage.common.grid.ExtractTransaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.refinedmods.refinedstorage.common.grid.CraftingGridResultSlot", remap = false)
public abstract class CraftingGridResultSlotMixin {
    @Shadow
    @Final
    private CraftingGrid craftingGrid;

    @Inject(
            method = "useIngredient",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/common/grid/CraftingGridResultSlot;decrementMatrixSlot(I)Lnet/minecraft/world/item/ItemStack;"
            ),
            cancellable = true
    )
    private void rngtech$refillCalibrated(
            Player player, ExtractTransaction transaction, int slot, ItemStack stack, CallbackInfo callback) {
        if (stack.getCount() == 1 && RefinedStorageGridRefill.refillCalibrated(craftingGrid, player, slot, stack)) {
            callback.cancel();
        }
    }
}
