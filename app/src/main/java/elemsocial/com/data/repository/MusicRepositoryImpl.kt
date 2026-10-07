package elemsocial.com.data.repository

import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.data.remote.MusicRemoteDataSource
import elemsocial.com.data.remote.SocialRemoteDataSource
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.MusicPlaylistDetail
import elemsocial.com.domain.model.MusicPlaylistPreview
import elemsocial.com.domain.model.MusicResult
import elemsocial.com.domain.model.MusicTrack
import elemsocial.com.domain.model.MusicTrackMetadata
import elemsocial.com.domain.model.MusicUploadPayload
import elemsocial.com.domain.model.SearchCategory
import elemsocial.com.domain.repository.MusicRepository

class MusicRepositoryImpl(
    socketClient: ElementSocketClient
) : MusicRepository {
    private val remote = MusicRemoteDataSource(socketClient)
    private val socialRemote = SocialRemoteDataSource(socketClient)

    override suspend fun loadLibrary(): MusicResult<List<MusicPlaylistPreview>> {
        val response = remote.loadLibrary()
        return MusicResult(
            status = response["status"]?.toString() ?: "error",
            message = response["message"]?.toString(),
            data = parseMusicLibrary(response["playlists"])
        )
    }

    override suspend fun loadTracks(type: String, startIndex: Int): MusicResult<List<MusicTrack>> {
        val response = remote.loadTracks(type, startIndex)
        return MusicResult(
            status = response["status"]?.toString() ?: "error",
            message = response["message"]?.toString(),
            data = parseMusicTracks(response["songs"])
        )
    }

    override suspend fun loadTrack(trackId: Int): MusicResult<MusicTrack> {
        val response = remote.loadTrack(trackId)
        return MusicResult(
            status = response["status"]?.toString() ?: "error",
            message = response["message"]?.toString(),
            data = parseMusicTrack(response["song"])
        )
    }

    override suspend fun loadPlaylist(playlistId: String): MusicResult<MusicPlaylistDetail> {
        if (playlistId == FavoritesPlaylistId) {
            val favoriteTracks = loadTracks(type = "favorites", startIndex = 0)
            return MusicResult(
                status = favoriteTracks.status,
                message = favoriteTracks.message,
                data = MusicPlaylistDetail(
                    id = FavoritesPlaylistId,
                    title = "Избранное",
                    description = "Тут собраны ваши любимые песни :)",
                    isFavorites = true,
                    songs = favoriteTracks.data.orEmpty()
                )
            )
        }

        val numericId = playlistId.toIntOrNull()
            ?: return MusicResult(status = "error", message = "Плейлист не найден")
        val response = remote.loadPlaylist(numericId)
        val detailMap = response["playlist_data"].asRichMap()
        val detail = detailMap?.let {
            MusicPlaylistDetail(
                id = playlistId,
                numericId = numericId,
                title = it["title"]?.toString() ?: "Плейлист",
                description = it["description"]?.toString(),
                createDate = it["create_date"]?.toString(),
                songs = parseMusicTracks(response["songs"])
            )
        }

        return MusicResult(
            status = response["status"]?.toString() ?: "error",
            message = response["message"]?.toString(),
            data = detail
        )
    }

    override suspend fun searchTracks(query: String): MusicResult<List<MusicTrack>> {
        val response = socialRemote.search(SearchCategory.Music, query)
        val raw = response["results"] as? List<*> ?: emptyList<Any?>()
        val tracks = raw.mapNotNull { item ->
            val map = item.asRichMap() ?: return@mapNotNull null
            if (map["type"]?.toString() != "song") return@mapNotNull null
            parseMusicTrack(map)
        }
        return MusicResult(
            status = response["status"]?.toString() ?: "error",
            message = response["message"]?.toString(),
            data = tracks
        )
    }

    override suspend fun toggleLike(trackId: Int): ActionResult {
        val current = loadTrack(trackId).data
        val response = if (current?.liked == true) {
            remote.removeFavorite(trackId)
        } else {
            remote.addFavorite(trackId)
        }
        return actionResult(response)
    }

    override suspend fun createPlaylist(name: String, description: String): MusicResult<Int> {
        val response = remote.createPlaylist(name, description)
        return MusicResult(
            status = response["status"]?.toString() ?: "error",
            message = response["message"]?.toString(),
            data = response["playlist_id"].asInt()
        )
    }

    override suspend fun deletePlaylist(playlistId: Int): ActionResult {
        return actionResult(remote.deletePlaylist(playlistId))
    }

    override suspend fun addTrackToPlaylist(playlistId: Int, trackId: Int): ActionResult {
        return actionResult(remote.addTrackToPlaylist(playlistId, trackId))
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Int): ActionResult {
        return actionResult(remote.removeTrackFromPlaylist(playlistId, trackId))
    }

    override suspend fun uploadSong(payload: MusicUploadPayload): ActionResult {
        return actionResult(
            remote.uploadSong(
                mapOf(
                    "title" to payload.title,
                    "artist" to payload.artist,
                    "album" to payload.album,
                    "track_number" to payload.trackNumber,
                    "genre" to payload.genre,
                    "release_year" to payload.releaseYear,
                    "composer" to payload.composer,
                    "audio_file" to payload.audioBytes,
                    "cover_file" to payload.coverBytes
                )
            )
        )
    }

    companion object {
        const val FavoritesPlaylistId = "fav"
    }
}

internal fun parseMusicTracks(raw: Any?): List<MusicTrack> {
    val list = raw as? List<*> ?: return emptyList()
    return list.mapNotNull { parseMusicTrack(it) }
}

internal fun parseMusicTrack(raw: Any?): MusicTrack? {
    val map = raw.asRichMap() ?: return null
    val id = map["id"].asInt() ?: return null
    val title = map["title"]?.toString()?.takeIf { it.isNotBlank() } ?: return null
    val artist = map["artist"]?.toString()?.takeIf { it.isNotBlank() } ?: return null
    val originalFileId = map["original_file"].asInt(0) ?: 0

    return MusicTrack(
        id = id,
        title = title,
        artist = artist,
        cover = parseAsset(map["cover"]),
        originalFileId = originalFileId.coerceAtLeast(0),
        liked = map["liked"].asBoolean(),
        metadata = MusicTrackMetadata(
            album = map["album"]?.toString(),
            genre = map["genre"]?.toString(),
            trackNumber = map["track_number"].asInt(),
            releaseYear = map["release_year"].asInt(),
            composer = map["composer"]?.toString(),
            durationSeconds = map["duration"].asDouble(),
            bitrate = map["bitrate"].asInt(),
            audioFormat = map["audio_format"]?.toString(),
            dateAdded = map["date_added"]?.toString()
        )
    )
}

private fun parseMusicLibrary(raw: Any?): List<MusicPlaylistPreview> {
    val list = raw as? List<*> ?: return emptyList()
    return list.mapNotNull { item ->
        val map = item.asRichMap() ?: return@mapNotNull null
        val numericId = map["id"].asInt() ?: return@mapNotNull null
        val title = map["title"]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val authorName = map["author"].asRichMap()?.get("name")?.toString()

        MusicPlaylistPreview(
            id = numericId.toString(),
            numericId = numericId,
            title = title,
            authorName = authorName,
            createDate = map["add_date"]?.toString()
        )
    }
}

private fun actionResult(response: Map<String, Any?>): ActionResult {
    val status = response["status"]?.toString()?.lowercase()
    val normalized = when (status) {
        "success", "ok", "200", null, "" -> "success"
        else -> "error"
    }
    return ActionResult(
        status = normalized,
        message = response["message"]?.toString()
    )
}
