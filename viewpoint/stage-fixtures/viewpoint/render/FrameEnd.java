package viewpoint.render;
import viewpoint.platform.PackPass;
final class FrameEnd {
 private final TemporalPass temporal;private final PackStages stages=new PackStages();private final FrameGenPass gen=new FrameGenPass();
 FrameEnd(TemporalPass t){temporal=t;}
 void draw(FrameContext f){
  stages.run(PackPass.Stage.COMPOSITE,f,123);boolean has=stages.any(PackPass.Stage.FINAL);
  if(Probe.upscaled)temporal.remember(f);else temporal.run(f,123,!WorldRenderer.off(1));
  if(has)stages.run(PackPass.Stage.FINAL,f,123);gen.capture(f);
 }
}
