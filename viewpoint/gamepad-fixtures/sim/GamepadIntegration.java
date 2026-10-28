package sim;
import viewpointvr.input.*;
import viewpointvr.instrument.TestAgent;
import viewpointvr.xr.XrCamera.Pose;
import org.lwjglx.input.*;
import zombie.core.input.Input;
import zombie.input.JoypadManager;
import zombie.characters.IsoPlayer;
/** Native polling/edge lifecycle against authored doubles only. */
public final class GamepadIntegration {
 static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static ControllerState.Hand hand(float x,int bits){return new ControllerState.Hand(new Pose(0,0,0,0,0,0,1),null,x,0,0,false,0,bits);}
 static void publish(float x,int bits){InputBridge.state=new ControllerState(1,System.nanoTime(),true,hand(x,0),hand(0,bits));InputBridge.padReady=true;}
 static void tick(Input input){for(int i=0;i<2;i++){input.pollFixture();input.updateGameThread();}}
 public static void main(String[] args)throws Exception{
  ControllerInstallation.install(TestAgent.instrumentation,GamepadIntegration.class.getClassLoader());
  var input=new Input();zombie.GameWindow.GameInput=input;var physical=new Controller(0);Controllers.physical[0]=physical;
  check(!ControllerBridge.available(),"assignment unavailable before owned controller exists");
  InputBridge.configure(3);publish(0,0);tick(input);var vr=input.getController(15);
  check(vr!=null&&vr!=physical,"registered");check(Controller.physicalConstructions==1,"synthetic constructor owns no physical device");
  check(ControllerBridge.available(),"assignment recognizes exact owned native controller");
  check(vr.getGamepadName().equals("Viewpoint VR Gamepad")&&vr.getGUID().equals(ControllerBridge.GUID),"unique identity");
  publish(.6f,1);tick(input);check(vr.getAxisValue(0)==.6f&&vr.isButtonPressed(0),"axes and face button reach consumer");
  int presses=input.presses;tick(input);check(input.presses==presses,"no duplicate held edge");
  check(physical.getAxisValue(0)==.75f&&input.getController(0)==physical,"physical pad preserved");
  int releases=input.releases;InputBridge.clear();tick(input);check(input.releases==releases+1&&!vr.isButtonPressed(0)&&vr.getAxisValue(4)==-1,"loss releases buttons and triggers");
  publish(.6f,1);tick(input);check(!vr.isButtonPressed(0),"held resume blocked");publish(0,0);tick(input);publish(.6f,1);tick(input);check(vr.isButtonPressed(0),"neutral rearms");
  InputBridge.padReady=false;tick(input);check(!vr.isButtonPressed(0),"UI routing suppresses gameplay buttons");InputBridge.padReady=true;tick(input);check(!vr.isButtonPressed(0),"held UI recovery blocked");
  InputBridge.configure(0);tick(input);check(input.getController(15)==vr&&!vr.isButtonPressed(0),"Off retains neutral identity");
  check(!ControllerBridge.available(),"Off cannot request gameplay assignment");
  InputBridge.configure(3);publish(0,0);tick(input);
  var manager=JoypadManager.instance;var joy=new JoypadManager.Joypad();manager.joypads[0]=joy;manager.joypadsController[15]=joy;manager.joypadList.add(joy);
  IsoPlayer.players[0]=new IsoPlayer();IsoPlayer.players[0].setJoypadBind(15);zombie.GameWindow.activatedJoyPad=joy;
  var collision=new Controller(15);Controllers.physical[15]=collision;tick(input);check(input.getController(15)==vr&&!vr.isButtonPressed(0),"hotplug drains virtual");
  check(!ControllerBridge.available(),"assignment rejects retiring controller");
  input.pollFixture();input.updateGameThread();var race=new GamepadState[16];for(int i=0;i<16;i++)race[i]=new GamepadState();race[15].axesButtons.axes(0,.9f);race[15].axesButtons.buttons(0,(byte)1);ControllerBridge.poll(race);
  check(race[15].axesButtons.axes(0)==0&&race[15].axesButtons.buttons(0)==0,"detach race cannot leak physical input");
  for(int i=0;i<4;i++)tick(input);
  check(input.getController(15)==collision&&input.disconnects==1,"one disconnect hands back physical slot");
  check(!ControllerBridge.available(),"assignment cannot claim replacement physical controller");
  check(manager.joypads[0]==null&&manager.joypadsController[15]==null&&manager.joypadList.isEmpty(),"only virtual assignment removed");
  check(IsoPlayer.players[0].getJoypadBind()==-1&&zombie.GameWindow.activatedJoyPad==null,"virtual player unbound");
  publish(.6f,1);tick(input);check(input.getController(15)==collision&&collision.getAxisValue(0)==.75f,"occupied slot never replaced");
  check(GamepadIntegration.class.getProtectionDomain().getCodeSource().getLocation().equals(Controller.class.getProtectionDomain().getCodeSource().getLocation()),"native classes are fixtures");
  System.out.println("Gamepad lifecycle: "+checks+" checks passed; authored fixtures only");
 }
}
