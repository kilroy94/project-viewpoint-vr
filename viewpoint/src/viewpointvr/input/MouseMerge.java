package viewpointvr.input;
/** Merges one virtual left button without fabricating or releasing physical mouse edges. */
public final class MouseMerge {
 public record Buttons(boolean down,boolean previous) {}
 private boolean virtual,physical;private int route;
 public boolean held(){return virtual;}
 public Buttons step(boolean physicalNow,boolean nativePrevious,boolean virtualNow,int nextRoute){
  boolean previous=virtual?physical||(route==nextRoute):nativePrevious;
  physical=physicalNow;virtual=virtualNow;route=nextRoute;
  return new Buttons(physicalNow||virtualNow,previous);
 }
}
