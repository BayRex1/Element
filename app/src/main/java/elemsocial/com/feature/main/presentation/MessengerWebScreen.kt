package elemsocial.com.feature.main.presentation

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import elemsocial.com.ui.pack.theme.ElementUiPalette

/**
 * Uses the same Messenger implementation as the current web client.
 * The web bundle contains the complete chat protocol/UI in a lazy-loaded
 * Messenger chunk, so this avoids inventing unsupported native actions.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MessengerWebScreen(
    sessionKey: String?,
    initialPath: String = "/chat",
    modifier: Modifier = Modifier
) {
    val key = sessionKey?.trim().orEmpty()
    var keyHolder = key
    val webView = remember {
        WebView(androidx.compose.ui.platform.LocalContext.current).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val currentKey = keyHolder
                    if (currentKey.isBlank()) return
                    val escaped = currentKey.replace("\\", "\\\\").replace("'", "\\'")
                    view.evaluateJavascript("localStorage.setItem('S_KEY','$escaped'); true;") {
                        if (!url.contains(initialPath)) view.loadUrl("https://elemsocial.com$initialPath")
                    }
                }
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.allowFileAccess = false
            settings.allowContentAccess = true
        }
    }

    DisposableEffect(webView) {
        onDispose { webView.stopLoading() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ElementUiPalette.Body)
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                keyHolder = key
                if (view.url.isNullOrBlank()) view.loadUrl("https://elemsocial.com/")
            }
        )
    }
}
