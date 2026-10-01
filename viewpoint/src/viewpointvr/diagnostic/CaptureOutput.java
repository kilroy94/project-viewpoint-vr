package viewpointvr.diagnostic;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;
import viewpointvr.*;

/** Owned final-color targets and complete-pair publication. All GL operations stay on the render thread. */
public final class CaptureOutput implements ViewpointBackend.Output,AutoCloseable {
    public interface Graphics {
        ViewpointBackend.SavedOutput save() throws Throwable;
        int create(int width,int height) throws Throwable;
        void delete(int target);
        void bind(int target,int width,int height) throws Throwable;
        BufferedImage read(int target,int width,int height) throws Throwable;
        void mirror(int target,int width,int height,ViewpointBackend.SavedOutput destination) throws Throwable;
    }
    private final Graphics graphics;
    private final Path root;
    private final Thread owner=Thread.currentThread();
    private final int[] targets=new int[2];
    private final BufferedImage[] images=new BufferedImage[2];
    private ViewpointBackend.SavedOutput saved;
    private int width,height;
    private boolean published,closed,restored;
    private Path result;
    public CaptureOutput(Graphics graphics,Path root) { this.graphics=Objects.requireNonNull(graphics); this.root=Objects.requireNonNull(root); }
    private void owner() { if(Thread.currentThread()!=owner || closed) throw new IllegalStateException("Output scope/thread mismatch"); }
    public ViewpointBackend.SavedOutput save() throws Throwable {
        owner(); if(saved!=null) throw new IllegalStateException("Output is one-shot");
        saved=Objects.requireNonNull(graphics.save());
        try {
            double scale=Math.min(1,1280.0/Math.max(saved.viewport().width(),saved.viewport().height()));
            width=Math.max(1,(int)Math.round(saved.viewport().width()*scale));
            height=Math.max(1,(int)Math.round(saved.viewport().height()*scale));
            for(int i=0;i<2;i++) {
                targets[i]=graphics.create(width,height);
                if(targets[i]==0 || i==1 && targets[0]==targets[1]) throw new IllegalStateException("Invalid eye target");
            }
        } catch(Throwable error) {
            try { close(); } catch(Throwable cleanup) { error=StageHooks.append(error,cleanup); }
            try { saved.restore().restore(); } catch(Throwable restore) { error=StageHooks.append(error,restore); }
            throw error;
        }
        return new ViewpointBackend.SavedOutput(saved.drawFbo(),saved.readFbo(),saved.x(),saved.y(),saved.viewport(),saved.scissor(),()-> {
            if(restored) throw new IllegalStateException("Output already restored");
            restored=true; saved.restore().restore();
        });
    }
    public ViewpointBackend.Extent bind(int eye) throws Throwable {
        owner(); checkEye(eye);
        if(saved==null || restored) throw new IllegalStateException("Output not drawing");
        graphics.bind(targets[eye],width,height); return new ViewpointBackend.Extent(width,height);
    }
    public void copy(int eye) throws Throwable {
        owner(); checkEye(eye);
        if(saved==null || restored || images[eye]!=null || eye==1 && images[0]==null) throw new IllegalStateException("Unexpected eye copy");
        images[eye]=Objects.requireNonNull(graphics.read(targets[eye],width,height));
        if(images[eye].getWidth()!=width || images[eye].getHeight()!=height) throw new IllegalStateException("Readback size mismatch");
    }
    public void publish() throws Throwable {
        owner();
        if(!restored || published || images[0]==null || images[1]==null) throw new IllegalStateException("Incomplete capture pair");
        // Return a world image to the caller for this frame. Ordinary rendering resumes next frame.
        graphics.mirror(targets[0],width,height,saved);
        Files.createDirectories(root);
        String name="capture-"+Instant.now().toString().replace(':','-')+"-"+UUID.randomUUID().toString().substring(0,8);
        Path pending=Files.createDirectory(root.resolve(".pending-"+name));
        write(images[0],pending.resolve("left.png")); write(images[1],pending.resolve("right.png"));
        BufferedImage stereo=new BufferedImage(width*2,height,BufferedImage.TYPE_INT_ARGB);
        var painter=stereo.createGraphics();
        try { painter.drawImage(images[0],0,0,null); painter.drawImage(images[1],width,0,null); } finally { painter.dispose(); }
        write(stereo,pending.resolve("stereo.png"));
        Files.writeString(pending.resolve("capture.txt"),"Project Viewpoint VR 0.5.0\nComplete synthetic stereo pair\nEye size: "+width+"x"+height
                +"\nIPD: 0.064 scene units\nViewpoint SHA-256: "+BinaryPins.VIEWPOINT+"\nGame SHA-256: "+BinaryPins.GAME
                +"\nWorld image only; no UI or headset output.\nDiagnostic effects: TAA/GI/volumetrics/clouds/pack post passes disabled.\n");
        Path destination=root.resolve(name);
        try { Files.move(pending,destination,StandardCopyOption.ATOMIC_MOVE); }
        catch(AtomicMoveNotSupportedException unsupported) { Files.move(pending,destination); }
        result=destination; published=true;
    }
    private static void write(BufferedImage image,Path file) throws Exception {
        if(!ImageIO.write(image,"png",file.toFile())) throw new IllegalStateException("PNG writer unavailable");
    }
    private static void checkEye(int eye) { if(eye<0 || eye>1) throw new IllegalArgumentException("Invalid eye"); }
    public Path result() { return result; }
    public boolean published() { return published; }
    @Override public void close() {
        if(closed) return; owner(); closed=true;
        RuntimeException failure=null;
        for(int i=0;i<2;i++) if(targets[i]!=0) {
            try { graphics.delete(targets[i]); } catch(RuntimeException error) { if(failure==null)failure=error;else failure.addSuppressed(error); }
            targets[i]=0;
        }
        if(failure!=null) throw failure;
    }
}
