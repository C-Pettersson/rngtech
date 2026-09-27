package com.rngtech.content.material;

public record GenericMaterialForm(String id, String displayName) {
    public String texture() {
        return "rngtech:item/materials/forms/" + id;
    }
}
