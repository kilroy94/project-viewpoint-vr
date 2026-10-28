package viewpointvr.rig;
import java.lang.reflect.Field;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import viewpointvr.StereoCamera;
import viewpointvr.input.*;
/** Game-thread snapshot adapter. Metadata is resolved without native class initialization. */
public final class NativeRig implements RigBridge.Native {
 private final Field place,model;private final SnapshotArms arms=new SnapshotArms();private long lastLog;
 public NativeRig(ClassLoader loader)throws ReflectiveOperationException{place=SnapshotArms.field(Class.forName("viewpoint.models.ModelCapture",false,loader),"place");model=SnapshotArms.field(Class.forName("viewpoint.models.ModelCapture",false,loader),"model");
  for(String name:List.of("eyeX","eyeY","eyeZ","eyeLean","viewYaw"))SnapshotArms.field(Class.forName("viewpoint.core.Frame",false,loader),name);
  SnapshotArms.field(Class.forName("zombie.core.skinnedmodel.model.ModelSlotRenderData",false,loader),"modelData");
  for(String name:List.of("matrixPalette","xfrm","model","modelInstance","parent"))SnapshotArms.field(Class.forName("zombie.core.skinnedmodel.model.ModelInstanceRenderData",false,loader),name);}
 private static float number(Object object,String name)throws ReflectiveOperationException{return ((Number)SnapshotArms.get(object,name)).floatValue();}
 public RigBridge.Scope begin(Object frame,Object slot,Object player,boolean local,boolean own,Object kind,float x,float y,float z,float angle)throws Throwable{
  var h=BodyTracking.current();var state=h==null?ControllerState.EMPTY:h.controllers();
  if(!local||!own||InputBridge.mode!=3||h==null||h.pose()==null||!state.fresh(System.nanoTime())||state.left().grip()==null&&state.right().grip()==null||!kind.toString().equals("SNAPSHOT"))return null;
  float yaw=number(frame,"viewYaw"),forward=.12f+number(frame,"eyeLean");
  var center=new Vector3f(number(frame,"eyeX")-(float)java.lang.Math.cos(yaw)*forward,number(frame,"eyeY"),number(frame,"eyeZ")-(float)java.lang.Math.sin(yaw)*forward);
  var base=new Matrix4f().translation(center).rotate(StereoCamera.viewpointLook(yaw,0)).scale(h.scale());
  var sceneFromHead=new Matrix4f(base).mul(h.pose().matrix());
  var shoulder=base.transformDirection(new Vector3f(0,h.y(),0));
  arms.recenter(BodyTracking.calibration());
  var result=arms.open((List<?>)SnapshotArms.get(slot,"modelData"),()->{try{return new Matrix4f((Matrix4f)place.get(null));}catch(IllegalAccessException e){throw new IllegalStateException(e);}},()->{try{return new Matrix4f((Matrix4f)model.get(null));}catch(IllegalAccessException e){throw new IllegalStateException(e);}},sceneFromHead,state,shoulder);
  long now=System.nanoTime();if(now-lastLog>2_000_000_000L){lastLog=now;System.out.println("[Project Viewpoint VR Rig] captured L="+(state.left().grip()!=null)+" R="+(state.right().grip()!=null)+" | "+RigBridge.status);}
  return result;
 }
}
