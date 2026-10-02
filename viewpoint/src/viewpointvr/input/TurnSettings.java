package viewpointvr.input;
/** Session settings, matching the preserved mod's angle and speed choices. */
public record TurnSettings(int mode,int angle,int speed) {
 public static final TurnSettings DEFAULT=new TurnSettings(1,30,90);
 public TurnSettings {
  if(mode<0||mode>2)throw new IllegalArgumentException("Turning mode must be Off, Snap or Smooth");
  if(angle!=15&&angle!=30&&angle!=45&&angle!=60&&angle!=90)throw new IllegalArgumentException("Snap angle must be 15, 30, 45, 60 or 90 degrees");
  if(speed<30||speed>240||speed%15!=0)throw new IllegalArgumentException("Smooth speed must be 30..240 in 15 degree/second steps");
 }
 public String label(){return switch(mode){case 0->"Turning Off";case 1->"Snap "+angle+" deg";default->"Smooth "+speed+" deg/s";};}
}
