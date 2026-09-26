# Just Recipe Unlocker

Unlocks all crafting recipes for players on join. A lightweight,
server-installed mod, no client-side mod required.

## Configuration

Just Recipe Unlocker writes `config/justrecipeunlocker.toml`
on first run:

| Key | Default | Description |
| --- | --- | --- |
| `unlock_on_join` | `true` | Unlock all non-excluded recipes when a player joins |
| `excluded_namespaces` | `[]` | Exclude all recipes from these mod ids/namespaces |
| `excluded_recipe_ids` | `[]` | Exclude individual recipes by `namespace:path` id |
| `suppress_recipe_toast` | `false` | Suppress the "New recipes unlocked!" toast for admin commands |
| `suppress_tutorial_toast` | `false` | Currently has no effect (reserved for future/config compatibility) |

## Commands

- `/justrecipeunlocker unlock <targets>`, unlock recipes for the given player(s) (op level 2)
- `/justrecipeunlocker unlockAll`, unlock recipes for all online players (op level 2)
- `/jru`, alias for `/justrecipeunlocker`

Currently supports Minecraft 1.21.1, 26.1.2, 26.2, and 26.3 on Fabric and NeoForge.

## License

LGPL-3.0-only. See [COPYING](COPYING) and [COPYING.LESSER](COPYING.LESSER).
