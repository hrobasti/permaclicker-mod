# Third-Party Notices

This file documents third-party dependencies and license references for **PermaClicker**.

## Dependencies and licenses

| Library             | Coordinates / source                                                                                                                         | Usage scope                                                                      | License    | License text                                                                         |
| ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- | ---------- | ------------------------------------------------------------------------------------ |
| TurtleLib           | [hrobasti/turtle-lib-mod](https://github.com/hrobasti/turtle-lib-mod) (core jar from GitHub releases; not bundled, installed as its own mod) | Runtime dependency                                                               | MIT        | [TurtleLib LICENSE](https://github.com/hrobasti/turtle-lib-mod/blob/HEAD/LICENSE)    |
| Gson                | `com.google.code.gson:gson`                                                                                                                  | Compile dependency (provided by Minecraft at runtime, not bundled)               | Apache-2.0 | [Gson LICENSE](https://github.com/google/gson/blob/main/LICENSE)                     |
| Fabric API          | `net.fabricmc.fabric-api:fabric-api`                                                                                                         | Fabric runtime/API                                                               | Apache-2.0 | [Fabric LICENSE](https://github.com/FabricMC/fabric/blob/HEAD/LICENSE)               |
| NeoForge API        | `net.neoforged:neoforge`                                                                                                                     | NeoForge runtime/API                                                             | LGPL-2.1   | [NeoForge LICENSE](https://github.com/neoforged/NeoForge/blob/HEAD/LICENSE.txt)      |
| Mod Menu            | `com.terraformersmc:modmenu`                                                                                                                 | Optional Fabric integration for the config screen (not bundled)                  | MIT        | [Mod Menu LICENSE](https://github.com/TerraformersMC/ModMenu/blob/HEAD/LICENSE)      |
| SpongePowered Mixin | `net.fabricmc:sponge-mixin`                                                                                                                  | Mixin framework, provided by Fabric Loader and NeoForge at runtime (not bundled) | MIT        | [Mixin LICENSE](https://github.com/SpongePowered/Mixin/blob/HEAD/LICENSE.txt)        |
| Fabric Loader       | `net.fabricmc:fabric-loader`                                                                                                                 | Fabric runtime (not bundled)                                                     | Apache-2.0 | [Fabric Loader LICENSE](https://github.com/FabricMC/fabric-loader/blob/HEAD/LICENSE) |
| SLF4J               | `org.slf4j:slf4j-api`                                                                                                                        | Logging API, provided by Minecraft at runtime (not bundled)                      | MIT        | [SLF4J LICENSE](https://github.com/qos-ch/slf4j/blob/HEAD/LICENSE.txt)               |
| JUnit Jupiter       | `org.junit.jupiter:junit-jupiter`                                                                                                            | Test-only (not bundled)                                                          | EPL-2.0    | [JUnit LICENSE](https://github.com/junit-team/junit-framework/blob/HEAD/LICENSE.md)  |

## Local compliance notes

- PermaClicker itself is licensed under MIT (see [`LICENSE`](LICENSE)); releases up to 1.4.0 were Apache-2.0.
- Produced JARs include `META-INF/LICENSE` from this mod folder.
- This file is the mod-local source for dependency/license notices and compliance metadata.
