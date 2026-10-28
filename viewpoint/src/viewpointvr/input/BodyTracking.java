package viewpointvr.input;
import org.joml.Vector3f;
/** Render/game-thread mailbox. Compensation uses only collision-resolved actor movement. */
public final class BodyTracking {
 public record Head(long time,float x,float z,float yaw,float pitch,float scale,float y,viewpointvr.xr.XrCamera.Pose pose,ControllerState controllers) {
  public Head(long time,float x,float z,float yaw,float pitch,float scale){this(time,x,z,yaw,pitch,scale,0,null,ControllerState.EMPTY);}
 }
 public record Offset(float x,float z) {}
 private static volatile Head head;
 private static Offset offset=new Offset(0,0);
 private static final java.util.IdentityHashMap<Object,Offset> frames=new java.util.IdentityHashMap<>();
 public static synchronized void snapshot(Object frame){if(frames.size()>64)frames.clear();frames.put(frame,offset);}
 public static synchronized Offset offsetFor(Object frame){return frames.getOrDefault(frame,offset);}
 public static volatile boolean enabled,xrFocused;
 public static volatile String status="not started";
 private static long revision,calibration;
 public static synchronized long calibration(){return calibration;}
 public static synchronized void clear(){head=null;offset=new Offset(0,0);frames.clear();enabled=false;revision++;calibration++;}
 public static synchronized void suspend(){head=null;enabled=false;revision++;}
 public static synchronized long revision(){return revision;}
 public static synchronized Offset offset(){return offset;}
 public static void head(float x,float z,float yaw,float pitch,float scale){head(x,0,z,yaw,pitch,scale);}
 public static void head(float x,float y,float z,float yaw,float pitch,float scale){var q=new org.joml.Quaternionf().rotationY(yaw).rotateX(pitch);head=new Head(System.nanoTime(),x,z,yaw,pitch,scale,y,new viewpointvr.xr.XrCamera.Pose(x,y,z,q.x,q.y,q.z,q.w),InputBridge.state);}
 public static synchronized void headRaw(viewpointvr.xr.XrCamera.Pose pose,float scale){head(new viewpointvr.xr.XrCamera.Pose(pose.x()-offset.x(),pose.y(),pose.z()-offset.z(),pose.qx(),pose.qy(),pose.qz(),pose.qw()),scale);}
 public static void head(viewpointvr.xr.XrCamera.Pose pose,float scale){
  var f=pose.matrix().transformDirection(new Vector3f(0,0,-1));head=new Head(System.nanoTime(),pose.x(),pose.z(),(float)Math.atan2(-f.x,-f.z),(float)Math.atan2(f.y,Math.hypot(f.x,f.z)),scale,pose.y(),pose,InputBridge.state);
 }
 public static Head latest(){var h=head;long age=h==null?Long.MAX_VALUE:System.nanoTime()-h.time();return age>=0&&age<250_000_000L?h:null;}
 public static Head current(){return enabled?latest():null;}
 public static float yaw(float nativeYaw){var h=current();return h==null?nativeYaw:(float)Math.IEEEremainder(nativeYaw-h.yaw(),Math.PI*2);}
 public static float pitch(float nativePitch){var h=current();return h==null?nativePitch:h.pitch();}
 /** Game world XY = negative scene XZ. Return recentered XR metres consumed. */
 public static synchronized void consume(float gameX,float gameY,float baseYaw,float scale){
  if(!Float.isFinite(gameX+gameY+baseYaw+scale)||scale<=0||Math.hypot(gameX,gameY)>.35)return;
  float a=(float)Math.PI/2-baseYaw,c=(float)Math.cos(a),s=(float)Math.sin(a);
  float dx=(-c*gameX+s*gameY)/scale,dz=(-s*gameX-c*gameY)/scale;
  offset=new Offset(offset.x()+dx,offset.z()+dz);
  var h=head;if(h!=null)head=new Head(h.time(),h.x()-dx,h.z()-dz,h.yaw(),h.pitch(),h.scale(),h.y(),h.pose()==null?null:new viewpointvr.xr.XrCamera.Pose(h.x()-dx,h.y(),h.z()-dz,h.pose().qx(),h.pose().qy(),h.pose().qz(),h.pose().qw()),h.controllers());
 }
}
