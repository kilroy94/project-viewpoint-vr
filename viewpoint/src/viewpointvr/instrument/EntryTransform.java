package viewpointvr.instrument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.jar.JarInputStream;
import java.io.ByteArrayInputStream;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.jar.asm.Opcodes.*;

/** Offline transform; callers must separately establish loader ownership and verify output. */
public final class EntryTransform {
    public static final String TARGET = "viewpoint/SceneDrawer";
    private static final String PIN = viewpointvr.diagnostic.BinaryPins.VIEWPOINT;
    private final byte[] expected;

    private EntryTransform(byte[] original) { expected = CanonicalClass.encode(original); }

    public static EntryTransform fromPinnedJar(Path jar) throws IOException {
        byte[] bytes = Files.readAllBytes(jar);
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!hash.equals(PIN)) throw new IllegalArgumentException("Unsupported Viewpoint SHA-256: " + hash);
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        try (var input = new JarInputStream(new ByteArrayInputStream(bytes))) {
            for (var entry = input.getNextJarEntry(); entry != null; entry = input.getNextJarEntry())
                if (entry.getName().equals(TARGET + ".class")) return new EntryTransform(input.readAllBytes());
        }
        throw new IOException("Pinned JAR has no SceneDrawer");
    }

    public byte[] transform(String name, byte[] source) {
        if (!name.equals(TARGET)) throw new IllegalArgumentException("Unexpected target: " + name);
        if (!Arrays.equals(expected, CanonicalClass.encode(source)))
            throw new IllegalArgumentException("Loaded SceneDrawer differs from the pinned executable/schema");
        return patch(source);
    }

    // Package-private for synthetic fixtures. Production callers must pass the pinned gate above.
    static byte[] patch(byte[] source) {
        ClassReader reader = new ClassReader(source);
        if (!reader.getClassName().equals(TARGET)) throw new IllegalArgumentException("Wrong class");
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        int[] count = new int[3];
        reader.accept(new ClassVisitor(ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                       String signature, String[] exceptions) {
                MethodVisitor output = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (name.equals("drawFrame") && descriptor.equals("()V") && (access & ACC_PRIVATE) != 0
                        && (access & ACC_STATIC) == 0) count[0]++;
                if (!name.equals("render") || !descriptor.equals("()V") || (access & ACC_STATIC) != 0) return output;
                count[1]++;
                return new MethodVisitor(ASM9, output) {
                    @Override public void visitInvokeDynamicInsn(String method,String desc,Handle bootstrap,Object... args){
                        if(method.equals("run")&&desc.equals("(Lviewpoint/SceneDrawer;)Ljava/lang/Runnable;")&&args.length==3
                                &&args[1] instanceof Handle body&&body.getOwner().equals(TARGET)&&body.getName().equals("drawFrame")&&body.getDesc().equals("()V")&&body.getTag()==H_INVOKEVIRTUAL){
                            count[2]++;super.visitLdcInsn(body);
                            super.visitMethodInsn(INVOKESTATIC,"viewpointvr/FrameBoundary","runnable","(Ljava/lang/Object;Ljava/lang/invoke/MethodHandle;)Ljava/lang/Runnable;",false);
                        }else super.visitInvokeDynamicInsn(method,desc,bootstrap,args);
                    }
                };
            }
        }, 0);
        if (!Arrays.equals(count, new int[]{1,1,1}))
            throw new IllegalArgumentException("Expected one private body, render method and entry call: " + Arrays.toString(count));
        return writer.toByteArray();
    }
}
