package viewpointvr.melee;
import org.joml.Vector3f;
import viewpointvr.input.*;
import viewpointvr.xr.XrCamera.Pose;
/** Recognized motion feeds one native gamepad RT pulse; no direct damage or combat replay. */
public final class SwingInput {
 public static volatile boolean eligibleWeapon;
 public static volatile int mode; // Off, diagnostics, native attacks
 public static volatile String status="motion melee Off";
 private static final SwingDetector detector=new SwingDetector();private static long epoch=-1,pulseUntil,turnUntil,sequence;
 private SwingInput(){}
 public static synchronized String feedback(){return (mode==0?"Motion Off":mode==1?"Diagnostics: recognized swings ":"Live: native attack pulses ")+(mode==0?"":Long.toString(sequence));}
 public static void configure(int value){synchronized(SwingInput.class){mode=value>=0&&value<=2?value:0;reset();sequence=0;status=mode==0?"motion melee Off":mode==1?"motion diagnostics":"motion attacks: release RT, then hold and swing";}ControllerBridge.invalidateMapping();}
 public static synchronized void reset(){detector.reset();pulseUntil=0;epoch=-1;turnUntil=0;}
 public static synchronized Float trigger(long now){return mode==0?null:mode==2&&eligibleWeapon&&ControllerBridge.assigned&&InputBridge.gamepadReady()&&BodyTracking.xrFocused&&now<pulseUntil&&now>=pulseUntil-80_000_000L?1f:-1f;}
 public static synchronized void sample(Pose head,ControllerState state,long now){
  if(mode==0)return;
  if(InputBridge.output.turn()!=0)turnUntil=now+200_000_000L;
  long current=InputBridge.generation()+BodyTracking.calibration();
  if(current!=epoch){detector.reset();pulseUntil=0;epoch=current;}
  if(!BodyTracking.xrFocused||!InputBridge.gamepadReady()||!state.fresh(now)||state.right().grip()==null||now<turnUntil){detector.reset();pulseUntil=0;status="motion blocked: focus/UI/tracking/turn";return;}
  if(mode==2&&!eligibleWeapon){detector.reset();pulseUntil=0;status="motion attacks need an equipped melee weapon";return;}
  if(mode==2&&!ControllerBridge.assigned){detector.reset();pulseUntil=0;status="motion attacks need Viewpoint VR Gamepad assigned (ID 15)";return;}
  var hand=head.matrix().mul(state.right().grip().matrix()).getTranslation(new Vector3f());
  boolean fire=detector.sample(new SwingDetector.Sample(now,hand.x,hand.y,hand.z,head.x(),head.y(),head.z(),state.right().trigger()>.65f,true));
  if(fire){sequence++;if(mode==2)pulseUntil=now+80_000_000L;status=(mode==2?"native RT pulse":"diagnostic swing")+" #"+sequence;
   System.out.println("[Project Viewpoint VR Melee] "+status+" (native targeting, animation, cooldown and damage rules; not weapon contact)");}
 }
}
