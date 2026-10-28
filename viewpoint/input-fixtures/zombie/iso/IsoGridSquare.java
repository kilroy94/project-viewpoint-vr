package zombie.iso;
/** Authored collision facade. */
public final class IsoGridSquare {public int x,y;public static boolean blocked,floor=true,solid;public IsoGridSquare(int x,int y){this.x=x;this.y=y;}public boolean TreatAsSolidFloor(){return floor;}public boolean isSolid(){return solid;}public boolean isSolidTrans(){return false;}public boolean testCollideAdjacent(zombie.iso.IsoMovingObject p,int x,int y,int z){return blocked;}}
