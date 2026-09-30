# PZ3D VR prototype

Preserved source baseline: **`pz3d-vr-baseline`**, package version **0.11.0**. Read the [current architecture, integration points and maintenance guide](docs/PZ3D-BASELINE.md) before continuing development. The tag preserves the implementation and its validation record; it does not imply that every experimental feature has been confirmed in a headset.

Experimental OpenXR support for Project Zomboid through PZ3D and ZombieBuddy. Current release: **[v0.10.1 prerelease](https://github.com/kilroy94/pz3d-vr/releases/tag/v0.10.1)**, Windows x64, for Zomboid **42.21.0** and PZ3D **0.3.0**. The older v0.9.0 release targets Zomboid 42.20.4 / PZ3D 0.2.2.

The prototype now includes stereo headset rendering, head tracking, the vanilla UI in VR, tracked first-person arms and held items, optional VR-controller gamepad input, and two experimental motion-melee modes. It is under active development and is not a complete VR conversion. The **0.11.0** baseline adds grip-button hand proximity interactions; no numbered 0.11.0 release or headset confirmation is recorded.

## Current features

| Area | Implemented behavior |
|---|---|
| Headset rendering | Live OpenXR stereo with headset position and orientation driving the visual camera. |
| Desktop testing | Continuous side-by-side stereo, saved eye-image pairs, and optional simulated SteamVR with a movable, bordered Headset Window. |
| Vanilla UI | A transparent, head-following panel displays the existing game UI in VR. Native menu controls remain available; there is no motion-controller pointer yet. |
| Tracked arms | First-person arm IK, visible upper arms, palm-centered controller alignment, and held-item transforms that follow the hands. |
| Calibration and fitting | Calibration survives tracking interruptions; recenter uses a five-second countdown. Physical kneeling lowers the arm roots, and adjustable arm extension helps match controller reach. |
| Controller input | Optional OpenXR Touch-to-native-gamepad bridge with diagnostics; no Windows virtual-controller driver required. |
| Motion melee | Animation-timed armed swings, plus a contact-timed plain-baseball-bat pilot with separate diagnostic modes. |
| Hand interaction (0.11.0 workspace build) | Reach within 25 cm of a reachable world surface and squeeze grip to invoke PZ3D's native short-press use path. Off/diagnostic/live modes and left/right/either-hand selection. |
| Settings and diagnostics | Remappable shortcuts, persisted Mods settings, frame-timing reports, and tracking/input/combat logs. |

Live desktop stereo, simulated-headset output, and the vanilla UI have been confirmed in user testing. A physical-headset tester confirmed arm tracking in an earlier build. The later calibration/reach refinements, gamepad bridge, and melee prototypes still need physical-headset and in-game confirmation; automated checks alone do not establish gameplay correctness.

## Requirements

The harness checks exact binary hashes for these versions:

- Project Zomboid **42.21.0**
- PZ3D **0.3.0**
- ZombieBuddy **2.3.2**, either the original pinned JAR or the specifically verified **B42.21 temporary-fix** JAR. Unknown/rebuilt variants remain unsupported.

Obtain these separately. This repository contains the prototype source and synthetic test fixtures, not copies of the game or either dependency. Headset output requires a working OpenXR runtime, such as SteamVR configured for OpenXR. Desktop stereo does not require a headset or VR runtime. Start with a disposable single-player save, in first person and on foot.

## Using the prototype

See the [mod instructions](experiments/zombiebuddy-harness/README.md) for installation, runtime setup, controls, and limitations. The published download is still [v0.10.1](https://github.com/kilroy94/pz3d-vr/releases/tag/v0.10.1); build the workspace version below for the new hand interactions. The steps here use the workspace 0.11.0 ZIP. Older releases do not accept the ZombieBuddy B42.21 temporary-fix JAR. Do not use the older v0.9.0 release with the updated game.

1. Close the game and extract `PZ3DVRTest-0.11.0.zip` into your local mods directory, normally `%USERPROFILE%\Zomboid\mods`. The descriptor should be at `PZ3DVRTest\42.20.4\mod.info`. Replace the old prototype folder when updating.
2. Enable ZombieBuddy, PZ3D, and **PZ3D Stereo Capture Test [Java]** for the test save. Approve the updated prototype JAR if ZombieBuddy prompts.
3. Start your VR runtime, load the save, enter PZ3D with **Insert**, and use first person on foot. Toggle OpenXR with the shortcut below.
4. Recenter while upright: press the shortcut, return both hands to the controllers, face forward, and hold a neutral pose during the five-second countdown.

The package retains its `42.20.4` directory name for installation continuity; its descriptor now requires exactly 42.21.0. Replace the old folder rather than editing the old version limits. The in-game mod name still reflects the original capture harness. No game or Workshop JAR needs to be replaced.

Shortcuts are remappable in **Options > Mods > PZ3D VR**. Select a keyboard key and modifiers, then press **Apply**. The defaults are:

| Shortcut | Action |
|---|---|
| Ctrl+Shift+Scroll Lock | Toggle OpenXR |
| Ctrl+Shift+Alt+Scroll Lock | Start five-second headset/arm recenter countdown |
| Ctrl+Alt+Scroll Lock (without Shift) | Toggle synthetic arms in XR; desktop stereo when XR is off |
| Ctrl+Shift+F10 | Capture stereo PNGs while OpenXR is off |

OpenXR automatically includes the vanilla UI as a transparent, head-following panel. The desktop cursor is not included yet. Desktop stereo needs no VR runtime; headset output needs a working OpenXR runtime. A SteamVR simulated-headset helper is included for development without hardware.

## Arms and held items

Controller grip poses drive the first-person arms, with estimated palm alignment and held-item attachment updates. Calibration is retained through tracking loss and dashboard interruptions. Physical headset height changes move the shoulder roots, so kneeling lowers the arms; this does not change the game's crouch state or add full-body IK.

**Options > Mods > PZ3D VR > Maximum arm reach (percent)** controls bounded arm extension: 150% by default, adjustable from 100% to 175%. Hands and held items retain their size; larger extensions can stretch sleeves and elbows. This does not increase native melee range. The contact pilot uses the rendered bat position but retains its own reach checks. Two-handed items follow their native owning hand; a support-hand constraint is not implemented.

## Movement and controller input

Keyboard/mouse and native gamepad controls remain available. The optional **VR controllers as gamepad** setting maps Touch controller inputs into **PZ VR Gamepad**. Begin with **Input diagnostics only**, then select **Gamepad with diagnostics**, return controls to neutral, and enable/assign the device in Zomboid's native controller settings. Assignment is not automatic.

Sticks and face buttons use native gamepad bindings; grips act as bumpers. Tapping left Menu sends Start; holding Menu with the left stick supplies a D-pad, and Menu + X supplies Back. The runtime may reserve Menu. See the [full control mapping](experiments/zombiebuddy-harness/README.md#touch-controllers-as-a-conventional-gamepad-080).

### Stick turning and ready/aim (0.10.0)

In **Options > Mods > PZ3D VR**, choose **Stick turning: Snap or Smooth** (default Off). Snap angles are 15, 30, 45, 60, or 90 degrees; one sideways deflection produces one snap, and returning to center rearms it. Smooth speed is adjustable from 30-240 degrees/second, default 90, scaled by stick deflection. Vertical stick input does not pitch the camera.

Turning works during focused, first-person XR gameplay with either VR thumbsticks or an assigned ordinary gamepad. **Turning input** can be Automatic, VR controllers, or Assigned gamepad. Automatic prefers an assigned physical gamepad; otherwise it reads the VR controllers directly, even with the gamepad bridge Off. Physical pads use their configured native aiming axes. Unselected controllers retain their native behavior.

The selected right stick is reserved for turning. **Hold ready/aim** defaults to the left trigger, with left bumper/grip, right bumper/grip, and right-stick click alternatives. That binding is consumed during owned gameplay to avoid also invoking its native action; menus regain native input. Turning Off restores the original controls. Return controls to neutral after changing settings or resuming tracking.

Artificial turning updates PZ3D's shared heading so movement, headset view and tracked items use the same base orientation; physical head tracking remains independent. It temporarily disarms both motion-melee modes while turning and for 200 ms afterward. Release the attack trigger after the turn to rearm. This first version does not support simultaneous stick turning and motion-melee attacks. Native button attacks retain their existing path.

Teleport locomotion and a replacement movement scheme are not implemented. Turning speed, ready-stance behavior and menu transitions still need in-game/headset validation.

## Motion melee

Motion melee is **Off by default**. Choose a mode under **Options > Mods > PZ3D VR > Motion melee prototype**:

| Mode | Behavior |
|---|---|
| Diagnostics only (no attacks) | Logs eligible armed swing gestures without attacking. |
| Live armed melee (animation-timed) | A deliberate right-hand swing requests one native attack; character facing and the native animation determine the hit. Supports ordinary one-handed, two-handed, and heavy swing weapons. |
| Bat contact diagnostics (no attacks) | Logs swept contact between the rendered plain baseball bat and an eligible standing zombie. |
| Bat contact-timed attacks | Selects the contacted zombie and resolves through native combat as soon as its attack state is ready, without waiting for the animation impact event. |

Release the right trigger, then hold it while swinging. Keep holding through the attack and release between attempts. Start with diagnostics and inspect `console.txt` before enabling live attacks.

For movement through the gamepad bridge alongside motion melee, check **Allow motion melee with gamepad input**. It is unchecked by default. When checked and any melee mode is active, the right trigger belongs exclusively to motion melee; native gamepad RT stays released, including in menus and for firearms. Set melee to Off to restore native RT. Return controls to neutral after changing settings.

The contact pilot supports only **plain `Base.BaseballBat` weapons against standing zombies in single-player**. It uses approximate bat/body capsules, selects one target per trigger hold, checks obstructions, and suppresses the later animation collision. Native recovery remains; unresolved contacts expire after 150 ms. It does not provide per-limb hitboxes, positional headshot bonuses, scenery damage, or multi-hit swings. Misses currently do not start a native attack or incur its missed-swing cost.

Neither motion-melee mode implements shoves, stomps, firearms, or unarmed combat. Swing thresholds, actual damage behavior, and contact feel require headset testing. See the [detailed combat instructions](experiments/zombiebuddy-harness/README.md#contact-timed-baseball-bat-pilot-090).

## Logs and known limits

The game log is normally `%USERPROFILE%\Zomboid\console.txt` (or the equivalent under a custom game cache directory). Useful prefixes include `[PZ3D OpenXR]`, `[PZ3D VR Input]`, `[PZ3D VR Melee]`, and `[PZ3D VR Contact]`. Combat logs distinguish requests and collision processing; a collision or `resolved` entry alone is not proof of damage. Stereo captures and their reports are stored under `Zomboid\PZ3D-VR-Test\<timestamp-id>`.

This prototype targets first-person, on-foot, single-player use. Multiplayer, vehicles, full-body IK, natural inventory interaction, and controller-ray UI input are outside the current implementation. Some PZ3D effects are omitted from stereo output; see the mod instructions. A simulated headset is useful for rendering and lifecycle checks but cannot validate physical tracking, comfort, or combat feel. Performance, world scale, latency, and hardware compatibility remain under evaluation.

The local suite passes rendering, OpenXR lifecycle, arm tracking, controller input, melee geometry/ownership, settings, and copied-class transformation checks. Actual in-game validation is tracked separately in the [validation record](experiments/zombiebuddy-harness/VALIDATION.md).

## Building and testing

Use Windows x64, PowerShell, and Python 3. Run this from the repository root to download the pinned workspace-local JDK 25 and LWJGL dependencies:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File experiments/openxr-diagnostic/Setup.ps1
```

Copy the following JARs from your own matching installations into these local paths (they are ignored by Git):

| Dependency | Destination |
|---|---|
| projectzomboid.jar | reference/project-zomboid/42.21.0/projectzomboid.jar |
| PZ3D-0.3.0.jar | reference/pz3d/0.3.0/PZ3D-0.3.0.jar |
| ZombieBuddy.jar | reference/zombiebuddy/binaries/ZombieBuddy.jar |

Then build and run the local suite:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File experiments/zombiebuddy-harness/Test.ps1
```

This produces `experiments/zombiebuddy-harness/dist/PZ3DVRTest-0.11.0.zip`. The suite uses synthetic fixtures, copied-class inspection/retransformation without initialization, and standalone OpenGL checks. It does not install the mod or launch the game. Native OpenXR smoke tests are separate; see the harness instructions. Do not run a competing test scene while the game is using XR.

## Project layout

- [Current baseline map](docs/PZ3D-BASELINE.md): architecture, concrete hooks, PZ3D coupling, build/deployment and unfinished work. This is the technical starting point for future sessions.

- [ZombieBuddy harness](experiments/zombiebuddy-harness/README.md): mod, OpenXR backend, UI panel, packaging and tests.
- [Renderer adapter](experiments/pz3d-adapter/README.md): version-gated stereo instrumentation.
- [Standalone OpenXR diagnostic](experiments/openxr-diagnostic/README.md): runtime experiments and dependency setup.
- [Research report](research/final-report.md): original feasibility investigation; historical findings are not a statement of current implementation status.
- [Validation record](experiments/zombiebuddy-harness/VALIDATION.md): tests, user confirmations, and remaining limitations.

Research notes may link to local evidence and decompiled references. Those files, downloaded tools, logs, screenshots, and build products are intentionally excluded from the repository. Some historical research scripts assume the original author's installation paths; they are not needed to build the mod.

See [third-party dependencies](experiments/zombiebuddy-harness/THIRD_PARTY.md). No license for the original project code has been selected yet; publication is not an additional license grant for third-party game or mod material.
