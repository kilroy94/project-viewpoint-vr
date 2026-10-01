package viewpointvr;

/** Conservative all-direction producer visibility; expires if render callbacks stop. */
public final class Visibility {
    private static volatile long until;
    public static void touch() {until=System.nanoTime()+1_000_000_000L;}
    public static void off() {until=0;}
    public static boolean active() {long deadline=until;return deadline!=0 && System.nanoTime()-deadline<0;}
    public static boolean visible(boolean original) {return original||active();}
    public static boolean hidden(boolean original) {return original&&!active();}
    private Visibility() {}
}
