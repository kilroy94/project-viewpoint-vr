package viewpointvr.instrument;

import java.lang.classfile.*;
import java.lang.instrument.*;
import java.nio.file.*;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.*;
import viewpointvr.ViewpointBackend;

public final class StageBinaryTest {
    public static void main(String[] args) throws Exception {
        var transform=StageTransform.fromPinnedJar(Path.of(args[0]));
        var loader=StageBinaryTest.class.getClassLoader();
        new ViewpointBackend.Access(loader); // Resolve the entire backend metadata contract without initialization.
        var verifier=ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        try(var jar=new JarFile(args[0])) {
            for(String name:StageTransform.TARGETS) {
                byte[] source;
                try(var in=jar.getInputStream(jar.getJarEntry(name+".class"))) { source=in.readAllBytes(); }
                byte[] result=transform.transform(name,source);
                var errors=verifier.verify(result);
                if(!errors.isEmpty()) throw new AssertionError(errors);
                try { transform.transform(name,result); throw new AssertionError("Patched input accepted"); }
                catch(IllegalArgumentException expected) {}
            }
        }
        Class<?>[] targets=new Class<?>[StageTransform.TARGETS.size()];
        var instrumentation=TestAgent.instrumentation;
        for(int i=0;i<targets.length;i++) {
            targets[i]=Class.forName(StageTransform.TARGETS.get(i).replace('/','.'),false,loader);
            if(targets[i].getClassLoader()!=loader || !instrumentation.isModifiableClass(targets[i])) throw new AssertionError("Target ownership");
        }
        List<Throwable> failures=new ArrayList<>(); int[] accepted={0};
        ClassFileTransformer hook=new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader owner,String name,Class<?> target,ProtectionDomain domain,byte[] bytes) {
                if(!StageTransform.TARGETS.contains(name)) return null;
                try {
                    if(owner!=loader || target==null) throw new AssertionError("Wrong target");
                    byte[] result=transform.transform(name,bytes);
                    if(!verifier.verify(result).isEmpty()) throw new AssertionError("Invalid output");
                    accepted[0]++; return result;
                } catch(Throwable error) { failures.add(error); return null; }
            }
        };
        instrumentation.addTransformer(hook,true);
        try {
            instrumentation.retransformClasses(targets);
            instrumentation.retransformClasses(targets);
            if(!failures.isEmpty() || accepted[0]!=6) throw new AssertionError("Stage retransformation: "+failures);
        } finally { instrumentation.removeTransformer(hook); instrumentation.retransformClasses(targets); }
        System.out.println("Native stages: backend metadata plus 3 copied classes verified/retransformed twice; no initialization");
    }
}
