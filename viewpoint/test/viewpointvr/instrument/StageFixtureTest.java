package viewpointvr.instrument;

import java.net.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import org.joml.Matrix4f;
import viewpointvr.*;

/** Executes only independent synthetic classes through the production transform/backend. */
public final class StageFixtureTest {
    private static int checks;
    private static void check(boolean ok,String message) { checks++; if(!ok) throw new AssertionError(message); }
    static final class Loader extends URLClassLoader {
        final Path root;
        Loader(Path root) throws Exception { super(new URL[]{root.toUri().toURL()},StageFixtureTest.class.getClassLoader()); this.root=root; }
        @Override protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException {
            if(!name.startsWith("viewpoint.") && !name.startsWith("zombie.network.")) return super.loadClass(name,resolve);
            synchronized(getClassLoadingLock(name)) {
                Class<?> type=findLoadedClass(name);
                if(type==null) {
                    try {
                        String internal=name.replace('.','/');
                        byte[] bytes=Files.readAllBytes(root.resolve(internal+".class"));
                        if(internal.equals(EntryTransform.TARGET)) bytes=EntryTransform.patch(bytes);
                        if(StageTransform.TARGETS.contains(internal)) bytes=StageTransform.patch(internal,bytes);
                        type=defineClass(name,bytes,0,bytes.length);
                    } catch(Exception error) { throw new ClassNotFoundException(name,error); }
                }
                if(resolve) resolveClass(type); return type;
            }
        }
    }
    private static Object get(Object o,String name) throws Exception { Field f=o.getClass().getField(name); return f.get(o); }
    private static void set(Object o,String name,Object value) throws Exception { o.getClass().getField(name).set(o,value); }
    private static Object staticGet(Class<?> c,String name) throws Exception { return c.getField(name).get(null); }
    private static long count(List<String> events,String name) { return events.stream().filter(name::equals).count(); }
    @SuppressWarnings({"unchecked","try"})
    public static void main(String[] args) throws Throwable {
        List<String> scenarios=List.of("inactive","success","skipped","models","world","rightWorld","finish","copy0","copy1","texturesRelease","restore","extent","recycle","generation","third","iris","irisSwitch","offAxis");
        for(String scenario:scenarios) try(var loader=new Loader(Path.of(args[0]))) {
            var access=new ViewpointBackend.Access(loader);
            Class<?> drawerType=loader.loadClass("viewpoint.SceneDrawer");
            Class<?> probe=loader.loadClass("viewpoint.render.Probe");
            Class<?> world=loader.loadClass("viewpoint.render.WorldRenderer");
            Object drawer=drawerType.getConstructor().newInstance();
            Object frame=drawerType.getMethod("snapshot").invoke(drawer), scene=get(frame,"scene");
            Matrix4f projection=(Matrix4f)get(scene,"projection");
            if(scenario.equals("offAxis")) projection.m20(.1f);
            Matrix4f before=new Matrix4f(projection);
            List<String> events=(List<String>)staticGet(probe,"events");
            if(scenario.equals("inactive")) {
                world.getMethod("begin",scene.getClass(),Matrix4f.class,float.class,float.class,float.class)
                        .invoke(null,scene,new Matrix4f(),0f,0f,0f);
                world.getMethod("finish",scene.getClass()).invoke(null,scene);
                check(count(events,"world")==1 && count(events,"stream")==1 && count(events,"texturesRelease")==1,"Inactive transformed stages preserve ordinary work");
                check(events.contains("iris") && events.contains("jitter") && events.contains("resolve") && events.contains("passes") && events.contains("packHistory"),"Inactive scope preserves native temporal/pack behavior");
                continue;
            }
            List<Matrix4f> views=new ArrayList<>();
            List<Matrix4f> hands=new ArrayList<>();
            if(List.of("models","world","finish","texturesRelease").contains(scenario)) probe.getField("fail").set(null,scenario);
            if(scenario.equals("rightWorld")) { probe.getField("fail").set(null,"world"); probe.getField("failOccurrence").setInt(null,2); }
            if(scenario.equals("skipped")) probe.getField("skip").setBoolean(null,true);
            if(scenario.equals("third")) loader.loadClass("viewpoint.input.ThirdPerson").getField("active").setBoolean(null,true);
            if(scenario.equals("iris"))loader.loadClass("viewpoint.platform.IrisPacks").getField("active").set(null,loader.loadClass("viewpoint.platform.IrisPacks$Active").getConstructor().newInstance());
            var output=new ViewpointBackend.Output() {
                public ViewpointBackend.SavedOutput save() {
                    events.add("save");
                    return new ViewpointBackend.SavedOutput(91,92,1,2,new ViewpointBackend.Extent(800,600),true,()-> {
                        probe.getField("bound").setInt(null,91); events.add("restore");
                        if(scenario.equals("restore")) throw new IllegalStateException("output restoration failed");
                    });
                }
                public ViewpointBackend.Extent bind(int eye) throws Exception {
                    probe.getField("bound").setInt(null,100+eye); events.add("bind"+eye);
                    return new ViewpointBackend.Extent(scenario.equals("extent") && eye==1?401:400,300);
                }
                public void copy(int eye) throws Exception {
                    events.add("copy"+eye);
                    check(!events.contains("texturesRelease") && !events.contains("drawersRelease"),"Resources survive both copies");
                    Object context=world.getMethod("context").invoke(null);
                    check((long)get(context,"index")==1,"One native frame index advance");
                    check(Math.abs((float)get(context,"frameSeconds")-.5f)<1e-6f,"Equal frozen delta time per eye");
                    check(!(boolean)get(context,"previousValid"),"No mixed-eye velocity history");
                    views.add(new Matrix4f((Matrix4f)get(context,"view")));
                    hands.add(new Matrix4f((Matrix4f)staticGet(world,"handView")));
                    if(scenario.equals("copy"+eye)) throw new IllegalStateException("copy failure");
                    if(scenario.equals("recycle") && eye==0) set(frame,"number",8L);
                    if(scenario.equals("irisSwitch") && eye==0)loader.loadClass("viewpoint.platform.IrisPacks").getField("active").set(null,loader.loadClass("viewpoint.platform.IrisPacks$Active").getConstructor().newInstance());
                    if(scenario.equals("generation") && eye==0) loader.loadClass("viewpoint.platform.HotReload").getField("generation").setInt(null,1);
                }
                public void publish() { events.add("publish"); }
            };
            try(var scope=FrameBoundary.register(ViewpointBackend.synthetic(access,output,.064f))) { drawerType.getMethod("render").invoke(drawer); }
            Throwable caught=(Throwable)get(drawer,"caught");
            if(scenario.equals("third") || scenario.equals("iris") || scenario.equals("offAxis")) {
                check((int)get(drawer,"nativeCalls")==1 && !events.contains("save"),"Unsupported camera delegates ordinary draw");
                continue;
            }
            check((int)get(drawer,"nativeCalls")==0,"Native whole-frame method never replayed");
            check(projection.equals(before),"Scene projection restored on "+scenario);
            check((boolean)staticGet(world,"handFromPlayer") && (boolean)staticGet(world,"cullFromPlayer")
                    && (boolean)staticGet(world,"freeCamera") && ((Matrix4f)staticGet(world,"handView")).equals(new Matrix4f()),"Borrowed native hand/culling state restored");
            check(count(events,"restore")==1 && (int)staticGet(probe,"bound")==91,"Outer failure handler also restores caller output: "+scenario);
            check(count(events,"texturesRelease")== (scenario.equals("skipped")?0:1),"Acquired model textures released once even when prepare/eye fails: "+scenario);
            check(count(events,"retirement")==1,"Retirement once at pair end");
            check(count(events,"drawersRelease")== (scenario.equals("recycle")?0:1),"Never release a recycled frame's drawers");
            check(!events.contains("jitter") && !events.contains("resolve") && !events.contains("remember")
                    && !events.contains("iris") && !events.contains("passes") && !events.contains("packHistory") && !events.contains("indirect"),"Diagnostic history/effects suppressed");
            if(!scenario.equals("skipped")) check(count(events,"stream")==1 && count(events,"models")==1 && count(events,"uniforms")==1,"Shared preparation once");
            if(scenario.equals("success")) {
                check(caught==null,"Successful synthetic pair: "+caught);
                check(count(events,"world")==2 && count(events,"frustum")==2 && count(events,"mask")==2,"Per-eye world/frusta/masks twice");
                for(String name:List.of("shells","cells","bandLight","cellLight","trees","clock","developer","pack","floors","ground","uploads","meshes","shadows","weatherMap","mousePick","farShadows","lateLook","cameraEye","latency"))
                    check(count(events,name)==1,"Shared native stage once: "+name);
                check(count(events,"uniformBind")==2,"Per-eye uniform texture rebinding");
                check(views.size()==2 && !views.get(0).equals(views.get(1)),"Distinct eye cameras reach native begin");
                var eye=new org.joml.Vector3f();hands.get(0).origin(eye);
                check(eye.distance(new org.joml.Vector3f(3-(float)Math.cos(.6)*.32f,2,4-(float)Math.sin(.6)*.32f))<1e-5f,"Late yaw and native lean reach centered camera");
                check(hands.size()==2 && hands.get(0).equals(hands.get(1)),"Native hand/flashlight camera stays centered for both eyes");
                check(events.indexOf("copy1")<events.indexOf("texturesRelease") && events.indexOf("restore")<events.indexOf("publish"),"Final copy, cleanup, restore, publish ordering");
            } else if(scenario.equals("skipped")) check(caught==null && !events.contains("publish") && !events.contains("world"),"Native begin false skips both eyes and still restores state");
            else check(caught!=null && !events.contains("publish"),"Failed/incomplete pair never published: "+scenario);
            // No leaked native stage scope after any exception.
            var recovery=StageHooks.open(); recovery.release(); recovery.close(); checks++;
        }
        System.out.println("Native stage fixture: "+checks+" checks passed through transformed synthetic native classes");
    }
}
