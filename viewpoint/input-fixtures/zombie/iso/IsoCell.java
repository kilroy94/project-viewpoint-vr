package zombie.iso; public final class IsoCell {public static boolean loaded=true;public IsoGridSquare getGridSquare(int x,int y,int z){return loaded?new IsoGridSquare(x,y):null;}}
