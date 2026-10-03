package sim;
import java.lang.invoke.*;
import viewpointvr.input.*;
import viewpointvr.xr.XrCamera.Pose;
import zombie.input.Mouse;
import zombie.iso.Vector2;
import zombie.characters.IsoPlayer;
import viewpoint.input.Look;
import viewpoint.platform.SettingsWindow;
import imgui.ImGui;
/** Executes the production adapter against authored native/device doubles, never game classes. */
public final class InputIntegration {
 static int checks,nativeMouse,nativeUi,nativeMove;static boolean physical;static int physicalWheel;
 static MethodHandle mouse,ui,move;
 static final ControllerState.Hand NEUTRAL=hand(.5f,.5f,0,0,0);
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static void near(float a,float b,String why){check(Math.abs(a-b)<.001,why+": "+a);}
 static ControllerState.Hand hand(float x,float y,float trigger,float sx,float sy){return new ControllerState.Hand(new Pose((x-.5f)*2,(.5f-y)*1.125f,0,0,0,0,1),null,sx,sy,trigger,false);}
 public static void physicalMouse(){nativeMouse++;Mouse.buttonPrevStates[0]=Mouse.buttonDownStates[0];Mouse.buttonDownStates[0]=physical;Mouse.x=11;Mouse.y=13;Mouse.wheelDelta=physicalWheel;}
 public static void physicalUi(){nativeUi++;ImGui.io.x=11;ImGui.io.y=13;ImGui.io.down=physical;ImGui.io.wheel=physicalWheel;}
 public static void movement(Object p,Object v){nativeMove++;}
 static void sample(ControllerState.Hand left,ControllerState.Hand right){InputBridge.state=new ControllerState(1,System.nanoTime(),true,left,right);InputBridge.panel(16f/9);InputBridge.world(true);}
 static void tick(ControllerState.Hand right)throws Throwable{sample(NEUTRAL,right);InputBridge.mouse(mouse);InputBridge.imgui(ui);}
 static void fresh()throws Throwable{
  InputBridge.clear();InputBridge.configure(2);InputBridge.turning(1,30,90);InputBridge.install(new NativeInput(InputIntegration.class.getClassLoader()));
  physical=false;physicalWheel=0;Mouse.buttonDownStates[0]=Mouse.buttonPrevStates[0]=false;
  Look.captured=false;Look.yaw=0;SettingsWindow.shown=false;viewpoint.platform.Onboarding.shown=false;
  zombie.ui.UIManager.modal=false;zombie.GameTime.paused=false;org.lwjgl.glfw.GLFW.focused=true;
  IsoPlayer.players[0].blocked=false;IsoPlayer.players[0].dead=false;tick(NEUTRAL);
 }
 static final class Widget {
  int presses,releases,drags;boolean held;float releaseX,releaseY;
  void read(boolean down,float x,float y){if(down&&!held)presses++;if(down&&held)drags++;if(!down&&held){releases++;releaseX=x;releaseY=y;}held=down;}
 }
 public static void main(String[] args)throws Throwable{
  var path=InputIntegration.class.getProtectionDomain().getCodeSource().getLocation();
  for(Class<?> type:new Class<?>[]{Mouse.class,IsoPlayer.class,ImGui.class,Look.class,org.lwjgl.glfw.GLFW.class})check(path.equals(type.getProtectionDomain().getCodeSource().getLocation()),"native dependency is an authored fixture: "+type);
  check(!path.equals(NativeInput.class.getProtectionDomain().getCodeSource().getLocation()),"adapter comes from production compilation");
  var lookup=MethodHandles.lookup();mouse=lookup.findStatic(InputIntegration.class,"physicalMouse",MethodType.methodType(void.class));ui=lookup.findStatic(InputIntegration.class,"physicalUi",MethodType.methodType(void.class));move=lookup.findStatic(InputIntegration.class,"movement",MethodType.methodType(void.class,Object.class,Object.class));
  fresh();var w=new Widget();tick(hand(.2f,.3f,1,0,0));w.read(Mouse.buttonDownStates[0],Mouse.x,Mouse.y);
  near(Mouse.x,320,"vanilla pointer x");near(Mouse.y,270,"vanilla pointer top-left y");check(!Mouse.buttonPrevStates[0],"press edge after physical poll");
  tick(hand(.7f,.6f,1,0,0));w.read(Mouse.buttonDownStates[0],Mouse.x,Mouse.y);check(Mouse.buttonPrevStates[0],"drag preserves down history");
  tick(hand(.7f,.6f,0,0,0));w.read(Mouse.buttonDownStates[0],Mouse.x,Mouse.y);
  check(w.presses==1&&w.drags==1&&w.releases==1,"one vanilla press/drag/release");near(w.releaseX,1120,"vanilla drop position");
  fresh();physical=true;tick(hand(.4f,.4f,1,0,0));tick(ControllerState.NONE);check(Mouse.buttonDownStates[0]&&ImGui.io.down,"controller loss does not release physical hold");physical=false;tick(NEUTRAL);check(!Mouse.buttonDownStates[0],"physical release retained");
  fresh();tick(hand(.4f,.4f,1,0,0));tick(ControllerState.NONE);check(!Mouse.buttonDownStates[0]&&Mouse.buttonPrevStates[0],"tracking loss releases vanilla hold");near(Mouse.x,640,"loss releases at last vanilla point");tick(hand(.4f,.4f,1,0,0));check(!Mouse.buttonDownStates[0],"held tracking recovery cannot click");
  fresh();SettingsWindow.shown=true;tick(NEUTRAL);var iw=new Widget();tick(hand(.25f,.25f,1,0,0));iw.read(ImGui.io.down,ImGui.io.x,ImGui.io.y);
  check(ImGui.io.down&&!Mouse.buttonDownStates[0],"settings routes press to ImGui only");
  tick(hand(.75f,.65f,1,0,0));iw.read(ImGui.io.down,ImGui.io.x,ImGui.io.y);near(ImGui.io.x,1200,"ImGui drag x");near(ImGui.io.y,585,"ImGui drag y");
  tick(ControllerState.NONE);iw.read(ImGui.io.down,ImGui.io.x,ImGui.io.y);
  check(iw.presses==1&&iw.drags==1&&iw.releases==1,"one ImGui press/drag/loss release");near(iw.releaseX,1200,"ImGui loss releases at last controller point");near(iw.releaseY,585,"ImGui loss release y");
  fresh();SettingsWindow.shown=true;tick(NEUTRAL);tick(hand(.6f,.4f,1,0,0));InputBridge.configure(0);InputBridge.imgui(ui);check(!ImGui.io.down,"Off releases ImGui held button");near(ImGui.io.x,960,"Off keeps ImGui release location");
  fresh();tick(hand(.5f,.5f,0,0,1));check(Mouse.wheelDelta==1,"vanilla scroll direction");InputBridge.imgui(ui);check(ImGui.io.wheel==0,"vanilla scroll not copied into settings");
  fresh();SettingsWindow.shown=true;tick(NEUTRAL);tick(hand(.5f,.5f,0,0,-1));check(Mouse.wheelDelta==0&&ImGui.io.wheel==-1,"settings scroll isolated");InputBridge.imgui(ui);check(ImGui.io.wheel==0,"settings wheel consumed once");
  fresh();Look.captured=true;tick(NEUTRAL);sample(hand(.5f,.5f,0,0,1),hand(.5f,.5f,0,1,0));InputBridge.mouse(mouse);
  var vector=new Vector2();InputBridge.move(IsoPlayer.players[0],vector,move);near(vector.y,-1,"walk reaches native vector convention");near(Look.yaw,(float)Math.PI/6,"snap reaches native yaw");
  vector.set(.8f,.2f);InputBridge.move(IsoPlayer.players[0],vector,move);near(vector.x,.8f,"physical movement takes priority");
  SettingsWindow.shown=true;tick(hand(.5f,.5f,0,1,0));vector.set(0,0);InputBridge.move(IsoPlayer.players[0],vector,move);near(vector.getLength(),0,"settings stop walking");near(Look.yaw,(float)Math.PI/6,"settings stop turning");
  fresh();Look.captured=true;tick(NEUTRAL);zombie.GameTime.paused=true;sample(hand(.5f,.5f,0,0,1),hand(.5f,.5f,0,1,0));InputBridge.mouse(mouse);vector.set(0,0);InputBridge.move(IsoPlayer.players[0],vector,move);near(vector.getLength(),0,"pause prevents locomotion");near(Look.yaw,0,"pause prevents turn");
  fresh();tick(hand(.3f,.4f,1,0,0));org.lwjgl.glfw.GLFW.focused=false;tick(hand(.3f,.4f,1,0,0));check(!Mouse.buttonDownStates[0],"window focus loss releases");org.lwjgl.glfw.GLFW.focused=true;tick(hand(.3f,.4f,1,0,0));check(!Mouse.buttonDownStates[0],"window focus recovery held blocked");
  fresh();tick(hand(.3f,.4f,1,0,0));InputBridge.state=new ControllerState(1,System.nanoTime()-300_000_000L,true,NEUTRAL,NEUTRAL);InputBridge.mouse(mouse);check(!Mouse.buttonDownStates[0],"stale sample releases through adapter");
  fresh();SettingsWindow.shown=true;tick(NEUTRAL);tick(hand(.2f,.6f,1,0,0));org.lwjgl.glfw.GLFW.focused=false;InputBridge.imgui(ui);check(!ImGui.io.down,"focus loss releases ImGui");near(ImGui.io.x,320,"focus loss keeps release location");
  fresh();tick(hand(.3f,.4f,1,0,0));SettingsWindow.shown=true;tick(hand(.3f,.4f,1,0,0));check(!ImGui.io.down&&!Mouse.buttonDownStates[0],"opening settings cannot carry held click into new UI");tick(NEUTRAL);tick(hand(.3f,.4f,1,0,0));check(ImGui.io.down,"settings arms after neutral");SettingsWindow.shown=false;tick(hand(.3f,.4f,1,0,0));check(!Mouse.buttonDownStates[0]&&!ImGui.io.down,"closing settings cannot click through");
  fresh();zombie.core.Core.width=800;zombie.core.Core.height=450;ImGui.io.width=1200;ImGui.io.height=675;tick(hand(.75f,.25f,0,0,0));near(Mouse.x,600,"vanilla resized pointer");near(ImGui.io.x,900,"ImGui own display size mapping");zombie.core.Core.width=1600;zombie.core.Core.height=900;ImGui.io.width=1600;ImGui.io.height=900;
  fresh();SettingsWindow.shown=true;tick(NEUTRAL);sample(NEUTRAL,hand(.5f,.5f,0,0,1));InputBridge.mouse(mouse);InputBridge.configure(0);InputBridge.imgui(ui);check(ImGui.io.wheel==0,"Off discards queued scroll");InputBridge.configure(2);tick(NEUTRAL);check(ImGui.io.wheel==0,"reenable cannot replay old scroll");
  fresh();tick(hand(.3f,.4f,1,0,0));InputBridge.clear();InputBridge.mouse(mouse);InputBridge.imgui(ui);check(!Mouse.buttonDownStates[0]&&!ImGui.io.down,"session disconnect releases both UI paths");tick(hand(.3f,.4f,1,0,0));check(!Mouse.buttonDownStates[0],"reconnect held input requires neutral");
  fresh();SettingsWindow.shown=true;tick(NEUTRAL);sample(NEUTRAL,hand(.5f,.5f,0,0,1));InputBridge.mouse(mouse);InputBridge.trackingLost();InputBridge.imgui(ui);check(ImGui.io.wheel==0,"tracking loss between input and drawing discards queued scroll");
  fresh();int m=nativeMouse,u=nativeUi;tick(NEUTRAL);check(nativeMouse==m+1&&nativeUi==u+1,"native physical callbacks exactly once");
  InputBridge.install(null);InputBridge.clear();InputBridge.configure(0);
  System.out.println("Native input integration: "+checks+" checks passed; authored fixtures only");
 }
}
