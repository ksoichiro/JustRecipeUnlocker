# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
