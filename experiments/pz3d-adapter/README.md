# PZ3D renderer boundary — milestone 3

This directory supplies the **current stereo instrumentation used by the 0.11.0 harness**, as well as an independently runnable offline adapter test. It transforms four classes from the pinned PZ3D 0.3.0 binary. The complete installable mod, runtime transformer installation and OpenXR connection live in the [harness](../zombiebuddy-harness/README.md); this directory's standalone commands do not launch or install anything. See the [current baseline map](../../docs/PZ3D-BASELINE.md) for the integrated lifecycle and exact coupling.

Subsequent milestone: a [ZombieBuddy capture harness](../zombiebuddy-harness/README.md) now packages the bridge and shared transformer for the user's first in-game test. The offline adapter below remains available independently. The harness additionally intercepts capture-only errors and performs retained-style fallback; that behavior was not part of the original adapter-only milestone.

Current compatibility target: Zomboid 42.21.0, PZ3D 0.3.0, ZombieBuddy 2.3.2. The architectural discussion below originated with PZ3D 0.2.2; the renderer boundaries were rechecked against 0.3.0.

## Build and test

From the workspace root:

```powershell
.\experiments\pz3d-adapter\Test.ps1
```

Requires the portable JDK already downloaded for the standalone diagnostic and the local reference JARs. `Build.ps1` builds without running the synthetic tests. The builder verifies the SHA-256 of Project Zomboid, PZ3D, and ZombieBuddy before producing output. No new dependency downloads are needed: the offline tool uses ZombieBuddy's bundled ASM; the bridge uses the game's bundled JOML/LWJGL APIs. No game or mod entry point runs.

Outputs under ignored `build/`:

| Artifact | Purpose |
|---|---|
| `pz3d-pair-hooks.jar` | Bridge classes only; no mod descriptor, agent, automatic entry point, or bundled game code |
| `instrumented/com/pavelvoronin/pz3d/*.class` | Locally transformed reference classes; proprietary, not for distribution or manual installation |
| `instrumented/transform-report.txt` | Matched boundaries and independent JDK classfile verification result |
| `test-results.txt` | Results from executing transformed synthetic fixtures with `-Xverify:all` |

The test fixture classes intentionally use the target class names and method descriptors. They live on a separate test classpath and are never included in the hook JAR. They are original small stand-ins, not decompiled game implementations. The game archive contributes JOML and class metadata to these tests; actual game/mod classes are not executed.

## Actual transformation

The transformer keeps the existing `Frame.draw(boolean)` method and its original exception/finally structure. It adds no methods or fields to target classes, which keeps later schema-preserving instrumentation possible. The ordinary path stays single-view when the bridge is inactive.

1. The existing scene adoption, private `ad()` prediction, resource preparation, tree preparation, stream-fade update, and common shadow updates execute once.
2. Immediately after `ActorShadows.render`, the bridge records the prepared base camera, scene identity/version, body/vehicle world matrices, and animation-palette fingerprint.
3. Each eye sets scene-relative `x/y/z` and replaces the **combined** `ViewMath.projection` result with an explicit full view-projection matrix. Per-eye frustum setup and `WorldBuffers.beginView()` remain inside the loop. Each eye clears the original scratch color/depth target.
4. The original final desktop blit becomes a synchronous eye copy. The second eye starts only after the first copy completes. Two separate caller-owned RGBA8 color FBOs are the initial intended destinations; `GlEyeTargets` restores read/draw FBO bindings even on failure. No OpenXR swapchain ownership is involved yet.
5. The camera is restored, then the original draw tail and GL restoration execute once. One additional frame lease spans the entire call and is released in `finally`. Early exits, missing instrumentation, nested pairs, changed generations, and detected scene/body/palette changes cause rejection.

The bridge must be invoked on the existing game render/context thread, with an actual first-person, on-foot `Renderer.Frame`. It calls `draw`, not `render` or the game producer. A future caller must preserve `RetainedRender.accept` and ordinary ownership before replacing the consumer's single-view draw. The wrapper also detects incomplete pairs when the original renderer catches an exception internally. That does **not** undo PZ3D's existing `Main.fail` side effect or promise clean runtime fallback.

Additional changes:

- `StreamFade.aB()` uses one pair timestamp while active. Freezing only `StreamFade.update` was insufficient because shader uploads read the clock again.
- Eleven `TreeEnvironment.state` reads in `TreeRenderer` use the first observed state for the pair, including season/snow values which the game thread can update.
- Corpse `rendered` flags are deferred until the second eye.
- Adaptive depth prepass and GPU telemetry are suppressed during the proof. Existing CPU counters are not valid stereo performance measurements.
- Sky rendering, outlines/highlighting, contact-shadow overlays, tracers, crosshair, captions, and chunk debug are omitted in active pairs. The sky pass can upload asynchronously completed moon assets, so it was not safe to assume both eyes would see the same resources. The clear-color background remains. Ordinary mode retains these calls.

The default synthetic eye factory uses ±half the requested separation along the base camera's horizontal right axis and the original PZ3D view conventions. Separation is in **PZ3D scene units**, not calibrated physical metres. A caller can instead supply two full matrices and matching positions, but the OpenXR-to-PZ3D coordinate/scale conversion is not wired here.

## Evidence and limits

Three transformed installed classes passed independent JDK classfile verification. The same transformer ran on executable synthetic fixtures; **46 assertions/rejection checks passed** under JVM verification. They cover inactive single-view behavior, one preparation/two visibility passes, copy ordering, distinct eye positions, frozen fade/weather state, deferred corpse flags, and lease/camera cleanup. Failure cases include second-eye copy failure, generation change, body mutation, early draw skip, nested pairs, wrong thread, bad binary hash, invalid/aliased targets, and framebuffer restoration after an incomplete-target error.

These checks establish bytecode structure and adapter control flow. They do not establish that the live PZ3D renderer is stereo-correct. Real GL state, native model/texture lifetimes, unseen side effects, concurrent configuration updates, shared-shadow coverage of both eyes, and shader behavior still need in-game evidence. The palette fingerprint is a diagnostic hash, not an immutable copy or synchronization primitive. The JOML bundled in the game emits an existing Unsafe deprecation warning under Java 25 during tests.

## Historical integration plan (completed by the harness)

The following paragraphs preserve the original milestone plan, not outstanding implementation tasks. Runtime loading, XR, UI and tracked arms are implemented in the harness; current limitations are listed in the baseline map.

The missing part is the loader-backed test harness: install an all-or-nothing, version-checked transformer through the supported ZombieBuddy development workflow, invoke the bridge from the existing consumer path, allocate and retire two test targets on the render thread, mirror one eye, and save an opt-in capture with scene/generation and lease evidence. Transformer ordering and original input class hashes must be checked before activation; matching files on disk alone does not prove compatible loaded bytecode.

Do not copy these generated class files into the game or replace the signed PZ3D JAR. Once the harness is packaged, the user can run the first in-game synthetic stereo test. OpenXR submission, headset pose conversion, UI integration, and retained-frame pacing follow that test.

The fourth target, `Renderer`, has a version-checked hook at the part attachment-matrix upload. `AttachmentPoses` supplies owned matrices only within a stereo scope, returning the original matrix when no override exists. This supports held-item motion without writing shared snapshot matrices.
