package zombie.characters;
/** Authored input test double, never packaged. */
public final class IsoPlayer extends IsoGameCharacter {
 public int bind=-1;public int getJoypadBind(){return bind;}
 public float lx,ly;public float getLastX(){return lx;}public float getLastY(){return ly;}public float setLastX(float v){return lx=v;}public float setLastY(float v){return ly=v;}
 public float nx,ny;public zombie.iso.IsoGridSquare current=new zombie.iso.IsoGridSquare(0,0);public final zombie.iso.IsoCell cell=new zombie.iso.IsoCell();public zombie.inventory.InventoryItem item;
 public zombie.inventory.InventoryItem getPrimaryHandItem(){return item;}
 public zombie.iso.IsoGridSquare getCurrentSquare(){return current;}public zombie.iso.IsoCell getCell(){return cell;}
 public boolean isCollidable(){return true;}public boolean isOnFloor(){return false;}public boolean isSittingOnFurniture(){return false;}public boolean isSitOnGround(){return false;}
 public java.util.Stack<Object> getCharacterActions(){return new java.util.Stack<>();}
 public float getNextX(){return nx;}public float getNextY(){return ny;}public float setX(float v){return x=v;}public float setY(float v){return y=v;}public float setNextX(float v){return nx=v;}public float setNextY(float v){return ny=v;}public void setCurrent(zombie.iso.IsoGridSquare v){current=v;}public void setMovingSquareNow(){}
 public float x,y,z;public float getX(){return x;}public float getY(){return y;}public float getZ(){return z;}
 public static IsoPlayer[] players={new IsoPlayer()};public boolean dead,blocked;public zombie.vehicles.BaseVehicle vehicle;public boolean isDead(){return dead;}public boolean isBlockMovement(){return blocked;}public zombie.vehicles.BaseVehicle getVehicle(){return vehicle;}
}
