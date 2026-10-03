package viewpointvr.input;
import java.lang.invoke.MethodHandle;
/** Cross-thread immutable snapshots. Native callbacks are installed only in the user's game. */
public final class InputBridge {
 public interface Native {void mouse() throws Throwable;void move(Object player,Object vector) throws Throwable;void imgui() throws Throwable;}
 private static volatile Native adapter;
 public static volatile ControllerState state=ControllerState.EMPTY;
 public static volatile ControllerLogic.Output output=ControllerLogic.Output.NONE;
 public static volatile int mode;
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
 public static void trackingLost(){epoch.incrementAndGet();output=ControllerLogic.Output.NONE;}
 public static void panel(float value){aspect=value;panelUntil=System.nanoTime()+2_000_000_000L;}
 public static boolean panelReady(){return panelUntil!=0&&System.nanoTime()-panelUntil<0;}
 public static String status(){var s=state;boolean valid=s.fresh(System.nanoTime());return mode==0?"Controllers Off":(mode==1?"UI pointer":"Pointer + move/turn")+" | "+turning.label()+" | tracking L="+(valid&&s.left().tracked())+" R="+(valid&&s.right().tracked());}
 public static void install(Native value){adapter=value;}
 public static void configure(int value){epoch.incrementAndGet();mode=value>=0&&value<=2?value:0;output=ControllerLogic.Output.NONE;}
 public static void world(boolean tracked){worldUntil=tracked?System.nanoTime()+250_000_000L:0;}
 public static boolean gameplay(){return worldUntil!=0&&System.nanoTime()-worldUntil<0;}
 public static void clear(){viewpointvr.diagnostic.ControllerDiagnostic.cancel("XR stopped or lost focus");epoch.incrementAndGet();panelUntil=0;state=ControllerState.EMPTY;output=ControllerLogic.Output.NONE;worldUntil=0;}
 public static PanelRay.Hit ray(){try{var s=sample();return effectiveMode()!=0&&panelReady()&&s.fresh(System.nanoTime())&&output.hit()!=null?PanelRay.hit(s.right().aim(),aspect):null;}catch(RuntimeException error){failed(error);return null;}}
 private static void failed(Throwable error){configure(0);clear();System.err.println("[Project Viewpoint VR Input] Disabled: "+error);}
 public static void mouse(MethodHandle original) throws Throwable {
  original.invokeExact();var n=adapter;if(n!=null)try{n.mouse();}catch(Throwable error){failed(error);}
 }
 public static void move(Object player,Object vector,MethodHandle original) throws Throwable {
  var n=adapter;if(n!=null)try{n.move(player,vector);}catch(Throwable error){failed(error);}
  original.invoke(player,vector);
 }
 public static void imgui(MethodHandle original) throws Throwable {
  original.invokeExact();var n=adapter;if(n!=null)try{n.imgui();}catch(Throwable error){failed(error);}
 }
}
