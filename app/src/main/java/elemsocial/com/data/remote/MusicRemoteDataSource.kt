package elemsocial.com.data.remote

import elemsocial.com.core.ws.ElementSocketClient

class MusicRemoteDataSource(
    private val socketClient: ElementSocketClient
) {
    suspend fun loadLibrary(): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "music/get_tracks",
                "payload" to mapOf("type" to "my")
            )
        )
    }

    suspend fun loadTracks(type: String, startIndex: Int): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "music/get_tracks",
                "payload" to mapOf(
                    "type" to type,
                    "start_index" to startIndex
                )
            )
        )
    }

    suspend fun loadTrack(trackId: Int): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "music/get_track",
                "payload" to mapOf("song_id" to trackId)
            )
        )
    }

    suspend fun loadPlaylist(playlistId: Int): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "music/get_song",
                "payload" to mapOf("song_id" to playlistId)
            )
        )
    }

    suspend fun toggleLike(trackId: Int): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "music/fav/add",
                "payload" to mapOf("song_id" to trackId)
            )
        )
    }

    suspend fun createPlaylist(
        name: String,
        description: String
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "music/playlists/create",
                "payload" to mapOf(
                    "name" to name,
                    "description" to description,
                    "privacy" to 0
                )
            )
        )
    }

    suspend fun deletePlaylist(playlistId: Int): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "music/playlists/delete",
                "payload" to mapOf(
                    "playlist_id" to playlistId
                )
            )
        )
    }

    suspend fun addTrackToPlaylist(
        playlistId: Int,
        trackId: Int
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "music/playlists/add_song",
                "payload" to mapOf(
                    "playlist_id" to playlistId,
                    "song_id" to trackId
                )
            )
        )
    }

    suspend fun removeTrackFromPlaylist(
        playlistId: Int,
        trackId: Int
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "music/playlists/remove_song",
                "payload" to mapOf(
                    "playlist_id" to playlistId,
                    "song_id" to trackId
                )
            )
        )
    }

    suspend fun uploadSong(payload: Map<String, Any?>): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "social",
                "action" to "music/upload",
                "payload" to payload
            ),
            timeoutMs = 120_000
        )
    }
}
