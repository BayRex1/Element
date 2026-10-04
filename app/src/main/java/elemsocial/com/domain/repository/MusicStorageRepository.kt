package elemsocial.com.domain.repository

import elemsocial.com.domain.model.MusicResult
import elemsocial.com.domain.model.StorageFileChunk
import elemsocial.com.domain.model.StorageFileMeta

interface MusicStorageRepository {
    suspend fun getFileMeta(fileId: Int): MusicResult<StorageFileMeta>
    suspend fun downloadChunk(fileId: Int, offset: Long): MusicResult<StorageFileChunk>
}
