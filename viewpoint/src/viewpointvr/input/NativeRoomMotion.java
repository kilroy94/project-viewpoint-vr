package viewpointvr.input;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoGridSquare;
import zombie.pathfind.PolygonalMap2;
import org.joml.Vector2f;
/** Uses the installed game's capsule collision query, loaded-floor and adjacent-tile checks. */
public final class NativeRoomMotion {
 public static volatile String status="not requested";
 private NativeRoomMotion(){}
 public static RoomMotion.Step move(IsoPlayer player,RoomMotion.Step request){
  status="no physical step";if(request.equals(RoomMotion.Step.NONE))return RoomMotion.Step.NONE;
  if(player.getCurrentSquare()==null||!player.isCollidable()||player.isOnFloor()||player.isSittingOnFurniture()||player.isSitOnGround()||!player.getCharacterActions().isEmpty()){status="native posture/action blocked";return RoomMotion.Step.NONE;}
  status="collision stopped step";
  var accepted=RoomMotion.apply(new RoomMotion.Body(){
   IsoGridSquare destination;
   public float x(){return player.getX();}public float y(){return player.getY();}public float nextX(){return player.getNextX();}public float nextY(){return player.getNextY();}
   public Vector2f resolve(float x,float y){return PolygonalMap2.instance.resolveCollision(player,x,y,new Vector2f());}
   public boolean accepts(float x,float y){
    var current=player.getCurrentSquare();destination=player.getCell().getGridSquare((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(player.getZ()));
    if(destination==null||!destination.TreatAsSolidFloor()||destination.isSolid()||destination.isSolidTrans()){status="destination floor/solid blocked";return false;}
    int dx=destination.x-current.x,dy=destination.y-current.y;
    if(Math.abs(dx)>1||Math.abs(dy)>1){status="destination not adjacent";return false;}
    // Native DoCollide checks only transitions. The self bit starts set and is
    // excluded by RecalcAllWithNeighbours; it is not an intra-tile collision test.
    if((dx!=0||dy!=0)&&current.testCollideAdjacent(player,dx,dy,0)){status="tile boundary blocked";return false;}
    return true;
   }
   public void move(float x,float y,float nx,float ny){float dx=x-player.getX(),dy=y-player.getY();player.setLastX(player.getLastX()+dx);player.setLastY(player.getLastY()+dy);player.setX(x);player.setY(y);player.setNextX(nx);player.setNextY(ny);player.setCurrent(destination);player.setMovingSquareNow();}
  },request);
  if(!accepted.equals(RoomMotion.Step.NONE))status="accepted";
  return accepted;
 }
}
