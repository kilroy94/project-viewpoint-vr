package com.pavelvoronin.pz3d;
import zombie.iso.*;
public class Traversal {public static boolean busy;public static int cameraSelections;public static boolean active(){return busy;}
 public static Edge aimed(Controller c,WorldMirror w){cameraSelections++;return null;}
 public record Edge(IsoObject object,int x,int y,boolean north,IsoDirections travel){}
}
