# Project Viewpoint VR

A new Viewpoint-based VR project for Project Zomboid, forked from the preserved PZ3D VR implementation. Viewpoint is the sole renderer target; useful OpenXR, capture and interaction code will be reused as needed.

**Current milestone: controller input, 0.7.3.** Adds OpenXR controller poses, a right-hand UI ray, trigger clicking, scrolling, and optional walking with configurable snap or smooth turning. Turning preferences and world scale persist across restarts. Renderer and controllers start Off. The user confirmed the 0.6.1 renderer update and UI work; controller behavior has automated simulation/GPU coverage but awaits in-game and physical-controller validation. Physical-headset testing remains deferred.

**Targets Viewpoint 0.1.5a-hotfix.** Development follows the latest available Viewpoint; older versions are no longer supported. Exact binary checks remain in place so an unaudited future update cannot silently run incompatible hooks.

Start with the [manual installation/capture guide](viewpoint/TESTING.md), [build and tests](viewpoint/README.md), [integration plan](docs/VIEWPOINT-PLAN.md) and [validation record](viewpoint/VALIDATION.md). Target binaries: Viewpoint 0.1.5a-hotfix, Project Zomboid 42.21.0 and ZombieBuddy 2.3.2 (original or the audited B42.21 temporary fix).

After manual installation, use the on-screen Off/Desktop/XR fixed/XR tracked controls. Pause/Break alone switches Off; Shift+Pause/Break saves a PNG pair while Off. The generated archive is `viewpoint/dist/ProjectViewpointVR-0.7.3.zip`; this repository does not track generated binaries. See [controller controls and scope](viewpoint/INPUT.md). SteamVR is needed only for its headset modes when SteamVR is your OpenXR runtime.

## Inherited reference code

Git history and `pz3d-vr-baseline` preserve the previous implementation. The `experiments/` tree and [PZ3D architecture guide](docs/PZ3D-BASELINE.md) describe that historical project, including features that are **not implemented for Viewpoint**. Its build/install scripts still target PZ3D and are not this project's build path. Historical `research/final-report.md` is PZ3D research, not the current Viewpoint plan.

No game or dependency binaries are distributed. Tests use local copied inputs without launching the game or mod entry points. The working game, Workshop installation and Zomboid user data are outside this project's write scope.
