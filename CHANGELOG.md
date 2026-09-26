# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Minecraft 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8, 1.21.9, 1.21.10, and 1.21.11
  support for Fabric and NeoForge (target versions chosen to match Just Coordinates'
  supported 1.21.x releases).

### Fixed

- Fabric: recipes were not unlocked on player join on Minecraft 1.21.9/1.21.10 (confirmed via
  real-server testing; the `/justrecipeunlocker unlockAll` command still worked). As on
  26.1.2+, Fabric's JOIN event fires before the player is registered in the player list on
  these versions. A first fix that reused the 26.1.2+ retry (a recursive `server.execute()`
  poll) still failed real-server testing: `server.execute()` runs its task inline when called
  from the server thread the JOIN event already fires on, so all 100 "ticks" of that retry ran
  in zero elapsed real time and could never observe the later registration (confirmed with
  temporary debug logging on a real dedicated server + client). Replaced it with a queue
  processed once per genuine tick via `ServerTickEvents.END_SERVER_TICK`, which guarantees each
  retry attempt runs on a separate real tick.
- NeoForge: the mod failed to load at all on Minecraft 1.21.3/1.21.4 (confirmed via real-server
  testing), crashing with `IllegalArgumentException: IModBusEvent events are not allowed on the
  common NeoForge bus! Use a mod bus instead.` `RegisterGameTestsEvent` is a mod-bus-only event,
  but the `@EventBusSubscriber`-annotated GameTest registration class relied on automatic bus
  detection, which this NeoForge version validates more strictly than 1.21.1 (where the same
  code loads without error). Replaced it with an explicit `modEventBus.addListener(...)` call
  from the mod constructor, matching the pattern already used on 1.21.5+.

## [0.2.0] - 2026-09-26

### Added

- Minecraft 26.1.2, 26.2, and 26.3 support for Fabric and NeoForge.

### Changed

- Fabric builds for Minecraft 26.1.2/26.2/26.3 now use Fabric's new no-remap toolchain
  (`net.fabricmc.fabric-loom` plugin) instead of the legacy `fabric-loom` plugin, since
  Minecraft ships unobfuscated from 26.1 onward and no longer publishes official mappings.
  Minecraft 1.21.1 is unaffected and keeps using the legacy `fabric-loom` plugin.
- Bumped the Gradle wrapper from 9.4.0 to 9.8.0 and the `mise` Java toolchain from
  Temurin 21 to Temurin 25 (Minecraft 26.1+ requires Java 25 to run Gradle itself).

### Fixed

- Fabric: recipes were not unlocked on player join on Minecraft 26.1.2/26.2/26.3 (the
  `/justrecipeunlocker unlockAll` command still worked). Fabric's join event now fires before
  the player is registered in the player list on these versions, so the existing "wait one
  tick, then check the player is still valid" guard always failed and silently skipped the
  unlock. Replaced it with a per-tick retry that waits until the player is actually registered
  (bounded to 100 ticks) and checks `player.isRemoved()` instead, which doesn't depend on this
  registration-timing change.

[Unreleased]: https://github.com/ksoichiro/JustRecipeUnlocker/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/ksoichiro/JustRecipeUnlocker/releases/tag/v0.2.0
