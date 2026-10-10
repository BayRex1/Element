package de.robv.android.xposed;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import top.canyie.pine.Pine;
import top.canyie.pine.PineConfig;
import top.canyie.pine.callback.MethodHook;

/** Minimal XposedBridge compatibility layer for in-process plugin hooks. */
public final class XposedBridge {
    private static final String TAG = "ElementXposedBridge";
    private XposedBridge() {}

    public static XC_MethodHook.Unhook hookMethod(Member member, XC_MethodHook callback) {
        if (member == null || callback == null) return null;
        if (!elemsocial.com.core.plugins.hooks.ElementPythonHookBridge.ensureInitialized()) {
            Log.w(TAG, elemsocial.com.core.plugins.hooks.ElementPythonHookBridge.getUnavailableReason());
            return null;
        }
        final MethodHook.Unhook pineHandle = Pine.hook(member, new MethodHook() {
            @Override public void beforeCall(Pine.CallFrame frame) throws Throwable {
                XC_MethodHook.MethodHookParam param = copy(frame);
                try {
                    callback.beforeHookedMethod(param);
                } catch (Throwable error) {
                    Log.e(TAG, "beforeHookedMethod failed for " + member, error);
                    return;
                }
                apply(frame, param);
            }
            @Override public void afterCall(Pine.CallFrame frame) throws Throwable {
                XC_MethodHook.MethodHookParam param = copy(frame);
                param.setResult(frame.getResult());
                if (frame.getThrowable() != null) param.setThrowable(frame.getThrowable());
                try {
                    callback.afterHookedMethod(param);
                } catch (Throwable error) {
                    Log.e(TAG, "afterHookedMethod failed for " + member, error);
                    return;
                }
                apply(frame, param);
            }
        });
        return callback.new Unhook(member, pineHandle::unhook);
    }

    private static XC_MethodHook.MethodHookParam copy(Pine.CallFrame frame) {
        XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
        param.method = frame.method;
        param.thisObject = frame.thisObject;
        param.args = frame.args;
        return param;
    }

    private static void apply(Pine.CallFrame frame, XC_MethodHook.MethodHookParam param) {
        frame.thisObject = param.thisObject;
        if (param.args != null) frame.args = param.args;
        if (param.returnEarly) {
            if (param.hasThrowable()) frame.setThrowable(param.getThrowable());
            else frame.setResult(param.getResult());
        }
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> clazz, String name, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> result = new HashSet<>();
        if (clazz == null || name == null) return result;
        for (Method method : clazz.getDeclaredMethods()) {
            if (name.equals(method.getName())) {
                XC_MethodHook.Unhook unhook = hookMethod(method, callback);
                if (unhook != null) result.add(unhook);
            }
        }
        return result;
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> clazz, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> result = new HashSet<>();
        if (clazz == null) return result;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            XC_MethodHook.Unhook unhook = hookMethod(constructor, callback);
            if (unhook != null) result.add(unhook);
        }
        return result;
    }

    public static Object invokeOriginalMethod(Member member, Object receiver, Object[] args) throws Throwable {
        return Pine.invokeOriginalMethod(member, receiver, args == null ? new Object[0] : args);
    }

    public static void log(String message) { Log.d(TAG, String.valueOf(message)); }
    public static void log(Throwable error) { Log.e(TAG, "Plugin hook error", error); }
}
