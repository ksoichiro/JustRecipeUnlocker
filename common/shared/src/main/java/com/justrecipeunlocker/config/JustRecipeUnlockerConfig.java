package com.justrecipeunlocker.config;

import java.util.List;

public record JustRecipeUnlockerConfig(
        boolean unlockOnJoin,
        List<String> excludedNamespaces,
        List<String> excludedRecipeIds,
        boolean suppressRecipeToast,
        boolean suppressTutorialToast
) {
}
