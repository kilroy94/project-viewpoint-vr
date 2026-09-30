package viewpoint;

/** Entirely synthetic: no game/mod code, imports, GL or entry points. */
public final class SceneDrawer {
    public int nativeCalls;
    public int restores;
    public Throwable failure;
    public Throwable caught;
    public void render() {
        try { drawFrame(); }
        catch (Throwable error) { caught = error; }
        finally { restores++; }
    }
    private void drawFrame() throws Throwable {
        nativeCalls++;
        if (failure != null) throw failure;
    }
}
