package viewpoint.render;
import org.joml.Matrix4f;
import viewpoint.platform.PackPass;
/** Test doubles expose state transitions, never OpenGL or game code. */
public final class WorldRenderer {
 private static final FrameContext frame=new FrameContext();
 private static final TemporalPass temporal=new TemporalPass();
 private static final ModelPass models=new ModelPass();
 private static final Targets targets=new Targets();
 private static final FloorBakes floors=new FloorBakes();
 private static final ShellGround ground=new ShellGround();
 private static final FarPass far=new FarPass();
 private static final PackStages stages=new PackStages();
 private static final ShadowPass shadows=new ShadowPass();
 private static final WeatherMap weatherMap=new WeatherMap();
 public static final Matrix4f handView=new Matrix4f(),cullView=new Matrix4f();
 public static boolean handFromPlayer=true,cullFromPlayer=true;
 static java.util.function.LongSupplier clock=()-> { Probe.hit("clock"); return 1000000000L; };
 public static boolean begin(SceneData scene,Matrix4f view,float x,float y,float z) {
  Probe.hit("begin"); if(Probe.skip)return false; developer(); followPack();
  frame.scene=scene; frame.view=view; frame.outputDrawFbo=Probe.bound; frame.outputReadFbo=Probe.bound;
  FrameStream.frameStarted(); frame.index++;
  temporal.begin(frame); FrameUniforms.set(frame,targets);
  floors.update(scene); ground.update(scene); PackModels.upload(System.nanoTime()); Meshes.prepare(scene); models.prepare(scene);
  drawWorld(); return true;
 }
 private static void developer(){Probe.hit("developer");}
 private static void followPack(){Probe.hit("pack");}
 private static void drawWorld(){far.prepare(frame); shadows.draw(frame); MousePick.read(frame,targets); weatherMap.draw(frame); Probe.hit("world");}
 public static void drawOutlines(){Probe.hit("outlines");}
 public static void drawTranslucent(SceneData scene){runPasses(PackPass.Stage.TRANSLUCENT,123);}
 public static void drawIndirect(SceneData scene){if(!off(2))Probe.hit("indirect");}
 public static void drawWeather(SceneData scene,float x,float y,float z){Probe.hit("weather");}
 private static int runPasses(PackPass.Stage stage,int input){Probe.hit("passes");return input;}
 public static void finish(SceneData scene){
  runPasses(PackPass.Stage.COMPOSITE,123);
  boolean hasFinal=stages.any(PackPass.Stage.FINAL);
  if(!off(1))temporal.resolve(frame);else temporal.remember(frame);
  if(hasFinal)runPasses(PackPass.Stage.FINAL,123);
  models.endFrame(); Probe.hit("finish");
 }
 static boolean off(int feature){return false;}
 public static void restoreOutput(){Probe.bound=frame.outputDrawFbo;Probe.hit("outerRestore");}
 public static FrameContext context(){return frame;}
}
