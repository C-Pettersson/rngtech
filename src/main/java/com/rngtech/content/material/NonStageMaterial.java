package com.rngtech.content.material;

public record NonStageMaterial(
        String id,
        String displayName,
        String handling,
        String mapsTo,
        String reason
) {
    public boolean registered() {
        return "register".equals(handling);
    }

    public String texture() {
        return "rngtech:item/materials/non_stage/" + id;
    }
}
