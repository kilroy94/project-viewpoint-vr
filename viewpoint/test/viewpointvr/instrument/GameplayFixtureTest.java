package viewpointvr.instrument;
import java.nio.file.*;
import viewpointvr.input.BodyTracking;
/** Executes transformed authored input consumers. Does not load native Viewpoint classes. */
public final class GameplayFixtureTest {
 static int checks;static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static void near(float a,float b,String why){check(Math.abs(a-b)<.0001,why);}
 public static void main(String[] args)throws Exception{
  Path fixtures=Path.of(args[0]);ClassLoader loader=new ClassLoader(GameplayFixtureTest.class.getClassLoader()){
   protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
    if(!name.startsWith("viewpoint.input."))return super.loadClass(name,resolve);
    synchronized(getClassLoadingLock(name)){var type=findLoadedClass(name);if(type==null)try{String internal=name.replace('.','/');byte[] bytes=Files.readAllBytes(fixtures.resolve(internal+".class"));if(GameplayTransform.LOOK.contains(internal))bytes=GameplayTransform.patchLook(internal,bytes);type=defineClass(name,bytes,0,bytes.length);}catch(Exception e){throw new ClassNotFoundException(name,e);}if(resolve)resolveClass(type);return type;}
   }
  };
  var free=Class.forName("viewpoint.input.FreeLook",true,loader);var aim=Class.forName("viewpoint.input.CrosshairAim",true,loader);var controls=Class.forName("viewpoint.input.Controls",true,loader);var pad=Class.forName("viewpoint.input.PadLook",true,loader);var look=Class.forName("viewpoint.input.Look",true,loader);
  BodyTracking.clear();near((Float)free.getMethod("yaw").invoke(null),.7f,"Off native heading");
  BodyTracking.enabled=true;BodyTracking.head(0,0,.4f,-.3f,1);
  near((Float)free.getMethod("yaw").invoke(null),.3f,"walking follows head");near((Float)aim.getMethod("yaw").invoke(null),.3f,"body/aim follows same head");near((Float)aim.getMethod("pitch").invoke(null),-.3f,"aim pitch follows head");near((Float)controls.getMethod("handPitch").invoke(null),-.3f,"arms pitch consumer");
  near(look.getField("yaw").getFloat(null),.7f,"renderer base yaw never changed");near(look.getField("pitch").getFloat(null),.2f,"renderer base pitch never changed");
  check((Boolean)pad.getMethod("move",boolean.class).invoke(null,true),"ordinary gamepad retained");BodyTracking.following=true;check(!(Boolean)pad.getMethod("move",boolean.class).invoke(null,true),"room follow bypasses zero native pad stick");check(!(Boolean)pad.getMethod("move",boolean.class).invoke(null,false),"absent pad unchanged");BodyTracking.suspend();check((Boolean)pad.getMethod("move",boolean.class).invoke(null,true),"Off restores assigned pad");near((Float)aim.getMethod("yaw").invoke(null),.7f,"tracking loss restores native aim");
  System.out.println("Gameplay transformed fixtures: "+checks+" checks passed");
 }
}
