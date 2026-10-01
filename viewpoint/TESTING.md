# Project Viewpoint VR 0.5.0 - desktop stereo and head tracking

This experimental build implements continuous desktop stereo, OpenXR headset output, tracked head rotation/translation, and recentering. Version 0.4.1 produced two complete in-game stereo captures inspected on 2026-09-30. The new continuous and headset modes still require your live validation; automated tests do not establish headset comfort or gameplay correctness.

## Install

1. Close Project Zomboid. Extract `ProjectViewpointVR-0.5.0.zip` into `%USERPROFILE%\Zomboid\mods`, replacing the previous ProjectViewpointVR files. The descriptor is `ProjectViewpointVR\42\mod.info`.
2. Use Zomboid **42.21.0**, Viewpoint **0.1.3**, and ZombieBuddy **2.3.2** (original pinned JAR or audited B42.21 temporary fix). Exact binary checks remain mandatory. Approve the updated Java JAR if ZombieBuddy prompts.
3. Enable those mods in a disposable single-player save. Disable PZ3D and the old PZ3D VR mod for this save. Enter Viewpoint first person, on foot. Viewpoint's inspected default toggle is **O**; use your established control if different.
4. The **Project Viewpoint VR** buttons appear near the upper left. Use Viewpoint's **middle mouse button** to free/capture the cursor when you need to click them. Settings are session-only; startup mode is always Off.

## Desktop test first

Click **Desktop stereo**. The world appears in two letterboxed views; vanilla UI stays on the desktop. Four initial scene draws allow the broader visibility snapshots to reach the renderer. Walk and turn, open a door, inspect an outdoor scene and moving characters. Look for missing objects, differences between eyes, flicker and large frame-time spikes. This mode reuses two GPU targets and performs no PNG readback.

Click **Off**, or press **Pause/Break alone**, to restore ordinary rendering. To save a pair while Off, hold Shift, tap Pause/Break, then release Shift, or click **Save PNG pair (Off)**. Saved pairs remain under the Zomboid cache's `Project-Viewpoint-VR` folder. PNG capture is not active during the continuous/XR modes.

## Headset test

1. Start your normal headset connection and SteamVR yourself. The headset must be ready in the OpenXR runtime selected for the game process. This mod does not change global runtime settings, install drivers or configure SteamVR. A missing runtime is reported on the desktop and returns the mode to Off.
2. **XR fixed** checks submission with a fixed visual camera and the runtime's eye separation/FOV. Inspect it briefly in the desktop mirror or headset without moving your head; this diagnostic mode intentionally does not make the camera follow head motion.
3. Switch to **XR tracked** for head rotation and positional tracking. Face forward when tracking first becomes valid. Native mouse yaw remains your body-heading control; headset pitch/roll supply the visual pitch/roll in this mode. No controller input is installed.
4. Click **Recenter in 5 seconds**, face forward, and wait for the countdown. Recenter resets position and yaw, preserving gravity/horizon rather than cancelling your physical head tilt. Reference-space changes also request a fresh anchor. Tracking loss submits no world layer; it does not invent an eye pose.
5. Check world scale while stationary. The default is **1 scene unit per meter**, not physically calibrated. If needed, switch Off, enter a value between **0.25 and 4** in the units/meter box, click Apply, and re-enter XR tracked. This scales physical head movement and runtime IPD together. It does not change world geometry.
6. Test Off, then re-enter XR to verify cleanup/recreation. The runtime is pumped with empty frames while Viewpoint has no eligible world draw. A stopped runtime returns Off before native scene work; a failure after drawing has begun disarms the new modes and may cause Viewpoint's own error handler to disable Viewpoint. Preserve the log and restart after a rendering failure.

## Scope and known limits

- World imagery only in the headset: **no VR UI, controllers or tracked hands yet**. Use the desktop to operate menus and mode controls. Native keyboard/mouse gameplay and picking still use the native player camera, not an independent headset aim ray.
- First person, on foot, single player only. Third person/freecam/vehicles suspend the pair path; multiplayer is rejected.
- Eye targets retain the 0.05/400 clipping planes. Desktop eye images are capped at 1280 pixels on their longest side; XR source targets use the runtime's recommended dimensions capped at 2048, then scale to each runtime swapchain.
- Each headset eye is rendered with a symmetric frustum containing its requested asymmetric FOV, then cropped during GPU submission. This preserves the native symmetric sky/depth reconstruction. Crop boundaries are rounded to source pixels; edge error is below one source pixel.
- Continuous modes broaden the desktop-facing chunk and character filters and bypass room hiding. This retains geometry for independent head turns, but costs CPU/GPU time and does not expand the game's loaded world or remove all game visibility rules. The broadening expires if rendering stops and turns off with the mode.
- TAA, indirect lighting, volumetrics, clouds and shader-pack post passes remain disabled only inside stereo rendering. Color/gamma, sustained frame rate, transparency sorting, head movement near walls, rapid turns and full scene visibility need live testing. Physical head displacement is visual only and has no collision correction.
- Shift+Pause/Break retains the locally audited capture shortcut. Pause/Break alone is the emergency Off control. No F10 binding is used. Unrelated desktop-app shortcuts are outside the local game/mod audit.

Return `console.txt` lines containing `[Project Viewpoint VR]` and `[Project Viewpoint VR OpenXR]`, any errors, the selected mode, and what each eye showed. Useful headset evidence includes `Session state=...`, eye dimensions, and increasing `Projection pairs submitted=...` counts. Confirm continuous desktop rendering before assessing headset-specific issues.

Disable the mod and restart to remove its hooks. The package replaces no dependency JAR, Workshop file, SteamVR configuration or saved graphics setting. It bundles pinned LWJGL OpenXR bindings and the Windows OpenXR loader with their licenses; it reuses the game's LWJGL core/OpenGL.
