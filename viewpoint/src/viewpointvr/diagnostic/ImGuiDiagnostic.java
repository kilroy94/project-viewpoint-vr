package viewpointvr.diagnostic;
import imgui.ImGui;
/** Widgets run inside Viewpoint's actual settings context via its pinned panel registration API. */
public final class ImGuiDiagnostic {
 private static final float[] value={.25f};private static boolean held;private static float scroll;private static int clicks;private static boolean resetScroll;
 public static void install(ClassLoader loader)throws ReflectiveOperationException{
  Class.forName("viewpoint.platform.SettingsWindow",false,loader).getMethod("panel",String.class,Runnable.class).invoke(null,"VR/Controller diagnostic",(Runnable)ImGuiDiagnostic::draw);
 }
 private static void draw(){
  try{
   ImGui.textUnformatted("Null-headset UI test. Controllers Off; release cursor. Keep this section visible.");
   boolean start=ImGui.button("Start scripted UI test");ImGui.sameLine();if(ImGui.button("Stop test"))ControllerDiagnostic.cancel("Stopped by user");
   ImGui.textUnformatted(ControllerDiagnostic.status());
   if(ImGui.button("Test click ("+clicks+")###vr-test-click",260,30)){clicks++;ControllerDiagnostic.event("viewpoint","click",clicks);}
   float cx=(ImGui.getItemRectMinX()+ImGui.getItemRectMaxX())/2,cy=(ImGui.getItemRectMinY()+ImGui.getItemRectMaxY())/2;
   ImGui.pushItemWidth(260);
   if(ImGui.sliderFloat("Test drag###vr-test-drag",value,0,1))ControllerDiagnostic.event("viewpoint","drag",value[0]);
   float dx=ImGui.getItemRectMinX()+50,ex=ImGui.getItemRectMinX()+200,dy=(ImGui.getItemRectMinY()+ImGui.getItemRectMaxY())/2;
   boolean active=ImGui.isItemActive();if(held&&!active)ControllerDiagnostic.event("viewpoint","release",value[0]);held=active;ImGui.popItemWidth();
   ImGui.beginChild("vr-test-scroll",300,100,true);
   if(resetScroll){ImGui.setScrollY(0);resetScroll=false;}
   float sx=ImGui.getWindowPosX()+100,sy=ImGui.getWindowPosY()+45;
   float next=ImGui.getScrollY();if(next!=scroll){ControllerDiagnostic.event("viewpoint","scroll",next);scroll=next;}
   for(int i=0;i<35;i++)ImGui.textUnformatted("Diagnostic scroll row "+i);
   ImGui.endChild();
   var io=ImGui.getIO();Main.diagnosticTargets("viewpoint",cx,cy,dx,dy,ex,dy,sx,sy,io.getDisplaySizeX(),io.getDisplaySizeY());
   if(start){value[0]=.25f;resetScroll=true;ControllerDiagnostic.start("viewpoint",System.nanoTime());}
  }catch(RuntimeException error){ControllerDiagnostic.cancel("Diagnostic widget error: "+error);}
 }
}
