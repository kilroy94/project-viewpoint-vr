package viewpointvr.instrument;

import java.lang.instrument.Instrumentation;

/** Test-only premain; never part of the production artifact. */
public final class TestAgent {
    public static Instrumentation instrumentation;
    public static void premain(String args, Instrumentation value) { instrumentation = value; }
}
