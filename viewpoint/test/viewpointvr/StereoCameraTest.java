package viewpointvr;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public final class StereoCameraTest {
    private static int checks;
    private static void near(float actual, float expected) {
        checks++;
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > 0.0001f)
            throw new AssertionError(actual + " != " + expected);
    }
    private static void vector(Vector3fc a, Vector3fc b) {
        near(a.x(), b.x()); near(a.y(), b.y()); near(a.z(), b.z());
    }
    private static void rejects(Runnable action) {
        checks++;
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid input accepted");
    }
    public static void main(String[] args) {
        var fov = new StereoCamera.Fov(-1.2f, 0.8f, -0.7f, 1.1f);
        var center = new Vector3f(3, 2, -4);
        for (float yaw : new float[]{0, 0.7f, 1.5707963f, -2.1f}) {
            for (float pitch : new float[]{0, 0.5f, -0.4f}) {
                var rotation = StereoCamera.viewpointLook(yaw, pitch);
                var direction = rotation.transform(new Vector3f(0, 0, -1));
                vector(direction, new Vector3f(-(float)java.lang.Math.cos(yaw)*(float)java.lang.Math.cos(pitch),
                        (float)java.lang.Math.sin(pitch), -(float)java.lang.Math.sin(yaw)*(float)java.lang.Math.cos(pitch)));
                var pair = StereoCamera.synthetic(center, rotation, 0.064f, fov, fov);
                near(pair.left().position().distance(pair.right().position()), 0.064f);
                vector(new Vector3f(pair.left().position()).add(pair.right().position()).mul(0.5f), center);
                for (var eye : new StereoCamera.Eye[]{pair.left(), pair.right()}) {
                    vector(eye.view().transformPosition(new Vector3f(eye.position())), new Vector3f());
                    vector(eye.view().transformDirection(new Vector3f(direction)), new Vector3f(0,0,-1));
                }
            }
        }
        var rotation = new Quaternionf().rotationXYZ(0.2f, 0.4f, 0.8f);
        var before = new Quaternionf(rotation);
        var pair = StereoCamera.synthetic(center, rotation, 0.064f, fov, fov);
        vector(new Vector3f(pair.right().position()).sub(pair.left().position()),
                rotation.transform(new Vector3f(0.064f,0,0)));
        vector(center, new Vector3f(3,2,-4));
        near(rotation.dot(before), 1);
        for (float x : new float[]{fov.left(), fov.right()}) {
            for (float y : new float[]{fov.down(), fov.up()}) {
                var clip = pair.left().projection().transform(new Vector4f(x*StereoCamera.NEAR,y*StereoCamera.NEAR,-StereoCamera.NEAR,1));
                near(clip.x/clip.w, x == fov.left() ? -1 : 1);
                near(clip.y/clip.w, y == fov.down() ? -1 : 1);
                near(clip.z/clip.w, -1);
            }
        }
        var far = pair.left().projection().transform(new Vector4f(0,0,-StereoCamera.FAR,1));
        near(far.z/far.w,1);
        vector(StereoCamera.worldDelta(1,2,3), new Vector3f(-1,3*(float)java.lang.Math.sqrt(6),-2));
        rejects(() -> new StereoCamera.Fov(1,-1,-1,1));
        rejects(() -> new StereoCamera.Fov(Float.NaN,1,-1,1));
        rejects(() -> StereoCamera.synthetic(center, rotation, -0.064f, fov, fov));
        rejects(() -> StereoCamera.eye(center, new Quaternionf(0,0,0,0), new Vector3f(), fov));
        rejects(() -> StereoCamera.viewpointLook(Float.NaN,0));
        System.out.println("StereoCamera: " + checks + " checks passed");
    }
}
