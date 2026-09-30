package pzvr.interaction;

import java.lang.reflect.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import pzvr.input.ControllerBridge;
import pzvr.xr.*;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoObject;
import zombie.iso.IsoDirections;
import zombie.iso.objects.IsoWindow;
import com.pavelvoronin.pz3d.NativeAvatar;
import com.pavelvoronin.pz3d.NativeLocomotion;

/** Render publishes immutable world-space poses; all targeting and native use runs on the game thread. */
public final class HandUse {
    public static volatile boolean installed;
    private record Settings(int mode,int hand){}
    private static volatile Settings settings=new Settings(0,2); // off, right
    private static Settings applied;
    public record Point(float x,float y,float z){
        Vector3f vector(){return new Vector3f(x,y,z);}
        public boolean finite(){return Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z);}
    }
    private record Pose(long time,long epoch,Point head,Point left,Point right){}
    private static final java.util.concurrent.atomic.AtomicLong epoch=new java.util.concurrent.atomic.AtomicLong();
    private static long appliedEpoch;
    private static int trackedMask;
    private static volatile Pose pose;
    private static volatile boolean blocked;
    private static final PressGate[] presses={new PressGate(),new PressGate()};
    private static boolean failed;
    private static Object playerIdentity;
    private static final ThreadLocal<Object> override=new ThreadLocal<>();
    private static final ThreadLocal<Boolean> selecting=ThreadLocal.withInitial(()->false);
    private static final float RADIUS=.25f;
    public static void configure(int mode,int hand){
        Settings next=new Settings(Math.clamp(mode,0,2),Math.clamp(hand,0,2));
        if(!next.equals(settings)){settings=next;clear();ControllerBridge.invalidateMapping();pzvr.turn.TurnRuntime.bindingsChanged();System.out.println("[PZ3D VR Use] settings="+next);}
    }
    /** Reserved globally while enabled, so menus cannot receive the same grip as a bumper. */
    public static boolean reserves(int side){var s=settings;return installed&&s.mode()!=0&&(s.hand()==0||s.hand()==side+1);}
    public static Object overrideChoice(){return override.get();}
    public static boolean selecting(){return selecting.get();}
    public static void block(boolean value){blocked=value;if(value)clear();}
    public static void clear(){epoch.incrementAndGet();pose=null;}
    private static Point point(Matrix4f matrix,XrCamera.Pose p,Vector3f origin){
        if(p==null)return null;
        Vector3f v=matrix.transformPosition(new Vector3f(p.x(),p.y(),p.z())).add(origin);
        Point result=new Point(v.x,v.y,v.z);return result.finite()?result:null;
    }
    public static void capture(Object frame,Matrix4f sceneFromLocal,XrCamera.Pose head,HandPoses hands,boolean valid){
        if(!valid||!installed||failed||settings.mode()==0){clear();return;}
        try{
            Object scene=field(frame,"scene");
            Vector3f origin=new Vector3f(number(scene,"ox"),number(scene,"oy"),number(scene,"oz"));
            int mask=(hands.left()!=null?1:0)|(hands.right()!=null?2:0);
            if(mask!=trackedMask){epoch.incrementAndGet();trackedMask=mask;}
            pose=new Pose(System.nanoTime(),epoch.get(),point(sceneFromLocal,head,origin),point(sceneFromLocal,hands.left(),origin),point(sceneFromLocal,hands.right(),origin));
        }catch(Exception e){fail(e);}
    }
    private static void reset(){for(var p:presses)p.reset();playerIdentity=null;}
    public static void tick(){
        try{
            long now=System.nanoTime();Pose snapshot=pose;Settings cfg=settings;
            if(applied!=cfg){reset();applied=cfg;}
            if(snapshot!=null&&appliedEpoch!=snapshot.epoch()){reset();appliedEpoch=snapshot.epoch();}
            var input=ControllerBridge.state;
            if(!installed||failed||cfg.mode()==0||blocked||snapshot==null||snapshot.head()==null||now<snapshot.time()||now-snapshot.time()>150_000_000L
                ||!input.usable(now)||input.left().menu()||input.right().menu()||pzvr.turn.TurnRuntime.meleeBlocked()||zombie.GameTime.isGamePaused()
                ||zombie.core.Core.getInstance().isDoingTextEntry()||zombie.network.GameClient.client||zombie.network.GameServer.server){reset();return;}
            IsoPlayer player=IsoPlayer.getInstance();
            if(player==null||player.isDead()||player.getVehicle()!=null||player.hasTimedActions()||player.isGrappling()
                ||player.isAttackStarted()||!NativeLocomotion.firstPerson(player)||!NativeAvatar.controls(player)){reset();return;}
            Object access=call(type("Main"),"controlAccess");
            if(!(boolean)field(access,"actions")||!(boolean)field(access,"look")){reset();return;}
            if(playerIdentity!=player){reset();playerIdentity=player;}
            Object controller=field(type("Main"),"jq"),world=field(type("Main"),"jr");
            if(controller==null||world==null||(boolean)call(type("Traversal"),"active")||(boolean)invoke(type("NativeInteraction"),"ownsMotion",player)
                ||(boolean)invoke(type("Vehicles"),"ownsMotion",player)){reset();return;}
            for(int side=0;side<2;side++){
                Point hand=side==0?snapshot.left():snapshot.right();
                float value=(side==0?input.left():input.right()).squeeze();
                if(!presses[side].update(now,value,reserves(side)&&hand!=null))continue;
                Object choice=choose(player,controller,world,snapshot.head(),hand);
                if(choice==null){System.out.println("[PZ3D VR Use] "+(side==0?"left":"right")+": no nearby reachable use target");continue;}
                System.out.println("[PZ3D VR Use] "+(cfg.mode()==1?"diagnostic":"use")+" hand="+side+" object="+call(choice,"object").getClass().getSimpleName()+" action="+call(choice,"tap"));
                if(cfg.mode()==2){
                    override.set(choice);
                    try{invoke(type("Main"),"b",false);}finally{override.remove();}
                }
                // One native action per tick, including simultaneous grips; consume the other press.
                for(int other=side+1;other<2;other++)presses[other].update(now,input.right().squeeze(),reserves(other)&&snapshot.right()!=null);
                break;
            }
        }catch(Exception e){fail(e);}finally{override.remove();selecting.remove();}
    }
    /** Bounds are only a candidate filter. Native ray picking/visibility must confirm a surface beside the hand. */
    private static Object choose(IsoPlayer player,Object controller,Object world,Point head,Point hand)throws Exception{
        if(!hand.finite()||head.vector().distance(hand.vector())>1.75f)return null;
        Object scene=call(world,"scene");List<Candidate> nearby=new ArrayList<>();
        for(Object hit:(Iterable<?>)call(scene,"targets")){
            Object object=call(hit,"object");if(!(object instanceof IsoObject obj)||obj.getSquare()==null||obj.getObjectIndex()<0)continue;
            Object box=call(hit,"box");
            Point near=closest(hand,number(box,"x0"),number(box,"y0"),number(box,"z0"),number(box,"x1"),number(box,"y1"),number(box,"z1"));
            float distance=hand.vector().distance(near.vector());
            if(distance<=RADIUS)nearby.add(new Candidate(obj,(boolean)call(hit,"north"),near,distance));
        }
        for(Object pick:(Iterable<?>)call(world,"itemTargets")){
            Object object=call(pick,"object");if(!(object instanceof IsoObject obj)||obj.getSquare()==null||obj.getWorldObjectIndex()<0)continue;
            Object box=call(pick,"bounds");
            Point near=closest(hand,number(box,"x0"),number(box,"y0"),number(box,"z0"),number(box,"x1"),number(box,"y1"),number(box,"z1"));
            float distance=hand.vector().distance(near.vector());
            if(distance<=RADIUS)nearby.add(new Candidate(obj,false,near,distance));
        }
        nearby.sort(Comparator.comparingDouble(Candidate::distance));
        selecting.set(true);
        try{
            for(Candidate c:nearby){
                Vector3f direction=c.point().vector().sub(head.vector());if(direction.lengthSquared()<.000001f)continue;direction.normalize();
                Object choice=invoke(type("Interaction"),"choose",player,controller,world,head.vector(),direction,false);
                if(choice==null||call(choice,"object")!=c.object()||((String)call(choice,"tap")).isEmpty())continue;
                Vector3f surface=new Vector3f(direction).mul(number(choice,"distance")).add(head.vector());
                if(surface.distance(hand.vector())>RADIUS+.005f)continue;
                IsoObject obj=(IsoObject)call(choice,"object");
                if(obj instanceof IsoWindow){
                    boolean north=c.north();var sq=obj.getSquare();
                    IsoDirections travel=north?(direction.y<0?IsoDirections.N:IsoDirections.S):(direction.x<0?IsoDirections.W:IsoDirections.E);
                    Object edge=construct(type("Traversal$Edge"),obj,sq.getX(),sq.getY(),north,travel);
                    choice=construct(type("Interaction$Choice"),obj,edge,number(choice,"distance"),call(choice,"tap"),call(choice,"container"));
                }
                return choice;
            }
            return null;
        }finally{selecting.remove();}
    }
    private record Candidate(IsoObject object,boolean north,Point point,float distance){}
    public static Point closest(Point p,float x0,float y0,float z0,float x1,float y1,float z1){
        return new Point(Math.clamp(p.x(),x0,x1),Math.clamp(p.y(),y0,y1),Math.clamp(p.z(),z0,z1));
    }
    private static void fail(Exception e){failed=true;clear();System.err.println("[PZ3D VR Use] Disabled: "+e);e.printStackTrace();}
    private static Class<?> type(String name)throws ClassNotFoundException{return Class.forName("com.pavelvoronin.pz3d."+name,false,HandUse.class.getClassLoader());}
    private static float number(Object o,String name)throws Exception{return ((Number)call(o,name)).floatValue();}
    private static Object field(Object o,String name)throws Exception{Class<?> c=o instanceof Class<?> t?t:o.getClass();Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o instanceof Class<?>?null:o);}
    private static Object call(Object o,String name)throws Exception{return invoke(o,name);}
    private static boolean matches(Class<?>[] types,Object[] args){
        if(types.length!=args.length)return false;
        for(int i=0;i<types.length;i++)if(args[i]!=null&&!types[i].isInstance(args[i])&&!(types[i]==boolean.class&&args[i] instanceof Boolean)&&!(types[i]==int.class&&args[i] instanceof Integer)&&!(types[i]==float.class&&args[i] instanceof Float))return false;
        return true;
    }
    private static Object invoke(Object o,String name,Object...args)throws Exception{
        Class<?> c=o instanceof Class<?> t?t:o.getClass();
        for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&matches(m.getParameterTypes(),args)){m.setAccessible(true);return m.invoke(o instanceof Class<?>?null:o,args);}
        throw new NoSuchMethodException(c.getName()+"."+name);
    }
    private static Object construct(Class<?> c,Object...args)throws Exception{
        for(Constructor<?> m:c.getDeclaredConstructors())if(matches(m.getParameterTypes(),args)){m.setAccessible(true);return m.newInstance(args);}
        throw new NoSuchMethodException(c.getName()+" constructor");
    }
}
