package viewpointvr;

import java.util.ArrayList;
import java.util.List;

public final class StereoFrameTest {
    private static int checks;
    private static final Object SNAPSHOT = new Object();
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static final class Fake implements StereoFrame.Backend<Object> {
        final List<String> events = new ArrayList<>();
        final List<String> fail = new ArrayList<>();
        final RuntimeException primary = new RuntimeException("injected");
        int validations;
        boolean skip, stale;
        String state = "desktop";
        void event(String name) {
            events.add(name);
            if (fail.contains(name)) throw name.equals(fail.getFirst()) ? primary : new IllegalStateException(name);
        }
        void same(Object snapshot) { check(snapshot == SNAPSHOT, "Snapshot identity must not change"); }
        public StereoFrame.Restore save(Object snapshot) {
            same(snapshot); event("save");
            String before = state;
            return () -> { state = before; event("restore"); };
        }
        public boolean prepare(Object snapshot) { same(snapshot); state = "prepared"; event("prepare"); return !skip; }
        public void validate(Object snapshot) {
            same(snapshot); event("validate" + ++validations);
            if (stale) throw primary;
        }
        public void draw(Object snapshot, int eye) { same(snapshot); state = "eye" + eye; event("draw" + eye); }
        public void copy(int eye) { check(state.equals("eye" + eye), "Copy immediately follows its eye"); event("copy" + eye); }
        public void release(Object snapshot) { same(snapshot); event("release"); }
        public void publish() { check(state.equals("desktop"), "Restore before publishing"); event("publish"); }
    }
    public static void main(String[] args) throws Throwable {
        Fake good = new Fake();
        check(StereoFrame.render(SNAPSHOT, good) == StereoFrame.Result.COMPLETE, "Complete pair");
        List<String> expected = List.of("save", "validate1", "prepare", "validate2", "draw0", "validate3", "copy0",
                "validate4", "draw1", "validate5", "copy1", "validate6", "release", "restore", "publish");
        check(good.events.equals(expected), "One preparation, two copied eyes, one cleanup in order");
        for (String stage : expected) {
            Fake broken = new Fake(); broken.fail.add(stage);
            try { StereoFrame.render(SNAPSHOT, broken); throw new AssertionError("Expected " + stage); }
            catch (RuntimeException error) { check(error == broken.primary, "Original failure preserved: " + stage); }
            if (!stage.equals("save")) {
                check(broken.events.stream().filter("release"::equals).count() == 1, "Exactly one release at " + stage);
                check(broken.events.stream().filter("restore"::equals).count() == 1, "Exactly one restore at " + stage);
                check(broken.state.equals("desktop"), "Output state restored at " + stage);
            } else check(broken.events.equals(List.of("save")), "Failed transactional save owns no resources");
            if (!stage.equals("publish")) check(!broken.events.contains("publish"), "Incomplete pair never published");
            // Every failure must clear the recursion guard and allow a subsequent frame.
            check(StereoFrame.render(SNAPSHOT, new Fake()) == StereoFrame.Result.COMPLETE, "Recovery after " + stage);
        }
        Fake skipped = new Fake(); skipped.skip = true;
        check(StereoFrame.render(SNAPSHOT, skipped) == StereoFrame.Result.SKIPPED, "Skipped scene");
        check(skipped.events.equals(List.of("save", "validate1", "prepare", "release", "restore")), "Skipped cleanup without draw/copy");
        Fake multiple = new Fake(); multiple.fail.addAll(List.of("draw1", "release", "restore"));
        try { StereoFrame.render(SNAPSHOT, multiple); throw new AssertionError("Expected failure"); }
        catch (RuntimeException error) {
            check(error == multiple.primary, "Draw failure stays primary");
            check(error.getSuppressed().length == 2, "Both cleanup failures retained");
        }
        // A stale generation after a completed draw must stop before copying that eye.
        Fake stale = new Fake(); stale.fail.add("validate3");
        try { StereoFrame.render(SNAPSHOT, stale); throw new AssertionError("Expected invalidation"); }
        catch (RuntimeException error) { check(!stale.events.contains("copy0") && !stale.events.contains("draw1"), "Invalidated scene cannot be copied/replayed"); }
        StereoFrame.Backend<Object> nested = new StereoFrame.Backend<>() {
            public StereoFrame.Restore save(Object s) { return () -> {}; }
            public boolean prepare(Object s) throws Throwable { StereoFrame.render(s, new Fake()); return true; }
            public void validate(Object s) {}
            public void draw(Object s, int eye) { throw new AssertionError("Nested frame rendered"); }
            public void copy(int eye) {}
            public void release(Object s) { checks++; }
            public void publish() { throw new AssertionError("Nested frame published"); }
        };
        try { StereoFrame.render(SNAPSHOT, nested); throw new AssertionError("Expected nested rejection"); }
        catch (IllegalStateException expectedFailure) { checks++; }
        check(StereoFrame.render(SNAPSHOT, new Fake()) == StereoFrame.Result.COMPLETE, "Nested rejection clears scope");
        System.out.println("StereoFrame: " + checks + " checks passed");
    }
}
