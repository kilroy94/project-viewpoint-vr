package viewpointvr.rig;
import java.lang.reflect.Field;
import java.nio.*;
import java.util.*;
import java.util.function.Supplier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import viewpointvr.input.*;
/** Reuses the authored IK solver at Viewpoint's pre-pack snapshot boundary. */
public final class SnapshotArms {
 private final IdentityHashMap<Object,ArmRig> rigs=new IdentityHashMap<>();private long revision=-1;private boolean disabled;private final List<FloatBuffer> buffers=new ArrayList<>();
 public void recenter(long value){if(value!=revision){rigs.values().forEach(ArmRig::recenter);disabled=false;revision=value;}}
 public RigBridge.Scope open(List<?> parts,Supplier<Matrix4f> world,Supplier<Matrix4f> itemWorld,Matrix4f sceneFromHead,ControllerState state,Vector3f shoulderShift){
  return new RigBridge.Scope(){
   final IdentityHashMap<Object,FloatBuffer> palettes=new IdentityHashMap<>();
   final IdentityHashMap<Object,ArmRig.PoseChanges> changes=new IdentityHashMap<>();
   final IdentityHashMap<Object,Matrix4f> attachments=new IdentityHashMap<>();
   final Set<Object> visiting=Collections.newSetFromMap(new IdentityHashMap<>());
   boolean prepared;int bufferIndex;
   private FloatBuffer compute(Object data,FloatBuffer original)throws Throwable{
    if(original==null)return original;
    if(original.position()!=0||original.limit()%16!=0)throw new IllegalArgumentException("Unexpected native palette span");if(palettes.containsKey(data))return palettes.get(data);
     var model=get(data,"model");if((boolean)get(model,"isStatic"))return original;
     Object skin=get(model,"tag");if(skin==null)skin=get(get(model,"mesh"),"skinningData");
     var solver=rigs.get(skin);if(solver==null){solver=create(skin,original.remaining()/16);if(rigs.size()>128)rigs.clear();rigs.put(skin,solver);}
     var modelFromHead=new Matrix4f(world.get()).invert().mul(sceneFromHead);
     var targets=new Matrix4f[2];var poles=new Vector3f[2];
     for(int side=0;side<2;side++){
      var hand=side==0?state.left():state.right();
      if(hand.grip()!=null)targets[side]=new Matrix4f(modelFromHead).mul(hand.grip().matrix()).scale(-1,1,1);
      poles[side]=modelFromHead.transformPosition(new Vector3f(side==0?-.55f:.55f,-.5f,-.1f),new Vector3f());
     }
     if(bufferIndex==buffers.size())buffers.add(null);var output=buffers.get(bufferIndex);
     if(output==null||output.capacity()<original.remaining()){output=ByteBuffer.allocateDirect(original.remaining()*4).order(ByteOrder.nativeOrder()).asFloatBuffer();buffers.set(bufferIndex,output);}bufferIndex++;
     var shift=new Matrix4f(world.get()).invert().transformDirection(new Vector3f(shoulderShift));
     var result=solver.pose(original,targets,poles,output,shift,1.5f);palettes.put(data,output);changes.put(data,result);return output;
   }
   private Matrix4f delta(Object data)throws Throwable{
    if(attachments.containsKey(data))return attachments.get(data);
    if(!visiting.add(data))throw new IllegalArgumentException("Cyclic held-item snapshot");
    Object parent=get(data,"parent");Matrix4f result=null;
    if(parent!=null){
     if((boolean)get(get(parent,"model"),"isStatic"))result=delta(parent);
     else{
      compute(parent,(FloatBuffer)get(parent,"matrixPalette"));var pose=changes.get(parent);
      if(pose!=null){
       Object instance=get(data,"modelInstance"),parentInstance=get(parent,"modelInstance");String bone=(String)get(instance,"parentBoneName");
       Object attachment=parentInstance.getClass().getMethod("getAttachmentById",String.class).invoke(parentInstance,(String)get(instance,"attachmentNameParent"));
       if(attachment==null&&bone!=null)attachment=parentInstance.getClass().getMethod("getAttachmentById",String.class).invoke(parentInstance,bone);
       if(attachment!=null)bone=(String)attachment.getClass().getMethod("getBone").invoke(attachment);
       result=pose.delta(bone);
      }
     }
    }
    visiting.remove(data);attachments.put(data,result);return result;
   }
   public void prepare()throws Throwable{
    if(disabled)return;
    try{
     for(Object data:parts)if(!(boolean)get(get(data,"model"),"isStatic"))compute(data,(FloatBuffer)get(data,"matrixPalette"));
     for(Object data:parts)if((boolean)get(get(data,"model"),"isStatic"))delta(data);
     prepared=true;RigBridge.status="tracked arms + held items";
    }catch(Exception error){disabled=true;close();RigBridge.status="arms disabled until recenter: "+error;System.err.println("[Project Viewpoint VR Rig] "+RigBridge.status);}
   }
   public FloatBuffer palette(Object data,FloatBuffer original){return prepared?palettes.getOrDefault(data,original):original;}
   public Matrix4f attachment(Object data,Matrix4f original){var delta=prepared?attachments.get(data):null;if(delta==null)return original;
    var body=world.get();var item=itemWorld.get();return new Matrix4f(item).invert().mul(body).mul(delta).mul(new Matrix4f(body).invert()).mul(item).mul(new Matrix4f(original).transpose()).transpose();}

   public void close(){prepared=false;palettes.clear();changes.clear();attachments.clear();visiting.clear();}
  };
 }
 @SuppressWarnings("unchecked")
 private static ArmRig create(Object skin,int count)throws ReflectiveOperationException{
  if(skin==null||count<1||count>60)throw new IllegalArgumentException("Unsupported skin");
  var indices=(Map<String,Integer>)get(skin,"boneIndices");var hierarchy=(List<Integer>)get(skin,"skeletonHierarchy");var offsets=(List<?>)get(skin,"boneOffset");
  if(hierarchy.size()<count||offsets.size()<count)throw new IllegalArgumentException("Incomplete skin");
  var names=new String[count];var parents=new int[count];var matrices=new Matrix4f[count];
  for(var e:indices.entrySet())if(e.getValue()>=0&&e.getValue()<count)names[e.getValue()]=e.getKey();
  for(int i=0;i<count;i++){parents[i]=hierarchy.get(i);float[] f=new float[16];for(int c=0;c<4;c++)for(int r=0;r<4;r++)f[c*4+r]=((Number)get(offsets.get(i),"m"+c+r)).floatValue();matrices[i]=new Matrix4f().set(f).transpose();}
  return new ArmRig(names,parents,matrices);
 }
 public static Field field(Class<?> type,String name)throws NoSuchFieldException{for(Class<?> c=type;c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(type+"."+name);}
 public static Object get(Object object,String name)throws ReflectiveOperationException{return field(object.getClass(),name).get(object);}
}
