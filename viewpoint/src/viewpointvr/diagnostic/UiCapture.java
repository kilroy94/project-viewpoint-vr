package viewpointvr.diagnostic;

import viewpointvr.UiBridge;
import java.util.function.LongSupplier;
import static org.lwjgl.opengl.GL33.*;

/** Render-thread transparent UI targets. Completed UI is submitted on the next XR frame. */
public final class UiCapture implements UiBridge.Sink,AutoCloseable {
    public record Image(int framebuffer,int width,int height) {}
    private final LwjglGraphics gl=new LwjglGraphics();
    private final LongSupplier clock;
    private final int[] layers=new int[4];
    private final boolean[] valid=new boolean[4];
    private int width,height,combined,depthStencil,program,vao,open=-1,caller;
    private long vanillaTime,published;
    private boolean failed;
    public UiCapture(){this(System::nanoTime);}
    public UiCapture(LongSupplier clock){this.clock=clock;}
    public Image image(){return !failed&&combined!=0&&published!=0&&clock.getAsLong()-published<2_000_000_000L?new Image(combined,width,height):null;}
    public void begin(int layer) throws Throwable {
        if(failed)return;
        if(open!=-1)throw new IllegalStateException("Unbalanced UI capture markers");
        int[] viewport=new int[4];glGetIntegerv(GL_VIEWPORT,viewport);
        if(viewport[0]!=0||viewport[1]!=0)throw new IllegalStateException("UI viewport origin unsupported");
        ensure(viewport[2],viewport[3]);
        caller=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        clear(layers[layer]);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,layers[layer]);
        open=layer;
    }
    public void end(int layer) throws Throwable {
        if(failed)return;
        if(open!=layer)throw new IllegalStateException("Mismatched UI capture markers");
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER,caller);open=-1;
        valid[layer]=true;if(layer==0)vanillaTime=clock.getAsLong();
        // Restore the normal desktop result without running UI logic or drawing commands twice.
        blend(layers[layer]);
    }
    public void present() throws Throwable {
        if(failed)return;
        if(open!=-1)throw new IllegalStateException("Unclosed UI capture at swap");
        if(combined==0)return;
        var saved=gl.save();
        try{
            clear(combined);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,combined);glViewport(0,0,width,height);
            if(clock.getAsLong()-vanillaTime>2_000_000_000L)valid[0]=false;
            for(int i=0;i<4;i++)if(valid[i])blend(layers[i]);
            var cursor=UiBridge.cursor();
            if(cursor!=null&&cursor.visible()&&System.nanoTime()-cursor.time()<500_000_000L
                &&cursor.x()>=0&&cursor.x()<1&&cursor.y()>=0&&cursor.y()<1){
                int x=Math.round(cursor.x()*width),y=height-1-Math.round(cursor.y()*height);
                glEnable(GL_SCISSOR_TEST);glColorMask(true,true,true,true);
                glClearColor(0,0,0,1);glScissor(x-5,y-5,11,11);glClear(GL_COLOR_BUFFER_BIT);
                glClearColor(1,1,1,1);glScissor(x-1,y-4,3,9);glClear(GL_COLOR_BUFFER_BIT);
                glScissor(x-4,y-1,9,3);glClear(GL_COLOR_BUFFER_BIT);
            }
            published=clock.getAsLong();
        }finally{saved.restore().restore();for(int i=1;i<4;i++)valid[i]=false;}
    }
    public void failed(Throwable error){
        failed=true;
        if(open!=-1){glBindFramebuffer(GL_DRAW_FRAMEBUFFER,caller);open=-1;}
        System.err.println("[Project Viewpoint VR UI] Capture disabled: "+error);
    }
    private void ensure(int w,int h) throws Throwable {
        if(w<1||h<1||w>8192||h>8192)throw new IllegalArgumentException("Invalid UI extent");
        if(width==w&&height==h)return;
        releaseTargets();width=w;height=h;
        for(int i=0;i<4;i++)layers[i]=gl.create(w,h);
        combined=gl.create(w,h);
        var state=gl.save();int previous=glGetInteger(GL_RENDERBUFFER_BINDING);
        try{
            depthStencil=glGenRenderbuffers();glBindRenderbuffer(GL_RENDERBUFFER,depthStencil);
            glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH24_STENCIL8,w,h);
            for(int target:layers){
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER,target);
                glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER,GL_DEPTH_STENCIL_ATTACHMENT,GL_RENDERBUFFER,depthStencil);
                if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("UI depth/stencil framebuffer incomplete");
            }
        }finally{glBindRenderbuffer(GL_RENDERBUFFER,previous);state.restore().restore();}
    }
    private void clear(int target) throws Throwable {
        var state=gl.save();
        try{glBindFramebuffer(GL_DRAW_FRAMEBUFFER,target);glDisable(GL_SCISSOR_TEST);glColorMask(true,true,true,true);glClearColor(0,0,0,0);glDepthMask(true);glStencilMask(-1);glClearDepth(1);glClearStencil(0);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT|GL_STENCIL_BUFFER_BIT);}
        finally{state.restore().restore();}
    }
    private void blend(int target) throws Throwable {
        var state=gl.save();int oldProgram=glGetInteger(GL_CURRENT_PROGRAM),oldVao=glGetInteger(GL_VERTEX_ARRAY_BINDING),oldSampler=glGetIntegeri(GL_SAMPLER_BINDING,0);
        try{
            if(program==0)createProgram();
            glUseProgram(program);glBindVertexArray(vao);
            glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,gl.texture(target));glBindSampler(0,0);
            glDisable(GL_ALPHA_TEST);glDisable(GL_DEPTH_TEST);glDisable(GL_STENCIL_TEST);glDisable(GL_SCISSOR_TEST);glDisable(GL_CULL_FACE);
            glDisable(GL_FRAMEBUFFER_SRGB);glDisable(GL_RASTERIZER_DISCARD);glDisable(GL_COLOR_LOGIC_OP);
            glPolygonMode(GL_FRONT_AND_BACK,GL_FILL);glColorMask(true,true,true,true);
            glEnable(GL_BLEND);glBlendEquation(GL_FUNC_ADD);glBlendFuncSeparate(GL_ONE,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
            glDrawArrays(GL_TRIANGLE_STRIP,0,4);
        }finally{glBindSampler(0,oldSampler);glUseProgram(oldProgram);glBindVertexArray(oldVao);state.restore().restore();}
    }
    private void createProgram(){
        int vertex=0,fragment=0,p=0;
        try{
            vertex=shader(GL_VERTEX_SHADER,"#version 330\nout vec2 uv;void main(){uv=vec2(gl_VertexID&1,gl_VertexID>>1);gl_Position=vec4(uv*2.0-1.0,0,1);}");
            fragment=shader(GL_FRAGMENT_SHADER,"#version 330\nin vec2 uv;uniform sampler2D panel;out vec4 color;void main(){color=texture(panel,uv);}");
            p=glCreateProgram();glAttachShader(p,vertex);glAttachShader(p,fragment);glLinkProgram(p);
            if(glGetProgrami(p,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(p));
            vao=glGenVertexArrays();program=p;p=0;
        }finally{if(vertex!=0)glDeleteShader(vertex);if(fragment!=0)glDeleteShader(fragment);if(p!=0)glDeleteProgram(p);}
    }
    private static int shader(int kind,String text){int shader=glCreateShader(kind);glShaderSource(shader,text);glCompileShader(shader);if(glGetShaderi(shader,GL_COMPILE_STATUS)==0){String error=glGetShaderInfoLog(shader);glDeleteShader(shader);throw new IllegalStateException(error);}return shader;}
    private void releaseTargets(){
        for(int i=0;i<4;i++){gl.delete(layers[i]);layers[i]=0;valid[i]=false;}
        gl.delete(combined);combined=0;if(depthStencil!=0)glDeleteRenderbuffers(depthStencil);depthStencil=0;published=0;width=height=0;
    }
    public void close(){
        if(open!=-1){glBindFramebuffer(GL_DRAW_FRAMEBUFFER,caller);open=-1;}
        releaseTargets();if(program!=0)glDeleteProgram(program);if(vao!=0)glDeleteVertexArrays(vao);program=vao=0;
    }
}
