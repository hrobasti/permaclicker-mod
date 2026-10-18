# PermaClicker ⛏️

PermaClicker keeps your attack input going while you're AFK: it mines continuously for block farms such as a cobblestone generator, or hits mobs and animals at full strength for mob and animal farms. It is intended for single-player or explicitly allowed environments, with configurable safety limits.

## Anti-cheat notice ⚠️

PermaClicker automates input behavior. Use it only where this is explicitly allowed. On many servers this may violate rules and can result in penalties such as kick/ban.

## Quick start for players 🎮

1. Put `PermaClicker` and `TurtleLib` (required dependency) in the `mods` folder.
2. Start the game.
3. Adjust the hotkeys in Minecraft's key binds if needed. Defaults: `F7` toggles PermaClicker on/off, `U` opens its settings.
4. Configure PermaClicker in-game via the config screen (on Fabric also available through Mod Menu).

## Update together ⚠️

PermaClicker 2.0.0 requires TurtleLib 2.0.0 or newer, and TurtleLib 2.0.0 does not work with older PermaClicker versions. Update both mods together.

## What PermaClicker provides ✨

- click mode: **Mining** (holds attack to mine the targeted block; never hits mobs, animals or players) or **Mobs & Animals** (hits the targeted mob or animal, including villagers and pets, whenever the attack cooldown is fully charged; never attacks players)
- configurable attack buffer for **Mobs & Animals** (`0..20` ticks, default `2`) as a safety margin against server lag
- runtime mode: **Focused** or **Background**
- auto-stop timer (`0..9999` minutes)
- optional movement lock while active
- status overlay in the action bar showing the active mode and remaining auto-stop time (can be disabled, configurable color)
- rebindable hotkeys and an in-game config screen for Fabric and NeoForge
- update check via Modrinth and CurseForge (Stable / Beta / Alpha), can be turned off (see below)

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

## Update check & privacy 🔔

When the game starts, PermaClicker checks once in the background whether a newer version exists, and shows a chat message with links after you join a world. The check is on by default; turn it off with **Update check** in the settings. The channel (**Stable**: releases only, **Beta**: releases and betas, **Alpha**: everything) decides which versions count.

- **Queried services** (only platforms where PermaClicker is published): `api.modrinth.com` (Modrinth, official API) and `api.cfwidget.com` (CFWidget, an unofficial third-party service that mirrors CurseForge data; it publishes no privacy policy and states that it stores no identifiable information).
- **Transmitted data:** your IP address (part of every web request), the mod name and version (user agent), the Minecraft version, the loader (Fabric or NeoForge) and the project ID. No account, player or world data.
- Modrinth's handling of request data: [Modrinth privacy policy](https://modrinth.com/legal/privacy).
- With Mod Menu installed (Fabric), Mod Menu additionally checks your installed mods on Modrinth; that check is configured in Mod Menu's own settings.

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

PermaClicker uses TurtleLib only through `UpdateFacade` in `common`; no other code imports TurtleLib. Translations use Minecraft's lang files.

## License 📄

MIT

Full license text:

- [`LICENSE`](LICENSE)

Third-party license texts/references:

- [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)
