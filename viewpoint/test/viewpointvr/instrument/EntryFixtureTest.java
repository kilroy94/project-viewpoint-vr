package viewpointvr.instrument;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import viewpointvr.FrameBoundary;
import viewpointvr.StereoFrame;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.jar.asm.Opcodes.*;

public final class EntryFixtureTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static int number(Object drawer, String field) throws Exception { return drawer.getClass().getField(field).getInt(drawer); }
    private static Throwable caught(Object drawer) throws Exception { return (Throwable) drawer.getClass().getField("caught").get(drawer); }
    private static void render(Object drawer) throws Exception { drawer.getClass().getMethod("render").invoke(drawer); }
    private static void rejects(FrameBoundary.NativeDraw work) throws Throwable {
        try { work.draw(); } catch (IllegalStateException | IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Expected rejection");
    }
    private static byte[] removeCall(byte[] source) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(source).accept(new ClassVisitor(ASM9, writer) {
            @Override public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
                return new MethodVisitor(ASM9, super.visitMethod(a,n,d,s,e)) {
                    @Override public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                        if (name.equals("drawFrame")) super.visitInsn(POP);
                        else super.visitMethodInsn(op,owner,name,desc,itf);
                    }
                };
            }
        },0);
        return writer.toByteArray();
    }
    @SuppressWarnings("try") // Several tests deliberately exercise registration close/ownership rules.
    public static void main(String[] args) throws Throwable {
        byte[] source = Files.readAllBytes(Path.of(args[0], "viewpoint/SceneDrawer.class"));
        byte[] patched = EntryTransform.patch(source);
        rejects(() -> EntryTransform.patch(removeCall(source)));
        rejects(() -> EntryTransform.patch(patched));
        class FixtureLoader extends ClassLoader {
            Class<?> define() { return defineClass("viewpoint.SceneDrawer", patched, 0, patched.length); }
        }
        Class<?> type = new FixtureLoader().define();
        Object ordinary = type.getConstructor().newInstance(); render(ordinary);
        check(number(ordinary,"nativeCalls") == 1 && number(ordinary,"restores") == 1, "Inactive hook preserves ordinary body/state restoration");
        Object failure = type.getConstructor().newInstance();
        var cause = new IllegalStateException("native failure"); type.getField("failure").set(failure,cause); render(failure);
        check(caught(failure) == cause && number(failure,"restores") == 1, "Existing native catch/finally preserved");

        AtomicReference<FrameBoundary.NativeDraw> saved = new AtomicReference<>();
        try (var scope = FrameBoundary.register((drawer, original) -> { saved.set(original); original.draw(); })) {
            rejects(() -> FrameBoundary.register((d,o) -> {}));
            Object delegated = type.getConstructor().newInstance(); render(delegated);
            check(number(delegated,"nativeCalls") == 1, "Driver can explicitly delegate once");
            rejects(() -> saved.get().draw());
        }
        try (var scope = FrameBoundary.register((drawer, original) -> { original.draw(); original.draw(); })) {
            Object doubled = type.getConstructor().newInstance(); render(doubled);
            check(number(doubled,"nativeCalls") == 1 && caught(doubled) instanceof IllegalStateException, "Blind native two-eye replay rejected");
            check(number(doubled,"restores") == 1, "Replay rejection stays inside native cleanup");
        }
        try (var scope = FrameBoundary.register((drawer, original) -> { throw cause; })) {
            Object broken = type.getConstructor().newInstance(); render(broken);
            check(number(broken,"nativeCalls") == 0 && caught(broken) == cause, "No unsafe native fallback after replacement failure");
        }
        AtomicReference<Throwable> threadFailure = new AtomicReference<>();
        try (var scope = FrameBoundary.register((drawer, original) -> {
            Thread worker = new Thread(() -> { try { original.draw(); } catch (Throwable error) { threadFailure.set(error); } });
            worker.start(); worker.join();
        })) {
            Object other = type.getConstructor().newInstance(); render(other);
            check(threadFailure.get() instanceof IllegalStateException && number(other,"nativeCalls") == 0, "Borrowed native callback cannot cross threads");
            Thread worker = new Thread(() -> { try { scope.close(); } catch (Throwable error) { threadFailure.set(error); } });
            threadFailure.set(null); worker.start(); worker.join();
            check(threadFailure.get() instanceof IllegalStateException, "Registration can only close on owner thread");
        }
        try (var scope = FrameBoundary.register((drawer, original) -> render(drawer))) {
            Object nested = type.getConstructor().newInstance(); render(nested);
            check(caught(nested) instanceof IllegalStateException && number(nested,"nativeCalls") == 0, "Recursive scene consumption rejected");
        }
        // Exercise the entry hook and pair coordinator together, using an independent synthetic
        // stage adapter. Never run the whole original drawFrame once per eye.
        for (boolean failRight : new boolean[]{false,true}) {
            var events = new java.util.ArrayList<String>();
            try (var scope = FrameBoundary.register((drawer, original) -> StereoFrame.render(drawer, new StereoFrame.Backend<Object>() {
                public StereoFrame.Restore save(Object snapshot) { events.add("save"); return () -> events.add("restore"); }
                public boolean prepare(Object snapshot) { events.add("prepare"); return true; }
                public void validate(Object snapshot) { check(snapshot == drawer, "Same borrowed drawer throughout pair"); }
                public void draw(Object snapshot,int eye) { events.add("draw"+eye); if (failRight && eye==1) throw cause; }
                public void copy(int eye) { events.add("copy"+eye); }
                public void release(Object snapshot) { events.add("release"); }
                public void publish() { events.add("publish"); }
            }))) {
                Object paired = type.getConstructor().newInstance(); render(paired);
                check(number(paired,"nativeCalls") == 0 && number(paired,"restores") == 1, "Replacement stage adapter remains inside native restoration boundary");
                var expected = failRight
                        ? java.util.List.of("save","prepare","draw0","copy0","draw1","release","restore")
                        : java.util.List.of("save","prepare","draw0","copy0","draw1","copy1","release","restore","publish");
                check(events.equals(expected), "Integrated pair ordering / cleanup");
                check(caught(paired) == (failRight ? cause : null), "Replacement failure reaches native catch unchanged");
            }
        }
        Object recovered = type.getConstructor().newInstance(); render(recovered);
        check(number(recovered,"nativeCalls") == 1, "Driver and drawing scope removed after failures");
        System.out.println("Entry fixture: " + checks + " checks passed; synthetic classes only");
    }
}
