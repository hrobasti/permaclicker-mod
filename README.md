# PermaClicker ⛏️

PermaClicker keeps client-side mining/click input active while the mod is enabled.

It is intended for single-player or explicitly allowed environments, with configurable safety limits.

[!WARNING]
**Anti-cheat and server notice**

PermaClicker automates input behavior.
Use it only where this is explicitly allowed.
On many servers this may violate rules and can result in penalties such as kick/ban.

## Quick start (for players) 🎮

1. Put **both** mods in the same `mods` folder:
   - `permaclicker`
   - `turtle-lib-mod` (required dependency)
2. Start the game.
3. Enable/configure PermaClicker in-game via the config screen.

Without `turtle-lib-mod`, PermaClicker cannot load correctly.

## What PermaClicker provides ✨

- configurable toggle key
- in-game config screen for Fabric and NeoForge
- overlay output
- runtime mode: **Focused** or **Background**
- auto-stop timer (`0..9999` minutes)
- optional movement lock while active
- optional update checks (Stable / Beta / Alpha)

## Runtime behavior (current) 🧭

- While PermaClicker is active, opening chat is intentionally blocked.
- In **Focused** mode, `ESC` stops PermaClicker and opens the pause menu.
- In **Background** mode, pause opening stays blocked while active.

## Configuration ⚙️

Most settings are managed directly in the in-game config screen.

Additional update source metadata is stored in `update-sources.properties`.

## Supported languages 🌍

PermaClicker bundles each locale as a JSON file. Included languages:

🇺🇸 English (en_US)
🇩🇪 German (de_DE)
🇸🇦 Arabic (ar_SA)
🇪🇸 Spanish (es_ES)
🇫🇷 French (fr_FR)
🇮🇹 Italian (it_IT)
🇯🇵 Japanese (ja_JP)
🇰🇷 Korean (ko_KR)
🇳🇱 Dutch (nl_NL)
🇵🇱 Polish (pl_PL)
🇵🇹 Portuguese (pt_PT)
🇹🇷 Turkish (tr_TR)
🇺🇦 Ukrainian (uk_UA)
🇨🇳 Simplified Chinese (zh_CN)

## Build quickstart (developers) 🛠️

PermaClicker ships loader-local Gradle wrappers, so no global Gradle install is required.

- Fabric build entrypoints: `fabric/gradlew` (Linux/macOS), `fabric/gradlew.bat` (Windows)
- NeoForge build entrypoints: `neoforge/gradlew` (Linux/macOS), `neoforge/gradlew.bat` (Windows)

Typical tasks: `build`, `test`, `tasks`.

## AI support & privacy transparency 🤖

Parts of the source code and documentation were created with AI assistance.

- **No personal player data is used** in this process.
- Ideas, design decisions, and quality standards come from humans.
- Quality assurance is reviewed and validated by humans.

## Developer notes (short) 🧩

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
