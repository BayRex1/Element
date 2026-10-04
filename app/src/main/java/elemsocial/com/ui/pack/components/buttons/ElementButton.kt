package elemsocial.com.ui.pack.components.buttons

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import elemsocial.com.ui.pack.theme.ElementUiPalette

enum class ElementButtonVariant {
    Primary,
    Soft
}

@Composable
fun ElementButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    variant: ElementButtonVariant = ElementButtonVariant.Primary,
    textFontWeight: FontWeight? = null
) {
    val colors = when (variant) {
        ElementButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = ElementUiPalette.Accent,
            contentColor = Color.White,
            disabledContainerColor = ElementUiPalette.Accent.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.8f)
        )

        ElementButtonVariant.Soft -> ButtonDefaults.buttonColors(
            containerColor = ElementUiPalette.BlockSoft,
            contentColor = ElementUiPalette.TextPrimary,
            disabledContainerColor = ElementUiPalette.BlockSoft.copy(alpha = 0.7f),
            disabledContentColor = ElementUiPalette.TextPrimary.copy(alpha = 0.7f)
        )
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = colors,
        modifier = modifier.fillMaxWidth()
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = if (variant == ElementButtonVariant.Primary) Color.White else ElementUiPalette.TextPrimary
            )
        } else {
            Text(
                text = title,
                fontWeight = textFontWeight
                    ?: if (variant == ElementButtonVariant.Primary) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Medium
                    }
            )
        }
    }
}
