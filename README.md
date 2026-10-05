# PermaClicker ⛏️

PermaClicker keeps client-side mining/click input active while the mod is enabled, e. g. to provide a cobblestone farm while you're AFK. It is intended for single-player or explicitly allowed environments, with configurable safety limits.

## Anti-cheat notice ⚠️

PermaClicker automates input behavior.
Use it only where this is explicitly allowed.
On many servers this may violate rules and can result in penalties such as kick/ban.

## Quick start for players 🎮

1. Put `PermaClicker` and `TurtleLib` (required dependency) in the `mods` folder.
2. Start the game.
3. Configure Hotkeys in Minecraft key binds. Defaults:  `u` for Options, `F7` toggle on/off.
4. Configure PermaClicker in-game via the config screen.

## What PermaClicker provides ✨

- configurable toggle key
- in-game config screen for Fabric and NeoForge
- overlay output
- runtime mode: **Focused** or **Background**
- click mode: **Mining** (hold attack on blocks) or **Mobs & Animals** (full-strength hits on mobs and animals)
- auto-stop timer (`0..9999` minutes)
- optional movement lock while active
- optional update checks (Stable / Beta / Alpha)

## Runtime behavior 🧭

- While PermaClicker is active, opening chat is intentionally blocked.
- **Focused** mode: `ESC` stops PermaClicker and opens the pause menu.  `u` stops PermaClicker and opens the config screen.
- **Background** mode: pause screen and PermaClicker's own config screen (`u`) stay blocked while active — even if the game window is currently focused. Disable PermaClicker (`F7`) first to change settings.
- **Mining** click mode (default): keeps the attack key held to continuously mine the targeted block.
- **Mobs & Animals** click mode: hits the mob or animal in your crosshair once the attack cooldown is fully charged, plus a configurable buffer (`0..20` ticks, default `2`). No block is mined and nothing happens while no mob or animal is targeted. These are full-strength hits, not jump crits.

## Tool durability

If you care about tool durability (breaking while PermaCLickers runs), you can use [Inventory Profiles Next](https://inventory-profiles-next.github.io/en/downloads/) to replace them before they break or after they're broken.

## Configuration ⚙️

Most settings are managed directly in the in-game config screen.

## Supported languages 🌍

PermaClicker bundles each locale as a JSON file. Included languages:

- 🇺🇸 English (en_us)
- 🇩🇪 German (de_de)
- 🇸🇦 Arabic (ar_sa)
- 🇪🇸 Spanish (es_es)
- 🇫🇷 French (fr_fr)
- 🇮🇹 Italian (it_it)
- 🇯🇵 Japanese (ja_jp)
- 🇰🇷 Korean (ko_kr)
- 🇳🇱 Dutch (nl_nl)
- 🇵🇱 Polish (pl_pl)
- 🇵🇹 Portuguese (pt_pt)
- 🇹🇷 Turkish (tr_tr)
- 🇺🇦 Ukrainian (uk_ua)
- 🇨🇳 Simplified Chinese (zh_cn)

## Quick start for devs 🛠️

Use the workspace root wrapper for all build/test tasks.

- PermaClicker full matrix build: `./gradlew releasePermaClicker`
- Typical checks: `./gradlew verifyMatrixTargets`, `./gradlew tasks`

## AI support & privacy transparency 🤖

Parts of the source code and documentation were created with AI assistance.

- **No personal player data is used** in this process.
- Ideas, design decisions, and quality standards come from humans.
- Quality assurance is reviewed and validated by humans.

## Developer notes 🧩

PermaClicker uses TurtleLib helpers in `common` intentionally via facades:

- `LocaleFacade`
- `MessageFacade`
- `UpdateFacade`
- `VersionFacade`

Direct TurtleLib imports are intentionally limited to these integration points.

## License 📄

Apache-2.0

Full license text:

- [`LICENSE`](LICENSE)

Third-party license texts/references:

- [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)
