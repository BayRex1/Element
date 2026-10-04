package elemsocial.com.feature.music.presentation

import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.MusicPlaylistDetail
import elemsocial.com.domain.model.MusicPlaylistPreview
import elemsocial.com.domain.model.MusicResult
import elemsocial.com.domain.model.MusicTrack
import elemsocial.com.domain.model.MusicUploadPayload
import elemsocial.com.domain.model.StorageFileChunk
import elemsocial.com.domain.model.StorageFileMeta
import elemsocial.com.domain.repository.MusicRepository
import elemsocial.com.domain.repository.MusicStorageRepository

class MusicGateway(
    private val musicRepository: MusicRepository,
    private val musicStorageRepository: MusicStorageRepository
) {
    private val trackLikeOverrides = mutableMapOf<Int, Boolean>()

    suspend fun loadLibrary(): MusicResult<List<MusicPlaylistPreview>> {
        return musicRepository.loadLibrary()
    }

    suspend fun loadTracks(type: String, startIndex: Int = 0): MusicResult<List<MusicTrack>> {
        val result = musicRepository.loadTracks(type, startIndex)
        val adjusted = applyTrackLikeOverrides(result.data.orEmpty())
        return result.copy(
            data = if (type == "favorites") {
                adjusted.filter { it.liked }
            } else {
                adjusted
            }
        )
    }

    suspend fun loadTrack(trackId: Int): MusicResult<MusicTrack> {
        val result = musicRepository.loadTrack(trackId)
        return result.copy(data = result.data?.let(::applyTrackLikeOverride))
    }

    suspend fun loadPlaylist(playlistId: String): MusicResult<MusicPlaylistDetail> {
        val result = musicRepository.loadPlaylist(playlistId)
        return result.copy(
            data = result.data?.let { detail ->
                val adjustedSongs = applyTrackLikeOverrides(detail.songs)
                detail.copy(
                    songs = if (detail.isFavorites) {
                        adjustedSongs.filter { it.liked }
                    } else {
                        adjustedSongs
                    }
                )
            }
        )
    }

    suspend fun searchTracks(query: String): MusicResult<List<MusicTrack>> {
        val result = musicRepository.searchTracks(query)
        return result.copy(data = applyTrackLikeOverrides(result.data.orEmpty()))
    }

    suspend fun toggleLike(trackId: Int): ActionResult {
        return musicRepository.toggleLike(trackId)
    }

    fun applyTrackLikeState(trackId: Int, liked: Boolean) {
        trackLikeOverrides[trackId] = liked
    }

    suspend fun createPlaylist(name: String, description: String): MusicResult<Int> {
        return musicRepository.createPlaylist(name, description)
    }

    suspend fun deletePlaylist(playlistId: Int): ActionResult {
        return musicRepository.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Int, trackId: Int): ActionResult {
        return musicRepository.addTrackToPlaylist(playlistId, trackId)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Int): ActionResult {
        return musicRepository.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun uploadSong(payload: MusicUploadPayload): ActionResult {
        return musicRepository.uploadSong(payload)
    }

    suspend fun getFileMeta(fileId: Int): MusicResult<StorageFileMeta> {
        return musicStorageRepository.getFileMeta(fileId)
    }

    suspend fun downloadChunk(fileId: Int, offset: Long): MusicResult<StorageFileChunk> {
        return musicStorageRepository.downloadChunk(fileId, offset)
    }

    private fun applyTrackLikeOverrides(tracks: List<MusicTrack>): List<MusicTrack> {
        return tracks.map(::applyTrackLikeOverride)
    }

    private fun applyTrackLikeOverride(track: MusicTrack): MusicTrack {
        val override = trackLikeOverrides[track.id] ?: return track
        return if (track.liked == override) {
            track
        } else {
            track.copy(liked = override)
        }
    }
}
