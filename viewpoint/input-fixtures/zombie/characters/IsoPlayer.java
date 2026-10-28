package zombie.characters;
/** Authored input test double, never packaged. */
public final class IsoPlayer {
 public float x,y,z;public float getX(){return x;}public float getY(){return y;}public float getZ(){return z;}
 public static IsoPlayer[] players={new IsoPlayer()};public boolean dead,blocked;public zombie.vehicles.BaseVehicle vehicle;public boolean isDead(){return dead;}public boolean isBlockMovement(){return blocked;}public zombie.vehicles.BaseVehicle getVehicle(){return vehicle;}
}
