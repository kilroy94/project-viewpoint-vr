# Steam Frame / SteamVR input focus

## Observed 0.11.0 failure, reviewed 2026-10-28

The user reported that physical movement, arm tracking and swinging still did not work. The latest console confirms 0.11.0 loaded. Its OpenXR session entered states 1/2/3/4, but never state 5 (FOCUSED). It recorded 122 Body windows waiting for a tracked world or reporting XR not focused, zero rig/melee messages and no controller-profile queries. Five profile binding suggestions succeeded. The physical pad later appeared as native pad 0; the owned ID-15 device never registered/assigned in that run.

Controller sampling intentionally stops without XR input focus, so the missing profile queries/hand poses do not establish a binding or IK failure. Body movement is also focus-gated. This shared blocked state explains why none of the new features activated. It does not prove that the movement/rig code will behave correctly once focus is obtained; no live success is claimed.

The user already used Stream VR Without Game and launched Zomboid on the PC. That launch sequence was appropriate. Read-only SteamVR evidence showed the correct active OpenXR runtime, Zomboid promoted from OpenXRInstance to OpenXRScene, and Frame bindings loaded. Dashboard logs also showed Zomboid's desktop-game overlay (`valve.steam.desktopgame.108600`) being created/selected. The saved last external overlay referred to it.

The installed SteamVR defaults enable `dashboard.autoShowGameTheater`; the user configuration had no override. The local dashboard bundle reads this setting when a desktop game frame becomes alive and can place it in theater mode. The installed English UI labels it **Present Non-VR Applications on Theater Screen Upon Launch**, an Advanced setting. This is a supported local setting, not an inferred new OpenXR API.

## Applied reversible configuration change

With SteamVR and Zomboid stopped, set only `dashboard.autoShowGameTheater` to false in Steam's `config/steamvr.vrsettings`. All other parsed settings were preserved. The exact original bytes, proposed configuration and reviewed before-hash were retained in ignored workspace evidence. The apply step checked the target, unchanged before-hash and stopped processes, then read back the changed setting.

This disables automatic theater presentation of non-VR games globally in SteamVR. Manual theater use remains available. Restore the behavior by turning the setting On in SteamVR Settings > Dashboard with Advanced settings shown. Prefer the UI for later restoration so newer unrelated settings are not overwritten by an old whole-file backup.

No game/mod/Workshop or Zomboid user-data files were written. SteamVR and the game were not launched by the investigation. There is no new mod build or dependency pin change. Future source builds include this guide in the package. This is a plausible correction for the observed theater/focus conflict and needs a new user-run session to establish that it restores focus.

## Next test

1. Start SteamVR and connect through Stream VR Without Game, then launch Zomboid on the PC as before.
2. Use the existing 0.11.0 package, first person/on foot, frame generation Off, and select VR gameplay. Return to the actual VR game and dismiss any remaining dashboard/theater view.
3. Check Body status. Success at this stage means **Body: active** and an OpenXR **Session state=5** log. Rendering/head tracking alone are insufficient.
4. Test a small physical step, then hands/held item. Only afterward test motion Diagnostics and Live. Live still needs Viewpoint VR Gamepad assigned and a melee weapon; see TESTING.md.
5. If the body still says XR not focused, preserve the log before changing more settings. If state 5 appears but movement/arms fail, their new Body/Rig logs can identify the next actual code failure.

Do not bypass focus checks or force an unfocused session to accept combat input. A visible scene can legitimately be behind another VR UI.
