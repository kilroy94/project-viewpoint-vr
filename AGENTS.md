# Research workspace boundary

Read README.md and research/final-report.md before continuing. The user's request is preserved in research/original-request.md.

For the current preserved 0.11.0 implementation, also read docs/PZ3D-BASELINE.md and experiments/zombiebuddy-harness/VALIDATION.md. The original request/report are historical research; they do not describe the current feature set. The baseline includes hand-interaction source without claiming new in-game validation. Do not infer authorization for another renderer or architecture refactoring from that documentation.

The user authorized the standalone OpenXR diagnostic, 3D scene, renderer adapter, and explicitly requested the ZombieBuddy test harness and its OpenXR integration. The user confirmed live desktop stereo works in version 0.2.0. Build and test the harness in the workspace and package it for the user's in-game capture test. Do not launch Project Zomboid, run either existing mod's entry point or installer, or write to the working game installation, Workshop folders, or Zomboid user data. Tests may define/retransform copied classes without initialization or executing game/mod entry points. The user will install and perform the first in-game test. Prefer workspace-local SteamVR configuration/log overrides and process-local runtime selection over global changes.

Installed copied binaries are authoritative. PZ3D names such as Renderer.Frame.ad and Renderer.nw are actual installed names, not stable APIs. For the pinned 0.3.0 implementation prefer reference/pz3d/0.3.0/vineflower; reference/pz3d/vineflower and CFR output are historical comparisons. Do not treat decompiler output as buildable source. Record uncertainty rather than inferring runtime success from static code.
