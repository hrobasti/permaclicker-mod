# Third-Party Notices

This file documents third-party dependencies and license references for **PermaClicker**.

## Dependencies and licenses

| Library | Coordinates / source | Usage scope | License | License text |
| --- | --- | --- | --- | --- |
| TurtleLib | [hrobasti/turtle-lib-mod](https://github.com/hrobasti/turtle-lib-mod) (core jar from GitHub releases; not bundled, installed as its own mod) | Runtime dependency | Apache-2.0 | [TurtleLib LICENSE](https://github.com/hrobasti/turtle-lib-mod/blob/HEAD/LICENSE) |
| Gson | `com.google.code.gson:gson` | Compile dependency (provided by Minecraft at runtime, not bundled) | Apache-2.0 | [Gson LICENSE](https://github.com/google/gson/blob/main/LICENSE) |
| Fabric API | `net.fabricmc.fabric-api:fabric-api` | Fabric runtime/API | Apache-2.0 | [Fabric LICENSE](https://github.com/FabricMC/fabric/blob/HEAD/LICENSE) |
| NeoForge API | `net.neoforged:neoforge` | NeoForge runtime/API | LGPL-2.1 | [NeoForge LICENSE](https://github.com/NeoForged/NeoForge/blob/1.13-pre/LICENSE.txt) |

## Local compliance notes

- PermaClicker itself is licensed under Apache-2.0 (see [`LICENSE`](LICENSE)).
- Produced JARs include `META-INF/LICENSE` from this mod folder.
- This file is the mod-local source for dependency/license notices and compliance metadata.
