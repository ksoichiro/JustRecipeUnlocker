package com.justrecipeunlocker.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

public final class ConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("justrecipeunlocker");
    private static final String CONFIG_FILE_NAME = "justrecipeunlocker.toml";
    private static final String DEFAULT_RESOURCE = "/justrecipeunlocker-default-config.toml";

    private ConfigLoader() {
    }

    public static JustRecipeUnlockerConfig load(Path configDir) {
        Path configFile = configDir.resolve(CONFIG_FILE_NAME);
        try {
            ensureConfigFileExists(configDir, configFile);
            CommentedConfig parsed;
            try (InputStream in = Files.newInputStream(configFile)) {
                parsed = new TomlParser().parse(in);
            }
            return new JustRecipeUnlockerConfig(
                    readBoolean(parsed, "unlock_on_join", true),
                    readStringList(parsed, "excluded_namespaces"),
                    readStringList(parsed, "excluded_recipe_ids"),
                    readBoolean(parsed, "suppress_recipe_toast", false),
                    readBoolean(parsed, "suppress_tutorial_toast", false)
            );
        } catch (Exception e) {
            LOGGER.error("Failed to load {}; using built-in defaults", configFile, e);
            return ConfigDefaults.defaults();
        }
    }

    private static void ensureConfigFileExists(Path configDir, Path configFile) throws IOException {
        if (Files.exists(configFile)) {
            return;
        }
        Files.createDirectories(configDir);
        try (InputStream in = ConfigLoader.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IOException("Bundled default config resource not found: " + DEFAULT_RESOURCE);
            }
            Files.copy(in, configFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean readBoolean(CommentedConfig parsed, String path, boolean defaultValue) {
        Object value = parsed.get(path);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value != null) {
            LOGGER.error("Invalid {} = {} (must be true/false); using default {}", path, value, defaultValue);
        }
        return defaultValue;
    }

    private static List<String> readStringList(CommentedConfig parsed, String path) {
        Object value = parsed.get(path);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toUnmodifiableList());
        }
        if (value != null) {
            LOGGER.error("Invalid {} = {} (must be a list of strings); using empty list", path, value);
        }
        return List.of();
    }
}
