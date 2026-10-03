# Project Viewpoint VR 0.7.2 - controller input

This experimental build implements continuous desktop stereo, OpenXR headset output, tracked head rotation/translation, and recentering. Version 0.4.1 produced two complete in-game stereo captures inspected on 2026-09-30. The user has since confirmed continuous desktop and null-headset tests without issues. Physical-headset testing is deferred. The user also confirmed the 0.6.1 renderer update and UI work. Controller input is new and requires live validation.

**Compatibility:** this package targets Viewpoint **0.1.5a-hotfix**, the installed Workshop update audited for this build. It replaces support for 0.1.3. Future updates must be audited before their binaries are accepted; do not bypass the hash check.

## Install

1. Close Project Zomboid. Extract `ProjectViewpointVR-0.7.2.zip` into `%USERPROFILE%\Zomboid\mods`, replacing the previous ProjectViewpointVR files. The descriptor is `ProjectViewpointVR\42\mod.info`.
2. Use Zomboid **42.21.0**, Viewpoint **0.1.5a-hotfix**, and ZombieBuddy **2.3.2** (original pinned JAR or audited B42.21 temporary fix). Exact binary checks remain mandatory. Approve the updated Java JAR if ZombieBuddy prompts.
3. Enable those mods in a disposable single-player save. For an existing save, use **Load > select the save > Choose Mods** and explicitly enable **Project Viewpoint VR - Desktop + OpenXR [Java]** there; enabling it only at the main menu does not enable it for that save. Disable PZ3D and the old PZ3D VR mod for this save. Enter Viewpoint first person, on foot. Viewpoint's inspected default toggle is **O**; use your established control if different.
4. The **Project Viewpoint VR** buttons appear near the upper left. Use Viewpoint's **middle mouse button** to free/capture the cursor when you need to click them. Turning preferences and world scale are saved across restarts; renderer and controller activation always start Off.

If the buttons are missing, first check the save's own mod list. In the observed case, the Java startup message appeared at the main menu but the mod was absent from the save, so the Lua controls did not load. After changing the save's mod selection, restart the game. Pause/Break alone only turns a mode Off and has no visible effect when already Off.

## Updated-version regression

Before testing the UI, repeat a short desktop-stereo and null-headset run on this version. Check mouse turning, camera lean, near/far scenery and corpses while turning; confirm Off restores ordinary rendering. Open Viewpoint settings and, if shown, its new setup wizard. Both use the same captured ImGui path. Existing Viewpoint keybindings are now customizable; use your configured controls. No default Pause/Break assignment was found in the audited new Viewpoint key declarations, but do not assign it yourself to a second action.

## Desktop test first

Click **Desktop stereo**. The world appears in two letterboxed views; vanilla UI stays on the desktop. Four initial scene draws allow the broader visibility snapshots to reach the renderer. Walk and turn, open a door, inspect an outdoor scene and moving characters. Look for missing objects, differences between eyes, flicker and large frame-time spikes. This mode reuses two GPU targets and performs no PNG readback.

Click **Off**, or press **Pause/Break alone**, to restore ordinary rendering. To save a pair while Off, hold Shift, tap Pause/Break, then release Shift, or click **Save PNG pair (Off)**. Saved pairs remain under the Zomboid cache's `Project-Viewpoint-VR` folder. PNG capture is not active during the continuous/XR modes.

## Headset test

1. Start your normal headset connection and SteamVR yourself. The headset must be ready in the OpenXR runtime selected for the game process. This mod does not change global runtime settings, install drivers or configure SteamVR. A missing runtime is reported on the desktop and returns the mode to Off.
2. **XR fixed** checks submission with a fixed visual camera and the runtime's eye separation/FOV. Inspect it briefly in the desktop mirror or headset without moving your head; this diagnostic mode intentionally does not make the camera follow head motion.
3. Switch to **XR tracked** for head rotation and positional tracking. Face forward when tracking first becomes valid. Native mouse yaw remains your body-heading control; headset pitch/roll supply the visual pitch/roll in this mode. Controller input starts Off; enable it separately as described below.
4. Click **Recenter in 5 seconds**, face forward, and wait for the countdown. Recenter resets position and yaw, preserving gravity/horizon rather than cancelling your physical head tilt. Reference-space changes also request a fresh anchor. Tracking loss submits no world layer; it does not invent an eye pose.
5. Check world scale while stationary. The default is **1 scene unit per meter**, not physically calibrated. If needed, switch Off, enter a value between **0.25 and 4** in the units/meter box, click Apply, and re-enter XR tracked. This scales physical head movement and runtime IPD together. It does not change world geometry.
6. Test Off, then re-enter XR to verify cleanup/recreation. The runtime is pumped with UI-only frames when a completed UI panel exists, otherwise empty frames while Viewpoint has no eligible world draw. A stopped runtime returns Off before native scene work; a failure after drawing has begun disarms the new modes and may cause Viewpoint's own error handler to disable Viewpoint. Preserve the log and restart after a rendering failure.

## UI test with the null headset

Select **XR fixed** or **XR tracked**. UI capture is automatic in both XR modes. No new hotkeys are assigned.

1. Open inventory, a context menu and the pause menu. Confirm readable text, transparent surroundings, correct orientation, and matching desktop/headset contents.
2. Open Viewpoint's loot panel and performance overlay, then its settings window using your existing controls (Delete is the inspected settings default for 0.1.5a-hotfix). Confirm each appears above the game UI.
3. Release the mouse with Viewpoint's middle mouse control. A white cross in a black square marks the desktop mouse position on the headset panel. Click and scroll in both native and Viewpoint windows; confirm actions happen once.
4. Close the windows and confirm their images disappear. Resize the game window, switch Off and back to XR, and check the panel redraws without stale text or desktop changes. Compare desktop appearance with XR Off.
5. Check the log for increasing `UI panels submitted=...` alongside `Projection pairs submitted=...`. Report any `[Project Viewpoint VR UI] Capture disabled` message.

UI comes from the previous completed desktop frame. Menus may therefore trail by one frame. The vanilla layer is retained across UI-throttled frames and expires after two seconds without an update. The mouse marker hides when the cursor is captured or the window loses focus. Third-party UI outside the captured draw events, the operating-system cursor artwork and the game's developer ImGui interface are not included. Returning to the main menu turns XR Off.

## Controller test (physical hardware deferred)

The null headset does not provide controller poses. Leave **Controllers Off** for normal null-headset regression; an enabled input mode should report left/right tracking false and produce no clicks or movement. No new keyboard hotkeys are assigned.

When controllers become available, follow [INPUT.md](INPUT.md): start with **UI pointer**, then test **Pointer + move/turn** in XR tracked. First release all buttons/sticks. Release Viewpoint's cursor with middle mouse or the mapped right-controller menu button. Aim at the panel, pull the right trigger to click, and move the right stick vertically to scroll. Verify clicks and scrolling in both inventory and Viewpoint settings. Test tracking/focus loss while holding a trigger and confirm release without a repeated click on recovery. Only then test walking and turning with the cursor captured. The new bottom-row dropdowns select Off/Snap/Smooth, snap angle (15/30/45/60/90 degrees) and smooth speed (30?240 degrees/second). Center the stick after each setting change; confirm one snap per deflection and continuous turning only in Smooth. Off/Pause restores ordinary rendering; **Controllers Off** disables controller actions separately.

## Persistent settings check

Choose Smooth, a different snap angle/speed, and Apply a world scale while Off. Restart the game and confirm all four values are restored in the panel while renderer/controller activation remains Off. Preferences live in `Project-Viewpoint-VR/settings.properties` under the Zomboid cache directory (normally `%USERPROFILE%\Zomboid`). Each successful turning change and accepted scale Apply saves immediately. Invalid or missing values fall back to defaults. A write failure displays a settings warning and retains the choice for the current session. Remove the preferences file while the game is closed to restore defaults.

## Scope and known limits

- UI is displayed as one transparent panel 1.5 meters ahead of the head, using desktop pixel layout and aspect ratio. The panel is at most 2 meters wide. Mouse/keyboard input remains available; the optional controller pointer targets this panel. Tracked hand models are not implemented. Native keyboard/mouse gameplay and picking still use the native player camera, not an independent headset aim ray.
- First person, on foot, single player, using native Viewpoint shaders. Switch Off before changing shader packs. Selecting an Iris/Minecraft shader pack suspends stereo and leaves normal Viewpoint rendering active; choose a native Viewpoint shader to resume. Third person/freecam/vehicles suspend the pair path; multiplayer is rejected.
- Eye targets retain the 0.05/400 clipping planes. Desktop eye images are capped at 1280 pixels on their longest side; XR source targets use the runtime's recommended dimensions capped at 2048, then scale to each runtime swapchain.
- Each headset eye is rendered with a symmetric frustum containing its requested asymmetric FOV, then cropped during GPU submission. This preserves the native symmetric sky/depth reconstruction. Crop boundaries are rounded to source pixels; edge error is below one source pixel.
- Continuous modes broaden the desktop-facing chunk and character filters and bypass room hiding. This retains geometry for independent head turns, but costs CPU/GPU time and does not expand the game's loaded world or remove all game visibility rules. The broadening expires if rendering stops and turns off with the mode.
- TAA, indirect lighting, volumetrics, clouds and shader-pack post passes remain disabled only inside stereo rendering. Color/gamma, sustained frame rate, transparency sorting, head movement near walls, rapid turns and full scene visibility need live testing. Physical head displacement is visual only and has no collision correction.
- Shift+Pause/Break retains the locally audited capture shortcut. Pause/Break alone is the emergency Off control. No F10 binding is used. Unrelated desktop-app shortcuts are outside the local game/mod audit.

Return `console.txt` lines containing `[Project Viewpoint VR]` and `[Project Viewpoint VR OpenXR]`, any errors, the selected mode, and what each eye showed. Useful headset evidence includes `Session state=...`, eye dimensions, and increasing `Projection pairs submitted=...` counts. Confirm continuous desktop rendering before assessing headset-specific issues.

Disable the mod and restart to remove its hooks. The package replaces no dependency JAR, Workshop file, SteamVR configuration or saved graphics setting. It bundles pinned LWJGL OpenXR bindings and the Windows OpenXR loader with their licenses; it reuses the game's LWJGL core/OpenGL.
