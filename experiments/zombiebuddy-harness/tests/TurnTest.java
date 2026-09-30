import pzvr.turn.*;
import pzvr.input.*;
import com.pavelvoronin.pz3d.*;
import zombie.input.JoypadManager;
public class TurnTest {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static void tick(){TurnRuntime.heartbeat(true);Main.tick();}
 static VrControllerState vr(float x,float y,float trigger){return new VrControllerState(1,System.nanoTime(),true,new VrControllerState.Hand("",true,0,0,trigger,0,false,false,false,false),new VrControllerState.Hand("",true,x,y,0,0,false,false,false,false));}
 public static void main(String[] args)throws Exception{
  var f=new TurnFilter();long t=1_000_000_000L;
  ok(f.update(t,1,0,true,1,30,90)==0,"held at enable");
  f.update(t+=10_000_000,0,0,true,1,30,90);
  ok(Math.abs(f.update(t+=10_000_000,1,0,true,1,30,90)-Math.PI/6)<.00001,"snap angle");
  ok(f.update(t+=10_000_000,1,0,true,1,30,90)==0,"held no repeat");
  ok(f.update(t+=10_000_000,-1,0,true,1,30,90)==0,"reversal needs center");
  f.update(t+=10_000_000,0,0,true,1,45,90);
  ok(f.update(t+=10_000_000,-1,0,true,1,45,90)<0,"left snap");
  f.reset();f.update(t,0,0,true,2,30,90);
  ok(Math.abs(f.update(t+=50_000_000,1,0,true,2,30,90)-Math.toRadians(4.5))<.00001,"smooth rate");
  ok(f.update(t+=10_000_000,.5f,1,true,2,30,90)==0,"vertical exclusion");
  ok(f.update(t+=1_000_000_000,1,0,true,2,30,90)==0,"stall disarms");
  ok(f.update(t+=10_000_000,Float.NaN,0,true,2,30,90)==0,"nonfinite");
  for(String name:TurnInstallation.TARGETS){
   byte[] bytes;try(var in=TurnTest.class.getClassLoader().getResourceAsStream(name+".class")){bytes=in.readAllBytes();}
   byte[] patched=TurnInstallation.transform(name,bytes,TurnTest.class.getClassLoader());
   try{TurnInstallation.transform(name,patched,TurnTest.class.getClassLoader());throw new AssertionError("double install");}catch(IllegalStateException expected){checks++;}
   var writer=new net.bytebuddy.jar.asm.ClassWriter(0);
   new net.bytebuddy.jar.asm.ClassReader(bytes).accept(new net.bytebuddy.jar.asm.ClassVisitor(net.bytebuddy.jar.asm.Opcodes.ASM9,writer){
    @Override public net.bytebuddy.jar.asm.MethodVisitor visitMethod(int a,String n,String d,String signature,String[] e){
     if(java.util.Set.of("tick","attackAim","getLTValue").contains(n))return null;
     return super.visitMethod(a,n,d,signature,e);
    }
   },0);
   try{TurnInstallation.transform(name,writer.toByteArray(),TurnTest.class.getClassLoader());throw new AssertionError("missing hook accepted");}catch(IllegalStateException expected){checks++;}
  }
  TurnInstallation.install(InstrumentationAgent.instrumentation,TurnTest.class.getClassLoader());
  var pad=new JoypadManager.Joypad();JoypadManager.instance.joypadsController[0]=pad;
  TurnRuntime.configure(1,30,90,0,0);tick();pad.x=1;tick();
  ok(Math.abs(LookState.yaw()-Math.PI/6)<.0001,"physical configured stick turns shared heading right");
  ok(LookState.pitch()==.2f,"pitch unchanged");ok(pad.getAimingAxisXRaw()==0,"stick consumed");
  ok(!NativeAvatar.attackAim(),"turn does not aim");ok(TurnRuntime.meleeBlocked(),"turn disarms contact gestures");
  tick();ok(Math.abs(LookState.yaw()-Math.PI/6)<.0001,"no repeated snap via tick");
  pad.lt=1;tick();ok(NativeAvatar.attackAim(),"separate LT aim");ok(pad.getLTValue()==-1,"native LT consumed");
  var other=new JoypadManager.Joypad();other.x=1;ok(other.getAimingAxisXRaw()==1,"unowned controller unaffected");
  Main.access.look=false;tick();ok(pad.getAimingAxisXRaw()==1&&pad.getLTValue()==1,"menu restores native controls");ok(!NativeAvatar.attackAim(),"menu clears aim");
  Main.access.look=true;tick();ok(!NativeAvatar.attackAim(),"held aim after menu needs release");
  pad.x=0;pad.lt=-1;tick();pad.lt=1;tick();ok(NativeAvatar.attackAim(),"aim rearms");
  TurnRuntime.configure(1,45,90,2,3);tick();pad.r3=false;pad.x=0;tick();pad.r3=true;tick();
  ok(NativeAvatar.attackAim()&&!pad.isR3Pressed()&&!pad.isButtonStartPress(10),"custom aim suppresses semantic and generic button");
  ok(pad.getLTValue()==1,"unselected LT restored");
  TurnRuntime.heartbeat(false);ok(!NativeAvatar.attackAim()&&pad.getAimingAxisXRaw()==0&&pad.isR3Pressed(),"focus loss releases ownership");
  TurnRuntime.configure(1,15,90,1,0);ControllerBridge.publish(vr(0,0,0));tick();ControllerBridge.publish(vr(1,0,0));float before=LookState.yaw();tick();
  ok(Math.abs(LookState.yaw()-before-Math.toRadians(15))<.0001,"direct XR input without gamepad mode");
  ControllerBridge.publish(vr(0,0,1));tick();ok(NativeAvatar.attackAim(),"XR aim");
  ControllerBridge.clear();tick();ok(!NativeAvatar.attackAim(),"stale/lost XR input disarms");
  pzvr.interaction.HandUse.installed=true;pzvr.interaction.HandUse.configure(2,2);
  TurnRuntime.configure(1,30,90,1,2);ControllerBridge.publish(vr(0,0,0));tick();
  var grip=new VrControllerState.Hand("touch",true,0,0,0,1,false,false,false,false);
  ControllerBridge.publish(new VrControllerState(1,System.nanoTime(),true,grip,grip));tick();
  ok(!NativeAvatar.attackAim(),"interaction grip cannot also ready via raw VR input");
  pzvr.interaction.HandUse.configure(0,2);
  tick();ok(!NativeAvatar.attackAim(),"restoring grip aim while held requires release");
  ControllerBridge.publish(vr(0,0,0));tick();ControllerBridge.publish(new VrControllerState(1,System.nanoTime(),true,grip,grip));tick();
  ok(NativeAvatar.attackAim(),"restored grip aim rearms after release");
  TurnRuntime.configure(0,30,90,0,0);pad.x=1;tick();ok(pad.getAimingAxisXRaw()==1&&!NativeAvatar.attackAim(),"Off restores native");
  TurnRuntime.configure(1,30,90,2,0);pad.x=0;pad.lt=-1;tick();
  float saved=LookState.yaw();pad.connected=false;pad.x=1;tick();ok(LookState.yaw()==saved,"disconnected physical pad ignored");
  pad.connected=true;tick();ok(LookState.yaw()==saved,"reconnect held stick needs neutral");
  pad.x=0;tick();TurnRuntime.block(true);pad.x=1;tick();ok(LookState.yaw()==saved&&pad.getAimingAxisXRaw()==1,"options block restores native");
  TurnRuntime.block(false);tick();ok(LookState.yaw()==saved,"options return requires neutral");
  NativeLocomotion.first=false;pad.lt=1;tick();ok(!NativeAvatar.attackAim()&&pad.getLTValue()==1,"unsupported camera restores native");
  System.out.println("Turning checks passed: "+checks);
 }
}
