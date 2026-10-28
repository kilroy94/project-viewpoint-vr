package viewpoint.render;
final class FrameStream { static void frameStarted(){Probe.hit("stream");} }
final class FloorBakes { void update(SceneData s){Probe.hit("floors");} }
final class ShellGround { void update(SceneData s){Probe.hit("ground");} }
final class PackModels { static void upload(long n){Probe.hit("uploads");} }
final class Meshes { static void prepare(SceneData s){Probe.hit("meshes");} }
final class ModelPass { void prepare(SceneData s){Probe.hit("models");} void endFrame(){Probe.hit("texturesRelease");} }
final class Targets {}
final class FrameUniforms {
 static void set(FrameContext f,Targets t){Probe.hit("uniforms");}
 static void bind(Targets t){Probe.hit("uniformBind");}
}
final class PackStages {int run(viewpoint.platform.PackPass.Stage s,FrameContext f,int input){Probe.hit("passes");return input;} boolean any(viewpoint.platform.PackPass.Stage s){Probe.hit("packHistory");return true;} }
final class FarGpu { static void uploadQueued(){Probe.hit("queued");} }
final class BandLight { static void update(){Probe.hit("bandLight");} }
final class CellLightPages { static void update(){Probe.hit("cellLight");} }
final class TreeBaker { void bake(){Probe.hit("trees");} }
final class ShadowPass { void draw(FrameContext f){Probe.hit("shadows");} }
final class WeatherMap { void draw(FrameContext f){Probe.hit("weatherMap");} }
final class MousePick { static void read(FrameContext f,int target,org.joml.Matrix4f inverse){Probe.hit("mousePick");} }
final class FarShadow { void draw(FrameContext f,float range,int mask,int shell,boolean quiet){Probe.hit("farShadows");} }

final class PackLinks { static void follow(){Probe.hit("pack");} }
final class IrisMode { static boolean draw(FrameContext f,ShadowPass s,ModelPass m,FarPass far){Probe.hit("iris");return false;} }

final class DevReload {static void frame(){Probe.hit("developer");}}
final class FarRelief {static void frame(SceneData s){Probe.hit("relief");}}
final class FrameGenPass {static void midframe(){Probe.hit("midframe");}void capture(FrameContext f){Probe.hit("framegen");}}
