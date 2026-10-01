# Project Viewpoint VR 0.4.1 â€” first desktop capture test

This is a **manual diagnostic**, not playable VR. Automated tests and a standalone hidden OpenGL test have passed. The mod has **not been tested inside Project Zomboid**. No headset or SteamVR is needed.

## Requirements

- Project Zomboid **42.21.0**.
- Viewpoint **0.1.3** (Workshop item 3809306528).
- ZombieBuddy **2.3.2**, either the original pinned JAR or the audited B42.21 temporary-fix JAR.

The loader checks exact binary hashes, not just version labels. Unknown builds leave capture disabled. The ZIP contains this project's classes and Lua only; obtain the dependencies separately.

## Install and capture

1. Close the game. Extract the ZIP into `%USERPROFILE%\Zomboid\mods`. The descriptor should end up at `ProjectViewpointVR\42\mod.info`, with no extra duplicate folder around it.
2. Use a disposable single-player test save. Enable **ZombieBuddy**, **Viewpoint**, and **Project Viewpoint VR - Stereo Capture [Java]**. Disable PZ3D and the old PZ3D VR test mod for this save. Approve this diagnostic's Java JAR if ZombieBuddy prompts.
3. Load the save, enable Viewpoint with **Insert**, and use first person while on foot. Keep the game window focused and stand still for the first capture. Do not use third person, freecam, vehicles or multiplayer.
4. Hold **Shift**, press and release the **Pause/Break** key, then release Shift. Do not hold Ctrl, Alt or a Windows key. A short pause during readback/PNG writing is expected. The status overlay and `console.txt` report the result. Ordinary rendering resumes on the next frame; there is no continuous stereo toggle.
5. Open `%USERPROFILE%\Zomboid\Project-Viewpoint-VR` (or the equivalent folder beneath your custom Zomboid cache directory). A successful `capture-...` folder contains `left.png`, `right.png`, `stereo.png`, and `capture.txt`.

Each eye preserves the viewport aspect ratio, capped at 1280 pixels on its longest side. `stereo.png` places left and right images side by side. The desktop receives the left-eye world image for that one frame. The PNGs contain the world only, without the game's UI.

The 0.4.1 shortcut replaces F10, which conflicts with game screenshots and Viewpoint developer controls. No Pause binding was found in the installed game Lua, Workshop/local mod Lua, Viewpoint input code or saved key bindings checked on 2026-09-30. This does not cover unrelated desktop applications or future mods. A keyboard with a Pause/Break key is required for this shortcut.

## What to check and return

Capture a scene with a nearby doorframe or furniture edge and distant terrain. Check that both eyes contain the same objects and poses, with a small positional difference for nearby objects. Neither eye should be black, vertically flipped, obviously clipped or missing characters/trees. Confirm normal rendering continues afterward.

Return the complete capture folder, relevant `[Project Viewpoint VR]` lines from `console.txt`, and a short description of anything wrong. If capture fails or Viewpoint turns off, keep the log and restart before retrying. A `.pending-...` folder is an incomplete disk write and must not be treated as a successful capture.

## Deliberate limitations

The diagnostic uses a synthetic 0.064 scene-unit eye separation and symmetric camera projections. This is not a validated physical scale. Head tracking, OpenXR, controllers, VR hands and VR UI are absent. TAA, indirect lighting, volumetrics, clouds and shader-pack post effects are suppressed only for the captured pair, so its appearance can differ from ordinary Viewpoint. Native picking runs once from the first eye; this test does not establish stereo interaction accuracy or full visibility coverage.

To remove the diagnostic, disable it in the save's mod list and restart the game. No dependency JAR, Workshop file, SteamVR setting or saved graphics property is replaced by the package. Captures remain in the output folder until you remove them.
