# Project Viewpoint VR

A new Viewpoint-based VR project for Project Zomboid, forked from the preserved PZ3D VR implementation. Viewpoint is the sole renderer target; useful OpenXR, capture and interaction code will be reused as needed.

**Current milestone: headset UI on current Viewpoint, 0.6.1.** A transparent, head-following panel combines Zomboid UI, Viewpoint loot/performance overlays and its settings window, with a mouse-position marker. Desktop keyboard/mouse interaction is retained. The user confirmed the earlier continuous desktop and null-headset tests without issues; physical-headset testing is deferred. The new UI needs its first in-game test.

**Targets Viewpoint 0.1.5a-hotfix.** Development follows the latest available Viewpoint; older versions are no longer supported. Exact binary checks remain in place so an unaudited future update cannot silently run incompatible hooks. This update still needs an in-game test.

Start with the [manual installation/capture guide](viewpoint/TESTING.md), [build and tests](viewpoint/README.md), [integration plan](docs/VIEWPOINT-PLAN.md) and [validation record](viewpoint/VALIDATION.md). Target binaries: Viewpoint 0.1.5a-hotfix, Project Zomboid 42.21.0 and ZombieBuddy 2.3.2 (original or the audited B42.21 temporary fix).

After manual installation, use the on-screen Off/Desktop/XR fixed/XR tracked controls. Pause/Break alone switches Off; Shift+Pause/Break saves a PNG pair while Off. The generated archive is `viewpoint/dist/ProjectViewpointVR-0.6.1.zip`; this repository does not track generated binaries. SteamVR is needed only for its headset modes when SteamVR is your OpenXR runtime.

## Inherited reference code

Git history and `pz3d-vr-baseline` preserve the previous implementation. The `experiments/` tree and [PZ3D architecture guide](docs/PZ3D-BASELINE.md) describe that historical project, including features that are **not implemented for Viewpoint**. Its build/install scripts still target PZ3D and are not this project's build path. Historical `research/final-report.md` is PZ3D research, not the current Viewpoint plan.

No game or dependency binaries are distributed. Tests use local copied inputs without launching the game or mod entry points. The working game, Workshop installation and Zomboid user data are outside this project's write scope.
