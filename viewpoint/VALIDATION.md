# Viewpoint baseline validation

2026-09-30, baseline 0.1.0, Windows x64 / JDK 25.

`Test.ps1` passed against separately obtained Project Zomboid 42.21.0, Viewpoint 0.1.3 and ZombieBuddy 2.3.2 with the audited B42.21 temporary fix. Exact input hashes are in `pins.json`; the generated report records which accepted loader was used.

- Three Python test cases passed, including missing/duplicate/wrong-descriptor methods, missing/duplicate/reordered invocations and unknown-hash rejection before copying.
- Actual copied Viewpoint bytecode passed five method contracts with 21 invocation anchors. The tool ran `javap`, not game/mod classes.
- Camera code compiled against the game's bundled JOML API. All 256 numeric/input checks passed: Viewpoint yaw/pitch conventions, eye separation and midpoint, inverse camera transforms, rolled eye offsets, input nonmutation, asymmetric near-plane corners, far depth, world-axis mapping and invalid input rejection.

No OpenGL context, Viewpoint rendering, headset session, game launch, installation or user-data modification was performed. Stereo visibility, shared preparation, temporal state, failure cleanup, physical scale and performance remain unvalidated. There is no installable Viewpoint VR package at this milestone.

Historical PZ3D test results apply only to the inherited implementation. They are not evidence of Viewpoint compatibility. Run the new `viewpoint/Test.ps1` entry point rather than the inherited harness installer/build.
