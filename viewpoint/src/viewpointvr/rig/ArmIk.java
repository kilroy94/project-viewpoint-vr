package viewpointvr.rig;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Two-segment positional IK, preserving bone lengths and avoiding full-extension singularities. */
public final class ArmIk {
    private ArmIk() {}
    public record Solution(Vector3f elbow,Vector3f wrist,boolean clamped) {}
    public static Solution solve(Vector3f shoulder,Vector3f elbow,Vector3f wrist,Vector3f target,Vector3f pole) {
        return solve(shoulder,elbow,wrist,target,pole,1);
    }
    public static Solution solve(Vector3f shoulder,Vector3f elbow,Vector3f wrist,Vector3f target,Vector3f pole,float maxReach) {
        if(!Float.isFinite(maxReach)||maxReach<1||maxReach>1.75f) throw new IllegalArgumentException("Invalid reach limit");
        if(!finite(shoulder)||!finite(elbow)||!finite(wrist)||!finite(target)||!finite(pole)) throw new IllegalArgumentException("Nonfinite arm");
        float a=shoulder.distance(elbow),b=elbow.distance(wrist);
        if(a<.0001f||b<.0001f) throw new IllegalArgumentException("Degenerate arm chain");
        Vector3f direction=new Vector3f(target).sub(shoulder);
        float requested=direction.length();
        // Extend only as needed, preserving the native upper/forearm length ratio.
        float extension=Math.max(1,Math.min(maxReach,requested/(a+b-Math.min(a,b)*.001f)));
        a*=extension; b*=extension;
        if(requested<.00001f) direction.set(wrist).sub(shoulder);
        if(direction.lengthSquared()<1e-10f) direction.set(0,0,-1);
        direction.normalize();
        float epsilon=Math.min(a,b)*.001f;
        float distance=Math.max(Math.abs(a-b)+epsilon,Math.min(a+b-epsilon,requested));
        Vector3f bend=new Vector3f(pole).sub(shoulder);
        bend.sub(new Vector3f(direction).mul(bend.dot(direction)));
        if(bend.lengthSquared()<1e-8f) {
            bend.set(Math.abs(direction.y)<.9f?new Vector3f(0,1,0):new Vector3f(1,0,0));
            bend.sub(new Vector3f(direction).mul(bend.dot(direction)));
        }
        bend.normalize();
        float along=(a*a-b*b+distance*distance)/(2*distance);
        float height=(float)Math.sqrt(Math.max(0,a*a-along*along));
        return new Solution(new Vector3f(shoulder).add(new Vector3f(direction).mul(along)).add(bend.mul(height)),
            new Vector3f(shoulder).add(direction.mul(distance)),Math.abs(distance-requested)>1e-5f);
    }
    public static Matrix4f moved(Matrix4f original,Vector3f oldDirection,Vector3f newDirection,Vector3f position) {
        Quaternionf delta=new Quaternionf().rotationTo(new Vector3f(oldDirection).normalize(),new Vector3f(newDirection).normalize());
        return new Matrix4f().translation(position).rotate(delta).mul(new Matrix4f(original).setTranslation(0,0,0));
    }
    /** Stretch along the segment axis, preserving thickness and separate hand/item scale. */
    public static Matrix4f extended(Matrix4f original,Vector3f oldDirection,Vector3f newDirection,Vector3f position) {
        float scale=newDirection.length()/oldDirection.length();
        Quaternionf alignment=new Quaternionf().rotationTo(new Vector3f(1,0,0),new Vector3f(oldDirection).normalize());
        Quaternionf delta=new Quaternionf().rotationTo(new Vector3f(oldDirection).normalize(),new Vector3f(newDirection).normalize());
        return new Matrix4f().translation(position).rotate(delta).rotate(alignment).scale(scale,1,1)
            .rotate(new Quaternionf(alignment).invert()).mul(new Matrix4f(original).setTranslation(0,0,0));
    }
    private static boolean finite(Vector3f v) { return Float.isFinite(v.x)&&Float.isFinite(v.y)&&Float.isFinite(v.z); }
}
