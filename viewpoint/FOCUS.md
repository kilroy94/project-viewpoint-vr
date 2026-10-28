# Steam Frame / SteamVR input focus

## 0.11.1 startup failure: runtime connection stalled (2026-10-28)

The user reports the game crashes when entering Viewpoint first person after selecting XR tracked. The latest console confirms 0.11.1, waits while Viewpoint is disabled, and stops at OpenXR initialization immediately after the Frame-extension message. It never logs the runtime name/session creation, an arm capture or physical-step acceptance in this run. There is no Java exception, JVM fatal-error report or matching Windows Application error found in the checked locations; the user subsequently confirmed that it froze and a full SteamVR restart fixed it.

SteamVR's matching XR client log starts at 18:49:18 with PID 16604 and repeatedly times out waiting for `SteamVR_Namespace` (21 namespace-reply warnings). The original vrserver PID 8376 remains running. Its log records a broken namespace connection and `CSharedResourceNamespaceServer shutting down` at 18:23:08.748, exactly when the earlier Background diagnostic probe failed its sandbox process-access check. That probe may have triggered the listener failure. Its intent was read-only and it invoked no setter, but that does not establish absence of runtime side effects. Existing connected clients continued working; the next game process could not establish a connection. Do not rerun that live probe.

The next required user action is a full SteamVR exit/restart and Frame reconnection, followed by a new game run with Viewpoint first person enabled before VR gameplay. Keep **Use Gamepad Inputs Off**. This order is a recovery/test instruction, not a demonstrated requirement that XR-before-first-person is inherently invalid. The user confirmed recovery after that full restart. This establishes recovery in this session, not an independently proven defect in the mod activation order. No new mod build or renderer/threading change is justified by these logs alone. Do not attempt to cancel a stuck native `xrCreateInstance` by destroying handles or moving the borrowed WGL context to a different thread.

## Observed 0.11.0 failure, reviewed 2026-10-28

The user reported that physical movement, arm tracking and swinging still did not work. The latest console confirms 0.11.0 loaded. Its OpenXR session entered states 1/2/3/4, but never state 5 (FOCUSED). It recorded 122 Body windows waiting for a tracked world or reporting XR not focused, zero rig/melee messages and no controller-profile queries. Five profile binding suggestions succeeded. The physical pad later appeared as native pad 0; the owned ID-15 device never registered/assigned in that run.

Controller sampling intentionally stops without XR input focus, so the missing profile queries/hand poses do not establish a binding or IK failure. Body movement is also focus-gated. This shared blocked state explains why none of the new features activated. It does not prove that the movement/rig code will behave correctly once focus is obtained; no live success is claimed.

The user already used Stream VR Without Game and launched Zomboid on the PC. That launch sequence was appropriate. Read-only SteamVR evidence showed the correct active OpenXR runtime, Zomboid promoted from OpenXRInstance to OpenXRScene, and Frame bindings loaded. Dashboard logs also showed Zomboid's desktop-game overlay (`valve.steam.desktopgame.108600`) being created/selected. The saved last external overlay referred to it.

The installed SteamVR defaults enable `dashboard.autoShowGameTheater`; the user configuration had no override. The local dashboard bundle reads this setting when a desktop game frame becomes alive and can place it in theater mode. The installed English UI labels it **Present Non-VR Applications on Theater Screen Upon Launch**, an Advanced setting. This is a supported local setting, not an inferred new OpenXR API.

## Applied reversible configuration change

With SteamVR and Zomboid stopped, set only `dashboard.autoShowGameTheater` to false in Steam's `config/steamvr.vrsettings`. All other parsed settings were preserved. The exact original bytes, proposed configuration and reviewed before-hash were retained in ignored workspace evidence. The apply step checked the target, unchanged before-hash and stopped processes, then read back the changed setting.

This disables automatic theater presentation of non-VR games globally in SteamVR. Manual theater use remains available. Restore the behavior by turning the setting On in SteamVR Settings > Dashboard with Advanced settings shown. Prefer the UI for later restoration so newer unrelated settings are not overwritten by an old whole-file backup.

No game/mod/Workshop or Zomboid user-data files were written. SteamVR and the game were not launched by the investigation. There is no new mod build or dependency pin change. Future source builds include this guide in the package. This is a plausible correction for the observed theater/focus conflict and needs a new user-run session to establish that it restores focus.

## Follow-up live test: theater override was insufficient

The user's 2026-10-28 session still never entered OpenXR state 5 with the theater override saved as false. The user confirmed an immersive 3D world with no visible SteamVR dashboard. SteamVR's dashboard log also records `return_to_game` and the remote dashboard closing. Therefore the lack of XR focus must not be presented as proof that the user left the dashboard open, and the theater override is not a confirmed fix. Native pad 0 could move the actor, while the XR hand/action path remained inactive.

The installed SteamVR 2.18.4 UI contains a separate per-application **Use Gamepad Inputs** setting. Its description explicitly selects Steam Input gamepad events instead of native VR controller input; its handler calls `SetAppVRInputPreference` for the selected Steam app. Valve's [SteamVR update notes](https://steamcommunity.com/app/250820/allnews/?l=english) describe defaulting Frame controllers to gamepad input for VR games considered not to support VR inputs. This is a plausible next routing condition to inspect for modded Zomboid, not a confirmed diagnosis: the current preference has not been read successfully.

While the game runs, inspect Now Playing > VR Controller settings for the current Zomboid/Viewpoint application. If **Use Gamepad Inputs** is On, turn it Off and return to the game. Check the console for state 5 and profile/hand sampling before assessing movement/IK/melee. This setting is separate from assigning the mod's native virtual gamepad in Zomboid. Do not edit live Steam configuration files to change it.

A workspace-only read-only OpenVR Background probe was attempted. The sandbox denied process access; an outside-sandbox retry stalled and was terminated by its exact diagnostic PID and command line. It produced no usable focus-owner snapshot. No game/SteamVR process was launched or terminated, and no runtime setter was invoked. Do not infer a focus owner from this failed diagnostic.

## Confirmed input-routing correction (2026-10-28)

The user found **Use Gamepad Inputs On**, switched it **Off**, and confirmed that hands now work. The same running 0.11.0 session then entered **FOCUSED/state 5** at frame 74892, with both controllers tracked and both profiles resolving to `/interaction_profiles/valve/frame_controller_valve`. The owned virtual gamepad registered as ID 15. Eleven rig logs recorded both tracked arms; the user report supplies the visual confirmation that logs alone cannot.

Body status became active independently of desktop focus and pad assignment. The log records accepted movement in at least two diagnostic windows (0.12035117 and 0.049122646 scene units, one moved tick each), alongside other windows with requested but no accepted displacement. This establishes that the movement path activated, not that room-scale movement/collision feels correct or is fully validated. Native pad remained -1 during these focused windows: the owned controller still needs enabling/assignment for its native gameplay actions.

No Melee events were recorded. The focused status explicitly reports that motion attacks need ID-15 assignment. Swing attacks and actual native combat remain unvalidated. Do not equate hand tracking with melee success.

Keep **Use Gamepad Inputs Off** for tracked-controller play; the mod supplies its own native gamepad bridge. This per-application routing switch fixed focus/hands in this session without a new mod build. The automatic-theater override alone had failed, and its necessity has not been established. Later state 4/3 transitions show focus loss; no permanent focus guarantee is claimed.

## Remaining gameplay failures after focus recovery

The user reports physical steps still fail, could not find the virtual pad in native options, and was unsure whether Diagnostics worked. The log confirms two diagnostic swings. A concrete within-tile collision-query error was subsequently found; the 0.11.1 package corrects it, provides an explicit native player-assignment button, and displays a persistent swing count. These corrections need a new in-game test; see [TESTING.md](TESTING.md) and [HYBRID-RIG.md](HYBRID-RIG.md).

## Next test

1. Start SteamVR and connect through Stream VR Without Game, then launch Zomboid on the PC as before.
2. Use the existing 0.11.0 package, first person/on foot, frame generation Off, and select VR gameplay. In SteamVR Now Playing > VR Controller settings for the current game, keep **Use Gamepad Inputs Off**. Return to the immersive game and dismiss any visible dashboard.
3. Check Body status. Success at this stage means **Body: active** and an OpenXR **Session state=5** log. Rendering/head tracking alone are insufficient.
4. Test a small physical step, then hands/held item. Only afterward test motion Diagnostics and Live. Live still needs Viewpoint VR Gamepad assigned and a melee weapon; see TESTING.md.
5. If the body still says XR not focused, preserve the log before changing more settings. If state 5 appears but movement/arms fail, their new Body/Rig logs can identify the next actual code failure.

Do not bypass focus checks or force an unfocused session to accept combat input. A visible scene can legitimately be behind another VR UI.
