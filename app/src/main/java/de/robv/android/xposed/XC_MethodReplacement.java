package de.robv.android.xposed;

/** Xposed-style method replacement callback. */
public abstract class XC_MethodReplacement extends XC_MethodHook {
    protected abstract Object replaceHookedMethod(MethodHookParam param) throws Throwable;

    @Override
    protected final void beforeHookedMethod(MethodHookParam param) throws Throwable {
        param.setResult(replaceHookedMethod(param));
    }

    public static XC_MethodReplacement returnConstant(Object constant) {
        return new XC_MethodReplacement() {
            @Override protected Object replaceHookedMethod(MethodHookParam param) {
                return constant;
            }
        };
    }
}
