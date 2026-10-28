package viewpointvr.melee;
import viewpointvr.input.*;import viewpointvr.xr.XrCamera.Pose;
public final class SwingTest {
 static int checks;static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static final Pose HEAD=new Pose(0,1.7f,0,0,0,0,1);
 static ControllerState state(long now,float x,float trigger){var grip=new Pose(x,-.3f,-.5f,0,0,0,1);var h=new ControllerState.Hand(grip,grip,0,0,trigger,false);return new ControllerState(1,now,true,h,h);}
 static long swing(long now){SwingInput.sample(HEAD,state(now,.3f,0),now);for(int i=1;i<=5;i++){now+=20_000_000L;SwingInput.sample(HEAD,state(now,.3f+i*.04f,1),now);}return now;}
 public static void main(String[] args){
  InputBridge.configure(3);InputBridge.padReady=true;BodyTracking.xrFocused=true;ControllerBridge.assigned=true;SwingInput.eligibleWeapon=true;long now=System.nanoTime();
  SwingInput.configure(1);now=swing(now);check(SwingInput.status.startsWith("diagnostic swing")&&SwingInput.trigger(now)==-1,"diagnostic recognizes gesture without attack");
  check(SwingInput.feedback().equals("Diagnostics: recognized swings 1"),"persistent diagnostic count exposed for panel");
  InputBridge.padReady=false;SwingInput.sample(HEAD,state(now+20_000_000L,.7f,1),now+20_000_000L);check(SwingInput.feedback().endsWith("1"),"diagnostic count survives blocked status and focus loss");InputBridge.padReady=true;
  SwingInput.configure(2);now=swing(now+20_000_000L);check(SwingInput.trigger(now)==1,"deliberate held swing creates native pulse");check(SwingInput.trigger(now+100_000_000L)==-1,"pulse expires");
  SwingInput.sample(HEAD,state(now+20_000_000L,.7f,1),now+20_000_000L);check(SwingInput.trigger(now+100_000_000L)==-1,"held trigger cannot repeat");
  SwingInput.eligibleWeapon=false;check(SwingInput.trigger(now)==-1,"weapon loss immediately removes pulse");SwingInput.eligibleWeapon=true;
  ControllerBridge.assigned=false;SwingInput.sample(HEAD,state(now,.3f,0),now);check(SwingInput.trigger(now)==-1&&SwingInput.status.contains("ID 15"),"unowned physical pad cannot receive motion attacks");ControllerBridge.assigned=true;
  SwingInput.configure(2);var detector=new SwingDetector();detector.sample(new SwingDetector.Sample(now,0,1,0,0,1.7f,0,false,true));boolean fired=false;for(int i=1;i<=8;i++)fired|=detector.sample(new SwingDetector.Sample(now+i*20_000_000L,i*.04f,1,0,i*.04f,1.7f,0,true,true));check(!fired,"whole-body physical walking never counts as swing");
  InputBridge.padReady=true;BodyTracking.xrFocused=true;SwingInput.configure(2);now=swing(System.nanoTime());InputBridge.padReady=false;check(SwingInput.trigger(now)==-1,"UI instantly releases pending pulse");InputBridge.padReady=true;BodyTracking.xrFocused=false;check(SwingInput.trigger(now)==-1,"XR focus instantly releases pending pulse");BodyTracking.xrFocused=true;
  SwingInput.configure(1);long testTime=System.nanoTime();var fixedHand=new org.joml.Matrix4f().translation(.3f,1.4f,-.5f);
  for(int i=0;i<10;i++){float angle=i*.1f;var q=new org.joml.Quaternionf().rotationY(angle);var movingHead=new Pose(0,1.7f,0,q.x,q.y,q.z,q.w);var relative=movingHead.matrix().invert().mul(fixedHand);var position=relative.getTranslation(new org.joml.Vector3f());var rotation=relative.getNormalizedRotation(new org.joml.Quaternionf());var grip=new Pose(position.x,position.y,position.z,rotation.x,rotation.y,rotation.z,rotation.w);long t=testTime+i*20_000_000L;var hand=new ControllerState.Hand(grip,grip,0,0,i==0?0:1,false);SwingInput.sample(movingHead,new ControllerState(1,t,true,hand,hand),t);}
  check(!SwingInput.status.startsWith("diagnostic swing"),"rotating head with stationary physical hand cannot count as attack");
  var mapper=new GamepadMapper();var neutral=state(now,0,0);mapper.map(neutral,now,true,-1f);var moving=new ControllerState(now,now,true,new ControllerState.Hand(HEAD,HEAD,.8f,0,0,false),new ControllerState.Hand(HEAD,HEAD,0,0,1,false));var mapped=mapper.map(moving,now,true,1f);check(mapped.axes()[0]==.8f&&mapped.axes()[5]==1,"native stick movement and motion RT coexist");
  SwingInput.configure(0);check(SwingInput.trigger(now)==null,"Off restores native trigger mapping");SwingInput.configure(1);check(SwingInput.feedback().endsWith("0"),"new diagnostic selection resets count");SwingInput.configure(0);InputBridge.configure(0);InputBridge.clear();System.out.println("Motion/native hybrid: "+checks+" checks passed");
 }
}
