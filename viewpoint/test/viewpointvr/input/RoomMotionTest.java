package viewpointvr.input;
import org.joml.Vector2f;
/** Room movement has its own collision pass and preserves native pending stick movement. */
public final class RoomMotionTest {
 static int checks;static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}static void near(float a,float b,String why){check(Math.abs(a-b)<.0001,why+": "+a);}
 static final class Body implements RoomMotion.Body {
  float x,y,nx=.2f,ny=.1f;int writes,queries;boolean blocked,loaded=true;float slide=1;
  public float x(){return x;}public float y(){return y;}public float nextX(){return nx;}public float nextY(){return ny;}
  public Vector2f resolve(float tx,float ty){queries++;return blocked?new Vector2f(x,y):new Vector2f(tx,y+(ty-y)*slide);}
  public boolean accepts(float x,float y){return loaded;}public void move(float a,float b,float c,float d){x=a;y=b;nx=c;ny=d;writes++;}
 }
 public static void main(String[] args){
  long now=System.nanoTime();var h=new BodyTracking.Head(now,.3f,-.4f,0,0,1);
  for(float yaw:new float[]{0,.7f,-2})for(float scale:new float[]{.5f,1,2}){
   BodyTracking.clear();var head=new BodyTracking.Head(now,h.x(),h.z(),0,0,scale);var step=RoomMotion.request(head,yaw);check(Math.hypot(step.x(),step.y())<=.12001,"bounded collision step");
   var body=new Body();var accepted=RoomMotion.apply(body,step);check(body.queries==1&&body.writes==1,"collision resolved once before commit");near(body.nx-body.x,.2f,"stick X retained");near(body.ny-body.y,.1f,"stick Y retained");
   BodyTracking.consume(accepted.x(),accepted.y(),yaw,scale);var offset=BodyTracking.offset();check(offset.x()*head.x()+offset.z()*head.z()>0,"consumed movement heads toward headset");
  }
  var body=new Body();body.blocked=true;check(RoomMotion.apply(body,new RoomMotion.Step(.1f,0)).equals(RoomMotion.Step.NONE)&&body.writes==0,"blocked movement never committed");
  body.blocked=false;body.loaded=false;check(RoomMotion.apply(body,new RoomMotion.Step(.1f,0)).equals(RoomMotion.Step.NONE)&&body.writes==0,"unloaded/unsafe tile rejected");
  body.loaded=true;body.slide=0;var slide=RoomMotion.apply(body,new RoomMotion.Step(.05f,.05f));near(slide.x(),.05f,"slide accepted X");near(slide.y(),0,"blocked Y not consumed");
  check(RoomMotion.request(new BodyTracking.Head(now,.02f,0,0,0,1),0).equals(RoomMotion.Step.NONE),"lean allowance");check(RoomMotion.request(new BodyTracking.Head(now,2,0,0,0,1),0).equals(RoomMotion.Step.NONE),"tracking discontinuity rejected");
  BodyTracking.clear();BodyTracking.head(.5f,0,0,0,1);Object frame=new Object();BodyTracking.snapshot(frame);var frozen=BodyTracking.offsetFor(frame);BodyTracking.consume(.1f,0,0,1);check(BodyTracking.offsetFor(frame).equals(frozen)&&!BodyTracking.offset().equals(frozen),"render compensation belongs to actor snapshot, not a newer simulation tick");
  long calibration=BodyTracking.calibration();BodyTracking.suspend();check(BodyTracking.calibration()==calibration,"focus loss retains hand calibration");BodyTracking.clear();check(BodyTracking.calibration()!=calibration,"explicit recenter resets calibration");
  System.out.println("Independent room collision: "+checks+" checks passed");
 }
}
