package viewpointvr;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.*;

/** Call-site policies for the pinned native stage split. Inactive calls invoke their originals. */
public final class StageHooks {
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();
    public enum Phase { PREPARE, LEFT, RIGHT, RELEASE }
    public static Scope open() {
        if (CURRENT.get() != null) throw new IllegalStateException("Nested native stages");
        Scope scope = new Scope(); CURRENT.set(scope); return scope;
    }
    public static final class Scope implements AutoCloseable {
        private final Thread owner = Thread.currentThread();
        private final Map<String,Object> once = new HashMap<>();
        private final List<MethodHandle> cleanup = new ArrayList<>();
        private final IdentityHashMap<Object,Long> temporalTime = new IdentityHashMap<>();
        private Phase phase = Phase.PREPARE;
        private boolean released, closed;
        private Scope() {}
        private void check() {
            if (owner != Thread.currentThread() || closed || CURRENT.get() != this)
                throw new IllegalStateException("Native stage scope ownership");
        }
        public void eye(int eye) {
            check();
            if (eye == 0 && phase == Phase.PREPARE) phase = Phase.LEFT;
            else if (eye == 1 && phase == Phase.LEFT) phase = Phase.RIGHT;
            else throw new IllegalStateException("Invalid eye transition");
        }
        /** Attempt all resource cleanup, including resources acquired by a failed preparation. */
        public void release() throws Throwable {
            check(); if (released) return;
            released = true; phase = Phase.RELEASE;
            Throwable failure = null;
            for (MethodHandle action : cleanup) {
                try { action.invokeWithArguments(); }
                catch (Throwable error) { failure = append(failure,error); }
            }
            cleanup.clear();
            if (failure != null) throw failure;
        }
        @Override public void close() {
            check();
            if (!released) throw new IllegalStateException("Release native resources before closing scope");
            closed = true; CURRENT.remove();
        }
    }
    public static Throwable append(Throwable first,Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }
    /** Applies globally to native off() callers while a diagnostic scope owns this thread. */
    public static boolean off(boolean original,int feature) {
        return original || CURRENT.get() != null && (feature==1 || feature==2 || feature==4 || feature==6);
    }
    public static CallSite bootstrap(MethodHandles.Lookup caller,String name,MethodType type,
                                     MethodHandle original,MethodHandle alternate,String policy,String key) throws Exception {
        MethodHandle dispatch = MethodHandles.lookup().findStatic(StageHooks.class,"dispatch",
                MethodType.methodType(Object.class,MethodHandle.class,MethodHandle.class,String.class,String.class,Object[].class));
        dispatch = MethodHandles.insertArguments(dispatch,0,original,alternate,policy,key)
                .asCollector(Object[].class,type.parameterCount()).asType(type);
        return new ConstantCallSite(dispatch);
    }
    private static Object dispatch(MethodHandle original,MethodHandle alternate,String policy,String key,Object[] args) throws Throwable {
        Scope scope = CURRENT.get();
        if (scope == null) return original.invokeWithArguments(args);
        scope.check();
        if (scope.phase == Phase.RELEASE) throw new IllegalStateException("Drawing during native cleanup");
        switch (policy) {
            case "world": return scope.phase == Phase.PREPARE ? null : original.invokeWithArguments(args);
            case "pass": return args[args.length-1];
            case "false": return false;
            case "skip": return null;
            case "end":
                if (scope.cleanup.isEmpty()) throw new IllegalStateException("End without model preparation");
                return null;
            case "temporal": {
                Object temporal = args[0], frame = args[1];
                Field last = field(temporal.getClass(),"lastFrameNanos");
                if (!scope.temporalTime.containsKey(temporal)) scope.temporalTime.put(temporal,last.getLong(temporal));
                last.setLong(temporal,scope.temporalTime.get(temporal));
                Object result = original.invokeWithArguments(args);
                field(temporal.getClass(),"historyValid").setBoolean(temporal,false);
                field(frame.getClass(),"previousValid").setBoolean(frame,false);
                return result;
            }
            case "once": case "model": case "uniforms": {
                if (scope.once.containsKey(key)) {
                    if (policy.equals("uniforms")) alternate.invokeWithArguments(args[1]);
                    return scope.once.get(key);
                }
                if (policy.equals("model")) scope.cleanup.add(alternate.bindTo(args[0]));
                Object result = original.invokeWithArguments(args);
                scope.once.put(key,result);
                return result;
            }
            default: throw new IllegalArgumentException("Unknown stage policy: " + policy);
        }
    }
    private static Field field(Class<?> type,String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private StageHooks() {}
}
