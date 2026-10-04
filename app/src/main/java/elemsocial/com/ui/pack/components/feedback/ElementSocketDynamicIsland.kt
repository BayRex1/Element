package elemsocial.com.ui.pack.components.feedback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import elemsocial.com.R
import elemsocial.com.core.ws.ElementSocketClient
import kotlinx.coroutines.delay

private data class IslandMessage(
    val id: Long,
    val text: String,
    val animationRes: Int
)

@Composable
fun ElementSocketDynamicIsland(
    connectionState: ElementSocketClient.ConnectionState,
    modifier: Modifier = Modifier
) {
    var initialized by remember { mutableStateOf(false) }
    var nonce by remember { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf<IslandMessage?>(null) }
    var renderedMessage by remember { mutableStateOf<IslandMessage?>(null) }
    var islandVisible by remember { mutableStateOf(false) }

    fun show(text: String, animationRes: Int) {
        nonce += 1
        message = IslandMessage(
            id = nonce,
            text = text,
            animationRes = animationRes
        )
    }

    LaunchedEffect(connectionState) {
        if (!initialized) {
            initialized = true
            if (
                connectionState == ElementSocketClient.ConnectionState.Connecting ||
                connectionState == ElementSocketClient.ConnectionState.Handshaking
            ) {
                show("Подключение", R.raw.dynamic_island_clock)
            }
            return@LaunchedEffect
        }

        when (connectionState) {
            ElementSocketClient.ConnectionState.Connecting,
            ElementSocketClient.ConnectionState.Handshaking -> {
                show("Подключение", R.raw.dynamic_island_clock)
            }

            ElementSocketClient.ConnectionState.Ready -> {
                show("Подключено", R.raw.dynamic_island_success)
            }

            ElementSocketClient.ConnectionState.Disconnected -> {
                show("Отключение", R.raw.dynamic_island_error)
            }

            ElementSocketClient.ConnectionState.Error -> {
                show("Ошибка подключения", R.raw.dynamic_island_error)
            }
        }
    }

    LaunchedEffect(message?.id) {
        val current = message
        if (current != null) {
            renderedMessage = current
            islandVisible = true
            return@LaunchedEffect
        }

        if (renderedMessage != null) {
            islandVisible = false
            delay(320)
            renderedMessage = null
        }
    }

    LaunchedEffect(message?.id) {
        val current = message ?: return@LaunchedEffect
        delay(3_000)
        if (message?.id == current.id) {
            message = null
        }
    }

    val shown = renderedMessage ?: return
    val transition = updateTransition(targetState = islandVisible, label = "dynamic-island")

    val yOffset by transition.animateDp(
        transitionSpec = { tween(durationMillis = 300) },
        label = "dynamic-island-y"
    ) { visible ->
        if (visible) 0.dp else (-100).dp
    }

    val topCorner by transition.animateDp(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 300, delayMillis = 200)
            } else {
                tween(durationMillis = 300)
            }
        },
        label = "dynamic-island-top-corner"
    ) { visible ->
        if (visible) 100.dp else 0.dp
    }

    val blurRadius by transition.animateDp(
        transitionSpec = { tween(durationMillis = 300, delayMillis = 150) },
        label = "dynamic-island-blur"
    ) { visible ->
        if (visible) 0.dp else 10.dp
    }

    val opacity by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 220) },
        label = "dynamic-island-opacity"
    ) { visible ->
        if (visible) 1f else 0f
    }

    val shape = RoundedCornerShape(
        topStart = topCorner,
        topEnd = topCorner,
        bottomStart = 100.dp,
        bottomEnd = 100.dp
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = opacity
                translationY = yOffset.toPx()
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            .blur(blurRadius)
            .background(color = Color.Black, shape = shape)
            .border(width = 1.dp, color = Color.Black, shape = shape)
            .padding(horizontal = 15.dp, vertical = 8.dp)
    ) {
        AnimatedContent(
            targetState = shown,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
            },
            label = "dynamic-island-content"
        ) { current ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IslandAnimation(
                    animationRes = current.animationRes,
                    animationKey = current.id
                )

                Text(
                    text = current.text,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun IslandAnimation(
    animationRes: Int,
    animationKey: Long
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(animationRes)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = true,
        iterations = LottieConstants.IterateForever.takeIf { animationRes == R.raw.dynamic_island_clock } ?: 1,
        restartOnPlay = true
    )

    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(40.dp)
        )
    }
}
