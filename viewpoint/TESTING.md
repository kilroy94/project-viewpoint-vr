# Project Viewpoint VR 0.11.0 - hybrid rig prototype

This experimental build implements continuous desktop stereo, OpenXR headset output, tracked head rotation/translation, and recentering. Version 0.4.1 produced two complete in-game stereo captures inspected on 2026-09-30. The user has since confirmed continuous desktop and null-headset tests without issues. On 0.9.0 the user confirmed stereo, head tracking and physical movement with Steam Frame; starting SteamVR first resolved OpenXR create-instance error -2. The user also confirmed the 0.6.1 renderer update and UI work. The user confirmed 0.10.0 thumbstick walking but physical steps did not move the actor. The independent movement, tracked rig and motion attack changes in this build need their first live test.

**Compatibility:** this package targets Viewpoint **0.1.6.1**, the installed Workshop update audited for this build. It replaces the old 0.1.5a-hotfix target. Future updates must be audited before their binaries are accepted; do not bypass the hash check.

## Install

1. Close Project Zomboid. Extract `ProjectViewpointVR-0.11.0.zip` into `%USERPROFILE%\Zomboid\mods`, replacing the previous ProjectViewpointVR files. The descriptor is `ProjectViewpointVR\42\mod.info`.
2. Use Zomboid **42.21.0**, Viewpoint **0.1.6.1**, and ZombieBuddy **2.3.4**. Exact binary checks remain mandatory. Approve the updated Java JAR if ZombieBuddy prompts.
3. Enable those mods in a disposable single-player save. For an existing save, use **Load > select the save > Choose Mods** and explicitly enable **Project Viewpoint VR - Desktop + OpenXR [Java]** there; enabling it only at the main menu does not enable it for that save. Disable PZ3D and the old PZ3D VR mod for this save. Enter Viewpoint first person, on foot. Viewpoint's inspected default toggle is **O**; use your established control if different.
4. The **Project Viewpoint VR** buttons appear near the upper left. Use Viewpoint's **middle mouse button** to free/capture the cursor when you need to click them. Turning preferences and world scale are saved across restarts; renderer and controller activation always start Off.

If the buttons are missing, first check the save's own mod list. In the observed case, the Java startup message appeared at the main menu but the mod was absent from the save, so the Lua controls did not load. After changing the save's mod selection, restart the game. Pause/Break alone only turns a mode Off and has no visible effect when already Off.

## Compatibility update 0.9.0

Targets the installed Zomboid 42.21.0, Viewpoint 0.1.6.1 and ZombieBuddy 2.3.4 binaries only. **Turn frame generation Off in Viewpoint before using stereo/XR.** If it is enabled, the adapter suspends stereo and delegates ordinary rendering. Native vendor temporal upscaling (FSR 3 / XeSS / DLSS) is inactive inside stereo; ordinary spatial resizing remains available. Panini is disabled only for the borrowed stereo scene and restored afterward, preserving headset eye projection. Saved graphics properties are not changed. Iris packs remain outside stereo support.

Replace the installed mod with this package; an updated source checkout alone does not install it. Physical rendering/tracking on 0.9.0 is confirmed by the user. Thumbstick walking is user-confirmed on 0.10.0. Its physical body following failed. Version 0.11.0 changes that path and adds a tracked rig; these changes are not yet confirmed in-game.

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
2. Open Viewpoint's loot panel and performance overlay, then its settings window using your existing controls (Delete is the inspected settings default for 0.1.6.1). Confirm each appears above the game UI.
3. Release the mouse with Viewpoint's middle mouse control. A white cross in a black square marks the desktop mouse position on the headset panel. Click and scroll in both native and Viewpoint windows; confirm actions happen once.
4. Close the windows and confirm their images disappear. Resize the game window, switch Off and back to XR, and check the panel redraws without stale text or desktop changes. Compare desktop appearance with XR Off.
5. Check the log for increasing `UI panels submitted=...` alongside `Projection pairs submitted=...`. Report any `[Project Viewpoint VR UI] Capture disabled` message.

UI comes from the previous completed desktop frame. Menus may therefore trail by one frame. The vanilla layer is retained across UI-throttled frames and expires after two seconds without an update. The mouse marker hides when the cursor is captured or the window loses focus. Third-party UI outside the captured draw events, the operating-system cursor artwork and the game's developer ImGui interface are not included. Returning to the main menu turns XR Off.

## Controller pointer test

The null headset does not provide controller poses. Leave **Controllers Off** for normal null-headset regression; an enabled input mode should report left/right tracking false and produce no clicks or movement. No new keyboard hotkeys are assigned.

With physical controllers, follow [INPUT.md](INPUT.md): start with **UI pointer**, then test **Pointer + move/turn** in XR tracked. First release all buttons/sticks. Release Viewpoint's cursor with middle mouse or the mapped right-controller menu button. Aim at the panel, pull the right trigger to click, and move the right stick vertically to scroll. Verify clicks and scrolling in both inventory and Viewpoint settings. Drag a control/item, then interrupt tracking or switch controller input Off: the release should stay at the last controller position instead of jumping to the physical mouse. Test tracking/focus loss while holding a trigger and confirm release without a repeated click on recovery. Only then test walking and turning with the cursor captured. The new bottom-row dropdowns select Off/Snap/Smooth, snap angle (15/30/45/60/90 degrees) and smooth speed (30?240 degrees/second). Center the stick after each setting change; confirm one snap per deflection and continuous turning only in Smooth. Off/Pause restores ordinary rendering; **Controllers Off** disables controller actions separately.

## Steam Frame focus check after the failed 0.11.0 test

The latest user test loaded 0.11.0 but never reached OpenXR FOCUSED/state 5, so physical movement, hand acquisition and motion attacks remained blocked. The investigation found Zomboid's desktop-theater overlay selected in SteamVR and automatic non-VR theater presentation enabled by default. **Present Non-VR Applications on Theater Screen Upon Launch** has now been turned Off in this user's SteamVR configuration, with a backup. Other users can find this under SteamVR Settings > Dashboard with Advanced settings shown. This affects automatic flat-game theater display; manual theater use remains available.

Use the existing 0.11.0 build and the same Stream VR Without Game -> launch Zomboid on the PC sequence. Return to the VR game after enabling VR gameplay. First confirm **Body: active** / **Session state=5**, then test movement/hands. The configuration correction needs live confirmation and is not proof of working rig/combat behavior. See [FOCUS.md](FOCUS.md) for evidence, scope and restoration.

## Hybrid movement and rig test (0.11.0)

1. Start SteamVR, connect both Frame controllers and close the SteamVR dashboard. Use first person, on foot, single player, with Viewpoint frame generation Off. Click **VR gameplay (tracked + native gamepad)**. Use **Recenter in 5 seconds** while facing forward with your hands comfortably in front.
2. **Test physical movement first, before controller assignment.** With the sticks centered, step 20?30 cm on a clear floor. The actor should move with you beyond a 4 cm lean allowance. Desktop mouse capture and virtual gamepad assignment are no longer required for this. Check `Body: active`; if it says **XR not focused**, return focus to the VR game/close the SteamVR dashboard. A session can render tracked stereo while it is visible but not focused.
3. Repeat while using the working native stick controls. Physical movement should coexist with them rather than cancelling them or moving the camera twice. Test a closed wall: the actor should stop/slide through native collision checks, not pass through it. This prototype does not physically block or fade the headset at walls. Pause/blocked native actions stop physical-body updates. An offset over 1.5 m needs recentering. Physical stepping does not yet drive a walking gait, footsteps or endurance accounting.
4. Inspect each arm, clothing and held item. Move a single hand, rotate the wrist and inspect both eyes. Hands/items should follow grips while the body remains native. Recenter in a neutral pose if palm/wrist alignment is wrong. Interrupt tracking briefly: that side should return to native animation, then recover. These are newly ported rig hooks and have not been visually validated on the real avatar.
5. For the owned snap/smooth turn, buttons and motion attacks, enable **Viewpoint VR Gamepad** in native controller options and assign it to your existing player (ID 15). A generic/native **pad 0** can supply working stick controls without being this tracked pad. It is preserved; physical body following does not depend on replacing it. Motion Live requires the owned pad to avoid competing with pad 0's raw trigger. Release all controls after assignment.
6. Use **right-stick click** or **middle mouse** to switch between gameplay and the UI cursor. With the cursor free, right aim/trigger/vertical stick provide pointer/click/scroll. Stick/attack input is neutral while using UI; physical following can continue during unpaused menus. Custom settings/modal UI also block gameplay actions. Check focus/loss recovery and Pause/Break Off before combat.
7. Equip an ordinary melee weapon. Leave **Motion melee: Off** to test normal native button combat. Select **Diagnostics** to inspect gestures without attacks: release RT, hold it, then deliberately swing the right hand. Look for a `diagnostic swing` log. Moving your whole body or rotating your head with the hand stationary should not count.
8. Select **Motion melee: Hold right trigger + swing (native attacks)** when ready to test it. Hold native ready/aim (Viewpoint's inspected default is LT), release then hold RT and swing. Each gesture sends one short native RT pulse; native attack targeting, recovery, animation and damage rules apply. Release RT between swings. Artificial turning briefly disarms motion attacks. Off restores native RT after neutral input. Ranged/bare-hand equipment cannot emit live motion pulses.

This is a swing-triggered attack prototype, **not weapon-contact damage**. A visible weapon crossing a zombie does not independently select a hit; native aim/targeting rules still matter. Controller-directed firearms, physical shoves, grabbing, two-handed constraints and full body IK are not implemented.

Return `[Project Viewpoint VR Body]`, `[Project Viewpoint VR Rig]`, `[Project Viewpoint VR Melee]`, controller profile/input and Gamepad lines from `console.txt`. Body windows include requested/accepted displacement, total physical distance and moved-tick counts, so short steps are not missed between reports. Describe whether the actor moved, the hands/item looked correct and a native attack actually occurred. Logs alone do not prove visual fit, correct targets, damage or comfort. [HYBRID-RIG.md](HYBRID-RIG.md) describes the integration and limits.

## Persistent settings check

Choose Smooth, a different snap angle/speed, and Apply a world scale while Off. Restart the game and confirm all four values are restored in the panel while renderer/controller activation remains Off. Preferences live in `Project-Viewpoint-VR/settings.properties` under the Zomboid cache directory (normally `%USERPROFILE%\Zomboid`). Each successful turning change and accepted scale Apply saves immediately. Invalid or missing values fall back to defaults. A write failure displays a settings warning and retains the choice for the current session. Remove the preferences file while the game is closed to restore defaults.

## Scripted controller diagnostic

Version 0.9.0 adds an explicitly armed null-headset diagnostic with two text reports. Follow [DIAGNOSTIC.md](DIAGNOSTIC.md) for the Zomboid test panel and Viewpoint settings VR tab. It tests dedicated UI widgets, not inventory transfers. Keep Controllers Off; the simulator temporarily supplies UI-only input and leaves preferences unchanged. Stop/Pause cancels. No diagnostic starts automatically.

## Scope and known limits

- UI is displayed as one transparent panel 1.5 meters ahead of the head, using desktop pixel layout and aspect ratio. The panel is at most 2 meters wide. Mouse/keyboard input remains available; the optional controller pointer targets this panel. Gameplay mode retargets the native arms and held items and aligns body/aim angles with head orientation, but world picking still uses the native player camera; independent headset/hand interaction rays are not implemented.
- First person, on foot, single player, using native Viewpoint shaders. Switch Off before changing shader packs. Selecting an Iris/Minecraft shader pack suspends stereo and leaves normal Viewpoint rendering active; choose a native Viewpoint shader to resume. Third person/freecam/vehicles suspend the pair path; multiplayer is rejected.
- Eye targets retain the 0.05/400 clipping planes. Desktop eye images are capped at 1280 pixels on their longest side; XR source targets use the runtime's recommended dimensions capped at 2048, then scale to each runtime swapchain.
- Each headset eye is rendered with a symmetric frustum containing its requested asymmetric FOV, then cropped during GPU submission. This preserves the native symmetric sky/depth reconstruction. Crop boundaries are rounded to source pixels; edge error is below one source pixel.
- Continuous modes broaden the desktop-facing chunk and character filters and bypass room hiding. This retains geometry for independent head turns, but costs CPU/GPU time and does not expand the game's loaded world or remove all game visibility rules. The broadening expires if rendering stops and turns off with the mode.
- TAA, indirect lighting, volumetrics, clouds and shader-pack post passes remain disabled only inside stereo rendering. Color/gamma, sustained frame rate, transparency sorting, head movement near walls, rapid turns and full scene visibility need live testing. Physical head displacement has no collision correction. Gameplay mode adds independent horizontal collision movement, with a lean allowance; other modes retain visual-only displacement. Physical footsteps/gait/endurance and vertical movement are unfinished.
- Shift+Pause/Break retains the locally audited capture shortcut. Pause/Break alone is the emergency Off control. No F10 binding is used. Unrelated desktop-app shortcuts are outside the local game/mod audit.

Return `console.txt` lines containing `[Project Viewpoint VR]` and `[Project Viewpoint VR OpenXR]`, any errors, the selected mode, and what each eye showed. Useful headset evidence includes `Session state=...`, eye dimensions, and increasing `Projection pairs submitted=...` counts. Confirm continuous desktop rendering before assessing headset-specific issues.

Disable the mod and restart to remove its hooks. The package replaces no dependency JAR, Workshop file, SteamVR configuration or saved graphics setting. It bundles pinned LWJGL OpenXR bindings and the Windows OpenXR loader with their licenses; it reuses the game's LWJGL core/OpenGL.
