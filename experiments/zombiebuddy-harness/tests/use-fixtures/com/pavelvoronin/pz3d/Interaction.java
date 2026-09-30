package com.pavelvoronin.pz3d;
import org.joml.Vector3f;import zombie.characters.IsoPlayer;import zombie.iso.IsoObject;
public class Interaction {
 public static boolean occluded;public static Choice gaze;
 public record Choice(IsoObject object,Traversal.Edge edge,float distance,String tap,Object container){}
 public static Choice choose(IsoPlayer p,Controller c,WorldMirror w){return gaze;}
 public static Choice choose(IsoPlayer p,Controller c,WorldMirror w,Vector3f head,Vector3f dir,boolean pointer){
  Traversal.aimed(c,w);if(occluded||w.current.hits.isEmpty())return null;
  // Original fixture ray through a thin plane, independent of production candidate filtering.
  var hit=w.current.hits.getFirst();float t=(hit.box().x0()-head.x)/dir.x;
  float y=head.y+dir.y*t,z=head.z+dir.z*t;
  if(t<0||y<hit.box().y0()||y>hit.box().y1()||z<hit.box().z0()||z>hit.box().z1())return null;
  return new Choice(hit.object(),null,t,"use",null);
 }
}
