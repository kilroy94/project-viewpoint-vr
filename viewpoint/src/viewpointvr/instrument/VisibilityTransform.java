package viewpointvr.instrument;

import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.jar.asm.Opcodes.*;
import viewpointvr.diagnostic.BinaryPins;

/** Pinned return/field-read wrappers; no native schema change or game initialization. */
public final class VisibilityTransform {
    public static final List<String> TARGETS=List.of("viewpoint/world/ChunkWalk","viewpoint/models/Characters","viewpoint/models/ModelCull","viewpoint/visibility/Rooms","viewpoint/models/CorpseView");
    private final Map<String,byte[]> expected=new HashMap<>();
    public VisibilityTransform(Path jar) throws Exception {
        BinaryPins.check(jar,Set.of(BinaryPins.VIEWPOINT));
        try(var input=new JarFile(jar.toFile())) {for(String target:TARGETS)try(var stream=input.getInputStream(input.getJarEntry(target+".class"))){expected.put(target,CanonicalClass.encode(stream.readAllBytes()));}}
    }
    public byte[] transform(String name,byte[] source) {
        if(!Arrays.equals(expected.get(name),CanonicalClass.encode(source)))throw new IllegalArgumentException("Visibility target differs: "+name);
        var reader=new ClassReader(source);var writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);int[] hits={0},rig={0};
        reader.accept(new ClassVisitor(ASM9,writer){
            public MethodVisitor visitMethod(int access,String method,String desc,String signature,String[] exceptions) {
                return new MethodVisitor(ASM9,super.visitMethod(access,method,desc,signature,exceptions)) {
                    public void visitMethodInsn(int op,String owner,String called,String descriptor,boolean itf){
                        if(name.endsWith("/Characters")&&method.equals("snapshot")&&op==INVOKESTATIC&&owner.equals(RigTransform.TARGET)&&called.equals("capture")&&descriptor.equals(RigTransform.CAPTURE)){
                            super.visitLdcInsn(new Handle(H_INVOKESTATIC,owner,called,descriptor,false));
                            super.visitMethodInsn(INVOKESTATIC,"viewpointvr/rig/RigBridge","capture","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ZZLjava/lang/Object;FFFFLjava/lang/invoke/MethodHandle;)Z",false);rig[0]++;return;
                        }
                        super.visitMethodInsn(op,owner,called,descriptor,itf);
                    }
                    public void visitInsn(int op) {
                        String policy=null;
                        if(name.endsWith("/CorpseView")&&method.equals("sees")&&desc.equals("(FFF)Z"))policy="visible";
                        if(name.endsWith("/ChunkWalk")&&method.equals("inView")&&desc.equals("(FFFFFF)Z"))policy="visible";
                        if(name.endsWith("/Characters")&&method.equals("behind")&&desc.equals("(Lzombie/iso/IsoMovingObject;FFFF)Z"))policy="hidden";
                        if(name.endsWith("/ModelCull")&&method.equals("wanted")&&desc.equals("(Lzombie/iso/IsoMovingObject;Lzombie/characters/IsoPlayer;)Z"))policy="visible";
                        if(op==IRETURN&&policy!=null){hits[0]++;super.visitMethodInsn(INVOKESTATIC,"viewpointvr/Visibility",policy,"(Z)Z",false);}
                        super.visitInsn(op);
                    }
                    public void visitFieldInsn(int op,String owner,String field,String desc) {
                        super.visitFieldInsn(op,owner,field,desc);
                        if(name.endsWith("/Rooms")&&op==GETSTATIC&&owner.equals(name)&&field.equals("hiding")&&desc.equals("Z")) {
                            hits[0]++;super.visitMethodInsn(INVOKESTATIC,"viewpointvr/Visibility","hidden","(Z)Z",false);
                        }
                    }
                };
            }
        },0);
        if(hits[0]==0)throw new IllegalArgumentException("No visibility anchors: "+name);
        if(name.endsWith("/Characters")&&rig[0]!=1)throw new IllegalArgumentException("Rig capture anchor count: "+rig[0]);
        return writer.toByteArray();
    }
}
