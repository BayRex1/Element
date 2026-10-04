package elemsocial.com.domain.repository

import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.NotificationsResult

interface NotificationsRepository {
    suspend fun loadNotifications(startIndex: Int): NotificationsResult
    suspend fun viewNotifications(): ActionResult
}
