package viewpointvr.input;
import java.lang.invoke.MethodHandle;
/** Cross-thread immutable snapshots. Native callbacks are installed only in the user's game. */
public final class InputBridge {
 public interface Native {void mouse() throws Throwable;void move(Object player,Object vector) throws Throwable;void imgui() throws Throwable;default void beforeMovement(Object player)throws Throwable{}}
 private static volatile Native adapter;
 public static volatile ControllerState state=ControllerState.EMPTY;
 public static volatile ControllerLogic.Output output=ControllerLogic.Output.NONE;
 public static volatile int mode;
 public static volatile boolean uiMode;
 public static final java.util.concurrent.atomic.AtomicBoolean enterGameplay=new java.util.concurrent.atomic.AtomicBoolean();
 public static int effectiveMode(){return viewpointvr.diagnostic.ControllerDiagnostic.active()?1:mode;}
 public static ControllerState sample(){return viewpointvr.diagnostic.ControllerDiagnostic.sample(state);}
 public static volatile TurnSettings turning=TurnSettings.DEFAULT;
 public static String turning(int mode,int angle,int speed){
  var next=new TurnSettings(mode,angle,speed);turning=next;epoch.incrementAndGet();output=ControllerLogic.Output.NONE;return next.label();
 }
 public static volatile float aspect=16f/9;
 private static volatile long worldUntil,panelUntil;
 private static final java.util.concurrent.atomic.AtomicLong epoch=new java.util.concurrent.atomic.AtomicLong();
 public static long generation(){return epoch.get();}
 public static void trackingLost(){padReady=false;ControllerBridge.invalidateMapping();epoch.incrementAndGet();output=ControllerLogic.Output.NONE;}
 public static void panel(float value){aspect=value;panelUntil=System.nanoTime()+2_000_000_000L;}
 public static boolean panelReady(){return panelUntil!=0&&System.nanoTime()-panelUntil<0;}
 public static String status(){var s=state;boolean valid=s.fresh(System.nanoTime());return (mode==0?"Controllers Off":mode==1?"UI pointer":mode==2?"Pointer + move/turn":"VR gameplay / native gamepad")+" | "+turning.label()+" | tracking L="+(valid&&s.left().tracked())+" R="+(valid&&s.right().tracked())+(mode==3?" | "+ControllerBridge.status+" | Body: "+BodyTracking.status:"");}
 public static void install(Native value){adapter=value;}
 public static void configure(int value){epoch.incrementAndGet();if(value==3){uiMode=false;enterGameplay.set(true);}mode=value>=0&&value<=3?value:0;output=ControllerLogic.Output.NONE;padReady=false;BodyTracking.enabled=false;ControllerBridge.invalidateMapping();ControllerBridge.configure(mode==3?2:0);}
 public static void world(boolean tracked){worldUntil=tracked?System.nanoTime()+250_000_000L:0;if(!tracked){BodyTracking.enabled=false;padReady=false;}}
 public static boolean gameplay(){return worldUntil!=0&&System.nanoTime()-worldUntil<0;}
 public static volatile boolean padReady;
 public static boolean gamepadReady(){return mode==3&&padReady&&!viewpointvr.diagnostic.ControllerDiagnostic.active();}
 public static void clear(){viewpointvr.melee.SwingInput.reset();BodyTracking.xrFocused=false;padReady=false;ControllerBridge.invalidateMapping();BodyTracking.suspend();viewpointvr.diagnostic.ControllerDiagnostic.cancel("XR stopped or lost focus");epoch.incrementAndGet();panelUntil=0;state=ControllerState.EMPTY;output=ControllerLogic.Output.NONE;worldUntil=0;}
 public static PanelRay.Hit ray(){try{var s=sample();return effectiveMode()!=0&&panelReady()&&s.fresh(System.nanoTime())&&output.hit()!=null?PanelRay.hit(s.right().aim(),aspect):null;}catch(RuntimeException error){failed(error);return null;}}
 private static void failed(Throwable error){configure(0);clear();System.err.println("[Project Viewpoint VR Input] Disabled: "+error);}
 public static void mouse(MethodHandle original) throws Throwable {
  original.invokeExact();var n=adapter;if(n!=null)try{n.mouse();}catch(Throwable error){failed(error);}
 }
 public static void beforeMovement(Object player,MethodHandle original)throws Throwable{
  var n=adapter;if(n!=null)try{n.beforeMovement(player);}catch(Throwable error){failed(error);}
  original.invoke(player);
 }
 public static void move(Object player,Object vector,MethodHandle original) throws Throwable {
  var n=adapter;if(n!=null)try{n.move(player,vector);}catch(Throwable error){failed(error);}
  original.invoke(player,vector);
 }
 public static void imgui(MethodHandle original) throws Throwable {
  original.invokeExact();var n=adapter;if(n!=null)try{n.imgui();}catch(Throwable error){failed(error);}
 }
}
