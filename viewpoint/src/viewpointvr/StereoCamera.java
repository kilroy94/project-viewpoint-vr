package viewpointvr;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Pure camera math; no Viewpoint/game/GL entry points or global state. */
public final class StereoCamera {
    public static final float NEAR = 0.05f;
    public static final float FAR = 400f;

    public record Fov(float left, float right, float down, float up) {
        public Fov {
            if (!finite(left, right, down, up) || left >= right || down >= up)
                throw new IllegalArgumentException("FOV tangents must be finite and ordered");
        }
    }
    public record Eye(Vector3f position, Matrix4f view, Matrix4f projection) {}
    public record Pair(Eye left, Eye right) {}

    /** Viewpoint Look.yaw/pitch are radians; yaw zero faces scene -X. */
    public static Quaternionf viewpointLook(float yaw, float pitch) {
        if (!finite(yaw, pitch)) throw new IllegalArgumentException("Invalid look angles");
        return new Quaternionf().rotationY((float) Math.PI / 2f - yaw).rotateX(pitch);
    }

    public static Pair synthetic(Vector3fc center, Quaternionfc orientation, float ipd,
                                 Fov left, Fov right) {
        if (!finite(ipd) || ipd < 0 || ipd > 0.2f)
            throw new IllegalArgumentException("IPD must be between 0 and 0.2 scene units");
        return new Pair(eye(center, orientation, new Vector3f(-ipd / 2, 0, 0), left),
                        eye(center, orientation, new Vector3f(ipd / 2, 0, 0), right));
    }

    /** eyeOffset is in head-local coordinates; orientation maps head-local to scene. */
    public static Eye eye(Vector3fc center, Quaternionfc orientation, Vector3fc eyeOffset, Fov fov) {
        if (!finite(center.x(), center.y(), center.z(), eyeOffset.x(), eyeOffset.y(), eyeOffset.z(),
                    orientation.x(), orientation.y(), orientation.z(), orientation.w())
                || !Float.isFinite(orientation.lengthSquared()) || orientation.lengthSquared() < 1e-12f)
            throw new IllegalArgumentException("Invalid pose");
        Quaternionf rotation = new Quaternionf(orientation).normalize();
        Vector3f position = rotation.transform(new Vector3f(eyeOffset)).add(center);
        Matrix4f view = new Matrix4f().translationRotate(position.x, position.y, position.z, rotation).invert();
        Matrix4f projection = new Matrix4f().setFrustum(
                fov.left * NEAR, fov.right * NEAR, fov.down * NEAR, fov.up * NEAR, NEAR, FAR);
        return new Eye(position, view, projection);
    }

    /** World tile displacement to Viewpoint scene displacement (not a meter calibration). */
    public static Vector3f worldDelta(float dx, float dy, float dz) {
        if (!finite(dx, dy, dz)) throw new IllegalArgumentException("Invalid world displacement");
        return new Vector3f(-dx, dz * (float) Math.sqrt(6), -dy);
    }

    private static boolean finite(float... values) {
        for (float value : values) if (!Float.isFinite(value)) return false;
        return true;
    }
    private StereoCamera() {}
}
