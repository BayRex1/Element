package elemsocial.com.domain.repository

import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.MusicPlaylistDetail
import elemsocial.com.domain.model.MusicPlaylistPreview
import elemsocial.com.domain.model.MusicResult
import elemsocial.com.domain.model.MusicTrack
import elemsocial.com.domain.model.MusicUploadPayload

interface MusicRepository {
    suspend fun loadLibrary(): MusicResult<List<MusicPlaylistPreview>>
    suspend fun loadTracks(type: String, startIndex: Int): MusicResult<List<MusicTrack>>
    suspend fun loadTrack(trackId: Int): MusicResult<MusicTrack>
    suspend fun loadPlaylist(playlistId: String): MusicResult<MusicPlaylistDetail>
    suspend fun searchTracks(query: String): MusicResult<List<MusicTrack>>
    suspend fun toggleLike(trackId: Int): ActionResult
    suspend fun createPlaylist(name: String, description: String): MusicResult<Int>
    suspend fun deletePlaylist(playlistId: Int): ActionResult
    suspend fun addTrackToPlaylist(playlistId: Int, trackId: Int): ActionResult
    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Int): ActionResult
    suspend fun uploadSong(payload: MusicUploadPayload): ActionResult
}
