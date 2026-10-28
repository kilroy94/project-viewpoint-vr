package viewpointvr.input;
import viewpointvr.xr.*;
import org.joml.Vector3f;
import java.util.List;
public final class GameplayTest {
 static int checks;static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static void near(float a,float b,String why){check(Math.abs(a-b)<.0001,why+": "+a+" != "+b);}
 static ControllerState.Hand hand(float x,float y,float trigger,float grip,int buttons){return new ControllerState.Hand(new XrCamera.Pose(0,0,0,0,0,0,1),null,x,y,trigger,false,grip,buttons);}
 static ControllerState state(long now,ControllerState.Hand l,ControllerState.Hand r){return new ControllerState(1,now,true,l,r);}
 static List<XrCamera.View> views(XrCamera.Pose h){return List.of(new XrCamera.View(h,-.8f,.8f,.8f,-.8f),new XrCamera.View(h,-.8f,.8f,.8f,-.8f));}
 public static void main(String[] args){
  var neutral=hand(0,0,0,0,0);long now=System.nanoTime();var mapper=new GamepadMapper();
  check(!mapper.map(state(now,neutral,hand(0,0,1,0,1)),now,true).buttons()[0],"initial held input blocked");
  mapper.map(state(now,neutral,neutral),now,true);
  var p=mapper.map(state(now,hand(.6f,1,.5f,1,1<<6),hand(1,1,1,1,1|1<<2|1<<7|1<<10|1<<11|1<<14)),now,true);
  near(p.axes()[0],.6f,"left movement");near(p.axes()[1],-1,"native y");near(p.axes()[2],0,"right stick reserved");near(p.axes()[3],0,"no native pitch");near(p.axes()[4],0,"LT normalization");near(p.axes()[5],1,"RT normalization");
  check(p.buttons()[0]&&p.buttons()[2]&&p.buttons()[4]&&p.buttons()[5]&&p.buttons()[6]&&p.buttons()[7],"faces shoulders view menu");check(!p.buttons()[10],"cursor toggle reserved");check(p.hat()==9,"dpad up-left");
  check(!mapper.map(state(now,neutral,neutral),now+300_000_000L,true).buttons()[0],"stale neutral");
  check(mapper.map(state(now,neutral,hand(0,0,1,0,1)),now,true).axes()[5]==-1,"stale recovery requires neutral");
  mapper.map(state(now,neutral,neutral),now,true);check(mapper.map(state(now,neutral,hand(0,0,1,0,1)),now,true).buttons()[0],"neutral rearms");
  check(mapper.map(state(now,neutral,ControllerState.NONE),now,true).axes()[5]==-1,"one missing controller releases all gameplay");
  for(String profile:List.of("oculus/touch_controller","valve/index_controller","microsoft/motion_controller","khr/simple_controller",ControllerBindings.FRAME)){
   var bindings=ControllerBindings.profile(profile);check(bindings.stream().filter(b->b.action().equals("aim")).count()==2,"two aim bindings");
   check(bindings.stream().noneMatch(b->b.input().startsWith("system")),"system button reserved");
  }
  var frame=ControllerBindings.profile(ControllerBindings.FRAME);
  for(int i=0;i<4;i++){final int n=i;check(frame.stream().anyMatch(b->b.hand()==1&&b.action().equals("button_"+n)&&b.input().equals(new String[]{"a","b","x","y"}[n]+"/click")),"Frame four faces on right");}
  check(frame.stream().anyMatch(b->b.hand()==0&&b.input().equals("view/click")),"Frame left View");check(frame.stream().anyMatch(b->b.hand()==1&&b.input().equals("menu/click")),"Frame right Menu");
  BodyTracking.clear();BodyTracking.enabled=true;BodyTracking.head(.3f,-.4f,.5f,.2f,1);
  near(BodyTracking.yaw(1),.5f,"head yaw sign");near(BodyTracking.pitch(-1),.2f,"head pitch sign");
  var follow=BodyTracking.follow(BodyTracking.current(),0);check(follow.x>0&&follow.y>0,"physical forward-right follows");
  near(BodyTracking.follow(new BodyTracking.Head(now,.1f,0,0,0,1),0).length(),0,"lean deadzone");near(BodyTracking.follow(new BodyTracking.Head(now,2,0,0,0,1),0).length(),0,"large tracking jump not walked");
  for(float yaw:new float[]{0,.7f,-2f})for(float scale:new float[]{.5f,1,2}){
   BodyTracking.clear();var camera=new XrCamera(scale);var start=new XrCamera.Pose(0,1.7f,0,0,0,0,1);camera.eyes(start,views(start),new Vector3f(),yaw,0,true);
   var moved=new XrCamera.Pose(.3f,1.7f,-.4f,0,0,0,1);var before=camera.eyes(moved,views(moved),new Vector3f(),yaw,0,true);
   BodyTracking.consume(.07f,.03f,yaw,scale);var after=camera.eyes(moved,views(moved),new Vector3f(-.07f,0,-.03f),yaw,0,true);
   near(before.left().position().distance(after.left().position()),0,"resolved actor movement not applied twice");
   var saved=BodyTracking.offset();BodyTracking.consume(0,0,yaw,scale);check(saved.equals(BodyTracking.offset()),"blocked collision consumes nothing");
   BodyTracking.consume(10,0,yaw,scale);check(saved.equals(BodyTracking.offset()),"teleport not consumed");
   BodyTracking.suspend();check(saved.equals(BodyTracking.offset()),"focus loss preserves room calibration");
   camera.recenter();near(BodyTracking.offset().x(),0,"recenter clears body offset");
  }
  BodyTracking.clear();near(BodyTracking.yaw(.7f),.7f,"inactive preserves native yaw");
  System.out.println("Gameplay mapping/body: "+checks+" checks passed");
 }
}
