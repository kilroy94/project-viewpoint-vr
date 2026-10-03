# In-game controller diagnostic (0.8.0)

This opt-in simulator runs a roughly 25-second UI-only script using synthetic head-relative controller poses. It needs an active XR UI panel but no physical controllers, so the existing null-headset setup is suitable. No real controller runtime bindings or tracking are verified. Normal installation does not arm it.

## Run two reports

1. Install the 0.8.0 package normally with the game closed. Launch the game and your existing null-headset environment yourself. Use a disposable single-player save, Viewpoint first person on foot, with its native shaders. Complete Viewpoint onboarding before testing. Choose XR fixed or XR tracked and wait for the UI to appear. Keep **Controllers Off** and release Viewpoint's mouse capture with middle mouse. Pausing the world is fine; close any blocking modal/pause menu before starting.
2. Click **Open controller diagnostic** in the Project Viewpoint VR panel. The test window contains a real Zomboid ISButton, a Lua UI drag target, and an ISScrollingListBox. Click **Start scripted UI test**, release the mouse, and leave the panel still until the status says COMPLETE. A three-second countdown precedes input. The script clicks once, drags, scrolls, then repeats dragging with simulated tracking/focus interruptions and held-trigger recovery.
3. Close the Zomboid diagnostic panel. Open Viewpoint settings using your configured settings key, select its new **VR** tab and expand **Controller diagnostic**. Keep that entire section visible. Click its **Start scripted UI test** button. This runs the same script through Viewpoint's actual ImGui input/frame path against an ImGui button, slider and scrollable child. These diagnostic widgets change only their own temporary state, not saved renderer settings or inventory.
4. Return the two `controller-vanilla-*.txt` and `controller-viewpoint-*.txt` reports from **`%USERPROFILE%\Zomboid\Project-Viewpoint-VR\controller-diagnostics`** (or the corresponding folder under your custom Zomboid cache directory). Status displays the report filename. Both are plain UTF-8 text; no screenshots are required to assess logged results.

During the script, do not click, scroll, move/resize the test window, change tabs or enable real controllers. Those actions can abort it. **Stop diagnostic**, either test's Stop button, **Controllers Off**, renderer Off and **Pause/Break** cancel. Losing actual window focus, changing the UI route, showing a blocking modal/onboarding screen, losing the XR panel, closing/moving the target, or stopping input ticks also cancels. Preferences are unchanged and controller activation remains Off afterward. No movement, turning, attacks, item transfers or OS input are scripted.

## Read the report

- `INFO INPUT`: a requested script step, not proof of an action succeeding.
- `INFO OBSERVED` / `PASS adapter`: the native input adapter observed the expected button state and pointer location for that phase. Interrupted/held-recovery phases verify release rather than claiming a coordinate check.
- `INFO WIDGET` / `PASS widget`: actual UI callbacks or state changes were observed: exactly one click, a changed drag value, changed scroll offset, and drag deactivation on ordinary release and simulated tracking/focus loss. The script never calls those callbacks directly.
- `FAIL`: a required observation did not arrive, or the run aborted. A failed action is not silently upgraded to a pass because the input was sent.
- `NEEDS VISUAL CHECK`: pointer/beam alignment, text readability, flicker and panel appearance. Real inventory operations and physical-controller bindings/comfort are explicitly outside this automated script.

A complete report ends with `SUMMARY` and `END OF REPORT`. A file lacking that ending is incomplete (for example, the game was closed or crashed). The script is not paused/resumed after an interruption; resolve the cause and start a fresh report. A game/input stall over one second aborts rather than skipping held-input phases.

The standalone test suite validates phase timing, neutral rearming through ControllerLogic, positive versus missing widget observations, report classification, cancellation and file flushing using workspace-local fixtures. It does not execute the live Lua/ImGui widgets or claim an in-game pass. The first real UI diagnostic run remains for the user.
