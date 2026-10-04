package elemsocial.com.ui.pack.components.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import elemsocial.com.core.model.LocalTransparencyMode
import elemsocial.com.core.model.TransparencyMode
import elemsocial.com.core.model.glassAlphaFor
import elemsocial.com.core.model.glassSoftAlphaFor
import elemsocial.com.core.model.shouldUseGlassBlur
import elemsocial.com.ui.pack.theme.ElementUiPalette

data class ElementContextMenuItem(
    val title: String,
    val onClick: () -> Unit,
    val color: Color = ElementUiPalette.TextPrimary,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val destructive: Boolean = false
)

@Composable
fun ElementContextMenu(
    expanded: Boolean,
    anchorBounds: IntRect?,
    items: List<ElementContextMenuItem>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    minWidth: Dp = 200.dp
) {
    if (!expanded || anchorBounds == null || items.isEmpty()) return

    val positionProvider = remember(anchorBounds) {
        ElementContextMenuPositionProvider(anchorBounds = anchorBounds)
    }
    val transparencyMode = LocalTransparencyMode.current
    val opaque = transparencyMode == TransparencyMode.OPAQUE
    val menuAlpha = if (opaque) 1f else glassAlphaFor(transparencyMode)
    val menuSoftAlpha = if (opaque) 1f else glassSoftAlphaFor(transparencyMode)
    val blurEnabled = shouldUseGlassBlur(transparencyMode)
    val menuBrush = Brush.verticalGradient(
        colors = listOf(
            ElementUiPalette.Block.copy(alpha = menuAlpha),
            if (opaque) {
                ElementUiPalette.Block.copy(alpha = menuSoftAlpha)
            } else {
                ElementUiPalette.BlockSoft.copy(alpha = menuSoftAlpha)
            }
        )
    )
    val menuShape = RoundedCornerShape(22.dp)

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = modifier
                .shadow(
                    elevation = 14.dp,
                    shape = menuShape,
                    ambientColor = ElementUiPalette.TextPrimary.copy(alpha = 0.10f),
                    spotColor = ElementUiPalette.TextPrimary.copy(alpha = 0.14f)
                )
                .clip(menuShape)
                .widthIn(min = minWidth, max = 260.dp)
        ) {
            if (blurEnabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(22.dp)
                        .background(brush = menuBrush)
                )
            }

            Column(
                modifier = Modifier
                    .clip(menuShape)
                    .background(brush = menuBrush)
                    .border(
                        width = 1.dp,
                        color = ElementUiPalette.scaledBorder(0.9f),
                        shape = menuShape
                    )
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (item.destructive) {
                                    ElementUiPalette.Error.copy(alpha = 0.15f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable(
                                enabled = item.enabled,
                                onClick = {
                                    item.onClick()
                                    onDismissRequest()
                                }
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.icon != null) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = if (item.destructive) ElementUiPalette.Error else item.color,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = item.title,
                            color = if (!item.enabled) {
                                item.color.copy(alpha = 0.45f)
                            } else if (item.destructive) {
                                ElementUiPalette.Error
                            } else {
                                item.color
                            },
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private class ElementContextMenuPositionProvider(
    private val anchorBounds: IntRect
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val margin = 8
        val verticalOffset = 6

        var x = this.anchorBounds.right - popupContentSize.width
        var y = this.anchorBounds.bottom + verticalOffset

        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        x = x.coerceIn(margin, maxX)

        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
        if (y > maxY) {
            y = this.anchorBounds.top - popupContentSize.height - verticalOffset
        }
        y = y.coerceIn(margin, maxY)

        return IntOffset(x, y)
    }
}
