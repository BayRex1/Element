package elemsocial.com.core.plugins.hooks;

import android.os.Build;
import android.util.Log;

import com.chaquo.python.PyObject;

import java.lang.reflect.Member;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import elemsocial.com.BuildConfig;
import top.canyie.pine.Pine;
import top.canyie.pine.PineConfig;
import top.canyie.pine.callback.MethodHook;

/**
 * In-process ART hook backend used by Python plugins.
 *
 * Pine is intentionally enabled only on arm64 and Android API 26-35. Its published
 * native backend does not support x86_64 and upstream 0.3.0 does not declare
 * Android 16 support. Unsupported devices get a clear error instead of a crash.
 */
public final class ElementPythonHookBridge {
    private static final String TAG = "ElementPluginHooks";
    private static final Object INIT_LOCK = new Object();
    private static volatile boolean initialized;
    private static volatile String unavailableReason = "Not initialized";
    private static final ConcurrentHashMap<String, CopyOnWriteArrayList<HookHandle>> PLUGIN_HOOKS =
            new ConcurrentHashMap<>();

    private ElementPythonHookBridge() {}

    public static boolean isSupported() {
        if (Build.VERSION.SDK_INT < 26 || Build.VERSION.SDK_INT > 35) return false;
        for (String abi : Build.SUPPORTED_ABIS) {
            if ("arm64-v8a".equals(abi)) return true;
        }
        return false;
    }

    public static String getUnavailableReason() {
        if (Build.VERSION.SDK_INT > 35) return "Pine 0.3.0 не заявляет поддержку Android 16/API 36+";
        if (Build.VERSION.SDK_INT < 26) return "Требуется Android 8.0/API 26+";
        return "Нативный backend Pine поддерживает только arm64-v8a; x86_64 не поддерживается";
    }

    public static boolean ensureInitialized() {
        if (initialized) return true;
        synchronized (INIT_LOCK) {
            if (initialized) return true;
            if (!isSupported()) {
                unavailableReason = getUnavailableReason();
                return false;
            }
            try {
                PineConfig.debug = BuildConfig.DEBUG;
                PineConfig.debuggable = BuildConfig.DEBUG;
                // Avoid globally changing Android hidden-API policy just to enable plugin hooks.
                PineConfig.disableHiddenApiPolicy = false;
                PineConfig.disableHiddenApiPolicyForPlatformDomain = false;
                Pine.ensureInitialized();
                initialized = true;
                unavailableReason = "";
                Log.i(TAG, "Pine initialized for Python plugins");
                return true;
            } catch (Throwable error) {
                unavailableReason = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
                Log.e(TAG, "Pine initialization failed", error);
                return false;
            }
        }
    }

    public static HookHandle hook(Member member, String pluginId, PyObject callback, int priority) {
        if (member == null) throw new IllegalArgumentException("method/constructor is null");
        if (callback == null) throw new IllegalArgumentException("hook callback is null");
        if (!ensureInitialized()) {
            throw new UnsupportedOperationException("Java method hooks unavailable: " + unavailableReason);
        }
        final String owner = pluginId == null ? "" : pluginId;
        MethodHook.Unhook unhook = Pine.hook(member, new MethodHook() {
            @Override public void beforeCall(Pine.CallFrame frame) {
                ElementMethodHookParam param = new ElementMethodHookParam(frame);
                try {
                    if (callback.hasAttr("before_hooked_method")) {
                        callback.callAttr("before_hooked_method", param);
                    }
                } catch (Throwable error) {
                    Log.e(TAG, "Plugin " + owner + " before-hook failed: " + member, error);
                    return;
                }
                applyBefore(frame, param);
            }

            @Override public void afterCall(Pine.CallFrame frame) {
                ElementMethodHookParam param = new ElementMethodHookParam(frame);
                param.result = frame.getResult();
                param.throwable = frame.getThrowable();
                try {
                    if (callback.hasAttr("after_hooked_method")) {
                        callback.callAttr("after_hooked_method", param);
                    }
                } catch (Throwable error) {
                    Log.e(TAG, "Plugin " + owner + " after-hook failed: " + member, error);
                    return;
                }
                applyAfter(frame, param);
            }
        });
        HookHandle handle = new HookHandle(unhook, owner, member);
        if (!owner.isEmpty()) {
            PLUGIN_HOOKS.computeIfAbsent(owner, ignored -> new CopyOnWriteArrayList<>()).add(handle);
        }
        return handle;
    }

    private static void applyBefore(Pine.CallFrame frame, ElementMethodHookParam param) {
        if (param.args != null) frame.args = param.args;
        frame.thisObject = param.thisObject;
        if (param.returnEarly) {
            if (param.throwable != null) frame.setThrowable(param.throwable);
            else frame.setResult(param.result);
        }
    }

    private static void applyAfter(Pine.CallFrame frame, ElementMethodHookParam param) {
        if (param.args != null) frame.args = param.args;
        frame.thisObject = param.thisObject;
        if (param.returnEarly || param.resultChanged || param.throwableChanged) {
            if (param.throwable != null) frame.setThrowable(param.throwable);
            else frame.setResult(param.result);
        }
    }

    public static void unhookAll(String pluginId) {
        if (pluginId == null) return;
        CopyOnWriteArrayList<HookHandle> hooks = PLUGIN_HOOKS.remove(pluginId);
        if (hooks == null) return;
        for (HookHandle hook : hooks) {
            try { hook.unhook(); } catch (Throwable error) {
                Log.e(TAG, "Unable to remove plugin hook " + hook.member, error);
            }
        }
    }

    public static final class HookHandle {
        private final MethodHook.Unhook unhook;
        private final String pluginId;
        private final Member member;
        private volatile boolean removed;

        HookHandle(MethodHook.Unhook unhook, String pluginId, Member member) {
            this.unhook = unhook;
            this.pluginId = pluginId;
            this.member = member;
        }

        public Member getHookedMember() { return member; }

        public void unhook() {
            if (removed) return;
            removed = true;
            if (unhook != null) unhook.unhook();
            CopyOnWriteArrayList<HookHandle> hooks = PLUGIN_HOOKS.get(pluginId);
            if (hooks != null) {
                hooks.remove(this);
                if (hooks.isEmpty()) PLUGIN_HOOKS.remove(pluginId, hooks);
            }
        }
    }

    /** Xposed-like parameter object exposed to Python callbacks. */
    public static final class ElementMethodHookParam {
        public Object thisObject;
        public Object[] args;
        public final Member method;
        public Object result;
        public Throwable throwable;
        public boolean returnEarly;
        boolean resultChanged;
        boolean throwableChanged;

        ElementMethodHookParam(Pine.CallFrame frame) {
            this.thisObject = frame.thisObject;
            this.args = frame.args;
            this.method = frame.method;
            this.result = frame.getResult();
            this.throwable = frame.getThrowable();
        }

        public Object getResult() { return result; }
        public void setResult(Object value) {
            result = value;
            throwable = null;
            returnEarly = true;
            resultChanged = true;
            throwableChanged = true;
        }
        public Throwable getThrowable() { return throwable; }
        public boolean hasThrowable() { return throwable != null; }
        public void setThrowable(Throwable value) {
            throwable = value;
            result = null;
            returnEarly = true;
            throwableChanged = true;
            resultChanged = true;
        }
        public Object getResultOrThrowable() throws Throwable {
            if (throwable != null) throw throwable;
            return result;
        }
        public void setObjectExtra(String key, Object value) {}
        public Object getObjectExtra(String key) { return null; }
    }
}
