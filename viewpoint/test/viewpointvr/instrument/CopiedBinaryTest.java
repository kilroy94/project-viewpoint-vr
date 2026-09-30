package viewpointvr.instrument;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.jar.asm.Opcodes.*;

/** Define, verify and retransform actual copied code; never initialize or execute it. */
public final class CopiedBinaryTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void rejects(Runnable work) {
        try { work.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Expected unsupported class rejection");
    }
    private static byte[] withoutRender(byte[] source) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(source).accept(new ClassVisitor(ASM9, writer) {
            @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e) {
                return n.equals("render") && d.equals("()V") ? null : super.visitMethod(a,n,d,s,e);
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return CanonicalClass.encode(writer.toByteArray());
    }
    public static void main(String[] args) throws Exception {
        EntryTransform transform = EntryTransform.fromPinnedJar(Path.of(args[0]));
        byte[] source;
        try (JarFile jar = new JarFile(args[0])) {
            try (var in = jar.getInputStream(jar.getJarEntry(EntryTransform.TARGET + ".class"))) { source = in.readAllBytes(); }
        }
        byte[] output = transform.transform(EntryTransform.TARGET, source);
        check(Arrays.equals(withoutRender(source), withoutRender(output)), "All fields and non-render methods remain unchanged");
        Path unsupported = Files.createTempFile(Path.of("."), "unsupported-", ".jar");
        try {
            Files.writeString(unsupported,"unrecognized dependency");
            try { EntryTransform.fromPinnedJar(unsupported); throw new AssertionError("Expected unknown JAR rejection"); }
            catch (IllegalArgumentException expected) { checks++; }
        } finally { Files.delete(unsupported); }
        ClassLoader loader = CopiedBinaryTest.class.getClassLoader();
        var verifier = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
        check(verifier.verify(output).isEmpty(), "JDK verifier accepts rewritten copied bytecode");
        rejects(() -> transform.transform("unexpected/Class", source));
        rejects(() -> transform.transform(EntryTransform.TARGET, output));
        // Retain method signatures but change an executable instruction: the pin must reject it.
        ClassWriter changed = new ClassWriter(0);
        new ClassReader(source).accept(new ClassVisitor(ASM9, changed) {
            @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e) {
                return new MethodVisitor(ASM9, super.visitMethod(a,n,d,s,e)) {
                    @Override public void visitCode() { super.visitCode(); if (n.equals("drawFrame")) super.visitInsn(NOP); }
                };
            }
        }, 0);
        rejects(() -> transform.transform(EntryTransform.TARGET, changed.toByteArray()));
        // Method/constant-pool ordering and debug information are intentionally not executable differences.
        ClassWriter reordered = new ClassWriter(0);
        new ClassReader(source).accept(reordered, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        check(transform.transform(EntryTransform.TARGET, reordered.toByteArray()).length > 0, "Normalized comparison accepts equivalent encoding");

        var instrumentation = TestAgent.instrumentation;
        check(instrumentation != null && instrumentation.isRetransformClassesSupported(), "Test instrumentation available");
        Class<?> target = Class.forName("viewpoint.SceneDrawer", false, loader);
        check(target.getClassLoader() == loader && instrumentation.isModifiableClass(target), "Expected loader and modifiable copied target");
        target.getDeclaredMethod("render"); target.getDeclaredMethod("drawFrame");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicInteger accepted = new AtomicInteger();
        ClassFileTransformer hook = new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader owner,String name,Class<?> type,ProtectionDomain domain,byte[] bytes) {
                if (!EntryTransform.TARGET.equals(name)) return null;
                try {
                    check(owner == loader && type == target, "Retransformation target ownership");
                    byte[] result = transform.transform(name,bytes);
                    check(verifier.verify(result).isEmpty(), "Retransformed bytes verify");
                    accepted.incrementAndGet(); return result;
                } catch (Throwable error) { failure.set(error); return null; }
            }
        };
        instrumentation.addTransformer(hook, true);
        try {
            instrumentation.retransformClasses(target);
            check(failure.get() == null && accepted.get() == 1, "Actual copied target retransform accepted: " + failure.get());
            instrumentation.retransformClasses(target);
            check(failure.get() == null && accepted.get() == 2, "Repeated retransformation receives original bytes");
        } finally {
            check(instrumentation.removeTransformer(hook), "Test transform removed");
            instrumentation.retransformClasses(target);
        }
        AtomicInteger restored = new AtomicInteger();
        ClassFileTransformer observer = new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader owner,String name,Class<?> type,ProtectionDomain domain,byte[] bytes) {
                if (EntryTransform.TARGET.equals(name)) {
                    if (Arrays.equals(CanonicalClass.encode(source),CanonicalClass.encode(bytes))) restored.incrementAndGet();
                }
                return null;
            }
        };
        instrumentation.addTransformer(observer,true);
        try { instrumentation.retransformClasses(target); }
        finally { instrumentation.removeTransformer(observer); }
        check(restored.get() == 1, "Native executable restored after test removal");
        System.out.println("Copied binary: " + checks + " checks passed; SceneDrawer verified/retransformed without initialization");
    }
}
