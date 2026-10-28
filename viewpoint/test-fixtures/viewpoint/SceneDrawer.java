package viewpoint;

/** Entirely synthetic: no game/mod code, imports, GL or entry points. */
public final class SceneDrawer {
    public int nativeCalls;
    public int restores;
    public Throwable failure;
    public Throwable caught;
    public void render() {
        try { guarded(this::drawFrame); }
        catch (Throwable error) { caught = error; }
        finally { restores++; }
    }
    @SuppressWarnings("unchecked") private static <T extends Throwable> void raise(Throwable error)throws T{throw (T)error;}
    private static void guarded(Runnable body){body.run();}
    private void drawFrame() {
        nativeCalls++;
        if (failure != null) SceneDrawer.<RuntimeException>raise(failure);
    }
}
