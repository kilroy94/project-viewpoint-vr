package viewpoint.input;
public final class Look {
 public static float yaw=.6f,pitch=.1f; public static boolean captured=true;public static long readNanos=44;
 public static void readBeforeDrawing(){viewpoint.render.Probe.hit("lateLook");}
}
