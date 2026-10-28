package viewpointvr.input;

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.*;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.jar.asm.Opcodes.*;

/** Exact-version, schema-preserving constructor/registry/poll hooks. No GLFW patches. */
public final class ControllerInstallation implements ClassFileTransformer {
    public static final List<String> TARGETS=List.of("org/lwjglx/input/Controller","org/lwjglx/input/Controllers","zombie/core/input/Input");
    private static final String HOOK="viewpointvr/input/ControllerBridge", C=TARGETS.get(0);
    private final ClassLoader loader;
    private final Set<String> accepted=new HashSet<>();
    private String failure;
    private ControllerInstallation(ClassLoader loader) { this.loader=loader; }
    public static void install(Instrumentation inst,ClassLoader loader) throws Exception {
        var hook=new ControllerInstallation(loader); Class<?>[] types=new Class<?>[TARGETS.size()];
        for(int i=0;i<types.length;i++) {
            types[i]=Class.forName(TARGETS.get(i).replace('/','.'),false,loader);
            if(types[i].getClassLoader()!=loader||!inst.isModifiableClass(types[i])) throw new IllegalStateException("Controller class ownership");
        }
        inst.addTransformer(hook,true);
        try {
            inst.retransformClasses(types);
            if(hook.failure!=null||hook.accepted.size()!=types.length) throw new IllegalStateException("Controller hooks incomplete: "+hook.failure);
            ControllerBridge.installed=true;
        } catch(Throwable ex) {
            ControllerBridge.installed=false; inst.removeTransformer(hook);
            try { inst.retransformClasses(types); } catch(Throwable rollback) { ex.addSuppressed(rollback); }
            throw new IllegalStateException("Controller bridge disabled",ex);
        }
    }
    @Override public synchronized byte[] transform(ClassLoader actual,String name,Class<?> type,ProtectionDomain domain,byte[] bytes) {
        if(!TARGETS.contains(name)) return null;
        try {
            if(actual!=loader) throw new IllegalStateException("Controller loader mismatch");
            byte[] result=transform(name,bytes,loader); accepted.add(name); return result;
        } catch(Throwable ex) { failure=ex.toString(); ControllerBridge.installed=false; return null; }
    }
    public static byte[] transform(String name,byte[] bytes,ClassLoader loader) {
        var writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS) {
            @Override protected ClassLoader getClassLoader() { return loader; }
        };
        Set<String> found=new HashSet<>();
        new ClassReader(bytes).accept(new ClassVisitor(ASM9,writer) {
            @Override public MethodVisitor visitMethod(int access,String method,String desc,String signature,String[] exceptions) {
                var original=super.visitMethod(access,method,desc,signature,exceptions);
                String kind=name.equals(C)&&method.equals("<init>")&&desc.equals("(I)V")?"ctor":
                    name.equals(TARGETS.get(1))&&method.equals("getController")&&desc.equals("(I)L"+C+";")?"select":
                    name.equals(TARGETS.get(1))&&method.equals("poll")&&desc.equals("([Lorg/lwjglx/input/GamepadState;)V")?"poll":
                    name.equals(TARGETS.get(2))&&method.equals("updateGameThread")&&desc.equals("()V")?"consume":null;
                if(kind==null) return original;
                if(!found.add(kind)) throw new IllegalStateException("Duplicate controller hook");
                return new MethodVisitor(ASM9,original) {
                    int superCalls;
                    private void call(String method,String desc) { super.visitMethodInsn(INVOKESTATIC,HOOK,method,desc,false); }
                    private void field(String f,String d) { super.visitFieldInsn(PUTFIELD,C,f,d); }
                    private void string(String f,String value) { super.visitVarInsn(ALOAD,0); super.visitLdcInsn(value); field(f,"Ljava/lang/String;"); }
                    private void number(String f,int value,String desc) { super.visitVarInsn(ALOAD,0); super.visitLdcInsn(value); field(f,desc); }
                    @Override public void visitMethodInsn(int op,String owner,String method,String descriptor,boolean iface) {
                        if(owner.equals(HOOK)) throw new IllegalStateException("Controller hook already present");
                        super.visitMethodInsn(op,owner,method,descriptor,iface);
                        if(kind.equals("ctor")&&op==INVOKESPECIAL&&owner.equals("java/lang/Object")&&method.equals("<init>")) {
                            superCalls++;
                            call("constructing","()Z"); Label normal=new Label(); super.visitJumpInsn(IFEQ,normal);
                            super.visitVarInsn(ALOAD,0); super.visitVarInsn(ILOAD,1); field("id","I");
                            string("joystickName","Viewpoint VR Gamepad"); string("gamepadName","Viewpoint VR Gamepad"); string("guid",ControllerBridge.GUID);
                            number("isGamepad",1,"Z"); number("axisCount",6,"I"); number("buttonsCount",15,"I"); number("hatCount",1,"I");
                            super.visitVarInsn(ALOAD,0); super.visitIntInsn(BIPUSH,6); super.visitIntInsn(NEWARRAY,T_FLOAT); field("deadZone","[F");
                            super.visitVarInsn(ALOAD,0); super.visitFieldInsn(GETFIELD,C,"deadZone","[F"); super.visitLdcInsn(.2f);
                            super.visitMethodInsn(INVOKESTATIC,"java/util/Arrays","fill","([FF)V",false);
                            super.visitInsn(RETURN); super.visitLabel(normal);
                        }
                    }
                    @Override public void visitInsn(int op) {
                        if(kind.equals("select")&&op==ARETURN) { super.visitVarInsn(ILOAD,0); call("select","(L"+C+";I)L"+C+";"); }
                        if(kind.equals("poll")&&op==RETURN) { super.visitVarInsn(ALOAD,0); call("poll","([Lorg/lwjglx/input/GamepadState;)V"); }
                        if(kind.equals("consume")&&op==RETURN) { super.visitVarInsn(ALOAD,0); call("consumed","(Lzombie/core/input/Input;)V"); }
                        super.visitInsn(op);
                    }
                    @Override public void visitEnd() { if(kind.equals("ctor")&&superCalls!=1) throw new IllegalStateException("Controller constructor shape"); super.visitEnd(); }
                };
            }
        },ClassReader.EXPAND_FRAMES);
        int expected=name.equals(TARGETS.get(1))?2:1;
        if(!TARGETS.contains(name)||found.size()!=expected) throw new IllegalStateException("Controller method shape: "+found);
        byte[] result=writer.toByteArray();
        var cf=java.lang.classfile.ClassFile.of(java.lang.classfile.ClassFile.ClassHierarchyResolverOption.of(java.lang.classfile.ClassHierarchyResolver.ofResourceParsing(loader)));
        if(!cf.verify(result).isEmpty()) throw new IllegalStateException("Controller verification failed");
        return result;
    }
}
