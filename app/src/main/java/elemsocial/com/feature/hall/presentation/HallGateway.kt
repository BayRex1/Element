package elemsocial.com.feature.hall.presentation

import elemsocial.com.domain.model.HallResult
import elemsocial.com.domain.repository.HallRepository

class HallGateway(
    private val hallRepository: HallRepository
) {
    suspend fun loadHall(startIndex: Int = 0): HallResult {
        return hallRepository.loadHall(startIndex)
    }
}
