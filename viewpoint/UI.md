# Headset UI implementation (0.9.0)

Target: exact-pinned Viewpoint 0.1.6.1 and Zomboid 42.21.0. Decompiled classes informed call-site selection only; no dependency source or binaries are distributed. The current update preserves the audited UI capture call sites and adapts mouse pinning to MouseButtons.pin. ImGui draw data is consumed synchronously before native freeDrawData, retaining the upstream memory-leak fix. Crash-report windows use the same captured ImGui path and block locomotion while visible. The new setup wizard uses the same ImGuiFrame draw-data path as settings and is captured by that hook. Older Viewpoint binaries are no longer accepted.

## Single-eye desktop spectator mirror (0.11.2)

XR fixed/tracked modes now present the already-rendered left-eye world image across the desktop viewport, preserving its aspect ratio with black bars as needed. Both world eyes are still rendered/copied exactly once into their existing XR targets. There is no third camera, world redraw, eye blending, independent spectator control or head-motion smoothing. The separate Desktop stereo diagnostic mode keeps its two-eye layout.

Vanilla UI, inventory/loot panels, supported Viewpoint overlays/settings and this mod's controls still composite once over the desktop at normal desktop coordinates and resolution. They are not scaled into the eye image, doubled, cropped with the world or pasted into either XR eye. The spectator can see the menus/actions exposed on the desktop; desktop input remains available. Existing cursor capture/release rules still apply.

The headset receives the same separately captured transparent UI quad as before, using the previous completed UI frame. The single-eye world mirror does not contain that runtime-composited floating panel. The spectator sees its flat desktop counterpart, not the headset's spatial placement. The OS cursor is visible according to normal desktop capture/focus; the custom headset pointer marker remains governed by the existing UI capture rules. No new pointer-marker or third-party UI coverage is claimed.

Hidden GL integration checks cover distinct eye-copy sources, one-eye desktop presentation/letterboxing, nonzero output origin and borrowed GL-state restoration, normal desktop-size premultiplied UI blended exactly once, a separate transparent headset UI texture and return to side-by-side diagnostics. A user-run Frame/desktop check is still required.

## Capture boundaries

- Lua `OnPreUIDraw` and `OnPostUIDraw` queue render-thread markers around ordinary Zomboid UI and this mod's controls/status. This avoids assuming the completed vanilla UI FBO includes all later overlays.
- `UiTransform` wraps the pinned `Hooks.uiFrameEnding` calls to `LootPanel.draw` and `PerformanceOverlay.draw`. Markers surround each producer's queued draw commands, including frames where vanilla UI is throttled.
- `ImGuiFrame.draw` explicitly binds framebuffer zero. The transform therefore wraps `ImGuiImplGl3.renderDrawData` after that binding, rather than redirecting the outer settings method.
- `Hooks.frameSwapping` invokes settings once and then assembles completed layers in vanilla/loot/performance/settings order. Closing a late overlay clears it on the next assembly; vanilla contents survive throttling for up to two seconds.

Each native producer/input/event path executes once. `UiCapture` redirects draw output into transparent RGBA8 targets with a shared, cleared depth/stencil attachment for native clipping, then composites the premultiplied result back into the caller framebuffer. The composition shader preserves program, VAO, sampler, framebuffer and compatibility GL state. Allocation, resize and destruction stay on the render thread. Queued markers retain their capture owner so Off/restart ignores obsolete callbacks. Capture failure restores an open caller target, disables the UI sink, and leaves native producers running; Off/re-enter recreates resources.

## XR submission and interaction

The next XR frame copies the previous completed UI FBO into a separately owned swapchain. A VIEW-space quad at z=-1.5 uses source-alpha blending with premultiplied color. Its size preserves desktop aspect ratio, capped at 2 meters wide and 1.3 meters high. The runtime must support two composition layers. Swapchain dimensions respect runtime limits; acquire/wait/copy/release uses the same protected GL copy path as world eyes. UI can be submitted without a world layer during idle frames, but returning to the main menu requests Off.

Desktop keyboard/mouse interaction remains unchanged. A small high-contrast marker follows the normalized desktop cursor while it is released and focused; the operating-system cursor texture is not copied. Version 0.7.0 adds opt-in controller ray/click/scroll through the native input callbacks; see [INPUT.md](INPUT.md). No UI event replay, hand models or menu redesign is added. Third-party draw handlers outside the marked interval and developer ImGui are outside this milestone. UI lag is one completed desktop frame; headset comfort, gamma and readability are unverified.

## Verification boundary

The hidden OpenGL test verifies transparent compositing, alpha, stencil clipping/reset, overlay order, desktop restoration, retained/expired vanilla contents, disappearing overlays, resize, cursor visibility and failure restoration. Bridge tests check queued ordering, native exceptions, once-only invocation, obsolete callbacks and capture failure. Both additional pinned classes are verified/retransformed without initialization alongside eleven renderer/visibility targets; altered/already-patched inputs are rejected. No game/mod entry point or SteamVR is launched. See VALIDATION.md for the final build record and TESTING.md for the user's eventual null-headset test.
