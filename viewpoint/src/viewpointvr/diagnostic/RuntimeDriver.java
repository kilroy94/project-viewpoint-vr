package viewpointvr.diagnostic;

import java.util.function.*;
import viewpointvr.*;

/** Cross-thread requests; all native/session/output work remains on the scene render thread. */
public final class RuntimeDriver implements FrameBoundary.Driver {
    public enum Mode {OFF,DESKTOP,XR_FIXED,XR_TRACKED}
    public interface Pipeline {
        boolean eligible(Object drawer) throws Throwable;
        boolean desktop(Object drawer,FrameBoundary.NativeDraw original) throws Throwable;
        boolean xr(Object drawer,FrameBoundary.NativeDraw original,boolean tracked,boolean recenter) throws Throwable;
        void idle() throws Throwable;
        void close() throws Throwable;
        void scale(float value);
    }
    public static final class Unavailable extends Exception {
        private static final long serialVersionUID=1L;
        public Unavailable(Throwable cause){super("OpenXR unavailable: "+cause,cause);}
    }
    private final Pipeline pipeline;
    private final CaptureController capture;
    private final BooleanSupplier ready,stop;
    private final LongSupplier clock;
    private volatile Mode requested=Mode.OFF;
    private Mode active=Mode.OFF;
    private volatile String status="Off - choose Desktop, XR fixed, or XR tracked";
    private volatile long recenterAt;
    private volatile float scale=1;
    private boolean worldSeen;
    private volatile boolean failed;
    private int warmup;
    public RuntimeDriver(Pipeline pipeline,CaptureController capture,BooleanSupplier ready,BooleanSupplier stop,LongSupplier clock) {
        this.pipeline=pipeline;this.capture=capture;this.ready=ready;this.stop=stop;this.clock=clock;
    }
    public String mode(String name) {
        Mode next=Mode.valueOf(name);
        if(failed&&next!=Mode.OFF)return status="Disabled after rendering failure; restart before retrying";
        requested=next;return status="Requested "+next;
    }
    public String scale(double value) {
        if(requested!=Mode.OFF)return "Switch Off before changing scale";
        if(!Double.isFinite(value)||value<.25||value>4)return "Scale must be 0.25..4 scene units/meter";
        scale=(float)value;return status="Scale: "+scale+" scene units/meter";
    }
    public String recenter() {recenterAt=clock.getAsLong()+5_000_000_000L;return status="Recenter in 5 seconds: face forward";}
    public String status() {
        long at=recenterAt;
        if(at!=0&&clock.getAsLong()<at)return "Recenter in "+Math.max(1,(at-clock.getAsLong()+999_999_999L)/1_000_000_000L)+"s: face forward";
        return active==Mode.OFF?status+" | "+capture.status():status+" | Pause/Break: Off";
    }
    private void transition() throws Throwable {
        if(stop.getAsBoolean()||!ready.getAsBoolean())requested=Mode.OFF;
        if(requested!=active) {
            Visibility.off();pipeline.close();active=requested;recenterAt=0;warmup=4;
            pipeline.scale(scale);status=active==Mode.OFF?"Off":active+" warming visibility";
        }
    }
    @Override public void render(Object drawer,FrameBoundary.NativeDraw original) throws Throwable {
        worldSeen=true;
        try {
            transition();
            if(active==Mode.OFF){capture.render(drawer,original);return;}
            if(!pipeline.eligible(drawer)){Visibility.off();pipeline.idle();original.draw();status="Suspended: first person, on foot, single player required";return;}
            Visibility.touch();
            if(warmup>0){warmup--;original.draw();return;}
            if(active==Mode.DESKTOP){pipeline.desktop(drawer,original);status="Desktop stereo active (no PNG readback)";return;}
            long at=recenterAt;boolean recenter=at!=0&&clock.getAsLong()>=at;
            if(recenter)recenterAt=0;
            boolean rendered=pipeline.xr(drawer,original,active==Mode.XR_TRACKED,recenter);
            status=rendered?active+" projection pairs submitted":"OpenXR waiting for session/tracking";
        } catch(Unavailable error) {
            requested=active=Mode.OFF;Visibility.off();pipeline.close();status=error.getMessage();System.err.println("[Project Viewpoint VR] "+status);original.draw();
        } catch(Throwable error) {
            failed=true;requested=active=Mode.OFF;Visibility.off();
            try{pipeline.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}
            status="Rendering failed; restart before retrying. See console.txt";throw error;
        }
    }
    /** Queued from the UI; pump empty XR frames if Viewpoint is disabled or not drawing. */
    public void idleTick() {
        try {transition();if(!worldSeen){Visibility.off();pipeline.idle();warmup=4;if(active!=Mode.OFF)status="Suspended: waiting for a Viewpoint world draw";}}catch(Throwable error){
            requested=active=Mode.OFF;Visibility.off();try{pipeline.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}
            status="OpenXR stopped: "+error;System.err.println("[Project Viewpoint VR] "+status);
        }finally{worldSeen=false;}
    }
}
