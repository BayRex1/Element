package elemsocial.com.feature.music.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.ui.PlayerNotificationManager
import elemsocial.com.MainActivity
import elemsocial.com.R
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MUSIC_NOTIFICATION_CHANNEL_ID = "element.music.playback"
private const val MUSIC_NOTIFICATION_ID = 4401

class MusicPlaybackService : Service() {
    private var notificationManager: PlayerNotificationManager? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notificationRefreshJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()

        val runtime = MusicRuntime.peek() ?: run {
            stopSelf()
            return
        }

        val mediaDescriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun createCurrentContentIntent(player: Player): PendingIntent {
                val intent = Intent(this@MusicPlaybackService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                return PendingIntent.getActivity(
                    this@MusicPlaybackService,
                    4001,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            override fun getCurrentContentTitle(player: Player): CharSequence {
                return runtime.controller.state.value.selectedTrack?.title ?: "Музыка"
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                return runtime.controller.state.value.selectedTrack?.artist
            }

            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback
            ): Bitmap? = null
        }

        val customActionReceiver = object : PlayerNotificationManager.CustomActionReceiver {
            override fun createCustomActions(
                context: Context,
                instanceId: Int
            ): Map<String, NotificationCompat.Action> {
                return emptyMap()
            }

            override fun getCustomActions(player: Player): List<String> {
                return emptyList()
            }

            override fun onCustomAction(player: Player, action: String, intent: Intent) {
                Unit
            }
        }

        mediaSession = MediaSession(this, "ElementMusicSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    MusicPlaybackService.start(this@MusicPlaybackService)
                    runtime.controller.playerHandle.play()
                    updateMediaSessionState(runtime)
                }

                override fun onPause() {
                    runtime.controller.playerHandle.pause()
                    updateMediaSessionState(runtime)
                }

                override fun onSkipToPrevious() {
                    runtime.controller.previous()
                    updateMediaSessionState(runtime)
                }

                override fun onSkipToNext() {
                    runtime.controller.next()
                    updateMediaSessionState(runtime)
                }

                override fun onSeekTo(pos: Long) {
                    runtime.controller.seekTo(pos / 1000.0)
                    updateMediaSessionState(runtime)
                }

                override fun onStop() {
                    runtime.controller.playerHandle.pause()
                    updateMediaSessionState(runtime)
                }
            })
            isActive = true
        }
        val platformSessionToken = mediaSession?.sessionToken ?: return

        notificationManager = ElementMusicPlayerNotificationManager(
            context = this,
            runtime = runtime,
            notificationListener = object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        startForeground(notificationId, notification)
                    } else {
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                }
            },
            mediaDescriptionAdapter = mediaDescriptionAdapter,
            customActionReceiver = customActionReceiver
        ).apply {
            setUseNextAction(true)
            setUsePreviousAction(true)
            setUseNextActionInCompactView(true)
            setUsePreviousActionInCompactView(true)
            setUseFastForwardAction(false)
            setUseRewindAction(false)
            setUseStopAction(false)
            setUseChronometer(false)
            setColorized(false)
            setMediaSessionToken(platformSessionToken)
            setPlayer(runtime.controller.playerHandle)
        }

        updateMediaSessionState(runtime)

        notificationRefreshJob = serviceScope.launch {
            while (true) {
                updateMediaSessionState(runtime)
                notificationManager?.invalidate()
                delay(900L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = MusicRuntime.peek()?.controller?.playerHandle
        if (player?.isPlaying != true) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        notificationRefreshJob?.cancel()
        serviceScope.cancel()
        notificationManager?.setPlayer(null)
        notificationManager = null
        mediaSession?.run {
            isActive = false
            release()
        }
        mediaSession = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            MUSIC_NOTIFICATION_CHANNEL_ID,
            "Музыка Element",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Управление воспроизведением музыки"
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MusicPlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private fun updateMediaSessionState(runtime: MusicRuntime) {
        val session = mediaSession ?: return
        val player = runtime.controller.playerHandle
        val state = runtime.controller.state.value
        val track = state.selectedTrack
        val durationMs = (state.durationSeconds.coerceAtLeast(0.0) * 1000.0).toLong()
        val positionMs = (state.currentTimeSeconds.coerceAtLeast(0.0) * 1000.0).toLong()

        val playbackState = when {
            track == null -> PlaybackState.STATE_STOPPED
            state.isBuffering -> PlaybackState.STATE_BUFFERING
            player.isPlaying -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_PAUSED
        }

        val playbackActions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SEEK_TO or
            PlaybackState.ACTION_STOP

        val playbackSpeed = if (player.isPlaying) 1f else 0f

        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(playbackActions)
                .setState(
                    playbackState,
                    positionMs.coerceAtLeast(0L),
                    playbackSpeed
                )
                .build()
        )

        if (track == null) {
            session.setMetadata(null)
            return
        }

        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, track.artist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs.coerceAtLeast(0L))
                .build()
        )
    }
}

private class ElementMusicPlayerNotificationManager(
    context: Context,
    private val runtime: MusicRuntime,
    notificationListener: PlayerNotificationManager.NotificationListener,
    mediaDescriptionAdapter: PlayerNotificationManager.MediaDescriptionAdapter,
    customActionReceiver: PlayerNotificationManager.CustomActionReceiver
) : PlayerNotificationManager(
    context,
    MUSIC_NOTIFICATION_CHANNEL_ID,
    MUSIC_NOTIFICATION_ID,
    mediaDescriptionAdapter,
    notificationListener,
    customActionReceiver,
    R.drawable.ic_nav_music,
    R.drawable.ic_music_play,
    R.drawable.ic_music_pause,
    R.drawable.ic_header_close,
    R.drawable.ic_music_previous,
    R.drawable.ic_music_next,
    R.drawable.ic_music_previous,
    R.drawable.ic_music_next,
    null
) {
    override fun createNotification(
        player: Player,
        builder: NotificationCompat.Builder?,
        ongoing: Boolean,
        largeIcon: Bitmap?
    ): NotificationCompat.Builder? {
        val notification = super.createNotification(player, builder, ongoing, largeIcon)
        val state = runtime.controller.state.value
        val durationMs = (state.durationSeconds.coerceAtLeast(0.0) * 1000.0).roundToInt()
        val currentMs = (state.currentTimeSeconds.coerceAtLeast(0.0) * 1000.0).roundToInt()
        return notification
            ?.setProgress(
                durationMs.coerceAtLeast(0),
                currentMs.coerceIn(0, durationMs.coerceAtLeast(0)),
                durationMs <= 0
            )
            ?.setSubText(null)
            ?.setOnlyAlertOnce(true)
            ?.setColorized(false)
            ?.setSilent(true)
            ?.setShowWhen(false)
    }
}
