package viewpointvr.instrument;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.jar.asm.Opcodes.*;
public final class RigTransform {
 public static final String TARGET="viewpoint/models/ModelCapture";
 public static final String CAPTURE="(Lviewpoint/core/Frame;Lzombie/core/skinnedmodel/model/ModelSlotRenderData;Lzombie/characters/IsoGameCharacter;ZZLviewpoint/models/ModelCapture$Pose;FFFF)Z";
 private final byte[] expected;
 public RigTransform(Path jar)throws Exception{try(var j=new JarFile(jar.toFile());var in=j.getInputStream(j.getJarEntry(TARGET+".class"))){expected=CanonicalClass.encode(in.readAllBytes());}}
 public byte[] transform(byte[] source){
  if(!Arrays.equals(expected,CanonicalClass.encode(source)))throw new IllegalArgumentException("Rig target differs");
  var reader=new ClassReader(source);var writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);int[] hits={0,0,0};
  reader.accept(new ClassVisitor(ASM9,writer){public MethodVisitor visitMethod(int a,String name,String desc,String signature,String[] errors){return new MethodVisitor(ASM9,super.visitMethod(a,name,desc,signature,errors)){
   public void visitFieldInsn(int op,String owner,String field,String d){
    if(op==GETFIELD&&owner.equals("zombie/core/skinnedmodel/model/ModelInstanceRenderData")&&(field.equals("matrixPalette")&&(name.equals("add")||name.equals("palette"))||field.equals("xfrm")&&name.equals("add"))){
     super.visitInsn(DUP);super.visitFieldInsn(op,owner,field,d);String method=field.equals("xfrm")?"attachment":"palette";
     super.visitMethodInsn(INVOKESTATIC,"viewpointvr/rig/RigBridge",method,"(Ljava/lang/Object;"+d+")"+d,false);hits[field.equals("xfrm")?1:0]++;return;
    }
    super.visitFieldInsn(op,owner,field,d);
   }
   public void visitMethodInsn(int op,String owner,String called,String d,boolean itf){super.visitMethodInsn(op,owner,called,d,itf);
    if(name.equals("capture")&&op==INVOKEVIRTUAL&&owner.equals("viewpoint/render/ModelDraws")&&called.equals("slot")){super.visitMethodInsn(INVOKESTATIC,"viewpointvr/rig/RigBridge","prepared","()V",false);hits[2]++;}
   }
  };}},0);
  if(hits[0]!=2||hits[1]!=1||hits[2]!=1)throw new IllegalArgumentException("Rig anchor counts "+Arrays.toString(hits));return writer.toByteArray();
 }
}
