package elemsocial.com.feature.home.presentation

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import elemsocial.com.R
import elemsocial.com.core.time.formatTimeAge
import elemsocial.com.domain.model.FeedPost
import elemsocial.com.domain.model.PostComment
import elemsocial.com.domain.model.PostImage
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.UploadFilePayload
import elemsocial.com.feature.music.presentation.PostMusicTracksBlock
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.components.modals.ElementContextMenuItem
import elemsocial.com.ui.pack.components.posts.ElementEditPostModal
import elemsocial.com.ui.pack.components.posts.ElementPostComposer
import elemsocial.com.ui.pack.components.posts.ElementPostCard
import elemsocial.com.ui.pack.components.posts.ElementImageViewerOverlay
import elemsocial.com.ui.pack.components.posts.ElementPostPoll
import elemsocial.com.ui.pack.components.posts.ElementPostVideosBlock
import elemsocial.com.ui.pack.components.posts.buildPostGovernItems
import elemsocial.com.ui.pack.components.text.ElementLinkText
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

private data class ModalPostInteractionState(
    val likes: Int,
    val dislikes: Int,
    val liked: Boolean,
    val disliked: Boolean,
    val isSending: Boolean = false
)

private data class ModalImageViewerState(
    val images: List<PostImage>,
    val index: Int
)

private const val VERIFY_ICON_ID = "VERIFY"
private const val GOLD_ICON_ID = "GOLD"
private const val MAX_TOTAL_COMMENT_FILE_SIZE = 52_428_800L

@Composable
fun PostDetailsModal(
    postId: Int,
    initialPost: FeedPost?,
    accountId: Int?,
    accountName: String?,
    accountAvatar: PostImageAsset?,
    isAdmin: Boolean,
    homeGateway: HomeGateway,
    onClose: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onCommentCountChanged: (postId: Int, commentsCount: Int) -> Unit,
    onPostMutated: (FeedPost) -> Unit = {},
    onPostRemoved: (Int) -> Unit = {},
    onReportPost: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var post by remember(postId) { mutableStateOf(initialPost) }
    var comments by remember(postId) { mutableStateOf<List<PostComment>>(emptyList()) }
    var loadingPost by remember(postId) { mutableStateOf(initialPost == null) }
    var loadingComments by remember(postId) { mutableStateOf(true) }
    var submittingComment by remember(postId) { mutableStateOf(false) }
    var activeReply by remember(postId) { mutableStateOf<PostComment?>(null) }
    var composerText by remember(postId) { mutableStateOf("") }
    var selectedFiles by remember(postId) { mutableStateOf<List<UploadFilePayload>>(emptyList()) }
    var errorText by remember(postId) { mutableStateOf<String?>(null) }
    var transientSuccess by remember(postId) { mutableStateOf<String?>(null) }
    var likeBurstToken by remember(postId) { mutableStateOf(0) }
    var likeBurstOrigin by remember(postId) { mutableStateOf<Offset?>(null) }
    var imageViewerState by remember(postId) { mutableStateOf<ModalImageViewerState?>(null) }
    var editingPost by remember(postId) { mutableStateOf<FeedPost?>(null) }
    var interaction by remember(postId, initialPost?.id) {
        mutableStateOf(
            initialPost?.let {
                ModalPostInteractionState(
                    likes = it.likes,
                    dislikes = it.dislikes,
                    liked = it.liked,
                    disliked = it.disliked
                )
            }
        )
    }

    fun postLink(targetId: Int): String = "https://elemsocial.com/post/$targetId"

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
        if (existingSize + newSize > MAX_TOTAL_COMMENT_FILE_SIZE) {
            Toast.makeText(context, "Общий размер файлов не должен превышать 50MB", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        selectedFiles = selectedFiles + attachments
    }

    fun openProfileFromModal(username: String?) {
        val normalized = username?.trim().orEmpty()
        if (normalized.isBlank()) return
        onClose()
        onOpenProfile(normalized)
    }

    fun refreshComments() {
        loadingComments = true
        scope.launch {
            val result = runCatching { homeGateway.loadComments(postId) }.getOrNull()
            if (result?.isSuccess == true) {
                comments = result.comments
                onCommentCountChanged(postId, result.comments.size)
                errorText = null
            } else {
                errorText = result?.message ?: "Не удалось загрузить комментарии"
            }
            loadingComments = false
        }
    }

    fun openImageViewer(images: List<PostImage>, index: Int) {
        if (images.isEmpty()) return
        imageViewerState = ModalImageViewerState(
            images = images,
            index = index.coerceIn(0, images.lastIndex)
        )
    }

    fun refreshPost() {
        loadingPost = true
        scope.launch {
            val details = runCatching { homeGateway.loadPost(postId) }.getOrNull()
            if (details?.post != null) {
                post = details.post
                interaction = ModalPostInteractionState(
                    likes = details.post.likes,
                    dislikes = details.post.dislikes,
                    liked = details.post.liked,
                    disliked = details.post.disliked
                )
            } else if (details?.isSuccess != true) {
                errorText = details?.message ?: "Не удалось загрузить пост"
            }
            loadingPost = false
        }
    }

    fun toggleLike(burstOrigin: Offset? = null) {
        val currentPost = post ?: return
        val current = interaction ?: return
        if (current.isSending) return

        val next = if (current.liked) {
            current.copy(
                likes = max(0, current.likes - 1),
                liked = false,
                isSending = true
            )
        } else {
            likeBurstToken += 1
            likeBurstOrigin = burstOrigin
            current.copy(
                likes = current.likes + 1,
                liked = true,
                dislikes = if (current.disliked) max(0, current.dislikes - 1) else current.dislikes,
                disliked = false,
                isSending = true
            )
        }

        interaction = next
        scope.launch {
            val ok = runCatching { homeGateway.likePost(currentPost.id) }.getOrDefault(false)
            if (ok) {
                interaction = next.copy(isSending = false)
                errorText = null
            } else {
                interaction = current
                errorText = "Не удалось обновить лайк"
            }
        }
    }

    fun toggleDislike() {
        val currentPost = post ?: return
        val current = interaction ?: return
        if (current.isSending) return

        val next = if (current.disliked) {
            current.copy(
                dislikes = max(0, current.dislikes - 1),
                disliked = false,
                isSending = true
            )
        } else {
            current.copy(
                dislikes = current.dislikes + 1,
                disliked = true,
                likes = if (current.liked) max(0, current.likes - 1) else current.likes,
                liked = false,
                isSending = true
            )
        }

        interaction = next
        scope.launch {
            val ok = runCatching { homeGateway.dislikePost(currentPost.id) }.getOrDefault(false)
            if (ok) {
                interaction = next.copy(isSending = false)
                errorText = null
            } else {
                interaction = current
                errorText = "Не удалось обновить дизлайк"
            }
        }
    }

    fun sendComment() {
        val text = composerText.trim()
        if (text.isBlank() && selectedFiles.isEmpty()) return
        if (submittingComment) return

        submittingComment = true
        scope.launch {
            val result = runCatching {
                homeGateway.addComment(
                    postId = postId,
                    text = text,
                    replyToCommentId = activeReply?.id,
                    files = selectedFiles
                )
            }.getOrNull()

            if (result?.isSuccess == true) {
                composerText = ""
                activeReply = null
                selectedFiles = emptyList()
                transientSuccess = "Комментарий отправлен"
                refreshComments()
                refreshPost()
            } else {
                errorText = result?.message ?: "Не удалось отправить комментарий"
            }

            submittingComment = false
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
    }

    fun deleteComment(comment: PostComment) {
        scope.launch {
            val result = runCatching {
                homeGateway.deleteComment(comment.id)
            }.getOrNull()

            if (result?.isSuccess == true) {
                transientSuccess = "Комментарий удалён"
                refreshComments()
                refreshPost()
            } else {
                errorText = result?.message ?: "Не удалось удалить комментарий"
            }
        }
    }

    fun mutatePost(transform: (FeedPost) -> FeedPost): FeedPost? {
        val current = post ?: return null
        val next = transform(current)
        post = next
        onPostMutated(next)
        return next
    }

    fun handleDeleteOrRestore() {
        val current = post ?: return
        scope.launch {
            val result = if (current.deleted) {
                homeGateway.restorePost(current.id)
            } else {
                homeGateway.deletePost(current.id)
            }

            if (!result.isSuccess) {
                errorText = result.message ?: "Не удалось обновить состояние поста"
                return@launch
            }

            errorText = null
            if (current.deleted) {
                mutatePost { it.copy(deleted = false) }
            } else {
                if (isAdmin) {
                    mutatePost { it.copy(deleted = true) }
                } else {
                    onPostRemoved(current.id)
                    onClose()
                }
            }
        }
    }

    fun handleDeleteForever() {
        val current = post ?: return
        scope.launch {
            val result = homeGateway.deletePostForever(current.id)
            if (result.isSuccess) {
                errorText = null
                onPostRemoved(current.id)
                onClose()
            } else {
                errorText = result.message ?: "Не удалось удалить пост навсегда"
            }
        }
    }

    fun handleArchiveToggle() {
        val current = post ?: return
        scope.launch {
            val result = if (current.archived) {
                homeGateway.removePostFromArchive(current.id)
            } else {
                homeGateway.addPostToArchive(current.id)
            }
            if (result.isSuccess) {
                errorText = null
                onPostRemoved(current.id)
                onClose()
            } else {
                errorText = result.message ?: "Не удалось обновить архив поста"
            }
        }
    }

    fun handleBlockToggle() {
        val current = post ?: return
        val username = current.author?.username?.trim().orEmpty()
        if (username.isBlank()) {
            errorText = "Невозможно определить пользователя"
            return
        }

        scope.launch {
            val shouldBlock = !(current.author?.blocked ?: false)
            val result = if (shouldBlock) {
                homeGateway.blockProfile(username)
            } else {
                homeGateway.unblockProfile(username)
            }

            if (result.isSuccess) {
                errorText = null
                mutatePost {
                    it.copy(author = it.author?.copy(blocked = shouldBlock))
                }
                Toast.makeText(
                    context,
                    if (shouldBlock) "Пользователь заблокирован" else "Пользователь разблокирован",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                errorText = result.message ?: "Не удалось обновить блокировку пользователя"
            }
        }
    }

    LaunchedEffect(postId) {
        refreshPost()
        refreshComments()
    }

    LaunchedEffect(transientSuccess) {
        if (transientSuccess.isNullOrBlank()) return@LaunchedEffect
        delay(1_500)
        transientSuccess = null
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        UIKit.RoutedModal(
            title = "Пост",
            onClose = onClose
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!errorText.isNullOrBlank()) {
                    item {
                        Text(
                            text = errorText.orEmpty(),
                            color = ElementUiPalette.Error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!transientSuccess.isNullOrBlank()) {
                    item {
                        Text(
                            text = transientSuccess.orEmpty(),
                            color = ElementUiPalette.Success,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                item {
                    val readyPost = post
                    if (readyPost == null && loadingPost) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            UIKit.Loader(size = 30)
                        }
                    } else if (readyPost != null) {
                        val authorName = readyPost.author?.name ?: "Удаленный аккаунт"
                        val authorUsername = readyPost.author?.username ?: "unknown"
                        val authorProfileUsername = readyPost.author?.username?.trim()?.takeIf {
                            it.isNotEmpty() && it != "unknown"
                        }
                        val authorAvatar = readyPost.author?.avatar
                        val governItems = buildPostGovernItems(
                            post = readyPost,
                            isAdmin = isAdmin,
                            onReportPost = onReportPost,
                            onEditPost = { editingPost = readyPost },
                            onDeleteOrRestorePost = ::handleDeleteOrRestore,
                            onDeletePostForever = ::handleDeleteForever,
                            onArchiveToggle = ::handleArchiveToggle,
                            onBlockToggle = ::handleBlockToggle
                        )
                        val interactionState = interaction ?: ModalPostInteractionState(
                            likes = readyPost.likes,
                            dislikes = readyPost.dislikes,
                            liked = readyPost.liked,
                            disliked = readyPost.disliked
                        )

                        ElementPostCard(
                            postId = readyPost.id,
                            authorName = authorName,
                            authorUsername = authorUsername,
                            authorAvatar = {
                                AuthorAvatar(
                                    name = authorName,
                                    asset = authorAvatar,
                                    homeGateway = homeGateway
                                )
                            },
                            authorBadge = if (readyPost.author?.icons.hasVerifyBadge() == true ||
                                readyPost.author?.icons.hasGoldBadge() == true
                            ) {
                                {
                                    AuthorBadges(
                                        icons = readyPost.author?.icons,
                                        size = 16.dp
                                    )
                                }
                            } else {
                                null
                            },
                            onAuthorClick = authorProfileUsername?.let { username ->
                                { openProfileFromModal(username) }
                            },
                            dateText = formatTimeAge(readyPost.createDate),
                            text = readyPost.text,
                            archived = readyPost.archived,
                            deleted = readyPost.deleted,
                            edited = readyPost.editedAt != null,
                            likes = interactionState.likes,
                            dislikes = interactionState.dislikes,
                            comments = comments.size,
                            liked = interactionState.liked,
                            disliked = interactionState.disliked,
                            interactionsEnabled = !interactionState.isSending,
                            showCommentButton = false,
                            shareLink = postLink(readyPost.id),
                            governItems = governItems,
                            likeBurstTrigger = likeBurstToken,
                            likeBurstOrigin = likeBurstOrigin,
                            onLike = { toggleLike() },
                            onDislike = ::toggleDislike,
                            onComment = {},
                            onCopyLink = {
                                clipboard.setText(AnnotatedString(postLink(readyPost.id)))
                                Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
                            },
                            onDoubleTapLike = { origin ->
                                if (!interactionState.liked) toggleLike(origin)
                            }
                        ) {
                            readyPost.poll?.let { poll ->
                                ElementPostPoll(
                                    postId = readyPost.id,
                                    poll = poll,
                                    onVote = { optionIds -> homeGateway.votePostPoll(readyPost.id, optionIds) }
                                )
                            }
                            if (readyPost.content.songs.isNotEmpty()) {
                                PostMusicTracksBlock(
                                    homeGateway = homeGateway,
                                    songs = readyPost.content.songs
                                )
                            }
                            if (readyPost.content.images.isNotEmpty()) {
                                PostImagesBlock(
                                    images = readyPost.content.images,
                                    homeGateway = homeGateway,
                                    onOpenImage = { imageIndex ->
                                        openImageViewer(readyPost.content.images, imageIndex)
                                    },
                                    onDoubleTap = { origin ->
                                        if (!interactionState.liked) {
                                            toggleLike(origin)
                                        }
                                    }
                                )
                            }
                            if (readyPost.content.videos.isNotEmpty()) {
                                ElementPostVideosBlock(
                                    videos = readyPost.content.videos,
                                    resolveCachedFile = { cacheKey ->
                                        homeGateway.resolveCachedFile(cacheKey)
                                    },
                                    loadPreviewBytes = { asset ->
                                        homeGateway.loadImageBytes(asset)
                                    },
                                    loadVideoFile = { video, onProgress, isCancelled ->
                                        homeGateway.loadVideoFile(video, onProgress, isCancelled)
                                    },
                                    onDoubleTapLike = { origin ->
                                        if (!interactionState.liked) {
                                            toggleLike(origin)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    UIKit.Block(contentPadding = 7.dp) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(
                                text = "Комментарии",
                                color = ElementUiPalette.TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (activeReply != null) {
                                val replyAuthor = activeReply?.author?.name ?: "Пользователь"
                                val replyAura = parseAura(activeReply?.author?.avatar?.aura)
                                val replyAuthorIcons = activeReply?.author?.icons
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(replyAura.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(1.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Ответ для $replyAuthor",
                                                    color = replyAura,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                AuthorBadges(
                                                    icons = replyAuthorIcons,
                                                    size = 13.dp
                                                )
                                            }
                                            Text(
                                                text = activeReply?.text.orEmpty().ifBlank { "Без текста" },
                                                color = ElementUiPalette.TextLite,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .clickable { activeReply = null },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Снять ответ",
                                                tint = replyAura,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            ElementPostComposer(
                                text = composerText,
                                isSending = submittingComment,
                                avatar = {
                                    AuthorAvatar(
                                        name = accountName ?: "Вы",
                                        asset = accountAvatar,
                                        homeGateway = homeGateway,
                                        size = 28.dp
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
                                onMusicClick = {
                                    Toast.makeText(context, "Музыку в комментарии подключим следующим шагом", Toast.LENGTH_SHORT).show()
                                },
                                onPollClick = {
                                    Toast.makeText(context, "Опросы в комментариях подключим следующим шагом", Toast.LENGTH_SHORT).show()
                                },
                                showEmojiButton = false,
                                showMusicButton = false,
                                showPollButton = false,
                                showAuthorAvatar = false,
                                onSend = ::sendComment,
                                framed = false,
                                placeholder = "Комментировать...",
                                inputTextFontSize = 14.sp,
                                inputTextLineHeight = 18.sp,
                                inputPlaceholderFontSize = 14.sp,
                                inputMinHeight = 34.dp,
                                inputHorizontalPadding = 10.dp,
                                inputVerticalPadding = 6.dp,
                                sendButtonHeight = 28.dp,
                                sendButtonMinWidth = 98.dp,
                                sendButtonTextFontSize = 14.sp,
                                actionButtonSize = 28.dp,
                                actionIconSize = 15.dp,
                                attachments = selectedFiles,
                                onRemoveAttachment = ::removeAttachment
                            )
                        }
                    }
                }

                when {
                    loadingComments -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                UIKit.Loader(size = 26)
                            }
                        }
                    }

                    comments.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Комментариев пока нет",
                                    color = ElementUiPalette.TextLite,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    else -> {
                        items(comments, key = { it.id }) { comment ->
                            CommentCard(
                                comment = comment,
                                accountId = accountId,
                                homeGateway = homeGateway,
                                onOpenProfile = ::openProfileFromModal,
                                onOpenImageViewer = ::openImageViewer,
                                onReply = { activeReply = comment },
                                onDelete = { deleteComment(comment) },
                                onReport = {
                                    Toast.makeText(context, "Жалобы на комментарии подключим следующим шагом", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        imageViewerState?.let { state ->
            ElementImageViewerOverlay(
                images = state.images,
                initialIndex = state.index,
                onDismiss = { imageViewerState = null },
                loadImageBytes = { asset -> homeGateway.loadImageBytes(asset) }
            )
        }

        editingPost?.let { currentPost ->
            ElementEditPostModal(
                post = currentPost,
                onClose = { editingPost = null },
                onSaveRequest = { text ->
                    homeGateway.editPost(currentPost.id, text)
                },
                onSaved = { updated ->
                    val nextPost = (post ?: currentPost).copy(
                        text = updated.text,
                        editedAt = updated.editedAt
                    )
                    post = nextPost
                    onPostMutated(nextPost)
                }
            )
        }
    }
}

@Composable
private fun CommentCard(
    comment: PostComment,
    accountId: Int?,
    homeGateway: HomeGateway,
    onOpenProfile: (String) -> Unit,
    onOpenImageViewer: (List<PostImage>, Int) -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit
) {
    var menuOpen by remember(comment.id) { mutableStateOf(false) }
    var menuAnchor by remember(comment.id) { mutableStateOf<IntRect?>(null) }
    var showFullText by remember(comment.id) { mutableStateOf(false) }
    val ownComment = accountId != null && comment.author?.id == accountId
    val authorName = comment.author?.name ?: "Удаленный аккаунт"
    val authorProfileUsername = comment.author?.username?.trim()?.takeIf {
        it.isNotEmpty() && it != "unknown"
    }
    val authorAvatar = comment.author?.avatar
    val authorIcons = comment.author?.icons
    val commentText = remember(comment.id, comment.text) { comment.text.trimEnd() }
    val longText = commentText.length > 700
    val menuItems = buildList {
        add(
            ElementContextMenuItem(
                title = "Ответить",
                icon = Icons.AutoMirrored.Filled.Reply,
                onClick = onReply
            )
        )
        if (ownComment) {
            add(
                ElementContextMenuItem(
                    title = "Удалить",
                    icon = Icons.Default.Delete,
                    color = ElementUiPalette.Error,
                    onClick = onDelete
                )
            )
        }
    }

    UIKit.Block(
        modifier = Modifier.alpha(if (comment.deleted) 0.56f else 1f),
        contentPadding = 7.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuthorAvatar(
                    name = authorName,
                    asset = authorAvatar,
                    homeGateway = homeGateway,
                    size = 34.dp,
                    modifier = if (authorProfileUsername != null) {
                        Modifier.clickable { onOpenProfile(authorProfileUsername) }
                    } else {
                        Modifier
                    }
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                        .let { base ->
                            if (authorProfileUsername != null) {
                                base.clickable { onOpenProfile(authorProfileUsername) }
                            } else {
                                base
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = authorName,
                            color = ElementUiPalette.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        AuthorBadges(
                            icons = authorIcons,
                            size = 16.dp
                        )
                    }
                    Text(
                        text = comment.date
                            ?.takeIf { it.isNotBlank() }
                            ?.let { formatTimeAge(it) }
                            ?: "только что",
                        color = ElementUiPalette.TextLite,
                        fontSize = 12.sp
                    )
                }

                Box {
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                val rect = coordinates.boundsInWindow()
                                menuAnchor = IntRect(
                                    left = rect.left.toInt(),
                                    top = rect.top.toInt(),
                                    right = rect.right.toInt(),
                                    bottom = rect.bottom.toInt()
                                )
                            }
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { menuOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_element_dots),
                            contentDescription = null,
                            tint = ElementUiPalette.InteractionText,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    UIKit.ContextMenu(
                        expanded = menuOpen,
                        anchorBounds = menuAnchor,
                        items = menuItems,
                        onDismissRequest = { menuOpen = false },
                        minWidth = 200.dp
                    )
                }
            }

            val reply = comment.content.reply
            if (reply != null) {
                val replyName = reply.author?.name ?: "Пользователь"
                val replyAura = parseAura(reply.author?.avatar?.aura)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(replyAura.copy(alpha = 0.2f))
                        .padding(horizontal = 7.dp, vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 22.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AuthorAvatar(
                                name = replyName,
                                asset = reply.author?.avatar,
                                homeGateway = homeGateway,
                                size = 30.dp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = replyName,
                                    color = ElementUiPalette.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                AuthorBadges(
                                    icons = reply.author?.icons,
                                    size = 14.dp
                                )
                            }
                        }
                        if (reply.text.isNotBlank()) {
                            Text(
                                text = reply.text,
                                color = ElementUiPalette.TextSecondary,
                                fontSize = 14.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = replyAura.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                    )
                }
            }

            if (commentText.isNotBlank()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ElementLinkText(
                        text = commentText,
                        style = androidx.compose.ui.text.TextStyle(
                            color = ElementUiPalette.TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 21.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            ),
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        ),
                        modifier = if (longText && !showFullText) {
                            Modifier.heightIn(max = 300.dp)
                        } else {
                            Modifier
                        }
                    )
                    if (longText && !showFullText) {
                        Text(
                            text = "Полный текст",
                            color = ElementUiPalette.Accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { showFullText = true }
                        )
                    }
                }
            }

            if (comment.content.filesCount > 0) {
                Text(
                    text = "Вложений: ${comment.content.filesCount}",
                    color = ElementUiPalette.TextLite,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElementUiPalette.BlockSoft)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (comment.content.images.isNotEmpty()) {
                PostImagesBlock(
                    images = comment.content.images,
                    homeGateway = homeGateway,
                    onOpenImage = { imageIndex ->
                        onOpenImageViewer(comment.content.images, imageIndex)
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthorAvatar(
    name: String,
    asset: PostImageAsset?,
    homeGateway: HomeGateway,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val bitmap by rememberImageBitmap(
        asset = asset,
        homeGateway = homeGateway
    )
    val base = parseAura(asset?.aura)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, ElementUiPalette.Block, CircleShape)
            .background(Brush.linearGradient(listOf(base.copy(alpha = 0.95f), base.copy(alpha = 0.7f)))),
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
                fontSize = (size.value * 0.36f).sp
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
            .height(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ElementUiPalette.Block),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        PostImageTile(
            image = first,
            homeGateway = homeGateway,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentScale = ContentScale.Crop,
            onDoubleTap = onDoubleTap,
            onClick = { onOpenImage(0) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            PostImageTile(
                image = second,
                homeGateway = homeGateway,
                modifier = if (third == null) Modifier.fillMaxSize() else Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 32.sp,
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
            }
            Image(
                bitmap = bitmap!!,
                contentDescription = image.fileName ?: "comment-image",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            UIKit.Loader(size = 20)
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
    if (raw.isNullOrBlank()) return Color(0xFF9183B8)
    val regex = Regex("""rgb\((\d+),\s*(\d+),\s*(\d+)\)""")
    val match = regex.find(raw) ?: return Color(0xFF9183B8)
    val r = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return Color(0xFF9183B8)
    val g = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return Color(0xFF9183B8)
    val b = match.groupValues.getOrNull(3)?.toIntOrNull() ?: return Color(0xFF9183B8)
    return Color(r, g, b)
}

private fun List<String>?.hasVerifyBadge(): Boolean {
    return this?.any { it.equals(VERIFY_ICON_ID, ignoreCase = true) } == true
}

private fun List<String>?.hasGoldBadge(): Boolean {
    return this?.any { it.equals(GOLD_ICON_ID, ignoreCase = true) } == true
}

@Composable
private fun AuthorBadges(
    icons: List<String>?,
    size: androidx.compose.ui.unit.Dp
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
