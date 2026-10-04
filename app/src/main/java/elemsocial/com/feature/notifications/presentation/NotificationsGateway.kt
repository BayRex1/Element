package elemsocial.com.feature.notifications.presentation

import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.NotificationsResult
import elemsocial.com.domain.repository.NotificationsRepository

class NotificationsGateway(
    private val notificationsRepository: NotificationsRepository
) {
    suspend fun loadNotifications(startIndex: Int): NotificationsResult {
        return notificationsRepository.loadNotifications(startIndex)
    }

    suspend fun viewNotifications(): ActionResult {
        return notificationsRepository.viewNotifications()
    }
}
