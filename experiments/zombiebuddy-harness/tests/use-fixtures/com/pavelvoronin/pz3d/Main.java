package com.pavelvoronin.pz3d;
import zombie.characters.IsoPlayer;
public class Main {
 public static final Access access=new Access();public static class Access {public boolean look=true,combat=true,actions=true;}
 public static Controller jq=new Controller();public static WorldMirror jr=new WorldMirror();public static int calls;public static Interaction.Choice used;public static boolean throwUse;
 static Access controlAccess(){return access;}public static void tick(){}
 private static void b(boolean alternate){used=Interaction.choose(IsoPlayer.instance,jq,jr);calls++;if(throwUse)throw new IllegalStateException("fixture native failure");}
 public static void keyboard(){b(false);}
}
