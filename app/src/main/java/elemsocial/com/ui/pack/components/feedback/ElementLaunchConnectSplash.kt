package elemsocial.com.ui.pack.components.feedback

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.R
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.delay

@Composable
fun ElementLaunchConnectSplash(
    wsUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isLightTheme = ElementUiPalette.Body.luminance() > 0.5f
    var showSignature by remember { mutableStateOf(false) }
    val signaturePulse = rememberInfiniteTransition(label = "launch-signature")
    val signatureTone by signaturePulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "launch-signature-tone"
    )
    val signatureAlpha by signaturePulse.animateFloat(
        initialValue = if (isLightTheme) 0.9f else 0.84f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "launch-signature-alpha"
    )
    val signatureColor = lerp(
        start = if (isLightTheme) Color(0xFF696473) else Color(0xFF9B9BA3),
        stop = if (isLightTheme) ElementUiPalette.Accent.copy(alpha = 0.9f) else Color(0xFFF1F1F3),
        fraction = signatureTone
    )

    LaunchedEffect(Unit) {
        delay(250)
        showSignature = true
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(
                id = if (isLightTheme) {
                    R.drawable.ic_element_logo
                } else {
                    R.drawable.ic_element_logo_light
                }
            ),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(72.dp)
        )

        AnimatedVisibility(
            visible = showSignature,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                animationSpec = tween(500),
                initialOffsetY = { it / 2 }
            ),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Text(
                text = "by moretti",
                color = signatureColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.graphicsLayer(alpha = signatureAlpha)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = ElementUiPalette.Block,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    clipboard.setText(AnnotatedString(wsUrl))
                    Toast.makeText(context, "Ссылка WebSocket скопирована", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = wsUrl,
                color = ElementUiPalette.TextPrimary,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = ElementUiPalette.TextSecondary
            )
        }

        Text(
            text = "Если подключение длится слишком долго, попробуйте перезапустить приложение, сменить сеть или включить VPN.",
            color = ElementUiPalette.TextPrimary,
            fontSize = 16.sp,
            lineHeight = 19.sp
        )
    }
}
