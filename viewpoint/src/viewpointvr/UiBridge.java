package viewpointvr;

import java.lang.invoke.MethodHandle;
import java.util.function.Consumer;

/** Native UI producers run exactly once. Queue markers share their SpriteRenderer ordering. */
public final class UiBridge {
    public record Cursor(float x,float y,boolean visible,long time) {}
    private static volatile Cursor cursor;
    public static void cursor(float x,float y,boolean visible){cursor=new Cursor(x,y,visible,System.nanoTime());}
    public static Cursor cursor(){return cursor;}
    public interface Sink { void begin(int layer) throws Throwable; void end(int layer) throws Throwable; void present() throws Throwable; void failed(Throwable error); }
    @FunctionalInterface public interface Action { void run() throws Throwable; }
    private static volatile Sink sink;
    private static final ThreadLocal<Sink[]> pending=ThreadLocal.withInitial(()->new Sink[4]);
    private static volatile Consumer<Runnable> queue;
    public static void queue(Consumer<Runnable> value){queue=value;}
    public static void sink(Sink value){sink=value;}
    private static void guarded(Sink expected,Action action){
        if(sink!=expected)return;
        try{action.run();}catch(Throwable error){sink=null;expected.failed(error);}
    }
    public static void marker(int layer,boolean begin){
        Sink[] markers=pending.get();
        Sink captured=begin?sink:markers[layer];markers[layer]=begin?captured:null;var q=queue;
        if(captured!=null&&q!=null)q.accept(()->guarded(captured,()->{if(begin)captured.begin(layer);else captured.end(layer);}));
    }
    public static void late(MethodHandle original,int layer) throws Throwable {
        marker(layer,true);
        try{original.invokeExact();}finally{marker(layer,false);}
    }
    public static void settings(Object backend,Object data,MethodHandle original) throws Throwable {
        Sink captured=sink;
        if(captured!=null)guarded(captured,()->captured.begin(3));
        try{original.invoke(backend,data);}finally{if(captured!=null)guarded(captured,()->captured.end(3));}
    }
    public static void present(MethodHandle original) throws Throwable {
        try{original.invokeExact();}finally{Sink captured=sink;if(captured!=null)guarded(captured,captured::present);}
    }
}
