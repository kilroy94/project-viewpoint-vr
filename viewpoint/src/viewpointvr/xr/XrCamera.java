package viewpointvr.xr;

import java.util.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import viewpointvr.*;

/** OpenXR local Y-up space to Viewpoint Y-up scene, with a yaw-only recenter. */
public final class XrCamera {
    public record Pose(float x,float y,float z,float qx,float qy,float qz,float qw) {
        public Matrix4f matrix() {
            if(!Float.isFinite(x+y+z+qx+qy+qz+qw) || qx*qx+qy*qy+qz*qz+qw*qw<.0001f) throw new IllegalArgumentException("Invalid XR pose");
            return new Matrix4f().translationRotate(x,y,z,new Quaternionf(qx,qy,qz,qw).normalize());
        }
    }
    public record View(Pose pose,float left,float right,float up,float down) {
        public StereoCamera.Fov tangents() {
            if(left<=-1.56f || right>=1.56f || down<=-1.56f || up>=1.56f || !(left<0 && right>0 && down<0 && up>0)) throw new IllegalArgumentException("Unsupported XR FOV");
            return new StereoCamera.Fov((float)Math.tan(left),(float)Math.tan(right),(float)Math.tan(down),(float)Math.tan(up));
        }
        public StereoCamera.Fov symmetric() {
            var f=tangents();float x=Math.max(-f.left(),f.right()),y=Math.max(-f.down(),f.up());
            return new StereoCamera.Fov(-x,x,-y,y);
        }
        public int[] crop(int width,int height) {
            var f=tangents();var s=symmetric();
            return new int[]{Math.round((f.left()-s.left())/(s.right()-s.left())*width),Math.round((f.down()-s.down())/(s.up()-s.down())*height),
                Math.round((f.right()-s.left())/(s.right()-s.left())*width),Math.round((f.up()-s.down())/(s.up()-s.down())*height)};
        }
    }
    private Vector3f origin;
    private Quaternionf inverseYaw;
    private final float scale;
    public XrCamera(float sceneUnitsPerMeter) {
        if(!Float.isFinite(sceneUnitsPerMeter)||sceneUnitsPerMeter<.25f||sceneUnitsPerMeter>4) throw new IllegalArgumentException("Scale must be .25..4 scene units per meter");
        scale=sceneUnitsPerMeter;
    }
    public void recenter() {origin=null;inverseYaw=null;}
    public StereoCamera.Pair eyes(Pose head,List<View> views,Vector3f center,float yaw,float pitch,boolean tracked) {
        if(views.size()!=2) throw new IllegalArgumentException("Two XR eyes required");
        Matrix4f headMatrix=head.matrix();
        if(origin==null) {
            origin=new Vector3f(head.x(),head.y(),head.z());
            Vector3f forward=headMatrix.transformDirection(new Vector3f(0,0,-1));
            float heading=(float)Math.atan2(-forward.x,-forward.z);
            inverseYaw=new Quaternionf().rotationY(-heading);
        }
        Matrix4f base=new Matrix4f().translationRotate(center.x,center.y,center.z,StereoCamera.viewpointLook(yaw,tracked?0:pitch));
        Matrix4f mapping=new Matrix4f(base).scale(scale).rotate(inverseYaw).translate(new Vector3f(origin).negate());
        Matrix4f fixed=headMatrix.invert(new Matrix4f());
        StereoCamera.Eye[] eyes=new StereoCamera.Eye[2];
        for(int i=0;i<2;i++) {
            View view=views.get(i);
            Matrix4f camera=tracked?new Matrix4f(mapping).mul(view.pose().matrix()):new Matrix4f(base).scale(scale).mul(fixed).mul(view.pose().matrix());
            // Extract rotation before inverting: uniform physical scale must not scale scene geometry.
            Vector3f position=camera.getTranslation(new Vector3f());
            Quaternionf orientation=camera.getUnnormalizedRotation(new Quaternionf()).normalize();
            eyes[i]=StereoCamera.eye(position,orientation,new Vector3f(),view.symmetric());
        }
        return new StereoCamera.Pair(eyes[0],eyes[1]);
    }
}
