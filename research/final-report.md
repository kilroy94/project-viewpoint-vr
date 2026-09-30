# PZ3D VR feasibility report

**Historical research snapshot, not current implementation status.** The authorized implementation has since progressed to a 0.11.0 PZ3D VR baseline. Read [the current implementation map](../docs/PZ3D-BASELINE.md) and [validation record](../experiments/zombiebuddy-harness/VALIDATION.md) for implemented features and remaining evidence gaps. Statements below about missing integration and required next-phase approval describe the original research phase only.

**Conclusion: an OpenXR proof-of-concept is technically plausible with the installed PZ3D 0.2.2 architecture. It is not currently a safe two-eye renderer, and acceptable VR performance remains unproven.** The most useful next step is a narrowly scoped, version-pinned compatibility add-on with a deliberate once-per-frame preparation / per-eye drawing boundary.

This report is based on copied local binaries and decompilation, supplemented by official OpenXR/LWJGL documentation. Project Zomboid was not launched, and the working installation/mods were not modified. No VR prototype was implemented.

Subsequent work: the user authorized a [standalone OpenXR diagnostic](../experiments/openxr-diagnostic/README.md), now tested with a simulated SteamVR headset. The statement above describes the research phase; game-renderer feasibility remains unproven.

## 1. Is VR technically feasible?

**Likely yes, with renderer adaptation.** PZ3D owns a real perspective OpenGL renderer, captures world/actor data, retains it for independent redraw and produces an RGBA8 framebuffer image. These remove the need to reconstruct 3D or simulate the game independently for each eye. What is missing is a full eye pose/projection interface and a coherent two-eye presentation boundary. [Renderer map](rendering-map.md), [stereo audit](stereo-audit.md).

## 2. Strongest evidence

`Renderer.Frame.render()` calls `RetainedRender.accept(this)` and `draw(true)`. `RetainedRender.repeat(Runnable)` can subsequently call the retained frame's `draw(false)` without another simulation update. `FrameLease` and `PoseData` retain frame/model resources. `ViewMath.projection` creates perspective matrices, while `Renderer.f(int,int)` allocates a color/depth FBO. This is direct installed-code evidence, not an inference from a public source release. [Rendering references](rendering-map.md).

## 3. Largest technical obstacles

`Frame.draw` is not pure: it advances `SceneUpload`, resamples look/motion/vehicles, updates shadows and stream fades, expires tracers and touches resource state. It also assumes one global target, desktop dimensions and a camera with no roll. The independent renderer is paced around 60 Hz. Head look is coupled to body/weapon/aiming, and native UI capture excludes some overlays/cursor behavior. [Hazard-by-hazard audit](stereo-audit.md).

## 4. Can it safely render two views per simulation frame?

**Not by simply calling the current draw method twice.** It already supports repeated mono rendering, but the two calls can adopt different scenes or different presentation state. A safe pair appears achievable by retaining one frame/scene, preparing resources and prediction once, freezing effects/shadows/time, and recomputing only eye-specific matrices, visibility and GPU passes. Runtime proof is still required.

## 5. Exact stereo hook points

Preserve the producer chain `Core.RenderOffScreenBuffer() -> Patches.Render.exit() -> Main.enqueue() -> SpriteRenderer.drawGeneric(new Renderer.Frame(...))`. Intercept the **render-thread consumer** `Renderer.Frame.render()/draw(boolean)` and coordinate the retained path `RetainedRender.repeat`, `begin/end`, and `Patches.IndependentFrame` on `RenderThread.lockStepRenderStep()`.

Within the renderer, separate `SceneUpload.advance` and private `Frame.ad()` from per-eye draws; replace `ViewMath.projection(FFFFFFFF)`, provide per-eye position and target size, reset `WorldBuffers.beginView`, and redirect the final `glBlitFramebuffer` to an acquired eye destination. Merely patching `Camera.view` or duplicating the final color image is insufficient. [Concrete map](rendering-map.md), [170 installed patch targets and descriptors](patch-inventory.md).

## 6. Can eye images be passed to OpenXR?

**Likely yes through the existing OpenGL context.** OpenXR supplies swapchain texture handles; the current arbitrary PZ3D texture cannot itself be registered as a standard swapchain image. The initial approach is a GPU copy/blit or color-conversion draw into acquired runtime-owned textures. Direct rendering into those images is a later target-injection option. Format, gamma, extent, synchronization and GPU compatibility require tests. [OpenGL swapchain contract](https://registry.khronos.org/OpenXR/specs/1.1/man/html/XrSwapchainImageOpenGLKHR.html), [integration details](openxr-head-ui.md).

## 7. How should headset pose integrate?

Keep player movement/body aiming and mouse/gamepad control separate from visual head pose. Anchor a recentered OpenXR reference space to the base eye pivot, convert axes and physical scale, then use each runtime-provided eye pose with its asymmetric FOV and full quaternion orientation, including roll. Do not overwrite `Controller.yaw/pitch` with headset look or apply headset pose twice to already composed eye poses. Start on foot, first person, with limited head translation and explicit recentering. [Pose mapping](openxr-head-ui.md).

## 8. Simplest viable UI?

Reuse `UIManager.uiFbo` / `UiLayer.capture`, capture once after the UI completes, and display it as a floating virtual monitor via an OpenXR quad. Keep normal mouse pixel coordinates and render a visible cursor on the panel. Native transitions and Lua UI events must run once. World captions and the mono crosshair need separate handling. [UI design and capture caveats](openxr-head-ui.md).

## 9. Separate compatibility mod?

**Plausible for a version-pinned prototype; conditional for long-term maintenance.** ZombieBuddy can target PZ3D classes and retransforms loaded classes. Reflection/advice can reach the needed boundaries, but many helpers/fields are internal and the draw method needs multiple interventions. Prefer a separate add-on with small public upstream view/lease/UI hooks and an internal preparation/draw split. No evidence makes a permanent fork unavoidable. [Access and maintenance analysis](prototype-plan.md).

## 10. External dependencies?

Use `lwjgl-openxr:3.4.1` and its `natives-windows` artifact with the existing **LWJGL 3.4.1** core/OpenGL/GLFW and **Java 25.0.1**. The downloaded native artifact contains `openxr-loader.dll`. An installed Windows x64 OpenXR runtime with `XR_KHR_opengl_enable`, headset drivers and a compatible GPU/context is required. Do not replace the game's LWJGL core. A JNI or Java FFM bridge is a fallback, not the preferred first path. No active runtime registration was found in the read-only local check. [Environment](environment.md), [binding research](openxr-head-ui.md).

## 11. What cannot be proven statically?

Native linkage and runtime/session creation, actual headset support, coherent resource lifetime under concurrent simulation, all-scene stereo correctness, input/UI behavior, gamma/orientation, acceptable tracking latency, and sustainable VR frame rate. No benchmark or headset test was performed. Existing code gives a good reason to prototype, not a completed feasibility demonstration on hardware.

## 12. Exact recommended first implementation scope?

A Windows x64, single-player, first-person, on-foot proof for the exact installed versions: OpenXR initialization and clean fallback; deterministic two-camera render; tracked stereo with head rotation and small positional offsets; existing controls; basic UI monitor/cursor; and measured lifecycle/performance checks performed in-game by the user. Exclude motion controllers, new hands, physical melee, VR inventory, room-scale locomotion, third-person VR, vehicle cameras and scope zoom. Begin with a context/session diagnostic, then prove deterministic stereo before integrating tracking. [Milestones and acceptance criteria](prototype-plan.md).

```text
Project Zomboid simulation/UI
        |
ZombieBuddy loader + patch engine
        +---- PZ3D world capture + retained Renderer.Frame
        +---- PZ3D VR compatibility add-on [proposed]
                    |
             PZ3D view adapter / upstream hooks
                    |
             existing GL render thread
             prepare once -> left eye -> right eye
                    |
             OpenXR swapchain images + UI quad
                    |
             LWJGL OpenXR / Khronos loader
                    |
             active OpenXR runtime
             (SteamVR OR another compatible PC runtime)
                    |
                  headset
```

The next phase requires approval. The research workspace contains no VR implementation and no launch/install tasks.
