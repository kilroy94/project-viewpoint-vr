# PZ3D VR mod guide

The preserved **`pz3d-vr-baseline`** source/package is **0.11.0**, including hand-proximity use. The [baseline implementation map](../../docs/PZ3D-BASELINE.md) documents the current architecture and renderer coupling. The numbered GitHub release remains 0.10.1; the preservation task publishes source and an annotated baseline tag, not a new numbered release. Headset validation gaps are retained below.

Current release: **[v0.10.1 prerelease](https://github.com/kilroy94/pz3d-vr/releases/tag/v0.10.1)**, adding stick turning and a separate ready/aim binding for Zomboid 42.21.0 and PZ3D 0.3.0. The published v0.9.0 package is for the older binaries. The prototype provides live OpenXR stereo and head tracking, the vanilla UI in VR, tracked first-person arms and held items, optional Touch-to-gamepad input, and experimental motion melee. Continuous desktop stereo and one-shot eye-image capture remain available without a headset or VR runtime.

Supported binaries are exactly Project Zomboid **42.21.0**, PZ3D **0.3.0**, and ZombieBuddy **2.3.2**, checked by SHA-256 rather than version strings alone. Use first-person, on-foot, single-player testing. This remains an experimental VR conversion. Live desktop stereo, simulated-headset output and UI have user confirmation, and a physical-headset tester confirmed earlier arm tracking. Later arm refinements, gamepad input and melee still need headset/in-game validation; see [VALIDATION.md](VALIDATION.md).

Start with [installation](#install-for-your-test) and the [project overview](../../README.md) for default shortcuts and feature summaries. All prototype shortcuts are remappable in **Options > Mods > PZ3D VR**. Close the game before replacing the local `PZ3DVRTest` folder; restart and approve the changed JAR if ZombieBuddy prompts. Sections below describe current behavior and identify the version that introduced each feature.

## Hand proximity interaction (0.11.0 workspace build)

This package adds a prototype for using nearby objects with a tracked hand. In **Options > Mods > PZ3D VR**, set **Hand proximity interaction** to **Diagnostics only** or **Live**, then choose **Interaction grip**: right (default), left, or either hand. The feature defaults to Off. Start XR, release the grip, reach within **25 cm** of the object's surface, and squeeze once. Release before another interaction. Diagnostics prints the selected object and action under `[PZ3D VR Use]` in `Zomboid/console.txt` without dispatching the action.

Target selection uses the tracked controller's palm-centered grip position and PZ3D's world geometry, native picking, visibility, and player reach checks. The existing PZ3D short-press handler then uses its native door/window, curtain, light-switch, world-item pickup, container UI, or contextual-action route as applicable. Looking at a different object does not change the hand-selected target. The code does not simulate the E key, teleport the character, or implement new object behavior. Geometry selection is conservative: an occluded surface or a point that does not pass native picking will not interact.

The selected grip is reserved while the feature is enabled, **including in menus and diagnostics**. Its virtual gamepad bumper is held released, and that grip cannot also serve as the VR ready/aim binding. Choose a separate ready/aim input, such as left trigger, or turn hand interaction Off to restore the grip. Physical gamepad bindings are unchanged. Return the controls to neutral after changing settings.

This first version handles short-press targets represented in PZ3D's world/item geometry. It does not add hold-to-climb, vehicle entry, corpse/animal targeting, remote grabbing, or a VR menu pointer. Native restrictions and contextual behavior still apply; it is not a promise that every vanilla E scenario is covered. Synthetic arms, tracking/focus loss, menus, pause, recentering, active timed actions/combat, traversal and multiplayer block hand use. A grip held during an interruption must be released again. If an interaction error disables the feature, its grip stays reserved until you set the feature Off.

Install the workspace `dist/PZ3DVRTest-0.11.0.zip` with the game closed. First test a door opening and closing while looking elsewhere, then a window, light switch, and container. Test each chosen hand, hold-to-prevent-repeat, SteamVR dashboard return, menus and synthetic preview. Automated tests verify input/scoping and copied-binary contracts; real headset/gameplay confirmation is still required.

## ZombieBuddy temporary-fix compatibility (0.10.1)

Accepts the exact verified B42.21 temporary-fix ZombieBuddy JAR as well as the original 2.3.2 JAR. The game may load ZombieBuddy from its game-root agent JAR rather than the Workshop copy; compatibility is checked against the actual loaded JAR. Unknown binaries still fail closed. No loader replacement or installation is included in this package.

Also changes the arm-reach slider label from a literal percent sign to the word "percent", avoiding the updated game's translation/formatting error when building the Mods settings page. The setting ID, range and saved value are unchanged.

With the game closed, replace the prototype folder with 0.10.1 and restart; approve the changed JAR if prompted. The earlier startup rejection occurs before XR initialization, so restarting SteamVR alone cannot fix it. The null-headset helper is unchanged.

To reproduce the additional copied-loader compatibility test, place your temporary-fix JAR at `reference/zombiebuddy/42.21-temporary-fix/ZombieBuddy.jar`, run `Test.ps1`, then `Test-ZombieBuddy.ps1`. Neither test runs loader/game entrypoints.

## Stick turning and separate ready/aim (0.10.0)

**Options > Mods > PZ3D VR > Stick turning** offers Off (default), Snap and Smooth. Snap angle: 15/30/45/60/90 degrees, default 30; smooth speed: 30-240 degrees/second, default 90. Snap once per sideways deflection, then center to rearm. Smooth speed scales with horizontal deflection; vertical input never pitches the camera.

**Turning input** selects Automatic, VR controllers or Assigned gamepad. Automatic prefers the player's assigned physical gamepad, otherwise VR controller input. Physical pads use native configured aiming axes/inversions; VR controllers use OpenXR sticks directly without requiring the virtual gamepad. Assign physical pads through native controller settings. Current VR stick bindings are Touch-profile bindings; other profiles are not newly added by this feature.

During focused first-person XR gameplay, the selected right stick is consumed for turning. **Hold ready/aim** selects left trigger (default), left bumper/grip, right bumper/grip or right-stick click. The chosen aim input is also withheld from native gameplay actions. Native UI routing, unsupported contexts and Turning Off restore native controls. Release aim and center the stick after changing settings, menus, disconnects or focus loss. Keyboard/mouse and unselected physical controllers keep their native paths.

Turning changes the shared PZ3D heading used by movement and the base camera, preserving pitch and physical head tracking. Head, hands and held attachments keep their existing common coordinate transform. Both motion-melee modes are disarmed during artificial turns and for 200 ms afterward; release the attack trigger after turning to rearm. Pending contact hits are cancelled. Simultaneous stick turning and motion melee is deliberately not supported by this first implementation. Native button attacks remain native. Calibration is not reset by turning.

Check `[PZ3D VR Turn]` in `console.txt` for settings or installation/runtime failures. First test turning with melee Off, then verify ready/aim, menu navigation and returning to neutral, and finally re-enable motion melee. Test actual physical and VR controllers separately. Fixture/bytecode tests cannot establish in-game comfort, body alignment, aim behavior or all menu transitions.

## Compatibility update (0.9.1)

Zomboid 42.21.0 is outside the old descriptor limits, and both its JAR and PZ3D 0.3.0 differ from the previous pinned binaries. This update changes the exact compatibility pins and adapts the contact-melee scenery guard to the new `CombatManager.processTreeHit` argument layout. The installed ZombieBuddy JAR is still the supported 2.3.2 binary. Unknown binaries continue to be rejected.

Replace the old prototype folder with the 0.9.1 package while the game is closed; do not just edit version limits in an older package. The internal `42.20.4` folder name is retained for installation continuity, but `mod.info` requires exactly 42.21.0. Dependencies remain separately installed. No game or Workshop files need modification.

## Contact-timed baseball bat pilot (0.9.0)

This opt-in pilot detects the **rendered bat's swept contact**, selects that zombie, and resolves through native combat as soon as the native attack state is ready. It does not wait for the animation impact event. The old animation-timed mode remains available.

In **Options > Mods > PZ3D VR > Motion melee prototype**, choose **Bat contact diagnostics (no attacks)** first, then **Bat contact-timed attacks** for gameplay. Equip the plain **Base.BaseballBat** (not nailed, metal, crafted or broken variants), start XR, release the right trigger, then hold it and swing into a **standing zombie**. Keep holding through resolution; release between attempts. Enable **Allow motion melee with gamepad input** for gamepad movement alongside contact attacks; RT is then reserved for this mode. Synthetic arms cannot initiate contact attacks.

`console.txt` contains `[PZ3D VR Contact]` entries for geometry acquisition, diagnostic contacts, accepted contacts, native resolution delay and completion. Diagnostics applies no damage. A `resolved` entry records a native collision call/hit-list count, not proof of actual damage. No new shortcut is required.

Geometry and scope:

- The bat capsule is derived from its mesh bounds and the same attachment/world matrices used for tracked held-item rendering, including scale, IK reach clamping and scene-origin offsets. It is not an exact mesh collider.
- Standing zombies use an approximate upright body capsule, not animated per-limb/head hitboxes. No positional headshot bonus is added.
- Sweeps subdivide endpoint motion at up to 2 cm intervals with a 1 cm tolerance; they approximate the path between samples. Fast/discontinuous tracking, stale samples, nonfinite geometry, queue overflow, missing geometry, focus/UI loss and equipment changes fail closed. Motion must exceed initial speed/travel thresholds in world space and relative to head translation, preventing stationary overlap and whole-body movement alone from attacking. These thresholds need headset tuning.
- Only the first valid zombie contact is considered per trigger hold, subject to a bounded reach guard, same-floor restriction, native readiness/recovery, and conservative line/solid-obstruction checks. Windows/closed doors block this pilot. No shoves, stomps, unarmed, stabbing weapons, scenery destruction, multiplayer or multi-hit sweeps.
- The contacted target replaces the native facing-based hit list only for this owned attack. Native damage/endurance/condition processing and recovery run; the later animation collision is suppressed, and unrelated native scenery-hit processing is excluded. Other players/ordinary attacks keep their original path.
- Contact is resolved on the simulation thread at native attack-state readiness. Pending contacts expire after **150 ms**, and are cancelled if the target moves more than 25 cm before resolution. This is contact-driven timing, not a promise of zero latency or of immediate damage during any animation state. The native animation still controls recovery.
- A miss does not initiate a native attack in this first pilot, so it does not incur the ordinary native missed-swing cost. No damage scaling from physical swing speed is added.

Geometry tests, synthetic native-combat fixtures and copied-class verification cannot prove in-game attachment fit, animation-hook coexistence or impact feel. Those require the user's headset test; the game was not launched during development. Installation and dependencies remain unchanged.


## Touch controllers as a conventional gamepad (0.8.0)

This opt-in bridge combines both Quest Touch controllers into **PZ VR Gamepad**, using native Zomboid controller bindings. No Windows virtual-controller driver is required. Standard OpenXR Touch bindings are used; the actual Quest/Steam Link profile and input delivery still need the user's hardware test.

1. Install the updated folder with the game closed. Start a disposable single-player game using the existing dependencies and start OpenXR.
2. In **Options > Mods > PZ3D VR > VR controllers as gamepad**, select **Input diagnostics only**, Apply, and check `[PZ3D VR Input]` and `Input profile` in `console.txt`. Both hands should report active; stick, trigger, squeeze and button values should change.
3. Select **Gamepad with diagnostics** for the first live test. Return both sticks/triggers/grips/buttons to neutral. In Zomboid's native controller settings, enable **PZ VR Gamepad**, then use native controller activation/assignment to control the existing player. The bridge does not automatically assign a player. Desktop input is needed for initial setup.
4. After verifying input, select **Gamepad** to disable periodic diagnostics. **Off** neutralizes the already registered device; its identity remains until game restart so dashboard/session interruptions do not repeatedly disconnect the player. Use Zomboid's normal return-to-keyboard controls when switching away from gamepad play.

| Touch control | Native gamepad input |
|---|---|
| Left/right thumbsticks | Left/right sticks; right stick retains native aiming, not camera turning |
| A/B/X/Y | A/B/X/Y |
| Left/right trigger | LT/RT |
| Left/right grip pressure | LB/RB, with press/release hysteresis |
| Left/right stick click | L3/R3 |
| Tap left Menu | Start, on release |
| Hold left Menu + left stick | D-pad; consumes left-stick movement while held |
| Hold left Menu + X | Back; consumes X while held |
| Right system button | Runtime-owned; no gamepad binding |

Menu can be reserved by the runtime; the alternate layer is unavailable if SteamVR does not deliver it. Neither Guide nor runtime system access is overridden. Optional snap/smooth turning and a separate ready/aim binding are available in 0.10.0; see the turning section above. Existing native UI input routing remains; entering game menus does not disable the emulated controller.

**Allow motion melee with gamepad input** (default unchecked) enables hybrid controls: movement and buttons remain native, while the selected motion-melee mode receives the right trigger exclusively. Native RT stays released whenever any motion-melee mode is active, including contact diagnostics/attacks, firearms and menus. Set Motion melee to Off to restore RT. With the checkbox unchecked, gamepad mode suppresses motion melee as before. Return controls to neutral after changing ownership. Turning gamepad mode Off restores the selected melee setting. Tracking/pose rendering remains separate from button acquisition. Stale input (250 ms), loss of focus/action availability, XR stop, and mode disable neutralize all inputs; resume requires neutral controls. Very short taps between native input samples may be missed.

The initial adapter reserves native controller slot **15** (the sixteenth slot). If a physical controller already occupies it, the bridge refuses registration. If one arrives later, the virtual pad drains neutral input, disconnects through native events, releases its assignment, and hands the slot back. Other physical controllers keep their native path. This conservative fixed-slot policy avoids dynamic reassignment; multiplayer/local co-op and physical hotplug coexistence still need in-game validation.

Diagnostics only creates no virtual device if one was not already registered. XR snapshots are capped at two per second; game-thread state logs at one per two seconds, with source sequence IDs. Normal Gamepad mode logs registration/profile/lifecycle changes only. Missing action support disables acquisition without intentionally changing stereo or arm tracking. The bridge and hybrid setting are included in v0.9.0.


## Motion-triggered armed melee (0.7.0)

Off by default. In **Options > Mods > PZ3D VR > Motion melee prototype**, choose **Diagnostics only (no attacks)** first and Apply. Equip an ordinary swing weapon in the primary hand. Release the right trigger, then hold it and make a deliberate translational swing with the right controller. Release the trigger between attempts. Diagnostics logs eligible gestures without initiating attacks. After reviewing those logs, choose **Live armed melee (animation-timed)** to request native attacks.

Keep the trigger held until the native attack finishes. Releasing it, losing tracking/focus, opening menus, changing equipment, or switching modes can suppress an attack that has not reached its collision event yet. Recenter and synthetic preview invalidate gesture input. Native keyboard/gamepad controls continue to work. No new keyboard shortcut is required. The right trigger is suggested for Touch, Index, Vive, and Windows MR profiles; simple-controller profiles use Select. Controller bindings can also be configured through the runtime where supported.

This first prototype supports ordinary one-handed, two-handed, and heavy swing weapons. Two-handed inventory equipment continues to use vanilla rules. It excludes unarmed attacks, firearms, throwing weapons, knives, spears, chainsaws, and floor attacks. It does not implement off-hand attacks, motion-selected attack animations, physical blade collision, or motion-based damage scaling. Aim using the existing character-facing controls; the headset and controller do not independently steer native attack direction.

Gestures request one native attack immediately on the simulation thread rather than entering PZ3D's delayed attack queue. Damage remains tied to the native animation collision event. Shove/grapple/floor fallbacks are rejected or have their collision suppressed for VR-owned attacks. Existing native weapon condition, endurance, cooldown, target filtering, and damage processing remain authoritative. Runtime errors disable the melee adapter without disabling stereo rendering.

Initial detector thresholds: at least 0.12 m of continuous motion, speed at least 1.2 m/s both in physical tracking space and relative to head translation; gaps over 120 ms and large tracking jumps reset detection. Requests/heartbeats expire after 150 ms. These are engineering defaults, not hardware-tuned gestures. Wrist-only rotation does not trigger this translational prototype. Whole-body translation and head motion alone are excluded. Synthetic hand preview never initiates combat.

Find **[PZ3D VR Melee]** lines in the game's `console.txt`. Correlated IDs distinguish `gesture`, `diagnostic`, `request`, `started`, `collision_event`, `collision_suppressed`, `rejected`, and `finished`. The collision event is not proof of a damaging hit. Compare gesture-to-start and gesture-to-collision delays during your first hardware test. No attacks should occur while changing settings or immediately after tracking returns with the trigger already held.

Physical-headset attack feel, thresholds, native animation timing, and actual in-game hook coexistence need user validation. Use a disposable single-player save. Dependencies and existing settings remain the same.

Implementation details and the earlier source investigation are in [the melee research report](../../research/motion-melee-feasibility.md). OpenXR trigger input uses the standard float action and suggested binding conversions in the [Khronos specification](https://registry.khronos.org/OpenXR/specs/1.0-khr/html/xrspec.html#input-suggested-bindings).

## Controller reach fitting (0.6.5)

Tracked arms can now extend beyond the avatar's native reach to meet controller targets. Extension happens only when needed, maintaining the native upper-arm/forearm proportions. The arm mesh stretches along the segments rather than merely moving the wrist; hand size, palm offsets, and held-item scale remain unchanged. Nearby targets use native segment lengths again.

**Options > Mods > PZ3D VR > Maximum arm reach (percent)** sets the limit: default **150%**, adjustable from 100% to 175%. Press Apply to save. 100% restores the native reach cap. This is bounded automatic extension with a manual fitting limit, not a measurement of your anatomical arm length. Hands still clamp beyond the configured maximum; increase it only if your normal full extension remains short. Larger values can visibly stretch sleeves/elbows. It does not increase native melee range. Contact mode follows the rendered bat position, including this fitting, while retaining its separate reach guard.

After updating, recenter upright using the countdown. Extend and retract each arm, rotate the wrists, then repeat while kneeling and holding an item. Verify palms stay at controller grips, items retain their size, and arms retract without drift. Real headset fit remains to be confirmed.

## Physical kneeling: arm-root height (0.6.4)

Tracked arm roots now move with physical headset height changes relative to the last recenter. Kneeling lowers the shoulders used by the arm solver; standing back up restores them. The adjustment uses the same tracking-space conversion as the hands and is added to the current native arm pose. Native camera/crouch motion is not counted a second time. Tracking interruptions preserve the height reference; explicit recenter establishes a new reference, so calibrate in your intended neutral posture.

This is a first-person visual arm-root correction. Torso, pelvis, legs, collision, and gameplay crouch state remain native; this is not full-body IK. Shoulder/torso mesh transitions may still look stretched and need headset inspection. Arm lengths and maximum reach are unchanged; player/avatar reach calibration is separate. Untracked hands keep their existing native-pose fallback.

Test by recentering upright, kneeling with both controllers held in front, then standing again. Repeat while holding an item and after a SteamVR dashboard interruption. Check clothing, shoulder seams, palm alignment, and that native crouching does not double the correction. Physical-headset confirmation remains pending.

## Hands-free recenter countdown (0.6.3)

Press **Recenter headset** once, release the keyboard, then face forward and hold both controllers neutrally. A **five-second countdown** appears in the vanilla UI (also presented in VR), followed by a completion message. Another press restarts the countdown. Stopping XR or switching synthetic-preview mode cancels it. After the countdown, calibration waits for VR focus and any hands that were tracked when you requested it. Headset-only/simulated use still supports recentering without controllers.

The calibration uses your pose when the countdown finishes, not the pose when you press the shortcut. The same remappable shortcut now schedules this delay rather than calibrating immediately. Physical-headset verification of the countdown overlay remains pending.

## Hand tracking resume fix (0.6.2)

Temporary tracking loss and SteamVR dashboard/focus interruptions now preserve the existing controller-to-hand rotation calibration and palm offset. Missing hands still use native animation until valid tracking returns. Runtime reference-space changes reanchor the camera without recalibrating the hands from an arbitrary controller pose.

Initial calibration still occurs on the first valid tracked pose. To correct an existing bad alignment, hold the controllers comfortably forward in a neutral pose and use **Recenter headset** (default Ctrl+Shift+Alt+Scroll Lock). Starting a new XR session and changing synthetic-preview mode also reset arm calibration. This update does not replace that initial calibration with a fixed anatomical controller mapping.

Retest with both hands: open the SteamVR dashboard, move/rotate the controllers while it is open, close it, and return to neutral. Repeat with one controller briefly losing tracking and with a held item. Orientation should return consistently without needing another recenter. This sequence has numerical regression coverage; physical-headset confirmation remains pending.

## Remappable shortcuts (0.6.1)

Open **Options > Mods > PZ3D VR**. Each action has a keyboard key picker and a modifier selector (None, Ctrl, Shift, Alt, or combinations). Press the key alone in the picker; choose modifiers in the separate selector. Press **Apply** to activate and save changes. Use **Clear** in the key picker to disable an action. Defaults retain the shortcuts documented below; all shortcut examples in these instructions assume defaults.

The four bindings are Toggle OpenXR, Recenter headset, Toggle synthetic arms / desktop stereo, and Save stereo PNG pair. The preview shortcut retains its context-dependent behavior: synthetic arms while XR is active, desktop stereo otherwise. Bindings persist through the game's built-in `ModOptions.ini`; no additional options mod is required.

Shortcuts are suppressed while Options is visible or a text field is active. Release the action key after changing settings or restoring window focus. Modifiers must match exactly; adding/removing modifiers while holding the key does not trigger another action. Identical PZ3D VR chords are disabled until made distinct. Native game and other mod controls are not consumed; select keys that do not conflict with your other controls. Mouse buttons and modifier-only bindings are not supported.

Close the game before replacing the local mod folder with this package, then restart and approve the changed JAR if ZombieBuddy prompts. To verify: remap an action, Apply, check its old shortcut no longer works, try the new shortcut, then restart the game and check it persists. The settings UI still needs this user-run in-game check.

## Palm alignment and held items (0.6.0)

Active OpenXR controller grip poses now target the center of the character's palm, rather than the hand bone's wrist origin. The palm point is estimated halfway between the wrist and the average non-thumb finger bases, in the model's own hand coordinates. Rigs without suitable finger bases use a small offset along the forearm direction. The offset rotates with the hand, so turning the controller pivots around the palm. Natural arm lengths and reach limits still apply. This is a skeletal estimate; exact palm fit can vary with models and controller profiles.

Held items remain visible and follow their tracked hand. The renderer preserves the captured native grip offset, orientation, mesh transform and scale, then applies the corresponding bone's visual motion. Primary/secondary prop bones follow right/left hands respectively. Named model attachments use their declared bone. Captured nested attachments inherit their parent's motion exactly once. Losing tracking restores that hand and its items to their native animation. Unrelated/untracked attachments remain native.

This is still visual tracking: controller buttons, attacking, hit detection, gun aim/projectiles, flashlight illumination direction and world interactions use the existing game behavior. Two-handed items follow their native owning hand; the other hand is not constrained to a second grip. PZ3D's existing capture/visibility rules still apply, including items it omits while scoped. Fingers retain native animation, and shadows prepared before the stereo pair retain native poses.

Install `PZ3DVRTest-0.11.0.zip` with the game closed and approve the updated JAR if prompted. Start the runtime, enter first-person PZ3D on foot in single player, hold controllers comfortably forward, and enable XR with **Ctrl+Shift+Scroll Lock**. Initial valid tracking aligns controller orientation to the native hand orientation; subsequent rotation turns the wrist. **Ctrl+Shift+Alt+Scroll Lock** recalibrates both camera and hand alignment. Controller position needs no button press.

Test empty hands first: rotate each controller in place and check that the palm stays at the grip position. Then equip a one-handed item, a secondary-hand item, and a two-handed weapon. Check item alignment while translating and rotating each hand, after swapping equipment, after recentering, and after losing/reacquiring one controller. Native shadows and simulated attacks are not evidence of tracked interaction.

Without controllers, start simulated SteamVR and XR as usual, then press **Ctrl+Alt+Scroll Lock without Shift** to animate synthetic hand targets on the actual character and held-item meshes. Press again to return to physical poses. With XR off, the same shortcut toggles desktop stereo. Both former F9 shortcuts were removed in 0.5.1 because vanilla's debug Seam Editor handles F9 even with modifiers. Release Scroll Lock between actions.

One owned arm pose and attachment set is used by both eyes at the head's predicted display time. Original palettes and attachment matrices stay untouched. Arm palette/mask references are restored in the pair's `finally`; a narrowly version-gated fourth renderer hook substitutes owned attachment matrices only at upload. Attachment overrides are cleared on success and failure. Output palette buffers are reused. Controller lookup is included in `LOCATE` timing; retargeting is included in `PREPARE`.

Suggested bindings cover OpenXR simple controllers, Oculus Touch, Valve Index, HTC Vive and Microsoft motion controllers. Each hand falls back independently when tracking is invalid, inactive or not fully tracked; losing application focus disables physical hand poses. Logs in `%USERPROFILE%\Zomboid\console.txt` include `[PZ3D OpenXR] Tracked arms: left=..., right=...`, `Arm IK active: palm-centered grips...`, and the initial attachment override count. Unsupported skeletons or pose failures log `Arm IK disabled; native pose restored` once and disable IK until XR restarts.

The user reports that an acquaintance confirmed physical motion tracking in the preceding prototype. The new palm alignment and held-item rendering still need their in-game test; automated checks do not establish visual fit, hardware latency or comfort.

## Vanilla UI panel (0.4.0)

When XR is enabled, the vanilla UI is automatically submitted as one transparent, flat, head-following panel in front of the scene. Its center is 1.5 metres ahead, with width at most 2 metres and height at most 1.3 metres, preserving the desktop UI aspect ratio. Layout and keyboard/mouse input remain unchanged. Open inventory and other vanilla windows normally; no extra hotkey or UI configuration is required.

The panel reuses PZ3D's completed `UiLayer` snapshot and copies it once per submitted stereo pair into a separate OpenXR swapchain. UI callbacks are not rerun. It displays the latest completed UI image, which can be one UI refresh behind the current game state. UI refresh rate remains the game's existing setting. The panel is included in SteamVR's Headset Window; the game's own stereo mirror keeps its existing desktop UI overlay.

This first version captures content already in the vanilla UI texture. The OS/software mouse cursor drawn afterward, debug ImGui, and PZ3D world-space captions/crosshair are not added to the VR panel. There is no controller pointer or changed aiming behavior. The panel exists while the first-person XR session is running; it does not extend XR to the main menu or unsupported views. A not-yet-ready UI texture gives a world-only frame, then resumes automatically when available.

Install with the game closed and approve the new JAR if prompted. Enable XR with **Ctrl+Shift+Scroll Lock**, open inventory, and check that text is upright, the transparent regions reveal the scene, and menu updates appear in both eyes. Close/reopen the inventory and toggle XR off/on to check cleanup. The log reports `Vanilla UI panel: ...`, includes `UI_COPY` timings, and reports submitted UI panels on XR shutdown. The user confirmed the UI works in-game with the simulated Headset Window. Physical-headset stereo comfort and latency remain unmeasured; earlier controller arm tracking has a user report of success.

## Timing diagnostics (0.3.2)

Replace the local test-mod folder with `PZ3DVRTest-0.11.0.zip` while the game is closed, then approve the new JAR if prompted. Controls are unchanged. Enable XR with **Ctrl+Shift+Scroll Lock**, remain in the same scene for about 20 seconds, then walk/turn for about 30 seconds. Toggle XR off to flush the final partial report. Reports appear automatically in the game's `console.txt` with `[PZ3D XR Timing]`; there is no extra hotkey.

Each five-second window reports successful stereo submissions per elapsed second (`stereoHz`), XR calls, submitted/skipped/failed counts, the runtime's latest predicted display period, and calls whose total wall time exceeds that period (`overBudget`). This is not a compositor dropped-frame count. `stereoHz` measures application submission, not presentation to the display.

Stages report **average / approximate p95 upper bound / maximum**, in milliseconds:

| Stage | Scope |
|---|---|
| INTERVAL | Spacing between calls into XR, including calls that skip rendering |
| OUTSIDE | Previous XR call completion to next entry: game scheduling, desktop presentation, other work, plus reporting overhead |
| TOTAL | Entire XR call, including waits, eye rendering/copies, and submission |
| WAIT_FRAME / BEGIN_FRAME / END_FRAME | Runtime frame pacing and submission calls |
| LOCATE | Runtime view/head pose lookup and associated setup |
| PREPARE | Pair setup, shared preparation and checks through first eye entry |
| LEFT / RIGHT | Each eye's draw and consistency checks, excluding destination copies |
| MIRROR_COPY | Desktop eye texture copy, per eye |
| XR_IMAGE_WAIT | Swapchain acquire and image wait, per eye |
| XR_COPY_RELEASE | XR blit, GL state restoration/flush and image release, per eye |
| UI_COPY | UI swapchain allocation when needed, texture copy, acquire/wait/release, once per pair |
| MIRROR_PRESENT | Drawing the stereo pair into the desktop framebuffer; excludes the game's later window swap |

All values are CPU **wall-clock elapsed time**, including any driver blocking, not GPU execution timings or pure CPU utilization. Copy stages are per-eye averages, while TOTAL is per pair. These stages do not exhaustively partition TOTAL. The histogram uses fixed memory and 0.25 ms buckets; p95 beyond 511.75 ms is explicitly shown as `>511.75`, while mean and maximum retain the actual duration. Only five-second summaries and a final partial summary are printed; no GPU fences, readback or per-frame disk writes are added. Existing SteamVR compositor logs remain the complementary source of GPU/presentation evidence. Physical-headset latency remains unmeasured.

## OpenXR integration (0.3.1)

Version 0.3.1 moves XR off vanilla time-control/debug-editor keys. Hold Ctrl+Shift and press Scroll Lock to toggle XR; add Alt to recenter. Release Scroll Lock before another action. Changing modifiers while it remains held never triggers a second action.

Install this version with the game closed, replacing the existing local test-mod folder. Approve the updated JAR if ZombieBuddy prompts. The package includes pinned LWJGL OpenXR 3.4.1 bindings and the Windows loader; do not install a separate OpenXR SDK or replace the game's LWJGL libraries.

| Shortcut | Action |
|---|---|
| Ctrl+Shift+Scroll Lock | Start/stop OpenXR with the current runtime |
| Ctrl+Shift+Alt+Scroll Lock | Recenter the current headset pose onto the game camera |
| Ctrl+Alt+Scroll Lock (without Shift) | Toggle synthetic arms in XR; desktop stereo when XR is off |
| Ctrl+Shift+F10 | Save a synthetic stereo pair while OpenXR is off |

Start the runtime before enabling XR. With a physical headset, use its working OpenXR runtime. For the current no-headset test, use the workspace's [simulated SteamVR helper](SimulatedRuntime.ps1):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ".\experiments\zombiebuddy-harness\SimulatedRuntime.ps1" -Action Start
```

Run that from the project workspace, then launch the game normally yourself, enter first-person PZ3D on foot, and press **Ctrl+Shift+Scroll Lock**. The helper starts only SteamVR, writes its simulation settings/logs under its own `build` directory (the workspace when run from here), and restores its process-local environment afterward. Close any existing SteamVR session before using it. SteamVR is already the registered OpenXR runtime on this machine; the helper does not change the registry. It was tested with a separate client using that default runtime and no configuration-path override. The helper uses the installed Python interpreter and SteamVR API DLL to keep a non-rendering overlay client connected until `-Action Stop`. This prevents the server's 20-second idle exit while the game loads. The isolated profile also keeps the simulated headset awake and disables the SteamVR dashboard and automatic game theater so they cannot cover the scene. These settings apply only to this simulated runtime. Keep `RuntimeKeepalive.py` and `HeadsetWindow.py` next to the PowerShell script. The keeper adds a normal draggable title bar to SteamVR's simulated Headset Window, including windows recreated later. It preserves the rendering-area dimensions. `-WindowX` and `-WindowY` set its initial position; afterward, drag the title bar normally. A `Status` check reports a missing keepalive.

The log should show `[PZ3D OpenXR] Runtime: SteamVR/OpenXR`, `Session created on existing context`, and increasing `Projection pairs submitted` counts. The desktop shows the actual runtime-view pair. The null driver supplies fixed simulated poses; physical head movement is not expected. Ctrl+Shift+Scroll Lock again tears down the session; Ctrl+Alt+Scroll Lock controls the independent desktop preview when XR is off. After testing, run the same helper with `-Action Stop` (or `Status` to inspect it).

The adapter borrows the game's WGL context, locates both eyes and the head at predicted display time, maps them relative to a recentered game-camera anchor, draws one coherent pair and submits a projection layer. Runtime poses/FOV in layer metadata remain unchanged. It reuses GPU mirror targets and performs no PNG readback in XR mode. The prototype renders at the existing game framebuffer resolution, then scales into each runtime-recommended swapchain extent. This is a temporary rendering-resolution policy, not native-resolution VR optimization.

XR stops on unsupported views, minimization, runtime errors/stopping, or a heartbeat detecting that PZ3D stopped drawing. Failure attempts ordinary rendering without repeating fresh-frame native preparation. The native runtime can block in frame timing/image waits; forced termination or runtime stalls cannot be made interruption-proof by this prototype. No extra graphics context, render thread, or game simulation loop is created.

Head movement changes the visual camera only. Existing keyboard/mouse movement, body aiming, and torch direction remain game-controlled. Scale is provisionally one scene unit per metre; hardware calibration, latency, comfort, gamma, head-related culling/shadow coverage, and performance remain unvalidated. Version 0.4.0 also submits the vanilla UI as the simple head-following panel described above. Use this as a world-rendering test, not a complete VR gameplay interface.

## Continuous desktop stereo preview (0.2.0)

Press **Ctrl+Alt+Scroll Lock** in first-person PZ3D, on foot, to toggle continuous side-by-side stereo in the **existing game window**. Left eye is on the left; right eye is on the right. Press the same chord again to return to the ordinary view. Both full-aspect images are fitted into the window with black bars, without stretching. Windowed mode is convenient for desktop inspection. No headset or SteamVR is needed.

This mode renders a new stereo pair on each eligible fresh or retained draw and reuses two GPU targets. It performs no PNG encoding or CPU image readback unless you press **Ctrl+Shift+F10** to save a pair. Resizing recreates the targets. Unsupported camera/vehicle contexts use ordinary rendering and release targets when the draw hook next runs; supported draws resume stereo. The preview starts off after each game launch. A draw failure disables it until restart and attempts ordinary retained rendering.

The game UI remains one desktop overlay, so this is a world-rendering preview rather than a VR UI. The same omitted effects listed below stay omitted while stereo is enabled. Two scene views and diagnostic consistency checks add cost; frame-timing diagnostics help separate rendering work from runtime waits, but hardware performance is not established. This is not headset-tracked output or a separate floating spectator window. The user confirmed smooth live stereo and consistent scene details in version 0.2.0. Version 0.3.1 has now submitted more than 6,600 stereo projection pairs from the live game to simulated SteamVR, with working output confirmed by the user. Physical-headset stereo comfort and latency remain unmeasured; earlier controller arm tracking has a user report of success.

The log reports `[PZ3D VR Mirror] ON`, the first completed pair, progress every 600 pairs, and `OFF` with the completed count. Close the game before replacing the local mod folder; approve the changed JAR if prompted after restart.

## Install for your test

1. Close Project Zomboid. Extract `PZ3DVRTest-0.11.0.zip` into your local Zomboid mods directory, normally `%USERPROFILE%\Zomboid\mods`. The resulting descriptor should be `mods\PZ3DVRTest\42.20.4\mod.info`, with a sibling `PZ3DVRTest\common` directory. Do not put it in the Steam game directory or replace either existing mod JAR.
2. Enable **ZombieBuddy**, **PZ3D**, and **PZ3D Stereo Capture Test [Java]**, in that order, for a new disposable single-player test save. Keep other mods disabled for this first test. ZombieBuddy and PZ3D remain the existing installations.
3. The harness JAR is unsigned local development code. If ZombieBuddy presents its Java-mod approval dialog, review and approve this particular `PZ3DVRTest.jar`. The adjacent package `SHA256.txt` identifies the built JAR. No preload permission or global policy change is required. If your loader policy blocks unsigned code outright, the harness will remain unavailable; the package does not bypass that policy.
4. Launch the save yourself. Enter PZ3D with **Insert**, use first person, and remain on foot. Look at a nearby object with more distant scenery behind it.
5. Press **Ctrl+Shift+F10 once**. The capture can briefly stall while PNGs are written. Watch the console for `[PZ3D VR Test] Captured stereo pair:` and its directory. A Ctrl+Shift+F10 request made before entering first person stays pending until a supported draw occurs.

Outputs are under the game's configured cache directory, normally:

```text
%USERPROFILE%\Zomboid\PZ3D-VR-Test\<timestamp-id>\
    left.png
    right.png
    capture.properties
```

The properties file records the scene version, frame generation, prepared body/palette fingerprint, eye positions/matrices, one preparation, two copies, and successful completion of the extra lease bracket. It distinguishes failed captures from successful image writes. These counters are diagnostic evidence, not a claim of visual correctness.

Compare the PNGs: nearby objects should move horizontally more than distant ones; actors should be in the same animation pose. The capture intentionally omits sky rendering, outlines, contact-shadow overlays, tracers, crosshair, captions, and chunk debug. These features remain available on ordinary frames. Existing game UI is not included in the eye PNGs.

After a successful capture you can press Ctrl+Shift+F10 again. After a capture failure the harness disables further captures until restart, records the error, and attempts ordinary retained-style drawing without repeating fresh native preparation. Preserve the entire capture directory and the relevant `console.txt` lines for review.

To remove: close the game, disable the test mod, and remove its local `PZ3DVRTest` folder. Restarting removes the in-memory transformations. Captures can be kept or deleted separately. No game/Workshop files are replaced by this package.

## Build and verification

In this source workspace:

```powershell
.\experiments\zombiebuddy-harness\Build.ps1
.\experiments\zombiebuddy-harness\Test.ps1
```

The builder produces `dist\PZ3DVRTest-0.11.0.zip`, an unpacked `dist\PZ3DVRTest` folder, and `dist\SHA256.txt`. It uses the portable JDK and copied reference JARs already present. It never installs the mod or launches the game. `Test-XR.ps1 -Mode xr -NullRuntime` exercises the packaged XR backend in isolation after `Test.ps1`; `-Mode missing` checks unavailable-runtime fallback. Test fixtures and transformed proprietary reference classes are excluded from the mod JAR.

The tests exercise real JVM retransformation with an original synthetic renderer, including all-target activation, mismatch rollback, inactive rendering, one-shot requests, unsupported views, recursive entry protection, success/failure reports, lease cleanup, and capture failure isolation. A separate process defines and retransforms eleven actual copied render, melee and controller target classes **without initializing them, constructing a Frame, or invoking any game/mod entry point**. A standalone hidden OpenGL context tests the real capture helper: separate eye copies, image orientation, PNG writing, and texture/framebuffer/pixel-buffer state restoration.

These checks do not replace your in-game test. The original package loaded through ZombieBuddy successfully, but its F8 handler produced no capture request or images. The corrected trigger subsequently produced a live 2560x1440 stereo pair with one preparation and two copies; both images were visually inspected. Earlier physical arm tracking has user confirmation; the new combat/input behavior and later tracking refinements remain unverified on hardware.

The source workspace has `experiments/zombiebuddy-harness/VALIDATION.md` with the test counts and local evidence paths.

## Implementation and boundaries

`Main` obtains the existing ZombieBuddy instrumentation handle using the same loader field used by PZ3D's own `ChunkProbe`. It verifies the three JARs from their actual code-source locations, then installs one schema-preserving transformer for `Renderer$Frame`, `StreamFade`, `TreeRenderer`, and `Renderer`. Activation requires all four transformations to succeed. Failed installation removes the transformer and retransforms the targets to roll back its changes.

Incoming class bytes are compared with normalized originals, preserving executable instructions while ignoring debug/stack-map attributes and JVM constant-pool/method ordering. Mismatches visible to this transformer disable captures. This cannot detect a different transformer installed later that changes bytes after this transformer has seen them; keep the first test limited to the three listed mods.

An enabled live mirror or a pending capture intercepts `Frame.draw`; the original `Frame.render`/retained consumer and frame acceptance remain in place. The bridge's active-pair guard allows the inner draw to reach the once-per-pair/per-eye adapter without recursive capture. Native simulation, producer capture, and UI are not replayed. On a capture exception, the adapted draw records the failure instead of calling PZ3D's global failure handler; ordinary draws retain that handler.

Eye separation is 0.064 **PZ3D scene units**, not calibrated metres. The test uses the original camera direction/FOV and synthetic parallel-eye offsets. OpenXR pose conversion and swapchain submission are now implemented experimentally; controller UI interaction, hardware calibration, optimized pacing, and broad live resource-lifetime confidence remain later work. Frame-timing diagnostics are implemented; hardware performance still needs evaluation.
