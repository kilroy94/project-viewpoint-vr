package zombie.characters;
public class IsoPlayer extends IsoGameCharacter {
 public static final IsoPlayer instance=new IsoPlayer();public boolean dead,busy,attack;public static IsoPlayer getInstance(){return instance;}
 public boolean isDead(){return dead;}public boolean hasTimedActions(){return busy;}public boolean isGrappling(){return false;}
 public boolean isAttackStarted(){return attack;}public zombie.vehicles.BaseVehicle getVehicle(){return null;}
 public int getJoypadBind(){return -1;}
}
