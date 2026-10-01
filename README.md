# Project Viewpoint VR

A new Viewpoint-based VR project for Project Zomboid, forked from the preserved PZ3D VR implementation. Viewpoint is the sole renderer target; useful OpenXR, capture and interaction code will be reused as needed.

**Current milestone: manual desktop capture diagnostic 0.4.1.** A version-pinned ZombieBuddy loader and real OpenGL capture output now connect the native-stage adapter to a one-shot stereo capture. The installable ZIP is ready for the user's first in-game test. Automated copied-binary, lifecycle and standalone GPU tests passed; **no Viewpoint stereo image has been confirmed in-game yet**. There is no headset support in this project yet.

Start with the [manual installation/capture guide](viewpoint/TESTING.md), [build and tests](viewpoint/README.md), [integration plan](docs/VIEWPOINT-PLAN.md) and [validation record](viewpoint/VALIDATION.md). Target binaries: Viewpoint 0.1.3, Project Zomboid 42.21.0 and ZombieBuddy 2.3.2 (original or the audited B42.21 temporary fix).

After manual installation, Shift+Pause/Break saves left/right/side-by-side PNGs under the Zomboid cache's `Project-Viewpoint-VR` folder. Ordinary rendering resumes on the next frame. The generated archive is `viewpoint/dist/ProjectViewpointVR-0.4.1.zip`; this repository does not track generated binaries. SteamVR is not required for this diagnostic.

## Inherited reference code

Git history and `pz3d-vr-baseline` preserve the previous implementation. The `experiments/` tree and [PZ3D architecture guide](docs/PZ3D-BASELINE.md) describe that historical project, including features that are **not implemented for Viewpoint**. Its build/install scripts still target PZ3D and are not this project's build path. Historical `research/final-report.md` is PZ3D research, not the current Viewpoint plan.

No game or dependency binaries are distributed. Tests use local copied inputs without launching the game or mod entry points. The working game, Workshop installation and Zomboid user data are outside this project's write scope.
