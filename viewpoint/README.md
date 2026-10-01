# Project Viewpoint VR runtime 0.6.1

Read [TESTING.md](TESTING.md) for installation, mode controls, runtime setup, scale/recenter and limitations. The user's two 0.4.1 captures confirm the one-shot in-game path. The user also confirmed continuous desktop and null-headset tests. Physical-headset testing is deferred; new UI live validation is pending. This build targets only Viewpoint 0.1.5a-hotfix. Development tracks the latest available renderer; previous Viewpoint versions are not retained as compatibility targets.

```powershell
./viewpoint/Build.ps1 -JavaHome 'path/to/jdk-25' -GameJar 'path/to/projectzomboid.jar' -ViewpointJar 'path/to/Viewpoint.jar' -ZombieBuddyJar 'path/to/ZombieBuddy.jar' -LwjglLibDirectory 'path/to/lwjgl-3.4.1-libraries'
```

The build requires Python 3, JDK 25 and the pinned separately obtained game/mod JARs. Supply LWJGL 3.4.1 core, GLFW, OpenGL and OpenXR JARs plus their Windows native JARs. Existing libraries under the original project's `experiments/openxr-diagnostic/lib` can be reused. OpenXR bindings/native hashes are checked before compilation and packaging; only the OpenXR bindings/loader are embedded. Game, Viewpoint, ZombieBuddy, LWJGL core/OpenGL, test agents and fixtures are not distributed. Licenses are included.

The required build checks cover camera math, pair ownership/cleanup, mode transitions/recenter requests, copied native transforms without initialization, hidden standalone GPU capture/mirror/resize, and the real OpenXR loader's missing-runtime path. That last test sets `XR_RUNTIME_JSON` to a nonexistent workspace path for its child process and restores the environment afterward: it cannot start SteamVR. No game/mod entry point is executed by the build. Inputs, logs, compiler output and generated ZIP/checksum stay under ignored `build`/`dist`.

Eleven exact-pinned native classes are activated together: SceneDrawer, WorldRenderer, FarPass, TemporalPass, ChunkWalk, Characters, ModelCull, Rooms, CorpseView, Hooks and ImGuiFrame. Activation failure rolls all targets back; incompatible retransformation disarms the runtime. See [NATIVE-STAGES.md](NATIVE-STAGES.md) and [VALIDATION.md](VALIDATION.md).

`RuntimeDriver` processes cross-thread requests on the native render thread. `NativePipeline` lazily creates reusable eye targets and an OpenXR WGL session borrowing the game's context. The UI queues one bounded render-thread maintenance callback to pump UI-only XR frames when no eligible world scene is drawn and to process Off/cleanup. Only a complete pair is mirrored/submitted; partial-render failure never replays the native scene. Ordinary frames and unsupported camera modes use the original draw.

`XrCamera` uses runtime eye poses, a yaw-only recenter, native body yaw, and adjustable uniform meters-to-scene scale. It renders symmetric frusta enclosing each runtime FOV, then the XR copy crops the asymmetric region. This avoids changing native sky/depth shaders. Eye orientation/position remain independent, including canted eye poses; no vertical-axis reflection from the old PZ3D camera is carried over. Physical scale and color must be validated with the headset.

The [UI implementation](UI.md) combines game UI, Viewpoint loot/performance overlays and ImGui settings into a head-relative quad submitted alongside the world. It retains desktop input and adds a cursor marker. Controller/hand/gameplay features from the inherited project remain outside this build. The inherited experiments and research remain historical references; do not use their installers for this project.
