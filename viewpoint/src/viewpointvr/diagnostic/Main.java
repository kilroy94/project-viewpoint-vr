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
    private static volatile RuntimeDriver runtime;
    private static final java.util.concurrent.atomic.AtomicBoolean tickQueued=new java.util.concurrent.atomic.AtomicBoolean();
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
                    return target.published()?"Saved stereo pair: "+target.result():"Skipped: use Viewpoint first person, on foot, single player, native Viewpoint shaders";
                }
            });
            UiBridge.queue(action->zombie.core.SpriteRenderer.instance.drawGeneric(new zombie.core.textures.TextureDraw.GenericDrawer(){
                @Override public void render(){action.run();}
            }));
            viewpointvr.input.InputBridge.install(new viewpointvr.input.NativeInput(owner));
            runtime=new RuntimeDriver(new NativePipeline(access),captures,installed::ready,Main::stopKey,System::nanoTime);
            FrameBoundary.dispatcher(runtime);
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
    private static boolean stopKey() {
        return org.lwjglx.input.Keyboard.isKeyDown(org.lwjglx.input.Keyboard.KEY_PAUSE)
            && !org.lwjglx.input.Keyboard.isKeyDown(42)&&!org.lwjglx.input.Keyboard.isKeyDown(54)
            && !org.lwjglx.input.Keyboard.isKeyDown(29)&&!org.lwjglx.input.Keyboard.isKeyDown(157)
            && !org.lwjglx.input.Keyboard.isKeyDown(56)&&!org.lwjglx.input.Keyboard.isKeyDown(184);
    }
    public static void uiPointer(double x,double y,double width,double height){
        long window=org.lwjglx.opengl.Display.getWindow();
        boolean visible=window!=0&&org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(window,org.lwjgl.glfw.GLFW.GLFW_FOCUSED)!=0
            &&org.lwjgl.glfw.GLFW.glfwGetInputMode(window,org.lwjgl.glfw.GLFW.GLFW_CURSOR)==org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
        if(width>0&&height>0)UiBridge.cursor((float)(x/width),(float)(y/height),visible);
    }
    public static void uiBegin(){UiBridge.marker(0,true);}
    public static void uiEnd(){UiBridge.marker(0,false);}
    public static String turning(double mode,double angle,double speed){return viewpointvr.input.InputBridge.turning((int)mode,(int)angle,(int)speed);}
    public static String controllers(double value){viewpointvr.input.InputBridge.configure((int)value);return "Controllers: "+(int)value;}
    public static String mode(String name){return runtime==null?status:runtime.mode(name);}
    public static String recenter(){return runtime==null?status:runtime.recenter();}
    public static String scale(double value){return runtime==null?status:runtime.scale(value);}
    public static void tick() {
        if(runtime==null||!tickQueued.compareAndSet(false,true))return;
        zombie.core.SpriteRenderer.instance.drawGeneric(new zombie.core.textures.TextureDraw.GenericDrawer(){
            @Override public void render(){try{runtime.idleTick();}finally{tickQueued.set(false);}}
        });
    }
    public static String requestCapture() { return captures==null?status:captures.request(); }
    public static String status() {
        if(installation!=null && !installation.ready()) return installation.failure()==null?status:"Disabled: "+installation.failure();
        return runtime==null?status:runtime.status()+" | "+viewpointvr.input.InputBridge.status();
    }
}
