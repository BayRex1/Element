package elemsocial.com.feature.profile.presentation

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import elemsocial.com.R
import elemsocial.com.core.time.formatTimeAge
import elemsocial.com.domain.model.AuthAccountChannel
import elemsocial.com.domain.model.FeedPost
import elemsocial.com.domain.model.MusicTrack
import elemsocial.com.domain.model.PostImage
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostPoll
import elemsocial.com.domain.model.Profile
import elemsocial.com.domain.model.ProfileCatalogGift
import elemsocial.com.domain.model.ProfileGift
import elemsocial.com.domain.model.ProfileRelationUser
import elemsocial.com.domain.model.UploadFilePayload
import elemsocial.com.feature.home.presentation.HomeGateway
import elemsocial.com.feature.home.presentation.PostDetailsModal
import elemsocial.com.feature.music.presentation.MusicAttachedTracks
import elemsocial.com.feature.music.presentation.MusicPickerModal
import elemsocial.com.feature.music.presentation.PostMusicTracksBlock
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.components.buttons.ElementButtonVariant
import elemsocial.com.ui.pack.components.modals.ElementContextMenuItem
import elemsocial.com.ui.pack.components.posts.ElementPostCard
import elemsocial.com.ui.pack.components.posts.ElementPostComposer
import elemsocial.com.ui.pack.components.posts.ElementEditPostModal
import elemsocial.com.ui.pack.components.posts.ElementImageViewerOverlay
import elemsocial.com.ui.pack.components.posts.ElementPostPoll
import elemsocial.com.ui.pack.components.posts.PollCreatorModal
import elemsocial.com.ui.pack.components.posts.AttachedPollPreview
import elemsocial.com.ui.pack.components.posts.ElementPostVideosBlock
import elemsocial.com.ui.pack.components.posts.buildPostGovernItems
import elemsocial.com.ui.pack.components.text.ElementLinkText
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

private const val PAGE_SIZE = 25
private const val MAX_TOTAL_FILE_SIZE = 52_428_800L
private val ProfileDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val ProfileGiftDateFormatter = DateTimeFormatter.ofPattern("HH:mm d.M.yyyy")
private val ProfileGiftRawDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private const val VerifyIconId = "VERIFY"

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

private enum class ProfileTab {
    Posts,
    Gifts,
    Wall,
    Info
}

private enum class ProfileRelationsModalKind(
    val title: String
) {
    Subscriptions("Подписки"),
    Subscribers("Подписчики")
}

private data class ProfileImageViewerState(
    val images: List<PostImage>,
    val index: Int
)

@Composable
fun ProfileScreen(
    username: String,
    profileGateway: ProfileGateway,
    homeGateway: HomeGateway,
    onBack: () -> Unit,
    requestedOpenPostId: Int? = null,
    onPostRequestHandled: () -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    isAdmin: Boolean = false,
    accountId: Int? = null,
    accountName: String? = null,
    accountUsername: String? = null,
    accountAvatar: PostImageAsset? = null,
    accountChannels: List<AuthAccountChannel> = emptyList(),
    selectedComposerChannel: AuthAccountChannel? = null,
    onSelectedComposerChannelChange: (AuthAccountChannel?) -> Unit = {},
    onOpenChannelSettings: (AuthAccountChannel) -> Unit = {},
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 90.dp,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var profile by remember(username) { mutableStateOf<Profile?>(null) }
    var posts by remember(username) { mutableStateOf<List<FeedPost>>(emptyList()) }
    var startIndex by remember(username) { mutableStateOf(0) }
    var hasMore by remember(username) { mutableStateOf(true) }
    var wallPosts by remember(username) { mutableStateOf<List<FeedPost>>(emptyList()) }
    var wallStartIndex by remember(username) { mutableStateOf(0) }
    var wallHasMore by remember(username) { mutableStateOf(true) }
    var loadingProfile by remember(username) { mutableStateOf(true) }
    var loadingPosts by remember(username) { mutableStateOf(false) }
    var loadingWallPosts by remember(username) { mutableStateOf(false) }
    var wallLoaded by remember(username) { mutableStateOf(false) }
    var giftsLoaded by remember(username) { mutableStateOf(false) }
    var loadingGifts by remember(username) { mutableStateOf(false) }
    var gifts by remember(username) { mutableStateOf<List<ProfileGift>>(emptyList()) }
    var errorText by remember(username) { mutableStateOf<String?>(null) }
    var activeTab by remember(username) { mutableStateOf(ProfileTab.Posts) }
    var composerText by remember(username) { mutableStateOf("") }
    var selectedFiles by remember(username) { mutableStateOf<List<UploadFilePayload>>(emptyList()) }
    var selectedMusicTracks by remember(username) { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var postFilesSettingsOpen by remember(username) { mutableStateOf(false) }
    var clearMetadataImage by remember(username) { mutableStateOf(false) }
    var censoringImage by remember(username) { mutableStateOf(false) }
    var isPostSending by remember(username) { mutableStateOf(false) }
    var giftVisibilityActionId by remember(username) { mutableStateOf<Int?>(null) }
    var imageViewerState by remember(username) { mutableStateOf<ProfileImageViewerState?>(null) }
    var openedPost by remember(username) { mutableStateOf<FeedPost?>(null) }
    var editingPost by remember(username) { mutableStateOf<FeedPost?>(null) }
    var openedGift by remember(username) { mutableStateOf<ProfileGift?>(null) }
    var sendGiftModalOpen by remember(username) { mutableStateOf(false) }
    var openedRelationsModal by remember(username) { mutableStateOf<ProfileRelationsModalKind?>(null) }
    var relationsUsers by remember(username) { mutableStateOf<List<ProfileRelationUser>>(emptyList()) }
    var relationsLoading by remember(username) { mutableStateOf(false) }
    var relationsErrorText by remember(username) { mutableStateOf<String?>(null) }
    var cachedSubscriptions by remember(username) { mutableStateOf<List<ProfileRelationUser>?>(null) }
    var cachedSubscribers by remember(username) { mutableStateOf<List<ProfileRelationUser>?>(null) }
    var channelPickerExpanded by remember(username) { mutableStateOf(false) }
    var musicPickerOpen by remember(username) { mutableStateOf(false) }
    var pollCreatorOpen by remember(username) { mutableStateOf(false) }
    var composerPoll by remember(username) { mutableStateOf<PostPoll?>(null) }

    val composerAuthorName = selectedComposerChannel?.name
        ?.takeIf { it.isNotBlank() }
        ?: selectedComposerChannel?.username
            ?.takeIf { it.isNotBlank() }
        ?: accountName
            ?.takeIf { it.isNotBlank() }
        ?: accountUsername
            ?.takeIf { it.isNotBlank() }
        ?: "Пользователь"
    val composerAuthorAvatar = if (selectedComposerChannel != null) {
        selectedComposerChannel.avatar
    } else {
        accountAvatar
    }

    fun extractFileName(uri: Uri): String {
        val fallback = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: "file_${System.currentTimeMillis()}"

        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) {
                        cursor.getString(index)?.takeIf { it.isNotBlank() } ?: fallback
                    } else {
                        fallback
                    }
                } ?: fallback
        }.getOrDefault(fallback)
    }

    val pickFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val attachments = uris.mapNotNull { uri ->
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull() ?: return@mapNotNull null
            if (bytes.isEmpty()) return@mapNotNull null

            val mime = context.contentResolver.getType(uri)?.takeIf { it.isNotBlank() }
                ?: "application/octet-stream"
            val name = extractFileName(uri)

            UploadFilePayload(
                name = name,
                mimeType = mime,
                bytes = bytes
            )
        }

        if (attachments.isEmpty()) {
            Toast.makeText(context, "Не удалось прочитать файлы", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        val existingSize = selectedFiles.sumOf { it.size.toLong() }
        val newSize = attachments.sumOf { it.size.toLong() }
        if (existingSize + newSize > MAX_TOTAL_FILE_SIZE) {
            Toast.makeText(context, "Общий размер файлов не должен превышать 50MB", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        selectedFiles = selectedFiles + attachments
    }

    fun loadPosts(reset: Boolean) {
        val readyProfile = profile ?: return
        if (loadingPosts) return
        if (!reset && !hasMore) return

        val type = if (readyProfile.type == "channel") 1 else 0
        val nextStart = if (reset) 0 else startIndex
        loadingPosts = true
        scope.launch {
            val result = runCatching {
                profileGateway.loadProfilePosts(readyProfile.id, type, nextStart)
            }.getOrNull()
            if (result?.isSuccess == true) {
                posts = if (reset) result.posts else (posts + result.posts).distinctBy { it.id }
                startIndex = if (reset) result.posts.size else startIndex + result.posts.size
                hasMore = result.posts.size >= PAGE_SIZE
                errorText = null
            } else {
                errorText = result?.message ?: "Не удалось загрузить посты профиля"
            }
            loadingPosts = false
        }
    }

    fun loadWallPosts(reset: Boolean) {
        val readyProfile = profile ?: return
        if (loadingWallPosts) return
        if (!reset && !wallHasMore) return

        val nextStart = if (reset) 0 else wallStartIndex
        loadingWallPosts = true
        scope.launch {
            val result = runCatching {
                profileGateway.loadProfileWallPosts(
                    username = readyProfile.username,
                    startIndex = nextStart
                )
            }.getOrNull()
            if (result?.isSuccess == true) {
                wallPosts = if (reset) result.posts else (wallPosts + result.posts).distinctBy { it.id }
                wallStartIndex = if (reset) result.posts.size else wallStartIndex + result.posts.size
                wallHasMore = result.posts.size >= PAGE_SIZE
                wallLoaded = true
                errorText = null
            } else {
                errorText = result?.message ?: "Не удалось загрузить стену"
            }
            loadingWallPosts = false
        }
    }

    fun loadGifts() {
        if (loadingGifts || giftsLoaded) return
        val readyProfile = profile ?: return
        loadingGifts = true
        scope.launch {
            val result = runCatching { profileGateway.loadProfileGifts(readyProfile.username) }.getOrNull()
            if (result?.isSuccess == true) {
                gifts = result.gifts
                giftsLoaded = true
                errorText = null
            } else {
                errorText = result?.message ?: "Не удалось загрузить подарки"
            }
            loadingGifts = false
        }
    }

    fun updatePost(postId: Int, transform: (FeedPost) -> FeedPost) {
        var updatedPost: FeedPost? = null
        posts = posts.map { post ->
            if (post.id == postId) {
                transform(post).also { updatedPost = it }
            } else {
                post
            }
        }
        wallPosts = wallPosts.map { post ->
            if (post.id == postId) {
                transform(post).also { updatedPost = it }
            } else {
                post
            }
        }
        if (openedPost?.id == postId) {
            openedPost = updatedPost ?: openedPost?.let(transform)
        }
    }

    fun removePost(postId: Int) {
        posts = posts.filterNot { it.id == postId }
        wallPosts = wallPosts.filterNot { it.id == postId }
    }

    fun setPostFeedback(resultMessage: String?, fallbackError: String) {
        errorText = resultMessage?.takeIf { it.isNotBlank() } ?: fallbackError
    }

    fun toggleLike(post: FeedPost) {
        val before = posts.firstOrNull { it.id == post.id }
            ?: wallPosts.firstOrNull { it.id == post.id }
            ?: post
        val next = if (before.liked) {
            before.copy(likes = max(0, before.likes - 1), liked = false)
        } else {
            before.copy(
                likes = before.likes + 1,
                liked = true,
                dislikes = if (before.disliked) max(0, before.dislikes - 1) else before.dislikes,
                disliked = false
            )
        }
        updatePost(post.id) { next }
        scope.launch {
            val success = runCatching { homeGateway.likePost(post.id) }.getOrDefault(false)
            if (!success) {
                updatePost(post.id) { before }
                errorText = "Не удалось обновить лайк"
            }
        }
    }

    fun toggleDislike(post: FeedPost) {
        val before = posts.firstOrNull { it.id == post.id }
            ?: wallPosts.firstOrNull { it.id == post.id }
            ?: post
        val next = if (before.disliked) {
            before.copy(dislikes = max(0, before.dislikes - 1), disliked = false)
        } else {
            before.copy(
                dislikes = before.dislikes + 1,
                disliked = true,
                likes = if (before.liked) max(0, before.likes - 1) else before.likes,
                liked = false
            )
        }
        updatePost(post.id) { next }
        scope.launch {
            val success = runCatching { homeGateway.dislikePost(post.id) }.getOrDefault(false)
            if (!success) {
                updatePost(post.id) { before }
                errorText = "Не удалось обновить дизлайк"
            }
        }
    }

    fun sendPost() {
        val text = composerText.trim()
        if ((text.isBlank() && selectedFiles.isEmpty() && selectedMusicTracks.isEmpty() && composerPoll == null) || isPostSending) return

        val readyProfile = profile ?: return
        isPostSending = true
        scope.launch {
            val result = runCatching {
                homeGateway.createPost(
                    text = text,
                    files = selectedFiles,
                    songs = selectedMusicTracks.map { it.id },
                    fromChannelId = selectedComposerChannel?.id,
                    wallUsername = if (activeTab == ProfileTab.Wall) readyProfile.username else null,
                    poll = composerPoll,
                    clearMetadataImage = clearMetadataImage,
                    censoringImage = censoringImage
                )
            }.getOrNull()
            if (result?.isSuccess == true) {
                composerText = ""
                selectedFiles = emptyList()
                selectedMusicTracks = emptyList()
                composerPoll = null
                postFilesSettingsOpen = false
                clearMetadataImage = false
                censoringImage = false
                errorText = null
                if (activeTab == ProfileTab.Wall) {
                    loadWallPosts(reset = true)
                } else {
                    loadPosts(reset = true)
                }
            } else {
                errorText = result?.message ?: "Не удалось отправить пост"
            }
            isPostSending = false
        }
    }

    fun removeAttachment(file: UploadFilePayload) {
        val index = selectedFiles.indexOfFirst {
            it.name == file.name &&
                it.mimeType == file.mimeType &&
                it.size == file.size &&
                it.bytes.contentEquals(file.bytes)
        }
        if (index !in selectedFiles.indices) return
        val next = selectedFiles.toMutableList()
        next.removeAt(index)
        selectedFiles = next
        if (selectedFiles.none { it.mimeType.startsWith("image/") }) {
            postFilesSettingsOpen = false
            clearMetadataImage = false
            censoringImage = false
        }
    }

    fun handleDeleteOrRestore(post: FeedPost) {
        scope.launch {
            val result = if (post.deleted) {
                homeGateway.restorePost(post.id)
            } else {
                homeGateway.deletePost(post.id)
            }

            if (!result.isSuccess) {
                setPostFeedback(result.message, "Не удалось обновить состояние поста")
                return@launch
            }

            errorText = null
            if (post.deleted) {
                updatePost(post.id) { it.copy(deleted = false) }
            } else {
                if (isAdmin) {
                    updatePost(post.id) { it.copy(deleted = true) }
                } else {
                    removePost(post.id)
                }
            }
        }
    }

    fun handleDeleteForever(post: FeedPost) {
        scope.launch {
            val result = homeGateway.deletePostForever(post.id)
            if (result.isSuccess) {
                errorText = null
                removePost(post.id)
            } else {
                setPostFeedback(result.message, "Не удалось удалить пост навсегда")
            }
        }
    }

    fun handleArchiveToggle(post: FeedPost) {
        scope.launch {
            val result = if (post.archived) {
                homeGateway.removePostFromArchive(post.id)
            } else {
                homeGateway.addPostToArchive(post.id)
            }

            if (result.isSuccess) {
                errorText = null
                removePost(post.id)
            } else {
                setPostFeedback(result.message, "Не удалось обновить архив поста")
            }
        }
    }

    fun handleBlockToggle(post: FeedPost) {
        val usernameValue = post.author?.username?.trim().orEmpty()
        if (usernameValue.isBlank()) {
            errorText = "Невозможно определить пользователя"
            return
        }

        scope.launch {
            val currentBlocked = posts.firstOrNull { it.id == post.id }?.author?.blocked ?: (post.author?.blocked ?: false)
            val shouldBlock = !currentBlocked
            val result = if (shouldBlock) {
                homeGateway.blockProfile(usernameValue)
            } else {
                homeGateway.unblockProfile(usernameValue)
            }

            if (result.isSuccess) {
                errorText = null
                updatePost(post.id) { current ->
                    current.copy(author = current.author?.copy(blocked = shouldBlock))
                }
            } else {
                setPostFeedback(result.message, "Не удалось обновить блокировку пользователя")
            }
        }
    }

    fun toggleGiftVisibility(gift: ProfileGift) {
        val readyProfile = profile ?: return
        if (giftVisibilityActionId == gift.id) return

        val targetHidden = !gift.hidden
        val before = gifts
        gifts = gifts.map { current ->
            if (current.id == gift.id) current.copy(hidden = targetHidden) else current
        }
        giftVisibilityActionId = gift.id

        scope.launch {
            val result = runCatching {
                profileGateway.setProfileGiftHidden(
                    username = readyProfile.username,
                    giftEntityId = gift.id,
                    hidden = targetHidden
                )
            }.getOrNull()
            if (result?.isSuccess != true) {
                gifts = before
                errorText = result?.message ?: "Не удалось обновить видимость подарка"
            }
            giftVisibilityActionId = null
        }
    }

    fun toggleSubscribe() {
        val before = profile ?: return
        val nextSubscribed = !before.subscribed
        val subscribersDelta = if (nextSubscribed) 1 else -1
        val next = before.copy(
            subscribed = nextSubscribed,
            stats = before.stats.copy(
                subscribers = max(0, before.stats.subscribers + subscribersDelta)
            )
        )
        profile = next
        scope.launch {
            val result = runCatching { profileGateway.subscribeProfile(before.username) }.getOrNull()
            if (result?.isSuccess != true) {
                profile = before
                errorText = result?.message ?: "Не удалось обновить подписку"
            }
        }
    }

    fun openRelations(kind: ProfileRelationsModalKind) {
        val readyProfile = profile ?: return
        val total = when (kind) {
            ProfileRelationsModalKind.Subscriptions -> readyProfile.stats.subscriptions
            ProfileRelationsModalKind.Subscribers -> readyProfile.stats.subscribers
        }
        if (total <= 0) return

        openedRelationsModal = kind
        relationsErrorText = null

        val cachedUsers = when (kind) {
            ProfileRelationsModalKind.Subscriptions -> cachedSubscriptions
            ProfileRelationsModalKind.Subscribers -> cachedSubscribers
        }
        if (cachedUsers != null) {
            relationsUsers = cachedUsers
            relationsLoading = false
            return
        }

        relationsUsers = emptyList()
        relationsLoading = true
        scope.launch {
            val result = runCatching {
                when (kind) {
                    ProfileRelationsModalKind.Subscriptions -> {
                        profileGateway.loadProfileSubscriptions(
                            username = readyProfile.username,
                            startIndex = 0
                        )
                    }

                    ProfileRelationsModalKind.Subscribers -> {
                        profileGateway.loadProfileSubscribers(
                            username = readyProfile.username,
                            startIndex = 0
                        )
                    }
                }
            }.getOrNull()

            if (result?.isSuccess == true) {
                relationsUsers = result.users
                relationsErrorText = null
                when (kind) {
                    ProfileRelationsModalKind.Subscriptions -> cachedSubscriptions = result.users
                    ProfileRelationsModalKind.Subscribers -> cachedSubscribers = result.users
                }
            } else {
                relationsErrorText = result?.message ?: "Не удалось загрузить список"
            }
            relationsLoading = false
        }
    }

    fun unblockProfile() {
        val before = profile ?: return
        profile = before.copy(blocked = false)
        scope.launch {
            val result = runCatching { homeGateway.unblockProfile(before.username) }.getOrNull()
            if (result?.isSuccess != true) {
                profile = before
                errorText = result?.message ?: "Не удалось разблокировать профиль"
            }
        }
    }

    fun blockProfile() {
        val before = profile ?: return
        if (before.blocked) return
        profile = before.copy(blocked = true)
        scope.launch {
            val result = runCatching { homeGateway.blockProfile(before.username) }.getOrNull()
            if (result?.isSuccess != true) {
                profile = before
                errorText = result?.message ?: "Не удалось заблокировать профиль"
            } else {
                errorText = null
            }
        }
    }

    LaunchedEffect(username) {
        loadingProfile = true
        val targetUsername = normalizeProfileUsername(username)
        if (targetUsername.isNullOrBlank()) {
            errorText = "Профиль не найден"
            profile = null
            loadingProfile = false
            return@LaunchedEffect
        }

        val result = runCatching { profileGateway.loadProfile(targetUsername) }.getOrNull()
        if (result?.isSuccess == true) {
            profile = result.profile
            errorText = null
            loadPosts(reset = true)
        } else {
            errorText = result?.message ?: "Профиль не найден"
            profile = null
        }
        loadingProfile = false
    }

    LaunchedEffect(activeTab, profile?.username) {
        if (profile == null) return@LaunchedEffect
        if (activeTab == ProfileTab.Wall && !wallLoaded && !loadingWallPosts) {
            loadWallPosts(reset = true)
        }
        if (activeTab == ProfileTab.Gifts && !giftsLoaded && !loadingGifts) {
            loadGifts()
        }
    }

    fun openImageViewer(images: List<PostImage>, index: Int) {
        if (images.isEmpty()) return
        imageViewerState = ProfileImageViewerState(
            images = images,
            index = index.coerceIn(0, images.lastIndex)
        )
    }

    fun openPostById(postId: Int) {
        val localPost = posts.firstOrNull { it.id == postId }
            ?: wallPosts.firstOrNull { it.id == postId }
        if (localPost != null) {
            openedPost = localPost
            onPostRequestHandled()
            return
        }

        scope.launch {
            val result = runCatching { homeGateway.loadPost(postId) }.getOrNull()
            val loaded = result?.post
            if (loaded != null) {
                openedPost = loaded
                errorText = null
            } else {
                errorText = result?.message ?: "Не удалось открыть пост"
            }
            onPostRequestHandled()
        }
    }

    LaunchedEffect(requestedOpenPostId) {
        val target = requestedOpenPostId ?: return@LaunchedEffect
        openPostById(target)
    }

    LaunchedEffect(activeTab, profile?.id) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            lastVisible to total
        }.collect { (lastVisible, total) ->
            if (lastVisible < 0 || total <= 0) return@collect
            val nearEnd = lastVisible >= (total - 3).coerceAtLeast(0)
            if (!nearEnd) return@collect

            when (activeTab) {
                ProfileTab.Posts -> {
                    if (!loadingPosts && hasMore && posts.isNotEmpty()) {
                        loadPosts(reset = false)
                    }
                }

                ProfileTab.Wall -> {
                    if (!loadingWallPosts && wallHasMore && wallPosts.isNotEmpty()) {
                        loadWallPosts(reset = false)
                    }
                }

                else -> Unit
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ElementUiPalette.Body)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            state = listState,
            contentPadding = PaddingValues(bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (topPadding > 0.dp) {
                item { Spacer(modifier = Modifier.height(topPadding)) }
            }

            item {
                ProfileCenteredItem {
                    when {
                        loadingProfile -> {
                            ProfileHeaderSkeleton()
                        }

                        profile != null -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ProfileHeader(
                                    profile = profile!!,
                                    homeGateway = homeGateway,
                                    accountId = accountId,
                                    onSubscribeClick = ::toggleSubscribe,
                                    onSubscriptionsClick = {
                                        openRelations(ProfileRelationsModalKind.Subscriptions)
                                    },
                                    onSubscribersClick = {
                                        openRelations(ProfileRelationsModalKind.Subscribers)
                                    },
                                    onUnblockClick = ::unblockProfile,
                                    governItems = buildProfileGovernItems(
                                        profile = profile!!,
                                        onSendGift = { sendGiftModalOpen = true },
                                        onBlock = ::blockProfile,
                                        onEditChannel = {
                                            val readyProfile = profile!!
                                            val existingChannel = accountChannels.firstOrNull { it.id == readyProfile.id }
                                            val targetChannel = existingChannel ?: AuthAccountChannel(
                                                id = readyProfile.id,
                                                name = readyProfile.name,
                                                username = readyProfile.username,
                                                avatar = readyProfile.avatar
                                            )
                                            onOpenChannelSettings(targetChannel)
                                        }
                                    )
                                )
                            }
                        }

                        else -> {
                            ErrorBlock(errorText ?: "Профиль не найден")
                        }
                    }
                }
            }

            if (loadingProfile) {
                item {
                    ProfileCenteredItem {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(3) { index ->
                                UIKit.SkeletonPost(
                                    modifier = Modifier.fillMaxWidth(),
                                    mediaHeight = if (index == 0) 206.dp else 174.dp,
                                    showMedia = index != 1
                                )
                            }
                        }
                    }
                }
            }

            val readyProfile = profile
            val hasImageAttachments = selectedFiles.any { it.mimeType.startsWith("image/") }
            if (readyProfile != null) {
                item {
                    ProfileCenteredItem {
                        ProfileTabs(
                            profile = readyProfile,
                            isAuthorized = accountId != null,
                            selected = activeTab,
                            onSelect = { activeTab = it }
                        )
                    }
                }

                when (activeTab) {
                    ProfileTab.Posts -> {
                        if (readyProfile.myProfile || accountId == readyProfile.id) {
                            item {
                                ProfileCenteredItem {
                                    ElementPostComposer(
                                        text = composerText,
                                        isSending = isPostSending,
                                        avatar = {
                                            ProfileComposerChannelPicker(
                                                expanded = channelPickerExpanded,
                                                onExpandedChange = { channelPickerExpanded = it },
                                                composerAuthorName = composerAuthorName,
                                                composerAuthorAvatar = composerAuthorAvatar,
                                                accountName = accountName,
                                                accountUsername = accountUsername,
                                                accountAvatar = accountAvatar,
                                                accountChannels = accountChannels,
                                                selectedComposerChannel = selectedComposerChannel,
                                                homeGateway = homeGateway,
                                                onSelectedComposerChannelChange = onSelectedComposerChannelChange
                                            )
                                        },
                                        onTextChange = { composerText = it },
                                        onEmojiClick = {
                                            composerText = if (composerText.isBlank()) {
                                                "🙂"
                                            } else {
                                                "${composerText.trimEnd()} 🙂"
                                            }
                                        },
                                        onFileClick = { pickFilesLauncher.launch("*/*") },
                                        onMusicClick = { musicPickerOpen = true },
                                        onPollClick = { pollCreatorOpen = true },
                                        onSend = ::sendPost,
                                        placeholder = "Оставить запись...",
                                        attachments = selectedFiles,
                                        onRemoveAttachment = ::removeAttachment,
                                        hasExtraContent = selectedMusicTracks.isNotEmpty() || composerPoll != null,
                                        showEmojiButton = false,
                                        showImageSettingsButton = hasImageAttachments,
                                        showMusicButton = true,
                                        showPollButton = true,
                                        imageSettingsExpanded = postFilesSettingsOpen,
                                        onToggleImageSettings = {
                                            if (hasImageAttachments) {
                                                postFilesSettingsOpen = !postFilesSettingsOpen
                                            }
                                        },
                                        clearMetadataImage = clearMetadataImage,
                                        onClearMetadataImageChange = { clearMetadataImage = it },
                                        censoringImage = censoringImage,
                                        onCensoringImageChange = { censoringImage = it }
                                    )
                                }
                                if (selectedMusicTracks.isNotEmpty()) {
                                    ProfileCenteredItem {
                                        MusicAttachedTracks(
                                            homeGateway = homeGateway,
                                            tracks = selectedMusicTracks,
                                            onRemove = { track ->
                                                selectedMusicTracks = selectedMusicTracks.filterNot { it.id == track.id }
                                            }
                                        )
                                    }
                                }
                                if (composerPoll != null) {
                                    ProfileCenteredItem {
                                        AttachedPollPreview(
                                            poll = composerPoll!!,
                                            onRemove = { composerPoll = null }
                                        )
                                    }
                                }
                            }
                        }

                        if (!errorText.isNullOrBlank()) {
                            item {
                                ProfileCenteredItem {
                                    ErrorBlock(errorText.orEmpty())
                                }
                            }
                        }

                        when {
                            loadingPosts && posts.isEmpty() -> {
                                item {
                                    ProfileCenteredItem {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            repeat(3) { index ->
                                                UIKit.SkeletonPost(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    mediaHeight = if (index == 0) 206.dp else 174.dp,
                                                    showMedia = index != 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            posts.isEmpty() -> {
                                item {
                                    ProfileCenteredItem {
                                        EmptyProfilePosts()
                                    }
                                }
                            }

                            else -> {
                                items(posts, key = { it.id }) { post ->
                                    ProfileCenteredItem {
                                        ProfilePostCard(
                                            post = post,
                                            homeGateway = homeGateway,
                                            isAdmin = isAdmin,
                                            onLike = { toggleLike(post) },
                                            onDislike = { toggleDislike(post) },
                                            onOpenPost = {
                                                openedPost = posts.firstOrNull { it.id == post.id } ?: post
                                            },
                                            onOpenProfile = onOpenProfile,
                                            onReportPost = {
                                                Toast.makeText(context, "Жалобы подключим следующим шагом", Toast.LENGTH_SHORT).show()
                                            },
                                            onEditPost = { editingPost = post },
                                            onDeleteOrRestorePost = { handleDeleteOrRestore(post) },
                                            onDeletePostForever = { handleDeleteForever(post) },
                                            onArchiveToggle = { handleArchiveToggle(post) },
                                            onBlockToggle = { handleBlockToggle(post) },
                                            onOpenImageViewer = ::openImageViewer
                                        )
                                    }
                                }
                            }
                        }

                        if (loadingPosts && posts.isNotEmpty()) {
                            item {
                                ProfileCenteredItem {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        UIKit.Loader(size = 22)
                                    }
                                }
                            }
                        }
                    }

                    ProfileTab.Gifts -> {
                        if (!errorText.isNullOrBlank()) {
                            item {
                                ProfileCenteredItem {
                                    ErrorBlock(errorText.orEmpty())
                                }
                            }
                        }

                        when {
                            loadingGifts && gifts.isEmpty() -> {
                                item {
                                    ProfileCenteredItem {
                                        ProfileGiftsSkeletonGrid()
                                    }
                                }
                            }

                            gifts.isEmpty() -> {
                                item {
                                    ProfileCenteredItem {
                                        EmptyProfileGifts()
                                    }
                                }
                            }

                            else -> {
                                item {
                                    ProfileCenteredItem {
                                        ProfileGiftsGrid(
                                            gifts = gifts,
                                            homeGateway = homeGateway,
                                            canToggleVisibility = readyProfile.myProfile,
                                            giftVisibilityActionId = giftVisibilityActionId,
                                            onToggleVisibility = ::toggleGiftVisibility,
                                            onOpenProfile = onOpenProfile,
                                            onOpenGift = { openedGift = it }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ProfileTab.Wall -> {
                        if (accountId != null) {
                            item {
                                ProfileCenteredItem {
                                    ElementPostComposer(
                                        text = composerText,
                                        isSending = isPostSending,
                                        avatar = {
                                            ProfileComposerChannelPicker(
                                                expanded = channelPickerExpanded,
                                                onExpandedChange = { channelPickerExpanded = it },
                                                composerAuthorName = composerAuthorName,
                                                composerAuthorAvatar = composerAuthorAvatar,
                                                accountName = accountName,
                                                accountUsername = accountUsername,
                                                accountAvatar = accountAvatar,
                                                accountChannels = accountChannels,
                                                selectedComposerChannel = selectedComposerChannel,
                                                homeGateway = homeGateway,
                                                onSelectedComposerChannelChange = onSelectedComposerChannelChange
                                            )
                                        },
                                        onTextChange = { composerText = it },
                                        onEmojiClick = {
                                            composerText = if (composerText.isBlank()) {
                                                "🙂"
                                            } else {
                                                "${composerText.trimEnd()} 🙂"
                                            }
                                        },
                                        onFileClick = { pickFilesLauncher.launch("*/*") },
                                        onMusicClick = { musicPickerOpen = true },
                                        onPollClick = { pollCreatorOpen = true },
                                        onSend = ::sendPost,
                                        attachments = selectedFiles,
                                        onRemoveAttachment = ::removeAttachment,
                                        hasExtraContent = selectedMusicTracks.isNotEmpty() || composerPoll != null,
                                        showEmojiButton = false,
                                        showImageSettingsButton = hasImageAttachments,
                                        showMusicButton = true,
                                        showPollButton = true,
                                        imageSettingsExpanded = postFilesSettingsOpen,
                                        onToggleImageSettings = {
                                            if (hasImageAttachments) {
                                                postFilesSettingsOpen = !postFilesSettingsOpen
                                            }
                                        },
                                        clearMetadataImage = clearMetadataImage,
                                        onClearMetadataImageChange = { clearMetadataImage = it },
                                        censoringImage = censoringImage,
                                        onCensoringImageChange = { censoringImage = it }
                                    )
                                }
                                if (selectedMusicTracks.isNotEmpty()) {
                                    ProfileCenteredItem {
                                        MusicAttachedTracks(
                                            homeGateway = homeGateway,
                                            tracks = selectedMusicTracks,
                                            onRemove = { track ->
                                                selectedMusicTracks = selectedMusicTracks.filterNot { it.id == track.id }
                                            }
                                        )
                                    }
                                }
                                if (composerPoll != null) {
                                    ProfileCenteredItem {
                                        AttachedPollPreview(
                                            poll = composerPoll!!,
                                            onRemove = { composerPoll = null }
                                        )
                                    }
                                }
                            }
                        }

                        if (!errorText.isNullOrBlank()) {
                            item {
                                ProfileCenteredItem {
                                    ErrorBlock(errorText.orEmpty())
                                }
                            }
                        }

                        when {
                            loadingWallPosts && wallPosts.isEmpty() -> {
                                item {
                                    ProfileCenteredItem {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            repeat(3) { index ->
                                                UIKit.SkeletonPost(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    mediaHeight = if (index == 0) 206.dp else 174.dp,
                                                    showMedia = index != 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            wallPosts.isEmpty() -> {
                                item {
                                    ProfileCenteredItem {
                                        EmptyWallPosts()
                                    }
                                }
                            }

                            else -> {
                                items(wallPosts, key = { it.id }) { post ->
                                    ProfileCenteredItem {
                                        ProfilePostCard(
                                            post = post,
                                            homeGateway = homeGateway,
                                            isAdmin = isAdmin,
                                            onLike = { toggleLike(post) },
                                            onDislike = { toggleDislike(post) },
                                            onOpenPost = {
                                                openedPost = wallPosts.firstOrNull { it.id == post.id } ?: post
                                            },
                                            onOpenProfile = onOpenProfile,
                                            onReportPost = {
                                                Toast.makeText(context, "Жалобы подключим следующим шагом", Toast.LENGTH_SHORT).show()
                                            },
                                            onEditPost = { editingPost = post },
                                            onDeleteOrRestorePost = { handleDeleteOrRestore(post) },
                                            onDeletePostForever = { handleDeleteForever(post) },
                                            onArchiveToggle = { handleArchiveToggle(post) },
                                            onBlockToggle = { handleBlockToggle(post) },
                                            onOpenImageViewer = ::openImageViewer
                                        )
                                    }
                                }
                            }
                        }

                        if (loadingWallPosts && wallPosts.isNotEmpty()) {
                            item {
                                ProfileCenteredItem {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        UIKit.Loader(size = 22)
                                    }
                                }
                            }
                        }
                    }

                    ProfileTab.Info -> {
                        item { ProfileCenteredItem { ProfileInfoBlock(readyProfile) } }
                    }
                }
            }
        }

        if (musicPickerOpen) {
            MusicPickerModal(
                homeGateway = homeGateway,
                selectedTracks = selectedMusicTracks,
                onSelectionChange = { selectedMusicTracks = it },
                onClose = { musicPickerOpen = false }
            )
        }

        if (pollCreatorOpen) {
            PollCreatorModal(
                onClose = { pollCreatorOpen = false },
                onSave = {
                    composerPoll = it
                    pollCreatorOpen = false
                }
            )
        }

        openedRelationsModal?.let { kind ->
            ProfileRelationsModal(
                kind = kind,
                users = relationsUsers,
                loading = relationsLoading,
                errorText = relationsErrorText,
                homeGateway = homeGateway,
                onClose = { openedRelationsModal = null },
                onOpenProfile = { targetUsername ->
                    openedRelationsModal = null
                    onOpenProfile(targetUsername)
                }
            )
        }

        imageViewerState?.let { state ->
            ElementImageViewerOverlay(
                images = state.images,
                initialIndex = state.index,
                onDismiss = { imageViewerState = null },
                loadImageBytes = { asset -> homeGateway.loadImageBytes(asset) }
            )
        }

        editingPost?.let { post ->
            ElementEditPostModal(
                post = post,
                onClose = { editingPost = null },
                onSaveRequest = { text ->
                    homeGateway.editPost(post.id, text)
                },
                onSaved = { updated ->
                    updatePost(updated.id) { current ->
                        current.copy(
                            text = updated.text,
                            editedAt = updated.editedAt
                        )
                    }
                }
            )
        }

        openedGift?.let { gift ->
            ProfileGiftDetailsModal(
                gift = gift,
                receiverProfile = profile,
                homeGateway = homeGateway,
                onOpenProfile = onOpenProfile,
                onClose = { openedGift = null }
            )
        }

        if (sendGiftModalOpen && profile != null) {
            ProfileSendGiftModal(
                profile = profile!!,
                profileGateway = profileGateway,
                homeGateway = homeGateway,
                onClose = { sendGiftModalOpen = false },
                onSent = {
                    errorText = null
                    Toast.makeText(context, "Подарок отправлен", Toast.LENGTH_SHORT).show()
                    sendGiftModalOpen = false
                    if (activeTab == ProfileTab.Gifts) {
                        giftsLoaded = false
                        loadGifts()
                    }
                },
                onError = { message ->
                    errorText = message
                }
            )
        }

        openedPost?.let { post ->
            PostDetailsModal(
                postId = post.id,
                initialPost = post,
                accountId = accountId,
                accountName = profile?.name ?: profile?.username,
                accountAvatar = accountAvatar ?: profile?.avatar,
                isAdmin = isAdmin,
                homeGateway = homeGateway,
                onClose = { openedPost = null },
                onOpenProfile = onOpenProfile,
                onCommentCountChanged = { postId, commentsCount ->
                    updatePost(postId) { current -> current.copy(comments = commentsCount) }
                },
                onPostMutated = { mutated -> updatePost(mutated.id) { mutated } },
                onPostRemoved = { postId ->
                    removePost(postId)
                    if (openedPost?.id == postId) {
                        openedPost = null
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileCenteredItem(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.widthIn(max = 600.dp)) {
            content()
        }
    }
}

@Composable
private fun ProfileHeaderSkeleton() {
    UIKit.Block {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(134.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElementUiPalette.BlockSoft)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 84.dp)
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(ElementUiPalette.BlockSoft)
                )
            }

            ProfileSkeletonLine(
                widthFraction = 0.32f,
                height = 18.dp
            )
            Spacer(modifier = Modifier.height(6.dp))
            ProfileSkeletonLine(
                widthFraction = 0.24f,
                height = 13.dp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(ElementUiPalette.BlockSoft)
                )
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ElementUiPalette.BlockSoft)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        ProfileSkeletonLine(
                            widthFraction = 0.48f,
                            height = 11.dp
                        )
                        ProfileSkeletonLine(
                            widthFraction = 0.7f,
                            height = 9.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            ProfileSkeletonLine(
                widthFraction = 0.86f,
                height = 11.dp
            )
            Spacer(modifier = Modifier.height(6.dp))
            ProfileSkeletonLine(
                widthFraction = 0.62f,
                height = 11.dp
            )
        }
    }
}

@Composable
private fun ProfileSkeletonLine(
    widthFraction: Float,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(ElementUiPalette.BlockSoft)
    )
}

@Composable
private fun ProfileHeader(
    profile: Profile,
    homeGateway: HomeGateway,
    accountId: Int?,
    onSubscribeClick: () -> Unit,
    onSubscriptionsClick: () -> Unit,
    onSubscribersClick: () -> Unit,
    onUnblockClick: () -> Unit,
    governItems: List<ElementContextMenuItem>
) {
    val isAuthorized = accountId != null
    val isOwnProfile = profile.myProfile || accountId == profile.id
    val hasCover = profile.cover != null

    UIKit.Block {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (hasCover) 186.dp else 104.dp)
            ) {
                ProfileCover(
                    asset = profile.cover,
                    homeGateway = homeGateway,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (hasCover) 134.dp else 52.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = if (hasCover) 84.dp else 8.dp)
                ) {
                    ProfileAvatar(
                        name = profile.name ?: profile.username,
                        asset = profile.avatar,
                        homeGateway = homeGateway,
                        size = 90.dp
                    )
                    if (profile.online) {
                        OnlineMarker(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 5.dp, bottom = 5.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    text = profile.name ?: "Пользователь",
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ProfileBadges(
                    icons = profile.icons,
                    size = 18.dp,
                    modifier = Modifier.padding(start = 3.dp)
                )
            }

            Text(
                text = "@${profile.username}",
                color = ElementUiPalette.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            if (isAuthorized) {
                ProfileHeaderButtons(
                    profile = profile,
                    isOwnProfile = isOwnProfile,
                    onSubscribeClick = onSubscribeClick,
                    onUnblockClick = onUnblockClick,
                    governItems = governItems
                )
            }

            if (isAuthorized && !profile.online && profile.type != "channel") {
                Text(
                    text = profile.lastOnline
                        ?.takeIf { it.isNotBlank() }
                        ?.let { "был(а) в сети ${formatTimeAge(it)}" }
                        ?: "не в сети",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = if (isAuthorized) 2.dp else 0.dp)
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            ProfileStatsRow(
                profile = profile,
                onSubscriptionsClick = onSubscriptionsClick,
                onSubscribersClick = onSubscribersClick
            )

            if (!profile.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                ProfileDescription(text = profile.description.orEmpty())
            }

            if (profile.links.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                ProfileLinks(profile = profile)
            }
        }
    }
}

@Composable
private fun ProfileCover(
    asset: PostImageAsset?,
    homeGateway: HomeGateway,
    modifier: Modifier = Modifier
) {
    val bitmap by rememberImageBitmap(asset = asset, homeGateway = homeGateway)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (asset == null) Color.Transparent else ElementUiPalette.BlockSoft)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun ProfileVerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 17.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_profile_verify),
        contentDescription = "Верифицированный профиль",
        modifier = modifier.size(size)
    )
}

@Composable
private fun ProfileGoldBadge(
    modifier: Modifier = Modifier,
    size: Dp = 17.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_profile_gold),
        contentDescription = "Подписка Gold",
        modifier = modifier.size(size)
    )
}

@Composable
private fun ProfileBadges(
    icons: List<String>?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icons.hasVerifyBadge()) {
            ProfileVerifiedBadge(size = size)
        }
        if (icons.hasGoldBadge()) {
            ProfileGoldBadge(size = size)
        }
    }
}

@Composable
private fun ProfileHeaderButtons(
    profile: Profile,
    isOwnProfile: Boolean,
    onSubscribeClick: () -> Unit,
    onUnblockClick: () -> Unit,
    governItems: List<ElementContextMenuItem>
) {
    val showSubscribe = if (profile.type == "user") !isOwnProfile else true
    var governOpen by remember(profile.id) { mutableStateOf(false) }
    var governAnchor by remember(profile.id) { mutableStateOf<IntRect?>(null) }

    Box {
        Row(
            modifier = Modifier.height(42.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (profile.blocked) {
                ProfileHeaderTextButton(
                    title = "Разблокировать",
                    accent = false,
                    onClick = onUnblockClick
                )
            } else {
                if (showSubscribe) {
                    ProfileHeaderTextButton(
                        title = if (profile.subscribed) "Отписаться" else "Подписаться",
                        accent = !profile.subscribed,
                        onClick = onSubscribeClick
                    )
                }

                ProfileHeaderIconButton(
                    painter = painterResource(id = R.drawable.ic_element_dots),
                    onClick = { governOpen = true },
                    onAnchorChanged = { governAnchor = it }
                )
            }
        }

        UIKit.ContextMenu(
            expanded = governOpen && governItems.isNotEmpty(),
            anchorBounds = governAnchor,
            items = governItems,
            onDismissRequest = { governOpen = false },
            minWidth = 220.dp
        )
    }
}

@Composable
private fun ProfileHeaderTextButton(
    title: String,
    accent: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(42.dp)
            .widthIn(min = 126.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (accent) ElementUiPalette.Accent else ElementUiPalette.BlockSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (accent) Color.White else ElementUiPalette.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ProfileHeaderIconButton(
    painter: Painter,
    onClick: () -> Unit,
    onAnchorChanged: ((IntRect) -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .then(
                if (onAnchorChanged != null) {
                    Modifier.onGloballyPositioned { coordinates ->
                        val rect = coordinates.boundsInWindow()
                        onAnchorChanged(
                            IntRect(
                                left = rect.left.toInt(),
                                top = rect.top.toInt(),
                                right = rect.right.toInt(),
                                bottom = rect.bottom.toInt()
                            )
                        )
                    }
                } else {
                    Modifier
                }
            )
            .clip(CircleShape)
            .background(ElementUiPalette.BlockSoft)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = ElementUiPalette.InteractionText,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun ProfileStatsRow(
    profile: Profile,
    onSubscriptionsClick: () -> Unit,
    onSubscribersClick: () -> Unit
) {
    val items: List<Triple<String, String, (() -> Unit)?>> = if (profile.type == "channel") {
        listOf(
            Triple(profile.stats.subscribers.toString(), "подписчиков", onSubscribersClick),
            Triple(profile.stats.posts.toString(), "постов", null)
        )
    } else {
        listOf(
            Triple(profile.stats.subscriptions.toString(), "подписок", onSubscriptionsClick),
            Triple(profile.stats.subscribers.toString(), "подписчиков", onSubscribersClick),
            Triple(profile.stats.posts.toString(), "постов", null)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEach { (value, title, onClick) ->
            ProfileStatCell(
                value = value,
                title = title,
                onClick = onClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProfileStatCell(
    value: String,
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ElementUiPalette.BlockSoft)
            .padding(vertical = 2.dp)
            .let {
                if (onClick != null) {
                    it.clickable(onClick = onClick)
                } else {
                    it
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            color = ElementUiPalette.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp
        )
        Text(
            text = title,
            color = ElementUiPalette.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offset(y = (-2).dp)
        )
    }
}

@Composable
private fun ProfileDescription(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.BlockSoft)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "описание",
            color = ElementUiPalette.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        ElementLinkText(
            text = text,
            style = TextStyle(
                color = ElementUiPalette.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun ProfileLinks(profile: Profile) {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        profile.links.forEach { link ->
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElementUiPalette.BlockSoft.copy(alpha = 0.94f))
                    .clickable {
                        val target = normalizeProfileLinkTarget(link.link)
                        runCatching { uriHandler.openUri(target) }
                    }
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                ProfileLinkIcon(link = link.link)
                Text(
                    text = link.title,
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun profileComposerChannelName(channel: AuthAccountChannel): String {
    val channelId = channel.id
    return channel.name
        ?.takeIf { it.isNotBlank() }
        ?: channel.username
            ?.takeIf { it.isNotBlank() }
        ?: if (channelId != null) "Канал #$channelId" else "Канал"
}

@Composable
private fun ProfileComposerChannelPicker(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    composerAuthorName: String,
    composerAuthorAvatar: PostImageAsset?,
    accountName: String?,
    accountUsername: String?,
    accountAvatar: PostImageAsset?,
    accountChannels: List<AuthAccountChannel>,
    selectedComposerChannel: AuthAccountChannel?,
    homeGateway: HomeGateway,
    onSelectedComposerChannelChange: (AuthAccountChannel?) -> Unit
) {
    val canSelectChannel = accountChannels.isNotEmpty()
    val personalName = accountName ?: accountUsername ?: "Пользователь"

    Box {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .border(1.dp, ElementUiPalette.scaledBorder(0.95f), CircleShape)
                .clickable(enabled = canSelectChannel) {
                    onExpandedChange(true)
                }
        ) {
            key(
                selectedComposerChannel?.id,
                composerAuthorAvatar?.cacheKey,
                composerAuthorName
            ) {
                ProfileAvatar(
                    name = composerAuthorName,
                    asset = composerAuthorAvatar,
                    homeGateway = homeGateway,
                    size = 30.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        UIKit.DropdownMenu(
            expanded = expanded && canSelectChannel,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.widthIn(min = 270.dp, max = 300.dp)
        ) {
            Text(
                text = "Написать от имени...",
                color = ElementUiPalette.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )

            ProfileComposerChannelMenuItem(
                name = personalName,
                subtitle = "Личный профиль",
                selected = selectedComposerChannel == null,
                avatar = {
                    ProfileAvatar(
                        name = personalName,
                        asset = accountAvatar,
                        homeGateway = homeGateway,
                        size = 30.dp
                    )
                },
                onClick = {
                    onSelectedComposerChannelChange(null)
                    onExpandedChange(false)
                }
            )

            accountChannels.forEach { channel ->
                val channelId = channel.id ?: return@forEach
                val channelName = profileComposerChannelName(channel)
                ProfileComposerChannelMenuItem(
                    name = channelName,
                    subtitle = "Канал",
                    selected = selectedComposerChannel?.id == channelId,
                    avatar = {
                        ProfileAvatar(
                            name = channelName,
                            asset = channel.avatar,
                            homeGateway = homeGateway,
                            size = 30.dp
                        )
                    },
                    onClick = {
                        onSelectedComposerChannelChange(channel)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileComposerChannelMenuItem(
    name: String,
    subtitle: String,
    selected: Boolean,
    avatar: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) ElementUiPalette.Accent.copy(alpha = 0.12f) else Color.Transparent
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) ElementUiPalette.Accent.copy(alpha = 0.38f) else Color.Transparent,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        avatar()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = name,
                color = ElementUiPalette.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = ElementUiPalette.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = ElementUiPalette.Accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ProfileTabs(
    profile: Profile,
    isAuthorized: Boolean,
    selected: ProfileTab,
    onSelect: (ProfileTab) -> Unit
) {
    val tabs = buildProfileTabs(profile, isAuthorized)
    val selectedIndex = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0)

    UIKit.SegmentTabs(
        tabs = tabs.map { it.second },
        selectedIndex = selectedIndex,
        onSelect = { index ->
            tabs.getOrNull(index)?.first?.let(onSelect)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun buildProfileTabs(profile: Profile, isAuthorized: Boolean): List<Pair<ProfileTab, String>> {
    return buildList {
        add(ProfileTab.Posts to "Посты")
        if (profile.stats.giftsCount > 0 && isAuthorized) {
            add(ProfileTab.Gifts to "Подарки (${profile.stats.giftsCount})")
        }
        if (isAuthorized) {
            val wallTitle = if (profile.stats.wallCount > 0) {
                "Стена (${profile.stats.wallCount})"
            } else {
                "Стена"
            }
            add(ProfileTab.Wall to wallTitle)
        }
        if (isAuthorized && !profile.deleted) {
            add(ProfileTab.Info to "Доп. инфо.")
        }
    }
}

private fun buildProfileGovernItems(
    profile: Profile,
    onSendGift: () -> Unit,
    onBlock: () -> Unit,
    onEditChannel: () -> Unit
): List<ElementContextMenuItem> {
    return buildList {
        add(
            ElementContextMenuItem(
                title = "Отправить подарок",
                icon = Icons.Default.CardGiftcard,
                onClick = onSendGift
            )
        )

        if (!profile.myProfile) {
            add(
                ElementContextMenuItem(
                    title = "Заблокировать",
                    icon = Icons.Default.Block,
                    color = ElementUiPalette.Error,
                    onClick = onBlock
                )
            )
        }

        if (profile.type == "channel" && profile.myProfile) {
            add(
                ElementContextMenuItem(
                    title = "Изменить",
                    icon = Icons.Default.Edit,
                    onClick = onEditChannel
                )
            )
        }
    }
}

@Composable
private fun ProfilePostCard(
    post: FeedPost,
    homeGateway: HomeGateway,
    isAdmin: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onOpenPost: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onReportPost: () -> Unit,
    onEditPost: () -> Unit,
    onDeleteOrRestorePost: () -> Unit,
    onDeletePostForever: () -> Unit,
    onArchiveToggle: () -> Unit,
    onBlockToggle: () -> Unit,
    onOpenImageViewer: (List<PostImage>, Int) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val authorName = post.author?.name ?: "Пользователь"
    val authorUsername = post.author?.username ?: "unknown"
    val authorAvatar = post.author?.avatar
    val authorProfileUsername = post.author?.username?.trim()?.takeIf {
        it.isNotEmpty() && it != "unknown"
    }
    val shareText = "https://elemsocial.com/post/${post.id}"

    val governItems = buildPostGovernItems(
        post = post,
        isAdmin = isAdmin,
        onReportPost = onReportPost,
        onEditPost = onEditPost,
        onDeleteOrRestorePost = onDeleteOrRestorePost,
        onDeletePostForever = onDeletePostForever,
        onArchiveToggle = onArchiveToggle,
        onBlockToggle = onBlockToggle
    )

    ElementPostCard(
        postId = post.id,
        authorName = authorName,
        authorUsername = authorUsername,
        authorAvatar = {
            ProfileAvatar(
                name = authorName,
                asset = authorAvatar,
                homeGateway = homeGateway,
                size = 40.dp
            )
        },
        authorBadge = if (post.author?.icons.hasVerifyBadge()) {
            {
                ProfileBadges(icons = post.author?.icons, size = 17.dp)
            }
        } else if (post.author?.icons.hasGoldBadge()) {
            {
                ProfileBadges(icons = post.author?.icons, size = 17.dp)
            }
        } else {
            null
        },
        onAuthorClick = authorProfileUsername?.let { username ->
            { onOpenProfile(username) }
        },
        dateText = formatTimeAge(post.createDate),
        text = post.text,
        archived = post.archived,
        deleted = post.deleted,
        edited = post.editedAt != null,
        likes = post.likes,
        dislikes = post.dislikes,
        comments = post.comments,
        liked = post.liked,
        disliked = post.disliked,
        interactionsEnabled = true,
        shareLink = shareText,
        governItems = governItems,
        likeBurstTrigger = 0,
        likeBurstOrigin = null,
        onLike = onLike,
        onDislike = onDislike,
        onComment = onOpenPost,
        onCopyLink = {
            clipboard.setText(AnnotatedString(shareText))
            Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
        },
        onDoubleTapLike = {
            if (!post.liked) onLike()
        },
        showShadow = false
    ) {
        post.poll?.let { poll ->
            ElementPostPoll(
                postId = post.id,
                poll = poll,
                onVote = { optionIds -> homeGateway.votePostPoll(post.id, optionIds) }
            )
        }
        if (post.content.songs.isNotEmpty()) {
            PostMusicTracksBlock(
                homeGateway = homeGateway,
                songs = post.content.songs
            )
        }
        if (post.content.images.isNotEmpty()) {
            ProfilePostImages(
                post = post,
                homeGateway = homeGateway,
                onOpenImage = { index -> onOpenImageViewer(post.content.images, index) },
                onDoubleTapLike = {
                    if (!post.liked) {
                        onLike()
                    }
                }
            )
        }
        if (post.content.videos.isNotEmpty()) {
            ElementPostVideosBlock(
                videos = post.content.videos,
                resolveCachedFile = { cacheKey ->
                    homeGateway.resolveCachedFile(cacheKey)
                },
                loadPreviewBytes = { asset ->
                    homeGateway.loadImageBytes(asset)
                },
                loadVideoFile = { video, onProgress, isCancelled ->
                    homeGateway.loadVideoFile(video, onProgress, isCancelled)
                },
                onDoubleTapLike = {
                    if (!post.liked) {
                        onLike()
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfilePostImages(
    post: FeedPost,
    homeGateway: HomeGateway,
    onOpenImage: (Int) -> Unit,
    onDoubleTapLike: (() -> Unit)? = null
) {
    val images = post.content.images.take(3)
    if (images.isEmpty()) return

    if (images.size == 1) {
        val image = images.first()
        val bitmap by rememberImageBitmap(image.asset, homeGateway)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        ) {
            val calculatedHeight = remember(bitmap, maxWidth) {
                calculateWebLikeImageHeight(
                    maxWidth = maxWidth,
                    bitmap = bitmap,
                    minHeight = 120.dp,
                    maxHeight = 300.dp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(calculatedHeight)
                    .background(ElementUiPalette.BlockSoft)
                    .pointerInput(onDoubleTapLike, onOpenImage) {
                        detectTapGestures(
                            onTap = { onOpenImage(0) },
                            onDoubleTap = { onDoubleTapLike?.invoke() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(10.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x47C9C9C9))
                    )
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = image.fileName ?: "post image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    UIKit.Loader(size = 20)
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        images.forEachIndexed { index, image ->
            val bitmap by rememberImageBitmap(image.asset, homeGateway)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElementUiPalette.BlockSoft)
                    .pointerInput(onDoubleTapLike, onOpenImage, index) {
                        detectTapGestures(
                            onTap = { onOpenImage(index) },
                            onDoubleTap = { onDoubleTapLike?.invoke() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = image.fileName ?: "post image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

private fun calculateWebLikeImageHeight(
    maxWidth: Dp,
    bitmap: ImageBitmap?,
    minHeight: Dp,
    maxHeight: Dp
): Dp {
    if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
        return minHeight
    }
    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
    if (ratio <= 0f) return minHeight
    return (maxWidth / ratio).coerceIn(minHeight, maxHeight)
}

@Composable
private fun ProfileInfoBlock(profile: Profile) {
    UIKit.Block {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ProfileInfoLine(
                title = "Дата регистрации:",
                date = profile.createDate
            )
            if (profile.type == "user") {
                ProfileInfoLine(
                    title = "Последний раз в сети:",
                    date = profile.lastOnline
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoLine(title: String, date: String?) {
    Text(
        text = buildString {
            append(title)
            append(" ")
            append(formatProfileDate(date))
            if (!date.isNullOrBlank()) {
                append(" (")
                append(formatTimeAge(date, showDetailed = true))
                append(")")
            }
        },
        color = ElementUiPalette.TextPrimary,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun formatProfileDate(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return "—"

    return runCatching {
        LocalDate.parse(value.take(10)).format(ProfileDateFormatter)
    }.getOrElse {
        value
    }
}

@Composable
private fun ProfileTabPlaceholder(text: String) {
    UIKit.Block {
        Text(
            text = text,
            color = ElementUiPalette.TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun EmptyProfilePosts() {
    UIKit.Block {
        Text(
            text = "Постов пока нет",
            color = ElementUiPalette.TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun EmptyWallPosts() {
    UIKit.Block {
        Text(
            text = "На стене пока нет записей",
            color = ElementUiPalette.TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun EmptyProfileGifts() {
    UIKit.Block {
        Text(
            text = "Подарков пока нет",
            color = ElementUiPalette.TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun ProfileGiftsSkeletonGrid() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(2) {
                    Box(modifier = Modifier.weight(1f)) {
                        UIKit.SkeletonGift(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileGiftsGrid(
    gifts: List<ProfileGift>,
    homeGateway: HomeGateway,
    canToggleVisibility: Boolean,
    giftVisibilityActionId: Int?,
    onToggleVisibility: (ProfileGift) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenGift: (ProfileGift) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        gifts.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { gift ->
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileGiftCard(
                            gift = gift,
                            homeGateway = homeGateway,
                            canToggleVisibility = canToggleVisibility,
                            visibilityActionRunning = giftVisibilityActionId == gift.id,
                            onToggleVisibility = onToggleVisibility,
                            onOpenProfile = onOpenProfile,
                            onOpenGift = onOpenGift
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileGiftCard(
    gift: ProfileGift,
    homeGateway: HomeGateway,
    canToggleVisibility: Boolean,
    visibilityActionRunning: Boolean,
    onToggleVisibility: (ProfileGift) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenGift: (ProfileGift) -> Unit
) {
    val giftBitmap by rememberImageBitmap(asset = gift.image, homeGateway = homeGateway)
    val sender = gift.sender
    val senderUsername = sender?.username?.trim().orEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.Block)
            .clickable { onOpenGift(gift) }
            .padding(horizontal = 7.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                if (canToggleVisibility) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(ElementUiPalette.BlockSoft)
                            .clickable(enabled = !visibilityActionRunning) {
                                onToggleVisibility(gift)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (gift.hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (gift.hidden) "Показать подарок" else "Скрыть подарок",
                            tint = ElementUiPalette.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(86.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (giftBitmap != null) {
                        Image(
                            bitmap = giftBitmap!!,
                            contentDescription = gift.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = "Gift",
                            color = ElementUiPalette.TextSecondary,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = gift.name,
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!gift.description.isNullOrBlank()) {
                    Text(
                        text = gift.description.orEmpty(),
                        color = ElementUiPalette.TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            sender?.let {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = senderUsername.isNotEmpty()) {
                            if (senderUsername.isNotEmpty()) onOpenProfile(senderUsername)
                        }
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProfileAvatar(
                        name = sender.name ?: sender.username ?: "?",
                        asset = sender.avatar,
                        homeGateway = homeGateway,
                        size = 18.dp
                    )
                    Text(
                        text = "от ${sender.name ?: sender.username ?: "Отправителя"}",
                        color = ElementUiPalette.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (gift.hidden || (gift.price != null && gift.price > 0.0)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (gift.hidden) {
                        Text(
                            text = "Скрыт",
                            color = ElementUiPalette.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (gift.price != null && gift.price > 0.0) {
                        Text(
                            text = formatGiftPrice(gift.price),
                            color = ElementUiPalette.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        UIKit.Eball(size = 17.dp, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRelationsModal(
    kind: ProfileRelationsModalKind,
    users: List<ProfileRelationUser>,
    loading: Boolean,
    errorText: String?,
    homeGateway: HomeGateway,
    onClose: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        UIKit.RoutedModal(
            title = kind.title,
            onClose = onClose
        ) {
            when {
                loading -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(8) {
                            UIKit.SkeletonRelation(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                !errorText.isNullOrBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorText,
                            color = ElementUiPalette.Error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                users.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Список пуст",
                            color = ElementUiPalette.TextSecondary,
                            fontSize = 15.sp
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = users,
                            key = { "${it.id}:${it.username}" }
                        ) { user ->
                            ProfileRelationsRow(
                                user = user,
                                homeGateway = homeGateway,
                                onClick = { onOpenProfile(user.username) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSendGiftModal(
    profile: Profile,
    profileGateway: ProfileGateway,
    homeGateway: HomeGateway,
    onClose: () -> Unit,
    onSent: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var gifts by remember(profile.id) { mutableStateOf<List<ProfileCatalogGift>>(emptyList()) }
    var selectedGiftId by remember(profile.id) { mutableStateOf<Int?>(null) }
    var loading by remember(profile.id) { mutableStateOf(true) }
    var sending by remember(profile.id) { mutableStateOf(false) }
    var localError by remember(profile.id) { mutableStateOf<String?>(null) }
    var pendingMessageGiftId by remember(profile.id) { mutableStateOf<Int?>(null) }
    var confirmGiftId by remember(profile.id) { mutableStateOf<Int?>(null) }
    var pendingMessage by remember(profile.id) { mutableStateOf("") }

    LaunchedEffect(profile.id) {
        loading = true
        localError = null
        val result = runCatching { profileGateway.loadCatalogGifts() }.getOrNull()
        if (result?.isSuccess == true) {
            gifts = result.gifts
            val preferred = result.gifts.firstOrNull { it.quantity > 0 } ?: result.gifts.firstOrNull()
            selectedGiftId = preferred?.id
        } else {
            val message = result?.message ?: "Не удалось загрузить подарки"
            localError = message
            onError(message)
        }
        loading = false
    }

    val pendingMessageGift = gifts.firstOrNull { it.id == pendingMessageGiftId }
    val confirmGift = gifts.firstOrNull { it.id == confirmGiftId }

    fun sendGift(gift: ProfileCatalogGift, message: String?) {
        if (gift.quantity <= 0 || sending) return
        localError = null
        sending = true
        scope.launch {
            val result = runCatching {
                profileGateway.sendGift(
                    username = profile.username,
                    giftId = gift.id,
                    message = message
                )
            }.getOrNull()

            if (result?.isSuccess == true) {
                onSent()
            } else {
                val error = result?.message ?: "Не удалось отправить подарок"
                localError = error
                onError(error)
            }
            sending = false
        }
    }

    UIKit.RoutedModal(
        title = "Отправить подарок",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileAvatar(
                    name = profile.name ?: profile.username,
                    asset = profile.avatar,
                    homeGateway = homeGateway,
                    size = 58.dp,
                    borderWidth = 3.dp
                )
                Text(
                    text = "Подарок для ${profile.name ?: profile.username}",
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Выберите подарок и при желании добавьте сообщение",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            UIKit.Loader(size = 24)
                        }
                    }

                    gifts.isEmpty() -> {
                        Text(
                            text = "Подарков пока нет",
                            color = ElementUiPalette.TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            gridItems(gifts, key = { it.id }) { gift ->
                                ProfileCatalogGiftCard(
                                    gift = gift,
                                    homeGateway = homeGateway,
                                    selected = gift.id == selectedGiftId,
                                    sending = sending,
                                    onClick = { selectedGiftId = gift.id },
                                    onPay = {
                                        if (gift.quantity <= 0 || sending) return@ProfileCatalogGiftCard
                                        selectedGiftId = gift.id
                                        pendingMessage = ""
                                        pendingMessageGiftId = gift.id
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            if (!localError.isNullOrBlank()) {
                UIKit.Block(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = localError.orEmpty(),
                        color = ElementUiPalette.Error,
                        fontSize = 13.sp
                    )
                }
            }

            UIKit.Button(
                title = "Отмена",
                onClick = onClose,
                variant = ElementButtonVariant.Soft,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (pendingMessageGift != null) {
        AlertDialog(
            onDismissRequest = { pendingMessageGiftId = null },
            title = { Text("Добавить сообщение") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Вы можете добавить сообщение к подарку, по желанию",
                        color = ElementUiPalette.TextSecondary,
                        fontSize = 13.sp
                    )
                    UIKit.Input(
                        value = pendingMessage,
                        onValueChange = { value ->
                            if (value.length <= 500) pendingMessage = value
                        },
                        placeholder = "Сообщение",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 36.dp, max = 130.dp),
                        singleLine = false
                    )
                    Text(
                        text = "${pendingMessage.length}/500",
                        color = ElementUiPalette.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            properties = DialogProperties(decorFitsSystemWindows = false),
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmGiftId = pendingMessageGift.id
                        pendingMessageGiftId = null
                    }
                ) { Text("Продолжить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingMessageGiftId = null }) { Text("Отмена") }
            }
        )
    }

    if (confirmGift != null) {
        AlertDialog(
            onDismissRequest = { confirmGiftId = null },
            title = { Text("Точно купить?") },
            text = {
                Text("Вы точно хотите купить подарок «${confirmGift.name}» для ${profile.name}?")
            },
            properties = DialogProperties(decorFitsSystemWindows = false),
            confirmButton = {
                TextButton(
                    onClick = {
                        val gift = confirmGift ?: return@TextButton
                        confirmGiftId = null
                        sendGift(
                            gift = gift,
                            message = pendingMessage.trim().ifBlank { null }
                        )
                    }
                ) { Text("Купить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmGiftId = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun ProfileCatalogGiftCard(
    gift: ProfileCatalogGift,
    homeGateway: HomeGateway,
    selected: Boolean,
    sending: Boolean,
    onClick: () -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBitmap by rememberImageBitmap(asset = gift.image, homeGateway = homeGateway)
    val hasDescription = !gift.description.isNullOrBlank()
    val quantityText = gift.quantity.toString()
    val ribbonColor = if (gift.quantity > 0) ElementUiPalette.Accent else ElementUiPalette.TextSecondary.copy(alpha = 0.86f)

    Box(
        modifier = modifier
            .heightIn(min = 170.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ElementUiPalette.Block)
            .border(
                width = if (selected) 1.2.dp else 0.dp,
                color = if (selected) ElementUiPalette.Accent else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp)
    ) {
        if (gift.quantity >= 0) {
            Text(
                text = quantityText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .zIndex(2f)
                    .align(Alignment.TopStart)
                    .offset(x = (-30).dp, y = 8.dp)
                    .rotate(-45f)
                    .background(ribbonColor)
                    .padding(horizontal = 40.dp, vertical = 2.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = gift.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "Gift",
                        color = ElementUiPalette.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = gift.name,
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (hasDescription) {
                        Text(
                            text = gift.description.orEmpty(),
                            color = ElementUiPalette.TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (gift.quantity > 0) ElementUiPalette.Block else ElementUiPalette.Block.copy(alpha = 0.72f))
                    .clickable(
                        enabled = gift.quantity > 0 && !sending,
                        onClick = onPay
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatGiftPrice(gift.price),
                    color = if (gift.quantity > 0) ElementUiPalette.TextPrimary else ElementUiPalette.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                UIKit.Eball(
                    size = 20.dp,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileRelationsRow(
    user: ProfileRelationUser,
    homeGateway: HomeGateway,
    onClick: () -> Unit
) {
    UIKit.Block(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                name = user.name,
                asset = user.avatar,
                homeGateway = homeGateway,
                size = 34.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = user.name,
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${user.username} • ${user.subscribers} подписчиков • ${user.posts} постов",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProfileGiftDetailsModal(
    gift: ProfileGift,
    receiverProfile: Profile?,
    homeGateway: HomeGateway,
    onOpenProfile: (String) -> Unit,
    onClose: () -> Unit
) {
    val imageBitmap by rememberImageBitmap(asset = gift.image, homeGateway = homeGateway)

    UIKit.RoutedModal(
        title = "Подарок",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = gift.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "Gift",
                        color = ElementUiPalette.TextSecondary,
                        fontSize = 16.sp
                    )
                }
            }

            Text(
                text = gift.name,
                color = ElementUiPalette.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!gift.description.isNullOrBlank()) {
                Text(
                    text = gift.description.orEmpty(),
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }

            val rowDividerColor = ElementUiPalette.TextSecondary.copy(alpha = 0.24f)
            val labels = listOf("От", "Кому", "Цена", "Сообщение", "Дата")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.width(92.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    labels.forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 30.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = label,
                                color = ElementUiPalette.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (index < labels.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(rowDividerColor)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val senderName = gift.sender?.name ?: gift.sender?.username ?: "—"
                    val senderUsername = gift.sender?.username?.trim().orEmpty()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 30.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (senderUsername.isNotEmpty()) {
                            Text(
                                text = senderName,
                                color = ElementUiPalette.Accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onOpenProfile(senderUsername) }
                            )
                        } else {
                            Text(
                                text = senderName,
                                color = ElementUiPalette.TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(rowDividerColor)
                    )

                    val receiverName = receiverProfile?.name ?: receiverProfile?.username ?: "—"
                    val receiverUsername = receiverProfile?.username?.trim().orEmpty()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 30.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (receiverUsername.isNotEmpty()) {
                            Text(
                                text = receiverName,
                                color = ElementUiPalette.Accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onOpenProfile(receiverUsername) }
                            )
                        } else {
                            Text(
                                text = receiverName,
                                color = ElementUiPalette.TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(rowDividerColor)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 30.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (gift.price != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = formatGiftPrice(gift.price),
                                    color = ElementUiPalette.TextPrimary,
                                    fontSize = 14.sp
                                )
                                UIKit.Eball(size = 18.dp, fontSize = 9.sp)
                            }
                        } else {
                            Text(
                                text = "—",
                                color = ElementUiPalette.TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(rowDividerColor)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 30.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = gift.message?.takeIf { it.isNotBlank() } ?: "—",
                            color = ElementUiPalette.TextPrimary,
                            fontSize = 14.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(rowDividerColor)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 30.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = formatGiftDate(gift.date),
                            color = ElementUiPalette.TextPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatGiftDate(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return "—"

    return runCatching {
        LocalDateTime.parse(value, ProfileGiftRawDateFormatter).format(ProfileGiftDateFormatter)
    }.recoverCatching {
        LocalDateTime.parse(value.replace("T", " ").take(19), ProfileGiftRawDateFormatter)
            .format(ProfileGiftDateFormatter)
    }.getOrElse {
        value
    }
}

private fun formatGiftPrice(price: Double?): String {
    val amount = price ?: return "0"
    return amount.toString().trimEnd('0').trimEnd('.').ifBlank { "0" }
}

@Composable
private fun ProfileAvatar(
    name: String,
    asset: PostImageAsset?,
    homeGateway: HomeGateway,
    size: Dp = 56.dp,
    borderWidth: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val bitmap by rememberImageBitmap(
        asset = asset,
        homeGateway = homeGateway
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF7F6EB0), Color(0xFFA19DB1))))
            .border(borderWidth, ElementUiPalette.Block, CircleShape),
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
                fontSize = (size.value * 0.34f).sp
            )
        }
    }
}

private fun List<String>?.hasVerifyBadge(): Boolean {
    return this?.any { it.equals(VerifyIconId, ignoreCase = true) } == true
}

private fun List<String>?.hasGoldBadge(): Boolean {
    return this?.any { it.equals("GOLD", ignoreCase = true) } == true
}

@Composable
private fun OnlineMarker(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(3.dp, ElementUiPalette.Block, CircleShape)
            .background(Color(0xFF63D53A))
    )
}

@Composable
private fun rememberImageBitmap(
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
private fun ErrorBlock(text: String) {
    UIKit.Block(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 12.dp
    ) {
        Text(
            text = text,
            color = ElementUiPalette.Error,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
