package elemsocial.com

import android.app.Activity
import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import elemsocial.com.config.AppConfig
import elemsocial.com.core.cache.ImageDiskCache
import elemsocial.com.core.cache.MusicCacheIndexStore
import elemsocial.com.core.model.TransparencyMode
import elemsocial.com.core.notifications.ElementNotificationManager
import elemsocial.com.core.session.AccountSessionsStore
import elemsocial.com.core.session.SessionStore
import elemsocial.com.core.settings.AppSettingsStore
import elemsocial.com.core.settings.AppThemeMode
import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.data.repository.parseNotification
import elemsocial.com.data.repository.AuthRepositoryImpl
import elemsocial.com.data.repository.HallRepositoryImpl
import elemsocial.com.data.repository.MusicRepositoryImpl
import elemsocial.com.data.repository.MusicStorageRepositoryImpl
import elemsocial.com.data.repository.NotificationsRepositoryImpl
import elemsocial.com.data.repository.PostsRepositoryImpl
import elemsocial.com.data.repository.ProfileRepositoryImpl
import elemsocial.com.data.repository.SearchRepositoryImpl
import elemsocial.com.data.repository.WalletRepositoryImpl
import elemsocial.com.domain.model.AuthAccountChannel
import elemsocial.com.domain.model.AuthGoldHistory
import elemsocial.com.domain.model.AuthResult
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostsCategory
import elemsocial.com.feature.auth.presentation.AuthGateway
import elemsocial.com.feature.hall.presentation.HallGateway
import elemsocial.com.feature.auth.presentation.AuthScreen
import elemsocial.com.feature.home.presentation.HomeGateway
import elemsocial.com.feature.main.presentation.MainShell
import elemsocial.com.feature.music.presentation.LocalMusicController
import elemsocial.com.feature.music.presentation.LocalMusicGateway
import elemsocial.com.feature.music.presentation.MusicController
import elemsocial.com.feature.music.presentation.MusicGateway
import elemsocial.com.feature.music.presentation.MusicRuntime
import elemsocial.com.feature.notifications.presentation.NotificationsGateway
import elemsocial.com.feature.profile.presentation.ProfileGateway
import elemsocial.com.feature.search.presentation.SearchGateway
import elemsocial.com.feature.wallet.presentation.WalletGateway
import elemsocial.com.ui.pack.components.feedback.ElementLaunchConnectSplash
import elemsocial.com.ui.pack.components.feedback.ElementSocketDynamicIsland
import elemsocial.com.ui.pack.theme.ElementUiPalette
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.launch

private enum class LaunchState {
    Checking,
    Unauthorized,
    Authorized
}

private enum class StoredAuthAttempt {
    Authorized,
    NoStoredSessions,
    RetryableFailure
}

private fun resolveWsUrls(raw: String): List<String> {
    val typed = raw
        .split(',', ';', '\n', ' ')
        .map { it.trim() }
        .filter { it.startsWith("ws://") || it.startsWith("wss://") }

    return (typed + AppConfig.Domains.DEFAULT_USER_WS_URLS).distinct()
}

private fun normalizeUsername(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isBlank()) return null

    return trimmed
        .removePrefix("@")
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .trim()
        .takeIf { it.isNotBlank() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val socketClient = remember { ElementSocketClient() }
            val authRepository = remember { AuthRepositoryImpl(socketClient) }
            val authGateway = remember { AuthGateway(authRepository) }
            val imageDiskCache = remember(context) { ImageDiskCache(context) }
            val postsRepository = remember { PostsRepositoryImpl(socketClient) }
            val homeGateway = remember { HomeGateway(postsRepository, imageDiskCache) }
            val musicRuntime = remember(context.applicationContext) {
                MusicRuntime.get(context.applicationContext)
            }
            val musicGateway = remember(musicRuntime) { musicRuntime.gateway }
            val musicController = remember(musicRuntime) { musicRuntime.controller }
            val profileRepository = remember { ProfileRepositoryImpl(socketClient) }
            val searchRepository = remember { SearchRepositoryImpl(socketClient) }
            val notificationsRepository = remember { NotificationsRepositoryImpl(socketClient) }
            val hallRepository = remember { HallRepositoryImpl(socketClient) }
            val walletRepository = remember { WalletRepositoryImpl(socketClient) }
            val profileGateway = remember { ProfileGateway(profileRepository) }
            val searchGateway = remember { SearchGateway(searchRepository) }
            val notificationsGateway = remember { NotificationsGateway(notificationsRepository) }
            val hallGateway = remember { HallGateway(hallRepository) }
            val walletGateway = remember { WalletGateway(walletRepository) }
            val sessionStore = remember(context) { SessionStore(context) }
            val accountsStore = remember(context) { AccountSessionsStore(context) }
            val settingsStore = remember(context) { AppSettingsStore(context) }
            val notificationManager = remember(context) {
                ElementNotificationManager(context, settingsStore)
            }
            val appScope = rememberCoroutineScope()
            val connectionState by socketClient.connectionState.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    notificationManager.ensureChannels()
                }
            }

            var launchState by remember { mutableStateOf(LaunchState.Checking) }
            var accountName by remember { mutableStateOf<String?>(null) }
            var accountEmail by remember { mutableStateOf<String?>(null) }
            var accountUsername by remember { mutableStateOf<String?>(null) }
            var accountAvatar by remember { mutableStateOf<PostImageAsset?>(null) }
            var accountBalance by remember { mutableStateOf<Double?>(null) }
            var accountChannels by remember { mutableStateOf<List<AuthAccountChannel>>(emptyList()) }
            var goldStatus by remember { mutableStateOf(false) }
            var goldHistory by remember { mutableStateOf<List<AuthGoldHistory>>(emptyList()) }
            var notificationsCount by remember { mutableStateOf(0) }
            var accountId by remember { mutableStateOf<Int?>(null) }
            var isAdmin by remember { mutableStateOf(false) }
            var savedAccounts by remember { mutableStateOf(accountsStore.getAccounts()) }
            var activeSessionKey by remember { mutableStateOf(accountsStore.getActiveSessionKey()) }
            var themeMode by remember { mutableStateOf(settingsStore.getThemeMode()) }
            var defaultFeed by remember {
                mutableStateOf(parseDefaultFeed(settingsStore.getDefaultFeed()))
            }
            var notificationsToastEnabled by remember {
                mutableStateOf(settingsStore.getNotificationsToastEnabled())
            }
            var autoVideoDownloadEnabled by remember {
                mutableStateOf(settingsStore.getAutoVideoDownloadEnabled())
            }
            var videoAutoplayEnabled by remember {
                mutableStateOf(settingsStore.getVideoAutoplayEnabled())
            }
            var transparencyMode by remember {
                mutableStateOf(settingsStore.getTransparencyMode())
            }
            var requestedNotificationPermissionThisSession by remember { mutableStateOf(false) }
            var deliveredNotificationIds by remember { mutableStateOf(setOf<Int>()) }

            val effectiveThemeMode = when (themeMode) {
                AppThemeMode.System -> if (systemDark) AppThemeMode.Dark else AppThemeMode.Light
                else -> themeMode
            }

            fun hasNotificationPermission(): Boolean {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }

            LaunchedEffect(themeMode, systemDark) {
                if (themeMode == AppThemeMode.System) {
                    themeMode = effectiveThemeMode
                    settingsStore.saveThemeMode(effectiveThemeMode)
                }
            }

            LaunchedEffect(Unit) {
                notificationManager.ensureChannels()
            }

            fun clearActiveAccountState() {
                socketClient.setAuthorizationSessionKey(null)
                musicRuntime.clearSession()
                accountName = null
                accountEmail = null
                accountUsername = null
                accountAvatar = null
                accountBalance = null
                accountChannels = emptyList()
                goldStatus = false
                goldHistory = emptyList()
                notificationsCount = 0
                deliveredNotificationIds = emptySet()
                accountId = null
                isAdmin = false
            }

            fun refreshSavedAccounts() {
                savedAccounts = accountsStore.getAccounts()
                activeSessionKey = accountsStore.getActiveSessionKey()
            }

            fun applyAuthorizedState(auth: AuthResult, sessionKey: String, persist: Boolean) {
                val normalized = sessionKey.trim()
                val fallback = savedAccounts.firstOrNull { it.sessionKey == normalized }
                deliveredNotificationIds = emptySet()
                if (normalized.isNotBlank()) {
                    socketClient.setAuthorizationSessionKey(normalized)
                    musicRuntime.updateSessionKey(normalized)
                }
                if (persist && normalized.isNotBlank()) {
                    accountsStore.upsertFromAuth(normalized, auth)
                    sessionStore.saveSessionKey(normalized)
                    refreshSavedAccounts()
                }

                accountName = auth.accountName ?: fallback?.name
                accountEmail = auth.email
                accountUsername = normalizeUsername(auth.accountUsername)
                    ?: normalizeUsername(fallback?.username)
                accountAvatar = auth.accountAvatar ?: fallback?.avatar
                accountBalance = auth.accountBalance ?: fallback?.balance
                accountChannels = auth.accountChannels
                goldStatus = auth.goldStatus
                goldHistory = auth.goldHistory
                notificationsCount = auth.notificationsCount
                accountId = auth.accountId ?: fallback?.accountId
                isAdmin = auth.isAdmin
                launchState = LaunchState.Authorized
            }

            LaunchedEffect(launchState, activeSessionKey) {
                if (launchState == LaunchState.Authorized) {
                    musicRuntime.updateSessionKey(sessionStore.getSessionKey() ?: activeSessionKey)
                } else if (launchState == LaunchState.Unauthorized) {
                    musicRuntime.clearSession()
                }
            }

            suspend fun authorizeBySession(
                sessionKey: String,
                persist: Boolean,
                removeInvalid: Boolean,
                forceReconnect: Boolean = false
            ): Boolean {
                val normalized = sessionKey.trim()
                if (normalized.isBlank()) return false
                socketClient.setAuthorizationSessionKey(normalized)

                if (forceReconnect) {
                    socketClient.disconnect(clearQueue = true, disableReconnect = true)
                }

                val connected = runCatching {
                    socketClient.connectAndAwaitReady(
                        resolveWsUrls(AppConfig.Domains.DEFAULT_USER_WS_URL),
                        timeoutMs = 35_000
                    )
                }.getOrDefault(false)

                if (!connected) return false

                val auth = runCatching {
                    authGateway.connectBySessionKey(normalized)
                }.getOrNull()

                if (auth == null) {
                    return false
                }

                if (auth.isAccountNotFound) {
                    if (removeInvalid) {
                        accountsStore.removeSession(normalized)
                        refreshSavedAccounts()
                        if (sessionStore.getSessionKey() == normalized) {
                            sessionStore.clearSessionKey()
                        }
                    }
                    socketClient.setAuthorizationSessionKey(null)
                    return false
                }

                val fallback = savedAccounts.firstOrNull { it.sessionKey == normalized }
                val hasIdentity = auth.hasAccountIdentity ||
                    fallback?.accountId != null ||
                    !normalizeUsername(fallback?.username).isNullOrBlank()

                if (auth.isSuccess && hasIdentity) {
                    applyAuthorizedState(
                        auth = auth.copy(sessionKey = normalized),
                        sessionKey = normalized,
                        persist = persist
                    )
                    return true
                }

                if (!auth.isSuccess) {
                    socketClient.setAuthorizationSessionKey(null)
                    return false
                }

                if (removeInvalid) {
                    accountsStore.removeSession(normalized)
                    refreshSavedAccounts()
                    if (sessionStore.getSessionKey() == normalized) {
                        sessionStore.clearSessionKey()
                    }
                }
                socketClient.setAuthorizationSessionKey(null)
                return false
            }

            suspend fun tryAuthorizeStoredSessions(): StoredAuthAttempt {
                val accountsFromStore = accountsStore.getAccounts()
                val primary = accountsStore.getActiveSessionKey()
                val legacy = sessionStore.getSessionKey()

                val candidates = buildList {
                    primary?.takeIf { it.isNotBlank() }?.let(::add)
                    accountsFromStore.forEach { add(it.sessionKey) }
                    legacy?.takeIf { it.isNotBlank() }?.let(::add)
                }.distinct()

                if (candidates.isEmpty()) return StoredAuthAttempt.NoStoredSessions

                for (candidate in candidates) {
                    val success = authorizeBySession(
                        sessionKey = candidate,
                        persist = true,
                        removeInvalid = true
                    )
                    if (success) {
                        return StoredAuthAttempt.Authorized
                    }
                }

                return if (accountsStore.getAccounts().isNotEmpty() || !sessionStore.getSessionKey().isNullOrBlank()) {
                    StoredAuthAttempt.RetryableFailure
                } else {
                    sessionStore.clearSessionKey()
                    StoredAuthAttempt.NoStoredSessions
                }
            }

            LaunchedEffect(Unit) {
                launchState = LaunchState.Checking
                while (launchState == LaunchState.Checking) {
                    when (tryAuthorizeStoredSessions()) {
                        StoredAuthAttempt.Authorized -> return@LaunchedEffect
                        StoredAuthAttempt.NoStoredSessions -> {
                            clearActiveAccountState()
                            refreshSavedAccounts()
                            launchState = LaunchState.Unauthorized
                        }
                        StoredAuthAttempt.RetryableFailure -> {
                            refreshSavedAccounts()
                            kotlinx.coroutines.delay(1_500)
                        }
                    }
                }
            }

            LaunchedEffect(socketClient, launchState, accountId) {
                socketClient.events.collect { payload ->
                    if (launchState != LaunchState.Authorized || accountId == null) {
                        return@collect
                    }

                    val type = payload["type"]?.toString()
                    val action = payload["action"]?.toString()
                    if (type != "social" || action != "notify") {
                        return@collect
                    }

                    runCatching {
                        val incoming = parseNotification(payload["notification"]) ?: return@runCatching
                        notificationsCount = (notificationsCount + 1).coerceAtLeast(0)
                        if (!deliveredNotificationIds.contains(incoming.id)) {
                            notificationManager.show(incoming)
                            deliveredNotificationIds = deliveredNotificationIds + incoming.id
                        }
                        val unreadResult = notificationsGateway.loadNotifications(0)
                        if (unreadResult.isSuccess) {
                            unreadResult.notifications
                                .filter { !it.viewed }
                                .asReversed()
                                .forEach { unread ->
                                    if (!deliveredNotificationIds.contains(unread.id)) {
                                        notificationManager.show(unread)
                                        deliveredNotificationIds = deliveredNotificationIds + unread.id
                                    }
                                }
                        }
                    }.onFailure { error ->
                        Log.e("ElementNotifications", "Failed to display socket notification", error)
                    }
                }
            }

            LaunchedEffect(launchState, notificationsToastEnabled) {
                if (launchState != LaunchState.Authorized) return@LaunchedEffect
                if (!notificationsToastEnabled) return@LaunchedEffect
                if (requestedNotificationPermissionThisSession) return@LaunchedEffect
                if (hasNotificationPermission()) return@LaunchedEffect
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect

                requestedNotificationPermissionThisSession = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            AppThemeWrapper(themeMode = effectiveThemeMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(
                        LocalMusicGateway provides musicGateway,
                        LocalMusicController provides musicController
                    ) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            when (launchState) {
                                LaunchState.Checking -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                            .background(ElementUiPalette.Body),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ElementLaunchConnectSplash(
                                            wsUrl = AppConfig.Domains.DEFAULT_USER_WS_URL,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 44.dp)
                                        )
                                    }
                                }

                                LaunchState.Authorized -> {
                                    MainShell(
                                        accountName = accountName,
                                        accountEmail = accountEmail,
                                        accountUsername = accountUsername,
                                        accountAvatar = accountAvatar,
                                        accountBalance = accountBalance,
                                        accountChannels = accountChannels,
                                        goldStatus = goldStatus,
                                        goldHistory = goldHistory,
                                        notificationsCount = notificationsCount,
                                        accountId = accountId,
                                        isAdmin = isAdmin,
                                        savedAccounts = savedAccounts,
                                        activeAccountSessionKey = activeSessionKey,
                                        homeGateway = homeGateway,
                                        profileGateway = profileGateway,
                                        searchGateway = searchGateway,
                                        notificationsGateway = notificationsGateway,
                                        hallGateway = hallGateway,
                                        walletGateway = walletGateway,
                                        authGateway = authGateway,
                                        defaultFeed = defaultFeed,
                                        themeMode = effectiveThemeMode,
                                        notificationsToastEnabled = notificationsToastEnabled,
                                        autoVideoDownloadEnabled = autoVideoDownloadEnabled,
                                        videoAutoplayEnabled = videoAutoplayEnabled,
                                        transparencyMode = transparencyMode,
                                        onDefaultFeedChanged = { category ->
                                            defaultFeed = category
                                            settingsStore.saveDefaultFeed(category.apiValue)
                                        },
                                        onThemeChanged = { mode ->
                                            themeMode = mode
                                            settingsStore.saveThemeMode(mode)
                                        },
                                        onNotificationsToastChanged = { enabled ->
                                            notificationsToastEnabled = enabled
                                            settingsStore.saveNotificationsToastEnabled(enabled)
                                            if (enabled && hasNotificationPermission()) {
                                                notificationManager.ensureChannels()
                                            } else if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                requestedNotificationPermissionThisSession = true
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        },
                                        onAutoVideoDownloadChanged = { enabled ->
                                            autoVideoDownloadEnabled = enabled
                                            settingsStore.saveAutoVideoDownloadEnabled(enabled)
                                        },
                                        onVideoAutoplayChanged = { enabled ->
                                            videoAutoplayEnabled = enabled
                                            settingsStore.saveVideoAutoplayEnabled(enabled)
                                        },
                                        onTransparencyModeChanged = { mode ->
                                            transparencyMode = mode
                                            settingsStore.saveTransparencyMode(mode)
                                        },
                                        onAccountEmailUpdated = { updatedEmail ->
                                            accountEmail = updatedEmail
                                        },
                                        onAccountUsernameUpdated = { updatedUsername ->
                                            accountUsername = normalizeUsername(updatedUsername)
                                        },
                                        onSwitchAccount = { targetSession ->
                                            appScope.launch {
                                                if (targetSession == activeSessionKey) {
                                                    return@launch
                                                }

                                                val switched = authorizeBySession(
                                                    sessionKey = targetSession,
                                                    persist = true,
                                                    removeInvalid = true,
                                                    forceReconnect = true
                                                )

                                                if (!switched) {
                                                    val fallback = accountsStore.getAccounts()
                                                        .firstOrNull()
                                                        ?.sessionKey

                                                    val fallbackSwitched = if (!fallback.isNullOrBlank()) {
                                                        authorizeBySession(
                                                            sessionKey = fallback,
                                                            persist = true,
                                                            removeInvalid = true
                                                        )
                                                    } else {
                                                        false
                                                    }

                                                    if (!fallbackSwitched) {
                                                        clearActiveAccountState()
                                                        refreshSavedAccounts()
                                                        launchState = LaunchState.Unauthorized
                                                    }
                                                }
                                            }
                                        },
                                        onAddAccount = {
                                            appScope.launch {
                                                sessionStore.clearSessionKey()
                                                socketClient.setAuthorizationSessionKey(null)
                                                socketClient.disconnect(clearQueue = true, disableReconnect = true)
                                                clearActiveAccountState()
                                                launchState = LaunchState.Unauthorized
                                            }
                                        },
                                        onLogout = {
                                            appScope.launch {
                                                val currentSession = activeSessionKey
                                                    ?: accountsStore.getActiveSessionKey()
                                                    ?: sessionStore.getSessionKey()

                                                currentSession
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?.let { key ->
                                                        runCatching { authGateway.logout(key) }
                                                        accountsStore.removeSession(key)
                                                    }

                                                sessionStore.clearSessionKey()
                                                refreshSavedAccounts()
                                                socketClient.disconnect(clearQueue = true, disableReconnect = true)

                                                val nextSession = accountsStore.getAccounts()
                                                    .firstOrNull()
                                                    ?.sessionKey
                                                if (!nextSession.isNullOrBlank()) {
                                                    launchState = LaunchState.Checking
                                                    val switched = authorizeBySession(
                                                        sessionKey = nextSession,
                                                        persist = true,
                                                        removeInvalid = true
                                                    )
                                                    if (!switched) {
                                                        clearActiveAccountState()
                                                        refreshSavedAccounts()
                                                        launchState = LaunchState.Unauthorized
                                                    }
                                                } else {
                                                    clearActiveAccountState()
                                                    launchState = LaunchState.Unauthorized
                                                }
                                            }
                                        },
                                        onNotificationsCountChange = { next ->
                                            notificationsCount = next.coerceAtLeast(0)
                                        },
                                        socketClient = socketClient,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                LaunchState.Unauthorized -> {
                                    AuthScreen(
                                        socketClient = socketClient,
                                        authGateway = authGateway,
                                        sessionStore = sessionStore,
                                        attemptStoredSessionOnStart = false,
                                        onAuthorized = { auth ->
                                            val resolved = auth.sessionKey
                                                ?.trim()
                                                ?.takeIf { it.isNotBlank() }

                                            if (resolved.isNullOrBlank()) {
                                                if (!auth.hasAccountIdentity || auth.isAccountNotFound) {
                                                    clearActiveAccountState()
                                                    launchState = LaunchState.Unauthorized
                                                    return@AuthScreen
                                                }
                                                accountName = auth.accountName
                                                accountUsername = normalizeUsername(auth.accountUsername)
                                                accountAvatar = auth.accountAvatar
                                                accountBalance = auth.accountBalance
                                                accountChannels = auth.accountChannels
                                                notificationsCount = auth.notificationsCount
                                                accountId = auth.accountId
                                                isAdmin = auth.isAdmin
                                                launchState = LaunchState.Authorized
                                            } else {
                                                if (auth.hasAccountIdentity && !auth.isAccountNotFound) {
                                                    applyAuthorizedState(
                                                        auth = auth,
                                                        sessionKey = resolved,
                                                        persist = true
                                                    )
                                                } else {
                                                    appScope.launch {
                                                        val switched = authorizeBySession(
                                                            sessionKey = resolved,
                                                            persist = true,
                                                            removeInvalid = false,
                                                            forceReconnect = false
                                                        )
                                                        if (!switched) {
                                                            clearActiveAccountState()
                                                            refreshSavedAccounts()
                                                            launchState = LaunchState.Unauthorized
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    if (launchState != LaunchState.Checking) {
                        ElementSocketDynamicIsland(
                            connectionState = connectionState,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppThemeWrapper(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    SideEffect {
        ElementUiPalette.applyTheme(themeMode)
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        val lightBars = themeMode == AppThemeMode.Light
        controller.isAppearanceLightStatusBars = lightBars
        controller.isAppearanceLightNavigationBars = lightBars
    }

    elemsocial.com.ui.theme.ElementTheme(
        themeMode = themeMode,
        dynamicColor = false,
        content = content
    )
}

private fun parseDefaultFeed(raw: String): PostsCategory {
    return PostsCategory.entries.firstOrNull { it.apiValue == raw } ?: PostsCategory.Last
}
