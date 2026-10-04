package elemsocial.com.feature.music.presentation

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.ExoPlayer
import elemsocial.com.core.cache.ImageDiskCache
import elemsocial.com.core.cache.MusicCacheIndexStore
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.MusicDownloadItem
import elemsocial.com.domain.model.MusicDownloadStatus
import elemsocial.com.domain.model.MusicPlayerState
import elemsocial.com.domain.model.MusicResult
import elemsocial.com.domain.model.MusicTrack
import elemsocial.com.domain.model.StorageFileMeta
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MusicController(
    private val context: Context,
    private val gateway: MusicGateway,
    private val diskCache: ImageDiskCache,
    private val cacheIndexStore: MusicCacheIndexStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadManager = MusicProgressiveDownloadManager(
        context = context,
        gateway = gateway,
        diskCache = diskCache,
        cacheIndexStore = cacheIndexStore,
        scope = ioScope,
        onSnapshot = ::handleDownloadSnapshot
    )
    private val player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(
                ProgressiveMusicDataSourceFactory(downloadManager)
            )
        )
        .build()
        .apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            setWakeMode(C.WAKE_MODE_LOCAL)
            setHandleAudioBecomingNoisy(true)
        }
    private val _state = MutableStateFlow(MusicPlayerState())
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()
    private val _downloads = MutableStateFlow<List<MusicDownloadItem>>(emptyList())
    val downloads: StateFlow<List<MusicDownloadItem>> = _downloads.asStateFlow()
    val playerHandle: Player
        get() = player
    private var progressJob: Job? = null
    private val downloadTracks = linkedMapOf<Int, MusicTrack>()
    private val downloadLock = Any()

    init {
        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(
                        playing = currentVisualPlaying(),
                        pendingTrackId = if (isPlaying) null else _state.value.pendingTrackId
                    )
                    syncProgressLoop()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val buffering = playbackState == Player.STATE_BUFFERING
                    _state.value = _state.value.copy(
                        isBuffering = buffering,
                        durationSeconds = safeDurationSeconds(),
                        desiredSeekSeconds = if (playbackState == Player.STATE_READY) {
                            null
                        } else {
                            _state.value.desiredSeekSeconds
                        },
                        pendingTrackId = if (playbackState == Player.STATE_READY) {
                            null
                        } else {
                            _state.value.pendingTrackId
                        }
                    )

                    if (playbackState == Player.STATE_ENDED) {
                        handlePlaybackEnded()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    syncSelectedTrackFromPlayer(resetProgress = true)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _state.value = _state.value.copy(
                        playing = false,
                        isBuffering = false,
                        pendingTrackId = null
                    )
                }
            }
        )
        player.volume = _state.value.volume
    }

    fun release() {
        progressJob?.cancel()
        stopAndReset()
        downloadManager.release()
        player.release()
        scope.cancel()
        ioScope.cancel()
    }

    fun stopAndReset() {
        progressJob?.cancel()
        player.pause()
        player.clearMediaItems()
        _state.value = MusicPlayerState(
            library = _state.value.library,
            volume = _state.value.volume
        )
        _downloads.value = emptyList()
        synchronized(downloadLock) {
            downloadTracks.clear()
        }
    }

    fun replaceLibrary(library: List<elemsocial.com.domain.model.MusicPlaylistPreview>) {
        _state.value = _state.value.copy(library = library)
    }

    fun openFullPlayer() {
        _state.value = _state.value.copy(fullPlayerOpen = true)
    }

    fun closeFullPlayer() {
        _state.value = _state.value.copy(fullPlayerOpen = false)
    }

    fun togglePlayPause() {
        if (!_state.value.isSelected) return
        if (player.isPlaying) {
            player.pause()
        } else {
            MusicPlaybackService.start(context)
            player.play()
        }
    }

    fun setRandom(enabled: Boolean) {
        player.shuffleModeEnabled = enabled
        _state.value = _state.value.copy(random = enabled)
    }

    fun setLoop(enabled: Boolean) {
        player.repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _state.value = _state.value.copy(loop = enabled)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        player.volume = clamped
        _state.value = _state.value.copy(volume = clamped)
    }

    fun seekTo(seconds: Double) {
        if (seconds.isNaN() || seconds.isInfinite()) return
        val targetMs = (seconds * 1000.0).toLong().coerceAtLeast(0L)
        player.seekTo(targetMs)
        _state.value = _state.value.copy(
            desiredSeekSeconds = seconds,
            currentTimeSeconds = seconds
        )
    }

    fun previous() {
        val snapshot = _state.value
        val queue = snapshot.queue
        if (queue.isEmpty()) return

        val index = if (snapshot.random && queue.size > 1) {
            randomOtherIndex(snapshot.currentIndex, queue.lastIndex)
        } else if (snapshot.currentIndex == 0) {
            queue.lastIndex
        } else {
            snapshot.currentIndex - 1
        }
        seekToQueueIndex(index)
    }

    fun next() {
        val snapshot = _state.value
        val queue = snapshot.queue
        if (queue.isEmpty()) return

        val index = if (snapshot.random && queue.size > 1) {
            randomOtherIndex(snapshot.currentIndex, queue.lastIndex)
        } else {
            (snapshot.currentIndex + 1) % queue.size
        }
        seekToQueueIndex(index)
    }

    suspend fun playTrack(
        track: MusicTrack,
        queue: List<MusicTrack> = listOf(track),
        requestedIndex: Int = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0),
        autoPlay: Boolean = true
    ) {
        _state.value = _state.value.copy(pendingTrackId = track.id)

        val playableTrack = ensurePlayableTrack(track) ?: run {
            _state.value = _state.value.copy(pendingTrackId = null)
            return
        }
        val normalizedQueue = ensurePlayableQueue(
            queue = queue.ifEmpty { listOf(playableTrack) },
            preferredTrack = playableTrack
        ) ?: run {
            _state.value = _state.value.copy(pendingTrackId = null)
            return
        }
        val resolvedIndex = normalizedQueue.indexOfFirst { it.id == playableTrack.id }
            .takeIf { it >= 0 }
            ?: requestedIndex.coerceIn(0, normalizedQueue.lastIndex)

        val current = _state.value.selectedTrack
        val sameSelection = current?.id == playableTrack.id && _state.value.queue == normalizedQueue
        if (
            sameSelection &&
            player.currentMediaItem != null &&
            player.playbackState != Player.STATE_IDLE &&
            player.mediaItemCount == normalizedQueue.size
        ) {
            if (autoPlay) {
                MusicPlaybackService.start(context)
                player.play()
            } else {
                player.pause()
            }
            _state.value = _state.value.copy(pendingTrackId = null)
            return
        }

        normalizedQueue.forEachIndexed { index, queueTrack ->
            rememberDownloadTrack(queueTrack)
            val fileMeta = gateway.getFileMeta(queueTrack.originalFileId).data ?: run {
                _state.value = _state.value.copy(pendingTrackId = null)
                return
            }
            downloadManager.prepare(
                meta = fileMeta,
                startDownload = index == resolvedIndex
            )
        }

        _state.value = _state.value.copy(
            selectedTrack = playableTrack,
            queue = normalizedQueue,
            currentIndex = resolvedIndex,
            isSelected = true,
            currentTimeSeconds = 0.0,
            durationSeconds = playableTrack.metadata.durationSeconds ?: 0.0,
            desiredSeekSeconds = null,
            pendingTrackId = playableTrack.id,
            isDownloading = !downloadManager.isFullyCached(playableTrack.originalFileId)
        )

        val mediaItems = normalizedQueue.map { queueTrack ->
            MediaItem.Builder()
                .setMediaId(queueTrack.id.toString())
                .setUri("element-music://track/${queueTrack.originalFileId}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(queueTrack.title)
                        .setArtist(queueTrack.artist)
                        .build()
                )
                .build()
        }
        player.setMediaItems(mediaItems, resolvedIndex, 0L)
        player.prepare()
        MusicPlaybackService.start(context)
        player.playWhenReady = autoPlay
        if (!autoPlay) {
            player.pause()
        }

        syncProgressLoop()
    }

    suspend fun playTrackById(
        trackId: Int,
        queue: List<MusicTrack> = emptyList(),
        autoPlay: Boolean = true
    ) {
        _state.value = _state.value.copy(pendingTrackId = trackId)
        val loaded = gateway.loadTrack(trackId).data ?: run {
            _state.value = _state.value.copy(pendingTrackId = null)
            return
        }
        val normalizedQueue = if (queue.isEmpty()) listOf(loaded) else {
            queue.map { if (it.id == loaded.id) loaded else it }
        }
        playTrack(
            track = loaded,
            queue = normalizedQueue,
            requestedIndex = normalizedQueue.indexOfFirst { it.id == loaded.id }.coerceAtLeast(0),
            autoPlay = autoPlay
        )
    }

    suspend fun toggleLikeCurrent(): ActionResult {
        val currentTrack = _state.value.selectedTrack ?: return ActionResult("error", "Трек не выбран")
        val trackId = currentTrack.id
        val result = gateway.toggleLike(trackId)
        if (result.isSuccess) {
            gateway.applyTrackLikeState(trackId, liked = !currentTrack.liked)
            updateTrackLike(trackId)
        }
        return result
    }

    fun applyTrackLikeToggleLocally(trackId: Int) {
        updateTrackLike(trackId)
    }

    suspend fun exportCachedTrack(track: MusicTrack): ActionResult {
        val entry = cacheIndexStore.read(track.originalFileId)
            ?: return ActionResult("error", "Для начала прослушайте трек полностью")
        val file = diskCache.resolveFile(entry.cacheKey)
            ?: return ActionResult("error", "Кэш трека не найден")

        val extension = when {
            entry.mime.contains("mpeg") -> "mp3"
            entry.mime.contains("flac") -> "flac"
            else -> "audio"
        }
        val displayName = buildString {
            append(track.artist.ifBlank { "artist" })
            append(" - ")
            append(track.title.ifBlank { "track" })
            append(".")
            append(extension)
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, entry.mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Element")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return ActionResult("error", "Не удалось создать файл")

        return runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IOException("Не удалось открыть поток")

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            ActionResult("success", "Трек сохранён")
        }.getOrElse { error ->
            resolver.delete(uri, null, null)
            ActionResult("error", error.message ?: "Не удалось сохранить трек")
        }
    }

    private suspend fun ensurePlayableTrack(track: MusicTrack): MusicTrack? {
        return if (track.originalFileId > 0) {
            track
        } else {
            gateway.loadTrack(track.id).data
        }
    }

    private suspend fun ensurePlayableQueue(
        queue: List<MusicTrack>,
        preferredTrack: MusicTrack
    ): List<MusicTrack>? {
        val normalized = ArrayList<MusicTrack>(queue.size)
        queue.forEach { item ->
            val playableItem = if (item.id == preferredTrack.id) {
                preferredTrack
            } else {
                ensurePlayableTrack(item)
            } ?: return null
            normalized += playableItem
        }
        return normalized
    }

    private fun rememberDownloadTrack(track: MusicTrack) {
        if (track.originalFileId <= 0) return
        synchronized(downloadLock) {
            downloadTracks[track.originalFileId] = track
        }
    }

    private fun handleDownloadSnapshot(snapshot: MusicDownloadSnapshot) {
        val track = synchronized(downloadLock) {
            downloadTracks[snapshot.fileId]
        } ?: return

        val updated = _downloads.value.toMutableList()
        val existingIndex = updated.indexOfFirst { it.fileId == snapshot.fileId }
        val existing = updated.getOrNull(existingIndex)
        val item = MusicDownloadItem(
            fileId = snapshot.fileId,
            track = track,
            downloadedBytes = snapshot.downloadedBytes,
            totalBytes = snapshot.totalBytes,
            status = when {
                snapshot.hasError -> MusicDownloadStatus.Error
                snapshot.isComplete -> MusicDownloadStatus.Completed
                else -> MusicDownloadStatus.Downloading
            },
            downloadDate = existing?.downloadDate ?: System.currentTimeMillis()
        )
        if (existingIndex >= 0) {
            updated[existingIndex] = item
        } else {
            updated += item
        }
        _downloads.value = updated.sortedByDescending { it.downloadDate }
    }

    private fun updateTrackLike(trackId: Int) {
        val snapshot = _state.value
        val updatedQueue = snapshot.queue.map { track ->
            if (track.id == trackId) {
                track.copy(liked = !track.liked)
            } else {
                track
            }
        }
        val selected = snapshot.selectedTrack?.let { track ->
            if (track.id == trackId) track.copy(liked = !track.liked) else track
        }
        _state.value = snapshot.copy(
            queue = updatedQueue,
            selectedTrack = selected
        )
    }

    private fun handlePlaybackEnded() {
        if (_state.value.loop) {
            return
        }
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val hasNext = _state.value.currentIndex < queue.lastIndex || (_state.value.random && queue.size > 1)
        if (hasNext) {
            next()
        } else {
            player.pause()
            player.seekTo(0L)
            _state.value = _state.value.copy(
                playing = false,
                currentTimeSeconds = 0.0
            )
        }
    }

    private fun seekToQueueIndex(index: Int) {
        val queue = _state.value.queue
        val normalizedIndex = index.coerceIn(0, queue.lastIndex)
        val track = queue.getOrNull(normalizedIndex) ?: return
        if (track.originalFileId > 0) {
            downloadManager.startDownload(track.originalFileId)
        }
        MusicPlaybackService.start(context)
        player.seekToDefaultPosition(normalizedIndex)
        player.playWhenReady = true
        _state.value = _state.value.copy(
            selectedTrack = track,
            currentIndex = normalizedIndex,
            currentTimeSeconds = 0.0,
            desiredSeekSeconds = null,
            pendingTrackId = track.id,
            isDownloading = !downloadManager.isFullyCached(track.originalFileId)
        )
        syncProgressLoop()
    }

    private fun syncSelectedTrackFromPlayer(resetProgress: Boolean) {
        val snapshot = _state.value
        val queue = snapshot.queue
        if (queue.isEmpty()) return

        val trackId = player.currentMediaItem?.mediaId?.toIntOrNull()
        val resolvedIndex = when {
            trackId != null -> queue.indexOfFirst { it.id == trackId }
            player.currentMediaItemIndex in queue.indices -> player.currentMediaItemIndex
            else -> -1
        }.takeIf { it in queue.indices } ?: snapshot.currentIndex.coerceIn(0, queue.lastIndex)

        val selectedTrack = queue.getOrNull(resolvedIndex) ?: snapshot.selectedTrack ?: return
        if (selectedTrack.originalFileId > 0) {
            downloadManager.startDownload(selectedTrack.originalFileId)
        }

        _state.value = snapshot.copy(
            selectedTrack = selectedTrack,
            currentIndex = resolvedIndex,
            isSelected = true,
            currentTimeSeconds = if (resetProgress) {
                player.currentPosition.coerceAtLeast(0L) / 1000.0
            } else {
                snapshot.currentTimeSeconds
            },
            durationSeconds = safeDurationSeconds(),
            desiredSeekSeconds = if (resetProgress) {
                null
            } else {
                resolveDesiredSeek(snapshot.desiredSeekSeconds)
            },
            pendingTrackId = snapshot.pendingTrackId,
            isDownloading = !downloadManager.isFullyCached(selectedTrack.originalFileId),
            playing = currentVisualPlaying()
        )
    }

    private fun syncProgressLoop() {
        progressJob?.cancel()
        if (!_state.value.isSelected) return

        progressJob = scope.launch {
            while (true) {
                val selectedTrack = _state.value.selectedTrack
                if (selectedTrack == null) break
                val currentTimeSeconds = player.currentPosition.coerceAtLeast(0L) / 1000.0
                _state.value = _state.value.copy(
                    currentTimeSeconds = currentTimeSeconds,
                    durationSeconds = safeDurationSeconds(),
                    desiredSeekSeconds = resolveDesiredSeek(
                        desiredSeekSeconds = _state.value.desiredSeekSeconds,
                        currentTimeSeconds = currentTimeSeconds
                    ),
                    playing = currentVisualPlaying(),
                    isBuffering = player.playbackState == Player.STATE_BUFFERING,
                    isDownloading = !downloadManager.isFullyCached(selectedTrack.originalFileId)
                )
                delay(if (player.isPlaying) 250L else 500L)
            }
        }
    }

    private fun currentVisualPlaying(): Boolean {
        return player.isPlaying || (
            player.playWhenReady &&
                _state.value.desiredSeekSeconds != null
            )
    }

    private fun resolveDesiredSeek(
        desiredSeekSeconds: Double?,
        currentTimeSeconds: Double = player.currentPosition.coerceAtLeast(0L) / 1000.0
    ): Double? {
        val target = desiredSeekSeconds ?: return null
        return if (abs(currentTimeSeconds - target) <= 0.35) {
            null
        } else {
            target
        }
    }

    private fun safeDurationSeconds(): Double {
        val duration = player.duration
        return if (duration > 0L) duration / 1000.0 else (_state.value.selectedTrack?.metadata?.durationSeconds ?: 0.0)
    }

    private fun randomOtherIndex(currentIndex: Int, maxIndex: Int): Int {
        if (maxIndex <= 0) return 0
        var next = currentIndex
        repeat(8) {
            next = Random.nextInt(0, maxIndex + 1)
            if (next != currentIndex) return next
        }
        return (currentIndex + 1).mod(maxIndex + 1)
    }
}

private data class MusicDownloadSnapshot(
    val fileId: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val isComplete: Boolean,
    val hasError: Boolean
)

private class ProgressiveMusicDataSourceFactory(
    private val downloadManager: MusicProgressiveDownloadManager
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return ProgressiveMusicDataSource(downloadManager)
    }
}

private class ProgressiveMusicDataSource(
    private val downloadManager: MusicProgressiveDownloadManager
) : BaseDataSource(true) {
    private var currentUri: Uri? = null
    private var session: MusicProgressiveDownloadManager.DownloadSession? = null
    private var readPosition: Long = 0L
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        currentUri = dataSpec.uri
        val fileId = dataSpec.uri.lastPathSegment?.toIntOrNull()
            ?: dataSpec.uri.host?.toIntOrNull()
            ?: throw IOException("Invalid music uri: ${dataSpec.uri}")
        session = downloadManager.requirePreparedSession(fileId)
        downloadManager.startDownload(fileId)
        readPosition = dataSpec.position
        opened = true
        transferStarted(dataSpec)

        val target = session ?: throw IOException("Music session not found")
        return if (target.isComplete) {
            (target.meta.size - dataSpec.position).coerceAtLeast(0L)
        } else {
            C.LENGTH_UNSET.toLong()
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val target = session ?: return C.RESULT_END_OF_INPUT
        val read = target.readAt(
            position = readPosition,
            buffer = buffer,
            offset = offset,
            length = length
        )
        if (read > 0) {
            readPosition += read
            bytesTransferred(read)
        }
        return read
    }

    override fun close() {
        if (opened) {
            transferEnded()
        }
        opened = false
        currentUri = null
        session = null
    }

    override fun getUri(): Uri? = currentUri
}

private class MusicProgressiveDownloadManager(
    context: Context,
    private val gateway: MusicGateway,
    private val diskCache: ImageDiskCache,
    private val cacheIndexStore: MusicCacheIndexStore,
    private val scope: CoroutineScope,
    private val onSnapshot: (MusicDownloadSnapshot) -> Unit
) {
    data class DownloadSession(
        val meta: StorageFileMeta,
        val tempFile: File,
        private val diskCache: ImageDiskCache,
        private val cacheIndexStore: MusicCacheIndexStore,
        private val onSnapshot: (MusicDownloadSnapshot) -> Unit
    ) {
        private val waitLock = Object()
        private val ioLock = Mutex()
        @Volatile
        private var readFile: File = tempFile

        @Volatile
        var downloadedBytes: Long = tempFile.length().coerceAtLeast(0L)

        @Volatile
        var isComplete: Boolean = false

        @Volatile
        var failure: Throwable? = null

        fun notifyChanged() {
            synchronized(waitLock) {
                waitLock.notifyAll()
            }
        }

        private fun emitSnapshot() {
            onSnapshot(
                MusicDownloadSnapshot(
                    fileId = meta.fileId,
                    downloadedBytes = downloadedBytes,
                    totalBytes = meta.size,
                    isComplete = isComplete,
                    hasError = failure != null
                )
            )
        }

        fun markComplete() {
            isComplete = true
            failure = null
            cacheIndexStore.write(
                MusicCacheIndexStore.Entry(
                    fileId = meta.fileId,
                    cacheKey = meta.cacheKey,
                    mime = meta.mime,
                    size = meta.size
                )
            )
            diskCache.writeFromFile(meta.cacheKey, tempFile)
            val cachedFile = diskCache.resolveFile(meta.cacheKey)
            if (cachedFile != null) {
                readFile = cachedFile
            }
            if (tempFile != readFile && tempFile.exists()) {
                runCatching { tempFile.delete() }
            }
            emitSnapshot()
            notifyChanged()
        }

        suspend fun appendChunk(offset: Long, bytes: ByteArray) {
            if (bytes.isEmpty()) {
                throw IOException("Received empty music chunk")
            }
            ioLock.withLock {
                if (offset != downloadedBytes) {
                    throw IOException(
                        "Unexpected music chunk offset: expected=$downloadedBytes actual=$offset"
                    )
                }
                RandomAccessFile(tempFile, "rw").use { raf ->
                    raf.seek(offset)
                    raf.write(bytes)
                }
                downloadedBytes = offset + bytes.size
            }
            emitSnapshot()
            notifyChanged()
        }

        fun markFailure(error: Throwable) {
            isComplete = false
            failure = error
            emitSnapshot()
            notifyChanged()
        }

        suspend fun resetForRetry() {
            ioLock.withLock {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                tempFile.parentFile?.mkdirs()
                tempFile.createNewFile()
                readFile = tempFile
                downloadedBytes = 0L
                isComplete = false
                failure = null
            }
            emitSnapshot()
            notifyChanged()
        }

        fun readAt(
            position: Long,
            buffer: ByteArray,
            offset: Int,
            length: Int
        ): Int {
            while (true) {
                failure?.let { throw IOException(it.message ?: "Music download failed", it) }

                val available = (downloadedBytes - position).coerceAtLeast(0L)
                if (available > 0L) {
                    val toRead = min(length.toLong(), available).toInt()
                    RandomAccessFile(readFile, "r").use { raf ->
                        raf.seek(position)
                        raf.readFully(buffer, offset, toRead)
                    }
                    return toRead
                }

                if (isComplete) {
                    return C.RESULT_END_OF_INPUT
                }

                synchronized(waitLock) {
                    waitLock.wait(150L)
                }
            }
        }
    }

    private val sessions = linkedMapOf<Int, DownloadSession>()
    private val jobs = linkedMapOf<Int, Job>()
    private val lock = Any()
    private val tempDir = File(context.cacheDir, "element_music_progressive").apply {
        mkdirs()
    }

    fun release() {
        synchronized(lock) {
            jobs.values.forEach { it.cancel() }
            jobs.clear()
            sessions.clear()
        }
    }

    fun isFullyCached(fileId: Int): Boolean {
        val session = synchronized(lock) { sessions[fileId] }
        if (session?.isComplete == true) return true

        val cacheKey = cacheIndexStore.read(fileId)?.cacheKey ?: return false
        return diskCache.resolveFile(cacheKey)?.exists() == true
    }

    suspend fun prepare(meta: StorageFileMeta, startDownload: Boolean = true): DownloadSession {
        synchronized(lock) {
            sessions[meta.fileId]?.let { session ->
                if (startDownload) {
                    ensureDownloadStarted(session)
                }
                return session
            }
        }

        val cachedFile = diskCache.resolveFile(meta.cacheKey)
        if (cachedFile != null && cachedFile.length() == meta.size) {
            return DownloadSession(
                meta = meta,
                tempFile = cachedFile,
                diskCache = diskCache,
                cacheIndexStore = cacheIndexStore,
                onSnapshot = onSnapshot
            ).also { session ->
                session.downloadedBytes = cachedFile.length()
                session.isComplete = true
                cacheIndexStore.write(
                    MusicCacheIndexStore.Entry(
                        fileId = meta.fileId,
                        cacheKey = meta.cacheKey,
                        mime = meta.mime,
                        size = meta.size
                    )
                )
                synchronized(lock) {
                    sessions[meta.fileId] = session
                }
                if (startDownload) {
                    onSnapshot(
                        MusicDownloadSnapshot(
                            fileId = meta.fileId,
                            downloadedBytes = session.downloadedBytes,
                            totalBytes = meta.size,
                            isComplete = true,
                            hasError = false
                        )
                    )
                }
            }
        }

        val tempFile = File(tempDir, "${meta.fileId}.part").apply {
            if (exists()) {
                delete()
            }
            parentFile?.mkdirs()
            createNewFile()
        }
        val session = DownloadSession(
            meta = meta,
            tempFile = tempFile,
            diskCache = diskCache,
            cacheIndexStore = cacheIndexStore,
            onSnapshot = onSnapshot
        )
        synchronized(lock) {
            sessions[meta.fileId] = session
            if (startDownload) {
                ensureDownloadStarted(session)
                onSnapshot(
                    MusicDownloadSnapshot(
                        fileId = meta.fileId,
                        downloadedBytes = session.downloadedBytes,
                        totalBytes = meta.size,
                        isComplete = session.isComplete,
                        hasError = false
                    )
                )
            }
        }
        return session
    }

    fun requirePreparedSession(fileId: Int): DownloadSession {
        return synchronized(lock) {
            sessions[fileId]
        } ?: throw IOException("Music session is not prepared for fileId=$fileId")
    }

    fun startDownload(fileId: Int) {
        val session = synchronized(lock) { sessions[fileId] } ?: return
        synchronized(lock) {
            ensureDownloadStarted(session)
        }
        onSnapshot(
            MusicDownloadSnapshot(
                fileId = session.meta.fileId,
                downloadedBytes = session.downloadedBytes,
                totalBytes = session.meta.size,
                isComplete = session.isComplete,
                hasError = session.failure != null
            )
        )
    }

    private fun ensureDownloadStarted(session: DownloadSession) {
        if (session.isComplete || jobs[session.meta.fileId] != null) {
            return
        }
        jobs[session.meta.fileId] = scope.launch {
            if (session.failure != null) {
                session.resetForRetry()
            }
            runDownload(session)
        }
    }

    private suspend fun runDownload(session: DownloadSession) {
        try {
            var offset = session.downloadedBytes
            while (offset < session.meta.size) {
                val result = gateway.downloadChunk(session.meta.fileId, offset)
                val chunk = result.data
                if (!result.isSuccess || chunk == null) {
                    throw IOException(result.message ?: "Chunk download failed")
                }
                if (chunk.bytes.isEmpty()) {
                    throw IOException("Received empty chunk")
                }
                session.appendChunk(chunk.offset, chunk.bytes)
                offset = chunk.offset + chunk.bytes.size
            }
            session.markComplete()
        } catch (error: Throwable) {
            session.markFailure(error)
        } finally {
            synchronized(lock) {
                jobs.remove(session.meta.fileId)
            }
        }
    }
}
