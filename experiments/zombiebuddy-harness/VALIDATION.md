# Harness validation

## Baseline preservation (2026-09-30, source/package 0.11.0)

The `pz3d-vr-baseline` preservation includes the previously uncommitted hand-interaction implementation, its tests and settings. It adds current architecture/coupling documentation at [docs/PZ3D-BASELINE.md](../../docs/PZ3D-BASELINE.md), updates documentation entry points, and extends ignore rules. No new runtime behavior was introduced for preservation.

Before building, existing adapter output, harness compiler/test-class directories and the unpacked package were moved into ignored `.baseline-artifacts/20260930/`. The previous 0.11.0 ZIP/checksum and a crash log were copied there. Original diagnostics, research, proprietary references, local configurations and downloaded dependencies were retained. This gave the normal builder empty production compiler/package directories without destroying prior evidence. All **99 rebuilt project class files are byte-identical** to the preserved pre-housekeeping output (80 harness, 19 adapter).

The normal `Test.ps1` build/package and complete suite returned **exit 0**, with evidence in `build/runs/20260930-181826-989/` and `build/baseline-20260930-suite.log`. Hand-use checks: 35; turning: 43; controller lifecycle: 34; arm checks: 982; all existing rendering, camera, XR lifecycle, settings-related Java, melee/contact and GL checks passed. Fifteen actual copied classes passed transformation/verification without initialization. The expected deprecated `getForwardDirection` compiler warnings and JOML Unsafe runtime warnings remain; intentional negative fixtures still emit exception messages.

Additional `Test-ZombieBuddy.ps1` passed (exit 0), checking both pinned loader hashes, unknown-hash rejection and actual copied-binary transformation with the temporary-fix loader. The independent adapter suite passed **63 checks**. `ModOptionsTest.py` passed settings default/Apply/persistence/guard and recenter-status checks using read-only vanilla Lua plus mocks.

The package was refreshed with the final mod guide and audited against source/compiled classes. It contains no proprietary game/PZ3D classes, synthetic fixtures or personal absolute paths. Tracked/new candidates were also reviewed for generated binaries/logs, personal paths and credential patterns before staging; the staged diff was reviewed before the baseline commit. Generated ZIPs, logs, local runtime/editor configuration, downloaded tools, reference/decompiled material and preservation backups remain intentionally ignored.

This is a reproducible source/build preservation, **not new headset confirmation**. No Project Zomboid or dependency entrypoint was launched, installed files were not changed, and no live XR smoke test was started. The existing gameplay-validation gaps below remain. No Project Viewpoint work, branch, dependency installation or renderer refactor was performed.

## Hand proximity use (0.11.0 workspace build)

Inspected the copied PZ3D 0.3.0 `Main.b(boolean)`, `Interaction.choose/quick`, `Traversal.aimed/interact`, `WorldHit`, `WorldItems.Pick`, and `ControlRouting.Access`. The short-press dispatcher already selects native door/window/light/curtain, pickup, container UI and Lua contextual-action paths. The prototype keeps that dispatcher and substitutes its target only within a game-thread grip request. Hand selection suppresses the camera-derived traversal edge during its own native ray query; a selected intact window receives an edge for that object. Ordinary keyboard E and traversal run with no override.

Render capture publishes immutable tracked grip/head positions plus scene origin; no world action executes there. On a fresh grip edge the game thread filters world/item bounds within 0.25 scene units, confirms native ray selection and visibility from the tracked head, and checks that the selected surface remains within hand reach. It never falls back to a camera target when no hand target passes. This conservative selection and reuse of native dispatch is source-backed design, not proof that every object works in gameplay.

`Test.ps1` completed all suites in `build/runs/20260928-202438-881/`; aggregate output is `build/hand-use-suite-verified.log`. New fixtures passed **35 hand-interaction checks**, including nonzero scene origin/upper floor, a different gaze target, removed and occluded objects, proximity rejection, grip hysteresis/hold, menus, tracking interruptions between game ticks, active timed actions, diagnostics, simultaneous grips, window edge construction, and exception cleanup preserving keyboard E. The deliberate native-action exception in that log is an expected negative test. Controller fixtures passed **34 checks**, including selected-bumper reservation; turning passed **43**, including raw-VR aim conflicts and release-to-rearm after changing ownership. The existing renderer, XR lifecycle, melee/contact, arm, hotkey and GL suites also passed.

Copied binaries accepted three additional hook targets (`Main`, `Interaction`, `Traversal`), including composition with the existing `Main.tick` turning hook; this adds two unique classes, for fifteen unique transformed classes overall. Reflection contracts for target bounds, routing, native dispatch and window/choice constructors were inspected without class initialization. Lua settings tests passed defaults, Apply, persistence and guards against the copied/read-only vanilla settings implementation.

The 0.11.0 package was checked against compiled project classes and source Lua/README, scanned for personal absolute paths, and checked to exclude proprietary game/PZ3D classes. No installed files were changed and no game/mod entrypoint was run. Hand interaction remains **unconfirmed in actual gameplay/headset testing**. Begin with doors while looking elsewhere, then windows, switches and containers; verify dashboard/menu return and synthetic-preview rejection. Vehicle entry, hold-to-climb and corpse/animal targeting are outside this prototype.

## Loaded ZombieBuddy temporary fix and Mods slider label (0.10.1)

The user's 0.10.0 game log rejects the actually loaded game-root ZombieBuddy.jar with SHA-256 `dd13e6e06e64be0e832a4f13508c6872de36c9c7b2290023c5884a7f74467283` before renderer/XR initialization. It matches the subscribed B42.21 temporary-fix JAR, not the original Workshop 2.3.2 binary checked during the earlier compatibility update. The null SteamVR server/compositor are running and its server log identifies the existing null HMD; this does not prove a game XR session.

Read-only archive comparison found fifteen differing entries, confined to Loader/nested classes, ZomboidFileSystem patches and signature/manifest metadata. The ASM and Lua exposure classes are unchanged. Inspection shows the temporary fix adapts mod-loading List signatures; the static instrumentation handle used by our bridge retains its contract. Both exact hashes are now accepted, with game/PZ3D pins unchanged. This is compatibility verification, not execution or general certification of the third-party loader.

The same console reports an UnknownFormatConversionException for `)` through Translator.getText while MainOptions.lua:3025 constructs a slider. Our literal `Maximum arm reach (%)` label follows that path. Replaced it with `Maximum arm reach (percent)`; saved setting ID/value remain unchanged.

Full harness regression passed: `build/runs/20260928-195523-457/`, with 40 turning, 33 contact, 982 arm and 31 controller checks plus the existing suites. Separate `Test-ZombieBuddy.ps1` passed acceptance of both pinned variants, rejection of unknown loader/game data, and all thirteen unique copied target transformations with the temporary-fix JAR on the classpath. The actual Loader instrumentation field was inspected without initialization (`build/zb-variants.log`, `build/zb-fix-transform.log`). Lua settings tests passed, including a literal-percent slider-label regression. No game/loader entrypoint was run and no installed files or running runtime were modified. User restart and in-game confirmation remain required.

## Stick turning and ready/aim (0.10.0, 2026-09-28)

Adds Off/Snap/Smooth, 15/30/45/60/90-degree snap angles, smooth speed 30–240 degrees/second, input-source selection, and a separate held ready/aim binding. Automatic input prefers the assigned physical gamepad and otherwise uses existing OpenXR Touch input. Physical pads use configured native aiming axes; direct VR turning works without registering the virtual gamepad. All features are restricted to focused first-person XR gameplay and default Off.

An additive Main.tick hook updates PZ3D's shared LookState yaw under its existing synchronization lock, preserving pitch and calibration. NativeAvatar.attackAim is extended with the separate ready input. Scoped Joypad methods consume the selected pad's aiming axes and chosen aim binding, including generic button queries; native menus and unowned pads retain input. Snap requires centering between deflections; focus/context/source/settings changes require neutral controls. Failures disable turning and release input ownership.

Artificial turns disarm both motion-melee modes for the turn and 200 ms afterward. Motion melee requires attack-trigger release to rearm; pending contact resolution is rejected. This deliberately excludes simultaneous artificial turning and motion-melee attacks in the first implementation. Native button attacks retain their original path. No new controller interaction profiles or full-body tracking are introduced.

The full harness suite passed, including rendering/XR lifecycle, 982 arm checks, 31 controller lifecycle checks, existing melee/geometry tests and actual copied-class retransformation without initialization. Three turning targets (two new classes and NativeAvatar shared with melee, thirteen unique target classes total) verified with the existing transforms. Evidence: `build/runs/20260928-190614-583/`. Final targeted turning tests passed 40 checks (`build/turn-test.log`) covering smooth timing, snap rearming, axis consumption, separate aim, native menu restoration, physical disconnect, direct VR input, settings/context changes, duplicate/missing hook rejection and preserved pitch. Contact tests passed 33 checks including turn suppression, release-to-rearm and pending-contact cancellation (`build/contact-turn-test.log`). Lua settings tests passed default/Apply/save/load checks for the new controls with mocked game services.

The final package was rebuilt after the targeted guard refinements. No game/mod entrypoint was run or installed files modified. Live gamepad assignment, menu transitions, weapon ready stance, body/hand alignment and comfort remain user-test items. The native OpenXR action set and graphics submission are unchanged; no additional runtime smoke test was needed.

## Zomboid 42.21.0 / PZ3D 0.3.0 compatibility (0.9.1, 2026-09-28)

The user's current console identifies Zomboid 42.21.0 and PZ3D 0.3.0; the VR mod is absent from the loader list. The locally installed prototype descriptor reports 0.4.0 and caps Zomboid at 42.20.4. Source 0.9.0 had the same game-version restriction. The installed ZombieBuddy JAR is byte-identical to the supported 2.3.2 copy despite the reported Workshop update. A separate subscribed temporary-fix mod is not the ZombieBuddy JAR identified in the current log and is not certified by this build.

Updated exact game/PZ3D hash pins, descriptor limits, build/test reference paths and failure-report version labels. The package remains under `42.20.4` for installation continuity, with `versionMin` and `versionMax` both set to 42.21.0. Unknown binaries are still rejected. Replace the old folder with the new package; editing its version limits alone is insufficient.

Inspection of copied PZ3D 0.3.0 shows unchanged renderer preparation/eye boundaries, attachment upload and reflection layouts used by the adapter. The first updated-binary test correctly rejected CombatManager: 42.21.0 added a leading `List` argument to `processTreeHit`. The scenery guard now matches the new descriptor and reads the owner from local slot 2. Its synthetic fixture now uses that signature, checks that owned contact attacks suppress scenery effects and clear cached objects, and preserves ordinary unowned behavior. The native SwipeStatePlayer and low-level Controller/Controllers class bytes are unchanged. Native multiplayer hit aggregation changed, but this prototype remains single-player only.

Validation: the full harness suite passed against the new copied binaries, including eleven actual class retransforms without initialization, 16 melee hook contracts, 29 contact adapter checks, 20 contact geometry checks, 47 earlier melee adapter checks, 982 arm checks, 31 controller lifecycle checks, and existing rendering, XR lifecycle, mapping, timing and shortcut suites. Evidence: `build/runs/20260928-182626-253/` and `build/controller-lifecycle.log`. Lua settings/recenter UI checks passed against the installed 42.21.0 ModOptions with mocked game services and I/O. The offline renderer adapter suite also passed 63 checks.

No game/mod entry point was executed and no installed game, Workshop or user-data files were changed. This establishes build, bytecode and fixture compatibility; a user-run game launch, XR session and combat test are still needed to confirm live behavior. No additional physical-headset validation is claimed.

Pinned SHA-256 values:

- Zomboid: `e1a69eb743ede60b213a0fe7f8b83d4fcab773036d256cc4543a336f3b058a33`
- PZ3D: `e8ddcdb6047dfe1bffe017aceda12169497f60a19d4039a171d44199a61df57d`
- ZombieBuddy: `6dd95cedce60f03bf8b8cefd0d19eb156230e0d54bffa07de9da5212a06c7be6`

## Contact-timed baseball bat pilot (0.9.0)

The opt-in contact modes derive a swept capsule from the rendered plain baseball bat's mesh bounds and tracked attachment/world transforms. The simulation thread selects the first eligible standing zombie, starts native combat, and resolves its collision at native attack-state readiness, with a 150 ms contact expiry. Scoped hooks replace only that attack's hit list, suppress its subsequent animation collision and unarmed callbacks, and exclude unrelated scenery hits. Native recovery remains authoritative. Off and animation-timed modes remain available.

The full `Test.ps1` suite passed: 29 contact combat fixture checks, 47 existing melee adapter checks, 92 gesture checks, 982 arm checks, 31 controller lifecycle checks, and the existing capture, mirror, XR lifecycle, mapping, OpenGL, camera, timing and hotkey suites. Eleven copied rendering/controller/combat target classes passed JVM retransformation verification without initialization; 16 melee transformation contract checks passed. Evidence: `build/runs/20260927-211958-176/` and `build/controller-lifecycle.log`.

Contact fixtures exercise diagnostics, selected-target identity, native state readiness, duplicate collision suppression, scenery isolation, misses, prone/fake-dead exclusion, window/solid obstruction, target movement, contact expiry, trigger release, tracking/equipment changes, native cooldown, UI, multiplayer exclusion, stationary overlap, body translation, and earliest-target selection. They use original synthetic game fixtures, including an instrumented synthetic CombatManager; they do not execute the actual game's combat implementation.

The final geometry suite passed 20 checks (`contact-geometry-final.log`): segment/sweep geometry, discontinuities, size limits, queue overflow, scene origins, row-major attachment conversion through the render capture path, local-player selection and ambiguous attachment rejection. Standalone Lua settings tests passed with native ModOptions and mocked services, including the contact mode Apply mapping and persistence regressions.

No game launch or installation was performed. Actual mesh fit, damage/recovery behavior, animation coexistence, headset thresholds and contact feel remain unverified. Zombie capsules are approximate; targets moving during sampling are not swept. Translation compensation does not establish immunity to camera rotation or recenter discontinuities. Misses do not incur native missed-swing costs. This is a single-player, plain-bat, standing-zombie pilot, with no positional headshots, scenery damage, multi-hit or multiplayer support.

## Bounded controller reach extension (0.6.5)

The arm solver now extends upper-arm and forearm lengths proportionally only when a tracked target exceeds native reach. Extension is capped by a persisted Mods setting (100–175%, default 150%). Segment palette transforms stretch along the bone direction so mesh endpoints follow the new joints; wrist/finger transforms, palm offset, and item scale stay independent. This is automatic bounded extension with a manual limit, not anatomical arm-length measurement. Gameplay attack range is unchanged.

The full harness suite passed. Final arm tests passed 982 assertions, including beyond-native controller targets, proportional lengths, mesh endpoint continuity, preserved thickness and hand/item size, bounded outliers, retraction, no accumulated growth, settings applied through the full bridge, and prior kneeling/tracking-loss regressions. The standalone Lua test confirmed the new default, Apply, and native settings save/load. Evidence: `build/runs/20260927-185814-728/` (`arm-tracking-final.log` includes the final bridge checks).

Physical-headset fit and sleeve/elbow appearance remain unverified. Targets beyond the configured limit still clamp. No game launch or installation was performed.

## Physical headset height and shoulder roots (0.6.4)

Physical headset height relative to the camera's recenter anchor now supplies a displacement for tracked arm roots. The camera converts only the LOCAL-space Y-height delta into scene space; each skin part converts that direction into model space before the arm solver. The shoulder, reference elbow, and reference wrist translate together, preserving native segment lengths. Native camera height/crouch changes are not included in this additional displacement. Arm palette and item overrides retain their existing per-pair scope and restoration behavior.

The full harness suite passed, including 933 arm/attachment assertions and 47 camera checks. New checks cover lowering both arms and clothing, held-item translation exactly once, untracked-hand fallback, unchanged torso, restoration on standing, original palette restoration, physical height versus native camera height, horizontal/head-rotation exclusion, and recenter resetting the height reference. Evidence: `build/runs/20260927-185040-653/`. Packaged classes match the tested build.

Physical-headset confirmation remains pending, especially shoulder/torso mesh blending. This is a first-person arm-root correction, not full-body IK or gameplay crouch detection. Avatar reach limits are unchanged. No game launch or installation was performed.

## Delayed recenter (0.6.3)

The recenter shortcut schedules a five-second countdown, allowing both hands to return to controllers before camera and arm calibration. A vanilla UI overlay shows remaining seconds, tracking/focus wait, and completion; the existing VR UI panel carries that overlay. Repeating the shortcut restarts the timer; XR stop or preview-mode change cancels it. Completion requires XR focus and the controller sides tracked when the request was made. No-controller use remains supported.

The full `Test.ps1` suite passed, including 11 deterministic timer checks for delayed execution, focus/tracking wait, single completion, notice expiry, restart, and cancellation. Existing arm, capture, mirror, XR lifecycle, camera, timing, hotkey, OpenGL, and copied-class checks passed. Evidence: `build/runs/20260927-183723-114/`. Standalone Lua tests passed for idle/active overlay drawing, missing bridge, and existing settings persistence. The package was checked against the compiled classes and Lua source.

No game was launched or installed. Physical-headset countdown readability and calibration timing remain user-test items.

## Hand rotation after tracking interruption (0.6.2)

User reported downward-pointing hands at neutral controller orientation, especially after opening the SteamVR dashboard. Inspection found two automatic calibration resets: `TrackedArms.apply` called recenter when both hands were missing, and `ArmRig.pose` cleared each missing hand's wrist correction and palm offset. Resumption therefore learned a fresh correction from an arbitrary controller/native-animation pose.

Both loss paths now preserve calibration while retaining per-frame native-pose fallback. Runtime LOCAL-space change notifications reanchor the camera without learning a new hand correction. Explicit user recenter, session restart, and synthetic-preview mode changes retain their existing calibration-reset behavior.

`Test.ps1` passed, including 596 arm/attachment assertions and the existing lifecycle, hotkey, camera, timing, OpenGL, and four-class retransformation checks. New regressions cover changed native animation/controller rotation on resume, return to neutral, explicit recalibration, repeated both-hand loss and one-hand occlusion, and held-item orientation. Running those tests against isolated copies of each pre-fix class failed at the expected orientation assertions; both pass with the fixes. Evidence: `build/runs/20260927-183149-034/` and `build/rotation-regression-before/`.

Physical-headset dashboard/resume confirmation remains pending. No game launch or installation was performed. Initial orientation still depends on the first calibrated pose; this is not an anatomical mapping redesign.

## Remappable shortcuts (0.6.1)

Added a built-in Options > Mods > PZ3D VR page with four action keys and independent Ctrl/Shift/Alt selectors. Default chords remain unchanged. Java receives immutable settings and keeps polling physical keys on the render thread; it does not depend on Lua key events filtered by PZ3D. Bindings use vanilla ModOptions persistence. Options/text entry suppress input, and settings/focus changes require key release. Exact duplicate chords are inactive.

Validation: `Test.ps1` passed all 559 checks across capture, mirror, XR lifecycle, OpenGL, camera, timing, configurable hotkeys, and tracked arms; all four copied target classes retransformed without initialization. The 21 new hotkey checks cover remapping all actions, native key-code conversion, clearing, exact modifiers, physical-key latching, duplicate chords, settings changes, and focus/UI release behavior. Evidence: `build/runs/20260927-182613-514/`.

`tests/ModOptionsTest.py` also passed using the installed vanilla `PZAPI/ModOptions.lua` under workspace-local Lupa 2.8 / Lua 5.1, with in-memory file and game-service stubs. It checks registration/defaults, the key-picker Apply bridge, clearing, native save/load, options/text-entry suppression, and an unavailable Java bridge. To reproduce, install `lupa==2.8` into `build/lua-test-tools` and pass your vanilla `PZAPI/ModOptions.lua` path to the script. It does not run game or dependency-mod entrypoints or write game settings.

Still pending: user-run in-game confirmation that the settings page appears, remapping applies, and choices persist after restart. The agent did not install the mod or launch the game. No native OpenXR runtime test was repeated for this input/settings change.

The package was built and tested in the workspace. The agent did not launch Project Zomboid or install the harness. The user installed and launched version 0.1.0; its live log confirms successful loading, but no capture was requested. Version 0.1.1 subsequently captured a live stereo pair in the user-run game at 12:36:17 EDT.

| Check | Result |
|---|---|
| Shared adapter bytecode | All three copied PZ3D target classes verify with the JDK classfile verifier |
| Harness lifecycle | 23 checks passed using real JVM instrumentation and original synthetic fixtures |
| Actual copied binaries | Three supported classes defined without initialization and successfully retransformed under `-Xverify:all` |
| Continuous mirror lifecycle | 18 checks passed: toggling/focus, repeated frames, reuse, resize, unsupported views, capture coexistence and failure cleanup |
| Real OpenGL capture and mirror | 25 checks passed on NVIDIA RTX 4090 using a hidden standalone GLFW compatibility context |
| In-game ZombieBuddy loading | Versions 0.1.0 and 0.1.1 loaded successfully |
| Live world stereo images | First indoor pair captured and visually inspected successfully; broader scene/animation coverage remains |

Lifecycle checks include deliberate incompatible bytecode and second-eye copy failure. Their logged errors are expected negative tests. The mismatch rolls back the installation; the copy failure produces a failed report, releases ownership/targets, avoids PZ3D's global failure handler, and resumes `draw(false)` to avoid repeating native preparation. JOML emits its existing Java 25 Unsafe deprecation warning.

OpenGL checks verify persistent red/blue eye images despite scratch-target reuse, green top/yellow bottom orientation, PNG dimensions, allocation bindings, separate read/draw framebuffer restoration, pixel pack/unpack buffers and pack layout, and scissor/sRGB enable restoration. This is the actual packaged `EyeCapture` code, independent of the CPU fixture stand-in.

Local evidence from the final full harness run:

- [Lifecycle log](build/runs/20260927-130656-632/lifecycle.log).
- [Actual copied binary retransformation](build/runs/20260927-130656-632/real-binary.log).
- [GPU test log](build/runs/20260927-130656-632/gpu.log).
- [GPU left image](build/runs/20260927-130656-632/gpu/left.png) and [right image](build/runs/20260927-130656-632/gpu/right.png).

All evidence and built packages are local ignored artifacts. The source, build scripts, and original test fixtures remain reviewable. Generated proprietary target class files and test-agent/stub classes are excluded from the distributed mod ZIP.

## Live input diagnosis and correction

The user's console.txt (2026-09-27, inspected at 12:25) reported the harness Ready message and successful main invocation. No capture request or output directory was present. F8 is owned by PZ3D's camera panel, and Input.blockEvent filters owned Lua key events. Version 0.1.1 removes the Lua handler and polls Ctrl+Shift+F10 through GLFW from the existing render-thread hook. Tests cover missing modifiers, focus loss/refocus, held keys, release/repress, both modifier sides, and pending unsupported views. The Lua file remains as a comment-only replacement to overwrite the previous handler during extraction.

## First successful live capture

User-operated version 0.1.1, 2026-09-27 12:36:17 EDT. Read-only inspection of the game log found the corrected Ready message, a capture request and success at frame 4661, with no capture exception. Output directory: `%USERPROFILE%\Zomboid\PZ3D-VR-Test\20260927-123617-277-c89f0bc5`.

The report records `STEREO_PAIR_CAPTURED`, `preparations=1`, `copies=2`, `leaseBracketCompleted=true`, scene version 44, generation 2, and a fresh frame. Both PNGs decode and show upright 2560x1440 indoor views. Eye positions differ by 0.063999709 scene units with zero vertical offset, matching the requested 0.064 separation. Distinct view matrices and visible depth-dependent horizontal parallax support a successful synthetic stereo capture. The nearby hands/bat shift substantially more than the distant doorway; no gross blank-eye or orientation failure is visible. This is visual inspection of one static pair, not a comfort, animation-coherence, physical-scale, performance or all-scene validation. OpenXR submission and headset validation remain false.

Image SHA-256 values:

- Left: `114c5316cec46f9241232c91066dc3eaba63fc794fd69d6871333d3cbb026c43`.
- Right: `adaf18467b2f1b4e1c3f035491de734ebbfcd139cbde3d3e97e4aee8bdd727d8`.

## Version 0.2.0 live desktop mirror

Workspace tests passed for continuous GPU-only side-by-side rendering in the existing game window. [Mirror lifecycle log](build/runs/20260927-130656-632/mirror.log) covers fresh/retained draws, toggle edges/focus, target reuse, resize, suspension/resumption, PNG capture while mirroring, and injected second-eye failure with ordinary-draw fallback and full target cleanup. The real OpenGL test verifies red-left/blue-right placement, black letterboxing, restored color mask/clear color/framebuffer/enables, and three subsequent pairs using the same targets. The existing 23 one-shot lifecycle checks and actual three-class retransformation also pass.

The user subsequently confirmed continuous mode works smoothly in-game, with distinct eye positions and consistent scene details. No measured FPS or hardware-headset result was supplied. This validates the helper and synthetic lifecycle, not continuous live-world resource consistency, gameplay UI usability, or sustained performance. The agent did not install the package or launch the game.

## Version 0.3.0 OpenXR integration

The package now includes an opt-in OpenXR session backend, runtime eye/head pose conversion, asymmetric projections, recentering, same-context swapchain copies, desktop mirroring and owner-thread teardown. Live game integration remains pending the user's test. The agent did not launch the game or modify installed mods.

Final verification on 2026-09-27:

| Check | Result |
|---|---|
| Existing capture / desktop mirror / GPU checks | 23 / 18 / 25 passed |
| XR game glue with synthetic renderer/runtime | 14 passed: opt-in, unavailable runtime, pair coherence, target reuse, render skipping, recenter, toggle, unsupported view, runtime loss and inactivity heartbeat |
| XR camera math | 39 passed: identity agreement with existing stereo math, axis conversion, translation, yaw, roll, recenter, asymmetric FOV |
| Actual copied PZ3D classes | All three retransformed and verified without initialization or game/mod entry points |
| Packaged XR backend with simulated SteamVR | 371 assertions, 120 submitted projection pairs across two session lifetimes |
| Partial-pair callback failure | A zero-layer frame ended successfully; subsequent projection submissions succeeded |
| Missing-runtime test | Expected initialization failure, caller GL context preserved and subsequent desktop GL rendering succeeded |
| Simulated runtime helper | Start/status/stop passed; separate default-runtime client connected without config-path overrides |
| In-game XR / physical headset | Not yet tested |

Evidence: [XR glue](build/runs/20260927-130656-632/xr-lifecycle.log), [camera math](build/runs/20260927-130656-632/xr-camera.log), [final simulated runtime](build/xr-runs/20260927-130755-140-xr/stdout.log), [missing runtime](build/xr-runs/20260927-130755-074-missing/stdout.log), [default-runtime client](build/xr-runs/20260927-130503-278-xr/stdout.log). The runtime uses fixed null-driver poses; successful submission does not verify binocular comfort, physical tracking, color calibration, or live PZ3D frame scheduling under XR pacing. Session states reached READY/SYNCHRONIZED/VISIBLE/FOCUSED. Tests preserved the caller context across destruction and recreation. The simulated runtime was stopped after testing.

The shipped backend deliberately keeps the process-wide LWJGL XR loader alive, since other components may share it. It destroys its own swapchains, spaces, session and instance. Game UI is desktop-only. One scene unit per metre and game-resolution rendering with scaling to recommended swapchain sizes are provisional policies.

## Version 0.3.1 input correction

Vanilla's installed shared/keyBinding.lua and the user's Zomboid/Lua/keys.ini assign F6 to Fast Forward x3 (LWJGL2 key 64). Vanilla IngameState also hard-codes F7 (65) for debug editors, including Ctrl and Shift combinations. Scroll Lock (LWJGL2 70 / GLFW 281) has no assignment in those bindings or the vanilla/PZ3D input paths inspected. Version 0.3.1 uses Ctrl+Shift+Scroll Lock to toggle XR and Ctrl+Shift+Alt+Scroll Lock to recenter. Neither F6 nor F7 is read as an XR trigger.

The physical Scroll Lock key is latched until release, preventing modifier transitions from turning a recenter press into a toggle. Both Alt keys are accepted. The source, package metadata, startup messages, runtime helper, and instructions agree on these shortcuts.

The complete local suite passed: 23 capture, 18 live mirror, 21 XR glue/input, 39 camera-math, 25 GPU checks, and retransformation of all three actual copied target classes. [XR input/lifecycle evidence](build/runs/20260927-131951-225/xr-lifecycle.log). The XR backend itself is unchanged; the previous native runtime validation still applies. No installed mods, game bindings, or running game were modified.

## Simulated runtime lifetime fix

The user's 0.3.1 game log confirmed that Ctrl+Shift+Scroll Lock reached the XR hook twice; both calls failed at xrCreateInstance with result -2. The 13:46:05 simulated server log explicitly recorded shutdown after 20 seconds without a client (13:46:26). Steam's xrclient_ProjectZomboid64 log reported HmdNotFound from its client-side presence check. The installed mod hash matched the expected 0.3.1 artifact.

SimulatedRuntime.ps1 now starts RuntimeKeepalive.py, which connects through the installed OpenVR API as a non-rendering overlay client (no overlay window or scene submission). It exits on a stop marker or the original server process ending. Stop verifies the keeper process start time, signals graceful exit, then stops owned SteamVR processes. A trial with SteamVR's monitor was discarded because it restarted the server and launched room setup; those trial processes were stopped.

A longer test also exposed simulated-headset standby: the installed SteamVR defaults place pauseCompositorOnStandby under `power`, not `steamvr`. The isolated profile now sets it in the correct section and extends screen/controller sleep to 24 hours. Game/mod code and bindings are unchanged.

Final runtime started at 13:54:17 and remained available without a scene client beyond the old idle timeout. A separate default-runtime client connected at 13:55:00, reached VISIBLE/FOCUSED, and submitted 120 stereo projection pairs across two sessions (371 checks passed): [delayed-start smoke evidence](build/xr-runs/20260927-135500-427-xr/stdout.log). The corrected runtime is intentionally left running for the user's immediate in-game retry. This remains a standalone backend check, not proof of in-game XR success.

OpenVR application-type semantics are defined in the [official header](https://github.com/ValveSoftware/openvr/blob/master/headers/openvr.h). Background clients explicitly do not keep SteamVR running, so the keeper uses the non-scene overlay type instead.

## Draggable simulated headset window

HeadsetWindow.py targets only the exact `Headset Window` title owned by the installed SteamVR `bin/win64/vrcompositor.exe`. It replaces the popup frame with a caption, system menu and minimize button, then applies SWP_FRAMECHANGED without changing activation or stacking. DPI-aware frame sizing preserves the client extent. The keepalive checks once per second and applies the frame only when missing, leaving manual positioning alone.

Applied to the user's existing compositor window on the interactive desktop (PID 15364, HWND 658038). Readback verified style `0x16ca0000` and an unchanged 1280x720 client area. A subsequent WM_NCHITTEST query in the caption returned HTCAPTION (2), confirming Windows recognizes a draggable title bar. No game/runtime restart was performed. Future helper starts apply it automatically; the currently running older keeper was left uninterrupted.

API references: [SetWindowLongW](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowlongw), [SetWindowPos](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowpos).


## In-game OpenXR confirmation and dashboard fix

The user confirmed working game output in SteamVR's Headset Window. The live game console recorded two 940x940 swapchains, VISIBLE session state, and increasing projection-pair counts through 6,600 before the game exited normally. This confirms in-game submission to the simulated runtime; physical tracking and headset comfort remain untested.

The simulated profile now explicitly disables dashboard.enableDashboard and dashboard.autoShowGameTheater. Disabling startDashboardFromAppLaunch alone did not prevent Steam from starting its system UI. The game was already closed when the isolated runtime was restarted. No mod JAR changes or reinstall are required.

Read-only OpenVR queries against the restarted runtime returned enableDashboard=false, autoShowGameTheater=false, and IsDashboardVisible=false. Evidence: build/simulated-runtime/20260927-141519-343/dashboard-verification.json; query source: build/verify_dashboard.py. The first probe from the restricted desktop failed (121) and a subsequent client stalled; those probe processes were cleared and verification succeeded after restarting the isolated runtime on the interactive desktop. The helper is left running for the user's next game test; gameplay after this settings change is not yet verified. Function signatures/slots were checked against Valve's official openvr_capi.h, IVRSettings_003 and IVROverlay_028.


## Version 0.3.2 frame timing instrumentation

Adds render-thread wall-clock summaries every five seconds and on session close: successful stereo submission rate, skipped/failed counts, call intervals, time outside XR, runtime waits, shared preparation, separate eye draws, desktop/XR copies, and frame submission. A default no-op Sink.beginEye observer marks the existing eye-loop boundary without changing transforms or rendering order. Fixed-size histograms provide 0.25 ms p95 upper bounds and explicit overflow; no GPU synchronization/readback was added. CPU wall time is not GPU execution time, and submission rate is not display refresh.

Test.ps1 passed: 23 capture lifecycle, 18 live mirror, 21 XR glue/input, 39 camera-math, 25 real OpenGL capture checks, all three actual copied-class retransforms, and 13 new deterministic timing checks covering rates, skipped/failed accounting, spikes, overflow, window reset and timed/final flush. Evidence: build/runs/20260927-142557-270/. Native successful-session testing of this version was deferred because the user's game is running; no competing scene client or game restart was attempted. New in-game timing results remain pending.

The packaged missing-runtime test also passed both checks (expected initialization failure, caller context preserved): build/xr-runs/20260927-142704-567-missing/. Archive CRCs, version, timing class, exclusion of project test classes and SHA-256 were verified. JAR SHA-256: 53a7817cfeeec1f18c11fac6fbfea128857ea0c50c4af9f7927864071887f2a1.


## First live 0.3.2 timing review

Latest user test reached FOCUSED and logged 19 full timing windows totaling approximately 95.19 seconds: 4,213 submitted pairs, zero skipped/failed calls, approximately 44.26 submissions/second (window range 37.66?54.36). All recorded calls exceeded the runtime's 11.11 ms period. Weighted wall-clock averages: XR total 19.73 ms, outside XR 2.87 ms, preparation 3.51 ms, left eye 6.54 ms, right eye 6.29 ms, xrWaitFrame 0.02 ms. Stereo frame intervals averaged 22.60 ms; largest was 119.58 ms in the startup window. Destination-copy averages are per eye.

The matching SteamVR scene process (PID 14396, 14:39:15?14:40:52) reported application CPU 22.124 ms / GPU 2.837 ms, compositor CPU 0.376 ms / GPU 0.145 ms. This supports a render-thread-side bottleneck rather than xrWaitFrame pacing or GPU saturation; wall time includes driver waits and prototype consistency checks and does not isolate pure renderer CPU work. Null-driver dropped/present counters are not treated as literal physical-display statistics. Separate Knoxify Lua exceptions were present and may add overhead; their contribution is unmeasured.

Filtered timing evidence and aggregate calculations saved to build/timing-review/latest-timing.txt and latest-summary.json. No code or runtime settings were changed during this review. Next profiling should separate per-eye renderer work from diagnostic consistency checking; use a minimal-mod comparison to isolate the unrelated Lua errors.


## Version 0.4.0 vanilla UI quad

Reuses the completed, version-gated PZ3D UiLayer texture after the stereo pair. Cached reflection reads only its texture/width/height fields, verified against the copied binary without initialization. Copies the premultiplied-alpha image once into a separate XR swapchain, then submits a VIEW-space quad after the world projection layer. Panel is centered 1.5 m ahead, at most 2 m wide / 1.3 m high with preserved aspect ratio. Copy preserves GL read/draw FBO bindings and scissor/sRGB enables. Runtime dimensions limit allocation; resize replaces owned swapchains. A missing snapshot omits UI for that frame. Frame failure submits no layers; all owned resources are released with the session.

The game still updates its native UI once. The snapshot can lag by one UI refresh; cursor/ImGui drawn outside the texture and PZ3D world-space annotations are excluded. Input is unchanged. Main-menu VR and controller interaction are outside this change. UI_COPY wall timing is separate from eye copies.

Test.ps1 passed: 23 capture, 18 mirror, 24 XR glue (including one UI copy per pair, absent snapshot and recovery), 39 camera, 25 GPU capture, 13 timing checks and all three actual-class retransforms plus UiLayer field checks. Evidence: build/runs/20260927-151736-942/.

Packaged native backend passed 883 checks against simulated SteamVR, submitting 120 projection pairs and 100 UI panels across two session lifetimes. Tests exercise premultiplied-alpha flags, both-eye visibility, aspect and placement, 320x240 to 160x120 resizing, world-only frames, GL binding/enables preservation, out-of-frame rejection, partial-eye and post-UI injected failures, and context reuse after teardown. Evidence: build/xr-runs/20260927-151842-702-xr/. Initial native attempt failed because the earlier simulated server was no longer running; the workspace helper was started and the test passed. No game or existing mod entry point was launched, and no installed files were modified. Runtime is left available for the user. Actual in-game UI appearance and physical-headset comfort remain unverified.

Composition contract: https://registry.khronos.org/OpenXR/specs/1.1/man/html/XrCompositionLayerQuad.html . Premultiplied alpha is retained (BLEND_TEXTURE_SOURCE_ALPHA without UNPREMULTIPLIED_ALPHA).


## User confirmation of 0.4.0 UI

The user confirmed that the vanilla UI panel works in-game with the simulated Headset Window. Physical-headset validation remains pending.

## Version 0.5.0 tracked arm prototype

Adds OpenXR left/right grip pose actions and action spaces, sampled in LOCAL space at the head/eye predicted display time while the session is focused. Missing/inactive/untracked controllers retain native animation per hand. Suggested bindings cover simple, Touch, Index, Vive and Microsoft motion profiles. Action setup/sync failure leaves the world renderer available and logs the controller failure.

The stereo preparation hook applies one owned palette per local body/clothing part after common preparation and before both eyes. It decodes vanilla offset/model storage, solves two-bone arm IK, applies wrist orientation deltas with initial native-pose alignment, propagates finger descendants, and restores original palette/mask references in the outer pair `finally`. Original buffer contents and animation players are untouched. Upper-arm masks are widened only on tracked sides; static held props are hidden during overrides. Other characters are untouched. Unchanged bones retain exact source floats. Model reach limits apply. A skeleton/pose failure rolls back all earlier substitutions and disables IK until the next XR session. Native shadows are prepared before the override and remain a known visual limitation.

Workspace validation on 2026-09-27:

- 298 arm checks: real legacy matrix mul/store round trips with nonidentity offsets; reachable/clamped/singular targets; bone lengths; wrist rotation; finger-relative transforms; untouched opposite arm/spine; mask selection; world/LOCAL/model conversion; separate clothing buffers; reuse; exact original bytes; reference restoration; tracking loss and partial-override rollback. Heap palette buffers are rejected before entering the old JOML Unsafe buffer path.
- 59 adapter checks, including one override for both eyes and exactly one restoration after normal completion or injected second-eye failure.
- Full harness suite: 23 capture, 18 mirror, 27 XR lifecycle, 42 camera, 25 real OpenGL capture, and 13 timing checks. All three copied PZ3D classes retransform without initialization; actual arm/mask/skinning metadata field layouts are checked. Evidence: `build/runs/20260927-164525-417/` and `../pz3d-adapter/build/test-results.txt`.
- Native packaged backend: 1,251 checks against simulated SteamVR, including successful real action-set/space creation, attachment and synchronization across two session lifetimes, with no controllers present. Submitted 120 projection pairs and 100 UI panels; existing resize, partial-pair/post-UI failure and GL-state cleanup tests pass. Evidence: `build/xr-runs/20260927-164657-178-xr/`.

The first isolated arm test used a heap test buffer with the game's older JOML, triggering its Unsafe native access failure. The fixture now uses direct buffers like the game; production retargeting also rejects heap/nonwritable output buffers. Subsequent tests pass. This occurred in a standalone test process, not the game.

Ctrl+Shift+Alt+F9 enables synthetic waving while XR runs, allowing the user to test actual character/clothing deformation without controllers. In-game arm rendering, controller motion, wrist/palm alignment and clothing fit have **not** been validated by these standalone checks. No game/mod entry point was launched, no installed files were modified, and no 0.5.0 GitHub release was published. The simulated runtime remains available for the user's test.


## Version 0.5.1 preview hotkey correction

Removed both F9 preview bindings: vanilla debug mode handles KEY_F9 (67) in IngameState to enter SeamEditorState without excluding modifiers. Ctrl+Alt+Scroll Lock (without Shift) now toggles synthetic arm preview while XR is active or desktop stereo otherwise. Existing Ctrl+Shift+Scroll Lock XR toggle and Ctrl+Shift+Alt+Scroll Lock recenter remain unchanged. The physical Scroll Lock edge is shared across actions; modifier changes while held cannot trigger a second action.

No Scroll Lock input handler was found in the inspected vanilla Lua, PZ3D input source, or vanilla IngameState/GameWindow/UIManager/GameKeyboard bytecode. The local user key bindings do not assign Scroll Lock. This does not cover arbitrary additional mods or custom future bindings.

Full Test.ps1 suite passed: 23 capture, 19 mirror, 28 XR lifecycle, 42 camera, 25 OpenGL, 13 timing, and 298 arm checks, plus copied binary retransformation/layout checks. New regressions ensure both old F9 chords are inactive and the new context-sensitive chord preserves XR. Evidence: build/runs/20260927-165350-665/. No game was launched or installed files changed.


## User report: physical arm tracking

The user reports that an acquaintance confirmed controller motion tracking in the preceding prototype. Reported issues were the controller grip aligning at the wrist rather than inside the hand, and hidden held items. The report does not specify the headset/controller model or establish measured latency or comfort.

## Version 0.6.0 palm alignment and held attachments

Grip targets now position an estimated palm center, with wrist position derived from the calibrated hand orientation and a cached hand-local offset. The offset is halfway from wrist to the average directly parented, non-thumb finger bases. If unavailable or implausible, the fallback is 15% of forearm length along the native forearm direction. Rotation therefore pivots around the palm; reach clamping and independent tracking fallback remain. This anatomical estimate still needs visual fitting on physical controllers.

Held attachments are no longer hidden. The retargeter exposes per-bone pose deltas, including right/left primary/secondary Prop1/Prop2 aliases. Named parent attachment metadata selects the corresponding bone. A scene-space bone delta updates the captured static item transform through each part's prepared world transform, preserving existing item offsets, mesh transforms and scale. Captured nested static attachments inherit the same delta exactly once. Untracked/unrelated items remain native. No live animation player, original palette contents, native attachment matrix, or item world matrix is modified.

A fourth version-gated transform targets Renderer.a(Part, Matrix4f, float, float, float, float), substituting the attachment matrix only at its existing upload read. AttachmentPoses owns copied matrices in a render-thread scope. Both eyes use the same overrides, and finally removes the scope on success/failure. The actual binary has exactly one matching upload read; all four transformed classes verify and retransform without initialization or executing game/mod entry points.

Validation:

- Full harness suite: 23 capture, 19 mirror, 28 XR lifecycle, 42 camera, 25 OpenGL and 13 timing checks passed, plus actual four-class retransformation and attachment field/accessor checks. Evidence: build/runs/20260927-175159-352/.
- Final targeted arm/attachment suite: 388 checks passed, including palm pivot/rotation, fingerless fallback, right/left items, named attachment precedence, retained native grip/scale, distinct part world transforms, nested items before parents in draw order, tracking loss per hand, untouched native matrices, and rollback for invalid attachment graphs. Evidence: build/runs/20260927-175159-352/arm-tracking-final.log.
- Adapter suite: 63 checks passed, including the actual transformed upload path in original fixtures, identical attachment matrices for both eyes, and normal/second-eye-failure cleanup. Evidence: ../pz3d-adapter/build/test-results.txt.

The OpenXR backend/action code is unchanged from the previously tested build; no additional native runtime run was needed. The user must test the new palm fit and held-item appearance in-game. Two-handed weapons still follow their native owning hand; no support-hand constraint, motion combat, changed projectiles, flashlight direction, or other world interaction is implemented. PZ3D's existing attachment capture and scoped visibility rules remain. Shadows retain the native pose. No game was launched, no installed files changed, and no 0.6.0 release was published.

## Version 0.7.0 motion-triggered armed melee prototype

Off by default, with Diagnostics and Live modes in Mods settings. A released-then-held right trigger and deliberate translational controller swing produce one short-lived request. The simulation-thread adapter calls native AttemptAttack directly, reads PZ3D's pending/active native request fields to avoid conflicting input, and retains native combat processing. VR-owned attack selection and animation callbacks reject unarmed/floor fallbacks. Tracking, focus, menus, equipment, readiness, and request freshness gate attacks; collision events are logged separately from damage success. Physical weapon contact and motion-scaled damage are not implemented.

Workspace validation on 2026-09-27:

- Full harness regression run passed: 23 capture, 19 mirror, 28 XR lifecycle, 25 real OpenGL capture, 47 camera, 13 timing, 21 hotkey, 11 countdown, and 982 arm checks. Gesture/mailbox tests passed 92 checks. Evidence: `build/runs/20260927-192600-501/`.
- Final native-adapter fixture run passed 47 checks, including stale input, duplicate collision events, native request conflicts, equipment changes, diagnostic mode, and shove/floor rejection. Fixtures exercise production adapter logic with original synthetic game facades; they do not execute actual combat. Evidence: `melee-runtime-final.log` in that run.
- Four render classes and four additional melee classes from the pinned copied binaries retransform and verify without initialization. Final evidence: `real-binary-final.log`. Sixteen additional transformation checks cover original class shapes, duplicate-hook rejection, missing-method rejection, and composition with inserted NOP instructions. This checks additive transformation mechanics, not live coexistence with every PZ3D/ZombieBuddy patch. Evidence: `melee-transform.log`.
- Lua 5.1 settings tests passed against the installed ModOptions implementation with in-memory game I/O, including melee defaults, Apply, and persistence. No real settings were written.
- Real packaged OpenXR backend passed 1,251 checks across two sessions against isolated simulated SteamVR. Controller action creation, attachment, and synchronization succeeded with no controllers present. Evidence: `build/xr-runs/20260927-192653-094-xr/`. No physical trigger gestures were tested.

No game or installed mod entry point was launched, and no installed mod files were changed. Actual attack start/damage, native patch coexistence, controller bindings on physical devices, gesture thresholds, and attack feel remain unverified in-game. Local package: `dist/PZ3DVRTest-0.7.0.zip`; this validation does not constitute a published release.

## Version 0.8.0 internal OpenXR gamepad bridge

Opt-in standard Touch actions feed an immutable normalized input record, a separate conventional gamepad mapper, and a native controller polling adapter. Three version-gated classes are retransformed without schema changes. The controller constructor gets a synthetic branch restricted to bridge-owned creation; a registry overlay reserves slot 15 and preserves the physical registry. Existing ControllerStateCache and Input handle publication and edges. Player assignment/activation stays native. Off/focus loss/session stop publish neutral state while retaining identity. Physical collision drains neutral input, delivers native disconnect, performs scoped assignment cleanup and returns the slot. Gamepad mode excludes motion-melee input.

Validation on 2026-09-27:

- Full harness regression suite passed, including existing stereo, XR lifecycle, OpenGL, camera, timing, hotkey, countdown, 982 arm and 92 melee detector checks. Evidence: `build/runs/20260927-202938-629/`.
- Four render, four melee, and three new controller classes from copied pinned binaries retransform with JVM verification, without class initialization or game execution. Evidence: `real-binary.log` in that run.
- 34 pure mapper checks passed: button/axis/trigger conventions, grip hysteresis, menu/D-pad/Back layer, one Start pulse, invalid/nonfinite/stale input, focus loss and neutral rearming. Evidence: `gamepad-mapping.log`.
- Final controller fixtures passed 23 checks using production instrumentation/bridge against original synthetic game classes, including a poll/publish/consume double buffer model and native-style button edges. Covers synthetic metadata, physical construction/polling preservation, duplicate-edge prevention, neutral release, resume, retained identity while Off, physical hotplug handback, assignment cleanup, shared-trigger exclusion, and a detach race that must keep the last virtual polling buffer neutral. Evidence: `build/controller-lifecycle.log`. This is not execution of actual game input code.
- Lua 5.1 checks passed using native ModOptions.lua and mocked I/O, including mode persistence and synthetic-only disconnect UI cleanup. No user settings were written.
- Real packaged backend passed 1,251 checks across two isolated simulated SteamVR session lifetimes with the expanded action set. Action creation, Touch binding suggestions, attachment and state queries produced no input errors; there were no physical controllers. Evidence: `build/xr-runs/20260927-203126-296-xr/`. The later change was confined to synthetic buffer handback/diagnostic metadata and did not change OpenXR actions.

No game or existing mod entrypoint was launched, no drivers were installed, and no installed files were changed. Actual Quest/Steam Link profile availability, menu delivery, native controller UI activation, gameplay/menu behavior, game-thread scheduling, and hotplug coexistence require user testing. Simulated runtime and fixture checks do not prove those behaviors. Local package: `dist/PZ3DVRTest-0.8.0.zip`; not published to GitHub.

## Version 0.8.1 optional hybrid gamepad / motion melee

Adds `Allow motion melee with gamepad input` in Mods settings, unchecked by default for existing behavior. The requested melee mode is retained independently of gamepad mode. With hybrid enabled and melee set to Diagnostics or Live, motion melee receives the right trigger and the emulated native RT axis stays at -1. Other gamepad controls remain available. Melee Off restores native RT even with hybrid checked. This is global trigger ownership, including menus and firearms; automatic weapon-specific switching is not implemented. Changing RT ownership invalidates the gamepad mapper and requires neutral controls before rearming. Settings changes clear pending melee requests/permits.

The targeted production bridge/fixture suite passed 31 checks, including eight hybrid-control regressions for retained movement/buttons, native RT exclusion, chosen-mode retention, safe routing changes, diagnostic mode and RT restoration. Lua tests passed checkbox defaults, Apply and save/load persistence with mocked I/O. Build and package validation passed. OpenXR acquisition and bytecode hooks are unchanged from 0.8.0; no additional native smoke test was needed. No game launched or installed files modified. Physical hybrid-control behavior remains for the user to test. Local package: `dist/PZ3DVRTest-0.8.1.zip`.
