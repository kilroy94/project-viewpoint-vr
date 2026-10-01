package viewpointvr;
import java.lang.invoke.*;
import java.util.*;
public final class UiBridgeTest {
    static int checks,calls;static boolean fail;
    static void original(){calls++;if(fail)throw new IllegalStateException("native failure");}
    public static final class Backend {public void draw(Object data){original();}}
    static final class Sink implements UiBridge.Sink {
        final List<String> events=new ArrayList<>();boolean broken;
        public void begin(int n){events.add("begin"+n);if(broken)throw new IllegalStateException("capture failure");}
        public void end(int n){events.add("end"+n);}
        public void present(){events.add("present");}
        public void failed(Throwable error){events.add("failed");}
    }
    static void check(boolean b){checks++;if(!b)throw new AssertionError("UI bridge "+checks);}
    public static void main(String[] args)throws Throwable{
        var handle=MethodHandles.lookup().findStatic(UiBridgeTest.class,"original",MethodType.methodType(void.class));
        var queue=new ArrayList<Runnable>();UiBridge.queue(queue::add);
        UiBridge.late(handle,1);check(calls==1&&queue.isEmpty());
        var sink=new Sink();UiBridge.sink(sink);UiBridge.late(handle,1);check(calls==2&&queue.size()==2&&sink.events.isEmpty());
        queue.forEach(Runnable::run);queue.clear();check(sink.events.equals(List.of("begin1","end1")));
        fail=true;try{UiBridge.late(handle,2);throw new AssertionError();}catch(IllegalStateException expected){check(calls==3);}
        queue.forEach(Runnable::run);queue.clear();check(sink.events.contains("end2"));fail=false;
        UiBridge.marker(0,true);UiBridge.sink(null);UiBridge.marker(0,false);queue.forEach(Runnable::run);queue.clear();check(sink.events.size()==4);
        UiBridge.sink(sink);UiBridge.present(handle);check(calls==4&&sink.events.get(4).equals("present"));
        sink.broken=true;UiBridge.late(handle,1);queue.forEach(Runnable::run);queue.clear();check(calls==5&&sink.events.get(6).equals("failed"));
        UiBridge.late(handle,1);check(calls==6&&queue.isEmpty());
        var fresh=new Sink();UiBridge.sink(fresh);
        var draw=MethodHandles.lookup().findVirtual(Backend.class,"draw",MethodType.methodType(void.class,Object.class));
        UiBridge.settings(new Backend(),new Object(),draw);check(calls==7&&fresh.events.equals(List.of("begin3","end3")));
        fail=true;try{UiBridge.settings(new Backend(),new Object(),draw);throw new AssertionError();}catch(IllegalStateException expected){check(calls==8&&fresh.events.size()==4);}fail=false;
        fresh.broken=true;UiBridge.settings(new Backend(),new Object(),draw);check(calls==9&&fresh.events.get(5).equals("failed"));
        UiBridge.sink(null);UiBridge.queue(null);System.out.println("UI bridge: "+checks+" checks passed");
    }
}
