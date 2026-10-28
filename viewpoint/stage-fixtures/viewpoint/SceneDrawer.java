package viewpoint;
/** Synthetic replacement for control-flow testing; contains no native implementation. */
public final class SceneDrawer {
 private final viewpoint.core.Frame frame=new viewpoint.core.Frame();
 public Throwable caught; public int nativeCalls;
 public void render() { try { guarded(this::drawFrame); } catch(Throwable t) { caught=t; viewpoint.render.WorldRenderer.restoreOutput(); } }
 private static void guarded(Runnable body){body.run();}
 private void drawFrame() { nativeCalls++; }
 public viewpoint.core.Frame snapshot() { return frame; }
}