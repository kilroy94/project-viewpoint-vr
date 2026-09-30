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
    private static final String PIN = "8e2aa52087c9c111c09f28e8ee1f8f50fc8d9132c20c533a05305e134a7d695c";
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
                    @Override public void visitMethodInsn(int opcode, String owner, String method,
                                                          String desc, boolean itf) {
                        if (owner.equals(TARGET) && method.equals("drawFrame") && desc.equals("()V")
                                && opcode == INVOKEVIRTUAL && !itf) {
                            count[2]++;
                            // Receiver is already on the stack. Resolve private handle in the caller's
                            // own lookup context; no reflective access or added fields/methods required.
                            super.visitLdcInsn(new Handle(H_INVOKEVIRTUAL, TARGET, "drawFrame", "()V", false));
                            super.visitMethodInsn(INVOKESTATIC, "viewpointvr/FrameBoundary", "draw",
                                    "(Ljava/lang/Object;Ljava/lang/invoke/MethodHandle;)V", false);
                        } else super.visitMethodInsn(opcode, owner, method, desc, itf);
                    }
                };
            }
        }, 0);
        if (!Arrays.equals(count, new int[]{1,1,1}))
            throw new IllegalArgumentException("Expected one private body, render method and entry call: " + Arrays.toString(count));
        return writer.toByteArray();
    }
}
