# Architecture Review TODOs — State Machine & Mixins

Findings from a deep-dive review of the PermaClicker state machine (`PermaClickService`,
`PermaClickClientController`) and the four client mixins (`*ChatMiningAttackScreenBypassMixin`,
`*ChatOpenBlockMixin`, `*PauseScreenSetScreenMixin`, `*PauseEscKeyMixin`) on both Fabric and
NeoForge. Captured 2026-09-16. All nine findings (F1-F9) resolved 2026-09-16 — see each section.

## F1 — Loader-neutral code duplicated between fabric/ and neoforge/

- [x] Extract the shared runtime engine into `permaclicker-common` / a new module

Large parts of the "runtime execution engine" behind the mixins were byte-for-byte identical
between `fabric/` and `neoforge/`. Chosen approach (discussed with maintainer 2026-09-16): move
truly loader-neutral (zero-Minecraft-dependency) pieces straight into the existing
`permaclicker-common` module; anything that genuinely needs Minecraft classes gets a **new,
separate module** (not `common`, which stays MC-free by design) — that second part is not started
yet and needs a build-engineering feasibility check first (see below).

**Done 2026-09-16 — moved into `permaclicker-common` (zero Minecraft dependency, verified by
reading every file before moving: only `java.util.*` and the mod's own `common` classes):**
- `FabricPermaClickBridge.java` + `NeoForgePermaClickBridge.java` → `common/.../core/PermaClickBridge.java`
- `FabricRuntimeBindings.java` + `NeoForgeRuntimeBindings.java` → `common/.../core/PermaClickRuntimeBindings.java`
- `FabricClientEventLoop.java` + `NeoForgeClientEventLoop.java` → `common/.../core/PermaClickClientEventLoop.java`
- Their per-loader tests (`FabricBridgeEventLoopTest` / `NeoForgeBridgeEventLoopTest`) merged into
  one `common/.../core/PermaClickBridgeEventLoopTest.java`.
- All call sites in both entrypoints, both `ConfigLifecycle` classes, both config-screen
  templates, and both `ConfigLifecycleTest`s updated. Full `testPermaClicker`/`testTurtleLib`
  build verified green after the move.

**Done 2026-09-16 — new `:permaclicker-client` module created and wired up for the
Minecraft-touching pieces (see feasibility check below for why this was safe):**
- New Gradle module `permaclicker-client` (`mods/permaclicker-mod/client/`), registered in
  `settings.gradle`, applying `fabric-loom` (compiling against the Fabric-shaped Minecraft
  classpath per the feasibility rule below) + `java-library`, depending on `permaclicker-common`.
  `fabric-loader`/`fabric-api` are `compileOnly` there (needed only so Loom's compile classpath
  resolves; not leaked as transitive runtime deps to the NeoForge consumer).
- `FabricFocusedModeExecutor.java` + `NeoForgeFocusedModeExecutor.java` → `client/.../client/FocusedModeExecutor.java`
- `FabricBackgroundModeExecutor.java` + `NeoForgeBackgroundModeExecutor.java` → `client/.../client/BackgroundModeExecutor.java`
  (also confirmed byte-identical before moving; uses `Minecraft`, `KeyMapping`,
  `MultiPlayerGameMode`, `BlockPos`, `Direction`, `PauseScreen`, `BlockHitResult` — all verified
  compatible between the Fabric and NeoForge jars, see below)
- Both loader `build.gradle`s now `implementation project(':permaclicker-client')` and bundle its
  compiled output into the loader jar (`from(clientMainOutput)`, same pattern as the existing
  `commonMainOutput` bundling). Verified by inspecting the actual built jars — both
  `PermaClicker_..._FABRIC_....jar` and `PermaClicker_..._NEOFORGE_....jar` now contain identical
  `com/github/hrobasti/permaclicker/client/*.class` entries.
- Root `build.gradle`'s `testPermaClicker` aggregate task now also depends on
  `:permaclicker-client:test`.
- Full `testPermaClicker`/`testTurtleLib` plus real `:permaclicker-fabric:jar` /
  `:permaclicker-neoforge:jar` builds verified green after the move.

**Done 2026-09-16 — the remaining entrypoint predicate/helper methods extracted too**, split into
five focused classes in `:permaclicker-client` rather than one god-class:
- `ReflectionCompat` — generic version-compat reflection helpers (`getAccessibleMethod`,
  `getAccessibleField`, `invokeCompatibleMethod(ByShape)`, `isCompatible`/`isAssignable`,
  `read/writeBooleanFromOption`). NeoForge's own key-category resolution code (previously a 5th
  private duplicate of `getAccessibleMethod`) now calls this too instead of keeping its own copy.
- `PermaClickGameContext` (extended) — `isWindowMinimized`, `isChatMiningContextActive`,
  `isChatBlockContextActive`, `isMiningAllowedScreen`, `isInGameHotkeyContext`,
  `stopForFocusedEscPause`, `resolveOverlayColor`.
- `ClientMessages` — `displayClientMessageCompat`, `displayProviderLine`, `resolveCurrentVersion`.
- `PauseOnLostFocusOverride` (stateful, one instance per entrypoint) — replaces the
  `pauseOnLostFocusOverridden`/`originalPauseOnLostFocusValue` fields + their read/write methods.
- `MovementLockController` (stateful) — replaces `movementLockActive`/`hasLockedView`/
  `lockedYaw`/`lockedPitch` + their apply/capture methods.
- `MiningRuntimeState` (stateful) — owns the `FocusedModeExecutor`/`BackgroundModeExecutor`
  instances plus `attackRequestedThisTick`/`attackHoldForcedByPermaClick`/
  `backgroundCursorFreeActive`/`backgroundPauseSuppressedThisTick`.
- `UpdateNoticeController` (stateful) — owns `pendingUpdateStatus`/`pendingToggleKeyResetNotice`
  and their schedule/flush methods.

Also removed the `updateCursorCapture(Minecraft)` method while touching this code — it was a
literal no-op (empty body, just a comment) called from several places in both entrypoints; deleted
both the method and its call sites rather than moving dead code into the new module.

Net effect: `PermaClickFabricEntrypoint.java` went from ~1187 lines to 582;
`PermaClickNeoForgeEntrypoint.java` went from ~955 to 349. What's left in each is genuinely
loader-specific: event/lifecycle registration, `KeyMapping` creation and registration mechanics
(materially different between Fabric and NeoForge), and the config-screen reflection glue.

**Feasibility check — done 2026-09-16, result: feasible.** `permaclicker-common` is a plain
`java-library` module with zero Minecraft dependency today (verified in `common/build.gradle`).
Moving the MC-touching code needs a new Gradle module whose compiled output works correctly on
**both** Fabric's and NeoForge's Minecraft jar — a real risk, since NeoForge patches vanilla with
its own ASM changes (confirmed by its `applyNeoforgePatches` build step) and Fabric's own compile
jar turned out to have Fabric-API interfaces woven in too (discovered while building the pilot,
see below) — neither side is actually "clean vanilla."

Verification method: compiled the real `FabricFocusedModeExecutor.java` unmodified (renamed only)
through the actual working `:permaclicker-fabric:compileJava` Gradle task — so it used the same
classpath resolution the real build already relies on, not a hand-assembled one — producing a
real `.class` file. Disassembled its constant pool (`javap -v`) to list every Minecraft/Mojang
method and field it actually references at the bytecode level (18 distinct members: `Minecraft`,
`Options`, `KeyMapping`, `MultiPlayerGameMode`, `BlockPos`, `Direction`, `BlockHitResult`,
`ClientLevel`→`Level`, `BlockState`→`BlockBehaviour.BlockStateBase`). Checked every one of those
18 against the NeoForge-side patched jar (`javap -p`, following the inheritance chain for two
that are declared on a superclass) — **all 18 resolve with identical signatures.**

Class-level `javap -p` diffing of the 6 classes with any difference at all (`Minecraft`,
`KeyMapping`, `KeyMapping.Category`, `MultiPlayerGameMode`, `Screen`, `LocalPlayer`) showed every
difference is either NeoForge adding new members (constructors, methods, an extra implemented
interface) or widening visibility (`private`→package-private, `private`→`public`) — with exactly
one narrowing (`Minecraft.missTime`: `public`→`protected`), confirmed unused anywhere in this
codebase (`grep`). MC 26.x shipping "unobfuscated" (per `isUnobfuscatedFabricTarget` in the root
`build.gradle`) is presumably why the surface stays this close between loaders.

**Practical rule going forward:** compile the new shared module against the Fabric-side jar (the
narrower of the two, closer to vanilla) as the reference classpath, not NeoForge's — NeoForge's
patched classes are a superset for everything checked, so code that only needs Fabric's surface
links fine on both; the reverse isn't guaranteed (NeoForge-only additions like `Screen.getMinecraft()`
or the new `KeyMapping` constructors would fail to link under Fabric if used).

**`BackgroundModeExecutor`'s extra members** (`PauseScreen`, `Minecraft.gui` field,
`Minecraft.setScreenAndShow`, `Gui.screen()`) checked the same way — all identical between the
Fabric and NeoForge jars. It and `FocusedModeExecutor` both now compile and are bundled into real
loader jars on both sides (see "Done" above). The remaining, not-yet-moved entrypoint helper
methods use an overlapping API surface (`Screen`, `LocalPlayer`, `Window`, `InputConstants`,
reflection-heavy `Options`/`KeyMapping` access) — `Window`/`InputConstants` were already confirmed
identical earlier in this session (see CLAUDE.md's MC 26.3 GLFW→SDL notes), reflection-based
access doesn't care about compile-time linking at all, and `Screen`/`LocalPlayer` were already
checked above (additive-only differences). No reason to expect a different result whenever that
remaining slice gets moved.

The Gradle plumbing (module setup, classpath shape, bundling into both loader jars) is done — see
above. What's left is only the remaining entrypoint helper methods listed above, which is more
source-level surgery (extracting them out of two ~900-1000 line classes cleanly) than a build
question at this point.

## F2 — Background mode blocks opening PermaClicker's own settings screen

- [x] Decide and document intended behavior; fix or document as intentional

**Resolved 2026-09-16:** kept as intentional behavior (no code change). Documented explicitly in
`README.md`, `wiki/user-guide.md`, and `CLAUDE.md`'s runtime-behavior baseline: while Background
mode is active, PermaClicker's own config screen (`U`) stays blocked even if the window is
currently focused — disable PermaClicker (`F7`) first to change settings.

`shouldBlockSettingsOpenForBackgroundMode` returns `true` whenever background mode is running,
**regardless of current window focus**. `*PauseScreenSetScreenMixin` then cancels opening
PermaClicker's own config screen in that case.

## F3 — Duplicate predicate bodies within the same class

- [x] Deduplicate `isPauseBlockContextActive` and `shouldBlockSettingsOpenForBackgroundMode`

**Resolved 2026-09-16:** both predicates take `Minecraft minecraft` as a parameter (for the
`player`/`level`/`gameMode` null-guards), so they needed the new `:permaclicker-client` module
from F1 rather than `common`. Added `PermaClickGameContext.isBackgroundModeActive(Minecraft,
PermaClickConfig)` there with the one shared implementation; both entrypoint methods (kept as two
names for call-site clarity, since mixins call them by name) now just delegate to it — down from 4
identical copies to 1. Verified with a full test + real jar build.

## F4 — `runWhenUnfocused` / `runWhenMinimized` are always set equal

- [x] Simplify to a single `runInBackground` flag or restore independent configurability

**Resolved 2026-09-16:** simplified. `PermaClickConfig` now has a single `runInBackground`
boolean instead of two record fields that could never actually differ. Updated everywhere that
computed `runWhenUnfocused() || runWhenMinimized()`: `PermaClickService`, both loader
entrypoints' predicate methods, `PermaClickConfigStore`, and both config-screen templates. All
existing tests adjusted; behavior is unchanged (every call site only ever consumed the OR of the
two flags, confirmed by test coverage before the change).

## F5 — Dead code: `BackgroundWorkerState` enum never leaves `IDLE`

- [x] Remove or finish the `BackgroundWorkerState` state machine

**Resolved 2026-09-16:** removed. The enum, its field, `transitionWorkerState`/
`resetBackgroundWorkerState`, and all call sites are gone from both loader entrypoints — it never
left `IDLE` in practice, so removal has no behavior change.

## F6 — `require = 0` silently disables a hard-failure guard

- [x] Reconsider `require = 0` on `*ChatMiningAttackScreenBypassMixin`

**Resolved 2026-09-16 — no action needed.** Reviewed with the maintainer: every release is
pinned to one specific, tested Minecraft version via the matrix/SSOT files (see "Single source of
truth" in `CLAUDE.md`), so a future MC update silently breaking this mixin at runtime for a
*shipped* build isn't a realistic scenario — a version bump always goes through
`verifyMatrixTargets` + a real compile/test pass first (this session's own MC 26.3 port is a live
example: the break would have surfaced immediately during that work, not silently in production).
Left as `require = 0` with no further changes.

## F7 — Auto-stop timer semantics are non-obvious and undocumented

- [x] Document the auto-stop timer semantics in the runtime-behavior baseline

**Resolved 2026-09-16:** added to `CLAUDE.md`'s runtime-behavior baseline and
`wiki/user-guide.md`'s auto-stop timer bullet.

`remainingTicks` only decrements on a successful mining tick, not once per real (wall-clock) tick.
If PermaClicker is "active" but mining is currently suppressed (e.g. Focused mode + window
unfocused, or player not ready), the on-screen countdown timer freezes rather than counting down
wall-clock time. This is intentional and covered by tests, but not documented in `CLAUDE.md`'s
"Runtime behavior baseline" section and could surprise a user who alt-tabs away in Focused mode
expecting the timer to keep running.

**Suggested action:** add a note to `CLAUDE.md` and `wiki/user-guide.md` clarifying that the
auto-stop timer counts active mining ticks, not elapsed time since enabling.

## F8 — No thread-safety documentation on `PermaClickService` state

- [x] Add a client-thread-only contract note (or thread-safety guards) to `PermaClickService`

**Resolved 2026-09-16:** added a class-level Javadoc note stating the client-thread-only contract
and pointing at the existing `pendingUpdateStatus` volatile hand-off pattern as the template to
follow for any future async code path.

All fields on `PermaClickService` (`enabled`, `remainingTicks`, `movementLockActive`,
`backgroundCursorFreeActive`, `overlayRefreshTicks`, `lastActionBarMessage`) are plain,
non-`volatile`, non-synchronized instance fields. No actual race was found — all current access
happens on the client tick/render thread — but there is no documentation preventing a future
contributor from introducing a real race by adding an async code path (e.g. a network callback)
that touches this state directly instead of going through the already-`volatile`
`pendingUpdateStatus` pattern used for update checks.

**Suggested action:** add a short class-level comment stating the client-thread-only contract, or
enforce it (assertion / thread check in debug builds).

## F9 — Confusingly named duplicate Fabric entrypoint

- [x] Verify `fabric.mod.json` registers only one entrypoint, then rename or remove the alias

**Resolved 2026-09-16:** verified `fabric.mod.json` / `fabric.mod.unobf.json` register only the
alias (no double-init risk — `PermaClickFabricEntrypoint` itself was never directly registered).
Renamed the alias class from `PermaClickerFabricEntrypoint` to `PermaClickerFabricBootstrap` to
make it visually distinct from `PermaClickFabricEntrypoint`, and updated both mod-metadata files
accordingly.

Side note found during this check: the checked-in `fabric.mod.json` in `src/main/resources`
appears to be dead weight — `fabric/build.gradle`'s `processResources` task excludes it and
generates the real one from `fabric.mod.unobf.json` at build time. Kept both files in sync for
now since removing the static one wasn't asked for, but it's worth confirming it's not needed for
some IDE/tooling reason before deleting it later.
