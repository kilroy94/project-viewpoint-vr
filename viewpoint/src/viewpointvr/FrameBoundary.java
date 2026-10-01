package viewpointvr;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;

/** Inert until a verified loader supplies a dispatcher; scoped test drivers take precedence. */
public final class FrameBoundary {
    @FunctionalInterface public interface NativeDraw { void draw() throws Throwable; }
    @FunctionalInterface public interface Driver {
        /** A replacement driver must split native stages; original is single-use, not a stereo loop. */
        void render(Object drawer, NativeDraw original) throws Throwable;
    }
    private static final ThreadLocal<Registration> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> DRAWING = new ThreadLocal<>();
    private static volatile Driver dispatcher;
    public static void dispatcher(Driver value) { dispatcher = value; }

    /** Intended for a future render-thread adapter; only synthetic tests register one today. */
    public static Registration register(Driver driver) {
        Objects.requireNonNull(driver, "driver");
        if (DRIVER.get() != null || DRAWING.get() != null)
            throw new IllegalStateException("A frame driver is already scoped to this thread");
        Registration registration = new Registration(driver);
        DRIVER.set(registration);
        return registration;
    }

    public static final class Registration implements AutoCloseable {
        private final Driver driver;
        private final Thread owner = Thread.currentThread();
        private boolean closed;
        private Registration(Driver driver) { this.driver = driver; }
        @Override public void close() {
            if (Thread.currentThread() != owner) throw new IllegalStateException("Wrong render thread");
            if (closed) return;
            if (DRAWING.get() != null) throw new IllegalStateException("Cannot remove an active driver");
            if (DRIVER.get() != this) throw new IllegalStateException("Driver ownership changed");
            DRIVER.remove();
            closed = true;
        }
    }

    /** Called from the transformed call site inside Viewpoint's existing failure/GL-state boundary. */
    public static void draw(Object drawer, MethodHandle nativeMethod) throws Throwable {
        Objects.requireNonNull(drawer, "drawer");
        Objects.requireNonNull(nativeMethod, "nativeMethod");
        if (!nativeMethod.type().equals(MethodType.methodType(void.class, drawer.getClass())))
            throw new IllegalArgumentException("Unexpected native draw signature");
        if (DRAWING.get() != null) throw new IllegalStateException("Recursive scene consumption");
        OneShot original = new OneShot(drawer, nativeMethod);
        DRAWING.set(true);
        try {
            Registration registration = DRIVER.get();
            Driver driver = registration == null ? dispatcher : registration.driver;
            if (driver == null) original.draw();
            else driver.render(drawer, original);
        } finally {
            original.valid = false;
            DRAWING.remove();
        }
    }

    private static final class OneShot implements NativeDraw {
        private final Thread owner = Thread.currentThread();
        private final MethodHandle method;
        private boolean valid = true;
        private boolean used;
        OneShot(Object receiver, MethodHandle method) { this.method = method.bindTo(receiver); }
        @Override public void draw() throws Throwable {
            if (Thread.currentThread() != owner || !valid || used)
                throw new IllegalStateException("Native draw must run at most once within its owning scope/thread");
            used = true;
            method.invokeExact();
        }
    }
    private FrameBoundary() {}
}
