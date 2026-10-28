package viewpointvr.input;
/** Conventional native button mapping; right stick belongs exclusively to VR turning. */
public final class GamepadMapper {
 public record Pad(float[] axes,boolean[] buttons,int hat) {}
 private boolean armed,lb,rb;
 public void invalidate(){armed=lb=rb=false;}
 public static Pad neutral(){return new Pad(new float[]{0,0,0,0,-1,-1},new boolean[15],0);}
 public Pad map(ControllerState state,long now,boolean enabled){
  if(!enabled||!state.fresh(now)||!state.left().tracked()||!state.right().tracked()){invalidate();return neutral();}
  var l=state.left();var r=state.right();
  int buttons=l.buttons()|r.buttons();
  if(!armed){armed=Math.hypot(l.x(),l.y())<.2&&Math.hypot(r.x(),r.y())<.2&&l.trigger()<.25&&r.trigger()<.25&&l.squeeze()<.25&&r.squeeze()<.25&&buttons==0;return neutral();}
  float[] axes={l.x(),-l.y(),0,0,2*l.trigger()-1,2*r.trigger()-1};
  boolean[] b=new boolean[15];for(int i=0;i<15;i++)b[i]=(buttons&(1<<i))!=0;
  lb=l.squeeze()>=(lb?.45f:.65f);rb=r.squeeze()>=(rb?.45f:.65f);
  b[4]|=lb;b[5]|=rb;b[10]=false; // R3 toggles cursor, never also a native action.
  int hat=(b[11]?1:0)|(b[12]?2:0)|(b[13]?4:0)|(b[14]?8:0);
  return new Pad(axes,b,hat);
 }
}
