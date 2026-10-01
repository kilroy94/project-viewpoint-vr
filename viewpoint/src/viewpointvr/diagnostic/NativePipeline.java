package viewpointvr.diagnostic;

import viewpointvr.*;
import viewpointvr.xr.*;

public final class NativePipeline implements RuntimeDriver.Pipeline {
    private final ViewpointBackend.Access access;
    private LiveOutput output;
    private OpenXrSession xr;
    private UiCapture ui;
    private XrCamera camera=new XrCamera(1);
    public NativePipeline(ViewpointBackend.Access access){this.access=access;}
    private LiveOutput output(){if(output==null)output=new LiveOutput(new LwjglGraphics());return output;}
    public void scale(float value){camera=new XrCamera(value);}
    public boolean eligible(Object drawer) throws Throwable{return ViewpointBackend.eligible(access,drawer);}
    public boolean desktop(Object drawer,FrameBoundary.NativeDraw original) throws Throwable {
        var out=output();out.begin(0,0,null);ViewpointBackend.synthetic(access,out,.064f).render(drawer,original);return out.published();
    }
    public boolean xr(Object drawer,FrameBoundary.NativeDraw original,boolean tracked,boolean recenter) throws Throwable {
        if(xr==null)try{xr=new OpenXrSession();ui=new UiCapture();UiBridge.sink(ui);camera.recenter();}catch(Throwable error){throw new RuntimeDriver.Unavailable(error);}
        if(recenter)camera.recenter();
        boolean[] consumed={false};
        boolean rendered;
        try {rendered=xr.frame((head,views,sink)->{
            if(xr.consumeRecenter())camera.recenter();
            var out=output();float factor=Math.min(1f,2048f/Math.max(xr.width(),xr.height()));
            out.begin(Math.max(1,Math.round(xr.width()*factor)),Math.max(1,Math.round(xr.height()*factor)),sink::copy);
            consumed[0]=true;
            ViewpointBackend.cameras(access,out,(center,yaw,pitch,fov)->camera.eyes(head,views,center,yaw,pitch,tracked)).render(drawer,original);
            if(!out.published())throw new IllegalStateException("Native scene declined XR pair");
        },ui.image());
        }catch(Throwable error){if(!consumed[0])throw new RuntimeDriver.Unavailable(error);throw error;}
        if(!consumed[0])original.draw();
        return rendered;
    }
    public void idle() throws Throwable{if(xr!=null)xr.frame(null,ui.image());}
    public void close() throws Throwable {
        Throwable failure=null;
        UiBridge.sink(null);
        try{if(ui!=null)ui.close();}catch(Throwable error){failure=error;}finally{ui=null;}
        try{if(xr!=null)xr.close();}catch(Throwable error){failure=StageHooks.append(failure,error);}finally{xr=null;}
        try{if(output!=null)output.close();}catch(Throwable error){failure=StageHooks.append(failure,error);}finally{output=null;}
        camera.recenter();if(failure!=null)throw failure;
    }
}
