package viewpointvr.rig;
import java.lang.invoke.MethodHandle;
import java.nio.FloatBuffer;
import org.joml.Matrix4f;
/** Scoped pose substitutions before native packing, not writes to animation players or borrowed buffers. */
public final class RigBridge {
 public interface Scope extends AutoCloseable {default void prepare()throws Throwable{};FloatBuffer palette(Object data,FloatBuffer original)throws Throwable;Matrix4f attachment(Object data,Matrix4f original)throws Throwable;void close();}
 public interface Native {Scope begin(Object frame,Object slot,Object player,boolean local,boolean own,Object kind,float x,float y,float z,float angle)throws Throwable;}
 private static volatile Native adapter;private static final ThreadLocal<Scope> active=new ThreadLocal<>();
 public static volatile String status="native arms";
 public static void install(Native value){adapter=value;}
 public static void prepared()throws Throwable{var scope=active.get();if(scope!=null)scope.prepare();}
 public static FloatBuffer palette(Object data,FloatBuffer original)throws Throwable{var scope=active.get();return scope==null?original:scope.palette(data,original);}
 public static Matrix4f attachment(Object data,Matrix4f original)throws Throwable{var scope=active.get();return scope==null?original:scope.attachment(data,original);}
 public static boolean capture(Object frame,Object slot,Object player,boolean local,boolean own,Object kind,float x,float y,float z,float angle,MethodHandle original)throws Throwable{
  if(local)viewpointvr.input.BodyTracking.snapshot(frame);
  var before=active.get();Scope scope=null;var n=adapter;
  if(n!=null)try{scope=n.begin(frame,slot,player,local,own,kind,x,y,z,angle);}catch(Exception error){adapter=null;status="arms disabled: "+error;System.err.println("[Project Viewpoint VR Rig] "+status);}
  active.set(scope);
  Throwable failure=null;
  try{return (boolean)original.invoke(frame,slot,player,local,own,kind,x,y,z,angle);}catch(Throwable error){failure=error;throw error;}
  finally{active.set(before);if(scope!=null)try{scope.close();}catch(Throwable error){if(failure!=null)failure.addSuppressed(error);else throw error;}}
 }
}
