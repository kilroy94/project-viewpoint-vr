# Controller input (0.11.0)

This is an opt-in implementation for the exact supported Viewpoint build. The user confirmed stereo and physical head tracking with Steam Frame on 0.9.0, but reported disconnected body tracking and no controller gameplay. Its log showed Controllers Off throughout, right aim tracking present and an early null mouse-array exception. Version 0.10.0 fixes that startup exception and adds a first gameplay mode. Automated coverage does not establish working physical bindings or comfortable gameplay.

## Hybrid rig update

The user confirmed thumbstick walking, but the 0.10.0 body-follow implementation did not work in-game. Version 0.11.0 removes physical following from the stick/animation vector and gives it a separate bounded native collision query/translation. It no longer depends on desktop focus, mouse capture, controller assignment or neutral sticks. Physical and virtual movement can occur together. Input status reports the blocking gate. See [HYBRID-RIG.md](HYBRID-RIG.md) for the exact ownership, head/hand spaces, native snapshot integration, motion attack modes and limitations.

Gameplay mode now includes tracked arms and held items. Motion melee is explicitly Off / Diagnostics / Live, starts Off, and requires the owned virtual pad assigned for Live. Holding RT arms a gesture; a deliberate right-hand swing generates a native RT pulse while native LT ready/aim and left-stick movement remain available. This is animation-timed native combat, not physical weapon-contact damage.

## Accessing the controls panel in first person / VR

Opening the inventory alone does not release Viewpoint's mouse capture. In the installed user configuration, **press the middle mouse button (click the wheel / MOUSE3)** to toggle the free cursor; click it again to return to mouse look. In VR gameplay with focused controller tracking, **press the right thumbstick down (R3)** for the same toggle. This XR-side shortcut works before the virtual gamepad is assigned. With the cursor free, use the desktop mouse or aim the right controller at the UI and use its trigger to click. The Project Viewpoint VR panel is on the left; its **Assign VR gamepad to player 1** button is below the motion-melee selector. Return to gameplay after assignment and release the controls to rearm.

The middle-button binding is Viewpoint's remappable `keys.cursor`; the current local setting was read as `MOUSE3`. A user who remaps that native action must use their chosen binding. Inventory windows are not treated as modal dialogs that automatically free the cursor.

## Gameplay mode

**VR gameplay (tracked + native gamepad)** selects XR tracked and controller mode 3 together. SteamVR must already be running. In 0.11.1, click **Assign VR gamepad to player 1** in our panel to enable and bind the owned native ID-15 controller through Zomboid's takeover path. This explicit action may replace player 1's previous pad binding. Other devices/players are preserved. The native controller options are an alternative when the device appears there. The bridge registers only when XR input is available and that slot is empty; it does not install a driver or rewrite the physical controller registry. Registration alone never changes an existing player assignment; the panel button explicitly requests takeover of player 1. Native Viewpoint controller switching may activate an enabled pad; status reports actual player binding. Renderer/controller activation is not persisted.

| Frame control | Gameplay effect |
|---|---|
| Left stick | Head-relative native walking |
| Right stick sideways | Configured snap/smooth turning; native pad look axes remain neutral |
| Right stick click | Toggle cursor capture; reserved from native R3 actions |
| Right A/B/X/Y | Native A/B/X/Y |
| Left D-pad | Native D-pad and hat |
| Left View / right Menu | Native Back / Start; system buttons remain reserved |
| Left/right trigger | Native LT/RT; Viewpoint's default ready/aim is LT |
| Left/right shoulder or grip | Native LB/RB |
| Left stick click | Native L3 |

Actual button actions follow the user's native Zomboid/Viewpoint bindings. While the cursor is released, the right ray/trigger/stick operate the UI and the gamepad publishes neutral controls. B is native cancel in gameplay mode, so right stick click replaces the earlier B cursor toggle. Custom settings, modal UI, pause, invalid ownership, lost focus or stale input suppress gameplay. Both tracked aim poses and a fully neutral sample are required before gamepad actions rearm.

Head orientation is applied only to native movement/body/aim consumers: FreeLook yaw, CrosshairAim yaw/pitch and Controls hand pitch. The renderer's base Look fields stay unchanged, avoiding double head rotation. This supplies angular native aiming, not an independently tracked weapon or interaction ray. Native picking/crosshair depth retains its ordinary camera origin and requires live assessment.

Head displacement beyond a 4 cm lean allowance requests up to 0.12 scene units per update through PolygonalMap2 collision and loaded-floor/adjacent-tile guards. Only accepted physical displacement is consumed in XR metres. Pending native stick movement is preserved. The per-snapshot compensation prevents an older render frame from using a newer simulation offset. Missing XR focus, stale tracking, pause, native action/movement blocks and unsafe tiles inhibit it; UI cursor release alone does not. Offsets above 1.5 m need recentering. This is horizontal body movement, not head collision, walking gait/footstep/endurance integration, physical crouch-state control or full body IK.

Steam Frame support enables `XR_VALVE_frame_controller_interaction` only when advertised and suggests `/interaction_profiles/valve/frame_controller_valve`. Frame's four face buttons are on the right and its D-pad/View button on the left. Paths follow [Valve's controller documentation](https://github.com/ValveSoftware/Unity/blob/main/com.valvesoftware.openxr.utils/Documentation~/index.md) and [Steam Frame input documentation](https://partner.steamgames.com/doc/steamhardware/steamframe/input). Core Touch/Index/WMR/simple profiles remain available if the extension is absent. One rejected profile suggestion no longer disables the entire controller action set.

The native gamepad mapper and registry overlay reuse authored PZ3D-era code without its renderer, melee or hand-interaction dependencies. Off/loss publishes neutral input and retains identity. A real device claiming slot 15 drains and disconnects the virtual pad, removes only its own assignment and returns the slot. Logs report profile selection separately from pose tracking, normalized controller inputs, native button delivery/assignment and body following, at bounded intervals.

The earlier ArmIk/ArmRig math now adapts native 16-float snapshots before Viewpoint's own 12-float packing. Hands and held items follow tracked grip poses; originals remain untouched and a failed preflight publishes no overrides. Native target picking, contact damage and hand proximity use remain further work.

## UI pointer and diagnostic move/turn modes

This section describes the older pointer/move diagnostic modes. Gameplay mode and motion ownership are described above.

The on-screen panel adds **Controllers Off** (startup default), **UI pointer**, and **Pointer + move/turn**. Controller activation lasts for this game session. Off/Pause stops XR and clears all sampled input; a controller selection can resume after re-entering XR, with controls first returned to neutral. Desktop stereo has no controller acquisition.

| Control | Effect |
|---|---|
| Right controller aim | Ray to the existing UI panel when Viewpoint's cursor is released |
| Right trigger | Left mouse press/hold/release on that panel |
| Right stick up/down | Scroll while pointing at the panel; repeats every 180 ms |
| Right B (Touch/Index), menu (WMR/simple) | Toggle Viewpoint cursor capture during unpaused gameplay |
| Left stick | Walk relative to native body heading in Pointer + move/turn, XR tracked, cursor captured |
| Right stick left/right | Selected snap or smooth turn in the same gameplay mode; center stick to arm |

**Turning settings:** the panel provides Off / Snap / Smooth, snap angles of 15, 30, 45, 60 or 90 degrees, and smooth speeds of 30?240 degrees/second in 15-degree steps. Defaults are Snap, 30 degrees and 90 degrees/second. Turning settings and world scale are saved automatically to `Project-Viewpoint-VR/settings.properties` under the game's Zomboid cache directory and restored across restarts. Renderer and controller activation still start Off. Return the stick to neutral after changes. Smooth speed scales with horizontal stick deflection outside a 0.3 deadzone, uses elapsed game-input time, caps a single update at 50 ms, and disarms after a gap over 250 ms. Vertical-dominant stick input does not turn. Turning Off retains pointer, scrolling and walking.

Middle mouse retains Viewpoint's native cursor toggle. No new keyboard shortcuts are assigned. Trigger does not attack or interact with the 3D world. Keyboard movement takes priority over stick movement. Menus/paused gameplay inhibit walking/turning. Mode changes, context changes, stale samples, focus loss and tracking loss require controls to return to neutral. Trigger thresholds are 0.65 press/0.25 release; walking has a 0.2 radial deadzone.

Suggested OpenXR profiles: Steam Frame (optional extension), Oculus Touch, Valve Index, Microsoft motion controller and Khronos simple controller. The simple profile has select/menu buttons but no sticks, so it supplies pointer/click without scrolling or locomotion. Other controller support depends on runtime binding/remapping. The controller status reports tracked left/right aim poses; a null headset normally reports neither. Both aim and grip poses are acquired. Gameplay mode now retargets the existing character arms and held items.

## Native integration

A session-owned action set is attached before session begin. Poses are located relative to the VIEW/head space at predicted display time, with valid and tracked position/orientation required. Samples are immutable across render/game threads and expire after 250 ms. An epoch preserves focus/tracking loss even if sampling recovers before the next game input tick. Acquisition failure disables controller acquisition for that XR session while preserving world submission; Off/re-enter rebuilds it.

The head-relative ray intersects the same z=-1.5 UI plane and aspect-preserving size as the submitted quad. A cyan beam is drawn in each owned eye image using runtime eye/head transforms before the asymmetric crop. The captured UI shows a high-contrast hit marker. UI-only idle frames retain the marker but have no world projection beam. Capture lag remains one completed desktop frame.

The original three exact-pinned call sites extend the existing Hooks/ImGuiFrame transforms, without adding native targets or changing class schema:

- After MouseButtons.pin in Hooks.mouseUpdated, merge the virtual pointer/button into native Mouse state, before SettingsWindow.blockGameMouse and LootMenu.mouse consume it. Maintain physical button history and release virtual holds after loss. No OS cursor movement or synthetic OS input is sent.
- Before Controls.moveVector in Hooks.inputMoveVector, overlay a zero native movement vector only. Native Viewpoint conversion supplies body-relative walking; snap turn changes its native yaw.
- After ImGuiFrame.input and before newFrame, overlay normalized cursor/button/wheel values in that frame's active ImGui context. Physical input runs exactly once. Viewpoint's native settings blocker still prevents game clicks behind its window.

The diagnostic input modes require a focused game window; gameplay mode also accepts fresh focused XR tracking with an unfocused desktop. All modes require an eligible Viewpoint first-person single-player character on foot and fresh XR data. Pointer use additionally requires a recently published UI panel and released cursor. Walking/turning additionally requires a recent tracked world frame and no blocking menu. Renderer/session loss clears input. Exceptions disable the controller layer and retain original callbacks; disable/re-enable the controller mode after resolving an input error.

## Validation and remaining work

ControllerTest exercises panel geometry, click hysteresis, ray misses, neutral rearming, stale/focus/tracking loss, scroll repeat, movement deadzones, diagonal speed limits, snap-turn/menu edges, physical/virtual button merging and bridge invocation order/failures. RayGpuTest checks real cyan pixels for both eye offsets and restores framebuffer, viewport, VAO/program and compatibility GL state. All twenty copied targets are verified/retransformed without game/mod initialization.

Physical binding selection, controller orientation, in-game inventory/settings clicks and scrolling, movement direction, snap-turn direction, focus/runtime recovery and comfort remain live acceptance tests. Inventory grabbing, controller-directed firearm aim, contact damage, vehicles and multiplayer are not implemented. Tracked arms/items and opt-in swing-triggered native attacks are present in gameplay mode, pending live tests. No SteamVR or game launch is part of automated validation.


## Simulated native input integration

The standalone `sim.InputIntegration` suite feeds poses and buttons through the production InputBridge and NativeInput adapters. Authored doubles supply physical mouse snapshots, window focus, player/menu state, and an ImGui IO surface. Test consumers record press/drag/release events and their positions. Fixture classes are compiled separately, checked for fixture origin, and excluded from the mod package. The game JAR supplies math dependencies; game/mod entry points are never executed.

Coverage includes vanilla and ImGui dragging, pixel mapping at different display sizes, UI routing changes while holding a trigger, physical mouse priority, wheel isolation and single consumption, keyboard movement priority, pause/menu locomotion blocking, focus/tracking/stale-sample loss, Off and session disconnect. The existing deterministic turning tests cover rates and repetition without sleep timing. These are adapter integration tests, not real inventory widgets, a running ImGui context or physical OpenXR devices.

Version 0.7.3 corrects ImGui drag release after controller loss/Off: release uses the last controller point for that frame, then physical input resumes. It also discards queued controller wheel input when the controller target is no longer valid. Version 0.8.0 added an explicitly armed in-game diagnostic; see [DIAGNOSTIC.md](DIAGNOSTIC.md).
