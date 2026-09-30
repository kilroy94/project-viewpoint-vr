# Project Viewpoint VR

A new Viewpoint-based VR project for Project Zomboid, forked from the preserved PZ3D VR implementation. Viewpoint is the sole renderer target; useful OpenXR, capture and interaction code will be reused as needed.

**Current milestone: development baseline 0.2.0.** Exact binary checks, stereo-camera math, a two-eye lifecycle and a dormant render-entry hook are implemented and tested. The hook has been verified/retransformed against copied Viewpoint bytecode without initialization. There is no installable Viewpoint VR mod or working Viewpoint stereo output yet.

Start with the [baseline build and tests](viewpoint/README.md), [integration plan](docs/VIEWPOINT-PLAN.md) and [validation record](viewpoint/VALIDATION.md). Target binaries: Viewpoint 0.1.3, Project Zomboid 42.21.0 and ZombieBuddy 2.3.2 (original or the audited B42.21 temporary fix).

The first runtime milestone is coherent synthetic desktop stereo from one captured Viewpoint scene, followed by OpenXR and head tracking. The renderer's temporal state, asset preparation and resource cleanup must be adapted before replaying a scene for two eyes.

## Inherited reference code

Git history and `pz3d-vr-baseline` preserve the previous implementation. The `experiments/` tree and [PZ3D architecture guide](docs/PZ3D-BASELINE.md) describe that historical project, including features that are **not implemented for Viewpoint**. Its build/install scripts still target PZ3D and are not this project's build path. Historical `research/final-report.md` is PZ3D research, not the current Viewpoint plan.

No game or dependency binaries are distributed. Tests use local copied inputs without launching the game or mod entry points. The working game, Workshop installation and Zomboid user data are outside this project's write scope.
