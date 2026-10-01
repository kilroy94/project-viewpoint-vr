package viewpointvr;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** First-person stage adapter for synthetic and tracked cameras; requires the verified native transforms. */
public final class ViewpointBackend implements StereoFrame.Backend<Object> {
    public record Extent(int width,int height) {
        public Extent { if(width<=0 || height<=0) throw new IllegalArgumentException("Empty target"); }
    }
    public record SavedOutput(int drawFbo,int readFbo,int x,int y,Extent viewport,boolean scissor,StereoFrame.Restore restore) {
        public SavedOutput { Objects.requireNonNull(viewport); Objects.requireNonNull(restore); }
    }
    public interface Output {
        /** Transactional capture of actual caller GL bindings/state. */
        SavedOutput save() throws Throwable;
        /** Bind an owned eye target and viewport; both eyes must have equal size. */
        Extent bind(int eye) throws Throwable;
        void copy(int eye) throws Throwable;
        void publish() throws Throwable;
    }

    /** Resolves metadata without initialization; constructor is exercised on the actual copied classes. */
    public static final class Access {
        private final Map<String,Field> fields=new HashMap<>();
        private final Map<String,MethodHandle> methods=new HashMap<>();
        public Access(ClassLoader loader) throws ReflectiveOperationException {
            Class<?> drawer=load(loader,"viewpoint.SceneDrawer"), frame=load(loader,"viewpoint.core.Frame");
            Class<?> scene=load(loader,"viewpoint.render.SceneData"), world=load(loader,"viewpoint.render.WorldRenderer");
            Class<?> context=load(loader,"viewpoint.render.FrameContext");
            add("frame",drawer,"frame",frame);
            add("scene",frame,"scene",scene); add("number",frame,"number",long.class); add("takenNanos",frame,"takenNanos",long.class);
            for(String n:List.of("eyeX","eyeY","eyeZ","viewYaw","viewPitch")) add(n,frame,n,float.class);
            add("projection",scene,"projection",Matrix4f.class);
            add("context",world,"frame",context);
            for(String n:List.of("handView","cullView")) add(n,world,n,Matrix4f.class);
            for(String n:List.of("handFromPlayer","cullFromPlayer","freeCamera")) add(n,world,n,boolean.class);
            for(String n:List.of("outputDrawFbo","outputReadFbo")) add(n,context,n,int.class);
            add("outputScissor",context,"outputScissor",boolean.class); add("viewport",context,"viewport",int[].class);
            add("generation",load(loader,"viewpoint.platform.HotReload"),"generation",int.class);
            add("third",load(loader,"viewpoint.input.ThirdPerson"),"active",boolean.class);
            add("free",load(loader,"viewpoint.input.FreeCam"),"active",boolean.class);
            add("client",load(loader,"zombie.network.GameClient"),"client",boolean.class);
            add("server",load(loader,"zombie.network.GameServer"),"server",boolean.class);
            Class<?> squares=load(loader,"viewpoint.core.CameraSquares");
            add("squares",frame,"cameraSquares",squares); add("seated",squares,"seated",boolean.class);
            method("begin",world,"begin",scene,Matrix4f.class,float.class,float.class,float.class);
            method("outlines",world,"drawOutlines");
            for(String n:List.of("drawTranslucent","drawIndirect","finish")) method(n,world,n,scene);
            method("weather",world,"drawWeather",scene,float.class,float.class,float.class);
            method("release",load(loader,"viewpoint.models.Models"),"release",frame);
            Class<?> look=load(loader,"viewpoint.input.Look");
            add("lookYaw",look,"yaw",float.class);add("lookPitch",look,"pitch",float.class);
            add("captured",look,"captured",boolean.class);add("readNanos",look,"readNanos",long.class);
            method("readLook",look,"readBeforeDrawing");
            method("eye",load(loader,"viewpoint.input.Camera"),"eye",frame,float.class,float[].class);
            add("iris",load(loader,"viewpoint.platform.IrisPacks"),"active",load(loader,"viewpoint.platform.IrisPacks$Active"));
            method("latency",load(loader,"viewpoint.render.Latency"),"viewBuilt",long.class,long.class,long.class,long.class);
            Class<?> retirement=load(loader,"viewpoint.render.Retirement");
            method("collect",retirement,"collect"); method("drawn",retirement,"drawn",long.class);
        }
        private static Class<?> load(ClassLoader loader,String name) throws ClassNotFoundException { return Class.forName(name,false,loader); }
        private void add(String key,Class<?> owner,String name,Class<?> type) throws ReflectiveOperationException {
            Field f=owner.getDeclaredField(name);
            if(f.getType()!=type) throw new NoSuchFieldException("Unexpected type for "+owner.getName()+"."+name);
            f.setAccessible(true); fields.put(key,f);
        }
        private void method(String key,Class<?> owner,String name,Class<?>... args) throws ReflectiveOperationException {
            Method m=owner.getDeclaredMethod(name,args); m.setAccessible(true);
            if(!Modifier.isStatic(m.getModifiers())) throw new NoSuchMethodException("Expected static "+name);
            methods.put(key,MethodHandles.lookup().unreflect(m));
        }
        Object get(String key,Object receiver) throws IllegalAccessException { return fields.get(key).get(receiver); }
        void set(String key,Object receiver,Object value) throws IllegalAccessException { fields.get(key).set(receiver,value); }
        Object call(String key,Object... args) throws Throwable { return methods.get(key).invokeWithArguments(args); }
        boolean eligible(Object frame) throws IllegalAccessException {
            return !(boolean)get("third",null) && !(boolean)get("free",null) && !(boolean)get("client",null)
                    && get("iris",null)==null && !(boolean)get("server",null) && !(boolean)get("seated",get("squares",frame));
        }
    }

    /** The caller must have verified all required target transforms before registering this driver. */
    public static FrameBoundary.Driver synthetic(Access access,Output output,float ipd) {
        if(!Float.isFinite(ipd) || ipd<0 || ipd>0.064f) throw new IllegalArgumentException("Diagnostic IPD must be 0..0.064");
        return cameras(access,output,(center,yaw,pitch,fov)->StereoCamera.synthetic(center,StereoCamera.viewpointLook(yaw,pitch),ipd,fov,fov));
    }
    @FunctionalInterface public interface CameraFactory {
        StereoCamera.Pair create(Vector3f center,float yaw,float pitch,StereoCamera.Fov nativeFov);
    }
    public static boolean eligible(Access access,Object drawer) throws IllegalAccessException {
        return access.eligible(access.get("frame",drawer));
    }
    public static FrameBoundary.Driver cameras(Access access,Output output,CameraFactory factory) {
        Objects.requireNonNull(access); Objects.requireNonNull(output); Objects.requireNonNull(factory);
        return (drawer,original)-> {
            Object frame=access.get("frame",drawer);
            if(!access.eligible(frame)) { original.draw(); return; }
            Object scene=access.get("scene",frame);
            Matrix4f projection=(Matrix4f)access.get("projection",scene);
            if(projection.m20()!=0 || projection.m21()!=0 || !Float.isFinite(projection.m00())
                    || !Float.isFinite(projection.m11()) || projection.m00()<=0 || projection.m11()<=0) {
                original.draw(); return;
            }
            access.call("readLook");
            float yaw=(float)access.get("lookYaw",null), pitch=(float)access.get("lookPitch",null);
            float[] nativeEye=new float[3];access.call("eye",frame,yaw,nativeEye);
            Vector3f center=new Vector3f(nativeEye[0],nativeEye[1],nativeEye[2]);
            var fov=new StereoCamera.Fov(-1/projection.m00(),1/projection.m00(),-1/projection.m11(),1/projection.m11());
            var pair=Objects.requireNonNull(factory.create(new Vector3f(center),yaw,pitch,fov));
            StereoFrame.render(frame,new ViewpointBackend(access,output,frame,scene,center,pair));
        };
    }
    private final Access access;
    private final Output output;
    private final Object frame,scene;
    private final long number;
    private final int generation;
    private final Vector3f center;
    private final StereoCamera.Pair pair;
    private StageHooks.Scope scope;
    private Extent extent;
    private boolean started;
    private ViewpointBackend(Access access,Output output,Object frame,Object scene,Vector3f center,StereoCamera.Pair pair) throws IllegalAccessException {
        this.access=access; this.output=output; this.frame=frame; this.scene=scene; this.center=center; this.pair=pair;
        number=(long)access.get("number",frame); generation=(int)access.get("generation",null);
    }
    public StereoFrame.Restore save(Object snapshot) throws Throwable {
        Matrix4f projection=new Matrix4f((Matrix4f)access.get("projection",scene));
        Matrix4f hand=new Matrix4f((Matrix4f)access.get("handView",null));
        Matrix4f cull=new Matrix4f((Matrix4f)access.get("cullView",null));
        Object handFlag=access.get("handFromPlayer",null), cullFlag=access.get("cullFromPlayer",null), freeFlag=access.get("freeCamera",null);
        Object context=access.get("context",null);
        SavedOutput saved=Objects.requireNonNull(output.save(),"saved output");
        try { scope=StageHooks.open(); }
        catch(Throwable failure) { try { saved.restore.restore(); } catch(Throwable restore) { failure.addSuppressed(restore); } throw failure; }
        return ()-> {
            Throwable failure=null;
            try {
                if((long)access.get("number",frame)==number && access.get("scene",frame)==scene)
                    ((Matrix4f)access.get("projection",scene)).set(projection);
                ((Matrix4f)access.get("handView",null)).set(hand);
                ((Matrix4f)access.get("cullView",null)).set(cull);
                access.set("handFromPlayer",null,handFlag); access.set("cullFromPlayer",null,cullFlag);access.set("freeCamera",null,freeFlag);
            } catch(Throwable error) { failure=error; }
            // The enclosing native catch invokes restoreOutput too: it must point at the real caller,
            // never the final eye FBO left in FrameContext by begin().
            try {
                access.set("outputDrawFbo",context,saved.drawFbo); access.set("outputReadFbo",context,saved.readFbo);
                access.set("outputScissor",context,saved.scissor);
                int[] viewport=(int[])access.get("viewport",context);
                viewport[0]=saved.x; viewport[1]=saved.y; viewport[2]=saved.viewport.width; viewport[3]=saved.viewport.height;
            } catch(Throwable error) { failure=StageHooks.append(failure,error); }
            try { saved.restore.restore(); } catch(Throwable error) { failure=StageHooks.append(failure,error); }
            try { scope.close(); } catch(Throwable error) { failure=StageHooks.append(failure,error); }
            if(failure!=null) throw failure;
        };
    }
    public boolean prepare(Object snapshot) throws Throwable {
        access.call("collect");
        access.call("latency",System.nanoTime(),(boolean)access.get("captured",null)?(long)access.get("readNanos",null):0L,number,(long)access.get("takenNanos",frame));
        extent=Objects.requireNonNull(output.bind(0));
        // Keep native hand/flashlight placement centered while the visual camera moves between eyes.
        access.set("handFromPlayer",null,true); access.set("cullFromPlayer",null,false);access.set("freeCamera",null,false);
        Matrix4f view=new Matrix4f(pair.left().view()).translate(new Vector3f(pair.left().position()).sub(center));
        ((Matrix4f)access.get("handView",null)).set(view);
        started=true;
        return (boolean)access.call("begin",scene,view,center.x,center.y,center.z);
    }
    public void validate(Object snapshot) throws Throwable {
        if(snapshot!=frame || access.get("scene",frame)!=scene || (long)access.get("number",frame)!=number
                || (int)access.get("generation",null)!=generation || !access.eligible(frame))
            throw new IllegalStateException("Borrowed frame, renderer generation or supported camera mode changed");
    }
    public void draw(Object snapshot,int eye) throws Throwable {
        scope.eye(eye);
        if(!extent.equals(output.bind(eye))) throw new IllegalStateException("Eye target size changed within pair");
        var camera=eye==0?pair.left():pair.right();
        ((Matrix4f)access.get("projection",scene)).set(camera.projection());
        Vector3f p=camera.position();
        if(!(boolean)access.call("begin",scene,camera.view(),p.x,p.y,p.z)) throw new IllegalStateException("Native eye begin failed");
        access.call("outlines"); access.call("drawTranslucent",scene); access.call("drawIndirect",scene);
        access.call("weather",scene,p.x,p.y,p.z); access.call("finish",scene);
    }
    public void copy(int eye) throws Throwable { output.copy(eye); }
    public void release(Object snapshot) throws Throwable {
        Throwable failure=null;
        try { scope.release(); } catch(Throwable error) { failure=error; }
        if(started) {
            try {
                if((long)access.get("number",frame)!=number) throw new IllegalStateException("Cannot release a recycled frame");
                access.call("release",frame);
            } catch(Throwable error) { failure=StageHooks.append(failure,error); }
            try { access.call("drawn",number); } catch(Throwable error) { failure=StageHooks.append(failure,error); }
        }
        if(failure!=null) throw failure;
    }
    public void publish() throws Throwable { output.publish(); }
}
