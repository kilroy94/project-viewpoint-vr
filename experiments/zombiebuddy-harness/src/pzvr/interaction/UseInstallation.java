package pzvr.interaction;

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.*;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.jar.asm.Opcodes.*;

/** Scoped target replacement; ordinary E selection and traversal retain their native implementations. */
public final class UseInstallation implements ClassFileTransformer {
    public static final List<String> TARGETS=List.of("com/pavelvoronin/pz3d/Main","com/pavelvoronin/pz3d/Interaction","com/pavelvoronin/pz3d/Traversal");
    private final ClassLoader loader;private final Set<String> accepted=new HashSet<>();private Throwable failure;
    private UseInstallation(ClassLoader loader){this.loader=loader;}
    public static void install(Instrumentation inst,ClassLoader loader)throws Exception{
        var hook=new UseInstallation(loader);Class<?>[] types=new Class<?>[TARGETS.size()];
        for(int i=0;i<types.length;i++){types[i]=Class.forName(TARGETS.get(i).replace('/','.'),false,loader);if(types[i].getClassLoader()!=loader||!inst.isModifiableClass(types[i]))throw new IllegalStateException("Use target ownership");}
        inst.addTransformer(hook,true);
        try{inst.retransformClasses(types);if(hook.failure!=null||hook.accepted.size()!=types.length)throw new IllegalStateException("Use hooks incomplete",hook.failure);HandUse.installed=true;}
        catch(Throwable e){HandUse.installed=false;inst.removeTransformer(hook);inst.retransformClasses(types);throw new IllegalStateException("Hand use disabled",e);}
    }
    @Override public byte[] transform(ClassLoader actual,String name,Class<?> type,ProtectionDomain domain,byte[] bytes){
        if(!TARGETS.contains(name))return null;
        try{if(actual!=loader)throw new IllegalStateException("Use loader mismatch");byte[] result=transform(name,bytes,loader);accepted.add(name);return result;}
        catch(Throwable e){failure=e;HandUse.installed=false;return null;}
    }
    public static byte[] transform(String name,byte[] bytes,ClassLoader loader){
        var writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS){@Override protected ClassLoader getClassLoader(){return loader;}};
        Set<String> found=new HashSet<>();String owner="pzvr/interaction/HandUse",pz="com/pavelvoronin/pz3d/";
        new ClassReader(bytes).accept(new ClassVisitor(ASM9,writer){
            @Override public MethodVisitor visitMethod(int access,String method,String desc,String sig,String[] ex){
                var target=super.visitMethod(access,method,desc,sig,ex);int kind=-1;
                if(name.equals(TARGETS.get(0))&&method.equals("tick")&&desc.equals("()V"))kind=0;
                if(name.equals(TARGETS.get(1))&&method.equals("choose")&&desc.equals("(Lzombie/characters/IsoPlayer;L"+pz+"Controller;L"+pz+"WorldMirror;)L"+pz+"Interaction$Choice;"))kind=1;
                if(name.equals(TARGETS.get(2))&&method.equals("aimed")&&desc.equals("(L"+pz+"Controller;L"+pz+"WorldMirror;)L"+pz+"Traversal$Edge;"))kind=2;
                if(kind<0)return target;
                if(!found.add(method))throw new IllegalStateException("Duplicate use method");final int k=kind;
                return new MethodVisitor(ASM9,target){
                    @Override public void visitCode(){super.visitCode();
                        if(k==0)super.visitMethodInsn(INVOKESTATIC,owner,"tick","()V",false);
                        else if(k==1){
                            super.visitMethodInsn(INVOKESTATIC,owner,"overrideChoice","()Ljava/lang/Object;",false);super.visitInsn(DUP);
                            Label ordinary=new Label();super.visitJumpInsn(IFNULL,ordinary);
                            super.visitTypeInsn(CHECKCAST,pz+"Interaction$Choice");super.visitInsn(ARETURN);super.visitLabel(ordinary);super.visitInsn(POP);
                        }else{
                            super.visitMethodInsn(INVOKESTATIC,owner,"selecting","()Z",false);Label ordinary=new Label();super.visitJumpInsn(IFEQ,ordinary);
                            super.visitInsn(ACONST_NULL);super.visitInsn(ARETURN);super.visitLabel(ordinary);
                        }
                    }
                    @Override public void visitMethodInsn(int op,String o,String m,String d,boolean itf){if(o.equals(owner))throw new IllegalStateException("Use hooks already installed");super.visitMethodInsn(op,o,m,d,itf);}
                };
            }
        },ClassReader.EXPAND_FRAMES);
        if(!TARGETS.contains(name)||found.size()!=1)throw new IllegalStateException("Use shape: "+name+found);
        byte[] result=writer.toByteArray();
        var verifier=java.lang.classfile.ClassFile.of(java.lang.classfile.ClassFile.ClassHierarchyResolverOption.of(java.lang.classfile.ClassHierarchyResolver.ofResourceParsing(loader)));
        if(!verifier.verify(result).isEmpty())throw new IllegalStateException("Use bytecode verification");return result;
    }
}
