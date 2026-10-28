# Project Viewpoint VR: integration baseline

2026-09-30. Forked from PZ3D VR commit `161fc81b37f8cb7727dd495337cd15a5a8a04b43` (`pz3d-vr-baseline`). Viewpoint is the sole future renderer target. PZ3D compatibility is not a requirement. Existing sources remain available for selective reuse; they are not part of the new baseline build.

## Evidence and scope

The installed Viewpoint 0.1.6.1 and Zomboid 42.21.0 JARs were inspected with JDK bytecode tools and Vineflower. Exact SHA-256 pins are in [pins.json](../viewpoint/pins.json). Decompiled implementations are evidence only, not source to copy into this project. Generated disassembly and proprietary copies are local ignored build inputs. No game/mod entry point was run. Findings below describe the pinned binary, not stable public APIs.

The 0.4.1 one-shot diagnostic produced two complete in-game stereo captures during the user's 2026-09-30 test. Version 0.5.0 implements continuous desktop stereo, OpenXR submission, independent head tracking and recentering. The user subsequently confirmed continuous desktop and null-headset tests without issues; physical-headset testing was initially deferred. The user has now confirmed physical stereo/head tracking with Steam Frame on 0.9.0. Version 0.6.1 ported the UI/stereo implementation to 0.1.5a-hotfix; the user subsequently confirmed the updated renderer and UI work. Maintain the latest Viewpoint target only. See the current [test guide](../viewpoint/TESTING.md).

## The boundary that must be implemented

`FP.renderWorld` snapshots into one of three frames and enqueues a `SceneDrawer` using SpriteRenderer. `SceneDrawer.drawFrame` consumes that captured scene. Unlike the PZ3D lease-based path, the inspected Viewpoint path does not supply an equivalent retained frame lease. Both eyes must finish during the same normal scene consumption.

| Existing location | Evidence / required treatment |
|---|---|
| `SceneDrawer.drawFrame` | Reached through a guarded method reference; collects retirement, chooses camera, calls `WorldRenderer.begin`, releases model drawers, draws effects, finishes, then marks frame drawn. Wrap the pair here; do not call this method twice unchanged. |
| `WorldRenderer.begin` | Saves output framebuffer/viewport, ensures targets, resets FrameStream, advances frame index, temporal state and uniforms, updates/uploads scene assets, prepares model draws, then draws world. Separate shared preparation from per-eye state. FrameEnd owns the final post-processing chain. |
| `WorldRenderer.drawWorld` / `FarPass.prepare` | Far preparation mixes asynchronous upload/bake work with eye frusta and projection. Freeze common work without skipping the second eye's frustum. |
| `WorldRenderer.finish` | Resolves/remembers temporal history, presents to caller output and releases model texture resources. Delay pair-owned cleanup; prevent left/right history contamination. |
| `SceneDrawer.postRender` | Releases captured character/model resources. Leave ownership with the normal frame lifecycle; add failure-path tests before interception. |

The executable checker anchors six methods and 23 invocations in this contract. Lexical call order is deliberately distinguished from runtime/control-flow proof. A future transformer must also validate loaded bytes, classloader ownership and modifiability; offline acceptance is not runtime authorization to transform unknown classes.

## Stereo correctness work before an in-game package

1. Establish a scoped pair lifecycle: one snapshot, one common preparation, two eye draws, one cleanup. Test second-eye failure, skipped frames and restored output state with synthetic fixtures and copied-bytecode verification without initialization.
2. Freeze temporal index, clock and upload readiness for the pair. Initially disable TAA, motion blur and shader-pack history features in a scoped diagnostic configuration. TAA off alone is insufficient: `TemporalPass.remember` still runs and `PackStages` maintains its own previous textures. Do not edit the user's saved graphics properties.
3. Broaden producer visibility conservatively: room/chunk cones, character filtering and model culling use desktop look/FOV before render time. Per-eye view matrices cannot recover objects missing from the snapshot.
4. Handle asymmetric projection consistently. Sky/cloud ray construction uses reciprocal projection diagonals without off-axis offsets; `camera.glsl` view-position reconstruction also assumes symmetry. Disable affected effects in the initial diagnostic or correct those paths before calling it headset-ready. Keep near/far at 0.05/400 until all depth consumers are audited.
5. Render into two equally sized owned color FBOs. Final tone-mapped output goes to the caller framebuffer; `WorldRenderer.targetFbo()` is an HDR intermediate. Capture images and a side-by-side desktop mirror before adding XR.

The camera implementation starts from Viewpoint's scene-space eye anchor. World displacement maps to `(-dx, sqrt(6)*dz, -dy)`. Yaw zero faces -X. XR and Viewpoint are both right-handed Y-up, unlike the old PZ3D camera conversion. Explicit base-yaw alignment and physical scale calibration remain necessary. Avoid adding native head bob/forward offset twice. The matrix tests establish conventions, not comfortable in-game scale or collision behavior.

## Reuse after desktop stereo

Reuse the OpenXR WGL session lifecycle, frame timing, swapchain copy and quad-layer concepts from `experiments/zombiebuddy-harness/src/pzvr/xr`; extract them only when the new adapter needs them. The existing session acquires images at copy time, so owned output FBOs fit its current contract. Separate pose types and controller/gameplay coupling during that extraction. Reuse GL capture/mirror utilities selectively after removing `PairHooks.Sink` dependencies.

Version 0.6.0 captures vanilla UI between its Lua draw events, captures Viewpoint loot/performance passes separately, and redirects the ImGui draw-data submission. The completed layers form one transparent OpenXR quad. See [UI.md](../viewpoint/UI.md) for boundaries, lifetime and limitations.

Arm IK/calibration math may carry over later. Viewpoint uses packed 12-float bone palettes with distinct visible/shadow/previous records, so the old 16-float palette adapter cannot. Controller mapping has reusable parts, but movement/look, melee dispatch and hand interaction need Viewpoint-specific integration. Third person, freecam, seated vehicle VR and multiplayer are outside the first runtime milestone.

## Acceptance sequence

Offline contract/camera tests, native pair lifecycle, real GL capture and loader activation are complete. The first in-game capture milestone is confirmed. Continuous desktop -> fixed XR -> tracked XR/recenter are now implemented in one package at the user's request, with separate controls for live isolation. The user confirmed desktop and null-headset tests and authorized proceeding with UI while physical-headset testing was deferred. The user now requires tracking the latest available Viewpoint without maintaining old-version compatibility. Version 0.9.0 replaces the 0.1.5a-hotfix target with installed 0.1.6.1; future upstream changes require an audit of affected contracts.


## Controller milestone 0.7.0

OpenXR aim/grip acquisition, right-hand UI pointing/clicking/scrolling and optional left-stick walking/right-stick snap turning are implemented. These reuse acquisition patterns while adapting input at pinned Viewpoint call sites. The user authorized this next step after confirming the updated renderer and UI work. Simulation and standalone GL validation do not establish physical controller bindings, comfort or game-side interaction success. See [INPUT.md](../viewpoint/INPUT.md). Hand models, weapons and direct world interactions remain future work.


## Compatibility audit 0.9.0 (2026-10-27)

Installed Zomboid 42.21.0 is byte-identical to the prior game JAR. Viewpoint 0.1.6.1 and ZombieBuddy 2.3.4 require new exact pins. The scene entry now constructs a Runnable inside a native guarded boundary; FrameEnd owns post-processing, SkyPass owns weather-map drawing, FarGpu consolidates uploads, and CameraShot owns seated/third-person snapshot flags. MouseButtons.pin replaces Controls.pinMouse, and crash-report windows share the ImGui input route. See [compatibility review](../viewpoint/COMPATIBILITY.md). The user subsequently confirmed 0.9.0 physical stereo/head tracking with Steam Frame, and reported body/controller gameplay gaps.

## Gameplay prototype 0.10.0

User now owns Steam Frame. Start SteamVR before selecting XR; manual startup fixed the reported create-instance -2 error. Physical stereo/head tracking/room-scale visual motion are user-confirmed on 0.9.0, while native body/controller integration was absent. Read-only log review found Controllers Off throughout, right controller tracking and a startup null mouse-array exception.

Version 0.10.0 adds an explicit VR gameplay entry point, optional Frame extension/profile bindings, native controller slot-15 overlay, head-aligned movement/body/aim consumers and bounded horizontal actor following. It reuses the earlier authored gamepad mapper/registry approach without PZ3D runtime dependencies. Twenty native classes activate atomically, including seven new gameplay/gamepad targets. Native renderer base Look values remain unchanged. Room following consumes collision-resolved actor movement rather than teleporting or applying physical displacement twice. See [INPUT.md](../viewpoint/INPUT.md) for mapping/ownership and [TESTING.md](../viewpoint/TESTING.md) for assignment and first-test sequence.

This is a first gameplay prototype, pending live testing. Native world picking is still based on Viewpoint's ordinary camera, so angular head alignment is not a tracked-hand interaction ray. The next milestone is tracked arm/held-item adaptation and consistent world targeting. ModelCapture receives 16-float native palettes, then ModelDraws/ModelPass pack 12-float render palettes with motion, visibility/mask and attachment records; adapt the old IK math at an owned pose boundary rather than copying PZ3D renderer hooks. No arm or damage validation is claimed.
