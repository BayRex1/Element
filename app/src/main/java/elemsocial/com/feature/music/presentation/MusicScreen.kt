package elemsocial.com.feature.music.presentation

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.input.pointer.pointerInput
import elemsocial.com.R
import elemsocial.com.core.model.LocalTransparencyMode
import elemsocial.com.core.model.TransparencyMode
import elemsocial.com.core.model.glassAlphaFor
import elemsocial.com.core.model.glassSoftAlphaFor
import elemsocial.com.core.model.shouldUseGlassBlur
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.MusicPlayerState
import elemsocial.com.domain.model.MusicPlaylistDetail
import elemsocial.com.domain.model.MusicPlaylistPreview
import elemsocial.com.domain.model.MusicSearchMode
import elemsocial.com.domain.model.MusicTrack
import elemsocial.com.domain.model.MusicUploadPayload
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostSong
import elemsocial.com.feature.home.presentation.HomeGateway
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private sealed interface MusicScreenRoute {
    data object Home : MusicScreenRoute
    data class Playlist(val id: String) : MusicScreenRoute
}

private const val FavoritesPlaylistId = "fav"
private const val MusicTrackPageSize = 25

private data class MusicUploadDraft(
    val audioUri: Uri? = null,
    val audioName: String? = null,
    val coverUri: Uri? = null,
    val coverBytes: ByteArray? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val trackNumber: String = "",
    val genre: String = "",
    val releaseYear: String = "",
    val composer: String = ""
)

@Composable
fun MusicScreen(
    homeGateway: HomeGateway,
    topPadding: Dp = 76.dp,
    bottomPadding: Dp = 150.dp,
    modifier: Modifier = Modifier,
    transparencyMode: TransparencyMode = TransparencyMode.ADAPTIVE
) {
    val musicGateway = LocalMusicGateway.current
    val musicController = LocalMusicController.current
    val musicState by musicController.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var route by remember { mutableStateOf<MusicScreenRoute>(MusicScreenRoute.Home) }
    var refreshToken by remember { mutableIntStateOf(0) }
    var library by remember { mutableStateOf<List<MusicPlaylistPreview>>(emptyList()) }
    var latestTracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var randomTracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var latestStartIndex by remember { mutableIntStateOf(0) }
    var randomStartIndex by remember { mutableIntStateOf(0) }
    var latestHasMore by remember { mutableStateOf(true) }
    var randomHasMore by remember { mutableStateOf(true) }
    var latestLoadingMore by remember { mutableStateOf(false) }
    var randomLoadingMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var createPlaylistOpen by remember { mutableStateOf(false) }
    var uploadSongOpen by remember { mutableStateOf(false) }
    var addToPlaylistTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var playlistDeleteCandidate by remember { mutableStateOf<MusicPlaylistDetail?>(null) }

    suspend fun reloadLibrary() {
        loading = true
        errorText = null

        val libraryResult = runCatching { musicGateway.loadLibrary() }.getOrNull()
        val latestResult = runCatching { musicGateway.loadTracks(type = "latest") }.getOrNull()
        val randomResult = runCatching { musicGateway.loadTracks(type = "random") }.getOrNull()

        if (libraryResult?.isSuccess == true) {
            library = libraryResult.data.orEmpty()
            musicController.replaceLibrary(library)
        }
        if (latestResult?.isSuccess == true) {
            latestTracks = latestResult.data.orEmpty()
            latestStartIndex = latestTracks.size
            latestHasMore = latestTracks.size >= MusicTrackPageSize
        }
        if (randomResult?.isSuccess == true) {
            randomTracks = randomResult.data.orEmpty()
            randomStartIndex = randomTracks.size
            randomHasMore = randomTracks.size >= MusicTrackPageSize
        }
        latestLoadingMore = false
        randomLoadingMore = false

        if (libraryResult?.isSuccess != true && latestResult?.isSuccess != true && randomResult?.isSuccess != true) {
            errorText = libraryResult?.message
                ?: latestResult?.message
                ?: randomResult?.message
                ?: "Не удалось загрузить музыку"
        }
        loading = false
    }

    suspend fun loadMoreTracks(type: String) {
        when (type) {
            "latest" -> {
                if (latestLoadingMore || !latestHasMore) return
                latestLoadingMore = true
                val result = runCatching {
                    musicGateway.loadTracks(type = "latest", startIndex = latestStartIndex)
                }.getOrNull()
                val loadedTracks = result?.data.orEmpty()
                if (result?.isSuccess == true) {
                    latestTracks = mergeMusicTracks(latestTracks, loadedTracks)
                    latestStartIndex += loadedTracks.size
                    latestHasMore = loadedTracks.size >= MusicTrackPageSize
                } else {
                    latestHasMore = false
                }
                latestLoadingMore = false
            }

            "random" -> {
                if (randomLoadingMore || !randomHasMore) return
                randomLoadingMore = true
                val result = runCatching {
                    musicGateway.loadTracks(type = "random", startIndex = randomStartIndex)
                }.getOrNull()
                val loadedTracks = result?.data.orEmpty()
                if (result?.isSuccess == true) {
                    randomTracks = mergeMusicTracks(randomTracks, loadedTracks)
                    randomStartIndex += loadedTracks.size
                    randomHasMore = loadedTracks.size >= MusicTrackPageSize
                } else {
                    randomHasMore = false
                }
                randomLoadingMore = false
            }
        }
    }

    LaunchedEffect(refreshToken) {
        reloadLibrary()
    }

    BackHandler(enabled = route != MusicScreenRoute.Home) {
        route = MusicScreenRoute.Home
    }

    val contentBottomPadding = bottomPadding + if (musicState.isSelected) 62.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ElementUiPalette.Body)
    ) {
        when (val currentRoute = route) {
            MusicScreenRoute.Home -> {
                MusicHomeContent(
                    library = library,
                    latestTracks = latestTracks,
                    randomTracks = randomTracks,
                    latestHasMore = latestHasMore,
                    randomHasMore = randomHasMore,
                    latestLoadingMore = latestLoadingMore,
                    randomLoadingMore = randomLoadingMore,
                    loading = loading,
                    errorText = errorText,
                    topPadding = topPadding,
                    bottomPadding = contentBottomPadding,
                    homeGateway = homeGateway,
                    musicState = musicState,
                    onCreatePlaylist = { createPlaylistOpen = true },
                    onUploadSong = { uploadSongOpen = true },
                    onOpenPlaylist = { route = MusicScreenRoute.Playlist(it) },
                    onLoadMoreLatest = {
                        scope.launch { loadMoreTracks("latest") }
                    },
                    onLoadMoreRandom = {
                        scope.launch { loadMoreTracks("random") }
                    },
                    onPlayTrack = { track, queue, index ->
                        scope.launch {
                            musicController.playTrack(track, queue, index, autoPlay = true)
                        }
                    }
                )
            }

            is MusicScreenRoute.Playlist -> {
                MusicPlaylistContent(
                    playlistId = currentRoute.id,
                    homeGateway = homeGateway,
                    topPadding = topPadding,
                    bottomPadding = contentBottomPadding,
                    onBack = { route = MusicScreenRoute.Home },
                    onDeletePlaylist = { playlistDeleteCandidate = it },
                    onAddTrackToPlaylist = { addToPlaylistTrack = it },
                    onLibraryChanged = { refreshToken += 1 },
                    onOpenPlaylist = { route = MusicScreenRoute.Playlist(it) }
                )
            }
        }

        if (createPlaylistOpen) {
            CreatePlaylistModal(
                onClose = { createPlaylistOpen = false },
                onCreated = {
                    createPlaylistOpen = false
                    refreshToken += 1
                    Toast.makeText(context, "Плейлист создан", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (uploadSongOpen) {
            UploadSongModal(
                onClose = { uploadSongOpen = false },
                onUploaded = {
                    uploadSongOpen = false
                    refreshToken += 1
                    Toast.makeText(context, "Песня добавлена", Toast.LENGTH_SHORT).show()
                }
            )
        }

        addToPlaylistTrack?.let { track ->
            SelectPlaylistModal(
                track = track,
                library = library,
                onClose = { addToPlaylistTrack = null },
                onAdded = {
                    addToPlaylistTrack = null
                    refreshToken += 1
                    Toast.makeText(context, "Трек добавлен в плейлист", Toast.LENGTH_SHORT).show()
                }
            )
        }

        playlistDeleteCandidate?.let { playlist ->
            AlertDialog(
                onDismissRequest = { playlistDeleteCandidate = null },
                title = { Text("Удалить плейлист") },
                text = { Text("После удаления плейлист нельзя восстановить.") },
                properties = DialogProperties(decorFitsSystemWindows = false),
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val playlistId = playlist.numericId ?: return@launch
                                val result = musicGateway.deletePlaylist(playlistId)
                                if (result.isSuccess) {
                                    playlistDeleteCandidate = null
                                    route = MusicScreenRoute.Home
                                    refreshToken += 1
                                    Toast.makeText(context, "Плейлист удалён", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        result.message ?: "Не удалось удалить плейлист",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { playlistDeleteCandidate = null }) {
                        Text("Отмена")
                    }
                },
                containerColor = ElementUiPalette.Block,
                titleContentColor = ElementUiPalette.TextPrimary,
                textContentColor = ElementUiPalette.TextSecondary
            )
        }
    }
}

@Composable
private fun MusicCard(
    modifier: Modifier = Modifier,
    padding: Dp = 8.dp,
    borderAlpha: Float = 0.08f,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(ElementUiPalette.Block.copy(alpha = 0.9f))
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        content = content
    )
}

@Composable
private fun MusicActionPill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(ElementUiPalette.Block.copy(alpha = 0.38f))
            .border(
                width = 1.dp,
                color = ElementUiPalette.Border.copy(alpha = 0.18f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ElementUiPalette.TextPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = title,
            color = ElementUiPalette.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun MusicCompactMetaBlock(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    titleColor: Color = ElementUiPalette.TextPrimary,
    artistColor: Color = ElementUiPalette.TextSecondary,
    titleFontSize: TextUnit = 13.sp,
    artistFontSize: TextUnit = 12.sp,
    titleLineHeight: TextUnit = titleFontSize,
    artistLineHeight: TextUnit = artistFontSize,
    titleFontWeight: FontWeight = FontWeight.SemiBold
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = title,
            color = titleColor,
            fontWeight = titleFontWeight,
            fontSize = titleFontSize,
            lineHeight = titleLineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
        Text(
            text = artist,
            color = artistColor,
            fontSize = artistFontSize,
            lineHeight = artistLineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

@Composable
fun MusicPlayStateButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ElementUiPalette.TextPrimary,
    size: Dp = 30.dp,
    iconSize: Dp = 21.dp,
    horizontalPadding: Dp = 0.dp
) {
    Box(
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .size(size)
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            MusicVideoStyleLoader(
                modifier = Modifier.size(iconSize),
                color = tint,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                painter = painterResource(
                    if (isPlaying) {
                        R.drawable.ic_music_pause
                    } else {
                        R.drawable.ic_music_play
                    }
                ),
                contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun MusicInlineLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = ElementUiPalette.TextSecondary
) {
    Box(
        modifier = modifier.size(12.dp),
        contentAlignment = Alignment.Center
    ) {
        MusicVideoStyleLoader(
            modifier = Modifier.size(12.dp),
            color = color,
            strokeWidth = 1.8.dp
        )
    }
}

@Composable
fun MusicVideoStyleLoader(
    modifier: Modifier = Modifier,
    color: Color = ElementUiPalette.TextPrimary,
    strokeWidth: Dp = 2.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "music-loader-rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "music-loader-rotation-value"
    )

    CircularProgressIndicator(
        progress = { 0.82f },
        modifier = modifier.graphicsLayer { rotationZ = rotation },
        color = color,
        strokeWidth = strokeWidth,
        trackColor = Color.Transparent
    )
}

fun isMusicTrackLoading(
    state: MusicPlayerState,
    trackId: Int
): Boolean {
    val selected = state.selectedTrack?.id == trackId
    return state.pendingTrackId == trackId ||
        (
            selected &&
                state.pendingTrackId == null &&
                !state.playing &&
                state.isDownloading &&
                state.currentTimeSeconds <= 0.15
            )
}

fun isMusicTrackPlaybackActive(
    state: MusicPlayerState,
    trackId: Int
): Boolean {
    val selected = state.selectedTrack?.id == trackId
    return selected && (state.playing || (state.isBuffering && state.pendingTrackId == null))
}

@Composable
fun MusicMiniPlayerBar(
    homeGateway: HomeGateway,
    modifier: Modifier = Modifier
) {
    val controller = LocalMusicController.current
    val state by controller.state.collectAsState()
    val track = state.selectedTrack ?: return
    val loading = remember(state.pendingTrackId, state.selectedTrack?.id, state.isDownloading, state.isBuffering, state.playing) {
        isMusicTrackLoading(state, track.id)
    }
    val visualPlaying = remember(state.pendingTrackId, state.selectedTrack?.id, state.isBuffering, state.playing) {
        isMusicTrackPlaybackActive(state, track.id)
    }
    val transparencyMode = LocalTransparencyMode.current
    val blurEnabled = shouldUseGlassBlur(transparencyMode)
    val playerAlpha = glassAlphaFor(transparencyMode)
    val playerSoftAlpha = glassSoftAlphaFor(transparencyMode)
    val surfaceTop = ElementUiPalette.Block.copy(alpha = playerAlpha)
    val surfaceBottom = if (blurEnabled) {
        ElementUiPalette.Block.copy(alpha = (playerAlpha - 0.04f).coerceAtLeast(0f))
    } else {
        ElementUiPalette.BlockSoft.copy(alpha = playerSoftAlpha)
    }
    val compactInactiveColor = if (blurEnabled) {
        ElementUiPalette.TextPrimary.copy(alpha = 0.08f)
    } else {
        ElementUiPalette.TextSecondary.copy(alpha = 0.24f)
    }
    val compactTimeColor = if (blurEnabled) {
        ElementUiPalette.TextPrimary.copy(alpha = 0.56f)
    } else {
        ElementUiPalette.TextSecondary.copy(alpha = 0.68f)
    }

    AnimatedVisibility(
        visible = state.isSelected,
        enter = fadeIn(tween(180)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(220, easing = LinearOutSlowInEasing)
        ),
        exit = fadeOut(tween(120)) + slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = tween(140, easing = FastOutSlowInEasing)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .padding(bottom = 106.dp)
                .shadow(
                    elevation = 10.dp,
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
                .border(
                    width = 1.dp,
                    color = ElementUiPalette.scaledBorder(0.9f),
                    shape = RoundedCornerShape(30.dp)
                )
                .clickable { controller.openFullPlayer() }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MusicArtwork(
                    asset = track.cover,
                    title = track.title,
                    homeGateway = homeGateway,
                    size = 50.dp,
                    cornerRadius = 5.dp,
                    onClick = { controller.openFullPlayer() }
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    MusicCompactMetaBlock(
                        title = track.title,
                        artist = track.artist,
                        modifier = Modifier.clickable { controller.openFullPlayer() }
                    )
                    MusicProgressRow(
                        currentTimeSeconds = state.currentTimeSeconds,
                        durationSeconds = state.durationSeconds,
                        onSeek = { controller.seekTo(it) },
                        compact = true,
                        compactActiveColor = ElementUiPalette.TextPrimary,
                        compactInactiveColor = compactInactiveColor,
                        compactTimeColor = compactTimeColor,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    MiniPlayerIconButton(iconRes = R.drawable.ic_music_previous) {
                        controller.previous()
                    }
                    MusicPlayStateButton(
                        isPlaying = visualPlaying,
                        isLoading = loading,
                        onClick = { controller.togglePlayPause() },
                        tint = ElementUiPalette.TextPrimary,
                        size = 28.dp,
                        iconSize = 17.dp
                    )
                    MiniPlayerIconButton(iconRes = R.drawable.ic_music_next) {
                        controller.next()
                    }
                }
            }
        }
    }
}

@Composable
fun MusicFullPlayerOverlay(
    homeGateway: HomeGateway,
    modifier: Modifier = Modifier,
    transparencyMode: TransparencyMode = TransparencyMode.ADAPTIVE
) {
    val controller = LocalMusicController.current
    val gateway = LocalMusicGateway.current
    val state by controller.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedTrack = state.selectedTrack

    var metadataOpen by remember(selectedTrack?.id) { mutableStateOf(false) }
    var metadataTrack by remember(selectedTrack?.id) { mutableStateOf<MusicTrack?>(selectedTrack) }
    val dragOffsetY = remember(selectedTrack?.id) { Animatable(0f) }
    var dragCloseInProgress by remember(selectedTrack?.id) { mutableStateOf(false) }
    val overlayTransparencyMode = transparencyMode
    val dismissDragThreshold = with(LocalDensity.current) { 118.dp.toPx() }

    LaunchedEffect(metadataOpen, selectedTrack?.id) {
        val track = selectedTrack ?: return@LaunchedEffect
        if (!metadataOpen) return@LaunchedEffect
        metadataTrack = gateway.loadTrack(track.id).data ?: track
    }

    BackHandler(enabled = state.fullPlayerOpen) {
        controller.closeFullPlayer()
    }

    AnimatedVisibility(
        visible = state.fullPlayerOpen && selectedTrack != null,
        enter = fadeIn(tween(220)) + slideInVertically(
            initialOffsetY = { it / 5 },
            animationSpec = tween(260, easing = LinearOutSlowInEasing)
        ),
        exit = fadeOut(tween(160)) + slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(260, easing = FastOutSlowInEasing)
        ),
        modifier = modifier
    ) {
        val track = selectedTrack ?: return@AnimatedVisibility
        val visualPlaying = isMusicTrackPlaybackActive(state, track.id)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragOffsetY.value
                    alpha = 1f
                }
                .pointerInput(state.fullPlayerOpen, metadataOpen, dismissDragThreshold, dragCloseInProgress) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            if (dragCloseInProgress) {
                                change.consume()
                            } else if (dragAmount > 0f) {
                                val nextOffset = (dragOffsetY.value + dragAmount).coerceAtMost(dismissDragThreshold * 1.7f)
                                scope.launch {
                                    dragOffsetY.stop()
                                    dragOffsetY.snapTo(nextOffset)
                                }
                                change.consume()
                            } else if (dragOffsetY.value > 0f) {
                                val nextOffset = (dragOffsetY.value + dragAmount).coerceAtLeast(0f)
                                scope.launch {
                                    dragOffsetY.stop()
                                    dragOffsetY.snapTo(nextOffset)
                                }
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            if (!dragCloseInProgress && dragOffsetY.value >= dismissDragThreshold) {
                                dragCloseInProgress = true
                                scope.launch {
                                    metadataOpen = false
                                    controller.closeFullPlayer()
                                    delay(280)
                                    dragOffsetY.snapTo(0f)
                                    dragCloseInProgress = false
                                }
                            } else if (!dragCloseInProgress) {
                                scope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            if (!dragCloseInProgress) {
                                scope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            MusicBlurBackground(
                asset = track.cover,
                title = track.title,
                homeGateway = homeGateway,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.65f },
                transparencyMode = overlayTransparencyMode
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (metadataOpen) {
                                metadataOpen = false
                            } else {
                                controller.closeFullPlayer()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_player_close),
                        contentDescription = "Закрыть",
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(27.dp)
                    )
                }

                if (metadataOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 72.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        MusicMetadataPanel(
                            track = metadataTrack ?: track,
                            onClose = { metadataOpen = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .widthIn(max = 380.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .widthIn(max = 380.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            AnimatedContent(
                                targetState = track.id,
                                transitionSpec = {
                                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                                },
                                label = "music-cover"
                            ) {
                                MusicArtwork(
                                    asset = track.cover,
                                    title = track.title,
                                    homeGateway = homeGateway,
                                    size = 320.dp,
                                    cornerRadius = 16.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                            .padding(end = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = track.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 21.sp,
                                            lineHeight = 21.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = TextStyle(
                                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                                            )
                                        )
                                        Text(
                                            text = track.artist,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 18.sp,
                                            lineHeight = 20.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = TextStyle(
                                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                                            )
                                        )
                                    }

                                    MusicModeButton(
                                        iconRes = R.drawable.ic_music_info,
                                        active = false,
                                        onClick = { metadataOpen = true }
                                    )
                                    MusicModeButton(
                                        icon = if (track.liked) {
                                            Icons.Default.Favorite
                                        } else {
                                            Icons.Default.FavoriteBorder
                                        },
                                        active = track.liked,
                                        onClick = {
                                            scope.launch {
                                                val result = controller.toggleLikeCurrent()
                                                if (!result.isSuccess) {
                                                    Toast.makeText(
                                                        context,
                                                        result.message ?: "Не удалось обновить лайк",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    )
                                }

                                MusicProgressRow(
                                    currentTimeSeconds = state.currentTimeSeconds,
                                    durationSeconds = state.durationSeconds,
                                    onSeek = { controller.seekTo(it) },
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MusicModeButton(
                                        iconRes = R.drawable.ic_music_random,
                                        active = state.random,
                                        onClick = { controller.setRandom(!state.random) }
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FullPlayerControlButton(iconRes = R.drawable.ic_music_previous) {
                                            controller.previous()
                                        }
                                        FullPlayerControlButton(
                                            iconRes = if (visualPlaying) {
                                                R.drawable.ic_music_pause
                                            } else {
                                                R.drawable.ic_music_play
                                            },
                                            accent = true
                                        ) {
                                            controller.togglePlayPause()
                                        }
                                        FullPlayerControlButton(iconRes = R.drawable.ic_music_next) {
                                            controller.next()
                                        }
                                    }
                                    MusicModeButton(
                                        iconRes = R.drawable.ic_music_loop,
                                        active = state.loop,
                                        onClick = { controller.setLoop(!state.loop) }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_music_volume_minus),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    MusicLevelTimeline(
                                        value = state.volume,
                                        onValueChange = { controller.setVolume(it) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.ic_music_volume_plus),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
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

@Composable
fun MusicPickerModal(
    homeGateway: HomeGateway,
    selectedTracks: List<MusicTrack>,
    onSelectionChange: (List<MusicTrack>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val musicGateway = LocalMusicGateway.current
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }
    var tabMode by remember { mutableStateOf(MusicSearchMode.Favorites) }
    var favorites by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var newTracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var loadingFavorites by remember { mutableStateOf(true) }
    var loadingNew by remember { mutableStateOf(false) }
    var loadingSearch by remember { mutableStateOf(false) }

    val isSearchMode = query.trim().length >= 3
    val displayMode = if (isSearchMode) MusicSearchMode.Search else tabMode

    LaunchedEffect(Unit) {
        val result = musicGateway.loadTracks(type = "favorites")
        favorites = result.data.orEmpty()
        loadingFavorites = false
    }

    LaunchedEffect(tabMode) {
        if (tabMode != MusicSearchMode.New || newTracks.isNotEmpty()) return@LaunchedEffect
        loadingNew = true
        val result = musicGateway.loadTracks(type = "latest")
        newTracks = result.data.orEmpty()
        loadingNew = false
    }

    LaunchedEffect(query) {
        val value = query.trim()
        if (value.length < 3) {
            searchResults = emptyList()
            loadingSearch = false
            return@LaunchedEffect
        }
        loadingSearch = true
        delay(300)
        if (query.trim() != value) return@LaunchedEffect
        val result = musicGateway.searchTracks(value)
        searchResults = result.data.orEmpty()
        loadingSearch = false
    }

    fun toggleTrack(track: MusicTrack) {
        if (selectedTracks.any { it.id == track.id }) {
            onSelectionChange(selectedTracks.filterNot { it.id == track.id })
            return
        }
        if (selectedTracks.size >= 100) {
            Toast.makeText(context, "Можно выбрать до 100 треков", Toast.LENGTH_SHORT).show()
            return
        }
        onSelectionChange(selectedTracks + track)
    }

    UIKit.RoutedModal(
        title = "Музыка",
        onClose = onClose,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MusicModalInput(
                value = query,
                onValueChange = { query = it },
                placeholder = "Найти музыку",
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ElementUiPalette.TextSecondary
                    )
                }
            )

            Text(
                text = if (selectedTracks.isNotEmpty()) {
                    "Выбрано: ${selectedTracks.size}"
                } else {
                    "Выберите треки"
                },
                color = ElementUiPalette.TextSecondary,
                fontSize = 14.sp
            )

            if (!isSearchMode) {
                UIKit.SegmentTabs(
                    tabs = listOf("Избранное", "Новое"),
                    selectedIndex = if (tabMode == MusicSearchMode.New) 1 else 0,
                    onSelect = { index ->
                        tabMode = if (index == 1) {
                            MusicSearchMode.New
                        } else {
                            MusicSearchMode.Favorites
                        }
                    }
                )
            }

            val tracks = when (displayMode) {
                MusicSearchMode.Favorites -> favorites
                MusicSearchMode.New -> newTracks
                MusicSearchMode.Search -> searchResults
            }
            val loading = when (displayMode) {
                MusicSearchMode.Favorites -> loadingFavorites
                MusicSearchMode.New -> loadingNew
                MusicSearchMode.Search -> loadingSearch
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                when {
                    loading -> {
                        CenterMessage(text = "Загрузка...")
                    }

                    displayMode == MusicSearchMode.Search && tracks.isEmpty() && query.trim().length >= 3 -> {
                        CenterMessage(text = "Ничего не найдено")
                    }

                    displayMode == MusicSearchMode.Favorites && tracks.isEmpty() -> {
                        CenterMessage(text = "В избранном пока пусто")
                    }

                    displayMode == MusicSearchMode.New && tracks.isEmpty() -> {
                        CenterMessage(text = "Новых треков пока нет")
                    }

                    displayMode != MusicSearchMode.Search && query.trim().isEmpty() && tracks.isEmpty() -> {
                        CenterMessage(text = "Пока нечего показать")
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(tracks, key = { it.id }) { track ->
                                PickerTrackRow(
                                    track = track,
                                    selected = selectedTracks.any { it.id == track.id },
                                    homeGateway = homeGateway,
                                    onClick = { toggleTrack(track) }
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTracks.isNotEmpty()) {
                UIKit.Button(
                    title = "Добавить выбранное",
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
fun MusicAttachedTracks(
    homeGateway: HomeGateway,
    tracks: List<MusicTrack>,
    onRemove: (MusicTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        tracks.forEach { track ->
            MusicCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MusicArtwork(
                        asset = track.cover,
                        title = track.title,
                        homeGateway = homeGateway,
                        size = 36.dp,
                        cornerRadius = 5.dp
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = track.title,
                            color = ElementUiPalette.TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            color = ElementUiPalette.TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { onRemove(track) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Убрать",
                            tint = ElementUiPalette.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostMusicTracksBlock(
    homeGateway: HomeGateway,
    songs: List<PostSong>,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    val controller = LocalMusicController.current
    val state by controller.state.collectAsState()
    val scope = rememberCoroutineScope()
    val tracks = remember(songs) { songs.map(::postSongToTrack) }
    var allTracksOpen by remember(songs) { mutableStateOf(false) }

    if (tracks.size == 1) {
        PostMusicTrackCard(
            homeGateway = homeGateway,
            track = tracks.first(),
            queue = tracks,
            index = 0,
            state = state,
            onPlay = { track, queue, index ->
                scope.launch {
                    controller.playTrack(track = track, queue = queue, requestedIndex = index, autoPlay = true)
                }
            },
            onTogglePause = { controller.togglePlayPause() },
            onSeek = { controller.seekTo(it) },
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clickable { allTracksOpen = true }
    ) {
        repeat(3) { layer ->
            val level = layer + 1
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElementUiPalette.Interaction.copy(alpha = 0.5f))
                    .height(80.dp)
                    .offset(y = (-((level + 0.5f) * 3f)).dp)
                    .scale(1f - level * 0.05f)
            )
        }
        PostMusicTrackCard(
            homeGateway = homeGateway,
            track = tracks.first(),
            queue = tracks,
            index = 0,
            state = state,
            onPlay = { track, queue, index ->
                scope.launch {
                    controller.playTrack(track = track, queue = queue, requestedIndex = index, autoPlay = true)
                }
            },
            onTogglePause = { controller.togglePlayPause() },
            onSeek = { controller.seekTo(it) },
            songsCount = tracks.size,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }

    if (allTracksOpen) {
        UIKit.RoutedModal(
            title = "Песни (${tracks.size})",
            onClose = { allTracksOpen = false }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
                    PostMusicTrackCard(
                        homeGateway = homeGateway,
                        track = track,
                        queue = tracks,
                        index = index,
                        state = state,
                        onPlay = { clickedTrack, queue, clickedIndex ->
                            scope.launch {
                                controller.playTrack(
                                    track = clickedTrack,
                                    queue = queue,
                                    requestedIndex = clickedIndex,
                                    autoPlay = true
                                )
                            }
                        },
                        onTogglePause = { controller.togglePlayPause() },
                        onSeek = { controller.seekTo(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PostMusicTrackCard(
    homeGateway: HomeGateway,
    track: MusicTrack,
    queue: List<MusicTrack>,
    index: Int,
    state: MusicPlayerState,
    onPlay: (MusicTrack, List<MusicTrack>, Int) -> Unit,
    onTogglePause: () -> Unit,
    onSeek: (Double) -> Unit,
    songsCount: Int? = null,
    modifier: Modifier = Modifier
) {
    val selected = state.selectedTrack?.id == track.id
    val loading = remember(state.pendingTrackId, state.selectedTrack?.id, state.isDownloading, state.isBuffering, state.playing, track.id) {
        isMusicTrackLoading(state, track.id)
    }
    val visualPlaying = remember(state.pendingTrackId, state.selectedTrack?.id, state.isBuffering, state.playing, track.id) {
        isMusicTrackPlaybackActive(state, track.id)
    }
    val canPlay = (songsCount ?: 1) < 2

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.Interaction.copy(alpha = 0.96f))
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 140,
                    easing = LinearOutSlowInEasing
                )
            )
            .padding(5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicArtwork(
                asset = track.cover,
                title = track.title,
                homeGateway = homeGateway,
                size = 50.dp,
                cornerRadius = 7.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                MusicCompactMetaBlock(
                    title = track.title,
                    artist = track.artist,
                    titleColor = ElementUiPalette.TextPrimary,
                    artistColor = ElementUiPalette.TextPrimary.copy(alpha = 0.9f),
                    titleFontSize = 14.sp,
                    artistFontSize = 12.sp,
                    titleLineHeight = 14.sp,
                    artistLineHeight = 12.sp,
                    titleFontWeight = FontWeight.Medium
                )
                AnimatedVisibility(
                    visible = canPlay && (loading || visualPlaying),
                    enter = fadeIn(tween(110)) + expandVertically(
                        animationSpec = tween(110, easing = LinearOutSlowInEasing),
                        expandFrom = Alignment.Top
                    ),
                    exit = fadeOut(tween(100)) + shrinkVertically(
                        animationSpec = tween(100, easing = FastOutSlowInEasing),
                        shrinkTowards = Alignment.Top
                    )
                ) {
                    if (loading) {
                        MusicInlineLoadingIndicator(
                            modifier = Modifier.padding(top = 5.dp),
                            color = ElementUiPalette.TextPrimary.copy(alpha = 0.56f)
                        )
                    } else {
                        MusicProgressRow(
                            currentTimeSeconds = state.currentTimeSeconds,
                            durationSeconds = state.durationSeconds,
                            onSeek = onSeek,
                            compact = true,
                            compactActiveColor = ElementUiPalette.InteractionText,
                            compactInactiveColor = ElementUiPalette.Block.copy(alpha = 0.22f),
                            compactTimeColor = ElementUiPalette.TextPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                }
            }

            if (canPlay) {
                MusicPlayStateButton(
                    isPlaying = visualPlaying,
                    isLoading = loading,
                    onClick = {
                        if (selected) {
                            onTogglePause()
                        } else {
                            onPlay(track, queue, index)
                        }
                    },
                    tint = ElementUiPalette.TextPrimary,
                    horizontalPadding = 10.dp
                )
            } else {
                Text(
                    text = formatTracksCountLabel(songsCount ?: 0),
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
private fun MusicHomeContent(
    library: List<MusicPlaylistPreview>,
    latestTracks: List<MusicTrack>,
    randomTracks: List<MusicTrack>,
    latestHasMore: Boolean,
    randomHasMore: Boolean,
    latestLoadingMore: Boolean,
    randomLoadingMore: Boolean,
    loading: Boolean,
    errorText: String?,
    topPadding: Dp,
    bottomPadding: Dp,
    homeGateway: HomeGateway,
    musicState: MusicPlayerState,
    onCreatePlaylist: () -> Unit,
    onUploadSong: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onLoadMoreLatest: () -> Unit,
    onLoadMoreRandom: () -> Unit,
    onPlayTrack: (MusicTrack, List<MusicTrack>, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height((topPadding - 62.dp).coerceAtLeast(6.dp)))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MusicActionPill(
                    title = "Плейлист",
                    icon = Icons.Default.Add,
                    onClick = onCreatePlaylist
                )
                MusicActionPill(
                    title = "Загрузить",
                    icon = Icons.Default.Upload,
                    onClick = onUploadSong
                )
            }
        }

        if (!errorText.isNullOrBlank()) {
            item {
                CenterMessage(
                    text = errorText,
                    color = ElementUiPalette.Error
                )
            }
        }

        if (loading && library.isEmpty() && latestTracks.isEmpty() && randomTracks.isEmpty()) {
            item {
                MusicShelfTitle(title = "Моё")
            }
            item {
                MusicPlaylistCarouselLoading()
            }
            item {
                MusicShelfTitle(title = "Новое")
            }
            item {
                MusicTrackCarousel(
                    tracks = emptyList(),
                    showLoading = true,
                    homeGateway = homeGateway,
                    musicState = musicState,
                    onPlayTrack = onPlayTrack
                )
            }
            item {
                MusicShelfTitle(title = "Случайное")
            }
            item {
                MusicTrackCarousel(
                    tracks = emptyList(),
                    showLoading = true,
                    homeGateway = homeGateway,
                    musicState = musicState,
                    onPlayTrack = onPlayTrack
                )
            }
        } else {
            item {
                MusicShelfTitle(title = "Моё")
            }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item(key = FavoritesPlaylistId) {
                        PlaylistPreviewCard(
                            title = "Избранное",
                            subtitle = "Тут собраны ваши любимые песни :)",
                            active = false,
                            accentIcon = Icons.Default.Favorite,
                            accentIconSize = 68.dp,
                            index = 0,
                            isLast = library.isEmpty(),
                            onClick = { onOpenPlaylist(FavoritesPlaylistId) }
                        )
                    }
                    itemsIndexed(library, key = { _, item -> item.id }) { index, playlist ->
                        PlaylistPreviewCard(
                            title = playlist.title,
                            subtitle = playlist.authorName ?: "Плейлист",
                            active = false,
                            accentIcon = Icons.Default.QueueMusic,
                            index = index + 1,
                            isLast = index == library.lastIndex,
                            onClick = { onOpenPlaylist(playlist.id) }
                        )
                    }
                }
            }

            item {
                MusicShelfTitle(title = "Новое")
            }
            item {
                MusicTrackCarousel(
                    tracks = latestTracks,
                    showLoading = loading && latestTracks.isEmpty(),
                    canLoadMore = latestHasMore,
                    loadingMore = latestLoadingMore,
                    onLoadMore = onLoadMoreLatest,
                    homeGateway = homeGateway,
                    musicState = musicState,
                    onPlayTrack = onPlayTrack
                )
            }

            item {
                MusicShelfTitle(title = "Случайное")
            }
            item {
                MusicTrackCarousel(
                    tracks = randomTracks,
                    showLoading = loading && randomTracks.isEmpty(),
                    canLoadMore = randomHasMore,
                    loadingMore = randomLoadingMore,
                    onLoadMore = onLoadMoreRandom,
                    homeGateway = homeGateway,
                    musicState = musicState,
                    onPlayTrack = onPlayTrack
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

@Composable
private fun MusicPlaylistContent(
    playlistId: String,
    homeGateway: HomeGateway,
    topPadding: Dp,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onDeletePlaylist: (MusicPlaylistDetail) -> Unit,
    onAddTrackToPlaylist: (MusicTrack) -> Unit,
    onLibraryChanged: () -> Unit,
    onOpenPlaylist: (String) -> Unit
) {
    val musicGateway = LocalMusicGateway.current
    val musicController = LocalMusicController.current
    val musicState by musicController.state.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var playlist by remember(playlistId) { mutableStateOf<MusicPlaylistDetail?>(null) }
    var loading by remember(playlistId) { mutableStateOf(true) }
    var errorText by remember(playlistId) { mutableStateOf<String?>(null) }
    var menuTrackId by remember(playlistId) { mutableStateOf<Int?>(null) }
    var playlistMenuOpen by remember(playlistId) { mutableStateOf(false) }

    fun removeTrackLocally(trackId: Int) {
        val current = playlist ?: return
        playlist = current.copy(
            songs = current.songs.filterNot { it.id == trackId }
        )
    }

    fun toggleTrackLikeLocally(trackId: Int) {
        val current = playlist ?: return
        val target = current.songs.firstOrNull { it.id == trackId } ?: return
        playlist = if (current.isFavorites && target.liked) {
            current.copy(
                songs = current.songs.filterNot { it.id == trackId }
            )
        } else {
            current.copy(
                songs = current.songs.map { song ->
                    if (song.id == trackId) song.copy(liked = !song.liked) else song
                }
            )
        }
    }

    suspend fun reloadPlaylist() {
        loading = true
        val result = musicGateway.loadPlaylist(playlistId)
        playlist = result.data
        errorText = if (result.isSuccess) null else result.message ?: "Не удалось открыть плейлист"
        loading = false
    }

    LaunchedEffect(playlistId) {
        reloadPlaylist()
    }

    val currentPlaylist = playlist

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height((topPadding - 68.dp).coerceAtLeast(4.dp)))
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = ElementUiPalette.TextPrimary
                        )
                    }
                    if (currentPlaylist?.isFavorites == false && currentPlaylist.numericId != null) {
                        Box {
                            IconButton(onClick = { playlistMenuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Меню плейлиста",
                                    tint = ElementUiPalette.TextPrimary
                                )
                            }
                            UIKit.DropdownMenu(
                                expanded = playlistMenuOpen,
                                onDismissRequest = { playlistMenuOpen = false }
                            ) {
                                UIKit.DropdownMenuItem(
                                    text = "Удалить",
                                    destructive = true,
                                    onClick = {
                                        playlistMenuOpen = false
                                        currentPlaylist?.let(onDeletePlaylist)
                                    }
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                currentPlaylist?.let { detail ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        MusicArtwork(
                            asset = null,
                            title = detail.title,
                            homeGateway = homeGateway,
                            size = if (detail.isFavorites) 120.dp else 96.dp,
                            cornerRadius = 12.dp,
                            overlayIcon = if (detail.isFavorites) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.QueueMusic
                            },
                            overlayIconSizeFactor = if (detail.isFavorites) 0.40f else 0.28f
                        )
                        Text(
                            text = detail.title,
                            color = ElementUiPalette.TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 24.sp,
                            lineHeight = 26.sp,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                        if (!detail.description.isNullOrBlank()) {
                            Text(
                                text = detail.description.orEmpty(),
                                color = ElementUiPalette.TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 16.sp,
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                        Text(
                            text = buildPlaylistMeta(detail),
                            color = ElementUiPalette.TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }
        }

        when {
            loading -> {
                item { CenterMessage(text = "Загрузка...") }
            }

            !errorText.isNullOrBlank() -> {
                item { CenterMessage(text = errorText.orEmpty(), color = ElementUiPalette.Error) }
            }

            currentPlaylist == null -> {
                item { CenterMessage(text = "Плейлист не найден") }
            }

            currentPlaylist.songs.isEmpty() -> {
                item { CenterMessage(text = "В плейлисте пока пусто") }
            }

            else -> {
                itemsIndexed(currentPlaylist.songs, key = { _, item -> item.id }) { index, track ->
                    val selected = musicState.selectedTrack?.id == track.id
                    val loadingTrack = isMusicTrackLoading(musicState, track.id)
                    MusicCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.width(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (loadingTrack) {
                                    MusicInlineLoadingIndicator()
                                } else {
                                    Text(
                                        text = (index + 1).toString(),
                                        color = ElementUiPalette.TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            MusicArtwork(
                                asset = track.cover,
                                title = track.title,
                                homeGateway = homeGateway,
                                size = 40.dp,
                                cornerRadius = 6.dp,
                                onClick = {
                                    scope.launch {
                                        musicController.playTrack(
                                            track = track,
                                            queue = currentPlaylist.songs,
                                            requestedIndex = index,
                                            autoPlay = true
                                        )
                                    }
                                }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        scope.launch {
                                            musicController.playTrack(
                                                track = track,
                                                queue = currentPlaylist.songs,
                                                requestedIndex = index,
                                                autoPlay = true
                                            )
                                        }
                                    }
                            ) {
                                MusicCompactMetaBlock(
                                    title = track.title,
                                    artist = track.artist,
                                    titleColor = if (selected) ElementUiPalette.Accent else ElementUiPalette.TextPrimary,
                                    artistColor = ElementUiPalette.TextSecondary,
                                    titleFontSize = 14.sp,
                                    artistFontSize = 12.sp,
                                    titleLineHeight = 14.sp,
                                    artistLineHeight = 12.sp,
                                    titleFontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val result = musicGateway.toggleLike(track.id)
                                        if (result.isSuccess) {
                                            musicGateway.applyTrackLikeState(track.id, liked = !track.liked)
                                            toggleTrackLikeLocally(track.id)
                                            musicController.applyTrackLikeToggleLocally(track.id)
                                            onLibraryChanged()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                result.message ?: "Не удалось обновить лайк",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (track.liked) {
                                        Icons.Default.Favorite
                                    } else {
                                        Icons.Default.FavoriteBorder
                                    },
                                    contentDescription = "Лайк",
                                    tint = if (track.liked) ElementUiPalette.Accent else ElementUiPalette.TextPrimary
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { menuTrackId = track.id }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Меню трека",
                                        tint = ElementUiPalette.TextPrimary
                                    )
                                }
                                UIKit.DropdownMenu(
                                    expanded = menuTrackId == track.id,
                                    onDismissRequest = { menuTrackId = null }
                                ) {
                                    UIKit.DropdownMenuItem(
                                        text = "Добавить в плейлист",
                                        onClick = {
                                            menuTrackId = null
                                            onAddTrackToPlaylist(track)
                                        }
                                    )
                                    UIKit.DropdownMenuItem(
                                        text = "Скачать",
                                        onClick = {
                                            menuTrackId = null
                                            if (track.originalFileId > 0) {
                                                scope.launch {
                                                    val result = musicController.exportCachedTrack(track)
                                                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Файл недоступен для скачивания", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                    UIKit.DropdownMenuItem(
                                        text = "Поделиться",
                                        onClick = {
                                            menuTrackId = null
                                            clipboard.setText(androidx.compose.ui.text.AnnotatedString("https://element.social/post/${track.id}"))
                                            Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    if (currentPlaylist?.isFavorites == true) {
                                        UIKit.DropdownMenuItem(
                                            text = "Убрать из избранного",
                                            destructive = true,
                                            onClick = {
                                                menuTrackId = null
                                                scope.launch {
                                                    val result = musicGateway.toggleLike(track.id)
                                                    if (result.isSuccess) {
                                                        musicGateway.applyTrackLikeState(track.id, liked = false)
                                                        removeTrackLocally(track.id)
                                                        musicController.applyTrackLikeToggleLocally(track.id)
                                                        onLibraryChanged()
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        UIKit.DropdownMenuItem(
                                            text = "Убрать из плейлиста",
                                            destructive = true,
                                            onClick = {
                                                menuTrackId = null
                                                scope.launch {
                                                    val numericId = currentPlaylist?.numericId
                                                    if (numericId == null) {
                                                        Toast.makeText(context, "Плейлист не найден", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        val result = musicGateway.removeTrackFromPlaylist(numericId, track.id)
                                                        if (result.isSuccess) {
                                                            removeTrackLocally(track.id)
                                                            onLibraryChanged()
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                result.message ?: "Не удалось обновить плейлист",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

@Composable
private fun MusicTrackCarousel(
    tracks: List<MusicTrack>,
    showLoading: Boolean = false,
    canLoadMore: Boolean = false,
    loadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    homeGateway: HomeGateway,
    musicState: MusicPlayerState,
    onPlayTrack: (MusicTrack, List<MusicTrack>, Int) -> Unit
) {
    if (tracks.isEmpty()) {
        if (showLoading) {
            MusicTrackCarouselLoading()
            return
        }
        CenterMessage(text = "Пока пусто")
        return
    }

    val listState = rememberLazyListState()

    LaunchedEffect(listState, tracks.size, canLoadMore, loadingMore) {
        if (!canLoadMore || onLoadMore == null) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (!loadingMore && lastVisibleIndex >= tracks.lastIndex - 2) {
                    onLoadMore()
                }
            }
    }

    LazyRow(
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
            TrackPreviewCard(
                track = track,
                homeGateway = homeGateway,
                active = musicState.selectedTrack?.id == track.id,
                loading = isMusicTrackLoading(musicState, track.id),
                index = index,
                isLast = index == tracks.lastIndex && !canLoadMore && !loadingMore,
                onClick = { onPlayTrack(track, tracks, index) }
            )
        }
        if (canLoadMore || loadingMore) {
            item(key = "load-more-${tracks.size}") {
                LaunchedEffect(tracks.size, canLoadMore, loadingMore) {
                    if (canLoadMore && !loadingMore) {
                        onLoadMore?.invoke()
                    }
                }
                TrackPreviewLoadingCard(
                    index = tracks.size,
                    isLast = true
                )
            }
        }
    }
}

@Composable
private fun MusicTrackCarouselLoading() {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(3) { index ->
            TrackPreviewLoadingCard(
                index = index,
                isLast = index == 2
            )
        }
    }
}

@Composable
private fun MusicPlaylistCarouselLoading() {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(3) { index ->
            PlaylistPreviewLoadingCard(
                index = index,
                isLast = index == 2
            )
        }
    }
}

@Composable
private fun TrackPreviewCard(
    track: MusicTrack,
    homeGateway: HomeGateway,
    active: Boolean,
    loading: Boolean,
    index: Int,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(165.dp)
            .padding(
                start = if (index == 0) 8.dp else 12.dp,
                end = if (isLast) 8.dp else 0.dp,
                top = 6.dp,
                bottom = 6.dp
            )
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .scale(if (active) 0.93f else 1f)
        ) {
            MusicArtwork(
                asset = track.cover,
                title = track.title,
                homeGateway = homeGateway,
                size = 150.dp,
                cornerRadius = 10.dp,
                modifier = Modifier
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = loading,
                enter = fadeIn(tween(140)),
                exit = fadeOut(tween(140)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.26f)),
                    contentAlignment = Alignment.Center
                ) {
                    MusicInlineLoadingIndicator()
                }
            }
        }
        Row(
            modifier = Modifier
                .width(132.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = track.title,
                    color = ElementUiPalette.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Text(
                    text = track.artist,
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
private fun TrackPreviewLoadingCard(
    index: Int,
    isLast: Boolean
) {
    Column(
        modifier = Modifier
            .width(165.dp)
            .padding(
                start = if (index == 0) 8.dp else 12.dp,
                end = if (isLast) 8.dp else 0.dp,
                top = 6.dp,
                bottom = 6.dp
            ),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ElementUiPalette.Block),
            contentAlignment = Alignment.Center
        ) {
            MusicInlineLoadingIndicator()
        }
        Column(
            modifier = Modifier
                .width(132.dp)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(ElementUiPalette.Block)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ElementUiPalette.Block)
            )
        }
    }
}

@Composable
private fun PlaylistPreviewLoadingCard(
    index: Int,
    isLast: Boolean
) {
    Column(
        modifier = Modifier
            .width(165.dp)
            .padding(
                start = if (index == 0) 8.dp else 12.dp,
                end = if (isLast) 8.dp else 0.dp,
                top = 6.dp,
                bottom = 6.dp
            )
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ElementUiPalette.Block),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ElementUiPalette.BlockSoft)
            )
        }
        Column(
            modifier = Modifier
                .width(132.dp)
                .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(ElementUiPalette.Block)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.54f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ElementUiPalette.Block)
            )
        }
    }
}

@Composable
private fun PlaylistPreviewCard(
    title: String,
    subtitle: String,
    active: Boolean,
    accentIcon: androidx.compose.ui.graphics.vector.ImageVector,
    accentIconSize: Dp = 52.dp,
    index: Int,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(165.dp)
            .padding(
                start = if (index == 0) 8.dp else 12.dp,
                end = if (isLast) 8.dp else 0.dp,
                top = 6.dp,
                bottom = 6.dp
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            ElementUiPalette.Accent.copy(alpha = 0.85f),
                            ElementUiPalette.Accent.copy(alpha = 0.45f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = accentIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(accentIconSize)
            )
        }
        Row(
            modifier = Modifier
                .width(132.dp)
                .padding(start = 4.dp, top = 2.dp, end = 4.dp, bottom = 0.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = title,
                    color = if (active) ElementUiPalette.Accent else ElementUiPalette.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Text(
                    text = subtitle,
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
private fun SelectPlaylistModal(
    track: MusicTrack,
    library: List<MusicPlaylistPreview>,
    onClose: () -> Unit,
    onAdded: () -> Unit
) {
    val gateway = LocalMusicGateway.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playlists = remember(library) { library.filter { it.numericId != null } }

    UIKit.RoutedModal(
        title = "Добавить в плейлист",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (playlists.isEmpty()) {
                CenterMessage(text = "Сначала создайте плейлист")
            } else {
                playlists.forEach { playlist ->
                    UIKit.Block(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    val result = gateway.addTrackToPlaylist(
                                        playlistId = playlist.numericId ?: return@launch,
                                        trackId = track.id
                                    )
                                    if (result.isSuccess) {
                                        onAdded()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            result.message ?: "Не удалось добавить трек",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                        contentPadding = 12.dp
                    ) {
                        Column {
                            Text(
                                text = playlist.title,
                                color = ElementUiPalette.TextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                            Text(
                                text = playlist.authorName ?: "Плейлист",
                                color = ElementUiPalette.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicModalInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val isLightTheme = ElementUiPalette.Body.luminance() > 0.5f
    UIKit.Input(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        singleLine = singleLine,
        trailingIcon = trailingIcon,
        borderWidth = if (isLightTheme) 1.dp else 0.dp,
        borderColor = if (isLightTheme) ElementUiPalette.Border.copy(alpha = 0.68f) else Color.Transparent,
        containerColor = if (isLightTheme) Color(0xFFF7F6FB) else ElementUiPalette.BlockSoft
    )
}

@Composable
private fun CreatePlaylistModal(
    onClose: () -> Unit,
    onCreated: () -> Unit
) {
    val gateway = LocalMusicGateway.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    UIKit.RoutedModal(
        title = "Создать плейлист",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MusicModalInput(
                value = title,
                onValueChange = { title = it.take(60) },
                placeholder = "Введите название"
            )
            MusicModalInput(
                value = description,
                onValueChange = { description = it.take(1000) },
                placeholder = "Введите описание",
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp)
            )
            UIKit.Button(
                title = "Создать",
                onClick = {
                    if (title.isBlank() || sending) return@Button
                    scope.launch {
                        sending = true
                        val result = gateway.createPlaylist(
                            name = title.trim(),
                            description = description.trim()
                        )
                        sending = false
                        if (result.isSuccess) {
                            onCreated()
                        } else {
                            Toast.makeText(
                                context,
                                result.message ?: "Не удалось создать плейлист",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                enabled = title.isNotBlank() && !sending,
                loading = sending
            )
        }
    }
}

@Composable
private fun UploadSongModal(
    onClose: () -> Unit,
    onUploaded: () -> Unit
) {
    val gateway = LocalMusicGateway.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var draft by remember { mutableStateOf(MusicUploadDraft()) }
    var sending by remember { mutableStateOf(false) }

    fun clearCover() {
        draft = draft.copy(
            coverUri = null,
            coverBytes = null
        )
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val retriever = MediaMetadataRetriever()
        val embeddedCoverBytes = runCatching {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture
        }.getOrNull()
        val newDraft = MusicUploadDraft(
            audioUri = uri,
            audioName = context.resolveDisplayName(uri),
            coverUri = embeddedCoverBytes?.let { null } ?: draft.coverUri,
            coverBytes = embeddedCoverBytes ?: draft.coverBytes,
            title = runCatching {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            }.getOrNull().orEmpty(),
            artist = runCatching {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            }.getOrNull().orEmpty(),
            album = runCatching {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            }.getOrNull().orEmpty(),
            trackNumber = runCatching {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            }.getOrNull().orEmpty(),
            genre = runCatching {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            }.getOrNull().orEmpty(),
            releaseYear = runCatching {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            }.getOrNull().orEmpty(),
            composer = runCatching {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
            }.getOrNull().orEmpty()
        )
        runCatching { retriever.release() }
        draft = newDraft
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = context.readBytes(uri) ?: return@rememberLauncherForActivityResult
        draft = draft.copy(
            coverUri = uri,
            coverBytes = bytes
        )
    }

    val coverBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = draft.coverUri, key2 = draft.coverBytes) {
        val inlineBytes = draft.coverBytes
        if (inlineBytes != null && inlineBytes.isNotEmpty()) {
            value = BitmapFactory.decodeByteArray(inlineBytes, 0, inlineBytes.size)?.asImageBitmap()
            return@produceState
        }
        val source = draft.coverUri ?: return@produceState
        val bytes = context.readBytes(source) ?: return@produceState
        value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }

    UIKit.RoutedModal(
        title = "Добавить песню",
        onClose = onClose,
        contentHorizontalPadding = 6.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val stacked = maxWidth < 360.dp
            val coverSize = 150.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (stacked) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MusicUploadCoverCard(
                            bitmap = coverBitmap,
                            onPickCover = { coverPicker.launch("image/*") },
                            onRemoveCover = ::clearCover,
                            size = coverSize,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            MusicUploadSectionTitle("Основная информация")
                            MusicModalInput(
                                value = draft.title,
                                onValueChange = { draft = draft.copy(title = it) },
                                placeholder = "Название"
                            )
                            MusicModalInput(
                                value = draft.artist,
                                onValueChange = { draft = draft.copy(artist = it) },
                                placeholder = "Исполнитель"
                            )
                            MusicModalInput(
                                value = draft.album,
                                onValueChange = { draft = draft.copy(album = it) },
                                placeholder = "Альбом"
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        MusicUploadCoverCard(
                            bitmap = coverBitmap,
                            onPickCover = { coverPicker.launch("image/*") },
                            onRemoveCover = ::clearCover,
                            size = coverSize
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = coverSize),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MusicUploadSectionTitle("Основная информация")
                            MusicModalInput(
                                value = draft.title,
                                onValueChange = { draft = draft.copy(title = it) },
                                placeholder = "Название"
                            )
                            MusicModalInput(
                                value = draft.artist,
                                onValueChange = { draft = draft.copy(artist = it) },
                                placeholder = "Исполнитель"
                            )
                            MusicModalInput(
                                value = draft.album,
                                onValueChange = { draft = draft.copy(album = it) },
                                placeholder = "Альбом"
                            )
                        }
                    }
                }

                if (stacked) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MusicUploadAudioChooser(
                            fileName = draft.audioName,
                            onPickAudio = { audioPicker.launch("audio/*") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Если в файле уже есть метаданные, они подтянутся автоматически, а введённые в форме значения запишутся поверх них.",
                            color = ElementUiPalette.TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MusicUploadAudioChooser(
                            fileName = draft.audioName,
                            onPickAudio = { audioPicker.launch("audio/*") },
                            modifier = Modifier.width(150.dp)
                        )
                        Text(
                            text = "Если в файле уже есть метаданные, они подтянутся автоматически, а введённые в форме значения запишутся поверх них.",
                            color = ElementUiPalette.TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MusicUploadSectionTitle("Дополнительная информация")
                    if (stacked) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MusicModalInput(
                                value = draft.trackNumber,
                                onValueChange = { draft = draft.copy(trackNumber = it) },
                                placeholder = "Номер трека"
                            )
                            MusicModalInput(
                                value = draft.genre,
                                onValueChange = { draft = draft.copy(genre = it) },
                                placeholder = "Жанр"
                            )
                            MusicModalInput(
                                value = draft.releaseYear,
                                onValueChange = { draft = draft.copy(releaseYear = it) },
                                placeholder = "Год"
                            )
                            MusicModalInput(
                                value = draft.composer,
                                onValueChange = { draft = draft.copy(composer = it) },
                                placeholder = "Композитор"
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MusicModalInput(
                                    value = draft.trackNumber,
                                    onValueChange = { draft = draft.copy(trackNumber = it) },
                                    placeholder = "Номер трека"
                                )
                                MusicModalInput(
                                    value = draft.genre,
                                    onValueChange = { draft = draft.copy(genre = it) },
                                    placeholder = "Жанр"
                                )
                                MusicModalInput(
                                    value = draft.releaseYear,
                                    onValueChange = { draft = draft.copy(releaseYear = it) },
                                    placeholder = "Год"
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MusicModalInput(
                                    value = draft.composer,
                                    onValueChange = { draft = draft.copy(composer = it) },
                                    placeholder = "Композитор"
                                )
                            }
                        }
                    }
                }

                MusicActionButton(
                    title = "Отправить",
                    loading = sending,
                    enabled = draft.audioUri != null && draft.title.isNotBlank() && draft.artist.isNotBlank() && !sending,
                    onClick = {
                        val audioUri = draft.audioUri ?: return@MusicActionButton
                        if (draft.title.isBlank() || draft.artist.isBlank() || sending) return@MusicActionButton
                        scope.launch {
                            val audioBytes = context.readBytes(audioUri)
                            if (audioBytes == null || audioBytes.isEmpty()) {
                                Toast.makeText(context, "Не удалось прочитать аудиофайл", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            sending = true
                            val result = gateway.uploadSong(
                                MusicUploadPayload(
                                    title = draft.title.trim(),
                                    artist = draft.artist.trim(),
                                    album = draft.album.trim().ifBlank { null },
                                    trackNumber = draft.trackNumber.trim().ifBlank { null },
                                    genre = draft.genre.trim().ifBlank { null },
                                    releaseYear = draft.releaseYear.trim().ifBlank { null },
                                    composer = draft.composer.trim().ifBlank { null },
                                    audioBytes = audioBytes,
                                    coverBytes = draft.coverBytes
                                )
                            )
                            sending = false
                            if (result.isSuccess) {
                                onUploaded()
                            } else {
                                Toast.makeText(
                                    context,
                                    result.message ?: "Не удалось загрузить песню",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MusicUploadSectionTitle(
    text: String
) {
    Text(
        text = text,
        color = ElementUiPalette.Accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun MusicUploadCoverCard(
    bitmap: ImageBitmap?,
    onPickCover: () -> Unit,
    onRemoveCover: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.BlockSoft.copy(alpha = 0.94f))
            .clickable(onClick = onPickCover),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Обложка",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Text(
                text = "Удалить",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.34f))
                    .clickable(onClick = onRemoveCover)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = ElementUiPalette.TextPrimary,
                    modifier = Modifier.size(74.dp)
                )
                Text(
                    text = "Обложка",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MusicUploadAudioChooser(
    fileName: String?,
    onPickAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.BlockSoft.copy(alpha = 0.94f))
            .clickable(onClick = onPickAudio)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = fileName ?: "Выберите файл",
            color = ElementUiPalette.TextPrimary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MusicBlurBackground(
    asset: PostImageAsset?,
    title: String,
    homeGateway: HomeGateway,
    modifier: Modifier = Modifier,
    transparencyMode: TransparencyMode = TransparencyMode.ADAPTIVE
) {
    val bitmap by rememberMusicBitmap(asset = asset, homeGateway = homeGateway)
    val blurEnabled = shouldUseGlassBlur(transparencyMode)
    val backgroundAlpha = when (transparencyMode) {
        TransparencyMode.TRANSPARENT -> 0.82f
        TransparencyMode.OPAQUE -> 1.0f
        TransparencyMode.ADAPTIVE -> if (blurEnabled) 0.88f else 0.94f
    }
    val overlayAlpha = when (transparencyMode) {
        TransparencyMode.TRANSPARENT -> 0.22f
        TransparencyMode.OPAQUE -> 0.35f
        TransparencyMode.ADAPTIVE -> if (blurEnabled) 0.26f else 0.30f
    }

    Box(modifier = modifier) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (blurEnabled) 44.dp else 36.dp),
                contentScale = ContentScale.Crop,
                alpha = backgroundAlpha
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF161616), Color(0xFF2A2A2A), Color(0xFF111111))
                        )
                    )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha))
        )
    }
}

@Composable
private fun MusicMetadataPanel(
    track: MusicTrack,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metadataLines = remember(track) {
        buildList {
            add("Название" to track.title)
            add("Исполнитель" to track.artist)
            track.metadata.album?.takeIf { it.isNotBlank() }?.let { add("Альбом" to it) }
            track.metadata.genre?.takeIf { it.isNotBlank() }?.let { add("Жанр" to it) }
            track.metadata.trackNumber?.let { add("Номер трека" to it.toString()) }
            track.metadata.releaseYear?.let { add("Год релиза" to it.toString()) }
            track.metadata.composer?.takeIf { it.isNotBlank() }?.let { add("Композитор" to it) }
            track.metadata.durationSeconds?.takeIf { it > 0.0 }?.let {
                add("Длительность" to formatMusicDuration(it))
            }
            track.metadata.bitrate?.let {
                add("Битрейт" to "${(it / 1000.0).roundToInt()} кбит/сек")
            }
            track.metadata.audioFormat?.takeIf { it.isNotBlank() }?.let { add("Формат" to it) }
            track.metadata.dateAdded?.takeIf { it.isNotBlank() }?.let { add("Добавлено" to it) }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(metadataLines) { (title, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "$title:",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 16.sp,
                        modifier = Modifier.widthIn(min = 120.dp)
                    )
                    Text(
                        text = value,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicArtwork(
    asset: PostImageAsset?,
    title: String,
    homeGateway: HomeGateway,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    overlayIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    overlayIconSizeFactor: Float = 0.28f,
    onClick: (() -> Unit)? = null
) {
    val bitmap by rememberMusicBitmap(asset = asset, homeGateway = homeGateway)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    listOf(
                        ElementUiPalette.Accent.copy(alpha = 0.95f),
                        ElementUiPalette.Accent.copy(alpha = 0.35f)
                    )
                )
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = overlayIcon ?: Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size((size.value * overlayIconSizeFactor).dp)
            )
        }
    }
}

@Composable
fun MusicProgressRow(
    currentTimeSeconds: Double,
    durationSeconds: Double,
    onSeek: (Double) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    compactActiveColor: Color = ElementUiPalette.TextPrimary,
    compactInactiveColor: Color = ElementUiPalette.TextSecondary.copy(alpha = 0.28f),
    compactTimeColor: Color = ElementUiPalette.TextSecondary.copy(alpha = 0.78f)
) {
    val clampedDuration = durationSeconds.coerceAtLeast(0.1)
    var dragging by remember { mutableStateOf(false) }
    var sliderValue by remember(clampedDuration) {
        mutableStateOf(currentTimeSeconds.coerceIn(0.0, clampedDuration).toFloat())
    }
    LaunchedEffect(currentTimeSeconds, clampedDuration, dragging) {
        if (!dragging) {
            sliderValue = currentTimeSeconds.coerceIn(0.0, clampedDuration).toFloat()
        }
    }
    val displaySeconds = if (dragging) sliderValue.toDouble() else currentTimeSeconds

    if (compact) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = formatMusicDuration(displaySeconds),
                color = compactTimeColor,
                fontSize = 8.sp,
                lineHeight = 8.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            ElementMusicTimeline(
                value = sliderValue,
                onValueChange = {
                    dragging = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    dragging = false
                    onSeek(sliderValue.toDouble())
                },
                valueRange = 0f..clampedDuration.toFloat(),
                activeColor = compactActiveColor,
                inactiveColor = compactInactiveColor,
                collapsedHeight = 5.dp,
                expandedHeight = 10.dp,
                modifier = Modifier
                    .weight(1f)
            )
            Text(
                text = formatMusicDuration(clampedDuration),
                color = compactTimeColor,
                fontSize = 8.sp,
                lineHeight = 8.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        ElementMusicTimeline(
            value = sliderValue,
            onValueChange = {
                dragging = true
                sliderValue = it
            },
            onValueChangeFinished = {
                dragging = false
                onSeek(sliderValue.toDouble())
            },
            valueRange = 0f..clampedDuration.toFloat(),
            activeColor = Color.White,
            inactiveColor = Color.White.copy(alpha = 0.35f),
            collapsedHeight = 12.dp,
            expandedHeight = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMusicDuration(displaySeconds),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                lineHeight = 12.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            Text(
                text = formatMusicDuration(clampedDuration),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                lineHeight = 12.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@Composable
private fun MusicLevelTimeline(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(value.coerceIn(0f, 1f)) }

    LaunchedEffect(value, dragging) {
        if (!dragging) {
            sliderValue = value.coerceIn(0f, 1f)
        }
    }

    ElementMusicTimeline(
        value = sliderValue,
        onValueChange = {
            dragging = true
            sliderValue = it.coerceIn(0f, 1f)
            onValueChange(sliderValue)
        },
        onValueChangeFinished = {
            dragging = false
            onValueChange(sliderValue)
        },
        valueRange = 0f..1f,
        activeColor = Color.White,
        inactiveColor = Color.White.copy(alpha = 0.5f),
        collapsedHeight = 12.dp,
        expandedHeight = 18.dp,
        modifier = modifier
    )
}

@Composable
private fun ElementMusicTimeline(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
    collapsedHeight: Dp = 5.dp,
    expandedHeight: Dp = 10.dp
) {
    val rangeStart = valueRange.start
    val rangeEnd = valueRange.endInclusive
    val rangeSize = (rangeEnd - rangeStart).takeIf { it > 0f } ?: 1f
    var widthPx by remember(rangeStart, rangeEnd) { mutableStateOf(1f) }
    var dragFraction by remember(rangeStart, rangeEnd) {
        mutableStateOf(((value - rangeStart) / rangeSize).coerceIn(0f, 1f))
    }
    var dragging by remember { mutableStateOf(false) }
    val trackHeight by animateDpAsState(
        targetValue = if (dragging) expandedHeight else collapsedHeight,
        animationSpec = tween(durationMillis = 140),
        label = "music_timeline_height"
    )

    LaunchedEffect(value, rangeStart, rangeEnd, dragging) {
        if (!dragging) {
            dragFraction = ((value - rangeStart) / rangeSize).coerceIn(0f, 1f)
        }
    }

    fun dispatchFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        dragFraction = clamped
        onValueChange((rangeStart + (rangeSize * clamped)).coerceIn(rangeStart, rangeEnd))
    }

    Box(
        modifier = modifier
            .height(expandedHeight)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(rangeStart, rangeEnd, widthPx) {
                detectTapGestures { offset ->
                    dispatchFraction(offset.x / widthPx)
                    onValueChangeFinished()
                }
            }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    if (!dragging) dragging = true
                    dispatchFraction(dragFraction + (delta / widthPx))
                },
                onDragStarted = { dragging = true },
                onDragStopped = {
                    dragging = false
                    onValueChangeFinished()
                }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        val shape = RoundedCornerShape(trackHeight / 2)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(shape)
                .background(inactiveColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(dragFraction.coerceIn(0f, 1f))
                    .clip(shape)
                    .background(activeColor)
            )
        }
    }
}

@Composable
private fun MusicShelfTitle(
    title: String
) {
    Text(
        text = title,
        color = ElementUiPalette.TextPrimary,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 0.dp)
    )
}

@Composable
private fun PickerTrackRow(
    track: MusicTrack,
    selected: Boolean,
    homeGateway: HomeGateway,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) {
                    ElementUiPalette.Accent.copy(alpha = 0.12f)
                } else {
                    ElementUiPalette.Block
                }
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) ElementUiPalette.Accent else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MusicArtwork(
            asset = track.cover,
            title = track.title,
            homeGateway = homeGateway,
            size = 36.dp,
            cornerRadius = 4.dp
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                color = ElementUiPalette.TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = ElementUiPalette.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = ElementUiPalette.Accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MiniPlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconRes: Int? = null,
    tint: Color = ElementUiPalette.TextPrimary,
    rotation: Float = 0f,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(17.dp)
                    .rotate(rotation)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(17.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
private fun FullPlayerControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconRes: Int? = null,
    accent: Boolean = false,
    rotation: Float = 0f,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (accent) 38.dp else 28.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White.copy(alpha = if (accent) 1f else 0.55f),
                modifier = Modifier
                    .size(if (accent) 30.dp else 24.dp)
                    .rotate(rotation)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = if (accent) 1f else 0.55f),
                modifier = Modifier
                    .size(if (accent) 30.dp else 24.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
private fun MusicModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconRes: Int? = null,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (active) ElementUiPalette.Accent else Color.White.copy(alpha = 0.62f),
                modifier = Modifier.size(22.dp)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) ElementUiPalette.Accent else Color.White.copy(alpha = 0.62f),
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun CenterMessage(
    text: String,
    color: Color = ElementUiPalette.TextSecondary
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun rememberMusicBitmap(
    asset: PostImageAsset?,
    homeGateway: HomeGateway
) = produceState<ImageBitmap?>(initialValue = null, key1 = asset?.cacheKey) {
    val source = asset ?: return@produceState
    val bytes = runCatching { homeGateway.loadImageBytes(source) }.getOrNull() ?: return@produceState
    if (bytes.isEmpty()) return@produceState
    value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}

private fun postSongToTrack(song: PostSong): MusicTrack {
    return MusicTrack(
        id = song.id,
        title = song.title,
        artist = song.artist,
        cover = song.cover,
        originalFileId = 0,
        metadata = elemsocial.com.domain.model.MusicTrackMetadata(
            album = song.album,
            durationSeconds = song.durationSeconds
        )
    )
}

private fun mergeMusicTracks(
    current: List<MusicTrack>,
    incoming: List<MusicTrack>
): List<MusicTrack> {
    return (current + incoming).distinctBy { it.id }
}

@Composable
private fun MusicActionButton(
    title: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) {
                    ElementUiPalette.Accent
                } else {
                    ElementUiPalette.Accent.copy(alpha = 0.45f)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            MusicVideoStyleLoader(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

private fun buildPlaylistMeta(detail: MusicPlaylistDetail): String {
    val totalDuration = detail.songs.sumOf { it.metadata.durationSeconds ?: 0.0 }
    val durationText = if (totalDuration > 0.0) formatMusicDuration(totalDuration) else "0:00"
    return "${detail.songs.size} треков • $durationText"
}

private fun formatTracksCountLabel(count: Int): String {
    val mod100 = count % 100
    val mod10 = count % 10
    val suffix = when {
        mod100 in 11..19 -> "треков"
        mod10 == 1 -> "трек"
        mod10 in 2..4 -> "трека"
        else -> "треков"
    }
    return "$count $suffix"
}

private fun formatMusicDuration(seconds: Double): String {
    val safe = seconds.coerceAtLeast(0.0).roundToInt()
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val secs = safe % 60
    return if (hours > 0) {
        "${hours}ч"
    } else {
        String.format(Locale.US, "%d:%02d", minutes, secs)
    }
}

private fun trackShareLink(trackId: Int): String {
    return "https://elemsocial.com/music/$trackId"
}

private fun android.content.Context.readBytes(uri: Uri): ByteArray? {
    return runCatching {
        contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()
}

private fun android.content.Context.resolveDisplayName(uri: Uri): String? {
    val fallback = uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.takeIf { it.isNotBlank() }

    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    cursor.getString(index)
                } else {
                    fallback
                }
            } ?: fallback
    }.getOrDefault(fallback)
}
