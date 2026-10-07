package elemsocial.com.feature.main.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.PermissionRequest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentKey by rememberUpdatedState(key)
    val currentPath by rememberUpdatedState(initialPath)
    val webView = remember(context) {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    val activity = context as? Activity
                    if (activity == null) {
                        request.deny()
                        return
                    }
                    val needsAudio = request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                    val needsVideo = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                    val missing = buildList {
                        if (needsAudio && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            add(Manifest.permission.RECORD_AUDIO)
                        }
                        if (needsVideo && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            add(Manifest.permission.CAMERA)
                        }
                    }
                    if (missing.isNotEmpty()) {
                        ActivityCompat.requestPermissions(activity, missing.toTypedArray(), 2407)
                        request.deny()
                        return
                    }
                    val allowed = request.resources.filter { resource ->
                        resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                            resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE
                    }.toTypedArray()
                    if (allowed.isNotEmpty()) request.grant(allowed) else request.deny()
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val current = currentKey
                    if (current.isBlank()) return
                    val escaped = current.replace("\\", "\\\\").replace("'", "\\'")
                    view.evaluateJavascript("localStorage.setItem('S_KEY','$escaped'); true;") {
                        if (!url.contains(currentPath)) view.loadUrl("https://elemsocial.com$currentPath")
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
                if (view.url.isNullOrBlank()) view.loadUrl("https://elemsocial.com/")
            }
        )
    }
}
