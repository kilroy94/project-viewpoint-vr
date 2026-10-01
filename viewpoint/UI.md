# Headset UI implementation (0.6.0)

Target: exact-pinned Viewpoint 0.1.3 and Zomboid 42.21.0. Decompiled classes informed call-site selection only; no dependency source or binaries are distributed. The 0.1.5a-hotfix Workshop update was discovered during this work. It changes 326 archive entries including SceneDrawer, WorldRenderer, FarPass, Hooks and ImGuiFrame. At the user's explicit request its compatibility work is deferred; binary pins are unchanged.

## Capture boundaries

- Lua `OnPreUIDraw` and `OnPostUIDraw` queue render-thread markers around ordinary Zomboid UI and this mod's controls/status. This avoids assuming the completed vanilla UI FBO includes all later overlays.
- `UiTransform` wraps the pinned `Hooks.uiFrameEnding` calls to `LootPanel.draw` and `PerformanceOverlay.draw`. Markers surround each producer's queued draw commands, including frames where vanilla UI is throttled.
- `ImGuiFrame.draw` explicitly binds framebuffer zero. The transform therefore wraps `ImGuiImplGl3.renderDrawData` after that binding, rather than redirecting the outer settings method.
- `Hooks.frameSwapping` invokes settings once and then assembles completed layers in vanilla/loot/performance/settings order. Closing a late overlay clears it on the next assembly; vanilla contents survive throttling for up to two seconds.

Each native producer/input/event path executes once. `UiCapture` redirects draw output into transparent RGBA8 targets with a shared, cleared depth/stencil attachment for native clipping, then composites the premultiplied result back into the caller framebuffer. The composition shader preserves program, VAO, sampler, framebuffer and compatibility GL state. Allocation, resize and destruction stay on the render thread. Queued markers retain their capture owner so Off/restart ignores obsolete callbacks. Capture failure restores an open caller target, disables the UI sink, and leaves native producers running; Off/re-enter recreates resources.

## XR submission and interaction

The next XR frame copies the previous completed UI FBO into a separately owned swapchain. A VIEW-space quad at z=-1.5 uses source-alpha blending with premultiplied color. Its size preserves desktop aspect ratio, capped at 2 meters wide and 1.3 meters high. The runtime must support two composition layers. Swapchain dimensions respect runtime limits; acquire/wait/copy/release uses the same protected GL copy path as world eyes. UI can be submitted without a world layer during idle frames, but returning to the main menu requests Off.

Desktop keyboard/mouse interaction remains unchanged. A small high-contrast marker follows the normalized desktop cursor while it is released and focused; the operating-system cursor texture is not copied. No virtual clicks, ImGui input replay, controller ray, hand input or menu redesign is added. Third-party draw handlers outside the marked interval and developer ImGui are outside this milestone. UI lag is one completed desktop frame; headset comfort, gamma and readability are unverified.

## Verification boundary

The hidden OpenGL test verifies transparent compositing, alpha, stencil clipping/reset, overlay order, desktop restoration, retained/expired vanilla contents, disappearing overlays, resize, cursor visibility and failure restoration. Bridge tests check queued ordering, native exceptions, once-only invocation, obsolete callbacks and capture failure. Both additional pinned classes are verified/retransformed without initialization alongside the eight existing targets; altered/already-patched inputs are rejected. No game/mod entry point or SteamVR is launched. See VALIDATION.md for the final build record and TESTING.md for the user's eventual null-headset test.
