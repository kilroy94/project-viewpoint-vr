package viewpointvr.diagnostic;

import java.nio.file.*;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/** Standalone hidden GL context; no game/mod entry points and no OpenXR. */
public final class GpuOutputTest {
    static int checks;
    static void check(boolean ok,String message) { checks++;if(!ok)throw new AssertionError(message); }
    static void stripe(int y,int r,int g,int b) {
        glEnable(GL_SCISSOR_TEST);glScissor(0,y,160,4);glClearColor(r,g,b,1);glClear(GL_COLOR_BUFFER_BIT);glDisable(GL_SCISSOR_TEST);
    }
    public static void main(String[] args) throws Throwable {
        if(!glfwInit())throw new IllegalStateException("GLFW initialization failed");
        glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);
        glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_COMPAT_PROFILE);
        long window=glfwCreateWindow(200,150,"Viewpoint capture test",0,0);
        if(window==0)throw new IllegalStateException("Hidden context unavailable");
        try {
            glfwMakeContextCurrent(window);GL.createCapabilities();
            glViewport(7,9,160,120);glEnable(GL_SCISSOR_TEST);glScissor(1,2,3,4);glEnable(GL_FRAMEBUFFER_SRGB);
            int pack=glGenBuffers(),unpack=glGenBuffers(),texture=glGenTextures();
            glBindBuffer(GL_PIXEL_PACK_BUFFER,pack);glBindBuffer(GL_PIXEL_UNPACK_BUFFER,unpack);
            glPixelStorei(GL_PACK_ROW_LENGTH,177);glPixelStorei(GL_PACK_SKIP_PIXELS,3);glPixelStorei(GL_PACK_ALIGNMENT,8);
            glActiveTexture(GL_TEXTURE3);glBindTexture(GL_TEXTURE_2D,texture);
            glColorMask(false,false,false,false);glClearColor(.2f,.3f,.4f,1);
            Path result;int[] targetIds=new int[2];
            try(var capture=new CaptureOutput(new LwjglGraphics(),Path.of(args[0]))) {
                var state=capture.save();
                for(int eye=0;eye<2;eye++) {
                    capture.bind(eye);targetIds[eye]=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
                    glClearColor(eye==0?1:0,0,eye==1?1:0,1);glClear(GL_COLOR_BUFFER_BIT);
                    stripe(0,1,1,0);stripe(116,0,1,0);capture.copy(eye);
                    check(glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING)==pack,"Readback restores caller PBO");
                    check(glGetInteger(GL_PACK_ROW_LENGTH)==177,"Readback restores pixel stride");
                }
                state.restore().restore();capture.publish();result=capture.result();
                int[] vp=new int[4];glGetIntegerv(GL_VIEWPORT,vp);
                check(java.util.Arrays.equals(vp,new int[]{7,9,160,120}),"Viewport including origin restored");
                check(glGetInteger(GL_READ_FRAMEBUFFER_BINDING)==0 && glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)==0,"Framebuffer bindings restored");
                check(glIsEnabled(GL_SCISSOR_TEST) && glIsEnabled(GL_FRAMEBUFFER_SRGB),"Scissor/sRGB restored");
                check(glGetInteger(GL_ACTIVE_TEXTURE)==GL_TEXTURE3 && glGetInteger(GL_TEXTURE_BINDING_2D)==texture,"Texture unit/binding restored");
                check(glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING)==unpack && glGetInteger(GL_PACK_SKIP_PIXELS)==3,"Pixel-transfer state restored");
                check(!glGetBoolean(GL_COLOR_WRITEMASK),"Color mask restored");
            }
            for(int eye=0;eye<2;eye++) {
                var image=ImageIO.read(result.resolve(eye==0?"left.png":"right.png").toFile());
                check(image.getWidth()==160 && image.getHeight()==120,"PNG extent");
                check((image.getRGB(80,60)&0xffffff)==(eye==0?0xff0000:0x0000ff),"Eye identity");
                check((image.getRGB(80,1)&0xffffff)==0x00ff00 && (image.getRGB(80,118)&0xffffff)==0xffff00,"PNG vertical orientation");
                check(!glIsFramebuffer(targetIds[eye]),"Eye FBO deleted after publication");
            }
            // The game's caller may have different non-default read/draw FBOs.
            var offscreen=new LwjglGraphics();
            int read=offscreen.create(200,150),draw=offscreen.create(200,150);
            glBindFramebuffer(GL_READ_FRAMEBUFFER,read);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,draw);
            try(var capture=new CaptureOutput(offscreen,Path.of(args[0]))) {
                var saved=capture.save();capture.bind(0);saved.restore().restore();
                check(glGetInteger(GL_READ_FRAMEBUFFER_BINDING)==read && glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)==draw,"Distinct caller FBOs restored");
                check(glGetInteger(GL_READ_BUFFER)==GL_COLOR_ATTACHMENT0 && glGetInteger(GL_DRAW_BUFFER)==GL_COLOR_ATTACHMENT0,"Non-default buffer selectors restored");
            }
            check(glIsFramebuffer(read) && glIsFramebuffer(draw),"Borrowed caller FBOs never deleted");
            glBindFramebuffer(GL_FRAMEBUFFER,0);offscreen.delete(read);offscreen.delete(draw);
            check(glGetError()==GL_NO_ERROR,"No final GL errors");
            glDeleteTextures(texture);glDeleteBuffers(pack);glDeleteBuffers(unpack);
            System.out.println("Standalone GPU output: "+checks+" checks passed; "+glGetString(GL_RENDERER));
        } finally { glfwDestroyWindow(window);glfwTerminate(); }
    }
}
