package elemsocial.com.feature.home.presentation

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.R
import elemsocial.com.core.time.formatTimeAge
import elemsocial.com.domain.model.AuthAccountChannel
import elemsocial.com.domain.model.FeedPost
import elemsocial.com.domain.model.MusicTrack
import elemsocial.com.domain.model.OnlineUser
import elemsocial.com.domain.model.PostImage
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostPoll
import elemsocial.com.domain.model.PostsCategory
import elemsocial.com.domain.model.UploadFilePayload
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
import elemsocial.com.ui.pack.theme.ElementUiPalette
import elemsocial.com.feature.music.presentation.MusicAttachedTracks
import elemsocial.com.feature.music.presentation.MusicPickerModal
import elemsocial.com.feature.music.presentation.PostMusicTracksBlock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import kotlin.math.max

private const val PAGE_SIZE = 25
private const val MAX_TOTAL_FILE_SIZE = 52_428_800L

private data class CategoryState(
    val posts: List<FeedPost> = emptyList(),
    val startIndex: Int = 0,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val hasMore: Boolean = true,
    val errorText: String? = null
)

private data class PostInteractionState(
    val likes: Int,
    val dislikes: Int,
    val liked: Boolean,
    val disliked: Boolean,
    val isSending: Boolean = false
)

private data class ImageViewerState(
    val images: List<PostImage>,
    val index: Int
)

private val ErrorColor = Color(0xFFDC4D63)

@Composable
fun HomeScreen(
    homeGateway: HomeGateway,
    accountId: Int?,
    accountName: String? = null,
    accountUsername: String? = null,
    accountAvatar: PostImageAsset? = null,
    accountChannels: List<AuthAccountChannel> = emptyList(),
    selectedComposerChannel: AuthAccountChannel? = null,
    onSelectedComposerChannelChange: (AuthAccountChannel?) -> Unit = {},
    accountBalance: Double? = null,
    isAdmin: Boolean,
    initialCategory: PostsCategory = PostsCategory.Last,
    requestedOpenPostId: Int? = null,
    onPostRequestHandled: () -> Unit = {},
    onOpenSidebar: () -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    topPadding: Dp = 76.dp,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    var postComposerText by remember { mutableStateOf("") }
    var selectedFiles by remember { mutableStateOf<List<UploadFilePayload>>(emptyList()) }
    var selectedMusicTracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var postFilesSettingsOpen by remember { mutableStateOf(false) }
    var clearMetadataImage by remember { mutableStateOf(false) }
    var censoringImage by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var categoryStates by remember {
        mutableStateOf(PostsCategory.entries.associateWith { CategoryState() })
    }
    var onlineUsers by remember { mutableStateOf<List<OnlineUser>>(emptyList()) }
    var interactionStates by remember { mutableStateOf<Map<Int, PostInteractionState>>(emptyMap()) }
    var likeBurstTokens by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var likeBurstOrigins by remember { mutableStateOf<Map<Int, Offset?>>(emptyMap()) }
    var transientError by remember { mutableStateOf<String?>(null) }
    var openedPost by remember { mutableStateOf<FeedPost?>(null) }
    var editingPost by remember { mutableStateOf<FeedPost?>(null) }
    var isPostSending by remember { mutableStateOf(false) }
    var imageViewerState by remember { mutableStateOf<ImageViewerState?>(null) }
    var feedPreloadStarted by remember { mutableStateOf(false) }
    var channelPickerExpanded by remember { mutableStateOf(false) }
    var musicPickerOpen by remember { mutableStateOf(false) }
    var pollCreatorOpen by remember { mutableStateOf(false) }
    var composerPoll by remember { mutableStateOf<PostPoll?>(null) }

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

    fun postLink(postId: Int): String = "https://elemsocial.com/post/$postId"

    fun copyPostLink(postId: Int) {
        clipboard.setText(AnnotatedString(postLink(postId)))
        Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
    }

    fun openPost(post: FeedPost) {
        openedPost = post
    }

    fun openImageViewer(images: List<PostImage>, index: Int) {
        if (images.isEmpty()) return
        imageViewerState = ImageViewerState(
            images = images,
            index = index.coerceIn(0, images.lastIndex)
        )
    }

    fun openPostById(postId: Int) {
        scope.launch {
            val result = runCatching { homeGateway.loadPost(postId) }.getOrNull()
            val loaded = result?.post
            if (loaded != null) {
                openedPost = loaded
                transientError = null
            } else {
                transientError = result?.message ?: "Не удалось открыть пост"
            }
            onPostRequestHandled()
        }
    }

    fun appendQuickEmoji() {
        postComposerText = if (postComposerText.isBlank()) {
            "🙂"
        } else {
            "${postComposerText.trimEnd()} 🙂"
        }
    }

    fun showComposerFeatureStub(label: String) {
        Toast.makeText(context, "$label подключим следующим шагом", Toast.LENGTH_SHORT).show()
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

    fun refreshOnlineUsers() {
        scope.launch {
            val users = runCatching { homeGateway.loadOnlineUsers() }.getOrNull().orEmpty()
            onlineUsers = users
        }
    }

    fun readState(category: PostsCategory): CategoryState {
        return categoryStates[category] ?: CategoryState()
    }

    fun updateState(category: PostsCategory, update: (CategoryState) -> CategoryState) {
        val source = categoryStates.toMutableMap()
        source[category] = update(readState(category))
        categoryStates = source
    }

    fun loadPosts(category: PostsCategory, reset: Boolean) {
        val state = readState(category)
        if (state.isLoading) return
        if (!reset && !state.hasMore) return

        val startIndex = if (reset) 0 else state.startIndex
        updateState(category) { it.copy(isLoading = true, errorText = null) }

        scope.launch {
            val result = runCatching {
                homeGateway.loadPosts(category, startIndex)
            }.getOrElse { error ->
                updateState(category) {
                    it.copy(
                        isLoading = false,
                        isLoaded = true,
                        errorText = error.message ?: "Ошибка загрузки ленты"
                    )
                }
                return@launch
            }

            if (!result.isSuccess) {
                updateState(category) {
                    it.copy(
                        isLoading = false,
                        isLoaded = true,
                        errorText = result.message ?: "Сервер вернул ошибку"
                    )
                }
                return@launch
            }

            updateState(category) { old ->
                val merged = if (reset) {
                    result.posts
                } else {
                    (old.posts + result.posts).distinctBy { it.id }
                }

                old.copy(
                    posts = merged,
                    startIndex = if (reset) result.posts.size else old.startIndex + result.posts.size,
                    isLoading = false,
                    isLoaded = true,
                    hasMore = result.posts.size >= PAGE_SIZE,
                    errorText = null
                )
            }

            if (!feedPreloadStarted && category == selectedCategory) {
                feedPreloadStarted = true
                scope.launch {
                    val preloadOrder = PostsCategory.entries
                        .filterNot { it == selectedCategory }
                    preloadOrder.forEachIndexed { index, target ->
                        delay(250L * (index + 1))
                        val targetState = readState(target)
                        if (!targetState.isLoaded && !targetState.isLoading) {
                            loadPosts(target, reset = true)
                        }
                    }
                }
            }
        }
    }

    fun sendPost() {
        val text = postComposerText.trim()
        if ((text.isBlank() && selectedFiles.isEmpty() && selectedMusicTracks.isEmpty() && composerPoll == null) || isPostSending) return

        isPostSending = true
        scope.launch {
            val result = runCatching {
                homeGateway.createPost(
                    text = text,
                    files = selectedFiles,
                    songs = selectedMusicTracks.map { it.id },
                    fromChannelId = selectedComposerChannel?.id,
                    poll = composerPoll,
                    clearMetadataImage = clearMetadataImage,
                    censoringImage = censoringImage
                )
            }.getOrNull()

            if (result?.isSuccess == true) {
                postComposerText = ""
                selectedFiles = emptyList()
                selectedMusicTracks = emptyList()
                composerPoll = null
                postFilesSettingsOpen = false
                clearMetadataImage = false
                censoringImage = false
                transientError = null
                Toast.makeText(context, "Пост опубликован", Toast.LENGTH_SHORT).show()

                loadPosts(PostsCategory.Last, reset = true)
                if (selectedCategory == PostsCategory.Last) {
                    listState.animateScrollToItem(0)
                }
            } else {
                transientError = result?.message ?: "Не удалось опубликовать пост"
            }

            isPostSending = false
        }
    }

    fun readInteraction(post: FeedPost): PostInteractionState {
        return interactionStates[post.id] ?: PostInteractionState(
            likes = post.likes,
            dislikes = post.dislikes,
            liked = post.liked,
            disliked = post.disliked
        )
    }

    fun updateInteraction(postId: Int, update: (PostInteractionState) -> PostInteractionState) {
        val source = interactionStates.toMutableMap()
        val current = source[postId] ?: return
        source[postId] = update(current)
        interactionStates = source
    }

    fun ensureInteraction(post: FeedPost) {
        if (interactionStates.containsKey(post.id)) return
        interactionStates = interactionStates + (
            post.id to PostInteractionState(
                likes = post.likes,
                dislikes = post.dislikes,
                liked = post.liked,
                disliked = post.disliked
            )
        )
    }

    fun triggerLikeBurst(postId: Int, origin: Offset? = null) {
        val nextToken = (likeBurstTokens[postId] ?: 0) + 1
        likeBurstTokens = likeBurstTokens + (postId to nextToken)
        likeBurstOrigins = likeBurstOrigins + (postId to origin)
    }

    fun updateCommentsCount(postId: Int, comments: Int) {
        categoryStates = categoryStates.mapValues { (_, state) ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id == postId) post.copy(comments = comments) else post
                }
            )
        }
    }

    fun mutatePostEverywhere(postId: Int, transform: (FeedPost) -> FeedPost): FeedPost? {
        var updatedPost: FeedPost? = null
        categoryStates = categoryStates.mapValues { (_, state) ->
            state.copy(
                posts = state.posts.map { item ->
                    if (item.id != postId) {
                        item
                    } else {
                        transform(item).also { next ->
                            updatedPost = next
                        }
                    }
                }
            )
        }
        if (openedPost?.id == postId && updatedPost != null) {
            openedPost = updatedPost
        }
        return updatedPost
    }

    fun removePostEverywhere(postId: Int) {
        categoryStates = categoryStates.mapValues { (_, state) ->
            state.copy(posts = state.posts.filterNot { it.id == postId })
        }
        if (openedPost?.id == postId) {
            openedPost = null
        }
    }

    fun setPostFeedback(resultMessage: String?, fallbackError: String) {
        transientError = resultMessage?.takeIf { it.isNotBlank() } ?: fallbackError
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

            transientError = null
            if (post.deleted) {
                mutatePostEverywhere(post.id) { it.copy(deleted = false) }
            } else {
                if (isAdmin) {
                    mutatePostEverywhere(post.id) { it.copy(deleted = true) }
                } else {
                    removePostEverywhere(post.id)
                }
            }
        }
    }

    fun handleDeleteForever(post: FeedPost) {
        scope.launch {
            val result = homeGateway.deletePostForever(post.id)
            if (result.isSuccess) {
                transientError = null
                removePostEverywhere(post.id)
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
                transientError = null
                removePostEverywhere(post.id)
            } else {
                setPostFeedback(result.message, "Не удалось обновить архив поста")
            }
        }
    }

    fun handleBlockToggle(post: FeedPost) {
        val username = post.author?.username?.trim().orEmpty()
        if (username.isBlank()) {
            transientError = "Невозможно определить пользователя"
            return
        }

        scope.launch {
            val shouldBlock = !(post.author?.blocked ?: false)
            val result = if (shouldBlock) {
                homeGateway.blockProfile(username)
            } else {
                homeGateway.unblockProfile(username)
            }

            if (result.isSuccess) {
                transientError = null
                val successText = if (shouldBlock) {
                    "Пользователь заблокирован"
                } else {
                    "Пользователь разблокирован"
                }
                Toast.makeText(context, successText, Toast.LENGTH_SHORT).show()
                categoryStates = categoryStates.mapValues { (_, state) ->
                    state.copy(
                        posts = state.posts.map { item ->
                            if (item.author?.username == username) {
                                item.copy(
                                    author = item.author?.copy(blocked = shouldBlock)
                                )
                            } else {
                                item
                            }
                        }
                    )
                }
            } else {
                setPostFeedback(result.message, "Не удалось обновить блокировку пользователя")
            }
        }
    }

    fun toggleLike(post: FeedPost, burstOrigin: Offset? = null) {
        ensureInteraction(post)
        val before = readInteraction(post)
        if (before.isSending) return

        val next = if (before.liked) {
            before.copy(
                likes = max(0, before.likes - 1),
                liked = false,
                isSending = true
            )
        } else {
            triggerLikeBurst(post.id, burstOrigin)
            before.copy(
                likes = before.likes + 1,
                liked = true,
                dislikes = if (before.disliked) max(0, before.dislikes - 1) else before.dislikes,
                disliked = false,
                isSending = true
            )
        }

        interactionStates = interactionStates + (post.id to next)

        scope.launch {
            val success = runCatching { homeGateway.likePost(post.id) }.getOrDefault(false)
            if (success) {
                updateInteraction(post.id) { it.copy(isSending = false) }
                transientError = null
            } else {
                interactionStates = interactionStates + (post.id to before)
                transientError = "Не удалось обновить лайк"
            }
        }
    }

    fun toggleDislike(post: FeedPost) {
        ensureInteraction(post)
        val before = readInteraction(post)
        if (before.isSending) return

        val next = if (before.disliked) {
            before.copy(
                dislikes = max(0, before.dislikes - 1),
                disliked = false,
                isSending = true
            )
        } else {
            before.copy(
                dislikes = before.dislikes + 1,
                disliked = true,
                likes = if (before.liked) max(0, before.likes - 1) else before.likes,
                liked = false,
                isSending = true
            )
        }

        interactionStates = interactionStates + (post.id to next)

        scope.launch {
            val success = runCatching { homeGateway.dislikePost(post.id) }.getOrDefault(false)
            if (success) {
                updateInteraction(post.id) { it.copy(isSending = false) }
                transientError = null
            } else {
                interactionStates = interactionStates + (post.id to before)
                transientError = "Не удалось обновить дизлайк"
            }
        }
    }

    fun likeFromDoubleTap(post: FeedPost, origin: Offset) {
        val current = readInteraction(post)
        if (current.liked || current.isSending) return
        toggleLike(post, burstOrigin = origin)
    }

    LaunchedEffect(selectedCategory) {
        val state = readState(selectedCategory)
        if (!state.isLoaded && !state.isLoading) {
            loadPosts(selectedCategory, reset = true)
        }
    }

    LaunchedEffect(initialCategory) {
        if (!categoryStates.containsKey(initialCategory)) return@LaunchedEffect
        selectedCategory = initialCategory
    }

    LaunchedEffect(requestedOpenPostId) {
        val target = requestedOpenPostId ?: return@LaunchedEffect
        openPostById(target)
    }

    LaunchedEffect(Unit) {
        refreshOnlineUsers()
    }

    LaunchedEffect(transientError) {
        if (transientError.isNullOrBlank()) return@LaunchedEffect
        delay(2_500)
        transientError = null
    }

    val currentState = readState(selectedCategory)
    val visiblePosts = if (isAdmin) {
        currentState.posts
    } else {
        currentState.posts.filterNot { it.deleted }
    }
    val hasImageAttachments = selectedFiles.any { it.mimeType.startsWith("image/") }

    LaunchedEffect(selectedCategory) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            lastVisible to total
        }.collect { (lastVisible, total) ->
            if (lastVisible < 0 || total <= 0) return@collect
            val nearEnd = lastVisible >= (total - 3).coerceAtLeast(0)
            if (!nearEnd) return@collect

            val state = readState(selectedCategory)
            if (!state.isLoading && state.hasMore && state.isLoaded && state.posts.isNotEmpty()) {
                loadPosts(selectedCategory, reset = false)
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
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (topPadding > 0.dp) {
                item {
                    Spacer(modifier = Modifier.height(topPadding))
                }
            }

            item {
                FeedCenteredItem {
                    OnlineUsersBlock(
                        users = onlineUsers,
                        homeGateway = homeGateway,
                        onOpenProfile = onOpenProfile
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    FeedCenteredItem {
                            ElementPostComposer(
                                text = postComposerText,
                                isSending = isPostSending,
                                avatar = {
                                    HomeComposerChannelPicker(
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
                            onTextChange = { postComposerText = it },
                            onEmojiClick = ::appendQuickEmoji,
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
                        FeedCenteredItem {
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
                        FeedCenteredItem {
                            AttachedPollPreview(
                                poll = composerPoll!!,
                                onRemove = { composerPoll = null }
                            )
                        }
                    }

                    FeedCenteredItem {
                        CategoryTabs(
                            selected = selectedCategory,
                            onSelect = {
                                transientError = null
                                selectedCategory = it
                            }
                        )
                    }
                }
            }

            if (!transientError.isNullOrBlank()) {
                item {
                    FeedCenteredItem {
                        Text(
                            text = transientError.orEmpty(),
                            color = ErrorColor,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            when {
                currentState.isLoading && !currentState.isLoaded -> {
                    item {
                        FeedCenteredItem {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(3) { index ->
                                    UIKit.SkeletonPost(
                                        modifier = Modifier.fillMaxWidth(),
                                        mediaHeight = if (index == 0) 210.dp else 176.dp,
                                        showMedia = index != 1
                                    )
                                }
                            }
                        }
                    }
                }

                currentState.errorText != null && visiblePosts.isEmpty() -> {
                    item {
                        FeedCenteredItem {
                            ErrorBlock(
                                text = currentState.errorText ?: "Ошибка",
                                onRetry = { loadPosts(selectedCategory, reset = true) }
                            )
                        }
                    }
                }

                visiblePosts.isEmpty() -> {
                    item {
                        FeedCenteredItem {
                            EmptyBlock()
                        }
                    }
                }

                else -> {
                    items(visiblePosts, key = { it.id }) { post ->
                        val interaction = readInteraction(post)
                        val burstToken = likeBurstTokens[post.id] ?: 0
                        val burstOrigin = likeBurstOrigins[post.id]
                        FeedCenteredItem {
                            PostCard(
                                post = post,
                                interaction = interaction,
                                isAdmin = isAdmin,
                                likeBurstToken = burstToken,
                                likeBurstOrigin = burstOrigin,
                                homeGateway = homeGateway,
                                onLike = { toggleLike(post) },
                                onDislike = { toggleDislike(post) },
                                onDoubleLike = { origin -> likeFromDoubleTap(post, origin) },
                                onCopyLink = { copyPostLink(post.id) },
                                onOpenPost = { openPost(post) },
                                onOpenProfile = onOpenProfile,
                                onReportPost = {
                                    Toast.makeText(context, "Жалобы подключим следующим шагом", Toast.LENGTH_SHORT).show()
                                },
                                onEditPost = { editingPost = post },
                                onDeleteOrRestorePost = { handleDeleteOrRestore(post) },
                                onDeletePostForever = { handleDeleteForever(post) },
                                onArchiveToggle = { handleArchiveToggle(post) },
                                onBlockToggle = { handleBlockToggle(post) },
                                onOpenImageViewer = { imageIndex ->
                                    openImageViewer(post.content.images, imageIndex)
                                }
                            )
                        }
                    }

                    if (currentState.isLoading) {
                        item {
                            FeedCenteredItem {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    UIKit.Loader(size = 24)
                                }
                            }
                        }
                    }

                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
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

        openedPost?.let { selectedPost ->
            PostDetailsModal(
                postId = selectedPost.id,
                initialPost = selectedPost,
                accountId = accountId,
                accountName = accountName,
                accountAvatar = accountAvatar,
                isAdmin = isAdmin,
                homeGateway = homeGateway,
                onClose = { openedPost = null },
                onOpenProfile = onOpenProfile,
                onCommentCountChanged = ::updateCommentsCount,
                onPostMutated = { changed ->
                    mutatePostEverywhere(changed.id) { changed }
                },
                onPostRemoved = ::removePostEverywhere,
                onReportPost = {
                    Toast.makeText(context, "Жалобы подключим следующим шагом", Toast.LENGTH_SHORT).show()
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
                    mutatePostEverywhere(updated.id) { current ->
                        current.copy(
                            text = updated.text,
                            editedAt = updated.editedAt
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun OnlineUsersBlock(
    users: List<OnlineUser>,
    homeGateway: HomeGateway,
    onOpenProfile: (String) -> Unit
) {
    UIKit.Block {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "сейчас в сети",
                color = ElementUiPalette.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (users.isEmpty()) {
                Text(
                    text = "Сейчас никого нет онлайн",
                    color = ElementUiPalette.TextLite,
                    fontSize = 12.sp
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    users.forEach { user ->
                        val username = user.username.trim()
                        WebAvatar(
                            user = user,
                            homeGateway = homeGateway,
                            modifier = if (username.isNotEmpty()) {
                                Modifier.clickable { onOpenProfile(username) }
                            } else {
                                Modifier
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WebAvatar(
    name: String,
    asset: PostImageAsset?,
    homeGateway: HomeGateway,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF7F6EB0), Color(0xFFA19DB1)))),
        contentAlignment = Alignment.Center
    ) {
        val bitmap by rememberImageBitmap(
            asset = asset,
            homeGateway = homeGateway
        )

        if (bitmap != null) {
            androidx.compose.foundation.Image(
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

private fun homeComposerChannelName(channel: AuthAccountChannel): String {
    val channelId = channel.id
    return channel.name
        ?.takeIf { it.isNotBlank() }
        ?: channel.username
            ?.takeIf { it.isNotBlank() }
        ?: if (channelId != null) "Канал #$channelId" else "Канал"
}

@Composable
private fun HomeComposerChannelPicker(
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
                WebAvatar(
                    name = composerAuthorName,
                    asset = composerAuthorAvatar,
                    homeGateway = homeGateway,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        UIKit.DropdownMenu(
            expanded = expanded && canSelectChannel,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.widthIn(min = 270.dp, max = 300.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Написать от имени...",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HomeComposerChannelMenuItem(
                name = personalName,
                subtitle = "Личный профиль",
                selected = selectedComposerChannel == null,
                avatar = {
                    WebAvatar(
                        name = personalName,
                        asset = accountAvatar,
                        homeGateway = homeGateway,
                        modifier = Modifier.size(30.dp)
                    )
                },
                onClick = {
                    onSelectedComposerChannelChange(null)
                    onExpandedChange(false)
                }
            )

            accountChannels.forEach { channel ->
                val channelId = channel.id ?: return@forEach
                val channelName = homeComposerChannelName(channel)
                HomeComposerChannelMenuItem(
                    name = channelName,
                    subtitle = "Канал",
                    selected = selectedComposerChannel?.id == channelId,
                    avatar = {
                        WebAvatar(
                            name = channelName,
                            asset = channel.avatar,
                            homeGateway = homeGateway,
                            modifier = Modifier.size(30.dp)
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
private fun HomeComposerChannelMenuItem(
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
private fun OnlineMarker() {
    Box(
        modifier = Modifier
            .size(13.dp)
            .clip(CircleShape)
            .border(2.dp, ElementUiPalette.Block, CircleShape)
            .background(Color(0xFF63D53A))
    )
}

@Composable
private fun WebAvatar(
    user: OnlineUser,
    homeGateway: HomeGateway,
    withOnlineMarker: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(40.dp)) {
        WebAvatar(
            name = user.name,
            asset = user.avatar,
            homeGateway = homeGateway,
            modifier = Modifier.fillMaxSize()
        )
        if (withOnlineMarker) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
            ) {
                OnlineMarker()
            }
        }
    }
}

@Composable
private fun FeedCenteredItem(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ErrorBlock(
    text: String,
    onRetry: () -> Unit
) {
    UIKit.Block {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = text,
                color = ErrorColor,
                fontSize = 14.sp
            )
            UIKit.Button(
                title = "Повторить",
                onClick = onRetry,
                variant = ElementButtonVariant.Soft,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyBlock() {
    UIKit.Block {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Постов пока нет",
                color = ElementUiPalette.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun CategoryTabs(
    selected: PostsCategory,
    onSelect: (PostsCategory) -> Unit
) {
    val entries = PostsCategory.entries
    UIKit.SegmentTabs(
        tabs = entries.map { it.title },
        selectedIndex = entries.indexOf(selected).coerceAtLeast(0),
        onSelect = { index -> onSelect(entries[index]) }
    )
}

@Composable
private fun PostCard(
    post: FeedPost,
    interaction: PostInteractionState,
    isAdmin: Boolean,
    likeBurstToken: Int,
    likeBurstOrigin: Offset?,
    homeGateway: HomeGateway,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onDoubleLike: (Offset) -> Unit,
    onCopyLink: () -> Unit,
    onOpenPost: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onReportPost: () -> Unit,
    onEditPost: () -> Unit,
    onDeleteOrRestorePost: () -> Unit,
    onDeletePostForever: () -> Unit,
    onArchiveToggle: () -> Unit,
    onBlockToggle: () -> Unit,
    onOpenImageViewer: (Int) -> Unit
) {
    val authorName = post.author?.name ?: "Удаленный аккаунт"
    val authorUsername = post.author?.username ?: "unknown"
    val authorProfileUsername = post.author?.username?.trim()?.takeIf {
        it.isNotEmpty() && it != "unknown"
    }
    val authorAvatar = post.author?.avatar
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
            WebAvatar(
                name = authorName,
                asset = authorAvatar,
                homeGateway = homeGateway,
                modifier = Modifier.size(40.dp)
            )
        },
        authorBadge = if (post.author?.icons.hasVerifyBadge() == true ||
            post.author?.icons.hasGoldBadge() == true
        ) {
            {
                FeedAuthorBadges(
                    icons = post.author?.icons,
                    size = 16.dp
                )
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
        likes = interaction.likes,
        dislikes = interaction.dislikes,
        comments = post.comments,
        liked = interaction.liked,
        disliked = interaction.disliked,
        interactionsEnabled = !interaction.isSending,
        shareLink = "https://elemsocial.com/post/${post.id}",
        governItems = governItems,
        likeBurstTrigger = likeBurstToken,
        likeBurstOrigin = likeBurstOrigin,
        onLike = onLike,
        onDislike = onDislike,
        onComment = onOpenPost,
        onCopyLink = onCopyLink,
        onDoubleTapLike = onDoubleLike,
        showShadow = false,
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
            PostImagesBlock(
                images = post.content.images,
                homeGateway = homeGateway,
                onOpenImage = onOpenImageViewer,
                onDoubleTap = onDoubleLike
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
                onDoubleTapLike = onDoubleLike
            )
        }
    }
}

private fun List<String>?.hasVerifyBadge(): Boolean {
    return this?.any { it.equals("VERIFY", ignoreCase = true) } == true
}

private fun List<String>?.hasGoldBadge(): Boolean {
    return this?.any { it.equals("GOLD", ignoreCase = true) } == true
}

@Composable
private fun FeedAuthorBadges(
    icons: List<String>?,
    size: Dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icons.hasVerifyBadge()) {
            Image(
                painter = painterResource(id = R.drawable.ic_profile_verify),
                contentDescription = null,
                modifier = Modifier.size(size)
            )
        }
        if (icons.hasGoldBadge()) {
            Image(
                painter = painterResource(id = R.drawable.ic_profile_gold),
                contentDescription = null,
                modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
private fun PostImagesBlock(
    images: List<PostImage>,
    homeGateway: HomeGateway,
    onOpenImage: (Int) -> Unit,
    onDoubleTap: ((Offset) -> Unit)? = null
) {
    if (images.size == 1) {
        PostImageTile(
            image = images.first(),
            homeGateway = homeGateway,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Fit,
            blurBackdropForFit = true,
            dynamicFitHeight = true,
            onDoubleTap = onDoubleTap,
            onClick = { onOpenImage(0) }
        )
        return
    }

    val first = images.getOrNull(0)
    val second = images.getOrNull(1)
    val third = images.getOrNull(2)

    if (first == null || second == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ElementUiPalette.Block),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        PostImageTile(
            image = first,
            homeGateway = homeGateway,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentScale = ContentScale.Crop,
            onDoubleTap = onDoubleTap,
            onClick = { onOpenImage(0) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            PostImageTile(
                image = second,
                homeGateway = homeGateway,
                modifier = if (third == null) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                },
                contentScale = ContentScale.Crop,
                onDoubleTap = onDoubleTap,
                onClick = { onOpenImage(1) }
            )

            if (third != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    PostImageTile(
                        image = third,
                        homeGateway = homeGateway,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onDoubleTap = onDoubleTap,
                        onClick = { onOpenImage(2) }
                    )

                    if (images.size > 3) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${images.size - 3}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostImageTile(
    image: PostImage,
    homeGateway: HomeGateway,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    blurBackdropForFit: Boolean = false,
    dynamicFitHeight: Boolean = false,
    minFitHeight: Dp = 100.dp,
    maxFitHeight: Dp = 300.dp,
    onDoubleTap: ((Offset) -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val bitmap by rememberImageBitmap(
        asset = image.asset,
        homeGateway = homeGateway
    )

    val base = parseAura(image.asset?.aura)
    val bg = Brush.linearGradient(
        listOf(
            base.copy(alpha = 0.95f),
            base.copy(alpha = 0.65f)
        )
    )

    val content: @Composable BoxScope.() -> Unit = {
        if (bitmap != null) {
            if (blurBackdropForFit && contentScale == ContentScale.Fit) {
                androidx.compose.foundation.Image(
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
            }
            androidx.compose.foundation.Image(
                bitmap = bitmap!!,
                contentDescription = image.fileName ?: "post-image",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            UIKit.Loader(size = 22)
        }
    }

    if (dynamicFitHeight && contentScale == ContentScale.Fit) {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val calculatedHeight = remember(bitmap, maxWidth) {
                calculateWebLikeImageHeight(
                    maxWidth = maxWidth,
                    bitmap = bitmap,
                    minHeight = minFitHeight,
                    maxHeight = maxFitHeight
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(calculatedHeight)
                    .background(bg)
                    .pointerInput(onDoubleTap, onClick) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onDoubleTap = { offset -> onDoubleTap?.invoke(offset) }
                        )
                    },
                contentAlignment = Alignment.Center,
                content = content
            )
        }
        return
    }

    Box(
        modifier = modifier
            .background(bg)
            .pointerInput(onDoubleTap, onClick) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { offset -> onDoubleTap?.invoke(offset) }
                )
            },
        contentAlignment = Alignment.Center,
        content = content
    )
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

private fun parseAura(raw: String?): Color {
    if (raw.isNullOrBlank()) return Color(0xFFE8E5F1)
    val regex = Regex("""rgb\((\d+),\s*(\d+),\s*(\d+)\)""")
    val match = regex.find(raw) ?: return Color(0xFFE8E5F1)
    val r = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return Color(0xFFE8E5F1)
    val g = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return Color(0xFFE8E5F1)
    val b = match.groupValues.getOrNull(3)?.toIntOrNull() ?: return Color(0xFFE8E5F1)
    return Color(r, g, b)
}
