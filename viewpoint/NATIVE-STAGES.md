# Native stage adapter (0.3.0)

`ViewpointBackend` implements `StereoFrame.Backend` using the pinned renderer's own methods. `StageTransform` edits call sites in WorldRenderer, FarPass and TemporalPass, retaining the original class schema and method implementations. It uses method handles resolved with each native caller's access privileges. Inactive hooks invoke the original calls unchanged. Full-JAR pins and normalized executable comparisons reject unknown or already patched classes before transformation.

The three classes have 29 checked stage anchors: 22 in WorldRenderer (including two finish/runPasses sites), six in FarPass and one in TemporalPass. Together with the previous SceneDrawer entry hook, four native target classes require all-or-nothing verification/activation by a future installer.

## Execution and ownership

The synthetic driver reads the captured frame's eye anchor, yaw/pitch and symmetric projection. It retains the native horizontal 0.12-unit eye offset, creates two cameras with 0..0.064 scene-unit IPD, and keeps a center camera for native hand/flashlight placement. Unsupported projection/camera/gameplay modes delegate the ordinary single native draw. No game-thread snapshot or simulation work is repeated.

The backend saves the borrowed scene projection, native hand/culling state and output state, then opens a thread-owned stage scope. A preparation call to native `WorldRenderer.begin` performs shared work with `drawWorld` suppressed. Subsequent begin calls update native per-eye camera/output state and draw the world, while shared calls and frame-index increments are suppressed. Ordinary outline, translucency, indirect/weather and finish methods follow for each eye.

| Work | Pair behavior |
|---|---|
| Developer/pack polling, frame-stream reset, index, floors/ground, pack/mesh/model preparation | Once during preparation |
| Frame uniforms | Set once; native texture bindings reapplied for each eye |
| Temporal begin | Native matrices recalculated for each camera; one cached clock sample and the same pre-pair timestamp give equal delta time |
| Far shell/cell uploads, band/cell light updates, tree baking | Once, lazily when the first eye reaches far preparation |
| Far projection/frustum/mask processing and world draw | Per eye |
| Near/far shadow scheduling and weather map | Once; prevents scheduler age/tick counters advancing between eyes |
| Native mouse-pick read | Once from the first eye; center/VR targeting is not implemented |
| Model texture release | Registered before native model preparation, deferred until both copies or failure cleanup |
| Captured model drawers / retirement | Released/fenced once at pair end; character snapshots remain owned by normal postRender |

The native frame number, scene identity, hot-reload generation and supported camera mode are checked between stages. A recycled frame is not released or given its old projection back. A failed preparation still releases any registered partial model resources. Every cleanup operation is attempted, and the primary failure is preserved.

The outer SceneDrawer failure handler may call native `restoreOutput` after the backend has already restored GL state. The backend therefore also rewrites FrameContext's saved output framebuffer/scissor/viewport metadata to the actual caller's state. This prevents that outer handler from rebinding the last eye target.

## Diagnostic effects

While a scope is active, native off() treats TAA, indirect lighting, volumetrics and clouds as disabled. Shader-pack post passes are bypassed and FINAL-pass detection returns false. Native temporal history remembering is suppressed; history validity and previous-frame validity are cleared. No graphics property files or global user settings are changed. The next ordinary frame can resume native settings with invalidated temporal history.

The captured projection is symmetric and unchanged in angular extent. Asymmetric headset projections and independent head rotation remain unsupported until sky/depth reconstruction and producer visibility are adapted. Native shadow placement uses a centered hand camera, but actual visual consistency, visibility at edges, input targeting and effect compatibility still require in-game evidence.

## Output and activation still required

The backend depends on an `Output` provider that transactionally captures real caller GL state, binds equally sized owned eye targets, synchronously copies each image and publishes only a complete pair. It checks reported target extents and restores the provider's state on every exit. This milestone supplies a synthetic provider, not an OpenGL implementation; its tests cannot establish actual GL correctness.

Next: implement and test that provider with FBO/capture utilities from the inherited project, add a version-pinned ZombieBuddy loader that verifies hook visibility/ownership and activates all four transforms together with rollback, then package a manual single-player stereo-capture diagnostic. Before packaging, review remaining effect/picking and culling limitations against that narrow capture scope. No active mod entry point or installer exists in 0.3.0.
