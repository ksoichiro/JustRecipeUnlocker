package com.justrecipeunlocker.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {

    @Test
    void createsDefaultFileWhenMissing(@TempDir Path tempDir) {
        JustRecipeUnlockerConfig config = ConfigLoader.load(tempDir);

        assertTrue(Files.exists(tempDir.resolve("justrecipeunlocker.toml")));
        assertTrue(config.unlockOnJoin());
        assertTrue(config.excludedNamespaces().isEmpty());
        assertTrue(config.excludedRecipeIds().isEmpty());
        assertFalse(config.suppressRecipeToast());
        assertFalse(config.suppressTutorialToast());
    }

    @Test
    void readsExistingValues(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("justrecipeunlocker.toml"), """
                unlock_on_join = false
                excluded_namespaces = ["mymod"]
                excluded_recipe_ids = ["minecraft:tnt"]
                suppress_recipe_toast = true
                suppress_tutorial_toast = true
                """);

        JustRecipeUnlockerConfig config = ConfigLoader.load(tempDir);

        assertFalse(config.unlockOnJoin());
        assertEquals(List.of("mymod"), config.excludedNamespaces());
        assertEquals(List.of("minecraft:tnt"), config.excludedRecipeIds());
        assertTrue(config.suppressRecipeToast());
        assertTrue(config.suppressTutorialToast());
    }

    @Test
    void fallsBackToDefaultsOnMalformedToml(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("justrecipeunlocker.toml"), "not = [valid toml");

        JustRecipeUnlockerConfig config = ConfigLoader.load(tempDir);

        assertTrue(config.unlockOnJoin());
        assertTrue(config.excludedNamespaces().isEmpty());
    }
}
