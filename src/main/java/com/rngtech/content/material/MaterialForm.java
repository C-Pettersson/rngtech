package com.rngtech.content.material;

public record MaterialForm(
        String id,
        String displayPattern,
        String itemPattern,
        boolean stageAuthority
) {
    public String itemId(String materialId) {
        return itemPattern.replace("{material}", materialId);
    }

    public String displayName(String materialDisplayName) {
        return displayPattern.replace("{material}", materialDisplayName);
    }

    public String texture(String materialId) {
        return "rngtech:item/materials/" + id + "/" + materialId;
    }
}
