package elemsocial.com.feature.auth.presentation

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import elemsocial.com.R
import elemsocial.com.BuildConfig
import elemsocial.com.config.AppConfig
import elemsocial.com.core.session.SessionStore
import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.domain.model.AuthResult
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.components.buttons.ElementButtonVariant
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AuthPage { LOGIN, REGISTER, VERIFY }

private data class LastUser(
    val name: String,
    val username: String,
    val avatar: String?
)

private val BodyColor: Color
    get() = ElementUiPalette.Body
private val BlockColor: Color
    get() = ElementUiPalette.Body
private val InputColor: Color
    get() = ElementUiPalette.BlockSoft
private val AccentColor: Color
    get() = ElementUiPalette.Accent
private val TitleColor: Color
    get() = ElementUiPalette.TextPrimary
private val TextColor: Color
    get() = ElementUiPalette.TextPrimary
private val TextLiteColor: Color
    get() = ElementUiPalette.TextLite
private val ErrorColor: Color
    get() = ElementUiPalette.Error
private val SuccessColor: Color
    get() = ElementUiPalette.Success
private val InfoColor: Color
    get() = ElementUiPalette.Info

private const val HCAPTCHA_SITE_KEY = "29c6b1c2-7e78-43ec-8bf8-5de49c58c54a"

private fun resolveWsUrls(raw: String): List<String> {
    val typed = raw
        .split(',', ';', '\n', ' ')
        .map { it.trim() }
        .filter { it.startsWith("ws://") || it.startsWith("wss://") }

    return (typed + AppConfig.Domains.DEFAULT_USER_WS_URLS).distinct()
}

private fun parseLastUsers(raw: Any?): List<LastUser> {
    val source = raw as? List<*> ?: return emptyList()
    return source.mapNotNull { item ->
        val map = item as? Map<*, *> ?: return@mapNotNull null
        val name = map["name"]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        LastUser(
            name = name,
            username = map["username"]?.toString().orEmpty(),
            avatar = map["avatar"]?.toString()
        )
    }
}

@Composable
private fun AuthInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val isLightTheme = BodyColor.luminance() > 0.5f
    UIKit.Input(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        borderWidth = 1.dp,
        borderColor = if (isLightTheme) ElementUiPalette.Border else Color.Transparent,
        containerColor = if (isLightTheme) ElementUiPalette.Block else InputColor,
        textColor = TextColor,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun PrimaryButton(
    title: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    UIKit.Button(
        title = title,
        onClick = onClick,
        enabled = enabled,
        loading = loading,
        variant = ElementButtonVariant.Primary,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun SecondaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    UIKit.Button(
        title = title,
        onClick = onClick,
        variant = ElementButtonVariant.Soft,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun AuthFooter(modifier: Modifier = Modifier) {
    val isLightTheme = BodyColor.luminance() > 0.5f
    Text(
       text = "Создатель Moretti",
        color = if (isLightTheme) {
            ElementUiPalette.TextSecondary.copy(alpha = 0.82f)
        } else {
            TextLiteColor.copy(alpha = 0.72f)
        },
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
    )
}

private class HCaptchaBridge(
    private val onVerify: (String) -> Unit,
    private val onReset: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onVerify(token: String) {
        mainHandler.post {
            onVerify(token)
        }
    }

    @JavascriptInterface
    fun onExpire() {
        mainHandler.post(onReset)
    }

    @JavascriptInterface
    fun onError() {
        mainHandler.post(onReset)
    }
}

private fun buildHCaptchaHtml(theme: String): String {
    return """
        <!doctype html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" />
            <script src="https://js.hcaptcha.com/1/api.js" async defer></script>
            <style>
              html, body {
                width: 100%;
                height: 100%;
                margin: 0;
                padding: 0;
                overflow: hidden;
                background: transparent;
              }
              body {
                display: flex;
                align-items: center;
                justify-content: center;
              }
              .h-captcha {
                transform: scale(0.96);
                transform-origin: center center;
              }
            </style>
          </head>
          <body>
            <div
              class="h-captcha"
              data-sitekey="$HCAPTCHA_SITE_KEY"
              data-theme="$theme"
              data-size="normal"
              data-callback="captchaVerified"
              data-expired-callback="captchaExpired"
              data-error-callback="captchaError">
            </div>
            <script>
              function captchaVerified(token) {
                AndroidCaptcha.onVerify(token || '');
              }
              function captchaExpired() {
                AndroidCaptcha.onExpire();
              }
              function captchaError() {
                AndroidCaptcha.onError();
              }
            </script>
          </body>
        </html>
    """.trimIndent()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AuthCaptcha(
    token: String,
    resetNonce: Int,
    onTokenChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = BodyColor.luminance() > 0.5f
    val captchaTheme = if (isLightTheme) "light" else "dark"
    val html = remember(captchaTheme, resetNonce) { buildHCaptchaHtml(captchaTheme) }
    val currentOnTokenChange by rememberUpdatedState(onTokenChange)
    val bridge = remember {
        HCaptchaBridge(
            onVerify = { currentOnTokenChange(it) },
            onReset = { currentOnTokenChange("") }
        )
    }
    var loadedKey by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(InputColor)
            .border(
                width = 1.dp,
                color = ElementUiPalette.Border.copy(alpha = if (isLightTheme) 1f else 0.18f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    addJavascriptInterface(bridge, "AndroidCaptcha")
                }
            },
            update = { webView ->
                val nextKey = "$captchaTheme:$resetNonce"
                if (loadedKey != nextKey) {
                    loadedKey = nextKey
                    webView.loadDataWithBaseURL(
                        "https://elemsocial.com/",
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )
    }

    if (token.isNotBlank()) {
        Text(
            text = "Проверка пройдена",
            color = SuccessColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}

@Composable
fun AuthScreen(
    socketClient: ElementSocketClient,
    authGateway: AuthGateway,
    sessionStore: SessionStore,
    attemptStoredSessionOnStart: Boolean = true,
    onAuthorized: (AuthResult) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val connectionState by socketClient.connectionState.collectAsState()

    var page by remember { mutableStateOf(AuthPage.LOGIN) }
    var wsUrl by remember { mutableStateOf(AppConfig.Domains.DEFAULT_USER_WS_URL) }

    var loginLoading by remember { mutableStateOf(false) }
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showSessionKeyLogin by remember { mutableStateOf(false) }
    var sessionKeyInput by remember { mutableStateOf("") }

    var regLoading by remember { mutableStateOf(false) }
    var regName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regReferralCode by remember { mutableStateOf("") }
    var regAcceptRules by remember { mutableStateOf(false) }
    var regCaptchaToken by remember { mutableStateOf("") }
    var regCaptchaResetNonce by remember { mutableStateOf(0) }

    var verifyEmail by remember { mutableStateOf("") }
    var verifyCode by remember { mutableStateOf("") }

    var showRegPassword by remember { mutableStateOf(false) }

    var verifyLoading by remember { mutableStateOf(false) }
    var resendLoading by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableStateOf(0) }

    var statusText by remember { mutableStateOf("") }
    var statusColor by remember { mutableStateOf(TextLiteColor) }
    var accountName by remember { mutableStateOf<String?>(null) }

    var lastSocketLog by remember { mutableStateOf("") }
    var lastUsers by remember { mutableStateOf<List<LastUser>>(emptyList()) }
    var lastUsersLoaded by remember { mutableStateOf(false) }

    fun setError(message: String) {
        statusText = message
        statusColor = ErrorColor
    }

    fun setInfo(message: String) {
        statusText = message
        statusColor = InfoColor
    }

    fun setSuccess(message: String) {
        statusText = message
        statusColor = SuccessColor
    }

    fun resetRegistrationCaptcha() {
        regCaptchaToken = ""
        regCaptchaResetNonce += 1
    }

    suspend fun ensureSocketReady(): Boolean {
        if (connectionState == ElementSocketClient.ConnectionState.Ready) return true

        setInfo("Подключение к серверу...")
        val isReady = runCatching {
            socketClient.connectAndAwaitReady(resolveWsUrls(wsUrl), timeoutMs = 35_000)
        }.getOrDefault(false)

        if (!isReady) {
            setError("Сокет не готов. Проверьте сеть/VPN")
        }

        return isReady
    }

    suspend fun finalizeSession(sessionKey: String): Boolean {
        val connected = authGateway.connectBySessionKey(sessionKey)
        return if (connected.isSuccess && connected.hasAccountIdentity && !connected.isAccountNotFound) {
            sessionStore.saveSessionKey(sessionKey)
            accountName = connected.accountName
            setSuccess("Вы вошли как ${connected.accountName ?: "пользователь"}")
            onAuthorized(connected.copy(sessionKey = sessionKey))
            true
        } else if (connected.isSuccess || connected.isAccountNotFound) {
            sessionStore.clearSessionKey()
            setError("Сессия не содержит данных аккаунта. Войдите заново.")
            false
        } else {
            setError(connected.message ?: "Не удалось подключить сессию")
            false
        }
    }

    suspend fun submitRegistration() {
        if (regName.isBlank() || regUsername.isBlank() || regEmail.isBlank() || regPassword.isBlank()) {
            setError("Заполните обязательные поля")
            return
        }
        if (!regAcceptRules) {
            setError("Нужно принять правила")
            return
        }
        if (regCaptchaToken.isBlank()) {
            setError("Пройдите капчу")
            return
        }
        if (regLoading) return

        regLoading = true
        try {
            if (!ensureSocketReady()) return
            val captchaToken = regCaptchaToken

            val result = authGateway.register(
                name = regName.trim(),
                username = regUsername.trim(),
                email = regEmail.trim(),
                password = regPassword,
                referralCode = regReferralCode.ifBlank { null },
                acceptRules = regAcceptRules,
                hCaptchaToken = captchaToken
            )

            when {
                result.isSuccess -> {
                    val sKey = result.sessionKey
                    if (sKey.isNullOrBlank()) {
                        setError("Сервер не вернул S_KEY")
                    } else {
                        finalizeSession(sKey)
                    }
                }

                result.requiresEmailVerification -> {
                    verifyEmail = result.email ?: regEmail.trim()
                    verifyCode = ""
                    page = AuthPage.VERIFY
                    setInfo(result.message ?: "Подтвердите почту")
                }

                else -> {
                    val message = result.message ?: "Ошибка регистрации"
                    resetRegistrationCaptcha()
                    setError(message)
                }
            }
        } finally {
            regLoading = false
        }
    }

    suspend fun loadLastUsersIfNeeded() {
        if (lastUsersLoaded) return
        val response = runCatching {
            socketClient.sendRequest(
                mapOf(
                    "type" to "system",
                    "action" to "get_last_users"
                ),
                timeoutMs = 15_000
            )
        }.getOrNull() ?: return

        lastUsers = parseLastUsers(response["users"])
        lastUsersLoaded = true
    }

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown -= 1
        }
    }

    LaunchedEffect(socketClient) {
        socketClient.logs.collect { line ->
            lastSocketLog = line
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ElementSocketClient.ConnectionState.Ready) {
            loadLastUsersIfNeeded()
        }
    }

    LaunchedEffect(Unit) {
        val isReady = runCatching {
            socketClient.connectAndAwaitReady(resolveWsUrls(wsUrl), timeoutMs = 35_000)
        }.getOrDefault(false)

        if (!isReady) {
            setError("Не удалось подключиться к серверу")
            return@LaunchedEffect
        }

        loadLastUsersIfNeeded()

        if (attemptStoredSessionOnStart) {
            val stored = sessionStore.getSessionKey()
            if (!stored.isNullOrBlank()) {
                val result = runCatching {
                    authGateway.connectBySessionKey(stored)
                }.getOrNull()

                if (result?.isSuccess == true &&
                    result.hasAccountIdentity &&
                    !result.isAccountNotFound
                ) {
                    accountName = result.accountName
                    setSuccess("Вы вошли как ${result.accountName ?: "пользователь"}")
                    onAuthorized(result)
                } else {
                    sessionStore.clearSessionKey()
                    setInfo("Подключено. Войдите в аккаунт")
                }
            } else {
                setInfo("Подключено. Войдите в аккаунт")
            }
        } else {
            setInfo("Подключено. Войдите в аккаунт")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BodyColor)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val isMobile = maxWidth < 768.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BlockColor)
            ) {
                if (isMobile) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .align(Alignment.Center)
                                    .padding(top = 24.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = when (page) {
                                        AuthPage.LOGIN -> "Вход"
                                        AuthPage.REGISTER -> "Создать аккаунт"
                                        AuthPage.VERIFY -> "Подтвердите почту"
                                    },
                                    color = TitleColor,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                if (statusText.isNotBlank()) {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                when (page) {
                                    AuthPage.LOGIN -> {
                                        AuthInput(
                                            value = loginEmail,
                                            onValueChange = { loginEmail = it },
                                            placeholder = "Email"
                                        )

                                        AuthInput(
                                            value = loginPassword,
                                            onValueChange = { loginPassword = it },
                                            placeholder = "Пароль",
                                            visualTransformation = PasswordVisualTransformation()
                                        )

                                        PrimaryButton(
                                            title = "Войти",
                                            loading = loginLoading,
                                            onClick = {
                                                scope.launch {
                                                    if (loginEmail.isBlank() || loginPassword.isBlank()) {
                                                        setError("Заполните email и пароль")
                                                        return@launch
                                                    }
                                                    if (loginLoading) return@launch

                                                    loginLoading = true
                                                    try {
                                                        if (!ensureSocketReady()) return@launch

                                                        val result = authGateway.login(loginEmail.trim(), loginPassword)
                                                        when {
                                                            result.isSuccess -> {
                                                                val sKey = result.sessionKey
                                                                if (sKey.isNullOrBlank()) {
                                                                    setError("Сервер не вернул S_KEY")
                                                                } else {
                                                                    finalizeSession(sKey)
                                                                }
                                                            }

                                                            result.requiresEmailVerification -> {
                                                                verifyEmail = result.email ?: loginEmail.trim()
                                                                verifyCode = ""
                                                                page = AuthPage.VERIFY
                                                                setInfo(result.message ?: "Подтвердите почту")
                                                            }

                                                            else -> setError(result.message ?: "Ошибка авторизации")
                                                        }
                                                    } finally {
                                                        loginLoading = false
                                                    }
                                                }
                                            }
                                        )

                                        SecondaryButton(
                                            title = if (showSessionKeyLogin) "Скрыть вход по S_KEY" else "Войти по секретному ключу (S_KEY)",
                                            onClick = { showSessionKeyLogin = !showSessionKeyLogin }
                                        )

                                        if (showSessionKeyLogin) {
                                            AuthInput(
                                                value = sessionKeyInput,
                                                onValueChange = { sessionKeyInput = it },
                                                placeholder = "Секретный ключ (S_KEY)"
                                            )
                                            PrimaryButton(
                                                title = "Подключить ключ",
                                                loading = loginLoading,
                                                onClick = {
                                                    scope.launch {
                                                        val key = sessionKeyInput.trim()
                                                        if (key.isBlank()) {
                                                            setError("Введите S_KEY")
                                                            return@launch
                                                        }
                                                        if (loginLoading) return@launch
                                                        loginLoading = true
                                                        try {
                                                            if (ensureSocketReady()) finalizeSession(key)
                                                        } finally {
                                                            loginLoading = false
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        SecondaryButton(
                                            title = "Создать аккаунт",
                                            onClick = { page = AuthPage.REGISTER }
                                        )
                                    }

                                    AuthPage.REGISTER -> {
                                        AuthInput(regName, { regName = it }, "Имя")
                                        AuthInput(regUsername, { regUsername = it }, "Username")
                                        AuthInput(regEmail, { regEmail = it }, "Email")
                                        AuthInput(regReferralCode, { regReferralCode = it }, "Реферальный код")

                                        AuthInput(
                                            value = regPassword,
                                            onValueChange = { regPassword = it },
                                            placeholder = "Пароль",
                                            visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                            trailingIcon = {
                                                UIKit.PlainIconButton(
                                                    icon = if (showRegPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    onClick = { showRegPassword = !showRegPassword }
                                                )
                                            }
                                        )

                                        AuthCaptcha(
                                            token = regCaptchaToken,
                                            resetNonce = regCaptchaResetNonce,
                                            onTokenChange = { regCaptchaToken = it }
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = regAcceptRules, onCheckedChange = { regAcceptRules = it })
                                            Text(
                                                text = "Принимаю правила сервиса",
                                                color = TextLiteColor,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        PrimaryButton(
                                            title = "Создать аккаунт",
                                            loading = regLoading,
                                            onClick = {
                                                scope.launch {
                                                    submitRegistration()
                                                }
                                            }
                                        )

                                        SecondaryButton(
                                            title = "У меня уже есть аккаунт",
                                            onClick = { page = AuthPage.LOGIN }
                                        )
                                    }

                                    AuthPage.VERIFY -> {
                                        Text(
                                            text = "Мы отправили код на $verifyEmail",
                                            color = TextLiteColor,
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        AuthInput(
                                            value = verifyCode,
                                            onValueChange = { verifyCode = it },
                                            placeholder = "Введите код из письма"
                                        )

                                        PrimaryButton(
                                            title = "Подтвердить",
                                            loading = verifyLoading,
                                            onClick = {
                                                scope.launch {
                                                    if (verifyEmail.isBlank() || verifyCode.isBlank()) {
                                                        setError("Введите email и код")
                                                        return@launch
                                                    }
                                                    if (verifyLoading) return@launch

                                                    verifyLoading = true
                                                    try {
                                                        if (!ensureSocketReady()) return@launch

                                                        val result = authGateway.verifyEmail(
                                                            email = verifyEmail.trim(),
                                                            code = verifyCode.trim()
                                                        )

                                                        if (result.isSuccess) {
                                                            val sKey = result.sessionKey
                                                            if (sKey.isNullOrBlank()) {
                                                                setError("Сервер не вернул S_KEY")
                                                            } else {
                                                                finalizeSession(sKey)
                                                            }
                                                        } else {
                                                            setError(result.message ?: "Неверный код")
                                                        }
                                                    } finally {
                                                        verifyLoading = false
                                                    }
                                                }
                                            }
                                        )

                                        PrimaryButton(
                                            title = if (resendCooldown > 0) "Отправить снова ($resendCooldown c)" else "Отправить снова",
                                            loading = resendLoading,
                                            enabled = resendCooldown <= 0 && !resendLoading,
                                            onClick = {
                                                scope.launch {
                                                    if (verifyEmail.isBlank()) {
                                                        setError("Введите email")
                                                        return@launch
                                                    }
                                                    if (resendCooldown > 0 || resendLoading) return@launch

                                                    resendLoading = true
                                                    try {
                                                        if (!ensureSocketReady()) return@launch

                                                        val result = authGateway.resendVerification(verifyEmail.trim())
                                                        if (result.isSuccess) {
                                                            resendCooldown = 120
                                                            setSuccess(result.message ?: "Код отправлен повторно")
                                                        } else {
                                                            setError(result.message ?: "Не удалось отправить код")
                                                        }
                                                    } finally {
                                                        resendLoading = false
                                                    }
                                                }
                                            }
                                        )

                                        SecondaryButton(
                                            title = "Назад ко входу",
                                            onClick = { page = AuthPage.LOGIN }
                                        )
                                    }
                                }
                            }

                            AuthFooter(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
                            )
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .fillMaxHeight()
                        ) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth(0.82f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (BodyColor.luminance() > 0.5f) {
                                            R.drawable.ic_element_logo
                                        } else {
                                            R.drawable.ic_element_logo_light
                                        }
                                    ),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(72.dp)
                                )

                                Text(
                                    text = "Element - Добро пожаловать",
                                    color = TitleColor,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val usersToShow = if (lastUsers.isEmpty()) {
                                        listOf(
                                            LastUser("User", "user", null),
                                            LastUser("Guest", "guest", null),
                                            LastUser("Anon", "anon", null)
                                        )
                                    } else {
                                        lastUsers.take(3)
                                    }

                                    usersToShow.forEach { user ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(InputColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = user.name.first().uppercase(),
                                                    color = TextColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = user.name,
                                                color = TextLiteColor,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            AuthFooter(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth(0.7f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = when (page) {
                                        AuthPage.LOGIN -> "Вход"
                                        AuthPage.REGISTER -> "Создать аккаунт"
                                        AuthPage.VERIFY -> "Подтвердите почту"
                                    },
                                    color = TitleColor,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                if (statusText.isNotBlank()) {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                when (page) {
                                    AuthPage.LOGIN -> {
                                        AuthInput(loginEmail, { loginEmail = it }, "Email")
                                        AuthInput(
                                            value = loginPassword,
                                            onValueChange = { loginPassword = it },
                                            placeholder = "Пароль",
                                            visualTransformation = PasswordVisualTransformation()
                                        )

                                        PrimaryButton(
                                            title = "Войти",
                                            loading = loginLoading,
                                            onClick = {
                                                scope.launch {
                                                    if (loginEmail.isBlank() || loginPassword.isBlank()) {
                                                        setError("Заполните email и пароль")
                                                        return@launch
                                                    }
                                                    if (loginLoading) return@launch

                                                    loginLoading = true
                                                    try {
                                                        if (!ensureSocketReady()) return@launch

                                                        val result = authGateway.login(loginEmail.trim(), loginPassword)
                                                        when {
                                                            result.isSuccess -> {
                                                                val sKey = result.sessionKey
                                                                if (sKey.isNullOrBlank()) {
                                                                    setError("Сервер не вернул S_KEY")
                                                                } else {
                                                                    finalizeSession(sKey)
                                                                }
                                                            }

                                                            result.requiresEmailVerification -> {
                                                                verifyEmail = result.email ?: loginEmail.trim()
                                                                verifyCode = ""
                                                                page = AuthPage.VERIFY
                                                                setInfo(result.message ?: "Подтвердите почту")
                                                            }

                                                            else -> setError(result.message ?: "Ошибка авторизации")
                                                        }
                                                    } finally {
                                                        loginLoading = false
                                                    }
                                                }
                                            }
                                        )

                                        SecondaryButton(
                                            title = if (showSessionKeyLogin) "Скрыть вход по S_KEY" else "Войти по секретному ключу (S_KEY)",
                                            onClick = { showSessionKeyLogin = !showSessionKeyLogin }
                                        )

                                        if (showSessionKeyLogin) {
                                            AuthInput(
                                                value = sessionKeyInput,
                                                onValueChange = { sessionKeyInput = it },
                                                placeholder = "Секретный ключ (S_KEY)"
                                            )
                                            PrimaryButton(
                                                title = "Подключить ключ",
                                                loading = loginLoading,
                                                onClick = {
                                                    scope.launch {
                                                        val key = sessionKeyInput.trim()
                                                        if (key.isBlank()) {
                                                            setError("Введите S_KEY")
                                                            return@launch
                                                        }
                                                        if (loginLoading) return@launch
                                                        loginLoading = true
                                                        try {
                                                            if (ensureSocketReady()) finalizeSession(key)
                                                        } finally {
                                                            loginLoading = false
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        SecondaryButton(
                                            title = "Создать аккаунт",
                                            onClick = { page = AuthPage.REGISTER }
                                        )
                                    }

                                    AuthPage.REGISTER -> {
                                        AuthInput(regName, { regName = it }, "Имя")
                                        AuthInput(regUsername, { regUsername = it }, "Username")
                                        AuthInput(regEmail, { regEmail = it }, "Email")
                                        AuthInput(regReferralCode, { regReferralCode = it }, "Реферальный код")
                                        AuthInput(
                                            value = regPassword,
                                            onValueChange = { regPassword = it },
                                            placeholder = "Пароль",
                                            visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                            trailingIcon = {
                                                UIKit.PlainIconButton(
                                                    icon = if (showRegPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    onClick = { showRegPassword = !showRegPassword }
                                                )
                                            }
                                        )

                                        AuthCaptcha(
                                            token = regCaptchaToken,
                                            resetNonce = regCaptchaResetNonce,
                                            onTokenChange = { regCaptchaToken = it }
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = regAcceptRules, onCheckedChange = { regAcceptRules = it })
                                            Text(
                                                text = "Принимаю правила сервиса",
                                                color = TextLiteColor,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        PrimaryButton(
                                            title = "Создать аккаунт",
                                            loading = regLoading,
                                            onClick = {
                                                scope.launch {
                                                    submitRegistration()
                                                }
                                            }
                                        )

                                        SecondaryButton(
                                            title = "У меня уже есть аккаунт",
                                            onClick = { page = AuthPage.LOGIN }
                                        )
                                    }

                                    AuthPage.VERIFY -> {
                                        Text(
                                            text = "Мы отправили код на $verifyEmail",
                                            color = TextLiteColor,
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        AuthInput(verifyCode, { verifyCode = it }, "Введите код из письма")

                                        PrimaryButton(
                                            title = "Подтвердить",
                                            loading = verifyLoading,
                                            onClick = {
                                                scope.launch {
                                                    if (verifyEmail.isBlank() || verifyCode.isBlank()) {
                                                        setError("Введите email и код")
                                                        return@launch
                                                    }
                                                    if (verifyLoading) return@launch

                                                    verifyLoading = true
                                                    try {
                                                        if (!ensureSocketReady()) return@launch

                                                        val result = authGateway.verifyEmail(
                                                            email = verifyEmail.trim(),
                                                            code = verifyCode.trim()
                                                        )

                                                        if (result.isSuccess) {
                                                            val sKey = result.sessionKey
                                                            if (sKey.isNullOrBlank()) {
                                                                setError("Сервер не вернул S_KEY")
                                                            } else {
                                                                finalizeSession(sKey)
                                                            }
                                                        } else {
                                                            setError(result.message ?: "Неверный код")
                                                        }
                                                    } finally {
                                                        verifyLoading = false
                                                    }
                                                }
                                            }
                                        )

                                        PrimaryButton(
                                            title = if (resendCooldown > 0) "Отправить снова ($resendCooldown c)" else "Отправить снова",
                                            loading = resendLoading,
                                            enabled = resendCooldown <= 0 && !resendLoading,
                                            onClick = {
                                                scope.launch {
                                                    if (verifyEmail.isBlank()) {
                                                        setError("Введите email")
                                                        return@launch
                                                    }
                                                    if (resendCooldown > 0 || resendLoading) return@launch

                                                    resendLoading = true
                                                    try {
                                                        if (!ensureSocketReady()) return@launch

                                                        val result = authGateway.resendVerification(verifyEmail.trim())
                                                        if (result.isSuccess) {
                                                            resendCooldown = 120
                                                            setSuccess(result.message ?: "Код отправлен повторно")
                                                        } else {
                                                            setError(result.message ?: "Не удалось отправить код")
                                                        }
                                                    } finally {
                                                        resendLoading = false
                                                    }
                                                }
                                            }
                                        )

                                        SecondaryButton(
                                            title = "Назад ко входу",
                                            onClick = { page = AuthPage.LOGIN }
                                        )
                                    }
                                }

                                if (lastSocketLog.isNotBlank() && statusColor == ErrorColor) {
                                    Text(
                                        text = "Log: $lastSocketLog",
                                        color = TextLiteColor,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
