# Implementation and diagnostic directories

The current 0.11.0 mod combines the renderer adapter and ZombieBuddy harness. These directories contain the maintained implementation despite the historical `experiments` name. Start with the [baseline architecture map](../docs/PZ3D-BASELINE.md) and [mod guide](zombiebuddy-harness/README.md).

The first authorized milestone is the [OpenXR diagnostic](openxr-diagnostic/README.md). It uses a workspace-local SteamVR null-headset profile and independent Java/OpenGL rendering. It contains no game integration.

The next [renderer adapter](pz3d-adapter/README.md) transforms copied PZ3D classes to separate shared preparation from per-eye drawing. It is tested offline with synthetic fixtures and is not installed in the game.

The [ZombieBuddy harness](zombiebuddy-harness/README.md) packages that adapter with live OpenXR stereo, head tracking, native UI, arm IK/equipped items, controller input, turning, motion melee and hand-proximity use. It also retains stereo capture and live desktop mirror modes. Feature-specific validation limits are in its validation record.
