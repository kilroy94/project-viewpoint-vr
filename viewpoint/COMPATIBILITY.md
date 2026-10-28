# Compatibility review: Project Viewpoint VR 0.9.0

This records the renderer compatibility port. Current 0.10.0 keeps these exact binary targets and adds gameplay hooks; the user has since confirmed physical stereo/head tracking on 0.9.0. See [current gameplay scope](INPUT.md) and [validation](VALIDATION.md).

Audited 2026-10-27 against the installed binaries, with changes built in the isolated workspace. No game/SteamVR launch, installer execution, Workshop writes or Zomboid user-data writes were performed.

| Component | Installed target | Review result |
|---|---|---|
| Project Zomboid | 42.21.0, public/stable branch | Game JAR is byte-identical to the prior audited input; no game API port required. |
| Project Viewpoint | 0.1.6.1, built 2026-10-16 | Renderer entry, post-processing, camera snapshot and input adapters updated. |
| ZombieBuddy | 2.3.4 | Loader/approval changes audited; instrumentation field and copied-class activation verified. Exact pin/descriptor updated. |
| Installed Project Viewpoint VR | 0.6.1 | Installed descriptor is older than source; install the new package manually. |
| Project source before review | 0.8.0, commit 36a416d | Stereo/XR, UI, controller pointer/walking/turning, persistence and logged diagnostics were implemented. |
| Updated source/package | 0.9.0 | Current compatibility port, pending first live test. |

The developer lists [42.21 as stable and 42.22 as unstable](https://projectzomboid.com/blog/news/). The [Viewpoint change notes](https://steamcommunity.com/sharedfiles/filedetails/changelog/3809306528) list 0.1.6.1 as the latest release, including its ImGui memory fix. Installed descriptors/build metadata and binary hashes are authoritative for this adapter; no attempt was made to upgrade the game to the unstable branch. No controller diagnostic reports were present at the default cache path during this read-only review. Earlier user-confirmed desktop/null-headset/UI results cover previous Viewpoint targets, not this port.

## Adaptation

- SceneDrawer now constructs `this::drawFrame` inside its native guarded helper. The transform replaces that method-reference construction while retaining the existing failure/GL restoration boundary and single-use native callback rules.
- FrameEnd owns post-processing. Both shader-pack calls are bypassed during the pair; temporal history writes are suppressed in FrameEnd and TemporalPass. Model resources remain owned until both eye copies complete, including failure cleanup.
- SkyPass owns weather-map rendering; it runs once per pair while atmospheric view work remains per eye. FarGpu.uploadQueued replaces the split upload calls. FarRelief frame preparation and developer polling run once.
- CameraShot owns seated and third-person snapshot flags. Current View.thirdPerson and snapshot flags reject unsupported views. Native Camera.eye retains lean/terrain placement. Panini strength is zero only while a stereo snapshot is borrowed, then restored if that same snapshot is still owned.
- MouseButtons.pin replaces Controls.pinMouse. Settings/onboarding/crash windows share the ImGui route; crash popups block movement/turning and stop a scripted diagnostic. Native wheel forwarding and ImGui draw-data freeing remain in their original native order; captured draw data is consumed synchronously.
- Installed ZombieBuddy 2.3.4 retains the loader instrumentation bridge and Lua exposure types. Its merged common/version descriptor loading accepts this mod's existing descriptor layout. Java mod approval remains under the loader's normal policy.

Thirteen exact-pinned classes activate/roll back together: SceneDrawer; WorldRenderer, FarPass, TemporalPass, FrameEnd and SkyPass; ChunkWalk, Characters, ModelCull, Rooms and CorpseView; Hooks and ImGuiFrame. The adapter checks 33 stage call/return anchors, with normalized executable/schema comparisons. Only the current installed Viewpoint and ZombieBuddy binaries are accepted; older compatibility targets were retired.

## Runtime limits and first test

**Disable Viewpoint frame generation before enabling stereo/XR.** An enabled setting suspends the adapter and leaves native rendering active. Midframe swaps and frame-generation capture are suppressed inside an owned pair. Vendor temporal upscalers (FSR 3/XeSS/DLSS) are inactive during stereo because scoped TAA-off short-circuits their native selection; ordinary spatial resizing is retained. Panini is inactive for stereo eye projection. User graphics preferences are not rewritten. Iris packs, third person, vehicles and multiplayer remain outside the VR path.

Install `ProjectViewpointVR-0.9.0.zip` with the game closed and approve the new JAR normally if prompted. Repeat desktop stereo, Off/cleanup, null-headset XR/UI, settings wheel/drag and the two controller diagnostics in [TESTING.md](TESTING.md) and [DIAGNOSTIC.md](DIAGNOSTIC.md). Verify that disabling stereo restores native rendering/settings. The newer gunplay, free-look and temporal graphics features have not been gameplay-tested with VR; controller weapon actions and tracked hands remain unimplemented. Physical bindings, scale, comfort and live native input/render coupling remain acceptance tests.

## Automated evidence

Full Build.ps1 passed in run `18f4c09a03094859acdd05733a48136d`: 272 transformed-stage fixture checks, 33 guarded-entry fixture checks, 55 native-input integration checks, 17 activation/rollback/loader checks, and the complete existing camera, lifecycle, runtime, UI, controller, turning, persistence, diagnostic, Lua syntax and Python contract suite. Thirteen actual copied targets verified/retransformed without game/mod initialization. Hidden GL passed UI composition (59), output/capture (32) and beam (15) checks. OpenXR tested only its expected missing-runtime rejection, without a runtime launch.

The updated fixtures cover Panini restoration on normal/error/recycled frames; frame-generation rejection and changes mid-pair; seated/third-person rejection; consolidated uploads and shared weather work; guarded Throwable propagation; and crash-popup routing/locomotion suppression. Fixture results establish isolated behavior, not a successful live game run.

Package: `dist/ProjectViewpointVR-0.9.0.zip`; SHA-256 `a0839a167663ae2a4c457ae175d9ba75e6a6072b92d791402ef42cda14328aa4`. No game, Viewpoint, ZombieBuddy, Vulkan or test-fixture classes are distributed. Exact installed hashes are recorded in [pins.json](pins.json).
