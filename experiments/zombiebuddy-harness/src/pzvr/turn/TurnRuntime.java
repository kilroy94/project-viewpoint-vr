package pzvr.turn;

import java.lang.reflect.*;
import com.pavelvoronin.pz3d.NativeAvatar;
import com.pavelvoronin.pz3d.NativeLocomotion;
import pzvr.input.*;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.input.JoypadManager;

/** Game-thread turning and scoped native input ownership. Never runs game logic on rendering. */
public final class TurnRuntime {
    public record Settings(int mode,int snap,int speed,int source,int aim){}
    private static volatile Settings settings=new Settings(0,30,90,0,0);
    private static Settings applied;
    private static final TurnFilter filter=new TurnFilter();
    public static volatile boolean installed;
    private static volatile long xrHeartbeat,meleeUntil;
    private static volatile boolean uiBlocked;
    private static boolean ready,aimArmed,raw;
    private static Object pad,sourceIdentity;
    private static long tickTime;
    private static boolean faulted;
    private static volatile boolean bindingsChanged;
    public static void bindingsChanged(){bindingsChanged=true;}
    public static void configure(int mode,int snap,int speed,int source,int aim){
        Settings next=new Settings(Math.max(0,Math.min(2,mode)),java.util.Set.of(15,30,45,60,90).contains(snap)?snap:30,
            Math.max(30,Math.min(240,speed)),Math.max(0,Math.min(2,source)),Math.max(0,Math.min(3,aim)));
        if(!next.equals(settings)){settings=next;System.out.println("[PZ3D VR Turn] settings="+next);}
    }
    public static void heartbeat(boolean usable){xrHeartbeat=usable?System.nanoTime():0;}
    public static void block(boolean blocked){uiBlocked=blocked;}
    public static boolean meleeBlocked(){return System.nanoTime()<meleeUntil;}
    private static boolean context(){
        long now=System.nanoTime();
        return installed&&!faulted&&settings.mode()!=0&&!uiBlocked&&xrHeartbeat>0&&now>=xrHeartbeat&&now-xrHeartbeat<250_000_000L
            &&!GameTime.isGamePaused()&&!zombie.core.Core.getInstance().isDoingTextEntry()
            &&!zombie.network.GameClient.client&&!zombie.network.GameServer.server;
    }
    private static boolean live(){return applied==settings&&context()&&tickTime>0&&System.nanoTime()-tickTime<150_000_000L;}
    public static boolean aim(){return ready&&live();}
    public static boolean suppress(Object owner,int kind,int button){
        if(raw||owner!=pad||!live())return false;
        if(kind==0)return true; // both native aiming axes belong exclusively to turning
        int choice=settings.aim();
        if(kind==1)return choice==0;
        if(kind==2)return choice==1;
        if(kind==3)return choice==2;
        if(kind==4)return choice==3;
        if(kind==5&&choice!=0)try{return button==(int)field(owner,choice==1?"bumperLeft":choice==2?"bumperRight":"rightStickButton");}catch(Exception e){fail(e);}
        return false;
    }
    private static void clear(){pad=null;sourceIdentity=null;ready=false;aimArmed=false;tickTime=0;filter.reset();}
    public static void tick(){
        try {
            if(bindingsChanged){clear();bindingsChanged=false;}
            if(!context()){clear();return;}
            IsoPlayer p=IsoPlayer.getInstance();
            if(p==null||p.isDead()||!NativeLocomotion.firstPerson(p)||!NativeAvatar.controls(p)){clear();return;}
            Object access=callStatic("com.pavelvoronin.pz3d.Main","controlAccess");
            if(!(boolean)field(access,"look")||!(boolean)field(access,"combat")){clear();return;}
            Settings cfg=settings;
            if(applied!=cfg){clear();applied=cfg;}
            int id=p.getJoypadBind();
            Object assigned=id>=0&&id<JoypadManager.instance.joypadsController.length?JoypadManager.instance.joypadsController[id]:null;
            boolean physical=assigned!=null&&!ControllerBridge.owns(id);
            boolean usePad=cfg.source()==2||cfg.source()==0&&physical;
            Object identity=usePad?assigned:TurnRuntime.class;
            if(identity!=sourceIdentity){clear();sourceIdentity=identity;}
            float x,y;boolean held;
            raw=true;
            try {
                if(usePad){
                    if(assigned==null||(boolean)field(assigned,"disabled")||!(boolean)field(assigned,"connected")){clear();return;}
                    x=((Number)call(assigned,"getAimingAxisXRaw")).floatValue();y=((Number)call(assigned,"getAimingAxisYRaw")).floatValue();
                    held=(boolean)call(assigned,switch(cfg.aim()){case 1->"isLBPressed";case 2->"isRBPressed";case 3->"isR3Pressed";default->"isLTPressed";});
                    pad=assigned;
                }else{
                    var state=ControllerBridge.state;long now=System.nanoTime();
                    if(!state.usable(now)){clear();return;}
                    var l=state.left();var r=state.right();x=r.x();y=r.y();
                    held=switch(cfg.aim()){case 1->l.squeeze()>.65f;case 2->r.squeeze()>.65f;case 3->r.stick();default->l.trigger()>.7f;};
                    if(cfg.aim()==1&&pzvr.interaction.HandUse.reserves(0)||cfg.aim()==2&&pzvr.interaction.HandUse.reserves(1))held=false;
                    pad=assigned!=null&&ControllerBridge.owns(id)?assigned:null;
                }
            }finally{raw=false;}
            tickTime=System.nanoTime();
            if(!held)aimArmed=true;
            ready=held&&aimArmed;
            float radians=filter.update(tickTime,x,y,true,cfg.mode(),cfg.snap(),cfg.speed());
            if(radians!=0){
                meleeUntil=tickTime+200_000_000L;
                // Positive scene yaw turns toward the right eye offset (world +Y at yaw zero).
                Class<?> look=Class.forName("com.pavelvoronin.pz3d.LookState",false,TurnRuntime.class.getClassLoader());
                synchronized(look){
                    Object view=callStatic(look.getName(),"get");
                    float yaw=((Number)call(view,"yaw")).floatValue(),pitch=((Number)call(view,"pitch")).floatValue();
                    Method reset=look.getDeclaredMethod("reset",float.class,float.class);reset.setAccessible(true);reset.invoke(null,yaw+radians,pitch);
                }
            }
        }catch(Throwable e){fail(e);}
    }
    private static void fail(Throwable e){faulted=true;clear();System.err.println("[PZ3D VR Turn] Disabled: "+e);}
    private static Object callStatic(String name,String method)throws Exception{return method(Class.forName(name,false,TurnRuntime.class.getClassLoader()),method).invoke(null);}
    private static Object call(Object object,String name)throws Exception{return method(object.getClass(),name).invoke(object);}
    private static Method method(Class<?> type,String name)throws Exception{Method m=type.getDeclaredMethod(name);m.setAccessible(true);return m;}
    private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
}
