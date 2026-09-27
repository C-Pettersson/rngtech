package com.rngtech.content.item;

import com.rngtech.content.material.MaterialCatalog;
import com.rngtech.content.registry.ModDataComponents;
import com.rngtech.content.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public class MalformedIngotItem extends Item {
    public MalformedIngotItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(String material) {
        ItemStack stack = new ItemStack(ModItems.MALFORMED_INGOT.get());
        if (material != null && !material.isBlank()) {
            stack.set(ModDataComponents.MATERIAL.get(), material.toLowerCase(Locale.ROOT));
        }
        return stack;
    }

    public static boolean isMalformedIngot(ItemStack stack) {
        return stack.is(ModItems.MALFORMED_INGOT.get());
    }

    public static String material(ItemStack stack) {
        String material = stack.get(ModDataComponents.MATERIAL.get());
        return material == null ? "" : material;
    }

    public static ItemStack recoveryResidue(ItemStack stack) {
        return recoveryNuggets(stack);
    }

    public static ItemStack recoveryNuggets(ItemStack stack) {
        String material = material(stack);
        if (!material.isBlank()) {
            String nuggetId = material + "_nugget";
            if (MaterialCatalog.hasItemDefinition(nuggetId)) {
                return new ItemStack(ModItems.materialItem(nuggetId).get(), 2);
            }
            for (String itemId : MaterialCatalog.vanillaFormItems(material, "nugget")) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
                if (item != null) {
                    return new ItemStack(item, 2);
                }
            }
        }
        return new ItemStack(ModItems.materialItem("scrap").get(), 2);
    }

    @Override
    public Component getName(ItemStack stack) {
        String material = material(stack);
        if (material.isBlank()) {
            return super.getName(stack);
        }
        return Component.translatable("item.rngtech.malformed_ingot.material", materialName(material));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String material = material(stack);
        if (!material.isBlank()) {
            tooltipComponents.add(Component.translatable(
                    "rngtech.tooltip.malformed_ingot.material",
                    materialName(material)
            ).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    private static String materialName(String material) {
        String[] words = material.split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                name.append(word.substring(1));
            }
        }
        return name.toString();
    }
}
