# Viewpoint desktop capture diagnostic 0.4.0

This is an **installable manual-test diagnostic**, not playable VR. A real GL output provider and version-pinned ZombieBuddy entry point now connect the native-stage adapter to one-shot PNG capture. Read [the installation and capture guide](TESTING.md). No in-game result or headset support is claimed.

Requirements: Python 3, JDK 25, and separately obtained pinned Viewpoint 0.1.3 / Project Zomboid 42.21.0 / ZombieBuddy 2.3.2 binaries. No proprietary binaries are included.

```powershell
./viewpoint/Build.ps1 -JavaHome 'path/to/jdk-25' -GameJar 'path/to/projectzomboid.jar' -ViewpointJar 'path/to/Viewpoint.jar' -ZombieBuddyJar 'path/to/ZombieBuddy.jar' -LwjglLibDirectory 'path/to/lwjgl-3.4.1-libraries'
```

The script copies verified inputs into ignored `viewpoint/build/inputs`, runs JDK `javap`, compiles only this project's sources/tests, and writes `viewpoint/build/contract-report.json`. Each run has fresh compiler output under `viewpoint/build/runs/<id>`. Copied-binary tests define/retransform SceneDrawer, WorldRenderer, FarPass and TemporalPass without initialization; checked JVM class-initialization logs record that boundary. It does not launch Zomboid, install a mod, or change runtime/user settings. Hash or contract mismatch is a hard failure. Accepted loader hashes include the original 2.3.2 and the previously audited B42.21 temporary fix.

`Build.ps1` runs `Test.ps1` then produces `dist/ProjectViewpointVR-0.4.0.zip` and its SHA-256 file. The build requires LWJGL 3.4.1 core/GLFW/OpenGL JARs and their Windows native JARs to run a standalone **hidden** GPU test. Existing copies from the inherited project's diagnostic library directory can be reused. No OpenXR JAR or SteamVR runtime is loaded. `Test.ps1` without `-LwjglLibDirectory` runs only non-GPU checks; such a run cannot be packaged. If local PowerShell script policy blocks execution, invoke the script with `powershell -NoProfile -ExecutionPolicy Bypass -File ...`; this affects that process only.

The output includes only this project's production classes/Lua/instructions, not fixtures, the test agent, dependencies or proprietary classes. Install the ZIP's mod folder, not the intermediate core JAR. ASM is supplied by the separately obtained ZombieBuddy JAR. The runtime verifies exact game/Viewpoint/loader hashes, class ownership and hook visibility, then activates all four targets together. Failure rolls the transform back; a later incompatible transform disarms capture. Main is never called by the test suite.

See [the render boundary](RENDER-BOUNDARY.md) for the entry transform and cleanup contracts. After installation, the default dispatcher delegates ordinary rendering until a request arrives. Ctrl+Shift+F10 queues one capture on a rising key chord; duplicate requests coalesce, requests expire, and a render failure disarms further captures until restart. Per-thread scoped drivers remain available for isolated tests.

See [native stages](NATIVE-STAGES.md) for the backend, 29 native-stage anchors and diagnostic effect policy. This diagnostic supports a symmetric synthetic first-person camera only. Third person, freecam and seated modes take the ordinary native path; multiplayer is rejected at startup and guarded at render time.

`StereoCamera` accepts a scene-space center eye and camera-to-scene quaternion, in a right-handed Y-up system looking down local -Z. Synthetic eyes use a configurable IPD in scene units; physical meter-to-scene calibration remains unvalidated. Asymmetric FOV values are signed tangents, not degrees or radians. Its OpenGL projection retains Viewpoint's 0.05/400 clipping planes. Output matrices/vectors are independent, caller-owned values.

`inspect_contract.py` verifies selected method descriptors and lexical invocation order/counts. These are regression anchors for the audited binary, not a control-flow proof, bytecode transformer, or demonstration of runtime safety. See [the integration plan](../docs/VIEWPOINT-PLAN.md) for the remaining renderer work.
