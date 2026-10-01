package viewpointvr.diagnostic;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import me.zed_0xff.zombie_buddy.Exposer.LuaClass;
import me.zed_0xff.zombie_buddy.Loader;
import viewpointvr.*;

@LuaClass(name="ProjectViewpointVR")
public final class Main {
    private static boolean attempted;
    private static volatile Installation installation;
    private static volatile CaptureController captures;
    private static volatile String status="Not initialized";
    /** Called by ZombieBuddy in the user's game only. Never invoked by offline tests. */
    public static synchronized void main(String[] args) {
        if(attempted) return; attempted=true;
        try {
            if(zombie.network.GameClient.client || zombie.network.GameServer.server) throw new IllegalStateException("Single player only");
            Class<?> drawer=Class.forName("viewpoint.SceneDrawer",false,Main.class.getClassLoader());
            Path game=BinaryPins.location(zombie.GameWindow.class), mod=BinaryPins.location(drawer), loader=BinaryPins.location(Loader.class);
            BinaryPins.verify(game,mod,loader);
            ClassLoader owner=drawer.getClassLoader();
            var field=Loader.class.getDeclaredField("g_instrumentation"); field.setAccessible(true);
            Instrumentation instrumentation=(Instrumentation)field.get(null);
            Installation installed=Installation.install(instrumentation,owner,mod);
            installation=installed;
            var access=new ViewpointBackend.Access(owner);
            Path output=Path.of(zombie.ZomboidFileSystem.instance.getCacheDir(),"Project-Viewpoint-VR");
            captures=new CaptureController(installed::ready,Main::captureKey,System::nanoTime,(scene,original)-> {
                try(var target=new CaptureOutput(new LwjglGraphics(),output)) {
                    ViewpointBackend.synthetic(access,target,.064f).render(scene,original);
                    return target.published()?"Saved stereo pair: "+target.result():"Skipped: use Viewpoint first person, on foot, single player";
                }
            });
            FrameBoundary.dispatcher(captures);
            status=captures.status();
        } catch(Throwable error) {
            FrameBoundary.dispatcher(null);
            if(installation!=null) try { installation.close(); } catch(Throwable rollback) { error.addSuppressed(rollback); }
            status="Disabled: "+error; error.printStackTrace();
        }
        System.out.println("[Project Viewpoint VR] "+status);
    }
    private static boolean captureKey() {
        long window=org.lwjglx.opengl.Display.getWindow();
        boolean focused=window!=0 && org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(window,org.lwjgl.glfw.GLFW.GLFW_FOCUSED)==org.lwjgl.glfw.GLFW.GLFW_TRUE;
        return CaptureController.captureChord(focused,org.lwjglx.input.Keyboard::isKeyDown);
    }
    public static String requestCapture() { return captures==null?status:captures.request(); }
    public static String status() {
        if(installation!=null && !installation.ready()) return installation.failure()==null?status:"Disabled: "+installation.failure();
        return captures==null?status:captures.status();
    }
}
