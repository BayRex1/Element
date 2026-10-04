package elemsocial.com.ui.pack.components.modals

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

@Composable
internal fun ElementDialogBlurEffect(
    enabled: Boolean = true,
    backgroundBlurRadius: Int = 56,
    blurBehindRadius: Int = 24,
    dimAmount: Float = 0f
) {
    val view = LocalView.current

    DisposableEffect(view, enabled, backgroundBlurRadius, blurBehindRadius, dimAmount) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (!enabled || window == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            onDispose { }
        } else {
            val previousDimAmount = window.attributes.dimAmount
            val hadBlurBehindFlag = window.attributes.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND != 0

            window.setDimAmount(dimAmount)
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.setBackgroundBlurRadius(backgroundBlurRadius)

            val attributes = window.attributes
            attributes.setBlurBehindRadius(blurBehindRadius)
            window.attributes = attributes

            onDispose {
                window.setBackgroundBlurRadius(0)
                val restoreAttributes = window.attributes
                restoreAttributes.setBlurBehindRadius(0)
                window.attributes = restoreAttributes
                if (!hadBlurBehindFlag) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                }
                window.setDimAmount(previousDimAmount)
            }
        }
    }
}
