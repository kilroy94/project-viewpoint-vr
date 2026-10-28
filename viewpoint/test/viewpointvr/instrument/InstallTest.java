package viewpointvr.instrument;

import java.lang.instrument.*;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.jar.JarFile;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.jar.asm.Opcodes.*;
import viewpointvr.diagnostic.*;

/** Activation/rollback tests against actual copied classes, without initialization. */
public final class InstallTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    interface Action {void run() throws Exception;}
    static void rejects(Action action)throws Exception{try{action.run();}catch(Exception expected){checks++;return;}throw new AssertionError("Expected rejection");}
    public static void main(String[] args)throws Exception{
        Path mod=Path.of(args[0]);var instrumentation=TestAgent.instrumentation;var loader=InstallTest.class.getClassLoader();
        BinaryPins.verify(Path.of(args[1]),mod,Path.of(args[2]));checks++;
        Class<?> nativeLoader=Class.forName("me.zed_0xff.zombie_buddy.Loader",false,loader);
        var nativeInstrumentation=nativeLoader.getDeclaredField("g_instrumentation");
        check(java.lang.reflect.Modifier.isStatic(nativeInstrumentation.getModifiers())&&nativeInstrumentation.getType()==Instrumentation.class,"Current loader instrumentation bridge metadata");
        try(var loaderJar=new JarFile(args[2])){check("2.3.4".equals(loaderJar.getManifest().getMainAttributes().getValue("Implementation-Version")),"Audited installed ZombieBuddy version");}
        Class<?>[] targets=new Class<?>[Installation.TARGETS.size()];Map<String,byte[]> expected=new HashMap<>();
        try(var jar=new JarFile(mod.toFile())){
            for(int i=0;i<targets.length;i++){
                String name=Installation.TARGETS.get(i);targets[i]=Class.forName(name.replace('/','.'),false,loader);
                try(var gameJar=new JarFile(args[1])){var from=jar.getJarEntry(name+".class")!=null?jar:gameJar;try(var in=from.getInputStream(from.getJarEntry(name+".class"))){expected.put(name,CanonicalClass.encode(in.readAllBytes()));}}
            }
        }
        var visibilityTransform=new VisibilityTransform(mod);
        try(var jar=new JarFile(mod.toFile());var in=jar.getInputStream(jar.getJarEntry("viewpoint/models/CorpseView.class"))){
            byte[] transformed=visibilityTransform.transform("viewpoint/models/CorpseView",in.readAllBytes());
            int[] returns={0},wrapped={0};
            new ClassReader(transformed).accept(new ClassVisitor(ASM9){
                public MethodVisitor visitMethod(int a,String name,String desc,String signature,String[] exceptions){
                    if(!name.equals("sees")||!desc.equals("(FFF)Z"))return null;
                    return new MethodVisitor(ASM9){
                        public void visitInsn(int op){if(op==IRETURN)returns[0]++;}
                        public void visitMethodInsn(int op,String owner,String method,String d,boolean itf){if(owner.equals("viewpointvr/Visibility")&&method.equals("visible")&&d.equals("(Z)Z"))wrapped[0]++;}
                    };
                }
            },0);
            check(returns[0]>0&&returns[0]==wrapped[0],"Every corpse-cone result passes through timed VR visibility");
            rejects(()->visibilityTransform.transform("viewpoint/models/CorpseView",transformed));
        }
        var uiTransform=new UiTransform(mod);
        try(var jar=new JarFile(mod.toFile())){
            for(String name:UiTransform.TARGETS)try(var in=jar.getInputStream(jar.getJarEntry(name+".class"))){
                byte[] source=in.readAllBytes(),patched=uiTransform.transform(name,source);
                rejects(()->uiTransform.transform(name,patched));
                rejects(()->uiTransform.transform(name+"Unknown",source));
            }
        }
        var gameplay=new GameplayTransform(mod,Path.of(args[1]),loader);
        for(String name:GameplayTransform.TARGETS)try(var nativeJar=new JarFile(viewpointvr.input.ControllerInstallation.TARGETS.contains(name)?args[1]:args[0]);var in=nativeJar.getInputStream(nativeJar.getJarEntry(name+".class"))){
            byte[] patched=gameplay.transform(name,in.readAllBytes());rejects(()->gameplay.transform(name,patched));rejects(()->gameplay.transform(name+"Unknown",patched));
        }
        var rig=new RigTransform(mod);
        try(var nativeJar=new JarFile(args[0]);var in=nativeJar.getInputStream(nativeJar.getJarEntry(RigTransform.TARGET+".class"))){byte[] patched=rig.transform(in.readAllBytes());rejects(()->rig.transform(patched));}
        var locked=(Instrumentation)Proxy.newProxyInstance(loader,new Class<?>[]{Instrumentation.class},(p,m,a)->{
            if(m.getName().equals("isModifiableClass"))return false;return m.invoke(instrumentation,a);
        });
        rejects(()->Installation.install(locked,loader,mod,Path.of(args[1])));
        var installed=Installation.install(instrumentation,loader,mod,Path.of(args[1]));check(installed.ready(),"All-target activation");
        instrumentation.retransformClasses(targets);check(installed.ready(),"Compatible repeat retransform stays armed");
        installed.close();check(!installed.ready(),"Explicit removal disarms");
        AtomicBoolean corrupt=new AtomicBoolean(true);
        ClassFileTransformer other=new ClassFileTransformer(){
            public byte[] transform(ClassLoader l,String name,Class<?> type,ProtectionDomain d,byte[] source){
                if(!corrupt.get()||!name.equals("viewpoint/render/TemporalPass"))return null;
                ClassWriter writer=new ClassWriter(0);
                new ClassReader(source).accept(new ClassVisitor(ASM9,writer){
                    public MethodVisitor visitMethod(int a,String n,String desc,String s,String[] e){
                        return new MethodVisitor(ASM9,super.visitMethod(a,n,desc,s,e)){
                            public void visitCode(){super.visitCode();if(n.equals("begin"))super.visitInsn(NOP);}
                        };
                    }
                },0);return writer.toByteArray();
            }
        };
        instrumentation.addTransformer(other,true);
        try{
            rejects(()->Installation.install(instrumentation,loader,mod,Path.of(args[1]))); // Partial acceptance must roll back.
            corrupt.set(false);instrumentation.retransformClasses(targets);
            installed=Installation.install(instrumentation,loader,mod,Path.of(args[1]));check(installed.ready(),"Fresh install after rollback");
            corrupt.set(true);instrumentation.retransformClasses(targets);
            check(!installed.ready()&&installed.failure()!=null,"Incompatible later code permanently disarms capture");
            installed.close();
        }finally{instrumentation.removeTransformer(other);instrumentation.retransformClasses(targets);}
        AtomicInteger restored=new AtomicInteger();
        ClassFileTransformer observer=new ClassFileTransformer(){
            public byte[] transform(ClassLoader l,String n,Class<?> t,ProtectionDomain d,byte[] b){
                if(expected.containsKey(n)&&Arrays.equals(expected.get(n),CanonicalClass.encode(b)))restored.incrementAndGet();return null;
            }
        };
        instrumentation.addTransformer(observer,true);
        try{instrumentation.retransformClasses(targets);}finally{instrumentation.removeTransformer(observer);}
        check(restored.get()==targets.length,"All native classes restored");
        System.out.println("Loader activation/rollback: "+checks+" checks passed on uninitialized copied classes");
    }
}
