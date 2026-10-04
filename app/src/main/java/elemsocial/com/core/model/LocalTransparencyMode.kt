package elemsocial.com.core.model

import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf

val LocalTransparencyMode = staticCompositionLocalOf { TransparencyMode.ADAPTIVE }

fun supportsGlassBlur(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

fun glassAlphaFor(mode: TransparencyMode): Float {
    return when (mode) {
        TransparencyMode.TRANSPARENT -> 0.62f
        TransparencyMode.OPAQUE -> 1.0f
        TransparencyMode.ADAPTIVE -> if (supportsGlassBlur()) 0.84f else 0.94f
    }
}

fun glassSoftAlphaFor(mode: TransparencyMode): Float {
    return when (mode) {
        TransparencyMode.TRANSPARENT -> 0.46f
        TransparencyMode.OPAQUE -> 0.96f
        TransparencyMode.ADAPTIVE -> if (supportsGlassBlur()) 0.70f else 0.88f
    }
}

fun shouldUseGlassBlur(mode: TransparencyMode): Boolean {
    return supportsGlassBlur() && mode != TransparencyMode.OPAQUE
}

fun dialogBackgroundBlurRadiusFor(mode: TransparencyMode): Int {
    if (!shouldUseGlassBlur(mode)) return 0
    return when (mode) {
        TransparencyMode.TRANSPARENT -> 72
        TransparencyMode.OPAQUE -> 0
        TransparencyMode.ADAPTIVE -> 56
    }
}

fun dialogBlurBehindRadiusFor(mode: TransparencyMode): Int {
    if (!shouldUseGlassBlur(mode)) return 0
    return when (mode) {
        TransparencyMode.TRANSPARENT -> 28
        TransparencyMode.OPAQUE -> 0
        TransparencyMode.ADAPTIVE -> 22
    }
}
