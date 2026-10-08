package elemsocial.com.ui.pack.components.navigation

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.core.model.LocalTransparencyMode
import elemsocial.com.core.model.glassAlphaFor
import elemsocial.com.core.model.glassSoftAlphaFor
import elemsocial.com.core.model.shouldUseGlassBlur
import elemsocial.com.ui.pack.theme.ElementUiPalette

data class ElementBottomNavItem(
    val title: String,
    @DrawableRes val iconRes: Int? = null,
    val iconText: String? = null,
    val iconBase64: String? = null,
    val badgeCount: Int? = null
)

@Composable
fun ElementBottomNav(
    items: List<ElementBottomNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showShadow: Boolean = false
) {
    if (items.isEmpty()) return
    val containerShape = RoundedCornerShape(30.dp)
    val activeShape = RoundedCornerShape(22.dp)
    val hasSelection = selectedIndex in items.indices
    val safeSelectedIndex = selectedIndex.takeIf { it in items.indices } ?: 0
    val mode = LocalTransparencyMode.current
    val alpha = glassAlphaFor(mode)
    val softAlpha = glassSoftAlphaFor(mode)

    Box(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp).padding(bottom = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().widthIn(max = 520.dp).height(68.dp)) {
            val pad = 6.dp
            val segmentWidth = (maxWidth - pad * 2) / items.size
            val sliderOffset by animateDpAsState(
                pad + segmentWidth * safeSelectedIndex,
                tween(320, easing = FastOutSlowInEasing),
                label = "bottom_nav_slider"
            )

            Box(
                Modifier.fillMaxSize()
                    .shadow(if (showShadow) 18.dp else 0.dp, containerShape)
                    .clip(containerShape)
                    .background(Brush.verticalGradient(listOf(ElementUiPalette.Block.copy(alpha), ElementUiPalette.BlockSoft.copy(softAlpha))))
                    .border(1.dp, ElementUiPalette.scaledBorder(0.85f), containerShape)
            ) {
                if (shouldUseGlassBlur(mode)) {
                    Box(
                        Modifier.matchParentSize().blur(22.dp)
                            .background(Brush.verticalGradient(listOf(ElementUiPalette.Block.copy(alpha), ElementUiPalette.BlockSoft.copy(softAlpha))))
                    )
                }
            }

            if (hasSelection) {
                Box(
                    Modifier.offset(sliderOffset, 6.dp).width(segmentWidth).height(56.dp)
                        .shadow(10.dp, activeShape)
                        .clip(activeShape)
                        .background(Brush.verticalGradient(listOf(ElementUiPalette.Block.copy(alpha), ElementUiPalette.Accent.copy(alpha = 0.12f))))
                        .border(1.dp, ElementUiPalette.scaledBorder(1.15f), activeShape)
                )
            }

            Row(
                Modifier.fillMaxSize().padding(horizontal = pad, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val selected = hasSelection && index == selectedIndex
                    val interactionSource = remember { MutableInteractionSource() }
                    val iconColor by animateColorAsState(
                        if (selected) ElementUiPalette.Accent else ElementUiPalette.TextPrimary.copy(alpha = 0.72f),
                        tween(220), label = "bottom_nav_icon"
                    )
                    val textColor by animateColorAsState(
                        if (selected) ElementUiPalette.TextPrimary else ElementUiPalette.TextPrimary.copy(alpha = 0.68f),
                        tween(220), label = "bottom_nav_text"
                    )
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(activeShape).clickable(
                            interactionSource = interactionSource, indication = null
                        ) { onSelect(index) }
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(Modifier.width(30.dp).height(24.dp), contentAlignment = Alignment.Center) {
                                val bitmap: ImageBitmap? = remember(item.iconBase64) {
                                    item.iconBase64?.let { encoded ->
                                        runCatching {
                                            val bytes = Base64.decode(encoded, Base64.DEFAULT)
                                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                        }.getOrNull()
                                    }
                                }
                                when {
                                    item.iconRes != null && item.iconRes != 0 -> Icon(
                                        painter = painterResource(item.iconRes),
                                        contentDescription = item.title,
                                        tint = iconColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    bitmap != null -> Image(bitmap = bitmap, contentDescription = item.title, modifier = Modifier.size(22.dp))
                                    else -> Text(item.iconText.orEmpty().ifBlank { "•" }, color = iconColor, fontSize = 21.sp, maxLines = 1)
                                }
                                val count = item.badgeCount ?: 0
                                if (count > 0) {
                                    Box(
                                        Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp)
                                            .background(ElementUiPalette.Accent, CircleShape)
                                            .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                                            .padding(horizontal = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(count.coerceAtMost(99).toString(), color = Color.White, fontSize = 9.sp, lineHeight = 9.sp, maxLines = 1)
                                    }
                                }
                            }
                            Text(
                                item.title, color = textColor, fontSize = 9.sp, lineHeight = 9.sp,
                                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().padding(top = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
