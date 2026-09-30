package viewpointvr;

import java.util.Objects;

/** Synchronous borrowed-snapshot lifetime. No game, GL or retained-frame ownership. */
public final class StereoFrame {
    public enum Result { SKIPPED, COMPLETE }
    @FunctionalInterface public interface Restore { void restore() throws Throwable; }
    public interface Backend<S> {
        /** Must be transactional: either return a restoration action or leave state unchanged. */
        Restore save(S snapshot) throws Throwable;
        /** Called once after initial validation. Partial preparation is released. */
        boolean prepare(S snapshot) throws Throwable;
        /** Check that the borrowed snapshot/generation is still valid. Never adopt a newer frame. */
        void validate(S snapshot) throws Throwable;
        void draw(S snapshot, int eye) throws Throwable;
        /** Synchronous copy before the scratch target can be reused by the other eye. */
        void copy(int eye) throws Throwable;
        /** Called once after successful save, including skip and partially prepared failures. */
        void release(S snapshot) throws Throwable;
        /** Called only after both copies, release and restoration succeed. */
        void publish() throws Throwable;
    }

    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

    public static <S> Result render(S snapshot, Backend<S> backend) throws Throwable {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(backend, "backend");
        if (ACTIVE.get() != null) throw new IllegalStateException("Nested stereo frame");
        ACTIVE.set(true);
        try {
            Restore restore = Objects.requireNonNull(backend.save(snapshot), "restore");
            boolean complete = false;
            Throwable failure = null;
            try {
                backend.validate(snapshot);
                if (backend.prepare(snapshot)) {
                    for (int eye = 0; eye < 2; eye++) {
                        backend.validate(snapshot);
                        backend.draw(snapshot, eye);
                        backend.validate(snapshot);
                        backend.copy(eye);
                    }
                    backend.validate(snapshot);
                    complete = true;
                }
            } catch (Throwable error) {
                failure = error;
            }
            // Always attempt both cleanup operations; preserve the first failure.
            try { backend.release(snapshot); } catch (Throwable error) { failure = combine(failure, error); }
            try { restore.restore(); } catch (Throwable error) { failure = combine(failure, error); }
            if (failure != null) throw failure;
            if (!complete) return Result.SKIPPED;
            backend.publish();
            return Result.COMPLETE;
        } finally {
            ACTIVE.remove();
        }
    }

    private static Throwable combine(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }
    private StereoFrame() {}
}
