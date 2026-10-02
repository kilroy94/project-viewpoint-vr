package viewpointvr.input;
import viewpointvr.xr.XrCamera.Pose;
import java.lang.invoke.*;
/** Simulated device sequences, no runtime or game entry points. */
public final class ControllerTest {
 static int checks;static long now=1_000_000_000L;static String calls="";
 static final Pose CENTER=new Pose(0,0,0,0,0,0,1);
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static void near(float a,float b,String s){check(Math.abs(a-b)<.0001f,s+" "+a);}
 static ControllerState.Hand hand(float x,float y,float trigger,boolean menu){return new ControllerState.Hand(CENTER,CENTER,x,y,trigger,menu);}
 static ControllerState state(ControllerState.Hand left,ControllerState.Hand right){return new ControllerState(1,now,true,left,right);}
 static ControllerLogic.Output step(ControllerLogic logic,ControllerState.Hand left,ControllerState.Hand right,int mode,boolean released){return logic.step(state(left,right),now,mode,released,true,16f/9);}
 public static void original(){calls+="O";}
 public static void originalMove(Object p,Object v){calls+="M";}
 public static void throwing(){calls+="E";throw new IllegalStateException("expected native failure");}
 public static void main(String[] args)throws Throwable{
  var center=PanelRay.hit(CENTER,16f/9);near(center.x(),.5f,"center x");near(center.y(),.5f,"center y");near(center.end().z,-1.5f,"quad depth");
  var corner=PanelRay.hit(new Pose(1,.5625f,0,0,0,0,1),16f/9);near(corner.x(),1,"right edge");near(corner.y(),0,"top edge");
  near(PanelRay.hit(new Pose(.325f,0,0,0,0,0,1),.5f).x(),1,"portrait width cap");
  check(PanelRay.hit(new Pose(1.01f,0,0,0,0,0,1),16f/9)==null,"outside panel");
  check(PanelRay.hit(new Pose(0,0,0,0,1,0,0),1)==null,"backward aim");
  check(PanelRay.hit(new Pose(0,0,0,0,.70710677f,0,.70710677f),1)==null,"parallel aim");
  check(PanelRay.hit(new Pose(0,0,-2,0,0,0,1),1)==null,"plane behind controller");
  check(PanelRay.hit(new Pose(0,0,5,0,0,0,1),1)==null,"maximum reach");
  check(PanelRay.hit(null,1)==null&&PanelRay.hit(CENTER,Float.NaN)==null,"invalid ray inputs");
  var bad=hand(Float.NaN,0,0,false);check(!bad.tracked()&&bad.x()==0,"invalid axes discard sample");
  check(hand(4,-4,5,false).x()==1&&hand(4,-4,5,false).y()==-1&&hand(0,0,5,false).trigger()==1,"axis bounds");
  var neutral=hand(0,0,0,false);var trigger=hand(0,0,1,false);var missing=ControllerState.NONE;
  var logic=new ControllerLogic();
  check(!step(logic,neutral,trigger,1,true).down(),"held at enable blocked");
  check(!step(logic,neutral,neutral,1,true).down(),"neutral arms");
  check(step(logic,neutral,trigger,1,true).down(),"trigger press");
  check(step(logic,neutral,hand(0,0,.5f,false),1,true).down(),"hysteresis hold");
  check(!step(logic,neutral,hand(0,0,.1f,false),1,true).down(),"trigger release");
  check(step(logic,neutral,trigger,1,true).down(),"second press");
  check(!step(logic,neutral,missing,1,true).down(),"tracking loss releases");
  check(!step(logic,neutral,trigger,1,true).down(),"tracking return held blocked");
  step(logic,neutral,neutral,1,true);step(logic,neutral,trigger,1,true);
  var away=new ControllerState.Hand(new Pose(4,0,0,0,0,0,1),CENTER,0,0,1,false);
  check(!step(logic,neutral,away,1,true).down(),"ray miss releases");
  check(!step(logic,neutral,trigger,1,true).down(),"reenter while held blocked");
  step(logic,neutral,neutral,1,true);step(logic,neutral,trigger,1,true);
  check(!logic.step(new ControllerState(1,now,false,neutral,trigger),now,1,true,true,1).down(),"focus loss releases");
  check(!step(logic,neutral,trigger,1,true).down(),"focus return requires neutral");
  step(logic,neutral,neutral,1,true);
  check(!logic.step(new ControllerState(1,now-250_000_000L,true,neutral,trigger),now,1,true,true,1).down(),"stale sample releases");
  check(!step(logic,neutral,trigger,1,true).down(),"stale return requires neutral");
  step(logic,neutral,neutral,1,true);check(!step(logic,neutral,trigger,2,true).down(),"mode change rearms");
  step(logic,neutral,neutral,2,true);check(!step(logic,neutral,trigger,2,false).down(),"captured mouse prevents click");
  check(step(logic,neutral,trigger,0,true).equals(ControllerLogic.Output.NONE),"Off has no effects");
  logic=new ControllerLogic();var up=hand(0,1,0,false);
  check(step(logic,neutral,up,1,true).scroll()==0,"scroll held on enable blocked");
  step(logic,neutral,neutral,1,true);check(step(logic,neutral,up,1,true).scroll()==1,"scroll starts");
  now+=100_000_000L;check(step(logic,neutral,up,1,true).scroll()==0,"repeat throttled");
  now+=80_000_000L;check(step(logic,neutral,up,1,true).scroll()==1,"repeat interval");
  step(logic,neutral,missing,1,true);now+=180_000_000L;check(step(logic,neutral,up,1,true).scroll()==0,"scroll tracking loss rearms");
  step(logic,neutral,neutral,1,true);check(step(logic,neutral,hand(0,-1,0,false),1,true).scroll()==-1,"reverse scroll");
  logic=new ControllerLogic();var forward=hand(0,1,0,false);var turn=hand(1,0,0,false);
  check(step(logic,forward,turn,2,false).moveY()==0,"held walk blocked");
  step(logic,neutral,neutral,2,false);
  var move=step(logic,forward,turn,2,false);near(move.moveY(),1,"full forward");near(move.moveX(),0,"forward x");near(move.turn(),(float)Math.PI/6,"30 degree turn");
  check(step(logic,forward,turn,2,false).turn()==0,"held turn fires once");
  step(logic,neutral,neutral,2,false);near(step(logic,hand(1,1,0,false),hand(-1,0,0,false),2,false).turn(),-(float)Math.PI/6,"left turn");
  move=step(logic,hand(1,1,0,false),neutral,2,false);near((float)Math.hypot(move.moveX(),move.moveY()),1,"diagonal speed capped");
  near(step(logic,hand(.1f,0,0,false),neutral,2,false).moveX(),0,"walk deadzone");
  near(step(logic,hand(.6f,0,0,false),neutral,2,false).moveX(),.5f,"walk analog range");
  step(logic,missing,neutral,2,false);check(step(logic,forward,neutral,2,false).moveY()==0,"left tracking return blocked");
  step(logic,neutral,neutral,2,false);check(step(logic,forward,neutral,1,false).moveY()==0,"pointer mode never walks");
  step(logic,neutral,neutral,2,false);check(step(logic,forward,turn,2,true).moveY()==0,"UI mode never walks");
  check(logic.step(state(forward,turn),now,2,false,false,1).turn()==0,"gameplay disabled prevents turn");
  logic=new ControllerLogic();var menu=hand(0,0,0,true);
  check(!step(logic,neutral,menu,1,false).menu(),"held menu blocked");step(logic,neutral,neutral,1,false);
  check(step(logic,neutral,menu,1,false).menu(),"menu edge");check(!step(logic,neutral,menu,1,false).menu(),"menu held once");
  var merge=new MouseMerge();var b=merge.step(false,false,true,1);check(b.down()&&!b.previous(),"virtual press edge");
  b=merge.step(false,false,true,1);check(b.down()&&b.previous(),"virtual hold");
  b=merge.step(false,false,false,1);check(!b.down()&&b.previous(),"virtual release on disable/loss");
  b=merge.step(false,false,false,1);check(!b.down()&&!b.previous(),"release completes");
  merge.step(true,false,true,1);b=merge.step(true,true,false,1);check(b.down()&&b.previous(),"physical hold survives virtual release");
  merge=new MouseMerge();merge.step(false,false,true,1);b=merge.step(false,true,false,2);check(!b.down()&&!b.previous(),"route change removes virtual history");
  b=merge.step(true,false,false,2);check(b.down()&&!b.previous(),"physical press preserved");
  b=merge.step(false,true,false,2);check(!b.down()&&b.previous(),"physical release preserved");
  var lookup=MethodHandles.lookup();var original=lookup.findStatic(ControllerTest.class,"original",MethodType.methodType(void.class));
  var moveHandle=lookup.findStatic(ControllerTest.class,"originalMove",MethodType.methodType(void.class,Object.class,Object.class));
  InputBridge.install(new InputBridge.Native(){public void mouse(){calls+="i";}public void move(Object p,Object v){calls+="v";}public void imgui(){calls+="u";}});
  calls="";InputBridge.mouse(original);check(calls.equals("Oi"),"mouse original before overlay exactly once");
  calls="";InputBridge.move(null,null,moveHandle);check(calls.equals("vM"),"movement overlay before original exactly once");
  calls="";InputBridge.imgui(original);check(calls.equals("Ou"),"ImGui physical input before overlay exactly once");
  calls="";try{InputBridge.mouse(lookup.findStatic(ControllerTest.class,"throwing",MethodType.methodType(void.class)));throw new AssertionError();}catch(IllegalStateException expected){check(calls.equals("E"),"original failure propagates without replay");}
  InputBridge.configure(2);InputBridge.install(new InputBridge.Native(){public void mouse(){throw new IllegalStateException("simulated adapter failure");}public void move(Object p,Object v){throw new IllegalStateException("simulated move failure");}public void imgui(){throw new IllegalStateException("simulated UI failure");}});
  calls="";InputBridge.mouse(original);check(InputBridge.mode==0&&calls.equals("O"),"mouse adapter failure disables input, preserves original");
  calls="";InputBridge.move(null,null,moveHandle);check(calls.equals("M"),"move adapter failure still runs original once");
  calls="";InputBridge.imgui(original);check(calls.equals("O"),"UI adapter failure retains native input");
  InputBridge.configure(1);InputBridge.state=new ControllerState(1,System.nanoTime(),true,neutral,neutral);InputBridge.output=new ControllerLogic.Output(center,false,0,0,0,0,false);
  check(InputBridge.ray()==null,"no pointer before panel available");InputBridge.panel(16f/9);check(InputBridge.ray()!=null,"pointer on published panel");
  long epoch=InputBridge.generation();InputBridge.clear();check(InputBridge.generation()>epoch&&!InputBridge.panelReady()&&InputBridge.ray()==null,"clear invalidates epoch and panel");
  InputBridge.install(null);InputBridge.configure(0);
  System.out.println("Controller simulation: "+checks+" checks passed");
 }
}
