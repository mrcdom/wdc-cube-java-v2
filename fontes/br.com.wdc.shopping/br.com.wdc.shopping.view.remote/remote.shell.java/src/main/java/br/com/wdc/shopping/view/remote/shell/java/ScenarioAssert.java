package br.com.wdc.shopping.view.remote.shell.java;

/**
 * Assertion helpers for scenario classes.
 * <p>
 * Throw {@link AssertionError} with a descriptive message on failure —
 * same semantics as JUnit assertions, but without a test dependency.
 */
public final class ScenarioAssert {

    private ScenarioAssert() {}

    public static void assertEquals(String msg, long expected, long actual) {
        if (expected != actual)
            throw new AssertionError(msg + " — esperado: " + expected + ", obtido: " + actual);
    }

    public static void assertEquals(String msg, int expected, int actual) {
        if (expected != actual)
            throw new AssertionError(msg + " — esperado: " + expected + ", obtido: " + actual);
    }

    public static void assertNotNull(String msg, Object value) {
        if (value == null)
            throw new AssertionError(msg + " — valor é null");
    }

    public static void assertNull(String msg, Object value) {
        if (value != null)
            throw new AssertionError(msg + " — esperado null, obtido: " + value);
    }

    public static void assertTrue(String msg, boolean condition) {
        if (!condition)
            throw new AssertionError(msg);
    }

    public static void assertFalse(String msg, boolean condition) {
        if (condition)
            throw new AssertionError(msg);
    }
}
