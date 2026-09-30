package pzvr.interaction;

/** Release-to-arm, hysteresis and freshness. A held grip never repeats or queues a use. */
public final class PressGate {
    private boolean armed,held;
    private long last;
    public void reset(){armed=false;held=false;last=0;}
    public boolean update(long now,float value,boolean valid){
        if(!valid||!Float.isFinite(value)||now<=0){reset();return false;}
        if(last==0||now<last||now-last>150_000_000L){armed=false;held=false;}
        last=now;
        if(value<.45f){armed=true;held=false;return false;}
        if(value<.65f||held)return false;
        held=true;boolean fire=armed;armed=false;return fire;
    }
}
