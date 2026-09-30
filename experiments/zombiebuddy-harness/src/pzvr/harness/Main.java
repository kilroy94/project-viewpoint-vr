package pzvr.harness;

import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import me.zed_0xff.zombie_buddy.Exposer.LuaClass;
import me.zed_0xff.zombie_buddy.Loader;
import pzvr.VersionGate;

@LuaClass(name="PZVRStereo")
public final class Main {
    private static volatile Installation installation;
    private static volatile String status="Not initialized";
    private static boolean attempted;
    public static synchronized void main(String[] args) {
        if(attempted) return; attempted=true;
        try {
            if(zombie.network.GameServer.server || zombie.network.GameClient.client) throw new IllegalStateException("Single player only");
            ClassLoader loader=Main.class.getClassLoader();
            Class<?> frame=Class.forName("com.pavelvoronin.pz3d.Renderer$Frame",false,loader);
            Path game=location(zombie.GameWindow.class), pz=location(frame), zb=location(Loader.class);
            VersionGate.Verified verified=VersionGate.verify(game,pz,zb);
            Map<String,byte[]> originals=new HashMap<>();
            try(JarFile jar=new JarFile(pz.toFile())) {
                for(String name:Installation.TARGETS) try(var in=jar.getInputStream(jar.getJarEntry(name+".class"))) { originals.put(name,in.readAllBytes()); }
            }
            var field=Loader.class.getDeclaredField("g_instrumentation"); field.setAccessible(true);
            Instrumentation instrumentation=(Instrumentation)field.get(null);
            if(instrumentation==null) throw new IllegalStateException("ZombieBuddy instrumentation unavailable");
            Path output=Path.of(zombie.ZomboidFileSystem.instance.getCacheDir(),"PZ3D-VR-Test");
            installation=Installation.install(instrumentation,loader,originals);
            CaptureHarness.configure(verified,installation,output);
            try { pzvr.melee.MeleeInstallation.install(instrumentation,loader); }
            catch(Exception meleeFailure) { System.err.println("[PZ3D VR Melee] "+meleeFailure); }
            try { pzvr.input.ControllerInstallation.install(instrumentation,loader); }
            catch(Exception inputFailure) { System.err.println("[PZ3D VR Input] "+inputFailure); }
            try { pzvr.turn.TurnInstallation.install(instrumentation,loader); }
            catch(Exception turnFailure) { System.err.println("[PZ3D VR Turn] "+turnFailure); }
            try { pzvr.interaction.UseInstallation.install(instrumentation,loader); }
            catch(Exception useFailure) { System.err.println("[PZ3D VR Use] "+useFailure); }
            status="Ready: first person, on foot; configure shortcuts in Options > Mods > PZ3D VR";
        } catch(Throwable error) {
            status="Disabled: "+error; error.printStackTrace();
        }
        System.out.println("[PZ3D VR Test] "+status);
    }
    private static Path location(Class<?> type) throws Exception { return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()); }
    public static String requestCapture() {
        if(installation==null || !installation.ready()) return "Disabled: "+(installation==null?status:installation.failure());
        return CaptureHarness.request();
    }
    public static void tickXR() { XrHarness.watchdog(); }
    public static String recenterStatus() { return XrHarness.recenterStatus(); }
    public static void setTurning(int mode,int snap,int speed,int source,int aim) { pzvr.turn.TurnRuntime.configure(mode,snap,speed,source,aim); }
    public static void setHandInteraction(int mode,int hand) { pzvr.interaction.HandUse.configure(mode,hand); }
    public static void setArmReachPercent(int percent) { TrackedArms.setReachPercent(percent); }
    private static int requestedMeleeMode;
    private static boolean allowMotionMeleeWithGamepad;
    public static void setControllerMode(int mode) { pzvr.input.ControllerBridge.configure(mode); updateInputOwnership(); }
    public static void setAllowMotionMeleeWithGamepad(boolean allow) { allowMotionMeleeWithGamepad=allow; updateInputOwnership(); }
    public static boolean isBridgeController(int id) { return pzvr.input.ControllerBridge.owns(id); }
    public static void setMeleeMode(int mode) { requestedMeleeMode=mode>=0&&mode<=4?mode:0; updateInputOwnership(); }
    private static void updateInputOwnership() {
        boolean gamepad=pzvr.input.ControllerBridge.mode==2;
        int mode=gamepad&&!allowMotionMeleeWithGamepad?0:requestedMeleeMode;
        pzvr.input.ControllerBridge.reserveRightTrigger(gamepad&&mode!=0);
        pzvr.melee.MeleeInput.mode=mode;
        pzvr.melee.MeleeInput.permit=null;
        pzvr.melee.MeleeInput.pending.set(null);
    }
    public static void setHotkeys(int xr,int xrMods,int recenter,int recenterMods,int preview,int previewMods,int capture,int captureMods) {
        Hotkeys.configure(xr,xrMods,recenter,recenterMods,preview,previewMods,capture,captureMods);
    }
    public static void blockHotkeys(boolean blocked) { Hotkeys.block(blocked); pzvr.turn.TurnRuntime.block(blocked); pzvr.melee.MeleeInput.uiBlocked=blocked; pzvr.interaction.HandUse.block(blocked); }
    public static String status() { return installation!=null && installation.ready()?CaptureHarness.status():status; }
}
