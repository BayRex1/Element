package elemsocial.com.data.repository

import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.data.remote.MusicStorageRemoteDataSource
import elemsocial.com.domain.model.MusicResult
import elemsocial.com.domain.model.StorageFileChunk
import elemsocial.com.domain.model.StorageFileMeta
import elemsocial.com.domain.repository.MusicStorageRepository

class MusicStorageRepositoryImpl(
    socketClient: ElementSocketClient
) : MusicStorageRepository {
    private val remote = MusicStorageRemoteDataSource(socketClient)

    override suspend fun getFileMeta(fileId: Int): MusicResult<StorageFileMeta> {
        val response = remote.getFileMeta(fileId)
        val map = response["file_data"].asRichMap()
        val meta = map?.let {
            val id = it["id"].asInt()
            val path = it["path"]?.toString()
            val hash = it["hash_sha256"]?.toString()
            val size = it["size"].asLong()
            val mime = it["mime"]?.toString()

            if (id == null || path.isNullOrBlank() || hash.isNullOrBlank() || size == null || mime.isNullOrBlank()) {
                null
            } else {
                StorageFileMeta(
                    fileId = id,
                    path = path,
                    hashSha256 = hash,
                    size = size,
                    mime = mime
                )
            }
        }

        return MusicResult(
            status = normalizeStorageStatus(response["status"]),
            message = response["message"]?.toString(),
            data = meta
        )
    }

    override suspend fun downloadChunk(fileId: Int, offset: Long): MusicResult<StorageFileChunk> {
        val response = remote.downloadChunk(fileId, offset)
        val bytes = response["buffer"] as? ByteArray ?: ByteArray(0)
        val chunk = if (normalizeStorageStatus(response["status"]) == "success") {
            StorageFileChunk(
                fileId = fileId,
                offset = response["offset"].asLong(offset) ?: offset,
                bytes = bytes,
                totalSize = response["total_size"].asLong(0L) ?: 0L
            )
        } else {
            null
        }

        return MusicResult(
            status = normalizeStorageStatus(response["status"]),
            message = response["message"]?.toString(),
            data = chunk
        )
    }
}

private fun normalizeStorageStatus(raw: Any?): String {
    return when (raw?.toString()?.lowercase()) {
        "200", "success", "ok" -> "success"
        else -> "error"
    }
}
