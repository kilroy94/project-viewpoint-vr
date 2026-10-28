# Hybrid VR rig: 0.11.1

The user confirmed thumbstick walking in 0.10.0, but physical steps did not move the actor. The recorded session selected the Frame interaction profile for both hands and received stick/trigger input. It never logged an active body-follow pass or an assigned ID-15 virtual pad; native Viewpoint later activated pad 0. This does not establish which gate blocked every attempted physical step. It does establish that rendering/controller acquisition alone did not provide working room-scale gameplay.

## 0.11.1 corrections and live evidence

The user confirms tracked hands after turning SteamVR **Use Gamepad Inputs Off**, but reports physical steps still fail. The 0.11.0 console records two recognized diagnostic swings; native attacks remain unverified. The registered virtual controller was not found by the user in the native settings UI, so 0.11.1 adds an explicit assignment button in our panel rather than relying on that discovery path.

The installed square collision matrix starts with all 27 bits set, and neighbor recalculation explicitly skips the zero-offset/self entry. The previous adapter queried that self entry on every within-tile physical step, incorrectly rejecting ordinary motion. Native `IsoMovingObject.DoCollide` checks adjacency only when the square changes. The corrected adapter likewise queries adjacency only for a tile transition, while keeping the capsule collision, loaded-floor, solid-tile and distance guards for every requested step. This is a concrete static mismatch, backed by a fixture with a blocked self entry; it still needs a physical-headset retest.

The panel assignment button validates the exact owned registry object, a living single-player character and initialized native Lua controller data. Its user-triggered action enables ID 15 and follows the native Lua/Java player-takeover and inventory-routing path. It never creates a character or silently assigns during registration. Native menu visibility and this button's actual in-game behavior remain unvalidated. Persistent diagnostic recognition counts are displayed separately from the transient motion gating status.

## Device tracking and native locomotion

A VR rig separates its virtual tracking origin from poses measured inside the physical play space. See [Valve's public Player example](https://github.com/ValveSoftware/steamvr_unity_plugin/blob/master/Assets/SteamVR/InteractionSystem/Core/Scripts/Player.cs) and [Unity's locomotion overview](https://docs.unity3d.com/Packages/com.unity.xr.interaction.toolkit@3.0/manual/locomotion.html). These are framework examples, not evidence of Half-Life: Alyx's proprietary implementation.

This mod keeps Zomboid's actor as the gameplay body. Native stick/keyboard movement translates it normally. Physical head motion requests an additional bounded movement through the installed game's `PolygonalMap2.resolveCollision` query, loaded-floor and adjacent-tile checks. It no longer asks the walking animation to catch up through a fabricated movement vector.

For recentered physical head displacement D and consumed physical body displacement C, the offset relative to the actor is D minus C. A physical move updates the actor and C by the accepted collision displacement. Native stick displacement is preserved and never added to C. The native previous-position anchor and pending next-position delta are translated intact, so both inputs can coexist. No whole simulation method is replayed. The compensating offset is recorded with each local character snapshot; an older render frame does not borrow a newer simulation tick's compensation.

The lean allowance is 4 cm, the maximum additional movement per update is 0.12 scene units, and offsets over 1.5 m are rejected as discontinuities. Game pause, blocked native movement, sitting/lying, timed actions, unsupported ownership or lost/stale XR tracking inhibit physical movement. Unloaded, nonsolid-floor or blocked tiles reject it. Frame generation must remain Off, as in prior versions. Physical steps do not require mouse capture or assignment of the virtual pad. Releasing the UI cursor blocks stick/attack input, while the physical body may continue following the head during unpaused gameplay.

This is horizontal collision movement, not full body IK or a complete physical locomotion system. Physical stepping does not yet drive the native walking gait, footsteps or endurance accounting. Physical crouching changes the camera/arm height but does not toggle Zomboid crouch state. Head collision correction, stairs/vertical movement and comfort still need further work and live tests.

## Input ownership

XR focus is separate from the Windows desktop window's focus. In gameplay mode, a fresh focused OpenXR head pose can own input even when the desktop is unfocused. SteamVR's dashboard or another VR application taking focus stops gameplay; a visible but unfocused session can continue rendering. The Body status now names stale head data, missing tracked world, missing XR focus, pause or invalid native ownership. Body logs include desktop focus, capture state, native pad binding, requested physical displacement and accepted displacement.

VR gameplay starts with Viewpoint's cursor intent captured. Right-stick click and native middle mouse toggle the UI cursor; a desktop capture failure does not itself free the VR cursor. The original diagnostic controller modes retain desktop-focus behavior. Existing physical gamepads remain intact. If a different native pad is assigned, its native turning remains authoritative instead of also adding VR stick turns. For the owned snap/smooth turn and motion-melee mapping, select Viewpoint VR Gamepad (ID 15).

## Tracked arms and held items

The authored ArmIk/ArmRig solver is reused independently of PZ3D. Head and controller poses are sampled together. Grip poses in VIEW/head space are transformed through the calibrated head pose, native camera anchor and actor model transform. The adapter estimates palm placement, solves each available arm and carries finger/prop descendants. Missing tracking retains the native animation on that side. Physical height changes shift the tracked shoulder roots. Arm extension is bounded to 150 percent; full torso/leg IK and two-handed weapon constraints are not provided.

`Characters.snapshot` wraps its existing local `ModelCapture.capture` call. After placement, native leg relief and slot setup, the scope precomputes copied 16-float palettes and held-attachment deltas. Only a complete successful preflight publishes overrides. The existing capture then packs them into its 12-float visible/shadow palettes. Original animation players, palette bytes/references and attachment matrices remain unchanged. Static held items and nested attachments receive a single wrist delta through their own native world transform. Head-collapse and the normal scene lifetime remain native. The scope is removed on success or failure, without replaying native capture. An unsupported rig falls back to native poses until recenter; errors remain visible in the log.

The metadata and exact-binary transforms are checked offline without initializing game/mod classes. Authored snapshot tests establish adapter math/ownership, not actual clothing fit or a validated in-game hand pose.

## Motion attacks

Motion melee starts Off. Diagnostics recognizes gestures but sends no attacks. Live mode requires the owned Viewpoint VR Gamepad assigned to the player, an equipped usable melee weapon, focused tracked gameplay, a released-then-held right trigger and a deliberate right-hand swing. Hold native ready/aim (Viewpoint's default is LT) as needed by the current bindings. One gesture generates one 80 ms native RT pulse. The game retains attack targeting, cooldown, animation, hit processing and damage rules. Native stick movement remains available during the pulse.

The detector uses physical LOCAL-space hand motion, reconstructed from the head pose and relative grip pose. Both absolute hand motion and motion relative to the head must be fast enough; translating the body or rotating the head alone does not count. Travel must exceed 12 cm with speed at least 1.2 m/s; implausible jumps reset it. Release RT before another gesture. Focus/UI/tracking changes, recentering and artificial turning disarm it. Turning has a 200 ms exclusion window.

Diagnostics and Live reserve the owned pad's RT globally; holding RT alone no longer attacks, and ranged/bare-hand equipment cannot produce live motion pulses. Off restores the ordinary trigger mapping after neutral input. Unselected physical pads are not modified, so Live refuses to dispatch gestures through pad 0 or another unowned device. This avoids competing with that device's raw trigger.

This is swing-triggered native combat, not contact damage. A rendered weapon touching a zombie does not independently determine a hit. Attack direction/target selection still use native rules and head-aligned body aim, not a tracked weapon collision ray. Left-hand/unarmed swings, physical shoves, guns aimed by the controller and object grabbing are not implemented.

## First live checks

Use [TESTING.md](TESTING.md). Confirm physical movement first with sticks centered, then combined stick/physical movement and a wall. Next inspect the hands and held item, including recenter/tracking recovery. Finally use motion Diagnostics before Live. Console prefixes are `[Project Viewpoint VR Body]`, `[Project Viewpoint VR Rig]`, `[Project Viewpoint VR Melee]`, and the existing OpenXR/Gamepad prefixes. Requests, input delivery and native damage success are different observations; logs alone cannot prove comfort or correct hits.
