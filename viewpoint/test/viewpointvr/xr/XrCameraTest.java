package viewpointvr.xr;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
public final class XrCameraTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void near(float a,float b){check(Math.abs(a-b)<.0001f,a+" != "+b);}
    static XrCamera.Pose pose(float x,float y,float z,Quaternionf q){return new XrCamera.Pose(x,y,z,q.x,q.y,q.z,q.w);}
    static List<XrCamera.View> views(XrCamera.Pose head){
        var mat=head.matrix();var q=mat.getNormalizedRotation(new Quaternionf());var list=new ArrayList<XrCamera.View>();
        for(float x:new float[]{-.032f,.032f}){var p=mat.transformPosition(new Vector3f(x,0,0));list.add(new XrCamera.View(pose(p.x,p.y,p.z,q),-.9f,.7f,.8f,-.75f));}return list;
    }
    public static void main(String[] args){
        var head=pose(2,1.7f,3,new Quaternionf());var center=new Vector3f(5,2,7);var camera=new XrCamera(1);
        var pair=camera.eyes(head,views(head),center,0,0,true);
        near(pair.left().position().distance(pair.right().position()),.064f);near(pair.left().position().y,2);
        near(new Vector3f(pair.left().position()).add(pair.right().position()).mul(.5f).distance(center),0);
        var moved=pose(2,1.9f,3,new Quaternionf().rotationY(.4f));
        var tracked=camera.eyes(moved,views(moved),center,0,0,true);near(tracked.left().position().y,2.2f);
        check(!tracked.left().view().equals(pair.left().view()),"head rotation changes view");
        var fixed=camera.eyes(moved,views(moved),center,0,0,false);near(fixed.left().position().y,2);near(fixed.left().position().distance(pair.left().position()),0);
        camera.recenter();var recentered=camera.eyes(moved,views(moved),center,0,0,true);near(recentered.left().position().distance(pair.left().position()),0);
        var scaled=new XrCamera(2).eyes(head,views(head),center,0,0,true);near(scaled.left().position().distance(scaled.right().position()),.128f);
        var fov=views(head).get(0);var crop=fov.crop(2048,2048);check(crop[0]==0&&crop[2]<2048&&crop[1]>0&&crop[3]==2048,"asymmetric crop bounds");
        near(pair.left().projection().m20(),0);near(pair.left().projection().m21(),0);
        for(int i=0;i<4;i++)check(crop[i]>=0&&crop[i]<=2048,"crop in target");
        var pitched=pose(0,1.7f,0,new Quaternionf().rotationX(.3f));var pitchPair=new XrCamera(1).eyes(pitched,views(pitched),new Vector3f(),0,0,true);
        Vector3f forward= pitchPair.left().view().invert(new Matrix4f()).transformDirection(new Vector3f(0,0,-1));check(forward.y>.2f,"recenter preserves horizon; does not erase pitch");
        // Render/crop must map a ray to the same pixel fraction as an asymmetric projection.
        var tan=fov.tangents();var symmetric=fov.symmetric();
        for(float ray:new float[]{tan.left(),-.1f,0,tan.right()}) {
            float source=(ray-symmetric.left())/(symmetric.right()-symmetric.left())*2048;
            float cropped=(source-crop[0])/(crop[2]-crop[0]);
            float expected=(ray-tan.left())/(tan.right()-tan.left());
            check(Math.abs(cropped-expected)<.001f,"asymmetric crop preserves ray mapping");
        }
        System.out.println("XR camera/crop: "+checks+" checks passed");
    }
}
