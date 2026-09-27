package com.rngtech.content.cable;

import net.neoforged.fml.ModList;

import java.util.Locale;
import java.util.Optional;

public enum NetworkBridgeType {
    NONE(0, "none", "rngtech.network_bridge.none", ""),
    AE2(1, "ae2", "rngtech.network_bridge.ae2", "ae2"),
    REFINED_STORAGE(2, "refined_storage", "rngtech.network_bridge.refined_storage", "refinedstorage");

    private final int dataId;
    private final String serializedName;
    private final String translationKey;
    private final String modId;

    NetworkBridgeType(int dataId, String serializedName, String translationKey, String modId) {
        this.dataId = dataId;
        this.serializedName = serializedName;
        this.translationKey = translationKey;
        this.modId = modId;
    }

    public int dataId() {
        return dataId;
    }

    public String serializedName() {
        return serializedName;
    }

    public String translationKey() {
        return translationKey;
    }

    public String modId() {
        return modId;
    }

    public boolean isLoaded() {
        return this == NONE || ModList.get().isLoaded(modId);
    }

    public static NetworkBridgeType byDataId(int dataId) {
        for (NetworkBridgeType type : values()) {
            if (type.dataId == dataId) {
                return type;
            }
        }
        return NONE;
    }

    public static Optional<NetworkBridgeType> bySerializedName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        for (NetworkBridgeType type : values()) {
            if (type.serializedName.equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
