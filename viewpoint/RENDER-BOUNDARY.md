# Render boundary milestone (0.2.0)

This document describes the preserved 0.2.0 lifecycle/entry contract. The subsequent [0.3.0 native-stage adapter](NATIVE-STAGES.md) now implements a concrete Viewpoint backend; real output/capture and live activation remain absent.

## Entry hook

`EntryTransform.fromPinnedJar` accepts only the audited Viewpoint 0.1.3 JAR. It extracts the reference class from the same bytes whose hash was verified, avoiding a second-read substitution. `transform` compares normalized executable instructions and schema against that reference. Normalization reuses the inherited PZ3D installer approach to allow JVM constant-pool and method reordering, excluding debug/stack-map attributes. Changed executable code, unknown targets and already patched input are rejected.

The only edit replaces the single `SceneDrawer.render -> drawFrame` invocation with `FrameBoundary.draw(receiver, nativeMethodHandle)`. The private handle resolves using the original caller's privileges. No fields or methods are added; all non-render methods stay unchanged. Existing exception handlers and GL-state restoration remain around the new call site. The transformer preserves stack maps and recomputes maximum stack use because its control flow and boundary stack shape are unchanged.

The hook delegates to the original body once when no driver is registered. Driver registration is scoped to its owning thread. A driver may choose the ordinary path or replace it with split rendering stages. The borrowed original callback is single-use and cannot escape the scope, cross threads or recursively consume a scene. A replacement failure propagates to the native handler; it never retries the original frame after partial rendering.

There is no live installer. Before one is added it must validate game/loader pins, loaded classloader identity/modifiability, hook visibility, transformed-bytecode verification and rollback. The current copied-binary test performs ownership and verification checks in an isolated JVM; that is not a claim of compatibility with ZombieBuddy's live patch chain.

## Pair lifecycle

`StereoFrame.render(snapshot, backend)` borrows one snapshot synchronously:

1. Save caller state transactionally; receive its restoration action.
2. Validate the snapshot and prepare shared resources once.
3. Validate, draw left, validate and synchronously copy left.
4. Validate, draw right, validate and synchronously copy right.
5. Validate again, release shared resources once, restore caller state, then publish the complete pair.

Skipped preparation still releases partial preparation and restores caller state. Draw/copy/validation failures attempt both release and restoration. The first failure stays primary; later cleanup failures are attached as suppressed exceptions. An incomplete pair is never published. Nested pairs are rejected and the scope is cleared after every exit. A failed transactional save must leave state unchanged; adapters are responsible for honoring this acquisition contract.

The core does not retain game frames, run simulation, bind an FBO or assert that a snapshot stays valid on its own. The backend must implement those concrete operations and validation. Tests simulate output state and invalidation; actual OpenGL state restoration is not established by those tests.

## Adapter work identified at 0.2.0

Implement a version-pinned Viewpoint backend that separates `WorldRenderer.begin`'s shared uploads/preparation from per-eye matrices/drawing, defers texture/model/retirement cleanup to pair end, and preserves per-eye far frusta while freezing shared far uploads. Do not use the borrowed whole-frame callback as either eye's draw operation.

The 0.3.0 adapter addresses the stage split and diagnostic history policy using additional native call-site transforms. Producer culling, asymmetric projection effects and real output FBOs remain relevant before expanding beyond a narrow synthetic-camera capture test. See [native stages](NATIVE-STAGES.md) and [the integration plan](../docs/VIEWPOINT-PLAN.md) for current status.
