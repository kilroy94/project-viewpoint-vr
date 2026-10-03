package viewpointvr.diagnostic;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import viewpointvr.input.*;
import viewpointvr.xr.XrCamera.Pose;
/** Explicitly armed UI-only simulator. Production never arms it automatically. */
public final class ControllerDiagnostic {
 public record Targets(String kind,double cx,double cy,double dx,double dy,double ex,double ey,double sx,double sy,int width,int height){
  public Targets{if(!Set.of("vanilla","viewpoint").contains(kind)||width<1||height<1)throw new IllegalArgumentException("Invalid diagnostic surface");for(double v:new double[]{cx,cy,dx,dy,ex,ey,sx,sy})if(!Double.isFinite(v)||v<0||v>1)throw new IllegalArgumentException("Target outside screen");}
 }
 private record Surface(Targets target,long time){}
 private static final Map<String,Surface> surfaces=new HashMap<>();
 private static Path directory;
 private static Run run;
 private static ControllerState simulated=ControllerState.EMPTY;
 private static String status="Diagnostic idle";
 public static synchronized void directory(Path path){directory=path;}
 public static synchronized boolean active(){return run!=null;}
 public static synchronized String status(){return status;}
 public static synchronized ControllerState sample(ControllerState real){return run==null?real:simulated;}
 public static synchronized void targets(Targets value,long now){surfaces.put(value.kind(),new Surface(value,now));}
 public static synchronized String start(String kind,long now){
  if(run!=null)return status;
  Surface surface=surfaces.get(kind);
  if(directory==null)return status="Diagnostic unavailable: initialization incomplete";
  if(InputBridge.mode!=0)return status="Set Controllers Off before starting a diagnostic";
  if(surface==null||now-surface.time()>500_000_000L)return status="Open the diagnostic test surface first";
  try{Files.createDirectories(directory);Path file=directory.resolve("controller-"+kind+"-"+Instant.now().toString().replace(':','-')+"-"+UUID.randomUUID().toString().substring(0,8)+".txt");
   run=new Run(surface.target(),now,Files.newBufferedWriter(file,StandardOpenOption.CREATE_NEW),file.getFileName().toString());
   simulated=ControllerState.EMPTY;InputBridge.trackingLost();return status="Diagnostic starting: "+file.getFileName();
  }catch(IOException error){return status="Diagnostic report could not be created: "+error;}
 }
 public static synchronized void cancel(String reason){if(run!=null)finish("ABORTED: "+reason);}
 private static void finish(String reason){
  Run done=run;if(done==null)return;run=null;simulated=ControllerState.EMPTY;InputBridge.trackingLost();
  try{done.finish(reason);status=reason+(done.failures==0?" - logged checks passed; visual checks pending":" - FAIL checks="+done.failures)+" | report: "+done.name;}catch(IOException error){status="Report write failed: "+error;}
 }
 public static synchronized void watchdog(long now){if(run!=null&&now-run.lastTick>1_000_000_000L)finish("ABORTED: no native input ticks for one second");}
 public static synchronized void beforeMouse(long now,boolean eligible,boolean released,int route,boolean physical,int wheel,int width,int height){
  if(run==null)return;
  Surface live=surfaces.get(run.target.kind());
  if(!eligible||!released||!InputBridge.panelReady()){finish("ABORTED: window/cursor/player or XR panel unavailable");return;}
  if(live==null||now-live.time()>500_000_000L||!live.target().equals(run.target)){finish("ABORTED: target closed, moved or resized");return;}
  if(run.target.kind().equals("vanilla")&&(width!=run.target.width()||height!=run.target.height())){finish("ABORTED: game screen resized");return;}
  if(route!=(run.target.kind().equals("vanilla")?1:2)){finish("ABORTED: UI route changed");return;}
  if(InputBridge.mode!=0||InputBridge.state.left().tracked()||InputBridge.state.right().tracked()){finish("ABORTED: real controllers or another input mode active");return;}
  if(run.phase>0&&(physical||wheel!=0)){finish("ABORTED: physical mouse input during script");return;}
  try{
   if(!run.tick(now)){finish("COMPLETE");return;}
   simulated=run.state(now,InputBridge.aspect);
   status="Diagnostic "+run.target.kind()+" phase "+run.phase+"/21: "+run.label();
  }catch(IOException error){finish("ABORTED: report write failed");}
 }
 public static synchronized void observed(String route,boolean down,float x,float y){if(run==null||!run.target.kind().equals(route))return;try{run.observed(down,x,y);}catch(IOException error){finish("ABORTED: report write failed");}}
 public static synchronized void event(String kind,String event,double value){if(run==null||!run.target.kind().equals(kind))return;try{run.event(event,value);}catch(IOException error){finish("ABORTED: report write failed");}}
 /** Deterministic script/report engine; tests supply a monotonic clock and synthetic observations. */
 public static final class Run {
  final Targets target;final BufferedWriter writer;final String name;long phaseAt,lastTick;int phase;boolean matched;int clicks,drags,scrolls;boolean release,lossRelease,focusRelease;int failures;
  public Run(Targets target,long now,Writer writer,String name)throws IOException{this.target=target;this.phaseAt=this.lastTick=now;this.writer=new BufferedWriter(writer);this.name=name;line("Project Viewpoint VR 0.8.0 controller diagnostic | "+target.kind());line("INFO simulated poses, not physical OpenXR devices; input observations are separate from widget results");line("INFO started "+Instant.now());line("INFO targets "+target);line("INFO countdown: release mouse; keep test surface visible and stationary");}
  String label(){return switch(phase){case 0->"countdown";case 1,4,8,10,15,20,21->"neutral/rearm";case 2->"click press";case 3->"click release";case 5,11,16->"drag press";case 6,12,17->"drag move";case 7->"drag release";case 9->"scroll down";case 13->"tracking interruption";case 14,19->"return with trigger held (must stay released)";case 18->"simulated focus interruption";default->"done";};}
  boolean expected(){return Set.of(2,5,6,11,12,16,17).contains(phase);}
  public boolean tick(long now)throws IOException{
   if(now<lastTick||now-lastTick>1_000_000_000L)throw new IOException("Input clock stalled");lastTick=now;
   if(now-phaseAt>=(phase==0?3_000_000_000L:1_000_000_000L)){
    if(phase>0)result(matched,"adapter phase "+phase+" "+label());
    phase++;phaseAt=now;matched=false;if(phase>21)return false;
    line("INFO INPUT phase "+phase+" "+label()+" expectedDown="+expected());
   }
   return true;
  }
  double x(){return switch(phase){case 0,1,2,3->target.cx();case 8,9->target.sx();case 6,7,12,13,14,17,18,19->target.ex();default->target.dx();};}
  double y(){return switch(phase){case 0,1,2,3->target.cy();case 8,9->target.sy();case 6,7,12,13,14,17,18,19->target.ey();default->target.dy();};}
  public ControllerState state(long now,float aspect){
   if(phase==0)return ControllerState.EMPTY;
   boolean lost=phase==13||phase==18;float width=Math.min(2,1.3f*aspect),height=width/aspect;
   var pose=new Pose((float)(x()-.5)*width,(float)(.5-y())*height,0,0,0,0,1);
   var hand=lost?ControllerState.NONE:new ControllerState.Hand(pose,pose,0,phase==9?-1:0,expected()||phase==14||phase==19?1:0,false);
   return new ControllerState(phase,now,phase!=18,ControllerState.NONE,hand);
  }
  public void observed(boolean down,float x,float y)throws IOException{
   boolean point=phase==13||phase==18||phase==14||phase==19||Math.abs(x-x())<.015&&Math.abs(y-y())<.015;
   if(!matched&&down==expected()&&point){matched=true;line("INFO OBSERVED phase="+phase+" down="+down+" x="+x+" y="+y);}
  }
  public void event(String event,double value)throws IOException{
   if(!Double.isFinite(value)||!Set.of("click","drag","release","scroll").contains(event))return;
   if(phase==0)return;line("INFO WIDGET phase="+phase+" event="+event+" value="+value);
   switch(event){case "click"->clicks++;case "drag"->{if(Set.of(6,12,17).contains(phase))drags++;}case "scroll"->scrolls++;case "release"->{if(phase>=7&&phase<=8)release=true;if(phase>=13&&phase<=15)lossRelease=true;if(phase>=18&&phase<=20)focusRelease=true;}default->{}}
  }
  private void result(boolean passed,String label)throws IOException{if(!passed)failures++;line((passed?"PASS ":"FAIL ")+label);}
  public void finish(String reason)throws IOException{
   try{line("INFO "+reason);if(reason.equals("COMPLETE")){result(clicks==1,"widget click changed state exactly once (count="+clicks+")");result(drags>0,"widget drag changed value");result(scrolls>0,"widget scroll changed offset");result(release,"widget ordinary drag deactivated");result(lossRelease,"widget drag deactivated after tracking interruption");result(focusRelease,"widget drag deactivated after simulated focus interruption");}
    else{failures++;line("FAIL diagnostic incomplete; rerun after resolving abort");}
    line("NEEDS VISUAL CHECK pointer/beam placement, text readability, flicker, panel appearance");line("NEEDS VISUAL CHECK actual inventory operations and physical-controller bindings/comfort (not tested)");line("SUMMARY failures="+failures+"; "+(reason.equals("COMPLETE")?"script completed":"script aborted"));line("END OF REPORT");
   }finally{writer.close();}
  }
  private void line(String text)throws IOException{writer.write(text);writer.newLine();writer.flush();}
 }
}
