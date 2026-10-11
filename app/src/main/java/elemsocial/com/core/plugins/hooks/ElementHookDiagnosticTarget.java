package elemsocial.com.core.plugins.hooks;

/**
 * Isolated method used by the compatibility diagnostic plugin.
 * Keep this method side-effect-free: the test installs a hook, invokes it once,
 * verifies the changed result, and immediately removes the hook.
 */
public final class ElementHookDiagnosticTarget {
    private ElementHookDiagnosticTarget() {}

    public static String echo(String value) {
        return value;
    }
}
