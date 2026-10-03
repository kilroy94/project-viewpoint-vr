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
 private final Field mouseX,mouseY,captured,yaw,enabled,third,free,iris,cursorMode,settings,onboarding;
 private final ControllerLogic logic=new ControllerLogic();
 private final MouseMerge mouse=new MouseMerge();private int previousRoute;
 private long generation=-1;private PanelRay.Hit lastHit;
 private boolean imguiHeld;private PanelRay.Hit imguiLastHit;
 private final java.util.concurrent.atomic.AtomicInteger wheel=new java.util.concurrent.atomic.AtomicInteger();
 public NativeInput(ClassLoader loader)throws ReflectiveOperationException{
  mouseX=field(loader,"zombie.input.Mouse","x");mouseY=field(loader,"zombie.input.Mouse","y");
  captured=field(loader,"viewpoint.input.Look","captured");yaw=field(loader,"viewpoint.input.Look","yaw");
  enabled=field(loader,"viewpoint.core.View","enabled");third=field(loader,"viewpoint.input.ThirdPerson","active");free=field(loader,"viewpoint.input.FreeCam","active");
  iris=field(loader,"viewpoint.platform.IrisPacks","active");cursorMode=field(loader,"viewpoint.FP","cursorMode");
  settings=field(loader,"viewpoint.platform.SettingsWindow","shown");onboarding=field(loader,"viewpoint.platform.Onboarding","shown");
 }
 private static Field field(ClassLoader loader,String type,String name)throws ReflectiveOperationException{var f=Class.forName(type,false,loader).getDeclaredField(name);f.setAccessible(true);return f;}
 private boolean physicalButtons(){if(Mouse.buttonDownStates!=null)for(boolean pressed:Mouse.buttonDownStates)if(pressed)return true;return false;}
 private boolean focused(){long window=org.lwjglx.opengl.Display.getWindow();return window!=0&&glfwGetWindowAttrib(window,GLFW_FOCUSED)!=0;}
 private boolean owns()throws IllegalAccessException{
  var p=IsoPlayer.players[0];return focused()&&enabled.getBoolean(null)&&!third.getBoolean(null)&&!free.getBoolean(null)&&iris.get(null)==null
   &&!zombie.network.GameClient.client&&!zombie.network.GameServer.server&&p!=null&&!p.isDead()&&p.getVehicle()==null;
 }
 public void mouse()throws Throwable{
  boolean valid=owns();boolean released=!captured.getBoolean(null)&&InputBridge.panelReady();
  boolean modal=zombie.ui.UIManager.isModalVisible();
  boolean menu=settings.getBoolean(null)||onboarding.getBoolean(null);
  int route=menu?2:1;
  if(previousRoute!=route)logic.reset();
  long now=System.nanoTime();
  viewpointvr.diagnostic.ControllerDiagnostic.beforeMouse(now,valid&&!modal&&!onboarding.getBoolean(null),released,route,physicalButtons(),Mouse.wheelDelta,Core.getInstance().getScreenWidth(),Core.getInstance().getScreenHeight());
  long current=InputBridge.generation();if(current!=generation){generation=current;logic.reset();}
  var next=logic.step(valid?InputBridge.sample():ControllerState.EMPTY,now,InputBridge.effectiveMode(),released,
    valid&&InputBridge.gameplay()&&!modal&&!menu&&!zombie.GameTime.isGamePaused(),InputBridge.aspect,InputBridge.turning);
  if(next.menu()&&valid&&!modal&&!menu&&!zombie.GameTime.isGamePaused()){cursorMode.setBoolean(null,!cursorMode.getBoolean(null));logic.reset();next=ControllerLogic.Output.NONE;}
  InputBridge.output=next;
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
  if(route==1)viewpointvr.diagnostic.ControllerDiagnostic.observed("vanilla",Mouse.buttonDownStates[0],(float)mouseX.getInt(null)/Core.getInstance().getScreenWidth(),(float)mouseY.getInt(null)/Core.getInstance().getScreenHeight());
  if(next.turn()!=0)yaw.setFloat(null,(float)Math.IEEEremainder(yaw.getFloat(null)+next.turn(),Math.PI*2));
 }
 public void move(Object player,Object vector)throws Throwable{
  if(player!=IsoPlayer.players[0]||!owns()||!InputBridge.gameplay()||!InputBridge.state.fresh(System.nanoTime())||!captured.getBoolean(null))return;
  var p=(IsoPlayer)player;if(p.isBlockMovement()||zombie.GameTime.isGamePaused()||zombie.ui.UIManager.isModalVisible()||settings.getBoolean(null)||onboarding.getBoolean(null))return;
  var v=(Vector2)vector;var output=InputBridge.output;
  if(InputBridge.mode==2&&v.getLength()<.001f)v.set(output.moveX(),-output.moveY());
 }
 public void imgui()throws Throwable{
  boolean valid=InputBridge.effectiveMode()!=0&&InputBridge.sample().fresh(System.nanoTime())&&focused();
  var out=valid?InputBridge.output:ControllerLogic.Output.NONE;
  var point=out.hit()!=null?out.hit():imguiHeld?imguiLastHit:null;
  var io=ImGui.getIO();
  if(point!=null){io.setMousePos(point.x()*io.getDisplaySizeX(),point.y()*io.getDisplaySizeY());io.setMouseDown(0,io.getMouseDown(0)||out.down());}
  imguiHeld=out.hit()!=null&&out.down();if(out.hit()!=null)imguiLastHit=out.hit();
  int scroll=wheel.getAndSet(0);if(valid&&out.hit()!=null&&scroll!=0)io.setMouseWheel(io.getMouseWheel()+scroll);
  viewpointvr.diagnostic.ControllerDiagnostic.observed("viewpoint",io.getMouseDown(0),io.getMousePosX()/io.getDisplaySizeX(),io.getMousePosY()/io.getDisplaySizeY());
 }
}
