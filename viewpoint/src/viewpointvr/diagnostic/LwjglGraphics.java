package viewpointvr.diagnostic;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.*;
import viewpointvr.*;
import static org.lwjgl.opengl.GL33.*;

/** Compatibility-profile GL implementation; adapted from the inherited EyeCapture state scopes. */
public final class LwjglGraphics implements CaptureOutput.Graphics {
    private final Map<Integer,Integer> textures=new HashMap<>();
    @Override public ViewpointBackend.SavedOutput save() throws Throwable {
        check("capture boundary");
        int draw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING),read=glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int pack=glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING),unpack=glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING);
        int active=glGetInteger(GL_ACTIVE_TEXTURE);
        int[] viewport=new int[4]; glGetIntegerv(GL_VIEWPORT,viewport);
        boolean scissor=glIsEnabled(GL_SCISSOR_TEST),srgb=glIsEnabled(GL_FRAMEBUFFER_SRGB);
        var extent=new ViewpointBackend.Extent(viewport[2],viewport[3]);
        glPushAttrib(GL_ALL_ATTRIB_BITS); check("save server state");
        try { glPushClientAttrib(GL_CLIENT_PIXEL_STORE_BIT); check("save pixel state"); }
        catch(Throwable error) { glPopAttrib(); throw error; }
        return new ViewpointBackend.SavedOutput(draw,read,viewport[0],viewport[1],extent,scissor,()-> {
            // GL_COLOR_BUFFER_BIT includes draw-buffer selectors. Restore their owning FBO first:
            // replaying a saved GL_BACK selector while an eye FBO is bound is GL_INVALID_OPERATION.
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER,draw); glBindFramebuffer(GL_READ_FRAMEBUFFER,read);
            glPopClientAttrib(); glPopAttrib();
            glBindBuffer(GL_PIXEL_PACK_BUFFER,pack); glBindBuffer(GL_PIXEL_UNPACK_BUFFER,unpack);
            if(scissor) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);
            if(srgb) glEnable(GL_FRAMEBUFFER_SRGB); else glDisable(GL_FRAMEBUFFER_SRGB);
            glActiveTexture(active); check("restore output state");
        });
    }
    @Override public int create(int width,int height) throws Throwable {
        var state=save(); int texture=0,fbo=0; Throwable failure=null;
        try {
            glActiveTexture(GL_TEXTURE0); glBindBuffer(GL_PIXEL_UNPACK_BUFFER,0);
            texture=glGenTextures(); glBindTexture(GL_TEXTURE_2D,texture);
            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,width,height,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
            fbo=glGenFramebuffers(); glBindFramebuffer(GL_FRAMEBUFFER,fbo);
            glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,texture,0);
            glReadBuffer(GL_COLOR_ATTACHMENT0); glDrawBuffer(GL_COLOR_ATTACHMENT0);
            if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE) throw new IllegalStateException("Incomplete eye framebuffer");
            check("allocate eye framebuffer");
        } catch(Throwable error) { failure=error; }
        try { state.restore().restore(); } catch(Throwable error) { failure=StageHooks.append(failure,error); }
        if(failure!=null) {
            if(fbo!=0) glDeleteFramebuffers(fbo); if(texture!=0) glDeleteTextures(texture); throw failure;
        }
        textures.put(fbo,texture); return fbo;
    }
    @Override public void delete(int target) {
        Integer texture=textures.remove(target);
        if(texture!=null) { glDeleteFramebuffers(target); glDeleteTextures(texture); }
    }
    @Override public void bind(int target,int width,int height) {
        owned(target); glBindFramebuffer(GL_FRAMEBUFFER,target); glViewport(0,0,width,height);
        glDisable(GL_SCISSOR_TEST); glDisable(GL_FRAMEBUFFER_SRGB);
        glColorMask(true,true,true,true); glClearColor(0,0,0,1); glClear(GL_COLOR_BUFFER_BIT);
        check("bind eye target");
    }
    @Override public BufferedImage read(int target,int width,int height) throws Throwable {
        owned(target); var state=save();
        try {
            glBindFramebuffer(GL_READ_FRAMEBUFFER,target); glReadBuffer(GL_COLOR_ATTACHMENT0);
            glBindBuffer(GL_PIXEL_PACK_BUFFER,0); glPixelStorei(GL_PACK_ALIGNMENT,1);
            glPixelStorei(GL_PACK_ROW_LENGTH,0); glPixelStorei(GL_PACK_SKIP_ROWS,0); glPixelStorei(GL_PACK_SKIP_PIXELS,0);
            ByteBuffer pixels=ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(width,height),4));
            glReadPixels(0,0,width,height,GL_RGBA,GL_UNSIGNED_BYTE,pixels); check("read eye pixels");
            return image(pixels,width,height);
        } finally { state.restore().restore(); }
    }
    static BufferedImage image(ByteBuffer pixels,int width,int height) {
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
            int at=(y*width+x)*4;
            image.setRGB(x,height-y-1,0xff000000|(pixels.get(at)&255)<<16|(pixels.get(at+1)&255)<<8|pixels.get(at+2)&255);
        }
        return image;
    }
    @Override public void mirror(int target,int width,int height,ViewpointBackend.SavedOutput destination) throws Throwable {
        owned(target); var state=save();
        try {
            glBindFramebuffer(GL_READ_FRAMEBUFFER,target); glReadBuffer(GL_COLOR_ATTACHMENT0);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER,destination.drawFbo());
            glDisable(GL_SCISSOR_TEST); glDisable(GL_FRAMEBUFFER_SRGB);
            int x=destination.x(),y=destination.y();
            glBlitFramebuffer(0,0,width,height,x,y,x+destination.viewport().width(),y+destination.viewport().height(),GL_COLOR_BUFFER_BIT,GL_LINEAR);
            check("present captured world");
        } finally { state.restore().restore(); }
    }
    public void mirrorPair(int left,int right,int width,int height,ViewpointBackend.SavedOutput destination) throws Throwable {
        // Letterbox each eye to preserve its aspect ratio inside one desktop half.
        var state=save();
        try {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER,destination.drawFbo());
            glDisable(GL_SCISSOR_TEST);glDisable(GL_FRAMEBUFFER_SRGB);glColorMask(true,true,true,true);
            glEnable(GL_SCISSOR_TEST);glScissor(destination.x(),destination.y(),destination.viewport().width(),destination.viewport().height());
            glClearColor(0,0,0,1);glClear(GL_COLOR_BUFFER_BIT);glDisable(GL_SCISSOR_TEST);
            int half=destination.viewport().width()/2;
            for(int eye=0;eye<2;eye++) {
                int target=eye==0?left:right;owned(target);
                int slot=eye==0?half:destination.viewport().width()-half;
                float scale=Math.min((float)slot/width,(float)destination.viewport().height()/height);
                int w=Math.max(1,Math.round(width*scale)),h=Math.max(1,Math.round(height*scale));
                int x=destination.x()+eye*half+(slot-w)/2,y=destination.y()+(destination.viewport().height()-h)/2;
                glBindFramebuffer(GL_READ_FRAMEBUFFER,target);glReadBuffer(GL_COLOR_ATTACHMENT0);
                glBlitFramebuffer(0,0,width,height,x,y,x+w,y+h,GL_COLOR_BUFFER_BIT,GL_LINEAR);
            }
            check("stereo mirror");
        } finally {state.restore().restore();}
    }
    public int texture(int target) {owned(target);return textures.get(target);}
    private void owned(int target) { if(!textures.containsKey(target)) throw new IllegalArgumentException("Unowned eye target"); }
    private static void check(String stage) { int error=glGetError(); if(error!=GL_NO_ERROR) throw new IllegalStateException(stage+": GL error "+error); }
}
