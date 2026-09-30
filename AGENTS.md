# Project Viewpoint VR workspace

Read README.md, docs/VIEWPOINT-PLAN.md and viewpoint/VALIDATION.md first.

The user authorized this separate project and progression to its next implementation step. Viewpoint is the sole future renderer target. Reuse worthwhile PZ3D VR code and discard incompatible adapters; do not maintain dual renderer support by default. The inherited experiments, baseline docs and research report are historical PZ3D references, not current Viewpoint features or a request to run their installers.

Current implementation: pinned binary inspection, camera math, pair lifecycle, entry transform and a concrete native-stage backend. Read viewpoint/RENDER-BOUNDARY.md, viewpoint/NATIVE-STAGES.md and use viewpoint/Test.ps1. Four copied targets are verified/retransformed without initialization; synthetic fixtures exercise the complete adapter. The backend has an Output interface but no real GL output/capture provider or live loader/activation code yet. The generated core JAR is not an installable mod. Keep documentation honest about that boundary.

Do not launch Project Zomboid, execute existing mod entry points/installers, or write to game installations, Workshop folders or Zomboid user data. The user performs installation and in-game tests. Tests may inspect or define/retransform copied classes without initialization. Prefer local SteamVR configuration/log overrides and process-local runtime selection.

Installed copied binaries are authoritative; viewpoint/pins.json records exact hashes. Internals are not stable APIs. Decompiled output is evidence, not buildable source to republish. Never infer runtime success from static checks. Keep proprietary JARs, decompilation, generated builds, personal configuration and logs ignored. Retain clear automated versus live validation records.
