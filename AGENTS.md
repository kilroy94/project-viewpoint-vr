# Project Viewpoint VR workspace

Read README.md, docs/VIEWPOINT-PLAN.md and viewpoint/VALIDATION.md first.

The user authorized this separate project and progression to its next implementation step. Viewpoint is the sole future renderer target. Reuse worthwhile PZ3D VR code and discard incompatible adapters; do not maintain dual renderer support by default. The inherited experiments, baseline docs and research report are historical PZ3D references, not current Viewpoint features or a request to run their installers.

Current implementation: 0.5.0 continuous desktop stereo and OpenXR fixed/tracked camera modes, recentering and adjustable scale. Read viewpoint/TESTING.md, viewpoint/RENDER-BOUNDARY.md and viewpoint/NATIVE-STAGES.md. Use viewpoint/Build.ps1 to test and package; it requires the standalone hidden OpenGL test in addition to copied-class and fixture tests. Eight copied targets are verified/retransformed without initialization. The user produced two successful 0.4.1 in-game captures; continuous desktop and physical-headset validation remain pending. Do not infer runtime success from packaged output or standalone GPU results.

Do not launch Project Zomboid or SteamVR, execute mod entry points/installers, or write to game installations, Workshop folders or Zomboid user data. The user performs installation and in-game tests. Tests may inspect or define/retransform copied classes without initialization, and run the project's standalone hidden GL capture test without loading game/mod entry points. Runtime capture code writes user output only when the user installs/runs it in-game. Prefer local SteamVR configuration/log overrides and process-local runtime selection for future authorized XR work.

Installed copied binaries are authoritative; viewpoint/pins.json records exact hashes. Internals are not stable APIs. Decompiled output is evidence, not buildable source to republish. Never infer runtime success from static checks. Keep proprietary JARs, decompilation, generated builds, personal configuration and logs ignored. Retain clear automated versus live validation records.
