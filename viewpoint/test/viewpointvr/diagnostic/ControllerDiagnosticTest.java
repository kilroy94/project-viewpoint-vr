package viewpointvr.diagnostic;
import java.io.*;
import java.nio.file.*;
import viewpointvr.input.*;
public final class ControllerDiagnosticTest {
 static int checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static final ControllerDiagnostic.Targets TARGET=new ControllerDiagnostic.Targets("vanilla",.2,.2,.2,.4,.6,.4,.4,.7,1600,900);
 static String script(boolean widgets)throws Exception{
  var text=new StringWriter();long now=1_000_000_000L;var run=new ControllerDiagnostic.Run(TARGET,now,text,"fixture.txt");var logic=new ControllerLogic();int previous=-1;float x=.2f,y=.2f;
  for(int i=0;i<400;i++){
   now+=100_000_000L;if(!run.tick(now)){run.finish("COMPLETE");return text.toString();}
   var state=run.state(now,16f/9);var out=logic.step(state,now,1,true,false,16f/9);
   if(out.hit()!=null){x=out.hit().x();y=out.hit().y();}
   run.observed(out.down(),x,y);
   check(out.moveX()==0&&out.moveY()==0&&out.turn()==0&&!out.menu(),"script is UI-only");
   if(run.phase!=previous){previous=run.phase;
    if(run.phase==14||run.phase==19)check(!out.down(),"held recovery disarmed through real controller logic");
    if(widgets)switch(run.phase){case 3->run.event("click",1);case 6->run.event("drag",.8);case 7,13,18->run.event("release",.8);case 9->run.event("scroll",35);default->{}}
   }
  }
  throw new AssertionError("script did not terminate");
 }
 public static void main(String[] args)throws Exception{
  String positive=script(true);check(positive.contains("SUMMARY failures=0; script completed"),"observed script reports success");check(positive.contains("PASS widget click changed state exactly once"),"actual widget observation reported separately");
  String missing=script(false);check(missing.contains("FAIL widget click")&&missing.contains("FAIL widget drag changed")&&missing.contains("FAIL widget scroll"),"commands alone cannot claim widget success");check(missing.contains("NEEDS VISUAL CHECK")&&missing.contains("END OF REPORT"),"report caveats and completion marker");
  var abortedText=new StringWriter();var aborted=new ControllerDiagnostic.Run(TARGET,1,abortedText,"aborted.txt");aborted.finish("ABORTED: stopped");check(abortedText.toString().contains("FAIL diagnostic incomplete")&&!abortedText.toString().contains("PASS widget"),"abort is never a passing run");
  var stalled=new ControllerDiagnostic.Run(TARGET,1,new StringWriter(),"stall.txt");try{stalled.tick(2_000_000_000L);throw new AssertionError("stall accepted");}catch(IOException expected){checks++;}
  Path dir=Files.createTempDirectory(Path.of(args[0]),"diagnostic-");ControllerDiagnostic.directory(dir);InputBridge.configure(0);InputBridge.clear();
  long now=System.nanoTime();ControllerDiagnostic.targets(TARGET,now);String start=ControllerDiagnostic.start("vanilla",now);check(ControllerDiagnostic.active()&&start.contains("starting"),"explicit start opens report");check(InputBridge.effectiveMode()==1&&InputBridge.mode==0,"simulated pointer mode preserves activation preference");
  InputBridge.panel(16f/9);ControllerDiagnostic.beforeMouse(now+100_000_000L,true,true,1,false,0,1600,900);check(!InputBridge.sample().fresh(now+100_000_000L),"countdown injects no pointer/buttons");
  ControllerDiagnostic.cancel("test stop");check(!ControllerDiagnostic.active()&&InputBridge.effectiveMode()==0,"stop restores controller Off");
  try(var paths=Files.list(dir)){String report=Files.readString(paths.findFirst().orElseThrow());check(report.contains("END OF REPORT")&&report.contains("ABORTED: test stop"),"cancel flushes report to disk");}
  now=System.nanoTime();ControllerDiagnostic.targets(TARGET,now);ControllerDiagnostic.start("vanilla",now);InputBridge.clear();check(!ControllerDiagnostic.active(),"XR clear cancels simulation");
  now=System.nanoTime();ControllerDiagnostic.targets(TARGET,now);ControllerDiagnostic.start("vanilla",now);InputBridge.panel(16f/9);ControllerDiagnostic.beforeMouse(now,true,true,2,false,0,1600,900);check(!ControllerDiagnostic.active(),"route change cancels");
  now=System.nanoTime();ControllerDiagnostic.targets(TARGET,now);ControllerDiagnostic.start("vanilla",now);ControllerDiagnostic.watchdog(now+2_000_000_000L);check(!ControllerDiagnostic.active(),"missing input heartbeat cancels");
  now=System.nanoTime();ControllerDiagnostic.targets(TARGET,now);ControllerDiagnostic.start("vanilla",now);InputBridge.panel(16f/9);ControllerDiagnostic.beforeMouse(now,false,true,1,false,0,1600,900);check(!ControllerDiagnostic.active(),"window/player focus rejection cancels");
  InputBridge.configure(2);now=System.nanoTime();ControllerDiagnostic.targets(TARGET,now);ControllerDiagnostic.start("vanilla",now);check(!ControllerDiagnostic.active(),"cannot arm over normal controller mode");InputBridge.configure(0);
  now=System.nanoTime();ControllerDiagnostic.targets(TARGET,now);ControllerDiagnostic.start("vanilla",now);ControllerDiagnostic.targets(new ControllerDiagnostic.Targets("vanilla",.3,.2,.2,.4,.6,.4,.4,.7,1600,900),now);InputBridge.panel(16f/9);ControllerDiagnostic.beforeMouse(now,true,true,1,false,0,1600,900);check(!ControllerDiagnostic.active(),"target relocation cancels");
  now=System.nanoTime();ControllerDiagnostic.targets(TARGET,now);ControllerDiagnostic.start("vanilla",now);InputBridge.panel(16f/9);
  for(int i=1;i<=31;i++){long time=now+i*100_000_000L;ControllerDiagnostic.targets(TARGET,time);ControllerDiagnostic.beforeMouse(time,true,true,1,false,0,1600,900);}
  check(ControllerDiagnostic.active(),"neutral countdown arms without sleep");ControllerDiagnostic.beforeMouse(now+3_200_000_000L,true,true,1,true,0,1600,900);check(!ControllerDiagnostic.active(),"physical button after countdown aborts");
  InputBridge.clear();System.out.println("Controller diagnostic: "+checks+" checks passed; no real UI/runtime executed");
 }
}
