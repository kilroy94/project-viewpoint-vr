package viewpointvr.input;
import org.joml.Vector3f;
/** Render/game-thread mailbox. Compensation uses only collision-resolved actor movement. */
public final class BodyTracking {
 public record Head(long time,float x,float z,float yaw,float pitch,float scale) {}
 public record Offset(float x,float z) {}
 private static volatile Head head;
 private static Offset offset=new Offset(0,0);
 public static volatile boolean enabled;
 private static long revision;
 public static synchronized void clear(){head=null;offset=new Offset(0,0);enabled=false;following=false;revision++;}
 public static synchronized void suspend(){head=null;enabled=false;following=false;revision++;}
 public static volatile boolean following;
 public static boolean ownsFollow(){return enabled&&following;}
 public static synchronized long revision(){return revision;}
 public static synchronized Offset offset(){return offset;}
 public static void head(float x,float z,float yaw,float pitch,float scale){head=new Head(System.nanoTime(),x,z,yaw,pitch,scale);}
 public static Head current(){var h=head;return enabled&&h!=null&&System.nanoTime()-h.time()>=0&&System.nanoTime()-h.time()<250_000_000L?h:null;}
 public static float yaw(float nativeYaw){var h=current();return h==null?nativeYaw:(float)Math.IEEEremainder(nativeYaw-h.yaw(),Math.PI*2);}
 public static float pitch(float nativePitch){var h=current();return h==null?nativePitch:h.pitch();}
 /** Game world XY = negative scene XZ. Return recentered XR metres consumed. */
 public static synchronized void consume(float gameX,float gameY,float baseYaw,float scale){
  if(!Float.isFinite(gameX+gameY+baseYaw+scale)||scale<=0||Math.hypot(gameX,gameY)>.35)return;
  float a=(float)Math.PI/2-baseYaw,c=(float)Math.cos(a),s=(float)Math.sin(a);
  offset=new Offset(offset.x()+(-c*gameX+s*gameY)/scale,offset.z()+(-s*gameX-c*gameY)/scale);
 }
 /** View-relative left-stick coordinates for a horizontal physical head offset. */
 public static Vector3f follow(Head h,float heading){
  float distance=(float)Math.hypot(h.x(),h.z());if(distance<=.12f||distance>1.5f)return new Vector3f();
  float strength=Math.min(.65f,(distance-.12f)*2f),c=(float)Math.cos(heading),s=(float)Math.sin(heading);
  return new Vector3f((c*h.x()-s*h.z())/distance*strength,(-s*h.x()-c*h.z())/distance*strength,0);
 }
}
