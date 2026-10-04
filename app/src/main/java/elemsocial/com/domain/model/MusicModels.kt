package elemsocial.com.domain.model

data class MusicTrackMetadata(
    val album: String? = null,
    val genre: String? = null,
    val trackNumber: Int? = null,
    val releaseYear: Int? = null,
    val composer: String? = null,
    val durationSeconds: Double? = null,
    val bitrate: Int? = null,
    val audioFormat: String? = null,
    val dateAdded: String? = null
)

data class MusicTrack(
    val id: Int,
    val title: String,
    val artist: String,
    val cover: PostImageAsset? = null,
    val originalFileId: Int,
    val liked: Boolean = false,
    val metadata: MusicTrackMetadata = MusicTrackMetadata()
)

data class MusicPlaylistPreview(
    val id: String,
    val numericId: Int? = null,
    val title: String,
    val authorName: String? = null,
    val description: String? = null,
    val cover: PostImageAsset? = null,
    val createDate: String? = null,
    val isFavorites: Boolean = false
)

data class MusicPlaylistDetail(
    val id: String,
    val numericId: Int? = null,
    val title: String,
    val description: String? = null,
    val createDate: String? = null,
    val isFavorites: Boolean = false,
    val songs: List<MusicTrack> = emptyList()
)

enum class MusicLibrarySectionKind(
    val apiValue: String?
) {
    My(apiValue = null),
    Latest(apiValue = "latest"),
    Random(apiValue = "random")
}

sealed class MusicLibrarySectionItem {
    data class Playlist(
        val value: MusicPlaylistPreview
    ) : MusicLibrarySectionItem()

    data class Track(
        val value: MusicTrack
    ) : MusicLibrarySectionItem()
}

data class MusicLibrarySection(
    val title: String,
    val kind: MusicLibrarySectionKind,
    val items: List<MusicLibrarySectionItem> = emptyList()
)

enum class MusicSearchMode {
    Favorites,
    New,
    Search
}

data class MusicPlayerState(
    val library: List<MusicPlaylistPreview> = emptyList(),
    val selectedTrack: MusicTrack? = null,
    val queue: List<MusicTrack> = emptyList(),
    val currentIndex: Int = 0,
    val isSelected: Boolean = false,
    val playing: Boolean = false,
    val random: Boolean = false,
    val loop: Boolean = false,
    val durationSeconds: Double = 0.0,
    val currentTimeSeconds: Double = 0.0,
    val desiredSeekSeconds: Double? = null,
    val pendingTrackId: Int? = null,
    val volume: Float = 1f,
    val fullPlayerOpen: Boolean = false,
    val isBuffering: Boolean = false,
    val isDownloading: Boolean = false
)

enum class MusicDownloadStatus {
    Downloading,
    Completed,
    Error
}

data class MusicDownloadItem(
    val fileId: Int,
    val track: MusicTrack,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val status: MusicDownloadStatus = MusicDownloadStatus.Downloading,
    val downloadDate: Long = 0L
)

data class StorageFileMeta(
    val fileId: Int,
    val path: String,
    val hashSha256: String,
    val size: Long,
    val mime: String
) {
    val cacheKey: String
        get() = "$path/$hashSha256"
}

data class StorageFileChunk(
    val fileId: Int,
    val offset: Long,
    val bytes: ByteArray,
    val totalSize: Long
)

data class MusicUploadPayload(
    val title: String,
    val artist: String,
    val album: String? = null,
    val trackNumber: String? = null,
    val genre: String? = null,
    val releaseYear: String? = null,
    val composer: String? = null,
    val audioBytes: ByteArray,
    val coverBytes: ByteArray? = null
)

data class MusicResult<T>(
    val status: String,
    val message: String? = null,
    val data: T? = null
) {
    val isSuccess: Boolean
        get() = status == "success"
}
