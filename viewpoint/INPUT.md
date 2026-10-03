# Controller input (0.8.0)

This is an opt-in implementation for the exact supported Viewpoint build. The user confirmed the 0.6.1 renderer and UI, but has no physical headset tester currently. Automated results below do not establish working hardware bindings or comfortable gameplay.

## Controls

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

Suggested OpenXR profiles: Oculus Touch, Valve Index, Microsoft motion controller and Khronos simple controller. The simple profile has select/menu buttons but no sticks, so it supplies pointer/click without scrolling or locomotion. Other controller support depends on runtime binding/remapping. The controller status reports tracked left/right aim poses; a null headset normally reports neither. Both aim and grip poses are acquired, but no hand meshes or skeletons are rendered.

## Native integration

A session-owned action set is attached before session begin. Poses are located relative to the VIEW/head space at predicted display time, with valid and tracked position/orientation required. Samples are immutable across render/game threads and expire after 250 ms. An epoch preserves focus/tracking loss even if sampling recovers before the next game input tick. Acquisition failure disables controller acquisition for that XR session while preserving world submission; Off/re-enter rebuilds it.

The head-relative ray intersects the same z=-1.5 UI plane and aspect-preserving size as the submitted quad. A cyan beam is drawn in each owned eye image using runtime eye/head transforms before the asymmetric crop. The captured UI shows a high-contrast hit marker. UI-only idle frames retain the marker but have no world projection beam. Capture lag remains one completed desktop frame.

Three exact-pinned call sites extend the existing Hooks/ImGuiFrame transforms, without adding native targets or changing class schema:

- After Controls.pinMouse in Hooks.mouseUpdated, merge the virtual pointer/button into native Mouse state, before SettingsWindow.blockGameMouse and LootMenu.wheel consume it. Maintain physical button history and release virtual holds after loss. No OS cursor movement or synthetic OS input is sent.
- Before Controls.moveVector in Hooks.inputMoveVector, overlay a zero native movement vector only. Native Viewpoint conversion supplies body-relative walking; snap turn changes its native yaw.
- After ImGuiFrame.input and before newFrame, overlay normalized cursor/button/wheel values in that frame's active ImGui context. Physical input runs exactly once. Viewpoint's native settings blocker still prevents game clicks behind its window.

Input requires a focused game window, eligible Viewpoint first-person single-player character on foot and fresh XR data. Pointer use additionally requires a recently published UI panel and released cursor. Walking/turning additionally requires a recent tracked world frame and no blocking menu. Renderer/session loss clears input. Exceptions disable the controller layer and retain original callbacks; disable/re-enable the controller mode after resolving an input error.

## Validation and remaining work

ControllerTest exercises panel geometry, click hysteresis, ray misses, neutral rearming, stale/focus/tracking loss, scroll repeat, movement deadzones, diagonal speed limits, snap-turn/menu edges, physical/virtual button merging and bridge invocation order/failures. RayGpuTest checks real cyan pixels for both eye offsets and restores framebuffer, viewport, VAO/program and compatibility GL state. All eleven copied targets are verified/retransformed without game/mod initialization.

Physical binding selection, controller orientation, in-game inventory/settings clicks and scrolling, movement direction, snap-turn direction, focus/runtime recovery and comfort remain live acceptance tests. Hand models, inventory grabbing, weapons, melee, vehicles and multiplayer are not implemented. No SteamVR or game launch is part of automated validation.


## Simulated native input integration

The standalone `sim.InputIntegration` suite feeds poses and buttons through the production InputBridge and NativeInput adapters. Authored doubles supply physical mouse snapshots, window focus, player/menu state, and an ImGui IO surface. Test consumers record press/drag/release events and their positions. Fixture classes are compiled separately, checked for fixture origin, and excluded from the mod package. The game JAR supplies math dependencies; game/mod entry points are never executed.

Coverage includes vanilla and ImGui dragging, pixel mapping at different display sizes, UI routing changes while holding a trigger, physical mouse priority, wheel isolation and single consumption, keyboard movement priority, pause/menu locomotion blocking, focus/tracking/stale-sample loss, Off and session disconnect. The existing deterministic turning tests cover rates and repetition without sleep timing. These are adapter integration tests, not real inventory widgets, a running ImGui context or physical OpenXR devices.

Version 0.7.3 corrects ImGui drag release after controller loss/Off: release uses the last controller point for that frame, then physical input resumes. It also discards queued controller wheel input when the controller target is no longer valid. Version 0.8.0 adds an explicitly armed in-game diagnostic; see [DIAGNOSTIC.md](DIAGNOSTIC.md).
