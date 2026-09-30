import pzvr.input.*;
import org.lwjglx.input.*;
import zombie.core.input.Input;
import zombie.input.JoypadManager;
import zombie.characters.IsoPlayer;
/** Executes only original synthetic game fixtures with the production transformer and bridge. */
public final class ControllerBridgeTest {
    private static int checks;
    private static void ok(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    private static VrControllerState.Hand hand(float x,boolean a){return new VrControllerState.Hand("touch",true,x,0,0,0,a,false,false,false);}
    private static void publish(float x,boolean a){ControllerBridge.publish(new VrControllerState(1,System.nanoTime(),true,hand(x,false),hand(0,a)));}
    private static void tick(Input input) { for(int i=0;i<2;i++) { input.pollFixture(); input.updateGameThread(); } }
    public static void main(String[] args)throws Exception {
        ControllerInstallation.install(InstrumentationAgent.instrumentation,ControllerBridgeTest.class.getClassLoader());
        var input=new Input(); var physical=new Controller(0); Controllers.physical[0]=physical;
        ControllerBridge.configure(1);publish(0,false);tick(input);ok(input.getController(15)==null,"diagnostics no registration");
        pzvr.harness.Main.setMeleeMode(2);
        pzvr.harness.Main.setControllerMode(2);
        ok(pzvr.melee.MeleeInput.mode==0,"gamepad disables motion melee");
        pzvr.harness.Main.setMeleeMode(2);ok(pzvr.melee.MeleeInput.mode==0,"motion melee cannot compete");
        publish(0,false);tick(input);var vr=input.getController(15);
        ok(vr!=null&&vr!=physical,"registered");ok(Controller.physicalConstructions==1,"virtual constructor bypasses native metadata");
        ok(vr.getID()==15&&vr.getGUID().equals(ControllerBridge.GUID)&&vr.getGamepadName().equals("PZ VR Gamepad"),"identity");
        ok(vr.isGamepad()&&vr.getAxisCount()==6&&vr.getButtonCount()==15&&vr.getHatCount()==1&&vr.getDeadZone(0)==.2f,"metadata");
        publish(.6f,true);tick(input);ok(vr.getAxisValue(0)==.6f&&vr.isButtonPressed(0),"state reaches consumer");
        int presses=input.presses;tick(input);ok(input.presses==presses,"held input has no duplicate edge");
        ok(input.getController(0)==physical&&physical.getAxisValue(0)==.75f,"physical input unchanged");
        pzvr.interaction.HandUse.installed=true;
        pzvr.interaction.HandUse.configure(2,2);publish(0,false);tick(input);
        var grip=new VrControllerState.Hand("touch",true,0,0,0,1,false,false,false,false);
        ControllerBridge.publish(new VrControllerState(1,System.nanoTime(),true,grip,grip));tick(input);
        ok(vr.isButtonPressed(4)&&!vr.isButtonPressed(5),"only reserved right grip bumper suppressed");
        pzvr.interaction.HandUse.configure(2,0);publish(0,false);tick(input);
        ControllerBridge.publish(new VrControllerState(1,System.nanoTime(),true,grip,grip));tick(input);
        ok(!vr.isButtonPressed(4)&&!vr.isButtonPressed(5),"either interaction grip masks both bumpers");
        pzvr.interaction.HandUse.configure(0,2);tick(input);
        ok(!vr.isButtonPressed(4)&&!vr.isButtonPressed(5),"Off held grip requires neutral before bumper restore");
        publish(0,false);tick(input);publish(.6f,true);tick(input);
        int releases=input.releases; ControllerBridge.clear();tick(input);ok(input.releases==releases+1,"loss delivers release edge");ok(vr.getAxisValue(0)==0&&vr.getAxisValue(4)==-1&&!vr.isButtonPressed(0),"loss neutral");
        publish(.6f,true);tick(input);ok(!vr.isButtonPressed(0),"held resume blocked");
        publish(0,false);tick(input);publish(.6f,true);tick(input);ok(vr.isButtonPressed(0),"neutral rearms");
        ControllerBridge.configure(0);tick(input);ok(input.getController(15)==vr&&!vr.isButtonPressed(0),"off retains neutral identity");
        ControllerBridge.configure(2);publish(0,false);tick(input);
        var manager=JoypadManager.instance;var joy=new JoypadManager.Joypad();manager.joypads[0]=joy;manager.joypadsController[15]=joy;manager.joypadList.add(joy);
        IsoPlayer.players[0]=new IsoPlayer();IsoPlayer.players[0].setJoypadBind(15);zombie.GameWindow.activatedJoyPad=joy;
        var collision=new Controller(15);Controllers.physical[15]=collision;
        tick(input);ok(input.getController(15)==vr&&!vr.isButtonPressed(0),"collision drains virtual");
        input.pollFixture();input.updateGameThread(); // enters detach phase
        var raceStates=new GamepadState[16];for(int i=0;i<16;i++)raceStates[i]=new GamepadState();
        raceStates[15].axesButtons.axes(0,.9f);raceStates[15].axesButtons.buttons(0,(byte)1);
        ControllerBridge.poll(raceStates);
        ok(raceStates[15].axesButtons.axes(0)==0&&raceStates[15].axesButtons.buttons(0)==0,"detach race cannot leak physical state into old virtual buffer");
        for(int i=0;i<4;i++)tick(input);
        ok(input.getController(15)==collision,"physical handed back");ok(input.disconnects==1,"one virtual disconnect");
        ok(manager.joypads[0]==null&&manager.joypadsController[15]==null&&manager.joypadList.isEmpty(),"owned assignment released");
        ok(IsoPlayer.players[0].getJoypadBind()==-1&&zombie.GameWindow.activatedJoyPad==null,"player unbound");
        ok(!ControllerBridge.owns(15),"ownership cleared");
        publish(.6f,true);tick(input);ok(input.getController(15)==collision&&collision.getAxisValue(0)==.75f,"occupied slot preserved");
        // Hybrid ownership preserves gamepad controls but excludes RT from native combat.
        Controllers.physical[15]=null;
        pzvr.harness.Main.setControllerMode(2);
        pzvr.harness.Main.setAllowMotionMeleeWithGamepad(true);
        pzvr.harness.Main.setMeleeMode(2);
        publish(0,false);tick(input);
        ok(pzvr.melee.MeleeInput.mode==2,"hybrid keeps live motion melee");
        var triggerHand=new VrControllerState.Hand("touch",true,0,0,1,0,true,false,false,false);
        ControllerBridge.publish(new VrControllerState(2,System.nanoTime(),true,hand(.6f,false),triggerHand));tick(input);
        var hybrid=input.getController(15);
        ok(hybrid.getAxisValue(0)==.6f&&hybrid.isButtonPressed(0),"hybrid retains movement and buttons");
        ok(hybrid.getAxisValue(5)==-1,"hybrid consumes native RT");
        pzvr.harness.Main.setAllowMotionMeleeWithGamepad(false);tick(input);
        ok(pzvr.melee.MeleeInput.mode==0&&hybrid.getAxisValue(5)==-1,"routing change requires neutral");
        publish(0,false);tick(input);
        ControllerBridge.publish(new VrControllerState(3,System.nanoTime(),true,hand(0,false),triggerHand));tick(input);
        ok(hybrid.getAxisValue(5)==1,"gamepad-only restores native RT after neutral");
        pzvr.harness.Main.setAllowMotionMeleeWithGamepad(true);
        ok(pzvr.melee.MeleeInput.mode==2,"toggle retains chosen melee mode");
        pzvr.harness.Main.setMeleeMode(1);publish(0,false);tick(input);
        ControllerBridge.publish(new VrControllerState(4,System.nanoTime(),true,hand(0,false),triggerHand));tick(input);
        ok(pzvr.melee.MeleeInput.mode==1&&hybrid.getAxisValue(5)==-1,"diagnostics reserves RT without native attacks");
        pzvr.harness.Main.setMeleeMode(0);publish(0,false);tick(input);
        ControllerBridge.publish(new VrControllerState(5,System.nanoTime(),true,hand(0,false),triggerHand));tick(input);
        ok(hybrid.getAxisValue(5)==1,"melee Off restores RT even with hybrid checked");
        System.out.println("Controller lifecycle fixture checks passed: "+checks);
    }
}
