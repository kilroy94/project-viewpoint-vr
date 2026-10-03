package viewpointvr.diagnostic;
import java.util.concurrent.atomic.*;
import viewpointvr.*;
public final class RuntimeDriverTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static class Pipeline implements RuntimeDriver.Pipeline {
        int close,desktop,xr,idle;boolean allowed=true,recenter,tracked;Throwable error;
        public boolean eligible(Object d){return allowed;}
        public boolean desktop(Object d,FrameBoundary.NativeDraw n)throws Throwable{desktop++;if(error!=null)throw error;return true;}
        public boolean xr(Object d,FrameBoundary.NativeDraw n,boolean t,boolean r)throws Throwable{xr++;tracked=t;recenter=r;if(error!=null)throw error;return true;}
        public void idle(){idle++;}public void close(){close++;}public void scale(float f){}
    }
    public static void main(String[] args)throws Throwable {
        var pipeline=new Pipeline();var now=new AtomicLong(1);var stop=new AtomicBoolean();var ready=new AtomicBoolean(true);var nativeDraws=new AtomicInteger();
        var capture=new CaptureController(ready::get,()->false,now::get,(d,n)->"capture");
        var driver=new RuntimeDriver(pipeline,capture,ready::get,stop::get,now::get);
        driver.render(null,()->nativeDraws.incrementAndGet());check(nativeDraws.get()==1,"Off delegates");
        driver.mode("DESKTOP");for(int i=0;i<6;i++)driver.render(null,()->nativeDraws.incrementAndGet());
        check(nativeDraws.get()==5&&pipeline.desktop==2,"warm snapshots then continuous desktop");check(Visibility.active(),"conservative visibility active");
        driver.mode("XR_FIXED");for(int i=0;i<5;i++)driver.render(null,()->nativeDraws.incrementAndGet());check(pipeline.xr==1&&!pipeline.tracked,"fixed XR");
        driver.mode("XR_TRACKED");for(int i=0;i<5;i++)driver.render(null,()->nativeDraws.incrementAndGet());check(pipeline.tracked,"tracked XR");
        driver.recenter();driver.render(null,()->nativeDraws.incrementAndGet());check(!pipeline.recenter,"countdown does not fire early");now.addAndGet(5_000_000_000L);driver.render(null,()->nativeDraws.incrementAndGet());check(pipeline.recenter,"countdown fires");driver.render(null,()->nativeDraws.incrementAndGet());check(!pipeline.recenter,"recenter once");
        pipeline.allowed=false;int before=nativeDraws.get();driver.render(null,()->nativeDraws.incrementAndGet());check(nativeDraws.get()==before+1&&pipeline.idle==1&&!Visibility.active(),"unsupported scene empty XR and native draw");
        driver.idleTick();driver.idleTick();check(pipeline.idle==2,"no world still pumps session");pipeline.allowed=true;
        stop.set(true);driver.render(null,()->nativeDraws.incrementAndGet());check(!Visibility.active(),"emergency off restores visibility");stop.set(false);
        driver.mode("XR_TRACKED");pipeline.error=new RuntimeDriver.Unavailable(new IllegalStateException("missing runtime"));before=nativeDraws.get();for(int i=0;i<5;i++)driver.render(null,()->nativeDraws.incrementAndGet());check(nativeDraws.get()==before+5,"missing runtime falls back without losing scene");
        pipeline.error=new IllegalStateException("render failure");driver.mode("DESKTOP");for(int i=0;i<4;i++)driver.render(null,()->nativeDraws.incrementAndGet());before=nativeDraws.get();
        try{driver.render(null,()->nativeDraws.incrementAndGet());throw new AssertionError("failure expected");}catch(IllegalStateException expected){}
        check(nativeDraws.get()==before,"partial rendering never replays original");check(driver.mode("DESKTOP").contains("Disabled"),"failure disarms");
        driver.mode("OFF");var saved=new java.util.concurrent.atomic.AtomicInteger();
        driver.scale(1.5,value->{check(value==1.5,"accepted scale callback value");saved.incrementAndGet();});
        driver.scale(Double.NaN,value->saved.incrementAndGet());driver.scale(8,value->saved.incrementAndGet());
        check(saved.get()==1,"invalid scale is not persisted");
        Visibility.off();check(!Visibility.visible(false)&&Visibility.hidden(true),"inactive visibility preserved");Visibility.touch();check(Visibility.visible(false)&&!Visibility.hidden(true),"active visibility broadens");Visibility.off();
        System.out.println("Runtime modes: "+checks+" checks passed");
    }
}
