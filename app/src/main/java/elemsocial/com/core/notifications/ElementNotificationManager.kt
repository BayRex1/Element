package elemsocial.com.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import elemsocial.com.MainActivity
import elemsocial.com.R
import elemsocial.com.core.settings.AppSettingsStore
import elemsocial.com.domain.model.AppNotification
import java.util.concurrent.atomic.AtomicInteger

class ElementNotificationManager(
    private val context: Context,
    private val settingsStore: AppSettingsStore
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val platformNotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Уведомления Element (без звука)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Лайки, комментарии, сообщения и другие уведомления Element"
            enableVibration(true)
            setSound(null, null)
        }

        platformNotificationManager.createNotificationChannel(channel)
    }

    fun show(notification: AppNotification) {
        if (!settingsStore.getNotificationsToastEnabled()) return
        if (!hasPermission()) return

        ensureChannels()

        val content = ElementNotificationFormatter.buildSystemContent(notification)
        val systemNotificationId = nextSystemNotificationId()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            systemNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_notifications)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setSilent(true)

        notificationManager.notify(systemNotificationId, builder.build())
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private companion object {
        private const val CHANNEL_ID = "element.notifications.silent"
        private val NEXT_NOTIFICATION_ID = AtomicInteger(10_000)

        private fun nextSystemNotificationId(): Int {
            return NEXT_NOTIFICATION_ID.incrementAndGet()
        }
    }
}
