package viewpointvr.input;
/** One game-tick decision, with neutral rearming after focus/tracking/context changes. */
public final class ControllerLogic {
 public record Output(PanelRay.Hit hit,boolean down,int scroll,float moveX,float moveY,float turn,boolean menu) {
  public static final Output NONE=new Output(null,false,0,0,0,0,false);
 }
 private int previousMode=-1,previousContext=-1;
 private boolean clickArmed,moveArmed,menuArmed,scrollArmed,held;
 private long lastScroll;
 private final TurnFilter turning=new TurnFilter();
 private TurnSettings turnSettings=TurnSettings.DEFAULT;
 public void reset(){turning.reset();clickArmed=moveArmed=menuArmed=scrollArmed=held=false;previousContext=-1;lastScroll=0;}
 public Output step(ControllerState state,long now,int mode,boolean released,boolean gameplay,float aspect){
  return step(state,now,mode,released,gameplay,aspect,TurnSettings.DEFAULT);
 }
 public Output step(ControllerState state,long now,int mode,boolean released,boolean gameplay,float aspect,TurnSettings settings){
  if(!settings.equals(turnSettings)){reset();turnSettings=settings;}
  int context=(released?1:0)|(gameplay?2:0);
  if(mode!=previousMode||context!=previousContext){reset();previousMode=mode;previousContext=context;}
  if(mode==0||!state.fresh(now)){reset();return Output.NONE;}
  var right=state.right();var left=state.left();
  if(!right.tracked()){turning.reset();clickArmed=menuArmed=scrollArmed=held=false;}
  if(!left.tracked())moveArmed=false;
  boolean menu=false;
  if(right.tracked()){
   if(!right.menu())menuArmed=true;
   else if(menuArmed){menu=true;menuArmed=false;}
  }
  var hit=released&&right.tracked()?PanelRay.hit(right.aim(),aspect):null;
  boolean down=false;int scroll=0;float mx=0,my=0,turn=0;
  if(hit==null){clickArmed=false;scrollArmed=false;held=false;}
  else {
   if(right.trigger()<.25f){clickArmed=true;held=false;}
   if(clickArmed&&right.trigger()>.65f)held=true;
   down=held;
   if(Math.abs(right.y())<.25f)scrollArmed=true;
   if(scrollArmed&&Math.abs(right.y())>.6f&&now-lastScroll>=180_000_000L){scroll=right.y()>0?1:-1;lastScroll=now;}
  }
  if(mode==2&&gameplay&&!released){
   if(left.tracked()){
    float length=(float)Math.hypot(left.x(),left.y());
    if(length<.2f)moveArmed=true;
    if(moveArmed&&length>.2f){float scale=Math.min(1,(length-.2f)/.8f)/length;mx=left.x()*scale;my=left.y()*scale;}
   }
   turn=turning.update(now,right.x(),right.y(),right.tracked(),settings.mode(),settings.angle(),settings.speed());
  }else{moveArmed=false;turning.reset();}
  return new Output(hit,down,scroll,mx,my,turn,menu);
 }
}
