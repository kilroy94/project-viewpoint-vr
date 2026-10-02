package viewpointvr.input;
/** Simulated monotonic time, no game/runtime initialization. */
public final class TurningTest {
 static int checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static void near(float a,float b,String s){check(Math.abs(a-b)<.0001f,s+": "+a);}
 static float degrees(float v){return (float)Math.toDegrees(v);}
 static float sweep(int hz,int speed){var f=new TurnFilter();long start=1_000_000_000L;f.update(start,0,0,true,2,30,speed);float sum=0;for(int i=1;i<=hz;i++)sum+=degrees(f.update(start+Math.round(i*1e9/hz),1,0,true,2,30,speed));return sum;}
 public static void main(String[] args){
  for(int angle:new int[]{15,30,45,60,90})for(int direction:new int[]{-1,1}){
   var f=new TurnFilter();long t=1_000_000_000L;
   near(f.update(t,direction,0,true,1,angle,90),0,"enable held blocked");
   f.update(t+10_000_000,0,0,true,1,angle,90);
   near(degrees(f.update(t+20_000_000,direction,0,true,1,angle,90)),direction*angle,"selected snap");
   near(f.update(t+30_000_000,direction,0,true,1,angle,90),0,"snap once");
  }
  for(int speed:new int[]{30,90,240})for(int hz:new int[]{30,60,120})near(sweep(hz,speed),speed,"frame-rate independent smooth speed");
  var f=new TurnFilter();long t=1_000_000_000L;f.update(t,0,0,true,2,30,90);
  near(degrees(f.update(t+20_000_000,-1,0,true,2,30,90)),-1.8f,"smooth left");
  near(degrees(f.update(t+40_000_000,.65f,0,true,2,30,90)),.9f,"smooth analog magnitude");
  near(f.update(t+60_000_000,.2f,0,true,2,30,90),0,"smooth deadzone");
  near(f.update(t+80_000_000,.5f,1,true,2,30,90),0,"vertical dominant stick never turns");
  near(degrees(f.update(t+180_000_000,1,0,true,2,30,90)),4.5f,"stall delta capped at 50 ms");
  near(f.update(t+500_000_000,1,0,true,2,30,90),0,"long stall disarms");
  near(f.update(t+510_000_000,1,0,true,2,30,90),0,"held after stall blocked");
  f.update(t+520_000_000,0,0,true,2,30,90);
  near(f.update(t+530_000_000,1,0,false,2,30,90),0,"tracking loss");
  near(f.update(t+540_000_000,1,0,true,2,30,90),0,"tracking recovery blocked");
  f.update(t+550_000_000,0,0,true,2,30,90);
  near(f.update(t+560_000_000,1,0,true,0,30,90),0,"turning Off");
  var logic=new ControllerLogic();var neutral=ControllerTest.hand(0,0,0,false);var right=ControllerTest.hand(1,0,0,false);
  var smooth=new TurnSettings(2,45,120);var snap=new TurnSettings(1,90,90);
  logic.step(new ControllerState(1,t,true,neutral,neutral),t,2,false,true,1,smooth);
  var out=logic.step(new ControllerState(2,t+20_000_000,true,neutral,right),t+20_000_000,2,false,true,1,smooth);
  near(degrees(out.turn()),2.4f,"settings reach gameplay logic");
  out=logic.step(new ControllerState(3,t+40_000_000,true,neutral,right),t+40_000_000,2,false,true,1,snap);near(out.turn(),0,"settings change requires neutral");
  logic.step(new ControllerState(4,t+60_000_000,true,neutral,neutral),t+60_000_000,2,false,true,1,snap);
  out=logic.step(new ControllerState(5,t+80_000_000,true,neutral,right),t+80_000_000,2,false,true,1,snap);near(degrees(out.turn()),90,"new angle after neutral");
  for(int[] invalid:new int[][]{{3,30,90},{1,35,90},{2,30,0},{2,30,250},{2,30,31}}){try{new TurnSettings(invalid[0],invalid[1],invalid[2]);throw new AssertionError("invalid setting accepted");}catch(IllegalArgumentException expected){checks++;}}
  long epoch=InputBridge.generation();InputBridge.turning(2,60,150);check(InputBridge.generation()>epoch&&InputBridge.turning.equals(new TurnSettings(2,60,150)),"bridge publishes settings and rearms input");
  InputBridge.turning(1,30,90);
  System.out.println("Turning settings: "+checks+" checks passed");
 }
}
