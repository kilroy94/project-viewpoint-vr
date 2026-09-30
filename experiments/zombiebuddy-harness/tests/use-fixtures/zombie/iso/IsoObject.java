package zombie.iso;
public class IsoObject {public boolean present=true;public IsoGridSquare getSquare(){return present?new IsoGridSquare():null;}public int getObjectIndex(){return present?0:-1;}public int getWorldObjectIndex(){return present?0:-1;}}
