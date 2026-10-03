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
    private static volatile SettingsStore settings;
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
            settings=new SettingsStore(output.resolve("settings.properties"));
            var turn=settings.turning();
            viewpointvr.input.InputBridge.turning(turn.mode(),turn.angle(),turn.speed());
            runtime.scale(settings.scale());
            ControllerDiagnostic.directory(output.resolve("controller-diagnostics"));
            try{ImGuiDiagnostic.install(owner);}catch(ReflectiveOperationException error){System.err.println("[Project Viewpoint VR] Diagnostic settings panel unavailable: "+error);}
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
    public static String turning(double mode,double angle,double speed){
        String result=viewpointvr.input.InputBridge.turning((int)mode,(int)angle,(int)speed);
        if(settings!=null)settings.turning(viewpointvr.input.InputBridge.turning);return result;
    }
    public static double savedTurnMode(){return settings==null?1:settings.turning().mode();}
    public static double savedSnapAngle(){return settings==null?30:settings.turning().angle();}
    public static double savedSmoothSpeed(){return settings==null?90:settings.turning().speed();}
    public static double savedWorldScale(){return settings==null?1:settings.scale();}
    public static void diagnosticTargets(String kind,double cx,double cy,double dx,double dy,double ex,double ey,double sx,double sy,double width,double height){
        try{ControllerDiagnostic.targets(new ControllerDiagnostic.Targets(kind,cx/width,cy/height,dx/width,dy/height,ex/width,ey/height,sx/width,sy/height,(int)width,(int)height),System.nanoTime());}catch(IllegalArgumentException error){ControllerDiagnostic.cancel("Invalid test surface bounds");}
    }
    public static String diagnosticStart(String kind){return ControllerDiagnostic.start(kind,System.nanoTime());}
    public static void diagnosticStop(){ControllerDiagnostic.cancel("Stopped by user");}
    public static double diagnosticPointerX(){var hit=viewpointvr.input.InputBridge.output.hit();return ControllerDiagnostic.active()&&hit!=null?hit.x():-1;}
    public static double diagnosticPointerY(){var hit=viewpointvr.input.InputBridge.output.hit();return ControllerDiagnostic.active()&&hit!=null?hit.y():-1;}
    public static String diagnosticStatus(){return ControllerDiagnostic.status();}
    public static void diagnosticEvent(String kind,String event,double value){ControllerDiagnostic.event(kind,event,value);}
    public static String controllers(double value){ControllerDiagnostic.cancel("Controller mode changed");viewpointvr.input.InputBridge.configure((int)value);return "Controllers: "+(int)value;}
    public static String mode(String name){ControllerDiagnostic.cancel("Renderer mode changed");return runtime==null?status:runtime.mode(name);}
    public static String recenter(){return runtime==null?status:runtime.recenter();}
    public static String scale(double value){return runtime==null?status:runtime.scale(value,accepted->{if(settings!=null)settings.scale(accepted);});}
    public static void tick() {
        if(stopKey())ControllerDiagnostic.cancel("Pause/Break pressed");
        ControllerDiagnostic.watchdog(System.nanoTime());
        if(runtime==null||!tickQueued.compareAndSet(false,true))return;
        zombie.core.SpriteRenderer.instance.drawGeneric(new zombie.core.textures.TextureDraw.GenericDrawer(){
            @Override public void render(){try{runtime.idleTick();}finally{tickQueued.set(false);}}
        });
    }
    public static String requestCapture() { return captures==null?status:captures.request(); }
    public static String status() {
        if(installation!=null && !installation.ready()) return installation.failure()==null?status:"Disabled: "+installation.failure();
        return runtime==null?status:runtime.status()+" | "+viewpointvr.input.InputBridge.status()+(settings!=null&&!settings.warning().isEmpty()?" | "+settings.warning():"");
    }
}
