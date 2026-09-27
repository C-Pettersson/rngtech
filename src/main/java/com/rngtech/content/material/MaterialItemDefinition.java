package com.rngtech.content.material;

public record MaterialItemDefinition(
        String itemId,
        String displayName,
        String texture,
        String materialId,
        String formId,
        MaterialItemKind kind
) {
    public boolean belongsToMaterialFamily() {
        return materialId != null;
    }
}
