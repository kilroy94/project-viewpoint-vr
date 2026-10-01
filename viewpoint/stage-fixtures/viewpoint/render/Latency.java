package viewpoint.render;
public final class Latency {
 public static void viewBuilt(long now,long read,long number,long taken){if(read!=44||number!=7||taken!=55)throw new AssertionError("latency inputs");Probe.hit("latency");}
}
