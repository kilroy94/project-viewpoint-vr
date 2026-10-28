package viewpointvr.input;
import java.lang.reflect.Field;
import imgui.ImGui;
import zombie.input.Mouse;
import zombie.core.Core;
import zombie.characters.IsoPlayer;
import zombie.iso.Vector2;
import static org.lwjgl.glfw.GLFW.*;
/** Game-thread native input overlay; never sends OS input or changes physical device state. */
public final class NativeInput implements InputBridge.Native {
 private final Field mouseX,mouseY,captured,yaw,enabled,third,free,iris,cursorMode,settings,onboarding,crash;
 private boolean previousPadReady;
 private long lastBodyLog;private float roomDistance;private int roomTicks;
 private RoomMotion.Step lastRoom=RoomMotion.Step.NONE;
 private final ControllerLogic logic=new ControllerLogic();
 private final MouseMerge mouse=new MouseMerge();private int previousRoute;
 private long generation=-1;private PanelRay.Hit lastHit;
 private boolean imguiHeld;private PanelRay.Hit imguiLastHit;
 private final java.util.concurrent.atomic.AtomicInteger wheel=new java.util.concurrent.atomic.AtomicInteger();
 public NativeInput(ClassLoader loader)throws ReflectiveOperationException{
  mouseX=field(loader,"zombie.input.Mouse","x");mouseY=field(loader,"zombie.input.Mouse","y");
  captured=field(loader,"viewpoint.input.Look","captured");yaw=field(loader,"viewpoint.input.Look","yaw");
  enabled=field(loader,"viewpoint.core.View","enabled");third=field(loader,"viewpoint.core.View","thirdPerson");free=field(loader,"viewpoint.input.FreeCam","active");
  iris=field(loader,"viewpoint.platform.IrisPacks","active");cursorMode=field(loader,"viewpoint.FP","cursorMode");
  settings=field(loader,"viewpoint.platform.SettingsWindow","shown");onboarding=field(loader,"viewpoint.platform.Onboarding","shown");crash=field(loader,"viewpoint.platform.CrashReport","shown");
 }
 private static Field field(ClassLoader loader,String type,String name)throws ReflectiveOperationException{var f=Class.forName(type,false,loader).getDeclaredField(name);f.setAccessible(true);return f;}
 private boolean physicalButtons(){if(Mouse.buttonDownStates!=null)for(boolean pressed:Mouse.buttonDownStates)if(pressed)return true;return false;}
 private boolean focused(){long window=org.lwjglx.opengl.Display.getWindow();return window!=0&&glfwGetWindowAttrib(window,GLFW_FOCUSED)!=0;}
 private boolean owns()throws IllegalAccessException{
  var p=IsoPlayer.players[0];return (focused()||InputBridge.mode==3&&BodyTracking.xrFocused&&BodyTracking.latest()!=null)&&enabled.getBoolean(null)&&!third.getBoolean(null)&&!free.getBoolean(null)&&iris.get(null)==null
   &&!zombie.network.GameClient.client&&!zombie.network.GameServer.server&&p!=null&&!p.isDead()&&p.getVehicle()==null;
 }
 private boolean menu()throws IllegalAccessException{return settings.getBoolean(null)||onboarding.getBoolean(null)||crash.getBoolean(null);}
 private void refreshGameplay()throws Throwable{
  if(InputBridge.mode==3){if(InputBridge.enterGameplay.getAndSet(false))cursorMode.setBoolean(null,false);InputBridge.uiMode=cursorMode.getBoolean(null);}
  boolean valid=owns(),modal=zombie.ui.UIManager.isModalVisible(),menu=menu(),paused=zombie.GameTime.isGamePaused();
  boolean hmd=BodyTracking.latest()!=null;
  var player=IsoPlayer.players[0];
  viewpointvr.melee.SwingInput.eligibleWeapon=player!=null&&player.getPrimaryHandItem() instanceof zombie.inventory.types.HandWeapon weapon&&weapon.isMelee()&&!weapon.isRanged()&&weapon.getCondition()>0;
  BodyTracking.enabled=InputBridge.mode==3&&valid&&hmd&&BodyTracking.xrFocused&&InputBridge.gameplay()&&!paused;
  boolean released=InputBridge.mode==3?InputBridge.uiMode:!captured.getBoolean(null);
  InputBridge.padReady=valid&&!released&&!modal&&!menu&&!paused&&InputBridge.gameplay();
  if(previousPadReady&&!InputBridge.padReady)ControllerBridge.invalidateMapping();previousPadReady=InputBridge.padReady;
  BodyTracking.status=InputBridge.mode!=3?"gameplay Off":!InputBridge.gameplay()?"waiting for tracked world":!hmd?"head pose stale":!BodyTracking.xrFocused?"XR not focused; check SteamVR input routing/dashboard":!valid?"use first person, on foot, single player":paused?"paused":IsoPlayer.players[0].isBlockMovement()?"native movement blocked":"active (physical steps independent of mouse/pad assignment)";
 }
 public void mouse()throws Throwable{
  refreshGameplay();
  boolean valid=owns();boolean modal=zombie.ui.UIManager.isModalVisible(),menu=menu();
  boolean released=(InputBridge.mode==3?InputBridge.uiMode||modal||menu||zombie.GameTime.isGamePaused():!captured.getBoolean(null))&&InputBridge.panelReady();
  int route=menu?2:1;
  if(previousRoute!=route)logic.reset();
  long now=System.nanoTime();
  viewpointvr.diagnostic.ControllerDiagnostic.beforeMouse(now,valid&&!modal&&!onboarding.getBoolean(null)&&!crash.getBoolean(null),released,route,physicalButtons(),Mouse.wheelDelta,Core.getInstance().getScreenWidth(),Core.getInstance().getScreenHeight());
  long current=InputBridge.generation();if(current!=generation){generation=current;logic.reset();}
  var next=logic.step(valid?InputBridge.sample():ControllerState.EMPTY,now,InputBridge.effectiveMode(),released,
    valid&&InputBridge.gameplay()&&!modal&&!menu&&!zombie.GameTime.isGamePaused(),InputBridge.aspect,InputBridge.turning);
  if(next.menu()&&valid&&!modal&&!menu&&!zombie.GameTime.isGamePaused()){InputBridge.uiMode=InputBridge.mode==3?!InputBridge.uiMode:!cursorMode.getBoolean(null);cursorMode.setBoolean(null,InputBridge.uiMode);InputBridge.padReady=false;ControllerBridge.invalidateMapping();logic.reset();next=ControllerLogic.Output.NONE;}
  if(InputBridge.mode==3&&IsoPlayer.players[0]!=null&&IsoPlayer.players[0].getJoypadBind()>=0&&IsoPlayer.players[0].getJoypadBind()!=ControllerBridge.SLOT)next=new ControllerLogic.Output(next.hit(),next.down(),next.scroll(),next.moveX(),next.moveY(),0,next.menu());
  InputBridge.output=next;
  boolean mouseReady=Mouse.buttonDownStates!=null&&Mouse.buttonPrevStates!=null&&Mouse.buttonDownStates.length>0&&Mouse.buttonPrevStates.length>0;
  var point=next.hit()!=null?next.hit():mouse.held()?lastHit:null;
  if(next.hit()!=null)lastHit=next.hit();
  if(Mouse.buttonDownStates!=null&&Mouse.buttonPrevStates!=null&&Mouse.buttonDownStates.length>0&&Mouse.buttonPrevStates.length>0){
   var buttons=mouse.step(Mouse.buttonDownStates[0],Mouse.buttonPrevStates[0],route==1&&next.down(),route);
   Mouse.buttonPrevStates[0]=buttons.previous();Mouse.buttonDownStates[0]=buttons.down();
  }
  previousRoute=route;
  if(point!=null){
   int w=Core.getInstance().getScreenWidth(),h=Core.getInstance().getScreenHeight();
   mouseX.setInt(null,Math.min(w-1,Math.max(0,Math.round(point.x()*w))));
   mouseY.setInt(null,Math.min(h-1,Math.max(0,Math.round(point.y()*h))));
   if(route==1)Mouse.wheelDelta+=next.scroll();
   Mouse.lastActivity=System.currentTimeMillis();
  }
  if(route==2&&next.hit()!=null)wheel.addAndGet(next.scroll());else wheel.set(0);
  if(route==1&&mouseReady)viewpointvr.diagnostic.ControllerDiagnostic.observed("vanilla",Mouse.buttonDownStates[0],(float)mouseX.getInt(null)/Core.getInstance().getScreenWidth(),(float)mouseY.getInt(null)/Core.getInstance().getScreenHeight());
  if(next.turn()!=0)yaw.setFloat(null,(float)Math.IEEEremainder(yaw.getFloat(null)+next.turn(),Math.PI*2));
 }
 public void beforeMovement(Object player)throws Throwable{
  if(player!=IsoPlayer.players[0])return;
  refreshGameplay();var p=(IsoPlayer)player;var h=BodyTracking.current();lastRoom=RoomMotion.Step.NONE;RoomMotion.Step requested=RoomMotion.Step.NONE;String roomResult="tracking/native movement gated";
  if(h!=null&&!p.isBlockMovement()){
   requested=RoomMotion.request(h,yaw.getFloat(null));lastRoom=NativeRoomMotion.move(p,requested);roomResult=NativeRoomMotion.status;
   BodyTracking.consume(lastRoom.x(),lastRoom.y(),yaw.getFloat(null),h.scale());
  }
  float accepted=(float)Math.hypot(lastRoom.x(),lastRoom.y());if(accepted>0){roomDistance+=accepted;roomTicks++;}
  long now=System.nanoTime();if(InputBridge.mode==3&&now-lastBodyLog>2_000_000_000L){lastBodyLog=now;
   System.out.println("[Project Viewpoint VR Body] "+BodyTracking.status+" desktopFocus="+focused()+" mouseCaptured="+captured.getBoolean(null)+" ui="+InputBridge.uiMode+" headOffset="+(h==null?"missing":h.x()+","+h.z())+" actor="+p.getX()+","+p.getY()+" nativePad="+p.getJoypadBind()+" requested="+requested.x()+","+requested.y()+" physicalMove="+lastRoom.x()+","+lastRoom.y()+" roomResult="+roomResult+" windowDistance="+roomDistance+" movedTicks="+roomTicks);roomDistance=0;roomTicks=0;
  }
 }
 public void move(Object player,Object vector)throws Throwable{
  if(player!=IsoPlayer.players[0])return;refreshGameplay();
  if(!owns()||!InputBridge.gameplay()||!InputBridge.state.fresh(System.nanoTime()))return;
  boolean released=InputBridge.mode==3?InputBridge.uiMode:!captured.getBoolean(null);
  var p=(IsoPlayer)player;if(released||p.isBlockMovement()||zombie.GameTime.isGamePaused()||zombie.ui.UIManager.isModalVisible()||menu())return;
  var v=(Vector2)vector;var output=InputBridge.output;
  if((InputBridge.mode==2||InputBridge.mode==3)&&v.getLength()<.001f)v.set(output.moveX(),-output.moveY());
 }
 public void imgui()throws Throwable{
  boolean valid=InputBridge.effectiveMode()!=0&&InputBridge.sample().fresh(System.nanoTime())&&(focused()||InputBridge.mode==3&&BodyTracking.xrFocused);
  var out=valid?InputBridge.output:ControllerLogic.Output.NONE;
  var point=out.hit()!=null?out.hit():imguiHeld?imguiLastHit:null;
  var io=ImGui.getIO();
  if(point!=null){io.setMousePos(point.x()*io.getDisplaySizeX(),point.y()*io.getDisplaySizeY());io.setMouseDown(0,io.getMouseDown(0)||out.down());}
  imguiHeld=out.hit()!=null&&out.down();if(out.hit()!=null)imguiLastHit=out.hit();
  int scroll=wheel.getAndSet(0);if(valid&&out.hit()!=null&&scroll!=0)io.setMouseWheel(io.getMouseWheel()+scroll);
  viewpointvr.diagnostic.ControllerDiagnostic.observed("viewpoint",io.getMouseDown(0),io.getMousePosX()/io.getDisplaySizeX(),io.getMousePosY()/io.getDisplaySizeY());
 }
}
