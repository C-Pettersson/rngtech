package com.rngtech.content.item;

import com.rngtech.content.cable.NetworkBridgeType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class NetworkConnectorItem extends Item {
    private final NetworkBridgeType bridgeType;

    public NetworkConnectorItem(NetworkBridgeType bridgeType, Properties properties) {
        super(properties);
        this.bridgeType = bridgeType;
    }

    public NetworkBridgeType bridgeType() {
        return bridgeType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("rngtech.tooltip.network_connector.bridge", Component.translatable(bridgeType.translationKey())));
        tooltip.add(Component.translatable("rngtech.tooltip.network_connector.same_mod_only"));
    }
}
