package elemsocial.com.feature.search.presentation

import elemsocial.com.domain.model.SearchCategory
import elemsocial.com.domain.model.SearchResult
import elemsocial.com.domain.repository.SearchRepository

class SearchGateway(
    private val searchRepository: SearchRepository
) {
    suspend fun search(category: SearchCategory, value: String): SearchResult {
        return searchRepository.search(category, value)
    }
}

