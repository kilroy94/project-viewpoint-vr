package viewpointvr.diagnostic;

import viewpointvr.*;

/** Reuses two owned targets. No readback or PNG work in the continuous path. */
public final class LiveOutput implements ViewpointBackend.Output,AutoCloseable {
    @FunctionalInterface public interface Copy {void copy(int eye,int fbo,int width,int height) throws Throwable;}
    private final LwjglGraphics gl;
    private final Thread owner=Thread.currentThread();
    private final int[] targets=new int[2];
    private int width,height,wantedWidth,wantedHeight,copied;
    private ViewpointBackend.SavedOutput saved;
    private boolean restored,active,published;
    private Copy copy;
    public LiveOutput(LwjglGraphics gl) {this.gl=gl;}
    public void begin(int width,int height,Copy copy) {
        check();if(active)throw new IllegalStateException("Output frame still active");
        wantedWidth=width;wantedHeight=height;this.copy=copy;published=false;
    }
    private void check() {if(Thread.currentThread()!=owner)throw new IllegalStateException("Output thread changed");}
    public boolean published() {return published;}
    public ViewpointBackend.SavedOutput save() throws Throwable {
        check();if(active)throw new IllegalStateException("Nested output save");
        saved=gl.save();active=true;restored=false;copied=0;
        try {
            int w=wantedWidth,h=wantedHeight;
            if(w<=0||h<=0) {float factor=Math.min(1f,1280f/Math.max(saved.viewport().width(),saved.viewport().height()));w=Math.max(1,Math.round(saved.viewport().width()*factor));h=Math.max(1,Math.round(saved.viewport().height()*factor));}
            if(w!=width||h!=height) {
                release();int first=gl.create(w,h);
                try {targets[1]=gl.create(w,h);targets[0]=first;}catch(Throwable error){gl.delete(first);throw error;}
                width=w;height=h;
            }
            return new ViewpointBackend.SavedOutput(saved.drawFbo(),saved.readFbo(),saved.x(),saved.y(),saved.viewport(),saved.scissor(),()->{restored=true;active=false;saved.restore().restore();});
        } catch(Throwable error) {active=false;try{saved.restore().restore();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public ViewpointBackend.Extent bind(int eye) {check();if(!active||eye<0||eye>1)throw new IllegalStateException("No output frame");gl.bind(targets[eye],width,height);return new ViewpointBackend.Extent(width,height);}
    public void copy(int eye) throws Throwable {
        check();if(!active||eye!=copied)throw new IllegalStateException("Eye copy order");
        if(copy!=null)copy.copy(eye,targets[eye],width,height);copied++;
    }
    public void publish() throws Throwable {check();if(!restored||copied!=2)throw new IllegalStateException("Incomplete output pair");gl.mirrorPair(targets[0],targets[1],width,height,saved);published=true;}
    private void release() {for(int i=0;i<2;i++){if(targets[i]!=0)gl.delete(targets[i]);targets[i]=0;}width=height=0;}
    public void close() {check();if(active)throw new IllegalStateException("Close during pair");release();}
}
