package viewpointvr.melee;

/** Physical-space translational swing recognizer. No game or runtime dependencies. */
public final class SwingDetector {
    public record Sample(long time,float x,float y,float z,float hx,float hy,float hz,boolean held,boolean valid) {}
    private Sample previous;
    private boolean released,spent;
    private float travel;
    public void reset() { previous=null; released=false; spent=false; travel=0; }
    public boolean sample(Sample s) {
        if(!s.valid() || !finite(s)) { reset(); return false; }
        if(!s.held()) { previous=s; released=true; spent=false; travel=0; return false; }
        if(!released || spent) { previous=s; return false; }
        Sample p=previous; previous=s;
        if(p==null) return false;
        double dt=(s.time()-p.time())*1e-9;
        if(dt<=0 || dt>.12) { reset(); return false; }
        float dx=s.x()-p.x(),dy=s.y()-p.y(),dz=s.z()-p.z();
        float distance=length(dx,dy,dz);
        float relative=length(dx-(s.hx()-p.hx()),dy-(s.hy()-p.hy()),dz-(s.hz()-p.hz()));
        double speed=distance/dt, relativeSpeed=relative/dt;
        // Both must move: leaning the head alone and translating the whole body do not count.
        if(distance>.35f || speed>12) { reset(); return false; }
        if(speed<.4 || relativeSpeed<.4) { travel=0; return false; }
        travel+=Math.min(distance,relative);
        if(speed>=1.2 && relativeSpeed>=1.2 && travel>=.12f) {
            spent=true; travel=0; return true; // Release trigger before another swing.
        }
        return false;
    }
    private static float length(float x,float y,float z) { return (float)Math.sqrt(x*x+y*y+z*z); }
    private static boolean finite(Sample s) { return Float.isFinite(s.x()+s.y()+s.z()+s.hx()+s.hy()+s.hz()); }
}
