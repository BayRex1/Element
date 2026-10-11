package de.robv.android.xposed;

import java.lang.reflect.Member;

/** Minimal Xposed-style callback surface backed by Element's in-process Pine adapter. */
public abstract class XC_MethodHook {
    public int priority;

    public XC_MethodHook() { this(50); }
    public XC_MethodHook(int priority) { this.priority = priority; }

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    public static class MethodHookParam {
        public Object thisObject;
        public Object[] args;
        public Member method;
        private Object result;
        private Throwable throwable;
        public boolean returnEarly;

        public Object getResult() { return result; }
        public void setResult(Object value) {
            result = value;
            throwable = null;
            returnEarly = true;
        }
        public Throwable getThrowable() { return throwable; }
        public boolean hasThrowable() { return throwable != null; }
        public void setThrowable(Throwable value) {
            throwable = value;
            result = null;
            returnEarly = true;
        }
        public Object getResultOrThrowable() throws Throwable {
            if (throwable != null) throw throwable;
            return result;
        }
    }

    public class Unhook implements IXUnhook<XC_MethodHook> {
        private final Member member;
        private final ElementPythonHookBridgeHandle bridgeHandle;
        public Unhook(Member member, ElementPythonHookBridgeHandle bridgeHandle) {
            this.member = member;
            this.bridgeHandle = bridgeHandle;
        }
        public Member getHookedMethod() { return member; }
        @Override public XC_MethodHook getCallback() { return XC_MethodHook.this; }
        @Override public void unhook() { if (bridgeHandle != null) bridgeHandle.unhook(); }
    }

    /** Small indirection so this API does not expose Pine implementation types. */
    public interface ElementPythonHookBridgeHandle { void unhook(); }
}
