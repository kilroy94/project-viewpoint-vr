# Viewpoint baseline validation

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
