# Viewpoint baseline validation

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
