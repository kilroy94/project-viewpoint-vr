package viewpointvr.instrument;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.jar.asm.Opcodes.*;
import viewpointvr.input.ControllerInstallation;
import viewpointvr.diagnostic.BinaryPins;
/** Exact-binary, consumer-side look hooks and the inherited native controller overlay. */
public final class GameplayTransform {
 public static final List<String> LOOK=List.of("viewpoint/input/FreeLook","viewpoint/input/CrosshairAim","viewpoint/input/Controls");
 public static final List<String> TARGETS=java.util.stream.Stream.concat(LOOK.stream(),ControllerInstallation.TARGETS.stream()).toList();
 private final Map<String,byte[]> expected=new HashMap<>();private final ClassLoader loader;
 public GameplayTransform(Path viewpoint,Path game,ClassLoader loader)throws Exception{
  this.loader=loader;BinaryPins.check(game,Set.of(BinaryPins.GAME));
  for(Path path:List.of(viewpoint,game))try(var jar=new JarFile(path.toFile())){for(String name:TARGETS){var e=jar.getJarEntry(name+".class");if(e!=null)try(var in=jar.getInputStream(e)){expected.put(name,CanonicalClass.encode(in.readAllBytes()));}}}
  if(expected.size()!=TARGETS.size())throw new IllegalArgumentException("Gameplay targets missing");
 }
 public byte[] transform(String name,byte[] source){
  if(!Arrays.equals(expected.get(name),CanonicalClass.encode(source)))throw new IllegalArgumentException("Gameplay target differs: "+name);
  if(ControllerInstallation.TARGETS.contains(name))return ControllerInstallation.transform(name,source,loader);
  return patchLook(name,source);
 }
 public static byte[] patchLook(String name,byte[] source){
  var reader=new ClassReader(source);var writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);int[] hits={0};
  reader.accept(new ClassVisitor(ASM9,writer){public MethodVisitor visitMethod(int access,String method,String desc,String signature,String[] exceptions){
   return new MethodVisitor(ASM9,super.visitMethod(access,method,desc,signature,exceptions)){
    public void visitFieldInsn(int op,String owner,String field,String d){
     super.visitFieldInsn(op,owner,field,d);
     if(name.equals("viewpoint/input/Controls")&&op==GETSTATIC&&owner.equals("viewpoint/input/Look")&&field.equals("pitch")&&d.equals("F")){
      super.visitMethodInsn(INVOKESTATIC,"viewpointvr/input/BodyTracking","pitch","(F)F",false);hits[0]++;
     }
    }
    public void visitInsn(int op){
     if(op==FRETURN&&(name.endsWith("/FreeLook")&&method.equals("yaw")||name.endsWith("/CrosshairAim")&&(method.equals("yaw")||method.equals("pitch")))){
      super.visitMethodInsn(INVOKESTATIC,"viewpointvr/input/BodyTracking",method,"(F)F",false);hits[0]++;
     }
     super.visitInsn(op);
    }
   };
  }},0);
  int expectedHits=switch(name){case "viewpoint/input/FreeLook"->1;case "viewpoint/input/CrosshairAim"->2;case "viewpoint/input/Controls"->1;default->0;};
  if(hits[0]!=expectedHits)throw new IllegalArgumentException("Gameplay anchor count "+name+": "+hits[0]);return writer.toByteArray();
 }
}
