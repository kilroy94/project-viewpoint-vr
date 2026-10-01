package viewpoint.input;
public final class Camera {
 public static void eye(viewpoint.core.Frame frame,float yaw,float[] eye){
  viewpoint.render.Probe.hit("cameraEye");float forward=.12f+frame.eyeLean;
  eye[0]=frame.eyeX-(float)Math.cos(yaw)*forward;eye[1]=frame.eyeY;eye[2]=frame.eyeZ-(float)Math.sin(yaw)*forward;
 }
}
