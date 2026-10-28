package viewpointvr.input;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoGridSquare;
import zombie.pathfind.PolygonalMap2;
import org.joml.Vector2f;
/** Uses the installed game's capsule collision query, loaded-floor and adjacent-tile checks. */
public final class NativeRoomMotion {
 private NativeRoomMotion(){}
 public static RoomMotion.Step move(IsoPlayer player,RoomMotion.Step request){
  if(player.getCurrentSquare()==null||!player.isCollidable()||player.isOnFloor()||player.isSittingOnFurniture()||player.isSitOnGround()||!player.getCharacterActions().isEmpty())return RoomMotion.Step.NONE;
  return RoomMotion.apply(new RoomMotion.Body(){
   IsoGridSquare destination;
   public float x(){return player.getX();}public float y(){return player.getY();}public float nextX(){return player.getNextX();}public float nextY(){return player.getNextY();}
   public Vector2f resolve(float x,float y){return PolygonalMap2.instance.resolveCollision(player,x,y,new Vector2f());}
   public boolean accepts(float x,float y){
    var current=player.getCurrentSquare();destination=player.getCell().getGridSquare((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(player.getZ()));
    return destination!=null&&destination.TreatAsSolidFloor()&&!destination.isSolid()&&!destination.isSolidTrans()
     &&Math.abs(destination.x-current.x)<=1&&Math.abs(destination.y-current.y)<=1&&!current.testCollideAdjacent(player,destination.x-current.x,destination.y-current.y,0);
   }
   public void move(float x,float y,float nx,float ny){float dx=x-player.getX(),dy=y-player.getY();player.setLastX(player.getLastX()+dx);player.setLastY(player.getLastY()+dy);player.setX(x);player.setY(y);player.setNextX(nx);player.setNextY(ny);player.setCurrent(destination);player.setMovingSquareNow();}
  },request);
 }
}
