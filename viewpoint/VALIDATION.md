# Viewpoint baseline validation

## Turning options restored: 0.7.1

Restores the preserved mod's Off/Snap/Smooth selection, 15/30/45/60/90-degree snap choices, and 30?240-degree/second smooth speed choices in 15-degree steps. These are three dropdowns in the existing on-screen controls. Defaults retain 30-degree Snap, with 90 degrees/second selected for Smooth. Settings remain session-only. Turning Off preserves walking and pointer use.

Reused the project's authored TurnFilter, adapting ownership to Viewpoint's input tick. Smooth rotation is proportional to stick deflection and elapsed time, caps each update at 50 ms and disarms after a gap over 250 ms. Vertical-dominant input is rejected. Settings changes, tracking/focus loss and mode/menu transitions require neutral rearming.

Full Build.ps1 passed in run `c6d2c53939cf44b19d5a55505c58831e`, including 58 new turning checks and all prior controller, camera, lifecycle, copied-class initialization audits, Lua compilation and standalone GPU checks. Turning coverage includes both directions at every snap angle, smooth rates at 30/60/120 Hz, proportional input, deadzone, stall handling, tracking recovery, settings validation and held-stick setting changes. No game/mod entry point or SteamVR was launched. Physical-controller validation remains pending.

Package: `dist/ProjectViewpointVR-0.7.1.zip`, 82 production classes. ZIP SHA-256: `c7593f066cdabe0808bcc6186dff62cc8bd5bb6cbb0b7730ec3e09e00fea0f5e`.

## Controller input: 0.7.0

The user confirmed the 0.6.1 updated renderer and UI work, then authorized controller acquisition, UI pointing/clicking/scrolling and optional movement/turning. Physical-headset/controller testing remains deferred. The supported Viewpoint binary remains 0.1.5a-hotfix; its installed SHA-256 was rechecked unchanged for this work.

Full Build.ps1 passed in run `8bfcc303823a481e8882f7b0f64413c8`: four Python tests; 71 controller simulations; 256 stereo-camera, 485 lifecycle, 32 entry fixture, 21 XR camera/crop, 16 runtime, 239 transformed native-stage, 12 UI bridge, 38 capture/control, 17 copied-entry and 15 activation/rollback checks. Eleven copied targets verified/retransformed without game/mod initialization. Lua compiled with the bundled Kahlua compiler without execution. Hidden OpenGL passed 59 UI composition, 32 capture/output and 15 new controller-ray checks. The GPU ray test inspects actual beam pixels for both eye offsets and caller-state restoration. The real OpenXR loader rejected a deliberately missing process-local manifest with expected error -51; SteamVR was not launched.

Controller simulation covers geometry, neutral rearming, click hysteresis, tracking/focus/stale-sample loss, scroll repetition, walk deadzones and diagonal limits, turn/menu edges, physical/virtual mouse history and native callback ordering/failure. Exact-pinned Hooks and ImGuiFrame adapters now include three input call sites while retaining the existing capture hooks. Native originals run once; movement modifies only an otherwise zero input vector. Controls default Off and send no OS input.

Production classes were packaged as `dist/ProjectViewpointVR-0.7.0.zip` after the passing build; only the version descriptor and documentation changed afterward. The packaged JAR independently passed the missing-runtime smoke test using its bundled OpenXR bindings/loader. Archive inspection found no proprietary or project test classes; project source/documentation checks found no literal personal machine paths. ZIP SHA-256: `4dc7cb76dc07e3f4acfeb9e902c9c884ef2d9db6b60f2fed44ae273c946790a9`. This is a manual-test package, not a hardware-validated release. Live controller profile selection, ray orientation, vanilla/ImGui interaction, walking/turning direction and runtime/focus recovery remain unverified. No hand models, weapons or direct world interaction are claimed. See [INPUT.md](INPUT.md).

## Current Viewpoint port: 0.6.1

The user requires following the latest available Viewpoint and explicitly rejects maintaining old-version compatibility. This supersedes the previous UI-stage deferral. The sole supported Viewpoint target is now **0.1.5a-hotfix**, SHA-256 `94fedda302ab6c17ba1b38495789e4c9781d52823fb8204214c85402e3cab41f`; 0.1.3 is no longer accepted. Game and ZombieBuddy pins are unchanged. Java transform pins now share BinaryPins.VIEWPOINT to avoid divergent hard-coded values.

Read-only Steam metadata showed the installed and latest manifest both equal `9102883971095548858`, with update timestamp 2026-10-01 08:41:20 UTC. The public [Workshop change notes](https://steamcommunity.com/sharedfiles/filedetails/changelog/3809306528) list the October 1 crash-to-isometric hotfix as the latest entry. The installed descriptor identifies 0.1.5a-hotfix. No Workshop content or Steam configuration was modified.

The port adapts PackLinks.follow, post-processing moved into WorldRenderer.passes, the additional FarPass shadow argument and the new MousePick signature. It calls the current native late mouse read and Camera.eye once per pair, preserving lean; it records native latency once and restores the new freeCamera flag. The native postRender corpse release remains untouched. A fifth visibility target, CorpseView, keeps the new corpse cone from excluding head-turned views while preserving native distance/count budgets. Settings and onboarding share the captured ImGui path.

Iris/Minecraft shader packs remain outside stereo support: a selected Iris pack delegates normal Viewpoint drawing before pair ownership. IrisMode.draw is suppressed inside an owned pair to prevent its separate rendering path from running during preparation or an eye pass. A selection change during a pair rejects that incomplete pair and cleans up. Tests cover inactive native Iris calls, pre-pair rejection and selection changes after the left eye. Switch Off before changing shader packs; native Viewpoint shader packs remain the supported stereo path.

Full build evidence: `build/runs/f56d976bdf994036a389b1d652e47ac1/`. Passed **239 native-stage fixture checks**, **15 copied-target installation/rollback checks**, **59 UI GPU checks**, **32 output GPU checks**, and the existing 12 UI bridge, 256 camera, 485 lifecycle, 32 entry, 21 XR camera, 16 runtime, 38 capture, 17 copied-entry and 4 Python checks. The original-boundary checker now validates 6 methods / 23 invocation anchors; stage transforms validate 30 anchors. All eleven copied target classes passed verification/retransformation and no-initialization audits. New fixtures cover native lean/late yaw, once-only camera/latency work, free-camera-state restoration and Iris selection. Copied corpse-cone returns all pass through the timed VR visibility wrapper; already-transformed input is rejected.

The 0.6.1 ZIP contains 67 verified production classes plus pinned OpenXR dependencies, with no proprietary/test classes or personal absolute paths. The packaged loader passed the isolated missing-runtime test. ZIP SHA-256: `08239aa9cefda39367cae4f661b243e98417eb4ccbe15f843741c1b7b6878885`. No game/mod entry point or SteamVR was launched. The updated renderer/UI combination still requires the user's desktop/null-headset test; earlier version confirmations do not validate this port. Physical-headset testing remains deferred.

## UI milestone 0.6.0 and confirmed earlier tests

The user confirmed tests 1 and 2 (continuous desktop stereo and null-headset XR) without issues, and explicitly deferred physical-headset testing because a playtester is no longer available. This is user-reported live validation of the earlier 0.5.0 modes, not physical tracking/comfort verification or evidence for the new UI implementation.

Version 0.6.0 adds transparent capture of Zomboid UI, Viewpoint loot/performance overlays and ImGui settings, preserving native desktop input and adding a released-cursor marker. The combined result is submitted on the next XR frame as a head-relative quad; idle frames can submit UI without a world projection. Capture targets include depth/stencil storage for native scrolling-panel clipping. See [UI.md](UI.md) for exact boundaries and limitations.

The complete build passed in `build/runs/4b961a4e5c264202badfec5c0adccb04/`: **59 UI GPU checks**, **12 UI bridge checks**, **13 copied-target installation/rollback checks**, and the existing 32 GPU output, 38 capture, 21 XR camera, 16 runtime, 256 camera, 485 lifecycle, 32 entry, 219 stage-fixture, 17 copied-entry and 4 Python checks. All ten target classes passed transformation/retransformation and initialization audits without running dependency code. Lua compiled with the bundled Kahlua compiler without executing the script. Failure messages from deliberately injected UI/capture failures and missing runtime result -51 are expected test outcomes.

The ZIP was inspected against compiler output and source assets: 67 project classes, the pinned OpenXR bindings and Windows loader, no proprietary/test classes. The extracted production JAR passed a separate missing-runtime smoke test using its bundled loader. ZIP SHA-256: `bf510002186a8ae554b465bbd9a037ab6bfc99bf6cc55d7782e30e50b2761d22`. Changed source/docs and packaged project classes were checked for personal absolute paths. No game/mod entry point or SteamVR was launched; installed files and Zomboid data were not modified.

**Compatibility intentionally unchanged:** Steam had updated the installed Viewpoint to **0.1.5a-hotfix**, SHA-256 `94fedda302ab6c17ba1b38495789e4c9781d52823fb8204214c85402e3cab41f`. Archive comparison found 326 changed/added entries, including renderer and UI integration classes. The new binary was correctly rejected. The user explicitly chose to finish UI for **0.1.3** and handle the update separately. Final tests used the preserved, hash-matched 0.1.3 copy; the 0.6.0 package will not load with the current Workshop update. No compatibility claim follows from inspecting the newer classes.

**Pending:** first in-game/null-headset UI test, desktop appearance/input parity, UI gamma/readability and physical-headset comfort. Automated capture/composition and metadata tests establish implementation behavior in isolation, not successful live UI presentation.

## Save-specific controls issue resolved

The user reported missing 0.5.0 buttons and apparently inactive Pause/Break controls. Read-only inspection confirmed that 0.5.0 initialized at the main menu, but the save loaded a separate five-mod list without `ProjectViewpointVR`. Its Lua controls were therefore absent in that save. The user enabled the mod for the save and confirmed the issue was fixed. No code change or save-file edit was needed. This confirms resolution of the controls-availability issue; it is not additional confirmation of sustained stereo or headset behavior.

## User capture evidence and runtime milestone 0.5.0

The user's 0.4.1 test on 2026-09-30 at 22:55:47 and 22:55:49 America/New_York produced complete `capture-2026-10-01T02-55-47.025698100Z-6ccc9bb7` and `capture-2026-10-01T02-55-49.405564200Z-ec3c8b52` folders. Both metadata files report 1280x720 per eye and the pinned game/Viewpoint hashes. Both side-by-side images were visually inspected: upright bedroom/doorway geometry in both eyes, visible stereo displacement, no black eye. Console entries at native frames 536 and 831 report saved pairs; subsequent native rendering/performance logs continue without capture errors. This establishes the one-shot in-game path for those scenes, not comprehensive visibility, sustained performance or headset correctness. Captures and personal console logs are not committed.

At the user's request, 0.5.0 implements the remaining desktop/OpenXR/head-tracking steps together. New code reuses the inherited WGL OpenXR lifecycle while removing PZ3D camera coordinates, controllers, hands and UI composition. It adds persistent GPU targets/mirror, runtime mode controls, a pure Viewpoint XR camera with scale/yaw recenter, symmetric render plus asymmetric crop, and conservative producer visibility. Startup remains Off. Eight native targets now activate/roll back together; the four visibility classes retain their schemas and are pinned against canonical original bytecode.

The completed build passed 21 XR camera/crop checks, 16 runtime-mode checks, 32 standalone GPU checks, 38 capture/controller checks, and the prior camera (256), lifecycle (485), entry (32), native stage (219), copied entry (17), loader (9) and Python (4) checks. All eight copied native targets passed activation/rollback and the no-initialization audits. The package contains 57 project production classes plus the pinned OpenXR binding/loader dependencies. Its extracted production JAR also passed the missing-runtime smoke test using the bundled binding/DLL, with no separate OpenXR dependency JAR on the test classpath; archive inspection confirmed no proprietary or test classes were included.

Automated validation includes camera/crop ray equivalence, runtime transition/warmup/cleanup/failure behavior, copied-class activation/retransformation without initialization, GPU target reuse/resize and mirror eye ordering, and Lua syntax compilation using the bundled Kahlua compiler. The real OpenXR loader is tested only against a deliberately nonexistent process-local runtime manifest; expected loader error -51 is a passing missing-runtime test. No game/mod entry point or SteamVR is launched by this work. Previous physical/simulated runtime validation of the inherited PZ3D backend is historical, not evidence of live success for this adapted package.

**Live validation pending:** continuous desktop while moving/turning, headset image/gamma and physical scale, tracking latency/comfort, recenter, visibility around doors and at view edges, and runtime loss/restart with the actual headset. No VR UI, tracked hands or controller gameplay is claimed. Follow TESTING.md in order: desktop, fixed XR diagnostic, tracked XR. The package is an experimental implementation of steps 1-4, not a verified playable VR conversion.

## Capture shortcut correction 0.4.1

2026-09-30. Replaces Ctrl+Shift+F10 with **Shift+Pause/Break**. F10 is the installed game's screenshot default and the user's saved B42 screenshot binding; Viewpoint also handles it for developer isolation without checking modifiers. Installed Authentic Z Lua assigns F10 to Hotbar 14 and Scroll Lock to Hotbar 16, so Scroll Lock was also rejected.

No Pause/Break binding was found in the installed game Lua, installed Workshop/local mod Lua, saved keys/Mods options, or the audited Viewpoint Java input code. Inspection of GameKeyboard, IngameState, Core, GameWindow and UIManager bytecode found no direct Pause key-code use; the installed Keyboard/KeyCodes mapping confirms LWJGL2 Pause 197 maps to GLFW Pause 284. This is a scoped local audit, not a guarantee about all Java mods, unrelated desktop applications, hardware mappings or future configurations.

The production chord predicate requires focus and either Shift key, and rejects either Ctrl, Alt or Windows key. Twelve new input checks cover the supported chord, missing Shift, lost focus, all six extra modifiers and rejection of the old F10 chord. Capture/controller checks now total 38. Build/package verification, copied-class initialization audits and the 22 standalone GPU checks passed again. No game/mod entry point or SteamVR was launched; the physical shortcut and first in-game pair still await the user's test. Package: `dist/ProjectViewpointVR-0.4.1.zip`.

## Desktop capture diagnostic 0.4.0: packaged for first in-game test

2026-09-30, Windows x64 / JDK 25. Same pinned game, Viewpoint and audited ZombieBuddy temporary-fix binaries. The build produces `dist/ProjectViewpointVR-0.4.0.zip` with 37 production classes, the mod descriptor/Lua status overlay, manual test guide and JAR checksum. Archive contents/checksums are verified; no dependency classes, test fixtures, test agent or proprietary binaries are bundled.

- **22 standalone GPU checks** passed on an NVIDIA GeForce RTX 4090 using an invisible GLFW/OpenGL compatibility context. This is a small project test program, not Project Zomboid or SteamVR. It verifies eye colors, PNG vertical orientation and dimensions, owned-FBO deletion, viewport origin, scissor/sRGB, active texture/binding, pixel pack/unpack state, color mask, distinct non-default read/draw framebuffers and their buffer selectors. The GPU test caught and fixed FBO-before-attribute restoration ordering and explicit sRGB restoration.
- **26 capture/controller checks** passed: complete-pair publication, side-by-side eye ordering, partial allocation/readback/presentation failures, cleanup, request coalescing, key release/rearming, expiry, unready-install rejection and failure disarming. The intentionally injected render failure prints an expected stack trace.
- **9 loader activation/rollback checks** passed on actual copied, uninitialized classes: exact binary pins, unmodifiable-target rejection, all-target activation, compatible retransformation, removal, partial activation failure/rollback, reinstallation, later incompatible-code disarming and restoration of all four classes.
- Previous camera (256), pair lifecycle (485), entry fixture (now 32), native-stage fixture (219), copied-entry (17), copied-stage metadata/retransformation, Python cases (4), and original/stage anchors (21/29) passed. All three copied-class JVM logs passed the no-Viewpoint/Zomboid/ZombieBuddy-initialization audit. The existing JOML Unsafe deprecation warning remains.

**No game or SteamVR process was launched; no mod entry point was executed by the tests.** All test images/logs and native extraction stayed in local workspace/temp locations. Game installations, Workshop content, Zomboid user data and runtime settings were not changed. Installation and the first live capture are for the user, following `TESTING.md`.

This package is ready for manual validation, not a confirmed Viewpoint VR implementation. Actual scene visibility, first-eye picking, shader compatibility, timing and stereo images remain unvalidated inside the game. The first diagnostic deliberately uses symmetric synthetic eyes and limited effects; no OpenXR, headset, controller or VR-UI functionality is enabled.

## Development baseline 0.3.0: concrete native-stage adapter

2026-09-30, Windows x64 / JDK 25, same pinned game/Viewpoint inputs and audited ZombieBuddy temporary fix.

The new `ViewpointBackend` executes through the production entry/stage transforms against independent synthetic native classes. **219 stage-fixture checks** passed across inactive behavior, complete pairs, native begin returning false, partial model preparation, first/right-eye draw failure, finish/copy/cleanup/restoration failures, target-size mismatch, frame recycling, renderer-generation changes and unsupported camera modes. Checks establish one shared preparation/frame-index advance, equal frozen delta time, two distinct eye matrices, one centered hand camera, per-eye far visibility, once-per-pair shadow/upload scheduling, deferred release and restored native output metadata. These are synthetic state tests, not GL or game execution.

`StageBinaryTest` resolved the backend's entire metadata contract against actual copied classes without initialization. The JDK verifier accepted all **three new transformed classes** (WorldRenderer, FarPass and TemporalPass), covering **29 stage anchors**. Each actual class was retransformed twice and the test transformer was removed afterward. Already patched input was rejected. The existing SceneDrawer verification/retransformation tests also passed, for four native target classes in total. Both HotSpot logs passed the no-game/mod-initialization audit.

The existing 256 camera checks, 485 lifecycle checks, 30 entry-fixture checks, 17 copied-entry checks, four Python cases and 21 original contract anchors also passed. The game's bundled JOML emits its existing Java 25 Unsafe deprecation warning during matrix fixture tests. Generated output remains isolated by run; the core JAR contains 29 production classes and no proprietary classes, test agent or fixtures.

No game, native renderer draw, OpenGL context or headset was launched. No saved graphics configuration or installed mod/game files were changed. A concrete native backend now exists, but its Output implementation is synthetic: real GL targets/capture and a live all-target loader/activation path remain absent. Producer visibility, actual images, picking, effect behavior and performance remain unvalidated in-game. The symmetric first-person diagnostic scope does not establish asymmetric headset rendering or VR gameplay support.

## Development baseline 0.2.0: lifecycle and dormant entry hook

2026-09-30, Windows x64 / JDK 25, pinned game/Viewpoint inputs and the audited ZombieBuddy B42.21 temporary fix.

- Four Python test cases passed, covering contract rejection and initialization-log auditing.
- Existing copied-binary contract: five methods / 21 invocation anchors passed.
- Camera: 256 checks passed.
- Pair lifecycle: 485 checks passed. These include one preparation/two synchronous copies/one release, skipped scenes, failure at every stage, state restoration, stale snapshot rejection, nested frame rejection, preserved primary/suppressed cleanup failures and successful recovery after failure.
- Transformed synthetic entry fixture: 30 checks passed. Inactive behavior and the enclosing exception/restoration path were preserved. Single-use callback, scope/thread ownership, recursion rejection and integrated success/second-eye failure paths passed.
- Actual copied `SceneDrawer`: 17 checks passed. The JDK classfile verifier accepted output. All fields and non-render methods stayed unchanged. Unknown JARs, changed executable instructions, wrong targets and already patched input were rejected. Equivalent normalized encodings were accepted. The JVM retransformed the uninitialized class twice and restored original code after removing the test transformer.
- HotSpot class-initialization logs were checked: no `viewpoint`, `zombie`, or `me.zed_0xff.zombie_buddy` classes initialized. Copied class verification is distinct from executing game/mod code.

Each run uses fresh ignored compiler/test output under `build/runs/<id>`, including the initialization log and a production-only `viewpoint-vr-core.jar`. Test fixtures and the standalone test agent remain separate from that JAR. No new external dependencies were downloaded; bytecode tooling reuses ASM from the pinned loader.

No production driver or installer enables the transform. Actual Viewpoint shared/per-eye stage separation, GL restoration, temporal state, visibility, eye images and runtime performance remain unimplemented/unvalidated. Tests use synthetic rendering stages; they do not establish safe replay of the real native draw method. There is no installable Viewpoint VR package or new live/headset result.

## Historical baseline 0.1.0

2026-09-30, baseline 0.1.0, Windows x64 / JDK 25.

`Test.ps1` passed against separately obtained Project Zomboid 42.21.0, Viewpoint 0.1.3 and ZombieBuddy 2.3.2 with the audited B42.21 temporary fix. Exact input hashes are in `pins.json`; the generated report records which accepted loader was used.

- Three Python test cases passed, including missing/duplicate/wrong-descriptor methods, missing/duplicate/reordered invocations and unknown-hash rejection before copying.
- Actual copied Viewpoint bytecode passed five method contracts with 21 invocation anchors. The tool ran `javap`, not game/mod classes.
- Camera code compiled against the game's bundled JOML API. All 256 numeric/input checks passed: Viewpoint yaw/pitch conventions, eye separation and midpoint, inverse camera transforms, rolled eye offsets, input nonmutation, asymmetric near-plane corners, far depth, world-axis mapping and invalid input rejection.

No OpenGL context, Viewpoint rendering, headset session, game launch, installation or user-data modification was performed. Stereo visibility, shared preparation, temporal state, failure cleanup, physical scale and performance remain unvalidated. There is no installable Viewpoint VR package at this milestone.

Historical PZ3D test results apply only to the inherited implementation. They are not evidence of Viewpoint compatibility. Run the new `viewpoint/Test.ps1` entry point rather than the inherited harness installer/build.
