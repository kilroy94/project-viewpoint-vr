import pzvr.interaction.*;import pzvr.input.*;import pzvr.xr.*;import org.joml.Matrix4f;
import com.pavelvoronin.pz3d.*;import zombie.iso.*;import zombie.characters.IsoPlayer;
public class HandUseTest {
 static int checks;static void ok(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static final XrCamera.Pose head=new XrCamera.Pose(0,0,1,0,0,0,1);
 static XrCamera.Pose hand=new XrCamera.Pose(.6f,0,1,0,0,0,1);
 public static class Frame {public Object scene=Main.jr.scene();}
 static void tick(float grip,boolean valid){
  var h=new VrControllerState.Hand("touch",true,0,0,0,grip,false,false,false,false);
  ControllerBridge.publish(new VrControllerState(1,System.nanoTime(),true,h,h));
  HandUse.capture(new Frame(),new Matrix4f(),head,new HandPoses(hand,hand),valid);Main.tick();
 }
 static void press(){tick(0,true);tick(1,true);}
 public static void main(String[] args)throws Exception{
  var gate=new PressGate();long t=1_000_000_000L;
  ok(!gate.update(t,1,true),"held at startup");ok(!gate.update(t+=10,0,true),"release arms");
  ok(gate.update(t+=10,.7f,true),"press");ok(!gate.update(t+=10,1,true),"hold does not repeat");
  ok(!gate.update(t+=10,.5f,true),"hysteresis");ok(!gate.update(t+=10,.7f,true),"hysteresis no rearm");
  gate.update(t+=10,0,true);ok(!gate.update(t+=200_000_000L,1,true),"stale release cannot fire");
  gate.update(t+=10,0,true);gate.update(t+=10,0,false);ok(!gate.update(t+=10,1,true),"focus loss requires release");
  gate.update(t+=10,0,true);ok(!gate.update(t+=10,Float.NaN,true),"invalid input");ok(!gate.update(t+=10,1,true),"invalid disarms");
  var closest=HandUse.closest(new HandUse.Point(0,2,1),1,0,0,2,1,2);ok(closest.x()==1&&closest.y()==1&&closest.z()==1,"nearest box point");
  for(String name:UseInstallation.TARGETS){
   byte[] bytes;try(var in=HandUseTest.class.getClassLoader().getResourceAsStream(name+".class")){bytes=in.readAllBytes();}
   byte[] patched=UseInstallation.transform(name,bytes,HandUseTest.class.getClassLoader());
   try{UseInstallation.transform(name,patched,HandUseTest.class.getClassLoader());throw new AssertionError("duplicate");}catch(IllegalStateException expected){checks++;}
  }
  UseInstallation.install(InstrumentationAgent.instrumentation,HandUseTest.class.getClassLoader());
  IsoObject door=new IsoObject();Main.jr.current.hits.add(new WorldMirror.Hit(door,new WorldMirror.Box(10.7f,19.5f,2.5f,10.75f,20.5f,4.5f),false));
  IsoObject other=new IsoObject();Interaction.gaze=new Interaction.Choice(other,null,1,"gaze",null);
  HandUse.configure(2,2);tick(1,true);ok(Main.calls==0,"held at enable blocked");press();
  ok(Main.calls==1&&Main.used.object()==door,"hand-selected native dispatch with scene origin/upper floor");
  ok(Traversal.cameraSelections==0,"gaze traversal not used for hand pick");
  tick(1,true);ok(Main.calls==1,"no repeat");
  Main.keyboard();ok(Main.used.object()==other,"ordinary E unaffected after scope");
  int before=Main.calls;Interaction.occluded=true;press();ok(Main.calls==before,"occluded target blocked");Interaction.occluded=false;
  hand=new XrCamera.Pose(0,0,1,0,0,0,1);press();ok(Main.calls==before,"outside hand radius");hand=new XrCamera.Pose(.6f,0,1,0,0,0,1);
  Main.access.actions=false;press();Main.access.actions=true;tick(1,true);ok(Main.calls==before,"menu return held grip blocked");press();ok(Main.calls==++before,"menu release rearms");
  tick(0,true);tick(1,false);tick(1,true);ok(Main.calls==before,"tracking reacquisition held blocked");
  tick(0,true);HandUse.clear();tick(1,true);ok(Main.calls==before,"loss between game ticks remembered");
  IsoPlayer.instance.busy=true;press();IsoPlayer.instance.busy=false;tick(1,true);ok(Main.calls==before,"timed action cannot queue grip");
  HandUse.configure(1,0);press();ok(Main.calls==before,"diagnostics no native action");ok(HandUse.reserves(0)&&HandUse.reserves(1),"either grip reserved");
  HandUse.configure(2,0);press();ok(Main.calls==++before,"simultaneous grips one action");tick(1,true);ok(Main.calls==before,"other held grip consumed");
  var window=new zombie.iso.objects.IsoWindow();Main.jr.current.hits.set(0,new WorldMirror.Hit(window,new WorldMirror.Box(10.7f,19.5f,2.5f,10.75f,20.5f,4.5f),false));press();
  ok(Main.used.object()==window&&Main.used.edge()!=null&&Main.used.edge().travel()==IsoDirections.E,"window native edge matches hand-selected object");before=Main.calls;
  window.present=false;press();ok(Main.calls==before,"removed object ignored");window.present=true;
  HandUse.configure(0,2);press();ok(Main.calls==before&&!HandUse.reserves(0)&&!HandUse.reserves(1),"Off no actions/no ownership");
  HandUse.configure(2,2);Main.throwUse=true;press();ok(HandUse.overrideChoice()==null&&!HandUse.selecting(),"exception clears scoped overrides");
  Main.throwUse=false;Main.keyboard();ok(Main.used.object()==other,"keyboard survives native use exception");
  System.out.println("Hand interaction checks passed: "+checks);
 }
}
