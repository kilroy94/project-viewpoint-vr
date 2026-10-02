package viewpointvr.diagnostic;
import viewpointvr.input.PanelRay;
import viewpointvr.xr.XrCamera;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL33.*;
/** Runs in the standalone hidden context; verifies actual pixels and caller-state restoration. */
final class RayGpuTest {
 static int checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static void run()throws Throwable{
  var gl=new LwjglGraphics();int target=gl.create(200,150),other=gl.create(200,150),vao=glGenVertexArrays();
  var head=new XrCamera.Pose(0,0,0,0,0,0,1);
  var ray=new PanelRay.Hit(.5f,.5f,new Vector3f(.4f,-.3f,-.5f),new Vector3f(0,0,-1.5f));
  try(var renderer=new RayRenderer()){
   for(float eyeX:new float[]{-.032f,.032f}){
    gl.bind(target,200,150);glBindFramebuffer(GL_READ_FRAMEBUFFER,other);
    glBindVertexArray(vao);glUseProgram(0);glViewport(7,9,160,120);glEnable(GL_SCISSOR_TEST);glScissor(1,2,3,4);
    glEnable(GL_DEPTH_TEST);glEnable(GL_BLEND);glLineWidth(1);glColorMask(false,false,false,false);
    var eye=new XrCamera.View(new XrCamera.Pose(eyeX,0,0,0,0,0,1),-(float)Math.PI/4,(float)Math.PI/4,(float)Math.PI/4,-(float)Math.PI/4);
    renderer.draw(target,200,150,ray,head,eye);
    check(glGetInteger(GL_READ_FRAMEBUFFER_BINDING)==other&&glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)==target,"ray distinct FBO restore");
    int[] viewport=new int[4];glGetIntegerv(GL_VIEWPORT,viewport);check(java.util.Arrays.equals(viewport,new int[]{7,9,160,120}),"ray viewport restore");
    check(glGetInteger(GL_VERTEX_ARRAY_BINDING)==vao&&glGetInteger(GL_CURRENT_PROGRAM)==0,"ray VAO/program restore");
    check(glIsEnabled(GL_SCISSOR_TEST)&&glIsEnabled(GL_DEPTH_TEST)&&glIsEnabled(GL_BLEND)&&!glGetBoolean(GL_COLOR_WRITEMASK)&&glGetFloat(GL_LINE_WIDTH)==1,"ray fixed state restore");
    var image=gl.read(target,200,150);int cyan=0,minX=200,maxX=0;
    for(int y=0;y<150;y++)for(int x=0;x<200;x++){int rgb=image.getRGB(x,y);if((rgb&255)>240&&((rgb>>>8)&255)>210&&((rgb>>>16)&255)<40){cyan++;minX=Math.min(minX,x);maxX=Math.max(maxX,x);}}
    check(cyan>60,"visible cyan beam pixels");
    check(Math.abs(minX-(100-eyeX/1.5f*100))<4,"beam endpoint uses eye offset");
    check(Math.abs(maxX-(100+(.4f-eyeX)/.5f*100))<5,"beam start uses eye projection");
   }
   check(glGetError()==GL_NO_ERROR,"ray no GL errors");
  }finally{glBindFramebuffer(GL_FRAMEBUFFER,0);glBindVertexArray(0);gl.delete(target);gl.delete(other);glDeleteVertexArrays(vao);glDisable(GL_SCISSOR_TEST);glDisable(GL_DEPTH_TEST);glDisable(GL_BLEND);glColorMask(true,true,true,true);}
  System.out.println("Controller ray GPU: "+checks+" checks passed");
 }
}
