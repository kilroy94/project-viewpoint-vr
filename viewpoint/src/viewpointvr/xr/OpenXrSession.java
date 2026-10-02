package viewpointvr.xr;

import java.nio.*;
import java.util.*;
import org.lwjgl.PointerBuffer;
import viewpointvr.diagnostic.UiCapture;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.openxr.XR10.*;
import static org.lwjgl.openxr.KHROpenGLEnable.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.WGL.*;

/** Owns XR handles only; borrows the calling thread's existing WGL context. */
public final class OpenXrSession implements AutoCloseable {
    @FunctionalInterface public interface Renderer { void render(XrCamera.Pose head,List<XrCamera.View> views,Copy sink) throws Throwable; }
    @FunctionalInterface public interface Copy { void copy(int eye,int source,int width,int height); }
    private XrInstance instance;
    private XrSession session;
    private XrSpace local,head;
    private XrControllers controllers;
    private long system,format,context,dc;
    private final Thread owner=Thread.currentThread();
    private final Eye[] eyes=new Eye[2];
    private boolean running,ended,closed,focused;
    private long recenterTime=Long.MAX_VALUE;
    private boolean recenter;
    private int fbo,maxUiWidth,maxUiHeight;
    private Eye ui;
    private long uiSubmitted;
    private long submitted,frames,skipped;
    public int width() {return Math.max(eyes[0].width,eyes[1].width);}
    public int height() {return Math.max(eyes[0].height,eyes[1].height);}
    private static final class Eye { XrSwapchain handle; XrSwapchainImageOpenGLKHR.Buffer images; int width,height; }
    public OpenXrSession() {
        try { initialize(); } catch(Throwable failure) {try{close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}throw failure;}
    }
    public long submitted() { return submitted; }
    public boolean consumeRecenter() { boolean value=recenter; recenter=false; return value; }
    public static void log(String message) { System.out.println("[Project Viewpoint VR OpenXR] "+message); }
    private static void check(int result,String action) {
        if(result<0 || result==XR_SESSION_LOSS_PENDING) throw new IllegalStateException(action+": OpenXR result "+result);
    }
    private void current() {
        if(Thread.currentThread()!=owner || wglGetCurrentContext((IntBuffer)null)!=context || wglGetCurrentDC()!=dc)
            throw new IllegalStateException("OpenXR must use its original render thread and WGL context");
    }
    private void initialize() {
        context=wglGetCurrentContext((IntBuffer)null); dc=wglGetCurrentDC();
        if(context==0 || dc==0) throw new IllegalStateException("No current WGL context");
        // Do not destroy LWJGL's process-wide XR loader; other mods may share it.
        XR.getFunctionProvider();
        try(MemoryStack s=stackPush()) {
            XrInstanceCreateInfo create=XrInstanceCreateInfo.calloc(s).type$Default()
                .enabledExtensionNames(s.pointers(s.UTF8(XR_KHR_OPENGL_ENABLE_EXTENSION_NAME)));
            create.applicationInfo().applicationName(s.UTF8("Project Viewpoint VR")).applicationVersion(3)
                .engineName(s.UTF8("Viewpoint adapter")).engineVersion(1).apiVersion(XR_MAKE_VERSION(1,0,0));
            PointerBuffer pointer=s.mallocPointer(1);
            check(xrCreateInstance(create,pointer),"create instance"); instance=new XrInstance(pointer.get(0),create);
            XrInstanceProperties properties=XrInstanceProperties.calloc(s).type$Default();
            check(xrGetInstanceProperties(instance,properties),"runtime properties"); log("Runtime: "+properties.runtimeNameString());
            LongBuffer id=s.mallocLong(1);
            check(xrGetSystem(instance,XrSystemGetInfo.calloc(s).type$Default().formFactor(XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY),id),"get HMD system"); system=id.get(0);
            XrSystemProperties systemProperties=XrSystemProperties.calloc(s).type$Default();
            check(xrGetSystemProperties(instance,system,systemProperties),"system properties");
            if(systemProperties.graphicsProperties().maxLayerCount()<2)throw new IllegalStateException("Runtime requires two composition layers for UI");
            maxUiWidth=systemProperties.graphicsProperties().maxSwapchainImageWidth();
            maxUiHeight=systemProperties.graphicsProperties().maxSwapchainImageHeight();
            XrGraphicsRequirementsOpenGLKHR requirements=XrGraphicsRequirementsOpenGLKHR.calloc(s).type$Default();
            check(xrGetOpenGLGraphicsRequirementsKHR(instance,system,requirements),"graphics requirements");
            long version=XR_MAKE_VERSION(glGetInteger(GL_MAJOR_VERSION),glGetInteger(GL_MINOR_VERSION),0);
            if(version<requirements.minApiVersionSupported() || version>requirements.maxApiVersionSupported()) throw new IllegalStateException("Existing GL context outside runtime requirements");
            XrGraphicsBindingOpenGLWin32KHR binding=XrGraphicsBindingOpenGLWin32KHR.calloc(s).type$Default().hDC(dc).hGLRC(context);
            check(xrCreateSession(instance,XrSessionCreateInfo.calloc(s).type$Default().systemId(system).next(binding.address()),pointer),"create session");
            session=new XrSession(pointer.get(0),instance);
            local=space(XR_REFERENCE_SPACE_TYPE_LOCAL,s); head=space(XR_REFERENCE_SPACE_TYPE_VIEW,s);
            createSwapchains(s); fbo=glGenFramebuffers();
            try{controllers=new XrControllers(instance,session);}catch(RuntimeException error){log("Controllers unavailable; world rendering retained: "+error);}
            log("Session created on existing context; waiting for READY");
        }
    }
    private XrSpace space(int type,MemoryStack s) {
        XrReferenceSpaceCreateInfo info=XrReferenceSpaceCreateInfo.calloc(s).type$Default().referenceSpaceType(type);
        info.poseInReferenceSpace().orientation().w(1);
        PointerBuffer p=s.mallocPointer(1); check(xrCreateReferenceSpace(session,info,p),"create reference space");
        return new XrSpace(p.get(0),session);
    }
    private void createSwapchains(MemoryStack s) {
        IntBuffer count=s.mallocInt(1);
        check(xrEnumerateViewConfigurationViews(instance,system,XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO,count,null),"count views");
        if(count.get(0)!=2) throw new IllegalStateException("Requires two PRIMARY_STEREO views");
        XrViewConfigurationView.Buffer config=XrViewConfigurationView.calloc(2,s);
        for(int i=0;i<2;i++) config.get(i).type$Default();
        check(xrEnumerateViewConfigurationViews(instance,system,XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO,count,config),"view configuration");
        check(xrEnumerateEnvironmentBlendModes(instance,system,XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO,count,null),"count blend modes");
        IntBuffer modes=s.mallocInt(count.get(0)); check(xrEnumerateEnvironmentBlendModes(instance,system,XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO,count,modes),"blend modes");
        boolean opaque=false; for(int i=0;i<modes.capacity();i++) opaque|=modes.get(i)==XR_ENVIRONMENT_BLEND_MODE_OPAQUE;
        if(!opaque) throw new IllegalStateException("Requires opaque environment blend mode");
        check(xrEnumerateSwapchainFormats(session,count,null),"count formats");
        LongBuffer formats=s.mallocLong(count.get(0)); check(xrEnumerateSwapchainFormats(session,count,formats),"formats");
        for(long candidate:new long[]{GL_SRGB8_ALPHA8,GL_RGBA8}) {
            for(int i=0;i<formats.capacity();i++) if(formats.get(i)==candidate) format=candidate;
            if(format!=0) break;
        }
        if(format==0) throw new IllegalStateException("No supported 8-bit color swapchain format");
        for(int i=0;i<2;i++) {
            Eye eye=new Eye(); eyes[i]=eye;
            eye.width=config.get(i).recommendedImageRectWidth(); eye.height=config.get(i).recommendedImageRectHeight();
            allocate(eye,s);
            log("Eye "+i+": "+eye.width+"x"+eye.height+", images="+eye.images.capacity()+", format="+format);
        }
    }
    private void allocate(Eye eye,MemoryStack s) {
        IntBuffer count=s.mallocInt(1);
            XrSwapchainCreateInfo info=XrSwapchainCreateInfo.calloc(s).type$Default()
                .usageFlags(XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT|XR_SWAPCHAIN_USAGE_SAMPLED_BIT)
                .format(format).sampleCount(1).width(eye.width).height(eye.height).faceCount(1).arraySize(1).mipCount(1);
            PointerBuffer pointer=s.mallocPointer(1); check(xrCreateSwapchain(session,info,pointer),"create swapchain");
            eye.handle=new XrSwapchain(pointer.get(0),session);
            check(xrEnumerateSwapchainImages(eye.handle,count,null),"count images");
            eye.images=XrSwapchainImageOpenGLKHR.calloc(count.get(0));
            for(int j=0;j<eye.images.capacity();j++) eye.images.get(j).type$Default();
            check(xrEnumerateSwapchainImages(eye.handle,count,XrSwapchainImageBaseHeader.create(eye.images)),"swapchain images");
    }
    private void events(MemoryStack s) {
        XrEventDataBuffer event=XrEventDataBuffer.calloc(s);
        for(;;) {
            event.clear(); event.type$Default(); int result=xrPollEvent(instance,event);
            if(result==XR_EVENT_UNAVAILABLE) return;
            check(result,"poll event");
            if(event.type()==XR_TYPE_EVENT_DATA_INSTANCE_LOSS_PENDING) { ended=true; return; }
            if(event.type()==XR_TYPE_EVENT_DATA_REFERENCE_SPACE_CHANGE_PENDING) {
                XrEventDataReferenceSpaceChangePending change=XrEventDataReferenceSpaceChangePending.create(event.address());
                if(change.session()==session.address() && change.referenceSpaceType()==XR_REFERENCE_SPACE_TYPE_LOCAL) recenterTime=change.changeTime();
            }
            if(event.type()!=XR_TYPE_EVENT_DATA_SESSION_STATE_CHANGED) continue;
            XrEventDataSessionStateChanged changed=XrEventDataSessionStateChanged.create(event.address());
            if(changed.session()!=session.address()) continue;
            focused=changed.state()==XR_SESSION_STATE_FOCUSED;
            log("Session state="+changed.state());
            switch(changed.state()) {
                case XR_SESSION_STATE_READY -> {
                    check(xrBeginSession(session,XrSessionBeginInfo.calloc(s).type$Default().primaryViewConfigurationType(XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO)),"begin session"); running=true;
                }
                case XR_SESSION_STATE_STOPPING -> { check(xrEndSession(session),"end session"); running=false; ended=true; }
                case XR_SESSION_STATE_EXITING,XR_SESSION_STATE_LOSS_PENDING -> { running=false; ended=true; }
                default -> { }
            }
        }
    }
    public boolean frame(Renderer renderer) throws Throwable {return frame(renderer,null);}
    public boolean frame(Renderer renderer,UiCapture.Image panel) throws Throwable {
        current(); if(closed) throw new IllegalStateException("Session closed");

        boolean rendered=false;

        try(MemoryStack s=stackPush()) {
            events(s); if(!running)viewpointvr.input.InputBridge.clear(); if(ended) throw new IllegalStateException("Runtime session stopped or lost");
            if(!running) { return false; }
            XrFrameState state=XrFrameState.calloc(s).type$Default();

            check(xrWaitFrame(session,XrFrameWaitInfo.calloc(s).type$Default(),state),"wait frame");

            check(xrBeginFrame(session,XrFrameBeginInfo.calloc(s).type$Default()),"begin frame"); frames++;
            XrFrameEndInfo end=XrFrameEndInfo.calloc(s).type$Default().displayTime(state.predictedDisplayTime()).environmentBlendMode(XR_ENVIRONMENT_BLEND_MODE_OPAQUE);
            Throwable failure=null;
            try {
                if(controllers!=null)try{controllers.sample(head,state.predictedDisplayTime(),focused&&state.shouldRender(),s);}
                catch(RuntimeException error){log("Controllers disabled: "+error);controllers.close();controllers=null;}
                if(state.shouldRender() && renderer!=null) {

                    XrView.Buffer views=XrView.calloc(2,s); for(int i=0;i<2;i++) views.get(i).type$Default();
                    XrViewState validity=XrViewState.calloc(s).type$Default(); IntBuffer count=s.mallocInt(1);
                    check(xrLocateViews(session,XrViewLocateInfo.calloc(s).type$Default().viewConfigurationType(XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO)
                        .displayTime(state.predictedDisplayTime()).space(local),validity,count,views),"locate views");
                    XrSpaceLocation location=XrSpaceLocation.calloc(s).type$Default();
                    check(xrLocateSpace(head,local,state.predictedDisplayTime(),location),"locate head");
                    long required=XR_VIEW_STATE_ORIENTATION_VALID_BIT|XR_VIEW_STATE_POSITION_VALID_BIT;
                    long headRequired=XR_SPACE_LOCATION_ORIENTATION_VALID_BIT|XR_SPACE_LOCATION_POSITION_VALID_BIT;
                    long headTracked=XR_SPACE_LOCATION_ORIENTATION_TRACKED_BIT|XR_SPACE_LOCATION_POSITION_TRACKED_BIT;

                    if(count.get(0)==2 && (validity.viewStateFlags()&(required|XR_VIEW_STATE_ORIENTATION_TRACKED_BIT|XR_VIEW_STATE_POSITION_TRACKED_BIT))==(required|XR_VIEW_STATE_ORIENTATION_TRACKED_BIT|XR_VIEW_STATE_POSITION_TRACKED_BIT) && (location.locationFlags()&(headRequired|headTracked))==(headRequired|headTracked)) {
                        if(state.predictedDisplayTime()>=recenterTime) { recenter=true; recenterTime=Long.MAX_VALUE; }
                        List<XrCamera.View> samples=new ArrayList<>();
                        for(int i=0;i<2;i++) { var v=views.get(i); var f=v.fov(); samples.add(new XrCamera.View(pose(v.pose()),f.angleLeft(),f.angleRight(),f.angleUp(),f.angleDown())); }
                        boolean[] copied=new boolean[2];

                        renderer.render(pose(location.pose()),List.copyOf(samples),(eye,source,w,h)-> {
                            if(eye<0 || eye>1 || copied[eye]) throw new IllegalStateException("Duplicate/invalid XR eye");
                            copy(eyes[eye],source,w,h,s,samples.get(eye)); copied[eye]=true;
                        });
                        if(!copied[0] || !copied[1]) throw new IllegalStateException("Incomplete XR pair");
                        XrCompositionLayerProjectionView.Buffer projection=XrCompositionLayerProjectionView.calloc(2,s);
                        for(int i=0;i<2;i++) {
                            var view=projection.get(i).type$Default().pose(views.get(i).pose()).fov(views.get(i).fov());
                            view.subImage().swapchain(eyes[i].handle).imageArrayIndex(0);
                            view.subImage().imageRect().offset().set(0,0); view.subImage().imageRect().extent().set(eyes[i].width,eyes[i].height);
                        }
                        var layer=XrCompositionLayerProjection.calloc(s).type$Default().space(local).views(projection);
                        end.layers(s.pointers(layer.address()));
                        rendered=true;
                    }
                }
                if(state.shouldRender()&&panel!=null){
                    var layer=uiLayer(panel,s);
                    long world=end.layers()==null?0:end.layers().get(0);
                    end.layers(world==0?s.pointers(layer.address()):s.pointers(world,layer.address()));
                    uiSubmitted++;if(uiSubmitted==1||uiSubmitted%600==0)log("UI panels submitted="+uiSubmitted);
                }
            } catch(Throwable error){failure=error;throw error;} finally {
                try{check(xrEndFrame(session,end),"end frame");}catch(Throwable cleanup){if(failure!=null)failure.addSuppressed(cleanup);else throw cleanup;}
            }
            if(rendered) { submitted++; if(submitted==1 || submitted%600==0) log("Projection pairs submitted="+submitted); }
            else skipped++;
            return rendered;
        }
    }
    private XrCompositionLayerQuad uiLayer(UiCapture.Image panel,MemoryStack s){
        float scale=Math.min(1f,Math.min((float)maxUiWidth/panel.width(),(float)maxUiHeight/panel.height()));
        int w=Math.max(1,Math.round(panel.width()*scale)),h=Math.max(1,Math.round(panel.height()*scale));
        if(ui==null||ui.width!=w||ui.height!=h){
            destroy(ui);ui=new Eye();ui.width=w;ui.height=h;allocate(ui,s);
            log("UI swapchain: "+w+"x"+h);
        }
        copy(ui,panel.framebuffer(),panel.width(),panel.height(),s,null);
        var layer=XrCompositionLayerQuad.calloc(s).type$Default().space(head)
            .layerFlags(XR_COMPOSITION_LAYER_BLEND_TEXTURE_SOURCE_ALPHA_BIT).eyeVisibility(XR_EYE_VISIBILITY_BOTH);
        layer.pose().orientation().w(1);layer.pose().position$().set(0,0,-1.5f);
        float aspect=(float)panel.width()/panel.height(),width=Math.min(2f,1.3f*aspect);
        layer.size().set(width,width/aspect);
        layer.subImage().swapchain(ui.handle).imageArrayIndex(0);
        layer.subImage().imageRect().offset().set(0,0);layer.subImage().imageRect().extent().set(w,h);
        return layer;
    }
    private static XrCamera.Pose pose(XrPosef pose) {
        var p=pose.position$(); var q=pose.orientation(); return new XrCamera.Pose(p.x(),p.y(),p.z(),q.x(),q.y(),q.z(),q.w());
    }
    private void copy(Eye eye,int source,int width,int height,MemoryStack s,XrCamera.View view) {
        IntBuffer image=s.mallocInt(1);

        check(xrAcquireSwapchainImage(eye.handle,XrSwapchainImageAcquireInfo.calloc(s).type$Default(),image),"acquire image");
        int wait=xrWaitSwapchainImage(eye.handle,XrSwapchainImageWaitInfo.calloc(s).type$Default().timeout(XR_INFINITE_DURATION));
        // A positive timeout is NOT ownership. On failure the caller destroys the session.
        if(wait!=XR_SUCCESS) throw new IllegalStateException("wait image: OpenXR result "+wait);

        int read=glGetInteger(GL_READ_FRAMEBUFFER_BINDING),draw=glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        boolean scissor=glIsEnabled(GL_SCISSOR_TEST),srgb=glIsEnabled(GL_FRAMEBUFFER_SRGB);
        try {
            glDisable(GL_SCISSOR_TEST); glDisable(GL_FRAMEBUFFER_SRGB);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER,fbo);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,eye.images.get(image.get(0)).image(),0);
            glDrawBuffer(GL_COLOR_ATTACHMENT0);
            if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE) throw new IllegalStateException("XR FBO incomplete");
            glBindFramebuffer(GL_READ_FRAMEBUFFER,source);
            // Preserve display-encoded PZ color bytes; physical gamma still needs headset validation.
            int[] crop=view==null?new int[]{0,0,width,height}:view.crop(width,height);
            glReadBuffer(GL_COLOR_ATTACHMENT0);
            glBlitFramebuffer(crop[0],crop[1],crop[2],crop[3],0,0,eye.width,eye.height,GL_COLOR_BUFFER_BIT,GL_LINEAR);
            int error=glGetError(); if(error!=GL_NO_ERROR) throw new IllegalStateException("XR copy GL error "+error);
        } finally {
            glFlush();
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER,fbo); glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,0,0);
            glBindFramebuffer(GL_READ_FRAMEBUFFER,read); glBindFramebuffer(GL_DRAW_FRAMEBUFFER,draw);
            if(scissor) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);
            if(srgb) glEnable(GL_FRAMEBUFFER_SRGB); else glDisable(GL_FRAMEBUFFER_SRGB);
            check(xrReleaseSwapchainImage(eye.handle,XrSwapchainImageReleaseInfo.calloc(s).type$Default()),"release image");
        }
    }
    @Override public void close() {
        if(closed) return;
        if(context!=0) current();
        closed=true;
        // Called only on owner/context thread, outside frame/texture operations. Session destruction
        // is legal in every state; do not block game teardown waiting for another event/draw.
        if(session!=null && running) cleanup(xrRequestExitSession(session),"request exit");
        if(controllers!=null){controllers.close();controllers=null;}
        if(fbo!=0) { glDeleteFramebuffers(fbo); fbo=0; }
        for(Eye eye:eyes) destroy(eye);
        destroy(ui);ui=null;
        if(head!=null) cleanup(xrDestroySpace(head),"destroy head space");
        if(local!=null) cleanup(xrDestroySpace(local),"destroy local space");
        if(session!=null) cleanup(xrDestroySession(session),"destroy session");
        if(instance!=null) cleanup(xrDestroyInstance(instance),"destroy instance");
        log("Closed; frames="+frames+", submitted="+submitted+", skipped="+skipped);
    }
    private static void destroy(Eye eye) {
        if(eye==null) return;
        if(eye.handle!=null) cleanup(xrDestroySwapchain(eye.handle),"destroy swapchain");
        if(eye.images!=null) eye.images.free();
    }
    private static void cleanup(int result,String operation) { if(result<0) log(operation+": "+result); }
}
