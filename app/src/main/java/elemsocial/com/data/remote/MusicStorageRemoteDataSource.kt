package elemsocial.com.data.remote

import elemsocial.com.core.ws.ElementSocketClient

class MusicStorageRemoteDataSource(
    private val socketClient: ElementSocketClient
) {
    suspend fun getFileMeta(fileId: Int): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "storage",
                "action" to "get_file_data",
                "payload" to mapOf(
                    "file_id" to fileId
                )
            ),
            timeoutMs = 30_000
        )
    }

    suspend fun downloadChunk(
        fileId: Int,
        offset: Long
    ): Map<String, Any?> {
        return socketClient.sendRequest(
            mapOf(
                "type" to "storage",
                "action" to "download",
                "payload" to mapOf(
                    "file_id" to fileId,
                    "offset" to offset
                )
            ),
            timeoutMs = 30_000
        )
    }
}
