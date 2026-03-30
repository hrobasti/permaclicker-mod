# PermaClicker Fabric Matrix

## Source of truth

- Matrix targets are provided through the build configuration for this module.
- For Fabric, the authoritative fields are:
  - `minecraft`
  - `fabricLoader`
  - `fabricApi`
  - `java`

## Pipeline strategy

- `MC 1.21.x` targets use classic Loom remap flow.
- `MC 26.x` targets use unobfuscated Loom flow (runtime artifact from `jar`, no remap task).
- Matrix targets run isolated under `isolated/gradle-user-home/...`.

## Artifact pattern

`PermaClicker_Fabric_MC-X-Y-Z_FABRIC-A-B-C.jar`
