package elemsocial.com.ui.pack.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.ui.pack.theme.ElementUiPalette

@Composable
fun ElementInteractionButton(
    icon: ImageVector,
    active: Boolean,
    enabled: Boolean,
    count: Int? = null,
    text: String? = null,
    shape: RoundedCornerShape,
    activeTintColor: Color = ElementUiPalette.Accent,
    activeBackgroundColor: Color = activeTintColor.copy(alpha = 0.2f),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = if (active) activeBackgroundColor else ElementUiPalette.Interaction
    val tint = if (active) activeTintColor else ElementUiPalette.InteractionText

    Row(
        modifier = modifier
            .clip(shape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .height(30.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )

        when {
            !text.isNullOrBlank() -> {
                Text(
                    text = text,
                    color = tint,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            (count ?: 0) > 0 -> {
                Text(
                    text = count.toString(),
                    color = tint,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}
