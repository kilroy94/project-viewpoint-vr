package viewpointvr.diagnostic;
import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL33.*;
public final class UiGpuTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static int[] pixel(int target,int x,int y){
        int old=glGetInteger(GL_READ_FRAMEBUFFER_BINDING);glBindFramebuffer(GL_READ_FRAMEBUFFER,target);
        var data=ByteBuffer.allocateDirect(4);glReadPixels(x,y,1,1,GL_RGBA,GL_UNSIGNED_BYTE,data);glBindFramebuffer(GL_READ_FRAMEBUFFER,old);
        return new int[]{data.get(0)&255,data.get(1)&255,data.get(2)&255,data.get(3)&255};
    }
    static void equal(int[] actual,int... expected){for(int i=0;i<4;i++)check(Math.abs(actual[i]-expected[i])<=2,"UI pixel channel "+i+": "+java.util.Arrays.toString(actual));}
    static void fill(float r,float g,float b,float a){glClearColor(r,g,b,a);glClear(GL_COLOR_BUFFER_BIT);}
    public static void run() throws Throwable {
        var gl=new LwjglGraphics();int desktop=gl.create(160,120);long[] time={1};
        int sampler=glGenSamplers();glBindSampler(0,sampler);
        try(var ui=new UiCapture(()->time[0])){
            glBindFramebuffer(GL_FRAMEBUFFER,desktop);glViewport(0,0,160,120);glDisable(GL_SCISSOR_TEST);glColorMask(true,true,true,true);fill(0,0,1,1);
            ui.begin(0);glEnable(GL_SCISSOR_TEST);glScissor(0,0,80,120);fill(.5f,0,0,.5f);glDisable(GL_SCISSOR_TEST);ui.end(0);
            equal(pixel(desktop,20,20),128,0,127,255);equal(pixel(desktop,120,20),0,0,255,255);
            check(glGetIntegeri(GL_SAMPLER_BINDING,0)==sampler,"Sampler restored");
            ui.begin(1);fill(0,.25f,0,.25f);ui.end(1);
            ui.begin(3);glEnable(GL_SCISSOR_TEST);glScissor(0,60,160,60);fill(0,0,1,1);glDisable(GL_SCISSOR_TEST);ui.end(3);
            ui.present();var image=ui.image();check(image!=null&&image.width()==160&&image.height()==120,"Published extent");
            equal(pixel(image.framebuffer(),20,20),96,64,0,159);equal(pixel(image.framebuffer(),20,90),0,0,255,255);
            ui.present();equal(pixel(ui.image().framebuffer(),20,90),128,0,0,128);equal(pixel(ui.image().framebuffer(),120,20),0,0,0,0);
            time[0]+=2_000_000_001L;check(ui.image()==null,"Stale frame expires");ui.present();equal(pixel(ui.image().framebuffer(),20,20),0,0,0,0);
            glViewport(0,0,80,60);ui.begin(0);fill(0,1,0,1);ui.end(0);ui.present();check(ui.image().width()==80&&ui.image().height()==60,"Resize replaces layers");
            equal(pixel(ui.image().framebuffer(),20,20),0,255,0,255);
            viewpointvr.UiBridge.cursor(.25f,.75f,true);ui.present();equal(pixel(ui.image().framebuffer(),20,14),255,255,255,255);
            viewpointvr.UiBridge.cursor(.25f,.75f,false);ui.present();equal(pixel(ui.image().framebuffer(),20,14),0,255,0,255);
            ui.begin(0);
            glEnable(GL_SCISSOR_TEST);glScissor(0,0,40,60);glStencilMask(-1);glClearStencil(1);glClear(GL_STENCIL_BUFFER_BIT);glDisable(GL_SCISSOR_TEST);
            glEnable(GL_STENCIL_TEST);glStencilFunc(GL_EQUAL,1,255);glStencilOp(GL_KEEP,GL_KEEP,GL_KEEP);
            glUseProgram(0);glActiveTexture(GL_TEXTURE0);glDisable(GL_TEXTURE_2D);glDisable(GL_BLEND);glDisable(GL_DEPTH_TEST);
            glMatrixMode(GL_PROJECTION);glLoadIdentity();glMatrixMode(GL_MODELVIEW);glLoadIdentity();
            glColor4f(1,1,0,1);glBegin(GL_QUADS);glVertex2f(-1,-1);glVertex2f(1,-1);glVertex2f(1,1);glVertex2f(-1,1);glEnd();
            glDisable(GL_STENCIL_TEST);ui.end(0);ui.present();
            equal(pixel(ui.image().framebuffer(),20,30),255,255,0,255);equal(pixel(ui.image().framebuffer(),60,30),0,0,0,0);
            ui.begin(0);glEnable(GL_STENCIL_TEST);glStencilFunc(GL_EQUAL,1,255);glColor4f(1,0,0,1);
            glBegin(GL_QUADS);glVertex2f(-1,-1);glVertex2f(1,-1);glVertex2f(1,1);glVertex2f(-1,1);glEnd();
            glDisable(GL_STENCIL_TEST);ui.end(0);ui.present();equal(pixel(ui.image().framebuffer(),20,30),0,0,0,0);
            check(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)==desktop,"Desktop FBO restored");check(glGetError()==GL_NO_ERROR,"UI GL clean");
            ui.begin(0);ui.failed(new IllegalStateException("expected synthetic failure"));check(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)==desktop&&ui.image()==null,"Failure restores desktop and retires snapshot");
        }finally{glBindSampler(0,0);glDeleteSamplers(sampler);glBindFramebuffer(GL_FRAMEBUFFER,0);gl.delete(desktop);}
        System.out.println("UI GPU composition: "+checks+" checks passed");
    }
}
