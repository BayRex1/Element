package elemsocial.com.ui.pack.components.animations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import elemsocial.com.R
import kotlinx.coroutines.delay
import kotlin.random.Random

private data class LikeHeart(
    val id: Int,
    val x: Float,
    val y: Float,
    val rotation: Float,
    val scale: Float
)

@Composable
fun ElementLikeBurst(
    trigger: Int,
    origin: Offset? = null,
    modifier: Modifier = Modifier
) {
    var hearts by remember { mutableStateOf<List<LikeHeart>>(emptyList()) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val fallbackX = with(LocalDensity.current) { maxWidth.toPx() / 2f }
        val fallbackY = with(LocalDensity.current) { maxHeight.toPx() / 2f }

        LaunchedEffect(trigger, origin, fallbackX, fallbackY) {
            if (trigger <= 0) return@LaunchedEffect

            val burstX = origin?.x ?: fallbackX
            val burstY = origin?.y ?: fallbackY

            hearts = hearts + LikeHeart(
                id = trigger,
                x = burstX,
                y = burstY,
                rotation = Random.nextFloat() * 60f - 30f,
                scale = 0.85f + Random.nextFloat() * 0.35f
            )

            delay(1000)
            hearts = hearts.filterNot { it.id == trigger }
        }

        hearts.forEach { heart ->
            var started by remember(heart.id) { mutableStateOf(false) }

            LaunchedEffect(heart.id) {
                started = true
            }

            val progress by animateFloatAsState(
                targetValue = if (started) 1f else 0f,
                animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
                label = "like-heart-progress"
            )

            val alpha = when {
                progress < 0.15f -> progress / 0.15f
                progress > 0.8f -> (1f - progress) / 0.2f
                else -> 1f
            }.coerceIn(0f, 1f)

            val pulseScale = when {
                progress < 0.15f -> progress / 0.15f * 1.2f
                progress < 0.3f -> 1.2f - ((progress - 0.15f) / 0.15f * 0.2f)
                else -> 1f
            }

            val floatOffsetY = if (progress <= 0.8f) {
                0f
            } else {
                ((progress - 0.8f) / 0.2f) * -20f
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_element_like),
                contentDescription = null,
                tint = Color(0xFFFF4E7A).copy(alpha = alpha),
                modifier = Modifier
                    .size(70.dp)
                    .graphicsLayer {
                        val scale = heart.scale * pulseScale
                        scaleX = scale
                        scaleY = scale
                        rotationZ = heart.rotation
                    }
                    .offset {
                        IntOffset(
                            x = (heart.x - 35f).toInt(),
                            y = (heart.y - 35f + floatOffsetY).toInt()
                        )
                    }
            )
        }
    }
}
