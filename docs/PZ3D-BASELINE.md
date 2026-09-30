# Preserved PZ3D VR baseline

This is the current implementation map for the `pz3d-vr-baseline` tag, prepared on 2026-09-30. Start here and in [the mod guide](../experiments/zombiebuddy-harness/README.md), not in the original feasibility report. Paths below are repository-relative. This document describes existing code; it is not a portability design.

The source/package version is **0.11.0**. The preceding published prerelease is **v0.10.1**. The baseline includes the previously uncommitted hand-proximity interaction prototype and its tests. The preservation tag does not certify every prototype feature as headset-tested. See [validation](../experiments/zombiebuddy-harness/VALIDATION.md) for the distinction between automated checks and user confirmation.

## Supported environment and entry point

Windows x64, Java 25, Zomboid 42.21.0, PZ3D 0.3.0, and ZombieBuddy 2.3.2 are the current targets. `experiments/pz3d-adapter/src/pzvr/VersionGate.java` checks SHA-256 of the actual loaded game/PZ3D/loader JARs. It accepts the original pinned ZombieBuddy JAR and one pinned B42.21 temporary-fix JAR; it rejects unknown variants. These checks remain intentional and unchanged.

`experiments/zombiebuddy-harness/mod/42.20.4/mod.info` declares the Java JAR and `javaPkgName=pzvr.harness`, requires ZombieBuddy and PZ3D, and pins game version 42.21.0. The historical `42.20.4` folder remains for installation continuity; its name is not the compatibility policy.

ZombieBuddy loads `pzvr.harness.Main.main(String[])`. Its `@LuaClass(name="PZVRStereo")` exposes settings/control methods to Lua. Main gets `me.zed_0xff.zombie_buddy.Loader.g_instrumentation`, checks code-source hashes, and installs the rendering, melee, controller, turning and hand-use transformers. It does not rewrite the signed game or dependency JARs. The renderer installation compares normalized executable bytecode against the pinned original and checks classloader ownership/modifiability. Feature installations validate their target shapes and attempt rollback on failure. Main rejects multiplayer startup.

## Source map and coupling

| Source area | Responsibility | Coupling |
|---|---|---|
| `experiments/pz3d-adapter/src/pzvr/instrument/RendererTransform.java` | Schema-preserving bytecode edits for stereo boundaries, projection, attachment upload and selected effects | Strongly PZ3D-specific; exact internal names/descriptors and call counts |
| Adapter `PairHooks`, `AttachmentPoses`, `GlEyeTargets`, `VersionGate` | Pair scope/lease, borrowed render-state overrides, GL destinations, binary authorization | PairHooks/VersionGate are strongly PZ3D-specific; GL utilities have less renderer coupling |
| Harness `src/pzvr/harness/Main`, `Installation`, `CaptureHarness` | Loader entry, instrumentation, render interception and diagnostic capture | ZombieBuddy, Zomboid and PZ3D |
| `harness/XrHarness`, `LiveMirror`, `EyeCapture`, `VanillaUi`, `TrackedArms` | Game render-thread glue, desktop mirror, UI texture copy, model palette/attachment replacement | Strongly PZ3D-specific snapshots and camera/GL lifecycle |
| `src/pzvr/xr/OpenXrSession`, `XrHands`, `XrControllerInput`, `FrameTiming` | XR lifecycle, swapchains, action sampling and timing | Mostly XR/VR concerns, but current session code uses the game's current graphics context and application types; not an extracted independent library |
| `xr/XrCamera` | Eye and grip-space conversion, recenter anchor and shoulder-height offset | Explicitly PZ3D coordinates and `PairHooks.EyeFactory` |
| `xr/ArmIk`, `ArmRig`, `HandPoses` | Two-bone solve, rig/calibration data and immutable poses | IK math is relatively general; rig conventions and palette use are tailored to Zomboid models |
| `src/pzvr/input/*` | OpenXR-to-native-gamepad bridge, normalized state and mapping | Mapper/state largely generic; bridge/instrumentation uses Zomboid's LWJGLX registry and input lifecycle |
| `src/pzvr/turn/*` | Snap/smooth filter, source selection, ready/aim ownership | Filter mostly general; runtime changes PZ3D LookState and hooks native Joypad queries |
| `src/pzvr/melee/*`, `src/pzvr/contact/*` | Gesture detection, native attack ownership and contact-timed bat pilot | Math/detectors less coupled; dispatch is Zomboid combat plus PZ3D NativeAvatar/locomotion; capture uses PZ3D snapshots |
| `src/pzvr/interaction/*` | Fresh grip presses, hand proximity targeting and native short-use dispatch | PressGate general; HandUse/UseInstallation tightly tied to PZ3D world geometry, routing and private methods |
| `mod/42.20.4/media/lua/client/*` | Native Mods settings, settings guards, status text and disconnect cleanup | Zomboid Lua/ModOptions and ZombieBuddy's exposed PZVRStereo API |

The `experiments/` directory contains production prototype sources as well as diagnostics. Do not treat that name as permission to delete it. Original synthetic fixtures deliberately use game/mod package names; they are separate from proprietary decompilations and never go in the mod JAR.

## Frame lifecycle and stereo

The existing producer remains Zomboid/PZ3D: `Core.RenderOffScreenBuffer` -> PZ3D render patch -> `Main.enqueue` -> a retained `Renderer.Frame` on the existing render thread. We intercept the consumer, not the game simulation or native UI event loop. Retained redraws also pass through the intercepted `Frame.draw(boolean)`.

1. `CaptureHarness.intercept(frame,fresh)` polls remappable GLFW shortcuts on the render thread. It checks installation readiness and single-player, first-person, on-foot eligibility. It selects XR, live desktop stereo, one-shot capture, or the untouched ordinary renderer. It avoids recursively intercepting an active PairHooks scope.
2. `XrHarness.draw` calls `OpenXrSession.frame`, which manages the XR wait/begin/end lifecycle and supplies one predicted head/two-eye sample to a renderer callback. No fresh game simulation is requested for the second eye.
3. `PairHooks.render` retains the frame for the entire pair. `RendererTransform` leaves scene adoption, prediction (`Renderer.Frame.ad`), resource preparation, tree preparation and common shadows before the pair boundary, after `ActorShadows.render`.
4. At that boundary, the bridge captures base camera/scene identity and obtains both eye matrices. XrHarness publishes hand-use poses and applies scoped arm palette and item attachment overrides to the prepared render snapshot.
5. Each eye sets its scene-relative camera position and replaces the combined `ViewMath.projection` result with the eye's full view-projection matrix. Per-eye frustum/visibility and `WorldBuffers.beginView` still execute. PZ3D's scratch color/depth target is cleared for each eye.
6. The original desktop blit becomes a synchronous eye copy. EyeCapture keeps desktop mirror images; the XR copy sink writes into acquired runtime swapchain images. The second eye cannot overwrite the first before its copy completes. This is still copy-based rendering, not direct rendering into XR swapchains.
7. Palette/attachment overrides, camera fields, GL state and the frame lease are restored through scoped cleanup. The native draw tail executes once. `VanillaUi.copy` copies the completed native UI snapshot before XR submission; EyeCapture presents the side-by-side desktop mirror.

PairHooks validates thread/scope, generation and prepared-state fingerprints. These diagnostics do not make all borrowed engine resources immutable. `StreamFade.aB` reads one pair timestamp; TreeRenderer uses a consistent TreeEnvironment state; corpse rendered flags are deferred to the second eye. Adaptive depth prepass and GPU telemetry, sky, crosshair, tracers, captions, outlines/highlights, contact-shadow overlays and chunk debug are omitted in active stereo pairs. Ordinary rendering retains its original calls. These omissions are correctness compromises, not a completed visual parity solution.

Failure paths close owned XR/GL resources and resume ordinary/retained-style rendering where possible. `XrHarness.watchdog` is driven by Lua and queues cleanup on the render context if PZ3D stops producing frames. This is not a guarantee that every third-party failure is recoverable without restarting.

## OpenXR, camera and arms

`OpenXrSession` owns instance/session/spaces, eye and UI swapchains, action sets and submission. It uses the existing Windows OpenGL context with `XR_KHR_opengl_enable`. Acquire/wait/copy/release and frame submission stay on that context thread. `FrameTiming` reports CPU preparation, eye drawing, copies, mirror and runtime stages; it is not a GPU profiler. A null headset is useful for lifecycle/output checks, not physical tracking or comfort validation.

`XrCamera.eyes` maps OpenXR right-handed Y-up/-Z-forward poses to PZ3D's reflected-Y, Z-up view convention. It anchors a recentered head transform to the prepared base camera, uses each runtime eye pose and asymmetric FOV, and assumes one scene unit per metre. It exposes `sceneFromLocal` for hands and `shoulderShift` from physical head-height change. It does not turn physical head motion into native character locomotion/crouching. A user recenter has a five-second countdown. Runtime reference-space changes reanchor the camera without relearning hand orientation from an arbitrary controller pose.

`XrHands` supplies grip poses only with valid and tracked position/orientation. `TrackedArms.apply` targets local body/clothing palettes, uses `ArmRig`/`ArmIk`, and temporarily replaces render snapshot buffers. It does not modify shared AnimationPlayer pose data. Upper-arm visibility masks and shoulder offsets are overridden within the same pair. Maximum reach defaults to 150 percent (100-175 configurable); it stretches arm segments without scaling hands/items. Controller-to-palm orientation calibration survives ordinary tracking interruptions.

Static equipped-item attachments follow their native owning hand through `AttachmentPoses`, at Renderer attachment upload. They are not separately grabbed world objects, and two-handed support constraints/full-body IK are not implemented. Synthetic waving previews are explicitly barred from melee and hand use.

## Input, turning, combat and hand use

`XrControllerInput` publishes immutable `VrControllerState` independently of virtual-gamepad mode. Full button/stick suggested bindings currently target Oculus Touch; grip-pose/combat bindings in XrHands have a different, broader profile set. Do not assume full control parity across headsets because pose tracking works.

`ControllerBridge` overlays native controller slot 15 with `PZ VR Gamepad`. The player assigns it through native settings. `GamepadMapper` maps sticks, triggers, face buttons and grip bumpers, with a Menu alternate layer for D-pad/Back. Neutral/release gates prevent held inputs on activation/reconnect. If a physical pad claims the slot, the bridge drains its synthetic state, runs native disconnect and cleans up only its own assignment. Turning this feature Off retains a neutral synthetic identity until restart.

`TurnInstallation` calls `TurnRuntime.tick` from PZ3D `Main.tick`, extends `NativeAvatar.attackAim`, and scopes consumption of `JoypadManager$Joypad` axes/buttons. `TurnFilter` provides snap or time-scaled smooth turns. Runtime reads either raw VR input or the assigned physical pad, then updates synchronized `LookState.get/reset` yaw, preserving pitch. The default ready/aim binding is left trigger. Menus, missing focus and unsupported contexts restore ordinary input; turning inhibits motion melee until 200 ms after the turn and requires attack release to rearm.

`MeleeInput` transfers fresh gesture/permit state from rendering to the game thread. `SwingDetector` classifies armed motion. `MeleeRuntime` runs from `NativeAvatar.beforeAnimation`, scopes native aim/attack ownership and requests native attacks. Animation-timed melee supports eligible swinging weapon categories and may hit scenery through native combat; it is not restricted to the baseball bat. Unarmed, knives/spears/chainsaws/firearms are outside the prototype.

`ContactCapture` derives a capsule from the actual rendered plain Base.BaseballBat attachment, adds the scene origin, and publishes contact samples. `Sweep`/`ContactRuntime` test an approximate standing-zombie capsule, enforce freshness/reach/visibility, then use native combat when its attack state becomes ready. Owned hit-list and animation-event hooks deduplicate resolution; scenery is suppressed for this mode. The 150 ms resolution limit, one target per trigger hold and native recovery remain. Misses do not initiate native missed-swing cost. This is not general physical collision or per-limb damage.

`HandUse.capture` publishes immutable head/grip positions from `sceneFromLocal` plus `frame.scene.ox/oy/oz`. `HandUse.tick` consumes a fresh `PressGate` grip edge on the game thread, filters world/item bounds within 0.25 scene units, verifies native picking/visibility, and invokes PZ3D's existing private `Main.b(false)` with a thread-scoped `Interaction.Choice`. During selection only, camera-derived `Traversal.aimed` is suppressed; a selected intact window gets its matching native edge. No target means no fallback use. Native door/window/pickup/container/context handlers remain responsible for actual actions.

Hand use is Off/Diagnostics/Live with left/right/either grip selection. It does not carry, reposition or throw world items. Holding grip does not repeat or climb. Tracking loss, menus, pause, recenter, timed actions, traversal and attack/context guards disarm use. Scope cleanup preserves normal keyboard E after a request or exception.

### Ownership decisions to preserve

| Feature | Input ownership |
|---|---|
| Gamepad bridge + motion melee | Hybrid is opt-in. When selected, native virtual-pad RT stays released, including menus/firearms; motion melee Off restores it after neutral input. |
| Turning + ready/aim | Owns the selected controller's right stick and chosen aim binding only in eligible gameplay; unselected physical pads and menus keep their native behavior. |
| Hand interaction | Selected grips are reserved globally while enabled, including diagnostics/menus; their virtual bumpers and raw-VR ready/aim use are suppressed. Physical pads are unchanged. Set interaction Off to restore grips. |
| Hand-use failure | Feature disables, but chosen grips remain reserved until interaction is turned Off; a failure must not turn a held grip into a native bumper action. |

## Hook inventory

All class names below are exact current binary names, not stable APIs. Runtime classes under `com.pavelvoronin.pz3d` belong to PZ3D, not this repository.

| Installer | Target classes and key boundaries |
|---|---|
| `harness/Installation` + adapter `RendererTransform` | PZ3D `Renderer$Frame` draw/preparation/projection/copy/interception; `StreamFade` clock; `TreeRenderer` environment reads; `Renderer` attachment upload |
| `melee/MeleeInstallation` | PZ3D `NativeAvatar.beforeAnimation/attackAim` and queue fields `kw/kx`; game `IsoPlayer.DoAttack`; `CombatManager.calculateHitInfoList/calculateAttackVars/processTreeHit`; `SwipeStatePlayer` enter/exit and collision/shove/stomp/grapple animation events |
| `input/ControllerInstallation` | Game `org.lwjglx.input.Controller` constructor, `Controllers.getController/poll`, `zombie.core.input.Input.updateGameThread` |
| `turn/TurnInstallation` | PZ3D `Main.tick`, shared `NativeAvatar.attackAim`, game `JoypadManager$Joypad` aiming axes/LT/bumper/stick/generic button queries |
| `interaction/UseInstallation` | Shared PZ3D `Main.tick`; `Interaction.choose(IsoPlayer,Controller,WorldMirror)`; `Traversal.aimed(Controller,WorldMirror)` |

There are fifteen unique transformed classes. The shared Main and NativeAvatar targets require transformer composition tests. Additional reflection dependencies include Renderer frame/character/part/pose fields, skinning metadata, `UiLayer.texture/width/height`, `LookState`, `Main.jq/jr/b`, `WorldMirror.Scene`, `WorldHit`, `WorldItems.Pick`, `Interaction$Choice`, `Traversal$Edge`, `ControlRouting.Access`, NativeLocomotion/NativeInteraction and vehicle ownership. Exact descriptors and required matches live in installer source; copied-binary tests inspect further reflection contracts without initialization.

## UI, configuration and dependencies

`VanillaUi.copy` borrows PZ3D's completed `UiLayer` texture. OpenXrSession copies it to a transparent, head-following quad. Native UI logic runs normally. There is no controller-ray pointer or desktop cursor copy in this baseline. Native mouse/gamepad menu input remains the interaction mechanism, subject to explicit input reservations above.

`PZVROptions.lua` uses built-in `PZAPI.ModOptions` and syncs Apply/load/main-menu/game-start to Main's exposed setters. `PZVRStereoCapture.lua` drives watchdog/status drawing. Options persist in native `ModOptions.ini`. GLFW hotkeys live in `Hotkeys`, avoiding reliance on Lua key events that PZ3D can swallow. Defaults and settings values are in the mod guide; UI/text-entry guards inhibit hotkeys and relevant gameplay input.

Build dependencies are pinned in `experiments/openxr-diagnostic/dependencies.json`: Temurin JDK 25.0.4.1 and LWJGL 3.4.1 artifacts. The mod reuses the game's LWJGL core/OpenGL and JOML and ZombieBuddy's shaded ASM (`net.bytebuddy.jar.asm`). `Bundle-XR.py` checks hashes and embeds only LWJGL OpenXR bindings and the Windows OpenXR loader, with licenses. Do not replace the game's LWJGL core. Python 3/PowerShell are build/helper dependencies; Lupa is an optional local dependency for the Lua fixture test. A working OpenXR runtime is supplied separately.

`SimulatedRuntime.ps1`, `RuntimeKeepalive.py` and `HeadsetWindow.py` support a workspace-local SteamVR null-headset profile and a movable bordered window. Keep their configuration/log paths local. Do not run an additional XR scene while the game owns a session. These helpers are not required for a tester with a real headset/runtime.

## Build, validate, package and deploy

Run from the repository root. Obtain the pinned tools with `experiments/openxr-diagnostic/Setup.ps1` and copy matching proprietary JARs to the ignored reference destinations listed in the root README. A public clone intentionally does not contain those binaries. Decompilations are reading aids, not buildable inputs.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File experiments/zombiebuddy-harness/Test.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File experiments/zombiebuddy-harness/Test-ZombieBuddy.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File experiments/pz3d-adapter/Test.ps1
```

`Test.ps1` first runs the normal Build.ps1: compile adapter/tools, verify pinned reference hashes and offline transformations, compile harness, package project classes, bundle verified XR dependencies, copy Lua/descriptor/docs/helpers/licenses, and create `dist/PZ3DVRTest-0.11.0.zip`. `Build.ps1` alone packages without running the suite. These incremental builders do not clean their output directories; for a clean-source verification, preserve existing compiler/package directories outside their original paths first. Do not accidentally package stale classes from a removed source.

Tests execute original synthetic fixtures and standalone GL, or define/retransform actual copied classes without initialization. They do not launch Zomboid or either dependency's entry point. `Test-ZombieBuddy.ps1` additionally needs the pinned temporary-fix JAR at `reference/zombiebuddy/42.21-temporary-fix/ZombieBuddy.jar`. The separate Lua command is:

```powershell
python experiments/zombiebuddy-harness/tests/ModOptionsTest.py '<your-game-directory>/media/lua/client/PZAPI/ModOptions.lua'
```

That script reads vanilla settings code with in-memory game/I/O mocks. Native XR smoke tests are separate and not part of baseline preservation. Deliberate negative fixtures emit failure/exception logs; check test exit status and final assertions, not the mere presence of the word "failed".

Audit the ZIP and inner JAR for source/version consistency, personal paths, secrets and proprietary classes. Only project classes and permitted bundled XR dependencies belong in the JAR. Keep ZIPs, hashes, test logs, transformed references and downloaded tools ignored; put distributable ZIPs on release assets when a release is requested, not in Git history.

The user installs with the game closed: replace the local `PZ3DVRTest` mod folder, preserve its `common` and `42.20.4` layout, enable dependencies/test mod for the save, restart and approve a changed JAR if prompted. Never install by replacing game/PZ3D/Workshop JARs. Baseline preservation pushes the source commit and annotated tag; it does not create a new numbered GitHub release.

## Evidence, limitations and maintenance debt

User reports confirmed live desktop stereo, simulated-headset output, vanilla UI and earlier tracked arms/held items. Later changes have strong fixture/bytecode evidence but not comprehensive recorded headset confirmation; specifically 0.11.0 hand use remains unconfirmed. A baseline tag records a recoverable implementation, not a new gameplay test result.

- First person, on foot, single player, Windows/OpenGL only. No supported multiplayer or vehicle VR.
- No full-body IK, physical crouch-state mapping, room-scale collision/locomotion, teleport, two-hand weapon constraint, natural object carry/placement or throwing.
- UI is a panel; controller pointer/cursor and natural inventory are unfinished.
- Contact melee is one plain-bat/standing-zombie pilot. Other armed motion attacks retain animation timing; unarmed/firearm motion combat is absent.
- Stereo skips selected visual passes, still renders a desktop mirror and copies eye textures. Desktop suppression/direct swapchain rendering and performance optimization are unfinished. Runtime/headset timing cannot be inferred from game FPS alone.
- Scale, reach limits, tracking latency, control ergonomics and hardware/profile compatibility need physical tests.
- Exact hashes, obfuscated/internal methods and reflective snapshot layouts are deliberate PZ3D maintenance liabilities. Preserve the pins until compatibility is actually verified; changing version labels is not a compatibility fix.
- Shared borrowed model/scene resources, asynchronous uploads, native patch ordering and version-specific combat callbacks require continued scrutiny. Existing checks cover known contracts, not all engine behavior.
- Source lives across the adapter and harness trees; build scripts assume local pinned tools/reference JARs and use incremental output. No general renderer-independent core or portability abstraction has been extracted.

## Preservation and future-session rules

Read this map, root README, AGENTS.md and validation before changing the baseline. The original `research/` reports and earlier milestone documents remain as historical evidence, not current TODO lists. `reference/`, `external/`, diagnostic runs, local editor files and `.baseline-artifacts/` remain local and ignored. Preserve useful research/logs rather than deleting them because they look experimental. `scripts/audit_personal_paths.py` checks tracked text/history; also review new files and the staged diff for secrets/artifacts before committing.

This task creates `pz3d-vr-baseline` on the final source/documentation commit and pushes it with main without rewriting history. No Project Viewpoint code, branch, dependency installation, reverse engineering or preparatory refactor belongs to this task. Item grabbing/placement was discussed only and is not implemented.
