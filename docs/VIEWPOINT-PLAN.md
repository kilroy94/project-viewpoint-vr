# Project Viewpoint VR: integration baseline

2026-09-30. Forked from PZ3D VR commit `161fc81b37f8cb7727dd495337cd15a5a8a04b43` (`pz3d-vr-baseline`). Viewpoint is the sole future renderer target. PZ3D compatibility is not a requirement. Existing sources remain available for selective reuse; they are not part of the new baseline build.

## Evidence and scope

The installed Viewpoint 0.1.3 and Zomboid 42.21.0 JARs were inspected with JDK bytecode tools and Vineflower. Exact SHA-256 pins are in [pins.json](../viewpoint/pins.json). Decompiled implementations are evidence only, not source to copy into this project. Generated disassembly and proprietary copies are local ignored build inputs. No game/mod entry point was run. Findings below describe the pinned binary, not stable public APIs.

The first delivered implementation is an offline compatibility checker plus pure camera math. It is not a mod installer, runtime transformer, stereo renderer or OpenXR integration. The next runtime target is synthetic desktop stereo in first-person, on-foot, single-player mode.

## The boundary that must be implemented

`FP.renderWorld` snapshots into one of three frames and enqueues a `SceneDrawer` using SpriteRenderer. `SceneDrawer.drawFrame` consumes that captured scene. Unlike the PZ3D lease-based path, the inspected Viewpoint path does not supply an equivalent retained frame lease. Both eyes must finish during the same normal scene consumption.

| Existing location | Evidence / required treatment |
|---|---|
| `SceneDrawer.drawFrame` | Collects retirement, chooses camera, calls `WorldRenderer.begin`, releases model drawers, draws effects, finishes, then marks frame drawn. Wrap the pair here; do not call this method twice unchanged. |
| `WorldRenderer.begin` | Saves output framebuffer/viewport, ensures targets, resets FrameStream, advances frame index, temporal state and uniforms, updates/uploads scene assets, prepares model draws, then draws world. Separate shared preparation from per-eye state. |
| `WorldRenderer.drawWorld` / `FarPass.prepare` | Far preparation mixes asynchronous upload/bake work with eye frusta and projection. Freeze common work without skipping the second eye's frustum. |
| `WorldRenderer.finish` | Resolves/remembers temporal history, presents to caller output and releases model texture resources. Delay pair-owned cleanup; prevent left/right history contamination. |
| `SceneDrawer.postRender` | Releases captured character/model resources. Leave ownership with the normal frame lifecycle; add failure-path tests before interception. |

The executable checker anchors five methods and 21 invocations in this contract. Lexical call order is deliberately distinguished from runtime/control-flow proof. A future transformer must also validate loaded bytes, classloader ownership and modifiability; offline acceptance is not runtime authorization to transform unknown classes.

## Stereo correctness work before an in-game package

1. Establish a scoped pair lifecycle: one snapshot, one common preparation, two eye draws, one cleanup. Test second-eye failure, skipped frames and restored output state with synthetic fixtures and copied-bytecode verification without initialization.
2. Freeze temporal index, clock and upload readiness for the pair. Initially disable TAA, motion blur and shader-pack history features in a scoped diagnostic configuration. TAA off alone is insufficient: `TemporalPass.remember` still runs and `PackStages` maintains its own previous textures. Do not edit the user's saved graphics properties.
3. Broaden producer visibility conservatively: room/chunk cones, character filtering and model culling use desktop look/FOV before render time. Per-eye view matrices cannot recover objects missing from the snapshot.
4. Handle asymmetric projection consistently. Sky/cloud ray construction uses reciprocal projection diagonals without off-axis offsets; `camera.glsl` view-position reconstruction also assumes symmetry. Disable affected effects in the initial diagnostic or correct those paths before calling it headset-ready. Keep near/far at 0.05/400 until all depth consumers are audited.
5. Render into two equally sized owned color FBOs. Final tone-mapped output goes to the caller framebuffer; `WorldRenderer.targetFbo()` is an HDR intermediate. Capture images and a side-by-side desktop mirror before adding XR.

The camera implementation starts from Viewpoint's scene-space eye anchor. World displacement maps to `(-dx, sqrt(6)*dz, -dy)`. Yaw zero faces -X. XR and Viewpoint are both right-handed Y-up, unlike the old PZ3D camera conversion. Explicit base-yaw alignment and physical scale calibration remain necessary. Avoid adding native head bob/forward offset twice. The matrix tests establish conventions, not comfortable in-game scale or collision behavior.

## Reuse after desktop stereo

Reuse the OpenXR WGL session lifecycle, frame timing, swapchain copy and quad-layer concepts from `experiments/zombiebuddy-harness/src/pzvr/xr`; extract them only when the new adapter needs them. The existing session acquires images at copy time, so owned output FBOs fit its current contract. Separate pose types and controller/gameplay coupling during that extraction. Reuse GL capture/mirror utilities selectively after removing `PairHooks.Sink` dependencies.

Vanilla UI can use the completed `UIManager.uiFbo` texture; Viewpoint settings, loot/performance overlays and ImGui render later outside it and require separate capture work. Do not claim all UI is captured with the vanilla texture alone.

Arm IK/calibration math may carry over later. Viewpoint uses packed 12-float bone palettes with distinct visible/shadow/previous records, so the old 16-float palette adapter cannot. Controller mapping has reusable parts, but movement/look, melee dispatch and hand interaction need Viewpoint-specific integration. Third person, freecam, seated vehicle VR and multiplayer are outside the first runtime milestone.

## Acceptance sequence

Offline contract and camera tests -> lifecycle/transform verification -> user-installed synthetic stereo capture -> user-confirmed eye consistency and visibility -> OpenXR/head pose/recenter -> UI -> hands and gameplay. Each step must preserve a clear distinction between automated evidence and user live validation.
