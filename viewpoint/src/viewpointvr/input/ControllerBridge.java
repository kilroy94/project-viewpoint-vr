package viewpointvr.input;

import org.lwjglx.input.Controller;
import org.lwjglx.input.GamepadState;
import zombie.core.input.Input;
import zombie.input.JoypadManager;
import zombie.GameWindow;
import zombie.characters.IsoPlayer;

/** Registry overlay. Native registry and physical polling are never overwritten. */
public final class ControllerBridge {
    public static final int SLOT=15;
    public static final String GUID="56505652000000000000000000000001";
    public static volatile boolean installed;
    public static volatile int mode; // 0 Off, 2 gamepad (native bridge routing)
    private static final GamepadMapper mapper=new GamepadMapper();
    public static synchronized void invalidateMapping(){mapper.invalidate();}
    private static Controller virtual;
    private static boolean constructing,retiring,detaching;
    private static int drained;
    private static final java.util.IdentityHashMap<GamepadState,Long> sequences=new java.util.IdentityHashMap<>();
    private static long lastLog;
    public static volatile String status="not registered";
    private static boolean occupiedLogged;
    private ControllerBridge() {}
    private static boolean usable(ControllerState s,long now){return s.fresh(now)&&(s.left().tracked()||s.right().tracked());}
    public static synchronized boolean constructing() { return constructing; }
    public static synchronized boolean owns(int id) { return virtual!=null && id==SLOT; }
    public static synchronized void configure(int value) {
        int next=value==2?2:0;
        if(mode!=next) { mapper.invalidate(); mode=next; System.out.println("[Project Viewpoint VR Gamepad] mode="+mode); }
    }
    public static synchronized Controller select(Controller physical,int id) {
        if(id!=SLOT || !installed) return physical;
        if(virtual!=null) {
            if(physical!=null && !retiring) {
                retiring=true; mapper.invalidate(); drained=0;
                System.out.println("[Project Viewpoint VR Gamepad] Physical controller claimed slot 15; retiring VR gamepad.");
            }
            return detaching?null:virtual;
        }
        if(physical!=null) {
            if(mode==2&&!occupiedLogged) { System.out.println("[Project Viewpoint VR Gamepad] Slot 15 occupied; VR gamepad unavailable. Physical device preserved."); occupiedLogged=true; }
            return physical;
        }
        occupiedLogged=false;
        if(mode!=2 || !usable(InputBridge.state,System.nanoTime())) return null;
        try {
            constructing=true; virtual=new Controller(SLOT); mapper.invalidate();status="ID 15: enable/assign Viewpoint VR Gamepad in native controller options";
            System.out.println("[Project Viewpoint VR Gamepad] Registered Viewpoint VR Gamepad at native controller ID 15; enable/assign through native controller UI.");
            return virtual;
        } finally { constructing=false; }
    }
    public static synchronized void poll(GamepadState[] states) {
        if(virtual==null) return;
        var pad=mapper.map(InputBridge.state,System.nanoTime(),mode==2&&!retiring&&!detaching&&InputBridge.gamepadReady());
        GamepadState target=states[SLOT];
        for(int i=0;i<6;i++) target.axesButtons.axes(i,pad.axes()[i]);
        for(int i=0;i<15;i++) target.axesButtons.buttons(i,(byte)(pad.buttons()[i]?1:0));
        target.hats.clear(); for(int i=0;i<target.hats.capacity();i++) target.hats.put(i,(byte)0);
        target.hats.put(0,(byte)pad.hat()); target.hatState=pad.hat(); target.polled=true; sequences.put(target,InputBridge.state.sequence());
    }
    /** Runs after native update/swap, exclusively on the game thread. */
    public static synchronized void consumed(Input input) {
        if(virtual==null) return;
        if(retiring) {
            if(!detaching) {
                boolean neutral=input.getController(SLOT)==virtual;
                for(int i=0;i<15;i++) neutral&=!input.isButtonPressedD(i,SLOT);
                for(int i=0;i<6;i++) neutral&=virtual.getAxisValue(i)==(i<4?0:-1);
                if(neutral) { if(++drained>=3) detaching=true; } else drained=0;
            }
            else if(input.getController(SLOT)==null) {
                // Native disconnect/Lua events have run. Remove only our own assignment/config record.
                var manager=JoypadManager.instance; var joy=manager.joypadsController[SLOT];
                if(joy!=null) {
                    for(int i=0;i<manager.joypads.length;i++) if(manager.joypads[i]==joy) {
                        manager.joypads[i]=null;
                        if(i<IsoPlayer.players.length && IsoPlayer.players[i]!=null && IsoPlayer.players[i].getJoypadBind()==SLOT) IsoPlayer.players[i].setJoypadBind(-1);
                    }
                    if(GameWindow.activatedJoyPad==joy) GameWindow.activatedJoyPad=null;
                    manager.joypadList.remove(joy); manager.joypadsController[SLOT]=null;
                }
                virtual=null; sequences.clear(); retiring=false; detaching=false; drained=0; mapper.invalidate();
                System.out.println("[Project Viewpoint VR Gamepad] Slot 15 released to physical controller.");
            }
        }
        long now=System.nanoTime();
        if(mode==2 && now-lastLog>=2_000_000_000L) {
            lastLog=now; Controller c=input.getController(SLOT);var player=IsoPlayer.players[0];
            boolean assigned=player!=null&&player.getJoypadBind()==SLOT;
            status=assigned?"ID 15 assigned":"ID 15: enable/assign Viewpoint VR Gamepad in native controller options";
            if(c==virtual&&c!=null) System.out.println("[Project Viewpoint VR Gamepad] game-thread id=15 sourceSeq="+sequences.getOrDefault(c.gamepadState,0L)
                +" axes="+java.util.Arrays.toString(new float[]{c.getAxisValue(0),c.getAxisValue(1),c.getAxisValue(2),c.getAxisValue(3),c.getAxisValue(4),c.getAxisValue(5)})
                +" buttons="+buttonMask(c)+" | "+status);
        }
    }
    private static int buttonMask(Controller c) { int mask=0; for(int i=0;i<15;i++) if(c.isButtonPressed(i)) mask|=1<<i; return mask; }
}
