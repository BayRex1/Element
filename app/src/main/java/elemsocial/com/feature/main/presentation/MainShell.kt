package elemsocial.com.feature.main.presentation

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.feature.settings.presentation.ElementumSettingsScreen
import elemsocial.com.R
import elemsocial.com.core.model.LocalTransparencyMode
import elemsocial.com.core.model.glassAlphaFor
import elemsocial.com.core.model.glassSoftAlphaFor
import elemsocial.com.core.model.shouldUseGlassBlur
import elemsocial.com.core.model.TransparencyMode
import elemsocial.com.core.session.SavedAccountSession
import elemsocial.com.core.settings.AppThemeMode
import elemsocial.com.domain.model.AuthAccountChannel
import elemsocial.com.domain.model.AuthGoldHistory
import elemsocial.com.domain.model.MusicDownloadItem
import elemsocial.com.domain.model.MusicPlayerState
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostsCategory
import elemsocial.com.feature.auth.presentation.AuthGateway
import elemsocial.com.feature.hall.presentation.HallGateway
import elemsocial.com.feature.hall.presentation.HallScreen
import elemsocial.com.feature.home.presentation.HomeGateway
import elemsocial.com.feature.home.presentation.HomeScreen
import elemsocial.com.feature.music.presentation.LocalMusicController
import elemsocial.com.feature.music.presentation.MusicCompactMetaBlock
import elemsocial.com.feature.music.presentation.MusicFullPlayerOverlay
import elemsocial.com.feature.music.presentation.MusicMiniPlayerBar
import elemsocial.com.feature.music.presentation.MusicProgressRow
import elemsocial.com.feature.music.presentation.MusicScreen
import elemsocial.com.feature.music.presentation.isMusicTrackPlaybackActive
import elemsocial.com.feature.notifications.presentation.NotificationsGateway
import elemsocial.com.feature.notifications.presentation.NotificationsScreen
import elemsocial.com.feature.profile.presentation.ProfileGateway
import elemsocial.com.feature.profile.presentation.ProfileScreen
import elemsocial.com.feature.search.presentation.SearchGateway
import elemsocial.com.feature.search.presentation.SearchOverlay
import elemsocial.com.feature.settings.presentation.SettingsScreen
import elemsocial.com.feature.wallet.presentation.WalletGateway
import elemsocial.com.feature.wallet.presentation.WalletScreen
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.components.navigation.ElementBottomNavItem
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private enum class MainTab(
    val title: String,
    val iconRes: Int
) {
    Home("Главная", R.drawable.ic_nav_home),
    Notifications("Уведомления", R.drawable.ic_nav_notifications),
    Wallet("Кошелёк", R.drawable.ic_nav_wallet),
    Profile("Профиль", R.drawable.ic_nav_profile),
    Hall("Зал славы", R.drawable.ic_nav_panel),
    Panel("Панель", R.drawable.ic_nav_panel),
    Settings("Настройки", R.drawable.ic_nav_panel),
    Messenger("Мессенджер", R.drawable.ic_nav_messenger),
    Music("Музыка", R.drawable.ic_nav_music)
}

private val ShellContentTopPadding = 76.dp
private val ShellContentBottomPadding = 98.dp

private fun shellHeaderTopColor(transparencyMode: TransparencyMode): Color {
    return ElementUiPalette.Block.copy(alpha = glassAlphaFor(transparencyMode))
}

private fun shellHeaderSurfaceBrush(transparencyMode: TransparencyMode): Brush {
    val headerTopColor = shellHeaderTopColor(transparencyMode)
    if (transparencyMode == TransparencyMode.OPAQUE) {
        return Brush.verticalGradient(listOf(headerTopColor, headerTopColor))
    }

    val blurEnabled = shouldUseGlassBlur(transparencyMode)
    val headerAlpha = glassAlphaFor(transparencyMode)
    val headerSoftAlpha = glassSoftAlphaFor(transparencyMode)
    return Brush.verticalGradient(
        colors = listOf(
            headerTopColor,
            if (blurEnabled) {
                ElementUiPalette.Block.copy(alpha = (headerAlpha - 0.05f).coerceAtLeast(0f))
            } else {
                ElementUiPalette.BlockSoft.copy(alpha = headerSoftAlpha)
            }
        )
    )
}

private fun normalizeProfileUsername(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isBlank()) return null

    return trimmed
        .removePrefix("@")
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .trim()
        .takeIf { it.isNotBlank() }
}

@Composable
fun MainShell(
    accountName: String?,
    accountEmail: String?,
    accountUsername: String?,
    accountAvatar: PostImageAsset?,
    accountBalance: Double?,
    accountChannels: List<AuthAccountChannel>,
    goldStatus: Boolean,
    goldHistory: List<AuthGoldHistory>,
    notificationsCount: Int,
    accountId: Int?,
    isAdmin: Boolean,
    savedAccounts: List<SavedAccountSession>,
    activeAccountSessionKey: String?,
    homeGateway: HomeGateway,
    profileGateway: ProfileGateway,
    searchGateway: SearchGateway,
    notificationsGateway: NotificationsGateway,
    hallGateway: HallGateway,
    walletGateway: WalletGateway,
    authGateway: AuthGateway,
    defaultFeed: PostsCategory,
    themeMode: AppThemeMode,
    notificationsToastEnabled: Boolean,
    autoVideoDownloadEnabled: Boolean,
    videoAutoplayEnabled: Boolean,
    transparencyMode: TransparencyMode,
    onThemeChanged: (AppThemeMode) -> Unit,
    onDefaultFeedChanged: (PostsCategory) -> Unit,
    onNotificationsToastChanged: (Boolean) -> Unit,
    onAutoVideoDownloadChanged: (Boolean) -> Unit,
    onVideoAutoplayChanged: (Boolean) -> Unit,
    onTransparencyModeChanged: (TransparencyMode) -> Unit,
    onAccountEmailUpdated: (String) -> Unit,
    onAccountUsernameUpdated: (String) -> Unit,
    onSwitchAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    onLogout: () -> Unit,
    onNotificationsCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(MainTab.Home) }
    var tabHistory by remember { mutableStateOf<List<MainTab>>(emptyList()) }
    var shellAccountBalance by remember { mutableStateOf(accountBalance) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var openedProfileUsername by remember { mutableStateOf<String?>(null) }
    var requestedPostId by remember { mutableStateOf<Int?>(null) }
    var requestedProfilePostId by remember { mutableStateOf<Int?>(null) }
    var isSidebarOpen by remember { mutableStateOf(false) }
    var isChannelsExpanded by remember { mutableStateOf(false) }
    var isAccountsExpanded by remember { mutableStateOf(false) }
    var isHeaderMusicPlayerOpen by remember { mutableStateOf(false) }
    var isDownloadsOpen by remember { mutableStateOf(false) }
    var selectedComposerChannelId by remember { mutableStateOf<Int?>(null) }
    var editingChannel by remember { mutableStateOf<AuthAccountChannel?>(null) }
    var elementumSettingsOpen by remember { mutableStateOf(false) }
    var channelOverrides by remember { mutableStateOf<Map<Int, AuthAccountChannel>>(emptyMap()) }
    var refreshCounters by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var refreshingRouteId by remember { mutableStateOf<String?>(null) }
    val effectiveAccountChannels = remember(accountChannels, channelOverrides) {
        buildList {
            accountChannels.forEach { channel ->
                val override = channel.id?.let { channelOverrides[it] }
                add(override ?: channel)
            }
            channelOverrides.values.forEach { override ->
                if (none { it.id == override.id }) {
                    add(override)
                }
            }
        }
    }
    val selectedComposerChannel = remember(effectiveAccountChannels, selectedComposerChannelId) {
        effectiveAccountChannels.firstOrNull { it.id != null && it.id == selectedComposerChannelId }
    }
    val ownProfileUsername = remember(accountUsername) {
        normalizeProfileUsername(accountUsername)
    }
    val activeProfileUsername = openedProfileUsername
        ?: ownProfileUsername?.takeIf { currentTab == MainTab.Profile }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val musicController = LocalMusicController.current
    val musicState by musicController.state.collectAsState()
    val musicDownloads by musicController.downloads.collectAsState()
    val isSearchExpanded = isSearchFocused || searchQuery.isNotBlank()

    fun clearSearch() {
        searchQuery = ""
        isSearchFocused = false
        focusManager.clearFocus(force = true)
    }

    fun navigateToTab(tab: MainTab) {
        if (currentTab != tab) {
            tabHistory = (tabHistory + currentTab).takeLast(20)
            currentTab = tab
        }
    }

    fun popHistoryTab(): MainTab? {
        while (tabHistory.isNotEmpty()) {
            val candidate = tabHistory.last()
            tabHistory = tabHistory.dropLast(1)
            if (candidate != currentTab) return candidate
        }
        return null
    }

    fun routeIdForTab(tab: MainTab): String = "tab:${tab.name.lowercase(Locale.ROOT)}"

    fun routeIdForProfile(username: String): String {
        val normalized = normalizeProfileUsername(username)
            ?.lowercase(Locale.ROOT)
            ?: username.lowercase(Locale.ROOT)
        return "profile:$normalized"
    }

    fun routeIdForChannelSettings(channel: AuthAccountChannel): String {
        val suffix = channel.id?.toString()
            ?: normalizeProfileUsername(channel.username)?.lowercase(Locale.ROOT).orEmpty()
        return "settings:channel:$suffix"
    }

    fun currentRouteId(): String {
        return when {
            editingChannel != null -> routeIdForChannelSettings(editingChannel!!)
            activeProfileUsername != null -> routeIdForProfile(activeProfileUsername)
            else -> routeIdForTab(currentTab)
        }
    }

    fun refreshRoute(routeId: String) {
        refreshCounters = refreshCounters + (routeId to ((refreshCounters[routeId] ?: 0) + 1))
    }

    fun triggerPullRefresh(routeId: String = currentRouteId()) {
        if (refreshingRouteId != null) return
        refreshingRouteId = routeId
        refreshRoute(routeId)
        scope.launch {
            delay(700)
            if (refreshingRouteId == routeId) {
                refreshingRouteId = null
            }
        }
    }

    fun refreshCurrentPage() {
        triggerPullRefresh(currentRouteId())
    }

    fun refreshKey(routeId: String): Int = refreshCounters[routeId] ?: 0

    LaunchedEffect(accountBalance) {
        shellAccountBalance = accountBalance
    }

    LaunchedEffect(accountId) {
        channelOverrides = emptyMap()
        editingChannel = null
    }

    LaunchedEffect(effectiveAccountChannels) {
        val current = selectedComposerChannelId ?: return@LaunchedEffect
        if (effectiveAccountChannels.none { it.id != null && it.id == current }) {
            selectedComposerChannelId = null
        }
    }

    fun openPost(postId: Int) {
        if (activeProfileUsername != null) {
            requestedProfilePostId = postId
            clearSearch()
            return
        }
        navigateToTab(MainTab.Home)
        openedProfileUsername = null
        clearSearch()
        requestedPostId = postId
    }

    fun openProfile(username: String?): Boolean {
        val normalized = normalizeProfileUsername(username) ?: return false
        openedProfileUsername = normalized
        clearSearch()
        requestedPostId = null
        requestedProfilePostId = null
        return true
    }

    val navTabs = buildList {
        add(MainTab.Home)
        add(MainTab.Notifications)
        add(MainTab.Music)
        add(MainTab.Profile)
        // TODO(next-release): вернуть MainTab.Panel в нижнюю панель после релиза админ-экрана.
        // if (isAdmin) add(MainTab.Panel)
        // TODO(next-release): вернуть MainTab.Messenger в нижнюю панель после релиза мессенджера.
        // add(MainTab.Messenger)
    }
    val navItems = navTabs.map {
        ElementBottomNavItem(
            title = it.title,
            iconRes = it.iconRes,
            badgeCount = if (it == MainTab.Notifications) notificationsCount else null
        )
    }
    val homeRouteId = routeIdForTab(MainTab.Home)
    val notificationsRouteId = routeIdForTab(MainTab.Notifications)
    val walletRouteId = routeIdForTab(MainTab.Wallet)
    val hallRouteId = routeIdForTab(MainTab.Hall)
    val settingsRouteId = routeIdForTab(MainTab.Settings)
    val panelRouteId = routeIdForTab(MainTab.Panel)
    val messengerRouteId = routeIdForTab(MainTab.Messenger)
    val musicRouteId = routeIdForTab(MainTab.Music)
    val activeProfileRouteId = activeProfileUsername?.let(::routeIdForProfile)
    val activeRouteId = currentRouteId()
    val selectedBottomTab = when {
        activeProfileUsername != null -> MainTab.Profile
        navTabs.contains(currentTab) -> currentTab
        else -> null
    }
    val selectedBottomIndex = selectedBottomTab
        ?.let { navTabs.indexOf(it) }
        ?.takeIf { it >= 0 }
        ?: -1

    LaunchedEffect(currentTab, notificationsCount) {
        if (currentTab == MainTab.Notifications && notificationsCount > 0) {
            onNotificationsCountChange(0)
        }
    }

    LaunchedEffect(currentTab, isSearchExpanded) {
        if (currentTab == MainTab.Music || isSearchExpanded) {
            isHeaderMusicPlayerOpen = false
        }
        if (isSearchExpanded) {
            isDownloadsOpen = false
        }
    }

    val shouldHandleBackPress = isSidebarOpen ||
        isHeaderMusicPlayerOpen ||
        isDownloadsOpen ||
        editingChannel != null ||
        isSearchExpanded ||
        activeProfileUsername != null ||
        currentTab != MainTab.Home ||
        tabHistory.isNotEmpty()

    BackHandler(enabled = shouldHandleBackPress) {
        when {
            editingChannel != null -> {
                editingChannel = null
            }

            isSidebarOpen -> {
                isSidebarOpen = false
                isChannelsExpanded = false
                isAccountsExpanded = false
            }

            isHeaderMusicPlayerOpen -> {
                isHeaderMusicPlayerOpen = false
            }

            isDownloadsOpen -> {
                isDownloadsOpen = false
            }

            isSearchExpanded -> {
                clearSearch()
            }

            openedProfileUsername != null -> {
                openedProfileUsername = null
                requestedProfilePostId = null
                if (currentTab == MainTab.Profile) {
                    currentTab = popHistoryTab() ?: MainTab.Home
                }
            }

            currentTab != MainTab.Home || tabHistory.isNotEmpty() -> {
                val target = popHistoryTab() ?: MainTab.Home
                currentTab = target
                if (target != MainTab.Notifications) {
                    clearSearch()
                }
                requestedPostId = null
                requestedProfilePostId = null
                openedProfileUsername = null
            }
        }
    }

    CompositionLocalProvider(LocalTransparencyMode provides transparencyMode) {
    SearchKeyboardDismissEffect(
        enabled = isSearchExpanded,
        onKeyboardHidden = ::clearSearch
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ElementUiPalette.Body)
    ) {
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(ElementUiPalette.Body),
            isRefreshing = refreshingRouteId == activeRouteId,
            onRefresh = { refreshCurrentPage() }
        ) {
            if (editingChannel != null) {
    val channelSettingsRouteId = routeIdForChannelSettings(editingChannel!!)
    key(channelSettingsRouteId, refreshKey(channelSettingsRouteId)) {
        SettingsScreen(
            accountName = accountName,
            accountEmail = accountEmail,
            accountUsername = accountUsername,
            accountAvatar = accountAvatar,
            homeGateway = homeGateway,
            authGateway = authGateway,
            profileGateway = profileGateway,
            themeMode = themeMode,
            defaultFeed = defaultFeed,
            notificationsToastEnabled = notificationsToastEnabled,
            autoVideoDownloadEnabled = autoVideoDownloadEnabled,
            videoAutoplayEnabled = videoAutoplayEnabled,
            transparencyMode = transparencyMode,
            onThemeChanged = onThemeChanged,
            onDefaultFeedChanged = onDefaultFeedChanged,
            onNotificationsToastChanged = onNotificationsToastChanged,
            onAutoVideoDownloadChanged = onAutoVideoDownloadChanged,
            onVideoAutoplayChanged = onVideoAutoplayChanged,
            onTransparencyModeChanged = onTransparencyModeChanged,
            onAccountEmailUpdated = onAccountEmailUpdated,
            onAccountUsernameUpdated = onAccountUsernameUpdated,
            onLogout = onLogout,
            onOpenElementumSettings = {
                elementumSettingsOpen = true
            },
            channelToEdit = editingChannel,
            onChannelUpdated = { updated ->
                updated.id?.let { channelId ->
                    channelOverrides = channelOverrides + (channelId to updated)
                }
                val previousUsername = normalizeProfileUsername(editingChannel?.username)
                val updatedUsername = normalizeProfileUsername(updated.username)
                if (
                    !previousUsername.isNullOrBlank() &&
                    activeProfileUsername.equals(previousUsername, ignoreCase = true) &&
                    !updatedUsername.isNullOrBlank()
                ) {
                    openedProfileUsername = updatedUsername
                }
                editingChannel = updated
            },
            onBack = { editingChannel = null },
            modifier = Modifier.fillMaxSize()
        )
    }
}
            else if (activeProfileUsername != null) {
                key(activeProfileRouteId, activeProfileRouteId?.let(::refreshKey) ?: 0) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ProfileScreen(
                            username = activeProfileUsername.orEmpty(),
                            profileGateway = profileGateway,
                            homeGateway = homeGateway,
                            onBack = {
                                openedProfileUsername = null
                                requestedProfilePostId = null
                                if (currentTab == MainTab.Profile) {
                                    currentTab = popHistoryTab() ?: MainTab.Home
                                }
                            },
                            requestedOpenPostId = requestedProfilePostId,
                            onPostRequestHandled = { requestedProfilePostId = null },
                            onOpenProfile = { openProfile(it) },
                            isAdmin = isAdmin,
                            accountId = accountId,
                            accountName = accountName,
                            accountUsername = accountUsername,
                            accountAvatar = accountAvatar,
                            accountChannels = effectiveAccountChannels,
                            selectedComposerChannel = selectedComposerChannel,
                            onSelectedComposerChannelChange = { channel ->
                                selectedComposerChannelId = channel?.id
                            },
                            onOpenChannelSettings = { channel ->
                                editingChannel = channel
                                clearSearch()
                                requestedPostId = null
                                requestedProfilePostId = null
                            },
                            topPadding = ShellContentTopPadding,
                            bottomPadding = ShellContentBottomPadding,
                            modifier = Modifier.fillMaxSize()
                        )

                        SearchDismissBodyOverlay(
                            visible = isSearchExpanded,
                            onDismiss = ::clearSearch
                        )

                        ShellHeaderContainer {
                            ShellTopBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                searchExpanded = isSearchExpanded,
                                onSearchFocusChanged = { isSearchFocused = it },
                                accountName = accountName,
                                accountAvatar = accountAvatar,
                                accountBalance = shellAccountBalance,
                                homeGateway = homeGateway,
                                showMusicShortcut = currentTab != MainTab.Music && musicState.isSelected,
                                showDownloadsShortcut = musicDownloads.isNotEmpty(),
                                onLogoClick = {
                                    navigateToTab(MainTab.Home)
                                    openedProfileUsername = null
                                    clearSearch()
                                    requestedPostId = null
                                    requestedProfilePostId = null
                                },
                                onAvatarClick = { isSidebarOpen = true },
                                onBalanceClick = {
                                    navigateToTab(MainTab.Wallet)
                                    openedProfileUsername = null
                                    clearSearch()
                                    requestedPostId = null
                                    requestedProfilePostId = null
                                },
                                onMusicClick = {
                                    isHeaderMusicPlayerOpen = true
                                    isDownloadsOpen = false
                                },
                                onDownloadsClick = {
                                    isDownloadsOpen = true
                                    isHeaderMusicPlayerOpen = false
                                }
                            )

                            AnimatedVisibility(
                                visible = searchQuery.isNotBlank(),
                                enter = fadeIn(tween(durationMillis = 100)) + slideInVertically(
                                    animationSpec = tween(durationMillis = 100),
                                    initialOffsetY = { -150 }
                                ),
                                exit = fadeOut(tween(durationMillis = 100)) + slideOutVertically(
                                    animationSpec = tween(durationMillis = 100),
                                    targetOffsetY = { -150 }
                                )
                            ) {
                                SearchOverlay(
                                    query = searchQuery,
                                    searchGateway = searchGateway,
                                    homeGateway = homeGateway,
                                    onOpenProfile = { username ->
                                        openProfile(username)
                                    },
                                    onOpenPost = { postId ->
                                        openPost(postId)
                                    },
                                    onOpenMusicTrack = { trackId ->
                                        scope.launch {
                                            musicController.playTrackById(trackId)
                                        }
                                        navigateToTab(MainTab.Music)
                                        clearSearch()
                                    },
                                    transparencyMode = transparencyMode
                                )
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    val homeTabVisible = currentTab == MainTab.Home
                    val hallTabVisible = currentTab == MainTab.Hall

                    key(homeRouteId, refreshKey(homeRouteId)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (homeTabVisible) 1f else 0f)
                        ) {
                            HomeScreen(
                                homeGateway = homeGateway,
                                accountId = accountId,
                                accountName = accountName,
                                accountUsername = accountUsername,
                                accountAvatar = accountAvatar,
                                accountChannels = effectiveAccountChannels,
                                selectedComposerChannel = selectedComposerChannel,
                                onSelectedComposerChannelChange = { channel ->
                                    selectedComposerChannelId = channel?.id
                                },
                                accountBalance = shellAccountBalance,
                                isAdmin = isAdmin,
                                initialCategory = defaultFeed,
                                requestedOpenPostId = requestedPostId,
                                onPostRequestHandled = { requestedPostId = null },
                                onOpenSidebar = { isSidebarOpen = true },
                                onOpenProfile = { username ->
                                    openProfile(username)
                                },
                                topPadding = ShellContentTopPadding,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (homeTabVisible) {
                                SearchDismissBodyOverlay(
                                    visible = isSearchExpanded,
                                    onDismiss = ::clearSearch
                                )

                                ShellHeaderContainer {
                                    ShellTopBar(
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = { searchQuery = it },
                                        searchExpanded = isSearchExpanded,
                                        onSearchFocusChanged = { isSearchFocused = it },
                                        accountName = accountName,
                                        accountAvatar = accountAvatar,
                                        accountBalance = shellAccountBalance,
                                        homeGateway = homeGateway,
                                        showMusicShortcut = currentTab != MainTab.Music && musicState.isSelected,
                                        showDownloadsShortcut = musicDownloads.isNotEmpty(),
                                        onLogoClick = {
                                            navigateToTab(MainTab.Home)
                                            clearSearch()
                                        },
                                        onAvatarClick = { isSidebarOpen = true },
                                        onBalanceClick = {
                                            navigateToTab(MainTab.Wallet)
                                            clearSearch()
                                            requestedPostId = null
                                            requestedProfilePostId = null
                                        },
                                        onMusicClick = {
                                            isHeaderMusicPlayerOpen = true
                                            isDownloadsOpen = false
                                        },
                                        onDownloadsClick = {
                                            isDownloadsOpen = true
                                            isHeaderMusicPlayerOpen = false
                                        }
                                    )

                                    AnimatedVisibility(
                                        visible = searchQuery.isNotBlank(),
                                        enter = fadeIn(tween(durationMillis = 100)) + slideInVertically(
                                            animationSpec = tween(durationMillis = 100),
                                            initialOffsetY = { -150 }
                                        ),
                                        exit = fadeOut(tween(durationMillis = 100)) + slideOutVertically(
                                            animationSpec = tween(durationMillis = 100),
                                            targetOffsetY = { -150 }
                                        )
                                    ) {
                                        SearchOverlay(
                                            query = searchQuery,
                                            searchGateway = searchGateway,
                                            homeGateway = homeGateway,
                                            onOpenProfile = { username ->
                                                openProfile(username)
                                            },
                                            onOpenPost = { postId ->
                                                openPost(postId)
                                            },
                                            onOpenMusicTrack = { trackId ->
                                                scope.launch {
                                                    musicController.playTrackById(trackId)
                                                }
                                                navigateToTab(MainTab.Music)
                                                clearSearch()
                                            },
                                            transparencyMode = transparencyMode
                                        )
                                    }
                                }
                            }
                        }
                    }

                    when (currentTab) {
                        MainTab.Home -> Unit

                        MainTab.Notifications -> key(notificationsRouteId, refreshKey(notificationsRouteId)) {
                            NotificationsScreen(
                                notificationsGateway = notificationsGateway,
                                homeGateway = homeGateway,
                                onOpenProfile = { username ->
                                    openProfile(username)
                                },
                                onOpenPost = ::openPost,
                                onOpenMessenger = {
                                    navigateToTab(MainTab.Messenger)
                                    clearSearch()
                                    requestedPostId = null
                                },
                                onUnreadCountChange = { count ->
                                    onNotificationsCountChange(count)
                                },
                                onViewed = {
                                    onNotificationsCountChange(0)
                                },
                                onOpenWallet = {
                                    navigateToTab(MainTab.Wallet)
                                    clearSearch()
                                    requestedPostId = null
                                    requestedProfilePostId = null
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        MainTab.Wallet -> key(walletRouteId, refreshKey(walletRouteId)) {
                            WalletScreen(
                                walletGateway = walletGateway,
                                homeGateway = homeGateway,
                                initialBalance = shellAccountBalance,
                                goldStatus = goldStatus,
                                goldHistory = goldHistory,
                                onBalanceChanged = { shellAccountBalance = it },
                                onOpenProfile = { username ->
                                    openProfile(username)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        MainTab.Profile -> Unit

                        MainTab.Hall -> key(hallRouteId, refreshKey(hallRouteId)) {
                            HallScreen(
                                hallGateway = hallGateway,
                                homeGateway = homeGateway,
                                onOpenProfile = { username ->
                                    openProfile(username)
                                },
                                topPadding = ShellContentTopPadding,
                                bottomPadding = ShellContentBottomPadding,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        MainTab.Panel -> key(panelRouteId, refreshKey(panelRouteId)) {
                            PlaceholderScreen(
                                title = "Панель",
                                subtitle = "Админ-панель из веба (/panel/stat) ещё не перенесена. Её лучше делать отдельным следующим блоком: статистика, пользователи, подарки и музыка."
                            )
                        }

                         MainTab.Settings -> key(settingsRouteId, refreshKey(settingsRouteId)) {
                            SettingsScreen(
                               accountName = accountName,
                               accountEmail = accountEmail,
                               accountUsername = accountUsername,
                               accountAvatar = accountAvatar,
                               homeGateway = homeGateway,
                               authGateway = authGateway,
                               profileGateway = profileGateway,
                               themeMode = themeMode,
                               defaultFeed = defaultFeed,
                               notificationsToastEnabled = notificationsToastEnabled,
                               autoVideoDownloadEnabled = autoVideoDownloadEnabled,
                               videoAutoplayEnabled = videoAutoplayEnabled,
                               transparencyMode = transparencyMode,
                               onThemeChanged = onThemeChanged,
                               onDefaultFeedChanged = onDefaultFeedChanged,
                               onNotificationsToastChanged = onNotificationsToastChanged,
                               onAutoVideoDownloadChanged = onAutoVideoDownloadChanged,
                               onVideoAutoplayChanged = onVideoAutoplayChanged,
                               onTransparencyModeChanged = onTransparencyModeChanged,
                               onAccountEmailUpdated = onAccountEmailUpdated,
                               onAccountUsernameUpdated = onAccountUsernameUpdated,
                               onLogout = onLogout,
                               onOpenElementumSettings = {
                                 elementumSettingsOpen = true
                               },
                              modifier = Modifier.fillMaxSize()
                            )
                        }

                        MainTab.Messenger -> key(messengerRouteId, refreshKey(messengerRouteId)) {
                            PlaceholderScreen(
                                title = "Мессенджер",
                                subtitle = "Следующий шаг: список чатов и загрузка диалога."
                            )
                        }

                        MainTab.Music -> key(musicRouteId, refreshKey(musicRouteId)) {
                            MusicScreen(
                                homeGateway = homeGateway,
                                topPadding = ShellContentTopPadding,
                                bottomPadding = if (musicState.isSelected) 172.dp else ShellContentBottomPadding,
                                modifier = Modifier.fillMaxSize(),
                                transparencyMode = transparencyMode
                            )
                        }
                    }

                    if (hallTabVisible) {
                        SearchDismissBodyOverlay(
                            visible = isSearchExpanded,
                            onDismiss = ::clearSearch
                        )

                        ShellHeaderContainer {
                            ShellTopBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                searchExpanded = isSearchExpanded,
                                onSearchFocusChanged = { isSearchFocused = it },
                                accountName = accountName,
                                accountAvatar = accountAvatar,
                                accountBalance = shellAccountBalance,
                                homeGateway = homeGateway,
                                showMusicShortcut = currentTab != MainTab.Music && musicState.isSelected,
                                showDownloadsShortcut = musicDownloads.isNotEmpty(),
                                onLogoClick = {
                                    navigateToTab(MainTab.Home)
                                    clearSearch()
                                },
                                onAvatarClick = { isSidebarOpen = true },
                                onBalanceClick = {
                                    navigateToTab(MainTab.Wallet)
                                    clearSearch()
                                    requestedPostId = null
                                    requestedProfilePostId = null
                                },
                                onMusicClick = {
                                    isHeaderMusicPlayerOpen = true
                                    isDownloadsOpen = false
                                },
                                onDownloadsClick = {
                                    isDownloadsOpen = true
                                    isHeaderMusicPlayerOpen = false
                                }
                            )

                            AnimatedVisibility(
                                visible = searchQuery.isNotBlank(),
                                enter = fadeIn(tween(durationMillis = 100)) + slideInVertically(
                                    animationSpec = tween(durationMillis = 100),
                                    initialOffsetY = { -150 }
                                ),
                                exit = fadeOut(tween(durationMillis = 100)) + slideOutVertically(
                                    animationSpec = tween(durationMillis = 100),
                                    targetOffsetY = { -150 }
                                )
                            ) {
                                SearchOverlay(
                                    query = searchQuery,
                                    searchGateway = searchGateway,
                                    homeGateway = homeGateway,
                                    onOpenProfile = { username ->
                                        openProfile(username)
                                    },
                                    onOpenPost = { postId ->
                                        openPost(postId)
                                    },
                                    onOpenMusicTrack = { trackId ->
                                        scope.launch {
                                            musicController.playTrackById(trackId)
                                        }
                                        navigateToTab(MainTab.Music)
                                        clearSearch()
                                    },
                                    transparencyMode = transparencyMode
                                )
                            }
                        }
                    }
                }
            }
        }

        val shouldFillHeaderTopInset = true
        if (shouldFillHeaderTopInset) {
            ShellHeaderInsetBackground(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .align(Alignment.TopCenter)
            )
        }

        UIKit.BottomNav(
            items = navItems,
            selectedIndex = selectedBottomIndex,
            onSelect = { index ->
                editingChannel = null
                val selectedTab = navTabs[index]
                val selectedOwnProfile = !ownProfileUsername.isNullOrBlank() &&
                    activeProfileUsername.equals(ownProfileUsername, ignoreCase = true)
                val shouldRefreshCurrentTab = selectedTab == selectedBottomTab &&
                    (selectedTab != MainTab.Profile || selectedOwnProfile)

                if (shouldRefreshCurrentTab) {
                    refreshCurrentPage()
                    return@BottomNav
                }

                if (selectedTab == MainTab.Profile) {
                    val ownUsername = normalizeProfileUsername(accountUsername)
                    if (!ownUsername.isNullOrBlank()) {
                        navigateToTab(MainTab.Profile)
                        openProfile(ownUsername)
                    } else {
                        Toast
                            .makeText(context, "Не удалось определить username аккаунта", Toast.LENGTH_SHORT)
                            .show()
                        navigateToTab(MainTab.Home)
                    }
                } else {
                    navigateToTab(selectedTab)
                    if (selectedTab == MainTab.Notifications && notificationsCount > 0) {
                        onNotificationsCountChange(0)
                    }
                    clearSearch()
                    requestedPostId = null
                    requestedProfilePostId = null
                    if (openedProfileUsername != null) {
                        openedProfileUsername = null
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            showShadow = currentTab != MainTab.Settings
        )

        if (currentTab == MainTab.Music) {
            MusicMiniPlayerBar(
                homeGateway = homeGateway,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (
            isHeaderMusicPlayerOpen &&
            currentTab != MainTab.Music &&
            !isSearchExpanded &&
            musicState.isSelected
        ) {
            HeaderOverlayHost(
                onDismiss = { isHeaderMusicPlayerOpen = false }
            ) { requestDismiss ->
                HeaderMiniPlayerPanel(
                    homeGateway = homeGateway,
                    musicState = musicState,
                    onClose = requestDismiss,
                    onPlayPause = { musicController.togglePlayPause() },
                    onPrevious = { musicController.previous() },
                    onNext = { musicController.next() },
                    onSeek = { musicController.seekTo(it) }
                )
            }
        }

        if (
            isDownloadsOpen &&
            !isSearchExpanded &&
            musicDownloads.isNotEmpty()
        ) {
            HeaderOverlayHost(
                onDismiss = { isDownloadsOpen = false }
            ) { requestDismiss ->
                HeaderDownloadsPanel(
                    downloads = musicDownloads,
                    homeGateway = homeGateway,
                    onClose = requestDismiss
                )
            }
        }

        MusicFullPlayerOverlay(
            homeGateway = homeGateway,
            modifier = Modifier.align(Alignment.Center),
            transparencyMode = transparencyMode
        )

        val sidebarScrimAlpha by animateFloatAsState(
            targetValue = if (isSidebarOpen) 0.5f else 0f,
            animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing),
            label = "sidebar_scrim_alpha"
        )

        if (isSidebarOpen || sidebarScrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = sidebarScrimAlpha))
                    .then(
                        if (isSidebarOpen) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isSidebarOpen = false
                                isChannelsExpanded = false
                                isAccountsExpanded = false
                            }
                        } else {
                            Modifier
                        }
                    )
            )
        }

                AnimatedVisibility(
            visible = isSidebarOpen,
            enter = slideInHorizontally(
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                initialOffsetX = { -it }
            ) + fadeIn(
                animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing),
                targetOffsetX = { -it }
            ) + fadeOut(
                animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
            ),
            modifier = Modifier.fillMaxHeight()
        ) {
            MainSidebarPanel(
                accountName = accountName,
                accountUsername = accountUsername,
                accountAvatar = accountAvatar,
                savedAccounts = savedAccounts,
                activeAccountSessionKey = activeAccountSessionKey,
                accountChannels = effectiveAccountChannels,
                homeGateway = homeGateway,
                isAccountsExpanded = isAccountsExpanded,
                onToggleAccounts = { isAccountsExpanded = !isAccountsExpanded },
                onSwitchAccount = { sessionKey ->
                    onSwitchAccount(sessionKey)
                    isSidebarOpen = false
                    isChannelsExpanded = false
                    isAccountsExpanded = false
                },
                onAddAccount = {
                    onAddAccount()
                    isSidebarOpen = false
                    isChannelsExpanded = false
                    isAccountsExpanded = false
                },
                isChannelsExpanded = isChannelsExpanded,
                onToggleChannels = { isChannelsExpanded = !isChannelsExpanded },
                onOpenProfile = { username ->
                    val normalizedTarget = normalizeProfileUsername(username)
                    if (
                        normalizedTarget != null &&
                        normalizedTarget.equals(activeProfileUsername, ignoreCase = true)
                    ) {
                        refreshRoute(routeIdForProfile(normalizedTarget))
                        isSidebarOpen = false
                        isChannelsExpanded = false
                        isAccountsExpanded = false
                    } else if (openProfile(username)) {
                        isSidebarOpen = false
                        isChannelsExpanded = false
                        isAccountsExpanded = false
                    }
                },
                onOpenSettings = {
                    editingChannel = null
                    if (currentTab == MainTab.Settings && openedProfileUsername == null) {
                        refreshRoute(settingsRouteId)
                    } else {
                        navigateToTab(MainTab.Settings)
                    }
                    openedProfileUsername = null
                    clearSearch()
                    requestedPostId = null
                    requestedProfilePostId = null
                    isSidebarOpen = false
                    isChannelsExpanded = false
                    isAccountsExpanded = false
                },
                onNavigate = { tab ->
                    editingChannel = null
                    if (tab == currentTab && openedProfileUsername == null) {
                        refreshRoute(routeIdForTab(tab))
                    } else {
                        navigateToTab(tab)
                        if (tab == MainTab.Notifications && notificationsCount > 0) {
                            onNotificationsCountChange(0)
                        }
                    }
                    openedProfileUsername = null
                    clearSearch()
                    requestedPostId = null
                    requestedProfilePostId = null
                    isSidebarOpen = false
                    isChannelsExpanded = false
                    isAccountsExpanded = false
                },
                onFeatureClick = { label ->
                    Toast
                        .makeText(context, "$label подключим следующим шагом", Toast.LENGTH_SHORT)
                        .show()
                    isSidebarOpen = false
                    isChannelsExpanded = false
                    isAccountsExpanded = false
                },
                onLogout = {
                    isSidebarOpen = false
                    isChannelsExpanded = false
                    isAccountsExpanded = false
                    onLogout()
                }
            )
        }

        // ⬇️⬇️⬇️ ВСТАВЬ ЭТОТ БЛОК ЗДЕСЬ ⬇️⬇️⬇️
        // (перед закрывающей скобкой Box, но после AnimatedVisibility)
        if (elementumSettingsOpen) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { elementumSettingsOpen = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                ElementumSettingsScreen(
                    onBack = { elementumSettingsOpen = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        // ⬆️⬆️⬆️ КОНЕЦ ВСТАВКИ ⬆️⬆️⬆️
    }
}
}
@Composable
private fun ShellHeaderContainer(
    content: @Composable ColumnScope.() -> Unit
) {
    val transparencyMode = LocalTransparencyMode.current
    val blurEnabled = shouldUseGlassBlur(transparencyMode)
    val backgroundBrush = shellHeaderSurfaceBrush(transparencyMode)
    val headerShape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        if (blurEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(42.dp)
                    .clip(headerShape)
                    .background(backgroundBrush)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush, headerShape)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
private fun SearchDismissBodyOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = ShellContentTopPadding, bottom = ShellContentBottomPadding)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    )
}

@Composable
private fun SearchKeyboardDismissEffect(
    enabled: Boolean,
    onKeyboardHidden: () -> Unit
) {
    val view = LocalView.current
    val dismissCallback by rememberUpdatedState(onKeyboardHidden)
    var keyboardVisible by remember { mutableStateOf(false) }
    var keyboardWasVisible by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            keyboardVisible = false
            keyboardWasVisible = false
        }
    }

    DisposableEffect(view, enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
                val visibleFrame = Rect()
                view.getWindowVisibleDisplayFrame(visibleFrame)
                val keyboardHeight = view.rootView.height - visibleFrame.height()
                keyboardVisible = keyboardHeight > view.rootView.height * 0.15f
            }
            view.viewTreeObserver.addOnGlobalLayoutListener(listener)
            onDispose {
                view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }
    }

    LaunchedEffect(enabled, keyboardVisible) {
        if (!enabled) return@LaunchedEffect

        if (keyboardVisible) {
            keyboardWasVisible = true
            return@LaunchedEffect
        }

        if (keyboardWasVisible) {
            dismissCallback()
        }
    }
}

@Composable
private fun ShellHeaderInsetBackground(
    modifier: Modifier = Modifier
) {
    val transparencyMode = LocalTransparencyMode.current
    val blurEnabled = shouldUseGlassBlur(transparencyMode)
    val backgroundBrush = shellHeaderSurfaceBrush(transparencyMode)

    Box(
        modifier = modifier.background(backgroundBrush)
    ) {
        if (blurEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(42.dp)
                    .background(backgroundBrush)
            )
        }
    }
}

@Composable
private fun ShellTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchExpanded: Boolean,
    onSearchFocusChanged: (Boolean) -> Unit,
    accountName: String?,
    accountAvatar: PostImageAsset?,
    accountBalance: Double?,
    homeGateway: HomeGateway,
    showMusicShortcut: Boolean,
    showDownloadsShortcut: Boolean,
    onLogoClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onBalanceClick: () -> Unit,
    onMusicClick: () -> Unit,
    onDownloadsClick: () -> Unit
) {
    val balanceText = String.format(Locale.US, "%.3f", accountBalance ?: 0.0)
    val isDarkTheme = ElementUiPalette.Body.luminance() < 0.5f
    val logoTint = if (isDarkTheme) Color.White else ElementUiPalette.TextSecondary
    val transparencyMode = LocalTransparencyMode.current
    val inputAlpha = glassSoftAlphaFor(transparencyMode)
    val chipAlpha = glassAlphaFor(transparencyMode)
    val animatedBorderColor by animateColorAsState(
        targetValue = ElementUiPalette.scaledBorder(if (searchExpanded) 1.05f else 0.82f),
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onLogoClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_element_logo),
                    contentDescription = "Element",
                    tint = logoTint,
                    modifier = Modifier.size(40.dp)
                )
            }

            UIKit.Input(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Поиск",
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { state -> onSearchFocusChanged(state.isFocused) },
                containerColor = ElementUiPalette.Block.copy(alpha = inputAlpha),
                borderWidth = 1.dp,
                borderColor = animatedBorderColor,
                shape = RoundedCornerShape(22.dp),
                minHeight = 40.dp,
                contentHorizontalPadding = 14.dp,
                contentVerticalPadding = 0.dp,
                textFontSize = 15.sp,
                textLineHeight = 18.sp,
                placeholderFontSize = 15.sp
            )

            AnimatedVisibility(
                visible = !searchExpanded,
                enter = fadeIn(tween(200)) + expandHorizontally(
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.End
                ),
                exit = fadeOut(tween(150)) + shrinkHorizontally(
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.End
                )
            ) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ElementUiPalette.Block.copy(alpha = chipAlpha))
                        .clickable(onClick = onBalanceClick)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UIKit.Eball(size = 28.dp, fontSize = 13.sp)
                        Text(
                            text = balanceText,
                            color = ElementUiPalette.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!searchExpanded && showMusicShortcut) {
                HeaderSwitchButton(
                    iconRes = R.drawable.ic_nav_music,
                    onClick = onMusicClick
                )
            }

            if (!searchExpanded && showDownloadsShortcut) {
                HeaderSwitchButton(
                    iconRes = R.drawable.ic_header_download,
                    onClick = onDownloadsClick
                )
            }

            SidebarAvatar(
                name = accountName ?: "U",
                avatar = accountAvatar,
                homeGateway = homeGateway,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onAvatarClick() }
            )
        }
    }
}

@Composable
private fun HeaderSwitchButton(
    iconRes: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = ElementUiPalette.Accent,
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
private fun HeaderOverlayHost(
    onDismiss: () -> Unit,
    content: @Composable BoxScope.(requestDismiss: () -> Unit) -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var dismissQueued by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    fun requestDismiss() {
        if (dismissQueued) return
        dismissQueued = true
        visible = false
        scope.launch {
            delay(180)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = ::requestDismiss
            )
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)) + slideInVertically(
                animationSpec = tween(220, easing = LinearOutSlowInEasing),
                initialOffsetY = { -it / 4 }
            ),
            exit = fadeOut(tween(140)) + slideOutVertically(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                targetOffsetY = { -it / 4 }
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = ShellContentTopPadding + 32.dp, start = 10.dp, end = 10.dp)
                .fillMaxWidth()
                .widthIn(max = 500.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = ElementUiPalette.BlockSoft.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                content = { content(::requestDismiss) }
            )
        }
    }
}

@Composable
private fun HeaderMiniPlayerPanel(
    homeGateway: HomeGateway,
    musicState: MusicPlayerState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Double) -> Unit
) {
    val track = musicState.selectedTrack ?: return
    val visualPlaying = isMusicTrackPlaybackActive(musicState, track.id)
    val transparencyMode = LocalTransparencyMode.current
    val blurEnabled = shouldUseGlassBlur(transparencyMode)
    val panelAlpha = glassAlphaFor(transparencyMode)
    val panelSoftAlpha = glassSoftAlphaFor(transparencyMode)
    val surfaceTop = ElementUiPalette.Block.copy(alpha = panelAlpha)
    val surfaceBottom = if (blurEnabled) {
        ElementUiPalette.Block.copy(alpha = (panelAlpha - 0.04f).coerceAtLeast(0f))
    } else {
        ElementUiPalette.BlockSoft.copy(alpha = panelSoftAlpha)
    }
    val compactInactiveColor = if (blurEnabled) {
        ElementUiPalette.TextPrimary.copy(alpha = 0.08f)
    } else {
        ElementUiPalette.BlockSoft.copy(alpha = 0.8f)
    }
    val compactTimeColor = if (blurEnabled) {
        ElementUiPalette.TextPrimary.copy(alpha = 0.56f)
    } else {
        ElementUiPalette.TextPrimary.copy(alpha = 0.7f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(30.dp),
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.16f)
            )
            .clip(RoundedCornerShape(30.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        surfaceTop,
                        surfaceBottom
                    )
                )
            )
            .border(1.dp, ElementUiPalette.scaledBorder(0.9f), RoundedCornerShape(30.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderMusicCover(
                asset = track.cover,
                fallback = track.title,
                homeGateway = homeGateway,
                size = 50.dp,
                cornerRadius = 8.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 15.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                MusicCompactMetaBlock(
                    title = track.title,
                    artist = track.artist,
                    artistFontSize = 11.sp,
                    artistLineHeight = 11.sp
                )
                MusicProgressRow(
                    currentTimeSeconds = musicState.currentTimeSeconds,
                    durationSeconds = musicState.durationSeconds,
                    onSeek = onSeek,
                    compact = true,
                    compactActiveColor = ElementUiPalette.TextPrimary,
                    compactInactiveColor = compactInactiveColor,
                    compactTimeColor = compactTimeColor,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderTransportButton(
                    iconRes = R.drawable.ic_music_previous,
                    onClick = onPrevious
                )
                HeaderPlayButton(
                    iconRes = if (visualPlaying) {
                        R.drawable.ic_music_pause
                    } else {
                        R.drawable.ic_music_play
                    },
                    onClick = onPlayPause
                )
                HeaderTransportButton(
                    iconRes = R.drawable.ic_music_next,
                    onClick = onNext
                )
                HeaderTransportButton(
                    iconRes = R.drawable.ic_header_close,
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
private fun HeaderDownloadsPanel(
    downloads: List<MusicDownloadItem>,
    homeGateway: HomeGateway,
    onClose: () -> Unit
) {
    val panelAlpha = glassAlphaFor(LocalTransparencyMode.current)
    val panelSoftAlpha = glassSoftAlphaFor(LocalTransparencyMode.current)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(30.dp),
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.16f)
            )
            .clip(RoundedCornerShape(30.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ElementUiPalette.Block.copy(alpha = panelAlpha),
                        ElementUiPalette.BlockSoft.copy(alpha = panelSoftAlpha)
                    )
                )
            )
            .border(1.dp, ElementUiPalette.scaledBorder(0.9f), RoundedCornerShape(30.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Загрузки",
                color = ElementUiPalette.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            HeaderTransportButton(
                iconRes = R.drawable.ic_header_close,
                onClick = onClose
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            downloads
                .sortedByDescending { it.downloadDate }
                .forEach { item ->
                    HeaderDownloadItemRow(
                        item = item,
                        homeGateway = homeGateway
                    )
                }
        }
    }
}

@Composable
private fun HeaderDownloadItemRow(
    item: MusicDownloadItem,
    homeGateway: HomeGateway
) {
    val context = LocalContext.current
    val progress = if (item.totalBytes > 0L) {
        (item.downloadedBytes.toFloat() / item.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(ElementUiPalette.Interaction.copy(alpha = 0.72f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HeaderMusicCover(
            asset = item.track.cover,
            fallback = item.track.title,
            homeGateway = homeGateway,
            size = 40.dp,
            cornerRadius = 8.dp
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            MusicCompactMetaBlock(
                title = item.track.title,
                artist = item.track.artist,
                artistFontSize = 11.sp,
                artistLineHeight = 11.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeaderStaticProgressBar(
                    progress = progress,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${Formatter.formatFileSize(context, item.downloadedBytes)} / ${Formatter.formatFileSize(context, item.totalBytes)}",
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
private fun HeaderMusicCover(
    asset: PostImageAsset?,
    fallback: String,
    homeGateway: HomeGateway,
    size: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp
) {
    val bitmap by rememberSidebarImageBitmap(
        asset = asset,
        homeGateway = homeGateway
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(ElementUiPalette.BlockSoft),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = fallback,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_nav_music),
                contentDescription = fallback,
                tint = ElementUiPalette.Accent,
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}

@Composable
private fun HeaderStaticProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(5.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(ElementUiPalette.BlockSoft.copy(alpha = 0.8f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(ElementUiPalette.TextPrimary)
        )
    }
}

@Composable
private fun HeaderTransportButton(
    iconRes: Int,
    rotation: Float = 0f,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(5.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = ElementUiPalette.TextPrimary,
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation)
        )
    }
}

@Composable
private fun HeaderPlayButton(
    iconRes: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .size(30.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = ElementUiPalette.TextPrimary,
            modifier = Modifier.size(21.dp)
        )
    }
}

private fun formatMiniPlayerTime(seconds: Double): String {
    val safe = seconds.coerceAtLeast(0.0).toInt()
    val minutes = safe / 60
    val secs = safe % 60
    return String.format(Locale.US, "%d:%02d", minutes, secs)
}

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        UIKit.Block(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    color = ElementUiPalette.TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = ElementUiPalette.TextLite,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MainSidebarPanel(
    accountName: String?,
    accountUsername: String?,
    accountAvatar: PostImageAsset?,
    savedAccounts: List<SavedAccountSession>,
    activeAccountSessionKey: String?,
    accountChannels: List<AuthAccountChannel>,
    homeGateway: HomeGateway,
    isAccountsExpanded: Boolean,
    onToggleAccounts: () -> Unit,
    onSwitchAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    isChannelsExpanded: Boolean,
    onToggleChannels: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onNavigate: (MainTab) -> Unit,
    onFeatureClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    val username = accountUsername.orEmpty()
    val hiddenAccounts = remember(savedAccounts, activeAccountSessionKey) {
        savedAccounts.filterNot { it.sessionKey == activeAccountSessionKey }
    }
    val channelsArrowRotation by animateFloatAsState(
        targetValue = if (isChannelsExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "channels_arrow_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.85f)
            .widthIn(max = 320.dp)
            .background(ElementUiPalette.Body)
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        SidebarUserCard(
            name = accountName ?: "Пользователь",
            username = accountUsername ?: "user",
            avatar = accountAvatar,
            homeGateway = homeGateway,
            expanded = isAccountsExpanded,
            onClick = onToggleAccounts,
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(
            visible = isAccountsExpanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 180)
            ) + fadeIn(tween(durationMillis = 180)) + slideInVertically(
                animationSpec = tween(durationMillis = 180),
                initialOffsetY = { -it / 4 }
            ),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 160)
            ) + fadeOut(tween(durationMillis = 160)) + slideOutVertically(
                animationSpec = tween(durationMillis = 160),
                targetOffsetY = { -it / 4 }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                hiddenAccounts.forEach { account ->
                    SidebarAccountSwitchItem(
                        account = account,
                        homeGateway = homeGateway,
                        onClick = { onSwitchAccount(account.sessionKey) }
                    )
                }
                SidebarAddAccountItem(onClick = onAddAccount)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        SidebarMenuGroup {
            SidebarMenuItem(
                icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                title = "Мой профиль",
                onClick = {
                    if (username.isNotBlank()) {
                        onOpenProfile(username)
                    } else {
                        onFeatureClick("Мой профиль")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SidebarMenuGroup {
            SidebarMenuItem(
                icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                title = "Кошелёк",
                onClick = { onNavigate(MainTab.Wallet) }
            )
            SidebarDivider()
            SidebarMenuItem(
                icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                title = "Уведомления",
                onClick = { onNavigate(MainTab.Notifications) }
            )
            SidebarDivider()
            SidebarMenuItem(
                icon = { Icon(Icons.Filled.Campaign, contentDescription = null) },
                title = "Мои каналы",
                onClick = onToggleChannels,
                trailing = {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(channelsArrowRotation)
                    )
                }
            )

            AnimatedVisibility(
                visible = isChannelsExpanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 100)
                ) + fadeIn(tween(durationMillis = 100)),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 100)
                ) + fadeOut(tween(durationMillis = 100))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SidebarDivider()
                    if (accountChannels.isEmpty()) {
                        Text(
                            text = "Вы еще не создали ни один канал",
                            color = ElementUiPalette.TextLite,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    } else {
                        accountChannels.forEachIndexed { index, channel ->
                            if (index > 0) SidebarDivider()
                            SidebarChannelItem(
                                channel = channel,
                                homeGateway = homeGateway,
                                onClick = {
                                    channel.username
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let(onOpenProfile)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SidebarMenuGroup {
            SidebarMenuItem(
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                title = "Настройки",
                onClick = onOpenSettings
            )
            SidebarDivider()
            SidebarMenuItem(
                icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) },
                title = "Зал славы",
                onClick = { onNavigate(MainTab.Hall) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Выйти",
            color = ElementUiPalette.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogout() }
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun SidebarUserCard(
    name: String,
    username: String,
    avatar: PostImageAsset?,
    homeGateway: HomeGateway,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "account_arrow_rotation"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ElementUiPalette.Block)
            .clickable { onClick() }
            .animateContentSize(animationSpec = tween(durationMillis = 180))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SidebarAvatar(
            name = name,
            avatar = avatar,
            homeGateway = homeGateway,
            modifier = Modifier.size(44.dp)
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = name,
                color = ElementUiPalette.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "@$username",
                color = ElementUiPalette.TextSecondary,
                fontSize = 13.sp
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = ElementUiPalette.TextSecondary,
            modifier = Modifier.rotate(arrowRotation)
        )
    }
}

@Composable
private fun SidebarMenuGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElementUiPalette.Block)
            .heightIn(min = 52.dp),
        content = content
    )
}

@Composable
private fun SidebarMenuItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() }
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides ElementUiPalette.TextPrimary) {
                icon()
            }
        }
        Text(
            text = title,
            color = ElementUiPalette.TextPrimary,
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        )
        Box(contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides ElementUiPalette.TextSecondary) {
                trailing?.invoke() ?: Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun SidebarChannelItem(
    channel: AuthAccountChannel,
    homeGateway: HomeGateway,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable { onClick() }
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SidebarAvatar(
            name = channel.name ?: channel.username ?: "C",
            avatar = channel.avatar,
            homeGateway = homeGateway,
            modifier = Modifier.size(36.dp)
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = channel.name ?: channel.username ?: "Канал",
                color = ElementUiPalette.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            channel.username?.takeIf { it.isNotBlank() }?.let { username ->
                Text(
                    text = "@$username",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ElementUiPalette.TextSecondary
        )
    }
}

@Composable
private fun SidebarAccountSwitchItem(
    account: SavedAccountSession,
    homeGateway: HomeGateway,
    onClick: () -> Unit
) {
    val shownName = account.name ?: account.username ?: "Пользователь"
    val shownUsername = account.username?.takeIf { it.isNotBlank() } ?: "user"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElementUiPalette.Block)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SidebarAvatar(
            name = shownName,
            avatar = account.avatar,
            homeGateway = homeGateway,
            modifier = Modifier.size(36.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = shownName,
                color = ElementUiPalette.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "@$shownUsername",
                color = ElementUiPalette.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ElementUiPalette.TextLite,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SidebarAddAccountItem(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElementUiPalette.Block)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ElementUiPalette.BlockSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "add-account",
                tint = ElementUiPalette.TextPrimary
            )
        }
        Text(
            text = "Добавить аккаунт",
            color = ElementUiPalette.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SidebarAvatar(
    name: String,
    avatar: PostImageAsset?,
    homeGateway: HomeGateway,
    modifier: Modifier = Modifier
) {
    val bitmap by rememberSidebarImageBitmap(
        asset = avatar,
        homeGateway = homeGateway
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF7F6EB0), Color(0xFFA19DB1))))
            .border(1.dp, ElementUiPalette.scaledBorder(0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun rememberSidebarImageBitmap(
    asset: PostImageAsset?,
    homeGateway: HomeGateway
) = produceState<ImageBitmap?>(initialValue = null, key1 = asset?.cacheKey) {
    val target = asset ?: return@produceState
    val bytes = runCatching { homeGateway.loadImageBytes(target) }.getOrNull() ?: return@produceState
    if (bytes.isEmpty()) return@produceState

    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    value = bitmap?.asImageBitmap()
}

@Composable
private fun SidebarDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ElementUiPalette.scaledBorder(0.65f))
    )
}
