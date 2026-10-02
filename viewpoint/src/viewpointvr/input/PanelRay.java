package viewpointvr.input;
import org.joml.Vector3f;
import viewpointvr.xr.XrCamera.Pose;
/** Same head-relative plane and size used by the XR UI quad. */
public final class PanelRay {
 public record Hit(float x,float y,Vector3f start,Vector3f end) {}
 public static Hit hit(Pose aim,float aspect){
  if(aim==null||!Float.isFinite(aspect)||aspect<=0)return null;
  var matrix=aim.matrix();var start=new Vector3f(aim.x(),aim.y(),aim.z());
  var direction=matrix.transformDirection(new Vector3f(0,0,-1));
  if(direction.z>=-.0001f)return null;
  float distance=(-1.5f-start.z)/direction.z;if(distance<=0||distance>6)return null;
  var end=new Vector3f(direction).mul(distance).add(start);
  float width=Math.min(2,1.3f*aspect),height=width/aspect;
  float x=end.x/width+.5f,y=.5f-end.y/height;
  return x>=0&&x<=1&&y>=0&&y<=1?new Hit(x,y,start,end):null;
 }
}
