package viewpointvr.diagnostic;

import java.lang.classfile.*;
import java.lang.instrument.*;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.*;
import viewpointvr.*;
import viewpointvr.instrument.*;

/** All-target activation; a failed or incompatible later transformation permanently disarms capture. */
public final class Installation implements ClassFileTransformer,AutoCloseable {
    private static final List<String> RENDER_TARGETS=java.util.stream.Stream.concat(java.util.stream.Stream.of(EntryTransform.TARGET,
            "viewpoint/render/WorldRenderer","viewpoint/render/FarPass","viewpoint/render/TemporalPass","viewpoint/render/FrameEnd","viewpoint/render/SkyPass"),java.util.stream.Stream.concat(VisibilityTransform.TARGETS.stream(),UiTransform.TARGETS.stream())).toList();
    public static final List<String> TARGETS=java.util.stream.Stream.concat(RENDER_TARGETS.stream(),GameplayTransform.TARGETS.stream()).toList();
    private final GameplayTransform gameplay;
    private final Instrumentation instrumentation;
    private final ClassLoader loader;
    private final EntryTransform entry;
    private final StageTransform stages;
    private final VisibilityTransform visibility;
    private final UiTransform ui;
    private final Class<?>[] targets=new Class<?>[TARGETS.size()];
    private final Set<String> accepted=new HashSet<>();
    private volatile boolean ready;
    private volatile String failure;
    private boolean registered;
    private Installation(Instrumentation instrumentation,ClassLoader loader,Path jar,Path game) throws Exception {
        this.instrumentation=Objects.requireNonNull(instrumentation); this.loader=loader;
        gameplay=new GameplayTransform(jar,game,loader);
        entry=EntryTransform.fromPinnedJar(jar); stages=StageTransform.fromPinnedJar(jar);visibility=new VisibilityTransform(jar);ui=new UiTransform(jar);
        if(!instrumentation.isRetransformClassesSupported()) throw new IllegalStateException("Retransformation unavailable");
        for(Class<?> hook:List.of(FrameBoundary.class,StageHooks.class,Visibility.class,UiBridge.class,viewpointvr.input.InputBridge.class))
            if(Class.forName(hook.getName(),false,loader)!=hook) throw new IllegalStateException("Hook not visible to native loader");
        for(int i=0;i<targets.length;i++) {
            targets[i]=Class.forName(TARGETS.get(i).replace('/','.'),false,loader);
            if(targets[i].getClassLoader()!=loader || !instrumentation.isModifiableClass(targets[i])
                    || !BinaryPins.location(targets[i]).equals((viewpointvr.input.ControllerInstallation.TARGETS.contains(TARGETS.get(i))?game:jar).toRealPath()))
                throw new IllegalStateException("Unsupported target loader/origin/modifiability: "+TARGETS.get(i));
        }
        new ViewpointBackend.Access(loader);
        new viewpointvr.input.NativeInput(loader);
        Class.forName("viewpoint.platform.SettingsWindow",false,loader).getMethod("panel",String.class,Runnable.class);
    }
    public static Installation install(Instrumentation instrumentation,ClassLoader loader,Path jar,Path game) throws Exception {
        Installation install=new Installation(instrumentation,loader,jar,game);
        instrumentation.addTransformer(install,true); install.registered=true;
        try {
            instrumentation.retransformClasses(install.targets);
            if(install.failure!=null || install.accepted.size()!=TARGETS.size()) throw new IllegalStateException("Incomplete hook set: "+install.failure);
            install.ready=true; viewpointvr.input.ControllerBridge.installed=true;return install;
        } catch(Throwable error) {
            try { install.close(); } catch(Throwable rollback) { error.addSuppressed(rollback); }
            throw new IllegalStateException("Capture disabled; hook activation failed",error);
        }
    }
    public boolean ready() { return ready && failure==null; }
    public String failure() { return failure; }
    @Override public synchronized byte[] transform(ClassLoader owner,String name,Class<?> type,ProtectionDomain domain,byte[] source) {
        if(!TARGETS.contains(name)) return null;
        try {
            if(owner!=loader || type!=targets[TARGETS.indexOf(name)]) throw new IllegalStateException("Wrong transformation owner");
            byte[] result=GameplayTransform.TARGETS.contains(name)?gameplay.transform(name,source):name.equals(EntryTransform.TARGET)?entry.transform(name,source):VisibilityTransform.TARGETS.contains(name)?visibility.transform(name,source):UiTransform.TARGETS.contains(name)?ui.transform(name,source):stages.transform(name,source);
            var verifier=ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(ClassHierarchyResolver.ofResourceParsing(loader)));
            var errors=verifier.verify(result);
            if(!errors.isEmpty()) throw new IllegalStateException("Invalid transformed class: "+errors);
            accepted.add(name); return result;
        } catch(Throwable error) { failure=name+": "+error; ready=false;viewpointvr.input.InputBridge.configure(0);viewpointvr.input.BodyTracking.suspend();return null; }
    }
    @Override public void close() throws UnmodifiableClassException {
        ready=false;viewpointvr.input.ControllerBridge.installed=false;viewpointvr.input.ControllerBridge.configure(0);
        if(registered) {
            registered=false;
            if(!instrumentation.removeTransformer(this)) throw new IllegalStateException("Unable to remove transform");
            instrumentation.retransformClasses(targets);
        }
    }
}
