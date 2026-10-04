package elemsocial.com.feature.music.presentation

import androidx.compose.runtime.staticCompositionLocalOf

val LocalMusicGateway = staticCompositionLocalOf<MusicGateway> {
    error("MusicGateway is not provided")
}

val LocalMusicController = staticCompositionLocalOf<MusicController> {
    error("MusicController is not provided")
}
