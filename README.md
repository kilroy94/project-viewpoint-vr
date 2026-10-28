# Project Viewpoint VR

A new Viewpoint-based VR project for Project Zomboid, forked from the preserved PZ3D VR implementation. Viewpoint is the sole renderer target; useful OpenXR, capture and interaction code will be reused as needed.

Download the **[0.11.2 experimental prerelease](https://github.com/kilroy94/project-viewpoint-vr/releases/tag/v0.11.2)** and follow the [installation guide](viewpoint/TESTING.md).

**Current milestone: single-eye spectator mirror, version 0.11.2.** Native stick movement now coexists with independent physical movement through native collision queries, tracked arms/held items and opt-in swing-triggered native attacks. The user confirmed stereo/head tracking and thumbstick walking, but 0.10.0 physical body following failed. This build addresses its input/movement coupling; the new rig and movement still need in-game validation. See [hybrid rig design and limits](viewpoint/HYBRID-RIG.md). Renderer/controller activation and motion attacks start Off; turning preferences and world scale persist.

0.11.2 presents the existing left-eye world image across the desktop spectator viewport during XR, preserving aspect ratio and the normal full-size desktop UI. Both eyes and the floating headset UI continue through the existing OpenXR path; no third camera is rendered. Desktop stereo mode remains side by side. This mirror needs user-run headset/desktop confirmation.

0.11.1 fixes a same-tile collision rejection in physical stepping, adds an explicit **Assign VR gamepad to player 1** button and a persistent motion-diagnostic count. These changes require a new user-run test. The user confirmed tracked hands work after turning **Use Gamepad Inputs Off** in SteamVR's per-game VR Controller settings. The log now confirms focused XR, both Frame controllers and tracked-rig captures. The user reports physical steps still fail in 0.11.0 despite sparse accepted-displacement logs; two diagnostic swings were recognized, but native combat remains unvalidated. See the [focus investigation and next test](viewpoint/FOCUS.md).

**Targets Viewpoint 0.1.6.1.** Development follows the latest available Viewpoint; older versions are no longer supported. Exact binary checks remain in place so an unaudited future update cannot silently run incompatible hooks. Turn Viewpoint frame generation Off for stereo/XR. Vendor temporal upscaling is inactive during stereo, and the borrowed eye images use regular perspective projection. See the [compatibility review](viewpoint/COMPATIBILITY.md).

The opt-in [controller diagnostic](viewpoint/DIAGNOSTIC.md) runs against dedicated Zomboid and Viewpoint UI widgets and writes PASS/FAIL/NEEDS VISUAL CHECK text reports. Its first in-game run remains pending.

Start with the [manual installation/capture guide](viewpoint/TESTING.md), [build and tests](viewpoint/README.md), [integration plan](docs/VIEWPOINT-PLAN.md) and [validation record](viewpoint/VALIDATION.md). Target binaries: Viewpoint 0.1.6.1, Project Zomboid 42.21.0 and ZombieBuddy 2.3.4.

After manual installation, start SteamVR, then use the on-screen **VR gameplay (tracked + native gamepad)** button. Click **Assign VR gamepad to player 1** in our panel for its stick/button controls. This explicit action replaces that player's current pad binding; it does not create a second player. Off/Desktop/XR fixed/XR tracked remain separate diagnostic controls. Pause/Break alone switches Off; Shift+Pause/Break saves a PNG pair while Off. The generated archive is `viewpoint/dist/ProjectViewpointVR-0.11.2.zip`; this repository does not track generated binaries. See [controller controls and scope](viewpoint/INPUT.md). SteamVR is needed only for its headset modes when SteamVR is your OpenXR runtime.

## Inherited reference code

Git history and `pz3d-vr-baseline` preserve the previous implementation. The `experiments/` tree and [PZ3D architecture guide](docs/PZ3D-BASELINE.md) describe that historical project, including features that are **not implemented for Viewpoint**. Its build/install scripts still target PZ3D and are not this project's build path. Historical `research/final-report.md` is PZ3D research, not the current Viewpoint plan.

No game or dependency binaries are distributed. Tests use local copied inputs without launching the game or mod entry points. The working game, Workshop installation and Zomboid user data are outside this project's write scope.
