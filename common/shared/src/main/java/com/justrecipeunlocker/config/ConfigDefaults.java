package com.justrecipeunlocker.config;

import java.util.List;

public final class ConfigDefaults {

    private ConfigDefaults() {
    }

    public static JustRecipeUnlockerConfig defaults() {
        return new JustRecipeUnlockerConfig(true, List.of(), List.of(), false, false);
    }
}
