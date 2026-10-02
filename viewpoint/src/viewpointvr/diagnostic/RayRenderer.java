package viewpointvr.diagnostic;
import org.joml.Matrix4f;
import viewpointvr.input.PanelRay;
import viewpointvr.xr.XrCamera;
import static org.lwjgl.opengl.GL33.*;
/** Thin controller beam in head-relative meters, independent of world scale and recenter. */
public final class RayRenderer implements AutoCloseable {
 private int program,vao;
 public void draw(int target,int width,int height,PanelRay.Hit ray,XrCamera.Pose head,XrCamera.View eye)throws Throwable{
  if(ray==null)return;
  var f=eye.symmetric();var projection=new Matrix4f().setFrustum(f.left()*.01f,f.right()*.01f,f.down()*.01f,f.up()*.01f,.01f,10);
  var view=eye.pose().matrix().invert().mul(head.matrix());
  Matrix4f mvp=projection.mul(view);
  var saved=new LwjglGraphics().save();int previousProgram=glGetInteger(GL_CURRENT_PROGRAM),previousVao=glGetInteger(GL_VERTEX_ARRAY_BINDING);
  try{
   if(program==0)init();glBindFramebuffer(GL_DRAW_FRAMEBUFFER,target);glViewport(0,0,width,height);
   glUseProgram(program);glBindVertexArray(vao);glDisable(GL_DEPTH_TEST);glDisable(GL_STENCIL_TEST);glDisable(GL_SCISSOR_TEST);
   glDisable(GL_BLEND);glDisable(GL_ALPHA_TEST);glDisable(GL_FRAMEBUFFER_SRGB);glDisable(GL_COLOR_LOGIC_OP);glDisable(GL_RASTERIZER_DISCARD);
   glColorMask(true,true,true,true);glLineWidth(2);
   glUniformMatrix4fv(glGetUniformLocation(program,"mvp"),false,mvp.get(new float[16]));
   glUniform3f(glGetUniformLocation(program,"start"),ray.start().x,ray.start().y,ray.start().z);
   glUniform3f(glGetUniformLocation(program,"finish"),ray.end().x,ray.end().y,ray.end().z);
   glDrawArrays(GL_LINES,0,2);
  }finally{glUseProgram(previousProgram);glBindVertexArray(previousVao);saved.restore().restore();}
 }
 private void init(){
  int vertex=0,fragment=0,p=0;
  try{
   vertex=shader(GL_VERTEX_SHADER,"#version 330\nuniform mat4 mvp;uniform vec3 start,finish;void main(){gl_Position=mvp*vec4(gl_VertexID==0?start:finish,1);}");
   fragment=shader(GL_FRAGMENT_SHADER,"#version 330\nout vec4 color;void main(){color=vec4(0.1,0.9,1,1);}");
   p=glCreateProgram();glAttachShader(p,vertex);glAttachShader(p,fragment);glLinkProgram(p);
   if(glGetProgrami(p,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(p));
   vao=glGenVertexArrays();program=p;p=0;
  }finally{if(vertex!=0)glDeleteShader(vertex);if(fragment!=0)glDeleteShader(fragment);if(p!=0)glDeleteProgram(p);}
 }
 private static int shader(int type,String text){int shader=glCreateShader(type);glShaderSource(shader,text);glCompileShader(shader);if(glGetShaderi(shader,GL_COMPILE_STATUS)==0){String error=glGetShaderInfoLog(shader);glDeleteShader(shader);throw new IllegalStateException(error);}return shader;}
 public void close(){if(program!=0)glDeleteProgram(program);if(vao!=0)glDeleteVertexArrays(vao);program=vao=0;}
}
