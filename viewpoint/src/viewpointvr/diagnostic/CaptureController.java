package viewpointvr.diagnostic;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import viewpointvr.*;

/** One request/one captured frame. Requests may originate on the game thread; work runs at the render hook. */
public final class CaptureController implements FrameBoundary.Driver {
    @FunctionalInterface public interface Capture { String run(Object drawer,FrameBoundary.NativeDraw original) throws Throwable; }
    private final BooleanSupplier ready,key;
    private final LongSupplier clock;
    private final Capture capture;
    private final AtomicLong deadline=new AtomicLong();
    private volatile String status="Ready: Ctrl+Shift+F10 saves one stereo pair";
    private volatile boolean failed;
    private boolean held;
    public CaptureController(BooleanSupplier ready,BooleanSupplier key,LongSupplier clock,Capture capture) {
        this.ready=ready; this.key=key; this.clock=clock; this.capture=capture;
    }
    public String request() {
        if(failed || !ready.getAsBoolean()) return status="Capture disabled; see console.txt";
        long end=clock.getAsLong()+10_000_000_000L;
        return status=deadline.compareAndSet(0,end)?"Capture queued; first person, on foot":"Capture already queued";
    }
    public String status() {
        long pending=deadline.get();
        if(pending!=0 && clock.getAsLong()-pending>=0 && deadline.compareAndSet(pending,0)) status="Capture expired: enter Viewpoint first person and try again";
        return status;
    }
    @Override public void render(Object drawer,FrameBoundary.NativeDraw original) throws Throwable {
        if(!ready.getAsBoolean() || failed) { deadline.set(0); original.draw(); return; }
        boolean down=key.getAsBoolean();
        if(down && !held) request(); held=down;
        long pending=deadline.getAndSet(0);
        if(pending==0) { original.draw(); return; }
        if(clock.getAsLong()-pending>=0) { status="Capture expired"; original.draw(); return; }
        try { status=capture.run(drawer,original); System.out.println("[Project Viewpoint VR] "+status); }
        catch(Throwable error) {
            failed=true; status="Capture failed; restart before retrying. See console.txt";
            System.err.println("[Project Viewpoint VR] "+status); error.printStackTrace(); throw error;
        }
    }
}
