package viewpoint.render;
final class FarPass {
 private final TreeBaker trees=new TreeBaker();
 private final FarShadow shadow=new FarShadow();
 void prepare(FrameContext frame,boolean shadows){
  FarGpu.uploadShells();FarGpu.uploadCells();BandLight.update();CellLightPages.update();
  Probe.hit("frustum");trees.bake();Probe.hit("mask");shadow.draw(frame,1f,2,3,true);
 }
}
