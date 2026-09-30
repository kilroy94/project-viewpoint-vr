package com.pavelvoronin.pz3d;
import java.util.*;import zombie.iso.IsoObject;
public class WorldMirror {
 public final Scene current=new Scene();public Scene scene(){return current;}public List<Pick> itemTargets(){return List.of();}
 public record Box(float x0,float y0,float z0,float x1,float y1,float z1){}
 public record Hit(IsoObject object,Box box,boolean north){}
 public record Pick(IsoObject object,Box bounds){}
 public static class Scene {public List<Hit> hits=new ArrayList<>();public List<Hit> targets(){return hits;}public float ox(){return 10;}public float oy(){return 20;}public float oz(){return 2.44949f;}}
}
