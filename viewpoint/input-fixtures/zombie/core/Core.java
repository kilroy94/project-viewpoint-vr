package zombie.core;
/** Authored input test double, never packaged. */
public final class Core {
 public static int width=1600,height=900;private static final Core instance=new Core();public static Core getInstance(){return instance;}public int getScreenWidth(){return width;}public int getScreenHeight(){return height;}
}
