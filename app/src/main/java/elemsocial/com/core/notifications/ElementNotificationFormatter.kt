package elemsocial.com.core.notifications

import elemsocial.com.domain.model.AppNotification
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Locale

data class SystemNotificationContent(
    val title: String,
    val body: String
)

object ElementNotificationFormatter {
    fun buildSystemContent(notification: AppNotification): SystemNotificationContent {
        val action = notificationActionKey(notification)
        val authorName = notification.author?.name
            ?: notification.contentAuthor?.name
            ?: notification.author?.username
            ?: "Пользователь"

        return when (action) {
            "PostLike" -> SystemNotificationContent(
                title = "❤️ Новый лайк",
                body = "$authorName ставит лайк вашему посту"
            )

            "PostDislike" -> SystemNotificationContent(
                title = "👎 Дизлайк",
                body = "$authorName ставит дизлайк вашему посту"
            )

            "PostComment" -> SystemNotificationContent(
                title = "💬 Новый комментарий",
                body = "$authorName комментирует ваш пост ${quotedShortText(notification.commentText)}".trim()
            )

            "ReplyComment" -> SystemNotificationContent(
                title = "↩️ Ответ на комментарий",
                body = "$authorName отвечает на ваш комментарий ${quotedShortText(notification.commentText)}".trim()
            )

            "ProfileSubscribe" -> SystemNotificationContent(
                title = "🔔 Новый подписчик",
                body = "$authorName подписывается на вас"
            )

            "ProfileUnsubscribe" -> SystemNotificationContent(
                title = "🔕 Отписка",
                body = "$authorName отписывается от вас"
            )

            "Message" -> SystemNotificationContent(
                title = authorName,
                body = resolveMessageBody(notification)
            )

            "ReferralRewardInviter" -> SystemNotificationContent(
                title = "Реферальная награда",
                body = "Вам начислено ${formatAmount(notification.amount)} E за приглашённого пользователя"
            )

            "ReferralRewardInvited" -> SystemNotificationContent(
                title = "Бонус за приглашение",
                body = "Вам начислено ${formatAmount(notification.amount)} E за регистрацию по приглашению"
            )

            "NewPost" -> SystemNotificationContent(
                title = "🆕 Новый пост • $authorName",
                body = notification.postText.orEmpty()
            )

            "NewWallPost" -> SystemNotificationContent(
                title = "🆕 Новый пост на стене • $authorName",
                body = notification.postText.orEmpty()
            )

            "moderation_delete_post" -> SystemNotificationContent(
                title = "Модерация",
                body = "Ваш пост был удален модератором за нарушение правил сообщества"
            )

            "moderation_delete_comment" -> SystemNotificationContent(
                title = "Модерация",
                body = "Ваш комментарий был удален модератором за нарушение правил сообщества"
            )

            "punishment_applied" -> SystemNotificationContent(
                title = notification.title?.takeIf { it.isNotBlank() } ?: "Наложено ограничение",
                body = buildPunishmentAppliedText(notification)
            )

            "punishment_lifted" -> SystemNotificationContent(
                title = notification.title?.takeIf { it.isNotBlank() } ?: "Ограничение снято",
                body = notification.message?.takeIf { it.isNotBlank() } ?: "Ограничение автоматически снято"
            )

            "permissions_updated" -> SystemNotificationContent(
                title = "Права обновлены",
                body = "Ваши права доступа были обновлены администратором"
            )

            "post_deleted" -> SystemNotificationContent(
                title = "Пост удален",
                body = notification.message?.takeIf { it.isNotBlank() } ?: "Ваш пост был удален модератором"
            )

            "comment_deleted" -> SystemNotificationContent(
                title = "Комментарий удален",
                body = notification.message?.takeIf { it.isNotBlank() } ?: "Ваш комментарий был удален модератором"
            )

            "permissions_changed" -> SystemNotificationContent(
                title = "Изменение разрешений",
                body = notification.message?.takeIf { it.isNotBlank() } ?: "Настройки разрешений обновлены"
            )

            else -> SystemNotificationContent(
                title = "🔔 Новое уведомление",
                body = "$authorName выполнил действие: ${notification.action}"
            )
        }
    }

    private fun resolveMessageBody(notification: AppNotification): String {
        return when (notification.messageType?.lowercase(Locale.ROOT)) {
            "voice" -> "\uD83C\uDFA4 Голосовое сообщение"
            "video" -> "\uD83C\uDFA5 Видео сообщение"
            "image" -> "\uD83D\uDDBC\uFE0F Фото"
            "file" -> "\uD83D\uDCCE Файл"
            else -> notification.messageText?.takeIf { it.isNotBlank() } ?: "Новое сообщение"
        }
    }

    private fun notificationActionKey(notification: AppNotification): String {
        return if (notification.action == "notification" && !notification.subtype.isNullOrBlank()) {
            notification.subtype
        } else {
            notification.action
        }.orEmpty()
    }

    private fun quotedShortText(value: String?): String {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return ""
        return if (text.length > 30) {
            "\"${text.take(30)}...\""
        } else {
            "\"$text\""
        }
    }

    private fun formatAmount(amount: Double?): String {
        val value = amount ?: 0.0
        return String.format(Locale.US, "%.3f", value)
    }

    private fun buildPunishmentAppliedText(notification: AppNotification): String {
        val base = notification.message?.takeIf { it.isNotBlank() } ?: "На ваш аккаунт наложено ограничение"
        val suffix = punishmentTimeLeft(notification.endDate, notification.durationHours)
        return "$base$suffix"
    }

    private fun punishmentTimeLeft(endDate: String?, durationHours: Int?): String {
        val date = endDate?.trim().orEmpty()
        if (date.isNotEmpty()) {
            val end = parseOffsetDateTimeOrNull(date)
            if (end != null) {
                val diff = Duration.between(OffsetDateTime.now(), end)
                return if (diff.isNegative || diff.isZero) {
                    " (истекло)"
                } else {
                    val hours = diff.toHours()
                    val minutes = (diff.toMinutes() % 60).toInt()
                    when {
                        hours > 0 -> " (осталось: $hours ч. $minutes мин.)"
                        minutes > 0 -> " (осталось: $minutes мин.)"
                        else -> " (менее минуты)"
                    }
                }
            }
        }
        return if (durationHours != null && durationHours > 0) {
            " (срок: $durationHours ч.)"
        } else {
            ""
        }
    }

    private fun parseOffsetDateTimeOrNull(raw: String): OffsetDateTime? {
        return try {
            OffsetDateTime.parse(raw)
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(raw.replace(" ", "T"))
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
