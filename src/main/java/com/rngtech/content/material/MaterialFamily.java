package com.rngtech.content.material;

import java.util.List;

public record MaterialFamily(
        String id,
        String displayName,
        int stage,
        boolean defaultEnabled,
        String defaultReason,
        List<String> excludedForms
) {
    public boolean excludesForm(String formId) {
        return excludedForms.contains(formId);
    }
}
