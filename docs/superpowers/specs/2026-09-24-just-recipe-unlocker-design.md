# Just Recipe Unlocker Design

- Date: 2026-09-24
- Status: Draft (pending review)

## Overview

A server-side mod that unlocks all crafting recipes in the recipe book when a
player joins. Existing "unlock all recipes" mods are typically client-side,
single-loader, and single-version, and have not kept pace with recent
protocol changes in newer Minecraft versions. This mod fills that gap with a
lightweight, dependency-light, server-installed mod that works across
multiple loaders and versions, and requires no client-side mod (a vanilla
client works out of the box).

## Goals

- Automatically unlock recipes for players on join (all recipes, minus any
  excluded ones)
- Admin commands to unlock recipes for an individual player or everyone
- Exclusion configuration at the namespace level and the individual recipe
  ID level
- Automatically include recipes added by datapacks or other mods (no
  hardcoded recipe list)
- Independent settings to suppress the recipe-unlock toast notification and
  the tutorial toast notification
- Works with a vanilla client (no client-side mod required)
- Support Fabric, NeoForge, and Forge, targeting 1.20.1, 1.21.1, and the
  latest stable release
- Publish on CurseForge (Modrinth deferred)

## Non-goals

- A client-side GUI / settings screen
- Changing or adding recipe content itself (visibility only)
- Any runtime dependency on third-party API mods beyond Fabric's own API mod

## Architecture

The mod uses a multi-loader build (Fabric, NeoForge, Forge), without Mixin.
Each loader uses its own standard, loader-native Gradle toolchain: plain
`fabric-loom` for Fabric, and ModDevGradle/ForgeGradle for NeoForge/Forge
(added in later phases). Source sharing across loaders is done through
plain Gradle project structure (a `common/<version>` module consumed by
each loader module, plus a version-independent `common/shared` source set),
not through a forked-Loom multiloader tool. This is a build-time-only
concern; none of these tools introduce a runtime dependency on any
third-party API mod. The only runtime mod dependency is Fabric API on the
Fabric build, which is the de facto standard dependency for Fabric mods and
is used here to detect player join events.

Recipe unlocking and exclusion filtering are implemented entirely with
vanilla public APIs (`RecipeManager`, `ServerPlayer#getRecipeBook()`, etc.).
No Mixin is used, which avoids the class of runtime-only bugs that can arise
from Mixin remap/refmap toolchains differing across Forge/NeoForge build
setups.

### Directory layout

```
common/shared/        … config load/save, exclusion-filter logic (version-independent)
common/<version>/     … per-version recipe-unlock API calls, event wiring
fabric/base/, fabric/<version>/
forge/base/, forge/<version>/
neoforge/base/, neoforge/<version>/
gradle/shared          … github.com/ksoichiro/minecraft-mod-gradle-scripts (submodule)
props/<version>.properties
```

- Mod id: `justrecipeunlocker`
- Root package: `com.justrecipeunlocker`
- License: LGPL-3.0-only

## Configuration

No GUI. Server-side TOML config only (a client-side GUI would require a
client mod, which conflicts with the vanilla-client requirement).

```toml
unlock_on_join = true

# Exclude recipes in bulk by namespace (mod id)
excluded_namespaces = []

# Exclude individual recipes by id (namespace:path)
excluded_recipe_ids = []

# Suppress the "New recipes unlocked!" toast (independent of unlock behavior)
suppress_recipe_toast = false

# Suppress recipe-related tutorial toasts (independent setting)
suppress_tutorial_toast = false
```

- `excluded_namespaces` and `excluded_recipe_ids` together give a consistent
  way to exclude recipes on versions where vanilla recipe tags are not
  available or are limited (e.g. 1.20.1).
- Exclusion by recipe tag (`excluded_recipe_tags`) is deferred to a later
  phase, scoped to versions where the vanilla recipe-tag mechanism is
  available. It is not required to meet the stated goals, which are already
  satisfied by namespace- and id-based exclusion.
- The config is loaded at startup (not per-join) and used to build the
  unlock/exclusion decision for join events and commands alike.

## Commands

- `/justrecipeunlocker unlock <targets>` — immediately unlock all
  non-excluded recipes for the given player(s) (selector support)
- `/justrecipeunlocker unlockAll` — immediately unlock for all online
  players
- Permission level: op (level 2)
- Alias: `/jru`

## Recipe unlock / exclusion logic

1. On server start (or first use), read all recipes from `RecipeManager`.
2. Filter out recipes matching `excluded_namespaces` / `excluded_recipe_ids`
   to build the set of recipe ids to unlock. Because this reads from
   `RecipeManager`, datapack- and mod-added recipes are automatically
   included without any hardcoded list.
3. When `unlock_on_join` is true, apply the unlock set to a player on the
   join event (Fabric: `ServerPlayConnectionEvents.JOIN`; NeoForge/Forge:
   the equivalent player-join event).
4. Commands apply the same unlock logic to their target player(s).

## Notification suppression

- `suppress_recipe_toast`: prevents a burst of "New recipes unlocked!"
  toasts when many recipes are unlocked at once. The exact mechanism (a
  silent-unlock API path vs. suppressing the toast packet) differs across
  versions and will be investigated during implementation.
- `suppress_tutorial_toast`: suppresses recipe-related tutorial toasts.
  This is likely a different mechanism from the recipe toast, hence a
  separate flag; implementation details are investigated during
  implementation.

## Testing strategy

- For each supported version/loader combination, verify basic behavior
  using the `runServer`/`runClient` tasks provided by `gradle/shared`.
- Because the design avoids Mixin, the risk of remap-only runtime bugs is
  low, but every supported version is still built and launched via the
  `for-each-version` workflow.
- Manual verification: connect to a modded server with a vanilla client and
  confirm join-time unlock, command-based unlock, exclusion config, and
  notification suppression all behave as expected.

## Rollout phases

1. Implement `common/1.21.1` + `fabric/1.21.1` only; validate unlock logic,
   exclusion, commands, and notification suppression end to end.
2. Add `neoforge/1.21.1` and `forge/1.21.1`.
3. Add `1.20.1` across all three loaders (absorbing per-version API
   differences).
4. Add the latest stable release; maintain ongoing version coverage via the
   `for-each-version` workflow from this point on.
5. Set up CurseForge publishing (Modrinth is out of scope for now).

Each phase is an independently verifiable unit, so that design risks
(recipe-unlock API behavior, notification suppression mechanism) are
resolved in phase 1 before expanding to the rest of the matrix.

## Open questions

- The concrete mechanism for toast/tutorial notification suppression needs
  per-version API research; this is scoped as part of phase 1 in the
  implementation plan.
