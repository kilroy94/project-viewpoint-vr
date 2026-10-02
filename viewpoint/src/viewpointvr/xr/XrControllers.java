package viewpointvr.xr;
import java.util.*;
import org.lwjgl.openxr.*;
import org.lwjgl.system.MemoryStack;
import viewpointvr.input.*;
import static org.lwjgl.openxr.XR10.*;
/** Session-owned action set, adapted from the inherited OpenXR acquisition code. */
public final class XrControllers implements AutoCloseable {
 private final XrInstance instance;private final XrSession session;
 private XrActionSet set;private final Map<String,XrAction> actions=new LinkedHashMap<>();
 private final long[] paths=new long[2];private final XrSpace[][] spaces=new XrSpace[2][2];
 private long sequence;private int lastMask=-1;
 public XrControllers(XrInstance instance,XrSession session){
  this.instance=instance;this.session=session;
  try(var s=MemoryStack.stackPush()){
   var pointer=s.mallocPointer(1);
   check(xrCreateActionSet(instance,XrActionSetCreateInfo.calloc(s).type$Default().actionSetName(s.UTF8("viewpoint_controls"))
    .localizedActionSetName(s.UTF8("Viewpoint VR controls")).priority(0),pointer),"create actions");
   set=new XrActionSet(pointer.get(0),instance);
   paths[0]=path("/user/hand/left",s);paths[1]=path("/user/hand/right",s);
   for(String name:List.of("aim","grip","trigger","stick","menu")){
    int type=switch(name){case "aim","grip"->XR_ACTION_TYPE_POSE_INPUT;case "trigger"->XR_ACTION_TYPE_FLOAT_INPUT;case "stick"->XR_ACTION_TYPE_VECTOR2F_INPUT;default->XR_ACTION_TYPE_BOOLEAN_INPUT;};
    check(xrCreateAction(set,XrActionCreateInfo.calloc(s).type$Default().actionName(s.UTF8(name)).localizedActionName(s.UTF8("Viewpoint "+name)).actionType(type).subactionPaths(s.longs(paths)),pointer),"create "+name);
    actions.put(name,new XrAction(pointer.get(0),set));
   }
   for(String profile:List.of("oculus/touch_controller","valve/index_controller","microsoft/motion_controller","khr/simple_controller")){
    boolean simple=profile.startsWith("khr/");
    var bindings=XrActionSuggestedBinding.calloc(simple?8:10,s);int n=0;
    for(int hand=0;hand<2;hand++){
     String base="/user/hand/"+(hand==0?"left":"right")+"/input/";
     for(String pose:List.of("aim","grip"))bindings.get(n++).action(actions.get(pose)).binding(path(base+pose+"/pose",s));
     bindings.get(n++).action(actions.get("trigger")).binding(path(base+(simple?"select/click":"trigger/value"),s));
     if(!simple)bindings.get(n++).action(actions.get("stick")).binding(path(base+"thumbstick",s));
     String menu=profile.startsWith("oculus/")?(hand==0?"menu/click":"b/click"):profile.startsWith("valve/")?"b/click":"menu/click";
     bindings.get(n++).action(actions.get("menu")).binding(path(base+menu,s));
    }
    int result=xrSuggestInteractionProfileBindings(instance,XrInteractionProfileSuggestedBinding.calloc(s).type$Default().interactionProfile(path("/interaction_profiles/"+profile,s)).suggestedBindings(bindings));
    if(result==XR_ERROR_PATH_UNSUPPORTED)OpenXrSession.log("Controller profile unavailable: "+profile);else check(result,"suggest "+profile);
   }
   for(int hand=0;hand<2;hand++)for(int pose=0;pose<2;pose++){
    var info=XrActionSpaceCreateInfo.calloc(s).type$Default().action(actions.get(pose==0?"aim":"grip")).subactionPath(paths[hand]);info.poseInActionSpace().orientation().w(1);
    check(xrCreateActionSpace(session,info,pointer),"controller space");spaces[hand][pose]=new XrSpace(pointer.get(0),session);
   }
   check(xrAttachSessionActionSets(session,XrSessionActionSetsAttachInfo.calloc(s).type$Default().actionSets(s.pointers(set.address()))),"attach controllers");
  }catch(Throwable failure){close();throw failure;}
 }
 public void sample(XrSpace head,long time,boolean focused,MemoryStack s){
  if(!focused){if(InputBridge.state.focused())InputBridge.clear();return;}
  var active=XrActiveActionSet.calloc(1,s);active.get(0).actionSet(set).subactionPath(XR_NULL_PATH);
  int sync=xrSyncActions(session,XrActionsSyncInfo.calloc(s).type$Default().activeActionSets(active));
  if(sync==XR_SESSION_NOT_FOCUSED){if(InputBridge.state.focused())InputBridge.clear();return;}check(sync,"sync controllers");
  var left=hand(0,head,time,s);var right=hand(1,head,time,s);
  var previous=InputBridge.state;
  if((previous.left().tracked()&&!left.tracked())||(previous.right().tracked()&&!right.tracked()))InputBridge.trackingLost();
  InputBridge.state=new ControllerState(++sequence,System.nanoTime(),true,left,right);
  int mask=(left.tracked()?1:0)|(right.tracked()?2:0);
  if(mask!=lastMask){lastMask=mask;OpenXrSession.log("Controller tracking: left="+left.tracked()+", right="+right.tracked());}
 }
 private ControllerState.Hand hand(int hand,XrSpace base,long time,MemoryStack s){
  var get=XrActionStateGetInfo.calloc(s).type$Default().subactionPath(paths[hand]);
  XrCamera.Pose[] poses=new XrCamera.Pose[2];
  for(int pose=0;pose<2;pose++){
   var active=XrActionStatePose.calloc(s).type$Default();check(xrGetActionStatePose(session,get.action(actions.get(pose==0?"aim":"grip")),active),"pose state");
   if(!active.isActive())continue;
   var location=XrSpaceLocation.calloc(s).type$Default();check(xrLocateSpace(spaces[hand][pose],base,time,location),"locate controller");
   long flags=XR_SPACE_LOCATION_POSITION_VALID_BIT|XR_SPACE_LOCATION_ORIENTATION_VALID_BIT|XR_SPACE_LOCATION_POSITION_TRACKED_BIT|XR_SPACE_LOCATION_ORIENTATION_TRACKED_BIT;
   if((location.locationFlags()&flags)==flags){var p=location.pose().position$();var q=location.pose().orientation();poses[pose]=new XrCamera.Pose(p.x(),p.y(),p.z(),q.x(),q.y(),q.z(),q.w());}
  }
  var stick=XrActionStateVector2f.calloc(s).type$Default();check(xrGetActionStateVector2f(session,get.action(actions.get("stick")),stick),"stick");
  var trigger=XrActionStateFloat.calloc(s).type$Default();check(xrGetActionStateFloat(session,get.action(actions.get("trigger")),trigger),"trigger");
  var menu=XrActionStateBoolean.calloc(s).type$Default();check(xrGetActionStateBoolean(session,get.action(actions.get("menu")),menu),"menu");
  return new ControllerState.Hand(poses[0],poses[1],stick.isActive()?stick.currentState().x():0,stick.isActive()?stick.currentState().y():0,
   trigger.isActive()?trigger.currentState():0,menu.isActive()&&menu.currentState());
 }
 private long path(String name,MemoryStack s){var out=s.mallocLong(1);check(xrStringToPath(instance,s.UTF8(name),out),"input path");return out.get(0);}
 private static void check(int code,String stage){if(code!=XR_SUCCESS)throw new IllegalStateException(stage+": OpenXR "+code);}
 public void close(){
  InputBridge.clear();for(var hand:spaces)for(int i=0;i<hand.length;i++)if(hand[i]!=null){xrDestroySpace(hand[i]);hand[i]=null;}
  for(var action:actions.values())xrDestroyAction(action);actions.clear();if(set!=null){xrDestroyActionSet(set);set=null;}
 }
}
