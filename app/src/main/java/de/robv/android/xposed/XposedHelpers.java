package de.robv.android.xposed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Common reflection helpers used by Xposed-style plugin code. */
public final class XposedHelpers {
    private XposedHelpers() {}

    public static Class<?> findClass(String className, ClassLoader classLoader) throws ClassNotFoundError {
        try {
            ClassLoader loader = classLoader != null ? classLoader : XposedHelpers.class.getClassLoader();
            return Class.forName(className, false, loader);
        } catch (Throwable error) {
            throw new ClassNotFoundError(className, error);
        }
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Throwable error) {
            throw new NoSuchMethodError(clazz.getName() + "." + methodName);
        }
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (Throwable error) {
            throw new NoSuchMethodError(clazz.getName() + ".<init>");
        }
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
            Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback == null || parameterTypesAndCallback.length == 0) {
            throw new IllegalArgumentException("Missing XC_MethodHook callback");
        }
        Object callbackObject = parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
        if (!(callbackObject instanceof XC_MethodHook)) {
            throw new IllegalArgumentException("Last argument must be XC_MethodHook");
        }
        Class<?>[] parameterTypes = new Class<?>[parameterTypesAndCallback.length - 1];
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!(parameterTypesAndCallback[i] instanceof Class<?>)) {
                throw new IllegalArgumentException("Parameter type " + i + " is not a Class");
            }
            parameterTypes[i] = (Class<?>) parameterTypesAndCallback[i];
        }
        return XposedBridge.hookMethod(
                findMethodExact(clazz, methodName, parameterTypes), (XC_MethodHook) callbackObject);
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
            String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        return findAndHookMethod(findClass(className, classLoader), methodName, parameterTypesAndCallback);
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(
            Class<?> clazz, Object... parameterTypesAndCallback) {
        if (parameterTypesAndCallback == null || parameterTypesAndCallback.length == 0) {
            throw new IllegalArgumentException("Missing XC_MethodHook callback");
        }
        Object callbackObject = parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
        if (!(callbackObject instanceof XC_MethodHook)) {
            throw new IllegalArgumentException("Last argument must be XC_MethodHook");
        }
        Class<?>[] parameterTypes = new Class<?>[parameterTypesAndCallback.length - 1];
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!(parameterTypesAndCallback[i] instanceof Class<?>)) {
                throw new IllegalArgumentException("Parameter type " + i + " is not a Class");
            }
            parameterTypes[i] = (Class<?>) parameterTypesAndCallback[i];
        }
        return XposedBridge.hookMethod(
                findConstructorExact(clazz, parameterTypes), (XC_MethodHook) callbackObject);
    }

    public static final class ClassNotFoundError extends Error {
        public ClassNotFoundError(String name, Throwable cause) {
            super("Class not found: " + name, cause);
        }
    }
}
