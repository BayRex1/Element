package elemsocial.com.ui.pack.components.posts

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import elemsocial.com.core.settings.AppSettingsStore
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostVideo
import java.io.File
import java.net.URLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MaxInlineVideoHeightDp = 300
private const val OverlayHideDelayMillis = 2_000L
private val VideoOverlayBorderColor = Color.White.copy(alpha = 0.18f)

private fun videoOverlayBrush(): Brush {
    return Brush.verticalGradient(
        listOf(
            Color(0xE61A1A1F),
            Color(0xD9060608)
        )
    )
}

private fun videoCenterBrush(): Brush {
    return Brush.radialGradient(
        listOf(
            Color(0xD91C1C22),
            Color(0xF0060608)
        )
    )
}

private val VideoRotateIcon: ImageVector = ImageVector.Builder(
    name = "VideoRotateIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(7f, 2f)
        lineTo(3f, 6f)
        lineTo(7f, 10f)
        verticalLineTo(7f)
        horizontalLineTo(14f)
        curveTo(17.31f, 7f, 20f, 9.69f, 20f, 13f)
        curveTo(20f, 15.04f, 18.98f, 16.84f, 17.43f, 17.92f)
        lineTo(18.85f, 19.34f)
        curveTo(20.75f, 17.89f, 22f, 15.61f, 22f, 13f)
        curveTo(22f, 8.58f, 18.42f, 5f, 14f, 5f)
        horizontalLineTo(7f)
        verticalLineTo(2f)
        close()

        moveTo(5f, 12f)
        horizontalLineTo(15f)
        curveTo(16.1f, 12f, 17f, 12.9f, 17f, 14f)
        verticalLineTo(20f)
        curveTo(17f, 21.1f, 16.1f, 22f, 15f, 22f)
        horizontalLineTo(5f)
        curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f)
        verticalLineTo(14f)
        curveTo(3f, 12.9f, 3.9f, 12f, 5f, 12f)
        close()

        moveTo(5f, 14f)
        verticalLineTo(20f)
        horizontalLineTo(15f)
        verticalLineTo(14f)
        horizontalLineTo(5f)
        close()
    }
}.build()

@Composable
fun ElementPostVideosBlock(
    videos: List<PostVideo>,
    resolveCachedFile: suspend (String) -> File?,
    loadPreviewBytes: suspend (PostImageAsset) -> ByteArray?,
    loadVideoFile: suspend (
        video: PostVideo,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
        isCancelled: () -> Boolean
    ) -> File?,
    onDoubleTapLike: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        videos.forEach { video ->
            ElementPostVideoPlayer(
                video = video,
                resolveCachedFile = resolveCachedFile,
                loadPreviewBytes = loadPreviewBytes,
                loadVideoFile = loadVideoFile,
                onDoubleTapLike = onDoubleTapLike
            )
        }
    }
}

@Composable
private fun ElementPostVideoPlayer(
    video: PostVideo,
    resolveCachedFile: suspend (String) -> File?,
    loadPreviewBytes: suspend (PostImageAsset) -> ByteArray?,
    loadVideoFile: suspend (
        video: PostVideo,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
        isCancelled: () -> Boolean
    ) -> File?,
    onDoubleTapLike: ((Offset) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember(context) { AppSettingsStore(context) }
    val previewBitmap by rememberAssetBitmap(
        asset = video.preview,
        loadImageBytes = loadPreviewBytes
    )

    var sourceFile by remember(video.cacheKey) { mutableStateOf<File?>(null) }
    var isInitialized by remember(video.cacheKey) { mutableStateOf(false) }
    var isDownloading by remember(video.cacheKey) { mutableStateOf(false) }
    var isDownloaded by remember(video.cacheKey) { mutableStateOf(false) }
    var downloadedBytes by remember(video.cacheKey) { mutableLongStateOf(0L) }
    var totalBytes by remember(video.cacheKey) { mutableLongStateOf(video.fileSize ?: 0L) }
    var controlsVisible by remember(video.cacheKey) { mutableStateOf(false) }
    var settingsOpen by remember(video.cacheKey) { mutableStateOf(false) }
    var isFullscreen by remember(video.cacheKey) { mutableStateOf(false) }
    var currentPositionMs by remember(video.cacheKey) { mutableLongStateOf(0L) }
    var durationMs by remember(video.cacheKey) { mutableLongStateOf(0L) }
    var overlayPulse by remember(video.cacheKey) { mutableLongStateOf(0L) }
    var downloadJob by remember(video.cacheKey) { mutableStateOf<Job?>(null) }
    var cancelDownloadRequested by remember(video.cacheKey) { mutableStateOf(false) }
    var autoplayMuted by remember(video.cacheKey) { mutableStateOf(false) }
    var fullscreenLandscape by remember(video.cacheKey) {
        mutableStateOf(shouldUseLandscapeFullscreen(video.info?.width, video.info?.height))
    }
    var volume by remember(video.cacheKey) {
        mutableFloatStateOf(settingsStore.getVideoPlayerVolume())
    }

    val autoVideoDownloadEnabled = settingsStore.getAutoVideoDownloadEnabled()
    val videoAutoplayEnabled = settingsStore.getVideoAutoplayEnabled()
    val videoTitle = remember(video.fileName, video.file) {
        video.fileName?.takeIf { it.isNotBlank() } ?: video.file
    }

    val player = remember(video.cacheKey) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
            this.volume = volume
        }
    }

    var isPlaying by remember(video.cacheKey) { mutableStateOf(false) }

    fun pokeControls(forceVisible: Boolean = true) {
        if (forceVisible) {
            controlsVisible = true
        }
        overlayPulse += 1L
    }

    fun startDownload() {
        if (isDownloading || downloadJob?.isActive == true) return

        isInitialized = true
        isDownloading = true
        isDownloaded = false
        cancelDownloadRequested = false
        downloadedBytes = 0L
        totalBytes = maxOf(totalBytes, video.fileSize ?: 0L)

        downloadJob = scope.launch {
            val result = runCatching {
                loadVideoFile(
                    video,
                    { downloaded, total ->
                        downloadedBytes = downloaded
                        totalBytes = maxOf(totalBytes, total)
                    },
                    { cancelDownloadRequested }
                )
            }.getOrNull()

            if (result != null) {
                sourceFile = result
                isDownloaded = true
                downloadedBytes = result.length()
                totalBytes = maxOf(totalBytes, result.length())
            } else if (!cancelDownloadRequested) {
                Toast.makeText(context, "Не удалось загрузить видео", Toast.LENGTH_SHORT).show()
            }

            isDownloading = false
            cancelDownloadRequested = false
            downloadJob = null
        }
    }

    fun togglePlayback() {
        val readyFile = sourceFile ?: return
        if (!readyFile.exists()) return

        autoplayMuted = false
        player.volume = volume

        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        pokeControls()
    }

    fun seekToFraction(fraction: Float) {
        val duration = durationMs.takeIf { it > 0L } ?: return
        val target = (duration * fraction.coerceIn(0f, 1f)).toLong()
        player.seekTo(target)
        currentPositionMs = target
        pokeControls()
    }

    fun changeVolume(next: Float) {
        autoplayMuted = false
        volume = next.coerceIn(0f, 1f)
        player.volume = volume
        settingsStore.saveVideoPlayerVolume(volume)
        pokeControls()
    }

    fun saveVideoToDevice() {
        val file = sourceFile ?: return
        scope.launch {
            val success = runCatching {
                persistVideoToGallery(
                    context = context,
                    source = file,
                    displayName = video.fileName ?: video.file,
                    mimeType = guessVideoMimeType(video.fileName ?: video.file)
                )
            }.getOrDefault(false)

            Toast.makeText(
                context,
                if (success) "Видео сохранено" else "Не удалось сохранить видео",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun toggleFullscreenOrientation() {
        fullscreenLandscape = !fullscreenLandscape
        controlsVisible = true
        settingsOpen = false
        pokeControls(forceVisible = false)
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val currentDuration = player.duration
                if (currentDuration > 0L) {
                    durationMs = currentDuration
                }

                if (playbackState == Player.STATE_ENDED) {
                    player.pause()
                    player.seekTo(0L)
                    currentPositionMs = 0L
                    isPlaying = false
                }
            }
        }

        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(player) {
        while (true) {
            currentPositionMs = player.currentPosition.coerceAtLeast(0L)
            val currentDuration = player.duration
            if (currentDuration > 0L) {
                durationMs = currentDuration
            }
            delay(250L)
        }
    }

    LaunchedEffect(isFullscreen, video.cacheKey) {
        if (!isFullscreen) return@LaunchedEffect

        val preferredLandscape = shouldUseLandscapeFullscreen(video.info?.width, video.info?.height)
        if (fullscreenLandscape != preferredLandscape) {
            delay(80L)
            fullscreenLandscape = preferredLandscape
        }
    }

    LaunchedEffect(volume, autoplayMuted) {
        player.volume = if (autoplayMuted) 0f else volume
    }

    LaunchedEffect(sourceFile?.absolutePath, videoAutoplayEnabled) {
        val file = sourceFile ?: return@LaunchedEffect
        val mediaItem = MediaItem.Builder()
            .setUri(file.toUri())
            .setMimeType(guessVideoMimeType(video.fileName ?: video.file))
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.pause()
        player.seekTo(0L)
        currentPositionMs = 0L
        durationMs = 0L

        if (videoAutoplayEnabled) {
            autoplayMuted = true
            player.volume = 0f
            player.play()
            controlsVisible = false
            settingsOpen = false
            pokeControls(forceVisible = false)
        } else {
            autoplayMuted = false
            player.volume = volume
        }
    }

    LaunchedEffect(video.cacheKey, autoVideoDownloadEnabled) {
        val cached = resolveCachedFile(video.cacheKey)
        if (cached != null) {
            sourceFile = cached
            isInitialized = true
            isDownloaded = true
            downloadedBytes = cached.length()
            totalBytes = maxOf(totalBytes, cached.length())
        } else if (autoVideoDownloadEnabled) {
            startDownload()
        }
    }

    LaunchedEffect(controlsVisible, settingsOpen, overlayPulse) {
        if (!controlsVisible || settingsOpen) return@LaunchedEffect

        delay(OverlayHideDelayMillis)
        if (!settingsOpen) {
            controlsVisible = false
        }
    }

    val inlineShape = RoundedCornerShape(18.dp)

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val inlineHeight = remember(maxWidth, video.info?.width, video.info?.height) {
            calculateVideoHeight(
                maxWidth = maxWidth,
                width = video.info?.width,
                height = video.info?.height
            )
        }

        VideoPlayerFrame(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = MaxInlineVideoHeightDp.dp)
                .height(inlineHeight)
                .clip(inlineShape),
            attachPlayer = !isFullscreen,
            exoPlayer = player,
            previewBitmap = previewBitmap,
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            isInitialized = isInitialized,
            isPlaying = isPlaying,
            controlsVisible = controlsVisible,
            settingsOpen = settingsOpen,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            volume = volume,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            onDoubleTapLike = onDoubleTapLike,
            onBackgroundTap = {
                if (isDownloaded) {
                    controlsVisible = !controlsVisible
                    if (controlsVisible) {
                        pokeControls(forceVisible = false)
                    } else {
                        settingsOpen = false
                    }
                }
            },
            onLoaderClick = {
                if (isDownloading) {
                    cancelDownloadRequested = true
                    downloadJob?.cancel()
                } else {
                    startDownload()
                }
            },
            onPlayClick = ::togglePlayback,
            onSeek = ::seekToFraction,
            onVolumeChange = ::changeVolume,
            onDownload = ::saveVideoToDevice,
            onSettingsToggle = {
                settingsOpen = !settingsOpen
                controlsVisible = true
                pokeControls(forceVisible = false)
            },
            onRotateFullscreen = ::toggleFullscreenOrientation,
            onFullscreenToggle = {
                autoplayMuted = false
                player.volume = volume
                fullscreenLandscape = false
                isFullscreen = true
                controlsVisible = true
                pokeControls(forceVisible = false)
            },
            fullscreenLandscape = fullscreenLandscape
        )
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            VideoPlayerFrame(
                modifier = Modifier.fillMaxSize(),
                attachPlayer = true,
                exoPlayer = player,
                previewBitmap = previewBitmap,
                isDownloaded = isDownloaded,
                isDownloading = isDownloading,
                isInitialized = isInitialized,
                isPlaying = isPlaying,
                controlsVisible = controlsVisible,
                settingsOpen = settingsOpen,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                volume = volume,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                headerTitle = videoTitle,
                fullscreen = true,
                onDoubleTapLike = onDoubleTapLike,
                onBackgroundTap = {
                    if (isDownloaded) {
                        controlsVisible = !controlsVisible
                        if (controlsVisible) {
                            pokeControls(forceVisible = false)
                        } else {
                            settingsOpen = false
                        }
                    }
                },
                onLoaderClick = {
                    if (isDownloading) {
                        cancelDownloadRequested = true
                        downloadJob?.cancel()
                    } else {
                        startDownload()
                    }
                },
                onPlayClick = ::togglePlayback,
                onSeek = ::seekToFraction,
                onVolumeChange = ::changeVolume,
                onDownload = ::saveVideoToDevice,
                onSettingsToggle = {
                    settingsOpen = !settingsOpen
                    controlsVisible = true
                    pokeControls(forceVisible = false)
                },
                onRotateFullscreen = ::toggleFullscreenOrientation,
                onFullscreenToggle = {
                    autoplayMuted = false
                    player.volume = volume
                    isFullscreen = false
                    controlsVisible = true
                    settingsOpen = false
                    pokeControls(forceVisible = false)
                },
                fullscreenLandscape = fullscreenLandscape
            )
        }
    }
}

@Composable
private fun VideoPlayerFrame(
    modifier: Modifier,
    attachPlayer: Boolean,
    exoPlayer: ExoPlayer,
    previewBitmap: ImageBitmap?,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isInitialized: Boolean,
    isPlaying: Boolean,
    controlsVisible: Boolean,
    settingsOpen: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    volume: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    headerTitle: String? = null,
    onDoubleTapLike: ((Offset) -> Unit)? = null,
    onBackgroundTap: () -> Unit,
    onLoaderClick: () -> Unit,
    onPlayClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDownload: () -> Unit,
    onSettingsToggle: () -> Unit,
    onRotateFullscreen: () -> Unit,
    onFullscreenToggle: () -> Unit,
    fullscreenLandscape: Boolean = false,
    fullscreen: Boolean = false
) {
    val mediaRotation by animateFloatAsState(
        targetValue = if (fullscreen && fullscreenLandscape) 90f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "video-fullscreen-media-rotation"
    )

    BoxWithConstraints(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(onBackgroundTap, onDoubleTapLike) {
                detectTapGestures(
                    onTap = { onBackgroundTap() },
                    onDoubleTap = { offset -> onDoubleTapLike?.invoke(offset) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val mediaWidth = if (fullscreen && fullscreenLandscape) maxHeight else maxWidth
        val mediaHeight = if (fullscreen && fullscreenLandscape) maxWidth else maxHeight

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = mediaWidth, height = mediaHeight)
                .graphicsLayer { rotationZ = mediaRotation },
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null && !isDownloaded) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = "video-preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (attachPlayer && isDownloaded) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShutterBackgroundColor(android.graphics.Color.BLACK)
                            player = exoPlayer
                        }
                    },
                    update = { view ->
                        view.player = exoPlayer
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (!isDownloaded) {
            DownloadLoader(
                isDownloading = isDownloading,
                isInitialized = isInitialized,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                onClick = onLoaderClick
            )
        }

        AnimatedVisibility(
            visible = !fullscreen && !isPlaying,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = fadeOut(animationSpec = tween(durationMillis = 180)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.dp)
        ) {
            VideoInfoChip()
        }

        AnimatedVisibility(
            visible = fullscreen && controlsVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 220),
                    initialOffsetY = { -it / 3 }
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 240)) +
                slideOutVertically(
                    animationSpec = tween(durationMillis = 240),
                    targetOffsetY = { -it / 4 }
                ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 6.dp)
        ) {
            MediaOverlayHeader(
                title = headerTitle ?: "Видео",
                onDismiss = onFullscreenToggle,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        AnimatedVisibility(
            visible = isDownloaded && (!isPlaying || controlsVisible),
            enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                scaleIn(
                    animationSpec = tween(durationMillis = 180),
                    initialScale = 0.8f
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 180)) +
                scaleOut(
                    animationSpec = tween(durationMillis = 180),
                    targetScale = 0.82f
                ),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = if (fullscreen) (-22).dp else 0.dp)
        ) {
            VideoCenterButton(
                isPlaying = isPlaying,
                onClick = onPlayClick
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && isDownloaded,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 220),
                    initialOffsetY = { it / 3 }
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 220)) +
                slideOutVertically(
                    animationSpec = tween(durationMillis = 220),
                    targetOffsetY = { it / 4 }
                ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fullscreen) Modifier.navigationBarsPadding() else Modifier)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.28f),
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            ) {

                VideoControls(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            start = 10.dp,
                            end = 10.dp,
                            top = if (fullscreen) 14.dp else 10.dp,
                            bottom = if (fullscreen) 10.dp else 6.dp
                        ),
                    isPlaying = isPlaying,
                    settingsOpen = settingsOpen,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    volume = volume,
                    onPlayClick = onPlayClick,
                    onSeek = onSeek,
                    onVolumeChange = onVolumeChange,
                    onDownload = onDownload,
                    onSettingsToggle = onSettingsToggle,
                    onRotateFullscreen = onRotateFullscreen,
                    onFullscreenToggle = onFullscreenToggle,
                    fullscreenLandscape = fullscreenLandscape,
                    fullscreen = fullscreen
                )
            }
        }
    }
}

@Composable
private fun DownloadLoader(
    isDownloading: Boolean,
    isInitialized: Boolean,
    downloadedBytes: Long,
    totalBytes: Long,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val containerBrush = videoOverlayBrush()
    val infiniteTransition = rememberInfiniteTransition(label = "video-loader-rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "video-loader-rotation-value"
    )
    val progress = if (totalBytes > 0L) {
        (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(55.dp)
                .background(containerBrush, CircleShape)
                .border(1.dp, VideoOverlayBorderColor, CircleShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .graphicsLayer { rotationZ = rotation },
                    color = Color.White,
                    strokeWidth = 2.dp,
                    trackColor = Color.Transparent
                )
            }

            Icon(
                imageVector = if (isInitialized && isDownloading) {
                    Icons.Default.Close
                } else {
                    Icons.Default.Download
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(33.dp)
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(containerBrush)
                .border(1.dp, VideoOverlayBorderColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isInitialized) {
                Text(
                    text = Formatter.formatShortFileSize(context, downloadedBytes),
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Text(
                text = Formatter.formatShortFileSize(context, totalBytes.coerceAtLeast(0L)),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun VideoInfoChip(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(videoOverlayBrush())
            .border(1.dp, VideoOverlayBorderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = "Видео",
            color = Color.White,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun VideoCenterButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(94.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(18.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(videoCenterBrush())
                .border(1.dp, VideoOverlayBorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(durationMillis = 180)) +
                        scaleIn(
                            animationSpec = tween(durationMillis = 180),
                            initialScale = 0.55f
                        )).togetherWith(
                        fadeOut(animationSpec = tween(durationMillis = 180)) +
                            scaleOut(
                                animationSpec = tween(durationMillis = 180),
                                targetScale = 0.55f
                            )
                    )
                },
                label = "video-center-button"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Пауза" else "Играть",
                    tint = Color.White.copy(alpha = 0.98f),
                    modifier = Modifier.size(if (playing) 34.dp else 46.dp)
                )
            }
        }
    }
}

@Composable
private fun VideoVolumeRow(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 12.dp else 14.dp))
            .background(videoOverlayBrush())
            .border(
                1.dp,
                VideoOverlayBorderColor,
                RoundedCornerShape(if (compact) 12.dp else 14.dp)
            )
            .padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 7.dp else 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.VolumeDown,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(if (compact) 18.dp else 20.dp)
        )
        VideoSlider(
            modifier = Modifier.weight(1f),
            value = volume,
            onValueChange = onVolumeChange
        )
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(if (compact) 18.dp else 20.dp)
        )
    }
}

@Composable
private fun VideoControls(
    modifier: Modifier,
    isPlaying: Boolean,
    settingsOpen: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    volume: Float,
    onPlayClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDownload: () -> Unit,
    onSettingsToggle: () -> Unit,
    onRotateFullscreen: () -> Unit,
    onFullscreenToggle: () -> Unit,
    fullscreenLandscape: Boolean,
    fullscreen: Boolean
) {
    BoxWithConstraints(modifier = modifier) {
        val compactLayout = maxWidth < if (fullscreen) 420.dp else 360.dp
        val controlGap = if (compactLayout) 8.dp else 10.dp
        val buttonSize = if (compactLayout) 32.dp else 34.dp
        val playIconSize = if (compactLayout) 24.dp else 26.dp
        val actionIconSize = if (compactLayout) 20.dp else 22.dp
        val mainControlsHeight = if (compactLayout) 36.dp else 38.dp
        val volumeOverlayOffset = mainControlsHeight + if (compactLayout) 6.dp else 8.dp
        val sliderValue = if (durationMs > 0L) {
            currentPositionMs.toFloat() / durationMs.toFloat()
        } else {
            0f
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mainControlsHeight)
        ) {
            AnimatedVisibility(
                visible = settingsOpen,
                enter = fadeIn(animationSpec = tween(durationMillis = 160)) +
                    slideInVertically(
                        animationSpec = tween(durationMillis = 200),
                        initialOffsetY = { it / 2 }
                    ),
                exit = fadeOut(animationSpec = tween(durationMillis = 160)) +
                    slideOutVertically(
                        animationSpec = tween(durationMillis = 180),
                        targetOffsetY = { it / 2 }
                    ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = -volumeOverlayOffset)
            ) {
                VideoVolumeRow(
                    modifier = Modifier.fillMaxWidth(),
                    volume = volume,
                    onVolumeChange = onVolumeChange,
                    compact = compactLayout
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(controlGap)
            ) {
                IconButton(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    onClick = onPlayClick,
                    contentDescription = if (isPlaying) "Пауза" else "Играть",
                    buttonSize = buttonSize,
                    iconSize = playIconSize
                )

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp)
                ) {
                    DurationText(
                        text = formatDuration(currentPositionMs),
                        modifier = Modifier.widthIn(min = if (compactLayout) 34.dp else 42.dp)
                    )
                    VideoSlider(
                        modifier = Modifier.weight(1f),
                        value = sliderValue,
                        onValueChange = onSeek
                    )
                    DurationText(
                        text = formatDuration(durationMs),
                        modifier = Modifier.widthIn(min = if (compactLayout) 34.dp else 42.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(controlGap)
                ) {
                    IconButton(
                        icon = Icons.Default.Download,
                        onClick = onDownload,
                        contentDescription = "Скачать",
                        buttonSize = buttonSize,
                        iconSize = actionIconSize
                    )
                    IconButton(
                        icon = Icons.Default.VolumeUp,
                        onClick = onSettingsToggle,
                        contentDescription = if (settingsOpen) {
                            "Скрыть громкость"
                        } else {
                            "Громкость"
                        },
                        buttonSize = buttonSize,
                        iconSize = actionIconSize
                    )
                    if (fullscreen) {
                        IconButton(
                            icon = VideoRotateIcon,
                            onClick = onRotateFullscreen,
                            contentDescription = if (fullscreenLandscape) {
                                "Повернуть вертикально"
                            } else {
                                "Повернуть горизонтально"
                            },
                            buttonSize = buttonSize,
                            iconSize = actionIconSize
                        )
                    }
                    IconButton(
                        icon = if (fullscreen) {
                            Icons.Default.FullscreenExit
                        } else {
                            Icons.Default.Fullscreen
                        },
                        onClick = onFullscreenToggle,
                        contentDescription = if (fullscreen) "Свернуть" else "Полный экран",
                        buttonSize = buttonSize,
                        iconSize = actionIconSize
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = Color.White.copy(alpha = 0.35f),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun IconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    buttonSize: Dp = 30.dp,
    iconSize: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(buttonSize)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun VideoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    val normalized = value.coerceIn(0f, 1f)

    fun updateWithX(x: Float) {
        val fraction = if (widthPx <= 0f) {
            0f
        } else {
            (x / widthPx).coerceIn(0f, 1f)
        }
        onValueChange(fraction)
    }

    Box(
        modifier = modifier
            .height(10.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    updateWithX(offset.x)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> updateWithX(offset.x) },
                    onDrag = { change, _ ->
                        val targetX = change.position.x
                        updateWithX(targetX)
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .align(Alignment.Center)
        ) {
            val stroke = size.height
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width * normalized, size.height / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun rememberAssetBitmap(
    asset: PostImageAsset?,
    loadImageBytes: suspend (PostImageAsset) -> ByteArray?
) = produceState<ImageBitmap?>(initialValue = null, key1 = asset?.cacheKey) {
    val source = asset ?: return@produceState
    val bytes = runCatching { loadImageBytes(source) }.getOrNull() ?: return@produceState
    if (bytes.isEmpty()) return@produceState

    val bitmap = withContext(Dispatchers.Default) {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
    value = bitmap?.asImageBitmap()
}

private fun calculateVideoHeight(
    maxWidth: Dp,
    width: Int?,
    height: Int?
): Dp {
    if (width == null || height == null || width <= 0 || height <= 0) {
        return 220.dp
    }

    val ratio = width.toFloat() / height.toFloat()
    if (!ratio.isFinite() || ratio <= 0f) {
        return 220.dp
    }

    return (maxWidth / ratio).coerceIn(120.dp, MaxInlineVideoHeightDp.dp)
}

private fun shouldUseLandscapeFullscreen(width: Int?, height: Int?): Boolean {
    return width != null && height != null && width > height && height > 0
}

private fun formatDuration(durationMs: Long): String {
    val safeMs = durationMs.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun guessVideoMimeType(fileName: String): String {
    return URLConnection.guessContentTypeFromName(fileName)
        ?.takeIf { it.startsWith("video/") }
        ?: "video/mp4"
}

private suspend fun persistVideoToGallery(
    context: Context,
    source: File,
    displayName: String,
    mimeType: String
): Boolean = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val targetName = displayName.ifBlank { "video.mp4" }
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, targetName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            Environment.DIRECTORY_MOVIES + "/Element"
        )
        put(MediaStore.Video.Media.IS_PENDING, 1)
    }

    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        ?: return@withContext false

    runCatching {
        resolver.openOutputStream(uri)?.use { output ->
            source.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: error("No output stream")

        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        true
    }.getOrElse {
        resolver.delete(uri, null, null)
        false
    }
}
