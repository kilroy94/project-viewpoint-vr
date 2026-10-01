package viewpointvr.xr;
import java.nio.*;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.WGL.*;
public final class MissingRuntimeTest {
    public static void main(String[] args) {
        String runtime=System.getenv("XR_RUNTIME_JSON");
        if(runtime==null||!runtime.endsWith("viewpoint-no-runtime.json")||java.nio.file.Files.exists(java.nio.file.Path.of(runtime)))throw new IllegalStateException("Test requires explicit nonexistent runtime override");
        if(!glfwInit())throw new IllegalStateException("GLFW");
        glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_COMPAT_PROFILE);
        long window=glfwCreateWindow(64,64,"Viewpoint missing-runtime test",0,0);
        try {
            if(window==0)throw new AssertionError("Window");glfwMakeContextCurrent(window);GL.createCapabilities();long context=wglGetCurrentContext((IntBuffer)null);
            boolean rejected=false;
            try(var session=new OpenXrSession()){throw new AssertionError("Nonexistent runtime accepted: "+session.submitted());}catch(IllegalStateException expected){rejected=true;System.out.println("Expected runtime rejection: "+expected);}
            if(!rejected||wglGetCurrentContext((IntBuffer)null)!=context)throw new AssertionError("Caller context lost");
            glClear(GL_COLOR_BUFFER_BIT);if(glGetError()!=GL_NO_ERROR)throw new AssertionError("GL error after rejected session");
            System.out.println("OpenXR missing-runtime test passed; no runtime or SteamVR launched");
        }finally{if(window!=0)glfwDestroyWindow(window);glfwTerminate();}
    }
}
