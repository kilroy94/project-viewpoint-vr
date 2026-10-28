package viewpointvr.xr;
import java.util.*;
/** Input paths from Khronos core profiles and Valve's Steam Frame profile. */
public final class ControllerBindings {
 public static final String FRAME_EXTENSION="XR_VALVE_frame_controller_interaction";
 public static final String FRAME="valve/frame_controller_valve";
 public record Binding(String action,int hand,String input){}
 public static List<Binding> profile(String name){
  var b=new ArrayList<Binding>();boolean simple=name.startsWith("khr/"),frame=name.equals(FRAME),touch=name.startsWith("oculus/"),index=name.equals("valve/index_controller");
  for(int h=0;h<2;h++){
   b.add(new Binding("aim",h,"aim/pose"));b.add(new Binding("grip",h,"grip/pose"));b.add(new Binding("trigger",h,simple?"select/click":"trigger/value"));
   if(simple){b.add(new Binding("menu",h,"menu/click"));continue;}
   b.add(new Binding("stick",h,"thumbstick"));b.add(new Binding("button_"+(h==0?9:10),h,"thumbstick/click"));
   b.add(new Binding("squeeze",h,frame||touch||index?"squeeze/value":"squeeze/click"));
   if(frame){
    b.add(new Binding("button_"+(h==0?6:7),h,h==0?"view/click":"menu/click"));
    b.add(new Binding("button_"+(h==0?4:5),h,"shoulder/click"));
    if(h==0){String[] paths={"dpad_up","dpad_right","dpad_down","dpad_left"};for(int i=0;i<4;i++)b.add(new Binding("button_"+(11+i),h,paths[i]+"/click"));}
    else for(int i=0;i<4;i++)b.add(new Binding("button_"+i,h,new String[]{"a","b","x","y"}[i]+"/click"));
   }else if(touch||index){
    b.add(new Binding("button_"+(h==0?2:0),h,h==0&&touch?"x/click":"a/click"));
    b.add(new Binding("button_"+(h==0?3:1),h,h==0&&touch?"y/click":"b/click"));
    if(touch&&h==0)b.add(new Binding("button_7",h,"menu/click"));
   }else b.add(new Binding("button_"+(h==0?6:7),h,"menu/click"));
   b.add(new Binding("menu",h,frame?(h==0?"view/click":"b/click"):touch?(h==0?"menu/click":"b/click"):index?"b/click":"menu/click"));
  }
  return List.copyOf(b);
 }
}
