package viewpointvr.diagnostic;

import static org.lwjgl.opengl.GL33.*;

/** Hidden-context output/UI integration; never opens XR or runs game/mod entry points. */
public final class SpectatorGpuTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void pixel(int target,int x,int y,int... expected){
        int[] actual=UiGpuTest.pixel(target,x,y);
        for(int i=0;i<4;i++)check(Math.abs(actual[i]-expected[i])<=2,"Spectator pixel "+x+","+y+": "+java.util.Arrays.toString(actual));
    }
    private static void draw(LiveOutput live,boolean single,LiveOutput.Copy copy)throws Throwable{
        if(single)live.begin(80,80,copy,true);else live.begin(80,80,copy);
        var state=live.save();
        for(int eye=0;eye<2;eye++){
            live.bind(eye);glClearColor(eye==0?1:0,0,eye==1?1:0,1);glClear(GL_COLOR_BUFFER_BIT);live.copy(eye);
        }
        state.restore().restore();live.publish();
        check(live.published(),"Complete pair still published with either desktop presentation");
    }
    public static void run()throws Throwable{
        var gl=new LwjglGraphics();int desktop=gl.create(220,160),read=gl.create(220,160);
        try(var live=new LiveOutput(gl);var ui=new UiCapture()){
            glBindFramebuffer(GL_FRAMEBUFFER,desktop);glDisable(GL_SCISSOR_TEST);glDisable(GL_FRAMEBUFFER_SRGB);
            glColorMask(true,true,true,true);glClearColor(1,0,1,1);glClear(GL_COLOR_BUFFER_BIT);
            glBindFramebuffer(GL_READ_FRAMEBUFFER,read);glViewport(10,20,200,120);
            glEnable(GL_SCISSOR_TEST);glScissor(1,2,3,4);glEnable(GL_FRAMEBUFFER_SRGB);glColorMask(false,false,false,false);
            int[] copies={0,0};
            draw(live,true,(eye,fbo,w,h)->{
                copies[eye]++;check(w==80&&h==80,"Eye copy dimensions retained");
                pixel(fbo,40,40,eye==0?255:0,0,eye==1?255:0,255);
            });
            check(copies[0]==1&&copies[1]==1,"Both distinct eye images copied exactly once before desktop mirror");
            check(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)==desktop&&glGetInteger(GL_READ_FRAMEBUFFER_BINDING)==read,"Distinct borrowed output bindings restored");
            int[] viewport=new int[4];glGetIntegerv(GL_VIEWPORT,viewport);
            check(java.util.Arrays.equals(viewport,new int[]{10,20,200,120}),"Nonzero viewport origin restored");
            int[] scissor=new int[4];glGetIntegerv(GL_SCISSOR_BOX,scissor);
            check(glIsEnabled(GL_SCISSOR_TEST)&&java.util.Arrays.equals(scissor,new int[]{1,2,3,4}),"Caller scissor restored");
            check(glIsEnabled(GL_FRAMEBUFFER_SRGB)&&!glGetBoolean(GL_COLOR_WRITEMASK),"Caller sRGB/color mask restored");
            pixel(desktop,60,80,255,0,0,255);pixel(desktop,160,80,255,0,0,255);
            pixel(desktop,20,80,0,0,0,255);pixel(desktop,200,80,0,0,0,255);
            pixel(desktop,5,5,255,0,255,255);

            // A normal desktop UI producer draws once at desktop dimensions after the world.
            glBindFramebuffer(GL_FRAMEBUFFER,desktop);glViewport(0,0,220,160);
            glDisable(GL_SCISSOR_TEST);glDisable(GL_FRAMEBUFFER_SRGB);glColorMask(true,true,true,true);
            ui.begin(0);glEnable(GL_SCISSOR_TEST);glScissor(0,0,30,160);
            glClearColor(0,.5f,0,.5f);glClear(GL_COLOR_BUFFER_BIT);glDisable(GL_SCISSOR_TEST);ui.end(0);
            pixel(desktop,20,80,0,128,0,255); // one premultiplied blend, not duplicated
            pixel(desktop,100,80,255,0,0,255);
            ui.present();var image=ui.image();
            check(image!=null&&image.width()==220&&image.height()==160,"Headset UI retains full desktop extent");
            pixel(image.framebuffer(),20,80,0,128,0,128);
            pixel(image.framebuffer(),100,80,0,0,0,0); // no spectator world baked into UI quad
            pixel(desktop,20,80,0,128,0,255); // XR UI assembly does not blend desktop again

            draw(live,false,null);
            pixel(desktop,55,80,255,0,0,255);pixel(desktop,165,80,0,0,255,255);
            check(glIsFramebuffer(desktop)&&glIsFramebuffer(read),"Borrowed desktop targets remain owned by caller");
            check(glGetError()==GL_NO_ERROR,"Spectator/UI integration leaves no GL errors");
        }finally{
            glBindFramebuffer(GL_FRAMEBUFFER,0);gl.delete(desktop);gl.delete(read);
            glDisable(GL_SCISSOR_TEST);glDisable(GL_FRAMEBUFFER_SRGB);glColorMask(true,true,true,true);glViewport(0,0,200,150);
        }
        System.out.println("Spectator mirror + UI GPU: "+checks+" checks passed; no runtime/game launched");
    }
}
