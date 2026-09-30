package viewpoint.render;
public final class Probe {
 public static final java.util.List<String> events=new java.util.ArrayList<>();
 public static String fail=""; public static int bound=91,failOccurrence=1; public static boolean skip;
 public static void hit(String name) { events.add(name); if(name.equals(fail) && events.stream().filter(name::equals).count()==failOccurrence) throw new IllegalStateException("injected "+name); }
}
