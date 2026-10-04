package elemsocial.com.domain.repository

import elemsocial.com.domain.model.SearchCategory
import elemsocial.com.domain.model.SearchResult

interface SearchRepository {
    suspend fun search(category: SearchCategory, value: String): SearchResult
}

