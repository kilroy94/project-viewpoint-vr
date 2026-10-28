package viewpointvr.input;
import viewpointvr.xr.XrCamera.Pose;
public record ControllerState(long sequence,long time,boolean focused,Hand left,Hand right) {
 public record Hand(Pose aim,Pose grip,float x,float y,float trigger,boolean menu,float squeeze,int buttons) {
  public Hand(Pose aim,Pose grip,float x,float y,float trigger,boolean menu){this(aim,grip,x,y,trigger,menu,0,0);}
  public Hand {
   if(!Float.isFinite(x+y+trigger+squeeze)){aim=null;grip=null;x=y=trigger=0;menu=false;squeeze=0;buttons=0;}
   x=Math.max(-1,Math.min(1,x));y=Math.max(-1,Math.min(1,y));trigger=Math.max(0,Math.min(1,trigger));
   squeeze=Math.max(0,Math.min(1,squeeze));buttons&=0x7fff;
  }
  public boolean button(int index){return (buttons&(1<<index))!=0;}
  public boolean tracked(){return aim!=null;}
 }
 public static final Hand NONE=new Hand(null,null,0,0,0,false);
 public static final ControllerState EMPTY=new ControllerState(0,0,false,NONE,NONE);
 public boolean fresh(long now){return focused&&now-time>=0&&now-time<250_000_000L;}
}
