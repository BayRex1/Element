package elemsocial.com.feature.notifications.presentation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.R
import elemsocial.com.core.time.formatTimeAge
import elemsocial.com.domain.model.AppNotification
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.feature.home.presentation.HomeGateway
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

private const val PAGE_SIZE = 10

@Composable
fun NotificationsScreen(
    notificationsGateway: NotificationsGateway,
    homeGateway: HomeGateway,
    onOpenProfile: (String) -> Unit,
    onOpenPost: (Int) -> Unit,
    onOpenMessenger: () -> Unit,
    onOpenWallet: () -> Unit,
    onUnreadCountChange: (Int) -> Unit = {},
    onViewed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var unreadNotifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var readNotifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var initialLoading by remember { mutableStateOf(false) }
    var pageLoading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun upsert(target: List<AppNotification>, incoming: List<AppNotification>): List<AppNotification> {
        return (target + incoming).distinctBy { it.id }
    }

    fun load(reset: Boolean) {
        if (reset) {
            if (initialLoading || pageLoading) return
        } else {
            if (initialLoading || pageLoading || !hasMore) return
        }

        val startIndex = if (reset) 0 else unreadNotifications.size + readNotifications.size
        if (reset) {
            initialLoading = true
            errorText = null
            hasMore = true
        } else {
            pageLoading = true
        }

        scope.launch {
            val result = runCatching { notificationsGateway.loadNotifications(startIndex) }.getOrNull()
            if (result?.isSuccess == true) {
                val chunk = result.notifications
                val unreadChunk = chunk.filter { !it.viewed }
                val readChunk = chunk.filter { it.viewed }

                if (reset) {
                    unreadNotifications = unreadChunk
                    readNotifications = readChunk
                } else {
                    unreadNotifications = upsert(unreadNotifications, unreadChunk)
                    readNotifications = upsert(readNotifications, readChunk)
                }

                hasMore = chunk.size >= PAGE_SIZE
                errorText = null
            } else {
                if (reset) {
                    unreadNotifications = emptyList()
                    readNotifications = emptyList()
                }
                errorText = result?.message ?: "Не удалось загрузить уведомления"
                hasMore = false
            }

            initialLoading = false
            pageLoading = false
        }
    }

    LaunchedEffect(Unit) {
        load(reset = true)
        val viewResult = runCatching { notificationsGateway.viewNotifications() }.getOrNull()
        if (viewResult?.isSuccess == true) {
            onUnreadCountChange(0)
            onViewed()
        }
    }

    LaunchedEffect(listState, initialLoading, pageLoading, hasMore) {
        snapshotFlow {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to total
        }.collect { (lastVisible, total) ->
            if (total <= 0) return@collect
            if (lastVisible >= total - 2 && !initialLoading && !pageLoading && hasMore) {
                load(reset = false)
            }
        }
    }

    val totalNotifications = unreadNotifications.size + readNotifications.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ElementUiPalette.Body)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 5.dp, end = 5.dp, top = 5.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (initialLoading) {
                items(9) {
                    NotificationSkeleton()
                }
            } else {
                if (totalNotifications == 0) {
                    item {
                        UIKit.Block(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                Text(
                                    text = "Ой, а тут пусто",
                                    color = ElementUiPalette.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = errorText ?: "Уведомлений пока нет",
                                    color = ElementUiPalette.TextLite,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = unreadNotifications,
                        key = { "unread-${it.id}" }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            homeGateway = homeGateway,
                            isHighlighted = true,
                            onOpenProfile = onOpenProfile,
                            onOpenPost = onOpenPost,
                            onOpenMessenger = onOpenMessenger,
                            onWalletUnavailable = onOpenWallet
                        )
                    }
                    if (unreadNotifications.isNotEmpty() && readNotifications.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(1.dp)) }
                    }

                    items(
                        items = readNotifications,
                        key = { "read-${it.id}" }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            homeGateway = homeGateway,
                            isHighlighted = false,
                            onOpenProfile = onOpenProfile,
                            onOpenPost = onOpenPost,
                            onOpenMessenger = onOpenMessenger,
                            onWalletUnavailable = onOpenWallet
                        )
                    }
                }
            }

            if (pageLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        UIKit.Loader(size = 22)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    homeGateway: HomeGateway,
    isHighlighted: Boolean,
    onOpenProfile: (String) -> Unit,
    onOpenPost: (Int) -> Unit,
    onOpenMessenger: () -> Unit,
    onWalletUnavailable: () -> Unit
) {
    val content = remember(
        notification.id,
        notification.action,
        notification.subtype,
        notification.commentText,
        notification.messageText,
        notification.postText,
        notification.amount,
        notification.title,
        notification.message,
        notification.endDate,
        notification.durationHours
    ) {
        buildNotificationContent(notification)
    }
    val clickable = remember(notification.id, notification.action, notification.postId, notification.profileUsername) {
        resolveNotificationClick(notification)
    }

    val shape = RoundedCornerShape(10.dp)
    val outerModifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .then(
            if (isHighlighted) {
                Modifier.border(1.dp, ElementUiPalette.Accent, shape)
            } else {
                Modifier
            }
        )
        .background(ElementUiPalette.Block)
        .clickable(enabled = clickable != NotificationClick.None) {
            when (clickable) {
                is NotificationClick.OpenPost -> onOpenPost(clickable.postId)
                is NotificationClick.OpenProfile -> onOpenProfile(clickable.username)
                NotificationClick.OpenWallet -> onWalletUnavailable()
                NotificationClick.OpenMessenger -> onOpenMessenger()
                NotificationClick.None -> Unit
            }
        }

    Box(
        modifier = outerModifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    NotificationAvatar(
                        name = notification.author?.name ?: notification.contentAuthor?.name ?: content.title,
                        avatar = notification.author?.avatar ?: notification.contentAuthor?.avatar,
                        homeGateway = homeGateway,
                        modifier = Modifier.size(32.dp)
                    )
                    content.icon?.let { icon ->
                        NotificationBadge(
                            type = icon,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = notification.author?.name ?: content.title.ifBlank { "Система" },
                        color = ElementUiPalette.TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = content.text,
                        color = ElementUiPalette.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = formatTimeAge(notification.date),
                color = ElementUiPalette.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NotificationSkeleton() {
    UIKit.SkeletonNotification(modifier = Modifier.fillMaxWidth())
}

@Composable
private fun NotificationAvatar(
    name: String,
    avatar: PostImageAsset?,
    homeGateway: HomeGateway,
    modifier: Modifier = Modifier
) {
    val bitmap by rememberNotificationImageBitmap(avatar, homeGateway)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF7F6EB0), Color(0xFFA19DB1)))),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun NotificationBadge(
    type: NotificationIconType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(17.dp)
            .clip(CircleShape)
            .background(ElementUiPalette.Block)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            NotificationIconType.Like -> Image(
                painter = painterResource(id = R.drawable.ntf_like),
                contentDescription = null,
                modifier = Modifier.size(13.dp)
            )

            NotificationIconType.Dislike -> Image(
                painter = painterResource(id = R.drawable.ntf_dislike),
                contentDescription = null,
                modifier = Modifier.size(13.dp)
            )

            NotificationIconType.Bubble -> Image(
                painter = painterResource(id = R.drawable.ntf_bubble),
                contentDescription = null,
                modifier = Modifier.size(13.dp)
            )

            NotificationIconType.Plus -> Image(
                painter = painterResource(id = R.drawable.ntf_plus),
                contentDescription = null,
                modifier = Modifier.size(13.dp)
            )

            NotificationIconType.Minus -> Image(
                painter = painterResource(id = R.drawable.ntf_minus),
                contentDescription = null,
                modifier = Modifier.size(13.dp)
            )

            NotificationIconType.Warning -> Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                tint = ElementUiPalette.Error,
                modifier = Modifier.size(13.dp)
            )

            NotificationIconType.Success -> Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = ElementUiPalette.Success,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun rememberNotificationImageBitmap(
    avatar: PostImageAsset?,
    homeGateway: HomeGateway
) = produceState<ImageBitmap?>(initialValue = null, key1 = avatar?.cacheKey) {
    val target = avatar ?: return@produceState
    val bytes = runCatching { homeGateway.loadImageBytes(target) }.getOrNull() ?: return@produceState
    if (bytes.isEmpty()) return@produceState
    value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}

private data class NotificationContent(
    val title: String,
    val text: String,
    val icon: NotificationIconType?
)

private sealed interface NotificationClick {
    data object None : NotificationClick
    data class OpenPost(val postId: Int) : NotificationClick
    data class OpenProfile(val username: String) : NotificationClick
    data object OpenMessenger : NotificationClick
    data object OpenWallet : NotificationClick
}

private enum class NotificationIconType {
    Like,
    Dislike,
    Bubble,
    Plus,
    Minus,
    Warning,
    Success
}

private fun resolveNotificationClick(notification: AppNotification): NotificationClick {
    val action = notification.action

    return when (action) {
        "PostLike", "PostDislike", "PostComment", "ReplyComment", "NewPost", "NewWallPost" -> {
            notification.postId?.let(NotificationClick::OpenPost)
                ?: notification.profileUsername
                    ?.takeIf { it.isNotBlank() }
                    ?.let(NotificationClick::OpenProfile)
                ?: NotificationClick.None
        }

        "ProfileSubscribe", "ProfileUnsubscribe" -> {
            notification.profileUsername
                ?.takeIf { it.isNotBlank() }
                ?.let(NotificationClick::OpenProfile)
                ?: NotificationClick.None
        }

        "Message" -> NotificationClick.OpenMessenger
        "ReferralRewardInviter", "ReferralRewardInvited" -> NotificationClick.OpenWallet
        else -> NotificationClick.None
    }
}

private fun buildNotificationContent(notification: AppNotification): NotificationContent {
    val action = notificationActionKey(notification)
    val actorName = notification.author?.name ?: notification.contentAuthor?.name ?: "Система"
    val quotedComment = notification.commentText.toQuotedGuillemets()

    return when (action) {
        "PostLike" -> NotificationContent(
            title = "Новый лайк!",
            text = "ставит лайк на ваш пост",
            icon = NotificationIconType.Like,
        )

        "PostDislike" -> NotificationContent(
            title = "Похоже вы сделали что-то не так...",
            text = "ставит дизлайк на ваш пост",
            icon = NotificationIconType.Dislike,
        )

        "PostComment" -> NotificationContent(
            title = "Новый комментарий!",
            text = "комментирует ваш пост $quotedComment".trim(),
            icon = NotificationIconType.Bubble,
        )

        "ReplyComment" -> NotificationContent(
            title = "Вам ответили",
            text = "отвечает на ваш комментарий $quotedComment".trim(),
            icon = NotificationIconType.Bubble,
        )

        "Message" -> NotificationContent(
            title = actorName,
            text = notification.messageText?.takeIf { it.isNotBlank() } ?: "Новое сообщение",
            icon = NotificationIconType.Bubble,
        )

        "ProfileSubscribe" -> NotificationContent(
            title = "Новый подписчик!",
            text = "подписывается на вас",
            icon = NotificationIconType.Plus,
        )

        "ProfileUnsubscribe" -> NotificationContent(
            title = "Подписка отменена",
            text = "больше не ваш подписчик",
            icon = NotificationIconType.Minus,
        )

        "ReferralRewardInviter" -> NotificationContent(
            title = "Реферальная награда",
            text = "Вам начислено ${formatAmount(notification.amount)} E за приглашённого пользователя",
            icon = NotificationIconType.Plus,
        )

        "ReferralRewardInvited" -> NotificationContent(
            title = "Бонус за приглашение",
            text = "Вам начислено ${formatAmount(notification.amount)} E за регистрацию по приглашению",
            icon = NotificationIconType.Plus,
        )

        "NewPost" -> NotificationContent(
            title = "Новый пост от $actorName",
            text = notification.postText.orEmpty(),
            icon = NotificationIconType.Bubble,
        )

        "NewWallPost" -> NotificationContent(
            title = "Новый пост на стене от $actorName",
            text = notification.postText.orEmpty(),
            icon = NotificationIconType.Bubble,
        )

        "moderation_delete_post" -> NotificationContent(
            title = "Модерация",
            text = "Ваш пост был удален за нарушение правил",
            icon = NotificationIconType.Warning,
        )

        "moderation_delete_comment" -> NotificationContent(
            title = "Модерация",
            text = "Ваш комментарий был удален за нарушение правил",
            icon = NotificationIconType.Warning,
        )

        "punishment_applied" -> NotificationContent(
            title = notification.title?.takeIf { it.isNotBlank() } ?: "Наложено ограничение",
            text = buildPunishmentAppliedText(notification),
            icon = NotificationIconType.Warning,
        )

        "punishment_lifted" -> NotificationContent(
            title = notification.title?.takeIf { it.isNotBlank() } ?: "Ограничение снято",
            text = notification.message?.takeIf { it.isNotBlank() } ?: "Ограничение снято",
            icon = NotificationIconType.Success,
        )

        "permissions_updated" -> NotificationContent(
            title = "Права обновлены",
            text = "Ваши права доступа были обновлены администратором",
            icon = NotificationIconType.Success,
        )

        "post_deleted" -> NotificationContent(
            title = "Пост удален",
            text = notification.message?.takeIf { it.isNotBlank() } ?: "Ваш пост был удален модератором",
            icon = NotificationIconType.Warning,
        )

        "comment_deleted" -> NotificationContent(
            title = "Комментарий удален",
            text = notification.message?.takeIf { it.isNotBlank() } ?: "Ваш комментарий был удален модератором",
            icon = NotificationIconType.Warning,
        )

        "permissions_changed" -> NotificationContent(
            title = "Изменение разрешений",
            text = notification.message?.takeIf { it.isNotBlank() } ?: "Настройки разрешений обновлены",
            icon = NotificationIconType.Success,
        )

        else -> NotificationContent(
            title = "Неизвестное уведомление",
            text = notification.message?.takeIf { it.isNotBlank() } ?: "",
            icon = null,
        )
    }
}

private fun notificationActionKey(notification: AppNotification): String {
    return if (notification.action == "notification" && !notification.subtype.isNullOrBlank()) {
        notification.subtype
    } else {
        notification.action
    }.orEmpty()
}

private fun String?.toQuotedGuillemets(): String {
    val value = this?.trim().orEmpty()
    if (value.isEmpty()) return ""
    return "«$value»"
}

private fun formatAmount(amount: Double?): String {
    val value = amount ?: 0.0
    return String.format(java.util.Locale.US, "%.3f", value)
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
