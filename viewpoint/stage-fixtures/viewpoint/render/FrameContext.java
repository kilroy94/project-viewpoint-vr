package viewpoint.render;
public final class FrameContext {
 public SceneData scene; public org.joml.Matrix4f view;
 public boolean previousValid,outputScissor; public long index; public float frameSeconds;
 public int outputDrawFbo,outputReadFbo; public final int[] viewport=new int[4];
}