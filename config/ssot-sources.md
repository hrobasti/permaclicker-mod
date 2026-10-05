# PermaClicker – Single Source of Truth

## Authoritative sources

- `config/matrix-targets.json`
  - Authoritative per target values:
    - `minecraft`
    - `java`
    - `neoforge`
    - `fabricLoader`
    - `fabricApi`
    - `modmenu`
- `version.properties`
  - Authoritative mod version (`version`).
  - Required TurtleLib release (`turtlelib_dependency_version`). This is the core jar compiled against in standalone builds and the minimum TurtleLib version in `fabric.mod.json` / `neoforge.mods.toml`.
- `config/mod.properties`
  - Mod identity (`mod_id`, `mod_name`, `mod_group`).

## Derived locations

- `gradle/matrix-defaults.gradle` reads the matrix for every module build. Without `-Pminecraft_version`, the first target is the default. Explicit `-P` values (`neoforge_version`, `fabric_loader_version`, …) override single versions.
- `gradle/mod-info.gradle` reads `version.properties` and `config/mod.properties`.
- The repository root `build.gradle` derives the matrix release tasks (`releasePermaClicker`) from the matrix.
- No copies of these values exist in `gradle.properties`, neither in this repository nor in the hrobasti-mods workspace root.

## Change workflow

1. Change `config/matrix-targets.json` first.
2. Keep docs/build metadata in sync after the matrix update.
3. Run `./gradlew verifyMatrixTargets` before merge. Inside the hrobasti-mods workspace, the root task additionally checks that all mods agree on the first target's Minecraft/Java versions.

## PermaClicker-specific policy

- Fabric API minimum version is maintained per matrix target.
- NeoForge selection per MC branch is stable-first; if no stable exists, use latest beta.
- When PermaClicker starts using a newer TurtleLib API, raise `turtlelib_dependency_version` to the first TurtleLib release that provides it.
