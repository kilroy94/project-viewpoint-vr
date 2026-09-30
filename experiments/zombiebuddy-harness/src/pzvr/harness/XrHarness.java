package pzvr.harness;

import java.util.concurrent.atomic.AtomicBoolean;
import pzvr.*;
import pzvr.xr.*;

/** Game glue; all XR/GL calls run on the existing renderer/context thread. */
public final class XrHarness {
    private static OpenXrSession session;
    private static EyeCapture mirror;
    private static final XrCamera camera=new XrCamera();
    private static final TrackedArms arms=new TrackedArms();
    private static boolean armPreview;
    private static final RecenterCountdown recenterCountdown=new RecenterCountdown();
    private static int recenterHands;
    private static int width,height;
    private static volatile boolean active;
    private static volatile long lastDraw,lastWatch;
    private static final AtomicBoolean watchQueued=new AtomicBoolean();
    public static boolean active() { return active; }
    public static void toggle() {
        if(active) { stop("User toggle"); return; }
        try {
            session=new OpenXrSession(); active=true; lastDraw=System.nanoTime(); camera.recenter(); arms.reset(); armPreview=false;
            LiveMirror.suspend(); OpenXrSession.log("ON; use your Recenter or Toggle OpenXR shortcut (Options > Mods > PZ3D VR)");
        } catch(Throwable failure) { fail(failure); }
    }
    public static void recenter() {
        if(!active) return;
        pzvr.interaction.HandUse.clear();
        pzvr.melee.MeleeInput.reset();
        HandPoses hands=session.hands();
        recenterHands=(hands.left()!=null?1:0)|(hands.right()!=null?2:0);
        recenterCountdown.request(System.nanoTime());
        OpenXrSession.log("Recenter in 5 seconds; face forward and hold controllers neutrally");
    }
    public static String recenterStatus() { return recenterCountdown.message(); }
    public static void toggleArmPreview() {
        if(!active) { OpenXrSession.log("Start OpenXR before toggling synthetic arm preview"); return; }
        armPreview=!armPreview; arms.recenter();
        pzvr.interaction.HandUse.clear();
        pzvr.melee.MeleeInput.reset();
        recenterCountdown.cancel();
        OpenXrSession.log("Synthetic arm preview "+(armPreview?"ON":"OFF; using tracked controllers"));
    }
    public static void stop(String reason) {
        active=false;
        pzvr.interaction.HandUse.clear();
        pzvr.turn.TurnRuntime.heartbeat(false);
        pzvr.input.ControllerBridge.clear();
        pzvr.melee.MeleeInput.reset();
        recenterCountdown.cancel();
        arms.reset(); armPreview=false;
        if(mirror!=null) { mirror.close(); mirror=null; }
        if(session!=null) { try { session.close(); } finally { session=null; } }
        OpenXrSession.log("OFF: "+reason);
    }
    private static void fail(Throwable error) {
        try { stop("XR unavailable or failed; desktop rendering resumes"); }
        catch(Throwable cleanup) { error.addSuppressed(cleanup); }
        error.printStackTrace();
    }
    public static boolean draw(VersionGate.Verified verified,Object frame,boolean fresh,int w,int h) {
        if(!active) return false;
        lastDraw=System.nanoTime();
        boolean[] invoked={false};
        try {
            if(mirror==null || width!=w || height!=h) {
                if(mirror!=null) mirror.close(); mirror=new EyeCapture(w,h); width=w; height=h;
            }
            boolean rendered=session.frame((head,views,sink)-> {
                // A runtime reference-space change reanchors the camera; it is not a user
                // request to learn a new controller-to-hand orientation from an arbitrary pose.
                if(session.consumeRecenter()) { camera.recenter(); pzvr.melee.MeleeInput.reset(); pzvr.interaction.HandUse.clear(); }
                pzvr.turn.TurnRuntime.heartbeat(session.focused() && !recenterCountdown.pending());
                HandPoses hands=armPreview?TrackedArms.synthetic(head,System.nanoTime()*1e-9):session.hands();
                boolean tracked=((recenterHands&1)==0 || hands.left()!=null) && ((recenterHands&2)==0 || hands.right()!=null);
                if(recenterCountdown.update(System.nanoTime(),session.focused() && tracked)) {
                    camera.recenter(); arms.recenter(); OpenXrSession.log("Recenter complete");
                    pzvr.melee.MeleeInput.reset();
                }
                pzvr.melee.MeleeInput.sample(System.nanoTime(),session.handPoseTime(),hands.right(),head,session.combatHeld(),
                    session.focused() && session.combatTracking() && !pzvr.turn.TurnRuntime.meleeBlocked() && !armPreview && !recenterCountdown.pending());
                mirror.beginPair(); invoked[0]=true;
                FrameTiming timing=session.timing();
                PairHooks.render(verified,frame,fresh,camera.eyes(head,views),new PairHooks.Sink() {
                    private long stamp=System.nanoTime();
                    public void beginEye(int eye) {
                        long now=System.nanoTime();
                        if(eye==0) timing.add(FrameTiming.Stage.PREPARE,now-stamp);
                        stamp=now;
                    }
                    public void copy(int eye,int source,int sw,int sh) {
                        long now=System.nanoTime();
                        timing.add(eye==0?FrameTiming.Stage.LEFT:FrameTiming.Stage.RIGHT,now-stamp);
                        mirror.copy(eye,source,sw,sh);
                        timing.add(FrameTiming.Stage.MIRROR_COPY,System.nanoTime()-now);
                        sink.copy(eye,source,sw,sh);
                    }
                },(prepared,base)->{
                    pzvr.interaction.HandUse.capture(prepared,camera.sceneFromLocal(),head,hands,session.focused()&&!armPreview&&!recenterCountdown.pending());
                    return arms.apply(prepared,camera.sceneFromLocal(),head,hands,camera.shoulderShift());
                });
                VanillaUi.copy(session);
                long stamp=System.nanoTime();
                mirror.mirrorStereo();
                timing.add(FrameTiming.Stage.MIRROR_PRESENT,System.nanoTime()-stamp);
            });
            lastDraw=System.nanoTime();
            if(!rendered) { pzvr.melee.MeleeInput.reset(); pzvr.input.ControllerBridge.clear(); pzvr.interaction.HandUse.clear(); }
            return rendered;
        } catch(Throwable failure) {
            fail(failure);
            if(invoked[0]) {
                try { frame.getClass().getMethod("draw",boolean.class).invoke(frame,false); }
                catch(Throwable fallback) { fallback.printStackTrace(); }
                return true;
            }
            return false;
        }
    }
    /** Lua heartbeat handles leaving PZ3D even when no more Frame.draw calls arrive. */
    public static void watchdog() {
        long now=System.nanoTime();
        if(!active || now-lastWatch<500_000_000L || !watchQueued.compareAndSet(false,true)) return;
        lastWatch=now;
        zombie.core.opengl.RenderThread.queueInvokeOnRenderContext(()-> {
            try {
                if(active && (!com.pavelvoronin.pz3d.Main.active() || com.pavelvoronin.pz3d.Main.failed()
                    || System.nanoTime()-lastDraw>2_000_000_000L)) stop("Renderer inactive");
            } finally { watchQueued.set(false); }
        });
    }
}
