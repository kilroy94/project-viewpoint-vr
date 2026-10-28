package viewpoint.render;
import org.joml.Matrix4f;
/** Authored 0.1.6.1 call-shape fixture, no OpenGL or game code. */
public final class WorldRenderer {
 private static final FrameContext frame=new FrameContext();private static final TemporalPass temporal=new TemporalPass();
 private static final ModelPass models=new ModelPass();private static final Targets targets=new Targets();
 private static final FloorBakes floors=new FloorBakes();private static final ShellGround ground=new ShellGround();
 private static final FarPass far=new FarPass();private static final ShadowPass shadows=new ShadowPass();private static final WeatherMap weatherMap=new WeatherMap();
 private static final SkyPass sky=new SkyPass();
 private static final FrameEnd end=new FrameEnd(temporal);
 public static final Matrix4f handView=new Matrix4f(),cullView=new Matrix4f();public static boolean handFromPlayer=true,cullFromPlayer=true,freeCamera=true;
 static java.util.function.LongSupplier clock=()->{Probe.hit("clock");return 1000000000L;};
 public static boolean begin(SceneData scene,Matrix4f view,float x,float y,float z){
  Probe.hit("begin");if(Probe.skip)return false;DevReload.frame();PackLinks.follow();
  frame.scene=scene;frame.view=view;frame.outputDrawFbo=Probe.bound;frame.outputReadFbo=Probe.bound;
  FrameStream.frameStarted();frame.index++;FarRelief.frame(scene);
  temporal.begin(frame);FrameUniforms.set(frame,targets);floors.update(scene);ground.update(scene);
  PackModels.upload(System.nanoTime());Meshes.prepare(scene);models.prepare(scene);
  if(!IrisMode.draw(frame,shadows,models,far))drawWorld();return true;
 }
 private static void drawWorld(){far.prepare(frame,true);shadows.draw(frame);MousePick.read(frame,0,new Matrix4f());sky.prepare(frame);Probe.hit("world");}
 public static void drawOutlines(){Probe.hit("outlines");}
 public static void drawTranslucent(SceneData scene){Probe.hit("translucent");}
 public static void drawWeather(SceneData scene,float x,float y,float z){Probe.hit("weather");}
 public static void finish(SceneData scene){FrameGenPass.midframe();end.draw(frame);models.endFrame();Probe.hit("finish");}
 static boolean off(int feature){return false;}
 public static void restoreOutput(){Probe.bound=frame.outputDrawFbo;Probe.hit("outerRestore");}
 public static FrameContext context(){return frame;}
}
