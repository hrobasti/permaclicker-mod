# PermaClicker ⛏️

PermaClicker keeps your attack input going while you're AFK: it mines continuously for block farms such as a cobblestone generator, or hits mobs and animals at full strength for mob and animal farms. It is intended for single-player or explicitly allowed environments, with configurable safety limits.

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

- click mode: **Mining** (holds attack to mine the targeted block) or **Mobs & Animals** (hits the targeted mob or animal whenever the attack cooldown is fully charged; never attacks players)
- configurable attack buffer for **Mobs & Animals** (`0..20` ticks, default `2`) as a safety margin against server lag
- runtime mode: **Focused** or **Background**
- auto-stop timer (`0..9999` minutes)
- optional movement lock while active
- status overlay in the action bar showing the active mode and remaining auto-stop time (can be disabled, configurable color)
- rebindable hotkeys and an in-game config screen for Fabric and NeoForge
- optional update checks via Modrinth and CurseForge (Stable / Beta / Alpha)

## Runtime behavior 🧭

- While PermaClicker is active, opening chat is intentionally blocked.
- In **Focused** mode, PermaClicker only runs while the game window is focused. `ESC` stops PermaClicker and opens the pause menu, `U` stops it and opens the settings.
- In **Background** mode, PermaClicker keeps running while the game window is unfocused or minimized. The pause menu and PermaClicker's own settings stay blocked while it is active; press `F7` to stop it first.
- The auto-stop timer only counts down while PermaClicker is actually working, so it pauses instead of running out in the background (e.g. Focused mode with an unfocused window).
- **Mobs & Animals** never jumps for you: hits land at full strength, but they are not jump critical hits.

## Tool durability

If you care about tool or weapon durability (they can break while PermaClicker runs), you can use [Inventory Profiles Next](https://inventory-profiles-next.github.io/en/downloads/) to replace them before they break or after they're broken.

## Configuration ⚙️

All settings are managed directly in the in-game config screen. Hotkeys are rebound in Minecraft's controls menu.

## Supported languages 🌍

PermaClicker bundles each locale as a JSON file. Included languages:

- 🇺🇸 English (en\_US)
- 🇩🇪 German (de\_DE)
- 🇸🇦 Arabic (ar\_SA)
- 🇪🇸 Spanish (es\_ES)
- 🇫🇷 French (fr\_FR)
- 🇮🇹 Italian (it\_IT)
- 🇯🇵 Japanese (ja\_JP)
- 🇰🇷 Korean (ko\_KR)
- 🇳🇱 Dutch (nl\_NL)
- 🇵🇱 Polish (pl\_PL)
- 🇵🇹 Portuguese (pt\_PT)
- 🇹🇷 Turkish (tr\_TR)
- 🇺🇦 Ukrainian (uk\_UA)
- 🇨🇳 Simplified Chinese (zh\_CN)

## Quick start for devs 🛠️

This repository is a standalone Gradle build. Clone it and use its own wrapper; no other repository is needed. A JDK 17+ must be installed to start Gradle. The Java toolchain required for compiling is detected automatically or downloaded.

- Build and test: `./gradlew build`, `./gradlew testPermaClicker`
- Validate the build matrix: `./gradlew verifyMatrixTargets`
- Full release build (all matrix targets, Fabric + NeoForge) into `dist/`: `./gradlew releasePermaClicker`

Minecraft, Java and loader versions come from [`config/matrix-targets.json`](config/matrix-targets.json). Without parameters, builds use the first target in that file.

**TurtleLib:** the build downloads the [TurtleLib](https://github.com/hrobasti/turtle-lib-mod) core jar from its GitHub release. The version is set by `turtlelib_dependency_version` in [`version.properties`](version.properties).

To work on both at once, clone TurtleLib next to this repository (`../turtle-lib-mod`) and add `-PuseLocalTurtleLib=true`.

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
