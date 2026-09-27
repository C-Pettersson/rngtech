package com.rngtech.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface UniversalConnectorDataOwner {
    Level getLevel();

    BlockPos getBlockPos();

    boolean hasUniversalConnector(Direction face);

    void universalConnectorStateChanged();

    void clearConnectorNetworkCache();

    void setChanged();
}
