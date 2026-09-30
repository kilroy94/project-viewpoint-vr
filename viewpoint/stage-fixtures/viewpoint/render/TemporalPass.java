package viewpoint.render;
final class TemporalPass {
 private long lastFrameNanos=500000000L; boolean historyValid=true;
 void begin(FrameContext frame){
  long now=WorldRenderer.clock.getAsLong(); frame.frameSeconds=(now-lastFrameNanos)*1e-9f;lastFrameNanos=now;
  frame.previousValid=true; if(!WorldRenderer.off(1))Probe.hit("jitter"); Probe.hit("temporal");
 }
 int resolve(FrameContext frame){Probe.hit("resolve");return 123;}
 void remember(FrameContext frame){Probe.hit("remember");}
}