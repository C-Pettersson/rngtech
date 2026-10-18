package com.rngtech.content.item;

import com.rngtech.content.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * Urea from the Ammonia Synthesizer: bone meal for any plant by hand or dispenser, worth four bone meal in a Forestry
 * Companion's store, and a growth boost in the Algae Photobioreactor.
 */
public class UreaItem extends BoneMealItem {
    /** Bone meal one Urea counts as in a Forestry Companion or station store. */
    public static final int FERTILIZER_UNITS = 4;

    public UreaItem(Properties properties) {
        super(properties);
    }

    public static void registerDispenserBehavior() {
        DispenserBlock.registerBehavior(ModItems.UREA.get(), new OptionalDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                BlockPos target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                setSuccess(BoneMealItem.growCrop(stack, source.level(), target));
                if (isSuccess()) {
                    source.level().levelEvent(1505, target, 15);
                }
                return stack;
            }
        });
    }
}
