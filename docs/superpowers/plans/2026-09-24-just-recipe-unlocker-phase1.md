# Just Recipe Unlocker — Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a working `common/1.21.1` + `fabric/1.21.1` build of Just Recipe Unlocker: join-time recipe unlock, admin commands, namespace/id-based exclusion, and toast/tutorial suppression, with a vanilla client able to connect and see the effect.

**Architecture:** A plain `fabric-loom` Gradle build (no Architectury Loom, no Mixin) with a `common/<version>` module holding version-specific Minecraft-API glue and a `common/shared` source set holding pure, unit-testable logic (config load and recipe-exclusion filtering). The Fabric module wires that logic to `ServerPlayConnectionEvents.JOIN` and a Brigadier command tree. This mirrors the proven, working structure of the sibling `MinersMarket`/`JustCoordinates` projects on this machine, adapted to Fabric-only scope for Phase 1.

**Tech Stack:** Java 21, Gradle 9.4.0, Fabric Loom, Fabric API `0.116.7+1.21.1`, Fabric Loader `>=0.17.3`, night-config (TOML) `3.8.3`, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-24-just-recipe-unlocker-design.md`

## Global Constraints

- Minecraft version: `1.21.1` only in this phase (other versions/loaders are later phases per the spec's rollout plan).
- Loader: Fabric only in this phase.
- No Mixin.
- No client-side GUI or client mod requirement — a vanilla client must work unmodified.
- No runtime dependency on any third-party API mod; the only runtime mod dependency is Fabric API.
- Mod id: `justrecipeunlocker`. Root package: `com.justrecipeunlocker`.
- License: `LGPL-3.0-only`.
- Admin commands require permission level 2 (op).
- Config is a single TOML file (`justrecipeunlocker.toml`), no in-game editor, loaded once at startup.

## Review Focus

- A missing or malformed `justrecipeunlocker.toml` must not crash server startup — the mod must fall back to built-in defaults and keep running. (Task 3)
- An `excluded_recipe_ids`/`excluded_namespaces` entry that is malformed (no `:`, empty string) must not throw — it should simply fail to match anything unexpected. (Task 2)
- `excluded_namespaces` matching must be an exact namespace match, not a prefix match (e.g. `"mine"` must not exclude `minecraft:torch`). (Task 2)
- `/justrecipeunlocker unlockAll` run with zero players online must complete without error. (Task 5)
- A player who already has some recipes unlocked (an existing world, not a fresh one) must not error when join-time unlock re-awards already-known recipes. (Task 5)

---

## File Structure

```
.gitmodules                                                        # gradle/shared submodule
gradle/shared/                                                      # submodule: minecraft-mod-gradle-scripts
gradle.properties
props/1.21.1.properties
settings.gradle
build.gradle
mise.toml
COPYING, COPYING.LESSER                                             # LGPL-3.0 license text
README.md
.gitignore

common/shared/src/main/java/com/justrecipeunlocker/recipe/RecipeExclusionFilter.java
common/shared/src/test/java/com/justrecipeunlocker/recipe/RecipeExclusionFilterTest.java
common/shared/src/main/java/com/justrecipeunlocker/config/JustRecipeUnlockerConfig.java
common/shared/src/main/java/com/justrecipeunlocker/config/ConfigDefaults.java
common/shared/src/main/java/com/justrecipeunlocker/config/ConfigLoader.java
common/shared/src/test/java/com/justrecipeunlocker/config/ConfigLoaderTest.java
common/shared/src/main/resources/justrecipeunlocker-default-config.toml

common/1.21.1/build.gradle
common/1.21.1/src/main/java/com/justrecipeunlocker/recipe/RecipeUnlockService.java

fabric/1.21.1/build.gradle
fabric/1.21.1/src/main/resources/fabric.mod.json
fabric/1.21.1/src/main/java/com/justrecipeunlocker/fabric/JustRecipeUnlockerFabric.java
```

Notes:
- `common/shared` holds version-independent, pure-Java logic (no Minecraft classes) so it is trivially unit-testable with JUnit and reusable once later phases add more MC versions. It is pulled into `common/1.21.1`'s source set via `srcDir '../shared/src/main/java'` (and `src/test/java` for tests), matching the working sibling-project pattern.
- `common/1.21.1` holds the one class that touches Minecraft API (`RecipeManager`, `ServerPlayer`) and is not unit-tested (no local precedent for unit-testing against real game data); it is verified manually via `runServer`/`runClient` in Task 5.
- `fabric/1.21.1` holds only the loader entrypoint, event/command wiring, and `fabric.mod.json`.

---

### Task 1: Repository & Gradle scaffolding (Fabric/1.21.1 only)

**Files:**
- Create: `.gitmodules`
- Create: `gradle/shared/` (git submodule checkout)
- Create: `gradle.properties`
- Create: `props/1.21.1.properties`
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `mise.toml`
- Modify: `.gitignore` (already contains `.worktrees/` from the parent repo; append build-related entries)
- Create: `COPYING`, `COPYING.LESSER`
- Create: `README.md`
- Create: `common/1.21.1/build.gradle` (placeholder, no source yet)
- Create: `fabric/1.21.1/build.gradle` (placeholder, no source yet)
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`

**Interfaces:**
- Produces: a configured multi-project Gradle build with `:common` → `common/1.21.1` and `:fabric` → `fabric/1.21.1`, both applying `fabric-loom`. Later tasks add source files under these two modules; no build.gradle changes should be needed for Tasks 2–4 (only Task 5 touches `fabric.mod.json`'s `processResources` wiring, which is already present here).

- [ ] **Step 1: Add the `gradle/shared` submodule**

```bash
git submodule add https://github.com/ksoichiro/minecraft-mod-gradle-scripts.git gradle/shared
git submodule update --init --recursive
```

- [ ] **Step 2: Write `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx4G
org.gradle.parallel=true

mod_version=0.1.0
maven_group=com.justrecipeunlocker
archives_name=justrecipeunlocker

target_mc_version=1.21.1
supported_mc_versions=1.21.1
```

- [ ] **Step 3: Write `props/1.21.1.properties`**

```properties
# Minecraft 1.21.1 version configuration
minecraft_version=1.21.1
pack_format=48
java_version=21

fabric_loader_version=0.17.3
fabric_api_version=0.116.7+1.21.1

enabled_platforms=fabric
```

- [ ] **Step 4: Write `settings.gradle`**

```groovy
pluginManagement {
    repositories {
        maven { url 'https://maven.fabricmc.net/' }
        gradlePluginPortal()
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}

rootProject.name = 'JustRecipeUnlocker'

def targetMcVersion = settings.providers.gradleProperty('target_mc_version').getOrNull()
if (targetMcVersion == null) {
    throw new GradleException("'target_mc_version' property is not defined. Check gradle.properties or pass -Ptarget_mc_version=<version>")
}

include 'common'
project(':common').projectDir = file("common/${targetMcVersion}")

def propsFile = file("props/${targetMcVersion}.properties")
if (!propsFile.exists()) {
    throw new GradleException("Version properties not found: ${propsFile.absolutePath}")
}
def props = new Properties()
propsFile.withInputStream { props.load(it) }
def enabledPlatforms = props.getProperty('enabled_platforms', '').split(',')

enabledPlatforms.each { platform ->
    platform = platform.trim()
    if (platform) {
        include platform
        project(":${platform}").projectDir = file("${platform}/${targetMcVersion}")
    }
}
```

- [ ] **Step 5: Write root `build.gradle`**

```groovy
ext.loadVersionProperties = { String version ->
    def propsFile = file("props/${version}.properties")
    if (!propsFile.exists()) {
        throw new GradleException("Version properties not found: ${propsFile.absolutePath}")
    }
    def props = new Properties()
    propsFile.withInputStream { props.load(it) }
    props.each { key, value -> project.ext.set(key.toString(), value.toString()) }
}

def targetVersion = project.findProperty('target_mc_version')
if (targetVersion == null) {
    throw new GradleException("'target_mc_version' property is not defined.")
}
loadVersionProperties(targetVersion)

allprojects {
    group = rootProject.maven_group
    version = rootProject.mod_version
}

subprojects {
    apply plugin: 'java'

    ext.minecraft_version = rootProject.ext.minecraft_version
    ext.fabric_loader_version = rootProject.ext.fabric_loader_version
    ext.fabric_api_version = rootProject.ext.fabric_api_version
    ext.java_version = rootProject.ext.java_version

    repositories {
        maven { url = 'https://libraries.minecraft.net/' }
        mavenCentral()
    }

    java {
        withSourcesJar()
        toolchain {
            languageVersion = JavaLanguageVersion.of(project.ext.java_version as int)
        }
    }

    tasks.withType(JavaCompile).configureEach {
        options.encoding = 'UTF-8'
        options.release = java_version as int
    }
}

if (!file('gradle/shared/multi-version-tasks.gradle').exists()) {
    throw new GradleException("Shared Gradle scripts not found. Run: git submodule update --init")
}
apply from: 'gradle/shared/multi-version-tasks.gradle'
```

- [ ] **Step 6: Write placeholder `common/1.21.1/build.gradle`**

```groovy
plugins {
    id 'fabric-loom'
}

dependencies {
    minecraft "com.mojang:minecraft:${minecraft_version}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${fabric_loader_version}"
}
```

- [ ] **Step 7: Write placeholder `fabric/1.21.1/build.gradle`**

```groovy
plugins {
    id 'fabric-loom'
}

dependencies {
    minecraft "com.mojang:minecraft:${minecraft_version}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${fabric_loader_version}"
    modApi "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}"

    compileOnly project(':common')
}
```

- [ ] **Step 8: Add the Gradle wrapper (pinned to Gradle 9.4.0, matching sibling projects)**

Copy `gradlew`, `gradlew.bat`, and `gradle/wrapper/` from an existing sibling project that already pins Gradle 9.4.0 (e.g. `/Users/ksoichiro/src/github.com/ksoichiro/MinersMarket`), then verify:

```bash
./gradlew --version
```

Expected: reports `Gradle 9.4.0` and exits 0.

- [ ] **Step 9: Append build-related entries to `.gitignore`**

`.gitignore` in this repo already contains `.worktrees/` (added by the controller before this worktree was created). Append:

```
.gradle/
build/
run/
.idea/
*.iml
out/
```

- [ ] **Step 10: Add license files and a minimal README**

Copy `COPYING` and `COPYING.LESSER` verbatim from `/Users/ksoichiro/src/github.com/ksoichiro/JustCoordinates` (same LGPL-3.0-only license, same author).

```markdown
# Just Recipe Unlocker

Unlocks all crafting recipes for players on join. A lightweight,
server-installed mod — no client-side mod required.

## License

LGPL-3.0-only. See [COPYING](COPYING) and [COPYING.LESSER](COPYING.LESSER).
```

- [ ] **Step 11: Verify the build configures**

```bash
./gradlew projects
```

Expected: lists `Root project 'JustRecipeUnlocker'`, `Project ':common'`, `Project ':fabric'` with no errors.

- [ ] **Step 12: Commit**

```bash
git add .gitmodules gradle.properties props/ settings.gradle build.gradle mise.toml \
  .gitignore COPYING COPYING.LESSER README.md common/1.21.1/build.gradle \
  fabric/1.21.1/build.gradle gradlew gradlew.bat gradle/wrapper
git commit -m "chore: scaffold Gradle multi-module build for fabric/1.21.1"
```

---

### Task 2: Recipe exclusion filter (TDD)

**Files:**
- Create: `common/shared/src/main/java/com/justrecipeunlocker/recipe/RecipeExclusionFilter.java`
- Test: `common/shared/src/test/java/com/justrecipeunlocker/recipe/RecipeExclusionFilterTest.java`
- Modify: `common/1.21.1/build.gradle` (add source dirs + test deps)

**Interfaces:**
- Produces: `RecipeExclusionFilter(Collection<String> excludedNamespaces, Collection<String> excludedRecipeIds)` with `boolean isExcluded(String recipeId)`, where `recipeId` is a `namespace:path` string. Consumed by Task 4's `RecipeUnlockService`.

- [ ] **Step 1: Wire `common/shared` into `common/1.21.1` and add JUnit**

Edit `common/1.21.1/build.gradle`:

```groovy
plugins {
    id 'fabric-loom'
}

sourceSets {
    main {
        java {
            srcDir 'src/main/java'
            srcDir '../shared/src/main/java'
        }
        resources {
            srcDir 'src/main/resources'
            srcDir '../shared/src/main/resources'
        }
    }
    test {
        java {
            srcDir 'src/test/java'
            srcDir '../shared/src/test/java'
        }
    }
}

dependencies {
    minecraft "com.mojang:minecraft:${minecraft_version}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${fabric_loader_version}"

    testImplementation platform('org.junit:junit-bom:5.11.3')
    testImplementation 'org.junit.jupiter:junit-jupiter'
}

test {
    useJUnitPlatform()
}
```

- [ ] **Step 2: Write the failing test**

`common/shared/src/test/java/com/justrecipeunlocker/recipe/RecipeExclusionFilterTest.java`:

```java
package com.justrecipeunlocker.recipe;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeExclusionFilterTest {

    @Test
    void excludesRecipeInExcludedNamespace() {
        var filter = new RecipeExclusionFilter(List.of("mymod"), List.of());
        assertTrue(filter.isExcluded("mymod:widget"));
    }

    @Test
    void excludesExplicitRecipeId() {
        var filter = new RecipeExclusionFilter(List.of(), List.of("minecraft:tnt"));
        assertTrue(filter.isExcluded("minecraft:tnt"));
    }

    @Test
    void allowsUnlistedRecipe() {
        var filter = new RecipeExclusionFilter(List.of("mymod"), List.of("minecraft:tnt"));
        assertFalse(filter.isExcluded("minecraft:torch"));
    }

    @Test
    void allowsAllWhenListsEmpty() {
        var filter = new RecipeExclusionFilter(List.of(), List.of());
        assertFalse(filter.isExcluded("minecraft:torch"));
    }

    @Test
    void namespaceMatchIsNotAPrefixMatch() {
        var filter = new RecipeExclusionFilter(List.of("mine"), List.of());
        assertFalse(filter.isExcluded("minecraft:torch"));
    }

    @Test
    void malformedExcludedRecipeIdWithoutNamespaceDoesNotMatchOtherRecipes() {
        var filter = new RecipeExclusionFilter(List.of(), List.of("nonamespace"));
        assertFalse(filter.isExcluded("minecraft:torch"));
        assertTrue(filter.isExcluded("nonamespace"));
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
./gradlew :common:test --tests "com.justrecipeunlocker.recipe.RecipeExclusionFilterTest"
```

Expected: FAIL — compilation error, `RecipeExclusionFilter` does not exist.

- [ ] **Step 4: Write the implementation**

`common/shared/src/main/java/com/justrecipeunlocker/recipe/RecipeExclusionFilter.java`:

```java
package com.justrecipeunlocker.recipe;

import java.util.Collection;
import java.util.Set;

public final class RecipeExclusionFilter {

    private final Set<String> excludedNamespaces;
    private final Set<String> excludedRecipeIds;

    public RecipeExclusionFilter(Collection<String> excludedNamespaces, Collection<String> excludedRecipeIds) {
        this.excludedNamespaces = Set.copyOf(excludedNamespaces);
        this.excludedRecipeIds = Set.copyOf(excludedRecipeIds);
    }

    public boolean isExcluded(String recipeId) {
        if (excludedRecipeIds.contains(recipeId)) {
            return true;
        }
        int colon = recipeId.indexOf(':');
        String namespace = colon >= 0 ? recipeId.substring(0, colon) : recipeId;
        return excludedNamespaces.contains(namespace);
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
./gradlew :common:test --tests "com.justrecipeunlocker.recipe.RecipeExclusionFilterTest"
```

Expected: PASS, 6 tests.

- [ ] **Step 6: Commit**

```bash
git add common/1.21.1/build.gradle common/shared/src/main/java/com/justrecipeunlocker/recipe/RecipeExclusionFilter.java \
  common/shared/src/test/java/com/justrecipeunlocker/recipe/RecipeExclusionFilterTest.java
git commit -m "feat: add recipe exclusion filter"
```

---

### Task 3: Config load with TOML defaults (TDD)

**Files:**
- Create: `common/shared/src/main/java/com/justrecipeunlocker/config/JustRecipeUnlockerConfig.java`
- Create: `common/shared/src/main/java/com/justrecipeunlocker/config/ConfigDefaults.java`
- Create: `common/shared/src/main/java/com/justrecipeunlocker/config/ConfigLoader.java`
- Create: `common/shared/src/main/resources/justrecipeunlocker-default-config.toml`
- Test: `common/shared/src/test/java/com/justrecipeunlocker/config/ConfigLoaderTest.java`
- Modify: `common/1.21.1/build.gradle` (add night-config dependency)

**Interfaces:**
- Consumes: nothing from prior tasks.
- Produces: `JustRecipeUnlockerConfig` record with accessors `unlockOnJoin()`, `excludedNamespaces()`, `excludedRecipeIds()`, `suppressRecipeToast()`, `suppressTutorialToast()`. `ConfigLoader.load(Path configDir)` returns a `JustRecipeUnlockerConfig`, creating `configDir/justrecipeunlocker.toml` from the bundled default if missing, and never throwing. Consumed by Task 4 (`RecipeUnlockService`) and Task 5 (Fabric entrypoint).

- [ ] **Step 1: Add night-config to `common/1.21.1/build.gradle`**

Add to the `dependencies` block:

```groovy
    compileOnly "com.electronwill.night-config:core:3.8.3"
    compileOnly "com.electronwill.night-config:toml:3.8.3"

    testImplementation "com.electronwill.night-config:core:3.8.3"
    testImplementation "com.electronwill.night-config:toml:3.8.3"
```

- [ ] **Step 2: Write the failing tests**

`common/shared/src/test/java/com/justrecipeunlocker/config/ConfigLoaderTest.java`:

```java
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
```

- [ ] **Step 3: Run the tests to verify they fail**

```bash
./gradlew :common:test --tests "com.justrecipeunlocker.config.ConfigLoaderTest"
```

Expected: FAIL — compilation error, `JustRecipeUnlockerConfig`/`ConfigLoader` do not exist.

- [ ] **Step 4: Write the config record**

`common/shared/src/main/java/com/justrecipeunlocker/config/JustRecipeUnlockerConfig.java`:

```java
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
```

- [ ] **Step 5: Write the defaults holder**

`common/shared/src/main/java/com/justrecipeunlocker/config/ConfigDefaults.java`:

```java
package com.justrecipeunlocker.config;

import java.util.List;

public final class ConfigDefaults {

    private ConfigDefaults() {
    }

    public static JustRecipeUnlockerConfig defaults() {
        return new JustRecipeUnlockerConfig(true, List.of(), List.of(), false, false);
    }
}
```

- [ ] **Step 6: Write the bundled default config resource**

`common/shared/src/main/resources/justrecipeunlocker-default-config.toml`:

```toml
# Just Recipe Unlocker configuration

# Unlock all non-excluded recipes for a player when they join the server.
unlock_on_join = true

# Exclude recipes in bulk by namespace (mod id).
excluded_namespaces = []

# Exclude individual recipes by id (namespace:path).
excluded_recipe_ids = []

# Suppress the "New recipes unlocked!" toast notification.
suppress_recipe_toast = false

# Suppress recipe-related tutorial toast notifications.
suppress_tutorial_toast = false
```

- [ ] **Step 7: Write `ConfigLoader`**

`common/shared/src/main/java/com/justrecipeunlocker/config/ConfigLoader.java`:

```java
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
```

- [ ] **Step 8: Run the tests to verify they pass**

```bash
./gradlew :common:test --tests "com.justrecipeunlocker.config.ConfigLoaderTest"
```

Expected: PASS, 3 tests.

- [ ] **Step 9: Commit**

```bash
git add common/1.21.1/build.gradle common/shared/src/main/java/com/justrecipeunlocker/config \
  common/shared/src/main/resources/justrecipeunlocker-default-config.toml \
  common/shared/src/test/java/com/justrecipeunlocker/config
git commit -m "feat: add TOML config loader with defaults fallback"
```

---

### Task 4: Recipe unlock service (Minecraft API integration)

**Files:**
- Create: `common/1.21.1/src/main/java/com/justrecipeunlocker/recipe/RecipeUnlockService.java`

**Interfaces:**
- Consumes: `RecipeExclusionFilter(Collection<String>, Collection<String>)` / `isExcluded(String)` from Task 2; `JustRecipeUnlockerConfig` accessors from Task 3.
- Produces: `RecipeUnlockService(RecipeManager recipeManager)` with `List<ResourceLocation> resolveUnlockableRecipeIds(JustRecipeUnlockerConfig config)` and `void unlock(ServerPlayer player, List<ResourceLocation> recipeIds)`. Consumed by Task 5's Fabric entrypoint.

This class touches real Minecraft classes (`RecipeManager`, `ServerPlayer`, `ResourceLocation`) and has no local precedent for unit testing against real game data, so it is verified manually in Task 5 via `runServer`/`runClient` rather than with JUnit here.

- [ ] **Step 1: Write the implementation**

`common/1.21.1/src/main/java/com/justrecipeunlocker/recipe/RecipeUnlockService.java`:

```java
package com.justrecipeunlocker.recipe;

import com.justrecipeunlocker.config.JustRecipeUnlockerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

public final class RecipeUnlockService {

    private final RecipeManager recipeManager;

    public RecipeUnlockService(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    public List<ResourceLocation> resolveUnlockableRecipeIds(JustRecipeUnlockerConfig config) {
        RecipeExclusionFilter filter = new RecipeExclusionFilter(config.excludedNamespaces(), config.excludedRecipeIds());
        List<ResourceLocation> recipeIds = new ArrayList<>();
        for (var holder : recipeManager.getRecipes()) {
            ResourceLocation id = holder.id();
            if (!filter.isExcluded(id.toString())) {
                recipeIds.add(id);
            }
        }
        return recipeIds;
    }

    public void unlock(ServerPlayer player, List<ResourceLocation> recipeIds) {
        player.awardRecipesByKey(recipeIds);
    }
}
```

**Verify during implementation:** if this does not compile against the actual Fabric Loom-remapped Minecraft 1.21.1 classes, run `./gradlew :common:genSources` and inspect the generated sources for `net.minecraft.world.item.crafting.RecipeManager#getRecipes()` and `net.minecraft.server.level.ServerPlayer#awardRecipesByKey`. Both method names are confirmed to exist on this version; only exact generic/parameter types may need adjusting (the `var holder` loop above is written to be resilient to the exact generic signature of `getRecipes()`).

- [ ] **Step 2: Compile to confirm the API surface**

```bash
./gradlew :common:compileJava
```

Expected: BUILD SUCCESSFUL. If it fails on an unresolved symbol, follow the verification note above and adjust the method call accordingly (do not change the public method signatures of `RecipeUnlockService`, since Task 5 depends on them as written).

- [ ] **Step 3: Commit**

```bash
git add common/1.21.1/src/main/java/com/justrecipeunlocker/recipe/RecipeUnlockService.java
git commit -m "feat: add recipe unlock service"
```

---

### Task 5: Fabric entrypoint — join-time unlock + commands

**Files:**
- Modify: `fabric/1.21.1/build.gradle`
- Create: `fabric/1.21.1/src/main/resources/fabric.mod.json`
- Create: `fabric/1.21.1/src/main/java/com/justrecipeunlocker/fabric/JustRecipeUnlockerFabric.java`

**Interfaces:**
- Consumes: `ConfigLoader.load(Path)` (Task 3), `RecipeUnlockService` (Task 4).
- Produces: the Fabric `ModInitializer` entrypoint `com.justrecipeunlocker.fabric.JustRecipeUnlockerFabric`, which Task 6 modifies to add toast-suppression behavior.

- [ ] **Step 1: Update `fabric/1.21.1/build.gradle`**

```groovy
plugins {
    id 'fabric-loom'
}

configurations {
    commonJava { canBeResolved = true }
    commonResources { canBeResolved = true }
}

dependencies {
    minecraft "com.mojang:minecraft:${minecraft_version}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${fabric_loader_version}"
    modApi "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}"
    modImplementation include("com.electronwill.night-config:core:3.8.3")
    modImplementation include("com.electronwill.night-config:toml:3.8.3")

    compileOnly project(':common')

    commonJava project(path: ':common', configuration: 'commonJava')
    commonResources project(path: ':common', configuration: 'commonResources')
}

tasks.named('compileJava') {
    dependsOn configurations.commonJava
    source configurations.commonJava
}

tasks.named('processResources') {
    dependsOn configurations.commonResources
    from configurations.commonResources
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    inputs.property 'version', project.version
    inputs.property 'minecraft_version', project.ext.minecraft_version
    inputs.property 'fabric_loader_version', project.ext.fabric_loader_version
    filesMatching('fabric.mod.json') {
        expand(
            version: project.version,
            minecraft_version: project.ext.minecraft_version,
            fabric_loader_version: project.ext.fabric_loader_version,
        )
    }
}

loom {
    runs {
        client { client(); runDir("run") }
        server { server(); runDir("run") }
    }
}

jar {
    archiveBaseName = rootProject.archives_name
    archiveVersion = "${rootProject.mod_version}+${minecraft_version}-fabric"
    archiveClassifier = 'dev'
}
remapJar {
    archiveBaseName = rootProject.archives_name
    archiveVersion = "${rootProject.mod_version}+${minecraft_version}-fabric"
    archiveClassifier = null
}
sourcesJar {
    archiveBaseName = rootProject.archives_name
    archiveVersion = "${rootProject.mod_version}+${minecraft_version}-fabric"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

- [ ] **Step 2: Write `fabric.mod.json`**

`fabric/1.21.1/src/main/resources/fabric.mod.json`:

```json
{
  "schemaVersion": 1,
  "id": "justrecipeunlocker",
  "version": "${version}",
  "name": "Just Recipe Unlocker",
  "description": "Unlocks all crafting recipes for players on join.",
  "authors": [],
  "license": "LGPL-3.0-only",
  "environment": "*",
  "entrypoints": {
    "main": ["com.justrecipeunlocker.fabric.JustRecipeUnlockerFabric"]
  },
  "depends": {
    "fabricloader": ">=${fabric_loader_version}",
    "fabric-api": "*",
    "minecraft": "${minecraft_version}"
  }
}
```

- [ ] **Step 3: Write the Fabric entrypoint**

`fabric/1.21.1/src/main/java/com/justrecipeunlocker/fabric/JustRecipeUnlockerFabric.java`:

```java
package com.justrecipeunlocker.fabric;

import com.justrecipeunlocker.config.ConfigLoader;
import com.justrecipeunlocker.config.JustRecipeUnlockerConfig;
import com.justrecipeunlocker.recipe.RecipeUnlockService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

public final class JustRecipeUnlockerFabric implements ModInitializer {

    public static final String MOD_ID = "justrecipeunlocker";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private JustRecipeUnlockerConfig config;

    @Override
    public void onInitialize() {
        config = ConfigLoader.load(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!config.unlockOnJoin()) {
                return;
            }
            ServerPlayer player = handler.getPlayer();
            RecipeUnlockService service = new RecipeUnlockService(server.getRecipeManager());
            List<ResourceLocation> recipeIds = service.resolveUnlockableRecipeIds(config);
            service.unlock(player, recipeIds);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = Commands.literal("justrecipeunlocker")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("unlockAll").executes(ctx -> {
                        var server = ctx.getSource().getServer();
                        RecipeUnlockService service = new RecipeUnlockService(server.getRecipeManager());
                        List<ResourceLocation> recipeIds = service.resolveUnlockableRecipeIds(config);
                        Collection<ServerPlayer> targets = server.getPlayerList().getPlayers();
                        for (ServerPlayer player : targets) {
                            service.unlock(player, recipeIds);
                        }
                        ctx.getSource().sendSuccess(
                                () -> Component.literal("Unlocked recipes for " + targets.size() + " player(s)."), true);
                        return targets.size();
                    }))
                    .then(Commands.literal("unlock")
                            .then(Commands.argument("targets", EntityArgument.players()).executes(ctx -> {
                                Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                RecipeUnlockService service =
                                        new RecipeUnlockService(ctx.getSource().getServer().getRecipeManager());
                                List<ResourceLocation> recipeIds = service.resolveUnlockableRecipeIds(config);
                                for (ServerPlayer player : targets) {
                                    service.unlock(player, recipeIds);
                                }
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("Unlocked recipes for " + targets.size() + " player(s)."), true);
                                return targets.size();
                            })));

            dispatcher.register(root);
            dispatcher.register(Commands.literal("jru")
                    .requires(source -> source.hasPermission(2))
                    .redirect(dispatcher.getRoot().getChild("justrecipeunlocker")));
        });

        LOGGER.info("Just Recipe Unlocker initialized");
    }
}
```

**Verify during implementation:** `CommandRegistrationCallback.EVENT`'s registration lambda signature (`dispatcher, registryAccess, environment`) and `Commands`/`EntityArgument` package paths are standard Fabric API 1.21.1 usage but have no local precedent in this project family — if compilation fails on the lambda parameter list or an import, check the actual signature via `./gradlew :fabric:genSources` on the `fabric-api` dependency, or the decompiled `CommandRegistrationCallback` class, and adjust the lambda parameters accordingly without changing `RecipeUnlockService`'s public API.

- [ ] **Step 4: Compile**

```bash
./gradlew :fabric:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Manual verification**

```bash
./gradlew :fabric:runServer
```

With the server running, connect using an unmodified vanilla 1.21.1 client and verify, in order:

1. On first join, the recipe book shows all recipes unlocked (open the crafting table recipe book).
2. As an op, run `/justrecipeunlocker unlockAll` with only yourself online — command succeeds and reports "Unlocked recipes for 1 player(s)."
3. Run `/jru unlock @s` — the alias resolves and the command succeeds.
4. Stop the server, restart it, and rejoin the same player (who already has recipes unlocked from step 1) — join must not error or throw in the server log; the recipe book must still show all recipes.
5. Run `/justrecipeunlocker unlockAll` while no players are online (e.g. from server console, or after everyone disconnects) — command must report "Unlocked recipes for 0 player(s)." without an error or stack trace in the log.
6. Set `excluded_recipe_ids = ["minecraft:diamond_block"]` in `run/config/justrecipeunlocker/justrecipeunlocker.toml`, restart the server, rejoin — the diamond block recipe must NOT appear unlocked while other vanilla recipes are.

- [ ] **Step 6: Commit**

```bash
git add fabric/1.21.1/build.gradle fabric/1.21.1/src/main/resources/fabric.mod.json \
  fabric/1.21.1/src/main/java/com/justrecipeunlocker/fabric/JustRecipeUnlockerFabric.java
git commit -m "feat: wire join-time unlock and admin commands into the fabric entrypoint"
```

---

### Task 6: Notification suppression

**Files:**
- Modify: `common/1.21.1/src/main/java/com/justrecipeunlocker/recipe/RecipeUnlockService.java`
- Modify: `fabric/1.21.1/src/main/java/com/justrecipeunlocker/fabric/JustRecipeUnlockerFabric.java`

**Interfaces:**
- Consumes: `JustRecipeUnlockerConfig.suppressRecipeToast()` / `suppressTutorialToast()` (Task 3).
- Produces: `RecipeUnlockService.unlock(ServerPlayer, List<ResourceLocation>, boolean suppressToast)` (signature change from Task 4/5 — update both call sites in `JustRecipeUnlockerFabric`).

This task starts with investigation because the exact mechanism is genuinely uncertain (flagged as an open question in the spec) — do not skip the investigation step.

- [ ] **Step 1: Investigate the recipe-toast mechanism**

Run `./gradlew :common:genSources` and open the decompiled sources for:
- `net.minecraft.stats.ServerRecipeBook` — confirm the visibility and exact parameters of `sendInitialRecipeBook` and whatever method is used for a subsequent "add" sync (these are confirmed to exist on this version but exact signatures need to be read from source, not guessed).
- `net.minecraft.network.protocol.game.ClientboundRecipePacket` and its nested `State` enum — confirm the enum constants (`INIT`, `ADD`, `REMOVE` are expected) and the packet constructor.
- `net.minecraft.server.level.ServerPlayer#awardRecipesByKey` — confirm what it does internally (expected: delegates to the recipe book and sends a `ClientboundRecipePacket` with `State.ADD`, which is what shows the "New recipes unlocked!" toast).

Record which of the two following implementation strategies applies:
- **If `ServerRecipeBook#sendInitialRecipeBook(ServerPlayer)` (or equivalent) is public/accessible:** call it directly after adding the recipes to the player's recipe book, instead of `awardRecipesByKey`, when suppression is requested.
- **If it is not accessible:** construct and send a `ClientboundRecipePacket` with `State.INIT` directly via the player's connection, after adding the recipe IDs to `player.getRecipeBook()`.

- [ ] **Step 2: Implement `suppress_recipe_toast`**

Update `RecipeUnlockService`:

```java
public void unlock(ServerPlayer player, List<ResourceLocation> recipeIds, boolean suppressToast) {
    if (suppressToast) {
        unlockSilently(player, recipeIds);
    } else {
        player.awardRecipesByKey(recipeIds);
    }
}

private void unlockSilently(ServerPlayer player, List<ResourceLocation> recipeIds) {
    // Implementation depends on Step 1's findings: either call the confirmed
    // public "send without ADD-state toast" API on ServerRecipeBook, or add
    // the recipes to player.getRecipeBook() and send a hand-built
    // ClientboundRecipePacket with State.INIT via player.connection.send(...).
}
```

Fill in `unlockSilently` per the Step 1 findings; keep the public method signature (`unlock(ServerPlayer, List<ResourceLocation>, boolean)`) exactly as above, since `JustRecipeUnlockerFabric` is updated to call it with `config.suppressRecipeToast()` at both call sites (join event and both commands).

- [ ] **Step 3: Update call sites in `JustRecipeUnlockerFabric`**

Replace every `service.unlock(player, recipeIds)` call with `service.unlock(player, recipeIds, config.suppressRecipeToast())`.

- [ ] **Step 4: Investigate `suppress_tutorial_toast`**

Search the decompiled client sources (`./gradlew :fabric:genSources`) for the recipe-related tutorial hint (the "Move the item into the crafting grid" style hint shown to new players). Determine whether it is:
- (a) triggered by a server-sent signal you can suppress (e.g. a stat/advancement trigger), or
- (b) purely client-side heuristic behavior with no server-side lever.

If (b), this setting cannot be implemented as specified. In that case, keep the `suppress_tutorial_toast` config field (already implemented in Task 3, so existing configs referencing it don't break) but leave it a no-op, and report this finding back for a decision (raise it in the PR description / final report for Phase 1, per the spec's explicit open-question framing — do not silently drop the setting or silently pretend it works).

- [ ] **Step 5: Compile**

```bash
./gradlew :fabric:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Manual verification**

```bash
./gradlew :fabric:runServer
```

With `suppress_recipe_toast = true` in the config, join with a vanilla client on a fresh world and confirm the "New recipes unlocked!" toast does NOT appear, while the recipe book still shows all recipes unlocked. Then set `suppress_recipe_toast = false`, rejoin with a different/reset player, and confirm the toast DOES appear (confirming the flag actually changes behavior both ways, not just always-suppressed).

- [ ] **Step 7: Commit**

```bash
git add common/1.21.1/src/main/java/com/justrecipeunlocker/recipe/RecipeUnlockService.java \
  fabric/1.21.1/src/main/java/com/justrecipeunlocker/fabric/JustRecipeUnlockerFabric.java
git commit -m "feat: implement recipe toast suppression"
```

---

### Task 7: Phase 1 verification and wrap-up

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: the full Phase 1 mod built in Tasks 1–6.
- Produces: nothing consumed by later tasks; this closes out Phase 1.

- [ ] **Step 1: Run the full test suite**

```bash
./gradlew :common:test
```

Expected: all tests from Tasks 2 and 3 pass (9 tests total).

- [ ] **Step 2: Run a full build**

```bash
./gradlew :fabric:build
```

Expected: BUILD SUCCESSFUL, producing a remapped jar under `fabric/1.21.1/build/libs/`.

- [ ] **Step 3: Re-run the full manual checklist end to end**

Repeat all manual verification steps from Task 5 Step 5 and Task 6 Step 6 in one continuous server session (not just per-task in isolation), to catch any interaction between join-unlock, commands, exclusion config, and toast suppression.

- [ ] **Step 4: Document configuration in the README**

Update `README.md` to add:

```markdown
## Configuration

Just Recipe Unlocker writes `config/justrecipeunlocker/justrecipeunlocker.toml`
on first run:

| Key | Default | Description |
| --- | --- | --- |
| `unlock_on_join` | `true` | Unlock all non-excluded recipes when a player joins |
| `excluded_namespaces` | `[]` | Exclude all recipes from these mod ids/namespaces |
| `excluded_recipe_ids` | `[]` | Exclude individual recipes by `namespace:path` id |
| `suppress_recipe_toast` | `false` | Suppress the "New recipes unlocked!" toast |
| `suppress_tutorial_toast` | `false` | Suppress recipe-related tutorial toasts |

## Commands

- `/justrecipeunlocker unlock <targets>` — unlock recipes for the given player(s) (op level 2)
- `/justrecipeunlocker unlockAll` — unlock recipes for all online players (op level 2)
- `/jru` — alias for `/justrecipeunlocker`

Currently supports Minecraft 1.21.1 on Fabric.
```

- [ ] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: document Phase 1 configuration and commands"
```

---

## Phase 1 exit criteria

- `./gradlew :common:test` and `./gradlew :fabric:build` both succeed.
- The manual checklist in Task 7 Step 3 passes end to end against a real vanilla client.
- The `suppress_tutorial_toast` finding from Task 6 Step 4 is reported, whichever way it resolves.
- Phase 2 (`neoforge/1.21.1`, `forge/1.21.1`) is out of scope for this plan and starts only once this phase is merged, per the spec's rollout plan.
