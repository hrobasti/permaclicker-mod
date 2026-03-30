# PermaClicker NeoForge Matrix

## Source of truth

- Matrix targets are provided through the build configuration for this module.
- For NeoForge, the authoritative fields are:
  - `minecraft`
  - `neoforge`
  - `java`

## Pipeline strategy

- Matrix build with isolated Gradle user homes per target.

## Artifact pattern

`PermaClicker_NeoForge_MC-X-Y-Z_NEOFORGE-A-B-C.jar`
