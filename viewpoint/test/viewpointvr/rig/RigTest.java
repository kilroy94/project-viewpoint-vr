package viewpointvr.rig;
import java.nio.*;import java.util.*;import java.lang.invoke.*;
import org.joml.Matrix4f;import org.joml.Vector3f;
import viewpointvr.input.*;import viewpointvr.xr.XrCamera.Pose;
/** Authored skeletal snapshots and real bundled math; no game/mod class initialization. */
public final class RigTest {
 static int checks,calls;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}static void near(Matrix4f a,Matrix4f b,String why){float[] x=a.get(new float[16]),y=b.get(new float[16]);for(int i=0;i<16;i++)check(Math.abs(x[i]-y[i])<.0002,why+" "+i);}
 public static final class Skin {public final Map<String,Integer> boneIndices=new HashMap<>();public final List<Integer> skeletonHierarchy=new ArrayList<>();public final List<org.lwjgl.util.vector.Matrix4f> boneOffset=new ArrayList<>();}
 public static final class Model {public boolean isStatic;public Skin tag;}
 public static final class Instance {public String parentBoneName,attachmentNameParent;public Object getAttachmentById(String id){return null;}}
 public static final class Data {public Model model=new Model();public FloatBuffer matrixPalette;public Instance modelInstance=new Instance();public Data parent;public final Matrix4f xfrm=new Matrix4f();}
 static final String[] NAMES={"Bip01_Spine","Bip01_L_UpperArm","Bip01_L_Forearm","Bip01_L_Hand","Bip01_L_Finger1","Bip01_R_UpperArm","Bip01_R_Forearm","Bip01_R_Hand","Bip01_R_Finger1"};
 static final int[] PARENTS={-1,0,1,2,3,0,5,6,7};
 static Skin skin(){var s=new Skin();for(int i=0;i<NAMES.length;i++){s.boneIndices.put(NAMES[i],i);s.skeletonHierarchy.add(PARENTS[i]);s.boneOffset.add(new org.lwjgl.util.vector.Matrix4f());}return s;}
 static FloatBuffer palette(){var b=ByteBuffer.allocateDirect(NAMES.length*64).order(ByteOrder.nativeOrder()).asFloatBuffer();float[] x={0,-.2f,-.45f,-.7f,-.75f,.2f,.45f,.7f,.75f};for(int i=0;i<NAMES.length;i++){new Matrix4f().translation(x[i],i==0?0:.4f,0).transpose().get(b);b.position(b.position()+16);}return b.flip();}
 static Data data(Skin s){var d=new Data();d.model.tag=s;d.matrixPalette=palette();return d;}
 static Data item(Data parent,String bone){var d=new Data();d.model.isStatic=true;d.parent=parent;d.modelInstance.parentBoneName=bone;d.xfrm.translation(.1f,.2f,.3f).rotateY(.2f);return d;}
 static ControllerState state(boolean left,boolean right){var lp=new Pose(-.55f,.15f,-.15f,0,0,0,1);var rp=new Pose(.55f,.15f,-.15f,0,0,0,1);return new ControllerState(1,System.nanoTime(),true,new ControllerState.Hand(left?lp:null,left?lp:null,0,0,0,false),new ControllerState.Hand(right?rp:null,right?rp:null,0,0,0,false));}
 public static boolean nativeCapture(Object frame,Object slot,Object player,boolean local,boolean own,Object kind,float x,float y,float z,float angle)throws Throwable{calls++;RigBridge.prepared();var d=(Data)slot;check(RigBridge.palette(d,d.matrixPalette)!=d.matrixPalette,"bridge scope active during native packing");if(x==1)throw new IllegalStateException("native capture failed");return true;}
 public static void main(String[] args)throws Throwable{
  var skin=skin();var body=data(skin);var clothing=data(skin);var weapon=item(body,"Bip01_R_Hand");var nested=item(weapon,"nested");var original=body.matrixPalette;float[] bytes=new float[original.remaining()];original.duplicate().get(bytes);var originalItem=new Matrix4f(weapon.xfrm);
  var world=new Matrix4f().scaling(1,1,-1);var itemWorld=new Matrix4f(world).translate(.2f,0,.1f).rotateY(.2f);var arms=new SnapshotArms();arms.recenter(1);
  try(var scope=arms.open(List.of(nested,weapon,body,clothing),()->world,()->itemWorld,new Matrix4f(),state(false,true),new Vector3f())){
   scope.prepare();var posed=scope.palette(body,original);var dressed=scope.palette(clothing,clothing.matrixPalette);
   check(posed!=original&&posed!=dressed&&dressed!=clothing.matrixPalette,"separate body/clothing outputs");
   for(int i=0;i<bytes.length;i++){check(Float.floatToIntBits(original.get(i))==Float.floatToIntBits(bytes[i]),"source bytes untouched");if(i<5*16)check(Float.floatToIntBits(posed.get(i))==Float.floatToIntBits(bytes[i]),"opposite arm/spine untouched");}
   check(posed.get(7*16+3)!=original.get(7*16+3)||posed.get(7*16+7)!=original.get(7*16+7),"tracked wrist moved");
   check(!scope.attachment(weapon,weapon.xfrm).equals(weapon.xfrm),"held item follows wrist delta");check(!scope.attachment(nested,nested.xfrm).equals(nested.xfrm),"nested item inherits delta");
   var in=original.duplicate();in.position(7*16);var oldHand=new Matrix4f(in).transpose();var out=posed.duplicate();out.position(7*16);var newHand=new Matrix4f(out).transpose();var delta=new Matrix4f(newHand).mul(new Matrix4f(oldHand).invert());
   for(Data held:List.of(weapon,nested)){var expected=new Matrix4f(itemWorld).invert().mul(world).mul(delta).mul(new Matrix4f(world).invert()).mul(itemWorld).mul(new Matrix4f(held.xfrm).transpose()).transpose();near(scope.attachment(held,held.xfrm),expected,"one wrist delta through native item world");}near(weapon.xfrm,originalItem,"borrowed attachment unchanged");
   check(scope.palette(new Object(),original)==original,"unrelated native data unchanged");
  }
  check(body.matrixPalette==original,"native palette reference unchanged");
  var broken=data(skin());broken.model.tag.boneIndices.clear();
  try(var scope=arms.open(List.of(body,broken,weapon),()->world,()->world,new Matrix4f(),state(false,true),new Vector3f())){scope.prepare();check(scope.palette(body,original)==original&&scope.attachment(weapon,weapon.xfrm)==weapon.xfrm,"failed preflight publishes no partial overrides");}
  arms.recenter(2);try(var scope=arms.open(List.of(body,weapon),()->world,()->world,new Matrix4f(),state(false,true),new Vector3f())){scope.prepare();check(scope.palette(body,original)!=original,"recenter permits recovery");}
  RigBridge.install((f,s,p,l,o,k,x,y,z,a)->arms.open(List.of(body),()->world,()->world,new Matrix4f(),state(false,true),new Vector3f()));
  var method=MethodHandles.lookup().findStatic(RigTest.class,"nativeCapture",MethodType.methodType(boolean.class,Object.class,Object.class,Object.class,boolean.class,boolean.class,Object.class,float.class,float.class,float.class,float.class));
  check(RigBridge.capture(new Object(),body,new Object(),true,true,"SNAPSHOT",0,0,0,0,method),"native capture return preserved");check(calls==1,"native capture once");check(RigBridge.palette(body,original)==original,"scope removed after success");
  try{RigBridge.capture(new Object(),body,new Object(),true,true,"SNAPSHOT",1,0,0,0,method);throw new AssertionError("expected failure");}catch(IllegalStateException expected){check(expected.getMessage().equals("native capture failed"),"native exception preserved");}check(calls==2&&RigBridge.palette(body,original)==original,"no replay and scope cleanup on failure");
  RigBridge.install(null);System.out.println("Tracked rig snapshots: "+checks+" checks passed; authored snapshots only");
 }
}
