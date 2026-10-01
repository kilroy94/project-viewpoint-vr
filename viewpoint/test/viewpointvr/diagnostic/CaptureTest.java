package viewpointvr.diagnostic;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;
import viewpointvr.*;

public final class CaptureTest {
    static int checks;
    static void check(boolean condition,String message) { checks++;if(!condition)throw new AssertionError(message); }
    static void rejects(FrameBoundary.NativeDraw action) throws Throwable {
        try { action.draw(); } catch(Exception expected) { checks++;return; } throw new AssertionError("Expected rejection");
    }
    static final class Graphics implements CaptureOutput.Graphics {
        final List<String> events=new ArrayList<>(); int next; String fail="";
        void event(String value) { events.add(value);if(fail.equals(value))throw new IllegalStateException(value); }
        public ViewpointBackend.SavedOutput save() { event("save"); return new ViewpointBackend.SavedOutput(9,8,1,2,new ViewpointBackend.Extent(8,4),true,()->event("restore")); }
        public int create(int w,int h) { event("create"+(next+1));return ++next; }
        public void delete(int target) { event("delete"+target); }
        public void bind(int t,int w,int h) { event("bind"+t); }
        public BufferedImage read(int t,int w,int h) { event("read"+t);var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,t==1?0xffff0000:0xff0000ff);return image; }
        public void mirror(int t,int w,int h,ViewpointBackend.SavedOutput destination) { event("mirror"); }
    }
    public static void main(String[] args) throws Throwable {
        Set<Integer> keys=new HashSet<>();
        check(!CaptureController.captureChord(true,keys::contains),"no keys");
        keys.add(197);
        check(!CaptureController.captureChord(true,keys::contains),"Pause alone rejected");
        keys.add(42);
        check(CaptureController.captureChord(true,keys::contains),"left Shift+Pause");
        check(!CaptureController.captureChord(false,keys::contains),"unfocused rejected");
        keys.remove(42);keys.add(54);
        check(CaptureController.captureChord(true,keys::contains),"right Shift+Pause");
        for(int extra:new int[]{29,157,56,184,219,220}) {
            keys.add(extra);check(!CaptureController.captureChord(true,keys::contains),"extra modifier rejected: "+extra);keys.remove(extra);
        }
        keys.clear();keys.addAll(Set.of(68,29,42));
        check(!CaptureController.captureChord(true,keys::contains),"old Ctrl+Shift+F10 rejected");
        Path root=Files.createTempDirectory(Path.of(args[0]),"capture-test-");
        Graphics good=new Graphics(); Path result;
        try(var capture=new CaptureOutput(good,root)) {
            var state=capture.save();capture.bind(0);capture.copy(0);capture.bind(1);capture.copy(1);
            rejects(capture::publish); state.restore().restore();capture.publish();
            check(capture.published(),"Complete publication");result=capture.result();rejects(capture::publish);
        }
        check(good.events.equals(List.of("save","create1","create2","bind1","read1","bind2","read2","restore","mirror","delete1","delete2")),"Graphics lifetime ordering");
        var stereo=ImageIO.read(result.resolve("stereo.png").toFile());
        check(stereo.getWidth()==16 && stereo.getRGB(0,0)==0xffff0000 && stereo.getRGB(8,0)==0xff0000ff,"Eye identity in combined PNG");
        check(Files.isRegularFile(result.resolve("capture.txt")),"Completion metadata");
        for(String fail:List.of("create2","read1","read2","mirror")) {
            Graphics broken=new Graphics();broken.fail=fail;
            try(var capture=new CaptureOutput(broken,root)) {
                rejects(()->{
                    var saved=capture.save();
                    try { capture.bind(0);capture.copy(0);capture.bind(1);capture.copy(1); }
                    finally { saved.restore().restore(); }
                    capture.publish();
                });
                check(!capture.published(),"Failure never publishes");
            }
            check(broken.events.contains("restore") && broken.events.contains("delete1"),"Partial acquisition/failure restores and deletes resources");
        }
        AtomicLong clock=new AtomicLong(1);AtomicBoolean ready=new AtomicBoolean(true),key=new AtomicBoolean();AtomicInteger frames=new AtomicInteger(),ordinary=new AtomicInteger();
        var controller=new CaptureController(ready::get,key::get,clock::get,(d,o)->{frames.incrementAndGet();return "saved";});
        controller.render(new Object(),ordinary::incrementAndGet);check(ordinary.get()==1,"No unsolicited capture");
        controller.request();controller.request();controller.render(new Object(),ordinary::incrementAndGet);check(frames.get()==1,"Requests coalesced");
        key.set(true);controller.render(new Object(),ordinary::incrementAndGet);controller.render(new Object(),ordinary::incrementAndGet);check(frames.get()==2,"Held key captures once");
        key.set(false);controller.render(new Object(),ordinary::incrementAndGet);key.set(true);controller.render(new Object(),ordinary::incrementAndGet);check(frames.get()==3,"Key release rearms");
        key.set(false);controller.request();clock.addAndGet(11_000_000_000L);controller.render(new Object(),ordinary::incrementAndGet);check(frames.get()==3,"Expired request not captured");
        ready.set(false);controller.request();controller.render(new Object(),ordinary::incrementAndGet);check(frames.get()==3,"Unready install never captures");
        var failed=new CaptureController(()->true,()->false,clock::get,(d,o)->{throw new IllegalStateException("injected render failure");});
        failed.request();rejects(()->failed.render(new Object(),ordinary::incrementAndGet));check(failed.request().contains("disabled"),"Failure disarms future requests");
        System.out.println("Capture/controller: "+checks+" checks passed");
    }
}
