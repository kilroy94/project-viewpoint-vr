package viewpointvr.instrument;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.TreeMap;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.jar.asm.Opcodes.*;

/** Reused from the inherited PZ3D Installation: normalize JVM pool/method reordering. */
final class CanonicalClass {
    static byte[] encode(byte[] bytes) {
        ClassWriter writer = new ClassWriter(0);
        Map<String, ClassWriter> methods = new TreeMap<>();
        new ClassReader(bytes).accept(new ClassVisitor(ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                        String signature, String[] exceptions) {
                ClassWriter method = new ClassWriter(0);
                method.visit(V17, ACC_PUBLIC, "Canonical", null, "java/lang/Object", null);
                if (methods.put(name + descriptor, method) != null)
                    throw new IllegalArgumentException("Duplicate method");
                return method.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        try {
            var output = new ByteArrayOutputStream();
            var data = new DataOutputStream(output);
            byte[] metadata = writer.toByteArray();
            data.writeInt(metadata.length); data.write(metadata);
            for (ClassWriter method : methods.values()) {
                method.visitEnd();
                byte[] encoded = method.toByteArray();
                data.writeInt(encoded.length); data.write(encoded);
            }
            return output.toByteArray();
        } catch (IOException impossible) { throw new UncheckedIOException(impossible); }
    }
    private CanonicalClass() {}
}
