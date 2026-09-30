# Project Viewpoint VR workspace

Read README.md, docs/VIEWPOINT-PLAN.md and viewpoint/VALIDATION.md first.

The user authorized this separate project and its first research/implementation milestone. Viewpoint is the sole future renderer target. Reuse worthwhile PZ3D VR code and discard incompatible adapters; do not maintain dual renderer support by default. The inherited experiments, baseline docs and research report are historical PZ3D references, not current Viewpoint features or a request to run their installers.

Current implementation: offline pinned binary inspection and camera math. Use viewpoint/Test.ps1. No Viewpoint runtime interception or installable VR mod exists yet. Keep documentation honest about that boundary.

Do not launch Project Zomboid, execute existing mod entry points/installers, or write to game installations, Workshop folders or Zomboid user data. The user performs installation and in-game tests. Tests may inspect or define/retransform copied classes without initialization. Prefer local SteamVR configuration/log overrides and process-local runtime selection.

Installed copied binaries are authoritative; viewpoint/pins.json records exact hashes. Internals are not stable APIs. Decompiled output is evidence, not buildable source to republish. Never infer runtime success from static checks. Keep proprietary JARs, decompilation, generated builds, personal configuration and logs ignored. Retain clear automated versus live validation records.
