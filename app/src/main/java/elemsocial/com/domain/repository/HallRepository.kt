package elemsocial.com.domain.repository

import elemsocial.com.domain.model.HallResult

interface HallRepository {
    suspend fun loadHall(startIndex: Int = 0): HallResult
}
